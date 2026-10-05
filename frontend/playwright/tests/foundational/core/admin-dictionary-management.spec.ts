import { test, expect, type Page } from "../../../helpers/test-base";
import { NAV_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";
import { chooseCarbonOption } from "../../../helpers/carbon-select";
import {
  API_PREFIX,
  apiPost,
  cellUnder,
  clickAndAwaitPost,
  deactivateByName,
  letterSuffix,
  searchListFor,
} from "../../../helpers/admin-ui";

const DICTIONARY_PAGE = "/MasterListsPage/DictionaryMenu";
const CATEGORY = "VIROLOGY";

type YesNo = "Y" | "N";

interface Entry {
  entry: string;
  abbreviation: string;
  active: YesNo;
}

// Local abbreviations must be unique: a repeat is refused with a 500.
const newEntry = (label: string, active: YesNo): Entry => {
  const suffix = letterSuffix();
  return {
    entry: `Pw${label}${suffix}`,
    abbreviation: `A${suffix}`,
    active,
  };
};

/** Creates an entry through the endpoint the Add modal posts to. */
async function createEntry(page: Page, { entry, abbreviation, active }: Entry) {
  const categories = await page.request.get(
    `${API_PREFIX}/rest/dictionary-categories`,
  );
  const category = (
    (await categories.json()) as { id: string; description: string }[]
  ).find((item) => item.description === CATEGORY);
  if (!category) throw new Error(`no ${CATEGORY} dictionary category`);
  await apiPost(page, "/rest/Dictionary", {
    id: "",
    selectedDictionaryCategoryId: category.id,
    dictEntry: entry,
    localAbbreviation: abbreviation,
    isActive: active,
    loincCode: null,
    dirtyFormFields: "",
  });
}

async function openDictionary(page: Page) {
  await page.goto(DICTIONARY_PAGE, { waitUntil: "domcontentloaded" });
  await expect(
    page.getByRole("heading", { name: "Dictionary Menu", exact: true }),
  ).toBeVisible({ timeout: NAV_TIMEOUT });
  await expect(page.locator("table tbody tr").first()).toBeVisible({
    timeout: NAV_TIMEOUT,
  });
}

const searchBox = (page: Page) => page.locator("#dictionary-entry-search");

const findEntry = (page: Page, entry: string) =>
  searchListFor(page, searchBox(page), entry);

/** The Is Active cell of the one entry row the search finds. */
async function activeCell(page: Page, entry: string) {
  const row = await findEntry(page, entry);
  return cellUnder(page.locator("table"), row, "Is Active");
}

async function selectEntry(page: Page, entry: string) {
  const row = await findEntry(page, entry);
  await row.locator("label").click();
  await expect(row.getByRole("checkbox")).toBeChecked();
}

async function fillAddModal(
  page: Page,
  { entry, abbreviation, active }: Entry,
) {
  await page.getByRole("button", { name: "Add", exact: true }).click();
  const modal = page.getByRole("dialog").filter({ hasText: "Add Dictionary" });
  await expect(modal).toBeVisible();
  await expect(modal.locator("#dictNumber")).toBeDisabled();
  await chooseCarbonOption(
    modal.getByRole("combobox", { name: "Dictionary Category" }),
    CATEGORY,
  );
  await modal.locator("#dictEntry").fill(entry);
  await chooseCarbonOption(
    modal.getByRole("combobox", { name: "Is Active" }),
    active,
  );
  await modal.locator("#localAbbrev").fill(abbreviation);
  return modal;
}

test.describe("Dictionary management", () => {
  let created: string[] = [];

  test.beforeEach(() => {
    test.setTimeout(180_000);
    created = [];
  });

  test.afterEach(async ({ page }) => {
    await deactivateByName(page, "dictionary", created);
  });

  test("Cancel discards a filled-in entry, and Add saves it", async ({
    page,
  }) => {
    const item = newEntry("Cancel", "N");
    created.push(item.entry);
    await openDictionary(page);

    await test.step("Cancel closes the filled-in modal without saving", async () => {
      const cancelled = await fillAddModal(page, item);
      await cancelled
        .getByRole("button", { name: "Cancel", exact: true })
        .click();
      await expect(cancelled).toBeHidden({ timeout: UI_TIMEOUT });

      await page.reload({ waitUntil: "domcontentloaded" });
      await expect(page.locator("table tbody tr").first()).toBeVisible({
        timeout: NAV_TIMEOUT,
      });
      const searched = page.waitForResponse((response) =>
        response.url().includes(`searchString=${item.entry}`),
      );
      await searchBox(page).fill(item.entry);
      await searched;
      await expect(
        page.locator("table tbody tr").filter({ hasText: item.entry }),
      ).toHaveCount(0);
    });

    await test.step("Add saves the same entry", async () => {
      await searchBox(page).clear();
      const added = await fillAddModal(page, item);
      await clickAndAwaitPost(page, /\/rest\/Dictionary$/, () =>
        added.getByRole("button", { name: "Add", exact: true }).click(),
      );
      await expect(added).toBeHidden({ timeout: UI_TIMEOUT });

      await page.reload({ waitUntil: "domcontentloaded" });
      const row = await findEntry(page, item.entry);
      const table = page.locator("table");
      await expect(
        await cellUnder(table, row, "Local Abbreviation"),
      ).toHaveText(item.abbreviation);
      await expect(await cellUnder(table, row, "Is Active")).toHaveText("N");
    });

    await test.step("Modify shows the saved category", async () => {
      await selectEntry(page, item.entry);
      await page.getByRole("button", { name: "Modify", exact: true }).click();
      const edit = page
        .getByRole("dialog")
        .filter({ hasText: "Edit Dictionary" });
      await expect(edit.locator("#dictEntry")).toHaveValue(item.entry);
      await expect(
        edit.getByRole("combobox", { name: "Dictionary Category" }),
      ).toContainText(CATEGORY);
    });
  });

  test("Modify makes an inactive entry active, kept after reload", async ({
    page,
  }) => {
    const item = newEntry("Modify", "N");
    created.push(item.entry);
    await createEntry(page, item);
    await openDictionary(page);
    await expect(await activeCell(page, item.entry)).toHaveText("N");

    await selectEntry(page, item.entry);
    await page.getByRole("button", { name: "Modify", exact: true }).click();
    const edit = page
      .getByRole("dialog")
      .filter({ hasText: "Edit Dictionary" });
    await expect(edit.locator("#dictEntry")).toHaveValue(item.entry);
    await chooseCarbonOption(
      edit.getByRole("combobox", { name: "Is Active" }),
      "Y",
    );
    await clickAndAwaitPost(page, /\/rest\/Dictionary\?ID=/, () =>
      edit.getByRole("button", { name: "Update", exact: true }).click(),
    );

    await page.reload({ waitUntil: "domcontentloaded" });
    await expect(await activeCell(page, item.entry)).toHaveText("Y");
  });

  test("Deactivate marks an active entry inactive", async ({ page }) => {
    const item = newEntry("Deactivate", "Y");
    created.push(item.entry);
    await createEntry(page, item);
    await openDictionary(page);
    await expect(await activeCell(page, item.entry)).toHaveText("Y");

    await selectEntry(page, item.entry);
    await clickAndAwaitPost(page, /\/rest\/DeleteDictionary/, () =>
      page.getByRole("button", { name: "Deactivate", exact: true }).click(),
    );

    await page.reload({ waitUntil: "domcontentloaded" });
    await expect(await activeCell(page, item.entry)).toHaveText("N");
  });
});
