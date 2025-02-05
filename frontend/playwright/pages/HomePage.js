import { expect } from "@playwright/test";
import LoginPage from "./LoginPage";
import AdminPage from "./AdminPage";
import StudyReportPage from "./StudyReportPage";
import RoutineReportPage from "./RoutineReportPage";
// import Result 3 "../../../../Files-IMP/ResultsPage";
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

  // Will fix this ( main cause of flaky tests (Note for self))
  async openNavigationMenu() {
    const menuButtonSelector1 = "header#mainHeader > button[title='Open menu']";
    const menuButtonSelector2 = "button:has-text('Open menu')";

    // Wait for the first visible button among the selectors
    const firstAvailableSelector = await Promise.any([
      this.page
        .waitForSelector(menuButtonSelector1, {
          state: "visible",
          timeout: 20000,
        })
        .then(() => menuButtonSelector1), // Return the first selector if it's visible
      this.page
        .waitForSelector(menuButtonSelector2, {
          state: "visible",
          timeout: 20000,
        })
        .then(() => menuButtonSelector2), // Return the second selector if it's visible
    ]);
    // Now that we have the first available selector, click it
    await this.page.locator(firstAvailableSelector).click({
      timeout: 20000,
    });
  }

  async goToResultsByUnit() {
    await this.openNavigationMenu();
    const firstAvailable = await Promise.any([
      this.page
        .waitForSelector("#menu_results", { state: "visible", timeout: 5000 })
        .then(() => "#menu_results"),
      this.page
        .waitForSelector('button:has-text("Results")', {
          state: "visible",
          timeout: 5000,
        })
        .then(() => 'button:has-text("Results")'),
    ]);

    await this.page.click(firstAvailable);

    await this.page.locator("#menu_results_logbook").click();
    return new Result(this.page);
  }

  async goToResultsByOrder() {
    this.openNavigationMenu();
    const firstAvailable = await Promise.any([
      this.page
        .waitForSelector("#menu_results", { state: "visible", timeout: 3000 })
        .then(() => "#menu_results"),
      this.page
        .waitForSelector('button:has-text("Results")', {
          state: "visible",
          timeout: 3000,
        })
        .then(() => 'button:has-text("Results")'),
    ]);

    await this.page.click(firstAvailable);
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
    const firstAvailable = await Promise.any([
      this.page
        .waitForSelector("#menu_results", { state: "visible", timeout: 3000 })
        .then(() => "#menu_results"),
      this.page
        .waitForSelector('button:has-text("Results")', {
          state: "visible",
          timeout: 3000,
        })
        .then(() => 'button:has-text("Results")'),
    ]);

    await this.page.click(firstAvailable);
    await this.page.locator("#menu_results_patient").click();
    return new Result(this.page);
  }

  async goToResultsForRefferedOut() {
    this.openNavigationMenu();
    const firstAvailable = await Promise.any([
      this.page
        .waitForSelector("#menu_results", { state: "visible", timeout: 3000 })
        .then(() => "#menu_results"),
      this.page
        .waitForSelector('button:has-text("Results")', {
          state: "visible",
          timeout: 3000,
        })
        .then(() => 'button:has-text("Results")'),
    ]);

    await this.page.click(firstAvailable);
    await this.page.locator("#menu_results_referred").click();
    return new Result(this.page);
  }

  async goToResultsByRangeOrder() {
    this.openNavigationMenu();
    const firstAvailable = await Promise.any([
      this.page
        .waitForSelector("#menu_results", { state: "visible", timeout: 3000 })
        .then(() => "#menu_results"),
      this.page
        .waitForSelector('button:has-text("Results")', {
          state: "visible",
          timeout: 3000,
        })
        .then(() => 'button:has-text("Results")'),
    ]);

    await this.page.click(firstAvailable);
    await this.page.locator("#menu_results_range").click();
    return new Result(this.page);
  }
  async goToResultsByTestAndStatus() {
    this.openNavigationMenu();
    const firstAvailable = await Promise.any([
      this.page
        .waitForSelector("#menu_results", { state: "visible", timeout: 3000 })
        .then(() => "#menu_results"),
      this.page
        .waitForSelector('button:has-text("Results")', {
          state: "visible",
          timeout: 3000,
        })
        .then(() => 'button:has-text("Results")'),
    ]);

    await this.page.click(firstAvailable);
    await this.page.locator("#menu_results_status").click();
    return new Result(this.page);
  }
  async goToAdminPage() {
    await this.openNavigationMenu(); // Open the navigation menu
    await this.page.locator("#menu_administration").click(); // Click on the administration menu
    return new AdminPage(this.page);
  }
  async goToRoutineReports() {
    await this.openNavigationMenu();
    await this.page.locator("#menu_reports").click();
    await this.page.locator("#menu_reports_routine_nav").click();
    return new RoutineReportPage(this.page);
  }

  async goToStudyReports() {
    this.openNavigationMenu();
    await this.page.locator("#menu_reports").click();
    await this.page.locator("#menu_reports_study_nav").click();
    return new StudyReportPage(this.page);
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
