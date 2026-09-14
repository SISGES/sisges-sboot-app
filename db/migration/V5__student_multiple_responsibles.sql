-- SISGES - Migration V5: Student com múltiplos responsáveis (N:N)
-- Altera a relação 1:N para N:N entre student e student_responsible

-- Criar tabela de ligação student x responsible
CREATE TABLE sisges.student_responsible_link (
    student_id     INTEGER NOT NULL,
    responsible_id INTEGER NOT NULL,
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (student_id, responsible_id),
    CONSTRAINT fk_student_responsible_link_student    FOREIGN KEY (student_id)     REFERENCES sisges.student (id) ON DELETE CASCADE,
    CONSTRAINT fk_student_responsible_link_responsible FOREIGN KEY (responsible_id) REFERENCES sisges.student_responsible (id) ON DELETE CASCADE
);

CREATE INDEX idx_student_responsible_link_student ON sisges.student_responsible_link (student_id);
CREATE INDEX idx_student_responsible_link_responsible ON sisges.student_responsible_link (responsible_id);

-- Migrar dados existentes da relação 1:N para N:N
INSERT INTO sisges.student_responsible_link (student_id, responsible_id)
SELECT id, responsible_id
FROM sisges.student
WHERE responsible_id IS NOT NULL;

-- Remover coluna antiga responsible_id de student
ALTER TABLE sisges.student DROP CONSTRAINT IF EXISTS fk_student_responsible;
ALTER TABLE sisges.student DROP COLUMN IF EXISTS responsible_id;
