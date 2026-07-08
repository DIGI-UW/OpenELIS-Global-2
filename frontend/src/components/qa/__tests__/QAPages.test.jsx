import React from "react";
import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import messages from "../../../languages/en.json";
import QAPlaceholder from "../QAPlaceholder";

// Rendering with the real en.json also fails loudly if a referenced i18n key
// is missing (react-intl falls back to the raw key, breaking text assertions).
const renderPage = (component) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <MemoryRouter>{component}</MemoryRouter>
    </IntlProvider>,
  );

describe("QAPlaceholder", () => {
  test("reagent-qc future placeholder shows question, why, and design-doc link", () => {
    renderPage(<QAPlaceholder feature="reagent-qc" />);
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
    renderPage(<QAPlaceholder feature="manual-qc" />);
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

  test("qi coming-soon placeholder renders without design-doc link", () => {
    renderPage(<QAPlaceholder feature="qi" />);
    expect(
      screen.getByRole("heading", { name: "Quality Indicators" }),
    ).toBeInTheDocument();
    expect(
      screen.getByText("This page is planned but not yet built."),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole("link", { name: /Read design doc on GitHub/ }),
    ).not.toBeInTheDocument();
  });
});
