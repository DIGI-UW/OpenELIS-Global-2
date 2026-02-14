import { test as base, expect, type Page } from "@playwright/test";

type E2EBaseFixtures = {
  gotoAndWait: (route: string) => Promise<void>;
  ensureAuthenticatedShell: () => Promise<void>;
};

async function waitForShell(page: Page) {
  await expect(page.locator("#sidenav-menu-button")).toBeVisible();
}

export const test = base.extend<E2EBaseFixtures>({
  gotoAndWait: async ({ page }, use) => {
    await use(async (route: string) => {
      await page.goto(route);
      await waitForShell(page);
    });
  },

  ensureAuthenticatedShell: async ({ page }, use) => {
    await use(async () => {
      await waitForShell(page);
      await expect(page.locator("#mainHeader")).toBeVisible();
    });
  },
});

export { expect };
