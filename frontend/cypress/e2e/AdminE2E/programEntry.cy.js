import LoginPage from "../../pages/LoginPage";

describe("Program Entry Tests", () => {
  let loginPage, homePage, adminPage, programEntry;

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

  beforeEach(() => {
    // Navigate to the Program Entry page for each test
    loginPage = new LoginPage();
    loginPage.visit();
    homePage = loginPage.goToHomePage();
    adminPage = homePage.goToAdminPage();
    programEntry = adminPage.goToProgramEntry();
    programEntry.verifyPageLoads();
  });

  // Create a separate test for each program
  programs.forEach((program) => {
    it(`Can select and submit program: ${program.name}`, () => {
      programEntry.selectProgram(program.name);
      if (program.test) {
        programEntry.selectTest(program.test);
      }
      programEntry.submitProgram();

      // Add verification assertions here
      // e.g., cy.get('[data-testid="success-message"]').should('be.visible');
    });
  });
});
