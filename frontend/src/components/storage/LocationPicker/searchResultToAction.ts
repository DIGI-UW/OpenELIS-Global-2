import { LEVEL_ORDER } from "./useLocationPicker";
import type {
  LocationLevel,
  ReplaceSelectionAction,
  StorageLocationOption,
} from "./types";

/**
 * Map a flat search result into a REPLACE_SELECTION action.
 *
 * The picker's search endpoint returns a single node whose `type` names
 * its level in the hierarchy (room | device | shelf | rack | box). The
 * pick must atomically replace the whole selection so a previous partial
 * selection (say, Room A chosen in create-mode) does not linger under a
 * newly-picked Device B whose parent is Room B. Returning null signals
 * "not a valid location result; ignore" so callers can guard trivially.
 *
 * Shared by LocationPickerInline, LocationPickerPage, and
 * LocationPickerModal. Centralising the mapping prevents drift between
 * the three shells as the search payload evolves.
 */
export function searchResultToReplaceAction(
  result: StorageLocationOption | null | undefined,
): ReplaceSelectionAction | null {
  if (!result || typeof result !== "object") return null;
  const { type, id, name } = result;
  if (!type || !LEVEL_ORDER.includes(type as LocationLevel)) return null;
  const level = type as LocationLevel;
  return {
    type: "REPLACE_SELECTION",
    selection: { [level]: { id, name } },
  };
}
