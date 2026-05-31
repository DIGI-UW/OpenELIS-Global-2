import {
  buildRetrievalItemsPayload,
  formatQuantityWithUnit,
  isQuantityWithinAvailable,
  parseQuantityValue,
  validateRetrievalQuantity,
} from "./biorepositoryQuantityHelpers";

describe("biorepositoryQuantityHelpers", () => {
  test("parseQuantityValue handles numeric strings with units", () => {
    expect(parseQuantityValue("5.0 mL")).toBe(5);
    expect(parseQuantityValue(2)).toBe(2);
    expect(parseQuantityValue("invalid")).toBeNull();
  });

  test("formatQuantityWithUnit renders quantity and unit", () => {
    expect(formatQuantityWithUnit(5, "mL")).toBe("5 mL");
    expect(formatQuantityWithUnit("2.5 mL", null)).toBe("2.5");
  });

  test("isQuantityWithinAvailable validates bounds", () => {
    expect(isQuantityWithinAvailable(3, 5)).toBe(true);
    expect(isQuantityWithinAvailable(6, 5)).toBe(false);
  });

  test("validateRetrievalQuantity reports invalid requested quantity", () => {
    const errors = validateRetrievalQuantity(
      { quantityRequested: 10, remainingQuantity: 5, id: 99 },
      99,
    );
    expect(errors.length).toBe(1);
    expect(errors[0]).toMatch(/exceeds available/);
  });

  test("buildRetrievalItemsPayload maps selected samples", () => {
    expect(
      buildRetrievalItemsPayload([
        { id: 7, quantityRequested: "2.5", unitOfMeasure: "mL" },
      ]),
    ).toEqual([
      {
        bioSampleId: 7,
        quantityRequested: 2.5,
        unitOfMeasure: "mL",
      },
    ]);
  });
});
