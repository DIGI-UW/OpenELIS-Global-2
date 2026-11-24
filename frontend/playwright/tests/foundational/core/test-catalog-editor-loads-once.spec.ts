import type { Page } from "@playwright/test";
import { test, expect } from "../../../helpers/test-base";
import { NAV_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";

/**
 * The test catalog editor opens a section once, after its test has loaded,
 * and never for a test that failed to load. The Alerts list does not call a
 * disabled rule "Enabled". Test Activation names only the sample types a
 * change turns on, and it and order entry's new-patient notice render
 * without React warnings.
 *
 * Every record the spec creates is retired.
 */

const API = "/api/OpenELIS-Global";
const TC = `${API}/rest/test-catalog`;
const SERUM = "2";
const PLASMA = "3";

const csrfToken = async (page: Page): Promise<string> => {
  const state = await page.context().storageState();
  for (const origin of state.origins) {
    for (const item of origin.localStorage) {
      if (item.name === "CSRF") return item.value;
    }
  }
  return "";
};

const stamp = () => `${Date.now()}`.slice(-7);

const seedTest = async (
  page: Page,
  headers: Record<string, string>,
  sampleTypeIds: string[] = [SERUM],
) => {
  const s = stamp();
  const name = `PWEL ${s}`;
  const labUnits = await (await page.request.get(`${TC}/lab-units`)).json();
  const created = await page.request.post(`${TC}/tests`, {
    headers,
    data: {
      name,
      reportingName: name,
      code: `PWEL${s}`,
      domain: "CLINICAL",
      labUnitId: labUnits[0].id,
      sampleTypeIds,
      description: name,
    },
  });
  expect(created.status(), await created.text()).toBe(201);
  return { testId: String((await created.json()).testId), name };
};

const retire = async (
  page: Page,
  headers: Record<string, string>,
  testId: string,
) => {
  const info = await (
    await page.request.get(`${TC}/tests/${testId}/basic-info`)
  ).json();
  await page.request.put(`${TC}/tests/${testId}/basic-info`, {
    headers,
    data: { ...info, active: false, orderable: false },
  });
};

const reactWarnings = (page: Page) => {
  const seen: string[] = [];
  page.on("console", (message) => {
    if (
      message.type() === "error" &&
      /unique "key"|unmounted component|Failed (%s|prop) type/.test(
        message.text(),
      )
    ) {
      seen.push(message.text());
    }
  });
  return seen;
};

test.describe("Test catalog editor, Alerts and Test Activation", () => {
  test.beforeEach(() => test.setTimeout(180_000));

  test("Alerts loads its rules once and a disabled rule is not called Enabled", async ({
    page,
  }) => {
    const warnings = reactWarnings(page);
    await page.goto("/");
    const headers = { "X-CSRF-Token": await csrfToken(page) };
    const { testId } = await seedTest(page, headers);
    const rule = {
      name: `PWEL rule ${stamp()}`,
      enabled: false,
      triggerType: "CRITICAL",
      triggerValue: null,
      componentId: null,
      sampleTypeId: null,
      notifySms: false,
      notifyEmail: true,
      notifyOrderingPhysician: false,
      notifyPatient: false,
      notifyReferringFacility: false,
      notifyCustomPhone: null,
      notifyCustomEmail: "qa@example.org",
      notifyRoleId: null,
      acknowledgmentRequired: false,
    };
    const created = await page.request.post(`${TC}/${testId}/alerts`, {
      headers,
      data: rule,
    });
    expect(created.status()).toBe(201);
    const ruleId = (await created.json()).id;
    const ruleLoads: string[] = [];
    page.on("request", (request) => {
      if (
        request.method() === "GET" &&
        request.url().endsWith(`/test-catalog/${testId}/alerts`)
      ) {
        ruleLoads.push(request.url());
      }
    });
    try {
      await page.goto(`/MasterListsPage/TestCatalogEditor/${testId}/alerts`, {
        waitUntil: "domcontentloaded",
      });
      const row = page.getByTestId(`alert-row-${ruleId}`);
      await expect(row).toContainText(rule.name, { timeout: NAV_TIMEOUT });
      const enabled = row.getByRole("switch", { name: "Enabled" });
      await expect(enabled).toHaveAttribute("aria-checked", "false");
      await expect(row).not.toContainText("Enabled");

      const saved = page.waitForResponse(
        (r) =>
          r.url().endsWith(`/alerts/${ruleId}`) &&
          r.request().method() === "PUT",
      );
      await page.locator(`#enabled-${ruleId}_label`).click();
      expect((await saved).status()).toBe(200);
      await expect(enabled).toHaveAttribute("aria-checked", "true", {
        timeout: UI_TIMEOUT,
      });
      const stored = (
        (await (await page.request.get(`${TC}/${testId}/alerts`)).json()) as {
          id: string;
          enabled: boolean;
        }[]
      ).find((r) => r.id === ruleId);
      expect(stored?.enabled).toBe(true);
      expect(ruleLoads, "the section loads its rules once").toHaveLength(1);
      expect(warnings).toEqual([]);
    } finally {
      await page.request.delete(`${TC}/${testId}/alerts/${ruleId}`, {
        headers,
      });
      await retire(page, headers, testId);
    }
  });

  test("a test that cannot be loaded shows the error and opens no section", async ({
    page,
  }) => {
    const missing = "9999997";
    const sectionReads: string[] = [];
    page.on("request", (request) => {
      if (request.url().includes(`/test-catalog/tests/${missing}/`)) {
        sectionReads.push(request.url());
      }
    });
    await page.goto(
      `/MasterListsPage/TestCatalogEditor/${missing}/basic-info`,
      {
        waitUntil: "domcontentloaded",
      },
    );

    await expect(
      page.getByText("Unable to load this test. Please try again."),
    ).toBeVisible({ timeout: NAV_TIMEOUT });
    expect(sectionReads).toEqual([]);
  });

  test("Test Activation names only what a change turns on, and an undone change leaves nothing to submit", async ({
    page,
  }) => {
    const warnings = reactWarnings(page);
    await page.goto("/");
    const headers = { "X-CSRF-Token": await csrfToken(page) };
    const { testId, name } = await seedTest(page, headers);
    try {
      await page.goto("/MasterListsPage/TestActivation", {
        waitUntil: "domcontentloaded",
      });
      const seeded = page.locator(`label[for="${SERUM}-${testId}"]`);
      await expect(seeded).toBeVisible({ timeout: NAV_TIMEOUT });
      await expect(page.locator(`[id="${SERUM}-${testId}"]`)).not.toBeChecked();
      const submit = page
        .getByRole("button", { name: "Submit", exact: true })
        .first();

      await seeded.click();
      await expect(submit).toBeEnabled();
      await seeded.click();
      await expect(submit).toBeDisabled();

      await seeded.click();
      await submit.click();

      const dialog = page.getByRole("dialog");
      await expect(
        dialog.getByText("The following tests will be activated:"),
      ).toBeVisible({ timeout: UI_TIMEOUT });
      await expect(dialog).toContainText(name);
      await expect(
        dialog.getByText("The following sample types will be activated:"),
      ).toHaveCount(0);
      await expect(
        page.getByTestId("test-activation-sample-types-to-activate"),
      ).toHaveCount(0);
      await expect(dialog.getByText(/will be deactivated/)).toHaveCount(0);
      await dialog.getByRole("button", { name: "Back", exact: true }).click();
      expect(warnings).toEqual([]);
    } finally {
      await retire(page, headers, testId);
    }
  });

  test("Test Activation turns off a test listed under two sample types from either one, and Back keeps the change", async ({
    page,
  }) => {
    const warnings = reactWarnings(page);
    await page.goto("/");
    const headers = { "X-CSRF-Token": await csrfToken(page) };
    const { testId, name } = await seedTest(page, headers, [SERUM, PLASMA]);
    const underSerum = page.locator(`[id="${SERUM}-${testId}"]`);
    const underPlasma = page.locator(`[id="${PLASMA}-${testId}"]`);
    const submit = page
      .getByRole("button", { name: "Submit", exact: true })
      .first();
    const dialog = page.getByRole("dialog");
    const accept = async () => {
      const saved = page.waitForResponse(
        (response) =>
          response.url().includes("/rest/TestActivation") &&
          response.request().method() === "POST",
      );
      await dialog.getByRole("button", { name: "Accept", exact: true }).click();
      await saved;
    };
    const open = async () => {
      await page.goto("/MasterListsPage/TestActivation", {
        waitUntil: "domcontentloaded",
      });
      await expect(page.locator(`label[for="${SERUM}-${testId}"]`)).toBeVisible(
        { timeout: NAV_TIMEOUT },
      );
    };
    try {
      await open();
      await page.locator(`label[for="${SERUM}-${testId}"]`).click();
      await submit.click();
      await expect(dialog).toContainText(name, { timeout: UI_TIMEOUT });
      await accept();
      await open();
      await expect(underSerum).toBeChecked();
      await expect(underPlasma).toBeChecked();

      await page.locator(`label[for="${PLASMA}-${testId}"]`).click();
      await expect(underSerum).toBeChecked();
      await submit.click();
      await expect(
        dialog.getByText("The following tests will be deactivated:"),
      ).toBeVisible({ timeout: UI_TIMEOUT });
      await expect(dialog).toContainText(name);
      await dialog.getByRole("button", { name: "Back", exact: true }).click();
      await expect(underPlasma).not.toBeChecked();
      await expect(submit).toBeEnabled();

      await submit.click();
      await expect(dialog).toContainText(name, { timeout: UI_TIMEOUT });
      await accept();
      await open();
      await expect(underSerum).not.toBeChecked();
      await expect(underPlasma).not.toBeChecked();
      expect(warnings).toEqual([]);
    } finally {
      await retire(page, headers, testId);
    }
  });

  test("order entry's new-patient notice renders without a prop warning", async ({
    page,
  }) => {
    const warnings = reactWarnings(page);
    await page.goto("/order/clinical/enter", { waitUntil: "domcontentloaded" });
    await page
      .getByRole("button", { name: "New Patient", exact: true })
      .click({ timeout: NAV_TIMEOUT });
    const section = page.getByTestId("patient-search-section");
    await section
      .getByRole("textbox", { name: "Last Name", exact: true })
      .fill(`Pwel${stamp()}`);
    await section
      .getByRole("textbox", { name: "First Name", exact: true })
      .fill("Notice");

    await page.getByTestId("order-create-patient").click();

    await expect(page.getByTestId("order-new-patient-confirmed")).toContainText(
      "The new patient is created when the order is saved.",
      { timeout: UI_TIMEOUT },
    );
    expect(warnings).toEqual([]);
  });
});
