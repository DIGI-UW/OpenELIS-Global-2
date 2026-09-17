import React, { useState } from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { IntlProvider } from "react-intl";
import CaseProtocolPanel from "../CaseProtocolPanel";
import messages from "../../../languages/en.json";

const renderPanel = (props) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <CaseProtocolPanel {...props} />
    </IntlProvider>,
  );

const activeOptions = [
  {
    id: "method-1",
    label: "Routine blood culture",
    active: true,
    current: false,
    mediaDefaults: "BAP + CHOC",
    incubationDefaults: "48 hours at 35 C",
    atmosphereDefaults: "aerobic + anaerobic",
  },
];

describe("CaseProtocolPanel", () => {
  it("sets an initially missing protocol through a reasoned inline action", async () => {
    const user = userEvent.setup();
    const service = {
      getCaseProtocolOptions: vi.fn().mockResolvedValue(activeOptions),
      changeCaseProtocol: vi.fn().mockResolvedValue({
        id: "case-1",
        cultureMethodId: "method-1",
      }),
    };
    const onChanged = vi.fn();

    const ControlledPanel = () => {
      const [open, setOpen] = useState(false);
      return (
        <CaseProtocolPanel
          caseId="case-1"
          currentMethodId=""
          open={open}
          service={service}
          onOpen={() => setOpen(true)}
          onClose={() => setOpen(false)}
          onChanged={onChanged}
        />
      );
    };

    render(
      <IntlProvider locale="en" messages={messages}>
        <ControlledPanel />
      </IntlProvider>,
    );

    expect(await screen.findByText("No protocol set")).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "Set protocol" }));
    const protocol = await screen.findByRole("combobox", {
      name: "Culture protocol",
    });
    const save = screen.getByRole("button", { name: "Save protocol" });
    expect(save).toBeDisabled();
    await user.selectOptions(protocol, "method-1");
    await user.type(
      screen.getByLabelText("Reason for protocol change"),
      "Growth requires alternate media",
    );
    expect(save).toBeEnabled();
    expect(screen.queryByText(/detach results/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/reclassif/i)).not.toBeInTheDocument();
    await user.click(save);

    expect(service.changeCaseProtocol).toHaveBeenCalledWith("case-1", {
      cultureMethodId: "method-1",
      reason: "Growth requires alternate media",
    });
    expect(onChanged).toHaveBeenCalledWith({
      id: "case-1",
      cultureMethodId: "method-1",
    });
  });

  it("shows the incumbent recipe and marks an inactive protocol", async () => {
    const service = {
      getCaseProtocolOptions: vi.fn().mockResolvedValue([
        {
          id: "method-old",
          label: "Legacy blood culture",
          active: false,
          current: true,
          mediaDefaults: "BAP",
          incubationDefaults: "5 days",
          atmosphereDefaults: "aerobic",
        },
        ...activeOptions,
      ]),
    };

    renderPanel({
      caseId: "case-1",
      currentMethodId: "method-old",
      open: false,
      service,
      onOpen: vi.fn(),
      onClose: vi.fn(),
      onChanged: vi.fn(),
    });

    expect(await screen.findByText("Legacy blood culture")).toBeInTheDocument();
    expect(screen.getByText("BAP - 5 days - aerobic")).toBeInTheDocument();
    expect(screen.getByText("Inactive")).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "Change protocol" }),
    ).toBeEnabled();
  });

  it("closes with Escape-free Carbon interaction and restores trigger focus", async () => {
    const user = userEvent.setup();
    const service = {
      getCaseProtocolOptions: vi.fn().mockResolvedValue(activeOptions),
    };

    const ControlledPanel = () => {
      const [open, setOpen] = useState(false);
      return (
        <CaseProtocolPanel
          caseId="case-1"
          currentMethodId=""
          open={open}
          service={service}
          onOpen={() => setOpen(true)}
          onClose={() => setOpen(false)}
          onChanged={vi.fn()}
        />
      );
    };

    render(
      <IntlProvider locale="en" messages={messages}>
        <ControlledPanel />
      </IntlProvider>,
    );

    const trigger = await screen.findByRole("button", {
      name: "Set protocol",
    });
    await user.click(trigger);
    await screen.findByRole("combobox", { name: "Culture protocol" });
    await user.click(screen.getByRole("button", { name: "Cancel" }));
    expect(trigger).toHaveFocus();
  });

  it("blocks protocol changes when the case is read-only", async () => {
    renderPanel({
      caseId: "case-1",
      currentMethodId: "method-1",
      open: false,
      readOnly: true,
      service: {
        getCaseProtocolOptions: vi
          .fn()
          .mockResolvedValue([{ ...activeOptions[0], current: true }]),
      },
      onOpen: vi.fn(),
      onClose: vi.fn(),
      onChanged: vi.fn(),
    });

    expect(
      await screen.findByRole("button", { name: "Change protocol" }),
    ).toBeDisabled();
  });

  it("keeps the action open when the protocol request cannot reach the server", async () => {
    const user = userEvent.setup();
    const onChanged = vi.fn();
    const onClose = vi.fn();
    renderPanel({
      caseId: "case-1",
      currentMethodId: "",
      open: true,
      service: {
        getCaseProtocolOptions: vi.fn().mockResolvedValue(activeOptions),
        changeCaseProtocol: vi.fn().mockResolvedValue({ status: 0 }),
      },
      onOpen: vi.fn(),
      onClose,
      onChanged,
    });

    await user.selectOptions(
      await screen.findByRole("combobox", { name: "Culture protocol" }),
      "method-1",
    );
    await user.type(
      screen.getByRole("textbox", { name: "Reason for protocol change" }),
      "Bench observation",
    );
    await user.click(screen.getByRole("button", { name: "Save protocol" }));

    expect(
      await screen.findByText("Protocol could not be changed"),
    ).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Save protocol" })).toBeEnabled();
    expect(onChanged).not.toHaveBeenCalled();
    expect(onClose).not.toHaveBeenCalled();
  });
});
