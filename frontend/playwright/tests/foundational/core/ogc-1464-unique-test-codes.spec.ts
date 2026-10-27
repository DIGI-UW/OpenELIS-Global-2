import type { Page } from "@playwright/test";
import { test, expect } from "../../../helpers/test-base";
import { NAV_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";

/**
 * OGC-1464: test codes are trimmed and unique, compared without case.
 *
 * - A create whose code differs only by spaces or case from an existing one is
 *   refused, and so is a name that differs only by a leading space.
 * - Basic Info cannot give a test another test's code; the field says why.
 * - Two creates with one code at the same moment make one test.
 * - A taken method name is reported as the name, not the code.
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

const createTest = async (
  page: Page,
  headers: Record<string, string>,
  name: string,
  code: string,
) => {
  const labUnits = await (await page.request.get(`${TC}/lab-units`)).json();
  return page.request.post(`${TC}/tests`, {
    headers,
    data: {
      name,
      reportingName: name,
      code,
      domain: "CLINICAL",
      labUnitId: labUnits[0].id,
      sampleTypeIds: ["2"],
    },
  });
};

const retire = async (
  page: Page,
  headers: Record<string, string>,
  testIds: string[],
) => {
  for (const testId of testIds) {
    const info = await (
      await page.request.get(`${TC}/tests/${testId}/basic-info`)
    ).json();
    await page.request.put(`${TC}/tests/${testId}/basic-info`, {
      headers,
      data: { ...info, active: false, orderable: false },
    });
  }
};

test.describe("Test codes are trimmed and unique (OGC-1464)", () => {
  test.beforeEach(() => test.setTimeout(180_000));

  test("a code or name that differs only by spaces or case is refused", async ({
    page,
  }) => {
    await page.goto("/");
    const headers = { "X-CSRF-Token": await csrfToken(page) };
    const s = `${Date.now()}`.slice(-6);
    const created = await createTest(
      page,
      headers,
      `PW1464 ${s}`,
      ` PW64${s} `,
    );
    expect(created.status()).toBe(201);
    const testId = String((await created.json()).testId);
    try {
      expect(
        (
          await (
            await page.request.get(`${TC}/tests/${testId}/basic-info`)
          ).json()
        ).code,
        "stored trimmed",
      ).toBe(`PW64${s}`);
      expect(
        (await createTest(page, headers, `PW1464 b ${s}`, `pw64${s}`)).status(),
      ).toBe(409);
      expect(
        (await createTest(page, headers, ` PW1464 ${s}`, `PW64c${s}`)).status(),
      ).toBe(409);
    } finally {
      await retire(page, headers, [testId]);
    }
  });

  test("Basic Info refuses another test's code on the code field", async ({
    page,
  }) => {
    await page.goto("/");
    const headers = { "X-CSRF-Token": await csrfToken(page) };
    const s = `${Date.now()}`.slice(-6);
    const a = await createTest(page, headers, `PW1464 a ${s}`, `PW64A${s}`);
    const b = await createTest(page, headers, `PW1464 b ${s}`, `PW64B${s}`);
    expect(a.status()).toBe(201);
    expect(b.status()).toBe(201);
    const aId = String((await a.json()).testId);
    const bId = String((await b.json()).testId);
    try {
      await page.goto(`/MasterListsPage/TestCatalogEditor/${bId}/basic-info`, {
        waitUntil: "domcontentloaded",
      });
      const code = page.locator("#basic-info-code");
      await expect(code).toHaveValue(`PW64B${s}`, { timeout: NAV_TIMEOUT });
      await code.fill(` pw64a${s} `);
      const saved = page.waitForResponse(
        (r) =>
          r.url().endsWith(`/tests/${bId}/basic-info`) &&
          r.request().method() === "PUT",
      );
      await page
        .getByRole("button", { name: /^save$/i })
        .last()
        .click();
      expect((await saved).status()).toBe(409);

      await expect(code).toHaveAttribute("aria-invalid", "true", {
        timeout: UI_TIMEOUT,
      });
      expect(
        (await (await page.request.get(`${TC}/tests/${bId}/basic-info`)).json())
          .code,
      ).toBe(`PW64B${s}`);
    } finally {
      await retire(page, headers, [aId, bId]);
    }
  });

  test("two creates with one code at the same moment make one test", async ({
    page,
  }) => {
    await page.goto("/");
    const headers = { "X-CSRF-Token": await csrfToken(page) };
    const s = `${Date.now()}`.slice(-6);
    const responses = await Promise.all([
      createTest(page, headers, `PW1464 race a ${s}`, `PW64R${s}`),
      createTest(page, headers, `PW1464 race b ${s}`, `PW64R${s}`),
    ]);
    const statuses = responses.map((r) => r.status()).sort();
    const ids: string[] = [];
    for (const r of responses) {
      if (r.status() === 201) ids.push(String((await r.json()).testId));
    }
    try {
      expect(statuses).toEqual([201, 409]);
    } finally {
      await retire(page, headers, ids);
    }
  });

  test("a taken method name is reported as the name", async ({ page }) => {
    await page.goto("/");
    const headers = { "X-CSRF-Token": await csrfToken(page) };
    const s = `${Date.now()}`.slice(-6);
    const created = await createTest(
      page,
      headers,
      `PW1464 m ${s}`,
      `PW64M${s}`,
    );
    const testId = String((await created.json()).testId);
    const inline = (name: string, code: string) =>
      page.request.post(`${API}/rest/test/${testId}/methods/inline-create`, {
        headers,
        data: {
          nameEnglish: name,
          nameFrench: name,
          code,
          isDefault: false,
          effectiveDate: "2026-10-13",
        },
      });
    try {
      expect((await inline(`PW64 ${s}`, `PWA${s}`)).status()).toBe(201);
      const taken = await inline(`PW64 ${s}`, `PWB${s}`);
      expect(taken.status()).toBe(409);
      expect((await taken.json()).field).toBe("name");
    } finally {
      await retire(page, headers, [testId]);
    }
  });
});
