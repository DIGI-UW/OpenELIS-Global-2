import type { Locator, Page } from "@playwright/test";
import { test, expect } from "../../../helpers/test-base";
import {
  createPathologyCase,
  discoverPathologyOrderTarget,
  ensureTissueType,
  SeededPathologyCase,
  TissueType,
} from "../../../helpers/seed-pathology-data";
import {
  advanceCase,
  cassetteRows,
  expandSection,
  expectStageSwitchesOn,
  openCase,
  primaryAction,
  railStep,
  returnCase,
  rowNamed,
  saveDraft,
  sectionHeader,
  summaryRow,
} from "../../../helpers/pathology-case-view";
import { chooseCarbonOption } from "../../../helpers/carbon-select";

/**
 * The grossing section of the pathology case view records what the bench did
 * with the specimen (FR-3): the macroscopic description, whether the specimen
 * needs decalcifying, and the cassettes cut from it with the tissue each
 * holds. The decalcification flag is also what decides whether the
 * Decalcification stage applies to the case at all (FR-3.2 driving FR-4.1).
 *
 * What only a deployed stack proves here:
 *   - a description of several lines is stored whole and comes back with its
 *     line breaks, which the old 255-character column could not hold,
 *   - the flag and the tissue type survive a fresh load, so they were stored
 *     rather than kept by the form,
 *   - the tissue types offered are the laboratory's own dictionary entries,
 *     served by the server, and the one picked is what the cassette keeps,
 *   - the Decalcification section and its rail step follow the stored flag,
 *     and a case standing at that stage is never locked out of it.
 *
 * Serial, and on one seeded case, because the walk is the point: the flag
 * set at grossing is what opens Decalcification later, and the cassettes cut
 * early are what the last screenshots show.
 *
 * The Decalcification and Processing stages must be tracked on the stack
 * (their stage switches on, the shipped default); with Decalcification off
 * the section says so instead and every assertion about the flag here would
 * be about the switch, so the spec checks both first.
 */

let seeded: SeededPathologyCase;
let tissue: TissueType;

const DECALCIFICATION = "pathology-section-decalcification";
const GROSSING = "pathology-section-grossing";

/** The summary row the flag is shown in. */
const FLAG_ROW = "Decalcification required";

/**
 * Longer than the 255 characters the description column used to hold, and
 * over several lines, so a stored and reloaded value proves the column and
 * the line breaks both survive.
 */
const DESCRIPTION =
  "Received in formalin, labelled with the patient's name and site: an ellipse of skin, 3.0 x 1.2 cm, excised to a depth of 0.8 cm.\n" +
  "The epidermal surface is tan with a central, irregular, pigmented lesion of 0.6 x 0.5 cm, lying 0.4 cm from the nearest lateral margin.\n" +
  "The deep and peripheral margins are inked blue, and the specimen is serially sectioned at 2 mm intervals.\n" +
  "Submitted entirely: the lesion in cassette A1 and both tips in cassette A2.";

/**
 * A row's tissue picker. Its accessible name is the visible title followed
 * by the row's own name ("Tissue type A1", "Tissue type New cassette 1"), so
 * each row's picker is addressed without relying on the order of the rows.
 */
function tissuePicker(page: Page, rowName: string) {
  return page.getByRole("combobox", {
    name: `Tissue type ${rowName}`,
    exact: true,
  });
}

function description(page: Page) {
  return page.getByLabel("Macroscopic description", { exact: true });
}

function decalcificationBox(page: Page) {
  return page.getByRole("checkbox", { name: "Decalcification required" });
}

/**
 * Toggle the flag through its label, as a person clicks it: Carbon draws the
 * box over a visually hidden input, which never takes the pointer itself.
 */
async function toggleDecalcification(page: Page, checked: boolean) {
  await page.locator('label[for="decalcificationRequired"]').click();
  if (checked) {
    await expect(decalcificationBox(page)).toBeChecked();
  } else {
    await expect(decalcificationBox(page)).not.toBeChecked();
  }
}

/** The badge in the grossing section's header. */
function grossingBadge(page: Page) {
  return sectionHeader(page, GROSSING).locator(".cds--tag");
}

/**
 * Each of the controls drawn below the one before it, in the order given.
 * Measured on the page, because the order is a layout claim the markup
 * alone does not make.
 */
async function expectTopToBottom(controls: [string, Locator][]) {
  let previous: { name: string; top: number } | undefined;
  for (const [name, control] of controls) {
    await expect(control).toBeVisible();
    const box = await control.boundingBox();
    expect(box, `${name} has a box`).not.toBeNull();
    if (previous) {
      expect(box!.y, `${name} is drawn below ${previous.name}`).toBeGreaterThan(
        previous.top,
      );
    }
    previous = { name, top: box!.y };
  }
}

/** The section locked because this specimen does not need decalcifying. */
async function expectDecalcificationNotRequired(page: Page) {
  const header = sectionHeader(page, DECALCIFICATION);
  await expect(header).toBeDisabled();
  await expect(header).toContainText("Not required for this specimen");
  const step = railStep(page, "Decalcification");
  await expect(step).toContainText("N/A");
  await expect(step.getByRole("button")).toBeDisabled();
}

/** The rail step back among the steps the case can still reach. */
async function expectDecalcificationOnTheRail(page: Page) {
  const step = railStep(page, "Decalcification");
  await expect(step).not.toContainText("N/A");
  await expect(step.getByRole("button")).toBeEnabled();
}

test.describe.serial("Pathology grossing section", () => {
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
    await expectStageSwitchesOn(page, ["DECALCIFICATION", "PROCESSING"]);
    const target = await discoverPathologyOrderTarget(page);
    seeded = await createPathologyCase(page, target);
    tissue = await ensureTissueType(page);
    await context.close();
  });

  test("an unflagged case does not need decalcification", async ({
    page,
  }, testInfo) => {
    await openCase(page, seeded);
    await advanceCase(page, { label: "Begin grossing" });

    await expectDecalcificationNotRequired(page);
    await expect(summaryRow(page, FLAG_ROW)).toHaveText(
      new RegExp(`^${FLAG_ROW}\\s*No$`),
    );

    await test.step("the case is sent on past the stage, to Processing", async () => {
      // Held until grossing is done, but already naming where it goes.
      await expect(primaryAction(page)).toHaveText("Send for processing");
      await expect(primaryAction(page)).toBeDisabled();
    });

    await page.screenshot({
      path: testInfo.outputPath("decalcification-not-required.png"),
      fullPage: true,
    });
  });

  test("the bench records the description, the flag and the cassettes", async ({
    page,
  }, testInfo) => {
    await openCase(page, seeded);
    await expandSection(page, GROSSING);

    await test.step("the description comes first, then the flag, the cassettes and the technician", async () => {
      await expectTopToBottom([
        ["the macroscopic description", description(page)],
        [
          "the decalcification flag",
          page.locator('label[for="decalcificationRequired"]'),
        ],
        [
          "the cassettes heading",
          page
            .locator(`#${GROSSING}`)
            .getByRole("heading", { name: "Cassettes", exact: true }),
        ],
        ["the technician selector", page.locator("select#assignedTechnician")],
      ]);
    });

    await test.step("the description runs past the old limit, over several lines", async () => {
      expect(
        DESCRIPTION.length,
        "the description is longer than the old 255-character column",
      ).toBeGreaterThan(255);
      await description(page).fill(DESCRIPTION);
      await expect(description(page)).toHaveValue(DESCRIPTION);
    });

    await test.step("the specimen is flagged for decalcification", async () => {
      await expect(decalcificationBox(page)).not.toBeChecked();
      await toggleDecalcification(page, true);
    });

    await test.step("two cassettes, the first given a tissue type", async () => {
      const addCassette = page.getByRole("button", { name: "Add cassette" });
      await addCassette.click();
      await addCassette.click();
      await expect(cassetteRows(page)).toHaveCount(2);

      const picker = tissuePicker(page, "New cassette 1");
      await expect(picker).toBeEnabled();
      await chooseCarbonOption(picker, tissue.name);
    });

    await saveDraft(page, seeded.pathologySampleId);

    await test.step("the cassettes are named and the first keeps its tissue", async () => {
      await expect(rowNamed(cassetteRows(page), "A1")).toHaveCount(1);
      await expect(rowNamed(cassetteRows(page), "A2")).toHaveCount(1);
      await expect(tissuePicker(page, "A1")).toHaveValue(tissue.name);
      await expect(tissuePicker(page, "A2")).toHaveValue("");
    });

    await test.step("described and cut, grossing is complete", async () => {
      await expect(grossingBadge(page)).toHaveText("Complete");
      await expect(summaryRow(page, "Cassettes")).toHaveText(/^Cassettes\s*2$/);
    });

    await test.step("flagged and saved, the case goes to Decalcification next", async () => {
      await expect(primaryAction(page)).toHaveText("Send for decalcification");
      await expect(primaryAction(page)).toBeEnabled();
    });

    await test.step("the flag puts Decalcification back on the case's path", async () => {
      await expect(summaryRow(page, FLAG_ROW)).toHaveText(
        new RegExp(`^${FLAG_ROW}\\s*Yes$`),
      );
      const header = sectionHeader(page, DECALCIFICATION);
      await expect(header).toBeDisabled();
      await expect(header).toContainText(
        "Available once the case reaches Decalcification",
      );
      await expect(header).not.toContainText("Not required for this specimen");
      await expectDecalcificationOnTheRail(page);
    });

    await page.screenshot({
      path: testInfo.outputPath("grossing-recorded.png"),
      fullPage: true,
    });
  });

  test("the description and the flag survive a fresh load", async ({
    page,
  }) => {
    // A fresh load, so every value read here is what the server stored
    // rather than what the form still held.
    await openCase(page, seeded);
    await expandSection(page, GROSSING);

    await expect(description(page)).toHaveValue(DESCRIPTION);
    await expect(decalcificationBox(page)).toBeChecked();
    await expect(tissuePicker(page, "A1")).toHaveValue(tissue.name);
    await expect(summaryRow(page, FLAG_ROW)).toHaveText(
      new RegExp(`^${FLAG_ROW}\\s*Yes$`),
    );
  });

  test("a blank description leaves grossing incomplete", async ({ page }) => {
    await openCase(page, seeded);
    await expandSection(page, GROSSING);
    await expect(grossingBadge(page)).toHaveText("Complete");

    await test.step("cassettes alone are counted, not complete", async () => {
      await description(page).fill("");
      await saveDraft(page, seeded.pathologySampleId);
      await expect(description(page)).toHaveValue("");
      await expect(grossingBadge(page)).toHaveText("2 cassettes");
    });

    await test.step("described again, complete again", async () => {
      await description(page).fill(DESCRIPTION);
      await saveDraft(page, seeded.pathologySampleId);
      await expect(grossingBadge(page)).toHaveText("Complete");
    });
  });

  test("the decalcification stage opens once the case reaches it", async ({
    page,
  }, testInfo) => {
    await openCase(page, seeded);

    await test.step("at the stage, the section opens on its placeholder", async () => {
      await advanceCase(page, { label: "Send for decalcification" });
      await expandSection(page, DECALCIFICATION);
      await expect(
        page
          .locator(`#${DECALCIFICATION}`)
          .getByText(/OpenELIS does not record work at this stage yet/),
      ).toBeVisible();
    });

    await test.step("the flag never locks the stage the case stands at", async () => {
      await expandSection(page, GROSSING);
      await toggleDecalcification(page, false);
      await saveDraft(page, seeded.pathologySampleId);

      const header = sectionHeader(page, DECALCIFICATION);
      await expect(header).toBeEnabled();
      await expect(header).not.toContainText("Not required for this specimen");
      await expect(summaryRow(page, FLAG_ROW)).toHaveText(
        new RegExp(`^${FLAG_ROW}\\s*No$`),
      );
    });

    await page.screenshot({
      path: testInfo.outputPath("decalcification-current-unflagged.png"),
      fullPage: true,
    });

    await test.step("returned to grossing, the unflagged case does not need it", async () => {
      await returnCase(page, "Specimen re-examined, no bone found");
      await expectDecalcificationNotRequired(page);
      await expect(primaryAction(page)).toHaveText("Send for processing");
      await expect(summaryRow(page, FLAG_ROW)).toHaveText(
        new RegExp(`^${FLAG_ROW}\\s*No$`),
      );
    });
  });

  test("the case view at two desktop widths, for the pull request", async ({
    page,
  }) => {
    // Opened once and resized in place: a second load while the first one's
    // lookups are still in flight aborts them, and the browser logs each as a
    // failed fetch.
    await page.setViewportSize({ width: 1480, height: 900 });
    await openCase(page, seeded);
    await expandSection(page, GROSSING);

    for (const width of [1480, 1280]) {
      await page.setViewportSize({ width, height: 900 });

      await expect(description(page)).toHaveValue(DESCRIPTION);
      await expect(rowNamed(cassetteRows(page), "A1")).toBeVisible();
      await expect(rowNamed(cassetteRows(page), "A2")).toBeVisible();
      await expect(tissuePicker(page, "A1")).toHaveValue(tissue.name);

      await page.screenshot({
        path: new URL(
          `../../../../e2e-evidence/ogc-264-s5-grossing-${width}.png`,
          import.meta.url,
        ).pathname,
        fullPage: true,
      });
    }
  });
});
