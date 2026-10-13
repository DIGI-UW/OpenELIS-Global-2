import { randomUUID } from "node:crypto";
import { expect, test } from "../../../helpers/test-base";
import { withAuthedPage } from "../../../helpers/api-session";
import {
  worklistFor,
  type Analyzer,
  type WorklistRow,
} from "../../../helpers/analyzer-api";
import { createClinicalOrder } from "../../../helpers/analyzer-clinical-order";
import { sendGeneXpertFixture } from "../../../helpers/analyzer-native-traffic";
import { activateShippedGeneXpert } from "../../../helpers/analyzer-setup-flow";
import { activeTestId } from "../../../helpers/analyzer-catalog-api";

/** The harness test each baseline GeneXpert assay code binds, and its specimen. */
const TESTS: Record<string, { name: string; specimen: string }> = {
  HIVVL: { name: "HIV-1 Viral Load", specimen: "Plasma" },
  SARSCOV2: { name: "SARS-CoV-2 PCR", specimen: "Nasopharyngeal Swab" },
  SARSCOV2_3: { name: "SARS-CoV-2 PCR", specimen: "Nasopharyngeal Swab" },
  FLUA: { name: "Influenza A PCR", specimen: "Nasopharyngeal Swab" },
  FLUB: { name: "Influenza B PCR", specimen: "Nasopharyngeal Swab" },
  RSV: { name: "RSV PCR", specimen: "Nasopharyngeal Swab" },
};

const HELD = "held: run_failed";
/** The cartridge's internal controls are flags on the sample, raised only when they fail. */
const SPC_FAILED = "Sample processing control failed";
const IQS_FAILED =
  "High internal quantitation standard failed, Low internal quantitation standard failed";
const NUMBER = expect.stringMatching(/^-?\d+(\.\d+)?$/);

/**
 * Every record staged for each assay code: "main" is the test's own result,
 * every other key a component's label. A run failure is held. The instrument's
 * Ct and EndPt readings are numbers that vary by fixture. The cartridge's internal
 * controls (SPC, IQS-H, IQS-L) are never staged as records.
 */
type Expected = Record<string, Record<string, unknown>>;

/** The Ct and EndPt readings the instrument reports for each target. */
const readings = (...targets: string[]) =>
  Object.fromEntries(
    targets.flatMap((target) => [
      [`${target} Ct`, NUMBER],
      [`${target} EndPt`, NUMBER],
    ]),
  );

const HIV_READINGS = readings("HIV-1");

const respiratory = (calls: Record<string, string>): Expected => {
  const analytes: Record<string, string[]> = {
    SARSCOV2: ["SARS-CoV-2"],
    SARSCOV2_3: ["SARS-CoV-2"],
    FLUA: ["Flu A 1", "Flu A 2"],
    FLUB: ["Flu B"],
    RSV: ["RSV"],
  };
  return Object.fromEntries(
    Object.entries(calls).map(([code, call]) => [
      code,
      {
        main: call,
        ...Object.fromEntries(analytes[code].map((label) => [label, call])),
        ...readings(...analytes[code]),
      },
    ]),
  );
};

const outcomes: Array<{
  assay: string;
  outcome: string;
  expected: Expected;
  flag?: string;
}> = [
  {
    assay: "hivvl",
    outcome: "quantified",
    expected: {
      HIVVL: {
        main: "1010",
        "Log viral load": "3.00",
        "HIV-1": "Positive",
        ...HIV_READINGS,
        "HIV-1 Delta Ct": NUMBER,
      },
    },
  },
  {
    assay: "hivvl",
    outcome: "below-40",
    expected: {
      HIVVL: {
        main: "<40.00",
        "HIV-1 call": "Detected",
        "HIV-1": "Positive",
        ...HIV_READINGS,
        "HIV-1 Delta Ct": NUMBER,
      },
    },
  },
  {
    assay: "hivvl",
    outcome: "above-1e7",
    expected: {
      HIVVL: {
        main: ">10000000.00",
        "HIV-1 call": "Detected",
        "HIV-1": "Positive",
        ...HIV_READINGS,
        "HIV-1 Delta Ct": NUMBER,
      },
    },
  },
  {
    assay: "hivvl",
    outcome: "not-detected",
    expected: {
      HIVVL: {
        "HIV-1 call": "Not detected",
        "HIV-1": "Negative",
        ...HIV_READINGS,
      },
    },
  },
  {
    assay: "hivvl",
    outcome: "invalid",
    expected: {
      HIVVL: {
        "HIV-1 call": "Invalid",
        "HIV-1": "Invalid",
        ...HIV_READINGS,
      },
    },
    flag: IQS_FAILED,
  },
  {
    assay: "hivvl",
    outcome: "error",
    expected: {
      HIVVL: {
        main: HELD,
        "HIV-1": HELD,
        ...HIV_READINGS,
      },
    },
  },
  ...(["cov-flu-rsv-plus", "cov-flu-plus"] as const).flatMap((assay) => {
    const rsv = assay === "cov-flu-rsv-plus";
    const panel = (
      sars: string,
      fluA: string,
      fluB: string,
      rsvCall: string,
    ) => ({
      SARSCOV2: sars,
      FLUA: fluA,
      FLUB: fluB,
      ...(rsv ? { RSV: rsvCall } : {}),
    });
    const [P, N, I] = ["Positive", "Negative", "Invalid"];
    return [
      {
        assay,
        outcome: "all-positive",
        expected: respiratory(panel(P, P, P, P)),
      },
      {
        assay,
        outcome: "negative-all",
        expected: respiratory(panel(N, N, N, N)),
      },
      {
        assay,
        outcome: "sars-cov-2-positive",
        expected: respiratory(panel(P, N, N, N)),
      },
      {
        assay,
        outcome: "flu-a-positive",
        expected: respiratory(panel(N, P, N, N)),
      },
      {
        assay,
        outcome: "flu-b-positive",
        expected: respiratory(panel(N, N, P, N)),
      },
      {
        assay,
        outcome: "invalid",
        expected: respiratory(panel(I, I, I, I)),
        flag: SPC_FAILED,
      },
      {
        assay,
        outcome: "error",
        expected: respiratory(panel(HELD, HELD, HELD, HELD)),
      },
      ...(rsv
        ? [
            {
              assay,
              outcome: "rsv-positive",
              expected: respiratory(panel(N, N, N, P)),
            },
            {
              assay,
              outcome: "no-result",
              expected: respiratory(panel(HELD, HELD, HELD, HELD)),
            },
          ]
        : []),
    ];
  }),
  {
    assay: "cov-plus",
    outcome: "positive",
    expected: respiratory({ SARSCOV2_3: "Positive" }),
  },
  {
    assay: "cov-plus",
    outcome: "negative",
    expected: respiratory({ SARSCOV2_3: "Negative" }),
  },
];

type StagedRow = WorklistRow & {
  componentId?: string | null;
  componentLabel?: string | null;
  result: string;
  dictionaryResultList?: Array<{ id: string; displayValue: string }>;
  instrumentNote?: string | null;
  instrumentSampleFlags?: string | null;
  placement?: { state: string };
};

const read = (row: StagedRow) =>
  row.importIssueReason
    ? `held: ${row.importIssueReason}`
    : (row.dictionaryResultList?.find((option) => option.id == row.result)
        ?.displayValue ?? row.result);

test.describe("Every documented GeneXpert outcome", () => {
  const run = randomUUID().slice(0, 8);
  const senderId = `GX-OUT-${run}`;
  let analyzer: Analyzer;

  test.beforeAll(async ({ browser }) => {
    analyzer = await withAuthedPage(browser, (page) =>
      activateShippedGeneXpert(page, `Outcomes GeneXpert ${run}`, senderId),
    );
  });

  for (const { assay, outcome, expected, flag } of outcomes) {
    test(`${assay} ${outcome} lands on its tests and components, its internal control a flag`, async ({
      page,
    }) => {
      const codes = Object.keys(expected);
      const specimen = TESTS[codes[0]].specimen;
      const ordered = new Map<string, string>();
      for (const code of codes) {
        ordered.set(code, await activeTestId(page, TESTS[code].name, specimen));
      }
      const order = await createClinicalOrder(page, {
        testIds: [...new Set(ordered.values())],
        specimenName: specimen,
      });
      await sendGeneXpertFixture(
        page,
        analyzer.id,
        order.accession,
        { assay, outcome },
        senderId,
      );

      let rows: StagedRow[] = [];
      await expect
        .poll(async () => {
          rows = await worklistFor<StagedRow>(
            page,
            analyzer.id,
            order.accession,
          );
          return new Set(rows.map((row) => row.rawTestCode)).size;
        })
        .toBe(codes.length);

      const actual: Expected = {};
      for (const row of rows) {
        expect(row.testId, `${row.rawTestCode} lands on its ordered test`).toBe(
          ordered.get(row.rawTestCode),
        );
        const record = row.componentId ? row.componentLabel! : "main";
        actual[row.rawTestCode] ??= {};
        actual[row.rawTestCode][record] = read(row);
        expect(row.instrumentSampleFlags ?? null, "the sample's flags").toBe(
          flag ?? null,
        );
        if (row.importIssueReason && !row.componentId) {
          // The instrument's note follows the test's own record; its parts carry none.
          expect(
            row.instrumentNote ?? "",
            "A held result keeps the instrument's note",
          ).not.toBe("");
        } else if (!row.importIssueReason) {
          expect(row.placement?.state).toBe("RESOLVED");
        }
      }
      expect(actual).toEqual(expected);
    });
  }
});
