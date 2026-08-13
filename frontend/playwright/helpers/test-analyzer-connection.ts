/**
 * Reusable test-connection helper for any analyzer type.
 *
 * Opens the overflow menu → clicks "Test Connection" → clicks "Test" button →
 * waits for success tag → closes modal. Works for all protocols (the modal is
 * protocol-agnostic).
 *
 * Shared by analyzer harness integration scenarios.
 */

import { expect, Locator, Page } from "@playwright/test";
import { SHORT_TIMEOUT, UI_TIMEOUT, LONG_TIMEOUT } from "./timeouts";

export async function testAnalyzerConnection(page: Page, analyzerRow: Locator) {
  const overflow = analyzerRow
    .first()
    .locator('[data-testid^="analyzer-row-overflow-"]')
    .first();
  await overflow.click();

  const testConnectionAction = page
    .locator('[data-testid*="analyzer-action-test-connection"]')
    .first();
  await expect(testConnectionAction).toBeVisible({ timeout: SHORT_TIMEOUT });
  await testConnectionAction.click();

  const connectionModal = page.locator('[data-testid="test-connection-modal"]');
  await expect(connectionModal).toBeVisible({ timeout: UI_TIMEOUT });

  const testButton = page.locator(
    '[data-testid="test-connection-test-button"]',
  );
  await testButton.click();

  const successTag = page.locator('[data-testid="test-connection-success"]');
  await expect(successTag).toBeVisible({ timeout: LONG_TIMEOUT });

  await connectionModal
    .locator('[data-testid="test-connection-close-button"]')
    .click();
  await expect(connectionModal).toBeHidden({ timeout: UI_TIMEOUT });
}
