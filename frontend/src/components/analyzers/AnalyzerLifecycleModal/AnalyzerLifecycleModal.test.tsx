import React from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import messages from "../../../languages/en.json";
import { deactivateAnalyzer } from "../../../services/analyzerService";
import AnalyzerLifecycleModal from "./AnalyzerLifecycleModal";

vi.mock("../../../services/analyzerService", () => ({
  deactivateAnalyzer: vi.fn(),
}));

const renderModal = () =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <AnalyzerLifecycleModal
        analyzer={{ id: "501", name: "GeneXpert - Main Lab" }}
        open
        onClose={vi.fn()}
        onConfirm={vi.fn()}
      />
    </IntlProvider>,
  );

describe("AnalyzerLifecycleModal", () => {
  beforeEach(() => vi.clearAllMocks());

  it("names a Bridge failure in words when deactivation fails", async () => {
    vi.mocked(deactivateAnalyzer).mockImplementation((_id, callback) =>
      callback({
        analyzerId: "501",
        status: "ACTIVE",
        deactivated: false,
        failure: "analyzer.bridge.connection.unreachable",
      }),
    );
    renderModal();

    await userEvent.click(
      screen.getByRole("button", { name: /Deactivate analyzer$/ }),
    );

    expect(
      await screen.findByText("The Analyzer Bridge could not be reached."),
    ).toBeInTheDocument();
  });

  it("shows its own message, never the server's text, for an unnamed failure", async () => {
    vi.mocked(deactivateAnalyzer).mockImplementation((_id, callback) =>
      callback({
        analyzerId: "501",
        status: "ACTIVE",
        deactivated: false,
        failure: "Bridge connection does not belong to this analyzer",
      }),
    );
    renderModal();

    await userEvent.click(
      screen.getByRole("button", { name: /Deactivate analyzer$/ }),
    );

    expect(
      await screen.findByText("The analyzer could not be deactivated."),
    ).toBeInTheDocument();
    expect(
      screen.queryByText("Bridge connection does not belong to this analyzer"),
    ).not.toBeInTheDocument();
  });
});
