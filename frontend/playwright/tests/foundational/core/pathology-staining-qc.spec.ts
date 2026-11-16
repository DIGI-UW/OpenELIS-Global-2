import AxeBuilder from "@axe-core/playwright";
import { readFileSync } from "fs";
import { writeFile } from "fs/promises";
import type { Browser, Locator, Page } from "@playwright/test";
import { test, expect } from "../../../helpers/test-base";
import {
  apiGet,
  createPathologyCase,
  discoverPathologyOrderTarget,
  ensureStain,
  moveCaseToStage,
  readCase,
  recordStainStatus,
  saveCaseThroughApi,
  stainSlides,
  type CaseSlide,
  type SeededPathologyCase,
  type Stain,
} from "../../../helpers/seed-pathology-data";
import {
  advanceCase,
  expandSection,
  expectStage,
  expectStageSwitchesOn,
  openCase,
  primaryAction,
  railStep,
  readStageSwitch,
  saveDraft,
  sectionHeader,
  selectSlides,
  setStageSwitch,
  slideBatchAction,
  slideBatchConfirm,
  slideWorkButton,
  slideWorkHold,
  slideWorkRow,
  slideWorkSection,
  summaryRow,
  waitForSlideWork,
  type SlideWorkStage,
} from "../../../helpers/pathology-case-view";
import { chooseCarbonOption } from "../../../helpers/carbon-select";
import { UI_TIMEOUT } from "../../../helpers/timeouts";

/**
 * At staining each slide is marked stained, one at a time or as a batch of
 * selected slides, or marked unsatisfactory for a restain (FR-8.1 to FR-8.3);
 * at coverslipping the batch is written once and each slide's coverslip
 * passes or fails QC (FR-8.4). The case is not sent on from either stage
 * until every slide in use is done.
 *
 * What only a deployed stack proves here:
 *   - the marks land in the signed-in user's name and the counts, banners
 *     and the held primary read them,
 *   - a batch confirmation names the slides it marks, never a count,
 *   - an unsatisfactory slide holds the case again and frees its stain,
 *   - the batch is recorded once and read-only after,
 *   - a failed coverslip holds the case until it passes, and
 *   - with Coverslipping switched off a stained case goes straight to the
 *     pathologist.
 *
 * Serial, on one seeded case, because each test reads what the earlier ones
 * recorded; the switch test and the staining screenshots each seed a case of
 * their own. The server lists a case's slides block by block, so every
 * sentence naming slides is checked in that order. Embedding,
 * Microtomy, Staining and Coverslipping must be tracked (the shipped
 * default), so the spec checks that first.
 */

const API_PREFIX = "/api/OpenELIS-Global";

const STAINING_SECTION = "pathology-section-staining";
const COVERSLIPPING_SECTION = "pathology-section-coverslipping";

const WCAG_21_AA_TAGS = ["wcag2a", "wcag2aa", "wcag21a", "wcag21aa"];

/**
 * The application's own English, so an expected sentence is the one the
 * screen is built from rather than a copy of it that drifts.
 */
const MESSAGES = JSON.parse(
  readFileSync(
    new URL("../../../../src/languages/en.json", import.meta.url),
    "utf8",
  ),
) as Record<string, string>;

/** One message with its plain `{name}` placeholders filled in. */
function text(key: string, values: Record<string, string | number> = {}) {
  const message = MESSAGES[key];
  if (message === undefined) {
    throw new Error(`en.json has no message ${key}`);
  }
  return Object.entries(values).reduce(
    (filled, [name, value]) => filled.split(`{${name}}`).join(String(value)),
    message,
  );
}

let seeded: SeededPathologyCase;
/** The stain every slide is cut with, and the one a restain switches to. */
let firstStain: Stain;
let secondStain: Stain;
/** The ids of the three slides, by the name each goes by under its block. */
const slideIds: Record<string, string> = {};
/** The Coverslipping switch as the deployment had it before this spec ran. */
let coverslippingBefore: string | undefined;

/**
 * A case standing at Staining with two blocks and three slides (A1.1, A1.2
 * under A1, A2.1 under A2), each slide carrying `stain` and its label
 * checked, nothing marked yet. Grossing, embedding, cutting and the label
 * checks are not what this spec is about: the cassettes and slides are cut
 * through the case save and the case is moved through the move endpoints,
 * which embed and check labels on the way. Returns the case and its slide
 * ids by name.
 */
async function seedCaseAtStaining(page: Page, stain: Stain) {
  const seededCase = await createPathologyCase(
    page,
    await discoverPathologyOrderTarget(page),
  );
  const caseId = seededCase.pathologySampleId;

  await moveCaseToStage(page, seededCase, "GROSSING");
  await saveCaseThroughApi(page, caseId, await readCase(page, caseId), {
    grossExam: "Sigmoid colon, 12 cm, with a 3 cm ulcerated mass.",
    addCassette: true,
  });
  await saveCaseThroughApi(page, caseId, await readCase(page, caseId), {
    addCassette: true,
  });
  await moveCaseToStage(page, seededCase, "MICROTOMY");

  let stored = await readCase(page, caseId);
  const blocks = stored.blocks ?? [];
  const blockA1 = String(blocks.find((b) => b.designation === "A1")?.id);
  const blockA2 = String(blocks.find((b) => b.designation === "A2")?.id);
  for (const blockId of [blockA1, blockA1, blockA2]) {
    await saveCaseThroughApi(page, caseId, await readCase(page, caseId), {
      addSlideUnder: blockId,
    });
  }
  stored = await readCase(page, caseId);
  const ids: Record<string, string> = {};
  for (const [name, blockId, designation] of [
    ["A1.1", blockA1, "1"],
    ["A1.2", blockA1, "2"],
    ["A2.1", blockA2, "1"],
  ]) {
    const slide = slideUnder(stored.slides ?? [], blockId, designation);
    expect(slide?.id, `slide ${name} was cut`).toBeDefined();
    ids[name] = String(slide!.id);
  }
  await saveCaseThroughApi(page, caseId, stored, {
    slideStains: Object.fromEntries(
      Object.values(ids).map((id) => [id, stain.id]),
    ),
  });

  await moveCaseToStage(page, seededCase, "STAINING");
  stored = await readCase(page, caseId);
  expect(stored.status).toBe("STAINING");
  expect(
    (stored.slides ?? []).map((slide) => slide.id && String(slide.id)),
    "the server lists the slides block by block",
  ).toEqual([ids["A1.1"], ids["A1.2"], ids["A2.1"]]);
  expect(
    (stored.slides ?? []).map((slide) => slide.stainStatus ?? null),
    "no slide is marked on the way",
  ).toEqual([null, null, null]);
  return { seededCase, ids };
}

/**
 * Where a Stained slide's row shows its stain: the stain's name as text,
 * with the way to change it under it, since the stain applied is frozen.
 */
function frozenStain(page: Page, stage: SlideWorkStage, slideName: string) {
  return slideWorkRow(page, stage, slideName).locator(
    ".pathology-case-view__frozen-stain-name",
  );
}

/** The stain picker a Pending or Unsatisfactory slide's row offers. */
function stainPicker(page: Page, slideName: string) {
  return slideWorkSection(page, "STAINING").getByRole("combobox", {
    name: text("pathology.label.stainFor", { slide: slideName }),
    exact: true,
  });
}

/** Who recorded a slide's status and when, in its status cell. */
function stamp(page: Page, stage: SlideWorkStage, slideName: string) {
  return slideWorkRow(page, stage, slideName).locator(
    ".pathology-case-view__slide-stamp",
  );
}

/** Where an evidence screenshot is written, beside the other slices'. */
function evidencePath(name: string) {
  return new URL(`../../../../e2e-evidence/${name}`, import.meta.url).pathname;
}

/**
 * Whether one element scrolls sideways: its content is wider than the box
 * that shows it. A tolerance of one pixel covers rounding.
 */
async function overflowsSideways(element: Locator) {
  return element.evaluate((node) => node.scrollWidth > node.clientWidth + 1);
}

/**
 * Full-page pictures of the case at 1480 and 1280 wide, for the pull
 * request, with one section open. At each width the section's table and the
 * page itself must fit without scrolling sideways. The page is resized in
 * place rather than reloaded, since a second load while the first one's
 * lookups are in flight aborts them, and taken from the top so the fixed
 * header stays at the top.
 */
async function shootAtTwoWidths(
  page: Page,
  stage: SlideWorkStage,
  fileStem: string,
) {
  for (const width of [1480, 1280]) {
    await page.setViewportSize({ width, height: 900 });
    await expect(slideWorkSection(page, stage)).toBeVisible();
    const region = page.locator(
      stage === "STAINING" ? "#stainingRegion" : "#coverslippingRegion",
    );
    await expect(region).toBeVisible();
    expect(
      await overflowsSideways(region),
      `the ${stage} table scrolls sideways at ${width} wide`,
    ).toBe(false);
    expect(
      await overflowsSideways(page.locator("html")),
      `the page scrolls sideways at ${width} wide`,
    ).toBe(false);
    await page.evaluate(() => window.scrollTo(0, 0));
    await page.screenshot({
      path: evidencePath(`${fileStem}-${width}.png`),
      fullPage: true,
    });
  }
}

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

/** Put the Coverslipping switch back as the deployment had it. */
async function restoreCoverslipping(page: Page) {
  await setStageSwitch(page, "COVERSLIPPING", coverslippingBefore ?? "true");
}

/**
 * The name the server records as the one who marked a slide: the signed-in
 * user's own, as the session reports it, in the "Last,First" form every
 * operator name on the case view takes.
 */
async function signedInName(page: Page) {
  const session = await apiGet<{ firstName?: string; lastName?: string }>(
    page,
    `${API_PREFIX}/session`,
  );
  return `${session.lastName},${session.firstName}`;
}

/** The one slide in use under a block, by the block's id and designation. */
function slideUnder(slides: CaseSlide[], blockId: string, designation: string) {
  return slides.find(
    (slide) =>
      String(slide.blockId) === blockId &&
      slide.designation === designation &&
      slide.active !== false,
  );
}

/** The status tag a slide's row shows in one column of its section. */
function statusTag(page: Page, stage: SlideWorkStage, slideName: string) {
  return slideWorkRow(page, stage, slideName).locator(".cds--tag");
}

/** The toast a mark raises, carrying the sentence it was given. */
function toast(page: Page, sentence: string | RegExp) {
  return page.locator(".cds--toast-notification").filter({ hasText: sentence });
}

/** The warning a section opens on while slides wait for it. */
function waitingBanner(page: Page, sectionId: string) {
  return page.locator(`#${sectionId} .cds--inline-notification--warning`);
}

/** The notice a section shows for slides that need doing again. */
function redoBanner(page: Page, sectionId: string) {
  return page.locator(`#${sectionId} .cds--inline-notification--info`);
}

/**
 * Press one row's action and wait for the screen to finish with it: the
 * mark is answered, its toast names the slide, and the case is read back.
 */
async function markFromRow(
  page: Page,
  stage: SlideWorkStage,
  buttonName: string,
  toastSentence: string,
) {
  const button = slideWorkButton(page, stage, buttonName);
  await expect(button).toBeEnabled();
  const answer = waitForSlideWork(page, seeded.pathologySampleId, stage);
  await button.click();
  await answer;
  await expect(toast(page, toastSentence)).not.toHaveCount(0, {
    timeout: UI_TIMEOUT,
  });
}

test.describe.serial("Pathology staining and coverslip QC", () => {
  // Order entry and the case view are both heavy screens, and a first visit
  // on a development server compiles them as it serves them.
  test.setTimeout(120_000);

  test.beforeAll(async ({ browser }) => {
    test.setTimeout(180_000);
    await withOwnPage(browser, async (page) => {
      coverslippingBefore = await readStageSwitch(page, "COVERSLIPPING");
      await expectStageSwitchesOn(page, [
        "EMBEDDING",
        "MICROTOMY",
        "STAINING",
        "COVERSLIPPING",
      ]);

      firstStain = await ensureStain(page);
      secondStain = await ensureStain(page);
      const { seededCase, ids } = await seedCaseAtStaining(page, firstStain);
      seeded = seededCase;
      Object.assign(slideIds, ids);
    });
  });

  // A test Playwright times out never reaches its own finally, and the stage
  // left switched off changes what every later pathology spec reads, so it
  // is put back from a hook as well.
  test.afterAll(async ({ browser }) => {
    if (coverslippingBefore === undefined) {
      return;
    }
    await withOwnPage(browser, restoreCoverslipping);
  });

  test("at Staining every slide waits, and the section and the primary name them", async ({
    page,
  }) => {
    await openCase(page, seeded);
    await expandSection(page, STAINING_SECTION);

    const barcodes = new Map(
      ((await readCase(page, seeded.pathologySampleId)).slides ?? []).map(
        (slide) => [String(slide.id), slide.barcode],
      ),
    );
    for (const name of ["A1.1", "A1.2", "A2.1"]) {
      await expect(slideWorkRow(page, "STAINING", name)).toHaveCount(1);
      // The slide cell shows the name; its barcode is read out with it and
      // shown on hover.
      const slideCell = slideWorkRow(page, "STAINING", name).locator(
        "td.pathology-case-view__slide-col--slide",
      );
      await expect(slideCell).toHaveAttribute(
        "title",
        barcodes.get(slideIds[name]) ?? "",
      );
      await expect(slideCell).toContainText(barcodes.get(slideIds[name])!);
      await expect(statusTag(page, "STAINING", name)).toHaveText(
        text("common.pending"),
      );
    }

    const banner = waitingBanner(page, STAINING_SECTION);
    await expect(banner).toHaveCount(1);
    await expect(banner).toContainText("3 slides to stain");
    await expect(banner).toContainText(
      text("pathology.banner.slidesToStainDetail", {
        designations: "A1.1, A1.2, A2.1",
      }),
    );

    await expect(primaryAction(page)).toHaveText("Send for coverslipping");
    await expect(primaryAction(page)).toBeDisabled();
    // The button's hint caps the names it lists; the summary's hover names
    // every one.
    await expect(primaryAction(page)).toHaveAttribute(
      "title",
      text("pathology.locked.stainingIncomplete", {
        outstanding: text("pathology.badge.outstandingMore", {
          designations: "A1.1, A1.2",
          count: 1,
        }),
      }),
    );
    await expect(summaryRow(page, "Staining")).toHaveAttribute(
      "title",
      text("pathology.badge.outstanding", {
        designations: "A1.1, A1.2, A2.1",
      }),
    );
    await expect(sectionHeader(page, STAINING_SECTION)).toContainText(
      text("pathology.badge.stainedOf", { stained: 0, total: 3 }),
    );
  });

  test("one slide marked stained from its row, in the signed-in user's name", async ({
    page,
  }) => {
    await openCase(page, seeded);
    await expandSection(page, STAINING_SECTION);

    await markFromRow(
      page,
      "STAINING",
      text("pathology.action.markStainedOf", { slide: "A1.1" }),
      text("pathology.toast.slidesStained", { slides: "A1.1" }),
    );

    await expect(statusTag(page, "STAINING", "A1.1")).toHaveText(
      text("pathology.badge.stained"),
    );
    await expect(stamp(page, "STAINING", "A1.1")).toHaveText(
      new RegExp(`^${await signedInName(page)}, .+`),
    );
    await expect(sectionHeader(page, STAINING_SECTION)).toContainText(
      text("pathology.badge.stainedOf", { stained: 1, total: 3 }),
    );
    await expect(statusTag(page, "STAINING", "A1.2")).toHaveText(
      text("common.pending"),
    );
  });

  test("two selected slides are stained as a batch the confirmation names", async ({
    page,
  }) => {
    await openCase(page, seeded);
    await expandSection(page, STAINING_SECTION);

    // The batch button stays out of the tab order until a row is selected.
    await expect(slideBatchAction(page, "STAINING")).toHaveAttribute(
      "tabindex",
      "-1",
    );
    await selectSlides(page, "STAINING", ["A1.2", "A2.1"]);
    await expect(slideBatchAction(page, "STAINING")).toHaveAttribute(
      "tabindex",
      "0",
    );
    await expect(slideBatchAction(page, "STAINING")).toHaveText(
      text("pathology.action.markSelectedStained"),
    );
    await slideBatchAction(page, "STAINING").click();

    const confirmRow = slideBatchConfirm(page, "STAINING");
    await expect(confirmRow).toBeVisible();
    await expect(confirmRow).toContainText(
      text("pathology.label.confirmStainBatch", { slides: "A1.2, A2.1" }),
    );
    const confirm = confirmRow.getByRole("button", {
      name: text("common.confirm"),
      exact: true,
    });
    await expect(confirm).toBeEnabled();

    const answer = waitForSlideWork(page, seeded.pathologySampleId, "STAINING");
    await confirm.click();
    await answer;
    await expect(
      toast(
        page,
        text("pathology.toast.slidesStained", { slides: "A1.2, A2.1" }),
      ),
    ).not.toHaveCount(0, { timeout: UI_TIMEOUT });

    for (const name of ["A1.2", "A2.1"]) {
      await expect(statusTag(page, "STAINING", name)).toHaveText(
        text("pathology.badge.stained"),
      );
    }
    await expect(confirmRow).toHaveCount(0);
    await expect(waitingBanner(page, STAINING_SECTION)).toHaveCount(0);
    await expect(sectionHeader(page, STAINING_SECTION)).toContainText(
      text("common.complete"),
    );
    await expect(primaryAction(page)).toHaveText("Send for coverslipping");
    await expect(primaryAction(page)).toBeEnabled();
  });

  test("an unsatisfactory slide holds the case again until it is restained", async ({
    page,
  }) => {
    await openCase(page, seeded);
    await expandSection(page, STAINING_SECTION);

    await markFromRow(
      page,
      "STAINING",
      text("pathology.action.markUnsatisfactoryOf", { slide: "A1.2" }),
      text("pathology.toast.slidesUnsatisfactory", { slides: "A1.2" }),
    );

    await test.step("the notice and the held primary name it", async () => {
      await expect(statusTag(page, "STAINING", "A1.2")).toHaveText(
        text("pathology.badge.unsatisfactory"),
      );
      const notice = redoBanner(page, STAINING_SECTION);
      await expect(notice).toContainText(
        text("pathology.banner.unsatisfactorySlidesHere", {
          designations: "A1.2",
        }),
      );
      await expect(notice).toContainText(
        text("pathology.banner.restainOrRecut"),
      );
      await expect(primaryAction(page)).toBeDisabled();
      await expect(primaryAction(page)).toHaveAttribute(
        "title",
        text("pathology.locked.stainingIncomplete", {
          outstanding: text("pathology.badge.outstanding", {
            designations: "A1.2",
          }),
        }),
      );
    });

    await test.step("its stain can be changed again, a stained slide's cannot", async () => {
      // A stained slide's stain is the one applied: text, with the way to
      // change it written under it, and no picker.
      await expect(stainPicker(page, "A1.1")).toHaveCount(0);
      const applied = frozenStain(page, "STAINING", "A1.1");
      await expect(applied).toHaveText(firstStain.name);
      const appliedId = await applied.getAttribute("id");
      expect(appliedId).toMatch(/^stainingStain\d+$/);
      await expect(page.locator(`#${appliedId}Hint`)).toHaveText(
        text("pathology.locked.stainFrozen"),
      );
      await expect(stainPicker(page, "A1.2")).toBeEnabled();

      await chooseCarbonOption(stainPicker(page, "A1.2"), secondStain.name);
      // A mark records the stored stain, so it waits for the save.
      await expect(slideWorkHold(page, "STAINING")).toHaveText(
        text("pathology.locked.saveBeforeMarking"),
      );
      await saveDraft(page, seeded.pathologySampleId);
      const stored = await readCase(page, seeded.pathologySampleId);
      const restained = (stored.slides ?? []).find(
        (slide) => String(slide.id) === slideIds["A1.2"],
      );
      expect(restained?.stainId).toBe(secondStain.id);
    });

    await test.step("marked stained again, it releases the case", async () => {
      await markFromRow(
        page,
        "STAINING",
        text("pathology.action.markStainedOf", { slide: "A1.2" }),
        text("pathology.toast.slidesStained", { slides: "A1.2" }),
      );
      await expect(statusTag(page, "STAINING", "A1.2")).toHaveText(
        text("pathology.badge.stained"),
      );
      await expect(redoBanner(page, STAINING_SECTION)).toHaveCount(0);
      await expect(sectionHeader(page, STAINING_SECTION)).toContainText(
        text("common.complete"),
      );
      await expect(primaryAction(page)).toBeEnabled();
    });
  });

  test("at Coverslipping the batch is recorded once, and a failed slide holds the case until it passes", async ({
    page,
  }) => {
    await openCase(page, seeded);
    await advanceCase(page, { label: "Send for coverslipping" });
    await expandSection(page, COVERSLIPPING_SECTION);
    const batchField = page.getByRole("textbox", {
      name: text("pathology.label.coverslipBatch"),
    });
    const batch = `CS-${Date.now().toString(36)}`;

    await test.step("the batch saved once reads back and is read-only", async () => {
      await expect(batchField).toBeEditable();
      await batchField.fill(batch);
      await saveDraft(page, seeded.pathologySampleId);

      await openCase(page, seeded);
      await expandSection(page, COVERSLIPPING_SECTION);
      await expect(batchField).toHaveValue(batch);
      await expect(batchField).not.toBeEditable();
    });

    await test.step("two pass and one fails, which holds Send to pathologist", async () => {
      for (const name of ["A1.1", "A1.2"]) {
        await markFromRow(
          page,
          "COVERSLIPPING",
          text("pathology.action.qcPassOf", { slide: name }),
          text("pathology.toast.qcPassed", { slides: name }),
        );
        await expect(statusTag(page, "COVERSLIPPING", name).last()).toHaveText(
          text("pathology.badge.qcPassed"),
        );
        await expect(stamp(page, "COVERSLIPPING", name)).toHaveText(
          new RegExp(`^${await signedInName(page)}, .+`),
        );
      }
      await markFromRow(
        page,
        "COVERSLIPPING",
        text("pathology.action.qcFailOf", { slide: "A2.1" }),
        text("pathology.toast.qcFailed", { slides: "A2.1" }),
      );

      await expect(redoBanner(page, COVERSLIPPING_SECTION)).toContainText(
        text("pathology.banner.failedQcSlidesHere", { designations: "A2.1" }),
      );
      await expect(primaryAction(page)).toHaveText("Send to pathologist");
      await expect(primaryAction(page)).toBeDisabled();
      await expect(primaryAction(page)).toHaveAttribute(
        "title",
        text("pathology.locked.coverslipQcIncomplete", {
          outstanding: text("pathology.badge.outstanding", {
            designations: "A2.1",
          }),
        }),
      );
    });

    await test.step("passed on a second check, it releases the case to the pathologist", async () => {
      await markFromRow(
        page,
        "COVERSLIPPING",
        text("pathology.action.qcPassOf", { slide: "A2.1" }),
        text("pathology.toast.qcPassed", { slides: "A2.1" }),
      );
      await expect(redoBanner(page, COVERSLIPPING_SECTION)).toHaveCount(0);
      await expect(sectionHeader(page, COVERSLIPPING_SECTION)).toContainText(
        text("common.complete"),
      );
      await advanceCase(page, { label: "Send to pathologist" });
    });

    await test.step("with the pathologist, both sections are held and nothing reads pending", async () => {
      await openCase(page, seeded);
      await expectStage(page, "Ready for Pathologist");
      for (const [stage, sectionId, button] of [
        [
          "STAINING",
          STAINING_SECTION,
          text("pathology.action.markUnsatisfactoryOf", { slide: "A1.1" }),
        ],
        [
          "COVERSLIPPING",
          COVERSLIPPING_SECTION,
          text("pathology.action.qcFailOf", { slide: "A1.1" }),
        ],
      ] as const) {
        await expandSection(page, sectionId);
        await expect(slideWorkHold(page, stage)).toHaveText(
          text("pathology.locked.slideWorkAfterBench"),
        );
        await expect(slideWorkButton(page, stage, button)).toBeDisabled();
        // Nothing can be selected for a batch while the case is not on the
        // bench, so the selection and the batch bar are not drawn at all.
        await expect(
          slideWorkSection(page, stage).getByRole("checkbox"),
        ).toHaveCount(0);
        await expect(slideBatchAction(page, stage)).toHaveCount(0);
        await expect(
          slideWorkSection(page, stage).locator(".cds--tag", {
            hasText: new RegExp(
              `^(${text("common.pending")}|${text("pathology.badge.qcAwaiting")})$`,
            ),
          }),
        ).toHaveCount(0);
      }
    });
  });

  test("with Coverslipping switched off, a stained case goes from Staining to the pathologist", async ({
    page,
  }) => {
    await openCase(page, seeded);
    // A case of its own, so the switch is read against slides only this test
    // marked.
    const own = await createPathologyCase(page);
    await moveCaseToStage(page, own, "STAINING");
    await stainSlides(page, own);

    try {
      await setStageSwitch(page, "COVERSLIPPING", "false");
      // The browser reads the configuration once, when the application
      // boots, so a full load puts the switch in front of the screen.
      await openCase(page, own);
      await expect(primaryAction(page)).toHaveText("Send to pathologist");
      await advanceCase(page, { label: "Send to pathologist" });

      const coverslipping = text("pathology.stage.coverslipping");
      await expect(sectionHeader(page, COVERSLIPPING_SECTION)).toContainText(
        text("pathology.locked.stageDisabled"),
      );
      await expect(railStep(page, coverslipping)).toContainText(
        text("caseView.badge.notApplicable"),
      );
      await expect(summaryRow(page, coverslipping)).toContainText(
        text("caseView.badge.notApplicable"),
      );
    } finally {
      await restoreCoverslipping(page);
    }
  });

  test("returned to Coverslipping, slides passed, failed and awaiting, at two desktop widths", async ({
    page,
  }) => {
    await page.setViewportSize({ width: 1480, height: 900 });
    await openCase(page, seeded);
    // Returned to Coverslipping with its statuses kept. A1.2 is restained
    // there, which clears its coverslip check, and A2.1's coverslip fails
    // again, so the section shows each kind of result. The scan below reads
    // the same state.
    await moveCaseToStage(page, seeded, "COVERSLIPPING");
    await recordStainStatus(page, seeded, [slideIds["A1.2"]], "UNSATISFACTORY");
    await recordStainStatus(page, seeded, [slideIds["A1.2"]], "STAINED");
    await openCase(page, seeded);
    await expectStage(page, text("pathology.stage.coverslipping"));
    await expandSection(page, COVERSLIPPING_SECTION);

    const batchField = page.getByRole("textbox", {
      name: text("pathology.label.coverslipBatch"),
    });
    const batch = `CS-${Date.now().toString(36)}`;
    await test.step("the new Coverslipping entry takes a batch of its own", async () => {
      await expect(batchField).toBeEditable();
      await batchField.fill(batch);
      await saveDraft(page, seeded.pathologySampleId);
      await expect(batchField).toHaveValue(batch);
    });

    await markFromRow(
      page,
      "COVERSLIPPING",
      text("pathology.action.qcFailOf", { slide: "A2.1" }),
      text("pathology.toast.qcFailed", { slides: "A2.1" }),
    );

    await test.step("passed, awaiting and failed read in block order", async () => {
      const qc = (name: string) =>
        statusTag(page, "COVERSLIPPING", name).last();
      await expect(qc("A1.1")).toHaveText(text("pathology.badge.qcPassed"));
      await expect(qc("A1.2")).toHaveText(text("pathology.badge.qcAwaiting"));
      await expect(qc("A2.1")).toHaveText(text("common.failed"));
      await expect(waitingBanner(page, COVERSLIPPING_SECTION)).toContainText(
        text("pathology.banner.slidesToQcDetail", {
          designations: "A1.2, A2.1",
        }),
      );
      await expect(redoBanner(page, COVERSLIPPING_SECTION)).toContainText(
        text("pathology.banner.failedQcSlidesHere", { designations: "A2.1" }),
      );
      await expect(primaryAction(page)).toHaveAttribute(
        "title",
        text("pathology.locked.coverslipQcIncomplete", {
          outstanding: text("pathology.badge.outstanding", {
            designations: "A1.2, A2.1",
          }),
        }),
      );
    });

    // Read afresh for the pictures, so neither the toasts nor the print
    // panel a save opens cover the section.
    await openCase(page, seeded);
    await expandSection(page, COVERSLIPPING_SECTION);
    await shootAtTwoWidths(
      page,
      "COVERSLIPPING",
      "ogc-264-s10-coverslipping-qc",
    );
  });

  test("a case at Staining with stained, pending and unsatisfactory slides, at two desktop widths", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    await page.setViewportSize({ width: 1480, height: 900 });
    await openCase(page, seeded);
    // A case of its own, so its slides can stand in all three states at once.
    const { seededCase, ids } = await seedCaseAtStaining(page, firstStain);
    await stainSlides(page, seededCase, { slideIds: [ids["A1.1"]] });
    await recordStainStatus(page, seededCase, [ids["A2.1"]], "UNSATISFACTORY");

    await openCase(page, seededCase);
    await expandSection(page, STAINING_SECTION);
    await expect(statusTag(page, "STAINING", "A1.1")).toHaveText(
      text("pathology.badge.stained"),
    );
    await expect(stamp(page, "STAINING", "A1.1")).toHaveText(
      new RegExp(`^${await signedInName(page)}, .+`),
    );
    await expect(statusTag(page, "STAINING", "A1.2")).toHaveText(
      text("common.pending"),
    );
    await expect(statusTag(page, "STAINING", "A2.1")).toHaveText(
      text("pathology.badge.unsatisfactory"),
    );
    await expect(redoBanner(page, STAINING_SECTION)).toContainText(
      text("pathology.banner.unsatisfactorySlidesHere", {
        designations: "A2.1",
      }),
    );
    await expect(waitingBanner(page, STAINING_SECTION)).toContainText(
      text("pathology.banner.slidesToStainDetail", {
        designations: "A1.2, A2.1",
      }),
    );

    await shootAtTwoWidths(page, "STAINING", "ogc-264-s10-staining-qc");
  });

  test("both sections, with slides passed, failed and awaiting, pass an accessibility scan", async ({
    page,
  }, testInfo) => {
    await page.setViewportSize({ width: 1480, height: 900 });
    await openCase(page, seeded);
    await expectStage(page, text("pathology.stage.coverslipping"));
    await expandSection(page, STAINING_SECTION);
    await expandSection(page, COVERSLIPPING_SECTION);
    await expect(redoBanner(page, COVERSLIPPING_SECTION)).toBeVisible();

    // The shared header's notification bell is named "Notifications" while
    // its visible text is the unread count, which axe reports on every page
    // of the application. It is the layout's, not these sections'.
    const scan = await new AxeBuilder({ page })
      .withTags(WCAG_21_AA_TAGS)
      .include(`#${STAINING_SECTION}`)
      .include(`#${COVERSLIPPING_SECTION}`)
      .exclude("#notification-Icon")
      .analyze();
    const jsonPath = testInfo.outputPath("axe-pathology-staining-qc.json");
    await writeFile(jsonPath, `${JSON.stringify(scan.violations, null, 2)}\n`);
    await testInfo.attach("axe-pathology-staining-qc", {
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
