import React from "react";
import { render, screen } from "@testing-library/react";
import ResponsiveDataTableToolbar, {
  ResponsiveBatchActionLabel,
} from "./ResponsiveDataTableToolbar";

describe("ResponsiveDataTableToolbar", () => {
  it("marks the Carbon toolbar only while batch actions are active", () => {
    const { container, rerender } = render(
      <ResponsiveDataTableToolbar aria-label="Phrase actions">
        <span>Toolbar content</span>
      </ResponsiveDataTableToolbar>,
    );

    expect(screen.getByRole("group", { name: "Phrase actions" })).toBeVisible();
    expect(
      container.querySelector(".oe-responsive-data-table-toolbar--batch"),
    ).not.toBeInTheDocument();

    rerender(
      <ResponsiveDataTableToolbar batchActive aria-label="Phrase actions">
        <ResponsiveBatchActionLabel>Activate</ResponsiveBatchActionLabel>
      </ResponsiveDataTableToolbar>,
    );

    expect(
      container.querySelector(".oe-responsive-data-table-toolbar--batch"),
    ).toBeInTheDocument();
    expect(screen.getByText("Activate")).toBeInTheDocument();
  });
});
