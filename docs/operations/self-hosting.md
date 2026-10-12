# Self-hosting the foundation build

The foundation build is intended for private local evaluation. It does not yet include authentication or authorization and must not be exposed to the public internet.

## Start

```bash
cp .env.example .env
# Edit .env and choose a strong POSTGRES_PASSWORD.
docker compose up --build -d
docker compose ps
```

Open `http://localhost:8081`. The backend is also mapped to `http://localhost:8080` for development and API inspection.

## Stop

```bash
docker compose down
```

The named database volume remains. `docker compose down -v` deletes the database and must be treated as destructive.

## Back up

```bash
./scripts/backup.sh
# Or select an output file:
./scripts/backup.sh /secure/path/nutricoach.dump
```

The script creates a PostgreSQL custom-format dump with restrictive file permissions. Copy backups away from the application host and test restoration regularly.

## Restore

```bash
ALLOW_DESTRUCTIVE_RESTORE=1 ./scripts/restore.sh /secure/path/nutricoach.dump
```

Restore stops the application services, replaces matching database objects, and starts the services again. Take a fresh backup before restoring.

## Upgrade

1. Read release notes and migration notes.
2. Create and verify a backup.
3. Pull the selected source-code release; do not deploy a moving branch in production.
4. Rebuild with `docker compose build --pull`.
5. Start with `docker compose up -d`.
6. Check `docker compose ps`, the API health endpoint, and calculation smoke tests.
7. Roll back the application image and restore the backup if a migration is not backward compatible.

PostgreSQL major-version upgrades require a deliberate dump/restore or `pg_upgrade` procedure. Do not change the image from one major version to another as an unattended update.

## Security baseline before public hosting

- Authentication, session management, and password-reset controls.
- Per-user authorization tests for every private resource.
- TLS termination and secure proxy headers.
- Rate limiting, abuse controls, and request-size limits.
- Encrypted backups and a tested deletion policy.
- Secret storage outside source control.
- Monitoring that excludes meal details and sensitive profile data.
- Dependency and container-image patching.
- A documented incident-response and breach-notification process.
