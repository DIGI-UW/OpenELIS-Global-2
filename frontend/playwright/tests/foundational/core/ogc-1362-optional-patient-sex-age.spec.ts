import { Page } from "@playwright/test";
import { test, expect } from "../../../helpers/test-base";
import { isSettingOn, setSetting } from "../../../fixtures/esig-admin";
import { createSampleOrder } from "../../../helpers/seed-tat-data";
import { NAV_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";

/**
 * OGC-1362 — reception may leave the patient's sex blank when the site allows
 * it, and a test whose only ranges are for men or for women then shows why no
 * range was applied, on Results Entry and in Validation, instead of borrowing
 * the other sex's range. With the default settings the sex stays required.
 */

const API = "/api/OpenELIS-Global";
const CATALOG = `${API}/rest/test-catalog`;
const SERUM_SAMPLE_TYPE_ID = "2";
const SEX_SETTING = "Patient sex required";
const UNIFIED_ROUTE_SETTING = "resultsEntryUnifiedRoute";
const REASON = "Reference range not applied: patient sex not recorded";

async function createSexSpecificTest(page: Page): Promise<string> {
  await page.goto("/", { waitUntil: "domcontentloaded" });
  const stamp = Date.now().toString().slice(-8);
  const name = `OGC1362 Sex ${stamp}`;
  const result = await page.evaluate(
    async ({ catalog, name, stamp, sampleTypeId }) => {
      const headers = {
        "Content-Type": "application/json",
        "X-CSRF-Token": localStorage.getItem("CSRF") || "",
      };
      const call = async (path: string, method: string, body: unknown) => {
        const res = await fetch(`${catalog}${path}`, {
          method,
          credentials: "include",
          headers,
          body: body === undefined ? undefined : JSON.stringify(body),
        });
        const text = await res.text();
        return { ok: res.ok, status: res.status, text };
      };
      const labUnits = JSON.parse(
        (await call("/lab-units", "GET", undefined)).text,
      );
      const created = await call("/tests", "POST", {
        name,
        reportingName: name,
        code: `T1362${stamp}`,
        labUnitId: Array.isArray(labUnits) && labUnits[0]?.id,
        sampleTypeIds: [sampleTypeId],
        domain: "CLINICAL",
        orderable: true,
      });
      if (!created.ok) {
        return { error: `create ${created.status}: ${created.text}` };
      }
      const testId = JSON.parse(created.text).testId as string;
      const configured = await call(`/tests/${testId}/sample-results`, "PUT", {
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
      });
      if (!configured.ok) {
        return { error: `sample-results ${configured.status}` };
      }
      const componentId = JSON.parse(configured.text).components[0].id;
      const range = (gender: string, low: number, high: number) => ({
        componentId,
        gender,
        minAge: 0,
        maxAge: null,
        lowNormal: low,
        highNormal: high,
        lowValid: 0,
        highValid: 1000,
      });
      const ranges = await call(`/tests/${testId}/ranges`, "PUT", {
        testId,
        ranges: [range("M", 13, 17), range("F", 12, 15)],
      });
      if (!ranges.ok) {
        return { error: `ranges ${ranges.status}: ${ranges.text}` };
      }
      const activated = await call(`/tests/${testId}/activate`, "POST", {});
      if (!activated.ok) {
        return { error: `activate ${activated.status}: ${activated.text}` };
      }
      return { testId };
    },
    { catalog: CATALOG, name, stamp, sampleTypeId: SERUM_SAMPLE_TYPE_ID },
  );
  expect(result.error, "sex-specific test setup must succeed").toBeUndefined();
  return result.testId as string;
}

/** A referring site and provider that exist on whichever stack runs the spec. */
async function existingSiteAndProvider(
  page: Page,
): Promise<{ referringSiteId?: string; providerPersonId?: string }> {
  const response = await page.request.get(`${API}/rest/SamplePatientEntry`);
  const form = await response.json();
  const items = form?.sampleOrderItems || {};
  return {
    referringSiteId: items.referringSiteList?.[0]?.id,
    providerPersonId: items.providersList?.[0]?.id,
  };
}

test.describe("OGC-1362 optional patient sex", () => {
  let sexWasRequired = true;
  let unifiedWasOn = false;

  test.beforeEach(async ({ page }) => {
    sexWasRequired = await isSettingOn(page, SEX_SETTING);
    unifiedWasOn = await isSettingOn(page, UNIFIED_ROUTE_SETTING);
  });

  test.afterEach(async ({ page }) => {
    if ((await isSettingOn(page, SEX_SETTING)) !== sexWasRequired) {
      await setSetting(page, SEX_SETTING, sexWasRequired);
    }
    if ((await isSettingOn(page, UNIFIED_ROUTE_SETTING)) !== unifiedWasOn) {
      await setSetting(page, UNIFIED_ROUTE_SETTING, unifiedWasOn);
    }
  });

  test("sex stays marked required on a new patient by default", async ({
    page,
  }) => {
    if (!sexWasRequired) {
      await setSetting(page, SEX_SETTING, true);
    }
    await page.goto("/SamplePatientEntry", { waitUntil: "domcontentloaded" });
    await page.getByRole("button", { name: "New Patient" }).click();
    await expect(
      page.locator("fieldset legend").filter({ hasText: /^Sex/ }),
    ).toContainText("*", { timeout: NAV_TIMEOUT });
  });

  test("a result for a patient with no recorded sex says why no range applies", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    if (sexWasRequired) {
      await setSetting(page, SEX_SETTING, false);
    }
    if (unifiedWasOn) {
      await setSetting(page, UNIFIED_ROUTE_SETTING, false);
    }
    const testId = await createSexSpecificTest(page);
    const now = new Date();
    const pad = (n: number) => String(n).padStart(2, "0");
    const accession = await createSampleOrder(page, {
      labNo: "",
      receivedDate: `${now.getUTCFullYear()}-${pad(now.getUTCMonth() + 1)}-${pad(now.getUTCDate())}`,
      receivedTime: `${pad(now.getUTCHours())}:${pad(now.getUTCMinutes())}`,
      sampleTypeId: SERUM_SAMPLE_TYPE_ID,
      testIds: testId,
      ...(await existingSiteAndProvider(page)),
      patient: { gender: "" },
    });
    expect(accession, "order without a patient sex must save").toBeTruthy();

    await page.goto(
      `/result?type=order&accessionNumber=${encodeURIComponent(accession)}`,
      { waitUntil: "domcontentloaded" },
    );
    const entryRow = page
      .getByRole("main")
      .getByRole("row")
      .filter({ hasText: "OGC1362 Sex" })
      .first();
    await expect(entryRow).toContainText(REASON, { timeout: NAV_TIMEOUT });
    await entryRow.locator('input[id^="ResultValue"]').fill("14");
    const saved = page.waitForResponse(
      (response) =>
        response.url().includes("/rest/LogbookResults") &&
        response.request().method() === "POST",
    );
    await page
      .getByRole("button", { name: "Save", exact: true })
      .last()
      .click();
    await saved;

    await page.goto(
      `/validation?type=order&accessionNumber=${encodeURIComponent(accession)}`,
      { waitUntil: "domcontentloaded" },
    );
    await expect(page.getByText(REASON).first()).toBeVisible({
      timeout: NAV_TIMEOUT,
    });
    await expect(page.getByText("Range not applied").first()).toBeVisible({
      timeout: UI_TIMEOUT,
    });
    await expect(
      page.getByRole("button", { name: /Needs review \(1\)/ }),
    ).toBeVisible({ timeout: UI_TIMEOUT });
  });
});
