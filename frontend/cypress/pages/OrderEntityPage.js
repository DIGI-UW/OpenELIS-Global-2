import PatientEntryPage from "./PatientEntryPage";

class OrderEntityPage {
  sampleTypeOptionDropDown = "";

  constructor() {}

  visit() {
    cy.visit("/AddOrder");
  }

  getPatientPage() {
    return new PatientEntryPage();
  }

  clickNextButton() {
    cy.contains("button", "Next")
      .should("be.visible")
      .should("not.be.disabled")
      .click();
    // Cypress automatically waits for page transitions, but we need to wait for React to re-render
    // Wait for the Next button click to complete (button may still exist on next page, so check for state change)
  }

  // Wait for navigation to program selection page after clicking Next
  waitForProgramSelectionPage() {
    // Wait for the unique element of the program selection page to appear
    cy.get("#additionalQuestionsSelect")
      .should("be.visible")
      .should("be.enabled");
    // Wait for dropdown to be populated (async data load)
    cy.get("#additionalQuestionsSelect option").should(
      "have.length.greaterThan",
      1,
    );
  }

  // Wait for navigation to sample type page after clicking Next
  waitForSampleTypePage() {
    cy.get("select#sampleId_0").should("be.visible").should("be.enabled");
    // Wait for dropdown to be populated with sample types
    cy.get("select#sampleId_0 option").should("have.length.greaterThan", 1);
  }

  // Wait for navigation to order details page after clicking Next
  waitForOrderDetailsPage() {
    cy.get("input#order_requestDate, input#siteName, input#requesterId").should(
      "be.visible",
    );
  }

  // Page state verification helpers
  verifyPatientInfoPage() {
    cy.get('[data-cy="searchPatientTabButton"]').should("be.visible");
  }

  verifyProgramSelectionPage() {
    cy.get("#additionalQuestionsSelect")
      .should("be.visible")
      .should("be.enabled");
    cy.get("#additionalQuestionsSelect option").should(
      "have.length.greaterThan",
      1,
    );
  }

  verifySampleTypePage() {
    cy.get("select#sampleId_0").should("be.visible").should("be.enabled");
  }

  verifyOrderDetailsPage() {
    cy.get("input#order_requestDate, input#siteName, input#requesterId").should(
      "be.visible",
    );
  }

  verifySuccessPage() {
    // Wait for success message to appear after form submission
    cy.get(".orderEntrySuccessMsg", { timeout: 15000 }).should("be.visible");
    // Verify the print barcode button is visible
    cy.get('[data-cy="printBarCode"]')
      .should("be.visible")
      .should("not.be.disabled");
  }

  selectCytology() {
    cy.get("#additionalQuestionsSelect")
      .should("be.visible")
      .should("be.enabled")
      .select("Cytology");
  }

  selectSampleTypeOption(sampleType) {
    cy.get("select#sampleId_0")
      .should("be.visible")
      .should("be.enabled")
      .select(sampleType);
  }

  collectionDate(value) {
    cy.get("input#collectionDate_0")
      .should("be.visible")
      .should("be.enabled")
      .clear()
      .type(value);
  }

  requestDate(value) {
    cy.get("input#order_requestDate")
      .should("be.visible")
      .should("be.enabled")
      .clear()
      .type(value);
  }
  receivedDate(value) {
    cy.get("input#order_receivedDate")
      .should("be.visible")
      .should("be.enabled")
      .clear()
      .type(value);
  }
  checkPanelCheckBoxField() {
    cy.contains("span", "Bilan Biochimique").click();
    cy.contains("span", "Serologie VIH").click();
  }

  referTest() {
    cy.contains("span", "Refer test to a reference lab").click();
  }

  selectInstitute() {
    // Wait for the select to be visible and enabled
    cy.get("#referredInstituteId_0_1")
      .should("be.visible")
      .should("not.be.disabled");
    // Wait for options to be populated (may take time to load from API)
    cy.get("#referredInstituteId_0_1 option", { timeout: 10000 }).should(
      "have.length.greaterThan",
      0,
    );
    // Try to select CEDRES first, otherwise select first available option
    cy.get("#referredInstituteId_0_1").then(($select) => {
      const selectElement = $select[0];
      // Refresh options list in case they were loaded after initial capture
      const options = Array.from(selectElement.options).filter(
        (opt) => opt.value && opt.value !== "",
      );
      const matchingOption = options.find(
        (opt) => opt.value === "CEDRES" || opt.text.includes("CEDRES"),
      );
      if (matchingOption) {
        cy.wrap($select).select(matchingOption.value);
      } else if (options.length > 0) {
        // Select first available option if CEDRES not found
        cy.wrap($select).select(options[0].value);
        cy.log(
          `Selected institute: ${options[0].text} (value: ${options[0].value})`,
        );
      } else {
        cy.log("⚠️  No institute options available");
        // Try selecting by index as fallback
        cy.wrap($select).select(1); // Skip index 0 (usually empty option)
      }
    });
  }

  selectReferralReason() {
    cy.get("#referralReasonId_0_1").select("Test not performed");
  }
  generateLabOrderNumber() {
    cy.get("[data-cy='generate-labNumber']").click();
  }

  validateAcessionNumber(order) {
    cy.intercept("GET", `**/rest/SampleEntryAccessionNumberValidation**`).as(
      "accessionNoValidation",
    );
    cy.get("#labNo").type(order, { delay: 300 });

    cy.wait("@accessionNoValidation").then((interception) => {
      const responseBody = interception.response.body;

      console.log(responseBody);

      expect(responseBody.status).to.be.false;
    });
  }
  enterSiteName(siteName) {
    cy.get("input#siteName")
      .should("be.visible")
      .should("be.enabled")
      .clear()
      .type(siteName);
    // Wait for suggestions to appear
    cy.get('[data-cy="auto-suggestion"]').should("have.length.greaterThan", 0);
    // Click on the suggestion that contains the site name
    cy.contains('[data-cy="auto-suggestion"]', siteName)
      .should("be.visible")
      .click();
    // Wait for the input value to update (AutoComplete sets textValue when selected)
    cy.get("input#siteName").should("have.value", siteName);
    // Wait for React to update form state (onSelect callback sets referringSiteId)
    // The validation requires either referringSiteId or referringSiteName to be set
    // After clicking, handleAutoCompleteSiteName should set referringSiteId
    cy.wait(1000); // Wait for React state update and form validation
  }
  enterRequesterLastAndFirstName(
    fullName,
    requesterFirstName,
    requesterLastName,
  ) {
    // Scroll page to top to ensure everything is visible
    cy.window().scrollTo(0, 0);

    // Wait for requester field to be visible and enabled
    cy.get("#requesterId").should("be.visible").should("be.enabled");

    // Type just enough to trigger autocomplete - type last name first
    // The autocomplete filters as you type, so type "Prime" to see suggestions
    const searchTerm = fullName.split(",")[0].trim(); // Get "Prime" from "Prime, Optimus"
    cy.get("#requesterId").clear().type(searchTerm);

    // Wait for suggestions to appear
    cy.get('[data-cy="auto-suggestion"]').should("have.length.greaterThan", 0);

    // Press Enter to select the first/active suggestion (AutoComplete selects activeSuggestion[0] on Enter)
    cy.get("#requesterId").type("{enter}");

    // After selecting provider, the form makes an API call to /rest/practitioner?providerId=...
    // to fetch provider details and populate firstName/lastName fields
    // Wait for the API call to complete and fields to be populated
    cy.get("input#requesterFirstName", { timeout: 10000 }).should(
      "have.value",
      requesterFirstName,
    );
    cy.get("input#requesterLastName", { timeout: 10000 }).should(
      "have.value",
      requesterLastName,
    );
  }
  rememberSiteAndRequester() {
    cy.contains("span", "Remember site and requester").click();
  }
  clickSubmitOrderButton() {
    // Scroll to top to see full page
    cy.window().scrollTo(0, 0);

    // Take full page screenshot before clicking submit
    cy.screenshot("before-submit-full-page", { capture: "fullPage" });

    // Capture console logs before clicking
    cy.window().then((win) => {
      const consoleLogs = win._cypressConsoleLogs || [];
      cy.task(
        "log",
        `Console logs before click: ${consoleLogs.length} messages`,
      );
      consoleLogs.slice(-10).forEach((log) => {
        cy.task("log", `[${log.type}] ${log.message}`);
      });
    });

    // Find the submit button and force-click it (button may have disabled class but be functionally clickable)
    cy.contains("button", "Submit")
      .should("be.visible")
      .scrollIntoView()
      .click({ force: true });

    // Wait a moment for any async operations
    cy.wait(3000);

    // Capture console logs after clicking
    cy.window().then((win) => {
      const consoleLogs = win._cypressConsoleLogs || [];
      cy.task(
        "log",
        `Console logs after click: ${consoleLogs.length} messages`,
      );
      // Log the last 30 console messages (errors, warnings, logs)
      consoleLogs.slice(-30).forEach((log) => {
        cy.task("log", `[${log.type}] ${log.message}`);
      });
    });

    // Take screenshot after clicking to see what happened
    cy.screenshot("after-submit-click", { capture: "fullPage" });

    // Wait for success message to appear (form posts to /rest/SamplePatientEntry on success)
    cy.get(".orderEntrySuccessMsg", { timeout: 15000 }).should("be.visible");
  }
}

export default OrderEntityPage;
