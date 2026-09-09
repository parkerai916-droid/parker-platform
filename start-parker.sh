#!/usr/bin/env bash
set -euo pipefail

die() {
    printf 'Parker startup failed: %s\n' "$1" >&2
    exit 1
}

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
REPO_ROOT=$(git -C "$SCRIPT_DIR" rev-parse --show-toplevel 2>/dev/null) || die "not inside a Parker git repository"
[[ "$REPO_ROOT" == "$SCRIPT_DIR" ]] || die "start-parker.sh must be in the Parker repository root"
cd "$REPO_ROOT"
[[ "$(hostname)" == "parker" ]] || die "this startup script must run on hostname parker"
[[ -z "$(git status --porcelain)" ]] || die "git working tree is not clean; refusing production startup"

PARKER_BUILD_COMMIT=$(git rev-parse HEAD) || die "cannot determine git HEAD"
PARKER_PRODUCTION_COMMIT="$PARKER_BUILD_COMMIT"
SECRET_FILE=${PARKER_OWNER_HIGH_AUTHORITY_VERIFICATION_SECRET_FILE:-/mnt/parker-secrets/parker/owner-high-authority-verification.secret}
sudo test -f "$SECRET_FILE" || die "Owner high-authority verification secret file is missing: $SECRET_FILE"

resolve_env_value() {
    local name=$1 value
    value=${!name-}
    if [[ -n "$value" ]]; then printf '%s' "$value"; return; fi
    if [[ -f .env ]]; then
        awk -v wanted="$name" '$0 ~ "^[[:space:]]*" wanted "=" { sub("^[[:space:]]*" wanted "=", ""); sub("[[:space:]]*#.*$", ""); print; exit }' .env
    fi
}

OWNER_HIGH_AUTHORITY_PRINCIPAL_ID=$(resolve_env_value PARKER_OWNER_HIGH_AUTHORITY_PRINCIPAL_ID)
[[ -n "$OWNER_HIGH_AUTHORITY_PRINCIPAL_ID" ]] || die "PARKER_OWNER_HIGH_AUTHORITY_PRINCIPAL_ID is not set in the environment or .env"

if ! sudo env \
    PARKER_OWNER_HIGH_AUTHORITY_VERIFICATION_SECRET_FILE="$SECRET_FILE" \
    PARKER_OWNER_HIGH_AUTHORITY_PRINCIPAL_ID="$OWNER_HIGH_AUTHORITY_PRINCIPAL_ID" \
    PARKER_BUILD_COMMIT="$PARKER_BUILD_COMMIT" \
    PARKER_PRODUCTION_COMMIT="$PARKER_PRODUCTION_COMMIT" \
    docker compose up -d --build parker; then
    die "docker compose could not start parker"
fi

deadline=$((SECONDS + ${PARKER_STARTUP_TIMEOUT_SECONDS:-90}))
while (( SECONDS < deadline )); do
    running=$(docker inspect --format '{{.State.Running}}' parker-runtime 2>/dev/null || true)
    if [[ "$running" == "true" ]] && curl --silent --show-error --connect-timeout 1 --max-time 2 -o /dev/null http://127.0.0.1:8080/ && curl --silent --show-error --connect-timeout 1 --max-time 2 -o /dev/null http://127.0.0.1:8090/; then break; fi
    sleep 2
done
[[ "$(docker inspect --format '{{.State.Running}}' parker-runtime 2>/dev/null || true)" == "true" ]] || die "container parker-runtime did not become running"
curl --silent --show-error --connect-timeout 2 --max-time 3 -o /dev/null http://127.0.0.1:8080/ || die "Owner UI port 8080 is not reachable"
curl --silent --show-error --connect-timeout 2 --max-time 3 -o /dev/null http://127.0.0.1:8090/ || die "Agent Gateway port 8090 is not reachable"
running_commit=$(docker exec parker-runtime printenv PARKER_PRODUCTION_COMMIT 2>/dev/null || true)
[[ "$running_commit" == "$PARKER_BUILD_COMMIT" ]] || die "running PARKER_PRODUCTION_COMMIT does not match git HEAD"

PARKER_LAN_IP=${PARKER_LAN_IP:-192.168.178.44}
printf 'Parker READY\nOwner UI: http://%s:8080\nAgent Gateway: http://%s:8090\nCommit: %s\n' "$PARKER_LAN_IP" "$PARKER_LAN_IP" "$PARKER_BUILD_COMMIT"
