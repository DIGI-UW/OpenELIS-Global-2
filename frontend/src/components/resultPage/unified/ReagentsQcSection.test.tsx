import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import messages from "../../../languages/en.json";

vi.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  postToOpenElisServerJsonResponse: vi.fn(),
}));
// eslint-disable-next-line import/first
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../utils/Utils";
const getMock = getFromOpenElisServer as ReturnType<typeof vi.fn>;
const postMock = postToOpenElisServerJsonResponse as ReturnType<typeof vi.fn>;

import ReagentsQcSection from "./ReagentsQcSection";

/**
 * OGC-1024 (R5) — reagent links (catalog) + FEFO lots (inventory) + record
 * use via the shipped consume endpoint; analyzer rows read-only.
 * OGC-1025 (R6, FR-D3/D4) — polymorphic control capture: RDT control-line
 * outcome + kit lot, or manual quantitative measured/expected/uncertainty +
 * Pass/Fail against a bench lot, POSTed to /rest/qc/results.
 */
const wrap = (node: React.ReactElement) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      {node}
    </IntlProvider>,
  );

const LINKS = [
  {
    reagentId: 1,
    usageType: "PRIMARY",
    quantityPerTest: 1,
    quantityUnit: "mL",
    name: "Glucose HK Gen.3",
    units: "mL",
  },
];
const LOTS = [
  {
    id: 11,
    lotNumber: "LOT-2026-0421",
    expirationDate: "2026-08-20",
    currentQuantity: 2,
    status: "ACTIVE",
  },
  {
    id: 12,
    lotNumber: "LOT-2027-0001",
    expirationDate: "2027-01-01",
    currentQuantity: 50,
    status: "ACTIVE",
  },
];

const respondWith = (routes: Record<string, unknown>) =>
  getMock.mockImplementation((url: string, cb: (body: unknown) => void) => {
    if (typeof url !== "string" || typeof cb !== "function") {
      return;
    }
    const match = Object.keys(routes).find((route) => url.includes(route));
    if (match) {
      cb(routes[match]);
    }
  });

describe("ReagentsQcSection (R5/R6)", () => {
  beforeEach(() => {
    getMock.mockReset();
    postMock.mockReset();
  });

  it("manual editable row lists FEFO lots and records use via /consume", () => {
    respondWith({
      "/rest/results-entry/test/6/reagents": LINKS,
      "/rest/inventory/lots/item/1/available": LOTS,
    });
    postMock.mockImplementation(
      (_url: string, _body: string, cb: (r: unknown) => void) => {
        if (typeof cb === "function") {
          cb({ consumedLots: [{ lotNumber: "LOT-2026-0421" }] });
        }
      },
    );
    wrap(
      <ReagentsQcSection
        testId="6"
        analysisId="28"
        editable
        open
        onToggle={() => {}}
      />,
    );
    expect(screen.getByText("Glucose HK Gen.3")).toBeInTheDocument();
    expect(screen.getByTestId("lot-LOT-2026-0421")).toBeInTheDocument();
    expect(screen.getByText("FEFO Suggested")).toBeInTheDocument();
    fireEvent.click(screen.getByTestId("record-use-1"));
    expect(postMock).toHaveBeenCalledWith(
      "/rest/inventory/management/consume",
      JSON.stringify({ itemId: "1", quantity: 1, analysisId: "28" }),
      expect.any(Function),
    );
    expect(
      screen.getByText("Recorded 1 mL — consumed FEFO across lots."),
    ).toBeInTheDocument();
  });

  it("no linked reagents renders the R5 gray state", () => {
    respondWith({ "/rest/results-entry/test/6/reagents": [] });
    wrap(<ReagentsQcSection testId="6" editable open onToggle={() => {}} />);
    expect(
      screen.getByText(
        "No reagents linked to this test — links are configured in the Test Catalog.",
      ),
    ).toBeInTheDocument();
  });

  it("analyzer-loaded row is read-only and shows configured control lots", () => {
    respondWith({
      "/rest/results-entry/test/6/reagents": LINKS,
      "/rest/qc/controlLots": [
        {
          id: "cl-1",
          productName: "Liquichek L1",
          lotNumber: "QC-2026-031",
          controlLevel: "NORMAL",
          status: "ACTIVE",
        },
      ],
    });
    wrap(
      <ReagentsQcSection
        testId="6"
        editable
        fromAnalyzerId="3"
        analyzerName="Cobas 6000"
        open
        onToggle={() => {}}
      />,
    );
    expect(
      screen.getByText("capture mode: analyzer-reported"),
    ).toBeInTheDocument();
    expect(screen.queryByTestId("record-use-1")).not.toBeInTheDocument();
    expect(screen.getByText("Liquichek L1")).toBeInTheDocument();
    expect(screen.queryByTestId("qc-capture")).not.toBeInTheDocument();
  });

  it("RDT row records an Invalid control line with its kit lot", () => {
    respondWith({ "/rest/results-entry/test/13/reagents": [] });
    postMock.mockImplementation(
      (_url: string, _body: string, cb: (r: unknown) => void) => {
        if (typeof cb === "function") {
          cb({ id: "qc-1" });
        }
      },
    );
    wrap(
      <ReagentsQcSection
        testId="13"
        testSectionId="36"
        resultType="D"
        editable
        open
        onToggle={() => {}}
      />,
    );
    // RDT capture needs no lot record — no bench-lot fetch
    expect(getMock).not.toHaveBeenCalledWith(
      expect.stringContaining("/rest/qc/controlLots"),
      expect.any(Function),
    );
    fireEvent.change(screen.getByLabelText("Control line"), {
      target: { value: "INVALID" },
    });
    expect(screen.getByTestId("qc-blocked-hint")).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText("Kit lot"), {
      target: { value: "UAT-1025 kit" },
    });
    fireEvent.click(screen.getByTestId("qc-record"));
    expect(postMock).toHaveBeenCalledWith(
      "/rest/qc/results",
      JSON.stringify({
        source: "RDT",
        qualitativeOutcome: "INVALID",
        controlLabel: "UAT-1025 kit",
        testId: "13",
        testSectionId: "36",
      }),
      expect.any(Function),
    );
    expect(
      screen.getByText(
        "Control result recorded. Reporting is blocked for covered results until a passing repeat is recorded.",
      ),
    ).toBeInTheDocument();
  });

  it("manual quantitative row records measured/expected/uncertainty against a bench lot", () => {
    respondWith({
      "/rest/results-entry/test/6/reagents": [],
      "/rest/qc/controlLots?testId=6": [
        {
          id: "cl-9",
          productName: "Liquichek L1",
          lotNumber: "QC-2026-031",
          controlLevel: "NORMAL",
          status: "ACTIVE",
        },
      ],
    });
    postMock.mockImplementation(
      (_url: string, _body: string, cb: (r: unknown) => void) => {
        if (typeof cb === "function") {
          cb({ id: "qc-2", zScore: 2.5 });
        }
      },
    );
    wrap(
      <ReagentsQcSection
        testId="6"
        testSectionId="36"
        resultType="N"
        unitOfMeasure="mg/dL"
        editable
        open
        onToggle={() => {}}
      />,
    );
    fireEvent.change(screen.getByLabelText("Outcome"), {
      target: { value: "FAIL" },
    });
    fireEvent.change(screen.getByLabelText("Control lot"), {
      target: { value: "cl-9" },
    });
    fireEvent.change(screen.getByLabelText("Measured value"), {
      target: { value: "112.5" },
    });
    fireEvent.change(screen.getByLabelText("Expected value"), {
      target: { value: "100" },
    });
    fireEvent.change(screen.getByLabelText("Uncertainty (±)"), {
      target: { value: "5" },
    });
    fireEvent.click(screen.getByTestId("qc-record"));
    expect(postMock).toHaveBeenCalledWith(
      "/rest/qc/results",
      JSON.stringify({
        source: "MANUAL",
        qualitativeOutcome: "FAIL",
        resultValue: "112.5",
        expectedValue: "100",
        uncertainty: "5",
        controlLotId: "cl-9",
        testId: "6",
        testSectionId: "36",
        unitOfMeasure: "mg/dL",
      }),
      expect.any(Function),
    );
  });

  it("incomplete capture never POSTs and shows the inline error", () => {
    respondWith({ "/rest/results-entry/test/13/reagents": [] });
    wrap(
      <ReagentsQcSection
        testId="13"
        testSectionId="36"
        editable
        open
        onToggle={() => {}}
      />,
    );
    fireEvent.click(screen.getByTestId("qc-record"));
    expect(postMock).not.toHaveBeenCalled();
    expect(
      screen.getByText("Complete the control result fields before recording."),
    ).toBeInTheDocument();

    // an outcome without the kit lot is equally incomplete
    fireEvent.change(screen.getByLabelText("Control line"), {
      target: { value: "VALID" },
    });
    fireEvent.click(screen.getByTestId("qc-record"));
    expect(postMock).not.toHaveBeenCalled();
  });

  it("a 400 capture response surfaces the backend message verbatim", () => {
    respondWith({ "/rest/results-entry/test/13/reagents": [] });
    postMock.mockImplementation(
      (_url: string, _body: string, cb: (r: unknown) => void) => {
        if (typeof cb === "function") {
          cb({ status: 400, message: "Control lot QC-1 is not a bench lot" });
        }
      },
    );
    wrap(
      <ReagentsQcSection
        testId="13"
        testSectionId="36"
        editable
        open
        onToggle={() => {}}
      />,
    );
    fireEvent.change(screen.getByLabelText("Control line"), {
      target: { value: "VALID" },
    });
    fireEvent.change(screen.getByLabelText("Kit lot"), {
      target: { value: "kit A" },
    });
    fireEvent.click(screen.getByTestId("qc-record"));
    expect(
      screen.getByText("Control lot QC-1 is not a bench lot"),
    ).toBeInTheDocument();
  });

  it("a transport failure is never reported as recorded", () => {
    // the fetch helper reports a network error as {error, status: 0} — a falsy
    // status, so a status-only check would call this a success
    respondWith({ "/rest/results-entry/test/13/reagents": [] });
    postMock.mockImplementation(
      (_url: string, _body: string, cb: (r: unknown) => void) => {
        if (typeof cb === "function") {
          cb({
            error: "Failed to fetch",
            message: "Failed to fetch",
            status: 0,
          });
        }
      },
    );
    wrap(
      <ReagentsQcSection
        testId="13"
        testSectionId="36"
        editable
        open
        onToggle={() => {}}
      />,
    );
    fireEvent.change(screen.getByLabelText("Control line"), {
      target: { value: "INVALID" },
    });
    fireEvent.change(screen.getByLabelText("Kit lot"), {
      target: { value: "kit A" },
    });
    fireEvent.click(screen.getByTestId("qc-record"));
    expect(screen.getByText("Failed to fetch")).toBeInTheDocument();
    expect(screen.queryByText(/Control result recorded/)).toBeNull();
  });

  it("a passing control confirms without the reporting-blocked warning", () => {
    respondWith({ "/rest/results-entry/test/13/reagents": [] });
    postMock.mockImplementation(
      (_url: string, _body: string, cb: (r: unknown) => void) => {
        if (typeof cb === "function") {
          cb({ id: "qc-3" });
        }
      },
    );
    wrap(
      <ReagentsQcSection
        testId="13"
        testSectionId="36"
        editable
        open
        onToggle={() => {}}
      />,
    );
    fireEvent.change(screen.getByLabelText("Control line"), {
      target: { value: "VALID" },
    });
    fireEvent.change(screen.getByLabelText("Kit lot"), {
      target: { value: "kit A" },
    });
    expect(screen.queryByTestId("qc-blocked-hint")).not.toBeInTheDocument();
    fireEvent.click(screen.getByTestId("qc-record"));
    expect(screen.getByText("Control result recorded.")).toBeInTheDocument();
  });

  it("a quantitative row with no bench lot says so instead of offering an empty picker", () => {
    respondWith({
      "/rest/results-entry/test/6/reagents": [],
      "/rest/qc/controlLots?testId=6": [],
    });
    wrap(
      <ReagentsQcSection
        testId="6"
        testSectionId="36"
        resultType="N"
        editable
        open
        onToggle={() => {}}
      />,
    );
    expect(screen.getByTestId("qc-no-bench-lots")).toBeInTheDocument();
  });

  it("without a lab-unit scope neither the capture form nor its heading render", () => {
    respondWith({ "/rest/results-entry/test/13/reagents": [] });
    wrap(<ReagentsQcSection testId="13" editable open onToggle={() => {}} />);
    expect(screen.queryByTestId("qc-capture")).not.toBeInTheDocument();
    expect(screen.queryByText("Control result")).not.toBeInTheDocument();
  });

  it("a saved (read-only) row can still record a control", () => {
    // the hold covers results already at Technical Acceptance, so capture must
    // survive the row going non-editable after Save
    respondWith({ "/rest/results-entry/test/13/reagents": [] });
    wrap(
      <ReagentsQcSection
        testId="13"
        testSectionId="36"
        editable={false}
        open
        onToggle={() => {}}
      />,
    );
    expect(screen.getByTestId("qc-capture")).toBeInTheDocument();
  });

  it("a 4xx consume response surfaces the server message", () => {
    respondWith({
      "/rest/results-entry/test/6/reagents": LINKS,
      "/rest/inventory/lots/item/1/available": LOTS,
    });
    postMock.mockImplementation(
      (_url: string, _body: string, cb: (r: unknown) => void) => {
        if (typeof cb === "function") {
          cb({ status: 409, message: "Insufficient stock" });
        }
      },
    );
    wrap(
      <ReagentsQcSection
        testId="6"
        analysisId="28"
        editable
        open
        onToggle={() => {}}
      />,
    );
    fireEvent.click(screen.getByTestId("record-use-1"));
    expect(screen.getByText("Insufficient stock")).toBeInTheDocument();
  });
});
