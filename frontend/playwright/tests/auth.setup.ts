import { test as setup, expect } from "@playwright/test";

const AUTH_FILE = "playwright/.auth/user.json";

/**
 * Shared authentication setup for all Playwright projects.
 *
 * Flow:
 *   1. Verify backend health (API responds, not just HTML shell)
 *   2. Login with retry (handles transient CSRF / session issues)
 *   3. Verify authenticated state on a protected route
 *   4. Save storage state for downstream tests
 */
setup("authenticate", async ({ page, request }, testInfo) => {
  testInfo.setTimeout(120_000);

  const username = process.env.TEST_USER;
  const password = process.env.TEST_PASS;

  if (!username || !password) {
    throw new Error(
      "TEST_USER and TEST_PASS environment variables must be set.\n" +
        "  export TEST_USER=admin TEST_PASS='adminADMIN!'",
    );
  }

  // ── Step 1: Backend health check ──────────────────────────────
  // Wait for OE to be fully booted (not just serving HTML).
  // The login page can render before Spring Security is initialized.
  let backendReady = false;
  for (let attempt = 1; attempt <= 24; attempt++) {
    try {
      const health = await request.get("/health", { timeout: 5_000 });
      if (health.ok()) {
        backendReady = true;
        break;
      }
    } catch {
      // connection refused or timeout — backend not ready yet
    }
    if (attempt % 6 === 0) {
      console.log(
        `  auth-setup: waiting for backend... (${attempt * 5}s elapsed)`,
      );
    }
    await page.waitForTimeout(5_000);
  }
  if (!backendReady) {
    throw new Error(
      "Backend health check failed after 120s.\n" +
        "  Ensure the OE container is running and accessible at the baseURL.\n" +
        '  Check logs: docker logs <oe-container> 2>&1 | grep -i "error\\|exception"',
    );
  }

  // ── Step 2: Login with retry ──────────────────────────────────
  // Retry the full login flow (navigate → fill → submit → verify redirect).
  // This handles transient failures: CSRF token mismatch, stale session,
  // form not yet hydrated, or server temporarily rejecting logins.
  const MAX_LOGIN_ATTEMPTS = 3;
  let loginSuccess = false;
  let lastError: string | undefined;

  for (let attempt = 1; attempt <= MAX_LOGIN_ATTEMPTS; attempt++) {
    // Navigate to login page with a fresh page state
    await page.goto("login", { waitUntil: "domcontentloaded" });

    // If we're already past login (e.g., session still valid), skip
    if (!page.url().includes("/login")) {
      loginSuccess = true;
      break;
    }

    // Wait for form inputs to appear (OE renders shell before hydration)
    const usernameInput = page
      .getByRole("textbox", { name: /username/i })
      .or(page.locator('input[name="loginName"]'))
      .first();
    const passwordInput = page
      .locator('input[type="password"]')
      .or(page.locator('input[name="password"]'))
      .first();

    try {
      await expect(usernameInput).toBeVisible({ timeout: 10_000 });
      await expect(passwordInput).toBeVisible({ timeout: 5_000 });
    } catch {
      lastError = `Login form not visible (attempt ${attempt})`;
      await page.waitForTimeout(3_000);
      continue;
    }

    // Clear and fill credentials
    await usernameInput.clear();
    await usernameInput.fill(username);
    await passwordInput.clear();
    await passwordInput.fill(password);

    // Submit and wait for redirect away from /login
    const loginButton = page
      .getByRole("button", { name: /^(login|submit|sign.in)$/i })
      .first();
    try {
      await Promise.all([
        page.waitForURL((url) => !url.pathname.endsWith("/login"), {
          timeout: 15_000,
        }),
        loginButton.click(),
      ]);
      loginSuccess = true;
      break;
    } catch {
      // Check what went wrong
      const currentUrl = page.url();
      if (!currentUrl.includes("/login")) {
        // Actually navigated away — success despite timeout race
        loginSuccess = true;
        break;
      }
      // Check for visible error messages on the page
      const errorText = await page
        .locator(".error, .login-error, [role='alert']")
        .textContent()
        .catch(() => null);
      lastError = errorText
        ? `Login rejected (attempt ${attempt}): ${errorText}`
        : `Login did not redirect (attempt ${attempt}). Page stayed on /login.`;
      console.log(`  auth-setup: ${lastError}`);
      // Brief pause before retry to let server state settle
      await page.waitForTimeout(2_000);
    }
  }

  if (!loginSuccess) {
    throw new Error(
      `Authentication failed after ${MAX_LOGIN_ATTEMPTS} attempts.\n` +
        `  Last error: ${lastError}\n` +
        `  Credentials: ${username} / ***\n` +
        "  Possible causes:\n" +
        "    - Wrong password (check TEST_PASS env var)\n" +
        "    - Account locked (check login_user.account_locked in DB)\n" +
        '    - Backend error (check: docker logs <oe-container> 2>&1 | grep "login")',
    );
  }

  // ── Step 3: Verify authenticated state ────────────────────────
  // Navigate to a protected route and confirm we're not bounced to /login.
  await page.goto("analyzers", { waitUntil: "networkidle" });
  await expect(page).not.toHaveURL(/\/login(?:\?|$)/, { timeout: 15_000 });

  // ── Step 4: Save session ──────────────────────────────────────
  await page.context().storageState({ path: AUTH_FILE });
});
