import { test, expect } from "../../../helpers/test-base";
import type { Page } from "@playwright/test";

/**
 * OGC-1652: Custom Data Export says where a specimen came from. Every report
 * type offers a Location group, and the shared "Referrals by location" report
 * prints the requesting facility, its code, the ward and the areas above the
 * facility, nearest first. src/test/resources/fixtures/reporting-locations.sql
 * places the REPORTING-MVP-REPEAT order at Synthetic Location Clinic (SYN-LOC),
 * Synthetic Maternity Ward, in Synthetic District of Synthetic Region.
 */

const LOCATION = [
  "Requesting Facility",
  "Requesting Facility Code",
  "Ward / Dept",
];

let browserErrors: string[];
test.beforeEach(async ({ page }) => {
  browserErrors = [];
  page.on("pageerror", (error) => browserErrors.push(error.message));
});
test.afterEach(() => {
  expect(browserErrors).toEqual([]);
});

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

async function download(page: Page, count: number) {
  await page.getByRole("button", { name: "Generate CSV", exact: true }).click();
  const current = page.getByRole("region", { name: "Your current report" });
  const link = current.getByRole("link", { name: "Download CSV" });
  await expect(link).toBeVisible({ timeout: 20_000 });
  await expect(current).toContainText(`${count} rows`);
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

test.describe("OGC-1652 Location columns in Custom Data Export", () => {
  test("every report type offers where the specimen came from", async ({
    page,
  }) => {
    await page.goto("/reports/custom-data-export");
    await page
      .getByRole("button", { name: "Start a new export", exact: true })
      .click();
    for (const type of [
      /^Sample & Testing/,
      /^Referrals/,
      /^Non-Conformance/,
    ]) {
      await page.getByRole("radio", { name: type }).click();
      const available = page.getByRole("region", { name: "Available fields" });
      const group = available.getByRole("button", {
        name: "Location",
        exact: true,
      });
      await expect(group).toBeVisible();
      if ((await group.getAttribute("aria-expanded")) !== "true")
        await group.click();
      for (const label of LOCATION)
        await expect(
          available.getByRole("button", { name: `Add ${label}`, exact: true }),
        ).toBeVisible();
      await expect(
        available.getByRole("button", {
          name: "Add Health District",
          exact: true,
        }),
      ).toBeVisible();
    }
  });

  test("Referrals by location prints the facility, its ward and its areas on every row", async ({
    page,
  }, testInfo) => {
    testInfo.setTimeout(90_000);
    await page.goto("/reports/custom-data-export");
    await page
      .getByRole("button", { name: "Shared reports", exact: true })
      .click();
    await page
      .getByRole("searchbox", { name: "Search shared reports", exact: true })
      .fill("Referrals by location");
    await page
      .getByRole("article", { name: "Referrals by location", exact: true })
      .getByRole("button", { name: "Use report", exact: true })
      .click();
    await page.getByLabel("Date from", { exact: true }).fill("2026-05-07");
    await page.getByLabel("Date to", { exact: true }).fill("2026-05-07");
    await page
      .getByRole("button", { name: "Next: Review & Submit", exact: true })
      .click();

    const [headers, ...records] = await download(page, 3);

    const at = (label: string) => headers.indexOf(label);
    for (const label of [...LOCATION, "Health District", "Health Region"])
      expect(headers, label).toContain(label);
    expect(at("Health District")).toBeLessThan(at("Health Region"));
    for (const row of records) {
      expect(row[at("Accession Number")]).toBe("REPORTING-MVP-REPEAT");
      expect([
        row[at("Requesting Facility")],
        row[at("Requesting Facility Code")],
        row[at("Ward / Dept")],
        row[at("Health District")],
        row[at("Health Region")],
      ]).toEqual([
        "Synthetic Location Clinic",
        "SYN-LOC",
        "Synthetic Maternity Ward",
        "Synthetic District",
        "Synthetic Region",
      ]);
    }
  });
});
