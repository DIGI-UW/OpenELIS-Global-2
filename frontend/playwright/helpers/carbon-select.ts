import { Locator, expect } from "@playwright/test";

/**
 * Carbon's Dropdown, ComboBox and MultiSelect are built on downshift, which
 * (since downshift 6) calls `onChange` from an effect that runs after the click
 * has been handled. Playwright's click therefore returns before the
 * application has seen the choice, and a following step can act on the screen
 * as it was. These helpers finish only when the control shows the choice.
 *
 * For a controlled component (`selectedItem`/`selectedItems` from application
 * state) the control shows the new choice only after `onChange` has run and
 * the screen has re-rendered, so that is the point where the choice has taken
 * effect. An uncontrolled component shows it at once; after choosing from one,
 * assert the outcome the next step depends on.
 *
 * Options are looked up in the control's own listbox (`aria-controls`), so
 * another select on the page cannot match first.
 */

async function openListbox(combobox: Locator, filter?: string) {
  if (filter !== undefined) await combobox.fill(filter);
  else if ((await combobox.getAttribute("aria-expanded")) !== "true")
    await combobox.click();
  await expect(combobox).toHaveAttribute("aria-expanded", "true");
  const listboxId = await combobox.getAttribute("aria-controls");
  if (!listboxId) throw new Error("Carbon select has no controlled listbox");
  return combobox.page().locator(`[id="${listboxId}"]`);
}

const optionIn = (listbox: Locator, option: string | RegExp) =>
  listbox.getByRole("option", {
    name: option,
    exact: typeof option === "string",
  });

const isEditable = (combobox: Locator) =>
  combobox.evaluate((element) => element.matches("input"));

// A filterable control (ComboBox, FilterableMultiSelect) is typed into to
// narrow the list to a string option.
const filterFor = (editable: boolean, option: string | RegExp) =>
  editable && typeof option === "string" ? option : undefined;

/**
 * Choose an option from a Carbon Dropdown or ComboBox. `shown` is the text the
 * control displays afterwards when it differs from the option's accessible
 * name.
 */
export async function chooseCarbonOption(
  combobox: Locator,
  option: string | RegExp,
  shown: string | RegExp = option,
): Promise<void> {
  const editable = await isEditable(combobox);
  const listbox = await openListbox(combobox, filterFor(editable, option));
  await optionIn(listbox, option).click();
  if (editable) await expect(combobox).toHaveValue(shown);
  else await expect(combobox).toContainText(shown);
}

/**
 * Tick an option in a Carbon MultiSelect or FilterableMultiSelect. The menu
 * stays open and the control shows only a count, so the option's
 * `aria-selected`, which Carbon derives from `selectedItems`, is the signal.
 * The menu is left open for further picks.
 */
export async function tickCarbonMultiSelectOption(
  combobox: Locator,
  option: string | RegExp,
): Promise<void> {
  const editable = await isEditable(combobox);
  const choice = optionIn(
    await openListbox(combobox, filterFor(editable, option)),
    option,
  );
  await choice.click();
  await expect(choice).toHaveAttribute("aria-selected", "true");
}
