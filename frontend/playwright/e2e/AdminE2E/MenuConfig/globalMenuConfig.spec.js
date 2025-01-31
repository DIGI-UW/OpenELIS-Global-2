import { test, expect } from "@playwright/test";
import LoginPage from "../../../pages/LoginPage";

let browser;
let context;
let page;
let loginPage;
let homePage;
let adminPage;
let globalMenuConfigPage;

test.beforeAll(async ({ browser: browserInstance }) => {
  browser = browserInstance;
  context = await browser.newContext();
  page = await context.newPage();

  // Initialize LoginPage object and navigate to Admin Page
  loginPage = new LoginPage(page);
  await loginPage.visit();

  homePage = await loginPage.goToHomePage();
  adminPage = await homePage.goToAdminPage();
});

test.describe("Global Menu Configuration", () => {
  test("User navigates to the Global Menu Configuration page", async () => {
    globalMenuConfigPage = await adminPage.goToGlobalMenuConfigPage();
  });

  test("User turns off the toggle switch and submits", async () => {
    await globalMenuConfigPage.turnOffToggleSwitch();
    await globalMenuConfigPage.submitButton();
  });

  test("User turns on the toggle switch", async () => {
    await globalMenuConfigPage.turnOnToggleSwitch();
  });

  test("User checks the menu items and submits", async () => {
    await globalMenuConfigPage.checkMenuItem("home");
    await globalMenuConfigPage.checkMenuItem("order");
    await globalMenuConfigPage.checkMenuItem("billing");
    await globalMenuConfigPage.checkMenuItem("immunoChem");
    await globalMenuConfigPage.checkMenuItem("cytology");
    await globalMenuConfigPage.checkMenuItem("results");
    await globalMenuConfigPage.checkMenuItem("validation");
    await globalMenuConfigPage.checkMenuItem("patient");
    await globalMenuConfigPage.checkMenuItem("pathology");
    await globalMenuConfigPage.checkMenuItem("workplan");
    await globalMenuConfigPage.checkMenuItem("nonConform");
    await globalMenuConfigPage.checkMenuItem("reports");
    await globalMenuConfigPage.checkMenuItem("study");
    await globalMenuConfigPage.checkMenuItem("admin");
    await globalMenuConfigPage.checkMenuItem("help");
    await globalMenuConfigPage.submitButton();
  });

  test("User relogs in to verify the menu changes", async ({ browser }) => {
    // Launch a new browser context
    const context = await browser.newContext();
    const page = await context.newPage();

    // Login fresh
    const loginPage = new LoginPage(page);
    await loginPage.visit();
    const homePage = await loginPage.goToHomePage();
    const globalMenuConfigPage = await homePage.openNavigationMenu();

    // Close the context to simulate logout
    await context.close();
  });
});
