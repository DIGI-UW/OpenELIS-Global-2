/**
 * The content fields a label preset can carry (OGC-285 data-model §2.2,
 * OGC-1218). LAB_NUMBER is always present, required and first; the other 15
 * are selectable. The order here is the order the picker offers them in.
 */
export const LAB_NUMBER = "LAB_NUMBER";

export const FIELD_CATALOG = [
  { key: "LAB_NUMBER", labelId: "admin.labelPresets.fieldKey.LAB_NUMBER" },
  { key: "PATIENT_NAME", labelId: "admin.labelPresets.fieldKey.PATIENT_NAME" },
  { key: "PATIENT_ID", labelId: "admin.labelPresets.fieldKey.PATIENT_ID" },
  { key: "PATIENT_DOB", labelId: "admin.labelPresets.fieldKey.PATIENT_DOB" },
  { key: "PATIENT_SEX", labelId: "admin.labelPresets.fieldKey.PATIENT_SEX" },
  { key: "SITE_ID", labelId: "admin.labelPresets.fieldKey.SITE_ID" },
  {
    key: "COLLECTION_DATETIME",
    labelId: "admin.labelPresets.fieldKey.COLLECTION_DATETIME",
  },
  { key: "COLLECTED_BY", labelId: "admin.labelPresets.fieldKey.COLLECTED_BY" },
  { key: "TESTS", labelId: "admin.labelPresets.fieldKey.TESTS" },
  {
    key: "SPECIMEN_TYPE",
    labelId: "admin.labelPresets.fieldKey.SPECIMEN_TYPE",
  },
  { key: "BLOCK_ID", labelId: "admin.labelPresets.fieldKey.BLOCK_ID" },
  { key: "SLIDE_ID", labelId: "admin.labelPresets.fieldKey.SLIDE_ID" },
  { key: "STAIN_TYPE", labelId: "admin.labelPresets.fieldKey.STAIN_TYPE" },
  { key: "CASE_NUMBER", labelId: "admin.labelPresets.fieldKey.CASE_NUMBER" },
  {
    key: "STORAGE_LOCATION",
    labelId: "admin.labelPresets.fieldKey.STORAGE_LOCATION",
  },
  { key: "EXPIRY_DATE", labelId: "admin.labelPresets.fieldKey.EXPIRY_DATE" },
];

export const SELECTABLE_FIELD_KEYS = FIELD_CATALOG.map((f) => f.key).filter(
  (key) => key !== LAB_NUMBER,
);

export function fieldLabelId(key) {
  const entry = FIELD_CATALOG.find((f) => f.key === key);
  return entry ? entry.labelId : null;
}

/**
 * Puts the stored rows into editor order: Lab Number first and required, the
 * rest by display order, positions renumbered from 1 with no gaps. Unknown
 * keys are kept so a row the catalog does not know is never silently dropped.
 */
export function normalizeFields(fields) {
  const rows = Array.isArray(fields) ? fields : [];
  const others = rows
    .filter((f) => f && f.fieldKey && f.fieldKey !== LAB_NUMBER)
    .slice()
    .sort(
      (a, b) =>
        (a.displayOrder ?? Number.MAX_SAFE_INTEGER) -
        (b.displayOrder ?? Number.MAX_SAFE_INTEGER),
    )
    .map((f) => ({ fieldKey: f.fieldKey, isRequired: Boolean(f.isRequired) }));
  const ordered = [{ fieldKey: LAB_NUMBER, isRequired: true }, ...others];
  return ordered.map((f, index) => ({ ...f, displayOrder: index + 1 }));
}

export function addField(fields, key) {
  if (!key || key === LAB_NUMBER || fields.some((f) => f.fieldKey === key)) {
    return fields;
  }
  return normalizeFields([
    ...fields,
    { fieldKey: key, isRequired: false, displayOrder: fields.length + 1 },
  ]);
}

export function removeField(fields, key) {
  if (key === LAB_NUMBER) {
    return fields;
  }
  return normalizeFields(fields.filter((f) => f.fieldKey !== key));
}

/** Moves a selectable field one step; Lab Number and the edges stay put. */
export function moveField(fields, key, direction) {
  const index = fields.findIndex((f) => f.fieldKey === key);
  const target = index + direction;
  if (index <= 0 || target <= 0 || target >= fields.length) {
    return fields;
  }
  const next = fields.slice();
  [next[index], next[target]] = [next[target], next[index]];
  return normalizeFields(
    next.map((field, position) => ({ ...field, displayOrder: position + 1 })),
  );
}

export function toggleRequired(fields, key) {
  if (key === LAB_NUMBER) {
    return fields;
  }
  return fields.map((f) =>
    f.fieldKey === key ? { ...f, isRequired: !f.isRequired } : f,
  );
}

/**
 * Soft fit hint (OGC-1218): roughly how many text rows the configured height
 * leaves above the barcode. The renderer gives the barcode about a third of the
 * label and each text row about 3 mm, so a 25 mm label fits about five rows.
 * This never blocks a save; the real check belongs to the preview pane.
 */
export function estimateFittingRows(heightMm) {
  const height = Number(heightMm);
  if (!Number.isFinite(height) || height <= 0) {
    return null;
  }
  return Math.max(1, Math.floor((height * 0.66) / 3));
}

export function fieldsLikelyOverflow(fields, heightMm) {
  const rows = estimateFittingRows(heightMm);
  return rows != null && fields.length > rows;
}
