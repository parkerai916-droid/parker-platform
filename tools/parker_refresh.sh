#!/usr/bin/env bash
set -euo pipefail

# Controlled production refresh helper. This script deliberately does not select
# or retrieve a commit: parker-deploy deploys exactly the clean checkout it is
# given here.
PARKER_DIR="/home/steve/parker-live"
PARKER_DEPLOY="/usr/local/sbin/parker-deploy"
PARKER_CONTAINER="parker-runtime"
CONSOLE_SERVICE="parker-hermes-console.service"
OWNER_PORT=8080
AGENT_PORT=8090
CONSOLE_URL="http://192.168.178.44:8088/"

ANALYSIS_KEY_SOURCE="/mnt/parker-secrets/parker/parker_hermes_analysis_ed25519"
ANALYSIS_KEY_TARGET="/home/steve/.ssh/parker_hermes_analysis_ed25519"
STT_KEY_SOURCE="/mnt/parker-secrets/parker/parker_hermes_stt_ed25519"
STT_KEY_TARGET="/home/steve/.ssh/parker_hermes_stt_ed25519"
KNOWN_HOSTS_SOURCE="/mnt/parker-secrets/parker/parker_hermes_analysis_known_hosts"
KNOWN_HOSTS_TARGET="/home/steve/.ssh/parker_hermes_analysis_known_hosts"

fail() {
    echo "PARKER REFRESH FAILED: $1" >&2
    exit 1
}

[[ "${EUID}" -eq 0 ]] || fail "must run as root (use sudo)"
[[ -d "$PARKER_DIR" ]] || fail "production checkout is missing: $PARKER_DIR"
[[ -x "$PARKER_DEPLOY" ]] || fail "authoritative deployment helper is unavailable: $PARKER_DEPLOY"

production_commit="$(/usr/bin/git -C "$PARKER_DIR" rev-parse --verify HEAD)" || fail "could not read production HEAD"
[[ "$production_commit" =~ ^[0-9a-f]{40}$ ]] || fail "production HEAD is not an exact commit"
if ! /usr/bin/git -C "$PARKER_DIR" diff --quiet ||
   ! /usr/bin/git -C "$PARKER_DIR" diff --cached --quiet; then
    fail "production checkout has tracked changes"
fi
echo "Production commit: $production_commit"

"$PARKER_DEPLOY" || fail "authoritative Parker deployment failed"

deployed_commit="$(/usr/bin/docker exec "$PARKER_CONTAINER" printenv PARKER_PRODUCTION_COMMIT 2>/dev/null)" ||
    fail "could not read deployed PARKER_PRODUCTION_COMMIT"
[[ "$deployed_commit" == "$production_commit" ]] ||
    fail "deployed commit does not match production HEAD"

running="$(/usr/bin/docker inspect --format '{{.State.Running}}' "$PARKER_CONTAINER" 2>/dev/null)" ||
    fail "Parker runtime container is unavailable"
[[ "$running" == "true" ]] || fail "Parker runtime container is not running"
container_id="$(/usr/bin/docker inspect --format '{{.Id}}' "$PARKER_CONTAINER" 2>/dev/null)" ||
    fail "could not read Parker runtime container ID"
[[ "$container_id" =~ ^[0-9a-f]{64}$ ]] || fail "Parker runtime container ID is invalid"

for port in "$OWNER_PORT" "$AGENT_PORT"; do
    /usr/bin/docker port "$PARKER_CONTAINER" "$port/tcp" 2>/dev/null | /usr/bin/grep -Fq ":$port" ||
        fail "Parker port $port is not published"
    /usr/bin/ss -ltnH | /usr/bin/awk -v suffix=":$port" '$4 ~ suffix "$" { found=1 } END { exit !found }' ||
        fail "Parker port $port is not listening"
done

recent_logs="$(/usr/bin/docker logs --since 180s "$PARKER_CONTAINER" 2>&1 || true)"
/usr/bin/grep -Fq "Runtime started" <<<"$recent_logs" ||
    fail "Parker startup success was not found in recent logs"

steve_uid="$(/usr/bin/id -u steve)" || fail "Steve user is unavailable"
as_steve_systemctl() {
    /usr/bin/runuser -u steve -- env \
        XDG_RUNTIME_DIR="/run/user/$steve_uid" \
        DBUS_SESSION_BUS_ADDRESS="unix:path=/run/user/$steve_uid/bus" \
        /usr/bin/systemctl --user "$@"
}

as_steve_systemctl restart "$CONSOLE_SERVICE" || fail "Dual Ingestion service restart failed"
as_steve_systemctl is-active --quiet "$CONSOLE_SERVICE" || fail "Dual Ingestion service is not active"

console_status="$(/usr/bin/curl --fail --silent --show-error --max-time 15 -o /dev/null -w '%{http_code}' "$CONSOLE_URL")" ||
    fail "Dual Ingestion GET health check failed"
[[ "$console_status" == "200" ]] || fail "Dual Ingestion GET returned HTTP $console_status"

require_read_only_mount() {
    local source="$1"
    local destination="$2"
    local actual
    actual="$(/usr/bin/docker inspect --format "{{range .Mounts}}{{if eq .Destination \"$destination\"}}{{.Source}}|{{.RW}}{{end}}{{end}}" "$PARKER_CONTAINER" 2>/dev/null)" ||
        fail "could not inspect mount $destination"
    [[ "$actual" == "$source|false" ]] || fail "mount mismatch or writable mount: $destination"
}

require_read_only_mount "$ANALYSIS_KEY_SOURCE" "$ANALYSIS_KEY_TARGET"
require_read_only_mount "$STT_KEY_SOURCE" "$STT_KEY_TARGET"
require_read_only_mount "$KNOWN_HOSTS_SOURCE" "$KNOWN_HOSTS_TARGET"

stt_result="$(/usr/bin/docker exec "$PARKER_CONTAINER" /bin/sh -c '
    printf "{}\n" | /usr/bin/ssh -T \
      -i /home/steve/.ssh/parker_hermes_stt_ed25519 \
      -o IdentitiesOnly=yes \
      -o BatchMode=yes \
      -o StrictHostKeyChecking=yes \
      -o UserKnownHostsFile=/home/steve/.ssh/parker_hermes_analysis_known_hosts \
      steve@192.168.178.45
' 2>/dev/null)" || fail "Parker to Hermes STT transport failed"
[[ "$stt_result" == '{"status":"INVALID_REQUEST"}' ]] ||
    fail "Parker to Hermes STT transport returned an unexpected result"

echo "Production commit: $production_commit"
echo "Parker runtime: healthy ($PARKER_CONTAINER, $container_id)"
echo "Dual Ingestion: healthy"
echo "Hermes mounts: verified read-only"
echo "STT transport: verified"
echo "PARKER REFRESH PASSED"
