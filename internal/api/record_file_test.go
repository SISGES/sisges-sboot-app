package api

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"mime/multipart"
	"net/http"
	"net/http/httptest"
	"strings"
	"sync"
	"testing"
)

func TestRecordAttachmentFlowPostgres(t *testing.T) {
	app := openSchoolTestAPI(t, "SISGES_TEST_DATABASE_URL")
	ctx := t.Context()
	const teacherEmail = "ana.clara.ferreira@sisges.local"
	teacher := app.loginSeed(teacherEmail)
	student := app.loginSeed("alice.almeida@sisges.local")
	otherStudent := app.loginSeed("arthur.almeida@sisges.local")
	otherTeacher := app.loginSeed("bruno.henrique.costa@sisges.local")
	admin := app.loginSeed("adm0001@sisges.com")
	var cid, tid, did int
	err := app.db.QueryRow(ctx, `SELECT s.class_id,t.id,cd.discipline_id FROM sisges.student s
		JOIN sisges.users u ON u.id=s.user_id
		JOIN sisges.teacher_class tc ON tc.class_id=s.class_id AND tc.deleted_at IS NULL
		JOIN sisges.teacher t ON t.id=tc.teacher_id JOIN sisges.users tu ON tu.id=t.user_id
		JOIN sisges.class_discipline cd ON cd.class_id=s.class_id AND cd.deleted_at IS NULL
		WHERE u.register='ALU0001' AND tu.email=$1 ORDER BY cd.discipline_id LIMIT 1`, teacherEmail).Scan(&cid, &tid, &did)
	if err != nil {
		t.Fatal(err)
	}
	tag, err := app.db.Exec(ctx, "INSERT INTO sisges.discipline_teacher(discipline_id,teacher_id) VALUES($1,$2) ON CONFLICT DO NOTHING", did, tid)
	if err != nil {
		t.Fatal(err)
	}
	if tag.RowsAffected() > 0 {
		defer app.db.Exec(ctx, "DELETE FROM sisges.discipline_teacher WHERE discipline_id=$1 AND teacher_id=$2", did, tid)
	}
	var mid int
	err = app.db.QueryRow(ctx, `INSERT INTO sisges.class_meeting(class_id,teacher_id,discipline_id,meeting_date,start_time,end_time)
		VALUES($1,$2,$3,current_date,'10:00','10:50') RETURNING id`, cid, tid, did).Scan(&mid)
	if err != nil {
		t.Fatal(err)
	}
	defer app.db.Exec(ctx, "DELETE FROM sisges.class_meeting WHERE id=$1", mid)
	openGradeTestCycle(t, app)

	const payload = "Lista de exercícios"
	var objects sync.Map
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		switch r.Method {
		case "PUT":
			data, err := io.ReadAll(r.Body)
			if err != nil {
				t.Error(err)
				w.WriteHeader(500)
				return
			}
			objects.Store(r.URL.Path, string(data))
		case "GET":
			value, ok := objects.Load(r.URL.Path)
			if !ok {
				http.NotFound(w, r)
				return
			}
			w.Header().Set("Content-Type", "text/plain")
			io.WriteString(w, value.(string))
		default:
			w.WriteHeader(405)
		}
	}))
	defer server.Close()
	app.storage, err = NewR2Storage(Config{R2Endpoint: server.URL, R2AccessKeyID: "test", R2SecretAccessKey: "test", R2Bucket: "sisges-prd", R2Region: "auto"})
	if err != nil {
		t.Fatal(err)
	}
	upload := func(destination, token string) string {
		t.Helper()
		var body bytes.Buffer
		form := multipart.NewWriter(&body)
		part, err := form.CreateFormFile("file", "exercicios.txt")
		if err != nil {
			t.Fatal(err)
		}
		io.WriteString(part, payload)
		form.Close()
		r := httptest.NewRequest("POST", "/api/upload?subdir="+destination, &body)
		r.Header.Set("Content-Type", form.FormDataContentType())
		r.Header.Set("Authorization", "Bearer "+token)
		w := httptest.NewRecorder()
		app.Handler().ServeHTTP(w, r)
		var result struct{ Path string }
		if w.Code != 200 || json.Unmarshal(w.Body.Bytes(), &result) != nil || !strings.HasPrefix(result.Path, "/api/files/"+destination+"/") {
			t.Fatalf("envio de %s: %d %s", destination, w.Code, w.Body.String())
		}
		return result.Path
	}
	download := func(path, token string, want int) {
		t.Helper()
		r := httptest.NewRequest("GET", path, nil)
		r.Header.Set("Authorization", "Bearer "+token)
		w := httptest.NewRecorder()
		app.Handler().ServeHTTP(w, r)
		if w.Code != want || (want == 200 && (w.Body.String() != payload || w.Header().Get("Content-Disposition") != "attachment")) {
			t.Fatalf("leitura de %s: %d %s", path, w.Code, w.Body.String())
		}
	}
	for _, destination := range []string{"materials", "activities"} {
		file := upload(destination, teacher)
		download(file, student, 403)
		body := map[string]any{"title": "Exercícios", "classId": cid, "disciplineId": did, "classMeetingId": mid, "maxPoints": 10, "trimesterNumber": 1}
		body["filePath"] = upload(destination, otherTeacher)
		app.call("POST", "/"+destination, teacher, body, 400, nil)
		body["filePath"] = file
		var created struct {
			ID       int
			FilePath string
		}
		app.call("POST", "/"+destination, teacher, body, 201, &created)
		if created.FilePath != file {
			t.Fatalf("caminho do anexo: %q", created.FilePath)
		}
		if destination == "materials" {
			defer app.db.Exec(ctx, "DELETE FROM sisges.discipline_material WHERE id=$1", created.ID)
		}
		download(file, teacher, 200)
		download(file, student, 200)
		download(file, admin, 200)
		download(file, otherStudent, 403)
		download(file, otherTeacher, 403)
		if destination == "activities" {
			objects.Store("/sisges-prd/activities/exercicios-antigos.txt", payload)
			_, err = app.db.Exec(ctx, "UPDATE sisges.evaluative_activity SET file_path='/uploads/activities/exercicios-antigos.txt' WHERE id=$1", created.ID)
			if err != nil {
				t.Fatal(err)
			}
			file = "/api/files/activities/exercicios-antigos.txt"
			download(file, student, 200)
			download(file, otherStudent, 403)
		}
		app.call("DELETE", fmt.Sprintf("/%s/%d", destination, created.ID), teacher, nil, 204, nil)
		download(file, student, 403)
	}
}

func TestPostImageAudiencePostgres(t *testing.T) {
	app := openSchoolTestAPI(t, "SISGES_TEST_DATABASE_URL")
	teacher := app.loginSeed("ana.clara.ferreira@sisges.local")
	student := app.loginSeed("alice.almeida@sisges.local")
	admin := app.loginSeed("adm0001@sisges.com")

	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "image/png")
		io.WriteString(w, "imagem")
	}))
	defer server.Close()
	var err error
	app.storage, err = NewR2Storage(Config{R2Endpoint: server.URL, R2AccessKeyID: "teste", R2SecretAccessKey: "teste", R2Bucket: "sisges-prd", R2Region: "auto"})
	if err != nil {
		t.Fatal(err)
	}
	check := func(token, file string, want int) {
		t.Helper()
		r := httptest.NewRequest("GET", file, nil)
		r.Header.Set("Authorization", "Bearer "+token)
		w := httptest.NewRecorder()
		app.Handler().ServeHTTP(w, r)
		if w.Code != want || w.Header().Get("Cache-Control") != "private, no-store" || (want == 200 && w.Body.String() != "imagem") {
			t.Fatalf("imagem %s: status %d, cache %s, corpo %s", file, w.Code, w.Header().Get("Cache-Control"), w.Body.String())
		}
	}
	user, err := app.parseToken(teacher)
	if err != nil {
		t.Fatal(err)
	}
	path := fmt.Sprintf("/api/files/post-img/%d/0123456789abcdef0123456789abcdef.png", user.ID)
	check(teacher, path, 404)
	body := map[string]any{"title": "Aviso de teste", "type": "IMAGE", "imagePath": path, "hiddenForRoles": []string{"STUDENT"}}
	var post announcementResponse
	app.call("POST", "/announcements", teacher, body, 201, &post)
	postURL := fmt.Sprintf("/announcements/%d", post.ID)
	defer app.call("DELETE", postURL, admin, nil, 204, nil)
	check(student, path, 404)
	check(admin, path, 200)
	check(teacher, path, 200)
	app.call("PUT", postURL, admin, map[string]any{"hiddenForRoles": []string{}}, 200, nil)
	check(student, path, 200)
	app.call("PUT", postURL, admin, map[string]any{"hiddenForRoles": []string{"STUDENT", "ADMIN"}, "imagePath": "/uploads/announcements/antiga.png"}, 200, nil)
	check(admin, "/api/files/announcements/antiga.png", 200)
	check(student, "/api/files/post-img/antiga.png", 404)
	check(teacher, "/api/files/post-img/antiga.png", 200)
	body["imagePath"] = "/api/files/post-img/antiga.png"
	app.call("POST", "/announcements", teacher, body, 400, nil)
}
