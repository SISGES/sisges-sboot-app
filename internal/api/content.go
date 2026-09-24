package api

import (
	"net/http"
	"strconv"
	"strings"
	"time"
)

type materialResponse struct {
	ID             int       `json:"id"`
	DisciplineID   int       `json:"disciplineId"`
	DisciplineName string    `json:"disciplineName"`
	ClassID        *int      `json:"classId"`
	ClassName      *string   `json:"className"`
	Title          string    `json:"title"`
	Description    *string   `json:"description"`
	MaterialType   *string   `json:"materialType"`
	FilePath       *string   `json:"filePath"`
	CreatedAt      time.Time `json:"createdAt"`
}

func (a *App) listMaterials(w http.ResponseWriter, r *http.Request) {
	p := currentPrincipal(r)
	classID, _ := strconv.Atoi(r.URL.Query().Get("classId"))
	discID, _ := strconv.Atoi(r.URL.Query().Get("disciplineId"))
	teacherID := 0
	if p.Role == "STUDENT" {
		_ = a.db.QueryRow(r.Context(), `SELECT COALESCE(class_id,0) FROM sisges.student WHERE user_id=$1 AND deleted_at IS NULL`, p.ID).Scan(&classID)
	} else if p.Role == "TEACHER" {
		if classID == 0 {
			writeError(w, validationError("classId", "Turma é obrigatória"))
			return
		}
		teacherID, _ = a.teacherIDForUser(r, p.ID)
	} else if classID == 0 {
		writeError(w, validationError("classId", "Turma é obrigatória"))
		return
	}
	rows, err := a.db.Query(r.Context(), `SELECT m.id,d.id,d.name,c.id,c.name,m.title,m.description,m.material_type,m.file_path,m.created_at FROM sisges.discipline_material m JOIN sisges.discipline d ON d.id=m.discipline_id LEFT JOIN sisges.school_class c ON c.id=m.class_id WHERE m.deleted_at IS NULL AND m.class_id=$1 AND ($2=0 OR m.discipline_id=$2) AND ($3=0 OR EXISTS(SELECT 1 FROM sisges.discipline_teacher dt WHERE dt.discipline_id=m.discipline_id AND dt.teacher_id=$3)) ORDER BY m.created_at DESC LIMIT 1000`, classID, discID, teacherID)
	if err != nil {
		writeError(w, internalError(err))
		return
	}
	defer rows.Close()
	out := make([]materialResponse, 0, 32)
	for rows.Next() {
		var x materialResponse
		if rows.Scan(&x.ID, &x.DisciplineID, &x.DisciplineName, &x.ClassID, &x.ClassName, &x.Title, &x.Description, &x.MaterialType, &x.FilePath, &x.CreatedAt) == nil {
			out = append(out, x)
		}
	}
	writeJSON(w, http.StatusOK, out)
}
func (a *App) createMaterial(w http.ResponseWriter, r *http.Request) {
	var req struct {
		DisciplineID int     `json:"disciplineId"`
		ClassID      int     `json:"classId"`
		Title        string  `json:"title"`
		Description  *string `json:"description"`
		MaterialType *string `json:"materialType"`
		FilePath     *string `json:"filePath"`
	}
	if !decodeJSON(w, r, &req) {
		return
	}
	if strings.TrimSpace(req.Title) == "" {
		writeError(w, validationError("title", "Título é obrigatório"))
		return
	}
	tid, err := a.teacherIDForUser(r, currentPrincipal(r).ID)
	if err != nil {
		writeError(w, businessError("Apenas professores podem criar materiais."))
		return
	}
	var okClass, okDisc bool
	err = a.db.QueryRow(r.Context(), `SELECT EXISTS(SELECT 1 FROM sisges.teacher_class WHERE teacher_id=$1 AND class_id=$2 AND deleted_at IS NULL),EXISTS(SELECT 1 FROM sisges.class_discipline cd JOIN sisges.discipline_teacher dt ON dt.discipline_id=cd.discipline_id WHERE cd.class_id=$2 AND cd.discipline_id=$3 AND cd.deleted_at IS NULL AND dt.teacher_id=$1)`, tid, req.ClassID, req.DisciplineID).Scan(&okClass, &okDisc)
	if err != nil {
		writeError(w, internalError(err))
		return
	}
	if !okClass {
		writeError(w, businessError("O professor não está vinculado à turma."))
		return
	}
	if !okDisc {
		writeError(w, businessError("A disciplina não está vinculada à turma ou ao professor."))
		return
	}
	var id int
	err = a.db.QueryRow(r.Context(), `INSERT INTO sisges.discipline_material(discipline_id,class_id,teacher_id,title,description,material_type,file_path) VALUES($1,$2,$3,$4,$5,$6,$7) RETURNING id`, req.DisciplineID, req.ClassID, tid, strings.TrimSpace(req.Title), req.Description, req.MaterialType, req.FilePath).Scan(&id)
	if err != nil {
		writeError(w, internalError(err))
		return
	}
	var x materialResponse
	err = a.db.QueryRow(r.Context(), `SELECT m.id,d.id,d.name,c.id,c.name,m.title,m.description,m.material_type,m.file_path,m.created_at FROM sisges.discipline_material m JOIN sisges.discipline d ON d.id=m.discipline_id JOIN sisges.school_class c ON c.id=m.class_id WHERE m.id=$1`, id).Scan(&x.ID, &x.DisciplineID, &x.DisciplineName, &x.ClassID, &x.ClassName, &x.Title, &x.Description, &x.MaterialType, &x.FilePath, &x.CreatedAt)
	if err != nil {
		writeError(w, internalError(err))
		return
	}
	writeJSON(w, http.StatusCreated, x)
}
func (a *App) deleteMaterial(w http.ResponseWriter, r *http.Request) {
	id, e := pathInt(r, "id")
	if e != nil {
		writeError(w, e)
		return
	}
	tid, err := a.teacherIDForUser(r, currentPrincipal(r).ID)
	if err != nil {
		writeError(w, forbidden())
		return
	}
	tag, err := a.db.Exec(r.Context(), `UPDATE sisges.discipline_material SET deleted_at=now() WHERE id=$1 AND teacher_id=$2 AND deleted_at IS NULL`, id, tid)
	if err != nil {
		writeError(w, internalError(err))
		return
	}
	if tag.RowsAffected() == 0 {
		writeError(w, businessError("Apenas o professor que criou pode excluir o material."))
		return
	}
	w.WriteHeader(http.StatusNoContent)
}

type announcementResponse struct {
	ID                     int        `json:"id"`
	Title                  string     `json:"title"`
	Content                *string    `json:"content"`
	Type                   string     `json:"type"`
	ImagePath              *string    `json:"imagePath"`
	TargetRoles            []string   `json:"targetRoles"`
	HiddenForRoles         []string   `json:"hiddenForRoles"`
	ActiveFrom             *time.Time `json:"activeFrom"`
	ActiveUntil            *time.Time `json:"activeUntil"`
	CreatedAt              time.Time  `json:"createdAt"`
	LikeCount              int64      `json:"likeCount"`
	LikedByCurrentUser     bool       `json:"likedByCurrentUser"`
	CommentCount           int64      `json:"commentCount"`
	AuthorID               *int       `json:"authorId"`
	AuthorName             *string    `json:"authorName"`
	AuthorProfileImagePath *string    `json:"authorProfileImagePath"`
}

func splitRoles(v *string) []string {
	if v == nil || strings.TrimSpace(*v) == "" {
		return []string{}
	}
	parts := strings.Split(*v, ",")
	out := make([]string, 0, len(parts))
	for _, p := range parts {
		if p = strings.TrimSpace(p); p != "" {
			out = append(out, p)
		}
	}
	return out
}
func (a *App) readAnnouncements(r *http.Request, where string, args ...any) ([]announcementResponse, error) {
	uid := 0
	if p := a.optionalPrincipal(r); p != nil {
		uid = p.ID
	}
	args = append(args, uid)
	uidPos := len(args)
	q := `SELECT a.id,a.title,a.content,a.type,a.image_path,a.target_roles,a.hidden_for_roles,a.active_from,a.active_until,a.created_at,(SELECT count(*) FROM sisges.announcement_like l WHERE l.announcement_id=a.id),(SELECT count(*)>0 FROM sisges.announcement_like l WHERE l.announcement_id=a.id AND l.user_id=$` + strconv.Itoa(uidPos) + `),(SELECT count(*) FROM sisges.announcement_comment c WHERE c.announcement_id=a.id AND c.deleted_at IS NULL),u.id,u.name,u.profile_image_path FROM sisges.announcement a LEFT JOIN sisges.users u ON u.id=a.created_by WHERE a.deleted_at IS NULL AND ` + where + ` ORDER BY a.created_at DESC LIMIT 500`
	rows, err := a.db.Query(r.Context(), q, args...)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	out := make([]announcementResponse, 0, 16)
	for rows.Next() {
		var x announcementResponse
		var target, hidden *string
		if err = rows.Scan(&x.ID, &x.Title, &x.Content, &x.Type, &x.ImagePath, &target, &hidden, &x.ActiveFrom, &x.ActiveUntil, &x.CreatedAt, &x.LikeCount, &x.LikedByCurrentUser, &x.CommentCount, &x.AuthorID, &x.AuthorName, &x.AuthorProfileImagePath); err != nil {
			return nil, err
		}
		x.TargetRoles = splitRoles(target)
		x.HiddenForRoles = splitRoles(hidden)
		out = append(out, x)
	}
	return out, rows.Err()
}
func (a *App) announcementFeed(w http.ResponseWriter, r *http.Request) {
	role := "STUDENT"
	if p := a.optionalPrincipal(r); p != nil {
		role = p.Role
	}
	out, err := a.readAnnouncements(r, `(a.active_from IS NULL OR a.active_from<=now()) AND (a.active_until IS NULL OR a.active_until>=now()) AND (a.hidden_for_roles IS NULL OR NOT ($1=ANY(string_to_array(a.hidden_for_roles,','))))`, role)
	if err != nil {
		writeError(w, internalError(err))
		return
	}
	writeJSON(w, http.StatusOK, out)
}
func (a *App) allAnnouncements(w http.ResponseWriter, r *http.Request) {
	out, err := a.readAnnouncements(r, "true")
	if err != nil {
		writeError(w, internalError(err))
		return
	}
	writeJSON(w, http.StatusOK, out)
}
func (a *App) announcementByID(w http.ResponseWriter, r *http.Request) {
	id, e := pathInt(r, "id")
	if e != nil {
		writeError(w, e)
		return
	}
	out, err := a.readAnnouncements(r, "a.id=$1", id)
	if err != nil {
		writeError(w, internalError(err))
		return
	}
	if len(out) == 0 {
		writeError(w, resourceError("Anúncio"))
		return
	}
	writeJSON(w, http.StatusOK, out[0])
}

type announcementRequest struct {
	Title          *string    `json:"title"`
	Content        *string    `json:"content"`
	Type           *string    `json:"type"`
	ImagePath      *string    `json:"imagePath"`
	TargetRoles    []string   `json:"targetRoles"`
	HiddenForRoles []string   `json:"hiddenForRoles"`
	ActiveFrom     *time.Time `json:"activeFrom"`
	ActiveUntil    *time.Time `json:"activeUntil"`
	TTLHours       *int       `json:"ttlHours"`
}

func (a *App) createAnnouncement(w http.ResponseWriter, r *http.Request) {
	var req announcementRequest
	if !decodeJSON(w, r, &req) {
		return
	}
	if req.Title == nil || strings.TrimSpace(*req.Title) == "" {
		writeError(w, validationError("title", "Título é obrigatório"))
		return
	}
	if req.Type == nil || (*req.Type != "TEXT" && *req.Type != "IMAGE") {
		writeError(w, validationError("type", "Tipo deve ser TEXT ou IMAGE"))
		return
	}
	if req.TTLHours != nil {
		ok := false
		for _, h := range []int{1, 4, 10, 24, 48, 168} {
			if *req.TTLHours == h {
				ok = true
			}
		}
		if !ok {
			writeError(w, validationError("ttlHours", "TTL inválido"))
			return
		}
	}
	from := time.Now()
	if req.ActiveFrom != nil {
		from = *req.ActiveFrom
	}
	until := req.ActiveUntil
	if req.TTLHours != nil {
		v := from.Add(time.Duration(*req.TTLHours) * time.Hour)
		until = &v
	}
	hidden := strings.Join(req.HiddenForRoles, ",")
	target := strings.Join(req.TargetRoles, ",")
	if target == "" {
		target = "ADMIN,TEACHER,STUDENT"
	}
	var id int
	err := a.db.QueryRow(r.Context(), `INSERT INTO sisges.announcement(title,content,type,image_path,target_roles,hidden_for_roles,active_from,active_until,created_by) VALUES($1,$2,$3,$4,$5,NULLIF($6,''),$7,$8,$9) RETURNING id`, strings.TrimSpace(*req.Title), req.Content, *req.Type, req.ImagePath, target, hidden, from, until, currentPrincipal(r).ID).Scan(&id)
	if err != nil {
		writeError(w, internalError(err))
		return
	}
	a.feed.broadcast("ANNOUNCEMENT_CREATED", id)
	out, err := a.readAnnouncements(r, "a.id=$1", id)
	if err != nil {
		writeError(w, internalError(err))
		return
	}
	writeJSON(w, http.StatusCreated, out[0])
}
func (a *App) updateAnnouncement(w http.ResponseWriter, r *http.Request) {
	id, e := pathInt(r, "id")
	if e != nil {
		writeError(w, e)
		return
	}
	var req announcementRequest
	if !decodeJSON(w, r, &req) {
		return
	}
	target, hidden := (*string)(nil), (*string)(nil)
	if req.TargetRoles != nil {
		s := strings.Join(req.TargetRoles, ",")
		target = &s
	}
	if req.HiddenForRoles != nil {
		s := strings.Join(req.HiddenForRoles, ",")
		hidden = &s
	}
	tag, err := a.db.Exec(r.Context(), `UPDATE sisges.announcement SET title=COALESCE($2,title),content=COALESCE($3,content),type=COALESCE($4,type),image_path=COALESCE($5,image_path),target_roles=COALESCE($6,target_roles),hidden_for_roles=CASE WHEN $7::text IS NULL THEN hidden_for_roles ELSE NULLIF($7,'') END,active_from=COALESCE($8,active_from),active_until=COALESCE($9,active_until),updated_at=now() WHERE id=$1 AND deleted_at IS NULL`, id, req.Title, req.Content, req.Type, req.ImagePath, target, hidden, req.ActiveFrom, req.ActiveUntil)
	if err != nil {
		writeError(w, internalError(err))
		return
	}
	if tag.RowsAffected() == 0 {
		writeError(w, resourceError("Anúncio"))
		return
	}
	a.feed.broadcast("ANNOUNCEMENT_UPDATED", id)
	out, err := a.readAnnouncements(r, "a.id=$1", id)
	if err != nil {
		writeError(w, internalError(err))
		return
	}
	writeJSON(w, http.StatusOK, out[0])
}
func (a *App) deleteAnnouncement(w http.ResponseWriter, r *http.Request) {
	id, e := pathInt(r, "id")
	if e != nil {
		writeError(w, e)
		return
	}
	var imagePath *string
	if err := a.db.QueryRow(r.Context(), `SELECT image_path FROM sisges.announcement WHERE id=$1 AND deleted_at IS NULL`, id).Scan(&imagePath); err != nil {
		writeError(w, resourceError("Anúncio"))
		return
	}
	tag, err := a.db.Exec(r.Context(), `DELETE FROM sisges.announcement WHERE id=$1`, id)
	if err != nil {
		writeError(w, internalError(err))
		return
	}
	if tag.RowsAffected() == 0 {
		writeError(w, resourceError("Anúncio"))
		return
	}
	a.feed.broadcast("ANNOUNCEMENT_DELETED", id)
	a.deleteStoredPath(r.Context(), imagePath)
	w.WriteHeader(http.StatusNoContent)
}
func (a *App) toggleLike(w http.ResponseWriter, r *http.Request) {
	id, e := pathInt(r, "id")
	if e != nil {
		writeError(w, e)
		return
	}
	uid := currentPrincipal(r).ID
	tag, err := a.db.Exec(r.Context(), `DELETE FROM sisges.announcement_like WHERE announcement_id=$1 AND user_id=$2`, id, uid)
	if err != nil {
		writeError(w, internalError(err))
		return
	}
	liked := false
	if tag.RowsAffected() == 0 {
		_, err = a.db.Exec(r.Context(), `INSERT INTO sisges.announcement_like(announcement_id,user_id) VALUES($1,$2)`, id, uid)
		liked = err == nil
	}
	if err != nil {
		writeError(w, internalError(err))
		return
	}
	writeJSON(w, http.StatusOK, struct {
		Liked bool `json:"liked"`
	}{liked})
}

type commentResponse struct {
	ID        int       `json:"id"`
	Content   string    `json:"content"`
	CreatedAt time.Time `json:"createdAt"`
	User      struct {
		ID   int    `json:"id"`
		Name string `json:"name"`
	} `json:"user"`
}

func (a *App) comments(r *http.Request, id int) ([]commentResponse, error) {
	rows, err := a.db.Query(r.Context(), `SELECT c.id,c.content,c.created_at,u.id,u.name FROM sisges.announcement_comment c JOIN sisges.users u ON u.id=c.user_id WHERE c.announcement_id=$1 AND c.deleted_at IS NULL ORDER BY c.created_at`, id)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	out := []commentResponse{}
	for rows.Next() {
		var x commentResponse
		if err = rows.Scan(&x.ID, &x.Content, &x.CreatedAt, &x.User.ID, &x.User.Name); err != nil {
			return nil, err
		}
		out = append(out, x)
	}
	return out, rows.Err()
}
func (a *App) announcementComments(w http.ResponseWriter, r *http.Request) {
	id, e := pathInt(r, "id")
	if e != nil {
		writeError(w, e)
		return
	}
	out, err := a.comments(r, id)
	if err != nil {
		writeError(w, internalError(err))
		return
	}
	writeJSON(w, http.StatusOK, out)
}
func (a *App) createComment(w http.ResponseWriter, r *http.Request) {
	id, e := pathInt(r, "id")
	if e != nil {
		writeError(w, e)
		return
	}
	var req struct {
		Content string `json:"content"`
	}
	if !decodeJSON(w, r, &req) {
		return
	}
	if strings.TrimSpace(req.Content) == "" || len(req.Content) > 250 {
		writeError(w, validationError("content", "Conteúdo do comentário é obrigatório"))
		return
	}
	var x commentResponse
	err := a.db.QueryRow(r.Context(), `INSERT INTO sisges.announcement_comment(announcement_id,user_id,content) VALUES($1,$2,$3) RETURNING id,content,created_at`, id, currentPrincipal(r).ID, strings.TrimSpace(req.Content)).Scan(&x.ID, &x.Content, &x.CreatedAt)
	if err != nil {
		writeError(w, internalError(err))
		return
	}
	x.User.ID = currentPrincipal(r).ID
	_ = a.db.QueryRow(r.Context(), `SELECT name FROM sisges.users WHERE id=$1`, x.User.ID).Scan(&x.User.Name)
	writeJSON(w, http.StatusCreated, x)
}
func (a *App) updateComment(w http.ResponseWriter, r *http.Request) {
	aid, e := pathInt(r, "id")
	if e != nil {
		writeError(w, e)
		return
	}
	cid, e := pathInt(r, "commentID")
	if e != nil {
		writeError(w, e)
		return
	}
	var req struct {
		Content string `json:"content"`
	}
	if !decodeJSON(w, r, &req) {
		return
	}
	var x commentResponse
	err := a.db.QueryRow(r.Context(), `UPDATE sisges.announcement_comment SET content=$4 WHERE id=$1 AND announcement_id=$2 AND user_id=$3 AND deleted_at IS NULL RETURNING id,content,created_at,user_id`, cid, aid, currentPrincipal(r).ID, strings.TrimSpace(req.Content)).Scan(&x.ID, &x.Content, &x.CreatedAt, &x.User.ID)
	if err != nil {
		writeError(w, forbidden())
		return
	}
	_ = a.db.QueryRow(r.Context(), `SELECT name FROM sisges.users WHERE id=$1`, x.User.ID).Scan(&x.User.Name)
	writeJSON(w, http.StatusOK, x)
}
func (a *App) deleteComment(w http.ResponseWriter, r *http.Request) {
	aid, e := pathInt(r, "id")
	if e != nil {
		writeError(w, e)
		return
	}
	cid, e := pathInt(r, "commentID")
	if e != nil {
		writeError(w, e)
		return
	}
	tag, err := a.db.Exec(r.Context(), `UPDATE sisges.announcement_comment SET deleted_at=now() WHERE id=$1 AND announcement_id=$2 AND user_id=$3 AND deleted_at IS NULL`, cid, aid, currentPrincipal(r).ID)
	if err != nil {
		writeError(w, internalError(err))
		return
	}
	if tag.RowsAffected() == 0 {
		writeError(w, forbidden())
		return
	}
	w.WriteHeader(http.StatusNoContent)
}
func (a *App) announcementLikes(w http.ResponseWriter, r *http.Request) {
	id, e := pathInt(r, "id")
	if e != nil {
		writeError(w, e)
		return
	}
	rows, err := a.db.Query(r.Context(), `SELECT u.id,u.name FROM sisges.announcement_like l JOIN sisges.users u ON u.id=l.user_id WHERE l.announcement_id=$1 ORDER BY l.created_at`, id)
	if err != nil {
		writeError(w, internalError(err))
		return
	}
	defer rows.Close()
	type u struct {
		ID   int    `json:"id"`
		Name string `json:"name"`
	}
	liked := []u{}
	for rows.Next() {
		var x u
		if rows.Scan(&x.ID, &x.Name) == nil {
			liked = append(liked, x)
		}
	}
	writeJSON(w, http.StatusOK, struct {
		Count   int `json:"count"`
		LikedBy []u `json:"likedBy"`
	}{len(liked), liked})
}

type eventResponse struct {
	ID            int       `json:"id"`
	Title         string    `json:"title"`
	Description   *string   `json:"description"`
	EventAt       time.Time `json:"eventAt"`
	Audience      string    `json:"audience"`
	ClassID       *int      `json:"classId"`
	ClassName     *string   `json:"className"`
	CreatedByID   *int      `json:"createdById"`
	CreatedByName *string   `json:"createdByName"`
	CreatedAt     time.Time `json:"createdAt"`
}

func (a *App) listEvents(w http.ResponseWriter, r *http.Request) {
	p := currentPrincipal(r)
	includePast := p.Role == "ADMIN" && r.URL.Query().Get("includePast") == "true"
	rows, err := a.db.Query(r.Context(), `SELECT e.id,e.title,e.description,e.event_at,e.audience,c.id,c.name,CASE WHEN $2='ADMIN' THEN u.id END,CASE WHEN $2='ADMIN' THEN u.name END,e.created_at FROM sisges.school_event e LEFT JOIN sisges.school_class c ON c.id=e.class_id JOIN sisges.users u ON u.id=e.created_by WHERE ($3 OR e.event_at>=now()) AND ($2='ADMIN' OR e.audience='ALL' OR (e.audience='TEACHERS' AND $2='TEACHER') OR (e.audience='CLASS' AND (($2='STUDENT' AND EXISTS(SELECT 1 FROM sisges.student s WHERE s.user_id=$1 AND s.class_id=e.class_id AND s.deleted_at IS NULL)) OR ($2='TEACHER' AND EXISTS(SELECT 1 FROM sisges.teacher t JOIN sisges.teacher_class tc ON tc.teacher_id=t.id WHERE t.user_id=$1 AND tc.class_id=e.class_id AND tc.deleted_at IS NULL))))) ORDER BY CASE WHEN e.event_at>=now() THEN 0 ELSE 1 END, CASE WHEN e.event_at>=now() THEN e.event_at END ASC, CASE WHEN e.event_at<now() THEN e.event_at END DESC LIMIT 500`, p.ID, p.Role, includePast)
	if err != nil {
		writeError(w, internalError(err))
		return
	}
	defer rows.Close()
	out := []eventResponse{}
	for rows.Next() {
		var x eventResponse
		if rows.Scan(&x.ID, &x.Title, &x.Description, &x.EventAt, &x.Audience, &x.ClassID, &x.ClassName, &x.CreatedByID, &x.CreatedByName, &x.CreatedAt) == nil {
			out = append(out, x)
		}
	}
	writeJSON(w, http.StatusOK, out)
}
func (a *App) createEvent(w http.ResponseWriter, r *http.Request) {
	var req struct {
		Title       string    `json:"title"`
		Description *string   `json:"description"`
		EventAt     time.Time `json:"eventAt"`
		Audience    string    `json:"audience"`
		ClassID     *int      `json:"classId"`
	}
	if !decodeJSON(w, r, &req) {
		return
	}
	if strings.TrimSpace(req.Title) == "" || !req.EventAt.After(time.Now()) {
		writeError(w, validationError("eventAt", "Evento deve estar no futuro"))
		return
	}
	if req.Audience != "ALL" && req.Audience != "TEACHERS" && req.Audience != "CLASS" {
		writeError(w, validationError("audience", "Público inválido"))
		return
	}
	if req.Audience == "CLASS" && req.ClassID == nil {
		writeError(w, validationError("classId", "Turma é obrigatória"))
		return
	}
	var id int
	err := a.db.QueryRow(r.Context(), `INSERT INTO sisges.school_event(title,description,event_at,audience,class_id,created_by) VALUES($1,$2,$3,$4,$5,$6) RETURNING id`, strings.TrimSpace(req.Title), req.Description, req.EventAt, req.Audience, req.ClassID, currentPrincipal(r).ID).Scan(&id)
	if err != nil {
		writeError(w, internalError(err))
		return
	}
	var x eventResponse
	err = a.db.QueryRow(r.Context(), `SELECT e.id,e.title,e.description,e.event_at,e.audience,c.id,c.name,u.id,u.name,e.created_at FROM sisges.school_event e LEFT JOIN sisges.school_class c ON c.id=e.class_id JOIN sisges.users u ON u.id=e.created_by WHERE e.id=$1`, id).Scan(&x.ID, &x.Title, &x.Description, &x.EventAt, &x.Audience, &x.ClassID, &x.ClassName, &x.CreatedByID, &x.CreatedByName, &x.CreatedAt)
	if err != nil {
		writeError(w, internalError(err))
		return
	}
	writeJSON(w, http.StatusCreated, x)
}
func (a *App) deleteEvent(w http.ResponseWriter, r *http.Request) {
	id, e := pathInt(r, "id")
	if e != nil {
		writeError(w, e)
		return
	}
	tag, err := a.db.Exec(r.Context(), `DELETE FROM sisges.school_event WHERE id=$1`, id)
	if err != nil {
		writeError(w, internalError(err))
		return
	}
	if tag.RowsAffected() == 0 {
		writeError(w, resourceError("Evento"))
		return
	}
	w.WriteHeader(http.StatusNoContent)
}
