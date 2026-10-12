#!/usr/bin/env bash
set -Eeuo pipefail

ROOT=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
cd "$ROOT"
MODE=${1:-local}

case "$MODE" in local|lan|https) ;; *) echo "Usage: $0 [local|lan|https]" >&2; exit 2 ;; esac
[[ -f .env ]] || { echo ".env is missing; run init-deployment.sh first." >&2; exit 2; }

set -a
# shellcheck disable=SC1091
source .env
set +a

COMPOSE=(docker compose)
if [[ "$MODE" == https ]]; then
  COMPOSE+=( -f compose.yaml -f compose.https.yaml )
else
  COMPOSE+=( -f compose.yaml )
fi

attempt=0
until "${COMPOSE[@]}" exec -T web wget -qO- http://127.0.0.1:8081/ >/dev/null 2>&1; do
  attempt=$((attempt + 1))
  if (( attempt >= 60 )); then
    echo "Web container did not become ready." >&2
    "${COMPOSE[@]}" ps >&2 || true
    exit 1
  fi
  sleep 2
done

"${COMPOSE[@]}" exec -T web \
  wget -qO- http://backend:8080/actuator/health | grep -q '"status":"UP"'

echo "PASS  Internal frontend and backend health checks"

if [[ "$MODE" == local || "$MODE" == lan ]]; then
  curl --fail --silent --show-error "http://127.0.0.1:${WEB_PORT:-8081}/" >/dev/null
  curl --fail --silent --show-error "http://127.0.0.1:${BACKEND_PORT:-8080}/api/v1/meta" >/dev/null
  echo "PASS  Host-accessible web and API endpoints"
else
  "${COMPOSE[@]}" exec -T caddy \
    caddy validate --config /etc/caddy/Caddyfile --adapter caddyfile >/dev/null
  echo "PASS  Caddy HTTPS configuration"

  if [[ -n ${NUTRICOACH_HEALTHCHECK_PASSWORD:-} ]]; then
    site=${NUTRICOACH_SITE_ADDRESS}
    [[ "$site" == http://* || "$site" == https://* ]] || site="https://$site"
    if [[ ${HTTPS_PORT:-443} != 443 && "$site" != *:[0-9]* ]]; then
      site="${site}:${HTTPS_PORT}"
    fi
    auth_user=${NUTRICOACH_ADMIN_USER:-nutricoach}
    curl_args=(--fail --silent --show-error --user "${auth_user}:${NUTRICOACH_HEALTHCHECK_PASSWORD}")
    if [[ ${NUTRICOACH_HEALTHCHECK_INSECURE:-0} == 1 ]]; then
      curl_args+=(--insecure)
    fi
    curl "${curl_args[@]}" "$site/api/v1/meta" >/dev/null
    echo "PASS  Authenticated public endpoint"
  else
    echo "INFO  Set NUTRICOACH_HEALTHCHECK_PASSWORD to test the public authenticated URL."
  fi
fi
