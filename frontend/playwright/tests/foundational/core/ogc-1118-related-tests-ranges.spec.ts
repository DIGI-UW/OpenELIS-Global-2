import { test, expect, Page } from "../../../helpers/test-base";
import { NAV_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";

/**
 * OGC-1118 — "Edit related tests together" applies one reference-range set to
 * every selected sibling test. Each test must end up with a range on its OWN
 * result component (the set is seeded from the first test's component), and
 * every bound entered in the dialog, valid range included, must be written.
 *
 * Two sibling tests are created for the run (same name stem, two specimens) so
 * the seeded catalog is never changed.
 */

const API = "/api/OpenELIS-Global";
const CATALOG = `${API}/rest/test-catalog`;

interface Sibling {
  testId: string;
  componentId: string;
}

async function createSibling(
  page: Page,
  stem: string,
  suffix: string,
  sampleTypeId: string,
): Promise<Sibling> {
  const result = await page.evaluate(
    async ({ catalog, stem, suffix, sampleTypeId }) => {
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
          name: stem,
          reportingName: stem,
          // Siblings share the name stem; the description is unique per test.
          description: `${stem} ${suffix}`,
          code: `T1118${suffix}`,
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
                label: stem,
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
      const configuredBody = await configured.json();
      if (!configured.ok) {
        return { error: `sample-results ${configured.status}` };
      }
      const activated = await fetch(`${catalog}/tests/${testId}/activate`, {
        method: "POST",
        credentials: "include",
        headers,
        body: JSON.stringify({ gapsAcknowledged: "e2e OGC-1118 setup" }),
      });
      if (!activated.ok) {
        return {
          error: `activate ${activated.status}: ${await activated.text()}`,
        };
      }
      return { testId, componentId: configuredBody.components[0].id as string };
    },
    { catalog: CATALOG, stem, suffix, sampleTypeId },
  );
  expect(result.error, "sibling setup must succeed").toBeUndefined();
  return {
    testId: result.testId as string,
    componentId: result.componentId as string,
  };
}

async function rangesOf(page: Page, testId: string) {
  return page.evaluate(
    async ({ catalog, testId }) => {
      const res = await fetch(`${catalog}/tests/${testId}/ranges`, {
        credentials: "include",
      });
      return (await res.json()).ranges as {
        componentId: string | null;
        lowNormal: number | null;
        highNormal: number | null;
        lowCritical: number | null;
        highCritical: number | null;
        lowValid: number | null;
        highValid: number | null;
      }[];
    },
    { catalog: CATALOG, testId },
  );
}

test.describe("OGC-1118 related tests share one range set", () => {
  test("a range applied to related tests lands on each test's own component with every bound", async ({
    page,
  }) => {
    test.setTimeout(150_000);
    await page.goto("/", { waitUntil: "domcontentloaded" });
    const stamp = Date.now().toString().slice(-7);
    const stem = `OGC1118 Hb ${stamp}`;
    const serum = await createSibling(page, stem, `S${stamp}`, "2");
    const plasma = await createSibling(page, stem, `P${stamp}`, "3");
    expect(serum.componentId).not.toBe(plasma.componentId);

    await test.step("Open the combined editor from the seed test", async () => {
      await page.goto(
        `/MasterListsPage/TestCatalogEditor/${serum.testId}/basic-info`,
        { waitUntil: "domcontentloaded" },
      );
      await page.getByTestId("edit-related-tests").click();
      await expect(page).toHaveURL(/TestCatalogEditor\/group\//, {
        timeout: NAV_TIMEOUT,
      });
      await expect(
        page.getByRole("heading", { name: /Editing 2 tests/ }),
      ).toBeVisible({
        timeout: NAV_TIMEOUT,
      });
    });

    await test.step("Add a range with normal, critical and valid bounds and apply it to all", async () => {
      await page.getByRole("button", { name: "Add range" }).click();
      const dialog = page.getByRole("dialog");
      await dialog.getByRole("spinbutton", { name: "Normal low" }).fill("12");
      await dialog.getByRole("spinbutton", { name: "Normal high" }).fill("16");
      await dialog.getByRole("spinbutton", { name: "Critical low" }).fill("7");
      await dialog
        .getByRole("spinbutton", { name: "Critical high" })
        .fill("20");
      await dialog.getByRole("spinbutton", { name: "Valid low" }).fill("0");
      await dialog.getByRole("spinbutton", { name: "Valid high" }).fill("30");
      await dialog.getByRole("button", { name: "Save" }).click();

      await expect(page.getByTestId("group-range-valid-0")).toHaveText(
        "0 / 30",
      );
      await page
        .getByRole("button", { name: "Set all to these values" })
        .click();
      await expect(
        page.getByText(/ranges (saved|applied)/i).first(),
      ).toBeAttached({ timeout: UI_TIMEOUT });
    });

    await test.step("Each sibling holds the set on its own component, valid bounds included", async () => {
      for (const sibling of [serum, plasma]) {
        const ranges = await rangesOf(page, sibling.testId);
        expect(
          ranges,
          `test ${sibling.testId} has the shared range`,
        ).toHaveLength(1);
        expect(ranges[0].componentId).toBe(sibling.componentId);
        expect(ranges[0]).toMatchObject({
          lowNormal: 12,
          highNormal: 16,
          lowCritical: 7,
          highCritical: 20,
          lowValid: 0,
          highValid: 30,
        });
      }
    });
  });
});
