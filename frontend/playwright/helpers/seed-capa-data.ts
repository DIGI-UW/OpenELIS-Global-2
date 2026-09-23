import { Page, expect } from "@playwright/test";
import { csrfToken } from "./api-session";

/**
 * CAPA Register E2E seeding (OGC-707) via the same REST endpoints the
 * authoring UI uses, so the seed is self-contained on any stack.
 *
 * Two things the calls below do not show:
 *  - the server allocates the NCE number and ignores any the caller sends,
 *    so the seed posts under its own unique labOrderNumber and reads the
 *    allocated number back through the search endpoint;
 *  - the register reads completion from the parent NCE status, not from the
 *    action-log row, which is why `resolve` drives the legacy MVC endpoint.
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
  // A miss is not an empty list: the controller answers 200 with the bare
  // string "No results found for search criteria.", so quote the body to tell
  // a miss from a shape change.
  const body = await res.text();
  let rows: Array<{ nceNumber?: string }> = [];
  try {
    rows = JSON.parse(body)?.nceEventsSearchResults ?? [];
  } catch {
    // a miss is not JSON at all, so leave rows empty and let the message quote it
  }
  const found = rows.find((r) => r?.nceNumber)?.nceNumber;
  expect(
    found,
    `created NCE for ${labOrderNumber} must be findable, got: ${body.slice(0, 200)}`,
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

  const labOrderNumber = seed.nceNumber;

  // 1. Create the parent NCE (worker sets status = "Pending"). specimenId is
  //    required but this register never asserts specimens, and the worker skips
  //    linking an id it cannot parse, so a marker keeps the seed order-free.
  const created = await page.request.post(`${REST}/reportnonconformingevent`, {
    headers: jsonHeaders,
    data: {
      labOrderNumber,
      specimenId: "no-specimen",
      // MM/dd/yyyy, the format the NCE create path parses.
      dateOfEvent: new Date().toLocaleDateString("en-US", {
        year: "numeric",
        month: "2-digit",
        day: "2-digit",
      }),
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

  // 2. Read back the number the server allocated, then the form for its
  //    generated id + any existing action logs.
  seed.nceNumber = await allocatedNceNumber(page, labOrderNumber);
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
