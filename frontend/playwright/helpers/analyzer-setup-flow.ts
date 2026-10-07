import { expect, type Page } from "@playwright/test";
import { AnalyzerListPage } from "../fixtures/analyzer-list";
import { AnalyzerSetupPage } from "../fixtures/analyzer-setup";
import { analyzerByName, type Analyzer } from "./analyzer-api";
import { API } from "./analyzer-profile-api";

/** A profile the Bridge ships, as the setup picker names it. */
export type ShippedProfile = {
  displayName: string;
  profileId: string;
  revision: number;
};

export const GENEXPERT: ShippedProfile = {
  displayName: "Cepheid GeneXpert (ASTM Mode)",
  profileId: "cepheid-genexpert-astm",
  revision: 1,
};

export const FLUOROCYCLER: ShippedProfile = {
  displayName: "Bruker FluoroCycler XT",
  profileId: "hain-fluorocycler-xt",
  revision: 1,
};

/** How the instrument reaches the Bridge: a sender on the shared listener, or a watched directory. */
export type Connection = { senderId: string } | { importDirectory: string };

/**
 * Set an analyzer up on a shipped baseline profile through the setup screens:
 * the assays named in `assaysOff` turned off, its shipped mapping confirmed, its
 * connection set, and the connection active.
 */
export async function activateShippedAnalyzer(
  page: Page,
  profile: ShippedProfile,
  name: string,
  connection: Connection,
  assaysOff: string[] = [],
): Promise<Analyzer> {
  const list = new AnalyzerListPage(page);
  const setup = new AnalyzerSetupPage(page);
  await list.goto();
  await list.clickAdd();
  await setup.expectOpen();
  await setup.selectProfile(profile.displayName, profile);
  await setup.fillName(name);
  await setup.selectLabUnit("Molecular Biology");
  await setup.continueToVerify(assaysOff);
  const mapping = (await (
    await page.request.get(
      `${API}/analyzer-types/${profile.profileId}/mapping?revision=${profile.revision}`,
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
  if ("senderId" in connection) {
    await setup.fillSenderId(connection.senderId);
  } else {
    await setup.fillImportDirectory(connection.importDirectory);
  }
  await page.getByRole("button", { name: "Finish and activate" }).click();
  const analyzer = await analyzerByName(page, name, profile.profileId);
  await expect(page.getByTestId(`analyzer-row-${analyzer.id}`)).toContainText(
    "Active",
  );
  return analyzer;
}

/** A GeneXpert on the shipped baseline profile, active on the shared listener. */
export const activateShippedGeneXpert = (
  page: Page,
  name: string,
  senderId: string,
  assaysOff: string[] = [],
) => activateShippedAnalyzer(page, GENEXPERT, name, { senderId }, assaysOff);
