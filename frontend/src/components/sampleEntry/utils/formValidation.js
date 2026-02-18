// Form validation utility for Study Entry forms

/**
 * Validates patient information fields
 */
export const validatePatientInfo = (formData) => {
  const errors = [];

  // At least one patient identifier is required
  if (
    !formData.subjectNumber &&
    !formData.siteSubjectNumber &&
    !formData.upidCode
  ) {
    errors.push({
      field: "patientId",
      message: "At least one patient identifier is required",
    });
  }

  // Gender is required
  if (!formData.gender) {
    errors.push({
      field: "gender",
      message: "Gender is required",
    });
  }

  // Date of birth validation (optional but if provided, must be valid)
  if (formData.birthDateForDisplay) {
    const birthDate = parseDate(formData.birthDateForDisplay);
    const today = new Date();
    if (birthDate > today) {
      errors.push({
        field: "birthDateForDisplay",
        message: "Birth date cannot be in the future",
      });
    }
  }

  return errors;
};

/**
 * Validates sample information fields
 */
export const validateSampleInfo = (formData) => {
  const errors = [];

  // Lab number is required
  if (!formData.labNo || formData.labNo.trim() === "") {
    errors.push({
      field: "labNo",
      message: "Lab number is required",
    });
  }

  // Received date is required
  if (!formData.receivedDateForDisplay) {
    errors.push({
      field: "receivedDateForDisplay",
      message: "Received date is required",
    });
  } else {
    // Validate received date is not in the future
    const receivedDate = parseDate(formData.receivedDateForDisplay);
    const today = new Date();
    if (receivedDate > today) {
      errors.push({
        field: "receivedDateForDisplay",
        message: "Received date cannot be in the future",
      });
    }
  }

  // Interview date validation (if provided)
  if (formData.interviewDate) {
    const interviewDate = parseDate(formData.interviewDate);
    const today = new Date();
    if (interviewDate > today) {
      errors.push({
        field: "interviewDate",
        message: "Interview date cannot be in the future",
      });
    }
  }

  return errors;
};

/**
 * Validates project selection
 */
export const validateProjectSelection = (formData) => {
  const errors = [];

  if (!formData.project) {
    errors.push({
      field: "project",
      message: "Project/Study type selection is required",
    });
  }

  return errors;
};

/**
 * Validates test selection - at least one test must be selected,
 * and each selected specimen container must have at least one
 * corresponding test selected (tube/test coherence).
 *
 * These groupings mirror the backend ARVFormMapper / EIDFormMapper logic:
 *   dryTubeTaken  → serologyHIVTest | glycemiaTest | creatinineTest | transaminaseTest
 *   edtaTubeTaken → nfsTest | cd4cd8Test | viralLoadTest | genotypingTest
 *   dbsTaken      → dnaPCR
 *   dbsvlTaken    → viralLoadTest
 *   pscvlTaken    → viralLoadTest
 *   plasmaTaken   → asanteTest
 *   serumTaken    → asanteTest
 */
export const validateTestSelection = (projectData) => {
  const errors = [];

  // Safety check: ensure projectData exists
  if (!projectData || typeof projectData !== "object") {
    errors.push({
      field: "tests",
      message:
        "Select at least one specimen container (e.g. Dry Tube) and at least one test",
    });
    return errors;
  }

  // Specimen-container fields (these are NOT tests – they represent tubes/containers).
  const specimenFields = [
    "dryTubeTaken",
    "edtaTubeTaken",
    "dbsTaken",
    "dbsvlTaken",
    "pscvlTaken",
    "plasmaTaken",
    "serumTaken",
    "preservCytTaken",
  ];

  // Actual test fields that the backend mapper will resolve to DB tests.
  // Groups mirror ARVFormMapper, EIDFormMapper, RecencyFormMapper, HPVFormMapper.
  const dryTubeTestFields = [
    "serologyHIVTest",
    "glycemiaTest",
    "creatinineTest",
    "transaminaseTest",
    "transaminaseALTLTest",
    "transaminaseASTLTest",
    // individual HIV sub-tests (legacy / IND forms)
    "murexTest",
    "integralTest",
    "genscreenTest",
    "genieIITest",
    "vironostikaTest",
    "genieII100Test",
    "genieII10Test",
    "wb1Test",
    "wb2Test",
    "p24AgTest",
    "innoliaTest",
  ];

  const edtaTubeTestFields = [
    "nfsTest",
    "cd4cd8Test",
    "cd4CountTest",
    "cd3CountTest",
    "viralLoadTest",
    "genotypingTest",
    // individual NFS sub-tests (if user checks them directly)
    "gbTest",
    "neutTest",
    "lymphTest",
    "monoTest",
    "eoTest",
    "basoTest",
    "grTest",
    "hbTest",
    "hctTest",
    "vgmTest",
    "tcmhTest",
    "ccmhTest",
    "plqTest",
  ];

  const dbsTestFields = ["dnaPCR"];
  const dbsvlTestFields = ["viralLoadTest"];
  const pscvlTestFields = ["viralLoadTest"];
  const plasmaSerumTestFields = ["asanteTest"];

  // All actual test fields (union of all groups above plus HPV/recency)
  const allTestFields = [
    ...new Set([
      ...dryTubeTestFields,
      ...edtaTubeTestFields,
      ...dbsTestFields,
      "hpvTest",
      "abbottOrRocheAnalysis",
      "geneXpertAnalysis",
      "asanteTest",
    ]),
  ];

  const hasSpecimen = specimenFields.some((f) => projectData[f] === true);
  const hasTest = allTestFields.some((f) => projectData[f] === true);

  // Require BOTH specimen container AND test
  if (!hasSpecimen && !hasTest) {
    errors.push({
      field: "tests",
      message:
        "Select at least one specimen container (e.g. Dry Tube) and at least one test",
    });
    return errors;
  }

  if (!hasSpecimen) {
    errors.push({
      field: "tests",
      message:
        "Select at least one specimen container (e.g. Dry Tube, EDTA Tube)",
    });
    return errors;
  }

  if (!hasTest) {
    errors.push({
      field: "tests",
      message:
        "Select at least one test (e.g. Serology HIV Test, Glycemia Test, DNA PCR)",
    });
    return errors;
  }

  // --- Tube / test coherence checks ---
  // If a container is ticked but none of its associated tests are ticked,
  // the backend will receive an empty test list for that sample item and
  // reject with errors.no.sample.

  if (
    projectData.dryTubeTaken &&
    !dryTubeTestFields.some((f) => projectData[f])
  ) {
    errors.push({
      field: "dryTubeTaken",
      message:
        "Dry Tube is selected but no Dry Tube tests are selected " +
        "(e.g. Serology HIV, Glycémie, Créatininémie, Transaminases)",
    });
  }

  if (
    projectData.edtaTubeTaken &&
    !edtaTubeTestFields.some((f) => projectData[f])
  ) {
    errors.push({
      field: "edtaTubeTaken",
      message:
        "EDTA Tube is selected but no EDTA tests are selected " +
        "(e.g. NFS, CD4/CD8, Viral Load, Génotypage)",
    });
  }

  if (projectData.dbsTaken && !dbsTestFields.some((f) => projectData[f])) {
    errors.push({
      field: "dbsTaken",
      message: "DBS is selected but DNA PCR is not checked",
    });
  }

  if (projectData.dbsvlTaken && !dbsvlTestFields.some((f) => projectData[f])) {
    errors.push({
      field: "dbsvlTaken",
      message: "DBS VL is selected but Viral Load test is not checked",
    });
  }

  if (projectData.pscvlTaken && !pscvlTestFields.some((f) => projectData[f])) {
    errors.push({
      field: "pscvlTaken",
      message: "PSC VL is selected but Viral Load test is not checked",
    });
  }

  if (
    (projectData.plasmaTaken || projectData.serumTaken) &&
    !plasmaSerumTestFields.some((f) => projectData[f])
  ) {
    errors.push({
      field: "plasmaTaken",
      message:
        "Plasma/Serum is selected but no Recency test (Asante) is checked",
    });
  }

  return errors;
};

/**
 * Validates ARV section fields
 */
export const validateARVSection = (projectData, selectedProject) => {
  const errors = [];

  // Only validate if ARV project is selected
  if (
    !selectedProject.includes("ARV") &&
    !selectedProject.includes("Initial") &&
    !selectedProject.includes("Follow")
  ) {
    return errors;
  }

  // ARV center is required for ARV projects
  if (!projectData.arvcenterName) {
    errors.push({
      field: "arvcenterName",
      message: "ARV center is required for ARV projects",
    });
  }

  return errors;
};

/**
 * Validates EID section fields
 */
export const validateEIDSection = (projectData, selectedProject) => {
  const errors = [];

  // Only validate if EID project is selected
  if (!selectedProject.includes("EID")) {
    return errors;
  }

  // EID site is required
  if (!projectData.eidsiteName) {
    errors.push({
      field: "eidsiteName",
      message: "EID site is required for EID projects",
    });
  }

  // Infant number is required for EID
  if (!projectData.dbsInfantNumber) {
    errors.push({
      field: "dbsInfantNumber",
      message: "Infant number is required for EID projects",
    });
  }

  // PCR type is required
  if (!projectData.eidWhichPCR) {
    errors.push({
      field: "eidWhichPCR",
      message: "PCR type is required for EID projects",
    });
  }

  return errors;
};

/**
 * Validates special request section fields
 */
export const validateSpecialRequestSection = (projectData, selectedProject) => {
  const errors = [];

  // Only validate if Special Request project is selected
  if (!selectedProject.includes("Special")) {
    return errors;
  }

  // Reason for request is required for special requests
  if (
    !projectData.reasonForRequest ||
    projectData.reasonForRequest.trim() === ""
  ) {
    errors.push({
      field: "reasonForRequest",
      message: "Reason for request is required for special requests",
    });
  }

  // Email validation if provided
  if (projectData.email && !isValidEmail(projectData.email)) {
    errors.push({
      field: "email",
      message: "Invalid email format",
    });
  }

  // Phone validation if provided
  if (projectData.phoneNumber && !isValidPhone(projectData.phoneNumber)) {
    errors.push({
      field: "phoneNumber",
      message: "Invalid phone number format",
    });
  }

  return errors;
};

/**
 * Validates RTN section fields
 */
export const validateRTNSection = (projectData, selectedProject) => {
  const errors = [];

  // Only validate if RTN project is selected
  if (!selectedProject.includes("RTN")) {
    return errors;
  }

  // RTN validation - currently no required fields

  return errors;
};

/**
 * Validates HPV section fields
 */
export const validateHPVSection = (projectData, selectedProject) => {
  const errors = [];

  // Only validate if HPV project is selected
  if (!selectedProject.includes("HPV")) {
    return errors;
  }

  // Collection method is required
  if (
    !projectData.selfCollection &&
    !projectData.collectionDoneByHealthWorker
  ) {
    errors.push({
      field: "hpvCollectionMethod",
      message: "Collection method is required for HPV testing",
    });
  }

  // At least one analysis type is required
  if (!projectData.abbottOrRocheAnalysis && !projectData.geneXpertAnalysis) {
    errors.push({
      field: "hpvAnalysisType",
      message: "At least one analysis type is required for HPV testing",
    });
  }

  return errors;
};

/**
 * Validates Indeterminate section fields
 */
export const validateIndeterminateSection = (projectData, selectedProject) => {
  const errors = [];

  // Only validate if Indeterminate project is selected
  if (!selectedProject.includes("Indeterminate")) {
    return errors;
  }

  // Site is required
  if (!projectData.indsiteName) {
    errors.push({
      field: "indsiteName",
      message: "Site is required for indeterminate results",
    });
  }

  // Investigation notes are required
  if (
    !projectData.underInvestigationNote ||
    projectData.underInvestigationNote.trim() === ""
  ) {
    errors.push({
      field: "underInvestigationNote",
      message: "Investigation notes are required for indeterminate results",
    });
  }

  return errors;
};

/**
 * Main validation function - validates entire form
 */
export const validateEntireForm = (formData, selectedProject) => {
  let allErrors = [];

  // Validate each section
  allErrors = allErrors.concat(validatePatientInfo(formData));
  allErrors = allErrors.concat(validateSampleInfo(formData));
  allErrors = allErrors.concat(validateProjectSelection(formData));
  allErrors = allErrors.concat(validateTestSelection(formData.projectData));
  allErrors = allErrors.concat(
    validateARVSection(formData.projectData, selectedProject),
  );
  allErrors = allErrors.concat(
    validateEIDSection(formData.projectData, selectedProject),
  );
  allErrors = allErrors.concat(
    validateSpecialRequestSection(formData.projectData, selectedProject),
  );
  allErrors = allErrors.concat(
    validateRTNSection(formData.projectData, selectedProject),
  );
  allErrors = allErrors.concat(
    validateHPVSection(formData.projectData, selectedProject),
  );
  allErrors = allErrors.concat(
    validateIndeterminateSection(formData.projectData, selectedProject),
  );

  return allErrors;
};

/**
 * Helper function to parse date string (dd/mm/yyyy format)
 */
const parseDate = (dateString) => {
  if (!dateString) return null;

  const parts = dateString.split("/");
  if (parts.length !== 3) return null;

  const day = parseInt(parts[0], 10);
  const month = parseInt(parts[1], 10) - 1; // Month is 0-indexed
  const year = parseInt(parts[2], 10);

  return new Date(year, month, day);
};

/**
 * Helper function to validate email format
 */
const isValidEmail = (email) => {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return emailRegex.test(email);
};

/**
 * Helper function to validate phone number format
 */
const isValidPhone = (phone) => {
  // Remove spaces, dashes, parentheses
  const cleaned = phone.replace(/[\s\-()]/g, "");
  // Check if it contains only digits and optional + at start
  const phoneRegex = /^\+?[0-9]{7,15}$/;
  return phoneRegex.test(cleaned);
};

/**
 * Helper function to format validation errors for display
 */
export const formatValidationErrors = (errors) => {
  if (!errors || errors.length === 0) {
    return "";
  }

  return errors.map((error) => error.message).join(", ");
};

/**
 * Helper function to check if form has validation errors for a specific field
 */
export const hasFieldError = (errors, fieldName) => {
  return errors.some((error) => error.field === fieldName);
};

/**
 * Helper function to get error message for a specific field
 */
export const getFieldError = (errors, fieldName) => {
  const error = errors.find((error) => error.field === fieldName);
  return error ? error.message : "";
};
