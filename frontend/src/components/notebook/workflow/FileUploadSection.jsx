import React, { useCallback } from "react";
import {
  FileUploader,
  Grid,
  Column,
  Tag,
  Button,
  InlineNotification,
  Stack,
} from "@carbon/react";
import { TrashCan, Play } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";

/**
 * FileUploadSection Component
 *
 * Handles uploading analytical data files (CSV, XLSX, XLS, PDF, MZML, CDF)
 * and displays the list of uploaded files with mapping status.
 *
 * Features:
 * - Drag-and-drop file upload
 * - File validation (type and size)
 * - Display file metadata (name, size, row count)
 * - Show mapping status (mapped/unmapped)
 * - Actions: Preview & Map, Delete
 *
 * @param {Object} props
 * @param {Array} props.uploadedFiles - List of uploaded files with metadata
 * @param {Function} props.onFileUpload - Callback when file is uploaded
 * @param {Function} props.onFileDelete - Callback when file is deleted
 * @param {Function} props.onPreviewAndMap - Callback to open mapping modal
 * @param {string} props.notificationMessage - Optional notification message
 * @param {string} props.notificationType - Notification type (success, error, warning, info)
 * @param {Function} props.onNotificationClose - Callback to close notification
 */
function FileUploadSection({
  uploadedFiles = [],
  onFileUpload,
  onFileDelete,
  onPreviewAndMap,
  notificationMessage,
  notificationType,
  onNotificationClose,
}) {
  const intl = useIntl();

  const handleFileUpload = useCallback(
    (event) => {
      const files = event.target?.files;
      if (!files) return;

      Array.from(files).forEach((file) => {
        // Validate file type
        const validExtensions = [
          ".csv",
          ".xlsx",
          ".xls",
          ".pdf",
          ".mzml",
          ".cdf",
        ];
        const fileExt = file.name.substring(file.name.lastIndexOf("."));

        if (!validExtensions.includes(fileExt.toLowerCase())) {
          if (onFileUpload) {
            onFileUpload(null, {
              error: `Invalid file type: ${fileExt}. Allowed: CSV, XLSX, XLS, PDF, MZML, CDF`,
            });
          }
          return;
        }

        // Validate file size (max 50MB)
        const maxSize = 50 * 1024 * 1024;
        if (file.size > maxSize) {
          if (onFileUpload) {
            onFileUpload(null, {
              error: `File too large: ${file.name}. Maximum size is 50MB`,
            });
          }
          return;
        }

        // For CSV files, parse to get headers and row count
        if (fileExt.toLowerCase() === ".csv") {
          const reader = new FileReader();
          reader.onload = (e) => {
            try {
              const text = e.target.result;
              const lines = text.split("\n").filter((line) => line.trim());

              if (lines.length === 0) {
                if (onFileUpload) {
                  onFileUpload(null, {
                    error: `Empty file: ${file.name}`,
                  });
                }
                return;
              }

              // Find the first line with multiple comma-separated values (actual headers)
              // This handles multi-section CSVs that may have section headers
              let headerLineIndex = 0;
              let headers = [];

              for (let i = 0; i < lines.length; i++) {
                const potentialHeaders = lines[i]
                  .split(",")
                  .map((h) => h.trim().replace(/"/g, ""))
                  .filter((h) => h);

                // A valid header line should have multiple columns (at least 2)
                // and all columns should look like header names (no special patterns)
                if (potentialHeaders.length >= 2) {
                  // Check if this looks like a data row (has numbers, dates, etc)
                  // vs a header row
                  const hasOnlyAlphanumeric = potentialHeaders.every((h) =>
                    /^[a-zA-Z0-9_\-\.]+$/.test(h),
                  );

                  if (hasOnlyAlphanumeric) {
                    headers = potentialHeaders;
                    headerLineIndex = i;
                    break;
                  }
                }
              }

              if (headers.length === 0) {
                if (onFileUpload) {
                  onFileUpload(null, {
                    error: `Could not find CSV headers in file: ${file.name}`,
                  });
                }
                return;
              }

              // Extract preview rows (first 5 data rows after headers)
              const previewRows = lines
                .slice(headerLineIndex + 1, headerLineIndex + 6)
                .map((line) =>
                  line.split(",").map((c) => c.trim().replace(/"/g, "")),
                );

              // Count total data rows (all rows after header line)
              const totalRows = Math.max(0, lines.length - headerLineIndex - 1);

              if (onFileUpload) {
                onFileUpload({
                  name: file.name,
                  size: file.size,
                  fileObj: file,
                  headers,
                  previewRows,
                  totalRows,
                  fileType: "csv",
                });
              }
            } catch (error) {
              if (onFileUpload) {
                onFileUpload(null, {
                  error: `Error parsing file: ${error.message}`,
                });
              }
            }
          };
          reader.readAsText(file);
        } else {
          // For non-CSV files, just store file info
          if (onFileUpload) {
            onFileUpload({
              name: file.name,
              size: file.size,
              fileObj: file,
              headers: [],
              previewRows: [],
              totalRows: 0,
              fileType: "other",
            });
          }
        }
      });
    },
    [onFileUpload],
  );

  const formatFileSize = (bytes) => {
    if (bytes === 0) return "0 Bytes";
    const k = 1024;
    const sizes = ["Bytes", "KB", "MB"];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + " " + sizes[i];
  };

  return (
    <div className="file-upload-section">
      <Stack gap={5}>
        {notificationMessage && (
          <InlineNotification
            kind={notificationType || "info"}
            title={intl.formatMessage({
              id: `notification.${notificationType || "info"}.title`,
              defaultMessage: notificationType || "Info",
            })}
            subtitle={notificationMessage}
            onClose={onNotificationClose}
          />
        )}

        <div>
          <h4>
            <FormattedMessage
              id="notebook.bioanalytical.fileUpload.title"
              defaultMessage="Analytical Data Files"
            />
          </h4>
          <p
            style={{
              fontSize: "0.875rem",
              color: "#6f6f6f",
              marginBottom: "1rem",
            }}
          >
            <FormattedMessage
              id="notebook.bioanalytical.fileUpload.description"
              defaultMessage="Upload raw analytical data files (CSV, XLSX, PDF, MZML, CDF) for processing"
            />
          </p>

          <FileUploader
            accept={[".csv", ".xlsx", ".xls", ".pdf", ".mzml", ".cdf"]}
            buttonLabel={intl.formatMessage({
              id: "button.uploadFiles",
              defaultMessage: "Upload Files",
            })}
            filenameStatus="edit"
            iconDescription={intl.formatMessage({
              id: "button.clearFile",
              defaultMessage: "Clear file",
            })}
            labelDescription={intl.formatMessage({
              id: "fileUpload.dragAndDrop",
              defaultMessage: "Drag and drop files here or click to browse",
            })}
            labelTitle={intl.formatMessage({
              id: "fileUpload.uploadDataFiles",
              defaultMessage: "Upload Data Files",
            })}
            multiple
            onChange={handleFileUpload}
            size="md"
          />
        </div>

        {uploadedFiles && uploadedFiles.length > 0 && (
          <div>
            <h5>
              <FormattedMessage
                id="notebook.bioanalytical.uploadedFiles.title"
                defaultMessage="Uploaded Files ({count})"
                values={{ count: uploadedFiles.length }}
              />
            </h5>

            <Grid fullWidth narrow>
              {uploadedFiles.map((file) => (
                <Column key={file.id} lg={16} md={8} sm={4}>
                  <div
                    style={{
                      display: "flex",
                      alignItems: "center",
                      gap: "0.75rem",
                      padding: "0.75rem",
                      backgroundColor: "#f4f4f4",
                      borderRadius: "4px",
                      marginBottom: "0.5rem",
                    }}
                  >
                    {/* File Info */}
                    <div style={{ flex: 1 }}>
                      <div
                        style={{ fontWeight: "500", marginBottom: "0.25rem" }}
                      >
                        {file.name}
                      </div>
                      <div style={{ fontSize: "0.75rem", color: "#525252" }}>
                        📊 {file.totalRows || 0} rows •{" "}
                        {formatFileSize(file.size || 0)}
                      </div>
                    </div>

                    {/* Mapping Status Tag */}
                    <Tag
                      type={file.mapped ? "green" : "gray"}
                      size="sm"
                      style={{ marginRight: "0.5rem" }}
                    >
                      {file.mapped ? (
                        <FormattedMessage
                          id="fileUpload.status.mapped"
                          defaultMessage="Mapped"
                        />
                      ) : (
                        <FormattedMessage
                          id="fileUpload.status.unmapped"
                          defaultMessage="Unmapped"
                        />
                      )}
                    </Tag>

                    {/* Actions */}
                    <div style={{ display: "flex", gap: "0.5rem" }}>
                      {file.fileType === "csv" && (
                        <Button
                          kind="ghost"
                          size="sm"
                          iconDescription={intl.formatMessage({
                            id: "button.previewAndMap",
                            defaultMessage: "Preview & Map",
                          })}
                          renderIcon={Play}
                          onClick={() => onPreviewAndMap?.(file)}
                        >
                          <FormattedMessage
                            id="button.previewAndMap"
                            defaultMessage="Preview & Map"
                          />
                        </Button>
                      )}

                      <Button
                        kind="danger--ghost"
                        size="sm"
                        iconDescription={intl.formatMessage({
                          id: "button.delete",
                          defaultMessage: "Delete",
                        })}
                        renderIcon={TrashCan}
                        onClick={() => onFileDelete?.(file.id)}
                      />
                    </div>
                  </div>
                </Column>
              ))}
            </Grid>
          </div>
        )}

        {(!uploadedFiles || uploadedFiles.length === 0) && (
          <div
            style={{
              padding: "2rem",
              textAlign: "center",
              color: "#a8a8a8",
              backgroundColor: "#f4f4f4",
              borderRadius: "4px",
            }}
          >
            <p>
              <FormattedMessage
                id="fileUpload.noFiles"
                defaultMessage="No files uploaded yet"
              />
            </p>
          </div>
        )}
      </Stack>
    </div>
  );
}

export default FileUploadSection;
