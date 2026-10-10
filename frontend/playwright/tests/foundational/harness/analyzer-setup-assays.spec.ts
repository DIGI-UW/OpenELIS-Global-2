import type { Page } from "@playwright/test";
import { randomInt, randomUUID } from "node:crypto";
import { expect, test } from "../../../helpers/test-base";
import { AnalyzerListPage } from "../../../fixtures/analyzer-list";
import { AnalyzerSetupPage } from "../../../fixtures/analyzer-setup";
import { analyzerByName, worklistFor } from "../../../helpers/analyzer-api";
import { seedNumericTests } from "../../../helpers/analyzer-catalog-api";
import { createClinicalOrder } from "../../../helpers/analyzer-clinical-order";
import { sendGeneXpertAstm } from "../../../helpers/analyzer-native-traffic";
import { createProfile, numeric } from "../../../helpers/analyzer-profile-api";

const continueToConnect = (page: Page) =>
  page.getByRole("button", { name: "Continue to Connect" });

/** Picks a catalog test for one analyzer code in the mapping editor inside Verify. */
async function mapTo(page: Page, code: string, testName: string) {
  const picker = page.getByRole("combobox", {
    name: `OpenELIS test for ${code}`,
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
    const base = randomInt(1_000_000, 9_000_000);
    const loinc = {
      on: `${base}-1`,
      off: `${base + 1}-2`,
      extra: `${base + 2}-3`,
    };
    const [on, off, extra] = await seedNumericTests(
      page,
      run,
      [
        { name: `E2E On ${run}`, loinc: loinc.on },
        { name: `E2E Off ${run}`, loinc: loinc.off },
        { name: `E2E Extra ${run}`, loinc: loinc.extra },
      ],
      { testSection: "Molecular Biology", sampleType: "Sputum" },
    );
    const displayName = `Held bench ${run}`;
    const senderId = `GX-${run}`;
    const profile = await createProfile(page, displayName, [
      numeric("E2E-ON", loinc.on),
      numeric("E2E-OFF", loinc.off),
    ]);

    // The lab runs one assay on this instrument and turns the other off.
    const list = new AnalyzerListPage(page);
    const setup = new AnalyzerSetupPage(page);
    await list.goto();
    await list.clickAdd();
    await setup.expectOpen();
    await setup.selectProfile(displayName);
    await setup.fillName(displayName);
    await setup.selectLabUnit("Molecular Biology");
    await setup.continueToAssays();
    const assay = (code: string) => page.getByTestId(`analyzer-assay-${code}`);
    await expect(assay("E2E-OFF").getByRole("checkbox")).toBeChecked();
    await assay("E2E-OFF").locator("label").first().click();
    await expect(assay("E2E-OFF").getByRole("checkbox")).not.toBeChecked();
    await page.getByRole("button", { name: "Continue to Verify" }).click();
    await expect(page).toHaveURL(
      (url) => url.searchParams.get("setup") === "verify",
    );
    await confirmMapping(page);
    await setup.continueToConnect();
    await setup.fillSenderId(senderId);
    await page.getByRole("button", { name: "Finish and activate" }).click();
    const analyzer = await analyzerByName(
      page,
      displayName,
      profile.profile.profileMeta.id,
    );
    await expect(page.getByTestId(`analyzer-row-${analyzer.id}`)).toContainText(
      "Active",
    );

    // One result per code: an assay that is on, one that is off, one nobody declared.
    const orders = {
      on: await createClinicalOrder(page, {
        testIds: [on.id],
        specimenName: "Sputum",
      }),
      off: await createClinicalOrder(page, {
        testIds: [off.id],
        specimenName: "Sputum",
      }),
      extra: await createClinicalOrder(page, {
        testIds: [extra.id],
        specimenName: "Sputum",
      }),
    };
    const send = (accession: string, code: string) =>
      sendGeneXpertAstm(
        page.request,
        analyzer.bridgeConnectionId,
        accession,
        code,
        "1250",
        senderId,
      );
    await send(orders.on.accession, "E2E-ON");
    await send(orders.off.accession, "E2E-OFF");
    await send(orders.extra.accession, "E2E-EXTRA");

    const issueFor = async (accession: string) =>
      (await worklistFor(page, analyzer.id, accession))[0]?.importIssueReason;
    await expect.poll(() => issueFor(orders.on.accession)).toBeFalsy();
    await expect
      .poll(
        async () =>
          (await worklistFor(page, analyzer.id, orders.on.accession)).length,
      )
      .toBe(1);
    await expect
      .poll(() => issueFor(orders.off.accession))
      .toBe("assay_not_enabled");
    await expect
      .poll(() => issueFor(orders.extra.accession))
      .toBe("unknown_analyzer_test");

    // The code nobody declared is mapped from its held row, as that analyzer's own row.
    await page.goto(`/AnalyzerResults?id=${analyzer.id}`, {
      waitUntil: "domcontentloaded",
    });
    const extraRow = (
      await worklistFor(page, analyzer.id, orders.extra.accession)
    )[0];
    await page
      .getByTestId(`held-analyzer-result-${extraRow.id}`)
      .getByRole("link", { name: "Review analyzer mapping" })
      .click();
    await mapTo(page, "E2E-EXTRA", extra.name);
    await saveMapping(page);
    await expect(page.getByText("Edited", { exact: true })).toBeVisible();
    await confirmMapping(page);
    await page
      .getByRole("button", { name: "Apply mappings and retry held results" })
      .click();
    await expect(
      page.getByText(
        "Current mappings applied to this analyzer. Eligible held results were retried.",
      ),
    ).toBeVisible();
    await expect.poll(() => issueFor(orders.extra.accession)).toBeFalsy();
    expect(
      (await worklistFor(page, analyzer.id, orders.extra.accession))[0].testId,
    ).toBe(extra.id);

    // The held row for the assay that is off says so and leads to the Assays step.
    await page.goto(`/AnalyzerResults?id=${analyzer.id}`, {
      waitUntil: "domcontentloaded",
    });
    const offRow = (
      await worklistFor(page, analyzer.id, orders.off.accession)
    )[0];
    const heldOff = page.getByTestId(`held-analyzer-result-${offRow.id}`);
    await expect(heldOff).toContainText(
      "This assay is off in the analyzer's setup",
    );
    await heldOff.getByRole("link", { name: "Turn the assay on" }).click();
    await expect(page).toHaveURL(
      (url) => url.searchParams.get("setup") === "assays",
    );
    await assay("E2E-OFF").locator("label").first().click();
    await expect(assay("E2E-OFF").getByRole("checkbox")).toBeChecked();
    await page.getByRole("button", { name: "Continue to Verify" }).click();
    await confirmMapping(page);
    await setup.continueToConnect();
    await expect.poll(() => issueFor(orders.off.accession)).toBeFalsy();
    expect(
      (await worklistFor(page, analyzer.id, orders.off.accession))[0].testId,
    ).toBe(off.id);
  });
});
