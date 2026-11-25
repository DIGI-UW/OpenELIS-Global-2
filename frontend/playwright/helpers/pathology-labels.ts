import { expect, Page, Response } from "@playwright/test";
import { getDocument } from "pdfjs-dist/legacy/build/pdf.mjs";
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

/**
 * Click a print button and return the text of the labels it printed, with
 * the address the servlet was called at. The answer is taken from the route
 * before the browser hands the PDF off, so its bytes are read whole; a
 * servlet that answers anything but a PDF fails here, as expectPdf does.
 */
export async function printedLabelText(page: Page, buttonName: string) {
  const servlet = "**/LabelMakerServlet**";
  let pdf: Buffer | undefined;
  let url = "";
  await page.context().route(servlet, async (route) => {
    const response = await route.fetch();
    url = route.request().url();
    expect(response.status(), "label servlet status").toBe(200);
    expect(response.headers()["content-type"] ?? "").toMatch(
      /^application\/pdf/,
    );
    pdf = await response.body();
    await route.fulfill({ response });
  });
  try {
    const popupOpened = page.waitForEvent("popup");
    await page.getByRole("button", { name: buttonName, exact: true }).click();
    const popup = await popupOpened;
    await expect
      .poll(() => pdf?.subarray(0, 5).toString(), { timeout: UI_TIMEOUT })
      .toBe("%PDF-");
    await popup.close();

    const document = await getDocument({
      data: new Uint8Array(pdf!),
      useSystemFonts: true,
    }).promise;
    try {
      const pages: string[] = [];
      for (let index = 1; index <= document.numPages; index += 1) {
        const content = await (await document.getPage(index)).getTextContent();
        pages.push(
          content.items
            .map((item) => ("str" in item ? item.str : ""))
            .join(" "),
        );
      }
      return { url, text: pages.join(" ").replace(/\s+/g, " ") };
    } finally {
      await document.destroy();
    }
  } finally {
    await page.context().unroute(servlet);
  }
}
