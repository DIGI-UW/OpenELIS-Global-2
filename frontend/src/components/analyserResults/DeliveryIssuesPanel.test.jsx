import React from "react";
import { render, screen, within } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import userEvent from "@testing-library/user-event";
import { IntlProvider } from "react-intl";
import { beforeEach, describe, expect, it, vi } from "vitest";

import messages from "../../languages/en.json";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../utils/Utils";
import DeliveryIssuesPanel from "./DeliveryIssuesPanel";

vi.mock("../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn(),
  postToOpenElisServerJsonResponse: vi.fn(),
}));

const RECEIPT_ID =
  "recv-v1:72468791ea1a5109de4fba64eb61ff380b1c49883acdfc492559fc331e55b674";

const deadLettered = {
  id: RECEIPT_ID,
  state: "DMQ",
  analyzerId: "12",
  analyzerName: "GeneXpert bench 1",
  connectionId: "conn-7",
  sourceId: "10.1.2.3",
  protocol: "ASTM",
  accession: "DEV0126100001",
  attempts: 1,
  receivedAt: "2026-09-24T01:00:00Z",
  failureReason: "OE_CONFIG_STATE",
  lastHttpStatus: 422,
  lastError: "No site binding for profile genexpert-astm revision 5",
  actionable: true,
};

const retrying = {
  id: "recv-v1:bbbb",
  state: "RETRYING",
  analyzerId: "12",
  analyzerName: "GeneXpert bench 1",
  attempts: 3,
  receivedAt: "2026-09-24T02:00:00Z",
  failureReason: null,
  lastHttpStatus: 503,
  lastError: "Service Unavailable",
  actionable: false,
};

const unrecognized = {
  id: "recv-v1:cccc",
  state: "DMQ",
  analyzerId: null,
  analyzerName: null,
  sourceId: "192.168.158.1",
  protocol: "ASTM",
  attempts: 0,
  receivedAt: "2026-09-24T03:00:00Z",
  failureReason: "UNREGISTERED_SOURCE",
  lastError: "No saved analyzer connection for source 192.168.158.1",
  actionable: true,
};

const respondWith = (rows) =>
  getFromOpenElisServer.mockImplementation((_url, callback) =>
    callback({ status: "success", data: { count: rows.length, rows } }),
  );

const renderPanel = () =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <DeliveryIssuesPanel />
    </IntlProvider>,
  );

const rowFor = (text) => screen.getByText(text).closest("tr");

describe("DeliveryIssuesPanel", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("explains each undelivered result and offers actions only on results the Bridge stopped retrying", async () => {
    respondWith([deadLettered, retrying, unrecognized]);
    renderPanel();

    expect(
      await screen.findByRole("heading", {
        name: "Undelivered analyzer results",
      }),
    ).toBeVisible();
    expect(getFromOpenElisServer).toHaveBeenCalledWith(
      "/rest/analyzer/delivery-issues",
      expect.any(Function),
    );

    const configRow = rowFor(
      "No site binding for profile genexpert-astm revision 5",
    );
    expect(within(configRow).getByText("GeneXpert bench 1")).toBeVisible();
    expect(
      within(configRow).getByText(
        "OpenELIS declined the result because of its own setup. Fix the analyzer setup, then retry.",
      ),
    ).toBeVisible();
    expect(within(configRow).getByText("Not delivered")).toBeVisible();
    expect(
      within(configRow).getByRole("button", { name: "Retry" }),
    ).toBeVisible();
    expect(
      within(configRow).getByRole("button", { name: "Dismiss" }),
    ).toBeVisible();

    const retryingRow = rowFor("Service Unavailable");
    expect(within(retryingRow).getByText("Retrying")).toBeVisible();
    expect(
      within(retryingRow).queryByRole("button", { name: "Retry" }),
    ).not.toBeInTheDocument();

    const senderRow = rowFor(
      "No saved analyzer connection for source 192.168.158.1",
    );
    expect(
      within(senderRow).getByText("Unrecognized sender 192.168.158.1"),
    ).toBeVisible();
    expect(
      within(senderRow).getByText(
        "The sender matches no saved analyzer connection. Set up the analyzer, then retry.",
      ),
    ).toBeVisible();
  });

  it("retries one result through OpenELIS and reloads the list", async () => {
    respondWith([deadLettered]);
    postToOpenElisServerJsonResponse.mockImplementation(
      (_url, _body, callback) => {
        respondWith([]);
        callback({ status: "success", id: RECEIPT_ID });
      },
    );
    renderPanel();

    await userEvent.click(await screen.findByRole("button", { name: "Retry" }));

    expect(postToOpenElisServerJsonResponse).toHaveBeenCalledWith(
      `/rest/analyzer/delivery-issues/${encodeURIComponent(RECEIPT_ID)}/retry`,
      "{}",
      expect.any(Function),
    );
    expect(
      await screen.findByText("The Bridge has no undelivered results."),
    ).toBeVisible();
  });

  it("shows why the Bridge refused an action", async () => {
    respondWith([deadLettered]);
    postToOpenElisServerJsonResponse.mockImplementation(
      (_url, _body, callback) =>
        callback({
          status: 409,
          messageKey: "analyzer.deliveryIssues.error.bridgeRefused",
          messageArgs: { status: 409, reason: "not_dead_lettered" },
        }),
    );
    renderPanel();

    await userEvent.click(
      await screen.findByRole("button", { name: "Dismiss" }),
    );

    expect(
      await screen.findByText(
        "The Bridge refused this action (not_dead_lettered). Reload to see its current state.",
      ),
    ).toBeVisible();
  });

  it("says so when the Bridge cannot be reached", async () => {
    getFromOpenElisServer.mockImplementation((_url, callback) =>
      callback(undefined),
    );
    renderPanel();

    expect(
      await screen.findByText(
        "Undelivered results could not be loaded from the Analyzer Bridge.",
      ),
    ).toBeVisible();
    await waitFor(() =>
      expect(
        screen.queryByText("The Bridge has no undelivered results."),
      ).not.toBeInTheDocument(),
    );
  });
});
