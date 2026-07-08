import React from "react";
import { fireEvent, render, screen, within } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
import messages from "../../../../languages/en.json";
import QAOverview from "../QAOverview";

// Rendering with the real en.json also fails loudly if a referenced i18n key
// is missing (react-intl falls back to the raw key, breaking text assertions).
const renderPage = () =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <MemoryRouter>
        <QAOverview />
      </MemoryRouter>
    </IntlProvider>,
  );

beforeEach(() => {
  sessionStorage.clear();
});

describe("QAOverview shell", () => {
  test("renders page title and the six sections with the expected placeholder slots", () => {
    renderPage();

    expect(
      screen.getByRole("heading", { name: "QA Overview" }),
    ).toBeInTheDocument();
    expect(
      screen.getByText(/Daily snapshot for the QA Officer/),
    ).toBeInTheDocument();

    const slotCounts = {
      "Attention Required": 7,
      Today: 5,
      "This Week": 8,
      "Pillar Status": 4,
      "Recent Activity": 1,
    };
    Object.entries(slotCounts).forEach(([name, slots]) => {
      const region = screen.getByRole("region", { name });
      expect(within(region).getAllByText("Coming soon")).toHaveLength(slots);
    });
  });

  test("Today tiles carry the KPI titles and light-up tickets from the delivery plan", () => {
    renderPage();
    const today = screen.getByRole("region", { name: "Today" });

    [
      "Average TAT",
      "Rejection Rate",
      "Amendment Rate",
      "Critical Callback",
      "NCE Pulse",
    ].forEach((title) => {
      expect(within(today).getByText(title)).toBeInTheDocument();
    });
    ["OGC-696", "OGC-697", "OGC-698", "OGC-714", "OGC-699"].forEach(
      (ticket) => {
        expect(within(today).getByText(ticket)).toBeInTheDocument();
      },
    );
  });

  test("attention rows and pillars are ticket-annotated", () => {
    renderPage();

    const attention = screen.getByRole("region", {
      name: "Attention Required",
    });
    expect(
      within(attention).getByText("Critical NCEs pending acknowledgment"),
    ).toBeInTheDocument();
    // Overdue CAPAs + effectiveness reviews both light up via OGC-707
    expect(within(attention).getAllByText("OGC-707")).toHaveLength(2);
    expect(within(attention).getByText("NCE v2")).toBeInTheDocument();

    const pillars = screen.getByRole("region", { name: "Pillar Status" });
    [
      "Statistical QC",
      "EQA",
      "Quality Indicators",
      "QMS & Improvement",
    ].forEach((pillar) => {
      expect(within(pillars).getByText(pillar)).toBeInTheDocument();
    });
  });

  test("inspector readiness is collapsed by default and open state sticks across a remount", () => {
    const view = renderPage();
    const heading = screen.getByRole("button", {
      name: /Inspector readiness/,
    });
    expect(heading).toHaveAttribute("aria-expanded", "false");

    fireEvent.click(heading);
    expect(heading).toHaveAttribute("aria-expanded", "true");
    expect(sessionStorage.getItem("qa.overview.inspectorOpen")).toBe("1");

    const inspector = screen.getByRole("region", {
      name: /Inspector readiness/,
    });
    expect(within(inspector).getAllByText("Coming soon")).toHaveLength(5);
    expect(
      within(inspector).getByText("Are runs in control?"),
    ).toBeInTheDocument();

    view.unmount();
    renderPage();
    expect(
      screen.getByRole("button", { name: /Inspector readiness/ }),
    ).toHaveAttribute("aria-expanded", "true");
  });
});
