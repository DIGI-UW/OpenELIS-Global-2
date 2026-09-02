import { execFileSync } from "node:child_process";
import { resolveDbContainer } from "./db-container";

/**
 * EQA lifecycle E2E seeding (participant smoke + provider cycle specs).
 *
 * Seeded directly via `docker exec psql` (same pattern as
 * seed-eqa-shipped-cycle.ts). Two seeders:
 *
 * - seedParticipantCycle: a scheme this lab is self-enrolled in plus a
 *   PLANNED cycle — the exact starting state of the participant lane. The
 *   spec then drives Add Order (panel receipt), results and Review & submit
 *   through the real UI/REST paths.
 * - seedProviderScheme: a scheme this lab provides with five Active
 *   participant enrollments and NO cycle — the wizard creates the cycle, so
 *   restore() sweeps by scheme id to catch everything the test run writes.
 *
 * Plus seedReportedResults: the >=5 reported eqa_result rows scoreCycle
 * demands (MIN_PARTICIPANTS_FOR_STATS) — the participant submissions
 * themselves are out of scope for the provider spec, so the score container
 * rows are planted the way an intake would write them. Values are clustered
 * so the z-score verdicts stay ACCEPTABLE and the spec never depends on
 * statistics behaviour, which the backend ITs own.
 */

const SCHEMA = "clinlims";

function psql(sql: string): string {
  const container = resolveDbContainer();
  // First line only: a RETURNING insert prints the value and then the
  // "INSERT 0 1" command tag, which -tA does not suppress.
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
  )
    .trim()
    .split("\n")[0]
    .trim();
}

/** Guard against anything but a safe token reaching an interpolated SQL string. */
function asSafeString(value: string, label: string): string {
  if (!/^[A-Za-z0-9 _-]+$/.test(value)) {
    throw new Error(
      `Expected a plain alphanumeric string for ${label}, got: ${JSON.stringify(value)}`,
    );
  }
  return value;
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

/** Ids must fit a signed 32-bit int: organization.id is read as int in places,
 * and one larger row kills displayListService — and with it the webapp boot
 * (learned live, 2026-08-26). Time-derived over ~27h so a crashed run's
 * leftovers cannot collide with the next run; a collision fails on the PK. */
function intSafeIdBase(): number {
  return 1900000000 + (Date.now() % 100000000);
}

/**
 * Undo stack for a seeder's own inserts. A seeder that throws half way
 * through must not leave rows behind: `beforeAll` never returns a seed, so
 * `afterAll` has nothing to restore and the orphans stay in the shared dev
 * database for good (seen live — two orphaned schemes from a failed insert).
 * Each insert pushes its own delete; the stack drains in reverse.
 */
function undoStack() {
  const undo: (() => void)[] = [];
  return {
    push: (statement: string) => undo.push(() => psql(statement)),
    drain: () => {
      while (undo.length > 0) {
        try {
          undo.pop()?.();
        } catch {
          // best effort: keep unwinding the rest of the stack
        }
      }
    },
  };
}

function insertProgram(
  name: string,
  provider: string,
  requiresCycleReview: boolean,
): string {
  return asInt(
    psql(
      `INSERT INTO ${SCHEMA}.eqa_program (id, fhir_uuid, name, is_active, sys_user_id, provider, scheme_type,` +
        ` requires_cycle_review) VALUES (nextval('${SCHEMA}.eqa_program_seq'), gen_random_uuid(),` +
        ` '${asSafeString(name, "scheme name")}', true, '1', '${asSafeString(provider, "provider")}',` +
        ` 'REGIONAL_PT', ${requiresCycleReview}) RETURNING id`,
    ),
    "scheme id",
  );
}

export interface ParticipantCycleSeed {
  programId: string;
  enrollmentId: string;
  cycleId: string;
  programName: string;
  cycleName: string;
  /** Remove every seeded row plus what the UI run writes (children first).
   * The patient sample/analysis graph the order creates is left in place,
   * matching existing fixture behaviour. */
  restore: () => void;
}

export function seedParticipantCycle(runTag: string): ParticipantCycleSeed {
  asSafeString(runTag, "runTag");
  const programName = `E2E ${runTag} participant scheme`;
  const cycleName = `E2E ${runTag} cycle`;
  const seeded = undoStack();

  let programId: string;
  let enrollmentId: string;
  let cycleId: string;
  try {
    // requires_cycle_review=true is what renders the "Review & submit" button
    // once the cycle reaches READY_TO_SUBMIT.
    programId = insertProgram(programName, "E2E External Provider", true);
    seeded.push(`DELETE FROM ${SCHEMA}.eqa_program WHERE id = ${programId}`);

    // Self-enrollments are deliberately NOT linked to eqa_program (the
    // eqa_program_id column was dropped by eqa-011): the row is free-text.
    enrollmentId = asInt(
      psql(
        `INSERT INTO ${SCHEMA}.eqa_lab_program_enrollment (id, provider, program_name, is_active, sys_user_id)` +
          ` VALUES (nextval('${SCHEMA}.eqa_lab_enroll_seq'), 'E2E External Provider',` +
          ` '${asSafeString(programName, "program name")}', true, '1') RETURNING id`,
      ),
      "enrollment id",
    );
    seeded.push(
      `DELETE FROM ${SCHEMA}.eqa_lab_program_enrollment WHERE id = ${enrollmentId}`,
    );

    cycleId = asInt(
      psql(
        `INSERT INTO ${SCHEMA}.eqa_cycle (id, fhir_uuid, scheme_id, cycle_number, cycle_name, status,` +
          ` planned_start_date, planned_end_date, created_by, sys_user_id)` +
          ` VALUES (nextval('${SCHEMA}.eqa_cycle_seq'), gen_random_uuid(), ${programId}, 1,` +
          ` '${asSafeString(cycleName, "cycle name")}', 'PLANNED', now(), now() + interval '7 days', 1, '1')` +
          ` RETURNING id`,
      ),
      "cycle id",
    );
    seeded.push(`DELETE FROM ${SCHEMA}.eqa_cycle WHERE id = ${cycleId}`);
  } catch (error) {
    seeded.drain();
    throw error;
  }

  return {
    programId,
    enrollmentId,
    cycleId,
    programName,
    cycleName,
    restore: () => {
      psql(
        `DELETE FROM ${SCHEMA}.eqa_participant_result WHERE cycle_id = ${cycleId}`,
      );
      psql(
        `DELETE FROM ${SCHEMA}.eqa_panel_receipt WHERE cycle_id = ${cycleId}`,
      );
      psql(`DELETE FROM ${SCHEMA}.sample_eqa WHERE cycle_id = ${cycleId}`);
      psql(
        `DELETE FROM ${SCHEMA}.eqa_cycle_state_transition WHERE cycle_id = ${cycleId}`,
      );
      psql(
        `DELETE FROM ${SCHEMA}.eqa_lab_enrollment_test_map WHERE enrollment_id = ${enrollmentId}`,
      );
      psql(
        `DELETE FROM ${SCHEMA}.eqa_lab_enrollment_lab_unit WHERE enrollment_id = ${enrollmentId}`,
      );
      // Rows the UI run wrote are gone; the seeded cycle, enrollment and
      // scheme unwind in reverse insert order.
      seeded.drain();
    },
  };
}

export interface ProviderSchemeSeed {
  programId: string;
  schemeName: string;
  organizationIds: string[];
  organizationNames: string[];
  /** Sweep everything hanging off the scheme — including the cycle, panel,
   * boxes, shipments, distribution and results the TEST RUN creates through
   * the wizard/workbench — then the seeded enrollments, program and orgs. */
  restore: () => void;
}

export const PROVIDER_PARTICIPANT_COUNT = 5;

export function seedProviderScheme(runTag: string): ProviderSchemeSeed {
  asSafeString(runTag, "runTag");
  const schemeName = `E2E ${runTag} provider scheme`;
  const base = intSafeIdBase();
  const organizationIds: string[] = [];
  const organizationNames: string[] = [];
  const seeded = undoStack();

  let programId = "";
  try {
    for (let i = 0; i < PROVIDER_PARTICIPANT_COUNT; i++) {
      const id = String(base + i);
      const name = `E2E ${runTag} Lab ${i + 1}`;
      psql(
        `INSERT INTO ${SCHEMA}.organization (id, name, mls_sentinel_lab_flag, is_active, lastupdated)` +
          ` VALUES (${asInt(id, "org id")}, '${asSafeString(name, "org name")}', 'N', 'Y', now())`,
      );
      seeded.push(`DELETE FROM ${SCHEMA}.organization WHERE id = ${id}`);
      organizationIds.push(id);
      organizationNames.push(name);
    }

    programId = insertProgram(schemeName, "This laboratory", false);
    seeded.push(`DELETE FROM ${SCHEMA}.eqa_program WHERE id = ${programId}`);

    for (const orgId of organizationIds) {
      psql(
        `INSERT INTO ${SCHEMA}.eqa_program_enrollment (id, eqa_program_id, organization_id, status, sys_user_id)` +
          ` VALUES (nextval('${SCHEMA}.eqa_enrollment_seq'), ${programId}, ${asInt(orgId, "enrollment org")},` +
          ` 'Active', '1')`,
      );
    }
    seeded.push(
      `DELETE FROM ${SCHEMA}.eqa_program_enrollment WHERE eqa_program_id = ${programId}`,
    );
  } catch (error) {
    seeded.drain();
    throw error;
  }

  const cycles = `SELECT id FROM ${SCHEMA}.eqa_cycle WHERE scheme_id = ${programId}`;
  const boxes = `SELECT id FROM ${SCHEMA}.shipping_box WHERE eqa_cycle_id IN (${cycles})`;
  const distributions = `SELECT id FROM ${SCHEMA}.eqa_distribution WHERE cycle_id IN (${cycles})`;

  return {
    programId,
    schemeName,
    organizationIds,
    organizationNames,
    restore: () => {
      psql(
        `DELETE FROM ${SCHEMA}.eqa_participant_followup WHERE scheme_id = ${programId}`,
      );
      psql(
        `DELETE FROM ${SCHEMA}.eqa_result WHERE eqa_distribution_id IN (${distributions})`,
      );
      psql(
        `DELETE FROM ${SCHEMA}.eqa_distribution WHERE cycle_id IN (${cycles})`,
      );
      psql(
        `DELETE FROM ${SCHEMA}.eqa_participant_result WHERE cycle_id IN (${cycles})`,
      );
      psql(
        `DELETE FROM ${SCHEMA}.eqa_panel_receipt WHERE cycle_id IN (${cycles})`,
      );
      psql(
        `DELETE FROM ${SCHEMA}.eqa_cycle_state_transition WHERE cycle_id IN (${cycles})`,
      );
      psql(
        `DELETE FROM ${SCHEMA}.shipment WHERE shipping_box_id IN (${boxes})`,
      );
      psql(
        `DELETE FROM ${SCHEMA}.box_sample_item WHERE shipping_box_id IN (${boxes})`,
      );
      psql(
        `DELETE FROM ${SCHEMA}.shipping_box WHERE eqa_cycle_id IN (${cycles})`,
      );
      psql(
        `DELETE FROM ${SCHEMA}.eqa_panel_sample WHERE panel_id IN` +
          ` (SELECT id FROM ${SCHEMA}.eqa_panel WHERE cycle_id IN (${cycles}))`,
      );
      psql(`DELETE FROM ${SCHEMA}.eqa_panel WHERE cycle_id IN (${cycles})`);
      psql(`DELETE FROM ${SCHEMA}.eqa_round WHERE cycle_id IN (${cycles})`);
      psql(
        `DELETE FROM ${SCHEMA}.eqa_cycle_participant WHERE cycle_id IN (${cycles})`,
      );
      psql(`DELETE FROM ${SCHEMA}.eqa_cycle WHERE scheme_id = ${programId}`);
      // Test-created rows are gone; the seeded enrollments, scheme and
      // organizations unwind in reverse insert order.
      seeded.drain();
    },
  };
}

/** Reported results planted per organization, so a spec can assert the score
 * cell ("{unacceptable} unacceptable of {RESULTS_PER_ORGANIZATION}"). */
export const RESULTS_PER_ORGANIZATION = 3;

/**
 * Plant the score container: one eqa_distribution on the cycle plus reported
 * eqa_result rows, three tests per organization. scoreCycle refuses below
 * MIN_PARTICIPANTS_FOR_STATS (5) reported rows, so five organizations give a
 * comfortable fifteen.
 *
 * The first organization's first result is planted far from the pack, which
 * makes it UNACCEPTABLE and every other row ACCEPTABLE. The arithmetic is
 * worth stating, because a smaller seed cannot reach the verdict at all:
 * statistics pool every result in the distribution (not per test), so with n
 * rows of which n-1 are identical the odd one out scores exactly
 * (n-1)/sqrt(n) whatever its magnitude — 3.61 at n=15, over the
 * unacceptable threshold of 3.0, where five rows would cap at 1.79 and never
 * leave ACCEPTABLE. One unacceptable participant is what makes scoring
 * enqueue a follow-up, which is the behaviour under test.
 *
 * Rows are swept by the scheme-scoped restore().
 */
export function seedReportedResults(
  cycleId: string,
  organizationIds: string[],
): void {
  asInt(cycleId, "cycle id");
  const schemeId = asInt(
    psql(`SELECT scheme_id FROM ${SCHEMA}.eqa_cycle WHERE id = ${cycleId}`),
    "scheme id",
  );
  // eqa_result.test_id references test(id) — the panel's own rows key on
  // analyte, not test — so borrow existing tests the way
  // seed-qc-sigma-data.ts does. Which tests they are does not matter: the
  // rows only have to satisfy the FK and the (distribution, org, test) key.
  const testIds = psql(
    `SELECT string_agg(id::text, ',') FROM (SELECT id FROM ${SCHEMA}.test ORDER BY id` +
      ` LIMIT ${RESULTS_PER_ORGANIZATION}) t`,
  )
    .split(",")
    .map((id) => asInt(id, "test id"));
  if (testIds.length < RESULTS_PER_ORGANIZATION) {
    throw new Error(
      `Need ${RESULTS_PER_ORGANIZATION} tests in the catalog, found ${testIds.length}`,
    );
  }

  const distributionId = asInt(
    psql(
      `INSERT INTO ${SCHEMA}.eqa_distribution (id, fhir_uuid, eqa_program_id, distribution_name,` +
        ` distribution_date, deadline, status, created_by, cycle_id, sys_user_id)` +
        ` VALUES (nextval('${SCHEMA}.eqa_distribution_seq'), gen_random_uuid(), ${schemeId},` +
        ` 'E2E score intake', now(), now(), 'SHIPPED', 1, ${cycleId}, '1') RETURNING id`,
    ),
    "distribution id",
  );
  organizationIds.forEach((orgId, orgIndex) => {
    testIds.forEach((testId, testIndex) => {
      const outlier = orgIndex === 0 && testIndex === 0;
      psql(
        `INSERT INTO ${SCHEMA}.eqa_result (id, fhir_uuid, eqa_distribution_id, participant_organization_id,` +
          ` test_id, result_value, target_value, submission_method, submission_date, sys_user_id)` +
          ` VALUES (nextval('${SCHEMA}.eqa_result_seq'), gen_random_uuid(), ${distributionId},` +
          ` ${asInt(orgId, "result org")}, ${testId}, ${outlier ? 200 : 100}, 100, 'MANUAL', now(), '1')`,
      );
    });
  });
}
