/**
 * Shared QC seeding: a sentinel analyzer, one control lot, one computed
 * statistics row, and a per-test TEa.
 *
 * None of these has a lightweight REST create-path: statistics are produced
 * only by the ~20-run analyzer pipeline, and TEa has no write endpoint at all,
 * so they are written straight into Postgres via `docker exec psql`.
 *
 * Used by `seed-qc-sigma-data.ts` (OGC-704/705) and, with runs and a rule
 * violation layered on top, by `seed-qc-export-data.ts` (OGC-706).
 */
import { SCHEMA, asInt, psql } from "./db-container";

export interface QcLotIdentity {
  /** Sentinel analyzer id; keep in the high range to avoid fixture collisions. */
  analyzerId: number;
  /** Analyzer name, distinct per seed so exports can be attributed to it. */
  analyzerName: string;
  lotId: string;
  statId: string;
  /**
   * Which existing `test` row to borrow for TEa, counting up from the lowest
   * id. Two seeds that can be live at once MUST pick different rows: each
   * rewrites `test.tea` and restores whatever it saw at its own start, so
   * sharing a row makes the later restore clobber the earlier one.
   */
  testRowOffset: number;
}

export interface QcLotSeed {
  /** Sentinel analyzer id, as the REST layer spells it. */
  analyzerId: string;
  /** Control lot id. */
  lotId: string;
  /** The borrowed test row, whose TEa this seed owns until `restore`. */
  testId: string;
  /** Drop every seeded row and put the borrowed test's TEa back. */
  restore: () => void;
}

/** Everything hanging off one sentinel analyzer, in FK-safe order. */
function clearSql(identity: QcLotIdentity): string {
  return `
    DELETE FROM ${SCHEMA}.qc_rule_violation WHERE instrument_id = ${identity.analyzerId};
    DELETE FROM ${SCHEMA}.qc_result WHERE control_lot_id = '${identity.lotId}';
    DELETE FROM ${SCHEMA}.qc_statistics WHERE control_lot_id = '${identity.lotId}';
    DELETE FROM ${SCHEMA}.qc_control_lot WHERE id = '${identity.lotId}';
  `;
}

export function seedQcLot(
  identity: QcLotIdentity,
  stats: { tea: number; mean: number; sd: number },
): QcLotSeed {
  const testId = asInt(
    psql(
      `SELECT id FROM ${SCHEMA}.test ORDER BY id OFFSET ${identity.testRowOffset} LIMIT 1`,
    ),
    "testId",
  );
  // Empty string means the column was NULL; remember it verbatim to restore.
  const originalTea = psql(
    `SELECT COALESCE(tea::text, '') FROM ${SCHEMA}.test WHERE id = ${testId}`,
  );

  psql(`
    INSERT INTO ${SCHEMA}.analyzer (id, name, is_active, last_updated)
      VALUES (${identity.analyzerId}, '${identity.analyzerName}', true, now())
      ON CONFLICT (id) DO NOTHING;
    ${clearSql(identity)}
    INSERT INTO ${SCHEMA}.qc_control_lot
      (id, product_name, lot_number, control_level, test_id, instrument_id,
       calculation_method, status, expiration_date, sys_user_id, last_updated)
      VALUES ('${identity.lotId}', 'PW Control', '${identity.lotId}', 'LEVEL_1',
        ${testId}, ${identity.analyzerId}, 'INITIAL_RUNS', 'ACTIVE',
        now() + interval '1 year', 1, now());
    INSERT INTO ${SCHEMA}.qc_statistics
      (id, control_lot_id, calculation_date, mean, standard_deviation,
       num_values, calculation_method, validity_start, sys_user_id, last_updated)
      VALUES ('${identity.statId}', '${identity.lotId}', now(), ${stats.mean},
        ${stats.sd}, 25, 'INITIAL_RUNS', now(), 1, now());
    UPDATE ${SCHEMA}.test SET tea = ${stats.tea} WHERE id = ${testId};
  `);

  return {
    analyzerId: String(identity.analyzerId),
    lotId: identity.lotId,
    testId,
    restore: () => {
      psql(`
        ${clearSql(identity)}
        DELETE FROM ${SCHEMA}.analyzer WHERE id = ${identity.analyzerId};
        UPDATE ${SCHEMA}.test SET tea = ${originalTea === "" ? "NULL" : originalTea} WHERE id = ${testId};
      `);
    },
  };
}
