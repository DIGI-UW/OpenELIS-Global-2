import React from "react";
import { render, screen } from "@testing-library/react";
import { IntlProvider } from "react-intl";
import { MemoryRouter } from "react-router-dom";
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
    <MemoryRouter>
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
      </IntlProvider>
    </MemoryRouter>,
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

describe("GroupedTimeline trend graph link", () => {
  /**
   * The line graph for one test is reached by clicking its name here. That
   * link was dropped when the timeline was rewritten onto Carbon's DataTable,
   * which took the graph out of reach entirely — nothing else navigates to it.
   */
  const numericRow = {
    flatName: "Haematology-Serum-Blood Pressure — Systolic",
    display: "Blood Pressure — Systolic",
    testName: "Blood Pressure",
    sampleType: "Serum",
    component: "Systolic",
    conceptUuid: "42",
    sampleTypeId: "1",
    componentId: "c-sys",
    obs: [{ obsDatetime: "2026-08-01 09:00:00", value: "130" }],
    entries: [{ value: "130", interpretation: "HIGH" }],
  };

  it("links a numeric row to its own test, specimen and component", () => {
    renderTimeline([numericRow]);

    const link = screen.getByTestId(
      "trend-link-Haematology-Serum-Blood Pressure — Systolic",
    );
    expect(link).toHaveTextContent("Blood Pressure");
    expect(link).toHaveAttribute(
      "href",
      "#trendline/testId=42&sampleTypeId=1&componentId=c-sys",
    );
  });

  it("gives each component of a test its own link", () => {
    renderTimeline([
      numericRow,
      {
        ...numericRow,
        flatName: "Haematology-Serum-Blood Pressure — Diastolic",
        component: "Diastolic",
        componentId: "c-dia",
        obs: [{ obsDatetime: "2026-08-01 09:00:00", value: "80" }],
        entries: [{ value: "80", interpretation: "NORMAL" }],
      },
    ]);

    const hrefs = screen
      .getAllByRole("link")
      .map((a) => a.getAttribute("href"));
    expect(hrefs).toEqual([
      "#trendline/testId=42&sampleTypeId=1&componentId=c-sys",
      "#trendline/testId=42&sampleTypeId=1&componentId=c-dia",
    ]);
  });

  it("offers no graph for a result no line can be drawn through", () => {
    renderTimeline([
      {
        ...numericRow,
        flatName: "Molecular-Swab-COVID-19 PCR",
        component: undefined,
        obs: [{ obsDatetime: "2026-08-01 09:00:00", value: "Detected" }],
        entries: [{ value: "Detected", interpretation: "NORMAL" }],
      },
    ]);

    expect(screen.queryByRole("link")).not.toBeInTheDocument();
    expect(screen.getByText("Blood Pressure")).toBeInTheDocument();
  });
});

describe("GroupedTimeline result presentation", () => {
  const resultRow = (value: string) => [
    {
      flatName: "microbiology",
      display: "UAT microbiology culture",
      entries: [{ value, interpretation: "NORMAL" }],
    },
  ];

  it("renders long clinical narratives as readable text instead of a truncated tag", () => {
    const narrative =
      "ISO-1: Escherichia coli confirmed; Gentamicin (UAT) S, Ciprofloxacin (UAT) R";

    renderTimeline(resultRow(narrative));

    const result = screen.getByText(narrative);
    expect(result).toBeVisible();
    expect(result.closest(".cds--tag")).toBeNull();
  });

  it("preserves line breaks in multiline clinical narratives", () => {
    const narrative =
      "Culture: Escherichia coli\nCiprofloxacin: Resistant\nGentamicin: Susceptible";

    renderTimeline(resultRow(narrative));

    const result = screen.getByText(
      (_content, element) =>
        element?.tagName === "SPAN" && element.textContent === narrative,
    );
    expect(result).toHaveStyle({ whiteSpace: "pre-wrap" });
  });

  it("keeps concise interpreted results in a Carbon tag", () => {
    renderTimeline(resultRow("7.2 mmol/L"));

    expect(screen.getByText("7.2 mmol/L").closest(".cds--tag")).not.toBeNull();
  });
});
