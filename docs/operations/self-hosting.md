# Self-hosting NutriCoach

The reference deployment uses only free and open-source application and infrastructure software. Start with the complete guide:

- [Free and open-source deployment](free-open-source-deployment.md)

## Quick start: local-only

```bash
./scripts/init-deployment.sh local
./scripts/deploy.sh local
```

Open `http://127.0.0.1:8081`.

## Deployment modes

- `local`: loopback-only and recommended for foundation evaluation.
- `lan`: trusted private network only; never expose or port-forward port 8081.
- `https`: Caddy-managed HTTPS plus Basic Authentication for a single protected instance.

The foundation still lacks application accounts and per-user authorization. The HTTPS outer gate is not a multi-tenant security model.

## Stop

```bash
docker compose down
```

HTTPS mode:

```bash
docker compose -f compose.yaml -f compose.https.yaml down
```

Named volumes remain. `down -v` deletes data and is destructive.

## Back up

```bash
./scripts/backup.sh
# Or select an output file:
./scripts/backup.sh /secure/path/nutricoach.dump
```

The script creates a PostgreSQL custom-format dump with restrictive file permissions. Copy backups away from the application host, back up `.env` and `secrets/` separately, and test restoration regularly.

## Restore

```bash
ALLOW_DESTRUCTIVE_RESTORE=1 ./scripts/restore.sh /secure/path/nutricoach.dump
```

Restore stops the application services, replaces matching database objects, and starts the services again. Take a fresh backup before restoring.

## Upgrade

1. Read release and migration notes.
2. Create and verify a backup.
3. Back up `.env` and `secrets/` securely.
4. Select a reviewed release or commit; do not deploy a moving branch blindly.
5. Re-run `./scripts/deploy.sh <mode>`.
6. Run `./scripts/healthcheck.sh <mode>` and a calculation smoke test.
7. Keep the previous release and backup until verification is complete.

PostgreSQL major-version upgrades require a deliberate dump/restore or `pg_upgrade` procedure. Do not change the database image across major versions as an unattended update.

## Security baseline before multi-user hosting

- Application authentication, session management, and recovery controls.
- Per-user authorization tests for every private resource.
- Rate limiting and abuse controls.
- Encrypted off-host backups and tested deletion behavior.
- Monitoring that excludes meal details and sensitive profile data.
- Dependency and container-image patching.
- A documented incident-response and breach-notification process.
