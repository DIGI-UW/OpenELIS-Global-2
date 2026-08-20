import React from "react";
import { render as rtlRender, screen } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import messages from "../../../languages/en.json";
import PolymorphicResultCell, {
  blocksSaveOnPrecision,
  enteredDecimalPlaces,
  exceedsConfiguredPrecision,
  precisionStep,
  worklistRowKey,
} from "./PolymorphicResultCell";

// The cell names its control for a screen reader, so it needs the message
// catalogue (OGC-1179).
const render = (ui: React.ReactElement) =>
  rtlRender(
    <IntlProvider locale="en" messages={messages}>
      {ui}
    </IntlProvider>,
  );

/**
 * FR-A1 — the main-row result cell renders a full-size control per result
 * type (the row is the primary entry surface; sizing rides the
 * .unifiedResultsValueCell styles asserted live via computed min-widths).
 */
const baseRow = {
  id: "r1",
  analysisId: "29",
  resultType: "N",
};

describe("PolymorphicResultCell", () => {
  it("numeric rows render a number input", () => {
    const { container } = render(
      <PolymorphicResultCell
        row={{ ...baseRow, resultType: "N", resultValue: "42" }}
        editable
        onValueChange={() => {}}
      />,
    );
    const input = container.querySelector('input[type="number"]');
    expect(input).not.toBeNull();
    expect(input).toHaveValue(42);
  });

  it("dictionary rows render a select with readable options", () => {
    const { container } = render(
      <PolymorphicResultCell
        row={{
          ...baseRow,
          resultType: "D",
          resultValue: "552",
          dictionaryResults: [
            { id: "552", value: "Positive" },
            { id: "553", value: "Negative" },
          ],
        }}
        editable
        onValueChange={() => {}}
      />,
    );
    expect(container.querySelector("select")).not.toBeNull();
    expect(screen.getByText("Positive")).toBeInTheDocument();
    expect(screen.getByText("Negative")).toBeInTheDocument();
  });

  it("free-text rows render a textarea", () => {
    const { container } = render(
      <PolymorphicResultCell
        row={{ ...baseRow, resultType: "A", resultValue: "clear yellow" }}
        editable
        onValueChange={() => {}}
      />,
    );
    expect(container.querySelector("textarea")).toHaveValue("clear yellow");
  });

  it("read-only rows show the display value, dictionary-resolved", () => {
    render(
      <PolymorphicResultCell
        row={{
          ...baseRow,
          resultType: "D",
          resultValue: "552",
          dictionaryResults: [{ id: "552", value: "Positive" }],
        }}
        editable={false}
        onValueChange={() => {}}
      />,
    );
    expect(screen.getByText("Positive")).toBeInTheDocument();
  });

  it("component rows have distinct widget identities per component", () => {
    expect(
      worklistRowKey({ analysisId: "29", testResultComponentId: "a" }),
    ).not.toBe(
      worklistRowKey({ analysisId: "29", testResultComponentId: "b" }),
    );
  });
});

/**
 * OGC-1179 #7 — the result cell is the worklist's primary control and carries
 * no visible label; the column header names it for a sighted reader, but a
 * screen-reader user tabbing the page landed on an unnamed combobox.
 */
describe("PolymorphicResultCell accessible names", () => {
  const named = (resultType: string, extra = {}) => (
    <PolymorphicResultCell
      row={{
        ...baseRow,
        resultType,
        testName: "COVID-19 PCR — N2 (Ct)",
        dictionaryResults: [{ id: "552", value: "Positive" }],
        ...extra,
      }}
      editable
      onValueChange={() => {}}
    />
  );

  it.each(["N", "D", "A", "R"])("names the %s control", (resultType) => {
    render(named(resultType));
    expect(
      screen.getByLabelText("Result for COVID-19 PCR — N2 (Ct)"),
    ).toBeInTheDocument();
  });
});

/**
 * OGC-1179 #1 — a value entered to more decimal places than the test reports
 * to is stored in full and shown rounded, so the record and the screen stop
 * agreeing and the next edit writes the rounded form over the stored one.
 */
describe("configured precision", () => {
  it("counts the decimal places entered", () => {
    expect(enteredDecimalPlaces("23")).toBe(0);
    expect(enteredDecimalPlaces("23.7")).toBe(1);
    expect(enteredDecimalPlaces("5.1234")).toBe(4);
  });

  it("flags a value finer than the test reports to", () => {
    expect(exceedsConfiguredPrecision("23.7", 0)).toBe(true);
    expect(exceedsConfiguredPrecision("23", 0)).toBe(false);
    expect(exceedsConfiguredPrecision("5.1234", 2)).toBe(true);
    expect(exceedsConfiguredPrecision("5.12", 2)).toBe(false);
    expect(exceedsConfiguredPrecision("5.1", 2)).toBe(false);
  });

  it("says nothing when the test declares no precision", () => {
    expect(exceedsConfiguredPrecision("23.7", undefined)).toBe(false);
    expect(exceedsConfiguredPrecision("23.7", -1)).toBe(false);
    expect(exceedsConfiguredPrecision("", 0)).toBe(false);
    expect(exceedsConfiguredPrecision("not a number", 0)).toBe(false);
  });

  it("offers a step matching the configured precision", () => {
    expect(precisionStep(0)).toBe("1");
    expect(precisionStep(1)).toBe("0.1");
    expect(precisionStep(2)).toBe("0.01");
    expect(precisionStep(undefined)).toBe("any");
  });

  it("marks the entry field invalid rather than accepting the divergence", () => {
    const { container } = render(
      <PolymorphicResultCell
        row={{
          ...baseRow,
          resultType: "N",
          resultValue: "23.7",
          rawResultValue: "22",
          significantDigits: 0,
          testName: "E (Ct)",
        }}
        editable
        onValueChange={() => {}}
      />,
    );
    const input = container.querySelector('input[type="number"]');
    expect(input).toHaveAttribute("step", "1");
    expect(input).toHaveAttribute("data-invalid");
  });

  it("leaves a value within the configured precision alone", () => {
    const { container } = render(
      <PolymorphicResultCell
        row={{
          ...baseRow,
          resultType: "N",
          resultValue: "5.1234",
          significantDigits: 4,
          testName: "RBC",
        }}
        editable
        onValueChange={() => {}}
      />,
    );
    const input = container.querySelector('input[type="number"]');
    expect(input).not.toHaveAttribute("data-invalid");
  });
});

/**
 * A value already stored at a finer precision than the test reports to is a
 * record that exists. Refusing to save its row would strand it — the reader
 * could not add a note to say so. What is refused is entering a new one.
 */
describe("what the precision guard refuses", () => {
  const numeric = (resultValue: string, rawResultValue: string) => ({
    ...baseRow,
    resultType: "N",
    significantDigits: 0,
    resultValue,
    rawResultValue,
  });

  it("refuses a newly typed out-of-precision value", () => {
    expect(blocksSaveOnPrecision(numeric("23.7", "22"))).toBe(true);
    expect(blocksSaveOnPrecision(numeric("23.7", ""))).toBe(true);
  });

  it("leaves a stored out-of-precision value saveable", () => {
    expect(blocksSaveOnPrecision(numeric("23.7", "23.7"))).toBe(false);
  });

  it("says nothing about a value within the configured precision", () => {
    expect(blocksSaveOnPrecision(numeric("24", "22"))).toBe(false);
  });

  it("says nothing about a non-numeric result", () => {
    expect(
      blocksSaveOnPrecision({
        resultType: "D",
        resultValue: "1578",
        rawResultValue: "1334",
        significantDigits: 0,
      }),
    ).toBe(false);
  });
});
