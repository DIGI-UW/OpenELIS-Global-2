import { execFileSync } from "child_process";
import { resolveDbContainer } from "./db-container";

interface SeededMicrobiologyCase {
  caseId: string;
  sampleItemId: string;
  sampleId: string;
}

interface SeededMicrobiologyMvpCase extends SeededMicrobiologyCase {
  antibioticId: string;
  breakpointRuleId: string;
  panelAntibioticId: string;
  panelId: string;
  siblingCaseId?: string;
}

function pgLiteral(value: string): string {
  return `'${value.replace(/'/g, "''")}'`;
}

function psql(sql: string): string {
  return execFileSync(
    "docker",
    [
      "exec",
      "-i",
      resolveDbContainer(),
      "psql",
      "-U",
      "clinlims",
      "-d",
      "clinlims",
      "-At",
      "-c",
      sql,
    ],
    { encoding: "utf8" },
  ).trim();
}

export function seedMicrobiologyCase(): SeededMicrobiologyCase {
  const suffix = Date.now().toString(36);
  const caseId = `pw-micro-case-${suffix}`;
  const activityId = `pw-micro-act-${suffix}`;
  const accession = `PWMICRO${suffix}`.slice(0, 24);
  const sql = `
WITH method_row AS (
  SELECT id AS method_id FROM clinlims.method ORDER BY id LIMIT 1
), sample_row AS (
  INSERT INTO clinlims.sample
    (id, accession_number, entered_date, received_date, lastupdated, sys_user_id)
  SELECT nextval('clinlims.sample_seq'), ${pgLiteral(accession)}, CURRENT_DATE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1
  RETURNING id
), sample_item_row AS (
  INSERT INTO clinlims.sample_item (id, samp_id, sort_order, status_id, lastupdated)
  SELECT nextval('clinlims.sample_item_seq'), sample_row.id, 1, 1, CURRENT_TIMESTAMP
  FROM sample_row
  RETURNING id, samp_id
), case_row AS (
  INSERT INTO clinlims.micro_case
    (id, sample_item_id, workflow_type, stage, priority, culture_method_id, created_at, created_by, final_release_state, lastupdated, last_updated)
  SELECT ${pgLiteral(caseId)}, sample_item_row.id, 'BACTERIOLOGY', 'RECEIVED', 'ROUTINE',
    method_row.method_id, CURRENT_TIMESTAMP, '1', 'NOT_READY', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
  FROM sample_item_row CROSS JOIN method_row
  RETURNING id, sample_item_id
), activity_row AS (
  INSERT INTO clinlims.micro_case_activity
    (id, case_id, activity_type, occurred_at, performed_by, note, lastupdated, last_updated)
  SELECT ${pgLiteral(activityId)}, case_row.id, 'CASE_CREATED', CURRENT_TIMESTAMP, '1', 'Case created', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
  FROM case_row
)
SELECT case_row.id || '|' || case_row.sample_item_id || '|' || sample_item_row.samp_id
FROM case_row JOIN sample_item_row ON case_row.sample_item_id = sample_item_row.id;
`;
  const [createdCaseId, sampleItemId, sampleId] = psql(sql).split("|");
  return { caseId: createdCaseId, sampleItemId, sampleId };
}

export function seedMicrobiologyMvpCase(): SeededMicrobiologyMvpCase {
  const seeded = seedMicrobiologyCase();
  const suffix = Date.now().toString(36);
  const antibioticId = `pw-micro-abx-${suffix}`;
  const breakpointRuleId = `pw-micro-rule-${suffix}`;
  const panelAntibioticId = `pw-micro-panel-abx-${suffix}`;
  const panelId = `pw-micro-panel-${suffix}`;
  const standardId = `pw-micro-standard-${suffix}`;
  const sql = `
WITH standard_row AS (
  INSERT INTO clinlims.micro_breakpoint_standard
    (id, authority, version, effective_date, is_active, lastupdated, last_updated)
  VALUES (${pgLiteral(standardId)}, 'CLSI', '2026', CURRENT_DATE, 'Y', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
  ON CONFLICT (authority, version) DO UPDATE SET is_active = 'Y', lastupdated = CURRENT_TIMESTAMP, last_updated = CURRENT_TIMESTAMP
  RETURNING id
), antibiotic_row AS (
  INSERT INTO clinlims.micro_antibiotic
    (id, display_name, whonet_code, antibiotic_class, is_active, lastupdated, last_updated)
  VALUES (${pgLiteral(antibioticId)}, ${pgLiteral(`Ciprofloxacin ${suffix}`)}, ${pgLiteral(`CIP${suffix}`)}, 'Fluoroquinolone', 'Y', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
  RETURNING id
), panel_row AS (
  INSERT INTO clinlims.micro_ast_panel
    (id, name, workflow_type, organism_group, is_active, lastupdated, last_updated)
  VALUES (${pgLiteral(panelId)}, ${pgLiteral(`Gram negative AST panel ${suffix}`)}, 'BACTERIOLOGY', 'GRAM_NEGATIVE', 'Y', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
  RETURNING id
), panel_antibiotic_row AS (
  INSERT INTO clinlims.micro_ast_panel_antibiotic
    (id, panel_id, antibiotic_id, display_order, lastupdated, last_updated)
  SELECT ${pgLiteral(panelAntibioticId)}, panel_row.id, antibiotic_row.id, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
  FROM panel_row CROSS JOIN antibiotic_row
), rule_row AS (
  INSERT INTO clinlims.micro_breakpoint_rule
    (id, standard_id, antibiotic_id, method, breakpoint_type, susceptible_value,
     intermediate_lower_value, intermediate_upper_value, resistant_value, is_active, lastupdated, last_updated)
  SELECT ${pgLiteral(breakpointRuleId)}, standard_row.id, antibiotic_row.id, 'MIC', 'MIC',
    8, 16, 16, 32, 'Y', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
  FROM standard_row CROSS JOIN antibiotic_row
)
SELECT ${pgLiteral(antibioticId)} || '|' || ${pgLiteral(breakpointRuleId)} || '|' || ${pgLiteral(panelAntibioticId)} || '|' || ${pgLiteral(panelId)};
`;
  const [
    createdAntibioticId,
    createdBreakpointRuleId,
    createdPanelAntibioticId,
    createdPanelId,
  ] = psql(sql).split("|");
  return {
    ...seeded,
    antibioticId: createdAntibioticId,
    breakpointRuleId: createdBreakpointRuleId,
    panelAntibioticId: createdPanelAntibioticId,
    panelId: createdPanelId,
  };
}

export function seedMicrobiologyWorklistCase(): SeededMicrobiologyMvpCase {
  const seeded = seedMicrobiologyMvpCase();
  const suffix = Date.now().toString(36);
  const siblingCaseId = `pw-micro-tb-${suffix}`;
  const sql = `
WITH method_row AS (
  SELECT id AS method_id FROM clinlims.method ORDER BY id LIMIT 1
), sibling_case AS (
  INSERT INTO clinlims.micro_case
    (id, sample_item_id, workflow_type, stage, priority, culture_method_id, created_at, created_by, final_release_state, lastupdated, last_updated)
  SELECT ${pgLiteral(siblingCaseId)}, ${pgLiteral(seeded.sampleItemId)}, 'MYCOBACTERIOLOGY_TB', 'RECEIVED', 'ROUTINE',
    method_row.method_id, CURRENT_TIMESTAMP, '1', 'NOT_READY', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
  FROM method_row
  RETURNING id
), activity_row AS (
  INSERT INTO clinlims.micro_case_activity
    (id, case_id, activity_type, occurred_at, performed_by, note, lastupdated, last_updated)
  SELECT ${pgLiteral(`pw-micro-tb-act-${suffix}`)}, sibling_case.id, 'CASE_CREATED', CURRENT_TIMESTAMP, '1',
    'Sibling TB case created', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
  FROM sibling_case
)
SELECT id FROM sibling_case;
`;
  const createdSiblingCaseId = psql(sql);
  return { ...seeded, siblingCaseId: createdSiblingCaseId };
}

export function cleanupMicrobiologyCase(seeded: SeededMicrobiologyCase): void {
  const sql = `
DELETE FROM clinlims.micro_ast_reading WHERE ast_run_id IN (
  SELECT id FROM clinlims.micro_ast_run WHERE isolate_id IN (
    SELECT i.id FROM clinlims.micro_isolate i
    JOIN clinlims.micro_case c ON i.case_id = c.id
    WHERE c.sample_item_id = ${pgLiteral(seeded.sampleItemId)}
  )
);
DELETE FROM clinlims.micro_ast_run WHERE isolate_id IN (
  SELECT i.id FROM clinlims.micro_isolate i
  JOIN clinlims.micro_case c ON i.case_id = c.id
  WHERE c.sample_item_id = ${pgLiteral(seeded.sampleItemId)}
);
DELETE FROM clinlims.micro_critical_communication WHERE case_id IN (
  SELECT id FROM clinlims.micro_case WHERE sample_item_id = ${pgLiteral(seeded.sampleItemId)}
);
DELETE FROM clinlims.micro_isolate WHERE case_id IN (
  SELECT id FROM clinlims.micro_case WHERE sample_item_id = ${pgLiteral(seeded.sampleItemId)}
);
DELETE FROM clinlims.micro_case_activity WHERE case_id IN (
  SELECT id FROM clinlims.micro_case WHERE sample_item_id = ${pgLiteral(seeded.sampleItemId)}
);
DELETE FROM clinlims.micro_case WHERE sample_item_id = ${pgLiteral(seeded.sampleItemId)};
DELETE FROM clinlims.sample_item WHERE id = ${pgLiteral(seeded.sampleItemId)};
DELETE FROM clinlims.sample WHERE id = ${pgLiteral(seeded.sampleId)};
`;
  try {
    psql(sql);
  } catch (error) {
    console.warn(`Microbiology cleanup failed: ${error}`);
  }
}

export function cleanupMicrobiologyMvpCase(
  seeded: SeededMicrobiologyMvpCase,
): void {
  cleanupMicrobiologyCase(seeded);
  const sql = `
DELETE FROM clinlims.micro_breakpoint_rule WHERE id = ${pgLiteral(seeded.breakpointRuleId)};
DELETE FROM clinlims.micro_ast_panel_antibiotic WHERE id = ${pgLiteral(seeded.panelAntibioticId)};
DELETE FROM clinlims.micro_ast_panel WHERE id = ${pgLiteral(seeded.panelId)};
DELETE FROM clinlims.micro_antibiotic WHERE id = ${pgLiteral(seeded.antibioticId)};
`;
  try {
    psql(sql);
  } catch (error) {
    console.warn(`Microbiology reference cleanup failed: ${error}`);
  }
}
