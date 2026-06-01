describe("SampleSelectionSection", () => {
  test("reference entry module exports default component", () => {
    const SampleSelectionSection = require("./SampleSelectionSection").default;
    expect(typeof SampleSelectionSection).toBe("function");
  });
});
