import { expect, test } from "../../../helpers/test-base";
import { stockClinicalBinding } from "../../../helpers/analyzer-clinical-order";

const API = "/api/OpenELIS-Global/rest";

const cases = [
  {
    profileId: "genexpert-astm",
    sourceCode: "MTB-RIF",
    expectedTestName: "Xpert MTB/RIF",
    expectedLoinc: "85362-2",
    specimenName: "Sputum",
    expectedMappedValue: "NOT DETECTED",
  },
  {
    profileId: "genexpert-astm",
    sourceCode: "RIF",
    expectedTestName: "Xpert RIF Resistance",
    expectedLoinc: "46244-0",
    specimenName: "Sputum",
    expectedMappedValue: "DETECTED",
  },
] as const;

for (const scenario of cases) {
  test(`${scenario.profileId} ${scenario.sourceCode} resolves its shipped clinical default`, async ({
    page,
  }) => {
    const response = await page.request.get(`${API}/analyzer-types`);
    expect(response.ok()).toBeTruthy();
    const catalog = (await response.json()) as {
      types: Array<{
        profileId: string;
        revision: number;
        status: string;
        source: string;
      }>;
    };
    const active = catalog.types.filter(
      (type) =>
        type.profileId === scenario.profileId &&
        type.status === "ACTIVE" &&
        type.source === "SHIPPED",
    );
    expect(
      active,
      `One active shipped ${scenario.profileId} profile`,
    ).toHaveLength(1);
    await stockClinicalBinding(page, {
      ...scenario,
      profileRevision: active[0].revision,
    });
  });
}
