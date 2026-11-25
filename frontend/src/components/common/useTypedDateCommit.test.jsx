import React from "react";
import { act, fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { DatePicker, DatePickerInput } from "@carbon/react";
import messages from "../../languages/en.json";
import useSiteDateFormat from "./useSiteDateFormat";
import useTypedDateCommit from "./useTypedDateCommit";

function Picker({ onChange, range = false, maxDate }) {
  const siteDate = useSiteDateFormat();
  const typedDate = useTypedDateCommit();
  return (
    <>
      <DatePicker
        dateFormat={siteDate.dateFormat}
        parseDate={siteDate.parseDate}
        datePickerType={range ? "range" : "single"}
        maxDate={maxDate}
        {...typedDate.picker("when")}
        onChange={onChange}
      >
        <DatePickerInput
          id="when-from"
          labelText="From"
          placeholder={siteDate.placeholder}
          {...typedDate.input("when")}
        />
        {range && (
          <DatePickerInput
            id="when-to"
            labelText="To"
            placeholder={siteDate.placeholder}
            {...typedDate.input("when")}
          />
        )}
      </DatePicker>
      <button type="button">Elsewhere</button>
    </>
  );
}

const renderPicker = (props) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <Picker {...props} />
    </IntlProvider>,
  );

const type = (input, text) => {
  act(() => input.focus());
  fireEvent.click(input);
  fireEvent.input(input, { target: { value: text } });
};

const clickElsewhere = () => {
  const target = screen.getByRole("button", { name: "Elsewhere" });
  act(() => {
    fireEvent.touchStart(target);
    fireEvent.mouseDown(target);
    target.focus();
  });
};

const isoDays = (onChange) =>
  onChange.mock.calls
    .at(-1)[0]
    .map((date) =>
      [
        date.getFullYear(),
        String(date.getMonth() + 1).padStart(2, "0"),
        String(date.getDate()).padStart(2, "0"),
      ].join("-"),
    );

describe("useTypedDateCommit", () => {
  it("sends a typed date through onChange when the user clicks elsewhere", () => {
    const onChange = vi.fn();
    renderPicker({ onChange });

    type(screen.getByLabelText("From"), "03/09/2026");
    clickElsewhere();

    expect(isoDays(onChange)).toEqual(["2026-03-09"]);
    expect(screen.getByLabelText("From")).toHaveValue("03/09/2026");
  });

  it("keeps text that is not a date, marks it, and sends no date", () => {
    const onChange = vi.fn();
    renderPicker({ onChange });

    type(screen.getByLabelText("From"), "03/09/26");
    clickElsewhere();

    expect(onChange.mock.calls.every(([dates]) => dates.length === 0)).toBe(
      true,
    );
    expect(screen.getByLabelText("From")).toHaveValue("03/09/26");
    expect(screen.getByLabelText("From")).toHaveAttribute("data-invalid");
    expect(
      screen.getByText("Enter a valid date as mm/dd/yyyy."),
    ).toBeInTheDocument();
  });

  it("clears the mark and sends the date once the text is corrected", () => {
    const onChange = vi.fn();
    renderPicker({ onChange });
    const input = screen.getByLabelText("From");

    type(input, "3/9");
    clickElsewhere();
    expect(input).toHaveAttribute("data-invalid");

    type(input, "03/09/2026");
    clickElsewhere();

    expect(isoDays(onChange)).toEqual(["2026-03-09"]);
    expect(input).not.toHaveAttribute("data-invalid");
  });

  it("refuses a typed date past the picker's latest date", () => {
    const onChange = vi.fn();
    renderPicker({ onChange, maxDate: new Date(2026, 0, 31).getTime() });

    type(screen.getByLabelText("From"), "03/09/2026");
    clickElsewhere();

    expect(onChange.mock.calls.every(([dates]) => dates.length === 0)).toBe(
      true,
    );
    expect(screen.getByLabelText("From")).toHaveValue("03/09/2026");
    expect(screen.getByLabelText("From")).toHaveAttribute("data-invalid");
    expect(
      screen.getByText("The date cannot be in the future."),
    ).toBeInTheDocument();
  });

  it("lets the user clear a refused date", () => {
    const onChange = vi.fn();
    renderPicker({ onChange });
    const input = screen.getByLabelText("From");

    type(input, "3/9");
    clickElsewhere();
    type(input, "");
    clickElsewhere();

    expect(input).toHaveValue("");
    expect(input).not.toHaveAttribute("data-invalid");
  });

  it("sends both ends of a typed range", () => {
    const onChange = vi.fn();
    renderPicker({ onChange, range: true });

    type(screen.getByLabelText("From"), "03/01/2026");
    type(screen.getByLabelText("To"), "03/09/2026");
    clickElsewhere();

    expect(isoDays(onChange)).toEqual(["2026-03-01", "2026-03-09"]);
  });

  it("refuses bad start text on a committed range without breaking the range", () => {
    const onChange = vi.fn();
    const errors = [];
    const onError = (event) => {
      errors.push(event.message);
      event.preventDefault();
    };
    window.addEventListener("error", onError);
    renderPicker({ onChange, range: true });
    const from = screen.getByLabelText("From");

    type(from, "03/01/2026");
    type(screen.getByLabelText("To"), "03/09/2026");
    clickElsewhere();
    type(from, "1/1/20");
    clickElsewhere();
    window.removeEventListener("error", onError);

    expect(errors).toEqual([]);
    expect(from).toHaveValue("1/1/20");
    expect(from).toHaveAttribute("data-invalid");
    expect(onChange.mock.calls.at(-1)[0]).toEqual([]);
  });

  it("treats Enter on text that is not a date like a click elsewhere", () => {
    const onChange = vi.fn();
    renderPicker({ onChange });
    const input = screen.getByLabelText("From");

    type(input, "11/03/26");
    fireEvent.keyDown(input, { key: "Enter", keyCode: 13 });

    expect(input).toHaveValue("11/03/26");
    expect(input).toHaveAttribute("data-invalid");
    expect(onChange.mock.calls.every(([dates]) => dates.length === 0)).toBe(
      true,
    );
  });

  it("keeps the end of a range in view while its start is refused", () => {
    const onChange = vi.fn();
    renderPicker({ onChange, range: true });
    const from = screen.getByLabelText("From");
    const to = screen.getByLabelText("To");

    type(from, "03/01/2026");
    type(to, "03/09/2026");
    clickElsewhere();
    type(from, "1/1/20");
    clickElsewhere();

    expect(to).toHaveValue("03/09/2026");
    type(from, "03/02/2026");
    clickElsewhere();
    expect(isoDays(onChange)).toEqual(["2026-03-02", "2026-03-09"]);
  });

  it("leaves a date picked from the calendar alone", () => {
    const onChange = vi.fn();
    renderPicker({ onChange });
    const input = screen.getByLabelText("From");

    act(() => input.focus());
    fireEvent.click(input);
    act(() => input._flatpickr.setDate(new Date(2026, 2, 9), true));
    const calls = onChange.mock.calls.length;
    clickElsewhere();

    expect(onChange.mock.calls.length).toBe(calls);
    expect(isoDays(onChange)).toEqual(["2026-03-09"]);
  });
});
