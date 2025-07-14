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
    panels: test.panel && test.panel !== "None" ? [test.panel] : [],
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
    lowValid: "-Infinity", // this may be needs to fetched from resultLimits collection
    highValid: "Infinity",
    lowReportingRange: "-Infinity",
    highReportingRange: "Infinity",
    lowCritical: "-Infinity",
    highCritical: "Infinity",
    significantDigits:
      test.significantDigits !== "n/a" ? test.significantDigits : "0",
    // resultLimits:
    //   test.resultLimits?.map((limit) => {
    //     const normalRange = limit.normalRange?.split("-") || [];
    //     return {
    //       ageRange: limit.ageRange || "0",
    //       highAgeRange: "0",
    //       gender:
    //         limit.gender === "n/a"
    //           ? false
    //           : limit.gender === "M"
    //             ? true
    //             : limit.gender === "F"
    //               ? true
    //               : false,
    //       lowNormal: normalRange[0] || "-Infinity",
    //       highNormal: normalRange[1] || "Infinity",
    //       lowNormalFemale: normalRange[0] || "-Infinity",
    //       highNormalFemale: normalRange[1] || "Infinity",
    //     };
    //   }) || [],
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
        const [low, high] = limit.normalRange?.split("-") || [];

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

// {
//     "id": "381",
//     "localization": {
//         "lastupdated": 1751896824848,
//         "id": "562",
//         "description": "test name",
//         "localeValues": {
//             "fr": "democ",
//             "en": "democ"
//         },
//         "localizedValue": "democ",
//         "localesWithValue": [
//             "en",
//             "fr"
//         ],
//         "english": "democ",
//         "french": "democ",
//         "allActiveLocales": [
//             "en",
//             "fr"
//         ],
//         "localesSortedForDisplay": [
//             "en",
//             "fr"
//         ],
//         "localesWithValueSortedForDisplay": [
//             "en",
//             "fr"
//         ],
//         "localesAndValuesOfLocalesWithValues": [
//             "English: democ",
//             "French: democ"
//         ]
//     },
//     "reportLocalization": {
//         "lastupdated": 1751896824849,
//         "id": "563",
//         "description": "test report name",
//         "localeValues": {
//             "fr": "democ",
//             "en": "democ"
//         },
//         "localizedValue": "democ",
//         "localesWithValue": [
//             "en",
//             "fr"
//         ],
//         "english": "democ",
//         "french": "democ",
//         "allActiveLocales": [
//             "en",
//             "fr"
//         ],
//         "localesSortedForDisplay": [
//             "en",
//             "fr"
//         ],
//         "localesWithValueSortedForDisplay": [
//             "en",
//             "fr"
//         ],
//         "localesAndValuesOfLocalesWithValues": [
//             "English: democ",
//             "French: democ"
//         ]
//     },
//     "testUnit": "Biochemistry",
//     "sampleType": "Sputum",
//     "panel": "Bilan Biochimique",
//     "resultType": "N",
//     "uom": "cp/mL",
//     "significantDigits": "1",
//     "loinc": "1",
//     "active": "Active",
//     "orderable": "Orderable",
//     "notifyResults": true,
//     "hasDictionaryValues": false,
//     "hasLimitValues": true,
//     "resultLimits": [
//         {
//             "gender": "M",
//             "ageRange": "0D/0M/0Y-10D/0M/0Y",
//             "normalRange": "Any value",
//             "validRange": "Any value",
//             "reportingRange": "Any value",
//             "criticalRange": "Any value"
//         },
//         {
//             "gender": "F",
//             "ageRange": "0D/0M/0Y-10D/0M/0Y",
//             "normalRange": "Any value",
//             "validRange": "Any value",
//             "reportingRange": "Any value",
//             "criticalRange": "Any value"
//         },
//         {
//             "gender": "M",
//             "ageRange": "10D/0M/0Y-20D/0M/0Y",
//             "normalRange": "1.0-2.0",
//             "validRange": "Any value",
//             "reportingRange": "Any value",
//             "criticalRange": "Any value"
//         },
//         {
//             "gender": "F",
//             "ageRange": "10D/0M/0Y-20D/0M/0Y",
//             "normalRange": "1.0-2.0",
//             "validRange": "Any value",
//             "reportingRange": "Any value",
//             "criticalRange": "Any value"
//         },
//         {
//             "gender": "n/a",
//             "ageRange": "20D/0M/0Y-0D/1M/0Y",
//             "normalRange": "Any value",
//             "validRange": "Any value",
//             "reportingRange": "Any value",
//             "criticalRange": "Any value"
//         },
//         {
//             "gender": "n/a",
//             "ageRange": "0D/1M/0Y-10D/1M/0Y",
//             "normalRange": "1.0-2.0",
//             "validRange": "Any value",
//             "reportingRange": "Any value",
//             "criticalRange": "Any value"
//         }
//     ],
//     "testSortOrder": 3,
//     "inLabOnly": true,
//     "antimicrobialResistance": true
// }

// {
//     "testId": "381",
//     "testNameEnglish": "democ",
//     "testNameFrench": "democ",
//     "testReportNameEnglish": "democ",
//     "testReportNameFrench": "democ",
//     "testSection": "Biochemistry",
//     "panels": [
//         "Bilan Biochimique"
//     ],
//     "uom": "cp/mL",
//     "loinc": "1",
//     "resultType": "N",
//     "orderable": "Y",
//     "notifyResults": "Y",
//     "inLabOnly": "Y",
//     "antimicrobialResistance": "Y",
//     "active": "Y",
//     "dictionary": [],
//     "dictionaryReference": "",
//     "defaultTestResult": "",
//     "sampleTypes": [
//         "Sputum"
//     ],
//     "lowValid": "-Infinity",
//     "highValid": "Infinity",
//     "lowReportingRange": "-Infinity",
//     "highReportingRange": "Infinity",
//     "lowCritical": "-Infinity",
//     "highCritical": "Infinity",
//     "significantDigits": "1",
//     "resultLimits": [
//         {
//             "ageRange": "0D/0M/0Y-10D/0M/0Y",
//             "highAgeRange": "0",
//             "gender": true,
//             "lowNormal": "Any value",
//             "highNormal": "Infinity",
//             "lowNormalFemale": "Any value",
//             "highNormalFemale": "Infinity"
//         },
//         {
//             "ageRange": "0D/0M/0Y-10D/0M/0Y",
//             "highAgeRange": "0",
//             "gender": true,
//             "lowNormal": "Any value",
//             "highNormal": "Infinity",
//             "lowNormalFemale": "Any value",
//             "highNormalFemale": "Infinity"
//         },
//         {
//             "ageRange": "10D/0M/0Y-20D/0M/0Y",
//             "highAgeRange": "0",
//             "gender": true,
//             "lowNormal": "1.0",
//             "highNormal": "2.0",
//             "lowNormalFemale": "1.0",
//             "highNormalFemale": "2.0"
//         },
//         {
//             "ageRange": "10D/0M/0Y-20D/0M/0Y",
//             "highAgeRange": "0",
//             "gender": true,
//             "lowNormal": "1.0",
//             "highNormal": "2.0",
//             "lowNormalFemale": "1.0",
//             "highNormalFemale": "2.0"
//         },
//         {
//             "ageRange": "20D/0M/0Y-0D/1M/0Y",
//             "highAgeRange": "0",
//             "gender": false,
//             "lowNormal": "Any value",
//             "highNormal": "Infinity",
//             "lowNormalFemale": "Any value",
//             "highNormalFemale": "Infinity"
//         },
//         {
//             "ageRange": "0D/1M/0Y-10D/1M/0Y",
//             "highAgeRange": "0",
//             "gender": false,
//             "lowNormal": "1.0",
//             "highNormal": "2.0",
//             "lowNormalFemale": "1.0",
//             "highNormalFemale": "2.0"
//         }
//     ]
// }
