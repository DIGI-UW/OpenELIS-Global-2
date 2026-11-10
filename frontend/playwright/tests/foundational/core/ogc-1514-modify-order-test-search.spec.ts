import {
  test,
  expect,
  type Locator,
  type Page,
} from "../../../helpers/test-base";
import { createSampleOrder } from "../../../helpers/seed-tat-data";
import {
  LONG_TIMEOUT,
  NAV_TIMEOUT,
  UI_TIMEOUT,
} from "../../../helpers/timeouts";

/**
 * OGC-1514 — Modify Order offered the tests that can be added to an existing
 * sample as one checkbox table for the whole order, five rows per page, with
 * no search. Adding a test to an existing sample now works like a new sample
 * on Enter Order: a search box per sample that filters by name as the user
 * types, the chosen tests as removable tags, the sample type's panels, and the
 * full alphabetical list. Tests already live on the sample are not offered
 * again, so a test cannot be ordered twice on the same specimen.
 *
 * Run with:
 *   cd frontend && npm run pw:test:core-foundational -- ogc-1514
 */

const API = "/api/OpenELIS-Global";
const SERUM_SAMPLE_TYPE_ID = "2";

interface OrderableTest {
  id: string;
  name: string;
}

interface Panel {
  id: string;
  name: string;
  testIds: string;
}

async function serumCatalog(
  page: Page,
): Promise<{ tests: OrderableTest[]; panels: Panel[] }> {
  const res = await page.request.get(
    `${API}/rest/sample-type-tests?sampleType=${SERUM_SAMPLE_TYPE_ID}`,
  );
  expect(res.status(), "sample-type test list must load").toBe(200);
  const body = (await res.json()) as {
    tests?: OrderableTest[];
    panels?: Panel[];
  };
  const tests = body.tests ?? [];
  expect(
    tests.length,
    "Serum needs at least two orderable tests",
  ).toBeGreaterThan(1);
  return { tests, panels: (body.panels ?? []).filter((p) => p.name !== "") };
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

async function seedSerumOrder(page: Page, testIds: string[]): Promise<string> {
  const now = new Date();
  const pad = (n: number) => String(n).padStart(2, "0");
  const accessionNumber = await createSampleOrder(page, {
    labNo: "",
    receivedDate: `${now.getUTCFullYear()}-${pad(now.getUTCMonth() + 1)}-${pad(now.getUTCDate())}`,
    receivedTime: `${pad(now.getUTCHours())}:${pad(now.getUTCMinutes())}`,
    sampleTypeId: SERUM_SAMPLE_TYPE_ID,
    testIds: testIds.join(","),
    ...(await orderParties(page)),
  });
  expect(accessionNumber, "the seeded order must save").toBeTruthy();
  return accessionNumber;
}

/** Opens the order on Modify Order and moves to the Add Sample step. */
async function openSampleStep(page: Page, accessionNumber: string) {
  await page.goto(
    `/ModifyOrder?accessionNumber=${encodeURIComponent(accessionNumber)}`,
    { waitUntil: "domcontentloaded" },
  );
  await expect(
    page.getByRole("button", {
      name: `Accession Number : ${accessionNumber}`,
    }),
  ).toBeVisible({ timeout: NAV_TIMEOUT });
  await page.getByRole("button", { name: "Next", exact: true }).click();
  await expect(page.getByRole("table", { name: "Current Tests" })).toBeVisible({
    timeout: UI_TIMEOUT,
  });
}

/** The available-tests block of the order's first (Serum) sample. */
function serumSampleBlock(page: Page, accessionNumber: string): Locator {
  return page
    .getByTestId("available-tests")
    .locator("[data-testid^='existing-sample-tests-']")
    .filter({
      has: page.getByRole("heading", {
        name: `${accessionNumber}-1 (Serum)`,
      }),
    });
}

async function submitOrder(page: Page) {
  await page.getByRole("button", { name: "Next", exact: true }).click();
  const submit = page.getByRole("button", { name: "Submit", exact: true });
  await expect(submit).toBeEnabled({ timeout: UI_TIMEOUT });
  await submit.click();
  await expect(
    page.getByText("Sample Order Entry has been saved successfully"),
  ).toBeVisible({ timeout: LONG_TIMEOUT });
}

test.describe("OGC-1514 Modify Order test search on an existing sample", () => {
  test("a test found by search is added to the existing sample", async ({
    page,
  }) => {
    test.setTimeout(150_000);
    const { tests } = await serumCatalog(page);
    const [ordered, toAdd] = tests;
    const accessionNumber = await seedSerumOrder(page, [ordered.id]);

    await test.step("Open the order's Add Sample step", async () => {
      await page.goto("/SampleEdit", { waitUntil: "domcontentloaded" });
      const search = page.getByRole("textbox", {
        name: /enter accession number/i,
      });
      await expect(search).toBeVisible({ timeout: NAV_TIMEOUT });
      await search.fill(accessionNumber);
      await page.getByRole("button", { name: "Submit", exact: true }).click();
      await expect(page).toHaveURL(/\/ModifyOrder\?accessionNumber=/, {
        timeout: LONG_TIMEOUT,
      });
      await page.getByRole("button", { name: "Next", exact: true }).click();
      await expect(
        page.getByRole("table", { name: "Current Tests" }),
      ).toBeVisible({ timeout: UI_TIMEOUT });
    });

    const block = serumSampleBlock(page, accessionNumber);
    const search = block.getByPlaceholder("Choose Available Test");

    await test.step("The sample has a search box, no paged table, and no live test on offer", async () => {
      await expect(block).toBeVisible({ timeout: UI_TIMEOUT });
      await expect(search).toBeVisible();
      await expect(
        page.getByRole("table", { name: "Available Tests" }),
      ).toHaveCount(0);
      await expect(
        page.getByTestId("available-tests").locator(".cds--pagination"),
      ).toHaveCount(0);
      await expect(block.getByLabel(ordered.name, { exact: true })).toHaveCount(
        0,
      );
      await expect(block.getByLabel(toAdd.name, { exact: true })).toBeVisible();
    });

    await test.step("Typing filters by name; nothing matching says so", async () => {
      await search.fill("zzzz-no-such-test");
      await expect(block.getByText("No test Found Matching")).toBeVisible();
      await search.fill(toAdd.name.slice(0, 4));
      await expect(
        block.getByRole("menuitem", { name: toAdd.name, exact: true }),
      ).toBeVisible();
    });

    await test.step("Picking the match shows it as a tag and ticks it", async () => {
      await block
        .getByRole("menuitem", { name: toAdd.name, exact: true })
        .click();
      await expect(search).toHaveValue("");
      await expect(
        block.locator(".cds--tag").filter({ hasText: toAdd.name }),
      ).toBeVisible();
      await expect(block.getByLabel(toAdd.name, { exact: true })).toBeChecked();
    });

    await test.step("Submitting adds the test to the sample", async () => {
      await submitOrder(page);
      await openSampleStep(page, accessionNumber);
      const currentTests = page.getByRole("table", { name: "Current Tests" });
      await expect(currentTests.getByRole("row")).toHaveCount(3, {
        timeout: UI_TIMEOUT,
      });
      await expect(
        currentTests.getByRole("row").filter({ hasText: toAdd.name }),
      ).toHaveCount(1);
      const after = serumSampleBlock(page, accessionNumber);
      await expect(after).toBeVisible({ timeout: UI_TIMEOUT });
      await expect(after.getByLabel(toAdd.name, { exact: true })).toHaveCount(
        0,
      );
    });
  });

  test("a tag's close button drops the test before saving", async ({
    page,
  }) => {
    test.setTimeout(120_000);
    const { tests } = await serumCatalog(page);
    const [ordered, picked] = tests;
    const accessionNumber = await seedSerumOrder(page, [ordered.id]);
    await openSampleStep(page, accessionNumber);
    const block = serumSampleBlock(page, accessionNumber);
    await expect(block).toBeVisible({ timeout: UI_TIMEOUT });

    // Carbon hides the checkbox input; its label text is the click target.
    await block.getByText(picked.name, { exact: true }).click();
    const tag = block.locator(".cds--tag").filter({ hasText: picked.name });
    await expect(tag).toBeVisible();

    await tag.getByRole("button").click();

    await expect(tag).toHaveCount(0);
    await expect(
      block.getByLabel(picked.name, { exact: true }),
    ).not.toBeChecked();

    await submitOrder(page);
    await openSampleStep(page, accessionNumber);
    await expect(
      page.getByRole("table", { name: "Current Tests" }).getByRole("row"),
    ).toHaveCount(2, { timeout: UI_TIMEOUT });
  });

  test("a panel found by search adds its tests to the existing sample", async ({
    page,
  }) => {
    test.setTimeout(150_000);
    const { tests, panels } = await serumCatalog(page);
    const orderable = new Set(tests.map((t) => t.id));
    const panel = panels.find((p) =>
      p.testIds.split(",").every((id) => orderable.has(id.trim())),
    );
    test.skip(!panel, "Serum has no panel whose tests are all orderable");
    const panelTestIds = panel!.testIds.split(",").map((id) => id.trim());
    const panelTests = tests.filter((t) => panelTestIds.includes(t.id));
    const outside = tests.find((t) => !panelTestIds.includes(t.id));
    test.skip(!outside, "Serum has no test outside the panel to order first");
    const accessionNumber = await seedSerumOrder(page, [outside!.id]);
    await openSampleStep(page, accessionNumber);
    const block = serumSampleBlock(page, accessionNumber);
    await expect(block).toBeVisible({ timeout: UI_TIMEOUT });

    await test.step("Search the panel and pick it", async () => {
      const panelSearch = block.getByPlaceholder("Choose Available panel");
      await expect(panelSearch).toBeVisible({ timeout: UI_TIMEOUT });
      await panelSearch.fill(panel!.name.slice(0, 4));
      await block
        .getByRole("menuitem", { name: panel!.name, exact: true })
        .click();
      await expect(
        block.locator(".cds--tag").filter({ hasText: panel!.name }),
      ).toBeVisible();
      for (const t of panelTests) {
        await expect(block.getByLabel(t.name, { exact: true })).toBeChecked();
      }
    });

    await test.step("Back then Next keeps the panel chosen", async () => {
      // The Back button carries no id; it is the step's tertiary button.
      await page
        .locator(".navigationButtonsLayout .cds--btn--tertiary")
        .click();
      await expect(
        page.getByRole("button", {
          name: `Accession Number : ${accessionNumber}`,
        }),
      ).toBeVisible({ timeout: UI_TIMEOUT });
      await page.getByRole("button", { name: "Next", exact: true }).click();
      const again = serumSampleBlock(page, accessionNumber);
      await expect(again).toBeVisible({ timeout: UI_TIMEOUT });
      await expect(
        again.locator(".cds--tag").filter({ hasText: panel!.name }),
      ).toBeVisible();
      await expect(
        again.getByLabel(panel!.name, { exact: true }),
      ).toBeChecked();
      for (const t of panelTests) {
        await expect(again.getByLabel(t.name, { exact: true })).toBeChecked();
      }
    });

    await test.step("Submitting adds every panel test", async () => {
      await submitOrder(page);
      await openSampleStep(page, accessionNumber);
      const currentTests = page.getByRole("table", { name: "Current Tests" });
      // Current Tests pages at five rows; widen it before counting.
      await page
        .getByRole("combobox", { name: "Items per page" })
        .first()
        .selectOption("30");
      await expect(currentTests.getByRole("row")).toHaveCount(
        2 + panelTests.length,
        { timeout: UI_TIMEOUT },
      );
      for (const t of panelTests) {
        await expect(
          currentTests.getByRole("row").filter({ hasText: t.name }),
        ).toHaveCount(1);
      }
    });
  });
});
