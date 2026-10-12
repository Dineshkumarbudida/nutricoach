#!/usr/bin/env bash
set -euo pipefail
ROOT=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
OUT="$ROOT/target/core-test-classes"
SOURCES="$ROOT/target/core-test-sources.txt"

rm -rf "$OUT"
mkdir -p "$OUT" "$ROOT/target"

find "$ROOT/src/main/java/org/nutricoach/nutrition/domain" -name '*.java' -print > "$SOURCES"
find "$ROOT/src/main/java/org/nutricoach/nutrition/application" -name '*.java' -print >> "$SOURCES"
find "$ROOT/src/main/java/org/nutricoach/targets/domain" -name '*.java' -print >> "$SOURCES"
find "$ROOT/src/main/java/org/nutricoach/targets/application" -name '*.java' -print >> "$SOURCES"
find "$ROOT/src/core-test/java" -name '*.java' -print >> "$SOURCES"

javac --release 21 -Xlint:all -Werror -d "$OUT" @"$SOURCES"
java -ea -cp "$OUT" org.nutricoach.nutrition.CoreCalculationSmokeTest
java -ea -cp "$OUT" org.nutricoach.targets.DailyTargetServiceSmokeTest
