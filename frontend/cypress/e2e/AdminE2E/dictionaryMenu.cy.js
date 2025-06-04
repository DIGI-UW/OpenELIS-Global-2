import LoginPage from "../../pages/LoginPage";

let loginPage = null;
let homePage = null;
let adminPage = null;
let dictMenu = null;
let usersData;

before(() => {
  loginPage = new LoginPage();
  loginPage.visit();

  homePage = loginPage.goToHomePage();
  adminPage = homePage.goToAdminPage();
});

describe("Dictionary Menu", function () {
  beforeEach(() => {
    cy.fixture("DictionaryMenu").then((users) => {
      usersData = users;
    });
  });

  it("Navigate to Dictionary Menu Page", function () {
    dictMenu = adminPage.goToDictionaryMenuPage();
    dictMenu.verifyPageTitle();
  });

  describe("Add Dictionary and Cancel", function () {
    it("Add Dictionary", function () {
      dictMenu.clickAddButton();
      dictMenu.validateModal();
    });

    it("Enter details", function () {
      dictMenu.dictNumberDisabled();
      dictMenu.dictCategory(usersData.cG);
      dictMenu.dictEntry(usersData.dictionaryEntry);
      dictMenu.isActive(usersData.yes);
      dictMenu.localAbbreviation(usersData.abbrev);
      dictMenu.clickCancelButton();
    });
  });
  describe("Add Dictionary and Add", function () {
    it("Add Dictionary", function () {
      dictMenu.clickAddButton();
      dictMenu.validateModal();
    });

    it("Enter details", function () {
      dictMenu.dictNumberDisabled();
      dictMenu.dictCategory(usersData.cG);
      dictMenu.dictEntry(usersData.dictionaryEntry);
      dictMenu.isActive(usersData.yes);
      dictMenu.localAbbreviation(usersData.abbrev);
      dictMenu.clickAddButton();
    });
  });

  describe("Validate Added Dictionary", function(){
    it("Search By Dictionary Entry", function(){
        dictMenu.searchByDictionaryEntry(usersData.dictEntry);
        dictMenu.validateDictEntry(usersData.dictEntry);
    });
  });
});
