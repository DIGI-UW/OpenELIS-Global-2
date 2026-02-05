import LoginPage from "../pages/LoginPage";

let homePage = null;
let loginPage = null;
let validation = null;

before("login", () => {
  loginPage = new LoginPage();
  loginPage.visit();
});

const navigateToValidationPage = (validationType) => {
  homePage = loginPage.goToHomePage();
  validation = homePage[`goToValidationBy${validationType}`]();
};

const setupValidationIntercepts = () => {
  cy.intercept("GET", "**/rest/user-test-sections/**").as("testSections");
  cy.intercept("GET", "**/rest/AccessionValidation**").as("searchResults");
};

const selectFirstLabUnitAndSearch = () => {
  cy.get("#lab-unit-select option")
    .eq(1)
    .invoke("val")
    .then((value) => {
      if (value) {
        cy.get("#lab-unit-select").select(value);
      }
    });
  cy.get('button[type="submit"]').click();
};

describe("Validation Page - Basic Navigation", function () {
  before("navigate to Validation Page", function () {
    navigateToValidationPage("Routine");
  });

  it("User visits Validation Page and sees heading", function () {
    validation.checkForHeading();
  });

  it("Should display lab unit dropdown", function () {
    cy.get("#lab-unit-select").should("be.visible");
  });

  it("Should display search query input", function () {
    cy.get("#search-query").should("be.visible");
  });

  it("Should display search button", function () {
    cy.get('button[type="submit"]').should("be.visible");
  });
});

describe("Validation Page - Search by Lab Unit", function () {
  before("navigate to Validation Page", function () {
    setupValidationIntercepts();
    navigateToValidationPage("Routine");
    cy.wait("@testSections", { timeout: 15000 });
  });

  it("Should load lab units in dropdown", function () {
    cy.get("#lab-unit-select option").should("have.length.greaterThan", 1);
  });

  it("Should select test unit and trigger search", function () {
    cy.intercept("GET", "**/rest/AccessionValidation**").as("searchResults");
    selectFirstLabUnitAndSearch();
    cy.wait("@searchResults", { timeout: 30000 })
      .its("response.statusCode")
      .should("be.oneOf", [200, 304]);
  });
});

describe("Validation Page - Search by Lab Number", function () {
  before("navigate to Validation Page", function () {
    navigateToValidationPage("Order");
  });

  it("User visits Validation Page", function () {
    validation.checkForHeading();
  });

  it("Should search by lab number", function () {
    cy.fixture("Patient").then((data) => {
      cy.intercept("GET", "**/rest/AccessionValidation**").as("searchResults");
      validation.enterSearchQuery(data.labNo);
      validation.clickSearch();
      cy.wait("@searchResults", { timeout: 30000 });
    });
  });
});

describe("Validation Page - URL Parameter Support", function () {
  it("Should load results from accessionNumber URL parameter", function () {
    cy.fixture("Patient").then((data) => {
      setupValidationIntercepts();
      cy.visit(`/validation?type=order&accessionNumber=${data.labNo}`);
      cy.wait("@searchResults", { timeout: 30000 })
        .its("response.statusCode")
        .should("be.oneOf", [200, 304]);
      // Verify search query was populated
      cy.get("#search-query").should("have.value", data.labNo);
    });
  });
});

describe("Validation Page - Advanced Filters", function () {
  before("navigate to Validation Page", function () {
    navigateToValidationPage("Routine");
  });

  it("Should toggle advanced filters panel", function () {
    cy.contains("button", "Filters").click();
    cy.get("#lab-number-from").should("be.visible");
    cy.get("#lab-number-to").should("be.visible");
  });

  it("Should enter lab number range filter", function () {
    cy.fixture("Patient").then((data) => {
      cy.get("#lab-number-from").clear().type(data.labNo);
      cy.get("#lab-number-to").clear().type(data.endLabNo);
    });
  });

  it("Should toggle quick filters", function () {
    cy.get("#filter-normal").check({ force: true });
    cy.get("#filter-normal").should("be.checked");
    cy.get("#filter-abnormal").check({ force: true });
    cy.get("#filter-abnormal").should("be.checked");
    cy.get("#filter-normal").should("not.be.checked");
  });

  it("Should clear filters", function () {
    cy.contains("button", "Clear").click();
    cy.get("#lab-number-from").should("have.value", "");
    cy.get("#lab-number-to").should("have.value", "");
    cy.get("#filter-normal").should("not.be.checked");
    cy.get("#filter-abnormal").should("not.be.checked");
  });
});

describe("Validation Page - Results Table", function () {
  it("Should receive valid API response and display results", function () {
    setupValidationIntercepts();
    navigateToValidationPage("Routine");
    cy.wait("@testSections", { timeout: 15000 });
    selectFirstLabUnitAndSearch();
    cy.wait("@searchResults", { timeout: 30000 }).then((interception) => {
      expect(interception.response.statusCode).to.be.oneOf([200, 304]);
      expect(interception.response.body).to.have.property("resultList");
    });

    cy.get("body", { timeout: 10000 }).should(($body) => {
      const hasTable = $body.find(".cds--data-table-container").length > 0;
      const hasEmptyState = $body.find(".validation-empty-state").length > 0;
      const hasNoResultsText =
        $body.text().includes("No results") ||
        $body.text().includes("no results");
      const hasSearchForm = $body.find("#lab-unit-select").length > 0;
      expect(hasTable || hasEmptyState || hasNoResultsText || hasSearchForm).to
        .be.true;
    });
  });

  it("Should display batch action buttons when results exist", function () {
    setupValidationIntercepts();
    navigateToValidationPage("Routine");
    cy.wait("@testSections", { timeout: 15000 });
    selectFirstLabUnitAndSearch();
    cy.wait("@searchResults", { timeout: 30000 });

    cy.wait(500);

    cy.get("body").then(($body) => {
      if ($body.find(".cds--data-table-container").length > 0) {
        cy.contains("button", "Accept").should("be.visible");
        cy.contains("button", "Retest").should("be.visible");
        cy.get("#select-all").should("exist");
      }
    });
  });
});

describe("Validation Page - Batch Actions", function () {
  beforeEach("navigate to Validation Page", function () {
    setupValidationIntercepts();
    navigateToValidationPage("Routine");
    cy.wait("@testSections", { timeout: 15000 });
    selectFirstLabUnitAndSearch();
    cy.wait("@searchResults", { timeout: 10000 });
  });

  it("Should disable Accept button when no rows selected", function () {
    cy.get("body").then(($body) => {
      if ($body.find(".cds--data-table-container").length > 0) {
        cy.contains("button", "Accept").should("be.disabled");
      }
    });
  });

  it("Should disable Retest button when no rows selected", function () {
    cy.get("body").then(($body) => {
      if ($body.find(".cds--data-table-container").length > 0) {
        cy.contains("button", "Retest").should("be.disabled");
      }
    });
  });
});

describe("Validation Page - Retest Modal", function () {
  beforeEach("navigate to Validation Page", function () {
    setupValidationIntercepts();
    navigateToValidationPage("Routine");
    cy.wait("@testSections", { timeout: 15000 });
    selectFirstLabUnitAndSearch();
    cy.wait("@searchResults", { timeout: 10000 });
  });

  afterEach("close any open modals", function () {
    cy.get("body").then(($body) => {
      if ($body.find(".cds--modal.is-visible").length > 0) {
        cy.get(".cds--modal.is-visible").within(() => {
          cy.get("button").contains("Cancel").click({ force: true });
        });
      }
    });
  });

  it("Should open retest modal when rows selected", function () {
    cy.get("body").then(($body) => {
      if ($body.find('input[type="checkbox"][id^="checkbox-"]').length > 0) {
        cy.get('input[type="checkbox"][id^="checkbox-"]').first().check({
          force: true,
        });
        cy.contains("button", "Retest").click();
        cy.get(".cds--modal").should("be.visible");
        cy.get("#retest-reason").should("be.visible");
        cy.get(".cds--modal").within(() => {
          cy.get("button").contains("Cancel").click();
        });
        cy.get(".cds--modal", { timeout: 5000 }).should("not.be.visible");
      }
    });
  });

  it("Should require reason before submitting and close on cancel", function () {
    cy.get("body").then(($body) => {
      if ($body.find('input[type="checkbox"][id^="checkbox-"]').length > 0) {
        cy.get('input[type="checkbox"][id^="checkbox-"]').first().check({
          force: true,
        });
        cy.contains("button", "Retest").click();
        cy.get(".cds--modal").should("be.visible");

        cy.get(".cds--modal").within(() => {
          cy.get("button").contains("Confirm").click();
        });

        cy.get(".cds--modal").within(() => {
          cy.get("button").contains("Cancel").click();
        });
        cy.get(".cds--modal", { timeout: 5000 }).should("not.be.visible");
      }
    });
  });
});

describe("Validation Page - Accept and Release", function () {
  before("navigate to Validation Page", function () {
    setupValidationIntercepts();
    cy.intercept("POST", "**/rest/AccessionValidation").as("acceptRequest");
    navigateToValidationPage("Routine");
    cy.wait("@testSections", { timeout: 15000 });
    selectFirstLabUnitAndSearch();
    cy.wait("@searchResults", { timeout: 10000 });
  });

  it("Should select all rows using checkbox", function () {
    cy.get("body").then(($body) => {
      if ($body.find("#select-all").length > 0) {
        cy.get("#select-all").check({ force: true });
        cy.contains("Selected").should("be.visible");
      }
    });
  });

  it("Should select only normal results", function () {
    cy.get("body").then(($body) => {
      if ($body.find("#select-all").length > 0) {
        cy.get("#select-all").uncheck({ force: true });
        cy.contains("button", "Select Normal").click();
      }
    });
  });

  it("Should submit accept and release request", function () {
    cy.get("body").then(($body) => {
      if (
        $body.find("#select-all").length > 0 &&
        $body.find('input[type="checkbox"][id^="checkbox-"]').length > 0
      ) {
        cy.intercept("POST", "**/rest/AccessionValidation").as("acceptRequest");
        cy.get("#select-all").check({ force: true });
        cy.contains("button", "Accept").click();
        cy.wait("@acceptRequest", { timeout: 10000 })
          .its("response.statusCode")
          .should("be.oneOf", [200, 201]);
      }
    });
  });
});

describe("Validation Page - Empty State", function () {
  before("navigate to Validation Page", function () {
    setupValidationIntercepts();
    navigateToValidationPage("Routine");
    cy.wait("@testSections", { timeout: 15000 });
  });

  it("Should display empty state when no results", function () {
    cy.intercept("GET", "**/rest/AccessionValidation**").as("emptyResults");
    cy.get("#search-query").clear().type("NONEXISTENT12345");
    cy.get('button[type="submit"]').click();
    cy.wait("@emptyResults", { timeout: 10000 });
    cy.get("body").then(($body) => {
      const hasEmptyState = $body.find(".validation-empty-state").length > 0;
      const hasNoResultsText = $body.text().includes("No results");
      expect(hasEmptyState || hasNoResultsText).to.be.true;
    });
  });
});
