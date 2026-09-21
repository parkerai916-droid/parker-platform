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
STARTUP_TIMEOUT_SECONDS=30
STARTUP_POLL_SECONDS=1
CONSOLE_STARTUP_TIMEOUT_SECONDS=60
CONSOLE_STARTUP_POLL_SECONDS=1

ANALYSIS_KEY_SOURCE="/mnt/parker-secrets/parker/parker_hermes_analysis_ed25519"
ANALYSIS_KEY_TARGET="/home/steve/.ssh/parker_hermes_analysis_ed25519"
STT_KEY_SOURCE="/mnt/parker-secrets/parker/parker_hermes_stt_ed25519"
STT_KEY_TARGET="/home/steve/.ssh/parker_hermes_stt_ed25519"
KNOWN_HOSTS_SOURCE="/mnt/parker-secrets/parker/parker_hermes_analysis_known_hosts"
KNOWN_HOSTS_TARGET="/home/steve/.ssh/parker_hermes_analysis_known_hosts"
HERMES_PROCESSING_V1_VERIFY="${HERMES_PROCESSING_V1_VERIFY:-false}"
HERMES_PROCESSING_KEY_SOURCE="/mnt/parker-secrets/parker/parker_hermes_processing_ed25519"
HERMES_PROCESSING_KEY_TARGET="/home/steve/.ssh/parker_hermes_processing_ed25519"
HERMES_PROCESSING_KNOWN_HOSTS_SOURCE="/mnt/parker-secrets/parker/parker_hermes_processing_known_hosts"
HERMES_PROCESSING_KNOWN_HOSTS_TARGET="/home/steve/.ssh/parker_hermes_processing_known_hosts"

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

# An already-current healthy container may retain startup logs older than the
# log window below, so capture that safe state before deployment.
pre_deploy_container_id="$(/usr/bin/docker inspect --format '{{.Id}}' "$PARKER_CONTAINER" 2>/dev/null || true)"
pre_deploy_commit="$(/usr/bin/docker inspect --format '{{range .Config.Env}}{{println .}}{{end}}' "$PARKER_CONTAINER" 2>/dev/null |
    /usr/bin/awk -F= '$1 == "PARKER_PRODUCTION_COMMIT" { print substr($0, index($0, "=") + 1) }' || true)"
pre_deploy_running="$(/usr/bin/docker inspect --format '{{.State.Running}}' "$PARKER_CONTAINER" 2>/dev/null || true)"
pre_deploy_healthy=false
if [[ -n "$pre_deploy_container_id" && "$pre_deploy_running" == "true" &&
      "$pre_deploy_commit" == "$production_commit" ]]; then
    pre_deploy_healthy=true
    for port in "$OWNER_PORT" "$AGENT_PORT"; do
        if ! /usr/bin/docker port "$PARKER_CONTAINER" "$port/tcp" 2>/dev/null | /usr/bin/grep -Fq ":$port" ||
           ! /usr/bin/ss -ltnH | /usr/bin/awk -v suffix=":$port" '$4 ~ suffix "$" { found=1 } END { exit !found }'; then
            pre_deploy_healthy=false
            break
        fi
    done
fi

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

if [[ "$pre_deploy_healthy" != true || "$container_id" != "$pre_deploy_container_id" ]]; then
    startup_deadline=$((SECONDS + STARTUP_TIMEOUT_SECONDS))
    startup_ready=false
    while (( SECONDS < startup_deadline )); do
        recent_logs="$(/usr/bin/docker logs --since 180s "$PARKER_CONTAINER" 2>&1 || true)"
        if /usr/bin/grep -Fq "Runtime started" <<<"$recent_logs" &&
           /usr/bin/grep -Fq "Owner LAN Evidence Upload HTTP server listening on 0.0.0.0:8080" <<<"$recent_logs" &&
           /usr/bin/grep -Fq "Agent Gateway HTTP server started on 0.0.0.0:8090" <<<"$recent_logs"; then
            startup_ready=true
            break
        fi
        /usr/bin/sleep "$STARTUP_POLL_SECONDS"
    done
    [[ "$startup_ready" == true ]] ||
        fail "Parker startup readiness was not observed within ${STARTUP_TIMEOUT_SECONDS}s"
fi

RUNUSER_BIN="$(command -v runuser || true)"
[[ -n "$RUNUSER_BIN" ]] || fail "runuser command not found"

steve_uid="$(/usr/bin/id -u steve)" || fail "Steve user is unavailable"
as_steve_systemctl() {
    "$RUNUSER_BIN" -u steve -- env \
        XDG_RUNTIME_DIR="/run/user/$steve_uid" \
        DBUS_SESSION_BUS_ADDRESS="unix:path=/run/user/$steve_uid/bus" \
        /usr/bin/systemctl --user "$@"
}

as_steve_systemctl restart "$CONSOLE_SERVICE" || fail "Dual Ingestion service restart failed"
as_steve_systemctl is-active --quiet "$CONSOLE_SERVICE" || fail "Dual Ingestion service is not active"

console_deadline=$((SECONDS + CONSOLE_STARTUP_TIMEOUT_SECONDS))
console_ready=false
while (( SECONDS < console_deadline )); do
    console_status="$(
        /usr/bin/curl \
            --silent \
            --max-time 5 \
            -o /dev/null \
            -w '%{http_code}' \
            "$CONSOLE_URL" 2>/dev/null || true
    )"

    if [[ "$console_status" == "200" ]]; then
        console_ready=true
        break
    fi

    /usr/bin/sleep "$CONSOLE_STARTUP_POLL_SECONDS"
done

[[ "$console_ready" == true ]] ||
    fail "Dual Ingestion console was not ready within ${CONSOLE_STARTUP_TIMEOUT_SECONDS}s"

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

set +e
stt_result="$(/usr/bin/docker exec "$PARKER_CONTAINER" /bin/sh -c '
    printf "{}\n" | /usr/bin/ssh -T \
      -i /home/steve/.ssh/parker_hermes_stt_ed25519 \
      -o IdentitiesOnly=yes \
      -o BatchMode=yes \
      -o StrictHostKeyChecking=yes \
      -o UserKnownHostsFile=/home/steve/.ssh/parker_hermes_analysis_known_hosts \
      steve@192.168.178.45
' 2>/dev/null)"
stt_exit=$?
set -e
[[ "$stt_exit" -eq 1 ]] ||
    fail "Parker to Hermes STT transport returned unexpected exit status $stt_exit"
[[ "$stt_result" == '{"status":"INVALID_REQUEST"}' ]] ||
    fail "Parker to Hermes STT transport returned an unexpected result"

if [[ "$HERMES_PROCESSING_V1_VERIFY" == true ]]; then
    require_read_only_mount "$HERMES_PROCESSING_KEY_SOURCE" "$HERMES_PROCESSING_KEY_TARGET"
    require_read_only_mount "$HERMES_PROCESSING_KNOWN_HOSTS_SOURCE" "$HERMES_PROCESSING_KNOWN_HOSTS_TARGET"
    # Readiness crosses the same production boundary as processing. The frame
    # is the deterministic Unit 2 harmless request used by the reviewed
    # Hermes entrypoint; it has no source bytes and cannot admit evidence.
    readiness_frame='{"protocolVersion":"1","requestId":"readiness-request","jobId":"readiness-job","occurrenceId":"readiness-occurrence","batchId":"readiness-batch","source":{"reference":"readiness-source","sha256":"e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855","sizeBytes":0,"originalFilename":"readiness.txt","mediaType":"text/plain"}}'
    processing_readiness_file="$(/usr/bin/docker exec "$PARKER_CONTAINER" /bin/sh -c 'mktemp /tmp/hermes-processing-v1-readiness.XXXXXX')" ||
        fail "could not allocate bounded Hermes v1 readiness output"
    set +e
    /usr/bin/docker exec -i "$PARKER_CONTAINER" /bin/sh -c '
        output="$1"
        trap "rm -f -- \"$output\"" EXIT
        frame="$2"
        printf "\\000\\000\\001\\121%s" "$frame" | /usr/bin/ssh -T \
          -i /home/steve/.ssh/parker_hermes_processing_ed25519 \
          -o IdentitiesOnly=yes \
          -o BatchMode=yes \
          -o StrictHostKeyChecking=yes \
          -o UserKnownHostsFile=/home/steve/.ssh/parker_hermes_processing_known_hosts \
          -o ConnectTimeout=10 \
          -o ServerAliveInterval=5 \
          -o ServerAliveCountMax=2 \
          steve@192.168.178.45 >"$output" 2>/dev/null
        status=$?
        [[ "$status" -eq 0 ]] || exit "$status"
        bytes="$(/usr/bin/wc -c <"$output")"
        [[ "$bytes" -ge 5 && "$bytes" -le 8388612 ]] || exit 70
        [[ "$(/usr/bin/od -An -tx1 -N4 "$output" | tr -d " \\n")" == "00000151" ]] || exit 70
    ' sh "$processing_readiness_file" "$readiness_frame"
    processing_readiness_exit=$?
    set -e
    [[ "$processing_readiness_exit" -eq 0 ]] ||
        fail "Hermes Processing Service v1 SSH readiness failed"
    echo "Hermes Processing Service v1: readiness verified"
else
    echo "Hermes Processing Service v1: verification disabled (routing remains unchanged)"
fi

echo "Production commit: $production_commit"
echo "Parker runtime: healthy ($PARKER_CONTAINER, $container_id)"
echo "Dual Ingestion: healthy"
echo "Hermes mounts: verified read-only"
echo "STT transport: verified"
echo "PARKER REFRESH PASSED"
