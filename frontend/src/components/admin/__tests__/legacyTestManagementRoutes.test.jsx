import fs from "fs";
import path from "path";

/**
 * The legacy test-management pages are gone. Their addresses survive as
 * redirects to the editor that replaced each one, so a bookmark or an old
 * link still lands somewhere useful, and nothing under
 * testManagementConfigMenu/ is routed any more except the menu page and the
 * pages that have no replacement: result select lists, bulk test activation and
 * orderability, panel order, and View Test Catalog.
 *
 * This reads the source rather than rendering: Admin pulls in the whole admin
 * route tree, and what is being guarded is the wiring.
 */
const read = (relative) =>
  fs.readFileSync(path.join(__dirname, "..", relative), "utf8");

const admin = read("Admin.jsx");

/** The redirect target Admin gives a legacy path, or null when it has none. */
const redirectFor = (legacyPath) => {
  const escaped = legacyPath.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
  const pattern = new RegExp(
    "`\\$\\{path\\}/" +
      escaped +
      "`[\\s\\S]{0,400}?<Redirect to=\\{`\\$\\{path\\}/([^`]+)`\\} />",
  );
  const match = admin.match(pattern);
  return match && match[1];
};

describe("legacy Test Management addresses redirect to the new editors", () => {
  it.each([
    ["TestAdd", "TestCatalogEditor/new/basic-info"],
    ["TestModifyEntry", "TestCatalogList"],
    ["TestRenameEntry", "TestCatalogList"],
    ["UomManagement", "UnitsOfMeasure"],
    ["UomCreate", "UnitsOfMeasure"],
    ["UomRenameEntry", "UnitsOfMeasure"],
    ["PanelManagement", "TestCatalogList?entity=panels"],
    ["PanelCreate", "TestCatalogList?entity=panels"],
    ["PanelRenameEntry", "TestCatalogList?entity=panels"],
    ["SampleTypeManagement", "SampleTypeEditor"],
    ["SampleTypeRenameEntry", "SampleTypeEditor"],
    ["TestSectionManagement", "LabUnitManagement"],
    ["TestSectionRenameEntry", "LabUnitManagement"],
    ["MethodCreate", "MethodManagement"],
    ["MethodRenameEntry", "MethodManagement"],
  ])("%s opens %s", (legacyPath, target) => {
    expect(redirectFor(legacyPath)).toBe(target);
  });

  it("serves View Test Catalog at its own address rather than redirecting it", () => {
    expect(admin).toMatch(
      /<Route exact path=\{`\$\{path\}\/TestCatalog`\} component=\{ViewTestCatalog\} \/>/,
    );
  });

  it("routes from the legacy folder only the pages that have no replacement", () => {
    const imports = [
      ...admin.matchAll(/from "\.\/testManagementConfigMenu\/([^"]+)"/g),
    ].map((m) => m[1]);
    expect(imports.sort()).toEqual([
      "PanelOrder",
      "ResultSelectListAdd",
      "SelectListRenameEntry",
      "TestActivation",
      "TestManagementConfigMenu",
      "TestOrderability",
    ]);
  });
});
