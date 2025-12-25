import { useState, useMemo, useCallback } from "react";
import {
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableContainer,
  Tag,
  Button,
  Tile,
} from "@carbon/react";
import {
  ChevronRight,
  ChevronDown,
  Add,
  View,
  Renew,
  DocumentView,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import "./NotebookWorkflow.css";

/**
 * PathologyHierarchyTable - Displays pathology samples in a hierarchical tree structure.
 * Shows: Specimen → Cassette → Paraffin Block → Slide
 * Each level displays aliquot counts and children.
 *
 * @param {Object} props
 * @param {Array} props.samples - Array of sample objects with parent-child relationships
 * @param {function} props.onCreateChild - Callback when creating child samples
 * @param {function} props.onViewChildren - Callback when viewing children
 * @param {function} props.onProcessSample - Callback when processing a sample
 * @param {function} props.onRefresh - Callback to refresh data
 * @param {boolean} props.loading - Whether data is loading
 */
function PathologyHierarchyTable({
  samples = [],
  onCreateChild,
  onViewChildren,
  onProcessSample,
  onGrossingSample,
  onRefresh,
  loading = false,
}) {
  const intl = useIntl();

  // Track expanded rows
  const [expandedRows, setExpandedRows] = useState(new Set());

  // Sample type hierarchy definition
  const sampleTypeHierarchy = {
    specimen: {
      level: 0,
      icon: "🧪",
      color: "#198038", // green
      childType: "cassette",
      label: intl.formatMessage({
        id: "pathology.hierarchy.specimen",
        defaultMessage: "Specimen",
      }),
    },
    cassette: {
      level: 1,
      icon: "📦",
      color: "#0043ce", // blue
      childType: "block",
      label: intl.formatMessage({
        id: "pathology.hierarchy.cassette",
        defaultMessage: "Cassette",
      }),
    },
    block: {
      level: 2,
      icon: "🧊",
      color: "#8a3ffc", // purple
      childType: "slide",
      label: intl.formatMessage({
        id: "pathology.hierarchy.block",
        defaultMessage: "Block",
      }),
    },
    slide: {
      level: 3,
      icon: "🔬",
      color: "#ff7eb6", // pink
      childType: null,
      label: intl.formatMessage({
        id: "pathology.hierarchy.slide",
        defaultMessage: "Slide",
      }),
    },
  };

  // Determine sample type based on sample type name or prefix
  const getSampleHierarchyType = useCallback((sample) => {
    const sampleType = (sample.sampleType || "").toLowerCase();
    const externalId = (sample.externalId || "").toLowerCase();

    if (
      sampleType.includes("slide") ||
      externalId.startsWith("sld") ||
      externalId.includes("-sld-")
    ) {
      return "slide";
    }
    if (
      sampleType.includes("block") ||
      sampleType.includes("paraffin") ||
      externalId.startsWith("blk") ||
      externalId.includes("-blk-")
    ) {
      return "block";
    }
    if (
      sampleType.includes("cassette") ||
      externalId.startsWith("cas") ||
      externalId.includes("-cas-")
    ) {
      return "cassette";
    }
    // Default to specimen
    return "specimen";
  }, []);

  // Build hierarchical tree from flat samples
  const hierarchyTree = useMemo(() => {
    // Create a map of samples by ID
    const sampleMap = new Map();
    samples.forEach((s) => {
      sampleMap.set(String(s.id), {
        ...s,
        hierarchyType: getSampleHierarchyType(s),
        children: [],
      });
    });

    // Build parent-child relationships
    const rootSamples = [];
    samples.forEach((s) => {
      const sample = sampleMap.get(String(s.id));
      if (s.parentSampleItemId) {
        const parent = sampleMap.get(String(s.parentSampleItemId));
        if (parent) {
          parent.children.push(sample);
        } else {
          // Parent not in current page samples, treat as root
          rootSamples.push(sample);
        }
      } else {
        // No parent, it's a root sample
        rootSamples.push(sample);
      }
    });

    // Sort children by external ID
    const sortChildren = (node) => {
      node.children.sort((a, b) =>
        (a.externalId || "").localeCompare(b.externalId || ""),
      );
      node.children.forEach(sortChildren);
    };
    rootSamples.forEach(sortChildren);

    return rootSamples;
  }, [samples, getSampleHierarchyType]);

  // Count samples by type
  const typeCounts = useMemo(() => {
    const counts = { specimen: 0, cassette: 0, block: 0, slide: 0 };
    samples.forEach((s) => {
      const type = getSampleHierarchyType(s);
      counts[type]++;
    });
    return counts;
  }, [samples, getSampleHierarchyType]);

  // Toggle row expansion
  const toggleExpand = (id) => {
    setExpandedRows((prev) => {
      const newSet = new Set(prev);
      if (newSet.has(id)) {
        newSet.delete(id);
      } else {
        newSet.add(id);
      }
      return newSet;
    });
  };

  // Expand all
  const expandAll = () => {
    const allIds = new Set();
    const collectIds = (node) => {
      if (node.children && node.children.length > 0) {
        allIds.add(String(node.id));
        node.children.forEach(collectIds);
      }
    };
    hierarchyTree.forEach(collectIds);
    setExpandedRows(allIds);
  };

  // Collapse all
  const collapseAll = () => {
    setExpandedRows(new Set());
  };

  // Get status tag
  const getStatusTag = (status) => {
    switch (status) {
      case "COMPLETED":
        return (
          <Tag type="green" size="sm">
            Complete
          </Tag>
        );
      case "IN_PROGRESS":
        return (
          <Tag type="blue" size="sm">
            In Progress
          </Tag>
        );
      case "REJECTED":
        return (
          <Tag type="red" size="sm">
            Rejected
          </Tag>
        );
      default:
        return (
          <Tag type="gray" size="sm">
            Pending
          </Tag>
        );
    }
  };

  // Render a single row with its children
  const renderRow = (sample, depth = 0) => {
    const isExpanded = expandedRows.has(String(sample.id));
    const hasChildren = sample.children && sample.children.length > 0;
    const hierarchyInfo =
      sampleTypeHierarchy[sample.hierarchyType] || sampleTypeHierarchy.specimen;
    const indent = depth * 24;

    const rows = [];

    // Main row
    rows.push(
      <TableRow key={sample.id} className={`hierarchy-row level-${depth}`}>
        {/* Expand/Type Column */}
        <TableCell>
          <div
            style={{
              display: "flex",
              alignItems: "center",
              marginLeft: `${indent}px`,
            }}
          >
            {hasChildren ? (
              <Button
                kind="ghost"
                size="sm"
                hasIconOnly
                iconDescription={isExpanded ? "Collapse" : "Expand"}
                renderIcon={isExpanded ? ChevronDown : ChevronRight}
                onClick={() => toggleExpand(String(sample.id))}
                style={{ minHeight: "24px", padding: "0 4px" }}
              />
            ) : (
              <span style={{ width: "32px" }} />
            )}
            <span
              style={{
                fontSize: "16px",
                marginRight: "8px",
              }}
            >
              {hierarchyInfo.icon}
            </span>
            <Tag
              type="outline"
              size="sm"
              style={{
                borderColor: hierarchyInfo.color,
                color: hierarchyInfo.color,
              }}
            >
              {hierarchyInfo.label}
            </Tag>
          </div>
        </TableCell>

        {/* Sample ID */}
        <TableCell>
          <span style={{ fontWeight: depth === 0 ? 600 : 400 }}>
            {sample.externalId || sample.accessionNumber || "-"}
          </span>
          {sample.accessionNumber && sample.externalId && (
            <span
              style={{ color: "#6f6f6f", fontSize: "12px", marginLeft: "8px" }}
            >
              ({sample.accessionNumber})
            </span>
          )}
        </TableCell>

        {/* Sample Type */}
        <TableCell>{sample.sampleType || "-"}</TableCell>

        {/* Children Count */}
        <TableCell>
          {hasChildren ? (
            <Tag type="high-contrast" size="sm">
              {sample.children.length} {hierarchyInfo.childType || "child"}
              {sample.children.length === 1 ? "" : "s"}
            </Tag>
          ) : sample.childAliquotCount > 0 ? (
            <Tag type="cyan" size="sm">
              {sample.childAliquotCount} {hierarchyInfo.childType || "aliquot"}
              {sample.childAliquotCount === 1 ? "" : "s"}
            </Tag>
          ) : (
            <span style={{ color: "#8d8d8d" }}>—</span>
          )}
        </TableCell>

        {/* Status */}
        <TableCell>{getStatusTag(sample.status)}</TableCell>

        {/* Actions */}
        <TableCell>
          <div style={{ display: "flex", gap: "4px" }}>
            {/* Grossing button - only for specimens (level 0) */}
            {sample.hierarchyType === "specimen" && onGrossingSample && (
              <Button
                kind="ghost"
                size="sm"
                hasIconOnly
                iconDescription="Gross Examination"
                renderIcon={DocumentView}
                onClick={() => onGrossingSample(sample)}
              />
            )}
            {hierarchyInfo.childType && onCreateChild && (
              <Button
                kind="ghost"
                size="sm"
                hasIconOnly
                iconDescription={`Create ${hierarchyInfo.childType}`}
                renderIcon={Add}
                onClick={() => onCreateChild(sample, hierarchyInfo.childType)}
              />
            )}
            {hasChildren && onViewChildren && (
              <Button
                kind="ghost"
                size="sm"
                hasIconOnly
                iconDescription="View children"
                renderIcon={View}
                onClick={() => onViewChildren(sample)}
              />
            )}
            {onProcessSample && (
              <Button
                kind="ghost"
                size="sm"
                onClick={() => onProcessSample(sample)}
              >
                Process
              </Button>
            )}
          </div>
        </TableCell>
      </TableRow>,
    );

    // Render children if expanded
    if (isExpanded && hasChildren) {
      sample.children.forEach((child) => {
        rows.push(...renderRow(child, depth + 1));
      });
    }

    return rows;
  };

  return (
    <div className="pathology-hierarchy-table">
      {/* Summary Tiles */}
      <div className="hierarchy-summary" style={{ marginBottom: "1rem" }}>
        <div
          style={{
            display: "flex",
            gap: "1rem",
            flexWrap: "wrap",
            marginBottom: "1rem",
          }}
        >
          {Object.entries(sampleTypeHierarchy).map(([key, info]) => (
            <Tile
              key={key}
              className="hierarchy-count-tile"
              style={{
                padding: "0.75rem 1rem",
                display: "flex",
                alignItems: "center",
                gap: "0.5rem",
                borderLeft: `4px solid ${info.color}`,
              }}
            >
              <span style={{ fontSize: "20px" }}>{info.icon}</span>
              <div>
                <div style={{ fontSize: "0.75rem", color: "#6f6f6f" }}>
                  {info.label}s
                </div>
                <div style={{ fontSize: "1.25rem", fontWeight: 600 }}>
                  {typeCounts[key]}
                </div>
              </div>
            </Tile>
          ))}
        </div>

        {/* Workflow Hierarchy Visual */}
        <div
          style={{
            background: "#f4f4f4",
            padding: "0.75rem 1rem",
            borderRadius: "4px",
            marginBottom: "1rem",
          }}
        >
          <div
            style={{
              display: "flex",
              alignItems: "center",
              gap: "0.5rem",
              flexWrap: "wrap",
            }}
          >
            <span style={{ fontWeight: 500 }}>
              <FormattedMessage
                id="pathology.hierarchy.workflow"
                defaultMessage="Workflow:"
              />
            </span>
            {Object.entries(sampleTypeHierarchy).map(
              ([key, info], idx, arr) => (
                <span
                  key={key}
                  style={{ display: "flex", alignItems: "center" }}
                >
                  <span style={{ color: info.color, fontWeight: 500 }}>
                    {info.icon} {info.label}
                  </span>
                  {idx < arr.length - 1 && (
                    <ChevronRight
                      size={16}
                      style={{ margin: "0 4px", color: "#8d8d8d" }}
                    />
                  )}
                </span>
              ),
            )}
          </div>
        </div>
      </div>

      {/* Action Bar */}
      <div
        style={{
          display: "flex",
          gap: "0.5rem",
          marginBottom: "0.5rem",
          justifyContent: "flex-end",
        }}
      >
        <Button kind="ghost" size="sm" onClick={expandAll}>
          <FormattedMessage
            id="pathology.hierarchy.expandAll"
            defaultMessage="Expand All"
          />
        </Button>
        <Button kind="ghost" size="sm" onClick={collapseAll}>
          <FormattedMessage
            id="pathology.hierarchy.collapseAll"
            defaultMessage="Collapse All"
          />
        </Button>
        {onRefresh && (
          <Button kind="ghost" size="sm" renderIcon={Renew} onClick={onRefresh}>
            <FormattedMessage
              id="pathology.hierarchy.refresh"
              defaultMessage="Refresh"
            />
          </Button>
        )}
      </div>

      {/* Hierarchy Table */}
      <TableContainer>
        <Table size="md">
          <TableHead>
            <TableRow>
              <TableHeader style={{ width: "220px" }}>
                <FormattedMessage
                  id="pathology.hierarchy.type"
                  defaultMessage="Type / Level"
                />
              </TableHeader>
              <TableHeader>
                <FormattedMessage
                  id="pathology.hierarchy.sampleId"
                  defaultMessage="Sample ID"
                />
              </TableHeader>
              <TableHeader>
                <FormattedMessage
                  id="pathology.hierarchy.sampleType"
                  defaultMessage="Sample Type"
                />
              </TableHeader>
              <TableHeader style={{ width: "140px" }}>
                <FormattedMessage
                  id="pathology.hierarchy.children"
                  defaultMessage="Children"
                />
              </TableHeader>
              <TableHeader style={{ width: "100px" }}>
                <FormattedMessage
                  id="pathology.hierarchy.status"
                  defaultMessage="Status"
                />
              </TableHeader>
              <TableHeader style={{ width: "160px" }}>
                <FormattedMessage
                  id="pathology.hierarchy.actions"
                  defaultMessage="Actions"
                />
              </TableHeader>
            </TableRow>
          </TableHead>
          <TableBody>
            {loading ? (
              <TableRow>
                <TableCell colSpan={6} style={{ textAlign: "center" }}>
                  <FormattedMessage
                    id="loading.label"
                    defaultMessage="Loading..."
                  />
                </TableCell>
              </TableRow>
            ) : hierarchyTree.length === 0 ? (
              <TableRow>
                <TableCell colSpan={6} style={{ textAlign: "center" }}>
                  <FormattedMessage
                    id="pathology.hierarchy.noSamples"
                    defaultMessage="No samples available. Register samples from the previous page."
                  />
                </TableCell>
              </TableRow>
            ) : (
              hierarchyTree.map((sample) => renderRow(sample))
            )}
          </TableBody>
        </Table>
      </TableContainer>
    </div>
  );
}

export default PathologyHierarchyTable;
