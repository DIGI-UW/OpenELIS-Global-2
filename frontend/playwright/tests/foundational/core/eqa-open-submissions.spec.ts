import { test, expect } from "../../../helpers/test-base";
import { SHORT_TIMEOUT } from "../../../helpers/timeouts";
import {
  seedShippedCycleWithPartialDelivery,
  ShippedCycleSeed,
} from "../../../helpers/seed-eqa-shipped-cycle";

/**
 * EQA T-46 — opening submissions on a partial roster (OGC-613).
 *
 * A SHIPPED cycle with one participant delivered and one dormant must offer
 * the provider an explicit, audited way to open submissions: the "Open
 * submissions" action on the Receipt Monitor, riding the generic transition
 * endpoint as a MANUAL override with a mandatory written reason.
 *
 * Seeded straight into the DB (see seed-eqa-shipped-cycle.ts); the assertions
 * are all on visible UI state per the Playwright guide.
 */

const RUN = Date.now().toString(36);

test.describe("EQA open submissions on a partial roster (T-46)", () => {
  let seed: ShippedCycleSeed;

  test.beforeAll(() => {
    seed = seedShippedCycleWithPartialDelivery(RUN);
  });

  test.afterAll(() => {
    seed?.restore();
  });

  test("a partially delivered cycle opens submissions through the audited manual action", async ({
    page,
  }) => {
    await page.goto(`/qa/eqa/provider/cycles/${seed.cycleId}/workbench`);

    // The cycle arrives parked in SHIPPED with a split roster.
    await expect(page.getByText("Shipped").first()).toBeVisible({
      timeout: SHORT_TIMEOUT,
    });
    await page.getByRole("tab", { name: "Receipts & scoring" }).click();
    const receipts = page.getByRole("tabpanel", { name: "Receipts & scoring" });
    await expect(
      receipts.getByRole("cell", { name: seed.deliveredOrgName }),
    ).toBeVisible();
    await expect(
      receipts.getByRole("cell", { name: seed.unshippedOrgName }),
    ).toBeVisible();
    await expect(
      receipts.getByText("Delivered", { exact: true }),
    ).toBeVisible();
    await expect(
      receipts.getByText("Not shipped", { exact: true }),
    ).toBeVisible();

    // The action is offered, and it refuses to fire without a written reason.
    const openButton = page.getByRole("button", { name: "Open submissions" });
    await expect(openButton).toBeVisible();
    await openButton.click();
    const dialog = page.getByRole("dialog", {
      name: "Open submissions with undelivered panels",
    });
    await expect(dialog).toBeVisible();
    await expect(
      dialog.getByRole("button", { name: "Open submissions" }),
    ).toBeDisabled();

    await dialog
      .getByRole("textbox", { name: "Reason" })
      .fill(`E2E ${RUN}: second lab dormant, first lab holds its panel`);
    await dialog.getByRole("button", { name: "Open submissions" }).click();

    // The cycle visibly advances and the action disappears with it.
    await expect(page.getByText("Submissions are open.")).toBeVisible();
    await expect(page.getByText("Submissions open").first()).toBeVisible();
    await expect(openButton).not.toBeVisible();

    // The override is on the audited timeline with its reason.
    await page.getByRole("button", { name: "Cycle history" }).click();
    await expect(page.getByText("Manual override").last()).toBeVisible();
    await expect(
      page.getByText(
        `E2E ${RUN}: second lab dormant, first lab holds its panel`,
      ),
    ).toBeVisible();
  });
});
