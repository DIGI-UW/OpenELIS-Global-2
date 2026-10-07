import React from "react";
import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { describe, it, expect } from "vitest";
import CaseViewLayout from "./CaseViewLayout";

const renderLayout = (props) => render(<CaseViewLayout {...props} />);

const columnOf = (testId) => screen.getByTestId(testId).parentElement;

describe("CaseViewLayout", () => {
  it("renders the rail, the centre content, the summary and the action bar together", () => {
    renderLayout({
      rail: <div data-testid="rail">rail</div>,
      summary: <div data-testid="summary">summary</div>,
      actionBar: <div data-testid="action-bar">action bar</div>,
      children: <div data-testid="centre">centre</div>,
    });

    expect(screen.getByTestId("rail")).toBeInTheDocument();
    expect(screen.getByTestId("centre")).toBeInTheDocument();
    expect(screen.getByTestId("summary")).toBeInTheDocument();
    expect(screen.getByTestId("action-bar")).toBeInTheDocument();

    // This is the layout the design actually calls for: rail and summary
    // both present, centre taking what is left. Pinning the spans here is
    // what would catch a change like clamping the centre span to a minimum,
    // which would leave every other test in this file passing while this
    // row silently overflowed past 16 columns.
    expect(columnOf("rail").className).toContain("cds--lg:col-span-3");
    expect(columnOf("centre").className).toContain("cds--lg:col-span-9");
    expect(columnOf("summary").className).toContain("cds--lg:col-span-4");
  });

  it("renders neither the rail nor the summary when both are absent, and the centre content takes the full width", () => {
    renderLayout({
      children: <div data-testid="centre">centre</div>,
    });

    expect(screen.queryByTestId("rail")).not.toBeInTheDocument();
    expect(screen.queryByTestId("summary")).not.toBeInTheDocument();
    expect(screen.getByTestId("centre")).toBeInTheDocument();
    expect(columnOf("centre").className).toContain("cds--lg:col-span-16");
  });

  it("computes the centre span as 13 when only a rail is given, not one of a fixed set of layouts", () => {
    renderLayout({
      rail: <div data-testid="rail">rail</div>,
      children: <div data-testid="centre">centre</div>,
    });

    expect(columnOf("rail").className).toContain("cds--lg:col-span-3");
    expect(columnOf("centre").className).toContain("cds--lg:col-span-13");
  });

  it("computes the centre span as 12 when only a summary is given, not one of a fixed set of layouts", () => {
    renderLayout({
      summary: <div data-testid="summary">summary</div>,
      children: <div data-testid="centre">centre</div>,
    });

    expect(columnOf("summary").className).toContain("cds--lg:col-span-4");
    expect(columnOf("centre").className).toContain("cds--lg:col-span-12");
  });

  it("renders no extra column when the action bar is absent", () => {
    const { container } = renderLayout({
      children: <div data-testid="centre">centre</div>,
    });

    expect(screen.queryByTestId("action-bar")).not.toBeInTheDocument();
    const columns = container.querySelectorAll(".cds--css-grid-column");
    expect(columns).toHaveLength(1);
  });

  it("stacks every present column to full width on md and sm so the page never scrolls horizontally", () => {
    renderLayout({
      rail: <div data-testid="rail">rail</div>,
      summary: <div data-testid="summary">summary</div>,
      actionBar: <div data-testid="action-bar">action bar</div>,
      children: <div data-testid="centre">centre</div>,
    });

    for (const testId of ["rail", "centre", "summary", "action-bar"]) {
      const className = columnOf(testId).className;
      expect(className).toContain("cds--md:col-span-8");
      expect(className).toContain("cds--sm:col-span-4");
    }
  });
});
