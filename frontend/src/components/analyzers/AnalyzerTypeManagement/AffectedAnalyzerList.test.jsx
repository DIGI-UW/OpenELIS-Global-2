import React from "react";
import { render, screen, within } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import messages from "../../../languages/en.json";
import AffectedAnalyzerList from "./AffectedAnalyzerList";

const analyzer = (id, name, flags) => ({
  id,
  name,
  active: true,
  pinnedProfileRevision: 1,
  pinnedMappingRevision: 2,
  newerProfileRevision: false,
  newerMappingRevision: false,
  ...flags,
});

const renderList = (analyzers) =>
  render(
    <MemoryRouter>
      <IntlProvider locale="en" messages={messages}>
        <AffectedAnalyzerList analyzers={analyzers} revision={2} />
      </IntlProvider>
    </MemoryRouter>,
  );

const item = (name) => screen.getByText(name).closest("li");

describe("AffectedAnalyzerList", () => {
  it("links an analyzer on an older revision to adopting this one", () => {
    renderList([
      analyzer("502", "TB Bench", { newerProfileRevision: true }),
      analyzer("501", "Main Lab", {}),
    ]);

    expect(
      within(item("TB Bench")).getByRole("link", { name: "Adopt revision 2" }),
    ).toHaveAttribute("href", "/analyzers/502/adoption?revision=2");
    expect(within(item("Main Lab")).queryByRole("link")).toBeNull();
  });

  it("says which analyzers on the type are confirmed and current and which are behind", () => {
    renderList([
      analyzer("501", "Main Lab", { mappingConfirmed: true }),
      analyzer("502", "TB Bench", { mappingConfirmed: false }),
      analyzer("503", "Annex", {
        mappingConfirmed: true,
        newerProfileRevision: true,
      }),
    ]);

    expect(
      within(item("Main Lab")).getByText("Confirmed and current"),
    ).toBeVisible();
    expect(within(item("TB Bench")).getByText("Behind")).toBeVisible();
    expect(within(item("Annex")).getByText("Behind")).toBeVisible();
  });

  it("links an analyzer with a saved revision not yet in force to verifying it", () => {
    renderList([
      analyzer("503", "Reference Lab", { newerMappingRevision: true }),
    ]);

    expect(
      within(item("Reference Lab")).getByRole("link", {
        name: "Verify saved mapping",
      }),
    ).toHaveAttribute("href", "/analyzers/503/mapping");
  });
});
