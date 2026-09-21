#!/usr/bin/env bash
set -euo pipefail

ENTRYPOINT="${HERMES_PROCESSING_V1_ENTRYPOINT:-/usr/local/libexec/hermes-processing-v1-entrypoint}"
[[ -x "$ENTRYPOINT" ]] || { echo "Hermes v1 entrypoint is not executable" >&2; exit 70; }
output="$($ENTRYPOINT --readiness)"
[[ "$output" == "HERMES_PROCESSING_V1_READY" ]] || { echo "unexpected Hermes v1 readiness response" >&2; exit 70; }
printf '%s\n' "$output"
