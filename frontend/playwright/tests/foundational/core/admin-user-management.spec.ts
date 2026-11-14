import {
  test,
  expect,
  type Locator,
  type Page,
} from "../../../helpers/test-base";
import type { Browser } from "@playwright/test";
import { csrfToken } from "../../../helpers/api-session";
import { cellUnder } from "../../../helpers/admin-ui";
import { NAV_TIMEOUT } from "../../../helpers/timeouts";

/**
 * Admin, User Management: adding a user with its account state and roles,
 * leaving Add User without saving, finding users by name and by filter,
 * copying one user's permissions to another, and deactivating a user so the
 * account can no longer sign in.
 *
 * Neighbouring specs cover the rest of this screen: the Only Active
 * checkbox's own state (admin-user-filters), lab unit rows and the All Lab
 * Units rule (admin-user-lab-unit-roles), and refused saves
 * (ogc-1545-user-save-refusals).
 */

const API = "/api/OpenELIS-Global";
const USER_ENDPOINT = `${API}/rest/UnifiedSystemUser`;
const USER_SEARCH = `${API}/rest/SearchUnifiedSystemUserMenu`;
const ALL_LAB_UNITS = "AllLabUnits";
const PASSWORD = "Pw!Passw0rd";
const seededUsers = new WeakMap<Page, Set<string>>();

// The seeded username charset has no digits, so the unique suffix is letters.
const letters = (length: number) =>
  Array.from({ length }, () =>
    String.fromCharCode(97 + Math.floor(Math.random() * 26)),
  ).join("");

type Role = { elementID: string; roleId: string; roleName: string };
type Section = { id: string; value: string };
type UserForm = {
  globalRoles: Role[];
  labUnitRoles: Role[];
  testSections: Section[];
  selectedRoles: string[];
  selectedTestSectionLabUnits: Record<string, string[]>;
  accountLocked: string;
  accountDisabled: string;
  accountActive: string;
  [field: string]: unknown;
};

type SeededUser = {
  login: string;
  first: string;
  last: string;
  active?: boolean;
  roles?: string[];
  labUnits?: Record<string, string[]>;
};

async function blankForm(page: Page): Promise<UserForm> {
  return (
    await page.request.get(`${USER_ENDPOINT}?ID=0&startingRecNo=1&roleFilter=`)
  ).json();
}

/** The user's list row through the screen's own endpoint, if listed. */
async function listedUser(page: Page, login: string) {
  const menu = await (
    await page.request.get(
      `${USER_SEARCH}?search=Y&startingRecNo=1&searchString=${login}&filter=`,
    )
  ).json();
  const row = (
    menu.menuList as {
      systemUserId: string;
      loginUserId: string;
      loginName: string;
    }[]
  ).find((user) => user.loginName === login);
  // The edit screen addresses a user by "<systemUserId>-<loginUserId>".
  return row ? `${row.systemUserId}-${row.loginUserId}` : undefined;
}

async function readUser(page: Page, login: string): Promise<UserForm> {
  const combinedId = await listedUser(page, login);
  expect(combinedId, `${login} should be listed`).toBeTruthy();
  return (
    await page.request.get(
      `${USER_ENDPOINT}?ID=${combinedId}&startingRecNo=1&roleFilter=`,
    )
  ).json();
}

/** Creates a user through the endpoint the screen saves with. */
async function seedUser(page: Page, user: SeededUser) {
  seededUsers.get(page)!.add(user.login);
  const form = await blankForm(page);
  const created = await page.request.post(USER_ENDPOINT, {
    headers: { "X-CSRF-Token": await csrfToken(page) },
    data: {
      ...form,
      globalRoles: undefined,
      labUnitRoles: undefined,
      testSections: undefined,
      loginUserId: "",
      systemUserId: "",
      userLoginName: user.login,
      userFirstName: user.first,
      userLastName: user.last,
      userPassword: PASSWORD,
      confirmPassword: PASSWORD,
      accountActive: user.active === false ? "N" : "Y",
      accountDisabled: "N",
      accountLocked: "N",
      allowCopyUserRoles: "N",
      selectedRoles: user.roles ?? [],
      selectedTestSectionLabUnits: user.labUnits ?? {},
    },
  });
  expect(created.status(), await created.text()).toBe(200);
  expect(await created.json()).toEqual({
    forward: "redirect:/UnifiedSystemUser",
  });
}

async function openUserList(page: Page) {
  await page.goto("/MasterListsPage/userManagement", {
    waitUntil: "domcontentloaded",
  });
  await expect(
    page.getByRole("heading", { name: "User Management", exact: true }),
  ).toBeVisible({ timeout: NAV_TIMEOUT });
  await expect(page.locator(".cds--data-table")).toBeVisible();
}

const rowsWith = (page: Page, text: string) =>
  page.locator(".cds--data-table tbody tr").filter({ hasText: text });

async function search(page: Page, term: string) {
  const searched = page.waitForResponse(
    (response) =>
      response.url().includes("/rest/SearchUnifiedSystemUserMenu") &&
      new URL(response.url()).searchParams.get("searchString") === term,
  );
  await page.locator("#user-name-search-bar").fill(term);
  await searched;
}

async function loginCell(page: Page, row: Locator) {
  return cellUnder(
    page.locator(".cds--data-table"),
    row,
    "System User Login Name",
  );
}

/** Signs in on a fresh browser context; returns the page left behind. */
async function signIn(browser: Browser, baseURL: string, login: string) {
  const context = await browser.newContext({
    ignoreHTTPSErrors: true,
    baseURL,
    storageState: { cookies: [], origins: [] },
  });
  const page = await context.newPage();
  await page.goto("/login", { waitUntil: "domcontentloaded" });
  await page.locator("#loginName").fill(login);
  await page.locator("#password").fill(PASSWORD);
  await page.locator('[data-cy="loginButton"]').click();
  return { context, page };
}

test.describe("User Management", () => {
  const consoleErrors: string[] = [];

  test.beforeEach(async ({ page }) => {
    seededUsers.set(page, new Set());
    consoleErrors.length = 0;
    page.on("pageerror", (error) => consoleErrors.push(error.message));
    page.on("console", (message) => {
      if (message.type() === "error") consoleErrors.push(message.text());
    });
  });

  test.afterEach(async ({ page }) => {
    const cleanupErrors: string[] = [];
    for (const login of seededUsers.get(page) ?? []) {
      try {
        if (!(await listedUser(page, login))) continue;
        const form = await readUser(page, login);
        if (form.accountActive === "N") continue;
        await page.request.post(USER_ENDPOINT, {
          headers: { "X-CSRF-Token": await csrfToken(page) },
          data: { ...form, accountActive: "N", accountDisabled: "Y" },
        });
        expect
          .soft((await readUser(page, login)).accountActive, login)
          .toBe("N");
      } catch (error) {
        cleanupErrors.push(`${login}: ${String(error)}`);
      }
    }
    expect
      .soft(cleanupErrors, "Every seeded account must be deactivated")
      .toEqual([]);
    expect(consoleErrors).toEqual([]);
  });

  test("Add User saves the account state and the roles chosen on the screen", async ({
    page,
  }) => {
    const form = await blankForm(page);
    const globalAdmin = form.globalRoles.find(
      (role) => role.roleName === "Global Administrator",
    );
    expect(globalAdmin).toBeTruthy();
    const suffix = letters(6);
    const login = `pw_add_${suffix}`;

    await openUserList(page);
    await page.locator('[data-cy="add-button"]').click();
    await expect(
      page.getByRole("heading", { name: "Add User", exact: true }),
    ).toBeVisible();

    await page.locator("#login-name").fill(login);
    await page.locator("#login-password").fill(PASSWORD);
    await page.locator("#login-repeat-password").fill(PASSWORD);
    await page.locator("#first-name").fill("Playwright");
    await page.locator("#last-name").fill(`Added ${suffix}`);
    await page.locator("#login-timeout").fill("450");

    // Carbon hides radio and checkbox inputs; the label takes the click.
    // radio-1: locked Y, radio-3: disabled Y, radio-6: active N.
    for (const radio of ["radio-1", "radio-3", "radio-6"]) {
      await page.locator(`label[for="${radio}"]`).click();
      await expect(page.locator(`#${radio}`)).toBeChecked();
    }
    await page.locator(`label[for="${globalAdmin!.elementID}"]`).click();
    // Role element ids are numeric, which a CSS id selector cannot start with.
    await expect(
      page.locator(`[id="${globalAdmin!.elementID}"]`),
    ).toBeChecked();

    await page.getByRole("button", { name: "Add New Permission" }).click();
    await page.locator('select[id^="select-"]').selectOption(ALL_LAB_UNITS);
    await page.locator(`label[for="all-permissions-${ALL_LAB_UNITS}"]`).click();
    await expect(
      page.locator(`#all-permissions-${ALL_LAB_UNITS}`),
    ).toBeChecked();

    const saved = page.waitForResponse(
      (response) =>
        response.url().includes("/rest/UnifiedSystemUser") &&
        response.request().method() === "POST",
    );
    await page.locator('[data-cy="saveButton"]').click();
    await saved;
    await expect(
      page.getByText("User information added successfully."),
    ).toBeVisible();

    const stored = await readUser(page, login);
    expect(stored.accountLocked).toBe("Y");
    expect(stored.accountDisabled).toBe("Y");
    expect(stored.accountActive).toBe("N");
    expect(stored.selectedRoles).toContain(globalAdmin!.roleId);
    expect(Object.keys(stored.selectedTestSectionLabUnits)).toEqual([
      ALL_LAB_UNITS,
    ]);
    expect(
      [...stored.selectedTestSectionLabUnits[ALL_LAB_UNITS]].sort(),
    ).toEqual(form.labUnitRoles.map((role) => role.roleId).sort());
  });

  test("Exit leaves Add User without saving anything", async ({ page }) => {
    const login = `pw_exit_${letters(6)}`;
    const rows = page.locator('select[id^="select-"]');

    await openUserList(page);
    await page.locator('[data-cy="add-button"]').click();
    await expect(
      page.getByRole("heading", { name: "Add User", exact: true }),
    ).toBeVisible();
    await page.locator("#login-name").fill(login);
    await page.locator("#login-password").fill(PASSWORD);
    await page.locator("#login-repeat-password").fill(PASSWORD);

    await page.getByRole("button", { name: "Add New Permission" }).click();
    await expect(rows).toHaveCount(1);
    await page.getByRole("button", { name: "Remove Permission" }).click();
    await expect(rows).toHaveCount(0);

    await page.locator('[data-cy="exitButton"]').click();
    await expect(page).toHaveURL(/\/MasterListsPage\/userManagement$/);
    await expect(
      page.getByRole("heading", { name: "User Management", exact: true }),
    ).toBeVisible();
    expect(await listedUser(page, login)).toBeUndefined();
  });

  test("search finds a user by login, first name and last name", async ({
    page,
  }) => {
    const suffix = letters(8);
    const user = {
      login: `pw_find_${suffix}`,
      first: `First${suffix}`,
      last: `Last${suffix}`,
    };
    await seedUser(page, user);

    await openUserList(page);
    for (const term of [user.login, user.first, user.last]) {
      await search(page, term);
      const rows = rowsWith(page, term);
      await expect(rows).toHaveCount(1);
      await expect(await loginCell(page, rows)).toHaveText(user.login);
    }
  });

  test("the lab unit filter lists the users holding a role in that unit", async ({
    page,
  }) => {
    const form = await blankForm(page);
    const [held, other] = form.testSections;
    const suffix = letters(8);
    const login = `pw_unit_${suffix}`;
    await seedUser(page, {
      login,
      first: "Playwright",
      last: `Unit ${suffix}`,
      labUnits: { [held.id]: [form.labUnitRoles[0].roleId] },
    });

    await openUserList(page);
    await search(page, login);
    await expect(rowsWith(page, login)).toHaveCount(1);

    const filtered = (unit: string) =>
      page.waitForResponse(
        (response) =>
          response.url().includes("/rest/SearchUnifiedSystemUserMenu") &&
          new URL(response.url()).searchParams.get("roleFilter") === unit,
      );
    const filter = page.getByLabel("By Lab Unit Roles");

    let answered = filtered(other.id);
    await filter.selectOption(other.id);
    await answered;
    await expect(rowsWith(page, login)).toHaveCount(0);

    answered = filtered(held.id);
    await filter.selectOption(held.id);
    await answered;
    await expect(rowsWith(page, login)).toHaveCount(1);
  });

  test("Only Active hides inactive users", async ({ page }) => {
    const suffix = letters(8);
    const active = `pw_on_${suffix}`;
    const inactive = `pw_off_${suffix}`;
    await seedUser(page, { login: active, first: "Active", last: suffix });
    await seedUser(page, {
      login: inactive,
      first: "Inactive",
      last: suffix,
      active: false,
    });

    await openUserList(page);
    await search(page, suffix);
    await expect(rowsWith(page, suffix)).toHaveCount(2);

    const narrowed = page.waitForResponse(
      (response) =>
        response.url().includes("/rest/SearchUnifiedSystemUserMenu") &&
        (new URL(response.url()).searchParams.get("filter") || "")
          .split(",")
          .includes("isActive"),
    );
    await page.locator('label[for="only-active"]').click();
    await narrowed;
    await expect(rowsWith(page, suffix)).toHaveCount(1);
    await expect(rowsWith(page, active)).toHaveCount(1);
  });

  test("Only Administrator lists the built-in admin login, not Global Administrator holders", async ({
    page,
  }) => {
    const form = await blankForm(page);
    const globalAdmin = form.globalRoles.find(
      (role) => role.roleName === "Global Administrator",
    );
    const suffix = letters(8);
    const login = `pw_ga_${suffix}`;
    await seedUser(page, {
      login,
      first: "Playwright",
      last: `Global ${suffix}`,
      roles: [globalAdmin!.roleId],
    });

    await openUserList(page);
    await search(page, login);
    await expect(rowsWith(page, login)).toHaveCount(1);

    const narrowed = page.waitForResponse(
      (response) =>
        response.url().includes("/rest/SearchUnifiedSystemUserMenu") &&
        (new URL(response.url()).searchParams.get("filter") || "")
          .split(",")
          .includes("isAdmin"),
    );
    await page.locator('label[for="only-administrator"]').click();
    await narrowed;
    await expect(page.locator("#only-administrator")).toBeChecked();
    await expect(rowsWith(page, login)).toHaveCount(0);

    await search(page, "admin");
    const admin = page.locator(".cds--data-table tbody tr").filter({
      has: page.getByRole("cell", { name: "admin", exact: true }),
    });
    await expect(admin).toHaveCount(1);
  });

  test("Modify copies another user's permissions with Apply", async ({
    page,
  }) => {
    const form = await blankForm(page);
    const suffix = letters(8);
    const source = {
      login: `pw_src_${suffix}`,
      first: "Source",
      last: `Copied${suffix}`,
      labUnits: {
        [form.testSections[0].id]: [form.labUnitRoles[0].roleId],
        [form.testSections[1].id]: [form.labUnitRoles[1].roleId],
      },
    };
    const target = {
      login: `pw_dst_${suffix}`,
      first: "Target",
      last: `Receiving${suffix}`,
    };
    await seedUser(page, source);
    await seedUser(page, target);

    await openUserList(page);
    await search(page, target.login);
    await rowsWith(page, target.login).click();
    await page.locator('[data-cy="modify-button"]').click();
    await expect(page).toHaveURL(/\/MasterListsPage\/userEdit\?ID=/);
    await expect(page.locator("#login-name")).toHaveValue(target.login);

    await page.locator("#copy-permissions").fill(source.last);
    await page
      .locator('[data-cy="auto-suggestion"]')
      .filter({ hasText: source.last })
      .click();
    const applied = page.waitForResponse(
      (response) =>
        response.url().includes("/rest/UnifiedSystemUser") &&
        response.request().method() === "POST",
    );
    await page.getByRole("button", { name: "Apply", exact: true }).click();
    await applied;
    await expect(
      page.getByText("User information updated successfully."),
    ).toBeVisible();

    expect(
      (await readUser(page, target.login)).selectedTestSectionLabUnits,
    ).toEqual(source.labUnits);
  });

  test("Deactivate in the list stops that user signing in", async ({
    page,
    browser,
    baseURL,
  }) => {
    const form = await blankForm(page);
    const results = form.labUnitRoles.find(
      (role) => role.roleName === "Results",
    );
    const suffix = letters(8);
    const login = `pw_deact_${suffix}`;
    await seedUser(page, {
      login,
      first: "Playwright",
      last: `Deactivated ${suffix}`,
      labUnits: { [ALL_LAB_UNITS]: [results!.roleId] },
    });

    // The account works before it is deactivated.
    const before = await signIn(browser, baseURL!, login);
    await expect(before.page).toHaveURL(/\/$/, { timeout: NAV_TIMEOUT });
    await expect(
      before.page.getByRole("navigation", { name: "Side navigation" }),
    ).toBeVisible({ timeout: NAV_TIMEOUT });
    await before.context.close();

    await openUserList(page);
    await search(page, login);
    await rowsWith(page, login).click();
    const deactivated = page.waitForResponse(
      (response) =>
        response.url().includes("/rest/DeleteUnifiedSystemUser") &&
        response.request().method() === "POST",
    );
    await page.locator('[data-cy="deactivate-button"]').click();
    await deactivated;
    await expect(
      page.getByText("User Deactivated Successfully."),
    ).toBeVisible();
    expect((await readUser(page, login)).accountActive).toBe("N");

    const after = await signIn(browser, baseURL!, login);
    try {
      await expect(
        after.page.getByText("Username or Password are incorrect"),
      ).toBeVisible({ timeout: NAV_TIMEOUT });
      await expect(after.page).toHaveURL(/\/login/);
    } finally {
      await after.context.close();
    }
  });
});
