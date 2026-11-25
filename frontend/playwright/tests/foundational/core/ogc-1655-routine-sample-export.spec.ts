import { test, expect } from "../../../helpers/test-base";
import type { Page } from "@playwright/test";

/**
 * OGC-1655: the shared "Routine sample export" reproduces the Routine CSV
 * Report in Custom Data Export. It opens dated by order entry, its columns
 * follow the old file's order, and the fixture order REPORTING-MVP-REPEAT
 * (entered 2026-05-05 08:00, src/test/resources/fixtures) prints its requesting
 * facility, its entry time and the patient's age at collection.
 */

const ROUTINE_HEADERS = [
  "Accession Number",
  "Lab Unit",
  "National ID",
  "Sex",
  "Date of Birth",
  "Age at collection (years)",
  "Age at collection (months)",
  "Age at collection (weeks)",
  "Received Date",
  "Date Entered",
  "Collection Date",
  "Requesting Facility Code",
  "Requesting Facility",
  "Program",
  "Order Status",
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

test("the Routine sample export runs by entry date with the Routine CSV's columns", async ({
  page,
}, testInfo) => {
  testInfo.setTimeout(120_000);
  const errors: string[] = [];
  page.on("pageerror", (error) => errors.push(error.message));
  await page.goto("/reports/custom-data-export");
  await page
    .getByRole("button", { name: "Shared reports", exact: true })
    .click();
  await page
    .getByRole("searchbox", { name: "Search shared reports", exact: true })
    .fill("Routine sample export");
  await page
    .getByRole("article", { name: "Routine sample export", exact: true })
    .getByRole("button", { name: "Use report", exact: true })
    .click();
  await expect(page.getByLabel("Date basis")).toHaveValue("orderEnteredDate");
  await expect(page.getByText(/Uses order entry dates in/)).toBeVisible();
  await page.getByLabel("Date from", { exact: true }).fill("2026-05-05");
  await page.getByLabel("Date to", { exact: true }).fill("2026-05-05");
  await page
    .getByRole("button", { name: "Next: Review & Submit", exact: true })
    .click();

  const [headers, ...records] = await download(page);

  expect(headers.slice(0, ROUTINE_HEADERS.length)).toEqual(ROUTINE_HEADERS);
  expect(headers).toContain("Viral Load");
  const row = records.find((cells) => cells[0] === "REPORTING-MVP-REPEAT");
  expect(row, JSON.stringify(records.map((cells) => cells[0]))).toBeTruthy();
  const at = (label: string) => row![headers.indexOf(label)];
  expect(at("Date Entered")).toMatch(/^2026-05-05T\d\d:00/);
  expect(at("Requesting Facility")).toBe("Synthetic Location Clinic");
  expect(at("Requesting Facility Code")).toBe("SYN-LOC");
  expect(errors).toEqual([]);
});
