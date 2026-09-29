import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { vi } from "vitest";
import CustomDatePicker from "./CustomDatePicker";
import { ConfigurationContext } from "../layout/Layout";

const renderWithConfig = (props, locale = "en-US") =>
  render(
    <IntlProvider locale="en" messages={{}} onError={() => {}}>
      <ConfigurationContext.Provider
        value={{ configurationProperties: { DEFAULT_DATE_LOCALE: locale } }}
      >
        <CustomDatePicker {...props} />
      </ConfigurationContext.Provider>
    </IntlProvider>,
  );

const findInput = () => {
  const inputs = screen.getAllByRole("textbox");
  // Carbon's DatePickerInput is the visible text input.
  return inputs[inputs.length - 1];
};

describe("CustomDatePicker — controlled input contract", () => {
  test("typing a full date calls onChange with the typed value", async () => {
    const onChange = vi.fn();
    const user = userEvent.setup();

    renderWithConfig({ id: "dob", value: "", onChange });
    const input = findInput();

    expect(onChange).not.toHaveBeenCalled();

    await user.type(input, "01/15/1990");

    expect(onChange).toHaveBeenCalledWith("01/15/1990");
  });

  test("native input from Carbon's browser field propagates the typed date", () => {
    const onChange = vi.fn();

    renderWithConfig({ id: "collection-date", value: "", onChange }, "fr-FR");
    fireEvent.input(findInput(), { target: { value: "01/01/2026" } });

    expect(onChange).toHaveBeenCalledWith("01/01/2026");
  });

  test("controlled value synchronization does not emit a user change", () => {
    const onChange = vi.fn();

    renderWithConfig({ id: "dob", value: "01/15/1990", onChange });

    expect(findInput()).toHaveValue("01/15/1990");
    expect(onChange).not.toHaveBeenCalled();
  });

  test("clearing a fully-typed date calls onChange with empty string", async () => {
    const onChange = vi.fn();
    const user = userEvent.setup();

    renderWithConfig({ id: "dob", value: "", onChange });
    const input = findInput();

    await user.type(input, "01/15/1990");
    onChange.mockClear();

    await user.clear(input);

    expect(onChange).toHaveBeenCalledWith("");
  });
});

// OGC-1375: a typed future date of birth reached the form (negative ages, a
// silent 400) while the calendar blanked the box. It is refused in the field.
describe("CustomDatePicker — typed dates outside the allowed range", () => {
  const nextYear = new Date().getFullYear() + 1;
  const lastYear = new Date().getFullYear() - 1;

  test("a future date is refused with the caller's message when future dates are disallowed", () => {
    const onChange = vi.fn();
    renderWithConfig({
      id: "dob",
      value: "",
      onChange,
      disallowFutureDate: true,
      futureDateText: "Date of birth cannot be in the future.",
    });

    fireEvent.input(findInput(), { target: { value: `01/01/${nextYear}` } });

    expect(onChange).not.toHaveBeenCalledWith(`01/01/${nextYear}`);
    expect(onChange).toHaveBeenCalledWith("");
    expect(
      screen.getByText("Date of birth cannot be in the future."),
    ).toBeInTheDocument();
  });

  test("a past date is still accepted when only future dates are disallowed", () => {
    const onChange = vi.fn();
    renderWithConfig({
      id: "dob",
      value: "",
      onChange,
      disallowFutureDate: true,
      futureDateText: "Date of birth cannot be in the future.",
    });

    fireEvent.input(findInput(), { target: { value: `01/01/${lastYear}` } });

    expect(onChange).toHaveBeenCalledWith(`01/01/${lastYear}`);
    expect(
      screen.queryByText("Date of birth cannot be in the future."),
    ).toBeNull();
  });

  test("a future date is accepted where future dates are allowed", () => {
    const onChange = vi.fn();
    renderWithConfig({ id: "visit", value: "", onChange });

    fireEvent.input(findInput(), { target: { value: `01/01/${nextYear}` } });

    expect(onChange).toHaveBeenCalledWith(`01/01/${nextYear}`);
  });

  test("a past date is refused where past dates are disallowed", () => {
    const onChange = vi.fn();
    renderWithConfig({
      id: "required-by",
      value: "",
      onChange,
      disallowPastDate: true,
      pastDateText: "Pick today or a later date.",
    });

    fireEvent.input(findInput(), { target: { value: `01/01/${lastYear}` } });

    expect(onChange).toHaveBeenCalledWith("");
    expect(screen.getByText("Pick today or a later date.")).toBeInTheDocument();
  });
});
