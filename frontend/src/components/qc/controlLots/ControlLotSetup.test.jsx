vi.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  postToOpenElisServerFullResponse: vi.fn(),
}));

import React from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { Route, Router } from "react-router-dom";
import { createMemoryHistory } from "history";
import { beforeEach, describe, expect, test, vi } from "vitest";
import messages from "../../../languages/en.json";
import { getFromOpenElisServer } from "../../utils/Utils";
import ControlLotSetup, { buildControlLotPayload } from "./ControlLotSetup";

const VERIFY_ROUTE =
  "/analyzers/7/mappings?setup=1&step=verify&profile=astm%2Fgx";

const renderWithIntl = (
  component,
  entry = `/analyzers/qc/control-lots/new?analyzerId=7&returnTo=${encodeURIComponent(
    VERIFY_ROUTE,
  )}`,
) => {
  const history = createMemoryHistory({ initialEntries: [entry] });
  const result = render(
    <Router history={history}>
      <IntlProvider locale="en" messages={messages}>
        <Route path="/analyzers/qc/control-lots/new">{component}</Route>
      </IntlProvider>
    </Router>,
  );
  return { ...result, history };
};

describe("buildControlLotPayload", () => {
  test("testBuildPayload_PreservesAnalyzerAndTestIdsAsStrings", () => {
    const payload = buildControlLotPayload(
      {
        lotNumber: "QC-001",
        controlMaterial: "Acme control",
        controlLevel: "LOW",
        expirationDate: "12/31/2026",
        analyzerId: "AN-STR-1",
        testId: "TEST-STR-9",
        isActive: true,
      },
      {
        calculationMethod: "MANUFACTURER_FIXED",
        initialRunsRequired: 20,
        mean: 12.3,
        standardDeviation: 0.4,
      },
      { isEditMode: false },
    );

    expect(payload.instrumentId).toBe("AN-STR-1");
    expect(payload.testId).toBe("TEST-STR-9");
  });
});

describe("ControlLotSetup", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    getFromOpenElisServer.mockImplementation((endpoint, callback) => {
      if (endpoint === "/rest/analyzer/analyzers") {
        callback({ analyzers: [{ id: "7", name: "Analyzer 7" }] });
      }
      if (endpoint === "/rest/displayList/ALL_TESTS") {
        callback([{ id: "42", value: "Glucose" }]);
      }
    });
  });

  test("testPreselectAnalyzer_FromAnalyzerWorkflowQuery", async () => {
    renderWithIntl(<ControlLotSetup />);

    expect(
      await screen.findByTestId("control-lot-analyzer-dropdown"),
    ).toHaveTextContent("Analyzer 7");
  });

  test("testCancel_ReturnsToOriginatingVerifyStep", async () => {
    const { history } = renderWithIntl(<ControlLotSetup />);

    await screen.findByTestId("control-lot-analyzer-dropdown");
    await userEvent.click(screen.getByRole("button", { name: "Cancel" }));

    expect(`${history.location.pathname}${history.location.search}`).toBe(
      VERIFY_ROUTE,
    );
  });
});
