import type { Page } from "@playwright/test";
import { test, expect } from "../../../helpers/test-base";
import {
  createPathologyCase,
  discoverPathologyOrderTarget,
  PathologyOrderTarget,
  SeededPathologyCase,
} from "../../../helpers/seed-pathology-data";
import { NAV_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";

/**
 * The anatomic-pathology case view on the shared case-view shell.
 *
 * The screen used to be one flat form: every field of every bench step was on
 * it at once, whatever stage the case had actually reached, so a case just
 * accessioned offered a microscopy description and a report, and nothing on
 * the screen said which of them the bench was meant to be filling in. It is
 * now the shared shell: eleven numbered sections in bench order, each opened
 * or locked by the stage the case stands at, a progress rail down the left, a
 * case summary at the side and one action bar at the foot.
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
 *     spans the configuration endpoint, the browser's configuration load and
 *     the screen's own derivation.
 *
 * Serial, and on one seeded case. The case's stage is the input to almost
 * every assertion here, so the tests are written as one walk through it:
 * locked at Accessioned, unlocked at Grossing, saved at Grossing. Seeding a
 * fresh case per test would hide the one thing the walk is for, which is that
 * the same case changes shape as it moves.
 */

/** The accordion's own heading text, section number and title together. */
const NUMBERED_SECTION_TITLES = [
  "1. Case Information",
  "2. Grossing",
  "3. Decalcification",
  "4. Processing",
  "5. Embedding",
  "6. Microtomy",
  "7. Staining",
  "8. Coverslipping & QC",
  "9. Pathologist Review",
  "10. Findings & Conclusion",
  "11. Reports",
];

/** The same eleven sections as the progress rail labels them. */
const RAIL_STEP_LABELS = NUMBERED_SECTION_TITLES.map((title) =>
  title.replace(/^\d+\. /, ""),
);

/** What a row shows in place of a fact nobody recorded. */
const NOT_RECORDED = "— not recorded";

/** The optional stage switched off in the last test. */
const UNTRACKED_STAGE = "COVERSLIPPING";
const UNTRACKED_STAGE_LABEL = "Coverslipping & QC";

/**
 * Everything the stage control offers: the eleven bench stages, in bench
 * order. Asserting the whole list at once is what makes the switched-off case
 * below meaningful, since it proves the one stage left out and every other
 * stage still there in a single assertion.
 */
const STATUS_OPTIONS = [
  "Accessioned",
  "Grossing",
  "Decalcification",
  "Processing",
  "Embedding",
  "Microtomy",
  "Staining",
  UNTRACKED_STAGE_LABEL,
  "Ready for Pathologist",
  "Under Pathologist Review",
  "Completed",
];

const STATUS_OPTIONS_WITHOUT_UNTRACKED = STATUS_OPTIONS.filter(
  (option) => option !== UNTRACKED_STAGE_LABEL,
);

const API_PREFIX = "/api/OpenELIS-Global";

let target: PathologyOrderTarget;
let seeded: SeededPathologyCase;
/** The stage switch's value as the deployment had it before this spec ran. */
let untrackedStageBefore = "true";

/** Open the seeded case and wait until the shell has rendered. */
async function openCase(page: Page) {
  await page.goto(`/PathologyCaseView/${seeded.pathologySampleId}`, {
    waitUntil: "domcontentloaded",
  });
  // The heading is on the page before the case is, carrying only that fixed
  // prefix, so the load is waited on it and the lab number is asserted below.
  const heading = page.getByRole("heading", {
    name: /^Pathology Case\b/,
    level: 3,
  });
  await expect(heading).toBeVisible({ timeout: NAV_TIMEOUT });
  // The sections are rendered only once the case itself has arrived.
  await expect(sectionHeaders(page)).toHaveCount(
    NUMBERED_SECTION_TITLES.length,
    { timeout: NAV_TIMEOUT },
  );
  // The lab number the heading has gained by now is what says the case on
  // screen is the seeded one, which the prefix alone cannot.
  await expect(heading).toContainText(seeded.accessionNumber);
}

/** Every accordion heading, in the order the accordion holds them. */
function sectionHeaders(page: Page) {
  return page.locator("button.cds--accordion__heading");
}

/**
 * The heading of one section, addressed by the accordion item id the screen
 * gives it, which is also what the progress rail scrolls to.
 */
function sectionHeader(page: Page, sectionId: string) {
  return page.locator(`#${sectionId} button.cds--accordion__heading`);
}

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

/** One row of the case summary, addressed by the label in its first half. */
function summaryRow(page: Page, label: string) {
  return page.locator(".case-view__summary-row").filter({
    has: page.locator(".case-view__summary-label", {
      hasText: new RegExp(`^${label}$`),
    }),
  });
}

/** One row of the Case Information list, addressed by its label cell. */
function caseInformationRow(page: Page, label: string) {
  return page
    .locator("#pathology-section-case-info .cds--structured-list-row")
    .filter({ has: page.getByText(label, { exact: true }) });
}

/** One step of the progress rail, addressed by its label. */
function railStep(page: Page, label: string) {
  return page
    .locator('nav[aria-label="Case progress"] li.cds--progress-step')
    .filter({
      has: page.locator(".cds--progress-label", {
        hasText: new RegExp(`^${label}$`),
      }),
    });
}

/** Whether two rendered boxes cover any of the same pixels. */
function overlaps(
  first: { x: number; y: number; width: number; height: number },
  second: { x: number; y: number; width: number; height: number },
) {
  return (
    first.x < second.x + second.width &&
    second.x < first.x + first.width &&
    first.y < second.y + second.height &&
    second.y < first.y + first.height
  );
}

/**
 * The stage control in the action bar. It is a Carbon Dropdown rather than a
 * native select, because the bar sits at the foot of a long page where a
 * native menu opens downward and the browser clips it; the menu is markup in
 * the page, opened by its own toggle, and its options exist only while it is
 * open.
 */
function stageControl(page: Page) {
  return page.getByRole("combobox", { name: "Status" });
}

/**
 * The stage menu's options, with the menu opened.
 *
 * The menu is asserted to be drawn above its own toggle, which is the whole
 * point of the control: the action bar is at the foot of a long page, and the
 * native select this replaced opened downward, where the window clipped it.
 * Measured rather than read off a class name, because the class is Carbon's
 * and the clipping was the browser's.
 */
async function openStageMenu(page: Page) {
  const toggle = stageControl(page);
  await toggle.click();
  const options = page.locator('#status [role="option"]');
  await expect(options.first()).toBeVisible({ timeout: UI_TIMEOUT });

  const toggleBox = await toggle.boundingBox();
  const menuBox = await page
    .locator("#status .cds--list-box__menu")
    .boundingBox();
  expect(toggleBox).not.toBeNull();
  expect(menuBox).not.toBeNull();
  expect(
    menuBox!.y + menuBox!.height,
    "the stage menu is drawn above its toggle",
  ).toBeLessThanOrEqual(toggleBox!.y + 1);

  return options;
}

/** Move the case to a stage by name, exactly as a technician would. */
async function setStage(page: Page, label: string) {
  const options = await openStageMenu(page);
  await options.filter({ hasText: new RegExp(`^${label}$`) }).click();
  await expect(stageControl(page)).toContainText(label);
}

/**
 * Read a configuration row through the browser session, and throw with the
 * body when the deployment refuses, so a failed setup says so rather than
 * leaving a test to fail on an unexplained assertion.
 */
async function apiGet<T>(page: Page, path: string): Promise<T> {
  const result = await page.evaluate(async (p) => {
    const csrf = localStorage.getItem("CSRF") || "";
    const res = await fetch(p, {
      credentials: "include",
      headers: { Accept: "application/json", "X-CSRF-Token": csrf },
    });
    return { status: res.status, text: await res.text() };
  }, path);
  if (result.status < 200 || result.status >= 300) {
    throw new Error(`GET ${path}: HTTP ${result.status}: ${result.text}`);
  }
  return JSON.parse(result.text) as T;
}

/**
 * Turn one optional bench stage on or off exactly as the Result Entry
 * Configuration page does: read the configuration row the administrator would
 * select, change its value, and post it back to the same endpoint. The server
 * reloads its own configuration as part of that save.
 */
async function readStageSwitch(page: Page, stage: string) {
  const menu = await apiGet<{ menuList: { id: string; name: string }[] }>(
    page,
    `${API_PREFIX}/rest/ResultConfigurationMenu`,
  );
  const switchName = `pathology.stage.${stage}.enabled`;
  const row = menu.menuList.find((entry) => entry.name === switchName);
  if (!row) {
    throw new Error(`No configuration row named ${switchName}`);
  }
  const stored = await apiGet<Record<string, unknown>>(
    page,
    `${API_PREFIX}/rest/ResultConfiguration?ID=${row.id}`,
  );
  return { row, stored, switchName };
}

async function setStageSwitch(page: Page, stage: string, value: string) {
  const { row, stored, switchName } = await readStageSwitch(page, stage);
  const saved = await page.evaluate(
    async ({ path, data }) => {
      const csrf = localStorage.getItem("CSRF") || "";
      const res = await fetch(path, {
        method: "POST",
        credentials: "include",
        headers: {
          "Content-Type": "application/json",
          "X-CSRF-Token": csrf,
        },
        body: JSON.stringify(data),
      });
      return { status: res.status, text: (await res.text()).slice(0, 400) };
    },
    {
      path: `${API_PREFIX}/rest/ResultConfiguration?ID=${row.id}`,
      data: { ...stored, value },
    },
  );
  if (saved.status < 200 || saved.status >= 300) {
    throw new Error(
      `Setting ${switchName} to ${value} failed: HTTP ${saved.status}: ${saved.text}`,
    );
  }
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
    untrackedStageBefore = String(
      (await readStageSwitch(page, UNTRACKED_STAGE)).stored.value ?? "true",
    );
    target = await discoverPathologyOrderTarget(page);
    seeded = await createPathologyCase(page, target);
    await context.close();
  });

  // A test Playwright times out never reaches its own finally, and a stage
  // left switched off makes the stage-switch spec fail on every later run, so
  // the deployment is put back to the value it was found at, from a hook.
  test.afterAll(async ({ browser }) => {
    const context = await browser.newContext({
      storageState: "playwright/.auth/user.json",
    });
    try {
      const page = await context.newPage();
      await page.goto("/", { waitUntil: "domcontentloaded" });
      await setStageSwitch(page, UNTRACKED_STAGE, untrackedStageBefore);
    } finally {
      await context.close();
    }
  });

  test("opens a new case on the shell at Accessioned", async ({
    page,
  }, testInfo) => {
    await openCase(page);

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
    await openCase(page);

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
    await openCase(page);

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

  test("moving the case to Grossing unlocks the bench section and marks the form dirty", async ({
    page,
  }, testInfo) => {
    await openCase(page);

    await setStage(page, "Grossing");

    await test.step("the bar says the case is unsaved", async () => {
      await expect(page.getByText("Unsaved changes")).toBeVisible();
      await expect(
        page.getByRole("button", { name: "Discard changes" }),
      ).toBeEnabled();
    });

    await test.step("the grossing section is workable", async () => {
      const grossing = sectionHeader(page, "pathology-section-grossing");
      await expect(grossing).toBeEnabled();
      await expect(grossing).not.toContainText(
        "Available once the case reaches Grossing",
      );
      // A section that unlocks while the case is open is not thrown open:
      // Carbon re-syncs an accordion item whenever its open prop changes, so
      // opening it here would equally snap shut whatever the user had chosen
      // to expand. It is opened by the person who now has work to do in it.
      await grossing.click();
      await expect(grossing).toHaveAttribute("aria-expanded", "true");
    });

    await test.step("the rail and the summary both follow the status control", async () => {
      await expect(railStep(page, "Grossing")).toHaveClass(
        /cds--progress-step--current/,
      );
      await expect(summaryRow(page, "Stage")).toContainText("Grossing");
    });

    await page.screenshot({
      path: testInfo.outputPath("case-view-grossing.png"),
      fullPage: true,
    });
  });

  test("discard changes reloads the saved case", async ({ page }) => {
    await openCase(page);

    await setStage(page, "Grossing");
    await expect(summaryRow(page, "Stage")).toContainText("Grossing");
    await expect(page.getByText("Unsaved changes")).toBeVisible();

    await page.getByRole("button", { name: "Discard changes" }).click();

    await expect(summaryRow(page, "Stage")).toContainText("Accessioned", {
      timeout: UI_TIMEOUT,
    });
    await expect(page.getByText("Unsaved changes")).toHaveCount(0);
    await expect(stageControl(page)).toContainText("Accessioned");
  });

  test("save draft persists the stage and the case reloads at it", async ({
    page,
  }) => {
    await openCase(page);

    await setStage(page, "Grossing");
    await expect(page.getByText("Unsaved changes")).toBeVisible();

    await page.getByRole("button", { name: "Save draft" }).click();

    await expect(page.getByText("Unsaved changes")).toHaveCount(0, {
      timeout: UI_TIMEOUT,
    });

    await openCase(page);

    await expect(summaryRow(page, "Stage")).toContainText("Grossing");
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
    await openCase(page);
    await setStage(page, "Microtomy");

    const microtomy = sectionHeader(page, "pathology-section-microtomy");
    await expect(microtomy).toBeEnabled();
    await microtomy.click();

    await page.getByRole("button", { name: "Add Slide(s)" }).click();

    const row = page
      .locator("#pathology-section-microtomy .pathology-case-view__row")
      .first();
    await expect(row).toBeVisible();

    // The section bodies were moved onto the shell carrying the sixteen-column
    // Carbon Grid rows the flat form laid them out with, inside a centre
    // column that is nine of those sixteen. The columns wrapped and their
    // controls were drawn on top of one another: on a slide, Upload file over
    // Print Label. Only a browser can show it, so only this can catch it
    // coming back.
    const controls = [
      // Carbon's file uploader renders its label and its own trigger as two
      // buttons of the same name; the first is the one a technician sees.
      row.getByRole("button", { name: "Upload file" }).first(),
      row.getByRole("button", { name: "Print Label" }),
      row.getByRole("button", { name: "Remove Slide" }),
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
    // its button, with a margin, which set Upload file a line below Print
    // Label; the two share a top edge only while that paragraph is hidden.
    expect(
      Math.abs(boxes[0].y - boxes[1].y),
      "Upload file and Print Label sit on one line",
    ).toBeLessThanOrEqual(2);

    // For a human to look at what the assertion above only measures.
    await page.screenshot({
      path: testInfo.outputPath("case-view-slide-row.png"),
      fullPage: true,
    });
  });

  test("the rail navigates to a section", async ({ page }) => {
    await openCase(page);

    const reports = page.locator("#pathology-section-reports");
    // The last of eleven sections is below the fold on arrival, so the rail
    // is what brings it into view and the assertion below is not already
    // satisfied before the click.
    await expect(reports).not.toBeInViewport();

    await railStep(page, "Reports").getByRole("button").click();

    await expect(reports).toBeInViewport({ timeout: UI_TIMEOUT });
  });

  test("a stage the laboratory does not track shows as not applicable", async ({
    page,
  }) => {
    // Opened first both to establish what a tracked stage looks like and
    // because the configuration calls below run inside the browser.
    await openCase(page);
    const coverslippingStep = railStep(page, UNTRACKED_STAGE_LABEL);
    await expect(coverslippingStep).toHaveCount(1);
    await expect(coverslippingStep).not.toContainText("N/A");

    try {
      await setStageSwitch(page, UNTRACKED_STAGE, "false");
      // The browser reads the configuration once, when the application boots,
      // so a full page load is what puts the new switch in front of the
      // screen. A client-side route change would not.
      await openCase(page);

      await expect(railStep(page, UNTRACKED_STAGE_LABEL)).toContainText("N/A");
      await expect(
        sectionHeader(page, "pathology-section-coverslipping"),
      ).toContainText("Not tracked at this laboratory");

      await expect(await openStageMenu(page)).toHaveText(
        STATUS_OPTIONS_WITHOUT_UNTRACKED,
      );
    } finally {
      await setStageSwitch(page, UNTRACKED_STAGE, untrackedStageBefore);
    }

    await openCase(page);
    await expect(await openStageMenu(page)).toHaveText(STATUS_OPTIONS);
    await expect(railStep(page, UNTRACKED_STAGE_LABEL)).toHaveCount(1);
    await expect(railStep(page, UNTRACKED_STAGE_LABEL)).not.toContainText(
      "N/A",
    );
  });
});
