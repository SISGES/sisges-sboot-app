-- SISGES - Avisos: visível a todos por padrão, opção "ocultar para"
-- Migration V13
-- hidden_for_roles: roles que NÃO verão o aviso (ex: STUDENT = oculto para alunos)
-- target_roles: tornar nullable para compatibilidade

ALTER TABLE sisges.announcement
    ADD COLUMN IF NOT EXISTS hidden_for_roles VARCHAR(100);

ALTER TABLE sisges.announcement
    ALTER COLUMN target_roles DROP NOT NULL;

COMMENT ON COLUMN sisges.announcement.hidden_for_roles IS 'Comma-separated roles that will NOT see this announcement. Empty = visible to all.';
