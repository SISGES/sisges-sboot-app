-- V8: Disciplina pode ter vários professores (N:N)

-- Tabela de associação discipline_teacher
CREATE TABLE sisges.discipline_teacher (
    discipline_id INTEGER NOT NULL,
    teacher_id    INTEGER NOT NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (discipline_id, teacher_id),
    CONSTRAINT fk_discipline_teacher_discipline FOREIGN KEY (discipline_id) REFERENCES sisges.discipline (id) ON DELETE CASCADE,
    CONSTRAINT fk_discipline_teacher_teacher    FOREIGN KEY (teacher_id)    REFERENCES sisges.teacher (id) ON DELETE CASCADE
);

-- Migrar dados existentes de discipline.teacher_id
INSERT INTO sisges.discipline_teacher (discipline_id, teacher_id)
SELECT id, teacher_id FROM sisges.discipline
WHERE teacher_id IS NOT NULL
ON CONFLICT (discipline_id, teacher_id) DO NOTHING;

-- Remover coluna teacher_id da disciplina
ALTER TABLE sisges.discipline DROP CONSTRAINT IF EXISTS fk_discipline_teacher;
DROP INDEX IF EXISTS sisges.idx_discipline_teacher_id;
ALTER TABLE sisges.discipline DROP COLUMN IF EXISTS teacher_id;
