import { test, expect } from "../../../helpers/test-base";
import {
  UI_TIMEOUT,
  LONG_TIMEOUT,
  NAV_TIMEOUT,
} from "../../../helpers/timeouts";
import {
  seedProviderScheme,
  seedReportedResults,
  ProviderSchemeSeed,
  PROVIDER_PARTICIPANT_COUNT,
  RESULTS_PER_ORGANIZATION,
} from "../../../helpers/seed-eqa-data";

/**
 * EQA provider cycle lifecycle (OGC-613).
 *
 * Seeded: a scheme this lab provides, five Active participant enrollments,
 * no cycle. Everything after that happens through the UI: the five-step
 * wizard creates the cycle, prep clears the inventory/QC gate, courier rows
 * dispatch all five panels, and marking every delivery received lets the
 * cycle open submissions BY ITSELF (AUTO / all-shipments-delivered) — the
 * complementary edge to eqa-open-submissions.spec.ts, which covers the
 * manual override on a partial roster. Scoring then walks the banner to
 * Scored (the >=5 reported results it requires are planted in the score
 * container; the statistics themselves are integration-tested).
 *
 * Banner sequence asserted: Prep in progress → Ready to ship → Shipped →
 * Submissions open → Scored.
 */

const RUN = Date.now().toString(36);
const N = PROVIDER_PARTICIPANT_COUNT;

let seed: ProviderSchemeSeed;

test.describe("EQA provider cycle lifecycle", () => {
  test.beforeAll(() => {
    seed = seedProviderScheme(RUN);
  });

  test.afterAll(() => {
    seed?.restore();
  });

  test("a wizard-created cycle ships, delivers, opens submissions on its own, and scores", async ({
    page,
  }) => {
    test.setTimeout(240_000);
    const banner = (state: string) =>
      expect(page.getByText(state, { exact: true }).first()).toBeVisible({
        timeout: UI_TIMEOUT,
      });

    await test.step("scheme board lists the seeded scheme", async () => {
      await page.goto("/qa/eqa/provider/schemes", { timeout: NAV_TIMEOUT });
      await expect(
        page.getByRole("heading", { name: "EQA schemes we provide" }),
      ).toBeVisible({ timeout: UI_TIMEOUT });
      for (const tile of [
        "kpi-active-schemes",
        "kpi-open-cycles",
        "kpi-enrolled",
        "kpi-followups-open",
      ]) {
        await expect(page.getByTestId(tile)).toBeVisible();
      }
      const schemeRow = page.locator("tr", {
        hasText: seed.schemeName,
      });
      await expect(schemeRow.first()).toBeVisible({ timeout: UI_TIMEOUT });
      await schemeRow
        .first()
        .getByRole("button", { name: "New cycle" })
        .click();
    });

    await test.step("wizard step 1 collects distribution date and deadline", async () => {
      await expect(
        page.getByRole("heading", { name: `New cycle — ${seed.schemeName}` }),
      ).toBeVisible({ timeout: UI_TIMEOUT });
      await page.locator("#cycle-name").fill(`E2E ${RUN} cycle`);
      await page.locator("#cycle-number").fill("1");
      // The step-1 relabel: the range picker collects the dates the FRS
      // names, not "planned start/end".
      await expect(page.getByText("Distribution date")).toBeVisible();
      await expect(page.getByText("Submission deadline")).toBeVisible();
      await page.locator("#cycle-planned-start").fill("01/10/2026");
      await page.locator("#cycle-planned-end").fill("15/10/2026");
      await page.keyboard.press("Escape");
      await page.getByRole("button", { name: "Next", exact: true }).click();
    });

    await test.step("wizard steps 2-5 build panel, roster, method", async () => {
      await page.locator("#panel-name").fill(`E2E ${RUN} panel`);
      await page.locator("#sample-code-0").fill(`S-${RUN}-1`);
      await page.locator("select#sample-test-0").selectOption({ index: 1 });
      await page.locator("#sample-target-0").fill("100");
      await page.locator("#sample-unit-0").fill("mg");
      await page.locator("#sample-low-0").fill("90");
      await page.locator("#sample-high-0").fill("110");
      await page.getByRole("button", { name: "Next", exact: true }).click();

      // Step 3: every active enrollment arrives pre-selected.
      await expect(
        page.getByText("Participating laboratories").first(),
      ).toBeVisible({ timeout: UI_TIMEOUT });
      await page.getByRole("button", { name: "Next", exact: true }).click();

      await expect(
        page.getByText("Distribution method", { exact: true }).first(),
      ).toBeVisible();
      await page.locator('label[for="method-CSV"]').click();
      await page.getByRole("button", { name: "Next", exact: true }).click();

      // Step 5 summary names every pre-selected lab — proof step 3 arrived
      // with all active enrollments selected without us touching it.
      await expect(
        page.getByText(seed.organizationNames[0]).first(),
      ).toBeVisible();
      await expect(
        page.getByText(seed.organizationNames[N - 1]).first(),
      ).toBeVisible();
      await page
        .getByRole("button", { name: "Create cycle and begin prep" })
        .click();
      await expect(page).toHaveURL(
        /\/qa\/eqa\/provider\/cycles\/\d+\/workbench/,
        {
          timeout: LONG_TIMEOUT,
        },
      );
    });

    await test.step("prep clears the gate and the cycle is cleared to ship", async () => {
      await banner("Prep in progress");
      // Gate starts blocked: no aliquots, QC unchecked.
      await expect(
        page.getByText("Ready-to-ship gate", { exact: true }),
      ).toBeVisible({ timeout: UI_TIMEOUT });
      // 1 sample x 5 participants + 5 reserved = 10 aliquots needed.
      await page.locator('input[id^="produced-"]').fill("10");
      await page.locator('input[id^="reserved-"]').fill("5");
      await page.locator('label[for^="qc-"]').first().click();
      await page.getByRole("button", { name: "Save prep record" }).click();
      await expect(page.getByText("Prep record updated.").first()).toBeVisible({
        timeout: UI_TIMEOUT,
      });
      await expect(page.getByText("All prep requirements met")).toBeVisible({
        timeout: UI_TIMEOUT,
      });
      await page
        .getByRole("button", { name: "Mark cycle ready to ship" })
        .click();
      await banner("Ready to ship");
    });

    await test.step("courier details recorded, all five panels dispatched", async () => {
      await page.getByRole("tab", { name: "Shipments" }).click();
      const shipmentRows = page.locator("tr", {
        has: page.locator('input[id^="courier-"]'),
      });
      await expect(shipmentRows.first()).toBeVisible({ timeout: UI_TIMEOUT });
      await expect(shipmentRows).toHaveCount(N);
      for (let i = 0; i < N; i++) {
        const shipmentRow = shipmentRows.nth(i);
        await shipmentRow.locator('input[id^="courier-"]').fill("E2E courier");
        await shipmentRow
          .getByRole("button", { name: "Save", exact: true })
          .click();
        // Saving creates the box; the row's box tag is the per-row signal
        // that this save landed (the toast lingers across rows).
        await expect(shipmentRow.getByText("READY TO SEND")).toBeVisible({
          timeout: UI_TIMEOUT,
        });
      }
      await page.locator('label[for="select-all-shipments"]').click();
      await page.getByRole("button", { name: `Mark ${N} shipped` }).click();
      await expect(
        page.getByText(`${N} participant shipments dispatched.`),
      ).toBeVisible({ timeout: UI_TIMEOUT });
      await banner("Shipped");
    });

    await test.step("marking every delivery received opens submissions automatically", async () => {
      // Reload before reading the receipt rows: every workbench tab panel
      // mounts with the page and ReceiptMonitor fetches on mount only, so
      // the rows it holds predate the dispatch that just happened on the
      // Shipments tab. A reload is what a provider would do, and it is the
      // only way this spec can read post-dispatch truth.
      await page.reload({ timeout: NAV_TIMEOUT });
      // The workbench renders behind a spinner until prep loads; the tabs do
      // not exist before then.
      await expect(page.getByRole("tab", { name: "Prep" })).toBeVisible({
        timeout: LONG_TIMEOUT,
      });
      await page.getByRole("tab", { name: "Receipts & scoring" }).click();
      await expect(page.getByText("In transit").first()).toBeVisible({
        timeout: UI_TIMEOUT,
      });
      // Row-scoped, one participant at a time: the success toast lingers
      // between clicks, so only the row's own status tag proves the delivery
      // landed before moving on.
      for (const name of seed.organizationNames) {
        const receiptRow = page.locator("tr", { hasText: name });
        await receiptRow.getByRole("button", { name: "Mark received" }).click();
        await expect(
          receiptRow.getByText("Delivered", { exact: true }),
        ).toBeVisible({ timeout: UI_TIMEOUT });
      }
      // The AUTO all-shipments-delivered walk — no manual override involved
      // (the override path is eqa-open-submissions.spec.ts's subject).
      await banner("Submissions open");
      await expect(
        page.getByRole("button", { name: "Open submissions" }),
      ).toBeHidden();
    });

    await test.step("scoring walks the banner to Scored", async () => {
      const cycleId = page.url().match(/cycles\/(\d+)\/workbench/)?.[1];
      if (!cycleId) {
        throw new Error(`No cycle id in workbench URL: ${page.url()}`);
      }
      seedReportedResults(cycleId, seed.organizationIds);
      await page.reload({ timeout: NAV_TIMEOUT });
      await expect(page.getByRole("tab", { name: "Prep" })).toBeVisible({
        timeout: LONG_TIMEOUT,
      });
      await page.getByRole("tab", { name: "Receipts & scoring" }).click();
      await page.getByRole("button", { name: "Score cycle" }).click();
      await expect(
        page.getByText(
          "Cycle scored. Unacceptable participants are in the follow-up register.",
        ),
      ).toBeVisible({ timeout: UI_TIMEOUT });
      await banner("Scored");
      // The planted outlier belongs to the first participant, so exactly one
      // of its three results is unacceptable and every other lab is clean.
      const outlierRow = page.locator("tr", {
        hasText: seed.organizationNames[0],
      });
      await expect(
        outlierRow.getByText(`1 unacceptable of ${RESULTS_PER_ORGANIZATION}`),
      ).toBeVisible();
      await expect(
        page
          .locator("tr", { hasText: seed.organizationNames[1] })
          .getByText(`0 unacceptable of ${RESULTS_PER_ORGANIZATION}`),
      ).toBeVisible();
    });

    await test.step("cycle history carries the manual create and system walks", async () => {
      await page.getByRole("button", { name: "Cycle history" }).click();
      await expect(page.getByText("Manual override").first()).toBeVisible({
        timeout: UI_TIMEOUT,
      });
      await expect(
        page.getByText("System", { exact: true }).first(),
      ).toBeVisible();
    });

    await test.step("the unacceptable participant reaches the follow-up register", async () => {
      // Scoring enqueues a follow-up per failing participant. The register is
      // reached by the monitor's own link, so the navigation is covered too.
      // This step leaves the workbench, so it runs last.
      await page.getByRole("link", { name: "Follow-up register" }).click();
      await expect(
        page.getByRole("heading", { name: "Participant follow-up" }),
      ).toBeVisible({ timeout: LONG_TIMEOUT });
      const registerRow = page.locator("tr", {
        hasText: seed.organizationNames[0],
      });
      await expect(registerRow).toBeVisible({ timeout: UI_TIMEOUT });
      await expect(registerRow.getByText("Notified")).toBeVisible();
      // Clean labs are correspondence-free: no row is opened for them.
      await expect(
        page.locator("tr", { hasText: seed.organizationNames[1] }),
      ).toHaveCount(0);
    });
  });
});
