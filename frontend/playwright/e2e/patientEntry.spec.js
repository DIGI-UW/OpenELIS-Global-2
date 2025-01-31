import { test, expect } from "@playwright/test";
import LoginPage from "../pages/LoginPage";
import PatientEntryPage from "../pages/PatientEntryPage";
import patient from "../fixtures/patient.json";

let page;
let homePage = null;
let loginPage = null;
let patientPage = null;
let browser;
let context;

test.describe("Patient Search", () => {
  test.beforeAll(async ({ browser: browserInstance }) => {
    browser = browserInstance;
    context = await browser.newContext();
    page = await context.newPage();

    loginPage = new LoginPage(page);
    homePage = await loginPage.goToHomePage();
    // homePage = await loginPage.goToHomePage();
  });

  test("User Visits Home Page and goes to Add|Modify Patient Page", async () => {
    patientPage = await homePage.goToPatientEntry();
    const titleLocator = await patientPage.getPatientEntryPageTitle();
    await expect(titleLocator).toContainText("Add Or Modify Patient");
  });

  test("External search button should be deactivated", async () => {
    await patientPage.getExternalSearchButton();
  });

  test("User should be able to navigate to create Patient tab", async () => {
    await patientPage.clickNewPatientTab();
    const submitLocator = await patientPage.getSubmitButton();
    await expect(submitLocator).toBeVisible();
  });

  test("User should enter patient Information", async () => {
    await patientPage.enterPatientInfo(
      patient.firstName,
      patient.lastName,
      patient.subjectNumber,
      patient.nationalId,
      patient.DOB,
    );
  });

  test("User should click save new patient information button", async () => {
    await patientPage.clickSavePatientButton();
    await page.waitForTimeout(1000);
    await expect(page.locator("div[role='status']").first()).toBeVisible();
    await page.waitForTimeout(200);
    await page.reload();
  });

  test("Should be able to search patients By gender", async () => {
    await page.waitForTimeout(1000);
    await (await patientPage.getMaleGenderRadioButton()).click();
    await page.waitForTimeout(200);
    await patientPage.clickSearchPatientButton();
    await patientPage.validatePatientByGender("M");
    await page.waitForTimeout(200);
    await page.reload();
  });

  test("Should search Patient By FirstName only", async () => {
    await page.waitForTimeout(1000);
    await patientPage.searchPatientByFirstNameOnly(patient.firstName);
    const firstName = await patientPage.getFirstName();
    await expect(firstName).toHaveValue(patient.firstName);
    await patientPage.clickSearchPatientButton();
    await patientPage.validatePatientSearchTablebyRespectiveField(
      patient.firstName,
      "firstName",
    );
    await page.waitForTimeout(200);
    await page.reload();
  });

  test("Should search Patient By LastName only", async () => {
    await page.waitForTimeout(1000);
    await patientPage.searchPatientByLastNameOnly(patient.lastName);
    const lastName = await patientPage.getLastName();
    await expect(lastName).toHaveValue(patient.lastName);
    await patientPage.clickSearchPatientButton();
    await patientPage.validatePatientSearchTablebyRespectiveField(
      patient.lastName,
      "lastName",
    );
    await page.waitForTimeout(200);
    await page.reload();
  });

  test("Should search Patient By First and LastName", async () => {
    await page.waitForTimeout(1000);

    await patientPage.searchPatientByFirstAndLastName(
      patient.firstName,
      patient.lastName,
    );

    const firstName = await patientPage.getFirstName();
    const lastName = await patientPage.getLastName();

    // Check the stored values
    await expect(firstName).toHaveValue(patient.firstName);
    await expect(lastName).toHaveValue(patient.lastName);

    // Ensure that invalid name is not present
    await expect(lastName).not.toHaveValue(patient.inValidName);

    await patientPage.clickSearchPatientButton();
    await patientPage.validatePatientSearchTable(
      patient.firstName,
      patient.inValidName,
    );
    await page.waitForTimeout(200);
    await page.reload();
  });

  test("should search patient By Date Of Birth", async () => {
    await page.waitForTimeout(1000);

    await patientPage.searchPatientByDateOfBirth(patient.DOB);
    await patientPage.clickSearchPatientButton();
    await patientPage.validatePatientSearchTablebyRespectiveField(
      patient.DOB,
      "DOB",
    );
    await page.waitForTimeout(200);
    await page.reload();
  });

  test("should search patient By Lab Number", async () => {
    await patientPage.searchPatientBylabNo(patient.labNo);

    await page.route(
      `**/rest/patient-search-results?*labNumber=${patient.labNo}*`,
      (route) => route.continue(),
    );

    await patientPage.clickSearchPatientButton();

    const response = await page.waitForResponse(
      `**/rest/patient-search-results?*labNumber=${patient.labNo}*`,
    );

    const responseBody = await response.json();
    expect(responseBody.patientSearchResults).toBeInstanceOf(Array);
    expect(responseBody.patientSearchResults).toHaveLength(0);
    await page.waitForTimeout(200);
    await page.reload();
  });

  test("should search patient By PatientId", async () => {
    await page.waitForTimeout(1000);

    await patientPage.searchPatientByPatientId(patient.nationalId);
    await patientPage.clickSearchPatientButton();
    await patientPage.validatePatientSearchTable(
      patient.firstName,
      patient.inValidName,
    );
  });
});
