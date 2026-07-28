import React from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { Route, Router } from "react-router-dom";
import { createMemoryHistory } from "history";
import { beforeEach, describe, expect, test, vi } from "vitest";
import messages from "../../../languages/en.json";
import * as analyzerService from "../../../services/analyzerService";
import FieldMapping from "./FieldMapping";

vi.mock("../../../services/analyzerService", () => ({
  getAnalyzer: vi.fn(),
  getFields: vi.fn(),
  getMappings: vi.fn(),
  getPendingCodes: vi.fn(),
  getTestMappingOptions: vi.fn(),
  resolvePendingCode: vi.fn(),
  updatePendingCodeStatus: vi.fn(),
  getPluginConfig: vi.fn(),
  getResultValueMappings: vi.fn(),
  getResultValueOptions: vi.fn(),
  updateResultValueMappings: vi.fn(),
  getPendingResultValues: vi.fn(),
  resolvePendingResultValue: vi.fn(),
  getSetupVerification: vi.fn(),
  verifyAnalyzerSetup: vi.fn(),
}));

const renderComponent = (entry = "/analyzers/1/mappings") => {
  const history = createMemoryHistory({ initialEntries: [entry] });
  const result = render(
    <Router history={history}>
      <IntlProvider locale="en" messages={messages}>
        <Route path="/analyzers/:id/mappings">
          <FieldMapping />
        </Route>
      </IntlProvider>
    </Router>,
  );
  return { ...result, history };
};

describe("FieldMapping", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    analyzerService.getAnalyzer.mockImplementation((id, callback) => {
      callback({
        id: "1",
        name: "GeneXpert",
        analyzerType: "MOLECULAR",
        status: "SETUP",
      });
    });
    analyzerService.getFields.mockImplementation((id, callback) => {
      callback([]);
    });
    analyzerService.getMappings.mockImplementation((id, callback) => {
      callback([]);
    });
    analyzerService.getPendingCodes.mockImplementation((id, callback) => {
      callback([]);
    });
    analyzerService.getTestMappingOptions.mockImplementation((id, callback) => {
      callback([]);
    });
    analyzerService.getPluginConfig.mockImplementation((id, callback) => {
      callback(null);
    });
    analyzerService.getResultValueMappings.mockImplementation(
      (id, callback) => {
        callback([]);
      },
    );
    analyzerService.getResultValueOptions.mockImplementation(
      (id, testCode, callback) => {
        callback([]);
      },
    );
    analyzerService.getPendingResultValues.mockImplementation(
      (id, callback) => {
        callback([]);
      },
    );
    analyzerService.getSetupVerification.mockImplementation((id, callback) => {
      callback({
        verificationState: "INCOMPLETE",
        mappingReady: false,
        qcApplicable: true,
        qcReady: false,
        currentlyVerified: false,
        readyForActivation: false,
        mappingIds: [],
        qcIds: [],
        blockers: ["NO_TEST_MAPPINGS"],
      });
    });
  });

  test("testUnprofiledAnalyzer_DoesNotExposeLegacyOrRawConfigEditors", async () => {
    analyzerService.getPluginConfig.mockImplementation((id, callback) => {
      callback({ connectionRole: "SERVER", serverListenPort: 17001 });
    });

    renderComponent();

    expect(
      await screen.findByTestId("profile-applied-mappings-panel"),
    ).toBeInTheDocument();
    expect(
      screen.getByTestId("profile-applied-mappings-empty"),
    ).toBeInTheDocument();
    expect(
      screen.queryByTestId("plugin-config-snapshot"),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByTestId("field-mapping-warning"),
    ).not.toBeInTheDocument();
    expect(screen.queryByTestId("field-mapping-stats")).not.toBeInTheDocument();
    expect(
      screen.queryByTestId("field-mapping-actions"),
    ).not.toBeInTheDocument();
    expect(screen.queryByTestId("field-mapping-panel")).not.toBeInTheDocument();
  });

  test("testProfileAppliedMappings_RendersDeterministicReviewWorkflow", async () => {
    analyzerService.getPluginConfig.mockImplementation((id, callback) => {
      callback({
        profile: {
          id: "hl7/genexpert-hl7",
          analyzerName: "Cepheid GeneXpert",
          protocol: "HL7",
        },
        default_test_mappings: [
          {
            analyzer_code: "MTB",
            test_name_hint: "Mycobacterium tuberculosis",
            loinc: "38379-4",
          },
        ],
      });
    });

    renderComponent();

    expect(
      await screen.findByTestId("profile-applied-mapping-row-MTB"),
    ).toHaveTextContent("Mycobacterium tuberculosis");
    expect(screen.getByTestId("pending-codes-panel")).toBeInTheDocument();
    expect(
      screen.getByTestId("result-value-mappings-panel"),
    ).toBeInTheDocument();
    expect(screen.getByTestId("setup-verification-panel")).toBeInTheDocument();
  });

  test("testExistingMappings_RenderReadOnlyForUnprofiledAnalyzer", async () => {
    analyzerService.getFields.mockImplementation((id, callback) => {
      callback([{ id: "field-1", fieldName: "GLUCOSE" }]);
    });
    analyzerService.getMappings.mockImplementation((id, callback) => {
      callback([
        {
          id: "mapping-1",
          analyzerFieldId: "field-1",
          openelisFieldName: "Glucose",
          mappingType: "testCode",
          isActive: true,
        },
      ]);
    });

    renderComponent();

    expect(
      await screen.findByTestId("profile-applied-mapping-row-GLUCOSE"),
    ).toHaveTextContent("Glucose");
    expect(screen.queryByTestId("field-mapping-panel")).not.toBeInTheDocument();
  });

  test("guided Verify continues to Connect with preserved setup state", async () => {
    const { history } = renderComponent(
      "/analyzers/1/mappings?setup=1&step=verify&profile=hl7%2Fgenexpert-hl7&returnTo=%2Fanalyzers%3Fstatus%3DSETUP",
    );

    expect(
      await screen.findByRole("heading", { level: 1, name: "Verify" }),
    ).toBeInTheDocument();
    expect(screen.getByTestId("analyzer-setup-progress")).toHaveAttribute(
      "data-current-step",
      "verify",
    );

    await userEvent.click(
      screen.getByRole("button", { name: "Save and continue" }),
    );

    expect(`${history.location.pathname}${history.location.search}`).toBe(
      "/analyzers/1/edit?setup=1&step=connect&profile=hl7%2Fgenexpert-hl7&returnTo=%2Fanalyzers%3Fstatus%3DSETUP",
    );
  });
});
