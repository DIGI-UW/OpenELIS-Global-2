import { test, expect } from "../../../helpers/test-base";
import { NAV_TIMEOUT } from "../../../helpers/timeouts";

/**
 * OGC-1192 — environmental and vector orders outside the order workflow.
 *
 * Modify Order opened a patientless order in the clinical wizard as "No
 * Patient Information Available", and opened a lab number with no order the
 * same way. The Environmental Compliance Dashboard counted result rows as
 * orders, so a freshly entered order left it at zero, and showed a 0% rate
 * when no result had a threshold. A reopened saved order also lost the
 * user's typing to the "saved" notice, which held the keyboard focus.
 */

const MISSING_LAB_NUMBER = "DEV01260000000099999";

test.describe("OGC-1192 environmental orders", () => {
  test("Modify Order sends an environmental order to its own Enter Order page", async ({
    page,
  }) => {
    const labNumber = "DEV01260000000011920";
    await page.route("**/rest/order/search?labNumber=*", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          id: "1192",
          labNumber,
          sampleOrderItems: {
            environmentalFields: { workflowType: "environmental" },
          },
        }),
      }),
    );

    await page.goto(`/ModifyOrder?accessionNumber=${labNumber}`, {
      waitUntil: "domcontentloaded",
    });

    await expect(page).toHaveURL(
      new RegExp(`/order/environmental/enter\\?labNumber=${labNumber}$`),
      { timeout: NAV_TIMEOUT },
    );
    await expect(
      page.getByText("No Patient Information Available"),
    ).toHaveCount(0);
  });

  test("Modify Order says when a lab number has no order", async ({ page }) => {
    await page.goto(`/ModifyOrder?accessionNumber=${MISSING_LAB_NUMBER}`, {
      waitUntil: "domcontentloaded",
    });

    await expect(
      page.getByText("No sample found for the provided accession number."),
    ).toBeVisible({ timeout: NAV_TIMEOUT });
    await expect(
      page.getByText("No Patient Information Available"),
    ).toHaveCount(0);
    await expect(page.getByRole("button", { name: "Next" })).toHaveCount(0);
  });

  test("the compliance dashboard shows its order counts and every label", async ({
    page,
  }) => {
    const missingMessages: string[] = [];
    page.on("console", (message) => {
      if (message.text().includes("MISSING_TRANSLATION")) {
        missingMessages.push(message.text());
      }
    });
    const summary = page.waitForResponse((response) =>
      response.url().includes("/rest/compliance/dashboard/summary"),
    );

    await page.goto("/EnvironmentalDashboard", {
      waitUntil: "domcontentloaded",
    });
    await summary;

    await expect(page.getByText("Total Orders")).toBeVisible({
      timeout: NAV_TIMEOUT,
    });
    await expect(page.getByText("Sites Monitored")).toBeVisible();
    await expect(page.getByText("All sites").first()).toBeVisible();
    expect(
      missingMessages.filter((text) =>
        text.includes("compliance.dashboard.filter.sites.placeholder"),
      ),
    ).toEqual([]);
  });

  test("the compliance rate shows no value when no result was judged", async ({
    page,
  }) => {
    await page.route("**/rest/compliance/dashboard/summary?*", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          totalOrders: 2,
          totalExceedances: 0,
          sitesMonitored: 1,
          trend: { totalOrders: 2, totalExceedances: 0, sitesMonitored: 1 },
        }),
      }),
    );

    await page.goto("/EnvironmentalDashboard", {
      waitUntil: "domcontentloaded",
    });

    const rateTile = page.locator(".cds--tile", { hasText: "Compliance Rate" });
    await expect(rateTile).toContainText("\u2014", { timeout: NAV_TIMEOUT });
    await expect(rateTile).not.toContainText("%");
    await expect(
      page.locator(".cds--tile", { hasText: "Total Orders" }),
    ).toContainText("2");
  });

  test("typing into a reopened saved order stays in the form", async ({
    page,
  }) => {
    const labNumber = "DEV01260000000011921";
    await page.route("**/rest/order/search?labNumber=*", (route) =>
      route.fulfill({
        status: 200,
        contentType: "application/json",
        body: JSON.stringify({
          id: "1193",
          labNumber,
          samples: [],
          sampleOrderItems: {
            labNo: labNumber,
            environmentalFields: { workflowType: "environmental" },
          },
        }),
      }),
    );

    await page.goto(`/order/environmental/enter?labNumber=${labNumber}`, {
      waitUntil: "domcontentloaded",
    });
    await expect(page.locator(".order-saved-next-action")).toBeVisible({
      timeout: NAV_TIMEOUT,
    });

    const notes = page.getByRole("textbox", { name: "Field Notes" });
    await notes.click();
    await page.keyboard.type("Turbid after rain");
    await page.keyboard.press("Enter");

    await expect(notes).toHaveValue(/Turbid after rain/);
    await expect(notes).toBeFocused();
    await expect(page).toHaveURL(/\/order\/environmental\/enter/);
    await expect(page.getByText("Unsaved changes")).toBeVisible();
  });
});
