import { test as setup, expect } from "@playwright/test";

const AUTH_FILE = "playwright/.auth/user.json";

/**
 * Shared authentication setup for all Playwright projects.
 *
 * Flow:
 *   1. Verify backend health (API responds, not just HTML shell)
 *   2. Login via in-page fetch (same ValidateLogin endpoint the React form uses)
 *   3. Navigate to verify authenticated state and capture cookies
 *   4. Save storage state for downstream tests
 *
 * Note: The login uses page.evaluate() to call ValidateLogin directly rather
 * than clicking the UI form. OE's React login fires doLogin() twice with an
 * async GET /LoginPage in between, which creates navigation timing issues
 * with Playwright's waitForURL. The direct fetch approach is deterministic.
 */
setup("authenticate", async ({ page, request }, testInfo) => {
  testInfo.setTimeout(60_000);

  const username = process.env.TEST_USER;
  const password = process.env.TEST_PASS;

  if (!username || !password) {
    throw new Error(
      "TEST_USER and TEST_PASS environment variables must be set.\n" +
        "  export TEST_USER=admin TEST_PASS='adminADMIN!'\n" +
        "  Note: use single quotes to prevent zsh history expansion of !",
    );
  }

  // ── Step 1: Backend health check ──────────────────────────────
  let backendReady = false;
  for (let attempt = 1; attempt <= 12; attempt++) {
    try {
      const health = await request.get("/health", { timeout: 5_000 });
      if (health.ok()) {
        backendReady = true;
        break;
      }
    } catch {
      // connection refused or timeout
    }
    if (attempt % 4 === 0) {
      console.log(
        `  auth-setup: waiting for backend... (${attempt * 5}s elapsed)`,
      );
    }
    await page.waitForTimeout(5_000);
  }
  if (!backendReady) {
    throw new Error(
      "Backend health check failed after 60s.\n" +
        "  Ensure the OE container is running and accessible at the baseURL.",
    );
  }

  // ── Step 2: Login ─────────────────────────────────────────────
  // Navigate to login page to establish browser session cookie,
  // then call ValidateLogin via fetch from the page context.
  await page.goto("login", { waitUntil: "domcontentloaded" });
  await page.waitForTimeout(1_000);

  const loginResult = await page.evaluate(
    async ({ user, pass }) => {
      await fetch("/api/OpenELIS-Global/LoginPage", {
        credentials: "include",
      });
      const res = await fetch(
        "/api/OpenELIS-Global/ValidateLogin?apiCall=true",
        {
          credentials: "include",
          method: "POST",
          headers: { "Content-Type": "application/x-www-form-urlencoded" },
          body: `loginName=${encodeURIComponent(user)}&password=${encodeURIComponent(pass)}`,
        },
      );
      const data = await res.json().catch(() => null);
      return { status: res.status, data };
    },
    { user: username, pass: password },
  );

  if (loginResult.status !== 200 || !loginResult.data?.success) {
    throw new Error(
      `Login API returned ${loginResult.status}: ${JSON.stringify(loginResult.data)}\n` +
        `  Credentials: ${username} / ***\n` +
        "  Possible causes:\n" +
        "    - Wrong password (check TEST_PASS env var)\n" +
        '    - zsh ! escaping: use double quotes: TEST_PASS="adminADMIN!"\n' +
        "    - Account locked (check login_user.account_locked in DB)",
    );
  }

  // Navigate to propagate cookies and verify auth
  await page.goto("/", { waitUntil: "domcontentloaded" });
  await page.waitForTimeout(1_000);

  // ── Step 3: Verify authenticated state ────────────────────────
  await page.goto("analyzers", { waitUntil: "domcontentloaded" });
  await expect(page).not.toHaveURL(/\/login(?:\?|$)/, { timeout: 15_000 });

  // ── Step 4: Save session ──────────────────────────────────────
  await page.context().storageState({ path: AUTH_FILE });
});
