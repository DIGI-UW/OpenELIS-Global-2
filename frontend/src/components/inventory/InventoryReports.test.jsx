import React from "react";
import { render, screen, fireEvent, within } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import InventoryReports, { toIsoDate } from "./InventoryReports";
import messages from "../../languages/en.json";

describe("InventoryReports — toIsoDate", () => {
  const originalTz = process.env.TZ;
  afterEach(() => {
    process.env.TZ = originalTz;
  });

  // The exported string must be the picked local day in every zone, not the UTC day.
  it.each([
    ["Africa/Nairobi", "2026-07-13"],
    ["Africa/Abidjan", "2026-07-13"],
    ["America/Port-au-Prince", "2026-07-13"],
  ])("formats local midnight in %s as %s", (tz, expected) => {
    process.env.TZ = tz;
    expect(toIsoDate(new Date(2026, 6, 13))).toBe(expected);
  });

  it("keeps the picked day for a time late in the local evening", () => {
    process.env.TZ = "America/Port-au-Prince";
    expect(toIsoDate(new Date(2026, 6, 13, 23, 30))).toBe("2026-07-13");
  });

  it("returns null for a null/undefined date", () => {
    expect(toIsoDate(null)).toBeNull();
    expect(toIsoDate(undefined)).toBeNull();
  });
});

const renderWithIntl = () =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <InventoryReports />
    </IntlProvider>,
  );

describe("InventoryReports — dropdown options render (not blank)", () => {
  it("shows report type options with visible text when opened", () => {
    renderWithIntl();

    fireEvent.click(document.querySelector("#reportType button"));
    const listbox = document.querySelector("#reportType .cds--list-box__menu");

    expect(
      within(listbox).getByText("Stock Levels Report"),
    ).toBeInTheDocument();
  });

  it("shows export format options with visible text when opened", () => {
    renderWithIntl();

    fireEvent.click(document.querySelector("#exportFormat button"));
    const listbox = document.querySelector(
      "#exportFormat .cds--list-box__menu",
    );

    // Regression check: this Dropdown was missing itemToString, so Carbon
    // couldn't render the {id, text, icon} option objects as labels and every
    // row in the open list appeared blank.
    expect(within(listbox).getByText("PDF")).toBeInTheDocument();
    expect(within(listbox).getByText("Excel (.xlsx)")).toBeInTheDocument();
    expect(within(listbox).getByText("CSV")).toBeInTheDocument();
  });
});

describe("InventoryReports — filter options follow the report type", () => {
  const selectReportType = (label) => {
    fireEvent.click(document.querySelector("#reportType button"));
    const listbox = document.querySelector("#reportType .cds--list-box__menu");
    fireEvent.click(within(listbox).getByText(label));
  };

  it("offers 'include expired lots' only for the report that reads it", () => {
    renderWithIntl();

    selectReportType("Expiration Forecast");
    expect(screen.getByLabelText("Include expired lots")).toBeInTheDocument();

    selectReportType("Stock Levels Report");
    expect(screen.queryByLabelText("Include expired lots")).toBeNull();
    expect(screen.getByLabelText("Include inactive items")).toBeInTheDocument();
  });

  it("hides both filters for report types that read neither", () => {
    renderWithIntl();

    selectReportType("Transaction History");

    expect(screen.queryByLabelText("Include inactive items")).toBeNull();
    expect(screen.queryByLabelText("Include expired lots")).toBeNull();
  });

  it("offers the date range only for report types that read it", () => {
    renderWithIntl();

    selectReportType("Stock Levels Report");
    expect(screen.queryByLabelText("Start Date")).toBeNull();

    selectReportType("Expiration Forecast");
    expect(screen.getByLabelText("Start Date")).toBeInTheDocument();
    expect(screen.getByLabelText("End Date")).toBeInTheDocument();

    selectReportType("Low Stock Alert");
    expect(screen.queryByLabelText("Start Date")).toBeNull();
  });
});
