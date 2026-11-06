import { expect, type Locator, type Page } from "@playwright/test";
import { LONG_TIMEOUT } from "./timeouts";
import {
  ensureWorkspaceGrowthCulture,
  type SeededMicrobiologyCase,
} from "./seed-microbiology-data";

const caseScopeHeading = (page: Page, scope: string) =>
  page.locator(
    scope === "HISTORY"
      ? "#case-history .cds--accordion__heading"
      : `#case-scope-${scope} > .cds--accordion__heading`,
  );

const expandAccordion = async (heading: Locator) => {
  await expect(heading).toBeVisible({ timeout: LONG_TIMEOUT });
  if ((await heading.getAttribute("aria-expanded")) !== "true")
    await heading.click();
  await expect(heading).toHaveAttribute("aria-expanded", "true");
};

export const openMicrobiologyCaseScope = (page: Page, scope: string) =>
  expandAccordion(caseScopeHeading(page, scope));

export const expectMicrobiologyCaseScopeOpen = async (
  page: Page,
  scope: string,
) =>
  expect(caseScopeHeading(page, scope)).toHaveAttribute(
    "aria-expanded",
    "true",
    {
      timeout: LONG_TIMEOUT,
    },
  );

export const caseBarButton = (page: Page, name: string | RegExp) =>
  page
    .getByLabel("Case summary", { exact: true })
    .getByRole(
      "button",
      typeof name === "string" ? { name, exact: true } : { name },
    );

export const releaseFinalReport = async (page: Page) => {
  const release = caseBarButton(page, "Release final report");
  await expect(release).toBeEnabled({ timeout: LONG_TIMEOUT });
  await release.click();
  const confirm = page.getByRole("dialog", { name: "Release final report?" });
  await confirm
    .getByRole("button", { name: "Release final", exact: true })
    .click();
  await expect(confirm).toHaveCount(0, { timeout: LONG_TIMEOUT });
};

export const openMicrobiologyCaseInfo = (page: Page) =>
  expandAccordion(page.getByRole("button", { name: "Case info", exact: true }));

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
  await openMicrobiologyCaseScope(page, values[sectionName]);
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
