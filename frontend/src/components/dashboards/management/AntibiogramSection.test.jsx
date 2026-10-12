import React from "react";
import { screen, within } from "@testing-library/react";
import "@testing-library/jest-dom";
import AntibiogramSection from "./AntibiogramSection";
import { renderQa } from "../../qa/testUtils";

vi.mock("../../utils/Utils", async (importOriginal) => ({
  ...(await importOriginal()),
  fetchFromOpenElisServer: vi.fn(),
}));

import { fetchFromOpenElisServer } from "../../utils/Utils";

const render = (features = { ast: true }) =>
  renderQa(<AntibiogramSection scope="year" features={features} />);

describe("AntibiogramSection", () => {
  it("tints each cell by its resistance band and blanks cells with too few isolates", async () => {
    fetchFromOpenElisServer.mockResolvedValue({
      organisms: [{ id: "o1", name: "E. coli", isolates: 68 }],
      antibiotics: [
        { id: "a1", name: "Ampicillin" },
        { id: "a2", name: "Ciprofloxacin" },
        { id: "a3", name: "Meropenem" },
        { id: "a4", name: "Vancomycin" },
      ],
      cells: [
        { organismId: "o1", antibioticId: "a1", n: 68, pctR: 72.1 },
        { organismId: "o1", antibioticId: "a2", n: 60, pctR: 51 },
        { organismId: "o1", antibioticId: "a3", n: 9, pctR: null },
      ],
      fallback: false,
    });
    render();

    const row = (await screen.findByText("E. coli (n=68)")).closest("tr");
    const cells = within(row).getAllByRole("cell");
    expect(cells[1]).toHaveTextContent("72%n=68");
    expect(cells[1]).toHaveClass("mgmt-antibiogram__cell--red");
    expect(cells[2]).toHaveClass("mgmt-antibiogram__cell--orange");
    expect(cells[3]).toHaveTextContent("—");
    expect(cells[3]).toHaveAttribute(
      "title",
      "Only 9 tested; a percentage needs at least 10",
    );
    expect(cells[4]).toHaveTextContent("—");
    expect(
      screen.getByText(
        "First isolate per patient, 7-day window · CLSI breakpoints",
      ),
    ).toBeInTheDocument();
  });

  it("lists organism counts when there are no susceptibility results", async () => {
    fetchFromOpenElisServer.mockResolvedValue({
      organisms: [
        { id: "o1", name: "E. coli", isolates: 12 },
        { id: "o2", name: "S. aureus", isolates: 4 },
      ],
      antibiotics: [],
      cells: [],
      fallback: true,
    });
    render();

    expect(await screen.findByText("S. aureus (n=4)")).toBeInTheDocument();
    expect(screen.queryByRole("table")).not.toBeInTheDocument();
  });

  it("is absent without AST data and asks for nothing", () => {
    fetchFromOpenElisServer.mockClear();
    const { container } = render({ ast: false });

    expect(container.querySelector("[data-testid='mgmt-antibiogram']")).toBe(
      null,
    );
    expect(fetchFromOpenElisServer).not.toHaveBeenCalled();
  });
});
