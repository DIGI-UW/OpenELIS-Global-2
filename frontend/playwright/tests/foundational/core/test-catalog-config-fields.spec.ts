import { test, expect, Page } from "../../../helpers/test-base";
import { NAV_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";

/**
 * The fields the retired Test Modify page offered and the Test Catalogue
 * editor now carries: the SOP holding time, In Lab Only and Notify Patient on
 * Basic Info, and the lab-unit filter on the test list. The Test Management
 * page opens its editors inside the app rather than reloading it.
 */

const API = "/api/OpenELIS-Global/rest/test-catalog";

async function csrf(page: Page) {
  return page.evaluate(() => localStorage.getItem("CSRF") || "");
}

async function firstActiveClinicalTestId(page: Page) {
  const list = await page.request.get(
    `${API}/tests?domain=CLINICAL&status=active&page=1&pageSize=1`,
  );
  const body = await list.json();
  expect(body.rows.length, "an active clinical test to edit").toBe(1);
  return body.rows[0].testId as string;
}

test.describe("Test Catalogue: holding time, report flags and lab-unit filter", () => {
  test("a Test Management tile opens its editor without reloading the app", async ({
    page,
  }) => {
    await page.goto("/MasterListsPage/testManagementConfigMenu", {
      waitUntil: "domcontentloaded",
    });
    const tile = page.locator("#UnitsOfMeasure");
    await expect(tile).toBeVisible({ timeout: NAV_TIMEOUT });
    await page.evaluate(() => {
      (window as unknown as { stillLoaded: boolean }).stillLoaded = true;
    });

    await tile.click();

    await expect(page).toHaveURL(/\/MasterListsPage\/UnitsOfMeasure$/);
    await expect(page.getByRole("button", { name: "Add Unit" })).toBeVisible({
      timeout: UI_TIMEOUT,
    });
    expect(
      await page.evaluate(
        () => (window as unknown as { stillLoaded?: boolean }).stillLoaded,
      ),
    ).toBe(true);
  });

  test("holding time and the report flags save, reload and refuse a fractional time", async ({
    page,
  }) => {
    test.slow();
    await page.goto("/", { waitUntil: "domcontentloaded" });
    const token = await csrf(page);
    const testId = await firstActiveClinicalTestId(page);
    const original = await (
      await page.request.get(`${API}/tests/${testId}/basic-info`)
    ).json();

    try {
      await page.goto(
        `/MasterListsPage/TestCatalogEditor/${testId}/basic-info`,
        {
          waitUntil: "domcontentloaded",
        },
      );
      const holding = page.getByRole("textbox", {
        name: "SOP Max Holding Time (minutes)",
      });
      await expect(holding).toBeVisible({ timeout: NAV_TIMEOUT });
      const save = page.getByRole("button", { name: "Save", exact: true });

      await holding.fill("1.5");
      await expect(
        page.getByText("Enter a whole number of minutes, or leave blank."),
      ).toBeVisible();
      await expect(save).toBeDisabled();

      await holding.fill("95");
      const inLabOnly = page.locator("#basic-info-in-lab-only");
      const notify = page.locator("#basic-info-notify-results");
      const inLabBefore = await inLabOnly.getAttribute("aria-checked");
      const notifyBefore = await notify.getAttribute("aria-checked");
      await page.locator("label[for=basic-info-in-lab-only]").click();
      await page.locator("label[for=basic-info-notify-results]").click();
      const saved = page.waitForResponse(
        (r) =>
          r.url().includes(`/tests/${testId}/basic-info`) &&
          r.request().method() === "PUT",
      );
      await save.click();
      await saved;
      await expect(page.getByText("Basic info saved.").first()).toBeVisible({
        timeout: UI_TIMEOUT,
      });

      await page.reload({ waitUntil: "domcontentloaded" });
      await expect(holding).toHaveValue("95", { timeout: NAV_TIMEOUT });
      await expect(inLabOnly).not.toHaveAttribute(
        "aria-checked",
        inLabBefore || "false",
      );
      await expect(notify).not.toHaveAttribute(
        "aria-checked",
        notifyBefore || "false",
      );
    } finally {
      await page.request.put(`${API}/tests/${testId}/basic-info`, {
        headers: { "X-CSRF-Token": token },
        data: {
          timeHolding: original.timeHolding ?? "",
          inLabOnly: !!original.inLabOnly,
          notifyResults: !!original.notifyResults,
        },
      });
    }
  });

  test("the test list narrows to one lab unit and keeps it after a reload", async ({
    page,
  }) => {
    await page.goto("/", { waitUntil: "domcontentloaded" });
    const units = await (await page.request.get(`${API}/lab-units`)).json();
    let unit: { id: string; name: string } | undefined;
    let expected = 0;
    for (const candidate of units) {
      const page1 = await (
        await page.request.get(
          `${API}/tests?labUnit=${candidate.id}&page=1&pageSize=1`,
        )
      ).json();
      if (page1.total > 0) {
        unit = candidate;
        expected = page1.total;
        break;
      }
    }
    expect(unit, "a lab unit that has tests").toBeTruthy();

    await page.goto("/MasterListsPage/TestCatalogList", {
      waitUntil: "domcontentloaded",
    });
    const main = page.getByRole("main");
    await main.getByRole("button", { name: /^Filters/ }).click();
    await main.getByRole("combobox", { name: "Lab Unit" }).click();
    await page.getByRole("option", { name: unit!.name, exact: true }).click();

    await expect(page).toHaveURL(new RegExp(`labUnit=${unit!.id}`));
    await expect(main.locator("tbody tr")).toHaveCount(Math.min(expected, 25), {
      timeout: UI_TIMEOUT,
    });

    await page.reload({ waitUntil: "domcontentloaded" });
    await expect(page).toHaveURL(new RegExp(`labUnit=${unit!.id}`));
    await expect(main.getByRole("button", { name: "Filters (1)" })).toBeVisible(
      { timeout: NAV_TIMEOUT },
    );
  });
});
