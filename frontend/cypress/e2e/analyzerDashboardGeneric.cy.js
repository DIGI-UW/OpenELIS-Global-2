describe("Generic Analyzer Dashboard - All Protocol Types", () => {
  before(() => {
    // Login once for the test suite
    cy.login("admin", "adminADMIN!");
  });

  beforeEach(() => {
    // Set viewport before each test
    cy.viewport(1025, 900);
  });

  it("should display all 11 Madagascar analyzers from fixtures", () => {
    cy.visit("/AnalyzerDashboard");
    cy.get('[data-testid="analyzer-list"]').should("be.visible");

    // Verify minimum count (11 active analyzers)
    cy.get('[data-testid^="analyzer-"]').should("have.length.at.least", 11);
  });

  describe("HL7 TCP Analyzers", () => {
    beforeEach(() => {
      cy.visit("/AnalyzerDashboard");
    });

    it("should display Abbott Architect (2000) with HL7 badge", () => {
      cy.contains("Abbott Architect").should("be.visible");
      cy.contains("Abbott Architect")
        .parents('[data-testid^="analyzer-"]')
        .find('[data-testid="protocol-badge"]')
        .should("contain", "HL7");
    });

    it("should display Mindray BC-5380 (2007) with HL7 badge", () => {
      cy.contains("Mindray BC-5380").should("be.visible");
      cy.contains("Mindray BC-5380")
        .parents('[data-testid^="analyzer-"]')
        .find('[data-testid="protocol-badge"]')
        .should("contain", "HL7");
    });

    it("should display Mindray BS-360E (2008) with HL7 badge", () => {
      cy.contains("Mindray BS-360E").should("be.visible");
      cy.contains("Mindray BS-360E")
        .parents('[data-testid^="analyzer-"]')
        .find('[data-testid="protocol-badge"]')
        .should("contain", "HL7");
    });

    it("should test connection for Mindray BC-5380 (2007)", () => {
      // Find the analyzer row and click test connection button
      cy.contains("Mindray BC-5380")
        .parents('[data-testid^="analyzer-"]')
        .find('[data-testid="test-connection-btn"]')
        .click();

      // Wait for test connection modal or inline status
      cy.get('[data-testid="test-connection-result"]', {
        timeout: 10000,
      }).should("be.visible");

      // Expect success (mock server should respond with TCP connection)
      cy.get('[data-testid="test-connection-result"]').should(
        "contain.text",
        "success",
      );
    });
  });

  describe("ASTM TCP Analyzers", () => {
    beforeEach(() => {
      cy.visit("/AnalyzerDashboard");
    });

    it("should display Sysmex XN (2011) with ASTM badge", () => {
      cy.contains("Sysmex XN Series").should("be.visible");
      cy.contains("Sysmex XN Series")
        .parents('[data-testid^="analyzer-"]')
        .find('[data-testid="protocol-badge"]')
        .should("contain", "ASTM");
    });

    it("should show TCP/IP transport indicator for Sysmex XN (2011)", () => {
      cy.contains("Sysmex XN Series")
        .parents('[data-testid^="analyzer-"]')
        .find('[data-testid="transport-badge"]')
        .should("contain", "TCP");
    });
  });

  describe("ASTM RS232 Analyzers", () => {
    beforeEach(() => {
      cy.visit("/AnalyzerDashboard");
    });

    it("should display Horiba Pentra 60 (2005) with Serial badge", () => {
      cy.contains("Horiba ABX Pentra 60").should("be.visible");
      cy.contains("Horiba ABX Pentra 60")
        .parents('[data-testid^="analyzer-"]')
        .find('[data-testid="serial-badge"]')
        .should("be.visible");
    });

    it("should display Horiba Micros 60 (2004) with Serial badge", () => {
      cy.contains("Horiba ABX Micros 60").should("be.visible");
      cy.contains("Horiba ABX Micros 60")
        .parents('[data-testid^="analyzer-"]')
        .find('[data-testid="serial-badge"]')
        .should("be.visible");
    });

    it("should display Mindray BA-88A (2006) with Serial badge", () => {
      cy.contains("Mindray BA-88A").should("be.visible");
      cy.contains("Mindray BA-88A")
        .parents('[data-testid^="analyzer-"]')
        .find('[data-testid="serial-badge"]')
        .should("be.visible");
    });

    it("should display Stago STart 4 (2010) with Serial badge", () => {
      cy.contains("Stago STart 4").should("be.visible");
      cy.contains("Stago STart 4")
        .parents('[data-testid^="analyzer-"]')
        .find('[data-testid="serial-badge"]')
        .should("be.visible");
    });
  });

  describe("FILE Analyzers", () => {
    beforeEach(() => {
      cy.visit("/AnalyzerDashboard");
    });

    it("should display GeneXpert (2002) with FILE badge", () => {
      cy.contains("Cepheid GeneXpert").should("be.visible");
      cy.contains("Cepheid GeneXpert")
        .parents('[data-testid^="analyzer-"]')
        .find('[data-testid="file-badge"]')
        .should("be.visible");
    });

    it("should display FluoroCycler XT (2003) with FILE badge", () => {
      cy.contains("Hain FluoroCycler XT").should("be.visible");
      cy.contains("Hain FluoroCycler XT")
        .parents('[data-testid^="analyzer-"]')
        .find('[data-testid="file-badge"]')
        .should("be.visible");
    });

    it("should display QuantStudio 7 Flex (2009) with FILE badge", () => {
      cy.contains("Thermo Fisher QuantStudio 7").should("be.visible");
      cy.contains("Thermo Fisher QuantStudio 7")
        .parents('[data-testid^="analyzer-"]')
        .find('[data-testid="file-badge"]')
        .should("be.visible");
    });
  });

  describe("Analyzer Categories", () => {
    beforeEach(() => {
      cy.visit("/AnalyzerDashboard");
    });

    it("should show correct category badges", () => {
      // HEMATOLOGY
      cy.contains("Mindray BC-5380")
        .parents('[data-testid^="analyzer-"]')
        .should("contain", "HEMATOLOGY");

      // CHEMISTRY
      cy.contains("Mindray BA-88A")
        .parents('[data-testid^="analyzer-"]')
        .should("contain", "CHEMISTRY");

      // MOLECULAR
      cy.contains("Cepheid GeneXpert")
        .parents('[data-testid^="analyzer-"]')
        .should("contain", "MOLECULAR");

      // IMMUNOLOGY
      cy.contains("Abbott Architect")
        .parents('[data-testid^="analyzer-"]')
        .should("contain", "IMMUNOLOGY");

      // COAGULATION
      cy.contains("Stago STart 4")
        .parents('[data-testid^="analyzer-"]')
        .should("contain", "COAGULATION");
    });
  });

  describe("Active Status", () => {
    beforeEach(() => {
      cy.visit("/AnalyzerDashboard");
    });

    it("should show all analyzers as active", () => {
      // All 11 analyzers should have active status indicator
      cy.get('[data-testid^="analyzer-"]').each(($analyzer) => {
        cy.wrap($analyzer)
          .find('[data-testid="active-status"]')
          .should("exist");
      });
    });
  });
});
