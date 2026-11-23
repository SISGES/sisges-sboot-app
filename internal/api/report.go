package api

import (
	"context"
	"encoding/json"
	"errors"
	"github.com/jackc/pgx/v5"
	"math"
	"net/http"
	"strconv"
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
	MinPercentage         float64          `json:"minPercentage"`
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
type cycleOption struct {
	ID            int     `json:"id"`
	YearStartDate *string `json:"yearStartDate"`
	YearEndDate   *string `json:"yearEndDate"`
	Status        string  `json:"status"`
}
type finalResult struct {
	BaseScore  float64 `json:"baseScore"`
	FinalScore float64 `json:"finalScore"`
	Approved   bool    `json:"approved"`
	Promoted   bool    `json:"promoted"`
}
type studentReportResponse struct {
	CycleID                 int               `json:"cycleId"`
	AvailableCycles         []cycleOption     `json:"availableCycles"`
	YearResult              *finalResult      `json:"yearResult"`
	FixedApprovalPercentage float64           `json:"fixedApprovalPercentage"`
	YearMaxPoints           float64           `json:"yearMaxPoints"`
	TotalReleasedScore      float64           `json:"totalReleasedScore"`
	EligibleForYearRecovery bool              `json:"eligibleForYearRecovery"`
	Trimesters              []trimesterReport `json:"trimesters"`
	RecoveryRow             recoveryReport    `json:"recoveryRow"`
}

func (a *App) studentReport(w http.ResponseWriter, r *http.Request) {
	var selected int
	if v := r.URL.Query().Get("cycleId"); v != "" {
		id, err := strconv.Atoi(v)
		if err != nil || id < 1 {
			writeError(w, validationError("cycleId", "Ciclo inválido"))
			return
		}
		selected = id
	}
	ctx := r.Context()
	tx, err := a.db.BeginTx(ctx, pgx.TxOptions{IsoLevel: pgx.RepeatableRead, AccessMode: pgx.ReadOnly})
	if err != nil {
		writeError(w, err)
		return
	}
	defer tx.Rollback(ctx)
	var sid int
	if err = tx.QueryRow(ctx, `SELECT id FROM sisges.student WHERE user_id=$1 AND deleted_at IS NULL`, currentPrincipal(r).ID).Scan(&sid); err != nil {
		writeError(w, dbError(err))
		return
	}
	rows, err := tx.Query(ctx, `SELECT c.id,to_char(c.year_start_date,'YYYY-MM-DD'),to_char(c.year_end_date,'YYYY-MM-DD'),c.status
        FROM sisges.academic_cycle c WHERE c.is_current
        OR EXISTS(SELECT 1 FROM sisges.student_year_result y WHERE y.academic_cycle_id=c.id AND y.student_id=$1)
        OR EXISTS(SELECT 1 FROM sisges.activity_grade g JOIN sisges.evaluative_activity ea ON ea.id=g.activity_id WHERE ea.academic_cycle_id=c.id AND g.student_id=$1 AND ea.deleted_at IS NULL)
        ORDER BY c.is_current DESC,c.id DESC`, sid)
	if err != nil {
		writeError(w, err)
		return
	}
	choices, err := pgx.CollectRows(rows, pgx.RowToStructByPos[cycleOption])
	if err != nil {
		writeError(w, err)
		return
	}
	found := false
	for _, c := range choices {
		if selected == 0 {
			selected = c.ID
		}
		if c.ID == selected {
			found = true
		}
	}
	if !found {
		writeError(w, resourceError("Ciclo"))
		return
	}
	var out studentReportResponse
	var final finalResult
	var snapshot []byte
	err = tx.QueryRow(ctx, `SELECT base_score::float8,final_score::float8,approved,promoted,report_snapshot FROM sisges.student_year_result WHERE academic_cycle_id=$1 AND student_id=$2`, selected, sid).Scan(&final.BaseScore, &final.FinalScore, &final.Approved, &final.Promoted, &snapshot)
	hasResult := err == nil
	if err != nil && !errors.Is(err, pgx.ErrNoRows) {
		writeError(w, err)
		return
	}
	if len(snapshot) > 0 {
		err = json.Unmarshal(snapshot, &out)
	} else {
		var cfg gradingConfig
		cfg, err = gradingForCycle(ctx, tx, selected)
		if err == nil {
			out, err = buildReport(ctx, tx, sid, selected, cfg)
		}
	}
	if err != nil {
		writeError(w, err)
		return
	}
	if hasResult {
		out.YearResult = &final
	}
	out.AvailableCycles = choices
	if err = tx.Commit(ctx); err != nil {
		writeError(w, err)
		return
	}
	writeJSON(w, 200, out)
}

func points(value float64) float64 { return math.Round(value*100) / 100 }

func buildReport(ctx context.Context, db schoolQuery, sid, cycle int, cfg gradingConfig) (studentReportResponse, error) {
	out := studentReportResponse{CycleID: cycle, FixedApprovalPercentage: cfg.YearMinPercentage, YearMaxPoints: float64(cfg.YearMaxPoints), Trimesters: make([]trimesterReport, 3), RecoveryRow: recoveryReport{TrimesterRecoveryScores: make([]*float64, 3)}}
	maxes := []int{cfg.Trimester1MaxPoints, cfg.Trimester2MaxPoints, cfg.Trimester3MaxPoints}
	mins := []float64{cfg.Trimester1MinPercentage, cfg.Trimester2MinPercentage, cfg.Trimester3MinPercentage}
	for i := range 3 {
		out.Trimesters[i] = trimesterReport{Trimester: i + 1, TrimesterMaxPoints: float64(maxes[i]), MinPercentage: mins[i], Activities: []reportActivity{}}
	}
	rows, err := db.Query(ctx, `SELECT ea.id,ea.title,ea.activity_type,ea.trimester_number,ea.max_points::float8,ea.released,CASE WHEN ea.released THEN g.score::float8 END
        FROM sisges.activity_grade g JOIN sisges.evaluative_activity ea ON ea.id=g.activity_id
        JOIN sisges.class_meeting cm ON cm.id=ea.class_meeting_id
        WHERE g.student_id=$1 AND ea.academic_cycle_id=$2 AND cm.deleted_at IS NULL AND ea.deleted_at IS NULL ORDER BY ea.created_at,ea.id`, sid, cycle)
	if err != nil {
		return out, err
	}
	defer rows.Close()
	for rows.Next() {
		var cell reportActivity
		var tri *int
		if err = rows.Scan(&cell.ActivityID, &cell.Title, &cell.ActivityType, &tri, &cell.MaxPoints, &cell.Released, &cell.Score); err != nil {
			return out, err
		}
		if cell.ActivityType == "RECUPERACAO_ANUAL" {
			if cell.Released {
				out.RecoveryRow.YearRecoveryScore = cell.Score
			}
			continue
		}
		if tri == nil || *tri < 1 || *tri > 3 {
			continue
		}
		if cell.ActivityType == "RECUPERACAO_TRIMESTRE" {
			if cell.Released {
				out.RecoveryRow.TrimesterRecoveryScores[*tri-1] = cell.Score
			}
			continue
		}
		trimester := &out.Trimesters[*tri-1]
		trimester.Activities = append(trimester.Activities, cell)
		if cell.Score != nil {
			trimester.TotalReleasedScore += *cell.Score
		}
	}
	if err = rows.Err(); err != nil {
		return out, err
	}
	all := true
	for i := range out.Trimesters {
		t := &out.Trimesters[i]
		t.AllActivitiesReleased = len(t.Activities) > 0
		for _, v := range t.Activities {
			if !v.Released {
				t.AllActivitiesReleased = false
				break
			}
		}
		t.TotalReleasedScore = points(t.TotalReleasedScore)
		t.EligibleForRecovery = t.AllActivitiesReleased && t.TotalReleasedScore < points(t.TrimesterMaxPoints*t.MinPercentage/100)
		all = all && t.AllActivitiesReleased
		out.TotalReleasedScore += t.TotalReleasedScore
	}
	out.TotalReleasedScore = points(out.TotalReleasedScore)
	out.EligibleForYearRecovery = all && annualBase(out) < points(out.YearMaxPoints*out.FixedApprovalPercentage/100)
	return out, nil
}

func annualBase(report studentReportResponse) float64 {
	var total float64
	for i, t := range report.Trimesters {
		score := t.TotalReleasedScore
		threshold := points(t.TrimesterMaxPoints * t.MinPercentage / 100)
		recovery := report.RecoveryRow.TrimesterRecoveryScores[i]
		if t.EligibleForRecovery && recovery != nil && *recovery >= points(t.MinPercentage+threshold-score) {
			score = threshold
		}
		total += score
	}
	return points(total)
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
func processYearEnd(ctx context.Context, tx pgx.Tx, cycle int) error {
	cfg, err := gradingForCycle(ctx, tx, cycle)
	if err != nil {
		return err
	}
	// Serializa alterações na matrícula durante o censo e a progressão.
	if _, err = tx.Exec(ctx, `LOCK TABLE sisges.school_class, sisges.student IN SHARE ROW EXCLUSIVE MODE`); err != nil {
		return err
	}
	rows, err := tx.Query(ctx, `SELECT s.id,s.class_id,c.name,c.academic_year
        FROM sisges.student s JOIN sisges.school_class c ON c.id=s.class_id
        WHERE s.deleted_at IS NULL AND c.deleted_at IS NULL
        AND NOT EXISTS(SELECT 1 FROM sisges.student_year_result y WHERE y.student_id=s.id AND y.academic_cycle_id=$1)
        ORDER BY s.id`, cycle)
	if err != nil {
		return err
	}
	type enrolment struct {
		ID, ClassID int
		Name, Year  string
	}
	students, err := pgx.CollectRows(rows, pgx.RowToStructByPos[enrolment])
	if err != nil {
		return err
	}
	threshold := points(float64(cfg.YearMaxPoints) * cfg.YearMinPercentage / 100)
	for _, s := range students {
		report, err := buildReport(ctx, tx, s.ID, cycle, cfg)
		if err != nil {
			return err
		}
		base := annualBase(report)
		final := base
		recovery := report.RecoveryRow.YearRecoveryScore
		if base < threshold && recovery != nil && *recovery >= points(cfg.YearMinPercentage+threshold-base) {
			final = threshold
		}
		approved := final >= threshold
		var nextID *int
		if approved {
			next, valid := nextAcademicYear(s.Year)
			if !valid {
				return businessError("Série atual inválida para progressão: " + s.Year)
			}
			if next != "" {
				var id int
				err = tx.QueryRow(ctx, `SELECT id FROM sisges.school_class WHERE academic_year=$1 AND deleted_at IS NULL ORDER BY CASE WHEN name=$2 THEN 0 ELSE 1 END,name,id LIMIT 1`, next, s.Name).Scan(&id)
				if errors.Is(err, pgx.ErrNoRows) {
					return businessError("Não existe turma de destino para " + s.Name + " (" + s.Year + " -> " + next + ").")
				}
				if err != nil {
					return err
				}
				nextID = &id
			}
			if _, err = tx.Exec(ctx, `UPDATE sisges.student SET class_id=$2,updated_at=now() WHERE id=$1`, s.ID, nextID); err != nil {
				return err
			}
		}
		_, err = tx.Exec(ctx, `INSERT INTO sisges.student_year_result(academic_cycle_id,student_id,source_class_id,next_class_id,base_score,final_score,year_recovery_score,approved,promoted,report_snapshot)
            VALUES($1,$2,$3,$4,$5,$6,$7,$8,$8,$9)`, cycle, s.ID, s.ClassID, nextID, base, final, recovery, approved, report)
		if err != nil {
			return err
		}
	}
	return nil
}
