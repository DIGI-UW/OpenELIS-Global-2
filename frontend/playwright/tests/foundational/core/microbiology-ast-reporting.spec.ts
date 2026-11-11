import { test } from "../../../helpers/test-base";
import { runMicrobiologyAstReportingWorkflow } from "../../../helpers/microbiology-ast-reporting-workflow";

test("selects one repeated agent, issues immutable partial versions, and locks the final case", async ({
  page,
}, testInfo) => {
  test.setTimeout(120_000);
  await runMicrobiologyAstReportingWorkflow(page, testInfo);
});
