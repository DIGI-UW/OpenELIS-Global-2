import { randomUUID } from "node:crypto";
import type { Page } from "@playwright/test";
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
import { API } from "../../../helpers/analyzer-profile-api";

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
const SPC = "Sample processing control";

/**
 * What a reviewer reads for each record, by assay code: "main" is the test's
 * own result, every other key a component's label. A run failure is held.
 */
type Expected = Record<string, Record<string, string>>;

const respiratory = (
  calls: Record<string, string>,
  control: string,
): Expected => {
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
        ...(call === HELD
          ? {}
          : Object.fromEntries(analytes[code].map((label) => [label, call]))),
        [SPC]: control,
      },
    ]),
  );
};

const outcomes: Array<{ assay: string; outcome: string; expected: Expected }> =
  [
    {
      assay: "hivvl",
      outcome: "quantified",
      expected: {
        HIVVL: {
          main: "1010",
          "Log viral load": "3.00",
          "HIV-1": "Positive",
          "IQS-H": "Pass",
          "IQS-L": "Pass",
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
          "IQS-H": "Pass",
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
          "IQS-H": "Pass",
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
          "IQS-H": "Pass",
          "IQS-L": "Pass",
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
          "IQS-H": "Fail",
          "IQS-L": "Fail",
        },
      },
    },
    {
      assay: "hivvl",
      outcome: "error",
      expected: {
        HIVVL: { main: HELD, "HIV-1": HELD, "IQS-H": HELD, "IQS-L": HELD },
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
          expected: respiratory(panel(P, P, P, P), "Not applicable"),
        },
        {
          assay,
          outcome: "negative-all",
          expected: respiratory(panel(N, N, N, N), "Pass"),
        },
        {
          assay,
          outcome: "sars-cov-2-positive",
          expected: respiratory(panel(P, N, N, N), "Not applicable"),
        },
        {
          assay,
          outcome: "flu-a-positive",
          expected: respiratory(panel(N, P, N, N), "Not applicable"),
        },
        {
          assay,
          outcome: "flu-b-positive",
          expected: respiratory(panel(N, N, P, N), "Not applicable"),
        },
        {
          assay,
          outcome: "invalid",
          expected: respiratory(panel(I, I, I, I), "Fail"),
        },
        {
          assay,
          outcome: "error",
          expected: respiratory(panel(HELD, HELD, HELD, HELD), HELD),
        },
        ...(rsv
          ? [
              {
                assay,
                outcome: "rsv-positive",
                expected: respiratory(panel(N, N, N, P), "Not applicable"),
              },
              {
                assay,
                outcome: "no-result",
                expected: respiratory(panel(HELD, HELD, HELD, HELD), HELD),
              },
            ]
          : []),
      ];
    }),
    {
      assay: "cov-plus",
      outcome: "positive",
      expected: respiratory({ SARSCOV2_3: "Positive" }, "Not applicable"),
    },
    {
      assay: "cov-plus",
      outcome: "negative",
      expected: respiratory({ SARSCOV2_3: "Negative" }, "Pass"),
    },
  ];

type StagedRow = WorklistRow & {
  componentId?: string | null;
  componentLabel?: string | null;
  result: string;
  dictionaryResultList?: Array<{ id: string; displayValue: string }>;
  instrumentNote?: string | null;
  placement?: { state: string };
};

const read = (row: StagedRow) =>
  row.importIssueReason
    ? `held: ${row.importIssueReason}`
    : (row.dictionaryResultList?.find((option) => option.id == row.result)
        ?.displayValue ?? row.result);

/** The catalog test of this name on this specimen. */
async function testId(page: Page, name: string, specimen: string) {
  const response = await page.request.get(
    `${API}/test-catalog/tests?status=active&search=${encodeURIComponent(name)}&pageSize=100`,
  );
  const rows = (
    (await response.json()) as {
      rows: Array<{ testId: string; name: string; sampleTypes: string[] }>;
    }
  ).rows.filter(
    (row) =>
      (row.name === name || row.name.startsWith(`${name}(`)) &&
      row.sampleTypes.includes(specimen),
  );
  expect(rows, `One active ${name} on ${specimen}`).toHaveLength(1);
  return rows[0].testId;
}

test.describe("Every documented GeneXpert outcome", () => {
  const run = randomUUID().slice(0, 8);
  const senderId = `GX-OUT-${run}`;
  let analyzer: Analyzer;

  test.beforeAll(async ({ browser }) => {
    test.setTimeout(180_000);
    analyzer = await withAuthedPage(browser, (page) =>
      activateShippedGeneXpert(page, `Outcomes GeneXpert ${run}`, senderId),
    );
  });

  for (const { assay, outcome, expected } of outcomes) {
    test(`${assay} ${outcome} lands on its tests, components and controls`, async ({
      page,
    }) => {
      const codes = Object.keys(expected);
      const specimen = TESTS[codes[0]].specimen;
      const ordered = new Map<string, string>();
      for (const code of codes) {
        ordered.set(code, await testId(page, TESTS[code].name, specimen));
      }
      const order = await createClinicalOrder(page, {
        testIds: [...new Set(ordered.values())],
        specimenName: specimen,
      });
      await sendGeneXpertFixture(
        page.request,
        analyzer.bridgeConnectionId,
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
      for (const [code, records] of Object.entries(expected)) {
        expect(actual[code], code).toMatchObject(records);
      }
    });
  }
});
