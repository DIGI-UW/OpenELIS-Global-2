import LoginPage from "../pages/LoginPage";

const login = new LoginPage();

const NOTEBOOK_URL = "/NoteBookInstanceEditForm/16?mode=edit";
const API_BASE = "/OpenELIS-Global";
const shot = (name) =>
  cy.screenshot(`qc_evidence/${name}`, { capture: "fullPage" });
const clickReportingTab = (label) => {
  cy.get(".biorepository-reporting-page", { timeout: 30000 })
    .contains('[role="tab"], button', label, { timeout: 30000 })
    .click({ force: true });
};

describe("Biorepository QC final localhost validation", () => {
  beforeEach(() => {
    login.visit();
    login.enterUsername("admin");
    login.enterPassword("adminADMIN!");
    login.signIn();
    cy.url({ timeout: 30000 }).should("not.include", "/login");
    cy.visit(NOTEBOOK_URL);
  });

  it("validates QC workflow, reporting visibility, and exports in browser", () => {
    cy.intercept(
      "POST",
      `${API_BASE}/rest/biorepository/qc-inspection/generate-round`,
    ).as("generateRound");
    cy.intercept(
      "POST",
      `${API_BASE}/rest/biorepository/qc-inspection/bulk-apply`,
    ).as("applyQc");

    cy.contains("button, a, div", "Workflow", { timeout: 30000 }).click({
      force: true,
    });

    cy.contains(/6\.?\s*QC Inspection/, { timeout: 30000 }).click({
      force: true,
    });
    cy.get("#qc-include-inspected", { timeout: 30000 }).check({ force: true });
    shot("01_stage6_qc_page_loaded");

    cy.contains("button", "Generate Random QC Round", { timeout: 30000 })
      .should("be.visible")
      .as("generateButton");

    cy.request(
      `${API_BASE}/rest/biorepository/qc-inspection/storage-overview?includeInspected=true`,
    ).then((overviewResponse) => {
      const eligibleCount = overviewResponse.body?.counts?.eligibleSamples || 0;
      cy.log(`Eligible samples from API: ${eligibleCount}`);
      expect(eligibleCount).to.be.greaterThan(0);

      cy.get("@generateButton", { timeout: 30000 })
        .should("not.be.disabled")
        .click({ force: true });
      cy.wait("@generateRound").then((interception) => {
        expect(interception.response?.statusCode).to.eq(200);
        const responseSamples = interception.response?.body?.samples || [];
        expect(responseSamples.length).to.be.greaterThan(0);
        const generatedSampleCount = responseSamples.length;

        cy.get("@generateRound.all").then((calls) => {
          expect(calls.length).to.eq(1);
        });
        cy.contains(/QC batch .*generated from filtered storage scope/i, {
          timeout: 30000,
        }).should("be.visible");
        shot("02_generate_random_qc_round_success");
        cy.contains("button", "Show all eligible samples", {
          timeout: 30000,
        }).should("be.visible");
        cy.get(".sample-table-section table tbody tr", {
          timeout: 30000,
        }).should("have.length", generatedSampleCount);
        cy.contains("button", "Show all eligible samples").click({
          force: true,
        });
        cy.get(".sample-table-section table tbody tr").should(
          "have.length.greaterThan",
          generatedSampleCount - 1,
        );
      });
    });

    cy.get(".sample-table-section table tbody tr", { timeout: 30000 })
      .its("length")
      .should("be.greaterThan", 0);
    cy.contains("button", "Inspect", { timeout: 30000 }).should("be.visible");

    // PASS flow from real UI (Record Verification must save).
    cy.contains("button", "Inspect", { timeout: 30000 })
      .first()
      .click({ force: true });
    cy.contains("Individual QC Inspection", { timeout: 30000 }).should(
      "be.visible",
    );
    shot("03_single_inspect_modal_open");
    cy.get("#inspectorName").clear().type("Cypress Inspector");
    cy.contains("button", "Check All (Verify)").click({ force: true });
    cy.contains("button", "Record Verification").click({ force: true });
    cy.wait("@applyQc").then((interception) => {
      expect(interception.response?.statusCode).to.eq(200);
    });
    cy.contains("Individual QC Inspection").should("not.exist");
    shot("04_pass_recorded");

    // FAIL flow: create unresolved discrepancy (multi-sample, no correction action).
    cy.request(`${API_BASE}/rest/biorepository/qc-inspection/samples`).then(
      (sampleResponse) => {
        const samples = sampleResponse.body || [];
        const targetA = samples[0];
        const targetB = samples[1] || samples[0];
        expect(targetA, "sample available for unresolved FAIL").to.exist;
        cy.wrap(targetA.bioSampleId).as("unresolvedSampleId");
        const failPayload = {
          bioSampleIds: [targetA.bioSampleId, targetB.bioSampleId],
          inspectorName: "Cypress Inspector",
          inspectionDate: new Date().toISOString(),
          samplePresent: false,
          labelIntegrity: true,
          containerIntegrity: true,
          volumeAppearanceAcceptable: true,
          correctPosition: false,
          discrepancyType: "SAMPLE_MISSING",
          correctiveAction: "Sample not physically found during QC",
          remarks: "FAIL flow validation (unresolved)",
        };
        cy.request(
          "POST",
          `${API_BASE}/rest/biorepository/qc-inspection/bulk-apply`,
          failPayload,
        ).then((failResponse) => {
          expect(failResponse.status).to.eq(200);
        });
      },
    );

    cy.reload();
    cy.contains("button, a, div", "Workflow", { timeout: 30000 }).click({
      force: true,
    });
    cy.contains(/6\.?\s*QC Inspection/, { timeout: 30000 }).click({
      force: true,
    });
    cy.get("#qc-include-inspected").check({ force: true });
    cy.contains(/Correction required/i, { timeout: 30000 }).should(
      "be.visible",
    );
    shot("05_fail_recorded_unresolved_visible");

    // Re-open unresolved discrepancy later and complete correction.
    cy.contains("button", "Inspect", { timeout: 30000 })
      .first()
      .click({ force: true });
    cy.contains("Individual QC Inspection", { timeout: 30000 }).should(
      "be.visible",
    );
    cy.get("#inspectorName").clear().type("Cypress Inspector");
    cy.contains("button", "Clear All").click({ force: true });
    shot("06_correction_workflow_visible");
    cy.contains("button", /Cancel|Close/, { timeout: 30000 })
      .first()
      .click({ force: true });

    // Apply correction later (follow-up action) to prove delayed correction path.
    cy.get("@unresolvedSampleId").then((sampleId) => {
      const correctionPayload = {
        bioSampleIds: [sampleId],
        inspectorName: "Cypress Inspector",
        inspectionDate: new Date().toISOString(),
        samplePresent: false,
        labelIntegrity: true,
        containerIntegrity: true,
        volumeAppearanceAcceptable: true,
        correctPosition: false,
        discrepancyType: "SAMPLE_MISSING",
        correctiveAction: "Resolved during re-inspection",
        remarks: "Applying correction later",
        correctionActionType: "MARK_MISSING",
        correctionReason: "Confirmed unresolved sample after follow-up",
      };
      cy.request(
        "POST",
        `${API_BASE}/rest/biorepository/qc-inspection/bulk-apply`,
        correctionPayload,
      ).then((correctionResponse) => {
        expect(correctionResponse.status).to.eq(200);
      });
    });

    cy.reload();
    cy.contains("button, a, div", "Workflow", { timeout: 30000 }).click({
      force: true,
    });
    cy.contains(/6\.?\s*QC Inspection/, { timeout: 30000 }).click({
      force: true,
    });
    cy.get("#qc-include-inspected").check({ force: true });
    cy.get(".sample-table-section table tbody tr", { timeout: 30000 })
      .its("length")
      .should("be.gte", 2);
    shot("07_reopened_and_corrected_later");

    // Bulk inspect modal still opens from selected rows when table data exists.
    cy.get(".sample-table-section table tbody tr")
      .first()
      .within(() => {
        cy.get("input[type='checkbox']").first().check({ force: true });
      });
    cy.get(".sample-table-section table tbody tr")
      .eq(1)
      .within(() => {
        cy.get("input[type='checkbox']").first().check({ force: true });
      });
    cy.get(".sample-table-section table tbody input[type='checkbox']:checked", {
      timeout: 30000,
    })
      .its("length")
      .should("be.gte", 2);
    cy.get(".cds--batch-actions, .bx--batch-actions", { timeout: 30000 })
      .should("be.visible")
      .find("button")
      .then(($buttons) => {
        const buttons = Array.from($buttons);
        const button =
          buttons.find((el) =>
            /bulk inspect/i.test(
              `${el.textContent || ""} ${el.getAttribute("aria-label") || ""} ${
                el.getAttribute("title") || ""
              }`,
            ),
          ) ||
          buttons.find(
            (el) =>
              !/cancel|close/i.test(
                `${el.textContent || ""} ${el.getAttribute("aria-label") || ""} ${
                  el.getAttribute("title") || ""
                }`,
              ),
          ) ||
          buttons[buttons.length - 1];
        expect(button, "bulk action button").to.exist;
        cy.wrap(button).click({ force: true });
      });
    cy.contains("Bulk QC Inspection", { timeout: 30000 }).should("be.visible");
    shot("08_bulk_inspect_modal");
    cy.contains("button", /Cancel|Close/, { timeout: 30000 })
      .first()
      .click({ force: true });

    cy.contains(/7\.?\s*Reporting\s*&\s*Audit/, { timeout: 30000 }).click({
      force: true,
    });

    cy.contains(/Overview/i, { timeout: 30000 }).should("exist");
    cy.contains(/Detailed Metrics/i, { timeout: 30000 }).should("exist");
    cy.contains(/Audit/i, { timeout: 30000 }).should("exist");

    clickReportingTab(/Overview/i);
    cy.contains("button", /Export CSV/i, { timeout: 30000 }).should(
      "be.visible",
    );
    shot("09_reporting_overview_tab");

    clickReportingTab(/Detailed Metrics/i);
    cy.get("body", { timeout: 30000 }).should("contain.text", "Metrics");
    cy.get("body", { timeout: 30000 }).should(
      "contain.text",
      "Pending corrections",
    );
    cy.contains("button, h3, h4, h5, div", /QC History \(Completed Checks\)/i, {
      timeout: 30000,
    }).click({ force: true });
    cy.contains("QC Batch Summary", { timeout: 30000 }).should("be.visible");
    cy.contains("QC Batch ID", { timeout: 30000 }).should("be.visible");
    shot("10_reporting_detailed_metrics_tab");

    clickReportingTab(/Audit Trail/i);
    cy.contains(
      ".cds--tab-content:visible",
      /Chain of Custody Audit Trail|QC Outcome Audit Visibility/i,
      {
        timeout: 30000,
      },
    ).should("be.visible");
    cy.contains(".cds--tab-content:visible", "QC History by Batch ID", {
      timeout: 30000,
    }).should("be.visible");
    cy.contains(".cds--tab-content:visible", "QC Batch ID", {
      timeout: 30000,
    }).should("be.visible");
    shot("11_audit_trail_visibility");

    clickReportingTab(/Export Reports/i);
    cy.contains(/Dashboard Metrics Export/i, { timeout: 30000 }).should(
      "exist",
    );
    cy.contains(/Audit Trail Export/i, { timeout: 30000 }).should("exist");
    cy.contains("button, h3, h4, h5, div", /QC Batch Export/i, {
      timeout: 30000,
    }).click({ force: true });
    cy.contains(/Recent QC Batch IDs/i, { timeout: 30000 }).should(
      "be.visible",
    );
    shot("12_export_reports_ui");

    cy.request({
      url: `${API_BASE}/rest/biorepository/dashboard/export/csv`,
      encoding: "binary",
    }).then((csvExportResponse) => {
      expect(csvExportResponse.status).to.eq(200);
      expect(
        String(csvExportResponse.headers["content-type"] || ""),
      ).to.contain("text/csv");
    });

    cy.request({
      url: `${API_BASE}/rest/biorepository/dashboard/export/pdf`,
      encoding: "binary",
    }).then((pdfExportResponse) => {
      expect(pdfExportResponse.status).to.eq(200);
      expect(
        String(pdfExportResponse.headers["content-type"] || ""),
      ).to.contain("application/pdf");
    });

    cy.request({
      url: `${API_BASE}/rest/biorepository/dashboard/retrieval-stats?startDate=2026-99-99`,
      failOnStatusCode: false,
    }).then((invalidDateResponse) => {
      expect(invalidDateResponse.status).to.eq(400);
    });
  });
});
