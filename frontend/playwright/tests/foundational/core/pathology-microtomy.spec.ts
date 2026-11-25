import AxeBuilder from "@axe-core/playwright";
import { writeFile } from "fs/promises";
import type { Browser, Page } from "@playwright/test";
import { test, expect } from "../../../helpers/test-base";
import {
  createPathologyCase,
  discoverPathologyOrderTarget,
  ensureStain,
  ensureTissueType,
  moveCaseToStage,
  readCase,
  saveCaseThroughApi,
  SeededPathologyCase,
  Stain,
  TissueType,
  verifySlideLabels,
} from "../../../helpers/seed-pathology-data";
import {
  addSlideButton,
  addSlideTo,
  advanceCase,
  blockGroup,
  expandSection,
  expectStageSwitchesOn,
  openCase,
  primaryAction,
  railStep,
  readStageSwitch,
  rowNamed,
  saveDraft,
  sectionHeader,
  setStageSwitch,
  summaryRow,
  type OptionalStageId,
} from "../../../helpers/pathology-case-view";
import { chooseCarbonOption } from "../../../helpers/carbon-select";
import { expectPdf, printLabels } from "../../../helpers/pathology-labels";
import { UI_TIMEOUT } from "../../../helpers/timeouts";

/**
 * The microtomy section of the pathology case view lists the case's blocks,
 * each expandable to the slides cut from it (FR-7). A slide is added under
 * the block it is cut from, so it names its block without a picker, and it
 * records its level and its stain (FR-7.2).
 *
 * What only a deployed stack proves here:
 *   - a slide added under a block is saved as cut from that block and comes
 *     back under it, named by the server, with the level and stain it was
 *     given; the stain is an entry of the laboratory's stain category,
 *   - a cassette not yet embedded takes no slide while the laboratory tracks
 *     embedding, and takes one as soon as it is embedded, or once embedding
 *     is switched off,
 *   - the case is not sent for staining before a slide is cut and its label
 *     checked,
 *   - a slide cut from another page at the same time is kept beside this
 *     page's own,
 *   - the slide labels print as one PDF, and
 *   - with Microtomy switched off, the slides on record stay readable.
 *
 * Serial, and on one seeded case, because the walk is the point: each test
 * reads the slides the earlier ones cut.
 *
 * Embedding, Microtomy and Staining must be tracked on the stack (their stage
 * switches on, the shipped default), so the spec checks that first. Two
 * tests switch Embedding or Microtomy off and put them back.
 */

const MICROTOMY = "pathology-section-microtomy";
const GROSSING = "pathology-section-grossing";
const EMBEDDING = "pathology-section-embedding";

const WCAG_21_AA_TAGS = ["wcag2a", "wcag2aa", "wcag21a", "wcag21aa"];

const NO_SLIDES = "No slides in use under this block";
const EMBED_FIRST = "Embed this cassette at Embedding first";

let seeded: SeededPathologyCase;
let tissue: TissueType;
let stain: Stain;
/** The stage switches as the deployment had them before this spec ran. */
const switchesBefore: Partial<Record<OptionalStageId, string>> = {};

/** The slide rows listed under one block. */
function slidesUnder(page: Page, designation: string) {
  return blockGroup(page, designation).locator(".pathology-case-view__row");
}

/** The line a block's group opens with: the block, its tissue and count. */
function groupHeader(page: Page, designation: string) {
  return blockGroup(page, designation).locator(
    ".pathology-case-view__group-header",
  );
}

/** The count a block's group header carries, "1 slide", "2 slides". */
function groupCount(page: Page, designation: string) {
  return groupHeader(page, designation)
    .locator(".cds--tag")
    .filter({ hasText: /^\d+ slides?$/ });
}

/** A block's Cassette or Block tag in its group header, matched whole. */
function groupStateTag(page: Page, designation: string) {
  return groupHeader(page, designation).locator(".cds--tag", {
    hasText: /^(Cassette|Block)$/,
  });
}

/** The ghost button that opens and closes a block's group. */
function expandButton(page: Page, designation: string) {
  return page.getByRole("button", {
    name: `Slides of ${designation}`,
    exact: true,
  });
}

/** A slide's barcode, under its block. */
function slideBarcode(designation: string, slide: string) {
  return `${seeded.accessionNumber}.${designation}.${slide}`;
}

/** The badge in the microtomy section's header. */
function sectionBadge(page: Page) {
  return sectionHeader(page, MICROTOMY).locator(".cds--tag");
}

/** Wait for the case to be read back, the way the screen re-reads it. */
function caseReadBack(page: Page) {
  return page.waitForResponse(
    (r) =>
      r.request().method() === "GET" &&
      new URL(r.url()).pathname.endsWith(
        `/rest/pathology/caseView/${seeded.pathologySampleId}`,
      ),
    { timeout: UI_TIMEOUT },
  );
}

/** Fill a slide's level and choose its stain, by the slide's row name. */
async function describeSlide(
  page: Page,
  designation: string,
  rowName: string,
  level: string,
) {
  const rows = slidesUnder(page, designation);
  await rows.getByLabel(`Level ${rowName}`, { exact: true }).fill(level);
  await chooseCarbonOption(
    rows.getByRole("combobox", { name: `Stain ${rowName}`, exact: true }),
    stain.name,
  );
}

/**
 * Embed one cassette from the embedding section and wait for the screen to
 * finish with it: the post, the toast that names it, and the case read back.
 */
async function embed(page: Page, designation: string) {
  const button = page.getByRole("button", {
    name: `Embed cassette ${designation}`,
    exact: true,
  });
  await expect(button).toBeEnabled();
  const posted = page.waitForResponse(
    (r) =>
      r.request().method() === "POST" &&
      /\/rest\/pathology\/block\/[^/]+\/embed$/.test(new URL(r.url()).pathname),
  );
  const readBack = caseReadBack(page);
  await button.click();
  await posted;
  await readBack;
  await expect(
    page
      .locator(".cds--toast-notification")
      .filter({ hasText: `Cassette ${designation} embedded` }),
  ).not.toHaveCount(0, { timeout: UI_TIMEOUT });
  await expect(button).toHaveCount(0);
}

/** Open a fresh, signed-in page for a hook or a second user. */
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

/** Put one stage switch back as the deployment had it. */
async function restoreSwitch(page: Page, stage: OptionalStageId) {
  await setStageSwitch(page, stage, switchesBefore[stage] ?? "true");
}

test.describe.serial("Pathology microtomy section", () => {
  // Order entry and the case view are both heavy screens, and a first visit
  // on a development server compiles them as it serves them.
  test.setTimeout(120_000);

  test.beforeAll(async ({ browser }) => {
    test.setTimeout(120_000);
    await withOwnPage(browser, async (page) => {
      for (const stage of ["EMBEDDING", "MICROTOMY"] as const) {
        switchesBefore[stage] = await readStageSwitch(page, stage);
      }
      await expectStageSwitchesOn(page, ["EMBEDDING", "MICROTOMY", "STAINING"]);
      const target = await discoverPathologyOrderTarget(page);
      seeded = await createPathologyCase(page, target);
      tissue = await ensureTissueType(page);
      stain = await ensureStain(page);

      // Grossing is not what this spec is about, so the two cassettes are
      // cut through the case save the grossing section posts, the first with
      // a tissue, and the case taken to Microtomy through the move endpoints,
      // which embed both on the way and cut nothing at Microtomy itself.
      const caseId = seeded.pathologySampleId;
      await moveCaseToStage(page, seeded, "GROSSING");
      await saveCaseThroughApi(page, caseId, await readCase(page, caseId), {
        grossExam: "Two cores of skin, 0.4 cm each, submitted whole.",
        addCassette: true,
        cassetteTissueTypeId: tissue.id,
      });
      await saveCaseThroughApi(page, caseId, await readCase(page, caseId), {
        addCassette: true,
      });
      await moveCaseToStage(page, seeded, "MICROTOMY");

      const stored = await readCase(page, caseId);
      expect(stored.status).toBe("MICROTOMY");
      expect(
        (stored.blocks ?? []).map((block) => [
          block.designation,
          block.cassetteState,
        ]),
      ).toEqual([
        ["A1", "BLOCK"],
        ["A2", "BLOCK"],
      ]);
      expect(stored.slides ?? [], "no slide is cut on the way").toEqual([]);
    });
  });

  // A test Playwright times out never reaches its own finally, and a stage
  // left switched off changes what every later pathology spec reads, so the
  // switches are put back from a hook as well.
  test.afterAll(async ({ browser }) => {
    if (Object.keys(switchesBefore).length === 0) {
      return;
    }
    await withOwnPage(browser, async (page) => {
      await restoreSwitch(page, "EMBEDDING");
      await restoreSwitch(page, "MICROTOMY");
    });
  });

  test("each block is listed with nothing cut from it, and the case waits for a slide", async ({
    page,
  }) => {
    await openCase(page, seeded);
    await expandSection(page, MICROTOMY);

    await test.step("the section says how it is read", async () => {
      await expect(
        page.locator(`#${MICROTOMY} .pathology-case-view__lead`),
      ).toHaveText("Blocks expand to the slides cut from them.");
    });

    await test.step("both blocks are listed, closed, with no slide", async () => {
      for (const designation of ["A1", "A2"]) {
        await expect(blockGroup(page, designation)).toHaveCount(1);
        await expect(groupStateTag(page, designation)).toHaveText("Block");
        await expect(groupHeader(page, designation)).toContainText(NO_SLIDES);
        await expect(groupCount(page, designation)).toHaveCount(0);
        await expect(expandButton(page, designation)).toHaveAttribute(
          "aria-expanded",
          "false",
        );
        await expect(
          blockGroup(page, designation).locator(
            ".pathology-case-view__group-body",
          ),
        ).toBeHidden();
        await expect(addSlideButton(page, designation)).toBeEnabled();
      }
      await expect(groupHeader(page, "A1")).toContainText(tissue.name);
      await expect(groupHeader(page, "A2")).toContainText("— not recorded");
    });

    await test.step("Send for staining waits for the first slide", async () => {
      await expect(primaryAction(page)).toHaveText("Send for staining");
      await expect(primaryAction(page)).toBeDisabled();
      await expect(primaryAction(page)).toHaveAttribute(
        "title",
        "Cut at least one slide first",
      );
      await expect(summaryRow(page, "Slides")).toHaveText(/^Slides\s*0$/);
    });
  });

  test("a cassette cut after the stage takes slides once it is embedded", async ({
    page,
  }) => {
    await openCase(page, seeded);

    await test.step("a third cassette is cut at grossing", async () => {
      await expandSection(page, GROSSING);
      await page.getByRole("button", { name: "Add cassette" }).click();
      await saveDraft(page, seeded.pathologySampleId);
    });

    await test.step("its group says it must be embedded first", async () => {
      await expandSection(page, MICROTOMY);
      await expect(blockGroup(page, "A3")).toHaveCount(1);
      await expect(groupStateTag(page, "A3")).toHaveText("Cassette");
      const add = addSlideButton(page, "A3");
      await expect(add).toBeDisabled();
      await expect(add).toHaveAttribute("title", EMBED_FIRST);
      // A disabled button takes no focus, so the reason is also on the page.
      await expect(add).toHaveAccessibleDescription(EMBED_FIRST);
      await expect(groupHeader(page, "A3")).toContainText(EMBED_FIRST);
    });

    await test.step("embedded from the embedding section, it takes slides", async () => {
      await expandSection(page, EMBEDDING);
      await embed(page, "A3");
      await expect(groupStateTag(page, "A3")).toHaveText("Block");
      await expect(addSlideButton(page, "A3")).toBeEnabled();
      await expect(groupHeader(page, "A3")).not.toContainText(EMBED_FIRST);
    });
  });

  test("two slides cut under one block carry their level and stain", async ({
    page,
  }) => {
    await openCase(page, seeded);
    await expandSection(page, MICROTOMY);

    await addSlideTo(page, "A1");
    await addSlideTo(page, "A1");
    await expect(slidesUnder(page, "A1")).toHaveCount(2);
    await describeSlide(page, "A1", "New slide 1", "1");
    await describeSlide(page, "A1", "New slide 2", "2");
    await saveDraft(page, seeded.pathologySampleId);

    await test.step("both are named under A1 with what they were given", async () => {
      await expect(slidesUnder(page, "A1")).toHaveCount(2);
      for (const slide of ["1", "2"]) {
        const barcode = slideBarcode("A1", slide);
        const row = rowNamed(slidesUnder(page, "A1"), slide);
        await expect(row).toHaveCount(1);
        await expect(
          row.locator(".pathology-case-view__row-barcode"),
        ).toHaveText(
          new RegExp(`^Barcode:\\s*${barcode.replace(/\./g, "\\.")}$`),
        );
        await expect(
          row.getByLabel(`Level ${barcode}`, { exact: true }),
        ).toHaveValue(slide);
        await expect(
          row.getByRole("combobox", { name: `Stain ${barcode}`, exact: true }),
        ).toHaveValue(stain.name);
      }
    });

    await test.step("every count is read off the slides", async () => {
      await expect(sectionBadge(page)).toHaveText("2 slides");
      await expect(summaryRow(page, "Slides")).toHaveText(/^Slides\s*2$/);
      await expect(groupCount(page, "A1")).toHaveText("2 slides");
      await expect(groupHeader(page, "A2")).toContainText(NO_SLIDES);
      await expect(slidesUnder(page, "A2")).toHaveCount(0);
    });

    await test.step("the case now waits for both labels to be checked", async () => {
      await expect(primaryAction(page)).toHaveText("Send for staining");
      await expect(primaryAction(page)).toBeDisabled();
      await expect(primaryAction(page)).toHaveAttribute(
        "title",
        "Verify every slide label first (A1.1, A1.2 outstanding)",
      );
    });

    await test.step("a fresh load shows the same, A1 opened for its slides", async () => {
      await openCase(page, seeded);
      await expandSection(page, MICROTOMY);
      await expect(expandButton(page, "A1")).toHaveAttribute(
        "aria-expanded",
        "true",
      );
      await expect(slidesUnder(page, "A1")).toHaveCount(2);
      await expect(slidesUnder(page, "A1").first()).toBeVisible();
    });

    await test.step("once both labels are checked, the case may be sent for staining", async () => {
      // Checking a label is the label verification spec's subject, so here
      // it goes through the endpoint the section posts to.
      await verifySlideLabels(page, seeded);
      await openCase(page, seeded);
      await expect(primaryAction(page)).toHaveText("Send for staining");
      await expect(primaryAction(page)).toBeEnabled();
    });
  });

  test("the case's slide labels print as one PDF from the lab number", async ({
    page,
  }) => {
    await openCase(page, seeded);
    await expandSection(page, MICROTOMY);

    const { popup, response } = await printLabels(
      page,
      "Print all slide labels",
    );
    const wanted = `/LabelMakerServlet?labelType=slide&code=${encodeURIComponent(seeded.accessionNumber)}`;
    expect(response.url()).toContain(wanted);
    await expectPdf(response);
    await popup.close();
  });

  test("with Embedding switched off, a cassette takes slides as it is", async ({
    page,
  }) => {
    await openCase(page, seeded);
    await expandSection(page, GROSSING);
    await page.getByRole("button", { name: "Add cassette" }).click();
    await saveDraft(page, seeded.pathologySampleId);
    await expandSection(page, MICROTOMY);
    await expect(groupStateTag(page, "A4")).toHaveText("Cassette");
    await expect(addSlideButton(page, "A4")).toBeDisabled();

    try {
      await setStageSwitch(page, "EMBEDDING", "false");
      // The browser reads the configuration once, when the application
      // boots, so a full load puts the switch in front of the screen.
      await openCase(page, seeded);
      await expandSection(page, MICROTOMY);
      await expect(railStep(page, "Embedding")).toContainText("N/A");
      await expect(groupStateTag(page, "A4")).toHaveText("Cassette");
      await expect(addSlideButton(page, "A4")).toBeEnabled();
      await expect(groupHeader(page, "A4")).not.toContainText(EMBED_FIRST);
    } finally {
      await restoreSwitch(page, "EMBEDDING");
    }

    await openCase(page, seeded);
    await expandSection(page, MICROTOMY);
    await expect(addSlideButton(page, "A4")).toBeDisabled();
  });

  test("a slide cut from another page meanwhile is kept beside this page's own", async ({
    page,
    browser,
  }) => {
    await openCase(page, seeded);
    await expandSection(page, MICROTOMY);
    await expect(slidesUnder(page, "A2")).toHaveCount(0);

    await test.step("someone else cuts a slide under A2 from their own page", async () => {
      await withOwnPage(browser, async (otherPage) => {
        const caseId = seeded.pathologySampleId;
        const stored = await readCase(otherPage, caseId);
        const a2 = (stored.blocks ?? []).find(
          (block) => block.designation === "A2",
        );
        expect(a2?.id, "A2 is on the case").toBeDefined();
        await saveCaseThroughApi(otherPage, caseId, stored, {
          addSlideUnder: a2!.id!,
        });
      });
    });

    await test.step("this page, still showing A2 empty, cuts one there too", async () => {
      await expect(slidesUnder(page, "A2")).toHaveCount(0);
      await addSlideTo(page, "A2");
      await slidesUnder(page, "A2")
        .getByLabel("Level New slide 1", { exact: true })
        .fill("3");
      // The two slides are named apart by the server, so the save neither
      // collides nor drops the other page's slide.
      await saveDraft(page, seeded.pathologySampleId);
    });

    await test.step("both slides are under A2, each with its own name", async () => {
      await expect(slidesUnder(page, "A2")).toHaveCount(2);
      for (const [slide, level] of [
        ["1", "1"],
        ["2", "3"],
      ]) {
        const barcode = slideBarcode("A2", slide);
        const row = rowNamed(slidesUnder(page, "A2"), slide);
        await expect(row).toHaveCount(1);
        await expect(
          row.getByLabel(`Level ${barcode}`, { exact: true }),
        ).toHaveValue(level);
      }
      await expect(groupCount(page, "A2")).toHaveText("2 slides");
      await expect(summaryRow(page, "Slides")).toHaveText(/^Slides\s*4$/);
    });
  });

  test("the microtomy section passes an accessibility scan", async ({
    page,
  }, testInfo) => {
    await page.setViewportSize({ width: 1480, height: 900 });
    await openCase(page, seeded);
    await expandSection(page, MICROTOMY);
    await expect(slidesUnder(page, "A1").first()).toBeVisible();
    await expect(slidesUnder(page, "A2").first()).toBeVisible();

    // The shared header's notification bell is named "Notifications" while
    // its visible text is the unread count, which axe reports on every page
    // of the application. It is the layout's, not this section's.
    const scan = await new AxeBuilder({ page })
      .withTags(WCAG_21_AA_TAGS)
      .include(`#${MICROTOMY}`)
      .exclude("#notification-Icon")
      .analyze();
    const jsonPath = testInfo.outputPath("axe-pathology-microtomy.json");
    await writeFile(jsonPath, `${JSON.stringify(scan.violations, null, 2)}\n`);
    await testInfo.attach("axe-pathology-microtomy", {
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

  test("the case view at two desktop widths, for the pull request", async ({
    page,
  }) => {
    // Opened once and resized in place: a second load while the first one's
    // lookups are still in flight aborts them, and the browser logs each as a
    // failed fetch.
    await page.setViewportSize({ width: 1480, height: 900 });
    await openCase(page, seeded);
    await expandSection(page, MICROTOMY);

    for (const width of [1480, 1280]) {
      await page.setViewportSize({ width, height: 900 });
      await expect(expandButton(page, "A1")).toHaveAttribute(
        "aria-expanded",
        "true",
      );
      await expect(rowNamed(slidesUnder(page, "A1"), "1")).toBeVisible();
      await sectionHeader(page, MICROTOMY).scrollIntoViewIfNeeded();

      await page.screenshot({
        path: new URL(
          `../../../../e2e-evidence/ogc-264-s8-microtomy-${width}.png`,
          import.meta.url,
        ).pathname,
        fullPage: true,
      });
    }
  });

  test("a slide deactivated with a reason leaves every count", async ({
    page,
  }) => {
    await openCase(page, seeded);
    await expandSection(page, MICROTOMY);
    const barcode = slideBarcode("A1", "2");

    await test.step("it is deactivated through the dialog", async () => {
      await page
        .getByRole("button", {
          name: `Deactivate slide ${barcode}`,
          exact: true,
        })
        .click();
      const dialog = page.getByRole("dialog");
      await expect(dialog).toBeVisible();
      await expect(
        dialog.getByRole("heading", { name: `Deactivate slide ${barcode}?` }),
      ).toBeVisible();
      await dialog.getByLabel("Reason", { exact: true }).fill("Section torn");
      const retired = page.waitForResponse(
        (r) =>
          r.request().method() === "POST" &&
          /\/rest\/pathology\/slide\/[^/]+\/deactivate$/.test(
            new URL(r.url()).pathname,
          ),
      );
      const readBack = caseReadBack(page);
      // Carbon prefixes a danger button's name with a visually hidden word.
      await dialog.getByRole("button", { name: /Deactivate$/ }).click();
      await retired;
      await readBack;
      await expect(dialog).toHaveCount(0);
      await expect(
        page
          .locator(".cds--toast-notification")
          .filter({ hasText: `Slide ${barcode} deactivated` }),
      ).not.toHaveCount(0, { timeout: UI_TIMEOUT });
    });

    await test.step("it leaves A1's list and every count", async () => {
      await expect(rowNamed(slidesUnder(page, "A1"), "2")).toHaveCount(0);
      await expect(slidesUnder(page, "A1")).toHaveCount(1);
      await expect(groupCount(page, "A1")).toHaveText("1 slide");
      await expect(sectionBadge(page)).toHaveText("3 slides");
      await expect(summaryRow(page, "Slides")).toHaveText(/^Slides\s*3$/);
    });

    await test.step("the switch shows it under A1, retired", async () => {
      const toggle = page.locator("#showDeactivatedSlides");
      await expect(toggle).toBeVisible();
      await page.locator(`#${MICROTOMY} label.cds--toggle__label`).click();
      await expect(toggle).toHaveAttribute("aria-checked", "true");

      const row = rowNamed(slidesUnder(page, "A1"), "2");
      await expect(row).toBeVisible();
      await expect(
        row.locator(".cds--tag", { hasText: /^Deactivated$/ }),
      ).toBeVisible();
      await expect(
        row.getByRole("button", { name: /^Deactivate slide/ }),
      ).toHaveCount(0);
      // Shown, it is still not counted.
      await expect(groupCount(page, "A1")).toHaveText("1 slide");
    });
  });

  test("with Microtomy switched off, the slides on record are shown for reading", async ({
    page,
  }) => {
    // A section's own current stage always applies, so the case is sent on
    // to Staining before the switch is turned off, once the labels of the
    // slides cut since the last check are checked too.
    await openCase(page, seeded);
    await verifySlideLabels(page, seeded);
    await openCase(page, seeded);
    await advanceCase(page, { label: "Send for staining" });
    await expandSection(page, MICROTOMY);
    const slidesBefore = (await summaryRow(page, "Slides").innerText())
      .replace(/^Slides\s*/, "")
      .trim();

    try {
      await setStageSwitch(page, "MICROTOMY", "false");
      await openCase(page, seeded);

      await expect(sectionHeader(page, MICROTOMY)).toContainText(
        "Not tracked at this laboratory",
      );
      await expect(railStep(page, "Microtomy")).toContainText("N/A");
      await expandSection(page, MICROTOMY);
      await expect(
        page.locator(`#${MICROTOMY} .cds--inline-notification`),
      ).toHaveText(
        /Not tracked at this laboratory\. The slides on record are shown for reading only\./,
      );

      const barcode = slideBarcode("A1", "1");
      const row = rowNamed(slidesUnder(page, "A1"), "1");
      await expect(row).toBeVisible();
      await expect(
        row.getByLabel(`Level ${barcode}`, { exact: true }),
      ).toBeDisabled();
      await expect(
        page
          .locator(`#${MICROTOMY}`)
          .getByRole("button", { name: /^Deactivate slide/ }),
      ).toHaveCount(0);
      for (const designation of ["A1", "A2", "A3", "A4"]) {
        await expect(addSlideButton(page, designation)).toBeDisabled();
      }
      await expect(summaryRow(page, "Slides")).toHaveText(
        new RegExp(`^Slides\\s*${slidesBefore}$`),
      );
    } finally {
      await restoreSwitch(page, "MICROTOMY");
    }

    await openCase(page, seeded);
    await expect(railStep(page, "Microtomy")).not.toContainText("N/A");
  });
});
