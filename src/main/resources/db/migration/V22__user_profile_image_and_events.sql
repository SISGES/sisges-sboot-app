ALTER TABLE sisges.users
    ADD COLUMN IF NOT EXISTS profile_image_path VARCHAR(500);

CREATE TABLE sisges.school_event (
    id          SERIAL PRIMARY KEY,
    title       VARCHAR(255) NOT NULL,
    description TEXT,
    event_at    TIMESTAMP NOT NULL,
    audience    VARCHAR(20) NOT NULL,
    class_id    INTEGER,
    created_by  INTEGER NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_school_event_class FOREIGN KEY (class_id)
        REFERENCES sisges.school_class (id) ON DELETE CASCADE,
    CONSTRAINT fk_school_event_creator FOREIGN KEY (created_by)
        REFERENCES sisges.users (id) ON DELETE CASCADE,
    CONSTRAINT ck_school_event_audience CHECK (audience IN ('ALL', 'TEACHERS', 'CLASS')),
    CONSTRAINT ck_school_event_class CHECK (
        (audience = 'CLASS' AND class_id IS NOT NULL) OR
        (audience <> 'CLASS' AND class_id IS NULL)
    )
);

CREATE INDEX idx_school_event_event_at ON sisges.school_event (event_at);
