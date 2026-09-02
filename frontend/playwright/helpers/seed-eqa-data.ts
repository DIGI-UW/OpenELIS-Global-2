import { execFileSync } from "node:child_process";
import { resolveDbContainer } from "./db-container";

/**
 * EQA E2E seeding, shared by the EQA specs.
 *
 * Every seeder writes through `docker exec psql`, following the conventions
 * of seed-eqa-shipped-cycle.ts: guarded interpolation, organization ids that
 * fit a signed 32-bit int, and an undo stack so a seeder that fails half way
 * through unwinds instead of orphaning rows in a shared database.
 *
 * What each one sets up:
 *
 * - seedParticipantCycle: a scheme this lab is enrolled in plus a planned
 *   cycle — the starting state of the participant lane, which the spec then
 *   drives through Add Order, results and review.
 * - seedProviderScheme: a scheme this lab provides with five active
 *   participant enrollments and no cycle, since the wizard creates it. Its
 *   restore sweeps by scheme, so everything the run writes goes too.
 * - seedReportedResults: the score container, with one participant planted
 *   far enough from the pack to be unacceptable — which is what makes
 *   scoring enqueue a follow-up.
 * - seedFollowups: one follow-up about this lab and one about another, the
 *   whole of the split between the two registers.
 * - seedReceiptConditions: a dispatched cycle holding an overdue shipment
 *   and a damaged arrival, neither of which a healthy dispatch produces.
 * - seedOversightData: scored participant results and a competency event,
 *   which is what the three oversight dashboards read.
 * - seedInHousePanel: a distributed blinded panel, whose targets stay sealed.
 * - seedParticipantUser: a login without the provider grant, for the one
 *   spec that has to be someone other than an administrator.
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
  const undo: { statement: string; run: () => void }[] = [];
  return {
    push: (statement: string) =>
      undo.push({ statement, run: () => psql(statement) }),
    /** Unwind in reverse. Every entry is attempted even if an earlier one
     * fails, and the failures are returned rather than swallowed. */
    drain: (): string[] => {
      const failures: string[] = [];
      while (undo.length > 0) {
        const entry = undo.pop();
        try {
          entry?.run();
        } catch (error) {
          failures.push(`${entry?.statement}: ${String(error).slice(0, 200)}`);
        }
      }
      return failures;
    },
  };
}

/**
 * Run cleanup statements in the order given, attempting all of them, and
 * return whatever failed.
 *
 * One statement failing must not abort the rest — an unguarded sequence left
 * a panel receipt behind once, which then held a foreign key on its
 * enrollment and stranded the whole scheme. But a swallowed failure is just
 * as bad the other way: the run goes green while rows accumulate in a shared
 * database. So failures come back to the caller, which raises them once
 * cleanup has finished.
 */
function sweep(statements: string[]): string[] {
  const failures: string[] = [];
  for (const statement of statements) {
    try {
      psql(statement);
    } catch (error) {
      failures.push(`${statement}: ${String(error).slice(0, 200)}`);
    }
  }
  return failures;
}

/** Raise whatever cleanup could not delete, after every statement has run. */
function reportCleanupFailures(failures: string[]): void {
  if (failures.length > 0) {
    throw new Error(
      `Seed cleanup left ${failures.length} statement(s) unapplied — rows may be` +
        ` orphaned in a shared database:\n  ${failures.join("\n  ")}`,
    );
  }
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

/** Box codes must follow the dispatcher's own convention or the receipt
 * monitor will not match the box to its participant. */
function boxCode(cycleId: string, organizationId: string): string {
  return `EQA-C${cycleId}-${organizationId}`;
}

function insertOrganization(id: string, name: string): void {
  psql(
    `INSERT INTO ${SCHEMA}.organization (id, name, mls_sentinel_lab_flag, is_active, lastupdated)` +
      ` VALUES (${asInt(id, "org id")}, '${asSafeString(name, "org name")}', 'N', 'Y', now())`,
  );
}

function insertCycle(schemeId: string, name: string, status: string): string {
  return asInt(
    psql(
      `INSERT INTO ${SCHEMA}.eqa_cycle (id, fhir_uuid, scheme_id, cycle_number, cycle_name, status,` +
        ` created_by, sys_user_id) VALUES (nextval('${SCHEMA}.eqa_cycle_seq'), gen_random_uuid(),` +
        ` ${schemeId}, 1, '${asSafeString(name, "cycle name")}', '${asSafeString(status, "status")}', 1, '1')` +
        ` RETURNING id`,
    ),
    "cycle id",
  );
}

/** A self-enrollment row, needed wherever a receipt's NOT NULL
 * lab_enrollment_id has to point somewhere real. */
function insertSelfEnrollment(programName: string): string {
  return asInt(
    psql(
      `INSERT INTO ${SCHEMA}.eqa_lab_program_enrollment (id, provider, program_name, is_active, sys_user_id)` +
        ` VALUES (nextval('${SCHEMA}.eqa_lab_enroll_seq'), 'E2E External Provider',` +
        ` '${asSafeString(programName, "program name")}', true, '1') RETURNING id`,
    ),
    "enrollment id",
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
      const failures = sweep([
        `DELETE FROM ${SCHEMA}.eqa_participant_result WHERE cycle_id = ${cycleId}`,
        `DELETE FROM ${SCHEMA}.eqa_panel_receipt WHERE cycle_id = ${cycleId}`,
        `DELETE FROM ${SCHEMA}.eqa_panel_receipt WHERE lab_enrollment_id = ${enrollmentId}`,
        `DELETE FROM ${SCHEMA}.sample_eqa WHERE cycle_id = ${cycleId}`,
        `DELETE FROM ${SCHEMA}.sample_eqa WHERE eqa_enrollment_id = ${enrollmentId}`,
        `DELETE FROM ${SCHEMA}.eqa_cycle_state_transition WHERE cycle_id = ${cycleId}`,
        `DELETE FROM ${SCHEMA}.eqa_lab_enrollment_test_map WHERE enrollment_id = ${enrollmentId}`,
        `DELETE FROM ${SCHEMA}.eqa_lab_enrollment_lab_unit WHERE enrollment_id = ${enrollmentId}`,
      ]);
      // Rows the UI run wrote are gone; the seeded cycle, enrollment and
      // scheme unwind in reverse insert order.
      reportCleanupFailures([...failures, ...seeded.drain()]);
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
      const failures = sweep([
        `DELETE FROM ${SCHEMA}.eqa_participant_followup WHERE scheme_id = ${programId}`,
        `DELETE FROM ${SCHEMA}.eqa_result WHERE eqa_distribution_id IN (${distributions})`,
        `DELETE FROM ${SCHEMA}.eqa_distribution WHERE cycle_id IN (${cycles})`,
        `DELETE FROM ${SCHEMA}.eqa_participant_result WHERE cycle_id IN (${cycles})`,
        `DELETE FROM ${SCHEMA}.eqa_panel_receipt WHERE cycle_id IN (${cycles})`,
        `DELETE FROM ${SCHEMA}.eqa_cycle_state_transition WHERE cycle_id IN (${cycles})`,
        `DELETE FROM ${SCHEMA}.shipment WHERE shipping_box_id IN (${boxes})`,
        `DELETE FROM ${SCHEMA}.box_sample_item WHERE shipping_box_id IN (${boxes})`,
        `DELETE FROM ${SCHEMA}.shipping_box WHERE eqa_cycle_id IN (${cycles})`,
        // Also by panel-sample linkage, not only by box: a repeat box that
        // lost its cycle link would otherwise keep a foreign key on the
        // panel samples and strand the panel behind it.
        `DELETE FROM ${SCHEMA}.box_sample_item WHERE eqa_panel_sample_id IN` +
          ` (SELECT ps.id FROM ${SCHEMA}.eqa_panel_sample ps JOIN ${SCHEMA}.eqa_panel p` +
          ` ON ps.panel_id = p.id WHERE p.cycle_id IN (${cycles}))`,
        `DELETE FROM ${SCHEMA}.eqa_panel_sample WHERE panel_id IN` +
          ` (SELECT id FROM ${SCHEMA}.eqa_panel WHERE cycle_id IN (${cycles}))`,
        `DELETE FROM ${SCHEMA}.eqa_panel WHERE cycle_id IN (${cycles})`,
        `DELETE FROM ${SCHEMA}.eqa_round WHERE cycle_id IN (${cycles})`,
        `DELETE FROM ${SCHEMA}.eqa_cycle_participant WHERE cycle_id IN (${cycles})`,
        `DELETE FROM ${SCHEMA}.eqa_cycle WHERE scheme_id = ${programId}`,
      ]);
      // Test-created rows are gone; the seeded enrollments, scheme and
      // organizations unwind in reverse insert order.
      reportCleanupFailures([...failures, ...seeded.drain()]);
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

/**
 * The organization this installation calls itself, which is what splits the
 * two follow-up surfaces: rows about this lab belong to the participant
 * Follow-Up Queue, every other row to the provider register. The backend
 * resolves it from the SiteName site-information value, falling back to the
 * literal "This laboratory" when that is blank, and returns null when no
 * organization carries the name — in which case the queue can never
 * populate. The write path creates the row on demand, so a seeder that needs
 * a queue row does the same.
 */
function selfOrganizationName(): string {
  const siteName = psql(
    `SELECT trim(coalesce((SELECT value FROM ${SCHEMA}.site_information WHERE name = 'SiteName'), ''))`,
  );
  return siteName === "" ? "This laboratory" : siteName;
}

export interface FollowupSeed {
  cycleId: string;
  cycleName: string;
  selfOrganizationId: string;
  selfOrganizationName: string;
  foreignOrganizationId: string;
  foreignOrganizationName: string;
  analyteName: string;
  /** Name of the analyst carrying the dismissed result, so a spec can find
   * the competency row the dismissal is supposed to write. */
  analystName: string;
  restore: () => void;
}

/**
 * Two follow-up rows on one cycle: one about this lab and one about another
 * participant, which is the whole of the split rule between the Follow-Up
 * Queue and the provider register.
 */
export function seedFollowups(runTag: string): FollowupSeed {
  asSafeString(runTag, "runTag");
  const schemeName = `E2E ${runTag} followup scheme`;
  const cycleName = `E2E ${runTag} followup cycle`;
  const foreignOrganizationName = `E2E ${runTag} Foreign Lab`;
  const seeded = undoStack();

  let cycleId: string;
  let selfOrganizationId: string;
  let foreignOrganizationId: string;
  let analyteName: string;
  let analystName: string;
  let selfName: string;
  try {
    selfName = selfOrganizationName();
    selfOrganizationId = psql(
      `SELECT id FROM ${SCHEMA}.organization WHERE trim(lower(name)) = lower('${asSafeString(selfName, "self org name")}')`,
    );
    if (selfOrganizationId === "") {
      // No row names this lab yet, so the queue would be empty whatever we
      // seed. Create it exactly as the enqueue path would, and remove it
      // again — a pre-existing row is left alone.
      selfOrganizationId = String(intSafeIdBase() + 90);
      insertOrganization(selfOrganizationId, selfName);
      seeded.push(
        `DELETE FROM ${SCHEMA}.organization WHERE id = ${selfOrganizationId}`,
      );
    } else {
      asInt(selfOrganizationId, "self org id");
    }

    foreignOrganizationId = String(intSafeIdBase() + 91);
    insertOrganization(foreignOrganizationId, foreignOrganizationName);
    seeded.push(
      `DELETE FROM ${SCHEMA}.organization WHERE id = ${foreignOrganizationId}`,
    );

    const schemeId = insertProgram(schemeName, "E2E External Provider", false);
    seeded.push(`DELETE FROM ${SCHEMA}.eqa_program WHERE id = ${schemeId}`);

    cycleId = insertCycle(schemeId, cycleName, "SCORED");
    seeded.push(`DELETE FROM ${SCHEMA}.eqa_cycle WHERE id = ${cycleId}`);

    const analyte = psql(
      `SELECT id || '|' || name FROM ${SCHEMA}.analyte ORDER BY id LIMIT 1`,
    ).split("|");
    const analyteId = asInt(analyte[0], "analyte id");
    analyteName = analyte.slice(1).join("|");

    // A real participant result behind the follow-up, carrying an analyst.
    // Dismissal only records a competency event for result ids named in the
    // summary, and only when that row exists and has an analyst assigned —
    // a summary without the id dismisses successfully and writes nothing,
    // which would make any assertion about the competency trail vacuous.
    analystName = `Analyst${asSafeString(runTag, "runTag")}`;
    const analystId = asInt(
      psql(
        `INSERT INTO ${SCHEMA}.system_user (id, external_id, login_name, last_name, first_name, initials,` +
          ` is_active, is_employee, lastupdated) VALUES (nextval('${SCHEMA}.system_user_seq'),` +
          ` 'e2e-fu-${asSafeString(runTag, "runTag")}', 'e2e-fu-${asSafeString(runTag, "runTag")}',` +
          ` '${analystName}', 'E2E', 'E2E', 'Y', 'Y', now()) RETURNING id`,
      ),
      "analyst id",
    );
    seeded.push(`DELETE FROM ${SCHEMA}.system_user WHERE id = ${analystId}`);

    const roundId = asInt(
      psql(
        `INSERT INTO ${SCHEMA}.eqa_round (id, fhir_uuid, cycle_id, round_number, distribution_date,` +
          ` submission_deadline, sample_count, status, sys_user_id)` +
          ` VALUES (nextval('${SCHEMA}.eqa_round_seq'), gen_random_uuid(), ${cycleId}, 1,` +
          ` now() - interval '30 days', now() - interval '10 days', 1, 'CLOSED', '1') RETURNING id`,
      ),
      "round id",
    );
    seeded.push(`DELETE FROM ${SCHEMA}.eqa_round WHERE id = ${roundId}`);

    const enrollmentId = insertSelfEnrollment(schemeName);
    seeded.push(
      `DELETE FROM ${SCHEMA}.eqa_lab_program_enrollment WHERE id = ${enrollmentId}`,
    );

    seeded.push(
      `DELETE FROM ${SCHEMA}.eqa_participant_result WHERE cycle_id = ${cycleId}`,
    );
    const participantResultId = asInt(
      psql(
        `INSERT INTO ${SCHEMA}.eqa_participant_result (id, fhir_uuid, cycle_id, round_id, lab_enrollment_id,` +
          ` analyte_id, result_value, submission_status, performance_status, assigned_analyst_id, submitted_at,` +
          ` score_received_at, sys_user_id) VALUES (nextval('${SCHEMA}.eqa_participant_result_seq'),` +
          ` gen_random_uuid(), ${cycleId}, ${roundId}, ${enrollmentId}, ${analyteId}, '210',` +
          ` 'SCORED', 'UNACCEPTABLE', ${analystId}, now() - interval '20 days',` +
          ` now() - interval '15 days', '1') RETURNING id`,
      ),
      "participant result id",
    );

    // The summary JSON is what the triage tables render and what dismissal
    // reads result ids from, so it carries a realistic failing row.
    const summary =
      `{"source":"external","unacceptable":[{"participantResultId":${participantResultId},` +
      `"analyteId":${analyteId},"reported":"210.0",` +
      `"target":"100.0","zScore":"3.61","performanceStatus":"UNACCEPTABLE"}]}`;
    seeded.push(
      `DELETE FROM ${SCHEMA}.eqa_participant_followup WHERE cycle_id = ${cycleId}`,
    );
    for (const organizationId of [selfOrganizationId, foreignOrganizationId]) {
      psql(
        `INSERT INTO ${SCHEMA}.eqa_participant_followup (id, scheme_id, cycle_id, participant_org_id,` +
          ` participant_result_summary_json, followup_status, notified_at, persistent_failure_flag,` +
          ` sys_user_id) VALUES (nextval('${SCHEMA}.eqa_participant_followup_seq'), ${schemeId}, ${cycleId},` +
          ` ${organizationId}, $json$${summary}$json$, 'NOTIFIED', now(), false, '1')`,
      );
    }
  } catch (error) {
    seeded.drain();
    throw error;
  }

  return {
    cycleId,
    cycleName,
    selfOrganizationId,
    selfOrganizationName: selfName,
    foreignOrganizationId,
    foreignOrganizationName,
    analyteName,
    analystName,
    restore: () => {
      // Triage writes competency events against the cycle's scheme.
      const failures = sweep([
        `DELETE FROM ${SCHEMA}.eqa_analyst_competency_event WHERE cycle_id = ${cycleId}`,
      ]);
      reportCleanupFailures([...failures, ...seeded.drain()]);
    },
  };
}

export interface ReceiptConditionsSeed {
  cycleId: string;
  overdueOrganizationName: string;
  damagedOrganizationName: string;
  damageNotes: string;
  restore: () => void;
}

/**
 * A dispatched cycle holding the two receipt conditions a provider has to
 * notice, neither of which the happy path produces.
 *
 * Overdue is derived, never stored: the monitor tags a shipment whose
 * estimated delivery is more than two business days past, so the date is
 * planted well behind today. Arrived damaged needs both halves — the
 * shipment delivered AND a panel receipt carrying integrity_ok false whose
 * shipment_id is set. Only the participant receipt endpoint sets that
 * column; the Add Order path leaves it null, which is why a receipt recorded
 * there stays invisible to the provider.
 */
export function seedReceiptConditions(runTag: string): ReceiptConditionsSeed {
  asSafeString(runTag, "runTag");
  const schemeName = `E2E ${runTag} receipt scheme`;
  const overdueOrganizationName = `E2E ${runTag} Late Lab`;
  const damagedOrganizationName = `E2E ${runTag} Broken Lab`;
  const damageNotes = `E2E ${runTag} cold chain broken`;
  const seeded = undoStack();

  let cycleId: string;
  try {
    const base = intSafeIdBase();
    const overdueOrganizationId = String(base + 80);
    const damagedOrganizationId = String(base + 81);
    insertOrganization(overdueOrganizationId, overdueOrganizationName);
    insertOrganization(damagedOrganizationId, damagedOrganizationName);
    seeded.push(
      `DELETE FROM ${SCHEMA}.organization WHERE id IN (${overdueOrganizationId}, ${damagedOrganizationId})`,
    );

    const schemeId = insertProgram(schemeName, "This laboratory", false);
    seeded.push(`DELETE FROM ${SCHEMA}.eqa_program WHERE id = ${schemeId}`);

    cycleId = insertCycle(schemeId, `E2E ${runTag} receipt cycle`, "SHIPPED");
    seeded.push(`DELETE FROM ${SCHEMA}.eqa_cycle WHERE id = ${cycleId}`);

    seeded.push(
      `DELETE FROM ${SCHEMA}.eqa_cycle_participant WHERE cycle_id = ${cycleId}`,
    );
    for (const organizationId of [
      overdueOrganizationId,
      damagedOrganizationId,
    ]) {
      psql(
        `INSERT INTO ${SCHEMA}.eqa_cycle_participant (id, cycle_id, organization_id, sys_user_id)` +
          ` VALUES (nextval('${SCHEMA}.eqa_cycle_participant_seq'), ${cycleId}, ${organizationId}, '1')`,
      );
    }

    const boxIds: string[] = [];
    const shipmentIds: string[] = [];
    for (const [organizationId, boxState, shipmentStatus, delivered] of [
      [overdueOrganizationId, "SENT", "IN_TRANSIT", false],
      [damagedOrganizationId, "RECEIVED", "DELIVERED", true],
    ] as [string, string, string, boolean][]) {
      const boxId = asInt(
        psql(
          `INSERT INTO ${SCHEMA}.shipping_box (id, box_id, fhir_uuid, destination_facility_id, state,` +
            ` created_date, archived, sys_user_id, lastupdated, eqa_cycle_id, sent_date${delivered ? ", received_date" : ""})` +
            ` VALUES (nextval('${SCHEMA}.shipping_box_seq'), '${boxCode(cycleId, organizationId)}',` +
            ` gen_random_uuid(), ${organizationId}, '${boxState}', now(), false, 1, now(), ${cycleId},` +
            ` now() - interval '10 days'${delivered ? ", now() - interval '1 day'" : ""}) RETURNING id`,
        ),
        "box id",
      );
      boxIds.push(boxId);
      shipmentIds.push(
        asInt(
          psql(
            `INSERT INTO ${SCHEMA}.shipment (id, shipping_box_id, courier, tracking_number, shipped_date,` +
              ` estimated_delivery_date, ${delivered ? "actual_delivery_date, " : ""}status, sys_user_id,` +
              ` lastupdated) VALUES (nextval('${SCHEMA}.shipment_seq'), ${boxId}, 'E2E courier',` +
              ` 'TRK-${asSafeString(runTag, "runTag")}-${organizationId}', now() - interval '10 days',` +
              ` now() - interval '7 days', ${delivered ? "now() - interval '1 day', " : ""}'${shipmentStatus}',` +
              ` 1, now()) RETURNING id`,
          ),
          "shipment id",
        ),
      );
    }
    seeded.push(
      `DELETE FROM ${SCHEMA}.shipment WHERE id IN (${shipmentIds.join(", ")})`,
    );
    seeded.push(
      `DELETE FROM ${SCHEMA}.shipping_box WHERE id IN (${boxIds.join(", ")})`,
    );

    const enrollmentId = insertSelfEnrollment(schemeName);
    seeded.push(
      `DELETE FROM ${SCHEMA}.eqa_lab_program_enrollment WHERE id = ${enrollmentId}`,
    );

    psql(
      `INSERT INTO ${SCHEMA}.eqa_panel_receipt (id, cycle_id, lab_enrollment_id, shipment_id, received_date,` +
        ` received_by, integrity_ok, integrity_notes, sys_user_id)` +
        ` VALUES (nextval('${SCHEMA}.eqa_panel_receipt_seq'), ${cycleId}, ${enrollmentId},` +
        ` ${shipmentIds[1]}, now() - interval '1 day', 1, false,` +
        ` '${asSafeString(damageNotes, "damage notes")}', '1')`,
    );
    seeded.push(
      `DELETE FROM ${SCHEMA}.eqa_panel_receipt WHERE cycle_id = ${cycleId}`,
    );
  } catch (error) {
    seeded.drain();
    throw error;
  }

  return {
    cycleId,
    overdueOrganizationName,
    damagedOrganizationName,
    damageNotes,
    restore: () => reportCleanupFailures(seeded.drain()),
  };
}

export interface OversightSeed {
  schemeName: string;
  cycleName: string;
  sectionName: string;
  analystName: string;
  analyteName: string;
  restore: () => void;
}

/**
 * Scored participant results plus one competency event, which is what the
 * three oversight dashboards read.
 *
 * Lab Performance reads eqa_participant_result — not the provider-side
 * eqa_result — joined up through the cycle to the scheme, and skips any row
 * whose performance_status is null. The section shown in the coverage matrix
 * comes from the analysis behind the result, falling back to the scheme's own
 * test section, so the scheme carries one and the results leave analysis_id
 * null. Two results are planted, one acceptable and one not, so the matrix
 * cell takes the worst verdict and the recent row can be checked against a
 * known count.
 *
 * Analyst Competency unions competency events with scored results the events
 * do not already cover. One unacceptable-score event is planted: a single
 * evaluable sample sits under the four-sample evidence floor, so the analyst
 * reads as under review rather than competent, which is the honest band for
 * one data point.
 */
export function seedOversightData(runTag: string): OversightSeed {
  asSafeString(runTag, "runTag");
  const schemeName = `E2E ${runTag} oversight scheme`;
  const cycleName = `E2E ${runTag} oversight cycle`;
  const seeded = undoStack();

  let sectionName: string;
  let analystName: string;
  let analyteName: string;
  try {
    const section = psql(
      `SELECT id || '|' || name FROM ${SCHEMA}.test_section ORDER BY id LIMIT 1`,
    ).split("|");
    const sectionId = asInt(section[0], "section id");
    sectionName = section.slice(1).join("|");

    // One analyte per result: eqa_participant_result is unique on round,
    // enrollment and analyte together, so two verdicts need two analytes.
    const analytes = psql(
      `SELECT string_agg(id || '~' || name, '#') FROM (SELECT id, name FROM ${SCHEMA}.analyte` +
        ` ORDER BY id LIMIT 2) a`,
    )
      .split("#")
      .map((row) => row.split("~"));
    if (analytes.length < 2) {
      throw new Error(`Need 2 analytes, found ${analytes.length}`);
    }
    const analyteIds = analytes.map((a) => asInt(a[0], "analyte id"));
    // The competency event hangs off the failing result, so its analyte is
    // the one the evidence table shows.
    analyteName = analytes[1].slice(1).join("~");

    // A dedicated analyst, not the acting user: this stack's admin already
    // carries a decade of prior competency rows, which would decide the band
    // instead of the evidence seeded here. A system user is enough — the
    // analyst is only ever referenced, never logged in as.
    analystName = `Analyst${asSafeString(runTag, "runTag")}`;
    const analystId = asInt(
      psql(
        `INSERT INTO ${SCHEMA}.system_user (id, external_id, login_name, last_name, first_name, initials,` +
          ` is_active, is_employee, lastupdated) VALUES (nextval('${SCHEMA}.system_user_seq'),` +
          ` 'e2e-${asSafeString(runTag, "runTag")}', 'e2e-${asSafeString(runTag, "runTag")}',` +
          ` '${analystName}', 'E2E', 'E2E', 'Y', 'Y', now()) RETURNING id`,
      ),
      "analyst id",
    );
    seeded.push(`DELETE FROM ${SCHEMA}.system_user WHERE id = ${analystId}`);

    const schemeId = asInt(
      psql(
        `INSERT INTO ${SCHEMA}.eqa_program (id, fhir_uuid, name, is_active, sys_user_id, provider, scheme_type,` +
          ` test_section_id) VALUES (nextval('${SCHEMA}.eqa_program_seq'), gen_random_uuid(),` +
          ` '${asSafeString(schemeName, "scheme name")}', true, '1', 'E2E External Provider', 'REGIONAL_PT',` +
          ` ${sectionId}) RETURNING id`,
      ),
      "scheme id",
    );
    seeded.push(`DELETE FROM ${SCHEMA}.eqa_program WHERE id = ${schemeId}`);

    // Dates sit inside the twelve-month window every rollup applies, and the
    // submission lands before the deadline so the cycle is not counted late.
    const cycleId = asInt(
      psql(
        `INSERT INTO ${SCHEMA}.eqa_cycle (id, fhir_uuid, scheme_id, cycle_number, cycle_name, status,` +
          ` planned_start_date, planned_end_date, created_by, sys_user_id)` +
          ` VALUES (nextval('${SCHEMA}.eqa_cycle_seq'), gen_random_uuid(), ${schemeId}, 1,` +
          ` '${asSafeString(cycleName, "cycle name")}', 'SCORED', now() - interval '60 days',` +
          ` now() - interval '30 days', 1, '1') RETURNING id`,
      ),
      "cycle id",
    );
    seeded.push(`DELETE FROM ${SCHEMA}.eqa_cycle WHERE id = ${cycleId}`);

    const roundId = asInt(
      psql(
        `INSERT INTO ${SCHEMA}.eqa_round (id, fhir_uuid, cycle_id, round_number, distribution_date,` +
          ` submission_deadline, sample_count, status, sys_user_id)` +
          ` VALUES (nextval('${SCHEMA}.eqa_round_seq'), gen_random_uuid(), ${cycleId}, 1,` +
          ` now() - interval '60 days', now() - interval '30 days', 2, 'CLOSED', '1') RETURNING id`,
      ),
      "round id",
    );
    seeded.push(`DELETE FROM ${SCHEMA}.eqa_round WHERE id = ${roundId}`);

    const enrollmentId = insertSelfEnrollment(schemeName);
    seeded.push(
      `DELETE FROM ${SCHEMA}.eqa_lab_program_enrollment WHERE id = ${enrollmentId}`,
    );

    // Registered before the inserts: a failure part way through the loop
    // must still unwind the rows already written.
    seeded.push(
      `DELETE FROM ${SCHEMA}.eqa_participant_result WHERE cycle_id = ${cycleId}`,
    );
    const resultIds: string[] = [];
    for (const [index, verdict] of ["ACCEPTABLE", "UNACCEPTABLE"].entries()) {
      resultIds.push(
        asInt(
          psql(
            `INSERT INTO ${SCHEMA}.eqa_participant_result (id, fhir_uuid, cycle_id, round_id,` +
              ` lab_enrollment_id, analyte_id, result_value, submission_status, performance_status,` +
              ` assigned_analyst_id, submitted_at, score_received_at, sys_user_id)` +
              ` VALUES (nextval('${SCHEMA}.eqa_participant_result_seq'), gen_random_uuid(), ${cycleId},` +
              ` ${roundId}, ${enrollmentId}, ${analyteIds[index]}, '100', 'SCORED', '${verdict}',` +
              ` ${analystId},` +
              ` now() - interval '40 days', now() - interval '35 days', '1') RETURNING id`,
          ),
          "result id",
        ),
      );
    }
    psql(
      `INSERT INTO ${SCHEMA}.eqa_analyst_competency_event (id, analyst_id, event_type, event_date, scheme_id,` +
        ` cycle_id, participant_result_id, analyte_id, sys_user_id)` +
        ` VALUES (nextval('${SCHEMA}.eqa_analyst_competency_event_seq'), ${analystId}, 'UNACCEPTABLE_SCORE',` +
        ` current_date - 35, ${schemeId}, ${cycleId}, ${resultIds[1]}, ${analyteIds[1]}, '1')`,
    );
    seeded.push(
      `DELETE FROM ${SCHEMA}.eqa_analyst_competency_event WHERE cycle_id = ${cycleId}`,
    );
  } catch (error) {
    seeded.drain();
    throw error;
  }

  return {
    schemeName,
    cycleName,
    sectionName,
    analystName,
    analyteName,
    restore: () => reportCleanupFailures(seeded.drain()),
  };
}

/**
 * Remove self-enrollments whose programme name starts with the given prefix.
 * The enrollment page can deactivate a row but never delete one, so a spec
 * that creates enrollments through the UI has no in-app way to clean up.
 */
export function removeSelfEnrollments(programNamePrefix: string): void {
  asSafeString(programNamePrefix, "programme name prefix");
  const ids = psql(
    `SELECT string_agg(id::text, ',') FROM ${SCHEMA}.eqa_lab_program_enrollment` +
      ` WHERE program_name LIKE '${programNamePrefix}%'`,
  );
  if (ids === "") {
    return;
  }
  const enrollmentIds = ids.split(",").map((id) => asInt(id, "enrollment id"));
  const list = enrollmentIds.join(", ");
  psql(
    `DELETE FROM ${SCHEMA}.eqa_lab_enrollment_test_map WHERE enrollment_id IN (${list})`,
  );
  psql(
    `DELETE FROM ${SCHEMA}.eqa_lab_enrollment_lab_unit WHERE enrollment_id IN (${list})`,
  );
  psql(
    `DELETE FROM ${SCHEMA}.eqa_lab_program_enrollment WHERE id IN (${list})`,
  );
}

/** The participant-only fixture user, and the role that grants participant
 * EQA access without the provider grant. */
export const PARTICIPANT_USER = "e2eparticipant";
/** Both bench roles, matching the grant liquibase gives participant EQA
 * access: Reception opens order entry, Results opens result entry. */
const PARTICIPANT_ROLES = "'Results', 'Reception'";
const PARTICIPANT_ROLE_COUNT = 2;

/**
 * Ensure a participant-only login exists, so a spec can see the EQA module
 * as someone who is not a provider. Everything provider-side ORs in the
 * global administrator role, which is why the ordinary test user can never
 * render the participant-only branch.
 *
 * Idempotent, and deliberately permanent: a per-run user would need a
 * per-run storage state and a teardown hook the suite does not have. The
 * password is admin's own hash, copied — bcrypt hashing lives in Java, so a
 * SQL seeder cannot compute one, and this keeps the fixture credentials in
 * step with the existing test user.
 */
export function seedParticipantUser(): void {
  // One statement, so the three rows and the role grant either all land or
  // none do: a half-created user would be picked up by the early-exit check
  // on the next run and never repaired.
  //
  // The id comes from login_user_seq, not from max(id) + 1. Taking the max
  // leaves the sequence behind the table, and the next user the application
  // creates then asks for an id that already exists — a primary key
  // collision seeded by a test fixture. The setval first repairs a sequence
  // any earlier fixture may already have left behind.
  psql(
    `DO $seed$
     DECLARE
       map_id integer;
       user_id integer;
     BEGIN
       -- Keep the existing user only if it is complete: a login, a system
       -- user, and every role expected. An existence check that stopped at
       -- one role left a half-granted user in place run after run, and the
       -- spec then failed on a permission the fixture was supposed to give.
       IF EXISTS (SELECT 1 FROM ${SCHEMA}.login_user WHERE login_name = '${PARTICIPANT_USER}')
          AND (SELECT count(DISTINCT r.name) FROM ${SCHEMA}.system_user su
               JOIN ${SCHEMA}.system_user_role sur ON sur.system_user_id = su.id
               JOIN ${SCHEMA}.system_role r ON r.id = sur.role_id
               WHERE su.login_name = '${PARTICIPANT_USER}'
                 AND r.name IN (${PARTICIPANT_ROLES})) = ${PARTICIPANT_ROLE_COUNT}
          AND EXISTS (SELECT 1 FROM ${SCHEMA}.lab_unit_roles lur
                      JOIN ${SCHEMA}.system_user su ON su.id = lur.system_user_id
                      WHERE su.login_name = '${PARTICIPANT_USER}') THEN
         RETURN;
       END IF;

       DELETE FROM ${SCHEMA}.lab_unit_roles WHERE system_user_id IN
         (SELECT id FROM ${SCHEMA}.system_user WHERE login_name = '${PARTICIPANT_USER}');
       DELETE FROM ${SCHEMA}.user_lab_unit_roles WHERE system_user_id IN
         (SELECT id FROM ${SCHEMA}.system_user WHERE login_name = '${PARTICIPANT_USER}');
       DELETE FROM ${SCHEMA}.system_user_role WHERE system_user_id IN
         (SELECT id FROM ${SCHEMA}.system_user WHERE login_name = '${PARTICIPANT_USER}');
       DELETE FROM ${SCHEMA}.system_user WHERE login_name = '${PARTICIPANT_USER}';
       DELETE FROM ${SCHEMA}.login_user WHERE login_name = '${PARTICIPANT_USER}';

       PERFORM setval('${SCHEMA}.login_user_seq',
         GREATEST((SELECT max(id) FROM ${SCHEMA}.login_user),
                  (SELECT last_value FROM ${SCHEMA}.login_user_seq)));

       INSERT INTO ${SCHEMA}.login_user (id, login_name, password, password_expired_dt, account_locked,
         account_disabled, is_admin, user_time_out, last_updated)
       SELECT nextval('${SCHEMA}.login_user_seq'), '${PARTICIPANT_USER}', password,
         '2031-12-31', 'N', 'N', 'N', '720', now()
       FROM ${SCHEMA}.login_user WHERE login_name = 'admin';

       INSERT INTO ${SCHEMA}.system_user (id, external_id, login_name, last_name, first_name, initials,
         is_active, is_employee, lastupdated)
       VALUES (nextval('${SCHEMA}.system_user_seq'), '${PARTICIPANT_USER}', '${PARTICIPANT_USER}',
         'Participant', 'E2E', 'EP', 'Y', 'Y', now());

       INSERT INTO ${SCHEMA}.system_user_role (system_user_id, role_id)
       SELECT su.id, r.id FROM ${SCHEMA}.system_user su, ${SCHEMA}.system_role r
       WHERE su.login_name = '${PARTICIPANT_USER}' AND r.name IN (${PARTICIPANT_ROLES});

       -- Lab units, which an administrator never needs: it is handed every
       -- unit implicitly, while a non-administrator with no unit mapping is
       -- offered no sample types at all and cannot enter an order. The
       -- mapping is the same three-table shape the user admin screen writes —
       -- a map row naming the unit, the roles that apply within it, and the
       -- link to the user. A fixture user without it is not a realistic
       -- laboratory user, and a spec running as one would fail on data
       -- rather than on behaviour.
       INSERT INTO ${SCHEMA}.lab_unit_role_map (lab_unit_role_map_id, lab_unit)
       VALUES (nextval('${SCHEMA}.lab_unit_role_map_lab_unit_role_map_id_seq'), 'AllLabUnits')
       RETURNING lab_unit_role_map_id INTO map_id;

       INSERT INTO ${SCHEMA}.lab_roles (lab_unit_role_map_id, role)
       SELECT map_id, r.id::text FROM ${SCHEMA}.system_role r WHERE r.name IN (${PARTICIPANT_ROLES});

       SELECT id INTO user_id FROM ${SCHEMA}.system_user WHERE login_name = '${PARTICIPANT_USER}';

       INSERT INTO ${SCHEMA}.user_lab_unit_roles (system_user_id, last_updated)
       VALUES (user_id, now());

       INSERT INTO ${SCHEMA}.lab_unit_roles (system_user_id, lab_unit_role_map_id)
       VALUES (user_id, map_id);
     END
     $seed$;`,
  );
}

export interface InHouseSchemeSeed {
  schemeId: string;
  cycleId: string;
  schemeName: string;
  restore: () => void;
}

/**
 * An in-house scheme and cycle, with no panel.
 *
 * The panel is created through the application's own endpoint rather than
 * planted here, because a panel's target values are encrypted by an attribute
 * converter on the way in. A value written straight to the column cannot be
 * decrypted on read: the panels endpoint answers 500 and the page, which
 * guards only against a null reply, hands the error body to its tile
 * arithmetic and dies. Seeding the target also could not test the one thing
 * worth testing — that a sealed target is withheld from a caller without the
 * unblind privilege.
 *
 * The scheme must be IN_HOUSE: the panels page filters its picker on that
 * type, so a panel under any other scheme is unreachable there.
 */
export function seedInHouseScheme(runTag: string): InHouseSchemeSeed {
  asSafeString(runTag, "runTag");
  const schemeName = `E2E ${runTag} in-house scheme`;
  const seeded = undoStack();

  let schemeId: string;
  let cycleId: string;
  try {
    schemeId = asInt(
      psql(
        `INSERT INTO ${SCHEMA}.eqa_program (id, fhir_uuid, name, is_active, sys_user_id, provider, scheme_type)` +
          ` VALUES (nextval('${SCHEMA}.eqa_program_seq'), gen_random_uuid(),` +
          ` '${asSafeString(schemeName, "scheme name")}', true, '1', 'This laboratory', 'IN_HOUSE')` +
          ` RETURNING id`,
      ),
      "scheme id",
    );
    seeded.push(`DELETE FROM ${SCHEMA}.eqa_program WHERE id = ${schemeId}`);

    cycleId = insertCycle(schemeId, `E2E ${runTag} in-house cycle`, "SHIPPED");
    seeded.push(`DELETE FROM ${SCHEMA}.eqa_cycle WHERE id = ${cycleId}`);
  } catch (error) {
    seeded.drain();
    throw error;
  }

  return {
    schemeId,
    cycleId,
    schemeName,
    restore: () => {
      const failures = sweep([
        `DELETE FROM ${SCHEMA}.eqa_panel_sample WHERE panel_id IN` +
          ` (SELECT id FROM ${SCHEMA}.eqa_panel WHERE scheme_id = ${schemeId})`,
        `DELETE FROM ${SCHEMA}.eqa_panel WHERE scheme_id = ${schemeId}`,
      ]);
      reportCleanupFailures([...failures, ...seeded.drain()]);
    },
  };
}

/**
 * Move a panel to the status that offers unblinding.
 *
 * The application reaches DISTRIBUTED by sealing a panel and creating one
 * blinded order per aliquot, which needs an analyst roster and tests carrying
 * analyte mappings. That is the wizard's job and is covered by its own tests;
 * this is only the state change, so the spec can start from a distributed
 * panel without asserting how it got there.
 */
export function markPanelDistributed(panelId: string): void {
  psql(
    `UPDATE ${SCHEMA}.eqa_panel SET status = 'DISTRIBUTED' WHERE id = ${asInt(panelId, "panel id")}`,
  );
}

/** The participant-only fixture user, and the role that grants participant
 * EQA access without the provider grant. */
