import { test, expect, type Page } from "../../../helpers/test-base";

const USER_ENDPOINT = "/api/OpenELIS-Global/rest/UnifiedSystemUser";
const USER_SEARCH = "/api/OpenELIS-Global/rest/SearchUnifiedSystemUserMenu";
const SESSION = "/api/OpenELIS-Global/session";
const ALL_LAB_UNITS = "AllLabUnits";
const ALL_LAB_UNITS_ERROR = "labUnitRoles.allLabUnitsExclusive";

// The seeded username charset has no digits, so the unique suffix is letters.
const letters = (length: number) =>
  Array.from({ length }, () =>
    String.fromCharCode(97 + Math.floor(Math.random() * 26)),
  ).join("");

type LabUnitRole = { elementID: string; roleId: string; roleName: string };
type TestSection = { id: string; value: string };

async function csrfToken(page: Page) {
  const session = await page.request.get(SESSION);
  return (await session.json()).csrf as string;
}

/**
 * Creates a user through the same endpoint the screen uses, holding a single
 * scoped grant: the first lab-unit role on the first test section.
 */
async function createScopedUser(page: Page) {
  const template = await (
    await page.request.get(`${USER_ENDPOINT}?ID=0&startingRecNo=1&roleFilter=`)
  ).json();
  const sections = template.testSections as TestSection[];
  const roles = template.labUnitRoles as LabUnitRole[];
  const suffix = letters(6);
  const loginName = `pw_labunit_${suffix}`;
  const password = "Pw!Passw0rd";

  const created = await page.request.post(USER_ENDPOINT, {
    headers: { "X-CSRF-Token": await csrfToken(page) },
    data: {
      ...template,
      globalRoles: undefined,
      labUnitRoles: undefined,
      testSections: undefined,
      loginUserId: "",
      systemUserId: "",
      userLoginName: loginName,
      // Names are unique per run: a repeated first+last name is refused as a
      // duplicate system user, silently, with the same 200 as a success.
      userFirstName: "Playwright",
      userLastName: `LabUnit ${suffix}`,
      userPassword: password,
      confirmPassword: password,
      allowCopyUserRoles: "N",
      selectedRoles: [],
      selectedTestSectionLabUnits: { [sections[0].id]: [roles[0].roleId] },
    },
  });
  expect(created.status()).toBe(200);
  // A validation failure also answers 200; only the redirect means "written".
  const outcome = await created.json();
  expect(outcome.forward, JSON.stringify(outcome)).toBe(
    "redirect:/UnifiedSystemUser",
  );

  const menu = await (
    await page.request.get(
      `${USER_SEARCH}?search=Y&startingRecNo=1&searchString=${loginName}&filter=`,
    )
  ).json();
  const row = (
    menu.menuList as {
      systemUserId: string;
      loginUserId: string;
      loginName: string;
    }[]
  ).find((user) => user.loginName === loginName);
  expect(row, `user ${loginName} should be listed after creation`).toBeTruthy();

  return {
    // The edit screen addresses a user by "<systemUserId>-<loginUserId>".
    combinedId: `${row!.systemUserId}-${row!.loginUserId}`,
    loginName,
    section: sections[0],
    otherSection: sections[1],
    role: roles[0],
  };
}

async function readGrants(page: Page, combinedId: string) {
  const form = await (
    await page.request.get(
      `${USER_ENDPOINT}?ID=${combinedId}&startingRecNo=1&roleFilter=`,
    )
  ).json();
  return form as {
    selectedTestSectionLabUnits: Record<string, string[]>;
    [key: string]: unknown;
  };
}

test.describe("Modify User: lab unit roles", () => {
  test("shows the stored lab unit, refuses All Lab Units next to it, and reports an update", async ({
    page,
  }) => {
    const user = await createScopedUser(page);
    const rowSelects = page.locator('select[id^="select-"]');
    const addPermission = page.getByRole("button", {
      name: "Add New Permission",
    });

    await page.goto(
      `/MasterListsPage/userEdit?ID=${user.combinedId}&startingRecNo=1&roleFilter=`,
      { waitUntil: "domcontentloaded" },
    );

    // A: the row's dropdown used to strand on option 0 ("All Lab Units").
    await expect(page.locator(`#select-${user.section.id}`)).toHaveValue(
      user.section.id,
    );
    await expect(rowSelects).toHaveCount(1);

    // C: an underscore in the login name is valid and must not render red.
    await expect(page.locator("#login-name")).toHaveValue(user.loginName);
    await expect(page.locator("#login-name")).not.toHaveAttribute(
      "aria-invalid",
      "true",
    );

    // B: a new row is always a real lab unit, never All Lab Units.
    await addPermission.click();
    await expect(rowSelects).toHaveCount(2);
    await expect(page.locator(`#select-${ALL_LAB_UNITS}`)).toHaveCount(0);

    // B: switching a row to All Lab Units asks first, naming what goes.
    const dialog = page.getByRole("dialog", {
      name: "Replace lab unit permissions?",
    });
    await rowSelects.nth(1).selectOption(ALL_LAB_UNITS);
    await expect(dialog).toBeVisible();
    await expect(
      dialog.getByText(`${user.section.value}: ${user.role.roleName}`),
    ).toBeVisible();

    await dialog.getByRole("button", { name: "Cancel" }).click();
    await expect(dialog).toBeHidden();
    await expect(page.locator(`#select-${user.section.id}`)).toHaveValue(
      user.section.id,
    );
    await expect(rowSelects).toHaveCount(2);

    await rowSelects.nth(1).selectOption(ALL_LAB_UNITS);
    await expect(dialog).toBeVisible();
    await dialog.getByRole("button", { name: "Replace permissions" }).click();
    await expect(dialog).toBeHidden();
    await expect(rowSelects).toHaveCount(1);
    await expect(page.locator(`#select-${ALL_LAB_UNITS}`)).toHaveValue(
      ALL_LAB_UNITS,
    );
    await expect(addPermission).toBeDisabled();

    // Carbon hides the checkbox input; the label is the clickable surface.
    await page
      .locator(`label[for="${user.role.elementID}-${ALL_LAB_UNITS}"]`)
      .click();

    // D: saving an existing user reports an update, spelled correctly.
    const saved = page.waitForResponse(
      (response) =>
        response.url().includes("/rest/UnifiedSystemUser") &&
        response.request().method() === "POST",
    );
    await page.locator('[data-cy="saveButton"]').click();
    await saved;
    await expect(
      page.getByText("User information updated successfully."),
    ).toBeVisible();

    const stored = await readGrants(page, user.combinedId);
    expect(stored.selectedTestSectionLabUnits).toEqual({
      [ALL_LAB_UNITS]: [user.role.roleId],
    });

    // B (API): the combination is refused outright and nothing is written.
    const refused = await page.request.post(USER_ENDPOINT, {
      headers: { "X-CSRF-Token": await csrfToken(page) },
      data: {
        ...stored,
        globalRoles: undefined,
        labUnitRoles: undefined,
        testSections: undefined,
        selectedTestSectionLabUnits: {
          [ALL_LAB_UNITS]: [user.role.roleId],
          [user.otherSection.id]: [user.role.roleId],
        },
      },
    });
    expect(refused.status()).toBe(400);
    expect((await refused.json()).error).toBe(ALL_LAB_UNITS_ERROR);
    expect(
      (await readGrants(page, user.combinedId)).selectedTestSectionLabUnits,
    ).toEqual({ [ALL_LAB_UNITS]: [user.role.roleId] });

    // Leave the user list as found: deactivate the probe account.
    const deactivated = await page.request.post(USER_ENDPOINT, {
      headers: { "X-CSRF-Token": await csrfToken(page) },
      data: {
        ...stored,
        globalRoles: undefined,
        labUnitRoles: undefined,
        testSections: undefined,
        accountActive: "N",
        accountDisabled: "Y",
      },
    });
    expect(deactivated.status()).toBe(200);
  });
});
