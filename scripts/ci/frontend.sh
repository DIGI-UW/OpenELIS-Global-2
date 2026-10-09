#!/usr/bin/env bash
# Internal executor for frontend.yml's local checks and existing i18n checks.
set -euo pipefail
cd "$(dirname "$0")/../../frontend"
case "$1" in
  frontend-static)
    npm ci --legacy-peer-deps
    printf 'code\n' > "$OE_CI_PHASE_FILE"
    npx prettier ./ --check
    npm run lint
    npm run lint:all || echo 'Advisory src lint failed; GitHub treats it as non-blocking.'
    npm run pw:guard
    CI=true npm test
    ;;
  frontend-image)
    printf 'code\n' > "$OE_CI_PHASE_FILE"
    docker build -f Dockerfile -t "${OE_CI_IMAGE_PREFIX}-frontend" .
    ;;
  i18n)
    printf 'code\n' > "$OE_CI_PHASE_FILE"
    python3 - <<'PY'
import glob, json, os, pathlib, subprocess, sys
errors = []
for name in sorted(glob.glob('src/languages/*.json')):
    pairs = json.loads(pathlib.Path(name).read_text(), object_pairs_hook=lambda value: value)
    seen = set()
    duplicates = [key for key, _ in pairs if key in seen or seen.add(key)]
    if duplicates:
        errors.append(f'{name}: duplicate keys: {duplicates[:5]}')
if os.environ['OE_CI_SOURCE_BRANCH'] != 'chore/update-transifex':
    changed = subprocess.check_output(['git', 'diff', '--name-only',
        os.environ['OE_CI_BASE_SHA'] + '...HEAD', '--', ':/frontend/src/languages/'], text=True).splitlines()
    non_english = [name for name in changed if name.endswith('.json') and not name.endswith('/en.json')]
    if non_english:
        errors.append('Non-English locale changes: ' + ', '.join(non_english))
if errors:
    sys.exit('\n'.join(errors))
print('i18n duplicate-key and source-of-truth checks passed')
PY
    python3 "$(git rev-parse --show-toplevel)/scripts/ci/check-i18n-plurals.py" src/languages/en.json
    ;;
  *) echo "Unknown frontend job: $1" >&2; exit 2 ;;
esac
