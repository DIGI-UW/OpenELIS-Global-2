import type { Page } from "@playwright/test";
import { test, expect } from "../../../helpers/test-base";
import { NAV_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";

/**
 * OGC-1465: the server applies the rules the Test Catalog forms apply.
 *
 * - Storage refuses unknown conditions and negative durations, and the form
 *   names the field.
 * - Reagent quantities cannot be negative.
 * - The English name cannot be blanked.
 * - Terminology codes are stored trimmed.
 * - A sample type keeps its Display Order position through a Basic Info save,
 *   refuses an unknown domain, and refuses a test of another domain.
 *
 * Every record the spec creates is retired.
 */

const API = "/api/OpenELIS-Global";
const TC = `${API}/rest/test-catalog`;

const csrfToken = async (page: Page): Promise<string> => {
  const state = await page.context().storageState();
  for (const origin of state.origins) {
    for (const item of origin.localStorage) {
      if (item.name === "CSRF") return item.value;
    }
  }
  return "";
};

const seedTest = async (
  page: Page,
  headers: Record<string, string>,
  domain = "CLINICAL",
) => {
  const s = `${Date.now()}`.slice(-7);
  const units = (
    await (await page.request.get(`${API}/rest/lab-units-management`)).json()
  ).data as { id: string; domain: string; isActive: boolean }[];
  const types = (
    await (await page.request.get(`${API}/rest/sample-types`)).json()
  ).data as { id: string; domain: string; isActive: boolean }[];
  const created = await page.request.post(`${TC}/tests`, {
    headers,
    data: {
      name: `PW1465 ${s}`,
      reportingName: `PW1465 ${s}`,
      code: `P65${s}`,
      domain,
      labUnitId: units.find((u) => u.isActive)!.id,
      sampleTypeIds: [
        String(types.find((t) => t.isActive && t.domain === domain)!.id),
      ],
    },
  });
  expect(created.status(), await created.text()).toBe(201);
  return String((await created.json()).testId);
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

test.describe("The server applies the Test Catalog's data rules (OGC-1465)", () => {
  test.beforeEach(() => test.setTimeout(180_000));

  test("Storage refuses what the form would refuse, and names the field", async ({
    page,
  }) => {
    await page.goto("/");
    const headers = { "X-CSRF-Token": await csrfToken(page) };
    const testId = await seedTest(page, headers);
    try {
      for (const storage of [
        { storageCondition: "BOGUS" },
        { storageCondition: "frozen" },
        { storageDuration: -3 },
      ]) {
        const refused = await page.request.put(
          `${TC}/tests/${testId}/storage`,
          {
            headers,
            data: storage,
          },
        );
        expect(refused.status()).toBe(422);
      }

      await page.goto(`/MasterListsPage/TestCatalogEditor/${testId}/storage`, {
        waitUntil: "domcontentloaded",
      });
      const duration = page.locator("#storage-duration");
      await expect(duration).toBeVisible({ timeout: NAV_TIMEOUT });
      await duration.fill("-3");
      await page
        .getByRole("button", { name: /^save$/i })
        .last()
        .click();
      await expect(
        page.getByText(/Maximum storage duration: this value is not accepted/),
      ).toBeVisible({ timeout: UI_TIMEOUT });
    } finally {
      await retire(page, headers, testId);
    }
  });

  test("reagent quantities, the English name and terminology codes", async ({
    page,
  }) => {
    await page.goto("/");
    const headers = { "X-CSRF-Token": await csrfToken(page) };
    const testId = await seedTest(page, headers);
    try {
      const reagents = (await (
        await page.request.get(`${API}/rest/inventory/items`)
      ).json()) as { id: string }[];
      if (reagents.length > 0) {
        const negative = await page.request.post(`${TC}/${testId}/reagents`, {
          headers,
          data: {
            reagentId: reagents[0].id,
            usageType: "PRIMARY",
            quantityPerTest: -1,
            quantityUnit: "mL",
          },
        });
        expect(negative.status()).toBe(422);
      }

      const refs = await (
        await page.request.get(`${TC}/tests/${testId}/localization`)
      ).json();
      const nameId = refs.fields.find(
        (f: { field: string }) => f.field === "name",
      ).localizationId;
      const blank = await page.request.put(
        `${API}/rest/localizations/${nameId}/translations`,
        { headers, data: { en: " " } },
      );
      expect(blank.status()).toBe(400);
      expect(
        (
          await (
            await page.request.get(`${API}/rest/localizations/${nameId}`)
          ).json()
        ).translations.en,
      ).toMatch(/^PW1465 /);

      expect(
        (
          await page.request.put(`${TC}/tests/${testId}/terminology`, {
            headers,
            data: {
              testId,
              mappings: [
                { source: "LOINC", code: "  2345-7 ", relationship: "SAME_AS" },
              ],
            },
          })
        ).status(),
      ).toBe(200);
      const stored = await (
        await page.request.get(`${TC}/tests/${testId}/terminology`)
      ).json();
      expect(stored.mappings[0].code).toBe("2345-7");
    } finally {
      await retire(page, headers, testId);
    }
  });

  test("sample types keep their position and their domain rule", async ({
    page,
  }) => {
    await page.goto("/");
    const headers = { "X-CSRF-Token": await csrfToken(page) };
    const types = (
      await (await page.request.get(`${API}/rest/sample-types`)).json()
    ).data as {
      id: string;
      name: string;
      description: string;
      domain: string;
      isActive: boolean;
      sortOrder: number;
    }[];
    const clinical = types.find((t) => t.isActive && t.domain === "CLINICAL")!;
    const vectorTest = await seedTest(page, headers, "VECTOR");
    try {
      const saved = await page.request.put(
        `${API}/rest/sample-types/${clinical.id}`,
        {
          headers,
          data: {
            id: clinical.id,
            name: clinical.name,
            description: clinical.description,
            domain: clinical.domain,
            isActive: true,
            sortOrder: clinical.sortOrder + 50,
          },
        },
      );
      expect(saved.status()).toBe(200);
      expect(
        (
          await (
            await page.request.get(`${API}/rest/sample-types/${clinical.id}`)
          ).json()
        ).data.sortOrder,
        "the position belongs to Display Order",
      ).toBe(clinical.sortOrder);

      const unknownDomain = await page.request.put(
        `${API}/rest/sample-types/${clinical.id}`,
        { headers, data: { id: clinical.id, domain: "XYZ" } },
      );
      expect(unknownDomain.status()).toBe(422);

      const crossDomain = await page.request.put(
        `${API}/rest/sample-types/${clinical.id}/tests/${vectorTest}`,
        { headers, data: {} },
      );
      expect(crossDomain.status()).toBe(422);
    } finally {
      await retire(page, headers, vectorTest);
    }
  });
});
