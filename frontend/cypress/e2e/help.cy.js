import LoginPage from "../pages/LoginPage";

describe("Interacts with Help options", function () {
  let loginPage, homePage, helpPage;

  before(() => {
    loginPage = new LoginPage();
    loginPage.visit();

    homePage = loginPage.goToHomePage();
    helpPage = homePage.goToHelp();
  });

  it("User navigates to User Manual", function () {
    cy.window().then((win) => {
      cy.stub(win, "open").as("windowOpen"); // Stub to prevent opening a new tab
    });

    helpPage.clickUserManual();

    cy.get("@windowOpen").should("be.calledWithMatch", /\/docs\/UserManual/);
  });

  describe("User navigates to Process Documentation", function () {
    it("Navigates to Help", function () {
      helpPage.clickProcessDocumentation();
    });

    it("User opens VL Form", function () {
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
