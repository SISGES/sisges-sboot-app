package api

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"log/slog"
	"net/http"
	"runtime/debug"
	"strings"
	"time"

	"github.com/jackc/pgx/v5/pgxpool"
)

type App struct {
	db      *pgxpool.Pool
	cfg     Config
	storage *R2Storage
	feed    *feedHub
	router  http.Handler
}

func New(ctx context.Context, cfg Config) (*App, error) {
	poolCfg, err := pgxpool.ParseConfig(cfg.DatabaseURL)
	if err != nil {
		return nil, fmt.Errorf("parse database URL: %w", err)
	}
	poolCfg.MaxConns = cfg.DBMaxConnections
	poolCfg.MinConns = 0
	poolCfg.MaxConnIdleTime = 2 * time.Minute
	poolCfg.MaxConnLifetime = 30 * time.Minute
	poolCfg.HealthCheckPeriod = 30 * time.Second
	db, err := pgxpool.NewWithConfig(ctx, poolCfg)
	if err != nil {
		return nil, fmt.Errorf("create database pool: %w", err)
	}
	if err := db.Ping(ctx); err != nil {
		db.Close()
		return nil, fmt.Errorf("connect to database: %w", err)
	}
	a := &App{db: db, cfg: cfg, feed: newFeedHub(256)}
	if cfg.R2Endpoint != "" {
		a.storage, err = NewR2Storage(cfg)
		if err != nil {
			db.Close()
			return nil, err
		}
	}
	if err := a.migrate(ctx); err != nil {
		db.Close()
		return nil, err
	}
	if cfg.SeedEnabled {
		if err := a.seed(ctx); err != nil {
			db.Close()
			return nil, fmt.Errorf("seed database: %w", err)
		}
	}
	a.router = a.routes()
	go a.expiryLoop(ctx)
	return a, nil
}

func (a *App) Close()                { a.db.Close() }
func (a *App) Handler() http.Handler { return a.router }

func (a *App) routes() http.Handler {
	mux := http.NewServeMux()
	mux.HandleFunc("GET /health", a.health)
	mux.HandleFunc("GET /ws", a.webSocketFeed)
	mux.HandleFunc("POST /api/auth/login", a.login)
	mux.HandleFunc("POST /api/auth/register", a.require("ADMIN", a.register))
	mux.HandleFunc("GET /api/auth/validate", a.require("", a.validateToken))
	mux.HandleFunc("GET /api/users/me", a.require("", a.me))
	mux.HandleFunc("PATCH /api/users/me", a.require("", a.updateMe))
	mux.HandleFunc("POST /api/users/search", a.require("ADMIN", a.searchUsers))
	mux.HandleFunc("GET /api/users/{id}", a.require("ADMIN", a.userByID))
	mux.HandleFunc("POST /api/teachers/search", a.require("ADMIN", a.searchTeachers))
	mux.HandleFunc("GET /api/teachers/me", a.require("TEACHER", a.teacherMe))
	mux.HandleFunc("GET /api/teachers/{id}", a.require("ADMIN", a.teacherByID))
	mux.HandleFunc("POST /api/students/search", a.require("ADMIN", a.searchStudents))
	mux.HandleFunc("GET /api/students/me/turma", a.require("STUDENT", a.studentMyClass))
	mux.HandleFunc("GET /api/students/me/faltas-por-disciplina", a.require("STUDENT", a.studentAbsences))
	mux.HandleFunc("GET /api/students/{id}", a.require("ADMIN", a.studentByID))

	mux.HandleFunc("POST /api/classes", a.require("ADMIN", a.createClass))
	mux.HandleFunc("POST /api/classes/search", a.requireAny([]string{"ADMIN", "TEACHER"}, a.searchClasses))
	mux.HandleFunc("GET /api/classes/{id}", a.requireAny([]string{"ADMIN", "TEACHER"}, a.classByID))
	mux.HandleFunc("DELETE /api/classes/delete/{id}", a.require("ADMIN", a.deleteClass))
	mux.HandleFunc("POST /api/classes/{classID}/teacher/add/{id}", a.require("ADMIN", a.classRelation))
	mux.HandleFunc("POST /api/classes/{classID}/teacher/remove/{id}", a.require("ADMIN", a.classRelation))
	mux.HandleFunc("POST /api/classes/{classID}/student/add/{id}", a.require("ADMIN", a.classRelation))
	mux.HandleFunc("POST /api/classes/{classID}/student/remove/{id}", a.require("ADMIN", a.classRelation))
	mux.HandleFunc("POST /api/classes/{classID}/discipline/add/{id}", a.require("ADMIN", a.classRelation))
	mux.HandleFunc("POST /api/classes/{classID}/discipline/remove/{id}", a.require("ADMIN", a.classRelation))

	mux.HandleFunc("GET /api/disciplines", a.requireAny([]string{"ADMIN", "TEACHER"}, a.listDisciplines))
	mux.HandleFunc("POST /api/disciplines", a.require("ADMIN", a.createDiscipline))
	mux.HandleFunc("GET /api/disciplines/{id}", a.requireAny([]string{"ADMIN", "TEACHER"}, a.disciplineByID))
	mux.HandleFunc("PUT /api/disciplines/update/{id}", a.require("ADMIN", a.updateDiscipline))

	mux.HandleFunc("POST /api/class", a.require("TEACHER", a.createMeeting))
	mux.HandleFunc("POST /api/class/search", a.requireAny([]string{"ADMIN", "TEACHER"}, a.searchMeetings))
	mux.HandleFunc("GET /api/class/{id}", a.requireAny([]string{"ADMIN", "TEACHER"}, a.meetingByID))
	mux.HandleFunc("PUT /api/class/update/{id}", a.requireAny([]string{"ADMIN", "TEACHER"}, a.updateMeeting))
	mux.HandleFunc("DELETE /api/class/delete/{id}", a.require("ADMIN", a.deleteMeeting))
	mux.HandleFunc("POST /api/class/{id}/frequency", a.requireAny([]string{"ADMIN", "TEACHER"}, a.saveFrequency))

	mux.HandleFunc("GET /api/materials", a.require("", a.listMaterials))
	mux.HandleFunc("POST /api/materials", a.require("TEACHER", a.createMaterial))
	mux.HandleFunc("DELETE /api/materials/{id}", a.require("TEACHER", a.deleteMaterial))
	mux.HandleFunc("GET /api/activities/meeting/{id}", a.requireAny([]string{"ADMIN", "TEACHER"}, a.activitiesByMeeting))
	mux.HandleFunc("GET /api/activities/my", a.require("STUDENT", a.studentActivities))
	mux.HandleFunc("POST /api/activities", a.requireAny([]string{"ADMIN", "TEACHER"}, a.createActivity))
	mux.HandleFunc("DELETE /api/activities/{id}", a.requireAny([]string{"ADMIN", "TEACHER"}, a.deleteActivity))
	mux.HandleFunc("GET /api/activities/{rest...}", a.requireAny([]string{"ADMIN", "TEACHER"}, a.routeActivityGet))
	mux.HandleFunc("PUT /api/activities/{id}/grades", a.requireAny([]string{"ADMIN", "TEACHER"}, a.saveGrades))
	mux.HandleFunc("POST /api/activities/{id}/release", a.requireAny([]string{"ADMIN", "TEACHER"}, a.releaseActivity))

	mux.HandleFunc("GET /api/announcements/feed", a.announcementFeed)
	mux.HandleFunc("GET /api/announcements", a.require("ADMIN", a.allAnnouncements))
	mux.HandleFunc("GET /api/announcements/{id}", a.require("ADMIN", a.announcementByID))
	mux.HandleFunc("POST /api/announcements", a.requireAny([]string{"ADMIN", "TEACHER"}, a.createAnnouncement))
	mux.HandleFunc("PUT /api/announcements/{id}", a.require("ADMIN", a.updateAnnouncement))
	mux.HandleFunc("DELETE /api/announcements/{id}", a.require("ADMIN", a.deleteAnnouncement))
	mux.HandleFunc("POST /api/announcements/{id}/like", a.require("", a.toggleLike))
	mux.HandleFunc("GET /api/announcements/{id}/likes", a.require("", a.announcementLikes))
	mux.HandleFunc("GET /api/announcements/{id}/comments", a.require("", a.announcementComments))
	mux.HandleFunc("POST /api/announcements/{id}/comments", a.require("", a.createComment))
	mux.HandleFunc("PUT /api/announcements/{id}/comments/{commentID}", a.require("", a.updateComment))
	mux.HandleFunc("DELETE /api/announcements/{id}/comments/{commentID}", a.require("", a.deleteComment))

	mux.HandleFunc("GET /api/grading-config", a.requireAny([]string{"ADMIN", "TEACHER"}, a.getGradingConfig))
	mux.HandleFunc("PUT /api/grading-config", a.require("ADMIN", a.updateGradingConfig))
	mux.HandleFunc("GET /api/academic-cycle", a.require("", a.getAcademicCycle))
	mux.HandleFunc("POST /api/academic-cycle/start-year", a.require("ADMIN", a.startAcademicYear))
	mux.HandleFunc("POST /api/academic-cycle/end-trimester", a.require("ADMIN", a.endTrimester))
	mux.HandleFunc("POST /api/academic-cycle/end-year", a.require("ADMIN", a.endYear))
	mux.HandleFunc("GET /api/academic-cycle/pending-releases", a.require("ADMIN", a.pendingReleases))
	mux.HandleFunc("GET /api/boletim/me", a.require("STUDENT", a.studentReport))
	mux.HandleFunc("GET /api/events", a.require("", a.listEvents))
	mux.HandleFunc("POST /api/events", a.require("ADMIN", a.createEvent))
	mux.HandleFunc("DELETE /api/events/{id}", a.require("ADMIN", a.deleteEvent))
	mux.HandleFunc("POST /api/upload", a.requireAny([]string{"ADMIN", "TEACHER"}, a.uploadFile))
	mux.HandleFunc("GET /api/files/{key...}", a.require("", a.downloadFile))
	return a.recoverer(a.cors(mux))
}

func (a *App) health(w http.ResponseWriter, r *http.Request) {
	ctx, cancel := context.WithTimeout(r.Context(), time.Second)
	defer cancel()
	if err := a.db.Ping(ctx); err != nil {
		writeError(w, internalError(err))
		return
	}
	writeJSON(w, http.StatusOK, struct {
		Status string `json:"status"`
	}{"ok"})
}

func writeJSON(w http.ResponseWriter, status int, value any) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(value)
}

func decodeJSON(w http.ResponseWriter, r *http.Request, dst any) bool {
	r.Body = http.MaxBytesReader(w, r.Body, 1<<20)
	dec := json.NewDecoder(r.Body)
	if err := dec.Decode(dst); err != nil {
		writeError(w, validationError("body", "JSON inválido"))
		return false
	}
	if dec.Decode(&struct{}{}) == nil {
		writeError(w, validationError("body", "Somente um objeto JSON é permitido"))
		return false
	}
	return true
}

func (a *App) recoverer(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		defer func() {
			if v := recover(); v != nil {
				slog.Error("panic", "value", v, "stack", string(debug.Stack()))
				writeError(w, internalError(fmt.Errorf("panic: %v", v)))
			}
		}()
		next.ServeHTTP(w, r)
	})
}

func (a *App) cors(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		origin := r.Header.Get("Origin")
		if origin != "" && a.originAllowed(origin) {
			w.Header().Set("Access-Control-Allow-Origin", origin)
			w.Header().Set("Vary", "Origin")
			w.Header().Set("Access-Control-Allow-Credentials", "true")
			w.Header().Set("Access-Control-Allow-Headers", "Authorization, Content-Type")
			w.Header().Set("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS")
			w.Header().Set("Access-Control-Expose-Headers", "*")
		}
		if r.Method == http.MethodOptions {
			w.WriteHeader(http.StatusNoContent)
			return
		}
		next.ServeHTTP(w, r)
	})
}

func (a *App) originAllowed(origin string) bool {
	for _, p := range a.cfg.AllowedOrigins {
		if p == origin || (strings.Contains(p, "*") && wildcardMatch(p, origin)) {
			return true
		}
	}
	return false
}
func wildcardMatch(pattern, value string) bool {
	parts := strings.SplitN(pattern, "*", 2)
	return strings.HasPrefix(value, parts[0]) && strings.HasSuffix(value, parts[1])
}

var errNotFound = errors.New("not found")
