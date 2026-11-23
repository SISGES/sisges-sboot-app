# Test strategy and release evidence

Use this matrix when changing a school workflow or releasing a pair of client and server builds. The commands and coverage describe backend `6b37a18` and frontend `b58d2df`. A test with stubbed browser traffic verifies client behavior for the supplied payload. Use the integrated path to establish compatibility with the server.

| Risk | Existing checks | Additional release evidence for an affected change |
| --- | --- | --- |
| Session and academic data access | Go token tests, DB-backed login and password-change tests, Cypress auth and role-guard specs | Login and reload with real API validation, then compare allowed and denied reads for distinct roles and assignments. |
| Weekly schedules and recorded lessons | Go validation, PostgreSQL schedule/lesson flow, stub object transfers, Cypress lesson-schedule spec | Verify stale-save recovery, an existing lesson after timetable changes and file reads from permitted accounts. |
| Assessment and report cards | Cypress gradebook, grading configuration and boletim specs | Exercise a real scored activity through creation, draft grade, release and student report. Verify a second academic year with the school's expected results. |
| Announcements, events and materials | Browser specs and one real announcement write/read flow. Go and Node tests for the event date window. | Confirm a second browser sees the right event audience and a feed refresh, and verify both HTTP and object storage on uploads. |
| Persistence and migrations | PostgreSQL integration tests cover fresh startup, compatibility with Flyway history and schedule schema use. | Apply a candidate migration to a representative restored schema. Reconcile records and verify older clients during a staged rollout. |

[Go tests](../../internal/api/) · [Browser specs](https://github.com/SISGES/sisges-react-app/tree/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/cypress/e2e) · [Functional requirements](../product/prd.md)

## Backend

From the backend root, with working Go tooling, run the following. For full integration coverage provide `SISGES_TEST_DATABASE_URL` for a **dedicated disposable PostgreSQL database** first. The tests create school fixtures and alter migration history. With the variable unset, database-dependent tests skip.

```sh
go test -race -count=1 ./...
go vet ./...
```

For the SQL-backed paths alone, use `go test -v -run 'TestPostgresMigrationAndAuthFlow|TestScheduleAndDailyRecordsPostgres' ./internal/api` and confirm both actually ran. The race test and vet run against a PostgreSQL 16 service in the backend publishing and deployment workflows. [Integration fixture](../../internal/api/integration_test.go) · [Schedule cases](../../internal/api/schedules_test.go) · [Backend CI](../../.github/workflows/docker-publish.yml)

## Frontend

From the frontend root, install the locked dependencies and run the build, date tests and lint as separate checks.

```sh
npm ci
npm run build
node --test tests/eventDateTime.test.mjs
npm run lint
```

The build type-checks and bundles the SPA. Lint has a zero-warning threshold. The Docker workflows run the build and do not invoke Cypress or the standalone date tests. [Scripts](https://github.com/SISGES/sisges-react-app/blob/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/package.json) · [Date tests](https://github.com/SISGES/sisges-react-app/blob/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/tests/eventDateTime.test.mjs) · [Image build](https://github.com/SISGES/sisges-react-app/blob/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/Dockerfile)

Start the client first. With the local setup in this handbook, its origin is port 3001. The Cypress configuration otherwise defaults to port 3000. Cypress needs its matching binary and platform dependencies.

```sh
CYPRESS_BASE_URL=http://localhost:3001 npm run cypress:run:mock
CYPRESS_BASE_URL=http://localhost:3001 npm run cypress:run:mock -- --spec cypress/e2e/lesson-schedule.cy.ts
```

For the real API case, use an isolated seeded instance, set the API base to match the client's build, and supply local teacher credentials. The optional admin credentials allow the spec to remove its created announcement. At this baseline, the old V16 test users mentioned in a spec comment are not a dependable fixture. Use active users in the development seed. Keep local test credentials outside Git.

```sh
CYPRESS_BASE_URL=http://localhost:3001 \
CYPRESS_API_BASE_URL=http://localhost:8080/api \
CYPRESS_TEACHER_EMAIL=ana.clara.ferreira@sisges.local CYPRESS_TEACHER_PASSWORD=admin123 \
CYPRESS_ADMIN_EMAIL=adm0001@sisges.com CYPRESS_ADMIN_PASSWORD=admin123 \
npm run cypress:run:integration
```

The live case creates a text announcement from the UI and reads it back from the actual feed. Other fixtures can exercise flows that differ from the committed API. Capture the request and response with sensitive fields removed when a mock passes and the integrated flow fails. [Cypress config](https://github.com/SISGES/sisges-react-app/blob/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/cypress.config.ts) · [Live announcement case](https://github.com/SISGES/sisges-react-app/blob/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/cypress/e2e/announcement-create-api.cy.ts)

## Record a verification result

Record both revisions, database migration head, fixture roles and scope, exact checks run, pass/fail/skip counts and any remaining production-only validation. For academic changes include expected-versus-observed records. For deployments include image digests, health response and application smoke results. Re-run relevant checks when the route, schema, upload policy, school rules or client tooling changes.
