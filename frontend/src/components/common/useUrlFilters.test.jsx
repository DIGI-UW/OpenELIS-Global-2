import React from "react";
import { renderHook, act } from "@testing-library/react-hooks";
import { MemoryRouter } from "react-router-dom";
import { useUrlFilters } from "./useUrlFilters";

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
