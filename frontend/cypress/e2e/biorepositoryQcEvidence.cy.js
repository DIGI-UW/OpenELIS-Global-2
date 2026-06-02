import LoginPage from "../pages/LoginPage";

/**
 * Evidence capture for Biorepository QC data-source fix + workflow.
 * Screenshots: cypress/screenshots/.../biorepository-qc-evidence/*.png
 * Copy to: OpenELIS-Global-2/docs/biorepository-qc-evidence/
 */
const login = new LoginPage();
const NOTEBOOK_TEMPLATE_ID = 27;
const NOTEBOOK_URL = `/NoteBookInstanceEditForm/${NOTEBOOK_TEMPLATE_ID}?mode=edit`;
const API_BASE = "/OpenELIS-Global";
const shot = (name) =>
  cy.screenshot(`biorepository-qc-evidence/${name}`, { capture: "fullPage" });

/** Carbon Design Dropdown (not a native <select>). */
const pickCarbonDropdown = (elementId, optionLabel) => {
  cy.get('[role="dialog"]:visible').last().within(() => {
    cy.get(`#${elementId}`)
      .find('[role="combobox"], .cds--list-box__field, button[aria-haspopup="listbox"]')
      .first()
      .click({ force: true });
    cy.get('.cds--list-box__menu:visible [role="option"]', { timeout: 30000 })
      .contains(optionLabel)
      .click({ force: true });
  });
};

const openQcStage = () => {
  cy.visit(NOTEBOOK_URL);
  cy.contains("button, a, div", "Workflow", { timeout: 120000 }).click({
    force: true,
  });
  cy.get(".biorepository-workflow", { timeout: 120000 }).should("be.visible");
  cy.contains(/QC Inspection/i, { timeout: 120000 }).click({ force: true });
  cy.get(".biorepository-qc-inspection-page", { timeout: 120000 }).should(
    "exist",
  );
  cy.get("#qc-include-inspected", { timeout: 120000 }).check({ force: true });
};

describe("Biorepository QC evidence (data source + workflow)", () => {
  beforeEach(() => {
    Cypress.config("defaultCommandTimeout", 120000);
    login.visit();
    login.enterUsername("admin");
    login.enterPassword("adminADMIN!");
    login.signIn();
    cy.url({ timeout: 60000 }).should("not.include", "/login");
  });

  it("captures storage vs QC counts and full workflow evidence", () => {
    cy.intercept(
      "POST",
      `${API_BASE}/rest/biorepository/qc-inspection/generate-round`,
    ).as("generateRound");
    cy.intercept(
      "POST",
      `${API_BASE}/rest/biorepository/qc-inspection/bulk-apply`,
    ).as("applyQc");
    cy.intercept(
      "GET",
      `${API_BASE}/rest/biorepository/qc-inspection/storage-overview*`,
    ).as("storageOverview");
    cy.intercept(
      "GET",
      `${API_BASE}/rest/biorepository/qc-inspection/batch-escalation*`,
    ).as("batchEscalation");

    cy.visit("/Storage/samples");
    cy.get("body", { timeout: 60000 }).should("be.visible");
    shot("01-storage-management-active-samples");

    openQcStage();
    cy.wait("@storageOverview", { timeout: 120000 }).then((interception) => {
      const body = interception.response?.body || {};
      const scopeStats = body.scopeStats || {};
      const diagnostics = body.diagnostics || {};
      expect(scopeStats.totalStored, "totalStored").to.be.greaterThan(0);
      expect(diagnostics.qcPoolTotal, "qcPoolTotal").to.be.greaterThan(0);
    });
    cy.contains(".progress-tile", "Total Stored", { timeout: 120000 })
      .find(".progress-value")
      .should(($el) => {
        expect(parseInt(String($el.text()).replace(/\D/g, ""), 10)).to.be.greaterThan(
          0,
        );
      });
    shot("03-qc-overview-nonzero-counts");

    cy.get("#qc-filter-freezer", { timeout: 60000 }).then(($select) => {
      if ($select.length && $select.find("option").length > 1) {
        cy.wrap($select).select(1, { force: true });
      }
    });
    shot("04-qc-device-selected");

    cy.get("#qc-boxes-per-round", { timeout: 60000 }).clear().type("1");
    cy.get("#qc-samples-per-box").clear().type("2");

    cy.contains("button", "Generate Random QC Round", { timeout: 60000 })
      .should("be.visible")
      .click({ force: true });
    cy.wait("@generateRound").then((interception) => {
      expect(interception.response?.statusCode).to.eq(200);
      const roundBody = interception.response?.body || {};
      cy.wrap(roundBody).as("roundBody");
    });
    shot("05-qc-round-generated");

    cy.get("@roundBody").then((roundBody) => {
      const roundSamples = roundBody.samples || [];
      const batchId = roundBody.qcBatchId;
      expect(roundSamples.length).to.be.greaterThan(1);
      cy.request({
        method: "POST",
        url: `${API_BASE}/rest/biorepository/qc-inspection/bulk-apply`,
        body: {
          bioSampleIds: [roundSamples[0].bioSampleId],
          inspectorName: "Evidence Inspector",
          inspectionDate: new Date().toISOString(),
          qcBatchId: batchId,
          samplePresent: false,
          labelIntegrity: true,
          containerIntegrity: true,
          volumeAppearanceAcceptable: true,
          correctPosition: false,
          discrepancyType: "SAMPLE_MISSING",
          correctiveAction: "FAIL in active QC batch",
          remarks: "Batch escalation evidence",
        },
      }).its("status").should("eq", 200);
    });

    cy.get(".sample-table-section table tbody tr", { timeout: 60000 })
      .should("have.length.greaterThan", 1)
      .eq(1)
      .contains("button", "Inspect")
      .click({ force: true });
    cy.contains("Individual QC Inspection", { timeout: 60000 }).should(
      "be.visible",
    );
    cy.get("#inspectorName").clear().type("Evidence Inspector");
    cy.contains("button", "Clear All").click({ force: true });
    pickCarbonDropdown("discrepancyType", /Sample missing/i);
    cy.get("#correctiveAction, textarea[name='correctiveAction']")
      .first()
      .clear()
      .type("Corrective action documented for evidence", { force: true });
    cy.get("#remarks, textarea[name='remarks']")
      .first()
      .clear()
      .type("FAIL saved with correction workflow", { force: true });
    pickCarbonDropdown("correctionActionType", /Request supervisor review/i);
    cy.get("#correctionReason, textarea[name='correctionReason']")
      .first()
      .clear()
      .type("Supervisor review requested", { force: true });
    cy.contains("button", "Record Discrepancy").click({ force: true });
    cy.wait("@applyQc").its("response.statusCode").should("eq", 200);
    cy.wait("@batchEscalation")
      .its("response.body.supervisorNotificationRequired")
      .should("eq", true);
    cy.contains("QC batch escalation alert", { timeout: 60000 }).should(
      "be.visible",
    );
    shot("08-qc-fail-saved-with-correction");
    shot("09-qc-escalation-alert");

    openQcStage();
    cy.contains("button", "Inspect", { timeout: 60000 })
      .first()
      .click({ force: true });
    cy.contains("Individual QC Inspection", { timeout: 60000 }).should(
      "be.visible",
    );
    cy.get("#inspectorName").clear().type("Evidence Inspector");
    cy.contains("button", "Check All (Verify)").click({ force: true });
    cy.contains("button", "Record Verification").click({ force: true });
    cy.wait("@applyQc").its("response.statusCode").should("eq", 200);
    shot("06-qc-pass-saved");

    cy.contains("button", "Inspect", { timeout: 60000 })
      .first()
      .click({ force: true });
    cy.contains("Individual QC Inspection", { timeout: 60000 }).should(
      "be.visible",
    );
    cy.get("#inspectorName").clear().type("Evidence Inspector");
    cy.contains("button", "Clear All").click({ force: true });
    cy.contains("button", "Record Discrepancy").click({ force: true });
    cy.contains("Please select a discrepancy type", { timeout: 30000 }).should(
      "be.visible",
    );
    shot("07-qc-fail-validation");
    cy.contains("button", /Cancel|Close/, { timeout: 60000 })
      .first()
      .click({ force: true });

    cy.contains("button", "Inspect", { timeout: 60000 })
      .first()
      .click({ force: true });
    cy.contains("button", /History|Inspection history/i, { timeout: 60000 })
      .first()
      .click({ force: true });
    cy.contains(/QC|Inspection|Inspector/i, { timeout: 60000 }).should(
      "be.visible",
    );
    shot("10-qc-history-audit-trail");
  });
});
