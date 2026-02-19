import { Page, expect, Locator } from "@playwright/test";

/**
 * Sidenav Page Object - encapsulates sidenav interactions
 */
export class Sidenav {
  readonly page: Page;
  readonly nav: Locator;
  readonly menuButton: Locator;

  constructor(page: Page) {
    this.page = page;
    this.nav = page.locator(".cds--side-nav");
    // Use id for stability (data-cy is for testing, id is the DOM identifier)
    this.menuButton = page.locator("#sidenav-menu-button");
  }

  /** Check if sidenav is expanded */
  async expectExpanded() {
    await expect(this.nav).toHaveClass(/cds--side-nav--expanded/);
  }

  /** Check if sidenav is collapsed */
  async expectCollapsed() {
    await expect(this.nav).not.toHaveClass(/cds--side-nav--expanded/);
  }

  /** Toggle sidenav open/close */
  async toggle() {
    await this.menuButton.click();
  }

  /** Ensure the SideNav is expanded (click toggle if needed) */
  async ensureExpanded() {
    const hasExpandedClass = await this.nav.evaluate((el) =>
      el.classList.contains("cds--side-nav--expanded"),
    );
    if (!hasExpandedClass) {
      await this.toggle();
      await this.expectExpanded();
    }
  }

  /** Click a menu item by text */
  async clickMenu(text: string) {
    await this.nav.getByRole("link", { name: text }).click();
  }

  /** Expand a parent menu by text (exact match) */
  async expandMenu(text: string) {
    const button = this.nav.getByRole("button", { name: text, exact: true });
    const expanded = await button.getAttribute("aria-expanded");
    if (expanded !== "true") {
      await button.click();
    }
  }

  /**
   * Expand ALL currently-collapsed menus in the nav.
   *
   * Useful for "click every link" tests where we want the full tree visible.
   * Safe to call multiple times.
   */
  async expandAllMenus() {
    await this.ensureExpanded();

    // Expand iteratively since expanding one node can reveal more nodes.
    for (let i = 0; i < 25; i++) {
      const closed = this.nav.locator('button[aria-expanded="false"]');
      const count = await closed.count();
      if (count === 0) return;

      // Click the first closed menu button and continue.
      await closed.first().click();
    }
  }

  /**
   * Return visible sidenav link info. This intentionally only captures "real" links
   * (anchor tags) and skips menu toggle buttons.
   */
  async getVisibleLinkInfos(): Promise<
    Array<{ name: string; href: string; target: string | null }>
  > {
    await this.ensureExpanded();
    // Expand all menus so nested links are visible
    await this.expandAllMenus();

    const links = this.nav.locator("a.cds--side-nav__link");
    const count = await links.count();
    const infos: Array<{ name: string; href: string; target: string | null }> =
      [];

    for (let i = 0; i < count; i++) {
      const link = links.nth(i);
      if (!(await link.isVisible())) continue;
      const href = (await link.getAttribute("href")) || "";
      if (!href) continue;
      const target = await link.getAttribute("target");
      const rawName = (await link.textContent()) || "";
      const name = rawName.replace(/\s+/g, " ").trim();
      infos.push({ name, href, target });
    }

    return infos;
  }

  /** Check if a menu item is active/current */
  async expectMenuActive(text: string) {
    const link = this.nav.getByRole("link", { name: text });
    await expect(link).toHaveClass(/cds--side-nav__link--current/);
  }

  /** Check if a menu item is NOT active */
  async expectMenuInactive(text: string) {
    const link = this.nav.getByRole("link", { name: text });
    await expect(link).not.toHaveClass(/cds--side-nav__link--current/);
  }

  /** Navigate to storage section and wait for load */
  async gotoStorage(path = "samples") {
    await this.page.goto(`/Storage/${path}`);
    await expect(this.page).toHaveURL(new RegExp(`/Storage/${path}`));
  }

  /**
   * Returns true if Storage is an expandable menu (button with children).
   * Use to skip subnav tests when migrations haven't run (e.g. fresh DB with correct order).
   */
  async hasStorageSubnav(): Promise<boolean> {
    const btn = this.nav.getByRole("button", { name: "Storage", exact: true });
    try {
      await btn.waitFor({ state: "visible", timeout: 3000 });
      return true;
    } catch {
      return false;
    }
  }

  /** Navigate to freezer monitoring */
  async gotoFreezer(tab = 0) {
    await this.page.goto(`/FreezerMonitoring?tab=${tab}`);
    await expect(this.page).toHaveURL(/FreezerMonitoring/);
  }

  /**
   * Assert that no collapsed submenu button has Carbon's grey "active" background.
   *
   * Carbon applies `cds--side-nav__item--active` to `<li>` wrappers of SideNavMenu,
   * which triggers a grey `background-color` on the child `<button>` when
   * `aria-expanded=false`. This helper catches that transient flicker state.
   */
  async expectNoActiveGreyBackground() {
    const activeItems = this.nav.locator("li.cds--side-nav__item--active");
    const count = await activeItems.count();

    for (let i = 0; i < count; i++) {
      const button = activeItems
        .nth(i)
        .locator("button.cds--side-nav__submenu");
      if ((await button.count()) === 0) continue;

      const expanded = await button.getAttribute("aria-expanded");
      if (expanded === "false") {
        const bg = await button.evaluate(
          (el) => getComputedStyle(el).backgroundColor,
        );
        // Carbon's grey fallback is rgba(141, 141, 141, 0.2)
        expect(
          bg,
          `Collapsed active submenu button should not have grey background (got ${bg})`,
        ).not.toContain("141");
      }
    }
  }
}
