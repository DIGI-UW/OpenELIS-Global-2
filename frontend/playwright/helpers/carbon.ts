import type { Locator } from "@playwright/test";
import { expect } from "./test-base";

export async function setCarbonCheckbox(checkbox: Locator, checked = true) {
  if ((await checkbox.isChecked()) === checked) return;

  const id = await checkbox.getAttribute("id");
  if (!id) throw new Error("Carbon checkbox must have an associated label id");

  const label = checkbox.locator("xpath=following-sibling::label");
  await expect(label).toHaveAttribute("for", id);
  await label.click();
  if (checked) await expect(checkbox).toBeChecked();
  else await expect(checkbox).not.toBeChecked();
}

export async function setCarbonToggle(toggle: Locator, checked = true) {
  if ((await toggle.getAttribute("aria-checked")) === String(checked)) return;

  const id = await toggle.getAttribute("id");
  if (!id) throw new Error("Carbon toggle must have an associated label id");

  const label = toggle.locator("xpath=following-sibling::label");
  await expect(label).toHaveAttribute("for", id);
  await label.click();
  await expect(toggle).toHaveAttribute("aria-checked", String(checked));
}

function escapeRegExp(value: string) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

export async function clickCarbonModalPrimaryAction(
  dialog: Locator,
  label: string,
) {
  const action = dialog.getByRole("button", {
    name: new RegExp(`${escapeRegExp(label)}$`),
  });
  await expect(action).toBeEnabled();
  await action.click();
}
