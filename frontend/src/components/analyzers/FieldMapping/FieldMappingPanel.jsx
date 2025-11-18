/**
 * FieldMappingPanel Component
 * 
 * Left panel displaying analyzer fields table
 * Task Reference: T060
 */

import React from "react";
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
} from "@carbon/react";
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

  // Table headers
  const headers = [
    { key: "fieldName", header: "Field Name" },
    { key: "astmRef", header: "ASTM Ref" },
    { key: "type", header: "Type" },
    { key: "unit", header: "Unit" },
    { key: "action", header: "Action" },
  ];

  // Format fields for table rows
  const rows = fields.map((field) => {
    const hasMapping = mappings.some((m) => m.analyzerFieldId === field.id);
    
    return {
      id: field.id,
      fieldName: field.fieldName || "-",
      astmRef: field.astmRef || "-",
      type: field.fieldType || "-",
      unit: field.unit || "-",
      hasMapping: hasMapping,
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
          <FormattedMessage id="analyzer.fieldMapping.panel.source.title" values={{ type: "All" }} />
        </h3>
        <Search
          data-testid="field-mapping-search"
          placeholder={intl.formatMessage({ id: "analyzer.fieldMapping.panel.source.search" })}
          value={searchTerm}
          onChange={(e) => onSearchChange(e.target.value)}
          size="lg"
        />
      </div>

      <TableContainer data-testid="field-mapping-table-container">
        <DataTable rows={rows} headers={headers} isSortable>
          {({ rows, headers, getHeaderProps, getRowProps, getTableProps }) => (
            <Table {...getTableProps()} data-testid="field-mapping-table">
              <TableHead>
                <TableRow>
                  {headers.map((header) => (
                    <TableHeader key={header.key} {...getHeaderProps({ header })}>
                      {header.header}
                    </TableHeader>
                  ))}
                </TableRow>
              </TableHead>
              <TableBody>
                {rows.map((row) => {
                  const field = row._field || fields.find((f) => f.id === row.id);
                  const isSelected = selectedField && selectedField.id === row.id;
                  
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
                        } else if (headerKey === "astmRef") {
                          testId = `field-astm-ref-${row.id}`;
                        } else if (headerKey === "type") {
                          testId = `field-type-${row.id}`;
                          cellContent = (
                            <Tag type={getFieldTypeColor(cell.value)}>
                              {cell.value}
                            </Tag>
                          );
                        } else if (headerKey === "unit") {
                          testId = `field-unit-${row.id}`;
                        } else if (headerKey === "action") {
                          testId = `field-action-${row.id}`;
                          cellContent = cell.value === "Mapped" ? (
                            <Tag type="green">Mapped</Tag>
                          ) : (
                            <Tag type="gray">Unmapped</Tag>
                          );
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
          values={{ count: fields.length }}
        />
      </div>
    </div>
  );
};

export default FieldMappingPanel;

