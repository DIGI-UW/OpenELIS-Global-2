vi.mock("../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  postToOpenElisServerFullResponse: vi.fn(),
}));

vi.mock("react-router-dom", async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    useHistory: () => ({ goBack: vi.fn(), push: vi.fn() }),
    useLocation: () => ({ search: "?analyzerId=7" }),
    useParams: () => ({}),
  };
});

import React from "react";
import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { BrowserRouter } from "react-router-dom";
import { beforeEach, describe, expect, test, vi } from "vitest";
import messages from "../../../languages/en.json";
import { getFromOpenElisServer } from "../../utils/Utils";
import ControlLotSetup, { buildControlLotPayload } from "./ControlLotSetup";

const renderWithIntl = (component) =>
  render(
    <BrowserRouter>
      <IntlProvider locale="en" messages={messages}>
        {component}
      </IntlProvider>
    </BrowserRouter>,
  );

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
});
