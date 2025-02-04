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
let browser;

test.beforeAll(async ({ browser: browserInstance }) => {
  browser = browserInstance;
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
async function fillAccessionNumber(accessionNumber) {
  // Locate the elements for #display_accessionNumber and #accessionNumber
  const displayAccessionNumberLocator = page.locator(
    "#display_accessionNumber",
  );
  const accessionNumberLocator = page.locator("#accessionNumber");
  const textboxLocator = await page.getByRole("textbox", {
    name: "Enter Accession Number",
  });
  // Wait for either #display_accessionNumber or #accessionNumber to be visible
  const visibleLocator = await Promise.race([
    displayAccessionNumberLocator
      .waitFor({ state: "visible", timeout: 5000 })
      .then(() => displayAccessionNumberLocator),
    accessionNumberLocator
      .waitFor({ state: "visible", timeout: 5000 })
      .then(() => accessionNumberLocator),
    textboxLocator
      .waitFor({ state: "visible", timeout: 5000 })
      .then(() => textboxLocator),
  ]);

  // Fill the first visible locator with the accessionNumber
  if (visibleLocator) {
    await visibleLocator.fill(accessionNumber);
  } else {
    console.error("Neither field is visible within the timeout");
  }
}

async function fillLabNumber(labNumber) {
  // Wait for either #display_labNumber or #labNumber to be visible
  const displayLabNumberLocator = page.locator("#display_labNumber");
  const labNumberLocator = page.locator("#labNumber");

  // Wait for the visibility of either of the elements
  const visibleLocator = await Promise.race([
    displayLabNumberLocator
      .waitFor({ state: "visible", timeout: 5000 })
      .then(() => displayLabNumberLocator),
    labNumberLocator
      .waitFor({ state: "visible", timeout: 5000 })
      .then(() => labNumberLocator),
  ]);

  // Fill the visible element with labNumber
  if (visibleLocator) {
    await visibleLocator.fill(labNumber);
  } else {
    console.error("Neither field is visible within the timeout");
  }
}

test.describe("Result By Unit", () => {
  // fixing needed
  test("User visits Results Page", async () => {
    result = await homePage.goToResultsByUnit();
    await expect(await result.getResultTitle()).toContainText(res.pageTitle);
  });

  test("Should Search by Unit", async () => {
    await result.selectUnitType(workPlan.unitType);
  });
  // Unsure if the the checkbox is disabled intentionally or it's supposed to function that way
  // test("Should accept, refer, and save the result", async () => {
  //   await page.getByLabel("Select Test Unit").selectOption("117");
  //   await result.expandSampleDetails();
  //   await result.selectTestMethod(0, res.pcrTestMethod);

  //   // await result.enableAndCheckReferTestCheckbox();
  //   await page.evaluate(() => {
  //     document
  //       .querySelectorAll("input[type='checkbox']")
  //       .forEach((checkbox) => {
  //         checkbox.removeAttribute("disabled");
  //       });
  //   });

  //   await page
  //     .locator(".cds--checkbox-label")
  //     .filter({ hasText: "Refer test to a reference lab" })
  //     .check();
  //   await result.referSample(0, res.testNotPerformed, res.cedres);
  //   await result.setResultValue(0, res.positiveResult);
  //   await result.submitResults();
});

test.describe("Result By Patient", () => {
  test("User visits Results Page", async () => {
    result = await homePage.goToResultsByPatient();
    await expect(await result.getResultTitle()).toContainText(res.pageTitle);
  });
  // *BUG* - passes most of the times but fails sometimes
  // test("Should search Patient By Lab Number", async () => {
  //   await fillLabNumber(patient.labNo);
  //   await page.locator("#local_search").click();
  //   let res;

  //   if (await page.locator("#display_labNumber").isVisible()) {
  //     res = page.locator("#display_labNumber");
  //   } else if (await page.locator("#labNumber").isVisible()) {
  //     res = page.locator("#labNumber");
  //   }

  //   if (res) {
  //     await res.click(); // Focus on the input element
  //     await res.press("Control+A"); // Select all text
  //     await res.press("Backspace"); // Delete the selected text
  //   }
  // });

  test("Should search Patient By First and Last Name and validate", async () => {
    result = await homePage.goToResultsByPatient();
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
    result = await homePage.goToResultsByPatient();
    await patientPage.searchPatientByPatientId(patient.nationalId);
    await patientPage.clickSearchPatientButton();

    await patientPage.validatePatientSearchTable(
      patient.firstName,
      patient.inValidName,
    );
  });

  test("Should search patient and accept result", async () => {
    // result = await homePage.goToResultsByPatient();
    await patientPage.clickSearchPatientButton();
    await result.acceptResult();
    await result.expandSampleDetails();
    await result.selectTestMethod(0, res.stainTestMethod);
    await result.submitResults();
  });
});

test.describe("Result By Order", () => {
  test("User visits Results Page", async () => {
    result = await homePage.goToResultsByOrder();
    await expect(await result.getResultTitle()).toContainText(res.pageTitle);
  });
  // Needs fixing works most of them time but fails sometimes
  test("Should Search by Accession Number", async () => {
    result = await homePage.goToResultsByOrder();
    await fillAccessionNumber(workPlan.accessionNumber);

    await page.locator(":nth-child(4) > #submit").click();
  });

  test("Should accept and save result", async () => {
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
  test("User visits Referred Out Page", async () => {
    result = await homePage.goToResultsForRefferedOut();

    await expect(await result.getResultTitle()).toContainText(
      res.referralPageTitle,
    );
  });

  test("Should search Referrals By Patient", async () => {
    result = await homePage.goToResultsForRefferedOut();
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
    result = await homePage.goToResultsForRefferedOut();
    await page.locator("#testnames-input").fill(workPlan.testName);
    await page.locator("#testnames-item-0-item").click();
    await page
      .getByRole("button", { name: "Close" })
      .click({ waitForTimeout: 2000 });
    await page

      .getByRole("button", { name: "Search Referrals By Unit(s" })
      .click({
        Force: true,
      });
  });

  test("Should search Referrals By LabNumber", async () => {
    result = await homePage.goToResultsForRefferedOut();
    await page
      .getByRole("textbox", { name: "Scan OR Enter Manually" })
      .fill(workPlan.accessionNumber);
    await page
      .locator(":nth-child(4) > .cds--lg\\:col-span-4 > .cds--btn")
      .click();
  });
});

test.describe("Result By Range Of Order", () => {
  test("User visits Results Page", async () => {
    result = await homePage.goToResultsByRangeOrder();
    await expect(await result.getResultTitle()).toContainText(res.pageTitle);
  });

  test("Should Enter Lab Number and perform Search", async () => {
    result = await homePage.goToResultsByRangeOrder();
    await page
      .getByRole("textbox", { name: "From Accesion Number" })
      .fill(workPlan.accessionNumber);
    await page
      .getByRole("textbox", { name: "To Accesion Number" })
      .fill(workPlan.accessionNumber);
    await page.locator(":nth-child(5) > #submit").click();
  });
  test("Should Accept And Save the result", async () => {
    await page.locator("button#submit").first().click();

    await result.acceptSample();
    await result.expandSampleDetails();
    await result.selectTestMethod(0, res.eiaTestMethod);
    await result.submitResults();
  });
});

test.describe("Result By Test And Status", () => {
  test("User visits Results Page", async () => {
    result = await homePage.goToResultsByTestAndStatus();
    await expect(await result.getResultTitle()).toContainText(res.pageTitle);
  });

  test("Should select testName, analysis status, and perform Search", async () => {
    result = await homePage.goToResultsByTestAndStatus();
    await result.selectTestName(workPlan.testName);
    await result.selectAnalysisStatus(res.acceptedStatus);
    await result.searchByTest();
  });

  test("Should Validate And accept the result", async () => {
    await page.getByLabel("Select Test Name").selectOption("39");
    await page
      .getByRole("main")
      .getByRole("button", { name: "Search" })
      .click();

    await expect(
      await page.locator("#cell-testName-0 .sampleInfo"),
    ).toContainText(workPlan.testName);

    await result.acceptSample();
    await result.expandSampleDetails();
    await result.selectTestMethod(0, res.eiaTestMethod);
    await result.submitResults();
  });
});
