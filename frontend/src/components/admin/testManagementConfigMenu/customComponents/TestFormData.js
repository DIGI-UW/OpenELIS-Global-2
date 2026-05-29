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
  qcBlankThreshold: "",
  qcRpdThreshold: "",
  qcRecoveryWindowPct: "",
  timeHolding: "",
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

// Pick the largest non-zero unit (Y > M > D) to represent a (d,m,y) triple as
// a single number+unit pair.
const pickAgePart = (d, m, y) => {
  if (y > 0) return { raw: y, unit: "Y" };
  if (m > 0) return { raw: m, unit: "M" };
  if (d > 0) return { raw: d, unit: "D" };
  return { raw: 0, unit: "Y" };
};

// Of two units, return the finer-granularity one (D > M > Y).
const finerUnit = (a, b) => {
  const order = { Y: 0, M: 1, D: 2 };
  return (order[a] ?? 0) >= (order[b] ?? 0) ? a : b;
};

// Convert a (d,m,y) triple to a single rounded value in the chosen unit.
const valueInUnit = (d, m, y, unit) => {
  if (unit === "Y") return Math.round(y + m / 12 + d / 365);
  if (unit === "M") return Math.round(y * 12 + m + d / 30);
  return Math.round(y * 365 + m * 30 + d);
};

const parseDMY = (ageStr) => {
  let d = 0,
    m = 0,
    y = 0;
  for (let part of ageStr.split("/")) {
    part = part.trim().toUpperCase();
    if (part.endsWith("D")) d = parseInt(part.slice(0, -1), 10) || 0;
    else if (part.endsWith("M")) m = parseInt(part.slice(0, -1), 10) || 0;
    else if (part.endsWith("Y")) y = parseInt(part.slice(0, -1), 10) || 0;
  }
  return { d, m, y };
};

export const extractAgeRangeParts = (rangeStr) => {
  if (!rangeStr || typeof rangeStr !== "string") {
    return {
      low: { raw: "0", unit: "Y" },
      high: { raw: "Infinity", unit: "Y" },
    };
  }

  // Format B (open-ended high): ">NYNMND" — emitted by the backend when
  // maxAge is +Infinity. No "-" separator and no "/" between segments.
  const openHigh = rangeStr
    .trim()
    .match(/^>\s*(\d+)\s*Y\s*(\d+)\s*M\s*(\d+)\s*D\s*$/i);
  if (openHigh) {
    const y = parseInt(openHigh[1], 10) || 0;
    const m = parseInt(openHigh[2], 10) || 0;
    const d = parseInt(openHigh[3], 10) || 0;
    const low = pickAgePart(d, m, y);
    return { low, high: { raw: "Infinity", unit: low.unit } };
  }

  // Format A (bounded): "DD/MM/YY-DD/MM/YY"
  const [start, end] = rangeStr.split("-");
  const lowParts = start ? parseDMY(start) : { d: 0, m: 0, y: 0 };
  const lowPicked = pickAgePart(lowParts.d, lowParts.m, lowParts.y);

  if (!end) {
    return { low: lowPicked, high: { raw: "Infinity", unit: lowPicked.unit } };
  }

  const highParts = parseDMY(end);
  const highPicked = pickAgePart(highParts.d, highParts.m, highParts.y);

  // The form has a single Y/M/D radio per row, so both ends must share a
  // unit. Pick the finer-granularity one so e.g. "8M to 1Y" displays as
  // "8 to 12 (M)" instead of "8 to 1 (Y)".
  const unit = finerUnit(lowPicked.unit, highPicked.unit);
  const lowRaw =
    unit === lowPicked.unit
      ? lowPicked.raw
      : valueInUnit(lowParts.d, lowParts.m, lowParts.y, unit);
  const highRaw =
    unit === highPicked.unit
      ? highPicked.raw
      : valueInUnit(highParts.d, highParts.m, highParts.y, unit);

  return { low: { raw: lowRaw, unit }, high: { raw: highRaw, unit } };
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
    qcBlankThreshold: test.qcBlankThreshold || "",
    qcRpdThreshold: test.qcRpdThreshold || "",
    qcRecoveryWindowPct: test.qcRecoveryWindowPct || "",
    timeHolding: test.timeHolding || "",
    active: test.active === "Active" ? "Y" : "N",
    dictionary: test.dictionaryValues || [],
    dictionaryReference: Number.isNaN(Number(test.referenceValue))
      ? ""
      : test.referenceValue,
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
