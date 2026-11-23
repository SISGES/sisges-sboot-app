# Release and deployment runbook

Use this runbook when either service changes. The checked-in workflows describe an EC2 deployment using Docker Hub images. The host's Compose manifest, secrets, actual image digests and live infrastructure must be obtained from the deployment operator. An application repository alone cannot identify the currently running release. [Backend deployment](../../.github/workflows/deploy.yml) · [Frontend deployment](https://github.com/SISGES/sisges-react-app/blob/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/.github/workflows/deploy.yml)

## Triggers and artifacts

| Push or action | Backend | Frontend |
| --- | --- | --- |
| Push to a feature branch | Neither checked-in push workflow matches | Same |
| Push to `dev` | Image workflow tests with PostgreSQL, runs vet and publishes Docker Hub tag `dev` | Builds and publishes frontend tag `dev` |
| Push to `main`, including documentation-only changes | Image and deployment workflows each test and publish `latest`. Deployment workflow connects to EC2 and recreates the backend service. | Image and deployment workflows each publish `latest`. Deployment workflow connects to EC2 and recreates the frontend service. |
| Manual image workflow | Publishes `latest` for `main` and `dev` for the other selected refs | Same tag selection |

Image names are `<Docker Hub user>/sisges-backend` and `<Docker Hub user>/sisges-frontend`. Both main workflows can write the same mutable tag. Record the pulled digest for each service at release time. Repository secrets referenced by the workflows are `DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN`, `EC2_HOST`, `EC2_USER` and `EC2_SSH_KEY`. [Backend image workflow](../../.github/workflows/docker-publish.yml) · [Frontend image workflow](https://github.com/SISGES/sisges-react-app/blob/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/.github/workflows/docker-publish.yml)

## Release sequence

1. Identify the backend and frontend revisions, affected API operations and newly required migrations. Record existing image digests and the recovery point. Verify that backups include both PostgreSQL and relevant R2 objects for any migration touching file references. [Recovery runbook](recovery.md)
2. Run the relevant suites and inspect the changes against the documented behavior. For a cross-service change, release a backwards-compatible backend first and verify the previous client still works before updating the frontend. [Test strategy](../engineering/test-strategy.md)
3. Confirm the host has Docker Compose, the `~/sisges-deploy/docker-compose.hub.yml` manifest and `.env.prod`. Check the intended service, database target, network alias, TLS termination and seed setting through the operator's configuration. Keep secret values out of the release record.
4. The deploy action pulls and recreates the selected service. On API startup, the process connects to PostgreSQL, applies pending SQL files, optionally seeds, and only then begins listening. Run a single migrator at a time when deploying multiple replicas. A migration failure prevents the new process from serving. [Startup](../../internal/api/app.go) · [Migration runner](../../internal/api/migrate.go)
5. Inspect service state and check `GET /health` on the **API origin**. Nginx in the SPA image proxies `/api/` and `/ws` and otherwise serves the SPA. A 200 HTML response from the frontend root or `/health` on the frontend host does not verify the database.
6. From the intended browser origin, verify login, the current profile and a representative class read. For storage changes, upload and retrieve an allowed fixture in the approved test environment. For feed changes, confirm a subscription and a second client's refresh. Compare the digest on the host to the release record.

## Recovery decision

If the new container fails before serving, save its logs, failing migration version, previous digest and current schema versions. A previous image may be restarted only after checking that it accepts the actual schema and every file namespace in use. Where data changes prevent a code-only rollback, rehearse restoration to a separate target or prepare a reviewed forward migration. The workflows do not supply automatic rollback, backup or post-deploy smoke gates. [Incident and restore procedure](recovery.md)

## Legacy post images

An installation migrating from MinIO may still have database paths under `/api/files/announcements/`. In a controlled migration window, copy the old bucket's `announcements/` objects into `post-img/` in R2, preserving suffixes. Reconcile counts and verify sampled files before repointing the corresponding rows in `sisges.announcement.image_path`. Change only that old prefix to `/api/files/post-img/`. Verify authenticated retrieval through the API before retiring the historical bucket or backup. The current download handler recognizes `post-img/`, `profile-img/` and `lesson-files/`. [Storage implementation](../../internal/api/storage.go)
