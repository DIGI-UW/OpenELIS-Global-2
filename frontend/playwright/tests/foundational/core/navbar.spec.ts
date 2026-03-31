import { test, expect } from "@playwright/test";

test.describe("Navbar (Header) actions", () => {
  test("logo click navigates to home", async ({ page }) => {
    await page.goto("/Storage/samples");
    await expect(page.locator("#sidenav-menu-button")).toBeVisible();

    // Carbon HeaderName renders an anchor; clicking should navigate home
    await page.locator("#mainHeader a.cds--header__name").click();
    await expect(page).toHaveURL(/\/$/);
  });

  test("search icon toggles search bar", async ({ page }) => {
    await page.goto("/Dashboard");
    await expect(page.locator("#search-Icon")).toBeVisible();

    await page.locator("#search-Icon").click();
    await expect(page.locator("#searchItem")).toBeVisible();

    // Clicking again should close
    await page.locator("#search-Icon").click();
    await expect(page.locator("#searchItem")).toBeHidden();
  });

  test("notifications icon opens notifications panel", async ({ page }) => {
    await page.goto("/Dashboard");
    await expect(page.locator("#notification-Icon")).toBeVisible();

    await page.locator("#notification-Icon").click();
    // Wait for slide-over panel to open and show the title
    // Use nth(1) to get the notifications panel (second one opened)
    await expect(page.locator(".slide-over-title").nth(1)).toContainText(
      "Notifications",
    );

    // Close panel to avoid affecting subsequent tests
    await page.locator(".close-button").nth(1).click();
    await expect(page.locator(".slide-over-root.show")).not.toBeVisible();
  });

  test("user icon opens user panel (logout + language selector visible)", async ({
    page,
  }) => {
    await page.goto("/Dashboard");
    await expect(page.locator("#user-Icon")).toBeVisible();

    await page.locator("#user-Icon").click();

    // Verify slide-over panel structure matches notifications
    await expect(page.locator(".slide-over-root.show")).toBeVisible();
    await expect(page.locator(".slide-over-backdrop").first()).toBeVisible();

    // Verify title is "User" (consistent with "Notifications" pattern)
    await expect(page.locator(".slide-over-title").first()).toHaveText("User");

    // Verify close button with arrow icon exists
    await expect(page.locator(".close-button").first()).toBeVisible();

    // Basic smoke: panel contents present
    await expect(page.getByText(/logout/i)).toBeVisible();
    await expect(page.locator("#selector")).toBeVisible();

    // Verify text labels are visible (not white text on white background)
    await expect(page.getByText(/select locale/i)).toBeVisible();
    // Use context of user panel to avoid matching other "version" elements
    await expect(
      page.locator(".slide-over-root.show").getByText(/version::/i),
    ).toBeVisible();

    // Close panel using the close button
    await page.locator(".close-button").first().click();
    await expect(page.locator(".slide-over-root.show")).not.toBeVisible();
  });

  test("help icon toggles help panel", async ({ page }) => {
    await page.goto("/Dashboard");
    await expect(page.locator("#user-Help")).toBeVisible();

    await page.locator("#user-Help").click();
    await expect(page.getByLabel("Help Panel")).toBeVisible();

    // Basic smoke: expected items exist (names depend on translations)
    await expect(
      page.getByRole("button", { name: /user manual/i }),
    ).toBeVisible();

    // Close it
    await page.locator("#user-Help").click();
    await expect(page.getByLabel("Help Panel")).toBeHidden();
  });
});
