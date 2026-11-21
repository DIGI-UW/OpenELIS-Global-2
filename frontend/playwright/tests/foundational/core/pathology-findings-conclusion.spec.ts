import AxeBuilder from "@axe-core/playwright";
import { writeFile } from "fs/promises";
import type { Browser, Page } from "@playwright/test";
import { test, expect } from "../../../helpers/test-base";
import {
  DIAGNOSIS_CATEGORY,
  TECHNIQUE_CATEGORY,
  apiGet,
  createDictionaryEntry,
  createPathologyCase,
  discoverPathologyOrderTarget,
  ensureTissueType,
  moveCaseToStage,
  readCase,
  saveCaseThroughApi,
  type DictionaryEntry,
  type SeededPathologyCase,
  type TissueType,
} from "../../../helpers/seed-pathology-data";
import {
  FINDINGS_SECTION,
  REVIEW_SECTION,
  STAGE_LABELS,
  addDiagnosisControl,
  advanceCase,
  diagnosisBox,
  diagnosisChips,
  expandSection,
  expectStage,
  expectStageSwitchesOn,
  openCase,
  pickDiagnosis,
  primaryAction,
  returnCase,
  saveDraft,
  searchDiagnoses,
  sectionHeader,
  slidesReceivedTable,
  techniqueChips,
} from "../../../helpers/pathology-case-view";
import { text } from "../../../helpers/app-messages";
import { tickCarbonMultiSelectOption } from "../../../helpers/carbon-select";
import {
  LONG_TIMEOUT,
  NAV_TIMEOUT,
  UI_TIMEOUT,
} from "../../../helpers/timeouts";

/**
 * Findings and the coded conclusion (FR-12, FR-13): the pathologist begins
 * the review from the queue, writes both descriptions, the techniques, the
 * diagnoses found by code or by name, the narrative and the review notes,
 * and signs the case out; the Review section lists the slides received with
 * the checks the bench recorded.
 *
 * What only a deployed stack proves here:
 *   - Begin review moves the case to Under Pathologist Review and names the
 *     pathologist, and what is written then survives a reload,
 *   - the diagnosis search answers from the server by code and by name,
 *   - release waits for both descriptions, and a signed-out case is read-only,
 *   - a referral reaches the Immunohistochemistry dashboard once,
 *   - findings returned to the queue are locked, and kept for the next review,
 *   - a user without the Pathologist role reads and never writes, and
 *   - Review and Findings fit at three desktop widths and pass axe.
 *
 * Serial: the tests share one seeded case and its own dictionary entries; the
 * tests that sign out or refer a case seed their own. Each test brings the
 * shared case to the stage it needs first. Entry names and abbreviations
 * carry a per-run tag, so the spec runs again on the same stack. The
 * signed-in user must hold the Pathologist role and the laboratory must use
 * the review stage; the spec checks both first.
 */

const API_PREFIX = "/api/OpenELIS-Global";

const WCAG_21_AA_TAGS = ["wcag2a", "wcag2aa", "wcag21a", "wcag21aa"];

/** The widths Review and Findings must fit at without scrolling sideways. */
const DESKTOP_WIDTHS = [1480, 1366, 1280];

/** The widths the evidence pictures are taken at. */
const SCREENSHOT_WIDTHS = [1480, 1280];

/** This run's tag, kept short so an abbreviation stays within ten characters. */
const RUN =
  `${Date.now().toString(36)}${Math.floor(Math.random() * 36 * 36).toString(36)}`
    .slice(-7)
    .toUpperCase();

const GROSS_SEED = "Breast, core biopsy, three cores, 1.2 cm in aggregate.";
const GROSS_WRITTEN =
  "Breast, core biopsy: three tan cores, 1.4 cm in aggregate, all submitted.";
const MICROSCOPY_WRITTEN =
  "Infiltrating glands with moderate atypia in a desmoplastic stroma.";
const NARRATIVE_WRITTEN = "Invasive carcinoma, see the coded diagnoses.";
const NOTES_WRITTEN = "Discussed at the breast meeting.";
const RETURN_REASON = "Recut A2 before the review goes on";
const REFERRAL_MICROSCOPY =
  "Sheets of atypical cells; markers asked for to type them.";
const REFERRAL_NARRATIVE = "Markers ordered on block A1.";

/** A diagnosis as the picker lists it: "code name". */
interface Diagnosis {
  entry: DictionaryEntry;
  code: string;
  label: string;
}

let tissue: TissueType;
let technique: DictionaryEntry;
/** Found by name in the main walk. */
let byName: Diagnosis;
/** Found by code in the main walk. */
let byCode: Diagnosis;
/** Six diagnoses with long names, for the layout check. */
const longDiagnoses: Diagnosis[] = [];
/** The case the tests share. */
let shared: SeededPathologyCase;
/** The signed-in pathologist's last name, which the header shows. */
let lastName = "";

/** Open a fresh, signed-in page for a hook. */
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
 * A case with the pathologist, holding two blocks (A1, A2) and one slide
 * under each, stained and passed. The bench's work is done through the case
 * save and the move endpoints, since it is not what this spec is about.
 */
async function seedQueuedCase(page: Page): Promise<SeededPathologyCase> {
  // The seed calls carry the application's session, so a test's fresh page
  // is put on the application first.
  if (!page.url().startsWith("http")) {
    await page.goto("/", { waitUntil: "domcontentloaded" });
  }
  const seededCase = await createPathologyCase(
    page,
    await discoverPathologyOrderTarget(page),
  );
  const caseId = seededCase.pathologySampleId;

  await moveCaseToStage(page, seededCase, "GROSSING");
  await saveCaseThroughApi(page, caseId, await readCase(page, caseId), {
    grossExam: GROSS_SEED,
    addCassette: true,
    cassetteTissueTypeId: tissue.id,
  });
  await saveCaseThroughApi(page, caseId, await readCase(page, caseId), {
    addCassette: true,
    cassetteTissueTypeId: tissue.id,
  });
  await moveCaseToStage(page, seededCase, "MICROTOMY");

  const blocks = (await readCase(page, caseId)).blocks ?? [];
  for (const designation of ["A1", "A2"]) {
    const block = blocks.find((entry) => entry.designation === designation);
    expect(block?.id, `block ${designation} was cut`).toBeDefined();
    await saveCaseThroughApi(page, caseId, await readCase(page, caseId), {
      addSlideUnder: block!.id,
    });
  }

  await moveCaseToStage(page, seededCase, "READY_PATHOLOGIST");
  expect((await readCase(page, caseId)).status).toBe("READY_PATHOLOGIST");
  return seededCase;
}

/** Open the case with Review and Findings expanded. */
/** Put a test's fresh page on the application, for calls carrying its session. */
async function onApplication(page: Page) {
  if (!page.url().startsWith("http")) {
    await page.goto("/", { waitUntil: "domcontentloaded" });
  }
}

async function openReviewAndFindings(page: Page, seeded: SeededPathologyCase) {
  await openCase(page, seeded);
  await expandSection(page, REVIEW_SECTION);
  await expandSection(page, FINDINGS_SECTION);
}

/** Random lower-case letters, for a user name. */
const letters = (n: number) =>
  Array.from(
    { length: n },
    () => "abcdefghijklmnopqrstuvwxyz"[Math.floor(Math.random() * 26)],
  ).join("");

/**
 * A user who enters results in every lab unit and holds no other role, the
 * Pathologist role included. Returns the login name.
 */
async function createUserWithoutPathologistRole(page: Page) {
  await page.goto("/", { waitUntil: "domcontentloaded" });
  const endpoint = `${API_PREFIX}/rest/UnifiedSystemUser`;
  const form = await (await page.request.get(endpoint)).json();
  const results = form.labUnitRoles.find(
    (role: { roleName: string }) => role.roleName === "Results",
  );
  expect(results?.roleId).toBeTruthy();
  const password = process.env.TEST_PASS;
  expect(password, "TEST_PASS is required for the second user").toBeTruthy();
  const csrf = await page.evaluate(() => localStorage.getItem("CSRF") || "");
  const username = `pwpath${letters(8)}`;
  const created = await page.request.post(endpoint, {
    headers: { "X-CSRF-Token": csrf },
    data: {
      userLoginName: username,
      userPassword: password,
      confirmPassword: password,
      userFirstName: "Bench",
      userLastName: username,
      expirationDate: form.expirationDate,
      timeout: form.timeout,
      accountActive: "Y",
      accountDisabled: "N",
      accountLocked: "N",
      allowCopyUserRoles: "N",
      selectedRoles: [],
      selectedTestSectionLabUnits: { AllLabUnits: [results.roleId] },
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

/** The case's row on the Immunohistochemistry dashboard, listed under All. */
async function ihcDashboardRows(page: Page, accessionNumber: string) {
  await page.goto("/ImmunohistochemistryDashboard", {
    waitUntil: "domcontentloaded",
  });
  await expect(
    page.getByRole("heading", { name: "Immunohistochemistry", exact: true }),
  ).toBeVisible({ timeout: NAV_TIMEOUT });
  const stageFilter = page.locator("select#statusFilter");
  await expect(stageFilter.locator('option[value="All"]')).toHaveCount(1, {
    timeout: NAV_TIMEOUT,
  });
  // The dashboard's first load can land after the pick and put its own
  // grouping back, so the pick is repeated until it holds.
  await expect(async () => {
    await stageFilter.selectOption("All");
    await expect(stageFilter).toHaveValue("All", { timeout: 1000 });
  }).toPass({ timeout: NAV_TIMEOUT });
  await page
    .getByRole("searchbox", { name: "Search by LabNo or Family Name" })
    .fill(accessionNumber);
  return page.getByRole("row").filter({ hasText: accessionNumber });
}

/**
 * The techniques picker's search box. The id sits on the Carbon wrapper, so
 * the combobox inside it is what is typed into and what is disabled.
 */
function techniquesBox(page: Page) {
  return page.locator("#techniques").getByRole("combobox");
}

/** Every control of Review and Findings that can change the case. */
const WRITING_CONTROLS = [
  "#assignedPathologist",
  "#reviewNotes",
  "#grossExam",
  "#microscopyExam",
  '#techniques [role="combobox"]',
  "#conclusion",
  "#conclusionText",
];

test.describe.serial("Pathology findings and conclusion", () => {
  // Order entry and the case view are both heavy screens, and a first visit
  // on a development server compiles them as it serves them.
  test.setTimeout(240_000);

  test.beforeAll(async ({ browser }) => {
    test.setTimeout(300_000);
    await withOwnPage(browser, async (page) => {
      await expectStageSwitchesOn(page, [
        "EMBEDDING",
        "MICROTOMY",
        "STAINING",
        "COVERSLIPPING",
        "UNDER_REVIEW",
      ]);
      const session = await apiGet<{
        lastName?: string;
        roles?: string[];
      }>(page, `${API_PREFIX}/session`);
      if (Array.isArray(session.roles)) {
        expect(
          session.roles,
          "the signed-in user begins the review, which only the pathologist does",
        ).toContain("Pathologist");
      }
      lastName = session.lastName ?? "";

      tissue = await ensureTissueType(page);
      technique = await createDictionaryEntry(
        page,
        TECHNIQUE_CATEGORY,
        `E2E technique ${RUN}`,
        `T${RUN}`,
        "so the changeset that creates the pathology techniques has not run here.",
      );
      byName = await addDiagnosis(page, `Adenocarcinoma ${RUN}`, `D1${RUN}`);
      byCode = await addDiagnosis(page, `Fibroadenoma ${RUN}`, `D2${RUN}`);
      for (let index = 1; index <= 6; index += 1) {
        longDiagnoses.push(
          await addDiagnosis(
            page,
            `Invasive ductal carcinoma of no special type, Nottingham grade ${index}, ${RUN}`,
            `L${index}${RUN}`,
          ),
        );
      }
      shared = await seedQueuedCase(page);
    });
  });

  test("the pathologist begins the review, writes and saves the findings, and after sign-out every control is read-only", async ({
    page,
  }) => {
    const seeded = await seedQueuedCase(page);
    const caseId = seeded.pathologySampleId;

    await openCase(page, seeded);
    await advanceCase(page, { label: text("pathology.action.beginReview") });
    await expectStage(page, STAGE_LABELS.UNDER_REVIEW);
    // Begin review opens Findings & Conclusion by itself and puts the cursor
    // in the microscopic description.
    await expect(sectionHeader(page, FINDINGS_SECTION)).toHaveAttribute(
      "aria-expanded",
      "true",
    );
    await expect(page.locator("#microscopyExam")).toBeFocused();
    // The header band names who the case is assigned to; Begin review
    // assigns the pathologist who pressed it.
    expect(lastName, "the session names the signed-in user").not.toBe("");
    await expect(
      page
        .getByTestId("case-assigned-staff")
        .filter({ hasText: text("assigned.pathologist.label") }),
    ).toContainText(lastName);

    await expandSection(page, REVIEW_SECTION);
    await expandSection(page, FINDINGS_SECTION);
    await page.locator("#grossExam").fill(GROSS_WRITTEN);
    await page.locator("#microscopyExam").fill(MICROSCOPY_WRITTEN);
    await tickCarbonMultiSelectOption(techniquesBox(page), technique.name);
    await techniquesBox(page).press("Escape");
    await expect(techniqueChips(page)).toContainText(technique.name);
    await pickDiagnosis(page, byCode.code, byCode.label);
    // Case is folded on the server, so the name is searched in lower case.
    await pickDiagnosis(page, byName.entry.name.toLowerCase(), byName.label);
    await page.locator("#conclusionText").fill(NARRATIVE_WRITTEN);
    await page.locator("#reviewNotes").fill(NOTES_WRITTEN);
    await saveDraft(page, caseId);

    await openReviewAndFindings(page, seeded);
    await expect(page.locator("#grossExam")).toHaveValue(GROSS_WRITTEN);
    await expect(page.locator("#microscopyExam")).toHaveValue(
      MICROSCOPY_WRITTEN,
    );
    await expect(techniqueChips(page)).toContainText(technique.name);
    await expect(diagnosisChips(page)).toContainText(byCode.label);
    await expect(diagnosisChips(page)).toContainText(byName.label);
    await expect(page.locator("#conclusionText")).toHaveValue(
      NARRATIVE_WRITTEN,
    );
    await expect(page.locator("#reviewNotes")).toHaveValue(NOTES_WRITTEN);

    const release = page.locator("#release");
    await expect(release).toBeEnabled();
    await page.locator('label[for="release"]').click();
    await expect(release).toBeChecked();
    await saveDraft(page, caseId);
    await expectStage(page, STAGE_LABELS.COMPLETED);

    await openReviewAndFindings(page, seeded);
    for (const control of [...WRITING_CONTROLS, "#release"]) {
      await expect(
        page.locator(control),
        `${control} is read-only`,
      ).toBeDisabled();
    }
    await expect(diagnosisChips(page)).toContainText(byCode.label);
    await expect(diagnosisChips(page).getByRole("button")).toHaveCount(0);
    await expect(techniqueChips(page).getByRole("button")).toHaveCount(0);
  });

  test("Review and Findings fit at three desktop widths with six long diagnoses, the menu is not clipped, and both pass axe", async ({
    page,
  }, testInfo) => {
    await page.setViewportSize({ width: 1480, height: 900 });
    await onApplication(page);
    await moveCaseToStage(page, shared, "UNDER_REVIEW");
    await openReviewAndFindings(page, shared);
    for (const diagnosis of longDiagnoses) {
      await pickDiagnosis(page, diagnosis.code, diagnosis.label);
    }
    await diagnosisBox(page).press("Escape");
    await expect(diagnosisChips(page).getByRole("button")).toHaveCount(6);

    for (const width of DESKTOP_WIDTHS) {
      await page.setViewportSize({ width, height: 900 });
      await expect(diagnosisChips(page)).toBeVisible();
      const measured = await page.evaluate(() => {
        const root = document.documentElement;
        return {
          pageScrollWidth: root.scrollWidth,
          pageClientWidth: root.clientWidth,
        };
      });
      const line = `review and findings at ${width}: page ${measured.pageScrollWidth}/${measured.pageClientWidth} (scrollWidth/clientWidth)`;
      // Printed for the walk notes, and kept on the report.
      console.log(line);
      testInfo.annotations.push({ type: "layout", description: line });
      // One pixel covers rounding.
      expect(
        measured.pageScrollWidth,
        `the page scrolls sideways at ${width} wide`,
      ).toBeLessThanOrEqual(measured.pageClientWidth + 1);

      if (SCREENSHOT_WIDTHS.includes(width)) {
        await page.evaluate(() => window.scrollTo(0, 0));
        await page.screenshot({
          path: new URL(
            `../../../../e2e-evidence/ogc-264-s13-findings-${width}.png`,
            import.meta.url,
          ).pathname,
          fullPage: true,
        });
      }
    }

    // A long chip ends in an ellipsis inside its row: the code at its start
    // and the remove button at its end both stay inside the row.
    const outsideTheRow = await page
      .getByTestId("findings-conclusion-chips")
      .evaluate((row) => {
        const bounds = row.getBoundingClientRect();
        return Array.from(row.querySelectorAll(".cds--tag"))
          .flatMap((tag) => [
            tag,
            ...Array.from(tag.querySelectorAll(".cds--tag__label, button")),
          ])
          .filter((element) => {
            const rect = element.getBoundingClientRect();
            return rect.left < bounds.left - 1 || rect.right > bounds.right + 1;
          }).length;
      });
    expect(outsideTheRow, "a chip runs out of its row").toBe(0);

    // At the narrowest width the menu opens under six chips: its last item
    // is inside the viewport and is what the page shows at that point, so
    // neither the accordion nor anything else cuts it off.
    const menu = await searchDiagnoses(page, RUN.toLowerCase());
    const last = menu.getByRole("option").last();
    await expect(last).toBeVisible();
    await last.scrollIntoViewIfNeeded();
    const viewport = page.viewportSize()!;
    const box = (await last.boundingBox())!;
    expect(box.x).toBeGreaterThanOrEqual(0);
    expect(box.y).toBeGreaterThanOrEqual(0);
    expect(box.x + box.width).toBeLessThanOrEqual(viewport.width);
    expect(box.y + box.height).toBeLessThanOrEqual(viewport.height);
    const shown = await last.evaluate((element) => {
      const rect = element.getBoundingClientRect();
      const hit = document.elementFromPoint(
        rect.left + rect.width / 2,
        rect.top + rect.height / 2,
      );
      return Boolean(hit) && (hit === element || element.contains(hit));
    });
    expect(shown, "the diagnosis menu is cut off").toBe(true);
    await diagnosisBox(page).press("Escape");

    await page.setViewportSize({ width: 1480, height: 900 });
    // The shared header's notification bell is named "Notifications" while
    // its visible text is the unread count, which axe reports on every page
    // of the application. It is the layout's, not these sections'.
    const scan = await new AxeBuilder({ page })
      .withTags(WCAG_21_AA_TAGS)
      .include(`#${REVIEW_SECTION}`)
      .include(`#${FINDINGS_SECTION}`)
      .exclude("#notification-Icon")
      .analyze();
    const jsonPath = testInfo.outputPath(
      "axe-pathology-findings-conclusion.json",
    );
    await writeFile(jsonPath, `${JSON.stringify(scan.violations, null, 2)}\n`);
    await testInfo.attach("axe-pathology-findings-conclusion", {
      path: jsonPath,
      contentType: "application/json",
    });
    const blocking = scan.violations
      .filter(({ impact }) => impact === "serious" || impact === "critical")
      .map(({ id, impact, help, nodes }) => ({
        id,
        impact,
        help,
        targets: nodes.map((node) => node.target),
      }));
    expect(blocking, JSON.stringify(blocking, null, 2)).toEqual([]);
  });

  test("release is disabled with its reason until the microscopic description is written", async ({
    page,
  }) => {
    await onApplication(page);
    await moveCaseToStage(page, shared, "UNDER_REVIEW");
    expect(
      (
        (await readCase(page, shared.pathologySampleId)).microscopyExam ?? ""
      ).trim(),
      "the shared case starts with no microscopic description",
    ).toBe("");
    await openReviewAndFindings(page, shared);

    const reason = page
      .locator(`#${FINDINGS_SECTION}`)
      .getByText(text("pathology.error.releaseWithoutDescriptions"));
    await expect(page.locator("#release")).toBeDisabled();
    await expect(reason).toBeVisible();

    await page.locator("#microscopyExam").fill(MICROSCOPY_WRITTEN);
    await expect(reason).toHaveCount(0);
    await expect(page.locator("#release")).toBeEnabled();
  });

  test("a referral with one marker shows as referred after the save, reaches Immunohistochemistry once, and a second save orders nothing more", async ({
    page,
  }) => {
    const seeded = await seedQueuedCase(page);
    const caseId = seeded.pathologySampleId;
    await moveCaseToStage(page, seeded, "UNDER_REVIEW");
    await openReviewAndFindings(page, seeded);

    // The findings travel with the referral, so they are written with it.
    await page.locator("#microscopyExam").fill(REFERRAL_MICROSCOPY);
    await pickDiagnosis(page, byCode.code, byCode.label);
    await page.locator('label[for="referToImmunoHistoChemistry"]').click();
    await expect(page.locator("#referToImmunoHistoChemistry")).toBeChecked();
    const markers = page.locator("#ihctests").getByRole("combobox");
    await expect(markers).toBeEnabled();
    await markers.click();
    const markerList = page.locator(
      `[id="${await markers.getAttribute("aria-controls")}"]`,
    );
    const firstMarker = markerList.getByRole("option").first();
    await expect(
      firstMarker,
      "the stack offers no immunohistochemistry marker",
    ).toBeVisible();
    const markerName = ((await firstMarker.textContent()) ?? "").trim();
    await firstMarker.click();
    await expect(firstMarker).toHaveAttribute("aria-selected", "true");
    await markers.press("Escape");
    await saveDraft(page, caseId);

    await openReviewAndFindings(page, seeded);
    await expect(page.getByTestId("findings-ihc-referred")).toBeVisible();
    await expect(
      page.getByTestId("findings-ihc-referred-markers"),
    ).toContainText(markerName);
    await expect(page.locator("#referToImmunoHistoChemistry")).toBeChecked();
    await expect(page.locator("#referToImmunoHistoChemistry")).toBeDisabled();

    const rows = await ihcDashboardRows(page, seeded.accessionNumber);
    await expect(rows).toHaveCount(1, { timeout: LONG_TIMEOUT });
    await rows
      .getByRole("cell", { name: seeded.accessionNumber, exact: true })
      .click();
    await expect(page).toHaveURL(/\/ImmunohistochemistryCaseView\/\d+$/, {
      timeout: NAV_TIMEOUT,
    });
    await expect(page.locator("#blockNumber_0")).toBeVisible({
      timeout: NAV_TIMEOUT,
    });
    await expect(page.locator("#blockNumber_0")).not.toHaveValue("");
    // The referred block arrives with the pathology findings printed beside it.
    await expect(
      page.locator("p.pathology-exam-text").filter({
        hasText: REFERRAL_MICROSCOPY,
      }),
    ).toBeVisible();
    await expect(
      page.locator(".cds--tag").filter({ hasText: byCode.entry.name }),
    ).toBeVisible();

    await openReviewAndFindings(page, seeded);
    await page.locator("#conclusionText").fill(REFERRAL_NARRATIVE);
    await saveDraft(page, caseId);
    const after = await ihcDashboardRows(page, seeded.accessionNumber);
    await expect(after).toHaveCount(1, { timeout: LONG_TIMEOUT });
  });

  test("returned to the queue the findings are locked behind the hint, and begun again they are still there", async ({
    page,
  }) => {
    const caseId = shared.pathologySampleId;
    await onApplication(page);
    await moveCaseToStage(page, shared, "UNDER_REVIEW");
    await saveCaseThroughApi(page, caseId, await readCase(page, caseId), {
      microscopyExam: MICROSCOPY_WRITTEN,
      conclusionText: NARRATIVE_WRITTEN,
    });

    await openCase(page, shared);
    await returnCase(page, RETURN_REASON);
    await expectStage(page, STAGE_LABELS.READY_PATHOLOGIST);
    await expect(sectionHeader(page, FINDINGS_SECTION)).toBeDisabled();
    await expect(
      page
        .locator(`#${FINDINGS_SECTION}`)
        .getByText(text("pathology.locked.beginReviewFirst")),
    ).toBeVisible();
    await expect(page.locator("#microscopyExam")).toHaveCount(0);

    await advanceCase(page, { label: text("pathology.action.beginReview") });
    await expandSection(page, FINDINGS_SECTION);
    await expect(page.locator("#microscopyExam")).toHaveValue(
      MICROSCOPY_WRITTEN,
    );
    await expect(page.locator("#conclusionText")).toHaveValue(
      NARRATIVE_WRITTEN,
    );
  });

  test("a user without the Pathologist role sees no Begin review, reads both sections under review, and is not offered the add-diagnosis control", async ({
    page,
    browser,
    baseURL,
  }) => {
    await onApplication(page);
    await moveCaseToStage(page, shared, "READY_PATHOLOGIST");
    const username = await createUserWithoutPathologistRole(page);
    const other = await signIn(browser, baseURL!, username);
    try {
      await openCase(other.page, shared);
      await expectStage(other.page, STAGE_LABELS.READY_PATHOLOGIST);
      await expect(primaryAction(other.page)).toHaveCount(0);

      await onApplication(page);
      await moveCaseToStage(page, shared, "UNDER_REVIEW");
      await openReviewAndFindings(other.page, shared);
      await expectStage(other.page, STAGE_LABELS.UNDER_REVIEW);
      for (const control of [...WRITING_CONTROLS, "#release"]) {
        await expect(
          other.page.locator(control),
          `${control} is read-only`,
        ).toBeDisabled();
      }
      // The control is not rendered at all for a picker that cannot be edited.
      await expect(addDiagnosisControl(other.page)).toHaveCount(0);
    } finally {
      await other.context.close();
    }
  });

  test("Slides received lists both slides with their checks", async ({
    page,
  }) => {
    await onApplication(page);
    await moveCaseToStage(page, shared, "UNDER_REVIEW");
    await openCase(page, shared);
    await expandSection(page, REVIEW_SECTION);

    await expect(
      page.locator(`#${REVIEW_SECTION}`).getByRole("heading", {
        name: text("pathology.label.slidesReceived", { count: 2 }),
      }),
    ).toBeVisible({ timeout: UI_TIMEOUT });
    const rows = slidesReceivedTable(page).locator("tbody tr");
    await expect(rows).toHaveCount(2);
    for (const designation of ["A1", "A2"]) {
      const row = rows.filter({ hasText: designation });
      await expect(row).toHaveCount(1);
      await expect(row).toContainText(text("pathology.badge.verified"));
    }
  });
});
