// LESOTHO: which tests bring up which program's questionnaire.
import { describe, expect, it } from "vitest";
import { triggeredProgramCodes } from "./programTriggers";

describe("triggeredProgramCodes", () => {
  it("matches HIV viral load tests by name, whatever the spacing or case", () => {
    expect(triggeredProgramCodes([{ id: "174", name: "Viral Load" }])).toEqual([
      "HIV_VL",
    ]);
    expect(
      triggeredProgramCodes([{ id: "380", name: "HIV-1 Viral Load" }]),
    ).toEqual(["HIV_VL"]);
    expect(
      triggeredProgramCodes([{ id: "x", name: "hiv 1 viral   load" }]),
    ).toEqual(["HIV_VL"]);
  });

  it("matches the French viral load test", () => {
    expect(
      triggeredProgramCodes([
        { id: "37", name: "Mesure de la charge virale(Whole Blood)" },
      ]),
    ).toEqual(["HIV_VL"]);
  });

  it("matches by LOINC even when the name does not say viral load", () => {
    expect(
      triggeredProgramCodes([
        { id: "9", name: "Custom assay", loinc: "20447-9" },
      ]),
    ).toEqual(["HIV_VL"]);
    expect(
      triggeredProgramCodes([
        { id: "314", name: "HIVVIRALLOAD(Serum)", loinc: "10351-5" },
      ]),
    ).toEqual(["HIV_VL"]);
  });

  it("does not match by database id", () => {
    expect(triggeredProgramCodes([{ id: "37", name: "Hemoglobin" }])).toEqual(
      [],
    );
  });

  it("ignores hepatitis and infant viral load tests", () => {
    expect(
      triggeredProgramCodes([
        { id: "317", name: "HEPATITISCVIRALLOAD(Serum)" },
        { id: "319", name: "HEPATITISBVIRALLOAD(Plasma)" },
        { id: "311", name: "HIVINFANTVIRALLOAD(Serum)" },
      ]),
    ).toEqual([]);
  });

  it("returns each program once, however many of its tests are ordered", () => {
    expect(
      triggeredProgramCodes([
        { id: "174", name: "Viral Load" },
        { id: "380", name: "HIV-1 Viral Load", loinc: "20447-9" },
      ]),
    ).toEqual(["HIV_VL"]);
  });

  it("returns nothing for an order with no trigger tests", () => {
    expect(triggeredProgramCodes([])).toEqual([]);
    expect(triggeredProgramCodes()).toEqual([]);
    expect(
      triggeredProgramCodes([{ id: "1", name: "Crossmatch" }, "174", null]),
    ).toEqual([]);
  });
});
