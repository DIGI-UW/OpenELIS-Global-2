import { readFile } from "node:fs/promises";
import type { Locator, Page, TestInfo } from "@playwright/test";
import { test, expect } from "../../../helpers/test-base";
import { createDemoPresentation } from "../../../helpers/demo-presentation";

// OGC-479 / OGC-481 / OGC-483, openelis-work 5b2df7e34f.
// The shared fixture loader supplies these public synthetic records. This is
// representative implementation proof; detailed edge cases stay in core-app.
const cases = [
  {
    name: "Spreadsheet repeats and turnaround",
    source: "Sample & Testing",
    detailed: false,
    date: "2026-05-06",
    columns: [
      "Accession Number",
      "Viral Load",
      "Viral Load — Resulted to Validated (min)",
    ],
    rows: [
      ["REPORTING-MVP-TURNAROUND", "450", "30"],
      ["REPORTING-MVP-TURNAROUND", "450", "90"],
    ],
    stories: ["RPT-S01", "RPT-S02", "RPT-S04", "RPT-S05"],
  },
  {
    name: "Detailed results retain separate identities",
    source: "Sample & Testing",
    detailed: true,
    date: "2026-05-06",
    columns: [
      "Accession Number",
      "Result Value",
      "Resulted to Validated (min)",
      "Result ID",
    ],
    rows: [
      ["REPORTING-MVP-TURNAROUND", "450", "30"],
      ["REPORTING-MVP-TURNAROUND", "450", "90"],
    ],
    stories: ["RPT-S01", "RPT-S02", "RPT-S04", "RPT-S05"],
  },
  {
    name: "Referrals preserve returns and pending work",
    source: "Referrals",
    detailed: false,
    date: "2026-05-07",
    columns: ["Accession Number", "Referral Result Value", "Referral Status"],
    rows: [
      ["REPORTING-MVP-REPEAT", "450", "COMPLETED"],
      ["REPORTING-MVP-REPEAT", "450", "COMPLETED"],
      ["REPORTING-MVP-REPEAT", "", "REQUESTED"],
    ],
    stories: ["RPT-S02", "RPT-S03", "RPT-S04"],
  },
];

async function download(
  page: Page,
  link: Locator,
  info: TestInfo,
  name: string,
) {
  const received = page.waitForEvent("download");
  await link.click();
  const file = await received;
  expect(await file.failure()).toBeNull();
  const path = info.outputPath(name);
  await file.saveAs(path);
  const bytes = await readFile(path);
  await info.attach(name, { body: bytes, contentType: "text/csv" });
  return bytes;
}

async function openLibrary(page: Page) {
  await page
    .getByRole("button", { name: "Export overview", exact: true })
    .click();
  await page
    .getByRole("button", { name: "Shared reports", exact: true })
    .click();
}

for (const scenario of cases) {
  test(`OGC-479/481/483: ${scenario.name} through saved settings and the queue`, async ({
    page,
  }, testInfo) => {
    testInfo.setTimeout(120_000);
    const demo = createDemoPresentation(page, testInfo);
    const name = `E2E proof ${scenario.name} ${Date.now()}`;
    await page.setViewportSize({ width: 1280, height: 720 });
    await page.goto("/reports/custom-data-export");
    await expect(
      page.getByRole("button", { name: "Start a new export", exact: true }),
    ).toBeVisible();
    await demo.chapter({
      eyebrow: "OGC-479 / 481 / 483 · Implementation proof",
      title: scenario.name,
      subtitle:
        "Build a report, reuse its settings and download the verified results.",
      durationMs: 4000,
    });
    await demo.evidence(`${scenario.date}-${scenario.detailed}-01-overview`);

    await test.step("Choose the source and ordered columns", async () => {
      await page
        .getByRole("button", { name: "Start a new export", exact: true })
        .click();
      await page
        .getByRole("radio", { name: new RegExp(`^${scenario.source}`) })
        .click();
      if (scenario.detailed) {
        await page.getByRole("combobox", { name: "CSV layout" }).click();
        await page
          .getByRole("option", {
            name: "Detailed list — results in rows",
            exact: true,
          })
          .click();
      }
      for (const column of scenario.columns) {
        await page
          .getByRole("searchbox", { name: "Find a field", exact: true })
          .fill(column);
        await page
          .getByRole("button", { name: `Add ${column}`, exact: true })
          .click();
        await page
          .getByRole("button", { name: "Clear search", exact: true })
          .click();
      }
      await expect(
        page.getByRole("heading", {
          name: `Your CSV columns (${scenario.columns.length})`,
          exact: true,
        }),
      ).toBeVisible();
      await demo.scene("Choose the columns needed for this report");
      await demo.evidence(`${scenario.date}-${scenario.detailed}-02-columns`);
    });

    await test.step("Choose a period and save the reviewed report settings", async () => {
      await page
        .getByRole("button", { name: "Next: Set Filters", exact: true })
        .click();
      await page.getByLabel("Date from", { exact: true }).fill(scenario.date);
      await page.getByLabel("Date to", { exact: true }).fill(scenario.date);
      await demo.evidence(`${scenario.date}-${scenario.detailed}-03-filters`);
      await page
        .getByRole("button", { name: "Next: Review & Submit", exact: true })
        .click();
      await expect(
        page
          .getByRole("list", { name: "CSV columns in order" })
          .getByRole("listitem"),
      ).toHaveText(scenario.columns);
      // Carbon's visible label owns the hidden checkbox's pointer interaction.
      await page.locator('label[for="reporting-save-later"]').click();
      await page
        .getByRole("textbox", { name: "Report name", exact: true })
        .fill(name);
      await page
        .getByRole("button", { name: "Save report settings", exact: true })
        .click();
      await expect(
        page.getByText(`Saved as ${name}.`, { exact: true }),
      ).toBeVisible();
      await demo.evidence(`${scenario.date}-${scenario.detailed}-04-review`);
    });

    await demo.chapter({
      eyebrow: "OGC-483 · Reuse a report",
      title: "Saved choices, fresh reporting dates",
      subtitle:
        "The shared definition keeps its columns; each run chooses a new period.",
    });
    await openLibrary(page);
    await page
      .getByRole("searchbox", { name: "Search shared reports", exact: true })
      .fill(name);
    const saved = page.getByRole("article", { name, exact: true });
    await expect(saved).toBeVisible();
    await demo.evidence(`${scenario.date}-${scenario.detailed}-05-shared`);
    await saved
      .getByRole("button", { name: "Use report", exact: true })
      .click();
    await expect(page.getByLabel("Date from", { exact: true })).toHaveValue("");
    await expect(page.getByLabel("Date to", { exact: true })).toHaveValue("");
    await page.getByLabel("Date from", { exact: true }).fill(scenario.date);
    await page.getByLabel("Date to", { exact: true }).fill(scenario.date);
    await page
      .getByRole("button", { name: "Next: Review & Submit", exact: true })
      .click();
    await expect(
      page
        .getByRole("list", { name: "CSV columns in order" })
        .getByRole("listitem"),
    ).toHaveText(scenario.columns);
    await page
      .getByRole("button", { name: "Generate CSV", exact: true })
      .click();
    const current = page.getByRole("region", { name: "Your current report" });
    const link = current.getByRole("link", {
      name: "Download CSV",
      exact: true,
    });
    await expect(link).toBeVisible({ timeout: 20_000 });
    await expect(current).toContainText(`${scenario.rows.length} rows`);
    const bytes = await download(page, link, testInfo, "report.csv");
    expect(bytes.subarray(0, 3)).toEqual(Buffer.from([239, 187, 191]));
    // These selected fixture fields contain no commas, quotes or newlines;
    // general CSV escaping has separate writer tests.
    const [headers, ...rows] = bytes
      .subarray(3)
      .toString("utf8")
      .trimEnd()
      .split("\r\n")
      .map((line) => line.split(","));
    expect(headers).toEqual(scenario.columns);
    expect(rows.map((row) => row.slice(0, 3))).toEqual(scenario.rows);
    if (scenario.detailed) {
      expect(rows.every((row) => row[3])).toBe(true);
      expect(new Set(rows.map((row) => row[3])).size).toBe(2);
    }
    await demo.scene("Download checked against the synthetic fixture");
    await demo.evidence(`${scenario.date}-${scenario.detailed}-06-ready`);

    await test.step("Return through the queue and download identical stored bytes", async () => {
      await expect(link).toHaveAttribute("href", /\/jobs\/[^/]+\/download$/);
      const href = await link.getAttribute("href");
      await page
        .getByRole("button", { name: "My Report Queue", exact: true })
        .click();
      await expect(page).toHaveURL(/view=queue/);
      await page.reload();
      const stored = page.locator(`a[href="${href}"]`);
      await expect(stored).toBeVisible();
      expect(
        await download(page, stored, testInfo, "queue-download.csv"),
      ).toEqual(bytes);
      await demo.evidence(`${scenario.date}-${scenario.detailed}-07-queue`);
    });

    // Remove only this run's unique saved definition through its ordinary UI.
    await openLibrary(page);
    await page
      .getByRole("searchbox", { name: "Search shared reports", exact: true })
      .fill(name);
    await saved.getByRole("button", { name: /Delete shared report/ }).click();
    const dialog = page.getByRole("dialog", {
      name: "Delete shared report",
      exact: true,
    });
    await dialog.getByRole("button", { name: /Delete$/ }).click();
    await expect(dialog).toBeHidden();
    await expect(saved).toBeHidden();
    await testInfo.attach("story-coverage.json", {
      body: JSON.stringify({
        originalStories: ["OGC-479", "OGC-481", "OGC-483"],
        walkthroughs: scenario.stories,
        sourceRevision: "5b2df7e34ff5ad1f983f24c0e9e0ba4db5e8697f",
        humanAcceptance: "not assessed",
      }),
      contentType: "application/json",
    });
    await demo.chapter({
      eyebrow: "Automated evidence",
      title: "Report and repeat download verified",
      subtitle:
        "Original workflow exercised. Human UAT acceptance remains separate.",
      durationMs: 4000,
    });
  });
}
