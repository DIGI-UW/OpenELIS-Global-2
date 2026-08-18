import fs from "fs";
import path from "path";

/**
 * The legacy Test Management menu must keep opening the legacy pages.
 *
 * Its tiles are plain hrefs into /MasterListsPage/..., so the only thing that
 * decides where "Manage Sample Types" lands is which component Admin routes that
 * path to. Pointing it at the new Sample Type Editor took the legacy page out of
 * reach entirely — the tile still said Manage Sample Types and opened something
 * else. The new editor answers on /SampleTypeEditor instead.
 *
 * This reads the source rather than rendering: Admin pulls in the whole admin
 * route tree, and what is being guarded is the wiring, not any behaviour a render
 * would show.
 */

const read = (relative) =>
  fs.readFileSync(path.join(__dirname, "..", relative), "utf8");

const admin = read("Admin.jsx");
const legacyMenu = read(
  "testManagementConfigMenu/TestManagementConfigMenu.jsx",
);

/** The component name Admin routes a given path to. */
const componentFor = (routePath) => {
  const pattern = new RegExp(
    "path=\\{`\\$\\{path\\}/" +
      routePath +
      "[^`]*`\\}[\\s\\S]{0,120}?component=\\{([A-Za-z]+)\\}",
  );
  const match = admin.match(pattern);
  return match && match[1];
};

/** Where a given import name is imported from. */
const importSourceOf = (name) => {
  const match = admin.match(new RegExp("import " + name + ' from "([^"]+)"'));
  return match && match[1];
};

describe("legacy Test Management links open legacy pages", () => {
  it("the menu still points Manage Sample Types at the legacy path", () => {
    expect(legacyMenu).toContain(
      'href="/MasterListsPage/SampleTypeManagement"',
    );
  });

  it("routes that path to the legacy page, not the new editor", () => {
    const component = componentFor("SampleTypeManagement");

    expect(component).toBe("LegacySampleTypeManagement");
    expect(
      importSourceOf(component),
      "must resolve to the legacy module under testManagementConfigMenu",
    ).toContain("testManagementConfigMenu/SampleTypeManagement");
  });

  it("gives the new Sample Type Editor a path of its own", () => {
    const component = componentFor("SampleTypeEditor");

    expect(component).toBe("SampleTypeEditor");
    expect(importSourceOf(component)).toContain(
      "sampleTypeManagement/SampleTypeManagement",
    );
  });

  /**
   * Every other tile in the menu, so a future editor cannot quietly take one of
   * these paths over the way the sample type one was taken over.
   */
  it("leaves every other legacy tile on a legacy page", () => {
    const legacyByPath = {
      PanelManagement: "testManagementConfigMenu/PanelManagement",
      TestSectionManagement: "testManagementConfigMenu",
      MethodManagement: "testManagement/ManageMethod",
    };

    Object.entries(legacyByPath).forEach(([routePath, expectedSource]) => {
      expect(legacyMenu, `${routePath} tile`).toContain(
        `href="/MasterListsPage/${routePath}"`,
      );
      const component = componentFor(routePath);
      expect(component, `${routePath} route`).toBeTruthy();
      expect(
        importSourceOf(component),
        `${routePath} must still resolve to its legacy module`,
      ).toContain(expectedSource);
    });
  });
});
