import React from "react";
import { vi } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import messages from "../../../../languages/en.json";
import SentMessages, { maskAddress } from "../SentMessages";

const PAGE = {
  rows: [
    {
      id: 41,
      product: "ALERT",
      eventCode: "ALERT",
      ruleName: "Cholera RDT positive",
      firedAt: "2026-10-16T09:30:00+03:00",
      sentBy: null,
      automatic: true,
      recipientType: "TYPED_ADDRESS",
      recipientDisplayName: "Provincial Health Office",
      recipientEmail: "janet.kila@health.gov.pg",
      labNumber: "CPHL-26-004211",
      workflow: "clinical",
      status: "FAILED",
      channels: [
        { channel: "EMAIL", status: "FAILED", errorMessage: "Could not reach smtp.health.gov.pg on port 587", attempts: 4 },
      ],
    },
    {
      id: 42,
      product: "TRIGGER",
      eventCode: "REFERRAL_OUT",
      firedAt: "2026-10-16T09:31:00+03:00",
      sentBy: "Open ELIS",
      automatic: false,
      recipientType: "ORDERING_PROVIDER",
      recipientDisplayName: "Dr Kila",
      recipientPhone: "+67570000000",
      labNumber: "CPHL-26-004212",
      status: "QUEUED",
      channels: [{ channel: "WHATSAPP", status: "QUEUED", attempts: 0 }],
    },
    {
      id: 44,
      product: "TEST",
      eventCode: "TEST",
      firedAt: "2026-10-16T09:32:00+03:00",
      sentBy: "Open ELIS",
      automatic: false,
      recipientType: "TYPED_ADDRESS",
      recipientDisplayName: "admin@example.org",
      recipientEmail: "admin@example.org",
      status: "SENT",
      channels: [{ channel: "EMAIL", status: "SUCCESS", attempts: 1 }],
    },
  ],
  total: 3,
  page: 1,
  pageSize: 20,
};

const DETAIL = {
  ...PAGE.rows[0],
  renderedSubject: "CPHL alert: Cholera RDT positive",
  renderedMessage: "Lab number CPHL-26-004211: Cholera RDT Positive. Phone the lab.",
  attempts: [
    { channel: "EMAIL", attemptedAt: "2026-10-16T09:30:05+03:00", succeeded: false, errorMessage: "Could not reach smtp.health.gov.pg on port 587" },
  ],
};

const requests = [];
const addNotification = vi.fn();

vi.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer: vi.fn((url, callback) => {
    requests.push(url);
    if (url.startsWith("/rest/delivery/messages/")) {
      callback(DETAIL);
    } else {
      callback(PAGE);
    }
  }),
  postToOpenElisServerJsonResponse: vi.fn((url, body, callback) => {
    requests.push(`POST ${url}`);
    callback({ id: 43, status: "QUEUED" });
  }),
}));

vi.mock("../../../../config.json", () => ({
  default: { serverBaseUrl: "/api/OpenELIS-Global" },
}));

vi.mock("../../../common/PageBreadCrumb", () => ({
  default: () => <nav data-testid="breadcrumb" />,
}));

vi.mock("../../../layout/Layout", () => ({
  NotificationContext: React.createContext({
    notificationVisible: false,
    setNotificationVisible: () => {},
    addNotification: (notice) => addNotification(notice),
  }),
}));

vi.mock("../../../common/CustomNotification", () => ({
  AlertDialog: () => null,
  NotificationKinds: { success: "success", error: "error" },
}));

const renderPage = (path = "/SentMessages") =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <MemoryRouter initialEntries={[path]}>
        <SentMessages />
      </MemoryRouter>
    </IntlProvider>,
  );

describe("SentMessages", () => {
  beforeEach(() => {
    requests.length = 0;
    addNotification.mockClear();
  });

  it("lists every product with its status, masked address and attempts, and reads filters from the URL", async () => {
    renderPage("/SentMessages?product=alert&status=failed&labNo=CPHL-26-004211");
    expect(screen.getByRole("heading", { name: "Sent Messages" })).toBeInTheDocument();
    await waitFor(() => expect(screen.getByTestId("sent-row-41")).toBeInTheDocument());
    expect(requests[0]).toMatch(/^\/rest\/delivery\/messages\?/);
    expect(requests[0]).toContain("product=ALERT");
    expect(requests[0]).toContain("status=FAILED");
    expect(requests[0]).toContain("labNo=CPHL-26-004211");
    expect(screen.getByTestId("sent-status-41")).toHaveTextContent("Failed");
    expect(screen.getByTestId("sent-status-42")).toHaveTextContent("Queued");
    expect(screen.getByText("Cholera RDT positive")).toBeInTheDocument();
    expect(screen.getByText("ja••••••••@health.gov.pg")).toBeInTheDocument();
    expect(screen.getByText("Could not reach smtp.health.gov.pg on port 587")).toBeInTheDocument();
    expect(screen.getByText("Automatic")).toBeInTheDocument();
    expect(screen.getAllByText("Open ELIS").length).toBe(2);
  });

  it("shows a typed address once, masked", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("sent-row-44")).toBeInTheDocument());
    expect(screen.getByText("ad•••@example.org")).toBeInTheDocument();
    expect(screen.queryByText("admin@example.org")).not.toBeInTheDocument();
  });

  it("refreshes the list while a message is queued", async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true });
    try {
      renderPage();
      await waitFor(() => expect(screen.getByTestId("sent-row-42")).toBeInTheDocument());
      const listRequests = () => requests.filter((url) => url.startsWith("/rest/delivery/messages?")).length;
      const before = listRequests();
      await vi.advanceTimersByTimeAsync(5000);
      expect(listRequests()).toBe(before + 1);
    } finally {
      vi.useRealTimers();
    }
  });

  it("offers Resend only for rows that are not queued and queues the message again", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("sent-row-41")).toBeInTheDocument());
    expect(screen.getByTestId("resend-41")).toBeInTheDocument();
    expect(screen.queryByTestId("resend-42")).not.toBeInTheDocument();

    fireEvent.click(screen.getByTestId("resend-41"));

    await waitFor(() => expect(requests).toContain("POST /rest/delivery/messages/41/resend"));
    await waitFor(() => expect(addNotification).toHaveBeenCalled());
    expect(addNotification.mock.calls[0][0].message).toBe("Message queued again.");
  });

  it("expands a row to the rendered text and every attempt", async () => {
    renderPage();
    await waitFor(() => expect(screen.getByTestId("sent-row-41")).toBeInTheDocument());
    const expand = screen.getAllByRole("button", { name: /expand current row/i })[0];
    fireEvent.click(expand);
    await waitFor(() => expect(screen.getByText("CPHL alert: Cholera RDT positive")).toBeInTheDocument());
    expect(requests).toContain("/rest/delivery/messages/41");
    expect(screen.getByText(/Phone the lab/)).toBeInTheDocument();
    expect(screen.getByText("Open in the Report Print Queue")).toBeInTheDocument();
  });

  it("masks addresses and numbers", () => {
    expect(maskAddress("janet.kila@health.gov.pg")).toBe("ja••••••••@health.gov.pg");
    expect(maskAddress("+675 7000 0000")).toBe("+675•••••000");
    expect(maskAddress("")).toBe("");
  });
});
