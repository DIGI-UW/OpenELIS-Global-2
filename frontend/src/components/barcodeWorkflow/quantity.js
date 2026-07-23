// ---------------------------------------------------------------------------
// Shared label-quantity normalisation (OGC-285).
//
// Extracted from LabelsSection so the post-save / reprint dialog reuses exactly
// the same floor + clamp-to-max (decrease-only) logic the order-entry tables
// use. Keeping one implementation prevents the two surfaces from drifting.
// ---------------------------------------------------------------------------

// Coerce any input to a non-negative integer; null/blank/NaN/negative -> 0.
export const normalizeQuantity = (quantity) => {
  const num = Number(quantity);
  if (
    quantity === null ||
    quantity === undefined ||
    quantity === "" ||
    isNaN(num) ||
    num < 0
  ) {
    return 0;
  }
  return Math.floor(num);
};

// Clamp a chosen quantity to a ceiling (max <= 0 means "no ceiling").
export const clampToMax = (quantity, max) => {
  const normalized = normalizeQuantity(quantity);
  if (typeof max === "number" && max > 0 && normalized > max) {
    return max;
  }
  return normalized;
};
