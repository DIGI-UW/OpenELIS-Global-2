/**
 * Create an analyzer via the UI using a profile for auto-fill.
 *
 * Uses the AnalyzerFormPage and AnalyzerListPage page objects.
 * Profile selection auto-fills identifier pattern, analyzer type,
 * protocol version, and communication mode.
 */

import { Page } from "@playwright/test";
import { AnalyzerFormPage } from "../fixtures/analyzer-form";
import { AnalyzerListPage } from "../fixtures/analyzer-list";
import type { DemoPresentation } from "./demo-presentation";
import type { AnalyzerTestConfig } from "./analyzer-test-config";
import { LONG_TIMEOUT } from "./timeouts";

export async function createAnalyzerFromProfile(
  page: Page,
  config: AnalyzerTestConfig,
  presentation: DemoPresentation,
): Promise<void> {
  const list = new AnalyzerListPage(page);
  const form = new AnalyzerFormPage(page);

  await list.goto();
  await list.expectLoaded();
  await presentation.pause(500);

  await list.clickAdd();
  await form.expectOpen();

  // Select plugin type
  await form.selectPluginType(config.pluginType);
  await presentation.pause(500);

  // Select profile (auto-fills fields)
  if (config.profileName) {
    await form.selectDefaultConfig(config.profileName);
    await presentation.pause(1_000);
  }

  // Select analyzer type (may already be set by profile)
  await form.selectType(config.analyzerType);

  // Fill name
  await form.fillName(config.name);
  await presentation.pause(500);

  // Save
  await form.save();
  await form.expectSuccessNotification();

  // Wait for modal to close
  const { expect } = await import("@playwright/test");
  await expect(form.modal).not.toBeVisible({ timeout: LONG_TIMEOUT });
  await presentation.pause(1_000);
}
