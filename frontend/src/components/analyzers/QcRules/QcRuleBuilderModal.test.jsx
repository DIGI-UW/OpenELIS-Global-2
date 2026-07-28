vi.mock("../../../services/analyzerService", () => ({
  getAnalyzer: vi.fn(),
  getQcRules: vi.fn(),
  createQcRule: vi.fn(),
  updateQcRule: vi.fn(),
  deleteQcRule: vi.fn(),
}));

import React from "react";
import { render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { Route, Router } from "react-router-dom";
import { createMemoryHistory } from "history";
import QcRulePage from "./QcRuleBuilderModal";
import {
  createQcRule,
  getAnalyzer,
  getQcRules,
} from "../../../services/analyzerService";
import messages from "../../../languages/en.json";

const VERIFY_ROUTE =
  "/analyzers/AN-STR-1/mappings?setup=1&step=verify&profile=astm%2Fgx";

const renderWithIntl = (
  component,
  entry = `/analyzers/AN-STR-1/qc-rules?returnTo=${encodeURIComponent(
    VERIFY_ROUTE,
  )}`,
) => {
  const history = createMemoryHistory({ initialEntries: [entry] });
  const result = render(
    <Router history={history}>
      <IntlProvider locale="en" messages={messages}>
        <Route path="/analyzers/:id/qc-rules">{component}</Route>
      </IntlProvider>
    </Router>,
  );
  return { ...result, history };
};

describe("QcRulePage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    getAnalyzer.mockImplementation((id, callback) => {
      callback({ id, name: "Analyzer One" });
    });
    getQcRules.mockImplementation((id, callback) => {
      callback([]);
    });
  });

  test("testRoutedPage_LoadsRulesWithoutOpenProp", async () => {
    renderWithIntl(<QcRulePage />);

    await screen.findByTestId("qc-rule-page");
    expect(
      await screen.findByRole("heading", {
        level: 1,
        name: "QC Sample Identification Rules — Analyzer One",
      }),
    ).toBeInTheDocument();
    expect(screen.queryByText(/\\{analyzerName\\}/)).not.toBeInTheDocument();
    expect(
      screen
        .getByTestId("page-header-breadcrumbs")
        .querySelector('a[href*="/analyzers/AN-STR-1/mappings"]'),
    ).toHaveTextContent("Analyzer One");

    await waitFor(() => {
      expect(getQcRules).toHaveBeenCalledWith("AN-STR-1", expect.any(Function));
    });
  });

  test("testCreateQcRule_AddsRuleAndReturnsToAnalyzerWorkflow", async () => {
    createQcRule.mockImplementation((id, payload, callback) => {
      callback({ id: "rule-1", ...payload });
    });

    const { history } = renderWithIntl(<QcRulePage />);

    await userEvent.click(await screen.findByTestId("qc-rule-add-btn"));
    await userEvent.type(await screen.findByTestId("qc-rule-field-0"), "O.12");
    await userEvent.type(await screen.findByTestId("qc-rule-operand-0"), "Q");
    await userEvent.click(await screen.findByTestId("qc-rule-save-btn"));

    await waitFor(() => {
      expect(createQcRule).toHaveBeenCalledWith(
        "AN-STR-1",
        expect.objectContaining({
          ruleType: "FIELD_EQUALS",
          targetField: "O.12",
          operand: "Q",
          isActive: true,
        }),
        expect.any(Function),
      );
      expect(`${history.location.pathname}${history.location.search}`).toBe(
        VERIFY_ROUTE,
      );
    });
  });
});
