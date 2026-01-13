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
 * Validates test selection - at least one test must be selected
 */
export const validateTestSelection = (projectData) => {
  const errors = [];

  // Check if at least one test is selected
  const hasAnyTest =
    projectData.dryTubeTaken ||
    projectData.edtaTubeTaken ||
    projectData.dbsTaken ||
    projectData.dbsvlTaken ||
    projectData.pscvlTaken ||
    projectData.plasmaTaken ||
    projectData.serumTaken ||
    projectData.serologyHIVTest ||
    projectData.murexTest ||
    projectData.integralTest ||
    projectData.genscreenTest ||
    projectData.genieIITest ||
    projectData.vironostikaTest ||
    projectData.genieII100Test ||
    projectData.genieII10Test ||
    projectData.WB1Test ||
    projectData.WB2Test ||
    projectData.p24AgTest ||
    projectData.innoliaTest ||
    projectData.cd4cd8Test ||
    projectData.cd4CountTest ||
    projectData.cd3CountTest ||
    projectData.viralLoadTest ||
    projectData.genotypingTest ||
    projectData.dnaPCR ||
    projectData.hpvTest ||
    projectData.asanteTest ||
    projectData.glycemiaTest ||
    projectData.creatinineTest ||
    projectData.transaminaseTest ||
    projectData.transaminaseALTLTest ||
    projectData.transaminaseASTLTest ||
    projectData.nfsTest ||
    projectData.gbTest ||
    projectData.neutTest ||
    projectData.lymphTest ||
    projectData.monoTest ||
    projectData.eoTest ||
    projectData.basoTest ||
    projectData.grTest ||
    projectData.hbTest ||
    projectData.hctTest ||
    projectData.vgmTest ||
    projectData.tcmhTest ||
    projectData.ccmhTest ||
    projectData.plqTest;

  if (!hasAnyTest) {
    errors.push({
      field: "tests",
      message: "At least one test or sample type must be selected",
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
  if (!projectData.ARVcenterName) {
    errors.push({
      field: "ARVcenterName",
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
  if (!projectData.EIDsiteName) {
    errors.push({
      field: "EIDsiteName",
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

  // RTN site is required
  if (!projectData.RTNsiteName) {
    errors.push({
      field: "RTNsiteName",
      message: "RTN site is required for RTN projects",
    });
  }

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
  if (!projectData.INDsiteName) {
    errors.push({
      field: "INDsiteName",
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
