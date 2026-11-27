import { test, expect, type Page } from "../../../helpers/test-base";
import { NAV_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";
import { chooseCarbonOption } from "../../../helpers/carbon-select";
import {
  apiPost,
  cellUnder,
  clickAndAwaitPost,
  deactivateByName,
  letterSuffix,
  searchListFor,
} from "../../../helpers/admin-ui";

const PROVIDERS_PAGE = "/MasterListsPage/providerMenu";
const SAVE = /\/rest\/Provider\/FhirUuid/;

// Provider names accept letters only, hence the letter suffix.
const createProvider = (page: Page, lastName: string, active: boolean) =>
  apiPost(page, "/rest/Provider/FhirUuid?fhirUuid=", {
    person: { firstName: "Seeded", lastName },
    active,
  });

async function openProviders(page: Page) {
  await page.goto(PROVIDERS_PAGE, { waitUntil: "domcontentloaded" });
  await expect(page.locator("table tbody tr").first()).toBeVisible({
    timeout: NAV_TIMEOUT,
  });
}

const findProvider = (page: Page, lastName: string) =>
  searchListFor(page, page.locator("#provider-search-bar"), lastName);

/** The Is Active cell of the one provider row the search finds. */
async function activeCell(page: Page, lastName: string) {
  const row = await findProvider(page, lastName);
  return cellUnder(page.locator("table"), row, "Is Active");
}

async function selectProvider(page: Page, lastName: string) {
  const row = await findProvider(page, lastName);
  await row.locator("label").click();
  await expect(row.getByRole("checkbox")).toBeChecked();
}

test.describe("Provider management", () => {
  let created: string[] = [];

  test.beforeEach(() => {
    test.setTimeout(180_000);
    created = [];
  });

  test.afterEach(async ({ page }) => {
    await deactivateByName(page, "provider", created);
  });

  for (const [choice, active] of [
    ["No", false],
    ["Yes", true],
  ] as const) {
    test(`a provider added with Active "${choice}" is listed as ${active}`, async ({
      page,
    }) => {
      const lastName = `PwAdd${choice}${letterSuffix()}`;
      created.push(lastName);
      await openProviders(page);

      await test.step("add the provider through the Add modal", async () => {
        await page.getByRole("button", { name: "Add", exact: true }).click();
        const modal = page
          .getByRole("dialog")
          .filter({ hasText: "Add Provider" });
        await expect(modal).toBeVisible();
        await modal.locator("#lastName").fill(lastName);
        await modal.locator("#firstName").fill("Optimus");
        await chooseCarbonOption(
          modal.getByRole("combobox", { name: "Active" }),
          choice,
        );
        await clickAndAwaitPost(page, SAVE, () =>
          modal.getByRole("button", { name: "Add", exact: true }).click(),
        );
        await expect(modal).toBeHidden({ timeout: UI_TIMEOUT });
      });

      await test.step("the saved provider is found after reload", async () => {
        await page.reload({ waitUntil: "domcontentloaded" });
        await expect(await activeCell(page, lastName)).toHaveText(
          String(active),
        );
        const row = await findProvider(page, lastName);
        await expect(
          await cellUnder(page.locator("table"), row, "Provider Firstname"),
        ).toHaveText("Optimus");
      });
    });
  }

  test("modifying an inactive provider to active is kept after reload", async ({
    page,
  }) => {
    const lastName = `PwModify${letterSuffix()}`;
    created.push(lastName);
    await createProvider(page, lastName, false);
    await openProviders(page);
    await expect(await activeCell(page, lastName)).toHaveText("false");

    await test.step("set Active to Yes in the Update modal", async () => {
      await selectProvider(page, lastName);
      await page.getByRole("button", { name: "Modify", exact: true }).click();
      const modal = page
        .getByRole("dialog")
        .filter({ hasText: "Update Provider" });
      await expect(modal.locator("#lastName")).toHaveValue(lastName);
      await chooseCarbonOption(
        modal.getByRole("combobox", { name: "Active" }),
        "Yes",
      );
      await clickAndAwaitPost(page, SAVE, () =>
        modal.getByRole("button", { name: "Update", exact: true }).click(),
      );
      await expect(modal).toBeHidden({ timeout: UI_TIMEOUT });
    });

    await page.reload({ waitUntil: "domcontentloaded" });
    await expect(await activeCell(page, lastName)).toHaveText("true");
  });

  test("deactivating an active provider marks it inactive", async ({
    page,
  }) => {
    const lastName = `PwDeactivate${letterSuffix()}`;
    created.push(lastName);
    await createProvider(page, lastName, true);
    await openProviders(page);
    await expect(await activeCell(page, lastName)).toHaveText("true");

    await selectProvider(page, lastName);
    await clickAndAwaitPost(page, /\/rest\/DeleteProvider/, () =>
      page.getByRole("button", { name: "Deactivate", exact: true }).click(),
    );

    await page.reload({ waitUntil: "domcontentloaded" });
    await expect(await activeCell(page, lastName)).toHaveText("false");
  });
});
