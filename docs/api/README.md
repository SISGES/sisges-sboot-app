# SISGES API reference

For client developers integrating with the Go API. This reference describes the handlers at backend revision `6b37a18`. Each operation below uses the full path on the API host (locally `http://localhost:8080`). Class and lesson IDs are different resources. `/api/classes` addresses school cohorts and `/api/class` addresses individual class meetings. [Router](../../internal/api/app.go)

| Client task | Reference |
| --- | --- |
| Sign in, manage accounts, find teachers and students | [Identity and directory](identity.md) |
| Organize classes, subjects and teaching assignments | [Classes and disciplines](classes.md) |
| Save the weekly timetable, record a lesson or mark attendance | [Lessons and attendance](lessons.md) |
| Issue activities, release scores, close a term or read a report | [Assessment and academic cycle](assessment.md) |
| Publish notices, use likes and comments, distribute materials or events | [School communications](communications.md) |
| Transfer files or subscribe to feed updates | [Files and live feed](files-and-feed.md) |

## Transport and identity

Use `Content-Type: application/json` for JSON writes, and send a single object. Go limits JSON request bodies to 1 MiB. Most reads return JSON objects or arrays directly. IDs are integers and path IDs must be positive. Calendar dates use `YYYY-MM-DD`. Timestamp fields include a timezone offset or `Z`. The HTTP service has no version segment. [Request handling](../../internal/api/app.go)

Send `Authorization: Bearer <accessToken>` on protected HTTP requests. The server checks the token signature and expiry and verifies the user's current email, role and active account against the database. Sign in again when the token expires. Roles in the tables use their literal API values. **Any** means any authenticated role. Clients should use TLS for remote deployments and handle a rejected session before retrying a user action. [Authentication](../../internal/api/auth.go)

| Operation | Access | Inputs and success |
| --- | --- | --- |
| `GET /health` | Public | No body. `200 {"status":"ok"}` after a database ping with a one-second timeout. Database failure returns `500`. |

### Search and paging

Directory, class, lesson, discipline, material and activity lists return arrays with endpoint-specific caps. Announcement and event lists have a 500-row cap. Search filters for directory, classes and lessons go in POST bodies. Clients should account for truncation at the limit. These routes expose no page/offset/cursor or total count. Report, likes, comments and gradebook shapes have their own collection fields. [Router and handlers](../../internal/api/)

### Errors

| HTTP status | Body code | Client behavior |
| --- | --- | --- |
| 200 at login only | `AUTH_INVALID_CREDENTIALS` | Body `status` is 401. Check for `accessToken` before starting a session. |
| 400 | `VALIDATION_ERROR` | Read the reported `errors[].field` and `errors[].message`. |
| 400 | `BUSINESS_RULE_VIOLATION` | Read the Portuguese `message`, e.g. reload a changed schedule or resolve pending grade releases. |
| 401 | `AUTH_UNAUTHORIZED` | Protected route rejected the session. |
| 403 | `AUTH_FORBIDDEN` | Role or object checks denied access. Some domain checks return a 400 business error instead. |
| 404 | `RESOURCE_NOT_FOUND` | A handler could not find the requested resource. |
| 409 | `DATA_CONFLICT` | Registration or class creation encountered a data conflict. |
| 500 | `INTERNAL_ERROR` | Server-side failure. Check the resource's state before retrying a mutation. |

A typical validation body is shown below. Application errors have a numeric `status`, machine-readable `code`, display `message` and RFC 3339 `timestamp`. The Go validation helper currently reports one field at a time. Uploads also use `{"error":"…"}`. Unmatched routes and methods can produce Go router responses outside the JSON envelope. [Error definitions](../../internal/api/errors.go) · [Login handler](../../internal/api/users.go) · [File handler](../../internal/api/storage.go)

```json
{
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "Erro de validação",
  "timestamp": "2026-01-15T10:00:00Z",
  "errors": [{"field": "body", "message": "JSON inválido"}]
}
```

### Try a read and a validation failure

Against the local service, the first command returns `200` and the health object when PostgreSQL is reachable. The second returns `400` and the example validation error with the actual request timestamp. Neither command writes data.

```sh
curl -i http://localhost:8080/health
curl -i -H 'Content-Type: application/json' \
  --data-binary '{' http://localhost:8080/api/auth/login
```

### Sign in and find a class

Start the seeded local API and database using the [development guide](../engineering/development.md). Run this block in Bash with `curl` and `jq`. It uses the disposable administrator account, reads its profile and searches for a cohort by name. Class search uses a POST request and leaves school records unchanged.

```bash
(
  set -euo pipefail
  api_url=http://localhost:8080/api
  export SISGES_DEMO_EMAIL=adm0001@sisges.com
  export SISGES_DEMO_PASSWORD=admin123

  token=$(
    jq -n '{email: env.SISGES_DEMO_EMAIL, password: env.SISGES_DEMO_PASSWORD}' |
      curl -fsS -H 'Content-Type: application/json' \
        --data-binary @- "$api_url/auth/login" |
      jq -er '.accessToken // error(.code // "Missing accessToken")'
  )

  curl -fsS -H "Authorization: Bearer $token" "$api_url/users/me" |
    jq '{register, role, email}'

  curl -fsS -H "Authorization: Bearer $token" -H 'Content-Type: application/json' \
    --data-binary '{"name":"Turma 01"}' "$api_url/classes/search" |
    jq 'map({id, name, academicYear, studentCount, teacherCount})'
)
```

A fresh seed returns profile register `ADM0001` and role `ADMIN`. The search returns an array containing `Turma 01`, grade level `6º ano`, a numeric cohort ID and roster counts. An empty array means the name matched no visible cohort. If login returns `AUTH_INVALID_CREDENTIALS`, check that the local seed ran and that the fixture still has its original password. To work with a class timetable or a dated lesson, use the cohort ID from search and the role required by the operation. [Local seed](../../internal/api/seed.go) · [Timetable and lesson request examples](lessons.md)

## Maintaining the contract

When a route, guard, DTO, status code or database-backed filter changes, update its domain table and any examples. Reconcile the documented method/path pairs with `App.routes`, including the concrete gradebook path dispatched by the wildcard and the WebSocket upgrade. Check the linked handler for object-level rules as well as the router-level role. [Router](../../internal/api/app.go) · [API reference template](../templates/api-reference.md)
