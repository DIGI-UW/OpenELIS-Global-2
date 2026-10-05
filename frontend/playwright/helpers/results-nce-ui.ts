import { Locator, Page, Response, expect } from "@playwright/test";
import { csrfToken } from "./api-session";
import { createSampleOrder } from "./seed-tat-data";
import { LONG_TIMEOUT, UI_TIMEOUT } from "./timeouts";

/**
 * Shared steps for the results, validation, workplan and non-conformity
 * specs: ordering fresh accessions, reaching the server page that holds one,
 * completing the e-signature ceremony when the site asks for it, and seeding
 * a referral or an NCE through the same endpoints the screens post to.
 */

export const API = "/api/OpenELIS-Global";

/**
 * Tests of the base catalog these specs order. RBC and Hemoglobin are
 * whole-blood Hematology tests in the NFS panel; Amylase is a serum
 * Biochemistry test. `labUnitOf` reads the unit each belongs to at run time.
 */
export const WHOLE_BLOOD = "4";
export const SERUM = "2";
export const RBC_TEST = "14";
export const HEMOGLOBIN_TEST = "15";
export const AMYLASE_TEST = "5";

const password = process.env.TEST_PASS || "adminADMIN!";

export async function getJson<T>(page: Page, path: string): Promise<T> {
  const response = await page.request.get(`${API}${path}`);
  expect(response.status(), `GET ${path}`).toBe(200);
  return (await response.json()) as T;
}

/** A new order for the given tests; returns its accession number. */
export async function orderTests(
  page: Page,
  sampleTypeId: string,
  testIds: string[],
  priority: "routine" | "stat" = "routine",
): Promise<string> {
  const accession = await createSampleOrder(page, {
    labNo: "",
    receivedDate: "",
    receivedTime: "",
    sampleTypeId,
    testIds: testIds.join(","),
    priority,
  });
  expect(accession, "the order must be created").toBeTruthy();
  return accession;
}

/** The lab unit (test section) id a catalog test belongs to. */
export async function labUnitOf(page: Page, testId: string): Promise<string> {
  const info = await getJson<{ labUnitId: string }>(
    page,
    `/rest/test-catalog/tests/${testId}/basic-info`,
  );
  expect(info.labUnitId, `test ${testId} has a lab unit`).toBeTruthy();
  return String(info.labUnitId);
}

interface PagedList {
  paging?: {
    totalPages?: string | number;
    currentPage?: string | number;
    searchTermToPage?: { id: string; value: string }[];
  };
}

/**
 * The lists these screens show are split by the server into pages; the
 * paging block names the page each accession sits on. When the accession is
 * not on the first page, Carbon's page select asks the server for the one
 * that holds it.
 */
export async function openServerPageHolding(
  page: Page,
  loaded: Response,
  accession: string,
): Promise<void> {
  const body = (await loaded.json()) as PagedList;
  const totalPages = Number(body.paging?.totalPages) || 1;
  if (totalPages < 2) return;
  const target = body.paging?.searchTermToPage?.find(
    (entry) => entry.id === accession,
  )?.value;
  expect(target, `${accession} is on one of the server's pages`).toBeTruthy();
  if (Number(target) === (Number(body.paging?.currentPage) || 1)) return;
  const pageLoaded = page.waitForResponse(
    (response) =>
      response.request().method() === "GET" &&
      response.url().endsWith(`&page=${target}`),
    { timeout: LONG_TIMEOUT },
  );
  await page
    .getByRole("main")
    .locator(".cds--pagination__right select")
    .first()
    .selectOption(String(target));
  await pageLoaded;
}

async function esigEnabled(page: Page): Promise<boolean> {
  const state = await getJson<{ enabled?: boolean }>(
    page,
    "/rest/esig/enabled",
  );
  return state.enabled === true;
}

/** Completes the e-signature ceremony when the site has signing switched on. */
export async function signIfAsked(page: Page): Promise<void> {
  if (!(await esigEnabled(page))) return;
  const dialog = page.getByRole("dialog");
  const passwordInput = dialog.locator('input[type="password"]');
  await expect(passwordInput).toBeVisible({ timeout: UI_TIMEOUT });
  const acknowledgement = dialog.locator(
    'label[for="certification-acknowledgement"]',
  );
  if (await acknowledgement.isVisible()) {
    await acknowledgement.click();
    await passwordInput.fill(password);
    await dialog.getByRole("button", { name: /certify|continue/i }).click();
    await expect(passwordInput).toBeVisible({ timeout: UI_TIMEOUT });
  }
  await passwordInput.fill(password);
  await dialog.getByRole("button", { name: /^sign/i }).click();
  await expect(dialog).toBeHidden({ timeout: LONG_TIMEOUT });
}

/** The value and label of the first option of a native select that has a value. */
export async function firstRealOption(
  select: Locator,
): Promise<{ value: string; label: string }> {
  await expect
    .poll(() => select.locator("option:not([value=''])").count(), {
      timeout: UI_TIMEOUT,
    })
    .toBeGreaterThan(0);
  const option = select.locator("option:not([value=''])").first();
  return {
    value: (await option.getAttribute("value")) || "",
    label: ((await option.textContent()) || "").trim(),
  };
}

interface IdValue {
  id: string;
  value: string;
}

interface WorklistRow extends Record<string, unknown> {
  analysisId: string;
  testId: string;
  testName: string;
}

/**
 * Refers one test of an order to the first reference laboratory the site has,
 * with the payload the unified Results row posts when its referral is saved.
 */
export async function referViaApi(
  page: Page,
  accession: string,
  testId: string,
): Promise<{ referenceLab: string }> {
  const [labs, reasons, worklist] = await Promise.all([
    getJson<IdValue[]>(page, "/rest/displayList/REFERRAL_ORGANIZATIONS"),
    getJson<IdValue[]>(page, "/rest/displayList/REFERRAL_REASONS"),
    getJson<{ testResult?: WorklistRow[] }>(
      page,
      `/rest/LogbookResults?labNumber=${encodeURIComponent(accession)}&doRange=false&finished=false`,
    ),
  ]);
  expect(labs.length, "a reference laboratory is configured").toBeGreaterThan(
    0,
  );
  expect(reasons.length, "a referral reason is configured").toBeGreaterThan(0);
  const row = (worklist.testResult || []).find((r) => r.testId === testId);
  expect(row, `${accession} has test ${testId} to refer`).toBeTruthy();
  const { serverDate } = await serverToday(page);
  const item: Record<string, unknown> = { ...row, isModified: true };
  delete item.result;
  delete item.analysisNotes;
  delete item.resultFile;
  item.reportable = item.reportable !== "N";
  item.refer = true;
  item.referredOut = true;
  item.referralItem = {
    referralReasonId: reasons[0].id,
    referredInstituteId: labs[0].id,
    referredSendDate: serverDate,
    referredTestId: testId,
  };
  const saved = await page.request.post(
    `${API}/rest/results-entry/analysis/${row!.analysisId}/result`,
    {
      headers: { "X-CSRF-Token": await csrfToken(page) },
      data: { testResult: item },
    },
  );
  expect(saved.status(), "the referral save is accepted").toBe(200);
  return { referenceLab: labs[0].value };
}

/** Today on the server, as dd/MM/yyyy (the app's display format). */
export async function serverToday(
  page: Page,
): Promise<{ serverDate: string; iso: string }> {
  const time = await getJson<{ date: string }>(page, "/rest/server-time");
  const [yyyy, mm, dd] = time.date.split("-");
  return { serverDate: `${dd}/${mm}/${yyyy}`, iso: time.date };
}

/**
 * Today on the server, written the way a date input asks for it: the site's
 * locale decides day- or month-first, and the input's placeholder shows which.
 */
export async function serverTodayFor(input: Locator): Promise<string> {
  const { iso } = await serverToday(input.page());
  const [yyyy, mm, dd] = iso.split("-");
  const placeholder = ((await input.getAttribute("placeholder")) || "")
    .toLowerCase()
    .trim();
  expect(placeholder, "the date input names its format").toMatch(
    /^(dd\/mm|mm\/dd)\/yyyy$/,
  );
  return placeholder.startsWith("dd")
    ? `${dd}/${mm}/${yyyy}`
    : `${mm}/${dd}/${yyyy}`;
}

/**
 * Reports an NCE with the payload the Report Non-Conforming Event form sends,
 * linked to the given order's first specimen, and returns the NCE number the
 * server allocated.
 */
export async function reportNceViaApi(
  page: Page,
  accession: string,
  description: string,
): Promise<string> {
  const [samples, categories, units] = await Promise.all([
    getJson<{ sampleItems: { id: string }[] }[]>(
      page,
      `/rest/nonconformevents?labNumber=${encodeURIComponent(accession)}`,
    ),
    getJson<{ id: string }[]>(page, "/rest/nce/categories"),
    getJson<IdValue[]>(page, "/rest/displayList/TEST_SECTION_ACTIVE"),
  ]);
  expect(samples[0]?.sampleItems?.length, "the order has a specimen").toBe(1);
  const { iso } = await serverToday(page);
  const [yyyy, mm, dd] = iso.split("-");
  const created = await page.request.post(
    `${API}/rest/reportnonconformingevent`,
    {
      headers: { "X-CSRF-Token": await csrfToken(page) },
      data: {
        reporterName: "Open ELIS",
        dateOfEvent: `${mm}/${dd}/${yyyy}`,
        reportingUnit: units[0].id,
        labOrderNumber: accession,
        specimenId: samples[0].sampleItems[0].id,
        title: description,
        description,
        severity: "MINOR",
        nceCategoryId: categories[0].id,
        nceTypeId: "",
      },
    },
  );
  expect(created.status(), "the NCE is reported").toBe(200);
  const found = await getJson<{
    nceEventsSearchResults?: { nceNumber: string }[];
  }>(
    page,
    `/rest/viewNonConformEvents?labNumber=${encodeURIComponent(accession)}&nceNumber=&status=`,
  );
  const nceNumber = found.nceEventsSearchResults?.[0]?.nceNumber;
  expect(nceNumber, `an NCE is on record for ${accession}`).toBeTruthy();
  return nceNumber!;
}
