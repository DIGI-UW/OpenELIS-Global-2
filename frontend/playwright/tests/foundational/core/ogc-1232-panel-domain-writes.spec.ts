import type { Page } from "@playwright/test";
import { test, expect } from "../../../helpers/test-base";
import { NAV_TIMEOUT, UI_TIMEOUT } from "../../../helpers/timeouts";

/**
 * OGC-1232: an operator can create an Environmental or Vector panel from the
 * legacy Create New Panel screen, a refused create is reported instead of
 * answered with 200, and the Panel Editor's domain guard says which member
 * tests stand in the way of a domain change instead of refusing silently.
 *
 * Every panel these tests touch is created by the tests themselves, so the
 * cases hold on any database.
 */

const API = "/api/OpenELIS-Global";
const run = Date.now().toString().slice(-6);
const LOINC = `${run.slice(0, 5)}-${run.slice(5)}`;

interface PanelRow {
  id: string;
  name: string;
  domain: string;
}

interface ConflictingTest {
  testId: string;
  name: string;
  domain: string;
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

const panelNamed = async (
  page: Page,
  name: string,
): Promise<PanelRow | undefined> => {
  const response = await page.request.get(
    `${API}/rest/test-catalog/panels?includeInactive=true`,
  );
  expect(response.ok()).toBeTruthy();
  return ((await response.json()) as PanelRow[]).find((p) => p.name === name);
};

test.describe("Panel domain writes (OGC-1232)", () => {
  test.beforeEach(() => test.setTimeout(180_000));

  test("the legacy Create New Panel screen files a panel under the chosen domain", async ({
    page,
  }) => {
    const name = `E2E Env ${run}`;

    await page.goto("/MasterListsPage/PanelCreate", {
      waitUntil: "domcontentloaded",
    });
    await expect(page.locator("#eng")).toBeVisible({ timeout: NAV_TIMEOUT });
    await expect(
      page.locator('label[for="panel-domain-ENVIRONMENTAL"]'),
    ).toBeVisible({ timeout: UI_TIMEOUT });

    await page.locator("#eng").fill(name);
    await page.locator("#fr").fill(name);
    await page.locator("#smapleTypeSelect").selectOption({ index: 1 });
    await page.locator("#loincPost").fill(LOINC);
    // Carbon hides the radio input; the label is the click target.
    await page.locator('label[for="panel-domain-ENVIRONMENTAL"]').click();
    await expect(page.locator("#panel-domain-ENVIRONMENTAL")).toBeChecked();
    await expect(page.getByTestId("panel-create-domain-helper")).toHaveText(
      "Only Environmental-domain tests can be added to this panel.",
    );

    // exact names: the side navigation's "Sample Acceptance Checklist" also
    // answers to a loose "Accept"
    await page.getByRole("button", { name: "Next", exact: true }).click();
    const created = page.waitForResponse(
      (r) =>
        r.url().endsWith("/rest/PanelCreate") &&
        r.request().method() === "POST",
    );
    await page.getByRole("button", { name: "Accept", exact: true }).click();
    await created;

    await expect(
      page.getByText(
        "Panel created. It stays inactive until at least one test is assigned to it.",
      ),
    ).toBeVisible({ timeout: UI_TIMEOUT });
    // The screen lists the new panel among the inactive ones and is ready for
    // the next entry.
    await expect(page.getByText(name)).toBeVisible({ timeout: UI_TIMEOUT });
    await expect(page.locator("#eng")).toHaveValue("");

    const stored = await panelNamed(page, name);
    expect(stored, "the panel must exist").toBeTruthy();
    expect(stored?.domain).toBe("ENVIRONMENTAL");
  });

  test("the legacy create reports a refused insert instead of answering 200", async ({
    page,
  }) => {
    const token = await csrfToken(page);
    const headers = { "X-CSRF-Token": token };
    const form = await (
      await page.request.get(`${API}/rest/PanelCreate`)
    ).json();
    const sampleTypeId = String(form.existingSampleTypeList[0].id);
    const legacyCreate = (name: string, loinc: string, domain?: string) =>
      page.request.post(`${API}/rest/PanelCreate`, {
        headers,
        data: {
          panelEnglishName: name,
          panelFrenchName: name,
          sampleTypeId,
          panelLoinc: loinc,
          ...(domain ? { domain } : {}),
        },
      });

    // A blank LOINC fails validation: 400, nothing created.
    const blankName = `E2E Blank ${run}`;
    const blank = await legacyCreate(blankName, "");
    expect(blank.status()).toBe(400);
    expect(await panelNamed(page, blankName)).toBeUndefined();

    // An unknown domain is refused the same way.
    const bogusName = `E2E Bogus ${run}`;
    const bogus = await legacyCreate(bogusName, "99998-8", "MARINE");
    expect(bogus.status()).toBe(400);
    expect(await panelNamed(page, bogusName)).toBeUndefined();

    // A second panel with the same name is a 409, not a silent 200.
    const dupName = `E2E Dup ${run}`;
    const first = await legacyCreate(dupName, "99998-1", "vector");
    expect(first.status()).toBe(200);
    expect((await first.json()).createdPanelId).toBeTruthy();
    const second = await legacyCreate(dupName, "99998-2");
    expect(second.status()).toBe(409);
    expect((await second.json()).error).toBe("duplicate");
    const stored = await panelNamed(page, dupName);
    expect(stored?.domain).toBe("VECTOR");
  });

  test("a refused domain change tells the operator which tests stand in the way", async ({
    page,
  }) => {
    const token = await csrfToken(page);
    const headers = { "X-CSRF-Token": token };
    const name = `E2E Guard ${run}`;

    // Seed a Clinical panel with one Clinical member, the way the editor does.
    const tests = await page.request.get(
      `${API}/rest/test-catalog/tests?domain=CLINICAL&status=active&page=1&pageSize=1`,
    );
    const member = (await tests.json()).rows[0];
    expect(member?.testId, "the guard needs one clinical test").toBeTruthy();
    const created = await page.request.post(`${API}/rest/test-catalog/panels`, {
      headers,
      data: { name, active: false },
    });
    expect(created.ok()).toBeTruthy();
    const { id } = await created.json();
    const added = await page.request.put(
      `${API}/rest/test-catalog/panels/${id}/tests`,
      { headers, data: { tests: [{ testId: member.testId, position: 1 }] } },
    );
    expect(added.ok()).toBeTruthy();

    await page.goto(
      `/MasterListsPage/TestCatalogEditor/panel/${id}/basic-info`,
      {
        waitUntil: "domcontentloaded",
      },
    );
    await expect(page.locator("#panel-name")).toHaveValue(name, {
      timeout: NAV_TIMEOUT,
    });
    await page.locator('label[for="panel-domain-environmental"]').click();
    const refused = page.waitForResponse(
      (r) =>
        r.url().includes(`/panels/${id}/basic-info`) &&
        r.request().method() === "PUT",
    );
    await page.getByRole("button", { name: "Save", exact: true }).click();
    const response = await refused;

    // The contract: 422 whose body names the domain and the offending test.
    expect(response.status()).toBe(422);
    const body = await response.json();
    const offenders = body.domainConflict.tests as ConflictingTest[];
    expect(body.domainConflict.domain).toBe("ENVIRONMENTAL");
    expect(offenders.map((t) => t.testId)).toContain(String(member.testId));

    // The editor's own notification reaches the operator: the shell has to
    // render the dialog its sections raise messages into (OGC-1232).
    await expect(
      page
        .locator(".cds--toast-notification")
        .filter({ hasText: "This panel cannot be filed under Environmental" }),
    ).toBeVisible({ timeout: UI_TIMEOUT });

    // The screen: an explanation that names the test, beside the domain radios.
    const explanation = page.getByTestId("panel-domain-conflict");
    await expect(explanation).toBeVisible({ timeout: UI_TIMEOUT });
    await expect(explanation).toContainText(
      "This panel cannot be filed under Environmental: 1 of its tests belongs to another domain",
    );
    await expect(explanation).toContainText(offenders[0].name);
    await expect(explanation).toContainText(
      "Change the domain of those tests in the Test Catalog, or remove them from this panel, then save again.",
    );

    // The stored domain is untouched by the refused move.
    const stored = await page.request.get(
      `${API}/rest/test-catalog/panels/${id}`,
    );
    expect((await stored.json()).domain).toBe("CLINICAL");

    // Choosing a domain again starts over: the explanation goes with it.
    await page.locator('label[for="panel-domain-clinical"]').click();
    await expect(explanation).toHaveCount(0);
  });
});
