import { expect, test } from "@playwright/test";

type BarcodeConfigState = Record<string, string | number | boolean>;

const createInitialConfig = (): BarcodeConfigState => ({
  formName: "BarcodeConfigurationForm",
  cancelAction: "MasterListsPage",
  cancelMethod: "POST",
  numMaxOrderLabels: 10,
  numMaxSpecimenLabels: 10,
  numDefaultOrderLabels: 1,
  numDefaultSpecimenLabels: 1,
  specimenCollectionDateCheck: true,
  specimenCollectedByCheck: false,
  specimenTestsCheck: true,
  specimenPatientSexCheck: false,
  prePrintDontUseAltAccession: true,
  prePrintAltAccessionPrefix: "",
  sitePrefix: "SITE",
});

test.describe("Barcode configuration", () => {
  test("persists max count and optional field toggles after save/reload", async ({
    page,
  }) => {
    let configState = createInitialConfig();

    await page.route("**/rest/BarcodeConfiguration", async (route) => {
      const request = route.request();
      if (request.method() === "GET") {
        await route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify(configState),
        });
        return;
      }
      const payload = await request.postDataJSON();
      configState = { ...configState, ...payload };
      await route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({ status: "ok" }),
      });
    });

    await page.goto("/MasterListsPage/barcodeConfiguration");

    const maxOrderInput = page.getByRole("spinbutton", {
      name: "Order",
      exact: true,
    });
    const collectionDateCheckbox = page.getByLabel("Collection Date and Time");

    await expect(maxOrderInput).toBeVisible();
    await expect(maxOrderInput).toHaveValue("10");
    await expect(collectionDateCheckbox).toBeChecked();

    await maxOrderInput.fill("22");
    // Carbon checkbox: click the specific associated label instead of forcing the hidden input
    await page.locator('label[for="specimenCollectionDateCheck"]').click();
    await page.getByRole("button", { name: "Save" }).click();

    await page.reload();

    await expect(maxOrderInput).toHaveValue("22");
    await expect(collectionDateCheckbox).not.toBeChecked();
  });
});
