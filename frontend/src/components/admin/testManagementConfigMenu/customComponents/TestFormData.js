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
      highAgeRange: "Infinity",
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

  const low = start ? parseAge(start) : { raw: "0", unit: "Y" };
  const high = end ? parseAge(end) : { raw: "Infinity", unit: "Y" };

  return { low, high };
};

const isNumericRange = (str) => {
  if (typeof str !== "string") {
    return false;
  }
  const rangeRegex = /^\s*\d+(\.\d+)?\s*-\s*\d+(\.\d+)?\s*$/;
  return rangeRegex.test(str);
};

const extractRange = (rangeStr) => {
  if (!isNumericRange(rangeStr)) {
    return ["-Infinity", "Infinity"];
  }

  const parts = rangeStr?.split("-") || [];
  const low = parts[0]?.trim() || "-Infinity";
  const high = parts[1]?.trim() || "Infinity";

  return [low, high];
};

export const mapTestCatBeanToFormData = (test) => {
  console.log(JSON.stringify(test));
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
    lowValid: extractRange(test.resultLimits?.[0]?.validRange)[0],
    highValid: extractRange(test.resultLimits?.[0]?.validRange)[1],
    lowReportingRange: extractRange(test.resultLimits?.[0]?.reportingRange)[0],
    highReportingRange: extractRange(test.resultLimits?.[0]?.reportingRange)[1],
    lowCritical: extractRange(test.resultLimits?.[0]?.criticalRange)[0],
    highCritical: extractRange(test.resultLimits?.[0]?.criticalRange)[1],
    significantDigits: test.significantDigits
      ? test.significantDigits !== "n/a"
        ? test.significantDigits
        : "0"
      : "0",
    resultLimits:
      (test.resultLimits || []).length === 0
        ? [
            {
              ageRange: "0",
              highAgeRange: "Infinity",
              gender: false,
              lowNormal: "-Infinity",
              highNormal: "Infinity",
            },
          ]
        : Object.entries(
            (test.resultLimits || []).reduce((acc, limit) => {
              const key = limit.ageRange;
              if (!acc[key]) acc[key] = [];
              acc[key].push(limit);
              return acc;
            }, {}),
          ).map(([ageRange, limits]) => {
            const result = {
              ageRange,
              highAgeRange: "Infinity",
              gender: false,
              lowNormal: "-Infinity",
              highNormal: "Infinity",
              lowNormalFemale: "-Infinity",
              highNormalFemale: "Infinity",
            };

            limits.forEach((limit) => {
              let low = "-Infinity",
                high = "Infinity";

              if (isNumericRange(limit.normalRange)) {
                const parts = limit.normalRange.split("-");
                low = parts[0]?.trim() || "-Infinity";
                high = parts[1]?.trim() || "Infinity";
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
