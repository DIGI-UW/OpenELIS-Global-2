/**
 * Pure helpers for biorepository manifest import (legacy AHRI Excel + CSV).
 */

export const MANIFEST_FIELDS = [
  "barcode",
  "externalId",
  "sampleType",
  "projectId",
  "originLab",
  "receiptDate",
  "requiredTempMin",
  "requiredTempMax",
  "biosafetyLevel",
  "collectionDate",
  "principalInvestigator",
  "consentId",
  "ethicsApprovalRef",
  "mtaReference",
  "preservationMedium",
  "arrivalCondition",
  "specialHandling",
];

export const HEADER_ALIASES = {
  barcode: "barcode",
  sampleid: "barcode",
  samplebarcode: "barcode",
  externalid: "externalId",
  labid: "externalId",
  sampletype: "sampleType",
  samplegiventobiorepository: "biorepositorySampleType",
  sampletypeid: "sampleType",
  projectid: "projectId",
  projectname: "projectId",
  originlab: "originLab",
  transferingunit: "originLab",
  receiptdate: "receiptDate",
  requestdate: "receiptDate",
  transferdate: "collectionDate",
  requiredtempmin: "requiredTempMin",
  requiredtempmax: "requiredTempMax",
  biosafetylevel: "biosafetyLevel",
  collectiondate: "collectionDate",
  principalinvestigator: "principalInvestigator",
  consentid: "consentId",
  ethicsapprovalref: "ethicsApprovalRef",
  mtareference: "mtaReference",
  preservationmedium: "preservationMedium",
  preservativeormedium: "preservationMedium",
  arrivalcondition: "arrivalCondition",
  samplecondition: "arrivalCondition",
  specialhandling: "specialHandling",
  notes: "specialHandling",
  comments: "specialHandling",
  volume: "volume",
};

/** Unmapped AHRI template columns preserved in specialHandling notes. */
export const STORAGE_METADATA_ALIASES = {
  zone: "Zone",
  freezerno: "Freezer",
  shelfno: "Shelf",
  rackno: "Rack",
  boxno: "Box",
  location: "Location",
  qcstatus: "QC Status",
  deviationorincident: "Deviation",
  transferredby: "Transferred By",
  transferreason: "Transfer Reason",
  transferbatchnumber: "Transfer Batch",
};

export const FIRST_WINS_MANIFEST_FIELDS = new Set([
  "receiptDate",
  "barcode",
  "externalId",
]);

export const normalizeHeaderToken = (header) =>
  String(header || "")
    .trim()
    .replace(/[^a-zA-Z0-9]/g, "")
    .toLowerCase();

const pad2 = (n) => String(n).padStart(2, "0");

/**
 * Normalize common manifest date strings to yyyy-MM-dd or yyyy-MM-dd HH:mm:ss.
 */
export const normalizeDateValue = (value, allowTime = true) => {
  if (value === undefined || value === null) {
    return "";
  }
  if (value instanceof Date) {
    return value.toISOString().slice(0, 19).replace("T", " ");
  }

  const trimmed = String(value).trim();
  if (!trimmed) {
    return "";
  }

  const dotDateTime = trimmed.match(
    /^(\d{2})\.(\d{2})\.(\d{4})(?:[ T](\d{2}):(\d{2})(?::(\d{2}))?)?$/,
  );
  if (dotDateTime) {
    const [, dd, mm, yyyy, hh, min, sec] = dotDateTime;
    const datePart = `${yyyy}-${mm}-${dd}`;
    if (hh !== undefined && allowTime) {
      return `${datePart} ${pad2(hh)}:${pad2(min)}:${pad2(sec || "00")}`;
    }
    return datePart;
  }

  const slashDateTime = trimmed.match(
    /^(\d{2})\/(\d{2})\/(\d{4})(?:[ T](\d{2}):(\d{2})(?::(\d{2}))?)?$/,
  );
  if (slashDateTime) {
    const [, mm, dd, yyyy, hh, min, sec] = slashDateTime;
    const datePart = `${yyyy}-${mm}-${dd}`;
    if (hh !== undefined && allowTime) {
      return `${datePart} ${pad2(hh)}:${pad2(min)}:${pad2(sec || "00")}`;
    }
    return datePart;
  }

  const dashDateTime = trimmed.match(
    /^(\d{2})-(\d{2})-(\d{4})(?:[ T](\d{2}):(\d{2})(?::(\d{2}))?)?$/,
  );
  if (dashDateTime) {
    const [, dd, mm, yyyy, hh, min, sec] = dashDateTime;
    const datePart = `${yyyy}-${mm}-${dd}`;
    if (hh !== undefined && allowTime) {
      return `${datePart} ${pad2(hh)}:${pad2(min)}:${pad2(sec || "00")}`;
    }
    return datePart;
  }

  if (/^\d{4}-\d{2}-\d{2}/.test(trimmed)) {
    return trimmed.replace("T", " ");
  }

  return trimmed;
};

export const normalizeCellValue = (value) => {
  if (value === undefined || value === null) {
    return "";
  }
  if (value instanceof Date) {
    return value.toISOString().slice(0, 19).replace("T", " ");
  }
  const trimmed = String(value).trim();
  if (!trimmed) {
    return "";
  }
  const normalizedDate = normalizeDateValue(trimmed, true);
  if (normalizedDate !== trimmed && isSupportedDateValue(normalizedDate, true)) {
    return normalizedDate;
  }
  return trimmed;
};

export const isSupportedDateValue = (value, allowTime = false) => {
  if (!value) {
    return false;
  }

  const patterns = allowTime
    ? [
        /^\d{4}-\d{2}-\d{2}$/,
        /^\d{4}-\d{2}-\d{2}[ T]\d{2}:\d{2}(:\d{2})?$/,
        /^\d{2}\/\d{2}\/\d{4}$/,
        /^\d{2}\/\d{2}\/\d{4}[ T]\d{2}:\d{2}(:\d{2})?$/,
        /^\d{2}-\d{2}-\d{4}$/,
        /^\d{2}-\d{2}-\d{4}[ T]\d{2}:\d{2}(:\d{2})?$/,
        /^\d{2}\.\d{2}\.\d{4}$/,
        /^\d{2}\.\d{2}\.\d{4}[ T]\d{2}:\d{2}(:\d{2})?$/,
      ]
    : [
        /^\d{4}-\d{2}-\d{2}$/,
        /^\d{2}\/\d{2}\/\d{4}$/,
        /^\d{2}-\d{2}-\d{4}$/,
        /^\d{2}\.\d{2}\.\d{4}$/,
      ];

  return patterns.some((pattern) => pattern.test(value));
};

export const firstNonEmptyValue = (...values) =>
  values.find(
    (value) =>
      value !== undefined && value !== null && String(value).trim() !== "",
  ) || "";

export const formatCurrentReceiptDate = () =>
  new Date().toISOString().slice(0, 19).replace("T", " ");

/**
 * Map raw header row + values into manifest row object.
 * receiptDate keeps first non-empty; storage columns go to _storageNotes.
 */
export const mergeMappedRowValues = (rawHeaders, values) => {
  const row = {};
  const storageParts = [];

  rawHeaders.forEach((rawHeader, index) => {
    const token = normalizeHeaderToken(rawHeader);
    const mappedField = HEADER_ALIASES[token];
    const storageLabel = STORAGE_METADATA_ALIASES[token];
    const nextValue = normalizeCellValue(values[index]);

    if (!nextValue) {
      return;
    }

    if (mappedField) {
      if (FIRST_WINS_MANIFEST_FIELDS.has(mappedField) && row[mappedField]) {
        return;
      }
      if (row[mappedField] && !nextValue) {
        return;
      }
      row[mappedField] = row[mappedField] || nextValue;
      return;
    }

    if (storageLabel) {
      storageParts.push(`${storageLabel}: ${nextValue}`);
    }
  });

  if (storageParts.length > 0) {
    row._storageNotes = storageParts.join(" | ");
  }

  return row;
};

export const buildSpecialHandlingNotes = (row) =>
  [
    row.specialHandling,
    row._storageNotes,
    row.volume && row.volume.toLowerCase() !== "sufficient"
      ? `Volume: ${row.volume}`
      : "",
  ]
    .filter(Boolean)
    .join(" | ");

export const inferLegacyTemperatureRange = (sampleType, sheetName) => {
  const normalized = `${sampleType || ""} ${sheetName || ""}`.toLowerCase();

  if (normalized.includes("pbmc") || normalized.includes("cell line")) {
    return { min: "-196", max: "-150" };
  }

  if (
    normalized.includes("ffpe") ||
    normalized.includes("block") ||
    normalized.includes("paraffin")
  ) {
    return { min: "20", max: "25" };
  }

  return { min: "-80", max: "-20" };
};

export const inferLegacyBiosafetyLevel = (sampleType, sheetName) => {
  const normalized = `${sampleType || ""} ${sheetName || ""}`.toLowerCase();
  if (
    normalized.includes("bacter") ||
    normalized.includes("culture") ||
    normalized.includes("isolate") ||
    normalized.includes("pseudomonas") ||
    normalized.includes("k.") ||
    normalized.includes("e. coli") ||
    normalized.includes("staph") ||
    normalized.includes("enterococcus")
  ) {
    return "BSL_2";
  }
  return "BSL_2";
};

export const normalizeLegacyDuplicateBarcodes = (rows) => {
  const seen = new Map();

  return rows.map((row) => {
    const normalizedRow = Array.isArray(row) ? [...row] : row;
    const barcode = normalizeCellValue(normalizedRow[0]);

    if (!barcode) {
      return normalizedRow;
    }

    const seenCount = seen.get(barcode) || 0;
    seen.set(barcode, seenCount + 1);

    if (seenCount === 0) {
      normalizedRow[1] = normalizeCellValue(normalizedRow[1]) || barcode;
      return normalizedRow;
    }

    const duplicateIndex = seenCount + 1;
    normalizedRow[0] = `${barcode}-R${duplicateIndex}`;
    normalizedRow[1] = normalizeCellValue(normalizedRow[1]) || barcode;

    const duplicateNote = `Original Sample ID: ${barcode}`;
    const existingSpecialHandling = normalizeCellValue(normalizedRow[16]);
    normalizedRow[16] = existingSpecialHandling
      ? `${existingSpecialHandling} | ${duplicateNote}`
      : duplicateNote;

    return normalizedRow;
  });
};

export const convertLegacyWorksheetRows = (rows, sheetName) => {
  const normalizedRows = rows
    .map((row) =>
      Array.isArray(row) ? row.map((value) => normalizeCellValue(value)) : [],
    )
    .filter((row) => row.some((value) => value !== ""));

  if (normalizedRows.length < 2) {
    return [];
  }

  const rawHeaders = normalizedRows[0];
  const mappedHeaders = rawHeaders.map(
    (header) => HEADER_ALIASES[normalizeHeaderToken(header)] || null,
  );

  const looksLegacyWorkbook =
    mappedHeaders.includes("barcode") &&
    mappedHeaders.includes("originLab") &&
    mappedHeaders.includes("projectId");

  if (!looksLegacyWorkbook) {
    return [];
  }

  const convertedRows = [];

  for (let i = 1; i < normalizedRows.length; i++) {
    const values = normalizedRows[i];
    const row = mergeMappedRowValues(rawHeaders, values);

    const sampleType = firstNonEmptyValue(
      row.biorepositorySampleType,
      row.sampleType,
    );
    const barcode = row.barcode || "";
    if (!barcode) {
      continue;
    }

    const tempRange = inferLegacyTemperatureRange(sampleType, sheetName);
    const receiptDate = normalizeDateValue(
      firstNonEmptyValue(
        row.receiptDate,
        row.collectionDate,
        formatCurrentReceiptDate(),
      ),
      true,
    );
    const collectionDate = normalizeDateValue(row.collectionDate, false);

    convertedRows.push([
      barcode,
      row.externalId,
      sampleType,
      row.projectId,
      row.originLab,
      receiptDate,
      firstNonEmptyValue(row.requiredTempMin, tempRange.min),
      firstNonEmptyValue(row.requiredTempMax, tempRange.max),
      firstNonEmptyValue(
        row.biosafetyLevel,
        inferLegacyBiosafetyLevel(sampleType, sheetName),
      ),
      collectionDate,
      row.principalInvestigator,
      row.consentId,
      row.ethicsApprovalRef,
      row.mtaReference,
      row.preservationMedium,
      row.arrivalCondition,
      buildSpecialHandlingNotes(row),
    ]);
  }

  return convertedRows;
};
