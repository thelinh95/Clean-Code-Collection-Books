#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
MODE="${1:-stubs}"
case "$MODE" in
  stubs) SRC="$ROOT/src" ;;
  solutions) SRC="$ROOT/solutions" ;;
  *)
    echo "Usage: $0 [stubs|solutions]"
    exit 2
    ;;
esac

OUT="$ROOT/out"
rm -rf "$OUT"
mkdir -p "$OUT"

mapfile -t FILES < <(find "$SRC" "$ROOT/test" -name '*.java' | sort)
javac --release 21 -encoding UTF-8 -d "$OUT" "${FILES[@]}"
echo "Running tests against: $MODE"
java -cp "$OUT" conc.TestRunner
