import { PANEL_SECTIONS, isValidPanelSection } from "../panelSectionConfig";
import {
  SAMPLE_TYPE_SECTIONS,
  isValidSampleTypeSection,
} from "../../sampleTypeManagement/sectionConfig";
import messages from "../../../../languages/en.json";

/**
 * Localization is a section of the Panel and Sample Type editors, not only the
 * Test Catalogue Editor.
 *
 * The registries are the single source of truth for both the routed sections and
 * the SideNav items, so a section that is not registered is unreachable however
 * well the editor renders it — and one registered without a label shows its raw
 * message id in the nav.
 */
describe("Localization is offered by every editor that has a name to translate", () => {
  it("registers the section for panels", () => {
    expect(PANEL_SECTIONS).toContain("localization");
    expect(isValidPanelSection("localization")).toBe(true);
  });

  it("registers the section for sample types", () => {
    expect(SAMPLE_TYPE_SECTIONS).toContain("localization");
    expect(isValidSampleTypeSection("localization")).toBe(true);
  });

  it("leaves Basic Info as the section each editor opens on", () => {
    // Adding a section must not change where a deep link with no section lands.
    expect(PANEL_SECTIONS[0]).toBe("basic-info");
    expect(SAMPLE_TYPE_SECTIONS[0]).toBe("basic-info");
  });

  it("labels every registered section, for both entities", () => {
    PANEL_SECTIONS.forEach((key) =>
      expect(
        messages[`label.panel.section.${key}`],
        `label.panel.section.${key}`,
      ).toBeTruthy(),
    );
    SAMPLE_TYPE_SECTIONS.forEach((key) =>
      expect(
        messages[`label.sampleType.section.${key}`],
        `label.sampleType.section.${key}`,
      ).toBeTruthy(),
    );
  });
});

/**
 * Item 6's naming, asserted on the strings themselves so a rename cannot drift
 * back. "Catalog" is the legacy spelling and stays on the legacy pages; the new
 * area is "Catalogue".
 */
describe("the three editors are named consistently", () => {
  it("names them Test Catalogue Editor, Panel Editor, Sample Type Editor", () => {
    expect(messages["label.testCatalog.editor"]).toBe("Test Catalogue Editor");
    expect(messages["sidenav.label.admin.testmgt.testCatalogEditor"]).toBe(
      "Test Catalogue Editor",
    );
    expect(messages["heading.sampleType.management"]).toBe(
      "Sample Type Editor",
    );
    expect(messages["sidenav.label.admin.sampleTypeManagement"]).toBe(
      "Sample Type Editor",
    );
  });

  it("names the area Test Catalogue Management", () => {
    expect(messages["sidenav.label.admin.testCatalog"]).toBe(
      "Test Catalogue Management",
    );
  });

  it("uses one wording for all three empty states", () => {
    expect(messages["sidenav.label.admin.testCatalog.sectionsHelper"]).toBe(
      "Click a test to edit its sections",
    );
    expect(messages["sidenav.label.admin.panel.sectionsHelper"]).toBe(
      "Click a panel to edit its sections",
    );
    expect(messages["sidenav.label.admin.sampleType.sectionsHelper"]).toBe(
      "Click a sample type to edit its sections",
    );
  });

  it("no longer calls the Panel Editor 'Panel Management' where a user reads it", () => {
    [
      "label.testCatalog.panels.createPanelHint",
      "notification.testCatalog.panels.created",
    ].forEach((key) => {
      expect(messages[key]).not.toContain("Panel Management");
      expect(messages[key]).toContain("Panel Editor");
    });
  });
});
