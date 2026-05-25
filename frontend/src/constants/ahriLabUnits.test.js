import { filterAhriLabUnitTestSections } from "./ahriLabUnits";

describe("filterAhriLabUnitTestSections", () => {
  it("keeps only AHRI research labs from display list", () => {
    const sections = [
      { id: "1", value: "Hematology" },
      { id: "168", value: "Bacteriology" },
      { id: "200", value: "Biorepository Laboratory" },
      { id: "59", value: "Immunology" },
    ];
    const filtered = filterAhriLabUnitTestSections(sections);
    expect(filtered.map((s) => s.value)).toEqual([
      "Bacteriology",
      "Biorepository Laboratory",
      "Immunology",
    ]);
  });
});
