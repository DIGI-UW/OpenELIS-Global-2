import { test, expect } from "../../../helpers/test-base";
import {
  cleanupMicrobiologyCase,
  seedMicrobiologyCase,
} from "../../../helpers/seed-microbiology-data";
import { LONG_TIMEOUT } from "../../../helpers/timeouts";

test.describe("Microbiology case workbench", () => {
  test("records setup activity and creates an isolate", async ({ page }) => {
    const seeded = seedMicrobiologyCase();
    try {
      await page.goto(`/MicrobiologyCaseView/${seeded.caseId}`, {
        waitUntil: "domcontentloaded",
      });

      await expect(
        page.getByRole("heading", { name: "Microbiology case" }),
      ).toBeVisible({ timeout: LONG_TIMEOUT });
      const caseHeader = page.locator("header");
      await expect(caseHeader.getByText("RECEIVED")).toBeVisible();

      await page.getByLabel("Activity note").fill("setup complete");
      await page.getByRole("button", { name: "Record activity" }).click();
      await expect(caseHeader.getByText("SETUP_RECORDED")).toBeVisible({
        timeout: LONG_TIMEOUT,
      });
      await expect(page.getByText(/setup complete/)).toBeVisible();

      await page.getByLabel("Preliminary organism").fill("Escherichia coli");
      await page.getByRole("button", { name: "Create isolate" }).click();
      await expect(page.getByText(/ISO-1: Escherichia coli/)).toBeVisible({
        timeout: LONG_TIMEOUT,
      });
      await expect(page.getByText(/ISOLATE_CREATED/)).toBeVisible();
    } finally {
      cleanupMicrobiologyCase(seeded);
    }
  });
});
