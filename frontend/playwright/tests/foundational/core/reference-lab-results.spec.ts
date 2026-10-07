import { test, expect, Page } from "../../../helpers/test-base";
import {
  chooseCarbonOption,
  tickCarbonMultiSelectOption,
} from "../../../helpers/carbon-select";
import {
  RBC_TEST,
  WHOLE_BLOOD,
  orderTests,
  referViaApi,
  serverToday,
  signIfAsked,
} from "../../../helpers/results-nce-ui";
import {
  LONG_TIMEOUT,
  NAV_TIMEOUT,
  UI_TIMEOUT,
} from "../../../helpers/timeouts";

/**
 * Reference Lab Results lists each referral by where it stands: a test just
 * referred out is Outstanding until a result comes back, and a result typed in
 * for it closes it into History as manually entered.
 *
 * Returned (a result the reference laboratory sent back, awaiting Accept or
 * Reject) is reached only through the laboratory's own FHIR task update, which
 * no screen or seed endpoint of this stack produces, so it is not exercised.
 */

const PAGE = "/SampleShipment/reference-lab-results";

/** The table each view renders once its referrals have loaded. */
const TABLE_TITLE: Record<string, string> = {
  outstanding: "Outstanding referrals",
  history: "History",
};

const viewTable = (page: Page, view: string) =>
  page
    .getByRole("main")
    .getByRole("heading", { name: TABLE_TITLE[view], exact: true });

const referralsLoad = (page: Page, view: string) =>
  page.waitForResponse(
    (response) =>
      response
        .url()
        .includes(`/rest/reference-lab-results/referrals?view=${view}`),
    { timeout: NAV_TIMEOUT },
  );

async function openView(page: Page, view: string) {
  const loaded = referralsLoad(page, view);
  await page.goto(`${PAGE}?view=${view}`, { waitUntil: "domcontentloaded" });
  await loaded;
  await expect(viewTable(page, view)).toBeVisible({ timeout: NAV_TIMEOUT });
}

const referralRow = (page: Page, accession: string) =>
  page.getByRole("main").getByRole("row").filter({ hasText: accession });

test.describe("Reference Lab Results", () => {
  test("a referral raised from Results is outstanding and follows the filters", async ({
    page,
  }) => {
    test.setTimeout(120_000);
    const accession = await orderTests(page, WHOLE_BLOOD, [RBC_TEST]);
    const { referenceLab } = await referViaApi(page, accession, RBC_TEST);
    const main = page.getByRole("main");
    const views = main.getByRole("radiogroup", {
      name: "Reference lab results view",
    });
    const row = referralRow(page, accession);

    await test.step("Outstanding lists the new referral", async () => {
      await openView(page, "outstanding");
      await expect(
        views.getByRole("radio", { name: /^Outstanding/ }),
      ).toHaveAttribute("aria-checked", "true");
      await expect(row).toBeVisible({ timeout: UI_TIMEOUT });
      await expect(row).toContainText("Red Blood Cells Count (RBC)");
      await expect(row).toContainText(referenceLab);
      await expect(row).toContainText("Sent — awaiting acceptance");
      await expect(row).toContainText("Routine");
    });

    await test.step("the reference lab and priority filters keep or drop it", async () => {
      await chooseCarbonOption(
        main.getByRole("combobox", { name: "Reference Lab" }),
        referenceLab,
      );
      await expect(row).toBeVisible();
      await tickCarbonMultiSelectOption(
        main.getByRole("combobox", { name: "Priority" }),
        "STAT (Urgent)",
      );
      await page.keyboard.press("Escape");
      await expect(row).toHaveCount(0);
      await main.getByRole("button", { name: "Clear filters" }).click();
      await expect(row).toBeVisible();
    });

    await test.step("History does not list an open referral", async () => {
      const history = referralsLoad(page, "history");
      await views.getByRole("radio", { name: "History" }).click();
      await history;
      await expect(viewTable(page, "history")).toBeVisible({
        timeout: UI_TIMEOUT,
      });
      await expect(
        views.getByRole("radio", { name: "History" }),
      ).toHaveAttribute("aria-checked", "true");
      await expect(row).toHaveCount(0);
    });
  });

  test("a result entered for an outstanding referral closes it into History", async ({
    page,
  }) => {
    test.setTimeout(120_000);
    const accession = await orderTests(page, WHOLE_BLOOD, [RBC_TEST]);
    const { referenceLab } = await referViaApi(page, accession, RBC_TEST);
    const { serverDate } = await serverToday(page);
    const main = page.getByRole("main");

    await test.step("Enter result opens the order's worklist", async () => {
      await openView(page, "outstanding");
      const outstanding = referralRow(page, accession);
      await expect(outstanding).toBeVisible({ timeout: UI_TIMEOUT });
      await outstanding.getByRole("link", { name: "Enter result" }).click();
      await expect(page).toHaveURL(
        new RegExp(`/Results\\?.*accessionNumber=${accession}`),
        { timeout: NAV_TIMEOUT },
      );
    });

    await test.step("save the reference lab's result and report date", async () => {
      const resultRow = main
        .getByRole("row")
        .filter({ hasText: accession })
        .filter({
          has: page.getByRole("button", { name: /^(Expand|Collapse) row$/ }),
        });
      await expect(resultRow).toBeVisible({ timeout: NAV_TIMEOUT });
      await expect(
        resultRow.getByText("Referred out", { exact: true }),
      ).toBeVisible();
      await resultRow.locator('input[id^="unifiedResultValue-"]').fill("4.5");
      await resultRow.getByRole("button", { name: "Expand row" }).click();
      await main
        .getByRole("textbox", { name: "Reference lab report date" })
        .fill(serverDate);
      await resultRow
        .getByRole("button", { name: "Save", exact: true })
        .click();
      await signIfAsked(page);
      await expect(
        resultRow.getByRole("button", { name: "Edit", exact: true }),
      ).toBeVisible({ timeout: LONG_TIMEOUT });
    });

    await test.step("the referral has left Outstanding for History", async () => {
      await openView(page, "outstanding");
      await expect(referralRow(page, accession)).toHaveCount(0);
      await openView(page, "history");
      const closed = referralRow(page, accession);
      await expect(closed).toBeVisible({ timeout: UI_TIMEOUT });
      await expect(closed).toContainText(referenceLab);
      await expect(closed).toContainText("Reconciled");
      await expect(closed).toContainText("Manually entered");
    });
  });
});
