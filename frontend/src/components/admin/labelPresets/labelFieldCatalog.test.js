import {
  LAB_NUMBER,
  SELECTABLE_FIELD_KEYS,
  addField,
  estimateFittingRows,
  fieldLabelId,
  fieldsLikelyOverflow,
  moveField,
  normalizeFields,
  removeField,
  toggleRequired,
} from "./labelFieldCatalog";

const keys = (fields) => fields.map((f) => f.fieldKey);
const positions = (fields) => fields.map((f) => f.displayOrder);

describe("labelFieldCatalog (OGC-1218)", () => {
  test("offers fifteen selectable fields besides Lab Number, each with a label key", () => {
    expect(SELECTABLE_FIELD_KEYS).toHaveLength(15);
    expect(SELECTABLE_FIELD_KEYS).not.toContain(LAB_NUMBER);
    expect(fieldLabelId("PATIENT_DOB")).toBe(
      "admin.labelPresets.fieldKey.PATIENT_DOB",
    );
    expect(fieldLabelId("NOT_A_FIELD")).toBeNull();
  });

  test("normalizeFields puts Lab Number first and required and renumbers the rest in display order", () => {
    const stored = [
      { fieldKey: "TESTS", isRequired: 1, displayOrder: 9 },
      { fieldKey: "LAB_NUMBER", isRequired: false, displayOrder: 4 },
      { fieldKey: "PATIENT_NAME", displayOrder: 2 },
    ];
    const fields = normalizeFields(stored);
    expect(keys(fields)).toEqual(["LAB_NUMBER", "PATIENT_NAME", "TESTS"]);
    expect(positions(fields)).toEqual([1, 2, 3]);
    expect(fields[0].isRequired).toBe(true);
    expect(fields[1].isRequired).toBe(false);
    expect(fields[2].isRequired).toBe(true);
    expect(keys(normalizeFields([]))).toEqual(["LAB_NUMBER"]);
    expect(keys(normalizeFields(undefined))).toEqual(["LAB_NUMBER"]);
  });

  test("addField appends at the next position and ignores duplicates and Lab Number", () => {
    const start = normalizeFields([]);
    const withName = addField(start, "PATIENT_NAME");
    expect(keys(withName)).toEqual(["LAB_NUMBER", "PATIENT_NAME"]);
    expect(positions(withName)).toEqual([1, 2]);
    expect(addField(withName, "PATIENT_NAME")).toBe(withName);
    expect(addField(withName, LAB_NUMBER)).toBe(withName);
    expect(addField(withName, "")).toBe(withName);
  });

  test("moveField swaps neighbours, never moves Lab Number and stops at the edges", () => {
    const fields = normalizeFields([
      { fieldKey: "PATIENT_NAME", displayOrder: 2 },
      { fieldKey: "PATIENT_ID", displayOrder: 3 },
      { fieldKey: "TESTS", displayOrder: 4 },
    ]);
    expect(keys(moveField(fields, "PATIENT_ID", -1))).toEqual([
      "LAB_NUMBER",
      "PATIENT_ID",
      "PATIENT_NAME",
      "TESTS",
    ]);
    expect(positions(moveField(fields, "PATIENT_ID", -1))).toEqual([
      1, 2, 3, 4,
    ]);
    expect(keys(moveField(fields, "PATIENT_NAME", 1))).toEqual([
      "LAB_NUMBER",
      "PATIENT_ID",
      "PATIENT_NAME",
      "TESTS",
    ]);
    expect(moveField(fields, "PATIENT_NAME", -1)).toBe(fields);
    expect(moveField(fields, "TESTS", 1)).toBe(fields);
    expect(moveField(fields, LAB_NUMBER, 1)).toBe(fields);
  });

  test("removeField drops a selectable field and renumbers, but never Lab Number", () => {
    const fields = normalizeFields([
      { fieldKey: "PATIENT_NAME", displayOrder: 2 },
      { fieldKey: "TESTS", displayOrder: 3 },
    ]);
    const without = removeField(fields, "PATIENT_NAME");
    expect(keys(without)).toEqual(["LAB_NUMBER", "TESTS"]);
    expect(positions(without)).toEqual([1, 2]);
    expect(removeField(fields, LAB_NUMBER)).toBe(fields);
  });

  test("toggleRequired flips a selectable field and leaves Lab Number required", () => {
    const fields = normalizeFields([{ fieldKey: "TESTS", displayOrder: 2 }]);
    expect(toggleRequired(fields, "TESTS")[1].isRequired).toBe(true);
    expect(toggleRequired(fields, LAB_NUMBER)).toBe(fields);
  });

  test("the fit hint is a soft estimate from the height", () => {
    expect(estimateFittingRows(25)).toBe(5);
    expect(estimateFittingRows(15)).toBe(3);
    expect(estimateFittingRows("")).toBeNull();
    const six = normalizeFields(
      ["PATIENT_NAME", "PATIENT_ID", "PATIENT_DOB", "TESTS", "SITE_ID"].map(
        (fieldKey, index) => ({ fieldKey, displayOrder: index + 2 }),
      ),
    );
    expect(six).toHaveLength(6);
    expect(fieldsLikelyOverflow(six, 15)).toBe(true);
    expect(fieldsLikelyOverflow(six, 25)).toBe(true);
    expect(fieldsLikelyOverflow(six, 40)).toBe(false);
    expect(fieldsLikelyOverflow(normalizeFields([]), 10)).toBe(false);
  });
});
