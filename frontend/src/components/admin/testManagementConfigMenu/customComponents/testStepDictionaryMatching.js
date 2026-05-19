/**
 * OGC-525 — pure helpers for hydrating Test Modify wizard dictionary state
 * from the backend payload on edit. Previously, the inline logic in
 * TestStepForm.jsx used `value.trim().split(" ")[0]` to compare initialData
 * entries against the master dictionaryList, which only worked for
 * single-token values; multi-token entries like "DENGUE VIRUS TYPE2
 * DETECTED" silently dropped because their first token ("DENGUE") never
 * matched the full master entry. Saving back persisted only the survivors.
 *
 * These helpers compare on the FULL normalized value (stripping an optional
 * trailing "qualifiable" sentinel that the legacy payload uses to indicate
 * qualified status).
 */

const QUALIFIABLE_SENTINEL = /\s*qualifiable\s*$/i;

export const normalizeDictionaryValue = (raw) => {
  if (!raw) return "";
  return raw.trim().replace(QUALIFIABLE_SENTINEL, "").trim();
};

export const isQualifiableMarker = (raw) =>
  !!raw && raw.toLowerCase().includes("qualifiable");

const extractRawValue = (entry) => {
  if (entry == null) return "";
  if (typeof entry === "string") return entry;
  return entry.value ?? "";
};

export const hydrateDictionaryFromInitial = (initial, dictionaryList) => {
  if (!Array.isArray(initial) || initial.length === 0) return [];
  if (!Array.isArray(dictionaryList) || dictionaryList.length === 0) return [];

  return initial
    .map((entry) => {
      const raw = extractRawValue(entry);
      const normalized = normalizeDictionaryValue(raw);
      if (!normalized) return null;

      const matched = dictionaryList.find(
        (dictItem) => dictItem.value.trim() === normalized,
      );
      if (!matched) return null;

      return {
        id: matched.id,
        value: matched.value,
        qualified: isQualifiableMarker(raw) ? "Y" : "N",
      };
    })
    .filter(Boolean);
};

export const resolveDictionaryItemId = (raw, dictionaryList) => {
  const normalized = normalizeDictionaryValue(raw);
  if (!normalized || !Array.isArray(dictionaryList)) return null;
  const matched = dictionaryList.find(
    (dictItem) => dictItem.value.trim() === normalized,
  );
  return matched ? matched.id : null;
};
