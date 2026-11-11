import { test, expect, Page } from "../../../helpers/test-base";
import { NAV_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";

/**
 * OGC-1148 — QC Targets in the Test Catalog editor, and LOD/LOQ on a result
 * component. A quality officer enters a control level's expected value once;
 * the catalog keeps it, the prefill read resolves it, and the component's
 * detection limits round-trip as structured numbers.
 */

const API = "/api/OpenELIS-Global";
const CATALOG = `${API}/rest/test-catalog`;
const SERUM_SAMPLE_TYPE_ID = "2";

async function createNumericTest(page: Page): Promise<string> {
  await page.goto("/", { waitUntil: "domcontentloaded" });
  const stamp = Date.now().toString().slice(-8);
  const name = `OGC1148 Glucose ${stamp}`;
  const result = await page.evaluate(
    async ({ catalog, name, stamp, sampleTypeId }) => {
      const csrf = localStorage.getItem("CSRF") || "";
      const headers = {
        "Content-Type": "application/json",
        "X-CSRF-Token": csrf,
      };
      const labUnitsRes = await fetch(`${catalog}/lab-units`, {
        credentials: "include",
        headers,
      });
      const labUnits = await labUnitsRes.json();
      const labUnitId = Array.isArray(labUnits) && labUnits[0]?.id;
      const created = await fetch(`${catalog}/tests`, {
        method: "POST",
        credentials: "include",
        headers,
        body: JSON.stringify({
          name,
          reportingName: name,
          code: `T1148${stamp}`,
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
          }),
        },
      );
      if (!configured.ok) {
        return {
          error: `sample-results ${configured.status}: ${await configured.text()}`,
        };
      }
      return { testId };
    },
    { catalog: CATALOG, name, stamp, sampleTypeId: SERUM_SAMPLE_TYPE_ID },
  );
  expect(result.error, "test setup must succeed").toBeUndefined();
  return result.testId as string;
}

test.describe("OGC-1148 QC targets and detection limits", () => {
  test("a level target is entered once, kept, and resolved as the prefill", async ({
    page,
  }) => {
    test.setTimeout(120_000);
    const testId = await createNumericTest(page);

    await test.step("QC Targets sits in the editor's section nav right after Ranges", async () => {
      await page.goto(
        `/MasterListsPage/TestCatalogEditor/${testId}/qc-targets`,
        { waitUntil: "domcontentloaded" },
      );
      await expect(page.getByTestId("qc-targets-section")).toBeVisible({
        timeout: NAV_TIMEOUT,
      });
      const ranges = page.locator('[data-cy="section-ranges"]');
      const qcTargets = page.locator('[data-cy="section-qc-targets"]');
      await expect(qcTargets).toBeVisible();
      // The side nav can render its items more than once (rail + expanded), so
      // the order is judged on the distinct sequence.
      const order = await page
        .locator('[data-cy^="section-"]')
        .evaluateAll((items) =>
          Array.from(
            new Set(items.map((item) => item.getAttribute("data-cy"))),
          ),
        );
      expect(order.indexOf("section-qc-targets")).toBe(
        order.indexOf("section-ranges") + 1,
      );
      await expect(ranges).toBeVisible();
      await expect(page.getByTestId("qc-target-empty-NORMAL")).toContainText(
        /no target configured/i,
      );
    });

    await test.step("Entering the Normal level target and saving", async () => {
      await page.getByTestId("qc-target-add-NORMAL").click();
      await page.getByTestId("qc-target-NORMAL-expected").fill("5.5");
      await page.getByTestId("qc-target-NORMAL-uncertainty").fill("0.4");
      await page.getByTestId("qc-targets-save").click();
      await expect(page.getByText("QC targets saved.").first()).toBeAttached({
        timeout: UI_TIMEOUT,
      });
      await expect(page.getByTestId("qc-target-summary-NORMAL")).toHaveText(
        "5.5",
        { timeout: UI_TIMEOUT },
      );
    });

    await test.step("The target survives a reload and the prefill read resolves it", async () => {
      await page.reload({ waitUntil: "domcontentloaded" });
      await expect(page.getByTestId("qc-target-summary-NORMAL")).toHaveText(
        "5.5",
        { timeout: NAV_TIMEOUT },
      );
      await expect(page.getByTestId("qc-target-row-NORMAL")).toContainText(
        "± 0.4",
      );

      const effective = await page.evaluate(
        async ({ catalog, testId }) => {
          const res = await fetch(
            `${catalog}/tests/${testId}/qc-targets/effective?controlLevel=NORMAL`,
            { credentials: "include" },
          );
          return { status: res.status, body: await res.json() };
        },
        { catalog: CATALOG, testId },
      );
      expect(effective.status).toBe(200);
      expect(effective.body.source).toBe("LEVEL");
      expect(Number(effective.body.target.expectedValue)).toBe(5.5);
      expect(Number(effective.body.target.uncertainty)).toBe(0.4);
    });

    await test.step("Deactivating hides the target but keeps the row", async () => {
      await page.getByTestId("qc-target-toggle-NORMAL").click();
      await page.getByTestId("qc-targets-save").click();
      await expect(page.getByText("QC targets saved.").first()).toBeAttached({
        timeout: UI_TIMEOUT,
      });
      await expect(page.getByTestId("qc-target-empty-NORMAL")).toBeVisible({
        timeout: UI_TIMEOUT,
      });
      // Carbon's toggle label sits over the switch, so the click goes to the label.
      await page.locator('label[for="qc-targets-show-deactivated"]').click();
      await expect(page.getByTestId("qc-target-summary-NORMAL")).toHaveText(
        "5.5",
      );
    });
  });

  test("LOD and LOQ are kept on the component and an inverted pair is refused", async ({
    page,
  }) => {
    test.setTimeout(120_000);
    const testId = await createNumericTest(page);
    await page.goto(
      `/MasterListsPage/TestCatalogEditor/${testId}/sample-results`,
      { waitUntil: "domcontentloaded" },
    );
    const lod = page.locator("#comp-lod-0");
    const loq = page.locator("#comp-loq-0");
    await expect(lod).toBeVisible({ timeout: NAV_TIMEOUT });

    await test.step("An inverted pair is flagged and not saved", async () => {
      await lod.fill("0.5");
      await loq.fill("0.2");
      await expect(
        page.getByText("LOD cannot be greater than LOQ.").first(),
      ).toBeVisible();
      await page
        .getByRole("main")
        .getByRole("button", { name: "Save", exact: true })
        .click();
      const stored = await page.evaluate(
        async ({ catalog, testId }) => {
          const res = await fetch(`${catalog}/tests/${testId}/sample-results`, {
            credentials: "include",
          });
          return (await res.json()).components[0];
        },
        { catalog: CATALOG, testId },
      );
      expect(stored.lod ?? null).toBeNull();
    });

    await test.step("A coherent pair round-trips", async () => {
      await lod.fill("0.1");
      await loq.fill("0.3");
      await page
        .getByRole("main")
        .getByRole("button", { name: "Save", exact: true })
        .click();
      await expect(
        page
          .getByText(/sample & results saved|components saved|saved/i)
          .first(),
      ).toBeAttached({ timeout: UI_TIMEOUT });
      await page.reload({ waitUntil: "domcontentloaded" });
      await expect(page.locator("#comp-lod-0")).toHaveValue("0.1", {
        timeout: NAV_TIMEOUT,
      });
      await expect(page.locator("#comp-loq-0")).toHaveValue("0.3");
      const stored = await page.evaluate(
        async ({ catalog, testId }) => {
          const res = await fetch(`${catalog}/tests/${testId}/sample-results`, {
            credentials: "include",
          });
          return (await res.json()).components[0];
        },
        { catalog: CATALOG, testId },
      );
      expect(Number(stored.lod)).toBe(0.1);
      expect(Number(stored.loq)).toBe(0.3);
    });
  });
});
