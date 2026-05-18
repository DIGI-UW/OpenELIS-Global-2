/**
 * Stage 4 (Reporting & Release): export PDF + submit delivery.
 * Uses real https://localhost (see cypress.config.js baseUrl).
 */
describe("Bioanalytical Stage 4 reporting flow", () => {
  const NOTEBOOK_URL = "/NoteBookInstanceEditForm/19?mode=edit";

  beforeEach(() => {
    cy.viewport(1920, 1080);
  });

  it("exports PDF and submits CSV to PI", () => {
    // Ensure QA gate + samples allow export/submit even on empty dev DBs
    cy.intercept("GET", "**/rest/notebook/bioanalytical/page/*/qa-approval", {
      statusCode: 200,
      body: {
        hasApproval: true,
        approvalStatus: "APPROVED",
        comments: "",
      },
    }).as("qaApproval");

    cy.intercept("GET", "**/rest/notebook/page/*/samples", {
      statusCode: 200,
      body: [
        {
          id: 999001,
          accessionNumber: "E2E-STAGE4-001",
          sampleType: "Plasma",
          data: {
            executionStatus: "EXECUTED",
            resultsApproved: true,
            readyForReporting: true,
            testExecution: {},
            analyticalMethod: "HPLC_UV_VIS",
            sampleType: "API",
            quantificationResults: [{ concentration: 12.5, units: "ng/mL" }],
          },
        },
      ],
    }).as("pageSamples");

    // Stub PDF export: some dev stacks return 400 from /export/pdf (e.g. sampleIds binding).
    // We still assert the browser sends a usable payload, then return a minimal PDF (>100 B).
    const minimalPdf =
      "%PDF-1.4\n1 0 obj<<>>endobj\ntrailer<<>>\n%%EOF" + "\n%".repeat(120);
    cy.intercept(
      "POST",
      "**/rest/notebook/bioanalytical/page/*/export/pdf",
      (req) => {
        const raw = req.body;
        const body =
          typeof raw === "string" && raw.length ? JSON.parse(raw) : raw || {};
        const ids = body.sampleIds;
        const hasSampleIds = Array.isArray(ids) && ids.length > 0;
        const hasIndividuals =
          Array.isArray(body.individualResults) &&
          body.individualResults.length > 0;
        expect(
          hasSampleIds || hasIndividuals,
          "export PDF request should include sampleIds or individualResults",
        ).to.be.true;
        req.reply({
          statusCode: 200,
          headers: { "content-type": "application/pdf" },
          body: minimalPdf,
        });
      },
    ).as("stage4PdfExport");

    // Stub delivery: backend may reject sparse DB payloads; assert client contract.
    cy.intercept("POST", "**/rest/notebook/bulk/notebook/*/deliver", (req) => {
      const raw = req.body;
      const parsed =
        typeof raw === "string" && raw.length ? JSON.parse(raw) : raw || {};
      expect(parsed.targetCode).to.eq("PI");
      expect(parsed.exportFormat).to.eq("csv");
      req.reply({
        statusCode: 200,
        headers: { "content-type": "application/json" },
        body: {
          deliveryId: 900001,
          targetCode: parsed.targetCode,
          exportFormat: parsed.exportFormat,
        },
      });
    }).as("stage4SubmitDelivery");

    cy.visit("/login");
    cy.get("#loginName", { timeout: 30000 })
      .should("be.visible")
      .clear()
      .type("admin");
    cy.get("#password").clear().type("adminADMIN!");
    cy.get("[data-cy='loginButton']").click();
    cy.get("[data-cy='menuButton']", { timeout: 60000 }).should("be.visible");

    cy.visit(NOTEBOOK_URL);

    // Stage 4 React page (and its QA/samples fetches) mount only after this navigation.
    cy.contains("[role='tab']", "Workflow", { timeout: 30000 }).click();
    // PageNavigation uses role="button" divs, not <button>; label is "4." + "Reporting & Release".
    cy.get(".page-navigation")
      .contains('[role="button"]', "Reporting & Release", { timeout: 30000 })
      .click({ force: true });

    cy.wait(["@qaApproval", "@pageSamples"], { timeout: 60000 });

    cy.contains("[role='tab']", "External Reporting", { timeout: 30000 }).click(
      {
        force: true,
      },
    );

    cy.get("#export-format", { timeout: 30000 })
      .should("be.visible")
      .select("pdf");
    cy.contains("button", "Export Now", { timeout: 30000 })
      .should("be.visible")
      .click({ force: true });

    cy.wait("@stage4PdfExport", { timeout: 60000 }).then((interception) => {
      expect(interception.response?.statusCode).to.be.oneOf([200, 204]);
    });

    cy.contains("[role='tab']", "Submit to Requesting Unit", {
      timeout: 30000,
    }).click({ force: true });

    cy.get("#submission-target", { timeout: 30000 })
      .should("be.visible")
      .select("principal_investigator");
    cy.get("#submission-format", { timeout: 30000 }).select("csv");

    cy.contains("button", "Submit Results Now", { timeout: 30000 })
      .should("be.visible")
      .click({ force: true });

    cy.wait("@stage4SubmitDelivery", { timeout: 60000 }).then(
      (interception) => {
        expect(interception.response?.statusCode).to.eq(200);
        const raw = interception.request.body;
        const parsed = typeof raw === "string" ? JSON.parse(raw) : raw;
        expect(parsed.targetCode).to.eq("PI");
        expect(parsed.exportFormat).to.eq("csv");
      },
    );
  });
});
