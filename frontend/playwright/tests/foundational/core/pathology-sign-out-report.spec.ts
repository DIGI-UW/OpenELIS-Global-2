import AxeBuilder from "@axe-core/playwright";
import type { Browser, BrowserContext, Page, Response } from "@playwright/test";
import { test, expect } from "../../../helpers/test-base";
import {
  DIAGNOSIS_CATEGORY,
  apiGet,
  createDictionaryEntry,
  createPathologyCase,
  generateReport,
  moveCaseToStage,
  readCase,
  saveCaseThroughApi,
  signOutCase,
  uploadReport,
  type DictionaryEntry,
  type PathologyStageId,
  type SeededPathologyCase,
} from "../../../helpers/seed-pathology-data";
import {
  FINDINGS_SECTION,
  STAGE_LABELS,
  actionBarButton,
  actionBarButtons,
  criticalResultBanner,
  diagnosisChips,
  expandSection,
  expectStage,
  expectStageSwitchesOn,
  openCase,
  pickDiagnosis,
  primaryAction,
  readSiteSetting,
  reopenRow,
  reportGenerateButton,
  reportRow,
  reportUpload,
  reportWithdrawButton,
  reportsTable,
  searchDiagnoses,
  sectionHeader,
  setSiteSetting,
  signOutConfirm,
  toast,
} from "../../../helpers/pathology-case-view";
import { text } from "../../../helpers/app-messages";
import {
  LONG_TIMEOUT,
  NAV_TIMEOUT,
  UI_TIMEOUT,
} from "../../../helpers/timeouts";

/**
 * Sign-out from the action bar, the stored report versions, the critical
 * result and the reopen (FR-14, FR-15, S-8.3, S-8.4).
 *
 * What only a deployed stack proves here:
 *   - a report version opens in a real new tab at its own address, which
 *     answers a PDF to the session the tab carries,
 *   - the sign-out confirms, lands the case at Completed and opens the final
 *     report; a reopen voids it and a second sign-out issues the next one,
 *   - a critical diagnosis warns in Findings and raises an alert the Alerts
 *     dashboard lists,
 *   - focus lands where the screen sends it in a real browser,
 *   - Reports fits three desktop widths and passes axe, and
 *   - a user outside the case's lab units is answered 404 on a file address.
 *
 * Each test seeds its own case. The critical-code setting is set only to this
 * run's own code start and put back after each test that sets it. The
 * signed-in user must hold the Pathologist role and the laboratory must use
 * the review stage; the spec checks both first.
 */

const API_PREFIX = "/api/OpenELIS-Global";

const WCAG_21_AA_TAGS = ["wcag2a", "wcag2aa", "wcag21a", "wcag21aa"];

/** The widths Reports must fit at without scrolling sideways. */
const DESKTOP_WIDTHS = [1480, 1366, 1280];

const REPORTS_SECTION = "pathology-section-reports";

const CRITICAL_CODES_SETTING = "pathology.criticalResult.codes";

/** This run's tag, kept short so an abbreviation stays within ten characters. */
const RUN =
  `${Date.now().toString(36)}${Math.floor(Math.random() * 36 * 36).toString(36)}`
    .slice(-7)
    .toUpperCase();

const GROSS = "Stomach, antral biopsy, two fragments, 0.4 cm in aggregate.";
const MICROSCOPY = "Gastric mucosa with chronic inflammation.";
const NARRATIVE = "See the coded diagnosis.";
const REOPEN_REASON = "Addendum: deeper levels reviewed";
const LONG_AUTHOR =
  "Konstantinos-Alexandros Vanderbilt-Oyelaran-Nakamura de la Fontaine";

/** A diagnosis as the picker lists it: "code name". */
interface Diagnosis {
  entry: DictionaryEntry;
  code: string;
  label: string;
}

/** The critical diagnosis: its code starts with this run's critical start. */
let critical: Diagnosis;
/** A diagnosis whose code does not. */
let benign: Diagnosis;
/** The setting's stored value, while a test has changed it. */
let codesBefore: string | undefined;

async function withOwnPage<T>(
  browser: Browser,
  work: (page: Page) => Promise<T>,
): Promise<T> {
  const context = await browser.newContext({
    storageState: "playwright/.auth/user.json",
  });
  try {
    const page = await context.newPage();
    await page.goto("/", { waitUntil: "domcontentloaded" });
    return await work(page);
  } finally {
    await context.close();
  }
}

async function onApplication(page: Page) {
  if (!page.url().startsWith("http")) {
    await page.goto("/", { waitUntil: "domcontentloaded" });
  }
}

async function addDiagnosis(
  page: Page,
  name: string,
  code: string,
): Promise<Diagnosis> {
  const entry = await createDictionaryEntry(
    page,
    DIAGNOSIS_CATEGORY,
    name,
    code,
    "so the changeset that creates the pathologist conclusions has not run here.",
  );
  return { entry, code, label: `${code} ${name}` };
}

/**
 * A case at the stage its findings are written at, the review begun by the
 * signed-in pathologist, both descriptions written and, unless told not to,
 * a narrative conclusion: everything a sign-out needs.
 */
async function seedReadyCase(
  page: Page,
  { narrative = true }: { narrative?: boolean } = {},
): Promise<SeededPathologyCase & { readingStage: PathologyStageId }> {
  await onApplication(page);
  const seeded = await createPathologyCase(page);
  const caseId = seeded.pathologySampleId;
  await moveCaseToStage(page, seeded, "READY_PATHOLOGIST");
  const readingStage =
    (await readCase(page, caseId)).readingStage ?? "READY_PATHOLOGIST";
  await moveCaseToStage(page, seeded, readingStage);
  await saveCaseThroughApi(page, caseId, await readCase(page, caseId), {
    grossExam: GROSS,
    microscopyExam: MICROSCOPY,
    conclusionText: narrative ? NARRATIVE : undefined,
  });
  const stored = await readCase(page, caseId);
  expect(
    stored.assignedPathologistId,
    "the review names the pathologist",
  ).toBeTruthy();
  return { ...seeded, readingStage };
}

/** The address a stored version is read at, as the screen builds it. */
function reportFilePath(caseId: string, reportId: number | string) {
  return `${API_PREFIX}/rest/pathology/caseView/${caseId}/reports/${reportId}/file`;
}

/**
 * A wait, registered before the press, for the file a new tab reads. The
 * context hears every page's traffic, so the wait holds whether the tab shows
 * the PDF or hands it to a download.
 */
function waitForReportFile(context: BrowserContext, caseId: string) {
  const path = new RegExp(
    `/rest/pathology/caseView/${caseId}/reports/\\d+/file$`,
  );
  return context.waitForEvent("response", {
    predicate: (r) => path.test(new URL(r.url()).pathname),
    timeout: UI_TIMEOUT,
  });
}

/** The file came back a PDF, to the session the new tab carried. */
async function expectPdfToSession(response: Response) {
  expect(response.status(), "the tab's file is answered").toBe(200);
  expect(response.headers()["content-type"]).toContain("application/pdf");
  expect(
    await response.request().headerValue("cookie"),
    "the session cookie rides with the tab's request",
  ).toMatch(/JSESSIONID=/);
}

/** The version number the newest stored report carries. */
async function newestVersion(page: Page, caseId: string) {
  const reports = (await readCase(page, caseId)).reports ?? [];
  expect(reports.length, "the case holds a report").toBeGreaterThan(0);
  return Math.max(...reports.map((entry) => entry.version));
}

/** The sign-out question's Sign out, which stands in the bar for the primary. */
function signOutButton(page: Page) {
  return actionBarButton(page, text("pathology.action.confirmSignOut"));
}

/** The reopen row's Reopen, which stands in the bar for the primary. */
function reopenButton(page: Page) {
  return actionBarButton(page, text("pathology.action.confirmReopen"));
}

/** Neither the page nor the action bar scrolls sideways at any of `widths`. */
async function expectNoSidewaysScrollAt(page: Page, widths: number[]) {
  for (const width of widths) {
    await page.setViewportSize({ width, height: 900 });
    await expect(async () => {
      const overflow = await page.evaluate(() => {
        const root = document.documentElement;
        const bar = document.querySelector(".case-view__action-bar")!;
        return {
          page: root.scrollWidth - root.clientWidth,
          bar: bar.scrollWidth - bar.clientWidth,
        };
      });
      expect(overflow, `no sideways scroll at ${width}`).toEqual({
        page: 0,
        bar: 0,
      });
    }).toPass({ timeout: UI_TIMEOUT });
  }
}

/**
 * Sign the open case out from the action bar and wait for the screen to
 * finish: the new tab, its PDF, the toast naming the final version, and the
 * case read back at Completed. Returns the final version.
 */
async function signOutFromBar(
  page: Page,
  context: BrowserContext,
  seeded: SeededPathologyCase,
) {
  const caseId = seeded.pathologySampleId;
  await expect(primaryAction(page)).toHaveText(
    text("pathology.action.signOut"),
  );
  await expect(primaryAction(page)).toBeEnabled({ timeout: UI_TIMEOUT });
  await primaryAction(page).click();
  await expect(signOutConfirm(page)).toContainText(
    text("pathology.confirm.signOut", { labNumber: seeded.accessionNumber }),
  );
  await expect(signOutButton(page)).toBeFocused();

  const opened = context.waitForEvent("page", { timeout: UI_TIMEOUT });
  const file = waitForReportFile(context, caseId);
  await signOutButton(page).click();
  const tab = await opened;
  await expectPdfToSession(await file);
  const version = await newestVersion(page, caseId);
  await expect(
    toast(page, text("pathology.toast.signedOut", { version })),
  ).not.toHaveCount(0, { timeout: UI_TIMEOUT });
  await expectStage(page, STAGE_LABELS.COMPLETED);
  await expect(primaryAction(page)).toHaveText(text("pathology.action.reopen"));
  await tab.close();
  return version;
}

const letters = (n: number) =>
  Array.from(
    { length: n },
    () => "abcdefghijklmnopqrstuvwxyz"[Math.floor(Math.random() * 26)],
  ).join("");

/**
 * A user who enters results in one lab unit that is not pathology's, and
 * holds no other role. Returns the login name.
 */
async function createUserOutsidePathology(page: Page) {
  await onApplication(page);
  const endpoint = `${API_PREFIX}/rest/UnifiedSystemUser`;
  const form = await (await page.request.get(endpoint)).json();
  const results = form.labUnitRoles.find(
    (role: { roleName: string }) => role.roleName === "Results",
  );
  expect(results?.roleId).toBeTruthy();
  const sections = form.testSections as { id: string; value: string }[];
  const outside =
    sections.find((section) => section.value === "Hematology") ??
    sections.find(
      (section) => !/patho|histo|cyto|immuno/i.test(section.value ?? ""),
    );
  expect(
    outside,
    "the stack has a lab unit other than pathology's",
  ).toBeTruthy();
  const password = process.env.TEST_PASS;
  expect(password, "TEST_PASS is required for the second user").toBeTruthy();
  const csrf = await page.evaluate(() => localStorage.getItem("CSRF") || "");
  const username = `pwout${letters(8)}`;
  const created = await page.request.post(endpoint, {
    headers: { "X-CSRF-Token": csrf },
    data: {
      userLoginName: username,
      userPassword: password,
      confirmPassword: password,
      userFirstName: "Outside",
      userLastName: username,
      expirationDate: form.expirationDate,
      timeout: form.timeout,
      accountActive: "Y",
      accountDisabled: "N",
      accountLocked: "N",
      allowCopyUserRoles: "N",
      selectedRoles: [],
      selectedTestSectionLabUnits: { [outside!.id]: [results.roleId] },
    },
  });
  expect(await created.json()).toEqual({
    forward: "redirect:/UnifiedSystemUser",
  });
  return username;
}

/** Sign `username` in on a context of its own. */
async function signIn(browser: Browser, baseURL: string, username: string) {
  const context = await browser.newContext({
    ignoreHTTPSErrors: true,
    baseURL,
    storageState: { cookies: [], origins: [] },
  });
  const page = await context.newPage();
  await page.goto("/login");
  await page.locator("#loginName").fill(username);
  await page.locator("#password").fill(process.env.TEST_PASS!);
  await page.locator('[data-cy="loginButton"]').click();
  await expect(page).toHaveURL(/\/$/);
  return { context, page };
}

/** Change the critical-code setting, keeping what it held for afterEach. */
async function setCriticalCodes(page: Page, value: string) {
  await onApplication(page);
  if (codesBefore === undefined) {
    codesBefore = await readSiteSetting(page, CRITICAL_CODES_SETTING, "");
  }
  await setSiteSetting(page, CRITICAL_CODES_SETTING, value);
}

test.describe("Pathology sign-out and report versions", () => {
  // Order entry and the case view are both heavy screens, and a first visit
  // on a development server compiles them as it serves them.
  test.setTimeout(240_000);

  test.beforeAll(async ({ browser }) => {
    test.setTimeout(300_000);
    await withOwnPage(browser, async (page) => {
      await expectStageSwitchesOn(page, ["UNDER_REVIEW"]);
      const session = await apiGet<{ roles?: string[] }>(
        page,
        `${API_PREFIX}/session`,
      );
      if (Array.isArray(session.roles)) {
        expect(
          session.roles,
          "the signed-in user signs cases out, which only the pathologist does",
        ).toContain("Pathologist");
      }
      critical = await addDiagnosis(page, `Adenocarcinoma ${RUN}`, `C${RUN}`);
      benign = await addDiagnosis(page, `Chronic gastritis ${RUN}`, `K${RUN}`);
    });
  });

  test.afterEach(async ({ browser }) => {
    if (codesBefore === undefined) {
      return;
    }
    const value = codesBefore;
    await withOwnPage(browser, (page) =>
      setSiteSetting(page, CRITICAL_CODES_SETTING, value),
    );
    codesBefore = undefined;
  });

  test("generate opens the new version in a new tab and lists it", async ({
    page,
    context,
  }) => {
    const seeded = await seedReadyCase(page);
    const caseId = seeded.pathologySampleId;
    await openCase(page, seeded);
    await expandSection(page, REPORTS_SECTION);
    await expect(
      page
        .locator(`#${REPORTS_SECTION}`)
        .getByText(text("pathology.empty.noReports")),
    ).toBeVisible();

    // No version yet: the press asks nothing.
    let opened = context.waitForEvent("page", { timeout: UI_TIMEOUT });
    let file = waitForReportFile(context, caseId);
    await reportGenerateButton(page).click();
    let tab = await opened;
    await expectPdfToSession(await file);
    const first = await newestVersion(page, caseId);
    await expect(
      toast(page, text("pathology.toast.reportGenerated", { version: first })),
    ).not.toHaveCount(0, { timeout: UI_TIMEOUT });
    await expect(
      reportsTable(page).locator("tbody tr").first(),
    ).toHaveAttribute("data-testid", `report-row-${first}`);
    await expect(reportRow(page, first)).toContainText(text("common.draft"));
    // P4: focus stays on the button.
    await expect(reportGenerateButton(page)).toBeFocused({
      timeout: UI_TIMEOUT,
    });
    await tab.close();

    // A generated version exists: the press asks first, naming it.
    await reportGenerateButton(page).click();
    const question = page.getByRole("group", {
      name: text("pathology.confirm.regenerate", { version: first }),
    });
    await expect(question).toBeVisible();
    opened = context.waitForEvent("page", { timeout: UI_TIMEOUT });
    file = waitForReportFile(context, caseId);
    await question
      .getByRole("button", {
        name: text("pathology.action.generateNewVersion"),
      })
      .click();
    tab = await opened;
    await expectPdfToSession(await file);
    const second = await newestVersion(page, caseId);
    expect(second).toBeGreaterThan(first);
    await expect(reportRow(page, second)).toBeVisible({ timeout: UI_TIMEOUT });
    await expect(
      reportsTable(page).locator("tbody tr").first(),
    ).toHaveAttribute("data-testid", `report-row-${second}`);
    await expect(reportRow(page, first)).toContainText(
      text("pathology.report.state.SUPERSEDED"),
    );
    await tab.close();
  });

  test("a report row opens in a new tab on one click and has no other button", async ({
    page,
    context,
  }) => {
    const seeded = await seedReadyCase(page);
    const caseId = seeded.pathologySampleId;
    const draft = await generateReport(page, caseId);
    await openCase(page, seeded);
    await expandSection(page, REPORTS_SECTION);

    const row = reportRow(page, draft.version);
    const link = row.getByRole("link", {
      name: text("pathology.report.open", { version: draft.version }),
    });
    await expect(link).toHaveAttribute(
      "href",
      reportFilePath(caseId, draft.id),
    );
    await expect(row.getByRole("button")).toHaveCount(0);
    for (const name of [/^View/, /^Download/, /^Print/, /^Email/]) {
      await expect(
        page.locator(`#${REPORTS_SECTION}`).getByRole("button", { name }),
      ).toHaveCount(0);
    }

    const pagesBefore = context.pages().length;
    const opened = context.waitForEvent("page", { timeout: UI_TIMEOUT });
    const file = waitForReportFile(context, caseId);
    await row.getByRole("cell").nth(2).click();
    const tab = await opened;
    await expectPdfToSession(await file);
    expect(context.pages()).toHaveLength(pagesBefore + 1);
    await tab.close();
  });

  test("sign-out asks, signs the case out and opens the final report", async ({
    page,
    context,
  }) => {
    const seeded = await seedReadyCase(page);
    await openCase(page, seeded);
    const version = await signOutFromBar(page, context, seeded);

    await expect(page.getByRole("button", { name: "Save draft" })).toHaveCount(
      0,
    );
    await expandSection(page, REPORTS_SECTION);
    await expect(reportRow(page, version)).toContainText(
      text("pathology.report.type.FINAL"),
    );
    const stored = await readCase(page, seeded.pathologySampleId);
    expect(stored.status).toBe("COMPLETED");
    expect(stored.signedOut).toBe(true);
  });

  test("a critical diagnosis warns in Findings and raises an alert at sign-out", async ({
    page,
    context,
  }) => {
    test.setTimeout(300_000);
    const seeded = await seedReadyCase(page, { narrative: false });
    await setCriticalCodes(page, critical.code.toLowerCase());
    await openCase(page, seeded);
    await expandSection(page, FINDINGS_SECTION);

    await pickDiagnosis(page, benign.code, benign.label);
    await expect(criticalResultBanner(page)).toHaveCount(0);
    // F8: a critical entry says so in words, in its search item too, so the
    // option is found by the start of its name.
    const menu = await searchDiagnoses(page, critical.code);
    const option = menu.getByRole("option", {
      name: new RegExp(`^${critical.label}\\b`),
    });
    await expect(option).toContainText(text("common.critical"));
    await option.click();
    await expect(diagnosisChips(page)).toContainText(critical.label);
    const banner = criticalResultBanner(page);
    await expect(banner).toBeVisible();
    await expect(banner).toHaveAttribute("role", "alert");
    await expect(banner).toContainText(text("pathology.banner.criticalResult"));
    await expect(banner.getByRole("button")).toHaveCount(0);
    await expect(
      page.locator(`#${FINDINGS_SECTION} .cds--accordion__content > *`).first(),
    ).toContainText(text("pathology.banner.criticalResult"));

    await primaryAction(page).click();
    await expect(signOutConfirm(page)).toContainText(
      text("pathology.confirm.signOutCritical"),
    );
    const opened = context.waitForEvent("page", { timeout: UI_TIMEOUT });
    await signOutButton(page).click();
    const tab = await opened;
    await expectStage(page, STAGE_LABELS.COMPLETED);
    await tab.close();
    await expandSection(page, FINDINGS_SECTION);
    await expect(criticalResultBanner(page)).toContainText(
      text("pathology.banner.criticalResultSignedOut"),
    );

    // The alert is raised after the sign-out commits, so it is looked for
    // until it is listed.
    await expect(async () => {
      await page.goto("/Alerts", { waitUntil: "domcontentloaded" });
      await page.getByRole("searchbox").fill(seeded.accessionNumber);
      await expect(
        page.getByRole("row").filter({ hasText: seeded.accessionNumber }),
      ).toHaveCount(1, { timeout: 5_000 });
    }).toPass({ timeout: LONG_TIMEOUT });
    const alert = page
      .getByRole("row")
      .filter({ hasText: seeded.accessionNumber });
    await expect(alert).not.toContainText(critical.entry.name);
  });

  test("a signed-out case is reopened with a reason and signed out again", async ({
    page,
    context,
  }) => {
    const seeded = await seedReadyCase(page);
    const caseId = seeded.pathologySampleId;
    await signOutCase(page, seeded);
    const firstFinal = await newestVersion(page, caseId);
    await openCase(page, seeded);
    await expectStage(page, STAGE_LABELS.COMPLETED);

    await expect(primaryAction(page)).toHaveText(
      text("pathology.action.reopen"),
    );
    await primaryAction(page).click();
    const reason = page.locator("#reopenReason");
    await expect(reason).toBeFocused();
    const confirm = reopenButton(page);
    await expect(confirm).toBeDisabled();
    await expect(reopenRow(page)).toContainText(
      text("pathology.error.reopenReasonRequired"),
    );
    await reason.fill(REOPEN_REASON);
    await expect(confirm).toBeEnabled();
    await confirm.click();
    await expect(
      toast(
        page,
        text("pathology.toast.reopenedReportVoided", { version: firstFinal }),
      ),
    ).not.toHaveCount(0, { timeout: UI_TIMEOUT });
    await expectStage(page, STAGE_LABELS[seeded.readingStage]);
    await expect(page.locator("#microscopyExam")).toBeEnabled();
    await expandSection(page, REPORTS_SECTION);
    await expect(reportRow(page, firstFinal)).toContainText(
      text("pathology.report.state.VOIDED"),
    );
    await expect(
      reportRow(page, firstFinal).getByRole("link", {
        name: text("pathology.report.openVoided", { version: firstFinal }),
      }),
    ).toBeVisible();

    const secondFinal = await signOutFromBar(page, context, seeded);
    expect(secondFinal).toBeGreaterThan(firstFinal);
    await expect(reportRow(page, secondFinal)).toContainText(
      text("pathology.report.type.FINAL"),
    );
    await expect(reportRow(page, secondFinal)).not.toContainText(
      text("pathology.report.state.VOIDED"),
    );
    await expect(reportRow(page, firstFinal)).toContainText(
      text("pathology.report.state.VOIDED"),
    );
    const reports = (await readCase(page, caseId)).reports ?? [];
    expect(reports.find((r) => r.version === firstFinal)?.state).toBe("VOIDED");
    expect(reports.find((r) => r.version === secondFinal)?.state).toBe(
      "CURRENT",
    );
  });

  test("an uploaded file is listed and can be withdrawn", async ({ page }) => {
    await onApplication(page);
    const seeded = await createPathologyCase(page);
    await openCase(page, seeded);
    await expandSection(page, REPORTS_SECTION);

    await reportUpload(page)
      .locator('input[type="file"]')
      .setInputFiles({
        name: "outside-report.png",
        mimeType: "image/png",
        buffer: Buffer.from(
          "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+a7V8AAAAASUVORK5CYII=",
          "base64",
        ),
      });
    await expect(toast(page, /Report version \d+ uploaded\./)).not.toHaveCount(
      0,
      { timeout: UI_TIMEOUT },
    );
    const version = await newestVersion(page, seeded.pathologySampleId);
    await expect(reportRow(page, version)).toContainText(
      text("pathology.report.type.EXTERNAL"),
    );

    await reportWithdrawButton(page, version).click();
    const question = page.getByRole("group", {
      name: text("pathology.confirm.withdrawReport", { version }),
    });
    await expect(question).toBeVisible();
    await question
      .getByRole("button", { name: text("pathology.action.withdrawReport") })
      .click();
    await expect(
      toast(page, text("pathology.toast.reportWithdrawn", { version })),
    ).not.toHaveCount(0, { timeout: UI_TIMEOUT });
    await expect(reportRow(page, version)).toContainText(
      text("pathology.report.state.VOIDED"),
    );
    // The row stays, now voided, and its version link takes the focus the
    // withdrawal started from.
    await expect(
      reportRow(page, version).getByRole("link", {
        name: text("pathology.report.openVoided", { version }),
      }),
    ).toBeFocused({ timeout: UI_TIMEOUT });
    await expect(reportWithdrawButton(page, version)).toHaveCount(0);
    await expect(
      reportsTable(page).getByRole("columnheader", {
        name: text("common.actions"),
      }),
    ).toHaveCount(0);
  });

  test("focus goes to Reports after sign-out and to Findings after a reopen", async ({
    page,
    context,
  }) => {
    const seeded = await seedReadyCase(page);
    await openCase(page, seeded);

    // Cancel puts focus back on the primary and sends nothing.
    await primaryAction(page).click();
    await expect(signOutConfirm(page)).toBeVisible();
    await actionBarButton(page, text("common.cancel")).click();
    await expect(signOutConfirm(page)).toHaveCount(0);
    await expect(primaryAction(page)).toBeFocused();

    await signOutFromBar(page, context, seeded);
    await expect(sectionHeader(page, REPORTS_SECTION)).toBeFocused({
      timeout: UI_TIMEOUT,
    });

    await primaryAction(page).click();
    await page.locator("#reopenReason").fill(REOPEN_REASON);
    await reopenButton(page).click();
    await expectStage(page, STAGE_LABELS[seeded.readingStage]);
    await expect(sectionHeader(page, FINDINGS_SECTION)).toBeFocused({
      timeout: UI_TIMEOUT,
    });
    await expect(sectionHeader(page, FINDINGS_SECTION)).toHaveAttribute(
      "aria-expanded",
      "true",
    );
  });

  test("a question in the bar holds only its own two buttons, and Cancel gives focus back to the primary", async ({
    page,
  }) => {
    const seeded = await seedReadyCase(page);
    await openCase(page, seeded);
    const cancel = text("common.cancel");

    // The sign-out question: Cancel, then Sign out where the primary was.
    await expect(primaryAction(page)).toHaveText(
      text("pathology.action.signOut"),
    );
    await expect(primaryAction(page)).toBeEnabled({ timeout: UI_TIMEOUT });
    await primaryAction(page).click();
    await expect(signOutConfirm(page)).toBeVisible();
    await expect(actionBarButtons(page)).toHaveText([
      cancel,
      text("pathology.action.confirmSignOut"),
    ]);
    await expect(signOutButton(page)).toBeFocused();
    await expectNoSidewaysScrollAt(page, DESKTOP_WIDTHS);
    await actionBarButton(page, cancel).click();
    await expect(signOutConfirm(page)).toHaveCount(0);
    await expect(actionBarButtons(page)).toHaveText([
      text("caseView.action.discard"),
      text("caseView.action.saveDraft"),
      text("pathology.action.signOut"),
    ]);
    await expect(primaryAction(page)).toBeFocused();

    // The reopen row, once the case is signed out: Cancel, then Reopen.
    await signOutCase(page, seeded);
    await openCase(page, seeded);
    await expectStage(page, STAGE_LABELS.COMPLETED);
    await expect(primaryAction(page)).toHaveText(
      text("pathology.action.reopen"),
    );
    await primaryAction(page).click();
    await expect(page.locator("#reopenReason")).toBeFocused();
    await expect(actionBarButtons(page)).toHaveText([
      cancel,
      text("pathology.action.confirmReopen"),
    ]);
    await expectNoSidewaysScrollAt(page, DESKTOP_WIDTHS);
    await actionBarButton(page, cancel).click();
    await expect(reopenRow(page)).toHaveCount(0);
    await expect(actionBarButtons(page)).toHaveText([
      text("caseView.action.discard"),
      text("pathology.action.reopen"),
    ]);
    await expect(primaryAction(page)).toBeFocused();
  });

  test("Generate report and Upload report share one top, with the upload hint under Upload report, at 1480, 1366 and 1280", async ({
    page,
  }) => {
    await onApplication(page);
    // No findings yet, so Generate is held with its reason, which must not
    // push the two buttons out of line.
    const seeded = await createPathologyCase(page);
    const section = page.locator(`#${REPORTS_SECTION}`);
    for (const width of DESKTOP_WIDTHS) {
      await page.setViewportSize({ width, height: 900 });
      await openCase(page, seeded);
      await expandSection(page, REPORTS_SECTION);
      const generate = reportGenerateButton(page);
      // Carbon's uploader also holds a hidden file input with the same name,
      // and its own hidden copy of the hint: take the button and the hint
      // that are on screen.
      const upload = reportUpload(page).locator("button", {
        hasText: text("pathology.action.uploadReport"),
      });
      const hint = section.locator("#reportUploadHint");
      await expect(hint).toHaveText(text("clinicalAttachment.uploadHint"));
      await expect(
        section.getByText(text("pathology.error.reportNeedsFindings")),
      ).toBeVisible();
      await expect(upload).toBeVisible();
      await expect(hint).toBeVisible();
      const [g, u, h] = await Promise.all([
        generate.boundingBox(),
        upload.boundingBox(),
        hint.boundingBox(),
      ]);
      expect(
        Math.abs(g!.y - u!.y),
        `Generate and Upload share a top at ${width}`,
      ).toBeLessThan(1);
      // The hint is the upload button's: right under it, starting at its
      // left edge, never under Generate.
      const gap = h!.y - (u!.y + u!.height);
      expect(
        gap,
        `the hint is under Upload at ${width}`,
      ).toBeGreaterThanOrEqual(0);
      expect(gap, `the hint is close under Upload at ${width}`).toBeLessThan(
        17,
      );
      expect(
        Math.abs(h!.x - u!.x),
        `the hint starts at Upload's left edge at ${width}`,
      ).toBeLessThan(2);
    }
  });

  test("reports at 1480, 1366 and 1280 do not scroll sideways, and pass axe", async ({
    page,
  }) => {
    await onApplication(page);
    const seeded = await createPathologyCase(page);
    const caseId = seeded.pathologySampleId;
    for (let index = 0; index < 5; index += 1) {
      await uploadReport(page, caseId);
    }
    // The author is whoever stored the row; a long one is put on each row of
    // the read the screen loads, so the widest name the table must hold is
    // there without a user of that name.
    await page.route(`**/rest/pathology/caseView/${caseId}`, async (route) => {
      if (route.request().method() !== "GET") {
        return route.continue();
      }
      const response = await route.fetch();
      const body = await response.json();
      for (const report of body.reports ?? []) {
        report.generatedBy = LONG_AUTHOR;
      }
      await route.fulfill({ response, json: body });
    });

    for (const width of DESKTOP_WIDTHS) {
      await page.setViewportSize({ width, height: 900 });
      await openCase(page, seeded);
      await expandSection(page, REPORTS_SECTION);
      await expect(reportsTable(page).locator("tbody tr")).toHaveCount(5);
      await expect(reportsTable(page)).toContainText(LONG_AUTHOR);
      const overflow = await page.evaluate((sectionId) => {
        const root = document.documentElement;
        const section = document.getElementById(sectionId)!;
        const table = section.querySelector(".cds--data-table-content");
        return {
          page: root.scrollWidth - root.clientWidth,
          section: section.scrollWidth - section.clientWidth,
          table: table ? table.scrollWidth - table.clientWidth : 0,
        };
      }, REPORTS_SECTION);
      expect(overflow, `no sideways scroll at ${width}`).toEqual({
        page: 0,
        section: 0,
        table: 0,
      });
    }

    const results = await new AxeBuilder({ page })
      .include(`#${REPORTS_SECTION}`)
      .withTags(WCAG_21_AA_TAGS)
      .analyze();
    const blocking = results.violations
      .filter(({ impact }) => impact === "critical" || impact === "serious")
      .map(({ id, impact, help, nodes }) => ({
        id,
        impact,
        help,
        targets: nodes.map((node) => node.target),
      }));
    expect(blocking, JSON.stringify(blocking, null, 2)).toEqual([]);
  });

  test("a user outside the unit cannot read a report file", async ({
    page,
    browser,
    baseURL,
  }) => {
    await onApplication(page);
    const seeded = await createPathologyCase(page);
    const caseId = seeded.pathologySampleId;
    const uploaded = await uploadReport(page, caseId);
    const address = reportFilePath(caseId, uploaded.id);
    expect((await page.request.get(address)).status()).toBe(200);

    const username = await createUserOutsidePathology(page);
    const other = await signIn(browser, baseURL!, username);
    try {
      await expect(async () => {
        expect((await other.page.request.get(address)).status()).toBe(404);
      }).toPass({ timeout: NAV_TIMEOUT });
      expect(
        (
          await other.page.request.get(
            `${API_PREFIX}/rest/pathology/caseView/${caseId}`,
          )
        ).status(),
        "the case itself is out of reach too",
      ).toBe(404);
    } finally {
      await other.context.close();
    }
  });
});
