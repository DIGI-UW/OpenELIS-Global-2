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
import { createDemoPresentation } from "../../../helpers/demo-presentation";

test.describe("An analyzer whose type the Bridge has lost", () => {
  test("the operator resets it and sets it up again on an available type, keeping its name and history", async ({
    page,
  }, testInfo) => {
    const demo = createDemoPresentation(page, testInfo);
    await demo.intro(
      "An analyzer whose type the Bridge has lost",
      "The operator resets it and sets it up again on an available type. It stays the same analyzer, with its name.",
    );
    const run = randomUUID().slice(0, 8);
    const typeName = `Lost type ${run}`;
    const name = `Stranded analyzer ${run}`;

    // A site type whose one assay binds by LOINC, and an analyzer set up and active on it.
    await demo.caption(
      "Off screen: a site's own analyzer type is published. An analyzer is set up and activated on it.",
    );
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
    await demo.caption(
      "Off screen: the type's file leaves the Bridge's data volume and the Bridge restarts. OpenELIS notices the type is gone.",
    );
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
    const lost = page.getByText(
      "which the Analyzer Bridge no longer has. Reset the analyzer type, then choose an available one",
      { exact: false },
    );
    await expect(lost).toBeVisible();
    await demo.caption(
      "The analyzer's Connect step says its type is gone and offers a reset.",
    );
    await demo.highlight(lost);
    await page.getByRole("button", { name: "Reset analyzer type" }).click();

    // Setup starts again at the instrument step; the analyzer keeps its name.
    const setup = new AnalyzerSetupPage(page);
    await expect(page).toHaveURL(
      (url) => url.searchParams.get("setup") === "instrument",
    );
    await expect(setup.nameInput).toHaveValue(name);
    await demo.caption(
      "Setup starts again at the instrument step, with the analyzer's name kept. The operator picks the shipped GeneXpert type.",
    );
    await setup.selectProfile(GENEXPERT.displayName, GENEXPERT);
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
    const row = page.getByTestId(`analyzer-row-${again.id}`);
    await expect(row).toContainText("Active");
    await demo.highlight(row);
    await demo.verified(
      "The same analyzer is active again on an available type",
      `${name} kept its record; it now runs on ${GENEXPERT.displayName}.`,
    );
  });
});
