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

const buildByIdIndex = (...lists) => {
  const byId = new Map();
  const walk = (node) => {
    if (Array.isArray(node)) {
      node.forEach(walk);
    } else if (node && node.id != null) {
      const key = String(node.id);
      if (!byId.has(key)) byId.set(key, node);
    }
  };
  lists.forEach(walk);
  return byId;
};

export const hydrateDictionaryFromInitial = (
  initial,
  dictionaryList,
  extraLookup = [],
) => {
  if (!Array.isArray(initial) || initial.length === 0) return [];

  const byId = buildByIdIndex(dictionaryList, extraLookup);
  const byValue = Array.isArray(dictionaryList) ? dictionaryList : [];

  return initial
    .map((entry) => {
      const raw = extractRawValue(entry);
      const qualified = isQualifiableMarker(raw) ? "Y" : "N";

      const id = entry && typeof entry === "object" ? entry.id : null;
      if (id != null && byId.has(String(id))) {
        const matched = byId.get(String(id));
        return { id: matched.id, value: matched.value, qualified };
      }

      const normalized = normalizeDictionaryValue(raw);
      if (!normalized) return null;
      const matched = byValue.find(
        (dictItem) => dictItem.value.trim() === normalized,
      );
      if (!matched) return null;
      return { id: matched.id, value: matched.value, qualified };
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
