import { test, expect } from "../../../helpers/test-base";
import { chooseCarbonOption } from "../../../helpers/carbon-select";
import {
  RBC_TEST,
  WHOLE_BLOOD,
  getJson,
  orderTests,
  referViaApi,
} from "../../../helpers/results-nce-ui";
import {
  LONG_TIMEOUT,
  NAV_TIMEOUT,
  UI_TIMEOUT,
} from "../../../helpers/timeouts";

/**
 * Lists and tables that come from site data are searched by typing (OGC-1515):
 * the settings pages, the electronic signature log and the shipment dashboard's
 * facility filter, whose unassigned-samples list asked the server for a route
 * it did not offer.
 */

type IdValue = { id: string; value: string };

test.describe("Site data is searched by typing", () => {
  test("a settings page narrows to the settings matching what is typed", async ({
    page,
  }) => {
    await page.goto("/MasterListsPage/SiteInformationMenu", {
      waitUntil: "domcontentloaded",
    });
    const search = page.getByRole("searchbox", { name: "Search settings" });
    const rows = page.getByRole("main").locator("tbody tr");
    await expect(rows.nth(1)).toBeVisible({ timeout: NAV_TIMEOUT });
    const all = await rows.count();

    await search.fill("CLOCK");
    await expect(rows).toHaveCount(1);
    await expect(rows.first()).toContainText(/clock/i);

    await search.fill("no-such-setting-1515");
    await expect(rows).toHaveCount(0);
    await expect(
      page.getByText('Nothing matches "no-such-setting-1515".'),
    ).toBeVisible();

    await search.fill("");
    await expect(rows).toHaveCount(Math.min(all, 30));
  });

  test("the e-signature log asks the server for the signatures matching the search", async ({
    page,
  }) => {
    await page.goto("/qa/qms/e-signature-log", {
      waitUntil: "domcontentloaded",
    });
    const search = page.getByRole("searchbox", { name: "Search the log" });
    await expect(search).toBeVisible({ timeout: NAV_TIMEOUT });

    const searched = page.waitForResponse(
      (response) =>
        response.url().includes("/rest/esig/log?") &&
        response.url().includes("text=no-such-signer-1515"),
      { timeout: LONG_TIMEOUT },
    );
    await search.fill("no-such-signer-1515");
    expect((await searched).status()).toBe(200);
    await expect(
      page.getByText('Nothing matches "no-such-signer-1515".'),
    ).toBeVisible({
      timeout: UI_TIMEOUT,
    });
    expect(new URL((await searched).url()).searchParams.get("page")).toBe("0");
  });

  test("the unassigned samples narrow to the facility typed", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const accession = await orderTests(page, WHOLE_BLOOD, [RBC_TEST]);
    const { referenceLab } = await referViaApi(page, accession, RBC_TEST);
    const labs = await getJson<IdValue[]>(
      page,
      "/rest/displayList/REFERRAL_ORGANIZATIONS",
    );
    const lab = labs.find((item) => item.value === referenceLab);
    expect(lab, `${referenceLab} is a referral laboratory`).toBeTruthy();

    await page.goto("/SampleShipment/unassigned", {
      waitUntil: "domcontentloaded",
    });
    const main = page.getByRole("main");
    await expect(main.getByText(accession)).toBeVisible({
      timeout: LONG_TIMEOUT,
    });

    const narrowed = page.waitForResponse(
      (response) =>
        response
          .url()
          .includes(`/rest/unassigned-sample/items/by-facility/${lab?.id}`),
      { timeout: LONG_TIMEOUT },
    );
    await chooseCarbonOption(
      main.getByRole("combobox", { name: "Filter by Facility" }),
      referenceLab,
    );
    expect((await narrowed).status()).toBe(200);
    await expect(main.getByText(accession)).toBeVisible({
      timeout: UI_TIMEOUT,
    });
    await expect(
      page.locator(".cds--toast-notification").filter({
        hasText: "Failed to fetch the unassigned samples",
      }),
    ).toHaveCount(0);
  });
});
