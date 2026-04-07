import LoginPage from "../pages/LoginPage";

describe("Interacts with Help options", function () {
  let loginPage, homePage, helpPage;

  before(() => {
    loginPage = new LoginPage();
    loginPage.visit();

    homePage = loginPage.goToHomePage();
    helpPage = homePage.goToHelp();
  });

  // Alternative setup for new implementation
  beforeEach(() => {
    // Note: For new implementation tests, use:
    // helpPage = homePage.goToHelpNew();
  });

  it("User navigates to User Manual", function () {
    // Test using legacy implementation - may be work in progress
    cy.window().then((win) => {
      cy.stub(win, "open").as("windowOpen"); // Stub to prevent opening a new tab
    });

    helpPage.clickUserManual();

    cy.get("@windowOpen").should("be.calledWithMatch", /\/docs\/UserManual/);
  });

  it("User navigates to User Manual - New Implementation", function () {
    // Alternative test using new SlideOverHelp implementation
    cy.window().then((win) => {
      cy.stub(win, "open").as("windowOpenNew"); // Stub to prevent opening a new tab
    });

    helpPage.clickUserManualNew();

    cy.get("@windowOpenNew").should("be.calledWithMatch", /\/docs\/UserManual/);
  });

  describe("User navigates to Process Documentation", function () {
    it("Navigates to Help", function () {
      // Note: Process Documentation not yet implemented in new SlideOverHelp
      helpPage.clickProcessDocumentation();
    });

    it("User opens VL Form", function () {
      // Note: VL Form not yet implemented in new SlideOverHelp
      cy.window().then((win) => {
        cy.stub(win, "open").as("windowOpen"); // Stub to prevent opening a new tab
      });

      helpPage.clickVLForm();

      cy.get("@windowOpen").should(
        "be.calledWithMatch",
        /\/documentation\/FICHE_DEMANDE_CHARGE_VIRALE_VF_\d+\.pdf/,
      );
    });

    it("User opens DBS Form", function () {
      // Note: DBS Form not yet implemented in new SlideOverHelp
      cy.window().then((win) => {
        cy.stub(win, "open").as("windowOpen"); // Stub to prevent opening a new tab
      });

      helpPage.clickDBSForm();

      cy.get("@windowOpen").should(
        "be.calledWithMatch",
        /\/documentation\/DBS_Identn_\d+[A-Za-z]+\d+\.pdf/,
      );
    });
  });
});
