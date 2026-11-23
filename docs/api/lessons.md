# Timetables, lesson records and attendance

Administrators assign recurring teaching slots to a class. A teacher reads a specific date and saves a lesson for a scheduled slot. The resulting meeting ID is the key for attendance and activities. [Schedules and dated lessons](../../internal/api/schedules.go) · [Meeting handlers](../../internal/api/meetings.go)

## Weekly timetable and dated lessons

| Operation | Access | Request | Response and behavior |
| --- | --- | --- | --- |
| `GET /api/classes/{id}/schedule` | ADMIN | Class ID | `200 {revision,slots}`, ordered by weekday and slot, or `404`. |
| `PUT /api/classes/{id}/schedule` | ADMIN | Complete `{revision,slots:[{weekday,slot,disciplineId}]}` | `200` with incremented revision and saved slots. Replaces the whole recurring schedule. Empty slots clear it. Stale revision, duplicate time slot, invalid range or subject outside the class returns `400 BUSINESS_RULE_VIOLATION`. |
| `GET /api/classes/{id}/lesson-day` | TEACHER | Required query `date=YYYY-MM-DD` | `200 DailyLesson[]`. Requires a current class link. Includes unrecorded slots in the teacher's assigned disciplines and this teacher's recorded slots for the date. Saved slots retain their recorded discipline after timetable edits. |
| `PUT /api/classes/{id}/lesson-day` | TEACHER | `{date,slot,disciplineId,content,attachments:[{name,path}]}` | `200 {id}`. On first save, requires current class/subject assignments, a matching slot and no overlapping meeting. On update, requires the same teacher and recorded subject. Reuses the meeting ID and replaces content/attachments. |

`weekday` is 1–5 (Monday to Friday). Slot indexes `0,1,2,3,4` start at `07:00,07:50,08:40,09:45,10:35`. Each lasts 50 minutes. A **DailyLesson** contains `slot,disciplineId,disciplineName,startTime,endTime,meetingId,content,attachments`. A null `meetingId` identifies a slot awaiting a record.

Send nonblank content or at least one attachment. The trimmed content limit is 20,000 bytes and the attachment limit is ten. Each attachment name must be nonblank and at most 255 bytes. Use the same user's `/api/files/lesson-files/<userId>/<filename>` upload path. To save a text-only lesson, a teacher might send `{"date":"2026-09-14","slot":0,"disciplineId":5,"content":"Linear equations","attachments":[]}`. On a timetable conflict, fetch the latest schedule or day and reconcile the selection before resubmitting. [Lesson validation](../../internal/api/schedules.go) · [Uploads](files-and-feed.md)

## Meeting records and attendance

| Operation | Access | Request | Response and behavior |
| --- | --- | --- | --- |
| `POST /api/class` | TEACHER | Legacy manual creation | Always `400 BUSINESS_RULE_VIOLATION` after authentication. Create a record through the daily lesson PUT. |
| `POST /api/class/search` | ADMIN, TEACHER | Optional `{date,disciplineId,classId,teacherId}` | `200 MeetingSummary[]`. Normal teacher accounts are restricted to their meetings. Latest meeting date first, then start time, up to 1,000. |
| `GET /api/class/{id}` | ADMIN, TEACHER | Meeting ID | `200 MeetingDetail`. Teacher must own the meeting, otherwise `403`. |
| `PUT /api/class/update/{id}` | ADMIN, TEACHER | Legacy record only. `{date,startTime,endTime,disciplineId,classId,teacherId?}` | `200 MeetingDetail`. Use `HH:MM` on input, ending after start. Requires a linked class teacher, a class discipline and no overlapping lesson. Teacher input can select only self. A scheduled record returns `400` and must use the daily lesson route. |
| `DELETE /api/class/delete/{id}` | ADMIN | Meeting ID | `204` on soft deletion, `404` if already deleted or missing. |
| `POST /api/class/{id}/frequency` | ADMIN, TEACHER | `{entries:[{studentId,status}]}` | `204`. Teacher must own the meeting. An entry must identify a current class student and use `P` (present) or `F` (absent). All submitted entries upsert together. Empty batch or invalid entry returns `400`. |

**MeetingSummary** contains `id,date,startTime,endTime,disciplineName,className,teacherName`. Search and detail return times as `HH:MM:SS`. **MeetingDetail** contains `id,date,startTime,endTime,scheduleSlot,content,attachments,teacher,classInfo`. `scheduleSlot` is null for legacy records. `teacher` is a nullable `{id,name,email}` and `classInfo` contains `id,name,academicYear,students`. Each student includes `id,name,email,present`. A null attendance value means no mark for that meeting. [Meeting requests and responses](../../internal/api/meetings.go)
