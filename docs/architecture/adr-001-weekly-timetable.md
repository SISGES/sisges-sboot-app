# ADR 001 — Keep recurring timetables separate from dated lesson records

Audience / owner role — academic workflow and backend engineers

Status / scope — retrospective record of the implemented V24 design

Source baseline — backend `6b37a18`, frontend `b58d2df`

## Context

Administrators plan the weekly schedule by class and subject. Teachers record what happened on a particular date, attach files and later use that meeting's identity for attendance and assessments. Changing a recurring plan must preserve the historical subject, teacher and academic work attached to a recorded lesson. Older databases also contain manually created meetings with arbitrary times. [V24 migration](../../db/migration/V24__weekly_timetable_and_lesson_records.sql) · [Meeting handlers](../../internal/api/meetings.go)

## Implemented decision

1. Store one current weekly plan per class in `class_schedule`, keyed by class, weekday and slot. The five slot numbers map to fixed 50-minute intervals with a break from 09:30 to 09:45 and a final end time of 11:25. The database stores slot numbers, and Go defines the clock times. [Schedule handler](../../internal/api/schedules.go)
2. Replace the complete plan atomically. The administrator sends the last observed class revision with the desired slots. The server locks the class row, checks revision and current class-subject membership, replaces the rows, and increments the revision. An empty slot list clears the future plan. [Schedule write](../../internal/api/schedules.go)
3. Build a teacher's daily view from unrecorded eligible slots for the date's weekday plus the teacher's own existing scheduled meetings for that date. On first save, lock the class row, check current assignments and the slot, reject time overlaps, then persist the meeting. A partial unique index protects the active class/date/slot identity. On a subsequent authorized save, replace only content and attachments on the existing row. [Daily read and save](../../internal/api/schedules.go) · [Schema index](../../db/migration/V24__weekly_timetable_and_lesson_records.sql)
4. Preserve the existing meeting tables and child foreign keys. Existing unslotted meetings retain their original time interval. The legacy manual-create endpoint returns a business error, and generic updates of a scheduled meeting direct the caller back to the scheduled-record flow. [Legacy meeting boundary](../../internal/api/meetings.go)

## Consequences and limits

For example, suppose a teacher records Monday's first slot as mathematics, then an administrator removes that slot. A subsequent read of that recorded Monday still returns the mathematics meeting. An unrecorded later Monday reflects the revised timetable. The change also affects unrecorded past dates because the weekly plan has no effective-date history. [Read query](../../internal/api/schedules.go) · [Record preservation test](../../internal/api/schedules_test.go)

Using the class row as a shared lock serializes competing timetable and scheduled-record writes within that class, including writes for different dates. A stale timetable save returns HTTP 400 with `BUSINESS_RULE_VIOLATION`. The client must read the current revision and reconcile its draft. Lesson content itself has no revision field, so sequential edits by the same authorized teacher can replace earlier content. The lock and partial index preserve the meeting identity and its child relationships. [Schedule writes](../../internal/api/schedules.go) · [Application errors](../../internal/api/errors.go)

## Review triggers

Revisit this design for effective-dated timetables, school-specific bells or holidays, cross-class teacher clash checks, multiple editors of one lesson, or per-school scheduling rules. Those capabilities would affect the schedule key, historical query, UI draft behavior or write contract. Update the V24 regression scenarios, data model and API reference with any replacement. [Database-backed scenarios](../../internal/api/schedules_test.go) · [Data model](data-model.md) · [API reference](../api/README.md)
