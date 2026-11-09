package api

import "net/http"

// Save the teacher and chosen disciplines atomically, so a failed link leaves neither half saved.
func (a *App) linkTeacherWithDisciplines(w http.ResponseWriter, r *http.Request, classID, teacherID int) {
	var req struct {
		DisciplineIDs []int `json:"disciplineIds"`
	}
	if r.ContentLength != 0 && !decodeJSON(w, r, &req) {
		return
	}
	tx, err := a.db.Begin(r.Context())
	if err != nil {
		writeError(w, internalError(err))
		return
	}
	defer tx.Rollback(r.Context())
	var exists bool
	err = tx.QueryRow(r.Context(), `SELECT EXISTS(SELECT 1 FROM sisges.school_class c, sisges.teacher t JOIN sisges.users u ON u.id=t.user_id WHERE c.id=$1 AND t.id=$2 AND c.deleted_at IS NULL AND t.deleted_at IS NULL AND u.deleted_at IS NULL)`, classID, teacherID).Scan(&exists)
	if err != nil {
		writeError(w, internalError(err))
		return
	}
	if !exists {
		writeError(w, resourceError("Turma ou professor"))
		return
	}
	rows, err := tx.Query(r.Context(), `SELECT d.id FROM sisges.discipline_teacher dt JOIN sisges.discipline d ON d.id=dt.discipline_id WHERE dt.teacher_id=$1 AND d.deleted_at IS NULL ORDER BY d.id FOR SHARE OF dt, d`, teacherID)
	if err != nil {
		writeError(w, internalError(err))
		return
	}
	available := []int{}
	for rows.Next() {
		var id int
		if err = rows.Scan(&id); err != nil {
			rows.Close()
			writeError(w, internalError(err))
			return
		}
		available = append(available, id)
	}
	err = rows.Err()
	rows.Close()
	if err != nil {
		writeError(w, internalError(err))
		return
	}
	selected, message := teacherLinkDisciplines(available, req.DisciplineIDs)
	if message != "" {
		writeError(w, validationError("disciplineIds", message))
		return
	}
	if _, err = tx.Exec(r.Context(), `INSERT INTO sisges.teacher_class(class_id,teacher_id) VALUES($1,$2) ON CONFLICT(teacher_id,class_id) DO UPDATE SET deleted_at=NULL`, classID, teacherID); err != nil {
		writeError(w, internalError(err))
		return
	}
	for _, id := range selected {
		if _, err = tx.Exec(r.Context(), `INSERT INTO sisges.class_discipline(class_id,discipline_id) VALUES($1,$2) ON CONFLICT(class_id,discipline_id) DO UPDATE SET deleted_at=NULL`, classID, id); err != nil {
			writeError(w, internalError(err))
			return
		}
	}
	if err = tx.Commit(r.Context()); err != nil {
		writeError(w, internalError(err))
		return
	}
	a.writeClass(w, r, classID)
}

func teacherLinkDisciplines(available, selected []int) ([]int, string) {
	if len(available) == 0 {
		return nil, "Vincule uma disciplina ao professor antes de adicioná-lo à turma"
	}
	if len(selected) == 0 {
		if len(available) == 1 {
			return available, ""
		}
		return nil, "Selecione as disciplinas do professor para esta turma"
	}
	allowed := make(map[int]bool)
	for _, id := range available {
		allowed[id] = true
	}
	unique := []int{}
	seen := make(map[int]bool)
	for _, id := range selected {
		if !allowed[id] {
			return nil, "Selecione apenas disciplinas vinculadas ao professor"
		}
		if !seen[id] {
			unique = append(unique, id)
			seen[id] = true
		}
	}
	return unique, ""
}
