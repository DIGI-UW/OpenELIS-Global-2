import { toIsoDate } from "./InventoryReports";

describe("InventoryReports — toIsoDate", () => {
  it("formats a Date as yyyy-MM-dd, not Date.toString()", () => {
    const date = new Date(Date.UTC(2026, 6, 13)); // July 13, 2026
    expect(toIsoDate(date)).toBe("2026-07-13");
  });

  it("returns null for a null/undefined date", () => {
    expect(toIsoDate(null)).toBeNull();
    expect(toIsoDate(undefined)).toBeNull();
  });
});
