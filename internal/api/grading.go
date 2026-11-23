package api

import (
	"context"
	"fmt"
	"net/http"
	"strconv"
	"strings"
	"time"

	"github.com/jackc/pgx/v5"
	"golang.org/x/crypto/bcrypt"
)

type gradingConfig struct {
	YearMaxPoints              int     `json:"yearMaxPoints"`
	YearMinPercentage          float64 `json:"yearMinPercentage"`
	Trimester1MaxPoints        int     `json:"trimester1MaxPoints"`
	Trimester1MinPercentage    float64 `json:"trimester1MinPercentage"`
	Trimester2MaxPoints        int     `json:"trimester2MaxPoints"`
	Trimester2MinPercentage    float64 `json:"trimester2MinPercentage"`
	Trimester3MaxPoints        int     `json:"trimester3MaxPoints"`
	Trimester3MinPercentage    float64 `json:"trimester3MinPercentage"`
	Trimester1PointsProvas     int     `json:"trimester1PointsProvas"`
	Trimester1PointsAtividades int     `json:"trimester1PointsAtividades"`
	Trimester1PointsTrabalhos  int     `json:"trimester1PointsTrabalhos"`
	Trimester2PointsProvas     int     `json:"trimester2PointsProvas"`
	Trimester2PointsAtividades int     `json:"trimester2PointsAtividades"`
	Trimester2PointsTrabalhos  int     `json:"trimester2PointsTrabalhos"`
	Trimester3PointsProvas     int     `json:"trimester3PointsProvas"`
	Trimester3PointsAtividades int     `json:"trimester3PointsAtividades"`
	Trimester3PointsTrabalhos  int     `json:"trimester3PointsTrabalhos"`
}

type schoolQuery interface {
	Query(context.Context, string, ...any) (pgx.Rows, error)
	QueryRow(context.Context, string, ...any) pgx.Row
}

func gradingForCycle(ctx context.Context, db schoolQuery, cycle int) (gradingConfig, error) {
	var c gradingConfig
	err := db.QueryRow(ctx, `SELECT year_max_points,year_min_percentage::float8,trimester1_max_points,trimester1_min_percentage::float8,trimester2_max_points,trimester2_min_percentage::float8,trimester3_max_points,trimester3_min_percentage::float8,trimester1_points_provas,trimester1_points_atividades,trimester1_points_trabalhos,trimester2_points_provas,trimester2_points_atividades,trimester2_points_trabalhos,trimester3_points_provas,trimester3_points_atividades,trimester3_points_trabalhos
        FROM jsonb_populate_record(NULL::sisges.grading_config, COALESCE(
            (SELECT grading_policy FROM sisges.academic_cycle WHERE id=$1 OR ($1=0 AND is_current AND grading_locked)),
            (SELECT to_jsonb(g) FROM sisges.grading_config g ORDER BY id LIMIT 1)
        ))`, cycle).Scan(&c.YearMaxPoints, &c.YearMinPercentage, &c.Trimester1MaxPoints, &c.Trimester1MinPercentage, &c.Trimester2MaxPoints, &c.Trimester2MinPercentage, &c.Trimester3MaxPoints, &c.Trimester3MinPercentage, &c.Trimester1PointsProvas, &c.Trimester1PointsAtividades, &c.Trimester1PointsTrabalhos, &c.Trimester2PointsProvas, &c.Trimester2PointsAtividades, &c.Trimester2PointsTrabalhos, &c.Trimester3PointsProvas, &c.Trimester3PointsAtividades, &c.Trimester3PointsTrabalhos)
	return c, err
}

// As alterações de ano/configuração têm exclusividade; os lançamentos podem
// concorrer entre si e obtêm também o bloqueio da atividade que modificam.
func (a *App) academicTx(ctx context.Context, exclusive bool) (pgx.Tx, error) {
	tx, err := a.db.Begin(ctx)
	if err != nil {
		return nil, err
	}
	lock := "SELECT pg_advisory_xact_lock_shared(83801,25)"
	if exclusive {
		lock = "SELECT pg_advisory_xact_lock(83801,25)"
	}
	if _, err = tx.Exec(ctx, lock); err != nil {
		tx.Rollback(ctx)
		return nil, err
	}
	return tx, nil
}
func (a *App) getGradingConfig(w http.ResponseWriter, r *http.Request) {
	c, err := gradingForCycle(r.Context(), a.db, 0)
	if err != nil {
		writeError(w, internalError(err))
		return
	}
	writeJSON(w, http.StatusOK, c)
}
func (a *App) updateGradingConfig(w http.ResponseWriter, r *http.Request) {
	var c gradingConfig
	if !decodeJSON(w, r, &c) {
		return
	}
	for _, percentage := range []float64{c.YearMinPercentage, c.Trimester1MinPercentage, c.Trimester2MinPercentage, c.Trimester3MinPercentage} {
		if percentage < 0 || percentage > 100 {
			writeError(w, validationError("minPercentage", "O percentual deve estar entre 0 e 100."))
			return
		}
	}
	if c.YearMaxPoints != c.Trimester1MaxPoints+c.Trimester2MaxPoints+c.Trimester3MaxPoints {
		writeError(w, businessError(fmt.Sprintf("A soma dos pontos dos trimestres deve ser igual ao total do ano letivo (%d).", c.YearMaxPoints)))
		return
	}
	comps := [][4]int{{c.Trimester1MaxPoints, c.Trimester1PointsProvas, c.Trimester1PointsAtividades, c.Trimester1PointsTrabalhos}, {c.Trimester2MaxPoints, c.Trimester2PointsProvas, c.Trimester2PointsAtividades, c.Trimester2PointsTrabalhos}, {c.Trimester3MaxPoints, c.Trimester3PointsProvas, c.Trimester3PointsAtividades, c.Trimester3PointsTrabalhos}}
	for i, x := range comps {
		if x[1] < 1 || x[2] < 1 || x[3] < 1 || x[0] != x[1]+x[2]+x[3] {
			writeError(w, businessError(fmt.Sprintf("No %dº trimestre, a composição deve ser positiva e somar a pontuação máxima.", i+1)))
			return
		}
	}
	tx, err := a.academicTx(r.Context(), true)
	if err != nil {
		writeError(w, err)
		return
	}
	defer tx.Rollback(r.Context())
	cycle, err := currentCycle(r.Context(), tx)
	if err != nil {
		writeError(w, err)
		return
	}
	if cycle.GradingLocked {
		writeError(w, businessError("A configuração está bloqueada durante o ano letivo."))
		return
	}
	_, err = tx.Exec(r.Context(), `UPDATE sisges.grading_config SET year_max_points=$1,year_min_percentage=$2,trimester1_max_points=$3,trimester1_min_percentage=$4,trimester2_max_points=$5,trimester2_min_percentage=$6,trimester3_max_points=$7,trimester3_min_percentage=$8,trimester1_points_provas=$9,trimester1_points_atividades=$10,trimester1_points_trabalhos=$11,trimester2_points_provas=$12,trimester2_points_atividades=$13,trimester2_points_trabalhos=$14,trimester3_points_provas=$15,trimester3_points_atividades=$16,trimester3_points_trabalhos=$17,updated_at=now() WHERE id=(SELECT id FROM sisges.grading_config ORDER BY id LIMIT 1)`, c.YearMaxPoints, c.YearMinPercentage, c.Trimester1MaxPoints, c.Trimester1MinPercentage, c.Trimester2MaxPoints, c.Trimester2MinPercentage, c.Trimester3MaxPoints, c.Trimester3MinPercentage, c.Trimester1PointsProvas, c.Trimester1PointsAtividades, c.Trimester1PointsTrabalhos, c.Trimester2PointsProvas, c.Trimester2PointsAtividades, c.Trimester2PointsTrabalhos, c.Trimester3PointsProvas, c.Trimester3PointsAtividades, c.Trimester3PointsTrabalhos)
	if err != nil {
		writeError(w, internalError(err))
		return
	}
	if err = tx.Commit(r.Context()); err != nil {
		writeError(w, err)
		return
	}
	writeJSON(w, http.StatusOK, c)
}

type cycleResponse struct {
	ID               int        `json:"id"`
	Status           string     `json:"status"`
	YearStartDate    *string    `json:"yearStartDate"`
	YearEndDate      *string    `json:"yearEndDate"`
	CurrentTrimester int        `json:"currentTrimester"`
	GradingLocked    bool       `json:"gradingLocked"`
	YearStartedAt    *time.Time `json:"yearStartedAt"`
	YearFinishedAt   *time.Time `json:"yearFinishedAt"`
}

func currentCycle(ctx context.Context, db schoolQuery) (cycleResponse, error) {
	var c cycleResponse
	err := db.QueryRow(ctx, `SELECT id,status,to_char(year_start_date,'YYYY-MM-DD'),to_char(year_end_date,'YYYY-MM-DD'),current_trimester,grading_locked,year_started_at,year_finished_at FROM sisges.academic_cycle WHERE is_current`).Scan(&c.ID, &c.Status, &c.YearStartDate, &c.YearEndDate, &c.CurrentTrimester, &c.GradingLocked, &c.YearStartedAt, &c.YearFinishedAt)
	return c, err
}
func (a *App) getAcademicCycle(w http.ResponseWriter, r *http.Request) {
	c, err := currentCycle(r.Context(), a.db)
	if err != nil {
		writeError(w, err)
		return
	}
	writeJSON(w, 200, c)
}
func (a *App) startAcademicYear(w http.ResponseWriter, r *http.Request) {
	var req struct {
		YearEndDate     string `json:"yearEndDate"`
		ExpectedCycleID *int   `json:"expectedCycleId"`
	}
	if !decodeJSON(w, r, &req) {
		return
	}
	ctx := r.Context()
	today := time.Now().In(eventLocation).Format("2006-01-02")
	if _, err := time.Parse("2006-01-02", req.YearEndDate); err != nil || req.YearEndDate <= today {
		writeError(w, businessError("A data final deve ser maior que hoje."))
		return
	}
	tx, err := a.academicTx(ctx, true)
	if err != nil {
		writeError(w, err)
		return
	}
	defer tx.Rollback(ctx)
	c, err := currentCycle(ctx, tx)
	if err != nil {
		writeError(w, err)
		return
	}
	if c.Status == "IN_PROGRESS" || (req.ExpectedCycleID != nil && *req.ExpectedCycleID != c.ID) {
		writeError(w, businessError("O ciclo mudou ou já está em andamento. Atualize os dados."))
		return
	}
	if c.Status == "FINISHED" {
		if _, err = tx.Exec(ctx, `UPDATE sisges.academic_cycle SET is_current=false WHERE id=$1`, c.ID); err != nil {
			writeError(w, err)
			return
		}
		if err = tx.QueryRow(ctx, `INSERT INTO sisges.academic_cycle(status,is_current) VALUES('NOT_STARTED',true) RETURNING id`).Scan(&c.ID); err != nil {
			writeError(w, err)
			return
		}
	}
	_, err = tx.Exec(ctx, `UPDATE sisges.academic_cycle SET status='IN_PROGRESS',year_start_date=$2::text::date,year_end_date=$3::text::date,grading_locked=true,year_started_at=now(),updated_at=now(),grading_policy=(SELECT to_jsonb(g) FROM sisges.grading_config g ORDER BY id LIMIT 1) WHERE id=$1`, c.ID, today, req.YearEndDate)
	if err != nil {
		writeError(w, err)
		return
	}
	sendCycle(w, r, tx)
}

func sendCycle(w http.ResponseWriter, r *http.Request, tx pgx.Tx) {
	c, err := currentCycle(r.Context(), tx)
	if err == nil {
		err = tx.Commit(r.Context())
	}
	if err != nil {
		writeError(w, err)
		return
	}
	writeJSON(w, 200, c)
}
func (a *App) validateAdminPassword(r *http.Request, password string) error {
	if strings.TrimSpace(password) == "" {
		return businessError("Senha do administrador é obrigatória.")
	}
	var hash string
	if e := a.db.QueryRow(r.Context(), `SELECT password FROM sisges.users WHERE id=$1 AND user_role='ADMIN' AND deleted_at IS NULL`, currentPrincipal(r).ID).Scan(&hash); e != nil {
		return forbidden()
	}
	if bcrypt.CompareHashAndPassword([]byte(hash), []byte(password)) != nil {
		return businessError("Senha inválida.")
	}
	return nil
}
func pendingTeacherNames(ctx context.Context, db schoolQuery, cycleID int, trimester *int) ([]string, error) {
	rows, err := db.Query(ctx, `SELECT DISTINCT COALESCE(u.name,'Aula sem professor identificado') AS name
        FROM sisges.evaluative_activity ea JOIN sisges.class_meeting cm ON cm.id=ea.class_meeting_id
        LEFT JOIN sisges.teacher t ON t.id=cm.teacher_id LEFT JOIN sisges.users u ON u.id=t.user_id
        WHERE ea.academic_cycle_id=$1 AND ea.deleted_at IS NULL AND cm.deleted_at IS NULL AND NOT ea.released
        AND ($2::int IS NULL OR ea.trimester_number=$2) ORDER BY name`, cycleID, trimester)
	if err != nil {
		return nil, err
	}
	return pgx.CollectRows(rows, pgx.RowTo[string])
}
func (a *App) pendingReleases(w http.ResponseWriter, r *http.Request) {
	var tp *int
	if s := r.URL.Query().Get("trimester"); s != "" {
		x, err := strconv.Atoi(s)
		if err != nil || x < 1 || x > 3 {
			writeError(w, validationError("trimester", "Trimestre inválido"))
			return
		}
		tp = &x
	}
	c, err := currentCycle(r.Context(), a.db)
	if err != nil {
		writeError(w, err)
		return
	}
	names, err := pendingTeacherNames(r.Context(), a.db, c.ID, tp)
	if err != nil {
		writeError(w, err)
		return
	}
	writeJSON(w, 200, struct {
		Trimester *int     `json:"trimester"`
		Teachers  []string `json:"teachers"`
	}{tp, names})
}
func (a *App) endTrimester(w http.ResponseWriter, r *http.Request) { a.closePeriod(w, r, false) }
func (a *App) endYear(w http.ResponseWriter, r *http.Request)      { a.closePeriod(w, r, true) }

func (a *App) closePeriod(w http.ResponseWriter, r *http.Request, year bool) {
	var req struct {
		Password          string `json:"password"`
		ExpectedCycleID   *int   `json:"expectedCycleId"`
		ExpectedTrimester *int   `json:"expectedTrimester"`
	}
	if !decodeJSON(w, r, &req) {
		return
	}
	if err := a.validateAdminPassword(r, req.Password); err != nil {
		writeError(w, err)
		return
	}
	ctx := r.Context()
	tx, err := a.academicTx(ctx, true)
	if err != nil {
		writeError(w, err)
		return
	}
	defer tx.Rollback(ctx)
	c, err := currentCycle(ctx, tx)
	if err != nil {
		writeError(w, err)
		return
	}
	if c.Status != "IN_PROGRESS" || (req.ExpectedCycleID != nil && *req.ExpectedCycleID != c.ID) || (req.ExpectedTrimester != nil && *req.ExpectedTrimester != c.CurrentTrimester) {
		writeError(w, businessError("O período letivo mudou ou não está em andamento. Atualize os dados."))
		return
	}
	tp := &c.CurrentTrimester
	if year {
		tp = nil
	}
	names, err := pendingTeacherNames(ctx, tx, c.ID, tp)
	if err != nil {
		writeError(w, err)
		return
	}
	if len(names) > 0 {
		writeError(w, businessError("Ainda existem notas não liberadas: "+strings.Join(names, ", ")))
		return
	}
	if year {
		if err = processYearEnd(ctx, tx, c.ID); err != nil {
			writeError(w, err)
			return
		}
		_, err = tx.Exec(ctx, `UPDATE sisges.academic_cycle SET status='FINISHED',grading_locked=false,year_finished_at=now(),updated_at=now() WHERE id=$1`, c.ID)
	} else {
		_, err = tx.Exec(ctx, `UPDATE sisges.academic_cycle SET current_trimester=LEAST(3,current_trimester+1),updated_at=now() WHERE id=$1`, c.ID)
	}
	if err != nil {
		writeError(w, err)
		return
	}
	sendCycle(w, r, tx)
}
