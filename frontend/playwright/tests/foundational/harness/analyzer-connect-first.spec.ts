import { randomUUID } from "node:crypto";
import { expect as baseExpect, test } from "../../../helpers/test-base";
import { AnalyzerListPage } from "../../../fixtures/analyzer-list";
import { AnalyzerSetupPage } from "../../../fixtures/analyzer-setup";
import {
  analyzerByName,
  worklistFor,
  type WorklistRow,
} from "../../../helpers/analyzer-api";
import { activeTestId } from "../../../helpers/analyzer-catalog-api";
import { createClinicalOrder } from "../../../helpers/analyzer-clinical-order";
import { sendGeneXpertFixture } from "../../../helpers/analyzer-native-traffic";
import { acceptAll, savedValue } from "../../../helpers/analyzer-review";
import { GENEXPERT } from "../../../helpers/shipped-profiles";

const expect = baseExpect.configure({ timeout: 20000 });
type SetupRow = WorklistRow & { readOnly: boolean };

test(
  "receives typed setup evidence before mapping approval and keeps it isolated after activation and retransmission",
  {
    tag: ["@astm", "@evidence", "@setup"],
  },
  async ({ page }, testInfo) => {
    test.setTimeout(180000);
    const run = randomUUID().slice(0, 8);
    const name = `Connect first ${run}`;
    const sender = `S1573-${run}`;
    const list = new AnalyzerListPage(page);
    const setup = new AnalyzerSetupPage(page);
    const testId = await activeTestId(page, "HIV-1 Viral Load", "Plasma");
    // A real matching order makes this stronger than holding an unknown accession.
    const setupOrder = await createClinicalOrder(page, {
      testIds: [testId],
      specimenName: "Plasma",
    });
    await list.goto();
    await list.clickAdd();
    await setup.selectProfile(GENEXPERT.displayName, GENEXPERT);
    await setup.fillName(name);
    await setup.selectLabUnit(GENEXPERT.labUnit);
    await setup.continueToConnect();
    await setup.fillSenderId(sender);
    const analyzer = await analyzerByName(page, name, GENEXPERT.profileId);
    const reception = page.getByTestId("analyzer-setup-reception");
    await reception
      .getByRole("button", { name: "Start receiving setup samples" })
      .click();
    await expect(
      reception.getByRole("button", { name: "Stop setup reception" }),
    ).toBeVisible();
    expect((await analyzerByName(page, name, GENEXPERT.profileId)).status).toBe(
      "VALIDATION",
    );
    await sendGeneXpertFixture(
      page,
      analyzer.id,
      setupOrder.accession,
      { assay: "hivvl", outcome: "quantified" },
      sender,
      {},
      undefined,
      "20261028160000",
    );
    const captured = () =>
      worklistFor<SetupRow>(page, analyzer.id, setupOrder.accession);
    await expect
      .poll(async () => (await captured()).map((row) => row.importIssueReason))
      .toEqual(["setup_capture"]);
    const original = (await captured())[0];
    expect(original).toMatchObject({
      readOnly: true,
      testId: null,
      rawResultValue: "1009.64",
    });
    await reception
      .getByRole("button", { name: "Check for setup samples" })
      .click();
    await expect(
      reception.getByText(/received values? (?:is|are) available in Verify/),
    ).toBeVisible();
    await page.screenshot({
      path: testInfo.outputPath("connect-setup-reception.png"),
      fullPage: true,
    });
    await reception
      .getByRole("button", { name: "Stop setup reception" })
      .click();
    await expect(
      reception.getByRole("button", { name: "Start receiving setup samples" }),
    ).toBeVisible();
    expect((await captured())[0].id).toBe(original.id);
    await reception
      .getByRole("button", { name: "Start receiving setup samples" })
      .click();
    await expect(
      reception.getByRole("button", { name: "Stop setup reception" }),
    ).toBeVisible();
    await setup.continueToVerify();
    const code = page
      .getByRole("button")
      .filter({ has: page.getByText("HIVVL", { exact: true }) });
    if ((await code.getAttribute("aria-expanded")) === "false")
      await code.click();
    const received = page.getByRole("table").filter({ hasText: "1009.64" });
    await expect(received).toContainText("Number");
    await expect(received).toContainText("copies/mL");
    const confirm = page.getByRole("button", {
      name: "Confirm mappings",
      exact: true,
    });
    if (await confirm.isVisible()) {
      await expect(confirm).toBeEnabled();
      await confirm.click();
      await expect(page.getByText("Mappings confirmed")).toBeVisible();
    }
    await page.screenshot({
      path: testInfo.outputPath("verify-typed-setup-evidence.png"),
      fullPage: true,
    });
    await page.getByRole("button", { name: "Finish and activate" }).click();
    await expect(setup.surface).not.toBeVisible();
    expect((await analyzerByName(page, name, GENEXPERT.profileId)).status).toBe(
      "ACTIVE",
    );
    await sendGeneXpertFixture(
      page,
      analyzer.id,
      setupOrder.accession,
      { assay: "hivvl", outcome: "quantified" },
      sender,
      {},
      undefined,
      "20261028160000",
    );
    expect(await captured()).toEqual([
      expect.objectContaining({
        id: original.id,
        readOnly: true,
        importIssueReason: "setup_capture",
        testId: null,
      }),
    ]);
    await page.goto(`/AnalyzerResults?id=${analyzer.id}`);
    const held = page.getByTestId(`held-analyzer-result-${original.id}`);
    await expect(held).toContainText("Setup sample");
    await expect(held.getByRole("checkbox")).toHaveCount(0);
    await page.screenshot({
      path: testInfo.outputPath("setup-remains-held-after-activation.png"),
      fullPage: true,
    });
    const routineOrder = await createClinicalOrder(page, {
      testIds: [testId],
      specimenName: "Plasma",
    });
    await sendGeneXpertFixture(
      page,
      analyzer.id,
      routineOrder.accession,
      { assay: "hivvl", outcome: "quantified" },
      sender,
    );
    await expect
      .poll(async () =>
        (await worklistFor(page, analyzer.id, routineOrder.accession)).map(
          (row) => row.testId,
        ),
      )
      .toEqual([testId]);
    await acceptAll(page, analyzer, [routineOrder.accession]);
    const saved = await savedValue(page, routineOrder, testId);
    expect(saved).not.toBe("");
    // This checks the setup/routine boundary. Numeric reporting precision is tracked in OGC-1574.
    expect((await captured())[0]).toMatchObject({
      id: original.id,
      readOnly: true,
      importIssueReason: "setup_capture",
    });
    await testInfo.attach("setup-and-routine-evidence", {
      body: JSON.stringify(
        {
          analyzer,
          setupAccession: setupOrder.accession,
          setupRow: original,
          routineAccession: routineOrder.accession,
          savedRoutineValue: saved,
        },
        null,
        2,
      ),
      contentType: "application/json",
    });
  },
);
