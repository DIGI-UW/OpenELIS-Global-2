/**
 * FieldMappingPanel Component
 *
 * Left panel displaying analyzer fields table
 */

import React, { useState, useMemo } from "react";
import {
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Search,
  Tag,
  Dropdown,
  Tooltip,
} from "@carbon/react";
import { WarningAltFilled } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import "./FieldMappingPanel.css";

const FieldMappingPanel = ({
  fields,
  selectedField,
  onFieldSelect,
  searchTerm,
  onSearchChange,
  mappings,
}) => {
  const intl = useIntl();
  const [statusFilter, setStatusFilter] = useState("all"); // "all", "mapped", "unmapped", "draft", "active"

  // Table headers
  const headers = [
    { key: "fieldName", header: "Field Name" },
    { key: "astmRef", header: "ASTM Ref" },
    { key: "type", header: "Type" },
    { key: "unit", header: "Unit" },
    { key: "action", header: "Action" },
  ];

  // Calculate mapped/unmapped and draft/active counts
  const { mappedCount, unmappedCount, draftCount, activeCount } =
    useMemo(() => {
      const mapped = fields.filter((field) =>
        mappings.some((m) => m.analyzerFieldId === field.id),
      ).length;
      const unmapped = fields.length - mapped;
      const draft = fields.filter((field) => {
        const mapping = mappings.find((m) => m.analyzerFieldId === field.id);
        return mapping && mapping.isActive === false;
      }).length;
      const active = fields.filter((field) => {
        const mapping = mappings.find((m) => m.analyzerFieldId === field.id);
        return mapping && mapping.isActive === true;
      }).length;
      return {
        mappedCount: mapped,
        unmappedCount: unmapped,
        draftCount: draft,
        activeCount: active,
      };
    }, [fields, mappings]);

  // Filter fields by status
  const filteredFields = useMemo(() => {
    if (statusFilter === "all") return fields;
    if (statusFilter === "mapped") {
      return fields.filter((field) =>
        mappings.some((m) => m.analyzerFieldId === field.id),
      );
    }
    if (statusFilter === "unmapped") {
      return fields.filter(
        (field) => !mappings.some((m) => m.analyzerFieldId === field.id),
      );
    }
    if (statusFilter === "draft") {
      return fields.filter((field) => {
        const mapping = mappings.find((m) => m.analyzerFieldId === field.id);
        return mapping && mapping.isActive === false;
      });
    }
    if (statusFilter === "active") {
      return fields.filter((field) => {
        const mapping = mappings.find((m) => m.analyzerFieldId === field.id);
        return mapping && mapping.isActive === true;
      });
    }
    return fields;
  }, [fields, mappings, statusFilter]);

  // Format fields for table rows
  const rows = filteredFields.map((field) => {
    const mapping = mappings.find((m) => m.analyzerFieldId === field.id);
    const hasMapping = !!mapping;

    return {
      id: field.id,
      fieldName: field.fieldName || "-",
      astmRef: field.astmRef || "-",
      type: field.fieldType || "-",
      unit: field.unit || "-",
      hasMapping: hasMapping,
      mapping: mapping, // Store mapping object to access mapped field info
      _field: field, // Store full field object
    };
  });

  // Get field type tag color
  const getFieldTypeColor = (fieldType) => {
    const colorMap = {
      NUMERIC: "blue",
      QUALITATIVE: "purple",
      CONTROL_TEST: "green",
      MELTING_POINT: "teal",
      DATE_TIME: "cyan",
      TEXT: "gray",
      CUSTOM: "magenta",
    };
    return colorMap[fieldType] || "gray";
  };

  return (
    <div className="field-mapping-panel" data-testid="field-mapping-panel">
      <div className="panel-header">
        <h3>
          <FormattedMessage
            id="analyzer.fieldMapping.panel.source.title"
            values={{ type: "All" }}
          />
        </h3>
        <div className="panel-header-controls">
          <Search
            data-testid="field-mapping-search"
            labelText={intl.formatMessage({
              id: "analyzer.fieldMapping.panel.source.search",
            })}
            placeholder={intl.formatMessage({
              id: "analyzer.fieldMapping.panel.source.search",
            })}
            value={searchTerm}
            onChange={(e) => onSearchChange(e.target.value)}
            size="lg"
          />
          <Dropdown
            id="status-filter"
            data-testid="field-mapping-status-filter"
            label={intl.formatMessage({
              id: "analyzer.fieldMapping.panel.statusFilter.label",
            })}
            items={[
              {
                id: "all",
                text: intl.formatMessage({
                  id: "analyzer.fieldMapping.panel.statusFilter.all",
                }),
              },
              {
                id: "mapped",
                text: intl.formatMessage({
                  id: "analyzer.fieldMapping.panel.statusFilter.mapped",
                }),
              },
              {
                id: "unmapped",
                text: intl.formatMessage({
                  id: "analyzer.fieldMapping.panel.statusFilter.unmapped",
                }),
              },
              {
                id: "draft",
                text: intl.formatMessage({
                  id: "analyzer.fieldMapping.panel.statusFilter.draft",
                }),
              },
              {
                id: "active",
                text: intl.formatMessage({
                  id: "analyzer.fieldMapping.panel.statusFilter.active",
                }),
              },
            ]}
            selectedItem={
              statusFilter === "all"
                ? {
                    id: "all",
                    text: intl.formatMessage({
                      id: "analyzer.fieldMapping.panel.statusFilter.all",
                    }),
                  }
                : statusFilter === "mapped"
                  ? {
                      id: "mapped",
                      text: intl.formatMessage({
                        id: "analyzer.fieldMapping.panel.statusFilter.mapped",
                      }),
                    }
                  : statusFilter === "unmapped"
                    ? {
                        id: "unmapped",
                        text: intl.formatMessage({
                          id: "analyzer.fieldMapping.panel.statusFilter.unmapped",
                        }),
                      }
                    : statusFilter === "draft"
                      ? {
                          id: "draft",
                          text: intl.formatMessage({
                            id: "analyzer.fieldMapping.panel.statusFilter.draft",
                          }),
                        }
                      : {
                          id: "active",
                          text: intl.formatMessage({
                            id: "analyzer.fieldMapping.panel.statusFilter.active",
                          }),
                        }
            }
            onChange={({ selectedItem }) => {
              if (selectedItem) {
                setStatusFilter(selectedItem.id);
              }
            }}
            size="lg"
          />
        </div>
      </div>

      <TableContainer data-testid="field-mapping-table-container">
        <DataTable rows={rows} headers={headers} isSortable>
          {({ rows, headers, getHeaderProps, getRowProps, getTableProps }) => (
            <Table {...getTableProps()} data-testid="field-mapping-table">
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
                  const field =
                    row._field || fields.find((f) => f.id === row.id);
                  const isSelected =
                    selectedField && selectedField.id === row.id;

                  return (
                    <TableRow
                      key={row.id}
                      {...getRowProps({ row })}
                      className={isSelected ? "selected-row" : ""}
                      onClick={() => field && onFieldSelect(field)}
                      style={{ cursor: "pointer" }}
                      data-testid={`field-row-${row.id}`}
                    >
                      {row.cells.map((cell) => {
                        const headerKey = cell.info.header;
                        let testId = null;
                        let cellContent = cell.value;

                        if (headerKey === "fieldName") {
                          testId = `field-name-${row.id}`;
                          const isUnmapped = !row.hasMapping;
                          const mapping = row.mapping;
                          // Show field name with mapped OpenELIS field below (per Figma design)
                          cellContent = (
                            <div
                              style={{
                                display: "flex",
                                alignItems: "center",
                                gap: "0.5rem",
                              }}
                            >
                              {isUnmapped && (
                                <WarningAltFilled
                                  className="unmapped-field-icon"
                                  title={intl.formatMessage({
                                    id: "analyzer.fieldMapping.panel.unmappedField.tooltip",
                                  })}
                                  data-testid={`unmapped-icon-${row.id}`}
                                />
                              )}
                              <div
                                style={{
                                  display: "flex",
                                  flexDirection: "column",
                                  gap: "0.25rem",
                                }}
                              >
                                <span>{cell.value}</span>
                                {mapping &&
                                  (mapping.openelisFieldName ||
                                    mapping.openelisFieldId) && (
                                    <span
                                      style={{
                                        fontSize: "0.875rem",
                                        color: "var(--cds-text-secondary)",
                                      }}
                                    >
                                      →{" "}
                                      {mapping.openelisFieldName ||
                                        mapping.openelisFieldId}
                                    </span>
                                  )}
                              </div>
                            </div>
                          );
                        } else if (headerKey === "astmRef") {
                          testId = `field-astm-ref-${row.id}`;
                        } else if (headerKey === "type") {
                          testId = `field-type-${row.id}`;
                          const field = row._field;
                          const isCustomType = cell.value === "CUSTOM";
                          const customFieldType = field?.customFieldType;

                          cellContent = (
                            <div>
                              <Tag type={getFieldTypeColor(cell.value)}>
                                {cell.value}
                              </Tag>
                              {isCustomType && customFieldType && (
                                <Tooltip
                                  label={
                                    <div>
                                      <strong>
                                        {customFieldType.displayName}
                                      </strong>
                                      <br />
                                      <span>
                                        Type: {customFieldType.typeName}
                                      </span>
                                    </div>
                                  }
                                >
                                  <Tag
                                    type="magenta"
                                    size="sm"
                                    style={{ marginLeft: "4px" }}
                                  >
                                    {customFieldType.displayName}
                                  </Tag>
                                </Tooltip>
                              )}
                            </div>
                          );
                        } else if (headerKey === "unit") {
                          testId = `field-unit-${row.id}`;
                          const mapping = row.mapping;
                          const unit = cell.value !== "-" ? cell.value : null;
                          // Show unit with unit mapping below (per Figma design)
                          if (unit && mapping && mapping.unitMapping) {
                            // unitMapping can be an object { analyzerUnit, openelisUnit, conversionFactor } or a string
                            const openelisUnit =
                              typeof mapping.unitMapping === "string"
                                ? mapping.unitMapping
                                : mapping.unitMapping.openelisUnit;
                            cellContent = (
                              <div
                                style={{
                                  display: "flex",
                                  flexDirection: "column",
                                  gap: "0.25rem",
                                }}
                              >
                                <span>{unit}</span>
                                {openelisUnit && (
                                  <span
                                    style={{
                                      fontSize: "0.875rem",
                                      color: "var(--cds-text-secondary)",
                                    }}
                                  >
                                    → {openelisUnit}
                                  </span>
                                )}
                              </div>
                            );
                          } else if (unit) {
                            cellContent = <span>{unit}</span>;
                          } else {
                            cellContent = <span>{cell.value}</span>;
                          }
                        } else if (headerKey === "action") {
                          testId = `field-action-${row.id}`;
                          const isMapped = row.hasMapping;
                          const mapping = row.mapping;

                          if (!isMapped) {
                            // Unmapped field
                            cellContent = (
                              <Tag
                                type="red"
                                data-testid={`unmapped-badge-${row.id}`}
                              >
                                {intl.formatMessage({
                                  id: "analyzer.fieldMapping.panel.status.unmapped",
                                })}
                              </Tag>
                            );
                          } else if (mapping && mapping.isActive === false) {
                            // Draft mapping
                            cellContent = (
                              <div
                                style={{
                                  display: "flex",
                                  flexDirection: "column",
                                  gap: "0.25rem",
                                  alignItems: "flex-start",
                                }}
                              >
                                <Tag
                                  type="green"
                                  data-testid={`mapped-badge-${row.id}`}
                                >
                                  {intl.formatMessage({
                                    id: "analyzer.fieldMapping.panel.status.mapped",
                                  })}
                                </Tag>
                                <Tag
                                  type="gray"
                                  data-testid={`draft-badge-${row.id}`}
                                >
                                  {intl.formatMessage({
                                    id: "analyzer.fieldMapping.status.draft",
                                  })}
                                </Tag>
                              </div>
                            );
                          } else {
                            // Active mapping
                            cellContent = (
                              <div
                                style={{
                                  display: "flex",
                                  flexDirection: "column",
                                  gap: "0.25rem",
                                  alignItems: "flex-start",
                                }}
                              >
                                <Tag
                                  type="green"
                                  data-testid={`mapped-badge-${row.id}`}
                                >
                                  {intl.formatMessage({
                                    id: "analyzer.fieldMapping.panel.status.mapped",
                                  })}
                                </Tag>
                                <Tag
                                  type="green"
                                  data-testid={`active-badge-${row.id}`}
                                >
                                  {intl.formatMessage({
                                    id: "analyzer.fieldMapping.status.active",
                                  })}
                                </Tag>
                              </div>
                            );
                          }
                        }

                        return (
                          <TableCell key={cell.id} data-testid={testId}>
                            {cellContent}
                          </TableCell>
                        );
                      })}
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          )}
        </DataTable>
      </TableContainer>

      <div className="panel-footer">
        <FormattedMessage
          id="analyzer.fieldMapping.panel.source.fieldsAvailable"
          values={{ count: filteredFields.length }}
        />
        {" • "}
        <FormattedMessage id="analyzer.fieldMapping.panel.status.mapped" />:{" "}
        {mappedCount}
        {" • "}
        <FormattedMessage id="analyzer.fieldMapping.panel.status.unmapped" />:{" "}
        {unmappedCount}
        {" • "}
        <FormattedMessage id="analyzer.fieldMapping.status.draft" />:{" "}
        {draftCount}
        {" • "}
        <FormattedMessage id="analyzer.fieldMapping.status.active" />:{" "}
        {activeCount}
      </div>
    </div>
  );
};

export default FieldMappingPanel;
