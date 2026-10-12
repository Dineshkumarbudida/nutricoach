# Runtime secrets

This directory contains deployment secrets created by `scripts/init-deployment.sh`.
Only this README is committed.

Generated files:

- `postgres_password` — a random PostgreSQL password used through Compose secrets.
- `caddy_users` — a Caddy `basic_auth` username and one-way password hash for HTTPS mode.

The plaintext HTTPS password is shown once during initialization and is never written to disk.
Keep this directory mode-restricted, back it up securely, and never commit generated files.
