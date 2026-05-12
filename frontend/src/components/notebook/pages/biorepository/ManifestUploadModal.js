import React, { useState, useCallback } from "react";
import {
  Modal,
  FileUploader,
  Button,
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  InlineNotification,
  Loading,
  Tag,
} from "@carbon/react";
import { Checkmark, Warning, Download } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import PropTypes from "prop-types";
import * as XLSX from "xlsx";
import { postToOpenElisServerJsonResponse } from "../../../utils/Utils";
import {
  translateManifestImportMessage,
  translateManifestImportMessages,
} from "./manifestImportErrorMessages";

const MANIFEST_FIELDS = [
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

const REQUIRED_FIELDS = [
  "barcode",
  "sampleType",
  "originLab",
  "receiptDate",
  "requiredTempMin",
  "requiredTempMax",
];

const CONDITIONAL_FIELDS = [
  { name: "consentId", description: "Required for human samples" },
  { name: "ethicsApprovalRef", description: "Required for human samples" },
  { name: "mtaReference", description: "Required for external samples" },
];

const OPTIONAL_FIELDS = MANIFEST_FIELDS.filter(
  (field) => !REQUIRED_FIELDS.includes(field),
);

const HEADER_ALIASES = {
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
  transferdate: "receiptDate",
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

const normalizeHeaderToken = (header) =>
  String(header || "")
    .trim()
    .replace(/[^a-zA-Z0-9]/g, "")
    .toLowerCase();

const normalizeCellValue = (value) => {
  if (value === undefined || value === null) {
    return "";
  }
  if (value instanceof Date) {
    return value.toISOString().slice(0, 19).replace("T", " ");
  }
  return String(value).trim();
};

const formatCurrentReceiptDate = () =>
  new Date().toISOString().slice(0, 19).replace("T", " ");

const firstNonEmptyValue = (...values) =>
  values.find(
    (value) =>
      value !== undefined && value !== null && String(value).trim() !== "",
  ) || "";

const inferLegacyTemperatureRange = (sampleType, sheetName) => {
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

const inferLegacyBiosafetyLevel = (sampleType, sheetName) => {
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

const normalizeLegacyDuplicateBarcodes = (rows) => {
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

/**
 * ManifestUploadModal - manifest upload modal for bulk sample import
 * Part of Sub-stage 1b of the Biorepository Intake workflow
 *
 * Expected manifest format:
 * barcode,externalId,sampleType,projectId,originLab,receiptDate,requiredTempMin,requiredTempMax,...
 *
 * @param {Object} props
 * @param {boolean} props.open - Whether the modal is open
 * @param {Function} props.onClose - Callback to close the modal
 * @param {number} props.shipmentId - The shipment ID to associate samples with
 * @param {Function} props.onImportComplete - Callback when import is complete
 */
function ManifestUploadModal({ open, onClose, shipmentId, onImportComplete }) {
  const intl = useIntl();

  const [parsedData, setParsedData] = useState([]);
  const [validationErrors, setValidationErrors] = useState([]);
  const [validationWarnings, setValidationWarnings] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [importStatus, setImportStatus] = useState(null); // null, 'parsed', 'validating', 'preview', 'importing', 'complete'
  const [backendValidationDone, setBackendValidationDone] = useState(false);
  const [importResult, setImportResult] = useState(null);

  const requiredFields = REQUIRED_FIELDS;
  const conditionalFields = CONDITIONAL_FIELDS;
  const optionalFields = OPTIONAL_FIELDS;
  const expectedHeaders = MANIFEST_FIELDS;

  const formatImportMessage = useCallback(
    (message) => translateManifestImportMessage(message, intl.formatMessage),
    [intl],
  );

  /**
   * Generate and download CSV template per spec FR-MAN-002
   */
  const downloadTemplate = useCallback(() => {
    const headers = expectedHeaders.join(",");
    // Example row matching the header order
    const exampleRow =
      "BIO-2026-001,PARTICIPANT-001,Serum,PROJ-123,AHRI Lab,2026-01-09 09:30:00,2,8,BSL_2,2026-01-08,Dr. Smith,CONSENT-001,ETH-2025-001,MTA-001,EDTA,Good,Keep upright during transport";
    const csvContent = `${headers}\n${exampleRow}`;

    const blob = new Blob([csvContent], { type: "text/csv;charset=utf-8;" });
    const link = document.createElement("a");
    const url = URL.createObjectURL(blob);
    link.setAttribute("href", url);
    link.setAttribute("download", "biorepository_manifest_template.csv");
    link.style.visibility = "hidden";
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }, [expectedHeaders]);

  const isSupportedDateValue = useCallback((value, allowTime = false) => {
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
        ]
      : [/^\d{4}-\d{2}-\d{2}$/, /^\d{2}\/\d{2}\/\d{4}$/, /^\d{2}-\d{2}-\d{4}$/];

    return patterns.some((pattern) => pattern.test(value));
  }, []);

  const convertLegacyWorksheetRows = useCallback((rows, sheetName) => {
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
      const row = {};

      mappedHeaders.forEach((header, index) => {
        if (!header) {
          return;
        }
        const nextValue = values[index] || "";
        if (row[header] && !nextValue) {
          return;
        }
        row[header] = nextValue || row[header] || "";
      });

      const sampleType = firstNonEmptyValue(
        row.biorepositorySampleType,
        row.sampleType,
      );
      const barcode = row.barcode || "";
      if (!barcode) {
        continue;
      }
      const tempRange = inferLegacyTemperatureRange(sampleType, sheetName);
      const specialHandling = [
        row.specialHandling,
        row.volume && row.volume.toLowerCase() !== "sufficient"
          ? `Volume: ${row.volume}`
          : "",
      ]
        .filter(Boolean)
        .join(" | ");

      convertedRows.push([
        barcode,
        row.externalId,
        sampleType,
        row.projectId,
        row.originLab,
        firstNonEmptyValue(
          row.receiptDate,
          row.collectionDate,
          formatCurrentReceiptDate(),
        ),
        firstNonEmptyValue(row.requiredTempMin, tempRange.min),
        firstNonEmptyValue(row.requiredTempMax, tempRange.max),
        firstNonEmptyValue(
          row.biosafetyLevel,
          inferLegacyBiosafetyLevel(sampleType, sheetName),
        ),
        row.collectionDate,
        row.principalInvestigator,
        row.consentId,
        row.ethicsApprovalRef,
        row.mtaReference,
        row.preservationMedium,
        row.arrivalCondition,
        specialHandling,
      ]);
    }

    return convertedRows;
  }, []);

  const extractWorkbookRows = useCallback(
    (workbook) => {
      const sheetNames = workbook.SheetNames.filter(
        (name) => name.trim().toLowerCase() !== "key",
      );

      let legacyRows = [];

      for (const sheetName of sheetNames) {
        const sheet = workbook.Sheets[sheetName];
        const rows = XLSX.utils.sheet_to_json(sheet, {
          header: 1,
          defval: "",
          raw: false,
          dateNF: "yyyy-mm-dd hh:mm:ss",
        });

        const convertedLegacyRows = convertLegacyWorksheetRows(rows, sheetName);
        if (convertedLegacyRows.length > 0) {
          legacyRows = legacyRows.concat(convertedLegacyRows);
        }
      }

      if (legacyRows.length > 0) {
        return [
          MANIFEST_FIELDS,
          ...normalizeLegacyDuplicateBarcodes(legacyRows),
        ];
      }

      const firstSheet = workbook.Sheets[workbook.SheetNames[0]];
      return XLSX.utils.sheet_to_json(firstSheet, {
        header: 1,
        defval: "",
        raw: false,
        dateNF: "yyyy-mm-dd hh:mm:ss",
      });
    },
    [convertLegacyWorksheetRows],
  );

  const parseManifestRows = useCallback(
    (rows) => {
      const normalizedRows = rows
        .map((row) =>
          Array.isArray(row)
            ? row.map((value) => normalizeCellValue(value))
            : [],
        )
        .filter((row) => row.some((value) => value !== ""));

      if (normalizedRows.length === 0) {
        throw new Error(
          intl.formatMessage({
            id: "biorepository.manifest.error.noCellData",
            defaultMessage:
              "No spreadsheet cell data found. This file may contain only an image or formatting. Please upload a sheet with header and sample rows.",
          }),
        );
      }

      if (normalizedRows.length < 2) {
        throw new Error(
          intl.formatMessage({
            id: "biorepository.manifest.error.emptyFile",
            defaultMessage:
              "Manifest must contain at least a header row and one data row",
          }),
        );
      }

      const rawHeaders = normalizedRows[0];
      const headers = rawHeaders.map(
        (header) => HEADER_ALIASES[normalizeHeaderToken(header)] || null,
      );

      // Keep header requirements backward-compatible with older manifests.
      // Some historical files omit receiptDate/requiredTemp* and rely on
      // collectionDate/sampleType so we infer those values per-row later.
      const minimumHeaderFields = ["sampleType", "originLab"];
      const missingHeaders = minimumHeaderFields.filter(
        (field) => !headers.includes(field),
      );

      if (!headers.includes("barcode") && !headers.includes("externalId")) {
        missingHeaders.unshift("barcode/externalId");
      }
      if (missingHeaders.length > 0) {
        throw new Error(
          intl.formatMessage(
            {
              id: "biorepository.manifest.error.missingHeaders",
              defaultMessage:
                "Missing required columns: {fields}. Please download the template.",
            },
            { fields: missingHeaders.join(", ") },
          ),
        );
      }

      const unknownColumns = rawHeaders.filter((_, index) => !headers[index]);
      if (unknownColumns.length > 0) {
        throw new Error(
          intl.formatMessage(
            {
              id: "biorepository.manifest.error.unknownColumns",
              defaultMessage:
                "Unknown columns in manifest: {columns}. Please use the current template.",
            },
            { columns: unknownColumns.join(", ") },
          ),
        );
      }

      const data = [];
      const errors = [];

      for (let i = 1; i < normalizedRows.length; i++) {
        const values = normalizedRows[i];
        const row = {};

        headers.forEach((header, index) => {
          if (!header) {
            return;
          }
          const nextValue = values[index] || "";
          if (row[header] && !nextValue) {
            return;
          }
          row[header] = nextValue || row[header] || "";
        });

        row.barcode = row.barcode || row.externalId || "";
        row.externalId = row.externalId || row.barcode;

        row.receiptDate = firstNonEmptyValue(
          row.receiptDate,
          row.collectionDate,
          formatCurrentReceiptDate(),
        );

        const inferredTempRange = inferLegacyTemperatureRange(row.sampleType, "");
        row.requiredTempMin = firstNonEmptyValue(
          row.requiredTempMin,
          inferredTempRange.min,
        );
        row.requiredTempMax = firstNonEmptyValue(
          row.requiredTempMax,
          inferredTempRange.max,
        );

        if (!row.biosafetyLevel) {
          row.biosafetyLevel = inferLegacyBiosafetyLevel(row.sampleType, "");
        }

        requiredFields.forEach((field) => {
          const value = field === "barcode" ? row.barcode : row[field];
          if (!value) {
            errors.push({
              row: i + 1,
              field,
              message: intl.formatMessage(
                {
                  id: "biorepository.manifest.error.required",
                  defaultMessage: "{field} is required",
                },
                { field },
              ),
            });
          }
        });

        if (
          row.biosafetyLevel &&
          !["BSL_1", "BSL_2", "BSL_3", "BSL_4"].includes(row.biosafetyLevel)
        ) {
          errors.push({
            row: i + 1,
            field: "biosafetyLevel",
            message: intl.formatMessage(
              {
                id: "biorepository.manifest.error.invalidBSL",
                defaultMessage:
                  "Invalid biosafety level: {value}. Must be BSL_1, BSL_2, BSL_3, or BSL_4",
              },
              { value: row.biosafetyLevel },
            ),
          });
        }

        if (row.receiptDate && !isSupportedDateValue(row.receiptDate, true)) {
          errors.push({
            row: i + 1,
            field: "receiptDate",
            message: intl.formatMessage({
              id: "biorepository.manifest.error.invalidReceiptDate",
              defaultMessage:
                "Invalid receipt date format. Use yyyy-MM-dd or yyyy-MM-dd HH:mm:ss",
            }),
          });
        }

        if (
          row.collectionDate &&
          !isSupportedDateValue(row.collectionDate, false)
        ) {
          errors.push({
            row: i + 1,
            field: "collectionDate",
            message: intl.formatMessage({
              id: "biorepository.manifest.error.invalidDate",
              defaultMessage:
                "Invalid collection date format. Use yyyy-MM-dd, dd/MM/yyyy, or dd-MM-yyyy",
            }),
          });
        }

        const minTemp =
          row.requiredTempMin === "" ? Number.NaN : Number(row.requiredTempMin);
        const maxTemp =
          row.requiredTempMax === "" ? Number.NaN : Number(row.requiredTempMax);

        if (row.requiredTempMin !== "" && Number.isNaN(minTemp)) {
          errors.push({
            row: i + 1,
            field: "requiredTempMin",
            message: intl.formatMessage({
              id: "biorepository.manifest.error.invalidMinTemp",
              defaultMessage: "requiredTempMin must be a number",
            }),
          });
        }

        if (row.requiredTempMax !== "" && Number.isNaN(maxTemp)) {
          errors.push({
            row: i + 1,
            field: "requiredTempMax",
            message: intl.formatMessage({
              id: "biorepository.manifest.error.invalidMaxTemp",
              defaultMessage: "requiredTempMax must be a number",
            }),
          });
        }

        if (
          !Number.isNaN(minTemp) &&
          !Number.isNaN(maxTemp) &&
          minTemp > maxTemp
        ) {
          errors.push({
            row: i + 1,
            field: "requiredTempMax",
            message: intl.formatMessage({
              id: "biorepository.manifest.error.invalidTempRange",
              defaultMessage:
                "requiredTempMax must be greater than or equal to requiredTempMin",
            }),
          });
        }

        row._rowNumber = i + 1;
        row._valid = errors.filter((e) => e.row === i + 1).length === 0;
        data.push(row);
      }

      return { data, errors };
    },
    [intl, isSupportedDateValue, requiredFields],
  );

  const handleFileChange = useCallback(
    (event) => {
      const uploadedFile = event.target.files?.[0];
      if (!uploadedFile) return;

      const fileName = uploadedFile.name.toLowerCase();
      if (
        !fileName.endsWith(".csv") &&
        !fileName.endsWith(".xlsx") &&
        !fileName.endsWith(".xls")
      ) {
        setError(
          intl.formatMessage({
            id: "biorepository.manifest.error.invalidFormat",
            defaultMessage: "Please upload a CSV or Excel file",
          }),
        );
        return;
      }

      setError(null);

      const reader = new FileReader();
      reader.onload = (e) => {
        try {
          const workbook = XLSX.read(e.target.result, {
            type: "array",
            cellDates: true,
          });
          const rows = extractWorkbookRows(workbook);
          const { data, errors } = parseManifestRows(rows);
          setParsedData(data);
          setValidationErrors(errors);
          setValidationWarnings([]);
          setBackendValidationDone(false);
          setImportStatus("parsed");
        } catch (err) {
          setError(err.message);
          setParsedData([]);
          setValidationErrors([]);
        }
      };
      reader.onerror = () => {
        setError(
          intl.formatMessage({
            id: "biorepository.manifest.error.readFile",
            defaultMessage: "Failed to read file",
          }),
        );
      };
      reader.readAsArrayBuffer(uploadedFile);
    },
    [extractWorkbookRows, intl, parseManifestRows],
  );

  /**
   * Transform parsed CSV data to backend DTO format
   */
  const transformToBackendFormat = useCallback((data) => {
    return data.map((row) => {
      const barcode = row.barcode || row.externalId || "";
      const sample = {
        barcode,
        externalId: row.externalId || barcode,
        sampleType: row.sampleType,
        originLab: row.originLab,
        receiptDate: row.receiptDate,
      };

      if (row.projectId) {
        sample.projectId = row.projectId;
      }
      if (row.requiredTempMin !== "") {
        sample.requiredTempMin = Number(row.requiredTempMin);
      }
      if (row.requiredTempMax !== "") {
        sample.requiredTempMax = Number(row.requiredTempMax);
      }
      if (row.collectionDate) {
        sample.collectionDate = row.collectionDate;
      }
      if (row.principalInvestigator) {
        sample.principalInvestigator = row.principalInvestigator;
      }
      if (row.consentId) {
        sample.consentId = row.consentId;
      }
      if (row.ethicsApprovalRef) {
        sample.ethicsApprovalRef = row.ethicsApprovalRef;
      }
      if (row.mtaReference) {
        sample.mtaReference = row.mtaReference;
      }
      if (row.preservationMedium) {
        sample.preservationMedium = row.preservationMedium;
      }
      if (row.arrivalCondition) {
        sample.arrivalCondition = row.arrivalCondition;
      }
      if (row.specialHandling) {
        sample.specialHandling = row.specialHandling;
      }
      if (row.biosafetyLevel) {
        sample.biosafetyLevel = row.biosafetyLevel;
      } else {
        sample.biosafetyLevel = "BSL_1";
      }
      return sample;
    });
  }, []);

  /**
   * Validate manifest data against backend before import.
   * This catches issues like invalid sample types or projects that don't exist in the database.
   */
  const validateWithBackend = useCallback(
    (samples, callback) => {
      postToOpenElisServerJsonResponse(
        "/rest/biorepository/sample/validate-manifest-import",
        JSON.stringify({
          samples,
          shipmentId,
        }),
        callback,
      );
    },
    [shipmentId],
  );

  /**
   * Handle Preview & Validate button click.
   * Sends parsed data to backend for validation and shows results.
   */
  const handlePreviewValidation = useCallback(() => {
    // Don't proceed if there are frontend validation errors
    if (validationErrors.length > 0) {
      setError(
        intl.formatMessage({
          id: "biorepository.manifest.error.fixErrorsFirst",
          defaultMessage: "Please fix the validation errors before previewing.",
        }),
      );
      return;
    }

    setLoading(true);
    setError(null);
    setImportStatus("validating");

    const samples = transformToBackendFormat(parsedData);

    validateWithBackend(samples, (validationResult) => {
      setLoading(false);

      if (validationResult?.error) {
        setError(formatImportMessage(validationResult.message || validationResult.error));
        setImportStatus("parsed");
        return;
      }

      if (!validationResult || !Array.isArray(validationResult.rows)) {
        setError(
          intl.formatMessage({
            id: "biorepository.manifest.error.validationFailed",
            defaultMessage:
              "Failed to validate samples with server. Please try again.",
          }),
        );
        setImportStatus("parsed");
        return;
      }

      // Process backend validation results
      const backendErrors = [];
      const backendWarnings = [];
      const updatedData = parsedData.map((row, index) => {
        const backendRow = validationResult.rows?.[index];
        if (backendRow && !backendRow.valid && backendRow.errors) {
          backendRow.errors.forEach((errMsg) => {
            backendErrors.push({
              row: row._rowNumber,
              field: "backend",
              message: formatImportMessage(errMsg),
            });
          });
        }
        if (backendRow?.warnings) {
          backendRow.warnings.forEach((warningMsg) => {
            backendWarnings.push({
              row: row._rowNumber,
              field: "sampleType",
              message: warningMsg,
            });
          });
        }
        return {
          ...row,
          _valid: backendRow?.valid ?? row._valid,
          _backendErrors: backendRow?.errors || [],
          _backendWarnings: backendRow?.warnings || [],
        };
      });

      setParsedData(updatedData);
      setValidationErrors((prev) => [...prev, ...backendErrors]);
      setValidationWarnings(backendWarnings);
      setBackendValidationDone(true);
      setImportStatus("preview");

      if (!validationResult.valid) {
        setError(
          intl.formatMessage(
            {
              id: "biorepository.manifest.error.backendValidationPreview",
              defaultMessage:
                "{count} sample(s) have validation errors. Please review below.",
            },
            { count: validationResult.invalidCount },
          ),
        );
      }
    });
  }, [
    parsedData,
    validationErrors,
    intl,
    formatImportMessage,
    transformToBackendFormat,
    validateWithBackend,
  ]);

  // Count of valid samples for display and logic
  const validSampleCount = parsedData.filter((row) => row._valid).length;

  const handleImport = useCallback(() => {
    if (!backendValidationDone) {
      setError(
        intl.formatMessage({
          id: "biorepository.manifest.error.notValidated",
          defaultMessage:
            "Please validate the samples first by clicking Preview & Validate",
        }),
      );
      return;
    }

    // Filter to only valid samples
    const validSamples = parsedData.filter((row) => row._valid);

    if (validSamples.length === 0) {
      setError(
        intl.formatMessage({
          id: "biorepository.manifest.error.noValidSamples",
          defaultMessage:
            "No valid samples to import. Please fix errors first.",
        }),
      );
      return;
    }

    setLoading(true);
    setError(null);
    setImportStatus("importing");
    setImportResult(null);

    // Only send valid samples to backend
    const samples = transformToBackendFormat(validSamples);

    postToOpenElisServerJsonResponse(
      "/rest/biorepository/sample/register-bulk",
      JSON.stringify({
        samples,
        shipmentId,
      }),
      (response) => {
        setLoading(false);
        if (!response) {
          setError(
            intl.formatMessage({
              id: "biorepository.manifest.error.importFailed",
              defaultMessage:
                "Failed to import samples with server. Please try again.",
            }),
          );
          setImportStatus("preview");
        } else if (response?.error) {
          setError(formatImportMessage(response.message || response.error));
          setImportStatus("preview");
        } else {
          const rowErrors = translateManifestImportMessages(
            response?.rowErrors,
            intl.formatMessage,
          );
          const registeredCount = Number(
            response?.registeredCount ?? response?.samples?.length ?? 0,
          );
          const failedCount = Number(
            response?.failedCount ?? rowErrors.length ?? 0,
          );

          setImportResult({
            registeredCount,
            failedCount,
            rowErrors,
          });
          setImportStatus("complete");
          if (onImportComplete) {
            try {
              onImportComplete(response?.samples || []);
            } catch (callbackError) {
              // Preserve successful imports even if a follow-up UI refresh fails.
              // The intake page can still be refreshed manually without losing the import.
              // eslint-disable-next-line no-console
              console.error(
                "Biorepository manifest import completed, but follow-up refresh failed:",
                callbackError,
              );
            }
          }
          if (rowErrors.length === 0) {
            // Close modal after a fully successful import
            setTimeout(() => {
              handleClose();
            }, 2000);
          }
        }
      },
    );
  }, [
    parsedData,
    backendValidationDone,
    shipmentId,
    onImportComplete,
    intl,
    formatImportMessage,
    transformToBackendFormat,
  ]);

  const handleClear = useCallback(() => {
    setParsedData([]);
    setValidationErrors([]);
    setValidationWarnings([]);
    setImportStatus(null);
    setError(null);
    setBackendValidationDone(false);
    setImportResult(null);
  }, []);

  const handleClose = useCallback(() => {
    handleClear();
    onClose();
  }, [handleClear, onClose]);

  const tableHeaders = [
    { key: "row", header: "#" },
    {
      key: "barcode",
      header: intl.formatMessage({
        id: "biorepository.manifest.column.barcode",
        defaultMessage: "Sample ID",
      }),
    },
    {
      key: "sampleType",
      header: intl.formatMessage({
        id: "biorepository.manifest.column.sampleType",
        defaultMessage: "Sample Type",
      }),
    },
    {
      key: "originLab",
      header: intl.formatMessage({
        id: "biorepository.manifest.column.originLab",
        defaultMessage: "Origin Lab",
      }),
    },
    {
      key: "receiptDate",
      header: intl.formatMessage({
        id: "biorepository.manifest.column.receiptDate",
        defaultMessage: "Receipt Date",
      }),
    },
    {
      key: "biosafetyLevel",
      header: intl.formatMessage({
        id: "biorepository.manifest.column.bsl",
        defaultMessage: "BSL",
      }),
    },
    {
      key: "status",
      header: intl.formatMessage({
        id: "biorepository.manifest.column.status",
        defaultMessage: "Status",
      }),
    },
  ];

  const tableRows = parsedData.map((row) => ({
    id: String(row._rowNumber),
    row: row._rowNumber,
    barcode: row.barcode || row.externalId || "-",
    sampleType: row.sampleType || "-",
    originLab: row.originLab || "-",
    receiptDate: row.receiptDate || "-",
    biosafetyLevel: row.biosafetyLevel || "BSL_1",
    status: row._valid
      ? row._backendWarnings?.length > 0
        ? "warning"
        : "valid"
      : "error",
  }));

  // Show errors for specific rows
  const getRowErrors = (rowNumber) => {
    return validationErrors.filter((e) => e.row === rowNumber);
  };

  const getRowWarnings = (rowNumber) => {
    return validationWarnings.filter((e) => e.row === rowNumber);
  };

  const formatRowError = (error) => {
    if (!error) {
      return "";
    }

    if (error.field === "backend") {
      return error.message;
    }

    return `${error.field}: ${error.message}`;
  };

  return (
    <Modal
      open={open}
      onRequestClose={handleClose}
      modalHeading={intl.formatMessage({
        id: "biorepository.manifest.modal.title",
        defaultMessage: "Import Sample Manifest",
      })}
      modalLabel={intl.formatMessage({
        id: "biorepository.manifest.modal.label",
        defaultMessage: "Bulk Sample Registration",
      })}
      primaryButtonText={
        importStatus === "preview"
          ? intl.formatMessage(
              {
                id: "biorepository.manifest.button.import",
                defaultMessage: "Import {count} Valid Samples",
              },
              { count: validSampleCount },
            )
          : undefined
      }
      secondaryButtonText={
        importStatus === "preview"
          ? intl.formatMessage({
              id: "biorepository.manifest.button.clear",
              defaultMessage: "Upload Different File",
            })
          : importStatus === "parsed" || importStatus === "validating"
            ? undefined // Buttons are inline for parsed/validating states
            : intl.formatMessage({
                id: "biorepository.button.close",
                defaultMessage: "Close",
              })
      }
      onRequestSubmit={importStatus === "preview" ? handleImport : undefined}
      onSecondarySubmit={
        importStatus === "preview"
          ? handleClear
          : importStatus === "parsed" || importStatus === "validating"
            ? undefined
            : handleClose
      }
      primaryButtonDisabled={
        loading || validSampleCount === 0 || importStatus !== "preview"
      }
      size="lg"
      preventCloseOnClickOutside={loading}
    >
      {loading && <Loading description="Importing samples..." />}

      {error && (
        <InlineNotification
          kind="error"
          title={intl.formatMessage({
            id: "biorepository.manifest.error.title",
            defaultMessage: "Error",
          })}
          subtitle={error}
          lowContrast
          onClose={() => setError(null)}
          style={{ marginBottom: "1rem" }}
        />
      )}

      {importStatus === "complete" && (
        <InlineNotification
          kind="success"
          title={intl.formatMessage({
            id: "biorepository.manifest.success.title",
            defaultMessage: "Import Complete",
          })}
          subtitle={intl.formatMessage(
            {
              id: "biorepository.manifest.success.message",
              defaultMessage:
                "{importedCount} samples registered in intake successfully.{skippedMessage}",
            },
            {
              importedCount:
                importResult?.registeredCount ?? validSampleCount,
              skippedMessage:
                (importResult?.failedCount ?? 0) > 0
                  ? ` ${importResult.failedCount} sample(s) could not be imported.`
                  : parsedData.length > validSampleCount
                    ? ` ${parsedData.length - validSampleCount} invalid sample(s) were skipped.`
                  : "",
            },
          )}
          lowContrast
          hideCloseButton
          style={{ marginBottom: "1rem" }}
        />
      )}

      {importStatus === "complete" && (importResult?.failedCount ?? 0) > 0 && (
        <InlineNotification
          kind="warning"
          title={intl.formatMessage({
            id: "biorepository.manifest.partialImport.title",
            defaultMessage: "Some Samples Were Not Imported",
          })}
          subtitle={
            importResult?.rowErrors?.slice(0, 3).join("; ") ||
            intl.formatMessage({
              id: "biorepository.manifest.partialImport.message",
              defaultMessage:
                "One or more samples could not be imported. Please review the failed rows and try again.",
            })
          }
          lowContrast
          style={{ marginBottom: "1rem" }}
        />
      )}

      {importStatus === null && (
        <div style={{ marginBottom: "1.5rem" }}>
          <p style={{ marginBottom: "1rem" }}>
            <FormattedMessage
              id="biorepository.manifest.instructions"
              defaultMessage="Upload a CSV or Excel manifest to register samples on the intake page. Later storage, QC, retrieval, and disposal steps remain manual."
            />
          </p>
          <div style={{ marginBottom: "1rem" }}>
            <Button
              kind="tertiary"
              size="sm"
              renderIcon={Download}
              onClick={downloadTemplate}
            >
              <FormattedMessage
                id="biorepository.manifest.button.downloadTemplate"
                defaultMessage="Download Manifest Template"
              />
            </Button>
          </div>
          <div
            style={{
              padding: "1rem",
              backgroundColor: "#f4f4f4",
              borderRadius: "4px",
              marginBottom: "1rem",
            }}
          >
            <h6 style={{ marginBottom: "0.5rem" }}>
              <FormattedMessage
                id="biorepository.manifest.requiredFields.title"
                defaultMessage="Required Fields:"
              />
            </h6>
            <ul style={{ margin: 0, paddingLeft: "1.5rem" }}>
              {requiredFields.map((field) => (
                <li key={field}>{field}</li>
              ))}
            </ul>

            <h6 style={{ marginBottom: "0.5rem", marginTop: "1rem" }}>
              <FormattedMessage
                id="biorepository.manifest.conditionalFields.title"
                defaultMessage="Conditional Fields:"
              />
            </h6>
            <ul style={{ margin: 0, paddingLeft: "1.5rem" }}>
              {conditionalFields.map((field) => (
                <li key={field.name}>
                  {field.name}{" "}
                  <span style={{ color: "#525252", fontSize: "0.75rem" }}>
                    ({field.description})
                  </span>
                </li>
              ))}
            </ul>

            <h6 style={{ marginBottom: "0.5rem", marginTop: "1rem" }}>
              <FormattedMessage
                id="biorepository.manifest.optionalFields.title"
                defaultMessage="Optional Fields:"
              />
            </h6>
            <ul style={{ margin: 0, paddingLeft: "1.5rem" }}>
              {optionalFields.map((field) => (
                <li key={field}>{field}</li>
              ))}
            </ul>
          </div>

          <div
            style={{
              padding: "1rem",
              backgroundColor: "#e8f3ff",
              borderRadius: "4px",
              marginBottom: "1rem",
            }}
          >
            <h6 style={{ marginBottom: "0.5rem" }}>
              <FormattedMessage
                id="biorepository.manifest.sampleTypes.title"
                defaultMessage="Sample Type Handling:"
              />
            </h6>
            <p style={{ margin: 0, fontSize: "0.875rem", color: "#393939" }}>
              <FormattedMessage
                id="biorepository.manifest.sampleTypes.help"
                defaultMessage="The manifest accepts the sample type labels used by AHRI. Existing sample types are matched by ID, name, localized name, or abbreviation. If a biorepository sample type is not already configured, validation will mark it for creation and the import will register it automatically."
              />
            </p>
          </div>
          <FileUploader
            labelTitle={intl.formatMessage({
              id: "biorepository.manifest.upload.title",
              defaultMessage: "Select manifest file",
            })}
            labelDescription={intl.formatMessage({
              id: "biorepository.manifest.upload.description",
              defaultMessage: "Upload CSV, XLSX, or XLS file with sample data",
            })}
            buttonLabel={intl.formatMessage({
              id: "biorepository.manifest.upload.button",
              defaultMessage: "Select manifest file",
            })}
            iconDescription={intl.formatMessage({
              id: "biorepository.manifest.upload.iconDescription",
              defaultMessage: "Delete file",
            })}
            accept={[".csv", ".xlsx", ".xls"]}
            multiple={false}
            onChange={handleFileChange}
            filenameStatus="edit"
          />
        </div>
      )}

      {(importStatus === "parsed" || importStatus === "validating") &&
        parsedData.length > 0 && (
          <div>
            <div style={{ marginBottom: "1rem" }}>
              <h5 style={{ display: "inline-block", marginRight: "0.5rem" }}>
                <FormattedMessage
                  id="biorepository.manifest.parsed.title"
                  defaultMessage="Parsed: {count} samples"
                  values={{ count: parsedData.length }}
                />
              </h5>
              {validationErrors.length > 0 && (
                <Tag type="red">
                  <Warning size={16} style={{ marginRight: "0.25rem" }} />
                  {validationErrors.length}{" "}
                  <FormattedMessage
                    id="biorepository.manifest.parsed.formatErrors"
                    defaultMessage="format error(s)"
                  />
                </Tag>
              )}
              {validationErrors.length === 0 && (
                <Tag type="blue">
                  <FormattedMessage
                    id="biorepository.manifest.parsed.readyToValidate"
                    defaultMessage="Ready for validation"
                  />
                </Tag>
              )}
            </div>

            {validationErrors.length > 0 && (
              <InlineNotification
                kind="warning"
                title={intl.formatMessage({
                  id: "biorepository.manifest.formatErrors.title",
                  defaultMessage: "Format Errors",
                })}
                subtitle={intl.formatMessage({
                  id: "biorepository.manifest.formatErrors.subtitle",
                  defaultMessage:
                    "Please fix these errors before validation. You may need to upload a corrected manifest file.",
                })}
                lowContrast
                hideCloseButton
                style={{ marginBottom: "1rem" }}
              />
            )}

            {importStatus === "validating" && (
              <InlineNotification
                kind="info"
                title={intl.formatMessage({
                  id: "biorepository.manifest.validating.title",
                  defaultMessage: "Validating",
                })}
                subtitle={intl.formatMessage({
                  id: "biorepository.manifest.validating.subtitle",
                  defaultMessage:
                    "Checking samples against database for valid sample types, projects, etc...",
                })}
                lowContrast
                hideCloseButton
                style={{ marginBottom: "1rem" }}
              />
            )}

            <div
              style={{
                maxHeight: "300px",
                overflowY: "auto",
                marginBottom: "1rem",
              }}
            >
              <DataTable rows={tableRows} headers={tableHeaders} size="sm">
                {({
                  rows,
                  headers,
                  getTableProps,
                  getHeaderProps,
                  getRowProps,
                }) => (
                  <Table {...getTableProps()} size="sm">
                    <TableHead>
                      <TableRow>
                        {headers.map((header) => (
                          <TableHeader
                            key={header.key}
                            {...getHeaderProps({ header })}
                          >
                            {header.header}
                          </TableHeader>
                        ))}
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {rows.map((row) => {
                        const rowNumber = parseInt(row.id);
                        const rowErrors = getRowErrors(rowNumber);
                        const rowWarnings = getRowWarnings(rowNumber);
                        return (
                          <React.Fragment key={row.id}>
                            <TableRow {...getRowProps({ row })}>
                              {row.cells.map((cell) => (
                                <TableCell key={cell.id}>
                                  {cell.info.header === "status" ? (
                                    cell.value === "valid" ? (
                                      <Tag type="gray" size="sm">
                                        <FormattedMessage
                                          id="biorepository.manifest.status.pending"
                                          defaultMessage="Pending"
                                        />
                                      </Tag>
                                    ) : cell.value === "warning" ? (
                                      <Tag type="warm-gray" size="sm">
                                        <FormattedMessage
                                          id="biorepository.manifest.status.warning"
                                          defaultMessage="Will Create Type"
                                        />
                                      </Tag>
                                    ) : (
                                      <Tag type="red" size="sm">
                                        <FormattedMessage
                                          id="biorepository.manifest.status.error"
                                          defaultMessage="Error"
                                        />
                                      </Tag>
                                    )
                                  ) : (
                                    cell.value
                                  )}
                                </TableCell>
                              ))}
                            </TableRow>
                            {rowErrors.length > 0 && (
                              <TableRow>
                                <TableCell colSpan={headers.length}>
                                  <div
                                    style={{
                                      backgroundColor: "#fff1f1",
                                      padding: "0.5rem",
                                      fontSize: "0.875rem",
                                    }}
                                  >
                                    {rowErrors.map((err, idx) => (
                                      <div key={idx}>
                                        • {err.field}: {err.message}
                                      </div>
                                    ))}
                                  </div>
                                </TableCell>
                              </TableRow>
                            )}
                            {rowWarnings.length > 0 && (
                              <TableRow>
                                <TableCell colSpan={headers.length}>
                                  <div
                                    style={{
                                      backgroundColor: "#fff8e1",
                                      padding: "0.5rem",
                                      fontSize: "0.875rem",
                                    }}
                                  >
                                    {rowWarnings.map((warning, idx) => (
                                      <div key={idx}>• {warning.message}</div>
                                    ))}
                                  </div>
                                </TableCell>
                              </TableRow>
                            )}
                          </React.Fragment>
                        );
                      })}
                    </TableBody>
                  </Table>
                )}
              </DataTable>
            </div>

            <div
              style={{
                display: "flex",
                gap: "1rem",
                justifyContent: "flex-end",
              }}
            >
              <Button
                kind="secondary"
                size="md"
                onClick={handleClear}
                disabled={loading}
              >
                <FormattedMessage
                  id="biorepository.manifest.button.uploadDifferent"
                  defaultMessage="Upload Different File"
                />
              </Button>
              <Button
                kind="primary"
                size="md"
                onClick={handlePreviewValidation}
                disabled={loading || validationErrors.length > 0}
                renderIcon={
                  importStatus === "validating" ? undefined : Checkmark
                }
              >
                {importStatus === "validating" ? (
                  <FormattedMessage
                    id="biorepository.manifest.button.validating"
                    defaultMessage="Validating..."
                  />
                ) : (
                  <FormattedMessage
                    id="biorepository.manifest.button.previewValidate"
                    defaultMessage="Preview & Validate"
                  />
                )}
              </Button>
            </div>
          </div>
        )}

      {importStatus === "preview" && parsedData.length > 0 && (
        <div>
          <div style={{ marginBottom: "1rem" }}>
            <h5 style={{ display: "inline-block", marginRight: "0.5rem" }}>
              <FormattedMessage
                id="biorepository.manifest.preview.title"
                defaultMessage="Preview: {count} samples"
                values={{ count: parsedData.length }}
              />
            </h5>
            {validationErrors.length > 0 && (
              <Tag type="red">
                <Warning size={16} style={{ marginRight: "0.25rem" }} />
                {validationErrors.length} error(s)
              </Tag>
            )}
            {validationErrors.length === 0 && (
              <Tag type="green">
                <Checkmark size={16} style={{ marginRight: "0.25rem" }} />
                <FormattedMessage
                  id="biorepository.manifest.preview.valid"
                  defaultMessage="All valid"
                />
              </Tag>
            )}
            {validationWarnings.length > 0 && (
              <Tag type="warm-gray" style={{ marginLeft: "0.5rem" }}>
                <Warning size={16} style={{ marginRight: "0.25rem" }} />
                {validationWarnings.length} warning(s)
              </Tag>
            )}
          </div>

          {validationErrors.length > 0 && (
            <InlineNotification
              kind="warning"
              title={intl.formatMessage({
                id: "biorepository.manifest.validation.title",
                defaultMessage: "Validation Errors",
              })}
              subtitle={intl.formatMessage({
                id: "biorepository.manifest.validation.subtitle",
                defaultMessage:
                  "Please fix the errors below before importing. Rows with errors are highlighted.",
              })}
              lowContrast
              hideCloseButton
              style={{ marginBottom: "1rem" }}
            />
          )}

          {validationErrors.length > 0 && validSampleCount > 0 && (
            <InlineNotification
              kind="info"
              title={intl.formatMessage({
                id: "biorepository.manifest.partialImport.title",
                defaultMessage: "Partial Import Available",
              })}
              subtitle={intl.formatMessage(
                {
                  id: "biorepository.manifest.partialImport.subtitle",
                  defaultMessage:
                    "{count} valid sample(s) can still be imported. Rows marked Error will be skipped.",
                },
                { count: validSampleCount },
              )}
              lowContrast
              hideCloseButton
              style={{ marginBottom: "1rem" }}
            />
          )}

          {validationWarnings.length > 0 && (
            <InlineNotification
              kind="info"
              title={intl.formatMessage({
                id: "biorepository.manifest.warnings.title",
                defaultMessage: "Validation Warnings",
              })}
              subtitle={intl.formatMessage({
                id: "biorepository.manifest.warnings.subtitle",
                defaultMessage:
                  "Some rows introduce new sample types. Those sample types will be created automatically during import and added to the biorepository-approved list.",
              })}
              lowContrast
              hideCloseButton
              style={{ marginBottom: "1rem" }}
            />
          )}

          <div style={{ maxHeight: "400px", overflowY: "auto" }}>
            <DataTable rows={tableRows} headers={tableHeaders} size="sm">
              {({
                rows,
                headers,
                getTableProps,
                getHeaderProps,
                getRowProps,
              }) => (
                <Table {...getTableProps()} size="sm">
                  <TableHead>
                    <TableRow>
                      {headers.map((header) => (
                        <TableHeader
                          key={header.key}
                          {...getHeaderProps({ header })}
                        >
                          {header.header}
                        </TableHeader>
                      ))}
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {rows.map((row) => {
                      const rowNumber = parseInt(row.id);
                      const rowErrors = getRowErrors(rowNumber);
                      const rowWarnings = getRowWarnings(rowNumber);
                      return (
                        <React.Fragment key={row.id}>
                          <TableRow {...getRowProps({ row })}>
                            {row.cells.map((cell) => (
                              <TableCell key={cell.id}>
                                {cell.info.header === "status" ? (
                                  cell.value === "valid" ? (
                                    <Tag type="green" size="sm">
                                      <FormattedMessage
                                        id="biorepository.manifest.status.valid"
                                        defaultMessage="Valid"
                                      />
                                    </Tag>
                                  ) : cell.value === "warning" ? (
                                    <Tag type="warm-gray" size="sm">
                                      <FormattedMessage
                                        id="biorepository.manifest.status.warning"
                                        defaultMessage="Will Create Type"
                                      />
                                    </Tag>
                                  ) : (
                                    <Tag type="red" size="sm">
                                      <FormattedMessage
                                        id="biorepository.manifest.status.error"
                                        defaultMessage="Error"
                                      />
                                    </Tag>
                                  )
                                ) : (
                                  cell.value
                                )}
                              </TableCell>
                            ))}
                          </TableRow>
                          {rowErrors.length > 0 && (
                            <TableRow>
                              <TableCell colSpan={headers.length}>
                                <div
                                  style={{
                                    backgroundColor: "#fff1f1",
                                    padding: "0.5rem",
                                    fontSize: "0.875rem",
                                  }}
                                >
                                  {rowErrors.map((err, idx) => (
                                    <div key={idx}>• {formatRowError(err)}</div>
                                  ))}
                                </div>
                              </TableCell>
                            </TableRow>
                          )}
                          {rowWarnings.length > 0 && (
                            <TableRow>
                              <TableCell colSpan={headers.length}>
                                <div
                                  style={{
                                    backgroundColor: "#fff8e1",
                                    padding: "0.5rem",
                                    fontSize: "0.875rem",
                                  }}
                                >
                                  {rowWarnings.map((warning, idx) => (
                                    <div key={idx}>• {warning.message}</div>
                                  ))}
                                </div>
                              </TableCell>
                            </TableRow>
                          )}
                        </React.Fragment>
                      );
                    })}
                  </TableBody>
                </Table>
              )}
            </DataTable>
          </div>
        </div>
      )}
    </Modal>
  );
}

ManifestUploadModal.propTypes = {
  open: PropTypes.bool.isRequired,
  onClose: PropTypes.func.isRequired,
  shipmentId: PropTypes.number,
  onImportComplete: PropTypes.func,
};

export default ManifestUploadModal;
