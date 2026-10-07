import { test as setup, expect } from "../helpers/test-base";
import { LONG_TIMEOUT, NAV_TIMEOUT } from "../helpers/timeouts";
import {
  seedParticipantUser,
  PARTICIPANT_USER,
} from "../helpers/seed-eqa-data";

const AUTH_FILE = "playwright/.auth/participant.json";

/**
 * A second authenticated session, for a laboratory user who takes part in EQA
 * but does not administer it.
 *
 * Every provider-side check ORs in the global administrator role, so the
 * ordinary test user can never render the participant-only branch of the
 * module — asserting that branch needs a login without the provider grant.
 * The user is created here rather than assumed, because a fresh database or a
 * CI runner has no such row; the credentials are the standard test password,
 * since the seeder copies the existing test user's stored hash.
 *
 * The login dance mirrors auth.setup.ts, and for the same reason: the React
 * login page mints an anonymous session on mount, which Spring's session
 * fixation protection then refuses to upgrade, so credentials go through the
 * request API and the resulting cookie is re-added at the root path.
 */
setup("authenticate participant", async ({ page, request, context }, info) => {
  info.setTimeout(NAV_TIMEOUT);

  seedParticipantUser();

  const password = process.env.TEST_PASS || "adminADMIN!";
  const loginResponse = await request.post(
    "/api/OpenELIS-Global/ValidateLogin?apiCall=true",
    { form: { loginName: PARTICIPANT_USER, password } },
  );
  const loginData = await loginResponse.json().catch(() => null);
  if (loginResponse.status() !== 200 || !loginData?.success) {
    throw new Error(
      `Participant login returned ${loginResponse.status()}: ${JSON.stringify(loginData)}\n` +
        `  User: ${PARTICIPANT_USER}\n` +
        "  The seeder copies the admin password hash, so this fails if the\n" +
        "  admin password was changed after the fixture user was created.",
    );
  }

  const jsessionId = loginResponse
    .headersArray()
    .filter((header) => header.name.toLowerCase() === "set-cookie")
    .map((header) => header.value.match(/JSESSIONID=([^;]+)/)?.[1])
    .find((value) => value !== undefined);
  if (!jsessionId) {
    throw new Error("Participant login succeeded but returned no JSESSIONID.");
  }

  await context.addCookies([
    {
      name: "JSESSIONID",
      value: jsessionId,
      domain: new URL(process.env.BASE_URL || "https://localhost").hostname,
      path: "/",
      httpOnly: true,
      secure: true,
      sameSite: "Lax",
    },
  ]);

  const sessionResponse = page.waitForResponse(
    (response) =>
      response.url().includes("/api/OpenELIS-Global/session") && response.ok(),
    { timeout: LONG_TIMEOUT },
  );
  await page.goto("/", { waitUntil: "domcontentloaded" });
  await sessionResponse;
  await expect(page).not.toHaveURL(/\/login(?:\?|$)/, {
    timeout: LONG_TIMEOUT,
  });

  await page.context().storageState({ path: AUTH_FILE });
});
