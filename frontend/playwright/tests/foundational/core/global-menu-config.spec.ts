import type { Page } from "@playwright/test";
import { expect, test } from "../../../helpers/test-base";
import { LONG_TIMEOUT } from "../../../helpers/timeouts";

// This database-backed child remains editable in the Reporting instance profile.
const menuId = "menu_workplan_priority";

async function openSettings(page: Page) {
  if (new URL(page.url()).pathname === "/Dashboard") {
    const navigation = page.getByRole("navigation", {
      name: "Side navigation",
    });
    const adminLink = navigation.getByRole("link", {
      name: "Admin",
      exact: true,
    });
    const adminGroup = navigation.getByRole("button", {
      name: "Admin",
      exact: true,
    });
    await expect(adminLink.or(adminGroup)).toBeVisible();
    // The standard profile groups Admin dashboard under Admin; the Reporting
    // profile makes Admin a direct link to the same administration page.
    if (await adminGroup.count()) {
      await adminGroup.click();
      await navigation
        .getByRole("link", { name: "Admin dashboard", exact: true })
        .click();
    } else {
      await adminLink.click();
    }
    await navigation
      .getByRole("button", { name: "Menu Configuration", exact: true })
      .click();
    await navigation
      .getByRole("link", { name: "Global Menu Configuration", exact: true })
      .click();
  } else {
    await page.goto("/MasterListsPage/globalMenuManagement");
  }
  const item = page.getByTestId(`menu-settings-${menuId}`);
  await expect(item).toBeAttached({ timeout: LONG_TIMEOUT });
  // Walk the rendered configuration hierarchy so the same workflow works with
  // database defaults and with instance-defined parent sections.
  const ancestors = await item.evaluate((element) => {
    const ids: string[] = [];
    for (let node: Element | null = element; node; node = node.parentElement) {
      const id = node.getAttribute("data-testid");
      if (id?.startsWith("menu-settings-")) ids.unshift(id);
    }
    return ids;
  });
  for (const id of ancestors) {
    // MenuSettings renders exactly one Carbon accordion trigger per item.
    const trigger = page.getByTestId(id).locator(":scope > button");
    await expect(trigger).toHaveCount(1);
    if ((await trigger.getAttribute("aria-expanded")) !== "true") {
      await trigger.click();
    }
  }
  return page.getByTestId(`menu-fields-${menuId}`);
}

test("global menu changes persist and update the application navigation", async ({
  page,
}, testInfo) => {
  testInfo.setTimeout(90_000);
  const runtimeErrors: string[] = [];
  page.on("pageerror", (error) => runtimeErrors.push(error.message));
  page.on("console", (message) => {
    if (message.type() === "error" && message.text().includes("TypeError")) {
      runtimeErrors.push(message.text());
    }
  });
  const fields = await openSettings(page);
  const active = fields.getByRole("switch", { name: "Active", exact: true });
  const icon = fields.getByRole("combobox", { name: "Icon", exact: true });
  const style = fields.getByRole("combobox", {
    name: "Display as",
    exact: true,
  });
  await expect(active).toBeEnabled();
  await expect(icon).toBeEnabled();
  const previous = {
    active: await active.isChecked(),
    icon: await icon.inputValue(),
    style: await style.inputValue(),
  };
  const changedIcon = previous.icon === "patient" ? "reports" : "patient";
  const form = page.getByRole("form", {
    name: "Global Menu Management",
    exact: true,
  });
  const save = form.getByRole("button", { name: "Save", exact: true });
  const saved = form.getByText("Menu settings saved.", { exact: true });
  const toggle = fields.locator(`label[for="${menuId}-active"]`);
  try {
    await toggle.click();
    await expect(active).toBeChecked({ checked: !previous.active });
    await icon.selectOption(changedIcon);
    await save.click();
    await expect(saved).toBeVisible();
    await openSettings(page);
    await expect(active).toBeChecked({ checked: !previous.active });
    await expect(icon).toHaveValue(changedIcon);
    await expect(style).toHaveValue(previous.style);
    await fields.scrollIntoViewIfNeeded();
    await page.screenshot({ path: testInfo.outputPath("menu-persisted.png") });
    await page.getByTestId("admin-back-to-main-nav").click();
    await expect(
      page.getByRole("navigation", { name: "Side navigation" }),
    ).toBeVisible();
    await expect(page.locator("#menu_workplan")).toBeVisible();
    await expect(page.locator(`#${menuId}`)).toHaveCount(
      previous.active ? 0 : 1,
    );
  } finally {
    await openSettings(page);
    if ((await active.isChecked()) !== previous.active) await toggle.click();
    await icon.selectOption(previous.icon);
    if (await save.isEnabled()) {
      await save.click();
      await expect(saved).toBeVisible();
    }
    await openSettings(page);
    await expect(active).toBeChecked({ checked: previous.active });
    await expect(icon).toHaveValue(previous.icon);
    await expect(style).toHaveValue(previous.style);
    await page.getByTestId("admin-back-to-main-nav").click();
    await expect(
      page.getByRole("navigation", { name: "Side navigation" }),
    ).toBeVisible();
    await expect(page.locator("#menu_workplan")).toBeVisible();
    await expect(page.locator(`#${menuId}`)).toHaveCount(
      previous.active ? 1 : 0,
    );
  }
  await expect(page.getByText("In Progress", { exact: true })).toBeVisible();
  await expect(page.locator(".cds--loading-overlay")).toHaveCount(0);
  expect(runtimeErrors).toEqual([]);
  await page.screenshot({ path: testInfo.outputPath("dashboard-return.png") });
});
