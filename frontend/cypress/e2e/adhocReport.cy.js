import LoginPage from "../pages/LoginPage";

let homePage = null;
let loginPage = null;

before("login", () => {
  loginPage = new LoginPage();
  loginPage.visit();
  homePage = loginPage.goToHomePage();
});

describe("Ad-Hoc Report Builder", function () {
  describe("Page Load and Field Discovery", function () {
    it("Should load the ad-hoc report builder page", function () {
      cy.visit("/AdHocReport");
      cy.contains("Ad-Hoc Patient Report Builder", { timeout: 10000 }).should(
        "be.visible",
      );
      cy.contains("Patient Fields").should("be.visible");
      cy.contains("Sample Fields").should("be.visible");
    });

    it("Should display available patient fields", function () {
      cy.visit("/AdHocReport");
      cy.contains("National ID", { timeout: 10000 }).should("exist");
      cy.contains("First Name").should("exist");
      cy.contains("Last Name").should("exist");
      cy.contains("Gender").should("exist");
    });

    it("Should display available sample fields", function () {
      cy.visit("/AdHocReport");
      cy.contains("Lab Number", { timeout: 10000 }).should("exist");
      cy.contains("Collection Date").should("exist");
      cy.contains("Received Date").should("exist");
      cy.contains("Status").should("exist");
    });
  });

  describe("Field Selection", function () {
    it("Should allow selecting individual fields", function () {
      cy.visit("/AdHocReport");
      cy.get('[id="field-patient.nationalId"]', { timeout: 10000 })
        .scrollIntoView()
        .check({ force: true });
      cy.get('[id="field-patient.nationalId"]').should("be.checked");
      cy.contains("1 fields selected").should("be.visible");
    });

    it("Should allow selecting multiple fields", function () {
      cy.visit("/AdHocReport");
      cy.get('[id="field-patient.nationalId"]', { timeout: 10000 })
        .scrollIntoView()
        .check({ force: true });
      cy.get('[id="field-patient.firstName"]')
        .scrollIntoView()
        .check({ force: true });
      cy.get('[id="field-sample.accessionNumber"]')
        .scrollIntoView()
        .check({ force: true });
      cy.contains("3 fields selected").should("be.visible");
    });

    it("Should allow select all for an entity", function () {
      cy.visit("/AdHocReport");
      cy.get('[id="select-all-patient"]', { timeout: 10000 })
        .scrollIntoView()
        .check({ force: true });
      cy.get('[id="field-patient.nationalId"]').should("be.checked");
      cy.get('[id="field-patient.firstName"]').should("be.checked");
      cy.get('[id="field-patient.lastName"]').should("be.checked");
    });

    it("Should allow deselecting fields", function () {
      cy.visit("/AdHocReport");
      cy.get('[id="field-patient.nationalId"]', { timeout: 10000 })
        .scrollIntoView()
        .check({ force: true });
      cy.get('[id="field-patient.nationalId"]').uncheck({ force: true });
      cy.get('[id="field-patient.nationalId"]').should("not.be.checked");
    });
  });

  describe("Filter Builder", function () {
    it("Should show empty state when no filters added", function () {
      cy.visit("/AdHocReport");
      cy.contains("No filters added", { timeout: 10000 }).should("be.visible");
    });

    it("Should allow adding a filter", function () {
      cy.visit("/AdHocReport");
      cy.get('[data-testid="add-filter-btn"]', { timeout: 10000 })
        .scrollIntoView()
        .click({ force: true });
      cy.get(".adhoc-filter-row", { timeout: 10000 }).should("have.length", 1);
    });

    it("Should allow configuring a filter", function () {
      cy.visit("/AdHocReport");
      cy.get('[data-testid="add-filter-btn"]', { timeout: 10000 })
        .scrollIntoView()
        .click({ force: true });
      cy.get(".adhoc-filter-row", { timeout: 10000 }).should("exist");
      cy.get('[id^="filter-field-"]').first().select("patient.gender");
      cy.get('[id^="filter-operator-"]').first().select("EQUALS");
      cy.get('[id^="filter-value-"]').first().type("M");
    });

    it("Should allow removing a filter", function () {
      cy.visit("/AdHocReport");
      cy.get('[data-testid="add-filter-btn"]', { timeout: 10000 })
        .scrollIntoView()
        .click({ force: true });
      cy.get(".adhoc-filter-row", { timeout: 10000 }).should("have.length", 1);
      cy.get('[data-testid="remove-filter-btn"]', { timeout: 5000 })
        .first()
        .click({ force: true });
      cy.get(".adhoc-filter-row").should("have.length", 0);
    });

    it("Should allow clearing all filters", function () {
      cy.visit("/AdHocReport");
      cy.get('[data-testid="add-filter-btn"]', { timeout: 10000 })
        .scrollIntoView()
        .click({ force: true });
      cy.get(".adhoc-filter-row", { timeout: 10000 }).should("have.length", 1);
      cy.get('[data-testid="add-filter-btn"]').click({ force: true });
      cy.get(".adhoc-filter-row").should("have.length", 2);
      cy.contains("Clear All Filters").click({ force: true });
      cy.get(".adhoc-filter-row").should("have.length", 0);
    });

    it("Should show date pickers for date fields", function () {
      cy.visit("/AdHocReport");
      cy.get('[data-testid="add-filter-btn"]', { timeout: 10000 })
        .scrollIntoView()
        .click({ force: true });
      cy.get(".adhoc-filter-row", { timeout: 10000 }).should("exist");
      cy.get('[id^="filter-field-"]').first().select("sample.collectionDate");
      cy.get(".cds--date-picker").should("be.visible");
    });

    it("Should show range inputs for BETWEEN operator", function () {
      cy.visit("/AdHocReport");
      cy.get('[data-testid="add-filter-btn"]', { timeout: 10000 })
        .scrollIntoView()
        .click({ force: true });
      cy.get(".adhoc-filter-row", { timeout: 10000 }).should("exist");
      cy.get('[id^="filter-field-"]').first().select("sample.collectionDate");
      cy.get('[id^="filter-operator-"]').first().select("BETWEEN");
      cy.get(".cds--date-picker").should("have.length.at.least", 2);
    });
  });

  describe("Report Preview", function () {
    it("Should show message when no fields selected", function () {
      cy.visit("/AdHocReport");
      cy.contains("Select at least one field", { timeout: 10000 }).should(
        "be.visible",
      );
    });

    it("Should disable preview button when no fields selected", function () {
      cy.visit("/AdHocReport");
      cy.contains("Preview Report", { timeout: 10000 }).should("be.disabled");
    });

    it("Should enable preview button when fields are selected", function () {
      cy.visit("/AdHocReport");
      cy.get('[id="field-patient.nationalId"]', { timeout: 10000 })
        .scrollIntoView()
        .check({ force: true });
      cy.contains("Preview Report").should("not.be.disabled");
    });

    it("Should show preview data after clicking preview", function () {
      cy.intercept("POST", "**/rest/adhoc-report/preview").as("previewRequest");
      cy.visit("/AdHocReport");
      cy.get('[id="field-patient.nationalId"]', { timeout: 10000 })
        .scrollIntoView()
        .check({ force: true });
      cy.get('[id="field-patient.firstName"]')
        .scrollIntoView()
        .check({ force: true });
      cy.contains("Preview Report").scrollIntoView().click();
      cy.wait("@previewRequest", { timeout: 15000 });
      cy.get(".cds--data-table", { timeout: 10000 }).should("exist");
    });
  });

  describe("PDF Generation", function () {
    it("Should disable PDF button when no fields selected", function () {
      cy.visit("/AdHocReport");
      cy.contains("Generate PDF", { timeout: 10000 }).should("be.disabled");
    });

    it("Should enable PDF button when fields are selected", function () {
      cy.visit("/AdHocReport");
      cy.get('[id="field-patient.nationalId"]', { timeout: 10000 })
        .scrollIntoView()
        .check({ force: true });
      cy.contains("Generate PDF").should("not.be.disabled");
    });

    it("Should trigger PDF download when clicking generate", function () {
      cy.intercept("POST", "**/rest/adhoc-report/generate-pdf").as(
        "pdfRequest",
      );
      cy.visit("/AdHocReport");
      cy.get('[id="field-patient.nationalId"]', { timeout: 10000 })
        .scrollIntoView()
        .check({ force: true });
      cy.get('[id="field-patient.firstName"]')
        .scrollIntoView()
        .check({ force: true });
      cy.contains("Generate PDF").scrollIntoView().click();
      cy.wait("@pdfRequest", { timeout: 15000 }).then((interception) => {
        expect(interception.request.body.selectedFields).to.include(
          "patient.nationalId",
        );
        expect(interception.request.body.selectedFields).to.include(
          "patient.firstName",
        );
      });
    });
  });

  describe("Report Title", function () {
    it("Should allow entering a custom report title", function () {
      cy.visit("/AdHocReport");
      cy.get('[id="report-title"]', { timeout: 10000 })
        .scrollIntoView()
        .type("My Custom Report");
      cy.get('[id="report-title"]').should("have.value", "My Custom Report");
    });

    it("Should include title in PDF request", function () {
      cy.intercept("POST", "**/rest/adhoc-report/generate-pdf").as(
        "pdfRequest",
      );
      cy.visit("/AdHocReport");
      cy.get('[id="report-title"]', { timeout: 10000 })
        .scrollIntoView()
        .type("Test Report Title");
      cy.get('[id="field-patient.nationalId"]')
        .scrollIntoView()
        .check({ force: true });
      cy.contains("Generate PDF").scrollIntoView().click();
      cy.wait("@pdfRequest", { timeout: 15000 }).then((interception) => {
        expect(interception.request.body.reportTitle).to.equal(
          "Test Report Title",
        );
      });
    });
  });

  describe("Reset Functionality", function () {
    it("Should reset all selections when clicking reset", function () {
      cy.visit("/AdHocReport");
      cy.get('[id="field-patient.nationalId"]', { timeout: 10000 })
        .scrollIntoView()
        .check({ force: true });
      cy.get('[id="field-patient.firstName"]')
        .scrollIntoView()
        .check({ force: true });
      cy.get('[data-testid="add-filter-btn"]')
        .scrollIntoView()
        .click({ force: true });
      cy.get(".adhoc-filter-row", { timeout: 10000 }).should("have.length", 1);
      cy.get('[id="report-title"]').scrollIntoView().type("Test Title");
      cy.contains("Reset").scrollIntoView().click();
      cy.get('[id="field-patient.nationalId"]').should("not.be.checked");
      cy.get('[id="field-patient.firstName"]').should("not.be.checked");
      cy.get(".adhoc-filter-row").should("have.length", 0);
      cy.get('[id="report-title"]').should("have.value", "");
    });
  });

  describe("Error Handling", function () {
    it("Should show error when preview fails", function () {
      cy.intercept("POST", "**/rest/adhoc-report/preview", {
        statusCode: 500,
        body: { message: "Server error" },
      }).as("previewError");
      cy.visit("/AdHocReport");
      cy.get('[id="field-patient.nationalId"]', { timeout: 10000 })
        .scrollIntoView()
        .check({ force: true });
      cy.contains("Preview Report").scrollIntoView().click();
      cy.wait("@previewError", { timeout: 15000 });
      cy.get(".cds--inline-notification--error", { timeout: 10000 }).should(
        "be.visible",
      );
    });
  });

  describe("Complete Workflow Integration", function () {
    it("Should complete full workflow: select fields, add filter, preview, generate PDF", function () {
      cy.intercept("POST", "**/rest/adhoc-report/preview").as("preview");
      cy.intercept("POST", "**/rest/adhoc-report/generate-pdf").as(
        "generatePdf",
      );

      cy.visit("/AdHocReport");

      cy.get('[id="report-title"]', { timeout: 10000 })
        .scrollIntoView()
        .type("Patient Gender Report");
      cy.get('[id="field-patient.nationalId"]')
        .scrollIntoView()
        .check({ force: true });
      cy.get('[id="field-patient.firstName"]')
        .scrollIntoView()
        .check({ force: true });
      cy.get('[id="field-patient.lastName"]')
        .scrollIntoView()
        .check({ force: true });
      cy.get('[id="field-patient.gender"]')
        .scrollIntoView()
        .check({ force: true });
      cy.get('[id="field-sample.accessionNumber"]')
        .scrollIntoView()
        .check({ force: true });
      cy.contains("5 fields selected").should("be.visible");

      cy.get('[data-testid="add-filter-btn"]')
        .scrollIntoView()
        .click({ force: true });
      cy.get(".adhoc-filter-row", { timeout: 10000 }).should("exist");
      cy.get('[id^="filter-field-"]').first().select("patient.gender");
      cy.get('[id^="filter-operator-"]').first().select("EQUALS");
      cy.get('[id^="filter-value-"]').first().type("M");

      cy.contains("Preview Report").scrollIntoView().click();
      cy.wait("@preview", { timeout: 15000 });

      cy.contains("Generate PDF").scrollIntoView().click();
      cy.wait("@generatePdf", { timeout: 15000 }).then((interception) => {
        expect(interception.request.body.selectedFields).to.have.length(5);
        expect(interception.request.body.filters).to.have.length(1);
        expect(interception.request.body.filters[0].fieldId).to.equal(
          "patient.gender",
        );
        expect(interception.request.body.reportTitle).to.equal(
          "Patient Gender Report",
        );
      });
    });
  });
});
