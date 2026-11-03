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

  it("logs a call with its outcome and resolves its follow-up without an acknowledge step", async () => {
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
            outcome: "UNABLE_TO_REACH",
            followUpNeeded: true,
            acknowledgementStatus: "OPEN",
          },
        ])
        .mockResolvedValueOnce([
          {
            id: "comm-1",
            recipient: "Provider on call",
            message: "Positive blood culture called",
            outcome: "UNABLE_TO_REACH",
            followUpNeeded: true,
            acknowledgementStatus: "CLOSED",
            resolutionNote: "Reached the ward on the second call",
          },
        ]),
      logCriticalCommunication: vi.fn().mockResolvedValue({ id: "comm-1" }),
      closeCriticalCommunication: vi.fn().mockResolvedValue({ id: "comm-1" }),
    };

    renderPanel(service);

    await user.type(
      await screen.findByLabelText("Recipient"),
      "Provider on call",
    );
    await user.selectOptions(
      screen.getByLabelText("Call outcome"),
      "Unable to reach",
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
        outcome: "UNABLE_TO_REACH",
        message: "Positive blood culture called",
        followUpNeeded: true,
      }),
    );
    expect(await screen.findByText("Open")).toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: "Acknowledge" }),
    ).not.toBeInTheDocument();
    expect(screen.getByLabelText("Call outcome")).toHaveValue("");

    await user.click(screen.getByRole("button", { name: "Resolve follow-up" }));
    const save = screen.getByRole("button", { name: "Resolve follow-up" });
    expect(save).toBeDisabled();
    await user.type(
      screen.getByLabelText("Resolution note"),
      "Reached the ward on the second call",
    );
    expect(save).toBeEnabled();
    await user.click(save);

    await waitFor(() =>
      expect(service.closeCriticalCommunication).toHaveBeenCalledWith(
        "comm-1",
        {
          resolutionNote: "Reached the ward on the second call",
        },
      ),
    );
    expect(await screen.findByText("Closed")).toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: "Resolve follow-up" }),
    ).not.toBeInTheDocument();
  });

  it("offers Resolve follow-up and a status only on a record that needs follow-up", async () => {
    const service = {
      getCriticalCommunications: vi.fn().mockResolvedValue([
        {
          id: "comm-1",
          recipient: "Provider on call",
          message: "Read back by the ward",
          outcome: "CONFIRMED",
          followUpNeeded: false,
          acknowledgementStatus: "OPEN",
        },
        {
          id: "comm-2",
          recipient: "Provider on call",
          message: "No answer on the ward",
          outcome: "UNABLE_TO_REACH",
          followUpNeeded: true,
          acknowledgementStatus: "OPEN",
        },
      ]),
    };

    renderPanel(service);

    expect(
      await screen.findByRole("button", { name: "Resolve follow-up" }),
    ).toBeInTheDocument();
    expect(
      screen.getAllByRole("button", { name: "Resolve follow-up" }),
    ).toHaveLength(1);
    expect(screen.getAllByTestId("microbiology-critical-status")).toHaveLength(
      1,
    );
  });

  it("requires a call outcome before logging", async () => {
    const user = userEvent.setup();
    const service = {
      getCriticalCommunications: vi.fn().mockResolvedValue([]),
      logCriticalCommunication: vi.fn().mockResolvedValue({ id: "comm-1" }),
    };

    renderPanel(service);

    expect(await screen.findByLabelText("Call outcome")).toHaveValue("");
    await user.type(screen.getByLabelText("Recipient"), "Provider on call");
    await user.type(
      screen.getByLabelText("Message"),
      "Positive blood culture called",
    );
    await user.click(screen.getByRole("button", { name: "Log communication" }));

    expect(
      await screen.findByText(
        "Choose a target and an outcome, and provide the recipient and message.",
      ),
    ).toBeInTheDocument();
    expect(service.logCriticalCommunication).not.toHaveBeenCalled();
  });

  it("ticks follow-up for a call without read-back and unticks it for a confirmed one", async () => {
    const user = userEvent.setup();
    const service = {
      getCriticalCommunications: vi.fn().mockResolvedValue([]),
    };

    renderPanel(service);

    const outcome = await screen.findByLabelText("Call outcome");
    const followUp = screen.getByLabelText("Follow-up needed");
    await user.selectOptions(outcome, "Confirmed (read back)");
    expect(followUp).not.toBeChecked();
    await user.selectOptions(outcome, "Unable to reach");
    expect(followUp).toBeChecked();
    await user.selectOptions(outcome, "Confirmed (read back)");
    expect(followUp).not.toBeChecked();
    await user.selectOptions(outcome, "Reached, no read-back");
    expect(followUp).toBeChecked();
    await user.click(followUp);
    expect(followUp).not.toBeChecked();
  });

  it("shows each logged record's call outcome", async () => {
    const service = {
      getCriticalCommunications: vi.fn().mockResolvedValue([
        {
          id: "comm-1",
          recipient: "Provider on call",
          message: "Positive blood culture called",
          targetType: "CASE",
          communicationMethod: "PHONE",
          outcome: "REACHED_NO_READBACK",
          acknowledgementStatus: "OPEN",
        },
      ]),
    };

    renderPanel(service, { isReadOnly: true });

    expect(
      await screen.findByText("Reached, no read-back", { exact: false }),
    ).toBeInTheDocument();
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
    await user.selectOptions(
      screen.getByLabelText("Call outcome"),
      "Confirmed (read back)",
    );
    await user.type(screen.getByLabelText("Message"), "Critical isolate");
    await user.click(screen.getByRole("button", { name: "Log communication" }));

    await waitFor(() =>
      expect(service.logCriticalCommunication).toHaveBeenCalledWith("case-1", {
        targetType: "ISOLATE",
        targetId: "isolate-1",
        recipient: "Provider on call",
        recipientContact: "",
        communicationMethod: "PHONE",
        outcome: "CONFIRMED",
        message: "Critical isolate",
        followUpNeeded: false,
      }),
    );
  });
});
