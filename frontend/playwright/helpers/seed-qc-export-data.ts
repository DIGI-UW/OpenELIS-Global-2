/**
 * QC inspector-export E2E seed helper (OGC-706).
 *
 * The export reads QC runs, violations, statistics and per-test TEa for an
 * instrument over a date window. `seedQcLot` supplies the analyzer, lot,
 * statistics and TEa, and clears everything hanging off the analyzer on
 * `restore`, so this file only adds the three runs (one flagged) and the rule
 * violation the export is asserted on.
 */
import { SCHEMA, psql } from "./db-container";
import { QcLotSeed, seedQcLot } from "./seed-qc-lot";

const IDENTITY = {
  analyzerId: 990902,
  // Distinct from the sigma analyzer's name: the CSV assertions count rows by it.
  analyzerName: "PW Export Analyzer",
  lotId: "pw-exp-lot",
  statId: "pw-exp-stat",
  // The sigma seed borrows the lowest test row; take the next one so the two
  // seeds never rewrite the same row's TEa.
  testRowOffset: 1,
};
const RESULT_IDS = ["pw-exp-r1", "pw-exp-r2", "pw-exp-r3"];
const VIOLATION_ID = "pw-exp-v1";
const START_DATE = "2026-06-01";
const END_DATE = "2026-06-30";

export interface ExportSeed extends QcLotSeed {
  /** Window covering the seeded runs. */
  startDate: string;
  endDate: string;
}

export function seedExportData(): ExportSeed {
  const lot = seedQcLot(IDENTITY, { tea: 10, mean: 100, sd: 2 });

  psql(`
    INSERT INTO ${SCHEMA}.qc_result
      (id, control_lot_id, test_id, instrument_id, result_value, unit_of_measure,
       z_score, run_date_time, result_status, non_conformity_flag, sys_user_id, last_updated)
      VALUES
      ('${RESULT_IDS[0]}', '${lot.lotId}', ${lot.testId}, ${IDENTITY.analyzerId}, 100.0, 'mg/dL',
        0.0, '2026-06-15 09:00:00', 'ACCEPTED', false, 1, now()),
      ('${RESULT_IDS[1]}', '${lot.lotId}', ${lot.testId}, ${IDENTITY.analyzerId}, 108.0, 'mg/dL',
        4.0, '2026-06-16 09:00:00', 'ACCEPTED', true, 1, now()),
      ('${RESULT_IDS[2]}', '${lot.lotId}', ${lot.testId}, ${IDENTITY.analyzerId}, 96.0, 'mg/dL',
        -2.0, '2026-06-17 09:00:00', 'ACCEPTED', false, 1, now());
    INSERT INTO ${SCHEMA}.qc_rule_violation
      (id, triggering_result_id, rule_code, violation_date_time, severity,
       instrument_id, test_id, resolution_status, sys_user_id, last_updated)
      VALUES ('${VIOLATION_ID}', '${RESULT_IDS[1]}', '1_3S', '2026-06-16 09:00:00',
        'REJECTION', ${IDENTITY.analyzerId}, ${lot.testId}, 'UNRESOLVED', 1, now());
  `);

  // seedQcLot's restore already clears this analyzer's violations and runs.
  return { ...lot, startDate: START_DATE, endDate: END_DATE };
}
