import { test, expect, type Page } from "../../../helpers/test-base";
import { createSampleOrder } from "../../../helpers/seed-tat-data";
import {
  LONG_TIMEOUT,
  NAV_TIMEOUT,
  UI_TIMEOUT,
} from "../../../helpers/timeouts";

/**
 * OGC-1543 — Modify Order showed its steps and Next before the order had
 * loaded. A sample type and test picked in that window were wiped when the
 * load landed: the screen still showed the tick, but Submit posted an empty
 * sampleXML or stayed disabled. The page now shows loading until the order is
 * in, and only then opens the wizard.
 *
 * The order load is held back by the test and released only after the loading
 * state has been checked, so the race is reproduced on every run.
 *
 * Run with:
 *   cd frontend && npm run pw:test:core-foundational -- ogc-1543-modify-order-slow-load
 */

const API = "/api/OpenELIS-Global";
const SERUM_SAMPLE_TYPE_ID = "2";

interface OrderableTest {
  id: string;
  name: string;
}

async function serumTests(page: Page): Promise<OrderableTest[]> {
  const res = await page.request.get(
    `${API}/rest/sample-type-tests?sampleType=${SERUM_SAMPLE_TYPE_ID}`,
  );
  expect(res.status(), "sample-type test list must load").toBe(200);
  const body = (await res.json()) as { tests?: OrderableTest[] };
  const tests = body.tests ?? [];
  expect(
    tests.length,
    "Serum needs at least two orderable tests",
  ).toBeGreaterThan(1);
  return tests;
}

async function orderParties(
  page: Page,
): Promise<{ referringSiteId?: string; providerPersonId?: string }> {
  const response = await page.request.get(`${API}/rest/SamplePatientEntry`);
  expect(response.status(), "load Add Order form").toBe(200);
  const form = (await response.json()) as {
    sampleOrderItems?: {
      referringSiteList?: { id: string }[];
      providersList?: { id: string }[];
    };
  };
  return {
    referringSiteId: form.sampleOrderItems?.referringSiteList?.[0]?.id,
    providerPersonId: form.sampleOrderItems?.providersList?.[0]?.id,
  };
}

async function seedSerumOrder(page: Page, testId: string): Promise<string> {
  const now = new Date();
  const pad = (n: number) => String(n).padStart(2, "0");
  const accessionNumber = await createSampleOrder(page, {
    labNo: "",
    receivedDate: `${now.getUTCFullYear()}-${pad(now.getUTCMonth() + 1)}-${pad(now.getUTCDate())}`,
    receivedTime: `${pad(now.getUTCHours())}:${pad(now.getUTCMinutes())}`,
    sampleTypeId: SERUM_SAMPLE_TYPE_ID,
    testIds: testId,
    ...(await orderParties(page)),
  });
  expect(accessionNumber, "the seeded order must save").toBeTruthy();
  return accessionNumber;
}

test.describe("OGC-1543 Modify Order on a slow order load", () => {
  test("nothing is editable until the order loads, and a test picked after it is saved", async ({
    page,
  }) => {
    test.setTimeout(150_000);
    const [ordered, toAdd] = await serumTests(page);
    const accessionNumber = await seedSerumOrder(page, ordered.id);

    let releaseLoad: (() => void) | undefined;
    const loadReleased = new Promise<void>((resolve) => {
      releaseLoad = resolve;
    });
    await page.route(/\/rest\/SampleEdit\?/, async (route) => {
      if (route.request().method() === "GET") {
        await loadReleased;
      }
      await route.continue();
    });

    await test.step("While the order loads, the page says so and offers no step", async () => {
      const loadRequested = page.waitForRequest(
        (r) => r.url().includes("/rest/SampleEdit?") && r.method() === "GET",
        { timeout: NAV_TIMEOUT },
      );
      await page.goto(
        `/ModifyOrder?accessionNumber=${encodeURIComponent(accessionNumber)}`,
        { waitUntil: "domcontentloaded" },
      );
      await loadRequested;
      await expect(
        page.locator('[data-cy="modify-order-loading"]'),
      ).toBeVisible({ timeout: UI_TIMEOUT });
      await expect(
        page.getByRole("button", { name: "Next", exact: true }),
      ).toHaveCount(0);
      await expect(page.locator("#sampleId_0")).toHaveCount(0);
    });

    await test.step("Once the order is in, the wizard opens", async () => {
      releaseLoad?.();
      await expect(
        page.getByRole("button", { name: "Next", exact: true }),
      ).toBeVisible({ timeout: LONG_TIMEOUT });
      await expect(
        page.locator('[data-cy="modify-order-loading"]'),
      ).toHaveCount(0);
      await page.getByRole("button", { name: "Next", exact: true }).click();
      await expect(
        page.getByRole("table", { name: "Current Tests" }),
      ).toBeVisible({ timeout: UI_TIMEOUT });
    });

    await test.step("A new sample's test reaches the saved sampleXML", async () => {
      const newSample = page
        .locator(".sampleType")
        .filter({ has: page.locator("#sampleId_0") });
      await page.locator("#sampleId_0").selectOption(SERUM_SAMPLE_TYPE_ID);
      await newSample.getByText(toAdd.name, { exact: true }).click();
      await expect(
        newSample.getByRole("checkbox", { name: toAdd.name, exact: true }),
      ).toBeChecked();

      await page.getByRole("button", { name: "Next", exact: true }).click();
      const submit = page.getByRole("button", { name: "Submit", exact: true });
      await expect(submit).toBeEnabled({ timeout: UI_TIMEOUT });
      const posted = page.waitForRequest(
        (r) => r.url().includes("/rest/SampleEdit") && r.method() === "POST",
        { timeout: LONG_TIMEOUT },
      );
      await submit.click();
      const body = JSON.parse((await posted).postData() ?? "{}") as {
        sampleXML?: string;
      };
      expect(body.sampleXML ?? "").toContain(`tests='${toAdd.id}'`);
      await expect(
        page.getByText("Sample Order Entry has been saved successfully"),
      ).toBeVisible({ timeout: LONG_TIMEOUT });
    });

    await test.step("The order now carries the added test on a second sample", async () => {
      await page.unroute(/\/rest\/SampleEdit\?/);
      await page.goto(
        `/ModifyOrder?accessionNumber=${encodeURIComponent(accessionNumber)}`,
        { waitUntil: "domcontentloaded" },
      );
      await page
        .getByRole("button", { name: "Next", exact: true })
        .click({ timeout: NAV_TIMEOUT });
      const currentTests = page.getByRole("table", { name: "Current Tests" });
      await expect(
        currentTests
          .getByRole("row")
          .filter({ hasText: `${accessionNumber}-2` }),
      ).toContainText(toAdd.name, { timeout: UI_TIMEOUT });
    });
  });
});
