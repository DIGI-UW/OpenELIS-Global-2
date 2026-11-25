import { test, expect, type Page } from "../../../helpers/test-base";
import { NAV_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";

/**
 * OGC-1545 — two people with the same name could not both have accounts: the
 * second Add User was refused silently, with the same 200 as a success. The
 * login is what identifies a user, so a same-name user is now created, and a
 * save that writes nothing answers 400 and the page says why.
 *
 * Run with:
 *   cd frontend && npm run pw:test:core-foundational -- ogc-1545-user-save-refusals
 */

const API = "/api/OpenELIS-Global";
const USER_ENDPOINT = `${API}/rest/UnifiedSystemUser`;
const USER_SEARCH = `${API}/rest/SearchUnifiedSystemUserMenu`;
const PASSWORD = "Pw!Passw0rd";

// The seeded username charset has no digits, so the unique suffix is letters.
const letters = (length: number) =>
  Array.from({ length }, () =>
    String.fromCharCode(97 + Math.floor(Math.random() * 26)),
  ).join("");

async function createUser(
  page: Page,
  login: string,
  firstName: string,
  lastName: string,
) {
  const template = await (
    await page.request.get(`${USER_ENDPOINT}?ID=0&startingRecNo=1&roleFilter=`)
  ).json();
  const csrf = (await (await page.request.get(`${API}/session`)).json()).csrf;
  const created = await page.request.post(USER_ENDPOINT, {
    headers: { "X-CSRF-Token": csrf },
    data: {
      ...template,
      globalRoles: undefined,
      labUnitRoles: undefined,
      testSections: undefined,
      loginUserId: "",
      systemUserId: "",
      userLoginName: login,
      userFirstName: firstName,
      userLastName: lastName,
      userPassword: PASSWORD,
      confirmPassword: PASSWORD,
      allowCopyUserRoles: "N",
      selectedRoles: [],
    },
  });
  expect(created.status(), await created.text()).toBe(200);
}

async function listed(page: Page, login: string) {
  const menu = await (
    await page.request.get(
      `${USER_SEARCH}?search=Y&startingRecNo=1&searchString=${login}&filter=`,
    )
  ).json();
  return (menu.menuList as { loginName: string }[]).some(
    (user) => user.loginName === login,
  );
}

async function addUserThroughTheScreen(
  page: Page,
  login: string,
  firstName: string,
  lastName: string,
) {
  await page.goto(
    "/MasterListsPage/userEdit?ID=0&startingRecNo=1&roleFilter=",
    { waitUntil: "domcontentloaded", timeout: NAV_TIMEOUT },
  );
  await page.locator("#login-name").fill(login);
  await page.locator("#login-password").fill(PASSWORD);
  await page.locator("#login-repeat-password").fill(PASSWORD);
  await page.locator("#first-name").fill(firstName);
  await page.locator("#last-name").fill(lastName);
  const save = page.locator('[data-cy="saveButton"]');
  await expect(save).toBeEnabled({ timeout: UI_TIMEOUT });
  await save.click();
}

test.describe("OGC-1545 user saves", () => {
  test("a second person with the same name gets an account", async ({
    page,
  }) => {
    const suffix = letters(6);
    const lastName = `Twin ${suffix}`;
    const first = `pw_twin_a_${suffix}`;
    const second = `pw_twin_b_${suffix}`;
    await createUser(page, first, "Playwright", lastName);

    await addUserThroughTheScreen(page, second, "Playwright", lastName);

    await expect(
      page.getByText("User information added successfully."),
    ).toBeVisible({ timeout: UI_TIMEOUT });
    expect(await listed(page, first)).toBe(true);
    expect(await listed(page, second)).toBe(true);
  });

  test("a login already in use is refused and the page says so", async ({
    page,
  }) => {
    const suffix = letters(6);
    const login = `pw_taken_${suffix}`;
    await createUser(page, login, "Playwright", `Taken ${suffix}`);

    const saved = page.waitForResponse(
      (response) =>
        response.url().includes("/rest/UnifiedSystemUser") &&
        response.request().method() === "POST",
    );
    await addUserThroughTheScreen(page, login, "Other", `Person ${suffix}`);
    await saved;

    await expect(
      page.getByText(`Login name, ${login}, is already in use.`),
    ).toBeVisible({ timeout: UI_TIMEOUT });
    await expect(
      page.getByText("User information added successfully."),
    ).toHaveCount(0);
  });
});
