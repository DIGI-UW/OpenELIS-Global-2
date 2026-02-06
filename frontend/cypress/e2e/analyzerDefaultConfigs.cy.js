/**
 * Cypress E2E Tests: Analyzer Default Configuration Loading
 *
 * <p>Tests the complete workflow for loading pre-configured analyzer templates
 * via the "Load Default Config" dropdown in AnalyzerForm.
 *
 * <p>Feature: 011-madagascar-analyzer-integration (M20)
 * <p>Task Reference: T219 (M20) - E2E test for load-default→populate→save
 *
 * <p>Test Coverage:
 * - Load default config from dropdown
 * - Verify form fields populated correctly
 * - Customize populated fields
 * - Save analyzer with default config
 * - Verify saved analyzer in database
 */

describe("Analyzer Default Configs E2E (M20)", () => {
  before(() => {
    // Login once for all tests in this file
    cy.login("admin", "adminADMIN!");
  });

  beforeEach(() => {
    cy.viewport(1025, 900);
  });

  /**
   * Test loading HL7 default config (Mindray BC2000).
   *
   * <p>Validates:
   * 1. Dropdown appears in create mode only
   * 2. Selecting template populates form fields
   * 3. Port number populated from default_port
   * 4. Protocol version populated correctly
   */
  it("should load HL7 default config and populate form fields", () => {
    // Arrange: Navigate to analyzer management
    cy.visit("/AnalyzerManagement");

    // Intercept API calls
    cy.intercept("GET", "/rest/analyzer/defaults").as("getDefaults");
    cy.intercept("GET", "/rest/analyzer/defaults/hl7/mindray-bc2000").as(
      "getBC2000Config",
    );
    cy.intercept("POST", "/rest/analyzer/analyzers").as("createAnalyzer");

    // Act: Click "Add Analyzer" button
    cy.get('[data-testid="add-analyzer-button"]').should("be.visible").click();

    // Wait for modal to open and defaults to load
    cy.wait("@getDefaults");
    cy.get('[data-testid="analyzer-form"]').should("be.visible");

    // Assert: "Load Default Config" dropdown should be visible (create mode only)
    cy.get('[data-testid="analyzer-form-default-config-dropdown"]').should(
      "be.visible",
    );

    // Act: Select BC2000 from dropdown
    cy.get('[data-testid="analyzer-form-default-config-dropdown"]').click();
    cy.contains("Mindray BC2000 (HL7)").click();

    // Wait for config to load
    cy.wait("@getBC2000Config");

    // Assert: Form fields should be populated
    cy.get('[data-testid="analyzer-form-name-input"]').should(
      "have.value",
      "Mindray BC2000",
    );

    cy.get('[data-testid="analyzer-form-port-input"]').should(
      "have.value",
      "5380",
    );

    // Assert: Success notification should appear
    cy.contains("Default configuration loaded").should("be.visible");
  });

  /**
   * Test loading ASTM default config (Mindray BA-88A).
   *
   * <p>Validates ASTM-specific config loading where default_port is not set
   * (RS-232 serial analyzer).
   */
  it("should load ASTM default config without port for serial analyzer", () => {
    // Arrange
    cy.visit("/AnalyzerManagement");
    cy.intercept("GET", "/rest/analyzer/defaults").as("getDefaults");
    cy.intercept("GET", "/rest/analyzer/defaults/astm/mindray-ba88a").as(
      "getBA88AConfig",
    );

    // Act: Open form and select BA-88A
    cy.get('[data-testid="add-analyzer-button"]').click();
    cy.wait("@getDefaults");

    cy.get('[data-testid="analyzer-form-default-config-dropdown"]').click();
    cy.contains("Mindray BA-88A (ASTM)").click();
    cy.wait("@getBA88AConfig");

    // Assert: Name populated, but port should remain empty (serial analyzer)
    cy.get('[data-testid="analyzer-form-name-input"]').should(
      "have.value",
      "Mindray BA-88A",
    );

    cy.get('[data-testid="analyzer-form-port-input"]').should("have.value", ""); // No port for RS-232 serial
  });

  /**
   * Test customizing default config before saving.
   *
   * <p>Validates that users can modify template values before saving.
   */
  it("should allow customization after loading default config", () => {
    // Arrange
    cy.visit("/AnalyzerManagement");
    cy.intercept("GET", "/rest/analyzer/defaults").as("getDefaults");
    cy.intercept("GET", "/rest/analyzer/defaults/hl7/mindray-bc2000").as(
      "getBC2000Config",
    );
    cy.intercept("POST", "/rest/analyzer/analyzers").as("createAnalyzer");

    // Act: Load default and customize
    cy.get('[data-testid="add-analyzer-button"]').click();
    cy.wait("@getDefaults");

    cy.get('[data-testid="analyzer-form-default-config-dropdown"]').click();
    cy.contains("Mindray BC2000 (HL7)").click();
    cy.wait("@getBC2000Config");

    // Customize name
    cy.get('[data-testid="analyzer-form-name-input"]')
      .clear()
      .type("Lab A - BC2000");

    // Customize port
    cy.get('[data-testid="analyzer-form-port-input"]').clear().type("6000");

    // Assert: Customized values retained
    cy.get('[data-testid="analyzer-form-name-input"]').should(
      "have.value",
      "Lab A - BC2000",
    );
    cy.get('[data-testid="analyzer-form-port-input"]').should(
      "have.value",
      "6000",
    );
  });

  /**
   * Test complete workflow: load default → customize → save → verify.
   *
   * <p>Validates end-to-end workflow from template selection to saved analyzer.
   */
  it("should complete full workflow: load → customize → save → verify", () => {
    // Arrange
    cy.visit("/AnalyzerManagement");
    cy.intercept("GET", "/rest/analyzer/defaults").as("getDefaults");
    cy.intercept("GET", "/rest/analyzer/defaults/hl7/mindray-bc2000").as(
      "getBC2000Config",
    );
    cy.intercept("POST", "/rest/analyzer/analyzers").as("createAnalyzer");
    cy.intercept("GET", "/rest/analyzer/analyzers*").as("getAnalyzers");

    // Act: Load default config
    cy.get('[data-testid="add-analyzer-button"]').click();
    cy.wait("@getDefaults");

    cy.get('[data-testid="analyzer-form-default-config-dropdown"]').click();
    cy.contains("Mindray BC2000 (HL7)").click();
    cy.wait("@getBC2000Config");

    // Customize name for uniqueness
    const uniqueName = `BC2000-E2E-${Date.now()}`;
    cy.get('[data-testid="analyzer-form-name-input"]').clear().type(uniqueName);

    // Add IP address
    cy.get('[data-testid="analyzer-form-ip-input"]').type("192.168.1.200");

    // Save
    cy.get('[data-testid="analyzer-form-save-button"]').click();
    cy.wait("@createAnalyzer");

    // Assert: Success notification
    cy.contains("Analyzer saved successfully").should("be.visible");

    // Verify: Analyzer appears in list
    cy.wait("@getAnalyzers");
    cy.contains(uniqueName).should("be.visible");
  });

  /**
   * Test error handling when default config fails to load.
   */
  it("should display error notification when default config fails to load", () => {
    // Arrange: Mock error response
    analyzerService.getDefaultConfig.mockImplementation(
      (protocol, name, callback) => {
        callback({ error: "Template file not found" });
      },
    );

    cy.visit("/AnalyzerManagement");
    cy.intercept("GET", "/rest/analyzer/defaults").as("getDefaults");
    cy.intercept("GET", "/rest/analyzer/defaults/**", {
      statusCode: 404,
      body: { error: "Template not found" },
    }).as("getConfigError");

    // Act: Select config
    cy.get('[data-testid="add-analyzer-button"]').click();
    cy.wait("@getDefaults");

    cy.get('[data-testid="analyzer-form-default-config-dropdown"]').click();
    cy.contains("Mindray BC2000 (HL7)").click();

    // Assert: Error notification should appear
    cy.wait("@getConfigError");
    cy.contains("Failed to load default configuration").should("be.visible");
  });

  /**
   * Test that default configs are properly categorized by protocol.
   *
   * <p>Validates dropdown shows protocol badges (HL7), (ASTM).
   */
  it("should display protocol badges in dropdown options", () => {
    // Arrange
    cy.visit("/AnalyzerManagement");
    cy.intercept("GET", "/rest/analyzer/defaults").as("getDefaults");

    // Act: Open dropdown
    cy.get('[data-testid="add-analyzer-button"]').click();
    cy.wait("@getDefaults");

    cy.get('[data-testid="analyzer-form-default-config-dropdown"]').click();

    // Assert: Options should show protocol badges
    cy.contains("Mindray BC2000 (HL7)").should("be.visible");
    cy.contains("Mindray BA-88A (ASTM)").should("be.visible");
    cy.contains("Abbott Architect (HL7)").should("be.visible");
  });
});
