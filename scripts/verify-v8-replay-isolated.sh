#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 3 ]]; then
  echo "usage: $0 <A|B|C> <invocation-id> <result-root>" >&2
  exit 64
fi

fixture=$1
invocation_id=$2
result_root=$3
[[ "$fixture" =~ ^[ABC]$ ]] || { echo "invalid fixture" >&2; exit 64; }
[[ "$invocation_id" =~ ^[A-Za-z0-9_.-]{1,160}$ ]] || { echo "invalid invocation id" >&2; exit 64; }

repo_root=$(cd "$(dirname "$0")/.." && pwd -P)
result_path="$result_root/$fixture/result.json"
lock_path="$repo_root/build/oi11r1-v8-replay-verification.lock"
mkdir -p "$(dirname "$lock_path")"
exec 9>"$lock_path"
flock -n 9 || { echo "isolated V8 replay verification already active" >&2; exit 75; }
[[ ! -e "$result_path" ]] || { echo "fixture-specific result already exists" >&2; exit 73; }

cd "$repo_root"
OI10R5_ENABLED=true \
OI10R5_ACTION=REPLAY \
OI10R5_FIXTURE="$fixture" \
OI10R7_CONTINUATION=true \
OI10R5_IMPLEMENTATION_COMMIT=18b81a6d7834cb19e1ad884dbcc40a22289af288 \
OI10R5_ACCEPTANCE_ROOT=/home/steve/parker-acceptance/oi10r6-v8-canonical-freeze \
OI10R7_HISTORICAL_ROOT=/home/steve/parker-acceptance/oi10r5-v8-authoritative \
OI11R1_RESULT_PATH="$result_path" \
OI11R1_REPLAY_INVOCATION_ID="$invocation_id" \
./gradlew test --tests parker.core.runtime.OrdinaryRequestRegionV8AcceptanceHarnessTest \
  --rerun-tasks --no-daemon --max-workers=1 --console=plain

[[ -f "$result_path" ]] || { echo "fixture-specific replay result missing" >&2; exit 74; }
sha256sum "$result_path"
