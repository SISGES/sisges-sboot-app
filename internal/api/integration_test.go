package api

import (
	"bytes"
	"context"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"os"
	"testing"
	"time"
)

func TestPostgresMigrationAndAuthFlow(t *testing.T) {
	databaseURL := os.Getenv("SISGES_TEST_DATABASE_URL")
	if databaseURL == "" {
		t.Skip("SISGES_TEST_DATABASE_URL is not set")
	}

	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()
	app, err := New(ctx, Config{
		DatabaseURL:      databaseURL,
		JWTSecret:        []byte("integration-test-secret-at-least-32-bytes"),
		JWTExpiry:        time.Hour,
		DBMaxConnections: 2,
		MigrationDir:     "../../db/migration",
		SeedEnabled:      true,
	})
	if err != nil {
		t.Fatal(err)
	}
	defer app.Close()

	health := httptest.NewRecorder()
	app.Handler().ServeHTTP(health, httptest.NewRequest(http.MethodGet, "/health", nil))
	if health.Code != http.StatusOK {
		t.Fatalf("health status=%d body=%s", health.Code, health.Body.String())
	}

	loginBody := bytes.NewBufferString(`{"email":"adm0001@sisges.com","password":"admin123"}`)
	login := httptest.NewRecorder()
	app.Handler().ServeHTTP(login, httptest.NewRequest(http.MethodPost, "/api/auth/login", loginBody))
	if login.Code != http.StatusOK {
		t.Fatalf("login status=%d body=%s", login.Code, login.Body.String())
	}
	var response loginResponse
	if err := json.Unmarshal(login.Body.Bytes(), &response); err != nil {
		t.Fatal(err)
	}
	if response.AccessToken == "" || response.User.Role != "ADMIN" {
		t.Fatalf("unexpected login response: %#v", response)
	}

	request := httptest.NewRequest(http.MethodGet, "/api/users/me", nil)
	request.Header.Set("Authorization", "Bearer "+response.AccessToken)
	me := httptest.NewRecorder()
	app.Handler().ServeHTTP(me, request)
	if me.Code != http.StatusOK {
		t.Fatalf("authenticated request status=%d body=%s", me.Code, me.Body.String())
	}

	missingCurrentPassword := httptest.NewRequest(http.MethodPatch, "/api/users/me", bytes.NewBufferString(`{"password":"admin123"}`))
	missingCurrentPassword.Header.Set("Authorization", "Bearer "+response.AccessToken)
	missingCurrentPasswordResult := httptest.NewRecorder()
	app.Handler().ServeHTTP(missingCurrentPasswordResult, missingCurrentPassword)
	if missingCurrentPasswordResult.Code != http.StatusBadRequest {
		t.Fatalf("password update without current password status=%d body=%s", missingCurrentPasswordResult.Code, missingCurrentPasswordResult.Body.String())
	}

	wrongCurrentPassword := httptest.NewRequest(http.MethodPatch, "/api/users/me", bytes.NewBufferString(`{"password":"admin123","currentPassword":"incorrect"}`))
	wrongCurrentPassword.Header.Set("Authorization", "Bearer "+response.AccessToken)
	wrongCurrentPasswordResult := httptest.NewRecorder()
	app.Handler().ServeHTTP(wrongCurrentPasswordResult, wrongCurrentPassword)
	if wrongCurrentPasswordResult.Code != http.StatusBadRequest {
		t.Fatalf("password update with incorrect current password status=%d body=%s", wrongCurrentPasswordResult.Code, wrongCurrentPasswordResult.Body.String())
	}

	validPasswordUpdate := httptest.NewRequest(http.MethodPatch, "/api/users/me", bytes.NewBufferString(`{"password":"admin123","currentPassword":"admin123"}`))
	validPasswordUpdate.Header.Set("Authorization", "Bearer "+response.AccessToken)
	validPasswordUpdateResult := httptest.NewRecorder()
	app.Handler().ServeHTTP(validPasswordUpdateResult, validPasswordUpdate)
	if validPasswordUpdateResult.Code != http.StatusOK {
		t.Fatalf("password update with current password status=%d body=%s", validPasswordUpdateResult.Code, validPasswordUpdateResult.Body.String())
	}

	// Exercise startup against a schema previously managed by Flyway. This
	// catches pgx parameter type mismatches in the compatibility lookup.
	if _, err := app.db.Exec(ctx, `
		CREATE TABLE IF NOT EXISTS sisges.flyway_schema_history (
			version TEXT,
			success BOOLEAN NOT NULL
		);
		INSERT INTO sisges.flyway_schema_history(version,success) VALUES('1',true);
		DELETE FROM sisges.go_schema_migrations WHERE version=1;
	`); err != nil {
		t.Fatal(err)
	}
	if err := app.migrate(ctx); err != nil {
		t.Fatalf("migrate with Flyway history: %v", err)
	}
}
