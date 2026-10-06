import { test, expect } from "../../../helpers/test-base";
import { createSampleOrder } from "../../../helpers/seed-tat-data";
import { openSideNavItem } from "../../../helpers/sidenav";
import {
  LONG_TIMEOUT,
  NAV_TIMEOUT,
  UI_TIMEOUT,
} from "../../../helpers/timeouts";

/**
 * Print Bar Code Labels page (Order > Print Bar Code Labels).
 *
 * Pre-print: the Pre-Print Labels button stays disabled until at least one
 * test is chosen; choosing a panel for a sample type picks its tests, enables
 * the button, and printing renders the label frame for exactly those tests
 * and the chosen site.
 *
 * Existing order: submitting a freshly seeded accession shows that order's
 * patient and the print-set, reprint and per-label controls for its order and
 * specimen labels. An unknown accession shows no patient.
 */

// Reference catalog rows the foundational fixtures load; read, never changed.
const SITE_QUERY = "CAMES";
const SITE_NAME = "279 - CAMES MAN";
const SITE_ID = "9000100";
const SAMPLE_TYPE = "Serum";
const PANEL = "Bilan Biochimique";
const PANEL_TEST = "GPT/ALAT";

test.describe("Print Bar Code Labels", () => {
  test("pre-print is enabled only once a panel is chosen and prints labels for its tests at the chosen site", async ({
    page,
  }) => {
    const prePrint = page.getByRole("button", { name: "Pre-Print Labels" });

    await test.step("open the page from the side nav", async () => {
      await page.goto("/", { waitUntil: "domcontentloaded" });
      await openSideNavItem(page, ["menu_sample", "menu_sample_print_barcode"]);
      await expect(page).toHaveURL(/\/PrintBarcode$/, { timeout: NAV_TIMEOUT });
      await expect(
        page.getByRole("heading", { name: "Print Bar Code Labels" }),
      ).toBeVisible({ timeout: NAV_TIMEOUT });
      await expect(prePrint).toBeDisabled();
    });

    await test.step("choose the site", async () => {
      const site = page.getByRole("textbox", { name: "Search Site Name" });
      await site.fill(SITE_QUERY);
      await page.getByRole("listitem").filter({ hasText: SITE_NAME }).click();
      await expect(site).toHaveValue(SITE_NAME);
      await expect(prePrint).toBeDisabled();
    });

    const panel = page.getByRole("checkbox", { name: PANEL, exact: true });
    const panelTest = page.getByRole("checkbox", {
      name: PANEL_TEST,
      exact: true,
    });

    await test.step("choose the sample type", async () => {
      await page
        .locator("select#selectSampleType")
        .selectOption({ label: SAMPLE_TYPE });
      await expect(panel).toBeVisible({ timeout: UI_TIMEOUT });
      await expect(panel).not.toBeChecked();
      // A sample type alone chooses no test.
      await expect(prePrint).toBeDisabled();
    });

    await test.step("tick the panel", async () => {
      await page
        .locator(`label[for="${await panel.getAttribute("id")}"]`)
        .click();
      await expect(panel).toBeChecked();
      await expect(panelTest).toBeChecked();
      await expect(prePrint).toBeEnabled();
    });

    await test.step("unticking the panel disables pre-print again", async () => {
      await page
        .locator(`label[for="${await panel.getAttribute("id")}"]`)
        .click();
      await expect(panel).not.toBeChecked();
      await expect(panelTest).not.toBeChecked();
      await expect(prePrint).toBeDisabled();
      await page
        .locator(`label[for="${await panel.getAttribute("id")}"]`)
        .click();
      await expect(prePrint).toBeEnabled();
    });

    await test.step("pre-print renders the label frame for the panel's tests", async () => {
      const panelTestId = (await panelTest.getAttribute("id"))?.replace(
        /^test_/,
        "",
      );
      await prePrint.click();
      await expect(
        page.getByRole("heading", { name: "Barcode(s)", exact: true }),
      ).toBeVisible({ timeout: UI_TIMEOUT });
      const frame = page.locator('iframe[src*="LabelMakerServlet"]');
      await expect(frame).toHaveCount(1);
      const src = new URL(
        (await frame.getAttribute("src")) ?? "",
        "https://placeholder.invalid/",
      );
      expect(src.searchParams.get("prePrinting")).toBe("true");
      expect(src.searchParams.get("facilityName")).toBe(SITE_ID);
      expect(src.searchParams.get("testIds")?.split(",")).toContain(
        panelTestId,
      );
    });
  });

  test("an existing accession shows its patient and the reprint controls for its labels", async ({
    page,
  }) => {
    const labNo = await test.step("seed an order", async () => {
      const accession = await createSampleOrder(page, {
        labNo: "",
        receivedDate: "",
        receivedTime: "",
      });
      expect(accession, "createSampleOrder returned no accession").not.toBe("");
      return accession;
    });

    await test.step("search the accession", async () => {
      await page.goto("/PrintBarcode", { waitUntil: "domcontentloaded" });
      await expect(
        page.getByRole("heading", {
          name: "Print Barcodes for Existing Orders",
        }),
      ).toBeVisible({ timeout: NAV_TIMEOUT });
      await expect(page.getByTestId("order-labels")).toHaveCount(0);
      await page
        .getByRole("textbox", { name: "Enter Accession Number" })
        .fill(labNo);
      await page.getByRole("button", { name: "Submit", exact: true }).click();
    });

    // OGC-1169: one table of labels by display name, a stepper and one Print on
    // every row, one Print all, and no second reprint block.
    await test.step("the order's patient and one label table appear", async () => {
      await expect(page.getByText("Esig Testpatient")).toBeVisible({
        timeout: LONG_TIMEOUT,
      });
      const labels = page.getByTestId("order-labels");
      await expect(labels).toBeVisible();
      await expect(labels.getByText("Labels for this order")).toBeVisible();
      await expect(
        page.getByRole("heading", { name: "Reprint labels", exact: true }),
      ).toHaveCount(0);
      await expect(
        page.getByRole("heading", { name: "Print Sets", exact: true }),
      ).toHaveCount(0);
      await expect(page.getByText(/^order$/)).toHaveCount(0);
      await expect(page.getByText("specimen-1")).toHaveCount(0);

      const orderRow = labels.getByTestId("label-row-order");
      await expect(orderRow).toContainText("Order label");
      await expect(orderRow).toContainText(labNo);
      await expect(orderRow.getByRole("spinbutton")).toBeVisible();

      const specimenRows = labels.locator(
        '[data-testid^="label-row-specimen-"]',
      );
      await expect(specimenRows).toHaveCount(1);
      await expect(specimenRows).toContainText("Specimen label");
      await expect(specimenRows).toContainText(labNo);
      await expect(specimenRows).toContainText(SAMPLE_TYPE);
      await expect(specimenRows.getByRole("spinbutton")).toBeVisible();
      await expect(labels.getByTestId("print-all-labels")).toBeVisible();
      await expect(
        labels.getByText(
          /Prints \d+ order labels? and \d+ labels? per specimen\./,
        ),
      ).toBeVisible();
    });

    await test.step("printing a changed specimen quantity renders the frame with that quantity", async () => {
      const labels = page.getByTestId("order-labels");
      const specimenRow = labels
        .locator('[data-testid^="label-row-specimen-"]')
        .first();
      const stepper = specimenRow.getByRole("spinbutton");
      // The stepper stops at the laboratory's configured maximum for specimen
      // labels; a typed value above it is clamped to that maximum.
      const max = Number(await stepper.getAttribute("max"));
      expect(max).toBeGreaterThanOrEqual(1);
      await stepper.fill(String(max + 1));
      await specimenRow.getByRole("button", { name: /^Print/ }).click();
      const frame = page.locator('iframe[src*="LabelMakerServlet"]');
      await expect(frame).toHaveAttribute(
        "src",
        new RegExp(`labNo=${labNo}-1&type=specimen&quantity=${max}$`),
      );
      await labels
        .getByTestId("label-row-order")
        .getByRole("button", { name: /^Print/ })
        .click();
      await expect(frame).toHaveAttribute(
        "src",
        new RegExp(`labNo=${labNo}&type=order&quantity=\\d+`),
      );
    });
  });

  // OGC-1169: an order saved with label requests (order entry v4) lists its
  // presets by display name and size and reprints at a chosen quantity through
  // the snapshot endpoint, capped at the preset maximum.
  test("an order saved with label requests lists its presets by name and size and reprints a chosen quantity", async ({
    page,
    context,
  }) => {
    test.setTimeout(180_000);
    const labNo = await createSampleOrder(page, {});
    expect(labNo, "seeded order").not.toBe("");

    await test.step("save the order's labels from Prepare Samples", async () => {
      await page.goto(
        `/order/clinical/collect?labNumber=${encodeURIComponent(labNo)}`,
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
      expect((await pdf).status()).toBe(200);
      await (await popup).close();
    });

    await test.step("Print Bar Code Labels lists the saved presets", async () => {
      await page.goto("/PrintBarcode", { waitUntil: "domcontentloaded" });
      await page
        .getByRole("textbox", { name: "Enter Accession Number" })
        .fill(labNo);
      await page.getByRole("button", { name: "Submit", exact: true }).click();
      const labels = page.getByTestId("order-labels");
      await expect(labels).toBeVisible({ timeout: LONG_TIMEOUT });
      const rows = labels.locator('[data-testid^="label-row-saved-"]');
      await expect(rows).toHaveCount(2);
      await expect(rows.first()).toContainText(/Order Label \(\d+ × \d+ mm\)/);
      await expect(rows.nth(1)).toContainText(
        /Specimen Label \(\d+ × \d+ mm\)/,
      );
      await expect(rows.nth(1)).toContainText(`${labNo}-1`);
      await expect(
        labels.getByText(
          "Prints every label saved with this order at its saved quantity.",
        ),
      ).toBeVisible();
    });

    await test.step("a chosen quantity reprints that many copies through the snapshot endpoint", async () => {
      const labels = page.getByTestId("order-labels");
      const tubeRow = labels
        .locator('[data-testid^="label-row-saved-"]')
        .nth(1);
      const stepper = tubeRow.getByRole("spinbutton");
      const max = Number(await stepper.getAttribute("max"));
      expect(max).toBeGreaterThanOrEqual(2);
      await stepper.fill("2");
      const rendered = page.waitForResponse(
        (r) =>
          /\/api\/orders\/\d+\/labels\/pdf\?/.test(r.url()) &&
          r.url().includes("scope=sample") &&
          r.url().includes("quantity=2"),
      );
      await tubeRow.getByRole("button", { name: /^Print/ }).click();
      const response = await rendered;
      expect(response.status()).toBe(200);
      expect(response.headers()["content-type"]).toContain("application/pdf");

      const tooMany = await page.request.get(
        new URL(response.url()).pathname.replace(
          /^\/api\/OpenELIS-Global/,
          "/api/OpenELIS-Global",
        ) +
          "?" +
          new URL(response.url()).searchParams
            .toString()
            .replace("quantity=2", `quantity=${max + 1}`),
      );
      expect(tooMany.status()).toBe(422);
      expect(await tooMany.text()).toContain("error.labels.quantity.max");
    });
  });

  test("an unknown accession shows no patient and no print controls", async ({
    page,
  }) => {
    await page.goto("/PrintBarcode", { waitUntil: "domcontentloaded" });
    await expect(
      page.getByRole("heading", { name: "Print Barcodes for Existing Orders" }),
    ).toBeVisible({ timeout: NAV_TIMEOUT });
    await page
      .getByRole("textbox", { name: "Enter Accession Number" })
      .fill(`NOPE${Date.now()}`);
    await page.getByRole("button", { name: "Submit", exact: true }).click();

    await expect(
      page.getByText("No patients found matching search terms"),
    ).toBeVisible({ timeout: UI_TIMEOUT });
    await expect(
      page.getByRole("heading", { name: "Print Sets", exact: true }),
    ).toHaveCount(0);
  });
});
