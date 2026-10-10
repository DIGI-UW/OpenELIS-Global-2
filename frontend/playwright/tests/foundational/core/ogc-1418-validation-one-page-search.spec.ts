import { test, expect, Page } from "../../../helpers/test-base";
import {
  createSampleOrder,
  enterResults,
} from "../../../helpers/seed-tat-data";
import { NAV_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";

/**
 * OGC-1418 — Validation is one page with one search, like Results Entry.
 * However it is reached (the menu, a bare /validation or an old address), the
 * whole search area shows; a validator picks a Lab Unit and sees that unit's
 * queue; the search is kept in the address, so a reload reopens the same
 * queue; and a lab number typed into the box narrows the unit's queue.
 */

const CATALOG = "/api/OpenELIS-Global/rest/test-catalog";
const SERUM_SAMPLE_TYPE_ID = "2";

async function createTestInAValidatedLabUnit(page: Page): Promise<{
  testId: string;
  name: string;
  labUnitName: string;
}> {
  await page.goto("/", { waitUntil: "domcontentloaded" });
  const stamp = Date.now().toString().slice(-8);
  const name = `Validation 1418 ${stamp}`;
  const result = await page.evaluate(
    async ({ catalog, name, stamp, sampleTypeId }) => {
      const headers = {
        "Content-Type": "application/json",
        "X-CSRF-Token": localStorage.getItem("CSRF") || "",
      };
      const call = (url: string, method: string, body?: unknown) =>
        fetch(url, {
          method,
          credentials: "include",
          headers,
          body: body === undefined ? undefined : JSON.stringify(body),
        });
      const units = await (await call(`${catalog}/lab-units`, "GET")).json();
      const validated = await (
        await call(
          "/api/OpenELIS-Global/rest/user-test-sections/Validation",
          "GET",
        )
      ).json();
      const validatedIds = new Set(
        (Array.isArray(validated) ? validated : []).map((u) => String(u.id)),
      );
      const unit = (Array.isArray(units) ? units : []).find((u) =>
        validatedIds.has(String(u.id)),
      );
      const created = await call(`${catalog}/tests`, "POST", {
        name,
        reportingName: name,
        code: `V18${stamp}`,
        labUnitId: unit?.id,
        sampleTypeIds: [sampleTypeId],
        domain: "CLINICAL",
        orderable: true,
      });
      if (!created.ok) {
        return { error: `create ${created.status}` };
      }
      const testId = (await created.json()).testId as string;
      const configured = await call(
        `${catalog}/tests/${testId}/sample-results`,
        "PUT",
        {
          testId,
          components: [
            {
              code: "PRIMARY",
              label: name,
              displayOrder: 0,
              resultType: "N",
              isPrimary: true,
              showOnReport: true,
              interpretations: [],
              options: [],
            },
          ],
        },
      );
      if (!configured.ok) {
        return { error: `sample-results ${configured.status}` };
      }
      const activated = await call(
        `${catalog}/tests/${testId}/activate`,
        "POST",
        {},
      );
      if (!activated.ok) {
        return { error: `activate ${activated.status}` };
      }
      return { testId, labUnitName: unit?.name || "" };
    },
    { catalog: CATALOG, name, stamp, sampleTypeId: SERUM_SAMPLE_TYPE_ID },
  );
  expect(result.error, "test setup must succeed").toBeUndefined();
  return {
    testId: result.testId as string,
    name,
    labUnitName: result.labUnitName as string,
  };
}

async function orderAndResult(page: Page, testId: string): Promise<string> {
  const now = new Date();
  const pad = (n: number) => String(n).padStart(2, "0");
  const accession = await createSampleOrder(page, {
    labNo: "",
    receivedDate: `${now.getUTCFullYear()}-${pad(now.getUTCMonth() + 1)}-${pad(now.getUTCDate())}`,
    receivedTime: `${pad(now.getUTCHours())}:${pad(now.getUTCMinutes())}`,
    sampleTypeId: SERUM_SAMPLE_TYPE_ID,
    testIds: testId,
  });
  await enterResults(page, accession, "7");
  return accession;
}

test.describe("OGC-1418 — Validation one-page search", () => {
  test.describe.configure({ timeout: 240_000 });

  test("a bare /validation shows the whole search area, and a Lab Unit opens its queue that a reload keeps", async ({
    page,
  }) => {
    const created = await createTestInAValidatedLabUnit(page);
    const accession = await orderAndResult(page, created.testId);
    const main = page.getByRole("main");

    await test.step("the bare page shows every search control", async () => {
      await page.goto("/validation", { waitUntil: "domcontentloaded" });
      await expect(main.getByTestId("validation-search-area")).toBeVisible({
        timeout: NAV_TIMEOUT,
      });
      await expect(main.locator("#validationSearch")).toBeVisible();
      await expect(main.getByLabel("Lab Unit")).toBeVisible();
      await expect(main.locator("#validationFromDate")).toBeVisible();
      await expect(main.locator("#validationToDate")).toBeVisible();
      await expect(
        main.getByTestId("validation-search-by-patient"),
      ).toBeVisible();
    });

    await test.step("picking the Lab Unit opens that unit's queue", async () => {
      await main
        .getByLabel("Lab Unit")
        .selectOption({ label: created.labUnitName });
      await expect(main.getByText(accession).first()).toBeVisible({
        timeout: UI_TIMEOUT,
      });
      await expect(page).toHaveURL(/\/validation\?testSectionId=/);
    });

    await test.step("a reload reopens the same queue", async () => {
      await page.reload({ waitUntil: "domcontentloaded" });
      await expect(main.getByText(accession).first()).toBeVisible({
        timeout: NAV_TIMEOUT,
      });
    });

    await test.step("a lab number narrows the unit's queue and joins the address", async () => {
      const box = main.locator("#validationSearch");
      await box.fill(accession);
      await box.press("Enter");
      await expect(page).toHaveURL(new RegExp(`labNumber=${accession}`));
      await expect(page).toHaveURL(/testSectionId=/);
      await expect(main.getByText(accession).first()).toBeVisible({
        timeout: UI_TIMEOUT,
      });
    });
  });

  test("an old Validation address opens the one page with its filter", async ({
    page,
  }) => {
    const created = await createTestInAValidatedLabUnit(page);
    const accession = await orderAndResult(page, created.testId);

    await page.goto(
      `/AccessionValidation?accessionNumber=${encodeURIComponent(accession)}`,
      { waitUntil: "domcontentloaded" },
    );
    const main = page.getByRole("main");
    await expect(main.getByTestId("validation-search-area")).toBeVisible({
      timeout: NAV_TIMEOUT,
    });
    await expect(main.getByText(accession).first()).toBeVisible({
      timeout: UI_TIMEOUT,
    });
    await expect(page).toHaveURL(
      new RegExp(`/validation\\?labNumber=${accession}$`),
    );
    await expect(main.locator("#validationSearch")).toHaveValue(accession);
  });
});
