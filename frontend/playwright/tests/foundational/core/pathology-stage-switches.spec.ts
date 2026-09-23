import { test, expect } from "../../../helpers/test-base";
import { NAV_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";

/**
 * Pathology stage switches on Result Entry Configuration.
 *
 * Seven of the eleven histopathology bench stages are optional: a laboratory
 * that does not run one turns it off and cases skip it, shown as not
 * applicable. The four remaining stages are the ones a case cannot skip, so
 * they are deliberately not configurable and must never be offered as a
 * switch. The switches are ordinary configuration rows, which is why an
 * administrator sets them on the Result Entry Configuration page rather than
 * in a pathology screen, and why this proof lives apart from the dashboard.
 *
 * What this proves on a deployed stack:
 *   - each of the seven optional stages has exactly one switch row, and each
 *     is enabled out of the box, and
 *   - none of the four mandatory stages has a row at all.
 *
 * Nothing is modified. The page is read, never saved, so the deployment's
 * configuration is left exactly as it was found.
 */

/** The configuration page names each switch by its configuration row name. */
const stageSwitchName = (stage: string) => `pathology.stage.${stage}.enabled`;

/** The seven stages a deployment may choose not to run. */
const OPTIONAL_STAGE_SWITCHES = [
  "DECALCIFICATION",
  "PROCESSING",
  "EMBEDDING",
  "MICROTOMY",
  "STAINING",
  "COVERSLIPPING",
  "UNDER_REVIEW",
].map(stageSwitchName);

/**
 * The four stages every case passes through. Offering a switch for any of
 * them would be the defect, so their absence is asserted rather than assumed.
 */
const MANDATORY_STAGE_SWITCHES = [
  "ACCESSIONED",
  "GROSSING",
  "READY_PATHOLOGIST",
  "COMPLETED",
].map(stageSwitchName);

/**
 * The range the pagination reports once the whole table is on one page: it
 * starts at the first row and its last row is the last row there is. The
 * backreference is what carries the meaning, so the assertion cannot be
 * satisfied by a partial page. Carbon's own wording uses an en dash, this
 * deployment's message bundle a hyphen, so either is accepted.
 */
const WHOLE_TABLE_ON_ONE_PAGE = /^1[-–](\d+) of \1 items?$/;

test.describe("Pathology stage switches on Result Entry Configuration", () => {
  test("Result Configuration lists the seven stage switches", async ({
    page,
  }) => {
    await page.goto("/MasterListsPage/ResultConfigurationMenu", {
      waitUntil: "domcontentloaded",
    });

    await expect(
      page.getByRole("heading", {
        name: "Result Entry Configuration",
        exact: true,
      }),
    ).toBeVisible({ timeout: NAV_TIMEOUT });

    // This page's column headers are not sortable, so the accessible name is
    // the header text alone.
    await expect(page.getByRole("columnheader", { name: "Name" })).toBeVisible({
      timeout: NAV_TIMEOUT,
    });

    // The table paginates, and a row on a second page is as invisible to a
    // locator as a row that does not exist, which would make the absence
    // assertions below quietly vacuous. So widen to the largest page size the
    // control offers and then prove, rather than assume, that the whole table
    // now fits on this one page. The pagination control labels its
    // items-per-page select through an associated label element.
    const pagination = page.locator(".cds--pagination");
    await expect(pagination).toBeVisible({ timeout: UI_TIMEOUT });
    await pagination.getByLabel("Items per page").selectOption("50");

    // Carbon gives the range text no role of its own, so it is read by the
    // class the pagination puts on it. If this domain ever outgrows one page
    // of 50 rows, this fails loudly here instead of silently weakening the
    // absence assertions that follow.
    await expect(
      pagination.locator(".cds--pagination__items-count"),
    ).toHaveText(WHOLE_TABLE_ON_ONE_PAGE, { timeout: UI_TIMEOUT });

    for (const switchName of OPTIONAL_STAGE_SWITCHES) {
      // The row is found by the cell holding the switch's own name, and the
      // value is then read from a sibling cell of that same row, so a value
      // belonging to some other row can never satisfy the assertion.
      const switchRow = page
        .getByRole("row")
        .filter({ has: page.getByRole("cell", { name: switchName }) });
      await expect(switchRow).toHaveCount(1);

      // Exact: the description in the next cell also contains the word true.
      await expect(
        switchRow.getByRole("cell", { name: "true", exact: true }),
      ).toBeVisible();
    }

    for (const switchName of MANDATORY_STAGE_SWITCHES) {
      await expect(page.getByRole("cell", { name: switchName })).toHaveCount(0);
    }
  });
});
