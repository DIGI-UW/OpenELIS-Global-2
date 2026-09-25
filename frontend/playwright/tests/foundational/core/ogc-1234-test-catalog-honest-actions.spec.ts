import type { Page } from "@playwright/test";
import { test, expect } from "../../../helpers/test-base";
import { NAV_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";

/**
 * OGC-1234: Test Catalog editor actions report what actually happened.
 *
 * - Sample & Results "Copy from test" replaces this test's configuration with
 *   the source's, after a confirmation, staged in the editor until Save.
 * - Methods "Create New Method" / "Copy from Test" show the server's refusal
 *   as an error and say what a copy did.
 * - Sample Type create with markup in the name is refused (400), not reported
 *   as saved.
 * - Sample Types whose names share their first ten characters are all created
 *   with distinct abbreviations; a name already taken is refused (409) on the
 *   name field.
 * - A panel description may repeat another panel's; a rename onto another
 *   panel's name is refused by name.
 *
 * Panels, methods and sample type names are made unique per run. The one save
 * that rewrites an existing test's configuration restores it afterwards.
 */

const API = "/api/OpenELIS-Global";
const run = Date.now().toString().slice(-6);

interface TestOption {
  id: string;
  value: string;
}

interface Component {
  id?: string;
  code: string;
  options: { id?: string; value: string; valueName?: string }[];
  interpretations: { id?: string }[];
}

interface SampleResults {
  testId: string;
  components: Component[];
}

/** CSRF token saved by auth.setup into storageState localStorage (key "CSRF"). */
const csrfToken = async (page: Page): Promise<string> => {
  const state = await page.context().storageState();
  for (const origin of state.origins) {
    for (const item of origin.localStorage) {
      if (item.name === "CSRF") return item.value;
    }
  }
  return "";
};

const sampleResults = async (
  page: Page,
  testId: string,
): Promise<SampleResults> => {
  const response = await page.request.get(
    `${API}/rest/test-catalog/tests/${testId}/sample-results`,
  );
  expect(response.status()).toBe(200);
  return (await response.json()) as SampleResults;
};

const optionNames = (sr: SampleResults): string[] =>
  sr.components.flatMap((c) => c.options.map((o) => o.valueName || o.value));

/**
 * Two tests whose select-list options differ: a target and a source that has
 * options the target does not.
 */
const pickCopyPair = async (page: Page) => {
  const tests = (await (
    await page.request.get(`${API}/rest/test-list`)
  ).json()) as TestOption[];
  const configured: { test: TestOption; sr: SampleResults }[] = [];
  for (const t of tests) {
    const sr = await sampleResults(page, t.id);
    if (optionNames(sr).length > 0) {
      configured.push({ test: t, sr });
    }
    if (configured.length >= 2) {
      const [a, b] = configured;
      const aNames = optionNames(a.sr).join("|");
      const bNames = optionNames(b.sr).join("|");
      if (aNames !== bNames) {
        return { target: a, source: b };
      }
      configured.pop();
    }
  }
  return null;
};

test.describe("Test Catalog editor actions report what happened (OGC-1234)", () => {
  test.beforeEach(() => test.setTimeout(240_000));

  test("Copy from test asks first, stages the source's configuration, and Save replaces this test's", async ({
    page,
  }) => {
    const pair = await pickCopyPair(page);
    test.skip(!pair, "needs two tests with different select-list options");
    const { target, source } = pair!;
    const headers = { "X-CSRF-Token": await csrfToken(page) };
    const original = target.sr;
    const sourceNames = optionNames(source.sr);

    try {
      await page.goto(
        `/MasterListsPage/TestCatalogEditor/${target.test.id}/sample-results`,
        { waitUntil: "domcontentloaded" },
      );
      const picker = page.getByRole("combobox", {
        name: "Copy configuration from test",
      });
      await expect(picker).toBeVisible({ timeout: NAV_TIMEOUT });
      await picker.fill(source.test.value);
      await page
        .getByRole("option", { name: source.test.value, exact: true })
        .click();
      await page.getByRole("button", { name: "Copy from test" }).click();

      const dialog = page.getByRole("dialog", {
        name: "Replace this test's result configuration?",
      });
      await expect(dialog).toBeVisible({ timeout: UI_TIMEOUT });
      await expect(dialog).toContainText(
        `will be replaced with those of ${source.test.value}`,
      );

      await dialog.getByRole("button", { name: "Cancel" }).click();
      await expect(dialog).toBeHidden({ timeout: UI_TIMEOUT });
      await expect(page.getByTestId("copy-staged-warning")).toHaveCount(0);

      await page.getByRole("button", { name: "Copy from test" }).click();
      await dialog
        .getByRole("button", { name: /Replace configuration/ })
        .click();
      await expect(page.getByTestId("copy-staged-warning")).toContainText(
        `Configuration copied from ${source.test.value}, not saved yet`,
        { timeout: UI_TIMEOUT },
      );
      expect(
        optionNames(await sampleResults(page, target.test.id)),
        "staging writes nothing",
      ).toEqual(optionNames(original));

      await page
        .getByRole("button", { name: "Discard copied configuration" })
        .click();
      await expect(page.getByTestId("copy-staged-warning")).toHaveCount(0, {
        timeout: UI_TIMEOUT,
      });

      await picker.fill(source.test.value);
      await page
        .getByRole("option", { name: source.test.value, exact: true })
        .click();
      await page.getByRole("button", { name: "Copy from test" }).click();
      await dialog
        .getByRole("button", { name: /Replace configuration/ })
        .click();
      await expect(page.getByTestId("copy-staged-warning")).toBeVisible({
        timeout: UI_TIMEOUT,
      });
      const saved = page.waitForResponse(
        (r) =>
          r.url().endsWith(`/tests/${target.test.id}/sample-results`) &&
          r.request().method() === "PUT",
      );
      await page.getByRole("button", { name: "Save", exact: true }).click();
      await saved;
      await expect(page.getByTestId("copy-staged-warning")).toHaveCount(0, {
        timeout: UI_TIMEOUT,
      });

      expect(optionNames(await sampleResults(page, target.test.id))).toEqual(
        sourceNames,
      );
      expect(optionNames(await sampleResults(page, source.test.id))).toEqual(
        sourceNames,
      );
    } finally {
      const restore = {
        testId: target.test.id,
        components: original.components.map((c) => ({
          ...c,
          options: c.options.map(({ id: _id, ...o }) => o),
          interpretations: c.interpretations.map(({ id: _id, ...i }) => i),
        })),
      };
      await page.request.put(
        `${API}/rest/test-catalog/tests/${target.test.id}/sample-results`,
        { headers, data: restore },
      );
    }
  });

  test("copy-from an unknown source test is a 404", async ({ page }) => {
    const headers = { "X-CSRF-Token": await csrfToken(page) };
    const tests = (await (
      await page.request.get(`${API}/rest/test-list`)
    ).json()) as TestOption[];
    const response = await page.request.post(
      `${API}/rest/test-catalog/tests/${tests[0].id}/sample-results/copy-from/999999999`,
      { headers, data: {} },
    );
    expect(response.status()).toBe(404);
  });

  test("Methods: a duplicate code is an error, and a copy says what it did", async ({
    page,
  }) => {
    const tests = (await (
      await page.request.get(`${API}/rest/test-list`)
    ).json()) as TestOption[];
    const [first, second] = tests;
    const code = `E${run}`;

    const createMethod = async (testId: string, name: string) => {
      await page.goto(`/MasterListsPage/TestCatalogEditor/${testId}/methods`, {
        waitUntil: "domcontentloaded",
      });
      await page
        .getByRole("button", { name: "+ Create New Method" })
        .click({ timeout: NAV_TIMEOUT });
      await page.getByLabel("Method Name (English)").fill(name);
      await page.getByLabel("Method Name (French)").fill(name);
      await page.getByLabel("Shortcode").fill(code);
      await page.locator("#inline-effective-date").fill("2026-09-24");
      await page.locator("#inline-effective-date").press("Enter");
      await page.getByRole("button", { name: "Create & Link" }).click();
    };

    // method.name is varchar(20)
    await createMethod(first.id, `E2E M ${run}`);
    await expect(
      page
        .getByTestId("methods-section")
        .getByText("Method created and linked."),
    ).toBeVisible({ timeout: UI_TIMEOUT });
    await expect(
      page.getByRole("cell", { name: code, exact: true }),
    ).toBeVisible();

    await createMethod(second.id, `E2E D ${run}`);
    await expect(
      page
        .locator(".cds--inline-notification--error")
        .filter({ hasText: "A method with this code already exists" }),
    ).toBeVisible({ timeout: UI_TIMEOUT });
    await expect(
      page.getByRole("button", { name: "Create & Link" }),
    ).toBeVisible();
    await expect(
      page.getByRole("cell", { name: code, exact: true }),
    ).toHaveCount(0);

    const linkedIds = async (testId: string): Promise<string[]> =>
      (
        (await (
          await page.request.get(`${API}/rest/test/${testId}/methods`)
        ).json()) as { methodId: string }[]
      ).map((l) => l.methodId);
    const onTarget = new Set(await linkedIds(second.id));
    const toCopy = (await linkedIds(first.id)).filter(
      (id) => !onTarget.has(id),
    ).length;
    expect(toCopy, "the source has the new method to copy").toBeGreaterThan(0);

    const picker = page.getByRole("combobox", {
      name: "Copy methods from test",
    });
    await picker.fill(first.value);
    await page.getByRole("option", { name: first.value, exact: true }).click();
    await page.getByRole("button", { name: "Copy from Test" }).click();
    await expect(
      page.getByText(
        `From ${first.value}: ${toCopy} ${toCopy === 1 ? "method" : "methods"} copied.`,
      ),
    ).toBeVisible({ timeout: UI_TIMEOUT });
    await expect(
      page.getByRole("cell", { name: code, exact: true }),
    ).toBeVisible();

    await picker.fill(first.value);
    await page.getByRole("option", { name: first.value, exact: true }).click();
    await page.getByRole("button", { name: "Copy from Test" }).click();
    await expect(
      page.getByText(
        `No methods copied: ${first.value} has no methods that are not already linked to this test.`,
      ),
    ).toBeVisible({ timeout: UI_TIMEOUT });
  });

  test("Sample Type create with markup in the name is refused, not reported as saved", async ({
    page,
  }) => {
    const headers = { "X-CSRF-Token": await csrfToken(page) };
    const name = `E2E<b>${run}</b>`;

    await page.goto("/MasterListsPage/SampleTypeEditor/new/basic-info", {
      waitUntil: "domcontentloaded",
    });
    const nameField = page.locator("#st-name");
    await expect(nameField).toBeVisible({ timeout: NAV_TIMEOUT });
    await nameField.fill(name);
    await page.locator("#st-description").fill(`E2E description ${run}`);
    const posted = page.waitForResponse(
      (r) =>
        r.url().endsWith("/rest/SampleTypeCreate") &&
        r.request().method() === "POST",
    );
    await page.getByRole("button", { name: "Create Sample Type" }).click();
    await posted;

    const refusal =
      "This name contains characters that are not allowed, such as HTML tags. Remove them and try again.";
    await expect(
      page.getByText(`Failed to create sample type: ${refusal}`),
    ).toBeVisible({ timeout: UI_TIMEOUT });
    await expect(page).toHaveURL(/SampleTypeEditor\/new\/basic-info/);
    await expect(nameField).toHaveAttribute("aria-invalid", "true");

    const api = await page.request.post(`${API}/rest/SampleTypeCreate`, {
      headers,
      data: {
        sampleTypeEnglishName: name,
        sampleTypeFrenchName: name,
        domain: "CLINICAL",
        active: true,
      },
    });
    expect(api.status()).toBe(400);
    const body = await api.json();
    expect(body.error).toBe("validation");
    expect(body.fieldErrors.map((f: { field: string }) => f.field)).toContain(
      "sampleTypeEnglishName",
    );
  });

  test("Sample Types sharing a ten-character prefix are both created; a taken name is refused on the name field", async ({
    page,
  }) => {
    const headers = { "X-CSRF-Token": await csrfToken(page) };
    const first = `E2ESTP${run} Venous`;
    const second = `E2ESTP${run} Arterial`;

    const created = await page.request.post(`${API}/rest/SampleTypeCreate`, {
      headers,
      data: {
        sampleTypeEnglishName: first,
        sampleTypeFrenchName: first,
        domain: "CLINICAL",
      },
    });
    expect(created.status()).toBe(200);

    const createInUi = async (name: string) => {
      await page.goto("/MasterListsPage/SampleTypeEditor/new/basic-info", {
        waitUntil: "domcontentloaded",
      });
      await expect(page.locator("#st-name")).toBeVisible({
        timeout: NAV_TIMEOUT,
      });
      await page.locator("#st-name").fill(name);
      await page.locator("#st-description").fill(name);
      const posted = page.waitForResponse(
        (r) =>
          r.url().endsWith("/rest/SampleTypeCreate") &&
          r.request().method() === "POST",
      );
      await page.getByRole("button", { name: "Create Sample Type" }).click();
      await posted;
    };

    await createInUi(second);
    await expect(page).toHaveURL(/SampleTypeEditor\/\d+\/basic-info/, {
      timeout: UI_TIMEOUT,
    });
    await expect(page.locator("#st-name")).toHaveValue(second);

    await page.goto("/MasterListsPage/SampleTypeEditor/new/basic-info", {
      waitUntil: "domcontentloaded",
    });
    await expect(page.locator("#st-name")).toBeVisible({
      timeout: NAV_TIMEOUT,
    });
    const third = `E2ESTP${run} Capillary`;
    await page.locator("#st-name").fill(third);
    await page.locator("#st-description").fill(third);
    const raced = await page.request.post(`${API}/rest/SampleTypeCreate`, {
      headers,
      data: {
        sampleTypeEnglishName: third,
        sampleTypeFrenchName: third,
        domain: "CLINICAL",
      },
    });
    expect(raced.status()).toBe(200);
    const refused = page.waitForResponse(
      (r) =>
        r.url().endsWith("/rest/SampleTypeCreate") &&
        r.request().method() === "POST",
    );
    await page.getByRole("button", { name: "Create Sample Type" }).click();
    await refused;
    await expect(page.locator("#st-name")).toHaveAttribute(
      "aria-invalid",
      "true",
    );
    await expect(
      page.getByText("A sample type with this name already exists.").first(),
    ).toBeVisible({ timeout: UI_TIMEOUT });
    await expect(page).toHaveURL(/SampleTypeEditor\/new\/basic-info/);

    const list = await page.request.get(`${API}/rest/sample-types`);
    const rows = (
      (await list.json()).data as {
        description: string;
        abbreviation: string;
      }[]
    ).filter((row) => row.description.startsWith(`E2ESTP${run}`));
    expect(rows).toHaveLength(3);
    expect(new Set(rows.map((row) => row.abbreviation)).size).toBe(3);
  });

  test("a panel description may repeat another's; a rename onto another panel's name is refused", async ({
    page,
  }) => {
    const headers = { "X-CSRF-Token": await csrfToken(page) };
    const nameA = `E2E PA ${run}`;
    const nameB = `E2E PB ${run}`;
    const create = async (name: string) => {
      const response = await page.request.post(
        `${API}/rest/test-catalog/panels`,
        { headers, data: { name, active: false } },
      );
      expect(response.status()).toBe(201);
      return (await response.json()).id as string;
    };
    await create(nameA);
    const idB = await create(nameB);

    await page.goto(
      `/MasterListsPage/TestCatalogEditor/panel/${idB}/basic-info`,
      {
        waitUntil: "domcontentloaded",
      },
    );
    await expect(page.locator("#panel-name")).toHaveValue(nameB, {
      timeout: NAV_TIMEOUT,
    });

    await page.locator("#panel-description").fill(nameA);
    await page.getByRole("button", { name: "Save", exact: true }).click();
    await expect(
      page
        .locator(".cds--toast-notification")
        .filter({ hasText: "Successfully Added/Edited" }),
    ).toBeVisible({ timeout: UI_TIMEOUT });
    await expect
      .poll(
        async () =>
          (
            await (
              await page.request.get(`${API}/rest/test-catalog/panels/${idB}`)
            ).json()
          ).description,
        { timeout: UI_TIMEOUT },
      )
      .toBe(nameA);

    await page.locator("#panel-name").fill(nameA.toLowerCase());
    await page.getByRole("button", { name: "Save", exact: true }).click();
    await expect(
      page
        .locator(".cds--toast-notification")
        .filter({ hasText: "Another panel already uses this name" }),
    ).toBeVisible({ timeout: UI_TIMEOUT });
    const stored = await (
      await page.request.get(`${API}/rest/test-catalog/panels/${idB}`)
    ).json();
    expect(stored.name).toBe(nameB);
  });
});
