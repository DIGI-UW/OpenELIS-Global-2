import { test, expect } from "../../../helpers/test-base";
import { SiteInformationPage } from "../../../fixtures/esig-admin";
import { createEsigSampleOrder } from "../../../helpers/seed-esig-data";
import {
  QUICK_TIMEOUT,
  SHORT_TIMEOUT,
  UI_TIMEOUT,
  LONG_TIMEOUT,
  NAV_TIMEOUT,
} from "../../../helpers/timeouts";

const ESIG_API = "/api/OpenELIS-Global/rest/esig";

const username = process.env.TEST_USER || "admin";
const password = process.env.TEST_PASS || "adminADMIN!";

/**
 * E-Signature integration with Result Entry and Validation workflows.
 *
 * Tests the complete user journey:
 * 1. Admin enables e-signatures via Site Information UI
 * 2. User enters results → e-sig modal appears (AUTHORED)
 * 3. User completes first-use certification + signing ceremony
 * 4. User validates results → e-sig modal appears (VALIDATED_AND_RELEASED)
 * 5. Session continuation shows password-only mode
 *
 * No direct database access. All interactions through UI or REST API.
 */

test("E-Signature — full result entry and validation flow", async ({
  page,
}) => {
  // ── Step 1: Enable e-signatures via admin UI ──────────────────

  await test.step("Enable e-signatures in Site Information", async () => {
    const siteInfo = new SiteInformationPage(page);
    await siteInfo.goto();
    await siteInfo.setBooleanSetting("electronicSignatureEnabled", true);
  });

  // Verify it took effect via the API
  await test.step("Verify e-sig is enabled", async () => {
    const res = await page.request.get(`${ESIG_API}/enabled`);
    const { enabled } = await res.json();
    expect(enabled, "e-sig should be enabled after admin toggle").toBe(true);
  });

  // ── Step 2: Create test data via API ──────────────────────────

  let accessionNumber: string;

  await test.step("Create sample order via API", async () => {
    const data = await createEsigSampleOrder(page);
    expect(data, "sample order must be created for e-sig testing").toBeTruthy();
    accessionNumber = data!.accessionNumber;
  });

  // ── Step 3: Result Entry — AUTHORED signature ─────────────────

  await test.step("Navigate to Result Entry and search for order", async () => {
    await page.goto("/result?type=order&doRange=false", {
      waitUntil: "domcontentloaded",
    });

    // Search by accession number (scope to main to avoid header Search button)
    const main = page.getByRole("main");
    const searchInput = main.getByPlaceholder(/accession/i);
    await expect(searchInput).toBeVisible({ timeout: NAV_TIMEOUT });
    await searchInput.fill(accessionNumber);
    await main.getByRole("button", { name: /search/i }).click();

    // Wait for results to load
    await expect(main.getByRole("button", { name: "Save" })).toBeVisible({
      timeout: NAV_TIMEOUT,
    });
  });

  await test.step("Click Save — e-sig modal appears", async () => {
    await page.getByRole("button", { name: "Save" }).click();

    const modal = page.getByRole("dialog");
    await expect(modal).toBeVisible({ timeout: UI_TIMEOUT });

    await expect(
      modal.getByRole("heading", { name: /electronic signature/i }),
    ).toBeVisible({ timeout: SHORT_TIMEOUT });
  });

  await test.step("Complete certification if needed, then verify signing step", async () => {
    const modal = page.getByRole("dialog");

    // The user may or may not be certified already (depends on whether
    // the API contract tests ran first in this shard). Handle both paths.
    const certText = modal.getByText(/certification/i);
    if (
      await certText.isVisible({ timeout: QUICK_TIMEOUT }).catch(() => false)
    ) {
      // Certification ceremony — acknowledge and certify
      await modal.locator('label[for="certification-acknowledgement"]').click();
      await modal.locator('input[type="password"]').fill(password);
      await modal.getByRole("button", { name: /certify|continue/i }).click();
    }

    // Now we should be on the signing step with AUTHORED meaning
    await expect(modal.getByText("Authored")).toBeVisible({
      timeout: UI_TIMEOUT,
    });
    await expect(modal.getByText(/21 CFR Part 11/)).toBeVisible({
      timeout: SHORT_TIMEOUT,
    });
  });

  await test.step("Sign with valid credentials", async () => {
    const modal = page.getByRole("dialog");

    // Fill username if visible (full-auth mode)
    const usernameInput = modal.locator("#signature-username");
    if (
      await usernameInput
        .isVisible({ timeout: QUICK_TIMEOUT })
        .catch(() => false)
    ) {
      await usernameInput.fill(username);
    }

    // Fill password
    const pwInput = page.locator("#signature-password");
    await expect(pwInput).toBeVisible({ timeout: UI_TIMEOUT });
    await pwInput.click();
    await pwInput.pressSequentially(password, { delay: 30 });

    // Click Sign
    await modal.getByRole("button", { name: /sign/i }).click();

    // Modal should close after successful signature
    await expect(modal).toBeHidden({ timeout: LONG_TIMEOUT });
  });

  // ── Step 4: Validation — VALIDATED_AND_RELEASED signature ─────

  await test.step("Navigate to Validation and find the order", async () => {
    await page.goto("/validation?type=order", {
      waitUntil: "domcontentloaded",
    });

    // Search by accession number (scope to main to avoid header Search button)
    const main = page.getByRole("main");
    const searchInput = main.getByPlaceholder(/accession|lab no/i);
    await expect(searchInput).toBeVisible({ timeout: NAV_TIMEOUT });
    await searchInput.fill(accessionNumber);
    await main.getByRole("button", { name: /search/i }).click();

    await expect(main.getByRole("button", { name: "Validate" })).toBeVisible({
      timeout: NAV_TIMEOUT,
    });
  });

  await test.step("Select all results and click Validate — e-sig modal shows VALIDATED_AND_RELEASED", async () => {
    // Select all results for validation
    const saveAllLabel = page.locator('label[for="saveallresults"]');
    if (await saveAllLabel.isVisible()) {
      await saveAllLabel.click();
    }

    await page.getByRole("button", { name: "Validate" }).click();

    const modal = page.getByRole("dialog");
    await expect(modal).toBeVisible({ timeout: UI_TIMEOUT });

    // Verify VALIDATED_AND_RELEASED meaning
    await expect(modal.getByText(/validated.*released/i)).toBeVisible({
      timeout: SHORT_TIMEOUT,
    });
  });

  await test.step("Session continuation — password-only mode", async () => {
    const modal = page.getByRole("dialog");

    // After first signature, subsequent ones should be password-only
    // Username should be pre-filled and read-only (shown as text, not input)
    await expect(modal.getByText(/active signing session/i)).toBeVisible({
      timeout: SHORT_TIMEOUT,
    });
  });

  await test.step("Sign validation with valid credentials", async () => {
    const modal = page.getByRole("dialog");

    // In password-only mode, just enter password
    await modal.locator('input[type="password"]').fill(password);
    await modal.getByRole("button", { name: /sign/i }).click();

    await expect(modal).toBeHidden({ timeout: LONG_TIMEOUT });

    // Wait for the page to finish any post-sign navigation/redirect
    // before attempting cleanup navigation
    await page.waitForLoadState("networkidle");
  });

  // ── Cleanup: Restore original e-sig setting ───────────────────

  await test.step("Disable e-signatures (cleanup)", async () => {
    const siteInfo = new SiteInformationPage(page);
    await siteInfo.goto();
    await siteInfo.setBooleanSetting("electronicSignatureEnabled", false);
  });
});
