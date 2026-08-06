import { expect, type Page } from "@playwright/test";
import { LONG_TIMEOUT } from "./timeouts";

export const openMicrobiologyCaseSection = async (
  page: Page,
  sectionName: string,
) => {
  await page
    .getByTestId("microbiology-case-view")
    .getByRole("button", { name: sectionName, exact: true })
    .click();
};

export const createAndIdentifyMicrobiologyIsolate = async (
  page: Page,
  organismId: string,
) => {
  await openMicrobiologyCaseSection(page, "Isolates");
  await page.getByLabel("Gram stain").fill("Gram negative rods");
  await page
    .getByLabel("Colony morphology")
    .fill("Lactose fermenting colonies");
  await page.getByRole("button", { name: "Create isolate" }).click();
  await expect(page.getByText("Identification pending")).toBeVisible({
    timeout: LONG_TIMEOUT,
  });

  await page.getByRole("button", { name: "Identify organism" }).click();
  await page.getByLabel("Organism").selectOption(organismId);
  await page.getByLabel("ID method").selectOption("MALDI_TOF");
  await page.getByLabel("ID confidence (%)").fill("99.5");
  await page.getByRole("button", { name: "Save identification" }).click();
  await expect(page.getByText("Identified", { exact: true })).toBeVisible({
    timeout: LONG_TIMEOUT,
  });
};
