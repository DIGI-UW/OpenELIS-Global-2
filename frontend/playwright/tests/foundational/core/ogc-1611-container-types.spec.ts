import { test, expect } from "../../../helpers/test-base";
import { NAV_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";

/**
 * OGC-1611 P1: Container Types beside Sample Types. An install is seeded with
 * the clinical containers; a container type is added and edited in its row,
 * refused with the field that is wrong, and deactivated after a warning, never
 * deleted.
 */

test.describe("OGC-1611 container types", () => {
  test("seeded, added, refused, deactivated and activated again", async ({
    page,
  }) => {
    const code = `QA${Date.now().toString().slice(-8)}`;
    const name = `QA container ${code}`;
    await page.goto("/MasterListsPage/ContainerTypes", {
      waitUntil: "domcontentloaded",
    });
    const table = page.getByTestId("container-types");

    await test.step("The clinical containers are seeded with their cap colours", async () => {
      const sst = page.getByTestId("container-type-SST");
      await expect(sst).toContainText("Serum separator tube (SST)", {
        timeout: NAV_TIMEOUT,
      });
      await expect(sst).toContainText("Clinical");
      await expect(sst).toContainText("Clot activator, gel");
      await page.locator("#container-types-search").fill("K2EDTA tube");
      await expect(table.locator("tbody tr")).toHaveCount(2, {
        timeout: UI_TIMEOUT,
      });
      await page.locator("#container-types-search").fill("");
    });

    await test.step("A container type is added for the environmental domain", async () => {
      await page.getByTestId("container-type-add").click();
      await page.getByLabel("Name *").fill(name);
      await page.getByLabel("Code *").fill(code.toLowerCase());
      await page.locator("label[for=container-type-domain-CLINICAL]").click();
      await page
        .locator("label[for=container-type-domain-ENVIRONMENTAL]")
        .click();
      await page.locator("#container-type-additive").fill("Sodium thiosulfate");
      await page.getByTestId("container-type-save").click();
      const row = page.getByTestId(`container-type-${code}`);
      await expect(row).toContainText(name, { timeout: UI_TIMEOUT });
      await expect(row).toContainText("Environmental");
      await expect(row).not.toContainText("Clinical");
    });

    await test.step("A code already in use is refused on the Code field", async () => {
      await page.getByTestId("container-type-add").click();
      await page.getByLabel("Name *").fill(`${name} again`);
      await page.getByLabel("Code *").fill(code);
      await page.getByTestId("container-type-save").click();
      await expect(
        page.getByText("Another container type has this code."),
      ).toBeVisible({ timeout: UI_TIMEOUT });
      await page
        .getByTestId("container-type-form-new")
        .getByRole("button", { name: "Cancel", exact: true })
        .click();
    });

    await test.step("Deactivating warns what still uses it and keeps it", async () => {
      const row = page.getByTestId(`container-type-${code}`);
      await row
        .getByRole("button", { name: "Deactivate", exact: true })
        .click();
      await expect(page.getByTestId("container-type-usage")).toHaveText(
        "Used by 0 tests and 0 samples. Existing samples keep it; it can no longer be proposed or chosen.",
      );
      await page
        .getByRole("button", { name: "danger Deactivate", exact: true })
        .click();
      await expect(row).toHaveCount(0, { timeout: UI_TIMEOUT });

      await page.locator("label[for=container-types-show-inactive]").click();
      await expect(row).toContainText("Deactivated", { timeout: UI_TIMEOUT });
      await row.getByRole("button", { name: "Activate", exact: true }).click();
      await expect(row).toContainText("Active", { timeout: UI_TIMEOUT });
    });
  });
});
