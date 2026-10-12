#!/usr/bin/env bash
set -Eeuo pipefail

ROOT=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
cd "$ROOT"

MODE=${1:-local}
INPUT_SITE_ADDRESS=${NUTRICOACH_SITE_ADDRESS:-}
INPUT_ADMIN_USER=${NUTRICOACH_ADMIN_USER:-}
INPUT_ADMIN_PASSWORD=${NUTRICOACH_ADMIN_PASSWORD:-}
CADDY_IMAGE=${CADDY_IMAGE:-caddy:2.11.7-alpine}

usage() {
  cat <<'USAGE'
Usage: ./scripts/init-deployment.sh [local|lan|https]

local  Bind the web UI and backend to 127.0.0.1.
lan    Bind only the web UI to all interfaces; use only on a trusted LAN.
https  Configure Caddy HTTPS and Basic Authentication for a real DNS name.

For non-interactive HTTPS setup, provide:
  NUTRICOACH_SITE_ADDRESS=nutrition.example.com
  NUTRICOACH_ADMIN_USER=nutricoach
  NUTRICOACH_ADMIN_PASSWORD='a long unique password'
USAGE
}

case "$MODE" in
  local|lan|https) ;;
  -h|--help) usage; exit 0 ;;
  *) usage >&2; exit 2 ;;
esac

if ! command -v docker >/dev/null 2>&1 || ! docker compose version >/dev/null 2>&1; then
  echo "Docker Engine and the Docker Compose plugin are required." >&2
  echo "Use the open-source Linux engine/CLI, not Docker Desktop, for the strict FOSS path." >&2
  exit 1
fi

random_secret() {
  if command -v openssl >/dev/null 2>&1; then
    openssl rand -base64 48 | tr -d '\n'
  else
    head -c 48 /dev/urandom | base64 | tr -d '\n'
  fi
}

set_env_value() {
  local key=$1 value=$2 temporary
  temporary=$(mktemp)
  awk -v key="$key" -v value="$value" '
    BEGIN { found = 0 }
    index($0, key "=") == 1 { print key "=" value; found = 1; next }
    { print }
    END { if (!found) print key "=" value }
  ' .env > "$temporary"
  mv "$temporary" .env
  chmod 600 .env
}

umask 077
mkdir -p secrets
chmod 700 secrets

if [[ ! -f .env ]]; then
  cp .env.example .env
fi
chmod 600 .env

# Load existing non-secret settings so HTTPS re-initialization can reuse them.
set -a
# shellcheck disable=SC1091
source .env
set +a

if [[ ! -s secrets/postgres_password ]]; then
  random_secret > secrets/postgres_password
fi
# Compose mounts a local secret with its source-file mode. The containing
# directory remains owner-only, while 0444 lets non-root service users read the
# read-only mount inside only the containers to which it is explicitly granted.
chmod 444 secrets/postgres_password

case "$MODE" in
  local)
    set_env_value WEB_BIND_ADDRESS 127.0.0.1
    set_env_value BACKEND_BIND_ADDRESS 127.0.0.1
    ;;
  lan)
    set_env_value WEB_BIND_ADDRESS 0.0.0.0
    set_env_value BACKEND_BIND_ADDRESS 127.0.0.1
    ;;
  https)
    set_env_value WEB_BIND_ADDRESS 127.0.0.1
    set_env_value BACKEND_BIND_ADDRESS 127.0.0.1

    SITE_ADDRESS=${INPUT_SITE_ADDRESS:-${NUTRICOACH_SITE_ADDRESS:-}}
    if [[ -z "$SITE_ADDRESS" && -t 0 ]]; then
      read -r -p "Public DNS name (for example nutrition.example.com): " SITE_ADDRESS
    fi
    if [[ -z "$SITE_ADDRESS" || "$SITE_ADDRESS" == *example.com* || "$SITE_ADDRESS" == http://* ]]; then
      echo "HTTPS mode requires a real DNS name and must not use plain HTTP." >&2
      exit 2
    fi
    set_env_value NUTRICOACH_SITE_ADDRESS "$SITE_ADDRESS"

    ADMIN_USER=${INPUT_ADMIN_USER:-${NUTRICOACH_ADMIN_USER:-nutricoach}}
    if [[ ! "$ADMIN_USER" =~ ^[A-Za-z0-9._-]+$ ]]; then
      echo "NUTRICOACH_ADMIN_USER may contain only letters, numbers, dot, underscore, and dash." >&2
      exit 2
    fi

    GENERATED_PASSWORD=0
    ADMIN_PASSWORD=${INPUT_ADMIN_PASSWORD:-}
    if [[ -z "$ADMIN_PASSWORD" ]]; then
      ADMIN_PASSWORD=$(random_secret)
      GENERATED_PASSWORD=1
    fi
    if (( ${#ADMIN_PASSWORD} < 16 )); then
      echo "The HTTPS password must contain at least 16 characters." >&2
      exit 2
    fi

    set_env_value NUTRICOACH_ADMIN_USER "$ADMIN_USER"

    PASSWORD_HASH=$(docker run --rm "$CADDY_IMAGE" \
      caddy hash-password --plaintext "$ADMIN_PASSWORD")
    printf '%s %s\n' "$ADMIN_USER" "$PASSWORD_HASH" > secrets/caddy_users
    chmod 600 secrets/caddy_users

    echo
    echo "HTTPS credentials (save them now; the plaintext password is not stored):"
    DISPLAY_SITE=$SITE_ADDRESS
    [[ "$DISPLAY_SITE" == http://* || "$DISPLAY_SITE" == https://* ]] || DISPLAY_SITE="https://$DISPLAY_SITE"
    echo "  Site:     $DISPLAY_SITE"
    echo "  Username: $ADMIN_USER"
    echo "  Password: $ADMIN_PASSWORD"
    if [[ $GENERATED_PASSWORD -eq 0 ]]; then
      echo "  Source:   NUTRICOACH_ADMIN_PASSWORD environment variable"
    fi
    ;;
esac

echo
echo "Deployment configuration initialized for mode: $MODE"
echo "Next command: ./scripts/deploy.sh $MODE"
