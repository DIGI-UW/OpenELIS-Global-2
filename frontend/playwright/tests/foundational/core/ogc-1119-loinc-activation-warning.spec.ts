import { test, expect, Page } from "../../../helpers/test-base";
import { NAV_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";

/**
 * OGC-1119 (FR-16/17/18) — a shared LOINC routes incoming results to only one
 * of the tests. The Terminology section already warns; activation, the moment
 * the collision starts to matter, must re-surface the same warning without
 * blocking. And a range can only constrain one of the test's own components.
 */

const API = "/api/OpenELIS-Global";
const CATALOG = `${API}/rest/test-catalog`;
const SERUM_SAMPLE_TYPE_ID = "2";

async function createTestWithLoinc(
  page: Page,
  name: string,
  code: string,
  loinc: string,
  activate: boolean,
): Promise<string> {
  const result = await page.evaluate(
    async ({ catalog, name, code, loinc, activate, sampleTypeId }) => {
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
          code,
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
        return { error: `sample-results ${configured.status}` };
      }
      // A test-level SAME_AS LOINC mapping is what feeds the test's LOINC.
      const saved = await fetch(`${catalog}/tests/${testId}/terminology`, {
        method: "PUT",
        credentials: "include",
        headers,
        body: JSON.stringify({
          testId,
          mappings: [{ source: "LOINC", code: loinc, relationship: "SAME_AS" }],
          components: [],
          sampleTypes: [],
        }),
      });
      if (!saved.ok) {
        return { error: `terminology ${saved.status}: ${await saved.text()}` };
      }
      if (activate) {
        const activated = await fetch(`${catalog}/tests/${testId}/activate`, {
          method: "POST",
          credentials: "include",
          headers,
          body: JSON.stringify({ gapsAcknowledged: "e2e OGC-1119 setup" }),
        });
        if (!activated.ok) {
          return {
            error: `activate ${activated.status}: ${await activated.text()}`,
          };
        }
      }
      return { testId };
    },
    {
      catalog: CATALOG,
      name,
      code,
      loinc,
      activate,
      sampleTypeId: SERUM_SAMPLE_TYPE_ID,
    },
  );
  expect(result.error, "test setup must succeed").toBeUndefined();
  return result.testId as string;
}

test.describe("OGC-1119 LOINC guardrails at activation", () => {
  test("activating a test that shares a LOINC names the other test, and still activates", async ({
    page,
  }) => {
    test.setTimeout(150_000);
    await page.goto("/", { waitUntil: "domcontentloaded" });
    const stamp = Date.now().toString().slice(-7);
    const loinc = `1119${stamp.slice(-4)}-9`;
    const firstName = `OGC1119 First ${stamp}`;
    await createTestWithLoinc(page, firstName, `T1119A${stamp}`, loinc, true);
    const second = await createTestWithLoinc(
      page,
      `OGC1119 Second ${stamp}`,
      `T1119B${stamp}`,
      loinc,
      false,
    );

    await page.goto(`/MasterListsPage/TestCatalogEditor/${second}/basic-info`, {
      waitUntil: "domcontentloaded",
    });
    const active = page.getByRole("switch", { name: /Active/ });
    await expect(active).toBeVisible({ timeout: NAV_TIMEOUT });
    await expect(active).not.toBeChecked();

    // Carbon's toggle label sits over the switch, so the click goes to the label.
    await page.locator('label[for="basic-info-active"]').click();
    // The coverage gate asks for an acknowledgment first when the test has no
    // ranges yet; either that modal or the warning itself is the next thing seen.
    const warning = page.getByTestId("activation-duplicate-loinc-warning");
    const ack = page.getByRole("button", { name: "Acknowledge and activate" });
    await expect(warning.or(ack)).toBeVisible({ timeout: UI_TIMEOUT });
    if (await ack.isVisible()) {
      await ack.click();
    }

    await expect(warning).toBeVisible({ timeout: UI_TIMEOUT });
    await expect(warning).toContainText(loinc);
    await expect(warning).toContainText(firstName);
    await expect(active).toBeChecked();
  });

  test("a range cannot be saved against a component of another test", async ({
    page,
  }) => {
    await page.goto("/", { waitUntil: "domcontentloaded" });
    const stamp = Date.now().toString().slice(-7);
    const testId = await createTestWithLoinc(
      page,
      `OGC1119 Range ${stamp}`,
      `T1119R${stamp}`,
      `1119${stamp.slice(-4)}-1`,
      false,
    );
    const outcome = await page.evaluate(
      async ({ catalog, testId }) => {
        const headers = {
          "Content-Type": "application/json",
          "X-CSRF-Token": localStorage.getItem("CSRF") || "",
        };
        const foreign = await fetch(`${catalog}/tests/${testId}/ranges`, {
          method: "PUT",
          credentials: "include",
          headers,
          body: JSON.stringify({
            testId,
            ranges: [
              {
                componentId: "00000000-0000-0000-0000-000000000000",
                gender: null,
                minAge: 0,
                maxAge: null,
                lowNormal: 1,
                highNormal: 2,
              },
            ],
          }),
        });
        const after = await fetch(`${catalog}/tests/${testId}/ranges`, {
          credentials: "include",
        });
        return { status: foreign.status, ranges: (await after.json()).ranges };
      },
      { catalog: CATALOG, testId },
    );
    expect(outcome.status).toBe(422);
    expect(outcome.ranges).toEqual([]);
  });
});
