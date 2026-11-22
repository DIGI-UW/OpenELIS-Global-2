import { test, expect } from "../../../helpers/test-base";

// OGC-1378: a picked date was formatted with toISOString(), which converts the
// picker's local midnight to UTC, so east of UTC it became the day before.
// Papua New Guinea (UTC+10) is where the bug was reported.
test.use({ timezoneId: "Pacific/Port_Moresby" });

test.describe("Picked dates east of UTC (OGC-1378)", () => {
  test("a holiday picked in UTC+10 is saved and listed on the day picked", async ({
    page,
  }) => {
    const name = `OGC-1378 holiday ${Date.now()}`;
    await page.goto("/MasterListsPage/calendarManagement", {
      waitUntil: "domcontentloaded",
    });

    await page.getByTestId("add-holiday-button").click();
    const date = page.locator("#new-holiday-date");
    await date.fill("2026-12-24");
    await date.press("Enter");
    await expect(date).toHaveValue("2026-12-24");
    await page.locator("#new-holiday-name").fill(name);
    const saved = page.waitForResponse(
      (response) =>
        response.url().includes("/rest/calendar/holidays") &&
        response.request().method() === "POST",
    );
    await page.getByTestId("save-holiday-button").click();
    await saved;

    const row = page.locator("tr").filter({ hasText: name });
    await expect(row).toContainText("2026-12-24");

    await row.getByRole("button", { name: /Delete/ }).click();
    const confirm = page.getByRole("dialog", {
      name: "Are you sure you want to delete this holiday?",
    });
    await confirm.getByRole("button", { name: /Delete/ }).click();
    await expect(page.locator("tr").filter({ hasText: name })).toHaveCount(0);
  });
});
