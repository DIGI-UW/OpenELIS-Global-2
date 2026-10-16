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
      const views = page.getByTestId("analyzer-type-views");
      await expect(views).toContainText("Active");
      await expect(views).toContainText("In use");
      await expect(views).toContainText("Needs mapping");
      await expect(views).toContainText("Deactivated");
      await demo.caption(
        "One line of counts: active types, those in use, those that need mapping, and those deactivated. Each count is the view it names.",
      );
      await demo.highlight(views);

      const sourceRow = analyzerTypeRow(page, SOURCE_PROFILE);
      await expect(sourceRow).toBeVisible();
      await expect(sourceRow).toContainText("Shipped");
      await expect(sourceRow).toContainText("ASTM");
      await expect(sourceRow).toContainText(/revision [1-9]\d*/);
      await expect(sourceRow).toContainText("Active");
      // Mapping reads "mapped of total": never more mapped than there are.
      const mappingCell = sourceRow.getByRole("cell").nth(1);
      await expect(mappingCell).toHaveText(/\d+ of \d+ tests/);
      for (const [, mapped, total] of (
        (await mappingCell.textContent()) ?? ""
      ).matchAll(/(\d+) of (\d+)/g)) {
        expect(Number(mapped)).toBeLessThanOrEqual(Number(total));
      }
      await expect(sourceRow.getByRole("cell").nth(2)).toContainText(
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
        .getByRole("combobox", { name: "Source" })
        .selectOption("SHIPPED");
      await page
        .getByRole("combobox", { name: "Protocol" })
        .selectOption("ASTM");

      const filteredUrl =
        "/analyzers/types?q=gene&source=SHIPPED&protocol=ASTM";
      await expect(page).toHaveURL(new RegExp(`${escapeRegExp(filteredUrl)}$`));
      await expect(analyzerTypeRow(page, SOURCE_PROFILE)).toBeVisible();
      await expect(page.getByRole("table").getByRole("row")).toHaveCount(2);

      await page.reload({
        waitUntil: "domcontentloaded",
      });
      await expect(page.getByRole("table")).toBeVisible();
      await expect(search).toHaveValue("gene");
      await expect(page.getByRole("combobox", { name: "Source" })).toHaveValue(
        "SHIPPED",
      );
      await expect(
        page.getByRole("combobox", { name: "Protocol" }),
      ).toHaveValue("ASTM");

      await page.goBack();
      await expect(page).toHaveURL(
        /\/analyzers\/types\?q=gene&source=SHIPPED$/,
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
      page.getByRole("button", { name: "Duplicate type", exact: true }),
    ).toBeVisible();
    await expect(
      page.getByRole("button", { name: "Create type", exact: true }),
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
      page.getByRole("menuitem", { name: "Duplicate type", exact: true }),
    ).toBeVisible();
    await page.keyboard.press("Escape");

    await testInfo.attach("analyzer-types-mobile", {
      body: await page.screenshot({ fullPage: true }),
      contentType: "image/png",
    });
  });

  test("starts a Bridge-owned site profile draft from Create type", async ({
    page,
  }) => {
    const draftName = `M1 Site Draft ${Date.now()}`;
    await openAnalyzerTypes(page);
    await page
      .getByRole("button", { name: "Create type", exact: true })
      .click();

    const dialog = page.getByRole("dialog", { name: "Create type" });
    await expect(dialog).toBeVisible();
    await dialog.getByRole("textbox", { name: "Type name" }).fill(draftName);
    await dialog
      .getByRole("button", { name: "Create type", exact: true })
      .click();

    // The draft opens on its own page; Finish later leaves it saved.
    await expect(page).toHaveURL(
      /\/analyzers\/types\/drafts\/[^?]+\?returnTo=%2Fanalyzers%2Ftypes$/,
    );
    const draft = page.getByTestId("analyzer-type-draft-page");
    await expect(
      draft.getByRole("heading", { level: 1, name: `New type: ${draftName}` }),
    ).toBeVisible();
    await expect(draft).toContainText("saved in Analyzer Bridge");
    await draft
      .getByRole("button", { name: "Finish later", exact: true })
      .click();
    await expect(page).toHaveURL(/\/analyzers\/types$/);

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
      .getByRole("button", { name: "Duplicate type", exact: true })
      .click();
    const dialog = page.getByRole("dialog", { name: "Duplicate type" });
    await dialog
      .getByRole("combobox", { name: "Source analyzer type" })
      .selectOption({ label: `${SOURCE_PROFILE} · ASTM` });
    await expect(
      dialog.getByText("The new type keeps its lineage"),
    ).toBeVisible();
    await dialog
      .getByRole("textbox", { name: "Name of the new type" })
      .fill(duplicateName);
    await dialog
      .getByRole("button", { name: "Duplicate type", exact: true })
      .click();

    await expect(page).toHaveURL(
      /\/analyzers\/types\/drafts\/[^?]+\?returnTo=/,
    );
    const bar = page.getByTestId("analyzer-type-draft-actions");
    await expect(bar.getByText("Ready to publish")).toBeVisible();
    await bar
      .getByRole("button", { name: "Publish type", exact: true })
      .click();

    await expect(page.getByText("Type duplicated")).toBeVisible();
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

    await openRowAction(page, duplicateName, "History");
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
    await expect(page.getByText("Type deactivated")).toBeVisible();
    await expect(duplicateRow).toHaveCount(0);

    const deactivatedView = page.getByTestId("analyzer-type-view-deactivated");
    await deactivatedView.click();
    await expect(deactivatedView).toHaveAttribute("aria-pressed", "true");
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
    await expect(page.getByText("Type reactivated")).toBeVisible();
    // A reactivated type leaves the Deactivated view for the Active one.
    await expect(analyzerTypeRow(page, duplicateName)).toHaveCount(0);
    await page.getByTestId("analyzer-type-view-all").click();
    await expect(analyzerTypeRow(page, duplicateName)).toContainText("Active");

    await openRowAction(page, duplicateName, "Deactivate");
    lifecycle = page.getByRole("dialog", {
      name: `Deactivate ${duplicateName}`,
    });
    await lifecycle.getByRole("button", { name: /Deactivate$/ }).click();
    await expect(page.getByText("Type deactivated")).toBeVisible();
    await deactivatedView.click();
    await expect(analyzerTypeRow(page, duplicateName)).toContainText(
      "Deactivated",
    );

    await testInfo.attach("analyzer-type-lifecycle", {
      body: await page.screenshot(),
      contentType: "image/png",
    });
  });
});
