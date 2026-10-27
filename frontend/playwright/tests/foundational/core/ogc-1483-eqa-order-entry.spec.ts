import type { Page } from "@playwright/test";
import { test, expect } from "../../../helpers/test-base";
import { LONG_TIMEOUT, NAV_TIMEOUT } from "../../../helpers/timeouts";

/**
 * OGC-1483: the v4 Enter Order EQA box offers the lab's active enrollments,
 * stores the enrollment, offers its open cycles with their deadline, warns
 * when no cycle is chosen, and keeps the order patient-less.
 */

const API = "/api/OpenELIS-Global";

const csrfToken = async (page: Page): Promise<string> => {
  const state = await page.context().storageState();
  for (const origin of state.origins) {
    for (const item of origin.localStorage) {
      if (item.name === "CSRF") return item.value;
    }
  }
  return "";
};

const post = async (page: Page, path: string, data: unknown) =>
  page.request.post(`${API}${path}`, {
    headers: { "X-CSRF-Token": await csrfToken(page) },
    data,
  });

const seeded: string[] = [];

/** An active enrollment with an open cycle, and a withdrawn enrollment. */
const seed = async (page: Page) => {
  const s = `${Date.now()}`.slice(-6);
  const scheme = `PW1483 scheme ${s}`;
  expect(
    (
      await post(page, "/rest/eqa/programs", {
        name: scheme,
        schemeType: "INTERNATIONAL_PT",
        provider: "PW",
      })
    ).status(),
  ).toBe(200);
  const active = await post(page, "/rest/eqa/my-programs", {
    programName: scheme,
    provider: "PW",
  });
  const activeId = String((await active.json()).id);
  const withdrawn = await post(page, "/rest/eqa/my-programs", {
    programName: `PW1483 withdrawn ${s}`,
    provider: "PW",
  });
  const withdrawnId = String((await withdrawn.json()).id);
  await page.request.put(`${API}/rest/eqa/my-programs/${withdrawnId}/status`, {
    headers: { "X-CSRF-Token": await csrfToken(page) },
    data: {
      status: "Withdrawn",
      reason: "PW1483",
      effectiveDate: "2026-10-14",
    },
  });
  const cycle = await post(page, "/rest/eqa/cycles/mine", {
    schemeName: scheme,
    cycleName: `PW1483 cycle ${s}`,
    distributionDate: "2026-10-14",
    submissionDeadline: "2026-12-15",
  });
  expect(cycle.status()).toBe(201);
  seeded.push(activeId, withdrawnId);
  return {
    activeId,
    withdrawnId,
    cycleId: String((await cycle.json()).id),
  };
};

test.describe("Enter Order EQA box (OGC-1483)", () => {
  test.beforeEach(() => test.setTimeout(180_000));
  test.afterEach(async ({ page }) => {
    const headers = { "X-CSRF-Token": await csrfToken(page) };
    for (const id of seeded.splice(0)) {
      await page.request.delete(`${API}/rest/eqa/my-programs/${id}`, {
        headers,
      });
    }
  });

  test("only active enrollments are offered", async ({ page }) => {
    await page.goto("/", { waitUntil: "domcontentloaded" });
    const { activeId, withdrawnId } = await seed(page);

    const listed = (await (
      await page.request.get(`${API}/rest/eqa/my-programs?active=true`)
    ).json()) as { id: number }[];
    expect(listed.map((e) => String(e.id))).toContain(activeId);
    expect(listed.map((e) => String(e.id))).not.toContain(withdrawnId);

    await page.goto("/order/clinical/enter?eqa=true", {
      waitUntil: "domcontentloaded",
    });
    await expect(
      page.locator(`#eqaProgramId option[value="${activeId}"]`),
    ).toHaveCount(1, { timeout: NAV_TIMEOUT });
    await expect(
      page.locator(`#eqaProgramId option[value="${withdrawnId}"]`),
    ).toHaveCount(0);
  });

  test("a chosen cycle prefills the deadline, and no cycle only warns", async ({
    page,
  }) => {
    await page.goto("/", { waitUntil: "domcontentloaded" });
    const { activeId, cycleId } = await seed(page);
    await page.goto("/order/clinical/enter?eqa=true", {
      waitUntil: "domcontentloaded",
    });

    await expect(page.locator("#noPatientOverride")).toBeChecked({
      timeout: NAV_TIMEOUT,
    });
    await expect(page.locator("#noPatientOverride")).toBeDisabled();
    await expect(page.locator("#order-patient-search-lastName")).toHaveCount(0);
    await expect(page.getByText("Choose the EQA scheme")).toBeVisible();

    await expect(
      page.locator(`#eqaProgramId option[value="${activeId}"]`),
    ).toHaveCount(1, { timeout: LONG_TIMEOUT });
    await page.locator("#eqaProgramId").selectOption(activeId);
    await expect(
      page.getByText(/not linked to a cycle, so it will not be submitted/),
    ).toBeVisible({ timeout: LONG_TIMEOUT });
    await expect(page.getByRole("button", { name: "New cycle" })).toBeVisible();

    await expect(
      page.locator(`#eqaCycleId option[value="${cycleId}"]`),
    ).toHaveCount(1, { timeout: LONG_TIMEOUT });
    await page.locator("#eqaCycleId").selectOption(cycleId);
    await expect(page.locator("#eqaDeadline")).toHaveValue(/2026/);
    await expect(
      page.getByText(/not linked to a cycle, so it will not be submitted/),
    ).toHaveCount(0);
  });
});
