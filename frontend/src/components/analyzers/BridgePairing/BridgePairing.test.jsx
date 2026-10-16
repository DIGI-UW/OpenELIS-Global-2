vi.mock("../../../services/analyzerService", () => ({
  getBridgePairing: vi.fn(),
  pairBridge: vi.fn(),
}));

import React from "react";
import { render, screen, within } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import userEvent from "@testing-library/user-event";
import { vi } from "vitest";
import { IntlProvider } from "react-intl";
import BridgePairing from "./BridgePairing";
import {
  getBridgePairing,
  pairBridge,
} from "../../../services/analyzerService";
import messages from "../../../languages/en.json";

const UNPAIRED = {
  bridgeUrl: "https://bridge.openelis.org:8443",
  paired: false,
  pairsAutomatically: false,
};
const PAIRED = {
  ...UNPAIRED,
  paired: true,
  bridgeCertificateSha256: "ab".repeat(32),
  clientCertificateSha256: "cd".repeat(32),
  pairedAt: "2026-10-08T12:00:00Z",
};

const renderPairing = () =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <BridgePairing />
    </IntlProvider>,
  );

describe("BridgePairing", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("asks for the code while the Bridge is unpaired, then shows the pairing", async () => {
    getBridgePairing.mockImplementation((callback) => callback(UNPAIRED));
    pairBridge.mockImplementation((code, callback) => callback(PAIRED));
    renderPairing();

    expect(
      screen.getByText("The Analyzer Bridge is not paired"),
    ).toBeInTheDocument();
    await userEvent.type(screen.getByLabelText("Pairing code"), "ABCD-EFGH");
    await userEvent.click(screen.getByTestId("bridge-pairing-submit"));

    expect(pairBridge).toHaveBeenCalledWith("ABCD-EFGH", expect.any(Function));
    await waitFor(() =>
      expect(screen.getByTestId("bridge-pairing-paired")).toHaveTextContent(
        "Paired with the Analyzer Bridge",
      ),
    );
    expect(screen.getByTestId("bridge-pairing-paired")).toHaveTextContent(
      "abababababababab",
    );
  });

  it("names why the Bridge refused the code", async () => {
    getBridgePairing.mockImplementation((callback) => callback(UNPAIRED));
    pairBridge.mockImplementation((code, callback) =>
      callback({
        errorKey: "analyzer.bridgePairing.error.wrongCode",
        status: 422,
      }),
    );
    renderPairing();

    await userEvent.type(screen.getByLabelText("Pairing code"), "WRONG");
    await userEvent.click(screen.getByTestId("bridge-pairing-submit"));

    // A problem with the code is said on the field itself.
    expect(
      await screen.findByText("The Bridge did not accept that code."),
    ).toBeInTheDocument();
    expect(screen.getByLabelText("Pairing code")).toBeInvalid();
    expect(screen.queryByTestId("bridge-pairing-paired")).toBeNull();
  });

  it("reports a Bridge it could not reach as a notification, not a field error", async () => {
    getBridgePairing.mockImplementation((callback) => callback(UNPAIRED));
    pairBridge.mockImplementation((code, callback) =>
      callback({
        errorKey: "analyzer.bridgePairing.error.unreachable",
        status: 502,
      }),
    );
    renderPairing();

    await userEvent.type(screen.getByLabelText("Pairing code"), "ABCD");
    await userEvent.click(screen.getByTestId("bridge-pairing-submit"));

    expect(await screen.findByTestId("bridge-pairing-error")).toHaveTextContent(
      "OpenELIS could not reach the Analyzer Bridge.",
    );
    expect(screen.getByLabelText("Pairing code")).not.toBeInvalid();
  });

  it("shows the pairing as labelled facts and offers to pair again in a dialog", async () => {
    getBridgePairing.mockImplementation((callback) => callback(PAIRED));
    pairBridge.mockImplementation((code, callback) =>
      callback({ ...PAIRED, pairedAt: "2026-10-13T09:00:00Z" }),
    );
    renderPairing();

    const paired = screen.getByTestId("bridge-pairing-paired");
    expect(paired).toHaveTextContent("Bridge");
    expect(paired).toHaveTextContent("https://bridge.openelis.org:8443");
    expect(paired).toHaveTextContent("Certificate");
    expect(paired).toHaveTextContent("Paired on");
    expect(screen.queryByTestId("bridge-pairing-form")).toBeNull();

    await userEvent.click(screen.getByTestId("bridge-pairing-again"));
    const dialog = screen.getByRole("dialog", {
      name: "Pair with the Analyzer Bridge",
    });
    expect(screen.getByTestId("bridge-pairing-form")).toBeInTheDocument();
    await userEvent.type(screen.getByLabelText("Pairing code"), "NEW-CODE");
    await userEvent.click(within(dialog).getByRole("button", { name: "Pair" }));

    expect(pairBridge).toHaveBeenCalledWith("NEW-CODE", expect.any(Function));
    await waitFor(() =>
      expect(screen.queryByRole("dialog")).not.toBeInTheDocument(),
    );
  });

  it("shows nothing when no Bridge is configured", () => {
    getBridgePairing.mockImplementation((callback) =>
      callback({ ...UNPAIRED, bridgeUrl: "" }),
    );
    renderPairing();

    expect(screen.queryByTestId("bridge-pairing")).toBeNull();
  });
});
