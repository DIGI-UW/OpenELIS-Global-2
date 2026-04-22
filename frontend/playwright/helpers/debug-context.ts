import { Page } from "@playwright/test";
import { QUICK_TIMEOUT } from "./timeouts";

export async function captureDebugContext(
  page: Page,
  consoleErrors: string[],
): Promise<{ url: string; bodyPreview: string; consoleErrors: string[] }> {
  const bodyText = await page
    .locator("body")
    .textContent({ timeout: QUICK_TIMEOUT })
    .catch(() => "(empty)");
  return {
    url: page.url(),
    bodyPreview: (bodyText || "(empty)").substring(0, 500),
    consoleErrors,
  };
}
