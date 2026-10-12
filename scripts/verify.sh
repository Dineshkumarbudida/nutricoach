#!/usr/bin/env bash
set -euo pipefail
ROOT=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)

required=(
  README.md
  PRIVACY.md
  IMPLEMENTATION_REPORT.md
  LICENSE
  compose.yaml
  compose.https.yaml
  deploy/caddy/Caddyfile
  secrets/README.md
  backend/pom.xml
  backend/Dockerfile
  backend/docker-entrypoint.sh
  backend/src/main/resources/application.yml
  backend/src/main/resources/db/migration/V1__baseline.sql
  backend/src/main/java/org/nutricoach/targets/application/DailyTargetService.java
  web/package.json
  web/Dockerfile
  openapi/nutricoach-v1.yaml
  docs/product/PRD.md
  docs/architecture/ADR-0001-modular-monolith.md
  docs/architecture/ADR-0002-free-open-source-deployment.md
  docs/testing/calculation-contract.md
  docs/operations/self-hosting.md
  docs/operations/free-open-source-deployment.md
  docs/research/target-methods.md
  data/catalog/RIGHTS.md
  data/catalog/english-foods.json
  data/catalog/fdc-import-report.json
  public-demo/index.html
  public-demo/styles.css
  public-demo/app.js
  public-demo/planner-core.mjs
  public-demo/data/food-catalog.json
  scripts/build_fdc_catalog.py
  scripts/evidence_watch.py
  scripts/init-deployment.sh
  scripts/deploy.sh
  scripts/doctor.sh
  scripts/healthcheck.sh
  scripts/backup-rotate.sh
)

for path in "${required[@]}"; do
  if [[ ! -s "$ROOT/$path" ]]; then
    echo "Missing or empty required file: $path" >&2
    exit 1
  fi
done

while IFS= read -r script; do
  bash -n "$script"
done < <(find "$ROOT" -type f -name '*.sh' -not -path '*/.git/*' -print)

python3 -m py_compile \
  "$ROOT/scripts/build_fdc_catalog.py" \
  "$ROOT/scripts/evidence_watch.py"

for json_file in \
  "$ROOT/data/catalog/english-foods.json" \
  "$ROOT/data/catalog/fdc-import-report.json" \
  "$ROOT/public-demo/data/food-catalog.json"; do
  python3 -m json.tool "$json_file" >/dev/null
done

python3 - "$ROOT" <<'PY'
import json
import pathlib
import sys

root = pathlib.Path(sys.argv[1])
manifest = json.loads((root / 'data/catalog/english-foods.json').read_text(encoding='utf-8'))
report = json.loads((root / 'data/catalog/fdc-import-report.json').read_text(encoding='utf-8'))
catalog = json.loads((root / 'public-demo/data/food-catalog.json').read_text(encoding='utf-8'))

assert manifest['displayLanguage'] == 'en'
assert manifest['source']['license'] == 'CC0-1.0'
assert not report.get('unresolved'), f"Unresolved FDC foods: {report['unresolved']}"
assert not report.get('ambiguous'), f"Ambiguous FDC foods: {report['ambiguous']}"
assert catalog['displayLanguage'] == 'en'
assert len(catalog['foods']) == len(manifest['foods'])
assert all(food['source']['license'] == 'CC0-1.0' for food in catalog['foods'])
assert all(food.get('displayName') for food in catalog['foods'])
PY

if grep -RIn --exclude-dir=.git --exclude='.env.example' --exclude-dir=secrets \
    -E '(sk-[A-Za-z0-9]{20,}|BEGIN (RSA|OPENSSH|EC) PRIVATE KEY)' "$ROOT"; then
  echo "Potential secret material detected" >&2
  exit 1
fi

while IFS= read -r data_file; do
  relative=${data_file#"$ROOT/"}
  case "$relative" in
    data/catalog/english-foods.json|data/catalog/fdc-import-report.json) ;;
    *)
      echo "Unreviewed data payload found: $relative" >&2
      exit 1
      ;;
  esac
done < <(find "$ROOT/data" -type f \( -name '*.csv' -o -name '*.json' -o -name '*.parquet' \) -print)

if command -v git >/dev/null 2>&1 && git -C "$ROOT" rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  for secret_path in .env secrets/postgres_password secrets/caddy_users; do
    if git -C "$ROOT" ls-files --error-unmatch "$secret_path" >/dev/null 2>&1; then
      echo "A generated deployment secret is tracked by Git: $secret_path" >&2
      exit 1
    fi
  done
fi

echo "Repository structure, reviewed catalog, deployment files, and shell verification passed."
