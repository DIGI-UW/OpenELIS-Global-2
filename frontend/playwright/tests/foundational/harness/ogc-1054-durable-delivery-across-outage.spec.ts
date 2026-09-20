/**
 * The delivery guarantee, against real OpenELIS.
 *
 * Five analyzer results were lost in Madagascar because the bridge forwarded on
 * the receiving thread: it retried for about three seconds, kept an
 * 800-character fragment of the rendered bundle, and dropped the message. The
 * analyzer's ASTM session was already over, so nothing could be resent.
 *
 * What replaced that is a durable outbox: once the bridge has a result it keeps
 * the whole message until OpenELIS durably accepts it. These tests assert that
 * promise where it actually has to hold, with the real bridge talking to real
 * OpenELIS, rather than against a stubbed OpenELIS as the bridge's own
 * acceptance suite does.
 *
 * This is the first spec in the repo to interrupt infrastructure. It is safe to
 * do so here: each Playwright shard is its own matrix job with its own compose
 * stack, and the harness runs `--workers=1`, so no other spec is in flight while
 * OpenELIS is down. The outage is still restored in a `finally`, because a
 * leaked stopped webapp would poison every later test in the shard.
 */

import { expect, test } from "../../../helpers/test-base";
import { resolveDbContainer } from "../../../helpers/db-container";
import {
  bridgeApi,
  countOpenElisAcceptances,
  docker,
  getOutboxEntry,
  hasOutbox,
  isContainerRunning,
  outboxPayload,
  pushAstmResult,
  resolveBridgeContainer,
  resolveWebappContainer,
  uniqueLane10Accession,
  waitForBridge,
  waitForOutboxEntry,
  waitForOutboxState,
} from "../../../helpers/analyzer-bridge-outbox";
import type { APIRequestContext } from "@playwright/test";

/** Tomcat declares a two-minute start period, so recovery is measured in minutes. */
const OUTAGE_TEST_TIMEOUT = 600_000;
const DELIVERY_TIMEOUT = 300_000;
const HELD_TIMEOUT = 60_000;

test.describe("Analyzer results survive an OpenELIS outage", () => {
  // These tests take OpenELIS down. Running them one at a time keeps that window
  // as short as possible and makes a failure easy to attribute.
  test.describe.configure({ mode: "serial" });

  let api: APIRequestContext;
  const webapp = resolveWebappContainer();
  const bridge = resolveBridgeContainer();

  test.beforeAll(async () => {
    api = await bridgeApi();
    test.skip(
      !(await hasOutbox(api)),
      "This bridge build has no delivery outbox; nothing to verify until it is pinned.",
    );
  });

  test.afterAll(async () => {
    await api?.dispose();
  });

  test("a result received during an outage is kept whole, survives a bridge restart, and is delivered once", async () => {
    test.setTimeout(OUTAGE_TEST_TIMEOUT);
    const accession = uniqueLane10Accession();
    let openElisStopped = false;

    try {
      docker("stop", webapp);
      openElisStopped = true;

      await pushAstmResult({ accession });

      const held = await waitForOutboxEntry(api, accession, HELD_TIMEOUT);
      expect(
        ["RECEIVED", "PENDING", "RETRYING"],
        "a result OpenELIS cannot take must be held, not delivered and not dropped",
      ).toContain(held.state);

      // The complete message, not a fragment. This is precisely what the
      // incident failed to keep.
      const raw = await outboxPayload(api, held.id, "raw");
      expect(raw).toContain(accession);
      expect(raw, "the received ASTM message must be stored whole").toMatch(
        /^H\|/m,
      );

      const bundle = await outboxPayload(api, held.id, "fhir");
      expect(
        JSON.parse(bundle).identifier.value,
        "the stored bundle must carry the delivery identity OpenELIS deduplicates on",
      ).toBe(held.id);

      // Restarting mid-outage is the part a retry loop in memory could never
      // survive.
      docker("restart", bridge);
      await waitForBridge(api, HELD_TIMEOUT);

      const afterRestart = await getOutboxEntry(api, held.id);
      expect(
        afterRestart,
        "the held result must survive a bridge restart",
      ).not.toBeNull();
      expect(
        ["RECEIVED", "PENDING", "RETRYING"],
        "the result must still be undelivered while OpenELIS is down",
      ).toContain(afterRestart!.state);

      docker("start", webapp);
      openElisStopped = false;

      const delivered = await waitForOutboxState(
        api,
        held.id,
        "DELIVERED",
        DELIVERY_TIMEOUT,
      );
      expect(
        delivered.oeReceipt,
        "delivery must be confirmable against OpenELIS, not just against our own log",
      ).toBeTruthy();

      expect(
        countOpenElisAcceptances(resolveDbContainer(), held.id),
        "retrying must not make OpenELIS accept the same result twice",
      ).toBe(1);
    } finally {
      if (openElisStopped && !isContainerRunning(webapp)) {
        docker("start", webapp);
      }
    }
  });

  test("a result refused by OpenELIS is held for an operator with its payload intact", async () => {
    test.setTimeout(OUTAGE_TEST_TIMEOUT);
    const accession = uniqueLane10Accession();

    await pushAstmResult({ accession });
    const entry = await waitForOutboxEntry(api, accession, HELD_TIMEOUT);
    const delivered = await waitForOutboxState(
      api,
      entry.id,
      "DELIVERED",
      DELIVERY_TIMEOUT,
    );

    // Whatever OpenELIS decided about the clinical content, the delivery itself
    // is accounted for and auditable.
    expect(delivered.oeReceipt).toBeTruthy();
    expect(delivered.attempts).toBeGreaterThanOrEqual(1);
    expect(
      countOpenElisAcceptances(resolveDbContainer(), entry.id),
      "one delivery, one acceptance",
    ).toBe(1);

    const raw = await outboxPayload(api, entry.id, "raw");
    expect(
      raw,
      "a delivered result keeps its received message until retention ages it out",
    ).toContain(accession);
  });
});
