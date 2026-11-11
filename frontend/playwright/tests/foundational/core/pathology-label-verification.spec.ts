import AxeBuilder from "@axe-core/playwright";
import { writeFile } from "fs/promises";
import type { Browser, Locator, Page } from "@playwright/test";
import { test, expect } from "../../../helpers/test-base";
import {
  apiGet,
  createPathologyCase,
  discoverPathologyOrderTarget,
  moveCaseToStage,
  readCase,
  saveCaseThroughApi,
  type CaseSlide,
  type SeededPathologyCase,
} from "../../../helpers/seed-pathology-data";
import {
  advanceCase,
  blockGroup,
  expandSection,
  expectStage,
  expectStageSwitchesOn,
  labelTag,
  openCase,
  primaryAction,
  readSiteSetting,
  rowNamed,
  scanFields,
  SCAN_VERIFICATION_SETTING,
  sectionHeader,
  setSiteSetting,
  verifyLabelButton,
} from "../../../helpers/pathology-case-view";
import { printedLabelText } from "../../../helpers/pathology-labels";
import { UI_TIMEOUT } from "../../../helpers/timeouts";

/**
 * Before a slide goes on to staining, its printed label is checked against
 * the block it was cut from (FR-7.3 to FR-7.5): the block and the slide's
 * label are scanned, or typed as printed where the laboratory has no scanner,
 * and the server says whether they belong together.
 *
 * What only a deployed stack proves here:
 *   - a slide label scanned against the wrong block is refused with an alert
 *     naming both, and the slide stays unverified,
 *   - the right pair verifies the slide in the signed-in user's name,
 *   - the case is not sent for staining until every slide label is verified,
 *   - with the laboratory's scanner setting off, the identifiers as printed
 *     verify a slide, and
 *   - the slide labels print with the slide barcodes on them.
 *
 * Serial, and on one seeded case, because each test reads what the earlier
 * ones verified. Embedding, Microtomy and Staining must be tracked, and the
 * scanner setting on (the shipped default), so the spec checks both first.
 */

const API_PREFIX = "/api/OpenELIS-Global";

const MICROTOMY = "pathology-section-microtomy";

const WCAG_21_AA_TAGS = ["wcag2a", "wcag2aa", "wcag21a", "wcag21aa"];

let seeded: SeededPathologyCase;
/** The scanner setting as the deployment had it before this spec ran. */
let scanSettingBefore: string | undefined;

/** One block or slide of the seeded case: its id and the barcode on it. */
interface Labelled {
  id: string;
  barcode: string;
}

let blockA1: Labelled;
let blockA2: Labelled;
let slideA11: Labelled;
let slideA21: Labelled;
/** Cut once the case is returned to Microtomy, and left unverified. */
let slideA22: Labelled;

/** The slide rows listed under one block. */
function slidesUnder(page: Page, designation: string) {
  return blockGroup(page, designation).locator(".pathology-case-view__row");
}

/** The open label check of one slide. */
function verifyBlock(page: Page, slideId: string) {
  return page.locator(`#verifyLabel-${slideId}`);
}

/** The warning the section opens on while a slide label waits. */
function unverifiedBanner(page: Page) {
  return page.locator(`#${MICROTOMY} .cds--inline-notification--warning`);
}

/** The span a Verified tag sits in, whose title names who checked it. */
function verifiedBy(row: Locator) {
  return row.locator(".pathology-case-view__label-verification");
}

/** Wait for the answer to one slide's label check. */
function verifyAnswer(page: Page, slideId: string) {
  return page.waitForResponse(
    (r) =>
      r.request().method() === "POST" &&
      new URL(r.url()).pathname.endsWith(
        `/rest/pathology/slide/${slideId}/verify-label`,
      ),
    { timeout: UI_TIMEOUT },
  );
}

/** The success toast a label check raises, naming the slide. */
function verifiedToast(page: Page, slideName: string) {
  return page
    .locator(".cds--toast-notification")
    .filter({ hasText: `${slideName} label verified` });
}

/**
 * The name the server records as the one who checked a label: the signed-in
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

/** The stored id and barcode of one row. */
function labelled(row: { id?: number | string; barcode?: string } | undefined) {
  expect(row?.id, "the row is on the case").toBeDefined();
  expect(row?.barcode, "the row carries a barcode").toBeTruthy();
  return { id: String(row!.id), barcode: row!.barcode! };
}

/** The one slide in use under a block, by the block's id and its designation. */
function slideUnder(slides: CaseSlide[], blockId: string, designation: string) {
  return slides.find(
    (slide) =>
      String(slide.blockId) === blockId &&
      slide.designation === designation &&
      slide.active !== false,
  );
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

/** Put the scanner setting back as the deployment had it. */
async function restoreScanSetting(page: Page) {
  await setSiteSetting(
    page,
    SCAN_VERIFICATION_SETTING,
    scanSettingBefore ?? "true",
  );
}

test.describe.serial("Pathology slide label verification", () => {
  // Order entry and the case view are both heavy screens, and a first visit
  // on a development server compiles them as it serves them.
  test.setTimeout(120_000);

  test.beforeAll(async ({ browser }) => {
    test.setTimeout(120_000);
    await withOwnPage(browser, async (page) => {
      scanSettingBefore = await readSiteSetting(
        page,
        SCAN_VERIFICATION_SETTING,
      );
      if (scanSettingBefore !== "true") {
        throw new Error(
          `${SCAN_VERIFICATION_SETTING} reads "${scanSettingBefore}" on this deployment. ` +
            "This spec checks labels with the scanner first; switch it back on on the " +
            "Result Entry Configuration page, or find the spec that left it off.",
        );
      }
      await expectStageSwitchesOn(page, ["EMBEDDING", "MICROTOMY", "STAINING"]);

      // Grossing, embedding and cutting are not what this spec is about, so
      // two cassettes are cut through the case save, the case is taken to
      // Microtomy through the move endpoints, which embed both on the way,
      // and one slide is cut under each block through the case save.
      seeded = await createPathologyCase(
        page,
        await discoverPathologyOrderTarget(page),
      );
      const caseId = seeded.pathologySampleId;
      await moveCaseToStage(page, seeded, "GROSSING");
      await saveCaseThroughApi(page, caseId, await readCase(page, caseId), {
        grossExam: "Two cores of breast tissue, 1.2 cm each, submitted whole.",
        addCassette: true,
      });
      await saveCaseThroughApi(page, caseId, await readCase(page, caseId), {
        addCassette: true,
      });
      await moveCaseToStage(page, seeded, "MICROTOMY");

      let stored = await readCase(page, caseId);
      const blocks = stored.blocks ?? [];
      blockA1 = labelled(blocks.find((block) => block.designation === "A1"));
      blockA2 = labelled(blocks.find((block) => block.designation === "A2"));
      await saveCaseThroughApi(page, caseId, stored, {
        addSlideUnder: blockA1.id,
      });
      await saveCaseThroughApi(page, caseId, await readCase(page, caseId), {
        addSlideUnder: blockA2.id,
      });

      stored = await readCase(page, caseId);
      expect(stored.status).toBe("MICROTOMY");
      slideA11 = labelled(slideUnder(stored.slides ?? [], blockA1.id, "1"));
      slideA21 = labelled(slideUnder(stored.slides ?? [], blockA2.id, "1"));
      expect(
        Object.values(stored.labelVerifications ?? {}).filter(Boolean),
        "no slide label is checked on the way",
      ).toEqual([]);
    });
  });

  // A test Playwright times out never reaches its own finally, and the
  // setting left off changes what every later pathology spec reads, so it is
  // put back from a hook as well.
  test.afterAll(async ({ browser }) => {
    if (scanSettingBefore === undefined) {
      return;
    }
    await withOwnPage(browser, restoreScanSetting);
  });

  test("both slides wait unverified, and the section names them", async ({
    page,
  }) => {
    await openCase(page, seeded);
    await expandSection(page, MICROTOMY);

    for (const designation of ["A1", "A2"]) {
      const row = rowNamed(slidesUnder(page, designation), "1");
      await expect(row).toHaveCount(1);
      await expect(labelTag(row)).toHaveText("Unverified");
    }
    await expect(verifyLabelButton(page, "A1.1")).toBeEnabled();
    await expect(verifyLabelButton(page, "A2.1")).toBeEnabled();

    await expect(unverifiedBanner(page)).toHaveCount(1);
    await expect(unverifiedBanner(page)).toContainText(
      "2 slide labels to verify",
    );
    await expect(unverifiedBanner(page)).toContainText(
      "Verify the labels of A1.1, A2.1 before the case moves on.",
    );
  });

  test("the case is not sent for staining while a label waits", async ({
    page,
  }) => {
    await openCase(page, seeded);
    await expect(primaryAction(page)).toHaveText("Send for staining");
    await expect(primaryAction(page)).toBeDisabled();
    await expect(primaryAction(page)).toHaveAttribute(
      "title",
      "Verify every slide label first (A1.1, A2.1 outstanding)",
    );
  });

  test("a slide label scanned against the wrong block is refused, naming both", async ({
    page,
  }) => {
    await openCase(page, seeded);
    await expandSection(page, MICROTOMY);
    const button = verifyLabelButton(page, "A1.1");
    const fields = scanFields(page, slideA11.id);

    await test.step("Verify label opens the check with the block field ready", async () => {
      await button.click();
      await expect(button).toHaveAttribute("aria-expanded", "true");
      await expect(verifyBlock(page, slideA11.id)).toBeVisible();
      await expect(verifyBlock(page, slideA11.id)).toContainText(
        "Print the slide labels, then scan the block and this slide's printed label before cutting.",
      );
      await expect(fields.block).toBeFocused();
    });

    await test.step("block A2 is scanned for slide A1.1, each read ending in Enter", async () => {
      // A scanner types the barcode and presses Enter, which moves on from
      // the block to the slide and confirms from the slide.
      await fields.block.pressSequentially(blockA2.barcode);
      await fields.block.press("Enter");
      await expect(fields.slide).toBeFocused();
      await fields.slide.pressSequentially(slideA11.barcode);
      const answer = verifyAnswer(page, slideA11.id);
      await fields.slide.press("Enter");
      await answer;
    });

    await test.step("an alert names both, and the slide stays unverified", async () => {
      // Carbon gives each text field an empty live region of its own, so the
      // notification is addressed as the one alert that is a notification.
      const alert = verifyBlock(page, slideA11.id).locator(
        '.cds--inline-notification--error[role="alert"]',
      );
      await expect(alert).toBeVisible();
      await expect(alert).toContainText("Label does not match block");
      await expect(alert).toContainText(
        `Slide label ${slideA11.barcode} does not match block ${blockA2.barcode}. The slide stays unverified.`,
      );
      await expect(fields.block).toHaveValue(blockA2.barcode);
      await expect(fields.slide).toHaveValue(slideA11.barcode);
      await expect(fields.slide).toBeFocused();
      const row = rowNamed(slidesUnder(page, "A1"), "1");
      await expect(labelTag(row)).toHaveText("Unverified");
    });

    await test.step("a fresh load still shows it unverified", async () => {
      await openCase(page, seeded);
      await expandSection(page, MICROTOMY);
      const row = rowNamed(slidesUnder(page, "A1"), "1");
      await expect(labelTag(row)).toHaveText("Unverified");
      await expect(primaryAction(page)).toHaveAttribute(
        "title",
        "Verify every slide label first (A1.1, A2.1 outstanding)",
      );
    });
  });

  test("the right block and slide verify the label in the signed-in user's name", async ({
    page,
  }) => {
    await openCase(page, seeded);
    await expandSection(page, MICROTOMY);
    const fields = scanFields(page, slideA11.id);

    await verifyLabelButton(page, "A1.1").click();
    await fields.block.fill(blockA1.barcode);
    await fields.slide.fill(slideA11.barcode);
    const confirm = verifyBlock(page, slideA11.id).getByRole("button", {
      name: /^Confirm match/,
    });
    await expect(confirm).toBeEnabled();
    const answer = verifyAnswer(page, slideA11.id);
    await confirm.click();
    await answer;

    const row = rowNamed(slidesUnder(page, "A1"), "1");
    await test.step("the tag reads Verified and names who checked it", async () => {
      await expect(labelTag(row)).toHaveText("Verified");
      await expect(verifiedToast(page, "A1.1")).not.toHaveCount(0, {
        timeout: UI_TIMEOUT,
      });
      await expect(verifiedBy(row)).toHaveAttribute(
        "title",
        new RegExp(`^Verified by ${await signedInName(page)} on .+`),
      );
      await expect(verifyBlock(page, slideA11.id)).toHaveCount(0);
      await expect(verifyLabelButton(page, "A1.1")).toHaveCount(0);
    });

    await test.step("focus moves on to the next label waiting", async () => {
      await expect(verifyLabelButton(page, "A2.1")).toBeFocused();
    });

    await test.step("a fresh load keeps it, and only A2.1 is left", async () => {
      await openCase(page, seeded);
      await expandSection(page, MICROTOMY);
      await expect(labelTag(rowNamed(slidesUnder(page, "A1"), "1"))).toHaveText(
        "Verified",
      );
      await expect(unverifiedBanner(page)).toContainText(
        "1 slide label to verify",
      );
      await expect(primaryAction(page)).toHaveAttribute(
        "title",
        "Verify every slide label first (A2.1 outstanding)",
      );
    });
  });

  test("once every label is verified, the case is sent for staining", async ({
    page,
  }) => {
    await openCase(page, seeded);
    await expandSection(page, MICROTOMY);
    const fields = scanFields(page, slideA21.id);

    await verifyLabelButton(page, "A2.1").click();
    await expect(fields.block).toBeFocused();
    await fields.block.pressSequentially(blockA2.barcode);
    await fields.block.press("Enter");
    await fields.slide.pressSequentially(slideA21.barcode);
    const answer = verifyAnswer(page, slideA21.id);
    await fields.slide.press("Enter");
    await answer;

    await expect(labelTag(rowNamed(slidesUnder(page, "A2"), "1"))).toHaveText(
      "Verified",
    );
    await expect(unverifiedBanner(page)).toHaveCount(0);
    // The last label checked, focus goes back to the section.
    await expect(sectionHeader(page, MICROTOMY)).toBeFocused();

    await advanceCase(page, { label: "Send for staining" });
    await expectStage(page, "Staining");
  });

  test("with no scanner, the identifiers as printed verify a slide", async ({
    page,
  }) => {
    await openCase(page, seeded);
    const caseId = seeded.pathologySampleId;

    // Returned to Microtomy for two more slides, one under each block: the
    // first is checked here, the second is left for the scan and screenshots
    // below.
    await moveCaseToStage(page, seeded, "MICROTOMY");
    await saveCaseThroughApi(page, caseId, await readCase(page, caseId), {
      addSlideUnder: blockA1.id,
    });
    await saveCaseThroughApi(page, caseId, await readCase(page, caseId), {
      addSlideUnder: blockA2.id,
    });
    const stored = await readCase(page, caseId);
    const added = labelled(slideUnder(stored.slides ?? [], blockA1.id, "2"));
    slideA22 = labelled(slideUnder(stored.slides ?? [], blockA2.id, "2"));

    try {
      await setSiteSetting(page, SCAN_VERIFICATION_SETTING, "false");
      // The browser reads the configuration once, when the application
      // boots, so a full load puts the setting in front of the screen.
      await openCase(page, seeded);
      await expandSection(page, MICROTOMY);

      await verifyLabelButton(page, "A1.2").click();
      const fields = scanFields(page, added.id);
      await expect(verifyBlock(page, added.id)).toContainText(
        "No scanner here: type the text printed under each barcode, before cutting.",
      );
      await expect(fields.block).toHaveAccessibleName(`Block ${added.barcode}`);
      await expect(fields.slide).toHaveAccessibleName(`Slide ${added.barcode}`);

      await fields.block.fill("A1");
      await fields.slide.fill("A1.2");
      const answer = verifyAnswer(page, added.id);
      await fields.slide.press("Enter");
      await answer;

      await expect(labelTag(rowNamed(slidesUnder(page, "A1"), "2"))).toHaveText(
        "Verified",
      );
      await expect(verifiedToast(page, "A1.2")).not.toHaveCount(0, {
        timeout: UI_TIMEOUT,
      });
      await expect(labelTag(rowNamed(slidesUnder(page, "A2"), "2"))).toHaveText(
        "Unverified",
      );
    } finally {
      await restoreScanSetting(page);
    }

    await test.step("with the setting back on, a check asks for the scan again", async () => {
      await openCase(page, seeded);
      await expandSection(page, MICROTOMY);
      await verifyLabelButton(page, "A2.2").click();
      await expect(scanFields(page, slideA22.id).block).toHaveAccessibleName(
        `Block barcode ${slideA22.barcode}`,
      );
    });
  });

  test("the slide labels print with each slide's barcode", async ({ page }) => {
    await openCase(page, seeded);
    await expandSection(page, MICROTOMY);

    const { url, text } = await printedLabelText(
      page,
      "Print all slide labels",
    );
    expect(url).toContain(
      `/LabelMakerServlet?labelType=slide&code=${encodeURIComponent(seeded.accessionNumber)}`,
    );
    expect(text).toContain(slideA11.barcode);
    expect(text).toContain(slideA21.barcode);
  });

  test("the microtomy section with a label check open passes an accessibility scan", async ({
    page,
  }, testInfo) => {
    await page.setViewportSize({ width: 1480, height: 900 });
    await openCase(page, seeded);
    await expandSection(page, MICROTOMY);
    await verifyLabelButton(page, "A2.2").click();
    await expect(
      page.locator(`#${MICROTOMY} .pathology-case-view__verify`),
    ).toBeVisible();

    // The shared header's notification bell is named "Notifications" while
    // its visible text is the unread count, which axe reports on every page
    // of the application. It is the layout's, not this section's.
    const scan = await new AxeBuilder({ page })
      .withTags(WCAG_21_AA_TAGS)
      .include(`#${MICROTOMY}`)
      .exclude("#notification-Icon")
      .analyze();
    const jsonPath = testInfo.outputPath(
      "axe-pathology-label-verification.json",
    );
    await writeFile(jsonPath, `${JSON.stringify(scan.violations, null, 2)}\n`);
    await testInfo.attach("axe-pathology-label-verification", {
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
    await verifyLabelButton(page, "A2.2").click();
    const check = page.locator(`#${MICROTOMY} .pathology-case-view__verify`);

    for (const width of [1480, 1280]) {
      await page.setViewportSize({ width, height: 900 });
      await expect(check).toBeVisible();
      await expect(unverifiedBanner(page)).toBeVisible();
      await sectionHeader(page, MICROTOMY).scrollIntoViewIfNeeded();

      await page.screenshot({
        path: new URL(
          `../../../../e2e-evidence/ogc-264-s9-label-verification-${width}.png`,
          import.meta.url,
        ).pathname,
        fullPage: true,
      });
    }
  });
});
