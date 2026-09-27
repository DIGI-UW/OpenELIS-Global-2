import React from "react";
import { render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import userEvent from "@testing-library/user-event";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import CriticalCommunicationPanel from "../CriticalCommunicationPanel";
import messages from "../../../languages/en.json";

const renderPanel = (service, props = {}) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <CriticalCommunicationPanel
        caseId="case-1"
        sampleItemId="sample-1"
        service={service}
        {...props}
      />
    </IntlProvider>,
  );

describe("CriticalCommunicationPanel", () => {
  it("offers projected patient-report results as communication targets", async () => {
    const user = userEvent.setup();
    const service = {
      getCriticalCommunications: vi.fn().mockResolvedValue([]),
    };

    renderPanel(service, { projectedResultIds: ["result-1", "result-2"] });

    await user.selectOptions(
      await screen.findByLabelText("Critical result target"),
      "RESULT",
    );

    expect(
      screen.getByRole("option", { name: "result-1" }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("option", { name: "result-2" }),
    ).toBeInTheDocument();
  });

  it("logs, acknowledges, and closes critical communication", async () => {
    const user = userEvent.setup();
    const service = {
      getCriticalCommunications: vi
        .fn()
        .mockResolvedValueOnce([])
        .mockResolvedValueOnce([
          {
            id: "comm-1",
            recipient: "Provider on call",
            message: "Positive blood culture called",
            acknowledgementStatus: "OPEN",
          },
        ])
        .mockResolvedValueOnce([
          {
            id: "comm-1",
            recipient: "Provider on call",
            message: "Positive blood culture called",
            acknowledgementStatus: "ACKNOWLEDGED",
          },
        ])
        .mockResolvedValueOnce([
          {
            id: "comm-1",
            recipient: "Provider on call",
            message: "Positive blood culture called",
            acknowledgementStatus: "CLOSED",
            resolutionNote: "Read-back documented",
          },
        ]),
      logCriticalCommunication: vi.fn().mockResolvedValue({ id: "comm-1" }),
      acknowledgeCriticalCommunication: vi
        .fn()
        .mockResolvedValue({ id: "comm-1" }),
      closeCriticalCommunication: vi.fn().mockResolvedValue({ id: "comm-1" }),
    };

    renderPanel(service);

    await user.type(
      await screen.findByLabelText("Recipient"),
      "Provider on call",
    );
    await user.type(
      screen.getByLabelText("Message"),
      "Positive blood culture called",
    );
    await user.click(screen.getByRole("button", { name: "Log communication" }));

    await waitFor(() =>
      expect(service.logCriticalCommunication).toHaveBeenCalledWith("case-1", {
        targetType: "CASE",
        targetId: "case-1",
        recipient: "Provider on call",
        recipientContact: "",
        communicationMethod: "PHONE",
        message: "Positive blood culture called",
        followUpNeeded: true,
      }),
    );
    expect(await screen.findByText("Open")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Acknowledge" }));

    await waitFor(() =>
      expect(service.acknowledgeCriticalCommunication).toHaveBeenCalledWith(
        "comm-1",
      ),
    );
    expect(await screen.findByText("Acknowledged")).toBeInTheDocument();

    await user.click(
      screen.getByRole("button", { name: "Close communication" }),
    );
    await user.type(
      screen.getByLabelText("Resolution note"),
      "Read-back documented",
    );
    await user.click(
      screen.getAllByRole("button", { name: "Close communication" })[1],
    );

    await waitFor(() =>
      expect(service.closeCriticalCommunication).toHaveBeenCalledWith(
        "comm-1",
        {
          resolutionNote: "Read-back documented",
        },
      ),
    );
    expect(await screen.findByText("Closed")).toBeInTheDocument();
  });

  it("locks a communication opened from an isolate entry point to that isolate", async () => {
    const service = {
      getCriticalCommunications: vi.fn().mockResolvedValue([]),
      logCriticalCommunication: vi.fn().mockResolvedValue({ id: "comm-1" }),
    };
    const user = userEvent.setup();

    renderPanel(service, {
      isolates: [{ id: "isolate-1", isolateLabel: "ISO-1" }],
      entryTargetType: "ISOLATE",
      entryTargetId: "isolate-1",
    });

    expect(
      await screen.findByLabelText("Critical result target"),
    ).toBeDisabled();
    expect(screen.getByLabelText("Target record")).toBeDisabled();
    await user.type(screen.getByLabelText("Recipient"), "Provider on call");
    await user.type(screen.getByLabelText("Message"), "Critical isolate");
    await user.click(screen.getByRole("button", { name: "Log communication" }));

    await waitFor(() =>
      expect(service.logCriticalCommunication).toHaveBeenCalledWith("case-1", {
        targetType: "ISOLATE",
        targetId: "isolate-1",
        recipient: "Provider on call",
        recipientContact: "",
        communicationMethod: "PHONE",
        message: "Critical isolate",
        followUpNeeded: true,
      }),
    );
  });
});
