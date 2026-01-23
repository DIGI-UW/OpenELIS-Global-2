/**
 * TMMRD Quality Control Parameters and Validation Logic
 * Based on TMMRD SRS Section 5: QC Systems & Acceptance Criteria
 */

export const TMMRD_QC_TEMPLATES = {
  // TLC Quality Control Parameters
  TLC_ALKALOID: {
    parameters: [
      { name: "Rf Value", unit: "", acceptanceRange: "0.1-0.9", required: true },
      { name: "Spot Color", unit: "", acceptanceValues: ["Yellow", "Orange", "Brown", "Red"], required: true },
      { name: "UV Response (254nm)", unit: "", acceptanceValues: ["Positive", "Negative"], required: false },
      { name: "UV Response (366nm)", unit: "", acceptanceValues: ["Positive", "Negative"], required: false },
      { name: "Plate Quality", unit: "", acceptanceValues: ["Good", "Fair", "Poor"], required: true },
    ],
    acceptanceCriteria: {
      "Rf Value": { min: 0.1, max: 0.9, type: "range" },
      "Spot Color": { required: true, type: "categorical" },
      "Plate Quality": { acceptedValues: ["Good", "Fair"], type: "categorical" },
    },
  },

  TLC_FLAVONOID: {
    parameters: [
      { name: "Rf Value", unit: "", acceptanceRange: "0.2-0.8", required: true },
      { name: "Spot Color", unit: "", acceptanceValues: ["Yellow", "Orange", "Green", "Brown"], required: true },
      { name: "AlCl3 Response", unit: "", acceptanceValues: ["Positive", "Negative"], required: true },
      { name: "NP/PEG Response", unit: "", acceptanceValues: ["Fluorescent", "Non-fluorescent"], required: false },
    ],
    acceptanceCriteria: {
      "Rf Value": { min: 0.2, max: 0.8, type: "range" },
      "AlCl3 Response": { acceptedValues: ["Positive"], type: "categorical" },
    },
  },

  // HPLC Quality Control Parameters
  HPLC_QUANTITATIVE: {
    parameters: [
      { name: "Retention Time", unit: "min", acceptanceRange: "±0.5", required: true },
      { name: "Peak Area", unit: "mAU*min", acceptanceRange: "RSD ≤2%", required: true },
      { name: "Peak Height", unit: "mAU", acceptanceRange: "RSD ≤5%", required: true },
      { name: "Tailing Factor", unit: "", acceptanceRange: "≤2.0", required: true },
      { name: "Resolution", unit: "", acceptanceRange: "≥1.5", required: true },
      { name: "Injection Precision", unit: "%RSD", acceptanceRange: "≤2.0", required: true },
    ],
    acceptanceCriteria: {
      "Retention Time": { tolerance: 0.5, type: "tolerance" },
      "Peak Area": { rsdLimit: 2.0, type: "rsd" },
      "Peak Height": { rsdLimit: 5.0, type: "rsd" },
      "Tailing Factor": { max: 2.0, type: "maximum" },
      "Resolution": { min: 1.5, type: "minimum" },
      "Injection Precision": { max: 2.0, type: "maximum" },
    },
  },

  // Antimicrobial Assay Quality Control
  ANTIMICROBIAL_ECOLI: {
    parameters: [
      { name: "Zone Diameter", unit: "mm", acceptanceRange: "6-50", required: true },
      { name: "Zone Edge Clarity", unit: "", acceptanceValues: ["Clear", "Fuzzy", "Partial"], required: true },
      { name: "Growth Control", unit: "", acceptanceValues: ["Confluent", "Sparse", "No Growth"], required: true },
      { name: "Sterility Control", unit: "", acceptanceValues: ["Sterile", "Contaminated"], required: true },
      { name: "Positive Control", unit: "mm", acceptanceRange: "Standard ±10%", required: true },
    ],
    acceptanceCriteria: {
      "Zone Diameter": { min: 6, max: 50, type: "range" },
      "Zone Edge Clarity": { acceptedValues: ["Clear"], type: "categorical" },
      "Growth Control": { acceptedValues: ["Confluent"], type: "categorical" },
      "Sterility Control": { acceptedValues: ["Sterile"], type: "categorical" },
    },
  },

  // Antioxidant DPPH Assay Quality Control
  DPPH_ASSAY: {
    parameters: [
      { name: "IC50 Value", unit: "μg/mL", acceptanceRange: "1-1000", required: true },
      { name: "R² Value", unit: "", acceptanceRange: "≥0.95", required: true },
      { name: "Positive Control IC50", unit: "μg/mL", acceptanceRange: "Literature ±20%", required: true },
      { name: "Blank Absorbance", unit: "", acceptanceRange: "0.8-1.2", required: true },
      { name: "Curve Linearity", unit: "", acceptanceValues: ["Linear", "Non-linear"], required: true },
    ],
    acceptanceCriteria: {
      "IC50 Value": { min: 1, max: 1000, type: "range" },
      "R² Value": { min: 0.95, type: "minimum" },
      "Blank Absorbance": { min: 0.8, max: 1.2, type: "range" },
      "Curve Linearity": { acceptedValues: ["Linear"], type: "categorical" },
    },
  },

  // Heavy Metals Quality Control
  HEAVY_METALS_LEAD: {
    parameters: [
      { name: "Lead Concentration", unit: "ppm", acceptanceRange: "≤10", required: true },
      { name: "Recovery %", unit: "%", acceptanceRange: "80-120", required: true },
      { name: "RSD", unit: "%", acceptanceRange: "≤10", required: true },
      { name: "Blank Value", unit: "ppm", acceptanceRange: "≤LOD", required: true },
      { name: "Spike Recovery", unit: "%", acceptanceRange: "85-115", required: true },
    ],
    acceptanceCriteria: {
      "Lead Concentration": { max: 10, type: "maximum" },
      "Recovery %": { min: 80, max: 120, type: "range" },
      "RSD": { max: 10, type: "maximum" },
      "Spike Recovery": { min: 85, max: 115, type: "range" },
    },
  },

  // Microbial Contamination Quality Control
  MICROBIAL_TOTAL: {
    parameters: [
      { name: "Total Count", unit: "CFU/g", acceptanceRange: "≤10⁵", required: true },
      { name: "Plate Count Validity", unit: "", acceptanceValues: ["Valid", "Invalid"], required: true },
      { name: "Dilution Factor", unit: "", acceptanceRange: "10¹-10⁶", required: true },
      { name: "Incubation Time", unit: "hours", acceptanceRange: "24-48", required: true },
      { name: "Positive Control", unit: "CFU/g", acceptanceRange: "Expected ±1 log", required: true },
    ],
    acceptanceCriteria: {
      "Total Count": { max: 100000, type: "maximum" },
      "Plate Count Validity": { acceptedValues: ["Valid"], type: "categorical" },
      "Incubation Time": { min: 24, max: 48, type: "range" },
    },
  },

  // Stability Testing Quality Control
  STABILITY_ACCELERATED: {
    parameters: [
      { name: "Temperature", unit: "°C", acceptanceRange: "40±2", required: true },
      { name: "Humidity", unit: "%RH", acceptanceRange: "75±5", required: true },
      { name: "Assay %", unit: "%", acceptanceRange: "90-110", required: true },
      { name: "Degradation Products", unit: "%", acceptanceRange: "≤5", required: true },
      { name: "Physical Appearance", unit: "", acceptanceValues: ["Unchanged", "Slight Change", "Significant Change"], required: true },
    ],
    acceptanceCriteria: {
      "Temperature": { target: 40, tolerance: 2, type: "tolerance" },
      "Humidity": { target: 75, tolerance: 5, type: "tolerance" },
      "Assay %": { min: 90, max: 110, type: "range" },
      "Degradation Products": { max: 5, type: "maximum" },
      "Physical Appearance": { acceptedValues: ["Unchanged", "Slight Change"], type: "categorical" },
    },
  },
};

/**
 * Validate QC results against acceptance criteria
 * @param {string} testId - The test ID
 * @param {Object} results - The QC results to validate
 * @returns {Object} Validation result with pass/fail and details
 */
export const validateQCResults = (testId, results) => {
  const template = TMMRD_QC_TEMPLATES[testId];
  if (!template) {
    return { valid: false, error: "No QC template found for test" };
  }

  const validationResults = {
    overall: "PASS",
    parameters: [],
    errors: [],
    warnings: [],
  };

  template.parameters.forEach(param => {
    const result = results[param.name];
    const criteria = template.acceptanceCriteria[param.name];

    if (!result && param.required) {
      validationResults.parameters.push({
        name: param.name,
        value: result,
        status: "FAIL",
        message: "Required parameter missing",
      });
      validationResults.overall = "FAIL";
      validationResults.errors.push(`${param.name}: Required parameter missing`);
      return;
    }

    if (!result) {
      validationResults.parameters.push({
        name: param.name,
        value: result,
        status: "NOT_TESTED",
        message: "Parameter not provided",
      });
      return;
    }

    let paramStatus = "PASS";
    let message = "Within acceptance criteria";

    if (criteria) {
      switch (criteria.type) {
        case "range":
          const numValue = parseFloat(result);
          if (isNaN(numValue) || numValue < criteria.min || numValue > criteria.max) {
            paramStatus = "FAIL";
            message = `Value ${result} outside range ${criteria.min}-${criteria.max}`;
          }
          break;

        case "maximum":
          const maxValue = parseFloat(result);
          if (isNaN(maxValue) || maxValue > criteria.max) {
            paramStatus = "FAIL";
            message = `Value ${result} exceeds maximum ${criteria.max}`;
          }
          break;

        case "minimum":
          const minValue = parseFloat(result);
          if (isNaN(minValue) || minValue < criteria.min) {
            paramStatus = "FAIL";
            message = `Value ${result} below minimum ${criteria.min}`;
          }
          break;

        case "tolerance":
          const tolValue = parseFloat(result);
          const target = criteria.target;
          const tolerance = criteria.tolerance;
          if (isNaN(tolValue) || Math.abs(tolValue - target) > tolerance) {
            paramStatus = "FAIL";
            message = `Value ${result} outside tolerance range ${target}±${tolerance}`;
          }
          break;

        case "rsd":
          const rsdValue = parseFloat(result);
          if (isNaN(rsdValue) || rsdValue > criteria.rsdLimit) {
            paramStatus = "FAIL";
            message = `RSD ${result}% exceeds limit ${criteria.rsdLimit}%`;
          }
          break;

        case "categorical":
          if (!criteria.acceptedValues.includes(result)) {
            paramStatus = "FAIL";
            message = `Value '${result}' not in accepted values: ${criteria.acceptedValues.join(', ')}`;
          }
          break;

        default:
          paramStatus = "UNKNOWN";
          message = "Unknown validation criteria type";
      }
    }

    validationResults.parameters.push({
      name: param.name,
      value: result,
      status: paramStatus,
      message: message,
      unit: param.unit,
    });

    if (paramStatus === "FAIL") {
      validationResults.overall = "FAIL";
      validationResults.errors.push(`${param.name}: ${message}`);
    }
  });

  return validationResults;
};

/**
 * Get QC template for a specific test
 * @param {string} testId - The test ID
 * @returns {Object|null} QC template or null if not found
 */
export const getQCTemplate = (testId) => {
  return TMMRD_QC_TEMPLATES[testId] || null;
};

/**
 * Get all available QC templates
 * @returns {Object} All QC templates
 */
export const getAllQCTemplates = () => {
  return TMMRD_QC_TEMPLATES;
};

/**
 * Generate empty QC results object for a test
 * @param {string} testId - The test ID
 * @returns {Object} Empty QC results object
 */
export const generateEmptyQCResults = (testId) => {
  const template = TMMRD_QC_TEMPLATES[testId];
  if (!template) return {};

  const emptyResults = {};
  template.parameters.forEach(param => {
    emptyResults[param.name] = "";
  });

  return emptyResults;
};

export default {
  TMMRD_QC_TEMPLATES,
  validateQCResults,
  getQCTemplate,
  getAllQCTemplates,
  generateEmptyQCResults,
};