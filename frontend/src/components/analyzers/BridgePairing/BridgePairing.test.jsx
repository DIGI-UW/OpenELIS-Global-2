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

  it("says when the Bridge presents a different certificate and asks for its code", () => {
    getBridgePairing.mockImplementation((callback) =>
      callback({ ...PAIRED, pinCheck: "OTHER_CERTIFICATE" }),
    );
    renderPairing();

    expect(screen.getByTestId("bridge-pairing-stale")).toHaveTextContent(
      "Paired with a different Bridge certificate",
    );
    expect(screen.getByTestId("bridge-pairing-stale")).toHaveTextContent(
      "abababababababab",
    );
    expect(screen.queryByTestId("bridge-pairing-paired")).toBeNull();
    expect(screen.getByTestId("bridge-pairing-form")).toBeInTheDocument();
  });

  it("says when the Bridge lost the pairing", () => {
    getBridgePairing.mockImplementation((callback) =>
      callback({
        ...PAIRED,
        pinCheck: "BRIDGE_UNPAIRED",
        pairsAutomatically: true,
      }),
    );
    renderPairing();

    expect(screen.getByTestId("bridge-pairing-stale")).toHaveTextContent(
      "The Analyzer Bridge lost its pairing",
    );
    expect(screen.getByTestId("bridge-pairing-stale")).toHaveTextContent(
      "OpenELIS pairs with the code it is configured with on its own.",
    );
  });

  it("shows a pin the Bridge confirms as paired", () => {
    getBridgePairing.mockImplementation((callback) =>
      callback({ ...PAIRED, pinCheck: "MATCHES" }),
    );
    renderPairing();

    expect(screen.getByTestId("bridge-pairing-paired")).toBeInTheDocument();
    expect(screen.queryByTestId("bridge-pairing-stale")).toBeNull();
  });

  it("says when no Bridge is configured instead of showing nothing (OGC-1546)", () => {
    getBridgePairing.mockImplementation((callback) =>
      callback({ ...UNPAIRED, bridgeUrl: "" }),
    );
    renderPairing();

    expect(
      screen.getByTestId("bridge-pairing-not-configured"),
    ).toHaveTextContent(
      "No Analyzer Bridge address is configured for OpenELIS.",
    );
  });

  it("says no Bridge is configured when a paired install lost its address (OGC-1546)", () => {
    getBridgePairing.mockImplementation((callback) =>
      callback({ ...PAIRED, bridgeUrl: "", pinCheck: null }),
    );
    renderPairing();

    expect(
      screen.getByTestId("bridge-pairing-not-configured"),
    ).toHaveTextContent(
      "No Analyzer Bridge address is configured for OpenELIS.",
    );
    expect(screen.queryByText("Paired with the Analyzer Bridge")).toBeNull();
  });

  it("says when the paired Bridge cannot be reached instead of showing it paired (OGC-1546)", () => {
    getBridgePairing.mockImplementation((callback) =>
      callback({ ...PAIRED, pinCheck: "UNREACHABLE" }),
    );
    renderPairing();

    expect(screen.getByTestId("bridge-pairing-unreachable")).toHaveTextContent(
      "OpenELIS could not reach the Analyzer Bridge.",
    );
    expect(screen.queryByTestId("bridge-pairing-paired")).toBeNull();
  });

  it("shows nothing when the pairing status cannot be read", () => {
    getBridgePairing.mockImplementation((callback) => callback(undefined));
    renderPairing();

    expect(screen.queryByTestId("bridge-pairing")).toBeNull();
  });
});
