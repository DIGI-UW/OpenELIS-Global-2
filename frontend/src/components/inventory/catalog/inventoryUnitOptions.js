/** Default units when UOM API returns empty (matches legacy catalog fallbacks). */
const DEFAULT_UNIT_OPTIONS = [
  { id: "mL", text: "mL" },
  { id: "tests", text: "tests" },
  { id: "kits", text: "kits" },
  { id: "cartridges", text: "cartridges" },
  { id: "each", text: "each" },
  { id: "units", text: "units" },
];

const ADD_NEW_UNIT_OPTION = { id: "__add_new__", text: "Add new unit..." };

/**
 * Normalize /rest/UomCreate existingUomList (IdValuePair: id + value) for Carbon dropdowns.
 */
export const formatUnitOptionsFromUomResponse = (response) => {
  const raw = response?.existingUomList;
  if (!Array.isArray(raw) || raw.length === 0) {
    return [...DEFAULT_UNIT_OPTIONS, ADD_NEW_UNIT_OPTION];
  }

  const units = [];
  const seen = new Set();

  raw.forEach((unit) => {
    const text = String(
      unit?.value ?? unit?.unitOfMeasureName ?? unit?.text ?? "",
    ).trim();
    if (!text) {
      return;
    }
    const id = String(unit?.id ?? text).trim();
    const key = `${id}::${text}`;
    if (seen.has(key)) {
      return;
    }
    seen.add(key);
    units.push({ id, text });
  });

  if (units.length === 0) {
    return [...DEFAULT_UNIT_OPTIONS, ADD_NEW_UNIT_OPTION];
  }

  return [...units, ADD_NEW_UNIT_OPTION];
};

/** Equipment catalog items use a fixed unit label; not shown in the form. */
export const DEFAULT_EQUIPMENT_UNIT = "each";
