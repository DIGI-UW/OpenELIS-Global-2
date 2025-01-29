import { test, expect } from "@playwright/test";
import LoginPage from "../pages/LoginPage";
import users from "../fixtures/Users.json";

test.describe("Failing or Succeeding to Login", () => {
  let loginPage;

  test.beforeEach(async ({ page }) => {
    loginPage = new LoginPage(page);
    await loginPage.visit();
  });

  test.afterEach(async ({ page }) => {
    await page.context().clearCookies();
  });

  test("Attempts to login without providing username and password", async () => {
    await loginPage.signIn();
  });

  test("Attempts to login with only a username", async () => {
    let user = users[3];
    await loginPage.enterUsername(user.username);
    await loginPage.signIn();
  });

  test("Attempts to login with only a password", async () => {
    let user = users[3];
    await loginPage.enterPassword(user.password);
    await loginPage.signIn();
  });

  test("Should validate user authentication", async ({ page }) => {
    await page.waitForTimeout(500);

    for (const user of users.filter((user) => user.correctPass === "true")) {
      await loginPage.enterUsername(user.username);
      await loginPage.enterPassword(user.password);
      await loginPage.signIn();

      await expect(
        page.locator("header#mainHeader > button[title='Open menu']"),
      ).toBeVisible();
    }
  });
});
