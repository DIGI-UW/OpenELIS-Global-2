import type { Download, Locator, Page, Response } from "@playwright/test";
import { csrfHeaders } from "./authenticated-request";
import {
  clickCarbonModalPrimaryAction,
  setCarbonCheckbox,
  setCarbonToggle,
} from "./carbon";
import { expect } from "./test-base";
import { LONG_TIMEOUT } from "./timeouts";

export interface TextMacroFixture {
  code: string;
  expansionText: string;
  contexts: Array<
    "Culture activity" | "Clinical history" | "Antibiotic exposure"
  >;
}

export interface TextMacroLibraryQuery {
  q?: string;
  context?:
    | "all"
    | "CULTURE_ACTIVITY"
    | "CLINICAL_HISTORY"
    | "ANTIBIOTIC_EXPOSURE";
  status?: "active" | "inactive" | "all";
  sort?: "code:asc" | "code:desc" | "updated:asc" | "updated:desc";
  page?: string;
  pageSize?: string;
}

export interface TextMacroBulkAction {
  codes: string[];
  actionLabel: "Activate" | "Deactivate" | "Remove local phrases";
  dialogName: string;
  confirmLabel: "Activate phrases" | "Deactivate phrases" | "Remove phrases";
}

const adminResponse =
  (pathSuffix = "", method = "GET") =>
  (response: Response) =>
    new URL(response.url()).pathname.endsWith(
      `/rest/text-macros/admin${pathSuffix}`,
    ) &&
    response.request().method() === method &&
    response.ok();

const adminItemResponse = (method: "PUT") => (response: Response) =>
  /\/rest\/text-macros\/admin\/[^/]+$/.test(new URL(response.url()).pathname) &&
  response.request().method() === method &&
  response.ok();

const escapeRegExp = (value: string) =>
  value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");

const contextByLabel = {
  "Culture activity": "MICROBIOLOGY_CULTURE_ACTIVITY",
  "Clinical history": "MICROBIOLOGY_CLINICAL_HISTORY",
  "Antibiotic exposure": "MICROBIOLOGY_ANTIBIOTIC_EXPOSURE",
} as const;

export const textMacroRow = (page: Page, code: string) =>
  page.getByRole("row").filter({ hasText: code });

export async function openTextMacroLibrary(
  page: Page,
  overrides: TextMacroLibraryQuery = {},
) {
  const query = new URLSearchParams({
    q: "",
    context: "all",
    status: "all",
    sort: "code:asc",
    page: "1",
    pageSize: "20",
    ...overrides,
  });
  const loaded = page.waitForResponse(adminResponse());
  await page.goto(`/admin/MacroLibrary?${query}`, {
    waitUntil: "domcontentloaded",
  });
  await loaded;
  await expect(
    page.getByRole("heading", { name: "Macro Library", exact: true }),
  ).toBeVisible({ timeout: LONG_TIMEOUT });
}

export async function ensureTextMacroViaAdmin(
  page: Page,
  macro: TextMacroFixture,
) {
  await openTextMacroLibrary(page, { q: macro.code });

  const row = textMacroRow(page, macro.code);
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
    await setCarbonCheckbox(checkbox, shouldBeChecked);
  }
  const active = dialog.getByRole("switch", { name: "Status" });
  await setCarbonToggle(active);

  const saved = page.waitForResponse(
    editing ? adminItemResponse("PUT") : adminResponse("", "POST"),
  );
  await dialog.getByRole("button", { name: "Save phrase" }).click();
  await saved;
  await expect(dialog).toBeHidden();
  await expect(textMacroRow(page, macro.code)).toContainText(
    macro.expansionText,
    { timeout: LONG_TIMEOUT },
  );
}

export async function ensureTextMacroViaAdminApi(
  browserPage: Page,
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
  const adminPath = "/api/OpenELIS-Global/rest/text-macros/admin";
  const request = browserPage.request;
  const listed = await request.get(`${adminPath}?${query}`);
  expect(listed.ok()).toBeTruthy();
  const page = await listed.json();
  const existing = page.items.find(
    (item: { code: string }) => item.code === macro.code,
  );
  const data = {
    ...(existing ? { id: existing.id } : {}),
    code: macro.code,
    expansionText: macro.expansionText,
    contexts: macro.contexts.map((context) => contextByLabel[context]),
    active: true,
  };
  const headers = await csrfHeaders(browserPage);
  const saved = existing
    ? await request.put(`${adminPath}/${existing.id}`, { data, headers })
    : await request.post(adminPath, { data, headers });
  if (!saved.ok()) {
    throw new Error(
      `Text macro fixture failed: HTTP ${saved.status()} ${await saved.text()}`,
    );
  }
  return saved.json();
}

export async function selectTextMacroRows(page: Page, codes: string[]) {
  for (const code of codes) {
    const row = textMacroRow(page, code);
    await expect(row).toBeVisible({ timeout: LONG_TIMEOUT });
    await setCarbonCheckbox(
      row.getByRole("checkbox", { name: `Select ${code}`, exact: true }),
    );
  }
}

export async function openTextMacroBulkAction(
  page: Page,
  action: TextMacroBulkAction,
) {
  await selectTextMacroRows(page, action.codes);
  await page
    .getByRole("button", { name: action.actionLabel, exact: true })
    .click();
  const dialog = page.getByRole("dialog", {
    name: action.dialogName,
    exact: true,
  });
  await expect(dialog).toBeVisible();
  for (const code of action.codes) {
    await expect(dialog.getByText(code, { exact: true })).toBeVisible();
  }
  return dialog;
}

export async function submitOpenTextMacroBulkAction(
  page: Page,
  dialog: Locator,
  confirmLabel: TextMacroBulkAction["confirmLabel"],
) {
  const saved = page.waitForResponse(adminResponse("/bulk", "POST"));
  await clickCarbonModalPrimaryAction(dialog, confirmLabel);
  const response = await saved;
  await expect(dialog).toBeHidden();
  return response;
}

export async function confirmTextMacroBulkAction(
  page: Page,
  action: TextMacroBulkAction,
) {
  const dialog = await openTextMacroBulkAction(page, action);
  return submitOpenTextMacroBulkAction(page, dialog, action.confirmLabel);
}

export async function exportTextMacroLibrary(page: Page): Promise<Download> {
  const response = page.waitForResponse(adminResponse("/export"));
  const download = page.waitForEvent("download");
  await page
    .getByRole("button", { name: "Export phrases", exact: true })
    .click();
  await response;
  const attachment = await download;
  expect(attachment.suggestedFilename()).toBe("openelis-text-macros.csv");
  return attachment;
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
