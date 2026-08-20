import type { Locator, Page, Response } from "@playwright/test";
import { expect } from "./test-base";
import { LONG_TIMEOUT } from "./timeouts";

export interface TextMacroFixture {
  code: string;
  expansionText: string;
  contexts: Array<
    "Culture activity" | "Clinical history" | "Antibiotic exposure"
  >;
}

const adminResponse =
  (method = "GET") =>
  (response: Response) =>
    response.url().includes("/rest/text-macros/admin") &&
    response.request().method() === method &&
    response.ok();

const escapeRegExp = (value: string) =>
  value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");

export async function ensureTextMacroViaAdmin(
  page: Page,
  macro: TextMacroFixture,
) {
  const query = new URLSearchParams({
    q: macro.code,
    context: "all",
    status: "all",
    sort: "code:asc",
    page: "1",
    pageSize: "20",
  });
  const loaded = page.waitForResponse(adminResponse());
  await page.goto(`/admin/MacroLibrary?${query}`, {
    waitUntil: "domcontentloaded",
  });
  await loaded;
  await expect(
    page.getByRole("heading", { name: "Macro Library", exact: true }),
  ).toBeVisible({ timeout: LONG_TIMEOUT });

  const row = page.getByRole("row").filter({ hasText: macro.code });
  const editing = (await row.count()) > 0;
  if (editing) {
    await row.getByRole("button", { name: "Phrase actions" }).click();
    await page.getByRole("menuitem", { name: "Edit" }).click();
  } else {
    await page.getByRole("button", { name: "Add phrase" }).click();
  }

  const dialog = page.getByRole("dialog");
  await expect(dialog).toBeVisible();
  await dialog.getByLabel("Shortcut code").fill(macro.code);
  await dialog.getByLabel("Phrase text").fill(macro.expansionText);
  for (const context of [
    "Culture activity",
    "Clinical history",
    "Antibiotic exposure",
  ] as const) {
    const checkbox = dialog.getByLabel(context);
    const shouldBeChecked = macro.contexts.includes(context);
    if ((await checkbox.isChecked()) !== shouldBeChecked) {
      await checkbox.click();
    }
  }
  const active = dialog.getByRole("switch", { name: "Status" });
  if ((await active.getAttribute("aria-checked")) !== "true") {
    await active.click();
  }

  const saved = page.waitForResponse(adminResponse(editing ? "PUT" : "POST"));
  await dialog.getByRole("button", { name: "Save phrase" }).click();
  await saved;
  await expect(dialog).toBeHidden();
  await expect(
    page.getByRole("row").filter({ hasText: macro.code }),
  ).toContainText(macro.expansionText, { timeout: LONG_TIMEOUT });
}

export async function expandTextMacro(
  page: Page,
  field: Locator,
  macro: Pick<TextMacroFixture, "code" | "expansionText">,
  prefix = "",
) {
  const loaded = page.waitForResponse(
    (response) =>
      response.url().includes("/rest/text-macros?") &&
      response.request().method() === "GET" &&
      response.ok(),
  );
  await field.focus();
  await loaded;
  await field.fill(`${prefix}${macro.code}`);
  await expect(
    page.getByRole("option", {
      name: new RegExp(
        `${escapeRegExp(macro.code)}.*${escapeRegExp(macro.expansionText)}`,
      ),
    }),
  ).toBeVisible();
  await field.press("Space");
  await expect(field).toHaveValue(`${prefix}${macro.expansionText} `);
}
