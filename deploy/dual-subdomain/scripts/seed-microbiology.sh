#!/usr/bin/env bash
set -euo pipefail
# =============================================================================
# Seed a reviewable microbiology demo case on the amr stack.
#
# Reuses the EXACT SQL from the PR's own test fixture
# (frontend/playwright/helpers/seed-microbiology-data.ts — seedMicrobiologyWorklistCase,
# which chains seedMicrobiologyCase -> seedMicrobiologyMvpCase -> sibling TB case).
# Run directly via psql rather than through the Playwright/Node wrapper: the
# helper itself only shells out to `docker exec ... psql`, so routing it through
# a full Node + Playwright + browser toolchain on the box would be pure overhead
# for a one-time data seed with no UI interaction needed.
#
# Deliberately leaves both cases at stage=RECEIVED with no isolates/AST readings
# — a reviewer exercising the case-detail workflow (isolate creation, manual AST,
# override, review) should drive those steps themselves, not find a pre-completed
# case. If frontend/node_modules + Playwright browsers are ever installed on the
# box, the full spec (frontend: npm run pw:test:core-demo -- ogc-782-microbiology-mvp)
# is the more complete alternative — it also drives the UI end-to-end.
#
# Idempotent-ish: safe to re-run — each run creates a new, uniquely-suffixed
# case rather than erroring on conflict (no cleanup between runs; call
# cleanupMicrobiologyMvpCase's SQL by hand — see the helper file — to remove one).
#
# Env: DB_CONTAINER (default amr-openelisglobal-database), BASE_URL (for the
#      printed review links only; default https://amr.openelis-global.org)
# =============================================================================

DB="${DB_CONTAINER:-amr-openelisglobal-database}"
BASE_URL="${BASE_URL:-https://amr.openelis-global.org}"
SUFFIX=$(date +%s%N | md5sum | cut -c1-9)
CASE_ID="oe-micro-case-${SUFFIX}"
ACT_ID="oe-micro-act-${SUFFIX}"
ACCESSION="OEMICRO${SUFFIX}"
STANDARD_ID="oe-micro-standard-${SUFFIX}"
ABX_ID="oe-micro-abx-${SUFFIX}"
PANEL_ID="oe-micro-panel-${SUFFIX}"
PANEL_ABX_ID="oe-micro-panel-abx-${SUFFIX}"
RULE_ID="oe-micro-rule-${SUFFIX}"
TB_CASE_ID="oe-micro-tb-${SUFFIX}"
TB_ACT_ID="oe-micro-tb-act-${SUFFIX}"

psql() { docker exec -i "$DB" psql -U clinlims -d clinlims -At -c "$1"; }

echo ">> seeding base bacteriology case + sample (DB_CONTAINER=$DB)"
RESULT=$(psql "
WITH method_row AS (
  SELECT id AS method_id FROM clinlims.method ORDER BY id LIMIT 1
), sample_row AS (
  INSERT INTO clinlims.sample (id, accession_number, entered_date, received_date, lastupdated, sys_user_id)
  SELECT nextval('clinlims.sample_seq'), '${ACCESSION}', CURRENT_DATE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1
  RETURNING id
), sample_item_row AS (
  INSERT INTO clinlims.sample_item (id, samp_id, sort_order, status_id, lastupdated)
  SELECT nextval('clinlims.sample_item_seq'), sample_row.id, 1, 1, CURRENT_TIMESTAMP FROM sample_row
  RETURNING id, samp_id
), case_row AS (
  INSERT INTO clinlims.micro_case (id, sample_item_id, workflow_type, stage, priority, culture_method_id, created_at, created_by, final_release_state, lastupdated, last_updated)
  SELECT '${CASE_ID}', sample_item_row.id, 'BACTERIOLOGY', 'RECEIVED', 'ROUTINE', method_row.method_id, CURRENT_TIMESTAMP, '1', 'NOT_READY', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
  FROM sample_item_row CROSS JOIN method_row
  RETURNING id, sample_item_id
), activity_row AS (
  INSERT INTO clinlims.micro_case_activity (id, case_id, activity_type, occurred_at, performed_by, note, lastupdated, last_updated)
  SELECT '${ACT_ID}', case_row.id, 'CASE_CREATED', CURRENT_TIMESTAMP, '1', 'Case created', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM case_row
)
SELECT case_row.id || '|' || case_row.sample_item_id || '|' || sample_item_row.samp_id
FROM case_row JOIN sample_item_row ON case_row.sample_item_id = sample_item_row.id;
")
CREATED_CASE_ID=$(echo "$RESULT" | cut -d'|' -f1)
SAMPLE_ITEM_ID=$(echo "$RESULT" | cut -d'|' -f2)
SAMPLE_ID=$(echo "$RESULT" | cut -d'|' -f3)
echo "   case=$CREATED_CASE_ID sampleItem=$SAMPLE_ITEM_ID sample=$SAMPLE_ID"

echo ">> seeding AST reference data (breakpoint standard, antibiotic, panel)"
psql "
WITH standard_row AS (
  INSERT INTO clinlims.micro_breakpoint_standard (id, authority, version, effective_date, is_active, lastupdated, last_updated)
  VALUES ('${STANDARD_ID}', 'CLSI', '2026', CURRENT_DATE, 'Y', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
  ON CONFLICT (authority, version) DO UPDATE SET is_active = 'Y', lastupdated = CURRENT_TIMESTAMP, last_updated = CURRENT_TIMESTAMP
  RETURNING id
), antibiotic_row AS (
  INSERT INTO clinlims.micro_antibiotic (id, display_name, whonet_code, antibiotic_class, is_active, lastupdated, last_updated)
  VALUES ('${ABX_ID}', 'Ciprofloxacin ${SUFFIX}', 'CIP${SUFFIX}', 'Fluoroquinolone', 'Y', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
  RETURNING id
), panel_row AS (
  INSERT INTO clinlims.micro_ast_panel (id, name, workflow_type, organism_group, is_active, lastupdated, last_updated)
  VALUES ('${PANEL_ID}', 'Gram negative AST panel ${SUFFIX}', 'BACTERIOLOGY', 'GRAM_NEGATIVE', 'Y', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
  RETURNING id
), panel_antibiotic_row AS (
  INSERT INTO clinlims.micro_ast_panel_antibiotic (id, panel_id, antibiotic_id, display_order, lastupdated, last_updated)
  SELECT '${PANEL_ABX_ID}', panel_row.id, antibiotic_row.id, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM panel_row CROSS JOIN antibiotic_row
), rule_row AS (
  INSERT INTO clinlims.micro_breakpoint_rule (id, standard_id, antibiotic_id, method, breakpoint_type, susceptible_value, intermediate_lower_value, intermediate_upper_value, resistant_value, is_active, lastupdated, last_updated)
  SELECT '${RULE_ID}', standard_row.id, antibiotic_row.id, 'MIC', 'MIC', 8, 16, 16, 32, 'Y', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
  FROM standard_row CROSS JOIN antibiotic_row
)
SELECT 1;
" >/dev/null
echo "   standard=$STANDARD_ID antibiotic=$ABX_ID panel=$PANEL_ID rule=$RULE_ID"

echo ">> seeding sibling TB case (same specimen, worklist demonstrates 2 workflow types)"
psql "
WITH method_row AS (
  SELECT id AS method_id FROM clinlims.method ORDER BY id LIMIT 1
), sibling_case AS (
  INSERT INTO clinlims.micro_case (id, sample_item_id, workflow_type, stage, priority, culture_method_id, created_at, created_by, final_release_state, lastupdated, last_updated)
  SELECT '${TB_CASE_ID}', '${SAMPLE_ITEM_ID}', 'MYCOBACTERIOLOGY_TB', 'RECEIVED', 'ROUTINE', method_row.method_id, CURRENT_TIMESTAMP, '1', 'NOT_READY', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
  FROM method_row
  RETURNING id
), activity_row AS (
  INSERT INTO clinlims.micro_case_activity (id, case_id, activity_type, occurred_at, performed_by, note, lastupdated, last_updated)
  SELECT '${TB_ACT_ID}', sibling_case.id, 'CASE_CREATED', CURRENT_TIMESTAMP, '1', 'Sibling TB case created', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM sibling_case
)
SELECT 1;
" >/dev/null
echo "   tbCase=$TB_CASE_ID"

echo
echo "=== SEEDED ==="
echo "primary case:  $CREATED_CASE_ID  -> $BASE_URL/MicrobiologyCaseView/$CREATED_CASE_ID"
echo "sibling case:  $TB_CASE_ID  -> $BASE_URL/MicrobiologyCaseView/$TB_CASE_ID"
echo "worklist:      $BASE_URL/MicrobiologyWorklist"
echo "MICRO_SEED_DONE"
