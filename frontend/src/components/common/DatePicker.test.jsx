import React from "react";
import { act, fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { DatePickerInput } from "@carbon/react";
import messages from "../../languages/en.json";
import { DatePicker } from "./DatePicker";

const renderPicker = (props = {}) => {
  const onChange = vi.fn();
  render(
    <IntlProvider locale="en" messages={messages}>
      <DatePicker
        datePickerType="single"
        dateFormat="Y-m-d"
        onChange={onChange}
        {...props}
      >
        <DatePickerInput id="when" labelText="When" placeholder="yyyy-mm-dd" />
      </DatePicker>
      <button type="button">Elsewhere</button>
    </IntlProvider>,
  );
  return onChange;
};

const typeAndLeave = (text) => {
  const input = screen.getByLabelText("When");
  act(() => input.focus());
  fireEvent.click(input);
  fireEvent.input(input, { target: { value: text } });
  const elsewhere = screen.getByRole("button", { name: "Elsewhere" });
  act(() => {
    fireEvent.touchStart(elsewhere);
    fireEvent.mouseDown(elsewhere);
    elsewhere.focus();
  });
  return input;
};

describe("DatePicker (OGC-1635)", () => {
  it("sends a typed date in the page's own format through onChange", () => {
    const onChange = renderPicker();
    typeAndLeave("2026-03-09");
    const [dates] = onChange.mock.calls.at(-1);
    expect(dates[0].getFullYear()).toBe(2026);
    expect(dates[0].getMonth()).toBe(2);
    expect(dates[0].getDate()).toBe(9);
  });

  it("marks text that is not a date and keeps it in the field", () => {
    const onChange = renderPicker();
    const input = typeAndLeave("2026-13-45x");
    expect(input).toHaveValue("2026-13-45x");
    expect(input).toHaveAttribute("data-invalid");
    expect(onChange.mock.calls.every(([dates]) => dates.length === 0)).toBe(
      true,
    );
  });

  it("sends a date typed into a field that had focus from the start", () => {
    const onChange = vi.fn();
    render(
      <IntlProvider locale="en" messages={messages}>
        <DatePicker
          datePickerType="single"
          dateFormat="Y-m-d"
          onChange={onChange}
        >
          <DatePickerInput
            id="first"
            labelText="First"
            placeholder="yyyy-mm-dd"
            autoFocus
          />
        </DatePicker>
        <button type="button">Elsewhere</button>
      </IntlProvider>,
    );
    const input = screen.getByLabelText("First");
    expect(input).toHaveFocus();
    fireEvent.mouseDown(input);
    fireEvent.click(input);
    fireEvent.input(input, { target: { value: "2026-12-23" } });
    const elsewhere = screen.getByRole("button", { name: "Elsewhere" });
    act(() => {
      fireEvent.touchStart(elsewhere);
      fireEvent.mouseDown(elsewhere);
      elsewhere.focus();
    });
    const [dates] = onChange.mock.calls.at(-1) || [[]];
    expect(dates[0]?.getDate()).toBe(23);
  });

  it("keeps the page's own invalid state and message", () => {
    render(
      <IntlProvider locale="en" messages={messages}>
        <DatePicker datePickerType="single" dateFormat="Y-m-d" invalid>
          <DatePickerInput
            id="required"
            labelText="Required"
            invalidText="A date is required"
          />
        </DatePicker>
      </IntlProvider>,
    );
    expect(screen.getByText("A date is required")).toBeInTheDocument();
  });
});
