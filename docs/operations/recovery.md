# Service incidents, backup and restore

Audience — deployment operator, backend engineer and school data owner. The observations below follow the service code. The backup and response procedures are proposed operating practices. The repositories supply no backup schedule, on-call roster, incident notification window, recovery-point objective (RPO) or recovery-time objective (RTO).

## Triage by symptom

| Signal | First check | Next action |
| --- | --- | --- |
| API unavailable after deploy | Inspect container state and startup logs for configuration, DB connection, R2 initialization or a named migration failure. | Compare the new environment and image digest to the last working release. Preserve the failed SQL version and inspect migration history before retrying. |
| Database check fails | `GET /health` performs a database ping with a one-second timeout. | Check PostgreSQL reachability, connection budget, latency and database availability. Confirm response body and origin so the SPA fallback cannot mask a failure. |
| Invalid login appears successful in HTTP metrics | The login contract returns HTTP 200 with an application error for invalid credentials. | Use the result code and presence of a token in a synthetic probe. Keep passwords, tokens and bodies out of logs. |
| File operations fail alone | Inspect storage configuration, target prefix, proxy body limit, temp directory and object response. | Compare the DB reference with the object inventory. A 503 on upload can indicate unconfigured storage. Validate with an authorized synthetic account. |
| Timetable save rejected | Inspect the revision and class discipline assignments. | Re-read the schedule and reconcile drafts before saving. A retry with a stale revision cannot resolve the conflict. |
| Feed content is stale | Check whether an HTTP refresh succeeds and whether the browser reached the correct WS host. | Check upgrade routing and per-process fan-out. Recover visible state with a fresh HTTP read. |
| Potential exposure of school data | Record time, affected data category, service and scope privately. | Restrict affected access and involve the responsible operator and school contact. Use redacted identifiers in incident notes. Validate access before reopening the workflow. |

[Health and routing](../../internal/api/app.go) · [Error handling](../../internal/api/errors.go) · [Access and data handling](security-privacy.md)

## Recovery drill for PostgreSQL and R2

1. Identify the source deployment, backup time, image digests and file-store account. Choose a compatible PostgreSQL version, an **empty isolated destination**, and a protected storage location for the archive. Coordinate a write pause or other consistent snapshot strategy when matching files to database records. A database transaction snapshot alone does not establish a recovery point across R2.
2. With libpq credentials supplied privately, take a custom-format dump and inspect its contents. These Unix shell examples assume operator-set database names or connection references. The backup directory should be access-controlled and encrypted according to the deployment's policy.

   ```sh
   umask 077
   pg_dump --format=custom --file="$BACKUP_DIR/sisges.dump" --dbname="$SOURCE_DATABASE"
   pg_restore --list "$BACKUP_DIR/sisges.dump"
   ```

3. Inventory and preserve object keys and versions or copies for `post-img/`, `profile-img/` and `lesson-files/`. Include private backups of the environment and deploy manifest through the operator's secret store. For a restore under a separate storage account, the binary still requires a bucket named `sisges-prd`.
4. Provision the empty recovery database and restore into it. The example omits original ownership and grants so the operator can apply environment-specific roles in the isolated destination. Restore those grants separately before reconnecting the service.

   ```sh
   pg_restore --exit-on-error --no-owner --no-acl \
     --dbname="$EMPTY_RECOVERY_DATABASE" "$BACKUP_DIR/sisges.dump"
   psql -X --dbname="$EMPTY_RECOVERY_DATABASE" \
     -c 'SELECT version, name FROM sisges.go_schema_migrations ORDER BY version;'
   ```

5. Compare row counts for school classes, users, meetings and grades with the source manifest. Reconcile successful historical Flyway entries if present. Start the matching API image with seed disabled, the restored database and the matching object set. Exercise health, login, class lookup, one timetable, a student's report and representative authorized file reads. Use a restricted test environment for any real school data.
6. Record the verified recovery point, elapsed recovery time, unavailable objects and mismatched rows. Obtain the data owner's acceptance of the restored state and lost-write window before any production cutover. Retain the prior source during reconciliation and dispose of temporary copies under the approved retention policy.

The repository has no down-migration runner. Every completed version remains committed when a later migration fails. Restoring only the binary, or only PostgreSQL after a file migration, may leave incompatible state. [Migration runner](../../internal/api/migrate.go) · [Data model](../architecture/data-model.md)

## Operational evidence to collect

Record deployment and migration errors, API request rates and latency, failed auth outcomes, database connection pressure, object-store error rates, and time since the last successful restore drill. Treat alert thresholds and contact routing as decisions for the service owner. Logs and traces should omit URL query tokens, credentials, student grades, guardian contacts and file contents. The application emits process and request error logs. External metrics, redaction, alert routing and retention require deployment evidence.
