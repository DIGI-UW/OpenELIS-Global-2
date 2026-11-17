import { test, expect } from "../../../helpers/test-base";
import { UI_TIMEOUT } from "../../../helpers/timeouts";
import { API_PREFIX } from "../../../helpers/admin-ui";

interface RegistryModule {
  id: string;
  state: string;
  assetBase?: string;
  frontend?: { entry?: string; routes?: Array<{ path: string }> } | null;
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

  test("the registry is requested beside the session check, not after it", async ({
    page,
  }) => {
    // 300 ms round trips on the API alone: on a fresh document load the
    // registry request must leave before the session answer arrives, so a
    // signed-in user's modules start one round trip earlier. A dev server
    // loads more than the 250 resources the timing buffer keeps by default.
    await page.addInitScript(() =>
      performance.setResourceTimingBufferSize(5000),
    );
    await page.route("**/api/OpenELIS-Global/**", async (route) => {
      await new Promise((resolve) => setTimeout(resolve, 300));
      await route.continue();
    });
    await page.goto("/Dashboard", { waitUntil: "networkidle" });
    const timings = await page.evaluate(() =>
      performance
        .getEntriesByType("resource")
        .filter((entry) =>
          /\/api\/OpenELIS-Global\/(session|rest\/module-registry)(\?|$)/.test(
            entry.name,
          ),
        )
        .map((entry) => ({
          url: entry.name.replace(/^.*\/api\/OpenELIS-Global/, ""),
          start: entry.startTime,
          end: (entry as PerformanceResourceTiming).responseEnd,
        })),
    );
    const session = timings.find((entry) => entry.url === "/session");
    const registry = timings.find((entry) =>
      entry.url.startsWith("/rest/module-registry"),
    );
    expect(session, "the session check was requested").toBeTruthy();
    expect(registry, "the registry was requested").toBeTruthy();
    expect(registry!.start).toBeLessThan(session!.end);
    await page.unroute("**/api/OpenELIS-Global/**");
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

  test("a module whose entry does not load shows its notice at once, and Retry loads it", async ({
    page,
  }) => {
    await page.goto("/Dashboard", { waitUntil: "domcontentloaded" });
    const registry = (await (
      await page.request.get(`${API_PREFIX}/rest/module-registry`)
    ).json()) as Registry;
    const started = registry.modules.find(
      (module) =>
        module.state === "STARTED" &&
        module.assetBase &&
        module.frontend?.entry &&
        module.frontend?.routes?.length,
    );
    test.skip(!started, "no started module with pages in this installation");

    const entry = `**${started!.assetBase}${started!.frontend!.entry}*`;
    let blockedAt = 0;
    await page.route(entry, (route) => {
      blockedAt = blockedAt || Date.now();
      return route.abort();
    });
    await page.goto(started!.frontend!.routes![0].path, {
      waitUntil: "domcontentloaded",
    });
    // One import attempt: the runtime keeps a failed entry, so a second try
    // at the same URL would fail without a request. The notice follows the
    // failure directly, not after a back-off.
    await expect(page.getByTestId("module-error")).toBeVisible({
      timeout: UI_TIMEOUT,
    });
    const noticeAfterMs = Date.now() - blockedAt;
    test.info().annotations.push({
      type: "notice after the blocked entry",
      description: `${noticeAfterMs} ms`,
    });
    expect(blockedAt).toBeGreaterThan(0);
    // The removed back-off alone kept the notice 900 ms away; the bound leaves
    // room for a loaded machine (950 ms seen beside a full unit-test run).
    expect(noticeAfterMs).toBeLessThan(1500);
    await expect(
      page.getByText("Module unavailable", { exact: true }),
    ).toBeVisible();
    await expect(page.locator(".cds--side-nav")).toBeVisible();

    await page.unroute(entry);
    await page.getByRole("button", { name: "Retry", exact: true }).click();
    await expect(page.getByTestId("module-error")).toHaveCount(0, {
      timeout: UI_TIMEOUT,
    });
    await expect(page.getByTestId("content-wrapper")).not.toBeEmpty();
  });
});
