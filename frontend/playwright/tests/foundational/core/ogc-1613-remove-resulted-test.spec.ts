import { test, expect, Page } from "../../../helpers/test-base";
import { createSampleOrder } from "../../../helpers/seed-tat-data";
import { NAV_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";

/**
 * OGC-1613 — removing a test that has results from an order asks for the
 * cancel reason, and the save then cancels that test (its results are kept).
 * Before, the save was refused with "cancelReason: A reason is required" and
 * nothing asked for one; a save with a reason left the test active.
 */

const API = "/api/OpenELIS-Global";
const CATALOG = `${API}/rest/test-catalog`;
const SERUM_SAMPLE_TYPE_ID = "2";

async function createNumericTest(
  page: Page,
  label: string,
): Promise<{ id: string; name: string }> {
  const stamp = Date.now().toString().slice(-8);
  const name = `OGC1613 ${label} ${stamp}`;
  const result = await page.evaluate(
    async ({ catalog, name, code, sampleTypeId }) => {
      const csrf = localStorage.getItem("CSRF") || "";
      const headers = {
        "Content-Type": "application/json",
        "X-CSRF-Token": csrf,
      };
      const labUnits = await (
        await fetch(`${catalog}/lab-units`, { credentials: "include", headers })
      ).json();
      const created = await fetch(`${catalog}/tests`, {
        method: "POST",
        credentials: "include",
        headers,
        body: JSON.stringify({
          name,
          reportingName: name,
          code,
          labUnitId: Array.isArray(labUnits) && labUnits[0]?.id,
          sampleTypeIds: [sampleTypeId],
          domain: "CLINICAL",
          orderable: true,
        }),
      });
      if (!created.ok) {
        return { error: `create ${created.status}: ${await created.text()}` };
      }
      const testId = (await created.json()).testId as string;
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
            ],
          }),
        },
      );
      if (!configured.ok) {
        return { error: `sample-results ${configured.status}` };
      }
      const activated = await fetch(`${catalog}/tests/${testId}/activate`, {
        method: "POST",
        credentials: "include",
        headers,
        body: "{}",
      });
      if (!activated.ok) {
        return { error: `activate ${activated.status}` };
      }
      return { testId };
    },
    {
      catalog: CATALOG,
      name,
      code: `C1613${label}${stamp}`.slice(0, 20),
      sampleTypeId: SERUM_SAMPLE_TYPE_ID,
    },
  );
  expect(result.error, "test setup must succeed").toBeUndefined();
  return { id: result.testId as string, name };
}

test.describe("OGC-1613 removing a resulted test", () => {
  test("asks for the reason and cancels the test, keeping its result", async ({
    page,
  }) => {
    test.setTimeout(150_000);
    await page.goto("/", { waitUntil: "domcontentloaded" });
    const resulted = await createNumericTest(page, "R");
    const kept = await createNumericTest(page, "K");
    const now = new Date();
    const pad = (n: number) => String(n).padStart(2, "0");
    const accessionNumber = await createSampleOrder(page, {
      labNo: "",
      receivedDate: `${now.getUTCFullYear()}-${pad(now.getUTCMonth() + 1)}-${pad(now.getUTCDate())}`,
      receivedTime: `${pad(now.getUTCHours())}:${pad(now.getUTCMinutes())}`,
      sampleTypeId: SERUM_SAMPLE_TYPE_ID,
      testIds: `${resulted.id},${kept.id}`,
    });
    expect(accessionNumber, "the order must be created").toBeTruthy();
    const main = page.getByRole("main");
    const resultedName = resulted.name;
    const keptName = kept.name;

    await test.step("Result one test", async () => {
      await page.goto(
        `/Results?accessionNumber=${encodeURIComponent(accessionNumber)}`,
        { waitUntil: "domcontentloaded" },
      );
      const row = main
        .locator("tr", { hasText: accessionNumber })
        .filter({ hasText: resultedName })
        .first();
      await expect(row).toBeVisible({ timeout: NAV_TIMEOUT });
      await row.getByRole("textbox").fill("42");
      await row.getByRole("button", { name: /^save$/i }).click();
      await expect(row.getByRole("button", { name: /^edit$/i })).toBeVisible({
        timeout: UI_TIMEOUT,
      });
    });

    const openEnterOrder = async () => {
      await page.goto(
        `/order/clinical/enter?labNumber=${encodeURIComponent(accessionNumber)}`,
        { waitUntil: "domcontentloaded" },
      );
      await expect(
        main.locator("label").filter({ hasText: resultedName }),
      ).toBeVisible({ timeout: NAV_TIMEOUT });
    };
    const dialog = page.getByRole("dialog", {
      name: "Remove a test that has results",
    });

    await test.step("Saving the order unchanged asks nothing", async () => {
      await openEnterOrder();
      await main.getByRole("button", { name: "Save and exit" }).click();
      await expect(page).toHaveURL(/\/order\/clinical\?highlight=/, {
        timeout: NAV_TIMEOUT,
      });
    });

    await test.step("Keeping the test saves nothing and ticks it again", async () => {
      await openEnterOrder();
      await main.locator("label").filter({ hasText: resultedName }).click();
      await main.getByRole("button", { name: "Save and exit" }).click();
      await expect(dialog).toBeVisible({ timeout: UI_TIMEOUT });
      await expect(
        dialog.getByRole("button", { name: /Remove test/ }),
      ).toBeDisabled();
      await dialog.getByRole("button", { name: "Keep the test" }).click();
      await expect(
        page.getByText("The test was kept and nothing was saved.").first(),
      ).toBeVisible({ timeout: UI_TIMEOUT });
      await expect(page.getByText("cancelReason:")).toHaveCount(0);
      await expect(
        main.getByRole("checkbox", { name: resultedName }),
      ).toBeChecked();
    });

    await test.step("With a reason the test is cancelled", async () => {
      await openEnterOrder();
      await main.locator("label").filter({ hasText: resultedName }).click();
      await main.getByRole("button", { name: "Save and exit" }).click();
      await expect(dialog).toBeVisible({ timeout: UI_TIMEOUT });
      await dialog
        .getByLabel("Reason for cancelling")
        .selectOption({ label: "Duplicate order" });
      await dialog.getByRole("button", { name: /Remove test/ }).click();
      await expect(page).toHaveURL(/\/order\/clinical\?highlight=/, {
        timeout: NAV_TIMEOUT,
      });
    });

    await test.step("Results and the order no longer list the test", async () => {
      await page.goto(
        `/Results?accessionNumber=${encodeURIComponent(accessionNumber)}`,
        { waitUntil: "domcontentloaded" },
      );
      await expect(
        main.locator("tr", { hasText: accessionNumber }).filter({
          hasText: keptName,
        }),
      ).toBeVisible({ timeout: NAV_TIMEOUT });
      await expect(
        main.locator("tr", { hasText: accessionNumber }).filter({
          hasText: resultedName,
        }),
      ).toHaveCount(0);

      await page.goto(
        `/order/clinical/enter?labNumber=${encodeURIComponent(accessionNumber)}`,
        { waitUntil: "domcontentloaded" },
      );
      await expect(main.getByRole("checkbox", { name: keptName })).toBeChecked({
        timeout: NAV_TIMEOUT,
      });
      await expect(
        main.getByRole("checkbox", { name: resultedName }),
      ).not.toBeChecked();
    });
  });
});
