import { describe, expect, it } from "vitest";
import { scaleToOne } from "./weightsScaling";

describe("scaleToOne", () => {
  it("scales equal weights to equal shares of 1.00", () => {
    expect(
      scaleToOne({ qc: 0.5, eqa: 0.5, nceInverse: 0.5, amendmentInverse: 0.5 }),
    ).toEqual({
      qc: 0.25,
      eqa: 0.25,
      nceInverse: 0.25,
      amendmentInverse: 0.25,
    });
  });

  it("gives the rounding remainder to the largest weight", () => {
    const scaled = scaleToOne({ a: 1, b: 1, c: 1 });
    expect(scaled).toEqual({ a: 0.34, b: 0.33, c: 0.33 });
  });

  it("cannot scale weights that are all zero", () => {
    expect(scaleToOne({ a: 0, b: 0 })).toBeNull();
  });
});
