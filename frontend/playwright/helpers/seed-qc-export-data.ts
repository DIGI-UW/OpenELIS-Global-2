/**
 * QC inspector-export E2E seed helper (OGC-706).
 *
 * The export reads QC runs, violations, statistics and per-test TEa for an
 * instrument over a date window. None of qc_result / qc_statistics / TEa has a
 * lightweight REST create-path (they come from the ~20-run analyzer pipeline),
 * so — like seed-qc-sigma-data.ts — this seeds directly via `docker exec psql`:
 * a sentinel analyzer + control lot, a statistics row, three runs in a known
 * window (one flagged with a rule violation), and TEa on a borrowed test.
 * `restore()` removes everything and puts the borrowed test's TEa back.
 */
import { execFileSync } from "child_process";
import { resolveDbContainer } from "./db-container";

const SCHEMA = "clinlims";
const ANALYZER_ID = 990902; // sentinel, distinct from the sigma seed's 990901
const LOT_ID = "pw-exp-lot";
const STAT_ID = "pw-exp-stat";
const RESULT_IDS = ["pw-exp-r1", "pw-exp-r2", "pw-exp-r3"];
const VIOLATION_ID = "pw-exp-v1";
const START_DATE = "2026-06-01";
const END_DATE = "2026-06-30";

export interface ExportSeed {
  /** Sentinel analyzer id — the export's required instrumentId. */
  analyzerId: string;
  /** Control lot id. */
  lotId: string;
  /** Borrowed test id whose TEa is set to 10 (restored on teardown). */
  testId: string;
  /** Window covering the seeded runs. */
  startDate: string;
  endDate: string;
  /** Remove all seeded rows and restore the borrowed test's original TEa. */
  restore: () => void;
}

function psql(sql: string): string {
  const container = resolveDbContainer();
  return execFileSync(
    "docker",
    [
      "exec",
      "-i",
      container,
      "psql",
      "-U",
      "clinlims",
      "-d",
      "clinlims",
      "-tAc",
      sql,
    ],
    { encoding: "utf8" },
  ).trim();
}

function asInt(value: string, label: string): string {
  if (!/^\d+$/.test(value)) {
    throw new Error(
      `Expected integer for ${label}, got: ${JSON.stringify(value)}`,
    );
  }
  return value;
}

export function seedExportData(): ExportSeed {
  const testId = asInt(
    psql(`SELECT id FROM ${SCHEMA}.test ORDER BY id LIMIT 1`),
    "testId",
  );
  const originalTea = psql(
    `SELECT COALESCE(tea::text, '') FROM ${SCHEMA}.test WHERE id = ${testId}`,
  );

  psql(`
    INSERT INTO ${SCHEMA}.analyzer (id, name, is_active)
      VALUES (${ANALYZER_ID}, 'PW Export Analyzer', true)
      ON CONFLICT (id) DO NOTHING;
    DELETE FROM ${SCHEMA}.qc_rule_violation WHERE id = '${VIOLATION_ID}';
    DELETE FROM ${SCHEMA}.qc_result WHERE control_lot_id = '${LOT_ID}';
    DELETE FROM ${SCHEMA}.qc_statistics WHERE control_lot_id = '${LOT_ID}';
    DELETE FROM ${SCHEMA}.qc_control_lot WHERE id = '${LOT_ID}';
    INSERT INTO ${SCHEMA}.qc_control_lot
      (id, product_name, lot_number, control_level, test_id, instrument_id,
       calculation_method, status, expiration_date, sys_user_id, last_updated)
      VALUES ('${LOT_ID}', 'PW Export Control', 'PW-EXP-1', 'NORMAL',
        ${testId}, ${ANALYZER_ID}, 'INITIAL_RUNS', 'ACTIVE',
        now() + interval '1 year', 1, now());
    INSERT INTO ${SCHEMA}.qc_statistics
      (id, control_lot_id, calculation_date, mean, standard_deviation,
       num_values, calculation_method, validity_start, sys_user_id, last_updated)
      VALUES ('${STAT_ID}', '${LOT_ID}', now(), 100, 2, 25, 'INITIAL_RUNS', now(), 1, now());
    INSERT INTO ${SCHEMA}.qc_result
      (id, control_lot_id, test_id, instrument_id, result_value, unit_of_measure,
       z_score, run_date_time, result_status, non_conformity_flag, sys_user_id, last_updated)
      VALUES
      ('${RESULT_IDS[0]}', '${LOT_ID}', ${testId}, ${ANALYZER_ID}, 100.0, 'mg/dL',
        0.0, '2026-06-15 09:00:00', 'ACCEPTED', false, 1, now()),
      ('${RESULT_IDS[1]}', '${LOT_ID}', ${testId}, ${ANALYZER_ID}, 108.0, 'mg/dL',
        4.0, '2026-06-16 09:00:00', 'ACCEPTED', true, 1, now()),
      ('${RESULT_IDS[2]}', '${LOT_ID}', ${testId}, ${ANALYZER_ID}, 96.0, 'mg/dL',
        -2.0, '2026-06-17 09:00:00', 'ACCEPTED', false, 1, now());
    INSERT INTO ${SCHEMA}.qc_rule_violation
      (id, triggering_result_id, rule_code, violation_date_time, severity,
       instrument_id, test_id, resolution_status, sys_user_id, last_updated)
      VALUES ('${VIOLATION_ID}', '${RESULT_IDS[1]}', '1_3S', '2026-06-16 09:00:00',
        'REJECTION', ${ANALYZER_ID}, ${testId}, 'UNRESOLVED', 1, now());
    UPDATE ${SCHEMA}.test SET tea = 10 WHERE id = ${testId};
  `);

  return {
    analyzerId: String(ANALYZER_ID),
    lotId: LOT_ID,
    testId,
    startDate: START_DATE,
    endDate: END_DATE,
    restore: () => {
      psql(`
        DELETE FROM ${SCHEMA}.qc_rule_violation WHERE id = '${VIOLATION_ID}';
        DELETE FROM ${SCHEMA}.qc_result WHERE control_lot_id = '${LOT_ID}';
        DELETE FROM ${SCHEMA}.qc_statistics WHERE control_lot_id = '${LOT_ID}';
        DELETE FROM ${SCHEMA}.qc_control_lot WHERE id = '${LOT_ID}';
        DELETE FROM ${SCHEMA}.analyzer WHERE id = ${ANALYZER_ID};
        UPDATE ${SCHEMA}.test SET tea = ${originalTea === "" ? "NULL" : originalTea} WHERE id = ${testId};
      `);
    },
  };
}
