import { test, expect } from "../../../helpers/test-base";
import AxeBuilder from "@axe-core/playwright";
import type { Page, TestInfo } from "@playwright/test";

// src/test/resources/fixtures/reporting-repeated-results.sql is loaded by the
// shared fixture loader. Its two equal readings have distinct result identities.
const accession = "REPORTING-MVP-REPEAT";
let browserErrors: string[];
test.beforeEach(async ({ page }) => {
  browserErrors = [];
  page.on("pageerror", (error) => browserErrors.push(error.message));
});
test.afterEach(() => {
  expect(browserErrors).toEqual([]);
});

async function createReportUsers(page: Page, usernames: string[]) {
  await page.goto("/");
  const endpoint = "/api/OpenELIS-Global/rest/UnifiedSystemUser";
  const response = await page.request.get(endpoint);
  expect(response.status()).toBe(200);
  const form = await response.json();
  const reports = form.labUnitRoles.find(
    (role: { roleName: string }) => role.roleName === "Reports",
  );
  expect(reports?.roleId).toBeTruthy();
  const password = process.env.TEST_PASS;
  expect(
    password,
    "TEST_PASS is required for the disposable report users",
  ).toBeTruthy();
  const csrf = await page.evaluate(() => localStorage.getItem("CSRF") || "");
  for (const username of usernames) {
    const created = await page.request.post(endpoint, {
      headers: { "X-CSRF-Token": csrf },
      data: {
        userLoginName: username,
        userPassword: password,
        confirmPassword: password,
        userFirstName: "Reporting",
        // User management rejects duplicate first/last-name pairs.
        userLastName: username,
        expirationDate: form.expirationDate,
        timeout: form.timeout,
        accountActive: "Y",
        accountDisabled: "N",
        accountLocked: "N",
        allowCopyUserRoles: "N",
        selectedRoles: [],
        selectedTestSectionLabUnits: { AllLabUnits: [reports.roleId] },
      },
    });
    expect(created.status()).toBe(200);
    expect(await created.json()).toEqual({
      forward: "redirect:/UnifiedSystemUser",
    });
  }
}

async function signInAsReportUser(page: Page, username: string) {
  // A clean browser state proves shared definitions come from the server,
  // rather than the previous user's locally retained draft.
  // Establish the origin without starting dashboard requests during teardown.
  await page.goto("/manifest.json");
  await page.evaluate(() => {
    localStorage.clear();
    sessionStorage.clear();
  });
  await page.context().clearCookies();
  await page.goto("/login");
  await page.locator("#loginName").fill(username);
  await page.locator("#password").fill(process.env.TEST_PASS!);
  await page.locator('[data-cy="loginButton"]').click();
  await expect(page).toHaveURL(/\/$/);
  await expect(
    page.getByRole("navigation", { name: "Side navigation" }),
  ).toBeVisible();
  await page.getByRole("button", { name: "Reports", exact: true }).click();
  await page
    .getByRole("link", { name: "Custom Data Export", exact: true })
    .click();
  await expect(
    page.getByRole("heading", { name: "Custom Data Export", exact: true }),
  ).toBeVisible();
}

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
  expect(quoted).toBe(false);
  expect(cell).toBe("");
  expect(row).toEqual([]);
  return rows;
}

async function addField(page: Page, label: string) {
  const search = page.getByRole("searchbox", {
    name: "Find a field",
    exact: true,
  });
  await search.fill(label);
  await page.getByRole("button", { name: `Add ${label}`, exact: true }).click();
  await expect(
    page.getByRole("button", { name: `Added ${label}`, exact: true }),
  ).toHaveAttribute("aria-disabled", "true");
  await page.getByRole("button", { name: "Clear search", exact: true }).click();
}
async function startReport(page: Page) {
  await page
    .getByRole("button", { name: "Start a new export", exact: true })
    .click();
  await page.getByRole("radio", { name: /^Sample & Testing/ }).click();
}
async function setPeriod(page: Page, date = "2026-05-05") {
  await page.getByLabel("Date from", { exact: true }).fill(date);
  await page.getByLabel("Date to", { exact: true }).fill(date);
}
async function openBuilder(page: Page, date = "2026-05-05") {
  await page.goto("/CustomDataExport");
  await startReport(page);
  for (const label of ["Accession Number", "Specimen ID", "Viral Load"])
    await addField(page, label);
  await page
    .getByRole("button", { name: "Next: Set Filters", exact: true })
    .click();
  await setPeriod(page, date);
  await page.getByRole("button", { name: "Back", exact: true }).click();
}
async function reviewReport(page: Page) {
  const columnsNext = page.getByRole("button", {
    name: "Next: Set Filters",
    exact: true,
  });
  if (await columnsNext.isVisible()) await columnsNext.click();
  const filtersNext = page.getByRole("button", {
    name: "Next: Review & Submit",
    exact: true,
  });
  if (await filtersNext.isVisible()) await filtersNext.click();
  await expect(
    page.getByRole("button", { name: "Generate CSV", exact: true }),
  ).toBeEnabled();
}
async function downloadReport(page: Page, count: number) {
  await reviewReport(page);
  const headers = await page
    .getByRole("list", { name: "CSV columns in order" })
    .getByRole("listitem")
    .allTextContents();
  await page.getByRole("button", { name: "Generate CSV", exact: true }).click();
  const current = page.getByRole("region", { name: "Your current report" });
  const link = current.getByRole("link", { name: "Download CSV" });
  await expect(link).toBeVisible({ timeout: 20_000 });
  await expect(current).toContainText(`${count} rows`);
  const completed = page.waitForEvent("download");
  await link.click();
  const download = await completed;
  expect(await download.failure()).toBeNull();
  const stream = await download.createReadStream();
  const chunks: Buffer[] = [];
  for await (const chunk of stream) chunks.push(Buffer.from(chunk));
  const bytes = Buffer.concat(chunks);
  expect([...bytes.subarray(0, 3)]).toEqual([239, 187, 191]);
  const rows = parseCsv(bytes.subarray(3).toString("utf8"));
  expect(rows[0]).toEqual(headers);
  expect(rows.slice(1)).toHaveLength(count);
  await test
    .info()
    .attach("download.csv", { body: bytes, contentType: "text/csv" });
  await page.screenshot({
    path: test.info().outputPath("report-ready.png"),
    fullPage: true,
  });
  return { headers, records: rows.slice(1) };
}
async function detailedLayout(page: Page) {
  await page.getByRole("combobox", { name: "CSV layout" }).click();
  await page
    .getByRole("option", {
      name: "Detailed list — results in rows",
      exact: true,
    })
    .click();
  for (const label of ["Accession Number", "Result Value", "Result ID"])
    await addField(page, label);
}
async function configuredReport(page: Page, name: string) {
  await page
    .getByRole("button", { name: "Change type and clear fields", exact: true })
    .click();
  await page.getByRole("combobox", { name: "Configured reports" }).click();
  await page.getByRole("option", { name, exact: true }).click();
}
async function savedLibrary(page: Page) {
  await page
    .getByRole("button", { name: "Export overview", exact: true })
    .click();
  await page
    .getByRole("button", { name: "Shared reports", exact: true })
    .click();
}
async function saveReport(page: Page, name: string) {
  await reviewReport(page);
  await expect(
    page.getByRole("checkbox", {
      name: "Save these report settings for later",
      exact: true,
    }),
  ).not.toBeChecked();
  // Carbon delegates the hidden input's pointer interaction to its visible label.
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
}

test("spreadsheet preserves both identical readings in an instance-configured test column", async ({
  page,
}) => {
  await openBuilder(page);
  await expect(
    page.getByRole("combobox", { name: "CSV layout" }),
  ).toContainText("Spreadsheet");
  await page
    .getByRole("button", { name: "Move Specimen ID up", exact: true })
    .click();
  const { headers, records } = await downloadReport(page, 2);
  expect(headers).toEqual(["Specimen ID", "Accession Number", "Viral Load"]);
  expect(
    records.map((row) => row[headers.indexOf("Accession Number")]),
  ).toEqual([accession, accession]);
  expect(records.map((row) => row[headers.indexOf("Viral Load")])).toEqual([
    "450",
    "450",
  ]);
  expect(new Set(records.map((row) => row[0])).size).toBe(1);
});

// Uses reporting-field-values.sql: real configured text/dictionary options and
// two separate, equal results for each specimen on a dedicated collection date.
for (const layout of ["SPREADSHEET", "RESULT_LIST"] as const) {
  test(`${layout} downloads complete text and dictionary labels without losing repeats`, async ({
    page,
  }) => {
    await page.goto("/CustomDataExport");
    await startReport(page);
    if (layout === "SPREADSHEET") {
      for (const label of ["Accession Number", "Viral Load", "DNA PCR"])
        await addField(page, label);
    } else {
      await detailedLayout(page);
      await addField(page, "Test Name");
    }
    await page.evaluate(async () => {
      await document.fonts.ready;
      window.scrollTo(0, 0);
    });
    await expect.poll(() => page.evaluate(() => window.scrollY)).toBe(0);
    await page.screenshot({
      path: test.info().outputPath("field-columns-desktop.png"),
      fullPage: true,
    });
    await page.setViewportSize({ width: 390, height: 844 });
    await expect
      .poll(() => page.evaluate(() => document.documentElement.scrollWidth))
      .toBeLessThanOrEqual(390);
    await page.evaluate(async () => {
      await document.fonts.ready;
      window.scrollTo(0, 0);
    });
    await expect.poll(() => page.evaluate(() => window.scrollY)).toBe(0);
    await page.screenshot({
      path: test.info().outputPath("field-columns-narrow.png"),
      fullPage: true,
    });
    await page
      .getByRole("button", { name: "Next: Set Filters", exact: true })
      .click();
    await setPeriod(page, "2026-05-08");
    const { headers, records } = await downloadReport(page, 4);
    const textValue = (
      'Operator says "repeat, please"\r\n<tag> & ' +
      "untruncated text ".repeat(20)
    ).slice(0, 200);
    const values = records.map((row) => {
      const name = row[headers.indexOf("Accession Number")];
      const field =
        layout === "RESULT_LIST"
          ? "Result Value"
          : name === "REPORTING-MVP-TEXT"
            ? "Viral Load"
            : "DNA PCR";
      if (layout === "SPREADSHEET") {
        expect(
          row[
            headers.indexOf(
              name === "REPORTING-MVP-TEXT" ? "DNA PCR" : "Viral Load",
            )
          ],
        ).toBe("");
      } else {
        expect(row[headers.indexOf("Test Name")]).toBe(
          name === "REPORTING-MVP-TEXT" ? "Viral Load" : "DNA PCR",
        );
      }
      return [name, row[headers.indexOf(field)]];
    });
    expect(values.sort()).toEqual(
      [
        ["REPORTING-MVP-TEXT", textValue],
        ["REPORTING-MVP-TEXT", textValue],
        ["REPORTING-MVP-DICTIONARY", "Positive"],
        ["REPORTING-MVP-DICTIONARY", "Positive"],
      ].sort(),
    );
    if (layout === "RESULT_LIST")
      expect(
        new Set(records.map((row) => row[headers.indexOf("Result ID")])).size,
      ).toBe(4);
    await expect
      .poll(() => page.evaluate(() => document.documentElement.scrollWidth))
      .toBeLessThanOrEqual(390);
  });
}

test("turnaround beside a test preserves each repeated result's own duration", async ({
  page,
}) => {
  await openBuilder(page, "2026-05-06");
  const duration = "Viral Load — Resulted to Validated (min)";
  await addField(page, duration);
  const { headers, records } = await downloadReport(page, 2);
  const testColumn = headers.indexOf("Viral Load");
  expect(headers[testColumn + 1]).toBe(duration);
  expect(
    records.map((row) => [
      row[headers.indexOf("Accession Number")],
      row[testColumn],
      row[testColumn + 1],
    ]),
  ).toEqual([
    ["REPORTING-MVP-TURNAROUND", "450", "30"],
    ["REPORTING-MVP-TURNAROUND", "450", "90"],
  ]);
  await page.getByRole("button", { name: "Edit report", exact: true }).click();
  await detailedLayout(page);
  await addField(page, "Resulted to Validated (min)");
  const detail = await downloadReport(page, 2);
  expect(
    detail.records.map((row) => [
      row[detail.headers.indexOf("Result Value")],
      row[detail.headers.indexOf("Resulted to Validated (min)")],
    ]),
  ).toEqual([
    ["450", "30"],
    ["450", "90"],
  ]);
});

test("detailed layout exports both result identities and keeps the chosen period", async ({
  page,
}) => {
  await openBuilder(page);
  await detailedLayout(page);
  await page
    .getByRole("button", { name: "Next: Set Filters", exact: true })
    .click();
  await expect(page.getByLabel("Date from", { exact: true })).toHaveValue(
    "2026-05-05",
  );
  const { headers, records } = await downloadReport(page, 2);
  expect(
    records.map((row) => row[headers.indexOf("Accession Number")]),
  ).toEqual([accession, accession]);
  expect(records.map((row) => row[headers.indexOf("Result Value")])).toEqual([
    "450",
    "450",
  ]);
  const identities = records.map((row) => row[headers.indexOf("Result ID")]);
  expect(identities.every(Boolean)).toBe(true);
  expect(new Set(identities).size).toBe(2);
});

test("another configured report uses explicit column choices and the same builder and queue", async ({
  page,
}) => {
  await openBuilder(page);
  await configuredReport(page, "Sample summary");
  await expect(
    page.getByRole("heading", { name: "Your CSV columns (0)", exact: true }),
  ).toBeVisible();
  for (const label of ["Specimen ID", "Accession Number"])
    await addField(page, label);
  const preview = page.getByRole("region", { name: "CSV header preview" });
  await expect(preview.getByRole("columnheader")).toHaveText([
    "Specimen ID",
    "Accession Number",
  ]);
  await page
    .getByRole("searchbox", { name: "Find a field", exact: true })
    .fill("Patient Name");
  await expect(
    page.getByRole("button", { name: "Add Patient Name", exact: true }),
  ).toHaveCount(0);
  await addField(page, "Viral Load");
  const { headers, records } = await downloadReport(page, 2);
  expect(headers).toEqual(["Specimen ID", "Accession Number", "Viral Load"]);
  expect(records.map((row) => row.slice(1))).toEqual([
    [accession, "450"],
    [accession, "450"],
  ]);
  await expect(
    page.getByRole("region", { name: "Your current report" }),
  ).toContainText("Sample summary");
  await page
    .getByRole("button", { name: "My Report Queue", exact: true })
    .click();
  await expect(
    page.getByRole("region", { name: "My Report Queue" }),
  ).toContainText("Sample summary");
});

test("an empty period produces a header-only download and an explicit zero-row result", async ({
  page,
}) => {
  await openBuilder(page, "2026-05-07");
  const { records } = await downloadReport(page, 0);
  expect(records).toEqual([]);
});

test("switching to a report without optional filters cannot retain a hidden test restriction", async ({
  page,
}) => {
  await openBuilder(page);
  await page
    .getByRole("button", { name: "Next: Set Filters", exact: true })
    .click();
  const tests = page.getByRole("combobox", { name: /^Tests/ });
  await tests.click();
  const testOptions = page
    .getByRole("listbox", { name: "Tests", exact: true })
    .getByRole("option");
  const excluded = (await testOptions.allTextContents())
    .map((label) => label.trim())
    .find((label) => label && label !== "Viral Load");
  expect(excluded).toBeTruthy();
  await testOptions.filter({ hasText: excluded! }).click();
  await tests.press("Escape");
  await page.getByRole("button", { name: "Back", exact: true }).click();
  await configuredReport(page, "Finalized sample summary");
  for (const label of ["Specimen ID", "Accession Number", "Viral Load"])
    await addField(page, label);
  await page
    .getByRole("button", { name: "Next: Set Filters", exact: true })
    .click();
  for (const name of [/^Lab sections/, /^Tests/, /^Result statuses/])
    await expect(page.getByRole("combobox", { name })).toHaveCount(0);
  await expect(
    page.getByText(
      "Some previous filters are unavailable for this report. Review the current filters before generating.",
      { exact: true },
    ),
  ).toBeVisible();
  const { headers, records } = await downloadReport(page, 2);
  expect(headers).toEqual(["Specimen ID", "Accession Number", "Viral Load"]);
  expect(records.map((row) => row.slice(1))).toEqual([
    [accession, "450"],
    [accession, "450"],
  ]);
  await page.getByRole("button", { name: "Edit report", exact: true }).click();
  await page
    .getByRole("button", { name: "Change type and clear fields", exact: true })
    .click();
  await page.getByRole("radio", { name: /^Sample & Testing/ }).click();
  await addField(page, "Accession Number");
  await page
    .getByRole("button", { name: "Next: Set Filters", exact: true })
    .click();
  await tests.click();
  await expect(
    page.getByRole("option", { name: excluded!, exact: true }),
  ).toHaveAttribute("aria-selected", "true");
});

test("Reports entry explains invalid periods and restores a reviewed draft with Back, Forward and reload", async ({
  page,
}, testInfo) => {
  testInfo.setTimeout(60_000);
  await page.goto("/");
  await page.getByRole("button", { name: "Reports", exact: true }).click();
  await page
    .getByRole("link", { name: "Custom Data Export", exact: true })
    .click();
  await startReport(page);
  await addField(page, "Accession Number");
  await page
    .getByRole("button", { name: "Next: Set Filters", exact: true })
    .click();
  const from = page.getByLabel("Date from", { exact: true });
  const to = page.getByLabel("Date to", { exact: true });
  const review = page.getByRole("button", {
    name: "Next: Review & Submit",
    exact: true,
  });
  await review.click();
  await expect(
    page.getByText("Choose a start date.", { exact: true }),
  ).toBeVisible();
  // Exercise native keyboard entry as well as programmatic fill: committing
  // either date must preserve the other controlled input.
  for (const input of [from, to]) {
    await input.focus();
    await input.press("ArrowLeft");
    await input.press("ArrowLeft");
    await input.press("ArrowLeft");
    await input.pressSequentially("05052026");
    await input.press("Tab");
  }
  await expect(from).toHaveValue("2026-05-05");
  await expect(to).toHaveValue("2026-05-05");
  await from.fill("2026-01-01");
  await to.fill("2025-12-31");
  await expect(
    page.getByText("End date must be on or after the start date.", {
      exact: true,
    }),
  ).toBeVisible();
  await expect(review).toBeDisabled();
  await to.fill("2026-04-01");
  await expect(
    page.getByText(
      "Choose a period of 90 days or fewer, including both dates.",
      { exact: true },
    ),
  ).toBeVisible();
  await expect(review).toBeDisabled();
  await to.fill("2026-03-31");
  await expect(review).toBeEnabled();
  await setPeriod(page);
  await review.click();
  await expect(page).toHaveURL(/step=review/);
  const headers = await page
    .getByRole("list", { name: "CSV columns in order" })
    .getByRole("listitem")
    .allTextContents();
  await page
    .getByRole("button", { name: "My Report Queue", exact: true })
    .click();
  await expect(page).toHaveURL(/view=queue/);
  await page.goBack();
  await expect(
    page.getByRole("button", { name: "Generate CSV", exact: true }),
  ).toBeEnabled();
  await page.goForward();
  await expect(
    page.getByRole("heading", { name: "My Report Queue", exact: true }),
  ).toBeVisible();
  await page
    .getByRole("button", { name: "Continue current export", exact: true })
    .click();
  await page.reload();
  await expect(
    page.getByRole("button", { name: "Generate CSV", exact: true }),
  ).toBeEnabled();
  await expect(page.getByText(/2026-05-05 – 2026-05-05/)).toBeVisible();
  await expect(
    page
      .getByRole("list", { name: "CSV columns in order" })
      .getByRole("listitem"),
  ).toHaveText(headers);
});

test("a shared report reopens with fresh dates and supports confirmed update, copy, and delete", async ({
  page,
}, testInfo) => {
  testInfo.setTimeout(60_000);
  const reportName = `Reporting UAT ${Date.now()}`,
    copyName = `${reportName} copy`;
  await openBuilder(page);
  await saveReport(page, reportName);
  await savedLibrary(page);
  const search = page.getByRole("searchbox", {
    name: "Search shared reports",
    exact: true,
  });
  await search.fill(reportName);
  let card = page.getByRole("article", { name: reportName, exact: true });
  await card.getByRole("button", { name: "Use report", exact: true }).click();
  await expect(page.getByLabel("Date from", { exact: true })).toHaveValue("");
  await expect(page.getByLabel("Date to", { exact: true })).toHaveValue("");
  await expect(
    page.getByText("Choose fresh dates before running this report.", {
      exact: true,
    }),
  ).toBeVisible();
  await page.getByRole("button", { name: "Back", exact: true }).click();
  await addField(page, "Patient Name");
  await page
    .getByRole("button", { name: "Next: Set Filters", exact: true })
    .click();
  await setPeriod(page);
  await reviewReport(page);
  await page
    .getByRole("button", { name: "Update shared report", exact: true })
    .click();
  await page.getByRole("button", { name: "Update", exact: true }).click();
  await expect(
    page.getByText(`Updated ${reportName}.`, { exact: true }),
  ).toBeVisible();
  await page.getByRole("button", { name: "Save a copy", exact: true }).click();
  await page
    .getByRole("textbox", { name: "Report name", exact: true })
    .fill(copyName);
  await page
    .getByRole("button", { name: "Save shared report", exact: true })
    .click();
  await expect(
    page.getByText(`Saved as ${copyName}.`, { exact: true }),
  ).toBeVisible();
  for (const name of [reportName, copyName]) {
    await savedLibrary(page);
    await search.fill(name);
    card = page.getByRole("article", { name, exact: true });
    await expect(card).toBeVisible();
    await card.getByRole("button", { name: /Delete shared report/ }).click();
    await page.getByRole("button", { name: /Delete$/ }).click();
    await expect(card).toBeHidden();
  }
});

test("a saved detailed report reruns for a different period with fresh results", async ({
  page,
}, testInfo) => {
  testInfo.setTimeout(90_000);
  const name = `Reporting period rerun ${Date.now()}`;
  let savedId: string | null = null;
  const jobs: unknown[] = [];
  await openBuilder(page);
  await detailedLayout(page);
  await addField(page, "Resulted to Validated (min)");
  try {
    await saveReport(page, name);
    savedId = new URL(page.url()).searchParams.get("saved");
    expect(savedId).toBeTruthy();
    const definitionResponse = await page.request.get(
      `/api/OpenELIS-Global/rest/reports/data-export/saved-configs/${savedId}`,
    );
    expect(definitionResponse.status()).toBe(200);
    const definition = await definitionResponse.json();
    expect(definition.name).toBe(name);
    for (const day of ["2026-05-05", "2026-05-06"]) {
      if (day === "2026-05-06") {
        await savedLibrary(page);
        await page
          .getByRole("searchbox", {
            name: "Search shared reports",
            exact: true,
          })
          .fill(name);
        await page
          .getByRole("article", { name, exact: true })
          .getByRole("button", { name: "Use report", exact: true })
          .click();
        await expect(page.getByLabel("Date from", { exact: true })).toHaveValue(
          "",
        );
        await expect(page.getByLabel("Date to", { exact: true })).toHaveValue(
          "",
        );
        await setPeriod(page, day);
      }
      const { headers, records } = await downloadReport(page, 2);
      expect(headers).toEqual([
        "Accession Number",
        "Result Value",
        "Result ID",
        "Resulted to Validated (min)",
      ]);
      expect(records.map((row) => [row[0], row[1], row[3]])).toEqual(
        day === "2026-05-05"
          ? [
              ["REPORTING-MVP-REPEAT", "450", "0"],
              ["REPORTING-MVP-REPEAT", "450", "0"],
            ]
          : [
              ["REPORTING-MVP-TURNAROUND", "450", "30"],
              ["REPORTING-MVP-TURNAROUND", "450", "90"],
            ],
      );
      expect(new Set(records.map((row) => row[2])).size).toBe(2);
      const link = await page
        .getByRole("region", { name: "Your current report" })
        .getByRole("link", { name: "Download CSV" })
        .getAttribute("href");
      expect(link).toMatch(/\/jobs\/[^/]+\/download$/);
      const jobResponse = await page.request.get(
        link!.replace(/\/download$/, ""),
      );
      expect(jobResponse.status()).toBe(200);
      const job = await jobResponse.json();
      expect(job.state).toBe("READY");
      expect(job.request.filterSpec.dateFrom).toBe(day);
      expect(job.request.filterSpec.dateTo).toBe(day);
      jobs.push(job);
    }
    await testInfo.attach("saved-period-provenance.json", {
      body: Buffer.from(
        JSON.stringify(
          { baseUrl: testInfo.project.use.baseURL, definition, jobs },
          null,
          2,
        ),
      ),
      contentType: "application/json",
    });
  } finally {
    if (savedId) {
      const endpoint = `/api/OpenELIS-Global/rest/reports/data-export/saved-configs/${savedId}`;
      const current = await page.request.get(endpoint);
      expect(current.status()).toBe(200);
      const definition = await current.json();
      expect(definition.name).toBe(name);
      const csrf = await page.evaluate(
        () => localStorage.getItem("CSRF") || "",
      );
      const removed = await page.request.delete(
        `${endpoint}?expectedVersion=${encodeURIComponent(definition.version)}`,
        { headers: { "X-CSRF-Token": csrf } },
      );
      expect(removed.status()).toBe(204);
    }
  }
});

test("a stale shared-report editor keeps its draft and can save a separate copy", async ({
  page,
  context,
}, testInfo) => {
  testInfo.setTimeout(120_000);
  const reportName = `Reporting editors ${Date.now()}`;
  const copyName = `${reportName} copy`;
  const created = new Map<string, string>();
  const other = await context.newPage();
  other.on("pageerror", (error) => browserErrors.push(error.message));
  try {
    await openBuilder(page);
    await saveReport(page, reportName);
    const originalId = new URL(page.url()).searchParams.get("saved");
    expect(originalId).toBeTruthy();
    created.set(originalId!, reportName);
    await savedLibrary(page);
    await other.goto(page.url());
    // Both editors open the same version before either changes the definition.
    for (const editor of [page, other]) {
      await editor
        .getByRole("searchbox", { name: "Search shared reports", exact: true })
        .fill(reportName);
      await editor
        .getByRole("article", { name: reportName, exact: true })
        .getByRole("button", { name: "Use report", exact: true })
        .click();
      await expect(editor.getByLabel("Date from", { exact: true })).toHaveValue(
        "",
        { timeout: 20_000 },
      );
      await setPeriod(editor);
      await editor.getByRole("button", { name: "Back", exact: true }).click();
    }
    await addField(page, "Patient Name");
    await other
      .getByRole("button", { name: "Move Viral Load up", exact: true })
      .click();
    await reviewReport(page);
    await reviewReport(other);
    const retainedHeaders = ["Accession Number", "Viral Load", "Specimen ID"];
    await expect(
      other
        .getByRole("list", { name: "CSV columns in order" })
        .getByRole("listitem"),
    ).toHaveText(retainedHeaders);
    await page
      .getByRole("button", { name: "Update shared report", exact: true })
      .click();
    await page.getByRole("button", { name: "Update", exact: true }).click();
    await expect(
      page.getByText(`Updated ${reportName}.`, { exact: true }),
    ).toBeVisible();
    await other
      .getByRole("button", { name: "Update shared report", exact: true })
      .click();
    await other.getByRole("button", { name: "Update", exact: true }).click();
    await expect(
      other.getByText(
        "Someone changed this shared report after you opened it. Your choices are still here; reopen the current saved report or save a copy.",
        { exact: true },
      ),
    ).toBeVisible();
    await expect(
      other
        .getByRole("list", { name: "CSV columns in order" })
        .getByRole("listitem"),
    ).toHaveText(retainedHeaders);
    await other.evaluate(() => window.scrollTo(0, 0));
    await expect.poll(() => other.evaluate(() => window.scrollY)).toBe(0);
    await other.screenshot({
      path: testInfo.outputPath("shared-editor-conflict.png"),
      fullPage: true,
    });
    await other.setViewportSize({ width: 390, height: 844 });
    await expect
      .poll(() =>
        other.evaluate(
          () => document.documentElement.scrollWidth <= innerWidth,
        ),
      )
      .toBe(true);
    await other.screenshot({
      path: testInfo.outputPath("shared-editor-conflict-narrow.png"),
      fullPage: true,
    });
    await other
      .getByRole("button", { name: "Save a copy", exact: true })
      .click();
    await other
      .getByRole("textbox", { name: "Report name", exact: true })
      .fill(copyName);
    await other
      .getByRole("button", { name: "Save shared report", exact: true })
      .click();
    await expect(
      other.getByText(`Saved as ${copyName}.`, { exact: true }),
    ).toBeVisible();
    const copyId = new URL(other.url()).searchParams.get("saved");
    expect(copyId).toBeTruthy();
    expect(copyId).not.toBe(originalId);
    created.set(copyId!, copyName);
    await expect(
      other.getByText(
        "Someone changed this shared report after you opened it. Your choices are still here; reopen the current saved report or save a copy.",
        { exact: true },
      ),
    ).toBeHidden();
    await other.screenshot({
      path: testInfo.outputPath("shared-editor-copy-saved-narrow.png"),
      fullPage: true,
    });
    await other.setViewportSize({ width: 1280, height: 720 });
    // Reopen both server definitions: the rejected edit must not alter the winner.
    for (const [editor, id, headers] of [
      [
        page,
        originalId!,
        ["Accession Number", "Specimen ID", "Viral Load", "Patient Name"],
      ],
      [other, copyId!, retainedHeaders],
    ] as const) {
      await savedLibrary(editor);
      await editor.reload();
      const name = created.get(id)!;
      await editor
        .getByRole("searchbox", { name: "Search shared reports", exact: true })
        .fill(name);
      await editor
        .getByRole("article", { name, exact: true })
        .getByRole("button", { name: "Use report", exact: true })
        .click();
      await expect(editor).toHaveURL(new RegExp(`saved=${id}`));
      await expect(editor.getByLabel("Date from", { exact: true })).toHaveValue(
        "",
        { timeout: 20_000 },
      );
      await setPeriod(editor);
      const result = await downloadReport(editor, 2);
      expect(result.headers).toEqual(headers);
      await editor.screenshot({
        path: testInfo.outputPath(
          name === reportName
            ? "shared-editor-original.png"
            : "shared-editor-copy.png",
        ),
        fullPage: true,
      });
      expect(
        result.records.map((row) => row[result.headers.indexOf("Viral Load")]),
      ).toEqual(["450", "450"]);
    }
  } catch (error) {
    await other.screenshot({
      path: testInfo.outputPath("other-editor-failure.png"),
      fullPage: true,
    });
    throw error;
  } finally {
    // Remove only this run's uniquely named definitions, using their latest versions.
    const csrf = await page.evaluate(() => localStorage.getItem("CSRF") || "");
    for (const [id, name] of created) {
      const endpoint = `/api/OpenELIS-Global/rest/reports/data-export/saved-configs/${encodeURIComponent(id)}`;
      const response = await page.request.get(endpoint);
      expect(response.status()).toBe(200);
      const current = await response.json();
      expect(current.name).toBe(name);
      const removed = await page.request.delete(
        `${endpoint}?expectedVersion=${encodeURIComponent(current.version)}`,
        { headers: { "X-CSRF-Token": csrf } },
      );
      expect(removed.status()).toBe(204);
    }
    await other.close();
  }
});

test("ordinary report users share a definition and independently download its repeated results", async ({
  page,
}, testInfo) => {
  testInfo.setTimeout(90_000);
  const run = Date.now();
  const suffix = String(run).replace(/\d/g, (digit) =>
    String.fromCharCode(97 + Number(digit)),
  );
  const firstUser = process.env.REPORTING_TEST_USER_A || `reporting${suffix}a`;
  const secondUser = process.env.REPORTING_TEST_USER_B || `reporting${suffix}b`;
  if (!process.env.REPORTING_TEST_USER_A && !process.env.REPORTING_TEST_USER_B)
    await createReportUsers(page, [firstUser, secondUser]);
  const reportName = `Shared reporting ${run}`;
  await signInAsReportUser(page, firstUser);
  await openBuilder(page);
  await addField(page, "Patient Name");
  await page
    .getByRole("button", { name: "Move Specimen ID up", exact: true })
    .click();
  await saveReport(page, reportName);
  const original = await downloadReport(page, 2);
  expect(original.headers.slice(0, 2)).toEqual([
    "Specimen ID",
    "Accession Number",
  ]);
  expect(
    original.records.map(
      (row) => row[original.headers.indexOf("Patient Name")],
    ),
  ).toEqual(["Synthetic Reporting Fixture", "Synthetic Reporting Fixture"]);
  expect(
    original.records.map((row) => row[original.headers.indexOf("Viral Load")]),
  ).toEqual(["450", "450"]);
  await signInAsReportUser(page, secondUser);
  await page
    .getByRole("button", { name: "Shared reports", exact: true })
    .click();
  const search = page.getByRole("searchbox", {
    name: "Search shared reports",
    exact: true,
  });
  await search.fill(reportName);
  const card = page.getByRole("article", { name: reportName, exact: true });
  await card.getByRole("button", { name: "Use report", exact: true }).click();
  await expect(page.getByLabel("Date from", { exact: true })).toHaveValue("");
  await expect(page.getByLabel("Date to", { exact: true })).toHaveValue("");
  await setPeriod(page);
  const reused = await downloadReport(page, 2);
  expect(reused).toEqual(original);
  await savedLibrary(page);
  await search.fill(reportName);
  await card.getByRole("button", { name: /Delete shared report/ }).click();
  await page.getByRole("button", { name: /Delete$/ }).click();
  await expect(card).toBeHidden();
});

test("the mock column interactions work at desktop and narrow widths without losing selection", async ({
  page,
}, testInfo) => {
  testInfo.setTimeout(60_000);
  await page.setViewportSize({ width: 1280, height: 900 });
  await page.goto("/CustomDataExport");
  await startReport(page);
  const available = page.getByRole("region", {
    name: "Available fields",
    exact: true,
  });
  await expect(available.getByRole("checkbox")).toHaveCount(0);
  await expect(
    available.getByRole("button", { name: "Configured tests", exact: true }),
  ).toHaveAttribute("aria-expanded", "false");
  await page.screenshot({
    path: testInfo.outputPath("app-desktop-collapsed.png"),
    fullPage: true,
  });
  await page
    .getByRole("searchbox", { name: "Find a field", exact: true })
    .fill("Configured tests");
  await expect(
    page.getByRole("button", { name: "Add Viral Load", exact: true }),
  ).toBeVisible();
  await page.getByRole("button", { name: "Clear search", exact: true }).click();
  await expect(
    available.getByRole("button", { name: "Configured tests", exact: true }),
  ).toHaveAttribute("aria-expanded", "false");
  for (const label of ["Accession Number", "Specimen ID", "Viral Load"])
    await addField(page, label);
  const grip = page.getByRole("button", {
    name: "Drag Viral Load to reorder",
    exact: true,
  });
  await grip.press("Home");
  await expect(grip).toBeFocused();
  const preview = page.getByRole("region", { name: "CSV header preview" });
  await expect(preview.getByRole("columnheader")).toHaveText([
    "Viral Load",
    "Accession Number",
    "Specimen ID",
  ]);
  const accessionColumn = page
    .getByRole("button", {
      name: "Drag Accession Number to reorder",
      exact: true,
    })
    .locator("..");
  const viralColumn = grip.locator("..");
  await viralColumn.dragTo(accessionColumn, {
    targetPosition: { x: 100, y: 45 },
  });
  await expect(preview.getByRole("columnheader")).toHaveText([
    "Accession Number",
    "Viral Load",
    "Specimen ID",
  ]);
  await grip.press("Home");
  await page.screenshot({
    path: testInfo.outputPath("app-desktop-selected.png"),
    fullPage: true,
  });
  const accessibility = await new AxeBuilder({ page })
    .include(".reporting-design")
    .analyze();
  expect(accessibility.violations).toEqual([]);
  await page.setViewportSize({ width: 390, height: 844 });
  const panes = page.getByRole("group", { name: "Column builder views" });
  await panes.getByRole("button", { name: /Your columns/ }).click();
  await expect(grip).toBeVisible();
  await grip.press("End");
  await expect(grip).toBeFocused();
  await expect(preview.getByRole("columnheader")).toHaveText([
    "Accession Number",
    "Specimen ID",
    "Viral Load",
  ]);
  await page.screenshot({
    path: testInfo.outputPath("app-narrow-selected.png"),
    fullPage: true,
  });
  await panes
    .getByRole("button", { name: "Available fields", exact: true })
    .click();
  await expect(available).toBeVisible();
  await expect(
    page.getByRole("region", { name: "Selected columns", exact: true }),
  ).toBeHidden();
  await page.screenshot({
    path: testInfo.outputPath("app-narrow-collapsed.png"),
    fullPage: true,
  });
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBe(true);
  await page
    .getByRole("button", { name: "Next: Set Filters", exact: true })
    .click();
  await setPeriod(page);
  await captureWidths(page, testInfo, "app-filters", true);
  await page
    .getByRole("button", { name: "Show more filters", exact: true })
    .click();
  await captureWidths(page, testInfo, "app-more-filters", true);
  await reviewReport(page);
  await captureWidths(page, testInfo, "app-review", true);
  await page
    .getByRole("button", { name: "My Report Queue", exact: true })
    .click();
  await captureWidths(page, testInfo, "app-queue", true);
});

async function captureWidths(
  page: Page,
  testInfo: TestInfo,
  state: string,
  audit: boolean,
) {
  for (const [name, width, height] of [
    ["desktop", 1280, 900],
    ["narrow", 390, 844],
  ] as const) {
    await page.setViewportSize({ width, height });
    await page.getByRole("heading", { level: 1 }).scrollIntoViewIfNeeded();
    await page.screenshot({
      path: testInfo.outputPath(`${state}-${name}.png`),
      fullPage: state.includes("queue") ? false : true,
    });
    expect(
      await page.evaluate(
        () => document.documentElement.scrollWidth <= innerWidth,
      ),
    ).toBe(true);
    if (audit)
      expect(
        (await new AxeBuilder({ page }).include(".reporting-design").analyze())
          .violations,
      ).toEqual([]);
    if (state.includes("review")) {
      await page.locator(".save-config-panel").scrollIntoViewIfNeeded();
      await page.screenshot({
        path: testInfo.outputPath(`${state}-${name}-save.png`),
      });
    }
  }
}

// Capture the same supplied design states when running the local parity gate.
// Runtime workflow tests above remain independent of the mock's fictional data.
test("capture the canonical mock at the application validation widths", async ({
  page,
}, testInfo) => {
  testInfo.setTimeout(60_000);
  test.skip(
    !process.env.REPORTING_MOCK_URL,
    "Set the pinned mock URL for the design comparison run.",
  );
  await page.setViewportSize({ width: 1280, height: 900 });
  await page.goto(process.env.REPORTING_MOCK_URL!);
  await page
    .getByRole("button", { name: "Start a new export", exact: true })
    .click();
  await page.getByRole("radio", { name: /^Sample & Testing/ }).click();
  const search = page.getByRole("searchbox", {
    name: "Find a field",
    exact: true,
  });
  await expect(search).toBeVisible();
  await page.screenshot({
    path: testInfo.outputPath("mock-desktop-collapsed.png"),
    fullPage: true,
  });
  for (const label of ["Accession Number", "Sample Type", "Test Name"])
    await addField(page, label);
  await page.screenshot({
    path: testInfo.outputPath("mock-desktop-selected.png"),
    fullPage: true,
  });
  await page.setViewportSize({ width: 390, height: 844 });
  await page
    .getByRole("button", { name: "Your columns (3)", exact: true })
    .click();
  await page.screenshot({
    path: testInfo.outputPath("mock-narrow-selected.png"),
    fullPage: true,
  });
  await page
    .getByRole("button", { name: "Available fields", exact: true })
    .click();
  await page.screenshot({
    path: testInfo.outputPath("mock-narrow-collapsed.png"),
    fullPage: true,
  });
  await page.getByRole("button", { name: /^Next: Set Filters/ }).click();
  await page.getByLabel("Date From *", { exact: true }).fill("2026-05-05");
  await page.getByLabel("Date To *", { exact: true }).fill("2026-05-05");
  await captureWidths(page, testInfo, "mock-filters", false);
  await page
    .getByRole("button", { name: "Show more filters", exact: true })
    .click();
  await captureWidths(page, testInfo, "mock-more-filters", false);
  await page.getByRole("button", { name: /^Next: Review & Submit/ }).click();
  await captureWidths(page, testInfo, "mock-review", false);
  await page.getByRole("button", { name: /My Report Queue/ }).click();
  await captureWidths(page, testInfo, "mock-queue", false);
});

async function expectConsistentNavigationType(page: Page) {
  const mismatches = await page
    .locator(
      ".application-side-nav .cds--side-nav__link, .application-side-nav .cds--side-nav__submenu",
    )
    .evaluateAll((items) =>
      items.flatMap((item) => {
        const style = getComputedStyle(item);
        const label = item.querySelector(
          ".cds--side-nav__link-text, .cds--side-nav__submenu-title",
        );
        const labelStyle = label ? getComputedStyle(label) : style;
        const active =
          item.getAttribute("aria-current") === "page" ||
          item.classList.contains("cds--side-nav__link--current");
        return style.fontSize === "14px" &&
          style.fontFamily.includes("IBM Plex Sans") &&
          Math.abs(parseFloat(style.lineHeight) - 18) < 0.01 &&
          labelStyle.fontWeight === (active ? "600" : "400")
          ? []
          : [
              {
                label: item.textContent?.trim(),
                font: style.font,
                labelWeight: labelStyle.fontWeight,
              },
            ];
      }),
    );
  expect(mismatches).toEqual([]);
}

test("the reporting instance sidebar follows the mock and preserves the draft across queue navigation", async ({
  page,
}, testInfo) => {
  test.skip(
    process.env.REPORTING_INSTANCE_NAV !== "true",
    "Requires the Reporting UAT instance menu profile.",
  );
  testInfo.setTimeout(60_000);
  await page.setViewportSize({ width: 1280, height: 900 });
  await page.goto("/CustomDataExport?uat=navigation");
  await expect(page).toHaveURL(
    /\/reports\/custom-data-export\?uat=navigation$/,
  );
  const nav = page.getByRole("navigation", { name: "Side navigation" });
  await expect(nav.getByRole("heading")).toHaveText([
    "Main Menu",
    "Patient & Orders",
    "Reports",
    "Administration",
  ]);
  for (const [name, href] of [
    ["Home", "/Dashboard"],
    ["Order Test", "/SamplePatientEntry"],
    ["Results Validation", "/ResultValidation?type=&test="],
    ["Patient Management", "/PatientManagement"],
    ["Admin", "/MasterListsPage"],
  ])
    await expect(nav.getByRole("link", { name, exact: true })).toHaveAttribute(
      "href",
      href,
    );
  await expect(
    nav.getByRole("link", { name: "Results Entry", exact: true }),
  ).toHaveCount(1);
  await expect(
    nav.getByRole("button", { name: "Other reports", exact: true }),
  ).toHaveAttribute("aria-expanded", "false");
  const more = nav.getByRole("button", { name: "More tools", exact: true });
  await expect(more).toHaveAttribute("aria-expanded", "false");
  await expect(
    nav.getByText("Not yet connected", { exact: true }),
  ).toBeVisible();
  await page.screenshot({
    path: testInfo.outputPath("app-sidebar-desktop.png"),
    fullPage: true,
  });
  await more.click();
  await expect(
    nav.getByRole("link", { name: "Alerts", exact: true }),
  ).toBeVisible();
  await more.click();
  await expectConsistentNavigationType(page);
  await startReport(page);
  await addField(page, "Accession Number");
  const queue = nav.getByRole("link", { name: "My Report Queue", exact: true });
  await nav
    .getByRole("link", { name: "Custom Data Export", exact: true })
    .press("Tab");
  await expect(queue).toBeFocused();
  await expect(queue).toHaveCSS("outline-width", "2px");
  await expect(queue).toHaveCSS("outline-style", "solid");
  await queue.press("Enter");
  await expect(page).toHaveURL(/view=queue/);
  await expect(
    page.getByRole("heading", { name: "My Report Queue", exact: true }),
  ).toBeVisible();
  await expect(queue).toHaveAttribute("aria-current", "page");
  await expect(nav.locator('[aria-current="page"]')).toHaveCount(1);
  await page.goBack();
  await expect(
    page.getByRole("heading", { name: "Your CSV columns (1)", exact: true }),
  ).toBeVisible();
  await page.goForward();
  await expect(queue).toHaveAttribute("aria-current", "page");
  await page.reload();
  await expect(queue).toHaveAttribute("aria-current", "page");
  await nav
    .getByRole("link", { name: "Custom Data Export", exact: true })
    .click();
  await page
    .getByRole("button", { name: "Continue current export", exact: true })
    .click();
  await expect(
    page.getByRole("heading", { name: "Your CSV columns (1)", exact: true }),
  ).toBeVisible();
  await expect(
    page
      .getByRole("region", { name: "CSV header preview" })
      .getByRole("columnheader"),
  ).toHaveText(["Accession Number"]);
  await page.setViewportSize({ width: 390, height: 844 });
  await page.locator('[data-cy="menuButton"]').click();
  await expect(queue).toBeVisible();
  await page.screenshot({
    path: testInfo.outputPath("app-sidebar-narrow.png"),
    fullPage: true,
  });
  const accessibility = await new AxeBuilder({ page })
    .include('nav[aria-label="Side navigation"]')
    .withTags(["wcag2a", "wcag2aa"])
    .analyze();
  expect(accessibility.violations).toEqual([]);
  await queue.click();
  await expect(
    page.getByRole("heading", { name: "My Report Queue", exact: true }),
  ).toBeVisible();
  await expect(page).toHaveURL(/uat=navigation/);
  await expect(nav).toHaveClass(/cds--side-nav--hidden/);
});

test("administration uses the same navigation typography and readable sections", async ({
  page,
}, testInfo) => {
  test.skip(
    process.env.REPORTING_INSTANCE_NAV !== "true",
    "Requires the Reporting UAT instance menu profile.",
  );
  await page.setViewportSize({ width: 1280, height: 900 });
  await page.goto("/MasterListsPage");
  await expect(page.getByTestId("admin-dashboard")).toBeVisible();
  await expectConsistentNavigationType(page);
  await expect
    .poll(async () => {
      const nav = await page
        .getByRole("navigation", { name: "Side navigation" })
        .boundingBox();
      const heading = await page
        .getByTestId("admin-dashboard")
        .getByRole("heading", { level: 2 })
        .boundingBox();
      return Boolean(nav && heading && heading.x >= nav.x + nav.width);
    })
    .toBe(true);
  await page.screenshot({
    path: testInfo.outputPath("app-sidebar-administration.png"),
    animations: "disabled",
    fullPage: true,
  });
  await page.setViewportSize({ width: 390, height: 844 });
  await page.locator('[data-cy="menuButton"]').click();
  await expect(
    page.getByRole("navigation", { name: "Side navigation" }),
  ).toBeVisible();
  await expectConsistentNavigationType(page);
  await page.screenshot({
    path: testInfo.outputPath("app-sidebar-administration-narrow.png"),
    fullPage: true,
  });
});

test("menu administration saves database icons and identifies instance-controlled settings", async ({
  page,
}, testInfo) => {
  test.skip(
    process.env.REPORTING_INSTANCE_NAV !== "true",
    "Requires the Reporting UAT instance profile.",
  );
  await page.setViewportSize({ width: 1280, height: 900 });
  await page.goto("/MasterListsPage/globalMenuManagement");
  const form = page.getByRole("form", {
    name: "Global Menu Management",
    exact: true,
  });
  await expect(
    form.getByText(
      "Update menu visibility, section headings and icons. Settings managed by this instance are read-only here.",
    ),
  ).toBeVisible();
  await form
    .getByRole("button", { name: "Administration", exact: true })
    .click();
  await form.getByRole("button", { name: "More tools", exact: true }).click();
  await form.getByRole("button", { name: "Alerts", exact: true }).click();
  const fields = page.getByTestId("menu-fields-menu_alerts_standalone");
  const icon = fields.getByRole("combobox", { name: "Icon", exact: true });
  await expect(icon).toBeEnabled();
  const previousIcon = await icon.inputValue();
  const selectedIcon = previousIcon === "patient" ? "reports" : "patient";
  try {
    await icon.selectOption(selectedIcon);
    await form.getByRole("button", { name: "Save", exact: true }).click();
    await expect(
      form.getByText("Menu settings saved.", { exact: true }),
    ).toBeVisible();
    await page.reload();
    await form
      .getByRole("button", { name: "Administration", exact: true })
      .click();
    await form.getByRole("button", { name: "More tools", exact: true }).click();
    await form.getByRole("button", { name: "Alerts", exact: true }).click();
    await expect(icon).toHaveValue(selectedIcon);
    await icon.scrollIntoViewIfNeeded();
    await page.screenshot({
      path: testInfo.outputPath("app-menu-settings-desktop.png"),
      animations: "disabled",
    });
    await page.setViewportSize({ width: 390, height: 844 });
    await icon.scrollIntoViewIfNeeded();
    await expect(icon).toBeVisible();
    await expect
      .poll(async () => (await form.boundingBox())?.width || 0)
      .toBeGreaterThan(300);
    await expect
      .poll(() =>
        page.evaluate(
          () => document.documentElement.scrollWidth <= window.innerWidth,
        ),
      )
      .toBe(true);
    await page.screenshot({
      path: testInfo.outputPath("app-menu-settings-narrow.png"),
      animations: "disabled",
    });
  } finally {
    await icon.selectOption(previousIcon);
    await form.getByRole("button", { name: "Save", exact: true }).click();
    await expect(
      form.getByText("Menu settings saved.", { exact: true }),
    ).toBeVisible();
  }
  await page.setViewportSize({ width: 1280, height: 900 });
  await page
    .getByTestId("menu-settings-menu_section_reports")
    .getByRole("button", { name: "Reports", exact: true })
    .click();
  await page
    .getByTestId("menu-settings-menu_reports")
    .getByRole("button", { name: "Reports", exact: true })
    .click();
  const managed = page.getByTestId("menu-fields-menu_reports");
  await expect(
    managed.getByRole("combobox", { name: "Icon", exact: true }),
  ).toBeDisabled();
  await expect(
    managed.getByText("Managed by instance configuration"),
  ).toHaveCount(2);
});

for (const viewport of [
  { width: 1280, height: 900 },
  { width: 390, height: 844 },
]) {
  test(`Referrals preserve returned and pending rows through shared reports at ${viewport.width}px`, async ({
    page,
  }, testInfo) => {
    testInfo.setTimeout(90_000);
    await page.setViewportSize(viewport);
    await page.goto("/reports/custom-data-export");
    await page
      .getByRole("button", { name: "Start a new export", exact: true })
      .click();
    await page.getByRole("radio", { name: /^Referrals/ }).click();
    const available = page.getByRole("region", { name: "Available fields" });
    await expect(
      available.getByRole("button", { name: "Referrals", exact: true }),
    ).toHaveAttribute("aria-expanded", "false");
    if (viewport.width < 600) {
      await expect(
        page.getByRole("button", { name: "Your columns (0)", exact: true }),
      ).toBeVisible();
    } else {
      await expect(
        page.getByRole("heading", { name: "Your CSV columns (0)" }),
      ).toBeVisible();
    }
    const columns = [
      "Accession Number",
      "Referral ID",
      "Referral Result ID",
      "Result ID",
      "Referred Lab",
      "Referred Test Name",
      "Referral Date",
      "Referral Result Value",
      "Referral Result Date",
      "Referral Status",
    ];
    for (const label of columns) await addField(page, label);
    if (viewport.width < 600)
      await page
        .getByRole("button", { name: "Your columns (10)", exact: true })
        .click();
    await page.screenshot({
      path: testInfo.outputPath("referral-columns.png"),
      fullPage: true,
    });
    await page
      .getByRole("button", { name: "Next: Set Filters", exact: true })
      .click();
    await expect(page.getByText(/Uses referral sent dates in/)).toBeVisible();
    await expect(
      page.getByRole("combobox", { name: /^Result statuses/ }),
    ).toHaveCount(0);
    await setPeriod(page, "2026-05-07");
    await reviewReport(page);
    await expect(page.getByText(/Finalized/)).toHaveCount(0);
    await expect(
      page.getByText(/2026-05-07.*referral sent dates/),
    ).toBeVisible();
    await page.screenshot({
      path: testInfo.outputPath("referral-review.png"),
      fullPage: true,
    });
    const reportName = `Referral UAT ${viewport.width} ${Date.now()}`;
    await saveReport(page, reportName);
    await savedLibrary(page);
    await page
      .getByRole("searchbox", { name: "Search shared reports", exact: true })
      .fill(reportName);
    const card = page.getByRole("article", { name: reportName, exact: true });
    await card.getByRole("button", { name: "Use report", exact: true }).click();
    await expect(page.getByLabel("Date from", { exact: true })).toHaveValue("");
    await expect(page.getByLabel("Date to", { exact: true })).toHaveValue("");
    await setPeriod(page, "2026-05-07");
    const { headers, records } = await downloadReport(page, 3);
    expect(headers).toEqual(columns);
    const returned = records.filter((row) => row[7] !== "");
    const pending = records.filter((row) => row[7] === "");
    expect(returned).toHaveLength(2);
    expect(new Set(returned.map((row) => row[2])).size).toBe(2);
    expect(new Set(returned.map((row) => row[3])).size).toBe(2);
    expect(returned.map((row) => row.slice(4))).toEqual([
      [
        "Synthetic Reference Lab",
        "Viral Load",
        "2026-05-07",
        "450",
        "2026-05-08",
        "COMPLETED",
      ],
      [
        "Synthetic Reference Lab",
        "Viral Load",
        "2026-05-07",
        "450",
        "2026-05-09",
        "COMPLETED",
      ],
    ]);
    expect(pending).toHaveLength(1);
    expect(pending[0].slice(2)).toEqual([
      "",
      "",
      "Synthetic Reference Lab",
      "Viral Load",
      "2026-05-07",
      "",
      "",
      "REQUESTED",
    ]);
    expect(records.map((row) => row[0])).toEqual([
      accession,
      accession,
      accession,
    ]);
    expect(new Set(records.map((row) => row[1])).size).toBe(2);
    await savedLibrary(page);
    await page
      .getByRole("searchbox", { name: "Search shared reports", exact: true })
      .fill(reportName);
    await card.getByRole("button", { name: /Delete shared report/ }).click();
    const deleted = page.waitForResponse(
      (response) =>
        response.request().method() === "DELETE" &&
        response.url().includes("/saved-configs/"),
    );
    await page.getByRole("button", { name: /Delete$/ }).click();
    await deleted;
    await expect(
      page.getByRole("dialog", { name: "Delete shared report", exact: true }),
    ).toBeHidden();
    await expect(card).toBeHidden();
    await page.reload();
    await expect(
      page.getByRole("searchbox", {
        name: "Search shared reports",
        exact: true,
      }),
    ).toHaveValue(reportName);
    await expect(card).toBeHidden();
  });
}

test("capture the canonical Referral workflow at matching widths", async ({
  page,
}, testInfo) => {
  test.skip(
    !process.env.REPORTING_MOCK_URL,
    "Set the pinned mock URL for direct design comparison.",
  );
  await page.goto(process.env.REPORTING_MOCK_URL!);
  await page
    .getByRole("button", { name: "Start a new export", exact: true })
    .click();
  await page.getByRole("radio", { name: /^Referrals/ }).click();
  await captureWidths(page, testInfo, "mock-referral-collapsed", false);
  for (const name of [
    "Accession Number",
    "Referring Lab",
    "Referred Test Name",
    "Referral Date",
    "Referral Result Value",
    "Referral Result Date",
    "Referral Status",
  ])
    await addField(page, name);
  await captureWidths(page, testInfo, "mock-referral-columns", false);
  await page.getByRole("button", { name: /^Next: Set Filters/ }).click();
  await page.getByLabel("Date From *", { exact: true }).fill("2026-05-07");
  await page.getByLabel("Date To *", { exact: true }).fill("2026-05-07");
  await captureWidths(page, testInfo, "mock-referral-filters", false);
  await page.getByRole("button", { name: /^Next: Review & Submit/ }).click();
  await expect(
    page.getByRole("button", { name: "Create CSV", exact: true }),
  ).toBeVisible();
  await captureWidths(page, testInfo, "mock-referral-review", false);
});
