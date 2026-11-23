# Timetables, daily lessons and attendance

**Scope** · Functional specification for P-03 to P-05. The administrator defines a recurring subject timetable. A teacher records what happened on a particular date and marks the cohort’s attendance.

## From timetable to lesson

| Stage | Preconditions and action | Persisted result or exception |
| --- | --- | --- |
| Plan | At `/admin/grade-horaria`, an administrator selects a cohort and places linked subjects into weekday slots. | A draft stays in browser memory across route changes for the current user. A page reload or logout clears it. |
| Save timetable | Submit the complete slot list and the revision returned by the last read. Weekdays are Monday–Friday (1–5), with slots 0–4. | A matching revision replaces the list atomically and increments the revision, including an empty schedule. Duplicate positions, unrelated subjects and stale revisions are rejected. Re-read before reconciling concurrent edits. |
| Open teaching day | A teacher selects an assigned cohort and date in the daily lesson flow. | The API combines unrecorded slots the teacher is eligible to teach and that teacher’s existing scheduled records for the selected day. No matching slots yields an empty list. |
| Record a lesson | Submit the date, slot, subject, content and/or attachments. A new slot requires the teacher’s cohort membership and matching subject assignment. | The first save creates a dated lesson. Re-saving the same owned slot with the same subject updates content and attachments in place. A conflicting legacy meeting in the interval blocks a new record. |
| Read lesson | Staff open the lesson through Aulas. | Assigned teachers can read their own records. Administrators can inspect and remove records. Legacy, unscheduled meetings retain an edit path. New manual meeting creation is rejected. |

The five slot intervals are 07:00–07:50, 07:50–08:40, 08:40–09:30, 09:45–10:35 and 10:35–11:25. A later timetable revision affects unrecorded periods. A recorded slot retains its original subject, content and meeting ID. The daily-write API permits teachers only, even though the frontend teacher route wrapper also admits administrators. [Schedule and daily handlers](../../internal/api/schedules.go) · [Meeting handlers](../../internal/api/meetings.go) · [Draft lifecycle](https://github.com/SISGES/sisges-react-app/blob/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/src/contexts/TimetableDraftProvider.tsx)

## Attendance

The lesson detail shows the current cohort roster. In the browser, a student with no saved mark initially appears present. The UI sends a single-student update when a mark changes, so displayed defaults alone do not establish that staff completed the register. The lesson’s teacher or an administrator submits one or more entries, each `P` (present) or `F` (absent). The API validates that every referenced student currently belongs to the lesson’s cohort and writes the submitted entries as a transaction. A subsequent save updates each submitted student’s mark. The student portal counts explicit absences grouped by subject. An unmarked lesson contributes no absence. [Attendance save](../../internal/api/meetings.go) · [Lesson detail screen](https://github.com/SISGES/sisges-react-app/blob/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/src/pages/AulaDetail/AulaDetail.tsx) · [Student absence query](../../internal/api/students.go)

## Lesson attachments and study materials

**Lesson record.** Text or at least one attachment is required. Trimmed content accepts up to 20,000 bytes, and a record accepts up to ten attachment references. With R2 configured, a teacher can upload an accepted image, PDF, text or Office file to `lesson-files` (up to 10 MiB each). A lesson attachment reference must use the current uploader’s path. Download authorization also checks the referenced lesson for eligible class members. [Lesson validation](../../internal/api/schedules.go) · [Storage](../../internal/api/storage.go) · [Daily lesson screen](https://github.com/SISGES/sisges-react-app/blob/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/src/pages/CreateAula/CreateAula.tsx)

**Study material.** On `/materiais` a teacher chooses a cohort and subject, enters a title, and can supply a description. Creation checks the teacher’s cohort membership and the teacher–subject and cohort–subject links. Students see materials for their current cohort. Only the creating teacher can remove a material. The optional file picker submits the destination `materials`, which the Go upload handler rejects. A text-only material can be published without object storage. [Materials handlers](../../internal/api/content.go) · [Materials screen](https://github.com/SISGES/sisges-react-app/blob/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/src/pages/Materials/Materials.tsx)

## Acceptance scenarios

| Given | When | Then |
| --- | --- | --- |
| Two administrators loaded revision 2 | The first saves revision 3 and the second submits the old revision | The second receives a business-rule error and the first change survives. |
| A linked teacher, a Monday period and an assigned subject | The teacher records content, marks attendance and later edits the lesson content | The meeting ID and attendance are preserved. |
| An existing recorded slot whose subject was later changed in the timetable | Its original teacher reopens that date | The saved record retains the original subject. |
| A lesson with two enrolled students and one unrelated student | Staff include the unrelated student in a batch of attendance marks | The batch fails and none of its changes persist. |
| A teacher with a linked class and subject | They publish a titled material without a file | An enrolled student can see it. Removing it as its creator takes it out of the active list. |

## Review triggers

Coordinate changes to slot times, cohort membership, file rules or attendance status values across schedule validation, timetable editor, daily-record form and the student portal. [Schedule flow tests](../../internal/api/schedules_test.go)
