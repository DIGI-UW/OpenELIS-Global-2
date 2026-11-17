import AxeBuilder from "@axe-core/playwright";
import { writeFile } from "fs/promises";
import type { Browser, Page } from "@playwright/test";
import { test, expect } from "../../../helpers/test-base";
import {
  apiGet,
  changeRequestStatus,
  createPathologyCase,
  discoverPathologyOrderTarget,
  ensureStain,
  ensureTissueType,
  moveCaseToStage,
  raiseRequest,
  readCase,
  saveCaseThroughApi,
  type CaseRequest,
  type SeededPathologyCase,
  type Stain,
  type TissueType,
} from "../../../helpers/seed-pathology-data";
import {
  REVIEW_SECTION,
  STAGE_LABELS,
  chooseRequestKind,
  expandSection,
  expectStage,
  expectStageSwitchesOn,
  newRequestButton,
  newRequestRow,
  openCase,
  pickerOptions,
  primaryAction,
  raiseRequestButton,
  requestAction,
  requestInstruction,
  requestKindRadio,
  requestRow,
  requestRows,
  requestStainPicker,
  requestStatusTag,
  requestTargetPicker,
  requestsRegion,
  returnCase,
  sectionHeader,
  sendAnywayButton,
  sendAnywayConfirmation,
  toast,
  waitForRequestChange,
  type RequestKindId,
} from "../../../helpers/pathology-case-view";
import { plural, text } from "../../../helpers/app-messages";
import { chooseCarbonOption } from "../../../helpers/carbon-select";
import { UI_TIMEOUT } from "../../../helpers/timeouts";

/**
 * Bench requests (FR-10): the pathologist asks the bench for another block,
 * slide or stain from a table in the Review section, the bench works the
 * request and marks it completed, naming what fulfilled it, or cancelled
 * with a reason, and a case sent back to the pathologist with requests still
 * open asks first, listing each one.
 *
 * What only a deployed stack proves here:
 *   - each kind is raised from the screen, the target list offers blocks for
 *     a slide request and slides for a stain request, and only a stain
 *     request asks for a stain,
 *   - a completed request names the slide that fulfilled it, a cancelled one
 *     its reason, and either reopens,
 *   - Request stain and Request slide in Microtomy open the Review section
 *     with the request filled in,
 *   - a request changed in one tab is refused in a tab still showing it as
 *     it was, with the conflict message,
 *   - a case returned to the bench shows its requests for the bench to work,
 *     with no New request, and
 *   - sending it to the pathologist again lists what is still open.
 *
 * Serial, on one seeded case, because each test reads what the earlier ones
 * recorded. The signed-in user must hold the Pathologist role, since only
 * the pathologist's open review raises a request; the spec checks that, and
 * the stage switches it walks through, first.
 */

const API_PREFIX = "/api/OpenELIS-Global";

const MICROTOMY_SECTION = "pathology-section-microtomy";

const WCAG_21_AA_TAGS = ["wcag2a", "wcag2aa", "wcag21a", "wcag21aa"];

/** The widths the requests table must fit at without scrolling sideways. */
const DESKTOP_WIDTHS = [1480, 1366, 1280];

/** The widths the evidence pictures are taken at. */
const SCREENSHOT_WIDTHS = [1480, 1280];

/** One request of each kind, by the instruction it is raised with. */
const BLOCK_INSTRUCTION = "Submit the remaining fat as one more block";
const SLIDE_INSTRUCTION = "Recut A1 at 3 microns, two levels";
const STAIN_INSTRUCTION = "PAS on the first slide of A1";
const TWO_TABS_INSTRUCTION = "Deeper level on A2 for the margin";
const CANCELLED_INSTRUCTION = "Reticulin on A2.1";

const CANCEL_REASON = "Asked for in error, the H&E is enough";
const RETURN_REASON = "Recut A1 before sign-out";

let seeded: SeededPathologyCase;
let tissue: TissueType;
let stain: Stain;
/** The ids of the blocks and slides, by the name each is printed with. */
const rowIds: Record<string, string> = {};
/** The signed-in user's last name, which the Raised cell shows. */
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

/**
 * A case with the pathologist, holding two blocks (A1, A2, each of `tissue`)
 * and one slide under each (A1.1, A2.1), stained and passed. Grossing,
 * cutting and the bench marks are not what this spec is about, so they are
 * done through the case save and the move endpoints. Returns the case and
 * the ids of its blocks and slides by name.
 */
async function seedCaseWithPathologist(page: Page, tissueType: TissueType) {
  const seededCase = await createPathologyCase(
    page,
    await discoverPathologyOrderTarget(page),
  );
  const caseId = seededCase.pathologySampleId;

  await moveCaseToStage(page, seededCase, "GROSSING");
  await saveCaseThroughApi(page, caseId, await readCase(page, caseId), {
    grossExam: "Breast, wide local excision, 45 g, with a 2 cm firm mass.",
    addCassette: true,
    cassetteTissueTypeId: tissueType.id,
  });
  await saveCaseThroughApi(page, caseId, await readCase(page, caseId), {
    addCassette: true,
    cassetteTissueTypeId: tissueType.id,
  });
  await moveCaseToStage(page, seededCase, "MICROTOMY");

  const blocks = (await readCase(page, caseId)).blocks ?? [];
  const ids: Record<string, string> = {};
  for (const designation of ["A1", "A2"]) {
    const block = blocks.find((entry) => entry.designation === designation);
    expect(block?.id, `block ${designation} was cut`).toBeDefined();
    ids[designation] = String(block!.id);
    await saveCaseThroughApi(page, caseId, await readCase(page, caseId), {
      addSlideUnder: block!.id,
    });
  }

  await moveCaseToStage(page, seededCase, "READY_PATHOLOGIST");
  const stored = await readCase(page, caseId);
  expect(stored.status).toBe("READY_PATHOLOGIST");
  for (const designation of ["A1", "A2"]) {
    const slide = (stored.slides ?? []).find(
      (entry) =>
        String(entry.blockId) === ids[designation] &&
        entry.designation === "1" &&
        entry.active !== false,
    );
    expect(slide?.id, `slide ${designation}.1 was cut`).toBeDefined();
    ids[`${designation}.1`] = String(slide!.id);
  }
  return { seededCase, ids };
}

/** A block as the target and Fulfilled by pickers name it. */
function blockOption(designation: string) {
  return text("pathology.label.blockWithTissue", {
    designation,
    tissue: tissue.name,
  });
}

/** Every field of a request the case view reads. */
const REQUEST_FIELDS: (keyof CaseRequest)[] = [
  "id",
  "kind",
  "targetBlockId",
  "targetSlideId",
  "targetName",
  "stainId",
  "stainName",
  "priority",
  "instruction",
  "status",
  "cancelReason",
  "resultBlockId",
  "resultSlideId",
  "resultName",
  "raisedAt",
  "raisedBy",
];

/**
 * The stored request raised with this instruction, every field it lacks
 * read as null, since the server leaves empty fields out of what it sends.
 * The read runs in the browser with the application's session, so a page
 * not yet on the application is taken there first.
 */
async function storedRequest(
  page: Page,
  instruction: string,
): Promise<CaseRequest> {
  if (!page.url().startsWith("http")) {
    await page.goto("/", { waitUntil: "domcontentloaded" });
  }
  const stored = await readCase(page, seeded.pathologySampleId);
  const request = (stored.requests ?? []).find(
    (entry) => entry.instruction === instruction,
  );
  if (!request) {
    throw new Error(
      `Case ${seeded.pathologySampleId} holds no request "${instruction}"`,
    );
  }
  return Object.fromEntries(
    REQUEST_FIELDS.map((field) => [field, request[field] ?? null]),
  ) as unknown as CaseRequest;
}

/** The Request cell of a request's row: its kind, target, stain and priority. */
function requestCell(page: Page, instruction: string) {
  return requestRow(page, instruction).locator(
    "td.pathology-case-view__request-col--request",
  );
}

/** Open the case with the Review section expanded. */
async function openReview(page: Page) {
  await openCase(page, seeded);
  await expandSection(page, REVIEW_SECTION);
}

/** Open the new request's row from the toolbar. */
async function openNewRequest(page: Page) {
  await expect(newRequestRow(page)).toHaveCount(0);
  await expect(newRequestButton(page)).toBeEnabled();
  await newRequestButton(page).click();
  await expect(newRequestRow(page)).toBeVisible();
}

/**
 * Press Raise request on a filled row and wait for the screen to finish with
 * it: the raise is answered, its toast shows, the row closes and the new
 * request is listed.
 */
async function pressRaise(page: Page, instruction: string) {
  await expect(raiseRequestButton(page)).toBeEnabled();
  const answer = waitForRequestChange(page, seeded.pathologySampleId);
  await raiseRequestButton(page).click();
  await answer;
  await expect(
    toast(page, text("pathology.toast.requestRaised")),
  ).not.toHaveCount(0, { timeout: UI_TIMEOUT });
  await expect(newRequestRow(page)).toHaveCount(0);
  await expect(requestRow(page, instruction)).toHaveCount(1);
}

/** The Raise request hint, which names the first rule the row does not meet. */
async function expectRaiseHeld(page: Page, hintKey: string) {
  await expect(raiseRequestButton(page)).toBeDisabled();
  await expect(raiseRequestButton(page)).toHaveAttribute(
    "title",
    text(hintKey),
  );
  await expect(page.locator("#raiseRequestHint")).toHaveText(text(hintKey));
}

/**
 * Press one of a request's row actions, or an inline row's Confirm, by its
 * accessible name, and wait until the change is answered and its toast
 * shows.
 */
async function changeFromRow(
  page: Page,
  requestId: number,
  buttonName: string,
  toastKey: string,
) {
  const button = requestAction(page, buttonName);
  await expect(button).toBeEnabled();
  const answer = waitForRequestChange(
    page,
    seeded.pathologySampleId,
    requestId,
  );
  await button.click();
  await answer;
  await expect(toast(page, text(toastKey))).not.toHaveCount(0, {
    timeout: UI_TIMEOUT,
  });
}

/** The name Confirm on a request's inline row is read out with. */
function confirmName(instruction: string) {
  return `${text("common.confirm")} ${instruction}`;
}

test.describe.serial("Pathology bench requests", () => {
  // Order entry and the case view are both heavy screens, and a first visit
  // on a development server compiles them as it serves them.
  test.setTimeout(120_000);

  test.beforeAll(async ({ browser }) => {
    test.setTimeout(240_000);
    await withOwnPage(browser, async (page) => {
      await expectStageSwitchesOn(page, [
        "EMBEDDING",
        "MICROTOMY",
        "STAINING",
        "COVERSLIPPING",
      ]);
      const session = await apiGet<{
        lastName?: string;
        roles?: string[];
      }>(page, `${API_PREFIX}/session`);
      if (Array.isArray(session.roles)) {
        expect(
          session.roles,
          "the signed-in user raises requests, which only the pathologist does",
        ).toContain("Pathologist");
      }
      lastName = session.lastName ?? "";

      tissue = await ensureTissueType(page);
      stain = await ensureStain(page);
      const { seededCase, ids } = await seedCaseWithPathologist(page, tissue);
      seeded = seededCase;
      Object.assign(rowIds, ids);
    });
  });

  test("a block request is raised from the empty table with no target and no stain", async ({
    page,
  }) => {
    await openReview(page);
    await expect(
      page
        .locator(`#${REVIEW_SECTION} .cds--inline-notification`)
        .filter({ hasText: text("pathology.empty.noRequests") }),
    ).toHaveCount(1);

    await openNewRequest(page);
    // A slide request is what is asked for most, so the row opens on it.
    await expect(requestKindRadio(page, "SLIDE")).toBeChecked();
    await chooseRequestKind(page, "BLOCK");
    await expect(requestTargetPicker(page)).toHaveCount(0);
    await expect(requestStainPicker(page)).toHaveCount(0);
    await expectRaiseHeld(page, "pathology.locked.requestInstructionRequired");

    await requestInstruction(page).fill(BLOCK_INSTRUCTION);
    await pressRaise(page, BLOCK_INSTRUCTION);

    const row = requestRow(page, BLOCK_INSTRUCTION);
    const what = requestCell(page, BLOCK_INSTRUCTION);
    await expect(what).toContainText(text("pathology.requestKind.block"));
    await expect(what).toContainText(text("common.normal"));
    await expect(requestStatusTag(page, BLOCK_INSTRUCTION)).toHaveText(
      text("pathology.requestStatus.opened"),
    );
    await expect(
      row.locator("td.pathology-case-view__request-col--raised"),
    ).toContainText(lastName);

    const stored = await storedRequest(page, BLOCK_INSTRUCTION);
    expect(stored).toMatchObject({
      kind: "BLOCK",
      priority: "NORMAL",
      status: "OPENED",
      targetBlockId: null,
      targetSlideId: null,
      stainId: null,
    });
  });

  test("a slide request offers the blocks in use as its target and asks for no stain", async ({
    page,
  }) => {
    await openReview(page);
    await openNewRequest(page);
    await chooseRequestKind(page, "SLIDE");
    await expect(requestStainPicker(page)).toHaveCount(0);

    expect(await pickerOptions(requestTargetPicker(page))).toEqual([
      blockOption("A1"),
      blockOption("A2"),
    ]);

    await requestInstruction(page).fill(SLIDE_INSTRUCTION);
    await expectRaiseHeld(page, "pathology.locked.requestTargetRequired");
    await chooseCarbonOption(requestTargetPicker(page), blockOption("A1"));
    await page.locator("#requestPriority").selectOption("URGENT");
    await expect(page.locator("#requestPriority")).toHaveValue("URGENT");
    await pressRaise(page, SLIDE_INSTRUCTION);

    const what = requestCell(page, SLIDE_INSTRUCTION);
    await expect(what).toContainText(text("pathology.requestKind.slide"));
    await expect(what).toContainText("A1");
    await expect(what).toContainText(text("pathology.priority.urgent"));
    // Newest first.
    await expect(
      requestRows(page)
        .first()
        .locator("td.pathology-case-view__request-col--instruction"),
    ).toHaveText(SLIDE_INSTRUCTION);

    const stored = await storedRequest(page, SLIDE_INSTRUCTION);
    expect(stored).toMatchObject({
      kind: "SLIDE",
      priority: "URGENT",
      targetBlockId: Number(rowIds.A1),
      targetName: "A1",
      stainId: null,
    });
  });

  test("a stain request offers the slides in use as its target, and only it asks for a stain", async ({
    page,
  }) => {
    await openReview(page);
    await openNewRequest(page);
    await chooseRequestKind(page, "STAIN");
    await expect(requestStainPicker(page)).toBeVisible();

    expect(await pickerOptions(requestTargetPicker(page))).toEqual([
      "A1.1",
      "A2.1",
    ]);

    await requestInstruction(page).fill(STAIN_INSTRUCTION);
    await expectRaiseHeld(page, "pathology.locked.requestTargetRequired");
    await chooseCarbonOption(requestTargetPicker(page), "A1.1");
    await expectRaiseHeld(page, "pathology.locked.requestStainRequired");
    await chooseCarbonOption(requestStainPicker(page), stain.name);

    await test.step("switching the kind drops the target and the stain", async () => {
      for (const kind of ["SLIDE", "BLOCK"] as RequestKindId[]) {
        await chooseRequestKind(page, kind);
        await expect(requestStainPicker(page)).toHaveCount(0);
      }
      await chooseRequestKind(page, "STAIN");
      await expect(requestTargetPicker(page)).toHaveValue("");
      await expect(requestStainPicker(page)).toHaveValue("");
      await chooseCarbonOption(requestTargetPicker(page), "A1.1");
      await chooseCarbonOption(requestStainPicker(page), stain.name);
    });

    await pressRaise(page, STAIN_INSTRUCTION);
    const what = requestCell(page, STAIN_INSTRUCTION);
    await expect(what).toContainText(text("pathology.requestKind.stain"));
    await expect(what).toContainText("A1.1");
    await expect(what).toContainText(stain.name);

    const stored = await storedRequest(page, STAIN_INSTRUCTION);
    expect(stored).toMatchObject({
      kind: "STAIN",
      targetSlideId: Number(rowIds["A1.1"]),
      targetName: "A1.1",
      stainId: stain.id,
      stainName: stain.name,
    });
  });

  test("a stain request is completed naming the slide that fulfilled it", async ({
    page,
  }) => {
    const request = await storedRequest(page, STAIN_INSTRUCTION);
    await openReview(page);

    await requestAction(
      page,
      text("pathology.action.completeRequestOf", { name: STAIN_INSTRUCTION }),
    ).click();
    const inline = page.locator(
      `[data-testid="requestComplete-${request.id}"]`,
    );
    await expect(inline).toBeVisible();
    const fulfilledBy = page.locator(`input#requestFulfilledBy-${request.id}`);
    await expect(fulfilledBy).toBeFocused();
    // A stain is fulfilled on a slide, so slides are what it offers.
    expect(await pickerOptions(fulfilledBy)).toEqual(["A1.1", "A2.1"]);
    await chooseCarbonOption(fulfilledBy, "A1.1");

    await changeFromRow(
      page,
      request.id,
      confirmName(STAIN_INSTRUCTION),
      "pathology.toast.requestCompleted",
    );

    await expect(inline).toHaveCount(0);
    await expect(requestStatusTag(page, STAIN_INSTRUCTION)).toHaveText(
      text("common.completed"),
    );
    await expect(requestRow(page, STAIN_INSTRUCTION)).toContainText(
      text("pathology.label.fulfilledByName", { name: "A1.1" }),
    );
    await expect(
      requestAction(
        page,
        text("pathology.action.reopenRequestOf", { name: STAIN_INSTRUCTION }),
      ),
    ).toBeEnabled();

    expect(await storedRequest(page, STAIN_INSTRUCTION)).toMatchObject({
      status: "COMPLETED",
      resultSlideId: Number(rowIds["A1.1"]),
      resultName: "A1.1",
    });
  });

  test("a block request is cancelled with a reason", async ({ page }) => {
    const request = await storedRequest(page, BLOCK_INSTRUCTION);
    await openReview(page);

    await requestAction(
      page,
      text("pathology.action.cancelRequestOf", { name: BLOCK_INSTRUCTION }),
    ).click();
    const inline = page.locator(`[data-testid="requestCancel-${request.id}"]`);
    await expect(inline).toBeVisible();
    const reason = page.locator(`#requestCancelReason-${request.id}`);
    await expect(reason).toBeFocused();

    const confirm = requestAction(page, confirmName(BLOCK_INSTRUCTION));
    await expect(confirm).toBeDisabled();
    await expect(confirm).toHaveAttribute(
      "title",
      text("pathology.locked.requestCancelReasonRequired"),
    );

    await reason.fill(CANCEL_REASON);
    await changeFromRow(
      page,
      request.id,
      confirmName(BLOCK_INSTRUCTION),
      "pathology.toast.requestCancelled",
    );

    await expect(inline).toHaveCount(0);
    await expect(requestStatusTag(page, BLOCK_INSTRUCTION)).toHaveText(
      text("pathology.requestStatus.cancelled"),
    );
    await expect(requestRow(page, BLOCK_INSTRUCTION)).toContainText(
      CANCEL_REASON,
    );
    expect(await storedRequest(page, BLOCK_INSTRUCTION)).toMatchObject({
      status: "CANCELLED",
      cancelReason: CANCEL_REASON,
    });
  });

  test("a cancelled request reopens, losing its reason", async ({ page }) => {
    const request = await storedRequest(page, BLOCK_INSTRUCTION);
    await openReview(page);

    await changeFromRow(
      page,
      request.id,
      text("pathology.action.reopenRequestOf", { name: BLOCK_INSTRUCTION }),
      "pathology.toast.requestReopened",
    );

    await expect(requestStatusTag(page, BLOCK_INSTRUCTION)).toHaveText(
      text("pathology.requestStatus.opened"),
    );
    await expect(requestRow(page, BLOCK_INSTRUCTION)).not.toContainText(
      CANCEL_REASON,
    );
    // After a change focus goes back to New request.
    await expect(newRequestButton(page)).toBeFocused();
    expect(await storedRequest(page, BLOCK_INSTRUCTION)).toMatchObject({
      status: "OPENED",
      cancelReason: null,
    });
  });

  test("Request stain and Request slide in Microtomy open the review with the request filled in", async ({
    page,
  }) => {
    await openCase(page, seeded);
    const review = sectionHeader(page, REVIEW_SECTION);
    await expect(review).toBeEnabled();
    if ((await review.getAttribute("aria-expanded")) === "true") {
      await review.click();
    }
    await expect(review).toHaveAttribute("aria-expanded", "false");
    await expandSection(page, MICROTOMY_SECTION);

    await test.step("Request stain on a slide", async () => {
      await page
        .getByRole("button", {
          name: text("pathology.action.requestStainOf", { name: "A2.1" }),
          exact: true,
        })
        .click();
      await expect(review).toHaveAttribute("aria-expanded", "true");
      await expect(newRequestRow(page)).toBeVisible();
      await expect(requestKindRadio(page, "STAIN")).toBeChecked();
      await expect(requestTargetPicker(page)).toHaveValue("A2.1");
      await expect(requestStainPicker(page)).toHaveValue("");
      await expect(requestInstruction(page)).toBeFocused();
    });

    await test.step("Request slide on a block, while the row is open", async () => {
      await page
        .getByRole("button", {
          name: text("pathology.action.requestSlideOf", { name: "A2" }),
          exact: true,
        })
        .click();
      await expect(requestKindRadio(page, "SLIDE")).toBeChecked();
      await expect(requestTargetPicker(page)).toHaveValue(blockOption("A2"));
      await expect(requestStainPicker(page)).toHaveCount(0);
      await expect(requestInstruction(page)).toBeFocused();
    });

    await test.step("Cancel closes the row and raises nothing", async () => {
      await page.locator("#raiseRequestCancel").click();
      await expect(newRequestRow(page)).toHaveCount(0);
      await expect(newRequestButton(page)).toBeFocused();
      await expect(requestRows(page)).toHaveCount(3);
    });
  });

  test("a request changed in one tab is refused in a tab still showing it, with the conflict message", async ({
    page,
  }) => {
    await openCase(page, seeded);
    const requestId = await raiseRequest(page, seeded, {
      kind: "BLOCK",
      instruction: TWO_TABS_INSTRUCTION,
    });
    const stale = await page.context().newPage();
    try {
      await openReview(page);
      await openReview(stale);
      await expect(requestStatusTag(stale, TWO_TABS_INSTRUCTION)).toHaveText(
        text("pathology.requestStatus.opened"),
      );

      await test.step("completed in the first tab", async () => {
        await requestAction(
          page,
          text("pathology.action.completeRequestOf", {
            name: TWO_TABS_INSTRUCTION,
          }),
        ).click();
        await changeFromRow(
          page,
          requestId,
          confirmName(TWO_TABS_INSTRUCTION),
          "pathology.toast.requestCompleted",
        );
        await expect(requestStatusTag(page, TWO_TABS_INSTRUCTION)).toHaveText(
          text("common.completed"),
        );
      });

      await test.step("cancelled in the second, which still shows it open", async () => {
        await requestAction(
          stale,
          text("pathology.action.cancelRequestOf", {
            name: TWO_TABS_INSTRUCTION,
          }),
        ).click();
        await stale
          .locator(`#requestCancelReason-${requestId}`)
          .fill(CANCEL_REASON);
        const confirm = requestAction(stale, confirmName(TWO_TABS_INSTRUCTION));
        await expect(confirm).toBeEnabled();
        const answer = waitForRequestChange(
          stale,
          seeded.pathologySampleId,
          requestId,
        );
        await confirm.click();
        await answer;
        await expect(
          stale
            .locator(".cds--toast-notification--error")
            .filter({ hasText: text("pathology.error.requestConflict") }),
        ).not.toHaveCount(0, { timeout: UI_TIMEOUT });
        // The case is read again, so the stale tab now shows what happened.
        await expect(requestStatusTag(stale, TWO_TABS_INSTRUCTION)).toHaveText(
          text("common.completed"),
        );
      });

      expect(await storedRequest(page, TWO_TABS_INSTRUCTION)).toMatchObject({
        status: "COMPLETED",
        cancelReason: null,
      });
    } finally {
      await stale.close();
    }
  });

  test("the Review section with its requests passes an accessibility scan", async ({
    page,
  }, testInfo) => {
    await page.setViewportSize({ width: 1480, height: 900 });
    await openReview(page);
    // The new request open on a stain, so every field it can show is scanned.
    await openNewRequest(page);
    await chooseRequestKind(page, "STAIN");
    await expect(requestStainPicker(page)).toBeVisible();

    // The shared header's notification bell is named "Notifications" while
    // its visible text is the unread count, which axe reports on every page
    // of the application. It is the layout's, not this section's.
    const scan = await new AxeBuilder({ page })
      .withTags(WCAG_21_AA_TAGS)
      .include(`#${REVIEW_SECTION}`)
      .exclude("#notification-Icon")
      .analyze();
    const jsonPath = testInfo.outputPath("axe-pathology-bench-requests.json");
    await writeFile(jsonPath, `${JSON.stringify(scan.violations, null, 2)}\n`);
    await testInfo.attach("axe-pathology-bench-requests", {
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

  test("open, completed and cancelled requests fit at three desktop widths", async ({
    page,
  }, testInfo) => {
    await page.setViewportSize({ width: 1480, height: 900 });
    await openCase(page, seeded);
    // A STAT stain request cancelled through the API, so the table holds
    // every status and both kinds of note under it.
    const cancelledId = await raiseRequest(page, seeded, {
      kind: "STAIN",
      targetSlideId: rowIds["A2.1"],
      stainId: stain.id,
      priority: "STAT",
      instruction: CANCELLED_INSTRUCTION,
    });
    await changeRequestStatus(page, seeded, cancelledId, {
      status: "CANCELLED",
      cancelReason: CANCEL_REASON,
    });

    await openReview(page);
    await expect(requestStatusTag(page, CANCELLED_INSTRUCTION)).toHaveText(
      text("pathology.requestStatus.cancelled"),
    );
    await expect(requestRow(page, CANCELLED_INSTRUCTION)).toContainText(
      text("pathology.priority.stat"),
    );

    for (const width of DESKTOP_WIDTHS) {
      await page.setViewportSize({ width, height: 900 });
      await expect(requestsRegion(page)).toBeVisible();
      const measured = await page.evaluate(() => {
        const region = document.getElementById("requestsRegion")!;
        const root = document.documentElement;
        return {
          regionScrollWidth: region.scrollWidth,
          regionClientWidth: region.clientWidth,
          pageScrollWidth: root.scrollWidth,
          pageClientWidth: root.clientWidth,
        };
      });
      const line = `requests table at ${width}: region ${measured.regionScrollWidth}/${measured.regionClientWidth}, page ${measured.pageScrollWidth}/${measured.pageClientWidth} (scrollWidth/clientWidth)`;
      // Printed for the walk notes, and kept on the report.
      console.log(line);
      testInfo.annotations.push({ type: "layout", description: line });
      // One pixel covers rounding.
      expect(
        measured.regionScrollWidth,
        `the requests table scrolls sideways at ${width} wide`,
      ).toBeLessThanOrEqual(measured.regionClientWidth + 1);
      expect(
        measured.pageScrollWidth,
        `the page scrolls sideways at ${width} wide`,
      ).toBeLessThanOrEqual(measured.pageClientWidth + 1);

      if (SCREENSHOT_WIDTHS.includes(width)) {
        await page.evaluate(() => window.scrollTo(0, 0));
        await page.screenshot({
          path: new URL(
            `../../../../e2e-evidence/ogc-264-s11-bench-requests-${width}.png`,
            import.meta.url,
          ).pathname,
          fullPage: true,
        });
      }
    }
  });

  test("returned to the bench, the requests are worked there and none is raised", async ({
    page,
  }) => {
    const slideRequest = await storedRequest(page, SLIDE_INSTRUCTION);
    await openCase(page, seeded);
    await returnCase(page, RETURN_REASON);
    await expectStage(page, STAGE_LABELS.COVERSLIPPING);

    await expandSection(page, REVIEW_SECTION);
    await expect(newRequestButton(page)).toHaveCount(0);
    await expect(page.locator("#assignedPathologist")).toBeDisabled();
    await expect(
      requestAction(
        page,
        text("pathology.action.cancelRequestOf", { name: BLOCK_INSTRUCTION }),
      ),
    ).toBeEnabled();

    await requestAction(
      page,
      text("pathology.action.completeRequestOf", { name: SLIDE_INSTRUCTION }),
    ).click();
    await expect(
      page.locator(`[data-testid="requestComplete-${slideRequest.id}"]`),
    ).toBeVisible();
    await changeFromRow(
      page,
      slideRequest.id,
      confirmName(SLIDE_INSTRUCTION),
      "pathology.toast.requestCompleted",
    );
    await expect(requestStatusTag(page, SLIDE_INSTRUCTION)).toHaveText(
      text("common.completed"),
    );
    expect(await storedRequest(page, SLIDE_INSTRUCTION)).toMatchObject({
      status: "COMPLETED",
      resultSlideId: null,
    });
  });

  test("sent to the pathologist again, the confirmation lists the request still open", async ({
    page,
  }) => {
    const blockRequest = await storedRequest(page, BLOCK_INSTRUCTION);
    await openCase(page, seeded);
    await expectStage(page, STAGE_LABELS.COVERSLIPPING);
    const primary = primaryAction(page);
    await expect(primary).toHaveText("Send to pathologist");

    const confirmation = sendAnywayConfirmation(page);
    await test.step("the confirmation names it, and Cancel goes back", async () => {
      await primary.click();
      await expect(confirmation).toBeVisible();
      await expect(confirmation).toContainText(
        plural("pathology.banner.openRequests", 1),
      );
      await expect(confirmation.getByRole("listitem")).toHaveText([
        BLOCK_INSTRUCTION,
      ]);
      await expect(sendAnywayButton(page)).toBeFocused();
      await expect(sendAnywayButton(page)).toHaveAccessibleName(
        text("pathology.action.sendAnywayOf", { name: BLOCK_INSTRUCTION }),
      );

      await page.locator("#sendAnywayCancel").click();
      await expect(confirmation).toHaveCount(0);
      await expect(primary).toBeFocused();
      await expectStage(page, STAGE_LABELS.COVERSLIPPING);
    });

    await test.step("Send anyway names the open request and moves the case", async () => {
      await primary.click();
      await expect(confirmation).toBeVisible();
      const moved = page.waitForResponse(
        (r) =>
          r.request().method() === "POST" &&
          new URL(r.url()).pathname.endsWith(
            `/rest/pathology/caseView/${seeded.pathologySampleId}/advance`,
          ),
        { timeout: UI_TIMEOUT },
      );
      await sendAnywayButton(page).click();
      const posted = (await moved).request().postDataJSON();
      expect(posted).toEqual({
        from: "COVERSLIPPING",
        acknowledgedRequestIds: [blockRequest.id],
      });
      await expect(
        toast(page, text("pathology.toast.sentWithOpenRequests")),
      ).not.toHaveCount(0, { timeout: UI_TIMEOUT });
      await expectStage(page, STAGE_LABELS.READY_PATHOLOGIST);
      await expect(confirmation).toHaveCount(0);
    });

    const stored = await readCase(page, seeded.pathologySampleId);
    expect(stored.status).toBe("READY_PATHOLOGIST");
    expect(
      (stored.requests ?? []).find((entry) => entry.id === blockRequest.id)
        ?.status,
      "sending the case on leaves the request open",
    ).toBe("OPENED");
  });
});
