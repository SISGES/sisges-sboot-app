ALTER TABLE sisges.grading_config
    ADD COLUMN trimester1_points_provas INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN trimester1_points_atividades INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN trimester1_points_trabalhos INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN trimester2_points_provas INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN trimester2_points_atividades INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN trimester2_points_trabalhos INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN trimester3_points_provas INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN trimester3_points_atividades INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN trimester3_points_trabalhos INTEGER NOT NULL DEFAULT 0;

UPDATE sisges.grading_config SET
    trimester1_points_provas = trimester1_max_points / 3,
    trimester1_points_atividades = trimester1_max_points / 3,
    trimester1_points_trabalhos = trimester1_max_points - (trimester1_max_points / 3) - (trimester1_max_points / 3),
    trimester2_points_provas = trimester2_max_points / 3,
    trimester2_points_atividades = trimester2_max_points / 3,
    trimester2_points_trabalhos = trimester2_max_points - (trimester2_max_points / 3) - (trimester2_max_points / 3),
    trimester3_points_provas = trimester3_max_points / 3,
    trimester3_points_atividades = trimester3_max_points / 3,
    trimester3_points_trabalhos = trimester3_max_points - (trimester3_max_points / 3) - (trimester3_max_points / 3);

ALTER TABLE sisges.grading_config
    ALTER COLUMN trimester1_points_provas DROP DEFAULT,
    ALTER COLUMN trimester1_points_atividades DROP DEFAULT,
    ALTER COLUMN trimester1_points_trabalhos DROP DEFAULT,
    ALTER COLUMN trimester2_points_provas DROP DEFAULT,
    ALTER COLUMN trimester2_points_atividades DROP DEFAULT,
    ALTER COLUMN trimester2_points_trabalhos DROP DEFAULT,
    ALTER COLUMN trimester3_points_provas DROP DEFAULT,
    ALTER COLUMN trimester3_points_atividades DROP DEFAULT,
    ALTER COLUMN trimester3_points_trabalhos DROP DEFAULT;
