import {
  formatBrf02SamplePath,
  formatBrf02SamplePathFromHierarchical,
} from "./biorepositorySamplePathHelpers";

describe("biorepositorySamplePathHelpers", () => {
  test("formatBrf02SamplePath builds full BRF-02 path from structured fields", () => {
    expect(
      formatBrf02SamplePath({
        roomName: "Room-A",
        deviceName: "Freezer-1",
        shelfLabel: "S2",
        rackLabel: "R15",
        boxLabel: "BX078",
        positionCoordinate: "B3",
      }),
    ).toBe(
      "Zn Room-A / FRZ Freezer-1 / SH S2 / RK R15 / Box BX078 / Pos B3",
    );
  });

  test("formatBrf02SamplePath omits missing levels", () => {
    expect(
      formatBrf02SamplePath({
        roomName: "Room-A",
        deviceName: "Freezer-1",
      }),
    ).toBe("Zn Room-A / FRZ Freezer-1");
  });

  test("formatBrf02SamplePathFromHierarchical parses hierarchical path", () => {
    expect(
      formatBrf02SamplePathFromHierarchical(
        "Room-A > Freezer-1 > S2 > R15 > BX078",
        "B3",
      ),
    ).toBe(
      "Zn Room-A / FRZ Freezer-1 / SH S2 / RK R15 / Box BX078 / Pos B3",
    );
  });

  test("formatBrf02SamplePath uses existing samplePath when present", () => {
    expect(
      formatBrf02SamplePath({
        samplePath: "Zn Room-A / FRZ Freezer-1",
        roomName: "Other",
      }),
    ).toBe("Zn Room-A / FRZ Freezer-1");
  });

  test("formatBrf02SamplePathFromHierarchical returns Pos-only path when needed", () => {
    expect(formatBrf02SamplePathFromHierarchical(null, "B3")).toBe("Pos B3");
  });
});
