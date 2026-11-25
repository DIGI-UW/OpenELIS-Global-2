import React from "react";
import { fireEvent, screen, within } from "@testing-library/react";
import "@testing-library/jest-dom";
import WorkloadSection from "./WorkloadSection";
import { renderQa } from "../../qa/testUtils";

vi.mock("../../utils/Utils", async (importOriginal) => ({
  ...(await importOriginal()),
  fetchFromOpenElisServer: vi.fn(),
}));

vi.mock("@carbon/charts-react", () => ({
  StackedAreaChart: ({ data }) => (
    <div data-testid="workload-chart">{JSON.stringify(data)}</div>
  ),
}));

import { fetchFromOpenElisServer } from "../../utils/Utils";

const section = (id, name, samples, p90Tat, onTimePct) => ({
  id,
  name,
  counts: [samples],
  samples,
  medianTat: p90Tat == null ? null : p90Tat / 2,
  p90Tat,
  onTimePct,
});

const serve = (body) => fetchFromOpenElisServer.mockResolvedValue(body);

const firstColumn = () =>
  screen
    .getAllByRole("row")
    .slice(1)
    .map((row) => within(row).getAllByRole("cell")[0].textContent);

describe("WorkloadSection", () => {
  it("sorts by P90 in both directions and keeps a section without TAT last", async () => {
    serve({
      days: ["2026-03-01"],
      sections: [
        section("1", "Hematology", 40, 22, 95.1),
        section("2", "Serology", 3, null, null),
        section("3", "Microbiology", 12, 98, 81.7),
        section("4", "Chemistry", 30, 28, 92.4),
      ],
    });
    renderQa(<WorkloadSection scope="30d" scopeLabel="Last 30 days" />);

    const p90 = await screen.findByRole("button", { name: "P90" });
    fireEvent.click(p90);
    expect(firstColumn()).toEqual([
      "Hematology",
      "Chemistry",
      "Microbiology",
      "Serology",
    ]);

    fireEvent.click(p90);
    expect(firstColumn()).toEqual([
      "Microbiology",
      "Chemistry",
      "Hematology",
      "Serology",
    ]);
    expect(screen.getAllByText("—")).toHaveLength(3);
  });

  it("shows the empty-state copy instead of a chart for an empty scope", async () => {
    serve({ days: ["2026-03-01"], sections: [] });
    renderQa(<WorkloadSection scope="7d" scopeLabel="Last 7 days" />);

    expect(
      await screen.findByText("No samples were received in this period"),
    ).toBeInTheDocument();
    expect(screen.queryByTestId("workload-chart")).not.toBeInTheDocument();
  });
});
