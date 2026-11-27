import { expect, test, type Page } from "../../../helpers/test-base";
import { UI_TIMEOUT } from "../../../helpers/timeouts";
import { csrfToken } from "../../../helpers/api-session";

// OGC-1227: no label preset could be edited (the editor's PUT was rejected
// 400, a clean PUT on a preset with fields 500ed, a PUT without `fields`
// silently deleted them, a successful save gave no confirmation and the
// client lower-cased the name). Deactivate also POSTed to a PATCH-only
// endpoint. These walks pin the admin surface end to end: the real editor, a
// confirmation the user can see, and the value read back on another surface.

const LIST = "/MasterListsPage/labelPresets";
const API = "/api/OpenELIS-Global/api/labelPresets";

interface Preset {
  id: number;
  name: string;
  heightMm: number;
  widthMm: number;
  barcodeType: string;
  printsPerOrder: boolean;
  printsPerSample: boolean;
  defaultPerOrder: number;
  maxPerOrder: number;
  defaultPerSample: number;
  maxPerSample: number;
  isActive: boolean;
  fields: Array<{ fieldKey: string }>;
}

async function readPreset(page: Page, name: string): Promise<Preset> {
  const res = await page.request.get(API);
  expect(res.status()).toBe(200);
  const all = (await res.json()) as Preset[];
  const hit = all.find((p) => p.name === name);
  expect(hit, `preset "${name}" is listed`).toBeTruthy();
  return hit as Preset;
}

async function restorePreset(page: Page, preset: Preset) {
  const token = await csrfToken(page);
  await page.request.put(`${API}/${preset.id}`, {
    headers: { "X-CSRF-Token": token, "Content-Type": "application/json" },
    data: {
      name: preset.name,
      heightMm: preset.heightMm,
      widthMm: preset.widthMm,
      barcodeType: preset.barcodeType,
      printsPerOrder: preset.printsPerOrder,
      printsPerSample: preset.printsPerSample,
      defaultPerOrder: preset.defaultPerOrder,
      maxPerOrder: preset.maxPerOrder,
      defaultPerSample: preset.defaultPerSample,
      maxPerSample: preset.maxPerSample,
      isActive: preset.isActive,
    },
  });
}

const statusCell = (page: Page, name: string) =>
  page.locator("tr", { hasText: name }).first().locator("td").nth(4);

test.describe("Label presets admin: saving a preset (OGC-1227)", () => {
  test("editing a system preset's height saves, confirms, and reads back", async ({
    page,
  }) => {
    await page.goto(LIST, { waitUntil: "domcontentloaded" });
    const before = await readPreset(page, "Order Label");
    const row = page.locator("tr", { hasText: "Order Label" }).first();
    await expect(row).toBeVisible({ timeout: UI_TIMEOUT });

    try {
      await row.getByRole("button", { name: "Options" }).click();
      await page.getByRole("menuitem", { name: "Edit" }).click();
      const dialog = page.getByRole("dialog");
      await expect(
        dialog.getByRole("heading", { name: "Edit Label Preset" }),
      ).toBeVisible({ timeout: UI_TIMEOUT });

      // A system preset's name is locked, and the stored case must survive.
      await expect(dialog.locator("#preset-name")).toBeDisabled();

      const height = dialog.locator("#preset-heightMm");
      await expect(height).toHaveValue(String(before.heightMm));
      await dialog
        .getByRole("button", { name: "Increment number" })
        .first()
        .click();
      await expect(height).toHaveValue(String(before.heightMm + 1));

      const saved = page.waitForResponse(
        (r) =>
          r.request().method() === "PUT" &&
          /\/api\/labelPresets\/\d+$/.test(r.url()),
      );
      await dialog.getByRole("button", { name: "Save", exact: true }).click();
      await saved;

      await expect(
        page.locator(".cds--toast-notification--success"),
      ).toBeVisible({ timeout: UI_TIMEOUT });
      await expect(page.getByRole("dialog")).toBeHidden({
        timeout: UI_TIMEOUT,
      });
      await expect(row.locator("td").nth(2)).toHaveText(
        `${before.heightMm + 1} x ${before.widthMm} mm`,
        { timeout: UI_TIMEOUT },
      );

      const after = await readPreset(page, "Order Label");
      expect(after.heightMm).toBe(before.heightMm + 1);
      expect(after.widthMm).toBe(before.widthMm);
      expect(after.name).toBe(before.name);
      expect(after.fields.map((f) => f.fieldKey)).toEqual(
        before.fields.map((f) => f.fieldKey),
      );
    } finally {
      await restorePreset(page, before);
    }
  });

  test("creating a preset confirms, and Deactivate / Activate toggle its status", async ({
    page,
  }) => {
    const name = `PW preset ${Date.now()}`;
    await page.goto(LIST, { waitUntil: "domcontentloaded" });
    await page.getByRole("button", { name: /add preset/i }).click();
    const dialog = page.getByRole("dialog");
    await expect(
      dialog.getByRole("heading", { name: "Add Label Preset" }),
    ).toBeVisible({ timeout: UI_TIMEOUT });
    await expect(dialog.locator("#preset-name")).toHaveValue("");
    await dialog.locator("#preset-name").fill(name);

    const created = page.waitForResponse(
      (r) =>
        r.request().method() === "POST" && /\/api\/labelPresets$/.test(r.url()),
    );
    await dialog.getByRole("button", { name: "Save", exact: true }).click();
    await created;

    await expect(page.locator(".cds--toast-notification--success")).toBeVisible(
      { timeout: UI_TIMEOUT },
    );
    const row = page.locator("tr", { hasText: name }).first();
    await expect(row).toBeVisible({ timeout: UI_TIMEOUT });
    await expect(statusCell(page, name)).toHaveText("Active");

    await row.getByRole("button", { name: "Options" }).click();
    const deactivated = page.waitForResponse(
      (r) => r.request().method() === "PATCH" && /\/activate$/.test(r.url()),
    );
    await page.getByRole("menuitem", { name: "Deactivate" }).click();
    await deactivated;
    await expect(statusCell(page, name)).toHaveText("Inactive", {
      timeout: UI_TIMEOUT,
    });

    await row.getByRole("button", { name: "Options" }).click();
    await page.getByRole("menuitem", { name: "Activate", exact: true }).click();
    await expect(statusCell(page, name)).toHaveText("Active", {
      timeout: UI_TIMEOUT,
    });

    // Presets are never hard-deleted; leave the scratch preset inactive.
    await row.getByRole("button", { name: "Options" }).click();
    await page.getByRole("menuitem", { name: "Deactivate" }).click();
    await expect(statusCell(page, name)).toHaveText("Inactive", {
      timeout: UI_TIMEOUT,
    });
  });

  test("a rejected save names the server's reason and keeps the editor open", async ({
    page,
  }) => {
    await page.goto(LIST, { waitUntil: "domcontentloaded" });
    await page.getByRole("button", { name: /add preset/i }).click();
    const dialog = page.getByRole("dialog");
    await expect(
      dialog.getByRole("heading", { name: "Add Label Preset" }),
    ).toBeVisible({ timeout: UI_TIMEOUT });

    // Same name as a system preset, differing only in case: the server's
    // collision check normalises and refuses with 422.
    await dialog.locator("#preset-name").fill("order label");
    const rejected = page.waitForResponse(
      (r) =>
        r.request().method() === "POST" && /\/api\/labelPresets$/.test(r.url()),
    );
    await dialog.getByRole("button", { name: "Save", exact: true }).click();
    await rejected;

    const notice = dialog.getByTestId("label-preset-editor-error");
    await expect(notice).toContainText("422", { timeout: UI_TIMEOUT });
    await expect(notice).toContainText(/already exists/, {
      timeout: UI_TIMEOUT,
    });
    await expect(dialog).toBeVisible();
    await expect(page.locator(".cds--toast-notification--success")).toHaveCount(
      0,
    );
  });
});
