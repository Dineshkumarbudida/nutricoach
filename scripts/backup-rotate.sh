#!/usr/bin/env bash
set -Eeuo pipefail

ROOT=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
cd "$ROOT"

RETENTION_DAYS=${BACKUP_RETENTION_DAYS:-14}
if [[ ! "$RETENTION_DAYS" =~ ^[0-9]+$ ]] || (( RETENTION_DAYS < 1 )); then
  echo "BACKUP_RETENTION_DAYS must be a positive integer." >&2
  exit 2
fi

output="backups/nutricoach-$(date -u +%Y%m%dT%H%M%SZ).dump"
./scripts/backup.sh "$output"

if command -v sha256sum >/dev/null 2>&1; then
  sha256sum "$output" > "${output}.sha256"
elif command -v shasum >/dev/null 2>&1; then
  shasum -a 256 "$output" > "${output}.sha256"
else
  echo "WARN: no SHA-256 utility found; checksum was not created." >&2
fi

find backups -type f \( -name 'nutricoach-*.dump' -o -name 'nutricoach-*.dump.sha256' \) \
  -mtime "+$RETENTION_DAYS" -print -delete

echo "Backup rotation complete; retention is $RETENTION_DAYS day(s)."
