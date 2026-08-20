import type { Page } from "@playwright/test";

export async function csrfHeaders(page: Page) {
  const state = await page.context().storageState();
  const csrf = state.origins
    .flatMap((origin) => origin.localStorage)
    .find((item) => item.name === "CSRF")?.value;
  if (!csrf)
    throw new Error("Authenticated Playwright state has no CSRF token");
  return { "X-CSRF-Token": csrf };
}
