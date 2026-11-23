# Identity and directory

Use the role-specific directory to obtain a teacher or student ID for enrollment, attendance and grade operations. A user ID identifies an account. A teacher ID or a student ID identifies a separate role record and can have a different numeric value. [Users and teachers](../../internal/api/users.go) · [Students](../../internal/api/students.go)

## Authentication and accounts

| Operation | Access | Request | Response and behavior |
| --- | --- | --- | --- |
| `POST /api/auth/login` | Public | `{email,password}` | `200 {accessToken,tokenType:"Bearer",user}`. Email matching is trimmed and case-insensitive. Invalid credentials return HTTP 200 with an error object whose `status` is 401. |
| `POST /api/auth/register` | ADMIN | `{name,password,birthDate,gender,role}`, plus student fields below | `201 User`. Allocates the register and email in one transaction. |
| `GET /api/auth/validate` | Any | No body | `200`, empty body. |
| `GET /api/users/me` | Any | No body | `200 User` for the account on the token. |
| `PATCH /api/users/me` | ADMIN, TEACHER | Any of `name`, `password`, `currentPassword`, `profileImagePath` | `200 User`. Changing password requires the matching current password and at least eight bytes for the replacement. Name requires at least two trimmed bytes and at most 255 raw bytes. Empty image path clears the image. Student requests receive `403`. |
| `GET /api/users/{id}` | ADMIN | User ID | `200 User` or `404`. |
| `POST /api/users/search` | ADMIN | Optional `{name,email,register,gender,initialDate,finalDate}` | `200 [{id,name,email,role}]` sorted by name, up to 1,000 active accounts. Text filters for name/email/register use case-insensitive substring matching, gender is exact, birth-date bounds are inclusive. |

**User** contains `id`, `name`, `email`, `register`, `role`, `birthDate`, `gender` and nullable `profileImagePath`. Registration requires a nonblank name, a password of at least six bytes, a parseable birth date, gender and one of the three role values. The server generates an `A`, `P` or `ADM` register and lowercase `@sisges.com` email. For `STUDENT`, send an existing `responsibleId` or `responsibleData` with `name`, `phone`, `email` and optional `alternativePhone` and `alternativeEmail`. Inline responsible data takes precedence when both are supplied. `classId` is optional. An unknown existing responsible returns `404`. [Account payloads and validations](../../internal/api/users.go)

For example, a signed-in administrator searching for student accounts born in an interval can submit the following body to `POST /api/users/search` and filter the returned `role` client-side. The search handler has no role filter.

```json
{"name":"Ana","initialDate":"2010-01-01","finalDate":"2011-12-31"}
```

## Teachers and students

| Operation | Access | Request | Response and behavior |
| --- | --- | --- | --- |
| `POST /api/teachers/search` | ADMIN | Optional `{name,email}` | `200 [{id,name,email}]` with teacher IDs. Partial case-insensitive matches, sorted by name, maximum 1,000. |
| `GET /api/teachers/me` | TEACHER | No body | `200 Teacher` resolved by the signed-in user ID. |
| `GET /api/teachers/{id}` | ADMIN | Teacher ID | `200 Teacher` or `404`. |
| `POST /api/students/search` | ADMIN | Optional `{name,email}` | `200 [{id,name,email}]` with student IDs. Same matching, ordering and bound as teacher search. |
| `GET /api/students/{id}` | ADMIN | Student ID | `200 Student` or `404`. Includes up to 1,000 attendance records, latest meeting date first. |
| `GET /api/students/me/turma` | STUDENT | No body | `200 {className,academicYear,classmates,teachers}` for the student's active class. `404` if no active class. Classmates include the caller. |
| `GET /api/students/me/faltas-por-disciplina` | STUDENT | No body | `200 [{disciplineName,absenceCount}]`. Counts recorded absences by discipline for this student, sorted by name. An empty list means no recorded absences. |

**Teacher** contains `id,name,email,register,birthDate,gender,classes`. Each class is `{id,name,academicYear}`. **Student** has the same identity fields and `currentClass` (nullable), `responsibles` (`id,name,phone,email`), and `attendances` (`present` plus `classMeeting:{disciplineName,meetingDate,createdAt}`). In the self-service class directory, teacher rows carry `id,name,email`. Classmate rows also contain `present`, returned as null in this view. [Teacher detail](../../internal/api/users.go) · [Student detail and self-service](../../internal/api/students.go)
