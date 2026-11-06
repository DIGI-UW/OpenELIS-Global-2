import { readFile } from "node:fs/promises";
import type { Page } from "@playwright/test";
import { test, expect } from "../../../helpers/test-base";

const png = Buffer.from(
  "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+a7V8AAAAASUVORK5CYII=",
  "base64",
);

const jpeg = Buffer.from(
  "/9j/4AAQSkZJRgABAQEASABIAAD/2wBDAP////////////////////////////////////////////////////////////////////////////////////8AAP/2wBDAf//////////////////////////////////////////////////////////////////////////////////////wAARCAABAAEDASIAAhEBAxEB/8QAFQABAQAAAAAAAAAAAAAAAAAAAAP/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/8QAFAEBAAAAAAAAAAAAAAAAAAAAAP/EABQRAQAAAAAAAAAAAAAAAAAAAAD/2gAMAwEAAhEDEQA/AKAAAAH/2Q==",
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

const markupNote = "Run <b>3</b> <script>notAllowed()</script> passed";

/** Creates a notebook template through the API; the screen is only used for attachments. */
async function seedNotebook(page: Page, title: string): Promise<number> {
  await page.goto("/");
  const result = await page.evaluate(async (notebookTitle) => {
    const csrf = localStorage.getItem("CSRF") || "";
    const headers = {
      Accept: "application/json",
      "Content-Type": "application/json",
      "X-CSRF-Token": csrf,
    };
    const types = await fetch(
      "/api/OpenELIS-Global/rest/displayList/NOTEBOOK_EXPT_TYPE",
      { credentials: "include", headers },
    ).then((res) => res.json());
    const res = await fetch("/api/OpenELIS-Global/rest/notebook/create", {
      method: "POST",
      credentials: "include",
      headers,
      body: JSON.stringify({
        title: notebookTitle,
        type: types.length > 0 ? Number(types[0].id) : null,
        objective: "Attachment handling",
        isTemplate: true,
        files: [],
        pages: [],
        sampleIds: [],
        analyzerIds: [],
        tags: [],
        comments: [],
      }),
    });
    return { status: res.status, text: await res.text() };
  }, title);
  if (result.status !== 200) {
    throw new Error(
      `notebook seed failed: HTTP ${result.status}: ${result.text.substring(0, 400)}`,
    );
  }
  return JSON.parse(result.text).id;
}

async function openAttachments(page: Page, notebookId: number) {
  await page.goto(`/NoteBookEntryForm/${notebookId}`);
  await page.getByRole("tab", { name: "Attachments" }).click();
}

/** Saves the open notebook and waits for the server's answer; callers assert on the screen. */
async function saveNotebook(page: Page, notebookId: number) {
  const answered = page.waitForResponse(
    (response) =>
      response.url().endsWith(`/rest/notebook/update/${notebookId}`) &&
      response.request().method() === "POST",
  );
  await page.getByRole("button", { name: "Save", exact: true }).click();
  await answered;
}

for (const file of [
  { name: "gel.png", mimeType: "image/png", buffer: png },
  { name: "plate.jpg", mimeType: "image/jpeg", buffer: jpeg },
  { name: "protocol.pdf", mimeType: "application/pdf", buffer: blankPdf() },
  {
    name: "observations.txt",
    mimeType: "text/plain",
    buffer: Buffer.from(markupNote),
  },
]) {
  test(`notebook attachment ${file.mimeType}: upload, save, reload and preview`, async ({
    page,
    context,
  }) => {
    const notebookId = await seedNotebook(
      page,
      `Attachment ${file.name} ${Date.now()}`,
    );
    await openAttachments(page, notebookId);
    await page.locator('input[type="file"]').setInputFiles(file);
    await saveNotebook(page, notebookId);

    await openAttachments(page, notebookId);
    const tile = page
      .getByTestId("notebook-attachment")
      .filter({ hasText: file.name });
    await expect(tile).toBeVisible();
    const opened = context.waitForEvent("page");
    // The CI headless shell delivers PDFs as downloads; other types open in a tab.
    const pdfDownload =
      file.mimeType === "application/pdf"
        ? page.waitForEvent("download")
        : null;
    await tile.getByRole("button", { name: "View File", exact: true }).click();
    const preview = await opened;
    if (pdfDownload) {
      const download = await pdfDownload;
      expect(download.url()).toMatch(/^blob:/);
      const path = await download.path();
      expect(path).not.toBeNull();
      expect(await readFile(path!)).toEqual(file.buffer);
    } else {
      await preview.waitForLoadState("domcontentloaded");
      expect(preview.url()).toMatch(/^blob:/);
      expect(await preview.evaluate(() => window.opener)).toBeNull();
      const downloaded = await page.evaluate(async (url) => {
        const response = await fetch(url);
        return {
          type: response.headers.get("Content-Type"),
          bytes: Array.from(new Uint8Array(await response.arrayBuffer())),
        };
      }, preview.url());
      expect(downloaded.type).toBe(file.mimeType);
      expect(Buffer.from(downloaded.bytes)).toEqual(file.buffer);
      if (file.mimeType === "text/plain") {
        await expect(preview.locator("body")).toHaveText(markupNote);
        await expect(preview.locator("b, script")).toHaveCount(0);
      }
    }
    await preview.close();
  });
}

test("an unsupported notebook attachment is refused and the saved notebook stays as it was", async ({
  page,
}) => {
  const title = `Refused attachment ${Date.now()}`;
  const notebookId = await seedNotebook(page, title);
  await openAttachments(page, notebookId);
  await page
    .locator('input[type="file"]')
    .setInputFiles({ name: "gel.png", mimeType: "image/png", buffer: png });
  await saveNotebook(page, notebookId);

  await openAttachments(page, notebookId);
  await page.locator('input[type="file"]').setInputFiles({
    name: "diagram.svg",
    mimeType: "image/svg+xml",
    buffer: Buffer.from(
      '<svg xmlns="http://www.w3.org/2000/svg" onload="notAllowed()"/>',
    ),
  });
  await saveNotebook(page, notebookId);
  await expect(page.getByText("Error While saving")).toBeVisible();

  await openAttachments(page, notebookId);
  await expect(
    page.getByTestId("notebook-attachment").filter({ hasText: "gel.png" }),
  ).toBeVisible();
  await expect(page.getByText("diagram.svg")).toHaveCount(0);
  await page.getByRole("tab", { name: "Entry Details" }).click();
  await expect(page.locator("#entryTitle")).toHaveValue(title);
});
