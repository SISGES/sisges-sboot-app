package api

import (
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
	out, err := a.activities(r, "ea.class_meeting_id=$1", id)
	if err != nil {
		writeError(w, internalError(err))
		return
	}
	writeJSON(w, http.StatusOK, out)
}
func (a *App) studentActivities(w http.ResponseWriter, r *http.Request) {
	out, err := a.activities(r, `cm.class_id=(SELECT class_id FROM sisges.student WHERE user_id=$1 AND deleted_at IS NULL)`, currentPrincipal(r).ID)
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
	var status string
	if err := a.db.QueryRow(r.Context(), `SELECT status FROM sisges.academic_cycle ORDER BY id LIMIT 1`).Scan(&status); err != nil || status != "IN_PROGRESS" {
		writeError(w, businessError("O ano letivo ainda não foi iniciado."))
		return
	}
	p := currentPrincipal(r)
	if p.Role == "TEACHER" {
		var ok bool
		_ = a.db.QueryRow(r.Context(), `SELECT EXISTS(SELECT 1 FROM sisges.class_meeting cm JOIN sisges.teacher t ON t.id=cm.teacher_id WHERE cm.id=$1 AND cm.deleted_at IS NULL AND t.user_id=$2)`, req.ClassMeetingID, p.ID).Scan(&ok)
		if !ok {
			writeError(w, businessError("Apenas o professor da aula pode criar atividades."))
			return
		}
	}
	var id int
	err := a.db.QueryRow(r.Context(), `INSERT INTO sisges.evaluative_activity(class_meeting_id,title,description,file_path,activity_type,trimester_number,max_points) VALUES($1,$2,$3,$4,$5,$6,$7) RETURNING id`, req.ClassMeetingID, strings.TrimSpace(req.Title), req.Description, req.FilePath, req.ActivityType, req.TrimesterNumber, req.MaxPoints).Scan(&id)
	if err != nil {
		writeError(w, internalError(err))
		return
	}
	out, err := a.activities(r, "ea.id=$1", id)
	if err != nil {
		writeError(w, internalError(err))
		return
	}
	writeJSON(w, http.StatusCreated, out[0])
}
func (a *App) deleteActivity(w http.ResponseWriter, r *http.Request) {
	id, e := pathInt(r, "id")
	if e != nil {
		writeError(w, e)
		return
	}
	if e := a.canManageActivity(r, id); e != nil {
		writeError(w, e)
		return
	}
	tag, err := a.db.Exec(r.Context(), `UPDATE sisges.evaluative_activity SET deleted_at=now() WHERE id=$1 AND deleted_at IS NULL`, id)
	if err != nil {
		writeError(w, internalError(err))
		return
	}
	if tag.RowsAffected() == 0 {
		writeError(w, resourceError("Atividade"))
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
	var cid int
	err := a.db.QueryRow(r.Context(), `SELECT ea.id,ea.class_meeting_id,ea.title,ea.activity_type,ea.trimester_number,ea.max_points::float8,ea.released,cm.class_id FROM sisges.evaluative_activity ea JOIN sisges.class_meeting cm ON cm.id=ea.class_meeting_id WHERE ea.id=$1 AND ea.deleted_at IS NULL`, id).Scan(&x.ActivityID, &x.ClassMeetingID, &x.Title, &x.ActivityType, &x.TrimesterNumber, &x.MaxPoints, &x.Released, &cid)
	if err != nil {
		return x, err
	}
	x.Students = []gradeLine{}
	rows, err := a.db.Query(r.Context(), `SELECT s.id,u.id,u.name,g.score::float8 FROM sisges.student s JOIN sisges.users u ON u.id=s.user_id LEFT JOIN sisges.activity_grade g ON g.student_id=s.id AND g.activity_id=$2 WHERE s.class_id=$1 AND s.deleted_at IS NULL AND u.deleted_at IS NULL ORDER BY s.id`, cid, id)
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
	if e := a.canManageActivity(r, id); e != nil {
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
	var max float64
	var released bool
	var cid int
	if err := a.db.QueryRow(r.Context(), `SELECT ea.max_points::float8,ea.released,cm.class_id FROM sisges.evaluative_activity ea JOIN sisges.class_meeting cm ON cm.id=ea.class_meeting_id WHERE ea.id=$1 AND ea.deleted_at IS NULL`, id).Scan(&max, &released, &cid); err != nil {
		writeError(w, resourceError("Atividade"))
		return
	}
	if released {
		writeError(w, businessError("As notas desta atividade já foram liberadas."))
		return
	}
	tx, err := a.db.Begin(r.Context())
	if err != nil {
		writeError(w, internalError(err))
		return
	}
	defer tx.Rollback(r.Context())
	for _, g := range req.Entries {
		if g.Score != nil && (*g.Score < 0 || *g.Score > max) {
			writeError(w, businessError("Nota não pode ser maior que a pontuação máxima da atividade."))
			return
		}
		tag, err := tx.Exec(r.Context(), `INSERT INTO sisges.activity_grade(activity_id,student_id,score) SELECT $1,s.id,$3 FROM sisges.student s WHERE s.id=$2 AND s.class_id=$4 AND s.deleted_at IS NULL ON CONFLICT(activity_id,student_id) DO UPDATE SET score=excluded.score,updated_at=now()`, id, g.StudentID, g.Score, cid)
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
	if e := a.canManageActivity(r, id); e != nil {
		writeError(w, e)
		return
	}
	tag, err := a.db.Exec(r.Context(), `UPDATE sisges.evaluative_activity SET released=true,released_at=COALESCE(released_at,now()),released_by_user_id=COALESCE(released_by_user_id,$2),updated_at=now() WHERE id=$1 AND deleted_at IS NULL`, id, currentPrincipal(r).ID)
	if err != nil {
		writeError(w, internalError(err))
		return
	}
	if tag.RowsAffected() == 0 {
		writeError(w, resourceError("Atividade"))
		return
	}
	out, err := a.activities(r, "ea.id=$1", id)
	if err != nil {
		writeError(w, internalError(err))
		return
	}
	writeJSON(w, http.StatusOK, out[0])
}
