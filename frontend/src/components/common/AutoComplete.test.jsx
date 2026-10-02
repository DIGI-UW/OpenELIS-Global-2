/**
 * A list that arrives after the user has started typing must still offer the
 * matching entries (the Add Order site search showed "No suggestions
 * available." for good when the sites loaded a moment late).
 */
import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import AutoComplete from "./AutoComplete";

const sites = [
  { id: "1", value: "CAMES" },
  { id: "2", value: "Kampala Clinic" },
];

const renderSearch = (suggestions) => (
  <AutoComplete
    id="siteName"
    label="Search Site Name"
    suggestions={suggestions}
    onSelect={() => {}}
  />
);

describe("AutoComplete", () => {
  it("offers matches from a list that arrives after typing", () => {
    const { rerender } = render(renderSearch([]));
    fireEvent.change(screen.getByLabelText("Search Site Name"), {
      target: { value: "cam" },
    });
    expect(screen.queryByText("CAMES")).not.toBeInTheDocument();

    rerender(renderSearch(sites));

    expect(screen.getByText("CAMES")).toBeInTheDocument();
    expect(screen.queryByText("Kampala Clinic")).not.toBeInTheDocument();
  });

  it("still filters as the user types once the list is there", () => {
    render(renderSearch(sites));
    fireEvent.change(screen.getByLabelText("Search Site Name"), {
      target: { value: "kam" },
    });

    expect(screen.getByText("Kampala Clinic")).toBeInTheDocument();
    expect(screen.queryByText("CAMES")).not.toBeInTheDocument();
  });
});
