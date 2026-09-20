import { test, expect, Page } from "../../../helpers/test-base";
import { NAV_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";

/**
 * OGC-1120 — the order-entry test list (/rest/sample-type-tests) answered 500
 * for a missing or malformed sampleType, and 500 for a whole sample type once
 * one of its active tests had no lab unit. The first is a client error and is
 * now answered as one; the second can no longer be produced, because the
 * editor refuses to activate a test that has no lab unit and says why.
 */

const API = "/api/OpenELIS-Global";
const CATALOG = `${API}/rest/test-catalog`;
const SERUM_SAMPLE_TYPE_ID = "2";

interface Probe {
  status: number;
  text: string;
}

async function probe(page: Page, query: string): Promise<Probe> {
  return page.evaluate(
    async ({ api, query }) => {
      const res = await fetch(`${api}/rest/sample-type-tests${query}`, {
        credentials: "include",
      });
      return { status: res.status, text: await res.text() };
    },
    { api: API, query },
  );
}

interface CreatedTest {
  testId: string;
  labUnitId: string;
}

/** Creates an inactive Serum test that deliberately has no lab unit. */
async function createTestWithoutLabUnit(page: Page): Promise<CreatedTest> {
  const stamp = Date.now().toString().slice(-8);
  const name = `OGC1120 NoUnit ${stamp}`;
  const result = await page.evaluate(
    async ({ catalog, name, stamp, sampleTypeId }) => {
      const csrf = localStorage.getItem("CSRF") || "";
      const headers = {
        "Content-Type": "application/json",
        "X-CSRF-Token": csrf,
      };
      const labUnitsRes = await fetch(`${catalog}/lab-units`, {
        credentials: "include",
        headers,
      });
      const labUnits = await labUnitsRes.json();
      const labUnitId = Array.isArray(labUnits) && labUnits[0]?.id;
      const created = await fetch(`${catalog}/tests`, {
        method: "POST",
        credentials: "include",
        headers,
        body: JSON.stringify({
          name,
          reportingName: name,
          code: `T1120${stamp}`,
          sampleTypeIds: [sampleTypeId],
          domain: "CLINICAL",
          orderable: true,
        }),
      });
      const createdText = await created.text();
      if (!created.ok) {
        return { error: `create ${created.status}: ${createdText}` };
      }
      const testId = JSON.parse(createdText).testId as string;
      const configured = await fetch(
        `${catalog}/tests/${testId}/sample-results`,
        {
          method: "PUT",
          credentials: "include",
          headers,
          body: JSON.stringify({
            testId,
            components: [
              {
                code: "PRIMARY",
                label: name,
                displayOrder: 0,
                resultType: "N",
                isPrimary: true,
                showOnReport: true,
                interpretations: [],
                options: [],
              },
            ],
          }),
        },
      );
      if (!configured.ok) {
        return {
          error: `sample-results ${configured.status}: ${await configured.text()}`,
        };
      }
      return { testId, labUnitId };
    },
    { catalog: CATALOG, name, stamp, sampleTypeId: SERUM_SAMPLE_TYPE_ID },
  );
  expect(result.error, "test setup must succeed").toBeUndefined();
  return { testId: result.testId as string, labUnitId: result.labUnitId };
}

test.describe("OGC-1120 sample-type test list guards", () => {
  test("a missing or malformed sampleType is a client error; a real one still lists tests", async ({
    page,
  }) => {
    await page.goto("/", { waitUntil: "domcontentloaded" });

    const missing = await probe(page, "");
    expect(missing.status).toBe(400);
    expect(missing.text).toContain("sampleType");

    expect((await probe(page, "?sampleType=abc")).status).toBe(400);
    expect((await probe(page, "?sampleType=null")).status).toBe(400);

    const serum = await probe(page, `?sampleType=${SERUM_SAMPLE_TYPE_ID}`);
    expect(serum.status).toBe(200);
    expect(JSON.parse(serum.text).tests.length).toBeGreaterThan(0);

    const unknown = await probe(page, "?sampleType=999999");
    expect(unknown.status).toBe(200);
    expect(JSON.parse(unknown.text).tests).toEqual([]);
  });

  test("a test without a lab unit cannot be activated, and the editor says why", async ({
    page,
  }) => {
    test.setTimeout(90_000);
    await page.goto("/", { waitUntil: "domcontentloaded" });
    const created = await createTestWithoutLabUnit(page);

    await test.step("Activation is refused with the lab unit named", async () => {
      const activation = await page.evaluate(
        async ({ catalog, testId }) => {
          const res = await fetch(`${catalog}/tests/${testId}/activate`, {
            method: "POST",
            credentials: "include",
            headers: {
              "Content-Type": "application/json",
              "X-CSRF-Token": localStorage.getItem("CSRF") || "",
            },
            body: "{}",
          });
          return { status: res.status, text: await res.text() };
        },
        { catalog: CATALOG, testId: created.testId },
      );
      expect(activation.status).toBe(422);
      expect(JSON.parse(activation.text).missing).toContain("NO_LAB_UNIT");
    });

    await test.step("Basic Info lists the missing lab unit in the checklist", async () => {
      await page.goto(
        `/MasterListsPage/TestCatalogEditor/${created.testId}/basic-info`,
        { waitUntil: "domcontentloaded" },
      );
      const checklist = page.getByTestId("completeness-checklist");
      await expect(checklist).toBeVisible({ timeout: NAV_TIMEOUT });
      await expect(checklist).toContainText(/lab unit/i, {
        timeout: UI_TIMEOUT,
      });
    });

    await test.step("With a lab unit set, the same test activates and is orderable", async () => {
      const outcome = await page.evaluate(
        async ({ catalog, api, testId, labUnitId, sampleTypeId }) => {
          const headers = {
            "Content-Type": "application/json",
            "X-CSRF-Token": localStorage.getItem("CSRF") || "",
          };
          const basicInfoRes = await fetch(
            `${catalog}/tests/${testId}/basic-info`,
            { credentials: "include", headers },
          );
          const basicInfo = await basicInfoRes.json();
          const saved = await fetch(`${catalog}/tests/${testId}/basic-info`, {
            method: "PUT",
            credentials: "include",
            headers,
            body: JSON.stringify({ ...basicInfo, labUnitId }),
          });
          if (!saved.ok) {
            return {
              error: `basic-info ${saved.status}: ${await saved.text()}`,
            };
          }
          const activated = await fetch(`${catalog}/tests/${testId}/activate`, {
            method: "POST",
            credentials: "include",
            headers,
            body: "{}",
          });
          const listed = await fetch(
            `${api}/rest/sample-type-tests?sampleType=${sampleTypeId}`,
            { credentials: "include" },
          );
          const list = await listed.json();
          return {
            activated: activated.status,
            listStatus: listed.status,
            listed: (list.tests || []).some(
              (t: { id: string }) => String(t.id) === String(testId),
            ),
          };
        },
        {
          catalog: CATALOG,
          api: API,
          testId: created.testId,
          labUnitId: created.labUnitId,
          sampleTypeId: SERUM_SAMPLE_TYPE_ID,
        },
      );
      expect(outcome.error).toBeUndefined();
      expect(outcome.activated).toBe(200);
      expect(outcome.listStatus).toBe(200);
      expect(outcome.listed, "the activated test must reach order entry").toBe(
        true,
      );
    });
  });
});
