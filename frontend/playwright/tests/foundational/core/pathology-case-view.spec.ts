import type { Page } from "@playwright/test";
import { test, expect } from "../../../helpers/test-base";
import {
  createPathologyCase,
  discoverPathologyOrderTarget,
  moveCaseToStage,
  PathologyOrderTarget,
  SeededPathologyCase,
} from "../../../helpers/seed-pathology-data";
import {
  addSlideTo,
  advanceCase,
  blockGroup,
  expandSection,
  expectStage,
  expectStageSwitchesOn,
  NUMBERED_SECTION_TITLES,
  openCase,
  overlaps,
  primaryAction,
  railStep,
  readStageSwitch,
  returnControl,
  saveDraft,
  sectionHeader,
  setStageSwitch,
  summaryRow,
} from "../../../helpers/pathology-case-view";
import { UI_TIMEOUT } from "../../../helpers/timeouts";

/**
 * The anatomic-pathology case view on the shared case-view shell.
 *
 * The screen used to be one flat form: every field of every bench step was on
 * it at once, whatever stage the case had actually reached, so a case just
 * accessioned offered a microscopy description and a report, and nothing on
 * the screen said which of them the bench was meant to be filling in. It is
 * now the shared shell: eleven numbered sections in bench order, each opened
 * or locked by the stage the case stands at, a progress rail down the left, a
 * case summary at the side and one action bar at the foot, whose primary
 * button names the stage the case is sent to next.
 *
 * What a deployed stack proves that the component tests cannot:
 *   - caseView.scss is actually loaded and applied, which no test that mounts
 *     a component in isolation can show,
 *   - the sticky summary and the action bar really render alongside the
 *     sections in the page's grid rather than only in a test renderer,
 *   - the accordion item ids the progress rail scrolls to resolve to real
 *     elements, which is only true once both halves are rendered by the same
 *     screen, and
 *   - the stage switches an administrator sets reach the case view, which
 *     spans the configuration endpoint, the server's choice of the next stage
 *     and the screen's own derivation.
 *
 * Serial, and on one seeded case. The case's stage is the input to almost
 * every assertion here, so the tests are written as one walk through it:
 * locked at Accessioned, sent to Grossing, worked there, then moved on.
 * Seeding a fresh case per test would hide the one thing the walk is for,
 * which is that the same case changes shape as it moves.
 *
 * The walk passes Microtomy and Staining and reads Coverslipping, so those
 * three stages must be tracked on the stack (the shipped default); the spec
 * checks that first and says so when one is off.
 */

/** The same eleven sections as the progress rail labels them. */
const RAIL_STEP_LABELS = NUMBERED_SECTION_TITLES.map((title) =>
  title.replace(/^\d+\. /, ""),
);

/** What a row shows in place of a fact nobody recorded. */
const NOT_RECORDED = "— not recorded";

/** The optional stage switched off in the last test. */
const UNTRACKED_STAGE = "COVERSLIPPING";
const UNTRACKED_STAGE_LABEL = "Coverslipping & QC";

let target: PathologyOrderTarget;
let seeded: SeededPathologyCase;
/**
 * The stage switch's value as the deployment had it before this spec ran,
 * once read; unset, nothing is put back.
 */
let untrackedStageBefore: string | undefined;

/**
 * The numbered title of each section, read from its own element rather than
 * from the heading button: a locked section's heading also carries the hint
 * naming the stage it waits for, so the button's whole text is title and
 * hint together.
 */
function sectionTitles(page: Page) {
  return page.locator(
    "li.cds--accordion__item .case-view__section-title > span:first-child",
  );
}

/** One row of the Case Information list, addressed by its label cell. */
function caseInformationRow(page: Page, label: string) {
  return page
    .locator("#pathology-section-case-info .cds--structured-list-row")
    .filter({ has: page.getByText(label, { exact: true }) });
}

function description(page: Page) {
  return page.getByLabel("Macroscopic description", { exact: true });
}

test.describe.serial("Pathology case view on the shared shell", () => {
  // The case view and the order entry it is seeded from are both heavy
  // screens, and a first visit on a development server compiles them as it
  // serves them, so the default per-test budget is not enough for a walk that
  // reloads the case between steps.
  test.setTimeout(120_000);

  test.beforeAll(async ({ browser }) => {
    test.setTimeout(120_000);
    const context = await browser.newContext({
      storageState: "playwright/.auth/user.json",
    });
    const page = await context.newPage();
    await page.goto("/", { waitUntil: "domcontentloaded" });
    untrackedStageBefore = await readStageSwitch(page, UNTRACKED_STAGE);
    await expectStageSwitchesOn(page, [
      "MICROTOMY",
      "STAINING",
      UNTRACKED_STAGE,
    ]);
    target = await discoverPathologyOrderTarget(page);
    seeded = await createPathologyCase(page, target);
    await context.close();
  });

  // A test Playwright times out never reaches its own finally, and a stage
  // left switched off makes the stage-switch spec fail on every later run, so
  // the deployment is put back to the value it was found at, from a hook.
  test.afterAll(async ({ browser }) => {
    if (untrackedStageBefore === undefined) {
      return;
    }
    const restored = untrackedStageBefore;
    const context = await browser.newContext({
      storageState: "playwright/.auth/user.json",
    });
    try {
      const page = await context.newPage();
      await page.goto("/", { waitUntil: "domcontentloaded" });
      await setStageSwitch(page, UNTRACKED_STAGE, restored);
    } finally {
      await context.close();
    }
  });

  test("opens a new case on the shell at Accessioned", async ({
    page,
  }, testInfo) => {
    await openCase(page, seeded);

    await test.step("the eleven sections are all there, in bench order", async () => {
      await expect(sectionTitles(page)).toHaveText(NUMBERED_SECTION_TITLES);
    });

    await test.step("the progress rail lists the same eleven", async () => {
      await expect(
        page.locator('nav[aria-label="Case progress"] .cds--progress-label'),
      ).toHaveText(RAIL_STEP_LABELS);
    });

    await test.step("the case summary says where the case stands", async () => {
      await expect(
        page.getByRole("heading", { name: "Case summary" }),
      ).toBeVisible();
      // The label is the other half of the row, so the stage is what the row
      // adds to it.
      await expect(summaryRow(page, "Stage")).toContainText("Accessioned");
    });

    await test.step("the action bar offers a discard nothing can be discarded from", async () => {
      await expect(
        page.getByRole("button", { name: "Discard changes" }),
      ).toBeDisabled();
      await expect(
        page.getByRole("button", { name: "Save draft" }),
      ).toBeEnabled();
    });

    await test.step("the primary begins grossing, and there is nothing to return to", async () => {
      await expect(primaryAction(page)).toHaveText("Begin grossing");
      await expect(primaryAction(page)).toBeEnabled();
      await expect(returnControl(page)).toBeDisabled();
      await expect(returnControl(page)).toHaveAttribute(
        "title",
        "No earlier stage to return to",
      );
    });

    await test.step("the patient header carries the stage as a badge", async () => {
      await expect(
        page.locator(".cds--tag").filter({ hasText: "Accessioned" }),
      ).toBeVisible();
    });

    // For a human to check that caseView.scss loaded: the sticky summary, the
    // action bar's top border and the section header rows. Nothing about
    // styling is asserted here.
    await page.screenshot({
      path: testInfo.outputPath("case-view-accessioned.png"),
      fullPage: true,
    });
  });

  test("case information is collapsed and shows the specimen facts", async ({
    page,
  }) => {
    await openCase(page, seeded);

    const header = sectionHeader(page, "pathology-section-case-info");
    await expect(header).toHaveAttribute("aria-expanded", "false");

    await header.click();
    await expect(header).toHaveAttribute("aria-expanded", "true");

    await expect(caseInformationRow(page, "Lab Number")).toContainText(
      seeded.accessionNumber,
    );
    await expect(caseInformationRow(page, "Received Date")).toContainText(
      /\d{4}-\d{2}-\d{2}/,
    );
    await expect(caseInformationRow(page, "Sample Type")).toContainText(
      target.sampleTypeName,
    );
    // A pathologist is assigned only when the case reaches Ready for
    // Pathologist, so on a case still at Accessioned this row is empty by the
    // workflow's own rule, and an unrecorded fact is shown as an absence
    // rather than as an empty cell.
    await expect(
      caseInformationRow(page, "Pathologist Assigned"),
    ).toContainText(NOT_RECORDED);
  });

  test("sections ahead of the case are locked with the stage they wait for", async ({
    page,
  }) => {
    await openCase(page, seeded);

    const grossing = sectionHeader(page, "pathology-section-grossing");
    await expect(grossing).toBeDisabled();
    await expect(grossing).toContainText(
      "Available once the case reaches Grossing",
    );

    await expect(sectionHeader(page, "pathology-section-review")).toContainText(
      "Available once the case reaches Ready for Pathologist",
    );

    // Locked, never removed: the workflow's shape must read the same whatever
    // stage the case stands at.
    await expect(sectionTitles(page)).toHaveText(NUMBERED_SECTION_TITLES);
  });

  test("Begin grossing sends the case to Grossing and unlocks the bench section", async ({
    page,
  }, testInfo) => {
    await openCase(page, seeded);

    await advanceCase(page, { label: "Begin grossing" });

    await test.step("the move is stored at once, so nothing waits to be saved", async () => {
      await expect(page.getByText("Unsaved changes")).toHaveCount(0);
      await expect(
        page.getByRole("button", { name: "Discard changes" }),
      ).toBeDisabled();
    });

    await test.step("the grossing section is workable", async () => {
      const grossing = sectionHeader(page, "pathology-section-grossing");
      await expect(grossing).toBeEnabled();
      await expect(grossing).not.toContainText(
        "Available once the case reaches Grossing",
      );
      await expandSection(page, "pathology-section-grossing");
    });

    await test.step("the rail and the summary both follow the stage", async () => {
      await expect(railStep(page, "Grossing")).toHaveClass(
        /cds--progress-step--current/,
      );
      await expectStage(page, "Grossing");
    });

    await page.screenshot({
      path: testInfo.outputPath("case-view-grossing.png"),
      fullPage: true,
    });
  });

  test("discard changes reloads the saved case", async ({ page }) => {
    await openCase(page, seeded);
    await expandSection(page, "pathology-section-grossing");

    await description(page).fill("Not to be kept");
    await expect(page.getByText("Unsaved changes")).toBeVisible();
    // Sending the case on re-reads it, which would drop the typed words, so
    // the primary waits for them to be saved or discarded.
    await expect(primaryAction(page)).toBeDisabled();
    await expect(primaryAction(page)).toHaveAttribute(
      "title",
      "Save or discard your changes before moving the case",
    );

    await page.getByRole("button", { name: "Discard changes" }).click();

    await expect(description(page)).toHaveValue("", { timeout: UI_TIMEOUT });
    await expect(page.getByText("Unsaved changes")).toHaveCount(0);
    await expectStage(page, "Grossing");
  });

  test("the stage the case was sent to is stored, and the case reloads at it", async ({
    page,
  }) => {
    await openCase(page, seeded);

    await expectStage(page, "Grossing");
    await expect(
      page.locator(".cds--tag").filter({ hasText: "Grossing" }),
    ).toBeVisible();

    const grossing = sectionHeader(page, "pathology-section-grossing");
    await expect(grossing).toBeEnabled();
    // Opened on arrival this time: a case whose current stage is grossing puts
    // the person who opens it straight into the work waiting for them.
    await expect(grossing).toHaveAttribute("aria-expanded", "true");
  });

  test("a slide row lays its controls out side by side, not over one another", async ({
    page,
  }, testInfo) => {
    await openCase(page, seeded);

    // A slide is added under the block it was cut from, and only a block the
    // server has already saved is listed, so the walk saves a cassette first.
    await expandSection(page, "pathology-section-grossing");
    await page.getByRole("button", { name: "Add cassette" }).click();
    await saveDraft(page, seeded.pathologySampleId);

    // The stages between are not what this test is about, so the case is
    // taken there through the move endpoints, which embed the cassette on the
    // way, and opened again to show it.
    await moveCaseToStage(page, seeded, "MICROTOMY");
    await openCase(page, seeded);
    await expandSection(page, "pathology-section-microtomy");
    await addSlideTo(page, "A1");

    // The one slide under A1, still unsaved and so not yet named.
    const row = blockGroup(page, "A1").locator(".pathology-case-view__row");
    await expect(row).toBeVisible();

    // The section bodies were moved onto the shell carrying the sixteen-column
    // Carbon Grid rows the flat form laid them out with, inside a centre
    // column that is nine of those sixteen. The columns wrapped and their
    // controls were drawn on top of one another. A new slide row now carries
    // its identity line, its level and stain, the location and its actions;
    // only a browser can show that they still sit apart, so only this can
    // catch an overlap coming back.
    const controls = [
      row.locator(".pathology-case-view__row-identity"),
      row.getByLabel("Level New slide 1", { exact: true }),
      row.getByRole("combobox", { name: "Stain New slide 1", exact: true }),
      // Carbon's file uploader renders its label and its own trigger as two
      // buttons of the same name; the first is the one a technician sees.
      row.getByRole("button", { name: "Upload file" }).first(),
      // Named after the row it drops, "Remove New slide 1".
      row.getByRole("button", { name: /^Remove/ }),
    ];
    const boxes = [];
    for (const control of controls) {
      await expect(control).toBeVisible();
      const box = await control.boundingBox();
      expect(box).not.toBeNull();
      boxes.push(box!);
    }

    for (let first = 0; first < boxes.length; first += 1) {
      for (let second = first + 1; second < boxes.length; second += 1) {
        expect(
          overlaps(boxes[first], boxes[second]),
          `controls ${first} and ${second} overlap`,
        ).toBe(false);
      }
    }

    // Carbon's file uploader renders an empty description paragraph above
    // its button, with a margin, which set Upload file a line below the
    // button beside it; the two share a top edge only while that paragraph
    // is hidden.
    expect(
      Math.abs(boxes[3].y - boxes[4].y),
      "Upload file and Remove sit on one line",
    ).toBeLessThanOrEqual(2);

    // For a human to look at what the assertion above only measures.
    await page.screenshot({
      path: testInfo.outputPath("case-view-slide-row.png"),
      fullPage: true,
    });
  });

  test("the rail navigates to a section", async ({ page }) => {
    // A fixed window, the long Case Information section open and the page
    // at its top, so the last section starts out of view whatever window the
    // run was given and however many sections the stage opens on arrival.
    await page.setViewportSize({ width: 1280, height: 720 });
    await openCase(page, seeded);
    await expandSection(page, "pathology-section-case-info");
    await page.evaluate(() => window.scrollTo(0, 0));

    const reports = page.locator("#pathology-section-reports");
    // The last of eleven sections is below the fold on arrival, so the rail
    // is what brings it into view and the assertion below is not already
    // satisfied before the click.
    await expect(reports).not.toBeInViewport();

    await railStep(page, "Reports").getByRole("button").click();

    await expect(reports).toBeInViewport({ timeout: UI_TIMEOUT });
  });

  test("a stage the laboratory does not track is skipped when the case is sent on", async ({
    page,
  }) => {
    // Opened first because the calls below run inside the browser.
    await openCase(page, seeded);
    await moveCaseToStage(page, seeded, "STAINING");
    await openCase(page, seeded);

    const coverslippingStep = railStep(page, UNTRACKED_STAGE_LABEL);
    await expect(coverslippingStep).toHaveCount(1);
    await expect(coverslippingStep).not.toContainText("N/A");
    await expect(primaryAction(page)).toHaveText("Send for coverslipping");

    try {
      await setStageSwitch(page, UNTRACKED_STAGE, "false");
      // The browser reads the configuration once, when the application boots,
      // so a full page load is what puts the new switch in front of the
      // screen. A client-side route change would not.
      await openCase(page, seeded);

      await expect(railStep(page, UNTRACKED_STAGE_LABEL)).toContainText("N/A");
      await expect(
        sectionHeader(page, "pathology-section-coverslipping"),
      ).toContainText("Not tracked at this laboratory");
      await expect(primaryAction(page)).toHaveText("Send to pathologist");
    } finally {
      await setStageSwitch(
        page,
        UNTRACKED_STAGE,
        untrackedStageBefore ?? "true",
      );
    }

    await openCase(page, seeded);
    await expect(primaryAction(page)).toHaveText("Send for coverslipping");
    await expect(railStep(page, UNTRACKED_STAGE_LABEL)).toHaveCount(1);
    await expect(railStep(page, UNTRACKED_STAGE_LABEL)).not.toContainText(
      "N/A",
    );
  });
});
