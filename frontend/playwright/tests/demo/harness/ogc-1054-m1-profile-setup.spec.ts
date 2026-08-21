import { expect, test } from "../../../helpers/test-base";
import type { Locator, Page } from "@playwright/test";
import { AnalyzerFormPage } from "../../../fixtures/analyzer-form";
import { AnalyzerListPage } from "../../../fixtures/analyzer-list";
import { TIMEOUT_SCALE, UI_TIMEOUT } from "../../../helpers/timeouts";

function escapeRegExp(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

async function findAnalyzerRow(page: Page, name: string): Promise<Locator> {
  await page.locator('[data-testid="analyzer-search-input"]').fill(name);
  await expect(page).toHaveURL(
    (url) => url.searchParams.get("search") === name,
    {
      timeout: UI_TIMEOUT,
    },
  );

  const rows = page.locator("tbody tr");
  await expect(rows).toHaveCount(1, { timeout: UI_TIMEOUT });
  const row = rows.first();
  await expect(row).toContainText(new RegExp(escapeRegExp(name), "i"));
  await expect(row).toBeVisible();
  await row.scrollIntoViewIfNeeded();
  return row;
}

test.describe("OGC-1054 M1 profile-backed analyzer setup", () => {
  test.describe.configure({ mode: "serial" });

  const suffix = Date.now();
  const geneXpertName = `M1 GeneXpert ${suffix}`;
  const secondGeneXpertName = `M1 GeneXpert second ${suffix}`;
  const fluoroCyclerName = `M1 FluoroCycler ${suffix}`;

  test("creates two GeneXpert connections from the same exact reusable type", async ({
    page,
  }, testInfo) => {
    test.setTimeout(180_000 * TIMEOUT_SCALE);
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

    await testInfo.attach("gene-xpert-profile-setup", {
      body: await page.screenshot(),
      contentType: "image/png",
    });
  });

  test("creates a FluoroCycler FILE connection from its exact reusable type", async ({
    page,
  }, testInfo) => {
    test.setTimeout(120_000 * TIMEOUT_SCALE);
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

    await testInfo.attach("fluorocycler-profile-setup", {
      body: await page.screenshot(),
      contentType: "image/png",
    });
  });
});
