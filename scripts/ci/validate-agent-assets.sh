#!/usr/bin/env bash
# The same command/skill compilation checks run locally and in GitHub validation.
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/../.."
python3 scripts/install-agent-skills.py -y all

commands=(speckit.analyze speckit.checklist speckit.clarify speckit.constitution
  speckit.implement speckit.plan speckit.specify speckit.tasks speckit.taskstoissues
  plan-record-playwright write-playwright-test debug-playwright audit-playwright
  write-backend-test)
for agent in cursor claude; do
  for command in "${commands[@]}"; do
    test -f ".$agent/commands/$command.md" || {
      echo "Missing compiled command: .$agent/commands/$command.md" >&2; exit 1;
    }
  done
  for skill in playwright backend-integration-testing; do
    test -f ".$agent/skills/$skill/SKILL.md" || {
      echo "Missing compiled skill: .$agent/skills/$skill/SKILL.md" >&2; exit 1;
    }
  done
done
if grep -r '\.specify/\.specify/' .cursor/commands/ .claude/commands/; then
  echo "Invalid rewritten command paths" >&2; exit 1
fi
if grep -rE '(^|[^.])templates/' .cursor/commands/ .claude/commands/ |
  grep -Ev '\.specify/templates/|\.specify/oe/skills/.*/templates/|\.ai/skills/.*/templates/' | grep -q .; then
  echo "Invalid bare template paths" >&2; exit 1
fi
echo 'Agent command compilation, required outputs and paths passed'
