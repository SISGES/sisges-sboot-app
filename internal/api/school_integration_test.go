package api

import (
	"bytes"
	"context"
	"encoding/json"
	"net/http/httptest"
	"os"
	"strconv"
	"testing"
	"time"
)

type schoolTestAPI struct {
	*App
	t *testing.T
}

func openSchoolTestAPI(t *testing.T, databaseEnv string) schoolTestAPI {
	t.Helper()
	url := os.Getenv(databaseEnv)
	if url == "" {
		t.Skipf("%s is not set", databaseEnv)
	}
	app, err := New(context.Background(), Config{
		DatabaseURL: url,
		JWTSecret:   []byte("school-integration-test-secret-key"),
		JWTExpiry:   time.Hour, DBMaxConnections: 4,
		MigrationDir: "../../db/migration", SeedEnabled: true,
	})
	if err != nil {
		t.Fatal(err)
	}
	t.Cleanup(app.Close)
	return schoolTestAPI{App: app, t: t}
}

func (app schoolTestAPI) call(method, path, token string, payload any, status int, dest any) {
	app.t.Helper()
	var body bytes.Buffer
	if payload != nil {
		if err := json.NewEncoder(&body).Encode(payload); err != nil {
			app.t.Fatal(err)
		}
	}
	r := httptest.NewRequest(method, "/api"+path, &body)
	r.Header.Set("Content-Type", "application/json")
	if token != "" {
		r.Header.Set("Authorization", "Bearer "+token)
	}
	w := httptest.NewRecorder()
	app.Handler().ServeHTTP(w, r)
	if w.Code != status {
		app.t.Fatalf("%s %s: got %d, want %d: %s", method, path, w.Code, status, w.Body.String())
	}
	if dest != nil {
		if err := json.Unmarshal(w.Body.Bytes(), dest); err != nil {
			app.t.Fatal(err)
		}
	}
}

func (app schoolTestAPI) loginSeed(email string) string {
	app.t.Helper()
	var result loginResponse
	app.call("POST", "/auth/login", "", map[string]string{"email": email, "password": "admin123"}, 200, &result)
	return result.AccessToken
}

func TestEventAudiencePostgres(t *testing.T) {
	app := openSchoolTestAPI(t, "SISGES_TEST_DATABASE_URL")
	admin := app.loginSeed("adm0001@sisges.com")
	teacher := app.loginSeed("ana.clara.ferreira@sisges.local")
	student := app.loginSeed("alice.almeida@sisges.local")
	now := time.Now().In(eventLocation)
	end := time.Date(now.Year()+1, 1, 1, 0, 0, 0, 0, eventLocation).Add(-time.Microsecond)
	body := map[string]any{"title": "Reunião dos professores", "audience": "TEACHERS", "eventAt": end}
	var created eventResponse
	app.call("POST", "/events", student, body, 403, nil)
	app.call("POST", "/events", admin, body, 201, &created)
	t.Cleanup(func() {
		_, err := app.db.Exec(context.Background(), "DELETE FROM sisges.school_event WHERE id=$1", created.ID)
		if err != nil {
			t.Error(err)
		}
	})
	visible := func(token string) bool {
		t.Helper()
		var list []eventResponse
		app.call("GET", "/events", token, nil, 200, &list)
		for _, event := range list {
			if event.ID == created.ID {
				return true
			}
		}
		return false
	}
	if !visible(teacher) || visible(student) {
		t.Fatal("teachers-only event has incorrect audience")
	}
	app.call("DELETE", "/events/"+strconv.Itoa(created.ID), admin, nil, 204, nil)
	if visible(teacher) {
		t.Fatal("deleted event still visible")
	}
}

// Year closure writes results for the entire school. The full-suite runner
// supplies a fresh database exclusively for this scenario.
func TestAcademicCycleClosurePostgres(t *testing.T) {
	app := openSchoolTestAPI(t, "SISGES_TEST_CYCLE_DATABASE_URL")
	admin := app.loginSeed("adm0001@sisges.com")
	password := map[string]string{"password": "admin123"}
	var cycle cycleResponse
	app.call("GET", "/academic-cycle", admin, nil, 200, &cycle)
	if cycle.Status != "NOT_STARTED" {
		t.Fatal("expected a fresh cycle test database")
	}
	end := time.Now().AddDate(1, 0, 0).Format("2006-01-02")
	app.call("POST", "/academic-cycle/start-year", admin, map[string]string{"yearEndDate": end}, 200, &cycle)
	if cycle.Status != "IN_PROGRESS" || !cycle.GradingLocked || cycle.CurrentTrimester != 1 {
		t.Fatalf("unexpected started cycle: %+v", cycle)
	}
	app.call("POST", "/academic-cycle/end-trimester", admin, map[string]string{"password": "incorrect"}, 400, nil)
	for _, want := range []int{2, 3} {
		app.call("POST", "/academic-cycle/end-trimester", admin, password, 200, &cycle)
		if cycle.CurrentTrimester != want {
			t.Fatalf("trimester %d, want %d", cycle.CurrentTrimester, want)
		}
	}
	app.call("POST", "/academic-cycle/end-year", admin, password, 200, &cycle)
	if cycle.Status != "FINISHED" || cycle.YearFinishedAt == nil {
		t.Fatalf("unexpected closing result: %+v", cycle)
	}
	app.call("POST", "/academic-cycle/end-year", admin, password, 400, nil)
	var results, enrolled int
	err := app.db.QueryRow(t.Context(), `SELECT (SELECT count(*) FROM sisges.student_year_result),
        (SELECT count(*) FROM sisges.student WHERE class_id IS NOT NULL AND deleted_at IS NULL)`).Scan(&results, &enrolled)
	if err != nil {
		t.Fatal(err)
	}
	if results == 0 || results != enrolled {
		t.Fatalf("closure stored %d results for %d students", results, enrolled)
	}
	verifyNextAcademicYear(t, app, admin, cycle)
}

func TestGuardianDirectoryPostgres(t *testing.T) {
	app := openSchoolTestAPI(t, "SISGES_TEST_DATABASE_URL")
	filters := map[string]string{"name": "ADRIANA", "email": "adriana.almeida001@sisges.local"}
	app.call("POST", "/responsibles/search", app.loginSeed("alice.almeida@sisges.local"), filters, 403, nil)
	var result []responsibleResponse
	app.call("POST", "/responsibles/search", app.loginSeed("adm0001@sisges.com"), filters, 200, &result)
	if len(result) != 1 || result[0].ID <= 0 || result[0].Name != "Adriana Almeida" || result[0].Email != filters["email"] {
		t.Fatalf("incorrect guardian search results: %+v", result)
	}
}

func TestLegacyLessonUpdatePostgres(t *testing.T) {
	app := openSchoolTestAPI(t, "SISGES_TEST_DATABASE_URL")
	ctx := t.Context()
	email := "ana.clara.ferreira@sisges.local"
	teacher := app.loginSeed(email)
	var teacherID, disciplineID, classID int
	err := app.db.QueryRow(ctx, `SELECT t.id,cd.discipline_id,tc.class_id
		FROM sisges.teacher t JOIN sisges.users u ON u.id=t.user_id
		JOIN sisges.teacher_class tc ON tc.teacher_id=t.id AND tc.deleted_at IS NULL
		JOIN sisges.class_discipline cd ON cd.class_id=tc.class_id AND cd.deleted_at IS NULL
		WHERE u.email=$1 ORDER BY tc.class_id,cd.discipline_id LIMIT 1`, email).
		Scan(&teacherID, &disciplineID, &classID)
	if err != nil {
		t.Fatal(err)
	}
	var lessonID int
	err = app.db.QueryRow(ctx, `INSERT INTO sisges.class_meeting(class_id,teacher_id,discipline_id,meeting_date,start_time,end_time)
		VALUES($1,$2,$3,'2014-05-12','08:00','08:50') RETURNING id`, classID, teacherID, disciplineID).Scan(&lessonID)
	if err != nil {
		t.Fatal(err)
	}
	defer app.db.Exec(ctx, "DELETE FROM sisges.class_meeting WHERE id=$1", lessonID)

	date := "2014-05-13"
	var updated meetingDetail
	app.call("PUT", "/class/update/"+strconv.Itoa(lessonID), teacher, meetingRequest{
		ClassID: classID, DisciplineID: disciplineID, Date: date, StartTime: "09:00", EndTime: "09:50",
	}, 200, &updated)
	if updated.ID != lessonID || updated.Teacher == nil || updated.Teacher.ID != teacherID ||
		updated.DisciplineID != disciplineID || updated.Date != date || updated.StartTime != "09:00:00" {
		t.Fatalf("Dados incorretos após a edição da aula: %+v", updated)
	}
	var found []meetingSearch
	app.call("POST", "/class/search", teacher, map[string]any{"date": date, "classId": classID}, 200, &found)
	if len(found) != 1 || found[0].ID != lessonID || found[0].EndTime != "09:50:00" {
		t.Fatalf("A consulta não retornou a aula atualizada: %+v", found)
	}
}

func TestListsWithoutSchoolProfilePostgres(t *testing.T) {
	app := openSchoolTestAPI(t, "SISGES_TEST_DATABASE_URL")
	ctx := t.Context()
	var classID, teacherID, disciplineID int
	err := app.db.QueryRow(ctx, `SELECT tc.class_id,tc.teacher_id,cd.discipline_id
		FROM sisges.teacher_class tc JOIN sisges.class_discipline cd ON cd.class_id=tc.class_id
		WHERE tc.deleted_at IS NULL AND cd.deleted_at IS NULL LIMIT 1`).Scan(&classID, &teacherID, &disciplineID)
	if err != nil {
		t.Fatal(err)
	}
	var materialID, lessonID int
	err = app.db.QueryRow(ctx, `INSERT INTO sisges.discipline_material(class_id,discipline_id,teacher_id,title)
		VALUES($1,$2,$3,'Leitura da turma') RETURNING id`, classID, disciplineID, teacherID).Scan(&materialID)
	if err != nil {
		t.Fatal(err)
	}
	defer app.db.Exec(ctx, "DELETE FROM sisges.discipline_material WHERE id=$1", materialID)
	err = app.db.QueryRow(ctx, `INSERT INTO sisges.class_meeting(class_id,discipline_id,teacher_id,meeting_date,start_time,end_time)
		VALUES($1,$2,$3,'2014-06-10','08:00','08:50') RETURNING id`, classID, disciplineID, teacherID).Scan(&lessonID)
	if err != nil {
		t.Fatal(err)
	}
	defer app.db.Exec(ctx, "DELETE FROM sisges.class_meeting WHERE id=$1", lessonID)

	for _, role := range []string{"STUDENT", "TEACHER"} {
		t.Run(role, func(t *testing.T) {
			register := role + strconv.FormatInt(time.Now().UnixNano(), 10)
			email := register + "@sisges.local"
			var id int
			err := app.db.QueryRow(ctx, `INSERT INTO sisges.users(name,email,register,password,birth_date,gender,user_role)
				SELECT 'Conta sem vínculo',$1,$2,password,birth_date,gender,$3 FROM sisges.users
				WHERE email='adm0001@sisges.com' RETURNING id`, email, register, role).Scan(&id)
			if err != nil {
				t.Fatal(err)
			}
			defer app.db.Exec(ctx, "DELETE FROM sisges.users WHERE id=$1", id)
			client := schoolTestAPI{App: app.App, t: t}
			token := client.loginSeed(email)
			var materials []materialResponse
			client.call("GET", "/materials?classId="+strconv.Itoa(classID), token, nil, 200, &materials)
			if materials == nil || len(materials) != 0 {
				t.Fatalf("Esperava uma lista vazia de materiais, recebeu %+v", materials)
			}
			if role == "TEACHER" {
				var lessons []meetingSearch
				client.call("POST", "/class/search", token, nil, 200, &lessons)
				if lessons == nil || len(lessons) != 0 {
					t.Fatalf("Esperava uma lista vazia de aulas, recebeu %+v", lessons)
				}
			}
		})
	}
}
