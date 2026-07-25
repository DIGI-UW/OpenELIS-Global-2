import React from "react";
import { render, screen, within } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import ResultValueMappingsPanel from "./ResultValueMappingsPanel";
import messages from "../../../languages/en.json";
import * as analyzerService from "../../../services/analyzerService";

vi.mock("../../../services/analyzerService", () => ({
  getResultValueOptions: vi.fn(),
  updateResultValueMappings: vi.fn(),
  resolvePendingResultValue: vi.fn(),
}));

const renderWithIntl = (component) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      {component}
    </IntlProvider>,
  );

describe("ResultValueMappingsPanel", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  test("resolves a pending value with a catalog option from its mapped test", async () => {
    const onUpdated = vi.fn();
    analyzerService.getResultValueOptions.mockImplementation(
      (analyzerId, testCode, callback) => {
        callback([
          {
            id: "result-option-detected",
            value: "9001",
            label: "Detected",
            testId: "501",
          },
          {
            id: "result-option-indeterminate",
            value: "9002",
            label: "Indeterminate",
            testId: "501",
          },
        ]);
      },
    );
    analyzerService.resolvePendingResultValue.mockImplementation(
      (analyzerId, pendingId, payload, callback) => {
        callback({ id: pendingId, status: "MAPPED", ...payload });
      },
    );

    renderWithIntl(
      <ResultValueMappingsPanel
        analyzerId="2013"
        onUpdated={onUpdated}
        mappings={[
          {
            analyzerValue: "Detected",
            openelisValue: "POSITIVE",
            bindingStatus: "LEGACY_UNBOUND",
            testCode: "MTB",
            active: true,
          },
        ]}
        pendingValues={[
          {
            id: "rv-1",
            analyzerValue: "Trace",
            testCode: "MTB",
            status: "PENDING",
          },
        ]}
      />,
    );

    expect(screen.getByText("Detected")).toBeInTheDocument();
    expect(screen.getByText("Trace")).toBeInTheDocument();
    expect(screen.getByText("LEGACY_UNBOUND")).toBeInTheDocument();

    const pendingTable = await screen.findByTestId(
      "pending-result-values-table",
    );
    const resultOption = await within(pendingTable).findByRole("combobox", {
      name: "OpenELIS Result Option",
    });
    await userEvent.click(resultOption);
    await userEvent.click(
      await screen.findByRole("option", { name: "Indeterminate" }),
    );
    await userEvent.click(screen.getByTestId("result-value-resolve-rv-1"));

    await waitFor(() => {
      expect(analyzerService.resolvePendingResultValue).toHaveBeenCalledWith(
        "2013",
        "rv-1",
        { openelisResultOptionId: "result-option-indeterminate" },
        expect.any(Function),
      );
      expect(onUpdated).toHaveBeenCalled();
    });
  });

  test("shows catalog guidance when the mapped test has no active options", async () => {
    analyzerService.getResultValueOptions.mockImplementation(
      (analyzerId, testCode, callback) => callback([]),
    );

    renderWithIntl(
      <ResultValueMappingsPanel
        analyzerId="2013"
        pendingValues={[
          {
            id: "rv-2",
            analyzerValue: "Equivocal",
            testCode: "MTB",
            status: "PENDING",
          },
        ]}
      />,
    );

    expect(
      await screen.findByText(
        "No active result options are configured for this mapped test.",
      ),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("link", { name: "Open Test Catalog" }),
    ).toHaveAttribute("href", "/MasterListsPage/TestCatalogList");
  });

  test("does not leave resolved values in the pending queue", () => {
    renderWithIntl(
      <ResultValueMappingsPanel
        analyzerId="2013"
        pendingValues={[
          {
            id: "rv-1",
            analyzerValue: "Trace",
            testCode: "MTB",
            status: "MAPPED",
            openelisResultOptionId: "result-option-detected",
            openelisLabel: "Detected",
          },
        ]}
      />,
    );

    expect(
      screen.getByTestId("pending-result-values-empty"),
    ).toBeInTheDocument();
    expect(
      screen.queryByTestId("pending-result-values-table"),
    ).not.toBeInTheDocument();
    expect(
      screen.queryByTestId("result-value-resolve-rv-1"),
    ).not.toBeInTheDocument();
  });

  test("binds legacy profile mappings to catalog options before verification", async () => {
    const onUpdated = vi.fn();
    analyzerService.getResultValueOptions.mockImplementation(
      (analyzerId, testCode, callback) => {
        callback([
          {
            id: "result-option-detected",
            value: "9001",
            label: "Detected",
            testId: "501",
          },
          {
            id: "result-option-not-detected",
            value: "9002",
            label: "Not detected",
            testId: "501",
          },
        ]);
      },
    );
    analyzerService.updateResultValueMappings.mockImplementation(
      (analyzerId, mappings, callback) => {
        callback({ resultValueMappings: mappings });
      },
    );

    renderWithIntl(
      <ResultValueMappingsPanel
        analyzerId="2013"
        onUpdated={onUpdated}
        mappings={[
          {
            analyzerValue: "DETECTED",
            openelisValue: "POSITIVE",
            bindingStatus: "LEGACY_UNBOUND",
            testCode: "MTB",
            active: true,
          },
          {
            analyzerValue: "NOT DETECTED",
            openelisValue: "NEGATIVE",
            bindingStatus: "LEGACY_UNBOUND",
            testCode: "MTB",
            active: true,
          },
        ]}
      />,
    );

    const selectors = await screen.findAllByRole("combobox", {
      name: "OpenELIS Result Option",
    });
    const saveButton = screen.getByRole("button", {
      name: "Save result mappings",
    });
    expect(saveButton).toBeDisabled();

    await userEvent.click(selectors[0]);
    await userEvent.click(
      await screen.findByRole("option", { name: "Detected" }),
    );
    await userEvent.click(selectors[1]);
    await userEvent.click(
      await screen.findByRole("option", { name: "Not detected" }),
    );
    expect(saveButton).toBeEnabled();
    await userEvent.click(saveButton);

    await waitFor(() => {
      expect(analyzerService.updateResultValueMappings).toHaveBeenCalledWith(
        "2013",
        [
          {
            analyzerValue: "DETECTED",
            testCode: "MTB",
            active: true,
            openelisResultOptionId: "result-option-detected",
          },
          {
            analyzerValue: "NOT DETECTED",
            testCode: "MTB",
            active: true,
            openelisResultOptionId: "result-option-not-detected",
          },
        ],
        expect.any(Function),
      );
      expect(onUpdated).toHaveBeenCalled();
    });
  });

  test("preserves readable fields for inactive legacy mappings when saving", async () => {
    analyzerService.getResultValueOptions.mockImplementation(
      (analyzerId, testCode, callback) =>
        callback([
          {
            id: "result-option-detected",
            value: "9001",
            label: "Detected",
            testId: "501",
          },
        ]),
    );
    analyzerService.updateResultValueMappings.mockImplementation(
      (analyzerId, mappings, callback) => {
        callback({ resultValueMappings: mappings });
      },
    );

    renderWithIntl(
      <ResultValueMappingsPanel
        analyzerId="2013"
        mappings={[
          {
            analyzerValue: "DETECTED",
            openelisValue: "POSITIVE",
            bindingStatus: "LEGACY_UNBOUND",
            testCode: "MTB",
            active: true,
          },
          {
            analyzerValue: "OBSOLETE",
            openelisValue: "OLD",
            openelisLabel: "Obsolete result",
            bindingStatus: "LEGACY_UNBOUND",
            testCode: "MTB",
            active: false,
          },
        ]}
      />,
    );

    const selector = await screen.findByRole("combobox", {
      name: "OpenELIS Result Option",
    });
    await userEvent.click(selector);
    await userEvent.click(
      await screen.findByRole("option", { name: "Detected" }),
    );
    await userEvent.click(
      screen.getByRole("button", { name: "Save result mappings" }),
    );

    await waitFor(() =>
      expect(analyzerService.updateResultValueMappings).toHaveBeenCalledWith(
        "2013",
        [
          {
            analyzerValue: "DETECTED",
            testCode: "MTB",
            active: true,
            openelisResultOptionId: "result-option-detected",
          },
          {
            analyzerValue: "OBSOLETE",
            testCode: "MTB",
            active: false,
            openelisValue: "OLD",
            openelisLabel: "Obsolete result",
          },
        ],
        expect.any(Function),
      ),
    );
  });
});
