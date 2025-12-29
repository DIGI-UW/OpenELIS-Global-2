/**
 * NotebookDashboardPage - Page object for the notebook dashboard with hierarchy support.
 * Handles navigation and interactions with the notebook template/instance hierarchy.
 */
class NotebookDashboardPage {
  constructor() {
    this.selectors = {
      // Tree View
      treeView: ".notebook-tree-view",
      treeNode: ".cds--tree-node",
      treeNodeLabel: ".cds--tree-node__label",
      parentNode: '[data-testid^="tree-node-parent-"]',
      childNode: '[data-testid^="tree-node-child-"]',
      treeNodeIcon: ".tree-node-icon",
      treeNodeTitle: ".tree-node-title",
      treeNodeCount: ".tree-node-count",

      // Parent Template Banner
      parentTemplateBanner: ".parent-template-banner",

      // Breadcrumb for child instances
      breadcrumb: ".cds--breadcrumb",
      breadcrumbItem: ".cds--breadcrumb-item",

      // Create Instance Modal
      createInstanceModal: ".cds--modal",
      createInstanceTitleInput: "#instance-title",
      modalPrimaryButton: ".cds--modal-footer .cds--btn--primary",
      modalSecondaryButton: ".cds--modal-footer .cds--btn--secondary",

      // Buttons
      createInstanceButton: 'button:contains("Create Instance")',
      newEntryButton: 'button:contains("New Entry")',
      newLabNotebookButton: 'button:contains("New Lab Notebook")',
      allEntriesButton: 'button:contains("All Entries")',

      // Dashboard tiles
      dashboardTile: ".dashboard-tile",
      notebookTile: ".notebook-dashboard-tile",

      // Selected notebook header
      notebookTitle: ".orderLegendBody h4",

      // Loading
      loading: ".cds--loading",

      // DataTable (legacy list)
      dataTable: ".cds--data-table",
      tableRow: ".cds--data-table tbody tr",
    };
  }

  /**
   * Visit the notebook dashboard page.
   */
  visit() {
    cy.visit("/NoteBookDashBoard");
  }

  /**
   * Wait for the dashboard to load.
   */
  waitForLoad() {
    cy.get(this.selectors.loading, { timeout: 10000 }).should("not.exist");
    cy.get(this.selectors.treeView, { timeout: 10000 }).should("be.visible");
  }

  /**
   * Get the tree view component.
   */
  getTreeView() {
    return cy.get(this.selectors.treeView);
  }

  /**
   * Get all tree nodes.
   */
  getTreeNodes() {
    return cy.get(this.selectors.treeNode);
  }

  /**
   * Get a parent template node by title.
   * @param {string} title - The template title
   */
  getParentNodeByTitle(title) {
    return cy
      .get(this.selectors.treeView)
      .contains(title)
      .closest(this.selectors.treeNode);
  }

  /**
   * Click on a parent template node to select it.
   * @param {string} title - The template title
   */
  selectParentTemplate(title) {
    this.getParentNodeByTitle(title).click();
  }

  /**
   * Expand a parent template node to show children.
   * @param {string} title - The template title
   */
  expandParentNode(title) {
    cy.get(this.selectors.treeView)
      .contains(title)
      .closest(this.selectors.treeNode)
      .find('[aria-expanded="false"]')
      .click();
  }

  /**
   * Get child nodes under a parent.
   * @param {string} parentTitle - The parent template title
   */
  getChildNodesUnderParent(parentTitle) {
    return cy
      .get(this.selectors.treeView)
      .contains(parentTitle)
      .closest(this.selectors.treeNode)
      .find(this.selectors.treeNode);
  }

  /**
   * Select a child instance node.
   * @param {string} childTitle - The child instance title
   */
  selectChildInstance(childTitle) {
    cy.get(this.selectors.treeView).contains(childTitle).click();
  }

  /**
   * Verify the parent template banner is displayed.
   */
  verifyParentTemplateBanner() {
    cy.get(this.selectors.parentTemplateBanner).should("be.visible");
  }

  /**
   * Verify the parent template banner is NOT displayed.
   */
  verifyNoParentTemplateBanner() {
    cy.get(this.selectors.parentTemplateBanner).should("not.exist");
  }

  /**
   * Get the Create Instance button.
   */
  getCreateInstanceButton() {
    return cy.contains("button", "Create Instance");
  }

  /**
   * Click the Create Instance button.
   */
  clickCreateInstance() {
    this.getCreateInstanceButton().click();
  }

  /**
   * Verify the Create Instance button is visible.
   */
  verifyCreateInstanceButtonVisible() {
    this.getCreateInstanceButton().should("be.visible");
  }

  /**
   * Get the New Entry button.
   */
  getNewEntryButton() {
    return cy.contains("button", "New Entry");
  }

  /**
   * Verify the New Entry button is visible.
   */
  verifyNewEntryButtonVisible() {
    this.getNewEntryButton().should("be.visible");
  }

  /**
   * Verify the New Entry button is NOT visible.
   */
  verifyNewEntryButtonNotVisible() {
    this.getNewEntryButton().should("not.exist");
  }

  /**
   * Get the Create Instance modal.
   */
  getCreateInstanceModal() {
    return cy.get(this.selectors.createInstanceModal);
  }

  /**
   * Enter a title in the Create Instance modal.
   * @param {string} title - The instance title
   */
  enterInstanceTitle(title) {
    cy.get(this.selectors.createInstanceTitleInput).clear().type(title);
  }

  /**
   * Submit the Create Instance modal.
   */
  submitCreateInstance() {
    cy.get(this.selectors.modalPrimaryButton).click();
  }

  /**
   * Cancel the Create Instance modal.
   */
  cancelCreateInstance() {
    cy.get(this.selectors.modalSecondaryButton).click();
  }

  /**
   * Create a new instance with the given title.
   * @param {string} title - The instance title
   */
  createInstance(title) {
    this.clickCreateInstance();
    this.getCreateInstanceModal().should("be.visible");
    this.enterInstanceTitle(title);
    this.submitCreateInstance();
  }

  /**
   * Get the breadcrumb navigation.
   */
  getBreadcrumb() {
    return cy.get(this.selectors.breadcrumb);
  }

  /**
   * Verify breadcrumb shows parent link.
   * @param {string} parentTitle - The expected parent title
   */
  verifyBreadcrumbParent(parentTitle) {
    cy.get(this.selectors.breadcrumb)
      .contains(parentTitle)
      .should("be.visible");
  }

  /**
   * Click on the parent link in the breadcrumb.
   * @param {string} parentTitle - The parent title to click
   */
  clickBreadcrumbParent(parentTitle) {
    cy.get(this.selectors.breadcrumb).contains(parentTitle).click();
  }

  /**
   * Get the notebook title in the main content area.
   */
  getNotebookTitle() {
    return cy.get(this.selectors.notebookTitle);
  }

  /**
   * Verify the selected notebook title.
   * @param {string} expectedTitle - The expected notebook title
   */
  verifySelectedNotebookTitle(expectedTitle) {
    this.getNotebookTitle().should("contain.text", expectedTitle);
  }

  /**
   * Click the All Entries button to reset filters.
   */
  clickAllEntries() {
    cy.contains("button", "All Entries").click();
  }

  /**
   * Get entry count badge on a tree node.
   * @param {string} title - The notebook title
   */
  getEntryCount(title) {
    return cy
      .get(this.selectors.treeView)
      .contains(title)
      .closest(this.selectors.treeNode)
      .find(this.selectors.treeNodeCount);
  }

  /**
   * Verify a node shows the expected entry count.
   * @param {string} title - The notebook title
   * @param {string} expectedCount - The expected count (as string)
   */
  verifyEntryCount(title, expectedCount) {
    this.getEntryCount(title).should("contain.text", expectedCount);
  }

  /**
   * Get the legacy DataTable.
   */
  getLegacyTable() {
    return cy.get(this.selectors.dataTable);
  }

  /**
   * Click on a row in the legacy table.
   * @param {number} index - The row index (0-based)
   */
  clickLegacyTableRow(index) {
    cy.get(this.selectors.tableRow).eq(index).click();
  }
}

export default NotebookDashboardPage;
