/**
 * OGC-1364 v3: the Result & Validation Configuration page shows the result
 * settings grouped by purpose instead of one flat table. Every setting lands in
 * a group, so a setting added later is never hidden: anything not listed here
 * goes under "Other".
 */
export const RESULT_SETTING_GROUPS = [
  {
    id: "validation",
    titleKey: "resultSettings.group.validation",
    names: [
      "useMultiLevelValidation",
      "validate all results",
      "blockSelfValidation",
      "qcFailBlocksValidation",
      "allowBulkReleaseClear",
      "validateTechnicalRejection",
    ],
  },
  {
    id: "resultEntry",
    titleKey: "resultSettings.group.resultEntry",
    names: [
      "alertWhenInvalidResult",
      "restrictFreeTextMethodEntry",
      "ResultTechnicianName",
      "autoFillTechNameBox",
      "autoFillTechNameUser",
      "customCriticalMessage",
    ],
  },
  {
    id: "modification",
    titleKey: "resultSettings.group.modification",
    names: [
      "allowResultRejection",
      "modify results note required",
      "modify results role",
      "retestNoteRequired",
    ],
  },
  {
    id: "display",
    titleKey: "resultSettings.group.display",
    names: ["showValidationFailureIcon"],
  },
  {
    id: "access",
    titleKey: "resultSettings.group.access",
    names: ["roleForPatientOnResults"],
  },
  {
    id: "pathology",
    titleKey: "resultSettings.group.pathology",
    prefix: "pathology.",
  },
  {
    id: "microbiology",
    titleKey: "resultSettings.group.microbiology",
    prefix: "microbiology",
  },
];

/**
 * Settings the validation policy replaces: shown while the policy is off, left
 * out while it decides.
 */
export const REPLACED_SETTINGS = ["validate all results"];

/** A setting that only applies while another one is on. */
export const SETTING_DEPENDS_ON = {
  autoFillTechNameBox: "ResultTechnicianName",
  autoFillTechNameUser: "ResultTechnicianName",
};

const groupOf = (name) =>
  RESULT_SETTING_GROUPS.find(
    (group) =>
      (group.names && group.names.includes(name)) ||
      (group.prefix && name.startsWith(group.prefix)),
  );

/**
 * The settings in page order: each group with its settings in the order listed
 * (prefixed groups by name), then "Other". Empty groups are left out.
 */
export function groupResultSettings(settings, { policyEnabled = true } = {}) {
  const shown = (settings || []).filter(
    (setting) => !(policyEnabled && REPLACED_SETTINGS.includes(setting.name)),
  );
  const groups = RESULT_SETTING_GROUPS.map((group) => ({
    id: group.id,
    titleKey: group.titleKey,
    settings: shown
      .filter((setting) => groupOf(setting.name) === group)
      .sort((a, b) =>
        group.names
          ? group.names.indexOf(a.name) - group.names.indexOf(b.name)
          : a.name.localeCompare(b.name),
      ),
  }));
  groups.push({
    id: "other",
    titleKey: "resultSettings.group.other",
    settings: shown.filter((setting) => !groupOf(setting.name)),
  });
  return groups.filter((group) => group.settings.length > 0);
}
