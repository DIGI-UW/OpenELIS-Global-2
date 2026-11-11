import {
  getDeepestLocationSelection,
  positionToCoordinate,
  selectionToHierarchicalPath,
} from "./locationSelectionMapper";

describe("locationSelectionMapper", () => {
  test("returns deepest assignable level for device/shelf/rack/box", () => {
    expect(
      getDeepestLocationSelection(
        { room: { id: 1, name: "R" }, device: { id: 2, name: "D" } },
        { requireAssignable: true },
      ),
    ).toEqual({
      type: "device",
      value: { id: 2, name: "D" },
    });

    expect(
      getDeepestLocationSelection(
        {
          room: { id: 1, name: "R" },
          device: { id: 2, name: "D" },
          shelf: { id: 3, name: "S" },
        },
        { requireAssignable: true },
      ),
    ).toEqual({
      type: "shelf",
      value: { id: 3, name: "S" },
    });

    expect(
      getDeepestLocationSelection(
        {
          room: { id: 1, name: "R" },
          device: { id: 2, name: "D" },
          shelf: { id: 3, name: "S" },
          rack: { id: 4, name: "RK" },
        },
        { requireAssignable: true },
      ),
    ).toEqual({
      type: "rack",
      value: { id: 4, name: "RK" },
    });

    expect(
      getDeepestLocationSelection(
        {
          room: { id: 1, name: "R" },
          device: { id: 2, name: "D" },
          shelf: { id: 3, name: "S" },
          rack: { id: 4, name: "RK" },
          box: { id: 5, name: "B1" },
        },
        { requireAssignable: true },
      ),
    ).toEqual({
      type: "box",
      value: { id: 5, name: "B1" },
    });
  });

  test("accepts room-only selection (room is an assignable level)", () => {
    expect(
      getDeepestLocationSelection(
        { room: { id: 1, name: "Main Lab" } },
        { requireAssignable: true },
      ),
    ).toEqual({
      type: "room",
      value: { id: 1, name: "Main Lab" },
    });
  });

  test("builds hierarchical path in level order", () => {
    expect(
      selectionToHierarchicalPath({
        room: { id: 1, name: "Main Lab" },
        device: { id: 2, name: "Freezer 1" },
        shelf: { id: 3, name: "Shelf A" },
      }),
    ).toBe("Main Lab > Freezer 1 > Shelf A");
  });

  test("maps text/grid position state to coordinate string", () => {
    expect(positionToCoordinate({ mode: "text", value: " A1 " })).toBe("A1");
    expect(positionToCoordinate({ mode: "grid", row: "B", column: "2" })).toBe(
      "B2",
    );
    expect(positionToCoordinate(null, { emptyValue: null })).toBeNull();
  });
});
