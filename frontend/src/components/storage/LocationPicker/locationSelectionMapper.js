import { LEVEL_ORDER } from "./useLocationPicker";

export const ASSIGNABLE_LEVELS = ["room", "device", "shelf", "rack", "box"];

export function selectionToHierarchicalPath(selection = {}) {
  return LEVEL_ORDER.map((level) => selection[level]?.name)
    .filter(Boolean)
    .join(" > ");
}

export function getDeepestLocationSelection(
  selection = {},
  { requireAssignable = false } = {},
) {
  let deepest = null;

  LEVEL_ORDER.forEach((level) => {
    const candidate = selection[level];
    if (candidate?.id) {
      deepest = { type: level, value: candidate };
    }
  });

  if (!deepest) {
    return null;
  }

  if (requireAssignable && !ASSIGNABLE_LEVELS.includes(deepest.type)) {
    return null;
  }

  return deepest;
}

export function positionToCoordinate(position, { emptyValue = "" } = {}) {
  if (!position) return emptyValue;

  if (position.mode === "text") {
    return (position.value || "").trim() || emptyValue;
  }

  if (position.mode === "grid") {
    const row = (position.row || "").toString().trim();
    const col = (position.column || "").toString().trim();
    return row + col || emptyValue;
  }

  return emptyValue;
}
