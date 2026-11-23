# Classes and disciplines

A class is a cohort. `academicYear` is its grade/series label, such as `6º ano`. Use teacher and student IDs from their role-specific directory searches. The examples below assume the caller already holds the appropriate role. [Directory](identity.md)

## Classes and membership

| Operation | Access | Request | Response and behavior |
| --- | --- | --- | --- |
| `POST /api/classes` | ADMIN | `{name,academicYear,studentIds?,teacherIds?,disciplineIds?}` | `200 Class`. Requires nonblank name and a series. Creates the cohort and assigns selected students, teachers and disciplines transactionally. A class insert conflict returns `409`. |
| `POST /api/classes/search` | ADMIN, TEACHER | Optional `{name,academicYear}` | `200 [{id,name,academicYear,studentCount,teacherCount}]`. Partial name and exact series. Sorted by name, capped at 1,000. Normal teacher accounts see assigned classes. |
| `GET /api/classes/{id}` | ADMIN, TEACHER | Class ID | `200 Class`. Teacher must have an active class assignment, otherwise `403`. |
| `DELETE /api/classes/delete/{id}` | ADMIN | Class ID | `204`. Soft-deletes the class. Missing or already deleted returns `404`. |
| `POST /api/classes/{classID}/student/add/{id}` | ADMIN | Student ID, no body | `200 Class`. Sets the student's current class, including reassignment from another class. |
| `POST /api/classes/{classID}/student/remove/{id}` | ADMIN | Student ID, no body | `200 Class`. Clears the current class only if it matches the path. |
| `POST /api/classes/{classID}/teacher/add/{id}` | ADMIN | Teacher ID and optional `{disciplineIds:[…]}` | `200 Class`. Restores or creates the teacher link and links the selected subjects in one transaction. |
| `POST /api/classes/{classID}/teacher/remove/{id}` | ADMIN | Teacher ID, no body | `200 Class`. Soft-deletes this class/teacher link. Class/discipline links persist. |
| `POST /api/classes/{classID}/discipline/add/{id}` | ADMIN | Discipline ID, no body | `200 Class`. Restores or creates the subject link. |
| `POST /api/classes/{classID}/discipline/remove/{id}` | ADMIN | Discipline ID, no body | `200 Class`. Soft-deletes the subject link. |

**Class** contains `id,name,academicYear,students,teachers,disciplines`. Each student has `id,name,email,present` (null in this view), each teacher has `id,name,email`, and each discipline has `id,name`. Membership updates return the resulting class. Some updates affect zero rows and still return it, so inspect membership when confirmation matters. [Class handlers](../../internal/api/classes.go)

When adding a teacher to an existing class, first list their linked disciplines. With exactly one available, an empty selection automatically uses that discipline. With several, supply at least one. Zero available subjects or an invalid selection returns `400 VALIDATION_ERROR` on `disciplineIds`. Duplicate IDs in a valid selection are collapsed. Initial class creation treats `teacherIds` and `disciplineIds` as independent assignments. [Teacher assignment](../../internal/api/class_teacher.go)

## Disciplines

| Operation | Access | Request | Response and behavior |
| --- | --- | --- | --- |
| `GET /api/disciplines` | ADMIN, TEACHER | No filters | `200 Discipline[]`, sorted by name, up to 1,000 active subjects. |
| `GET /api/disciplines/{id}` | ADMIN, TEACHER | Discipline ID | `200 Discipline` or `404`. |
| `POST /api/disciplines` | ADMIN | `{name,description?,teacherIds?}` | `201 Discipline`. Requires a nonblank name. Duplicate name returns `400`. |
| `PUT /api/disciplines/update/{id}` | ADMIN | `{name,description?,teachers?:[{teacherId,vinculado}]}` | `200 Discipline`. Each `true` flag links and each `false` flag unlinks that teacher. Omitted teachers keep their links. Omitted description becomes null. |

**Discipline** contains `id,name,description,teachers`. The description is nullable and each teacher carries `id,name,email`. For example, to rename a subject and add teacher 17, send `{"name":"Matemática","description":"Álgebra","teachers":[{"teacherId":17,"vinculado":true}]}`. [Discipline handlers](../../internal/api/disciplines.go)
