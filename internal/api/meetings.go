package api

import (
	"net/http"
	"time"
)

type meetingRequest struct {
	Date         string `json:"date"`
	DisciplineID int    `json:"disciplineId"`
	StartTime    string `json:"startTime"`
	EndTime      string `json:"endTime"`
	ClassID      int    `json:"classId"`
	TeacherID    *int   `json:"teacherId"`
}
type meetingSearch struct {
	ID             int     `json:"id"`
	Date           string  `json:"date"`
	StartTime      string  `json:"startTime"`
	EndTime        string  `json:"endTime"`
	DisciplineName string  `json:"disciplineName"`
	ClassName      string  `json:"className"`
	TeacherName    *string `json:"teacherName"`
}

func validateMeeting(req meetingRequest) *apiError {
	if _, e := time.Parse("2006-01-02", req.Date); e != nil {
		return validationError("date", "Data da aula é obrigatória")
	}
	st, e1 := time.Parse("15:04", req.StartTime)
	et, e2 := time.Parse("15:04", req.EndTime)
	if e1 != nil || e2 != nil {
		return validationError("startTime", "Horário inválido")
	}
	if !st.Before(et) {
		return businessError("Horário de início deve ser anterior ao horário de término.")
	}
	if req.DisciplineID < 1 || req.ClassID < 1 {
		return validationError("classId", "Turma e disciplina são obrigatórias")
	}
	return nil
}
func (a *App) teacherIDForUser(r *http.Request, userID int) (int, error) {
	var id int
	err := a.db.QueryRow(r.Context(), `SELECT id FROM sisges.teacher WHERE user_id=$1 AND deleted_at IS NULL`, userID).Scan(&id)
	return id, err
}
func (a *App) checkMeeting(r *http.Request, req meetingRequest, teacherID, exclude int) error {
	var linkedClass, linkedDisc, overlap bool
	err := a.db.QueryRow(r.Context(), `SELECT EXISTS(SELECT 1 FROM sisges.teacher_class WHERE teacher_id=$1 AND class_id=$2 AND deleted_at IS NULL),EXISTS(SELECT 1 FROM sisges.class_discipline WHERE class_id=$2 AND discipline_id=$3 AND deleted_at IS NULL),EXISTS(SELECT 1 FROM sisges.class_meeting WHERE class_id=$2 AND meeting_date=$4::date AND deleted_at IS NULL AND id<>$7 AND start_time<$6::time AND end_time>$5::time)`, teacherID, req.ClassID, req.DisciplineID, req.Date, req.StartTime, req.EndTime, exclude).Scan(&linkedClass, &linkedDisc, &overlap)
	if err != nil {
		return internalError(err)
	}
	if !linkedDisc {
		return businessError("A disciplina não está vinculada à turma.")
	}
	if !linkedClass {
		return businessError("O professor não está vinculado à turma.")
	}
	if overlap {
		return businessError("Uma turma não pode ter mais de uma aula ao mesmo tempo. Já existe aula cadastrada com horário sobreposto.")
	}
	return nil
}
func (a *App) createMeeting(w http.ResponseWriter, r *http.Request) {
	var req meetingRequest
	if !decodeJSON(w, r, &req) {
		return
	}
	if e := validateMeeting(req); e != nil {
		writeError(w, e)
		return
	}
	tid, err := a.teacherIDForUser(r, currentPrincipal(r).ID)
	if err != nil {
		writeError(w, businessError("Professor não encontrado."))
		return
	}
	if err = a.checkMeeting(r, req, tid, 0); err != nil {
		writeError(w, err)
		return
	}
	var id int
	err = a.db.QueryRow(r.Context(), `INSERT INTO sisges.class_meeting(class_id,discipline_id,teacher_id,meeting_date,start_time,end_time) VALUES($1,$2,$3,$4,$5,$6) RETURNING id`, req.ClassID, req.DisciplineID, tid, req.Date, req.StartTime, req.EndTime).Scan(&id)
	if err != nil {
		writeError(w, internalError(err))
		return
	}
	x, err := a.readMeetingSearch(r, id)
	if err != nil {
		writeError(w, internalError(err))
		return
	}
	writeJSON(w, http.StatusCreated, x)
}
func (a *App) readMeetingSearch(r *http.Request, id int) (meetingSearch, error) {
	var x meetingSearch
	var date time.Time
	err := a.db.QueryRow(r.Context(), `SELECT cm.id,cm.meeting_date,to_char(cm.start_time,'HH24:MI:SS'),to_char(cm.end_time,'HH24:MI:SS'),d.name,c.name,u.name FROM sisges.class_meeting cm JOIN sisges.discipline d ON d.id=cm.discipline_id JOIN sisges.school_class c ON c.id=cm.class_id LEFT JOIN sisges.teacher t ON t.id=cm.teacher_id AND t.deleted_at IS NULL LEFT JOIN sisges.users u ON u.id=t.user_id WHERE cm.id=$1 AND cm.deleted_at IS NULL`, id).Scan(&x.ID, &date, &x.StartTime, &x.EndTime, &x.DisciplineName, &x.ClassName, &x.TeacherName)
	x.Date = date.Format("2006-01-02")
	return x, err
}
func (a *App) searchMeetings(w http.ResponseWriter, r *http.Request) {
	var req struct {
		Date         string `json:"date"`
		DisciplineID *int   `json:"disciplineId"`
		ClassID      *int   `json:"classId"`
		TeacherID    *int   `json:"teacherId"`
	}
	if r.ContentLength != 0 && !decodeJSON(w, r, &req) {
		return
	}
	p := currentPrincipal(r)
	forced := 0
	if p.Role == "TEACHER" {
		forced, _ = a.teacherIDForUser(r, p.ID)
	}
	rows, err := a.db.Query(r.Context(), `SELECT cm.id,cm.meeting_date,to_char(cm.start_time,'HH24:MI:SS'),to_char(cm.end_time,'HH24:MI:SS'),d.name,c.name,u.name FROM sisges.class_meeting cm JOIN sisges.discipline d ON d.id=cm.discipline_id JOIN sisges.school_class c ON c.id=cm.class_id LEFT JOIN sisges.teacher t ON t.id=cm.teacher_id AND t.deleted_at IS NULL LEFT JOIN sisges.users u ON u.id=t.user_id WHERE cm.deleted_at IS NULL AND ($1='' OR cm.meeting_date=NULLIF($1,'')::date) AND ($2::int IS NULL OR cm.discipline_id=$2) AND ($3::int IS NULL OR cm.class_id=$3) AND ($4::int IS NULL OR cm.teacher_id=$4) AND ($5=0 OR cm.teacher_id=$5) ORDER BY cm.meeting_date DESC,cm.start_time LIMIT 1000`, req.Date, req.DisciplineID, req.ClassID, req.TeacherID, forced)
	if err != nil {
		writeError(w, internalError(err))
		return
	}
	defer rows.Close()
	out := make([]meetingSearch, 0, 64)
	for rows.Next() {
		var x meetingSearch
		var d time.Time
		if rows.Scan(&x.ID, &d, &x.StartTime, &x.EndTime, &x.DisciplineName, &x.ClassName, &x.TeacherName) == nil {
			x.Date = d.Format("2006-01-02")
			out = append(out, x)
		}
	}
	writeJSON(w, http.StatusOK, out)
}

type meetingDetail struct {
	ID        int    `json:"id"`
	Date      string `json:"date"`
	StartTime string `json:"startTime"`
	EndTime   string `json:"endTime"`
	ClassInfo struct {
		ID           int             `json:"id"`
		Name         string          `json:"name"`
		AcademicYear string          `json:"academicYear"`
		Students     []studentSimple `json:"students"`
	} `json:"classInfo"`
	Teacher *teacherSimple `json:"teacher"`
}

func (a *App) writeMeeting(w http.ResponseWriter, r *http.Request, id int) {
	p := currentPrincipal(r)
	var x meetingDetail
	var date time.Time
	var tid *int
	var tn, te *string
	err := a.db.QueryRow(r.Context(), `SELECT cm.id,cm.meeting_date,to_char(cm.start_time,'HH24:MI:SS'),to_char(cm.end_time,'HH24:MI:SS'),c.id,c.name,c.academic_year,t.id,u.name,u.email FROM sisges.class_meeting cm JOIN sisges.school_class c ON c.id=cm.class_id LEFT JOIN sisges.teacher t ON t.id=cm.teacher_id AND t.deleted_at IS NULL LEFT JOIN sisges.users u ON u.id=t.user_id WHERE cm.id=$1 AND cm.deleted_at IS NULL`, id).Scan(&x.ID, &date, &x.StartTime, &x.EndTime, &x.ClassInfo.ID, &x.ClassInfo.Name, &x.ClassInfo.AcademicYear, &tid, &tn, &te)
	if err != nil {
		writeError(w, resourceError("Aula"))
		return
	}
	if p.Role == "TEACHER" {
		mine, _ := a.teacherIDForUser(r, p.ID)
		if tid == nil || *tid != mine {
			writeError(w, forbidden())
			return
		}
	}
	x.Date = date.Format("2006-01-02")
	if tid != nil {
		x.Teacher = &teacherSimple{*tid, *tn, *te}
	}
	x.ClassInfo.Students = []studentSimple{}
	rows, err := a.db.Query(r.Context(), `SELECT s.id,u.name,u.email,at.present FROM sisges.student s JOIN sisges.users u ON u.id=s.user_id LEFT JOIN sisges.attendance at ON at.student_id=s.id AND at.class_meeting_id=$2 AND at.deleted_at IS NULL WHERE s.class_id=$1 AND s.deleted_at IS NULL ORDER BY u.name`, x.ClassInfo.ID, id)
	if err == nil {
		for rows.Next() {
			var y studentSimple
			if rows.Scan(&y.ID, &y.Name, &y.Email, &y.Present) == nil {
				x.ClassInfo.Students = append(x.ClassInfo.Students, y)
			}
		}
		rows.Close()
	}
	writeJSON(w, http.StatusOK, x)
}
func (a *App) meetingByID(w http.ResponseWriter, r *http.Request) {
	id, e := pathInt(r, "id")
	if e != nil {
		writeError(w, e)
		return
	}
	a.writeMeeting(w, r, id)
}
func (a *App) updateMeeting(w http.ResponseWriter, r *http.Request) {
	id, e := pathInt(r, "id")
	if e != nil {
		writeError(w, e)
		return
	}
	var req meetingRequest
	if !decodeJSON(w, r, &req) {
		return
	}
	if e := validateMeeting(req); e != nil {
		writeError(w, e)
		return
	}
	p := currentPrincipal(r)
	var tid int
	if p.Role == "TEACHER" {
		tid, _ = a.teacherIDForUser(r, p.ID)
		if req.TeacherID != nil && *req.TeacherID != tid {
			writeError(w, forbidden())
			return
		}
	} else if req.TeacherID != nil {
		tid = *req.TeacherID
	} else {
		_ = a.db.QueryRow(r.Context(), `SELECT teacher_id FROM sisges.class_meeting WHERE id=$1`, id).Scan(&tid)
	}
	if err := a.checkMeeting(r, req, tid, id); err != nil {
		writeError(w, err)
		return
	}
	tag, err := a.db.Exec(r.Context(), `UPDATE sisges.class_meeting SET class_id=$2,discipline_id=$3,teacher_id=$4,meeting_date=$5,start_time=$6,end_time=$7 WHERE id=$1 AND deleted_at IS NULL`, id, req.ClassID, req.DisciplineID, tid, req.Date, req.StartTime, req.EndTime)
	if err != nil {
		writeError(w, internalError(err))
		return
	}
	if tag.RowsAffected() == 0 {
		writeError(w, resourceError("Aula"))
		return
	}
	a.writeMeeting(w, r, id)
}
func (a *App) deleteMeeting(w http.ResponseWriter, r *http.Request) {
	id, e := pathInt(r, "id")
	if e != nil {
		writeError(w, e)
		return
	}
	tag, err := a.db.Exec(r.Context(), `UPDATE sisges.class_meeting SET deleted_at=now() WHERE id=$1 AND deleted_at IS NULL`, id)
	if err != nil {
		writeError(w, internalError(err))
		return
	}
	if tag.RowsAffected() == 0 {
		writeError(w, resourceError("Aula"))
		return
	}
	w.WriteHeader(http.StatusNoContent)
}
func (a *App) saveFrequency(w http.ResponseWriter, r *http.Request) {
	id, e := pathInt(r, "id")
	if e != nil {
		writeError(w, e)
		return
	}
	p := currentPrincipal(r)
	if p.Role == "TEACHER" {
		var ok bool
		_ = a.db.QueryRow(r.Context(), `SELECT EXISTS(SELECT 1 FROM sisges.class_meeting cm JOIN sisges.teacher t ON t.id=cm.teacher_id WHERE cm.id=$1 AND t.user_id=$2 AND cm.deleted_at IS NULL)`, id, p.ID).Scan(&ok)
		if !ok {
			writeError(w, forbidden())
			return
		}
	}
	var req struct {
		Entries []struct {
			StudentID int    `json:"studentId"`
			Status    string `json:"status"`
		} `json:"entries"`
	}
	if !decodeJSON(w, r, &req) {
		return
	}
	if len(req.Entries) == 0 {
		writeError(w, validationError("entries", "Lista de frequências é obrigatória"))
		return
	}
	tx, err := a.db.Begin(r.Context())
	if err != nil {
		writeError(w, internalError(err))
		return
	}
	defer tx.Rollback(r.Context())
	for _, x := range req.Entries {
		if x.Status != "P" && x.Status != "F" {
			writeError(w, validationError("status", "Status deve ser P (presente) ou F (faltoso)"))
			return
		}
		tag, err := tx.Exec(r.Context(), `INSERT INTO sisges.attendance(class_meeting_id,student_id,present) SELECT cm.id,s.id,$3 FROM sisges.class_meeting cm JOIN sisges.student s ON s.class_id=cm.class_id WHERE cm.id=$1 AND s.id=$2 AND cm.deleted_at IS NULL AND s.deleted_at IS NULL ON CONFLICT(class_meeting_id,student_id) DO UPDATE SET present=excluded.present,deleted_at=NULL,updated_at=now()`, id, x.StudentID, x.Status == "P")
		if err != nil {
			writeError(w, internalError(err))
			return
		}
		if tag.RowsAffected() == 0 {
			writeError(w, businessError("Aluno não pertence à turma desta aula."))
			return
		}
	}
	if err = tx.Commit(r.Context()); err != nil {
		writeError(w, internalError(err))
		return
	}
	w.WriteHeader(http.StatusNoContent)
}
