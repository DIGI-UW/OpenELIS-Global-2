import { test, expect } from "@playwright/test";
import {
  StudyElectronicOrdersPage,
  MOCK_CANCELLED_SEARCH_RESPONSE,
  MOCK_STATUS_OPTIONS,
  MOCK_QA_EVENTS,
} from "../../../fixtures/study-electronic-orders";

/**
 * Study Electronic Orders — E2E tests
 *
 * All API calls are intercepted via page.route() so the suite runs on the
 * build stack without requiring pre-seeded database records.
 *
 * Project: core-app
 * Run:
 *   cd frontend && TEST_USER=admin TEST_PASS='adminADMIN!' \
 *     npm run pw:test -- playwright/tests/study-electronic-orders.spec.ts \
 *     --project=core-app
 */
test.describe("Study Electronic Orders", () => {
  test("page renders heading and both search sections", async ({ page }) => {
    const eop = new StudyElectronicOrdersPage(page);
    await eop.goto();

    await expect(
      page.getByRole("heading", { name: "Study Electronic Orders" }),
    ).toBeVisible();

    // Identifier search section
    await expect(eop.searchValueInput).toBeVisible();
    await expect(eop.identifierSearchButton).toBeVisible();

    // Date / status search section
    await expect(eop.startDateInput).toBeVisible();
    await expect(eop.endDateInput).toBeVisible();
    await expect(eop.statusSelect).toBeVisible();
    await expect(eop.dateSearchButton).toBeVisible();
  });

  test("status dropdown is populated from API on load", async ({ page }) => {
    const eop = new StudyElectronicOrdersPage(page);
    await eop.goto();

    for (const option of MOCK_STATUS_OPTIONS) {
      await expect(
        eop.statusSelect.locator(`option[value="${option.id}"]`),
      ).toHaveText(option.value);
    }
  });

  test("QA events are loaded for reject modal reason dropdown", async ({
    page,
  }) => {
    const eop = new StudyElectronicOrdersPage(page);
    await eop.mockSearch();
    await eop.goto();

    // Trigger a search so the results table appears
    const searchResponse = page.waitForResponse(
      (resp) =>
        resp.url().includes("/rest/StudyElectronicOrders") &&
        resp.url().includes("searchType=IDENTIFIER"),
    );
    await eop.searchValueInput.fill("TEST123456");
    await eop.identifierSearchButton.click();
    await searchResponse;
    await expect(eop.tableRows.first()).toBeVisible();

    // Open reject modal
    await eop.getRowRejectButton(0).click();
    await expect(
      page.getByRole("heading", { name: "Reject Electronic Order" }),
    ).toBeVisible();

    // Each QA event should be available as an option
    for (const event of MOCK_QA_EVENTS) {
      await expect(
        eop.rejectReasonSelect.locator(`option[value="${event.id}"]`),
      ).toHaveText(event.value);
    }

    await eop.rejectModalCancelButton.click();
  });

  test("identifier search displays results table with order data", async ({
    page,
  }) => {
    const eop = new StudyElectronicOrdersPage(page);
    await eop.mockSearch();
    await eop.goto();

    const searchResponse = page.waitForResponse(
      (resp) =>
        resp.url().includes("/rest/StudyElectronicOrders") &&
        resp.url().includes("searchType=IDENTIFIER"),
    );
    await eop.searchValueInput.fill("TEST123456");
    await eop.identifierSearchButton.click();
    await searchResponse;

    await expect(page.getByText("Test Requests Matching Search")).toBeVisible();
    await expect(eop.tableRows).toHaveCount(1);
    await expect(
      page.getByRole("cell", { name: "Test Hospital" }),
    ).toBeVisible();
    await expect(
      page.getByRole("cell", { name: "HIV Viral Load" }),
    ).toBeVisible();
  });

  test("date and status search displays results table", async ({ page }) => {
    const eop = new StudyElectronicOrdersPage(page);
    await eop.mockSearch();
    await eop.goto();

    const searchResponse = page.waitForResponse(
      (resp) =>
        resp.url().includes("/rest/StudyElectronicOrders") &&
        resp.url().includes("searchType=DATE_STATUS"),
    );
    // Click the date/status Search button directly —
    // the mock responds to any searchType=DATE_STATUS request regardless of dates
    await eop.dateSearchButton.click();
    await searchResponse;

    await expect(page.getByText("Test Requests Matching Search")).toBeVisible();
    await expect(eop.tableRows.first()).toBeVisible();
  });

  test("results table renders expected column headers", async ({ page }) => {
    const eop = new StudyElectronicOrdersPage(page);
    await eop.mockSearch();
    await eop.goto();

    const searchResponse = page.waitForResponse(
      (resp) =>
        resp.url().includes("/rest/StudyElectronicOrders") &&
        resp.url().includes("searchType=IDENTIFIER"),
    );
    await eop.searchValueInput.fill("TEST123456");
    await eop.identifierSearchButton.click();
    await searchResponse;

    await expect(page.getByText("Test Requests Matching Search")).toBeVisible();

    const headers = page.locator("table thead th");
    await expect(
      headers.filter({ hasText: "Requesting Facility" }),
    ).toBeVisible();
    await expect(headers.filter({ hasText: "Patient Code" })).toBeVisible();
    await expect(headers.filter({ hasText: "Status" })).toBeVisible();
    await expect(headers.filter({ hasText: "Actions" })).toBeVisible();
  });

  test("Edit button navigates to SamplePatientEntry with the order ID", async ({
    page,
  }) => {
    const eop = new StudyElectronicOrdersPage(page);
    await eop.mockSearch();
    await eop.goto();

    const searchResponse = page.waitForResponse(
      (resp) =>
        resp.url().includes("/rest/StudyElectronicOrders") &&
        resp.url().includes("searchType=IDENTIFIER"),
    );
    await eop.searchValueInput.fill("TEST123456");
    await eop.identifierSearchButton.click();
    await searchResponse;
    await expect(eop.tableRows.first()).toBeVisible();

    await eop.getRowEditButton(0).click();

    await expect(page).toHaveURL(/\/SamplePatientEntry/);
    await expect(page).toHaveURL(/ID=TEST-EORDER-001/);
  });

  test("Reject button opens modal with all required fields", async ({
    page,
  }) => {
    const eop = new StudyElectronicOrdersPage(page);
    await eop.mockSearch();
    await eop.goto();

    const searchResponse = page.waitForResponse(
      (resp) =>
        resp.url().includes("/rest/StudyElectronicOrders") &&
        resp.url().includes("searchType=IDENTIFIER"),
    );
    await eop.searchValueInput.fill("TEST123456");
    await eop.identifierSearchButton.click();
    await searchResponse;
    await expect(eop.tableRows.first()).toBeVisible();

    await eop.getRowRejectButton(0).click();

    await expect(
      page.getByRole("heading", { name: "Reject Electronic Order" }),
    ).toBeVisible();
    await expect(eop.rejectReasonSelect).toBeVisible();
    await expect(eop.rejectAuthorizerInput).toBeVisible();
    await expect(eop.rejectNoteTextArea).toBeVisible();
  });

  test("Reject modal primary button is disabled until a reason is selected", async ({
    page,
  }) => {
    const eop = new StudyElectronicOrdersPage(page);
    await eop.mockSearch();
    await eop.goto();

    const searchResponse = page.waitForResponse(
      (resp) =>
        resp.url().includes("/rest/StudyElectronicOrders") &&
        resp.url().includes("searchType=IDENTIFIER"),
    );
    await eop.searchValueInput.fill("TEST123456");
    await eop.identifierSearchButton.click();
    await searchResponse;
    await expect(eop.tableRows.first()).toBeVisible();

    await eop.getRowRejectButton(0).click();
    await expect(
      page.getByRole("heading", { name: "Reject Electronic Order" }),
    ).toBeVisible();

    // No reason selected — primary button must be disabled
    await expect(eop.rejectModalPrimaryButton).toBeDisabled();
  });

  test("Reject modal primary button is enabled after a reason is selected", async ({
    page,
  }) => {
    const eop = new StudyElectronicOrdersPage(page);
    await eop.mockSearch();
    await eop.goto();

    const searchResponse = page.waitForResponse(
      (resp) =>
        resp.url().includes("/rest/StudyElectronicOrders") &&
        resp.url().includes("searchType=IDENTIFIER"),
    );
    await eop.searchValueInput.fill("TEST123456");
    await eop.identifierSearchButton.click();
    await searchResponse;
    await expect(eop.tableRows.first()).toBeVisible();

    await eop.getRowRejectButton(0).click();
    await expect(
      page.getByRole("heading", { name: "Reject Electronic Order" }),
    ).toBeVisible();

    await eop.rejectReasonSelect.selectOption({ value: "1" });

    await expect(eop.rejectModalPrimaryButton).toBeEnabled();
  });

  test("successful rejection calls reject API and removes order from table", async ({
    page,
  }) => {
    const eop = new StudyElectronicOrdersPage(page);
    await eop.mockSearch();
    await eop.mockReject();
    await eop.goto();

    const searchResponse = page.waitForResponse(
      (resp) =>
        resp.url().includes("/rest/StudyElectronicOrders") &&
        resp.url().includes("searchType=IDENTIFIER"),
    );
    await eop.searchValueInput.fill("TEST123456");
    await eop.identifierSearchButton.click();
    await searchResponse;
    await expect(eop.tableRows).toHaveCount(1);

    await eop.getRowRejectButton(0).click();
    await expect(
      page.getByRole("heading", { name: "Reject Electronic Order" }),
    ).toBeVisible();

    await eop.rejectReasonSelect.selectOption({ value: "1" });
    await eop.rejectAuthorizerInput.fill("Test Authorizer");
    await eop.rejectNoteTextArea.fill("Test rejection note");

    const rejectResponsePromise = page.waitForResponse(
      "**/rest/rejectStudyElectronicOrder",
    );
    await eop.rejectModalPrimaryButton.click();
    await rejectResponsePromise;

    // Modal should close and rejected order removed from the results table
    await expect(
      page.getByRole("heading", { name: "Reject Electronic Order" }),
    ).toBeHidden();
    await expect(eop.tableRows).toHaveCount(0);
  });

  test("cancelled order shows disabled Edit and Reject buttons", async ({
    page,
  }) => {
    const eop = new StudyElectronicOrdersPage(page);
    await eop.mockSearch(MOCK_CANCELLED_SEARCH_RESPONSE);
    await eop.goto();

    const searchResponse = page.waitForResponse(
      (resp) =>
        resp.url().includes("/rest/StudyElectronicOrders") &&
        resp.url().includes("searchType=IDENTIFIER"),
    );
    await eop.searchValueInput.fill("TEST123456");
    await eop.identifierSearchButton.click();
    await searchResponse;
    await expect(eop.tableRows.first()).toBeVisible();

    await expect(eop.getRowEditButton(0)).toBeDisabled();
    await expect(eop.getRowRejectButton(0)).toBeDisabled();
  });
});
