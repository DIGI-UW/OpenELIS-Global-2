import { expect, Page } from "@playwright/test";
import type { SeededPathologyCase } from "./seed-pathology-data";
import { NAV_TIMEOUT, UI_TIMEOUT } from "./timeouts";

/**
 * Shared steps for the anatomic-pathology case view on the case-view shell:
 * opening a seeded case, moving it between stages, opening its sections and
 * saving it, plus the locators the specs read the shell through.
 */

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
 * The stage control in the action bar. It is a Carbon Dropdown rather than a
 * native select, because the bar sits at the foot of a long page where a
 * native menu opens downward and the browser clips it; the menu is markup in
 * the page, opened by its own toggle, and its options exist only while it is
 * open.
 */
export function stageControl(page: Page) {
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
export async function openStageMenu(page: Page) {
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
    // Two pixels: sub-pixel layout put the menu 0.97 px over the line once.
  ).toBeLessThanOrEqual(toggleBox!.y + 2);

  return options;
}

/** Move the case to a stage by name, exactly as a technician would. */
export async function setStage(page: Page, label: string) {
  const options = await openStageMenu(page);
  await options.filter({ hasText: new RegExp(`^${label}$`) }).click();
  await expect(stageControl(page)).toContainText(label);
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
