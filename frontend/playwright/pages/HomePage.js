import { expect } from "@playwright/test";
import LoginPage from "./LoginPage";
import AdminPage from "./AdminPage";
import Result from "./ResultsPage";
// import WorkPlan from "../../cypress/pages/WorkPlan";
import PatientEntryPage from "./PatientEntryPage";
class HomePage {
  constructor(page) {
    this.page = page;
  }

  async visit() {
    await this.page.goto("/"); // Navigate to home page
  }

  goToSign() {
    return new LoginPage(this.page); // Navigate to login page
  }

  async openNavigationMenu() {
    // Wait for the button to be visible and enabled
    const menuButton = this.page.locator(
      "header#mainHeader > button[title='Open menu']",
    );

    await menuButton.waitFor({
      state: "visible",
      timeout: 20000,
    });

    await menuButton.click({
      timeout: 20000,
    });
  }

  async goToResultsByUnit() {
    await this.openNavigationMenu();
    await this.page.locator("#menu_results").click();
    await this.page.locator("#menu_results_logbook").click();
    return new Result(this.page);
  }

  async goToResultsByOrder() {
    this.openNavigationMenu();
    await this.page.locator("#menu_results").click();
    await this.page.locator("#menu_results_accession").click();
    return new Result(this.page);
  }

  async goToPatientEntry() {
    await this.openNavigationMenu();
    await this.page.locator("#menu_patient_dropdown").click();
    await this.page.locator("#menu_patient_add_or_edit_nav").click();
    return new PatientEntryPage(this.page);
  }
  // async goToWorkPlanPlanByTest() {
  //   await this.openNavigationMenu();
  //   await this.page.locator("#menu_workplan_dropdown").click();
  //   await this.page.locator("#menu_workplan_test_nav").click();

  //   return new WorkPlan(this.page);
  // }
  async goToResultsByPatient() {
    await this.openNavigationMenu();
    await this.page.locator("#menu_results").click();
    await this.page.locator("#menu_results_patient").click();
    return new Result(this.page);
  }

  async goToResultsForRefferedOut() {
    this.openNavigationMenu();
    await this.page.locator("#menu_results").click();
    await this.page.locator("#menu_results_referred").click();
    return new Result(this.page);
  }

  async goToResultsByRangeOrder() {
    this.openNavigationMenu();
    await this.page.locator("#menu_results").click();
    await this.page.locator("#menu_results_range").click();
    return new Result(this.page);
  }
  async goToResultsByTestAndStatus() {
    this.openNavigationMenu();
    await this.page.locator("#menu_results").click();
    await this.page.locator("#menu_results_status").click();
    return new Result(this.page);
  }
  async goToAdminPage() {
    await this.openNavigationMenu(); // Open the navigation menu
    await this.page.locator("#menu_administration").click(); // Click on the administration menu
    return new AdminPage(this.page);
  }

  async afterAll() {
    await this.page.locator(".clickable-icon").click();
  }

  async selectInProgress() {
    await this.page.getByText("In Progress").click();

    await Promise.any([
      expect(this.page).toHaveURL("http://localhost/in-progress"),
      expect(
        this.page.getByRole("heading", { name: "In Progress" }),
      ).toBeVisible(),
    ]);
  }

  async selectReadyforValidation() {
    await this.page.getByText("Ready For ValidationAwaiting").click();

    await Promise.any([
      expect(this.page).toHaveURL("http://localhost/ready-for-validation"),
      expect(
        this.page.getByRole("heading", { name: "Ready For Validation" }),
      ).toBeVisible(),
    ]);
  }

  async selectOrdersCompletedToday() {
    await this.page
      .getByText(/Orders Completed Today/i) //Regex to match the text "Orders Completed Today" (case-insensitive)
      .nth(0) // Selects the first matching element
      .click();

    await Promise.any([
      expect(this.page).toHaveURL("http://localhost/orders-completed-today"),
      expect(
        this.page.getByRole("heading", { name: "Orders Completed Today" }),
      ).toBeVisible(),
    ]);
  }

  async selectPartiallyCompletedToday() {
    await this.page
      .getByText("Partially Completed TodayTotal Orders Completed Today0")
      .click();

    await Promise.any([
      expect(this.page).toHaveURL("http://localhost/partially-completed-today"),
      expect(
        this.page.getByRole("heading", { name: "Partially Completed Today" }),
      ).toBeVisible(),
    ]);
  }

  async selectOrdersEnteredByUsers() {
    await this.page
      .getByText("Orders Entered By UsersEntered by users Today0")
      .click();

    await Promise.any([
      expect(this.page).toHaveURL("http://localhost/orders-entered-by-users"),
      expect(
        this.page.getByRole("heading", { name: "Orders Entered By Users" }),
      ).toBeVisible(),
    ]);
  }

  async selectOrdersRejected() {
    await this.page.getByText("Orders RejectedRejected By").click();

    await Promise.any([
      expect(this.page).toHaveURL("http://localhost/orders-rejected"),
      expect(
        this.page.getByRole("heading", { name: "Orders Rejected" }),
      ).toBeVisible(),
    ]);
  }

  async selectUnPrintedResults() {
    await this.page.getByText("UnPrinted ResultsUnPrinted").click();

    await Promise.any([
      expect(this.page).toHaveURL("http://localhost/unprinted-results"),
      expect(
        this.page.getByRole("heading", { name: "UnPrinted Results" }),
      ).toBeVisible(),
    ]);
  }

  async selectElectronicOrders() {
    await this.page.getByText("Electronic OrdersElectronic").click();

    await Promise.any([
      expect(this.page).toHaveURL("http://localhost/electronic-orders"),
      expect(
        this.page.getByRole("heading", { name: "Electronic Orders" }),
      ).toBeVisible(),
    ]);
  }

  async selectAverageTurnAroundTime() {
    await this.page
      .getByText("Average Turn Around timeReception to Validation0")
      .click();

    await Promise.any([
      expect(this.page).toHaveURL("http://localhost/average-turn-around-time"),
      expect(
        this.page.getByRole("heading", { name: "Average Turn Around Time" }),
      ).toBeVisible(),
    ]);
  }

  async selectDelayedTurnAround() {
    await this.page.getByText("Delayed Turn AroundMore Than").click();

    await Promise.any([
      expect(this.page).toHaveURL("http://localhost/delayed-turn-around"),
      expect(
        this.page.getByRole("heading", { name: "Delayed Turn Around" }),
      ).toBeVisible(),
    ]);
  }
}

export default HomePage;
