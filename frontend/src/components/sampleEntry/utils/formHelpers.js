// Form helper utilities for Study Entry forms

/**
 * Formats date from Date object to dd/mm/yyyy string
 */
export const formatDateForDisplay = (date) => {
  if (!date) return "";

  const d = new Date(date);
  const day = String(d.getDate()).padStart(2, "0");
  const month = String(d.getMonth() + 1).padStart(2, "0");
  const year = d.getFullYear();

  return `${day}/${month}/${year}`;
};

/**
 * Parses date string (dd/mm/yyyy) to Date object
 */
export const parseDateFromDisplay = (dateString) => {
  if (!dateString) return null;

  const parts = dateString.split("/");
  if (parts.length !== 3) return null;

  const day = parseInt(parts[0], 10);
  const month = parseInt(parts[1], 10) - 1; // Month is 0-indexed
  const year = parseInt(parts[2], 10);

  return new Date(year, month, day);
};

/**
 * Formats time to HH:MM format
 */
export const formatTimeForDisplay = (time) => {
  if (!time) return "";

  const d = new Date(time);
  const hours = String(d.getHours()).padStart(2, "0");
  const minutes = String(d.getMinutes()).padStart(2, "0");

  return `${hours}:${minutes}`;
};

/**
 * Normalizes time input to 24-hour HH:mm format.
 * Accepts "HH:mm", "H:mm", "HH:mm AM/PM", "H:mmAM/PM".
 */
export const normalizeTimeTo24Hour = (timeValue) => {
  if (!timeValue) return timeValue;

  const trimmed = String(timeValue).trim();
  const match = trimmed.match(/^(\d{1,2}):(\d{2})(?:\s*([AaPp][Mm]))?$/);
  if (!match) {
    return trimmed;
  }

  let hours = parseInt(match[1], 10);
  const minutes = match[2];
  const period = match[3] ? match[3].toUpperCase() : null;

  if (period) {
    if (hours > 12) {
      return `${String(hours).padStart(2, "0")}:${minutes}`;
    }
    if (hours === 12) {
      hours = period === "AM" ? 0 : 12;
    } else if (period === "PM") {
      hours += 12;
    }
  }

  if (hours < 0 || hours > 23) {
    return trimmed;
  }

  return `${String(hours).padStart(2, "0")}:${minutes}`;
};

/**
 * Gets current date in dd/mm/yyyy format
 */
export const getCurrentDateForDisplay = () => {
  return formatDateForDisplay(new Date());
};

/**
 * Gets current time in HH:MM format
 */
export const getCurrentTimeForDisplay = () => {
  return formatTimeForDisplay(new Date());
};

/**
 * Checks if any test is selected
 */
export const hasAnyTestSelected = (projectData) => {
  const testFields = [
    "dryTubeTaken",
    "edtaTubeTaken",
    "dbsTaken",
    "dbsvlTaken",
    "pscvlTaken",
    "plasmaTaken",
    "serumTaken",
    "serologyHIVTest",
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
    "cd4cd8Test",
    "cd4CountTest",
    "cd3CountTest",
    "viralLoadTest",
    "genotypingTest",
    "dnaPCR",
    "hpvTest",
    "asanteTest",
    "glycemiaTest",
    "creatinineTest",
    "transaminaseTest",
    "transaminaseALTLTest",
    "transaminaseASTLTest",
    "nfsTest",
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
    "preservCytTaken",
  ];

  return testFields.some((field) => projectData[field] === true);
};

/**
 * Gets list of selected test names
 */
export const getSelectedTestNames = (projectData) => {
  const testNames = {
    dryTubeTaken: "Dry Tube",
    edtaTubeTaken: "EDTA Tube",
    dbsTaken: "DBS",
    dbsvlTaken: "DBS Viral Load",
    pscvlTaken: "Plasma Viral Load",
    plasmaTaken: "Plasma",
    serumTaken: "Serum",
    serologyHIVTest: "Serology HIV",
    murexTest: "Murex",
    integralTest: "Integral",
    genscreenTest: "Genscreen",
    genieIITest: "Genie II",
    vironostikaTest: "Vironostika",
    genieII100Test: "Genie II 1/100",
    genieII10Test: "Genie II 1/10",
    wb1Test: "Western Blot 1",
    wb2Test: "Western Blot 2",
    p24AgTest: "P24 Ag",
    innoliaTest: "Innolia",
    cd4cd8Test: "CD4/CD8",
    cd4CountTest: "CD4 Count",
    cd3CountTest: "CD3 Count",
    viralLoadTest: "Viral Load",
    genotypingTest: "Genotyping",
    dnaPCR: "DNA PCR",
    hpvTest: "HPV Test",
    asanteTest: "Asante Test",
    glycemiaTest: "Glycemia",
    creatinineTest: "Creatinine",
    transaminaseTest: "Transaminase",
    transaminaseALTLTest: "ALT (SGPT)",
    transaminaseASTLTest: "AST (SGOT)",
    nfsTest: "NFS (CBC)",
    gbTest: "GB (WBC)",
    neutTest: "Neutrophils",
    lymphTest: "Lymphocytes",
    monoTest: "Monocytes",
    eoTest: "Eosinophils",
    basoTest: "Basophils",
    grTest: "GR (RBC)",
    hbTest: "Hemoglobin",
    hctTest: "Hematocrit",
    vgmTest: "VGM (MCV)",
    tcmhTest: "TCMH (MCH)",
    ccmhTest: "CCMH (MCHC)",
    plqTest: "Platelets",
    preservCytTaken: "PreservCyt",
  };

  const selected = [];

  Object.keys(testNames).forEach((key) => {
    if (projectData[key] === true) {
      selected.push(testNames[key]);
    }
  });

  return selected;
};

/**
 * Determines which project sections should be visible
 */
export const getVisibleSections = (selectedProject) => {
  if (!selectedProject) {
    return {
      showARV: false,
      showEID: false,
      showRTN: false,
      showHPV: false,
      showIndeterminate: false,
      showSpecialRequest: false,
    };
  }

  return {
    showARV:
      selectedProject.includes("ARV") ||
      selectedProject.includes("Initial") ||
      selectedProject.includes("Follow"),
    showEID: selectedProject.includes("EID"),
    showRTN: selectedProject.includes("RTN"),
    showHPV: selectedProject.includes("HPV"),
    showIndeterminate: selectedProject.includes("Indeterminate"),
    showSpecialRequest: selectedProject.includes("Special"),
  };
};

/**
 * Cleans form data before submission (removes empty strings, nulls)
 */
export const cleanFormDataForSubmission = (formData) => {
  const cleaned = JSON.parse(JSON.stringify(formData));

  // Remove empty strings and convert to null
  const cleanObject = (obj) => {
    Object.keys(obj).forEach((key) => {
      if (obj[key] === "" || obj[key] === undefined) {
        obj[key] = null;
      } else if (typeof obj[key] === "object" && obj[key] !== null) {
        cleanObject(obj[key]);
      }
    });
  };

  cleanObject(cleaned);
  return cleaned;
};

/**
 * Resets project data to initial state
 */
export const getInitialProjectData = () => {
  return {
    // ARV specific
    arvcenterName: "",
    arvcenterCode: "",
    doctor: "",

    // EID specific
    eidsiteName: "",
    eidsiteCode: "",
    dbsInfantNumber: "",
    dbsSiteInfantNumber: "",
    eidWhichPCR: "",
    eidSecondPCRReason: "",
    requester: "",

    // Indeterminate specific
    indsiteName: "",
    indsiteCode: "",

    // Test selections
    dryTubeTaken: false,
    edtaTubeTaken: false,
    dbsTaken: false,
    dbsvlTaken: false,
    pscvlTaken: false,
    plasmaTaken: false,
    serumTaken: false,

    // HIV Tests
    serologyHIVTest: false,
    murexTest: false,
    integralTest: false,
    genscreenTest: false,
    genieIITest: false,
    vironostikaTest: false,
    genieII100Test: false,
    genieII10Test: false,
    wb1Test: false,
    wb2Test: false,
    p24AgTest: false,
    innoliaTest: false,

    // Other Tests
    cd4cd8Test: false,
    cd4CountTest: false,
    cd3CountTest: false,
    viralLoadTest: false,
    genotypingTest: false,
    dnaPCR: false,

    // Chemistry Tests
    glycemiaTest: false,
    creatinineTest: false,
    transaminaseTest: false,
    transaminaseALTLTest: false,
    transaminaseASTLTest: false,

    // Hematology Tests
    nfsTest: false,
    gbTest: false,
    neutTest: false,
    lymphTest: false,
    monoTest: false,
    eoTest: false,
    basoTest: false,
    grTest: false,
    hbTest: false,
    hctTest: false,
    vgmTest: false,
    tcmhTest: false,
    ccmhTest: false,
    plqTest: false,

    // HPV specific
    hpvTest: false,
    preservCytTaken: false,
    abbottOrRocheAnalysis: false,
    geneXpertAnalysis: false,
    selfCollection: false,
    collectionDoneByHealthWorker: false,

    // Special Request
    address: "",
    phoneNumber: "",
    faxNumber: "",
    email: "",
    reasonForRequest: "",
    underInvestigationNote: "",
  };
};

/**
 * Copies data from one object to another (for double entry verification)
 */
export const copyFormData = (sourceData) => {
  return JSON.parse(JSON.stringify(sourceData));
};

/**
 * Compares two form data objects and returns differences (for double entry)
 */
export const compareFormData = (data1, data2) => {
  const differences = [];

  const compare = (obj1, obj2, path = "") => {
    Object.keys(obj1).forEach((key) => {
      const newPath = path ? `${path}.${key}` : key;

      if (typeof obj1[key] === "object" && obj1[key] !== null) {
        if (typeof obj2[key] === "object" && obj2[key] !== null) {
          compare(obj1[key], obj2[key], newPath);
        }
      } else if (obj1[key] !== obj2[key]) {
        differences.push({
          field: newPath,
          value1: obj1[key],
          value2: obj2[key],
        });
      }
    });
  };

  compare(data1, data2);
  return differences;
};

/**
 * Generates a unique lab number (placeholder - should be replaced with actual logic)
 */
export const generateLabNumber = (projectType) => {
  const date = new Date();
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  const random = Math.floor(Math.random() * 1000);

  const prefix = projectType
    ? projectType.substring(0, 3).toUpperCase()
    : "LAB";

  return `${prefix}${year}${month}${day}${String(random).padStart(3, "0")}`;
};

/**
 * Checks if form data has been modified
 */
export const isFormModified = (currentData, initialData) => {
  return JSON.stringify(currentData) !== JSON.stringify(initialData);
};

/**
 * Gets project type display name
 */
export const getProjectDisplayName = (projectValue) => {
  const projectNames = {
    ARV_INITIAL: "Initial ARV",
    ARV_FOLLOWUP: "Follow-up ARV",
    RTN: "RTN",
    EID: "EID",
    INDETERMINATE: "Indeterminate",
    SPECIAL_REQUEST: "Special Request",
    ARV_VIRAL_LOAD: "ARV - Viral Load",
    RECENCY_TESTING: "Recency Testing",
    HPV_TESTING: "HPV Testing",
  };

  return projectNames[projectValue] || projectValue;
};
