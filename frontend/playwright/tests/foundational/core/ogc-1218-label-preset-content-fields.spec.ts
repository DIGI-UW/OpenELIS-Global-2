import { test, expect, Page } from "../../../helpers/test-base";
import { createSampleOrder } from "../../../helpers/seed-tat-data";
import { NAV_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";
import { csrfToken } from "../../../helpers/api-session";

/**
 * OGC-1218 — the per-preset content fields editor. The data model shipped
 * with OGC-285 (label_preset_field) and the system presets carried only
 * LAB_NUMBER; the editor rendered nothing for it and a printed label showed
 * raw keys. Now the editor lists the fields by name with Lab Number locked at
 * position 1, lets an administrator add, reorder (buttons, so the keyboard
 * works) and mark fields required, hints softly when they are unlikely to fit,
 * the server keeps Lab Number first and refuses unknown keys, the system
 * presets carry their shipped defaults, and a label printed from a saved order
 * names its fields.
 */

const API = "/api/OpenELIS-Global";
const LIST = "/MasterListsPage/labelPresets";
const PRESETS = `${API}/api/labelPresets`;
const NAME = `OGC1218 ${Date.now().toString(36).toUpperCase()}`;

interface Field {
  fieldKey: string;
  isRequired: boolean;
  displayOrder: number;
}

async function headers(page: Page) {
  return {
    "X-CSRF-Token": await csrfToken(page),
    "Content-Type": "application/json",
  };
}

async function presetByName(page: Page, name: string) {
  const all = await (await page.request.get(PRESETS)).json();
  const list = Array.isArray(all) ? all : all.content || [];
  return list.find((p) => p.name === name) || null;
}

const keysOf = (fields: Field[]) =>
  [...fields]
    .sort((a, b) => a.displayOrder - b.displayOrder)
    .map((f) => `${f.fieldKey}@${f.displayOrder}${f.isRequired ? "!" : ""}`);

async function openEditor(page: Page, name: string) {
  const row = page.locator("tr", { hasText: name }).first();
  await expect(row).toBeVisible({ timeout: UI_TIMEOUT });
  await row.getByRole("button", { name: "Options" }).click();
  await page.getByRole("menuitem", { name: "Edit" }).click();
  const dialog = page.getByRole("dialog");
  await expect(
    dialog.getByRole("heading", { name: "Edit Label Preset" }),
  ).toBeVisible({ timeout: UI_TIMEOUT });
  return dialog;
}

test.describe("Label preset content fields (OGC-1218)", () => {
  test("the system presets carry their shipped defaults, shown by name with Lab Number locked first", async ({
    page,
  }) => {
    const specimen = await presetByName(page, "Specimen Label");
    expect(specimen, "system Specimen Label preset").toBeTruthy();
    expect(keysOf(specimen.fields)).toEqual([
      "LAB_NUMBER@1!",
      "PATIENT_NAME@2",
      "PATIENT_DOB@3",
      "PATIENT_ID@4",
      "PATIENT_SEX@5",
      "COLLECTION_DATETIME@6",
      "COLLECTED_BY@7",
      "TESTS@8",
    ]);

    await page.goto(LIST, { waitUntil: "domcontentloaded" });
    const dialog = await openEditor(page, "Specimen Label");
    const fields = dialog.getByTestId("label-preset-fields");
    await expect(fields).toBeVisible();
    const labNumber = fields.getByTestId("label-field-row-LAB_NUMBER");
    await expect(labNumber).toContainText("Lab Number");
    await expect(labNumber).toContainText("Position 1");
    await expect(labNumber).toContainText("Always first");
    await expect(labNumber.getByRole("button")).toHaveCount(0);
    await expect(fields.getByTestId("label-field-row-TESTS")).toContainText(
      "Tests",
    );
    await expect(fields.getByTestId("label-field-row-TESTS")).toContainText(
      "Position 8",
    );
    await dialog.getByRole("button", { name: "Cancel" }).click();
  });

  test("an administrator adds, reorders and requires fields; the server keeps Lab Number first and refuses unknown keys", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const created = await page.request.post(PRESETS, {
      headers: await headers(page),
      data: {
        name: NAME,
        heightMm: 25,
        widthMm: 76,
        barcodeType: "CODE_128",
        printsPerOrder: false,
        printsPerSample: true,
        defaultPerOrder: 0,
        maxPerOrder: 10,
        defaultPerSample: 1,
        maxPerSample: 10,
        isActive: true,
        fields: [
          { fieldKey: "PATIENT_NAME", isRequired: false, displayOrder: 5 },
        ],
      },
    });
    expect(created.status()).toBe(201);
    const preset = await presetByName(page, NAME);
    expect(
      keysOf(preset.fields),
      "Lab Number is added first by the server",
    ).toEqual(["LAB_NUMBER@1!", "PATIENT_NAME@2"]);

    try {
      await test.step("the editor adds Specimen Type, moves it above Patient Name, requires it and warns about the fit", async () => {
        await page.goto(LIST, { waitUntil: "domcontentloaded" });
        const dialog = await openEditor(page, NAME);
        const fields = dialog.getByTestId("label-preset-fields");
        await fields.getByLabel("Add a field").selectOption("SPECIMEN_TYPE");
        await fields.getByTestId("label-field-add-button").click();
        await expect(
          fields.getByTestId("label-field-row-SPECIMEN_TYPE"),
        ).toContainText("Position 3");
        await fields
          .getByRole("button", { name: "Move Specimen Type up" })
          .click();
        await expect(
          fields.getByTestId("label-field-row-SPECIMEN_TYPE"),
        ).toContainText("Position 2");
        await expect(
          fields.getByTestId("label-field-row-PATIENT_NAME"),
        ).toContainText("Position 3");
        await fields
          .getByTestId("label-field-row-SPECIMEN_TYPE")
          .locator('label[for="label-field-required-SPECIMEN_TYPE"]')
          .click();
        await expect(
          fields.locator("#label-field-required-SPECIMEN_TYPE"),
        ).toBeChecked();
        await expect(fields.getByTestId("label-fields-fit-hint")).toHaveCount(
          0,
        );

        const height = dialog.locator("#preset-heightMm");
        await height.fill("8");
        await expect(fields.getByTestId("label-fields-fit-hint")).toBeVisible();
        await expect(fields.getByTestId("label-fields-fit-hint")).toContainText(
          "You can still save.",
        );

        const saved = page.waitForResponse(
          (r) =>
            r.url().includes("/api/labelPresets/") &&
            r.request().method() === "PUT",
        );
        await dialog.getByRole("button", { name: "Save" }).click();
        expect((await saved).status()).toBe(200);
        await expect(page.getByText("Preset updated")).toBeVisible({
          timeout: UI_TIMEOUT,
        });
      });

      await test.step("the rows are stored in that order and shown by name on reopening", async () => {
        const stored = await presetByName(page, NAME);
        expect(keysOf(stored.fields)).toEqual([
          "LAB_NUMBER@1!",
          "SPECIMEN_TYPE@2!",
          "PATIENT_NAME@3",
        ]);
        expect(stored.heightMm).toBe(8);
        await page.reload({ waitUntil: "domcontentloaded" });
        const dialog = await openEditor(page, NAME);
        const fields = dialog.getByTestId("label-preset-fields");
        await expect(
          fields.getByTestId("label-field-row-SPECIMEN_TYPE"),
        ).toContainText("Specimen Type");
        await expect(
          fields.getByTestId("label-field-row-SPECIMEN_TYPE"),
        ).toContainText("Position 2");
        await fields
          .getByRole("button", { name: "Remove Patient Name" })
          .click();
        await expect(
          fields.getByTestId("label-field-row-PATIENT_NAME"),
        ).toHaveCount(0);
        await dialog.getByRole("button", { name: "Cancel" }).click();
      });

      await test.step("the API refuses an unknown key and keeps Lab Number when a client omits it", async () => {
        const base = {
          name: NAME,
          heightMm: 25,
          widthMm: 76,
          barcodeType: "CODE_128",
          printsPerOrder: false,
          printsPerSample: true,
          defaultPerOrder: 0,
          maxPerOrder: 10,
          defaultPerSample: 1,
          maxPerSample: 10,
          isActive: true,
        };
        const refused = await page.request.put(`${PRESETS}/${preset.id}`, {
          headers: await headers(page),
          data: {
            ...base,
            fields: [
              {
                fieldKey: "FAVOURITE_COLOUR",
                isRequired: false,
                displayOrder: 2,
              },
            ],
          },
        });
        expect(refused.status()).toBe(422);
        expect(await refused.text()).toContain(
          "error.labelpreset.field.key.unknown",
        );

        const accepted = await page.request.put(`${PRESETS}/${preset.id}`, {
          headers: await headers(page),
          data: {
            ...base,
            fields: [{ fieldKey: "TESTS", isRequired: false, displayOrder: 1 }],
          },
        });
        expect(accepted.status()).toBe(200);
        expect(keysOf((await presetByName(page, NAME)).fields)).toEqual([
          "LAB_NUMBER@1!",
          "TESTS@2",
        ]);
      });
    } finally {
      const token = await csrfToken(page);
      const current = await presetByName(page, NAME);
      if (current) {
        await page.request.patch(`${PRESETS}/${current.id}/activate`, {
          headers: {
            "X-CSRF-Token": token,
            "Content-Type": "application/json",
          },
          data: { isActive: false },
        });
      }
    }
  });

  test("a label printed from a saved order names its fields instead of showing keys", async ({
    page,
    context,
  }) => {
    test.setTimeout(180_000);
    const accessionNumber = await createSampleOrder(page, {});
    expect(accessionNumber, "seeded order").not.toBe("");
    await page.goto(
      `/order/clinical/collect?labNumber=${encodeURIComponent(accessionNumber)}`,
      { timeout: NAV_TIMEOUT },
    );
    const section = page.getByTestId("prepare-labels-section");
    await expect(section).toBeVisible({ timeout: NAV_TIMEOUT });
    const printAll = section.getByRole("button", {
      name: "Print all labels",
      exact: true,
    });
    await expect(printAll).toBeEnabled({ timeout: UI_TIMEOUT });

    const pdf = page.waitForResponse((r) =>
      /\/api\/orders\/\d+\/labels\/pdf/.test(r.url()),
    );
    const popup = context.waitForEvent("page");
    await printAll.click();
    const pdfResponse = await pdf;
    expect(pdfResponse.status()).toBe(200);
    expect(pdfResponse.headers()["content-type"]).toContain("application/pdf");
    await (await popup).close();

    const rows = await (
      await page.request.get(
        `${API}/api/orders/by-accession/${encodeURIComponent(accessionNumber)}/labels`,
      )
    ).json();
    expect(rows.length).toBeGreaterThan(0);
    const labels = rows.flatMap((row) =>
      (row.preset_snapshot?.fields || []).map(
        (f) => `${f.field_key ?? f.fieldKey}=${f.field_label ?? f.fieldLabel}`,
      ),
    );
    expect(labels).toContain("LAB_NUMBER=Accession number");
    expect(labels).toContain("PATIENT_NAME=Patient Name");
    expect(
      labels.some((l) => /=[A-Z_]+$/.test(l)),
      "no raw key as a label",
    ).toBe(false);
  });
});
