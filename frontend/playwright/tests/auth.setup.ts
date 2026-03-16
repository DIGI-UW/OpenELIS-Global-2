import { test as setup, expect } from "@playwright/test";

const AUTH_FILE = "playwright/.auth/user.json";

/**
 * Shared authentication setup for all Playwright projects.
 *
 * Flow:
 *   1. Verify backend health (API responds, not just HTML shell)
 *   2. Login via Playwright request API (ValidateLogin endpoint)
 *   3. Inject the authenticated JSESSIONID into the browser context
 *   4. Navigate to verify authenticated state
 *   5. Save storage state for downstream tests
 *
 * Why request API + cookie injection:
 *   - The React login page creates an anonymous JSESSIONID on mount.
 *     Spring Security's session fixation protection rejects credentials
 *     when a prior session exists → UI form login fails from Playwright.
 *   - The request API avoids this, but its JSESSIONID has path=/api/...
 *     which doesn't cover frontend routes.
 *   - Solution: authenticate via request API, extract the JSESSIONID,
 *     and add it to the browser context with path=/ so all routes work.
 */
setup("authenticate", async ({ page, request, context }, testInfo) => {
  testInfo.setTimeout(60_000);

  const username = process.env.TEST_USER;
  const password = process.env.TEST_PASS;

  if (!username || !password) {
    throw new Error(
      "TEST_USER and TEST_PASS environment variables must be set.\n" +
        "  Source .env from repo root: set -a; . .env; set +a\n" +
        "  Or use ANSI-C quoting: export TEST_PASS=$'adminADMIN!'",
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

  // ── Step 2: Login via request API ───────────────────────────────
  const loginResponse = await request.post(
    "/api/OpenELIS-Global/ValidateLogin?apiCall=true",
    {
      form: { loginName: username, password: password },
    },
  );
  const loginData = await loginResponse.json().catch(() => null);

  if (loginResponse.status() !== 200 || !loginData?.success) {
    throw new Error(
      `Login API returned ${loginResponse.status()}: ${JSON.stringify(loginData)}\n` +
        `  Credentials: ${username} / ***\n` +
        "  Possible causes:\n" +
        "    - Wrong password (check TEST_PASS env var)\n" +
        "    - Credentials: source .env from repo root (set -a; . .env; set +a)\n" +
        "    - Account locked (check login_user.account_locked in DB)\n" +
        "    - Fixtures not loaded (run load-test-fixtures.sh to reset admin password)",
    );
  }

  // ── Step 3: Inject JSESSIONID into browser context ──────────────
  // The request API's JSESSIONID has path=/api/OpenELIS-Global — too
  // narrow for frontend routes. Extract it and re-add with path=/.
  const setCookieHeaders = loginResponse
    .headersArray()
    .filter((h) => h.name.toLowerCase() === "set-cookie");

  let jsessionId: string | null = null;
  for (const header of setCookieHeaders) {
    const match = header.value.match(/JSESSIONID=([^;]+)/);
    if (match) {
      jsessionId = match[1];
      break;
    }
  }

  if (!jsessionId) {
    // Fallback: try to get from the request context's stored cookies
    const storageState = await request.storageState();
    const sessionCookie = storageState.cookies.find(
      (c) => c.name === "JSESSIONID",
    );
    if (sessionCookie) {
      jsessionId = sessionCookie.value;
    }
  }

  if (!jsessionId) {
    throw new Error(
      "Login succeeded but no JSESSIONID cookie found in response.\n" +
        "  This is unexpected — check proxy/backend cookie configuration.",
    );
  }

  // Add the cookie to the browser context with root path
  await context.addCookies([
    {
      name: "JSESSIONID",
      value: jsessionId,
      domain: "localhost",
      path: "/",
      httpOnly: true,
      secure: true,
      sameSite: "Lax",
    },
  ]);

  // ── Step 4: Verify authenticated state ────────────────────────
  await page.goto("analyzers", { waitUntil: "domcontentloaded" });
  await expect(page).not.toHaveURL(/\/login(?:\?|$)/, { timeout: 15_000 });

  // ── Step 5: Save session ──────────────────────────────────────
  await page.context().storageState({ path: AUTH_FILE });
});
