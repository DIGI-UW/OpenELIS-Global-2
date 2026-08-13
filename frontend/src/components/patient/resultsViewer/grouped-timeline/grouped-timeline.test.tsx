import React from "react";
import { render, screen } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import messages from "../../../../languages/en.json";
import GroupedTimeline from "./grouped-timeline";
import FilterContext from "../filter/filter-context";

/**
 * A test can be configured with several sample types and several components,
 * so the Test column has to say which of them a row is: the name on its own
 * cannot tell two components of the same test apart.
 */
const timelineData = (rowData: any[]) => ({
  loaded: true,
  data: {
    parsedTime: { sortedTimes: ["2026-08-01 09:00:00.0"] },
    rowData,
    panelName: "timeline",
  },
});

const renderTimeline = (rowData: any[]) =>
  render(
    <IntlProvider locale="en" messages={messages}>
      <FilterContext.Provider
        value={
          {
            activeTests: [],
            timelineData: timelineData(rowData),
            checkboxes: {},
            someChecked: false,
          } as any
        }
      >
        <GroupedTimeline />
      </FilterContext.Provider>
    </IntlProvider>,
  );

describe("GroupedTimeline test column", () => {
  it("identifies a row by its test, sample type and component", () => {
    renderTimeline([
      {
        flatName: "Haematology-Serum-Blood Pressure — Systolic",
        display: "Blood Pressure — Systolic",
        testName: "Blood Pressure",
        sampleType: "Serum",
        component: "Systolic",
        range: "90 - 120",
        units: "mmHg",
        entries: [{ value: "130", interpretation: "HIGH" }],
      },
    ]);

    expect(screen.getByText("Blood Pressure")).toBeInTheDocument();
    expect(screen.getByText("Serum · Systolic")).toBeInTheDocument();
    expect(screen.getByText("90 - 120 mmHg")).toBeInTheDocument();
    expect(screen.getByText("130")).toBeInTheDocument();
  });

  it("keeps two components of the same test as two rows", () => {
    renderTimeline([
      {
        flatName: "Haematology-Serum-Blood Pressure — Systolic",
        display: "Blood Pressure — Systolic",
        testName: "Blood Pressure",
        sampleType: "Serum",
        component: "Systolic",
        range: "90 - 120",
        entries: [{ value: "130", interpretation: "HIGH" }],
      },
      {
        flatName: "Haematology-Serum-Blood Pressure — Diastolic",
        display: "Blood Pressure — Diastolic",
        testName: "Blood Pressure",
        sampleType: "Serum",
        component: "Diastolic",
        range: "60 - 80",
        entries: [{ value: "75", interpretation: "NORMAL" }],
      },
    ]);

    expect(screen.getByText("Serum · Systolic")).toBeInTheDocument();
    expect(screen.getByText("Serum · Diastolic")).toBeInTheDocument();
    expect(screen.getByText("90 - 120")).toBeInTheDocument();
    expect(screen.getByText("60 - 80")).toBeInTheDocument();
    expect(screen.getByText("130")).toBeInTheDocument();
    expect(screen.getByText("75")).toBeInTheDocument();
  });

  it("falls back to the tree label when the row carries no test name", () => {
    renderTimeline([
      {
        flatName: "Haematology-Serum-Glucose",
        display: "Glucose",
        sampleType: "Serum",
        entries: [{ value: "5", interpretation: "NORMAL" }],
      },
    ]);

    expect(screen.getByText("Glucose")).toBeInTheDocument();
    expect(screen.getByText("Serum")).toBeInTheDocument();
  });
});
