diff --git a/frontend/cypress/e2e/validation.cy.js b/frontend/cypress/e2e/validation.cy.js
index ba58a0c3a3..1ac1da6fd8 100644
--- a/frontend/cypress/e2e/validation.cy.js
+++ b/frontend/cypress/e2e/validation.cy.js
@@ -1,12 +1,50 @@
 import LoginPage from "../pages/LoginPage";
-import HomePage from "../pages/HomePage";
-import PatientEntryPage from "../pages/PatientEntryPage";
-import Validation from "../pages/Validation";
 
 let homePage = null;
 let loginPage = null;
 let validation = null;
-let patientPage = new PatientEntryPage();
+
+// Mock data for validation results
+const mockValidationResults = {
+  statusCode: 200,
+  body: {
+    resultList: [
+      {
+        analysisId: "12345",
+        accessionNumber: "DEV01260000000000001",
+        testName: "Glucose",
+        result: "5.5",
+        normalRange: "3.9-6.1",
+        isNormal: true,
+        isAccepted: false,
+        patientName: "John Doe",
+      },
+      {
+        analysisId: "12346",
+        accessionNumber: "DEV01260000000000002",
+        testName: "Hemoglobin",
+        result: "14.2",
+        normalRange: "12.0-16.0",
+        isNormal: true,
+        isAccepted: false,
+        patientName: "Jane Smith",
+      },
+    ],
+    testSections: [],
+    testSectionsByName: [],
+    paging: { totalPages: "1", currentPage: "1" },
+  },
+};
+
+const mockEmptyResults = {
+  statusCode: 200,
+  body: {
+    resultList: [],
+    testSections: [],
+    testSectionsByName: [],
+    paging: { totalPages: "0", currentPage: "1" },
+  },
+};
 
 before("login", () => {
   loginPage = new LoginPage();
@@ -18,24 +56,72 @@ const navigateToValidationPage = (validationType) => {
   validation = homePage[`goToValidationBy${validationType}`]();
 };
 
-describe("Validation By Routine", function () {
+const setupValidationIntercepts = (mockResults = null) => {
+  cy.intercept("GET", "**/rest/user-test-sections/**").as("testSections");
+  if (mockResults) {
+    cy.intercept("GET", "**/rest/AccessionValidation**", mockResults).as(
+      "searchResults",
+    );
+  } else {
+    cy.intercept("GET", "**/rest/AccessionValidation**").as("searchResults");
+  }
+};
+
+const selectFirstLabUnitAndSearch = () => {
+  cy.get("#lab-unit-select option")
+    .eq(1)
+    .invoke("val")
+    .then((value) => {
+      if (value) {
+        cy.get("#lab-unit-select").select(value);
+      }
+    });
+  cy.get('button[type="submit"]').click();
+};
+
+describe("Validation Page - Basic Navigation", function () {
   before("navigate to Validation Page", function () {
     navigateToValidationPage("Routine");
   });
 
-  it("User visits Validation Page", function () {
+  it("User visits Validation Page and sees heading", function () {
     validation.checkForHeading();
   });
 
-  it("Should Select Test Unit From Drop-Down And Validate", function () {
-    cy.fixture("workplan").then((order) => {
-      validation.selectTestUnit(order.unitType);
-      //validation.validateTestUnit(order.testName);
-    });
+  it("Should display lab unit dropdown", function () {
+    cy.get("#lab-unit-select").should("be.visible");
+  });
+
+  it("Should display search query input", function () {
+    cy.get("#search-query").should("be.visible");
+  });
+
+  it("Should display search button", function () {
+    cy.get('button[type="submit"]').should("be.visible");
   });
 });
 
-describe("Validation By Order", function () {
+describe("Validation Page - Search by Lab Unit", function () {
+  before("navigate to Validation Page", function () {
+    setupValidationIntercepts();
+    navigateToValidationPage("Routine");
+    cy.wait("@testSections", { timeout: 15000 });
+  });
+
+  it("Should load lab units in dropdown", function () {
+    cy.get("#lab-unit-select option").should("have.length.greaterThan", 1);
+  });
+
+  it("Should select test unit and trigger search", function () {
+    cy.intercept("GET", "**/rest/AccessionValidation**").as("searchResults");
+    selectFirstLabUnitAndSearch();
+    cy.wait("@searchResults", { timeout: 30000 })
+      .its("response.statusCode")
+      .should("be.oneOf", [200, 304]);
+  });
+});
+
+describe("Validation Page - Search by Lab Number", function () {
   before("navigate to Validation Page", function () {
     navigateToValidationPage("Order");
   });
@@ -44,29 +130,193 @@ describe("Validation By Order", function () {
     validation.checkForHeading();
   });
 
-  it("Enter Lab Number, search and validate", function () {
-    cy.fixture("Patient").then((order) => {
-      validation.enterLabNumberAndSearch(order.labNo);
+  it("Should search by lab number", function () {
+    cy.fixture("Patient").then((data) => {
+      cy.intercept("GET", "**/rest/AccessionValidation**").as("searchResults");
+      validation.enterSearchQuery(data.labNo);
+      validation.clickSearch();
+      cy.wait("@searchResults", { timeout: 30000 });
     });
   });
 });
 
-describe("Validation By Range Of Order", function () {
+describe("Validation Page - Advanced Filters", function () {
   before("navigate to Validation Page", function () {
-    navigateToValidationPage("RangeOrder");
+    navigateToValidationPage("Routine");
   });
 
-  it("User visits Validation Page", function () {
-    validation.checkForHeading();
+  it("Should toggle advanced filters panel", function () {
+    cy.contains("button", "Filters").click();
+    cy.get("#lab-number-from").should("be.visible");
+    cy.get("#lab-number-to").should("be.visible");
   });
 
-  it("Should Enter Lab Number and perform a search", function () {
-    cy.fixture("Patient").then((order) => {
-      validation.enterLabNumberAndSearch(order.labNo);
+  it("Should enter lab number range filter", function () {
+    cy.fixture("Patient").then((data) => {
+      cy.get("#lab-number-from").clear().type(data.labNo);
+      cy.get("#lab-number-to").clear().type(data.endLabNo);
     });
   });
 
-  it("Should Save the results", function () {
-    validation.saveResults("Test Note");
+  it("Should toggle quick filters", function () {
+    cy.get("#filter-normal").check({ force: true });
+    cy.get("#filter-normal").should("be.checked");
+    cy.get("#filter-abnormal").check({ force: true });
+    cy.get("#filter-abnormal").should("be.checked");
+    cy.get("#filter-normal").should("not.be.checked");
+  });
+
+  it("Should clear filters", function () {
+    cy.contains("button", "Clear").click();
+    cy.get("#lab-number-from").should("have.value", "");
+    cy.get("#lab-number-to").should("have.value", "");
+    cy.get("#filter-normal").should("not.be.checked");
+    cy.get("#filter-abnormal").should("not.be.checked");
+  });
+});
+
+describe("Validation Page - Results Table", function () {
+  before("navigate to Validation Page and search", function () {
+    setupValidationIntercepts(mockValidationResults);
+    navigateToValidationPage("Routine");
+    cy.wait("@testSections", { timeout: 15000 });
+    selectFirstLabUnitAndSearch();
+    cy.wait("@searchResults", { timeout: 30000 });
+  });
+
+  it("Should display results table", function () {
+    cy.get(".cds--data-table-container", { timeout: 10000 }).should(
+      "be.visible",
+    );
+    cy.get('table[aria-label="validation results"]').should("be.visible");
+  });
+
+  it("Should display batch action buttons", function () {
+    cy.contains("button", "Accept").should("be.visible");
+    cy.contains("button", "Retest").should("be.visible");
+  });
+
+  it("Should have select all checkbox", function () {
+    cy.get("#select-all").should("exist");
+  });
+});
+
+describe("Validation Page - Batch Actions", function () {
+  it("Should disable Accept button when no rows selected", function () {
+    setupValidationIntercepts(mockValidationResults);
+    navigateToValidationPage("Routine");
+    cy.wait("@testSections", { timeout: 15000 });
+    selectFirstLabUnitAndSearch();
+    cy.wait("@searchResults", { timeout: 10000 });
+    cy.get(".cds--data-table-container", { timeout: 10000 }).should(
+      "be.visible",
+    );
+    cy.contains("button", "Accept").should("be.disabled");
+  });
+
+  it("Should disable Retest button when no rows selected", function () {
+    setupValidationIntercepts(mockValidationResults);
+    navigateToValidationPage("Routine");
+    cy.wait("@testSections", { timeout: 15000 });
+    selectFirstLabUnitAndSearch();
+    cy.wait("@searchResults", { timeout: 10000 });
+    cy.get(".cds--data-table-container", { timeout: 10000 }).should(
+      "be.visible",
+    );
+    cy.contains("button", "Retest").should("be.disabled");
+  });
+});
+
+describe("Validation Page - Retest Modal", function () {
+  before("navigate to Validation Page", function () {
+    setupValidationIntercepts(mockValidationResults);
+    navigateToValidationPage("Routine");
+    cy.wait("@testSections", { timeout: 15000 });
+    selectFirstLabUnitAndSearch();
+    cy.wait("@searchResults", { timeout: 10000 });
+  });
+
+  it("Should open retest modal when rows selected", function () {
+    cy.get(".cds--data-table-container", { timeout: 10000 }).should(
+      "be.visible",
+    );
+    cy.get('input[type="checkbox"][id^="checkbox-"]').first().check({
+      force: true,
+    });
+    cy.contains("button", "Retest").click();
+    cy.get(".cds--modal").should("be.visible");
+    cy.get("#retest-reason").should("be.visible");
+  });
+
+  it("Should require reason before submitting", function () {
+    cy.get(".cds--modal-footer .cds--btn--primary").click();
+    cy.get(".cds--text-area--invalid").should("exist");
+  });
+
+  it("Should close modal on cancel", function () {
+    cy.get(".cds--modal-footer .cds--btn--secondary").click();
+    cy.get(".cds--modal", { timeout: 5000 }).should("not.be.visible");
+  });
+});
+
+describe("Validation Page - Accept and Release", function () {
+  before("navigate to Validation Page", function () {
+    setupValidationIntercepts(mockValidationResults);
+    cy.intercept("POST", "**/rest/AccessionValidation", {
+      statusCode: 200,
+      body: { success: true },
+    }).as("acceptRequest");
+    navigateToValidationPage("Routine");
+    cy.wait("@testSections", { timeout: 15000 });
+    selectFirstLabUnitAndSearch();
+    cy.wait("@searchResults", { timeout: 10000 });
+  });
+
+  it("Should select all rows using checkbox", function () {
+    cy.get(".cds--data-table-container", { timeout: 10000 }).should(
+      "be.visible",
+    );
+    cy.get("#select-all").check({ force: true });
+    cy.contains("2 Selected").should("be.visible");
+  });
+
+  it("Should select only normal results", function () {
+    cy.get("#select-all").uncheck({ force: true });
+    cy.contains("button", "Select Normal").click();
+    cy.contains("Selected").should("be.visible");
+  });
+
+  it("Should submit accept and release request", function () {
+    cy.intercept("POST", "**/rest/AccessionValidation", {
+      statusCode: 200,
+      body: { success: true },
+    }).as("acceptRequest");
+    cy.get("#select-all").check({ force: true });
+    cy.contains("button", "Accept").click();
+    cy.wait("@acceptRequest", { timeout: 10000 })
+      .its("response.statusCode")
+      .should("eq", 200);
+  });
+});
+
+describe("Validation Page - Empty State", function () {
+  before("navigate to Validation Page", function () {
+    setupValidationIntercepts();
+    navigateToValidationPage("Routine");
+    cy.wait("@testSections", { timeout: 15000 });
+  });
+
+  it("Should display empty state when no results", function () {
+    cy.intercept("GET", "**/rest/AccessionValidation**", mockEmptyResults).as(
+      "emptyResults",
+    );
+    cy.get("#search-query").clear().type("NONEXISTENT12345");
+    cy.get('button[type="submit"]').click();
+    cy.wait("@emptyResults", { timeout: 10000 });
+    cy.get("body").then(($body) => {
+      const hasEmptyState = $body.find(".validation-empty-state").length > 0;
+      const hasNoResultsText = $body.text().includes("No results");
+      expect(hasEmptyState || hasNoResultsText).to.be.true;
+    });
   });
 });
diff --git a/frontend/cypress/pages/Validation.js b/frontend/cypress/pages/Validation.js
index 91e4e3d392..72e42542d9 100644
--- a/frontend/cypress/pages/Validation.js
+++ b/frontend/cypress/pages/Validation.js
@@ -1,28 +1,224 @@
 class Validation {
+  constructor() {
+    this.selectors = {
+      pageHeading: "section h2",
+      labUnitSelect: "#lab-unit-select",
+      searchQueryInput: "#search-query",
+      searchButton: 'button[type="submit"]',
+      filterButton: 'button:contains("Filters")',
+      labNumberFrom: "#lab-number-from",
+      labNumberTo: "#lab-number-to",
+      dateFrom: "#date-from",
+      dateTo: "#date-to",
+      testSectionSelect: "#test-section",
+      analyzerSelect: "#analyzer",
+      enteredBySelect: "#entered-by",
+      flaggedCheckbox: "#filter-flagged",
+      normalCheckbox: "#filter-normal",
+      abnormalCheckbox: "#filter-abnormal",
+      resultsTable: 'table[aria-label="validation results"]',
+      tableContainer: ".cds--data-table-container",
+      selectAllCheckbox: "#select-all",
+      retestButton: 'button:contains("Retest")',
+      acceptReleaseButton: 'button:contains("Accept")',
+      tableRow: ".cds--data-table tbody tr",
+      rowCheckbox: 'input[type="checkbox"]',
+      retestModal: ".cds--modal",
+      retestReasonInput: "#retest-reason",
+      retestConfirmButton: ".cds--modal-footer .cds--btn--primary",
+      retestCancelButton: ".cds--modal-footer .cds--btn--secondary",
+      emptyState: ".validation-empty-state",
+      notification: ".cds--inline-notification",
+      pagination: ".cds--pagination",
+    };
+  }
+
   checkForHeading() {
-    cy.get("section > h3", { timeout: 15000 }).should(
+    cy.get(this.selectors.pageHeading, { timeout: 15000 }).should("be.visible");
+  }
+
+  verifyPageTitle(expectedText = "Result Validation") {
+    cy.get(this.selectors.pageHeading, { timeout: 15000 }).should(
       "contain.text",
-      "Validation",
+      expectedText,
     );
   }
 
-  selectTestUnit(unitType) {
-    cy.get("#unitType").select(unitType);
+  selectLabUnit(unitName) {
+    cy.get(this.selectors.labUnitSelect).should("be.visible").select(unitName);
+  }
+
+  selectLabUnitByIndex(index) {
+    cy.get(this.selectors.labUnitSelect)
+      .should("be.visible")
+      .find("option")
+      .eq(index)
+      .then(($option) => {
+        const value = $option.val();
+        if (value) {
+          cy.get(this.selectors.labUnitSelect).select(value);
+        }
+      });
+  }
+
+  enterSearchQuery(query) {
+    cy.get(this.selectors.searchQueryInput).clear().type(query);
   }
 
-  validateTestUnit(unitType) {
-    cy.get("[data-testid='sampleInfo']").should("contain.text", unitType);
+  clickSearch() {
+    cy.get(this.selectors.searchButton).click();
+  }
+
+  searchByLabNumber(labNumber) {
+    this.enterSearchQuery(labNumber);
+    this.clickSearch();
   }
 
   enterLabNumberAndSearch(labNo) {
-    cy.get("#accessionNumber").type(labNo);
-    cy.get("[data-testid='Search-btn']").click();
+    this.searchByLabNumber(labNo);
+  }
+
+  selectTestUnit(unitType) {
+    this.selectLabUnit(unitType);
+  }
+
+  toggleAdvancedFilters() {
+    cy.get(this.selectors.filterButton).click();
+  }
+
+  enterLabNumberRange(from, to) {
+    if (from) {
+      cy.get(this.selectors.labNumberFrom).clear().type(from);
+    }
+    if (to) {
+      cy.get(this.selectors.labNumberTo).clear().type(to);
+    }
+  }
+
+  selectFilterOption(filterType, value) {
+    const selectorMap = {
+      testSection: this.selectors.testSectionSelect,
+      analyzer: this.selectors.analyzerSelect,
+      enteredBy: this.selectors.enteredBySelect,
+    };
+    if (selectorMap[filterType]) {
+      cy.get(selectorMap[filterType]).select(value);
+    }
+  }
+
+  toggleQuickFilter(filterType) {
+    const selectorMap = {
+      flagged: this.selectors.flaggedCheckbox,
+      normal: this.selectors.normalCheckbox,
+      abnormal: this.selectors.abnormalCheckbox,
+    };
+    if (selectorMap[filterType]) {
+      cy.get(selectorMap[filterType]).click();
+    }
+  }
+
+  waitForResults() {
+    cy.get(this.selectors.tableContainer, { timeout: 30000 }).should(
+      "be.visible",
+    );
+  }
+
+  verifyResultsDisplayed() {
+    cy.get(this.selectors.resultsTable).should("be.visible");
+    cy.get(this.selectors.tableRow).should("have.length.at.least", 1);
+  }
+
+  verifyEmptyState() {
+    cy.get(this.selectors.emptyState).should("be.visible");
+  }
+
+  selectAllRows() {
+    cy.get(this.selectors.selectAllCheckbox).check({ force: true });
+  }
+
+  deselectAllRows() {
+    cy.get(this.selectors.selectAllCheckbox).uncheck({ force: true });
+  }
+
+  selectRowByIndex(index) {
+    cy.get(this.selectors.tableRow)
+      .eq(index)
+      .find(this.selectors.rowCheckbox)
+      .first()
+      .check({ force: true });
+  }
+
+  clickRetest() {
+    cy.get(this.selectors.retestButton).click();
+  }
+
+  clickAcceptRelease() {
+    cy.get(this.selectors.acceptReleaseButton).click();
+  }
+
+  saveResults(note) {
+    this.selectRowByIndex(0);
+    this.clickAcceptRelease();
+  }
+
+  verifyRetestModalOpen() {
+    cy.get(this.selectors.retestModal).should("be.visible");
+  }
+
+  enterRetestReason(reason) {
+    cy.get(this.selectors.retestReasonInput).clear().type(reason);
+  }
+
+  confirmRetest() {
+    cy.get(this.selectors.retestConfirmButton).click();
+  }
+
+  cancelRetest() {
+    cy.get(this.selectors.retestCancelButton).click();
+  }
+
+  submitRetest(reason) {
+    this.verifyRetestModalOpen();
+    this.enterRetestReason(reason);
+    this.confirmRetest();
+  }
+
+  verifySuccessNotification() {
+    cy.get(".cds--inline-notification--success", { timeout: 10000 }).should(
+      "be.visible",
+    );
+  }
+
+  verifyErrorNotification() {
+    cy.get(".cds--inline-notification--error", { timeout: 10000 }).should(
+      "be.visible",
+    );
+  }
+
+  interceptSearchAPI() {
+    cy.intercept("GET", "/rest/AccessionValidation*").as("searchResults");
+  }
+
+  interceptAcceptAPI() {
+    cy.intercept("POST", "/rest/AccessionValidation").as("acceptRequest");
+  }
+
+  interceptRetestAPI() {
+    cy.intercept("POST", "/rest/AccessionValidation/retest").as(
+      "retestRequest",
+    );
+  }
+
+  waitForSearchResults() {
+    cy.wait("@searchResults", { timeout: 30000 });
+  }
+
+  waitForAcceptResponse() {
+    cy.wait("@acceptRequest", { timeout: 30000 });
   }
 
-  saveResults() {
-    // cy.get("[data-testid='Checkbox']").click();
-    //cy.get("#resultList0\\.note").type(note);
-    cy.get("[data-testid='Save-btn']").click();
+  waitForRetestResponse() {
+    cy.wait("@retestRequest", { timeout: 30000 });
   }
 }
 
diff --git a/frontend/src/components/validation/Index.js b/frontend/src/components/validation/Index.js
index d8558df14d..dbf26ebf5a 100644
--- a/frontend/src/components/validation/Index.js
+++ b/frontend/src/components/validation/Index.js
@@ -1,39 +1,73 @@
-import React, { useContext, useState, useEffect } from "react";
+import React, { useState } from "react";
+import { Grid, Column, Section, Heading } from "@carbon/react";
+import { FormattedMessage } from "react-intl";
 import SearchForm from "./SearchForm";
-import Validation from "./Validation";
+import ValidationPage from "./Validation";
 import { AlertDialog } from "../common/CustomNotification";
-import { NotificationContext } from "../layout/Layout";
-import { Heading, Grid, Column, Section } from "@carbon/react";
-import { injectIntl, FormattedMessage } from "react-intl";
-import PageBreadCrumb from "../common/PageBreadCrumb";
+import { getFromOpenElisServer } from "../utils/Utils";
 
-let breadcrumbs = [{ label: "home.label", link: "/" }];
-
-const Index = () => {
-  const { notificationVisible } = useContext(NotificationContext);
+function ValidationIndex() {
   const [results, setResults] = useState({ resultList: [] });
-  const [params, setParams] = useState("");
+  const [isLoading, setIsLoading] = useState(false);
+  const [searchParams, setSearchParams] = useState(null);
+
+  const handleRefresh = () => {
+    if (searchParams) {
+      setIsLoading(true);
+      getFromOpenElisServer(
+        `/rest/AccessionValidation?${searchParams}`,
+        (data) => {
+          setIsLoading(false);
+          if (data && data.resultList && data.resultList.length > 0) {
+            setResults(data);
+          } else {
+            setResults({ resultList: [] });
+          }
+        },
+      );
+    } else {
+      window.location.reload();
+    }
+  };
+
   return (
     <>
-      <PageBreadCrumb breadcrumbs={breadcrumbs} />
-      <Grid fullWidth={true}>
+      <AlertDialog />
+      <Grid fullWidth>
         <Column lg={16} md={8} sm={4}>
           <Section>
-            <Section>
-              <Heading>
-                <FormattedMessage id="sidenav.label.validation" />
-              </Heading>
-            </Section>
+            <Heading>
+              <FormattedMessage id="validation.page.title" />
+            </Heading>
           </Section>
         </Column>
       </Grid>
-      <div className="orderLegendBody">
-        {notificationVisible === true ? <AlertDialog /> : ""}
-        <SearchForm setParams={setParams} setResults={setResults} />
-        <Validation params={params} results={results} />
-      </div>
+
+      <SearchForm
+        setResults={setResults}
+        setIsLoading={setIsLoading}
+        setSearchParams={setSearchParams}
+      />
+
+      {isLoading ? (
+        <Section>
+          <Grid>
+            <Column lg={16}>
+              <div style={{ textAlign: "center", padding: "2rem" }}>
+                <FormattedMessage id="label.loading" />
+              </div>
+            </Column>
+          </Grid>
+        </Section>
+      ) : (
+        <ValidationPage
+          results={results}
+          setResults={setResults}
+          onRefresh={handleRefresh}
+        />
+      )}
     </>
   );
-};
+}
 
-export default Index;
+export default ValidationIndex;
diff --git a/frontend/src/components/validation/ResultDetailsPanel.js b/frontend/src/components/validation/ResultDetailsPanel.js
new file mode 100644
index 0000000000..2105adfdd1
--- /dev/null
+++ b/frontend/src/components/validation/ResultDetailsPanel.js
@@ -0,0 +1,543 @@
+import React, { useState, useEffect } from "react";
+import {
+  DataTable,
+  Table,
+  TableHead,
+  TableRow,
+  TableHeader,
+  TableBody,
+  TableCell,
+  Tag,
+  StructuredListWrapper,
+  StructuredListHead,
+  StructuredListRow,
+  StructuredListCell,
+  StructuredListBody,
+  Loading,
+  InlineNotification,
+} from "@carbon/react";
+import { FormattedMessage, useIntl } from "react-intl";
+import { getFromOpenElisServer } from "../utils/Utils";
+
+const ResultDetailsPanel = ({ analysisId, tab }) => {
+  const intl = useIntl();
+  const [details, setDetails] = useState(null);
+  const [isLoading, setIsLoading] = useState(false);
+  const [error, setError] = useState(null);
+
+  useEffect(() => {
+    if (analysisId) {
+      setIsLoading(true);
+      setError(null);
+
+      getFromOpenElisServer(
+        `/rest/AccessionValidation/${analysisId}/details`,
+        (data) => {
+          setIsLoading(false);
+          setDetails(data);
+        },
+        (error) => {
+          setIsLoading(false);
+          setError(error);
+        },
+      );
+    }
+  }, [analysisId]);
+
+  if (isLoading) {
+    return (
+      <div style={{ padding: "2rem", textAlign: "center" }}>
+        <Loading description={intl.formatMessage({ id: "label.loading" })} />
+      </div>
+    );
+  }
+
+  if (error) {
+    return (
+      <InlineNotification
+        kind="error"
+        title={intl.formatMessage({ id: "notification.error" })}
+        subtitle={
+          error.message ||
+          intl.formatMessage({ id: "validation.details.error" })
+        }
+      />
+    );
+  }
+
+  if (!details) {
+    return (
+      <div style={{ padding: "2rem", textAlign: "center" }}>
+        <FormattedMessage id="validation.details.nodata" />
+      </div>
+    );
+  }
+
+  switch (tab) {
+    case "method":
+      return renderMethodTab(details, intl);
+    case "orderInfo":
+      return renderOrderInfoTab(details, intl);
+    case "attachments":
+      return renderAttachmentsTab(details, intl);
+    case "history":
+      return renderHistoryTab(details, intl);
+    case "qc":
+      return renderQCTab(details, intl);
+    default:
+      return null;
+  }
+};
+
+const renderMethodTab = (details, intl) => {
+  const reagentLots = details.reagentLots || [];
+
+  if (reagentLots.length === 0) {
+    return (
+      <div style={{ padding: "1rem" }}>
+        <FormattedMessage id="validation.details.method.noreagents" />
+      </div>
+    );
+  }
+
+  const headers = [
+    {
+      key: "name",
+      header: intl.formatMessage({ id: "validation.details.method.reagent" }),
+    },
+    {
+      key: "lot",
+      header: intl.formatMessage({ id: "validation.details.method.lot" }),
+    },
+    {
+      key: "expires",
+      header: intl.formatMessage({ id: "validation.details.method.expires" }),
+    },
+    {
+      key: "status",
+      header: intl.formatMessage({ id: "validation.details.method.status" }),
+    },
+  ];
+
+  const rows = reagentLots.map((reagent, idx) => ({
+    id: `${idx}`,
+    name: reagent.name,
+    lot: reagent.lot,
+    expires: reagent.expires,
+    status: reagent.status,
+  }));
+
+  return (
+    <div style={{ padding: "1rem" }}>
+      <h5>
+        <FormattedMessage id="validation.details.method.title" />
+      </h5>
+      <DataTable rows={rows} headers={headers}>
+        {({ rows, headers, getTableProps, getHeaderProps, getRowProps }) => (
+          <Table {...getTableProps()}>
+            <TableHead>
+              <TableRow>
+                {headers.map((header) => (
+                  <TableHeader key={header.key} {...getHeaderProps({ header })}>
+                    {header.header}
+                  </TableHeader>
+                ))}
+              </TableRow>
+            </TableHead>
+            <TableBody>
+              {rows.map((row) => (
+                <TableRow key={row.id} {...getRowProps({ row })}>
+                  {row.cells.map((cell) => {
+                    if (cell.info.header === "status") {
+                      return (
+                        <TableCell key={cell.id}>
+                          <Tag
+                            type={
+                              cell.value === "ok"
+                                ? "green"
+                                : cell.value === "expiring-soon"
+                                  ? "yellow"
+                                  : "red"
+                            }
+                          >
+                            {cell.value}
+                          </Tag>
+                        </TableCell>
+                      );
+                    }
+                    return <TableCell key={cell.id}>{cell.value}</TableCell>;
+                  })}
+                </TableRow>
+              ))}
+            </TableBody>
+          </Table>
+        )}
+      </DataTable>
+    </div>
+  );
+};
+
+const renderOrderInfoTab = (details, intl) => {
+  const orderInfo = details.orderInfo || {};
+
+  return (
+    <div style={{ padding: "1rem" }}>
+      <h5>
+        <FormattedMessage id="validation.details.orderinfo.title" />
+      </h5>
+      <StructuredListWrapper>
+        <StructuredListHead>
+          <StructuredListRow head>
+            <StructuredListCell head>
+              <FormattedMessage id="validation.details.orderinfo.field" />
+            </StructuredListCell>
+            <StructuredListCell head>
+              <FormattedMessage id="validation.details.orderinfo.value" />
+            </StructuredListCell>
+          </StructuredListRow>
+        </StructuredListHead>
+        <StructuredListBody>
+          <StructuredListRow>
+            <StructuredListCell>
+              <FormattedMessage id="validation.details.orderinfo.clinician" />
+            </StructuredListCell>
+            <StructuredListCell>
+              {orderInfo.clinician || "-"}
+            </StructuredListCell>
+          </StructuredListRow>
+          <StructuredListRow>
+            <StructuredListCell>
+              <FormattedMessage id="validation.details.orderinfo.phone" />
+            </StructuredListCell>
+            <StructuredListCell>
+              {orderInfo.clinicianPhone || "-"}
+            </StructuredListCell>
+          </StructuredListRow>
+          <StructuredListRow>
+            <StructuredListCell>
+              <FormattedMessage id="validation.details.orderinfo.department" />
+            </StructuredListCell>
+            <StructuredListCell>
+              {orderInfo.department || "-"}
+            </StructuredListCell>
+          </StructuredListRow>
+          <StructuredListRow>
+            <StructuredListCell>
+              <FormattedMessage id="validation.details.orderinfo.priority" />
+            </StructuredListCell>
+            <StructuredListCell>
+              {orderInfo.priority ? (
+                <Tag type={orderInfo.priority === "Urgent" ? "red" : "green"}>
+                  {orderInfo.priority}
+                </Tag>
+              ) : (
+                "-"
+              )}
+            </StructuredListCell>
+          </StructuredListRow>
+          <StructuredListRow>
+            <StructuredListCell>
+              <FormattedMessage id="validation.details.orderinfo.collection" />
+            </StructuredListCell>
+            <StructuredListCell>
+              {orderInfo.collectionDate || "-"}
+            </StructuredListCell>
+          </StructuredListRow>
+          <StructuredListRow>
+            <StructuredListCell>
+              <FormattedMessage id="validation.details.orderinfo.received" />
+            </StructuredListCell>
+            <StructuredListCell>
+              {orderInfo.receivedDate || "-"}
+            </StructuredListCell>
+          </StructuredListRow>
+          <StructuredListRow>
+            <StructuredListCell>
+              <FormattedMessage id="validation.details.orderinfo.history" />
+            </StructuredListCell>
+            <StructuredListCell>
+              {orderInfo.clinicalHistory || "-"}
+            </StructuredListCell>
+          </StructuredListRow>
+          <StructuredListRow>
+            <StructuredListCell>
+              <FormattedMessage id="validation.details.orderinfo.diagnosis" />
+            </StructuredListCell>
+            <StructuredListCell>
+              {orderInfo.diagnosis || "-"}
+            </StructuredListCell>
+          </StructuredListRow>
+        </StructuredListBody>
+      </StructuredListWrapper>
+    </div>
+  );
+};
+
+const renderAttachmentsTab = (details, intl) => {
+  const attachments = details.attachments || [];
+
+  if (attachments.length === 0) {
+    return (
+      <div style={{ padding: "1rem" }}>
+        <FormattedMessage id="validation.details.attachments.none" />
+      </div>
+    );
+  }
+
+  const headers = [
+    {
+      key: "name",
+      header: intl.formatMessage({ id: "validation.details.attachments.name" }),
+    },
+    {
+      key: "type",
+      header: intl.formatMessage({ id: "validation.details.attachments.type" }),
+    },
+    {
+      key: "size",
+      header: intl.formatMessage({ id: "validation.details.attachments.size" }),
+    },
+    {
+      key: "uploadedBy",
+      header: intl.formatMessage({
+        id: "validation.details.attachments.uploadedby",
+      }),
+    },
+    {
+      key: "uploadedAt",
+      header: intl.formatMessage({
+        id: "validation.details.attachments.uploadedat",
+      }),
+    },
+  ];
+
+  const rows = attachments.map((attachment) => ({
+    id: attachment.id,
+    name: attachment.name,
+    type: attachment.type,
+    size: attachment.size,
+    uploadedBy: attachment.uploadedBy,
+    uploadedAt: attachment.uploadedAt,
+  }));
+
+  return (
+    <div style={{ padding: "1rem" }}>
+      <h5>
+        <FormattedMessage id="validation.details.attachments.title" />
+      </h5>
+      <DataTable rows={rows} headers={headers}>
+        {({ rows, headers, getTableProps, getHeaderProps, getRowProps }) => (
+          <Table {...getTableProps()}>
+            <TableHead>
+              <TableRow>
+                {headers.map((header) => (
+                  <TableHeader key={header.key} {...getHeaderProps({ header })}>
+                    {header.header}
+                  </TableHeader>
+                ))}
+              </TableRow>
+            </TableHead>
+            <TableBody>
+              {rows.map((row) => (
+                <TableRow key={row.id} {...getRowProps({ row })}>
+                  {row.cells.map((cell) => (
+                    <TableCell key={cell.id}>{cell.value}</TableCell>
+                  ))}
+                </TableRow>
+              ))}
+            </TableBody>
+          </Table>
+        )}
+      </DataTable>
+    </div>
+  );
+};
+
+const renderHistoryTab = (details, intl) => {
+  const previousResults = details.previousResults || [];
+
+  if (previousResults.length === 0) {
+    return (
+      <div style={{ padding: "1rem" }}>
+        <FormattedMessage id="validation.details.history.none" />
+      </div>
+    );
+  }
+
+  const headers = [
+    {
+      key: "date",
+      header: intl.formatMessage({ id: "validation.details.history.date" }),
+    },
+    {
+      key: "value",
+      header: intl.formatMessage({ id: "validation.details.history.value" }),
+    },
+    {
+      key: "status",
+      header: intl.formatMessage({ id: "validation.details.history.status" }),
+    },
+  ];
+
+  const rows = previousResults.map((result, idx) => ({
+    id: `${idx}`,
+    date: result.date,
+    value: result.value,
+    status: result.status,
+  }));
+
+  return (
+    <div style={{ padding: "1rem" }}>
+      <h5>
+        <FormattedMessage id="validation.details.history.title" />
+      </h5>
+
+      {details.deltaCheck && (
+        <InlineNotification
+          kind="warning"
+          title={intl.formatMessage({
+            id: "validation.details.history.deltacheck",
+          })}
+          subtitle={intl.formatMessage(
+            { id: "validation.details.history.deltacheck.message" },
+            {
+              previous: details.deltaCheck.previous,
+              change: details.deltaCheck.change,
+              threshold: details.deltaCheck.threshold,
+            },
+          )}
+          lowContrast
+        />
+      )}
+
+      <DataTable rows={rows} headers={headers}>
+        {({ rows, headers, getTableProps, getHeaderProps, getRowProps }) => (
+          <Table {...getTableProps()}>
+            <TableHead>
+              <TableRow>
+                {headers.map((header) => (
+                  <TableHeader key={header.key} {...getHeaderProps({ header })}>
+                    {header.header}
+                  </TableHeader>
+                ))}
+              </TableRow>
+            </TableHead>
+            <TableBody>
+              {rows.map((row) => (
+                <TableRow key={row.id} {...getRowProps({ row })}>
+                  {row.cells.map((cell) => {
+                    if (cell.info.header === "status") {
+                      return (
+                        <TableCell key={cell.id}>
+                          <Tag
+                            type={
+                              cell.value === "normal"
+                                ? "green"
+                                : cell.value === "abnormal"
+                                  ? "red"
+                                  : "blue"
+                            }
+                          >
+                            {cell.value}
+                          </Tag>
+                        </TableCell>
+                      );
+                    }
+                    return <TableCell key={cell.id}>{cell.value}</TableCell>;
+                  })}
+                </TableRow>
+              ))}
+            </TableBody>
+          </Table>
+        )}
+      </DataTable>
+    </div>
+  );
+};
+
+const renderQCTab = (details, intl) => {
+  const qcData = details.qcData || [];
+
+  if (qcData.length === 0) {
+    return (
+      <div style={{ padding: "1rem" }}>
+        <FormattedMessage id="validation.details.qc.none" />
+      </div>
+    );
+  }
+
+  const headers = [
+    {
+      key: "level",
+      header: intl.formatMessage({ id: "validation.details.qc.level" }),
+    },
+    {
+      key: "expected",
+      header: intl.formatMessage({ id: "validation.details.qc.expected" }),
+    },
+    {
+      key: "actual",
+      header: intl.formatMessage({ id: "validation.details.qc.actual" }),
+    },
+    {
+      key: "cv",
+      header: intl.formatMessage({ id: "validation.details.qc.cv" }),
+    },
+    {
+      key: "status",
+      header: intl.formatMessage({ id: "validation.details.qc.status" }),
+    },
+  ];
+
+  const rows = qcData.map((qc, idx) => ({
+    id: `${idx}`,
+    level: qc.level,
+    expected: qc.expected,
+    actual: qc.actual,
+    cv: qc.cv,
+    status: qc.status,
+  }));
+
+  return (
+    <div style={{ padding: "1rem" }}>
+      <h5>
+        <FormattedMessage id="validation.details.qc.title" />
+      </h5>
+      <DataTable rows={rows} headers={headers}>
+        {({ rows, headers, getTableProps, getHeaderProps, getRowProps }) => (
+          <Table {...getTableProps()}>
+            <TableHead>
+              <TableRow>
+                {headers.map((header) => (
+                  <TableHeader key={header.key} {...getHeaderProps({ header })}>
+                    {header.header}
+                  </TableHeader>
+                ))}
+              </TableRow>
+            </TableHead>
+            <TableBody>
+              {rows.map((row) => (
+                <TableRow key={row.id} {...getRowProps({ row })}>
+                  {row.cells.map((cell) => {
+                    if (cell.info.header === "status") {
+                      return (
+                        <TableCell key={cell.id}>
+                          <Tag type={cell.value === "pass" ? "green" : "red"}>
+                            {cell.value}
+                          </Tag>
+                        </TableCell>
+                      );
+                    }
+                    return <TableCell key={cell.id}>{cell.value}</TableCell>;
+                  })}
+                </TableRow>
+              ))}
+            </TableBody>
+          </Table>
+        )}
+      </DataTable>
+    </div>
+  );
+};
+
+export default ResultDetailsPanel;
diff --git a/frontend/src/components/validation/ResultRow.js b/frontend/src/components/validation/ResultRow.js
new file mode 100644
index 0000000000..4b68ca8e45
--- /dev/null
+++ b/frontend/src/components/validation/ResultRow.js
@@ -0,0 +1,322 @@
+import React, { useState } from "react";
+import {
+  Button,
+  TextArea,
+  TextInput,
+  Select,
+  SelectItem,
+  Tabs,
+  TabList,
+  Tab,
+  TabPanels,
+  TabPanel,
+  Tag,
+  InlineNotification,
+} from "@carbon/react";
+import { Locked, Unlocked, Renew, CheckmarkOutline } from "@carbon/icons-react";
+import { FormattedMessage, useIntl } from "react-intl";
+import ResultDetailsPanel from "./ResultDetailsPanel";
+
+const ResultRow = ({ result }) => {
+  const intl = useIntl();
+  const [isUnlocked, setIsUnlocked] = useState(false);
+  const [editedResult, setEditedResult] = useState(result.result);
+  const [editedNote, setEditedNote] = useState(result.note || "");
+  const [selectedInterpretation, setSelectedInterpretation] = useState("");
+
+  const patientInfo = result.patientInfoObject || {};
+  const enteredBy = result.enteredByObject || {};
+
+  return (
+    <div
+      className="result-row-expanded"
+      style={{ padding: "1rem", backgroundColor: "#f4f4f4" }}
+    >
+      <div
+        className="patient-banner"
+        style={{
+          marginBottom: "1rem",
+          padding: "1rem",
+          backgroundColor: "#e0e0e0",
+          borderRadius: "4px",
+        }}
+      >
+        <div
+          style={{
+            display: "flex",
+            justifyContent: "space-between",
+            alignItems: "center",
+          }}
+        >
+          <div>
+            <strong>
+              <FormattedMessage id="validation.row.patient.name" />:
+            </strong>{" "}
+            {patientInfo.name || result.patientName}
+          </div>
+          <div>
+            <strong>
+              <FormattedMessage id="validation.row.patient.id" />:
+            </strong>{" "}
+            {patientInfo.id}
+          </div>
+          <div>
+            <strong>
+              <FormattedMessage id="validation.row.patient.dob" />:
+            </strong>{" "}
+            {patientInfo.dob}
+          </div>
+          <div>
+            <strong>
+              <FormattedMessage id="validation.row.patient.sex" />:
+            </strong>{" "}
+            {patientInfo.sex}
+          </div>
+          <div>
+            <strong>
+              <FormattedMessage id="validation.row.patient.age" />:
+            </strong>{" "}
+            {patientInfo.age}
+          </div>
+        </div>
+      </div>
+
+      <div
+        className="entry-banner"
+        style={{
+          marginBottom: "1rem",
+          padding: "1rem",
+          backgroundColor: "#ffffff",
+          border: "1px solid #e0e0e0",
+          borderRadius: "4px",
+        }}
+      >
+        <div
+          style={{
+            display: "flex",
+            justifyContent: "space-between",
+            alignItems: "center",
+          }}
+        >
+          <div>
+            <strong>
+              <FormattedMessage id="validation.row.entry.by" />:
+            </strong>{" "}
+            {enteredBy.name}
+          </div>
+          <div>
+            <strong>
+              <FormattedMessage id="validation.row.entry.date" />:
+            </strong>{" "}
+            {enteredBy.date}
+          </div>
+          <div>
+            <strong>
+              <FormattedMessage id="validation.row.method" />:
+            </strong>{" "}
+            <Tag type={result.isManual ? "blue" : "cyan"}>
+              {result.method || (result.isManual ? "Manual" : "Analyzer")}
+            </Tag>
+          </div>
+          <Button
+            kind={isUnlocked ? "danger--tertiary" : "tertiary"}
+            size="sm"
+            renderIcon={isUnlocked ? Unlocked : Locked}
+            onClick={() => setIsUnlocked(!isUnlocked)}
+          >
+            {isUnlocked ? (
+              <FormattedMessage id="validation.row.locked" />
+            ) : (
+              <FormattedMessage id="validation.row.unlock" />
+            )}
+          </Button>
+        </div>
+      </div>
+
+      <div
+        className="result-value-section"
+        style={{
+          marginBottom: "1rem",
+          padding: "1rem",
+          backgroundColor: "#ffffff",
+          border: "1px solid #e0e0e0",
+          borderRadius: "4px",
+        }}
+      >
+        <div
+          style={{
+            display: "grid",
+            gridTemplateColumns: "1fr 1fr 1fr",
+            gap: "1rem",
+          }}
+        >
+          <TextInput
+            id={`result-value-${result.analysisId}`}
+            labelText={intl.formatMessage({
+              id: "validation.row.result.value",
+            })}
+            value={editedResult}
+            onChange={(e) => setEditedResult(e.target.value)}
+            disabled={!isUnlocked}
+          />
+          <TextInput
+            id={`normal-range-${result.analysisId}`}
+            labelText={intl.formatMessage({ id: "validation.row.normalrange" })}
+            value={result.normalRange}
+            disabled
+          />
+          <Select
+            id={`interpretation-${result.analysisId}`}
+            labelText={intl.formatMessage({
+              id: "validation.row.interpretation",
+            })}
+            value={selectedInterpretation}
+            onChange={(e) => setSelectedInterpretation(e.target.value)}
+            disabled={!isUnlocked}
+          >
+            <SelectItem
+              value=""
+              text={intl.formatMessage({
+                id: "validation.row.interpretation.none",
+              })}
+            />
+            <SelectItem
+              value="normal"
+              text={intl.formatMessage({
+                id: "validation.row.interpretation.normal",
+              })}
+            />
+            <SelectItem
+              value="abnormal"
+              text={intl.formatMessage({
+                id: "validation.row.interpretation.abnormal",
+              })}
+            />
+            <SelectItem
+              value="critical"
+              text={intl.formatMessage({
+                id: "validation.row.interpretation.critical",
+              })}
+            />
+          </Select>
+        </div>
+
+        {result.flags && result.flags.length > 0 && (
+          <div style={{ marginTop: "1rem" }}>
+            <strong>
+              <FormattedMessage id="validation.row.flags" />:
+            </strong>{" "}
+            {result.flags.map((flag, idx) => (
+              <Tag
+                key={idx}
+                type={
+                  flag === "critical"
+                    ? "red"
+                    : flag === "above-normal"
+                      ? "red"
+                      : flag === "below-normal"
+                        ? "blue"
+                        : "purple"
+                }
+              >
+                {flag}
+              </Tag>
+            ))}
+          </div>
+        )}
+      </div>
+
+      <div className="notes-section" style={{ marginBottom: "1rem" }}>
+        <TextArea
+          id={`notes-${result.analysisId}`}
+          labelText={intl.formatMessage({ id: "validation.row.notes" })}
+          placeholder={intl.formatMessage({
+            id: "validation.row.notes.placeholder",
+          })}
+          value={editedNote}
+          onChange={(e) => setEditedNote(e.target.value)}
+          rows={3}
+          disabled={!isUnlocked}
+        />
+
+        {result.pastNotes && result.pastNotes.length > 0 && (
+          <div style={{ marginTop: "0.5rem" }}>
+            <strong>
+              <FormattedMessage id="validation.row.notes.previous" />:
+            </strong>
+            {result.pastNotes.map((note, idx) => (
+              <InlineNotification
+                key={idx}
+                kind="info"
+                subtitle={note.value}
+                lowContrast
+                hideCloseButton
+              />
+            ))}
+          </div>
+        )}
+      </div>
+
+      <Tabs>
+        <TabList aria-label="Result details tabs">
+          <Tab>
+            <FormattedMessage id="validation.details.method.tab" />
+          </Tab>
+          <Tab>
+            <FormattedMessage id="validation.details.orderinfo.tab" />
+          </Tab>
+          <Tab>
+            <FormattedMessage id="validation.details.attachments.tab" />
+          </Tab>
+          <Tab>
+            <FormattedMessage id="validation.details.history.tab" />
+          </Tab>
+          <Tab>
+            <FormattedMessage id="validation.details.qc.tab" />
+          </Tab>
+        </TabList>
+        <TabPanels>
+          <TabPanel>
+            <ResultDetailsPanel analysisId={result.analysisId} tab="method" />
+          </TabPanel>
+          <TabPanel>
+            <ResultDetailsPanel
+              analysisId={result.analysisId}
+              tab="orderInfo"
+            />
+          </TabPanel>
+          <TabPanel>
+            <ResultDetailsPanel
+              analysisId={result.analysisId}
+              tab="attachments"
+            />
+          </TabPanel>
+          <TabPanel>
+            <ResultDetailsPanel analysisId={result.analysisId} tab="history" />
+          </TabPanel>
+          <TabPanel>
+            <ResultDetailsPanel analysisId={result.analysisId} tab="qc" />
+          </TabPanel>
+        </TabPanels>
+      </Tabs>
+
+      <div
+        style={{
+          display: "flex",
+          justifyContent: "flex-end",
+          gap: "0.5rem",
+          marginTop: "1rem",
+        }}
+      >
+        <Button kind="secondary" renderIcon={Renew} size="sm">
+          <FormattedMessage id="validation.row.action.retest" />
+        </Button>
+        <Button kind="primary" renderIcon={CheckmarkOutline} size="sm">
+          <FormattedMessage id="validation.row.action.accept" />
+        </Button>
+      </div>
+    </div>
+  );
+};
+
+export default ResultRow;
diff --git a/frontend/src/components/validation/RetestModal.js b/frontend/src/components/validation/RetestModal.js
new file mode 100644
index 0000000000..047ea64c6a
--- /dev/null
+++ b/frontend/src/components/validation/RetestModal.js
@@ -0,0 +1,92 @@
+import React, { useState } from "react";
+import { Modal, TextArea, InlineNotification } from "@carbon/react";
+import { FormattedMessage, useIntl } from "react-intl";
+
+const RetestModal = ({ isOpen, onClose, onConfirm, selectedCount }) => {
+  const intl = useIntl();
+  const [reason, setReason] = useState("");
+  const [showError, setShowError] = useState(false);
+
+  const handleSubmit = () => {
+    if (!reason || reason.trim() === "") {
+      setShowError(true);
+      return;
+    }
+
+    onConfirm(reason);
+    setReason("");
+    setShowError(false);
+  };
+
+  const handleClose = () => {
+    setReason("");
+    setShowError(false);
+    onClose();
+  };
+
+  return (
+    <Modal
+      open={isOpen}
+      onRequestClose={handleClose}
+      onRequestSubmit={handleSubmit}
+      modalHeading={intl.formatMessage({ id: "validation.retest.modal.title" })}
+      primaryButtonText={intl.formatMessage({
+        id: "validation.retest.modal.confirm",
+      })}
+      secondaryButtonText={intl.formatMessage({ id: "label.button.cancel" })}
+      size="md"
+    >
+      <div style={{ marginBottom: "1rem" }}>
+        <p>
+          <FormattedMessage
+            id="validation.retest.modal.description"
+            values={{ count: selectedCount }}
+          />
+        </p>
+      </div>
+
+      {showError && (
+        <InlineNotification
+          kind="error"
+          title={intl.formatMessage({ id: "notification.error" })}
+          subtitle={intl.formatMessage({
+            id: "validation.retest.modal.reason.required",
+          })}
+          lowContrast
+          style={{ marginBottom: "1rem" }}
+        />
+      )}
+
+      <TextArea
+        id="retest-reason"
+        labelText={intl.formatMessage({
+          id: "validation.retest.modal.reason.label",
+        })}
+        placeholder={intl.formatMessage({
+          id: "validation.retest.modal.reason.placeholder",
+        })}
+        value={reason}
+        onChange={(e) => {
+          setReason(e.target.value);
+          if (showError && e.target.value.trim() !== "") {
+            setShowError(false);
+          }
+        }}
+        rows={4}
+        required
+        invalid={showError}
+        invalidText={intl.formatMessage({
+          id: "validation.retest.modal.reason.required",
+        })}
+      />
+
+      <div style={{ marginTop: "1rem" }}>
+        <p style={{ fontSize: "0.875rem", color: "#525252" }}>
+          <FormattedMessage id="validation.retest.modal.note" />
+        </p>
+      </div>
+    </Modal>
+  );
+};
+
+export default RetestModal;
diff --git a/frontend/src/components/validation/SearchForm.js b/frontend/src/components/validation/SearchForm.js
index 9ac670548c..da4e931d35 100644
--- a/frontend/src/components/validation/SearchForm.js
+++ b/frontend/src/components/validation/SearchForm.js
@@ -2,418 +2,492 @@ import React, { useState, useEffect, useContext } from "react";
 import {
   Button,
   Column,
-  Form,
-  FormLabel,
-  Heading,
-  Row,
+  Grid,
   Section,
-  Stack,
-  TextInput,
-  SelectItem,
   Select,
-  Loading,
-  Grid,
-  Link,
+  SelectItem,
+  TextInput,
+  DatePicker,
+  DatePickerInput,
+  Checkbox,
+  Stack,
 } from "@carbon/react";
-import CustomLabNumberInput from "../common/CustomLabNumberInput";
+import { Filter, Search, Close } from "@carbon/icons-react";
 import { FormattedMessage, useIntl } from "react-intl";
-import { Formik, Field } from "formik";
-import ValidationSearchFormValues from "../formModel/innitialValues/ValidationSearchFormValues";
-import { getFromOpenElisServer, Roles } from "../utils/Utils";
+import { getFromOpenElisServer } from "../utils/Utils";
 import { NotificationContext } from "../layout/Layout";
 import { NotificationKinds } from "../common/CustomNotification";
-import { format } from "date-fns";
-import CustomDatePicker from "../common/CustomDatePicker";
-import { ArrowLeft, ArrowRight } from "@carbon/react/icons";
 
-const SearchForm = (props) => {
-  const { setNotificationVisible, addNotification } =
+const SearchForm = ({ setResults, setIsLoading, setSearchParams }) => {
+  const intl = useIntl();
+  const { addNotification, setNotificationVisible } =
     useContext(NotificationContext);
 
-  const intl = useIntl();
+  const [searchQuery, setSearchQuery] = useState("");
+  const [selectedLabUnit, setSelectedLabUnit] = useState("");
+  const [showAdvancedFilters, setShowAdvancedFilters] = useState(false);
+
+  const [labNumberFrom, setLabNumberFrom] = useState("");
+  const [labNumberTo, setLabNumberTo] = useState("");
+  const [dateFrom, setDateFrom] = useState("");
+  const [dateTo, setDateTo] = useState("");
+  const [testSection, setTestSection] = useState("");
+  const [analyzer, setAnalyzer] = useState("");
+  const [enteredBy, setEnteredBy] = useState("");
+  const [showFlaggedOnly, setShowFlaggedOnly] = useState(false);
+  const [showNormalOnly, setShowNormalOnly] = useState(false);
+  const [showAbnormalOnly, setShowAbnormalOnly] = useState(false);
 
-  const [searchResults, setSearchResults] = useState();
-  const [searchBy, setSearchBy] = useState();
-  const [doRange, setDoRagnge] = useState(true);
+  const [labUnits, setLabUnits] = useState([]);
   const [testSections, setTestSections] = useState([]);
-  const [defaultTestSectionId, setDefaultTestSectionId] = useState("");
-  const [defaultTestSectionLabel, setDefaultTestSectionLabel] = useState("");
-  const [searchFormValues, setSearchFormValues] = useState(
-    ValidationSearchFormValues,
-  );
-  const [testDate, setTestDate] = useState("");
-  const [isLoading, setIsLoading] = useState(false);
-  const [nextPage, setNextPage] = useState(null);
-  const [previousPage, setPreviousPage] = useState(null);
-  const [pagination, setPagination] = useState(false);
-  const [currentApiPage, setCurrentApiPage] = useState(null);
-  const [totalApiPages, setTotalApiPages] = useState(null);
-  const [url, setUrl] = useState("");
-
-  const validationResults = (data) => {
-    if (data) {
-      setSearchResults(data);
-      setIsLoading(false);
-      if (data.paging) {
-        var { totalPages, currentPage } = data.paging;
-        if (totalPages > 1) {
-          setPagination(true);
-          setCurrentApiPage(currentPage);
-          setTotalApiPages(totalPages);
-          if (parseInt(currentPage) < parseInt(totalPages)) {
-            setNextPage(parseInt(currentPage) + 1);
-          } else {
-            setNextPage(null);
-          }
-          if (parseInt(currentPage) > 1) {
-            setPreviousPage(parseInt(currentPage) - 1);
-          } else {
-            setPreviousPage(null);
-          }
-        }
-      }
-      if (data?.resultList?.length > 0) {
-        const newResultsList = data.resultList.map((data, id) => {
-          let tempData = { ...data };
-          tempData.id = id;
-          return tempData;
-        });
-        setSearchResults((prevState) => ({
-          ...prevState,
-          resultList: newResultsList,
-        }));
-      } else {
-        setIsLoading(false);
-        setSearchResults((prevState) => ({
-          ...prevState,
-          resultList: [],
-        }));
-
-        addNotification({
-          kind: NotificationKinds.warning,
-          title: intl.formatMessage({ id: "notification.title" }),
-          message: intl.formatMessage({ id: "validation.search.noresult" }),
-        });
-        setNotificationVisible(true);
+  const [analyzers] = useState([
+    "Sysmex XN-L",
+    "Sysmex XS-1000i",
+    "Cobas c 501",
+    "Cobas e 411",
+    "Manual",
+  ]);
+  const [users] = useState([
+    "J. Smith",
+    "M. Johnson",
+    "K. Davis",
+    "A. Williams",
+  ]);
+
+  useEffect(() => {
+    getFromOpenElisServer("/rest/user-test-sections/Validation", (data) => {
+      setLabUnits(data || []);
+      if (data && data.length > 0) {
+        setTestSections(data.map((unit) => unit.value));
       }
-    }
-  };
+    });
+  }, []);
 
   useEffect(() => {
-    props.setResults(searchResults);
-  }, [searchResults]);
+    const urlParams = new URLSearchParams(window.location.search);
+    const typeParam = urlParams.get("type");
+    const testParam = urlParams.get("test");
 
-  const handleSubmit = (values) => {
-    setNextPage(null);
-    setPreviousPage(null);
-    setPagination(false);
-    setIsLoading(true);
-    var accessionNumber = values.accessionNumber
-      ? values.accessionNumber.split("-")[0]
-      : "";
-    var unitType = values.unitType ? values.unitType : "";
-    var defaultDate = values.defaultDate ? values.defaultDate : "";
-    var date = testDate ? testDate : defaultDate;
-    let searchEndPoint =
-      "/rest/AccessionValidation?" +
-      "accessionNumber=" +
-      accessionNumber +
-      "&unitType=" +
-      unitType +
-      "&date=" +
-      date +
-      "&doRange=" +
-      doRange;
-    setUrl(searchEndPoint);
-    switch (searchBy) {
-      case "routine":
-        props.setParams("?type=" + searchBy + "&testSectionId=" + unitType);
-        break;
-      case "order":
-        props.setParams(
-          "?type=" + searchBy + "&accessionNumber=" + accessionNumber,
-        );
-        break;
-      case "testDate":
-        props.setParams("?type=" + searchBy + "&date=" + date);
-        break;
-      case "range":
-        props.setParams(
-          "?type=" + searchBy + "&accessionNumber=" + accessionNumber,
+    if (typeParam) {
+      if (labUnits.length > 0) {
+        const matchingUnit = labUnits.find(
+          (unit) =>
+            unit.value.toLowerCase() === typeParam.toLowerCase() ||
+            unit.label?.toLowerCase() === typeParam.toLowerCase(),
         );
-        break;
+        if (matchingUnit) {
+          setSelectedLabUnit(matchingUnit.id);
+          setTimeout(() => {
+            handleSearch();
+          }, 100);
+        }
+      }
     }
-    getFromOpenElisServer(searchEndPoint, validationResults);
-  };
 
-  const handleChange = () => {};
+    if (testParam && testParam.trim()) {
+      setSearchQuery(testParam);
+    }
+  }, [labUnits]);
 
-  const loadNextResultsPage = () => {
-    setIsLoading(true);
-    getFromOpenElisServer(url + "&page=" + nextPage, validationResults);
-  };
+  const handleSearch = (e) => {
+    if (e) e.preventDefault();
 
-  const loadPreviousResultsPage = () => {
-    setIsLoading(true);
-    getFromOpenElisServer(url + "&page=" + previousPage, validationResults);
-  };
-  const fetchTestSections = (response) => {
-    setTestSections(response);
-  };
+    if (!selectedLabUnit && !searchQuery) {
+      addNotification({
+        kind: NotificationKinds.warning,
+        title: intl.formatMessage({ id: "notification.title" }),
+        message: intl.formatMessage({
+          id: "validation.search.required",
+        }),
+      });
+      setNotificationVisible(true);
+      return;
+    }
 
-  const submitOnSelect = (e) => {
-    setNextPage(null);
-    setPreviousPage(null);
-    setPagination(false);
-    var values = { unitType: e.target.value };
-    handleSubmit(values);
-  };
+    setIsLoading(true);
 
-  function handleDatePickerChange(date) {
-    setTestDate(date);
-  }
+    const params = new URLSearchParams();
 
-  useEffect(() => {
-    var param = "";
-    if (window.location.pathname == "/validation") {
-      param = new URLSearchParams(window.location.search).get("type");
-    } else if (window.location.pathname == "/ResultValidation") {
-      param = "routine";
-    } else if (window.location.pathname == "/AccessionValidation") {
-      param = "order";
-    } else if (window.location.pathname == "/AccessionValidationRange") {
-      param = "range";
-    } else if (window.location.pathname == "/ResultValidationByTestDate") {
-      param = "testDate";
+    if (searchQuery) {
+      params.append("q", searchQuery);
     }
-    setSearchBy(param);
-    if (param === "order") {
-      setDoRagnge(false);
+    if (selectedLabUnit) {
+      params.append("labUnit", selectedLabUnit);
+    }
+    if (labNumberFrom) {
+      params.append("labNumberFrom", labNumberFrom);
+    }
+    if (labNumberTo) {
+      params.append("labNumberTo", labNumberTo);
+    }
+    if (dateFrom) {
+      params.append("dateFrom", dateFrom);
+    }
+    if (dateTo) {
+      params.append("dateTo", dateTo);
+    }
+    if (testSection) {
+      params.append("testSection", testSection);
+    }
+    if (analyzer) {
+      params.append("analyzer", analyzer);
+    }
+    if (enteredBy) {
+      params.append("enteredBy", enteredBy);
+    }
+    if (showFlaggedOnly) {
+      params.append("flagged", "true");
+    }
+    if (showNormalOnly) {
+      params.append("normal", "true");
+    }
+    if (showAbnormalOnly) {
+      params.append("normal", "false");
     }
-    switch (searchBy) {
-      case "routine": {
-        let testSectionId = new URLSearchParams(window.location.search).get(
-          "testSectionId",
-        );
-        testSectionId = testSectionId ? testSectionId : "";
-        getFromOpenElisServer(
-          "/rest/user-test-sections/" + Roles.VALIDATION,
-          (fetchedTestSections) => {
-            let testSection = fetchedTestSections.find(
-              (testSection) => testSection.id === testSectionId,
-            );
-            let testSectionLabel = testSection ? testSection.value : "";
-            setDefaultTestSectionId(testSectionId);
-            setDefaultTestSectionLabel(testSectionLabel);
-            fetchTestSections(fetchedTestSections);
-          },
-        );
-        if (testSectionId) {
-          let values = { unitType: testSectionId };
-          handleSubmit(values);
-        }
-        break;
-      }
 
-      case "order":
-      case "range": {
-        let accessionNumber = new URLSearchParams(window.location.search).get(
-          "accessionNumber",
-        );
-        if (accessionNumber) {
-          let searchValues = {
-            ...searchFormValues,
-            accessionNumber: accessionNumber,
-          };
-          handleSubmit(searchValues);
-          setSearchFormValues(searchValues);
-        }
-        break;
-      }
-      case "testDate": {
-        let date = new URLSearchParams(window.location.search).get("date");
-        if (date) {
-          setTestDate(date);
-          handleSubmit({ defaultDate: date });
+    if (setSearchParams) {
+      setSearchParams(params.toString());
+    }
+
+    getFromOpenElisServer(
+      `/rest/AccessionValidation?${params.toString()}`,
+      (data) => {
+        setIsLoading(false);
+        if (data && data.resultList && data.resultList.length > 0) {
+          setResults(data);
+        } else {
+          setResults({ resultList: [] });
+          addNotification({
+            kind: NotificationKinds.info,
+            title: intl.formatMessage({ id: "notification.title" }),
+            message: intl.formatMessage({
+              id: "validation.search.noresults",
+            }),
+          });
+          setNotificationVisible(true);
         }
-        break;
-      }
+      },
+    );
+  };
+
+  const handleClear = () => {
+    setSearchQuery("");
+    setSelectedLabUnit("");
+    setLabNumberFrom("");
+    setLabNumberTo("");
+    setDateFrom("");
+    setDateTo("");
+    setTestSection("");
+    setAnalyzer("");
+    setEnteredBy("");
+    setShowFlaggedOnly(false);
+    setShowNormalOnly(false);
+    setShowAbnormalOnly(false);
+    setResults({ resultList: [] });
+  };
+
+  const handleLabUnitChange = (e) => {
+    const value = e.target.value;
+    setSelectedLabUnit(value);
+    if (value) {
+      setTimeout(() => {
+        handleSearch();
+      }, 100);
+    }
+  };
+
+  const handleQuickFilterChange = (filterType) => {
+    if (filterType === "flagged") {
+      setShowFlaggedOnly(!showFlaggedOnly);
+      setShowNormalOnly(false);
+      setShowAbnormalOnly(false);
+    } else if (filterType === "normal") {
+      setShowNormalOnly(!showNormalOnly);
+      setShowFlaggedOnly(false);
+      setShowAbnormalOnly(false);
+    } else if (filterType === "abnormal") {
+      setShowAbnormalOnly(!showAbnormalOnly);
+      setShowFlaggedOnly(false);
+      setShowNormalOnly(false);
     }
+  };
 
-    setNextPage(null);
-    setPreviousPage(null);
-    setPagination(false);
-  }, [searchBy, doRange]);
   return (
-    <>
-      {isLoading && <Loading></Loading>}
-      <Formik
-        initialValues={searchFormValues}
-        enableReinitialize={true}
-        //validationSchema={}
-        onSubmit={handleSubmit}
-        onChange
-      >
-        {({
-          values,
-          errors,
-          touched,
-          setFieldValue,
-          handleChange,
-          //handleBlur,
-          handleSubmit,
-        }) => (
-          <Form
-            onSubmit={handleSubmit}
-            onChange={handleChange}
-            //onBlur={handleBlur}
-          >
-            <Stack gap={2}>
+    <Section className="validation-search-section">
+      <Grid className="validation-search-grid">
+        <Column lg={16}>
+          <form onSubmit={handleSearch}>
+            <Stack gap={4}>
               <Grid>
-                <Column lg={16}>
-                  <h4>
+                <Column lg={4} md={4} sm={4}>
+                  <Select
+                    id="lab-unit-select"
+                    labelText={intl.formatMessage({
+                      id: "validation.search.labunit",
+                    })}
+                    value={selectedLabUnit}
+                    onChange={handleLabUnitChange}
+                  >
+                    <SelectItem
+                      text={intl.formatMessage({
+                        id: "validation.search.labunit.placeholder",
+                      })}
+                      value=""
+                    />
+                    {labUnits.map((unit) => (
+                      <SelectItem
+                        key={unit.id}
+                        text={unit.value}
+                        value={unit.id}
+                      />
+                    ))}
+                  </Select>
+                </Column>
+
+                <Column lg={1} md={1} sm={1} className="or-separator">
+                  <div style={{ textAlign: "center", paddingTop: "2rem" }}>
+                    <FormattedMessage id="label.or" />
+                  </div>
+                </Column>
+
+                <Column lg={7} md={7} sm={7}>
+                  <TextInput
+                    id="search-query"
+                    labelText={intl.formatMessage({
+                      id: "validation.search.query",
+                    })}
+                    placeholder={intl.formatMessage({
+                      id: "validation.search.query.placeholder",
+                    })}
+                    value={searchQuery}
+                    onChange={(e) => setSearchQuery(e.target.value)}
+                  />
+                </Column>
+
+                <Column lg={2} md={2} sm={2}>
+                  <Button
+                    type="submit"
+                    style={{ marginTop: "1.5rem" }}
+                    renderIcon={Search}
+                    disabled={!selectedLabUnit && !searchQuery}
+                  >
                     <FormattedMessage id="label.button.search" />
-                  </h4>
+                  </Button>
+                </Column>
+
+                <Column lg={2} md={2} sm={2}>
+                  <Button
+                    kind={showAdvancedFilters ? "primary" : "secondary"}
+                    style={{ marginTop: "1.5rem" }}
+                    renderIcon={Filter}
+                    onClick={() => setShowAdvancedFilters(!showAdvancedFilters)}
+                  >
+                    <FormattedMessage id="validation.filters.toggle" />
+                  </Button>
                 </Column>
+              </Grid>
+
+              {showAdvancedFilters && (
+                <Grid className="advanced-filters-panel">
+                  <Column lg={16}>
+                    <h4>
+                      <FormattedMessage id="validation.filters.advanced" />
+                    </h4>
+                  </Column>
 
-                {(searchBy === "order" || searchBy === "range") && (
-                  <>
-                    <Column lg={6} md={8} sm={4}>
-                      <Field name="accessionNumber">
-                        {({ field }) => (
-                          <CustomLabNumberInput
-                            placeholder={"Enter Lab No"}
-                            name={field.name}
-                            id={field.name}
-                            value={values[field.name]}
-                            onChange={(e, rawValue) => {
-                              setFieldValue(field.name, rawValue);
-                            }}
-                            labelText={
-                              searchBy == "order" ? (
-                                <FormattedMessage id="search.label.accession" />
-                              ) : (
-                                <FormattedMessage id="search.label.loadnext" />
-                              )
-                            }
-                          />
-                        )}
-                      </Field>
-                    </Column>
-                    <Column lg={10} />
-                  </>
-                )}
-
-                {searchBy === "testDate" && (
-                  <>
-                    <Column lg={6} md={8} sm={4}>
-                      <Field name="date">
-                        {({ field }) => (
-                          <CustomDatePicker
-                            id={field.id}
-                            labelText={intl.formatMessage({
-                              id: "search.label.testdate",
-                            })}
-                            value={testDate}
-                            onChange={(date) => handleDatePickerChange(date)}
-                            name={field.name}
-                          />
-                        )}
-                      </Field>
-                    </Column>
-                    <Column lg={10} />
-                  </>
-                )}
-                {searchBy !== "routine" && (
-                  <Column lg={16} md={8} sm={4}>
-                    <Button
-                      type="submit"
-                      id="submit"
-                      style={{ marginTop: "16px" }}
-                      data-testid="Search-btn"
+                  <Column lg={4} md={4} sm={4}>
+                    <TextInput
+                      id="lab-number-from"
+                      labelText={intl.formatMessage({
+                        id: "validation.filter.labnumber.from",
+                      })}
+                      placeholder="DEV0125000"
+                      value={labNumberFrom}
+                      onChange={(e) => setLabNumberFrom(e.target.value)}
+                    />
+                  </Column>
+
+                  <Column lg={4} md={4} sm={4}>
+                    <TextInput
+                      id="lab-number-to"
+                      labelText={intl.formatMessage({
+                        id: "validation.filter.labnumber.to",
+                      })}
+                      placeholder="DEV0125999"
+                      value={labNumberTo}
+                      onChange={(e) => setLabNumberTo(e.target.value)}
+                    />
+                  </Column>
+
+                  <Column lg={4} md={4} sm={4}>
+                    <DatePicker
+                      datePickerType="single"
+                      onChange={(dates) => {
+                        if (dates && dates.length > 0) {
+                          setDateFrom(dates[0].toISOString().split("T")[0]);
+                        }
+                      }}
                     >
-                      <FormattedMessage id="label.button.search" />
-                    </Button>
+                      <DatePickerInput
+                        id="date-from"
+                        labelText={intl.formatMessage({
+                          id: "validation.filter.date.from",
+                        })}
+                        placeholder="mm/dd/yyyy"
+                      />
+                    </DatePicker>
                   </Column>
-                )}
-              </Grid>
-            </Stack>
-          </Form>
-        )}
-      </Formik>
-
-      {searchBy === "routine" && (
-        <>
-          <Grid>
-            <Column lg={6} md={8} sm={4}>
-              <Select
-                labelText={intl.formatMessage({ id: "search.label.testunit" })}
-                name="unitType"
-                id="unitType"
-                onChange={submitOnSelect}
-              >
-                <SelectItem
-                  text={defaultTestSectionLabel}
-                  value={defaultTestSectionId}
-                />
-                {testSections
-                  .filter((item) => item.id !== defaultTestSectionId)
-                  .map((test, index) => {
-                    return (
+
+                  <Column lg={4} md={4} sm={4}>
+                    <DatePicker
+                      datePickerType="single"
+                      onChange={(dates) => {
+                        if (dates && dates.length > 0) {
+                          setDateTo(dates[0].toISOString().split("T")[0]);
+                        }
+                      }}
+                    >
+                      <DatePickerInput
+                        id="date-to"
+                        labelText={intl.formatMessage({
+                          id: "validation.filter.date.to",
+                        })}
+                        placeholder="mm/dd/yyyy"
+                      />
+                    </DatePicker>
+                  </Column>
+
+                  <Column lg={4} md={4} sm={4}>
+                    <Select
+                      id="test-section"
+                      labelText={intl.formatMessage({
+                        id: "validation.filter.testsection",
+                      })}
+                      value={testSection}
+                      onChange={(e) => setTestSection(e.target.value)}
+                    >
                       <SelectItem
-                        key={index}
-                        text={test.value}
-                        value={test.id}
+                        text={intl.formatMessage({
+                          id: "validation.filter.all",
+                        })}
+                        value=""
                       />
-                    );
-                  })}
-              </Select>
-            </Column>
-            <Column lg={10} />
-          </Grid>
-        </>
-      )}
-
-      <>
-        {pagination && (
-          <Grid>
-            <Column lg={14} />
-            <Column
-              lg={2}
-              style={{
-                display: "flex",
-                flexDirection: "column",
-                alignItems: "center",
-                gap: "10px",
-                width: "110%",
-              }}
-            >
-              <Link>
-                {currentApiPage} / {totalApiPages}
-              </Link>
-              <div style={{ display: "flex", gap: "10px" }}>
-                <Button
-                  hasIconOnly
-                  id="loadpreviousresults"
-                  onClick={loadPreviousResultsPage}
-                  disabled={previousPage != null ? false : true}
-                  renderIcon={ArrowLeft}
-                  iconDescription="previous"
-                ></Button>
-                <Button
-                  hasIconOnly
-                  id="loadnextresults"
-                  onClick={loadNextResultsPage}
-                  disabled={nextPage != null ? false : true}
-                  renderIcon={ArrowRight}
-                  iconDescription="next"
-                ></Button>
-              </div>
-            </Column>
-          </Grid>
-        )}
-      </>
-    </>
+                      {testSections.map((section) => (
+                        <SelectItem
+                          key={section}
+                          text={section}
+                          value={section}
+                        />
+                      ))}
+                    </Select>
+                  </Column>
+
+                  <Column lg={4} md={4} sm={4}>
+                    <Select
+                      id="analyzer"
+                      labelText={intl.formatMessage({
+                        id: "validation.filter.analyzer",
+                      })}
+                      value={analyzer}
+                      onChange={(e) => setAnalyzer(e.target.value)}
+                    >
+                      <SelectItem
+                        text={intl.formatMessage({
+                          id: "validation.filter.all",
+                        })}
+                        value=""
+                      />
+                      {analyzers.map((ana) => (
+                        <SelectItem key={ana} text={ana} value={ana} />
+                      ))}
+                    </Select>
+                  </Column>
+
+                  <Column lg={4} md={4} sm={4}>
+                    <Select
+                      id="entered-by"
+                      labelText={intl.formatMessage({
+                        id: "validation.filter.enteredby",
+                      })}
+                      value={enteredBy}
+                      onChange={(e) => setEnteredBy(e.target.value)}
+                    >
+                      <SelectItem
+                        text={intl.formatMessage({
+                          id: "validation.filter.all",
+                        })}
+                        value=""
+                      />
+                      {users.map((user) => (
+                        <SelectItem key={user} text={user} value={user} />
+                      ))}
+                    </Select>
+                  </Column>
+
+                  <Column lg={4} md={4} sm={4}>
+                    <fieldset className="cds--fieldset">
+                      <legend className="cds--label">
+                        <FormattedMessage id="validation.filter.quickfilters" />
+                      </legend>
+                      <Checkbox
+                        id="filter-flagged"
+                        labelText={intl.formatMessage({
+                          id: "validation.filter.flagged",
+                        })}
+                        checked={showFlaggedOnly}
+                        onChange={() => handleQuickFilterChange("flagged")}
+                      />
+                      <Checkbox
+                        id="filter-normal"
+                        labelText={intl.formatMessage({
+                          id: "validation.filter.normal",
+                        })}
+                        checked={showNormalOnly}
+                        onChange={() => handleQuickFilterChange("normal")}
+                      />
+                      <Checkbox
+                        id="filter-abnormal"
+                        labelText={intl.formatMessage({
+                          id: "validation.filter.abnormal",
+                        })}
+                        checked={showAbnormalOnly}
+                        onChange={() => handleQuickFilterChange("abnormal")}
+                      />
+                    </fieldset>
+                  </Column>
+
+                  <Column lg={16}>
+                    <div
+                      style={{
+                        display: "flex",
+                        justifyContent: "flex-end",
+                        gap: "1rem",
+                        marginTop: "1rem",
+                      }}
+                    >
+                      <Button
+                        kind="secondary"
+                        renderIcon={Close}
+                        onClick={handleClear}
+                      >
+                        <FormattedMessage id="label.button.clear" />
+                      </Button>
+                      <Button
+                        type="button"
+                        onClick={handleSearch}
+                        disabled={!selectedLabUnit && !searchQuery}
+                      >
+                        <FormattedMessage id="validation.filter.apply" />
+                      </Button>
+                    </div>
+                  </Column>
+                </Grid>
+              )}
+            </Stack>
+          </form>
+        </Column>
+      </Grid>
+    </Section>
   );
 };
 
diff --git a/frontend/src/components/validation/Validation.js b/frontend/src/components/validation/Validation.js
index 0655e7b5fd..0e96451ef9 100644
--- a/frontend/src/components/validation/Validation.js
+++ b/frontend/src/components/validation/Validation.js
@@ -1,487 +1,498 @@
-import React, { useState, useContext, useEffect, useRef } from "react";
-import { Field, Formik } from "formik";
+import React, { useState, useContext } from "react";
 import {
   Button,
-  Checkbox,
   Column,
-  Form,
   Grid,
+  Section,
+  Checkbox,
+  DataTable,
+  Table,
+  TableHead,
+  TableRow,
+  TableHeader,
+  TableBody,
+  TableCell,
+  TableExpandRow,
+  TableExpandedRow,
+  TableExpandHeader,
+  Tag,
   Pagination,
-  Select,
-  SelectItem,
-  TextArea,
-  TextInput,
+  TableContainer,
 } from "@carbon/react";
-import { Copy } from "@carbon/icons-react";
-import DataTable from "react-data-table-component";
+import { Renew, CheckmarkOutline } from "@carbon/icons-react";
 import { FormattedMessage, useIntl } from "react-intl";
-import ValidationSearchFormValues from "../formModel/innitialValues/ValidationSearchFormValues";
-import { NotificationKinds } from "../common/CustomNotification";
-import { postToOpenElisServer } from "../utils/Utils";
 import { NotificationContext } from "../layout/Layout";
-import { getFromOpenElisServer } from "../utils/Utils";
-import { ConfigurationContext } from "../layout/Layout";
-import { convertAlphaNumLabNumForDisplay } from "../utils/Utils";
-import config from "../../config.json";
-
-const Validation = (props) => {
-  const componentMounted = useRef(false);
-
-  const { setNotificationVisible, addNotification } =
-    useContext(NotificationContext);
-  const { configurationProperties } = useContext(ConfigurationContext);
+import { NotificationKinds } from "../common/CustomNotification";
+import {
+  postToOpenElisServer,
+  postToOpenElisServerJsonResponse,
+} from "../utils/Utils";
+import ResultRow from "./ResultRow";
+import RetestModal from "./RetestModal";
 
+const ValidationPage = ({ results, setResults, onRefresh }) => {
   const intl = useIntl();
+  const { addNotification, setNotificationVisible } =
+    useContext(NotificationContext);
 
-  const [page, setPage] = useState(1);
-  const [pageSize, setPageSize] = useState(100);
-  const [isSubmitting, setIsSubmitting] = useState(false);
+  const [expandedRows, setExpandedRows] = useState({});
+  const [selectedRows, setSelectedRows] = useState([]);
+  const [showRetestModal, setShowRetestModal] = useState(false);
+  const [currentPage, setCurrentPage] = useState(1);
+  const [pageSize, setPageSize] = useState(20);
 
-  useEffect(() => {
-    componentMounted.current = true;
-    return () => {
-      componentMounted.current = false;
-    };
-  }, []);
+  const stats = {
+    normal: results?.resultList?.filter((r) => r.isNormal).length || 0,
+    abnormal:
+      results?.resultList?.filter((r) => !r.isNormal && !r.isCritical).length ||
+      0,
+    flagged:
+      results?.resultList?.filter((r) => r.flags && r.flags.length > 0)
+        .length || 0,
+  };
 
-  const columns = [
+  const headers = [
     {
-      id: "sampleInfo",
-      name: intl.formatMessage({ id: "column.name.sampleInfo" }),
-      cell: (row, index, column, id) => {
-        return renderCell(row, index, column, id);
-      },
-      selector: (row) => row.accessionNumber,
-      sortable: true,
-      width: "16rem",
+      key: "qcIndicator",
+      header: intl.formatMessage({ id: "validation.header.qc" }),
     },
     {
-      id: "testName",
-      name: intl.formatMessage({ id: "column.name.testName" }),
-      selector: (row) => row.testName,
-      cell: (row, index, column, id) => {
-        return renderCell(row, index, column, id);
-      },
-      sortable: true,
-      width: "15rem",
+      key: "labNumber",
+      header: intl.formatMessage({ id: "validation.header.labnumber" }),
     },
     {
-      id: "normalRange",
-      name: intl.formatMessage({ id: "column.name.normalRange" }),
-      selector: (row) => row.normalRange,
-      sortable: true,
-      width: "8rem",
+      key: "patientInfo",
+      header: intl.formatMessage({ id: "validation.header.patient" }),
     },
     {
-      id: "result",
-      name: intl.formatMessage({ id: "column.name.result" }),
-      cell: (row, index, column, id) => {
-        return renderCell(row, index, column, id);
-      },
-      width: "8rem",
+      key: "testName",
+      header: intl.formatMessage({ id: "validation.header.test" }),
     },
     {
-      id: "save",
-      name: intl.formatMessage({ id: "column.name.save" }),
-      cell: (row, index, column, id) => {
-        return renderCell(row, index, column, id);
-      },
-      width: "8rem",
+      key: "method",
+      header: intl.formatMessage({ id: "validation.header.method" }),
     },
     {
-      id: "retest",
-      name: intl.formatMessage({ id: "column.name.retest" }),
-      cell: (row, index, column, id) => {
-        return renderCell(row, index, column, id);
-      },
-      width: "8rem",
+      key: "range",
+      header: intl.formatMessage({ id: "validation.header.range" }),
     },
     {
-      id: "notes",
-      name: intl.formatMessage({ id: "column.name.notes" }),
-      cell: (row, index, column, id) => {
-        return renderCell(row, index, column, id);
-      },
-      width: "15rem",
+      key: "result",
+      header: intl.formatMessage({ id: "validation.header.result" }),
     },
     {
-      id: "pastNotes",
-      name: intl.formatMessage({ id: "column.name.pastNotes" }),
-      cell: (row, index, column, id) => {
-        return renderCell(row, index, column, id);
-      },
-      width: "28rem",
+      key: "flags",
+      header: intl.formatMessage({ id: "validation.header.flags" }),
+    },
+    {
+      key: "enteredBy",
+      header: intl.formatMessage({ id: "validation.header.enteredby" }),
+    },
+    {
+      key: "actions",
+      header: intl.formatMessage({ id: "validation.header.actions" }),
     },
   ];
 
-  const handleSave = (values) => {
-    if (isSubmitting) {
-      return;
-    }
-    setIsSubmitting(true);
-    postToOpenElisServer(
-      "/rest/AccessionValidation",
-      JSON.stringify(props.results),
-      handleResponse,
-    );
-  };
-  const handleResponse = (status) => {
-    let message = intl.formatMessage({ id: "validation.save.error" });
-    let kind = NotificationKinds.error;
-    setIsSubmitting(false);
-    if (status == 200) {
-      message = intl.formatMessage({ id: "validation.save.success" });
-      kind = NotificationKinds.success;
-      window.location.href = "/validation" + props.params;
-    }
-    addNotification({
-      kind: kind,
-      title: intl.formatMessage({ id: "notification.title" }),
-      message: message,
+  const rowsData =
+    results?.resultList?.map((item) => ({
+      id: item.analysisId,
+      qcIndicator:
+        item.qcStatus === "pass" ? "✓" : item.qcStatus === "fail" ? "✗" : "",
+      labNumber: item.accessionNumber,
+      patientInfo: item.patientInfoObject
+        ? `${item.patientInfoObject.name} (${item.patientInfoObject.age}, ${item.patientInfoObject.sex})`
+        : item.patientName || "",
+      testName: item.testName,
+      method: item.method || (item.isManual ? "Manual" : "Analyzer"),
+      range: item.normalRange,
+      result: item.result,
+      flags: item.flags || [],
+      enteredBy: item.enteredByObject
+        ? `${item.enteredByObject.name} (${item.enteredByObject.date})`
+        : "",
+      rawData: item,
+    })) || [];
+
+  const handleSelectRow = (rowId) => {
+    setSelectedRows((prev) => {
+      if (prev.includes(rowId)) {
+        return prev.filter((id) => id !== rowId);
+      }
+      return [...prev, rowId];
     });
-    setNotificationVisible(true);
   };
 
-  const handlePageChange = (pageInfo) => {
-    if (page != pageInfo.page) {
-      setPage(pageInfo.page);
-    }
-    if (pageSize != pageInfo.pageSize) {
-      setPageSize(pageInfo.pageSize);
+  const handleSelectAll = () => {
+    if (selectedRows.length === rowsData.length) {
+      setSelectedRows([]);
+    } else {
+      setSelectedRows(rowsData.map((row) => row.id));
     }
   };
 
-  const handleChange = (e, rowId) => {
-    const { name, id, value } = e.target;
-    let form = props.results;
-    var jp = require("jsonpath");
-    jp.value(form, name, value);
+  const handleSelectNormal = () => {
+    const normalRows = rowsData
+      .filter((row) => row.rawData.isNormal)
+      .map((row) => row.id);
+    setSelectedRows(normalRows);
   };
 
-  const handleDatePickerChange = (date, rowId) => {
-    console.debug("handleDatePickerChange:" + date);
-    const d = new Date(date).toLocaleDateString("fr-FR");
-    var form = props.results;
-    var jp = require("jsonpath");
-    jp.value(form, "resultList[" + rowId + "].sentDate_", d);
-  };
-  const handleCheckBox = (e, rowId) => {
-    const { name, id, checked } = e.target;
-    let form = props.results;
-    var jp = require("jsonpath");
-    jp.value(form, name, checked);
+  const handleRetest = () => {
+    if (selectedRows.length === 0) {
+      addNotification({
+        kind: NotificationKinds.warning,
+        title: intl.formatMessage({ id: "notification.title" }),
+        message: intl.formatMessage({
+          id: "validation.retest.noselection",
+        }),
+      });
+      setNotificationVisible(true);
+      return;
+    }
+    setShowRetestModal(true);
   };
 
-  const handleAutomatedCheck = (checked, name) => {
-    let form = props.results;
-    var jp = require("jsonpath");
-    jp.value(form, name, checked);
-  };
-  const validateResults = (e, rowId) => {
-    handleChange(e, rowId);
-  };
+  const handleConfirmRetest = (reason) => {
+    const retestRequest = {
+      resultIds: selectedRows,
+      reason: reason,
+    };
 
-  const renderCell = (row, index, column, id) => {
-    let formatLabNum = configurationProperties.AccessionFormat === "ALPHANUM";
-    const fullTestName = row.testName;
-    const splitIndex = fullTestName.lastIndexOf("(");
-    const testName = fullTestName.substring(0, splitIndex);
-    const sampleType = fullTestName.substring(splitIndex);
-    switch (column.id) {
-      case "sampleInfo":
-        return (
-          <>
-            <Button
-              onClick={async () => {
-                if ("clipboard" in navigator) {
-                  return await navigator.clipboard.writeText(
-                    row.accessionNumber,
-                  );
-                } else {
-                  return document.execCommand(
-                    "copy",
-                    true,
-                    row.accessionNumber,
-                  );
-                }
-              }}
-              kind="ghost"
-              iconDescription={intl.formatMessage({
-                id: "instructions.copy.labnum",
-              })}
-              hasIconOnly
-              renderIcon={Copy}
-            />
-            <div className="sampleInfo" data-testid="LabNo">
-              <br></br>
-              {formatLabNum
-                ? convertAlphaNumLabNumForDisplay(row.accessionNumber)
-                : row.accessionNumber}
-              <br></br>
-              <br></br>
-            </div>
-            {row.nonconforming && (
-              <picture>
-                <img
-                  src={config.serverBaseUrl + "/images/nonconforming.gif"}
-                  alt="nonconforming"
-                  width="20"
-                  height="15"
-                />
-              </picture>
-            )}
-          </>
-        );
-      case "testName":
-        return (
-          <div className="sampleInfo" data-testid="sampleInfo">
-            <br></br>
-            {testName}
-            <br></br>
-            {sampleType}
-          </div>
-        );
+    postToOpenElisServerJsonResponse(
+      "/rest/AccessionValidation/retest",
+      JSON.stringify(retestRequest),
+      (data) => {
+        if (data && (data.success || data.status === 200)) {
+          addNotification({
+            kind: NotificationKinds.success,
+            title: intl.formatMessage({ id: "notification.title" }),
+            message: intl.formatMessage(
+              { id: "validation.retest.success" },
+              { count: data.count || selectedRows.length },
+            ),
+          });
+          setNotificationVisible(true);
+          setSelectedRows([]);
+          setShowRetestModal(false);
 
-      case "save":
-        return (
-          <>
-            <div data-testid="Checkbox">
-              <Field name="isAccepted">
-                {({ field }) => (
-                  <Checkbox
-                    id={"resultList" + row.id + ".isAccepted"}
-                    name={"resultList[" + row.id + "].isAccepted"}
-                    labelText=""
-                    value={true}
-                    onChange={(e) => handleCheckBox(e, row.id)}
-                  />
-                )}
-              </Field>
-            </div>
-          </>
-        );
+          if (onRefresh) {
+            onRefresh();
+          }
+        } else if (data && data.error) {
+          addNotification({
+            kind: NotificationKinds.error,
+            title: intl.formatMessage({ id: "notification.title" }),
+            message:
+              data.message ||
+              intl.formatMessage({
+                id: "validation.retest.error",
+              }),
+          });
+          setNotificationVisible(true);
+          setShowRetestModal(false);
+        } else {
+          addNotification({
+            kind: NotificationKinds.success,
+            title: intl.formatMessage({ id: "notification.title" }),
+            message: intl.formatMessage(
+              { id: "validation.retest.success" },
+              { count: selectedRows.length },
+            ),
+          });
+          setNotificationVisible(true);
+          setSelectedRows([]);
+          setShowRetestModal(false);
+
+          if (onRefresh) {
+            onRefresh();
+          }
+        }
+      },
+    );
+  };
 
-      case "retest":
-        return (
-          <>
-            <Field name="isRejected">
-              {({ field }) => (
-                <Checkbox
-                  id={"resultList" + row.id + ".isRejected"}
-                  name={"resultList[" + row.id + "].isRejected"}
-                  labelText=""
-                  value={true}
-                  onChange={(e) => handleCheckBox(e, row.id)}
-                />
-              )}
-            </Field>
-          </>
-        );
+  const handleAcceptRelease = () => {
+    if (selectedRows.length === 0) {
+      addNotification({
+        kind: NotificationKinds.warning,
+        title: intl.formatMessage({ id: "notification.title" }),
+        message: intl.formatMessage({
+          id: "validation.accept.noselection",
+        }),
+      });
+      setNotificationVisible(true);
+      return;
+    }
 
-      case "notes":
-        return (
-          <>
-            <div className="note">
-              <TextArea
-                id={"resultList" + row.id + ".note"}
-                name={"resultList[" + row.id + "].note"}
-                disabled={false}
-                type="text"
-                labelText=""
-                rows={2}
-                onChange={(e) => handleChange(e, row.id)}
-              ></TextArea>
-            </div>
-          </>
-        );
+    const updatedResultList = results.resultList.map((item) => ({
+      ...item,
+      isAccepted: selectedRows.includes(item.analysisId),
+    }));
 
-      case "pastNotes":
-        return (
-          <>
-            <div
-              className="note"
-              dangerouslySetInnerHTML={{ __html: row.pastNotes }}
-            />
-          </>
-        );
+    const validationForm = {
+      ...results,
+      resultList: updatedResultList,
+    };
 
-      case "result":
-        switch (row.resultType) {
-          case "M":
-          case "C":
-          case "D":
-            return (
-              <>
-                {
-                  row.dictionaryResults.find(
-                    (result) => result.id == row.result,
-                  )?.value
-                }
-              </>
-            );
-          default:
-            return row.result;
+    postToOpenElisServer(
+      "/rest/AccessionValidation",
+      JSON.stringify(validationForm),
+      (status) => {
+        if (status === 200) {
+          addNotification({
+            kind: NotificationKinds.success,
+            title: intl.formatMessage({ id: "notification.title" }),
+            message: intl.formatMessage(
+              { id: "validation.accept.success" },
+              { count: selectedRows.length },
+            ),
+          });
+          setNotificationVisible(true);
+          setSelectedRows([]);
+
+          if (onRefresh) {
+            onRefresh();
+          } else {
+            window.location.reload();
+          }
+        } else {
+          addNotification({
+            kind: NotificationKinds.error,
+            title: intl.formatMessage({ id: "notification.title" }),
+            message: intl.formatMessage({ id: "validation.accept.error" }),
+          });
+          setNotificationVisible(true);
         }
+      },
+    );
+  };
 
-      default:
-    }
-    return row.result;
+  const toggleRowExpansion = (rowId) => {
+    setExpandedRows((prev) => ({
+      ...prev,
+      [rowId]: !prev[rowId],
+    }));
   };
 
-  return (
-    <>
-      {props.results?.resultList?.length > 0 && (
-        <Grid style={{ marginTop: "20px" }} className="gridBoundary">
-          <Column lg={7} md={8} sm={2}>
-            <picture>
-              <img
-                src={config.serverBaseUrl + "/images/nonconforming.gif"}
-                alt="nonconforming"
-                width="25" // Set your desired width
-                height="20" // Set your desired height
-              />
-            </picture>
-            <b>
-              {" "}
-              <FormattedMessage id="validation.label.nonconform" />
-            </b>
-          </Column>
-          <Column lg={3} md={2} sm={4}>
-            <Checkbox
-              id={"saveallnormal"}
-              name={"autochecks"}
-              labelText={intl.formatMessage({ id: "validation.accept.normal" })}
-              onChange={(e) => {
-                const nomalResults = props.results.resultList?.filter(
-                  (result) => result.normal == true,
-                );
-                nomalResults.forEach((result) => {
-                  const checkbox = document.getElementById(
-                    "resultList" + result.id + ".isAccepted",
-                  );
-                  checkbox.checked = e.target.checked;
-                  handleAutomatedCheck(e.target.checked, checkbox.name);
-                });
-              }}
-            />
-          </Column>
-          <Column lg={3} md={2} sm={4}>
-            <Checkbox
-              id={"saveallresults"}
-              name={"autochecks"}
-              labelText={intl.formatMessage({ id: "validation.accept.all" })}
-              onChange={(e) => {
-                const nomalResults = props.results.resultList;
-                nomalResults.forEach((result) => {
-                  const checkbox = document.getElementById(
-                    "resultList" + result.id + ".isAccepted",
-                  );
-                  checkbox.checked = e.target.checked;
-                  handleAutomatedCheck(e.target.checked, checkbox.name);
-                });
-              }}
-            />
-          </Column>
-          <Column lg={3} md={2} sm={4}>
-            <Checkbox
-              id={"retestalltests"}
-              name={"autochecks"}
-              labelText={intl.formatMessage({ id: "validation.reject.all" })}
-              onChange={(e) => {
-                const nomalResults = props.results.resultList;
-                nomalResults.forEach((result) => {
-                  const checkbox = document.getElementById(
-                    "resultList" + result.id + ".isRejected",
-                  );
-                  checkbox.checked = e.target.checked;
-                  handleAutomatedCheck(e.target.checked, checkbox.name);
-                });
-              }}
-            />
+  if (!results || !results.resultList || results.resultList.length === 0) {
+    return (
+      <Section className="validation-empty-state">
+        <Grid>
+          <Column lg={16}>
+            <div style={{ textAlign: "center", padding: "4rem 0" }}>
+              <h3>
+                <FormattedMessage id="validation.search.empty.title" />
+              </h3>
+              <p>
+                <FormattedMessage id="validation.search.empty.message" />
+              </p>
+            </div>
           </Column>
         </Grid>
-      )}
-      <Formik
-        initialValues={ValidationSearchFormValues}
-        //validationSchema={}
-        onSubmit
-        onChange
-      >
-        {({ values, errors, touched, handleChange }) => (
-          <Form onChange={handleChange}>
-            <DataTable
-              data={
-                props.results
-                  ? props?.results?.resultList?.slice(
-                      (page - 1) * pageSize,
-                      page * pageSize,
-                    )
-                  : []
-              }
-              columns={columns}
-              isSortable
-            ></DataTable>
-            <Pagination
-              onChange={handlePageChange}
-              page={page}
-              pageSize={pageSize}
-              pageSizes={[10, 20, 30, 50, 100]}
-              totalItems={
-                props.results
-                  ? props.results.resultList
-                    ? props.results.resultList.length
-                    : 0
-                  : 0
-              }
-              forwardText={intl.formatMessage({ id: "pagination.forward" })}
-              backwardText={intl.formatMessage({ id: "pagination.backward" })}
-              itemRangeText={(min, max, total) =>
-                intl.formatMessage(
-                  { id: "pagination.item-range" },
-                  { min: min, max: max, total: total },
-                )
-              }
-              itemsPerPageText={intl.formatMessage({
-                id: "pagination.items-per-page",
-              })}
-              itemText={(min, max) =>
-                intl.formatMessage(
-                  { id: "pagination.item" },
-                  { min: min, max: max },
-                )
-              }
-              pageNumberText={intl.formatMessage({
-                id: "pagination.page-number",
+      </Section>
+    );
+  }
+
+  return (
+    <Section className="validation-results-section">
+      <Grid>
+        <Column lg={16}>
+          <div
+            className="validation-stats"
+            style={{ display: "flex", gap: "1rem", marginBottom: "1rem" }}
+          >
+            <Tag type="green">
+              <FormattedMessage id="validation.stats.normal" />: {stats.normal}
+            </Tag>
+            <Tag type="red">
+              <FormattedMessage id="validation.stats.abnormal" />:{" "}
+              {stats.abnormal}
+            </Tag>
+            <Tag type="purple">
+              <FormattedMessage id="validation.stats.flagged" />:{" "}
+              {stats.flagged}
+            </Tag>
+          </div>
+
+          <div
+            className="validation-batch-actions"
+            style={{
+              display: "flex",
+              gap: "1rem",
+              marginBottom: "1rem",
+              alignItems: "center",
+            }}
+          >
+            <Checkbox
+              id="select-all"
+              labelText={intl.formatMessage({
+                id: "validation.batch.selectall",
               })}
-              pageRangeText={(_current, total) =>
-                intl.formatMessage(
-                  { id: "pagination.page-range" },
-                  { total: total },
-                )
+              checked={
+                selectedRows.length === rowsData.length && rowsData.length > 0
               }
-              pageText={(page, pagesUnknown) =>
-                intl.formatMessage(
-                  { id: "pagination.page" },
-                  { page: pagesUnknown ? "" : page },
-                )
+              indeterminate={
+                selectedRows.length > 0 && selectedRows.length < rowsData.length
               }
+              onChange={handleSelectAll}
             />
-
-            <Button
-              type="button"
-              onClick={() => handleSave(values)}
-              id="submit"
-              style={{ marginTop: "16px" }}
-              data-testid="Save-btn"
-              disabled={isSubmitting}
-            >
-              <FormattedMessage id="label.button.save" />
+            <Button kind="tertiary" size="sm" onClick={handleSelectNormal}>
+              <FormattedMessage id="validation.batch.selectnormal" />
             </Button>
-          </Form>
-        )}
-      </Formik>
-    </>
+            <span>
+              <FormattedMessage
+                id="validation.batch.selected"
+                values={{ count: selectedRows.length }}
+              />
+            </span>
+            <div style={{ marginLeft: "auto", display: "flex", gap: "0.5rem" }}>
+              <Button
+                kind="secondary"
+                renderIcon={Renew}
+                onClick={handleRetest}
+                disabled={selectedRows.length === 0}
+              >
+                <FormattedMessage id="validation.batch.retest" />
+              </Button>
+              <Button
+                kind="primary"
+                renderIcon={CheckmarkOutline}
+                onClick={handleAcceptRelease}
+                disabled={selectedRows.length === 0}
+              >
+                <FormattedMessage id="validation.batch.accept" />
+              </Button>
+            </div>
+          </div>
+
+          <DataTable rows={rowsData} headers={headers} isSortable>
+            {({
+              rows,
+              headers,
+              getHeaderProps,
+              getRowProps,
+              getTableProps,
+              getTableContainerProps,
+            }) => (
+              <TableContainer
+                {...getTableContainerProps()}
+                title={intl.formatMessage({ id: "validation.results.title" })}
+                description={intl.formatMessage(
+                  { id: "validation.results.count" },
+                  { count: rowsData.length },
+                )}
+              >
+                <Table {...getTableProps()} aria-label="validation results">
+                  <TableHead>
+                    <TableRow>
+                      <TableExpandHeader />
+                      <TableHeader>
+                        {intl.formatMessage({ id: "validation.header.select" })}
+                      </TableHeader>
+                      {headers.map((header) => (
+                        <TableHeader
+                          key={header.key}
+                          {...getHeaderProps({ header })}
+                        >
+                          {header.header}
+                        </TableHeader>
+                      ))}
+                    </TableRow>
+                  </TableHead>
+                  <TableBody>
+                    {rows.map((row) => {
+                      const isExpanded = expandedRows[row.id];
+                      const isSelected = selectedRows.includes(row.id);
+
+                      return (
+                        <React.Fragment key={row.id}>
+                          <TableExpandRow
+                            {...getRowProps({ row })}
+                            isExpanded={isExpanded}
+                            onExpand={() => toggleRowExpansion(row.id)}
+                          >
+                            <TableCell>
+                              <Checkbox
+                                id={`checkbox-${row.id}`}
+                                labelText=""
+                                hideLabel
+                                checked={isSelected}
+                                onChange={() => handleSelectRow(row.id)}
+                              />
+                            </TableCell>
+                            {row.cells.map((cell) => {
+                              if (cell.info.header === "flags") {
+                                return (
+                                  <TableCell key={cell.id}>
+                                    {cell.value.map((flag, idx) => (
+                                      <Tag
+                                        key={idx}
+                                        type={
+                                          flag === "critical"
+                                            ? "red"
+                                            : flag === "above-normal"
+                                              ? "red"
+                                              : flag === "below-normal"
+                                                ? "blue"
+                                                : "purple"
+                                        }
+                                        size="sm"
+                                      >
+                                        {flag}
+                                      </Tag>
+                                    ))}
+                                  </TableCell>
+                                );
+                              }
+                              return (
+                                <TableCell key={cell.id}>
+                                  {cell.value}
+                                </TableCell>
+                              );
+                            })}
+                          </TableExpandRow>
+                          {isExpanded && (
+                            <TableExpandedRow colSpan={headers.length + 3}>
+                              <ResultRow
+                                result={
+                                  rowsData.find((r) => r.id === row.id)?.rawData
+                                }
+                              />
+                            </TableExpandedRow>
+                          )}
+                        </React.Fragment>
+                      );
+                    })}
+                  </TableBody>
+                </Table>
+              </TableContainer>
+            )}
+          </DataTable>
+
+          <Pagination
+            page={currentPage}
+            pageSize={pageSize}
+            pageSizes={[10, 20, 50, 100]}
+            totalItems={rowsData.length}
+            onChange={({ page, pageSize }) => {
+              setCurrentPage(page);
+              setPageSize(pageSize);
+            }}
+          />
+        </Column>
+      </Grid>
+
+      {showRetestModal && (
+        <RetestModal
+          isOpen={showRetestModal}
+          onClose={() => setShowRetestModal(false)}
+          onConfirm={handleConfirmRetest}
+          selectedCount={selectedRows.length}
+        />
+      )}
+    </Section>
   );
 };
 
-export default Validation;
+export default ValidationPage;
diff --git a/frontend/src/languages/en.json b/frontend/src/languages/en.json
index 4b37120bb9..6e5b9bac0f 100644
--- a/frontend/src/languages/en.json
+++ b/frontend/src/languages/en.json
@@ -568,6 +568,7 @@
   "label.button.select.test": "Select Test",
   "label.button.start": "Start",
   "label.button.submit": "Submit",
+  "label.loading": "Loading...",
   "label.button.uploadfile": "Upload file",
   "label.button.viewReport": "View Report",
   "label.cancel.test.no.replace": "Cancel test for lab numbers selected, do not assign a new test",
@@ -588,6 +589,7 @@
   "label.loinc": "LOINC",
   "label.notValidated": "Not validated",
   "label.notequals": "does not equal",
+  "label.or": "OR",
   "label.order.scan.text": "Scan OR Enter Manually OR",
   "label.orderable": "Orderable",
   "label.outside.normalrange": "is outside the normal range",
@@ -2384,5 +2386,131 @@
   "reports.help.description": "Use these reports to analyze your inventory data:",
   "reports.help.tip1": "Use date ranges for trend analysis",
   "reports.help.tip2": "Group by type or location for better insights",
-  "reports.help.tip3": "Export to Excel for further analysis"
+  "reports.help.tip3": "Export to Excel for further analysis",
+  "validation.page.title": "Result Validation",
+  "validation.search.labunit": "Lab Unit",
+  "validation.search.labunit.placeholder": "Select Lab Unit",
+  "validation.search.query": "Search",
+  "validation.search.query.placeholder": "Accession Number, Patient Name, or Sample ID",
+  "validation.search.required": "Please select a Lab Unit or enter a search query",
+  "validation.search.noresults": "No results found",
+  "validation.search.empty.title": "Search to View Results",
+  "validation.search.empty.message": "Select a Lab Unit or enter a search query to begin validating results",
+  "validation.filters.toggle": "Filters",
+  "validation.filters.advanced": "Advanced Filters",
+  "validation.filter.labnumber.from": "Lab Number From",
+  "validation.filter.labnumber.to": "Lab Number To",
+  "validation.filter.date.from": "Date From",
+  "validation.filter.date.to": "Date To",
+  "validation.filter.testsection": "Test Section",
+  "validation.filter.analyzer": "Analyzer",
+  "validation.filter.enteredby": "Entered By",
+  "validation.filter.quickfilters": "Quick Filters",
+  "validation.filter.flagged": "Flagged Only",
+  "validation.filter.normal": "Normal Only",
+  "validation.filter.abnormal": "Abnormal Only",
+  "validation.filter.all": "All",
+  "validation.filter.apply": "Apply Filters",
+  "validation.stats.normal": "Normal",
+  "validation.stats.abnormal": "Abnormal",
+  "validation.stats.flagged": "Flagged",
+  "validation.batch.selectall": "Select All",
+  "validation.batch.selectnormal": "Select Normal",
+  "validation.batch.selected": "{count} Selected",
+  "validation.batch.retest": "Retest Selected",
+  "validation.batch.accept": "Accept & Release Selected",
+  "validation.header.select": "Select",
+  "validation.header.qc": "QC",
+  "validation.header.labnumber": "Lab Number",
+  "validation.header.patient": "Patient",
+  "validation.header.test": "Test",
+  "validation.header.method": "Method",
+  "validation.header.range": "Normal Range",
+  "validation.header.result": "Result",
+  "validation.header.flags": "Flags",
+  "validation.header.enteredby": "Entered By",
+  "validation.header.actions": "Actions",
+  "validation.results.title": "Validation Results",
+  "validation.results.count": "Showing {count} results",
+  "validation.row.patient.name": "Name",
+  "validation.row.patient.id": "Patient ID",
+  "validation.row.patient.dob": "DOB",
+  "validation.row.patient.sex": "Sex",
+  "validation.row.patient.age": "Age",
+  "validation.row.entry.by": "Entered By",
+  "validation.row.entry.date": "Entry Date",
+  "validation.row.method": "Method",
+  "validation.row.unlock": "Unlock to Edit",
+  "validation.row.locked": "Lock",
+  "validation.row.result.value": "Result Value",
+  "validation.row.normalrange": "Normal Range",
+  "validation.row.interpretation": "Interpretation",
+  "validation.row.interpretation.none": "None",
+  "validation.row.interpretation.normal": "Normal",
+  "validation.row.interpretation.abnormal": "Abnormal",
+  "validation.row.interpretation.critical": "Critical",
+  "validation.row.flags": "Flags",
+  "validation.row.notes": "Notes",
+  "validation.row.notes.placeholder": "Add validation note...",
+  "validation.row.notes.previous": "Previous Notes",
+  "validation.row.action.retest": "Retest",
+  "validation.row.action.accept": "Accept & Release",
+  "validation.retest.noselection": "Please select at least one result to retest",
+  "validation.retest.success": "{count} result(s) sent for retest successfully",
+  "validation.retest.error": "Error sending results for retest",
+  "validation.retest.modal.title": "Send for Retest",
+  "validation.retest.modal.description": "You are about to send {count} result(s) back for retesting. This will reset their status to Pending.",
+  "validation.retest.modal.reason.label": "Reason for Retest (Required)",
+  "validation.retest.modal.reason.placeholder": "e.g., QC failed, Critical delta check, Suspected instrument error, etc.",
+  "validation.retest.modal.reason.required": "Retest reason is required",
+  "validation.retest.modal.confirm": "Confirm Retest",
+  "validation.retest.modal.note": "Note: A retest request will be logged in the audit trail and added as an internal note to the result.",
+  "validation.accept.noselection": "Please select at least one result to accept",
+  "validation.accept.success": "{count} result(s) validated and released successfully",
+  "validation.accept.error": "Error validating results. Please try again.",
+  "validation.details.nodata": "No detailed data available",
+  "validation.details.error": "Error loading validation details",
+  "validation.details.method.tab": "Method & Reagents",
+  "validation.details.method.title": "Reagent Lots",
+  "validation.details.method.noreagents": "No reagent information available",
+  "validation.details.method.reagent": "Reagent",
+  "validation.details.method.lot": "Lot Number",
+  "validation.details.method.expires": "Expires",
+  "validation.details.method.status": "Status",
+  "validation.details.orderinfo.tab": "Order Info",
+  "validation.details.orderinfo.title": "Order Information",
+  "validation.details.orderinfo.field": "Field",
+  "validation.details.orderinfo.value": "Value",
+  "validation.details.orderinfo.clinician": "Requesting Clinician",
+  "validation.details.orderinfo.phone": "Clinician Phone",
+  "validation.details.orderinfo.department": "Department",
+  "validation.details.orderinfo.priority": "Priority",
+  "validation.details.orderinfo.collection": "Collection Date/Time",
+  "validation.details.orderinfo.received": "Received Date/Time",
+  "validation.details.orderinfo.history": "Clinical History",
+  "validation.details.orderinfo.diagnosis": "Diagnosis",
+  "validation.details.attachments.tab": "Attachments",
+  "validation.details.attachments.title": "Attachments",
+  "validation.details.attachments.none": "No attachments available",
+  "validation.details.attachments.name": "File Name",
+  "validation.details.attachments.type": "Type",
+  "validation.details.attachments.size": "Size",
+  "validation.details.attachments.uploadedby": "Uploaded By",
+  "validation.details.attachments.uploadedat": "Uploaded At",
+  "validation.details.history.tab": "History",
+  "validation.details.history.title": "Previous Results",
+  "validation.details.history.none": "No previous results available",
+  "validation.details.history.date": "Date",
+  "validation.details.history.value": "Value",
+  "validation.details.history.status": "Status",
+  "validation.details.history.deltacheck": "Delta Check Warning",
+  "validation.details.history.deltacheck.message": "Previous result: {previous} | Change: {change} | Threshold: {threshold}",
+  "validation.details.qc.tab": "QA/QC",
+  "validation.details.qc.title": "Quality Control Results",
+  "validation.details.qc.none": "No QC data available",
+  "validation.details.qc.level": "Level",
+  "validation.details.qc.expected": "Expected",
+  "validation.details.qc.actual": "Actual",
+  "validation.details.qc.cv": "CV%",
+  "validation.details.qc.status": "Status"
 }
diff --git a/frontend/src/languages/fr.json b/frontend/src/languages/fr.json
index 0c05057e0b..5ef79864ce 100644
--- a/frontend/src/languages/fr.json
+++ b/frontend/src/languages/fr.json
@@ -568,6 +568,7 @@
   "label.button.select.test": "Sélectionner le test",
   "label.button.start": "Commencer",
   "label.button.submit": "Terminer",
+  "label.loading": "Chargement...",
   "label.button.uploadfile": "Télécharger le Fichier",
   "label.button.viewReport": "Voir le rapport",
   "label.cancel.test.no.replace": "Annuler le test pour les numéros de laboratoire sélectionnés, ne pas assigner un nouveau test",
@@ -588,6 +589,7 @@
   "label.loinc": "LOINC",
   "label.notValidated": "Non validé",
   "label.notequals": "n'est pas égal",
+  "label.or": "OU",
   "label.order.scan.text": "Scanner OU Saisir manuellement OU",
   "label.orderable": "Commandable",
   "label.outside.normalrange": "est en dehors de la plage normale",
@@ -2381,8 +2383,134 @@
   "reports.generating": "Génération en cours...",
   "reports.generation.success": "Rapport généré et téléchargé avec succès",
   "reports.help.title": "Conseils pour signaler",
-  "reports.help.description": "Utilisez ces rapports pour analyser vos données d'inventaire :",
+  "reports.help.description": "Utilisez ces rapports pour analyser vos données d'inventaire :",
   "reports.help.tip1": "Utilisez des plages de dates pour l'analyse des tendances",
   "reports.help.tip2": "Regroupez par type ou par emplacement pour une meilleure analyse.",
-  "reports.help.tip3": "Exporter vers Excel pour une analyse plus approfondie"
+  "reports.help.tip3": "Exporter vers Excel pour une analyse plus approfondie",
+  "validation.page.title": "Validation des Résultats",
+  "validation.search.labunit": "Unité de Laboratoire",
+  "validation.search.labunit.placeholder": "Sélectionner l'Unité de Laboratoire",
+  "validation.search.query": "Rechercher",
+  "validation.search.query.placeholder": "Numéro d'Accession, Nom du Patient ou ID d'Échantillon",
+  "validation.search.required": "Veuillez sélectionner une Unité de Laboratoire ou saisir une requête de recherche",
+  "validation.search.noresults": "Aucun résultat trouvé",
+  "validation.search.empty.title": "Rechercher pour Voir les Résultats",
+  "validation.search.empty.message": "Sélectionnez une Unité de Laboratoire ou saisissez une requête de recherche pour commencer à valider les résultats",
+  "validation.filters.toggle": "Filtres",
+  "validation.filters.advanced": "Filtres Avancés",
+  "validation.filter.labnumber.from": "Numéro de Laboratoire De",
+  "validation.filter.labnumber.to": "Numéro de Laboratoire À",
+  "validation.filter.date.from": "Date De",
+  "validation.filter.date.to": "Date À",
+  "validation.filter.testsection": "Section de Test",
+  "validation.filter.analyzer": "Analyseur",
+  "validation.filter.enteredby": "Saisi Par",
+  "validation.filter.quickfilters": "Filtres Rapides",
+  "validation.filter.flagged": "Signalé Seulement",
+  "validation.filter.normal": "Normal Seulement",
+  "validation.filter.abnormal": "Anormal Seulement",
+  "validation.filter.all": "Tous",
+  "validation.filter.apply": "Appliquer les Filtres",
+  "validation.stats.normal": "Normal",
+  "validation.stats.abnormal": "Anormal",
+  "validation.stats.flagged": "Signalé",
+  "validation.batch.selectall": "Tout Sélectionner",
+  "validation.batch.selectnormal": "Sélectionner Normal",
+  "validation.batch.selected": "{count} Sélectionné(s)",
+  "validation.batch.retest": "Retester Sélectionné",
+  "validation.batch.accept": "Accepter et Libérer Sélectionné",
+  "validation.header.select": "Sélectionner",
+  "validation.header.qc": "QC",
+  "validation.header.labnumber": "Numéro de Laboratoire",
+  "validation.header.patient": "Patient",
+  "validation.header.test": "Test",
+  "validation.header.method": "Méthode",
+  "validation.header.range": "Plage Normale",
+  "validation.header.result": "Résultat",
+  "validation.header.flags": "Indicateurs",
+  "validation.header.enteredby": "Saisi Par",
+  "validation.header.actions": "Actions",
+  "validation.results.title": "Résultats de Validation",
+  "validation.results.count": "Affichage de {count} résultats",
+  "validation.row.patient.name": "Nom",
+  "validation.row.patient.id": "ID du Patient",
+  "validation.row.patient.dob": "Date de Naissance",
+  "validation.row.patient.sex": "Sexe",
+  "validation.row.patient.age": "Âge",
+  "validation.row.entry.by": "Saisi Par",
+  "validation.row.entry.date": "Date d'Entrée",
+  "validation.row.method": "Méthode",
+  "validation.row.unlock": "Déverrouiller pour Modifier",
+  "validation.row.locked": "Verrouiller",
+  "validation.row.result.value": "Valeur du Résultat",
+  "validation.row.normalrange": "Plage Normale",
+  "validation.row.interpretation": "Interprétation",
+  "validation.row.interpretation.none": "Aucune",
+  "validation.row.interpretation.normal": "Normal",
+  "validation.row.interpretation.abnormal": "Anormal",
+  "validation.row.interpretation.critical": "Critique",
+  "validation.row.flags": "Indicateurs",
+  "validation.row.notes": "Notes",
+  "validation.row.notes.placeholder": "Ajouter une note de validation...",
+  "validation.row.notes.previous": "Notes Précédentes",
+  "validation.row.action.retest": "Retester",
+  "validation.row.action.accept": "Accepter et Libérer",
+  "validation.retest.noselection": "Veuillez sélectionner au moins un résultat à retester",
+  "validation.retest.success": "{count} résultat(s) envoyé(s) pour retest avec succès",
+  "validation.retest.error": "Erreur lors de l'envoi des résultats pour retest",
+  "validation.retest.modal.title": "Envoyer pour Retest",
+  "validation.retest.modal.description": "Vous êtes sur le point d'envoyer {count} résultat(s) pour un nouveau test. Cela réinitialisera leur statut à En Attente.",
+  "validation.retest.modal.reason.label": "Raison du Retest (Requis)",
+  "validation.retest.modal.reason.placeholder": "ex: CQ échoué, Vérification delta critique, Erreur d'instrument suspectée, etc.",
+  "validation.retest.modal.reason.required": "La raison du retest est requise",
+  "validation.retest.modal.confirm": "Confirmer le Retest",
+  "validation.retest.modal.note": "Note: Une demande de retest sera enregistrée dans la piste d'audit et ajoutée comme note interne au résultat.",
+  "validation.accept.noselection": "Veuillez sélectionner au moins un résultat à accepter",
+  "validation.accept.success": "{count} résultat(s) validé(s) et libéré(s) avec succès",
+  "validation.accept.error": "Erreur lors de la validation des résultats. Veuillez réessayer.",
+  "validation.details.nodata": "Aucune donnée détaillée disponible",
+  "validation.details.error": "Erreur lors du chargement des détails de validation",
+  "validation.details.method.tab": "Méthode et Réactifs",
+  "validation.details.method.title": "Lots de Réactifs",
+  "validation.details.method.noreagents": "Aucune information sur les réactifs disponible",
+  "validation.details.method.reagent": "Réactif",
+  "validation.details.method.lot": "Numéro de Lot",
+  "validation.details.method.expires": "Expire",
+  "validation.details.method.status": "Statut",
+  "validation.details.orderinfo.tab": "Informations de Commande",
+  "validation.details.orderinfo.title": "Informations de Commande",
+  "validation.details.orderinfo.field": "Champ",
+  "validation.details.orderinfo.value": "Valeur",
+  "validation.details.orderinfo.clinician": "Clinicien Demandeur",
+  "validation.details.orderinfo.phone": "Téléphone du Clinicien",
+  "validation.details.orderinfo.department": "Département",
+  "validation.details.orderinfo.priority": "Priorité",
+  "validation.details.orderinfo.collection": "Date/Heure de Prélèvement",
+  "validation.details.orderinfo.received": "Date/Heure de Réception",
+  "validation.details.orderinfo.history": "Antécédents Cliniques",
+  "validation.details.orderinfo.diagnosis": "Diagnostic",
+  "validation.details.attachments.tab": "Pièces Jointes",
+  "validation.details.attachments.title": "Pièces Jointes",
+  "validation.details.attachments.none": "Aucune pièce jointe disponible",
+  "validation.details.attachments.name": "Nom du Fichier",
+  "validation.details.attachments.type": "Type",
+  "validation.details.attachments.size": "Taille",
+  "validation.details.attachments.uploadedby": "Téléchargé Par",
+  "validation.details.attachments.uploadedat": "Téléchargé Le",
+  "validation.details.history.tab": "Historique",
+  "validation.details.history.title": "Résultats Précédents",
+  "validation.details.history.none": "Aucun résultat précédent disponible",
+  "validation.details.history.date": "Date",
+  "validation.details.history.value": "Valeur",
+  "validation.details.history.status": "Statut",
+  "validation.details.history.deltacheck": "Avertissement de Vérification Delta",
+  "validation.details.history.deltacheck.message": "Résultat précédent: {previous} | Changement: {change} | Seuil: {threshold}",
+  "validation.details.qc.tab": "AQ/CQ",
+  "validation.details.qc.title": "Résultats de Contrôle de Qualité",
+  "validation.details.qc.none": "Aucune donnée CQ disponible",
+  "validation.details.qc.level": "Niveau",
+  "validation.details.qc.expected": "Attendu",
+  "validation.details.qc.actual": "Réel",
+  "validation.details.qc.cv": "CV%",
+  "validation.details.qc.status": "Statut"
 }
diff --git a/src/main/java/org/openelisglobal/resultvalidation/bean/AnalysisItem.java b/src/main/java/org/openelisglobal/resultvalidation/bean/AnalysisItem.java
index d652f580b2..0c4708a84c 100644
--- a/src/main/java/org/openelisglobal/resultvalidation/bean/AnalysisItem.java
+++ b/src/main/java/org/openelisglobal/resultvalidation/bean/AnalysisItem.java
@@ -193,6 +193,105 @@ public class AnalysisItem implements Serializable {
 
     private boolean isNormal;
 
+    private PatientInfo patientInfoObject;
+    private EnteredByInfo enteredByObject;
+    private String method; // "manual" or "analyzer"
+    private String analyzer;
+    private List<String> flags; // above-normal, below-normal, delta-check, critical
+    private String qcStatus; // pass, fail
+    private String sampleType;
+    private String testDate;
+
+    public static class PatientInfo implements Serializable {
+        private static final long serialVersionUID = 1L;
+        private String name;
+        private String id;
+        private String dob;
+        private String sex;
+        private String age;
+
+        public PatientInfo() {
+        }
+
+        public PatientInfo(String name, String id, String dob, String sex, String age) {
+            this.name = name;
+            this.id = id;
+            this.dob = dob;
+            this.sex = sex;
+            this.age = age;
+        }
+
+        public String getName() {
+            return name;
+        }
+
+        public void setName(String name) {
+            this.name = name;
+        }
+
+        public String getId() {
+            return id;
+        }
+
+        public void setId(String id) {
+            this.id = id;
+        }
+
+        public String getDob() {
+            return dob;
+        }
+
+        public void setDob(String dob) {
+            this.dob = dob;
+        }
+
+        public String getSex() {
+            return sex;
+        }
+
+        public void setSex(String sex) {
+            this.sex = sex;
+        }
+
+        public String getAge() {
+            return age;
+        }
+
+        public void setAge(String age) {
+            this.age = age;
+        }
+    }
+
+    public static class EnteredByInfo implements Serializable {
+        private static final long serialVersionUID = 1L;
+        private String name;
+        private String date;
+
+        public EnteredByInfo() {
+        }
+
+        public EnteredByInfo(String name, String date) {
+            this.name = name;
+            this.date = date;
+        }
+
+        public String getName() {
+            return name;
+        }
+
+        public void setName(String name) {
+            this.name = name;
+        }
+
+        public String getDate() {
+            return date;
+        }
+
+        public void setDate(String date) {
+            this.date = date;
+        }
+    }
+
     public String getRejectReasonId() {
         return rejectReasonId;
     }
@@ -819,4 +918,68 @@ public String getPatientInfo() {
     public void setPatientInfo(String patientInfo) {
         this.patientInfo = patientInfo;
     }
+
+    public PatientInfo getPatientInfoObject() {
+        return patientInfoObject;
+    }
+
+    public void setPatientInfoObject(PatientInfo patientInfoObject) {
+        this.patientInfoObject = patientInfoObject;
+    }
+
+    public EnteredByInfo getEnteredByObject() {
+        return enteredByObject;
+    }
+
+    public void setEnteredByObject(EnteredByInfo enteredByObject) {
+        this.enteredByObject = enteredByObject;
+    }
+
+    public String getMethod() {
+        return method;
+    }
+
+    public void setMethod(String method) {
+        this.method = method;
+    }
+
+    public String getAnalyzer() {
+        return analyzer;
+    }
+
+    public void setAnalyzer(String analyzer) {
+        this.analyzer = analyzer;
+    }
+
+    public List<String> getFlags() {
+        return flags;
+    }
+
+    public void setFlags(List<String> flags) {
+        this.flags = flags;
+    }
+
+    public String getQcStatus() {
+        return qcStatus;
+    }
+
+    public void setQcStatus(String qcStatus) {
+        this.qcStatus = qcStatus;
+    }
+
+    public String getSampleType() {
+        return sampleType;
+    }
+
+    public void setSampleType(String sampleType) {
+        this.sampleType = sampleType;
+    }
+
+    public String getTestDate() {
+        return testDate;
+    }
+
+    public void setTestDate(String testDate) {
+        this.testDate = testDate;
+    }
 }
diff --git a/src/main/java/org/openelisglobal/resultvalidation/bean/RetestRequest.java b/src/main/java/org/openelisglobal/resultvalidation/bean/RetestRequest.java
new file mode 100644
index 0000000000..9bdbc26669
--- /dev/null
+++ b/src/main/java/org/openelisglobal/resultvalidation/bean/RetestRequest.java
@@ -0,0 +1,81 @@
+/**
+ * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
+ * you may not use this file except in compliance with the License. You may obtain a copy of the
+ * License at http://www.mozilla.org/MPL/
+ *
+ * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
+ * ANY KIND, either express or implied. See the License for the specific language governing rights
+ * and limitations under the License.
+ *
+ * <p>The Original Code is OpenELIS code.
+ *
+ * <p>Copyright (C) The Minnesota Department of Health. All Rights Reserved.
+ */
+package org.openelisglobal.resultvalidation.bean;
+
+import jakarta.validation.constraints.NotEmpty;
+import jakarta.validation.constraints.NotNull;
+import java.io.Serializable;
+import java.util.ArrayList;
+import java.util.List;
+import org.openelisglobal.validation.annotations.SafeHtml;
+
+/**
+ * Request bean for sending results back for retest
+ */
+public class RetestRequest implements Serializable {
+
+    private static final long serialVersionUID = 1L;
+
+    @NotNull(message = "Result IDs are required")
+    @NotEmpty(message = "At least one result ID is required")
+    private List<String> resultIds = new ArrayList<>();
+
+    @NotNull(message = "Retest reason is required")
+    @NotEmpty(message = "Retest reason cannot be empty")
+    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
+    private String reason;
+
+    private String requestedBy;
+    private String requestedAt;
+
+    public RetestRequest() {
+    }
+
+    public RetestRequest(List<String> resultIds, String reason) {
+        this.resultIds = resultIds;
+        this.reason = reason;
+    }
+
+    public List<String> getResultIds() {
+        return resultIds;
+    }
+
+    public void setResultIds(List<String> resultIds) {
+        this.resultIds = resultIds;
+    }
+
+    public String getReason() {
+        return reason;
+    }
+
+    public void setReason(String reason) {
+        this.reason = reason;
+    }
+
+    public String getRequestedBy() {
+        return requestedBy;
+    }
+
+    public void setRequestedBy(String requestedBy) {
+        this.requestedBy = requestedBy;
+    }
+
+    public String getRequestedAt() {
+        return requestedAt;
+    }
+
+    public void setRequestedAt(String requestedAt) {
+        this.requestedAt = requestedAt;
+    }
+}
diff --git a/src/main/java/org/openelisglobal/resultvalidation/bean/ValidationDetailsDTO.java b/src/main/java/org/openelisglobal/resultvalidation/bean/ValidationDetailsDTO.java
new file mode 100644
index 0000000000..b85026c348
--- /dev/null
+++ b/src/main/java/org/openelisglobal/resultvalidation/bean/ValidationDetailsDTO.java
@@ -0,0 +1,443 @@
+/**
+ * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
+ * you may not use this file except in compliance with the License. You may obtain a copy of the
+ * License at http://www.mozilla.org/MPL/
+ *
+ * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
+ * ANY KIND, either express or implied. See the License for the specific language governing rights
+ * and limitations under the License.
+ *
+ * <p>The Original Code is OpenELIS code.
+ *
+ * <p>Copyright (C) The Minnesota Department of Health. All Rights Reserved.
+ */
+package org.openelisglobal.resultvalidation.bean;
+
+import java.io.Serializable;
+import java.util.ArrayList;
+import java.util.List;
+
+/**
+ * DTO for on-demand validation details data (History, QC, Method/Reagents,
+ * Order Info, Attachments) Used for expandable row details that are fetched
+ * when needed
+ */
+public class ValidationDetailsDTO implements Serializable {
+
+    private static final long serialVersionUID = 1L;
+
+    private List<PreviousResult> previousResults = new ArrayList<>();
+    private List<QCResult> qcData = new ArrayList<>();
+    private List<ReagentLot> reagentLots = new ArrayList<>();
+    private OrderInfo orderInfo;
+    private List<Attachment> attachments = new ArrayList<>();
+    private DeltaCheck deltaCheck;
+
+    public ValidationDetailsDTO() {
+    }
+
+    public static class PreviousResult implements Serializable {
+        private static final long serialVersionUID = 1L;
+        private String date;
+        private String value;
+        private String status; // normal, abnormal, low-normal, high-normal
+
+        public PreviousResult() {
+        }
+
+        public PreviousResult(String date, String value, String status) {
+            this.date = date;
+            this.value = value;
+            this.status = status;
+        }
+
+        public String getDate() {
+            return date;
+        }
+
+        public void setDate(String date) {
+            this.date = date;
+        }
+
+        public String getValue() {
+            return value;
+        }
+
+        public void setValue(String value) {
+            this.value = value;
+        }
+
+        public String getStatus() {
+            return status;
+        }
+
+        public void setStatus(String status) {
+            this.status = status;
+        }
+    }
+
+    public static class QCResult implements Serializable {
+        private static final long serialVersionUID = 1L;
+        private String level;
+        private String expected;
+        private String actual;
+        private String status; // pass, fail
+        private String cv; // coefficient of variation
+
+        public QCResult() {
+        }
+
+        public QCResult(String level, String expected, String actual, String status, String cv) {
+            this.level = level;
+            this.expected = expected;
+            this.actual = actual;
+            this.status = status;
+            this.cv = cv;
+        }
+
+        public String getLevel() {
+            return level;
+        }
+
+        public void setLevel(String level) {
+            this.level = level;
+        }
+
+        public String getExpected() {
+            return expected;
+        }
+
+        public void setExpected(String expected) {
+            this.expected = expected;
+        }
+
+        public String getActual() {
+            return actual;
+        }
+
+        public void setActual(String actual) {
+            this.actual = actual;
+        }
+
+        public String getStatus() {
+            return status;
+        }
+
+        public void setStatus(String status) {
+            this.status = status;
+        }
+
+        public String getCv() {
+            return cv;
+        }
+
+        public void setCv(String cv) {
+            this.cv = cv;
+        }
+    }
+
+    public static class ReagentLot implements Serializable {
+        private static final long serialVersionUID = 1L;
+        private String name;
+        private String lot;
+        private String expires;
+        private String status; // ok, expiring-soon, expired
+
+        public ReagentLot() {
+        }
+
+        public ReagentLot(String name, String lot, String expires, String status) {
+            this.name = name;
+            this.lot = lot;
+            this.expires = expires;
+            this.status = status;
+        }
+
+        public String getName() {
+            return name;
+        }
+
+        public void setName(String name) {
+            this.name = name;
+        }
+
+        public String getLot() {
+            return lot;
+        }
+
+        public void setLot(String lot) {
+            this.lot = lot;
+        }
+
+        public String getExpires() {
+            return expires;
+        }
+
+        public void setExpires(String expires) {
+            this.expires = expires;
+        }
+
+        public String getStatus() {
+            return status;
+        }
+
+        public void setStatus(String status) {
+            this.status = status;
+        }
+    }
+
+    public static class OrderInfo implements Serializable {
+        private static final long serialVersionUID = 1L;
+        private String clinician;
+        private String clinicianPhone;
+        private String department;
+        private String priority;
+        private String collectionDate;
+        private String receivedDate;
+        private String clinicalHistory;
+        private String diagnosis;
+        private String fastingStatus;
+        private String medicationList;
+
+        public OrderInfo() {
+        }
+
+        public String getClinician() {
+            return clinician;
+        }
+
+        public void setClinician(String clinician) {
+            this.clinician = clinician;
+        }
+
+        public String getClinicianPhone() {
+            return clinicianPhone;
+        }
+
+        public void setClinicianPhone(String clinicianPhone) {
+            this.clinicianPhone = clinicianPhone;
+        }
+
+        public String getDepartment() {
+            return department;
+        }
+
+        public void setDepartment(String department) {
+            this.department = department;
+        }
+
+        public String getPriority() {
+            return priority;
+        }
+
+        public void setPriority(String priority) {
+            this.priority = priority;
+        }
+
+        public String getCollectionDate() {
+            return collectionDate;
+        }
+
+        public void setCollectionDate(String collectionDate) {
+            this.collectionDate = collectionDate;
+        }
+
+        public String getReceivedDate() {
+            return receivedDate;
+        }
+
+        public void setReceivedDate(String receivedDate) {
+            this.receivedDate = receivedDate;
+        }
+
+        public String getClinicalHistory() {
+            return clinicalHistory;
+        }
+
+        public void setClinicalHistory(String clinicalHistory) {
+            this.clinicalHistory = clinicalHistory;
+        }
+
+        public String getDiagnosis() {
+            return diagnosis;
+        }
+
+        public void setDiagnosis(String diagnosis) {
+            this.diagnosis = diagnosis;
+        }
+
+        public String getFastingStatus() {
+            return fastingStatus;
+        }
+
+        public void setFastingStatus(String fastingStatus) {
+            this.fastingStatus = fastingStatus;
+        }
+
+        public String getMedicationList() {
+            return medicationList;
+        }
+
+        public void setMedicationList(String medicationList) {
+            this.medicationList = medicationList;
+        }
+    }
+
+    public static class Attachment implements Serializable {
+        private static final long serialVersionUID = 1L;
+        private String id;
+        private String name;
+        private String type; // pdf, image, etc.
+        private String size;
+        private String uploadedBy;
+        private String uploadedAt;
+        private String source; // order, result
+
+        public Attachment() {
+        }
+
+        public String getId() {
+            return id;
+        }
+
+        public void setId(String id) {
+            this.id = id;
+        }
+
+        public String getName() {
+            return name;
+        }
+
+        public void setName(String name) {
+            this.name = name;
+        }
+
+        public String getType() {
+            return type;
+        }
+
+        public void setType(String type) {
+            this.type = type;
+        }
+
+        public String getSize() {
+            return size;
+        }
+
+        public void setSize(String size) {
+            this.size = size;
+        }
+
+        public String getUploadedBy() {
+            return uploadedBy;
+        }
+
+        public void setUploadedBy(String uploadedBy) {
+            this.uploadedBy = uploadedBy;
+        }
+
+        public String getUploadedAt() {
+            return uploadedAt;
+        }
+
+        public void setUploadedAt(String uploadedAt) {
+            this.uploadedAt = uploadedAt;
+        }
+
+        public String getSource() {
+            return source;
+        }
+
+        public void setSource(String source) {
+            this.source = source;
+        }
+    }
+
+    public static class DeltaCheck implements Serializable {
+        private static final long serialVersionUID = 1L;
+        private String previous;
+        private String change;
+        private String threshold;
+
+        public DeltaCheck() {
+        }
+
+        public DeltaCheck(String previous, String change, String threshold) {
+            this.previous = previous;
+            this.change = change;
+            this.threshold = threshold;
+        }
+
+        public String getPrevious() {
+            return previous;
+        }
+
+        public void setPrevious(String previous) {
+            this.previous = previous;
+        }
+
+        public String getChange() {
+            return change;
+        }
+
+        public void setChange(String change) {
+            this.change = change;
+        }
+
+        public String getThreshold() {
+            return threshold;
+        }
+
+        public void setThreshold(String threshold) {
+            this.threshold = threshold;
+        }
+    }
+
+    public List<PreviousResult> getPreviousResults() {
+        return previousResults;
+    }
+
+    public void setPreviousResults(List<PreviousResult> previousResults) {
+        this.previousResults = previousResults;
+    }
+
+    public List<QCResult> getQcData() {
+        return qcData;
+    }
+
+    public void setQcData(List<QCResult> qcData) {
+        this.qcData = qcData;
+    }
+
+    public List<ReagentLot> getReagentLots() {
+        return reagentLots;
+    }
+
+    public void setReagentLots(List<ReagentLot> reagentLots) {
+        this.reagentLots = reagentLots;
+    }
+
+    public OrderInfo getOrderInfo() {
+        return orderInfo;
+    }
+
+    public void setOrderInfo(OrderInfo orderInfo) {
+        this.orderInfo = orderInfo;
+    }
+
+    public List<Attachment> getAttachments() {
+        return attachments;
+    }
+
+    public void setAttachments(List<Attachment> attachments) {
+        this.attachments = attachments;
+    }
+
+    public DeltaCheck getDeltaCheck() {
+        return deltaCheck;
+    }
+
+    public void setDeltaCheck(DeltaCheck deltaCheck) {
+        this.deltaCheck = deltaCheck;
+    }
+}
diff --git a/src/main/java/org/openelisglobal/resultvalidation/controller/rest/AccessionValidationRestController.java b/src/main/java/org/openelisglobal/resultvalidation/controller/rest/AccessionValidationRestController.java
index 6ef6f6fe48..72e65fa1bd 100644
--- a/src/main/java/org/openelisglobal/resultvalidation/controller/rest/AccessionValidationRestController.java
+++ b/src/main/java/org/openelisglobal/resultvalidation/controller/rest/AccessionValidationRestController.java
@@ -5,6 +5,7 @@
 import jakarta.servlet.http.HttpServletRequest;
 import java.lang.reflect.InvocationTargetException;
 import java.util.*;
+import java.util.stream.Collectors;
 import org.apache.commons.lang3.StringUtils;
 import org.openelisglobal.analysis.service.AnalysisService;
 import org.openelisglobal.analysis.valueholder.Analysis;
@@ -23,6 +24,7 @@
 import org.openelisglobal.common.services.registration.interfaces.IResultUpdate;
 import org.openelisglobal.common.services.serviceBeans.ResultSaveBean;
 import org.openelisglobal.common.util.ConfigurationProperties;
+import org.openelisglobal.common.util.DateUtil;
 import org.openelisglobal.common.util.IdValuePair;
 import org.openelisglobal.common.util.validator.GenericValidator;
 import org.openelisglobal.common.validator.BaseErrors;
@@ -101,6 +103,8 @@ public class AccessionValidationRestController extends BaseResultValidationContr
     private ResultValidationService resultValidationService;
     private NoteService noteService;
     private FhirTransformService fhirTransformService;
+    @Autowired
+    private org.openelisglobal.resultvalidation.service.ValidationDetailsService validationDetailsService;
 
     private final String RESULT_SUBJECT = "Result Note";
     private final String RESULT_TABLE_ID;
@@ -136,10 +140,18 @@ public void initBinder(WebDataBinder binder) {
     @ResponseBody
     public ResultValidationForm showAccessionValidationRange(HttpServletRequest request,
             @RequestParam(required = false) String accessionNumber, @RequestParam(required = false) String date,
-            @RequestParam(required = false) String unitType, @RequestParam(defaultValue = "true") Boolean doRange)
+            @RequestParam(required = false) String unitType, @RequestParam(required = false) String q,
+            @RequestParam(required = false) String labUnit, @RequestParam(required = false) String labNumberFrom,
+            @RequestParam(required = false) String labNumberTo, @RequestParam(required = false) String dateFrom,
+            @RequestParam(required = false) String dateTo, @RequestParam(required = false) String testSection,
+            @RequestParam(required = false) String analyzer, @RequestParam(required = false) String enteredBy,
+            @RequestParam(required = false) Boolean flagged, @RequestParam(required = false) Boolean normal,
+            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer pageSize,
+            @RequestParam(defaultValue = "true") Boolean doRange)
             throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
 
         ResultValidationForm newForm = new ResultValidationForm();
+
         if (StringUtils.isNotBlank(accessionNumber)) {
             newForm.setAccessionNumber(accessionNumber);
         } else if (StringUtils.isNotBlank(date)) {
@@ -147,6 +159,15 @@ public ResultValidationForm showAccessionValidationRange(HttpServletRequest requ
         } else if (StringUtils.isNotBlank(unitType)) {
             newForm.setTestSectionId(unitType);
         }
+
+        if (StringUtils.isNotBlank(q)) {
+            newForm.setAccessionNumber(q);
+        }
+
+        if (StringUtils.isNotBlank(labUnit)) {
+            newForm.setTestSectionId(labUnit);
+        }
+
         return getResultValidation(request, newForm, doRange);
     }
 
@@ -207,6 +228,9 @@ private ResultValidationForm getResultValidation(HttpServletRequest request, Res
 
                 filteredresultList = userService.filterAnalysisResultsByLabUnitRoles(getSysUserId(request), resultList,
                         Constants.ROLE_VALIDATION);
+
+                filteredresultList = applyAdditionalFilters(request, filteredresultList);
+
                 request.setAttribute("pageSize", filteredresultList.size());
                 form.setSearchFinished(true);
             } else {
@@ -245,6 +269,14 @@ public ResultValidationForm showAccessionValidationRangeSave(HttpServletRequest
             @Validated(ResultValidationForm.ResultValidation.class) @RequestBody ResultValidationForm form,
             BindingResult result) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
 
+        int resultCount = form.getResultList() != null ? form.getResultList().size() : 0;
+        int acceptedCount = 0;
+        if (form.getResultList() != null) {
+            acceptedCount = (int) form.getResultList().stream().filter(AnalysisItem::getIsAccepted).count();
+        }
+        LogEvent.logInfo(this.getClass().getSimpleName(), "showAccessionValidationRangeSave",
+                "Processing validation: " + resultCount + " items, " + acceptedCount + " marked for acceptance");
+
         if ("true".equals(request.getParameter("pageResults"))) {
             return getResultValidation(request, form, false);
         }
@@ -260,10 +292,15 @@ public ResultValidationForm showAccessionValidationRangeSave(HttpServletRequest
 
         List<Result> checkPagedResults = (List<Result>) request.getSession()
                 .getAttribute(IActionConstants.RESULTS_SESSION_CACHE);
+
+        if (checkPagedResults == null || checkPagedResults.isEmpty()) {
+            return processValidationDirectly(request, form);
+        }
+
         List<Result> checkResults = (List<Result>) checkPagedResults.get(0);
         if (checkResults.size() == 0) {
             LogEvent.logDebug(this.getClass().getSimpleName(), "ResultValidation()", "Attempted save of stale page.");
-            return form;
+            return processValidationDirectly(request, form);
         }
 
         ResultValidationPaging paging = new ResultValidationPaging();
@@ -614,6 +651,228 @@ private void setEmptyResults(ResultValidationForm form)
         form.setResultList(new ArrayList<AnalysisItem>());
     }
 
+    /**
+     * Get detailed validation information for a specific analysis result This
+     * endpoint is called on-demand when a result row is expanded
+     * 
+     * @param analysisId The analysis ID
+     * @return ValidationDetailsDTO containing history, QC data, reagent lots, order
+     *         info, attachments
+     */
+    @GetMapping(value = "AccessionValidation/{analysisId}/details", produces = MediaType.APPLICATION_JSON_VALUE)
+    @ResponseBody
+    public org.openelisglobal.resultvalidation.bean.ValidationDetailsDTO getValidationDetails(
+            @PathVariable String analysisId) {
+        return validationDetailsService.getValidationDetails(analysisId);
+    }
+
+    @PostMapping(value = "AccessionValidation/retest", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
+    @ResponseBody
+    public Map<String, Object> requestRetest(HttpServletRequest request,
+            @jakarta.validation.Valid @RequestBody org.openelisglobal.resultvalidation.bean.RetestRequest retestRequest,
+            BindingResult bindingResult) {
+
+        Map<String, Object> response = new HashMap<>();
+
+        if (bindingResult.hasErrors()) {
+            response.put("success", false);
+            response.put("message", "Validation errors");
+            response.put("errors", bindingResult.getAllErrors());
+            return response;
+        }
+
+        try {
+            String sysUserId = getSysUserId(request);
+            retestRequest.setRequestedBy(sysUserId);
+            retestRequest.setRequestedAt(java.time.LocalDateTime.now()
+                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
+
+            String technicalAcceptanceStatusId = SpringContext.getBean(IStatusService.class)
+                    .getStatusID(AnalysisStatus.TechnicalAcceptance);
+            String biologistRejectedStatusId = SpringContext.getBean(IStatusService.class)
+                    .getStatusID(AnalysisStatus.BiologistRejected);
+
+            int processedCount = 0;
+            for (String analysisId : retestRequest.getResultIds()) {
+                Analysis analysis = analysisService.get(analysisId);
+                if (analysis != null && technicalAcceptanceStatusId.equals(analysis.getStatusId())) {
+                    analysis.setStatusId(biologistRejectedStatusId);
+                    analysis.setSysUserId(sysUserId);
+                    analysisService.update(analysis);
+
+                    Note note = noteService.createSavableNote(analysis, NoteType.INTERNAL,
+                            "[RETEST REQUESTED] " + retestRequest.getReason(), RESULT_SUBJECT, sysUserId);
+                    noteService.insert(note);
+                    processedCount++;
+                }
+            }
+
+            response.put("success", true);
+            response.put("message", "Retest request processed successfully");
+            response.put("count", processedCount);
+
+        } catch (Exception e) {
+            LogEvent.logError(e);
+            response.put("success", false);
+            response.put("message", "Error processing retest request: " + e.getMessage());
+        }
+
+        return response;
+    }
+
+    /**
+     * Apply additional filters from query parameters to the result list
+     */
+    private List<AnalysisItem> applyAdditionalFilters(HttpServletRequest request, List<AnalysisItem> resultList) {
+        List<AnalysisItem> filtered = new ArrayList<>(resultList);
+
+        String labNumberFrom = request.getParameter("labNumberFrom");
+        String labNumberTo = request.getParameter("labNumberTo");
+        if (StringUtils.isNotBlank(labNumberFrom) || StringUtils.isNotBlank(labNumberTo)) {
+            filtered = filtered.stream().filter(item -> {
+                String accessionNumber = item.getAccessionNumber();
+                if (StringUtils.isNotBlank(labNumberFrom) && accessionNumber.compareTo(labNumberFrom) < 0) {
+                    return false;
+                }
+                if (StringUtils.isNotBlank(labNumberTo) && accessionNumber.compareTo(labNumberTo) > 0) {
+                    return false;
+                }
+                return true;
+            }).collect(Collectors.toList());
+        }
+
+        String dateFrom = request.getParameter("dateFrom");
+        String dateTo = request.getParameter("dateTo");
+        if (StringUtils.isNotBlank(dateFrom) || StringUtils.isNotBlank(dateTo)) {
+            try {
+                Date fromDate = StringUtils.isNotBlank(dateFrom) ? DateUtil.convertStringDateToSqlDate(dateFrom) : null;
+                Date toDate = StringUtils.isNotBlank(dateTo) ? DateUtil.convertStringDateToSqlDate(dateTo) : null;
+                final Date finalFromDate = fromDate;
+                final Date finalToDate = toDate;
+
+                filtered = filtered.stream().filter(item -> {
+                    if (item.getTestDate() == null) {
+                        return false;
+                    }
+                    Date testDate = DateUtil.convertStringDateToSqlDate(item.getTestDate());
+                    if (finalFromDate != null && testDate.before(finalFromDate)) {
+                        return false;
+                    }
+                    if (finalToDate != null && testDate.after(finalToDate)) {
+                        return false;
+                    }
+                    return true;
+                }).collect(Collectors.toList());
+            } catch (Exception e) {
+                LogEvent.logWarn(this.getClass().getSimpleName(), "applyAdditionalFilters",
+                        "Error parsing date filters: " + e.getMessage());
+            }
+        }
+
+        String analyzer = request.getParameter("analyzer");
+        if (StringUtils.isNotBlank(analyzer)) {
+            filtered = filtered.stream().filter(item -> analyzer.equals(item.getAnalyzer()))
+                    .collect(Collectors.toList());
+        }
+
+        String enteredBy = request.getParameter("enteredBy");
+        if (StringUtils.isNotBlank(enteredBy)) {
+            filtered = filtered.stream().filter(item -> {
+                if (item.getEnteredByObject() != null) {
+                    return enteredBy.equals(item.getEnteredByObject().getName());
+                }
+                return false;
+            }).collect(Collectors.toList());
+        }
+
+        String normalParam = request.getParameter("normal");
+        if (StringUtils.isNotBlank(normalParam)) {
+            boolean isNormal = Boolean.parseBoolean(normalParam);
+            filtered = filtered.stream().filter(item -> item.isNormal() == isNormal).collect(Collectors.toList());
+        }
+
+        String flaggedParam = request.getParameter("flagged");
+        if (StringUtils.isNotBlank(flaggedParam) && Boolean.parseBoolean(flaggedParam)) {
+            filtered = filtered.stream().filter(item -> item.getFlags() != null && !item.getFlags().isEmpty())
+                    .collect(Collectors.toList());
+        }
+
+        return filtered;
+    }
+
+    /**
+     * Process validation directly from the form without relying on session cache.
+     * This handles REST API calls from the new React frontend.
+     */
+    private ResultValidationForm processValidationDirectly(HttpServletRequest request, ResultValidationForm form) {
+        List<AnalysisItem> resultItemList = form.getResultList();
+        if (resultItemList == null || resultItemList.isEmpty()) {
+            return form;
+        }
+
+        createSystemUser();
+
+        List<Analysis> analysisUpdateList = new ArrayList<>();
+        ArrayList<Sample> sampleUpdateList = new ArrayList<>();
+        ArrayList<Note> noteUpdateList = new ArrayList<>();
+        ArrayList<Result> resultUpdateList = new ArrayList<>();
+        List<Result> deletableList = new ArrayList<>();
+
+        IResultSaveService resultSaveService = new ResultValidationSaveService();
+        List<IResultUpdate> updaters = ValidationUpdateRegister.getRegisteredUpdaters();
+
+        List<String> analysisIdList = new ArrayList<>();
+
+        for (AnalysisItem analysisItem : resultItemList) {
+            if (analysisItem.getIsAccepted() || analysisItem.getIsRejected()) {
+                Analysis analysis = analysisService.get(analysisItem.getAnalysisId());
+                if (analysis == null) {
+                    LogEvent.logWarn(this.getClass().getSimpleName(), "processValidationDirectly",
+                            "Analysis not found for id: " + analysisItem.getAnalysisId());
+                    continue;
+                }
+
+                analysis.setSysUserId(getSysUserId(request));
+
+                if (!analysisIdList.contains(analysis.getId())) {
+                    if (analysisItem.getIsAccepted()) {
+                        analysis.setStatusId(
+                                SpringContext.getBean(IStatusService.class).getStatusID(AnalysisStatus.Finalized));
+                        analysis.setReleasedDate(new java.sql.Date(Calendar.getInstance().getTimeInMillis()));
+                        analysisIdList.add(analysis.getId());
+                        analysisUpdateList.add(analysis);
+                    }
+
+                    if (analysisItem.getIsRejected()) {
+                        analysis.setStatusId(SpringContext.getBean(IStatusService.class)
+                                .getStatusID(AnalysisStatus.BiologistRejected));
+                        analysisIdList.add(analysis.getId());
+                        analysisUpdateList.add(analysis);
+                    }
+                }
+            }
+        }
+
+        if (!analysisUpdateList.isEmpty()) {
+            try {
+                resultValidationService.persistdata(deletableList, analysisUpdateList, resultUpdateList, resultItemList,
+                        sampleUpdateList, noteUpdateList, resultSaveService, updaters, getSysUserId(request));
+
+                try {
+                    fhirTransformService.transformPersistResultValidationFhirObjects(deletableList, analysisUpdateList,
+                            resultUpdateList, resultItemList, sampleUpdateList, noteUpdateList);
+                } catch (FhirLocalPersistingException e) {
+                    LogEvent.logError(e);
+                }
+            } catch (LIMSRuntimeException e) {
+                LogEvent.logError(e);
+            }
+        }
+
+        form.setSearchFinished(true);
+        return form;
+    }
+
     @Override
     protected String findLocalForward(String forward) {
         if (FWD_SUCCESS.equals(forward)) {
diff --git a/src/main/java/org/openelisglobal/resultvalidation/service/ResultValidationServiceImpl.java b/src/main/java/org/openelisglobal/resultvalidation/service/ResultValidationServiceImpl.java
index 90ad55b6ef..8c15f8f966 100644
--- a/src/main/java/org/openelisglobal/resultvalidation/service/ResultValidationServiceImpl.java
+++ b/src/main/java/org/openelisglobal/resultvalidation/service/ResultValidationServiceImpl.java
@@ -75,14 +75,13 @@ public void persistdata(List<Result> deletableList, List<Analysis> analysisUpdat
             }
         }
 
-        checkIfSamplesFinished(resultItemList, sampleUpdateList);
+        checkIfSamplesFinished(resultItemList, sampleUpdateList, sysUserId);
 
-        // update finished samples
         for (Sample sample : sampleUpdateList) {
+            sample.setSysUserId(sysUserId);
             sampleService.update(sample);
         }
 
-        // create or update notes
         for (Note note : noteUpdateList) {
             if (note != null) {
                 if (note.getId() == null) {
@@ -111,14 +110,18 @@ private boolean isResultAnalysisFinalized(Result result, List<Analysis> analysis
         return false;
     }
 
-    private void checkIfSamplesFinished(List<AnalysisItem> resultItemList, List<Sample> sampleUpdateList) {
+    private void checkIfSamplesFinished(List<AnalysisItem> resultItemList, List<Sample> sampleUpdateList,
+            String sysUserId) {
         String currentSampleId = "";
         boolean sampleFinished = true;
         List<Integer> sampleFinishedStatus = getSampleFinishedStatuses();
 
         for (AnalysisItem analysisItem : resultItemList) {
-            String analysisSampleId = sampleService.getSampleByAccessionNumber(analysisItem.getAccessionNumber())
-                    .getId();
+            Sample analysisSample = sampleService.getSampleByAccessionNumber(analysisItem.getAccessionNumber());
+            if (analysisSample == null) {
+                continue;
+            }
+            String analysisSampleId = analysisSample.getId();
             if (!analysisSampleId.equals(currentSampleId)) {
 
                 currentSampleId = analysisSampleId;
@@ -135,6 +138,7 @@ private void checkIfSamplesFinished(List<AnalysisItem> resultItemList, List<Samp
                 if (sampleFinished) {
                     Sample sample = sampleService.get(currentSampleId);
                     sample.setStatusId(SpringContext.getBean(IStatusService.class).getStatusID(OrderStatus.Finished));
+                    sample.setSysUserId(sysUserId);
                     sampleUpdateList.add(sample);
                 }
 
diff --git a/src/main/java/org/openelisglobal/resultvalidation/service/ValidationDetailsService.java b/src/main/java/org/openelisglobal/resultvalidation/service/ValidationDetailsService.java
new file mode 100644
index 0000000000..f10ab79e9b
--- /dev/null
+++ b/src/main/java/org/openelisglobal/resultvalidation/service/ValidationDetailsService.java
@@ -0,0 +1,37 @@
+/**
+ * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
+ * you may not use this file except in compliance with the License. You may obtain a copy of the
+ * License at http://www.mozilla.org/MPL/
+ *
+ * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
+ * ANY KIND, either express or implied. See the License for the specific language governing rights
+ * and limitations under the License.
+ *
+ * <p>The Original Code is OpenELIS code.
+ *
+ * <p>Copyright (C) The Minnesota Department of Health. All Rights Reserved.
+ */
+package org.openelisglobal.resultvalidation.service;
+
+import org.openelisglobal.resultvalidation.bean.ValidationDetailsDTO;
+
+/**
+ * Service for fetching detailed validation data on-demand Used for expandable
+ * row details (History, QC, Method/Reagents, Order Info, Attachments)
+ */
+public interface ValidationDetailsService {
+
+    ValidationDetailsDTO getValidationDetails(String analysisId);
+
+    java.util.List<ValidationDetailsDTO.PreviousResult> getPreviousResults(String testId, String patientId);
+
+    java.util.List<ValidationDetailsDTO.QCResult> getQCData(String analyzerId, String testId);
+
+    java.util.List<ValidationDetailsDTO.ReagentLot> getReagentLots(String analysisId);
+
+    ValidationDetailsDTO.OrderInfo getOrderInfo(String analysisId);
+
+    java.util.List<ValidationDetailsDTO.Attachment> getAttachments(String analysisId);
+
+    ValidationDetailsDTO.DeltaCheck calculateDeltaCheck(String analysisId, String currentValue);
+}
diff --git a/src/main/java/org/openelisglobal/resultvalidation/service/ValidationDetailsServiceImpl.java b/src/main/java/org/openelisglobal/resultvalidation/service/ValidationDetailsServiceImpl.java
new file mode 100644
index 0000000000..98321e5f20
--- /dev/null
+++ b/src/main/java/org/openelisglobal/resultvalidation/service/ValidationDetailsServiceImpl.java
@@ -0,0 +1,192 @@
+package org.openelisglobal.resultvalidation.service;
+
+import java.time.ZoneId;
+import java.time.format.DateTimeFormatter;
+import java.util.ArrayList;
+import java.util.List;
+import org.openelisglobal.analysis.service.AnalysisService;
+import org.openelisglobal.analysis.valueholder.Analysis;
+import org.openelisglobal.patient.service.PatientService;
+import org.openelisglobal.patient.valueholder.Patient;
+import org.openelisglobal.result.service.ResultService;
+import org.openelisglobal.result.valueholder.Result;
+import org.openelisglobal.resultvalidation.bean.ValidationDetailsDTO;
+import org.openelisglobal.sample.service.SampleService;
+import org.openelisglobal.sample.valueholder.Sample;
+import org.openelisglobal.samplehuman.service.SampleHumanService;
+import org.openelisglobal.test.service.TestService;
+import org.openelisglobal.test.valueholder.Test;
+import org.springframework.beans.factory.annotation.Autowired;
+import org.springframework.stereotype.Service;
+import org.springframework.transaction.annotation.Transactional;
+
+@Service
+public class ValidationDetailsServiceImpl implements ValidationDetailsService {
+
+    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MM/dd/yyyy");
+    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm");
+
+    @Autowired
+    private AnalysisService analysisService;
+
+    @Autowired
+    private ResultService resultService;
+
+    @Autowired
+    private SampleHumanService sampleHumanService;
+
+    @Autowired
+    private PatientService patientService;
+
+    @Autowired
+    private SampleService sampleService;
+
+    @Autowired
+    private TestService testService;
+
+    @Override
+    @Transactional(readOnly = true)
+    public ValidationDetailsDTO getValidationDetails(String analysisId) {
+        ValidationDetailsDTO details = new ValidationDetailsDTO();
+
+        Analysis analysis = analysisService.get(analysisId);
+        if (analysis == null) {
+            return details;
+        }
+
+        Sample sample = analysis.getSampleItem().getSample();
+        Patient patient = sampleHumanService.getPatientForSample(sample);
+
+        if (patient != null) {
+            String testId = analysis.getTest().getId();
+            details.setPreviousResults(getPreviousResults(testId, patient.getId()));
+        }
+
+        details.setQcData(new ArrayList<>());
+        details.setReagentLots(new ArrayList<>());
+        details.setOrderInfo(getOrderInfo(analysisId));
+        details.setAttachments(new ArrayList<>());
+
+        List<Result> results = resultService.getResultsByAnalysis(analysis);
+        if (!results.isEmpty()) {
+            Result currentResult = results.get(0);
+            details.setDeltaCheck(calculateDeltaCheck(analysisId, currentResult.getValue()));
+        }
+
+        return details;
+    }
+
+    @Override
+    @Transactional(readOnly = true)
+    public List<ValidationDetailsDTO.PreviousResult> getPreviousResults(String testId, String patientId) {
+        List<ValidationDetailsDTO.PreviousResult> previousResults = new ArrayList<>();
+
+        Test test = testService.get(testId);
+        Patient patient = patientService.get(patientId);
+
+        if (test == null || patient == null) {
+            return previousResults;
+        }
+
+        List<Sample> patientSamples = sampleService.getSamplesForPatient(patientId);
+
+        String finalizedStatusId = org.openelisglobal.common.services.StatusService.getInstance()
+                .getStatusID(org.openelisglobal.common.services.StatusService.AnalysisStatus.Finalized);
+
+        for (Sample sample : patientSamples) {
+            List<Analysis> analyses = analysisService.getAnalysesBySampleId(sample.getId());
+
+            for (Analysis analysis : analyses) {
+                if (!testId.equals(analysis.getTest().getId())) {
+                    continue;
+                }
+                if (analysis.getStatusId() == null || !analysis.getStatusId().equals(finalizedStatusId)) {
+                    continue;
+                }
+
+                List<Result> results = resultService.getResultsByAnalysis(analysis);
+                if (!results.isEmpty()) {
+                    Result result = results.get(0);
+                    String value = result.getValue();
+
+                    if (value != null && !value.isEmpty()) {
+                        ValidationDetailsDTO.PreviousResult prevResult = new ValidationDetailsDTO.PreviousResult();
+
+                        if (analysis.getCompletedDate() != null) {
+                            prevResult.setDate(analysis.getCompletedDate().toInstant().atZone(ZoneId.systemDefault())
+                                    .toLocalDate().format(DATE_FORMAT));
+                        }
+                        prevResult.setValue(value);
+                        prevResult.setStatus("normal");
+
+                        previousResults.add(prevResult);
+                    }
+                }
+            }
+        }
+
+        previousResults.sort((a, b) -> {
+            if (a.getDate() == null || b.getDate() == null) {
+                return 0;
+            }
+            return b.getDate().compareTo(a.getDate());
+        });
+
+        if (previousResults.size() > 10) {
+            previousResults = new ArrayList<>(previousResults.subList(0, 10));
+        }
+
+        return previousResults;
+    }
+
+    @Override
+    @Transactional(readOnly = true)
+    public List<ValidationDetailsDTO.QCResult> getQCData(String analyzerId, String testId) {
+        return new ArrayList<>();
+    }
+
+    @Override
+    @Transactional(readOnly = true)
+    public List<ValidationDetailsDTO.ReagentLot> getReagentLots(String analysisId) {
+        return new ArrayList<>();
+    }
+
+    @Override
+    @Transactional(readOnly = true)
+    public ValidationDetailsDTO.OrderInfo getOrderInfo(String analysisId) {
+        ValidationDetailsDTO.OrderInfo orderInfo = new ValidationDetailsDTO.OrderInfo();
+
+        Analysis analysis = analysisService.get(analysisId);
+        if (analysis == null) {
+            return orderInfo;
+        }
+
+        Sample sample = analysis.getSampleItem().getSample();
+
+        if (sample != null) {
+            if (sample.getCollectionDate() != null) {
+                orderInfo.setCollectionDate(sample.getCollectionDate().toInstant().atZone(ZoneId.systemDefault())
+                        .toLocalDateTime().format(DATE_TIME_FORMAT));
+            }
+            if (sample.getReceivedDate() != null) {
+                orderInfo.setReceivedDate(sample.getReceivedDate().toInstant().atZone(ZoneId.systemDefault())
+                        .toLocalDateTime().format(DATE_TIME_FORMAT));
+            }
+            orderInfo.setPriority("Routine");
+        }
+
+        return orderInfo;
+    }
+
+    @Override
+    @Transactional(readOnly = true)
+    public List<ValidationDetailsDTO.Attachment> getAttachments(String analysisId) {
+        return new ArrayList<>();
+    }
+
+    @Override
+    @Transactional(readOnly = true)
+    public ValidationDetailsDTO.DeltaCheck calculateDeltaCheck(String analysisId, String currentValue) {
+        return null;
+    }
+}