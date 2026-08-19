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

  it("falls back to `label` when the result has no `name`", () => {
    const action = searchResultToReplaceAction({
      id: 9,
      type: "shelf",
      label: "Shelf A",
    });
    expect(action.selection.shelf).toEqual({ id: 9, name: "Shelf A" });
  });

  describe("ancestor chain", () => {
    it("fills in the room when a device is picked", () => {
      const action = searchResultToReplaceAction({
        id: 5,
        type: "device",
        name: "Freezer 1",
        parentRoomId: 1,
        parentRoomName: "Main Lab",
      });
      expect(action.selection).toEqual({
        room: { id: 1, name: "Main Lab" },
        device: { id: 5, name: "Freezer 1" },
      });
    });

    it("fills in room and device when a shelf is picked", () => {
      const action = searchResultToReplaceAction({
        id: 9,
        type: "shelf",
        label: "Shelf A",
        parentDeviceId: 5,
        parentDeviceName: "Freezer 1",
        parentRoomId: 1,
        parentRoomName: "Main Lab",
      });
      expect(action.selection).toEqual({
        room: { id: 1, name: "Main Lab" },
        device: { id: 5, name: "Freezer 1" },
        shelf: { id: 9, name: "Shelf A" },
      });
    });

    it("fills in the full chain when a rack is picked", () => {
      const action = searchResultToReplaceAction({
        id: 12,
        type: "rack",
        label: "Rack 3",
        parentShelfId: 9,
        parentShelfLabel: "Shelf A",
        parentDeviceId: 5,
        parentDeviceName: "Freezer 1",
        parentRoomId: 1,
        parentRoomName: "Main Lab",
      });
      expect(action.selection).toEqual({
        room: { id: 1, name: "Main Lab" },
        device: { id: 5, name: "Freezer 1" },
        shelf: { id: 9, name: "Shelf A" },
        rack: { id: 12, name: "Rack 3" },
      });
    });

    it("accepts the bare `roomName` / `deviceName` spelling too", () => {
      const action = searchResultToReplaceAction({
        id: 9,
        type: "shelf",
        label: "Shelf A",
        parentDeviceId: 5,
        deviceName: "Freezer 1",
        parentRoomId: 1,
        roomName: "Main Lab",
      });
      expect(action.selection.device).toEqual({ id: 5, name: "Freezer 1" });
      expect(action.selection.room).toEqual({ id: 1, name: "Main Lab" });
    });

    it("stops at the first missing ancestor so the chain stays contiguous", () => {
      const action = searchResultToReplaceAction({
        id: 12,
        type: "rack",
        label: "Rack 3",
        parentRoomId: 1,
        parentRoomName: "Main Lab",
      });
      expect(action.selection).toEqual({ rack: { id: 12, name: "Rack 3" } });
    });

    it("fills in the full four-level chain when a box is picked", () => {
      const action = searchResultToReplaceAction({
        id: 40,
        type: "box",
        label: "Box Alpha",
        code: "BX-001",
        boxType: "96-well",
        locationType: "box",
        parentRackId: 30,
        rackLabel: "Rack R1",
        parentShelfId: 20,
        shelfLabel: "Shelf-A",
        parentDeviceId: 10,
        deviceName: "Main Freezer",
        parentRoomId: 1,
        roomName: "Main Laboratory",
      });
      expect(action.selection).toEqual({
        room: { id: 1, name: "Main Laboratory" },
        device: { id: 10, name: "Main Freezer" },
        shelf: { id: 20, name: "Shelf-A" },
        rack: { id: 30, name: "Rack R1" },
        box: { id: 40, name: "Box Alpha" },
      });
    });

    it("leaves a room result with no ancestors", () => {
      const action = searchResultToReplaceAction({
        id: 1,
        type: "room",
        name: "Main Lab",
      });
      expect(action.selection).toEqual({ room: { id: 1, name: "Main Lab" } });
    });
  });
});
