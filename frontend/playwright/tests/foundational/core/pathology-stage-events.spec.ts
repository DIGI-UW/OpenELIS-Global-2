import AxeBuilder from "@axe-core/playwright";
import { writeFile } from "fs/promises";
import type { Locator, Page } from "@playwright/test";
import { test, expect } from "../../../helpers/test-base";
import {
  apiGet,
  createPathologyCase,
  discoverPathologyOrderTarget,
  moveCaseOnce,
  moveCaseToStage,
  SeededPathologyCase,
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
  returnCase,
  returnControl,
  returnRow,
  saveDraft,
  setStageSwitch,
} from "../../../helpers/pathology-case-view";
import { UI_TIMEOUT } from "../../../helpers/timeouts";

/**
 * Moving a pathology case between bench stages.
 *
 * The case used to be moved by picking any stage from a menu and saving the
 * whole form: nothing checked that the bench had done the work of the stage
 * it left, any stage could be skipped or revisited without a word, and
 * nothing recorded who moved the case or when. Now the action bar's one
 * primary button sends the case to the next stage the laboratory tracks,
 * named on the button, and only once the stage it leaves is done; a return
 * goes one stage back and asks why; and every move is a row in the case's
 * stage history, with the operator and the time taken from the session and
 * the server clock, never typed.
 *
 * What only a deployed stack proves here:
 *   - the server refuses and the screen holds back the same moves, with the
 *     hint naming what is missing,
 *   - the history row names the person signed in and today's date on the
 *     server's own clock,
 *   - a page another person has overtaken is refused rather than moving the
 *     case twice, and is read again,
 *   - a stage the laboratory switches off is skipped by the move itself.
 *
 * Serial, and on one seeded case, because the walk is the point: the history
 * the later tests read is the one the earlier moves wrote.
 *
 * The walk passes Decalcification's flag, Processing, Embedding and
 * Microtomy, so those stages must be tracked on the stack (the shipped
 * default); the spec checks that first. It switches Embedding off in its
 * last test and puts it back as it found it.
 */

const API_PREFIX = "/api/OpenELIS-Global";

const CASE_INFO = "pathology-section-case-info";
const GROSSING = "pathology-section-grossing";
const PROCESSING = "pathology-section-processing";

/**
 * The column order of the case's stage history. An entry's completion is
 * not a column there: the move that completes it is the next row.
 */
const HISTORY_COLUMNS = {
  stage: 0,
  enteredAt: 1,
  movedBy: 2,
  reason: 3,
};

const RETURN_REASON = "Cassette A1 to be re-cut, block face incomplete";

const WCAG_21_AA_TAGS = ["wcag2a", "wcag2aa", "wcag21a", "wcag21aa"];

let seeded: SeededPathologyCase;
/** The run reference recorded at the first entry into Processing. */
const runReference = `RUN-${Date.now().toString(36).toUpperCase()}`;
/** The Embedding switch as the deployment had it, once read. */
let embeddingBefore: string | undefined;

/** The case's stage history, in Case Information. */
function stageHistory(page: Page) {
  return page
    .locator(`#${CASE_INFO}`)
    .getByRole("table", { name: "Stage history" });
}

/** The history's entries, oldest first. */
function historyRows(page: Page) {
  return stageHistory(page).locator(
    ".cds--structured-list-tbody .cds--structured-list-row",
  );
}

function historyCell(row: Locator, column: keyof typeof HISTORY_COLUMNS) {
  return row.locator(".cds--structured-list-td").nth(HISTORY_COLUMNS[column]);
}

/** The history's entries for one stage. */
function historyRowsOf(page: Page, stageLabel: string) {
  return historyRows(page).filter({
    has: page
      .locator(".cds--structured-list-td")
      .first()
      .filter({
        hasText: new RegExp(`^${stageLabel}`),
      }),
  });
}

/**
 * The name the server writes as the operator of a move: the signed-in
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

/**
 * A time written today on the server's own clock, in the deployment's date
 * format, to the minute.
 */
async function todayOnTheServer(page: Page) {
  const config = await apiGet<Record<string, string>>(
    page,
    `${API_PREFIX}/rest/open-configuration-properties`,
  );
  const today = config.currentDateAsText;
  if (!today) {
    throw new Error("open-configuration-properties reported no date");
  }
  return new RegExp(
    `^${today.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")} \\d{2}:\\d{2}$`,
  );
}

/** The newest history entry: a move to the stage, by this user, today. */
async function expectLatestMove(page: Page, stageLabel: string) {
  await expandSection(page, CASE_INFO);
  const latest = historyRows(page).last();
  await expect(historyCell(latest, "stage")).toHaveText(
    new RegExp(`^${stageLabel}`),
  );
  await expect(historyCell(latest, "movedBy")).toHaveText(
    await signedInName(page),
  );
  await expect(historyCell(latest, "enteredAt")).toHaveText(
    await todayOnTheServer(page),
  );
  return latest;
}

function description(page: Page) {
  return page.getByLabel("Macroscopic description", { exact: true });
}

function runReferenceField(page: Page) {
  return page.getByLabel("Processor run", { exact: true });
}

/** Tick or untick the grossing flag through its label, as a person does. */
async function toggleDecalcification(page: Page, checked: boolean) {
  await page.locator('label[for="decalcificationRequired"]').click();
  const box = page.getByRole("checkbox", { name: "Decalcification required" });
  if (checked) {
    await expect(box).toBeChecked();
  } else {
    await expect(box).not.toBeChecked();
  }
}

/** The primary is held, and says why, on its title and to a screen reader. */
async function expectPrimaryHeld(page: Page, reason: string | RegExp) {
  const button = primaryAction(page);
  await expect(button).toBeDisabled();
  await expect(button).toHaveAttribute("title", reason);
  await expect(button).toHaveAccessibleDescription(reason);
}

test.describe.serial("Pathology stage moves and their history", () => {
  // Order entry and the case view are both heavy screens, and a first visit
  // on a development server compiles them as it serves them.
  test.setTimeout(120_000);

  test.beforeAll(async ({ browser }) => {
    test.setTimeout(120_000);
    const context = await browser.newContext({
      storageState: "playwright/.auth/user.json",
    });
    const page = await context.newPage();
    await page.goto("/", { waitUntil: "domcontentloaded" });
    embeddingBefore = await readStageSwitch(page, "EMBEDDING");
    await expectStageSwitchesOn(page, [
      "DECALCIFICATION",
      "PROCESSING",
      "EMBEDDING",
      "MICROTOMY",
    ]);
    const target = await discoverPathologyOrderTarget(page);
    seeded = await createPathologyCase(page, target);
    await context.close();
  });

  // A test Playwright times out never reaches its own finally, and a stage
  // left switched off changes what every later pathology spec reads, so the
  // switch is put back from a hook as well.
  test.afterAll(async ({ browser }) => {
    if (embeddingBefore === undefined) {
      return;
    }
    const restored = embeddingBefore;
    const context = await browser.newContext({
      storageState: "playwright/.auth/user.json",
    });
    try {
      const page = await context.newPage();
      await page.goto("/", { waitUntil: "domcontentloaded" });
      await setStageSwitch(page, "EMBEDDING", restored);
    } finally {
      await context.close();
    }
  });

  test("Begin grossing moves a new case, and the history names who moved it", async ({
    page,
  }) => {
    await openCase(page, seeded);

    await test.step("a new case has no moves recorded and nothing to return to", async () => {
      await expandSection(page, CASE_INFO);
      await expect(
        page
          .locator(`#${CASE_INFO}`)
          .getByText("No stage changes have been recorded for this case yet."),
      ).toBeVisible();
      await expect(returnControl(page)).toBeDisabled();
      await expect(returnControl(page)).toHaveAttribute(
        "title",
        "No earlier stage to return to",
      );
    });

    await advanceCase(page, { label: "Begin grossing" });

    await test.step("the move is the history's one entry, by this user, today", async () => {
      await expectLatestMove(page, "Grossing");
      await expect(historyRows(page)).toHaveCount(1);
    });
  });

  test("at Grossing the primary waits for the description, a cassette and a save", async ({
    page,
  }) => {
    await openCase(page, seeded);

    await test.step("an empty grossing holds the case and says what is missing", async () => {
      await expect(primaryAction(page)).toHaveText("Send for processing");
      await expectPrimaryHeld(
        page,
        "Enter the macroscopic description and cut at least one cassette first",
      );
    });

    await test.step("unsaved work holds it, since a move reads the case again", async () => {
      await expandSection(page, GROSSING);
      await description(page).fill(
        "Received in formalin: an ellipse of skin, 2.1 x 0.9 cm.",
      );
      await expectPrimaryHeld(
        page,
        "Save or discard your changes before moving the case",
      );
      await page.getByRole("button", { name: "Add cassette" }).click();
    });

    await test.step("described, cut and saved, the case can be sent on", async () => {
      await saveDraft(page, seeded.pathologySampleId);
      await expect(primaryAction(page)).toHaveText("Send for processing");
      await expect(primaryAction(page)).toBeEnabled();
    });

    await test.step("the saved flag decides whether Decalcification comes next", async () => {
      await toggleDecalcification(page, true);
      await saveDraft(page, seeded.pathologySampleId);
      await expect(primaryAction(page)).toHaveText("Send for decalcification");

      await toggleDecalcification(page, false);
      await saveDraft(page, seeded.pathologySampleId);
      await expect(primaryAction(page)).toHaveText("Send for processing");
    });
  });

  test("Send for processing closes the grossing entry and opens the next", async ({
    page,
  }) => {
    await openCase(page, seeded);

    await advanceCase(page, { label: "Send for processing" });

    await expectLatestMove(page, "Processing");
    await expect(historyRows(page)).toHaveCount(2);
    const grossing = historyRows(page).first();
    await expect(historyCell(grossing, "stage")).toHaveText("Grossing");

    // The history shows an entry's completion as the next row; the record
    // itself carries it on the entry, by the same user at the same moment.
    const stored = await apiGet<{
      stageEvents: {
        stage: string;
        completedAt?: string;
        completedBy?: string;
      }[];
    }>(
      page,
      `${API_PREFIX}/rest/pathology/caseView/${seeded.pathologySampleId}`,
    );
    expect(stored.stageEvents.map((event) => event.stage)).toEqual([
      "GROSSING",
      "PROCESSING",
    ]);
    expect(stored.stageEvents[0].completedBy).toBe(await signedInName(page));
    expect(stored.stageEvents[0].completedAt).toMatch(
      await todayOnTheServer(page),
    );
    expect(stored.stageEvents[1].completedAt).toBeUndefined();
  });

  test("at Processing the run is recorded once and then read only", async ({
    page,
  }) => {
    await openCase(page, seeded);
    await expandSection(page, PROCESSING);

    await test.step("the section says plainly that no run register exists", async () => {
      await expect(
        page
          .locator(`#${PROCESSING}`)
          .getByText("Processor runs are not yet managed in OpenELIS"),
      ).toBeVisible();
      await expect(runReferenceField(page)).toBeEditable();
      await expect(runReferenceField(page)).toHaveValue("");
    });

    await test.step("the run typed in is kept by the save", async () => {
      await runReferenceField(page).fill(runReference);
      await saveDraft(page, seeded.pathologySampleId);
    });

    await test.step("read again, it is there and can no longer be changed", async () => {
      await openCase(page, seeded);
      await expandSection(page, PROCESSING);
      await expect(runReferenceField(page)).toHaveValue(runReference);
      await expect(runReferenceField(page)).not.toBeEditable();
      await expect(runReferenceField(page)).toHaveAccessibleDescription(
        "The run is recorded once. To correct it, return the case and send it to Processing again.",
      );
      await expect(
        page
          .locator(`#${PROCESSING}`)
          .getByRole("table", { name: "Processing" }),
      ).toContainText(runReference);
    });
  });

  test("a return asks for a reason, and the history shows it", async ({
    page,
  }) => {
    await openCase(page, seeded);

    await test.step("the return row will not go on without a reason", async () => {
      await returnControl(page).click();
      const row = returnRow(page);
      await expect(row).toBeVisible();
      await expect(row.getByRole("textbox", { name: /^Reason/ })).toBeFocused();
      const confirm = row.getByRole("button", { name: "Return to Grossing" });
      await expect(confirm).toBeDisabled();
      await expect(confirm).toHaveAttribute(
        "title",
        "Give a reason before returning the case",
      );
      await row.getByRole("button", { name: "Cancel", exact: true }).click();
      await expect(row).toHaveCount(0);
      await expect(returnControl(page)).toBeFocused();
    });

    await returnCase(page, RETURN_REASON);

    await test.step("the return is in the history, marked and with its reason", async () => {
      const latest = await expectLatestMove(page, "Grossing");
      await expect(historyCell(latest, "stage")).toContainText("Returned");
      await expect(historyCell(latest, "reason")).toHaveText(RETURN_REASON);
    });

    await test.step("sent on again, the case opens a new processing entry", async () => {
      await advanceCase(page, { label: "Send for processing" });
      await expectLatestMove(page, "Processing");
      await expect(historyRowsOf(page, "Processing")).toHaveCount(2);
      await expandSection(page, PROCESSING);
      // A new entry takes its own run; the first entry keeps the one it had.
      await expect(runReferenceField(page)).toHaveValue("");
      await expect(runReferenceField(page)).toBeEditable();
      await expect(
        page
          .locator(`#${PROCESSING}`)
          .getByRole("table", { name: "Processing" }),
      ).toContainText(runReference);
    });
  });

  test("the case at Processing with its history, at two desktop widths", async ({
    page,
  }) => {
    // Opened once and resized in place: a second load while the first one's
    // lookups are still in flight aborts them, and the browser logs each as a
    // failed fetch.
    await page.setViewportSize({ width: 1480, height: 900 });
    await openCase(page, seeded);
    await expandSection(page, CASE_INFO);
    await expandSection(page, PROCESSING);

    for (const width of [1480, 1280]) {
      await page.setViewportSize({ width, height: 900 });
      await expectStage(page, "Processing");
      await expect(historyRows(page)).toHaveCount(4);

      await page.screenshot({
        path: new URL(
          `../../../../e2e-evidence/ogc-264-s7-stage-events-${width}.png`,
          import.meta.url,
        ).pathname,
        fullPage: true,
      });
    }
  });

  test("a move made in another tab is refused here, and the case is read again", async ({
    page,
    browser,
  }) => {
    await openCase(page, seeded);
    await expect(primaryAction(page)).toHaveText("Send for embedding");
    await expect(primaryAction(page)).toBeEnabled();

    await test.step("someone else sends the case on from their own page", async () => {
      const other = await browser.newContext({
        storageState: "playwright/.auth/user.json",
      });
      try {
        const otherPage = await other.newPage();
        await otherPage.goto("/", { waitUntil: "domcontentloaded" });
        const now = await moveCaseOnce(
          otherPage,
          seeded.pathologySampleId,
          "advance",
          "PROCESSING",
        );
        expect(now).toBe("EMBEDDING");
      } finally {
        await other.close();
      }
    });

    await test.step("Send on this page names the conflict and shows the case as it is", async () => {
      const casePath = `/rest/pathology/caseView/${seeded.pathologySampleId}`;
      const refused = page.waitForResponse(
        (r) =>
          r.request().method() === "POST" &&
          new URL(r.url()).pathname.endsWith(`${casePath}/advance`),
      );
      const readBack = page.waitForResponse(
        (r) =>
          r.request().method() === "GET" &&
          new URL(r.url()).pathname.endsWith(casePath),
        { timeout: UI_TIMEOUT },
      );
      await primaryAction(page).click();
      await refused;
      await expect(
        page.locator(".cds--toast-notification--error").filter({
          hasText:
            "The case is no longer at the stage shown. It has been reloaded.",
        }),
      ).not.toHaveCount(0, { timeout: UI_TIMEOUT });
      await readBack;
      await expectStage(page, "Embedding");
    });

    await test.step("the case moved once, not twice", async () => {
      await expandSection(page, CASE_INFO);
      await expect(historyRowsOf(page, "Embedding")).toHaveCount(1);
      await expect(historyRows(page)).toHaveCount(5);
    });
  });

  test("at Embedding the primary names the cassette still waiting", async ({
    page,
  }) => {
    await openCase(page, seeded);

    await expect(primaryAction(page)).toHaveText("Send to microtomy");
    await expectPrimaryHeld(
      page,
      "Embed every cassette first (A1 outstanding)",
    );
    await expect(returnControl(page)).toBeEnabled();
  });

  test("with Embedding switched off, the case goes from Processing straight to Microtomy", async ({
    page,
  }) => {
    await openCase(page, seeded);
    await moveCaseToStage(page, seeded, "PROCESSING");

    try {
      await setStageSwitch(page, "EMBEDDING", "false");
      // The browser reads the configuration once, when the application
      // boots, so a full load puts the switch in front of the screen.
      await openCase(page, seeded);
      await expectStage(page, "Processing");
      await expect(railStep(page, "Embedding")).toContainText("N/A");
      await expect(primaryAction(page)).toHaveText("Send to microtomy");

      await advanceCase(page, { label: "Send to microtomy" });

      await expectLatestMove(page, "Microtomy");
      // The Embedding entry is the one the other tab wrote; skipping the
      // stage added none.
      await expect(historyRowsOf(page, "Embedding")).toHaveCount(1);
    } finally {
      await setStageSwitch(page, "EMBEDDING", embeddingBefore ?? "true");
    }
  });
  test("the case view at Processing, with the return row open, passes an accessibility scan", async ({
    page,
  }, testInfo) => {
    // Last, so a finding here leaves every move above verified. The case is
    // returned to Processing, where the run, the history and the return row
    // are all on screen at once.
    await page.setViewportSize({ width: 1480, height: 900 });
    await openCase(page, seeded);
    await moveCaseToStage(page, seeded, "PROCESSING");
    await openCase(page, seeded);
    await expandSection(page, CASE_INFO);
    await expandSection(page, PROCESSING);
    await returnControl(page).click();
    await expect(returnRow(page)).toBeVisible();

    await test.step("the whole page, for the record", async () => {
      // The shared header's notification bell is named "Notifications" while
      // its visible text is the unread count, which axe reports as
      // label-content-name-mismatch on every page of the application. It is
      // the layout's, not the case view's, and is left out here so the
      // record shows what belongs to this screen.
      const pageWide = await new AxeBuilder({ page })
        .withTags(WCAG_21_AA_TAGS)
        .exclude("#notification-Icon")
        .analyze();
      const jsonPath = testInfo.outputPath("axe-pathology-case-view.json");
      await writeFile(
        jsonPath,
        `${JSON.stringify(pageWide.violations, null, 2)}\n`,
      );
      await testInfo.attach("axe-pathology-case-view", {
        path: jsonPath,
        contentType: "application/json",
      });
    });

    await test.step("the case view's own markup has no serious or critical violation", async () => {
      // The shell's grid holds every part of the case view this feature
      // draws: the rail, the sections, the summary and the action bar. Read
      // at both widths, since what overflows depends on the width.
      const blocking = [];
      for (const width of [1480, 1280]) {
        await page.setViewportSize({ width, height: 900 });
        const scoped = await new AxeBuilder({ page })
          .withTags(WCAG_21_AA_TAGS)
          .include(".case-view__layout")
          .analyze();
        blocking.push(
          ...scoped.violations
            .filter(
              ({ impact }) => impact === "serious" || impact === "critical",
            )
            .map(({ id, impact, help, nodes }) => ({
              width,
              id,
              impact,
              help,
              targets: nodes.map((node) => node.target),
            })),
        );
      }
      expect(blocking, JSON.stringify(blocking, null, 2)).toEqual([]);
    });

    await returnRow(page)
      .getByRole("button", { name: "Cancel", exact: true })
      .click();
    await expect(returnRow(page)).toHaveCount(0);
  });
});
