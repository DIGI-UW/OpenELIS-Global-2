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

test("another configured report uses its own defaults and the same builder and queue", async ({
  page,
}) => {
  await openBuilder(page);
  await configuredReport(page, "Sample summary");
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
  await addField(page, "Viral Load");
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
    page.getByText("Choose fresh dates before running this saved report.", {
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
