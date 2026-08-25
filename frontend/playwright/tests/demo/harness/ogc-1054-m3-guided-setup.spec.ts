import { expect, test } from "../../../helpers/test-base";
import type { Page, TestInfo } from "@playwright/test";
import { AnalyzerListPage } from "../../../fixtures/analyzer-list";
import { AnalyzerSetupPage } from "../../../fixtures/analyzer-setup";
import {
  LONG_TIMEOUT,
  NAV_TIMEOUT,
  TIMEOUT_SCALE,
} from "../../../helpers/timeouts";

const PROFILE_NAME = "Cepheid GeneXpert (ASTM Mode)";

function escapeRegExp(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

async function capture(page: Page, testInfo: TestInfo, name: string) {
  const path = testInfo.outputPath(`${name}.png`);
  await page.screenshot({ path, fullPage: false });
  await testInfo.attach(name, { path, contentType: "image/png" });
}

test.describe("OGC-1054 M3 guided analyzer setup", () => {
  test("creates, verifies, connects, activates, links QC, and deactivates through the UI", async ({
    page,
  }, testInfo) => {
    test.setTimeout(180_000 * TIMEOUT_SCALE);
    const runId = Date.now().toString().slice(-8);
    const analyzerName = `M3 GeneXpert ${runId}`;
    const listenerPort = String(45_000 + (Number(runId) % 1_000));
    const list = new AnalyzerListPage(page);
    const setup = new AnalyzerSetupPage(page);

    await list.goto();
    await list.expectLoaded();
    const breadcrumb = page.getByRole("navigation", { name: "Breadcrumb" });
    await expect(
      breadcrumb.getByRole("link", { name: "Home" }),
    ).toHaveAttribute("href", "/");
    await expect(
      page.getByRole("heading", { level: 1, name: "Analyzers" }),
    ).toBeVisible();

    await list.clickAdd();
    await setup.expectOpen();
    await expect(page).toHaveURL(/\/analyzers\?setup=instrument$/);
    await setup.selectProfile(PROFILE_NAME);
    await setup.fillName(analyzerName);
    await setup.selectLabUnit("Molecular Biology");
    await setup.continueToVerify();

    await expect(
      page.getByRole("button", { name: "Edit Instrument" }),
    ).toBeVisible();
    await expect(page.getByText("Current", { exact: true })).toBeVisible({
      timeout: LONG_TIMEOUT,
    });
    await expect(page.getByText("Current confirmation")).toBeVisible();
    await expect(
      page.getByRole("button", { name: "Continue to Connect" }),
    ).toBeEnabled();
    const verifyUrl = page.url();
    await capture(page, testInfo, "m3-verify");

    await page.reload({ waitUntil: "domcontentloaded", timeout: NAV_TIMEOUT });
    await expect(page).toHaveURL(verifyUrl);
    await expect(
      page.getByRole("button", { name: "Continue to Connect" }),
    ).toBeEnabled({
      timeout: LONG_TIMEOUT,
    });
    await setup.continueToConnect();
    await expect(
      page.getByRole("button", { name: "Edit Verify" }),
    ).toBeVisible();
    await expect(page).toHaveURL(
      (url) => url.searchParams.get("setup") === "connect",
    );

    await setup.fillPort(listenerPort);
    await page.getByRole("button", { name: "Save and finish later" }).click();
    await expect(setup.surface).not.toBeVisible({ timeout: LONG_TIMEOUT });

    let analyzerRow = page.getByRole("row", {
      name: new RegExp(escapeRegExp(analyzerName), "i"),
    });
    await expect(analyzerRow).toBeVisible({ timeout: LONG_TIMEOUT });
    await analyzerRow.getByRole("button", { name: "Actions" }).click();
    await page.getByRole("menuitem", { name: "Configure connection" }).click();
    await expect(page).toHaveURL(
      (url) => url.searchParams.get("setup") === "connect",
    );
    await expect(
      setup.surface.getByRole("spinbutton", { name: /port/i }),
    ).toHaveValue(listenerPort);
    await expect(page.getByText("Analyzer is ready to activate")).toBeVisible({
      timeout: LONG_TIMEOUT,
    });
    await capture(page, testInfo, "m3-ready-to-activate");

    await page.getByRole("button", { name: "Finish and activate" }).click();
    await expect(setup.surface).not.toBeVisible({ timeout: LONG_TIMEOUT });

    analyzerRow = page.getByRole("row", {
      name: new RegExp(escapeRegExp(analyzerName), "i"),
    });
    await expect(analyzerRow).toBeVisible({ timeout: LONG_TIMEOUT });
    await expect(analyzerRow).toContainText("Active");
    await expect(analyzerRow).toContainText(PROFILE_NAME);
    await expect(analyzerRow).not.toContainText(/\b\d+ units?\b/);
    await list.search(analyzerName);
    await expect(page).toHaveURL(
      (url) => url.searchParams.get("search") === analyzerName,
    );
    await expect(analyzerRow).toBeVisible();
    await capture(page, testInfo, "m3-active-dashboard");

    await analyzerRow.getByRole("button", { name: "Actions" }).click();
    await page.getByRole("menuitem", { name: "Configure connection" }).click();
    await expect(page).toHaveURL(
      (url) => url.searchParams.get("setup") === "connect",
    );
    await setup.testConnection();
    await capture(page, testInfo, "m3-connection-evidence");
    await setup.close();
    await expect(analyzerRow).toBeVisible({ timeout: LONG_TIMEOUT });

    await analyzerRow.getByRole("button", { name: "Actions" }).click();
    await page.getByRole("menuitem", { name: "Quality Control" }).click();
    await expect(page).toHaveURL(
      /\/analyzers\/qc\/instruments\/\d+\?returnTo=/,
    );
    expect(await page.evaluate(() => window.scrollY)).toBe(0);
    await expect(
      page.getByRole("heading", { level: 1, name: analyzerName }),
    ).toBeVisible({
      timeout: LONG_TIMEOUT,
    });
    const qcBreadcrumb = page.getByRole("navigation", { name: "Breadcrumb" });
    const analyzerReturnLink = qcBreadcrumb.getByRole("link", {
      name: "Analyzers",
    });
    const analyzerReturnHref = await analyzerReturnLink.getAttribute("href");
    expect(analyzerReturnHref).not.toBeNull();
    const analyzerReturnUrl = new URL(analyzerReturnHref!, page.url());
    expect(analyzerReturnUrl.pathname).toBe("/analyzers");
    expect(analyzerReturnUrl.searchParams.get("search")).toBe(analyzerName);
    await capture(page, testInfo, "m3-linked-operational-qc");

    await analyzerReturnLink.click();
    await expect(page).toHaveURL(
      (url) => url.searchParams.get("search") === analyzerName,
    );
    await list.expectLoaded();
    await expect(analyzerRow).toBeVisible({ timeout: LONG_TIMEOUT });
    await analyzerRow.getByRole("button", { name: "Actions" }).click();
    await page.getByRole("menuitem", { name: "Deactivate" }).click();
    await expect(page).toHaveURL(/lifecycle=deactivate/);
    await expect(
      page.getByRole("heading", { name: "Deactivate analyzer" }),
    ).toBeVisible();
    await page.getByRole("button", { name: "Deactivate analyzer" }).click();
    await expect(analyzerRow).toContainText("Inactive", {
      timeout: LONG_TIMEOUT,
    });

    await page.setViewportSize({ width: 390, height: 844 });
    await page.reload({ waitUntil: "domcontentloaded", timeout: NAV_TIMEOUT });
    await expect(analyzerRow).toBeVisible({ timeout: LONG_TIMEOUT });
    await expect(analyzerRow).toContainText(PROFILE_NAME, {
      timeout: LONG_TIMEOUT,
    });
    expect(
      await page.evaluate(
        () =>
          document.documentElement.scrollWidth <=
          document.documentElement.clientWidth,
      ),
      "Analyzer dashboard should not overflow the mobile page horizontally",
    ).toBe(true);
    await capture(page, testInfo, "m3-mobile-dashboard");
  });
});
