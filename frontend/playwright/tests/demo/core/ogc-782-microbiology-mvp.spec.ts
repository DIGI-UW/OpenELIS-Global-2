import { test } from "../../../helpers/test-base";
import { createDemoPresentation } from "../../../helpers/demo-presentation";
import { runMicrobiologyAstReportingWorkflow } from "../../../helpers/microbiology-ast-reporting-workflow";

test("susceptibility entry, per-drug repeat selection, immutable reports, and patient PDF", async ({
  page,
}, testInfo) => {
  test.setTimeout(180_000);
  const demo = createDemoPresentation(page, testInfo);
  await demo.title(
    "Microbiology susceptibility testing and reporting",
    "Validate readings, select a repeat, preserve issued versions, and print the patient report",
  );
  await runMicrobiologyAstReportingWorkflow(
    page,
    testInfo,
    async (step, description) => {
      await demo.step(step, description);
      await demo.evidence(`microbiology-reporting-${step}`, { fullPage: true });
    },
  );
});
