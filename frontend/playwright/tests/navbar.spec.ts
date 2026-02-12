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
    // Using .slide-over-title to avoid matching the aria-label on the icon
    await expect(page.locator(".slide-over-title")).toContainText(
      "Notifications",
    );
  });

  test("user icon opens user panel (logout + language selector visible)", async ({
    page,
  }) => {
    await page.goto("/Dashboard");
    await expect(page.locator("#user-Icon")).toBeVisible();

    await page.locator("#user-Icon").click();

    // Basic smoke: panel contents present
    await expect(page.getByText(/logout/i)).toBeVisible();
    await expect(page.locator("#selector")).toBeVisible();
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
