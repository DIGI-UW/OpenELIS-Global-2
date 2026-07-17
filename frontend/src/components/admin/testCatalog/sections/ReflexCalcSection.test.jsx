/**
 * ReflexCalcSection — OGC-949 / OGC-998 + OGC-999.
 *
 * Read-only cross-links: reflex rules triggered by this test and calculations
 * that produce/consume it. Covers render, empty states, and error state.
 */

// ========== MOCKS (before imports) ==========
vi.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
}));

// ========== IMPORTS ==========
import React from "react";
import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import ReflexCalcSection from "./ReflexCalcSection";
import { getFromOpenElisServer } from "../../../utils/Utils";
import messages from "../../../../languages/en.json";

const renderSection = () =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <ReflexCalcSection testId="42" />
    </IntlProvider>,
  );

beforeEach(() => {
  vi.clearAllMocks();
});

describe("ReflexCalcSection", () => {
  it("renders reflex rules and calculation cross-links", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) =>
      cb({
        reflexRules: [
          {
            id: "rx-1",
            ruleName: "Reflex to Culture",
            triggerCondition: "Positive",
            reflexTests: "Culture",
          },
        ],
        calculatedBy: [
          { id: 5, name: "eGFR", formula: "(A / B) * 1.0", outputTest: null },
        ],
        feedsInto: [
          {
            id: 9,
            name: "Ratio",
            formula: "ALT / AST",
            outputTest: "Ratio Test",
          },
        ],
      }),
    );
    renderSection();

    expect(await screen.findByText("Reflex to Culture")).toBeInTheDocument();
    expect(screen.getByText("Positive")).toBeInTheDocument();
    expect(screen.getByText("Culture")).toBeInTheDocument();
    expect(screen.getByText("eGFR")).toBeInTheDocument();
    expect(screen.getByText("Ratio")).toBeInTheDocument();
    expect(screen.getByText("Ratio Test")).toBeInTheDocument();
  });

  it("shows empty states when there are no cross-links", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) =>
      cb({ reflexRules: [], calculatedBy: [], feedsInto: [] }),
    );
    renderSection();
    expect(
      await screen.findByText(
        messages["label.testCatalog.reflexCalc.reflex.empty"],
      ),
    ).toBeInTheDocument();
  });

  it("shows an error state when the fetch fails", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) => cb(undefined));
    renderSection();
    expect(
      await screen.findByText(
        messages["label.testCatalog.reflexCalc.loadError"],
      ),
    ).toBeInTheDocument();
  });
});
