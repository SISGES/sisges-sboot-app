-- SISGES - Materiais de disciplina com arquivo, turma e professor
-- Migration V10
-- Professor insere materiais (pdf, txt, docx) para turma e disciplina específicas

ALTER TABLE sisges.discipline_material
    ADD COLUMN IF NOT EXISTS file_path VARCHAR(500);

ALTER TABLE sisges.discipline_material
    ADD COLUMN IF NOT EXISTS class_id INTEGER;

ALTER TABLE sisges.discipline_material
    ADD COLUMN IF NOT EXISTS teacher_id INTEGER;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_discipline_material_class'
    ) THEN
        ALTER TABLE sisges.discipline_material
            ADD CONSTRAINT fk_discipline_material_class
            FOREIGN KEY (class_id) REFERENCES sisges.school_class (id) ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_discipline_material_teacher'
    ) THEN
        ALTER TABLE sisges.discipline_material
            ADD CONSTRAINT fk_discipline_material_teacher
            FOREIGN KEY (teacher_id) REFERENCES sisges.teacher (id) ON DELETE SET NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_discipline_material_class ON sisges.discipline_material (class_id);
CREATE INDEX IF NOT EXISTS idx_discipline_material_teacher ON sisges.discipline_material (teacher_id);
