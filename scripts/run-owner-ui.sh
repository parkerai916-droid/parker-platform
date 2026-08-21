#!/usr/bin/env bash
#
# run-owner-ui.sh -- Launches Parker's existing real-runtime owner UI
# (:ui-desktop:runOwnerUi) with the known-good bare-JVM configuration this
# repository's own owner-facing UI acceptance already proved correct:
#
#   docs/reviews/REASONING_KNOWLEDGE_SOURCE_BOUNDED_SEMANTIC_RELEVANCE_COMPLETION_REVIEW.md
#
# This script does not add a UI, does not touch Memory Core, Knowledge Item,
# QMD, or Evidence architecture, and does not change any production
# persistence path. It only wraps the already-supported
# `sg parker-store-writers -c './gradlew :ui-desktop:runOwnerUi ...'`
# invocation with the environment variables that invocation already requires,
# plus fail-fast checks so a missing prerequisite produces a short, readable
# message instead of a Kotlin/Gradle stack trace.
#
# Usage: scripts/run-owner-ui.sh
# (run from the saved PuTTY X11 session, with VcXsrv already running)

set -euo pipefail

# --- Locate the repository root from this script's own location, never ---
# --- from the caller's current working directory. ------------------------
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." >/dev/null 2>&1 && pwd)"

fail() {
    echo "run-owner-ui.sh: $*" >&2
    exit 1
}

warn() {
    echo "run-owner-ui.sh: warning: $*" >&2
}

# --- Known-good production stores and QMD prerequisites (owner-facing UI ---
# --- acceptance record, section 9). Overridable via an already-exported ---
# --- environment, never silently altered by this script. ------------------
MEMORY_CORE_DIR_DEFAULT="/mnt/parker-data/parker/memory-core"
KNOWLEDGE_ITEMS_DIR_DEFAULT="/mnt/parker-data/parker/knowledge-items"
EVIDENCE_DIR_DEFAULT="/mnt/parker-data/parker/evidence"
QMD_SOURCE_ROOT_DEFAULT="/home/steve/qmd"
QMD_TSX_CLI_PATH_DEFAULT="/home/steve/qmd/node_modules/tsx/dist/cli.mjs"

# --- Pre-flight checks -----------------------------------------------------
# Read-only throughout: no durability store, evidence store, or QMD path is
# ever written to by this script.

if [ -z "${DISPLAY:-}" ]; then
    fail "DISPLAY is not set. This UI must be launched from the saved PuTTY X11 session (\"Parker X11\") with VcXsrv already running on Windows -- not a plain SSH session without X11 forwarding."
fi

[ -d "${MEMORY_CORE_DIR_DEFAULT}" ] || fail "Production Memory Core store not found: ${MEMORY_CORE_DIR_DEFAULT}"
[ -d "${KNOWLEDGE_ITEMS_DIR_DEFAULT}" ] || fail "Production Knowledge Item store not found: ${KNOWLEDGE_ITEMS_DIR_DEFAULT}"
[ -d "${EVIDENCE_DIR_DEFAULT}" ] || fail "Production evidence store not found: ${EVIDENCE_DIR_DEFAULT}"

[ -d "${QMD_SOURCE_ROOT_DEFAULT}" ] || fail "Local QMD checkout not found: ${QMD_SOURCE_ROOT_DEFAULT} -- required for Bounded Relevance Computation (see the owner-facing UI acceptance record, section 9)."
[ -f "${QMD_TSX_CLI_PATH_DEFAULT}" ] || fail "tsx CLI entry point not found: ${QMD_TSX_CLI_PATH_DEFAULT} -- required to launch the .mts QMD relevance bridge."

command -v sg >/dev/null 2>&1 || fail "'sg' is not available on this system; cannot activate the parker-store-writers group."
if ! id -nG "$(id -un)" | tr ' ' '\n' | grep -qx "parker-store-writers"; then
    fail "User '$(id -un)' is not a member of the parker-store-writers group. Ask an administrator to run: sudo usermod -aG parker-store-writers $(id -un) -- then start a fresh login session before retrying."
fi

[ -x "${REPO_ROOT}/gradlew" ] || fail "Gradle wrapper not found or not executable: ${REPO_ROOT}/gradlew"

# Optional, non-fatal: confirms Ollama is reachable without invoking any
# model -- /api/tags only lists installed models, it never generates.
if command -v curl >/dev/null 2>&1; then
    if ! curl -fsS --max-time 3 "http://localhost:11434/api/tags" >/dev/null 2>&1; then
        warn "Ollama does not appear to be reachable at http://localhost:11434/api/tags. Parker will still start, but reasoning calls will fail until Ollama is running."
    fi
fi

# --- Known-good runtime configuration --------------------------------------
# Every value below is exactly what the owner-facing UI acceptance record
# proved correct. An already-exported value is respected, never overridden --
# in particular, PARKER_MODEL_TIMEOUT_MS never silently reverts to a shorter
# default than the accepted 120000.
export PARKER_MODEL_ENDPOINT_URL="${PARKER_MODEL_ENDPOINT_URL:-http://localhost:11434/api/generate}"
export PARKER_MODEL_NAME="${PARKER_MODEL_NAME:-qwen2.5-coder:7b}"
export PARKER_MODEL_TIMEOUT_MS="${PARKER_MODEL_TIMEOUT_MS:-120000}"
export PARKER_OWNER_PRINCIPAL_ID="${PARKER_OWNER_PRINCIPAL_ID:-user.steve}"
export PARKER_OWNER_DISPLAY_NAME="${PARKER_OWNER_DISPLAY_NAME:-Owner}"
export PARKER_LOCAL_TEXT_CHANNEL_MODULE_ID="${PARKER_LOCAL_TEXT_CHANNEL_MODULE_ID:-channel.local-text}"
export PARKER_EVIDENCE_STORAGE_ROOT="${PARKER_EVIDENCE_STORAGE_ROOT:-${EVIDENCE_DIR_DEFAULT}}"
export PARKER_EVIDENCE_DELETION_AUDIT_LOG_PATH="${PARKER_EVIDENCE_DELETION_AUDIT_LOG_PATH:-/mnt/parker-data/parker/evidence-audit/deletion-audit.log}"
export PARKER_MEMORY_CORE_DURABILITY_LOG_PATH="${PARKER_MEMORY_CORE_DURABILITY_LOG_PATH:-${MEMORY_CORE_DIR_DEFAULT}/durability.log}"
export PARKER_KNOWLEDGE_ITEM_DURABILITY_LOG_PATH="${PARKER_KNOWLEDGE_ITEM_DURABILITY_LOG_PATH:-${KNOWLEDGE_ITEMS_DIR_DEFAULT}/durability.log}"
export PARKER_QMD_SOURCE_ROOT="${PARKER_QMD_SOURCE_ROOT:-${QMD_SOURCE_ROOT_DEFAULT}}"
export PARKER_QMD_TSX_CLI_PATH="${PARKER_QMD_TSX_CLI_PATH:-${QMD_TSX_CLI_PATH_DEFAULT}}"

# --- Launch ------------------------------------------------------------
# Always wrapped in `sg parker-store-writers`, exactly as the proven manual
# invocation was -- regardless of whether the calling shell's own session
# already has the group active, since a session predating group membership
# would otherwise fork a Gradle daemon lacking it (see the owner-facing UI
# acceptance record for the exact failure this avoids). The absolute
# gradlew path is used inside the sg-spawned shell so this does not depend
# on working-directory propagation through sg.
exec sg parker-store-writers -c "'${REPO_ROOT}/gradlew' :ui-desktop:runOwnerUi --console=plain"
