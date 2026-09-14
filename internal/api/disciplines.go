package api

import (
	"net/http"
	"strings"
)

type disciplineResponse struct {
	ID          int            `json:"id"`
	Name        string         `json:"name"`
	Description *string        `json:"description"`
	Teachers    []personSearch `json:"teachers"`
}

func (a *App) readDiscipline(r *http.Request, id int) (disciplineResponse, error) {
	var x disciplineResponse
	err := a.db.QueryRow(r.Context(), `SELECT id,name,description FROM sisges.discipline WHERE id=$1 AND deleted_at IS NULL`, id).Scan(&x.ID, &x.Name, &x.Description)
	if err != nil {
		return x, err
	}
	x.Teachers = []personSearch{}
	rows, err := a.db.Query(r.Context(), `SELECT t.id,u.name,u.email FROM sisges.discipline_teacher dt JOIN sisges.teacher t ON t.id=dt.teacher_id AND t.deleted_at IS NULL JOIN sisges.users u ON u.id=t.user_id AND u.deleted_at IS NULL WHERE dt.discipline_id=$1 ORDER BY u.name`, id)
	if err != nil {
		return x, err
	}
	defer rows.Close()
	for rows.Next() {
		var t personSearch
		if rows.Scan(&t.ID, &t.Name, &t.Email) == nil {
			x.Teachers = append(x.Teachers, t)
		}
	}
	return x, rows.Err()
}
func (a *App) listDisciplines(w http.ResponseWriter, r *http.Request) {
	rows, err := a.db.Query(r.Context(), `SELECT id FROM sisges.discipline WHERE deleted_at IS NULL ORDER BY name LIMIT 1000`)
	if err != nil {
		writeError(w, internalError(err))
		return
	}
	ids := make([]int, 0, 32)
	for rows.Next() {
		var id int
		if rows.Scan(&id) == nil {
			ids = append(ids, id)
		}
	}
	rows.Close()
	out := make([]disciplineResponse, 0, len(ids))
	for _, id := range ids {
		x, err := a.readDiscipline(r, id)
		if err != nil {
			writeError(w, internalError(err))
			return
		}
		out = append(out, x)
	}
	writeJSON(w, http.StatusOK, out)
}
func (a *App) createDiscipline(w http.ResponseWriter, r *http.Request) {
	var req struct {
		Name        string  `json:"name"`
		Description *string `json:"description"`
		TeacherIDs  []int   `json:"teacherIds"`
	}
	if !decodeJSON(w, r, &req) {
		return
	}
	req.Name = strings.TrimSpace(req.Name)
	if req.Name == "" {
		writeError(w, validationError("name", "Nome da disciplina é obrigatório"))
		return
	}
	tx, err := a.db.Begin(r.Context())
	if err != nil {
		writeError(w, internalError(err))
		return
	}
	defer tx.Rollback(r.Context())
	var id int
	err = tx.QueryRow(r.Context(), `INSERT INTO sisges.discipline(name,description) VALUES($1,$2) RETURNING id`, req.Name, req.Description).Scan(&id)
	if err != nil {
		writeError(w, businessError("Já existe uma disciplina com o nome informado."))
		return
	}
	for _, tid := range req.TeacherIDs {
		if _, err = tx.Exec(r.Context(), `INSERT INTO sisges.discipline_teacher(discipline_id,teacher_id) VALUES($1,$2) ON CONFLICT DO NOTHING`, id, tid); err != nil {
			writeError(w, internalError(err))
			return
		}
	}
	if err = tx.Commit(r.Context()); err != nil {
		writeError(w, internalError(err))
		return
	}
	x, err := a.readDiscipline(r, id)
	if err != nil {
		writeError(w, internalError(err))
		return
	}
	writeJSON(w, http.StatusCreated, x)
}
func (a *App) disciplineByID(w http.ResponseWriter, r *http.Request) {
	id, e := pathInt(r, "id")
	if e != nil {
		writeError(w, e)
		return
	}
	x, err := a.readDiscipline(r, id)
	if err != nil {
		writeError(w, resourceError("Disciplina"))
		return
	}
	writeJSON(w, http.StatusOK, x)
}
func (a *App) updateDiscipline(w http.ResponseWriter, r *http.Request) {
	id, e := pathInt(r, "id")
	if e != nil {
		writeError(w, e)
		return
	}
	var req struct {
		Name        string  `json:"name"`
		Description *string `json:"description"`
		Teachers    []struct {
			TeacherID int  `json:"teacherId"`
			Linked    bool `json:"vinculado"`
		} `json:"teachers"`
	}
	if !decodeJSON(w, r, &req) {
		return
	}
	if strings.TrimSpace(req.Name) == "" {
		writeError(w, validationError("name", "Nome da disciplina é obrigatório"))
		return
	}
	tx, err := a.db.Begin(r.Context())
	if err != nil {
		writeError(w, internalError(err))
		return
	}
	defer tx.Rollback(r.Context())
	tag, err := tx.Exec(r.Context(), `UPDATE sisges.discipline SET name=$2,description=$3,updated_at=now() WHERE id=$1 AND deleted_at IS NULL`, id, strings.TrimSpace(req.Name), req.Description)
	if err != nil {
		writeError(w, businessError("Já existe uma disciplina com o nome informado."))
		return
	}
	if tag.RowsAffected() == 0 {
		writeError(w, resourceError("Disciplina"))
		return
	}
	for _, t := range req.Teachers {
		if t.Linked {
			_, err = tx.Exec(r.Context(), `INSERT INTO sisges.discipline_teacher(discipline_id,teacher_id) VALUES($1,$2) ON CONFLICT DO NOTHING`, id, t.TeacherID)
		} else {
			_, err = tx.Exec(r.Context(), `DELETE FROM sisges.discipline_teacher WHERE discipline_id=$1 AND teacher_id=$2`, id, t.TeacherID)
		}
		if err != nil {
			writeError(w, internalError(err))
			return
		}
	}
	if err = tx.Commit(r.Context()); err != nil {
		writeError(w, internalError(err))
		return
	}
	x, err := a.readDiscipline(r, id)
	if err != nil {
		writeError(w, internalError(err))
		return
	}
	writeJSON(w, http.StatusOK, x)
}
