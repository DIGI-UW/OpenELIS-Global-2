import type { Page } from "@playwright/test";
import { test, expect } from "../../../helpers/test-base";
import { NAV_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";

/**
 * OGC-1467: the Test Catalog refuses input it cannot store with a 4xx that
 * names the problem, never a 500.
 *
 * - Values longer than their column, unknown units, roles and answers.
 * - Lists sent as null, or left out of a save that would clear them.
 * - References that do not exist on create, which creates nothing.
 * - Paging past the end, non-numeric filters and non-numeric ids.
 * - Two first Storage saves of one test at the same moment.
 * - The sample type form says a name or description is too long.
 *
 * Every test the spec creates is retired.
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

const seedTest = async (page: Page, headers: Record<string, string>) => {
  const s = `${Date.now()}`.slice(-7);
  const units = (
    await (await page.request.get(`${API}/rest/lab-units-management`)).json()
  ).data as { id: string; isActive: boolean }[];
  const types = (
    await (await page.request.get(`${API}/rest/sample-types`)).json()
  ).data as { id: string; domain: string; isActive: boolean }[];
  const created = await page.request.post(`${TC}/tests`, {
    headers,
    data: {
      name: `PW1467 ${s}`,
      reportingName: `PW1467 ${s}`,
      code: `P67${s}`,
      domain: "CLINICAL",
      labUnitId: units.find((u) => u.isActive)!.id,
      sampleTypeIds: [
        String(types.find((t) => t.isActive && t.domain === "CLINICAL")!.id),
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

test.describe("The Test Catalog refuses input it cannot store (OGC-1467)", () => {
  test.beforeEach(() => test.setTimeout(180_000));

  test("values too long, unknown references and null lists are refused", async ({
    page,
  }) => {
    await page.goto("/");
    const headers = { "X-CSRF-Token": await csrfToken(page) };
    const testId = await seedTest(page, headers);
    try {
      const sr = await (
        await page.request.get(`${TC}/tests/${testId}/sample-results`)
      ).json();
      const primary = { ...sr.components[0], resultType: "N" };
      for (const [change, refusal] of [
        [{ label: "L".repeat(256) }, "value.tooLong"],
        [{ defaultResult: "D".repeat(81) }, "value.tooLong"],
        [{ uomId: "abc" }, "unit.unknown"],
        [{ uomId: "99999999" }, "unit.unknown"],
      ] as const) {
        const refused = await page.request.put(
          `${TC}/tests/${testId}/sample-results`,
          {
            headers,
            data: { ...sr, components: [{ ...primary, ...change }] },
          },
        );
        expect(refused.status()).toBe(422);
        expect((await refused.json()).refusal).toBe(refusal);
      }
      for (const data of [
        { ...sr, components: null },
        { ...sr, components: [{ ...primary, options: null }] },
      ]) {
        const refused = await page.request.put(
          `${TC}/tests/${testId}/sample-results`,
          { headers, data },
        );
        expect(refused.status()).toBe(400);
      }

      const terminology = await page.request.put(
        `${TC}/tests/${testId}/terminology`,
        {
          headers,
          data: {
            mappings: [
              {
                source: "LOINC",
                code: "1".repeat(81),
                relationship: "SAME_AS",
              },
            ],
          },
        },
      );
      expect(terminology.status()).toBe(422);
      expect(
        (
          await page.request.put(`${TC}/tests/${testId}/ranges`, {
            headers,
            data: { testId },
          })
        ).status(),
        "a ranges save without its list no longer clears every range",
      ).toBe(400);

      const alert = await page.request.post(`${TC}/${testId}/alerts`, {
        headers,
        data: { name: "PW1467", triggerType: "ALL", notifyRoleId: "abc" },
      });
      expect(alert.status()).toBe(400);
      const qc = await page.request.put(`${TC}/tests/${testId}/qc-targets`, {
        headers,
        data: {
          targets: [
            {
              controlLevel: "NORMAL",
              expectedValue: 12345678901,
              uncertainty: 1,
              active: true,
            },
          ],
        },
      });
      expect(qc.status()).toBe(422);
    } finally {
      await retire(page, headers, testId);
    }
  });

  test("create checks every reference first; lists and ids never 500", async ({
    page,
  }) => {
    await page.goto("/");
    const headers = { "X-CSRF-Token": await csrfToken(page) };
    const s = `${Date.now()}`.slice(-6);
    for (const [i, over] of [
      { sampleTypeIds: ["99999999"] },
      { sampleTypeIds: ["abc"] },
      { labUnitId: "99999999" },
      { copyFromId: "x" },
    ].entries()) {
      const refused = await page.request.post(`${TC}/tests`, {
        headers,
        data: {
          name: `PW1467 bad ${i} ${s}`,
          reportingName: `PW1467 bad ${i} ${s}`,
          code: `P6X${i}${s}`,
          domain: "CLINICAL",
          sampleTypeIds: ["1"],
          ...over,
        },
      });
      expect(refused.status()).toBe(422);
    }
    const leftovers = (
      await (
        await page.request.get(
          `${TC}/tests?status=all&page=1&pageSize=50&search=${encodeURIComponent(`PW1467 bad`)}`,
        )
      ).json()
    ).rows as { name: string }[];
    expect(leftovers.filter((r) => r.name.includes(s))).toEqual([]);

    for (const query of [
      "page=1073741825&pageSize=2",
      "page=2147483647&pageSize=2147483647",
      "sampleType=abc",
    ]) {
      const listed = await page.request.get(`${TC}/tests?${query}`);
      expect(listed.status(), query).toBe(200);
    }
    for (const path of [
      `${API}/rest/sample-types/abc`,
      `${API}/rest/lab-units-management/abc`,
      `${API}/rest/test/abc/methods`,
      `${TC}/group/summary?ids=1,abc`,
    ]) {
      expect([400, 404], path).toContain(
        (await page.request.get(path)).status(),
      );
    }
    expect(
      (
        await page.request.put(`${TC}/sample-types/2/test-order`, {
          headers,
          data: { items: null },
        })
      ).status(),
    ).toBe(400);
  });

  test("two first Storage saves at the same moment both save", async ({
    page,
  }) => {
    await page.goto("/");
    const headers = { "X-CSRF-Token": await csrfToken(page) };
    const testId = await seedTest(page, headers);
    try {
      const [a, b] = await Promise.all(
        [3, 4].map((storageDuration) =>
          page.request.put(`${TC}/tests/${testId}/storage`, {
            headers,
            data: { storageDuration },
          }),
        ),
      );
      expect([a.status(), b.status()]).toEqual([200, 200]);
    } finally {
      await retire(page, headers, testId);
    }
  });

  test("the sample type form says a name or description is too long", async ({
    page,
  }) => {
    await page.goto("/MasterListsPage/SampleTypeEditor/new/basic-info", {
      waitUntil: "domcontentloaded",
    });
    const name = page.getByRole("textbox", { name: /Name/ }).first();
    await expect(name).toBeVisible({ timeout: NAV_TIMEOUT });
    await name.fill("n".repeat(41));
    await page.locator("#st-description").fill("d".repeat(41));
    let posted = false;
    page.on("request", (request) => {
      if (/SampleTypeCreate/.test(request.url()) && request.method() === "POST")
        posted = true;
    });
    await page.getByRole("button", { name: "Create Sample Type" }).click();

    await expect(
      page.getByText("A new sample type name has at most 40 characters."),
    ).toBeVisible({ timeout: UI_TIMEOUT });
    await expect(
      page.getByText("The description has at most 40 characters."),
    ).toBeVisible();
    expect(posted).toBe(false);
  });
});
