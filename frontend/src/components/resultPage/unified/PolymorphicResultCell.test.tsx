import React from "react";
import { render, screen } from "@testing-library/react";
import PolymorphicResultCell, { worklistRowKey } from "./PolymorphicResultCell";

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
