package api

import (
	"encoding/json"
	"fmt"
	"net/http"
	"strings"
	"time"

	"github.com/jackc/pgx/v5"
)

var lessonTimes = [5][2]string{{"07:00", "07:50"}, {"07:50", "08:40"}, {"08:40", "09:30"}, {"09:45", "10:35"}, {"10:35", "11:25"}}

type scheduleSlot struct {
	Weekday      int `json:"weekday"`
	Slot         int `json:"slot"`
	DisciplineID int `json:"disciplineId"`
}
type scheduleResponse struct {
	Revision int            `json:"revision"`
	Slots    []scheduleSlot `json:"slots"`
}

func validateSchedule(slots []scheduleSlot) error {
	seen := map[[2]int]bool{}
	for _, s := range slots {
		k := [2]int{s.Weekday, s.Slot}
		if s.Weekday < 1 || s.Weekday > 5 || s.Slot < 0 || s.Slot >= len(lessonTimes) || s.DisciplineID < 1 || seen[k] {
			return businessError("Grade inválida: verifique os dias, horários e disciplinas.")
		}
		seen[k] = true
	}
	return nil
}
func (a *App) getSchedule(w http.ResponseWriter, r *http.Request) {
	id, e := pathInt(r, "id")
	if e != nil {
		writeError(w, e)
		return
	}
	result := scheduleResponse{Slots: []scheduleSlot{}}
	err := a.db.QueryRow(r.Context(), `SELECT schedule_revision FROM sisges.school_class WHERE id=$1 AND deleted_at IS NULL`, id).Scan(&result.Revision)
	if err != nil {
		writeError(w, resourceError("Turma"))
		return
	}
	rows, err := a.db.Query(r.Context(), `SELECT weekday,slot,discipline_id FROM sisges.class_schedule WHERE class_id=$1 ORDER BY weekday,slot`, id)
	if err != nil {
		writeError(w, internalError(err))
		return
	}
	defer rows.Close()
	for rows.Next() {
		var s scheduleSlot
		if err = rows.Scan(&s.Weekday, &s.Slot, &s.DisciplineID); err != nil {
			writeError(w, internalError(err))
			return
		}
		result.Slots = append(result.Slots, s)
	}
	if rows.Err() != nil {
		writeError(w, internalError(rows.Err()))
		return
	}
	writeJSON(w, 200, result)
}
func (a *App) saveSchedule(w http.ResponseWriter, r *http.Request) {
	id, e := pathInt(r, "id")
	if e != nil {
		writeError(w, e)
		return
	}
	var req scheduleResponse
	if !decodeJSON(w, r, &req) {
		return
	}
	if e = validateSchedule(req.Slots); e != nil {
		writeError(w, e)
		return
	}
	tx, err := a.db.Begin(r.Context())
	if err != nil {
		writeError(w, internalError(err))
		return
	}
	defer tx.Rollback(r.Context())
	var revision int
	if err = tx.QueryRow(r.Context(), `SELECT schedule_revision FROM sisges.school_class WHERE id=$1 AND deleted_at IS NULL FOR UPDATE`, id).Scan(&revision); err != nil {
		writeError(w, resourceError("Turma"))
		return
	}
	if revision != req.Revision {
		writeError(w, businessError("A grade foi alterada por outra pessoa. Recarregue antes de salvar."))
		return
	}
	for _, s := range req.Slots {
		var ok bool
		err = tx.QueryRow(r.Context(), `SELECT EXISTS(SELECT 1 FROM sisges.class_discipline cd JOIN sisges.discipline d ON d.id=cd.discipline_id AND d.deleted_at IS NULL WHERE cd.class_id=$1 AND cd.discipline_id=$2 AND cd.deleted_at IS NULL)`, id, s.DisciplineID).Scan(&ok)
		if err != nil {
			writeError(w, internalError(err))
			return
		}
		if !ok {
			writeError(w, businessError("A disciplina deve estar vinculada à turma."))
			return
		}
	}
	if _, err = tx.Exec(r.Context(), `DELETE FROM sisges.class_schedule WHERE class_id=$1`, id); err != nil {
		writeError(w, internalError(err))
		return
	}
	for _, s := range req.Slots {
		if _, err = tx.Exec(r.Context(), `INSERT INTO sisges.class_schedule(class_id,weekday,slot,discipline_id) VALUES($1,$2,$3,$4)`, id, s.Weekday, s.Slot, s.DisciplineID); err != nil {
			writeError(w, internalError(err))
			return
		}
	}
	if _, err = tx.Exec(r.Context(), `UPDATE sisges.school_class SET schedule_revision=schedule_revision+1 WHERE id=$1`, id); err != nil {
		writeError(w, internalError(err))
		return
	}
	if err = tx.Commit(r.Context()); err != nil {
		writeError(w, internalError(err))
		return
	}
	req.Revision++
	if req.Slots == nil {
		req.Slots = []scheduleSlot{}
	}
	writeJSON(w, 200, req)
}

type lessonAttachment struct {
	Name string `json:"name"`
	Path string `json:"path"`
}
type dailyLesson struct {
	Slot           int                `json:"slot"`
	DisciplineID   int                `json:"disciplineId"`
	DisciplineName string             `json:"disciplineName"`
	StartTime      string             `json:"startTime"`
	EndTime        string             `json:"endTime"`
	MeetingID      *int               `json:"meetingId"`
	Content        string             `json:"content"`
	Attachments    []lessonAttachment `json:"attachments"`
}

func (a *App) teacherDay(w http.ResponseWriter, r *http.Request) {
	id, e := pathInt(r, "id")
	if e != nil {
		writeError(w, e)
		return
	}
	date, err := time.Parse("2006-01-02", r.URL.Query().Get("date"))
	if err != nil {
		writeError(w, validationError("date", "Data inválida"))
		return
	}
	tid, err := a.teacherIDForUser(r, currentPrincipal(r).ID)
	if err != nil {
		writeError(w, forbidden())
		return
	}
	var linked bool
	err = a.db.QueryRow(r.Context(), `SELECT EXISTS(SELECT 1 FROM sisges.teacher_class tc JOIN sisges.school_class c ON c.id=tc.class_id AND c.deleted_at IS NULL WHERE tc.teacher_id=$1 AND tc.class_id=$2 AND tc.deleted_at IS NULL)`, tid, id).Scan(&linked)
	if err != nil {
		writeError(w, internalError(err))
		return
	}
	if !linked {
		writeError(w, forbidden())
		return
	}
	// Existing records retain their subject/content even after timetable changes.
	rows, err := a.db.Query(r.Context(), `SELECT s.slot,s.discipline_id,d.name,NULL::integer,''::text,'[]'::jsonb FROM sisges.class_schedule s JOIN sisges.discipline d ON d.id=s.discipline_id AND d.deleted_at IS NULL JOIN sisges.discipline_teacher dt ON dt.discipline_id=s.discipline_id AND dt.teacher_id=$3 JOIN sisges.class_discipline cd ON cd.class_id=s.class_id AND cd.discipline_id=s.discipline_id AND cd.deleted_at IS NULL WHERE s.class_id=$1 AND s.weekday=$4 AND NOT EXISTS(SELECT 1 FROM sisges.class_meeting m WHERE m.class_id=$1 AND m.meeting_date=$2::date AND m.schedule_slot=s.slot AND m.deleted_at IS NULL)
 UNION ALL SELECT m.schedule_slot,m.discipline_id,d.name,m.id,m.content,m.attachments FROM sisges.class_meeting m JOIN sisges.discipline d ON d.id=m.discipline_id WHERE m.class_id=$1 AND m.meeting_date=$2::date AND m.teacher_id=$3 AND m.schedule_slot IS NOT NULL AND m.deleted_at IS NULL ORDER BY 1`, id, date.Format("2006-01-02"), tid, int(date.Weekday()))
	if err != nil {
		writeError(w, internalError(err))
		return
	}
	defer rows.Close()
	result := []dailyLesson{}
	for rows.Next() {
		var x dailyLesson
		var raw []byte
		if err = rows.Scan(&x.Slot, &x.DisciplineID, &x.DisciplineName, &x.MeetingID, &x.Content, &raw); err != nil {
			writeError(w, internalError(err))
			return
		}
		if err = json.Unmarshal(raw, &x.Attachments); err != nil {
			writeError(w, internalError(err))
			return
		}
		x.StartTime = lessonTimes[x.Slot][0]
		x.EndTime = lessonTimes[x.Slot][1]
		result = append(result, x)
	}
	if rows.Err() != nil {
		writeError(w, internalError(rows.Err()))
		return
	}
	writeJSON(w, 200, result)
}
func (a *App) saveLessonRecord(w http.ResponseWriter, r *http.Request) {
	id, e := pathInt(r, "id")
	if e != nil {
		writeError(w, e)
		return
	}
	var req struct {
		Date         string             `json:"date"`
		Slot         int                `json:"slot"`
		DisciplineID int                `json:"disciplineId"`
		Content      string             `json:"content"`
		Attachments  []lessonAttachment `json:"attachments"`
	}
	if !decodeJSON(w, r, &req) {
		return
	}
	date, err := time.Parse("2006-01-02", req.Date)
	if err != nil || req.Slot < 0 || req.Slot >= len(lessonTimes) {
		writeError(w, businessError("Data ou horário inválido."))
		return
	}
	req.Content = strings.TrimSpace(req.Content)
	if len(req.Content) > 20000 || len(req.Attachments) > 10 || (req.Content == "" && len(req.Attachments) == 0) {
		writeError(w, businessError("Informe o conteúdo ou anexe arquivos (até 10 arquivos e 20.000 caracteres)."))
		return
	}
	tid, err := a.teacherIDForUser(r, currentPrincipal(r).ID)
	if err != nil {
		writeError(w, forbidden())
		return
	}
	for _, file := range req.Attachments {
		if !validLessonAttachment(file, currentPrincipal(r).ID) {
			writeError(w, businessError("Anexo inválido."))
			return
		}
	}
	if req.Attachments == nil {
		req.Attachments = []lessonAttachment{}
	}
	raw, _ := json.Marshal(req.Attachments)
	tx, err := a.db.Begin(r.Context())
	if err != nil {
		writeError(w, internalError(err))
		return
	}
	defer tx.Rollback(r.Context())
	var lockID int
	err = tx.QueryRow(r.Context(), `SELECT id FROM sisges.school_class WHERE id=$1 AND deleted_at IS NULL FOR UPDATE`, id).Scan(&lockID)
	if err != nil {
		writeError(w, resourceError("Turma"))
		return
	}
	var linked bool
	err = tx.QueryRow(r.Context(), `SELECT EXISTS(SELECT 1 FROM sisges.teacher_class WHERE class_id=$1 AND teacher_id=$2 AND deleted_at IS NULL)`, id, tid).Scan(&linked)
	if err != nil {
		writeError(w, internalError(err))
		return
	}
	if !linked {
		writeError(w, forbidden())
		return
	}
	var mid, owner, disc int
	err = tx.QueryRow(r.Context(), `SELECT id,teacher_id,discipline_id FROM sisges.class_meeting WHERE class_id=$1 AND meeting_date=$2::date AND schedule_slot=$3 AND deleted_at IS NULL`, id, req.Date, req.Slot).Scan(&mid, &owner, &disc)
	if err == nil {
		if owner != tid || disc != req.DisciplineID {
			writeError(w, forbidden())
			return
		}
		_, err = tx.Exec(r.Context(), `UPDATE sisges.class_meeting SET content=$2,attachments=$3 WHERE id=$1`, mid, req.Content, raw)
	} else if err == pgx.ErrNoRows {
		err = tx.QueryRow(r.Context(), `SELECT s.discipline_id FROM sisges.class_schedule s JOIN sisges.class_discipline cd ON cd.class_id=s.class_id AND cd.discipline_id=s.discipline_id AND cd.deleted_at IS NULL JOIN sisges.discipline d ON d.id=s.discipline_id AND d.deleted_at IS NULL JOIN sisges.discipline_teacher dt ON dt.discipline_id=s.discipline_id AND dt.teacher_id=$4 WHERE s.class_id=$1 AND s.weekday=$2 AND s.slot=$3`, id, int(date.Weekday()), req.Slot, tid).Scan(&disc)
		if err == pgx.ErrNoRows || (err == nil && disc != req.DisciplineID) {
			writeError(w, businessError("A grade mudou ou esta aula não pertence às suas disciplinas. Recarregue o dia."))
			return
		}
		if err != nil {
			writeError(w, internalError(err))
			return
		}
		var overlap bool
		err = tx.QueryRow(r.Context(), `SELECT EXISTS(SELECT 1 FROM sisges.class_meeting WHERE class_id=$1 AND meeting_date=$2::date AND deleted_at IS NULL AND start_time<$4::time AND end_time>$3::time)`, id, req.Date, lessonTimes[req.Slot][0], lessonTimes[req.Slot][1]).Scan(&overlap)
		if err != nil {
			writeError(w, internalError(err))
			return
		}
		if overlap {
			writeError(w, businessError("Já existe uma aula neste intervalo. Consulte a lista de aulas."))
			return
		}
		err = tx.QueryRow(r.Context(), `INSERT INTO sisges.class_meeting(class_id,discipline_id,teacher_id,meeting_date,start_time,end_time,schedule_slot,content,attachments) VALUES($1,$2,$3,$4,$5,$6,$7,$8,$9) RETURNING id`, id, disc, tid, date, lessonTimes[req.Slot][0], lessonTimes[req.Slot][1], req.Slot, req.Content, raw).Scan(&mid)
	}
	if err != nil {
		writeError(w, internalError(err))
		return
	}
	if err = tx.Commit(r.Context()); err != nil {
		writeError(w, internalError(err))
		return
	}
	writeJSON(w, 200, map[string]int{"id": mid})
}
func validLessonAttachment(f lessonAttachment, userID int) bool {
	prefix := fmt.Sprintf("/api/files/lesson-files/%d/", userID)
	if strings.TrimSpace(f.Name) == "" || len(f.Name) > 255 || !strings.HasPrefix(f.Path, prefix) {
		return false
	}
	key := strings.TrimPrefix(f.Path, prefix)
	return key != "" && !strings.ContainsAny(key, "/\\") && !strings.Contains(key, "..")
}
