import { test, expect, Page } from "../../../helpers/test-base";
import { createSampleOrder } from "../../../helpers/seed-tat-data";
import { NAV_TIMEOUT } from "../../../helpers/timeouts";

/**
 * OGC-1614 — a result's Method list offers the test's linked active methods,
 * A to Z; a test with no linked method offers every active method, A to Z.
 * Before, every method (inactive ones too) was offered, in id order.
 */

const API = "/api/OpenELIS-Global";
const CATALOG = `${API}/rest/test-catalog`;
const SERUM_SAMPLE_TYPE_ID = "2";

interface Method {
  id: string;
  value: string;
}

async function createNumericTest(page: Page, label: string): Promise<string> {
  const stamp = Date.now().toString().slice(-8);
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
      name: `OGC1614 ${label} ${stamp}`,
      code: `M1614${label}${stamp}`.slice(0, 20),
      sampleTypeId: SERUM_SAMPLE_TYPE_ID,
    },
  );
  expect(result.error, "test setup must succeed").toBeUndefined();
  return result.testId as string;
}

async function getJson<T>(page: Page, path: string): Promise<T> {
  return page.evaluate(async (url) => {
    const res = await fetch(url, { credentials: "include" });
    return res.json();
  }, `${API}${path}`);
}

async function linkMethod(page: Page, testId: string, methodId: string) {
  const status = await page.evaluate(
    async ({ api, testId, methodId }) => {
      const res = await fetch(`${api}/rest/test/${testId}/methods`, {
        method: "POST",
        credentials: "include",
        headers: {
          "Content-Type": "application/json",
          "X-CSRF-Token": localStorage.getItem("CSRF") || "",
        },
        body: JSON.stringify({
          methodId,
          isDefault: false,
          effectiveDate: new Date().toISOString().slice(0, 10),
        }),
      });
      return res.status;
    },
    { api: API, testId, methodId },
  );
  expect(status, "linking a method must succeed").toBeLessThan(300);
}

const byName = (a: string, b: string) =>
  a.localeCompare(b, undefined, { sensitivity: "base" });

test.describe("OGC-1614 result method choices", () => {
  test("a result offers the test's linked active methods, A to Z", async ({
    page,
  }) => {
    test.setTimeout(150_000);
    await page.goto("/", { waitUntil: "domcontentloaded" });
    const linkedId = await createNumericTest(page, "L");
    const unlinkedId = await createNumericTest(page, "U");

    const active = (
      await getJson<{ methods: Method[] }>(
        page,
        `/rest/methods-for-test/${unlinkedId}`,
      )
    ).methods;
    expect(active.length, "the stack needs two active methods").toBeGreaterThan(
      1,
    );
    const [first, second] = [...active].sort((a, b) =>
      byName(b.value, a.value),
    );
    await linkMethod(page, linkedId, first.id);
    await linkMethod(page, linkedId, second.id);
    const inactive = await getJson<Method[]>(
      page,
      "/rest/displayList/METHODS_INACTIVE",
    );

    const now = new Date();
    const pad = (n: number) => String(n).padStart(2, "0");
    const accessionNumber = await createSampleOrder(page, {
      labNo: "",
      receivedDate: `${now.getUTCFullYear()}-${pad(now.getUTCMonth() + 1)}-${pad(now.getUTCDate())}`,
      receivedTime: `${pad(now.getUTCHours())}:${pad(now.getUTCMinutes())}`,
      sampleTypeId: SERUM_SAMPLE_TYPE_ID,
      testIds: `${linkedId},${unlinkedId}`,
    });
    expect(accessionNumber, "the order must be created").toBeTruthy();

    await page.goto(
      `/Results?accessionNumber=${encodeURIComponent(accessionNumber)}`,
      { waitUntil: "domcontentloaded" },
    );
    const main = page.getByRole("main");
    const methodSelect = async (name: RegExp, last = false) => {
      const row = main
        .locator("tr", { hasText: accessionNumber })
        .filter({ hasText: name })
        .first();
      await expect(row).toBeVisible({ timeout: NAV_TIMEOUT });
      await row
        .getByRole("button", { name: "Expand row", exact: true })
        .click();
      const selects = main.locator("select[id^='method-']");
      return last ? selects.last() : selects.first();
    };

    await test.step("The linked test offers its two methods, A to Z", async () => {
      const select = await methodSelect(/OGC1614 L \d+/);
      await expect(select.locator("option")).toHaveText(
        ["", second.value, first.value],
        { timeout: NAV_TIMEOUT },
      );
    });

    await test.step("A test with no link offers every active method, A to Z", async () => {
      const names = active.map((method) => method.value);
      expect(names).toEqual([...names].sort(byName));
      for (const method of inactive) {
        expect(names).not.toContain(method.value);
      }
      const select = await methodSelect(/OGC1614 U \d+/, true);
      await expect(select.locator("option")).toHaveText(["", ...names], {
        timeout: NAV_TIMEOUT,
      });
    });
  });

  test("a typed effective date links a method without the calendar", async ({
    page,
  }) => {
    await page.goto("/", { waitUntil: "domcontentloaded" });
    const testId = await createNumericTest(page, "D");
    const [method] = (
      await getJson<{ methods: Method[] }>(
        page,
        `/rest/methods-for-test/${testId}`,
      )
    ).methods;

    await page.goto(`/MasterListsPage/TestCatalogEditor/${testId}/methods`, {
      waitUntil: "domcontentloaded",
    });
    const main = page.getByRole("main");
    await main
      .getByRole("button", { name: "+ Link Method", exact: true })
      .first()
      .click({ timeout: NAV_TIMEOUT });
    const dialog = page.getByRole("dialog", { name: "Link Method" });
    await dialog.locator("#link-method-select").fill(method.value);
    await page.getByRole("option", { name: method.value, exact: true }).click();
    const confirm = dialog.getByRole("button", {
      name: "+ Link Method",
      exact: true,
    });
    await expect(confirm).toBeDisabled();

    await dialog.locator("#link-effective-date").fill("2026-11-10");
    await dialog.locator("#link-effective-date").press("Tab");
    await expect(confirm).toBeEnabled();
    await confirm.click();

    await expect(
      main.locator("tr", { hasText: method.value }).first(),
    ).toContainText("2026-11-10", { timeout: NAV_TIMEOUT });
  });
});
