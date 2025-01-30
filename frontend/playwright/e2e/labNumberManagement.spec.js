import LoginPage from "../pages/LoginPage";
import { test, expect } from "@playwright/test";
import LabNumberManagementData from "../fixtures/LabNumberManagement.json";

let page;
let context;
let loginPage = null;
let homePage = null;
let adminPage = null;
let labNumMgtPage = null;
let browser;
let labNMData = null;

test.describe("Lab Number Management", () => {
  test.beforeAll(async ({ browser: browserInstance }) => {
    browser = browserInstance;
    context = await browser.newContext();
    page = await context.newPage();

    loginPage = new LoginPage(page);
    loginPage.visit();

    homePage = await loginPage.goToHomePage();
    adminPage = await homePage.goToAdminPage();
  });

  test.beforeEach(async ({ page }) => {
    // Ensure user is on the correct page before each test
    labNMData = LabNumberManagementData;
  });

  test("User navigates to the Lab Number Management page", async ({ page }) => {
    labNumMgtPage = await adminPage.goToLabNumberManagementPage();
  });

  test("User selects legacy lab number type and submits", async ({ page }) => {
    await labNumMgtPage.selectLabNumber(
      LabNumberManagementData.legacyLabNumberType,
    );
    await labNumMgtPage.clickSubmitButton();
  });

  test("User selects alphanumeric lab number type and submits", async ({
    page,
  }) => {
    +(await labNumMgtPage.selectLabNumber(
      LabNumberManagementData.alphaLabNumberType,
    ));
    await labNumMgtPage.checkPrefixCheckBox();
    await labNumMgtPage.typePrefix(LabNumberManagementData.userPrefix);
    await labNumMgtPage.clickSubmitButton();
  });
});
