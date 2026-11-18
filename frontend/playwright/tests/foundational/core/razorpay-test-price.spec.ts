import { test, expect, type Page } from "../../../helpers/test-base";

/**
 * Test prices for online payment (Razorpay integration, step 3).
 *
 * Uses test 5 (Amylase(Serum)), the seeded reference test the other catalog
 * specs use. Every test leaves the price as it found it: the original is read
 * through the API before and written back after.
 */

const TEST_ID = "5";
const BASIC_INFO_API = `/api/OpenELIS-Global/rest/test-catalog/tests/${TEST_ID}/basic-info`;
const editorUrl = `/MasterListsPage/TestCatalogEditor/${TEST_ID}/basic-info`;

/** PUT through the page so the session cookie and CSRF token go with it. */
const putPrice = (page: Page, priceRupees: string) =>
  page.evaluate(
    async ({ url, priceRupees }) => {
      const res = await fetch(url, {
        method: "PUT",
        credentials: "include",
        headers: {
          "Content-Type": "application/json",
          "X-CSRF-Token": localStorage.getItem("CSRF") || "",
        },
        body: JSON.stringify({ priceRupees }),
      });
      return res.status;
    },
    { url: BASIC_INFO_API, priceRupees },
  );

test.describe("Test price (Razorpay)", () => {
  let originalPrice = "";

  test.beforeEach(async ({ page, request }) => {
    const res = await request.get(BASIC_INFO_API);
    expect(res.status()).toBe(200);
    originalPrice = (await res.json()).priceRupees || "";
    await page.goto(editorUrl, { waitUntil: "domcontentloaded" });
    await expect(page.locator("#basic-info-price")).toBeVisible();
  });

  test.afterEach(async ({ page }) => {
    expect(await putPrice(page, originalPrice)).toBe(200);
  });

  test("a price saved in the editor is still there after a reload", async ({
    page,
  }) => {
    const price = page.locator("#basic-info-price");
    await price.fill("123.45");
    // Sync on the save itself, then assert on what the user sees; a loose text
    // match could pass before the save lands and let the reload race it.
    const saved = page.waitForResponse(
      (r) =>
        r.url().endsWith(`/tests/${TEST_ID}/basic-info`) &&
        r.request().method() === "PUT",
    );
    await page.getByRole("button", { name: "Save", exact: true }).click();
    await saved;
    // The notification renders the message twice (toast and its live region).
    await expect(page.getByText("Basic info saved.").first()).toBeVisible();

    // The real proof: it reached the database, not just the form state.
    await page.reload({ waitUntil: "domcontentloaded" });
    await expect(page.locator("#basic-info-price")).toHaveValue("123.45");
  });

  test("a price that is not a plain amount is refused in the form", async ({
    page,
  }) => {
    await page.locator("#basic-info-price").fill("1e5");

    await expect(
      page.getByText(
        "Enter the amount in numbers only, for example 1250 or 1250.50.",
      ),
    ).toBeVisible();
    await expect(
      page.getByRole("button", { name: "Save", exact: true }),
    ).toBeDisabled();
  });

  test("the server refuses a bad price even when the form is bypassed", async ({
    page,
  }) => {
    expect(await putPrice(page, "1e5")).toBe(422);
    expect(await putPrice(page, "-5")).toBe(422);

    await page.reload({ waitUntil: "domcontentloaded" });
    await expect(page.locator("#basic-info-price")).toHaveValue(originalPrice);
  });
});
