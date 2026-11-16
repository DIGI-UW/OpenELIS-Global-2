import React from "react";
import { fireEvent, render, screen, within } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../../languages/en.json";

const { utilsMock } = vi.hoisted(() => ({
  utilsMock: {
    getFromOpenElisServer: vi.fn(),
    postToOpenElisServerJsonResponse: vi.fn(),
  },
}));

vi.mock("../../../utils/Utils", () => utilsMock);

import RunQcBlock, { RunQc } from "./RunQcBlock";
import WaitingTag from "./WaitingTag";

const HELD: RunQc = {
  overall: "HELD",
  tests: [
    {
      testId: "11",
      testName: "Glucose",
      verdict: "FAIL",
      controls: [
        {
          id: "c1",
          level: "NORMAL",
          lotNumber: "LOT-N",
          observed: "120",
          expected: "100 ± 5",
          verdict: "FAIL",
          verdictSource: "COMPUTED",
          operator: "Ana Lab",
        },
      ],
    },
    {
      testId: "12",
      testName: "Urea",
      verdict: "NOT_REQUIRED",
      controls: [],
    },
    {
      testId: "13",
      testName: "Malaria RDT",
      verdict: "FAIL",
      controls: [
        {
          id: "c2",
          level: "KIT-1",
          observed: "INVALID",
          verdict: "FAIL",
          verdictSource: "DECLARED",
        },
      ],
    },
  ],
};

const renderBlock = (onRecorded = vi.fn()) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <RunQcBlock runId={7} qc={HELD} onRecorded={onRecorded} />
    </IntlProvider>,
  );

describe("RunQcBlock (OGC-1200 slice 4)", () => {
  beforeEach(() => {
    utilsMock.getFromOpenElisServer.mockReset();
    utilsMock.postToOpenElisServerJsonResponse.mockReset();
    utilsMock.getFromOpenElisServer.mockImplementation(
      (url: string, callback: (body: unknown) => void) =>
        callback([{ id: "lot-n", lotNumber: "LOT-N", controlLevel: "NORMAL" }]),
    );
  });

  it("shows the run held and only the failed test as held, with its control against the target", () => {
    renderBlock();

    expect(screen.getByTestId("run-qc-overall")).toHaveTextContent("Held");
    const glucose = screen.getByTestId("run-qc-test-11");
    expect(glucose).toHaveTextContent("Fail");
    expect(glucose).toHaveTextContent("This test's rows in the run are held.");
    expect(within(glucose).getByTestId("run-qc-control-c1")).toHaveTextContent(
      "120 against 100 ± 5",
    );
    expect(within(glucose).getByTestId("run-qc-control-c1")).toHaveTextContent(
      "Judged against the QC target",
    );
    expect(screen.getByTestId("run-qc-control-c2")).toHaveTextContent(
      "KIT-1 · Invalid · Declared by the technician",
    );
    const urea = screen.getByTestId("run-qc-test-12");
    expect(urea).toHaveTextContent("Not required");
    expect(urea).not.toHaveTextContent("held");
  });

  it("records a measured control on the run and hands back the new QC block", () => {
    const updated: RunQc = { ...HELD, overall: "HELD" };
    utilsMock.postToOpenElisServerJsonResponse.mockImplementation(
      (url: string, body: string, callback: (response: unknown) => void) =>
        callback(updated),
    );
    const onRecorded = vi.fn();
    renderBlock(onRecorded);

    fireEvent.click(screen.getByTestId("run-qc-record"));
    fireEvent.change(screen.getByLabelText("Control lot"), {
      target: { value: "lot-n" },
    });
    fireEvent.change(screen.getByLabelText("Measured value"), {
      target: { value: "99" },
    });
    fireEvent.click(screen.getByLabelText("Pass"));
    fireEvent.click(screen.getByTestId("run-qc-save"));

    const [url, body] =
      utilsMock.postToOpenElisServerJsonResponse.mock.calls[0];
    expect(url).toBe("/rest/result-runs/7/controls");
    expect(JSON.parse(body)).toMatchObject({
      source: "MANUAL",
      testId: "11",
      controlLotId: "lot-n",
      resultValue: "99",
      qualitativeOutcome: "PASS",
    });
    expect(onRecorded).toHaveBeenCalledWith(updated);
  });

  it("says what is missing instead of posting an incomplete control", () => {
    renderBlock();

    fireEvent.click(screen.getByTestId("run-qc-record"));
    fireEvent.click(screen.getByTestId("run-qc-save"));

    expect(utilsMock.postToOpenElisServerJsonResponse).not.toHaveBeenCalled();
    expect(
      screen.getByText("Complete the control result fields before recording."),
    ).toBeInTheDocument();
  });
});

describe("WaitingTag on a held row (FR-D4)", () => {
  it("says the row is held because QC failed in the run", () => {
    render(
      <IntlProvider locale="en" messages={messages}>
        <WaitingTag
          run={{ runId: 7, label: "RUN-7", source: "ANALYZER", held: true }}
        />
      </IntlProvider>,
    );
    expect(screen.getByTestId("held-run-7")).toHaveTextContent(
      "Held: QC failed in RUN-7",
    );
  });
});
