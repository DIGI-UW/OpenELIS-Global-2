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

  // Each cross-link identifies its own record, so following one opens that rule
  // or calculation rather than the whole collection.
  it("links every cross-link to its own record by id", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) =>
      cb({
        reflexRules: [
          {
            id: "rx-1",
            ruleId: 77,
            ruleName: "Reflex to Culture",
            triggerCondition: "Positive",
            reflexTests: "Culture",
          },
          {
            id: "rx-2",
            ruleName: "Legacy row",
            triggerCondition: "Negative",
            reflexTests: "Smear",
          },
        ],
        calculatedBy: [
          { id: 5, name: "eGFR", formula: "(A / B) * 1.0", outputTest: null },
        ],
        feedsInto: [
          { id: 9, name: "Ratio", formula: "ALT / AST", outputTest: "Ratio" },
        ],
      }),
    );
    renderSection();

    expect(await screen.findByText("Reflex to Culture")).toBeInTheDocument();

    // The rule record that owns the row, not the row's own test_reflex id.
    expect(
      screen.getByRole("link", { name: /Reflex to Culture/ }),
    ).toHaveAttribute("href", "/MasterListsPage/reflex?id=77");
    // A legacy row owns no rule, so it falls back to the unfiltered list rather
    // than filtering to nothing.
    expect(screen.getByRole("link", { name: /Legacy row/ })).toHaveAttribute(
      "href",
      "/MasterListsPage/reflex",
    );
    expect(screen.getByRole("link", { name: /eGFR/ })).toHaveAttribute(
      "href",
      "/MasterListsPage/calculatedValue?id=5",
    );
    expect(screen.getByRole("link", { name: /Ratio/ })).toHaveAttribute(
      "href",
      "/MasterListsPage/calculatedValue?id=9",
    );
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

  it("renders the calculation empty states as inline notifications, not bare text (OGC-1153)", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) =>
      cb({ reflexRules: [], calculatedBy: [], feedsInto: [] }),
    );
    const { container } = renderSection();

    // Each calculation direction has its own message, and both must be
    // notifications rather than the bare unstyled <p> this ticket reported.
    for (const key of [
      "label.testCatalog.reflexCalc.calc.feedsInto.empty",
      "label.testCatalog.reflexCalc.calc.calculatedBy.empty",
    ]) {
      const el = await screen.findByText(messages[key]);
      expect(el.closest(".cds--inline-notification")).not.toBeNull();
    }
    // Three empty states in the panel, one treatment.
    expect(
      container.querySelectorAll(".cds--inline-notification"),
    ).toHaveLength(3);
    // The old treatment was a <p> with no class; assert it is gone.
    expect(container.querySelectorAll("p:not([class])")).toHaveLength(0);
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
