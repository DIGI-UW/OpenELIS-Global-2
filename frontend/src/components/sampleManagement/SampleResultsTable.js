import React, { useMemo } from "react";
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
  Tag,
} from "@carbon/react";
import { useIntl } from "react-intl";
import { Folder, Document } from "@carbon/icons-react";

/**
 * SampleResultsTable - Display search results for sample items in a data table.
 *
 * Features:
 * - Carbon DataTable with multi-select capability
 * - Parent-child hierarchy indicators
 * - Quantity display with unit of measure
 * - Status visualization with tags
 * - Nesting level indicators
 * - React Intl for internationalization
 *
 * Props:
 * - sampleItems: Array<SampleItemDTO> - array of sample items to display
 * - selectedRows: Array<string> - array of selected sample item IDs
 * - onSelectionChange: (selectedIds) => void - callback when selection changes
 *
 * Related: Feature 001-sample-management, User Story 1, Task T034
 */
function SampleResultsTable({
  sampleItems = [],
  selectedRows = [],
  onSelectionChange,
}) {
  const intl = useIntl();

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
        key: "originalQuantity",
        header: intl.formatMessage({
          id: "sample.management.table.header.originalQuantity",
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
   */
  const rows = useMemo(() => {
    return sampleItems.map((item) => ({
      id: item.id,
      externalId: item.externalId || "-",
      sampleType: item.sampleType || "-",
      originalQuantity: item.originalQuantity
        ? `${item.originalQuantity} ${item.unitOfMeasure || ""}`
        : "-",
      remainingQuantity: item.remainingQuantity
        ? `${item.remainingQuantity} ${item.unitOfMeasure || ""}`
        : "-",
      statusId: item.statusId,
      isAliquot: item.isAliquot,
      nestingLevel: item.nestingLevel || 0,
      hasRemainingQuantity: item.hasRemainingQuantity,
      childAliquotCount: item.childAliquots ? item.childAliquots.length : 0,
      parentExternalId: item.parentExternalId,
    }));
  }, [sampleItems]);

  /**
   * Render status tag based on statusId and remaining quantity.
   */
  const renderStatusTag = (row) => {
    if (!row.hasRemainingQuantity) {
      return (
        <Tag type="red">
          {intl.formatMessage({
            id: "sample.management.status.allVolumeDispensed",
          })}
        </Tag>
      );
    }

    // Map statusId to tag types (adjust based on actual status codes)
    // Common status codes might be: 1=Available, 2=In Use, 3=Consumed, etc.
    const statusConfig = {
      1: { type: "green", labelId: "sample.management.status.available" },
      2: { type: "blue", labelId: "sample.management.status.inUse" },
      3: { type: "gray", labelId: "sample.management.status.consumed" },
    };

    const config = statusConfig[row.statusId] || {
      type: "gray",
      labelId: "sample.management.status.unknown",
    };

    return (
      <Tag type={config.type}>{intl.formatMessage({ id: config.labelId })}</Tag>
    );
  };

  /**
   * Render hierarchy indicator showing parent-child relationships.
   */
  const renderHierarchyIndicator = (row) => {
    const nestingIndent = row.nestingLevel * 16; // 16px per level

    return (
      <div style={{ display: "flex", alignItems: "center" }}>
        {row.nestingLevel > 0 && (
          <span
            style={{ marginLeft: `${nestingIndent}px`, marginRight: "4px" }}
          >
            {"└─"}
          </span>
        )}
        {row.childAliquotCount > 0 ? (
          <Folder size={16} style={{ marginRight: "4px" }} />
        ) : (
          <Document size={16} style={{ marginRight: "4px" }} />
        )}
        {row.isAliquot && row.parentExternalId && (
          <span
            style={{ fontSize: "0.75rem", color: "#6f6f6f", marginLeft: "4px" }}
          >
            {intl.formatMessage(
              { id: "sample.management.hierarchy.aliquotOf" },
              { parent: row.parentExternalId },
            )}
          </span>
        )}
        {row.childAliquotCount > 0 && (
          <span
            style={{ fontSize: "0.75rem", color: "#6f6f6f", marginLeft: "4px" }}
          >
            ({row.childAliquotCount}{" "}
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
   * Handle row selection changes.
   */
  const handleSelectionChange = ({ selectedRows }) => {
    const selectedIds = selectedRows.map((row) => row.id);
    if (onSelectionChange) {
      onSelectionChange(selectedIds);
    }
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
      radio={false}
      isSortable
      render={({
        rows,
        headers,
        getHeaderProps,
        getRowProps,
        getSelectionProps,
        getTableProps,
        selectRow,
        selectAll,
        selectedRows,
      }) => (
        <Table {...getTableProps()}>
          <TableHead>
            <TableRow>
              <TableSelectAll
                {...getSelectionProps()}
                onSelect={selectAll}
                checked={selectedRows.length === rows.length && rows.length > 0}
              />
              {headers.map((header) => (
                <TableHeader key={header.key} {...getHeaderProps({ header })}>
                  {header.header}
                </TableHeader>
              ))}
            </TableRow>
          </TableHead>
          <TableBody>
            {rows.map((row) => (
              <TableRow key={row.id} {...getRowProps({ row })}>
                <TableSelectRow
                  {...getSelectionProps({ row })}
                  onSelect={() => selectRow(row.id)}
                  checked={selectedRows.some((r) => r.id === row.id)}
                />
                {row.cells.map((cell) => (
                  <TableCell key={cell.id}>
                    {cell.info.header === "status"
                      ? renderStatusTag(row)
                      : cell.info.header === "hierarchy"
                        ? renderHierarchyIndicator(row)
                        : cell.value}
                  </TableCell>
                ))}
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}
    />
  );
}

export default SampleResultsTable;
