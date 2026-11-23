# Run SISGES locally

The two repositories build independently. The steps below start PostgreSQL and the Go API with the backend Compose file and serve the React client with Vite. They use disposable school data and require Git, Docker with Compose, and Node with npm. The frontend Dockerfile uses Node 20. Use a Node release supported by the locked dependencies when developing on the host. [Backend Compose](../../docker-compose.yml) · [Frontend build](https://github.com/SISGES/sisges-react-app/blob/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/Dockerfile)

## Start a development session

1. In an empty parent directory, clone both repositories with the sibling names expected by Compose.

   ```sh
   git clone https://github.com/SISGES/sisges-sboot-app.git backend
   git clone https://github.com/SISGES/sisges-react-app.git frontend
   ```

2. From the parent, start the API and its database. The backend Compose file enables the demonstration seed and exposes the API on port 8080. It keeps PostgreSQL on the Compose network.

   ```sh
   docker compose -f backend/docker-compose.yml up -d --build backend
   docker compose -f backend/docker-compose.yml logs --tail=50 backend
   curl --fail http://localhost:8080/health
   ```

   Startup applies migrations first. Once the API is ready, health returns `{"status":"ok"}`. If startup fails, inspect the API container logs for a configuration, database or migration error. An empty R2 endpoint allows workflows without attachments.

3. In a second terminal, from the frontend checkout, run the client on the origin allowed by the backend Compose file.

   ```sh
   npm ci
   VITE_API_BASE_URL=http://localhost:8080/api npm run dev -- --port 3001 --strictPort
   ```

   Open `http://localhost:3001`. The local demonstration admin is `adm0001@sisges.com` with password `admin123`. Sample teacher and student emails come from the development seed. These accounts and the Compose signing key are fixtures for local use. Vite's configured default is port 3000, which requires a matching API CORS configuration for direct cross-origin requests. [Seed](../../internal/api/seed.go) · [Vite config](https://github.com/SISGES/sisges-react-app/blob/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/vite.config.ts)

4. Stop Vite with Ctrl-C. From the parent, stop the containers with `docker compose -f backend/docker-compose.yml down`. The named volume preserves school data between sessions. For a fresh fixture, start a separately named Compose project with an empty volume after freeing the host ports.

## Run the Go service on the host

Use Go 1.24 or later with a dedicated PostgreSQL database reachable from the host. The user needs permission to create the `sisges` schema, tables and indexes. Create a local signing key and run from the backend checkout so the migration directory resolves.

```sh
export DATABASE_URL='postgresql://localhost/sisges_dev?sslmode=disable'
export SECURITY_JWT_SECRET_KEY="$(openssl rand -hex 32)"
export SISGES_SEED_ENABLED=true
go run ./cmd/sisges
```

Supply the connection credentials through your local PostgreSQL configuration or environment. The default backend Compose database has no published port. The original two-repository workspace also has a parent-level `compose.yaml` that publishes port 5432. Standalone clones can use the tracked backend Compose file in the first procedure. [Configuration reference](../operations/configuration.md)

## Integration issues to recognize

| Symptom | Check |
| --- | --- |
| Browser rejects cross-origin API calls | Match the browser's exact protocol, host and port to the configured allowed origins. Rebuild the client image after a build-time URL change. |
| Containerized frontend exits with an upstream resolution error | The Nginx config names `sisges-backend`. The tracked development Compose service is named `backend`. Use the Vite procedure above, or provide the matching network alias in your local Compose override. |
| Refresh returns a signed-in browser to login | At the documented baseline, the API validation response is an empty 200 and the client GET helper attempts JSON parsing. Treat reload as a separate integration check. |
| Health is green and a file upload fails | Health checks the database. Uploads also require a configured R2 account endpoint, signing credentials, writable temp space, an accepted destination and a supported file type. |

[API client guide](frontend.md) · [Troubleshooting and recovery](../operations/recovery.md)
