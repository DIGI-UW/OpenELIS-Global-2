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
 * OGC-1025 (R6) — manual control capture renders the documented gray state.
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
    expect(screen.queryByTestId("control-blocked")).not.toBeInTheDocument();
  });

  it("manual row shows the R6 control-capture blocker note", () => {
    respondWith({ "/rest/results-entry/test/6/reagents": [] });
    wrap(<ReagentsQcSection testId="6" editable open onToggle={() => {}} />);
    expect(screen.getByTestId("control-blocked")).toBeInTheDocument();
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
