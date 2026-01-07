import React, { useState, useEffect } from "react";
import {
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableContainer,
  Button,
  Grid,
  Column,
  TextInput,
  Select,
  SelectItem,
  Checkbox,
} from "@carbon/react";
import { Upload, Download, Search } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";

export default function ImportExportTab() {
  const intl = useIntl();
  const [exportFormat, setExportFormat] = useState("json");
  const [selectedUnits, setSelectedUnits] = useState([]);
  const [importData, setImportData] = useState(null);
  const [exporting, setExporting] = useState(false);
  const [importing, setImporting] = useState(false);
  const [validationErrors, setValidationErrors] = useState([]);
  const [availableUnits, setAvailableUnits] = useState([]);

  // Fetch available units for import/export
  useEffect(() => {
    fetchAvailableUnits();
  }, []);

  const fetchAvailableUnits = async () => {
    try {
      const response = await fetch("/rest/api/lab-units");
      if (response.ok) {
        const data = await response.json();
        const units = data.labUnits.map((unit) => ({
          ...unit,
          selected: false,
        }));
        setAvailableUnits(units);
        setSelectedUnits(units);
      }
    } catch (error) {
      console.error("Error fetching lab units:", error);
      setAvailableUnits([]);
    }
  };

  const toggleUnitSelection = (unitId) => {
    setSelectedUnits((prev) =>
      prev.map((unit) =>
        unit.id === unitId ? { ...unit, selected: !unit.selected } : unit,
      ),
    );
  };

  const toggleAllUnits = () => {
    const allSelected = availableUnits.every((unit) => unit.selected);
    setSelectedUnits((prev) =>
      allSelected
        ? prev.map((unit) => ({ ...unit, selected: false }))
        : prev.map((unit) => ({ ...unit, selected: true })),
    );
  };

  const handleExport = async () => {
    const selectedLabUnits = selectedUnits.filter((unit) => unit.selected);
    if (selectedLabUnits.length === 0) return;

    setExporting(true);
    try {
      const response = await fetch("/rest/api/lab-units/export", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          labUnitIds: selectedLabUnits.map((unit) => unit.id),
          format: exportFormat,
        }),
      });

      if (response.ok) {
        // Trigger file download
        const data = await response.json();
        const blob = new Blob([JSON.stringify(data, null, 2)], {
          type: "application/json",
        });
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement("a");
        link.href = url;
        link.download = `lab-units-export-${new Date().toISOString()}.${exportFormat}`;
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(url);
      }
    } catch (error) {
      console.error("Error exporting lab units:", error);
    } finally {
      setExporting(false);
    }
  };

  const handleImportFile = (event) => {
    const file = event.target.files[0];
    if (!file) return;

    setImporting(true);
    setValidationErrors([]);

    const reader = new FileReader();
    reader.onload = (e) => {
      try {
        const data = JSON.parse(e.target.result);
        const validation = validateImportData(data);
        if (validation.isValid) {
          setImportData(data);
        } else {
          setValidationErrors(validation.errors);
        }
      } catch (error) {
        setValidationErrors([{ general: "Invalid JSON file" }]);
      } finally {
        setImporting(false);
      }
    };

    reader.readAsText(file);
  };

  const validateImportData = (data) => {
    const errors = [];

    if (!Array.isArray(data.labUnits)) {
      errors.push({ general: "Invalid file format" });
      return { isValid: false, errors };
    }

    data.labUnits.forEach((unit, index) => {
      if (!unit.name || unit.name.trim() === "") {
        errors.push({
          general: `Lab unit at index ${index + 1}: Name is required`,
        });
      }

      if (!unit.code || unit.code.trim() === "") {
        errors.push({
          general: `Lab unit at index ${index + 1}: Code is required`,
        });
      }

      if (
        unit.displayOrder !== undefined &&
        (typeof unit.displayOrder !== "number" || unit.displayOrder < 0)
      ) {
        errors.push({
          general: `Lab unit at index ${index + 1}: Display order must be a positive number`,
        });
      }
    });

    return { isValid: errors.length === 0, errors };
  };

  const getImportValidationSummary = () => {
    if (validationErrors.length === 0) return null;

    return (
      <div
        style={{
          marginBottom: "1.5rem",
          padding: "1rem",
          backgroundColor: "#fef2f2",
          borderRadius: "4px",
          border: "1px solid #da1e28",
        }}
      >
        <h4
          style={{
            marginBottom: "1rem",
            color: "#da1e28",
            fontSize: "1.125rem",
            fontWeight: "600",
          }}
        >
          {intl.formatMessage({ id: "import.validation.errors" })}:
        </h4>
        <ul
          style={{
            margin: 0,
            paddingLeft: "1.5rem",
            color: "#525252",
          }}
        >
          {validationErrors.map((error, index) => (
            <li key={index}>{error.general}</li>
          ))}
        </ul>
      </div>
    );
  };

  return (
    <div style={{ padding: "1rem" }}>
      <Grid fullWidth>
        <Column lg={8}>
          {/* Export Section */}
          <div
            style={{
              marginBottom: "2rem",
              padding: "1.5rem",
              backgroundColor: "#ffffff",
              borderRadius: "4px",
              border: "1px solid #e0e0e0",
            }}
          >
            <h3
              style={{
                marginBottom: "1rem",
                fontSize: "1.25rem",
                fontWeight: "600",
                color: "#161616",
              }}
            >
              {intl.formatMessage({ id: "labunit.export.title" })}
            </h3>
            <p
              style={{
                marginBottom: "1.5rem",
                color: "#525252",
                lineHeight: "1.5",
                fontSize: "0.875rem",
              }}
            >
              {intl.formatMessage({ id: "labunit.export.description" })}
            </p>

            <div
              style={{
                display: "flex",
                flexDirection: "column",
                gap: "1.5rem",
              }}
            >
              <div style={{ maxWidth: "300px" }}>
                <Select
                  value={exportFormat}
                  onChange={(e) => setExportFormat(e.target.value)}
                  labelText={intl.formatMessage({
                    id: "labunit.export.format",
                  })}
                >
                  <SelectItem value="json">JSON</SelectItem>
                  <SelectItem value="csv">CSV</SelectItem>
                </Select>
              </div>

              <div
                style={{
                  border: "1px solid #e0e0e0",
                  borderRadius: "4px",
                  padding: "1rem",
                  backgroundColor: "#f8f8f8",
                }}
              >
                <div
                  style={{
                    marginBottom: "1rem",
                    display: "flex",
                    alignItems: "center",
                    gap: "0.75rem",
                  }}
                >
                  <Checkbox
                    checked={availableUnits.every((unit) => unit.selected)}
                    onChange={toggleAllUnits}
                    indeterminate={
                      availableUnits.some((unit) => unit.selected) &&
                      !availableUnits.every((unit) => unit.selected)
                    }
                  />
                  <span style={{ fontWeight: "500" }}>
                    {selectedUnits.filter((unit) => unit.selected).length}{" "}
                    {intl.formatMessage({ id: "labunit.units.selected" })}
                  </span>
                </div>

                <Button
                  onClick={handleExport}
                  disabled={
                    selectedUnits.filter((unit) => unit.selected).length === 0
                  }
                  renderIcon={Download}
                  style={{ width: "100%" }}
                >
                  {intl.formatMessage({ id: "button.export.selected" })}
                </Button>
              </div>
            </div>
          </div>
        </Column>

        <Column lg={8}>
          {/* Import Section */}
          <div
            style={{
              marginBottom: "2rem",
              padding: "1.5rem",
              backgroundColor: "#ffffff",
              borderRadius: "4px",
              border: "1px solid #e0e0e0",
            }}
          >
            <h3
              style={{
                marginBottom: "1rem",
                fontSize: "1.25rem",
                fontWeight: "600",
                color: "#161616",
              }}
            >
              {intl.formatMessage({ id: "labunit.import.title" })}
            </h3>
            <p
              style={{
                marginBottom: "1.5rem",
                color: "#525252",
                lineHeight: "1.5",
                fontSize: "0.875rem",
              }}
            >
              {intl.formatMessage({ id: "labunit.import.description" })}
            </p>

            <div
              style={{
                display: "flex",
                flexDirection: "column",
                gap: "1rem",
              }}
            >
              <div>
                <input
                  type="file"
                  accept=".json"
                  onChange={handleImportFile}
                  style={{
                    display: "none",
                  }}
                  id="import-file-input"
                />
                <Button
                  onClick={() =>
                    document.getElementById("import-file-input").click()
                  }
                  renderIcon={Upload}
                  style={{ width: "100%" }}
                >
                  {intl.formatMessage({ id: "labunit.import.file" })}
                </Button>
              </div>
            </div>

            {/* Import Preview */}
            {importData && (
              <div
                style={{
                  marginTop: "2rem",
                  padding: "1.5rem",
                  backgroundColor: "#f8f8f8",
                  borderRadius: "4px",
                  border: "1px solid #e0e0e0",
                }}
              >
                <h4
                  style={{
                    marginBottom: "1.5rem",
                    fontSize: "1.125rem",
                    fontWeight: "600",
                    color: "#161616",
                  }}
                >
                  {intl.formatMessage({ id: "labunit.import.preview" })}
                </h4>

                <div style={{ marginBottom: "2rem" }}>
                  <h5
                    style={{
                      marginBottom: "1rem",
                      fontSize: "1rem",
                      fontWeight: "500",
                      color: "#161616",
                    }}
                  >
                    {intl.formatMessage({ id: "labunit.import.stats" })}:
                  </h5>
                  <div
                    style={{
                      display: "grid",
                      gridTemplateColumns: "repeat(3, 1fr)",
                      gap: "1rem",
                    }}
                  >
                    <div
                      style={{
                        padding: "1rem",
                        backgroundColor: "#ffffff",
                        borderRadius: "4px",
                        border: "1px solid #e0e0e0",
                        textAlign: "center",
                      }}
                    >
                      <div
                        style={{
                          fontSize: "1.5rem",
                          fontWeight: "600",
                          color: "#0f62fe",
                          marginBottom: "0.5rem",
                        }}
                      >
                        {importData.createCount}
                      </div>
                      <span
                        style={{
                          fontSize: "0.875rem",
                          color: "#525252",
                        }}
                      >
                        {intl.formatMessage({
                          id: "labunit.import.create.count",
                        })}
                      </span>
                    </div>
                    <div
                      style={{
                        padding: "1rem",
                        backgroundColor: "#ffffff",
                        borderRadius: "4px",
                        border: "1px solid #e0e0e0",
                        textAlign: "center",
                      }}
                    >
                      <div
                        style={{
                          fontSize: "1.5rem",
                          fontWeight: "600",
                          color: "#0f62fe",
                          marginBottom: "0.5rem",
                        }}
                      >
                        {importData.updateCount}
                      </div>
                      <span
                        style={{
                          fontSize: "0.875rem",
                          color: "#525252",
                        }}
                      >
                        {intl.formatMessage({
                          id: "labunit.import.update.count",
                        })}
                      </span>
                    </div>
                    <div
                      style={{
                        padding: "1rem",
                        backgroundColor: "#ffffff",
                        borderRadius: "4px",
                        border: "1px solid #e0e0e0",
                        textAlign: "center",
                      }}
                    >
                      <div
                        style={{
                          fontSize: "1.5rem",
                          fontWeight: "600",
                          color: "#f9c74f",
                          marginBottom: "0.5rem",
                        }}
                      >
                        {importData.warningCount}
                      </div>
                      <span
                        style={{
                          fontSize: "0.875rem",
                          color: "#525252",
                        }}
                      >
                        {intl.formatMessage({ id: "labunit.import.warnings" })}
                      </span>
                    </div>
                  </div>
                </div>

                <div style={{ marginBottom: "1.5rem" }}>
                  <DataTable rows={importData.labUnits}>
                    <TableContainer>
                      <Table size="sm">
                        <TableHead>
                          <TableRow>
                            <TableHeader>
                              <Checkbox checked={false} />
                            </TableHeader>
                            <TableHeader>
                              {intl.formatMessage({ id: "labunit.name" })}
                            </TableHeader>
                            <TableHeader>
                              {intl.formatMessage({ id: "labunit.code" })}
                            </TableHeader>
                            <TableHeader>
                              {intl.formatMessage({
                                id: "labunit.import.action",
                              })}
                            </TableHeader>
                          </TableRow>
                        </TableHead>
                        <TableBody>
                          {importData.labUnits.map((unit, index) => (
                            <TableRow key={index}>
                              <TableCell>
                                <Checkbox checked={false} />
                              </TableCell>
                              <TableCell>
                                <span
                                  style={{
                                    fontWeight: "500",
                                    color: "#161616",
                                  }}
                                >
                                  {unit.name}
                                </span>
                              </TableCell>
                              <TableCell>
                                <span
                                  style={{
                                    fontFamily: "monospace",
                                    fontSize: "0.875rem",
                                    color: "#525252",
                                  }}
                                >
                                  {unit.code}
                                </span>
                              </TableCell>
                              <TableCell>
                                {unit.action === "create" ? (
                                  <Tag type="green">Create</Tag>
                                ) : unit.action === "update" ? (
                                  <Tag type="blue">Update</Tag>
                                ) : (
                                  <Tag type="orange">Warning</Tag>
                                )}
                              </TableCell>
                            </TableRow>
                          ))}
                        </TableBody>
                      </Table>
                    </TableContainer>
                  </DataTable>
                </div>

                <div
                  style={{
                    display: "flex",
                    gap: "0.5rem",
                    justifyContent: "flex-end",
                    borderTop: "1px solid #e0e0e0",
                    paddingTop: "1rem",
                  }}
                >
                  <Button kind="secondary" onClick={() => setImportData(null)}>
                    {intl.formatMessage({ id: "button.cancel" })}
                  </Button>
                  <Button
                    onClick={() => {
                      /* Execute import */
                    }}
                    disabled={validationErrors.length > 0}
                  >
                    {intl.formatMessage({ id: "button.import.confirm" })}
                  </Button>
                </div>
              </div>
            )}
          </div>
        </Column>
      </Grid>

      {/* Loading States */}
      {exporting && (
        <div
          style={{
            position: "fixed",
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            backgroundColor: "rgba(255, 255, 255, 0.8)",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            zIndex: 1000,
          }}
        >
          <div
            style={{
              padding: "2rem",
              backgroundColor: "#ffffff",
              borderRadius: "8px",
              boxShadow: "0 4px 6px rgba(0, 0, 0, 0.1)",
              textAlign: "center",
            }}
          >
            <p style={{ margin: 0, color: "#525252" }}>
              {intl.formatMessage({ id: "labunit.exporting" })}
            </p>
          </div>
        </div>
      )}

      {importing && (
        <div
          style={{
            position: "fixed",
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            backgroundColor: "rgba(255, 255, 255, 0.8)",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            zIndex: 1000,
          }}
        >
          <div
            style={{
              padding: "2rem",
              backgroundColor: "#ffffff",
              borderRadius: "8px",
              boxShadow: "0 4px 6px rgba(0, 0, 0, 0.1)",
              textAlign: "center",
            }}
          >
            <p style={{ margin: 0, color: "#525252" }}>
              {intl.formatMessage({ id: "labunit.importing" })}
            </p>
          </div>
        </div>
      )}

      {getImportValidationSummary()}
    </div>
  );
}
