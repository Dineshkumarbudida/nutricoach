#!/usr/bin/env bash
set -euo pipefail

ROOT=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
cd "$ROOT"

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker is required for the reference backup command." >&2
  exit 1
fi

set -a
if [[ -f .env ]]; then
  # shellcheck disable=SC1091
  source .env
fi
set +a

DATABASE=${POSTGRES_DB:-nutricoach}
USER_NAME=${POSTGRES_USER:-nutricoach}
OUTPUT=${1:-"backups/nutricoach-$(date -u +%Y%m%dT%H%M%SZ).dump"}
mkdir -p "$(dirname "$OUTPUT")"

umask 077
docker compose exec -T database \
  pg_dump --username "$USER_NAME" --dbname "$DATABASE" \
  --format=custom --compress=9 --no-owner --no-privileges > "$OUTPUT"

if [[ ! -s "$OUTPUT" ]]; then
  echo "Backup was created but is empty: $OUTPUT" >&2
  exit 1
fi

printf 'Backup created: %s\n' "$OUTPUT"
