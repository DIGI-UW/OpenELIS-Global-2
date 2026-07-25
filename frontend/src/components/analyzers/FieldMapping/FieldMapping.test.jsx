import React from "react";
import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { BrowserRouter } from "react-router-dom";
import { beforeEach, describe, expect, test, vi } from "vitest";
import messages from "../../../languages/en.json";
import * as analyzerService from "../../../services/analyzerService";
import FieldMapping from "./FieldMapping";

vi.mock("../../../services/analyzerService", () => ({
  getAnalyzer: vi.fn(),
  getFields: vi.fn(),
  getMappings: vi.fn(),
  getPendingCodes: vi.fn(),
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

const mockHistory = {
  push: vi.fn(),
};

vi.mock("react-router-dom", async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    useHistory: () => mockHistory,
    useParams: () => ({ id: "1" }),
  };
});

const renderComponent = () =>
  render(
    <BrowserRouter>
      <IntlProvider locale="en" messages={messages}>
        <FieldMapping />
      </IntlProvider>
    </BrowserRouter>,
  );

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
});
