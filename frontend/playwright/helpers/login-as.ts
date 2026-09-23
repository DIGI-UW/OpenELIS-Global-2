import { BrowserContext, APIRequestContext } from "@playwright/test";

/**
 * Authenticate an arbitrary user into a browser context (spec 012 T029/T044).
 *
 * Mirrors auth.setup.ts: log in through the request API, then inject the
 * resulting JSESSIONID into the browser context with path=/ so frontend routes
 * see the authenticated session. Used to drive the app as distinct RBAC
 * personas (Reception, Results, Validation) that the shared admin storageState
 * cannot represent.
 *
 * The cookie jar is cleared BEFORE the login request, and that is load-bearing.
 * Inside the test runner, `browser.newContext()` inherits the project's `use`
 * options, including the admin `storageState`, so a "fresh" context already
 * carries the admin's JSESSIONID. Posting the login with that cookie makes
 * Spring's `sessionFixation().migrateSession()` rotate the ADMIN session's id:
 * the persona logs in fine, but the id every later test's storageState carries
 * is dead, and the rest of the shard redirects to /LoginPage (62 specs, E2E on
 * 2026-09-21; the trace showed the persona login sent the admin's cookie).
 *
 * @returns true on success; false if the credentials were rejected (e.g. the
 *          seeded persona is missing) so callers can skip gracefully.
 */
export async function loginAs(
  context: BrowserContext,
  request: APIRequestContext,
  loginName: string,
  password: string,
): Promise<boolean> {
  // See the note above: drop the inherited admin session before logging in.
  await context.clearCookies();
  const loginResponse = await request.post(
    "/api/OpenELIS-Global/ValidateLogin?apiCall=true",
    { form: { loginName, password } },
  );
  const loginData = await loginResponse.json().catch(() => null);
  if (loginResponse.status() !== 200 || !loginData?.success) {
    return false;
  }

  let jsessionId: string | null = null;
  for (const header of loginResponse.headersArray()) {
    if (header.name.toLowerCase() === "set-cookie") {
      const match = header.value.match(/JSESSIONID=([^;]+)/);
      if (match) {
        jsessionId = match[1];
        break;
      }
    }
  }
  if (!jsessionId) {
    const storageState = await request.storageState();
    jsessionId =
      storageState.cookies.find((c) => c.name === "JSESSIONID")?.value || null;
  }
  if (!jsessionId) {
    return false;
  }

  const host = new URL(process.env.BASE_URL || "https://localhost").hostname;
  await context.clearCookies();
  await context.addCookies([
    { name: "JSESSIONID", value: jsessionId, domain: host, path: "/" },
  ]);
  return true;
}
