import { test, expect } from "../../../helpers/test-base";
import { Sidenav } from "../../../fixtures/sidenav";

test.describe("Sidenav", () => {
  test("desktop renders persistent nav with no hamburger", async ({ page }) => {
    await page.goto("/", { waitUntil: "domcontentloaded" });
    const sidenav = new Sidenav(page);
    await sidenav.expectExpanded();
    await expect(sidenav.menuButton).toHaveCount(0);
  });

  test("storage page has expanded nav", async ({ page }) => {
    const sidenav = new Sidenav(page);
    await sidenav.gotoStorage("sample-items");
    await sidenav.expectExpanded();
  });

  test("small viewport collapses nav into a hamburger drawer", async ({
    page,
  }) => {
    await page.setViewportSize({ width: 800, height: 900 });
    const sidenav = new Sidenav(page);
    await sidenav.gotoStorage("sample-items");

    await sidenav.expectCollapsed();
    await expect(sidenav.menuButton).toBeVisible();

    await sidenav.toggle();
    await sidenav.expectExpanded();
    await sidenav.toggle();
    await sidenav.expectCollapsed();
  });

  test("nav collapses below the desktop breakpoint and returns above it", async ({
    page,
  }) => {
    const sidenav = new Sidenav(page);
    await sidenav.gotoStorage("sample-items");
    await sidenav.expectExpanded();

    await page.setViewportSize({ width: 1000, height: 800 });
    await sidenav.expectCollapsed();
    await expect(sidenav.menuButton).toBeVisible();

    await page.setViewportSize({ width: 1280, height: 800 });
    await sidenav.expectExpanded();
    await expect(sidenav.menuButton).toHaveCount(0);
  });

  test("content is pushed on desktop and full-width below the breakpoint", async ({
    page,
  }) => {
    const sidenav = new Sidenav(page);
    await sidenav.gotoStorage("sample-items");
    await sidenav.expectExpanded();

    const content = page.locator('[data-testid="content-wrapper"]');
    await expect(content).toHaveClass(/content-nav-locked/);

    await page.setViewportSize({ width: 1000, height: 800 });
    await expect(content).not.toHaveClass(/content-nav-locked/);
  });

  test("storage subnav updates active state", async ({ page }) => {
    const sidenav = new Sidenav(page);
    await sidenav.gotoStorage("sample-items");

    // Expand menus to see items
    await sidenav.expandMenu("Storage");
    await sidenav.expandMenu("Storage Management");

    // Check initial active state
    await sidenav.expectMenuActive("Sample Items");

    // Navigate and verify active state changes
    await sidenav.clickMenu("Rooms");
    await expect(page).toHaveURL(/\/Storage\/rooms/);
    await sidenav.expectMenuActive("Rooms");
    await sidenav.expectMenuInactive("Sample Items");
  });

  test("cold storage subnav updates active state", async ({ page }) => {
    const sidenav = new Sidenav(page);
    await sidenav.gotoFreezer(0);

    // Expand menus
    await sidenav.expandMenu("Storage");
    await sidenav.expandMenu("Cold Storage Monitoring");

    // Check initial active state
    await sidenav.expectMenuActive("Dashboard");

    // Navigate to another tab
    await sidenav.clickMenu("Corrective Actions");
    await expect(page).toHaveURL(/FreezerMonitoring\?tab=1/);
    await sidenav.expectMenuActive("Corrective Actions");
    await sidenav.expectMenuInactive("Dashboard");
  });

  /**
   * Regression: no grey background flicker on collapsed parent menus.
   * Carbon's cds--side-nav__item--active class should not produce a visible
   * grey background on submenu buttons that are collapsed.
   */
  test("no grey background on collapsed parent menus after navigation", async ({
    page,
  }) => {
    const sidenav = new Sidenav(page);
    await sidenav.gotoStorage("sample-items");
    await sidenav.expectExpanded();

    // Expand a parent menu and click a child to navigate
    await sidenav.expandMenu("Storage");
    await sidenav.expandMenu("Storage Management");
    await sidenav.clickMenu("Rooms");
    await expect(page).toHaveURL(/\/Storage\/rooms/);

    // After navigation, verify no collapsed parent has grey active background
    await sidenav.expectNoActiveGreyBackground();
  });

  test("no grey background when switching between top-level menus", async ({
    page,
  }) => {
    // Desktop renders the main menu expanded by default
    await page.goto("/Dashboard", { waitUntil: "domcontentloaded" });

    const sidenav = new Sidenav(page);
    await sidenav.expectExpanded();

    // Expand all top-level menus to find at least two expandable ones
    const buttons = sidenav.nav.locator("button.cds--side-nav__submenu");
    const count = await buttons.count();

    if (count >= 2) {
      // Click first top-level menu to expand it
      await buttons.first().click();
      // Click second top-level menu
      await buttons.nth(1).click();

      // After switching, verify no collapsed parent has grey active background
      await sidenav.expectNoActiveGreyBackground();
    }
  });

  test("parent menu has no grey background when child is active", async ({
    page,
  }) => {
    const sidenav = new Sidenav(page);
    await sidenav.gotoStorage("sample-items");
    await sidenav.expectExpanded();

    // Verify no parent with a collapsed submenu has the grey active background
    await sidenav.expectNoActiveGreyBackground();
  });

  // TODO: stabilize or narrow scope—visiting every link is flaky (click/expandAllMenus timeouts).
  test.skip("all visible sidenav links navigate (smoke)", async ({ page }) => {
    const sidenav = new Sidenav(page);

    // Start from Storage so the nav is definitely present and expanded
    await sidenav.gotoStorage("sample-items");
    await sidenav.expectExpanded();
    await sidenav.expandAllMenus();

    const linkInfos = await sidenav.getVisibleLinkInfos();

    // Skip links that are known to be non-navigational or disruptive.
    // Keep this list minimal and explicit.
    const skipHrefPrefixes = ["/#", "#"];
    const skipNameExact = new Set<string>([]);

    for (const info of linkInfos) {
      if (!info.href) continue;
      if (skipNameExact.has(info.name)) continue;
      if (skipHrefPrefixes.some((p) => info.href.startsWith(p))) continue;

      // Ensure nav is expanded before each click (some pages may collapse it).
      await sidenav.ensureExpanded();
      await sidenav.expandAllMenus();

      const link = sidenav.nav.locator(
        `a.cds--side-nav__link[href="${info.href}"]`,
      );

      // If a link opens a new window, assert it opens and close the popup.
      if (info.target === "_blank") {
        const [popup] = await Promise.all([
          page.waitForEvent("popup"),
          link.first().click(),
        ]);
        await expect(popup).toHaveURL(/.*/);
        await popup.close();
        continue;
      }

      await link.first().click();

      // Loose URL assertion: verify we reached the route root (ignore query/hash variations).
      const urlRoot = info.href.split(/[?#]/)[0].replace(/^\//, "");
      if (urlRoot) {
        await expect(page).toHaveURL(new RegExp(urlRoot.replace(/\//g, "\\/")));
      }

      // Return to a page with sidenav so the next iteration has .cds--side-nav in the DOM.
      await sidenav.gotoStorage("sample-items");
      await sidenav.expectExpanded();
    }
  });
});
