import type { Page } from "@playwright/test";
import { randomUUID } from "node:crypto";
import { expect, test } from "../../../helpers/test-base";
import { AnalyzerListPage } from "../../../fixtures/analyzer-list";
import { AnalyzerSetupPage } from "../../../fixtures/analyzer-setup";
import { csrfToken } from "../../../helpers/api-session";

const API = "/api/OpenELIS-Global/rest";

type ProfileTest = {
  test_code: string;
  loinc: string;
  unit: string;
  result_type: string;
};

type Draft = {
  draftId: string;
  profile: Record<string, unknown> & { profileMeta: { id: string } };
};

const numeric = (code: string, loinc: string): ProfileTest => ({
  test_code: code,
  loinc,
  unit: "mmol/L",
  result_type: "quantitative",
});

async function send(
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
async function publish(page: Page, draft: Draft, tests: ProfileTest[]) {
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

test.describe("Adopting a newer profile revision", () => {
  test("an analyzer adopts the next revision: a fixed LOINC binds by default and a new code needs mapping", async ({
    page,
  }) => {
    const runId = randomUUID().slice(0, 8);
    const displayName = `Adoption bench ${runId}`;

    // A catalog test the corrected LOINC finds unambiguously, and LOINC codes
    // the catalog does not know.
    const catalog = (await (
      await page.request.get(`${API}/analyzer-types/mapping-catalog/tests`)
    ).json()) as Array<{ id: string; name: string; loincCodes: string[] }>;
    const loincCounts = new Map<string, number>();
    catalog
      .flatMap((candidate) => candidate.loincCodes)
      .forEach((code) =>
        loincCounts.set(code, (loincCounts.get(code) || 0) + 1),
      );
    const target = catalog.find(
      (candidate) =>
        candidate.loincCodes.length === 1 &&
        loincCounts.get(candidate.loincCodes[0]) === 1,
    );
    expect(target, "a catalog test with its own LOINC code").toBeTruthy();
    const [wrongLoinc, newLoinc] = [
      "2093-3",
      "2571-8",
      "2085-9",
      "1975-2",
      "2160-0",
    ].filter((code) => !loincCounts.has(code));

    const shipped = (
      (await (await page.request.get(`${API}/analyzer-types`)).json()) as {
        types: Array<{ profileId: string; revision: number; status: string }>;
      }
    ).types.find(
      (type) => type.profileId === "genexpert-astm" && type.status === "ACTIVE",
    );
    const first: Draft = await send(
      page,
      "post",
      "/analyzer-types/genexpert-astm/duplicate",
      { sourceRevision: shipped!.revision, displayName },
    );
    const profileId = first.profile.profileMeta.id;
    await publish(page, first, [numeric("ADOPT-FIX", wrongLoinc)]);

    const list = new AnalyzerListPage(page);
    const setup = new AnalyzerSetupPage(page);
    await list.goto();
    await list.clickAdd();
    await setup.expectOpen();
    await setup.selectProfile(displayName);
    await setup.fillName(displayName);
    await setup.selectFirstLabUnit();
    await setup.continueToVerify();
    const analyzerId = new URL(page.url()).searchParams.get("analyzerId")!;

    const second: Draft = await send(
      page,
      "post",
      `/analyzer-types/${encodeURIComponent(profileId)}/update`,
      { sourceRevision: 1 },
    );
    await publish(page, second, [
      numeric("ADOPT-FIX", target!.loincCodes[0]),
      numeric("ADOPT-NEW", newLoinc),
    ]);

    await page.goto(
      `/analyzers/types/${encodeURIComponent(profileId)}/mapping?revision=2`,
      { waitUntil: "domcontentloaded" },
    );
    await page
      .getByRole("listitem")
      .filter({ hasText: displayName })
      .getByRole("link", { name: "Adopt revision 2" })
      .click();
    await expect(page).toHaveURL(
      new RegExp(`/analyzers/${analyzerId}/adoption\\?revision=2$`),
    );

    const changed = page
      .getByRole("heading", { name: "Changed" })
      .locator("xpath=ancestor::section[1]");
    await expect(
      changed.getByTestId("adoption-comparison-ADOPT-FIX"),
    ).toContainText(`New default: ${target!.name}`);
    await expect(
      page
        .getByRole("heading", { name: "Needs mapping" })
        .locator("xpath=ancestor::section[1]")
        .getByRole("button", { name: /^ADOPT-NEW\b/ }),
    ).toBeVisible();

    await page.getByRole("button", { name: "Save as revision 2" }).click();
    await expect(page).toHaveURL(
      new RegExp(`/analyzers/${analyzerId}/mapping$`),
    );
    await expect(
      page.getByText("Revision 2 adopted.", { exact: false }),
    ).toBeVisible();

    await page
      .getByRole("button", { name: "Confirm mappings and control recognition" })
      .click();
    await expect(
      page.getByText("Mappings and control recognition confirmed"),
    ).toBeVisible();
    await page
      .getByRole("button", { name: "Apply mappings and retry held results" })
      .click();
    await expect(
      page.getByText("Current mappings applied to this analyzer.", {
        exact: false,
      }),
    ).toBeVisible();

    const analyzer = await (
      await page.request.get(`${API}/analyzer/analyzers/${analyzerId}`)
    ).json();
    expect(analyzer.profileId).toBe(profileId);
    expect(analyzer.profileRevision).toBe(2);
  });
});
