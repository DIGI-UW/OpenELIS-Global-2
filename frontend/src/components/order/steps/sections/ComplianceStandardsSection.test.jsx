import React, { useState } from "react";
import { act, fireEvent, render, screen, within } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import messages from "../../../../languages/en.json";
import { getFromOpenElisServer } from "../../../utils/Utils";
import ComplianceStandardsSection from "./ComplianceStandardsSection";

vi.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
}));

const STANDARDS = [
  {
    id: "1",
    name: "Drinking Water Quality",
    regulationNumber: "PP 22/2021",
    issuingBody: "Ministry of Health",
    version: "2021",
    effectiveDate: "2021-02-01",
    sampleTypes: ["Drinking Water"],
  },
  {
    id: "2",
    name: "Guidelines for Drinking-water Quality",
    regulationNumber: "WHO-DWG-4",
    issuingBody: "WHO",
    version: "4",
    effectiveDate: [2022, 3, 21],
    sampleTypes: [],
  },
];
const cover = (id) => ({
  standardId: id,
  regulationNumber: STANDARDS[Number(id) - 1].regulationNumber,
});
const SUGGESTED = [
  {
    testId: "31",
    testName: "BOD",
    sampleTypeIds: ["8"],
    coveredBy: [cover("2")],
    thresholds: [
      {
        standardId: "2",
        regulationNumber: "WHO-DWG-4",
        type: "MAXIMUM",
        max: 3,
        units: "mg/L",
      },
    ],
    conflicting: false,
  },
  {
    testId: "32",
    testName: "Lead (Pb)",
    sampleTypeIds: ["7"],
    coveredBy: [cover("1")],
    thresholds: [
      {
        standardId: "1",
        regulationNumber: "PP 22/2021",
        type: "MAXIMUM",
        max: 0.01,
        units: "mg/L",
      },
    ],
    conflicting: false,
  },
  {
    testId: "33",
    testName: "pH",
    loinc: "11558-4",
    sampleTypeIds: ["7"],
    coveredBy: [cover("1"), cover("2")],
    thresholds: [
      {
        standardId: "1",
        regulationNumber: "PP 22/2021",
        type: "RANGE",
        min: 6,
        max: 9,
      },
      {
        standardId: "2",
        regulationNumber: "WHO-DWG-4",
        type: "RANGE",
        min: 6.5,
        max: 8.5,
      },
    ],
    conflicting: true,
  },
];

let suggestedResponse;
let state;
let planCalls;
const Harness = ({ standards = '["1","2"]', samples: initialSamples }) => {
  const [orderData, setOrderData] = useState({
    sampleOrderItems: {
      environmentalFields: {
        orderBranch: "REGULATION_DRIVEN",
        complianceStandards: standards,
      },
    },
  });
  const [samples, setSamples] = useState(initialSamples);
  const [plan, setPlan] = useState(null);
  state = { orderData, samples, plan };
  return (
    <IntlProvider locale="en" messages={messages}>
      <ComplianceStandardsSection
        orderData={orderData}
        setOrderData={setOrderData}
        isReadOnly={false}
        samples={samples}
        setSamples={setSamples}
        onPlanChange={(next) => {
          planCalls.push(next);
          setPlan(next);
        }}
      />
    </IntlProvider>
  );
};

beforeEach(() => {
  suggestedResponse = SUGGESTED;
  planCalls = [];
  vi.mocked(getFromOpenElisServer).mockReset();
  vi.mocked(getFromOpenElisServer).mockImplementation((url, callback) => {
    if (url === "/rest/compliance/standards/active") callback(STANDARDS);
    else if (url === "/rest/environmental-sample-types")
      callback([
        { id: "7", value: "Drinking Water" },
        { id: "8", value: "Surface Water" },
      ]);
    else if (url.startsWith("/rest/compliance/standards/suggested-tests"))
      callback(suggestedResponse);
  });
});

describe("Regulation-driven environmental order (OGC-768)", () => {
  it("shows the descriptive requirements in the conflict comparison and standard cards", () => {
    suggestedResponse = [
      {
        ...SUGGESTED[2],
        thresholds: ["Absent", "Present"].map((valueDescriptive, index) => ({
          ...cover(String(index + 1)),
          type: "DESCRIPTIVE",
          valueDescriptive,
        })),
      },
    ];
    render(<Harness samples={[]} />);

    const comparison = screen.getByTestId("compliance-plan-limits-33");
    expect(comparison).toHaveTextContent("PP 22/2021Absent");
    expect(comparison).toHaveTextContent("WHO-DWG-4Present");
    expect(
      screen.getByTestId("compliance-standard-thresholds-1"),
    ).toHaveTextContent("pH: Absent");
    expect(
      screen.getByTestId("compliance-standard-thresholds-2"),
    ).toHaveTextContent("pH: Present");
  });

  it("shows a card per standard and a test plan with every suggested test once", () => {
    render(<Harness samples={[{ index: 0, sampleTypeId: "7", tests: [] }]} />);

    expect(getFromOpenElisServer).toHaveBeenCalledWith(
      "/rest/compliance/standards/suggested-tests?standardIds=1%2C2",
      expect.any(Function),
    );
    expect(
      screen.getByTestId("compliance-standard-regulation-1"),
    ).toHaveTextContent("PP 22/2021");
    expect(screen.getByTestId("compliance-standard-card-2")).toHaveTextContent(
      "Issued by WHO, version 4, effective 2022-03-21",
    );
    expect(screen.getByTestId("compliance-standard-card-2")).toHaveTextContent(
      "Linked tests: 2",
    );
    expect(
      screen.getByTestId("compliance-test-plan-summary"),
    ).toHaveTextContent(
      "Tests in the plan: 3, from PP 22/2021 + WHO-DWG-4 (1 shared)",
    );
    const ph = screen.getByTestId("compliance-plan-test-33");
    expect(ph).toHaveTextContent("Limits differ");
    expect(screen.getByTestId("compliance-plan-limits-33")).toHaveTextContent(
      "PP 22/20216 to 9",
    );
    expect(screen.getByTestId("compliance-plan-limits-33")).toHaveTextContent(
      "WHO-DWG-46.5 to 8.5",
    );
    expect(state.plan).toEqual(["31", "32", "33"]);
  });

  it("lists a plan test no sample can run and adds a sample of its type or skips it", () => {
    render(<Harness samples={[{ index: 0, sampleTypeId: "7", tests: [] }]} />);

    const blocked = screen.getByTestId("compliance-plan-blocked");
    expect(blocked).toHaveTextContent("Plan tests no sample can run yet: 1");
    expect(blocked).toHaveTextContent(
      "BOD needs a sample of type Surface Water",
    );

    fireEvent.click(
      within(blocked).getByRole("button", {
        name: "Add a Surface Water sample",
      }),
    );
    expect(state.samples.map((sample) => sample.sampleTypeId)).toEqual([
      "7",
      "8",
    ]);
    expect(screen.queryByTestId("compliance-plan-blocked")).toBeNull();
  });

  it("drops a skipped or unticked test from the plan", () => {
    render(<Harness samples={[{ index: 0, sampleTypeId: "7", tests: [] }]} />);

    fireEvent.click(
      within(screen.getByTestId("compliance-plan-blocked")).getByRole(
        "button",
        {
          name: "Skip this test",
        },
      ),
    );
    expect(state.plan).toEqual(["32", "33"]);
    expect(screen.getByLabelText("BOD")).not.toBeChecked();

    fireEvent.click(screen.getByLabelText("pH"));
    expect(state.plan).toEqual(["32"]);
  });

  it("fills an empty sample row before adding new ones", () => {
    render(<Harness samples={[{ index: 0, sampleTypeId: "", tests: [] }]} />);

    fireEvent.click(screen.getByTestId("compliance-quick-add-7"));

    expect(state.samples.map((sample) => sample.sampleTypeId)).toEqual(["7"]);
  });

  it("adds several samples of a standard's sample type at once", () => {
    render(<Harness samples={[]} />);

    fireEvent.change(
      screen.getByLabelText("Number of Drinking Water samples"),
      {
        target: { value: "3" },
      },
    );
    fireEvent.click(screen.getByTestId("compliance-quick-add-7"));

    expect(state.samples.map((sample) => sample.sampleTypeId)).toEqual([
      "7",
      "7",
      "7",
    ]);
    expect(state.samples.map((sample) => sample.index)).toEqual([0, 1, 2]);
  });

  it("removes a standard from its card", () => {
    render(<Harness samples={[]} />);

    fireEvent.click(screen.getByTestId("compliance-standard-remove-2"));

    expect(
      state.orderData.sampleOrderItems.environmentalFields.complianceStandards,
    ).toBe('["1"]');
  });

  it("reports no plan until the plan of the chosen standards has loaded", () => {
    let answer;
    vi.mocked(getFromOpenElisServer).mockImplementation((url, callback) => {
      if (url === "/rest/compliance/standards/active") callback(STANDARDS);
      else if (url === "/rest/environmental-sample-types") callback([]);
      else if (url.startsWith("/rest/compliance/standards/suggested-tests"))
        answer = callback;
    });
    render(<Harness samples={[]} />);

    expect(planCalls.every((call) => call === null)).toBe(true);
    expect(state.plan).toBeNull();

    act(() => answer(SUGGESTED));
    expect(state.plan).toEqual(["31", "32", "33"]);
  });

  it("reports no plan when the plan cannot be loaded", () => {
    suggestedResponse = null;
    render(<Harness samples={[]} />);

    expect(planCalls.every((call) => call === null)).toBe(true);
  });

  it("says a plan test has no environmental sample type and offers only to skip it", () => {
    suggestedResponse = [
      { ...SUGGESTED[1], testId: "34", testName: "Glucose", sampleTypeIds: [] },
    ];
    render(<Harness samples={[]} />);

    const blocked = screen.getByTestId("compliance-plan-blocked-34");
    expect(blocked).toHaveTextContent(
      "Glucose has no environmental sample type that runs it",
    );
    expect(
      within(blocked).queryByRole("button", { name: /^Add a/ }),
    ).toBeNull();
    expect(
      within(blocked).getByRole("button", { name: "Skip this test" }),
    ).toBeInTheDocument();
  });

  it("says when the test plan cannot be loaded and retries", () => {
    suggestedResponse = null;
    render(<Harness samples={[]} />);
    const error = screen.getByTestId("compliance-test-plan-error");
    expect(error).toHaveTextContent("The test plan could not be loaded");

    suggestedResponse = SUGGESTED;
    fireEvent.click(within(error).getByRole("button", { name: "Retry" }));

    expect(screen.queryByTestId("compliance-test-plan-error")).toBeNull();
    expect(
      screen.getByTestId("compliance-test-plan-summary"),
    ).toHaveTextContent("Tests in the plan: 3");
  });
});
