import { Page, expect } from "@playwright/test";

/**
 * CAPA Register E2E seeding (OGC-707).
 *
 * Seeds a corrective/preventive action against a fresh NCE using the SAME
 * REST endpoints the authoring UI uses — no direct DB writes, so this seed is
 * self-contained on any stack:
 *
 *   1. POST /rest/reportnonconformingevent   → create the parent NCE (Pending)
 *   2. GET  /rest/NCECorrectiveAction         → read back its id + action log
 *   3. POST /rest/NCECorrectiveAction         → append one CAPA row (carries dueDate)
 *   4. POST /ResolveNonConformingEvent        → (optional) flip NCE to Completed
 *
 * `labOrderNumber` is a plain string column with no FK, so no sample order is
 * needed. The register reads completion from the parent NCE status (not the
 * action-log row), which is why `resolve` drives the legacy MVC endpoint.
 *
 * The server allocates the NCE number and ignores any the caller sends, so the
 * seed posts under its own unique labOrderNumber, reads the allocated number
 * back through the search endpoint, and writes it onto `seed.nceNumber` for the
 * spec to assert against.
 *
 * All calls run through `page.request` so they share the browser's
 * authenticated session; the CSRF token is lifted from stored auth state
 * (mirrors electronic-signature.spec.ts).
 */

const REST = "/api/OpenELIS-Global/rest";
const RESOLVE = "/api/OpenELIS-Global/ResolveNonConformingEvent";

export interface CapaSeed {
  /**
   * Unique per run. Goes out as the labOrderNumber the seed is found by, and is
   * overwritten with the NCE number the server allocates.
   */
  nceNumber: string;
  title: string;
  correctiveAction: string;
  /** Comma-joined action-type codes (see NCECorrectiveAction.jsx checkboxes), e.g. "1". */
  actionType: string;
  personResponsible: string;
  /** yyyy-MM-dd — bound straight to the additive nce_action_log.due_date column. */
  dueDate: string;
  /** Flip the parent NCE to Completed (green tag + Completed filter). */
  resolve?: boolean;
}

async function csrfToken(page: Page): Promise<string> {
  const state = await page.context().storageState();
  for (const origin of state.origins) {
    for (const item of origin.localStorage) {
      if (item.name === "CSRF") return item.value;
    }
  }
  return "";
}

/** yyyy -> MM/dd/yyyy, the format the NCE create path parses. */
function usDate(d: Date): string {
  const mm = String(d.getMonth() + 1).padStart(2, "0");
  const dd = String(d.getDate()).padStart(2, "0");
  return `${mm}/${dd}/${d.getFullYear()}`;
}

/** First id from a display-list style endpoint. */
async function firstId(page: Page, url: string): Promise<string> {
  const res = await page.request.get(url);
  expect(res.status(), `${url} should answer`).toBe(200);
  const rows = await res.json();
  expect(Array.isArray(rows) && rows.length > 0, `${url} must have rows`).toBe(
    true,
  );
  return String(rows[0].id ?? rows[0].value);
}

/** The NCE number the server chose, found by the labOrderNumber we sent. */
async function allocatedNceNumber(
  page: Page,
  labOrderNumber: string,
): Promise<string> {
  const res = await page.request.get(
    `${REST}/viewNonConformEvents?labNumber=${encodeURIComponent(labOrderNumber)}`,
  );
  expect(res.status()).toBe(200);
  const body = await res.json();
  const rows = Array.isArray(body) ? body : [body];
  const found = rows.find((r) => r?.nceNumber)?.nceNumber;
  expect(
    found,
    `created NCE for ${labOrderNumber} must be findable`,
  ).toBeTruthy();
  return String(found);
}

export async function seedCapa(page: Page, seed: CapaSeed): Promise<void> {
  const csrf = await csrfToken(page);
  const jsonHeaders = {
    "X-CSRF-Token": csrf,
    "Content-Type": "application/json",
  };

  // The create path validates against real reference data, so read an active
  // category and reporting unit rather than guessing ids that vary per stack.
  const categoryId = await firstId(page, `${REST}/nce/categories`);
  const reportingUnit = await firstId(
    page,
    `${REST}/displayList/TEST_SECTION_ACTIVE`,
  );

  // The seed is found again by this, since the NCE number is the server's to
  // choose. Captured before seed.nceNumber is overwritten below.
  const labOrderNumber = seed.nceNumber;

  // 1. Create the parent NCE (worker sets status = "Pending"). specimenId is
  //    required but this register never asserts specimens, and the worker skips
  //    linking an id it cannot parse, so a marker keeps the seed order-free.
  const created = await page.request.post(`${REST}/reportnonconformingevent`, {
    headers: jsonHeaders,
    data: {
      labOrderNumber,
      specimenId: "no-specimen",
      dateOfEvent: usDate(new Date()),
      reportingUnit,
      severity: "MINOR",
      nceCategoryId: categoryId,
      name: seed.personResponsible,
      title: seed.title,
      description: seed.title,
    },
  });
  expect(
    created.status(),
    `create NCE for ${labOrderNumber} should succeed`,
  ).toBeLessThan(300);

  // 1b. Read back the number the server allocated and use it from here on.
  seed.nceNumber = await allocatedNceNumber(page, labOrderNumber);

  // 2. Read back the form for its generated id + any existing action logs.
  const formRes = await page.request.get(
    `${REST}/NCECorrectiveAction?nceNumber=${encodeURIComponent(seed.nceNumber)}`,
  );
  expect(formRes.status()).toBe(200);
  const form = await formRes.json();
  expect(form.id, `NCE ${seed.nceNumber} must exist after create`).toBeTruthy();

  // 3. Append one CAPA action log (the dueDate rides on this row).
  const appended = await page.request.post(`${REST}/NCECorrectiveAction`, {
    headers: jsonHeaders,
    data: {
      id: form.id,
      actionLog: [
        ...(form.actionLog ?? []),
        {
          correctiveAction: seed.correctiveAction,
          actionType: seed.actionType,
          personResponsible: seed.personResponsible,
          dueDate: seed.dueDate,
          turnAroundTime: 0,
        },
      ],
      dateCompleted: "",
      discussionDate: "",
    },
  });
  expect(
    appended.status(),
    `append CAPA to ${seed.nceNumber} should succeed`,
  ).toBeLessThan(300);

  // 4. Optionally resolve the NCE → status "Completed" (legacy MVC form post;
  //    answers with a redirect on success, so don't follow it).
  if (seed.resolve) {
    const resolved = await page.request.post(RESOLVE, {
      headers: { "X-CSRF-Token": csrf },
      form: { id: String(form.id), currentUserId: "1", effective: "true" },
      maxRedirects: 0,
    });
    expect(
      resolved.status(),
      `resolve ${seed.nceNumber} should redirect on success`,
    ).toBeGreaterThanOrEqual(300);
  }
}
