/**
 * Helpers for talking to the API as the logged-in user.
 *
 * `page.request` already carries the stored session cookie, so seeds and
 * fixtures issue their calls through it. What it does NOT carry is the CSRF
 * token, which the app keeps in localStorage and every write endpoint demands
 * as `X-CSRF-Token`. `csrfToken` lifts it out of the saved auth state, so no
 * navigation is needed to obtain it.
 */
import { Browser, Page } from "@playwright/test";

const AUTH_STATE = "playwright/.auth/user.json";

/** The CSRF token `auth.setup` stored under localStorage key "CSRF". */
export async function csrfToken(page: Page): Promise<string> {
  const state = await page.context().storageState();
  for (const origin of state.origins) {
    for (const item of origin.localStorage) {
      if (item.name === "CSRF") return item.value;
    }
  }
  return "";
}

/**
 * Run `work` on a throwaway page carrying the stored session, then close the
 * context. For hooks that need a page of their own (`beforeAll`/`afterAll`
 * get a browser, not a page).
 */
export async function withAuthedPage<T>(
  browser: Browser,
  work: (page: Page) => Promise<T>,
): Promise<T> {
  const context = await browser.newContext({ storageState: AUTH_STATE });
  try {
    return await work(await context.newPage());
  } finally {
    await context.close();
  }
}
