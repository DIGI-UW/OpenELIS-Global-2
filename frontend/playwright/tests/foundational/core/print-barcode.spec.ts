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
      await expect(
        page.getByRole("heading", { name: "Print Sets", exact: true }),
      ).toHaveCount(0);
      await page
        .getByRole("textbox", { name: "Enter Accession Number" })
        .fill(labNo);
      await page.getByRole("button", { name: "Submit", exact: true }).click();
    });

    await test.step("the order's patient and print controls appear", async () => {
      await expect(page.getByText("Esig Testpatient")).toBeVisible({
        timeout: LONG_TIMEOUT,
      });
      await expect(
        page.getByRole("heading", { name: "Print Sets", exact: true }),
      ).toBeVisible();
      await expect(
        page.getByRole("button", { name: "Print Set", exact: true }),
      ).toBeVisible();
      await expect(
        page.getByRole("heading", { name: "Reprint labels", exact: true }),
      ).toBeVisible();

      const orderRow = page.getByRole("row").filter({
        has: page.getByRole("cell", { name: "Order", exact: true }),
      });
      await expect(orderRow).toHaveCount(1);
      await expect(
        orderRow.getByRole("cell", { name: labNo, exact: true }),
      ).toBeVisible();

      const specimenRows = page.getByRole("row").filter({
        has: page.getByRole("cell", { name: "Specimen", exact: true }),
      });
      await expect(specimenRows).toHaveCount(1);
      await expect(specimenRows).toContainText(labNo);
      await expect(specimenRows).toContainText(SAMPLE_TYPE);
    });

    await test.step("printing the order label renders the frame for that accession", async () => {
      const orderRow = page.getByRole("row").filter({
        has: page.getByRole("cell", { name: "Order", exact: true }),
      });
      await orderRow.getByRole("button", { name: "Print Label" }).click();
      const frame = page.locator('iframe[src*="LabelMakerServlet"]');
      await expect(frame).toHaveAttribute(
        "src",
        new RegExp(`labNo=${labNo}&type=order`),
      );
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
