import { test, expect, type Page } from "../../../helpers/test-base";
import { LONG_TIMEOUT, NAV_TIMEOUT } from "../../../helpers/timeouts";
import {
  API_PREFIX,
  apiPost,
  cellUnder,
  deactivateByName,
  letterSuffix,
  searchListFor,
} from "../../../helpers/admin-ui";

const LIST_PAGE = "/MasterListsPage/organizationManagement";
const ADD_PAGE = "/MasterListsPage/organizationEdit?ID=0";

type ActivityType = "referring clinic" | "referralLab";

const escapeRegExp = (text: string) =>
  text.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");

const activityRow = (page: Page, type: ActivityType) =>
  page
    .getByRole("row")
    .filter({ has: page.getByRole("cell", { name: type, exact: true }) });

/** Creates an organisation through the endpoint the form saves to. */
async function createOrganization(page: Page, name: string) {
  const form = await (
    await page.request.get(
      `${API_PREFIX}/rest/Organization?ID=0&startingRecNo=1`,
    )
  ).json();
  const type = (form.orgTypes as { id: string; name: string }[]).find(
    (item) => item.name === "referring clinic",
  );
  if (!type) throw new Error("no referring clinic organisation type");
  await apiPost(page, "/rest/Organization?ID=0&startingRecNo=1", {
    organizationName: name,
    shortName: "",
    isActive: "Y",
    mlsSentinelLabFlag: "N",
    selectedTypes: [type.id],
  });
}

async function addOrganization(
  page: Page,
  org: { name: string; prefix: string; parent: string; type: ActivityType },
) {
  const parents = page.waitForResponse(/\/rest\/displayList\/ACTIVE_ORG_LIST/);
  await page.goto(ADD_PAGE, { waitUntil: "domcontentloaded" });
  await parents;
  const typeRow = activityRow(page, org.type);
  await expect(typeRow).toBeVisible({ timeout: NAV_TIMEOUT });

  await page
    .getByRole("textbox", { name: "Org Name", exact: true })
    .fill(org.name);
  await page
    .getByRole("textbox", { name: "Org prefix", exact: true })
    .fill(org.prefix);
  await page.getByRole("textbox", { name: "Is Active", exact: true }).fill("Y");

  const parent = page.locator("#parentOrgName");
  await parent.fill(org.parent);
  await page
    .locator('[data-cy="auto-suggestion"]')
    .filter({ hasText: new RegExp(`^${escapeRegExp(org.parent)}$`) })
    .click();
  await expect(parent).toHaveValue(org.parent);

  await typeRow.locator("label").click();
  await expect(typeRow.getByRole("checkbox")).toBeChecked();

  await page.getByRole("button", { name: "Save", exact: true }).click();
  await expect(page).toHaveURL(new RegExp(`${LIST_PAGE}$`), {
    timeout: LONG_TIMEOUT,
  });
}

const findOrganization = (page: Page, name: string) =>
  searchListFor(page, page.locator("#org-name-search-bar"), name);

/** Opens a listed organisation in the edit form through Modify. */
async function reopen(page: Page, name: string) {
  const row = await findOrganization(page, name);
  await row.locator("label").click();
  await page.getByRole("button", { name: "Modify", exact: true }).click();
  await expect(page).toHaveURL(/organizationEdit\?ID=\d+/, {
    timeout: NAV_TIMEOUT,
  });
  await expect(
    page.getByRole("textbox", { name: "Org Name", exact: true }),
  ).toHaveValue(name, { timeout: NAV_TIMEOUT });
}

test.describe("Organisation management", () => {
  let created: string[] = [];

  test.beforeEach(() => {
    test.setTimeout(180_000);
    created = [];
  });

  test.afterEach(async ({ page }) => {
    await deactivateByName(page, "organization", created);
  });

  for (const [type, other] of [
    ["referring clinic", "referralLab"],
    ["referralLab", "referring clinic"],
  ] as const) {
    test(`a new ${type} is listed with its prefix and parent, and keeps its type`, async ({
      page,
    }) => {
      const suffix = letterSuffix();
      const parent = `PwParent${suffix}`;
      const name = `PwOrg${suffix}`;
      const prefix = `P${suffix.slice(-5)}`;
      created.push(name, parent);

      await test.step("seed a parent organisation", () =>
        createOrganization(page, parent));

      await test.step("add the organisation through the form", () =>
        addOrganization(page, { name, prefix, parent, type }));

      await test.step("the list shows it under a search for its name", async () => {
        const table = page.locator("table");
        const row = await findOrganization(page, name);
        await expect(await cellUnder(table, row, "Org Name")).toHaveText(name);
        await expect(await cellUnder(table, row, "Org prefix")).toHaveText(
          prefix,
        );
        await expect(await cellUnder(table, row, "Parent Org")).toHaveText(
          parent,
        );
        await expect(await cellUnder(table, row, "Is Active")).toHaveText("Y");
      });

      await test.step("the saved type is the only one ticked on reopen", async () => {
        await reopen(page, name);
        await expect(
          activityRow(page, type).getByRole("checkbox"),
        ).toBeChecked();
        await expect(
          activityRow(page, other).getByRole("checkbox"),
        ).not.toBeChecked();
      });
    });
  }
});
