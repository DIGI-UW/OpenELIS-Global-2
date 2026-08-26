import { expect, test } from "../../../helpers/test-base";
import type { Page } from "@playwright/test";
import { NAV_TIMEOUT, TIMEOUT_SCALE } from "../../../helpers/timeouts";

const PROFILE_NAME = "Cepheid GeneXpert (ASTM Mode)";
const FILTERED_CATALOG = "/analyzers/types?q=gene&mapping=INCOMPLETE";

function escapeRegExp(value: string) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

async function openSharedMappingEditor(page: Page): Promise<string> {
  await page.goto(FILTERED_CATALOG, {
    waitUntil: "domcontentloaded",
    timeout: NAV_TIMEOUT,
  });
  await expect(
    page.getByRole("heading", { level: 1, name: "Analyzer Types" }),
  ).toBeVisible();

  const profileRow = page.getByRole("row", {
    name: new RegExp(escapeRegExp(PROFILE_NAME), "i"),
  });
  await expect(profileRow).toBeVisible();
  const revision = (await profileRow.innerText()).match(
    /\brevision ([1-9]\d*)\b/i,
  )?.[1];
  expect(revision, "The profile row should identify its revision").toBeTruthy();
  await profileRow
    .getByRole("button", { name: `Actions for ${PROFILE_NAME}` })
    .click();
  await page.getByRole("menuitem", { name: "Edit mappings" }).click();
  return revision!;
}

async function openMappingEditorFor(page: Page, profileName: string) {
  const profileRow = page.getByRole("row", {
    name: new RegExp(escapeRegExp(profileName), "i"),
  });
  await expect(profileRow).toBeVisible();
  await profileRow
    .getByRole("button", { name: `Actions for ${profileName}` })
    .click();
  await page.getByRole("menuitem", { name: "Edit mappings" }).click();
}

test.describe("OGC-1054 M2 shared analyzer type mapping", () => {
  test("reviews every profile row and returns to the exact catalog bookmark", async ({
    page,
  }, testInfo) => {
    test.setTimeout(120_000 * TIMEOUT_SCALE);
    const revision = await openSharedMappingEditor(page);

    await expect(page).toHaveURL((url) => {
      return (
        url.pathname === "/analyzers/types/genexpert-astm/mapping" &&
        url.searchParams.get("revision") === revision &&
        url.searchParams.get("returnTo") === FILTERED_CATALOG
      );
    });
    await expect(
      page.getByRole("heading", {
        level: 1,
        name: `${PROFILE_NAME} mappings`,
      }),
    ).toBeVisible();
    await expect(page.locator("h1")).toHaveCount(1);

    const breadcrumb = page.getByRole("navigation", { name: "Breadcrumb" });
    await expect(
      breadcrumb.getByRole("link", { name: "Analyzers", exact: true }),
    ).toHaveAttribute("href", "/analyzers");
    await expect(
      breadcrumb.getByRole("link", { name: "Analyzer Types", exact: true }),
    ).toHaveAttribute("href", FILTERED_CATALOG);

    const sourceRows = page.getByTestId("analyzer-type-mapping-row");
    await expect(sourceRows).toHaveCount(4);
    for (const code of ["MTB-RIF", "RIF", "HIV-VL", "COVID19"]) {
      await expect(page.getByText(code, { exact: true }).first()).toBeVisible();
    }

    await expect(
      page.getByRole("heading", {
        level: 2,
        name: "Control result recognition",
      }),
    ).toBeVisible();
    await expect(
      page.getByText("Rule-based control recognition").first(),
    ).toBeVisible();
    await expect(page.getByText("O.12", { exact: true })).toHaveCount(0);
    await expect(
      page.getByRole("button", { name: "Update shared mappings" }),
    ).toBeDisabled();
    await expect(
      page.getByRole("button", {
        name: "Confirm mappings and control recognition",
      }),
    ).toBeDisabled();

    await testInfo.attach("analyzer-type-shared-mapping", {
      body: await page.screenshot({ fullPage: true }),
      contentType: "image/png",
    });

    const mappingUrl = page.url();
    await page.reload({ waitUntil: "domcontentloaded", timeout: NAV_TIMEOUT });
    await expect(page).toHaveURL(mappingUrl);
    await expect(sourceRows).toHaveCount(4);

    await breadcrumb
      .getByRole("link", { name: "Analyzer Types", exact: true })
      .click();
    await expect(page).toHaveURL(
      (url) => `${url.pathname}${url.search}` === FILTERED_CATALOG,
    );
    await expect(
      page.getByRole("searchbox", { name: "Search analyzer types" }),
    ).toHaveValue("gene");
    await expect(
      page.getByRole("combobox", { name: "Mapping status" }),
    ).toHaveValue("INCOMPLETE");
  });

  test("keeps the complete shared mapping review reachable on mobile", async ({
    page,
  }, testInfo) => {
    test.setTimeout(120_000 * TIMEOUT_SCALE);
    await page.setViewportSize({ width: 390, height: 844 });
    await openSharedMappingEditor(page);

    await expect(
      page.getByRole("heading", {
        level: 1,
        name: `${PROFILE_NAME} mappings`,
      }),
    ).toBeVisible();
    await expect(page.getByTestId("analyzer-type-mapping-row")).toHaveCount(4);
    await expect(
      page.getByRole("link", { name: "Duplicate Profile", exact: true }),
    ).toBeVisible();
    await expect(
      page.getByRole("heading", {
        level: 2,
        name: "Control result recognition",
      }),
    ).toBeVisible();
    expect(
      await page.evaluate(
        () =>
          document.documentElement.scrollWidth <=
          document.documentElement.clientWidth,
      ),
      "Shared mapping review should not overflow the mobile page horizontally",
    ).toBe(true);

    await testInfo.attach("analyzer-type-shared-mapping-mobile", {
      body: await page.screenshot({ fullPage: true }),
      contentType: "image/png",
    });
  });

  test("maps catalog-owned qualitative values, saves, reloads, and confirms", async ({
    page,
  }, testInfo) => {
    test.setTimeout(180_000 * TIMEOUT_SCALE);
    const duplicateName = `M2 GeneXpert mapping ${Date.now()}`;

    await page.goto("/analyzers/types", {
      waitUntil: "domcontentloaded",
      timeout: NAV_TIMEOUT,
    });
    await expect(
      page.getByRole("heading", { level: 1, name: "Analyzer Types" }),
    ).toBeVisible();
    await page
      .getByRole("button", { name: "Duplicate Profile", exact: true })
      .click();

    let dialog = page.getByRole("dialog", { name: "Duplicate Profile" });
    await dialog
      .getByRole("combobox", { name: "Source analyzer type" })
      .selectOption({ label: `${PROFILE_NAME} · ASTM` });
    await dialog
      .getByRole("textbox", { name: "New profile name" })
      .fill(duplicateName);
    await dialog
      .getByRole("button", { name: "Duplicate Profile", exact: true })
      .click();

    dialog = page.getByRole("dialog", { name: "Duplicate Profile" });
    await expect(dialog.getByText("Ready to publish")).toBeVisible();
    await dialog
      .getByRole("button", { name: "Publish Profile", exact: true })
      .click();
    await expect(page.getByText("Profile duplicated")).toBeVisible();
    await openMappingEditorFor(page, duplicateName);

    await expect(
      page.getByRole("heading", {
        level: 1,
        name: `${duplicateName} mappings`,
      }),
    ).toBeVisible();
    for (const code of ["MTB-RIF", "HIV-VL", "COVID19"]) {
      await page.getByText(`Do not receive ${code}`, { exact: true }).click();
    }

    const rifRow = page
      .getByTestId("analyzer-type-mapping-row")
      .filter({ has: page.getByText("RIF", { exact: true }) });
    await rifRow.getByRole("button", { name: "Use suggested test" }).click();
    await page.getByRole("button", { name: /^RIF.*Mapped$/ }).click();

    const resistant = rifRow.getByRole("combobox", {
      name: "OpenELIS result for DETECTED",
    });
    await expect(resistant).toBeVisible();
    await resistant.click();
    await page.getByRole("option", { name: "RESISTANT", exact: true }).click();

    const susceptible = rifRow.getByRole("combobox", {
      name: "OpenELIS result for NOT DETECTED",
    });
    await susceptible.click();
    await page
      .getByRole("option", { name: "Susceptible", exact: true })
      .click();
    const indeterminateRow = rifRow
      .locator(".analyzer-type-mapping__result-row")
      .filter({ has: page.getByText("INDETERMINATE", { exact: true }) });
    await indeterminateRow.getByText("Do not receive", { exact: true }).click();

    const save = page.getByRole("button", {
      name: "Update shared mappings",
    });
    await expect(save).toBeEnabled();
    await save.click();
    await expect(page.getByText("Mappings saved")).toBeVisible();

    await page.reload({ waitUntil: "domcontentloaded", timeout: NAV_TIMEOUT });
    await expect(
      page.getByRole("button", { name: /^MTB-RIF.*Do not receive$/ }),
    ).toBeVisible();
    await page.getByRole("button", { name: /^RIF.*Mapped$/ }).click();
    await expect(
      page.getByRole("combobox", { name: "OpenELIS result for DETECTED" }),
    ).toHaveAttribute("title", "RESISTANT");
    await expect(
      page.getByRole("combobox", {
        name: "OpenELIS result for NOT DETECTED",
      }),
    ).toHaveAttribute("title", "Susceptible");

    const confirm = page.getByRole("button", {
      name: "Confirm mappings and control recognition",
    });
    await expect(confirm).toBeEnabled();
    await confirm.click();
    await expect(page.getByText("Current confirmation")).toBeVisible();
    const confirmedBy = page.getByText(/^Confirmed by /);
    await expect(confirmedBy).toBeVisible();
    await expect(confirmedBy).not.toHaveText(/^Confirmed by \d+$/);
    await expect(confirmedBy).toContainText(String(new Date().getFullYear()));

    await page.evaluate(() => window.scrollTo(0, 0));
    await expect(
      page.getByText("Mappings and control recognition confirmed"),
    ).toBeVisible();
    await testInfo.attach("analyzer-type-mapping-confirmed-summary", {
      body: await page.screenshot(),
      contentType: "image/png",
    });

    await page.getByText("Current confirmation").scrollIntoViewIfNeeded();
    await testInfo.attach("analyzer-type-mapping-confirmed-evidence", {
      body: await page.screenshot(),
      contentType: "image/png",
    });

    await page.reload({ waitUntil: "domcontentloaded", timeout: NAV_TIMEOUT });
    await expect(page.getByText("Current confirmation")).toBeVisible();
  });
});
