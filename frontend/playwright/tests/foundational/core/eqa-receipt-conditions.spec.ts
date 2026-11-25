import { test, expect } from "../../../helpers/test-base";
import { UI_TIMEOUT, NAV_TIMEOUT } from "../../../helpers/timeouts";
import {
  seedReceiptConditions,
  ReceiptConditionsSeed,
} from "../../../helpers/seed-eqa-data";

/**
 * EQA receipt monitor: the two conditions a provider has to act on, neither
 * of which a healthy dispatch produces (OGC-613).
 *
 * Overdue is derived at read time from the expected delivery date and is the
 * provider's only prompt to chase a courier. Arrived damaged needs the
 * shipment delivered AND a panel receipt recording failed integrity — and
 * only the participant receipt endpoint writes the shipment link that joins
 * the two, so a receipt recorded through Add Order never reaches this page.
 * Both are read-side derivations over real rows, which is what makes them
 * worth driving in a browser rather than mocking.
 */

const RUN = Date.now().toString(36);

let seed: ReceiptConditionsSeed;

test.describe("EQA receipt conditions", () => {
  test.beforeAll(() => {
    seed = seedReceiptConditions(RUN);
  });

  test.afterAll(() => {
    seed?.restore();
  });

  test("an overdue shipment and a damaged arrival are both flagged", async ({
    page,
  }) => {
    test.setTimeout(180_000);

    await page.goto(`/qa/eqa/provider/cycles/${seed.cycleId}/workbench`, {
      timeout: NAV_TIMEOUT,
    });
    await page.getByRole("tab", { name: "Receipts & scoring" }).click();
    // Every workbench tab panel is mounted at once and the Shipments table
    // lists the same participants, so all row lookups are scoped to this
    // panel.
    const receipts = page.getByRole("tabpanel", {
      name: "Receipts & scoring",
    });
    await expect(
      receipts.getByText(
        "Submissions open on their own once every participant holds its panel. Overdue means two business days past the expected delivery.",
      ),
    ).toBeVisible({ timeout: UI_TIMEOUT });

    await test.step("the late shipment reads as overdue and still awaits receipt", async () => {
      const overdueRow = receipts.locator("tr", {
        hasText: seed.overdueOrganizationName,
      });
      await expect(overdueRow).toBeVisible({ timeout: UI_TIMEOUT });
      await expect(
        overdueRow.getByText("Overdue", { exact: true }),
      ).toBeVisible();
      // Nothing has arrived, so both the receipt action and a repeat remain.
      await expect(
        overdueRow.getByRole("button", { name: "Mark received" }),
      ).toBeVisible();
      await expect(
        overdueRow.getByRole("button", { name: "Send repeat" }),
      ).toBeVisible();
    });

    await test.step("the damaged arrival carries its condition and no receipt action", async () => {
      const damagedRow = receipts.locator("tr", {
        hasText: seed.damagedOrganizationName,
      });
      await expect(damagedRow).toBeVisible();
      await expect(damagedRow.getByText("Arrived damaged")).toBeVisible();
      await expect(
        damagedRow.getByText(`Damaged: ${seed.damageNotes}`),
      ).toBeVisible();
      // A damaged panel counts as delivered, so receipt is no longer offered
      // — a repeat is the remaining move.
      await expect(
        damagedRow.getByRole("button", { name: "Mark received" }),
      ).toHaveCount(0);
      await expect(
        damagedRow.getByRole("button", { name: "Send repeat" }),
      ).toBeVisible();
    });
  });
});
