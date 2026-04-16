/**
 * Phase 1a (RED) — Reducer tests for useLocationPicker.
 *
 * These tests assert every state transition of the picker's useReducer in
 * isolation. The reducer is a pure function — no DOM, no React, no API.
 * That's the point: by extracting all the picker state into one reducer,
 * we eliminate the family of bugs that came from useState bloat in the
 * legacy EnhancedCascadingMode.jsx (26 useStates, 14 useEffects, manual
 * locationUpdateTrigger re-render counter). State transitions are now
 * testable as data → data assertions.
 *
 * The plan's State + Action shapes (verbatim from /Users/pmanko/.claude/plans/foamy-sleeping-thacker.md):
 *
 *   type Container = { id: number; name: string };
 *   type Selection = { room?, device?, shelf?, rack?, box? };
 *   type Position =
 *     | { mode: 'text'; value: string }
 *     | { mode: 'grid'; row: string; column: string }
 *     | null;
 *   type PickerMode = 'search' | 'create';
 *   type State = {
 *     mode, selection, position, searchQuery, searchResults,
 *     initialAssignment, reason?, notes?, capacityWarning?
 *   };
 *
 * Actions:
 *   SET_LEVEL, SET_POSITION, SET_MODE, SET_SEARCH_QUERY,
 *   SET_SEARCH_RESULTS, SET_REASON, SET_NOTES, PRELOAD
 */

import { reducer, initialState, createInitialState } from "./useLocationPicker";

describe("useLocationPicker reducer", () => {
  describe("initial state", () => {
    it("starts in search mode with empty selection and null position", () => {
      expect(initialState.mode).toBe("search");
      expect(initialState.selection).toEqual({});
      expect(initialState.position).toBeNull();
      expect(initialState.searchQuery).toBe("");
      expect(initialState.searchResults).toEqual([]);
      expect(initialState.initialAssignment).toBeNull();
    });

    it("createInitialState merges in an initialAssignment for movement context", () => {
      const initialAssignment = {
        selection: {
          room: { id: 1, name: "Main Lab" },
          device: { id: 5, name: "Freezer 1" },
        },
        position: { mode: "text", value: "back left" },
      };
      const state = createInitialState({ initialAssignment });
      expect(state.initialAssignment).toEqual(initialAssignment);
      // The initial assignment also pre-populates the working selection so
      // the user sees their current location and can adjust from there.
      expect(state.selection).toEqual(initialAssignment.selection);
      expect(state.position).toEqual(initialAssignment.position);
    });
  });

  describe("SET_LEVEL", () => {
    it("sets a level value", () => {
      const next = reducer(initialState, {
        type: "SET_LEVEL",
        level: "room",
        value: { id: 1, name: "Main Lab" },
      });
      expect(next.selection.room).toEqual({ id: 1, name: "Main Lab" });
    });

    it("clears all descendant levels when a parent is set", () => {
      // Pre-fill room + device + shelf + rack + box
      const state = {
        ...initialState,
        selection: {
          room: { id: 1, name: "Main Lab" },
          device: { id: 5, name: "Freezer 1" },
          shelf: { id: 10, name: "Shelf A" },
          rack: { id: 50, name: "Rack 1" },
          box: { id: 200, name: "Box X" },
        },
      };
      // Change room — device/shelf/rack/box should clear because they were
      // children of the previous room.
      const next = reducer(state, {
        type: "SET_LEVEL",
        level: "room",
        value: { id: 2, name: "Other Lab" },
      });
      expect(next.selection.room).toEqual({ id: 2, name: "Other Lab" });
      expect(next.selection.device).toBeUndefined();
      expect(next.selection.shelf).toBeUndefined();
      expect(next.selection.rack).toBeUndefined();
      expect(next.selection.box).toBeUndefined();
    });

    it("clearing a level (value=undefined) also clears descendants", () => {
      const state = {
        ...initialState,
        selection: {
          room: { id: 1, name: "Main Lab" },
          device: { id: 5, name: "Freezer 1" },
          shelf: { id: 10, name: "Shelf A" },
        },
      };
      const next = reducer(state, {
        type: "SET_LEVEL",
        level: "device",
        value: undefined,
      });
      expect(next.selection.room).toEqual({ id: 1, name: "Main Lab" });
      expect(next.selection.device).toBeUndefined();
      expect(next.selection.shelf).toBeUndefined();
    });

    it("setting a deeper level keeps shallower selections intact", () => {
      const state = {
        ...initialState,
        selection: {
          room: { id: 1, name: "Main Lab" },
          device: { id: 5, name: "Freezer 1" },
        },
      };
      const next = reducer(state, {
        type: "SET_LEVEL",
        level: "shelf",
        value: { id: 10, name: "Shelf A" },
      });
      expect(next.selection.room).toEqual({ id: 1, name: "Main Lab" });
      expect(next.selection.device).toEqual({ id: 5, name: "Freezer 1" });
      expect(next.selection.shelf).toEqual({ id: 10, name: "Shelf A" });
    });
  });

  describe("SET_POSITION", () => {
    it("accepts a free-text position", () => {
      const next = reducer(initialState, {
        type: "SET_POSITION",
        position: { mode: "text", value: "back left corner" },
      });
      expect(next.position).toEqual({
        mode: "text",
        value: "back left corner",
      });
    });

    it("accepts a grid position (row × column)", () => {
      const next = reducer(initialState, {
        type: "SET_POSITION",
        position: { mode: "grid", row: "A", column: "1" },
      });
      expect(next.position).toEqual({ mode: "grid", row: "A", column: "1" });
    });

    it("accepts null to clear position", () => {
      const state = {
        ...initialState,
        position: { mode: "text", value: "anywhere" },
      };
      const next = reducer(state, { type: "SET_POSITION", position: null });
      expect(next.position).toBeNull();
    });
  });

  describe("SET_MODE", () => {
    it("toggles between 'search' and 'create'", () => {
      const after = reducer(initialState, {
        type: "SET_MODE",
        mode: "create",
      });
      expect(after.mode).toBe("create");
      const back = reducer(after, { type: "SET_MODE", mode: "search" });
      expect(back.mode).toBe("search");
    });

    it("preserves selection across mode change", () => {
      const state = {
        ...initialState,
        selection: { room: { id: 1, name: "Main Lab" } },
        position: { mode: "text", value: "x" },
      };
      const next = reducer(state, { type: "SET_MODE", mode: "create" });
      expect(next.selection.room).toEqual({ id: 1, name: "Main Lab" });
      expect(next.position).toEqual({ mode: "text", value: "x" });
    });
  });

  describe("SET_SEARCH_QUERY and SET_SEARCH_RESULTS", () => {
    it("updates the search query without touching results", () => {
      const next = reducer(initialState, {
        type: "SET_SEARCH_QUERY",
        query: "Freezer",
      });
      expect(next.searchQuery).toBe("Freezer");
      expect(next.searchResults).toEqual([]);
    });

    it("updates search results", () => {
      const results = [
        {
          id: 5,
          type: "device",
          name: "Freezer 1",
          hierarchicalPath: "Main Lab > Freezer 1",
        },
      ];
      const next = reducer(initialState, {
        type: "SET_SEARCH_RESULTS",
        results,
      });
      expect(next.searchResults).toEqual(results);
    });

    it("clearing the query also clears results (avoid stale results in dropdown)", () => {
      const state = {
        ...initialState,
        searchQuery: "Free",
        searchResults: [{ id: 5, type: "device", name: "Freezer 1" }],
      };
      const next = reducer(state, { type: "SET_SEARCH_QUERY", query: "" });
      expect(next.searchQuery).toBe("");
      expect(next.searchResults).toEqual([]);
    });
  });

  describe("SET_REASON and SET_NOTES (movement context)", () => {
    it("sets reason text", () => {
      const next = reducer(initialState, {
        type: "SET_REASON",
        reason: "Moving for testing",
      });
      expect(next.reason).toBe("Moving for testing");
    });

    it("sets notes text", () => {
      const next = reducer(initialState, {
        type: "SET_NOTES",
        notes: "Sample is fragile",
      });
      expect(next.notes).toBe("Sample is fragile");
    });
  });

  describe("PRELOAD (barcode auto-open)", () => {
    it("loads a full selection + position in one shot", () => {
      const next = reducer(initialState, {
        type: "PRELOAD",
        selection: {
          room: { id: 1, name: "Main Lab" },
          device: { id: 5, name: "Freezer 1" },
          shelf: { id: 10, name: "Shelf A" },
        },
        position: { mode: "grid", row: "B", column: "3" },
      });
      expect(next.selection.room).toEqual({ id: 1, name: "Main Lab" });
      expect(next.selection.device).toEqual({ id: 5, name: "Freezer 1" });
      expect(next.selection.shelf).toEqual({ id: 10, name: "Shelf A" });
      expect(next.position).toEqual({ mode: "grid", row: "B", column: "3" });
    });

    it("PRELOAD with empty selection + null position resets selection", () => {
      const state = {
        ...initialState,
        selection: { room: { id: 1, name: "Main Lab" } },
        position: { mode: "text", value: "x" },
      };
      const next = reducer(state, {
        type: "PRELOAD",
        selection: {},
        position: null,
      });
      expect(next.selection).toEqual({});
      expect(next.position).toBeNull();
    });
  });

  describe("immutability", () => {
    it("never mutates the previous state object", () => {
      const before = {
        ...initialState,
        selection: { room: { id: 1, name: "Main Lab" } },
      };
      const beforeSnapshot = JSON.parse(JSON.stringify(before));
      reducer(before, {
        type: "SET_LEVEL",
        level: "device",
        value: { id: 5, name: "Freezer 1" },
      });
      expect(before).toEqual(beforeSnapshot);
    });
  });
});
