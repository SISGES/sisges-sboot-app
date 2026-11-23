package api

import (
	"fmt"
	"testing"
	"time"
)

// Continuação no banco dedicado da suíte, depois do encerramento do primeiro ano.
func verifyNextAcademicYear(t *testing.T, app schoolTestAPI, admin string, previous cycleResponse) {
	t.Helper()
	ctx := t.Context()
	var cfg gradingConfig
	app.call("GET", "/grading-config", admin, nil, 200, &cfg)
	cfg.YearMinPercentage = 64
	cfg.Trimester1MinPercentage = 55
	app.call("PUT", "/grading-config", admin, cfg, 200, nil)
	var c cycleResponse
	app.call("POST", "/academic-cycle/start-year", admin, map[string]any{
		"yearEndDate": time.Now().AddDate(1, 0, 0).Format("2006-01-02"), "expectedCycleId": previous.ID,
	}, 200, &c)
	if c.ID == previous.ID || !c.GradingLocked {
		t.Fatal("o segundo ano reutilizou o ciclo anterior ou deixou de bloquear a política")
	}
	app.call("PUT", "/grading-config", admin, cfg, 400, nil)
	adminClose := map[string]any{"password": "admin123", "expectedCycleId": c.ID, "expectedTrimester": 1}
	app.call("POST", "/academic-cycle/end-year", admin, map[string]any{"password": "admin123", "expectedCycleId": previous.ID}, 400, nil)
	var sid, from, to, meeting int
	var email string
	if err := app.db.QueryRow(ctx, `SELECT s.id,s.class_id,u.email FROM sisges.student s JOIN sisges.users u ON u.id=s.user_id WHERE u.register='ALU0002'`).Scan(&sid, &from, &email); err != nil {
		t.Fatal(err)
	}
	if err := app.db.QueryRow(ctx, `SELECT id FROM sisges.school_class WHERE academic_year='6º ano' AND id<>$1 ORDER BY id LIMIT 1`, from).Scan(&to); err != nil {
		t.Fatal(err)
	}
	if err := app.db.QueryRow(ctx, `INSERT INTO sisges.class_meeting(class_id,discipline_id,meeting_date,start_time,end_time)
		SELECT $1,min(id),(now() AT TIME ZONE 'America/Sao_Paulo')::date,'07:00','07:50' FROM sisges.discipline RETURNING id`, from).Scan(&meeting); err != nil {
		t.Fatal(err)
	}
	student := app.loginSeed(email)
	var activity activityResponse
	app.call("POST", "/activities", admin, map[string]any{"classMeetingId": meeting, "title": "Avaliação de progressão", "maxPoints": 65}, 201, &activity)
	app.call("POST", "/academic-cycle/end-year", admin, adminClose, 400, nil) // atividade sem professor também bloqueia
	prefix := fmt.Sprintf("/activities/%d", activity.ID)
	app.call("PUT", prefix+"/grades", admin, map[string]any{"entries": []map[string]any{{"studentId": sid, "score": 64}}}, 200, nil)
	app.call("POST", prefix+"/release", admin, nil, 200, nil)
	app.call("DELETE", prefix, admin, nil, 400, nil)
	app.call("POST", fmt.Sprintf("/classes/%d/student/add/%d", to, sid), admin, nil, 200, nil)
	var report studentReportResponse
	app.call("GET", "/boletim/me", student, nil, 200, &report)
	if report.TotalReleasedScore != 64 || report.FixedApprovalPercentage != 64 || report.CycleID != c.ID || report.Trimesters[0].MinPercentage != 55 {
		t.Fatalf("boletim após transferência: %+v", report)
	}
	// Falta uma turma de destino. O erro deve reverter resultados de TODOS os alunos.
	app.call("POST", "/academic-cycle/end-year", admin, adminClose, 400, nil)
	var rows int
	if err := app.db.QueryRow(ctx, `SELECT count(*) FROM sisges.student_year_result WHERE academic_cycle_id=$1`, c.ID).Scan(&rows); err != nil || rows != 0 {
		t.Fatalf("fechamento parcial: %d, %v", rows, err)
	}
	if _, err := app.db.Exec(ctx, `INSERT INTO sisges.school_class(name,academic_year) VALUES('Turma de destino','7º ano')`); err != nil {
		t.Fatal(err)
	}
	app.call("POST", "/academic-cycle/end-year", admin, adminClose, 200, &c)
	if c.GradingLocked || c.Status != "FINISHED" {
		t.Fatalf("estado após encerramento: %+v", c)
	}
	cfg.YearMinPercentage = 82
	app.call("PUT", "/grading-config", admin, cfg, 200, nil)
	app.call("GET", "/boletim/me", student, nil, 200, &report)
	if report.YearResult == nil || !report.YearResult.Promoted || !report.YearResult.Approved || report.YearResult.FinalScore != 64 || report.FixedApprovalPercentage != 64 {
		t.Fatalf("resultado do ano encerrado não preservou a política: %+v", report)
	}
	app.call("GET", fmt.Sprintf("/boletim/me?cycleId=%d", previous.ID), student, nil, 200, &report)
	if report.CycleID != previous.ID || report.TotalReleasedScore != 0 || report.YearResult == nil || report.YearResult.Approved || report.FixedApprovalPercentage != 60 || len(report.AvailableCycles) != 2 {
		t.Fatalf("histórico de ciclos misturado: %+v", report)
	}
	app.call("GET", "/boletim/me?cycleId=999999", student, nil, 404, nil)
}
