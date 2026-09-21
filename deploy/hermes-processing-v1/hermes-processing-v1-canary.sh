#!/usr/bin/env bash
set -euo pipefail

# Controlled-fixture support only. The caller must provide a synthetic fixture
# and an already-approved Parker-side request envelope. This script never
# performs Parker admission and refuses the known real-evidence identifier.
fixture="${1:?usage: hermes-processing-v1-canary.sh <synthetic-fixture> <request-id> <profile>}"
request_id="${2:?usage: hermes-processing-v1-canary.sh <synthetic-fixture> <request-id> <profile>}"
profile="${3:?usage: hermes-processing-v1-canary.sh <synthetic-fixture> <request-id> <profile>}"
[[ "$request_id" != JOB-20260920-033328 ]] || { echo "real evidence is forbidden" >&2; exit 64; }
[[ -f "$fixture" ]] || { echo "synthetic fixture is missing" >&2; exit 64; }
case "$fixture" in
    *JOB-*|*Michael*|*Uber*) echo "real-case fixture name is forbidden" >&2; exit 64 ;;
esac
case "$profile" in
    txt) [[ "$fixture" == *.txt ]] || { echo "txt profile requires a .txt fixture" >&2; exit 64; } ;;
    structured) [[ "$fixture" == *.doc || "$fixture" == *.docx || "$fixture" == *.rtf || "$fixture" == *.eml || "$fixture" == *.msg ]] || { echo "structured profile requires a supported document fixture" >&2; exit 64; } ;;
    spreadsheet) [[ "$fixture" == *.xls || "$fixture" == *.xlsx ]] || { echo "spreadsheet profile requires an XLS/XLSX fixture" >&2; exit 64; } ;;
    wrong-hash|malformed-frame|disabled-ocr|disabled-transcription) : ;;
    *) echo "unknown canary profile: $profile" >&2; exit 64 ;;
esac
echo "fixture accepted for controlled canary support: requestId=$request_id profile=$profile"
echo "execution requires the approved Parker transport harness; no evidence admission performed"
