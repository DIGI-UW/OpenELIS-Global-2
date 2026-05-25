/**
 * SRS lab personas (§2.3 / access matrix p.59–61).
 * Used for notebook stage and action gates — not granular privilege roles.
 */
export const AHRI_SRS_LAB_PERSONAS = [
  "Sample Collector",
  "Laboratory Technician",
  "Junior Researcher",
  "Senior Researcher",
  "Lab Manager",
  "Biomedical Staff",
];

export const sampleRegistrationPersonas = [
  "Sample Collector",
  "Laboratory Technician",
];

export const sampleProcessingPersonas = [
  "Laboratory Technician",
  "Junior Researcher",
  "Senior Researcher",
];

export const resultValidationPersonas = [
  "Laboratory Technician",
  "Senior Researcher",
  "Lab Manager",
];

export const reportingPersonas = ["Lab Manager", "Senior Researcher"];

export const storagePersonas = ["Laboratory Technician", "Lab Manager"];

export const equipmentPersonas = ["Biomedical Staff", "Lab Manager"];

export const disposalPersonas = ["Lab Manager"];

export const notebookApprovalPersonas = ["Lab Manager", "Senior Researcher"];
