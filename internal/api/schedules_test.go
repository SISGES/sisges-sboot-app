package api

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io"
	"mime/multipart"
	"net/http"
	"net/http/httptest"
	"os"
	"strings"
	"testing"
	"time"
)

func TestScheduleValidation(t *testing.T) {
	for _, slots := range [][]scheduleSlot{{{0, 0, 1}}, {{1, 5, 1}}, {{1, 0, 0}}, {{1, 0, 1}, {1, 0, 2}}} {
		if validateSchedule(slots) == nil {
			t.Fatalf("accepted invalid schedule: %v", slots)
		}
	}
	if validateSchedule([]scheduleSlot{{1, 0, 1}, {5, 4, 2}}) != nil {
		t.Fatal("valid schedule rejected")
	}
	for i, times := range lessonTimes {
		start, _ := time.Parse("15:04", times[0])
		end, _ := time.Parse("15:04", times[1])
		if end.Sub(start) != 50*time.Minute {
			t.Fatalf("slot %d not 50 minutes", i)
		}
	}
	if validLessonAttachment(lessonAttachment{"x.pdf", "/api/files/lesson-files/2/x.pdf"}, 1) {
		t.Fatal("accepted another user's upload")
	}
	if validLessonAttachment(lessonAttachment{"x.pdf", "/api/files/lesson-files/1/../x.pdf"}, 1) {
		t.Fatal("accepted traversal")
	}
}
func TestScheduleAndDailyRecordsPostgres(t *testing.T) {
	databaseURL := os.Getenv("SISGES_TEST_DATABASE_URL")
	if databaseURL == "" {
		t.Skip("SISGES_TEST_DATABASE_URL is not set")
	}
	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()
	app, err := New(ctx, Config{DatabaseURL: databaseURL, JWTSecret: []byte("integration-test-secret-at-least-32-bytes"), JWTExpiry: time.Hour, DBMaxConnections: 4, MigrationDir: "../../db/migration", SeedEnabled: true})
	if err != nil {
		t.Fatal(err)
	}
	defer app.Close()
	var classID, teacherID, userID, disciplineID int
	err = app.db.QueryRow(ctx, `SELECT tc.class_id,tc.teacher_id,t.user_id,cd.discipline_id FROM sisges.teacher_class tc JOIN sisges.teacher t ON t.id=tc.teacher_id JOIN sisges.class_discipline cd ON cd.class_id=tc.class_id WHERE tc.deleted_at IS NULL AND cd.deleted_at IS NULL ORDER BY tc.class_id,tc.teacher_id,cd.discipline_id LIMIT 1`).Scan(&classID, &teacherID, &userID, &disciplineID)
	if err != nil {
		t.Fatal(err)
	}
	_, err = app.db.Exec(ctx, `INSERT INTO sisges.discipline_teacher(discipline_id,teacher_id) VALUES($1,$2) ON CONFLICT DO NOTHING`, disciplineID, teacherID)
	if err != nil {
		t.Fatal(err)
	}
	// Dedicated test DB only; choose a date with no seeded lessons.
	date := "2090-01-02" // Monday
	defer app.db.Exec(ctx, `DELETE FROM sisges.class_meeting WHERE class_id=$1 AND meeting_date=$2::date`, classID, date)
	defer app.db.Exec(ctx, `DELETE FROM sisges.class_schedule WHERE class_id=$1`, classID)
	tokenFor := func(role string, id int) string {
		t.Helper()
		var p principal
		err := app.db.QueryRow(ctx, `SELECT id,email,user_role FROM sisges.users WHERE user_role=$1 AND ($2=0 OR id=$2) AND deleted_at IS NULL ORDER BY id LIMIT 1`, role, id).Scan(&p.ID, &p.Email, &p.Role)
		if err != nil {
			t.Fatal(err)
		}
		token, err := app.signToken(p)
		if err != nil {
			t.Fatal(err)
		}
		return token
	}
	admin := tokenFor("ADMIN", 0)
	teacher := tokenFor("TEACHER", userID)
	student := tokenFor("STUDENT", 0)
	request := func(method, path, token string, body any, status int) *httptest.ResponseRecorder {
		t.Helper()
		raw, _ := json.Marshal(body)
		req := httptest.NewRequest(method, path, bytes.NewReader(raw))
		req.Header.Set("Authorization", "Bearer "+token)
		w := httptest.NewRecorder()
		app.Handler().ServeHTTP(w, req)
		if w.Code != status {
			t.Fatalf("%s %s status=%d want=%d body=%s", method, path, w.Code, status, w.Body.String())
		}
		return w
	}
	scheduleURL := fmt.Sprintf("/api/classes/%d/schedule", classID)
	dayURL := fmt.Sprintf("/api/classes/%d/lesson-day", classID)
	var original scheduleResponse
	json.Unmarshal(request("GET", scheduleURL, admin, nil, 200).Body.Bytes(), &original)
	request("PUT", scheduleURL, teacher, scheduleResponse{}, 403)
	request("GET", dayURL+"?date="+date, student, nil, 403)
	change := scheduleResponse{Revision: original.Revision, Slots: []scheduleSlot{{1, 0, disciplineID}, {1, 3, disciplineID}}}
	request("PUT", scheduleURL, admin, change, 200)
	request("PUT", scheduleURL, admin, change, 400) // stale version
	var day []dailyLesson
	json.Unmarshal(request("GET", dayURL+"?date="+date, teacher, nil, 200).Body.Bytes(), &day)
	if len(day) != 2 || day[0].StartTime != "07:00" || day[1].StartTime != "09:45" {
		t.Fatalf("unexpected slots: %#v", day)
	}
	request("GET", dayURL+"?date=2090-01-07", teacher, nil, 200) // weekend
	body := map[string]any{"date": date, "slot": 0, "disciplineId": disciplineID, "content": "Frações", "attachments": []lessonAttachment{}}
	request("PUT", dayURL, teacher, body, 200)
	body["content"] = "Frações e exercícios"
	request("PUT", dayURL, teacher, body, 200)
	var count int
	app.db.QueryRow(ctx, `SELECT count(*) FROM sisges.class_meeting WHERE class_id=$1 AND meeting_date=$2::date AND schedule_slot=0`, classID, date).Scan(&count)
	if count != 1 {
		t.Fatalf("duplicate records: %d", count)
	}
	json.Unmarshal(request("GET", dayURL+"?date="+date, teacher, nil, 200).Body.Bytes(), &day)
	if day[0].MeetingID == nil || day[0].Content != "Frações e exercícios" {
		t.Fatalf("record did not persist: %#v", day)
	}
	detail := request("GET", fmt.Sprintf("/api/class/%d", *day[0].MeetingID), teacher, nil, 200)
	if !bytes.Contains(detail.Body.Bytes(), []byte("Frações e exercícios")) {
		t.Fatal("content missing in detail")
	}
	request("PUT", fmt.Sprintf("/api/class/update/%d", *day[0].MeetingID), teacher, map[string]any{}, 400)
	request("PUT", scheduleURL, admin, scheduleResponse{Revision: original.Revision + 1, Slots: []scheduleSlot{}}, 200)
	json.Unmarshal(request("GET", dayURL+"?date="+date, teacher, nil, 200).Body.Bytes(), &day)
	if len(day) != 1 || day[0].Content != "Frações e exercícios" {
		t.Fatal("schedule change lost saved record")
	}
	body["slot"] = 3
	request("PUT", dayURL, teacher, body, 400) // removed pending slot
	body["slot"] = 0
	body["attachments"] = []lessonAttachment{{"foreign.pdf", "/api/files/lesson-files/99999/x.pdf"}}
	request("PUT", dayURL, teacher, body, 400)
	request("POST", "/api/class", teacher, map[string]any{}, 400) // manual creation no longer bypasses timetable
}

func TestLessonFileUploadAndDownload(t *testing.T) {
	var uploadedPath string
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		uploadedPath = r.URL.Path
		if r.Method == "GET" {
			w.Header().Set("Content-Type", "application/pdf")
			w.Write([]byte("%PDF-test"))
			return
		}
		io.Copy(io.Discard, r.Body)
		w.WriteHeader(200)
	}))
	defer server.Close()
	storage, err := NewR2Storage(Config{R2Endpoint: server.URL, R2AccessKeyID: "test", R2SecretAccessKey: "test", R2Bucket: "sisges-prd", R2Region: "auto"})
	if err != nil {
		t.Fatal(err)
	}
	app := testApp()
	app.storage = storage
	for _, tc := range []struct {
		name, subdir string
		want         int
	}{{"lesson.pdf", "lesson-files", 200}, {"lesson.pdf", "post-img", 400}, {"lesson.html", "lesson-files", 400}, {"photo.png", "post-img", 200}} {
		var body bytes.Buffer
		mw := multipart.NewWriter(&body)
		part, _ := mw.CreateFormFile("file", tc.name)
		part.Write([]byte("%PDF-test"))
		mw.WriteField("subdir", tc.subdir)
		mw.Close()
		req := httptest.NewRequest("POST", "/api/upload", &body)
		req.Header.Set("Content-Type", mw.FormDataContentType())
		req = req.WithContext(context.WithValue(req.Context(), principalKey{}, &principal{ID: 42, Role: "TEACHER"}))
		w := httptest.NewRecorder()
		app.uploadFile(w, req)
		if w.Code != tc.want {
			t.Fatalf("upload %s/%s: %d %s", tc.subdir, tc.name, w.Code, w.Body.String())
		}
		if tc.subdir == "lesson-files" && tc.want == 200 {
			var result map[string]string
			json.Unmarshal(w.Body.Bytes(), &result)
			if !strings.HasPrefix(uploadedPath, "/sisges-prd/lesson-files/42/") || !validLessonAttachment(lessonAttachment{"lesson.pdf", result["path"]}, 42) {
				t.Fatal("upload not scoped to owner")
			}
			get := httptest.NewRequest("GET", result["path"], nil)
			get.SetPathValue("key", strings.TrimPrefix(result["path"], "/api/files/"))
			get = get.WithContext(req.Context())
			download := httptest.NewRecorder()
			app.downloadFile(download, get)
			if download.Code != 200 || download.Header().Get("Content-Disposition") != "attachment" || download.Body.String() != "%PDF-test" {
				t.Fatal("download failed or not attachment")
			}
		}
	}
}
