package api

import (
	"net/http"
	"strings"
)

type classSearch struct {
	ID           int    `json:"id"`
	Name         string `json:"name"`
	AcademicYear string `json:"academicYear"`
	StudentCount int    `json:"studentCount"`
	TeacherCount int    `json:"teacherCount"`
}
type disciplineSimple struct {
	ID   int    `json:"id"`
	Name string `json:"name"`
}
type classDetail struct {
	ID           int                `json:"id"`
	Name         string             `json:"name"`
	AcademicYear string             `json:"academicYear"`
	Students     []studentSimple    `json:"students"`
	Teachers     []teacherSimple    `json:"teachers"`
	Disciplines  []disciplineSimple `json:"disciplines"`
}
type createClassRequest struct {
	Name          string `json:"name"`
	AcademicYear  string `json:"academicYear"`
	StudentIDs    []int  `json:"studentIds"`
	TeacherIDs    []int  `json:"teacherIds"`
	DisciplineIDs []int  `json:"disciplineIds"`
}

func (a *App) createClass(w http.ResponseWriter, r *http.Request) {
	var req createClassRequest
	if !decodeJSON(w, r, &req) {
		return
	}
	req.Name = strings.TrimSpace(req.Name)
	if req.Name == "" {
		writeError(w, validationError("name", "Nome da turma é obrigatório"))
		return
	}
	if req.AcademicYear == "" {
		writeError(w, validationError("academicYear", "Série é obrigatória"))
		return
	}
	tx, err := a.db.Begin(r.Context())
	if err != nil {
		writeError(w, internalError(err))
		return
	}
	defer tx.Rollback(r.Context())
	var id int
	err = tx.QueryRow(r.Context(), `INSERT INTO sisges.school_class(name,academic_year) VALUES($1,$2) RETURNING id`, req.Name, req.AcademicYear).Scan(&id)
	if err != nil {
		writeError(w, apiErr(409, "DATA_CONFLICT", "Já existe uma turma com o nome informado."))
		return
	}
	for _, sid := range req.StudentIDs {
		if _, err = tx.Exec(r.Context(), `UPDATE sisges.student SET class_id=$1,updated_at=now() WHERE id=$2 AND deleted_at IS NULL`, id, sid); err != nil {
			writeError(w, internalError(err))
			return
		}
	}
	for _, tid := range req.TeacherIDs {
		if _, err = tx.Exec(r.Context(), `INSERT INTO sisges.teacher_class(teacher_id,class_id) VALUES($1,$2) ON CONFLICT(teacher_id,class_id) DO UPDATE SET deleted_at=NULL`, tid, id); err != nil {
			writeError(w, internalError(err))
			return
		}
	}
	for _, did := range req.DisciplineIDs {
		if _, err = tx.Exec(r.Context(), `INSERT INTO sisges.class_discipline(class_id,discipline_id) VALUES($1,$2) ON CONFLICT(class_id,discipline_id) DO UPDATE SET deleted_at=NULL`, id, did); err != nil {
			writeError(w, internalError(err))
			return
		}
	}
	if err = tx.Commit(r.Context()); err != nil {
		writeError(w, internalError(err))
		return
	}
	a.writeClass(w, r, id)
}

func (a *App) searchClasses(w http.ResponseWriter, r *http.Request) {
	var req struct {
		Name         string `json:"name"`
		AcademicYear string `json:"academicYear"`
	}
	if r.ContentLength != 0 && !decodeJSON(w, r, &req) {
		return
	}
	p := currentPrincipal(r)
	teacherID := 0
	if p.Role == "TEACHER" {
		_ = a.db.QueryRow(r.Context(), `SELECT id FROM sisges.teacher WHERE user_id=$1 AND deleted_at IS NULL`, p.ID).Scan(&teacherID)
	}
	rows, err := a.db.Query(r.Context(), `SELECT c.id,c.name,c.academic_year,(SELECT count(*) FROM sisges.student s WHERE s.class_id=c.id AND s.deleted_at IS NULL)::int,(SELECT count(*) FROM sisges.teacher_class tc WHERE tc.class_id=c.id AND tc.deleted_at IS NULL)::int FROM sisges.school_class c WHERE c.deleted_at IS NULL AND ($1='' OR c.name ILIKE '%'||$1||'%') AND ($2='' OR c.academic_year=$2) AND ($3=0 OR EXISTS(SELECT 1 FROM sisges.teacher_class tc WHERE tc.class_id=c.id AND tc.teacher_id=$3 AND tc.deleted_at IS NULL)) ORDER BY c.name LIMIT 1000`, req.Name, req.AcademicYear, teacherID)
	if err != nil {
		writeError(w, internalError(err))
		return
	}
	defer rows.Close()
	out := make([]classSearch, 0, 32)
	for rows.Next() {
		var x classSearch
		if rows.Scan(&x.ID, &x.Name, &x.AcademicYear, &x.StudentCount, &x.TeacherCount) == nil {
			out = append(out, x)
		}
	}
	writeJSON(w, http.StatusOK, out)
}

func (a *App) writeClass(w http.ResponseWriter, r *http.Request, id int) {
	p := currentPrincipal(r)
	if p != nil && p.Role == "TEACHER" {
		var ok bool
		_ = a.db.QueryRow(r.Context(), `SELECT EXISTS(SELECT 1 FROM sisges.teacher_class tc JOIN sisges.teacher t ON t.id=tc.teacher_id WHERE tc.class_id=$1 AND t.user_id=$2 AND tc.deleted_at IS NULL AND t.deleted_at IS NULL)`, id, p.ID).Scan(&ok)
		if !ok {
			writeError(w, forbidden())
			return
		}
	}
	var out classDetail
	err := a.db.QueryRow(r.Context(), `SELECT id,name,academic_year FROM sisges.school_class WHERE id=$1 AND deleted_at IS NULL`, id).Scan(&out.ID, &out.Name, &out.AcademicYear)
	if err != nil {
		writeError(w, resourceError("Turma"))
		return
	}
	out.Students = []studentSimple{}
	out.Teachers = []teacherSimple{}
	out.Disciplines = []disciplineSimple{}
	rows, err := a.db.Query(r.Context(), `SELECT s.id,u.name,u.email FROM sisges.student s JOIN sisges.users u ON u.id=s.user_id WHERE s.class_id=$1 AND s.deleted_at IS NULL AND u.deleted_at IS NULL ORDER BY u.name`, id)
	if err == nil {
		for rows.Next() {
			var x studentSimple
			if rows.Scan(&x.ID, &x.Name, &x.Email) == nil {
				out.Students = append(out.Students, x)
			}
		}
		rows.Close()
	}
	rows, err = a.db.Query(r.Context(), `SELECT t.id,u.name,u.email FROM sisges.teacher_class tc JOIN sisges.teacher t ON t.id=tc.teacher_id JOIN sisges.users u ON u.id=t.user_id WHERE tc.class_id=$1 AND tc.deleted_at IS NULL AND t.deleted_at IS NULL AND u.deleted_at IS NULL ORDER BY u.name`, id)
	if err == nil {
		for rows.Next() {
			var x teacherSimple
			if rows.Scan(&x.ID, &x.Name, &x.Email) == nil {
				out.Teachers = append(out.Teachers, x)
			}
		}
		rows.Close()
	}
	rows, err = a.db.Query(r.Context(), `SELECT d.id,d.name FROM sisges.class_discipline cd JOIN sisges.discipline d ON d.id=cd.discipline_id WHERE cd.class_id=$1 AND cd.deleted_at IS NULL AND d.deleted_at IS NULL ORDER BY d.name`, id)
	if err == nil {
		for rows.Next() {
			var x disciplineSimple
			if rows.Scan(&x.ID, &x.Name) == nil {
				out.Disciplines = append(out.Disciplines, x)
			}
		}
		rows.Close()
	}
	writeJSON(w, http.StatusOK, out)
}
func (a *App) classByID(w http.ResponseWriter, r *http.Request) {
	id, e := pathInt(r, "id")
	if e != nil {
		writeError(w, e)
		return
	}
	a.writeClass(w, r, id)
}
func (a *App) deleteClass(w http.ResponseWriter, r *http.Request) {
	id, e := pathInt(r, "id")
	if e != nil {
		writeError(w, e)
		return
	}
	tag, err := a.db.Exec(r.Context(), `UPDATE sisges.school_class SET deleted_at=now() WHERE id=$1 AND deleted_at IS NULL`, id)
	if err != nil {
		writeError(w, internalError(err))
		return
	}
	if tag.RowsAffected() == 0 {
		writeError(w, resourceError("Turma"))
		return
	}
	w.WriteHeader(http.StatusNoContent)
}
func (a *App) classRelation(w http.ResponseWriter, r *http.Request) {
	cid, e := pathInt(r, "classID")
	if e != nil {
		writeError(w, e)
		return
	}
	id, e := pathInt(r, "id")
	if e != nil {
		writeError(w, e)
		return
	}
	parts := strings.Split(strings.Trim(r.URL.Path, "/"), "/")
	if len(parts) < 6 {
		writeError(w, validationError("path", "Rota inválida"))
		return
	}
	kind, action := parts[3], parts[4]
	var q string
	switch kind + "/" + action {
	case "student/add":
		q = `UPDATE sisges.student SET class_id=$1,updated_at=now() WHERE id=$2 AND deleted_at IS NULL`
	case "student/remove":
		q = `UPDATE sisges.student SET class_id=NULL,updated_at=now() WHERE id=$2 AND class_id=$1 AND deleted_at IS NULL`
	case "teacher/add":
		q = `INSERT INTO sisges.teacher_class(class_id,teacher_id) VALUES($1,$2) ON CONFLICT(teacher_id,class_id) DO UPDATE SET deleted_at=NULL`
	case "teacher/remove":
		q = `UPDATE sisges.teacher_class SET deleted_at=now() WHERE class_id=$1 AND teacher_id=$2 AND deleted_at IS NULL`
	case "discipline/add":
		q = `INSERT INTO sisges.class_discipline(class_id,discipline_id) VALUES($1,$2) ON CONFLICT(class_id,discipline_id) DO UPDATE SET deleted_at=NULL`
	case "discipline/remove":
		q = `UPDATE sisges.class_discipline SET deleted_at=now() WHERE class_id=$1 AND discipline_id=$2 AND deleted_at IS NULL`
	default:
		writeError(w, validationError("path", "Relação inválida"))
		return
	}
	if _, err := a.db.Exec(r.Context(), q, cid, id); err != nil {
		writeError(w, internalError(err))
		return
	}
	a.writeClass(w, r, cid)
}
