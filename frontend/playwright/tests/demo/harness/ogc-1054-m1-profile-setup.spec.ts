import { expect, test } from "../../../helpers/test-base";
import type { Locator, Page } from "@playwright/test";
import { AnalyzerFormPage } from "../../../fixtures/analyzer-form";
import { AnalyzerListPage } from "../../../fixtures/analyzer-list";

function escapeRegExp(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

async function findAnalyzerRow(page: Page, name: string): Promise<Locator> {
  await page.locator('[data-testid="analyzer-search-input"]').fill(name);
  const row = page.locator("tbody tr", {
    hasText: new RegExp(escapeRegExp(name), "i"),
  });
  await expect(row).toHaveCount(1);
  await expect(row).toBeVisible();
  return row;
}

async function deleteAnalyzerThroughUi(page: Page, name: string) {
  await page.goto("analyzers", { waitUntil: "domcontentloaded" });
  await expect(page.locator('[data-testid="analyzers-list"]')).toBeVisible();
  const row = await findAnalyzerRow(page, name);
  await row.locator('[data-testid^="analyzer-row-overflow-"]').click();
  await page.locator('[data-testid*="analyzer-action-delete"]').click();
  await page
    .getByRole("button", { name: /delete|confirm/i })
    .last()
    .click();
  await expect(row).not.toBeVisible();
}

test.describe("OGC-1054 M1 profile-backed analyzer setup", () => {
  test.describe.configure({ mode: "serial" });

  const suffix = Date.now();
  const geneXpertName = `M1 GeneXpert ${suffix}`;
  const secondGeneXpertName = `M1 GeneXpert second ${suffix}`;
  const fluoroCyclerName = `M1 FluoroCycler ${suffix}`;

  test.afterEach(async ({ page }) => {
    for (const name of [geneXpertName, secondGeneXpertName, fluoroCyclerName]) {
      try {
        await deleteAnalyzerThroughUi(page, name);
      } catch {
        // A failed story may not have reached creation.
      }
    }
  });

  test("creates two GeneXpert connections from the same exact reusable type", async ({
    page,
  }) => {
    const list = new AnalyzerListPage(page);
    const form = new AnalyzerFormPage(page);

    await list.goto();
    await list.expectLoaded();
    await list.clickAdd();
    await form.expectOpen();

    await expect(
      page.getByRole("heading", { level: 1, name: "Add New Analyzer" }),
    ).toBeVisible();
    await expect(
      page
        .getByTestId("analyzer-form-header")
        .getByRole("link", { name: "Analyzers", exact: true }),
    ).toBeVisible();

    await form.fillName(geneXpertName);
    await form.selectProfile("Cepheid GeneXpert (ASTM Mode)");

    await expect(page).toHaveURL(
      /\/analyzers\/new\?profile=genexpert-astm&revision=1$/,
    );
    await expect(form.communicationModeDropdown).toContainText(
      "Bidirectional (both directions)",
    );
    await form.fillIpAddress("10.42.20.50");
    await form.fillPort("9600");

    await form.save();
    await form.expectSuccessNotification();
    await expect(form.surface).not.toBeVisible();

    const row = await findAnalyzerRow(page, geneXpertName);
    await expect(row).toContainText("Cepheid GeneXpert (ASTM Mode)");

    await list.clickAdd();
    await form.expectOpen();
    await form.fillName(secondGeneXpertName);
    await form.selectProfile("Cepheid GeneXpert (ASTM Mode)");

    await expect(page).toHaveURL(
      /\/analyzers\/new\?profile=genexpert-astm&revision=1$/,
    );
    await expect(form.communicationModeDropdown).toContainText(
      "Bidirectional (both directions)",
    );
    await form.fillIpAddress("10.42.20.51");
    await form.fillPort("9601");

    await form.save();
    await form.expectSuccessNotification();
    await expect(form.surface).not.toBeVisible();

    const secondRow = await findAnalyzerRow(page, secondGeneXpertName);
    await expect(secondRow).toContainText("Cepheid GeneXpert (ASTM Mode)");
  });

  test("creates a FluoroCycler FILE connection from its exact reusable type", async ({
    page,
  }) => {
    const list = new AnalyzerListPage(page);
    const form = new AnalyzerFormPage(page);

    await list.goto();
    await list.expectLoaded();
    await list.clickAdd();
    await form.expectOpen();

    await form.fillName(fluoroCyclerName);
    await form.selectProfile("Bruker FluoroCycler XT");

    await expect(page).toHaveURL(
      /\/analyzers\/new\?profile=fluorocycler-xt&revision=1$/,
    );
    await expect(form.connectionFields).not.toBeVisible();
    await expect(form.importDirectoryInput).toBeVisible();
    await form.fillImportDirectory(
      `/data/analyzer-imports/m1-fluorocycler-${suffix}/incoming`,
    );

    await form.save();
    await form.expectSuccessNotification();
    await expect(form.surface).not.toBeVisible();

    const row = await findAnalyzerRow(page, fluoroCyclerName);
    await expect(row).toContainText("Bruker FluoroCycler XT");
  });
});
