import { test, expect } from "../../../helpers/test-base";
import { loginAs } from "../../../helpers/login-as";
import {
  seedRbacPersonas,
  RBAC_PERSONA_PASSWORD,
  RBAC_RESULTS_USER,
  RBAC_VALIDATION_USER,
} from "../../../helpers/seed-rbac-personas";

/**
 * Spec 012 T029 — role → privilege scoping, end to end.
 *
 * NOTE ON THE MODEL: the shipped base roles are FLAT (012-004 seed comment:
 * "BASE ROLES SHIP FLAT — no inheritance between them"). Inheritance via
 * grouping_parent is the extension mechanism for implementer-defined custom
 * roles, NOT a relationship between the base roles. So a Validation user is
 * scoped to result:validate/result:view and does NOT silently gain
 * result:enter — that separation is the property under test here. (The
 * inheritance walk itself is covered at the unit level by
 * PrivilegeResolutionTest / PrivilegeServiceImplTest.)
 *
 * Asserts, per single-role persona:
 *   - Results    → result:enter present  → reaches result entry
 *   - Validation → result:validate present, result:enter ABSENT → reaches
 *                  validation but not result entry
 *
 * Self-skips when the personas can't be seeded or logged in.
 */
test.describe("RBAC role → privilege scoping", () => {
  test.beforeAll(() => {
    test.skip(
      !seedRbacPersonas(),
      "RBAC personas could not be seeded (no DB access)",
    );
  });

  async function sessionPrivileges(page): Promise<string[]> {
    return page.evaluate(async () => {
      const res = await fetch("/api/OpenELIS-Global/session", {
        credentials: "include",
      });
      const body = await res.json();
      return body.privileges || [];
    });
  }

  test("Results persona is scoped to result:enter and reaches result entry", async ({
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
    await page.goto("/AccessionResults", { waitUntil: "domcontentloaded" });
    await expect(
      page.getByRole("textbox", { name: "Enter Accession Number" }),
    ).toBeVisible();

    const privileges = await sessionPrivileges(page);
    expect(privileges).toContain("result:enter");
    expect(privileges).toContain("result:view");
    await context.close();
  });

  test("Validation persona is scoped to result:validate — has validation, NOT result entry", async ({
    browser,
  }) => {
    const context = await browser.newContext();
    const ok = await loginAs(
      context,
      context.request,
      RBAC_VALIDATION_USER,
      RBAC_PERSONA_PASSWORD,
    );
    test.skip(!ok, "Validation persona login unavailable");

    const page = await context.newPage();
    page.on("dialog", (d) => {
      void d.accept().catch(() => undefined);
    });

    // Validation surface (role={Roles.VALIDATION} → result:validate): granted.
    await page.goto("/AccessionValidation", { waitUntil: "domcontentloaded" });
    await expect(
      page.getByRole("textbox", { name: "Enter Accession Number" }),
    ).toBeVisible();

    const privileges = await sessionPrivileges(page);
    expect(privileges).toContain("result:validate");
    // Flat roles: Validation does NOT gain Results' result:enter.
    expect(privileges).not.toContain("result:enter");
    await context.close();
  });
});
