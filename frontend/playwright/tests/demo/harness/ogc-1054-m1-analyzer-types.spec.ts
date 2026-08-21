import { expect, test } from "../../../helpers/test-base";
import type { Locator, Page } from "@playwright/test";
import { AnalyzerFormPage } from "../../../fixtures/analyzer-form";
import { AnalyzerListPage } from "../../../fixtures/analyzer-list";
import { NAV_TIMEOUT, TIMEOUT_SCALE } from "../../../helpers/timeouts";

const SOURCE_PROFILE = "Cepheid GeneXpert (ASTM Mode)";

function escapeRegExp(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

async function openAnalyzerTypes(page: Page) {
  await page.goto("/analyzers/types", {
    waitUntil: "domcontentloaded",
    timeout: NAV_TIMEOUT,
  });
  await expect(
    page.getByRole("heading", { level: 1, name: "Analyzer Types" }),
  ).toBeVisible();
  await expect(page.getByRole("table")).toBeVisible();
}

function analyzerTypeRow(page: Page, name: string): Locator {
  return page.getByRole("row", {
    name: new RegExp(escapeRegExp(name), "i"),
  });
}

async function openRowAction(page: Page, name: string, action: string) {
  await page
    .getByRole("button", { name: `Actions for ${name}`, exact: true })
    .click();
  await page.getByRole("menuitem", { name: action, exact: true }).click();
}

test.describe("OGC-1054 M1 Analyzer Types", () => {
  test.describe.configure({ mode: "serial" });

  test("shows the lab-facing catalog and restores bookmarkable filters", async ({
    page,
  }, testInfo) => {
    test.setTimeout(120_000 * TIMEOUT_SCALE);
    await openAnalyzerTypes(page);

    const breadcrumb = page.getByRole("navigation", { name: "Breadcrumb" });
    await expect(
      breadcrumb.getByRole("link", { name: "Home" }),
    ).toHaveAttribute("href", "/");
    await expect(
      breadcrumb.getByRole("link", { name: "Analyzers", exact: true }),
    ).toHaveAttribute("href", "/analyzers");
    await expect(page.locator("h1")).toHaveCount(1);
    await expect(page.getByText("Reusable analyzer setup")).toBeVisible();

    const summary = page.getByRole("region", {
      name: "Analyzer type summary",
    });
    await expect(summary).toContainText("Analyzer Types");
    await expect(summary).toContainText("In Use");
    await expect(summary).toContainText("Needs Attention");
    await expect(summary).toContainText("Deactivated");

    const sourceRow = analyzerTypeRow(page, SOURCE_PROFILE);
    await expect(sourceRow).toBeVisible();
    await expect(sourceRow).toContainText("Shipped");
    await expect(sourceRow).toContainText("ASTM");
    await expect(sourceRow).toContainText("revision 1");
    await expect(sourceRow).toContainText("Active");
    await expect(page.getByText("Plugin class")).toHaveCount(0);
    await expect(page.getByText("Identifier pattern")).toHaveCount(0);

    const search = page.getByRole("searchbox", {
      name: "Search analyzer types",
    });
    await search.fill("gene");
    await page
      .getByRole("combobox", { name: "Created" })
      .selectOption("SHIPPED");
    await page.getByRole("combobox", { name: "Protocol" }).selectOption("ASTM");
    await page
      .getByRole("combobox", { name: "Mapping status" })
      .selectOption("INCOMPLETE");
    await page.getByText("Show deactivated", { exact: true }).click();

    const filteredUrl =
      "/analyzers/types?q=gene&source=SHIPPED&protocol=ASTM&mapping=INCOMPLETE&showDeactivated=true";
    await expect(page).toHaveURL(new RegExp(`${escapeRegExp(filteredUrl)}$`));
    await expect(analyzerTypeRow(page, SOURCE_PROFILE)).toBeVisible();
    await expect(page.getByRole("table").getByRole("row")).toHaveCount(2);

    await page.reload({
      waitUntil: "domcontentloaded",
      timeout: NAV_TIMEOUT,
    });
    await expect(page.getByRole("table")).toBeVisible();
    await expect(search).toHaveValue("gene");
    await expect(page.getByRole("combobox", { name: "Created" })).toHaveValue(
      "SHIPPED",
    );
    await expect(page.getByRole("combobox", { name: "Protocol" })).toHaveValue(
      "ASTM",
    );
    await expect(
      page.getByRole("combobox", { name: "Mapping status" }),
    ).toHaveValue("INCOMPLETE");
    await expect(
      page.getByRole("checkbox", { name: "Show deactivated" }),
    ).toBeChecked();

    await page.goBack();
    await expect(page).toHaveURL(
      /\/analyzers\/types\?q=gene&source=SHIPPED&protocol=ASTM&mapping=INCOMPLETE$/,
    );
    await page.goForward();
    await expect(page).toHaveURL(new RegExp(`${escapeRegExp(filteredUrl)}$`));

    await testInfo.attach("analyzer-types-filtered", {
      body: await page.screenshot(),
      contentType: "image/png",
    });
  });

  test("starts a Bridge-owned site profile draft from Create Profile", async ({
    page,
  }) => {
    test.setTimeout(120_000 * TIMEOUT_SCALE);
    const draftName = `M1 Site Draft ${Date.now()}`;
    await openAnalyzerTypes(page);
    await page
      .getByRole("button", { name: "Create Profile", exact: true })
      .click();

    const dialog = page.getByRole("dialog", { name: "Create Profile" });
    await expect(dialog).toBeVisible();
    await dialog.getByRole("textbox", { name: "Profile name" }).fill(draftName);
    await dialog
      .getByRole("button", { name: "Create Profile", exact: true })
      .click();

    await expect(dialog.getByText("Profile draft created")).toBeVisible();
    await expect(dialog).toContainText("saved in Analyzer Bridge");
    await expect(page).toHaveURL(
      /\/analyzers\/types\?action=create&draft=[^&]+$/,
    );

    await page.keyboard.press("Escape");
    await expect(dialog).not.toBeVisible();

    const list = new AnalyzerListPage(page);
    const form = new AnalyzerFormPage(page);
    await page
      .getByRole("navigation", { name: "Breadcrumb" })
      .getByRole("link", { name: "Analyzers", exact: true })
      .click();
    await list.expectLoaded();
    await list.clickAdd();
    await form.expectOpen();
    await form.profileDropdown
      .locator('button[role="combobox"], .cds--list-box__field')
      .click();
    await expect(
      form.profileDropdown.getByRole("option", { name: draftName }),
    ).toHaveCount(0);
  });

  test("duplicates a profile without changing its source and retains lifecycle history", async ({
    page,
  }, testInfo) => {
    test.setTimeout(180_000 * TIMEOUT_SCALE);
    const duplicateName = `M1 GeneXpert Type ${Date.now()}`;
    await openAnalyzerTypes(page);
    const sourceCells = analyzerTypeRow(page, SOURCE_PROFILE).getByRole("cell");
    const sourceBefore = await sourceCells.allTextContents();

    await page
      .getByRole("button", { name: "Duplicate Profile", exact: true })
      .click();
    let dialog = page.getByRole("dialog", { name: "Duplicate Profile" });
    await dialog
      .getByRole("combobox", { name: "Source analyzer type" })
      .selectOption({ label: `${SOURCE_PROFILE} · ASTM` });
    await expect(
      dialog.getByText("New profile with preserved lineage"),
    ).toBeVisible();
    await dialog
      .getByRole("textbox", { name: "New profile name" })
      .fill(duplicateName);
    await dialog
      .getByRole("button", { name: "Duplicate Profile", exact: true })
      .click();

    dialog = page.getByRole("dialog", { name: "Duplicate Profile" });
    await expect(dialog.getByText("Ready to publish")).toBeVisible();
    await expect(page).toHaveURL(
      /\/analyzers\/types\?action=duplicate&draft=[^&]+$/,
    );
    await dialog
      .getByRole("button", { name: "Publish Profile", exact: true })
      .click();

    await expect(page.getByText("Profile duplicated")).toBeVisible();
    await expect(page).toHaveURL(/\/analyzers\/types$/);
    const duplicateRow = analyzerTypeRow(page, duplicateName);
    await expect(duplicateRow).toBeVisible();
    await expect(duplicateRow).toContainText("Site-created");
    await expect(duplicateRow).toContainText(
      "Derived from genexpert-astm revision 1",
    );
    await expect(duplicateRow).toContainText("revision 1");
    await expect(duplicateRow).toContainText("Not in use");
    await expect(sourceCells).toHaveText(sourceBefore);

    await openRowAction(page, duplicateName, "View history");
    const history = page.getByRole("dialog", {
      name: `${duplicateName} history`,
    });
    await expect(history).toContainText("Duplicated");
    await expect(history).toContainText("Changed by");
    await history.getByRole("button", { name: "Close" }).click();

    await openRowAction(page, duplicateName, "Deactivate");
    let lifecycle = page.getByRole("dialog", {
      name: `Deactivate ${duplicateName}`,
    });
    await lifecycle.getByRole("button", { name: /Deactivate$/ }).click();
    await expect(page.getByText("Profile deactivated")).toBeVisible();
    await expect(duplicateRow).toHaveCount(0);

    const showDeactivated = page.getByRole("checkbox", {
      name: "Show deactivated",
    });
    await page.getByText("Show deactivated", { exact: true }).click();
    await expect(showDeactivated).toBeChecked();
    await expect(analyzerTypeRow(page, duplicateName)).toContainText(
      "Deactivated",
    );

    await openRowAction(page, duplicateName, "Reactivate");
    lifecycle = page.getByRole("dialog", {
      name: `Reactivate ${duplicateName}`,
    });
    await lifecycle
      .getByRole("button", { name: "Reactivate", exact: true })
      .click();
    await expect(page.getByText("Profile reactivated")).toBeVisible();
    await expect(analyzerTypeRow(page, duplicateName)).toContainText("Active");

    await openRowAction(page, duplicateName, "Deactivate");
    lifecycle = page.getByRole("dialog", {
      name: `Deactivate ${duplicateName}`,
    });
    await lifecycle.getByRole("button", { name: /Deactivate$/ }).click();
    await expect(page.getByText("Profile deactivated")).toBeVisible();
    await expect(analyzerTypeRow(page, duplicateName)).toContainText(
      "Deactivated",
    );

    await testInfo.attach("analyzer-type-lifecycle", {
      body: await page.screenshot(),
      contentType: "image/png",
    });
  });
});
