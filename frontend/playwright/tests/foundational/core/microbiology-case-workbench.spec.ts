import { test, expect } from "../../../helpers/test-base";
import { seedMicrobiologyCase } from "../../../helpers/seed-microbiology-data";
import { LONG_TIMEOUT } from "../../../helpers/timeouts";

test.describe("Microbiology case workbench", () => {
  test("records setup activity and creates an isolate", async ({ page }) => {
    const seeded = await seedMicrobiologyCase(page);
    await page.goto(
      `/MicrobiologyCaseView/${seeded.caseId}?workflow=BACTERIOLOGY&sort=newest`,
      {
        waitUntil: "commit",
      },
    );
    await expect(page).toHaveURL(
      new RegExp(
        `/Microbiology/cases/${seeded.caseId}\\?workflow=BACTERIOLOGY&sort=newest$`,
      ),
    );

    await expect(
      page.getByRole("heading", { name: "Microbiology case" }),
    ).toBeVisible({ timeout: LONG_TIMEOUT });
    const caseHeader = page.locator("header");
    const caseView = page.getByTestId("microbiology-case-view");
    await expect(caseHeader.getByTitle("Received")).toBeVisible();

    await caseView
      .getByRole("button", { name: "Inoculation", exact: true })
      .click();
    await expect(page).toHaveURL(/section=setup/);
    await page.getByLabel("Activity note").fill("setup complete");
    await page.getByRole("button", { name: "Start inoculation" }).click();
    await expect(caseHeader.getByTitle("Setup Recorded")).toBeVisible({
      timeout: LONG_TIMEOUT,
    });

    await caseView
      .getByRole("button", { name: "Timeline", exact: true })
      .click();
    await expect(page).toHaveURL(/section=timeline/);
    await expect(page.getByText(/setup complete/)).toBeVisible();

    await caseView
      .getByRole("button", { name: "Isolates", exact: true })
      .click();
    await expect(page).toHaveURL(/section=isolates/);
    await page.getByLabel("Preliminary organism").fill("Escherichia coli");
    await page.getByRole("button", { name: "Create isolate" }).click();
    await expect(page.getByText(/ISO-1: Escherichia coli/)).toBeVisible({
      timeout: LONG_TIMEOUT,
    });

    await caseView
      .getByRole("button", { name: "Timeline", exact: true })
      .click();
    await expect(page).toHaveURL(/section=timeline/);
    await expect(page.getByText(/Isolate Created/)).toBeVisible();
  });
});
