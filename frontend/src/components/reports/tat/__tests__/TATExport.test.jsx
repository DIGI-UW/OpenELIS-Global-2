import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import messages from "../../../../languages/en.json";
import TATExport from "../TATExport";

const renderWithIntl = (component) => {
  return render(
    <IntlProvider locale="en" messages={messages}>
      {component}
    </IntlProvider>,
  );
};

const mockFilters = {
  fromDate: "2026-03-01",
  toDate: "2026-03-31",
  segment: "RECEIPT_TO_VALIDATION",
};

const mockBuildQueryString = vi.fn(
  (filters, extra) => `fromDate=${filters.fromDate}${extra || ""}`,
);

describe("TATExport", () => {
  let windowOpenSpy;

  beforeEach(() => {
    vi.clearAllMocks();
    windowOpenSpy = vi.spyOn(window, "open").mockImplementation(() => null);
  });

  afterEach(() => {
    windowOpenSpy.mockRestore();
  });

  test("renders OverflowMenu", () => {
    renderWithIntl(
      <TATExport filters={mockFilters} buildQueryString={mockBuildQueryString} />,
    );

    // The overflow menu button should be present
    expect(screen.getByRole("button")).toBeInTheDocument();
  });

  test("renders overflow menu with items", () => {
    const { container } = renderWithIntl(
      <TATExport filters={mockFilters} buildQueryString={mockBuildQueryString} />,
    );

    // Carbon OverflowMenu should render as a wrapper div
    const menu = container.querySelector(".cds--overflow-menu");
    expect(menu).toBeTruthy();
  });

  test("export menu button is present and clickable", () => {
    renderWithIntl(
      <TATExport filters={mockFilters} buildQueryString={mockBuildQueryString} />,
    );

    const button = screen.getByRole("button");
    expect(button).toBeInTheDocument();
    fireEvent.click(button);
    // No error means the menu opened successfully
  });

  test("CSV export opens URL with serverBaseUrl prefix", () => {
    renderWithIntl(
      <TATExport filters={mockFilters} buildQueryString={mockBuildQueryString} />,
    );

    // Open the overflow menu
    fireEvent.click(screen.getByRole("button"));

    // Click the CSV menu item
    const csvItem = screen.getByText("Export CSV");
    fireEvent.click(csvItem);

    expect(window.open).toHaveBeenCalledTimes(1);
    const url = windowOpenSpy.mock.calls[0][0];
    expect(url).toMatch(
      /^\/api\/OpenELIS-Global\/rest\/reports\/tat\/export/,
    );
  });
});
