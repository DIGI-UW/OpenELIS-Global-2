import { expect, Page, Response } from "@playwright/test";
import { UI_TIMEOUT } from "./timeouts";

/**
 * Printing a pathology case's labels from the case view, shared by the specs
 * that print cassette and slide labels.
 */

/**
 * Click a print button and return the label servlet's answer. The labels open
 * in a new window, but a headless browser hands a PDF off as a download and
 * the window never commits a URL, so the request and its answer are read from
 * the browser context, which sees the window's traffic either way.
 */
export async function printLabels(page: Page, buttonName: string) {
  const answer = page.context().waitForEvent("response", {
    predicate: (r) => r.url().includes("/LabelMakerServlet"),
    timeout: UI_TIMEOUT,
  });
  const popupOpened = page.waitForEvent("popup");
  await page.getByRole("button", { name: buttonName, exact: true }).click();
  const popup = await popupOpened;
  const response = await answer;
  return { popup, response };
}

/**
 * Fail with the servlet's own words when it answers anything but a PDF: an
 * HTML page from it means no label was produced, which is a defect.
 */
export async function expectPdf(response: Response) {
  const contentType = response.headers()["content-type"] ?? "";
  const preview = contentType.startsWith("application/pdf")
    ? ""
    : (await response.text()).slice(0, 200);
  expect(response.status(), `label servlet answered: ${preview}`).toBe(200);
  expect(
    contentType,
    `label servlet answered ${contentType} instead of a PDF: ${preview}`,
  ).toMatch(/^application\/pdf/);
}
