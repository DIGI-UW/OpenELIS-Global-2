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

    // Scope queries within the open notifications panel
    const notificationsPanel = page.locator(".slide-over-root.show");
    await expect(notificationsPanel).toBeVisible();

    // Get title within the specific open panel
    await expect(notificationsPanel.locator(".slide-over-title")).toContainText(
      "Notifications",
    );

    // Close panel using close button within the same panel
    await notificationsPanel.locator(".close-button").click();
    await expect(notificationsPanel).not.toBeVisible();
  });

  test("user icon opens user panel (logout + language selector visible)", async ({
    page,
  }) => {
    await page.goto("/Dashboard");
    await expect(page.locator("#user-Icon")).toBeVisible();

    await page.locator("#user-Icon").click();

    // Scope queries within the open user panel
    const userPanel = page.locator(".slide-over-root.show");
    await expect(userPanel).toBeVisible();

    // Verify backdrop within the specific open panel
    await expect(userPanel.locator(".slide-over-backdrop")).toBeVisible();

    // Verify title is "User" within the specific open panel
    await expect(userPanel.locator(".slide-over-title")).toHaveText("User");

    // Verify close button within the specific open panel
    await expect(userPanel.locator(".close-button")).toBeVisible();

    // Basic smoke: panel contents present
    await expect(page.getByText(/logout/i)).toBeVisible();
    await expect(page.locator("#selector")).toBeVisible();

    // Verify text labels are visible (not white text on white background)
    await expect(userPanel.getByText(/select locale/i)).toBeVisible();
    // Use context of user panel to avoid matching other "version" elements
    await expect(userPanel.getByText(/version::/i)).toBeVisible();

    // Close panel using close button within the same panel
    await userPanel.locator(".close-button").click();
    await expect(userPanel).not.toBeVisible();
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
