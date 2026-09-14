package api

import (
	"fmt"
	"net/http"
	"strings"
	"time"

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

func (a *App) readGrading(r *http.Request) (gradingConfig, error) {
	var c gradingConfig
	err := a.db.QueryRow(r.Context(), `SELECT year_max_points,year_min_percentage::float8,trimester1_max_points,trimester1_min_percentage::float8,trimester2_max_points,trimester2_min_percentage::float8,trimester3_max_points,trimester3_min_percentage::float8,trimester1_points_provas,trimester1_points_atividades,trimester1_points_trabalhos,trimester2_points_provas,trimester2_points_atividades,trimester2_points_trabalhos,trimester3_points_provas,trimester3_points_atividades,trimester3_points_trabalhos FROM sisges.grading_config ORDER BY id LIMIT 1`).Scan(&c.YearMaxPoints, &c.YearMinPercentage, &c.Trimester1MaxPoints, &c.Trimester1MinPercentage, &c.Trimester2MaxPoints, &c.Trimester2MinPercentage, &c.Trimester3MaxPoints, &c.Trimester3MinPercentage, &c.Trimester1PointsProvas, &c.Trimester1PointsAtividades, &c.Trimester1PointsTrabalhos, &c.Trimester2PointsProvas, &c.Trimester2PointsAtividades, &c.Trimester2PointsTrabalhos, &c.Trimester3PointsProvas, &c.Trimester3PointsAtividades, &c.Trimester3PointsTrabalhos)
	return c, err
}
func (a *App) getGradingConfig(w http.ResponseWriter, r *http.Request) {
	c, err := a.readGrading(r)
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
	_, err := a.db.Exec(r.Context(), `UPDATE sisges.grading_config SET year_max_points=$1,year_min_percentage=$2,trimester1_max_points=$3,trimester1_min_percentage=$4,trimester2_max_points=$5,trimester2_min_percentage=$6,trimester3_max_points=$7,trimester3_min_percentage=$8,trimester1_points_provas=$9,trimester1_points_atividades=$10,trimester1_points_trabalhos=$11,trimester2_points_provas=$12,trimester2_points_atividades=$13,trimester2_points_trabalhos=$14,trimester3_points_provas=$15,trimester3_points_atividades=$16,trimester3_points_trabalhos=$17,updated_at=now() WHERE id=(SELECT id FROM sisges.grading_config ORDER BY id LIMIT 1)`, c.YearMaxPoints, c.YearMinPercentage, c.Trimester1MaxPoints, c.Trimester1MinPercentage, c.Trimester2MaxPoints, c.Trimester2MinPercentage, c.Trimester3MaxPoints, c.Trimester3MinPercentage, c.Trimester1PointsProvas, c.Trimester1PointsAtividades, c.Trimester1PointsTrabalhos, c.Trimester2PointsProvas, c.Trimester2PointsAtividades, c.Trimester2PointsTrabalhos, c.Trimester3PointsProvas, c.Trimester3PointsAtividades, c.Trimester3PointsTrabalhos)
	if err != nil {
		writeError(w, internalError(err))
		return
	}
	writeJSON(w, http.StatusOK, c)
}

type cycleResponse struct {
	Status           string     `json:"status"`
	YearStartDate    *string    `json:"yearStartDate"`
	YearEndDate      *string    `json:"yearEndDate"`
	CurrentTrimester int        `json:"currentTrimester"`
	GradingLocked    bool       `json:"gradingLocked"`
	YearStartedAt    *time.Time `json:"yearStartedAt"`
	YearFinishedAt   *time.Time `json:"yearFinishedAt"`
}

func (a *App) readCycle(r *http.Request) (cycleResponse, error) {
	var c cycleResponse
	return c, a.db.QueryRow(r.Context(), `SELECT status,to_char(year_start_date,'YYYY-MM-DD'),to_char(year_end_date,'YYYY-MM-DD'),current_trimester,grading_locked,year_started_at,year_finished_at FROM sisges.academic_cycle ORDER BY id LIMIT 1`).Scan(&c.Status, &c.YearStartDate, &c.YearEndDate, &c.CurrentTrimester, &c.GradingLocked, &c.YearStartedAt, &c.YearFinishedAt)
}
func (a *App) getAcademicCycle(w http.ResponseWriter, r *http.Request) {
	c, e := a.readCycle(r)
	if e != nil {
		writeError(w, internalError(e))
		return
	}
	writeJSON(w, http.StatusOK, c)
}
func (a *App) startAcademicYear(w http.ResponseWriter, r *http.Request) {
	var req struct {
		YearEndDate string `json:"yearEndDate"`
	}
	if !decodeJSON(w, r, &req) {
		return
	}
	end, e := time.Parse("2006-01-02", req.YearEndDate)
	if e != nil || !end.After(time.Now()) {
		writeError(w, businessError("A data final deve ser maior que hoje."))
		return
	}
	tag, e := a.db.Exec(r.Context(), `UPDATE sisges.academic_cycle SET status='IN_PROGRESS',year_start_date=current_date,year_end_date=$1,current_trimester=1,grading_locked=true,year_started_at=now(),year_finished_at=NULL,updated_at=now() WHERE id=(SELECT id FROM sisges.academic_cycle ORDER BY id LIMIT 1) AND status<>'IN_PROGRESS'`, end)
	if e != nil {
		writeError(w, internalError(e))
		return
	}
	if tag.RowsAffected() == 0 {
		writeError(w, businessError("O ano letivo já foi iniciado."))
		return
	}
	a.getAcademicCycle(w, r)
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
func (a *App) pendingTeacherNames(r *http.Request, trimester *int) ([]string, error) {
	rows, e := a.db.Query(r.Context(), `SELECT DISTINCT u.name FROM sisges.evaluative_activity ea JOIN sisges.class_meeting cm ON cm.id=ea.class_meeting_id JOIN sisges.teacher t ON t.id=cm.teacher_id JOIN sisges.users u ON u.id=t.user_id WHERE ea.deleted_at IS NULL AND NOT ea.released AND ($1::int IS NULL OR ea.trimester_number=$1) ORDER BY u.name`, trimester)
	if e != nil {
		return nil, e
	}
	defer rows.Close()
	out := []string{}
	for rows.Next() {
		var s string
		if rows.Scan(&s) == nil {
			out = append(out, s)
		}
	}
	return out, rows.Err()
}
func (a *App) pendingReleases(w http.ResponseWriter, r *http.Request) {
	var tp *int
	if s := r.URL.Query().Get("trimester"); s != "" {
		var x int
		if _, e := fmt.Sscan(s, &x); e != nil || x < 1 || x > 3 {
			writeError(w, validationError("trimester", "Trimestre inválido"))
			return
		}
		tp = &x
	}
	names, e := a.pendingTeacherNames(r, tp)
	if e != nil {
		writeError(w, internalError(e))
		return
	}
	writeJSON(w, http.StatusOK, struct {
		Trimester *int     `json:"trimester"`
		Teachers  []string `json:"teachers"`
	}{tp, names})
}
func (a *App) endTrimester(w http.ResponseWriter, r *http.Request) {
	var req struct {
		Password string `json:"password"`
	}
	if !decodeJSON(w, r, &req) {
		return
	}
	if e := a.validateAdminPassword(r, req.Password); e != nil {
		writeError(w, e)
		return
	}
	c, e := a.readCycle(r)
	if e != nil || c.Status != "IN_PROGRESS" {
		writeError(w, businessError("O ano letivo não está em andamento."))
		return
	}
	t := c.CurrentTrimester
	names, e := a.pendingTeacherNames(r, &t)
	if e != nil {
		writeError(w, internalError(e))
		return
	}
	if len(names) > 0 {
		writeError(w, businessError("Ainda existem notas não liberadas: "+strings.Join(names, ", ")))
		return
	}
	_, e = a.db.Exec(r.Context(), `UPDATE sisges.academic_cycle SET current_trimester=LEAST(3,current_trimester+1),updated_at=now() WHERE id=(SELECT id FROM sisges.academic_cycle ORDER BY id LIMIT 1)`)
	if e != nil {
		writeError(w, internalError(e))
		return
	}
	a.getAcademicCycle(w, r)
}
func (a *App) endYear(w http.ResponseWriter, r *http.Request) {
	var req struct {
		Password string `json:"password"`
	}
	if !decodeJSON(w, r, &req) {
		return
	}
	if e := a.validateAdminPassword(r, req.Password); e != nil {
		writeError(w, e)
		return
	}
	c, e := a.readCycle(r)
	if e != nil || c.Status != "IN_PROGRESS" {
		writeError(w, businessError("O ano letivo não está em andamento."))
		return
	}
	names, e := a.pendingTeacherNames(r, nil)
	if e != nil {
		writeError(w, internalError(e))
		return
	}
	if len(names) > 0 {
		writeError(w, businessError("Ainda existem notas não liberadas: "+strings.Join(names, ", ")))
		return
	}
	if e = a.processYearEnd(r); e != nil {
		writeError(w, e)
		return
	}
	_, e = a.db.Exec(r.Context(), `UPDATE sisges.academic_cycle SET status='FINISHED',year_finished_at=now(),updated_at=now() WHERE id=(SELECT id FROM sisges.academic_cycle ORDER BY id LIMIT 1)`)
	if e != nil {
		writeError(w, internalError(e))
		return
	}
	a.getAcademicCycle(w, r)
}
