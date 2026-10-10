import React from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import messages from "../../../languages/en.json";
import {
  activateAnalyzer,
  deactivateAnalyzer,
  reactivateAnalyzer,
} from "../../../services/analyzerService";
import AnalyzerLifecycleModal, {
  type AnalyzerLifecycleAction,
} from "./AnalyzerLifecycleModal";

vi.mock("../../../services/analyzerService", () => ({
  activateAnalyzer: vi.fn(),
  deactivateAnalyzer: vi.fn(),
  reactivateAnalyzer: vi.fn(),
}));

const renderModal = (action: AnalyzerLifecycleAction) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <AnalyzerLifecycleModal
        action={action}
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
    renderModal("deactivate");

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
    renderModal("deactivate");

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

  it("shows its own message when reactivation fails without blockers", async () => {
    vi.mocked(reactivateAnalyzer).mockImplementation((_id, callback) =>
      callback({
        error: "Analyzer not found: 501",
      } as never),
    );
    renderModal("reactivate");

    await userEvent.click(
      screen.getByRole("button", { name: "Reactivate analyzer" }),
    );

    expect(
      await screen.findByText(messages["analyzer.lifecycle.reactivate.error"]),
    ).toBeInTheDocument();
    expect(
      screen.queryByText("Analyzer not found: 501"),
    ).not.toBeInTheDocument();
  });

  it("activates through the activation endpoint and shows its own message when that fails", async () => {
    vi.mocked(activateAnalyzer).mockImplementation((_id, callback) =>
      callback({ error: "Analyzer not found: 501" } as never),
    );
    renderModal("activate");

    await userEvent.click(
      screen.getByRole("button", { name: "Activate analyzer" }),
    );

    expect(activateAnalyzer).toHaveBeenCalledWith("501", expect.any(Function));
    expect(reactivateAnalyzer).not.toHaveBeenCalled();
    expect(
      await screen.findByText(messages["analyzer.lifecycle.activate.error"]),
    ).toBeInTheDocument();
    expect(
      screen.queryByText("Analyzer not found: 501"),
    ).not.toBeInTheDocument();
  });
});
