/**
 * The state an existing reflex rule renders in before its data has arrived.
 *
 * A saved rule renders once with its conditions already populated but with
 * `testResultList` still empty — the dictionary options and result types are
 * fetched afterwards. Reading `testResultList[index][conditionIndex]["type"]`
 * in that window threw "Cannot read properties of undefined (reading '0')",
 * and because the throw happened inside the fetch callback it took
 * buildSampleTests down with it ("...reading '26'", 26 being the sample type
 * whose tests were being mapped).
 *
 * These reproduce that window. The accessors must answer for a row that has
 * not loaded yet without inventing a type for it — a wrong type silently
 * changes which editor an existing condition gets.
 */

/** Dictionary options for a condition, or undefined before they arrive. */
const dictionaryResultsFor = (
  testResultList: any,
  index: number,
  itemIndex: number,
) => testResultList[index]?.[itemIndex]?.["list"];

/** The type the condition editor works against: the chosen component's. */
const conditionResultType = (
  testResultList: any,
  components: any[],
  condition: any,
  index: number,
  itemIndex: number,
) => {
  const component = components.find((c) => c.id === condition?.componentId);
  if (component?.resultType) {
    return component.resultType;
  }
  return testResultList[index]?.[itemIndex]?.type;
};

const COMPONENTS = [
  { id: "c-pcr", value: "Interpretation", resultType: "D" },
  { id: "c-ct", value: "Ct Value", resultType: "N" },
];

describe("reflex condition row before its data loads", () => {
  it("does not throw when the result list is still empty", () => {
    // The exact shape that produced "reading '0'".
    expect(() => dictionaryResultsFor({}, 0, 0)).not.toThrow();
    expect(dictionaryResultsFor({}, 0, 0)).toBeUndefined();
  });

  it("does not throw when only some rows have loaded", () => {
    const partiallyLoaded = { 0: { 0: { type: "N", list: [] } } };
    expect(() => dictionaryResultsFor(partiallyLoaded, 0, 1)).not.toThrow();
    expect(() => dictionaryResultsFor(partiallyLoaded, 1, 0)).not.toThrow();
    expect(dictionaryResultsFor(partiallyLoaded, 0, 0)).toEqual([]);
  });

  it("reports no type for a row that has not loaded, rather than guessing one", () => {
    expect(
      conditionResultType({}, [], { componentId: null }, 0, 0),
    ).toBeUndefined();
  });

  it("takes the type from the condition's component once one is chosen", () => {
    // A migrated legacy rule carries its test's primary component.
    expect(
      conditionResultType({}, COMPONENTS, { componentId: "c-pcr" }, 0, 0),
    ).toBe("D");
    // A rule authored against the numeric secondary must get the numeric
    // editor even though the test's primary is coded.
    expect(
      conditionResultType({}, COMPONENTS, { componentId: "c-ct" }, 0, 0),
    ).toBe("N");
  });

  it("falls back to the fetched row type when the rule names no component", () => {
    // A legacy rule whose component association has not been migrated yet.
    const loaded = { 0: { 0: { type: "N", list: [] } } };
    expect(
      conditionResultType(loaded, COMPONENTS, { componentId: "" }, 0, 0),
    ).toBe("N");
  });

  it("survives a condition object that is absent entirely", () => {
    expect(() =>
      conditionResultType({}, COMPONENTS, undefined, 0, 0),
    ).not.toThrow();
  });
});
