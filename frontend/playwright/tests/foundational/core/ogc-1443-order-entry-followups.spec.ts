import { Page } from "@playwright/test";
import { test, expect } from "../../../helpers/test-base";
import { csrfToken } from "../../../helpers/api-session";
import {
  LONG_TIMEOUT,
  NAV_TIMEOUT,
  UI_TIMEOUT,
} from "../../../helpers/timeouts";

/**
 * OGC-1443 — order entry v4 follow-ups from acceptance:
 *  - patient search on Enter Order matches names exactly or by prefix, and its
 *    sortable columns are announced by name;
 *  - a sample without tests holds Save and next on Enter Order, naming it;
 *  - Prepare Samples opened by Save and next carries the order's lab number
 *    and starts clean, with help text that points at controls that exist;
 *  - Refer Out names each tube by lab number and position under "Reference
 *    lab";
 *  - Sample check names the samples not accepted yet when a release needs a
 *    reason.
 */

const API = "/api/OpenELIS-Global";

async function acceptanceMode(page: Page): Promise<string> {
  const response = await page.request.get(
    `${API}/rest/sample-acceptance-checklist/enforcement`,
  );
  expect(response.status()).toBe(200);
  const modes = (await response.json()) as Record<string, string>;
  return (modes.clinical || "OPTIONAL").toUpperCase();
}

const CHECKLIST_ITEMS = `${API}/rest/sample-acceptance-checklist/admin/items`;

async function addClinicalChecklistItem(page: Page) {
  const label = `QA1443 label legible ${Date.now()}`;
  const response = await page.request.post(CHECKLIST_ITEMS, {
    headers: { "X-CSRF-Token": await csrfToken(page) },
    data: { domain: "CLINICAL", label },
  });
  expect(response.status()).toBe(200);
  const item = (await response.json()) as { id: string };
  return { id: String(item.id), label };
}

async function retireChecklistItem(
  page: Page,
  item: { id: string; label: string },
) {
  const response = await page.request.put(`${CHECKLIST_ITEMS}/${item.id}`, {
    headers: { "X-CSRF-Token": await csrfToken(page) },
    data: { label: item.label, active: false },
  });
  expect(response.status()).toBe(200);
}

async function openNewOrder(page: Page) {
  await page.goto("/order/clinical/enter", { waitUntil: "domcontentloaded" });
  await expect(page.locator("#labNumber")).not.toHaveValue("", {
    timeout: NAV_TIMEOUT,
  });
  return page.locator("#labNumber").inputValue();
}

async function enterNewPatient(page: Page, lastName: string) {
  const section = page.getByTestId("patient-search-section");
  await section.getByRole("button", { name: "New Patient" }).click();
  await section.locator("#nationalId").fill(`QA1443${Date.now()}`);
  await section.locator("#lastName").fill(lastName);
  await section.locator("#firstName").fill("Ida");
  await section
    .getByRole("textbox", { name: "Date of Birth" })
    .fill("05/03/1988");
  await section
    .locator("#create_patient_gender label")
    .filter({ hasText: "Female" })
    .click();
}

async function chooseSerum(page: Page, index: number, withTest: boolean) {
  const sampleSection = page.getByTestId("order-sample-test-section");
  await sampleSection
    .locator(`#sampleType-${index}`)
    .selectOption({ label: "Serum" });
  if (withTest) {
    const firstTest = sampleSection
      .locator(`label[for^="test-${index}-"]`)
      .first();
    await expect(firstTest).toBeVisible({ timeout: LONG_TIMEOUT });
    await firstTest.click();
  }
}

async function saveWith(page: Page, name: string) {
  const saved = page.waitForResponse(
    (response) =>
      response.url().includes("/rest/SamplePatientEntry") &&
      response.request().method() === "POST",
    { timeout: LONG_TIMEOUT },
  );
  await page.getByRole("button", { name, exact: true }).click();
  expect((await saved).status()).toBe(200);
}

const letters = (stamp: number) =>
  String(stamp)
    .slice(-6)
    .split("")
    .map((digit) => String.fromCharCode(97 + Number(digit)))
    .join("");

test.describe("OGC-1443 order entry follow-ups", () => {
  test("patient search on Enter Order matches by prefix only and names its sortable columns", async ({
    page,
  }) => {
    test.setTimeout(180_000);
    const tag = letters(Date.now());
    const lastName = `Prefixwalk${tag}`;
    await openNewOrder(page);
    await enterNewPatient(page, lastName);
    await chooseSerum(page, 0, true);
    await saveWith(page, "Save and exit");
    await expect(page).toHaveURL(/\/order\/clinical\?highlight=/, {
      timeout: LONG_TIMEOUT,
    });

    await openNewOrder(page);
    const results = page.getByTestId("order-patient-search");
    const search = async (term: string) => {
      await page.locator("#order-patient-search-lastName").fill(term);
      await results
        .getByRole("button", { name: "Search", exact: true })
        .click();
    };

    await search(`walk${tag}`);
    await expect(
      results.getByRole("cell", { name: lastName, exact: true }),
    ).toHaveCount(0, { timeout: UI_TIMEOUT });

    await search(`prefixwalk${tag.slice(0, 3)}`);
    await expect(
      results.getByRole("cell", { name: lastName, exact: true }),
    ).toBeVisible({ timeout: UI_TIMEOUT });
    await expect(
      results.getByRole("button", { name: /Last Name/ }).first(),
    ).toBeVisible();
    await expect(results).not.toContainText("[object Object]");
  });

  test("a sample without tests holds Save and next; Prepare Samples then opens clean on the order with current help text and lab-numbered Refer Out rows", async ({
    page,
  }) => {
    test.setTimeout(240_000);
    const labNumber = await openNewOrder(page);
    await enterNewPatient(page, `Followup${letters(Date.now())}`);
    await chooseSerum(page, 0, true);
    await page
      .getByTestId("order-sample-test-section")
      .getByRole("button", { name: "Add Sample" })
      .click();
    await chooseSerum(page, 1, false);

    const checklist = page.getByTestId("to-continue-checklist");
    await expect(checklist).toContainText(
      "Sample 2: choose at least one test",
      { timeout: UI_TIMEOUT },
    );
    await expect(
      page.getByRole("button", { name: "Save and next", exact: true }),
    ).toBeDisabled();
    await expect(
      page.getByRole("button", { name: "Save and exit", exact: true }),
    ).toBeEnabled();

    await page
      .getByTestId("order-sample-test-section")
      .locator('label[for^="test-1-"]')
      .first()
      .click();
    await expect(checklist).toHaveCount(0, { timeout: UI_TIMEOUT });
    await saveWith(page, "Save and next");

    await expect(page).toHaveURL(
      new RegExp(`/order/clinical/collect\\?order=${labNumber}$`),
      { timeout: LONG_TIMEOUT },
    );
    await expect(page.locator("#collectionTime-0")).not.toHaveValue("", {
      timeout: NAV_TIMEOUT,
    });
    await expect(
      page.getByText("Unsaved changes").filter({ visible: true }),
    ).toHaveCount(0);
    await expect(
      page.getByText(/Use Add Sample, then print its labels/),
    ).toBeVisible();
    await expect(page.getByText(/Print More Sample Labels/)).toHaveCount(0);

    await saveWith(page, "Save and exit");
    await page.goto(`/order/clinical/collect?labNumber=${labNumber}`, {
      waitUntil: "domcontentloaded",
    });
    const referOut = page.locator(".refer-out-section");
    await expect(referOut.getByText(`${labNumber}-2`)).toBeVisible({
      timeout: NAV_TIMEOUT,
    });
    await expect(referOut.getByText(`${labNumber}-1`)).toBeVisible();
    await expect(
      referOut.getByRole("columnheader", { name: /Reference lab/ }),
    ).toBeVisible();
  });

  test("Sample check names the samples not accepted yet when a release needs a reason", async ({
    page,
  }) => {
    test.setTimeout(240_000);
    const mode = await acceptanceMode(page);
    test.skip(
      mode !== "OPTIONAL",
      "a release reason is asked only under Optional acceptance",
    );
    const item = await addClinicalChecklistItem(page);
    try {
      const labNumber = await openNewOrder(page);
      await enterNewPatient(page, `Release${letters(Date.now())}`);
      await chooseSerum(page, 0, true);
      await saveWith(page, "Save and next");
      await expect(page.locator("#collectionTime-0")).not.toHaveValue("", {
        timeout: NAV_TIMEOUT,
      });
      await saveWith(page, "Save and next");
      await expect(page).toHaveURL(/\/order\/clinical\/qa\?order=/, {
        timeout: LONG_TIMEOUT,
      });

      const passes = page.locator("main label").filter({ hasText: /^Pass$/ });
      await expect(passes.first()).toBeVisible({ timeout: NAV_TIMEOUT });
      for (const pass of await passes.all()) {
        await pass.click();
      }
      await page
        .getByRole("button", { name: "Release for testing", exact: true })
        .click();

      await expect(
        page.getByLabel("Reason for releasing before every sample is accepted"),
      ).toBeVisible({ timeout: UI_TIMEOUT });
      await expect(
        page.getByText(
          `Not accepted yet: ${labNumber}-1 Serum. Answers count once you press Accept sample.`,
        ),
      ).toBeVisible();
      await expect(page.getByText(/unanswered/i)).toHaveCount(0);
    } finally {
      await retireChecklistItem(page, item);
    }
  });
});
