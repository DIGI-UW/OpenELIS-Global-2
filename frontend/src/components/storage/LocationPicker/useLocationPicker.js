import { useReducer } from "react";

/**
 * Reducer + hook for the storage location picker.
 *
 * State shape:
 *   Container  = { id, name }
 *   Selection  = { room?, device?, shelf?, rack?, box? }
 *   Position   = { mode: 'text', value }
 *              | { mode: 'grid', row, column }
 *              | null
 *   PickerMode = 'search' | 'create'
 *
 *   State { mode, selection, position, searchQuery, searchResults,
 *           initialAssignment, reason, notes, capacityWarning }
 *
 * Position is a property of the assignment, not a hierarchy level;
 * it is always optional. Users toggle between free text and row × column.
 *
 * Cascading invariant: setting or clearing a level clears all descendant
 * levels (set room → device/shelf/rack/box cleared). Applied uniformly in
 * the reducer so SET_LEVEL is the only place the rule lives.
 */

// Children come after parents. SET_LEVEL uses this to clear descendants.
export const LEVEL_ORDER = ["room", "device", "shelf", "rack", "box"];

export const initialState = {
  mode: "search",
  selection: {},
  position: null,
  searchQuery: "",
  searchResults: [],
  initialAssignment: null,
};

/**
 * Build an initial state, optionally seeded with an existing assignment
 * (movement context — the modal opens with the current location pre-
 * filled so the user can see and adjust it).
 */
export function createInitialState({ initialAssignment } = {}) {
  if (!initialAssignment) {
    return initialState;
  }
  return {
    ...initialState,
    initialAssignment,
    selection: { ...initialAssignment.selection },
    position: initialAssignment.position,
  };
}

/**
 * Pure reducer. Every state transition flows through here so they can be
 * tested as data → data.
 */
export function reducer(state, action) {
  switch (action.type) {
    case "SET_LEVEL": {
      const { level, value } = action;
      const idx = LEVEL_ORDER.indexOf(level);
      if (idx === -1) return state;

      // Build a fresh selection: keep ancestors, set this level (or omit if
      // value is undefined), drop all descendants.
      const nextSelection = {};
      for (let i = 0; i < idx; i++) {
        const k = LEVEL_ORDER[i];
        if (state.selection[k] !== undefined) {
          nextSelection[k] = state.selection[k];
        }
      }
      if (value !== undefined) {
        nextSelection[level] = value;
      }
      return { ...state, selection: nextSelection };
    }

    case "SET_POSITION":
      return { ...state, position: action.position };

    case "SET_MODE":
      return { ...state, mode: action.mode };

    case "SET_SEARCH_QUERY":
      // Clearing the query also clears results so a stale list doesn't
      // linger in the dropdown after the user empties the input.
      if (!action.query) {
        return { ...state, searchQuery: "", searchResults: [] };
      }
      return { ...state, searchQuery: action.query };

    case "SET_SEARCH_RESULTS":
      return { ...state, searchResults: action.results };

    case "SET_REASON":
      return { ...state, reason: action.reason };

    case "SET_NOTES":
      return { ...state, notes: action.notes };

    case "PRELOAD":
      return {
        ...state,
        selection: { ...action.selection },
        position: action.position,
      };

    default:
      return state;
  }
}

/**
 * React hook wrapping the reducer. Components consume `state` and dispatch
 * actions; no other state should live in the picker components.
 *
 * Optional `init.initialAssignment` seeds the state with an existing
 * assignment (used by the modal's movement-mode opening).
 */
export default function useLocationPicker(init = {}) {
  const [state, dispatch] = useReducer(reducer, init, createInitialState);
  return [state, dispatch];
}
