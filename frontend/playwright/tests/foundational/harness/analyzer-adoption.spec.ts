import { randomInt, randomUUID } from "node:crypto";
import { expect, test } from "../../../helpers/test-base";
import { AnalyzerListPage } from "../../../fixtures/analyzer-list";
import { AnalyzerSetupPage } from "../../../fixtures/analyzer-setup";
import { seedNumericTests } from "../../../helpers/analyzer-catalog-api";
import {
  API,
  createProfile,
  numeric,
  publish,
  send,
  type Draft,
} from "../../../helpers/analyzer-profile-api";

test.describe("Adopting a newer profile revision", () => {
  test("an analyzer adopts the next revision: a fixed LOINC binds by default and a new code needs mapping", async ({
    page,
  }) => {
    const runId = randomUUID().slice(0, 8);
    const displayName = `Adoption bench ${runId}`;

    // A catalog test of the spec's own that the corrected LOINC finds, and two
    // LOINC codes no catalog test carries.
    const base = randomInt(1_000_000, 9_000_000);
    const [target] = await seedNumericTests(
      page,
      runId,
      [{ name: `E2E Adopt ${runId}`, loinc: `${base}-1` }],
      { testSection: "Molecular Biology", sampleType: "Sputum" },
    );
    const [wrongLoinc, newLoinc] = [`${base + 1}-2`, `${base + 2}-3`];

    const first = await createProfile(page, displayName, [
      numeric("ADOPT-FIX", wrongLoinc),
    ]);
    const profileId = first.profile.profileMeta.id;

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
      numeric("ADOPT-FIX", target.loinc),
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
    ).toContainText(`New default: ${target.name}`);
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
