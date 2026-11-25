#!/usr/bin/env bash
# Internal executor for e2e-authoritative-reusable.yml's playwright-core lane.
set -euo pipefail
source "$(dirname "$0")/e2e-stack.sh"
start_e2e_stack core
printf 'code\n' > "$OE_CI_PHASE_FILE"
npm run pw:test -- --project=core-app --project=core-demo --shard="$1/4"
