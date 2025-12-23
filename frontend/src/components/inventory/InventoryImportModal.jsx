import { useState } from "react";
import {
  Modal,
  FileUploader,
  Dropdown,
  ProgressIndicator,
  ProgressStep,
  Tag,
  InlineNotification,
  Grid,
  Column,
  Loading,
  Tabs,
  Tab,
  TabList,
  TabPanels,
  TabPanel,
  Checkbox,
  Accordion,
  AccordionItem,
  Toggle,
} from "@carbon/react";
import {
  CheckmarkFilled,
  ErrorFilled,
  WarningAltFilled,
  Information,
} from "@carbon/icons-react";
import { FormattedMessage } from "react-intl";
import { InventoryImportAPI } from "./InventoryService";

const STEPS = [
  { id: "upload", label: "Upload & Select Sheets" },
  { id: "item-mapping", label: "Map Item Fields" },
  { id: "lot-mapping", label: "Map Lot Fields" },
  { id: "preview", label: "Preview & Import" },
];

// Fields that belong to Inventory Item
const ITEM_FIELDS = {
  itemName: {
    label: "Item Name",
    required: true,
    description: "The name of the reagent/item (e.g., 'Taq Polymerase')",
  },
  experimentType: {
    label: "Category/Experiment Type",
    required: false,
    description:
      "Category or experiment type (e.g., 'qPCR', 'Sequencing'). If not mapped, sheet name will be used.",
  },
  manufacturer: {
    label: "Manufacturer",
    required: false,
    description: "Manufacturer or supplier name",
  },
  catalogNumber: {
    label: "Catalog Number",
    required: false,
    description: "Manufacturer catalog or part number",
  },
  unit: {
    label: "Unit of Measure",
    required: false,
    description:
      "Unit for quantities (e.g., 'vials', 'ml', 'reactions'). Defaults to 'vial'.",
  },
  storageCondition: {
    label: "Storage Condition",
    required: false,
    description:
      "Temperature/storage requirements (e.g., '-20°C', 'Refrigerator')",
  },
  concentration: {
    label: "Concentration",
    required: false,
    description: "Concentration info (e.g., '5 U/µl', '10 mM')",
  },
  remarks: {
    label: "Description/Remarks",
    required: false,
    description: "Additional notes or description for the item",
  },
};

// Fields that belong to Inventory Lot
const LOT_FIELDS = {
  lotNumber: {
    label: "Lot/Reference Number",
    required: true,
    description:
      "Unique lot or reference number from manufacturer. Required to create lots.",
  },
  quantity: {
    label: "Quantity",
    required: false,
    description: "Current quantity available. Defaults to 1 if not mapped.",
  },
  expirationDate: {
    label: "Expiration Date",
    required: false,
    description:
      "Expiry date in any common format (YYYY-MM-DD, MM/DD/YYYY, etc.)",
  },
  manufacturingDate: {
    label: "Manufacturing/Receipt Date",
    required: false,
    description: "Date of manufacture or when received. Defaults to today.",
  },
  openDate: {
    label: "Date Opened",
    required: false,
    description:
      "If the item has been opened, when was it opened? Sets lot status to IN_USE.",
  },
  storageLocation: {
    label: "Storage Location (Box/Shelf)",
    required: false,
    description:
      "Where is this lot stored? (e.g., 'Box 1', 'Freezer A - Shelf 2')",
  },
};

const InventoryImportModal = ({ open, onClose, onImportComplete }) => {
  const [currentStep, setCurrentStep] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  // File and parse state
  const [selectedFile, setSelectedFile] = useState(null);
  const [parseResult, setParseResult] = useState(null);

  // Sheet selection - allow multiple sheets
  const [selectedSheets, setSelectedSheets] = useState([]);
  const [importAllSheets, setImportAllSheets] = useState(true);

  // Column mapping state - separate for items and lots
  const [itemMapping, setItemMapping] = useState({});
  const [lotMapping, setLotMapping] = useState({});

  // Preview state
  const [previewResult, setPreviewResult] = useState(null);

  // Import result state
  const [importResult, setImportResult] = useState(null);

  // Skip invalid rows option
  const [skipInvalidRows, setSkipInvalidRows] = useState(true);

  const resetState = () => {
    setCurrentStep(0);
    setLoading(false);
    setError(null);
    setSelectedFile(null);
    setParseResult(null);
    setSelectedSheets([]);
    setImportAllSheets(true);
    setItemMapping({});
    setLotMapping({});
    setPreviewResult(null);
    setImportResult(null);
    setSkipInvalidRows(true);
  };

  const handleClose = () => {
    resetState();
    onClose();
  };

  const handleFileChange = async (event) => {
    const file = event.target.files?.[0];
    if (!file) return;

    setSelectedFile(file);
    setError(null);
    setLoading(true);

    try {
      const result = await InventoryImportAPI.parseFile(file);
      setParseResult(result);

      // Auto-select all sheets by default
      if (result.sheetNames && result.sheetNames.length > 0) {
        setSelectedSheets(result.sheetNames);
        setImportAllSheets(true);

        // Apply suggested mapping from first sheet
        const firstSheet = result.sheetNames[0];
        if (result.sheets && result.sheets[firstSheet]?.suggestedMapping) {
          const suggested = result.sheets[firstSheet].suggestedMapping;
          // Split into item and lot mappings
          const itemMap = {};
          const lotMap = {};

          Object.keys(ITEM_FIELDS).forEach((field) => {
            if (suggested[field]) itemMap[field] = suggested[field];
          });
          Object.keys(LOT_FIELDS).forEach((field) => {
            if (suggested[field]) lotMap[field] = suggested[field];
          });

          setItemMapping(itemMap);
          setLotMapping(lotMap);
        }
      }
    } catch (err) {
      setError(err.message || "Failed to parse file");
    } finally {
      setLoading(false);
    }
  };

  const handleSheetToggle = (sheetName, checked) => {
    if (checked) {
      setSelectedSheets((prev) => [...prev, sheetName]);
    } else {
      setSelectedSheets((prev) => prev.filter((s) => s !== sheetName));
    }
  };

  const handleImportAllSheetsToggle = (checked) => {
    setImportAllSheets(checked);
    if (checked && parseResult?.sheetNames) {
      setSelectedSheets(parseResult.sheetNames);
    }
  };

  const handleItemMappingChange = (field, column) => {
    setItemMapping((prev) => ({
      ...prev,
      [field]: column,
    }));
  };

  const handleLotMappingChange = (field, column) => {
    setLotMapping((prev) => ({
      ...prev,
      [field]: column,
    }));
  };

  const handlePreview = async () => {
    if (!selectedFile || selectedSheets.length === 0) return;

    setLoading(true);
    setError(null);

    try {
      // Combine item and lot mappings
      const combinedMapping = { ...itemMapping, ...lotMapping };

      const result = await InventoryImportAPI.previewImport(
        selectedFile,
        selectedSheets.join(","), // Send comma-separated list of sheets
        combinedMapping,
      );
      setPreviewResult(result);
      setCurrentStep(3);
    } catch (err) {
      setError(err.message || "Failed to preview import");
    } finally {
      setLoading(false);
    }
  };

  const handleImport = async () => {
    if (!selectedFile || selectedSheets.length === 0) return;

    setLoading(true);
    setError(null);

    try {
      // Combine item and lot mappings
      const combinedMapping = { ...itemMapping, ...lotMapping };

      const result = await InventoryImportAPI.executeImport(
        selectedFile,
        selectedSheets.join(","), // Send comma-separated list of sheets
        combinedMapping,
        skipInvalidRows, // Pass skip invalid rows option
      );
      setImportResult(result);

      if (onImportComplete) {
        onImportComplete(result);
      }
    } catch (err) {
      setError(err.message || "Failed to execute import");
    } finally {
      setLoading(false);
    }
  };

  const getColumnOptions = () => {
    if (!parseResult?.sheetNames || selectedSheets.length === 0) {
      return [{ id: "", text: "-- Not Mapped --" }];
    }

    // Collect all unique headers from selected sheets
    const allHeaders = new Set();
    selectedSheets.forEach((sheetName) => {
      const headers = parseResult.sheets?.[sheetName]?.headers || [];
      headers.filter((h) => h && h.trim()).forEach((h) => allHeaders.add(h));
    });

    return [
      { id: "", text: "-- Not Mapped --" },
      ...Array.from(allHeaders).map((h) => ({ id: h, text: h })),
    ];
  };

  const isStepValid = () => {
    switch (currentStep) {
      case 0:
        return !!selectedFile && !!parseResult && selectedSheets.length > 0;
      case 1:
        // Item mapping - itemName must be mapped
        return !!itemMapping.itemName;
      case 2:
        // Lot mapping - lotNumber should be mapped for lots to be created
        // But it's optional - user might just want to create items
        return true;
      case 3:
        return previewResult && previewResult.validRows > 0;
      default:
        return true;
    }
  };

  const getTotalRowCount = () => {
    if (!parseResult?.sheets) return 0;
    return selectedSheets.reduce((sum, sheet) => {
      return sum + (parseResult.sheets[sheet]?.rowCount || 0);
    }, 0);
  };

  const renderUploadStep = () => (
    <div style={{ padding: "1rem 0" }}>
      {/* Instructions Section */}
      <div
        style={{
          marginBottom: "1.5rem",
          padding: "1rem",
          backgroundColor: "#f4f4f4",
          borderRadius: "8px",
          border: "1px solid #e0e0e0",
        }}
      >
        <div
          style={{
            display: "flex",
            alignItems: "center",
            marginBottom: "0.75rem",
          }}
        >
          <Information
            size={20}
            style={{ marginRight: "0.5rem", color: "#0f62fe" }}
          />
          <strong>
            <FormattedMessage
              id="inventory.import.instructions.title"
              defaultMessage="File Requirements & Field Guide"
            />
          </strong>
        </div>

        <p
          style={{
            fontSize: "0.875rem",
            color: "#525252",
            marginBottom: "1rem",
          }}
        >
          <FormattedMessage
            id="inventory.import.instructions.description"
            defaultMessage="Your file should have column headers in the first row. Use the table below to understand required and optional fields for each entity type."
          />
        </p>

        {/* Requirements Table */}
        <div style={{ overflowX: "auto" }}>
          <table
            style={{
              width: "100%",
              fontSize: "0.8rem",
              borderCollapse: "collapse",
              backgroundColor: "#ffffff",
            }}
          >
            <thead>
              <tr style={{ backgroundColor: "#e0e0e0" }}>
                <th
                  style={{
                    padding: "0.5rem",
                    textAlign: "left",
                    borderBottom: "2px solid #c6c6c6",
                  }}
                >
                  Entity
                </th>
                <th
                  style={{
                    padding: "0.5rem",
                    textAlign: "left",
                    borderBottom: "2px solid #c6c6c6",
                  }}
                >
                  Required Fields
                </th>
                <th
                  style={{
                    padding: "0.5rem",
                    textAlign: "left",
                    borderBottom: "2px solid #c6c6c6",
                  }}
                >
                  Optional Fields
                </th>
                <th
                  style={{
                    padding: "0.5rem",
                    textAlign: "left",
                    borderBottom: "2px solid #c6c6c6",
                  }}
                >
                  Notes
                </th>
              </tr>
            </thead>
            <tbody>
              <tr style={{ backgroundColor: "#fff1f1" }}>
                <td
                  style={{
                    padding: "0.5rem",
                    fontWeight: "bold",
                    verticalAlign: "top",
                    borderBottom: "1px solid #e0e0e0",
                  }}
                >
                  Inventory Item
                </td>
                <td
                  style={{
                    padding: "0.5rem",
                    verticalAlign: "top",
                    borderBottom: "1px solid #e0e0e0",
                  }}
                >
                  <Tag type="red" size="sm">
                    Item Name
                  </Tag>
                </td>
                <td
                  style={{
                    padding: "0.5rem",
                    verticalAlign: "top",
                    borderBottom: "1px solid #e0e0e0",
                  }}
                >
                  <div
                    style={{
                      display: "flex",
                      flexWrap: "wrap",
                      gap: "0.25rem",
                    }}
                  >
                    <Tag type="gray" size="sm">
                      Category
                    </Tag>
                    <Tag type="gray" size="sm">
                      Manufacturer
                    </Tag>
                    <Tag type="gray" size="sm">
                      Catalog #
                    </Tag>
                    <Tag type="gray" size="sm">
                      Unit
                    </Tag>
                    <Tag type="gray" size="sm">
                      Storage Condition
                    </Tag>
                    <Tag type="gray" size="sm">
                      Concentration
                    </Tag>
                    <Tag type="gray" size="sm">
                      Remarks
                    </Tag>
                  </div>
                </td>
                <td
                  style={{
                    padding: "0.5rem",
                    fontSize: "0.75rem",
                    color: "#525252",
                    verticalAlign: "top",
                    borderBottom: "1px solid #e0e0e0",
                  }}
                >
                  Items with the same name are reused (not duplicated)
                </td>
              </tr>
              <tr style={{ backgroundColor: "#e5f6ff" }}>
                <td
                  style={{
                    padding: "0.5rem",
                    fontWeight: "bold",
                    verticalAlign: "top",
                    borderBottom: "1px solid #e0e0e0",
                  }}
                >
                  Inventory Lot
                </td>
                <td
                  style={{
                    padding: "0.5rem",
                    verticalAlign: "top",
                    borderBottom: "1px solid #e0e0e0",
                  }}
                >
                  <div
                    style={{
                      display: "flex",
                      flexWrap: "wrap",
                      gap: "0.25rem",
                    }}
                  >
                    <Tag type="red" size="sm">
                      Item Name
                    </Tag>
                    <Tag type="blue" size="sm">
                      Lot Number
                    </Tag>
                  </div>
                </td>
                <td
                  style={{
                    padding: "0.5rem",
                    verticalAlign: "top",
                    borderBottom: "1px solid #e0e0e0",
                  }}
                >
                  <div
                    style={{
                      display: "flex",
                      flexWrap: "wrap",
                      gap: "0.25rem",
                    }}
                  >
                    <Tag type="gray" size="sm">
                      Quantity
                    </Tag>
                    <Tag type="gray" size="sm">
                      Expiration Date
                    </Tag>
                    <Tag type="gray" size="sm">
                      Manufacturing Date
                    </Tag>
                    <Tag type="gray" size="sm">
                      Date Opened
                    </Tag>
                    <Tag type="gray" size="sm">
                      Storage Location
                    </Tag>
                  </div>
                </td>
                <td
                  style={{
                    padding: "0.5rem",
                    fontSize: "0.75rem",
                    color: "#525252",
                    verticalAlign: "top",
                    borderBottom: "1px solid #e0e0e0",
                  }}
                >
                  Lot Number must be unique; duplicates are skipped. Quantity
                  defaults to 1.
                </td>
              </tr>
              <tr style={{ backgroundColor: "#defbe6" }}>
                <td
                  style={{
                    padding: "0.5rem",
                    fontWeight: "bold",
                    verticalAlign: "top",
                  }}
                >
                  Storage Location
                </td>
                <td style={{ padding: "0.5rem", verticalAlign: "top" }}>
                  <Tag type="green" size="sm">
                    Location Name
                  </Tag>
                </td>
                <td style={{ padding: "0.5rem", verticalAlign: "top" }}>—</td>
                <td
                  style={{
                    padding: "0.5rem",
                    fontSize: "0.75rem",
                    color: "#525252",
                    verticalAlign: "top",
                  }}
                >
                  Created automatically when Storage Location is mapped; new
                  locations are created if not found
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div
          style={{
            marginTop: "0.75rem",
            fontSize: "0.75rem",
            color: "#6f6f6f",
          }}
        >
          <strong>Tips:</strong> Excel files with multiple sheets will import
          each sheet as a separate category. Date formats like YYYY-MM-DD,
          MM/DD/YYYY, or DD/MM/YYYY are auto-detected.
        </div>
      </div>

      <p style={{ marginBottom: "1rem" }}>
        <FormattedMessage
          id="inventory.import.upload.description"
          defaultMessage="Upload an Excel or CSV file containing your reagent inventory data. Excel files can contain multiple sheets - each sheet can be imported as a separate category."
        />
      </p>

      <FileUploader
        accept={[".xlsx", ".xls", ".csv"]}
        buttonLabel="Select file"
        filenameStatus="edit"
        labelDescription="Supported formats: .xlsx, .xls, .csv (max 20MB)"
        labelTitle="Upload file"
        onChange={handleFileChange}
        size="lg"
      />

      {loading && <Loading description="Parsing file..." withOverlay={false} />}

      {parseResult && (
        <div style={{ marginTop: "1.5rem" }}>
          <h5 style={{ marginBottom: "1rem" }}>
            Found {parseResult.sheetNames.length} sheet(s) with{" "}
            {parseResult.totalRows} total rows
          </h5>

          <Toggle
            id="import-all-sheets"
            labelText="Import all sheets"
            toggled={importAllSheets}
            onToggle={(checked) => handleImportAllSheetsToggle(checked)}
            style={{ marginBottom: "1rem" }}
          />

          {!importAllSheets && (
            <div
              style={{
                maxHeight: "300px",
                overflowY: "auto",
                border: "1px solid #e0e0e0",
                padding: "1rem",
              }}
            >
              {parseResult.sheetNames.map((sheetName) => {
                const sheetInfo = parseResult.sheets?.[sheetName];
                return (
                  <div
                    key={sheetName}
                    style={{
                      display: "flex",
                      alignItems: "center",
                      padding: "0.5rem 0",
                      borderBottom: "1px solid #f4f4f4",
                    }}
                  >
                    <Checkbox
                      id={`sheet-${sheetName}`}
                      labelText={`${sheetName} (${sheetInfo?.rowCount || 0} rows)`}
                      checked={selectedSheets.includes(sheetName)}
                      onChange={(_, { checked }) =>
                        handleSheetToggle(sheetName, checked)
                      }
                    />
                  </div>
                );
              })}
            </div>
          )}

          <div
            style={{
              marginTop: "1rem",
              padding: "1rem",
              backgroundColor: "#f4f4f4",
              borderRadius: "4px",
            }}
          >
            <strong>Selected:</strong> {selectedSheets.length} sheet(s),{" "}
            {getTotalRowCount()} rows to process
          </div>
        </div>
      )}
    </div>
  );

  const renderFieldMapping = (
    fields,
    mapping,
    onMappingChange,
    title,
    description,
  ) => {
    const columnOptions = getColumnOptions();

    // Get sample data to show mapped values
    const sampleRows =
      selectedSheets.length > 0
        ? parseResult?.sheets?.[selectedSheets[0]]?.sampleRows || []
        : [];

    return (
      <div style={{ padding: "1rem 0" }}>
        <h5 style={{ marginBottom: "0.5rem" }}>{title}</h5>
        <p
          style={{
            marginBottom: "1rem",
            fontSize: "0.875rem",
            color: "#525252",
          }}
        >
          {description}
        </p>

        {/* Field Documentation Summary */}
        <Accordion style={{ marginBottom: "1rem" }}>
          <AccordionItem title="View field requirements and validation rules">
            <div
              style={{
                display: "grid",
                gridTemplateColumns: "repeat(auto-fill, minmax(280px, 1fr))",
                gap: "0.75rem",
                padding: "0.5rem 0",
              }}
            >
              {Object.entries(fields).map(([field, config]) => (
                <div
                  key={field}
                  style={{
                    padding: "0.75rem",
                    backgroundColor: config.required ? "#fff1f1" : "#f4f4f4",
                    borderRadius: "4px",
                    borderLeft: `3px solid ${config.required ? "#da1e28" : "#0f62fe"}`,
                  }}
                >
                  <div
                    style={{
                      fontWeight: "bold",
                      fontSize: "0.875rem",
                      marginBottom: "0.25rem",
                    }}
                  >
                    {config.label}
                    {config.required && (
                      <Tag
                        type="red"
                        size="sm"
                        style={{ marginLeft: "0.5rem" }}
                      >
                        Required
                      </Tag>
                    )}
                  </div>
                  <p
                    style={{
                      fontSize: "0.75rem",
                      color: "#525252",
                      margin: 0,
                    }}
                  >
                    {config.description}
                  </p>
                </div>
              ))}
            </div>
          </AccordionItem>
        </Accordion>

        <Grid narrow>
          {Object.entries(fields).map(([field, config]) => {
            const mappedColumn = mapping[field];
            // Get sample value from first row
            const sampleValue =
              mappedColumn && sampleRows.length > 0
                ? sampleRows[0][mappedColumn]
                : null;

            return (
              <Column lg={8} md={4} sm={4} key={field}>
                <div style={{ marginBottom: "1.5rem" }}>
                  <Dropdown
                    id={`mapping-${field}`}
                    titleText={
                      <span>
                        {config.label}
                        {config.required && (
                          <span style={{ color: "#da1e28" }}> *</span>
                        )}
                      </span>
                    }
                    label="Select column from your file"
                    items={columnOptions}
                    itemToString={(item) => (item ? item.text : "")}
                    selectedItem={columnOptions.find(
                      (c) => c.id === mapping[field],
                    )}
                    onChange={({ selectedItem }) =>
                      onMappingChange(field, selectedItem?.id || "")
                    }
                    size="md"
                  />
                  <div
                    style={{
                      fontSize: "0.75rem",
                      color: "#6f6f6f",
                      marginTop: "0.25rem",
                    }}
                  >
                    {config.description}
                  </div>
                  {/* Show sample value when column is mapped */}
                  {sampleValue && (
                    <div
                      style={{
                        fontSize: "0.75rem",
                        color: "#0f62fe",
                        marginTop: "0.25rem",
                        fontStyle: "italic",
                      }}
                    >
                      Sample: "{sampleValue}"
                    </div>
                  )}
                </div>
              </Column>
            );
          })}
        </Grid>

        {/* Sample data from first selected sheet */}
        {selectedSheets.length > 0 && sampleRows.length > 0 && (
          <Accordion>
            <AccordionItem title="View sample data from file (first 5 rows)">
              <div style={{ overflowX: "auto", maxHeight: "250px" }}>
                <table
                  style={{
                    width: "100%",
                    fontSize: "0.75rem",
                    borderCollapse: "collapse",
                  }}
                >
                  <thead>
                    <tr
                      style={{
                        backgroundColor: "#e0e0e0",
                        position: "sticky",
                        top: 0,
                      }}
                    >
                      <th
                        style={{
                          padding: "0.5rem",
                          textAlign: "left",
                          whiteSpace: "nowrap",
                          backgroundColor: "#d0d0d0",
                        }}
                      >
                        #
                      </th>
                      {parseResult.sheets[selectedSheets[0]].headers
                        .filter((h) => h)
                        .map((header, idx) => {
                          // Check if this header is mapped
                          const isMapped =
                            Object.values(mapping).includes(header);
                          return (
                            <th
                              key={idx}
                              style={{
                                padding: "0.5rem",
                                textAlign: "left",
                                whiteSpace: "nowrap",
                                backgroundColor: isMapped
                                  ? "#a6c8ff"
                                  : "#e0e0e0",
                              }}
                            >
                              {header}
                              {isMapped && (
                                <CheckmarkFilled
                                  size={12}
                                  style={{
                                    marginLeft: "0.25rem",
                                    color: "#0f62fe",
                                  }}
                                />
                              )}
                            </th>
                          );
                        })}
                    </tr>
                  </thead>
                  <tbody>
                    {sampleRows.map((row, rowIdx) => (
                      <tr
                        key={rowIdx}
                        style={{
                          backgroundColor:
                            rowIdx % 2 === 0 ? "#f4f4f4" : "#ffffff",
                        }}
                      >
                        <td
                          style={{
                            padding: "0.5rem",
                            color: "#6f6f6f",
                            fontWeight: "bold",
                          }}
                        >
                          {rowIdx + 1}
                        </td>
                        {parseResult.sheets[selectedSheets[0]].headers
                          .filter((h) => h)
                          .map((header, colIdx) => {
                            const isMapped =
                              Object.values(mapping).includes(header);
                            return (
                              <td
                                key={colIdx}
                                style={{
                                  padding: "0.5rem",
                                  whiteSpace: "nowrap",
                                  maxWidth: "150px",
                                  overflow: "hidden",
                                  textOverflow: "ellipsis",
                                  backgroundColor: isMapped
                                    ? "rgba(166, 200, 255, 0.2)"
                                    : "transparent",
                                }}
                              >
                                {row[header] || "-"}
                              </td>
                            );
                          })}
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </AccordionItem>
          </Accordion>
        )}
      </div>
    );
  };

  const renderItemMappingStep = () =>
    renderFieldMapping(
      ITEM_FIELDS,
      itemMapping,
      handleItemMappingChange,
      "Step 1: Map Inventory Item Fields",
      "Map columns from your Excel file to Inventory Item fields. These fields describe the reagent/product itself. The Item Name is required.",
    );

  const renderLotMappingStep = () =>
    renderFieldMapping(
      LOT_FIELDS,
      lotMapping,
      handleLotMappingChange,
      "Step 2: Map Inventory Lot Fields",
      "Map columns for Inventory Lot fields. Lots represent specific batches with lot numbers, quantities, and expiration dates. If Lot Number is not mapped, only items will be created without lots.",
    );

  const renderPreviewStep = () => {
    if (loading && !previewResult) {
      return (
        <div style={{ padding: "2rem", textAlign: "center" }}>
          <Loading description="Analyzing data..." withOverlay={false} />
        </div>
      );
    }

    if (importResult) {
      // Show import results
      return (
        <div style={{ padding: "1rem 0", textAlign: "center" }}>
          {importResult.failedRows === 0 ? (
            <div>
              <CheckmarkFilled
                size={64}
                style={{ color: "#198038", marginBottom: "1rem" }}
              />
              <h4 style={{ marginBottom: "1rem" }}>Import Successful!</h4>
            </div>
          ) : (
            <div>
              <ErrorFilled
                size={64}
                style={{ color: "#da1e28", marginBottom: "1rem" }}
              />
              <h4 style={{ marginBottom: "1rem" }}>
                Import Completed with Some Errors
              </h4>
            </div>
          )}

          <Grid narrow style={{ marginTop: "1.5rem", textAlign: "left" }}>
            <Column lg={4} md={2} sm={2}>
              <div
                style={{
                  padding: "1rem",
                  backgroundColor: "#a7f0ba",
                  textAlign: "center",
                  borderRadius: "4px",
                }}
              >
                <div style={{ fontSize: "2rem", fontWeight: "bold" }}>
                  {importResult.successfulItems}
                </div>
                <div style={{ fontSize: "0.875rem" }}>Items Created</div>
              </div>
            </Column>
            <Column lg={4} md={2} sm={2}>
              <div
                style={{
                  padding: "1rem",
                  backgroundColor: "#a7f0ba",
                  textAlign: "center",
                  borderRadius: "4px",
                }}
              >
                <div style={{ fontSize: "2rem", fontWeight: "bold" }}>
                  {importResult.successfulLots}
                </div>
                <div style={{ fontSize: "0.875rem" }}>Lots Created</div>
              </div>
            </Column>
            <Column lg={4} md={2} sm={2}>
              <div
                style={{
                  padding: "1rem",
                  backgroundColor: "#d0e2ff",
                  textAlign: "center",
                  borderRadius: "4px",
                }}
              >
                <div style={{ fontSize: "2rem", fontWeight: "bold" }}>
                  {importResult.totalRows}
                </div>
                <div style={{ fontSize: "0.875rem" }}>Total Rows</div>
              </div>
            </Column>
            <Column lg={4} md={2} sm={2}>
              <div
                style={{
                  padding: "1rem",
                  backgroundColor:
                    importResult.failedRows > 0 ? "#ffd7d9" : "#e0e0e0",
                  textAlign: "center",
                  borderRadius: "4px",
                }}
              >
                <div style={{ fontSize: "2rem", fontWeight: "bold" }}>
                  {importResult.failedRows}
                </div>
                <div style={{ fontSize: "0.875rem" }}>Skipped/Failed</div>
              </div>
            </Column>
          </Grid>

          {importResult.errors && importResult.errors.length > 0 && (
            <div style={{ marginTop: "1.5rem", textAlign: "left" }}>
              <h5 style={{ marginBottom: "0.5rem" }}>Details:</h5>
              <div
                style={{
                  maxHeight: "200px",
                  overflowY: "auto",
                  backgroundColor: "#f4f4f4",
                  padding: "1rem",
                  borderRadius: "4px",
                }}
              >
                {importResult.errors.slice(0, 50).map((err, idx) => (
                  <div
                    key={idx}
                    style={{
                      fontSize: "0.875rem",
                      padding: "0.25rem 0",
                      color: "#525252",
                    }}
                  >
                    Row {err.row}: {err.error}
                  </div>
                ))}
                {importResult.errors.length > 50 && (
                  <div style={{ color: "#6f6f6f", marginTop: "0.5rem" }}>
                    ... and {importResult.errors.length - 50} more
                  </div>
                )}
              </div>
            </div>
          )}
        </div>
      );
    }

    if (!previewResult) return null;

    return (
      <div style={{ padding: "1rem 0" }}>
        <h5 style={{ marginBottom: "1rem" }}>Import Preview</h5>

        {/* Summary Cards */}
        <Grid narrow style={{ marginBottom: "1.5rem" }}>
          <Column lg={3} md={2} sm={2}>
            <div
              style={{
                padding: "1rem",
                backgroundColor: "#e0e0e0",
                textAlign: "center",
                borderRadius: "4px",
              }}
            >
              <div style={{ fontSize: "1.5rem", fontWeight: "bold" }}>
                {previewResult.totalRows}
              </div>
              <div style={{ fontSize: "0.75rem" }}>Total Rows</div>
            </div>
          </Column>
          <Column lg={3} md={2} sm={2}>
            <div
              style={{
                padding: "1rem",
                backgroundColor: "#a7f0ba",
                textAlign: "center",
                borderRadius: "4px",
              }}
            >
              <div style={{ fontSize: "1.5rem", fontWeight: "bold" }}>
                {previewResult.validRows}
              </div>
              <div style={{ fontSize: "0.75rem" }}>Valid</div>
            </div>
          </Column>
          <Column lg={3} md={2} sm={2}>
            <div
              style={{
                padding: "1rem",
                backgroundColor: "#ffd7d9",
                textAlign: "center",
                borderRadius: "4px",
              }}
            >
              <div style={{ fontSize: "1.5rem", fontWeight: "bold" }}>
                {previewResult.invalidRows}
              </div>
              <div style={{ fontSize: "0.75rem" }}>Invalid</div>
            </div>
          </Column>
          <Column lg={3} md={2} sm={1}>
            <div
              style={{
                padding: "1rem",
                backgroundColor: "#d0e2ff",
                textAlign: "center",
                borderRadius: "4px",
              }}
            >
              <div style={{ fontSize: "1.5rem", fontWeight: "bold" }}>
                {previewResult.newItems}
              </div>
              <div style={{ fontSize: "0.75rem" }}>New Items</div>
            </div>
          </Column>
          <Column lg={4} md={2} sm={1}>
            <div
              style={{
                padding: "1rem",
                backgroundColor: "#d4bbff",
                textAlign: "center",
                borderRadius: "4px",
              }}
            >
              <div style={{ fontSize: "1.5rem", fontWeight: "bold" }}>
                {previewResult.newLots}
              </div>
              <div style={{ fontSize: "0.75rem" }}>New Lots</div>
            </div>
          </Column>
        </Grid>

        {/* Info about what will happen */}
        <InlineNotification
          kind="info"
          title="What will happen"
          subtitle={`${previewResult.newItems} new inventory items will be created. ${previewResult.newLots} new lots will be created. ${previewResult.existingItems} items already exist and will be reused.`}
          hideCloseButton
          lowContrast
          style={{ marginBottom: "1rem" }}
        />

        {/* Warnings */}
        {previewResult.warnings && previewResult.warnings.length > 0 && (
          <InlineNotification
            kind="warning"
            title="Warnings"
            subtitle={previewResult.warnings.join("; ")}
            hideCloseButton
            style={{ marginBottom: "1rem" }}
          />
        )}

        {/* Skip Invalid Rows Option */}
        {previewResult.invalidRows > 0 && (
          <div
            style={{
              marginBottom: "1rem",
              padding: "1rem",
              backgroundColor: "#fff8e1",
              borderRadius: "4px",
              border: "1px solid #ffb300",
              display: "flex",
              alignItems: "center",
              justifyContent: "space-between",
            }}
          >
            <div style={{ display: "flex", alignItems: "center" }}>
              <WarningAltFilled
                size={20}
                style={{ color: "#ffb300", marginRight: "0.5rem" }}
              />
              <div>
                <strong>{previewResult.invalidRows} invalid rows found</strong>
                <p
                  style={{
                    margin: 0,
                    fontSize: "0.875rem",
                    color: "#525252",
                  }}
                >
                  {skipInvalidRows
                    ? "Invalid rows will be skipped during import."
                    : "Import will stop if an invalid row is encountered."}
                </p>
              </div>
            </div>
            <Toggle
              id="skip-invalid-toggle"
              labelText="Skip invalid rows"
              toggled={skipInvalidRows}
              onToggle={(checked) => setSkipInvalidRows(checked)}
              size="sm"
            />
          </div>
        )}

        {/* Preview table - show by sheet */}
        <Tabs>
          <TabList aria-label="Preview by sheet">
            <Tab>All Rows</Tab>
            <Tab>Valid Only</Tab>
            <Tab>Invalid Only</Tab>
          </TabList>
          <TabPanels>
            <TabPanel>{renderPreviewTable(previewResult.rows)}</TabPanel>
            <TabPanel>
              {renderPreviewTable(
                previewResult.rows.filter((r) => r.status === "VALID"),
              )}
            </TabPanel>
            <TabPanel>
              {renderPreviewTable(
                previewResult.rows.filter((r) => r.status === "INVALID"),
              )}
            </TabPanel>
          </TabPanels>
        </Tabs>
      </div>
    );
  };

  const renderPreviewTable = (rows) => {
    const displayRows = rows.slice(0, 100);

    if (displayRows.length === 0) {
      return (
        <div style={{ padding: "2rem", textAlign: "center", color: "#6f6f6f" }}>
          No rows to display
        </div>
      );
    }

    return (
      <div style={{ overflowX: "auto", maxHeight: "400px" }}>
        <table
          style={{
            width: "100%",
            fontSize: "0.8rem",
            borderCollapse: "collapse",
          }}
        >
          <thead>
            <tr
              style={{ backgroundColor: "#e0e0e0", position: "sticky", top: 0 }}
            >
              <th style={{ padding: "0.5rem", textAlign: "left" }}>Row</th>
              <th style={{ padding: "0.5rem", textAlign: "left" }}>Sheet</th>
              <th style={{ padding: "0.5rem", textAlign: "left" }}>
                Item Name
              </th>
              <th style={{ padding: "0.5rem", textAlign: "left" }}>
                Lot Number
              </th>
              <th style={{ padding: "0.5rem", textAlign: "left" }}>Qty</th>
              <th style={{ padding: "0.5rem", textAlign: "left" }}>Status</th>
              <th style={{ padding: "0.5rem", textAlign: "left" }}>Notes</th>
            </tr>
          </thead>
          <tbody>
            {displayRows.map((row, idx) => (
              <tr
                key={idx}
                style={{
                  backgroundColor:
                    row.status === "INVALID"
                      ? "#fff1f1"
                      : idx % 2 === 0
                        ? "#f4f4f4"
                        : "#ffffff",
                }}
              >
                <td style={{ padding: "0.5rem" }}>{row.rowNumber}</td>
                <td
                  style={{
                    padding: "0.5rem",
                    maxWidth: "100px",
                    overflow: "hidden",
                    textOverflow: "ellipsis",
                  }}
                >
                  {row.sheetName}
                </td>
                <td
                  style={{
                    padding: "0.5rem",
                    maxWidth: "150px",
                    overflow: "hidden",
                    textOverflow: "ellipsis",
                  }}
                >
                  {row.itemName || "-"}
                  {row.isNewItem && (
                    <Tag
                      type="blue"
                      size="sm"
                      style={{ marginLeft: "0.25rem" }}
                    >
                      New
                    </Tag>
                  )}
                </td>
                <td style={{ padding: "0.5rem" }}>{row.lotNumber || "-"}</td>
                <td style={{ padding: "0.5rem" }}>
                  {row.mappedData?.quantity || "-"}
                </td>
                <td style={{ padding: "0.5rem" }}>
                  <Tag
                    type={row.status === "VALID" ? "green" : "red"}
                    size="sm"
                  >
                    {row.status}
                  </Tag>
                </td>
                <td
                  style={{
                    padding: "0.5rem",
                    fontSize: "0.75rem",
                    color: "#6f6f6f",
                    maxWidth: "200px",
                  }}
                >
                  {[
                    ...(row.validationErrors || []),
                    ...(row.validationWarnings || []),
                  ].join("; ")}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {rows.length > 100 && (
          <div
            style={{
              padding: "0.5rem",
              textAlign: "center",
              backgroundColor: "#f4f4f4",
              fontSize: "0.875rem",
            }}
          >
            Showing first 100 of {rows.length} rows
          </div>
        )}
      </div>
    );
  };

  const renderStepContent = () => {
    switch (currentStep) {
      case 0:
        return renderUploadStep();
      case 1:
        return renderItemMappingStep();
      case 2:
        return renderLotMappingStep();
      case 3:
        return renderPreviewStep();
      default:
        return null;
    }
  };

  const handleNext = () => {
    if (currentStep === 2) {
      // Moving from lot mapping to preview - fetch preview
      handlePreview();
    } else if (currentStep < 3) {
      setCurrentStep(currentStep + 1);
    }
  };

  const handleBack = () => {
    if (currentStep > 0) {
      if (currentStep === 3 && importResult) {
        // After import, go back to beginning
        resetState();
      } else {
        setCurrentStep(currentStep - 1);
        if (currentStep === 3) {
          setPreviewResult(null);
        }
      }
    }
  };

  const getPrimaryButtonText = () => {
    if (importResult) return "Close";
    if (currentStep === 3) return loading ? "Importing..." : "Import Now";
    if (currentStep === 2) return loading ? "Analyzing..." : "Preview Import";
    return "Next";
  };

  const getPrimaryButtonDisabled = () => {
    if (loading) return true;
    if (importResult) return false;
    if (currentStep === 3) return previewResult?.validRows === 0;
    return !isStepValid();
  };

  const handlePrimaryAction = () => {
    if (importResult) {
      handleClose();
    } else if (currentStep === 3) {
      handleImport();
    } else {
      handleNext();
    }
  };

  return (
    <Modal
      open={open}
      onRequestClose={handleClose}
      modalHeading={
        <FormattedMessage
          id="inventory.import.title"
          defaultMessage="Import Inventory from Excel"
        />
      }
      size="lg"
      primaryButtonText={getPrimaryButtonText()}
      primaryButtonDisabled={getPrimaryButtonDisabled()}
      onRequestSubmit={handlePrimaryAction}
      secondaryButtonText={importResult ? null : "Back"}
      onSecondarySubmit={handleBack}
      hasScrollingContent
    >
      {/* Progress indicator */}
      <ProgressIndicator currentIndex={currentStep} spaceEqually>
        {STEPS.map((step, idx) => (
          <ProgressStep
            key={step.id}
            label={step.label}
            complete={idx < currentStep}
            current={idx === currentStep}
          />
        ))}
      </ProgressIndicator>

      {/* Error notification */}
      {error && (
        <InlineNotification
          kind="error"
          title="Error"
          subtitle={error}
          onClose={() => setError(null)}
          style={{ marginTop: "1rem" }}
        />
      )}

      {/* Step content */}
      <div style={{ minHeight: "400px" }}>{renderStepContent()}</div>

      {loading && currentStep !== 0 && !previewResult && !importResult && (
        <Loading description="Processing..." withOverlay />
      )}
    </Modal>
  );
};

export default InventoryImportModal;
