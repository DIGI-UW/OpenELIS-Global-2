import { expect, type Page } from "@playwright/test";
import { AnalyzerListPage } from "../fixtures/analyzer-list";
import { AnalyzerSetupPage } from "../fixtures/analyzer-setup";
import { analyzerByName, type Analyzer } from "./analyzer-api";
import { API } from "./analyzer-profile-api";

export const GENEXPERT = {
  displayName: "Cepheid GeneXpert (ASTM Mode)",
  profileId: "cepheid-genexpert-astm",
  revision: 1,
} as const;

/**
 * Set up a GeneXpert on the shipped baseline profile through the setup screens:
 * its shipped mapping confirmed, its sender set, and the connection active.
 */
export async function activateShippedGeneXpert(
  page: Page,
  name: string,
  senderId: string,
): Promise<Analyzer> {
  const list = new AnalyzerListPage(page);
  const setup = new AnalyzerSetupPage(page);
  await list.goto();
  await list.clickAdd();
  await setup.expectOpen();
  await setup.selectProfile(GENEXPERT.displayName, GENEXPERT);
  await setup.fillName(name);
  await setup.selectLabUnit("Molecular Biology");
  await setup.continueToVerify();
  const mapping = (await (
    await page.request.get(
      `${API}/analyzer-types/${GENEXPERT.profileId}/mapping?revision=${GENEXPERT.revision}`,
    )
  ).json()) as { confirmation: { state: string } };
  if (mapping.confirmation.state !== "CURRENT") {
    const confirm = page.getByRole("button", {
      name: "Confirm mappings and control recognition",
    });
    await expect(confirm).toBeEnabled();
    await confirm.click();
    await expect(
      page.getByText("Mappings and control recognition confirmed"),
    ).toBeVisible();
  }
  await setup.continueToConnect();
  await setup.fillSenderId(senderId);
  await page.getByRole("button", { name: "Finish and activate" }).click();
  const analyzer = await analyzerByName(page, name, GENEXPERT.profileId);
  await expect(page.getByTestId(`analyzer-row-${analyzer.id}`)).toContainText(
    "Active",
  );
  return analyzer;
}
