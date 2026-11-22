import React from "react";
import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import { vi } from "vitest";
import messages from "../languages/en.json";

vi.mock("./home/Dashboard.tsx", () => ({
  default: () => <div data-testid="home-dashboard" />,
}));

import Home from "./Home";

/**
 * OGC-1448: the home Dashboard had no page heading; its title is the page's one
 * h1, as on the QC pages (OGC-1224).
 */
describe("Home", () => {
  it("has exactly one h1, the Dashboard title", () => {
    render(
      <IntlProvider locale="en" messages={messages}>
        <MemoryRouter>
          <Home />
        </MemoryRouter>
      </IntlProvider>,
    );

    const headings = screen.getAllByRole("heading", { level: 1 });
    expect(headings).toHaveLength(1);
    expect(headings[0]).toHaveTextContent("Dashboard");
  });
});
