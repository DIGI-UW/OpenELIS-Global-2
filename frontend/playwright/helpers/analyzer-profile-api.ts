/**
 * Authoring analyzer types through the REST API, so a spec can give an
 * analyzer a profile whose tests it controls.
 */
import { expect, type Page } from "@playwright/test";
import { csrfToken } from "./api-session";

export const API = "/api/OpenELIS-Global/rest";

export type ProfileTest = {
  test_code: string;
  loinc: string;
  unit: string;
  result_type: string;
};

export type Draft = {
  draftId: string;
  profile: Record<string, unknown> & { profileMeta: { id: string } };
};

export const numeric = (code: string, loinc: string): ProfileTest => ({
  test_code: code,
  loinc,
  unit: "mmol/L",
  result_type: "quantitative",
});

export async function send(
  page: Page,
  method: "post" | "put",
  path: string,
  data: unknown,
) {
  const response = await page.request[method](`${API}${path}`, {
    headers: { "X-CSRF-Token": await csrfToken(page) },
    data,
  });
  expect(
    response.ok(),
    `${method.toUpperCase()} ${path}: ${response.status()} ${await response.text()}`,
  ).toBeTruthy();
  return response.json();
}

/** Saves the draft with these tests and publishes it as the next revision. */
export async function publish(page: Page, draft: Draft, tests: ProfileTest[]) {
  const saved = await send(
    page,
    "put",
    `/analyzer-types/drafts/${draft.draftId}`,
    {
      profile: { ...draft.profile, default_test_mappings: tests },
    },
  );
  expect(saved.validationIssues || []).toEqual([]);
  await send(
    page,
    "post",
    `/analyzer-types/drafts/${draft.draftId}/publish`,
    {},
  );
}

/** A new analyzer type, copied from the baseline GeneXpert, with only these tests. */
export async function createProfile(
  page: Page,
  displayName: string,
  tests: ProfileTest[],
): Promise<Draft> {
  const shipped = (
    (await (await page.request.get(`${API}/analyzer-types`)).json()) as {
      types: Array<{ profileId: string; revision: number; status: string }>;
    }
  ).types.find(
    (type) =>
      type.profileId === "cepheid-genexpert-astm" && type.status === "ACTIVE",
  );
  expect(shipped, "the shipped GeneXpert type").toBeTruthy();
  const draft: Draft = await send(
    page,
    "post",
    "/analyzer-types/cepheid-genexpert-astm/duplicate",
    { sourceRevision: shipped!.revision, displayName },
  );
  await publish(page, draft, tests);
  return draft;
}
