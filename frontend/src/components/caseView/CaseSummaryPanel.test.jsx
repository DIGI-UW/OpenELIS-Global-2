import React from "react";
import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { Section } from "@carbon/react";
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

  // React renders nothing for a boolean, so before this a "report signed out"
  // style row showed its label beside an empty space for both answers. A
  // boolean is a recorded fact, not an absence, so neither value may fall
  // into the not-recorded branch.
  it("renders false as No and never as the not-recorded hint", () => {
    renderPanel({
      rows: [
        { id: "signedOut", labelKey: "pathology.label.report", value: false },
      ],
    });

    expect(screen.getByText(messages["label.no"])).toBeInTheDocument();
    expect(screen.queryByText(/not recorded/)).not.toBeInTheDocument();
  });

  it("renders true as Yes", () => {
    renderPanel({
      rows: [
        { id: "signedOut", labelKey: "pathology.label.report", value: true },
      ],
    });

    expect(screen.getByText(messages["label.yes"])).toBeInTheDocument();
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
  it("renders its title one heading level below the page around it", () => {
    // On its own the panel sits under the document's implicit level 1, so its
    // title is an h2; inside a screen that nests its own title two Sections
    // deep, the same title lands at h4. Neither number is written into the
    // panel, so a screen never has to know what the panel chose.
    renderPanel({ rows: [] });
    expect(
      screen.getByRole("heading", {
        name: messages["caseView.label.caseSummary"],
        level: 2,
      }),
    ).toBeInTheDocument();
  });

  it("follows the Section nesting of the screen that renders it", () => {
    render(
      <IntlProvider locale="en" messages={messages}>
        <Section>
          <Section>
            <CaseSummaryPanel rows={[]} />
          </Section>
        </Section>
      </IntlProvider>,
    );
    expect(
      screen.getByRole("heading", {
        name: messages["caseView.label.caseSummary"],
        level: 4,
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
  // A title attribute on a row is a hover and nothing else: it is not an
  // accessible name and a screen-reader user never reaches it, so the same
  // sentence is rendered in text that only they read.
  it("names what a counted row counted, for a pointer and for a screen reader alike", () => {
    renderPanel({
      rows: [
        {
          id: "requests",
          labelKey: "pathology.label.request",
          value: "2 open",
          title: "Open requests: Recut, Extra levels",
        },
      ],
    });

    const row = document.querySelector(".case-view__summary-row");

    expect(row).toHaveAttribute("title", "Open requests: Recut, Extra levels");
    // In a node of its own, and a hidden one: a row that simply printed the
    // sentence would say the same thing twice on screen and still satisfy an
    // assertion on the row's text.
    expect(row.querySelector(".cds--visually-hidden")).toHaveTextContent(
      "Open requests: Recut, Extra levels",
    );
  });
});
