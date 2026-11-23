# Security and privacy review

This review describes controls visible in the browser, Go service and SQL migrations. It gives a release reviewer concrete evidence to request before a school entrusts the service with identifiable student records. Hosting controls and school policies require separate verification.

## Data boundaries

| Data | Where it lives | Access and lifecycle to review |
| --- | --- | --- |
| Account identity, password hash, role and profile | PostgreSQL. Browser stores the signed-in user's profile and bearer token in local storage. | The API looks up the live account and compares email and role on protected HTTP requests. Logout clears client state. Review shared-device use, expiry and revocation needs. |
| Students, guardians and class rosters | User, student, responsible and link tables | Records can include birth dates, names and guardian phone numbers or email addresses. Guardians are contact records. Directory and self-service responses have different audiences. |
| Attendance, grades, recovery and promotion | Meetings, attendance, activities, grades and annual results in PostgreSQL | Academic history is sensitive. Preserve access by role, class/teacher association and student identity. Verify each handler used for reads, writes and reports. |
| Announcements, likes, comments and events | PostgreSQL content and relationships, with optional R2 image paths | The announcement feed accepts anonymous requests using the student view. Publication and hidden-role settings should be reviewed against the actual feed behavior before sensitive content is published. |
| Post images, profile images and lesson documents | R2 object bytes with references in application records | File routes authenticate readers. Lesson attachments add ownership or linked-record checks. Image reads follow different rules. An opaque key alone is insufficient as a privacy policy. |
| Backup, logs and browser/device copies | Operator-controlled infrastructure and client storage | Determine who may access, export or delete copies, which identifiers may appear in logs, and how restoration reconciles prior deletions. |

[Session checks](../../internal/api/auth.go) · [Content handlers](../../internal/api/content.go) · [Files](../../internal/api/storage.go) · [Data model](../architecture/data-model.md)

## Implemented controls

- The service compares bcrypt password hashes at login and signs HS256 tokens with a key of at least 32 bytes. A protected request checks the signature, expiry and live account. The self-service password change checks the current password. Account provisioning is an admin operation. [Users](../../internal/api/users.go) · [Authentication](../../internal/api/auth.go)
- Role middleware admits the registered roles. Handlers add scope checks for selected operations, including recording scheduled lessons and serving linked lesson attachments. Screen guards and navigation aid users. Server-side checks control data access. [Router](../../internal/api/app.go) · [Scheduled lessons](../../internal/api/schedules.go)
- Uploads have a file size limit, permitted extensions and destination checks. The service assigns object keys, signs storage requests and streams downloads. Lesson documents are served as attachments. Extension checks do not establish content scanning. [Storage](../../internal/api/storage.go)
- The backend image runs a static binary as a numeric non-root user with a writable private temp directory. Database credentials, the signing key and R2 credentials are process inputs. Frontend `VITE_*` settings are part of the public bundle. [Backend image](../../Dockerfile) · [Configuration](configuration.md)

## Decisions and acceptance evidence

| Area | Current limit | Proposed accountable role and evidence |
| --- | --- | --- |
| School isolation | Records, grading configuration and academic-cycle access use a shared school context. | Product and engineering choose a tenancy model. Verify isolation of directory, reports, files, backups and administrative access with two independent school datasets before offering a shared instance. |
| Identity abuse and recovery | The app has password login. The checked-in routes have no MFA enrollment, login throttling, self-service recovery or individual refresh-token revocation. | Service owner documents staff recovery and access removal, evaluates step-up authentication and rate control, then verifies enrollment, lost-device and revocation flows. CAPTCHA and MFA are proposals from earlier analysis. |
| Browser and transport | The SPA persists bearer tokens and the feed handshake passes a token in the query string. TLS termination, proxy log redaction and key rotation are deployment responsibilities. | Operator verifies HTTPS/WSS, restricts CORS origins, redacts sensitive fields, rehearses rotation and records its effect on active sessions and sockets. |
| Least privilege and file lifecycle | Object permissions vary by resource type. DB references and object writes or cleanup are separate operations. | Engineering reviews effective read/write scope per operation, adds negative authorization tests where missing, and reconciles unreferenced or expired files against retention rules. |
| Evidence and retention | Schema supports soft deletion for several records and includes a user log table. The Go routes do not implement a complete audit trail, data-export process or retention schedule. | School data owner defines record classes, retention, export, correction and deletion procedures, including archived copies. Engineering verifies a representative request and records completion evidence. |
| Abuse response and incident handling | Application errors are logged. No on-call or incident policy is supplied by these repositories. | Operations establishes escalation ownership, an access-controlled incident record and a restore exercise for affected data. [Incident runbook](recovery.md) |

Review this document when authentication, school boundaries, file namespaces, audience rules or retention behavior change. Any external security assurance or regulatory sign-off requires the responsible organization to verify its deployment and operating procedures.
