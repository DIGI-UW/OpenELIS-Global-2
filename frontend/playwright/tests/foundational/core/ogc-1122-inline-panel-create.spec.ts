import { test, expect } from "../../../helpers/test-base";
import { NAV_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";

/**
 * OGC-1122 — the Panels section's inline "Create new panel" used to do nothing
 * (first a front-end no-op, then a backend 500), so the membership never
 * appeared. The name-only create must make the panel, add this test to it, and
 * show the membership, which then survives a reload.
 *
 * Test 5 (Amylase(Serum)) is a seeded catalog test; the membership created here
 * is removed again at the end so the seed stays as found.
 */

const TEST_ID = "5";
const API = "/api/OpenELIS-Global";

test.describe("OGC-1122 inline panel create", () => {
  test("typing a name and creating adds this test to a new panel", async ({
    page,
  }) => {
    test.setTimeout(90_000);
    const name = `E2E 1122 ${Date.now().toString().slice(-6)}`;

    await page.goto(`/MasterListsPage/TestCatalogEditor/${TEST_ID}/panels`, {
      waitUntil: "domcontentloaded",
    });
    const section = page.getByTestId("panels-section");
    await expect(section).toBeVisible({ timeout: NAV_TIMEOUT });
    const memberships = page.getByRole("table", { name: "panel-memberships" });

    await page.locator("#new-panel-name").fill(name);
    await page.getByTestId("create-panel-button").click();

    await expect(
      memberships.getByRole("row", { name: new RegExp(name) }),
    ).toBeVisible({
      timeout: UI_TIMEOUT,
    });

    // The membership is on the server, not only in component state.
    await page.reload({ waitUntil: "domcontentloaded" });
    await expect(
      page
        .getByRole("table", { name: "panel-memberships" })
        .getByRole("row", { name: new RegExp(name) }),
    ).toBeVisible({ timeout: NAV_TIMEOUT });

    const readBack = await page.evaluate(
      async ({ api, testId }) => {
        const res = await fetch(
          `${api}/rest/test-catalog/tests/${testId}/panels`,
          {
            credentials: "include",
          },
        );
        return (await res.json()).memberships as { panelName: string }[];
      },
      { api: API, testId: TEST_ID },
    );
    expect(readBack.map((m) => m.panelName)).toContain(name);

    // Put the seeded test back the way it was found: drop only the membership.
    const row = page
      .getByRole("table", { name: "panel-memberships" })
      .getByRole("row", { name: new RegExp(name) });
    await row.getByRole("button", { name: /remove from panel/i }).click();
    // Removal asks for confirmation first (FR-44).
    await page
      .getByRole("dialog")
      .getByRole("button", { name: "Remove from panel" })
      .click();
    await page
      .getByTestId("panels-section")
      .getByRole("button", { name: "Save", exact: true })
      .click();
    await expect(
      page
        .getByRole("table", { name: "panel-memberships" })
        .getByRole("row", { name: new RegExp(name) }),
    ).toHaveCount(0, { timeout: UI_TIMEOUT });
  });
});
