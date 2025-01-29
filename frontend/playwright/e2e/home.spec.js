import { test, expect } from "@playwright/test";
import LoginPage from "../pages/LoginPage";

let browser;
let home;
let context;
let page;
let loginPage;

test.beforeAll(async ({ browser: browserInstance }) => {
  browser = browserInstance;
  context = await browser.newContext();
  page = await context.newPage();

  loginPage = new LoginPage(page);
  home = await loginPage.goToHomePage();
});

test.afterAll(async () => {
  await context.close();
});

test.afterEach(async () => {
  await home.afterAll();
});

test.describe("User navigates to different tiles", () => {
  test("User navigates to the In Progress", async () => {
    await home.selectInProgress();
  });

  test("User navigates to Ready for Validation", async () => {
    await home.selectReadyforValidation();
  });

  test("User navigates to Orders Completed Today", async () => {
    await home.selectOrdersCompletedToday();
  });

  test("User navigates to Partially Completed Today", async () => {
    await home.selectPartiallyCompletedToday();
  });

  test("User navigates to Orders Entered By Users", async () => {
    await home.selectOrdersEnteredByUsers();
  });

  test("User navigates to Orders Rejected", async () => {
    await home.selectOrdersRejected();
  });

  test("User navigates to UnPrinted Results", async () => {
    await home.selectUnPrintedResults();
  });

  test("User navigates to Electronic Orders", async () => {
    await home.selectElectronicOrders();
  });

  test("User navigates to Average Turn Around time", async () => {
    await home.selectAverageTurnAroundTime();
  });

  test("User navigates to Delayed Turn Around", async () => {
    await home.selectDelayedTurnAround();
  });
});
