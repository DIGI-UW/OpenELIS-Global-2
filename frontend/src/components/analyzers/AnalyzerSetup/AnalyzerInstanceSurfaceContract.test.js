import fs from "fs";
import path from "path";

const appSource = fs.readFileSync(
  path.resolve(__dirname, "../../../App.jsx"),
  "utf8",
);
const listSource = fs.readFileSync(
  path.resolve(__dirname, "../AnalyzersList/AnalyzersList.tsx"),
  "utf8",
);

describe("analyzer instance surface cutover", () => {
  it("keeps analyzer creation and editing on the inline dashboard", () => {
    expect(appSource).not.toContain('path="/analyzers/new"');
    expect(appSource).not.toContain('path="/analyzers/:id/edit"');
    expect(appSource).not.toContain("AnalyzerFormPage");
    expect(fs.existsSync(path.resolve(__dirname, "../AnalyzerForm"))).toBe(
      false,
    );
  });

  it("keeps connection testing inline without a second modal", () => {
    expect(listSource).not.toContain("TestConnectionModal");
    expect(
      fs.existsSync(path.resolve(__dirname, "../TestConnectionModal")),
    ).toBe(false);
  });
});
