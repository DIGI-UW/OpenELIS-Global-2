import AxeBuilder from "@axe-core/playwright";
import { writeFile } from "fs/promises";
import type { Browser, Page } from "@playwright/test";
import { test, expect } from "../../../helpers/test-base";
import {
  createConsultationReason,
  createPathologistUser,
  createPathologyCase,
  discoverPathologyOrderTarget,
  ensureTissueType,
  moveCaseToStage,
  readCase,
  requestConsultation,
  saveCaseThroughApi,
  signInAsUser,
  signOutCase,
  type DictionaryEntry,
  type SecondPathologist,
  type SeededPathologyCase,
  type TissueType,
} from "../../../helpers/seed-pathology-data";
import {
  REVIEW_SECTION,
  STAGE_LABELS,
  askColleagueButton,
  askColleagueHint,
  consultationAnswerConfirmButton,
  consultationAnswerHint,
  consultationColleagueBox,
  consultationColleagueOption,
  consultationNote,
  consultationOutcomeLabel,
  consultationPreTreatmentLabel,
  consultationReasonSelect,
  consultationRow,
  consultationRows,
  consultationWithdrawConfirmButton,
  consultationWithdrawRow,
  consultationsRegion,
  expandSection,
  expectStage,
  expectStageSwitchesOn,
  newConsultationRow,
  openCase,
  recordOutcomeButton,
  requestConsultationButton,
  sectionHeader,
  summaryRow,
  waitForConsultationChange,
  withdrawConsultationButton,
} from "../../../helpers/pathology-case-view";
import { text } from "../../../helpers/app-messages";
import { NAV_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";

/**
 * The second opinion (FR-11): a pathologist asks a colleague for a second
 * opinion from the Review section, the colleague records the outcome on the
 * same case, and the asker can withdraw a request still awaited.
 *
 * What only a deployed stack proves here:
 *   - the request reaches the server and is read back after a reload, with
 *     the rail, the Review header and the summary saying it is awaited,
 *   - the colleague, signed in as themselves, records the outcome and the
 *     asker then reads it with its note and its time,
 *   - the asker is offered Withdraw and never Record the outcome, and the
 *     colleague can still answer once the case is signed out,
 *   - a user without the Pathologist role reads the rows and is offered
 *     nothing, and
 *   - the panel fits at three desktop widths with the request form open, a
 *     long colleague name and a 1,000-character note, and passes axe.
 *
 * Serial: the tests share one case under review, the second pathologist and
 * one reason, set up through the API; the sign-out test seeds its own case.
 * Names carry a per-run tag, so the spec runs again on the same stack. The
 * signed-in user must hold the Pathologist role and the laboratory must use
 * the review stage; the spec checks the stage switches first. A global
 * setting is never changed here: the suggestion rule is walked by hand.
 */

const API_PREFIX = "/api/OpenELIS-Global";

const WCAG_21_AA_TAGS = ["wcag2a", "wcag2aa", "wcag21a", "wcag21aa"];

/** The widths the panel must fit at without scrolling sideways. */
const DESKTOP_WIDTHS = [1480, 1366, 1280];

/** The widths the evidence pictures are taken at. */
const SCREENSHOT_WIDTHS = [1480, 1280];

/** This run's tag. */
const RUN =
  `${Date.now().toString(36)}${Math.floor(Math.random() * 36 * 36).toString(36)}`
    .slice(-7)
    .toUpperCase();

const DISAGREE_NOTE = `The architecture favours a reactive process ${RUN}.`;
/** The longest first name the user form takes (30 characters). */
const LONG_FIRST_NAME = "Wilhelmina Konstantinopoulouas";
/** A note at the length the outcome allows, words of ordinary length. */
const LONG_NOTE = "Reactive atypia favoured over dysplasia, see deeper levels. "
  .repeat(20)
  .slice(0, 1000);

let tissue: TissueType;
let colleague: SecondPathologist;
let longColleague: SecondPathologist;
let readerUsername = "";
let reason: DictionaryEntry;
/** The case under review the tests share. */
let shared: SeededPathologyCase;
/** The first second opinion on the shared case, answered in the second test. */
let firstOpinionId = 0;

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

/** Put a test's fresh page on the application, for calls carrying its session. */
async function onApplication(page: Page) {
  if (!page.url().startsWith("http")) {
    await page.goto("/", { waitUntil: "domcontentloaded" });
  }
}

/**
 * A case with two blocks and a slide under each, the bench's work done
 * through the case save and the move endpoints, at Ready for Pathologist.
 */
async function seedQueuedCase(page: Page): Promise<SeededPathologyCase> {
  await onApplication(page);
  const seededCase = await createPathologyCase(
    page,
    await discoverPathologyOrderTarget(page),
  );
  const caseId = seededCase.pathologySampleId;

  await moveCaseToStage(page, seededCase, "GROSSING");
  for (let index = 0; index < 2; index += 1) {
    await saveCaseThroughApi(page, caseId, await readCase(page, caseId), {
      grossExam: index === 0 ? "Breast, core biopsy, three cores." : undefined,
      addCassette: true,
      cassetteTissueTypeId: tissue.id,
    });
  }
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
  return seededCase;
}

/** Random lower-case letters, for a user name. */
const letters = (n: number) =>
  Array.from(
    { length: n },
    () => "abcdefghijklmnopqrstuvwxyz"[Math.floor(Math.random() * 26)],
  ).join("");

/**
 * A user who enters results in every lab unit and holds no other role, the
 * Pathologist role included. Returns the login name. The form's role names
 * are padded with spaces, so they are compared trimmed.
 */
async function createUserWithoutPathologistRole(page: Page) {
  const endpoint = `${API_PREFIX}/rest/UnifiedSystemUser`;
  const form = await (await page.request.get(endpoint)).json();
  const results = form.labUnitRoles.find(
    (role: { roleName: string }) => role.roleName.trim() === "Results",
  );
  expect(results?.roleId).toBeTruthy();
  const password = process.env.TEST_PASS;
  expect(password, "TEST_PASS is required for the second user").toBeTruthy();
  const csrf = await page.evaluate(() => localStorage.getItem("CSRF") || "");
  const username = `pwread${letters(8)}`;
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

async function openReview(page: Page, seeded: SeededPathologyCase) {
  await openCase(page, seeded);
  await expandSection(page, REVIEW_SECTION);
}

/** The progress rail, which names what Review still waits on. */
function rail(page: Page) {
  return page.locator('nav[aria-label="Case progress"]');
}

const AWAITED = text("pathology.badge.secondOpinionAwaited");
const SECOND_OPINION = text("pathology.label.secondOpinion");

/** The rail, the Review header and the summary all say one is awaited. */
async function expectAwaitedFrom(page: Page, who: SecondPathologist) {
  await expect(rail(page)).toContainText(`${AWAITED}:`);
  await expect(rail(page)).toContainText(who.username);
  await expect(sectionHeader(page, REVIEW_SECTION)).toContainText(AWAITED);
  await expect(summaryRow(page, SECOND_OPINION)).toContainText(
    text("pathology.consultationStatus.REQUESTED"),
  );
}

test.describe.serial("Pathology second opinion", () => {
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
      tissue = await ensureTissueType(page);
      colleague = await createPathologistUser(page);
      longColleague = await createPathologistUser(page, {
        firstName: LONG_FIRST_NAME,
      });
      readerUsername = await createUserWithoutPathologistRole(page);
      reason = await createConsultationReason(
        page,
        `Difficult differential diagnosis ${RUN}`,
      );
      shared = await seedQueuedCase(page);
      await moveCaseToStage(page, shared, "UNDER_REVIEW");
    });
  });

  test("A requests a second opinion from B with a reason, before treatment; the row, the rail and the summary say it is awaited, and still do after a reload", async ({
    page,
  }) => {
    await openReview(page, shared);
    await expect(
      page.getByText(text("pathology.empty.noConsultation")),
    ).toBeVisible();
    await expect(summaryRow(page, SECOND_OPINION)).toContainText(
      text("common.none"),
    );

    await requestConsultationButton(page).click();
    await expect(newConsultationRow(page)).toBeVisible();
    // Held, with the first unmet rule in words, until a colleague is named.
    await expect(askColleagueButton(page)).toBeDisabled();
    await expect(askColleagueHint(page)).toBeVisible();

    const box = consultationColleagueBox(page);
    await box.fill(colleague.username);
    await consultationColleagueOption(page, colleague.displayName).click();
    await consultationReasonSelect(page).selectOption({ label: reason.name });
    await consultationPreTreatmentLabel(page, true).click();
    await expect(askColleagueButton(page)).toBeEnabled();

    const answered = waitForConsultationChange(page, shared.pathologySampleId);
    await askColleagueButton(page).click();
    const created = (await (await answered).json()) as { id?: number };
    expect(created.id, "the request answers the new row's id").toBeTruthy();
    firstOpinionId = created.id!;

    const row = consultationRow(page, firstOpinionId);
    await expect(newConsultationRow(page)).toHaveCount(0);
    await expect(row).toContainText(colleague.username);
    await expect(row).toContainText(reason.name);
    await expect(row).toContainText(
      `${text("pathology.label.preTreatment")} ${text("label.yes")}`,
    );
    await expect(row).toContainText(
      text("pathology.consultationStatus.REQUESTED"),
    );
    await expectAwaitedFrom(page, colleague);

    await openReview(page, shared);
    await expect(consultationRows(page)).toHaveCount(1);
    await expect(consultationRow(page, firstOpinionId)).toContainText(
      text("pathology.consultationStatus.REQUESTED"),
    );
    await expectAwaitedFrom(page, colleague);
  });

  test("B signs in, opens the case and records Disagree with a note; A then sees the outcome, the note and its time, and nothing is awaited", async ({
    browser,
    baseURL,
    page,
  }) => {
    const second = await signInAsUser(browser, baseURL!, colleague.username);
    try {
      const b = second.page;
      await openReview(b, shared);
      await expect(
        withdrawConsultationButton(b, firstOpinionId),
        "the colleague consulted is not offered Withdraw",
      ).toHaveCount(0);
      await recordOutcomeButton(b, firstOpinionId).click();
      // Held until an outcome is chosen.
      await expect(
        consultationAnswerConfirmButton(b, firstOpinionId),
      ).toBeDisabled();
      await expect(consultationAnswerHint(b, firstOpinionId)).toBeVisible();
      await consultationOutcomeLabel(b, firstOpinionId, "DISAGREE").click();
      await consultationNote(b, firstOpinionId).fill(DISAGREE_NOTE);
      await expect(
        consultationAnswerConfirmButton(b, firstOpinionId),
      ).toBeEnabled();
      const change = waitForConsultationChange(b, shared.pathologySampleId, {
        consultationId: firstOpinionId,
        action: "answer",
      });
      await consultationAnswerConfirmButton(b, firstOpinionId).click();
      await change;
      await expect(consultationRow(b, firstOpinionId)).toContainText(
        text("pathology.consultationOutcome.DISAGREE"),
      );
      await expect(recordOutcomeButton(b, firstOpinionId)).toHaveCount(0);
    } finally {
      await second.context.close();
    }

    await openReview(page, shared);
    const row = consultationRow(page, firstOpinionId);
    await expect(row).toContainText(
      text("pathology.consultationOutcome.DISAGREE"),
    );
    await expect(row).toContainText(DISAGREE_NOTE);
    // The time the outcome was recorded, with its hour.
    await expect(row).toContainText(/\d{1,2}:\d{2}/);
    await expect(rail(page)).not.toContainText(AWAITED);
    await expect(sectionHeader(page, REVIEW_SECTION)).not.toContainText(
      AWAITED,
    );
    await expect(summaryRow(page, SECOND_OPINION)).toContainText(
      text("pathology.consultationOutcome.DISAGREE"),
    );
  });

  test("A is offered no Record outcome on B's row; A asks again, withdraws, and the row reads Withdrawn", async ({
    page,
  }) => {
    await onApplication(page);
    const again = await requestConsultation(page, shared, {
      consultedUserId: colleague.userId,
      reasonId: reason.id,
      preTreatment: false,
    });

    await openReview(page, shared);
    await expect(consultationRow(page, again)).toContainText(
      text("pathology.consultationStatus.REQUESTED"),
    );
    await expect(
      recordOutcomeButton(page, again),
      "the asker is never offered Record the outcome",
    ).toHaveCount(0);
    // The answered row offers nothing to anyone.
    await expect(recordOutcomeButton(page, firstOpinionId)).toHaveCount(0);
    await expect(withdrawConsultationButton(page, firstOpinionId)).toHaveCount(
      0,
    );

    await withdrawConsultationButton(page, again).click();
    // The question names the colleague before anything is sent.
    await expect(consultationWithdrawRow(page, again)).toContainText(
      colleague.username,
    );
    const change = waitForConsultationChange(page, shared.pathologySampleId, {
      consultationId: again,
      action: "withdraw",
    });
    await consultationWithdrawConfirmButton(page, again).click();
    await change;

    const row = consultationRow(page, again);
    await expect(row).toContainText(
      text("pathology.consultationStatus.WITHDRAWN"),
    );
    await expect(withdrawConsultationButton(page, again)).toHaveCount(0);
    await expect(rail(page)).not.toContainText(AWAITED);
    await expect(requestConsultationButton(page)).toBeVisible();
  });

  test("a case is signed out with a second opinion awaited, and B records the outcome afterwards", async ({
    browser,
    baseURL,
    page,
  }) => {
    await onApplication(page);
    const own = await seedQueuedCase(page);
    await moveCaseToStage(page, own, "UNDER_REVIEW");
    const awaited = await requestConsultation(page, own, {
      consultedUserId: colleague.userId,
      reasonId: null,
      preTreatment: true,
    });
    await signOutCase(page, own);

    await openReview(page, own);
    await expectStage(page, STAGE_LABELS.COMPLETED);
    await expect(
      requestConsultationButton(page),
      "nobody is offered Request second opinion on a signed-out case",
    ).toHaveCount(0);
    await expect(consultationRow(page, awaited)).toContainText(
      text("pathology.consultationStatus.REQUESTED"),
    );

    const second = await signInAsUser(browser, baseURL!, colleague.username);
    try {
      const b = second.page;
      await openReview(b, own);
      await recordOutcomeButton(b, awaited).click();
      await consultationOutcomeLabel(b, awaited, "AGREE").click();
      const change = waitForConsultationChange(b, own.pathologySampleId, {
        consultationId: awaited,
        action: "answer",
      });
      await consultationAnswerConfirmButton(b, awaited).click();
      await change;
      await expect(consultationRow(b, awaited)).toContainText(
        text("pathology.consultationOutcome.AGREE"),
      );
    } finally {
      await second.context.close();
    }

    await openReview(page, own);
    await expect(consultationRow(page, awaited)).toContainText(
      text("pathology.consultationOutcome.AGREE"),
    );
    await expect(summaryRow(page, SECOND_OPINION)).toContainText(
      text("pathology.consultationOutcome.AGREE"),
    );
  });

  test("the user without the Pathologist role sees the rows and no action", async ({
    browser,
    baseURL,
    page,
  }) => {
    // One awaited row as well as the answered and the withdrawn ones, so the
    // reader is shown a row anyone else could act on.
    await onApplication(page);
    const awaited = await requestConsultation(page, shared, {
      consultedUserId: colleague.userId,
      reasonId: reason.id,
      preTreatment: false,
    });

    const reader = await signInAsUser(browser, baseURL!, readerUsername);
    try {
      const r = reader.page;
      await openReview(r, shared);
      await expect(consultationRows(r)).toHaveCount(3);
      await expect(consultationRow(r, awaited)).toContainText(
        text("pathology.consultationStatus.REQUESTED"),
      );
      await expect(consultationRow(r, firstOpinionId)).toContainText(
        text("pathology.consultationOutcome.DISAGREE"),
      );
      await expect(requestConsultationButton(r)).toHaveCount(0);
      await expect(
        r.locator(
          `#${REVIEW_SECTION} [id^="recordOutcome-"], #${REVIEW_SECTION} [id^="withdrawConsultation-"]`,
        ),
      ).toHaveCount(0);
      // No row has an action, so the table has no Actions column.
      await expect(
        consultationsRegion(r).getByRole("columnheader", {
          name: text("common.actions"),
          exact: true,
        }),
      ).toHaveCount(0);
    } finally {
      await reader.context.close();
    }

    // Withdrawn again, so the layout test starts from rows only answered.
    await openReview(page, shared);
    await withdrawConsultationButton(page, awaited).click();
    const change = waitForConsultationChange(page, shared.pathologySampleId, {
      consultationId: awaited,
      action: "withdraw",
    });
    await consultationWithdrawConfirmButton(page, awaited).click();
    await change;
    await expect(consultationRow(page, awaited)).toContainText(
      text("pathology.consultationStatus.WITHDRAWN"),
    );
  });

  test("no sideways scroll at 1480, 1366 and 1280 with the panel and the request form open, a long colleague name and a 1,000-character note; the colleague menu is not clipped; axe clean on Review; screenshots at 1480 and 1280", async ({
    browser,
    baseURL,
    page,
  }, testInfo) => {
    // The long-named colleague answers with a note at its full length.
    await onApplication(page);
    const long = await requestConsultation(page, shared, {
      consultedUserId: longColleague.userId,
      reasonId: reason.id,
      preTreatment: true,
    });
    const second = await signInAsUser(
      browser,
      baseURL!,
      longColleague.username,
    );
    try {
      // Through the screen: a post straight after the sign-in is answered
      // with the login page, while the screen's own calls are not.
      const b = second.page;
      await openReview(b, shared);
      await recordOutcomeButton(b, long).click();
      await consultationOutcomeLabel(b, long, "MODIFIED").click();
      await consultationNote(b, long).fill(LONG_NOTE);
      await expect(consultationNote(b, long)).toHaveValue(LONG_NOTE);
      const change = waitForConsultationChange(b, shared.pathologySampleId, {
        consultationId: long,
        action: "answer",
      });
      await consultationAnswerConfirmButton(b, long).click();
      await change;
      await expect(consultationRow(b, long)).toContainText(
        text("pathology.consultationOutcome.MODIFIED"),
      );
    } finally {
      await second.context.close();
    }
    // One row still awaited, which its asker can withdraw, so the widths are
    // measured with the Actions column there: the table at its widest.
    const awaited = await requestConsultation(page, shared, {
      consultedUserId: colleague.userId,
      reasonId: reason.id,
      preTreatment: false,
    });

    await page.setViewportSize({ width: 1480, height: 900 });
    await openReview(page, shared);
    await expect(consultationRow(page, long)).toContainText(LONG_NOTE);
    await expect(
      consultationsRegion(page).getByRole("columnheader", {
        name: text("common.actions"),
        exact: true,
      }),
    ).toBeVisible();
    await requestConsultationButton(page).click();
    const box = consultationColleagueBox(page);
    await box.fill(longColleague.username);
    await consultationColleagueOption(page, longColleague.displayName).click();
    await expect(box).toHaveValue(longColleague.displayName);

    for (const width of DESKTOP_WIDTHS) {
      await page.setViewportSize({ width, height: 900 });
      await expect(newConsultationRow(page)).toBeVisible();
      await expect(withdrawConsultationButton(page, awaited)).toBeVisible();
      const measured = await page.evaluate(() => {
        const root = document.documentElement;
        const columns = Array.from(
          document.querySelectorAll("#consultationsRegion thead th"),
        ).map((cell) => Math.round(cell.getBoundingClientRect().width));
        return {
          pageScrollWidth: root.scrollWidth,
          pageClientWidth: root.clientWidth,
          columns: columns.join(", "),
        };
      });
      const line = `second opinion at ${width}: page ${measured.pageScrollWidth}/${measured.pageClientWidth} (scrollWidth/clientWidth), columns ${measured.columns}`;
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
            `../../../../e2e-evidence/ogc-264-s14-second-opinion-${width}.png`,
            import.meta.url,
          ).pathname,
          fullPage: true,
        });
      }
    }

    // At the narrowest width the colleague menu's long item is inside the
    // viewport and is what the page shows at that point, so neither the
    // table's region nor the accordion cuts it off.
    await box.fill("");
    await box.fill(longColleague.username);
    const option = consultationColleagueOption(page, longColleague.displayName);
    await expect(option).toBeVisible({ timeout: UI_TIMEOUT });
    await option.scrollIntoViewIfNeeded();
    const viewport = page.viewportSize()!;
    const bounds = (await option.boundingBox())!;
    expect(bounds.x).toBeGreaterThanOrEqual(0);
    expect(bounds.y).toBeGreaterThanOrEqual(0);
    // One pixel covers the rounding the scroll into view leaves.
    expect(bounds.x + bounds.width).toBeLessThanOrEqual(viewport.width + 1);
    expect(bounds.y + bounds.height).toBeLessThanOrEqual(viewport.height + 1);
    const shown = await option.evaluate((element) => {
      const rect = element.getBoundingClientRect();
      const hit = document.elementFromPoint(
        rect.left + rect.width / 2,
        rect.top + rect.height / 2,
      );
      return Boolean(hit) && (hit === element || element.contains(hit));
    });
    expect(shown, "the colleague menu is cut off").toBe(true);
    await option.click();

    await page.setViewportSize({ width: 1480, height: 900 });
    await expect(newConsultationRow(page)).toBeVisible({
      timeout: NAV_TIMEOUT,
    });
    // The shared header's notification bell is named "Notifications" while
    // its visible text is the unread count, which axe reports on every page
    // of the application. It is the layout's, not this section's.
    const scan = await new AxeBuilder({ page })
      .withTags(WCAG_21_AA_TAGS)
      .include(`#${REVIEW_SECTION}`)
      .exclude("#notification-Icon")
      .analyze();
    const jsonPath = testInfo.outputPath("axe-pathology-second-opinion.json");
    await writeFile(jsonPath, `${JSON.stringify(scan.violations, null, 2)}\n`);
    await testInfo.attach("axe-pathology-second-opinion", {
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
});
