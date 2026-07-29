import React from "react";
import { render, screen } from "@testing-library/react";
import FilterContext from "../filter/filter-context";
import { GroupedTimeline } from "./grouped-timeline";

const renderTimeline = (value: string) =>
  render(
    <FilterContext.Provider
      value={
        {
          activeTests: ["microbiology"],
          checkboxes: {},
          someChecked: false,
          timelineData: {
            loaded: true,
            data: {
              parsedTime: {
                sortedTimes: ["2026-07-29T14:25:00Z"],
              },
              rowData: [
                {
                  flatName: "microbiology",
                  display: "UAT microbiology culture",
                  entries: [{ value, interpretation: "NORMAL" }],
                },
              ],
            },
          },
        } as any
      }
    >
      <GroupedTimeline />
    </FilterContext.Provider>,
  );

describe("GroupedTimeline", () => {
  it("renders long clinical narratives as readable text instead of a truncated tag", () => {
    const narrative =
      "ISO-1: Escherichia coli confirmed; Gentamicin (UAT) S, Ciprofloxacin (UAT) R";

    renderTimeline(narrative);

    const result = screen.getByText(narrative);
    expect(result).toBeVisible();
    expect(result.closest(".cds--tag")).toBeNull();
  });

  it("keeps concise interpreted results in a Carbon tag", () => {
    renderTimeline("7.2 mmol/L");

    expect(screen.getByText("7.2 mmol/L").closest(".cds--tag")).not.toBeNull();
  });
});
