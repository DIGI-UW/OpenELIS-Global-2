/**
 * SerialConfiguration Component Tests
 */

import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { waitFor } from "@testing-library/dom";
import "@testing-library/jest-dom/extend-expect";
import { IntlProvider } from "react-intl";
import SerialConfiguration from "../SerialConfiguration";
import * as serialService from "../../../../services/serialService";

// Mock the serial service
jest.mock("../../../../services/serialService");

const messages = {
  "serial.config.create.title": "Create Serial Port Configuration",
  "serial.config.edit.title": "Edit Serial Port Configuration",
  "serial.config.portName.label": "Port Name",
  "serial.config.baudRate.label": "Baud Rate",
  "serial.config.dataBits.label": "Data Bits",
  "serial.config.stopBits.label": "Stop Bits",
  "serial.config.parity.label": "Parity",
  "serial.config.parity.none": "None",
  "serial.config.parity.even": "Even",
  "serial.config.parity.odd": "Odd",
  "serial.config.parity.mark": "Mark",
  "serial.config.parity.space": "Space",
  "serial.config.flowControl.label": "Flow Control",
  "serial.config.flowControl.none": "None",
  "serial.config.active.label": "Active",
  "serial.config.status.label": "Status",
  "serial.config.connect.button": "Connect",
  "serial.config.disconnect.button": "Disconnect",
  "button.cancel": "Cancel",
  "button.save": "Save",
  "button.saving": "Saving...",
  "serial.config.validation.analyzerId.required": "Analyzer ID is required",
  "serial.config.validation.portName.required": "Port name is required",
  "serial.config.form.legend": "Serial Port Settings",
};

const IntlWrapper = ({ children }) => (
  <IntlProvider locale="en" messages={messages}>
    {children}
  </IntlProvider>
);

describe("SerialConfiguration", () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it("renders create form when no configuration provided", () => {
    render(
      <IntlWrapper>
        <SerialConfiguration analyzerId={1} open={true} onClose={() => {}} />
      </IntlWrapper>,
    );

    expect(
      screen.getByText("Create Serial Port Configuration"),
    ).toBeInTheDocument();
    expect(screen.getByLabelText("Port Name")).toBeInTheDocument();
  });

  it("renders edit form when configuration provided", () => {
    const config = {
      id: "CONFIG-001",
      analyzerId: 1,
      portName: "/dev/ttyUSB0",
      baudRate: 9600,
      dataBits: 8,
      stopBits: "ONE",
      parity: "NONE",
      flowControl: "NONE",
      active: true,
    };

    render(
      <IntlWrapper>
        <SerialConfiguration
          configuration={config}
          open={true}
          onClose={() => {}}
        />
      </IntlWrapper>,
    );

    expect(
      screen.getByText("Edit Serial Port Configuration"),
    ).toBeInTheDocument();
    expect(screen.getByDisplayValue("/dev/ttyUSB0")).toBeInTheDocument();
  });

  it("validates required fields", async () => {
    const onSave = jest.fn();
    render(
      <IntlWrapper>
        <SerialConfiguration
          analyzerId={1}
          open={true}
          onClose={() => {}}
          onSave={onSave}
        />
      </IntlWrapper>,
    );

    const saveButton = screen.getByText("Save");
    fireEvent.click(saveButton);

    await waitFor(() => {
      // Form validation should prevent save
      expect(onSave).not.toHaveBeenCalled();
    });
  });

  it("calls createSerialPortConfiguration when saving new configuration", async () => {
    serialService.createSerialPortConfiguration.mockImplementation(
      (data, callback) => {
        callback({ id: "NEW-CONFIG" });
      },
    );

    const onSave = jest.fn();
    render(
      <IntlWrapper>
        <SerialConfiguration
          analyzerId={1}
          open={true}
          onClose={() => {}}
          onSave={onSave}
        />
      </IntlWrapper>,
    );

    // Fill form
    fireEvent.change(screen.getByLabelText("Port Name"), {
      target: { value: "/dev/ttyUSB0" },
    });

    const saveButton = screen.getByText("Save");
    fireEvent.click(saveButton);

    await waitFor(() => {
      expect(serialService.createSerialPortConfiguration).toHaveBeenCalled();
    });
  });
});
