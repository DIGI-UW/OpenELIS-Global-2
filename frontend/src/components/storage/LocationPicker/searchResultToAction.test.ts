/**
 * Tests for the search-result → reducer-action mapper.
 *
 * The picker's flat search returns a single location node (Room, Device,
 * Shelf, Rack, or Box). Picking one must replace the entire selection
 * atomically — otherwise stale ancestors linger (e.g. Room A + Device B
 * after picking Device B out of Room B). This helper encodes that
 * contract in one place so all three picker consumers (Inline, Page,
 * Modal) stay consistent.
 */

import { searchResultToReplaceAction } from "./searchResultToAction";

describe("searchResultToReplaceAction", () => {
  it("maps a valid leaf result to a REPLACE_SELECTION action keyed by type", () => {
    const action = searchResultToReplaceAction({
      id: 5,
      type: "device",
      name: "Freezer 1",
    });
    expect(action).toEqual({
      type: "REPLACE_SELECTION",
      selection: { device: { id: 5, name: "Freezer 1" } },
    });
  });

  it("handles every valid level (room, device, shelf, rack, box)", () => {
    const levels = ["room", "device", "shelf", "rack", "box"];
    levels.forEach((level) => {
      const action = searchResultToReplaceAction({
        id: 1,
        type: level,
        name: "X",
      });
      expect(action.selection).toEqual({ [level]: { id: 1, name: "X" } });
    });
  });

  it("returns null when the result has an unknown type", () => {
    expect(
      searchResultToReplaceAction({ id: 1, type: "analyzer", name: "X" }),
    ).toBeNull();
  });

  it("returns null when the result has no type", () => {
    expect(searchResultToReplaceAction({ id: 1, name: "X" })).toBeNull();
  });

  it("returns null for null / undefined / non-object input", () => {
    expect(searchResultToReplaceAction(null)).toBeNull();
    expect(searchResultToReplaceAction(undefined)).toBeNull();
    expect(searchResultToReplaceAction("string")).toBeNull();
  });
});
