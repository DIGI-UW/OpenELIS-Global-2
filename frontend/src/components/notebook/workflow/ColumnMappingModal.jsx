import React, { useState, useEffect, useCallback, useMemo } from "react";
import {
  Modal,
  Dropdown,
  FormGroup,
  Stack,
  InlineNotification,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import FileDataPreviewTable from "./FileDataPreviewTable";

/**
 * ColumnMappingModal Component
 *
 * Allows users to map CSV columns to analytical result fields.
 * Provides live preview of how data will be parsed based on column selection.
 *
 * Features:
 * - Dropdown selectors for required and optional columns
 * - Live data preview updates as user selects columns
 * - Validation of required field selections
 * - Clear error messages
 *
 * Mapped Fields:
 * - Result Value Column (REQUIRED) - The concentration/result value
 * - Unit Column (OPTIONAL) - Unit of measurement
 * - QC Flag Column (OPTIONAL) - QC status/flag
 * - Notes Column (OPTIONAL) - Additional notes/comments
 *
 * @param {Object} props
 * @param {boolean} props.open - Whether modal is open
 * @param {Object} props.file - File object with headers and preview rows
 * @param {Function} props.onConfirm - Callback when user confirms mapping
 * @param {Function} props.onCancel - Callback when user cancels
 */
function ColumnMappingModal({ open = false, file, onConfirm, onCancel }) {
  const intl = useIntl();

  const [mapping, setMapping] = useState({
    resultColumn: "",
    unitColumn: "",
    qcColumn: "",
    notesColumn: "",
  });

  const [validationError, setValidationError] = useState("");

  /**
   * Automatic column mapping detection
   * Tries to identify columns based on common naming patterns
   */
  const detectMapping = useCallback((headers) => {
    const mapping = {
      resultColumn: "",
      unitColumn: "",
      qcColumn: "",
      notesColumn: "",
    };

    if (!headers || headers.length === 0) return mapping;

    // Convert headers to lowercase for pattern matching
    const lowerHeaders = headers.map((h) => h.toLowerCase());

    // Patterns for detecting columns
    const resultPatterns = [
      /concentration|result|value|conc|measured/,
      /analysis.*result|sample.*result/,
    ];
    const unitPatterns = [/^unit$|units|measurement.*unit/];
    const qcPatterns = [/qc|quality|flag|status/, /validity.*status|valid/];
    const notesPatterns = [/notes|comment|remarks|description|notes.*analysis/];

    // Find matching columns
    headers.forEach((header, idx) => {
      const lowerHeader = lowerHeaders[idx];

      // Result column (highest priority)
      if (!mapping.resultColumn) {
        if (resultPatterns.some((p) => p.test(lowerHeader))) {
          mapping.resultColumn = header;
        }
      }

      // Unit column
      if (!mapping.unitColumn) {
        if (unitPatterns.some((p) => p.test(lowerHeader))) {
          mapping.unitColumn = header;
        }
      }

      // QC/Status column
      if (!mapping.qcColumn) {
        if (qcPatterns.some((p) => p.test(lowerHeader))) {
          mapping.qcColumn = header;
        }
      }

      // Notes column
      if (!mapping.notesColumn) {
        if (notesPatterns.some((p) => p.test(lowerHeader))) {
          mapping.notesColumn = header;
        }
      }
    });

    return mapping;
  }, []);

  // Reset mapping when file changes or modal opens
  useEffect(() => {
    if (open && file?.headers) {
      // Try to auto-detect mapping first
      const detectedMapping = detectMapping(file.headers);
      setMapping(detectedMapping);
      setValidationError("");
    }
  }, [open, file, detectMapping]);

  // Generate dropdown options from file headers
  const headerOptions = useMemo(() => {
    if (!file?.headers) return [];

    return [
      { id: "", label: "-- None --" },
      ...file.headers.map((header, idx) => ({
        id: header,
        label: `${header} (Column ${idx + 1})`,
      })),
    ];
  }, [file?.headers]);

  // Generate preview data based on current mapping
  const previewData = useMemo(() => {
    if (!file?.previewRows || file.previewRows.length === 0) return [];

    if (!file.headers) return [];

    return file.previewRows.map((row) => {
      const rowObj = {};

      // Map headers to row values
      file.headers.forEach((header, idx) => {
        rowObj[header] = row[idx] || "--";
      });

      return rowObj;
    });
  }, [file?.previewRows, file?.headers]);

  // Generate columns for preview table based on selected mapping
  const previewColumns = useMemo(() => {
    const cols = [];

    if (mapping.resultColumn) {
      cols.push({
        key: mapping.resultColumn,
        header: intl.formatMessage({
          id: "table.column.resultValue",
          defaultMessage: "Result Value",
        }),
      });
    }

    if (mapping.unitColumn) {
      cols.push({
        key: mapping.unitColumn,
        header: intl.formatMessage({
          id: "table.column.unit",
          defaultMessage: "Unit",
        }),
      });
    }

    if (mapping.qcColumn) {
      cols.push({
        key: mapping.qcColumn,
        header: intl.formatMessage({
          id: "table.column.qcFlag",
          defaultMessage: "QC Flag",
        }),
      });
    }

    if (mapping.notesColumn) {
      cols.push({
        key: mapping.notesColumn,
        header: intl.formatMessage({
          id: "table.column.notes",
          defaultMessage: "Notes",
        }),
      });
    }

    // If no columns selected yet, show result column as placeholder
    if (cols.length === 0) {
      cols.push({
        key: "placeholder",
        header: intl.formatMessage({
          id: "table.selectColumns",
          defaultMessage: "Select columns to preview",
        }),
      });
    }

    return cols;
  }, [mapping, intl]);

  const handleMappingChange = useCallback((field, column) => {
    setMapping((prev) => ({
      ...prev,
      [field]: column,
    }));
    setValidationError("");
  }, []);

  const handleConfirm = useCallback(() => {
    // Validate required fields
    if (!mapping.resultColumn) {
      setValidationError(
        intl.formatMessage({
          id: "validation.resultColumnRequired",
          defaultMessage: "Result Value column is required",
        }),
      );
      return;
    }

    // Ensure no duplicate column selection
    const selectedColumns = Object.values(mapping).filter((col) => col);
    const uniqueColumns = new Set(selectedColumns);

    if (selectedColumns.length !== uniqueColumns.size) {
      setValidationError(
        intl.formatMessage({
          id: "validation.duplicateColumns",
          defaultMessage: "Cannot select the same column multiple times",
        }),
      );
      return;
    }

    onConfirm?.(mapping);
  }, [mapping, intl, onConfirm]);

  if (!file) return null;

  return (
    <Modal
      open={open}
      onRequestClose={onCancel}
      onRequestSubmit={handleConfirm}
      modalHeading={intl.formatMessage({
        id: "modal.columnMapping.title",
        defaultMessage: `Map Columns - ${file.name}`,
      })}
      primaryButtonText={intl.formatMessage({
        id: "button.confirmMapping",
        defaultMessage: "Confirm Mapping",
      })}
      secondaryButtonText={intl.formatMessage({
        id: "button.cancel",
        defaultMessage: "Cancel",
      })}
      size="lg"
    >
      <Stack gap={5}>
        {/* Auto-Detection Info */}
        {mapping.resultColumn && (
          <InlineNotification
            kind="info"
            title={intl.formatMessage({
              id: "columnMapping.autoDetected.title",
              defaultMessage: "Auto-Detected Mapping",
            })}
            subtitle={intl.formatMessage({
              id: "columnMapping.autoDetected.message",
              defaultMessage:
                "Column mappings have been automatically detected. Please review and adjust if needed.",
            })}
            onClose={() => {}}
          />
        )}

        {/* Validation Error */}
        {validationError && (
          <InlineNotification
            kind="error"
            title={intl.formatMessage({
              id: "notification.error.title",
              defaultMessage: "Error",
            })}
            subtitle={validationError}
            onClose={() => setValidationError("")}
          />
        )}

        {/* File Info */}
        <div
          style={{
            padding: "1rem",
            backgroundColor: "#e8f4f8",
            borderRadius: "4px",
            borderLeft: "4px solid #0043ce",
          }}
        >
          <strong>
            <FormattedMessage
              id="columnMapping.fileInfo"
              defaultMessage="File: {filename} ({rows} rows)"
              values={{
                filename: file.name,
                rows: file.totalRows || file.previewRows?.length || 0,
              }}
            />
          </strong>
        </div>

        {/* Column Mapping Selectors */}
        <FormGroup
          legendText={intl.formatMessage({
            id: "columnMapping.selectColumns",
            defaultMessage: "Select which columns contain what data",
          })}
        >
          <Stack gap={4}>
            {/* Result Value Column - REQUIRED */}
            <Dropdown
              id="result-column-select"
              titleText={intl.formatMessage({
                id: "columnMapping.resultColumn",
                defaultMessage: "Result Value Column *",
              })}
              helperText={intl.formatMessage({
                id: "columnMapping.resultColumnHelper",
                defaultMessage: "The concentration or measured result value",
              })}
              label={intl.formatMessage({
                id: "dropdown.selectColumn",
                defaultMessage: "Select a column...",
              })}
              items={headerOptions}
              itemToString={(item) => item?.label || ""}
              selectedItem={
                headerOptions.find((opt) => opt.id === mapping.resultColumn) ||
                headerOptions[0]
              }
              onChange={({ selectedItem }) =>
                handleMappingChange("resultColumn", selectedItem?.id || "")
              }
            />

            {/* Unit Column - OPTIONAL */}
            <Dropdown
              id="unit-column-select"
              titleText={intl.formatMessage({
                id: "columnMapping.unitColumn",
                defaultMessage: "Unit Column",
              })}
              helperText={intl.formatMessage({
                id: "columnMapping.unitColumnHelper",
                defaultMessage: "The unit of measurement (e.g., ng/mL, mg/dL)",
              })}
              label={intl.formatMessage({
                id: "dropdown.selectColumn",
                defaultMessage: "Select a column...",
              })}
              items={headerOptions}
              itemToString={(item) => item?.label || ""}
              selectedItem={
                headerOptions.find((opt) => opt.id === mapping.unitColumn) ||
                headerOptions[0]
              }
              onChange={({ selectedItem }) =>
                handleMappingChange("unitColumn", selectedItem?.id || "")
              }
            />

            {/* QC Flag Column - OPTIONAL */}
            <Dropdown
              id="qc-column-select"
              titleText={intl.formatMessage({
                id: "columnMapping.qcColumn",
                defaultMessage: "QC Flag Column",
              })}
              helperText={intl.formatMessage({
                id: "columnMapping.qcColumnHelper",
                defaultMessage: "QC status or flag (e.g., PASS, FAIL, WARNING)",
              })}
              label={intl.formatMessage({
                id: "dropdown.selectColumn",
                defaultMessage: "Select a column...",
              })}
              items={headerOptions}
              itemToString={(item) => item?.label || ""}
              selectedItem={
                headerOptions.find((opt) => opt.id === mapping.qcColumn) ||
                headerOptions[0]
              }
              onChange={({ selectedItem }) =>
                handleMappingChange("qcColumn", selectedItem?.id || "")
              }
            />

            {/* Notes Column - OPTIONAL */}
            <Dropdown
              id="notes-column-select"
              titleText={intl.formatMessage({
                id: "columnMapping.notesColumn",
                defaultMessage: "Notes Column",
              })}
              helperText={intl.formatMessage({
                id: "columnMapping.notesColumnHelper",
                defaultMessage: "Additional notes or comments",
              })}
              label={intl.formatMessage({
                id: "dropdown.selectColumn",
                defaultMessage: "Select a column...",
              })}
              items={headerOptions}
              itemToString={(item) => item?.label || ""}
              selectedItem={
                headerOptions.find((opt) => opt.id === mapping.notesColumn) ||
                headerOptions[0]
              }
              onChange={({ selectedItem }) =>
                handleMappingChange("notesColumn", selectedItem?.id || "")
              }
            />
          </Stack>
        </FormGroup>

        {/* Data Preview */}
        <div>
          <h6
            style={{
              marginBottom: "1rem",
              fontSize: "0.875rem",
              fontWeight: "600",
            }}
          >
            <FormattedMessage
              id="columnMapping.dataPreview"
              defaultMessage="Data Preview (First 5 Rows)"
            />
          </h6>

          <FileDataPreviewTable
            data={previewData}
            columns={previewColumns}
            maxRows={5}
            showRowNumbers={true}
          />
        </div>

        {/* Mapping Summary */}
        {mapping.resultColumn && (
          <div
            style={{
              padding: "1rem",
              backgroundColor: "#f4f4f4",
              borderRadius: "4px",
              fontSize: "0.75rem",
              color: "#525252",
              lineHeight: "1.5",
            }}
          >
            <strong>
              <FormattedMessage
                id="columnMapping.summary"
                defaultMessage="Mapping Summary:"
              />
            </strong>
            <ul style={{ marginTop: "0.5rem", paddingLeft: "1.5rem" }}>
              <li>
                <FormattedMessage
                  id="columnMapping.summaryResult"
                  defaultMessage="Result from: {column}"
                  values={{
                    column: mapping.resultColumn || "-- Not selected --",
                  }}
                />
              </li>
              {mapping.unitColumn && (
                <li>
                  <FormattedMessage
                    id="columnMapping.summaryUnit"
                    defaultMessage="Unit from: {column}"
                    values={{ column: mapping.unitColumn }}
                  />
                </li>
              )}
              {mapping.qcColumn && (
                <li>
                  <FormattedMessage
                    id="columnMapping.summaryQC"
                    defaultMessage="QC Flag from: {column}"
                    values={{ column: mapping.qcColumn }}
                  />
                </li>
              )}
              {mapping.notesColumn && (
                <li>
                  <FormattedMessage
                    id="columnMapping.summaryNotes"
                    defaultMessage="Notes from: {column}"
                    values={{ column: mapping.notesColumn }}
                  />
                </li>
              )}
            </ul>
          </div>
        )}
      </Stack>
    </Modal>
  );
}

export default ColumnMappingModal;
