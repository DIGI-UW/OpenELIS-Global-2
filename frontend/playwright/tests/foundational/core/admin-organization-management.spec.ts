import { test as base, expect } from "../../../helpers/test-base";
import { NAV_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";
import { apiPost, letterSuffix } from "../../../helpers/admin-ui";

const LOCATIONS = "/MasterListsPage/locations";

const test = base.extend<{ organizationIds: string[] }>({
  organizationIds: async ({ page }, use) => {
    const ids: string[] = [];
    try {
      await use(ids);
    } finally {
      if (ids.length)
        await apiPost(page, "/rest/locations/organizations/active", {
          ids,
          active: false,
          includeChildren: false,
        });
    }
  },
});

// Verify codes and activity types survive save and reopen on the current screen.
test.describe("Organisation management", () => {
  for (const [type, other] of [
    ["referring clinic", "referralLab"],
    ["referralLab", "referring clinic"],
  ] as const) {
    test(`a new ${type} is listed with its code and keeps its type on reopen`, async ({
      page,
      organizationIds,
    }) => {
      const suffix = letterSuffix();
      const name = `PwOrg${suffix}`;
      const code = `P${suffix}`;

      await page.goto(LOCATIONS, { waitUntil: "domcontentloaded" });
      await page.getByTestId("locations-add").click();
      const form = page.getByTestId("locations-form-new");
      await expect(form).toBeVisible({ timeout: NAV_TIMEOUT });
      await form.locator("#name-new").fill(name);
      await form.getByLabel("Code Value").fill(code);
      await form.locator("#types-new").click();
      await page.getByRole("option", { name: type, exact: true }).click();
      await page.keyboard.press("Escape");
      if (type === "referralLab") {
        await form.locator("#ref-status-new").selectOption("Approved");
      }
      const saved = page.waitForResponse(
        (response) =>
          response.url().endsWith("/rest/locations/organizations") &&
          response.request().method() === "POST",
      );
      await form.getByTestId("locations-save").click();
      const response = await saved;
      expect(response.ok(), await response.text()).toBeTruthy();
      const id = (await response.json()).detail.row.id;
      organizationIds.push(id);
      await expect(page.getByText(`${name} added.`)).toBeVisible({
        timeout: UI_TIMEOUT,
      });

      await page
        .getByPlaceholder("Search by name, code or any identifier")
        .fill(code);
      const row = page.getByTestId(`locations-row-${id}`);
      await expect(row).toContainText(name, { timeout: UI_TIMEOUT });
      await expect(row).toContainText(code);
      await expect(row).toContainText(type);
      await expect(row.locator(`#active-${id}`)).toHaveAttribute(
        "aria-checked",
        "true",
      );

      await page.goto(LOCATIONS, { waitUntil: "domcontentloaded" });
      await page
        .getByPlaceholder("Search by name, code or any identifier")
        .fill(code);
      await expect(page).toHaveURL((url) => url.searchParams.get("q") === code);
      await expect(row).toContainText(name, { timeout: UI_TIMEOUT });
      await page.getByTestId(`locations-edit-${id}`).click();
      const edit = page.getByTestId(`locations-form-${id}`);
      await expect(edit.locator(`#name-${id}`)).toHaveValue(name, {
        timeout: UI_TIMEOUT,
      });
      await expect(edit.getByLabel("Code Value")).toHaveValue(code);
      await edit.locator(`#types-${id}`).click();
      await expect(
        page.getByRole("option", { name: type, exact: true }),
      ).toHaveAttribute("aria-selected", "true");
      await expect(
        page.getByRole("option", { name: other, exact: true }),
      ).toHaveAttribute("aria-selected", "false");
    });
  }
});
