import { test, expect, Page } from "../../../helpers/test-base";
import { createSampleOrder } from "../../../helpers/seed-tat-data";
import { NAV_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";

/**
 * OGC-1422 — Prepare Samples prints barcode labels. The step carries a Labels
 * section fed by the presets and the test catalog; Print all and the sample
 * card's Print Labels save the step when anything is pending, then open the
 * saved labels as a PDF, and the chosen quantities are stored with the order.
 */

const API = "/api/OpenELIS-Global";

async function orderLabelPreset(page: Page) {
  const presets = await (
    await page.request.get(`${API}/api/labelPresets`)
  ).json();
  const list = Array.isArray(presets) ? presets : presets.content || [];
  const preset = list.find((p) => p.isSystem && p.printsPerOrder);
  expect(preset, "a system preset printed per order").toBeTruthy();
  return preset;
}

async function storedLabelRows(page: Page, accessionNumber: string) {
  const response = await page.request.get(
    `${API}/api/orders/by-accession/${encodeURIComponent(accessionNumber)}/labels`,
  );
  expect(response.status()).toBe(200);
  const rows = await response.json();
  return (Array.isArray(rows) ? rows : []).map((row) => ({
    presetId: String(row.presetId ?? row.preset_id),
    sampleItemId: row.sampleItemId ?? row.sample_item_id ?? null,
    qty: row.qty,
  }));
}

test.describe("Prepare Samples label printing (OGC-1422)", () => {
  test("Print all and the sample card save the step and open the labels as a PDF", async ({
    page,
    context,
  }) => {
    const preset = await orderLabelPreset(page);
    const accessionNumber = await createSampleOrder(page, {});
    expect(accessionNumber, "seeded order").not.toBe("");

    await page.goto(
      `/order/clinical/collect?labNumber=${encodeURIComponent(accessionNumber)}`,
      { timeout: NAV_TIMEOUT },
    );
    const section = page.getByTestId("prepare-labels-section");
    await expect(section).toBeVisible({ timeout: NAV_TIMEOUT });

    await test.step("the section proposes the preset's order quantity and one row per tube", async () => {
      await expect(
        section.locator(`#order-label-order-${preset.id}`),
      ).toHaveValue(String(preset.defaultPerOrder), { timeout: UI_TIMEOUT });
      await expect(section).toContainText(`${accessionNumber}-1`);
      await expect(section.getByTestId("labels-pending-save")).toBeVisible();
    });

    await test.step("Print all saves the step, stores the quantities and opens the PDF", async () => {
      const saved = page.waitForResponse(
        (r) =>
          /rest\/SamplePatientEntry$/.test(r.url()) &&
          r.request().method() === "POST",
      );
      const pdf = page.waitForResponse((r) =>
        /\/api\/orders\/\d+\/labels\/pdf(\?|$)/.test(r.url()),
      );
      const popup = context.waitForEvent("page");
      await section
        .getByRole("button", { name: "Print all labels", exact: true })
        .click();
      await saved;
      const pdfResponse = await pdf;
      expect(pdfResponse.status()).toBe(200);
      expect(pdfResponse.headers()["content-type"]).toContain(
        "application/pdf",
      );
      // Headless Chromium downloads a PDF instead of showing it, so the
      // window's address is not asserted; the response above is.
      const printWindow = await popup;
      await printWindow.close();
      await expect(page.getByTestId("prepare-labels-print-error")).toHaveCount(
        0,
      );

      const rows = await storedLabelRows(page, accessionNumber);
      const orderRow = rows.find(
        (row) => row.presetId === String(preset.id) && !row.sampleItemId,
      );
      expect(orderRow, "an order-level row").toBeTruthy();
      expect(orderRow.qty).toBe(preset.defaultPerOrder);
      expect(
        rows.some((row) => row.sampleItemId),
        "a tube row",
      ).toBe(true);
    });

    await test.step("reopening the step shows the saved quantities and a plain save keeps them", async () => {
      const tubeRowId = (await storedLabelRows(page, accessionNumber)).find(
        (row) => row.sampleItemId,
      )!.sampleItemId;
      const tubeInputs = section.locator(
        `input[id^="sample-label-item-${tubeRowId}-"]`,
      );
      await expect(tubeInputs.first()).toBeVisible({ timeout: UI_TIMEOUT });
      await tubeInputs.first().fill("3");
      const savedAgain = page.waitForResponse(
        (r) =>
          /rest\/SamplePatientEntry$/.test(r.url()) &&
          r.request().method() === "POST",
      );
      const popup = context.waitForEvent("page");
      await section
        .getByRole("button", { name: "Print all labels", exact: true })
        .click();
      await savedAgain;
      await (await popup).close();
      await expect
        .poll(
          async () =>
            (await storedLabelRows(page, accessionNumber)).find(
              (row) => row.sampleItemId === tubeRowId,
            )?.qty,
          { timeout: UI_TIMEOUT },
        )
        .toBe(3);

      // Before the fix the reopened step proposed the defaults again and the
      // next save wrote them over the saved rows.
      await page.reload({ waitUntil: "domcontentloaded" });
      await expect(section).toBeVisible({ timeout: NAV_TIMEOUT });
      await expect(tubeInputs.first()).toHaveValue("3", {
        timeout: UI_TIMEOUT,
      });
      await expect(
        section.getByText("Saved", { exact: true }).first(),
      ).toBeVisible();
      await expect(section.getByTestId("labels-pending-save")).toHaveCount(0);

      const plainSave = page.waitForResponse(
        (r) =>
          /rest\/SamplePatientEntry$/.test(r.url()) &&
          r.request().method() === "POST",
      );
      await page
        .locator("main")
        .getByRole("button", { name: "Save and exit", exact: true })
        .click();
      await plainSave;
      await expect
        .poll(
          async () =>
            (await storedLabelRows(page, accessionNumber)).find(
              (row) => row.sampleItemId === tubeRowId,
            )?.qty,
          { timeout: UI_TIMEOUT },
        )
        .toBe(3);
      await page.goto(
        `/order/clinical/collect?labNumber=${encodeURIComponent(accessionNumber)}`,
        { timeout: NAV_TIMEOUT },
      );
      await expect(section).toBeVisible({ timeout: NAV_TIMEOUT });
    });

    await test.step("the sample card's Print Labels prints that tube's labels", async () => {
      const pdf = page.waitForResponse(
        (r) =>
          /\/api\/orders\/\d+\/labels\/pdf\?/.test(r.url()) &&
          r.url().includes("scope=sample") &&
          r.url().includes("sampleItemId="),
      );
      const popup = context.waitForEvent("page");
      // The card's button stays disabled until the order has loaded; before
      // that gate the click saved an order without a lab number (400) and no
      // PDF followed.
      const cardPrint = page
        .locator("main")
        .getByRole("button", { name: "Print Labels", exact: true })
        .first();
      await expect(cardPrint).toBeEnabled({ timeout: UI_TIMEOUT });
      await cardPrint.click();
      const pdfResponse = await pdf;
      expect(pdfResponse.status()).toBe(200);
      const printWindow = await popup;
      await printWindow.close();
      await expect(page.getByTestId("prepare-labels-print-error")).toHaveCount(
        0,
      );
    });
  });
});
