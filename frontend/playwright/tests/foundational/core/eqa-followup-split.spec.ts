import { test, expect } from "../../../helpers/test-base";
import { UI_TIMEOUT, NAV_TIMEOUT } from "../../../helpers/timeouts";
import { seedFollowups, FollowupSeed } from "../../../helpers/seed-eqa-data";

/**
 * EQA follow-up: the split between the two registers, and triage on each
 * (OGC-613).
 *
 * One scored cycle carries two follow-up rows — one about this laboratory,
 * one about another participant. Which page a row appears on is decided by
 * nothing more than that: rows about this lab are the participant's own
 * corrective work and belong to the Follow-Up Queue, rows about other labs
 * are correspondence and belong to the provider register. Getting that
 * backwards would put another lab's failure into this lab's non-conformity
 * workflow, which is why both directions are asserted rather than one.
 *
 * Each page then gets its own triage action driven: the queue dismisses with
 * a category, the register records a response and resolves.
 */

const RUN = Date.now().toString(36);

let seed: FollowupSeed;

test.describe("EQA follow-up registers", () => {
  test.beforeAll(() => {
    seed = seedFollowups(RUN);
  });

  test.afterAll(() => {
    seed?.restore();
  });

  test("each register holds only its own rows, and triage closes them", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const foreignRow = () =>
      page.locator("tr", { hasText: seed.foreignOrganizationName });

    await test.step("the queue holds this lab's row and not the other lab's", async () => {
      await page.goto("/qa/eqa/follow-up-queue", { timeout: NAV_TIMEOUT });
      await expect(
        page.getByRole("heading", { name: "Follow-Up Queue" }),
      ).toBeVisible({ timeout: UI_TIMEOUT });
      for (const tile of [
        "kpi-queued",
        "kpi-questionable",
        "kpi-inhouse",
        "kpi-oldest",
      ]) {
        await expect(page.getByTestId(tile)).toBeVisible();
      }
      const queueRow = page.locator("tr", { hasText: seed.cycleName });
      await expect(queueRow).toBeVisible({ timeout: UI_TIMEOUT });
      await expect(queueRow.getByText(seed.analyteName)).toBeVisible();
      // An external scheme's failure is queued as questionable, from an
      // external provider.
      await expect(queueRow.getByText("Questionable")).toBeVisible();
      await expect(queueRow.getByText("External provider")).toBeVisible();
      // The other lab's row is correspondence, so it is not here at all.
      await expect(foreignRow()).toHaveCount(0);
    });

    await test.step("dismissing with a reason takes the row out of the queue", async () => {
      const queueRow = page.locator("tr", { hasText: seed.cycleName });
      await queueRow.getByRole("button", { name: "Triage" }).click();
      const triage = page.getByTestId(/^triage-/);
      await expect(triage).toBeVisible({ timeout: UI_TIMEOUT });
      // The expanded panel carries the per-result evidence the reviewer
      // decides on.
      await expect(triage.getByText("Unacceptable")).toBeVisible();
      await expect(triage.getByText("210.0")).toBeVisible();

      await triage.getByRole("button", { name: "Dismiss with reason" }).click();
      await expect(
        page.getByRole("heading", { name: "Dismiss with reason" }),
      ).toBeVisible({ timeout: UI_TIMEOUT });
      await page
        .locator("select#eqa-dismissal-category")
        .selectOption({ label: "Known equipment issue" });
      await page
        .locator("#eqa-dismissal-notes")
        .fill(`E2E ${RUN}: analyser fault already documented`);
      await page.getByRole("button", { name: "Dismiss", exact: true }).click();

      await expect(
        page
          .getByText(
            "Dismissed. The competency event for this category is recorded.",
          )
          .first(),
      ).toBeVisible({ timeout: UI_TIMEOUT });
      // The queue shows open rows only, so a dismissed row leaves it.
      await expect(page.locator("tr", { hasText: seed.cycleName })).toHaveCount(
        0,
      );
    });

    await test.step("the register holds the other lab's row and triages it", async () => {
      await page.goto("/qa/eqa/provider/follow-ups", { timeout: NAV_TIMEOUT });
      await expect(
        page.getByRole("heading", { name: "Participant follow-up" }),
      ).toBeVisible({ timeout: UI_TIMEOUT });
      await expect(foreignRow()).toBeVisible({ timeout: UI_TIMEOUT });
      await expect(foreignRow().getByText("Notified")).toBeVisible();

      // Triage lives in the expanded panel on this page too, alongside the
      // per-test evidence the correspondence is about.
      await foreignRow().getByRole("button", { name: "Triage" }).click();
      const triage = page.getByTestId(/^register-triage-/);
      await expect(triage).toBeVisible({ timeout: UI_TIMEOUT });
      await expect(triage.getByText("210.0")).toBeVisible();

      // Record response is a direct transition, no note required.
      await triage.getByRole("button", { name: "Record response" }).click();
      await expect(
        page.getByText("Follow-up moved to Response received.").first(),
      ).toBeVisible({ timeout: UI_TIMEOUT });
      await expect(foreignRow().getByText("Response received")).toBeVisible();

      // Resolving needs notes, and closes the row for further triage.
      await triage.getByRole("button", { name: "Resolve" }).click();
      await page
        .locator("#eqa-provider-followup-notes")
        .fill(`E2E ${RUN}: lab returned corrective action plan`);
      await page.getByRole("button", { name: "Confirm" }).click();
      await expect(
        page.getByText("Follow-up moved to Resolved.").first(),
      ).toBeVisible({ timeout: UI_TIMEOUT });
      // The register keeps resolved rows — unlike the queue — but offers no
      // further action on them.
      await expect(foreignRow().getByText("Resolved")).toBeVisible();
      await expect(
        page.getByTestId(/^register-triage-/).getByRole("button"),
      ).toHaveCount(0);
    });
  });
});
