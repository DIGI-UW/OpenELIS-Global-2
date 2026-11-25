import { test, expect, Page } from "../../../helpers/test-base";
import { NAV_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";

/**
 * OGC-1449, Enter Order (v4) test list for a sample:
 * - a late answer for the sample type chosen before replaced the list of the
 *   type chosen now, so a user on a slow link could tick tests that do not
 *   belong to the sample type shown (the earlier request is now cancelled, so
 *   the case waits for it to finish or fail, then for the page to go idle);
 * - a failed request left "Loading" on screen for good, with no error and no
 *   way to retry.
 */

const API = "/api/OpenELIS-Global";
const ENTER_ORDER = "/order/clinical/enter";
const TESTS_FOR_TYPE = /\/rest\/sample-type-tests\?sampleType=/;

type SampleType = { id: string; names: string[] };

async function twoSampleTypesWithDistinctTests(
  page: Page,
): Promise<[SampleType, SampleType]> {
  const types = await (
    await page.request.get(`${API}/rest/user-sample-types`)
  ).json();
  const found: SampleType[] = [];
  for (const type of types) {
    const catalog = await (
      await page.request.get(
        `${API}/rest/sample-type-tests?sampleType=${type.id}`,
      )
    ).json();
    const names = (catalog.tests || [])
      .map((t) => t.name)
      .filter(Boolean) as string[];
    if (
      names.length &&
      found.every((other) => names.some((n) => !other.names.includes(n)))
    ) {
      found.push({ id: String(type.id), names });
    }
    if (found.length === 2) {
      return [found[0], found[1]];
    }
  }
  throw new Error("Two sample types with distinct tests are needed");
}

test.describe("Enter Order tests loading (OGC-1449)", () => {
  test("a late answer for the previous sample type does not replace the current list", async ({
    page,
  }) => {
    const [first, second] = await twoSampleTypesWithDistinctTests(page);
    const onlyFirst = first.names.filter((n) => !second.names.includes(n));
    const onlySecond = second.names.filter((n) => !first.names.includes(n));

    await page.route(
      new RegExp(`sample-type-tests\\?sampleType=${first.id}$`),
      async (route) => {
        await new Promise((resolve) => setTimeout(resolve, 4000));
        await route.continue().catch(() => undefined);
      },
    );
    await page.goto(ENTER_ORDER, { timeout: NAV_TIMEOUT });
    const sampleType = page.locator("#sampleType-0");
    const section = page.getByTestId("order-sample-test-section");
    await expect(sampleType).toBeVisible({ timeout: NAV_TIMEOUT });

    const isFirst = (request) =>
      request.url().endsWith(`sampleType=${first.id}`);
    const lateRequestSettled = Promise.race([
      page.waitForEvent("requestfinished", { predicate: isFirst }),
      page.waitForEvent("requestfailed", { predicate: isFirst }),
    ]);
    await sampleType.selectOption(first.id);
    await sampleType.selectOption(second.id);
    await expect(section.getByText(onlySecond[0]).first()).toBeVisible({
      timeout: UI_TIMEOUT,
    });
    await lateRequestSettled;
    await page.evaluate(
      () =>
        new Promise((resolve) =>
          requestIdleCallback(() => resolve(undefined), { timeout: 1000 }),
        ),
    );

    await expect(sampleType).toHaveValue(second.id);
    await expect(section.getByText(onlySecond[0]).first()).toBeVisible();
    await expect(section.getByText(onlyFirst[0])).toHaveCount(0);
  });

  test("a failed request ends Loading with an error and a Retry that loads the tests", async ({
    page,
  }) => {
    const [first] = await twoSampleTypesWithDistinctTests(page);
    await page.route(TESTS_FOR_TYPE, (route) =>
      route.fulfill({
        status: 500,
        contentType: "application/json",
        body: '{"error":"OGC-1449"}',
      }),
    );
    await page.goto(ENTER_ORDER, { timeout: NAV_TIMEOUT });
    const sampleType = page.locator("#sampleType-0");
    const section = page.getByTestId("order-sample-test-section");
    await expect(sampleType).toBeVisible({ timeout: NAV_TIMEOUT });

    await sampleType.selectOption(first.id);

    await expect(section.getByRole("alert")).toHaveText(
      "The tests for this sample type could not be loaded.Retry",
      { timeout: 10_000 },
    );
    await expect(section.getByText(/^Loading/)).toHaveCount(0);
    await expect(sampleType).toHaveValue(first.id);

    await page.unroute(TESTS_FOR_TYPE);
    await section.getByRole("button", { name: "Retry" }).click();

    await expect(section.getByText(first.names[0]).first()).toBeVisible({
      timeout: UI_TIMEOUT,
    });
    await expect(
      section.getByText("The tests for this sample type could not be loaded."),
    ).toHaveCount(0);
  });
});
