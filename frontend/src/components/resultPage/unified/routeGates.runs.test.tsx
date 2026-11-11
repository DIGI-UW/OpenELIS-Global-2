import React from "react";
import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import "@testing-library/jest-dom";
import { BrowserRouter } from "react-router-dom";

/**
 * OGC-1200 FR-E1b: opening a run replaces the worklist's filters, and going
 * back to all results restores the worklist exactly as it was.
 */

vi.mock("./UnifiedResults", () => ({
  default: ({ onOpenRun }: { onOpenRun: (id: number) => void }) => (
    <button type="button" onClick={() => onOpenRun(7)}>
      worklist {window.location.search}
    </button>
  ),
}));

vi.mock("./runs/RunView", () => ({
  default: ({ runId, filtersReplaced, onBack }: any) => (
    <div>
      <span data-testid="run">
        run {runId} {filtersReplaced ? "replaced" : "kept"}
      </span>
      <button type="button" onClick={onBack}>
        back
      </button>
    </div>
  ),
}));

import { UnifiedResultsRoute } from "./routeGates";

const renderAt = (url: string) => {
  window.history.replaceState(null, "", url);
  render(
    <BrowserRouter>
      <UnifiedResultsRoute />
    </BrowserRouter>,
  );
};

describe("OGC-1200 opening a run from the worklist", () => {
  beforeEach(() => cleanup());

  it("opens the run in place of a filtered worklist and restores the filters on the way back", () => {
    renderAt("/Results?testSectionId=36&status=4");

    fireEvent.click(screen.getByRole("button", { name: /worklist/ }));

    expect(screen.getByTestId("run")).toHaveTextContent("run 7 replaced");
    expect(window.location.search).toBe("?run=7");

    fireEvent.click(screen.getByRole("button", { name: "back" }));

    expect(window.location.search).toBe("?testSectionId=36&status=4");
    expect(screen.getByRole("button", { name: /worklist/ })).toHaveTextContent(
      "testSectionId=36",
    );
  });

  it("opens a run from an unfiltered worklist without a filters notice", () => {
    renderAt("/Results");

    fireEvent.click(screen.getByRole("button", { name: /worklist/ }));

    expect(screen.getByTestId("run")).toHaveTextContent("run 7 kept");
  });

  it("opens a run named in the address directly", () => {
    renderAt("/Results?run=12");

    expect(screen.getByTestId("run")).toHaveTextContent("run 12 kept");
  });
});
