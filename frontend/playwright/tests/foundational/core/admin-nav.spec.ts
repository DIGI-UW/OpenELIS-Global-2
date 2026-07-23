import { test, expect } from "../../../helpers/test-base";

test.describe("Admin navigation context", () => {
  test("switches to Admin shell nav and returns to main nav", async ({
    page,
  }) => {
    await page.goto("/Dashboard", { waitUntil: "domcontentloaded" });
    await page.evaluate(() => {
      localStorage.setItem("mainSideNavMode", "lock");
    });

    await page.goto("/MasterListsPage", { waitUntil: "domcontentloaded" });

    const sideNav = page.locator(".cds--side-nav");
    await expect(sideNav).toHaveClass(/cds--side-nav--expanded/);
    await expect(sideNav).toContainText("Back to main menu");
    await expect(sideNav).toContainText("Reflex Tests Configuration");
    await expect(sideNav).toContainText("User Management");
    await expect(
      page.getByRole("heading", { name: "Admin dashboard" }),
    ).toBeVisible();
    await expect(page.getByText("Choose an administration area")).toBeVisible();
    await expect(page.getByRole("link", { name: "Add Order" })).toHaveCount(0);

    await page.getByTestId("admin-back-to-main-nav").click();

    await expect(page).toHaveURL(/\/Dashboard$/);
    await expect(sideNav).not.toContainText("Back to main menu");
    await expect(sideNav.getByRole("link", { name: "Home" })).toBeVisible();
    await expect(sideNav.getByRole("link", { name: "Admin" })).toBeVisible();
  });
});
