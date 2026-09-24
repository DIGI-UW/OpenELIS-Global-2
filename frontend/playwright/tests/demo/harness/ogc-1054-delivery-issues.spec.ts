import type { Page, TestInfo } from "@playwright/test";
import { expect, test } from "../../../helpers/test-base";
import { LONG_TIMEOUT, NAV_TIMEOUT } from "../../../helpers/timeouts";

// seed-mvp-traffic.sh posts one message from a sender no analyzer connection
// claims; the Bridge holds it in its dead-message queue.

async function capture(page: Page, testInfo: TestInfo, name: string) {
  const path = testInfo.outputPath(`${name}.png`);
  await page.screenshot({ path, fullPage: false });
  await testInfo.attach(name, { path, contentType: "image/png" });
}

test.describe("OGC-1054 undelivered analyzer results", () => {
  test("shows a result the Bridge could not deliver and dismisses it through the visible UI", async ({
    page,
  }, testInfo) => {
    await page.goto("/analyzers", {
      waitUntil: "domcontentloaded",
      timeout: NAV_TIMEOUT,
    });
    const banner = page.getByTestId("delivery-issues-attention");
    await expect(banner).toContainText("not delivered", {
      timeout: LONG_TIMEOUT,
    });
    await capture(page, testInfo, "01-analyzers-undelivered-banner");

    await banner
      .getByRole("button", { name: "Review undelivered results" })
      .click();
    await expect(page).toHaveURL(/\/AnalyzerResults\?view=import-issues/, {
      timeout: LONG_TIMEOUT,
    });

    const section = page.getByTestId("analyzer-delivery-issues");
    await expect(
      section.getByRole("heading", { name: "Undelivered analyzer results" }),
    ).toBeVisible({ timeout: LONG_TIMEOUT });
    const unrecognizedRow = section
      .getByRole("row")
      .filter({ hasText: "Unrecognized sender" })
      .filter({
        hasText:
          "The sender matches no saved analyzer connection. Set up the analyzer, then retry.",
      })
      .first();
    await expect(unrecognizedRow).toBeVisible();
    await expect(unrecognizedRow.getByText("Not delivered")).toBeVisible();
    await capture(page, testInfo, "02-undelivered-result-explained");

    const heldBefore = await section
      .getByRole("row")
      .filter({ hasText: "Unrecognized sender" })
      .count();
    await unrecognizedRow.getByRole("button", { name: "Dismiss" }).click();

    await expect(
      section.getByRole("row").filter({ hasText: "Unrecognized sender" }),
    ).toHaveCount(heldBefore - 1, { timeout: LONG_TIMEOUT });
    await capture(page, testInfo, "03-undelivered-result-dismissed");
  });
});
