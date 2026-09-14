-- Indexes for the direct-query Go backend. Partial indexes keep active-record
-- lookups small without adding write cost for historical soft-deleted rows.

CREATE INDEX IF NOT EXISTS idx_users_lower_email_active
    ON sisges.users (lower(email)) WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_school_class_name_active
    ON sisges.school_class (name) WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_student_class_active
    ON sisges.student (class_id) WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_teacher_class_active
    ON sisges.teacher_class (class_id, teacher_id) WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_class_discipline_active
    ON sisges.class_discipline (class_id, discipline_id) WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_class_meeting_schedule_active
    ON sisges.class_meeting (class_id, meeting_date, start_time, end_time)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_class_meeting_teacher_active
    ON sisges.class_meeting (teacher_id, meeting_date)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_material_class_discipline_active
    ON sisges.discipline_material (class_id, discipline_id, created_at DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_activity_meeting_active
    ON sisges.evaluative_activity (class_meeting_id, created_at DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_activity_pending_trimester
    ON sisges.evaluative_activity (trimester_number)
    WHERE deleted_at IS NULL AND released = FALSE;

CREATE INDEX IF NOT EXISTS idx_event_upcoming_audience
    ON sisges.school_event (event_at, audience, class_id);
