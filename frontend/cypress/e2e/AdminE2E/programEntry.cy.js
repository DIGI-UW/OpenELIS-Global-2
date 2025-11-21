import LoginPage from "../../pages/LoginPage";
import HomePage from "../../pages/HomePage";

let homePage = null;
let adminPage = null;
let programEntry = null;

// Test data: programs and their associated tests
const programs = [
  { name: "Routine Testing", test: "Biochemistry" },
  {
    name: "People living with HIV Program - Initial Visit",
    test: "Hematology",
  },
  {
    name: "People living with HIV Program - Follow-up Visit",
    test: "Serology-Immunology",
  },
  { name: "Cytology", test: null }, // No test selection needed
  { name: "Immunohistochemistry", test: null }, // No test selection needed
  { name: "Histopathology", test: null }, // No test selection needed
];

// Use cy.login() with cy.session() for login caching (10-20x faster - Testing Roadmap pattern)
before(() => {
  cy.login(); // Uses cy.session() - login runs ONCE, cached for all tests
  // Navigate to home page after login
  const loginPage = new LoginPage();
  homePage = loginPage.goToHomePage();
  adminPage = homePage.goToAdminPageProgram();

  // Navigate to the Program Entry page
  programEntry = adminPage.goToProgramEntry();
  programEntry.verifyPageLoads();
});

describe("Selects various Programs", () => {
  programs.forEach((program) => {
    it(`Selects program: ${program.name}`, () => {
      programEntry.selectProgram(program.name);
      if (program.test) {
        programEntry.selectTest(program.test);
      }
      programEntry.submitProgram();
    });
  });
});
