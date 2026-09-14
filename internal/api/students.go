package api

import (
	"net/http"
	"time"
)

type studentSimple struct {
	ID      int    `json:"id"`
	Name    string `json:"name"`
	Email   string `json:"email"`
	Present *bool  `json:"present"`
}
type teacherSimple struct {
	ID    int    `json:"id"`
	Name  string `json:"name"`
	Email string `json:"email"`
}

func (a *App) studentMyClass(w http.ResponseWriter, r *http.Request) {
	p := currentPrincipal(r)
	var sid, cid int
	var cn, year string
	err := a.db.QueryRow(r.Context(), `SELECT s.id,c.id,c.name,c.academic_year FROM sisges.student s JOIN sisges.school_class c ON c.id=s.class_id AND c.deleted_at IS NULL WHERE s.user_id=$1 AND s.deleted_at IS NULL`, p.ID).Scan(&sid, &cid, &cn, &year)
	if err != nil {
		writeError(w, resourceError("Turma do aluno"))
		return
	}
	out := struct {
		ClassName    string          `json:"className"`
		AcademicYear string          `json:"academicYear"`
		Classmates   []studentSimple `json:"classmates"`
		Teachers     []teacherSimple `json:"teachers"`
	}{cn, year, []studentSimple{}, []teacherSimple{}}
	rows, err := a.db.Query(r.Context(), `SELECT s.id,u.name,u.email FROM sisges.student s JOIN sisges.users u ON u.id=s.user_id WHERE s.class_id=$1 AND s.deleted_at IS NULL AND u.deleted_at IS NULL ORDER BY u.name`, cid)
	if err != nil {
		writeError(w, internalError(err))
		return
	}
	for rows.Next() {
		var x studentSimple
		if rows.Scan(&x.ID, &x.Name, &x.Email) == nil {
			out.Classmates = append(out.Classmates, x)
		}
	}
	rows.Close()
	rows, err = a.db.Query(r.Context(), `SELECT t.id,u.name,u.email FROM sisges.teacher_class tc JOIN sisges.teacher t ON t.id=tc.teacher_id AND t.deleted_at IS NULL JOIN sisges.users u ON u.id=t.user_id AND u.deleted_at IS NULL WHERE tc.class_id=$1 AND tc.deleted_at IS NULL ORDER BY u.name`, cid)
	if err != nil {
		writeError(w, internalError(err))
		return
	}
	for rows.Next() {
		var x teacherSimple
		if rows.Scan(&x.ID, &x.Name, &x.Email) == nil {
			out.Teachers = append(out.Teachers, x)
		}
	}
	rows.Close()
	writeJSON(w, http.StatusOK, out)
}

func (a *App) studentAbsences(w http.ResponseWriter, r *http.Request) {
	p := currentPrincipal(r)
	rows, err := a.db.Query(r.Context(), `SELECT d.name,count(*)::int FROM sisges.student s JOIN sisges.attendance at ON at.student_id=s.id AND at.deleted_at IS NULL AND NOT at.present JOIN sisges.class_meeting cm ON cm.id=at.class_meeting_id AND cm.deleted_at IS NULL JOIN sisges.discipline d ON d.id=cm.discipline_id AND d.deleted_at IS NULL WHERE s.user_id=$1 AND s.deleted_at IS NULL GROUP BY d.id,d.name ORDER BY d.name`, p.ID)
	if err != nil {
		writeError(w, internalError(err))
		return
	}
	defer rows.Close()
	type item struct {
		DisciplineName string `json:"disciplineName"`
		AbsenceCount   int    `json:"absenceCount"`
	}
	out := []item{}
	for rows.Next() {
		var x item
		if rows.Scan(&x.DisciplineName, &x.AbsenceCount) == nil {
			out = append(out, x)
		}
	}
	writeJSON(w, http.StatusOK, out)
}

type studentAttendance struct {
	ClassMeeting struct {
		DisciplineName string    `json:"disciplineName"`
		MeetingDate    string    `json:"meetingDate"`
		CreatedAt      time.Time `json:"createdAt"`
	} `json:"classMeeting"`
	Present bool `json:"present"`
}
type responsibleResponse struct {
	ID    int    `json:"id"`
	Name  string `json:"name"`
	Phone string `json:"phone"`
	Email string `json:"email"`
}
type studentDetail struct {
	ID           int                   `json:"id"`
	Name         string                `json:"name"`
	Email        string                `json:"email"`
	Register     string                `json:"register"`
	BirthDate    string                `json:"birthDate"`
	Gender       string                `json:"gender"`
	CurrentClass *classSimple          `json:"currentClass"`
	Attendances  []studentAttendance   `json:"attendances"`
	Responsibles []responsibleResponse `json:"responsibles"`
}

func (a *App) studentByID(w http.ResponseWriter, r *http.Request) {
	id, e := pathInt(r, "id")
	if e != nil {
		writeError(w, e)
		return
	}
	var x studentDetail
	var birth time.Time
	var cid *int
	var cn, cy *string
	err := a.db.QueryRow(r.Context(), `SELECT s.id,u.name,u.email,u.register,u.birth_date,u.gender,c.id,c.name,c.academic_year FROM sisges.student s JOIN sisges.users u ON u.id=s.user_id LEFT JOIN sisges.school_class c ON c.id=s.class_id AND c.deleted_at IS NULL WHERE s.id=$1 AND s.deleted_at IS NULL AND u.deleted_at IS NULL`, id).Scan(&x.ID, &x.Name, &x.Email, &x.Register, &birth, &x.Gender, &cid, &cn, &cy)
	if err != nil {
		writeError(w, resourceError("Aluno"))
		return
	}
	x.BirthDate = birth.Format("2006-01-02")
	if cid != nil {
		x.CurrentClass = &classSimple{*cid, *cn, *cy}
	}
	x.Attendances = []studentAttendance{}
	x.Responsibles = []responsibleResponse{}
	rows, err := a.db.Query(r.Context(), `SELECT d.name,cm.meeting_date,cm.created_at,at.present FROM sisges.attendance at JOIN sisges.class_meeting cm ON cm.id=at.class_meeting_id JOIN sisges.discipline d ON d.id=cm.discipline_id WHERE at.student_id=$1 AND at.deleted_at IS NULL AND cm.deleted_at IS NULL ORDER BY cm.meeting_date DESC LIMIT 1000`, id)
	if err == nil {
		for rows.Next() {
			var y studentAttendance
			var date time.Time
			if rows.Scan(&y.ClassMeeting.DisciplineName, &date, &y.ClassMeeting.CreatedAt, &y.Present) == nil {
				y.ClassMeeting.MeetingDate = date.Format("2006-01-02")
				x.Attendances = append(x.Attendances, y)
			}
		}
		rows.Close()
	}
	rows, err = a.db.Query(r.Context(), `SELECT sr.id,sr.name,sr.phone,sr.email FROM sisges.student_responsible_link l JOIN sisges.student_responsible sr ON sr.id=l.responsible_id WHERE l.student_id=$1 AND sr.deleted_at IS NULL ORDER BY sr.name`, id)
	if err == nil {
		for rows.Next() {
			var y responsibleResponse
			if rows.Scan(&y.ID, &y.Name, &y.Phone, &y.Email) == nil {
				x.Responsibles = append(x.Responsibles, y)
			}
		}
		rows.Close()
	}
	writeJSON(w, http.StatusOK, x)
}
