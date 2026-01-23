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
      dictMenu.dictCategory(usersData[0].cG);
      dictMenu.dictEntry(usersData[0].dictionaryEntry);
      dictMenu.isActive(usersData[0].yes);
      dictMenu.localAbbreviation(usersData[0].abbrev);
      dictMenu.clickCancelButton();
    });
  });

  describe("Add Dictionary and Add", function () {
    it("Add First Dictionary", function () {
      dictMenu.clickAddButton();
      dictMenu.validateModal();
    });

    it("Enter details", function () {
      dictMenu.dictNumberDisabled();
      dictMenu.dictCategory(usersData[0].cG);
      dictMenu.dictEntry(usersData[0].dictionaryEntry);
      dictMenu.notActive(usersData[0].no);
      dictMenu.localAbbreviation(usersData[0].abbrev);
      dictMenu.addButton();
    });

    it("Add Second Dictionary", function () {
      dictMenu.clickAddButton();
      dictMenu.validateModal();
    });

    it("Enter details", function () {
      dictMenu.dictNumberDisabled();
      dictMenu.dictCategory(usersData[1].cG);
      dictMenu.dictEntry(usersData[1].dictionaryEntry);
      dictMenu.isActive(usersData[0].yes);
      dictMenu.localAbbreviation(usersData[1].abbrev);
      dictMenu.addButton();
    });
  });

  // describe("Validate Added Dictionary", function () {
  //   it("Search By Dictionary Entry", function () {
  //     dictMenu.searchByDictionaryEntry(usersData[0].dictionaryEntry);
  //     dictMenu.validateColumnContent(
  //       "dictEntry",
  //       "1378",
  //       usersData[0].dictionaryEntry,
  //     );
  //     dictMenu.searchByDictionaryEntry(usersData[1].dictionaryEntry);
  //     dictMenu.validateColumnContent(
  //       "dictEntry",
  //       "1398",
  //       usersData[1].dictionaryEntry,
  //     );
  //     dictMenu.clearSearch();
  //   });
  // });

  describe("Modify Dictionary", function () {
    it("Check and Modify First Dictionary", () => {
      dictMenu.searchByDictionaryEntry(usersData[0].dictionaryEntry);
      dictMenu.checkFirstDict();
      dictMenu.clickModifyButton();
      dictMenu.isActive(usersData[0].yes);
      dictMenu.clickUpdateButton();
    });

    it("Validate Modified Dictionary", () => {
      cy.reload();
      cy.wait(2000);
      dictMenu.searchByDictionaryEntry(usersData[0].dictionaryEntry);
      dictMenu.validateColumnContent("isActive", "1378", usersData[0].yes);
    });
  });

  describe("Deactivate Dictionary", function () {
    it("Check and Deactivate Second Dictionary", () => {
      dictMenu.searchByDictionaryEntry(usersData[1].dictionaryEntry);
      dictMenu.checkFirstDict();
      dictMenu.clickDeactivateButton();
    });

    // it("Validate Deactivated Dictionary", () => {
    //   cy.reload();
    //   cy.wait(2000);
    //   dictMenu.searchByDictionaryEntry(usersData[1].dictionaryEntry);
    //   dictMenu.validateColumnContent("isActive", "1398", usersData[0].no);
    // });
  });
});
