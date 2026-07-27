/**
 * AnalyzersSection — OGC-949 M11 / OGC-959..960.
 *
 * Read-only section: lists the analyzers mapped to this test (derived from
 * analyzer test-code mappings), with an info card explaining it's edited
 * elsewhere, plus empty + error states. No writes.
 */

// ========== MOCKS (before imports) ==========
vi.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
}));

// ========== IMPORTS ==========
import React from "react";
import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import AnalyzersSection from "./AnalyzersSection";
import { getFromOpenElisServer } from "../../../utils/Utils";
import messages from "../../../../languages/en.json";

const renderSection = () =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <AnalyzersSection testId="42" />
    </IntlProvider>,
  );

beforeEach(() => {
  vi.clearAllMocks();
});

describe("AnalyzersSection", () => {
  it("lists the analyzers returned by the server with the info card", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) =>
      cb({
        testId: "42",
        analyzers: [
          {
            analyzerId: "7",
            analyzerName: "Cobalt 9000",
            analyzerTestName: "Cobalt Glucose",
          },
          {
            analyzerId: "9",
            analyzerName: "Sysmex XN",
            analyzerTestName: "XN Hgb",
          },
        ],
      }),
    );
    renderSection();
    // Both analyzer names and their analyzer-specific test names are rendered.
    expect(await screen.findByText("Cobalt 9000")).toBeInTheDocument();
    expect(screen.getByText("Cobalt Glucose")).toBeInTheDocument();
    expect(screen.getByText("Sysmex XN")).toBeInTheDocument();
    expect(screen.getByText("XN Hgb")).toBeInTheDocument();
    // The read-only info card is shown.
    expect(
      screen.getByText(messages["label.testCatalog.analyzers.infoCard"]),
    ).toBeInTheDocument();
  });

  it("shows the empty state when no analyzers are mapped", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) =>
      cb({ testId: "42", analyzers: [] }),
    );
    renderSection();
    expect(
      await screen.findByText(messages["label.testCatalog.analyzers.empty"]),
    ).toBeInTheDocument();
  });

  it("shows an error state when the fetch fails", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) => cb(undefined));
    renderSection();
    expect(
      await screen.findByText(
        messages["label.testCatalog.analyzers.loadError"],
      ),
    ).toBeInTheDocument();
  });
});
