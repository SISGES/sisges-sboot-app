package api

import (
	"context"
	"fmt"
	"testing"
)

// A draft activity is the fixture. Every grade change and student report read
// goes through the HTTP router, including authentication and database writes.
func TestGradePublicationPostgres(t *testing.T) {
	app := openSchoolTestAPI(t, "SISGES_TEST_DATABASE_URL")
	ctx := t.Context()
	var classID, studentID, teacherID, disciplineID int
	var studentEmail, teacherEmail string
	err := app.db.QueryRow(ctx, `SELECT s.class_id,s.id,t.id,cd.discipline_id,su.email,tu.email
		FROM sisges.student s JOIN sisges.users su ON su.id=s.user_id
		JOIN sisges.teacher_class tc ON tc.class_id=s.class_id AND tc.deleted_at IS NULL
		JOIN sisges.teacher t ON t.id=tc.teacher_id JOIN sisges.users tu ON tu.id=t.user_id
		JOIN sisges.class_discipline cd ON cd.class_id=s.class_id AND cd.deleted_at IS NULL
		WHERE su.register='ALU0001' AND tu.register='PRO0001' ORDER BY cd.discipline_id LIMIT 1`).
		Scan(&classID, &studentID, &teacherID, &disciplineID, &studentEmail, &teacherEmail)
	if err != nil {
		t.Fatal(err)
	}
	student, teacher := app.loginSeed(studentEmail), app.loginSeed(teacherEmail)
	openGradeTestCycle(t, app)
	var meeting int
	err = app.db.QueryRow(ctx, `INSERT INTO sisges.class_meeting(class_id,teacher_id,discipline_id,meeting_date,start_time,end_time)
		VALUES($1,$2,$3,current_date,'08:00','08:50') RETURNING id`, classID, teacherID, disciplineID).Scan(&meeting)
	if err != nil {
		t.Fatal(err)
	}
	defer app.db.Exec(ctx, "DELETE FROM sisges.class_meeting WHERE id=$1", meeting)
	var created activityResponse
	app.call("POST", "/activities", teacher, map[string]any{"classMeetingId": meeting, "title": "Prova E2E", "activityType": "PROVA", "trimesterNumber": 1, "maxPoints": 10}, 201, &created)
	activity := created.ID
	prefix := fmt.Sprintf("/activities/%d", activity)
	grades := func(score float64) map[string]any {
		return map[string]any{"entries": []map[string]any{{"studentId": studentID, "score": score}}}
	}
	report := func() (reportActivity, float64) {
		t.Helper()
		var result studentReportResponse
		app.call("GET", "/boletim/me", student, nil, 200, &result)
		for _, trimester := range result.Trimesters {
			for _, cell := range trimester.Activities {
				if cell.ActivityID == activity {
					return cell, result.TotalReleasedScore
				}
			}
		}
		t.Fatalf("activity %d missing from student's report", activity)
		return reportActivity{}, 0
	}

	app.call("GET", prefix+"/gradebook", student, nil, 403, nil)
	app.call("PUT", prefix+"/grades", teacher, grades(11), 400, nil)
	app.call("PUT", prefix+"/grades", teacher, grades(8.5), 200, nil)
	draft, baseline := report()
	if draft.Released || draft.Score != nil {
		t.Fatalf("draft score visible: %+v", draft)
	}
	app.call("POST", prefix+"/release", teacher, nil, 200, nil)
	cell, total := report()
	if !cell.Released || cell.Score == nil || *cell.Score != 8.5 || total != baseline+8.5 {
		t.Fatalf("released score or total wrong: %+v, %v", cell, total)
	}
	app.call("PUT", prefix+"/grades", teacher, grades(7), 400, nil)
	app.call("POST", prefix+"/release", teacher, nil, 200, nil)
	if _, total := report(); total != baseline+8.5 {
		t.Fatalf("repeated release changed total to %v", total)
	}
}

// O banco geral da suíte é compartilhado entre os cenários. Este ciclo tem
// identidade própria e os registros que o utilizam são removidos pelo cenário.
func openGradeTestCycle(t *testing.T, app schoolTestAPI) {
	t.Helper()
	ctx := t.Context()
	tx, err := app.db.Begin(ctx)
	if err != nil {
		t.Fatal(err)
	}
	defer tx.Rollback(ctx)
	var prev, id int
	if err = tx.QueryRow(ctx, `UPDATE sisges.academic_cycle SET is_current=false WHERE is_current RETURNING id`).Scan(&prev); err != nil {
		t.Fatal(err)
	}
	if err = tx.QueryRow(ctx, `INSERT INTO sisges.academic_cycle(status,year_start_date,year_end_date,is_current,grading_locked,grading_policy)
        SELECT 'IN_PROGRESS',current_date-7,current_date+7,true,true,to_jsonb(g) FROM sisges.grading_config g ORDER BY id LIMIT 1 RETURNING id`).Scan(&id); err != nil {
		t.Fatal(err)
	}
	if err = tx.Commit(ctx); err != nil {
		t.Fatal(err)
	}
	t.Cleanup(func() {
		tx, err := app.db.Begin(context.Background())
		if err != nil {
			t.Error(err)
			return
		}
		defer tx.Rollback(context.Background())
		if _, err = tx.Exec(context.Background(), "DELETE FROM sisges.academic_cycle WHERE id=$1", id); err != nil {
			t.Error(err)
			return
		}
		if _, err = tx.Exec(context.Background(), "UPDATE sisges.academic_cycle SET is_current=true WHERE id=$1", prev); err != nil {
			t.Error(err)
			return
		}
		if err = tx.Commit(context.Background()); err != nil {
			t.Error(err)
		}
	})
}
