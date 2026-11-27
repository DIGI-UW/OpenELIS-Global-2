import { expect, Locator, Page } from "@playwright/test";
import {
  apiGet,
  apiPost,
  type PathologyStageId,
  type SeededPathologyCase,
} from "./seed-pathology-data";
import { NAV_TIMEOUT, UI_TIMEOUT } from "./timeouts";

/**
 * Shared steps for the anatomic-pathology case view on the case-view shell:
 * opening a seeded case, sending it on and returning it, opening its sections
 * and saving it, plus the locators the specs read the shell through, and the
 * laboratory's stage switches the specs depend on.
 */

const API_PREFIX = "/api/OpenELIS-Global";

/** Each stage as the screen names it in English. */
export const STAGE_LABELS: Record<PathologyStageId, string> = {
  ACCESSIONED: "Accessioned",
  GROSSING: "Grossing",
  DECALCIFICATION: "Decalcification",
  PROCESSING: "Processing",
  EMBEDDING: "Embedding",
  MICROTOMY: "Microtomy",
  STAINING: "Staining",
  COVERSLIPPING: "Coverslipping & QC",
  READY_PATHOLOGIST: "Ready for Pathologist",
  UNDER_REVIEW: "Under Pathologist Review",
  COMPLETED: "Completed",
};

/** The optional stages, each of which a laboratory may switch off. */
export type OptionalStageId =
  | "DECALCIFICATION"
  | "PROCESSING"
  | "EMBEDDING"
  | "MICROTOMY"
  | "STAINING"
  | "COVERSLIPPING"
  | "UNDER_REVIEW";

function escapeRegExp(text: string) {
  return text.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

/** The accordion's own heading text, section number and title together. */
export const NUMBERED_SECTION_TITLES = [
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

/** Open the seeded case and wait until the shell has rendered. */
export async function openCase(page: Page, seeded: SeededPathologyCase) {
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
export function sectionHeaders(page: Page) {
  return page.locator("button.cds--accordion__heading");
}

/**
 * The heading of one section, addressed by the accordion item id the screen
 * gives it, which is also what the progress rail scrolls to.
 */
export function sectionHeader(page: Page, sectionId: string) {
  return page.locator(`#${sectionId} button.cds--accordion__heading`);
}

/**
 * Expand one section, addressed by its item id. A section unlocks as soon as
 * the stage reaches it but keeps the open state it had when the screen
 * mounted (only Begin review opens Findings itself), so it is opened here
 * only when it is shut.
 */
export async function expandSection(page: Page, sectionId: string) {
  const header = sectionHeader(page, sectionId);
  await expect(header).toBeEnabled({ timeout: UI_TIMEOUT });
  if ((await header.getAttribute("aria-expanded")) === "false") {
    await header.click();
  }
  await expect(header).toHaveAttribute("aria-expanded", "true");
}

/** Every cassette row on the grossing list, retired ones included when shown. */
export function cassetteRows(page: Page) {
  return page.locator("#pathology-section-grossing .pathology-case-view__row");
}

/** Every slide row on the microtomy list. */
export function slideRows(page: Page) {
  return page.locator("#pathology-section-microtomy .pathology-case-view__row");
}

/** The microtomy section, where every slide is listed under its block. */
const MICROTOMY_SECTION = "#pathology-section-microtomy";

/**
 * One block's group on the microtomy list: its header line and the slides cut
 * from it. Addressed by the designation in the header, matched whole, so the
 * slides' own designations under it never match.
 */
export function blockGroup(page: Page, designation: string) {
  return page
    .locator(`${MICROTOMY_SECTION} .pathology-case-view__group`)
    .filter({
      has: page.locator(
        ".pathology-case-view__group-header .pathology-case-view__row-designation",
        { hasText: new RegExp(`^${escapeRegExp(designation)}$`) },
      ),
    });
}

/** The last group on the microtomy list, holding slides that name no block. */
export function legacySlideGroup(page: Page) {
  return page
    .locator(`${MICROTOMY_SECTION} .pathology-case-view__group`)
    .filter({
      has: page.locator(".pathology-case-view__group-title", {
        hasText: /^Slides with no block recorded$/,
      }),
    });
}

/** The Add slide of one block's group, named after the block. */
export function addSlideButton(page: Page, designation: string) {
  return page.getByRole("button", {
    name: `Add slide to ${designation}`,
    exact: true,
  });
}

/** Add one unsaved slide under a block, from that block's own Add slide. */
export async function addSlideTo(page: Page, designation: string) {
  const button = addSlideButton(page, designation);
  await expect(button).toBeEnabled();
  await button.click();
}

/**
 * The control on a slide's row that opens its label check, named after the
 * slide as printed ("Verify label of A1.1").
 */
export function verifyLabelButton(page: Page, slideName: string) {
  return page.getByRole("button", {
    name: `Verify label of ${slideName}`,
    exact: true,
  });
}

/**
 * The two fields a slide's label check is scanned or typed into, the block
 * first and the slide's printed label second, addressed by the slide's id.
 */
export function scanFields(page: Page, slideId: number | string) {
  return {
    block: page.locator(`#scanBlock-${slideId}`),
    slide: page.locator(`#scanSlide-${slideId}`),
  };
}

/** The Verified or Unverified tag in a slide row's identity line. */
export function labelTag(row: Locator) {
  return row
    .locator(".pathology-case-view__row-identity .cds--tag")
    .filter({ hasText: /^(Verified|Unverified)$/ });
}

/** The two stages whose sections list the slides as a table to work through. */
export type SlideWorkStage = "STAINING" | "COVERSLIPPING";

/** Each slide-work section's item id, and the id its table's parts carry. */
const SLIDE_WORK_SECTIONS: Record<
  SlideWorkStage,
  { sectionId: string; tableId: string }
> = {
  STAINING: { sectionId: "pathology-section-staining", tableId: "staining" },
  COVERSLIPPING: {
    sectionId: "pathology-section-coverslipping",
    tableId: "coverslipping",
  },
};

/** The section of one slide-work stage. */
export function slideWorkSection(page: Page, stage: SlideWorkStage) {
  return page.locator(`#${SLIDE_WORK_SECTIONS[stage].sectionId}`);
}

/**
 * One slide's row in a slide-work section's table, by the name the slide
 * goes by under its block ("A1.1"), matched whole. Both sections list the
 * same slides, so the row is always addressed inside its section.
 */
export function slideWorkRow(
  page: Page,
  stage: SlideWorkStage,
  slideName: string,
) {
  const { tableId } = SLIDE_WORK_SECTIONS[stage];
  return slideWorkSection(page, stage)
    .locator(`tr[data-testid^="${tableId}Row-"]`)
    .filter({
      has: page.locator(".pathology-case-view__row-designation", {
        hasText: new RegExp(`^${escapeRegExp(slideName)}$`),
      }),
    });
}

/**
 * A row's own action on a slide, by its accessible name, which starts with
 * the words the button shows and then names the slide ("Mark stained: A1.1",
 * "Pass coverslip QC: A1.1").
 */
export function slideWorkButton(
  page: Page,
  stage: SlideWorkStage,
  name: string,
) {
  return slideWorkSection(page, stage).getByRole("button", {
    name,
    exact: true,
  });
}

/**
 * The checkbox that selects one slide, or every slide, for a batch.
 * Carbon hides the input itself, so the label it is drawn by is what a
 * person clicks, and the input is what is read back.
 */
export function slideSelectCheckbox(
  page: Page,
  stage: SlideWorkStage,
  slideName?: string,
) {
  return slideWorkSection(page, stage).getByRole("checkbox", {
    name: slideName ? `Select ${slideName}` : "Select all slides",
    exact: true,
  });
}

/** Select slides for a batch by clicking each one's checkbox label. */
export async function selectSlides(
  page: Page,
  stage: SlideWorkStage,
  slideNames: string[],
) {
  for (const slideName of slideNames) {
    const checkbox = slideSelectCheckbox(page, stage, slideName);
    const id = await checkbox.getAttribute("id");
    await slideWorkSection(page, stage).locator(`label[for="${id}"]`).click();
    await expect(checkbox).toBeChecked();
  }
}

/** The batch bar's one action ("Mark selected stained", "Pass selected"). */
export function slideBatchAction(page: Page, stage: SlideWorkStage) {
  return page.locator(`#${SLIDE_WORK_SECTIONS[stage].tableId}BatchAction`);
}

/** The row a batch is confirmed in, which names every selected slide. */
export function slideBatchConfirm(page: Page, stage: SlideWorkStage) {
  return page.locator(`#${SLIDE_WORK_SECTIONS[stage].tableId}BatchConfirm`);
}

/**
 * The line above a slide-work table that says why the whole section is
 * held, present only while it is.
 */
export function slideWorkHold(page: Page, stage: SlideWorkStage) {
  return page.locator(`#${SLIDE_WORK_SECTIONS[stage].tableId}Hold`);
}

/**
 * A wait for one slide mark (a stain status or a coverslip QC result),
 * registered before the click that starts it.
 */
export function waitForSlideWork(
  page: Page,
  caseId: string,
  stage: SlideWorkStage,
) {
  const path = `/rest/pathology/caseView/${caseId}/slides/${
    stage === "STAINING" ? "stain-status" : "coverslip-qc"
  }`;
  return page.waitForResponse(
    (r) =>
      r.request().method() === "POST" &&
      new URL(r.url()).pathname.endsWith(path),
    { timeout: UI_TIMEOUT },
  );
}

/**
 * The one row on a list carrying a designation, matched whole, so A1 never
 * also matches A10.
 */
export function rowNamed(rows: Locator, designation: string) {
  return rows.filter({
    has: rows.page().locator(".pathology-case-view__row-designation", {
      hasText: new RegExp(`^${designation}$`),
    }),
  });
}

/** One step of the progress rail, addressed by its label. */
export function railStep(page: Page, label: string) {
  return page
    .locator('nav[aria-label="Case progress"] li.cds--progress-step')
    .filter({
      has: page.locator(".cds--progress-label", {
        hasText: new RegExp(`^${label}$`),
      }),
    });
}

/** One row of the case summary, addressed by the label in its first half. */
export function summaryRow(page: Page, label: string) {
  return page.locator(".case-view__summary-row").filter({
    has: page.locator(".case-view__summary-label", {
      hasText: new RegExp(`^${label}$`),
    }),
  });
}

/** Whether two rendered boxes cover any of the same pixels. */
export function overlaps(
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
 * The action bar's one primary button, whose label names where the case goes
 * next ("Begin grossing", "Send for processing"). From Ready for Pathologist
 * it is "Begin review" where the laboratory uses the review stage and the
 * case read offers it, and there is none after that.
 */
export function primaryAction(page: Page) {
  return page.locator(".case-view__action-bar-actions .cds--btn--primary");
}

/** The control in the action bar's status slot that opens the return row. */
export function returnControl(page: Page) {
  return page.getByRole("button", {
    name: "Return to previous stage",
    exact: true,
  });
}

/** The inline row a return is confirmed in, while it is open. */
export function returnRow(page: Page) {
  return page.locator(".pathology-case-view__return-row");
}

/** A toast carrying the sentence it was given: a move's, a mark's, a request's. */
export function toast(page: Page, sentence: string | RegExp) {
  return page.locator(".cds--toast-notification").filter({ hasText: sentence });
}

/**
 * A wait for one move of the case, registered before the click that starts
 * it.
 */
function waitForMovePost(page: Page, caseId: string, direction: string) {
  const path = `/rest/pathology/caseView/${caseId}/${direction}`;
  return page.waitForResponse(
    (r) =>
      r.request().method() === "POST" &&
      new URL(r.url()).pathname.endsWith(path),
    { timeout: UI_TIMEOUT },
  );
}

/**
 * A wait for the read of the case the screen makes once a move is answered.
 * The screen reads the case back only on a clean form, which both moves
 * require before they can be pressed.
 */
function waitForCaseRead(page: Page, caseId: string) {
  const path = `/rest/pathology/caseView/${caseId}`;
  return page.waitForResponse(
    (r) =>
      r.request().method() === "GET" &&
      new URL(r.url()).pathname.endsWith(path),
    { timeout: UI_TIMEOUT },
  );
}

/** The id of the case the page shows, read from its address. */
function caseIdOf(page: Page) {
  const match = /\/PathologyCaseView\/(\d+)/.exec(page.url());
  if (!match) {
    throw new Error(`Not on a pathology case: ${page.url()}`);
  }
  return match[1];
}

/** The stage the case summary says the case stands at. */
export async function expectStage(page: Page, label: string) {
  await expect(summaryRow(page, "Stage")).toHaveText(
    new RegExp(`^Stage\\s*${escapeRegExp(label)}$`),
    { timeout: UI_TIMEOUT },
  );
}

/**
 * Send the case on with the primary button, exactly as the bench does, and
 * wait for the screen to finish with it: the move is answered, the toast
 * names the stage, and the case read back shows it. The button is found by
 * the label the caller expects it to carry, so a label that names another
 * stage fails here rather than moving the case somewhere unexpected. Returns
 * the stage the case was sent to.
 */
export async function advanceCase(
  page: Page,
  { label }: { label: string },
): Promise<PathologyStageId> {
  const button = primaryAction(page);
  await expect(button).toHaveText(label);
  await expect(button).toBeEnabled();

  const caseId = caseIdOf(page);
  const moved = waitForMovePost(page, caseId, "advance");
  const readBack = waitForCaseRead(page, caseId);
  await button.click();
  const answer = (await (await moved).json()) as { status?: PathologyStageId };
  const stage = answer.status;
  if (!stage) {
    throw new Error(`"${label}" was answered with no stage`);
  }
  await expect(
    toast(page, `Case sent to ${STAGE_LABELS[stage]}`),
  ).not.toHaveCount(0, { timeout: UI_TIMEOUT });
  await readBack;
  await expectStage(page, STAGE_LABELS[stage]);
  return stage;
}

/**
 * Return the case one stage, giving the reason in the inline row the return
 * control opens, and wait the same way as advanceCase. Returns the stage the
 * case was returned to.
 */
export async function returnCase(
  page: Page,
  reason: string,
): Promise<PathologyStageId> {
  await expect(returnControl(page)).toBeEnabled();
  await returnControl(page).click();
  const row = returnRow(page);
  await expect(row).toBeVisible();
  await row.getByRole("textbox", { name: /^Reason/ }).fill(reason);

  const confirm = row.getByRole("button", { name: /^Return to / });
  await expect(confirm).toBeEnabled();
  const caseId = caseIdOf(page);
  const moved = waitForMovePost(page, caseId, "return");
  const readBack = waitForCaseRead(page, caseId);
  await confirm.click();
  const answer = (await (await moved).json()) as { status?: PathologyStageId };
  const stage = answer.status;
  if (!stage) {
    throw new Error("The return was answered with no stage");
  }
  await expect(
    toast(page, `Case returned to ${STAGE_LABELS[stage]}`),
  ).not.toHaveCount(0, { timeout: UI_TIMEOUT });
  await readBack;
  await expectStage(page, STAGE_LABELS[stage]);
  await expect(row).toHaveCount(0);
  return stage;
}

/** One row of the Result Entry Configuration page, and its stored value. */
async function readSiteSettingRow(page: Page, name: string) {
  const menu = await apiGet<{ menuList: { id: string; name: string }[] }>(
    page,
    `${API_PREFIX}/rest/ResultConfigurationMenu`,
  );
  const row = menu.menuList.find((entry) => entry.name === name);
  if (!row) {
    throw new Error(`No configuration row named ${name}`);
  }
  const stored = await apiGet<Record<string, unknown>>(
    page,
    `${API_PREFIX}/rest/ResultConfiguration?ID=${row.id}`,
  );
  return { row, stored };
}

/**
 * One setting of the Result Entry Configuration page, as stored ("true" or
 * "false" for a switch). A row stored with no value reads as `fallback`.
 */
export async function readSiteSetting(
  page: Page,
  name: string,
  fallback = "true",
) {
  return String(
    (await readSiteSettingRow(page, name)).stored.value ?? fallback,
  );
}

/**
 * Change one setting exactly as the Result Entry Configuration page does:
 * read the configuration row the administrator would select, change its
 * value, and post it back to the same endpoint. The server reloads its own
 * configuration as part of that save; the browser reads it once, when the
 * application boots, so a page shows the new value only after a full load.
 */
export async function setSiteSetting(page: Page, name: string, value: string) {
  const { row, stored } = await readSiteSettingRow(page, name);
  await apiPost(
    page,
    `${API_PREFIX}/rest/ResultConfiguration?ID=${row.id}`,
    { ...stored, value },
    `Setting ${name} to ${value}`,
  );
}

/** The setting that says whether slide labels are checked with a scanner. */
export const SCAN_VERIFICATION_SETTING =
  "pathology.microtomy.scanVerificationRequired";

/** The name of the switch that says whether the laboratory tracks a stage. */
function stageSwitchName(stage: OptionalStageId) {
  return `pathology.stage.${stage}.enabled`;
}

/**
 * Whether the laboratory tracks one optional stage, as the stage switch on
 * the Result Entry Configuration page says ("true" or "false").
 */
export async function readStageSwitch(page: Page, stage: OptionalStageId) {
  return readSiteSetting(page, stageSwitchName(stage));
}

/** Turn one optional bench stage on or off, as setSiteSetting does. */
export async function setStageSwitch(
  page: Page,
  stage: OptionalStageId,
  value: string,
) {
  await setSiteSetting(page, stageSwitchName(stage), value);
}

/**
 * Fail at once, naming the switch, when a stage a spec walks through is
 * switched off on this deployment: the server skips such a stage, so every
 * later assertion would fail as if the feature were broken.
 */
export async function expectStageSwitchesOn(
  page: Page,
  stages: OptionalStageId[],
) {
  const off: string[] = [];
  for (const stage of stages) {
    if ((await readStageSwitch(page, stage)) === "false") {
      off.push(stageSwitchName(stage));
    }
  }
  if (off.length > 0) {
    throw new Error(
      `This spec walks a case through stages this deployment does not track: ${off.join(", ")} ` +
        "is switched off on the Result Entry Configuration page. Switch it back on, " +
        "or find the spec that left it off.",
    );
  }
}

/**
 * Save the case from the action bar and wait for the screen to finish with
 * it. The shell reads the saved case back after a successful save, so that
 * read is waited on before the test moves on: a navigation while it is in
 * flight aborts it and logs a fetch error, and the rows only carry the names
 * the server gave them once it has landed.
 */
export async function saveDraft(page: Page, caseId: string) {
  const casePath = `/rest/pathology/caseView/${caseId}`;
  const isCaseCall = (url: string, method: string, wanted: string) =>
    method === wanted && new URL(url).pathname.endsWith(casePath);
  const posted = page.waitForResponse((r) =>
    isCaseCall(r.url(), r.request().method(), "POST"),
  );
  const readBack = page.waitForResponse(
    (r) => isCaseCall(r.url(), r.request().method(), "GET"),
    { timeout: UI_TIMEOUT },
  );

  await page.getByRole("button", { name: "Save draft" }).click();
  await posted;

  // A refused save raises an error toast carrying the server's rule, and is
  // never read back; the toast text is what says why.
  await expect(
    page
      .locator(".cds--toast-notification")
      .filter({ hasText: "Successfully saved" }),
  ).not.toHaveCount(0, { timeout: UI_TIMEOUT });
  await expect(
    page.locator(".cds--toast-notification--error"),
    "the save raised no error toast",
  ).toHaveCount(0);

  await readBack;
  await expect(page.getByText("Unsaved changes")).toHaveCount(0, {
    timeout: UI_TIMEOUT,
  });
}

/** The Pathologist Review section, which holds the bench requests. */
export const REVIEW_SECTION = "pathology-section-review";

/** What a bench request asks for, by the id its radio button carries. */
export type RequestKindId = "BLOCK" | "SLIDE" | "STAIN";

/**
 * The region the requests table scrolls in, present while the table is,
 * which is what a layout check measures.
 */
export function requestsRegion(page: Page) {
  return page.locator("#requestsRegion");
}

/** The toolbar's New request, offered only in the pathologist's open review. */
export function newRequestButton(page: Page) {
  return page.locator("#newRequest");
}

/** The new request's row, the first of the table while it is open. */
export function newRequestRow(page: Page) {
  return page.locator('[data-testid="newRequestRow"]');
}

/** The radio button of one kind on the new request's row. */
export function requestKindRadio(page: Page, kind: RequestKindId) {
  return page.locator(`#requestKind-${kind}`);
}

/**
 * Pick the kind of the new request by clicking its label, since Carbon hides
 * the radio input itself, and wait until the input reads checked.
 */
export async function chooseRequestKind(page: Page, kind: RequestKindId) {
  await newRequestRow(page).locator(`label[for="requestKind-${kind}"]`).click();
  await expect(requestKindRadio(page, kind)).toBeChecked();
}

/** The new request's target picker: blocks for a slide, slides for a stain. */
export function requestTargetPicker(page: Page) {
  return page.locator("input#requestTarget");
}

/** The new request's stain picker, drawn only for a stain request. */
export function requestStainPicker(page: Page) {
  return page.locator("input#requestStain");
}

/** The new request's instruction. */
export function requestInstruction(page: Page) {
  return page.locator("#requestInstruction");
}

/** The new request's Raise request. */
export function raiseRequestButton(page: Page) {
  return page.locator("#raiseRequest");
}

/**
 * One request's row in the requests table, by its instruction, matched
 * whole in the instruction column.
 */
export function requestRow(page: Page, instruction: string) {
  return page
    .locator(`#${REVIEW_SECTION} tr[data-testid^="requestRow-"]`)
    .filter({
      has: page.locator("td.pathology-case-view__request-col--instruction", {
        hasText: new RegExp(`^${escapeRegExp(instruction)}$`),
      }),
    });
}

/** Every request row of the table, newest first. */
export function requestRows(page: Page) {
  return page.locator(`#${REVIEW_SECTION} tr[data-testid^="requestRow-"]`);
}

/** The status tag of one request's row. */
export function requestStatusTag(page: Page, instruction: string) {
  return requestRow(page, instruction).locator(
    "td.pathology-case-view__request-col--status .cds--tag",
  );
}

/**
 * A request action in the Review section by its accessible name, which is
 * the word the button shows and then the request's instruction ("Complete:
 * Recut A1 at 3 microns"), or Confirm followed by the instruction on an
 * inline row.
 */
export function requestAction(page: Page, name: string) {
  return page
    .locator(`#${REVIEW_SECTION}`)
    .getByRole("button", { name, exact: true });
}

/**
 * A wait for one request change (a raise, or a status change of the request
 * with this id), registered before the click that starts it.
 */
export function waitForRequestChange(
  page: Page,
  caseId: string,
  requestId?: number,
) {
  const path =
    requestId === undefined
      ? `/rest/pathology/caseView/${caseId}/requests`
      : `/rest/pathology/caseView/${caseId}/requests/${requestId}/status`;
  return page.waitForResponse(
    (r) =>
      r.request().method() === "POST" &&
      new URL(r.url()).pathname.endsWith(path),
    { timeout: UI_TIMEOUT },
  );
}

/**
 * The confirmation in the action bar's status slot that lists the open
 * requests before the case goes to the pathologist with them.
 */
export function sendAnywayConfirmation(page: Page) {
  return page.locator("#sendAnywayConfirm");
}

/** The confirmation's Send anyway. */
export function sendAnywayButton(page: Page) {
  return page.locator("#sendAnyway");
}

/**
 * The names a Carbon ComboBox offers, read by opening its list and closed
 * again with Escape, so nothing is chosen.
 */
export async function pickerOptions(combobox: Locator): Promise<string[]> {
  await expect(combobox).toBeEnabled();
  await combobox.click();
  await expect(combobox).toHaveAttribute("aria-expanded", "true");
  const listboxId = await combobox.getAttribute("aria-controls");
  if (!listboxId) {
    throw new Error("The picker has no list it controls");
  }
  const options = combobox
    .page()
    .locator(`[id="${listboxId}"]`)
    .getByRole("option");
  await expect(options).not.toHaveCount(0);
  const names = (await options.allInnerTexts()).map((name) => name.trim());
  await combobox.press("Escape");
  await expect(combobox).toHaveAttribute("aria-expanded", "false");
  return names;
}

/** The Findings & Conclusion section. */
export const FINDINGS_SECTION = "pathology-section-findings";

/** The coded conclusion's search box, a Carbon ComboBox. */
export function diagnosisBox(page: Page) {
  return page.locator("#conclusion");
}

/** The chips of the diagnoses chosen, in the order they were picked. */
export function diagnosisChips(page: Page) {
  return page.getByTestId("findings-conclusion-chips");
}

/** The chips of the techniques chosen. */
export function techniqueChips(page: Page) {
  return page.getByTestId("findings-technique-chips");
}

/** The add-diagnosis control; absent while the picker cannot be edited. */
export function addDiagnosisControl(page: Page) {
  return page.getByTestId("findings-add-diagnosis");
}

/** The read-only table of the slides the pathologist has received. */
export function slidesReceivedTable(page: Page) {
  return page.getByTestId("review-slides-received");
}

/** The list the diagnosis search box shows its answer in. */
export async function diagnosisMenu(page: Page) {
  const box = diagnosisBox(page);
  const listboxId = await box.getAttribute("aria-controls");
  if (!listboxId) {
    throw new Error("The diagnosis search has no list it controls");
  }
  return page.locator(`[id="${listboxId}"]`);
}

/**
 * Type `query` into the diagnosis search and wait for the server's answer to
 * it, leaving the list open. The search asks again only once typing pauses,
 * so the wait is registered before the text goes in.
 */
export async function searchDiagnoses(page: Page, query: string) {
  const box = diagnosisBox(page);
  await expect(box).toBeEnabled({ timeout: UI_TIMEOUT });
  const answered = page.waitForResponse(
    (r) => {
      const url = new URL(r.url());
      return (
        url.pathname.endsWith("/rest/pathology/conclusionEntries") &&
        url.searchParams.get("q") === query.trim()
      );
    },
    { timeout: UI_TIMEOUT },
  );
  await box.fill(query);
  await answered;
  if ((await box.getAttribute("aria-expanded")) !== "true") {
    await box.click();
  }
  await expect(box).toHaveAttribute("aria-expanded", "true");
  return diagnosisMenu(page);
}

/**
 * Find a diagnosis by `query` and pick the item reading `label` ("code
 * name"). The pick adds a chip, empties the box and leaves focus in it.
 */
export async function pickDiagnosis(page: Page, query: string, label: string) {
  const menu = await searchDiagnoses(page, query);
  await menu.getByRole("option", { name: label, exact: true }).click();
  await expect(diagnosisChips(page)).toContainText(label);
  await expect(diagnosisBox(page)).toHaveValue("");
  await expect(diagnosisBox(page)).toBeFocused();
}

/** The second opinion panel's ids are read inside the Review section. */
function inReview(page: Page) {
  return page.locator(`#${REVIEW_SECTION}`);
}

/** The panel's scrolling region, present while its table is. */
export function consultationsRegion(page: Page) {
  return page.locator("#consultationsRegion");
}

/** The sentence that says why the panel's controls are held, when they are. */
export function consultationsHold(page: Page) {
  return page.locator("#consultationsHold");
}

/** The toolbar's Request second opinion, offered to a pathologist only. */
export function requestConsultationButton(page: Page) {
  return page.locator("#requestConsultation");
}

/** The request's row, the first of the table while it is open. */
export function newConsultationRow(page: Page) {
  return page.getByTestId("newConsultationRow");
}

/** The suggestion that a second opinion is usual for the case's conclusion. */
export function secondOpinionSuggestion(page: Page) {
  return page.getByTestId("review-second-opinion-suggested");
}

/** The request's colleague picker, found by role and its whole name. */
export function consultationColleagueBox(page: Page) {
  return newConsultationRow(page).getByRole("combobox", {
    name: "Consulting pathologist",
    exact: true,
  });
}

/**
 * One colleague in the picker's open menu, by its whole name. Scoped to the
 * request row: the page's other lists name the same users.
 */
export function consultationColleagueOption(page: Page, name: string) {
  return newConsultationRow(page).getByRole("option", { name, exact: true });
}

/** The request's reason list, found by role and its whole name. */
export function consultationReasonSelect(page: Page) {
  return newConsultationRow(page).getByRole("combobox", {
    name: "Reason",
    exact: true,
  });
}

/**
 * The request's Yes or No for "Requested before treatment", by the label,
 * since Carbon hides the radio input itself.
 */
export function consultationPreTreatmentLabel(page: Page, yes: boolean) {
  return page.locator(
    `label[for="consultationPreTreatment-${yes ? "yes" : "no"}"]`,
  );
}

/** The request's Ask for the opinion, held until the rules are met. */
export function askColleagueButton(page: Page) {
  return page.locator("#askColleague");
}

/** The visible sentence that names the first rule still unmet. */
export function askColleagueHint(page: Page) {
  return page.locator("#askColleagueHint");
}

/** The request's Cancel. */
export function askColleagueCancelButton(page: Page) {
  return page.locator("#askColleagueCancel");
}

/** One second opinion's row in the panel's table, by the row's id. */
export function consultationRow(page: Page, consultationId: number) {
  return page.getByTestId(`consultationRow-${consultationId}`);
}

/** Every second opinion row of the table. */
export function consultationRows(page: Page) {
  return inReview(page).locator('[data-testid^="consultationRow-"]');
}

/** The row's Record the outcome, offered to the colleague consulted. */
export function recordOutcomeButton(page: Page, consultationId: number) {
  return page.locator(`#recordOutcome-${consultationId}`);
}

/** The inline row an outcome is recorded in. */
export function consultationAnswerRow(page: Page, consultationId: number) {
  return page.getByTestId(`consultationAnswer-${consultationId}`);
}

/**
 * The outcome's choice, by the label of its radio, since Carbon hides the
 * radio input itself.
 */
export function consultationOutcomeLabel(
  page: Page,
  consultationId: number,
  outcome: "AGREE" | "DISAGREE" | "MODIFIED",
) {
  return page.locator(
    `label[for="consultationOutcome-${consultationId}-${outcome}"]`,
  );
}

/** The sentence that names why the outcome cannot be recorded yet. */
export function consultationAnswerHint(page: Page, consultationId: number) {
  return page.locator(`#consultationAnswerHint-${consultationId}`);
}

/** The outcome's note. */
export function consultationNote(page: Page, consultationId: number) {
  return page.locator(`#consultationNote-${consultationId}`);
}

/** The outcome's confirm button. */
export function consultationAnswerConfirmButton(
  page: Page,
  consultationId: number,
) {
  return page.locator(`#consultationAnswerConfirm-${consultationId}`);
}

/** The outcome's cancel button. */
export function consultationAnswerCancelButton(
  page: Page,
  consultationId: number,
) {
  return page.locator(`#consultationAnswerCancel-${consultationId}`);
}

/** The row's withdraw button, offered to a pathologist who is not consulted. */
export function withdrawConsultationButton(page: Page, consultationId: number) {
  return page.locator(`#withdrawConsultation-${consultationId}`);
}

/** The inline row a withdrawal is confirmed in. */
export function consultationWithdrawRow(page: Page, consultationId: number) {
  return page.getByTestId(`consultationWithdraw-${consultationId}`);
}

/** The withdrawal's danger button. */
export function consultationWithdrawConfirmButton(
  page: Page,
  consultationId: number,
) {
  return page.locator(`#consultationWithdrawConfirm-${consultationId}`);
}

/** The withdrawal's keep button. */
export function consultationWithdrawKeepButton(
  page: Page,
  consultationId: number,
) {
  return page.locator(`#consultationWithdrawKeep-${consultationId}`);
}

/**
 * A wait for one second opinion change (a request, or an answer or a
 * withdrawal of the row with this id), registered before the click that
 * starts it.
 */
export function waitForConsultationChange(
  page: Page,
  caseId: string,
  change: { consultationId?: number; action?: "answer" | "withdraw" } = {},
) {
  const base = `/rest/pathology/caseView/${caseId}/consultations`;
  const path =
    change.consultationId === undefined
      ? base
      : `${base}/${change.consultationId}/${change.action ?? "answer"}`;
  return page.waitForResponse(
    (r) =>
      r.request().method() === "POST" &&
      new URL(r.url()).pathname.endsWith(path),
    { timeout: UI_TIMEOUT },
  );
}
