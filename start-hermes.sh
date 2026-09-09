#!/usr/bin/env bash
set -euo pipefail

die() {
    printf 'Hermes startup failed: %s\n' "$1" >&2
    exit 1
}

[[ "$(hostname)" == "hermes" ]] || die "this startup script must run on hostname hermes"
SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
REPO_ROOT=$(git -C "$SCRIPT_DIR" rev-parse --show-toplevel 2>/dev/null) || die "not inside a Parker git repository"
[[ "$REPO_ROOT" == "$SCRIPT_DIR" ]] || die "start-hermes.sh must be in the Parker repository root"
cd "$REPO_ROOT"
if [[ -f "$REPO_ROOT/.venv/bin/activate" ]]; then
    # Deliberately source only; this script never creates, changes, or installs into .venv.
    source "$REPO_ROOT/.venv/bin/activate"
fi
TOKEN_FILE=${HERMES_PARKER_TOKEN_FILE:-"$HOME/.hermes/parker-agent-gateway-token"}
[[ -f "$TOKEN_FILE" ]] || die "Parker Agent Gateway token file is missing: $TOKEN_FILE"
token_mode=$(stat -c '%a' "$TOKEN_FILE" 2>/dev/null) || die "cannot inspect token file permissions"
(( (8#$token_mode & 0177) == 0 )) || die "Parker Agent Gateway token file permissions are broader than 0600"
IFS= read -r PARKER_AGENT_GATEWAY_TOKEN < "$TOKEN_FILE" || die "cannot read Parker Agent Gateway token file"
[[ -n "$PARKER_AGENT_GATEWAY_TOKEN" ]] || die "Parker Agent Gateway token file is empty"
export PARKER_AGENT_GATEWAY_TOKEN

PARKER_GATEWAY_URL=${PARKER_GATEWAY_URL:-http://192.168.178.44:8090}
export PARKER_GATEWAY_URL
status=$(curl --silent --show-error --connect-timeout 3 --max-time 10 -o /dev/null -w '%{http_code}' \
    -H "Authorization: Bearer $PARKER_AGENT_GATEWAY_TOKEN" \
    "$PARKER_GATEWAY_URL/agent/evidence/test") \
    || die "Parker Gateway connection failed"
[[ "$status" != "401" ]] || die "Parker Gateway authentication failed (401)"
[[ "$status" == "404" ]] || die "Parker Gateway returned unexpected readiness HTTP $status"

printf 'Hermes READY\nParker Gateway: reachable\nAuthentication: OK\n\n'
if [[ -t 0 && -t 1 ]]; then
    exec "${SHELL:-/bin/bash}" -i
fi
