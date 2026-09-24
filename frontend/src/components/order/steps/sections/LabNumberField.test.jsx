import React from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import messages from "../../../../languages/en.json";

const { getFromOpenElisServer } = vi.hoisted(() => ({
  getFromOpenElisServer: vi.fn(),
}));

vi.mock("../../../utils/Utils", () => ({
  getFromOpenElisServer,
}));

import LabNumberField from "./LabNumberField";

const renderField = (props = {}) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <LabNumberField value="" onLabNumberChange={vi.fn()} {...props} />
    </IntlProvider>,
  );

describe("LabNumberField", () => {
  beforeEach(() => {
    getFromOpenElisServer.mockReset();
  });

  // OGC-1201 AI: the affordance used to be a Carbon <Link> with no href, so it
  // took no keyboard focus — on the form's only required control.
  it("offers generation as a control the keyboard can reach", async () => {
    const user = userEvent.setup();
    renderField();

    const generate = screen.getByRole("button", {
      name: "Generate Lab Number",
    });
    await user.tab();
    await user.tab();

    expect(generate).toHaveFocus();
  });

  // OGC-1201 AH: the form used to load with its one required field empty and
  // no code path that filled it.
  it("asks the server for a lab number when a new order opens", () => {
    const onLabNumberChange = vi.fn();
    getFromOpenElisServer.mockImplementation((_url, callback) =>
      callback({ body: "DEV01260000000000001" }),
    );

    renderField({ autoGenerate: true, onLabNumberChange });

    expect(getFromOpenElisServer).toHaveBeenCalledWith(
      "/rest/SampleEntryGenerateScanProvider",
      expect.any(Function),
    );
    expect(onLabNumberChange).toHaveBeenCalledWith("DEV01260000000000001");
  });

  it("leaves an order that already has a lab number alone", () => {
    renderField({ autoGenerate: true, value: "DEV01260000000000009" });

    expect(getFromOpenElisServer).not.toHaveBeenCalled();
  });

  it("does not generate for an order the URL already addresses", () => {
    renderField({ autoGenerate: false });

    expect(getFromOpenElisServer).not.toHaveBeenCalled();
  });

  it("does not generate into a read-only order", () => {
    renderField({ autoGenerate: true, disabled: true });

    expect(getFromOpenElisServer).not.toHaveBeenCalled();
  });

  it("generates once per form, not once per render", () => {
    getFromOpenElisServer.mockImplementation(() => {});
    const { rerender } = renderField({ autoGenerate: true });

    rerender(
      <IntlProvider locale="en" messages={messages}>
        <LabNumberField value="" onLabNumberChange={vi.fn()} autoGenerate />
      </IntlProvider>,
    );

    expect(getFromOpenElisServer).toHaveBeenCalledTimes(1);
  });

  // OGC-1201 AJ: a Carbon <Link> renders neither disabled nor aria-disabled,
  // so it stayed clickable in flight and every click spent a lab number.
  it("refuses further clicks while a request is in flight", async () => {
    const user = userEvent.setup();
    getFromOpenElisServer.mockImplementation(() => {});
    renderField();

    const generate = screen.getByRole("button", {
      name: "Generate Lab Number",
    });
    await user.click(generate);

    expect(
      screen.getByRole("button", { name: "Generating..." }),
    ).toBeDisabled();
    expect(getFromOpenElisServer).toHaveBeenCalledTimes(1);
  });

  it("conveys that generation is unavailable on a read-only order", () => {
    renderField({ disabled: true });

    expect(
      screen.getByRole("button", { name: "Generate Lab Number" }),
    ).toBeDisabled();
  });

  it("reports a rejected lab number on the field", () => {
    renderField({ invalid: true, invalidText: "Lab number already in use" });

    expect(screen.getByRole("textbox", { name: /Lab Number/ })).toBeInvalid();
    expect(screen.getByText("Lab number already in use")).toBeInTheDocument();
  });
});
