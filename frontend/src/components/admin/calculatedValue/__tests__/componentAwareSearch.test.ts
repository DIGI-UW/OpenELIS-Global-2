/**
 * Which tests a numeric rule may use, and which parts of them.
 *
 * A test does not have one result type any more. Filtering the search on the
 * test's own type reads the primary component's, so COVID-19 PCR — a coded
 * interpretation beside two numeric Ct values — never appeared when
 * configuring a numeric calculation, and there was no way to reach its Ct
 * value at all.
 *
 * These are the two decisions the rule builders make, kept apart deliberately:
 * whether a test belongs in the search, and which of its components may then
 * be chosen.
 */

/** A test is usable where a number is required if any component reports one. */
const hasNumericComponent = (test: any) =>
  Array.isArray(test?.resultTypes) && test.resultTypes.length
    ? test.resultTypes.includes("N")
    : test?.resultType === "N";

/** Only the components reporting a number may be chosen. */
const numericComponents = (test: any) =>
  (test?.components || [])
    .filter((c: any) => (c.resultType || test?.resultType) === "N")
    .map((c: any) => ({ id: c.id, value: c.value }));

const covid = {
  id: "300",
  value: "COVID-19 PCR",
  resultType: "D",
  resultTypes: ["D", "N"],
  components: [
    { id: "c-pcr", value: "Interpretation", resultType: "D", primary: true },
    { id: "c-vl", value: "Viral Load", resultType: "N", primary: false },
    { id: "c-ct", value: "Ct Value", resultType: "N", primary: false },
  ],
};

const glucose = {
  id: "1",
  value: "Glucose",
  resultType: "N",
  resultTypes: ["N"],
  components: [{ id: "g1", value: "Glucose", resultType: "N", primary: true }],
};

const hiv = {
  id: "2",
  value: "HIV",
  resultType: "D",
  resultTypes: ["D"],
  components: [{ id: "h1", value: "HIV", resultType: "D", primary: true }],
};

describe("component-aware numeric search", () => {
  it("finds a single-component numeric test", () => {
    expect(hasNumericComponent(glucose)).toBe(true);
  });

  it("does not find a single-component non-numeric test", () => {
    expect(hasNumericComponent(hiv)).toBe(false);
  });

  it("finds a test whose numeric components sit under a coded primary", () => {
    expect(hasNumericComponent(covid)).toBe(true);
  });

  it("finds a test whose primary is numeric and secondary is text", () => {
    expect(
      hasNumericComponent({
        resultType: "N",
        resultTypes: ["N", "A"],
        components: [
          { id: "a", value: "Count", resultType: "N" },
          { id: "b", value: "Notes", resultType: "A" },
        ],
      }),
    ).toBe(true);
  });

  it("falls back to the test's own type when it reports no components", () => {
    // Every test looked like this before components existed.
    expect(hasNumericComponent({ resultType: "N" })).toBe(true);
    expect(hasNumericComponent({ resultType: "D" })).toBe(false);
  });
});

describe("component-aware selection", () => {
  it("offers only the numeric components of a mixed test", () => {
    expect(numericComponents(covid).map((c: any) => c.value)).toEqual([
      "Viral Load",
      "Ct Value",
    ]);
  });

  it("never offers the coded primary where a number is required", () => {
    expect(numericComponents(covid).map((c: any) => c.id)).not.toContain(
      "c-pcr",
    );
  });

  it("offers the only component of a single-component numeric test", () => {
    expect(numericComponents(glucose).map((c: any) => c.id)).toEqual(["g1"]);
  });

  it("offers nothing for a test with no numeric component", () => {
    expect(numericComponents(hiv)).toEqual([]);
  });

  it("treats a component with no declared type as reporting the test's", () => {
    // A legacy single-component test: the component inherits the test's type.
    const legacy = {
      resultType: "N",
      components: [{ id: "l1", value: "Legacy", resultType: null }],
    };
    expect(numericComponents(legacy).map((c: any) => c.id)).toEqual(["l1"]);
  });
});
