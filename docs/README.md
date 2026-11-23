# SISGES technical handbook

SISGES manages school enrollment, teaching schedules, dated lessons, attendance, assessments and school communications. Start with the PRD for the supported workflows and their acceptance criteria. Use the readiness register before making commitments to a second school or promising commercial service levels.

This handbook covers the Go API and the React SPA. Its implementation baseline is backend `6b37a18` and frontend `b58d2df`. **Implemented** describes behavior in those revisions. **Proposed** identifies a product, engineering or operating decision that still needs an owner and acceptance evidence. Actual deployment configuration and school policies require confirmation from the operator. [Backend baseline](https://github.com/SISGES/sisges-sboot-app/tree/6b37a18f35047a2f6d6e85de57e90444ad488609) · [Frontend baseline](https://github.com/SISGES/sisges-react-app/tree/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc)

## Document map

| Reader task | Technical documents | Reusable template |
| --- | --- | --- |
| Agree on scope, users, outcomes and pilot measures | [Product requirements](product/prd.md) | [PRD](templates/product-requirements.md) |
| Implement or accept a school workflow | [Enrollment and school administration](product/school-administration.md) · [Teaching and attendance](product/teaching-and-attendance.md) · [Grading and progression](product/grading-and-progression.md) · [Communications and student portal](product/communications-and-student-portal.md) | [Functional specification](templates/functional-specification.md) |
| Set service expectations and plan acceptance at school scale | [Quality requirements](product/quality-requirements.md) | [Quality requirements](templates/quality-requirements.md) |
| Decide what is ready for a hosted pilot or commercial rollout | [SaaS readiness and open decisions](product/saas-readiness.md) | [Readiness register](templates/readiness-register.md) |
| Understand components, trust boundaries, critical flows and scaling | [System architecture](architecture/system.md) | [System architecture](templates/system-architecture.md) |
| Change a table or interpret historical school data | [Data model and migration history](architecture/data-model.md) | [Data model](templates/data-model.md) |
| Understand why historical lessons retain their identity | [ADR 001 — Timetables and dated records](architecture/adr-001-weekly-timetable.md) | [Architecture decision record](templates/decision-record.md) |
| Integrate a client or investigate an unexpected response | [API reference, errors and working examples](api/README.md) · [Identity](api/identity.md) · [Classes](api/classes.md) · [Lessons](api/lessons.md) · [Assessments](api/assessment.md) · [Communications](api/communications.md) · [Files and feed](api/files-and-feed.md) | [API reference](templates/api-reference.md) |
| Set up a development environment or change the browser client | [Local setup](engineering/development.md) · [Frontend engineering](engineering/frontend.md) | [Engineering guide](templates/engineering-guide.md) |
| Configure the API, browser build and container network | [Runtime and build configuration](operations/configuration.md) | [Configuration reference](templates/configuration-reference.md) |
| Verify an academic or technical change | [Test strategy and release evidence](engineering/test-strategy.md) | [Test strategy](templates/test-strategy.md) |
| Review access, sensitive data and control gaps | [Security and privacy review](operations/security-privacy.md) | [Security and privacy review](templates/security-privacy.md) |
| Ship an image, investigate an incident or restore a school | [Release and deployment](operations/release.md) · [Incident, backup and restore procedures](operations/recovery.md) | [Operational runbook](templates/runbook.md) |
| Extend this handbook | [Document index template](templates/documentation-index.md) | [All templates](templates/) |

## How to maintain the handbook

1. Start with the template for the reader's task. Set the repository revision and the scope. Replace the prompts with system-specific facts, examples, decision owners and observable acceptance criteria.
2. For a changed workflow, trace a real client call through its server handler, its authorization checks and its persisted data. Check status codes, empty results, validation, role scope and retries. Name the affected PRD requirement. A screen, a service declaration or a mocked test alone cannot prove the integrated flow.
3. Update neighboring contracts in the same change. A migration may affect the data model, API and recovery procedure. A new academic transition may affect the PRD, functional spec, client, tests and readiness register. Mark retrospective decisions and proposed service objectives explicitly.
4. Verify relative links inside this repository and pin cross-repository evidence to the actual source revision. Reconcile API method/path pairs with the Go route registrations, table names with a migrated database, and frontend commands with the committed package scripts. Record which checks ran in the change description.

The handbook supersedes the earlier schema snapshot, user-search example, error guide, CAPTCHA/2FA proposal, MVP gap analysis, and frontend implementation notes. The brand kit retains its own asset and licensing instructions. [Frontend brand kit](https://github.com/SISGES/sisges-react-app/tree/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/brand/open-book)
