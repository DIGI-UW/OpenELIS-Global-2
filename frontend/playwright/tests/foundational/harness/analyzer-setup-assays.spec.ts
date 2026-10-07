import type { Page } from "@playwright/test";
import { randomInt, randomUUID } from "node:crypto";
import { expect, test } from "../../../helpers/test-base";
import { AnalyzerListPage } from "../../../fixtures/analyzer-list";
import { AnalyzerSetupPage } from "../../../fixtures/analyzer-setup";
import { worklistFor, type WorklistRow } from "../../../helpers/analyzer-api";
import {
  activeTestId,
  seedNumericTests,
} from "../../../helpers/analyzer-catalog-api";
import { createClinicalOrder } from "../../../helpers/analyzer-clinical-order";
import { sendGeneXpertFixture } from "../../../helpers/analyzer-native-traffic";
import { activateShippedGeneXpert } from "../../../helpers/analyzer-setup-flow";
import { createProfile, numeric } from "../../../helpers/analyzer-profile-api";

const continueToConnect = (page: Page) =>
  page.getByRole("button", { name: "Continue to Connect" });

/** Picks a catalog test for one analyzer code in the mapping editor inside Verify. */
async function mapTo(page: Page, code: string, testName: string) {
  const picker = page.getByRole("combobox", {
    name: `OpenELIS test for ${code}`,
    exact: true,
  });
  await picker.click();
  await picker.fill(testName);
  await page
    .getByRole("option", { name: new RegExp(testName) })
    .first()
    .click();
}

async function saveMapping(page: Page) {
  await page.getByRole("button", { name: "Save mapping" }).click();
  await page.getByRole("button", { name: "Save changes" }).click();
  await expect(page.getByText("Mappings saved")).toBeVisible();
}

async function confirmMapping(page: Page) {
  const confirm = page.getByRole("button", {
    name: "Confirm mappings and control recognition",
  });
  await expect(confirm).toBeEnabled();
  await confirm.click();
  await expect(
    page.getByText("Mappings and control recognition confirmed"),
  ).toBeVisible();
}

test.describe("Assays step on a populated catalog", () => {
  test("turns on what the catalog can bind, maps the rest in Verify, and holds Continue until every assay that is on is mapped", async ({
    page,
  }) => {
    const run = randomUUID().slice(0, 6);
    const base = randomInt(1_000_000, 9_000_000);
    const loinc = {
      solo: `${base}-1`,
      twin: `${base + 1}-2`,
      none: `${base + 2}-3`,
    };
    const [solo, twinA, twinB] = await seedNumericTests(
      page,
      run,
      [
        { name: `E2E Solo ${run}`, loinc: loinc.solo },
        { name: `E2E Twin A ${run}`, loinc: loinc.twin },
        { name: `E2E Twin B ${run}`, loinc: loinc.twin },
      ],
      { testSection: "Molecular Biology", sampleType: "Sputum" },
    );
    const displayName = `Assay bench ${run}`;
    await createProfile(page, displayName, [
      numeric("E2E-SOLO", loinc.solo),
      numeric("E2E-TWIN", loinc.twin),
      numeric("E2E-NONE", loinc.none),
    ]);

    const list = new AnalyzerListPage(page);
    const setup = new AnalyzerSetupPage(page);
    await list.goto();
    await list.clickAdd();
    await setup.expectOpen();
    await setup.selectProfile(displayName);
    await setup.fillName(displayName);
    await setup.selectLabUnit("Molecular Biology");
    await setup.continueToAssays();

    // The catalog binds one, finds two candidates for another and none for the third.
    await expect(page.getByText("2 of 3 assays on")).toBeVisible();
    const assay = (code: string) => page.getByTestId(`analyzer-assay-${code}`);
    await expect(assay("E2E-SOLO")).toContainText(`Matches ${solo.name}`);
    await expect(assay("E2E-SOLO").getByRole("checkbox")).toBeChecked();
    await expect(assay("E2E-TWIN")).toContainText(
      "Several tests in this lab's catalog match",
    );
    await expect(assay("E2E-TWIN").getByRole("checkbox")).toBeChecked();
    await expect(assay("E2E-NONE")).toContainText(
      "No test in this lab's catalog",
    );
    await expect(assay("E2E-NONE").getByRole("checkbox")).not.toBeChecked();

    // Verify lists the assays that are on, never the one that is off.
    await page.getByRole("button", { name: "Continue to Verify" }).click();
    await expect(page).toHaveURL(
      (url) => url.searchParams.get("setup") === "verify",
    );
    await expect(
      page.getByText(
        "1 record or value of the assays that are on still need mapping.",
        { exact: false },
      ),
    ).toBeVisible();
    await expect(continueToConnect(page)).toBeDisabled();
    await expect(
      page.getByText(
        "Not mapped: several tests in this lab's catalog carry its code. Choose one.",
      ),
    ).toBeVisible();
    await expect(
      page.getByRole("combobox", { name: "OpenELIS test for E2E-NONE" }),
    ).toHaveCount(0);

    // Mapping the one that needs it still leaves the gate shut until it is confirmed.
    await mapTo(page, "E2E-TWIN", twinA.name);
    await saveMapping(page);
    await expect(continueToConnect(page)).toBeDisabled();
    await confirmMapping(page);
    await expect(continueToConnect(page)).toBeEnabled();

    // Turning the third assay on brings it into Verify and shuts the gate again.
    await page.getByRole("button", { name: "Edit Assays" }).click();
    await assay("E2E-NONE").locator("label").first().click();
    await expect(assay("E2E-NONE").getByRole("checkbox")).toBeChecked();
    await expect(page.getByText("3 of 3 assays on")).toBeVisible();
    await page.getByRole("button", { name: "Continue to Verify" }).click();
    await expect(
      page.getByText(
        "Not mapped: no test in this lab's catalog carries its code.",
      ),
    ).toBeVisible();
    await expect(continueToConnect(page)).toBeDisabled();

    await mapTo(page, "E2E-NONE", twinB.name);
    await saveMapping(page);
    await confirmMapping(page);
    await expect(continueToConnect(page)).toBeEnabled();
    await continueToConnect(page).click();
    await expect(page).toHaveURL(
      (url) => url.searchParams.get("setup") === "connect",
    );
  });
});

test.describe("Results the mapping does not cover", () => {
  test("a result for an assay that is off and one under a code the profile does not declare are held, then recover once the operator maps them", async ({
    page,
  }) => {
    test.setTimeout(240_000);
    const run = randomUUID().slice(0, 6);
    const senderId = `GX-${run}`;
    const specimen = "Nasopharyngeal Swab";

    // The lab turns Influenza B off in setup.
    const analyzer = await activateShippedGeneXpert(
      page,
      `Held bench ${run}`,
      senderId,
      ["FLUB"],
    );
    const tests = {
      sars: await activeTestId(page, "SARS-CoV-2 PCR", specimen),
      fluA: await activeTestId(page, "Influenza A PCR", specimen),
      fluB: await activeTestId(page, "Influenza B PCR", specimen),
      rsv: await activeTestId(page, "RSV PCR", specimen),
    };
    const orderPanel = () =>
      createClinicalOrder(page, {
        testIds: Object.values(tests),
        specimenName: specimen,
      });
    const panel = { assay: "cov-flu-rsv-plus", outcome: "all-positive" };

    type Row = WorklistRow & { componentId?: string | null };
    const own = async (accession: string, code: string) =>
      (await worklistFor<Row>(page, analyzer.id, accession)).find(
        (row) => row.rawTestCode === code && !row.componentId,
      );

    // A panel result: Influenza A lands, Influenza B is held because its assay is off.
    const assayOff = await orderPanel();
    await sendGeneXpertFixture(
      page.request,
      analyzer.bridgeConnectionId,
      assayOff.accession,
      panel,
      senderId,
    );
    await expect
      .poll(async () => (await own(assayOff.accession, "FLUA"))?.testId)
      .toBe(tests.fluA);
    expect(
      (await own(assayOff.accession, "FLUA"))?.importIssueReason,
    ).toBeFalsy();
    await expect
      .poll(
        async () => (await own(assayOff.accession, "FLUB"))?.importIssueReason,
      )
      .toBe("assay_not_enabled");

    // The held row for the assay that is off says so and leads to the Assays step.
    await page.goto(`/AnalyzerResults?id=${analyzer.id}`, {
      waitUntil: "domcontentloaded",
    });
    const fluBRow = (await own(assayOff.accession, "FLUB"))!;
    const heldOff = page.getByTestId(`held-analyzer-result-${fluBRow.id}`);
    await expect(heldOff).toContainText(
      "This assay is off in the analyzer's setup",
    );
    await heldOff.getByRole("link", { name: "Turn the assay on" }).click();
    await expect(page).toHaveURL(
      (url) => url.searchParams.get("setup") === "assays",
    );
    const fluBAssay = page.getByTestId("analyzer-assay-FLUB");
    await fluBAssay.locator("label").first().click();
    await expect(fluBAssay.getByRole("checkbox")).toBeChecked();
    await page.getByRole("button", { name: "Continue to Verify" }).click();
    await confirmMapping(page);
    await new AnalyzerSetupPage(page).continueToConnect();
    await expect
      .poll(
        async () => (await own(assayOff.accession, "FLUB"))?.importIssueReason,
      )
      .toBeFalsy();
    expect((await own(assayOff.accession, "FLUB"))?.testId).toBe(tests.fluB);

    // The instrument sends RSV as RSVX, a code the profile does not declare.
    const undeclared = await orderPanel();
    await sendGeneXpertFixture(
      page.request,
      analyzer.bridgeConnectionId,
      undeclared.accession,
      panel,
      senderId,
      { RSV: "RSVX" },
    );
    await expect
      .poll(
        async () =>
          (await own(undeclared.accession, "RSVX"))?.importIssueReason,
      )
      .toBe("unknown_analyzer_test");

    // The test and its answer are mapped from the held row, as that analyzer's own row.
    await page.goto(`/AnalyzerResults?id=${analyzer.id}`, {
      waitUntil: "domcontentloaded",
    });
    const rsvRow = (await own(undeclared.accession, "RSVX"))!;
    await page
      .getByTestId(`held-analyzer-result-${rsvRow.id}`)
      .getByRole("link", { name: "Review analyzer mapping" })
      .click();
    await mapTo(page, "RSVX", "RSV PCR");
    // Its answer is mapped to the test's own (the first Positive is the test's, the second its RSV component's).
    await page
      .getByRole("combobox", {
        name: "OpenELIS result for POSITIVE",
        exact: true,
      })
      .first()
      .click();
    await page
      .getByRole("option", { name: "Positive", exact: true })
      .first()
      .click();
    await saveMapping(page);
    await expect(
      page.getByText("Edited", { exact: true }).first(),
    ).toBeVisible();
    await confirmMapping(page);
    await page
      .getByRole("button", { name: "Apply mappings and retry held results" })
      .click();
    await expect(
      page.getByText(
        "Current mappings applied to this analyzer. Eligible held results were retried.",
      ),
    ).toBeVisible();
    await expect
      .poll(
        async () =>
          (await own(undeclared.accession, "RSVX"))?.importIssueReason,
      )
      .toBeFalsy();
    expect((await own(undeclared.accession, "RSVX"))?.testId).toBe(tests.rsv);
  });
});
