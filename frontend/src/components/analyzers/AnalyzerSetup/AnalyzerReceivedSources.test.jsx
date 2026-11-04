import React from "react";
import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { IntlProvider } from "react-intl";
import { beforeEach, expect, it, vi } from "vitest";
import messages from "../../../languages/en.json";
import { getAnalyzerDeliveryIssues } from "../../../services/analyzerService";
import AnalyzerReceivedSources, {
  unassignedSources,
} from "./AnalyzerReceivedSources";

vi.mock("../../../services/analyzerService", () => ({
  getAnalyzerDeliveryIssues: vi.fn(),
}));
const source = {
  id: "recv-v1:123",
  state: "DMQ",
  connectionId: null,
  sender: "GeneXpert PC^GeneXpert^1.0",
  sourceId: "10.1.2.3",
  listenerPort: 12001,
  protocol: "ASTM",
  transport: "TCP",
  failureReason: "UNREGISTERED_SOURCE",
};
beforeEach(() => vi.resetAllMocks());

it("refreshes traffic received after opening Connect and adopts the observed identity without typing", async () => {
  getAnalyzerDeliveryIssues.mockImplementationOnce((callback) =>
    callback({ status: "success", data: { rows: [] } }),
  );
  const onAdopt = vi.fn();
  render(
    <IntlProvider locale="en" messages={messages}>
      <AnalyzerReceivedSources protocol="ASTM" onAdopt={onAdopt} />
    </IntlProvider>,
  );
  expect(screen.getByText(/No unassigned senders/)).toBeVisible();
  getAnalyzerDeliveryIssues.mockImplementationOnce((callback) =>
    callback({ status: "success", data: { rows: [source] } }),
  );
  await userEvent.click(
    screen.getByRole("button", { name: "Check for received senders" }),
  );
  const row = screen.getByTestId(`analyzer-received-source-${source.id}`);
  expect(row).toHaveTextContent(
    "GeneXpert PC^GeneXpert^1.0 — 10.1.2.3, port 12001",
  );
  await userEvent.click(
    within(row).getByRole("button", { name: "This is my analyzer" }),
  );
  expect(onAdopt).toHaveBeenCalledWith(source);
});

it("keeps same-sender instruments at different addresses distinct and excludes bound or incompatible messages", () => {
  const second = { ...source, id: "second", sourceId: "10.1.2.4" };
  expect(
    unassignedSources(
      [
        source,
        { ...source, id: "duplicate" },
        second,
        { ...source, id: "bound", connectionId: "another-analyzer" },
        { ...source, id: "wrong-protocol", protocol: "HL7" },
        { ...source, id: "wrong-transport", transport: "SERIAL" },
        { ...source, id: "interpreted", failureReason: "OE_REJECTED" },
      ],
      "ASTM",
    ),
  ).toEqual([source, second]);
});

it("reports a failed refresh rather than claiming no traffic", () => {
  getAnalyzerDeliveryIssues.mockImplementation((callback) =>
    callback({ status: "error" }),
  );
  render(
    <IntlProvider locale="en" messages={messages}>
      <AnalyzerReceivedSources protocol="ASTM" onAdopt={vi.fn()} />
    </IntlProvider>,
  );
  expect(
    screen.getByText("Received senders could not be loaded. Try again."),
  ).toBeVisible();
  expect(screen.queryByText(/No unassigned senders/)).not.toBeInTheDocument();
});
