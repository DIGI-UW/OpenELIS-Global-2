import React from "react";
import { render, screen } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import { describe, expect, it, vi } from "vitest";
import messages from "../../../../languages/en.json";
import { RoutineReports } from "../Index";

vi.mock("../../common/ReportByDate", () => ({
  default: ({ report, id }) => (
    <section data-testid="report-by-date" data-report={report}>
      <h2>{messages[id]}</h2>
    </section>
  ),
}));

const renderReport = (report) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <RoutineReports type="indicator" report={report} />
    </IntlProvider>,
  );

describe("RoutineReports indicator routes", () => {
  it("opens the HIV summary report under its own title", () => {
    renderReport("indicatorCDILNSPHIV");
    expect(screen.getByTestId("report-by-date")).toHaveAttribute(
      "data-report",
      "indicatorCDILNSPHIV",
    );
    expect(
      screen.getByRole("heading", { name: "HIV Test Summary" }),
    ).toBeTruthy();
  });

  it("keeps the all-tests summary on its own report and title", () => {
    renderReport("indicatorHaitiLNSPAllTests");
    expect(screen.getByTestId("report-by-date")).toHaveAttribute(
      "data-report",
      "indicatorHaitiLNSPAllTests",
    );
    expect(
      screen.getByRole("heading", { name: "Test Report Summary" }),
    ).toBeTruthy();
  });
});
