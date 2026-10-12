# Free and open-source deployment

NutriCoach has a deployment path that requires no paid API, managed database, proprietary hosting platform, or commercial control plane. The reference path runs on a Linux machine you control.

## Stack

| Component | Purpose | License family |
|---|---|---|
| Linux | Host operating system | Open-source distributions available |
| Moby/Docker Engine | OCI container runtime | Apache-2.0 components |
| Docker Compose | Multi-container orchestration | Apache-2.0 |
| PostgreSQL 18 | Persistent database | PostgreSQL License |
| Eclipse Temurin / OpenJDK 21 | Java runtime | GPLv2 with Classpath Exception |
| Spring Boot | Backend framework | Apache-2.0 |
| React | Web application | MIT |
| Nginx | Static web server and internal proxy | 2-clause BSD |
| Caddy 2.11.7 | Optional HTTPS reverse proxy | Apache-2.0 |

The software cost is zero. Hardware, electricity, internet connectivity, backups, and an optional domain name remain the operator's responsibility.

## Security status

The foundation application does not yet implement application-level accounts or per-user authorization. Use one of these modes:

1. `local` — safest default; only the host can open the application.
2. `lan` — trusted private network only; never port-forward it.
3. `https` — single-instance access protected by Caddy HTTPS and Basic Authentication.

HTTPS Basic Authentication is an interim outer gate, not a replacement for future application authentication. Do not use this build for multiple mutually untrusted users or medically sensitive production data.

## Prerequisites

Use a Linux host with approximately:

- 2 CPU cores.
- 2 GB RAM minimum; 4 GB is more comfortable while building images.
- 10 GB free disk plus room for database growth and backups.
- Docker Engine and the Docker Compose plugin.

Docker Desktop is not required. For a strict open-source setup, install the Linux engine and Compose plugin directly from your distribution or the upstream open-source projects.

## Local-only deployment

```bash
git clone https://github.com/Dineshkumarbudida/nutricoach.git
cd nutricoach
./scripts/init-deployment.sh local
./scripts/deploy.sh local
```

Open `http://127.0.0.1:8081`.

## Trusted-LAN deployment

```bash
./scripts/init-deployment.sh lan
./scripts/deploy.sh lan
```

Open `http://SERVER_LAN_IP:8081` from the trusted network. The database is never published, and the backend remains bound to host loopback.

## Public HTTPS deployment

Before starting:

1. Point a DNS record at the server's public IP.
2. Permit inbound TCP 80 and TCP/UDP 443 in the host/network firewall.
3. Do not expose ports 8080 or 8081 publicly.

Initialize:

```bash
NUTRICOACH_SITE_ADDRESS=nutrition.example.org \
  ./scripts/init-deployment.sh https
./scripts/deploy.sh https
```

The initializer displays a generated Basic Authentication password once and stores only its one-way hash. Caddy obtains and renews TLS certificates automatically. Save the displayed password in a password manager.

For a fully non-interactive setup:

```bash
NUTRICOACH_SITE_ADDRESS=nutrition.example.org \
NUTRICOACH_ADMIN_USER=nutricoach \
NUTRICOACH_ADMIN_PASSWORD='use-a-long-unique-password' \
  ./scripts/init-deployment.sh https
./scripts/deploy.sh https
```

## Secret handling

Runtime secrets are stored outside Git-tracked files:

- `secrets/postgres_password` is mounted through Compose secrets.
- `secrets/caddy_users` contains a username and a one-way password hash.
- `.env` contains non-plaintext deployment settings and is ignored by Git.

The initializer applies restrictive local permissions. Back up the secrets separately from the database dump; both are needed for a faithful recovery.

## Operations

Status and health:

```bash
docker compose ps
./scripts/healthcheck.sh local   # or lan / https
```

Logs:

```bash
docker compose logs --tail=200 backend web database
# HTTPS mode:
docker compose -f compose.yaml -f compose.https.yaml logs --tail=200 caddy
```

Backup, rotation, and restore:

```bash
./scripts/backup.sh
BACKUP_RETENTION_DAYS=14 ./scripts/backup-rotate.sh
ALLOW_DESTRUCTIVE_RESTORE=1 ./scripts/restore.sh backups/example.dump
```

Schedule `backup-rotate.sh` with the host's cron or systemd timer and copy verified backups off the application host.

Stop without deleting data:

```bash
docker compose down
# HTTPS mode:
docker compose -f compose.yaml -f compose.https.yaml down
```

Never run `down -v` unless permanent deletion of the PostgreSQL and Caddy volumes is intended.

## Upgrade procedure

1. Back up PostgreSQL and copy the dump off the host.
2. Back up `.env` and `secrets/` securely.
3. Read release and migration notes.
4. Update to a specific reviewed release or commit.
5. Re-run the deployment command.
6. Run the health check and a calculation smoke test.
7. Keep the previous release and backup until verification is complete.

Do not perform an unattended PostgreSQL major-version image change. Use a planned dump/restore or `pg_upgrade` procedure.

## What is deliberately not required

- Paid AI APIs.
- Managed PostgreSQL.
- Render, Railway, Vercel, Netlify, Fly.io, or another proprietary PaaS.
- Coolify, Portainer, or another deployment dashboard.
- A commercial monitoring service.

A dashboard can be added later, but the direct Compose path remains the reference deployment and must continue to work independently.
