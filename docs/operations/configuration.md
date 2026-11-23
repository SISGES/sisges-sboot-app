# Runtime and build configuration

Operators configure the API process before startup and the SPA before bundling. The following defaults come from code. Compose files and image build arguments may override them. [Go config loader](../../internal/api/config.go) · [API entry point](../../cmd/sisges/main.go) · [Example environment](../../.env.example)

| API variable | Default or requirement | Operational effect |
| --- | --- | --- |
| `PORT` | 8080 | Go listens on this port on all interfaces. |
| `DATABASE_URL` | Required unless the legacy URL is supplied | pgx connection string. Configure database credentials and transport for the environment. |
| `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | Legacy fallback | The loader strips a leading `jdbc:` and can insert credentials into a PostgreSQL URI. An explicit `DATABASE_URL` wins. |
| `SECURITY_JWT_SECRET_KEY` | At least 32 bytes | HMAC signing key. All instances that accept the same sessions need a coordinated value. Rotating it invalidates tokens signed with the old value on subsequent verification. |
| `SECURITY_JWT_EXPIRATION_TIME` | 3600000 milliseconds | Login token lifetime. Keep units explicit when changing it. |
| `SISGES_CORS_ALLOWED_ORIGIN_PATTERNS` | Localhost, loopback and HTTPS Vercel subdomains | Comma-separated exact origins or wildcard patterns. The service reflects an allowed origin, sets credentials and permits Authorization and Content-Type. Set explicit deployment origins. |
| `SISGES_MIGRATION_DIR` | `db/migration` | SQL directory relative to working directory. The image supplies `/db/migration`. |
| `SISGES_SEED_ENABLED` | false | Case-insensitive `true` enables the demo dataset. The tracked development stacks enable it. |
| `SISGES_DB_MAX_CONNECTIONS` | 6, valid range 1–32 | Per-process pool ceiling. Allow headroom for every replica, database maintenance and recovery. |
| `GOMEMLIMIT_BYTES` | 167772160 | Go runtime soft memory target in bytes. Container memory needs additional headroom. |
| `GOGC_PERCENT` | 75 | Applied at startup through the Go runtime. |
| `SISGES_R2_ENDPOINT` | Empty | Account endpoint without a bucket path. Empty disables object storage. Use an HTTPS endpoint for hosted storage. |
| `SISGES_R2_ACCESS_KEY_ID`, `SISGES_R2_SECRET_ACCESS_KEY` | Required when endpoint is set | Server-side request signing credentials. Keep them out of frontend build args and source control. |
| `SISGES_R2_BUCKET` | `sisges-prd` | This is the only accepted bucket name when storage is enabled. Use isolated accounts or fixtures for storage tests. |
| `SISGES_R2_REGION` | `auto` | Signing region. |

Object storage covers post images, profile images and lesson files. The example environment contains illustrative R2 values. For local work without storage, leave the endpoint empty. The Go process reads its environment directly. Compose interpolates selected variables from its environment or matching env file. [Storage adapter](../../internal/api/storage.go) · [Tracked Compose file](../../docker-compose.yml)

| Frontend setting | Where it applies | Effect |
| --- | --- | --- |
| `VITE_API_BASE_URL` | Vite development process or Docker build argument | Includes the `/api` suffix. Default in the client source is `http://localhost:8080/api`. Production uses `/api` for same-origin HTTP. Changing container runtime environment after the bundle is built does not rewrite that URL. |
| Vite proxy and port | Development server | Port 3000 by default. Paths `/api` and `/ws` proxy to the local API if requested through Vite. |
| Nginx upstream | Container networking | `sisges-backend:8080` must resolve inside the frontend container. Configure the network or alias accordingly. |

The live-feed hook also derives the WebSocket host from the API URL. An absolute HTTPS API base yields a WSS host. The baseline builder has no host when the base is just `/api`. Also verify that the STOMP client dispatches server messages to its subscription callback. The baseline server omits the subscription header. A periodic HTTP refresh can mask a failed notification. [Client configuration](https://github.com/SISGES/sisges-react-app/blob/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/vite.config.ts) · [Nginx](https://github.com/SISGES/sisges-react-app/blob/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/nginx.docker.conf) · [Feed hook](https://github.com/SISGES/sisges-react-app/blob/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/src/hooks/useStompFeed.ts)
