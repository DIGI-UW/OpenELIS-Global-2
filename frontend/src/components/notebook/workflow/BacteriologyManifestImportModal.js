import React, { useState, useCallback, useEffect } from "react";
import {
  Modal,
  FileUploaderDropContainer,
  FileUploaderItem,
  Select,
  SelectItem,
  Button,
  InlineNotification,
  Tag,
  Loading,
  StructuredListWrapper,
  StructuredListHead,
  StructuredListRow,
  StructuredListCell,
  StructuredListBody,
  Accordion,
  AccordionItem,
  Grid,
  Column,
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
} from "@carbon/react";
import { Download } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import config from "../../../config.json";
import "./NotebookWorkflow.css";

/**
 * Expected dataPoints for Bacteriology Sample Import Manifest.
 * Aligned with Bacteriology Laboratory workflow requirements.
 */
const EXPECTED_DATA_POINTS = {
  projectInfo: [
    {
      key: "projectName",
      label: "Project Name",
      description: "Name of the research project or study",
      example: "Antimicrobial Resistance Study 2024",
      required: true,
    },
    {
      key: "studyId",
      label: "Study ID",
      description: "Unique identifier for the study",
      example: "STUDY-001",
      required: true,
    },
  ],
  sampleIdentity: [
    {
      key: "participantId",
      label: "Participant ID",
      description: "Patient/participant identifier (for clinical samples)",
      example: "PART-001",
      required: false,
    },
    {
      key: "barcode",
      label: "Barcode",
      description: "Sample barcode identifier",
      example: "BACT-2024-001",
      required: true,
    },
  ],
  collectionMetadata: [
    {
      key: "collectionSite",
      label: "Collection Site",
      description: "Where the sample was collected",
      example: "Main Hospital Lab",
      required: true,
    },
    {
      key: "sampleType",
      label: "Sample Type",
      description: "Type of sample (validated against Bacteriology lab types)",
      example: "Blood, Urine, Stool, Wastewater, Dairy Products, Animal Stool",
      required: true,
    },
    {
      key: "collectionDateTime",
      label: "Collection Date & Time",
      description: "When the sample was collected",
      example: "2024-06-15 09:30",
      required: true,
    },
  ],
  receptionMetadata: [
    {
      key: "sampleReceivedDate",
      label: "Sample Received Date",
      description: "Date when sample was received at the lab",
      example: "2024-06-15",
      required: true,
    },
    {
      key: "sampleArrivalTime",
      label: "Sample Arrival Time",
      description: "Time when sample arrived at the lab",
      example: "10:00",
      required: true,
    },
    {
      key: "receivedBy",
      label: "Received By",
      description: "Name/initials of person who received the sample",
      example: "John Doe",
      required: true,
    },
  ],
  storageConditions: [
    {
      key: "storageContainerType",
      label: "Storage Container Type",
      description: "Type of container used for storage",
      example: "Vacutainer Tube, Sterile Cup, Sample Bottle",
      required: true,
    },
    {
      key: "storageTemperatureOnArrival",
      label: "Storage Temperature on Arrival",
      description: "Temperature of sample when received (in Celsius)",
      example: "4, -20, Room Temperature",
      required: true,
    },
  ],
  complianceStatus: [
    {
      key: "consentStatus",
      label: "Consent Status",
      description: "Status of patient consent (for clinical trials)",
      example: "Obtained, Pending, Not Required, Waived",
      required: false,
    },
    {
      key: "crfStatus",
      label: "CRF Status",
      description: "Case Report Form status",
      example: "Complete, Incomplete, Pending, Not Applicable",
      required: false,
    },
  ],
  sampleOriginTracking: [
    {
      key: "sampleOrigin",
      label: "Sample Origin",
      description: "Source category of the sample",
      example: "Human, Animal, Environmental, Food/Beverage",
      required: true,
    },
    {
      key: "sourceLocationFacility",
      label: "Source Location/Facility",
      description: "Specific location or facility of sample origin",
      example: "Central Hospital, Municipal Water Treatment, Local Farm",
      required: true,
    },
  ],
};

/**
 * BacteriologyManifestImportModal - CSV import modal for Bacteriology workflow.
 * Supports mapping Bacteriology-specific columns aligned with the dataPoints schema.
 */
function BacteriologyManifestImportModal({
  open,
  onClose,
  entryId,
  onImportSuccess,
}) {
  const intl = useIntl();

  const [file, setFile] = useState(null);
  const [fileError, setFileError] = useState(null);
  const [csvHeaders, setCsvHeaders] = useState([]);
  const [columnMapping, setColumnMapping] = useState({
    // Project Information
    projectNameColumn: "",
    studyIdColumn: "",
    // Sample Identity
    participantIdColumn: "",
    barcodeColumn: "",
    // Collection Metadata
    collectionSiteColumn: "",
    sampleTypeColumn: "",
    collectionDateTimeColumn: "",
    // Reception Metadata
    sampleReceivedDateColumn: "",
    sampleArrivalTimeColumn: "",
    receivedByColumn: "",
    // Storage Conditions
    storageContainerTypeColumn: "",
    storageTemperatureOnArrivalColumn: "",
    // Compliance Status
    consentStatusColumn: "",
    crfStatusColumn: "",
    // Sample Origin Tracking
    sampleOriginColumn: "",
    sourceLocationFacilityColumn: "",
  });

  const [step, setStep] = useState(1);
  const [previewData, setPreviewData] = useState(null);
  const [previewErrors, setPreviewErrors] = useState([]);
  const [isPreviewLoading, setIsPreviewLoading] = useState(false);
  const [isImporting, setIsImporting] = useState(false);

  // Dynamic sample types from backend
  const [validSampleTypes, setValidSampleTypes] = useState([]);
  const [isSampleTypesLoading, setIsSampleTypesLoading] = useState(false);

  // Fetch valid sample types from backend when modal opens
  useEffect(() => {
    if (open) {
      setIsSampleTypesLoading(true);
      fetch(`${config.serverBaseUrl}/rest/notebook/bacteriology/sample-types`, {
        method: "GET",
        credentials: "include",
        headers: {
          "X-CSRF-Token": localStorage.getItem("CSRF"),
        },
      })
        .then((response) => response.json())
        .then((data) => {
          if (data.sampleTypes) {
            setValidSampleTypes(data.sampleTypes.map((st) => st.description));
          }
        })
        .catch((error) => {
          console.error("Failed to fetch sample types:", error);
          setValidSampleTypes([]);
        })
        .finally(() => {
          setIsSampleTypesLoading(false);
        });
    }
  }, [open]);

  /**
   * Auto-detect and map CSV columns based on header names.
   * Supports both snake_case (from template) and camelCase header formats.
   */
  const autoMapColumns = useCallback((headers) => {
    // Map normalized header names to field names
    // The normalization removes spaces, underscores, hyphens, and slashes
    const columnKeyToField = {
      // Project Information
      projectname: "projectNameColumn",
      project: "projectNameColumn",
      studyid: "studyIdColumn",
      study: "studyIdColumn",
      // Sample Identity
      participantid: "participantIdColumn",
      patientid: "participantIdColumn",
      participant: "participantIdColumn",
      barcode: "barcodeColumn",
      samplebarcode: "barcodeColumn",
      // Collection Metadata
      collectionsite: "collectionSiteColumn",
      site: "collectionSiteColumn",
      sampletype: "sampleTypeColumn",
      type: "sampleTypeColumn",
      collectiondatetime: "collectionDateTimeColumn",
      collectiondate: "collectionDateTimeColumn",
      // Reception Metadata
      samplereceiveddate: "sampleReceivedDateColumn",
      receiveddate: "sampleReceivedDateColumn",
      samplearrivaltime: "sampleArrivalTimeColumn",
      arrivaltime: "sampleArrivalTimeColumn",
      receivedby: "receivedByColumn",
      receptor: "receivedByColumn",
      // Storage Conditions
      storagecontainertype: "storageContainerTypeColumn",
      containertype: "storageContainerTypeColumn",
      container: "storageContainerTypeColumn",
      storagetemperatureonarrival: "storageTemperatureOnArrivalColumn",
      storagetemperature: "storageTemperatureOnArrivalColumn",
      temperature: "storageTemperatureOnArrivalColumn",
      // Compliance Status
      consentstatus: "consentStatusColumn",
      consent: "consentStatusColumn",
      crfstatus: "crfStatusColumn",
      crf: "crfStatusColumn",
      // Sample Origin Tracking
      sampleorigin: "sampleOriginColumn",
      origin: "sampleOriginColumn",
      sourcelocationfacility: "sourceLocationFacilityColumn",
      sourcelocation: "sourceLocationFacilityColumn",
      facility: "sourceLocationFacilityColumn",
    };

    // Also support exact matches for snake_case headers from the template
    const exactMatchMap = {
      project_name: "projectNameColumn",
      study_id: "studyIdColumn",
      participant_id: "participantIdColumn",
      barcode: "barcodeColumn",
      collection_site: "collectionSiteColumn",
      sample_type: "sampleTypeColumn",
      collection_date_time: "collectionDateTimeColumn",
      sample_received_date: "sampleReceivedDateColumn",
      sample_arrival_time: "sampleArrivalTimeColumn",
      received_by: "receivedByColumn",
      storage_container_type: "storageContainerTypeColumn",
      storage_temperature_on_arrival: "storageTemperatureOnArrivalColumn",
      consent_status: "consentStatusColumn",
      crf_status: "crfStatusColumn",
      sample_origin: "sampleOriginColumn",
      source_location_facility: "sourceLocationFacilityColumn",
    };

    const newMapping = {};

    headers.forEach((header) => {
      const trimmedHeader = header.trim();
      const lowerHeader = trimmedHeader.toLowerCase();

      // First try exact match (for snake_case headers from template)
      if (exactMatchMap[lowerHeader]) {
        newMapping[exactMatchMap[lowerHeader]] = trimmedHeader;
        return;
      }

      // Then try normalized match (removes spaces, underscores, hyphens, slashes)
      const normalizedHeader = lowerHeader.replace(/[\s_\-/]/g, "");
      const fieldName = columnKeyToField[normalizedHeader];
      if (fieldName) {
        newMapping[fieldName] = trimmedHeader;
      }
    });

    setColumnMapping((prev) => ({
      ...prev,
      ...newMapping,
    }));
  }, []);

  // Handle file upload
  const handleFileAdded = useCallback(
    (event, { addedFiles }) => {
      const addedFile = addedFiles[0];
      if (!addedFile) return;

      if (!addedFile.name.endsWith(".csv")) {
        setFileError(
          intl.formatMessage({
            id: "notebook.bacteriology.manifest.error.invalidFileType",
            defaultMessage: "Please upload a CSV file",
          }),
        );
        return;
      }

      setFile(addedFile);
      setFileError(null);

      const reader = new FileReader();
      reader.onload = (e) => {
        const text = e.target.result;
        const firstLine = text.split("\n")[0];
        const headers = firstLine
          .split(",")
          .map((h) => h.trim().replace(/"/g, ""));
        setCsvHeaders(headers);
        autoMapColumns(headers);
        setStep(2);
      };
      reader.readAsText(addedFile);
    },
    [intl, autoMapColumns],
  );

  // Handle file removal
  const handleRemoveFile = useCallback(() => {
    setFile(null);
    setCsvHeaders([]);
    setColumnMapping({
      projectNameColumn: "",
      studyIdColumn: "",
      participantIdColumn: "",
      barcodeColumn: "",
      collectionSiteColumn: "",
      sampleTypeColumn: "",
      collectionDateTimeColumn: "",
      sampleReceivedDateColumn: "",
      sampleArrivalTimeColumn: "",
      receivedByColumn: "",
      storageContainerTypeColumn: "",
      storageTemperatureOnArrivalColumn: "",
      consentStatusColumn: "",
      crfStatusColumn: "",
      sampleOriginColumn: "",
      sourceLocationFacilityColumn: "",
    });
    setPreviewData(null);
    setPreviewErrors([]);
    setStep(1);
  }, []);

  // Handle column mapping change
  const handleMappingChange = useCallback((field, value) => {
    setColumnMapping((prev) => ({
      ...prev,
      [field]: value,
    }));
  }, []);

  // Check if required fields are mapped
  const isRequiredMappingComplete = useCallback(() => {
    return (
      columnMapping.barcodeColumn &&
      columnMapping.sampleTypeColumn &&
      columnMapping.sampleOriginColumn &&
      columnMapping.sourceLocationFacilityColumn
    );
  }, [columnMapping]);

  // Preview import
  const handlePreview = useCallback(() => {
    if (!file || !entryId) return;

    setIsPreviewLoading(true);
    setPreviewErrors([]);

    const formData = new FormData();
    formData.append("file", file);
    formData.append(
      "mapping",
      new Blob([JSON.stringify(columnMapping)], { type: "application/json" }),
    );

    fetch(
      `${config.serverBaseUrl}/rest/notebook/bacteriology/entry/${entryId}/samples/preview-manifest`,
      {
        method: "POST",
        credentials: "include",
        headers: {
          "X-CSRF-Token": localStorage.getItem("CSRF"),
        },
        body: formData,
      },
    )
      .then((response) => response.json())
      .then((data) => {
        setIsPreviewLoading(false);
        if (data && data.rows) {
          setPreviewData(data);
          if (data.errors && data.errors.length > 0) {
            setPreviewErrors(data.errors);
          }
          setStep(3);
        } else if (data && data.error) {
          setPreviewErrors([
            { rowNumber: 0, column: "system", message: data.error },
          ]);
        }
      })
      .catch((error) => {
        setIsPreviewLoading(false);
        setPreviewErrors([
          { rowNumber: 0, column: "system", message: error.message },
        ]);
      });
  }, [file, entryId, columnMapping]);

  // Execute import
  const handleImport = useCallback(() => {
    if (!file || !entryId) return;

    setIsImporting(true);

    const formData = new FormData();
    formData.append("file", file);
    formData.append(
      "mapping",
      new Blob([JSON.stringify(columnMapping)], { type: "application/json" }),
    );

    fetch(
      `${config.serverBaseUrl}/rest/notebook/bacteriology/entry/${entryId}/samples/create-from-manifest`,
      {
        method: "POST",
        credentials: "include",
        headers: {
          "X-CSRF-Token": localStorage.getItem("CSRF"),
        },
        body: formData,
      },
    )
      .then((response) => response.json())
      .then((data) => {
        setIsImporting(false);
        if (data && data.success) {
          setStep(4);
          if (onImportSuccess) {
            onImportSuccess(data);
          }
        } else {
          setPreviewErrors([
            {
              rowNumber: 0,
              column: "system",
              message: data?.error || "Import failed",
            },
          ]);
        }
      })
      .catch((error) => {
        setIsImporting(false);
        setPreviewErrors([
          { rowNumber: 0, column: "system", message: error.message },
        ]);
      });
  }, [file, entryId, columnMapping, onImportSuccess]);

  // Close modal and reset
  const handleClose = useCallback(() => {
    handleRemoveFile();
    if (onClose) {
      onClose();
    }
  }, [handleRemoveFile, onClose]);

  // Download CSV template
  const handleDownloadTemplate = useCallback(() => {
    window.open(
      `${config.serverBaseUrl}/rest/notebook/bacteriology/manifest-template`,
      "_blank",
    );
  }, []);

  // Get primary button text based on step
  const getPrimaryButtonText = () => {
    switch (step) {
      case 1:
        return intl.formatMessage({
          id: "label.button.next",
          defaultMessage: "Next",
        });
      case 2:
        return intl.formatMessage({
          id: "label.button.preview",
          defaultMessage: "Preview",
        });
      case 3:
        return intl.formatMessage({
          id: "label.button.import",
          defaultMessage: "Import",
        });
      case 4:
        return intl.formatMessage({
          id: "label.button.done",
          defaultMessage: "Done",
        });
      default:
        return intl.formatMessage({
          id: "label.button.next",
          defaultMessage: "Next",
        });
    }
  };

  // Handle primary button click
  const handlePrimaryClick = () => {
    switch (step) {
      case 2:
        handlePreview();
        break;
      case 3:
        handleImport();
        break;
      case 4:
        handleClose();
        break;
      default:
        break;
    }
  };

  // Check if primary button should be disabled
  const isPrimaryDisabled = () => {
    switch (step) {
      case 1:
        return !file;
      case 2:
        return !isRequiredMappingComplete() || isPreviewLoading;
      case 3:
        return previewErrors.length > 0 || isImporting;
      default:
        return false;
    }
  };

  // Render data points section
  const renderDataPointsSection = (title, dataPoints) => (
    <div className="data-points-group">
      <h6>{title}</h6>
      <StructuredListWrapper isCondensed>
        <StructuredListHead>
          <StructuredListRow head>
            <StructuredListCell head>Column</StructuredListCell>
            <StructuredListCell head>Description</StructuredListCell>
            <StructuredListCell head>Example</StructuredListCell>
            <StructuredListCell head>Required</StructuredListCell>
          </StructuredListRow>
        </StructuredListHead>
        <StructuredListBody>
          {dataPoints.map((field) => (
            <StructuredListRow key={field.key}>
              <StructuredListCell>
                <code className="column-key">{field.key}</code>
              </StructuredListCell>
              <StructuredListCell>{field.description}</StructuredListCell>
              <StructuredListCell>
                <em>{field.example}</em>
              </StructuredListCell>
              <StructuredListCell>
                {field.required ? (
                  <Tag type="red" size="sm">
                    Yes
                  </Tag>
                ) : (
                  <Tag type="gray" size="sm">
                    No
                  </Tag>
                )}
              </StructuredListCell>
            </StructuredListRow>
          ))}
        </StructuredListBody>
      </StructuredListWrapper>
    </div>
  );

  return (
    <Modal
      open={open}
      onRequestClose={handleClose}
      modalHeading={intl.formatMessage({
        id: "notebook.bacteriology.manifest.title",
        defaultMessage: "Import Bacteriology Samples from Manifest",
      })}
      primaryButtonText={getPrimaryButtonText()}
      secondaryButtonText={intl.formatMessage({
        id: "label.button.cancel",
        defaultMessage: "Cancel",
      })}
      onRequestSubmit={handlePrimaryClick}
      onSecondarySubmit={handleClose}
      size="lg"
      primaryButtonDisabled={isPrimaryDisabled()}
      className="manifest-import-modal"
    >
      <div className="bacteriology-manifest-import-modal">
        {/* Step 1: Instructions & File Upload */}
        {step === 1 && (
          <div className="import-step">
            <div className="step-header">
              <h5>
                <FormattedMessage
                  id="notebook.bacteriology.manifest.step1.title"
                  defaultMessage="Step 1: Upload Manifest CSV"
                />
              </h5>
              <p className="step-description">
                <FormattedMessage
                  id="notebook.bacteriology.manifest.step1.description"
                  defaultMessage="Upload a CSV file containing sample data. The system will auto-detect column mappings based on header names."
                />
              </p>
            </div>

            {/* Download Template Button */}
            <div className="template-download-section">
              <Button
                kind="tertiary"
                size="sm"
                renderIcon={Download}
                onClick={handleDownloadTemplate}
              >
                <FormattedMessage
                  id="notebook.bacteriology.manifest.downloadTemplate"
                  defaultMessage="Download CSV Template"
                />
              </Button>
            </div>

            {/* Expected Data Points Documentation */}
            <Accordion>
              <AccordionItem
                title={intl.formatMessage({
                  id: "notebook.bacteriology.manifest.dataPointsTitle",
                  defaultMessage: "Expected Data Points (Click to expand)",
                })}
              >
                <div className="data-points-section">
                  {renderDataPointsSection(
                    "Project Information",
                    EXPECTED_DATA_POINTS.projectInfo,
                  )}
                  {renderDataPointsSection(
                    "Sample Identity",
                    EXPECTED_DATA_POINTS.sampleIdentity,
                  )}
                  {renderDataPointsSection(
                    "Collection Metadata",
                    EXPECTED_DATA_POINTS.collectionMetadata,
                  )}
                  {renderDataPointsSection(
                    "Reception Metadata",
                    EXPECTED_DATA_POINTS.receptionMetadata,
                  )}
                  {renderDataPointsSection(
                    "Storage Conditions",
                    EXPECTED_DATA_POINTS.storageConditions,
                  )}
                  {renderDataPointsSection(
                    "Compliance Status (Clinical Trials)",
                    EXPECTED_DATA_POINTS.complianceStatus,
                  )}
                  {renderDataPointsSection(
                    "Sample Origin Tracking",
                    EXPECTED_DATA_POINTS.sampleOriginTracking,
                  )}

                  {/* Valid Sample Types */}
                  <div className="data-points-group">
                    <h6>Valid Sample Types</h6>
                    <div className="valid-values-list">
                      {isSampleTypesLoading ? (
                        <Loading
                          small
                          withOverlay={false}
                          description="Loading sample types..."
                        />
                      ) : validSampleTypes.length > 0 ? (
                        validSampleTypes.map((value, idx) => (
                          <Tag key={idx} type="outline" size="sm">
                            {value}
                          </Tag>
                        ))
                      ) : (
                        <span className="no-sample-types">
                          <FormattedMessage
                            id="notebook.bacteriology.manifest.noSampleTypes"
                            defaultMessage="No sample types configured. Contact administrator."
                          />
                        </span>
                      )}
                    </div>
                  </div>
                </div>
              </AccordionItem>
            </Accordion>

            {/* File Upload */}
            <div className="file-upload-section">
              <FileUploaderDropContainer
                accept={[".csv"]}
                labelText={intl.formatMessage({
                  id: "notebook.bacteriology.manifest.dropzone",
                  defaultMessage:
                    "Drag and drop a CSV file here or click to upload",
                })}
                onAddFiles={handleFileAdded}
              />
              {fileError && (
                <InlineNotification
                  kind="error"
                  title={fileError}
                  lowContrast
                  hideCloseButton
                />
              )}
            </div>
          </div>
        )}

        {/* Step 2: Column Mapping */}
        {step === 2 && (
          <div className="import-step">
            <div className="step-header">
              <h5>
                <FormattedMessage
                  id="notebook.bacteriology.manifest.step2.title"
                  defaultMessage="Step 2: Map Columns"
                />
              </h5>
              <p className="step-description">
                <FormattedMessage
                  id="notebook.bacteriology.manifest.step2.description"
                  defaultMessage="Map the CSV columns to the required fields. Columns have been auto-detected where possible."
                />
              </p>
            </div>

            {file && (
              <FileUploaderItem
                name={file.name}
                status="edit"
                onDelete={handleRemoveFile}
              />
            )}

            {/* Required Fields Mapping */}
            <div className="mapping-section">
              <h6 className="mapping-section-title">
                <Tag type="red" size="sm">
                  Required
                </Tag>
                Required Fields
              </h6>
              <Grid fullWidth>
                <Column lg={8} md={4} sm={4}>
                  <Select
                    id="barcodeColumn"
                    labelText="Barcode *"
                    value={columnMapping.barcodeColumn}
                    onChange={(e) =>
                      handleMappingChange("barcodeColumn", e.target.value)
                    }
                  >
                    <SelectItem value="" text="Select column..." />
                    {csvHeaders.map((header) => (
                      <SelectItem key={header} value={header} text={header} />
                    ))}
                  </Select>
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <Select
                    id="sampleTypeColumn"
                    labelText="Sample Type *"
                    value={columnMapping.sampleTypeColumn}
                    onChange={(e) =>
                      handleMappingChange("sampleTypeColumn", e.target.value)
                    }
                  >
                    <SelectItem value="" text="Select column..." />
                    {csvHeaders.map((header) => (
                      <SelectItem key={header} value={header} text={header} />
                    ))}
                  </Select>
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <Select
                    id="sampleOriginColumn"
                    labelText="Sample Origin *"
                    value={columnMapping.sampleOriginColumn}
                    onChange={(e) =>
                      handleMappingChange("sampleOriginColumn", e.target.value)
                    }
                  >
                    <SelectItem value="" text="Select column..." />
                    {csvHeaders.map((header) => (
                      <SelectItem key={header} value={header} text={header} />
                    ))}
                  </Select>
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <Select
                    id="sourceLocationFacilityColumn"
                    labelText="Source Location/Facility *"
                    value={columnMapping.sourceLocationFacilityColumn}
                    onChange={(e) =>
                      handleMappingChange(
                        "sourceLocationFacilityColumn",
                        e.target.value,
                      )
                    }
                  >
                    <SelectItem value="" text="Select column..." />
                    {csvHeaders.map((header) => (
                      <SelectItem key={header} value={header} text={header} />
                    ))}
                  </Select>
                </Column>
              </Grid>
            </div>

            {/* Optional Fields Mapping */}
            <Accordion>
              <AccordionItem title="Optional Fields (Click to expand)">
                <Grid fullWidth>
                  <Column lg={8} md={4} sm={4}>
                    <Select
                      id="projectNameColumn"
                      labelText="Project Name"
                      value={columnMapping.projectNameColumn}
                      onChange={(e) =>
                        handleMappingChange("projectNameColumn", e.target.value)
                      }
                    >
                      <SelectItem value="" text="Select column..." />
                      {csvHeaders.map((header) => (
                        <SelectItem key={header} value={header} text={header} />
                      ))}
                    </Select>
                  </Column>
                  <Column lg={8} md={4} sm={4}>
                    <Select
                      id="studyIdColumn"
                      labelText="Study ID"
                      value={columnMapping.studyIdColumn}
                      onChange={(e) =>
                        handleMappingChange("studyIdColumn", e.target.value)
                      }
                    >
                      <SelectItem value="" text="Select column..." />
                      {csvHeaders.map((header) => (
                        <SelectItem key={header} value={header} text={header} />
                      ))}
                    </Select>
                  </Column>
                  <Column lg={8} md={4} sm={4}>
                    <Select
                      id="participantIdColumn"
                      labelText="Participant ID"
                      value={columnMapping.participantIdColumn}
                      onChange={(e) =>
                        handleMappingChange(
                          "participantIdColumn",
                          e.target.value,
                        )
                      }
                    >
                      <SelectItem value="" text="Select column..." />
                      {csvHeaders.map((header) => (
                        <SelectItem key={header} value={header} text={header} />
                      ))}
                    </Select>
                  </Column>
                  <Column lg={8} md={4} sm={4}>
                    <Select
                      id="collectionSiteColumn"
                      labelText="Collection Site"
                      value={columnMapping.collectionSiteColumn}
                      onChange={(e) =>
                        handleMappingChange(
                          "collectionSiteColumn",
                          e.target.value,
                        )
                      }
                    >
                      <SelectItem value="" text="Select column..." />
                      {csvHeaders.map((header) => (
                        <SelectItem key={header} value={header} text={header} />
                      ))}
                    </Select>
                  </Column>
                  <Column lg={8} md={4} sm={4}>
                    <Select
                      id="collectionDateTimeColumn"
                      labelText="Collection Date & Time"
                      value={columnMapping.collectionDateTimeColumn}
                      onChange={(e) =>
                        handleMappingChange(
                          "collectionDateTimeColumn",
                          e.target.value,
                        )
                      }
                    >
                      <SelectItem value="" text="Select column..." />
                      {csvHeaders.map((header) => (
                        <SelectItem key={header} value={header} text={header} />
                      ))}
                    </Select>
                  </Column>
                  <Column lg={8} md={4} sm={4}>
                    <Select
                      id="sampleReceivedDateColumn"
                      labelText="Sample Received Date"
                      value={columnMapping.sampleReceivedDateColumn}
                      onChange={(e) =>
                        handleMappingChange(
                          "sampleReceivedDateColumn",
                          e.target.value,
                        )
                      }
                    >
                      <SelectItem value="" text="Select column..." />
                      {csvHeaders.map((header) => (
                        <SelectItem key={header} value={header} text={header} />
                      ))}
                    </Select>
                  </Column>
                  <Column lg={8} md={4} sm={4}>
                    <Select
                      id="sampleArrivalTimeColumn"
                      labelText="Sample Arrival Time"
                      value={columnMapping.sampleArrivalTimeColumn}
                      onChange={(e) =>
                        handleMappingChange(
                          "sampleArrivalTimeColumn",
                          e.target.value,
                        )
                      }
                    >
                      <SelectItem value="" text="Select column..." />
                      {csvHeaders.map((header) => (
                        <SelectItem key={header} value={header} text={header} />
                      ))}
                    </Select>
                  </Column>
                  <Column lg={8} md={4} sm={4}>
                    <Select
                      id="receivedByColumn"
                      labelText="Received By"
                      value={columnMapping.receivedByColumn}
                      onChange={(e) =>
                        handleMappingChange("receivedByColumn", e.target.value)
                      }
                    >
                      <SelectItem value="" text="Select column..." />
                      {csvHeaders.map((header) => (
                        <SelectItem key={header} value={header} text={header} />
                      ))}
                    </Select>
                  </Column>
                  <Column lg={8} md={4} sm={4}>
                    <Select
                      id="storageContainerTypeColumn"
                      labelText="Storage Container Type"
                      value={columnMapping.storageContainerTypeColumn}
                      onChange={(e) =>
                        handleMappingChange(
                          "storageContainerTypeColumn",
                          e.target.value,
                        )
                      }
                    >
                      <SelectItem value="" text="Select column..." />
                      {csvHeaders.map((header) => (
                        <SelectItem key={header} value={header} text={header} />
                      ))}
                    </Select>
                  </Column>
                  <Column lg={8} md={4} sm={4}>
                    <Select
                      id="storageTemperatureOnArrivalColumn"
                      labelText="Storage Temperature on Arrival"
                      value={columnMapping.storageTemperatureOnArrivalColumn}
                      onChange={(e) =>
                        handleMappingChange(
                          "storageTemperatureOnArrivalColumn",
                          e.target.value,
                        )
                      }
                    >
                      <SelectItem value="" text="Select column..." />
                      {csvHeaders.map((header) => (
                        <SelectItem key={header} value={header} text={header} />
                      ))}
                    </Select>
                  </Column>
                  <Column lg={8} md={4} sm={4}>
                    <Select
                      id="consentStatusColumn"
                      labelText="Consent Status"
                      value={columnMapping.consentStatusColumn}
                      onChange={(e) =>
                        handleMappingChange(
                          "consentStatusColumn",
                          e.target.value,
                        )
                      }
                    >
                      <SelectItem value="" text="Select column..." />
                      {csvHeaders.map((header) => (
                        <SelectItem key={header} value={header} text={header} />
                      ))}
                    </Select>
                  </Column>
                  <Column lg={8} md={4} sm={4}>
                    <Select
                      id="crfStatusColumn"
                      labelText="CRF Status"
                      value={columnMapping.crfStatusColumn}
                      onChange={(e) =>
                        handleMappingChange("crfStatusColumn", e.target.value)
                      }
                    >
                      <SelectItem value="" text="Select column..." />
                      {csvHeaders.map((header) => (
                        <SelectItem key={header} value={header} text={header} />
                      ))}
                    </Select>
                  </Column>
                </Grid>
              </AccordionItem>
            </Accordion>

            {!isRequiredMappingComplete() && (
              <InlineNotification
                kind="warning"
                title={intl.formatMessage({
                  id: "notebook.bacteriology.manifest.mappingIncomplete",
                  defaultMessage:
                    "Please map all required fields before proceeding.",
                })}
                lowContrast
                hideCloseButton
              />
            )}
          </div>
        )}

        {/* Step 3: Preview */}
        {step === 3 && (
          <div className="import-step">
            <div className="step-header">
              <h5>
                <FormattedMessage
                  id="notebook.bacteriology.manifest.step3.title"
                  defaultMessage="Step 3: Preview Import"
                />
              </h5>
            </div>

            {isPreviewLoading ? (
              <Loading description="Loading preview..." />
            ) : (
              <>
                {previewData && (
                  <div className="preview-section">
                    <div className="preview-stats">
                      <Tag type="blue">Total Rows: {previewData.totalRows}</Tag>
                      <Tag type="green">Valid: {previewData.validRows}</Tag>
                      {previewData.totalRows - previewData.validRows > 0 && (
                        <Tag type="red">
                          Invalid:{" "}
                          {previewData.totalRows - previewData.validRows}
                        </Tag>
                      )}
                    </div>

                    {previewErrors.length > 0 && (
                      <div className="validation-errors-section">
                        <InlineNotification
                          kind="error"
                          title="Validation Errors"
                          subtitle={`${previewErrors.length} error(s) found. Please fix the CSV and try again.`}
                          lowContrast
                          hideCloseButton
                        />
                        <div className="error-list">
                          {previewErrors.slice(0, 10).map((error, idx) => (
                            <div key={idx} className="error-item">
                              <strong>Row {error.rowNumber}:</strong>{" "}
                              {error.column} - {error.message}
                            </div>
                          ))}
                          {previewErrors.length > 10 && (
                            <div className="error-item">
                              ... and {previewErrors.length - 10} more errors
                            </div>
                          )}
                        </div>
                      </div>
                    )}

                    {previewData.rows && previewData.rows.length > 0 && (
                      <div className="preview-table-section">
                        <h6>Preview (first 10 rows)</h6>
                        <DataTable
                          rows={previewData.rows
                            .slice(0, 10)
                            .map((row, idx) => ({
                              id: String(idx),
                              rowNumber: row.rowNumber,
                              barcode: row.barcode || "-",
                              sampleType: row.sampleType || "-",
                              sampleOrigin: row.sampleOrigin || "-",
                              projectName: row.projectName || "-",
                              collectionSite: row.collectionSite || "-",
                            }))}
                          headers={[
                            { key: "rowNumber", header: "Row #" },
                            { key: "barcode", header: "Barcode" },
                            { key: "sampleType", header: "Sample Type" },
                            { key: "sampleOrigin", header: "Origin" },
                            { key: "projectName", header: "Project" },
                            { key: "collectionSite", header: "Site" },
                          ]}
                        >
                          {({
                            rows,
                            headers,
                            getTableProps,
                            getHeaderProps,
                            getRowProps,
                          }) => (
                            <Table {...getTableProps()} size="sm">
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
                                {rows.map((row) => (
                                  <TableRow
                                    key={row.id}
                                    {...getRowProps({ row })}
                                  >
                                    {row.cells.map((cell) => (
                                      <TableCell key={cell.id}>
                                        {cell.value}
                                      </TableCell>
                                    ))}
                                  </TableRow>
                                ))}
                              </TableBody>
                            </Table>
                          )}
                        </DataTable>
                      </div>
                    )}
                  </div>
                )}
              </>
            )}

            {isImporting && <Loading description="Importing samples..." />}
          </div>
        )}

        {/* Step 4: Success */}
        {step === 4 && (
          <div className="import-step">
            <InlineNotification
              kind="success"
              title={intl.formatMessage({
                id: "notebook.bacteriology.manifest.importSuccess",
                defaultMessage: "Import Successful",
              })}
              subtitle={intl.formatMessage({
                id: "notebook.bacteriology.manifest.importSuccessMessage",
                defaultMessage:
                  "Samples have been imported successfully. You can now view them in the sample list.",
              })}
              lowContrast
              hideCloseButton
            />
          </div>
        )}
      </div>
    </Modal>
  );
}

export default BacteriologyManifestImportModal;
