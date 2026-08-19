import React from "react";
import { render, screen, fireEvent, within } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import InventoryReports, { toIsoDate } from "./InventoryReports";
import messages from "../../languages/en.json";

describe("InventoryReports — toIsoDate", () => {
  it("formats a Date as yyyy-MM-dd, not Date.toString()", () => {
    const date = new Date(Date.UTC(2026, 6, 13)); // July 13, 2026
    expect(toIsoDate(date)).toBe("2026-07-13");
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
