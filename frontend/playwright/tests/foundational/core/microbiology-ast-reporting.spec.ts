import { test } from "../../../helpers/test-base";
import { runMicrobiologyAstReportingWorkflow } from "../../../helpers/microbiology-ast-reporting-workflow";
import { createDemoPresentation } from "../../../helpers/demo-presentation";

const CHECKPOINT_CAPTIONS: Record<number, string> = {
  1: "Readings for both drugs are entered and the run is validated.",
  2: "A first partial report is issued. A repeat run is started with a reason.",
  3: "The repeat's resistant reading is used for one drug only, and a second partial report is issued.",
  4: "The first issued version still shows both drugs susceptible: an issued report never changes.",
  5: "The final report is released, and the case no longer offers new or repeat runs.",
  6: "The printed report shows the chosen readings and the note sent with the result, not the internal note.",
};

test(
  "selects one repeated agent, issues immutable partial versions, and locks the final case",
  { tag: ["@evidence", "@microbiology", "@ast"] },
  async ({ page }, testInfo) => {
    test.setTimeout(120_000);
    const demo = createDemoPresentation(page, testInfo);
    await demo.intro(
      "Susceptibility results through immutable reports",
      "A repeat result replaces one drug's reading on the report, partial reports stay as issued, and the final release locks the case.",
    );
    await demo.caption(
      "Off screen: an identified isolate is ready for AST, with its panel and breakpoints configured. Two case notes are added, one sent with the result.",
    );
    await runMicrobiologyAstReportingWorkflow(page, testInfo, (step) =>
      demo.caption(CHECKPOINT_CAPTIONS[step]),
    );
    await demo.verified(
      "Each issued version is preserved, and the final case is locked",
      "The report shows the repeat's resistant reading for one drug and the original susceptible reading for the other.",
    );
  },
);
