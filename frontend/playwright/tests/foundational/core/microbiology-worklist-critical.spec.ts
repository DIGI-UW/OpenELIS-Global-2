import { test, expect } from "../../../helpers/test-base";
import {
  cleanupMicrobiologyMvpCase,
  seedMicrobiologyWorklistCase,
} from "../../../helpers/seed-microbiology-data";
import { LONG_TIMEOUT } from "../../../helpers/timeouts";

test.describe("microbiology worklist and critical communication", () => {
  test("critical communication raises worklist priority and sibling visibility", async ({
    page,
  }) => {
    test.setTimeout(120_000);
    const seeded = seedMicrobiologyWorklistCase();

    try {
      await page.goto(`/MicrobiologyCaseView/${seeded.caseId}`, {
        waitUntil: "domcontentloaded",
      });
      await expect(
        page.getByRole("heading", { name: "Microbiology case" }),
      ).toBeVisible({ timeout: LONG_TIMEOUT });

      await page.getByLabel("Recipient").fill("Provider on call");
      await page
        .getByLabel("Message")
        .fill("Positive blood culture called to provider");
      await page.getByRole("button", { name: "Log communication" }).click();
      await expect(
        page.getByTestId("microbiology-critical-status"),
      ).toContainText("OPEN", { timeout: LONG_TIMEOUT });

      await page.goto("/MicrobiologyWorklist", {
        waitUntil: "domcontentloaded",
      });
      const row = page.getByTestId(
        `microbiology-worklist-row-${seeded.caseId}`,
      );
      await expect(row).toBeVisible({ timeout: LONG_TIMEOUT });
      await expect(row).toContainText("HIGH");
      await expect(row).toContainText("Critical communication");
      await expect(row).toContainText("MYCOBACTERIOLOGY_TB");

      await row.getByRole("button", { name: "Open case" }).click();
      await expect(
        page.getByRole("heading", { name: "Microbiology case" }),
      ).toBeVisible({ timeout: LONG_TIMEOUT });
      await page.getByRole("button", { name: "Acknowledge" }).click();
      await expect(
        page.getByTestId("microbiology-critical-status"),
      ).toContainText("ACKNOWLEDGED", { timeout: LONG_TIMEOUT });
    } finally {
      cleanupMicrobiologyMvpCase(seeded);
    }
  });
});
