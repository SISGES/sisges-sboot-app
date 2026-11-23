package api

import (
	"errors"
	"github.com/jackc/pgx/v5"
	"net/http"
	"strings"
	"time"
)

func (a *App) routeActivityGet(w http.ResponseWriter, r *http.Request) {
	parts := strings.Split(strings.Trim(r.PathValue("rest"), "/"), "/")
	if len(parts) == 2 && parts[1] == "gradebook" {
		r.SetPathValue("id", parts[0])
		a.activityGradebook(w, r)
		return
	}
	writeError(w, resourceError("Rota"))
}

type activityResponse struct {
	ID              int        `json:"id"`
	ClassMeetingID  int        `json:"classMeetingId"`
	Title           string     `json:"title"`
	Description     *string    `json:"description"`
	FilePath        *string    `json:"filePath"`
	ActivityType    string     `json:"activityType"`
	TrimesterNumber *int       `json:"trimesterNumber"`
	MaxPoints       float64    `json:"maxPoints"`
	Released        bool       `json:"released"`
	ReleasedAt      *time.Time `json:"releasedAt"`
	CreatedAt       time.Time  `json:"createdAt"`
}

func (a *App) activities(r *http.Request, where string, args ...any) ([]activityResponse, error) {
	rows, err := a.db.Query(r.Context(), `SELECT ea.id,ea.class_meeting_id,ea.title,ea.description,ea.file_path,ea.activity_type,ea.trimester_number,ea.max_points::float8,ea.released,ea.released_at,ea.created_at FROM sisges.evaluative_activity ea JOIN sisges.class_meeting cm ON cm.id=ea.class_meeting_id WHERE ea.deleted_at IS NULL AND cm.deleted_at IS NULL AND `+where+` ORDER BY ea.created_at DESC LIMIT 1000`, args...)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	out := []activityResponse{}
	for rows.Next() {
		var x activityResponse
		if err = rows.Scan(&x.ID, &x.ClassMeetingID, &x.Title, &x.Description, &x.FilePath, &x.ActivityType, &x.TrimesterNumber, &x.MaxPoints, &x.Released, &x.ReleasedAt, &x.CreatedAt); err != nil {
			return nil, err
		}
		out = append(out, x)
	}
	return out, rows.Err()
}
func (a *App) activitiesByMeeting(w http.ResponseWriter, r *http.Request) {
	id, e := pathInt(r, "id")
	if e != nil {
		writeError(w, e)
		return
	}
	if currentPrincipal(r).Role == "TEACHER" {
		var owns bool
		err := a.db.QueryRow(r.Context(), `SELECT EXISTS(SELECT 1 FROM sisges.class_meeting m JOIN sisges.teacher t ON t.id=m.teacher_id WHERE m.id=$1 AND m.deleted_at IS NULL AND t.user_id=$2 AND t.deleted_at IS NULL)`, id, currentPrincipal(r).ID).Scan(&owns)
		if err != nil {
			writeError(w, err)
			return
		}
		if !owns {
			writeError(w, forbidden())
			return
		}
	}
	out, err := a.activities(r, "ea.class_meeting_id=$1", id)
	if err != nil {
		writeError(w, internalError(err))
		return
	}
	writeJSON(w, http.StatusOK, out)
}
func (a *App) studentActivities(w http.ResponseWriter, r *http.Request) {
	out, err := a.activities(r, `ea.academic_cycle_id=(SELECT id FROM sisges.academic_cycle WHERE is_current) AND EXISTS(
        SELECT 1 FROM sisges.activity_grade g JOIN sisges.student s ON s.id=g.student_id
        WHERE g.activity_id=ea.id AND s.user_id=$1 AND s.deleted_at IS NULL)`, currentPrincipal(r).ID)
	if err != nil {
		writeError(w, internalError(err))
		return
	}
	writeJSON(w, http.StatusOK, out)
}

type activityRequest struct {
	ClassMeetingID  int     `json:"classMeetingId"`
	Title           string  `json:"title"`
	Description     *string `json:"description"`
	FilePath        *string `json:"filePath"`
	ActivityType    string  `json:"activityType"`
	TrimesterNumber *int    `json:"trimesterNumber"`
	MaxPoints       float64 `json:"maxPoints"`
}

func validActivityType(v string) bool {
	switch v {
	case "PROVA", "ATIVIDADE", "TRABALHO", "RECUPERACAO_TRIMESTRE", "RECUPERACAO_ANUAL":
		return true
	}
	return false
}
func (a *App) canManageActivity(r *http.Request, id int) error {
	p := currentPrincipal(r)
	if p.Role == "ADMIN" {
		return nil
	}
	var ok bool
	err := a.db.QueryRow(r.Context(), `SELECT EXISTS(SELECT 1 FROM sisges.evaluative_activity ea JOIN sisges.class_meeting cm ON cm.id=ea.class_meeting_id JOIN sisges.teacher t ON t.id=cm.teacher_id WHERE ea.id=$1 AND ea.deleted_at IS NULL AND t.user_id=$2 AND t.deleted_at IS NULL)`, id, p.ID).Scan(&ok)
	if err != nil {
		return internalError(err)
	}
	if !ok {
		return businessError("Apenas o professor da aula pode gerenciar esta atividade.")
	}
	return nil
}
func (a *App) createActivity(w http.ResponseWriter, r *http.Request) {
	var req activityRequest
	if !decodeJSON(w, r, &req) {
		return
	}
	if err := validateRecordFile(req.FilePath, "activities", currentPrincipal(r).ID); err != nil {
		writeError(w, err)
		return
	}
	if strings.TrimSpace(req.Title) == "" {
		writeError(w, validationError("title", "Título é obrigatório"))
		return
	}
	if req.ActivityType == "" {
		req.ActivityType = "ATIVIDADE"
	}
	if !validActivityType(req.ActivityType) {
		writeError(w, validationError("activityType", "Tipo de atividade inválido"))
		return
	}
	if req.MaxPoints <= 0 {
		writeError(w, validationError("maxPoints", "Pontuação máxima deve ser maior que zero"))
		return
	}
	if req.TrimesterNumber != nil && (*req.TrimesterNumber < 1 || *req.TrimesterNumber > 3) {
		writeError(w, validationError("trimesterNumber", "Trimestre inválido"))
		return
	}
	ctx := r.Context()
	tx, err := a.academicTx(ctx, false)
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
	if c.Status != "IN_PROGRESS" {
		writeError(w, businessError("O ano letivo ainda não foi iniciado."))
		return
	}
	if req.ActivityType == "RECUPERACAO_ANUAL" {
		req.TrimesterNumber = nil
	} else if req.TrimesterNumber == nil {
		req.TrimesterNumber = &c.CurrentTrimester
	} else if *req.TrimesterNumber != c.CurrentTrimester {
		writeError(w, businessError("A atividade deve pertencer ao trimestre atual."))
		return
	}
	var classID int
	var teacherUser *int
	var day string
	err = tx.QueryRow(ctx, `SELECT cm.class_id,t.user_id,to_char(cm.meeting_date,'YYYY-MM-DD')
        FROM sisges.class_meeting cm LEFT JOIN sisges.teacher t ON t.id=cm.teacher_id AND t.deleted_at IS NULL
        WHERE cm.id=$1 AND cm.deleted_at IS NULL FOR SHARE OF cm`, req.ClassMeetingID).Scan(&classID, &teacherUser, &day)
	if err != nil {
		writeError(w, dbError(err))
		return
	}
	p := currentPrincipal(r)
	if p.Role == "TEACHER" && (teacherUser == nil || *teacherUser != p.ID) {
		writeError(w, businessError("Apenas o professor da aula pode criar atividades."))
		return
	}
	if c.YearStartDate == nil || c.YearEndDate == nil || day < *c.YearStartDate || day > *c.YearEndDate {
		writeError(w, businessError("A data da aula está fora do ano letivo em andamento."))
		return
	}
	var id int
	err = tx.QueryRow(ctx, `INSERT INTO sisges.evaluative_activity(class_meeting_id,title,description,file_path,activity_type,trimester_number,max_points,academic_cycle_id)
        VALUES($1,$2,$3,$4,$5,$6,$7,$8) RETURNING id`, req.ClassMeetingID, strings.TrimSpace(req.Title), req.Description, req.FilePath, req.ActivityType, req.TrimesterNumber, req.MaxPoints, c.ID).Scan(&id)
	if err != nil {
		writeError(w, err)
		return
	}
	_, err = tx.Exec(ctx, `INSERT INTO sisges.activity_grade(activity_id,student_id)
        SELECT $1,s.id FROM sisges.student s JOIN sisges.users u ON u.id=s.user_id WHERE s.class_id=$2 AND s.deleted_at IS NULL AND u.deleted_at IS NULL`, id, classID)
	if err != nil {
		writeError(w, err)
		return
	}
	if err = tx.Commit(ctx); err != nil {
		writeError(w, err)
		return
	}
	out, err := a.activities(r, "ea.id=$1", id)
	if err != nil {
		writeError(w, internalError(err))
		return
	}
	if len(out) == 0 {
		writeError(w, resourceError("Atividade"))
		return
	}
	writeJSON(w, http.StatusCreated, out[0])
}

// Obtenha o bloqueio do ciclo antes da atividade em todas as operações.
func lockActivity(r *http.Request, tx pgx.Tx, id int) (float64, bool, error) {
	var maxPoints float64
	var released, active bool
	var owner *int
	err := tx.QueryRow(r.Context(), `SELECT ea.max_points::float8,ea.released,
        COALESCE(c.is_current AND c.status='IN_PROGRESS',false),
        t.user_id FROM sisges.evaluative_activity ea JOIN sisges.class_meeting m ON m.id=ea.class_meeting_id AND m.deleted_at IS NULL
        LEFT JOIN sisges.academic_cycle c ON c.id=ea.academic_cycle_id
        LEFT JOIN sisges.teacher t ON t.id=m.teacher_id AND t.deleted_at IS NULL
        WHERE ea.id=$1 AND ea.deleted_at IS NULL FOR UPDATE OF ea`, id).Scan(&maxPoints, &released, &active, &owner)
	if errors.Is(err, pgx.ErrNoRows) {
		return 0, false, resourceError("Atividade")
	}
	if err != nil {
		return 0, false, err
	}
	p := currentPrincipal(r)
	if p.Role == "TEACHER" && (owner == nil || *owner != p.ID) {
		return 0, false, businessError("Apenas o professor da aula pode gerenciar esta atividade.")
	}
	if !active {
		return 0, false, businessError("A atividade não pertence ao período letivo em andamento.")
	}
	return maxPoints, released, nil
}

func (a *App) deleteActivity(w http.ResponseWriter, r *http.Request) {
	id, err := pathInt(r, "id")
	if err != nil {
		writeError(w, err)
		return
	}
	tx, err := a.academicTx(r.Context(), false)
	if err != nil {
		writeError(w, err)
		return
	}
	defer tx.Rollback(r.Context())
	_, released, err := lockActivity(r, tx, id)
	if err != nil {
		writeError(w, err)
		return
	}
	if released {
		writeError(w, businessError("Uma atividade com notas liberadas não pode ser excluída."))
		return
	}
	if _, err = tx.Exec(r.Context(), `UPDATE sisges.evaluative_activity SET deleted_at=now() WHERE id=$1`, id); err != nil {
		writeError(w, err)
		return
	}
	if err = tx.Commit(r.Context()); err != nil {
		writeError(w, err)
		return
	}
	w.WriteHeader(http.StatusNoContent)
}

type gradeLine struct {
	StudentID   int      `json:"studentId"`
	UserID      int      `json:"userId"`
	StudentName string   `json:"studentName"`
	Score       *float64 `json:"score"`
}
type gradebook struct {
	ActivityID      int         `json:"activityId"`
	ClassMeetingID  int         `json:"classMeetingId"`
	Title           string      `json:"title"`
	ActivityType    string      `json:"activityType"`
	TrimesterNumber *int        `json:"trimesterNumber"`
	MaxPoints       float64     `json:"maxPoints"`
	Released        bool        `json:"released"`
	Students        []gradeLine `json:"students"`
}

func (a *App) readGradebook(r *http.Request, id int) (gradebook, error) {
	var x gradebook
	err := a.db.QueryRow(r.Context(), `SELECT ea.id,ea.class_meeting_id,ea.title,ea.activity_type,ea.trimester_number,ea.max_points::float8,ea.released FROM sisges.evaluative_activity ea JOIN sisges.class_meeting cm ON cm.id=ea.class_meeting_id WHERE ea.id=$1 AND ea.deleted_at IS NULL`, id).Scan(&x.ActivityID, &x.ClassMeetingID, &x.Title, &x.ActivityType, &x.TrimesterNumber, &x.MaxPoints, &x.Released)
	if err != nil {
		return x, err
	}
	x.Students = []gradeLine{}
	rows, err := a.db.Query(r.Context(), `SELECT s.id,u.id,u.name,g.score::float8 FROM sisges.activity_grade g JOIN sisges.student s ON s.id=g.student_id JOIN sisges.users u ON u.id=s.user_id WHERE g.activity_id=$1 ORDER BY s.id`, id)
	if err != nil {
		return x, err
	}
	defer rows.Close()
	for rows.Next() {
		var y gradeLine
		if err = rows.Scan(&y.StudentID, &y.UserID, &y.StudentName, &y.Score); err != nil {
			return x, err
		}
		x.Students = append(x.Students, y)
	}
	return x, rows.Err()
}
func (a *App) activityGradebook(w http.ResponseWriter, r *http.Request) {
	id, e := pathInt(r, "id")
	if e != nil {
		writeError(w, e)
		return
	}
	if e := a.canManageActivity(r, id); e != nil {
		writeError(w, e)
		return
	}
	x, err := a.readGradebook(r, id)
	if err != nil {
		writeError(w, resourceError("Atividade"))
		return
	}
	writeJSON(w, http.StatusOK, x)
}
func (a *App) saveGrades(w http.ResponseWriter, r *http.Request) {
	id, e := pathInt(r, "id")
	if e != nil {
		writeError(w, e)
		return
	}

	var req struct {
		Entries []struct {
			StudentID int      `json:"studentId"`
			Score     *float64 `json:"score"`
		} `json:"entries"`
	}
	if !decodeJSON(w, r, &req) {
		return
	}
	if len(req.Entries) == 0 {
		writeError(w, validationError("entries", "Informe pelo menos uma nota"))
		return
	}
	tx, err := a.academicTx(r.Context(), false)
	if err != nil {
		writeError(w, err)
		return
	}
	defer tx.Rollback(r.Context())
	max, released, err := lockActivity(r, tx, id)
	if err != nil {
		writeError(w, err)
		return
	}
	if released {
		writeError(w, businessError("As notas desta atividade já foram liberadas."))
		return
	}
	for _, g := range req.Entries {
		if g.Score != nil && (*g.Score < 0 || *g.Score > max) {
			writeError(w, businessError("Nota não pode ser maior que a pontuação máxima da atividade."))
			return
		}
		tag, err := tx.Exec(r.Context(), `UPDATE sisges.activity_grade SET score=$3,updated_at=now() WHERE activity_id=$1 AND student_id=$2`, id, g.StudentID, g.Score)
		if err != nil {
			writeError(w, internalError(err))
			return
		}
		if tag.RowsAffected() == 0 {
			writeError(w, businessError("Aluno não pertence à turma desta atividade."))
			return
		}
	}
	if err = tx.Commit(r.Context()); err != nil {
		writeError(w, internalError(err))
		return
	}
	x, err := a.readGradebook(r, id)
	if err != nil {
		writeError(w, internalError(err))
		return
	}
	writeJSON(w, http.StatusOK, x)
}
func (a *App) releaseActivity(w http.ResponseWriter, r *http.Request) {
	id, e := pathInt(r, "id")
	if e != nil {
		writeError(w, e)
		return
	}
	tx, err := a.academicTx(r.Context(), false)
	if err != nil {
		writeError(w, err)
		return
	}
	defer tx.Rollback(r.Context())
	_, released, err := lockActivity(r, tx, id)
	if err != nil {
		writeError(w, err)
		return
	}
	if !released {
		_, err = tx.Exec(r.Context(), `UPDATE sisges.evaluative_activity SET released=true,released_at=now(),released_by_user_id=$2,updated_at=now() WHERE id=$1`, id, currentPrincipal(r).ID)
		if err != nil {
			writeError(w, err)
			return
		}
	}
	if err = tx.Commit(r.Context()); err != nil {
		writeError(w, err)
		return
	}
	out, err := a.activities(r, "ea.id=$1", id)
	if err != nil {
		writeError(w, internalError(err))
		return
	}
	if len(out) == 0 {
		writeError(w, resourceError("Atividade"))
		return
	}
	writeJSON(w, http.StatusOK, out[0])
}
