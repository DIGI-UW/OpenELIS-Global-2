import { test, expect } from "@playwright/test";
import LoginPage from "../pages/LoginPage";

let browser;
let context;
let page;
let loginPage;

test.beforeAll(async ({ browser: browserInstance }) => {
  // Create a new browser context and page
  browser = browserInstance;
  context = await browser.newContext();
  page = await context.newPage();

  // Initialize the LoginPage and perform login
  loginPage = new LoginPage(page);
  await loginPage.login();
});

test.afterAll(async () => {
  // Close the browser context after all tests
  await context.close();
});

test.afterEach(async () => {
  await loginPage.goToHomePage();
});

test.describe("User navigates to different tiles", () => {
  test("User navigates to the In Progress", async () => {
    await page.click("text=In Progress");

    await Promise.any([
      expect(page).toHaveURL("http://localhost/in-progress"),
      expect(page.getByRole("heading", { name: "In Progress" })).toBeVisible(),
    ]);
  });

  test("User navigates to Ready for Validation", async () => {
    await page.click("text=Ready for Validation");

    await Promise.any([
      expect(page).toHaveURL("http://localhost/ready-for-validation"),
      expect(
        page.getByRole("heading", { name: "Ready For Validation" }),
      ).toBeVisible(),
    ]);
  });

  test("User navigates to Orders Completed Today", async () => {
    await page.click("text=Orders Completed Today");

    await Promise.any([
      expect(page).toHaveURL("http://localhost/orders-completed-today"),
      expect(
        page.getByRole("heading", { name: "Orders Completed Today" }),
      ).toBeVisible(),
    ]);
  });

  test("User navigates to Partially Completed Today", async () => {
    await page.click("text=Partially Completed Today");

    await Promise.any([
      expect(page).toHaveURL("http://localhost/partially-completed-today"),
      expect(
        page.getByRole("heading", { name: "Partially Completed Today" }),
      ).toBeVisible(),
    ]);
  });

  test("User navigates to Orders Entered By Users", async () => {
    await page.click("text=Orders Entered By Users");

    await Promise.any([
      expect(page).toHaveURL("http://localhost/orders-entered-by-users"),
      expect(
        page.getByRole("heading", { name: "Orders Entered By Users" }),
      ).toBeVisible(),
    ]);
  });

  test("User navigates to Orders Rejected", async () => {
    await page.click("text=Orders Rejected");

    await Promise.any([
      expect(page).toHaveURL("http://localhost/orders-rejected"),
      expect(
        page.getByRole("heading", { name: "Orders Rejected" }),
      ).toBeVisible(),
    ]);
  });

  test("User navigates to UnPrinted Results", async () => {
    await page.click("text=UnPrinted Results");

    await Promise.any([
      expect(page).toHaveURL("http://localhost/unprinted-results"),
      expect(
        page.getByRole("heading", { name: "UnPrinted Results" }),
      ).toBeVisible(),
    ]);
  });

  test("User navigates to Electronic Orders", async () => {
    await page.click("text=Electronic OrdersElectronic");

    await Promise.any([
      expect(page).toHaveURL("http://localhost/electronic-orders"),
      expect(
        page.getByRole("heading", { name: "Electronic Orders" }),
      ).toBeVisible(),
    ]);
  });

  test("User navigates to Average Turn Around time", async () => {
    await page.click("text=Average Turn Around Time");

    await Promise.any([
      expect(page).toHaveURL("http://localhost/average-turn-around-time"),
      expect(
        page.getByRole("heading", { name: "Average Turn Around Time" }),
      ).toBeVisible(),
    ]);
  });

  test("User navigates to Delayed Turn Around", async () => {
    await page.click("text=Delayed Turn Around");

    await Promise.any([
      expect(page).toHaveURL("http://localhost/delayed-turn-around"),
      expect(
        page.getByRole("heading", { name: "Delayed Turn Around" }),
      ).toBeVisible(),
    ]);
  });
});
