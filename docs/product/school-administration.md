# School administration

**Scope** · Functional specification for P-01 and P-02. Product and QA can use the scenarios to validate enrollment and roster management.

## Actors and preconditions

An existing administrator provisions other accounts through `/admin/register`. Login accepts a school-issued email and password. Successful login returns a bearer token and the user’s role. On refresh, the committed client tries to parse the empty validation response as JSON and clears the cached session. The user must sign in again. See the [session restoration flow](../architecture/system.md). Registrations require administrator authentication at the API. [Account handlers](../../internal/api/users.go) · [Role gates](../../internal/api/app.go) · [Frontend session](https://github.com/SISGES/sisges-react-app/blob/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/src/contexts/AuthContext.tsx)

## Enrollment and account changes

1. Enter the name, initial password, birth date, gender and role. For a student, include an existing responsible adult’s ID or new contact data. A current class is optional.
2. Registration generates a role-specific identifier using A for students, P for teachers, and ADM for administrators. The lowercased identifier followed by `@sisges.com` becomes the login email. This generates an account identifier without provisioning a mailbox. The registration transaction writes the user and the applicable student, teacher and guardian links together.
3. Administrators can search students and teachers. The role-specific student detail API includes current class, attendance and linked responsible adults. Directory endpoints use student or teacher IDs where indicated. The general user detail endpoint uses a user ID.
4. Signed-in users can read their own account. Administrators and teachers can change their name, profile image and password. A password change requires the current password and a new password of at least eight bytes. The initial-registration handler accepts six or more bytes. Students have read-only accounts in this flow. [Registration and profile handlers](../../internal/api/users.go) · [Student detail](../../internal/api/students.go) · [Account screen](https://github.com/SISGES/sisges-react-app/blob/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/src/pages/Account/Account.tsx)

**Integration limits.** The registration form calls `POST /api/responsibles/search` to populate its existing-guardian picker. The router has no such operation and the form converts the failed lookup to an empty list. The backend can accept a known guardian ID. The frontend also exposes `/users/:id` to teachers, while its general user-detail request is administrator-only. [Registration form](https://github.com/SISGES/sisges-react-app/blob/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/src/pages/RegisterUser/RegisterUser.tsx) · [Frontend calls](https://github.com/SISGES/sisges-react-app/blob/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/src/services/userService.ts) · [API routes](../../internal/api/app.go)

The student and teacher list screens pass role-profile IDs into the general user-detail screen. That screen queries by account ID, so selecting a row can open a different account or return a missing record when the IDs differ. For directory integrations use each profile ID with the corresponding student or teacher detail endpoint. [Student list](https://github.com/SISGES/sisges-react-app/blob/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/src/pages/Students/Students.tsx) · [Teacher list](https://github.com/SISGES/sisges-react-app/blob/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/src/pages/Teachers/Teachers.tsx) · [Detail screen](https://github.com/SISGES/sisges-react-app/blob/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/src/pages/UserDetail/UserDetail.tsx)

## Cohorts and assignments

An administrator creates a named cohort with a grade level and optional initial student, teacher and subject IDs. A student has one current cohort. Adding the same student to a different cohort changes the current membership. Removing the student from the current cohort clears it. A teacher can belong to multiple cohorts and teach multiple subjects. Cohort deletion sets a deletion timestamp. [Cohort handlers](../../internal/api/classes.go) · [Subject administration](../../internal/api/disciplines.go)

When adding a teacher to an **existing** cohort, the dedicated linking operation consults that teacher’s active subjects. It auto-selects the sole subject, requests a selection when there are several, and rejects an unassigned teacher or an unrelated subject. It saves the teacher link and selected cohort subjects in one transaction. The initial cohort-creation operation accepts separate teacher and discipline lists. Subsequent timetable eligibility uses teacher–cohort, teacher–subject and cohort–subject relationships. [Teacher linking](../../internal/api/class_teacher.go) · [Teacher linking cases](../../internal/api/class_teacher_test.go) · [Timetable eligibility](../../internal/api/schedules.go)

## Acceptance scenarios

| Given | When | Then |
| --- | --- | --- |
| An administrator and a new student with a guardian contact | The administrator completes enrollment | A generated login identifier is returned and the student record links to the guardian. |
| A student registration without either guardian input | The administrator submits it | Registration returns a business-rule error before creating the user. |
| A valid student session | The student opens their account and attempts a profile update | Account details are readable and the update returns HTTP 403. |
| A teacher with two subjects and a cohort with neither linked | An administrator submits the teacher link using one of those subjects | The class detail includes the teacher and the chosen subject. An omitted selection fails before persistence. |
| A student in cohort A | An administrator adds the student to cohort B | B’s roster includes the student and the student’s current-class view resolves to B. |

## Review triggers

Review registration policies, guardian search, role gates, student movement, teacher selection and accepted grade-level values when changing enrollment or class assignment. Coordinate roster changes with timetable, materials and gradebook consumers.
