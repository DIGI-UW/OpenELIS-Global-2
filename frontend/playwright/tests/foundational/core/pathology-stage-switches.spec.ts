import { test, expect } from "../../../helpers/test-base";
import { NAV_TIMEOUT } from "../../../helpers/timeouts";

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

/** A switch's configuration row name, shown when it has no translated name. */
const stageSwitchName = (stage: string) => `pathology.stage.${stage}.enabled`;

/**
 * The seven stages a deployment may choose not to run, by the name the page
 * shows for each switch (OGC-1547: translated, no longer the row name).
 */
const OPTIONAL_STAGE_SWITCHES = [
  "Pathology stage: Decalcification",
  "Pathology stage: Processing",
  "Pathology stage: Embedding",
  "Pathology stage: Microtomy",
  "Pathology stage: Staining",
  "Pathology stage: Coverslipping & QC",
  "Pathology stage: Under Pathologist Review",
];

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

test.describe("Pathology stage switches on Result Entry Configuration", () => {
  test("Result Configuration lists the seven stage switches", async ({
    page,
  }) => {
    await page.goto("/MasterListsPage/ResultConfigurationMenu", {
      waitUntil: "domcontentloaded",
    });

    await expect(
      page.getByRole("heading", {
        name: "Result & Validation Configuration",
        exact: true,
      }),
    ).toBeVisible({ timeout: NAV_TIMEOUT });

    // OGC-1364 v3: the settings are switches grouped by purpose, every one on
    // the page at once, so an absent switch is really absent.
    const pathology = page.getByTestId("settings-group-pathology");
    await expect(pathology).toBeVisible({ timeout: NAV_TIMEOUT });

    for (const switchName of OPTIONAL_STAGE_SWITCHES) {
      const stageSwitch = pathology.getByRole("switch", {
        name: switchName,
        exact: true,
      });
      await expect(stageSwitch).toHaveCount(1);
      await expect(stageSwitch).toHaveAttribute("aria-checked", "true");
    }

    // A mandatory stage has no translated name, so a switch offered for one
    // would show its raw name.
    for (const switchName of MANDATORY_STAGE_SWITCHES) {
      await expect(page.getByText(switchName, { exact: true })).toHaveCount(0);
    }
  });
});
