import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import { IntlProvider } from "react-intl";
import CriticalCommunicationPanel from "../CriticalCommunicationPanel";
import messages from "../../../languages/en.json";

const renderPanel = (service) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <CriticalCommunicationPanel
        caseId="case-1"
        sampleItemId="sample-1"
        service={service}
      />
    </IntlProvider>,
  );

describe("CriticalCommunicationPanel", () => {
  it("logs, acknowledges, and closes critical communication", async () => {
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

    fireEvent.change(await screen.findByLabelText("Recipient"), {
      target: { value: "Provider on call" },
    });
    fireEvent.change(screen.getByLabelText("Message"), {
      target: { value: "Positive blood culture called" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Log communication" }));

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

    fireEvent.click(screen.getByRole("button", { name: "Acknowledge" }));

    await waitFor(() =>
      expect(service.acknowledgeCriticalCommunication).toHaveBeenCalledWith(
        "comm-1",
      ),
    );
    expect(await screen.findByText("Acknowledged")).toBeInTheDocument();

    fireEvent.click(
      screen.getByRole("button", { name: "Close communication" }),
    );
    fireEvent.change(screen.getByLabelText("Resolution note"), {
      target: { value: "Read-back documented" },
    });
    fireEvent.click(
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
});
