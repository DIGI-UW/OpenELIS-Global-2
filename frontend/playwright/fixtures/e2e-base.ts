import { test as base, expect, type Page } from "@playwright/test";

type AuthFlowType = "read" | "mutating";
type AuthMode = "shared-session" | "worker-isolated";

type E2EBaseFixtures = {
  gotoAndWait: (route: string) => Promise<void>;
  ensureAuthenticatedShell: () => Promise<void>;
  ensureAuthForScenario: (flowType?: AuthFlowType) => Promise<void>;
  resolveAuthMode: (flowType?: AuthFlowType) => AuthMode;
};

async function waitForShell(page: Page) {
  await expect(page.locator("#sidenav-menu-button")).toBeVisible();
}

function normalizeAuthMode(mode: string): AuthMode {
  if (mode === "worker-isolated") {
    return "worker-isolated";
  }
  return "shared-session";
}

function resolveAuthModeForFlow(flowType: AuthFlowType): AuthMode {
  const strategyMode = process.env.PW_AUTH_STRATEGY_MODE || "hybrid";

  if (strategyMode === "shared") {
    return "shared-session";
  }

  if (strategyMode === "isolated") {
    return "worker-isolated";
  }

  if (flowType === "mutating") {
    return normalizeAuthMode(
      process.env.PW_AUTH_MUTATING_MODE || "worker-isolated",
    );
  }

  return normalizeAuthMode(process.env.PW_AUTH_READ_MODE || "shared-session");
}

function resolveWorkerCredential(baseName: string, workerIndex: number) {
  return (
    process.env[`${baseName}_WORKER_${workerIndex}`] ||
    process.env[`${baseName}_WORKER`] ||
    process.env[baseName]
  );
}

async function fillLoginForm(page: Page, username: string, password: string) {
  const usernameInputByLabel = page.getByLabel("Username");
  const passwordInputByLabel = page.getByLabel("Password");
  const usernameInputById = page.locator("#loginName");
  const passwordInputById = page.locator("#password");

  const usernameByLabelVisible = await usernameInputByLabel
    .isVisible({ timeout: 5000 })
    .catch(() => false);
  if (usernameByLabelVisible) {
    await usernameInputByLabel.fill(username);
  } else {
    await usernameInputById.waitFor({ state: "visible", timeout: 10000 });
    await usernameInputById.fill(username);
  }

  const passwordByLabelVisible = await passwordInputByLabel
    .isVisible({ timeout: 3000 })
    .catch(() => false);
  if (passwordByLabelVisible) {
    await passwordInputByLabel.fill(password);
  } else {
    await passwordInputById.waitFor({ state: "visible", timeout: 10000 });
    await passwordInputById.fill(password);
  }

  const loginByRole = page.getByRole("button", { name: /login/i });
  const loginByRoleVisible = await loginByRole
    .isVisible({ timeout: 3000 })
    .catch(() => false);
  if (loginByRoleVisible) {
    await loginByRole.click();
    return;
  }

  await page.locator("[data-cy='loginButton']").click();
}

async function ensureWorkerIsolatedAuth(page: Page, workerIndex: number) {
  const username = resolveWorkerCredential("TEST_USER", workerIndex);
  const password = resolveWorkerCredential("TEST_PASS", workerIndex);

  if (!username || !password) {
    await waitForShell(page);
    return;
  }

  await page.context().clearCookies();
  await page.goto("/login");
  await fillLoginForm(page, username, password);
  await waitForShell(page);
}

export const test = base.extend<E2EBaseFixtures>({
  gotoAndWait: async ({ page }, use) => {
    await use(async (route: string) => {
      await page.goto(route);
      await waitForShell(page);
    });
  },

  ensureAuthenticatedShell: async ({ page }, use) => {
    await use(async () => {
      await waitForShell(page);
      await expect(page.locator("#mainHeader")).toBeVisible();
    });
  },

  ensureAuthForScenario: async ({ page }, use, testInfo) => {
    await use(async (flowType: AuthFlowType = "read") => {
      const authMode = resolveAuthModeForFlow(flowType);
      if (flowType === "mutating" && authMode === "worker-isolated") {
        await ensureWorkerIsolatedAuth(page, testInfo.workerIndex);
      } else {
        await waitForShell(page);
      }

      await expect(page.locator("#mainHeader")).toBeVisible();
    });
  },

  resolveAuthMode: async ({}, use) => {
    await use((flowType: AuthFlowType = "read") =>
      resolveAuthModeForFlow(flowType),
    );
  },
});

export { expect };
