import React, { useMemo, useState, useCallback } from "react";
import {
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableSelectRow,
  TableSelectAll,
  TableExpandRow,
  TableExpandedRow,
  TableExpandHeader,
  Tag,
  Button,
  InlineLoading,
} from "@carbon/react";
import { useIntl, FormattedMessage } from "react-intl";
import { Folder, Document, TrashCan, Chemistry } from "@carbon/icons-react";
import { postToOpenElisServerFullResponse } from "../utils/Utils";

/**
 * SampleResultsTable - Display search results for sample items in a data table.
 *
 * Features:
 * - Carbon DataTable with multi-select capability
 * - Expandable rows to show ordered tests
 * - Parent-child hierarchy indicators
 * - Quantity display with unit of measure
 * - Status visualization with tags
 * - Nesting level indicators
 * - Test cancellation/removal functionality
 * - React Intl for internationalization
 *
 * Props:
 * - sampleItems: Array<SampleItemDTO> - array of sample items to display
 * - onSelectionChange: (selectedIds) => void - callback when selection changes
 * - onTestRemoved: (sampleItemId, testId, testName) => void - callback when a test is removed
 *
 * Related: Feature 001-sample-management, User Story 1, Task T034
 */
function SampleResultsTable({
  sampleItems = [],
  onSelectionChange,
  onTestRemoved,
}) {
  const intl = useIntl();

  // Track which tests are being cancelled (loading state)
  const [cancellingTests, setCancellingTests] = useState({});

  /**
   * Table headers configuration.
   */
  const headers = useMemo(
    () => [
      {
        key: "externalId",
        header: intl.formatMessage({
          id: "sample.management.table.header.externalId",
        }),
      },
      {
        key: "sampleType",
        header: intl.formatMessage({
          id: "sample.management.table.header.sampleType",
        }),
      },
      {
        key: "quantity",
        header: intl.formatMessage({
          id: "sample.management.table.header.quantity",
        }),
      },
      {
        key: "remainingQuantity",
        header: intl.formatMessage({
          id: "sample.management.table.header.remainingQuantity",
        }),
      },
      {
        key: "status",
        header: intl.formatMessage({
          id: "sample.management.table.header.status",
        }),
      },
      {
        key: "tests",
        header: intl.formatMessage({
          id: "sample.management.table.header.tests",
        }),
      },
      {
        key: "hierarchy",
        header: intl.formatMessage({
          id: "sample.management.table.header.hierarchy",
        }),
      },
    ],
    [intl],
  );

  /**
   * Transform sample items to table rows.
   * Uses effectiveRemainingQuantity from backend (already calculated with fallback logic).
   */
  const rows = useMemo(() => {
    return sampleItems.map((item) => {
      // Backend sends effectiveRemainingQuantity which handles null remainingQuantity
      const displayRemaining = item.effectiveRemainingQuantity;
      const testCount = item.orderedTests ? item.orderedTests.length : 0;

      return {
        id: item.id,
        externalId: item.externalId || "-",
        sampleType: item.sampleType || "-",
        quantity: item.quantity
          ? `${item.quantity} ${item.unitOfMeasure || ""}`
          : "-",
        remainingQuantity: displayRemaining
          ? `${displayRemaining} ${item.unitOfMeasure || ""}`
          : "-",
        statusId: item.statusId,
        isAliquot: item.isAliquot,
        nestingLevel: item.nestingLevel || 0,
        hasRemainingQuantity: item.hasRemainingQuantity,
        childAliquotCount: item.childAliquots ? item.childAliquots.length : 0,
        parentExternalId: item.parentExternalId,
        orderedTests: item.orderedTests || [],
        testCount: testCount,
        tests: testCount > 0 ? `${testCount}` : "-",
      };
    });
  }, [sampleItems]);

  /**
   * Handle test cancellation/removal.
   */
  const handleCancelTest = useCallback(
    (sampleItemId, analysisId, testName) => {
      // Set loading state for this specific test
      setCancellingTests((prev) => ({ ...prev, [analysisId]: true }));

      const payload = JSON.stringify({
        analysisId: analysisId,
        sampleItemId: sampleItemId,
      });

      postToOpenElisServerFullResponse(
        "/rest/sample-management/cancel-test",
        payload,
        (response) => {
          setCancellingTests((prev) => ({ ...prev, [analysisId]: false }));

          if (response.ok) {
            // Notify parent to refresh data
            if (onTestRemoved) {
              onTestRemoved(sampleItemId, analysisId, testName);
            }
          } else {
            // Handle error - could show notification
            console.error("Failed to cancel test");
          }
        },
      );
    },
    [onTestRemoved],
  );

  /**
   * Render status tag based on statusId and remaining quantity.
   * Finds the original row data to access all properties.
   */
  const renderStatusTag = (dataTableRow) => {
    // Find the original row data by ID
    const originalRow = rows.find((r) => r.id === dataTableRow.id);
    if (!originalRow) return null;

    // Only show a tag if there's no remaining quantity
    if (!originalRow.hasRemainingQuantity) {
      return (
        <Tag type="red">
          {intl.formatMessage({
            id: "sample.management.status.allVolumeDispensed",
          })}
        </Tag>
      );
    }

    // If there's remaining quantity, don't show a status tag
    return null;
  };

  /**
   * Render tests count with icon.
   */
  const renderTestsCount = (dataTableRow) => {
    const originalRow = rows.find((r) => r.id === dataTableRow.id);
    if (!originalRow) return null;

    if (originalRow.testCount > 0) {
      return (
        <div style={{ display: "flex", alignItems: "center", gap: "4px" }}>
          <Chemistry size={16} />
          <span>{originalRow.testCount}</span>
        </div>
      );
    }
    return <span style={{ color: "#6f6f6f" }}>-</span>;
  };

  /**
   * Render hierarchy indicator showing parent-child relationships.
   * Finds the original row data to access all properties.
   */
  const renderHierarchyIndicator = (dataTableRow) => {
    // Find the original row data by ID
    const originalRow = rows.find((r) => r.id === dataTableRow.id);
    if (!originalRow) return null;

    const nestingIndent = originalRow.nestingLevel * 16; // 16px per level

    return (
      <div style={{ display: "flex", alignItems: "center" }}>
        {originalRow.nestingLevel > 0 && (
          <span
            style={{ marginLeft: `${nestingIndent}px`, marginRight: "4px" }}
          >
            {"└─"}
          </span>
        )}
        {originalRow.childAliquotCount > 0 ? (
          <Folder size={16} style={{ marginRight: "4px" }} />
        ) : (
          <Document size={16} style={{ marginRight: "4px" }} />
        )}
        {originalRow.isAliquot && originalRow.parentExternalId && (
          <span
            style={{ fontSize: "0.75rem", color: "#6f6f6f", marginLeft: "4px" }}
          >
            {intl.formatMessage(
              { id: "sample.management.hierarchy.aliquotOf" },
              { parent: originalRow.parentExternalId },
            )}
          </span>
        )}
        {originalRow.childAliquotCount > 0 && (
          <span
            style={{ fontSize: "0.75rem", color: "#6f6f6f", marginLeft: "4px" }}
          >
            ({originalRow.childAliquotCount}{" "}
            {intl.formatMessage({
              id: "sample.management.hierarchy.aliquots",
            })}
            )
          </span>
        )}
      </div>
    );
  };

  /**
   * Render expanded row content with test details.
   */
  const renderExpandedContent = (row) => {
    const originalRow = rows.find((r) => r.id === row.id);
    if (!originalRow || originalRow.orderedTests.length === 0) {
      return (
        <div
          style={{
            padding: "1rem",
            color: "#6f6f6f",
            fontStyle: "italic",
          }}
        >
          <FormattedMessage id="sample.management.table.noTests" />
        </div>
      );
    }

    return (
      <div style={{ padding: "1rem" }}>
        <div
          style={{
            fontWeight: "600",
            marginBottom: "0.75rem",
            display: "flex",
            alignItems: "center",
            gap: "0.5rem",
          }}
        >
          <Chemistry size={20} />
          <FormattedMessage
            id="sample.management.table.orderedTests"
            values={{ count: originalRow.orderedTests.length }}
          />
        </div>
        <div
          style={{
            display: "grid",
            gridTemplateColumns: "repeat(auto-fill, minmax(300px, 1fr))",
            gap: "0.5rem",
          }}
        >
          {originalRow.orderedTests.map((test) => (
            <div
              key={test.analysisId}
              style={{
                display: "flex",
                alignItems: "center",
                justifyContent: "space-between",
                padding: "0.5rem 0.75rem",
                backgroundColor: "#f4f4f4",
                borderRadius: "4px",
                border: "1px solid #e0e0e0",
              }}
            >
              <div style={{ flex: 1 }}>
                <div style={{ fontWeight: "500" }}>{test.testName}</div>
                <div
                  style={{
                    fontSize: "0.75rem",
                    color: "#6f6f6f",
                    display: "flex",
                    gap: "0.75rem",
                    marginTop: "0.25rem",
                  }}
                >
                  {test.status && (
                    <Tag type={getTestStatusType(test.status)} size="sm">
                      {test.status}
                    </Tag>
                  )}
                  {test.orderedDate && (
                    <span>
                      <FormattedMessage id="sample.management.table.orderedDate" />
                      : {new Date(test.orderedDate).toLocaleDateString()}
                    </span>
                  )}
                </div>
              </div>
              <div style={{ marginLeft: "0.5rem" }}>
                {cancellingTests[test.analysisId] ? (
                  <InlineLoading
                    description={intl.formatMessage({
                      id: "sample.management.table.cancelling",
                    })}
                    status="active"
                  />
                ) : (
                  <Button
                    kind="ghost"
                    size="sm"
                    renderIcon={TrashCan}
                    iconDescription={intl.formatMessage({
                      id: "sample.management.table.cancelTest",
                    })}
                    hasIconOnly
                    onClick={() =>
                      handleCancelTest(row.id, test.analysisId, test.testName)
                    }
                    disabled={!canCancelTest(test.status)}
                    tooltipPosition="left"
                  />
                )}
              </div>
            </div>
          ))}
        </div>
      </div>
    );
  };

  /**
   * Get tag type based on test status.
   */
  const getTestStatusType = (status) => {
    if (!status) return "gray";
    const statusLower = status.toLowerCase();
    if (
      statusLower.includes("complete") ||
      statusLower.includes("final") ||
      statusLower.includes("validated")
    ) {
      return "green";
    }
    if (
      statusLower.includes("cancel") ||
      statusLower.includes("rejected") ||
      statusLower.includes("void")
    ) {
      return "red";
    }
    if (statusLower.includes("pending") || statusLower.includes("waiting")) {
      return "blue";
    }
    if (statusLower.includes("in progress") || statusLower.includes("active")) {
      return "cyan";
    }
    return "gray";
  };

  /**
   * Check if a test can be cancelled based on its status.
   * Tests that are already completed or validated cannot be cancelled.
   */
  const canCancelTest = (status) => {
    if (!status) return true;
    const statusLower = status.toLowerCase();
    // Cannot cancel tests that are already completed, validated, or cancelled
    return !(
      statusLower.includes("complete") ||
      statusLower.includes("final") ||
      statusLower.includes("validated") ||
      statusLower.includes("cancel") ||
      statusLower.includes("rejected") ||
      statusLower.includes("void")
    );
  };

  if (sampleItems.length === 0) {
    return (
      <div
        style={{
          padding: "2rem",
          textAlign: "center",
          color: "#6f6f6f",
        }}
      >
        {intl.formatMessage({ id: "sample.management.table.noResults" })}
      </div>
    );
  }

  return (
    <DataTable
      rows={rows}
      headers={headers}
      isSortable
      render={({
        rows,
        headers,
        getHeaderProps,
        getRowProps,
        getSelectionProps,
        getTableProps,
        getExpandHeaderProps,
        selectedRows,
        selectRow,
      }) => {
        // Notify parent of selection changes
        const notifySelectionChange = (newSelectedRows) => {
          if (onSelectionChange) {
            onSelectionChange(newSelectedRows.map((r) => r.id));
          }
        };

        return (
          <Table {...getTableProps()}>
            <TableHead>
              <TableRow>
                <TableExpandHeader
                  aria-label="expand row"
                  {...getExpandHeaderProps()}
                />
                <TableSelectAll
                  {...getSelectionProps()}
                  onSelect={() => {
                    // Toggle select all
                    if (selectedRows.length === rows.length) {
                      // Deselect all
                      rows.forEach((row) => {
                        if (selectedRows.some((r) => r.id === row.id)) {
                          selectRow(row.id);
                        }
                      });
                      notifySelectionChange([]);
                    } else {
                      // Select all
                      rows.forEach((row) => {
                        if (!selectedRows.some((r) => r.id === row.id)) {
                          selectRow(row.id);
                        }
                      });
                      notifySelectionChange(rows);
                    }
                  }}
                />
                {headers.map((header) => (
                  <TableHeader key={header.key} {...getHeaderProps({ header })}>
                    {header.header}
                  </TableHeader>
                ))}
              </TableRow>
            </TableHead>
            <TableBody>
              {rows.map((row) => {
                // Find original row data for styling and expansion
                const originalRow = sampleItems.find(
                  (item) => item.id === row.id,
                );
                const isAliquotRow = originalRow?.isAliquot;
                const hasTests =
                  originalRow?.orderedTests &&
                  originalRow.orderedTests.length > 0;

                return (
                  <React.Fragment key={row.id}>
                    <TableExpandRow
                      {...getRowProps({ row })}
                      style={{
                        // Add subtle left border for aliquots
                        borderLeft: isAliquotRow ? "3px solid #0f62fe" : "none",
                        backgroundColor: isAliquotRow ? "#f0f7ff" : "inherit",
                      }}
                    >
                      <TableSelectRow
                        {...getSelectionProps({ row })}
                        onSelect={() => {
                          selectRow(row.id);
                          // Calculate new selection after toggle
                          const isCurrentlySelected = selectedRows.some(
                            (r) => r.id === row.id,
                          );
                          const newSelection = isCurrentlySelected
                            ? selectedRows.filter((r) => r.id !== row.id)
                            : [...selectedRows, row];
                          notifySelectionChange(newSelection);
                        }}
                      />
                      {row.cells.map((cell) => (
                        <TableCell key={cell.id}>
                          {cell.info.header === "status"
                            ? renderStatusTag(row)
                            : cell.info.header === "hierarchy"
                              ? renderHierarchyIndicator(row)
                              : cell.info.header === "tests"
                                ? renderTestsCount(row)
                                : cell.value}
                        </TableCell>
                      ))}
                    </TableExpandRow>
                    <TableExpandedRow
                      colSpan={headers.length + 2}
                      className="sample-expanded-row"
                      style={{
                        backgroundColor: hasTests ? "#fafafa" : "#fff",
                      }}
                    >
                      {renderExpandedContent(row)}
                    </TableExpandedRow>
                  </React.Fragment>
                );
              })}
            </TableBody>
          </Table>
        );
      }}
    />
  );
}

export default SampleResultsTable;
