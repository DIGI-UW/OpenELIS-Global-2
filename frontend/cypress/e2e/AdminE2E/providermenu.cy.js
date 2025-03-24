import LoginPage from "../../pages/LoginPage";
import ProviderMenuPage from "../../pages/ProviderMenu";

let loginPage = null;
let homePage = null;
let adminPage = null;
let providerMenuPage = null;

before(() => {
  loginPage = new LoginPage();
  loginPage.visit();

  homePage = loginPage.goToHomePage();
  adminPage = homePage.goToAdminPage();
  providerMenuPage = adminPage.goToProviderMenuPage();
});

describe("Provider Menu Configuration", function () {
  beforeEach(() => {
    // Load fixture before each test
    cy.fixture("providerMenuData").as("providerData");
  });

  it("Navigates to Provider Menu", function () {
    providerMenuPage.verifyProviderMenuPage();
  });

  it("Fills in Provider Information", function () {
    cy.get("@providerData").then((data) => {
      providerMenuPage.fillProviderInformation(data.providerInfo);
    });
  });

  it("Sets Provider Address", function () {
    cy.get("@providerData").then((data) => {
      providerMenuPage.setProviderAddress(data.address);
    });
  });

  it("Sets Provider Contact Information", function () {
    cy.get("@providerData").then((data) => {
      providerMenuPage.setProviderContactInfo(data.contactInfo);
    });
  });

  it("Adds Provider Specialization", function () {
    cy.get("@providerData").then((data) => {
      providerMenuPage.addProviderSpecialization(data.specialization);
    });
  });

  it("Saves Provider Information", function () {
    providerMenuPage.saveProviderInformation();
  });

  it("Verifies Provider Information Saved", function () {
    providerMenuPage.verifySaveSuccess();
  });
});
