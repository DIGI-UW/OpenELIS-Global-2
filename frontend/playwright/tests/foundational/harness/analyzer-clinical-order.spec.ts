import { expect, test } from "../../../helpers/test-base";
import { createAnalyzerClinicalOrder } from "../../../helpers/analyzer-clinical-order";

test("the baseline GeneXpert MTB assay can be ordered through the ordinary clinical API", async ({
  page,
}) => {
  const response = await page.request.get(
    "/api/OpenELIS-Global/rest/analyzer-types",
  );
  expect(response.ok()).toBeTruthy();
  const payload = (await response.json()) as {
    types: Array<{
      profileId: string;
      revision: number;
      status: string;
      source: string;
    }>;
  };
  const matching = payload.types.filter(
    (type) =>
      type.profileId === "cepheid-genexpert-astm" &&
      type.status === "ACTIVE" &&
      type.source === "SHIPPED",
  );
  expect(matching, "One active shipped GeneXpert profile").toHaveLength(1);

  const order = await createAnalyzerClinicalOrder(page, {
    profileId: "cepheid-genexpert-astm",
    profileRevision: matching[0].revision,
    sourceCode: "MTB",
    expectedTestName: "Xpert MTB/RIF",
    expectedLoinc: "85362-2",
    specimenName: "Sputum",
  });
  expect(order.accession).toMatch(/^DEV01\d{15}$/);
});
