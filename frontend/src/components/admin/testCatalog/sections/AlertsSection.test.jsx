/**
 * AlertsSection — OGC-949 / OGC-994..997.
 *
 * Per-test alert rules table with trigger/channels/ack/enable columns, plus the
 * Add Rule affordance. Covers render with rules, empty state, error state, and
 * the Add Rule button.
 */

// ========== MOCKS (before imports) ==========
vi.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  postToOpenElisServer: vi.fn(),
  putToOpenElisServer: vi.fn(),
  deleteFromOpenElisServer: vi.fn(),
}));

// ========== IMPORTS ==========
import React from "react";
import { render, screen, within } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import AlertsSection from "./AlertsSection";
import { getFromOpenElisServer } from "../../../utils/Utils";
import messages from "../../../../languages/en.json";

const RULES = [
  {
    id: "rule-1",
    testId: "42",
    name: "Critical SMS",
    enabled: true,
    triggerType: "CRITICAL",
    triggerValue: null,
    notifySms: true,
    notifyEmail: false,
    acknowledgmentRequired: true,
  },
  {
    id: "rule-2",
    testId: "42",
    name: "Positive Email",
    enabled: false,
    triggerType: "SPECIFIC_VALUE",
    triggerValue: "Positive",
    notifySms: false,
    notifyEmail: true,
    acknowledgmentRequired: false,
  },
];

const renderSection = () =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <AlertsSection testId="42" />
    </IntlProvider>,
  );

beforeEach(() => {
  vi.clearAllMocks();
});

describe("AlertsSection", () => {
  it("lists alert rules with trigger and channels", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) => cb(RULES));
    renderSection();

    expect(await screen.findByText("Critical SMS")).toBeInTheDocument();
    expect(screen.getByText("Positive Email")).toBeInTheDocument();
    // Trigger tag uses the localized label (scoped to the row — the closed
    // Add/Edit modal also mounts radio labels with the same text).
    const row1 = screen.getByTestId("alert-row-rule-1");
    expect(
      within(row1).getByText(
        messages["label.testCatalog.alerts.trigger.CRITICAL"],
      ),
    ).toBeInTheDocument();
    const row2 = screen.getByTestId("alert-row-rule-2");
    expect(
      within(row2).getByText(/Specific Value: Positive/),
    ).toBeInTheDocument();
    // Per-row enable toggles render.
    expect(document.getElementById("enabled-rule-1")).toBeInTheDocument();
  });

  it("shows the empty state when no rules exist", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) => cb([]));
    renderSection();
    expect(
      await screen.findByText(messages["label.testCatalog.alerts.empty"]),
    ).toBeInTheDocument();
  });

  it("shows an error state when the fetch fails", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) => cb(undefined));
    renderSection();
    expect(
      await screen.findByText(messages["label.testCatalog.alerts.loadError"]),
    ).toBeInTheDocument();
  });

  it("renders the Add Rule button", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) => cb([]));
    renderSection();
    expect(await screen.findByTestId("add-rule-button")).toBeInTheDocument();
  });
});
