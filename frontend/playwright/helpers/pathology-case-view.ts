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
 * mounted, so it is opened here only when it is shut.
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
 * next ("Begin grossing", "Send for processing"). There is none from Ready
 * for Pathologist on.
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

/** The success toast a move raises, naming the stage the case went to. */
function moveToast(page: Page, text: string) {
  return page.locator(".cds--toast-notification").filter({ hasText: text });
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
    moveToast(page, `Case sent to ${STAGE_LABELS[stage]}`),
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
    moveToast(page, `Case returned to ${STAGE_LABELS[stage]}`),
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
