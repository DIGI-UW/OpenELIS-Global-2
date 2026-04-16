/**
 * Phase 4a (RED) — BreadcrumbNav tests.
 *
 * Small reusable wrapper around Carbon's Breadcrumb that:
 *   - Takes a `crumbs` array: [{ label, href }, ...]
 *   - Renders each crumb as a clickable link
 *   - Marks the LAST crumb as the current page (non-clickable)
 *   - Uses react-router-dom Link for client-side navigation (so back/
 *     forward stays SPA, no full-page reload)
 */

import React from "react";
import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { MemoryRouter } from "react-router-dom";
import { IntlProvider } from "react-intl";
import BreadcrumbNav from "./BreadcrumbNav";

const renderWithRouter = (ui, { route = "/" } = {}) =>
  render(
    <IntlProvider locale="en" messages={{}}>
      <MemoryRouter initialEntries={[route]}>{ui}</MemoryRouter>
    </IntlProvider>,
  );

describe("BreadcrumbNav", () => {
  it("renders a Breadcrumb landmark with each crumb as a list item", () => {
    renderWithRouter(
      <BreadcrumbNav
        crumbs={[
          { label: "Storage", href: "/Storage" },
          { label: "Sample Items", href: "/Storage/sample-items" },
        ]}
      />,
    );
    expect(screen.getByRole("navigation")).toBeInTheDocument();
    expect(screen.getByText("Storage")).toBeInTheDocument();
    expect(screen.getByText("Sample Items")).toBeInTheDocument();
  });

  it("renders each crumb except the last as a link to its href", () => {
    renderWithRouter(
      <BreadcrumbNav
        crumbs={[
          { label: "Storage", href: "/Storage" },
          { label: "Sample Items", href: "/Storage/sample-items" },
          {
            label: "DEV-001",
            href: "/Storage/sample-items/42/manage-location",
          },
        ]}
      />,
    );
    // The first two are links; the third is the current page (not a link)
    const storageLink = screen.getByRole("link", { name: "Storage" });
    expect(storageLink).toHaveAttribute("href", "/Storage");
    const sampleItemsLink = screen.getByRole("link", { name: "Sample Items" });
    expect(sampleItemsLink).toHaveAttribute("href", "/Storage/sample-items");
    expect(screen.queryByRole("link", { name: "DEV-001" })).toBeNull();
  });

  it("marks the last crumb with aria-current='page'", () => {
    renderWithRouter(
      <BreadcrumbNav
        crumbs={[
          { label: "Storage", href: "/Storage" },
          { label: "Rooms", href: "/Storage/rooms" },
        ]}
      />,
    );
    // Carbon BreadcrumbItem with isCurrentPage renders aria-current="page"
    // somewhere within the crumb. Find the element directly.
    const currents = document.querySelectorAll('[aria-current="page"]');
    expect(currents.length).toBe(1);
    expect(currents[0]).toHaveTextContent("Rooms");
  });

  it("renders nothing when crumbs is empty", () => {
    const { container } = renderWithRouter(<BreadcrumbNav crumbs={[]} />);
    expect(container.querySelector("nav")).toBeNull();
  });
});
