import { useState, useMemo } from "react";
import {
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableToolbar,
  TableToolbarContent,
  TableToolbarSearch,
  TableContainer,
  Pagination,
  Tag,
  OverflowMenu,
  OverflowMenuItem,
  Checkbox,
  Dropdown,
} from "@carbon/react";
import {
  CheckmarkFilled,
  InProgress,
  Pending,
  Folder,
  Document,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import "../workflow/NotebookWorkflow.css";

/**
 * SampleGrid - Displays samples in a DataTable with selection, filtering, and pagination.
 *
 * @param {Object} props
 * @param {string} props.gridId - Unique identifier for this grid instance (required when multiple grids on same page)
 * @param {Array} props.samples - Array of sample objects
 * @param {Array} props.selectedIds - Array of selected sample IDs
 * @param {function} props.onSelectionChange - Callback when selection changes
 * @param {function} props.onSampleClick - Callback when a sample row is clicked
 * @param {function} props.onStatusChange - Callback when sample status changes
 * @param {boolean} props.showSelection - Whether to show selection checkboxes
 * @param {string} props.statusFilter - Current status filter (ALL, PENDING, IN_PROGRESS, COMPLETED)
 * @param {function} props.onStatusFilterChange - Callback when status filter changes
 * @param {boolean} props.loading - Whether data is loading
 * @param {boolean} props.showHierarchy - Whether to show hierarchy column
 * @param {Array} props.additionalColumns - Additional columns to render [{key, header, render}]
 */
function SampleGrid({
  gridId = "default",
  samples = [],
  selectedIds = [],
  onSelectionChange,
  onSampleClick,
  onStatusChange,
  showSelection = true,
  statusFilter = "ALL",
  onStatusFilterChange,
  loading = false,
  showHierarchy = false,
  additionalColumns = [],
}) {
  const intl = useIntl();

  // Pagination state
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  // Search state
  const [searchTerm, setSearchTerm] = useState("");

  // Status filter options
  const statusOptions = [
    {
      id: "ALL",
      text: intl.formatMessage({
        id: "notebook.sample.filter.all",
        defaultMessage: "All",
      }),
    },
    {
      id: "PENDING",
      text: intl.formatMessage({
        id: "notebook.sample.filter.pending",
        defaultMessage: "Pending",
      }),
    },
    {
      id: "IN_PROGRESS",
      text: intl.formatMessage({
        id: "notebook.sample.filter.inProgress",
        defaultMessage: "In Progress",
      }),
    },
    {
      id: "COMPLETED",
      text: intl.formatMessage({
        id: "notebook.sample.filter.completed",
        defaultMessage: "Completed",
      }),
    },
    {
      id: "SKIPPED",
      text: intl.formatMessage({
        id: "notebook.sample.filter.skipped",
        defaultMessage: "Skipped",
      }),
    },
  ];

  // Filter samples based on search and status
  const filteredSamples = useMemo(() => {
    let result = samples;

    // Apply status filter
    if (statusFilter && statusFilter !== "ALL") {
      result = result.filter((s) => s.status === statusFilter);
    }

    // Apply search filter
    if (searchTerm) {
      const term = searchTerm.toLowerCase();
      result = result.filter(
        (s) =>
          (s.externalId && s.externalId.toLowerCase().includes(term)) ||
          (s.accessionNumber &&
            s.accessionNumber.toLowerCase().includes(term)) ||
          (s.sampleType && s.sampleType.toLowerCase().includes(term)) ||
          (s.patientName && s.patientName.toLowerCase().includes(term)),
      );
    }

    return result;
  }, [samples, statusFilter, searchTerm]);

  // Paginate
  const paginatedSamples = useMemo(() => {
    const start = (page - 1) * pageSize;
    return filteredSamples.slice(start, start + pageSize);
  }, [filteredSamples, page, pageSize]);

  // Handle pagination change
  const handlePaginationChange = ({ page: newPage, pageSize: newPageSize }) => {
    setPage(newPage);
    setPageSize(newPageSize);
  };

  // Calculate selection state - must be before handlers that use them
  const allSelected =
    filteredSamples.length > 0 && selectedIds.length === filteredSamples.length;
  const someSelected =
    selectedIds.length > 0 && selectedIds.length < filteredSamples.length;

  // Handle row selection - toggle based on current state
  const handleSelectRow = (id) => {
    if (onSelectionChange) {
      const isCurrentlySelected = selectedIds.includes(id);
      if (isCurrentlySelected) {
        onSelectionChange(selectedIds.filter((sid) => sid !== id));
      } else {
        onSelectionChange([...selectedIds, id]);
      }
    }
  };

  // Handle select all - toggle all based on current state
  const handleSelectAll = () => {
    if (onSelectionChange) {
      // If all are selected, deselect all; otherwise select all
      if (allSelected) {
        onSelectionChange([]);
      } else {
        onSelectionChange(filteredSamples.map((s) => String(s.id)));
      }
    }
  };

  // Get status icon
  const getStatusIcon = (status) => {
    switch (status) {
      case "COMPLETED":
        return <CheckmarkFilled size={16} className="status-icon complete" />;
      case "IN_PROGRESS":
        return <InProgress size={16} className="status-icon in-progress" />;
      case "SKIPPED":
        return (
          <Tag type="gray" size="sm">
            Skipped
          </Tag>
        );
      default:
        return <Pending size={16} className="status-icon pending" />;
    }
  };

  // Get status tag
  const getStatusTag = (status) => {
    switch (status) {
      case "COMPLETED":
        return (
          <Tag type="green">
            <FormattedMessage id="notebook.status.completed" />
          </Tag>
        );
      case "IN_PROGRESS":
        return (
          <Tag type="blue">
            <FormattedMessage id="notebook.status.inProgress" />
          </Tag>
        );
      case "SKIPPED":
        return (
          <Tag type="gray">
            <FormattedMessage id="notebook.status.skipped" />
          </Tag>
        );
      default:
        return (
          <Tag type="gray">
            <FormattedMessage id="notebook.status.pending" />
          </Tag>
        );
    }
  };

  // Table headers
  const baseHeaders = [
    ...(showHierarchy
      ? [
          {
            key: "hierarchy",
            header: intl.formatMessage({
              id: "notebook.sample.hierarchy",
              defaultMessage: "Hierarchy",
            }),
          },
        ]
      : []),
    {
      key: "externalId",
      header: intl.formatMessage({
        id: "notebook.sample.externalId",
        defaultMessage: "External ID",
      }),
    },
    {
      key: "accessionNumber",
      header: intl.formatMessage({
        id: "notebook.sample.accessionNumber",
        defaultMessage: "Accession #",
      }),
    },
    {
      key: "sampleType",
      header: intl.formatMessage({
        id: "notebook.sample.type",
        defaultMessage: "Sample Type",
      }),
    },
    {
      key: "collectionDate",
      header: intl.formatMessage({
        id: "notebook.sample.collectionDate",
        defaultMessage: "Collection Date",
      }),
    },
    {
      key: "status",
      header: intl.formatMessage({
        id: "notebook.sample.status",
        defaultMessage: "Status",
      }),
    },
  ];

  // Add additional column headers
  const additionalHeaders = additionalColumns.map((col) => ({
    key: col.key,
    header: col.header,
  }));

  const headers = [
    ...baseHeaders,
    ...additionalHeaders,
    { key: "actions", header: "" },
  ];

  // Render hierarchy cell with tree indicators
  const renderHierarchyCell = (sample) => {
    const nestingLevel = sample.nestingLevel || 0;
    const nestingIndent = nestingLevel * 16;
    const hasChildren = sample.hasChildren || sample.childAliquotCount > 0;

    return (
      <div style={{ display: "flex", alignItems: "center" }}>
        {nestingLevel > 0 && (
          <span
            style={{ marginLeft: `${nestingIndent}px`, marginRight: "4px" }}
          >
            {"└─"}
          </span>
        )}
        {hasChildren ? (
          <Folder size={16} style={{ marginRight: "4px", color: "#0f62fe" }} />
        ) : (
          <Document
            size={16}
            style={{ marginRight: "4px", color: "#525252" }}
          />
        )}
        {hasChildren && (
          <span style={{ fontSize: "12px", color: "#525252" }}>
            ({sample.childAliquotCount || 0})
          </span>
        )}
        {nestingLevel > 0 && sample.parentExternalId && (
          <span
            style={{ fontSize: "11px", color: "#8d8d8d", marginLeft: "4px" }}
          >
            from {sample.parentExternalId}
          </span>
        )}
      </div>
    );
  };

  // Transform samples to rows
  const rows = paginatedSamples.map((sample) => ({
    id: String(sample.id),
    externalId: sample.externalId || "-",
    accessionNumber: sample.accessionNumber || "-",
    sampleType: sample.sampleType || sample.typeOfSample?.description || "-",
    collectionDate: sample.collectionDate || "-",
    status: sample.status || "PENDING",
    _original: sample,
  }));

  return (
    <div className="sample-grid">
      <TableContainer>
        <TableToolbar>
          <TableToolbarContent>
            <TableToolbarSearch
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder={intl.formatMessage({
                id: "notebook.sample.search",
                defaultMessage: "Search samples...",
              })}
            />
            {onStatusFilterChange && (
              <Dropdown
                id="status-filter"
                label={intl.formatMessage({
                  id: "notebook.sample.filter.label",
                  defaultMessage: "Status",
                })}
                items={statusOptions}
                itemToString={(item) => (item ? item.text : "")}
                selectedItem={statusOptions.find((o) => o.id === statusFilter)}
                onChange={({ selectedItem }) =>
                  onStatusFilterChange(selectedItem?.id || "ALL")
                }
                size="sm"
              />
            )}
          </TableToolbarContent>
        </TableToolbar>

        <Table size="md">
          <TableHead>
            <TableRow>
              {showSelection && (
                <TableHeader className="cds--table-column-checkbox">
                  <Checkbox
                    id={`select-all-rows-${gridId}`}
                    checked={allSelected}
                    indeterminate={someSelected}
                    onChange={() => handleSelectAll()}
                    labelText=""
                    hideLabel
                  />
                </TableHeader>
              )}
              {headers.map((header) => (
                <TableHeader key={header.key}>{header.header}</TableHeader>
              ))}
            </TableRow>
          </TableHead>
          <TableBody>
            {loading ? (
              <TableRow>
                <TableCell colSpan={headers.length + (showSelection ? 1 : 0)}>
                  <FormattedMessage
                    id="loading.label"
                    defaultMessage="Loading..."
                  />
                </TableCell>
              </TableRow>
            ) : rows.length === 0 ? (
              <TableRow>
                <TableCell colSpan={headers.length + (showSelection ? 1 : 0)}>
                  <FormattedMessage
                    id="notebook.sample.noSamples"
                    defaultMessage="No samples found"
                  />
                </TableCell>
              </TableRow>
            ) : (
              rows.map((row) => (
                <TableRow
                  key={row.id}
                  onClick={(e) => {
                    // Only trigger row click if not clicking on checkbox or overflow menu
                    if (
                      !e.target.closest(".cds--checkbox") &&
                      !e.target.closest(".cds--overflow-menu") &&
                      !e.target.closest("input[type='checkbox']")
                    ) {
                      onSampleClick && onSampleClick(row._original);
                    }
                  }}
                  className={selectedIds.includes(row.id) ? "selected" : ""}
                >
                  {showSelection && (
                    <TableCell
                      className="cds--table-column-checkbox"
                      onClick={(e) => e.stopPropagation()}
                    >
                      <Checkbox
                        id={`select-row-${gridId}-${row.id}`}
                        checked={selectedIds.includes(row.id)}
                        onChange={() => handleSelectRow(row.id)}
                        labelText=""
                        hideLabel
                      />
                    </TableCell>
                  )}
                  {showHierarchy && (
                    <TableCell>{renderHierarchyCell(row._original)}</TableCell>
                  )}
                  <TableCell>{row.externalId}</TableCell>
                  <TableCell>{row.accessionNumber}</TableCell>
                  <TableCell>{row.sampleType}</TableCell>
                  <TableCell>{row.collectionDate}</TableCell>
                  <TableCell>{getStatusTag(row.status)}</TableCell>
                  {additionalColumns.map((col) => (
                    <TableCell key={col.key}>
                      {col.render
                        ? col.render(row._original)
                        : row._original[col.key]}
                    </TableCell>
                  ))}
                  <TableCell>
                    <OverflowMenu flipped size="sm">
                      <OverflowMenuItem
                        itemText={intl.formatMessage({
                          id: "notebook.sample.action.view",
                          defaultMessage: "View Details",
                        })}
                        onClick={(e) => {
                          e.stopPropagation();
                          onSampleClick && onSampleClick(row._original);
                        }}
                      />
                      {onStatusChange && row.status !== "COMPLETED" && (
                        <OverflowMenuItem
                          itemText={intl.formatMessage({
                            id: "notebook.sample.action.markComplete",
                            defaultMessage: "Mark Complete",
                          })}
                          onClick={(e) => {
                            e.stopPropagation();
                            onStatusChange(row.id, "COMPLETED");
                          }}
                        />
                      )}
                      {onStatusChange && row.status !== "SKIPPED" && (
                        <OverflowMenuItem
                          itemText={intl.formatMessage({
                            id: "notebook.sample.action.skip",
                            defaultMessage: "Skip",
                          })}
                          onClick={(e) => {
                            e.stopPropagation();
                            onStatusChange(row.id, "SKIPPED");
                          }}
                        />
                      )}
                    </OverflowMenu>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>

        <Pagination
          page={page}
          pageSize={pageSize}
          pageSizes={[10, 25, 50, 100]}
          totalItems={filteredSamples.length}
          onChange={handlePaginationChange}
        />
      </TableContainer>

      {/* Selection summary */}
      {showSelection && selectedIds.length > 0 && (
        <div className="selection-summary">
          <FormattedMessage
            id="notebook.sample.selected"
            defaultMessage="{count} samples selected"
            values={{ count: selectedIds.length }}
          />
        </div>
      )}
    </div>
  );
}

export default SampleGrid;
