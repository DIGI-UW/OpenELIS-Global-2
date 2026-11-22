import { test, expect } from "../../../helpers/test-base";

// The change-password flow is for users who cannot log in (expired password),
// so run unauthenticated instead of with the shared storageState.
test.use({ storageState: { cookies: [], origins: [] } });

test.describe("Change Password page", () => {
  test.beforeEach(async ({ page }) => {
    await page.goto("/ChangePasswordLogin", { waitUntil: "domcontentloaded" });
    await expect(page.locator("#loginName")).toBeVisible();
  });

  test("too-short new password shows visible length error and disables submit", async ({
    page,
  }) => {
    await page.locator("#new-password").fill("ab1!");
    await page.locator("#new-password").blur();

    await expect(page.locator("#new-password-error-msg")).toContainText(
      /at least 7 characters/i,
    );
    await expect(page.locator('[data-cy="submitNewPassword"]')).toBeDisabled();
  });

  test("new password without a special char shows visible complexity error", async ({
    page,
  }) => {
    // 7+ chars but no special char isolates the specialChar rule (min-length passes first)
    await page.locator("#new-password").fill("abcdefg");
    await page.locator("#new-password").blur();

    await expect(page.locator("#new-password-error-msg")).toContainText(
      /special character/i,
    );
    await expect(page.locator('[data-cy="submitNewPassword"]')).toBeDisabled();
  });

  test("lowercase-only password with special char is accepted client-side", async ({
    page,
  }) => {
    // backend complexity (Haiti/CDI) does not require an uppercase letter
    await page.locator("#new-password").fill("abcdef1!");
    await page.locator("#new-password").blur();

    await expect(page.locator("#new-password-error-msg")).toBeHidden();
  });

  test("mismatched repeat password shows visible match error and disables submit", async ({
    page,
  }) => {
    await page.locator("#new-password").fill("tempPASS1!");
    await page.locator("#repeat-new-password").fill("tempPASS2!");
    await page.locator("#repeat-new-password").blur();

    await expect(page.locator("#repeat-new-password-error-msg")).toContainText(
      /must match/i,
    );
    await expect(page.locator('[data-cy="submitNewPassword"]')).toBeDisabled();
  });

  test("touched empty required fields show visible required errors", async ({
    page,
  }) => {
    await page.locator("#loginName").click();
    await page.locator("#current-password").click();
    await page.locator("#new-password").click();
    await page.locator("#loginName").click();

    await expect(page.locator("#loginName-error-msg")).toContainText(
      /required/i,
    );
    await expect(page.locator("#current-password-error-msg")).toContainText(
      /required/i,
    );
  });

  test("wrong current password shows an error notification and stays on the form", async ({
    page,
  }) => {
    await page.locator("#loginName").fill("admin");
    await page.locator("#current-password").fill("wrongPASS9!");
    await page.locator("#new-password").fill("tempPASS1!");
    await page.locator("#repeat-new-password").fill("tempPASS1!");
    await page.locator('[data-cy="submitNewPassword"]').click();

    // backend rejects a wrong current password with
    // login.error.password.current.incorrect; the SPA must surface it as an
    // error toast and must NOT run the success flow (no redirect to /login).
    // .first(): the app mounts two AlertDialog instances, so the toast renders twice.
    await expect(
      page.locator(".cds--toast-notification--error").first(),
    ).toBeVisible();
    await expect(page).toHaveURL(/ChangePasswordLogin/);
  });
});
