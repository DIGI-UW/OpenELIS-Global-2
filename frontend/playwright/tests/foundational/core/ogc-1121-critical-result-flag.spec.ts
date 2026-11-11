import { Page } from "@playwright/test";
import { test, expect } from "../../../helpers/test-base";
import { isSettingOn, setSetting } from "../../../fixtures/esig-admin";
import { createSampleOrder } from "../../../helpers/seed-tat-data";
import { NAV_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";

/**
 * OGC-1121 — a result beyond the configured CRITICAL range looked exactly like
 * a merely abnormal one on the legacy Results Entry screen, and the Validation
 * queue showed no cue at all. A technologist must be able to tell 200 (critical)
 * from 120 (abnormal) at a glance, on both screens.
 *
 * Normal 5–100, Critical 2–150: the ticket's own setup.
 */

const API = "/api/OpenELIS-Global";
const CATALOG = `${API}/rest/test-catalog`;
const SERUM_SAMPLE_TYPE_ID = "2";
const ESIG_SETTING = "electronicSignatureEnabled";

interface CriticalTest {
  testId: string;
  name: string;
}

async function createTestWithCriticalRange(page: Page): Promise<CriticalTest> {
  await page.goto("/", { waitUntil: "domcontentloaded" });
  const stamp = Date.now().toString().slice(-8);
  const name = `OGC1121 Crit ${stamp}`;
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
          code: `T1121${stamp}`,
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
      const components = await configured.json();
      if (!configured.ok) {
        return { error: `sample-results ${configured.status}` };
      }
      const ranges = await fetch(`${catalog}/tests/${testId}/ranges`, {
        method: "PUT",
        credentials: "include",
        headers,
        body: JSON.stringify({
          testId,
          ranges: [
            {
              componentId: components.components[0].id,
              gender: null,
              minAge: 0,
              maxAge: null,
              lowNormal: 5,
              highNormal: 100,
              lowCritical: 2,
              highCritical: 150,
              lowValid: 0,
              highValid: 1000,
            },
          ],
        }),
      });
      if (!ranges.ok) {
        return { error: `ranges ${ranges.status}: ${await ranges.text()}` };
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
  expect(
    result.error,
    "critical-range test setup must succeed",
  ).toBeUndefined();
  return { testId: result.testId as string, name };
}

async function orderTest(page: Page, testId: string): Promise<string> {
  const now = new Date();
  const pad = (n: number) => String(n).padStart(2, "0");
  return createSampleOrder(page, {
    labNo: "",
    receivedDate: `${now.getUTCFullYear()}-${pad(now.getUTCMonth() + 1)}-${pad(now.getUTCDate())}`,
    receivedTime: `${pad(now.getUTCHours())}:${pad(now.getUTCMinutes())}`,
    sampleTypeId: SERUM_SAMPLE_TYPE_ID,
    testIds: testId,
  });
}

test.describe("OGC-1121 critical results look critical", () => {
  let esigWasOn = false;

  test.beforeEach(async ({ page }) => {
    // A signing ceremony is another spec's concern.
    esigWasOn = await isSettingOn(page, ESIG_SETTING);
    if (esigWasOn) {
      await setSetting(page, ESIG_SETTING, false);
    }
  });

  test.afterEach(async ({ page }) => {
    if (esigWasOn) {
      await setSetting(page, ESIG_SETTING, true);
    }
  });

  test("a critical value is marked at entry and in the validation queue, an abnormal one is not", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const critical = await createTestWithCriticalRange(page);
    const criticalAccession = await orderTest(page, critical.testId);
    const abnormalAccession = await orderTest(page, critical.testId);
    expect(
      criticalAccession && abnormalAccession,
      "orders must exist",
    ).toBeTruthy();

    const enterAndSave = async (
      accession: string,
      value: string,
      isCritical: boolean,
    ) => {
      await page.goto(
        `/Results?accessionNumber=${encodeURIComponent(accession)}`,
        { waitUntil: "domcontentloaded" },
      );
      const row = page
        .getByRole("main")
        .getByRole("row")
        .filter({ hasText: critical.name })
        .first();
      await expect(row).toBeVisible({ timeout: NAV_TIMEOUT });
      const input = row.locator('input[id^="unifiedResultValue-"]');
      await input.fill(value);
      await row.getByRole("button", { name: /^save$/i }).click();
      // OGC-1417: a critical value is acknowledged before it is saved; an
      // abnormal one goes straight through.
      if (isCritical) {
        await page
          .locator(".cds--modal.is-visible")
          .getByRole("button", { name: /Acknowledge and save/ })
          .click();
      }
      await expect(row.getByRole("button", { name: /^edit$/i })).toBeVisible({
        timeout: UI_TIMEOUT,
      });
    };

    await test.step("Enter 200 (critical) and 120 (abnormal) on the Results page", async () => {
      await enterAndSave(criticalAccession, "200", true);
      await enterAndSave(abnormalAccession, "120", false);
    });

    await test.step("Entry: the critical row carries a red Critical tag, the abnormal row a yellow Abnormal one", async () => {
      for (const [accession, expectation] of [
        [criticalAccession, "critical"],
        [abnormalAccession, "abnormal"],
      ] as const) {
        await page.goto(
          `/Results?accessionNumber=${encodeURIComponent(accession)}`,
          { waitUntil: "domcontentloaded" },
        );
        const row = page
          .getByRole("main")
          .getByRole("row")
          .filter({ hasText: critical.name })
          .first();
        await expect(row).toBeVisible({ timeout: NAV_TIMEOUT });
        await expect(row).toContainText(
          expectation === "critical" ? "200" : "120",
        );
        const criticalTag = row.locator('[data-testid="flag-CRITICAL"]');
        const abnormalTag = row.locator('[data-testid="flag-ABNORMAL"]');
        // Polled, not sampled once: Carbon transitions the tag background.
        const background = (tag: typeof criticalTag) => () =>
          tag.evaluate((el) => getComputedStyle(el).backgroundColor);
        if (expectation === "critical") {
          await expect(criticalTag).toBeVisible({ timeout: UI_TIMEOUT });
          await expect(criticalTag).toContainText("Critical");
          await expect(abnormalTag).toHaveCount(0);
          await expect
            .poll(background(criticalTag), { timeout: UI_TIMEOUT })
            .toBe("rgb(255, 241, 241)");
        } else {
          await expect(abnormalTag).toBeVisible({ timeout: UI_TIMEOUT });
          await expect(criticalTag).toHaveCount(0);
          await expect
            .poll(background(abnormalTag), { timeout: UI_TIMEOUT })
            .toBe("rgb(252, 244, 214)");
        }
      }
    });

    await test.step("Validation: the critical row is flagged in its own row, the abnormal row is marked abnormal", async () => {
      for (const [accession, flag] of [
        [criticalAccession, "CRITICAL"],
        [abnormalAccession, "ABNORMAL"],
      ] as const) {
        await page.goto("/validation?type=order", {
          waitUntil: "domcontentloaded",
        });
        const main = page.getByRole("main");
        const searchInput = main.locator("#validationSearch");
        await expect(searchInput).toBeVisible({ timeout: NAV_TIMEOUT });
        await searchInput.fill(accession);
        await main.getByTestId("validation-load").click();

        const cell = main
          .locator('[data-testid^="validation-result-"]')
          .first();
        await expect(cell).toBeVisible({ timeout: NAV_TIMEOUT });
        await expect(cell.locator(`[data-testid="flag-${flag}"]`)).toBeVisible({
          timeout: UI_TIMEOUT,
        });
        if (flag === "CRITICAL") {
          await expect(
            main.locator('[data-testid^="check-before-release-"]').first(),
          ).toContainText("Critical");
        } else {
          await expect(
            cell.locator('[data-testid="flag-CRITICAL"]'),
          ).toHaveCount(0);
        }
      }
    });
  });
});
