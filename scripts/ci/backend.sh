#!/usr/bin/env bash
# Internal executor for backend.yml and deployment-checks.yml.
set -euo pipefail
cd "$(dirname "$0")/../.."
case "$1" in
  backend-format)
    printf 'code\n' > "$OE_CI_PHASE_FILE"
    scripts/run-java21 mvn spotless:check
    ;;
  backend-tests)
    printf 'code\n' > "$OE_CI_PHASE_FILE"
    (cd dataexport && ../scripts/run-java21 mvn clean install)
    scripts/run-java21 mvn clean install -Dspotless.check.skip=true
    ;;
  deployment-contract)
    printf 'code\n' > "$OE_CI_PHASE_FILE"
    "$OE_CI_NODE22_BIN" --test .github/scripts/publish-checkpoints.test.cjs
    python3 -m unittest discover -s .github/scripts -p 'test_*.py' -v
    ;;
  agent-assets)
    printf 'code\n' > "$OE_CI_PHASE_FILE"
    python3 scripts/install-agent-skills.py -y all
    for command in speckit.analyze speckit.checklist speckit.clarify speckit.constitution \
        speckit.implement speckit.plan speckit.specify speckit.tasks speckit.taskstoissues \
        plan-record-playwright write-playwright-test debug-playwright audit-playwright write-backend-test; do
      test -f ".cursor/commands/$command.md"
      test -f ".claude/commands/$command.md"
    done
    for target in .cursor/skills/playwright/SKILL.md .claude/skills/playwright/SKILL.md \
        .cursor/skills/backend-integration-testing/SKILL.md .claude/skills/backend-integration-testing/SKILL.md; do
      test -f "$target"
    done
    if grep -r '\.specify/\.specify/' .cursor/commands/ .claude/commands/; then
      echo 'Invalid doubled .specify paths in compiled commands' >&2; exit 1
    fi
    if grep -E '(^|[^.])templates/' .cursor/commands/ .claude/commands/ | \
        grep -Ev '\.specify/templates/|\.ai/skills/.*/templates/' | grep -q .; then
      echo 'Invalid bare template paths in compiled commands' >&2; exit 1
    fi
    ;;
  *) echo "Unknown backend job: $1" >&2; exit 2 ;;
esac
