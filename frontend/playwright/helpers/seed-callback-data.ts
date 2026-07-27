/**
 * Critical Callback E2E seed helper (C.4 / OGC-714, OGC-715).
 *
 * The callback flow needs a test with configured critical bounds, and
 * ResultLimit has NO REST create/update path — bounds are settable only in
 * the DB. So this seeds via `docker exec psql`, the same pattern as
 * `seed-qc-sigma-data.ts`: borrow the target test's existing result_limits
 * rows (or insert a sentinel default row when none exist) and restore on
 * teardown.
 */
import { execFileSync } from "child_process";
import { resolveDbContainer } from "./db-container";

const SCHEMA = "clinlims";
const SENTINEL_LIMIT_ID = 990914; // high range to avoid fixture collisions

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

function asNumber(value: number, label: string): number {
  if (!Number.isFinite(value)) {
    throw new Error(`Expected finite number for ${label}, got: ${value}`);
  }
  return value;
}

export interface CriticalBandSeed {
  /** Put the borrowed rows' original critical bounds back (or drop the sentinel). */
  restore: () => void;
}

/**
 * Give `testId` a low/high critical band so a saved value at/beyond a bound
 * is critical (outside-band rule, D1). Updates every existing result_limits
 * row for the test (any demographic variant) or inserts one default row.
 */
export function seedCriticalBand(
  testId: number,
  low: number,
  high: number,
): CriticalBandSeed {
  asNumber(testId, "testId");
  asNumber(low, "low");
  asNumber(high, "high");

  const existing = psql(
    `SELECT id || '|' || COALESCE(low_critical::text, 'NULL') || '|' ||` +
      ` COALESCE(high_critical::text, 'NULL')` +
      ` FROM ${SCHEMA}.result_limits WHERE test_id = ${testId}`,
  );

  if (existing) {
    const originals = existing.split("\n").map((line) => {
      const [id, lo, hi] = line.split("|");
      if (!/^\d+$/.test(id)) {
        throw new Error(`Unexpected result_limits id: ${JSON.stringify(id)}`);
      }
      return { id, lo, hi };
    });
    psql(
      `UPDATE ${SCHEMA}.result_limits SET low_critical = ${low},` +
        ` high_critical = ${high} WHERE test_id = ${testId}`,
    );
    return {
      restore: () => {
        for (const o of originals) {
          const lo = o.lo === "NULL" ? "NULL" : `'${o.lo}'`;
          const hi = o.hi === "NULL" ? "NULL" : `'${o.hi}'`;
          psql(
            `UPDATE ${SCHEMA}.result_limits SET low_critical = ${lo},` +
              ` high_critical = ${hi} WHERE id = ${o.id}`,
          );
        }
      },
    };
  }

  psql(
    `INSERT INTO ${SCHEMA}.result_limits (id, test_id, test_result_type_id,` +
      ` min_age, max_age, low_critical, high_critical, lastupdated)` +
      ` VALUES (${SENTINEL_LIMIT_ID}, ${testId}, 4, 0, 'Infinity', ${low}, ${high}, NOW())`,
  );
  return {
    restore: () => {
      psql(
        `DELETE FROM ${SCHEMA}.result_limits WHERE id = ${SENTINEL_LIMIT_ID}`,
      );
    },
  };
}
