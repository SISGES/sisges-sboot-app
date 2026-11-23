# Assessment, academic cycle and reports

The activity ID identifies a graded task attached to one meeting. The student's role record ID identifies a gradebook row. A student's report card uses their current class. [Activity handlers](../../internal/api/activities.go) · [Report computation](../../internal/api/report.go)

## Activities and scores

| Operation | Access | Request | Response and behavior |
| --- | --- | --- | --- |
| `GET /api/activities/meeting/{id}` | ADMIN, TEACHER | Meeting ID | `200 Activity[]`. Lists active activities on the specified active meeting, newest first, up to 1,000. The list handler filters by meeting ID. |
| `GET /api/activities/my` | STUDENT | No body | `200 Activity[]` for the caller's current class. Includes released and pending activity metadata, newest first, up to 1,000. |
| `POST /api/activities` | ADMIN, TEACHER | `{classMeetingId,title,maxPoints,activityType?,trimesterNumber?,description?,filePath?}` | `201 Activity`. Cycle must be `IN_PROGRESS`. Teacher must own the lesson. Nonblank title, positive max points and a valid type required. `trimesterNumber` may be null or 1–3. |
| `GET /api/activities/{id}/gradebook` | ADMIN, TEACHER | Activity ID | `200 Gradebook`. Concrete route dispatched by `GET /api/activities/{rest...}`. Other suffixes in that wildcard return `404`. |
| `PUT /api/activities/{id}/grades` | ADMIN, TEACHER | `{entries:[{studentId,score}]}` | `200 Gradebook`. Requires a nonempty batch and unreleased activity. Upserts current class students only. `score` may be null or between zero and `maxPoints` inclusive. One invalid entry rolls the batch back. |
| `POST /api/activities/{id}/release` | ADMIN, TEACHER | No body | `200 Activity`. Marks released and records the first release time and user. The handler permits release with missing grades. Subsequent grade writes return `400`. |
| `DELETE /api/activities/{id}` | ADMIN, TEACHER | Activity ID | `204` on soft deletion. |

For gradebook, grade write, release and deletion, teachers must own the activity's meeting. Failed teacher ownership checks return `400 BUSINESS_RULE_VIOLATION`. Administrators can manage activities across meetings. Activity types are `PROVA`, `ATIVIDADE` (create default), `TRABALHO`, `RECUPERACAO_TRIMESTRE` and `RECUPERACAO_ANUAL`. [Activity checks](../../internal/api/activities.go)

**Activity** includes `id,classMeetingId,title,description,filePath,activityType,trimesterNumber,maxPoints,released,releasedAt,createdAt`. **Gradebook** includes `activityId,classMeetingId,title,activityType,trimesterNumber,maxPoints,released,students`. Each student row includes `studentId,userId,studentName,score`. For a 10-point activity, `{"entries":[{"studentId":12,"score":8.5},{"studentId":14,"score":null}]}` writes a mark for one student and clears the other's mark.

## Grading configuration and cycle transitions

| Operation | Access | Request | Response and behavior |
| --- | --- | --- | --- |
| `GET /api/grading-config` | ADMIN, TEACHER | No body | `200 GradingConfig`. |
| `PUT /api/grading-config` | ADMIN | Full `GradingConfig` object | `200 GradingConfig`. Year maximum must equal all trimester maxima combined. For each trimester, the three positive component values must sum to its maximum. The update handler does not consult `gradingLocked`. |
| `GET /api/academic-cycle` | Any | No body | `200 Cycle`. |
| `POST /api/academic-cycle/start-year` | ADMIN | `{yearEndDate:"YYYY-MM-DD"}` | `200 Cycle`. Future end date required. Updates the existing cycle to `IN_PROGRESS`, sets the start date, trimester 1 and `gradingLocked:true`. Already in progress returns `400`. |
| `GET /api/academic-cycle/pending-releases` | ADMIN | Optional query `trimester=1`, `2` or `3` | `200 {trimester,teachers}`. Returns distinct teacher names with pending activities. Omitting the filter checks all and returns `trimester:null`. |
| `POST /api/academic-cycle/end-trimester` | ADMIN | `{password}` of the signed-in admin | `200 Cycle`. Requires an in-progress year and no pending releases for the current trimester. Advances one trimester up to 3. |
| `POST /api/academic-cycle/end-year` | ADMIN | `{password}` of the signed-in admin | `200 Cycle`. Requires an in-progress year and no pending releases across trimesters. Processes year results and promotions, then sets `FINISHED`. Missing promotion destination can block finalization with `400`. |
| `GET /api/boletim/me` | STUDENT | No body | `200 Report` for the authenticated student. Missing student record returns `404`. A student without a class receives the grading maxima and empty trimesters. |

**GradingConfig** is a flat object. Send integer `yearMaxPoints`, numeric `yearMinPercentage` and, for each `N` in 1, 2, 3, `trimesterNMaxPoints`, `trimesterNMinPercentage`, `trimesterNPointsProvas`, `trimesterNPointsAtividades` and `trimesterNPointsTrabalhos` (e.g. `trimester2PointsProvas`). Only the percentage fields are fractional. A GET followed by a full validated edit preserves all fields. [Grading config and transitions](../../internal/api/grading.go)

**Cycle** includes `status` (`NOT_STARTED`, `IN_PROGRESS` or `FINISHED`), `yearStartDate,yearEndDate,currentTrimester,gradingLocked,yearStartedAt,yearFinishedAt`. Dates and timestamps can be null. Pending releases and password failures during a transition produce `400 BUSINESS_RULE_VIOLATION`. An end-year call may reassign students, and there is no HTTP undo operation. The result processor uses the existing cycle ID and skips processing if year results already exist for it. [Cycle handlers](../../internal/api/grading.go) · [Year-end processing](../../internal/api/report.go)

**Report** contains `fixedApprovalPercentage` (70), `yearMaxPoints,totalReleasedScore,eligibleForYearRecovery,trimesters,recoveryRow`. There are three trimester objects with `trimester,trimesterMaxPoints,activities,totalReleasedScore,allActivitiesReleased,eligibleForRecovery`. Each activity has `activityId,title,activityType,maxPoints,score,released`. Pending scores are null. Normal released scores contribute to totals. The recovery row carries `trimesterRecoveryScores` (three nullable scores) and `yearRecoveryScore`. A trimester with at least one ordinary activity, all released, qualifies below 70% of its maximum. Annual eligibility requires this release condition for all three trimesters and a total below 70% of the annual maximum. [Student report](../../internal/api/report.go)
