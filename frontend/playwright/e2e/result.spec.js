import { test, expect } from "@playwright/test";
import PatientEntryPage from "../pages/PatientEntryPage";
import LoginPage from "../pages/LoginPage";
import workPlan from "../fixtures/workplan.json";
import res from "../fixtures/result.json";
import patient from "../fixtures/patient.json";

let context;
let page;
let homePage;
let result;
let patientPage;
let loginPage;

test.beforeAll(async ({ browser }) => {
  context = await browser.newContext();
  page = await context.newPage();

  loginPage = new LoginPage(page);

  homePage = await loginPage.goToHomePage();

  patientPage = new PatientEntryPage(page);
});

// Clean up after all tests
test.afterAll(async () => {
  await context.close();
});

test.describe("Result By Unit", () => {
  test.beforeEach(async () => {
    result = await homePage.goToResultsByUnit();
  });

    test("User visits Results Page", async () => {
      await expect(await result.getResultTitle()).toContainText(res.pageTitle);
    });

    test("Should Search by Unit", async () => {
      await result.selectUnitType(workPlan.unitType);
    });

    test("Should accept, refer, and save the result", async () => {
      await page.getByLabel("Select Test Unit").selectOption("117");
      await result.expandSampleDetails();
      await result.selectTestMethod(0, res.pcrTestMethod);

      // await result.enableAndCheckReferTestCheckbox();
      await page.evaluate(() => {
        document
          .querySelectorAll("input[type='checkbox']")
          .forEach((checkbox) => {
            checkbox.removeAttribute("disabled");
          });
      });

      // Now check the checkbox
      await page
        .locator(".cds--checkbox-label")
        .filter({ hasText: "Refer test to a reference lab" })
        .check();
      await result.referSample(0, res.testNotPerformed, res.cedres);
      await result.setResultValue(0, res.positiveResult);
      await result.submitResults();
    });
  

  test.describe("Result By Patient", () => {
    test.beforeEach(async () => {
      result = await homePage.goToResultsByPatient();
    });

    test("User visits Results Page", async () => {
      await expect(await result.getResultTitle()).toContainText(res.pageTitle);
    });

    test("Should search Patient By First and Last Name and validate", async () => {
      await patientPage.searchPatientByFirstAndLastName(
        patient.firstName,
        patient.lastName,
      );

      await expect(await patientPage.getFirstName()).toHaveValue(
        patient.firstName,
      );

      await expect(await patientPage.getLastName()).toHaveValue(patient.lastName);
      await expect(await patientPage.getLastName()).not.toHaveValue(
        patient.inValidName,
      );

      await patientPage.clickSearchPatientButton();
      await patientPage.validatePatientSearchTable(
        patient.firstName,
        patient.inValidName,
      );
    });

    test("Should search Patient By ID and validate", async () => {
      await patientPage.searchPatientByPatientId(patient.nationalId);
      await patientPage.clickSearchPatientButton();

      await patientPage.validatePatientSearchTable(
        patient.firstName,
        patient.inValidName,
      );
    });

    test("Should search Patient By Lab Number", async () => {
      await page.locator("#labNumber").fill(patient.labNo);
      await patientPage.clickSearchPatientButton();
    });

    test("Should search patient and accept result", async () => {
      await (await patientPage.getMaleGenderRadioButton()).click();

      await patientPage.clickSearchPatientButton();
      await result.acceptResult();
      await result.expandSampleDetails();
      await result.selectTestMethod(0, res.stainTestMethod);
      await result.submitResults();
    });
  });

  test.describe("Result By Order", () => {
    test.beforeEach(async () => {
      result = await homePage.goToResultsByOrder();
    });

    test("User visits Results Page", async () => {
      await expect(await result.getResultTitle()).toContainText(res.pageTitle);
    });

    test("Should Search by Accession Number", async () => {
      await page.locator("#accessionNumber").fill(workPlan.labNo);
      await page.locator(":nth-child(4) > #submit").click();
    });

    test("Should accept and save result", async () => {
      await page.locator("#accessionNumber").fill(workPlan.accessionNumber);
      await page.locator(":nth-child(4) > #submit").click();

      await page
        .getByRole("row", { name: "Expand Row Copy Lab Number" })
        .locator("label")
        .click();
      await result.expandSampleDetails();
      await result.selectTestMethod(0, res.stainTestMethod);
      await result.submitResults();
    });
  });

  test.describe("Result By Referred Out", () => {
    test.beforeEach(async () => {
      result = await homePage.goToResultsForRefferedOut();
    });

    test("User visits Referred Out Page", async () => {
      await expect(await result.getResultTitle()).toContainText(
        res.referralPageTitle,
      );
    });

    test("Should search Referrals By Patient", async () => {
      await patientPage.searchPatientByPatientId(patient.nationalId);
      await patientPage.searchPatientByFirstAndLastName(
        patient.firstName,
        patient.lastName,
      );
      await expect(await patientPage.getFirstName()).toHaveValue(
        patient.firstName,
      );
      await expect(await patientPage.getLastName()).toHaveValue(patient.lastName);
      await patientPage.clickSearchPatientButton();
      await patientPage.validatePatientSearchTable(
        patient.firstName,
        patient.inValidName,
      );
    });

    test("Should search Referrals By Test Unit", async () => {
      await page.locator("#testnames-input").fill(workPlan.testName);
      await page.locator("#testnames-item-0-item").click();
      await page.locator(":nth-child(15) > .cds--btn").click({ force: true });
    });

    test("Should search Referrals By LabNumber", async () => {
      await page.locator("#labNumberInput").type(workPlan.labNo);
      await page
        .locator(":nth-child(4) > .cds--lg\\:col-span-4 > .cds--btn")
        .click();
    });
  });

  test.describe("Result By Range Of Order", () => {
    test.beforeEach(async () => {
      result = await homePage.goToResultsByRangeOrder();
    });

    test("User visits Results Page", async () => {
      await expect(await result.getResultTitle()).toContainText(res.pageTitle);
    });

    test("Should Enter Lab Number and perform Search", async () => {
      await page.locator("#startLabNo").type(workPlan.labNo);
      await page.locator(":nth-child(5) > #submit").click();
    });

    test("Should Accept And Save the result", async () => {
     
      await page.locator("#startLabNo").fill(workPlan.accessionNumber);
    
      await page.locator("#endLabNo").fill(workPlan.accessionNumber);

      await page.locator("button#submit").first().click();

      await result.acceptSample();
      await result.expandSampleDetails();
      await result.selectTestMethod(0, res.eiaTestMethod);
      await result.submitResults();
    });
  });

  test.describe("Result By Test And Status", () => {
    test.beforeEach(async ({ page }) => {
      result = await homePage.goToResultsByTestAndStatus();
    });

    test("User visits Results Page", async () => {
      await expect(await result.getResultTitle()).toContainText(res.pageTitle);
    });

    test("Should select testName, analysis status, and perform Search", async () => {
      await result.selectTestName(workPlan.testName);
      await result.selectAnalysisStatus(res.acceptedStatus);
      await result.searchByTest();
    });

    test("Should Validate And accept the result", async () => {
      await page.getByLabel('Select Test Name').selectOption('39');
      await page.getByRole('main').getByRole('button', { name: 'Search' }).click();

      await expect(
        await page.locator("#cell-testName-0 .sampleInfo"),
      ).toContainText(workPlan.testName);

      await result.acceptSample();
      await result.expandSampleDetails();
      await result.selectTestMethod(0, res.eiaTestMethod);
      await result.submitResults();
    });
  });
});
