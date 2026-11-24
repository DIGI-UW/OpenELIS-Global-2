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
      holdCause: "QC_FAILED",
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
      holdCause: "QC_FAILED",
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

describe("Deciding a held test (OGC-1200 FR-C5)", () => {
  beforeEach(() => {
    utilsMock.postToOpenElisServerJsonResponse.mockReset();
  });

  it("asks for a reason before accepting despite the QC failure, then files it", () => {
    const decided: RunQc = {
      overall: "DECIDED",
      tests: [
        {
          ...HELD.tests[0],
          decision: "ACCEPT_DESPITE",
          nceNumber: "NCE-26-0007",
        },
      ],
    };
    utilsMock.postToOpenElisServerJsonResponse.mockImplementation(
      (url: string, body: string, callback: (response: unknown) => void) =>
        callback(decided),
    );
    const onRecorded = vi.fn();
    renderBlock(onRecorded);

    const decide = screen.getByTestId("run-qc-decide-11");
    fireEvent.click(
      within(decide).getByRole("button", { name: /Accept despite QC failure/ }),
    );
    fireEvent.click(screen.getByTestId("run-qc-decide-confirm-11"));
    expect(utilsMock.postToOpenElisServerJsonResponse).not.toHaveBeenCalled();
    expect(
      screen.getByText(
        "Give the reason the results are accepted despite the QC failure.",
      ),
    ).toBeInTheDocument();

    const reason = screen.getByLabelText(/^Reason/);
    expect(reason).toHaveAttribute("aria-required", "true");
    expect(reason.closest(".cds--form-item")).toHaveTextContent("Reason *");
    fireEvent.change(reason, {
      target: { value: "Control material expired" },
    });
    fireEvent.click(screen.getByTestId("run-qc-decide-confirm-11"));

    const [url, body] =
      utilsMock.postToOpenElisServerJsonResponse.mock.calls[0];
    expect(url).toBe("/rest/result-runs/7/tests/11/decision");
    expect(JSON.parse(body)).toEqual({
      action: "ACCEPT_DESPITE",
      reason: "Control material expired",
      fileNce: true,
    });
    expect(onRecorded).toHaveBeenCalledWith(decided);
  });

  it("rejects with a non-conformity only when asked for one", () => {
    utilsMock.postToOpenElisServerJsonResponse.mockImplementation(
      (url: string, body: string, callback: (response: unknown) => void) =>
        callback(HELD),
    );
    renderBlock();

    fireEvent.click(
      within(screen.getByTestId("run-qc-decide-11")).getByRole("button", {
        name: "Reject",
      }),
    );
    fireEvent.click(screen.getByLabelText("File a non-conformity"));
    fireEvent.click(screen.getByTestId("run-qc-decide-confirm-11"));

    expect(
      JSON.parse(utilsMock.postToOpenElisServerJsonResponse.mock.calls[0][1]),
    ).toEqual({ action: "REJECT", reason: null, fileNce: true });
  });

  it("shows what was decided instead of the actions", () => {
    render(
      <IntlProvider locale="en" messages={messages}>
        <RunQcBlock
          runId={7}
          qc={{
            overall: "DECIDED",
            tests: [{ ...HELD.tests[0], decision: "REJECT", nceNumber: null }],
          }}
          onRecorded={vi.fn()}
        />
      </IntlProvider>,
    );

    expect(screen.getByTestId("run-qc-overall")).toHaveTextContent(
      "QC failed: decided",
    );
    expect(screen.getByTestId("run-qc-decision-11")).toHaveTextContent(
      "Reject",
    );
    expect(screen.queryByTestId("run-qc-decide-11")).not.toBeInTheDocument();
  });

  it("offers Record control only for tests not yet decided on the run", () => {
    render(
      <IntlProvider locale="en" messages={messages}>
        <RunQcBlock
          runId={7}
          qc={{
            overall: "HELD",
            tests: [
              { ...HELD.tests[0], decision: "RETEST", nceNumber: null },
              HELD.tests[1],
              HELD.tests[2],
            ],
          }}
          onRecorded={vi.fn()}
        />
      </IntlProvider>,
    );

    fireEvent.click(screen.getByTestId("run-qc-record"));
    const choices = within(screen.getByLabelText("Test"))
      .getAllByRole("option")
      .map((option) => option.textContent);
    expect(choices).toEqual(["Urea", "Malaria RDT"]);
    expect(screen.getByLabelText("Test")).toHaveValue("12");
  });

  it("offers no Record control once every test on the run is decided", () => {
    render(
      <IntlProvider locale="en" messages={messages}>
        <RunQcBlock
          runId={7}
          qc={{
            overall: "DECIDED",
            tests: [{ ...HELD.tests[0], decision: "REJECT", nceNumber: null }],
          }}
          onRecorded={vi.fn()}
        />
      </IntlProvider>,
    );

    expect(screen.getByTestId("run-qc-record")).toBeDisabled();
  });
});

describe("RunQcBlock control times", () => {
  it("shows when a control was recorded in the site date format", () => {
    utilsMock.getFromOpenElisServer.mockImplementation(
      (url: string, callback: (body: unknown) => void) => callback([]),
    );
    render(
      <IntlProvider locale="en" messages={messages}>
        <RunQcBlock
          runId={7}
          qc={{
            overall: "HELD",
            tests: [
              {
                testId: "11",
                testName: "Glucose",
                verdict: "FAIL",
                holdCause: "QC_FAILED",
                controls: [
                  {
                    id: "c9",
                    level: "KIT-1",
                    observed: "INVALID",
                    verdict: "FAIL",
                    verdictSource: "DECLARED",
                    operator: "Ana Lab",
                    recordedAt: "2026-11-24T09:00:00Z",
                  },
                ],
              },
            ],
          }}
          onRecorded={vi.fn()}
        />
      </IntlProvider>,
    );

    expect(screen.getByTestId("run-qc-control-c9")).toHaveTextContent(
      "11/24/2026",
    );
  });
});

describe("RunQcBlock retest links (OGC-1200 slice 5b)", () => {
  const LATER: RunQc = {
    overall: "PASS",
    tests: [
      {
        testId: "11",
        testName: "Glucose",
        verdict: "NOT_REQUIRED",
        controls: [],
        retestSuggestions: [{ runId: 5, label: "RUN-5", count: 1, total: 2 }],
      },
    ],
  };
  const renderLinks = (qc: RunQc, onRecorded = vi.fn(), onOpenRun = vi.fn()) =>
    render(
      <IntlProvider locale="en" messages={messages}>
        <RunQcBlock
          runId={9}
          qc={qc}
          onRecorded={onRecorded}
          onOpenRun={onOpenRun}
        />
      </IntlProvider>,
    );

  beforeEach(() => {
    utilsMock.getFromOpenElisServer.mockReset();
    utilsMock.postToOpenElisServerJsonResponse.mockReset();
    utilsMock.getFromOpenElisServer.mockImplementation(
      (url: string, callback: (body: unknown) => void) => callback([]),
    );
  });

  it("suggests the earlier run and confirms it as the retest", () => {
    const confirmed: RunQc = {
      overall: "PASS",
      tests: [
        {
          ...LATER.tests[0],
          retestSuggestions: [],
          retestOf: [{ runId: 5, label: "RUN-5", count: 1, total: 2 }],
        },
      ],
    };
    utilsMock.postToOpenElisServerJsonResponse.mockImplementation(
      (url: string, body: string, callback: (body: unknown) => void) =>
        callback(confirmed),
    );
    const onRecorded = vi.fn();
    renderLinks(LATER, onRecorded);

    expect(
      screen.getByTestId("run-qc-retest-suggestion-11-5"),
    ).toHaveTextContent("Suggested: retest of RUN-5 (1 of 2 samples)");
    fireEvent.click(
      screen.getByRole("button", { name: "Confirm retest of RUN-5" }),
    );

    expect(utilsMock.postToOpenElisServerJsonResponse).toHaveBeenCalledWith(
      "/rest/result-runs/9/tests/11/retest-of",
      JSON.stringify({ runId: 5 }),
      expect.any(Function),
    );
    expect(onRecorded).toHaveBeenCalledWith(confirmed);
  });

  it("says why the link was refused", () => {
    utilsMock.postToOpenElisServerJsonResponse.mockImplementation(
      (url: string, body: string, callback: (body: unknown) => void) =>
        callback({ message: "Already linked" }),
    );
    const onRecorded = vi.fn();
    renderLinks(LATER, onRecorded);

    fireEvent.click(
      screen.getByRole("button", { name: "Confirm retest of RUN-5" }),
    );

    expect(screen.getByText("Already linked")).toBeInTheDocument();
    expect(onRecorded).not.toHaveBeenCalled();
  });

  it("opens the run a confirmed link names", () => {
    const onOpenRun = vi.fn();
    renderLinks(
      {
        overall: "PASS",
        tests: [
          {
            testId: "11",
            testName: "Glucose",
            verdict: "FAIL",
            decision: "RETEST",
            controls: [],
            supersededBy: [{ runId: 9, label: "RUN-9", count: 2, total: 2 }],
          },
        ],
      },
      vi.fn(),
      onOpenRun,
    );

    expect(screen.getByTestId("run-qc-superseded-by-11-9")).toHaveTextContent(
      "Superseded by RUN-9 Samples: 2 of 2",
    );
    fireEvent.click(screen.getByText("Superseded by RUN-9"));
    expect(onOpenRun).toHaveBeenCalledWith(9);
    expect(screen.queryByRole("button", { name: /Confirm retest/ })).toBeNull();
  });
});

describe("RunQcBlock under the test's QC policy (OGC-1200 slice 8)", () => {
  const renderQc = (qc: RunQc) =>
    render(
      <IntlProvider locale="en" messages={messages}>
        <RunQcBlock runId={7} qc={qc} onRecorded={vi.fn()} />
      </IntlProvider>,
    );

  it("holds a test whose required control is missing, says why and offers the decisions", () => {
    renderQc({
      overall: "HELD",
      tests: [
        {
          testId: "11",
          testName: "Glucose",
          verdict: "NO_CONTROL",
          controls: [],
          holdCause: "QC_MISSING",
          whenMissing: "HOLD",
        },
      ],
    });

    expect(screen.getByTestId("run-qc-overall")).toHaveTextContent("Held");
    expect(screen.getByTestId("run-qc-missing-11")).toHaveTextContent(
      "requires a control on every run, and none is recorded on this one, so its rows are held",
    );
    expect(screen.getByRole("button", { name: "Retest" })).toBeInTheDocument();
    expect(screen.getByTestId("run-qc-record")).toBeEnabled();
  });

  it("explains an Accept despite a missing control as accepting without it, and asks for that reason", () => {
    renderQc({
      overall: "HELD",
      tests: [
        {
          testId: "11",
          testName: "Glucose",
          verdict: "NO_CONTROL",
          controls: [],
          holdCause: "QC_MISSING",
          whenMissing: "HOLD",
        },
      ],
    });

    fireEvent.click(
      screen.getByRole("button", { name: /Accept despite QC failure/ }),
    );
    expect(
      screen.getByText(/without the control the test requires/),
    ).toBeInTheDocument();
    expect(screen.queryByText(/failing control/)).toBeNull();
    fireEvent.click(screen.getByTestId("run-qc-decide-confirm-11"));
    expect(
      screen.getByText(
        "Give the reason the results are accepted without the required control.",
      ),
    ).toBeInTheDocument();
  });

  it("names a decided missing control in the run's overall verdict", () => {
    renderQc({
      overall: "DECIDED_MISSING",
      tests: [
        {
          testId: "11",
          testName: "Glucose",
          verdict: "NO_CONTROL",
          controls: [],
          decision: "REJECT",
          whenMissing: "HOLD",
        },
      ],
    });
    expect(screen.getByTestId("run-qc-overall")).toHaveTextContent(
      "QC missing: decided",
    );
  });

  it("only names a missing control under a Warn policy, with no decision to take", () => {
    renderQc({
      overall: "NO_CONTROL",
      tests: [
        {
          testId: "11",
          testName: "Glucose",
          verdict: "NO_CONTROL",
          controls: [],
          holdCause: null,
          whenMissing: "WARN",
        },
      ],
    });

    expect(screen.getByTestId("run-qc-overall")).toHaveTextContent(
      "No control",
    );
    expect(screen.getByTestId("run-qc-missing-11")).toHaveTextContent(
      "Its rows can still be accepted.",
    );
    expect(screen.queryByRole("button", { name: "Retest" })).toBeNull();
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

  it("says the row is held because a required control is missing in the run", () => {
    render(
      <IntlProvider locale="en" messages={messages}>
        <WaitingTag
          run={{
            runId: 7,
            label: "RUN-7",
            source: "ANALYZER",
            held: true,
            heldCause: "QC_MISSING",
          }}
        />
      </IntlProvider>,
    );
    expect(screen.getByTestId("held-run-7")).toHaveTextContent(
      "Held: QC missing in RUN-7",
    );
  });
});
