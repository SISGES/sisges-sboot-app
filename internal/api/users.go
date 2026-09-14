package api

import (
	"fmt"
	"net/http"
	"strings"
	"time"

	"github.com/jackc/pgx/v5"
	"golang.org/x/crypto/bcrypt"
)

type loginRequest struct {
	Email    string `json:"email"`
	Password string `json:"password"`
}
type userResponse struct {
	ID               int     `json:"id"`
	Name             string  `json:"name"`
	Email            string  `json:"email"`
	Register         string  `json:"register"`
	Role             string  `json:"role"`
	BirthDate        string  `json:"birthDate"`
	Gender           string  `json:"gender"`
	ProfileImagePath *string `json:"profileImagePath"`
}
type loginResponse struct {
	AccessToken string       `json:"accessToken"`
	TokenType   string       `json:"tokenType"`
	User        userResponse `json:"user"`
}
type responsibleData struct {
	Name             string  `json:"name"`
	Phone            string  `json:"phone"`
	AlternativePhone *string `json:"alternativePhone"`
	Email            string  `json:"email"`
	AlternativeEmail *string `json:"alternativeEmail"`
}
type registerRequest struct {
	Name            string           `json:"name"`
	Password        string           `json:"password"`
	BirthDate       string           `json:"birthDate"`
	Gender          string           `json:"gender"`
	Role            string           `json:"role"`
	ResponsibleID   *int             `json:"responsibleId"`
	ClassID         *int             `json:"classId"`
	ResponsibleData *responsibleData `json:"responsibleData"`
}

func (a *App) login(w http.ResponseWriter, r *http.Request) {
	var req loginRequest
	if !decodeJSON(w, r, &req) {
		return
	}
	var u userResponse
	var password string
	var birth time.Time
	err := a.db.QueryRow(r.Context(), `SELECT id,name,email,register,user_role,birth_date,gender,profile_image_path,password FROM sisges.users WHERE lower(email)=lower($1) AND deleted_at IS NULL`, strings.TrimSpace(req.Email)).Scan(&u.ID, &u.Name, &u.Email, &u.Register, &u.Role, &birth, &u.Gender, &u.ProfileImagePath, &password)
	if err != nil || bcrypt.CompareHashAndPassword([]byte(password), []byte(req.Password)) != nil {
		// Kept for compatibility with the original API, whose invalid-login body
		// contains status=401 while the HTTP response itself is 200.
		writeJSON(w, http.StatusOK, apiErr(401, "AUTH_INVALID_CREDENTIALS", "E-mail ou senha inválidos"))
		return
	}
	u.BirthDate = birth.Format("2006-01-02")
	token, err := a.signToken(principal{ID: u.ID, Email: u.Email, Role: u.Role})
	if err != nil {
		writeError(w, internalError(err))
		return
	}
	writeJSON(w, http.StatusOK, loginResponse{token, "Bearer", u})
}
func (a *App) validateToken(w http.ResponseWriter, _ *http.Request) { w.WriteHeader(http.StatusOK) }

func validateRegistration(req registerRequest) *apiError {
	if strings.TrimSpace(req.Name) == "" {
		return validationError("name", "Nome é obrigatório")
	}
	if len(req.Password) < 6 {
		return validationError("password", "Senha deve ter no mínimo 6 caracteres")
	}
	if _, err := time.Parse("2006-01-02", req.BirthDate); err != nil {
		return validationError("birthDate", "Data de nascimento inválida")
	}
	if req.Gender == "" {
		return validationError("gender", "Gênero é obrigatório")
	}
	if req.Role != "ADMIN" && req.Role != "TEACHER" && req.Role != "STUDENT" {
		return validationError("role", "Papel deve ser ADMIN, TEACHER ou STUDENT")
	}
	if req.Role == "STUDENT" && req.ResponsibleID == nil && req.ResponsibleData == nil {
		return businessError("Aluno deve ter um responsável legal (informe responsibleId ou responsibleData).")
	}
	return nil
}

func (a *App) register(w http.ResponseWriter, r *http.Request) {
	var req registerRequest
	if !decodeJSON(w, r, &req) {
		return
	}
	if err := validateRegistration(req); err != nil {
		writeError(w, err)
		return
	}
	birth, _ := time.Parse("2006-01-02", req.BirthDate)
	hash, err := bcrypt.GenerateFromPassword([]byte(req.Password), bcrypt.DefaultCost)
	if err != nil {
		writeError(w, internalError(err))
		return
	}
	tx, err := a.db.Begin(r.Context())
	if err != nil {
		writeError(w, internalError(err))
		return
	}
	defer tx.Rollback(r.Context())
	prefix, digits := "A", 5
	if req.Role == "TEACHER" {
		prefix, digits = "P", 4
	} else if req.Role == "ADMIN" {
		prefix, digits = "ADM", 4
	}
	_, err = tx.Exec(r.Context(), `SELECT pg_advisory_xact_lock(hashtext($1))`, "register-"+prefix)
	if err != nil {
		writeError(w, internalError(err))
		return
	}
	var last *string
	err = tx.QueryRow(r.Context(), `SELECT max(register) FROM sisges.users WHERE register ~ ('^' || $1 || '[0-9]+$')`, prefix).Scan(&last)
	if err != nil {
		writeError(w, internalError(err))
		return
	}
	next := 1
	if last != nil {
		var n int
		if _, e := fmt.Sscanf((*last)[len(prefix):], "%d", &n); e == nil {
			next = n + 1
		}
	}
	reg := fmt.Sprintf("%s%0*d", prefix, digits, next)
	email := strings.ToLower(reg) + "@sisges.com"
	var u userResponse
	err = tx.QueryRow(r.Context(), `INSERT INTO sisges.users(name,email,register,password,birth_date,gender,user_role) VALUES($1,$2,$3,$4,$5,$6,$7) RETURNING id,name,email,register,user_role,birth_date,gender,profile_image_path`, strings.TrimSpace(req.Name), email, reg, string(hash), birth, req.Gender, req.Role).Scan(&u.ID, &u.Name, &u.Email, &u.Register, &u.Role, &birth, &u.Gender, &u.ProfileImagePath)
	if err != nil {
		writeError(w, apiErr(409, "DATA_CONFLICT", "Conflito de dados. Verifique se os dados não estão duplicados ou inconsistentes"))
		return
	}
	u.BirthDate = birth.Format("2006-01-02")
	if req.Role == "TEACHER" {
		_, err = tx.Exec(r.Context(), `INSERT INTO sisges.teacher(user_id) VALUES($1)`, u.ID)
	}
	if req.Role == "STUDENT" {
		var respID int
		if req.ResponsibleData != nil {
			d := req.ResponsibleData
			err = tx.QueryRow(r.Context(), `INSERT INTO sisges.student_responsible(name,phone,alternative_phone,email,alternative_email) VALUES($1,$2,$3,$4,$5) RETURNING id`, strings.TrimSpace(d.Name), d.Phone, d.AlternativePhone, d.Email, d.AlternativeEmail).Scan(&respID)
		} else {
			respID = *req.ResponsibleID
			var ok bool
			err = tx.QueryRow(r.Context(), `SELECT EXISTS(SELECT 1 FROM sisges.student_responsible WHERE id=$1 AND deleted_at IS NULL)`, respID).Scan(&ok)
			if err == nil && !ok {
				err = errNotFound
			}
		}
		if err == nil {
			var sid int
			err = tx.QueryRow(r.Context(), `INSERT INTO sisges.student(user_id,class_id) VALUES($1,$2) RETURNING id`, u.ID, req.ClassID).Scan(&sid)
			if err == nil {
				_, err = tx.Exec(r.Context(), `INSERT INTO sisges.student_responsible_link(student_id,responsible_id) VALUES($1,$2)`, sid, respID)
			}
		}
	}
	if err != nil {
		if err == errNotFound {
			writeError(w, resourceError("Responsável"))
		} else {
			writeError(w, internalError(err))
		}
		return
	}
	if err = tx.Commit(r.Context()); err != nil {
		writeError(w, internalError(err))
		return
	}
	writeJSON(w, http.StatusCreated, u)
}

func scanUser(row pgx.Row) (userResponse, error) {
	var u userResponse
	var birth time.Time
	err := row.Scan(&u.ID, &u.Name, &u.Email, &u.Register, &u.Role, &birth, &u.Gender, &u.ProfileImagePath)
	u.BirthDate = birth.Format("2006-01-02")
	return u, err
}
func (a *App) user(w http.ResponseWriter, r *http.Request, id int) {
	u, err := scanUser(a.db.QueryRow(r.Context(), `SELECT id,name,email,register,user_role,birth_date,gender,profile_image_path FROM sisges.users WHERE id=$1 AND deleted_at IS NULL`, id))
	if err != nil {
		writeError(w, resourceError("Usuário"))
		return
	}
	writeJSON(w, http.StatusOK, u)
}
func (a *App) me(w http.ResponseWriter, r *http.Request) { a.user(w, r, currentPrincipal(r).ID) }
func (a *App) userByID(w http.ResponseWriter, r *http.Request) {
	id, err := pathInt(r, "id")
	if err != nil {
		writeError(w, err)
		return
	}
	a.user(w, r, id)
}

type updateProfileRequest struct {
	Name             *string `json:"name"`
	Password         *string `json:"password"`
	ProfileImagePath *string `json:"profileImagePath"`
}

func (a *App) updateMe(w http.ResponseWriter, r *http.Request) {
	p := currentPrincipal(r)
	if p.Role == "STUDENT" {
		writeError(w, forbidden())
		return
	}
	var req updateProfileRequest
	if !decodeJSON(w, r, &req) {
		return
	}
	if req.Name != nil && (len(strings.TrimSpace(*req.Name)) < 2 || len(*req.Name) > 255) {
		writeError(w, validationError("name", "Nome deve ter entre 2 e 255 caracteres"))
		return
	}
	if req.Password != nil && len(*req.Password) < 8 {
		writeError(w, validationError("password", "Senha deve ter ao menos 8 caracteres"))
		return
	}
	var hash *string
	if req.Password != nil && *req.Password != "" {
		b, err := bcrypt.GenerateFromPassword([]byte(*req.Password), bcrypt.DefaultCost)
		if err != nil {
			writeError(w, internalError(err))
			return
		}
		s := string(b)
		hash = &s
	}
	var previousImage *string
	if req.ProfileImagePath != nil {
		_ = a.db.QueryRow(r.Context(), `SELECT profile_image_path FROM sisges.users WHERE id=$1`, p.ID).Scan(&previousImage)
	}
	_, err := a.db.Exec(r.Context(), `UPDATE sisges.users SET name=COALESCE(NULLIF(trim($2),''),name),password=COALESCE($3,password),profile_image_path=CASE WHEN $4::text IS NULL THEN profile_image_path ELSE NULLIF($4,'') END,updated_at=now() WHERE id=$1 AND deleted_at IS NULL`, p.ID, req.Name, hash, req.ProfileImagePath)
	if err != nil {
		writeError(w, internalError(err))
		return
	}
	if req.ProfileImagePath != nil && previousImage != nil && *previousImage != *req.ProfileImagePath {
		a.deleteStoredPath(r.Context(), previousImage)
	}
	a.user(w, r, p.ID)
}

type searchUsersRequest struct{ Name, Email, Register, Gender, InitialDate, FinalDate string }
type userSearchResponse struct {
	ID    int    `json:"id"`
	Name  string `json:"name"`
	Email string `json:"email"`
	Role  string `json:"role"`
}

func (a *App) searchUsers(w http.ResponseWriter, r *http.Request) {
	var req struct {
		Name        string `json:"name"`
		Email       string `json:"email"`
		Register    string `json:"register"`
		Gender      string `json:"gender"`
		InitialDate string `json:"initialDate"`
		FinalDate   string `json:"finalDate"`
	}
	if r.ContentLength != 0 && !decodeJSON(w, r, &req) {
		return
	}
	rows, err := a.db.Query(r.Context(), `SELECT id,name,email,user_role FROM sisges.users WHERE deleted_at IS NULL AND ($1='' OR name ILIKE '%'||$1||'%') AND ($2='' OR email ILIKE '%'||$2||'%') AND ($3='' OR register ILIKE '%'||$3||'%') AND ($4='' OR gender=$4) AND ($5='' OR birth_date>=NULLIF($5,'')::date) AND ($6='' OR birth_date<=NULLIF($6,'')::date) ORDER BY name LIMIT 1000`, req.Name, req.Email, req.Register, req.Gender, req.InitialDate, req.FinalDate)
	if err != nil {
		writeError(w, internalError(err))
		return
	}
	defer rows.Close()
	out := make([]userSearchResponse, 0, 64)
	for rows.Next() {
		var x userSearchResponse
		if err = rows.Scan(&x.ID, &x.Name, &x.Email, &x.Role); err != nil {
			writeError(w, internalError(err))
			return
		}
		out = append(out, x)
	}
	writeJSON(w, http.StatusOK, out)
}

type personSearch struct {
	ID    int    `json:"id"`
	Name  string `json:"name"`
	Email string `json:"email"`
}

func (a *App) searchPeople(w http.ResponseWriter, r *http.Request, table string) {
	var req struct {
		Name  string `json:"name"`
		Email string `json:"email"`
	}
	if r.ContentLength != 0 && !decodeJSON(w, r, &req) {
		return
	}
	q := `SELECT p.id,u.name,u.email FROM sisges.` + table + ` p JOIN sisges.users u ON u.id=p.user_id WHERE p.deleted_at IS NULL AND u.deleted_at IS NULL AND ($1='' OR u.name ILIKE '%'||$1||'%') AND ($2='' OR u.email ILIKE '%'||$2||'%') ORDER BY u.name LIMIT 1000`
	rows, err := a.db.Query(r.Context(), q, req.Name, req.Email)
	if err != nil {
		writeError(w, internalError(err))
		return
	}
	defer rows.Close()
	out := make([]personSearch, 0, 64)
	for rows.Next() {
		var x personSearch
		if err := rows.Scan(&x.ID, &x.Name, &x.Email); err != nil {
			writeError(w, internalError(err))
			return
		}
		out = append(out, x)
	}
	writeJSON(w, http.StatusOK, out)
}
func (a *App) searchTeachers(w http.ResponseWriter, r *http.Request) { a.searchPeople(w, r, "teacher") }
func (a *App) searchStudents(w http.ResponseWriter, r *http.Request) { a.searchPeople(w, r, "student") }

type classSimple struct {
	ID           int    `json:"id"`
	Name         string `json:"name"`
	AcademicYear string `json:"academicYear"`
}
type teacherDetail struct {
	ID        int           `json:"id"`
	Name      string        `json:"name"`
	Email     string        `json:"email"`
	Register  string        `json:"register"`
	BirthDate string        `json:"birthDate"`
	Gender    string        `json:"gender"`
	Classes   []classSimple `json:"classes"`
}

func (a *App) teacherDetail(w http.ResponseWriter, r *http.Request, id int, byUser bool) {
	column := "t.id"
	if byUser {
		column = "t.user_id"
	}
	var x teacherDetail
	var birth time.Time
	err := a.db.QueryRow(r.Context(), `SELECT t.id,u.name,u.email,u.register,u.birth_date,u.gender FROM sisges.teacher t JOIN sisges.users u ON u.id=t.user_id WHERE `+column+`=$1 AND t.deleted_at IS NULL AND u.deleted_at IS NULL`, id).Scan(&x.ID, &x.Name, &x.Email, &x.Register, &birth, &x.Gender)
	if err != nil {
		writeError(w, resourceError("Professor"))
		return
	}
	x.BirthDate = birth.Format("2006-01-02")
	x.Classes = []classSimple{}
	rows, err := a.db.Query(r.Context(), `SELECT c.id,c.name,c.academic_year FROM sisges.teacher_class tc JOIN sisges.school_class c ON c.id=tc.class_id WHERE tc.teacher_id=$1 AND tc.deleted_at IS NULL AND c.deleted_at IS NULL ORDER BY c.name`, x.ID)
	if err == nil {
		defer rows.Close()
		for rows.Next() {
			var c classSimple
			if rows.Scan(&c.ID, &c.Name, &c.AcademicYear) == nil {
				x.Classes = append(x.Classes, c)
			}
		}
	}
	writeJSON(w, http.StatusOK, x)
}
func (a *App) teacherMe(w http.ResponseWriter, r *http.Request) {
	a.teacherDetail(w, r, currentPrincipal(r).ID, true)
}
func (a *App) teacherByID(w http.ResponseWriter, r *http.Request) {
	id, e := pathInt(r, "id")
	if e != nil {
		writeError(w, e)
		return
	}
	a.teacherDetail(w, r, id, false)
}
