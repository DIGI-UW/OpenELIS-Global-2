/**
 * Resolves the requesting lab unit for BR-F-02 sample requests.
 * Notebook / workflow context takes priority over session loginLabUnit so
 * Biorepository staff requesting on behalf of CTD (etc.) see the correct lab.
 *
 * Same-lab fast-track (auto approve/retrieve/release) is deferred — see
 * biorepository.retrieval.same-lab-fast-track.enabled (future).
 */

/** workflowType → departmentName (from ahri-workflows.csv) */
export const WORKFLOW_DEPARTMENT_NAMES = {
  bacteriology: "Bacteriology",
  bioanalytical: "Bioanalytical Laboratory",
  bioequivalence: "Bioequivalence Laboratory",
  biorepository: "Biorepository Laboratory",
  gbd: "Genomics & Bioinformatics Laboratory",
  genomics: "Genomics & Bioinformatics Laboratory",
  immunology: "Immunology",
  medlab: "CTD",
  mntd: "Malaria and Neglected Tropical Disease (MNTD) Laboratory",
  pathology: "Pathology Laboratory",
  pharmaceutical: "Pharmaceuticals Laboratory",
  traditional_medicine: "Traditional & Modern Medicine Research Lab",
  tuberculosis: "Tuberculosis Laboratory",
  viral_vaccine: "Viral Vaccine",
  virology: "Virology Laboratory",
};

export const normalizeLabUnitName = (value) =>
  String(value || "")
    .trim()
    .toLowerCase()
    .replace(/\s+/g, " ");

export const isBiorepositoryLabUnit = (value) => {
  const normalized = normalizeLabUnitName(value);
  return normalized.includes("biorepository");
};

const readDepartmentLabel = (department) => {
  if (!department || typeof department !== "object") {
    return null;
  }
  return (
    department.localizedName ||
    department.testSectionName ||
    department.name ||
    department.description ||
    null
  );
};

export const getNonBiorepositoryDepartments = (notebookData) => {
  const raw = notebookData?.departments;
  if (!raw) {
    return [];
  }
  const list = Array.isArray(raw) ? raw : Object.values(raw);
  return list
    .map(readDepartmentLabel)
    .filter((name) => name && !isBiorepositoryLabUnit(name));
};

/**
 * @param {{ entryData?: object, notebookData?: object, pageData?: object, session?: object }} context
 * @returns {string}
 */
export const resolveRequesterLabUnit = ({
  entryData,
  notebookData,
  pageData,
  session,
} = {}) => {
  const notebookDepartments = getNonBiorepositoryDepartments(notebookData);
  if (notebookDepartments.length === 1) {
    return notebookDepartments[0];
  }
  if (notebookDepartments.length > 1) {
    const workflowType = (
      notebookData?.workflowType ||
      pageData?.workflowType ||
      ""
    )
      .trim()
      .toLowerCase();
    const mapped = WORKFLOW_DEPARTMENT_NAMES[workflowType];
    if (mapped && !isBiorepositoryLabUnit(mapped)) {
      return mapped;
    }
    return notebookDepartments[0];
  }

  const workflowType = (
    notebookData?.workflowType ||
    pageData?.workflowType ||
    ""
  )
    .trim()
    .toLowerCase();
  const fromWorkflow = WORKFLOW_DEPARTMENT_NAMES[workflowType];
  if (fromWorkflow && !isBiorepositoryLabUnit(fromWorkflow)) {
    return fromWorkflow;
  }

  const organizationName = entryData?.organizationName;
  if (organizationName && !isBiorepositoryLabUnit(organizationName)) {
    return organizationName;
  }

  const sessionLabUnit = session?.loginLabUnit;
  if (sessionLabUnit && !isBiorepositoryLabUnit(sessionLabUnit)) {
    return sessionLabUnit;
  }

  return "";
};

export const buildContactInfo = (session, requesterLabUnit) => {
  const displayName =
    `${session?.firstName || ""} ${session?.lastName || ""}`.trim();
  const contactParts = [];
  if (displayName && session?.loginName) {
    contactParts.push(`${displayName} (${session.loginName})`);
  } else if (displayName || session?.loginName) {
    contactParts.push(displayName || session.loginName);
  }
  if (requesterLabUnit) {
    contactParts.push(requesterLabUnit);
  }
  return contactParts.join(" · ");
};
