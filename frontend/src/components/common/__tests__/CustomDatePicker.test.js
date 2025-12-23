import React from "react";
import { render } from "@testing-library/react";
import CustomDatePicker from "../CustomDatePicker";
import { ConfigurationContext } from "../../layout/Layout";

let latestDatePickerProps;

jest.mock("@carbon/react", () => {
  const ReactActual = jest.requireActual("react");
  return {
    DatePicker: ({ children, ...props }) => {
      latestDatePickerProps = props;
      return (
        <div data-testid="date-picker">
          {typeof children === "function"
            ? children({})
            : ReactActual.cloneElement(children, {})}
        </div>
      );
    },
    DatePickerInput: (props) => (
      <input data-testid="date-picker-input" {...props} />
    ),
  };
});

const getWrapper = (ui, config) => (
  <ConfigurationContext.Provider
    value={{ configurationProperties: config, reloadConfiguration: jest.fn() }}
  >
    {ui}
  </ConfigurationContext.Provider>
);

const renderWithConfig = (ui, config) => {
  // initial render
  const utils = render(getWrapper(ui, config));

  return {
    ...utils,
    rerenderWithConfig: (newUi, newConfig) =>
      utils.rerender(getWrapper(newUi, newConfig)),
  };
};

describe("CustomDatePicker", () => {
  const baseProps = {
    id: "test-date",
    labelText: "Test Date",
    onChange: jest.fn(),
  };

  beforeEach(() => {
    jest.useFakeTimers();
    jest.setSystemTime(new Date("2024-05-10T00:00:00Z"));
    baseProps.onChange.mockClear();
    latestDatePickerProps = null;
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  it("uses locale-specific placeholder text", () => {
    const { getByTestId, rerenderWithConfig } = renderWithConfig(
      <CustomDatePicker {...baseProps} />,
      { DEFAULT_DATE_LOCALE: "en-US" },
    );

    const input = getByTestId("date-picker-input");
    expect(input).toHaveProperty("placeholder", "MM/dd/yyyy");

    // rerender with a different locale
    rerenderWithConfig(<CustomDatePicker {...baseProps} />, {
      DEFAULT_DATE_LOCALE: "fr-FR",
    });

    expect(input).toHaveProperty("placeholder", "dd/MM/yyyy");
  });

  it("formats selected dates and calls onChange", () => {
    renderWithConfig(<CustomDatePicker {...baseProps} />, {
      DEFAULT_DATE_LOCALE: "en-US",
    });

    latestDatePickerProps.onChange(["2024-05-01"]);

    expect(baseProps.onChange).toHaveBeenCalledWith("05/01/2024");
  });

  it("exposes parseDate that understands compact input", () => {
    renderWithConfig(<CustomDatePicker {...baseProps} />, {
      DEFAULT_DATE_LOCALE: "en-US",
    });

    const parsed = latestDatePickerProps.parseDate("05102024");
    expect(parsed).toBeInstanceOf(Date);
    expect(parsed.getMonth()).toBe(4);
    expect(parsed.getDate()).toBe(10);
    expect(parsed.getFullYear()).toBe(2024);
  });

  it("sets min and max dates when restricted", () => {
    renderWithConfig(
      <CustomDatePicker {...baseProps} disallowPastDate disallowFutureDate />,
      {
        DEFAULT_DATE_LOCALE: "en-US",
      },
    );

    expect(latestDatePickerProps.maxDate).toBe("05/10/2024");
    expect(latestDatePickerProps.minDate).toBe("05/10/2024");
  });

  it("returns false for invalid or non-compliant date inputs", () => {
    renderWithConfig(<CustomDatePicker {...baseProps} />, {
      DEFAULT_DATE_LOCALE: "en-US",
    });

    const parseDate = latestDatePickerProps.parseDate;

    // 1. Full date string, but not compliant with locale (e.g., expecting MM/dd/yyyy)
    const invalidLocaleDate = "31/12/2024"; // day/month/year, invalid for en-US
    expect(parseDate(invalidLocaleDate)).toBe(false);

    // 2. Invalid compact value
    const invalidCompactDate = "071122222"; // too many digits
    expect(parseDate(invalidCompactDate)).toBe(false);

    // 3. Completely invalid string
    const garbageInput = "not-a-date";
    expect(parseDate(garbageInput)).toBe(false);

    // 4. Empty string should also return false
    expect(parseDate("")).toBe(false);

    // 5. Null/undefined input
    expect(parseDate(null)).toBe(false);
    expect(parseDate(undefined)).toBe(false);
  });
});
