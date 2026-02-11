-- Regra de negócio: telefone e e-mail alternativos do responsável são opcionais
ALTER TABLE sisges.student_responsible
    ALTER COLUMN alternative_phone DROP NOT NULL,
    ALTER COLUMN alternative_email DROP NOT NULL;
