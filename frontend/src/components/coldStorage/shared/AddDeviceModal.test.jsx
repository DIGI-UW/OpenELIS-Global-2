import React from "react";
import { vi } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";
import AddDeviceModal from "./AddDeviceModal";

const LOCATIONS = [{ id: 7, name: "Cold Room" }];

const renderModal = (onSubmit, editingDevice = null) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <AddDeviceModal
        isOpen={true}
        onClose={vi.fn()}
        onSubmit={onSubmit}
        locations={LOCATIONS}
        editingDevice={editingDevice}
      />
    </IntlProvider>,
  );

const fillRequiredFields = () => {
  fireEvent.change(screen.getByLabelText("Device Name *"), {
    target: { value: "Ultra-low 1" },
  });
  fireEvent.change(screen.getByLabelText("Room/Facility *"), {
    target: { value: "7" },
  });
  fireEvent.change(screen.getByLabelText("IP Address/Host *"), {
    target: { value: "10.0.0.9" },
  });
};

const submit = () =>
  fireEvent.click(screen.getByRole("button", { name: /Create|Update/ }));

/**
 * Humidity is an optional second Modbus register. Sending 0 for a device that
 * has no humidity probe makes the backend read register 0 twice and publish the
 * temperature register's raw contents as a humidity percentage, which the
 * dashboard, the trends chart and the FREEZER_HUMIDITY alert all then treat as
 * real.
 */
describe("AddDeviceModal humidity register", () => {
  it("leaves humidityRegister unset on a new device", () => {
    const onSubmit = vi.fn();
    renderModal(onSubmit);

    expect(screen.getByLabelText("Humidity Register")).toHaveValue(null);

    fillRequiredFields();
    submit();

    expect(onSubmit).toHaveBeenCalledTimes(1);
    expect(onSubmit.mock.calls[0][0].humidityRegister).toBeNull();
  });

  it("leaves humidityRegister unset when editing a device whose JSON omits it", () => {
    const onSubmit = vi.fn();
    // Shaped as GET /rest/coldstorage/devices returns it: Include.NON_NULL drops
    // a null humidityRegister, so the spread over INITIAL_FORM_DATA cannot
    // restore it and the default is what ships.
    renderModal(onSubmit, {
      id: 3,
      name: "Ultra-low 1",
      roomId: "7",
      protocol: "TCP",
      host: "10.0.0.9",
      temperatureRegister: 0,
      temperatureScale: 0.1,
      temperatureOffset: -80.0,
    });

    submit();

    expect(onSubmit).toHaveBeenCalledTimes(1);
    expect(onSubmit.mock.calls[0][0].humidityRegister).toBeNull();
  });

  it("still sends a humidity register that was typed in", () => {
    const onSubmit = vi.fn();
    renderModal(onSubmit);

    fillRequiredFields();
    fireEvent.change(screen.getByLabelText("Humidity Register"), {
      target: { value: "4" },
    });
    submit();

    expect(onSubmit.mock.calls[0][0].humidityRegister).toBe(4);
  });

  it("unsets a humidity register that is cleared again", () => {
    const onSubmit = vi.fn();
    renderModal(onSubmit);

    fillRequiredFields();
    const field = screen.getByLabelText("Humidity Register");
    fireEvent.change(field, { target: { value: "4" } });
    fireEvent.change(field, { target: { value: "" } });
    submit();

    expect(onSubmit.mock.calls[0][0].humidityRegister).toBeNull();
  });

  it("unsets a second temperature probe register that is cleared again", () => {
    const onSubmit = vi.fn();
    renderModal(onSubmit);

    fillRequiredFields();
    const field = screen.getByLabelText(
      messages["coldStorage.device.secondProbe.register"],
    );
    fireEvent.change(field, { target: { value: "2" } });
    fireEvent.change(field, { target: { value: "" } });
    submit();

    expect(onSubmit.mock.calls[0][0].temperatureRegister2).toBeNull();
  });
});
