import { Page, Locator } from "@playwright/test";

// ── Mock data constants ──────────────────────────────────────────────────────

export const MOCK_QA_EVENTS = [
  { id: "1", value: "Sample not received" },
  { id: "2", value: "Incorrect patient information" },
];

export const MOCK_STATUS_OPTIONS = [
  { id: "21", value: "Entered" },
  { id: "22", value: "Cancelled" },
  { id: "23", value: "Realized" },
];

export const MOCK_EORDER = {
  electronicOrderId: "1",
  externalOrderId: "TEST-EORDER-001",
  requestingFacility: "Test Hospital",
  patientNationalId: "TEST123456",
  patientUpid: "UPID-001",
  gender: "M",
  birthDate: "1990-01-01",
  requestDateDisplay: "01/05/2024",
  collectionDateDisplay: "01/05/2024",
  status: "Entered",
  testName: "HIV Viral Load",
  labNumber: "",
  qaEventId: null,
};

export const MOCK_CANCELLED_EORDER = {
  ...MOCK_EORDER,
  electronicOrderId: "2",
  externalOrderId: "TEST-EORDER-002",
  status: "Cancelled",
  qaEventId: "1",
};

export const MOCK_SEARCH_RESPONSE = {
  eOrders: [MOCK_EORDER],
  paging: { currentPage: 1, totalPages: 1 },
};

export const MOCK_CANCELLED_SEARCH_RESPONSE = {
  eOrders: [MOCK_CANCELLED_EORDER],
  paging: { currentPage: 1, totalPages: 1 },
};

// ── Page Object ──────────────────────────────────────────────────────────────

/**
 * StudyElectronicOrdersPage Page Object
 *
 * Encapsulates route mocking and DOM interactions for the /StudyElectronicOrders
 * page (composed of StudyElectronicOrders, StudyEOrderSearch, and StudyEOrder).
 *
 * Always call mockInitialLoad() and mockStatusList() (via goto()) before
 * navigating, so the page can bootstrap correctly.
 */
export class StudyElectronicOrdersPage {
  readonly page: Page;

  // ── Search section locators ──
  /** Patient code / identifier search input */
  readonly searchValueInput: Locator;
  /** Start date Carbon DatePickerInput (scoped to the <input> to avoid the wrapper <div> match) */
  readonly startDateInput: Locator;
  /** End date Carbon DatePickerInput (scoped to the <input> to avoid the wrapper <div> match) */
  readonly endDateInput: Locator;
  /** Page content area — excludes the header so Search button scoping is unambiguous */
  readonly searchContentArea: Locator;
  /** Status filter select */
  readonly statusSelect: Locator;

  // ── Reject modal locators ──
  /** Outer Carbon Modal element (data-cy attribute) */
  readonly rejectModal: Locator;
  /** Rejection reason select inside modal */
  readonly rejectReasonSelect: Locator;
  /** Authorizer text input inside modal */
  readonly rejectAuthorizerInput: Locator;
  /** Notes textarea inside modal */
  readonly rejectNoteTextArea: Locator;
  /** Carbon modal footer (scopes primary/secondary button lookups) */
  readonly rejectModalFooter: Locator;

  constructor(page: Page) {
    this.page = page;

    this.searchValueInput = page.locator("#searchValue");
    // Carbon DatePicker renders the same id on both the wrapper <div> and the inner
    // <input>. Scoping to `input` avoids the strict-mode "resolved to 2 elements" error.
    this.startDateInput = page.locator("input#studyEOrder_startDate");
    this.endDateInput = page.locator("input#studyEOrder_endDate");
    this.statusSelect = page.locator("#statusId");
    // The Header renders a HeaderGlobalAction with aria-label="Search" which is also a
    // <button role="button"> and appears before the page's Search buttons in the DOM.
    // Scoping to .orderLegendBody (the page content wrapper) excludes it entirely.
    this.searchContentArea = page.locator(".orderLegendBody");

    this.rejectModal = page.locator('[data-cy="reject-eorder-modal"]');
    this.rejectReasonSelect = page.locator("#reject-reason");
    this.rejectAuthorizerInput = page.locator("#reject-authorizer");
    this.rejectNoteTextArea = page.locator("#reject-note");
    this.rejectModalFooter = page.locator(".cds--modal-footer");
  }

  // ── Route mock helpers ───────────────────────────────────────────────────

  /**
   * Intercept the initial GET /rest/StudyElectronicOrders (no searchType).
   * Returns qaEvents and organizationList needed by StudyEOrder on mount.
   */
  async mockInitialLoad(): Promise<void> {
    await this.page.route(
      (url) =>
        url.pathname.includes("/rest/StudyElectronicOrders") &&
        !url.searchParams.has("searchType") &&
        !url.searchParams.has("page"),
      (route) =>
        route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify({
            organizationList: [
              { id: "1", value: "Test Hospital" },
              { id: "2", value: "Test Clinic" },
            ],
            qaEvents: MOCK_QA_EVENTS,
          }),
        }),
    );
  }

  /**
   * Intercept GET /rest/displayList/ELECTRONIC_ORDER_STATUSES.
   * Returns the status options for the status filter dropdown.
   */
  async mockStatusList(): Promise<void> {
    await this.page.route(
      "**/rest/displayList/ELECTRONIC_ORDER_STATUSES",
      (route) =>
        route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify(MOCK_STATUS_OPTIONS),
        }),
    );
  }

  /**
   * Intercept GET /rest/StudyElectronicOrders?searchType=...
   * Call before triggering a search. Defaults to MOCK_SEARCH_RESPONSE.
   */
  async mockSearch(responseBody: object = MOCK_SEARCH_RESPONSE): Promise<void> {
    await this.page.route(
      (url) =>
        url.pathname.includes("/rest/StudyElectronicOrders") &&
        (url.searchParams.has("searchType") || url.searchParams.has("page")),
      (route) =>
        route.fulfill({
          status: 200,
          contentType: "application/json",
          body: JSON.stringify(responseBody),
        }),
    );
  }

  /**
   * Intercept POST /rest/rejectStudyElectronicOrder.
   * Call before triggering a rejection.
   */
  async mockReject(statusCode = 200): Promise<void> {
    await this.page.route("**/rest/rejectStudyElectronicOrder", (route) =>
      route.fulfill({
        status: statusCode,
        contentType: "application/json",
        body: JSON.stringify({ success: true }),
      }),
    );
  }

  // ── Navigation ───────────────────────────────────────────────────────────

  /**
   * Register default mocks (initial load + status list) then navigate to
   * the Study Electronic Orders page.
   */
  async goto(): Promise<void> {
    await this.mockInitialLoad();
    await this.mockStatusList();
    await this.page.goto("/StudyElectronicOrders");
  }

  // ── Element accessors ────────────────────────────────────────────────────

  /** All data rows in the results table body */
  get tableRows(): Locator {
    return this.page.locator("table tbody tr");
  }

  /**
   * The first "Search" button in the page content area (identifier search).
   * Scoped to .orderLegendBody to exclude the Header's search icon button.
   */
  get identifierSearchButton(): Locator {
    return this.searchContentArea
      .getByRole("button", {
        name: /^Search$/i,
      })
      .first();
  }

  /**
   * The last "Search" button in the page content area (date + status search).
   * Scoped to .orderLegendBody to exclude the Header's search icon button.
   */
  get dateSearchButton(): Locator {
    return this.searchContentArea
      .getByRole("button", {
        name: /^Search$/i,
      })
      .last();
  }

  /** "Edit" button in the Nth results row (0-indexed) */
  getRowEditButton(rowIndex = 0): Locator {
    return this.tableRows
      .nth(rowIndex)
      .getByRole("button", { name: /^Edit$/i });
  }

  /** "Reject" or "Rejected" button in the Nth results row (0-indexed) */
  getRowRejectButton(rowIndex = 0): Locator {
    return this.tableRows
      .nth(rowIndex)
      .getByRole("button", { name: /Reject(ed)?/i });
  }

  /**
   * Primary action button in the reject modal footer.
   * Scoped to .cds--modal-footer to avoid matching row-level "Reject" buttons.
   */
  get rejectModalPrimaryButton(): Locator {
    return this.rejectModalFooter.getByRole("button", {
      name: /^Reject$/i,
    });
  }

  /** Secondary (Cancel) button in the reject modal footer */
  get rejectModalCancelButton(): Locator {
    return this.rejectModalFooter.getByRole("button", {
      name: /^Cancel$/i,
    });
  }
}
