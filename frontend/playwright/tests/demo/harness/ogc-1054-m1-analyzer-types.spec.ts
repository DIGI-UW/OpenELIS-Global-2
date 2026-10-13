import { expect, test } from "../../../helpers/test-base";
import { AnalyzerSetupPage } from "../../../fixtures/analyzer-setup";
import { AnalyzerListPage } from "../../../fixtures/analyzer-list";
import {
  analyzerTypeRow,
  openAnalyzerTypes,
  openRowAction,
} from "../../../helpers/analyzer-types-page";
import { createDemoPresentation } from "../../../helpers/demo-presentation";
import { expectNoPageHorizontalOverflow } from "../../../helpers/responsive-layout";

const SOURCE_PROFILE = "Cepheid GeneXpert (ASTM Mode)";

function escapeRegExp(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

test.describe("OGC-1054 M1 Analyzer Types", () => {
  test.describe.configure({ mode: "serial" });

  test(
    "shows the lab-facing catalog and restores bookmarkable filters",
    { tag: ["@evidence", "@astm", "@admin"] },
    async ({ page }, testInfo) => {
      const demo = createDemoPresentation(page, testInfo);
      await demo.intro(
        "The analyzer types a lab can set up",
        "What the Bridge ships, what this site created, and which types are in use.",
      );
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
      await demo.caption(
        "The summary counts the types, those in use, those needing attention, and those deactivated.",
      );
      await demo.highlight(summary);

      const sourceRow = analyzerTypeRow(page, SOURCE_PROFILE);
      await expect(sourceRow).toBeVisible();
      await expect(sourceRow).toContainText("Shipped");
      await expect(sourceRow).toContainText("ASTM");
      await expect(sourceRow).toContainText(/revision [1-9]\d*/);
      await expect(sourceRow).toContainText("Active");
      // Tests mapped and Results mapped read "mapped / total": never more mapped than there are.
      for (const column of [2, 3]) {
        const cell = sourceRow.getByRole("cell").nth(column);
        await expect(cell).toHaveText(/\d+ \/ \d+/);
        const [mapped, total] = ((await cell.textContent()) ?? "")
          .match(/(\d+) \/ (\d+)/)!
          .slice(1)
          .map(Number);
        expect(mapped).toBeLessThanOrEqual(total);
      }
      await expect(sourceRow.getByRole("cell").nth(4)).toContainText(
        /analyzer|Not in use/,
      );
      await expect(page.getByText("Plugin class")).toHaveCount(0);
      await expect(page.getByText("Identifier pattern")).toHaveCount(0);
      await demo.caption(
        "A shipped type shows its protocol, its current revision, and the analyzers using it. No plugin names, no patterns.",
      );
      await demo.highlight(sourceRow);

      await demo.caption(
        "Filters are part of the page's address: the view is bookmarkable and survives a reload.",
      );
      const search = page.getByRole("searchbox", {
        name: "Search analyzer types",
      });
      await search.fill("gene");
      await page
        .getByRole("combobox", { name: "Created" })
        .selectOption("SHIPPED");
      await page
        .getByRole("combobox", { name: "Protocol" })
        .selectOption("ASTM");
      await page.getByText("Show deactivated", { exact: true }).click();

      const filteredUrl =
        "/analyzers/types?q=gene&source=SHIPPED&protocol=ASTM&showDeactivated=true";
      await expect(page).toHaveURL(new RegExp(`${escapeRegExp(filteredUrl)}$`));
      await expect(analyzerTypeRow(page, SOURCE_PROFILE)).toBeVisible();
      await expect(page.getByRole("table").getByRole("row")).toHaveCount(2);

      await page.reload({
        waitUntil: "domcontentloaded",
      });
      await expect(page.getByRole("table")).toBeVisible();
      await expect(search).toHaveValue("gene");
      await expect(page.getByRole("combobox", { name: "Created" })).toHaveValue(
        "SHIPPED",
      );
      await expect(
        page.getByRole("combobox", { name: "Protocol" }),
      ).toHaveValue("ASTM");
      await expect(
        page.getByRole("checkbox", { name: "Show deactivated" }),
      ).toBeChecked();

      await page.goBack();
      await expect(page).toHaveURL(
        /\/analyzers\/types\?q=gene&source=SHIPPED&protocol=ASTM$/,
      );
      await page.goForward();
      await expect(page).toHaveURL(new RegExp(`${escapeRegExp(filteredUrl)}$`));
      await expect(sourceRow).toBeInViewport({ ratio: 1 });

      await testInfo.attach("analyzer-types-filtered", {
        body: await page.screenshot(),
        contentType: "image/png",
      });
      await demo.verified(
        "The catalog shows each type's source, protocol, revision and use",
        "Its filters restore from the address after a reload and through history.",
      );
    },
  );

  test("keeps the Analyzer Types workflow reachable on mobile", async ({
    page,
  }, testInfo) => {
    await page.setViewportSize({ width: 390, height: 844 });
    await openAnalyzerTypes(page);

    await expect(
      page.getByRole("button", { name: "Duplicate Profile", exact: true }),
    ).toBeVisible();
    await expect(
      page.getByRole("button", { name: "Create Profile", exact: true }),
    ).toBeVisible();
    await expect(
      page.getByRole("searchbox", { name: "Search analyzer types" }),
    ).toBeVisible();
    await expect(page.getByRole("table")).toBeVisible();
    await expectNoPageHorizontalOverflow(
      page,
      "Analyzer Types should not overflow the mobile page horizontally",
    );

    const rowAction = page.getByRole("button", {
      name: `Actions for ${SOURCE_PROFILE}`,
      exact: true,
    });
    await rowAction.scrollIntoViewIfNeeded();
    await expect(rowAction).toBeVisible();
    await rowAction.click();
    await expect(
      page.getByRole("menuitem", { name: "Duplicate Profile", exact: true }),
    ).toBeVisible();
    await page.keyboard.press("Escape");

    await testInfo.attach("analyzer-types-mobile", {
      body: await page.screenshot({ fullPage: true }),
      contentType: "image/png",
    });
  });

  test("starts a Bridge-owned site profile draft from Create Profile", async ({
    page,
  }) => {
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
    const setup = new AnalyzerSetupPage(page);
    await page
      .getByRole("navigation", { name: "Breadcrumb" })
      .getByRole("link", { name: "Analyzers", exact: true })
      .click();
    await list.expectLoaded();
    await list.clickAdd();
    await setup.expectOpen();
    await setup.typePicker.click();
    await setup.typePicker.fill(draftName);
    await expect(
      page.getByRole("option", { name: new RegExp(draftName, "i") }),
    ).toHaveCount(0);
  });

  test("duplicates a profile without changing its source and retains lifecycle history", async ({
    page,
  }, testInfo) => {
    const duplicateName = `M1 GeneXpert Type ${Date.now()}`;
    await openAnalyzerTypes(page);
    const sourceRow = analyzerTypeRow(page, SOURCE_PROFILE);
    await expect(sourceRow).toContainText(/revision [1-9]\d*/);
    const sourceCells = sourceRow.getByRole("cell");
    const sourceBefore = await sourceCells.allTextContents();
    const sourceRevision = (await sourceRow.innerText()).match(
      /\brevision ([1-9]\d*)\b/i,
    )?.[1];
    expect(
      sourceRevision,
      "The source row should identify its revision",
    ).toBeTruthy();

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
      `Derived from cepheid-genexpert-astm revision ${sourceRevision}`,
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
