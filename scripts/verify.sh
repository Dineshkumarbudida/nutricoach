#!/usr/bin/env bash
set -euo pipefail
ROOT=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)

required=(
  README.md
  IMPLEMENTATION_REPORT.md
  LICENSE
  compose.yaml
  backend/pom.xml
  backend/Dockerfile
  backend/src/main/resources/application.yml
  backend/src/main/resources/db/migration/V1__baseline.sql
  web/package.json
  web/Dockerfile
  openapi/nutricoach-v1.yaml
  docs/product/PRD.md
  docs/architecture/ADR-0001-modular-monolith.md
  docs/testing/calculation-contract.md
  docs/operations/self-hosting.md
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

if grep -RIn --exclude-dir=.git --exclude='.env.example' \
    -E '(sk-[A-Za-z0-9]{20,}|BEGIN (RSA|OPENSSH|EC) PRIVATE KEY)' "$ROOT"; then
  echo "Potential secret material detected" >&2
  exit 1
fi

if find "$ROOT/data" -type f \( -name '*.csv' -o -name '*.json' -o -name '*.parquet' \) -print | grep -q .; then
  echo "A data payload was added under data/. Complete the rights review before bundling datasets." >&2
  exit 1
fi

echo "Repository structure and shell verification passed."
