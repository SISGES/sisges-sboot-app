package api

import (
	"net/http"
	"time"
)

type reportActivity struct {
	ActivityID   int      `json:"activityId"`
	Title        string   `json:"title"`
	ActivityType string   `json:"activityType"`
	MaxPoints    float64  `json:"maxPoints"`
	Score        *float64 `json:"score"`
	Released     bool     `json:"released"`
}
type trimesterReport struct {
	Trimester             int              `json:"trimester"`
	TrimesterMaxPoints    float64          `json:"trimesterMaxPoints"`
	Activities            []reportActivity `json:"activities"`
	TotalReleasedScore    float64          `json:"totalReleasedScore"`
	AllActivitiesReleased bool             `json:"allActivitiesReleased"`
	EligibleForRecovery   bool             `json:"eligibleForRecovery"`
}
type recoveryReport struct {
	TrimesterRecoveryScores []*float64 `json:"trimesterRecoveryScores"`
	YearRecoveryScore       *float64   `json:"yearRecoveryScore"`
}
type studentReportResponse struct {
	FixedApprovalPercentage float64           `json:"fixedApprovalPercentage"`
	YearMaxPoints           float64           `json:"yearMaxPoints"`
	TotalReleasedScore      float64           `json:"totalReleasedScore"`
	EligibleForYearRecovery bool              `json:"eligibleForYearRecovery"`
	Trimesters              []trimesterReport `json:"trimesters"`
	RecoveryRow             recoveryReport    `json:"recoveryRow"`
}

func (a *App) studentReport(w http.ResponseWriter, r *http.Request) {
	var sid, cid int
	e := a.db.QueryRow(r.Context(), `SELECT s.id,COALESCE(s.class_id,0) FROM sisges.student s WHERE s.user_id=$1 AND s.deleted_at IS NULL`, currentPrincipal(r).ID).Scan(&sid, &cid)
	if e != nil {
		writeError(w, resourceError("Aluno"))
		return
	}
	cfg, e := a.readGrading(r)
	if e != nil {
		writeError(w, internalError(e))
		return
	}
	out := studentReportResponse{FixedApprovalPercentage: 70, YearMaxPoints: float64(cfg.YearMaxPoints), Trimesters: make([]trimesterReport, 3), RecoveryRow: recoveryReport{TrimesterRecoveryScores: make([]*float64, 3)}}
	maxes := []int{cfg.Trimester1MaxPoints, cfg.Trimester2MaxPoints, cfg.Trimester3MaxPoints}
	for i := range 3 {
		out.Trimesters[i] = trimesterReport{Trimester: i + 1, TrimesterMaxPoints: float64(maxes[i]), Activities: []reportActivity{}}
	}
	if cid == 0 {
		writeJSON(w, http.StatusOK, out)
		return
	}
	rows, e := a.db.Query(r.Context(), `SELECT ea.id,ea.title,ea.activity_type,ea.trimester_number,ea.max_points::float8,ea.released,CASE WHEN ea.released THEN g.score::float8 END,ea.created_at FROM sisges.evaluative_activity ea JOIN sisges.class_meeting cm ON cm.id=ea.class_meeting_id LEFT JOIN sisges.activity_grade g ON g.activity_id=ea.id AND g.student_id=$1 WHERE cm.class_id=$2 AND cm.deleted_at IS NULL AND ea.deleted_at IS NULL ORDER BY ea.created_at`, sid, cid)
	if e != nil {
		writeError(w, internalError(e))
		return
	}
	defer rows.Close()
	var recoveryDates [3]time.Time
	var annualDate time.Time
	for rows.Next() {
		var id int
		var tri *int
		var title, kind string
		var max float64
		var released bool
		var score *float64
		var created time.Time
		if e = rows.Scan(&id, &title, &kind, &tri, &max, &released, &score, &created); e != nil {
			continue
		}
		if kind == "RECUPERACAO_TRIMESTRE" && released && tri != nil && *tri >= 1 && *tri <= 3 && created.After(recoveryDates[*tri-1]) {
			out.RecoveryRow.TrimesterRecoveryScores[*tri-1] = score
			recoveryDates[*tri-1] = created
			continue
		}
		if kind == "RECUPERACAO_ANUAL" && released && created.After(annualDate) {
			out.RecoveryRow.YearRecoveryScore = score
			annualDate = created
			continue
		}
		if tri == nil || *tri < 1 || *tri > 3 || (kind != "PROVA" && kind != "ATIVIDADE" && kind != "TRABALHO") {
			continue
		}
		cell := reportActivity{id, title, kind, max, nil, released}
		if released {
			cell.Score = score
			if score != nil {
				out.Trimesters[*tri-1].TotalReleasedScore += *score
			}
		}
		out.Trimesters[*tri-1].Activities = append(out.Trimesters[*tri-1].Activities, cell)
	}
	for i := range out.Trimesters {
		t := &out.Trimesters[i]
		t.AllActivitiesReleased = len(t.Activities) > 0
		for _, x := range t.Activities {
			if !x.Released {
				t.AllActivitiesReleased = false
			}
		}
		t.EligibleForRecovery = t.AllActivitiesReleased && t.TotalReleasedScore < t.TrimesterMaxPoints*.7
		out.TotalReleasedScore += t.TotalReleasedScore
	}
	all := true
	for _, t := range out.Trimesters {
		if !t.AllActivitiesReleased {
			all = false
		}
	}
	out.EligibleForYearRecovery = all && out.TotalReleasedScore < out.YearMaxPoints*.7
	writeJSON(w, http.StatusOK, out)
}

var academicYears = []string{"1º ano - Fundamental", "2º ano - Fundamental", "3º ano - Fundamental", "4º ano - Fundamental", "5º ano - Fundamental", "6º ano", "7º ano", "8º ano", "9º ano", "1º ano - Médio", "2º ano - Médio", "3º ano - Médio"}

func nextAcademicYear(current string) (string, bool) {
	for i, y := range academicYears {
		if y == current {
			if i == len(academicYears)-1 {
				return "", true
			}
			return academicYears[i+1], true
		}
	}
	return "", false
}
func (a *App) processYearEnd(r *http.Request) error {
	tx, e := a.db.Begin(r.Context())
	if e != nil {
		return internalError(e)
	}
	defer tx.Rollback(r.Context())
	var cycle int
	var yearMax, trimester1Max, trimester2Max, trimester3Max float64
	e = tx.QueryRow(r.Context(), `SELECT id FROM sisges.academic_cycle ORDER BY id LIMIT 1`).Scan(&cycle)
	if e != nil {
		return internalError(e)
	}
	var exists bool
	_ = tx.QueryRow(r.Context(), `SELECT EXISTS(SELECT 1 FROM sisges.student_year_result WHERE academic_cycle_id=$1)`, cycle).Scan(&exists)
	if exists {
		return tx.Commit(r.Context())
	}
	e = tx.QueryRow(r.Context(), `SELECT year_max_points::float8,trimester1_max_points::float8,trimester2_max_points::float8,trimester3_max_points::float8 FROM sisges.grading_config ORDER BY id LIMIT 1`).Scan(&yearMax, &trimester1Max, &trimester2Max, &trimester3Max)
	if e != nil {
		return internalError(e)
	}
	rows, e := tx.Query(r.Context(), `SELECT s.id,s.class_id,c.name,c.academic_year FROM sisges.student s JOIN sisges.school_class c ON c.id=s.class_id WHERE s.deleted_at IS NULL AND c.deleted_at IS NULL ORDER BY s.id LIMIT 10000`)
	if e != nil {
		return internalError(e)
	}
	type student struct {
		id, cid    int
		name, year string
	}
	students := make([]student, 0, 256)
	for rows.Next() {
		var s student
		if e = rows.Scan(&s.id, &s.cid, &s.name, &s.year); e != nil {
			rows.Close()
			return internalError(e)
		}
		students = append(students, s)
	}
	rows.Close()
	if e = rows.Err(); e != nil {
		return internalError(e)
	}
	threshold := yearMax * .7
	trimesterMax := [3]float64{trimester1Max, trimester2Max, trimester3Max}
	for _, s := range students {
		var base float64
		scoreRows, queryErr := tx.Query(r.Context(), `WITH normal AS (
            SELECT ea.trimester_number AS trimester,
                   COALESCE(sum(CASE WHEN ea.released THEN COALESCE(g.score,0) ELSE 0 END),0)::float8 AS base,
                   count(*)::int AS activity_count,
                   bool_and(ea.released) AS all_released
            FROM sisges.evaluative_activity ea
            JOIN sisges.class_meeting cm ON cm.id=ea.class_meeting_id
            LEFT JOIN sisges.activity_grade g ON g.activity_id=ea.id AND g.student_id=$1
            WHERE cm.class_id=$2 AND cm.deleted_at IS NULL AND ea.deleted_at IS NULL
              AND ea.activity_type IN ('PROVA','ATIVIDADE','TRABALHO')
            GROUP BY ea.trimester_number
        ), recovery AS (
            SELECT DISTINCT ON (ea.trimester_number) ea.trimester_number AS trimester, g.score::float8 AS score
            FROM sisges.evaluative_activity ea
            JOIN sisges.class_meeting cm ON cm.id=ea.class_meeting_id
            LEFT JOIN sisges.activity_grade g ON g.activity_id=ea.id AND g.student_id=$1
            WHERE cm.class_id=$2 AND cm.deleted_at IS NULL AND ea.deleted_at IS NULL
              AND ea.released AND ea.activity_type='RECUPERACAO_TRIMESTRE'
            ORDER BY ea.trimester_number,ea.created_at DESC
        ) SELECT series.trimester,COALESCE(normal.base,0),COALESCE(normal.activity_count,0),COALESCE(normal.all_released,false),recovery.score
          FROM generate_series(1,3) AS series(trimester)
          LEFT JOIN normal USING(trimester) LEFT JOIN recovery USING(trimester)
          ORDER BY series.trimester`, s.id, s.cid)
		if queryErr != nil {
			return internalError(queryErr)
		}
		for scoreRows.Next() {
			var tri, count int
			var trimesterBase float64
			var allReleased bool
			var recoveryScore *float64
			if e = scoreRows.Scan(&tri, &trimesterBase, &count, &allReleased, &recoveryScore); e != nil {
				scoreRows.Close()
				return internalError(e)
			}
			triThreshold := trimesterMax[tri-1] * .7
			if trimesterBase < triThreshold && count > 0 && allReleased && recoveryScore != nil && *recoveryScore >= 70+(triThreshold-trimesterBase) {
				trimesterBase = triThreshold
			}
			base += trimesterBase
		}
		scoreRows.Close()
		e = scoreRows.Err()
		if e != nil {
			return internalError(e)
		}
		var recovery *float64
		_ = tx.QueryRow(r.Context(), `SELECT g.score::float8 FROM sisges.activity_grade g JOIN sisges.evaluative_activity ea ON ea.id=g.activity_id JOIN sisges.class_meeting cm ON cm.id=ea.class_meeting_id WHERE g.student_id=$1 AND cm.class_id=$2 AND ea.deleted_at IS NULL AND ea.released AND ea.activity_type='RECUPERACAO_ANUAL' ORDER BY ea.created_at DESC LIMIT 1`, s.id, s.cid).Scan(&recovery)
		approved := base >= threshold
		final := base
		if !approved && recovery != nil && *recovery >= 70+(threshold-base) {
			approved = true
			final = threshold
		}
		var nextID *int
		promoted := false
		if approved {
			next, valid := nextAcademicYear(s.year)
			if !valid {
				return businessError("Série atual inválida para progressão: " + s.year)
			}
			promoted = true
			if next != "" {
				var id int
				e = tx.QueryRow(r.Context(), `SELECT id FROM sisges.school_class WHERE academic_year=$1 AND deleted_at IS NULL ORDER BY CASE WHEN name=$2 THEN 0 ELSE 1 END,name LIMIT 1`, next, s.name).Scan(&id)
				if e != nil {
					return businessError("Não existe turma de destino para " + s.name + " (" + s.year + " -> " + next + ").")
				}
				nextID = &id
			}
			if _, e = tx.Exec(r.Context(), `UPDATE sisges.student SET class_id=$2,updated_at=now() WHERE id=$1`, s.id, nextID); e != nil {
				return internalError(e)
			}
		}
		_, e = tx.Exec(r.Context(), `INSERT INTO sisges.student_year_result(academic_cycle_id,student_id,source_class_id,next_class_id,base_score,final_score,year_recovery_score,approved,promoted) VALUES($1,$2,$3,$4,$5,$6,$7,$8,$9)`, cycle, s.id, s.cid, nextID, base, final, recovery, approved, promoted)
		if e != nil {
			return internalError(e)
		}
	}
	if e = tx.Commit(r.Context()); e != nil {
		return internalError(e)
	}
	return nil
}
