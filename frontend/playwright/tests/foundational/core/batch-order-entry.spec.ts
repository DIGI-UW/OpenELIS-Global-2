import { test as base, expect, type Page } from "../../../helpers/test-base";
import {
  LONG_TIMEOUT,
  NAV_TIMEOUT,
  UI_TIMEOUT,
} from "../../../helpers/timeouts";
import { apiPost } from "../../../helpers/admin-ui";
import { csrfToken } from "../../../helpers/api-session";
import {
  letters,
  seedPatient,
  type SeededPatient,
} from "../../../helpers/seed-patient-order";

/**
 * Batch Order Entry (/SampleBatchEntrySetup): the setup screen's gating, then
 * one order saved per barcode in On Demand and Pre-Printed modes, for the
 * routine form and the EID study form. Each saved order is read back on Modify
 * Order.
 */

const API = "/api/OpenELIS-Global/rest";
const SERUM_ID = "2";

// Test-scoped ownership keeps teardown running even when a browser assertion
// times out. The fixture depends on page so the authenticated context remains
// alive until every catalog entry and clinic it created has been deactivated.
const test = base.extend<{
  ownedRecords: { testIds: string[]; organizationIds: string[] };
}>({
  ownedRecords: async ({ page }, use) => {
    const records = {
      testIds: [] as string[],
      organizationIds: [] as string[],
    };
    try {
      await use(records);
    } finally {
      const headers = { "X-CSRF-Token": await csrfToken(page) };
      await Promise.all([
        ...records.testIds.map(async (id) => {
          const response = await page.request.put(
            `${API}/test-catalog/tests/${id}/basic-info`,
            {
              headers,
              data: { active: false },
            },
          );
          expect(response.ok(), await response.text()).toBeTruthy();
        }),
        ...(records.organizationIds.length
          ? [
              apiPost(page, "/rest/locations/organizations/active", {
                ids: records.organizationIds,
                active: false,
                includeChildren: false,
              }),
            ]
          : []),
      ]);
    }
  },
});

async function openSetup(page: Page) {
  await page.goto("/SampleBatchEntrySetup", { waitUntil: "domcontentloaded" });
  await expect(
    page.getByRole("heading", { name: "Batch Order Entry Setup" }),
  ).toBeVisible({ timeout: NAV_TIMEOUT });
}

const main = (page: Page) => page.getByRole("main");

function checkbox(page: Page, label: string) {
  return main(page).getByRole("checkbox", { name: label, exact: true });
}

/** Clicks the checkbox's own label; Carbon hides the input. */
async function toggle(page: Page, label: string) {
  await checkbox(page, label).locator("xpath=..").locator("label").click();
}

async function tick(page: Page, label: string) {
  await toggle(page, label);
  await expect(checkbox(page, label)).toBeChecked();
}

async function chooseRoutineSerum(
  page: Page,
  panels: string[],
  tests: string[],
) {
  await page.getByRole("combobox", { name: "Form:*" }).selectOption("routine");
  await page
    .getByRole("combobox", { name: "Sample Type" })
    .selectOption({ label: "Serum" });
  for (const label of [...panels, ...tests]) await tick(page, label);
}

async function toEntryPage(page: Page) {
  const next = main(page).getByRole("button", { name: "Next", exact: true });
  await expect(next).toBeEnabled();
  await next.click();
  await expect(
    page.getByRole("heading", { name: "Batch Order Entry", exact: true }),
  ).toBeVisible({ timeout: UI_TIMEOUT });
}

/** Names of the serum tests the given panels and tests order. */
async function orderedTestNames(
  page: Page,
  panels: string[],
  tests: string[],
): Promise<string[]> {
  const catalog: {
    panels: Array<{ name: string; testIds: string }>;
    tests: Array<{ id: string; name: string }>;
  } = await (
    await page.request.get(`${API}/sample-type-tests?sampleType=${SERUM_ID}`)
  ).json();
  const ids = new Set(
    catalog.panels
      .filter((panel) => panels.includes(panel.name))
      .flatMap((panel) => panel.testIds.split(",")),
  );
  const names = catalog.tests
    .filter((t) => ids.has(t.id) || tests.includes(t.name))
    .map((t) => t.name);
  expect(names.length, "panels and tests are in the serum catalog").toBe(
    ids.size + tests.length,
  );
  return names;
}

/** The saved order, opened on Modify Order, lists the patient and tests. */
async function expectSavedOrder(
  page: Page,
  labNumber: string,
  patient: { nationalId: string; lastName: string; firstName: string },
  testNames: string[],
  specimen = "Serum",
) {
  await page.goto(
    `/ModifyOrder?accessionNumber=${encodeURIComponent(labNumber)}`,
    { waitUntil: "domcontentloaded" },
  );
  await expect(
    page.getByRole("button", { name: `Accession Number : ${labNumber}` }),
  ).toBeVisible({ timeout: NAV_TIMEOUT });
  await expect(
    page.getByRole("button", { name: `National ID : ${patient.nationalId}` }),
  ).toBeVisible();
  await expect(main(page)).toContainText(
    `${patient.lastName}, ${patient.firstName}`,
  );
  await page.getByRole("button", { name: "Next", exact: true }).click();
  const currentTests = page.getByRole("table", { name: "Current Tests" });
  await expect(currentTests).toBeVisible({ timeout: UI_TIMEOUT });
  await expect(
    page
      .locator(".cds--data-table-container")
      .filter({ has: currentTests })
      .locator("..")
      .getByText(`of ${testNames.length} items`),
  ).toBeVisible();
  const specimenRow = currentTests
    .getByRole("row")
    .filter({ hasText: `${labNumber}-1` });
  await expect(specimenRow).toContainText(specimen);
  if (testNames.length === 1)
    await expect(specimenRow).toContainText(testNames[0]);
}

/** Searches for the patient by last name and selects them from the results. */
async function pickExistingPatient(page: Page, patient: SeededPatient) {
  await main(page)
    .getByRole("textbox", { name: "Last Name" })
    .fill(patient.lastName);
  await main(page).getByRole("button", { name: "Search", exact: true }).click();
  await page.locator(`label[for="${patient.patientPK}"]`).click();
  await expect(page.getByRole("textbox", { name: "National ID*" })).toHaveValue(
    patient.nationalId,
    { timeout: UI_TIMEOUT },
  );
}

/** Generates the pre-printed lab number, which saves the order; returns it. */
async function generateAndSave(page: Page): Promise<string> {
  const labNumberField = page.getByRole("textbox", { name: "Lab Number *" });
  await main(page).getByRole("link", { name: "Generate", exact: true }).click();
  await expect(labNumberField).not.toHaveValue("", { timeout: UI_TIMEOUT });
  const labNumber = await labNumberField.inputValue();
  await expect(
    main(page).getByRole("heading", { name: "Print labels" }),
  ).toBeVisible({
    timeout: LONG_TIMEOUT,
  });
  await expect(main(page).getByTitle(labNumber, { exact: true })).toBeVisible();
  return labNumber;
}

test.describe("Batch order entry", () => {
  test("Next stays disabled until tests and a method are chosen", async ({
    page,
  }) => {
    await openSetup(page);
    const next = main(page).getByRole("button", { name: "Next", exact: true });
    await expect(next).toBeDisabled();

    await chooseRoutineSerum(page, ["Serologie VIH"], []);
    await expect(next).toBeDisabled();

    await page
      .getByRole("combobox", { name: "Methods" })
      .selectOption({ label: "On Demand" });
    await expect(next).toBeEnabled();

    await toggle(page, "Serologie VIH");
    await expect(checkbox(page, "Serologie VIH")).not.toBeChecked();
    await expect(next).toBeDisabled();
  });

  test("the EID form offers its specimens and enables Next once DNA PCR is ticked", async ({
    page,
  }) => {
    await openSetup(page);
    await page.getByRole("combobox", { name: "Form:*" }).selectOption("EID");
    await page
      .getByRole("combobox", { name: "Methods" })
      .selectOption({ label: "Pre-Printed" });
    const next = main(page).getByRole("button", { name: "Next", exact: true });
    await expect(
      main(page).getByRole("checkbox", { name: "Dry Tube", exact: true }),
    ).toBeVisible();
    await expect(
      main(page).getByRole("checkbox", { name: "Dry Blood Spot", exact: true }),
    ).toBeVisible();
    await expect(next).toBeDisabled();

    await tick(page, "DNA PCR");
    await expect(next).toBeEnabled();
  });

  test("On Demand saves a new patient's order per generated barcode", async ({
    page,
    ownedRecords,
  }) => {
    test.setTimeout(120_000);
    const panels = ["Bilan Biochimique", "Serologie VIH"];
    const tests = ["DENGUE PCR"];
    const testNames = await orderedTestNames(page, panels, tests);
    const patient = {
      nationalId: `BO${Date.now()}${letters(4)}`,
      lastName: letters(12),
      firstName: letters(8),
    };
    let labNumber = "";
    const siteName = `Batch Clinic ${letters(12)}`;
    const listsResponse = await page.request.get(`${API}/locations/lists`);
    expect(listsResponse.ok()).toBeTruthy();
    const lists = await listsResponse.json();
    const clinicType = lists.facilityTypes.find(
      (type: { name: string }) => type.name === "referring clinic",
    );
    expect(clinicType).toBeTruthy();
    const { detail } = (await apiPost(page, "/rest/locations/organizations", {
      kind: "facility",
      name: siteName,
      typeIds: [clinicType.id],
    })) as { detail: { row: { id: string } } };
    ownedRecords.organizationIds.push(detail.row.id);
    await test.step("set up a routine serum batch with site and patient", async () => {
      await openSetup(page);
      await chooseRoutineSerum(page, panels, tests);
      await page
        .getByRole("combobox", { name: "Methods" })
        .selectOption({ label: "On Demand" });
      await tick(page, "Facility");
      await tick(page, "Patient Info");
      await page.getByRole("textbox", { name: "Site Name" }).fill(siteName);
      const suggestion = page
        .locator('[data-cy="auto-suggestion"]')
        .filter({ hasText: siteName });
      await expect(suggestion).toBeVisible({ timeout: UI_TIMEOUT });
      const siteLabel = (await suggestion.innerText()).trim();
      await suggestion.click();
      await expect(
        page.getByRole("textbox", { name: "Site Name" }),
      ).toHaveValue(siteLabel);
      await toEntryPage(page);
      await expect(main(page)).toContainText(/Sample Type\s*Serum/);
      await expect(main(page)).toContainText(siteName);
      for (const name of testNames) {
        await expect(main(page)).toContainText(name);
      }
    });

    await test.step("enter a new patient and generate the first barcode", async () => {
      await main(page).getByRole("button", { name: "New Patient" }).click();
      await page
        .getByRole("textbox", { name: "National ID*" })
        .fill(patient.nationalId);
      await page
        .getByRole("textbox", { name: "Last Name" })
        .fill(patient.lastName);
      await page
        .getByRole("textbox", { name: "First Name" })
        .fill(patient.firstName);
      await page.getByRole("spinbutton", { name: "Age/Years" }).fill("30");
      await page
        .getByRole("group", { name: "Sex *" })
        .getByText("Female", { exact: true })
        .click();
      await expect(
        page.getByRole("textbox", { name: "Date of Birth *" }),
      ).not.toHaveValue("");

      const labNumberField = page.getByRole("textbox", {
        name: "Lab Number *",
      });
      await expect(labNumberField).toHaveValue("");
      await page
        .getByRole("link", { name: "Generate Barcode and Save" })
        .click();
      await expect(
        page.getByRole("heading", { name: "Print labels" }),
      ).toBeVisible({ timeout: LONG_TIMEOUT });
      await expect(labNumberField).not.toHaveValue("");
      labNumber = await labNumberField.inputValue();
      await expect(
        main(page).getByTitle(labNumber, { exact: true }),
      ).toBeVisible();
    });

    await test.step("Next Label clears the lab number for the next order", async () => {
      await page.getByRole("button", { name: "Next Label" }).click();
      await expect(
        page.getByRole("textbox", { name: "Lab Number *" }),
      ).toHaveValue("");
      await expect(
        page.getByRole("button", { name: "Next Label" }),
      ).toBeDisabled();
      await page
        .getByRole("button", { name: "Previously used Accession Number" })
        .click();
      await expect(
        main(page).getByRole("heading", { name: labNumber }),
      ).toBeVisible();
    });

    await test.step("Finish returns to setup", async () => {
      await main(page).getByRole("button", { name: "Finish" }).click();
      await expect(
        page.getByRole("heading", { name: "Batch Order Entry Setup" }),
      ).toBeVisible({ timeout: NAV_TIMEOUT });
    });

    await test.step("the order was saved for the new patient", async () => {
      await expectSavedOrder(page, labNumber, patient, testNames);
    });
  });

  test("Pre-Printed saves an existing patient's order against the lab number", async ({
    page,
  }) => {
    test.setTimeout(120_000);
    const patient: SeededPatient = await seedPatient(page);
    const panels = ["Serologie VIH"];
    const testNames = await orderedTestNames(page, panels, []);
    let labNumber = "";

    await test.step("set up a pre-printed routine serum batch", async () => {
      await openSetup(page);
      await chooseRoutineSerum(page, panels, []);
      await page
        .getByRole("combobox", { name: "Methods" })
        .selectOption({ label: "Pre-Printed" });
      await tick(page, "Patient Info");
      await toEntryPage(page);
    });

    await test.step("pick the existing patient", async () => {
      await pickExistingPatient(page, patient);
    });

    await test.step("generate the lab number and save", async () => {
      labNumber = await generateAndSave(page);
      await main(page)
        .getByRole("button", { name: "Save", exact: true })
        .click();
      await page
        .getByRole("button", { name: "Previously used Accession Number" })
        .click();
      await expect(
        main(page).getByRole("heading", { name: labNumber }),
      ).toBeVisible();
    });

    await test.step("the order was saved for the chosen patient", async () => {
      await expectSavedOrder(page, labNumber, patient, testNames);
    });
  });

  test("Pre-Printed saves one order for a lab number typed by hand, never one per keystroke", async ({
    page,
  }) => {
    test.setTimeout(120_000);
    const patient: SeededPatient = await seedPatient(page);
    const panels = ["Serologie VIH"];
    const testNames = await orderedTestNames(page, panels, []);
    const generated = await page.request.get(
      `${API}/SampleEntryGenerateScanProvider`,
    );
    const labNumber =
      ((await generated.json()) as { body?: string }).body ?? "";
    expect(labNumber, "an unused lab number to type").not.toBe("");
    const saves: string[] = [];
    page.on("request", (request) => {
      if (
        request.method() === "POST" &&
        new URL(request.url()).pathname.endsWith(
          "/rest/SamplePatientEntryBatch",
        )
      ) {
        saves.push(request.postData() ?? "");
      }
    });

    await test.step("set up a pre-printed routine serum batch for an existing patient", async () => {
      await openSetup(page);
      await chooseRoutineSerum(page, panels, []);
      await page
        .getByRole("combobox", { name: "Methods" })
        .selectOption({ label: "Pre-Printed" });
      await tick(page, "Patient Info");
      await toEntryPage(page);
      await pickExistingPatient(page, patient);
    });

    const labNumberField = page.getByRole("textbox", { name: "Lab Number *" });
    const save = main(page).getByRole("button", { name: "Save", exact: true });

    await test.step("typing the lab number saves nothing", async () => {
      await expect(save).toBeDisabled();
      await labNumberField.pressSequentially(labNumber);
      await expect(labNumberField).toHaveValue(labNumber);
      await expect(save).toBeEnabled();
      expect(saves, "no order is saved while typing").toHaveLength(0);
      await expect(
        main(page).getByRole("heading", { name: "Print labels" }),
      ).toHaveCount(0);
    });

    await test.step("Save saves the order once", async () => {
      const saved = page.waitForResponse(
        (response) =>
          response.request().method() === "POST" &&
          response.url().endsWith("/rest/SamplePatientEntryBatch"),
      );
      await save.click();
      await saved;
      await expect(
        main(page).getByRole("heading", { name: "Print labels" }),
      ).toBeVisible({ timeout: LONG_TIMEOUT });
      expect(saves).toHaveLength(1);
      expect(JSON.parse(saves[0]).sampleOrderItems.labNo).toBe(labNumber);
    });

    await test.step("no order exists under a partial lab number", async () => {
      for (let length = 1; length < labNumber.length; length++) {
        const partial = labNumber.slice(0, length);
        const response = await page.request.get(
          `${API}/order/search?labNumber=${encodeURIComponent(partial)}`,
        );
        expect(response.status(), `no order numbered "${partial}"`).toBe(404);
      }
    });

    await test.step("the order was saved for the chosen patient", async () => {
      await expectSavedOrder(page, labNumber, patient, testNames);
    });
  });

  test("EID saves an order with the specimen and DNA PCR test that were ticked", async ({
    page,
    ownedRecords,
  }) => {
    test.setTimeout(120_000);
    const patient: SeededPatient = await seedPatient(page);
    let labNumber = "";
    const stamp = Date.now().toString();
    const unitsResponse = await page.request.get(
      `${API}/test-catalog/lab-units`,
    );
    expect(unitsResponse.ok()).toBeTruthy();
    const units = await unitsResponse.json();
    // The baseline catalog has no DNA PCR / Dry Tube link. Own this test's
    // catalog entry so the save exercises a configured, compatible specimen.
    const { testId } = (await apiPost(page, "/rest/test-catalog/tests", {
      name: "DNA PCR",
      reportingName: "DNA PCR",
      description: `EID batch ${stamp}`,
      code: `EID${stamp}`,
      labUnitId: units[0].id,
      sampleTypeIds: ["24"],
      domain: "CLINICAL",
      orderable: true,
    })) as { testId: string };
    ownedRecords.testIds.push(testId);
    const headers = { "X-CSRF-Token": await csrfToken(page) };
    const configured = await page.request.put(
      `${API}/test-catalog/tests/${testId}/sample-results`,
      {
        headers,
        data: {
          testId,
          components: [
            {
              code: "PRIMARY",
              label: "DNA PCR",
              displayOrder: 0,
              resultType: "N",
              isPrimary: true,
              showOnReport: true,
              interpretations: [],
              options: [],
            },
          ],
        },
      },
    );
    expect(configured.ok(), await configured.text()).toBeTruthy();
    await apiPost(page, `/rest/test-catalog/tests/${testId}/activate`, {
      gapsAcknowledged: "EID batch browser test does not enter numeric results",
    });

    await test.step("set up a pre-printed EID batch with a dry tube and DNA PCR", async () => {
      await openSetup(page);
      await page.getByRole("combobox", { name: "Form:*" }).selectOption("EID");
      await page
        .getByRole("combobox", { name: "Methods" })
        .selectOption({ label: "Pre-Printed" });
      await tick(page, "Dry Tube");
      await expect(
        main(page).getByRole("button", { name: "Next", exact: true }),
      ).toBeDisabled();
      await expect(checkbox(page, "DNA PCR")).not.toBeChecked();
      await tick(page, "DNA PCR");
      await expect(checkbox(page, "Dry Blood Spot")).not.toBeChecked();
      await tick(page, "Patient Info");
      await toEntryPage(page);
      await expect(main(page)).toContainText("DNA PCR");
    });

    await test.step("pick the existing patient and save under a generated lab number", async () => {
      await pickExistingPatient(page, patient);
      labNumber = await generateAndSave(page);
    });

    await test.step("the order carries one Dry Tube specimen with DNA PCR", async () => {
      const response = await page.request.get(
        `${API}/order/search?labNumber=${encodeURIComponent(labNumber)}`,
      );
      expect(response.status()).toBe(200);
      const order = await response.json();
      expect(order.samples).toHaveLength(1);
      expect(order.samples[0].tests).toHaveLength(1);
      expect(order.samples[0].tests[0].id).toBe(testId);
      await expectSavedOrder(page, labNumber, patient, ["DNA PCR"], "Dry Tube");
    });
  });
});
