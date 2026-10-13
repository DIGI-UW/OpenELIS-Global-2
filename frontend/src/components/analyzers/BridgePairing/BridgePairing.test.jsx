vi.mock("../../../services/analyzerService", () => ({
  getBridgePairing: vi.fn(),
  pairBridge: vi.fn(),
}));

import React from "react";
import { render, screen } from "@testing-library/react";
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

    expect(await screen.findByTestId("bridge-pairing-error")).toHaveTextContent(
      "The Bridge did not accept that code.",
    );
    expect(screen.queryByTestId("bridge-pairing-paired")).toBeNull();
  });

  it("offers to pair again once paired", async () => {
    getBridgePairing.mockImplementation((callback) => callback(PAIRED));
    renderPairing();

    expect(screen.queryByTestId("bridge-pairing-form")).toBeNull();
    await userEvent.click(screen.getByTestId("bridge-pairing-again"));

    expect(screen.getByTestId("bridge-pairing-form")).toBeInTheDocument();
  });

  it("shows nothing when no Bridge is configured", () => {
    getBridgePairing.mockImplementation((callback) =>
      callback({ ...UNPAIRED, bridgeUrl: "" }),
    );
    renderPairing();

    expect(screen.queryByTestId("bridge-pairing")).toBeNull();
  });
});
