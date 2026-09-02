import React from "react";
import { renderHook, act } from "@testing-library/react-hooks";
import { MemoryRouter } from "react-router-dom";
import { useUrlFilters, useUrlFilterAutoRun } from "./useUrlFilters";

// Router context so useLocation/useHistory resolve; initialEntries seeds the URL.
const routerWrapper =
  (entries) =>
  ({ children }) => (
    <MemoryRouter initialEntries={entries}>{children}</MemoryRouter>
  );

describe("useUrlFilters", () => {
  it("falls back to defaults when the URL carries no params", () => {
    const { result } = renderHook(
      () => useUrlFilters({ dateFrom: "", siteId: "" }),
      { wrapper: routerWrapper(["/VectorSurveillanceReport"]) },
    );
    expect(result.current.values).toEqual({ dateFrom: "", siteId: "" });
    expect(result.current.hasParams).toBe(false);
  });

  it("reads scalar and array filters from the URL", () => {
    const { result } = renderHook(
      () => useUrlFilters({ dateFrom: "", siteIds: [] }),
      {
        wrapper: routerWrapper([
          "/VectorSurveillanceReport?dateFrom=01%2F07%2F2026&siteIds=10,11",
        ]),
      },
    );
    expect(result.current.values.dateFrom).toBe("01/07/2026");
    expect(result.current.values.siteIds).toEqual(["10", "11"]);
    expect(result.current.hasParams).toBe(true);
  });

  it("writes filters to the URL on set and omits empty values", () => {
    const { result } = renderHook(
      () => useUrlFilters({ dateFrom: "", dateTo: "", siteIds: [] }),
      { wrapper: routerWrapper(["/VectorSurveillanceReport"]) },
    );

    act(() => {
      result.current.setUrlFilters({
        dateFrom: "01/07/2026",
        dateTo: "",
        siteIds: ["10", "11"],
      });
    });

    // The hook re-reads the pushed URL: set values round-trip, empty ones are absent.
    expect(result.current.values.dateFrom).toBe("01/07/2026");
    expect(result.current.values.siteIds).toEqual(["10", "11"]);
    expect(result.current.values.dateTo).toBe("");
    expect(result.current.hasParams).toBe(true);
  });
});

describe("useUrlFilterAutoRun", () => {
  it("runs once on mount when shouldRun is true at mount", () => {
    const run = vi.fn();
    renderHook(() => useUrlFilterAutoRun(true, run), {
      wrapper: routerWrapper(["/report?dateFrom=x"]),
    });
    expect(run).toHaveBeenCalledTimes(1);
  });

  it("never runs when shouldRun is false at mount", () => {
    const run = vi.fn();
    renderHook(() => useUrlFilterAutoRun(false, run), {
      wrapper: routerWrapper(["/report"]),
    });
    expect(run).not.toHaveBeenCalled();
  });

  it("does not run again when shouldRun flips true after mount (the double-fetch guard)", () => {
    const run = vi.fn();
    // shouldRun starts false; rerender with true (as happens when an Apply pushes
    // filters to the URL). The mount snapshot must keep it from firing.
    const { rerender } = renderHook(
      ({ shouldRun }) => useUrlFilterAutoRun(shouldRun, run),
      {
        wrapper: routerWrapper(["/report"]),
        initialProps: { shouldRun: false },
      },
    );
    rerender({ shouldRun: true });
    expect(run).not.toHaveBeenCalled();
  });

  it("runs only once across re-renders when the run identity changes", () => {
    let run = vi.fn();
    const { rerender } = renderHook(() => useUrlFilterAutoRun(true, run), {
      wrapper: routerWrapper(["/report?dateFrom=x"]),
    });
    const firstRun = run;
    run = vi.fn(); // new identity, as a fresh useCallback each render would be
    rerender();
    expect(firstRun).toHaveBeenCalledTimes(1);
    expect(run).not.toHaveBeenCalled();
  });
});
