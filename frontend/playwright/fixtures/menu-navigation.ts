import { expect, Locator, Page } from "@playwright/test";

export class MenuNavigation {
  readonly page: Page;
  readonly menuButton: Locator;
  readonly sideNav: Locator;

  constructor(page: Page) {
    this.page = page;
    this.menuButton = page.locator("#sidenav-menu-button");
    this.sideNav = page.locator(".cds--side-nav");
  }

  async ensureExpanded() {
    await expect(this.menuButton).toBeVisible();
    const expanded = await this.sideNav.evaluate((node) =>
      node.classList.contains("cds--side-nav--expanded"),
    );
    if (!expanded) {
      await this.menuButton.click();
      await expect(this.sideNav).toHaveClass(/cds--side-nav--expanded/);
    }
  }

  async openMenu(parentMenuId: string): Promise<boolean> {
    await this.ensureExpanded();
    const parentToggle = this.page
      .locator(`#${parentMenuId} button.cds--side-nav__submenu`)
      .first();
    if ((await parentToggle.count()) === 0) {
      return false;
    }

    if ((await parentToggle.getAttribute("aria-expanded")) !== "true") {
      await parentToggle.click();
    }

    return true;
  }

  async clickMenuLeaf(
    parentMenuId: string,
    leafMenuId: string,
  ): Promise<boolean> {
    const parentVisible = await this.openMenu(parentMenuId);
    if (!parentVisible) {
      return false;
    }

    const leafLink = this.page.locator(`#${leafMenuId}_nav`).first();
    if ((await leafLink.count()) === 0) {
      return false;
    }

    await leafLink.click();
    return true;
  }
}
