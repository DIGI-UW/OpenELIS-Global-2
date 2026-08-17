/**
 * QC Westgard sigma-metric E2E seed helper (C.1 / OGC-704, OGC-705).
 *
 * The sigma tile on ControlChartDetail reads
 * `GET /rest/qc/charts/{lot}/statistics`, which needs (a) a control lot,
 * (b) a computed `qc_statistics` row (mean/SD), and (c) a per-test `TEa`.
 * None of these have a lightweight REST create-path: statistics are produced
 * only by the ~20-run analyzer pipeline, and `TEa` has NO write endpoint at
 * all (settable only in the DB). So this seeds directly via `docker exec psql`
 * — the same pattern as `create-analyzer-from-profile.ts` — instead of driving
 * the whole analyzer harness.
 *
 * Fully self-contained: creates its own sentinel analyzer and control lot, and
 * borrows the lowest existing `test` row for the `TEa` field (restored on
 * teardown). `restore()` removes everything and puts the borrowed test back.
 */
import { execFileSync } from "child_process";
import { resolveDbContainer } from "./db-container";

const SCHEMA = "clinlims";
const ANALYZER_ID = 990901; // sentinel, high range to avoid fixture collisions
const LOT_ID = "pw-sigma-lot";
const STAT_ID = "pw-sigma-stat";

export interface SigmaSeed {
  /** Sentinel analyzer id — use in the /analyzers/qc/charts/:analyzerId URL. */
  analyzerId: string;
  /** Control lot id — use in the /rest/qc/charts/{lot}/statistics call. */
  lotId: string;
  /** Borrowed test id whose TEa is toggled per test case. */
  testId: string;
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

/** Guard against anything but a bare integer reaching an interpolated SQL id. */
function asInt(value: string, label: string): string {
  if (!/^\d+$/.test(value)) {
    throw new Error(
      `Expected integer for ${label}, got: ${JSON.stringify(value)}`,
    );
  }
  return value;
}

/** `null` → SQL NULL; a finite number → its literal. Rejects anything else. */
function asNumericLiteral(value: number | null): string {
  if (value === null) {
    return "NULL";
  }
  if (!Number.isFinite(value)) {
    throw new Error(`Expected finite number or null, got: ${value}`);
  }
  return String(value);
}

/**
 * Seed a control lot + statistics row + per-test TEa.
 *
 * @param opts.tea  total allowable error (percent); `null` exercises the
 *                  NOT_CALCULABLE path
 * @param opts.mean control mean (default 100)
 * @param opts.sd   control SD (default 2) → CV = sd/mean*100
 */
export function seedSigmaData(opts: {
  tea: number | null;
  mean?: number;
  sd?: number;
}): SigmaSeed {
  const mean = opts.mean ?? 100;
  const sd = opts.sd ?? 2;

  const testId = asInt(
    psql(`SELECT id FROM ${SCHEMA}.test ORDER BY id LIMIT 1`),
    "testId",
  );
  // Empty string means the column was NULL; remember it verbatim to restore.
  const originalTea = psql(
    `SELECT COALESCE(tea::text, '') FROM ${SCHEMA}.test WHERE id = ${testId}`,
  );

  psql(`
    INSERT INTO ${SCHEMA}.analyzer (id, name, is_active, last_updated)
      VALUES (${ANALYZER_ID}, 'PW Sigma Analyzer', true, now())
      ON CONFLICT (id) DO NOTHING;
    DELETE FROM ${SCHEMA}.qc_statistics WHERE control_lot_id = '${LOT_ID}';
    DELETE FROM ${SCHEMA}.qc_control_lot WHERE id = '${LOT_ID}';
    INSERT INTO ${SCHEMA}.qc_control_lot
      (id, product_name, lot_number, control_level, test_id, instrument_id,
       calculation_method, status, expiration_date, sys_user_id, last_updated)
      VALUES ('${LOT_ID}', 'PW Sigma Control', 'PW-LOT-1', 'LEVEL_1',
        ${testId}, ${ANALYZER_ID}, 'INITIAL_RUNS', 'ACTIVE',
        now() + interval '1 year', 1, now());
    INSERT INTO ${SCHEMA}.qc_statistics
      (id, control_lot_id, calculation_date, mean, standard_deviation,
       num_values, calculation_method, validity_start, sys_user_id, last_updated)
      VALUES ('${STAT_ID}', '${LOT_ID}', now(), ${asNumericLiteral(mean)},
        ${asNumericLiteral(sd)}, 25, 'INITIAL_RUNS', now(), 1, now());
    UPDATE ${SCHEMA}.test SET tea = ${asNumericLiteral(opts.tea)} WHERE id = ${testId};
  `);

  return {
    analyzerId: String(ANALYZER_ID),
    lotId: LOT_ID,
    testId,
    restore: () => {
      psql(`
        DELETE FROM ${SCHEMA}.qc_statistics WHERE control_lot_id = '${LOT_ID}';
        DELETE FROM ${SCHEMA}.qc_control_lot WHERE id = '${LOT_ID}';
        DELETE FROM ${SCHEMA}.analyzer WHERE id = ${ANALYZER_ID};
        UPDATE ${SCHEMA}.test SET tea = ${originalTea === "" ? "NULL" : originalTea} WHERE id = ${testId};
      `);
    },
  };
}
