-- A versão anterior consultava sempre o ciclo de menor id.
ALTER TABLE sisges.academic_cycle
    ADD COLUMN is_current boolean NOT NULL DEFAULT false,
    ADD COLUMN grading_policy jsonb;
UPDATE sisges.academic_cycle SET is_current = true
 WHERE id = (SELECT min(id) FROM sisges.academic_cycle);
CREATE UNIQUE INDEX academic_cycle_current ON sisges.academic_cycle (is_current) WHERE is_current;

ALTER TABLE sisges.student_year_result ADD COLUMN report_snapshot jsonb;
ALTER TABLE sisges.evaluative_activity ADD COLUMN academic_cycle_id integer REFERENCES sisges.academic_cycle(id);
CREATE INDEX evaluative_activity_cycle ON sisges.evaluative_activity (academic_cycle_id, trimester_number) WHERE deleted_at IS NULL;

-- Se o registro foi reutilizado, preserve os resultados antigos no id original e
-- copie o estado do último ano para outro id. Datas antigas perdidas ficam nulas.
DO $$
DECLARE old_cycle sisges.academic_cycle%ROWTYPE;
DECLARE last_result timestamp;
BEGIN
    SELECT * INTO old_cycle FROM sisges.academic_cycle WHERE is_current;
    SELECT max(created_at) INTO last_result FROM sisges.student_year_result WHERE academic_cycle_id=old_cycle.id;
    IF last_result < old_cycle.year_started_at THEN
        UPDATE sisges.academic_cycle SET is_current=false,status='FINISHED',grading_locked=false,
            year_start_date=NULL, year_started_at=NULL, year_end_date=last_result::date,
            year_finished_at=last_result WHERE id=old_cycle.id;
        INSERT INTO sisges.academic_cycle (status,year_start_date,year_end_date,current_trimester,grading_locked,
            year_started_at,year_finished_at,is_current)
        VALUES (old_cycle.status,old_cycle.year_start_date,old_cycle.year_end_date,old_cycle.current_trimester,
            old_cycle.grading_locked,old_cycle.year_started_at,old_cycle.year_finished_at,true);
    END IF;
END $$;

-- Nos anos já iniciados o código anterior aplicava efetivamente 70%, inclusive
-- quando os percentuais exibidos na configuração eram outros.
UPDATE sisges.academic_cycle c SET grading_policy = to_jsonb(g) || jsonb_build_object(
    'year_min_percentage',70,'trimester1_min_percentage',70,
    'trimester2_min_percentage',70,'trimester3_min_percentage',70),
    grading_locked=(c.status='IN_PROGRESS')
FROM (SELECT * FROM sisges.grading_config ORDER BY id LIMIT 1) g
WHERE c.status <> 'NOT_STARTED';

-- Atribuição de atividades anteriores exige um intervalo de datas inequívoco.
-- Dados sem evidência suficiente continuam consultáveis pela aula, sem participar
-- do novo ano. Não é possível reconstruir anos que nunca foram persistidos.
WITH candidates AS (
    SELECT ea.id AS activity_id, min(c.id) AS cycle_id
    FROM sisges.evaluative_activity ea
    JOIN sisges.class_meeting cm ON cm.id=ea.class_meeting_id
    JOIN sisges.academic_cycle c ON cm.meeting_date BETWEEN c.year_start_date AND c.year_end_date
        AND c.status <> 'NOT_STARTED'
    GROUP BY ea.id HAVING count(*)=1
)
UPDATE sisges.evaluative_activity ea SET academic_cycle_id=c.cycle_id
FROM candidates c WHERE c.activity_id=ea.id;

-- Cada linha de nota também identifica um aluno da lista daquela atividade.
-- Preservar notas existentes inclusive após transferência. Para anos encerrados,
-- a turma de origem do resultado é a evidência disponível de matrícula.
INSERT INTO sisges.activity_grade(activity_id,student_id)
SELECT ea.id,r.student_id FROM sisges.evaluative_activity ea
JOIN sisges.class_meeting cm ON cm.id=ea.class_meeting_id
JOIN sisges.student_year_result r ON r.academic_cycle_id=ea.academic_cycle_id AND r.source_class_id=cm.class_id
WHERE ea.deleted_at IS NULL
ON CONFLICT(activity_id,student_id) DO NOTHING;

INSERT INTO sisges.activity_grade(activity_id,student_id)
SELECT ea.id,s.id FROM sisges.evaluative_activity ea
JOIN sisges.academic_cycle c ON c.id=ea.academic_cycle_id AND c.status='IN_PROGRESS'
JOIN sisges.class_meeting cm ON cm.id=ea.class_meeting_id
JOIN sisges.student s ON s.class_id=cm.class_id AND s.deleted_at IS NULL
WHERE ea.deleted_at IS NULL
ON CONFLICT(activity_id,student_id) DO NOTHING;
