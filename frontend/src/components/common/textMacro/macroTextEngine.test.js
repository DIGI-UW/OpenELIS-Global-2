import {
  expandMacroToken,
  filterMacroSuggestions,
  getMacroToken,
} from "./macroTextEngine";

const macros = [
  { code: ".gpc", expansionText: "Gram-positive cocci" },
  { code: ".ng24", expansionText: "No growth at 24 hours" },
];

describe("macro text engine", () => {
  it("finds only a dot token at the caret after a text boundary", () => {
    expect(getMacroToken("Observed .gp later", 12)).toEqual({
      token: ".gp",
      start: 9,
      end: 12,
    });
    expect(getMacroToken("email@example.org", 17)).toBeNull();
  });

  it("matches codes and expansion text case-insensitively", () => {
    expect(filterMacroSuggestions(macros, ".NG")).toEqual([macros[1]]);
    expect(filterMacroSuggestions(macros, ".cocci")).toEqual([macros[0]]);
  });

  it("replaces only the active token and returns the restored caret", () => {
    expect(
      expandMacroToken(
        "Before .gpc after",
        { token: ".gpc", start: 7, end: 11 },
        macros[0],
        " ",
      ),
    ).toEqual({
      value: "Before Gram-positive cocci after",
      caret: 26,
    });
  });
});
