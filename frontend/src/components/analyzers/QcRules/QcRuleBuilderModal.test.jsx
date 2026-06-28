vi.mock("../../../services/analyzerService", () => ({
  getAnalyzer: vi.fn(),
  getQcRules: vi.fn(),
  createQcRule: vi.fn(),
  updateQcRule: vi.fn(),
  deleteQcRule: vi.fn(),
}));

vi.mock("react-router-dom", async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    useHistory: () => mockHistory,
    useParams: () => ({ id: "AN-STR-1" }),
  };
});

import React from "react";
import { render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { BrowserRouter } from "react-router-dom";
import QcRulePage from "./QcRuleBuilderModal";
import {
  createQcRule,
  getAnalyzer,
  getQcRules,
} from "../../../services/analyzerService";
import messages from "../../../languages/en.json";

const mockHistory = {
  push: vi.fn(),
};

const renderWithIntl = (component) =>
  render(
    <BrowserRouter>
      <IntlProvider locale="en" messages={messages}>
        {component}
      </IntlProvider>
    </BrowserRouter>,
  );

describe("QcRulePage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockHistory.push.mockClear();
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

    await waitFor(() => {
      expect(getQcRules).toHaveBeenCalledWith("AN-STR-1", expect.any(Function));
    });
  });

  test("testCreateQcRule_AddsRuleAndReturnsToAnalyzerWorkflow", async () => {
    createQcRule.mockImplementation((id, payload, callback) => {
      callback({ id: "rule-1", ...payload });
    });

    renderWithIntl(<QcRulePage />);

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
      expect(mockHistory.push).toHaveBeenCalledWith("/analyzers");
    });
  });
});
