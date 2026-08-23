#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
OUT="$ROOT/out"
rm -rf "$OUT"
mkdir -p "$OUT"

mapfile -t FILES < <(find "$ROOT/src" "$ROOT/test" -name '*.java' | sort)
javac --release 8 -encoding UTF-8 -d "$OUT" "${FILES[@]}"
echo "Running interview snippet tests (Java 8)"
java -cp "$OUT" iv.TestRunner
