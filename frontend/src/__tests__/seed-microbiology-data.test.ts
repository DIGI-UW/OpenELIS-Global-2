import type { Page } from "@playwright/test";
import { getCsrfToken } from "../../playwright/helpers/seed-microbiology-data";

describe("microbiology Playwright scenario authentication", () => {
  it("fails clearly when the authenticated storage state has no CSRF token", async () => {
    const page = {
      context: () => ({
        storageState: async () => ({ cookies: [], origins: [] }),
      }),
    } as unknown as Page;

    await expect(getCsrfToken(page)).rejects.toThrow(
      "Authenticated Playwright storage state is missing the CSRF token",
    );
  });
});
