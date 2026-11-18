// LESOTHO: which request form an order's tests bring up.
import { describe, expect, it } from "vitest";
import { formsForTests } from "./testForms";

describe("formsForTests", () => {
  it("links HIV DNA PCR to the DNA-PCR form, whatever its spacing or case", () => {
    expect(formsForTests([{ name: "HIV DNA PCR" }])).toEqual(["DNA_PCR"]);
    expect(formsForTests([{ name: "hiv dna-pcr" }])).toEqual(["DNA_PCR"]);
  });

  it("links a cross-match to the blood transfusion request, however it is spelt", () => {
    expect(formsForTests([{ name: "Crossmatch" }])).toEqual([
      "BLOOD_TRANSFUSION",
    ]);
    expect(formsForTests([{ name: "Cross match" }])).toEqual([
      "BLOOD_TRANSFUSION",
    ]);
    expect(formsForTests([{ name: "Cross-match" }])).toEqual([
      "BLOOD_TRANSFUSION",
    ]);
  });

  it("does not bring up the transfusion request for a blood group or antibody screen alone", () => {
    expect(
      formsForTests([
        { name: "ABO Blood Group" },
        { name: "Rhesus (D) typing" },
        { name: "Antibody screening" },
        { name: "Direct Coombs (DAT)" },
      ]),
    ).toEqual([]);
  });

  it("gives both forms when both tests are ordered", () => {
    expect(
      formsForTests([{ name: "Crossmatch" }, { name: "HIV DNA PCR" }]),
    ).toEqual(["DNA_PCR", "BLOOD_TRANSFUSION"]);
  });

  it("gives no form for tests that are not linked yet", () => {
    expect(
      formsForTests([
        { name: "HIV VIRAL LOAD" },
        { name: "Hemoglobin" },
        { name: "HIV rapid test HIV" },
      ]),
    ).toEqual([]);
    expect(formsForTests([])).toEqual([]);
    expect(formsForTests(undefined)).toEqual([]);
  });

  it("lists the form once when several of its tests are ordered", () => {
    expect(
      formsForTests([{ name: "HIV DNA PCR" }, { name: "HIV-1 DNA PCR" }]),
    ).toEqual(["DNA_PCR"]);
  });
});
