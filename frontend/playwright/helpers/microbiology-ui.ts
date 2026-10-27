import { expect, type Locator, type Page } from "@playwright/test";
import { LONG_TIMEOUT } from "./timeouts";
import {
  ensureWorkspaceGrowthCulture,
  type SeededMicrobiologyCase,
} from "./seed-microbiology-data";

export const openMicrobiologyCaseSection = async (
  page: Page,
  sectionName: string,
) => {
  const values: Record<string, string> = {
    Isolates: "ISOLATES",
    "Manual AST": "AST",
    Reports: "REPORT",
    Amendments: "AMENDMENT",
    "Critical communication": "CRITICAL",
    Timeline: "HISTORY",
    Inoculation: "CULTURE",
  };
  if (!values[sectionName])
    throw new Error(`Unknown case section ${sectionName}`);
  await page
    .getByLabel("Testing section", { exact: true })
    .selectOption(values[sectionName]);
};

export const selectCarbonRadio = async (page: Page, radio: Locator) => {
  await radio.focus();
  await expect(radio).toBeFocused();
  await page.keyboard.press("Space");
  await expect(radio).toBeChecked();
};

export const createMicrobiologyIsolate = async (
  page: Page,
  seeded: SeededMicrobiologyCase,
) => {
  const cultureId = await ensureWorkspaceGrowthCulture(page, seeded);
  await openMicrobiologyCaseSection(page, "Isolates");
  await page.getByRole("button", { name: "Pick isolate", exact: true }).click();
  const dialog = page.getByRole("dialog");
  await dialog
    .getByLabel("Source culture row", { exact: true })
    .selectOption(cultureId);
  await dialog
    .getByLabel("Gram stain", { exact: true })
    .fill("Gram negative rods");
  await dialog
    .getByLabel("Colony morphology", { exact: true })
    .fill("Lactose fermenting colonies");
  await dialog
    .getByLabel("Clinical significance", { exact: true })
    .selectOption("SIGNIFICANT");
  await dialog.getByRole("button", { name: "Save", exact: true }).click();
  await expect(
    page.getByText("Identification pending", { exact: true }),
  ).toBeVisible({ timeout: LONG_TIMEOUT });
};

export const identifyMicrobiologyIsolate = async (
  page: Page,
  organismId: string,
) => {
  await page
    .getByRole("button", { name: "Identify organism", exact: true })
    .click();
  const dialog = page.getByRole("dialog");
  const caseId = new URL(page.url()).pathname.split("/").pop();
  const options = await (
    await page.request.get(
      `/api/OpenELIS-Global/rest/microbiology/cases/${caseId}/isolates/options`,
    )
  ).json();
  const organism = options.organisms.find(
    (o: { id: string }) => o.id === organismId,
  );
  if (!organism)
    throw new Error("Fixture organism not offered for identification");
  const combo = dialog.getByRole("combobox", { name: "Organism", exact: true });
  await combo.fill(organism.value);
  await combo.press("ArrowDown");
  await combo.press("Enter");
  await dialog
    .getByLabel("ID method", { exact: true })
    .selectOption({ label: "UAT micro culture" });
  await dialog
    .getByLabel("Local identification status", { exact: true })
    .selectOption("FINAL");
  await dialog.getByLabel("ID confidence (%)", { exact: true }).fill("99.5");
  await dialog.getByRole("button", { name: "Save", exact: true }).click();
  await expect(page.getByRole("dialog")).toHaveCount(0, {
    timeout: LONG_TIMEOUT,
  });
  await expect(page.getByTitle("Final", { exact: true })).toBeVisible();
  const isolate = page.locator('[data-testid^="isolate-"]');
  await expect(
    isolate.getByRole("definition").filter({ hasText: /^UAT micro culture$/ }),
  ).toBeVisible();
  await expect(
    isolate.getByRole("definition").filter({ hasText: /^99\.5$/ }),
  ).toBeVisible();
};

export const createAndIdentifyMicrobiologyIsolate = async (
  page: Page,
  seeded: SeededMicrobiologyCase,
) => {
  await createMicrobiologyIsolate(page, seeded);
  await identifyMicrobiologyIsolate(page, seeded.organismId!);
};

export const selectCultureMedium = async (page: Page, mediumId: number) => {
  const caseId = new URL(page.url()).pathname.split("/").pop();
  const options = await (
    await page.request.get(
      `/api/OpenELIS-Global/rest/microbiology/cases/${caseId}/cultures/options`,
    )
  ).json();
  const medium = options.media.find(
    (m: { id: number }) => String(m.id) === String(mediumId),
  );
  if (!medium) throw new Error(`Fixture medium ${mediumId} not available`);
  const combo = page
    .getByRole("dialog")
    .getByRole("combobox", { name: "Medium", exact: true });
  await combo.fill(medium.name);
  await page
    .getByRole("option", {
      name: `${medium.name} · ${medium.trackLots ? "Lot tracked" : "Lot not tracked"}`,
      exact: true,
    })
    .click();
};

/**
 * Enters a container label the way a barcode scanner does: one burst of key
 * presses, then Enter. The worklist treats keys more than 80 ms apart as
 * typing, so the burst must not be paced by slowMo.
 */
export const scanContainerLabel = async (page: Page, code: string) => {
  await page.evaluate((text) => {
    for (const key of [...text, "Enter"]) {
      window.dispatchEvent(
        new KeyboardEvent("keydown", { key, bubbles: true }),
      );
    }
  }, code);
};
