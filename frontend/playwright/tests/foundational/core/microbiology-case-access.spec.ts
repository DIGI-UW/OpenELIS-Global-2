import { expect, test } from "../../../helpers/test-base";
import { seedMicrobiologyWorklistCase } from "../../../helpers/seed-microbiology-data";
import { LONG_TIMEOUT } from "../../../helpers/timeouts";

test.describe("Microbiology V2 MVP case visibility", () => {
  test("finds an owned case and opens the read-only sample summary", async ({
    page,
  }, testInfo) => {
    const seeded = await seedMicrobiologyWorklistCase(page);
    const shellResponse = await page.request.get(
      `/api/OpenELIS-Global/rest/microbiology/cases/${seeded.caseId}/shell`,
    );
    expect(shellResponse.ok()).toBeTruthy();
    const shell = await shellResponse.json();
    expect(shell.labUnitId).toBeTruthy();
    expect(shell.labUnit).toBeTruthy();
    const query = new URLSearchParams({
      q: seeded.accessionNumber,
      labUnitId: shell.labUnitId,
    });
    await page.goto(`/Microbiology/worklist?${query}`, {
      waitUntil: "domcontentloaded",
    });
    const link = page.getByRole("link", {
      name: seeded.accessionNumber,
      exact: true,
    });
    await expect(link).toBeVisible({ timeout: LONG_TIMEOUT });
    const row = page.getByRole("row").filter({ has: link });
    await expect(row).toContainText(shell.labUnit);
    await page.screenshot({
      path: testInfo.outputPath("case-worklist-desktop.png"),
      fullPage: true,
    });
    await link.click();
    await expect(
      page.getByRole("heading", {
        name: `Case ${seeded.accessionNumber}`,
        exact: true,
      }),
    ).toBeVisible({ timeout: LONG_TIMEOUT });
    await expect(
      page.getByText("Case details are read-only in this view."),
    ).toBeVisible();
    await expect(
      page.getByRole("heading", { name: "Samples", exact: true }),
    ).toBeVisible();
    await expect(
      page.getByRole("row").filter({ hasText: "Collected" }),
    ).toContainText(seeded.accessionNumber);
    await page.screenshot({
      path: testInfo.outputPath("case-shell-desktop.png"),
      fullPage: true,
    });
    await page.setViewportSize({ width: 390, height: 844 });
    await expect(
      page.getByRole("heading", {
        name: `Case ${seeded.accessionNumber}`,
        exact: true,
      }),
    ).toBeVisible();
    expect(
      await page.evaluate(
        () => document.documentElement.scrollWidth <= window.innerWidth,
      ),
    ).toBeTruthy();
    await page.screenshot({
      path: testInfo.outputPath("case-shell-mobile.png"),
      fullPage: true,
    });
    await page
      .getByRole("link", { name: "Microbiology worklist", exact: true })
      .filter({ hasNot: page.locator(".cds--side-nav__link-text") })
      .click();
    await expect(page).toHaveURL(`/Microbiology/worklist?${query}`);
    await expect(
      page.getByRole("link", { name: seeded.accessionNumber, exact: true }),
    ).toBeVisible();
  });
});
