import { test, expect, Page } from "../../../helpers/test-base";
import {
  createSampleOrder,
  enterResults,
} from "../../../helpers/seed-tat-data";
import { NAV_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";

/**
 * Attachments are added during order entry and results entry. The validation
 * review panel reuses the Results Entry attachments section as a read-only
 * list: it shows what was attached, with View and Download, but offers no
 * upload. Results entry keeps its upload whatever the row's edit state.
 */

const EXPANDER =
  'main button[aria-label*="xpand"], main .cds--table-expand__button';

async function newOrderAwaitingValidation(page: Page): Promise<string> {
  const now = new Date();
  const pad = (n: number) => String(n).padStart(2, "0");
  const accession = await createSampleOrder(page, {
    labNo: "",
    receivedDate: `${now.getUTCFullYear()}-${pad(now.getUTCMonth() + 1)}-${pad(now.getUTCDate())}`,
    receivedTime: `${pad(now.getUTCHours())}:${pad(now.getUTCMinutes())}`,
    referringSiteId: process.env.PW_REFERRING_SITE_ID,
    providerPersonId: process.env.PW_PROVIDER_PERSON_ID,
  });
  expect(accession).not.toBe("");
  await enterResults(page, accession);
  return accession;
}

async function openAttachments(page: Page, panel: ReturnType<Page["locator"]>) {
  await expect(panel).toBeVisible({ timeout: UI_TIMEOUT });
  const listed = panel.getByText(/No attachments|\.png|\.pdf|\.jpg/i);
  if ((await listed.count()) === 0) {
    await panel
      .getByRole("button", { name: /Attachments/ })
      .first()
      .click();
  }
  await expect(panel.getByText(/Attachments/).first()).toBeVisible({
    timeout: UI_TIMEOUT,
  });
}

test.describe("Validation attachments are read-only", () => {
  test("results entry uploads, validation only lists what was attached", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const accession = await newOrderAwaitingValidation(page);

    await page.goto(`/Results?accessionNumber=${accession}`, {
      waitUntil: "domcontentloaded",
      timeout: NAV_TIMEOUT,
    });
    const resultsExpander = page.locator(EXPANDER).first();
    await expect(resultsExpander).toBeVisible({ timeout: NAV_TIMEOUT });
    await resultsExpander.click();
    const resultsPanel = page.locator(".unifiedExpandedPanel").first();
    await openAttachments(page, resultsPanel);
    await expect(resultsPanel.getByTestId("attachment-upload")).toBeVisible({
      timeout: UI_TIMEOUT,
    });
    const uploaded = page.waitForResponse(
      (response) =>
        response.url().includes("/attachments") &&
        response.request().method() === "POST",
      { timeout: UI_TIMEOUT },
    );
    await resultsPanel
      .locator('input[type="file"]')
      .first()
      .setInputFiles({
        name: "worksheet.png",
        mimeType: "image/png",
        buffer: Buffer.from(
          "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==",
          "base64",
        ),
      });
    await uploaded;
    await expect(resultsPanel.getByText("worksheet.png")).toBeVisible({
      timeout: UI_TIMEOUT,
    });

    await page.goto(`/validation?type=order&accessionNumber=${accession}`, {
      waitUntil: "domcontentloaded",
      timeout: NAV_TIMEOUT,
    });
    const validationExpander = page.locator(EXPANDER).first();
    await expect(validationExpander).toBeVisible({ timeout: NAV_TIMEOUT });
    await validationExpander.click();
    const reviewPanel = page.locator(".validationReviewPanel").first();
    await openAttachments(page, reviewPanel);
    await expect(reviewPanel.getByText("worksheet.png")).toBeVisible({
      timeout: UI_TIMEOUT,
    });
    await expect(
      reviewPanel.getByRole("button", { name: "View" }).first(),
    ).toBeVisible();
    await expect(reviewPanel.getByTestId("attachment-upload")).toHaveCount(0);
    await expect(reviewPanel.locator('input[type="file"]')).toHaveCount(0);
    await expect(reviewPanel.getByText("Add attachment")).toHaveCount(0);
  });
});
