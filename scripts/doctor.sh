#!/usr/bin/env bash
set -Eeuo pipefail

ROOT=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
cd "$ROOT"
MODE=${1:-local}

case "$MODE" in local|lan|https) ;; *) echo "Usage: $0 [local|lan|https]" >&2; exit 2 ;; esac

failures=0
check() {
  if "$@"; then
    printf 'PASS  %s\n' "$*"
  else
    printf 'FAIL  %s\n' "$*" >&2
    failures=$((failures + 1))
  fi
}

check test -f compose.yaml
check test -f .env
check test -s secrets/postgres_password
check command -v docker
check docker compose version
check docker info

if [[ "$MODE" == https ]]; then
  check test -f compose.https.yaml
  check test -s secrets/caddy_users
fi

if [[ -r /proc/meminfo ]]; then
  memory_kib=$(awk '/MemTotal:/ {print $2}' /proc/meminfo)
  if (( memory_kib < 1800000 )); then
    echo "WARN  Less than about 2 GB RAM detected; builds may fail." >&2
  else
    echo "PASS  At least about 2 GB RAM detected"
  fi
fi

available_kib=$(df -Pk . | awk 'NR==2 {print $4}')
if [[ "$available_kib" =~ ^[0-9]+$ ]] && (( available_kib < 6000000 )); then
  echo "WARN  Less than about 6 GB free disk space detected." >&2
else
  echo "PASS  Sufficient free disk space for a foundation deployment"
fi

if (( failures > 0 )); then
  echo "$failures prerequisite check(s) failed." >&2
  exit 1
fi
