import { test, expect } from "../../../helpers/test-base";
import { createPathologyCase } from "../../../helpers/seed-pathology-data";
import {
  openCase,
  expandSection,
  reportRow,
  reportUpload,
  toast,
} from "../../../helpers/pathology-case-view";
import { UI_TIMEOUT } from "../../../helpers/timeouts";

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
  test(`clinical attachment ${file.mimeType}: upload, reload and open in a new tab`, async ({
    page,
    context,
  }) => {
    const seeded = await createPathologyCase(page);
    const caseId = seeded.pathologySampleId;
    await openCase(page, seeded);
    await expandSection(page, "pathology-section-reports");
    const reports = page.locator("#pathology-section-reports");

    // A chosen file is stored at once as the next report version.
    await reportUpload(page).locator('input[type="file"]').setInputFiles(file);
    await expect(toast(page, /Report version \d+ uploaded\./)).not.toHaveCount(
      0,
      { timeout: UI_TIMEOUT },
    );
    await page.reload();
    await expandSection(page, "pathology-section-reports");
    const row = reports.locator('[data-testid^="report-row-"]');
    await expect(row).toHaveCount(1);
    await expect(row).toContainText("Uploaded");
    const version = Number(
      (await row.getAttribute("data-testid"))!.replace("report-row-", ""),
    );
    const link = reportRow(page, version).getByRole("link");
    const address = await link.getAttribute("href");
    expect(address).toMatch(
      new RegExp(`/rest/pathology/caseView/${caseId}/reports/\\d+/file$`),
    );

    // The row opens the stored file at its own address in a new tab, which
    // cannot reach back to this page and carries the session.
    const opened = context.waitForEvent("page");
    const served = context.waitForEvent("response", {
      predicate: (r) => new URL(r.url()).pathname === address,
      timeout: UI_TIMEOUT,
    });
    await reportRow(page, version).getByRole("cell").nth(1).click();
    const tab = await opened;
    const response = await served;
    expect(response.status()).toBe(200);
    expect(response.headers()["content-type"]).toContain(file.mimeType);
    const stored = await page.request.get(address!);
    expect(Buffer.from(await stored.body())).toEqual(file.buffer);
    if (file.mimeType === "image/png") {
      await tab.waitForLoadState("domcontentloaded");
      expect(await tab.evaluate(() => window.opener)).toBeNull();
      await expect(tab.locator("img")).toHaveJSProperty("naturalWidth", 1);
    }
    await tab.close();

    // A stored type the screen does not trust changes nothing it renders:
    // the row still opens the server's own address, and the type is never
    // put into the page.
    const forged = 'data:image/png" onload="notAllowed';
    await page.route(`**/rest/pathology/caseView/${caseId}`, async (route) => {
      const original = await route.fetch();
      const body = await original.json();
      body.reports[0].fileType = forged;
      await route.fulfill({ response: original, json: body });
    });
    await page.reload();
    await expandSection(page, "pathology-section-reports");
    await expect(reportRow(page, version).getByRole("link")).toHaveAttribute(
      "href",
      address!,
    );
    expect(await reports.innerHTML()).not.toContain("notAllowed");
  });
}
