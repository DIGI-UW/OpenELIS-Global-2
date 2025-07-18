export const TestFormData = {
  testNameEnglish: "",
  testNameFrench: "",
  testReportNameEnglish: "",
  testReportNameFrench: "",
  testSection: "",
  panels: [],
  uom: "",
  loinc: "",
  resultType: "",
  orderable: "Y",
  notifyResults: "N",
  inLabOnly: "N",
  antimicrobialResistance: "N",
  active: "Y",
  dictionary: [],
  dictionaryReference: "",
  defaultTestResult: "",
  sampleTypes: [],
  lowValid: "-Infinity",
  highValid: "Infinity",
  lowReportingRange: "-Infinity",
  highReportingRange: "Infinity",
  lowCritical: "-Infinity",
  highCritical: "Infinity",
  significantDigits: "0",
  resultLimits: [
    {
      ageRange: "0",
      highAgeRange: "0",
      gender: false,
      lowNormal: "-Infinity",
      highNormal: "Infinity",
      lowNormalFemale: "-Infinity",
      highNormalFemale: "Infinity",
    },
  ],
};

export const extractAgeRangeParts = (rangeStr) => {
  const [start, end] = rangeStr.split("-");

  const parseAge = (ageStr) => {
    const parts = ageStr.split("/");

    let d = 0,
      m = 0,
      y = 0;

    for (let part of parts) {
      part = part.trim().toUpperCase();
      if (part.endsWith("D")) d = parseInt(part.replace("D", ""), 10);
      if (part.endsWith("M")) m = parseInt(part.replace("M", ""), 10);
      if (part.endsWith("Y")) y = parseInt(part.replace("Y", ""), 10);
    }

    if (y > 0) return { raw: y, unit: "Y" };
    if (m > 0) return { raw: m, unit: "M" };
    if (d > 0) return { raw: d, unit: "D" };

    return { raw: 0, unit: "Y" };
  };

  const low = start ? parseAge(start) : "";
  const high = end ? parseAge(end) : "";

  return { low, high };
};

export const mapTestCatBeanToFormData = (test) => {
  return {
    testId: test.id,
    testNameEnglish: test.localization?.english || "",
    testNameFrench: test.localization?.french || "",
    testReportNameEnglish: test.reportLocalization?.english || "",
    testReportNameFrench: test.reportLocalization?.french || "",
    testSection: test.testUnit || "",
    panels:
      typeof test.panel === "string" && test.panel !== "None"
        ? test.panel.split(",").map((p) => p.trim())
        : [],
    uom: test.uom || "",
    loinc: test.loinc || "",
    resultType: test.resultType || "",
    orderable: test.orderable === "Orderable" ? "Y" : "N",
    notifyResults: test.notifyResults ? "Y" : "N",
    inLabOnly: test.inLabOnly ? "Y" : "N",
    antimicrobialResistance: test.antimicrobialResistance ? "Y" : "N",
    active: test.active === "Active" ? "Y" : "N",
    dictionary: test.dictionaryValues || [],
    dictionaryReference:
      test.referenceValue !== "n/a" ? test.referenceValue : "",
    defaultTestResult: "",
    sampleTypes: test.sampleType ? [test.sampleType] : [],
    lowValid:
      test.resultLimits?.[0]?.validRange?.split("-")?.[0] || "-Infinity",
    highValid:
      test.resultLimits?.[0]?.validRange?.split("-")?.[1] || "Infinity",
    lowReportingRange:
      test.resultLimits?.[0]?.reportingRange?.split("-")?.[0] || "-Infinity",
    highReportingRange:
      test.resultLimits?.[0]?.reportingRange?.split("-")?.[1] || "Infinity",
    lowCritical:
      test.resultLimits?.[0]?.criticalRange?.split("-")?.[0] || "-Infinity",
    highCritical:
      test.resultLimits?.[0]?.criticalRange?.split("-")?.[1] || "Infinity",
    significantDigits:
      test.significantDigits !== "n/a" ? test.significantDigits : "0",
    resultLimits: Object.entries(
      (test.resultLimits || []).reduce((acc, limit) => {
        const key = limit.ageRange;
        if (!acc[key]) acc[key] = [];
        acc[key].push(limit);
        return acc;
      }, {}),
    ).map(([ageRange, limits]) => {
      const result = {
        ageRange,
        highAgeRange: "0",
        gender: false,
        lowNormal: "-Infinity",
        highNormal: "Infinity",
        lowNormalFemale: "-Infinity",
        highNormalFemale: "Infinity",
      };

      limits.forEach((limit) => {
        let low = "-Infinity",
          high = "Infinity";
        if (limit.normalRange && limit.normalRange !== "Any value") {
          [low, high] = limit.normalRange.split("-");
        }

        if (limit.gender === "M") {
          result.gender = true;
          result.lowNormal = low || "-Infinity";
          result.highNormal = high || "Infinity";
        } else if (limit.gender === "F") {
          result.gender = true;
          result.lowNormalFemale = low || "-Infinity";
          result.highNormalFemale = high || "Infinity";
        } else if (limit.gender === "n/a") {
          result.lowNormal = low || "-Infinity";
          result.highNormal = high || "Infinity";
        }
      });

      return result;
    }),
  };
};
