# School communications and the student portal

**Scope** · Functional specification for P-08 and P-09. Announcements are posts with an active window. Events are dated calendar entries with an audience. The student portal also collects class-specific academic information.

## Announcements and events

| Action | Actor | Product behavior |
| --- | --- | --- |
| Publish a notice | Administrator or teacher | Create a titled text or image post from the home feed. The server defaults its start to now. Accepted TTL choices at creation are 1, 4, 10, 24, 48 or 168 hours. With a TTL, expiry is calculated from the active start. Image uploads require R2. |
| Select visibility | Publisher | Feed reads apply the active interval and exclude roles in `hiddenForRoles`. The server stores `targetRoles`, but the feed query does not use it to select readers. The API treats anonymous feed reads as the student audience. The web dashboard itself requires sign-in. |
| Participate | Signed-in user | Toggle one like per user and notice, view likers, list comments, and create, edit or remove one's own comments. A new comment must be nonblank and at most 250 bytes. |
| Maintain or expire a notice | Administrator or background worker | An administrator can edit or delete a notice, including a teacher-authored post. Expiry immediately excludes it from feed reads. A once-per-minute worker deletes expired rows and attempts to remove associated image objects. The app fetches the feed on mount and polls every 60 seconds while the tab is visible. The live subscription has a client/server compatibility gap. See the [feed delivery flow](../architecture/system.md). |
| Schedule a calendar entry | Administrator | Supply a title and event timestamp from now through the end of the current calendar year in São Paulo. Audiences are `ALL`, `TEACHERS` or `CLASS`. A class event requires a class ID. The event-management API provides create, list and delete actions. |
| Read calendar | Signed-in user | Administrators can include past events. Other users see upcoming items for all users, their teacher role, or a class they teach or attend. |

The feed visibility rules describe selection for the feed list. Interactions have their own route-level access rules. School communications that must have a private audience require an explicit visibility policy across all reading surfaces. [Content handlers](../../internal/api/content.go) · [Expiry and live feed](../../internal/api/websocket.go) · [Event date rule](../../internal/api/event_date.go) · [Feed UI](https://github.com/SISGES/sisges-react-app/blob/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/src/components/AnnouncementFeed/AnnouncementFeed.tsx)

## Student views and empty states

| View | Information and boundaries |
| --- | --- |
| Home | The authenticated dashboard loads the announcement feed, event panel, cohort materials and cohort activity descriptors. An activity can appear before its grades are released. A student submission or homework-turn-in workflow is outside the current API. |
| Minha turma (`/minha-turma`) | Shows the current cohort, grade level, classmates and linked teachers. The response includes names and school email addresses. Without an active cohort, the class operation returns a resource-not-found error. |
| Faltas (`/faltas`) | Aggregates the student’s recorded absences by subject, including earlier active lessons with an absence record. Subjects with no recorded absence produce no count row. The screen loads the current class and absences together. |
| Boletim (`/boletim`) | Shows three trimester rows, released totals, recovery scores and annual eligibility. A student record without a current cohort receives empty trimester activity lists and configured maxima. |
| Conta (`/account`) | All roles can view their account. Student profile fields are read-only in both the UI and the API. |

[Student queries](../../internal/api/students.go) · [Report card](../../internal/api/report.go) · [Student home sections](https://github.com/SISGES/sisges-react-app/blob/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/src/components/StudentHomeSection/StudentHomeSection.tsx) · [Grading and progression](grading-and-progression.md)

## Acceptance scenarios

| Given | When | Then |
| --- | --- | --- |
| An active notice whose hidden roles include STUDENT | A student refreshes the home feed | The notice is absent from the returned list. |
| A teacher creates a text announcement | An administrator edits its title | The next feed load shows the revised title. The teacher lacks the post-update permission. |
| Two signed-in users and a comment by the first | The second tries to edit that comment | The update fails and the original content is preserved. |
| An upcoming CLASS event for cohort B | A student in A loads events, then a student in B loads events | Only the student in B sees that class event. |
| A student has two absent marks and one present mark in the same subject | The student opens Faltas | That subject has an absence count of two. |

## Review triggers

Recheck feed and interaction visibility together when changing a role or audience. Recheck date boundaries in São Paulo when adjusting event input or display. Review class transfers and score release with the academic team whenever a student view changes.
