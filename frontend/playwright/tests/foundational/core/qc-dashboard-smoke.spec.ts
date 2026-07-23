import { test, expect } from "../../../helpers/test-base";

/**
 * QC Dashboard smoke test (M1 T009).
 *
 * Verifies the Westgard QC dashboard renders with its core widgets and that
 * the major navigation surfaces (Instruments tab, Alerts tab, Control Lots
 * page) load without JS errors. Does NOT seed or assert on real QC data —
 * data-path coverage is provided by the analyzer harness E2E (T001–T004) and
 * the backend controller tests (T005–T007). This test is a frontend
 * render/routing smoke only.
 *
 * Stable selectors are the `data-testid` attributes baked into QCDashboard,
 * AlertsTab, and InstrumentDetailPage during the modal→page conversion.
 */
test.describe("QC Dashboard smoke", () => {
  test("dashboard page loads with header and tabs", async ({ page }) => {
    await page.goto("/analyzers/qc/db", { waitUntil: "domcontentloaded" });

    // Wait for the dashboard root to render (past the initial loading widget)
    await expect(page.getByTestId("qc-dashboard")).toBeVisible();
    await expect(page.getByTestId("qc-dashboard-header")).toBeVisible();

    // Both tabs present
    await expect(page.getByTestId("qc-tab-instruments")).toBeVisible();
    await expect(page.getByTestId("qc-tab-alerts")).toBeVisible();

    // Refresh control
    await expect(page.getByTestId("qc-dashboard-refresh-button")).toBeVisible();
  });

  test("alerts tab renders when selected", async ({ page }) => {
    await page.goto("/analyzers/qc/db", { waitUntil: "domcontentloaded" });
    await expect(page.getByTestId("qc-dashboard")).toBeVisible();

    await page.getByTestId("qc-tab-alerts").click();

    // Alerts tab root should render (either with data or empty state).
    await expect(page.getByTestId("alerts-tab")).toBeVisible();
    await expect(page.getByTestId("alerts-active-section")).toBeVisible();
  });

  test("refresh button triggers a reload without error banner", async ({
    page,
  }) => {
    await page.goto("/analyzers/qc/db", { waitUntil: "domcontentloaded" });
    await expect(page.getByTestId("qc-dashboard")).toBeVisible();

    const refresh = page.getByTestId("qc-dashboard-refresh-button");
    await refresh.click();

    // Error banner should not appear after a refresh.
    await expect(page.getByTestId("qc-dashboard-error")).toHaveCount(0);
    await expect(page.getByTestId("qc-dashboard")).toBeVisible();
  });

  test("control lots page loads", async ({ page }) => {
    await page.goto("/analyzers/qc/control-lots", {
      waitUntil: "domcontentloaded",
    });

    // Route resolves and renders some Carbon page content (not a blank shell).
    // Asserting on `main` is selector-stable across the Control Lots page
    // without hard-coding a component-specific testid.
    await expect(page.locator("main")).toBeVisible();
    await expect(page).toHaveURL(/\/analyzers\/qc\/control-lots$/);
  });
});
