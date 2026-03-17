import { Page } from "@playwright/test";

export async function captureDebugContext(
  page: Page,
  consoleErrors: string[],
): Promise<{ url: string; bodyPreview: string; consoleErrors: string[] }> {
  const bodyText = await page
    .locator("body")
    .textContent({ timeout: 2_000 })
    .catch(() => "(empty)");
  return {
    url: page.url(),
    bodyPreview: (bodyText || "(empty)").substring(0, 500),
    consoleErrors,
  };
}
