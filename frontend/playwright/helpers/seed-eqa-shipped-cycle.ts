import { execFileSync } from "node:child_process";
import { resolveDbContainer } from "./db-container";

/**
 * EQA T-46 E2E seeding — a provider cycle parked in SHIPPED with a partial
 * roster: participant A's shipment DELIVERED, participant B never shipped.
 * That is exactly the state the Receipt Monitor's "Open submissions" manual
 * override exists for.
 *
 * Seeded directly via `docker exec psql` (same pattern as
 * seed-qc-sigma-data.ts): the cycle wizard REST endpoint could create the
 * cycle, but walking it to SHIPPED requires the prep gate (panel material,
 * aliquot arithmetic, QC) which is irrelevant to what this spec asserts. The
 * DB rows mirror what dispatch itself writes.
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

export interface ShippedCycleSeed {
  cycleId: string;
  deliveredOrgName: string;
  unshippedOrgName: string;
  /** Remove every seeded row (children before parents). */
  restore: () => void;
}

export function seedShippedCycleWithPartialDelivery(
  runTag: string,
): ShippedCycleSeed {
  asSafeString(runTag, "runTag");
  // High id range, unique per run — organization.id has no sequence contract
  // we can borrow, and workers=1 means no same-stack concurrency. Milliseconds
  // over a ~27h window so a crashed run's leftovers cannot collide with the
  // next run's ids; a collision still fails loudly on the PK. The ids MUST fit
  // a signed 32-bit int: parts of the codebase read organization.id as int,
  // and one larger row kills displayListService — and with it the whole webapp
  // boot (learned live, 2026-08-26).
  const orgA = String(1900000000 + (Date.now() % 100000000));
  const orgB = String(Number(orgA) + 1);
  const deliveredOrgName = `E2E ${runTag} Delivered Lab`;
  const unshippedOrgName = `E2E ${runTag} Dormant Lab`;

  for (const [id, name] of [
    [orgA, deliveredOrgName],
    [orgB, unshippedOrgName],
  ]) {
    psql(
      `INSERT INTO ${SCHEMA}.organization (id, name, mls_sentinel_lab_flag, is_active, lastupdated)` +
        ` VALUES (${asInt(id, "org id")}, '${asSafeString(name, "org name")}', 'N', 'Y', now())`,
    );
  }

  const schemeId = asInt(
    psql(
      `INSERT INTO ${SCHEMA}.eqa_program (id, fhir_uuid, name, is_active, sys_user_id, provider, scheme_type)` +
        ` VALUES (nextval('${SCHEMA}.eqa_program_seq'), gen_random_uuid(), 'E2E ${runTag} scheme', true, '1',` +
        ` 'This laboratory', 'REGIONAL_PT') RETURNING id`,
    ),
    "scheme id",
  );

  const cycleId = asInt(
    psql(
      `INSERT INTO ${SCHEMA}.eqa_cycle (id, fhir_uuid, scheme_id, cycle_number, cycle_name, status, created_by,` +
        ` sys_user_id) VALUES (nextval('${SCHEMA}.eqa_cycle_seq'), gen_random_uuid(), ${schemeId}, 1,` +
        ` 'E2E ${runTag} cycle', 'SHIPPED', 1, '1') RETURNING id`,
    ),
    "cycle id",
  );

  for (const org of [orgA, orgB]) {
    psql(
      `INSERT INTO ${SCHEMA}.eqa_cycle_participant (id, cycle_id, organization_id, sys_user_id)` +
        ` VALUES (nextval('${SCHEMA}.eqa_cycle_participant_seq'), ${cycleId}, ${asInt(org, "roster org")}, '1')`,
    );
  }

  // Participant A's box, dispatched and delivered — the exact rows dispatch +
  // delivery confirmation write (box code convention EQA-C{cycle}-{org}).
  const boxId = asInt(
    psql(
      `INSERT INTO ${SCHEMA}.shipping_box (id, box_id, fhir_uuid, destination_facility_id, state, created_date,` +
        ` archived, sys_user_id, lastupdated, eqa_cycle_id, sent_date, received_date)` +
        ` VALUES (nextval('${SCHEMA}.shipping_box_seq'), 'EQA-C${cycleId}-${orgA}', gen_random_uuid(), ${orgA},` +
        ` 'RECEIVED', now(), false, 1, now(), ${cycleId}, now(), now()) RETURNING id`,
    ),
    "box id",
  );
  const shipmentId = asInt(
    psql(
      `INSERT INTO ${SCHEMA}.shipment (id, shipping_box_id, courier, tracking_number, shipped_date,` +
        ` actual_delivery_date, status, sys_user_id, lastupdated)` +
        ` VALUES (nextval('${SCHEMA}.shipment_seq'), ${boxId}, 'E2E courier', 'TRK-${runTag}', now(), now(),` +
        ` 'DELIVERED', 1, now()) RETURNING id`,
    ),
    "shipment id",
  );

  return {
    cycleId,
    deliveredOrgName,
    unshippedOrgName,
    restore: () => {
      psql(
        `DELETE FROM ${SCHEMA}.eqa_cycle_state_transition WHERE cycle_id = ${cycleId}`,
      );
      psql(`DELETE FROM ${SCHEMA}.shipment WHERE id = ${shipmentId}`);
      psql(`DELETE FROM ${SCHEMA}.shipping_box WHERE id = ${boxId}`);
      psql(
        `DELETE FROM ${SCHEMA}.eqa_cycle_participant WHERE cycle_id = ${cycleId}`,
      );
      psql(`DELETE FROM ${SCHEMA}.eqa_cycle WHERE id = ${cycleId}`);
      psql(`DELETE FROM ${SCHEMA}.eqa_program WHERE id = ${schemeId}`);
      psql(`DELETE FROM ${SCHEMA}.organization WHERE id IN (${orgA}, ${orgB})`);
    },
  };
}
