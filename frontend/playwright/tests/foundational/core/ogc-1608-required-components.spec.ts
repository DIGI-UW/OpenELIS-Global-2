import { test, expect, Page } from "../../../helpers/test-base";
import { SiteInformationPage } from "../../../fixtures/esig-admin";
import { createSampleOrder } from "../../../helpers/seed-tat-data";
import { NAV_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";

/**
 * OGC-1608 — a result component can be marked Required in the test catalog. A
 * required component without a result keeps its test open: validating the
 * other parts validates them only, and the test is released once the required
 * part is resulted and validated. Optional parts without a result do not hold
 * the test.
 */

const API = "/api/OpenELIS-Global";
const CATALOG = `${API}/rest/test-catalog`;
const SERUM_SAMPLE_TYPE_ID = "2";

async function createTestWithRequiredInterpretation(
  page: Page,
): Promise<{ testId: string; name: string }> {
  await page.goto("/", { waitUntil: "domcontentloaded" });
  const stamp = Date.now().toString().slice(-8);
  const name = `OGC1608 Req ${stamp}`;
  const result = await page.evaluate(
    async ({ catalog, name, stamp, sampleTypeId }) => {
      const csrf = localStorage.getItem("CSRF") || "";
      const headers = {
        "Content-Type": "application/json",
        "X-CSRF-Token": csrf,
      };
      const labUnits = await (
        await fetch(`${catalog}/lab-units`, { credentials: "include", headers })
      ).json();
      const labUnitId = Array.isArray(labUnits) && labUnits[0]?.id;
      const created = await fetch(`${catalog}/tests`, {
        method: "POST",
        credentials: "include",
        headers,
        body: JSON.stringify({
          name,
          reportingName: name,
          code: `R1608${stamp}`,
          labUnitId,
          sampleTypeIds: [sampleTypeId],
          domain: "CLINICAL",
          orderable: true,
        }),
      });
      const createdText = await created.text();
      if (!created.ok) {
        return { error: `create ${created.status}: ${createdText}` };
      }
      const testId = JSON.parse(createdText).testId as string;
      const options = ["Positive", "Negative"].map((value, index) => ({
        value,
        sortOrder: index + 1,
        normal: index === 1,
      }));
      const configured = await fetch(
        `${catalog}/tests/${testId}/sample-results`,
        {
          method: "PUT",
          credentials: "include",
          headers,
          body: JSON.stringify({
            testId,
            components: [
              {
                code: "VALUE",
                label: "Value",
                displayOrder: 0,
                resultType: "N",
                isPrimary: true,
                showOnReport: true,
                interpretations: [],
                options: [],
              },
              {
                code: "INTERP",
                label: "Interpretation",
                displayOrder: 1,
                resultType: "D",
                isPrimary: false,
                showOnReport: true,
                isRequired: true,
                interpretations: [],
                options,
              },
              {
                code: "COMMENT",
                label: "Comment",
                displayOrder: 2,
                resultType: "N",
                isPrimary: false,
                showOnReport: true,
                interpretations: [],
                options: [],
              },
            ],
          }),
        },
      );
      if (!configured.ok) {
        return {
          error: `sample-results ${configured.status}: ${await configured.text()}`,
        };
      }
      const activated = await fetch(`${catalog}/tests/${testId}/activate`, {
        method: "POST",
        credentials: "include",
        headers,
        body: "{}",
      });
      if (!activated.ok) {
        return {
          error: `activate ${activated.status}: ${await activated.text()}`,
        };
      }
      return { testId };
    },
    { catalog: CATALOG, name, stamp, sampleTypeId: SERUM_SAMPLE_TYPE_ID },
  );
  expect(result.error, "test setup must succeed").toBeUndefined();
  return { testId: result.testId as string, name };
}

async function esigEnabled(page: Page): Promise<boolean> {
  const res = await page.request.get(`${API}/rest/esig/enabled`);
  const body = await res.json();
  return Boolean(body?.enabled);
}

test.describe("OGC-1608 required result components", () => {
  let esigWasOn = false;

  test.beforeEach(async ({ page }) => {
    esigWasOn = await esigEnabled(page);
    if (esigWasOn) {
      const siteInfo = new SiteInformationPage(page);
      await siteInfo.goto();
      await siteInfo.setBooleanSetting("electronicSignatureEnabled", false);
    }
  });

  test.afterEach(async ({ page }) => {
    if (esigWasOn) {
      const siteInfo = new SiteInformationPage(page);
      await siteInfo.goto();
      await siteInfo.setBooleanSetting("electronicSignatureEnabled", true);
    }
  });

  test("a required component without a result keeps its test open until it is resulted and validated", async ({
    page,
  }) => {
    test.setTimeout(150_000);
    const seeded = await createTestWithRequiredInterpretation(page);
    const main = page.getByRole("main");

    await test.step("The catalog shows which components are required", async () => {
      await page.goto(
        `/MasterListsPage/TestCatalogEditor/${seeded.testId}/sample-results`,
        { waitUntil: "domcontentloaded" },
      );
      await expect(main.locator("#comp-required-0")).toHaveAttribute(
        "aria-checked",
        "true",
        { timeout: NAV_TIMEOUT },
      );
      await expect(main.locator("#comp-required-1")).toHaveAttribute(
        "aria-checked",
        "true",
      );
      await expect(main.locator("#comp-required-2")).toHaveAttribute(
        "aria-checked",
        "false",
      );
    });

    const now = new Date();
    const pad = (n: number) => String(n).padStart(2, "0");
    const accessionNumber = await createSampleOrder(page, {
      labNo: "",
      receivedDate: `${now.getUTCFullYear()}-${pad(now.getUTCMonth() + 1)}-${pad(now.getUTCDate())}`,
      receivedTime: `${pad(now.getUTCHours())}:${pad(now.getUTCMinutes())}`,
      sampleTypeId: SERUM_SAMPLE_TYPE_ID,
      testIds: seeded.testId,
    });
    expect(accessionNumber, "the test must be orderable").toBeTruthy();
    const resultsRow = (component: string) =>
      main
        .locator("tr", { hasText: accessionNumber })
        .filter({ hasText: component })
        .first();
    const openValidation = async () => {
      await page.goto(
        `/validation?type=order&accessionNumber=${encodeURIComponent(accessionNumber)}`,
        { waitUntil: "domcontentloaded" },
      );
    };

    await test.step("Result the Value only", async () => {
      await page.goto(
        `/Results?accessionNumber=${encodeURIComponent(accessionNumber)}`,
        { waitUntil: "domcontentloaded" },
      );
      const row = resultsRow("Value");
      await expect(row).toBeVisible({ timeout: NAV_TIMEOUT });
      await row.getByRole("textbox").fill("42");
      await row.getByRole("button", { name: /^save$/i }).click();
      await expect(row.getByRole("button", { name: /^edit$/i })).toBeVisible({
        timeout: UI_TIMEOUT,
      });
    });

    await test.step("Validating the Value leaves the test open", async () => {
      await openValidation();
      await main.getByTestId("review-row-0").click({ timeout: NAV_TIMEOUT });
      await main.getByRole("button", { name: "Validate & release" }).click();
      await expect(
        page.getByText(
          "Component validated. The test is released once its other components are resulted and validated.",
        ),
      ).toBeVisible({ timeout: UI_TIMEOUT });
      await expect(
        main.getByText("There are no records to display"),
      ).toBeVisible({ timeout: UI_TIMEOUT });
    });

    await test.step("The required Interpretation can still be resulted", async () => {
      await page.goto(
        `/Results?accessionNumber=${encodeURIComponent(accessionNumber)}`,
        { waitUntil: "domcontentloaded" },
      );
      const row = resultsRow("Interpretation");
      await expect(row).toBeVisible({ timeout: NAV_TIMEOUT });
      await row.locator("select").selectOption({ label: "Positive" });
      await row.getByRole("button", { name: /^save$/i }).click();
      await expect(row.getByRole("button", { name: /^edit$/i })).toBeVisible({
        timeout: UI_TIMEOUT,
      });
    });

    await test.step("Validating the Interpretation releases the test", async () => {
      await openValidation();
      await main.getByTestId("review-row-0").click({ timeout: NAV_TIMEOUT });
      await main.getByRole("button", { name: "Validate & release" }).click();
      await expect(
        page.getByText("Result validated and released."),
      ).toBeVisible({ timeout: UI_TIMEOUT });
    });
  });
});
