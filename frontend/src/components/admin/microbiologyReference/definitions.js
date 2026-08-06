const option = (value, label) => ({ value, label });

export const REFERENCE_DEFINITIONS = {
  "patient-origins": {
    resource: "patient-origins",
    title: "microbiology.admin.patientOrigins.title",
    description: "microbiology.admin.patientOrigins.description",
    readOnly: true,
    columns: [
      { key: "code", label: "microbiology.admin.field.code" },
      { key: "displayName", label: "microbiology.admin.field.name" },
      { key: "whonetCode", label: "microbiology.admin.field.whonetCode" },
    ],
    fields: [],
  },
  organisms: {
    resource: "organisms",
    title: "microbiology.admin.organisms.title",
    description: "microbiology.admin.organisms.description",
    addLabel: "microbiology.admin.organisms.add",
    editTitle: "microbiology.admin.organisms.edit",
    columns: [
      { key: "displayName", label: "microbiology.admin.field.name" },
      { key: "whonetCode", label: "microbiology.admin.field.whonetCode" },
      { key: "organismGroup", label: "microbiology.admin.field.group" },
      {
        key: "initialSignificance",
        label: "microbiology.admin.field.significance",
      },
      {
        key: "defaultAstPanelName",
        label: "microbiology.admin.field.defaultPanel",
      },
    ],
    fields: [
      {
        key: "displayName",
        label: "microbiology.admin.field.name",
        required: true,
      },
      {
        key: "shortName",
        label: "microbiology.admin.field.shortName",
      },
      {
        key: "whonetCode",
        label: "microbiology.admin.field.whonetCode",
        required: true,
      },
      { key: "oclCode", label: "microbiology.admin.field.oclCode" },
      {
        key: "organismGroup",
        label: "microbiology.admin.field.group",
        required: true,
      },
      { key: "gramStain", label: "microbiology.admin.field.gramStain" },
      {
        key: "initialSignificance",
        label: "microbiology.admin.field.significance",
        type: "select",
        options: [
          option("USUALLY", "microbiology.admin.significance.usually"),
          option("POSSIBLE", "microbiology.admin.significance.possible"),
          option("RARE", "microbiology.admin.significance.rare"),
        ],
      },
      {
        key: "defaultAstPanelId",
        label: "microbiology.admin.field.defaultPanel",
        type: "dynamic-select",
        optionsResource: "ast-panels",
      },
      {
        key: "notes",
        label: "microbiology.admin.field.notes",
        type: "textarea",
      },
      {
        key: "active",
        label: "microbiology.admin.field.active",
        type: "checkbox",
      },
    ],
  },
  antibiotics: {
    resource: "antibiotics",
    title: "microbiology.admin.antibiotics.title",
    description: "microbiology.admin.antibiotics.description",
    addLabel: "microbiology.admin.antibiotics.add",
    editTitle: "microbiology.admin.antibiotics.edit",
    columns: [
      { key: "displayName", label: "microbiology.admin.field.name" },
      { key: "whonetCode", label: "microbiology.admin.field.whonetCode" },
      { key: "antibioticClass", label: "microbiology.admin.field.class" },
      { key: "route", label: "microbiology.admin.field.route" },
    ],
    fields: [
      {
        key: "displayName",
        label: "microbiology.admin.field.name",
        required: true,
      },
      {
        key: "whonetCode",
        label: "microbiology.admin.field.whonetCode",
        required: true,
      },
      {
        key: "antibioticClass",
        label: "microbiology.admin.field.class",
        required: true,
      },
      {
        key: "route",
        label: "microbiology.admin.field.route",
        type: "select",
        options: [
          option("ORAL", "microbiology.admin.route.oral"),
          option("IV", "microbiology.admin.route.iv"),
          option("BOTH", "microbiology.admin.route.both"),
          option("TOPICAL", "microbiology.admin.route.topical"),
        ],
      },
      {
        key: "notes",
        label: "microbiology.admin.field.notes",
        type: "textarea",
      },
      {
        key: "active",
        label: "microbiology.admin.field.active",
        type: "checkbox",
      },
    ],
  },
  "culture-setups": {
    resource: "culture-setups",
    title: "microbiology.admin.cultureSetups.title",
    description: "microbiology.admin.cultureSetups.description",
    addLabel: "microbiology.admin.cultureSetups.add",
    editTitle: "microbiology.admin.cultureSetups.edit",
    canToggle: false,
    columns: [
      { key: "methodName", label: "microbiology.admin.field.method" },
      { key: "name", label: "microbiology.admin.field.name" },
      { key: "workflowType", label: "microbiology.admin.field.workflow" },
      { key: "mediaDefaults", label: "microbiology.admin.field.media" },
      {
        key: "incubationDefaults",
        label: "microbiology.admin.field.incubation",
      },
      {
        key: "maxIncubationDays",
        label: "microbiology.admin.field.maxIncubationDays",
      },
    ],
    fields: [
      {
        key: "methodId",
        label: "microbiology.admin.field.method",
        required: true,
        disabledOnEdit: true,
        type: "dynamic-select",
        optionsResource: "methods",
      },
      { key: "name", label: "microbiology.admin.field.name", required: true },
      {
        key: "workflowType",
        label: "microbiology.admin.field.workflow",
        type: "select",
        options: [
          option("BACTERIOLOGY", "microbiology.workflow.bacteriology"),
          option("MYCOBACTERIOLOGY", "microbiology.workflow.mycobacteriology"),
          option("MYCOLOGY", "microbiology.workflow.mycology"),
          option("PARASITOLOGY", "microbiology.workflow.parasitology"),
        ],
      },
      {
        key: "mediaDefaults",
        label: "microbiology.admin.field.media",
        type: "textarea",
      },
      {
        key: "incubationDefaults",
        label: "microbiology.admin.field.incubation",
        type: "textarea",
      },
      {
        key: "incubationHours",
        label: "microbiology.admin.field.incubationHours",
        type: "number",
        min: 1,
        max: 8760,
      },
      {
        key: "subcultureAtHours",
        label: "microbiology.admin.field.subcultureAtHours",
        type: "number",
        min: 1,
        max: 8760,
      },
      {
        key: "maxIncubationDays",
        label: "microbiology.admin.field.maxIncubationDays",
        type: "number",
        min: 1,
        max: 365,
      },
      {
        key: "atmosphereDefaults",
        label: "microbiology.admin.field.atmosphere",
        type: "textarea",
      },
      {
        key: "active",
        label: "microbiology.admin.field.active",
        type: "checkbox",
      },
    ],
  },
};
