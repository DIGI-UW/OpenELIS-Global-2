import { test, expect } from "@playwright/test";
import LoginPage from "../pages/LoginPage";

let browser;
let homePage;
let context;
let page;
let loginPage;

test.beforeAll(async ({ browser: browserInstance }) => {
  browser = browserInstance;
  context = await browser.newContext();
  page = await context.newPage();

  loginPage = new LoginPage(page);
  homePage = await loginPage.goToHomePage();
});

test.afterAll(async () => {
  await context.close();
});

test.afterEach(async () => {
  await homePage.afterAll();
});

test.describe("User navigates to different tiles", () => {
  test("User navigates to the In Progress", async () => {
    await homePage.selectInProgress();
  });

  test("User navigates to Ready for Validation", async () => {
    await homePage.selectReadyforValidation();
  });

  test("User navigates to Orders Completed Today", async () => {
    await homePage.selectOrdersCompletedToday();
  });

  test("User navigates to Partially Completed Today", async () => {
    await homePage.selectPartiallyCompletedToday();
  });

  test("User navigates to Orders Entered By Users", async () => {
    await homePage.selectOrdersEnteredByUsers();
  });

  test("User navigates to Orders Rejected", async () => {
    await homePage.selectOrdersRejected();
  });

  test("User navigates to UnPrinted Results", async () => {
    await homePage.selectUnPrintedResults();
  });

  test("User navigates to Electronic Orders", async () => {
    await homePage.selectElectronicOrders();
  });

  test("User navigates to Average Turn Around time", async () => {
    await homePage.selectAverageTurnAroundTime();
  });

  test("User navigates to Delayed Turn Around", async () => {
    await homePage.selectDelayedTurnAround();
  });
});
