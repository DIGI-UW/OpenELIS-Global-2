import React from "react";
import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { describe, it, expect } from "vitest";
import messages from "../../languages/en.json";
import CaseSummaryPanel from "./CaseSummaryPanel";

const renderPanel = (props = {}) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <CaseSummaryPanel {...props} />
    </IntlProvider>,
  );

describe("CaseSummaryPanel", () => {
  it("renders rows in the order they were given", () => {
    renderPanel({
      rows: [
        { id: "stage", labelKey: "pathology.label.stage", value: "Grossing" },
        { id: "blocks", labelKey: "pathology.label.blocks", value: 4 },
        { id: "slides", labelKey: "pathology.label.slides", value: 9 },
      ],
    });

    const labels = screen
      .getAllByText(/^(Stage|Blocks|Slides)$/, {
        selector: ".case-view__summary-label",
      })
      .map((el) => el.textContent);

    expect(labels).toEqual(["Stage", "Blocks", "Slides"]);
  });

  it("renders the not-recorded message for a null value", () => {
    renderPanel({
      rows: [{ id: "stage", labelKey: "pathology.label.stage", value: null }],
    });

    expect(
      screen.getByText(messages["caseView.label.notRecorded"]),
    ).toBeInTheDocument();
  });

  it("renders the not-recorded message for an empty-string value", () => {
    renderPanel({
      rows: [{ id: "stage", labelKey: "pathology.label.stage", value: "" }],
    });

    expect(
      screen.getByText(messages["caseView.label.notRecorded"]),
    ).toBeInTheDocument();
  });

  // A zero count is a recorded fact, not an absence of one: a case with zero
  // blocks embedded is different from a case whose block count was never
  // computed. Rendering the em dash for 0 would quietly turn a real answer
  // into a claim that nothing is known, which is a lie about the data.
  it("renders a value of 0 as 0 and never as the not-recorded hint", () => {
    renderPanel({
      rows: [{ id: "blocks", labelKey: "pathology.label.blocks", value: 0 }],
    });

    expect(screen.getByText("0")).toBeInTheDocument();
    expect(screen.queryByText(/not recorded/)).not.toBeInTheDocument();
    expect(screen.queryByText(/—/)).not.toBeInTheDocument();
  });

  it("renders a React node value as given", () => {
    renderPanel({
      rows: [
        {
          id: "report",
          labelKey: "pathology.label.stage",
          value: <span data-testid="report-badge">Verified</span>,
        },
      ],
    });

    expect(screen.getByTestId("report-badge")).toHaveTextContent("Verified");
  });

  // The level is asserted and not only the text. A heading query that names
  // the text alone passes at any level, so the panel's place in the page's
  // heading order would be free to drift back down without a single test
  // turning red, and someone navigating the case view by heading would be
  // the first to find out.
  it("renders the case summary heading at level 2", () => {
    renderPanel({ rows: [] });

    expect(
      screen.getByRole("heading", {
        name: messages["caseView.label.caseSummary"],
        level: 2,
      }),
    ).toBeInTheDocument();
  });

  it("formats row labels through react-intl rather than a hardcoded literal", () => {
    renderPanel({
      rows: [{ id: "blocks", labelKey: "pathology.label.blocks", value: 2 }],
    });

    expect(
      screen.getByText(messages["pathology.label.blocks"], {
        selector: ".case-view__summary-label",
      }),
    ).toBeInTheDocument();
  });
});
