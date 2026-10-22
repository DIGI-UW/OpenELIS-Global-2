import { describe, expect, it } from "vitest";
import { stripPatients, TOOLS, toolVisible } from "../src/tools.js";

const ALL = new Set([
  "orders.read",
  "results.read",
  "orders.search",
  "stats.read",
  "catalog.read",
  "patient.identity",
]);
const visible = (granted: Set<string>, agent: { identifiers: boolean; tools?: string[] }) =>
  TOOLS.filter((t) => toolVisible(t, granted, agent)).map((t) => t.name);

describe("tool filtering", () => {
  it("offers everything with every scope and identifiers on", () => {
    expect(visible(ALL, { identifiers: true })).toEqual(TOOLS.map((t) => t.name));
  });

  it("hides patient tools without the identifiers switch", () => {
    const v = visible(ALL, { identifiers: false });
    expect(v).not.toContain("find_patients");
    expect(v).not.toContain("get_patient_results");
    expect(v).toContain("get_order");
  });

  it("hides patient tools when the add-on lacks patient.identity, whatever the token says", () => {
    const g = new Set([...ALL].filter((s) => s !== "patient.identity"));
    expect(visible(g, { identifiers: true })).not.toContain("find_patients");
  });

  it("hides tools whose core scopes are not granted", () => {
    expect(visible(new Set(["stats.read"]), { identifiers: true })).toEqual(["lab_statistics"]);
    const noResults = new Set([...ALL].filter((s) => s !== "results.read"));
    const v = visible(noResults, { identifiers: true });
    expect(v).not.toContain("get_order");
    expect(v).not.toContain("get_patient_results");
    expect(v).toContain("list_orders");
    expect(visible(new Set(), { identifiers: true })).toEqual([]);
  });

  it("honours the token's allowlist", () => {
    expect(visible(ALL, { identifiers: false, tools: ["list_tests", "find_patients"] })).toEqual([
      "list_tests",
    ]);
  });
});

describe("input checks", () => {
  const tool = (n: string) => TOOLS.find((t) => t.name === n)!;
  it("lab numbers", () => {
    expect(tool("get_order").input.safeParse({ labNumber: "0221026-0004" }).success).toBe(true);
    expect(tool("get_order").input.safeParse({ labNumber: "../etc" }).success).toBe(false);
    expect(tool("get_order").input.safeParse({ labNumber: "a".repeat(65) }).success).toBe(false);
  });
  it("dates and ranges", () => {
    const lo = tool("list_orders").input;
    expect(lo.safeParse({ from: "2026-10-01", to: "2026-10-31" }).success).toBe(true);
    expect(lo.safeParse({ from: "2026-10-01", to: "2026-11-01" }).success).toBe(false);
    expect(lo.safeParse({ from: "2026-10-05", to: "2026-10-01" }).success).toBe(false);
    expect(lo.safeParse({ from: "2026-13-01", to: "2026-10-01" }).success).toBe(false);
    expect(lo.safeParse({ from: "01/10/2026", to: "2026-10-01" }).success).toBe(false);
    expect(lo.safeParse({ from: "2026-10-01", to: "2026-10-02", limit: 101 }).success).toBe(false);
  });
  it("statistics groupings", () => {
    const st = tool("lab_statistics").input;
    expect(
      st.safeParse({ report: "tat", from: "2026-10-01", to: "2026-10-02", groupBy: "test" })
        .success,
    ).toBe(true);
    expect(
      st.safeParse({ report: "tat", from: "2026-10-01", to: "2026-10-02", groupBy: "day" }).success,
    ).toBe(false);
    expect(
      st.safeParse({ report: "orders", from: "2026-10-01", to: "2026-10-02", groupBy: "test" })
        .success,
    ).toBe(false);
  });
  it("patient search", () => {
    expect(tool("find_patients").input.safeParse({ query: "L" }).success).toBe(false);
    expect(
      tool("get_patient_results").input.safeParse({ patientId: "1001", limit: 21 }).success,
    ).toBe(false);
  });
});

describe("patient stripping", () => {
  it("removes patient objects at any depth", () => {
    const data = {
      accessionNumber: "1",
      patient: { fullName: "Nguyễn Thị Lan" },
      orders: [{ accessionNumber: "2", patient: { fullName: "X" } }],
      patients: [{ fullName: "Y" }],
    };
    const out = stripPatients(data);
    expect(out).toEqual({ accessionNumber: "1", orders: [{ accessionNumber: "2" }] });
    expect(JSON.stringify(out)).not.toMatch(/Lan|"X"|"Y"/);
    expect(data.patient).toBeTruthy(); // input untouched
  });
});

describe("withFlag", () => {
  it("flags numbers outside a numeric range only", async () => {
    const { withFlag } = await import("../src/tools.js");
    expect(withFlag({ value: "5.2", low: 3.9, high: 6.1 }).flag).toBeUndefined();
    expect(withFlag({ value: "7.4", low: 3.9, high: 6.1 }).flag).toBe("H");
    expect(withFlag({ value: 2, low: "3.9", high: null }).flag).toBe("L");
    expect(withFlag({ value: "Negative", low: null, high: null }).flag).toBeUndefined();
    expect(withFlag({ value: "<5", low: 60, high: null }).flag).toBeUndefined();
  });
});
