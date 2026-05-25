/**
 * AHRI research-lab allowlist for User Management lab-unit assignment.
 * Must match research-lab-linkages.csv and AHRITestSectionCatalog.java.
 */
const AHRI_LAB_UNIT_NAMES = [
  "Traditional & Modern Medicine Research Lab",
  "Bioanalytical Laboratory",
  "Bioequivalence Laboratory",
  "Immunology Laboratory",
  "Immunology",
  "Pathology Laboratory",
  "Bacteriology Laboratory",
  "Bacteriology",
  "Malaria and Neglected Tropical Disease (MNTD) Laboratory",
  "Pharmaceuticals Laboratory",
  "Viral Vaccine",
  "Tuberculosis Laboratory",
  "CTD",
  "Biorepository Laboratory",
];

const normalizedAhriLabNames = new Set(
  AHRI_LAB_UNIT_NAMES.map((name) => normalizeLabUnitName(name)),
);

function normalizeLabUnitName(name) {
  if (!name) {
    return "";
  }
  return String(name).trim().toLowerCase().replace(/\s+/g, " ");
}

/**
 * @param {{ id: string, value: string }[]} sections
 * @returns {{ id: string, value: string }[]}
 */
export function filterAhriLabUnitTestSections(sections) {
  if (!sections || !Array.isArray(sections)) {
    return [];
  }
  return sections.filter((section) => {
    if (!section || section.id === "AllLabUnits") {
      return false;
    }
    return normalizedAhriLabNames.has(normalizeLabUnitName(section.value));
  });
}

export { AHRI_LAB_UNIT_NAMES };
