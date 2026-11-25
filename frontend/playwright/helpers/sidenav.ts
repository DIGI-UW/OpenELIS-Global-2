import { expect, type Page } from "@playwright/test";
import { NAV_TIMEOUT, UI_TIMEOUT } from "./timeouts";

/**
 * Follow a configured side-nav path by menu element ids, e.g.
 * ["menu_reports", "menu_reports_routine", "menu_reports_status_patient"].
 *
 * ConfiguredSideNav gives every group's toggle button the menu's elementId and
 * every leaf an anchor `{elementId}_nav`. Groups are expanded only when their
 * aria-expanded is not already "true", so a group the current route opened
 * stays open. Ids may contain dots, hence attribute selectors.
 */
export async function openSideNavItem(
  page: Page,
  path: string[],
): Promise<void> {
  const nav = page.getByRole("navigation", { name: "Side navigation" });
  await expect(nav).toBeVisible({ timeout: NAV_TIMEOUT });
  // A persistent nav has no menu button; a collapsible one is opened first.
  const menuButton = page.locator("#sidenav-menu-button");
  const sideNav = page.locator(".cds--side-nav");
  if (
    (await menuButton.count()) > 0 &&
    !(await sideNav.evaluate((el) =>
      el.classList.contains("cds--side-nav--expanded"),
    ))
  ) {
    await menuButton.click();
    await expect(sideNav).toHaveClass(/cds--side-nav--expanded/);
  }

  const groups = path.slice(0, -1);
  const leaf = path[path.length - 1];
  for (const id of groups) {
    const toggle = nav.locator(`button[id="${id}"]`);
    await expect(toggle).toBeVisible({ timeout: UI_TIMEOUT });
    if ((await toggle.getAttribute("aria-expanded")) !== "true") {
      await toggle.click();
    }
    await expect(toggle).toHaveAttribute("aria-expanded", "true");
  }
  const link = nav.locator(`a[id="${leaf}_nav"]`);
  await expect(link).toBeVisible({ timeout: UI_TIMEOUT });
  await link.click();
}
