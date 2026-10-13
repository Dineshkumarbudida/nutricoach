#!/usr/bin/env bash
set -euo pipefail
python - <<'PY'
from pathlib import Path
html = Path("public-demo/index.html").read_text(encoding="utf-8")
start = html.index("<script>")
end = html.rindex("</script>")
Path("/tmp/nutricoach-public-demo.js").write_text(html[start + len("<script>"):end], encoding="utf-8")
PY
node --check /tmp/nutricoach-public-demo.js
python - <<'PY'
import json
from pathlib import Path
p=Path("public-demo/data/indian-foods.json")
d=json.loads(p.read_text(encoding="utf-8"))
assert len(d["records"]) == 620, len(d["records"])
assert d["license"] == "CC0-1.0"
print("public-demo JS syntax: OK")
print("bundled real food records:", len(d["records"]))
PY
