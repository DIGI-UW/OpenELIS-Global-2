#!/bin/bash
# verify-strict-013-fixtures.sh - Assert strict 013 analyzer fixtures are loaded and linked.

set -euo pipefail

COMPOSE_FILES=(-f docker-compose.dev.yml -f docker-compose.analyzer-test.yml)

echo "Checking strict 013 analyzer fixtures in database..."

strict_count="$(docker compose "${COMPOSE_FILES[@]}" exec -T db psql -U clinlims -d clinlims -t -A -c "
SELECT COUNT(*) FROM analyzer
WHERE (id, name) IN (
  (2007, 'Mindray BC-5380'),
  (2008, 'Mindray BS-360E'),
  (2009, 'Mindray BS-200'),
  (2010, 'Mindray BS-300'),
  (2012, 'Mindray BC2000')
);
")"

linked_count="$(docker compose "${COMPOSE_FILES[@]}" exec -T db psql -U clinlims -d clinlims -t -A -c "
SELECT COUNT(*)
FROM analyzer a
JOIN analyzer_type t ON t.id = a.analyzer_type_id
WHERE a.id IN (2007, 2008, 2009, 2010, 2012)
  AND t.name = 'Generic HL7';
")"

strict_count="$(echo "$strict_count" | tr -d '[:space:]')"
linked_count="$(echo "$linked_count" | tr -d '[:space:]')"

if [[ "$strict_count" != "5" ]]; then
  echo "ERROR: Expected 5 HL7 analyzers (2007, 2008, 2009, 2010, 2012); found $strict_count."
  echo "Run fixture loading first (source of truth XML + linking SQL)."
  exit 1
fi

if [[ "$linked_count" != "5" ]]; then
  echo "ERROR: Expected 5 HL7 analyzers linked to 'Generic HL7'; found $linked_count."
  echo "Run fixture loading/type-linking before HL7 bridge validation."
  exit 1
fi

echo "Fixture check passed: all 5 HL7 analyzers present and linked to Generic HL7."
