import React from "react";
import { screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import QAPlaceholder from "../QAPlaceholder";
import { renderQa } from "../testUtils";

describe("QAPlaceholder", () => {
  test("reagent-qc future placeholder shows question, why, and design-doc link", () => {
    renderQa(<QAPlaceholder feature="reagent-qc" />);
    expect(
      screen.getByRole("heading", { name: "Reagent QC" }),
    ).toBeInTheDocument();
    expect(
      screen.getByText(
        "Has this reagent lot been verified before we use it on patients?",
      ),
    ).toBeInTheDocument();
    const docLink = screen.getByRole("link", {
      name: /Read design doc on GitHub/,
    });
    expect(docLink).toHaveAttribute(
      "href",
      "https://github.com/DIGI-UW/openelis-work/blob/main/designs/quality/batch-workplan-reagent-qc.md",
    );
  });

  test("manual-qc future placeholder links to its design doc", () => {
    renderQa(<QAPlaceholder feature="manual-qc" />);
    expect(
      screen.getByRole("heading", { name: "Analyzer Manual QC" }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("link", { name: /Read design doc on GitHub/ }),
    ).toHaveAttribute(
      "href",
      "https://github.com/DIGI-UW/openelis-work/blob/main/designs/quality/analyzer-manual-qc.md",
    );
  });
});
