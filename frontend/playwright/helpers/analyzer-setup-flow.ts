import { expect, type Page } from "@playwright/test";
import { AnalyzerListPage } from "../fixtures/analyzer-list";
import { AnalyzerSetupPage } from "../fixtures/analyzer-setup";
import { analyzerByName, type Analyzer } from "./analyzer-api";
import { API } from "./analyzer-profile-api";
import { BC5380, GENEXPERT, type ShippedProfile } from "./shipped-profiles";

/** How the instrument reaches the Bridge: a sender on the shared listener, or a watched directory. */
export type Connection = { senderId: string } | { importDirectory: string };

/** The assays turned off in the Assays step. */
export type AssayOptions = {
  assaysOff?: string[];
};

/**
 * Set an analyzer up on a shipped baseline profile through the setup screens:
 * the assays named in `assaysOff` turned off, its shipped mapping confirmed,
 * its connection set, and the connection active.
 */
export async function activateShippedAnalyzer(
  page: Page,
  profile: ShippedProfile,
  name: string,
  connection: Connection,
  assays: AssayOptions = {},
): Promise<Analyzer> {
  const list = new AnalyzerListPage(page);
  const setup = new AnalyzerSetupPage(page);
  await list.goto();
  await list.clickAdd();
  await setup.expectOpen();
  await setup.selectProfile(profile.displayName, profile);
  await setup.fillName(name);
  await setup.selectLabUnit(profile.labUnit);
  await setup.continueToVerify(assays);
  const mapping = (await (
    await page.request.get(
      `${API}/analyzer-types/${profile.profileId}/mapping?revision=${profile.revision}`,
    )
  ).json()) as { confirmation: { state: string } };
  if (mapping.confirmation.state !== "CURRENT") {
    const confirm = page.getByRole("button", {
      name: "Confirm mapping",
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

/** A Mindray BC-5380 on the shipped baseline profile, active on the shared MLLP listener. */
export const activateShippedBc5380 = (
  page: Page,
  name: string,
  senderId: string,
  options: AssayOptions = {},
) => activateShippedAnalyzer(page, BC5380, name, { senderId }, options);

/** A GeneXpert on the shipped baseline profile, active on the shared listener. */
export const activateShippedGeneXpert = (
  page: Page,
  name: string,
  senderId: string,
  options: AssayOptions = {},
) => activateShippedAnalyzer(page, GENEXPERT, name, { senderId }, options);
