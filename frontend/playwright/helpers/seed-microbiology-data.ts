import { execFileSync } from "child_process";
import { resolveDbContainer } from "./db-container";

interface SeededMicrobiologyCase {
  caseId: string;
  sampleItemId: string;
  sampleId: string;
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

export function cleanupMicrobiologyCase(seeded: SeededMicrobiologyCase): void {
  const sql = `
DELETE FROM clinlims.micro_isolate WHERE case_id = ${pgLiteral(seeded.caseId)};
DELETE FROM clinlims.micro_case_activity WHERE case_id = ${pgLiteral(seeded.caseId)};
DELETE FROM clinlims.micro_case WHERE id = ${pgLiteral(seeded.caseId)};
DELETE FROM clinlims.sample_item WHERE id = ${pgLiteral(seeded.sampleItemId)};
DELETE FROM clinlims.sample WHERE id = ${pgLiteral(seeded.sampleId)};
`;
  try {
    psql(sql);
  } catch (error) {
    console.warn(`Microbiology cleanup failed: ${error}`);
  }
}
