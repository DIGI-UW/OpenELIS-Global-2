import { test, expect } from "../../../helpers/test-base";
import { UI_TIMEOUT } from "../../../helpers/timeouts";
import { API_PREFIX } from "../../../helpers/admin-ui";

interface RegistryModule {
  id: string;
  state: string;
}

interface Registry {
  frontendContractVersion: string;
  modules: RegistryModule[];
}

// The module shell with whatever this installation carries: CI runs it with
// no module installed, a developer's stack may have one. Either way the
// registry answers, the admin page reflects it and an unknown module route
// is refused with a notice instead of a blank page.
test.describe("Module shell", () => {
  test("the registry answers for the signed-in shell", async ({ page }) => {
    await page.goto("/Dashboard", { waitUntil: "domcontentloaded" });

    const response = await page.request.get(
      `${API_PREFIX}/rest/module-registry`,
    );
    expect(response.status()).toBe(200);
    const registry = (await response.json()) as Registry;
    expect(registry.frontendContractVersion).toBe("1.0.0");
    expect(Array.isArray(registry.modules)).toBe(true);
  });

  test("the admin Modules page lists what is installed", async ({ page }) => {
    await page.goto("/Dashboard", { waitUntil: "domcontentloaded" });
    const registry = (await (
      await page.request.get(`${API_PREFIX}/rest/module-registry`)
    ).json()) as Registry;

    await page.goto("/MasterListsPage", { waitUntil: "domcontentloaded" });
    const modulesLink = page.locator('[data-cy="modulesAdmin"]');
    await expect(modulesLink).toBeVisible({ timeout: UI_TIMEOUT });
    await modulesLink.click();

    await expect(page).toHaveURL(/\/MasterListsPage\/modules$/);
    await expect(page.getByTestId("modules-admin-title")).toHaveText("Modules");
    await expect(page.getByTestId("modules-admin-versions")).toContainText(
      "frontend contract 1.0.0",
      { timeout: UI_TIMEOUT },
    );
    if (registry.modules.length === 0) {
      await expect(page.getByTestId("modules-admin-empty")).toHaveText(
        "No modules are installed.",
      );
    } else {
      for (const module of registry.modules) {
        await expect(page.getByTestId(`module-row-${module.id}`)).toContainText(
          module.state,
        );
      }
    }
  });

  test("an unknown module route shows the unavailable notice", async ({
    page,
  }) => {
    await page.goto("/m/no-such-module/home", {
      waitUntil: "domcontentloaded",
    });

    await expect(
      page.getByText("Module unavailable", { exact: true }),
    ).toBeVisible({ timeout: UI_TIMEOUT });
    await expect(
      page.getByText(
        "The module no-such-module is not available in this installation.",
        { exact: false },
      ),
    ).toBeVisible();
    await expect(page.locator(".cds--side-nav")).toBeVisible();
  });
});
