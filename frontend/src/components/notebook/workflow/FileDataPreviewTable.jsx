import React, { useMemo } from "react";
import {
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
} from "@carbon/react";
import { FormattedMessage } from "react-intl";

/**
 * FileDataPreviewTable Component
 *
 * Displays a preview of CSV data with mapped columns.
 * Used in both:
 * 1. Column mapping modal (shows first 5 rows)
 * 2. Execution confirmation modal (shows all rows)
 *
 * Features:
 * - Display data with selected/mapped columns
 * - Show truncated or full data
 * - Handle different data types
 * - Scrollable for large datasets
 *
 * @param {Object} props
 * @param {Array} props.data - Array of data rows (objects with mapped columns)
 * @param {Array} props.columns - Array of column definitions [{key, header, accessor?}]
 * @param {number} props.maxRows - Maximum rows to display (default: unlimited)
 * @param {boolean} props.showRowNumbers - Show row numbers column (default: true)
 * @param {string} props.title - Optional title for the table
 */
function FileDataPreviewTable({
  data = [],
  columns = [],
  maxRows,
  showRowNumbers = true,
  title,
}) {
  // Limit rows if maxRows specified
  const displayData = useMemo(() => {
    if (!maxRows) return data;
    return data.slice(0, maxRows);
  }, [data, maxRows]);

  // Show empty state
  if (!data || data.length === 0) {
    return (
      <div
        style={{
          padding: "2rem",
          textAlign: "center",
          backgroundColor: "#f4f4f4",
          borderRadius: "4px",
          color: "#a8a8a8",
        }}
      >
        <p>
          <FormattedMessage
            id="table.noData"
            defaultMessage="No data to display"
          />
        </p>
      </div>
    );
  }

  return (
    <div className="file-data-preview-table">
      {title && (
        <h6
          style={{
            marginBottom: "1rem",
            fontSize: "0.875rem",
            fontWeight: "600",
          }}
        >
          {title}
        </h6>
      )}

      <div
        style={{
          overflowX: "auto",
          border: "1px solid #e0e0e0",
          borderRadius: "4px",
        }}
      >
        <table
          style={{
            width: "100%",
            borderCollapse: "collapse",
            fontSize: "0.875rem",
          }}
        >
          <thead>
            <tr
              style={{
                backgroundColor: "#f4f4f4",
                borderBottom: "2px solid #e0e0e0",
              }}
            >
              {showRowNumbers && (
                <th
                  style={{
                    padding: "0.75rem",
                    textAlign: "left",
                    fontWeight: "600",
                    color: "#161616",
                    minWidth: "50px",
                  }}
                >
                  #
                </th>
              )}
              {columns.map((col) => (
                <th
                  key={col.key}
                  style={{
                    padding: "0.75rem",
                    textAlign: "left",
                    fontWeight: "600",
                    color: "#161616",
                    minWidth: "150px",
                    whiteSpace: "nowrap",
                  }}
                >
                  {col.header}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {displayData.map((row, rowIdx) => (
              <tr
                key={rowIdx}
                style={{
                  borderBottom: "1px solid #e0e0e0",
                  backgroundColor: rowIdx % 2 === 0 ? "#ffffff" : "#fafafa",
                }}
              >
                {showRowNumbers && (
                  <td
                    style={{
                      padding: "0.75rem",
                      color: "#525252",
                      fontWeight: "500",
                      backgroundColor: rowIdx % 2 === 0 ? "#f4f4f4" : "#efefef",
                    }}
                  >
                    {rowIdx + 1}
                  </td>
                )}
                {columns.map((col) => {
                  const value = col.accessor ? col.accessor(row) : row[col.key];

                  return (
                    <td
                      key={`${rowIdx}-${col.key}`}
                      style={{
                        padding: "0.75rem",
                        color: "#525252",
                        maxWidth: "300px",
                        overflow: "hidden",
                        textOverflow: "ellipsis",
                        whiteSpace: "nowrap",
                      }}
                      title={value || "--"}
                    >
                      {value || "--"}
                    </td>
                  );
                })}
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {maxRows && data.length > maxRows && (
        <div
          style={{
            marginTop: "0.75rem",
            fontSize: "0.75rem",
            color: "#525252",
            fontStyle: "italic",
          }}
        >
          <FormattedMessage
            id="table.showingXofY"
            defaultMessage="Showing {showing} of {total} rows"
            values={{ showing: displayData.length, total: data.length }}
          />
        </div>
      )}
    </div>
  );
}

export default FileDataPreviewTable;
