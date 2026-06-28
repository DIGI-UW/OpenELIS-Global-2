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
    useHistory: () => ({ push: vi.fn() }),
    useParams: () => ({ id: "AN-STR-1" }),
  };
});

import React from "react";
import { render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { BrowserRouter } from "react-router-dom";
import QcRulePage from "./QcRuleBuilderModal";
import { getAnalyzer, getQcRules } from "../../../services/analyzerService";
import messages from "../../../languages/en.json";

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
});
