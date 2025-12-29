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
} from "@carbon/react";
import { Upload, Checkmark, Warning, Information } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import config from "../../../config.json";
import "./NotebookWorkflow.css";

/**
 * Expected dataPoints for TB Sample Accession & Registration Manifest.
 * Aligned with the TB workflow SRS requirements (FR-014).
 *
 * Sections:
 * A. Sample Identity
 * B. Specimen Information
 * C. Request Paper Details
 * D. Patient / Participant Metadata
 * E. Clinical Context
 * F. Requested Tests
 * G. Receipt Details
 */
const EXPECTED_DATA_POINTS = {
  // A. Sample Identity + B. Specimen Information (Required)
  required: [
    {
      key: "sampleId",
      label: "Sample ID",
      description: "Unique sample identifier from the request form",
      example: "TB-2024-001",
    },
    {
      key: "specimenType",
      label: "Specimen Type",
      description: "Type of TB specimen (validated against TB lab types)",
      example: "Sputum, BAL, Pleural Fluid",
    },
    {
      key: "numOfSamples",
      label: "Number of Samples",
      description: "Number of sample aliquots/tubes for this specimen",
      example: "2",
    },
  ],
  // B. Specimen Type (Validated)
  specimenType: {
    key: "specimenType",
    label: "Specimen Type",
    description:
      "Type of TB specimen (validated against configured TB lab types)",
    example: "Sputum, BAL, Pleural Fluid, CSF, Tissue",
  },
  // C. Request Paper Details + D. Patient Metadata + E. Clinical Context + F. Requested Tests + G. Receipt Details
  optional: [
    // B. Specimen Information (continued)
    {
      key: "specimenQuality",
      label: "Specimen Quality",
      description: "Quality assessment of the specimen",
      example: "Good, Fair, Poor",
    },
    // C. Request Paper Details
    {
      key: "documentNumber",
      label: "Document Number",
      description: "Reference number from the request form",
      example: "REQ-2024-001",
    },
    {
      key: "referringFacility",
      label: "Referring Facility",
      description: "Health facility that sent the sample",
      example: "Central Hospital TB Clinic",
    },
    // D. Patient Metadata
    {
      key: "patientName",
      label: "Patient Name",
      description: "Full name of the patient",
      example: "John Doe",
    },
    {
      key: "patientAge",
      label: "Patient Age",
      description: "Age of the patient (years)",
      example: "45",
    },
    {
      key: "patientSex",
      label: "Patient Sex",
      description: "Sex of the patient (M/F)",
      example: "M",
    },
    {
      key: "patientId",
      label: "Patient ID",
      description: "Patient identifier from health records",
      example: "PAT-12345",
    },
    {
      key: "studyId",
      label: "Study ID",
      description: "Study/research participant ID (if applicable)",
      example: "STUDY-001",
    },
    {
      key: "patientAddress",
      label: "Patient Address",
      description: "Patient residential address",
      example: "123 Main Street",
    },
    {
      key: "patientPhone",
      label: "Patient Phone",
      description: "Patient contact number",
      example: "+251-911-123456",
    },
    {
      key: "physicianPhone",
      label: "Physician Phone",
      description: "Requesting physician contact",
      example: "+251-911-654321",
    },
    {
      key: "consentStatus",
      label: "Consent Status",
      description: "Patient consent for testing (Yes/No)",
      example: "Yes",
    },
    // E. Clinical Context
    {
      key: "treatmentHistory",
      label: "Treatment History",
      description: "TB treatment status (New, Retreatment, MDR-TB)",
      example: "New",
    },
    // F. Requested Tests
    {
      key: "culture",
      label: "Culture Requested",
      description: "Whether culture test is requested (Yes/No)",
      example: "Yes",
    },
    {
      key: "smearMicroscopy",
      label: "Smear Microscopy Requested",
      description: "Whether AFB smear microscopy is requested (Yes/No)",
      example: "Yes",
    },
    {
      key: "genexpert",
      label: "GeneXpert Requested",
      description: "Whether GeneXpert/MTB-RIF is requested (Yes/No)",
      example: "No",
    },
    {
      key: "identification",
      label: "Identification Requested",
      description: "Whether species identification is requested (Yes/No)",
      example: "Yes",
    },
    {
      key: "dstFirstLine",
      label: "DST First-Line Requested",
      description: "Whether first-line DST is requested (Yes/No)",
      example: "Yes",
    },
    {
      key: "dstSecondLine",
      label: "DST Second-Line Requested",
      description: "Whether second-line DST is requested (Yes/No)",
      example: "No",
    },
    {
      key: "intendedMethod",
      label: "Intended Method",
      description: "Preferred culture method (LJ, MGIT, Both)",
      example: "MGIT",
    },
    // G. Receipt Details
    {
      key: "receivedSite",
      label: "Received Site",
      description: "Laboratory site receiving the sample",
      example: "National TB Reference Lab",
    },
    {
      key: "receivedDate",
      label: "Received Date",
      description: "Date sample was received (YYYY-MM-DD)",
      example: "2024-01-15",
    },
    {
      key: "receivedTime",
      label: "Received Time",
      description: "Time sample was received (HH:MM)",
      example: "09:30",
    },
  ],
  autoGenerated: [
    {
      key: "accessionNumber",
      label: "Accession Number",
      description:
        "System-generated laboratory accession number (NNN/YY format)",
    },
    {
      key: "tbSampleId",
      label: "TB Sample ID",
      description: "System-generated TB-specific sample identifier",
    },
  ],
};

/**
 * TBManifestImportModal - CSV import modal for TB workflow.
 * Supports mapping TB-specific sample accession metadata columns.
 * Cloned from ImmunologyManifestImportModal with TB-specific fields per SRS.
 */
function TBManifestImportModal({ open, onClose, entryId, onImportSuccess }) {
  const intl = useIntl();

  const [file, setFile] = useState(null);
  const [fileError, setFileError] = useState(null);
  const [csvHeaders, setCsvHeaders] = useState([]);
  const [columnMapping, setColumnMapping] = useState({
    // A. Sample Identity
    sampleIdColumn: "",
    // B. Specimen Information
    specimenTypeColumn: "",
    specimenQualityColumn: "",
    // C. Request Paper Details
    documentNumberColumn: "",
    referringFacilityColumn: "",
    // D. Patient Metadata
    patientNameColumn: "",
    patientAgeColumn: "",
    patientSexColumn: "",
    patientIdColumn: "",
    studyIdColumn: "",
    patientAddressColumn: "",
    patientPhoneColumn: "",
    physicianPhoneColumn: "",
    consentStatusColumn: "",
    // E. Clinical Context
    treatmentHistoryColumn: "",
    // F. Requested Tests
    cultureColumn: "",
    smearMicroscopyColumn: "",
    genexpertColumn: "",
    identificationColumn: "",
    dstFirstLineColumn: "",
    dstSecondLineColumn: "",
    intendedMethodColumn: "",
    // G. Receipt Details
    receivedSiteColumn: "",
    receivedDateColumn: "",
    receivedTimeColumn: "",
    // Common
    numOfSamplesColumn: "",
    dateFormat: "yyyy-MM-dd",
  });

  const [step, setStep] = useState(1);
  const [previewData, setPreviewData] = useState(null);
  const [previewErrors, setPreviewErrors] = useState([]);
  const [isPreviewLoading, setIsPreviewLoading] = useState(false);
  const [isImporting, setIsImporting] = useState(false);

  // Dynamic specimen types from backend
  const [validSpecimenTypes, setValidSpecimenTypes] = useState([]);
  const [isSpecimenTypesLoading, setIsSpecimenTypesLoading] = useState(false);

  // Fetch valid specimen types from backend when modal opens
  useEffect(() => {
    if (open && entryId) {
      setIsSpecimenTypesLoading(true);
      fetch(
        `${config.serverBaseUrl}/rest/notebook/tb/entry/${entryId}/specimen-types`,
        {
          method: "GET",
          credentials: "include",
          headers: {
            "X-CSRF-Token": localStorage.getItem("CSRF"),
          },
        },
      )
        .then((response) => response.json())
        .then((data) => {
          if (data.specimenTypes) {
            setValidSpecimenTypes(
              data.specimenTypes.map((st) => st.description || st),
            );
          }
        })
        .catch((error) => {
          console.error("Failed to fetch specimen types:", error);
          // Fallback to empty array - validation will still happen on backend
          setValidSpecimenTypes([]);
        })
        .finally(() => {
          setIsSpecimenTypesLoading(false);
        });
    }
  }, [open, entryId]);

  /**
   * Auto-detect and map CSV columns based on header names.
   * Matches against expected column keys (case-insensitive).
   */
  const autoMapColumns = useCallback((headers) => {
    // Map of expected column keys to their form field names
    const columnKeyToField = {
      // Sample Identity
      sampleid: "sampleIdColumn",
      "sample id": "sampleIdColumn",
      tbsampleid: "sampleIdColumn",
      // Specimen Information
      specimentype: "specimenTypeColumn",
      "specimen type": "specimenTypeColumn",
      sampletype: "specimenTypeColumn",
      type: "specimenTypeColumn",
      specimenquality: "specimenQualityColumn",
      "specimen quality": "specimenQualityColumn",
      quality: "specimenQualityColumn",
      // Request Paper Details
      documentnumber: "documentNumberColumn",
      "document number": "documentNumberColumn",
      docnumber: "documentNumberColumn",
      requestnumber: "documentNumberColumn",
      referringfacility: "referringFacilityColumn",
      "referring facility": "referringFacilityColumn",
      facility: "referringFacilityColumn",
      source: "referringFacilityColumn",
      // Patient Metadata
      patientname: "patientNameColumn",
      "patient name": "patientNameColumn",
      name: "patientNameColumn",
      patientage: "patientAgeColumn",
      "patient age": "patientAgeColumn",
      age: "patientAgeColumn",
      patientsex: "patientSexColumn",
      "patient sex": "patientSexColumn",
      sex: "patientSexColumn",
      gender: "patientSexColumn",
      patientid: "patientIdColumn",
      "patient id": "patientIdColumn",
      studyid: "studyIdColumn",
      "study id": "studyIdColumn",
      patientaddress: "patientAddressColumn",
      "patient address": "patientAddressColumn",
      address: "patientAddressColumn",
      patientphone: "patientPhoneColumn",
      "patient phone": "patientPhoneColumn",
      phone: "patientPhoneColumn",
      physicianphone: "physicianPhoneColumn",
      "physician phone": "physicianPhoneColumn",
      doctorphone: "physicianPhoneColumn",
      consentstatus: "consentStatusColumn",
      "consent status": "consentStatusColumn",
      consent: "consentStatusColumn",
      // Clinical Context
      treatmenthistory: "treatmentHistoryColumn",
      "treatment history": "treatmentHistoryColumn",
      treatment: "treatmentHistoryColumn",
      // Requested Tests
      culture: "cultureColumn",
      smearmicroscopy: "smearMicroscopyColumn",
      "smear microscopy": "smearMicroscopyColumn",
      smear: "smearMicroscopyColumn",
      afb: "smearMicroscopyColumn",
      genexpert: "genexpertColumn",
      xpert: "genexpertColumn",
      mtbrif: "genexpertColumn",
      identification: "identificationColumn",
      id: "identificationColumn",
      species: "identificationColumn",
      dstfirstline: "dstFirstLineColumn",
      "dst first line": "dstFirstLineColumn",
      dst1: "dstFirstLineColumn",
      dstsecondline: "dstSecondLineColumn",
      "dst second line": "dstSecondLineColumn",
      dst2: "dstSecondLineColumn",
      intendedmethod: "intendedMethodColumn",
      "intended method": "intendedMethodColumn",
      method: "intendedMethodColumn",
      // Receipt Details
      receivedsite: "receivedSiteColumn",
      "received site": "receivedSiteColumn",
      lab: "receivedSiteColumn",
      receiveddate: "receivedDateColumn",
      "received date": "receivedDateColumn",
      receptiondate: "receivedDateColumn",
      receivedtime: "receivedTimeColumn",
      "received time": "receivedTimeColumn",
      // Common
      numofsamples: "numOfSamplesColumn",
      "num of samples": "numOfSamplesColumn",
      "number of samples": "numOfSamplesColumn",
      quantity: "numOfSamplesColumn",
      tubes: "numOfSamplesColumn",
    };

    const newMapping = {
      sampleIdColumn: "",
      specimenTypeColumn: "",
      specimenQualityColumn: "",
      documentNumberColumn: "",
      referringFacilityColumn: "",
      patientNameColumn: "",
      patientAgeColumn: "",
      patientSexColumn: "",
      patientIdColumn: "",
      studyIdColumn: "",
      patientAddressColumn: "",
      patientPhoneColumn: "",
      physicianPhoneColumn: "",
      consentStatusColumn: "",
      treatmentHistoryColumn: "",
      cultureColumn: "",
      smearMicroscopyColumn: "",
      genexpertColumn: "",
      identificationColumn: "",
      dstFirstLineColumn: "",
      dstSecondLineColumn: "",
      intendedMethodColumn: "",
      receivedSiteColumn: "",
      receivedDateColumn: "",
      receivedTimeColumn: "",
      numOfSamplesColumn: "",
      dateFormat: "yyyy-MM-dd",
    };

    headers.forEach((header) => {
      // Normalize header: lowercase, remove special chars
      const normalizedHeader = header
        .toLowerCase()
        .replace(/[\s_\-/\\]/g, "")
        .trim();

      const fieldName = columnKeyToField[normalizedHeader];
      if (fieldName) {
        newMapping[fieldName] = header;
      }
    });

    return newMapping;
  }, []);

  const handleFileAdded = useCallback(
    (event, { addedFiles }) => {
      const addedFile = addedFiles[0];
      if (!addedFile) return;

      if (!addedFile.name.endsWith(".csv")) {
        setFileError(
          intl.formatMessage({
            id: "notebook.tb.manifest.error.invalidFileType",
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

        // Auto-map columns based on header names
        const autoMappedColumns = autoMapColumns(headers);
        setColumnMapping(autoMappedColumns);

        setStep(2);
      };
      reader.readAsText(addedFile);
    },
    [intl, autoMapColumns],
  );

  const handleRemoveFile = () => {
    setFile(null);
    setCsvHeaders([]);
    setColumnMapping({
      sampleIdColumn: "",
      specimenTypeColumn: "",
      specimenQualityColumn: "",
      documentNumberColumn: "",
      referringFacilityColumn: "",
      patientNameColumn: "",
      patientAgeColumn: "",
      patientSexColumn: "",
      patientIdColumn: "",
      studyIdColumn: "",
      patientAddressColumn: "",
      patientPhoneColumn: "",
      physicianPhoneColumn: "",
      consentStatusColumn: "",
      treatmentHistoryColumn: "",
      cultureColumn: "",
      smearMicroscopyColumn: "",
      genexpertColumn: "",
      identificationColumn: "",
      dstFirstLineColumn: "",
      dstSecondLineColumn: "",
      intendedMethodColumn: "",
      receivedSiteColumn: "",
      receivedDateColumn: "",
      receivedTimeColumn: "",
      numOfSamplesColumn: "",
      dateFormat: "yyyy-MM-dd",
    });
    setPreviewData(null);
    setPreviewErrors([]);
    setStep(1);
  };

  const handleMappingChange = (field, value) => {
    setColumnMapping((prev) => ({ ...prev, [field]: value }));
  };

  // Required fields: specimenType and numOfSamples
  const requiredColumnsMapped =
    columnMapping.specimenTypeColumn && columnMapping.numOfSamplesColumn;

  const buildFormData = () => {
    const formData = new FormData();
    formData.append("file", file);
    formData.append(
      "mapping",
      new Blob([JSON.stringify(columnMapping)], { type: "application/json" }),
    );
    return formData;
  };

  const handlePreview = async () => {
    if (!file || !entryId) return;
    setIsPreviewLoading(true);
    setPreviewErrors([]);

    try {
      const endpoint = `${config.serverBaseUrl}/rest/notebook/tb/entry/${entryId}/samples/preview-manifest`;
      const response = await fetch(endpoint, {
        method: "POST",
        body: buildFormData(),
        credentials: "include",
        headers: {
          "X-CSRF-Token": localStorage.getItem("CSRF"),
        },
      });

      const data = await response.json();
      if (response.ok) {
        setPreviewData(data);
        setPreviewErrors(data.errors || []);
        setStep(3);
      } else {
        setPreviewErrors(
          data.errors || [
            {
              rowNumber: 0,
              column: "file",
              message: data.error || "Preview failed",
            },
          ],
        );
      }
    } catch (error) {
      setPreviewErrors([
        { rowNumber: 0, column: "file", message: error.message },
      ]);
    } finally {
      setIsPreviewLoading(false);
    }
  };

  const handleImport = async () => {
    if (!file || !entryId) return;
    setIsImporting(true);
    setPreviewErrors([]);

    try {
      const endpoint = `${config.serverBaseUrl}/rest/notebook/tb/entry/${entryId}/samples/create-from-manifest`;
      const response = await fetch(endpoint, {
        method: "POST",
        body: buildFormData(),
        credentials: "include",
        headers: {
          "X-CSRF-Token": localStorage.getItem("CSRF"),
        },
      });

      const data = await response.json();
      if (response.ok && data.success) {
        setStep(4);
        if (onImportSuccess) {
          onImportSuccess(data);
        }
      } else {
        setPreviewErrors(
          data.errors || [
            {
              rowNumber: 0,
              column: "import",
              message: data.error || "Import failed",
            },
          ],
        );
      }
    } catch (error) {
      setPreviewErrors([
        { rowNumber: 0, column: "import", message: error.message },
      ]);
    } finally {
      setIsImporting(false);
    }
  };

  const handleClose = () => {
    handleRemoveFile();
    onClose();
  };

  const mappingField = (field, labelId) => (
    <Select
      id={`mapping-${field}`}
      labelText={intl.formatMessage({ id: labelId })}
      value={columnMapping[field]}
      onChange={(e) => handleMappingChange(field, e.target.value)}
    >
      <SelectItem value="" text={intl.formatMessage({ id: "label.select" })} />
      {csvHeaders.map((header) => (
        <SelectItem key={header} value={header} text={header} />
      ))}
    </Select>
  );

  return (
    <Modal
      open={open}
      onRequestClose={handleClose}
      modalHeading={intl.formatMessage({
        id: "notebook.tb.manifest.title",
        defaultMessage: "Import TB Samples from Delivery Manifest",
      })}
      primaryButtonText={
        step === 1
          ? intl.formatMessage({ id: "label.button.next" })
          : step === 2
            ? intl.formatMessage({ id: "notebook.manifest.preview" })
            : step === 3
              ? intl.formatMessage({ id: "notebook.manifest.import" })
              : intl.formatMessage({ id: "label.button.close" })
      }
      secondaryButtonText={
        step > 1 && step < 4
          ? intl.formatMessage({ id: "label.button.back" })
          : null
      }
      onRequestSubmit={() => {
        if (step === 1 && file) setStep(2);
        else if (step === 2) handlePreview();
        else if (step === 3) handleImport();
        else handleClose();
      }}
      onSecondarySubmit={() => setStep(step - 1)}
      primaryButtonDisabled={
        (step === 1 && !file) ||
        (step === 2 && !requiredColumnsMapped) ||
        (step === 3 && previewErrors.length > 0) ||
        isPreviewLoading ||
        isImporting
      }
      size="lg"
    >
      <div className="manifest-import-modal">
        {step === 1 && (
          <div className="upload-step">
            <p className="step-description">
              <FormattedMessage
                id="notebook.tb.manifest.step1"
                defaultMessage="Upload a CSV file with TB sample accession metadata. Review the expected data points below before uploading."
              />
            </p>

            {/* Expected Data Points Section */}
            <Accordion className="expected-datapoints-accordion">
              <AccordionItem
                title={
                  <span className="accordion-title">
                    <Information size={16} />
                    <FormattedMessage
                      id="notebook.tb.manifest.expectedColumns"
                      defaultMessage="Expected CSV Columns & Data Points"
                    />
                  </span>
                }
                open={!file}
              >
                <div className="datapoints-container">
                  {/* Required Fields */}
                  <div className="datapoints-section">
                    <h5 className="section-title required-title">
                      <FormattedMessage
                        id="notebook.tb.manifest.requiredFields"
                        defaultMessage="Required Fields"
                      />
                      <Tag type="red" size="sm">
                        {EXPECTED_DATA_POINTS.required.length}
                      </Tag>
                    </h5>
                    <StructuredListWrapper isCondensed>
                      <StructuredListHead>
                        <StructuredListRow head>
                          <StructuredListCell head>
                            <FormattedMessage
                              id="label.column"
                              defaultMessage="Column"
                            />
                          </StructuredListCell>
                          <StructuredListCell head>
                            <FormattedMessage
                              id="label.description"
                              defaultMessage="Description"
                            />
                          </StructuredListCell>
                          <StructuredListCell head>
                            <FormattedMessage
                              id="label.example"
                              defaultMessage="Example"
                            />
                          </StructuredListCell>
                        </StructuredListRow>
                      </StructuredListHead>
                      <StructuredListBody>
                        {EXPECTED_DATA_POINTS.required.map((field) => (
                          <StructuredListRow key={field.key}>
                            <StructuredListCell>
                              <strong>{field.label}</strong>
                              <br />
                              <code className="column-key">{field.key}</code>
                            </StructuredListCell>
                            <StructuredListCell>
                              {field.description}
                            </StructuredListCell>
                            <StructuredListCell>
                              <code>{field.example}</code>
                            </StructuredListCell>
                          </StructuredListRow>
                        ))}
                      </StructuredListBody>
                    </StructuredListWrapper>
                  </div>

                  {/* Specimen Type Field (Validated) */}
                  <div className="datapoints-section">
                    <h5 className="section-title sampletype-title">
                      <FormattedMessage
                        id="notebook.tb.manifest.specimenTypeField"
                        defaultMessage="Specimen Type (Validated)"
                      />
                      <Tag type="purple" size="sm">
                        1
                      </Tag>
                    </h5>
                    <StructuredListWrapper isCondensed>
                      <StructuredListHead>
                        <StructuredListRow head>
                          <StructuredListCell head>
                            <FormattedMessage
                              id="label.column"
                              defaultMessage="Column"
                            />
                          </StructuredListCell>
                          <StructuredListCell head>
                            <FormattedMessage
                              id="label.description"
                              defaultMessage="Description"
                            />
                          </StructuredListCell>
                          <StructuredListCell head>
                            <FormattedMessage
                              id="notebook.tb.manifest.validValues"
                              defaultMessage="Valid Values"
                            />
                          </StructuredListCell>
                        </StructuredListRow>
                      </StructuredListHead>
                      <StructuredListBody>
                        <StructuredListRow>
                          <StructuredListCell>
                            <strong>
                              {EXPECTED_DATA_POINTS.specimenType.label}
                            </strong>
                            <br />
                            <code className="column-key">
                              {EXPECTED_DATA_POINTS.specimenType.key}
                            </code>
                          </StructuredListCell>
                          <StructuredListCell>
                            {EXPECTED_DATA_POINTS.specimenType.description}
                          </StructuredListCell>
                          <StructuredListCell>
                            <div className="valid-values-list">
                              {isSpecimenTypesLoading ? (
                                <Loading
                                  small
                                  withOverlay={false}
                                  description="Loading specimen types..."
                                />
                              ) : validSpecimenTypes.length > 0 ? (
                                validSpecimenTypes.map((value, idx) => (
                                  <Tag key={idx} type="outline" size="sm">
                                    {value}
                                  </Tag>
                                ))
                              ) : (
                                <span className="no-sample-types">
                                  <FormattedMessage
                                    id="notebook.tb.manifest.noSpecimenTypes"
                                    defaultMessage="Specimen types will be validated on import"
                                  />
                                </span>
                              )}
                            </div>
                          </StructuredListCell>
                        </StructuredListRow>
                      </StructuredListBody>
                    </StructuredListWrapper>
                  </div>

                  {/* Optional Fields */}
                  <div className="datapoints-section">
                    <h5 className="section-title optional-title">
                      <FormattedMessage
                        id="notebook.tb.manifest.optionalFields"
                        defaultMessage="Optional Fields"
                      />
                      <Tag type="blue" size="sm">
                        {EXPECTED_DATA_POINTS.optional.length}
                      </Tag>
                    </h5>
                    <StructuredListWrapper isCondensed>
                      <StructuredListHead>
                        <StructuredListRow head>
                          <StructuredListCell head>
                            <FormattedMessage
                              id="label.column"
                              defaultMessage="Column"
                            />
                          </StructuredListCell>
                          <StructuredListCell head>
                            <FormattedMessage
                              id="label.description"
                              defaultMessage="Description"
                            />
                          </StructuredListCell>
                          <StructuredListCell head>
                            <FormattedMessage
                              id="label.example"
                              defaultMessage="Example"
                            />
                          </StructuredListCell>
                        </StructuredListRow>
                      </StructuredListHead>
                      <StructuredListBody>
                        {EXPECTED_DATA_POINTS.optional.map((field) => (
                          <StructuredListRow key={field.key}>
                            <StructuredListCell>
                              <strong>{field.label}</strong>
                              <br />
                              <code className="column-key">{field.key}</code>
                            </StructuredListCell>
                            <StructuredListCell>
                              {field.description}
                            </StructuredListCell>
                            <StructuredListCell>
                              <code>{field.example}</code>
                            </StructuredListCell>
                          </StructuredListRow>
                        ))}
                      </StructuredListBody>
                    </StructuredListWrapper>
                  </div>

                  {/* Auto-Generated Fields */}
                  <div className="datapoints-section">
                    <h5 className="section-title auto-title">
                      <FormattedMessage
                        id="notebook.tb.manifest.autoGeneratedFields"
                        defaultMessage="Auto-Generated Fields (Do not include in CSV)"
                      />
                      <Tag type="green" size="sm">
                        {EXPECTED_DATA_POINTS.autoGenerated.length}
                      </Tag>
                    </h5>
                    <StructuredListWrapper isCondensed>
                      <StructuredListHead>
                        <StructuredListRow head>
                          <StructuredListCell head>
                            <FormattedMessage
                              id="label.field"
                              defaultMessage="Field"
                            />
                          </StructuredListCell>
                          <StructuredListCell head>
                            <FormattedMessage
                              id="label.description"
                              defaultMessage="Description"
                            />
                          </StructuredListCell>
                        </StructuredListRow>
                      </StructuredListHead>
                      <StructuredListBody>
                        {EXPECTED_DATA_POINTS.autoGenerated.map((field) => (
                          <StructuredListRow key={field.key}>
                            <StructuredListCell>
                              <strong>{field.label}</strong>
                            </StructuredListCell>
                            <StructuredListCell>
                              {field.description}
                            </StructuredListCell>
                          </StructuredListRow>
                        ))}
                      </StructuredListBody>
                    </StructuredListWrapper>
                  </div>
                </div>
              </AccordionItem>
            </Accordion>

            {/* File Upload */}
            <div className="file-upload-section">
              {!file ? (
                <FileUploaderDropContainer
                  accept={[".csv"]}
                  labelText={intl.formatMessage({
                    id: "notebook.tb.manifest.dropzone",
                    defaultMessage:
                      "Drag and drop a CSV file here or click to upload",
                  })}
                  onAddFiles={handleFileAdded}
                />
              ) : (
                <FileUploaderItem
                  name={file.name}
                  status="edit"
                  onDelete={handleRemoveFile}
                />
              )}
            </div>
            {fileError && (
              <InlineNotification
                kind="error"
                title={fileError}
                lowContrast
                hideCloseButton
              />
            )}
          </div>
        )}

        {step === 2 && (
          <div className="mapping-step">
            <p className="step-description">
              <FormattedMessage
                id="notebook.tb.manifest.step2"
                defaultMessage="Map your CSV columns to the expected TB sample metadata fields. Required fields (marked with *) must be mapped."
              />
            </p>

            {/* Required Fields Section */}
            <div className="mapping-section">
              <h5 className="mapping-section-title">
                <FormattedMessage
                  id="notebook.tb.manifest.requiredMappings"
                  defaultMessage="Required Fields"
                />
                <Tag type="red" size="sm">
                  2
                </Tag>
              </h5>
              <div className="mapping-grid">
                <div className="mapping-field required">
                  {mappingField(
                    "specimenTypeColumn",
                    "notebook.tb.manifest.column.specimenType",
                  )}
                  <span className="required-marker">*</span>
                </div>
                <div className="mapping-field required">
                  {mappingField(
                    "numOfSamplesColumn",
                    "notebook.tb.manifest.column.numOfSamples",
                  )}
                  <span className="required-marker">*</span>
                </div>
              </div>
            </div>

            {/* Sample Identity Section */}
            <div className="mapping-section">
              <h5 className="mapping-section-title">
                <FormattedMessage
                  id="notebook.tb.manifest.sampleIdentity"
                  defaultMessage="A. Sample Identity"
                />
              </h5>
              <div className="mapping-grid">
                {mappingField(
                  "sampleIdColumn",
                  "notebook.tb.manifest.column.sampleId",
                )}
              </div>
            </div>

            {/* Specimen Information Section */}
            <div className="mapping-section">
              <h5 className="mapping-section-title">
                <FormattedMessage
                  id="notebook.tb.manifest.specimenInfo"
                  defaultMessage="B. Specimen Information"
                />
              </h5>
              <div className="mapping-grid">
                {mappingField(
                  "specimenQualityColumn",
                  "notebook.tb.manifest.column.specimenQuality",
                )}
              </div>
            </div>

            {/* Request Paper Details Section */}
            <div className="mapping-section">
              <h5 className="mapping-section-title">
                <FormattedMessage
                  id="notebook.tb.manifest.requestDetails"
                  defaultMessage="C. Request Paper Details"
                />
              </h5>
              <div className="mapping-grid">
                {mappingField(
                  "documentNumberColumn",
                  "notebook.tb.manifest.column.documentNumber",
                )}
                {mappingField(
                  "referringFacilityColumn",
                  "notebook.tb.manifest.column.referringFacility",
                )}
              </div>
            </div>

            {/* Patient Metadata Section */}
            <div className="mapping-section">
              <h5 className="mapping-section-title">
                <FormattedMessage
                  id="notebook.tb.manifest.patientMetadata"
                  defaultMessage="D. Patient / Participant Metadata"
                />
              </h5>
              <div className="mapping-grid">
                {mappingField(
                  "patientNameColumn",
                  "notebook.tb.manifest.column.patientName",
                )}
                {mappingField(
                  "patientAgeColumn",
                  "notebook.tb.manifest.column.patientAge",
                )}
                {mappingField(
                  "patientSexColumn",
                  "notebook.tb.manifest.column.patientSex",
                )}
                {mappingField(
                  "patientIdColumn",
                  "notebook.tb.manifest.column.patientId",
                )}
                {mappingField(
                  "studyIdColumn",
                  "notebook.tb.manifest.column.studyId",
                )}
                {mappingField(
                  "patientAddressColumn",
                  "notebook.tb.manifest.column.patientAddress",
                )}
                {mappingField(
                  "patientPhoneColumn",
                  "notebook.tb.manifest.column.patientPhone",
                )}
                {mappingField(
                  "physicianPhoneColumn",
                  "notebook.tb.manifest.column.physicianPhone",
                )}
                {mappingField(
                  "consentStatusColumn",
                  "notebook.tb.manifest.column.consentStatus",
                )}
              </div>
            </div>

            {/* Clinical Context Section */}
            <div className="mapping-section">
              <h5 className="mapping-section-title">
                <FormattedMessage
                  id="notebook.tb.manifest.clinicalContext"
                  defaultMessage="E. Clinical Context"
                />
              </h5>
              <div className="mapping-grid">
                {mappingField(
                  "treatmentHistoryColumn",
                  "notebook.tb.manifest.column.treatmentHistory",
                )}
              </div>
            </div>

            {/* Requested Tests Section */}
            <div className="mapping-section">
              <h5 className="mapping-section-title">
                <FormattedMessage
                  id="notebook.tb.manifest.requestedTests"
                  defaultMessage="F. Requested Tests"
                />
              </h5>
              <div className="mapping-grid">
                {mappingField(
                  "cultureColumn",
                  "notebook.tb.manifest.column.culture",
                )}
                {mappingField(
                  "smearMicroscopyColumn",
                  "notebook.tb.manifest.column.smearMicroscopy",
                )}
                {mappingField(
                  "genexpertColumn",
                  "notebook.tb.manifest.column.genexpert",
                )}
                {mappingField(
                  "identificationColumn",
                  "notebook.tb.manifest.column.identification",
                )}
                {mappingField(
                  "dstFirstLineColumn",
                  "notebook.tb.manifest.column.dstFirstLine",
                )}
                {mappingField(
                  "dstSecondLineColumn",
                  "notebook.tb.manifest.column.dstSecondLine",
                )}
                {mappingField(
                  "intendedMethodColumn",
                  "notebook.tb.manifest.column.intendedMethod",
                )}
              </div>
            </div>

            {/* Receipt Details Section */}
            <div className="mapping-section">
              <h5 className="mapping-section-title">
                <FormattedMessage
                  id="notebook.tb.manifest.receiptDetails"
                  defaultMessage="G. Receipt Details"
                />
              </h5>
              <div className="mapping-grid">
                {mappingField(
                  "receivedSiteColumn",
                  "notebook.tb.manifest.column.receivedSite",
                )}
                {mappingField(
                  "receivedDateColumn",
                  "notebook.tb.manifest.column.receivedDate",
                )}
                {mappingField(
                  "receivedTimeColumn",
                  "notebook.tb.manifest.column.receivedTime",
                )}
              </div>
            </div>

            {previewErrors.length > 0 && (
              <InlineNotification
                kind="error"
                title={intl.formatMessage({
                  id: "notebook.manifest.validationErrors",
                  defaultMessage: "Validation Errors",
                })}
                subtitle={
                  <ul className="error-list">
                    {previewErrors.map((error, idx) => (
                      <li key={idx}>
                        {error.rowNumber > 0 ? `Row ${error.rowNumber}: ` : ""}
                        {error.message}
                      </li>
                    ))}
                  </ul>
                }
                lowContrast
                hideCloseButton
              />
            )}
          </div>
        )}

        {step === 3 && (
          <div className="preview-step">
            {isPreviewLoading ? (
              <Loading withOverlay={false} description="Loading preview..." />
            ) : (
              <>
                {previewErrors.length > 0 && (
                  <InlineNotification
                    kind="error"
                    title={intl.formatMessage({
                      id: "notebook.manifest.validationErrors",
                      defaultMessage: "Validation Errors",
                    })}
                    subtitle={
                      <ul className="error-list">
                        {previewErrors.map((error, idx) => (
                          <li key={idx}>
                            {error.rowNumber > 0
                              ? `Row ${error.rowNumber}: `
                              : ""}
                            {error.message}
                          </li>
                        ))}
                      </ul>
                    }
                    lowContrast
                    hideCloseButton
                  />
                )}

                {previewData && previewErrors.length === 0 && (
                  <div className="preview-summary">
                    <Tag type="blue">
                      <FormattedMessage
                        id="notebook.manifest.preview.rows"
                        defaultMessage="{count} rows"
                        values={{ count: previewData.totalRows }}
                      />
                    </Tag>
                    <Tag type="green">
                      <FormattedMessage
                        id="notebook.manifest.preview.samples"
                        defaultMessage="{count} samples to create"
                        values={{ count: previewData.totalSamples }}
                      />
                    </Tag>
                  </div>
                )}
              </>
            )}
          </div>
        )}

        {step === 4 && (
          <div className="success-step">
            <Checkmark size={48} className="success-icon" />
            <h3>
              <FormattedMessage
                id="notebook.manifest.success.title"
                defaultMessage="Import Successful"
              />
            </h3>
            <p>
              <FormattedMessage
                id="notebook.tb.manifest.success.message"
                defaultMessage="TB samples have been created and linked to the notebook entry with accession metadata."
              />
            </p>
          </div>
        )}
      </div>
    </Modal>
  );
}

export default TBManifestImportModal;
