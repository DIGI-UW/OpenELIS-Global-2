import { readFile } from "node:fs/promises";
import { test, expect } from "../../../helpers/test-base";
import { createPathologyCase } from "../../../helpers/seed-pathology-data";
import {
  openCase,
  expandSection,
  saveDraft,
} from "../../../helpers/pathology-case-view";

const png = Buffer.from(
  "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+a7V8AAAAASUVORK5CYII=",
  "base64",
);

// A single blank page, with byte-accurate cross references for the native PDF viewer.
function blankPdf(): Buffer {
  let pdf = "%PDF-1.4\n";
  const objects = [
    "<< /Type /Catalog /Pages 2 0 R >>",
    "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
    "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 200 200] >>",
  ];
  const offsets = [0];
  objects.forEach((body, index) => {
    offsets.push(Buffer.byteLength(pdf));
    pdf += `${index + 1} 0 obj\n${body}\nendobj\n`;
  });
  const start = Buffer.byteLength(pdf);
  pdf += "xref\n0 4\n0000000000 65535 f \n";
  offsets.slice(1).forEach((offset) => {
    pdf += `${String(offset).padStart(10, "0")} 00000 n \n`;
  });
  pdf += `trailer\n<< /Size 4 /Root 1 0 R >>\nstartxref\n${start}\n%%EOF\n`;
  return Buffer.from(pdf);
}

for (const file of [
  { name: "slide.png", mimeType: "image/png", buffer: png },
  { name: "report.pdf", mimeType: "application/pdf", buffer: blankPdf() },
]) {
  test(`clinical attachment ${file.mimeType}: upload, save, reload and preview`, async ({
    page,
    context,
  }) => {
    const seeded = await createPathologyCase(page);
    await openCase(page, seeded);
    await expandSection(page, "pathology-section-reports");
    const reports = page.locator("#pathology-section-reports");
    await reports.getByRole("button", { name: /Add report/i }).click();
    await reports.locator('input[type="file"]').setInputFiles(file);
    await saveDraft(page, seeded.pathologySampleId);
    await page.reload();
    await expandSection(page, "pathology-section-reports");
    const opened = context.waitForEvent("page");
    // The CI headless shell delivers PDFs as downloads; PNGs open as images.
    const pdfDownload =
      file.mimeType === "application/pdf"
        ? page.waitForEvent("download")
        : null;
    await reports
      .getByRole("button", { name: "View File", exact: true })
      .click();
    const preview = await opened;
    if (pdfDownload) {
      const download = await pdfDownload;
      expect(download.url()).toMatch(/^blob:/);
      expect(download.suggestedFilename()).toMatch(/\.pdf$/);
      const path = await download.path();
      expect(path).not.toBeNull();
      expect(await readFile(path!)).toEqual(file.buffer);
    } else {
      await preview.waitForLoadState("domcontentloaded");
      expect(preview.url()).toMatch(/^blob:/);
      expect(await preview.evaluate(() => window.opener)).toBeNull();
      await expect(preview.locator("img")).toHaveJSProperty("naturalWidth", 1);
      const downloaded = await page.evaluate(async (url) => {
        const response = await fetch(url);
        return {
          type: response.headers.get("Content-Type"),
          bytes: Array.from(new Uint8Array(await response.arrayBuffer())),
        };
      }, preview.url());
      expect(downloaded.type).toBe(file.mimeType);
      expect(Buffer.from(downloaded.bytes)).toEqual(file.buffer);
    }
    await preview.close();

    // Model an older stored attachment without changing any real stored record.
    await page.route(
      `**/rest/pathology/caseView/${seeded.pathologySampleId}`,
      async (route) => {
        const response = await route.fetch();
        const body = await response.json();
        body.reports[0].fileType = 'data:image/png" onload="notAllowed';
        await route.fulfill({ response, json: body });
      },
    );
    await page.reload();
    await expandSection(page, "pathology-section-reports");
    const pageCount = context.pages().length;
    await reports
      .getByRole("button", { name: "View File", exact: true })
      .click();
    await expect(
      reports.getByText(/This attachment cannot be previewed/),
    ).toBeVisible();
    expect(context.pages()).toHaveLength(pageCount);
  });
}
