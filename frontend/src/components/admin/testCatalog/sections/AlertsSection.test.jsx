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
  putToOpenElisServerFullResponse: vi.fn(),
  deleteFromOpenElisServer: vi.fn(),
}));

// ========== IMPORTS ==========
import React from "react";
import { fireEvent, render, screen, within } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import AlertsSection from "./AlertsSection";
import {
  deleteFromOpenElisServer,
  getFromOpenElisServer,
  putToOpenElisServerFullResponse,
} from "../../../utils/Utils";
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

  /** OGC-1463: each toggle sends the version the server last returned. */
  it("sends the rule's version and takes the new one from the answer", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) =>
      cb([{ ...RULES[0], version: "v1" }]),
    );
    putToOpenElisServerFullResponse.mockImplementation((url, body, cb) =>
      cb({
        status: 200,
        json: () =>
          Promise.resolve({
            ...RULES[0],
            enabled: JSON.parse(body).enabled,
            version: JSON.parse(body).version === "v1" ? "v2" : "v3",
          }),
      }),
    );
    renderSection();
    const toggle = await waitFor(() => {
      const el = document.getElementById("enabled-rule-1");
      expect(el).not.toBeNull();
      return el;
    });

    fireEvent.click(toggle);
    await waitFor(() =>
      expect(putToOpenElisServerFullResponse).toHaveBeenCalledTimes(1),
    );
    await waitFor(() =>
      expect(toggle).toHaveAttribute("aria-checked", "false"),
    );
    fireEvent.click(toggle);
    await waitFor(() =>
      expect(putToOpenElisServerFullResponse).toHaveBeenCalledTimes(2),
    );

    const versions = putToOpenElisServerFullResponse.mock.calls.map(
      ([, body]) => JSON.parse(body).version,
    );
    expect(versions).toEqual(["v1", "v2"]);
  });

  it("says so when a toggle is refused as stale", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) =>
      cb([{ ...RULES[0], version: "v1" }]),
    );
    putToOpenElisServerFullResponse.mockImplementation((url, body, cb) =>
      cb({
        status: 409,
        json: () => Promise.resolve({ conflict: "stale", version: "v9" }),
      }),
    );
    renderSection();
    await screen.findByText("Critical SMS");

    fireEvent.click(document.getElementById("enabled-rule-1"));

    expect(await screen.findByTestId("alerts-stale-save")).toHaveTextContent(
      messages["error.staleSave.section"],
    );
  });

  // OGC-1463 review: a delete from a page older than another admin's edit is
  // refused, and the section says so.
  it("sends the rule's version with a delete and shows a stale refusal", async () => {
    getFromOpenElisServer.mockImplementation((url, cb) =>
      cb([{ ...RULES[0], version: "v1" }]),
    );
    deleteFromOpenElisServer.mockImplementation((url, cb) => cb(409));
    renderSection();
    fireEvent.click(await screen.findByTestId("delete-alert-rule-1"));
    const dialog = await screen.findByRole("dialog", {
      name: messages["label.testCatalog.alerts.delete.title"],
    });
    fireEvent.click(within(dialog).getByRole("button", { name: /Delete$/ }));

    await waitFor(() => expect(deleteFromOpenElisServer).toHaveBeenCalled());
    expect(deleteFromOpenElisServer.mock.calls[0][0]).toBe(
      "/rest/test-catalog/42/alerts/rule-1?version=v1",
    );
    expect(await screen.findByTestId("alerts-stale-save")).toHaveTextContent(
      messages["error.staleSave.section"],
    );
  });
});
