import { Page, Request } from "@playwright/test";
import { test, expect } from "../../../helpers/test-base";
import { csrfToken } from "../../../helpers/api-session";
import {
  LONG_TIMEOUT,
  NAV_TIMEOUT,
  UI_TIMEOUT,
} from "../../../helpers/timeouts";

/**
 * OGC-1407: an order entered with a new patient keeps that one patient however
 * many times it is saved. The first save creates the patient; every later save
 * of the order, from Enter Order or from Collect, refers to it by id.
 *
 * The walk that found the bug: Save, Save, Save & Next left three patients
 * with the same national id, and only the last one on the order. With the
 * OGC-1266 footer the same walk is Save and exit, reopen, Save and exit,
 * reopen, Save and next.
 */

const SAVE_ENDPOINT = "/rest/SamplePatientEntry";

type PatientSent = {
  patientPK?: string;
  patientUpdateStatus?: string;
  nationalId?: string;
};

/** Records the patient carried by every order save the page sends. */
function recordPatientSaves(page: Page): PatientSent[] {
  const sent: PatientSent[] = [];
  page.on("request", (request: Request) => {
    if (request.url().includes(SAVE_ENDPOINT) && request.method() === "POST") {
      const body = request.postDataJSON() as {
        patientProperties?: PatientSent;
      };
      sent.push({ ...(body.patientProperties || {}) });
    }
  });
  return sent;
}

async function patientsWithNationalId(page: Page, nationalId: string) {
  const found = await page.request.get(
    `/api/OpenELIS-Global/rest/patient-search-results?lastName=&firstName=&STNumber=&subjectNumber=&nationalID=${encodeURIComponent(nationalId)}&labNumber=&guid=&dateOfBirth=&gender=&suppressExternalSearch=true`,
  );
  expect(found.status()).toBe(200);
  const body = (await found.json()) as {
    patientSearchResults?: Array<{ patientID: string; nationalId: string }>;
  };
  return (body.patientSearchResults || []).filter(
    (patient) => patient.nationalId === nationalId,
  );
}

async function openEnterOrder(page: Page) {
  await page.goto("/order/clinical/enter", { waitUntil: "domcontentloaded" });
  await expect(page.locator("#labNumber")).not.toHaveValue("", {
    timeout: NAV_TIMEOUT,
  });
  return page.locator("#labNumber").inputValue();
}

/** Reopens a saved order on Enter Order, the way the dashboard does. */
async function reopenEnterOrder(page: Page, labNumber: string) {
  await page.goto(
    `/order/clinical/enter?order=${encodeURIComponent(labNumber)}`,
    { waitUntil: "domcontentloaded" },
  );
  await expect(page.locator("#labNumber")).toHaveValue(labNumber, {
    timeout: NAV_TIMEOUT,
  });
}

async function enterNewPatient(page: Page, nationalId: string) {
  const section = page.getByTestId("patient-search-section");
  await section.getByRole("button", { name: "New Patient" }).click();
  await section.locator("#nationalId").fill(nationalId);
  await section.locator("#lastName").fill("Qadup");
  await section.locator("#firstName").fill("Nia");
  await section
    .getByRole("textbox", { name: "Date of Birth" })
    .fill("05/03/1988");
  await section
    .locator("#create_patient_gender label")
    .filter({ hasText: "Female" })
    .click();
}

async function chooseSerumWithOneTest(page: Page) {
  const sampleSection = page.getByTestId("order-sample-test-section");
  await sampleSection
    .getByLabel("Sample Type *")
    .selectOption({ label: "Serum" });
  const firstTest = sampleSection.locator('label[for^="test-0-"]').first();
  await expect(firstTest).toBeVisible({ timeout: LONG_TIMEOUT });
  await firstTest.click();
}

async function saveWith(page: Page, name: RegExp | string) {
  const saved = page.waitForResponse(
    (response) =>
      response.url().includes(SAVE_ENDPOINT) &&
      response.request().method() === "POST",
    { timeout: LONG_TIMEOUT },
  );
  await page.getByRole("button", { name, exact: true }).last().click();
  const response = await saved;
  expect(response.status()).toBe(200);
}

test.describe("OGC-1407: one patient per order, however often it is saved", () => {
  test("Save and exit, reopen, Save and exit, reopen, Save and next keeps one patient", async ({
    page,
  }) => {
    test.setTimeout(240_000);
    const nationalId = `QA1407A${Date.now()}`;
    const sent = recordPatientSaves(page);

    const labNumber = await openEnterOrder(page);
    await enterNewPatient(page, nationalId);
    await chooseSerumWithOneTest(page);

    await saveWith(page, "Save and exit");
    await expect(page.locator("tr.order-highlighted")).toContainText(
      labNumber,
      { timeout: UI_TIMEOUT },
    );

    // The patient is now a saved record: the reopened order shows it as the
    // selected patient, and its details open locked, with an Edit toggle,
    // instead of a form that would add the patient again.
    await reopenEnterOrder(page, labNumber);
    const section = page.getByTestId("patient-search-section");
    await expect(section.locator(".selected-entity-card")).toContainText(
      nationalId,
      { timeout: UI_TIMEOUT },
    );
    await section.getByRole("button", { name: "Edit details" }).click();
    await expect(section.locator("#patient-edit-toggle")).toBeVisible({
      timeout: UI_TIMEOUT,
    });
    await expect(section.locator("#nationalId")).toHaveValue(nationalId);
    await expect(section.locator("#nationalId")).toBeDisabled();

    await saveWith(page, "Save and exit");
    await reopenEnterOrder(page, labNumber);
    await saveWith(page, "Save and next");
    await expect(page).toHaveURL(/\/order\/clinical\/collect/, {
      timeout: LONG_TIMEOUT,
    });

    expect(sent).toHaveLength(3);
    expect(sent[0].patientUpdateStatus).toBe("ADD");
    expect(sent[0].patientPK || "").toBe("");
    for (const later of sent.slice(1)) {
      expect(later.patientPK).toBeTruthy();
      expect(later.patientPK).toBe(sent[1].patientPK);
      expect(later.patientUpdateStatus).not.toBe("ADD");
    }

    await expect
      .poll(() => patientsWithNationalId(page, nationalId), {
        timeout: LONG_TIMEOUT,
      })
      .toHaveLength(1);
  });

  test("Save and next then Save and exit on Prepare Samples keeps one patient", async ({
    page,
  }) => {
    test.setTimeout(240_000);
    const nationalId = `QA1407B${Date.now()}`;
    const sent = recordPatientSaves(page);

    await openEnterOrder(page);
    await enterNewPatient(page, nationalId);
    await chooseSerumWithOneTest(page);
    await saveWith(page, "Save and next");
    await expect(page).toHaveURL(/\/order\/clinical\/collect/, {
      timeout: LONG_TIMEOUT,
    });

    await expect(
      page.getByRole("button", { name: "Save and exit", exact: true }).last(),
    ).toBeEnabled({ timeout: LONG_TIMEOUT });
    await saveWith(page, "Save and exit");

    expect(sent).toHaveLength(2);
    expect(sent[1].patientPK).toBeTruthy();
    expect(sent[1].patientUpdateStatus).not.toBe("ADD");
    await expect
      .poll(() => patientsWithNationalId(page, nationalId), {
        timeout: LONG_TIMEOUT,
      })
      .toHaveLength(1);
  });

  test("an edit made after the first save reaches the patient record", async ({
    page,
  }) => {
    test.setTimeout(240_000);
    const nationalId = `QA1407C${Date.now()}`;
    const sent = recordPatientSaves(page);

    const labNumber = await openEnterOrder(page);
    await enterNewPatient(page, nationalId);
    await chooseSerumWithOneTest(page);
    await saveWith(page, "Save and exit");
    await reopenEnterOrder(page, labNumber);

    const section = page.getByTestId("patient-search-section");
    await section.getByRole("button", { name: "Edit details" }).click();
    await expect(section.locator("#patient-edit-toggle")).toBeVisible({
      timeout: UI_TIMEOUT,
    });
    await section.locator("#patient-edit-toggle_label").click();
    await expect(section.locator("#primaryPhone")).toBeEnabled({
      timeout: UI_TIMEOUT,
    });
    await section.locator("#primaryPhone").fill("0788123456");
    await saveWith(page, "Save and exit");

    expect(sent).toHaveLength(2);
    expect(sent[1].patientPK).toBeTruthy();
    expect(sent[1].patientUpdateStatus).toBe("UPDATE");

    const details = await page.request.get(
      `/api/OpenELIS-Global/rest/patient-details?patientID=${sent[1].patientPK}`,
    );
    expect(details.status()).toBe(200);
    expect(
      ((await details.json()) as { primaryPhone?: string }).primaryPhone,
    ).toBe("0788123456");
    expect(await patientsWithNationalId(page, nationalId)).toHaveLength(1);
  });

  test("the server keeps the order's patient when a save asks to add the same person again", async ({
    page,
  }) => {
    test.setTimeout(240_000);
    const nationalId = `QA1407D${Date.now()}`;
    let firstSave: Record<string, unknown> | undefined;
    page.on("request", (request: Request) => {
      if (
        request.url().includes(SAVE_ENDPOINT) &&
        request.method() === "POST" &&
        !firstSave
      ) {
        firstSave = request.postDataJSON() as Record<string, unknown>;
      }
    });

    const labNumber = await openEnterOrder(page);
    await enterNewPatient(page, nationalId);
    await chooseSerumWithOneTest(page);
    await saveWith(page, "Save and exit");
    expect(firstSave).toBeTruthy();

    const order = await page.request.get(
      `/api/OpenELIS-Global/rest/order/search?labNumber=${encodeURIComponent(labNumber)}`,
    );
    expect(order.status()).toBe(200);
    const orderId = ((await order.json()) as { id: string }).id;

    // The shape a client that lost the patient id after its first save sends.
    const replay = {
      ...firstSave,
      sampleOrderItems: {
        ...(firstSave!.sampleOrderItems as Record<string, unknown>),
        sampleId: orderId,
        orderKey: undefined,
      },
      patientProperties: {
        ...(firstSave!.patientProperties as Record<string, unknown>),
        patientPK: "",
        patientUpdateStatus: "ADD",
      },
    };
    const replayed = await page.request.post(
      `/api/OpenELIS-Global${SAVE_ENDPOINT}`,
      {
        data: replay,
        headers: { "X-CSRF-Token": await csrfToken(page) },
      },
    );
    expect(replayed.status()).toBe(200);

    expect(await patientsWithNationalId(page, nationalId)).toHaveLength(1);
  });
});
