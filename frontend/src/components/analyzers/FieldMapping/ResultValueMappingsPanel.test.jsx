import React from "react";
import { render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import ResultValueMappingsPanel from "./ResultValueMappingsPanel";
import messages from "../../../languages/en.json";
import * as analyzerService from "../../../services/analyzerService";

vi.mock("../../../services/analyzerService", () => ({
  resolvePendingResultValue: vi.fn(),
}));

const renderWithIntl = (component) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      {component}
    </IntlProvider>,
  );

describe("ResultValueMappingsPanel", () => {
  test("resolves pending result value with selected OpenELIS value", async () => {
    const onUpdated = vi.fn();
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

    await userEvent.type(
      screen.getByTestId("result-value-openelis-rv-1"),
      "INDETERMINATE",
    );
    await userEvent.click(screen.getByTestId("result-value-resolve-rv-1"));

    await waitFor(() => {
      expect(analyzerService.resolvePendingResultValue).toHaveBeenCalledWith(
        "2013",
        "rv-1",
        { openelisValue: "INDETERMINATE" },
        expect.any(Function),
      );
      expect(onUpdated).toHaveBeenCalled();
    });
  });
});
