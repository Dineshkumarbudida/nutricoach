#!/usr/bin/env bash
set -euo pipefail

ROOT=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
cd "$ROOT"

if [[ $# -ne 1 ]]; then
  echo "Usage: ALLOW_DESTRUCTIVE_RESTORE=1 $0 <backup.dump>" >&2
  exit 2
fi

if [[ ${ALLOW_DESTRUCTIVE_RESTORE:-0} != 1 ]]; then
  echo "Restore is destructive. Re-run with ALLOW_DESTRUCTIVE_RESTORE=1." >&2
  exit 2
fi

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker is required for the reference restore command." >&2
  exit 1
fi

BACKUP=$1
if [[ ! -s "$BACKUP" ]]; then
  echo "Backup file does not exist or is empty: $BACKUP" >&2
  exit 2
fi

set -a
if [[ -f .env ]]; then
  # shellcheck disable=SC1091
  source .env
fi
set +a

DATABASE=${POSTGRES_DB:-nutricoach}
USER_NAME=${POSTGRES_USER:-nutricoach}

cleanup() {
  docker compose start backend web >/dev/null 2>&1 || true
}
trap cleanup EXIT

docker compose stop web backend
cat "$BACKUP" | docker compose exec -T database \
  pg_restore --username "$USER_NAME" --dbname "$DATABASE" \
  --clean --if-exists --no-owner --no-privileges --exit-on-error

printf 'Restore completed from: %s\n' "$BACKUP"
