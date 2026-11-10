ALTER TABLE sisges.school_class ADD COLUMN schedule_revision INTEGER NOT NULL DEFAULT 0;
CREATE TABLE sisges.class_schedule (
 class_id INTEGER NOT NULL REFERENCES sisges.school_class(id),
 weekday INTEGER NOT NULL CHECK (weekday BETWEEN 1 AND 5),
 slot INTEGER NOT NULL CHECK (slot BETWEEN 0 AND 4),
 discipline_id INTEGER NOT NULL REFERENCES sisges.discipline(id),
 PRIMARY KEY(class_id,weekday,slot)
);
ALTER TABLE sisges.class_meeting ADD COLUMN schedule_slot INTEGER CHECK (schedule_slot BETWEEN 0 AND 4);
ALTER TABLE sisges.class_meeting ADD COLUMN content TEXT NOT NULL DEFAULT '';
ALTER TABLE sisges.class_meeting ADD COLUMN attachments JSONB NOT NULL DEFAULT '[]';
CREATE UNIQUE INDEX class_meeting_schedule_day ON sisges.class_meeting(class_id,meeting_date,schedule_slot) WHERE deleted_at IS NULL AND schedule_slot IS NOT NULL;
