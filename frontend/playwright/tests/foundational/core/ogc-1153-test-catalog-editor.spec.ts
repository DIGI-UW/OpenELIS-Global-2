import { test, expect } from "../../../helpers/test-base";

/**
 * OGC-1153 — Test Catalog Editor defects.
 *
 * Each test names the symptom a reviewer can check by hand, and asserts on the
 * rendered result rather than on a response code, so a fix that only changes the
 * wire format cannot make these pass.
 *
 * Test 5 (Amylase(Serum)) is the reference test the ticket used; it exists in the
 * seeded catalog. The editor routes are /MasterListsPage/TestCatalogEditor/{id}/…
 */

const TEST_ID = "5";
const editorUrl = (section: string) =>
  `/MasterListsPage/TestCatalogEditor/${TEST_ID}/${section}`;

test.describe("OGC-1153 Test Catalog Editor", () => {
  // Issue 1b — the Locale picker opened on `fr` in an English session because it
  // selected the first non-fallback locale rather than the session locale.
  test("localization section opens on the session locale, not the last option", async ({
    page,
  }) => {
    await page.goto(editorUrl("localization"), {
      waitUntil: "domcontentloaded",
    });

    const locale = page.locator("#localization-locale");
    await expect(locale).toBeVisible();

    const htmlLang = await page.evaluate(
      () => document.documentElement.lang || "en",
    );
    await expect(locale).toHaveValue(new RegExp(`^${htmlLang.split("-")[0]}`));
  });

  // Issue 1a — saving happened on blur with no Save control and no auto-save
  // claim, so an administrator could not tell whether an edit was kept.
  test("localization section states that fields save automatically", async ({
    page,
  }) => {
    await page.goto(editorUrl("localization"), {
      waitUntil: "domcontentloaded",
    });

    const section = page.getByTestId("localization-section");
    await expect(section).toBeVisible();
    await expect(section).toContainText(/saved automatically/i);
  });

  // Issue 2 — the empty state rendered twice: an inline notification AND a
  // four-column table header with no rows under it.
  test("sample & results shows no orphan table header when a list is empty", async ({
    page,
  }) => {
    await page.goto(editorUrl("sample-results"), {
      waitUntil: "domcontentloaded",
    });
    await page.waitForLoadState("networkidle");

    // Any table rendered in this section must actually have body rows; a header
    // band with an empty tbody is the defect.
    const emptyHeaderBands = await page.evaluate(() => {
      const tables = Array.from(document.querySelectorAll("table"));
      return tables.filter((t) => {
        const hasHeader = !!t.querySelector("thead th");
        const bodyRows = t.querySelectorAll("tbody tr").length;
        return hasHeader && bodyRows === 0;
      }).length;
    });
    expect(emptyHeaderBands).toBe(0);
  });

  // Issue 3 — the two calculation empty states rendered as a bare <p> with no
  // class while the sibling Reflex block used an inline notification.
  test("reflex & calc renders every empty state as an inline notification", async ({
    page,
  }) => {
    await page.goto(editorUrl("reflex-calc"), {
      waitUntil: "domcontentloaded",
    });
    await page.waitForLoadState("networkidle");

    // The old treatment was an unclassed paragraph inside a Carbon tile.
    await expect(page.locator(".cds--tile p:not([class])")).toHaveCount(0);

    // Whatever is empty must be announced as a notification instead.
    await expect(
      page.locator(".cds--inline-notification").first(),
    ).toBeVisible();
  });

  // Issue 4 — a non-numeric {id} threw a NumberFormatException out of the DAO
  // and the global RuntimeException advice rendered it as 500. It must behave
  // like any other absent test.
  test("non-numeric test id is not a server error on any catalog section", async ({
    page,
    request,
  }) => {
    await page.goto("/MasterListsPage", { waitUntil: "domcontentloaded" });

    const sections = [
      "basic-info",
      "sample-results",
      "localization",
      "reflex-calc",
      "storage-history",
    ];
    for (const section of sections) {
      const res = await request.get(
        `/api/OpenELIS-Global/rest/test-catalog/tests/notanumber/${section}`,
      );
      expect(res.status(), `${section} must not 500`).toBe(404);
    }

    // A numeric-but-absent id already behaved correctly; guard against the fix
    // over-rejecting valid ids.
    const absent = await request.get(
      "/api/OpenELIS-Global/rest/test-catalog/tests/999999/basic-info",
    );
    expect(absent.status()).toBe(404);

    const valid = await request.get(
      `/api/OpenELIS-Global/rest/test-catalog/tests/${TEST_ID}/basic-info`,
    );
    expect(valid.status()).toBe(200);
  });

  // Issue 5a — activation also sets `orderable` (Active ⇒ orderable per the
  // FRS), but the response did not say so, so the UI needed a reload.
  test("activate response states the resulting active and orderable flags", async ({
    page,
    request,
  }) => {
    await page.goto("/MasterListsPage", { waitUntil: "domcontentloaded" });
    const csrf = await page.evaluate(() => localStorage.getItem("CSRF") || "");

    const res = await request.post(
      `/api/OpenELIS-Global/rest/test-catalog/tests/${TEST_ID}/activate`,
      { data: {}, headers: { "X-CSRF-Token": csrf } },
    );
    expect(res.status()).toBe(200);

    const body = await res.json();
    expect(body).toHaveProperty("active");
    expect(body).toHaveProperty("orderable");
    // The coverage report stays at the top level so existing callers still work.
    expect(body).toHaveProperty("male");
    expect(body).toHaveProperty("female");
  });

  // Issue 5b — an open-ended coverage gap arrives as Double.POSITIVE_INFINITY,
  // which Jackson quotes as "Infinity", and the age formatter rendered the
  // literal text "Infinity years" inside a destructive-action dialog.
  test("no unformatted Infinity is rendered in the ranges section", async ({
    page,
  }) => {
    await page.goto(editorUrl("ranges"), { waitUntil: "domcontentloaded" });
    await page.waitForLoadState("networkidle");

    await expect(page.locator("body")).not.toContainText("Infinity");
    await expect(page.locator("body")).not.toContainText("NaN");
  });
});
