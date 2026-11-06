import {
  test,
  expect,
  type Locator,
  type Page,
} from "../../../helpers/test-base";
import { apiPost, API_PREFIX } from "../../../helpers/admin-ui";
import { NAV_TIMEOUT } from "../../../helpers/timeouts";

/**
 * Admin, Menu Configuration, Non-Conform Menu Configuration: switching the
 * Non-Conformity menu off removes it from the application navigation, and
 * switching it back on brings the group and its entries back.
 */

const SETTINGS = `${API_PREFIX}/rest/admin/menu/menu_nonconformity`;
const CHILDREN = [
  "menu_non_conforming_report",
  "menu_non_conforming_view",
  "menu_non_conforming_corrective_actions",
];

type MenuItem = { menu: { isActive: boolean }; childMenus?: MenuItem[] };

/** The stored settings with the menu and every entry under it active. */
function activated(item: MenuItem): MenuItem {
  return {
    ...item,
    menu: { ...item.menu, isActive: true },
    childMenus: item.childMenus?.map(activated),
  };
}

async function openSettings(page: Page) {
  await page.goto("/MasterListsPage/nonConformityMenuManagement", {
    waitUntil: "domcontentloaded",
  });
  await expect(
    page.getByRole("heading", { name: "Non-Conformity Menu Management" }),
  ).toBeVisible({ timeout: NAV_TIMEOUT });
  // The checkbox renders once the stored settings have been read.
  await expect(page.locator("#menu_nonconformity_checkbox")).toHaveCount(1, {
    timeout: NAV_TIMEOUT,
  });
}

async function submit(page: Page) {
  const saved = page.waitForResponse(
    (response) =>
      response.request().method() === "POST" &&
      response.url().includes("/rest/menu/menu_nonconformity"),
  );
  await page.getByRole("button", { name: "Submit", exact: true }).click();
  await saved;
  await expect(page.getByText("Successfully Added/Edited")).toBeVisible();
}

async function expand(navigation: Locator, id: string) {
  const toggle = navigation.locator(`button[id="${id}"]`);
  await expect(toggle).toBeVisible();
  if ((await toggle.getAttribute("aria-expanded")) !== "true") {
    await toggle.click();
  }
  await expect(toggle).toHaveAttribute("aria-expanded", "true");
}

/**
 * Loads the application (it reads its navigation on load) and opens Quality
 * Assurance, Quality Management, where the Non-Conformity group lives. A
 * sibling entry being visible shows the place was really opened, so the
 * group's absence there means something.
 */
async function openQualityManagement(page: Page) {
  await page.goto("/", { waitUntil: "domcontentloaded" });
  const navigation = page.getByRole("navigation", { name: "Side navigation" });
  await expect(navigation).toBeVisible({ timeout: NAV_TIMEOUT });
  await expand(navigation, "menu_qa");
  await expand(navigation, "menu_qa_qms");
  await expect(
    navigation.locator('a[id="menu_qa_qms_capa_register_nav"]'),
  ).toBeVisible();
  return navigation;
}

test("the Non-Conformity menu can be switched off and back on with its entries", async ({
  page,
}, testInfo) => {
  // Four full application loads, each behind the session check.
  testInfo.setTimeout(90_000);
  const original = await (await page.request.get(SETTINGS)).json();
  const active = page.locator("#menu_nonconformity_checkbox");
  // Carbon hides the checkbox input; the label is the clickable surface.
  const activeLabel = page.locator('label[for="menu_nonconformity_checkbox"]');

  try {
    await apiPost(page, "/rest/menu/menu_nonconformity", activated(original));

    await openSettings(page);
    await expect(active).toBeChecked();
    await activeLabel.click();
    await expect(active).not.toBeChecked();
    await submit(page);

    let navigation = await openQualityManagement(page);
    await expect(navigation.locator('[id="menu_nonconformity"]')).toHaveCount(
      0,
    );

    await openSettings(page);
    await expect(active).not.toBeChecked();
    // Carbon lays the toggle's label over its switch; the label takes the click.
    await page.locator('label[for="toggleShowChildren"]').click();
    await expect(
      page.getByRole("switch", { name: "Show Child Elements" }),
    ).toBeChecked();
    for (const child of CHILDREN) {
      await expect(page.locator(`#${child}_checkbox`)).not.toBeChecked();
    }

    // Activating the parent activates every entry under it.
    await activeLabel.click();
    await expect(active).toBeChecked();
    for (const child of CHILDREN) {
      await expect(page.locator(`#${child}_checkbox`)).toBeChecked();
    }
    await submit(page);

    navigation = await openQualityManagement(page);
    await expand(navigation, "menu_nonconformity");
    for (const child of CHILDREN) {
      await expect(navigation.locator(`a[id="${child}_nav"]`)).toBeVisible();
    }
  } finally {
    await apiPost(page, "/rest/menu/menu_nonconformity", original);
  }
});
