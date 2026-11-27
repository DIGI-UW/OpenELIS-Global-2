import React from "react";
import { fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { IntlProvider } from "react-intl";
import { MemoryRouter, Route } from "react-router-dom";
import { describe, it, expect } from "vitest";
import messages from "../../languages/en.json";
import RangeNotAppliedWarning from "./RangeNotAppliedWarning";

const renderWarning = (props) => {
  let location;
  render(
    <IntlProvider locale="en" messages={messages}>
      <MemoryRouter initialEntries={["/SamplePatientEntry"]}>
        <RangeNotAppliedWarning {...props} />
        <Route
          path="*"
          render={(routeProps) => {
            location = routeProps.location;
            return null;
          }}
        />
      </MemoryRouter>
    </IntlProvider>,
  );
  return () => location;
};

describe("RangeNotAppliedWarning", () => {
  it("renders nothing when every range applies", () => {
    renderWarning({ tests: [], labNumber: "DEV01" });
    expect(
      screen.queryByTestId("range-not-applied-warning"),
    ).not.toBeInTheDocument();
  });

  it("lists the affected tests without blocking the order", () => {
    renderWarning({ tests: ["Haemoglobin", "Creatinine"], labNumber: "DEV01" });
    expect(
      screen.getByText(
        "Some results will not be checked against a reference range",
      ),
    ).toBeInTheDocument();
    expect(screen.getByText(/Haemoglobin, Creatinine/)).toBeInTheDocument();
  });

  it("Report NCE opens the NCE form on this order with the reason drafted", () => {
    const currentLocation = renderWarning({
      tests: ["Haemoglobin"],
      labNumber: "DEV01",
    });
    fireEvent.click(screen.getByTestId("range-not-applied-report-nce"));
    const location = currentLocation();
    expect(location.pathname).toBe("/ReportNonConformingEvent");
    const params = new URLSearchParams(location.search);
    expect(params.get("labNumber")).toBe("DEV01");
    expect(params.get("description")).toContain("Haemoglobin");
  });
});
