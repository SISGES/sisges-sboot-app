# SaaS readiness and product decisions

**Audience** · Product owner, school leadership and technical leads

**Decision boundary** · The source supports school operations with one logical school context. Selling an independently administered service to several schools introduces isolation, lifecycle and service commitments beyond that model.

## How to use the register

“Observed” refers to the named implementation evidence. Acceptance evidence and owner roles below are proposals for deciding readiness. A school or operator may have external controls the repositories cannot establish. Capture those controls and a named approver before changing a status to accepted.

| Decision | Current evidence and customer impact | Suggested owner and acceptance evidence |
| --- | --- | --- |
| School boundary and isolation | The school tables, signed identity and API routes lack a school selector. Cycle and grading settings read the first row. A shared database would couple independent schools. The object store configuration accepts one fixed bucket name. [Schema](../../db/migration/V1__create_schema_and_tables.sql) · [Identity](../../internal/api/auth.go) · [Storage](../../internal/api/storage.go) | Architecture and product choose a deployment-per-school or tenant-aware model. Demonstrate that two independently administered schools cannot read, alter or disrupt one another’s data, objects or broadcasts. |
| Provisioning and account lifecycle | An administrator can enroll users. School signup, invitations, account recovery, and an operator-facing school lifecycle are absent from the registered API. Legal guardian records have no portal role. [Routes](../../internal/api/app.go) · [Enrollment](school-administration.md) | School operations and product define initial admin handover, lost access, staff departure and school closure. Rehearse one full onboarding and offboarding case with synthetic records. |
| Commercial entitlement | The current product has no subscription, plan, metering or entitlement model in its routes and schema. Roles grant academic capabilities. [Routes](../../internal/api/app.go) · [Migrations](../../db/migration) | Product and finance choose a commercial model, including whether invoices and access changes are operated manually. Record who can activate, suspend and restore a school. |
| Daily teaching workflow | Timetable and attendance are implemented. Material/activity file pickers request unsupported destinations, and existing-guardian search has no server route. [Teaching](teaching-and-attendance.md) · [Enrollment](school-administration.md) | Product and engineering agree which features enter the pilot. Demonstrate each advertised workflow on the integrated client/API with fixture data. |
| Assessment and repeat academic years | The creation form omits points, the gradebook has no lesson link, and cycle transitions lack a screen. The cycle row is reused. Prior results can suppress a later year’s processing. [Grading](grading-and-progression.md) | An academic lead and engineering agree on fixed versus configurable thresholds, recovery, closure, school history and reopening policy. Rehearse two consecutive years and a cohort transfer against a signed-off expected result set. |
| Privacy and role scope | The app processes student identities, guardian contacts, attendance and grades. The class directory includes school email addresses, and the anonymous feed uses the student audience. Local session data resides in the browser. [Portal](communications-and-student-portal.md) · [Identity and profile](../../internal/api/users.go) | School data owner and security lead define permitted audiences, minimum fields, retention, deletion/export and support access. Validate access with separate roles and document who handles data requests. |
| Operations, recovery and support | Health checks database connectivity. Deployment workflows build images and invoke the host’s Compose setup. The repositories do not establish uptime, recovery time, backup coverage, restore history or customer support terms. [Health](../../internal/api/app.go) · [Deploy workflow](../../.github/workflows/deploy.yml) | Operations and school leadership select service objectives, support channels, incident contacts and recovery targets. Record a timed restoration of both school records and referenced files and verify access from the client. |
| Capacity and evidence | The API uses bounded search results, a limited database connection pool and an in-process feed. Year-end work has a 10,000-student limit. Performance at a school’s actual enrollment is unverified. [Runtime configuration](../../internal/api/config.go) · [Year-end query](../../internal/api/report.go) · [Feed](../../internal/api/websocket.go) | Engineering and school operations supply expected roster size, peak marking/grade-release windows and data retention horizon. Benchmark those scenarios, reconcile returned counts and set acceptance limits before promising scale. |

## Decision sequence

1. **Before onboarding a school**, identify where its data and files will live, who administers the service and how that school recovers access or data.
2. **Before promising a pilot scope**, select the academic workflows the school needs, resolve or disclose the integration gaps affecting them, and test them with the same client/server pair that will be delivered.
3. **Before year-end use**, have the school validate grades, examples of recovery, progression destinations and historical access. Rehearse closure on a representative copy with synthetic or appropriately controlled records.
4. **Before commercial or multi-school rollout**, obtain the service owner’s acceptance of school isolation, operational recovery, capacity evidence and commercial responsibilities.

## Review triggers

Update the evidence when new routes, tenant keys, billing integrations, release checks or operating agreements appear. Link accepted decisions to the relevant requirement IDs in the [PRD](prd.md).
