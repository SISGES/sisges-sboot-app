# Grading, report cards and academic progression

**Scope** · Functional specification for P-06 and P-07. Administrators configure grading and operate the cycle. An administrator or the lesson’s teacher manages an activity gradebook. Students read their own report.

## Configuration and cycle transitions

The administrator’s grading screen defines annual and three trimester maxima and minimum percentages. Each trimester allocates positive integer points to tests (`PROVA`), activities (`ATIVIDADE`) and assignments (`TRABALHO`). Its three allocations must sum to the trimester maximum, and the three trimester maxima must sum to the annual maximum. The portal displays a fixed 70% approval threshold. The report and progression calculations use that fixed threshold even when the minimum percentages in configuration differ. Activity creation also has no aggregate check against the configured category allocation. [Grading configuration](../../internal/api/grading.go) · [Report calculations](../../internal/api/report.go) · [Grading screen](https://github.com/SISGES/sisges-react-app/blob/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/src/pages/GradingConfig/GradingConfig.tsx)

| Administrator action | Conditions | Observed state change |
| --- | --- | --- |
| Start year | A future end date and cycle outside `IN_PROGRESS` | Updates the existing first cycle row to `IN_PROGRESS`, sets trimester 1, records start dates, and sets `gradingLocked=true`. The configuration update handler does not consult the flag. |
| End trimester | In-progress cycle, current administrator password, and no unreleased active activities found by the pending-teacher query for this trimester | Increments the trimester, capped at 3. Calling the transition at 3 leaves it at 3. |
| End year | In-progress cycle, current administrator password, no pending releases found across trimesters, and valid promotion destinations | Processes student results and promotions, then marks the cycle `FINISHED`. The handler accepts closure before trimester 3 or the end date. |

The pending-release query reports teacher names by joining activities to meetings, teachers and users. The committed frontend defines the lifecycle service methods but its routed views do not invoke them. Treat cycle management as an API workflow. [Cycle handlers](../../internal/api/grading.go) · [Cycle client](https://github.com/SISGES/sisges-react-app/blob/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/src/services/academicCycleService.ts)

## Activity to released grade

1. During an in-progress cycle, create an activity against a lesson. The title and positive maximum points are required. The API accepts the three ordinary categories plus trimester and annual recovery. Omitted type defaults to `ATIVIDADE`. When supplied, the trimester must be 1–3. An ordinary activity needs a trimester to appear in a trimester report row.
2. Open the gradebook with the activity ID. The roster is assembled from active students currently in the lesson’s cohort. Saving an entry can set a null score or a score from zero to the activity maximum inclusive. The API checks every entry and saves the batch transactionally.
3. Release the saved scores. The gradebook becomes read-only for score writes. Release can succeed with blank scores, so the operator should reconcile missing marks according to the school’s policy before confirming. The release request does not save unsaved form edits.
4. A student opens `/boletim`. Unreleased ordinary activities can occupy cells with null scores. Only released non-null scores contribute to totals. Recovery scores appear in a separate row. The report uses the student’s current cohort and does not take an academic-year selector. [Activity handlers](../../internal/api/activities.go) · [Student report](../../internal/api/report.go) · [Gradebook screen](https://github.com/SISGES/sisges-react-app/blob/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/src/pages/ActivityGradebook/ActivityGradebook.tsx)

**Client integration.** The committed lesson activity form omits `maxPoints`, so the API rejects a submission even when a title and active cycle exist. Its optional `activities` upload destination is unsupported. The gradebook route `/atividades/:id/notas` can open for an existing activity, but the lesson activity list has no gradebook link. A UI test that intercepts the API cannot establish end-to-end creation. [Lesson activity form](https://github.com/SISGES/sisges-react-app/blob/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/src/pages/AulaDetail/AulaDetail.tsx) · [Activity client](https://github.com/SISGES/sisges-react-app/blob/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/src/services/activityService.ts) · [Storage destinations](../../internal/api/storage.go)

## Recovery and promotion

- **Recovery eligibility.** A trimester needs at least one ordinary activity, all ordinary activities released, and a total below 70% of the trimester maximum. Annual eligibility displayed in the portal requires all three trimesters to meet the release-completeness condition and an annual total below 70% of the annual maximum.
- **Year-end calculation.** For a below-threshold, fully released trimester with an ordinary activity, the latest released trimester recovery can raise that trimester to its threshold. The required recovery score is 70 plus the gap in points. For a 30-point trimester with 18 points, the threshold is 21 and the required recovery score is 73. Annual recovery applies the same gap rule after any trimester adjustments. The system records the base, final score and approval result.
- **Destination.** An approved student advances to the next recognized grade. If several destination cohorts exist, the same name is preferred, then name order. An approved final-grade student has the current class cleared. An unapproved student retains the current class. An invalid grade label or missing required destination causes the result transaction to roll back. [Year-end processing](../../internal/api/report.go)

**History limit.** The processor handles up to 10,000 active, assigned students and skips work if *any* result exists for the first cycle ID. Starting another year reuses that row. The assessment queries also select by cohort without a year partition. A second independent year therefore needs an explicit data-model and policy decision before schools rely on historical reporting or repeated promotions. [Cycle state](../../internal/api/grading.go) · [Result processing and reporting](../../internal/api/report.go)

## Acceptance scenarios

| Given | When | Then |
| --- | --- | --- |
| A valid annual maximum of 100 and trimester maxima of 30, 30 and 40 | The administrator changes a category allocation so that its trimester sums incorrectly | The API rejects the configuration. |
| An unreleased activity worth 10 and a roster member | The teacher saves 8 | The gradebook contains 8, while the student report’s cell remains null until release. |
| A released 10-point activity | Its teacher attempts to revise the score | The save returns a business-rule error. |
| An in-progress cycle and an unreleased activity for the current trimester attached to its teacher | The administrator confirms term closure | The cycle stays in its current trimester and the response identifies the pending teacher. |
| A passing student in a nonfinal grade and no next-grade cohort | The administrator closes the year | The result transaction fails, no promotions commit, and the cycle stays in progress. |

## Review triggers

Revisit the school’s approved policy and repeat the release, recovery and year-end examples when changing grade composition, cycle identity, membership history, threshold settings or the activity form.
