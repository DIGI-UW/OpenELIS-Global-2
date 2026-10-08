import { randomUUID } from "node:crypto";
import { expect, test } from "../../../helpers/test-base";
import { AnalyzerSetupPage } from "../../../fixtures/analyzer-setup";
import { analyzerByName } from "../../../helpers/analyzer-api";
import {
  API,
  createProfile,
  numeric,
} from "../../../helpers/analyzer-profile-api";
import {
  deleteSiteProfileRevision,
  restartBridge,
} from "../../../helpers/bridge-container";
import {
  GENEXPERT,
  activateShippedAnalyzer,
} from "../../../helpers/analyzer-setup-flow";

test.describe("An analyzer whose type the Bridge has lost", () => {
  test("the operator resets it and sets it up again on an available type, keeping its name and history", async ({
    page,
  }) => {
    test.setTimeout(300_000);
    const run = randomUUID().slice(0, 8);
    const typeName = `Lost type ${run}`;
    const name = `Stranded analyzer ${run}`;

    // A site type whose one assay binds by LOINC, and an analyzer set up and active on it.
    const authored = await createProfile(page, typeName, [
      numeric("VIHX", "20447-9"),
    ]);
    const profileId = authored.profile.profileMeta.id;
    const analyzer = await activateShippedAnalyzer(
      page,
      { displayName: typeName, profileId, revision: 1 },
      name,
      { senderId: `GX-LOST-${run}` },
    );

    // That revision's file leaves the Bridge's data volume and the Bridge restarts.
    deleteSiteProfileRevision(profileId, 1);
    restartBridge();
    await expect
      .poll(
        async () => {
          const response = await page.request.get(
            `${API}/analyzer/analyzers/${analyzer.id}/activation-readiness`,
          );
          const readiness = (await response.json()) as {
            blockers?: Array<{ code: string }>;
          };
          return (readiness.blockers ?? []).some(
            (blocker) =>
              blocker.code ===
              "analyzer.connection.readiness.profileUnavailable",
          );
        },
        { timeout: 120_000 },
      )
      .toBe(true);

    // The connection step says the type is gone, and offers the reset.
    await page.goto(`/analyzers?setup=connect&analyzerId=${analyzer.id}`, {
      waitUntil: "domcontentloaded",
    });
    await expect(
      page.getByText(
        "which the Analyzer Bridge no longer has. Reset the analyzer type, then choose an available one",
        { exact: false },
      ),
    ).toBeVisible();
    await page.getByRole("button", { name: "Reset analyzer type" }).click();

    // Setup starts again at the instrument step; the analyzer keeps its name.
    const setup = new AnalyzerSetupPage(page);
    await expect(page).toHaveURL(
      (url) => url.searchParams.get("setup") === "instrument",
    );
    await expect(setup.nameInput).toHaveValue(name);
    await setup.selectProfile(GENEXPERT.displayName);
    await setup.continueToVerify();
    const confirm = page.getByRole("button", {
      name: "Confirm mappings and control recognition",
    });
    await expect(confirm).toBeEnabled();
    await confirm.click();
    await expect(
      page.getByText("Mappings and control recognition confirmed"),
    ).toBeVisible();
    await setup.continueToConnect();
    await page.getByRole("button", { name: "Finish and activate" }).click();

    // Same analyzer, now on an available type, active again.
    const again = await analyzerByName(page, name, GENEXPERT.profileId);
    expect(again.id).toBe(analyzer.id);
    await expect(page.getByTestId(`analyzer-row-${again.id}`)).toContainText(
      "Active",
    );
  });
});
