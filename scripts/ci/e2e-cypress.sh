#!/usr/bin/env bash
# Internal executor for e2e-cypress-deprecated.yml's shards: core runs the
# workflow's CORE_SPECS list, independent runs every other spec.
set -euo pipefail
source "$(dirname "$0")/e2e-stack.sh"
start_e2e_stack cypress
export E2E_FAIL_FAST=false CYPRESS_FAIL_FAST_ENABLED=false
SPECS="$(python3 - "$1" <<'PY'
from pathlib import Path
import sys, yaml
shard = sys.argv[1]
workflow = yaml.safe_load(Path('../.github/workflows/e2e-cypress-deprecated.yml').read_text())
core = [spec.strip() for spec in workflow['jobs']['e2e-cypress']['env']['CORE_SPECS'].split(',') if spec.strip()]
if shard == 'core':
    selected = [spec for spec in core if Path(spec).is_file()]
elif shard == 'independent':
    selected = sorted(str(path) for path in Path('cypress/e2e').rglob('*.cy.js') if str(path) not in core)
else:
    sys.exit(f'Unknown Cypress shard {shard}')
if not selected:
    sys.exit(f'No Cypress specs resolved for {shard}')
print(','.join(selected))
PY
)"
printf 'code\n' > "$OE_CI_PHASE_FILE"
npm run cy:spec -- "$SPECS"
