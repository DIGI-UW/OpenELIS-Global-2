/**
 * NotebookWorkflowPage - Page object for the immunology notebook workflow.
 * Handles navigation and interactions with the 9-page workflow.
 */
class NotebookWorkflowPage {
  constructor() {
    this.selectors = {
      // Navigation
      pageNavigation: ".page-navigation",
      pageList: ".page-list",
      pageItem: ".page-item",
      activePageItem: ".page-item.active",
      navigationTitle: ".navigation-title",
      navigationSummary: ".navigation-summary",

      // Page content
      workflowContainer: ".notebook-workflow-container",
      workflowHeader: ".workflow-header",
      pagePanel: ".page-panel",
      pageHeader: ".page-header",
      pageContent: ".page-content",
      pageSectionHeader: ".page-section-header",

      // Progress tiles
      progressSection: ".progress-section",
      progressTiles: ".progress-tiles",
      progressTile: ".progress-tile",
      progressLabel: ".progress-label",
      progressValue: ".progress-value",

      // Action buttons
      pageActionsBar: ".page-actions-bar",
      bulkApplyButton: 'button:contains("Bulk Apply")',
      refreshButton: 'button:contains("Refresh")',
      markProcessedButton: 'button:contains("Mark Selected as Processed")',
      importManifestButton: 'button:contains("Import from Manifest")',
      searchSamplesButton: 'button:contains("Search & Link")',

      // Sample Grid
      sampleGridContainer: ".sample-grid-container",
      sampleGrid: ".sample-grid",
      dataTable: ".cds--data-table",
      tableRow: ".cds--data-table tbody tr",
      selectAllCheckbox: "#select-all-rows",
      rowCheckbox: 'input[type="checkbox"]',
      statusTag: ".cds--tag",

      // Bulk Apply Modal
      bulkApplyModal: ".cds--modal",
      bulkApplyForm: ".bulk-apply-form",
      bulkApplyFieldsGrid: ".bulk-apply-fields-grid",
      bulkApplySummary: ".bulk-apply-summary",
      modalPrimaryButton: ".cds--modal-footer .cds--btn--primary",
      modalSecondaryButton: ".cds--modal-footer .cds--btn--secondary",

      // Empty state
      emptyState: ".empty-state",

      // Status filter
      statusFilterDropdown: "#status-filter",

      // Pagination
      pagination: ".cds--pagination",

      // Loading
      loading: ".cds--loading",

      // Notifications
      inlineNotification: ".cds--inline-notification",
    };
  }

  /**
   * Visit the notebook workflow page with a specific notebook ID.
   * @param {number} notebookId - The notebook template ID
   */
  visit(notebookId = 1) {
    cy.visit(`/NotebookWorkflow?notebookId=${notebookId}`);
  }

  /**
   * Visit the notebook workflow page with a specific entry ID.
   * @param {number} entryId - The notebook entry ID
   */
  visitByEntry(entryId) {
    cy.visit(`/NotebookWorkflow?entryId=${entryId}`);
  }

  /**
   * Wait for the page to load.
   */
  waitForLoad() {
    cy.get(this.selectors.workflowContainer, { timeout: 10000 }).should(
      "be.visible",
    );
    cy.get(this.selectors.loading).should("not.exist");
  }

  /**
   * Get the page navigation sidebar.
   */
  getPageNavigation() {
    return cy.get(this.selectors.pageNavigation);
  }

  /**
   * Get all page items in the navigation.
   */
  getPageItems() {
    return cy.get(this.selectors.pageItem);
  }

  /**
   * Get a specific page item by index (0-based).
   * @param {number} index - The page index
   */
  getPageItemByIndex(index) {
    return cy.get(this.selectors.pageItem).eq(index);
  }

  /**
   * Click on a page item to navigate.
   * @param {number} index - The page index (0-based)
   */
  clickPage(index) {
    this.getPageItemByIndex(index).click();
  }

  /**
   * Get the currently active page item.
   */
  getActivePageItem() {
    return cy.get(this.selectors.activePageItem);
  }

  /**
   * Verify the active page index.
   * @param {number} expectedIndex - The expected active page index (0-based)
   */
  verifyActivePage(expectedIndex) {
    this.getPageItemByIndex(expectedIndex).should("have.class", "active");
  }

  /**
   * Get the page header title.
   */
  getPageTitle() {
    return cy.get(this.selectors.pageHeader).find("h3");
  }

  /**
   * Get all progress tiles.
   */
  getProgressTiles() {
    return cy.get(this.selectors.progressTile);
  }

  /**
   * Get a progress tile by label text.
   * @param {string} label - The label text (e.g., "Total Samples")
   */
  getProgressTileByLabel(label) {
    return cy.contains(this.selectors.progressTile, label);
  }

  /**
   * Get the action buttons bar.
   */
  getActionsBar() {
    return cy.get(this.selectors.pageActionsBar);
  }

  /**
   * Click the Bulk Apply button.
   */
  clickBulkApply() {
    cy.get(this.selectors.pageActionsBar)
      .contains("button", "Bulk Apply")
      .click();
  }

  /**
   * Click the Refresh button.
   */
  clickRefresh() {
    cy.get(this.selectors.pageActionsBar).contains("button", "Refresh").click();
  }

  /**
   * Click the Import Manifest button (Page 1).
   */
  clickImportManifest() {
    cy.get(this.selectors.pageActionsBar)
      .contains("button", "Import from Manifest")
      .click();
  }

  /**
   * Get the sample grid table.
   */
  getSampleGrid() {
    return cy.get(this.selectors.sampleGrid);
  }

  /**
   * Get all sample rows in the grid.
   */
  getSampleRows() {
    return cy.get(this.selectors.tableRow);
  }

  /**
   * Select a sample row by index.
   * @param {number} index - The row index (0-based)
   */
  selectSampleRow(index) {
    this.getSampleRows().eq(index).find('input[type="checkbox"]').check();
  }

  /**
   * Select all samples using the header checkbox.
   */
  selectAllSamples() {
    cy.get(this.selectors.selectAllCheckbox).check();
  }

  /**
   * Deselect all samples.
   */
  deselectAllSamples() {
    cy.get(this.selectors.selectAllCheckbox).uncheck();
  }

  /**
   * Get the status filter dropdown.
   */
  getStatusFilter() {
    return cy.get(this.selectors.statusFilterDropdown);
  }

  /**
   * Filter samples by status.
   * @param {string} status - The status to filter by (ALL, PENDING, IN_PROGRESS, COMPLETED)
   */
  filterByStatus(status) {
    this.getStatusFilter().click();
    cy.contains(status).click();
  }

  /**
   * Verify the empty state is displayed.
   */
  verifyEmptyState() {
    cy.get(this.selectors.emptyState).should("be.visible");
  }

  /**
   * Get the Bulk Apply modal.
   */
  getBulkApplyModal() {
    return cy.get(this.selectors.bulkApplyModal);
  }

  /**
   * Enter a value in a Bulk Apply form field.
   * Non-empty fields are automatically applied (no checkboxes needed).
   * @param {string} fieldId - The field ID
   * @param {string} value - The value to enter
   */
  enterBulkApplyValue(fieldId, value) {
    cy.get(`#${fieldId}`).clear().type(value);
  }

  /**
   * Get the fields grid in the Bulk Apply modal.
   */
  getBulkApplyFieldsGrid() {
    return cy.get(this.selectors.bulkApplyFieldsGrid);
  }

  /**
   * Submit the Bulk Apply modal.
   */
  submitBulkApply() {
    cy.get(this.selectors.modalPrimaryButton).click();
  }

  /**
   * Cancel the Bulk Apply modal.
   */
  cancelBulkApply() {
    cy.get(this.selectors.modalSecondaryButton).click();
  }

  /**
   * Verify inline notification is displayed.
   * @param {string} kind - The notification kind (error, success, info, warning)
   */
  verifyNotification(kind) {
    cy.get(this.selectors.inlineNotification).should(
      "have.class",
      `cds--inline-notification--${kind}`,
    );
  }

  /**
   * Get the navigation summary text.
   */
  getNavigationSummary() {
    return cy.get(this.selectors.navigationSummary);
  }
}

export default NotebookWorkflowPage;
