package api

import (
	"bytes"
	"context"
	"crypto/sha256"
	"encoding/hex"
	"io"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
	"time"
)

func testApp() *App {
	return &App{cfg: Config{JWTSecret: []byte("a-test-secret-that-is-at-least-32-bytes-long"), JWTExpiry: time.Hour}}
}

func TestJWTCompatibilityClaimsAndTamperDetection(t *testing.T) {
	a := testApp()
	token, err := a.signToken(principal{ID: 42, Email: "student@sisges.com", Role: "STUDENT"})
	if err != nil {
		t.Fatal(err)
	}
	p, err := a.parseToken(token)
	if err != nil {
		t.Fatal(err)
	}
	if p.ID != 42 || p.Email != "student@sisges.com" || p.Role != "STUDENT" {
		t.Fatalf("unexpected claims: %#v", p)
	}
	tampered := token[:len(token)-1] + map[bool]string{true: "A", false: "B"}[strings.HasSuffix(token, "A")]
	if _, err := a.parseToken(tampered); err == nil {
		t.Fatal("tampered token accepted")
	}
}

func TestPathAndOriginSafetyHelpers(t *testing.T) {
	for input, want := range map[string]string{"profiles": "profiles", "materials/2026": "materials/2026", "../../etc": "general", "bad space": "general", "": "general"} {
		if got := validSubdir(input); got != want {
			t.Errorf("validSubdir(%q)=%q want %q", input, got, want)
		}
	}
	if !wildcardMatch("https://*.vercel.app", "https://sisges.vercel.app") {
		t.Fatal("expected origin match")
	}
	if wildcardMatch("https://*.vercel.app", "https://vercel.app.evil.test") {
		t.Fatal("unsafe origin matched")
	}
}

func TestDomainValidation(t *testing.T) {
	if err := validateRegistration(registerRequest{Name: "Aluno", Password: "secret12", BirthDate: "2010-01-01", Gender: "MALE", Role: "STUDENT"}); err == nil {
		t.Fatal("student without responsible accepted")
	}
	if err := validateMeeting(meetingRequest{Date: "2026-09-14", StartTime: "10:00", EndTime: "09:00", ClassID: 1, DisciplineID: 1}); err == nil {
		t.Fatal("reversed meeting times accepted")
	}
	if _, ok := nextAcademicYear("6º ano"); !ok {
		t.Fatal("year progression missing")
	}
}

func TestRouteTableHasNoConflicts(t *testing.T) {
	a := testApp()
	a.feed = newFeedHub(1)
	if handler := a.routes(); handler == nil {
		t.Fatal("nil router")
	}
}

func TestLoadConfigRejectsMissingSecret(t *testing.T) {
	t.Setenv("DATABASE_URL", "postgresql://localhost/sisges")
	t.Setenv("SECURITY_JWT_SECRET_KEY", "")
	if _, err := LoadConfig(); err == nil {
		t.Fatal("missing JWT secret accepted")
	}
}

func TestLegacyDatabaseConfigEscapesCredentials(t *testing.T) {
	t.Setenv("DATABASE_URL", "")
	t.Setenv("SPRING_DATASOURCE_URL", "jdbc:postgresql://db:5432/sisges")
	t.Setenv("SPRING_DATASOURCE_USERNAME", "user@example.com")
	t.Setenv("SPRING_DATASOURCE_PASSWORD", "p@ss:word")
	t.Setenv("SECURITY_JWT_SECRET_KEY", "a-test-secret-that-is-at-least-32-bytes-long")
	cfg, err := LoadConfig()
	if err != nil {
		t.Fatal(err)
	}
	if !strings.Contains(cfg.DatabaseURL, "user%40example.com:p%40ss%3Aword@") {
		t.Fatalf("credentials not escaped: %s", cfg.DatabaseURL)
	}
}

func BenchmarkJWTParse(b *testing.B) {
	a := testApp()
	token, _ := a.signToken(principal{ID: 1, Email: "admin@sisges.com", Role: "ADMIN"})
	b.ReportAllocs()
	for b.Loop() {
		if _, err := a.parseToken(token); err != nil {
			b.Fatal(err)
		}
	}
}

func TestS3RequestStreamsAndSignsPayload(t *testing.T) {
	payload := []byte("streamed without buffering")
	sum := sha256.Sum256(payload)
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodPut || r.URL.Path != "/bucket/profiles/file.txt" {
			t.Errorf("unexpected request: %s %s", r.Method, r.URL.Path)
		}
		if !strings.HasPrefix(r.Header.Get("Authorization"), "AWS4-HMAC-SHA256 Credential=access/") {
			t.Error("missing SigV4 authorization")
		}
		if r.Header.Get("x-amz-content-sha256") != hex.EncodeToString(sum[:]) {
			t.Error("incorrect payload hash")
		}
		got, _ := io.ReadAll(r.Body)
		if !bytes.Equal(got, payload) {
			t.Errorf("payload=%q", got)
		}
		w.WriteHeader(http.StatusOK)
	}))
	defer server.Close()
	cfg := Config{MinioEndpoint: server.URL, MinioAccessKey: "access", MinioSecretKey: "secret", MinioBucket: "bucket", MinioRegion: "us-east-1"}
	storage, err := NewS3Storage(cfg)
	if err != nil {
		t.Fatal(err)
	}
	resp, err := storage.request(context.Background(), http.MethodPut, "profiles/file.txt", hex.EncodeToString(sum[:]), "text/plain", bytes.NewReader(payload), int64(len(payload)))
	if err != nil {
		t.Fatal(err)
	}
	resp.Body.Close()
}
