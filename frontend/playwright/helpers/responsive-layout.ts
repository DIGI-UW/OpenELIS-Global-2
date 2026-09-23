import { expect, type Page } from "@playwright/test";
import { LONG_TIMEOUT } from "./timeouts";

export async function expectNoPageHorizontalOverflow(
  page: Page,
  message: string,
) {
  await page.waitForFunction(
    () =>
      document.documentElement.scrollWidth <=
      document.documentElement.clientWidth,
    undefined,
    { timeout: LONG_TIMEOUT },
  );
  const overflow = await page.evaluate(
    () =>
      document.documentElement.scrollWidth -
      document.documentElement.clientWidth,
  );
  expect(overflow, message).toBeLessThanOrEqual(0);
}
