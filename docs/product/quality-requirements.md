# Quality requirements for a school deployment

Audience — product owner, school administrator, engineering and operations. These requirements distinguish limits encoded in the application from service levels that need measurement and agreement. Backend `6b37a18` and frontend `b58d2df` supply the implementation baseline.

## Operating envelope

| Quality attribute | Current behavior | What the school needs to establish |
| --- | --- | --- |
| Availability | `/health` checks PostgreSQL. File storage, browser login and live updates have independent failure modes. | Agree on school service hours, critical journeys, maintenance windows and who receives alerts. Measure each critical journey through its real entry point. |
| Response time and capacity | A process defaults to six PostgreSQL connections and a 160 MiB Go soft memory target. Feed capacity is 256 connections per process. HTTP timeouts bound connection use. | Supply class sizes, concurrent classes, marking and report-card peaks. Agree on latency percentiles and allowable errors at those loads. |
| Completeness | Several directory searches stop at 1,000 rows. Annual processing selects up to 10,000 active assigned students. | Reconcile fixtures at and above the limits with expected counts before adopting SISGES for a larger enrollment. |
| Record integrity | Timetable updates compare revisions and commit atomically. Lesson IDs preserve attendance and activities across edits. Grading and year closure have their own transaction boundaries. | Rehearse concurrent timetable edits, a failed year closure, a retry and a second academic year on an isolated copy. Reconcile grades and class membership against school-approved examples. |
| Data recovery | PostgreSQL contains the school records. R2 contains the bytes for referenced uploads. Restoring either side in isolation can break a lesson or a notice. | Establish the maximum acceptable loss of entries and duration of interrupted instruction. Time a restore and verify linked files, including authorization. |
| Accessibility and usability | The client has reusable forms, dialogs, light/dark themes and responsive layouts. The source supplies no accessibility conformance certification. | Observe keyboard-only login, timetable entry and report lookup, including focus, readable validation and narrow viewports. Agree which browsers, devices and assistive technologies are in scope. |
| Confidentiality | Role checks and some record-level checks protect school workflows. A deployment has one logical school context. | Verify the accepted audience for each student field and file type, and the isolation boundary for every school. |

[Runtime](../../cmd/sisges/main.go) · [Resource configuration](../operations/configuration.md) · [API](../api/README.md) · [Data invariants](../architecture/data-model.md) · [Security review](../operations/security-privacy.md)

## Proposed pilot evidence

| School scenario | Measurement | Pass decision |
| --- | --- | --- |
| Teachers mark attendance at the same time | Record successful saves / attempted saves, latency percentiles and the expected versus stored attendance for a known roster. Count unmarked rows separately from explicit presence. | School and service owner set workload and latency target in advance. Every acknowledged fixture change must reconcile. |
| Administrators publish a weekly schedule from two browsers | Record both revisions, responses, the final slots, and the ID and attendance of a previously recorded lesson. | One save using the original revision can succeed. The other must see the conflict and reconcile. The historical lesson must remain intact. |
| Students open report cards after grade release | Compare normal, unreleased, blank, zero and recovery scores against a signed-off example. Measure response time and errors at expected peak. | Academic lead confirms the expected report values. Service owner approves measured peak performance. |
| Operations restores service after loss of a database and file store | Measure backup age at incident time, time until successful login/read, reconciliation counts and file access results. | Data owner accepts documented RPO/RTO and lost-write window. Record the actual drill results and date. |

Run these tests with synthetic or appropriately controlled data. Record build revisions, workload, environment and actual observations alongside the decision. The repositories do not provide a contractual uptime or response-time guarantee. [Test strategy](../engineering/test-strategy.md) · [Recovery procedure](../operations/recovery.md) · [SaaS readiness](saas-readiness.md)
