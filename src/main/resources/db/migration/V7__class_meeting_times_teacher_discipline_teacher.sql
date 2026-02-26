-- V7: Adiciona horário e professor às aulas; professor opcional na disciplina

-- Disciplina: professor opcional
ALTER TABLE sisges.discipline
    ADD COLUMN IF NOT EXISTS teacher_id INTEGER;

ALTER TABLE sisges.discipline
    ADD CONSTRAINT fk_discipline_teacher
    FOREIGN KEY (teacher_id) REFERENCES sisges.teacher (id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_discipline_teacher_id ON sisges.discipline (teacher_id);

-- Class meeting: horários e professor
ALTER TABLE sisges.class_meeting
    ADD COLUMN IF NOT EXISTS start_time TIME;

ALTER TABLE sisges.class_meeting
    ADD COLUMN IF NOT EXISTS end_time TIME;

ALTER TABLE sisges.class_meeting
    ADD COLUMN IF NOT EXISTS teacher_id INTEGER;

ALTER TABLE sisges.class_meeting
    ADD CONSTRAINT fk_class_meeting_teacher
    FOREIGN KEY (teacher_id) REFERENCES sisges.teacher (id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_class_meeting_teacher_id ON sisges.class_meeting (teacher_id);

-- Para registros existentes, definir valores default (podem ser atualizados depois)
UPDATE sisges.class_meeting SET start_time = '08:00'::time WHERE start_time IS NULL;
UPDATE sisges.class_meeting SET end_time = '09:00'::time WHERE end_time IS NULL;

ALTER TABLE sisges.class_meeting
    ALTER COLUMN start_time SET NOT NULL;

ALTER TABLE sisges.class_meeting
    ALTER COLUMN end_time SET NOT NULL;
