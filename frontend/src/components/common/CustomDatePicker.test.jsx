import React from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import "@testing-library/jest-dom";
import CustomDatePicker from "./CustomDatePicker";
import { ConfigurationContext } from "../layout/Layout";

const renderWithConfig = (props, locale = "en-US") =>
  render(
    <ConfigurationContext.Provider
      value={{ configurationProperties: { DEFAULT_DATE_LOCALE: locale } }}
    >
      <CustomDatePicker {...props} />
    </ConfigurationContext.Provider>,
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

    // Initial mount fires onChange("") via the useEffect on currentDate. Ignore
    // that and only count post-mount typing.
    onChange.mockClear();

    await user.type(input, "01/15/1990");

    expect(
      onChange,
      "fully-typed valid date must propagate to the parent",
    ).toHaveBeenCalledWith("01/15/1990");
  });

  test("clearing a fully-typed date calls onChange with empty string", async () => {
    const onChange = vi.fn();
    const user = userEvent.setup();

    renderWithConfig({ id: "dob", value: "", onChange });
    const input = findInput();

    await user.type(input, "01/15/1990");
    onChange.mockClear();

    await user.clear(input);

    expect(
      onChange,
      "manual clear of a previously-valid date must reach the parent so " +
        "Formik does not keep submitting the stale value",
    ).toHaveBeenCalledWith("");
  });
});
