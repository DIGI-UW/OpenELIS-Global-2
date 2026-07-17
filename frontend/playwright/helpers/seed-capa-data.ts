import { Page, expect } from "@playwright/test";

/**
 * CAPA Register E2E seeding (OGC-707).
 *
 * Seeds a corrective/preventive action against a fresh NCE using the SAME
 * REST endpoints the authoring UI uses — no direct DB writes, so the spec is
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
 * All calls run through `page.request` so they share the browser's
 * authenticated session; the CSRF token is lifted from stored auth state
 * (mirrors electronic-signature.spec.ts).
 */

const REST = "/api/OpenELIS-Global/rest";
const RESOLVE = "/api/OpenELIS-Global/ResolveNonConformingEvent";

export interface CapaSeed {
  /** Deterministic, unique per run so the register can be filtered to this seed. */
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

export async function seedCapa(page: Page, seed: CapaSeed): Promise<void> {
  const csrf = await csrfToken(page);
  const jsonHeaders = {
    "X-CSRF-Token": csrf,
    "Content-Type": "application/json",
  };

  // 1. Create the parent NCE (worker sets status = "Pending").
  const created = await page.request.post(`${REST}/reportnonconformingevent`, {
    headers: jsonHeaders,
    data: {
      nceNumber: seed.nceNumber,
      labOrderNumber: seed.nceNumber,
      specimenId: "",
      name: seed.personResponsible,
      title: seed.title,
      description: seed.title,
    },
  });
  expect(
    created.status(),
    `create NCE ${seed.nceNumber} should succeed`,
  ).toBeLessThan(300);

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
