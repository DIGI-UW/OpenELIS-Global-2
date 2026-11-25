import { Page } from "@playwright/test";
import { test, expect } from "../../../helpers/test-base";
import { isSettingOn, setSetting } from "../../../fixtures/esig-admin";
import { createSampleOrder } from "../../../helpers/seed-tat-data";
import { NAV_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";

/**
 * OGC-1587 — a calculated-value rule could read its own result, two rules
 * could read each other, and two active rules could write one test; result
 * entry then overwrote the value the technician typed. Such rules are refused
 * with the reason on screen, a rule read from the list saves back unchanged,
 * and a value typed for a rule's test is never replaced by the rule.
 */

const API = "/api/OpenELIS-Global";
const CATALOG = `${API}/rest/test-catalog`;
const SERUM_SAMPLE_TYPE_ID = "2";
const ESIG_SETTING = "electronicSignatureEnabled";

interface NumericTest {
  testId: string;
  componentId: string;
  name: string;
}

async function createNumericTest(
  page: Page,
  label: string,
  stamp: string,
): Promise<NumericTest> {
  const name = `OGC1587 ${label} ${stamp}`;
  const result = await page.evaluate(
    async ({ catalog, name, code, sampleTypeId }) => {
      const headers = {
        "Content-Type": "application/json",
        "X-CSRF-Token": localStorage.getItem("CSRF") || "",
      };
      const labUnits = await (
        await fetch(`${catalog}/lab-units`, { credentials: "include", headers })
      ).json();
      const created = await fetch(`${catalog}/tests`, {
        method: "POST",
        credentials: "include",
        headers,
        body: JSON.stringify({
          name,
          reportingName: name,
          code,
          labUnitId: Array.isArray(labUnits) && labUnits[0]?.id,
          sampleTypeIds: [sampleTypeId],
          domain: "CLINICAL",
          orderable: true,
        }),
      });
      if (!created.ok) {
        return { error: `create ${created.status}: ${await created.text()}` };
      }
      const testId = (await created.json()).testId as string;
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
        return { error: `sample-results ${configured.status}` };
      }
      const componentId = (await configured.json()).components[0].id as string;
      const activated = await fetch(`${catalog}/tests/${testId}/activate`, {
        method: "POST",
        credentials: "include",
        headers,
        body: "{}",
      });
      if (!activated.ok) {
        return { error: `activate ${activated.status}` };
      }
      return { testId, componentId };
    },
    {
      catalog: CATALOG,
      name,
      code: `T1587${label}${stamp}`,
      sampleTypeId: SERUM_SAMPLE_TYPE_ID,
    },
  );
  expect(result.error, `test ${label} setup must succeed`).toBeUndefined();
  return {
    testId: result.testId as string,
    componentId: result.componentId as string,
    name,
  };
}

/** A rule writing `writes` as `reads` × factor, posted through the API. */
async function postRule(
  page: Page,
  body: Record<string, unknown>,
): Promise<{ status: number; body: { messageKey?: string } | null }> {
  return page.evaluate(
    async ({ api, body }) => {
      const res = await fetch(`${api}/rest/test-calculation`, {
        method: "POST",
        credentials: "include",
        headers: {
          "Content-Type": "application/json",
          "X-CSRF-Token": localStorage.getItem("CSRF") || "",
        },
        body: JSON.stringify(body),
      });
      const text = await res.text();
      return { status: res.status, body: text ? JSON.parse(text) : null };
    },
    { api: API, body },
  );
}

function rule(
  name: string,
  writes: NumericTest,
  reads: NumericTest,
  factor: string,
  active: boolean,
) {
  return {
    name,
    testId: Number(writes.testId),
    componentId: writes.componentId,
    sampleId: Number(SERUM_SAMPLE_TYPE_ID),
    active,
    toggled: active,
    note: "",
    result: "",
    operations: [
      {
        order: 0,
        type: "TEST_RESULT",
        value: reads.testId,
        sampleId: Number(SERUM_SAMPLE_TYPE_ID),
        componentId: reads.componentId,
      },
      { order: 1, type: "MATH_FUNCTION", value: "*" },
      { order: 2, type: "INTEGER", value: factor },
    ],
  };
}

async function ruleIdByName(page: Page, name: string): Promise<number> {
  const rules = await page.evaluate(async (api) => {
    const res = await fetch(`${api}/rest/test-calculations`, {
      credentials: "include",
    });
    return res.json();
  }, API);
  return rules.find((r: { name: string }) => r.name === name).id;
}

async function deactivate(page: Page, id: number) {
  await page.evaluate(
    async ({ api, id }) => {
      await fetch(`${api}/rest/deactivate-test-calculation/${id}`, {
        method: "POST",
        credentials: "include",
        headers: { "X-CSRF-Token": localStorage.getItem("CSRF") || "" },
      });
    },
    { api: API, id },
  );
}

test.describe("OGC-1587 calculated value rule integrity", () => {
  let esigWasOn = false;
  const createdRules: number[] = [];

  test.beforeEach(async ({ page }) => {
    esigWasOn = await isSettingOn(page, ESIG_SETTING);
    if (esigWasOn) {
      await setSetting(page, ESIG_SETTING, false);
    }
  });

  test.afterEach(async ({ page }) => {
    for (const id of createdRules) {
      await deactivate(page, id);
    }
    if (esigWasOn) {
      await setSetting(page, ESIG_SETTING, true);
    }
  });

  test("unsafe rules are refused with a reason, and a typed value is never replaced", async ({
    page,
  }) => {
    test.setTimeout(240_000);
    await page.goto("/", { waitUntil: "domcontentloaded" });
    const stamp = Date.now().toString().slice(-8);
    const source = await createNumericTest(page, "Src", stamp);
    const derived = await createNumericTest(page, "Calc", stamp);
    const ruleName = `OGC1587 calc from src ${stamp}`;
    const loopName = `OGC1587 src from calc ${stamp}`;

    await test.step("API: a self-reading rule is refused", async () => {
      const self = await postRule(
        page,
        rule(`OGC1587 self ${stamp}`, derived, derived, "2", true),
      );
      expect(self.status).toBe(400);
      expect(self.body?.messageKey).toBe("calculation.error.selfReference");
    });

    await test.step("API: a valid rule saves; a second one for the same test is refused", async () => {
      const saved = await postRule(
        page,
        rule(ruleName, derived, source, "2", true),
      );
      expect(saved.status).toBe(200);
      createdRules.push(await ruleIdByName(page, ruleName));
      const second = await postRule(
        page,
        rule(`OGC1587 second ${stamp}`, derived, source, "3", true),
      );
      expect(second.status).toBe(400);
      expect(second.body?.messageKey).toBe(
        "calculation.error.duplicateDestination",
      );
    });

    await test.step("API: the closing rule of a loop saves only switched off", async () => {
      const active = await postRule(
        page,
        rule(loopName, source, derived, "2", true),
      );
      expect(active.status).toBe(400);
      expect(active.body?.messageKey).toBe("calculation.error.cycle");
      const off = await postRule(
        page,
        rule(loopName, source, derived, "2", false),
      );
      expect(off.status).toBe(200);
      createdRules.push(await ruleIdByName(page, loopName));
    });

    await test.step("API: a rule read from the list saves back unchanged", async () => {
      const id = createdRules[0];
      const status = await page.evaluate(
        async ({ api, id }) => {
          const list = await (
            await fetch(`${api}/rest/test-calculations?id=${id}`, {
              credentials: "include",
            })
          ).json();
          const res = await fetch(`${api}/rest/test-calculation`, {
            method: "POST",
            credentials: "include",
            headers: {
              "Content-Type": "application/json",
              "X-CSRF-Token": localStorage.getItem("CSRF") || "",
            },
            body: JSON.stringify(list[0]),
          });
          return res.status;
        },
        { api: API, id },
      );
      expect(status).toBe(200);
    });

    await test.step("UI: switching the loop rule on is refused, says why, and stays off", async () => {
      await page.goto("/MasterListsPage/calculatedValue", {
        waitUntil: "domcontentloaded",
      });
      const panel = page
        .locator("div.rules")
        .filter({ has: page.locator(`input[value="${loopName}"]`) });
      await expect(panel).toBeVisible({ timeout: NAV_TIMEOUT });
      const activation = page.waitForResponse((r) =>
        r.url().includes("/rest/activate-test-calculation/"),
      );
      // Carbon lays the label over the toggle button; the label is what a
      // pointer reaches.
      await panel.locator("label.cds--toggle__label").click();
      await activation;
      await expect(
        page.getByText("These active rules would feed each other in a loop", {
          exact: false,
        }),
      ).toBeVisible({ timeout: UI_TIMEOUT });
      await expect(panel.getByText("Active: false")).toBeVisible();
    });

    await test.step("Results: a value typed for the rule's test before its input is kept", async () => {
      const now = new Date();
      const pad = (n: number) => String(n).padStart(2, "0");
      const accession = await createSampleOrder(page, {
        labNo: "",
        receivedDate: `${now.getUTCFullYear()}-${pad(now.getUTCMonth() + 1)}-${pad(now.getUTCDate())}`,
        receivedTime: `${pad(now.getUTCHours())}:${pad(now.getUTCMinutes())}`,
        sampleTypeId: SERUM_SAMPLE_TYPE_ID,
        testIds: `${source.testId},${derived.testId}`,
      });
      expect(accession, "the order must exist").toBeTruthy();
      await page.goto(
        `/Results?accessionNumber=${encodeURIComponent(accession)}`,
        { waitUntil: "domcontentloaded" },
      );
      const rowFor = (t: NumericTest) =>
        page.getByRole("main").getByRole("row").filter({ hasText: t.name });
      const save = async (t: NumericTest, value: string) => {
        const row = rowFor(t);
        await expect(row).toBeVisible({ timeout: NAV_TIMEOUT });
        await row.locator('input[id^="unifiedResultValue-"]').fill(value);
        await row.getByRole("button", { name: /^save$/i }).click();
        await expect(row.getByRole("button", { name: /^edit$/i })).toBeVisible({
          timeout: UI_TIMEOUT,
        });
      };
      await save(derived, "5");
      await save(source, "3");

      await page.reload({ waitUntil: "domcontentloaded" });
      const resultHeader = page
        .getByRole("main")
        .getByRole("columnheader")
        .filter({ hasText: /^Result$/ });
      await expect(resultHeader).toBeVisible({ timeout: NAV_TIMEOUT });
      const resultColumn = await resultHeader.evaluate(
        (th) => (th as HTMLTableCellElement).cellIndex,
      );
      await expect(
        rowFor(derived).getByRole("cell").nth(resultColumn),
        "the rule would have written 6; the typed 5 stays",
      ).toHaveText(/^\s*5\s*$/);
    });
  });
});
