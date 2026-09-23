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
 * This is the first spec in the repo to interrupt infrastructure, so it is
 * deliberately the narrowest interruption that still reproduces the incident.
 * OpenELIS keeps running and the browser keeps reaching it. Only the bridge's
 * path to it is cut, by detaching the webapp from the analyzer network, which
 * the bridge alone uses.
 *
 * An earlier version stopped the webapp container outright. That proved the
 * guarantee and broke the two UI specs that ran after it in the same shard: a
 * restarted webapp comes back on a new address the proxy has already cached, so
 * later specs get error pages from a stack that looks healthy. The path is
 * restored in a `finally`, and the restore is asserted, because a leaked
 * disconnect would be just as damaging.
 */

import { expect, test } from "../../../helpers/test-base";
import { resolveDbContainer } from "../../../helpers/db-container";
import {
  bridgeApi,
  countOpenElisAcceptances,
  docker,
  getOutboxEntry,
  hasOutbox,
  isOnNetwork,
  outboxPayload,
  pushAstmResult,
  resolveAnalyzerNetwork,
  resolveBridgeContainer,
  resolveWebappContainer,
  restoreBridgePathToOpenElis,
  severBridgePathToOpenElis,
  uniqueLane10Accession,
  waitForBridge,
  waitForOutboxEntry,
  waitForOutboxState,
} from "../../../helpers/analyzer-bridge-outbox";
import type { APIRequestContext } from "@playwright/test";

/**
 * Recovery is a retry cycle now, not a Tomcat boot, but the budget stays
 * generous: the bridge only notices the path is back on its next attempt, and
 * the JVM may be holding a cached address for the old one.
 */
const OUTAGE_TEST_TIMEOUT = 600_000;
const DELIVERY_TIMEOUT = 240_000;
const HELD_TIMEOUT = 60_000;

test.describe("Analyzer results survive an OpenELIS outage", () => {
  // One at a time, so the window in which the bridge cannot reach OpenELIS is as
  // short as it can be and a failure is easy to attribute.
  test.describe.configure({ mode: "serial" });

  let api: APIRequestContext;
  let analyzerNetwork: string;
  const webapp = resolveWebappContainer();
  const bridge = resolveBridgeContainer();

  test.beforeAll(async () => {
    api = await bridgeApi();
    // A hard failure, not a skip. Skipping here would turn a bridge pinned to a
    // build without the outbox into a green run, which is exactly the regression
    // this spec exists to block.
    expect(
      await hasOutbox(api),
      "the pinned bridge build must expose the delivery outbox",
    ).toBe(true);
    // Resolved here rather than in the describe body: that body is evaluated
    // while Playwright is collecting tests, when no stack need be running yet.
    analyzerNetwork = resolveAnalyzerNetwork(webapp);
  });

  test.afterAll(async () => {
    await api?.dispose();
  });

  test("a result received during an outage is kept whole, survives a bridge restart, and is delivered once", async () => {
    test.setTimeout(OUTAGE_TEST_TIMEOUT);
    const accession = uniqueLane10Accession();
    let pathSevered = false;

    try {
      severBridgePathToOpenElis(webapp, analyzerNetwork);
      pathSevered = true;

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
        "the result must still be undelivered while the bridge cannot reach OpenELIS",
      ).toContain(afterRestart!.state);

      restoreBridgePathToOpenElis(webapp, analyzerNetwork);
      pathSevered = false;

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
      if (pathSevered && !isOnNetwork(webapp, analyzerNetwork)) {
        try {
          restoreBridgePathToOpenElis(webapp, analyzerNetwork);
        } catch {
          // Reported by the assertion below, which says what actually matters.
        }
      }
      // Checked, not merely attempted: a leaked disconnect leaves OpenELIS
      // unable to reach the bridge for every spec that follows in this shard.
      // Soft, so that when the body failed first it is the body's error that
      // gets reported rather than this one.
      expect
        .soft(
          isOnNetwork(webapp, analyzerNetwork),
          "the bridge's path to OpenELIS must be restored before any later spec runs",
        )
        .toBe(true);
    }
  });

  test("a result delivered while OpenELIS is up is accepted once and keeps its received message", async () => {
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
