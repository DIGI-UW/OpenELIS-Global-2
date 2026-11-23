import { test, expect } from "../../../helpers/test-base";
import type { Page } from "@playwright/test";

/**
 * OGC-1656: the shared "Sample rejections" report reproduces the CSV Rejection
 * Report in Custom Data Export, with its columns in the old file's order. The
 * fixture order REPORTING-MVP-NCE (src/test/resources/fixtures, received
 * 2026-05-10) carries a rejected specimen and non-conformance events in that
 * period. Like the old file, the report lists the rejection only, one row per
 * test; an event would be a row without a test.
 */

const REJECTION_HEADERS = [
  "Accession Number",
  "Request Date",
  "Requesting Facility",
  "Sample Type",
  "Test",
  "Rejection Reason",
  "Rejection Date",
];

function parseCsv(text: string): string[][] {
  const rows: string[][] = [];
  let row: string[] = [],
    cell = "",
    quoted = false;
  for (let i = 0; i < text.length; i++) {
    const char = text[i];
    if (char === '"') {
      if (quoted && text[i + 1] === '"') {
        cell += '"';
        i++;
      } else quoted = !quoted;
    } else if (!quoted && char === ",") {
      row.push(cell);
      cell = "";
    } else if (!quoted && char === "\r" && text[i + 1] === "\n") {
      row.push(cell);
      rows.push(row);
      row = [];
      cell = "";
      i++;
    } else cell += char;
  }
  return rows;
}

async function download(page: Page) {
  await page.getByRole("button", { name: "Generate CSV", exact: true }).click();
  const current = page.getByRole("region", { name: "Your current report" });
  const link = current.getByRole("link", { name: "Download CSV" });
  await expect(link).toBeVisible({ timeout: 30_000 });
  const completed = page.waitForEvent("download");
  await link.click();
  const file = await completed;
  expect(await file.failure()).toBeNull();
  const chunks: Buffer[] = [];
  for await (const chunk of await file.createReadStream())
    chunks.push(Buffer.from(chunk));
  const bytes = Buffer.concat(chunks);
  await test
    .info()
    .attach("download.csv", { body: bytes, contentType: "text/csv" });
  return parseCsv(bytes.subarray(3).toString("utf8"));
}

test("Sample rejections runs with the Rejection Report's columns", async ({
  page,
}, testInfo) => {
  testInfo.setTimeout(90_000);
  const errors: string[] = [];
  page.on("pageerror", (error) => errors.push(error.message));
  await page.goto("/reports/custom-data-export");
  await page
    .getByRole("button", { name: "Shared reports", exact: true })
    .click();
  await page
    .getByRole("searchbox", { name: "Search shared reports", exact: true })
    .fill("Sample rejections");
  await page
    .getByRole("article", { name: "Sample rejections", exact: true })
    .getByRole("button", { name: "Use report", exact: true })
    .click();
  await page.getByLabel("Date from", { exact: true }).fill("2026-05-10");
  await page.getByLabel("Date to", { exact: true }).fill("2026-05-12");
  await page
    .getByRole("button", { name: "Next: Review & Submit", exact: true })
    .click();

  const [headers, ...records] = await download(page);

  expect(headers).toEqual(REJECTION_HEADERS);
  const rows = records.filter((cells) => cells[0] === "REPORTING-MVP-NCE");
  expect(rows.length).toBeGreaterThan(0);
  expect(rows.every((cells) => cells[1] === "2026-05-10")).toBe(true);
  expect(rows.every((cells) => cells[4] !== "")).toBe(true);
  expect(errors).toEqual([]);
});
