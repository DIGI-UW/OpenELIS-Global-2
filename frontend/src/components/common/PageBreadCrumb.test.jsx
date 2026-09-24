import React from "react";
import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import PageBreadCrumb from "./PageBreadCrumb";

const messages = {
  "home.label": "Home",
  "routine.reports": "Routine Reports",
  "reports.label.status": "Patient Status Report",
};

const wrap = (breadcrumbs) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <MemoryRouter>
        <PageBreadCrumb breadcrumbs={breadcrumbs} />
      </MemoryRouter>
    </IntlProvider>,
  );

const crumbLinks = () =>
  Array.from(
    screen
      .getByRole("navigation", { name: "Breadcrumb" })
      .querySelectorAll("a"),
  ).map((a) => a.textContent);

describe("PageBreadCrumb", () => {
  it("keeps a category crumb visible but unclickable when it has no destination", () => {
    // the shape every report leaf uses: Home / <category, no page> / <this report>
    wrap([
      { label: "home.label", link: "/" },
      { label: "routine.reports", link: "" },
      { label: "reports.label.status", link: "/RoutineReport?type=patient" },
    ]);

    const nav = screen.getByRole("navigation", { name: "Breadcrumb" });
    expect(nav.textContent).toContain("Routine Reports");
    // only Home navigates; the category and the current page are plain text
    expect(crumbLinks()).toEqual(["Home"]);
    expect(
      screen.getByText("Routine Reports").closest("a"),
    ).not.toBeInTheDocument();
  });

  it("marks the last crumb as the current page rather than a link", () => {
    wrap([
      { label: "home.label", link: "/" },
      { label: "routine.reports", link: "/RoutineReports" },
    ]);

    const current = screen
      .getByRole("navigation", { name: "Breadcrumb" })
      .querySelectorAll('[aria-current="page"]');
    expect(current).toHaveLength(1);
    expect(current[0].textContent).toBe("Routine Reports");
    expect(crumbLinks()).toEqual(["Home"]);
  });

  it("keeps a single crumb clickable", () => {
    wrap([{ label: "home.label", link: "/" }]);
    expect(crumbLinks()).toEqual(["Home"]);
  });

  it("falls back to the label when it is not a known message id", () => {
    // plugin/dynamic labels (an analyzer name, a box label) are passed verbatim
    wrap([
      { label: "home.label", link: "/" },
      { label: "Leonardo", link: "" },
    ]);
    expect(screen.getByText("Leonardo")).toBeInTheDocument();
  });
});
