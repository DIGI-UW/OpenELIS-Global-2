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
import {
  MANIFEST_FIELDS,
  convertLegacyWorksheetRows,
  firstNonEmptyValue,
  formatCurrentReceiptDate,
  inferLegacyBiosafetyLevel,
  inferLegacyTemperatureRange,
  isSupportedDateValue,
  mergeMappedRowValues,
  normalizeCellValue,
  normalizeDateValue,
  normalizeHeaderToken,
  normalizeLegacyDuplicateBarcodes,
  buildSpecialHandlingNotes,
  HEADER_ALIASES,
  STORAGE_METADATA_ALIASES,
} from "./manifestImportHelpers";

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

const VALIDATION_BATCH_SIZE = 500;

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
  const [validationProgress, setValidationProgress] = useState({
    completed: 0,
    total: 0,
  });
  const [importProgress, setImportProgress] = useState({
    completed: 0,
    total: 0,
  });

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
    [],
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

      const unknownColumns = rawHeaders.filter((header, index) => {
        const token = normalizeHeaderToken(header);
        return !headers[index] && !STORAGE_METADATA_ALIASES[token];
      });
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
        const row = mergeMappedRowValues(rawHeaders, values);

        row.barcode = row.barcode || row.externalId || "";
        row.externalId = row.externalId || row.barcode;

        row.receiptDate = normalizeDateValue(
          firstNonEmptyValue(
            row.receiptDate,
            row.collectionDate,
            formatCurrentReceiptDate(),
          ),
          true,
        );
        row.collectionDate = normalizeDateValue(row.collectionDate, false);
        row.specialHandling = buildSpecialHandlingNotes(row);

        const inferredTempRange = inferLegacyTemperatureRange(
          row.sampleType,
          "",
        );
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
                "Invalid receipt date format. Use yyyy-MM-dd, dd/MM/yyyy, dd-MM-yyyy, or dd.MM.yyyy",
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
                "Invalid collection date format. Use yyyy-MM-dd, dd/MM/yyyy, dd-MM-yyyy, or dd.MM.yyyy",
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

  const formatServerRequestError = useCallback(
    (result, fallbackId, fallbackMessage) => {
      if (result?.error || result?.message) {
        return formatImportMessage(result.message || result.error);
      }
      const status = result?.status || result?.statusCode;
      if (status === 413) {
        return intl.formatMessage({
          id: "biorepository.manifest.error.payloadTooLarge",
          defaultMessage:
            "The manifest batch is too large for the server. Try again after the update completes, or split the file into smaller uploads.",
        });
      }
      if (status === 504 || status === 408) {
        return intl.formatMessage({
          id: "biorepository.manifest.error.validationTimeout",
          defaultMessage:
            "Server validation timed out. Please try again; large manifests are processed in batches.",
        });
      }
      if (status) {
        return intl.formatMessage(
          {
            id: "biorepository.manifest.error.validationFailedWithStatus",
            defaultMessage:
              "Server validation failed (HTTP {status}). Please try again.",
          },
          { status },
        );
      }
      return intl.formatMessage({
        id: fallbackId,
        defaultMessage: fallbackMessage,
      });
    },
    [formatImportMessage, intl],
  );

  const validateBatchWithBackend = useCallback(
    (samples) =>
      new Promise((resolve) => {
        postToOpenElisServerJsonResponse(
          "/rest/biorepository/sample/validate-manifest-import",
          JSON.stringify({
            samples,
            shipmentId,
          }),
          (validationResult) => resolve(validationResult),
        );
      }),
    [shipmentId],
  );

  const runBatchedValidation = useCallback(
    async (allSamples) => {
      const mergedRows = new Array(allSamples.length);
      let mergedValid = true;
      let mergedInvalidCount = 0;

      setValidationProgress({ completed: 0, total: allSamples.length });

      for (
        let start = 0;
        start < allSamples.length;
        start += VALIDATION_BATCH_SIZE
      ) {
        const batch = allSamples.slice(start, start + VALIDATION_BATCH_SIZE);
        setValidationProgress({ completed: start, total: allSamples.length });

        const validationResult = await validateBatchWithBackend(batch);

        if (validationResult?.error) {
          throw validationResult;
        }
        if (!validationResult || !Array.isArray(validationResult.rows)) {
          throw validationResult || { status: 0 };
        }

        validationResult.rows.forEach((row, index) => {
          mergedRows[start + index] = row;
        });
        if (!validationResult.valid) {
          mergedValid = false;
        }
        mergedInvalidCount += Number(validationResult.invalidCount || 0);
      }

      setValidationProgress({
        completed: allSamples.length,
        total: allSamples.length,
      });

      return {
        valid: mergedValid,
        invalidCount: mergedInvalidCount,
        rows: mergedRows,
      };
    },
    [validateBatchWithBackend],
  );

  const registerBatchWithBackend = useCallback(
    (samples) =>
      new Promise((resolve) => {
        postToOpenElisServerJsonResponse(
          "/rest/biorepository/sample/register-bulk",
          JSON.stringify({
            samples,
            shipmentId,
          }),
          (response) => resolve(response),
        );
      }),
    [shipmentId],
  );

  const runBatchedImport = useCallback(
    async (allSamples) => {
      const merged = {
        registeredCount: 0,
        failedCount: 0,
        rowErrors: [],
        samples: [],
        success: true,
      };

      setImportProgress({ completed: 0, total: allSamples.length });

      for (
        let start = 0;
        start < allSamples.length;
        start += VALIDATION_BATCH_SIZE
      ) {
        const batch = allSamples.slice(start, start + VALIDATION_BATCH_SIZE);
        setImportProgress({ completed: start, total: allSamples.length });

        const response = await registerBatchWithBackend(batch);
        if (!response) {
          throw { status: 0 };
        }
        if (response?.error) {
          throw response;
        }

        merged.registeredCount += Number(response?.registeredCount || 0);
        merged.failedCount += Number(response?.failedCount || 0);
        if (Array.isArray(response?.rowErrors)) {
          merged.rowErrors.push(...response.rowErrors);
        }
        if (Array.isArray(response?.samples)) {
          merged.samples.push(...response.samples);
        }
        if (response?.success === false) {
          merged.success = false;
        }
      }

      setImportProgress({
        completed: allSamples.length,
        total: allSamples.length,
      });

      return merged;
    },
    [registerBatchWithBackend],
  );

  const applyBackendValidationResults = useCallback(
    (validationResult) => {
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
    },
    [parsedData, formatImportMessage, intl],
  );

  /**
   * Handle Preview & Validate button click.
   * Sends parsed data to backend for validation and shows results.
   */
  const handlePreviewValidation = useCallback(() => {
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
    setBackendValidationDone(false);

    const samples = transformToBackendFormat(parsedData);

    runBatchedValidation(samples)
      .then((validationResult) => {
        setLoading(false);
        applyBackendValidationResults(validationResult);
      })
      .catch((failure) => {
        setLoading(false);
        setImportStatus("parsed");
        setValidationProgress({ completed: 0, total: 0 });
        setError(
          formatServerRequestError(
            failure,
            "biorepository.manifest.error.validationFailed",
            "Failed to validate samples with server. Please try again.",
          ),
        );
      });
  }, [
    validationErrors,
    intl,
    transformToBackendFormat,
    parsedData,
    runBatchedValidation,
    applyBackendValidationResults,
    formatServerRequestError,
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

    const samples = transformToBackendFormat(validSamples);

    runBatchedImport(samples)
      .then((response) => {
        setLoading(false);
        const rowErrors = translateManifestImportMessages(
          response?.rowErrors,
          intl.formatMessage,
        );
        const registeredCount = Number(response?.registeredCount || 0);
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
            // eslint-disable-next-line no-console
            console.error(
              "Biorepository manifest import completed, but follow-up refresh failed:",
              callbackError,
            );
          }
        }
        if (rowErrors.length === 0) {
          setTimeout(() => {
            handleClose();
          }, 2000);
        }
      })
      .catch((failure) => {
        setLoading(false);
        setImportStatus("preview");
        setImportProgress({ completed: 0, total: 0 });
        setError(
          formatServerRequestError(
            failure,
            "biorepository.manifest.error.importFailed",
            "Failed to import samples with server. Please try again.",
          ),
        );
      });
  }, [
    parsedData,
    backendValidationDone,
    onImportComplete,
    intl,
    formatImportMessage,
    transformToBackendFormat,
    runBatchedImport,
    formatServerRequestError,
  ]);

  const handleClear = useCallback(() => {
    setParsedData([]);
    setValidationErrors([]);
    setValidationWarnings([]);
    setImportStatus(null);
    setError(null);
    setBackendValidationDone(false);
    setImportResult(null);
    setValidationProgress({ completed: 0, total: 0 });
    setImportProgress({ completed: 0, total: 0 });
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
              importedCount: importResult?.registeredCount ?? validSampleCount,
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
              defaultMessage:
                "Upload CSV, XLSX, or XLS. AHRI Bacteriology multi-sheet workbooks are supported (Request_Date = receipt, Transfer_Date = collection). Dates may use yyyy-MM-dd, dd/MM/yyyy, dd-MM-yyyy, or dd.MM.yyyy.",
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
                subtitle={intl.formatMessage(
                  {
                    id: "biorepository.manifest.validating.progress",
                    defaultMessage:
                      "Checking samples against the database ({completed} / {total})...",
                  },
                  {
                    completed: validationProgress.completed,
                    total: validationProgress.total || parsedData.length,
                  },
                )}
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
                                          id="biorepository.manifest.status.ready"
                                          defaultMessage="Ready"
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
