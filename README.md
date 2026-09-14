# SISGES backend (Go)

Memory-conscious Go backend for the SISGES school management application. It preserves the existing `/api` HTTP contract and PostgreSQL schema while replacing Spring Boot, Hibernate/JPA, embedded Tomcat, and the JVM.

## Run locally

The complete stack is available through Compose:

```sh
docker compose up --build
```

The API listens on `http://localhost:8080`, the React app on `http://localhost:3001`, MinIO on `http://localhost:9000`, and PostgreSQL is private to the Compose network. The development seed creates:

- e-mail: `adm0001@sisges.com`
- password: `admin123`

For a backend-only run, install Go 1.24+ and PostgreSQL 16+, then set at least:

```sh
export DATABASE_URL='postgresql://postgres:postgres@localhost:5432/sisges?sslmode=disable'
export SECURITY_JWT_SECRET_KEY='replace-with-at-least-32-random-bytes'
go run ./cmd/sisges
```

## Configuration

| Variable | Default | Purpose |
| --- | --- | --- |
| `PORT` | `8080` | HTTP port |
| `DATABASE_URL` | required | PostgreSQL connection URI |
| `SECURITY_JWT_SECRET_KEY` | required | HS256 signing key; at least 32 bytes |
| `SECURITY_JWT_EXPIRATION_TIME` | `3600000` | Token lifetime in milliseconds |
| `SISGES_DB_MAX_CONNECTIONS` | `6` | Hard upper bound for the PostgreSQL pool |
| `GOMEMLIMIT_BYTES` | `167772160` | Go runtime soft memory limit (160 MiB) |
| `GOGC_PERCENT` | `75` | GC target percentage |
| `SISGES_CORS_ALLOWED_ORIGIN_PATTERNS` | local + Vercel | Comma-separated origin patterns |
| `SISGES_MIGRATION_DIR` | `db/migration` | SQL migration directory |
| `SISGES_SEED_ENABLED` | `false` | Create a small development dataset |
| `SISGES_MINIO_ENDPOINT` | unset | Enables object storage when set |
| `SISGES_MINIO_ACCESS_KEY` | unset | MinIO access key |
| `SISGES_MINIO_SECRET_KEY` | unset | MinIO secret key |
| `SISGES_MINIO_BUCKET` | unset | MinIO bucket |
| `SISGES_MINIO_REGION` | `us-east-1` | S3 signing region |

The legacy `SPRING_DATASOURCE_*` variables are still accepted during deployment migration, but they no longer have unsafe credential defaults.

## Memory design

- Standard-library HTTP server and router; no reflection-heavy web framework.
- Direct, typed SQL through `pgx`; no ORM identity map or entity graph retention.
- Zero idle database connections and a six-connection default ceiling.
- Bounded JSON request bodies and bounded query result sizes.
- File uploads spool to a temporary file while hashing, keeping the 10 MiB payload off the Go heap.
- File downloads stream directly from object storage to the client.
- A static, stripped production binary in a `scratch` image.
- Runtime memory limit and GC target applied in-process, even if environment-level `GOMEMLIMIT` is absent.

## Database migrations

Migrations remain compatible with databases previously managed by Flyway. On startup, the Go runner recognizes successful entries in `sisges.flyway_schema_history`, applies only missing migrations, and records new work in `sisges.go_schema_migrations`.

## Verification

```sh
go test ./...
go test -run '^$' -bench . -benchmem ./internal/api
go vet ./...
```

`GET /health` reports database connectivity. All application routes remain under `/api`.
