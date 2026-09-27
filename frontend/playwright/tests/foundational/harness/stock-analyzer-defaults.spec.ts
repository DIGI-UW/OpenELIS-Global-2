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
    profileId: "fluorocycler-xt",
    sourceCode: "VIH-1",
    expectedTestName: "HIV Viral Load",
    expectedLoinc: "20447-9",
    specimenName: "Plasma",
  },
] as const;

test("shipped analyzer profiles resolve their intended clinical tests before any mapping edit", async ({
  page,
}) => {
  const response = await page.request.get(`${API}/analyzer-types`);
  expect(response.ok()).toBeTruthy();
  const catalog = (await response.json()) as {
    types: Array<{ profileId: string; revision: number; status: string; source: string }>;
  };

  for (const scenario of cases) {
    const active = catalog.types.filter(
      (type) =>
        type.profileId === scenario.profileId &&
        type.status === "ACTIVE" &&
        type.source === "SHIPPED",
    );
    expect(active, `One active shipped ${scenario.profileId} profile`).toHaveLength(1);
    await stockClinicalBinding(page, {
      ...scenario,
      profileRevision: active[0].revision,
    });
  }
});
