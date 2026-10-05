import { test, expect } from "../../../helpers/test-base";
import { loginAs } from "../../../helpers/login-as";
import {
  seedRbacPersonas,
  RBAC_PERSONA_PASSWORD,
  RBAC_RECEPTION_USER,
  RBAC_RESULTS_USER,
} from "../../../helpers/seed-rbac-personas";

/**
 * Spec 012 T044 — privilege-aware route access.
 *
 * The Results route (`/Results`, which /AccessionResults redirects to since
 * develop #4528) is guarded by SecureRoute with
 * `role={Roles.RESULTS}`. With privilege-equivalent granting (T040), access is
 * driven by the privilege that role really guards — `result:enter` — not the
 * role name string:
 *   - Reception (order:create, no result:enter) → access denied
 *   - Results   (result:enter) → access granted
 *
 * Base roles ship FLAT (012-004): each grants its own privilege set with no
 * inheritance between them, so this asserts the shipped model exactly. Single-
 * role personas make the assertion real — the admin session would trivially
 * pass everything.
 *
 * Self-skips if Docker/psql (seeding) or the persona login is unavailable.
 */
test.describe("RBAC privilege-aware route access", () => {
  test.beforeAll(() => {
    test.skip(
      !seedRbacPersonas(),
      "RBAC personas could not be seeded (no DB access)",
    );
  });

  test("Reception is denied the Results entry route", async ({ browser }) => {
    const context = await browser.newContext();
    const ok = await loginAs(
      context,
      context.request,
      RBAC_RECEPTION_USER,
      RBAC_PERSONA_PASSWORD,
    );
    test.skip(!ok, "Reception persona login unavailable");

    const page = await context.newPage();
    page.on("dialog", (d) => {
      void d.accept().catch(() => undefined);
    });
    await page.goto("/Results", { waitUntil: "domcontentloaded" });

    // SecureRoute denies (Reception lacks result:enter): confirmAlert fires and
    // redirects to origin, so the unified Results search box never renders.
    // Asserted against the control the page actually has - "Enter Accession
    // Number" belonged to the pages develop #4528 retired, so a count of 0 on
    // it would pass for every role and prove nothing.
    await expect(
      page.getByRole("searchbox", { name: "Search by lab number" }),
    ).toHaveCount(0);
    await context.close();
  });

  test("Results persona reaches the Results entry route via result:enter", async ({
    browser,
  }) => {
    const context = await browser.newContext();
    const ok = await loginAs(
      context,
      context.request,
      RBAC_RESULTS_USER,
      RBAC_PERSONA_PASSWORD,
    );
    test.skip(!ok, "Results persona login unavailable");

    const page = await context.newPage();
    // develop #4528 retired the legacy result-entry pages; /AccessionResults
    // now redirects to the unified /Results, which carries the same
    // RESULT_ENTER guard. Assert on where the role actually lands.
    await page.goto("/Results", { waitUntil: "domcontentloaded" });

    // Granted: the role={Roles.RESULTS} route opens because the persona holds
    // result:enter — the privilege that role check maps to (T040), proving the
    // gate is privilege-driven rather than a role-name comparison.
    await expect(
      page.getByRole("searchbox", { name: "Search by lab number" }),
    ).toBeVisible();
    await context.close();
  });
});
