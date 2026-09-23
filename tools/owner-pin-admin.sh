#!/usr/bin/env bash
set -euo pipefail
tool_root=$(cd -- "$(dirname -- "$BASH_SOURCE/0")/.." && pwd -P)
if [[ ! -t 0 || ! -t 1 ]]; then
  echo "owner-pin-admin requires an interactive terminal; it never accepts PIN input from a pipe." >&2
  exit 2
fi
exec "$tool_root/gradlew" --quiet ownerPinAdmin --args="$*"
