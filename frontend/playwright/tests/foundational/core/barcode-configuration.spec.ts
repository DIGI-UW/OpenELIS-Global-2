import { expect, test } from "../../../helpers/test-base";
import { UI_TIMEOUT } from "../../../helpers/timeouts";

// OGC-285 replaced the legacy Barcode Configuration form with the Label Presets
// admin page: BarcodeConfiguration.jsx was deleted and
// /MasterListsPage/barcodeConfiguration now redirects to
// /MasterListsPage/labelPresets. This spec verifies the new admin surface loads
// (via the legacy route's redirect) and lists the seeded system presets.
test.describe("Label presets admin", () => {
  test("legacy barcodeConfiguration route redirects to Label Presets and lists seeded presets", async ({
    page,
  }) => {
    await page.goto("/MasterListsPage/barcodeConfiguration", {
      waitUntil: "domcontentloaded",
    });

    // The deleted form's route redirects to the Label Presets page.
    await expect(page).toHaveURL(/\/MasterListsPage\/labelPresets/, {
      timeout: UI_TIMEOUT,
    });

    // The five Liquibase-seeded system presets render in the list (distinctive
    // names; "Order Label" is omitted to avoid colliding with the "Order Labels"
    // table title used elsewhere).
    for (const name of [
      "Specimen Label",
      "Block Label",
      "Slide Label",
      "Freezer Label",
    ]) {
      await expect(page.getByText(name, { exact: true }).first()).toBeVisible({
        timeout: UI_TIMEOUT,
      });
    }

    // The create affordance is present.
    await expect(page.getByRole("button", { name: /add preset/i })).toBeVisible(
      { timeout: UI_TIMEOUT },
    );
  });
});
