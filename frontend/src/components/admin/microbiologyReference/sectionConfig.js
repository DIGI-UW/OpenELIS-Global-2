export const MICROBIOLOGY_REFERENCE_BASE =
  "/MasterListsPage/MicrobiologyReference";

export const MICROBIOLOGY_REFERENCE_SECTIONS = [
  {
    key: "organisms",
    label: "microbiology.admin.organisms.title",
  },
  {
    key: "antibiotics",
    label: "microbiology.admin.antibiotics.title",
  },
  {
    key: "ast-panels",
    label: "microbiology.admin.astPanels.title",
  },
  {
    key: "culture-setups",
    label: "microbiology.admin.cultureSetups.title",
  },
  {
    key: "breakpoints",
    label: "microbiology.admin.breakpoints.title",
  },
];

export const DEFAULT_MICROBIOLOGY_REFERENCE_SECTION = "organisms";

export const sectionPath = (basePath, sectionKey, detailId) => {
  const base = `${basePath}/MicrobiologyReference/${sectionKey}`;
  return detailId ? `${base}/${encodeURIComponent(detailId)}` : base;
};

export const isKnownSection = (sectionKey) =>
  MICROBIOLOGY_REFERENCE_SECTIONS.some((section) => section.key === sectionKey);
