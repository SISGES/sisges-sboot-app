# SISGES product requirements

**Audience** · School operations, product, engineering and QA

**Baseline** · Backend `6b37a18` and frontend `b58d2df`. Statuses describe the source at these revisions. Production rollout, school adoption and service-level targets require separate evidence.

## Outcome and users

A school administrator sets up people, cohorts, subjects and teaching assignments. A teacher converts a scheduled period into a lesson record and marks attendance. A student sees the resulting absences, school communications and released grades. The current schema and application use one academic cycle and grading configuration per database. The product decision for serving multiple independent schools is open.

| Actor | Job to be done | Entry points |
| --- | --- | --- |
| Administrator | Prepare a cohort for instruction, maintain grading settings, oversee lessons, and publish events and notices | Administrative pages, lesson list, event management and home feed |
| Teacher | Find assigned periods, record content and attendance, distribute study material, enter and release grades | Aulas, Materiais, home feed and the gradebook route |
| Student | Find classmates and teachers, review work, track absences and see released grades | Home, Minha turma, Faltas and Boletim |
| Responsible adult | Supply contact information for a student at enrollment | A guardian contact record linked to the student. The implemented sign-in roles are ADMIN, TEACHER and STUDENT. |

The UI uses Portuguese labels. *Turma* means a cohort, *disciplina* a subject, *aula* a dated lesson, *frequência* attendance, and *boletim* the student report card. `academicYear` on a cohort denotes the grade level. The academic cycle represents the year currently being administered. [UI navigation](https://github.com/SISGES/sisges-react-app/blob/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/src/config/navConfig.ts) · [HTTP routing](../../internal/api/app.go)

## Requirements and acceptance

“Implemented” means the identified caller and handler support the path. “Partial” identifies a concrete handoff that still needs work. Scenarios in the specifications below can be used as release checks on synthetic school data.

| ID | Capability and acceptance | Delivery | Specification |
| --- | --- | --- | --- |
| P-01 | An administrator registers a student with a new guardian. The returned school-issued email can be used to sign in and open the student portal. | Implemented for enrollment with a new guardian and initial sign-in. Existing-guardian lookup and session restoration have client/server mismatches. | [School administration](school-administration.md) |
| P-02 | An administrator assigns a student, teachers and subjects to a cohort. Its current roster and assignments can be read back. | Implemented | [School administration](school-administration.md) |
| P-03 | An administrator publishes the weekly subject timetable. A stale schedule save leaves the newer revision intact. | Implemented | [Teaching and attendance](teaching-and-attendance.md) |
| P-04 | A teacher records an assigned slot and submits attendance. Re-saving the lesson preserves its ID, attendance and activities. | Implemented. Lesson attachments depend on object storage. | [Teaching and attendance](teaching-and-attendance.md) |
| P-05 | A teacher publishes a study material to the assigned subject and cohort. The enrolled student sees it on home. | Implemented for text/metadata. The file picker uses an unsupported upload destination. | [Teaching and attendance](teaching-and-attendance.md) |
| P-06 | Staff create a scored activity, save grades and release them. A student sees only released scores in the report. | Partial. The API supports the sequence, while the lesson form omits mandatory points and the gradebook lacks an entry link. | [Grading and progression](grading-and-progression.md) |
| P-07 | An administrator configures grading, advances terms and closes the year after pending releases are resolved. Results and eligible promotions are recorded. | Grading configuration has a screen. Cycle transitions are API operations. Subsequent-year processing has a known limitation. | [Grading and progression](grading-and-progression.md) |
| P-08 | Staff publish time-limited announcements, signed-in users interact with them, and administrators schedule events for selected audiences. | Implemented with distinct feed and event audience rules. | [Communications and student portal](communications-and-student-portal.md) |
| P-09 | A student views the current class directory, absences by subject, class materials and a personal report card. | Implemented. Some views depend on a current cohort and released grades. | [Communications and student portal](communications-and-student-portal.md) |

## Success measures

The following are proposed ways to judge a school pilot. Numerical targets and reporting cadence require school and product agreement. The reviewed applications supply operational records for these calculations but do not ship a pilot analytics dashboard.

| Question | Proposed measure and source | Interpretation |
| --- | --- | --- |
| Can teachers complete the register? | Within an agreed teaching week, compare dated scheduled periods with recorded lessons, then compare current roster size with marked attendance rows per lesson. | Exclude cancelled periods, holidays and roster changes using school-confirmed records. The weekly timetable is mutable. |
| Can staff finish grading in time? | Count unreleased active activities per trimester at the agreed closing date. Use the pending-release API to identify teachers to contact. | A released activity can still contain blank scores. Track blanks separately with authorized gradebook access. |
| Can students find the academic information they need? | During a supervised pilot, ask an enrolled student to find a named subject’s absence count and a released score. Record task completion without collecting grades in analytics. | Review where learners need help distinguishing pending, blank and zero scores. |

## Dependencies and decisions

The school supplies its roster, subject assignments, grading policy and destination cohorts for progression. Engineering supplies a migrated database, configured API/client and object storage for file features. Before a commercial launch, product and school leadership must agree on the enrollment boundary, grading rules and academic history, while operators establish recovery and support responsibilities. [SaaS readiness register](saas-readiness.md)

## Review triggers

Revisit affected requirement IDs when a role, portal route, academic transition, file destination, supported term structure or school-isolation model changes. Update the corresponding acceptance cases and the readiness register in the same change.
