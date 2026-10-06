#!/usr/bin/env bash
# Internal executor for e2e-cypress-deprecated.yml's matrix groups.
set -euo pipefail
source "$(dirname "$0")/e2e-stack.sh"
start_e2e_stack cypress
export E2E_FAIL_FAST=false CYPRESS_FAIL_FAST_ENABLED=false
SPECS="$(python3 - "$1" <<'PY'
from pathlib import Path
import sys, yaml
shard = sys.argv[1]
workflow = yaml.safe_load(Path('../.github/workflows/e2e-cypress-deprecated.yml').read_text())
entries = workflow['jobs']['e2e-cypress']['strategy']['matrix']['include']
assigned, selected = set(), []
for entry in entries:
    specs = [spec.strip() for spec in (entry.get('specs') or '').split(',') if spec.strip()]
    if entry['shard'] != 'independent':
        assigned.update(specs)
    if entry['shard'] == shard:
        selected = specs
if shard == 'independent':
    selected = sorted(str(path) for path in Path('cypress/e2e').rglob('*.cy.js') if str(path) not in assigned)
if not selected:
    sys.exit(f'No Cypress specs resolved for {shard}')
print(','.join(selected))
PY
)"
printf 'code\n' > "$OE_CI_PHASE_FILE"
npm run cy:spec -- "$SPECS"
