CREATE TABLE sisges.grading_config (
    id                        SERIAL PRIMARY KEY,
    year_max_points           INTEGER        NOT NULL,
    year_min_percentage       NUMERIC(5, 2)  NOT NULL,
    trimester1_max_points     INTEGER        NOT NULL,
    trimester1_min_percentage NUMERIC(5, 2)  NOT NULL,
    trimester2_max_points     INTEGER        NOT NULL,
    trimester2_min_percentage NUMERIC(5, 2)  NOT NULL,
    trimester3_max_points     INTEGER        NOT NULL,
    trimester3_min_percentage NUMERIC(5, 2)  NOT NULL,
    created_at                TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at                TIMESTAMP
);

INSERT INTO sisges.grading_config (
    year_max_points, year_min_percentage,
    trimester1_max_points, trimester1_min_percentage,
    trimester2_max_points, trimester2_min_percentage,
    trimester3_max_points, trimester3_min_percentage
) VALUES (
    100, 60.00,
    33, 60.00,
    33, 60.00,
    34, 60.00
);
