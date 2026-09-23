#!/usr/bin/env bash
set -euo pipefail
tool_root=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd -P)
"$tool_root/gradlew" --quiet installDist
exec /usr/bin/java -cp "$tool_root/build/install/parker/lib/*" parker.composition.OwnerPinAdminToolKt "$@"
