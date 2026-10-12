#!/usr/bin/env bash
set -Eeuo pipefail

ROOT=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
cd "$ROOT"
MODE=${1:-local}

case "$MODE" in
  local|lan) COMPOSE=(docker compose -f compose.yaml) ;;
  https) COMPOSE=(docker compose -f compose.yaml -f compose.https.yaml) ;;
  *) echo "Usage: $0 [local|lan|https]" >&2; exit 2 ;;
esac

./scripts/doctor.sh "$MODE"

set -a
# shellcheck disable=SC1091
source .env
set +a

if [[ "$MODE" == https ]]; then
  if [[ ${NUTRICOACH_SITE_ADDRESS:-} == *example.com* || ${NUTRICOACH_SITE_ADDRESS:-} == http://* ]]; then
    echo "Refusing HTTPS deployment with a placeholder or plain-HTTP site address." >&2
    exit 2
  fi
fi

"${COMPOSE[@]}" config --quiet
if [[ "$MODE" == https ]]; then
  "${COMPOSE[@]}" pull database caddy
else
  "${COMPOSE[@]}" pull database
fi
"${COMPOSE[@]}" build --pull backend web
"${COMPOSE[@]}" up -d --remove-orphans
./scripts/healthcheck.sh "$MODE"

case "$MODE" in
  local)
    echo "NutriCoach is available at http://127.0.0.1:${WEB_PORT:-8081}"
    ;;
  lan)
    echo "NutriCoach is listening on the trusted LAN at port ${WEB_PORT:-8081}."
    echo "Do not forward this port to the public internet."
    ;;
  https)
    echo "NutriCoach HTTPS deployment is running for ${NUTRICOACH_SITE_ADDRESS}."
    ;;
esac
