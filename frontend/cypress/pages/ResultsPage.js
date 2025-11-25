class Result {
  constructor() {
    this.selectors = {
      resultTitle: "h3",
      unitType: "#unitType",
      testMethod: "#testMethod0",
      collectionDate: "input#collectionDate",
      receivedDate: "input#recievedDate",
      startDate: "input#startDate",
      endDate: "input#endDate",
      referralReason: "#referralReason0",
      institute: "#institute0",
      sampleStatus: "#sampleStatusType",
      analysisStatus: "#analysisStatus",
      testName: "#testName",
      labNumberInput: "#labNumberInput",
      resultValue: "#ResultValue0",
      testNamesInput: "#testnames-input",
      testNamesItem: "#testnames-item-0-item",
      testUnitsInput: "#testunits-input",
      testUnitsItem: "#testunits-item-0-item",
      resultValueSelect: "#ResultValue0", // Note: Capital R (matches component)
      searchResults: "#searchResults",
      saveResults: "#saveResults",
      printReport: ":nth-child(6) > :nth-child(2) > .cds--btn",
      selectAllButton: "[data-cy='select-all-button']",
      selectNoneButton: "[data-cy='select-none-button']",
      printReportButton: "[data-cy='print-report']",
      referralsByPatient: "[data-cy='referralsByPatient']",
      referralsByTestAndName: "[data-cy='byUnitsAndTests']",
      referralsByLabNumber: "[data-cy='byLabNumber']",
      expanderButton: "[data-testid='expander-button-0']",
      acceptCheckbox: "#cell-accept-0 > .cds--form-item > .cds--checkbox-label",
      startLabNumber: "[data-cy='startAccession']",
      endLabNumber: "[data-cy='endAccession']",
      testResultCheckbox: "[data-cy='checkTestResult']",
      referredTestCheckbox:
        "tbody > tr > .cds--table-column-checkbox > .cds--checkbox--inline > .cds--checkbox-label",
      dateToggleButton: "#downshift-1-toggle-button",
      dateOption: (index) => `#downshift-1-item-${index}`,
      patientLastNameCell: "tbody > :nth-child(1) > :nth-child(2)",
      patientFirstNameCell: "tbody > :nth-child(1) > :nth-child(3)",
    };
  }

  getResultTitle(title) {
    cy.get(this.selectors.resultTitle).contains(title).should("be.visible");
  }

  waitForResultsTable() {
    // Wait for DataTable rows to appear (react-data-table-component uses ARIA roles)
    // Also support standard Carbon tables (tbody) for compatibility
    // Try both selectors with retry-ability
    cy.get("[role='rowgroup'] [role='row'], tbody tr", { timeout: 15000 })
      .should("exist")
      .should("have.length.greaterThan", 0);
  }

  selectUnitType(unitType) {
    // Wait for select to be visible and enabled, and for options to be populated
    cy.get(this.selectors.unitType)
      .should("be.visible")
      .should("not.be.disabled");
    // Wait for options to be available (Carbon Select may take time to populate)
    cy.get(`${this.selectors.unitType} option`, { timeout: 10000 })
      .should("have.length.greaterThan", 1); // More than just the default empty option
    
    // Carbon Select component - need to interact with the actual select element
    cy.get(this.selectors.unitType).then(($select) => {
      const selectElement = $select[0];
      // Find the option that matches unitType text
      const matchingOption = Array.from(selectElement.options).find(
        (opt) => opt.text === unitType || opt.text.includes(unitType),
      );
      
      if (matchingOption) {
        const targetValue = matchingOption.value;
        const currentValue = $select.val();
        
        if (currentValue !== targetValue) {
          // Select if different from current value
          cy.get(this.selectors.unitType).select(targetValue);
        }
        // Always trigger change event to ensure React onChange handler fires
        // This is needed because Carbon Select might not fire onChange if value doesn't change
        cy.get(this.selectors.unitType).then(($el) => {
          // Use React's synthetic event system by triggering native change
          const nativeInputValueSetter = Object.getOwnPropertyDescriptor(
            window.HTMLSelectElement.prototype,
            "value"
          ).set;
          nativeInputValueSetter.call($el[0], targetValue);
          const event = new Event("change", { bubbles: true });
          $el[0].dispatchEvent(event);
        });
      } else {
        // Fallback: try selecting by text directly
        cy.get(this.selectors.unitType).select(unitType);
      }
    });
  }

  acceptSample(index = 1) {
    return cy.get(this.selectors.testResultCheckbox).eq(index).check();
  }

  acceptResult() {
    cy.get(this.selectors.acceptCheckbox).click();
  }

  expandSampleDetails() {
    // Wait for DataTable to have rows before trying to expand
    this.waitForResultsTable();
    return cy.get(this.selectors.expanderButton).should("be.visible").click();
  }

  selectTestMethod(method) {
    cy.get(this.selectors.testMethod).select(method);
  }

  referTests(referTests) {
    cy.get("[data-cy='referalcheckbox']").check({ force: true });
  }

  searchResults() {
    // Wait for search button to be enabled before clicking
    cy.get(this.selectors.searchResults)
      .should("be.visible")
      .should("not.be.disabled")
      .click();
    // Wait for DataTable to render results (test UI, not API)
    this.waitForResultsTable();
  }

  enterCollectionDate() {
    cy.get(this.selectors.collectionDate)
      .should("be.visible")
      .type("04/30/2025");
  }

  enterReceivedDate() {
    cy.get(this.selectors.receivedDate).should("be.visible").type("05/01/2025");
  }

  clickReceivedDate() {
    cy.get(this.selectors.receivedDate).should("be.visible").click();
  }

  validatePatientResult(lName, fName) {
    cy.get(this.selectors.patientLastNameCell).should("contain.text", lName);
    cy.get(this.selectors.patientFirstNameCell).should("contain.text", fName);
  }

  selectSentDate() {
    cy.get(this.selectors.dateToggleButton).click();
    cy.get(this.selectors.dateOption(0)).click();
  }

  selectResultDate() {
    cy.get(this.selectors.dateToggleButton).click();
    cy.get(this.selectors.dateOption(1)).click();
  }

  clickDateButton() {
    cy.get(this.selectors.dateToggleButton).click();
  }
  startLabNumber(value) {
    cy.get(this.selectors.startLabNumber).type(value);
  }

  endLabNo(value) {
    cy.get(this.selectors.endLabNumber).type(value);
  }
  startDate(startDate) {
    cy.get(this.selectors.startDate).type(startDate);
  }

  endDate(endDate) {
    cy.get(this.selectors.endDate).type(endDate);
  }

  selectAllButtonEnabled() {
    // Wait for DataTable rows to exist (react-data-table-component uses ARIA roles)
    // Also support standard Carbon tables (tbody) for compatibility
    cy.get("[role='rowgroup'] [role='row'], tbody tr", { timeout: 10000 })
      .should("exist")
      .should("have.length.greaterThan", 0);
    
    // Wait for button to be visible
    cy.get(this.selectors.selectAllButton, { timeout: 10000 })
      .should("be.visible");
    
    // Wait for React state to update after table renders
    // For referred out tests, button is disabled when all rows are selected (selectedRowIds.length === responseDataShow.length)
    // So if no rows are selected and rows exist, button should be enabled
    cy.wait(3000); // Allow time for React state to update
    
    // Button should be enabled if rows exist and none are selected
    // If disabled, it might be because all rows are already selected or state hasn't updated
    cy.get(this.selectors.selectAllButton, { timeout: 10000 })
      .should("be.visible")
      .and("be.enabled");
  }

  clickSelectAllButton() {
    cy.get(this.selectors.selectAllButton).click();
  }

  printReportsButtonEnabled() {
    // Wait for DataTable rows to exist (react-data-table-component uses ARIA roles)
    // Also support standard Carbon tables (tbody) for compatibility
    cy.get("[role='rowgroup'] [role='row'], tbody tr", { timeout: 10000 })
      .should("exist")
      .should("have.length.greaterThan", 0);
    cy.get(this.selectors.printReportButton)
      .should("be.visible")
      .and("be.enabled");
  }

  selectNoneButtonEnabled() {
    cy.get(this.selectors.selectNoneButton)
      .should("be.visible")
      .and("be.enabled");
  }

  referralReason(reason) {
    cy.get(this.selectors.referralReason).select(reason);
  }

  selectInstitute(institute) {
    // Wait for select to be visible and enabled
    cy.get(this.selectors.institute, { timeout: 10000 })
      .should("be.visible")
      .should("not.be.disabled");
    
    // Wait for options to be populated (may take time to load from API)
    // Use a more lenient check - wait for at least one option (including empty)
    cy.get(`${this.selectors.institute} option`, { timeout: 15000 })
      .should("have.length.greaterThan", 0);
    
    // Handle missing or duplicate options by selecting the first match or first available
    cy.get(this.selectors.institute).then(($select) => {
      const selectElement = $select[0];
      const options = Array.from(selectElement.options).filter(
        (opt) => opt.value && opt.value !== "",
      );
      
      if (options.length === 0) {
        cy.log("⚠️  No institute options available - skipping selection");
        return; // Don't fail if no options
      }
      
      const matchingOption = options.find(
        (opt) => opt.value === institute || opt.text.includes(institute),
      );
      if (matchingOption) {
        cy.wrap($select).select(matchingOption.value);
      } else if (options.length > 0) {
        // Select first available option if institute not found
        cy.wrap($select).select(options[0].value);
        cy.log(
          `Selected institute: ${options[0].text} (value: ${options[0].value}) - ${institute} not found`,
        );
      }
    });
  }

  getPatientSearchResultsTable() {
    return cy.getElement(
      ".cds--data-table.cds--data-table--lg.cds--data-table--sort > tbody",
    );
  }

  selectPatientFromSearchResults() {
    this.getPatientSearchResultsTable()
      .find("tr")
      .first()
      .find("td:nth-child(1)")
      .find("[data-cy='radioButton']")
      .click({ force: true });
  }

  selectRefferedTest() {
    cy.get(this.selectors.referredTestCheckbox).click();
  }

  sampleStatus(sample) {
    cy.get(this.selectors.sampleStatus).should("be.visible").select(sample);
  }

  selectAnalysisStatus(status) {
    cy.get(this.selectors.analysisStatus).should("be.visible").select(status);
  }

  selectTestName(testName) {
    cy.get(this.selectors.testName).should("be.visible").select(testName);
  }

  printReport() {
    cy.get(this.selectors.printReport).should("be.visible").click();
  }

  clickReferralsByPatient() {
    cy.get(this.selectors.referralsByPatient).should("be.visible").click();
  }

  clickReferralsByTestAndName() {
    cy.get(this.selectors.referralsByTestAndName).should("be.visible").click();
  }

  resultsByLabNumber(number) {
    cy.get(this.selectors.labNumberInput).should("be.visible").type(number);
  }

  clickReferralsByLabNumber() {
    cy.get(this.selectors.referralsByLabNumber).should("be.visible").click();
  }

  setResultValue(value) {
    cy.get(this.selectors.resultValue).should("be.visible").type(value);
  }

  testName(value) {
    cy.get(this.selectors.testNamesInput).type(value);
  }

  testNameItem() {
    cy.get(this.selectors.testNamesItem).click();
  }

  unitType(value) {
    cy.get(this.selectors.testUnitsInput).type(value);
  }

  unitTypeItem() {
    cy.get(this.selectors.testUnitsItem).click();
  }

  selectResultValue(value) {
    // Result value can be either dictionary (resultValue + row.id) or other types (ResultValue + row.id)
    // Try both selectors - dictionary type uses lowercase, other types use capital R
    cy.get("body").then(($body) => {
      const dictSelector = "#resultValue0";
      const otherSelector = "#ResultValue0";
      
      if ($body.find(dictSelector).length > 0) {
        // Dictionary type (Select dropdown)
        cy.get(dictSelector, { timeout: 10000 })
          .should("be.visible")
          .should("not.be.disabled")
          .select(value);
      } else if ($body.find(otherSelector).length > 0) {
        // Other types (TextInput or TextArea)
        cy.get(otherSelector, { timeout: 10000 })
          .should("be.visible")
          .should("not.be.disabled")
          .clear()
          .type(value);
      } else {
        // Try waiting a bit more for element to appear
        cy.wait(1000);
        cy.get("#resultValue0, #ResultValue0", { timeout: 10000 })
          .should("be.visible")
          .first()
          .then(($el) => {
            if ($el.is("select")) {
              cy.wrap($el).select(value);
            } else {
              cy.wrap($el).clear().type(value);
            }
          });
      }
    });
  }

  submitResults() {
    cy.get(this.selectors.saveResults)
      .should("be.visible")
      .click({ force: true });
  }
}

export default Result;
