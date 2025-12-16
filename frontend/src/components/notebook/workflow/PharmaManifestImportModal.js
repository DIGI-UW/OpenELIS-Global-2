import React, { useState, useCallback } from "react";
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
} from "@carbon/react";
import { Upload, Checkmark, Warning } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import config from "../../../config.json";
import "./NotebookWorkflow.css";

/**
 * PharmaManifestImportModal - CSV import modal for Pharmaceuticals workflow.
 * Supports mapping pharma-specific columns (group_id, sample_type, num_of_samples, chemistry metadata).
 */
function PharmaManifestImportModal({ open, onClose, entryId, onImportSuccess }) {
  const intl = useIntl();

  const [file, setFile] = useState(null);
  const [fileError, setFileError] = useState(null);
  const [csvHeaders, setCsvHeaders] = useState([]);
  const [columnMapping, setColumnMapping] = useState({
    groupIdColumn: "",
    sampleTypeColumn: "",
    numOfSamplesColumn: "",
    chemicalNameColumn: "",
    gradeColumn: "",
    lotNumberColumn: "",
    dateOfManufactureColumn: "",
    expiryOrRetestDateColumn: "",
    storageConditionColumn: "",
    ownerColumn: "",
    patientIdColumn: "",
    clinicalTrialNumberColumn: "",
    consentStatusColumn: "",
    notesColumn: "",
  });

  const [step, setStep] = useState(1);
  const [previewData, setPreviewData] = useState(null);
  const [previewErrors, setPreviewErrors] = useState([]);
  const [isPreviewLoading, setIsPreviewLoading] = useState(false);
  const [isImporting, setIsImporting] = useState(false);

  const handleFileAdded = useCallback(
    (event, { addedFiles }) => {
      const addedFile = addedFiles[0];
      if (!addedFile) return;

      if (!addedFile.name.endsWith(".csv")) {
        setFileError(
          intl.formatMessage({
            id: "notebook.pharma.manifest.error.invalidFileType",
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
        setStep(2);
      };
      reader.readAsText(addedFile);
    },
    [intl],
  );

  const handleRemoveFile = () => {
    setFile(null);
    setCsvHeaders([]);
    setColumnMapping({
      groupIdColumn: "",
      sampleTypeColumn: "",
      numOfSamplesColumn: "",
      chemicalNameColumn: "",
      gradeColumn: "",
      lotNumberColumn: "",
      dateOfManufactureColumn: "",
      expiryOrRetestDateColumn: "",
      storageConditionColumn: "",
      ownerColumn: "",
      patientIdColumn: "",
      clinicalTrialNumberColumn: "",
      consentStatusColumn: "",
      notesColumn: "",
    });
    setPreviewData(null);
    setPreviewErrors([]);
    setStep(1);
  };

  const handleMappingChange = (field, value) => {
    setColumnMapping((prev) => ({ ...prev, [field]: value }));
  };

  const requiredColumnsMapped =
    columnMapping.groupIdColumn &&
    columnMapping.sampleTypeColumn &&
    columnMapping.numOfSamplesColumn;

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
      const endpoint = `${config.serverBaseUrl}/rest/notebook/pharma/entry/${entryId}/samples/preview-manifest`;
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
            { rowNumber: 0, column: "file", message: data.error || "Preview failed" },
          ],
        );
      }
    } catch (error) {
      setPreviewErrors([{ rowNumber: 0, column: "file", message: error.message }]);
    } finally {
      setIsPreviewLoading(false);
    }
  };

  const handleImport = async () => {
    if (!file || !entryId) return;
    setIsImporting(true);
    setPreviewErrors([]);

    try {
      const endpoint = `${config.serverBaseUrl}/rest/notebook/pharma/entry/${entryId}/samples/create-from-manifest`;
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
            { rowNumber: 0, column: "import", message: data.error || "Import failed" },
          ],
        );
      }
    } catch (error) {
      setPreviewErrors([{ rowNumber: 0, column: "import", message: error.message }]);
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
        id: "notebook.pharma.manifest.title",
        defaultMessage: "Import Pharmaceutical Samples from Manifest",
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
        step > 1 && step < 4 ? intl.formatMessage({ id: "label.button.back" }) : null
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
                id="notebook.pharma.manifest.step1"
                defaultMessage="Upload a CSV with pharma sample data (group_id, sample_type, num_of_samples, chemistry metadata)."
              />
            </p>
            {!file ? (
              <FileUploaderDropContainer
                accept={[".csv"]}
                labelText={intl.formatMessage({
                  id: "notebook.pharma.manifest.dropzone",
                  defaultMessage: "Drag and drop a CSV file here or click to upload",
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
                id="notebook.pharma.manifest.step2"
                defaultMessage="Map CSV columns to required pharma fields. Required fields are marked with *."
              />
            </p>
            <div className="mapping-grid">
              <div className="mapping-field required">
                {mappingField("groupIdColumn", "notebook.pharma.manifest.column.groupId")}
                <span className="required-marker">*</span>
              </div>
              <div className="mapping-field required">
                {mappingField("sampleTypeColumn", "notebook.pharma.manifest.column.sampleType")}
                <span className="required-marker">*</span>
              </div>
              <div className="mapping-field required">
                {mappingField("numOfSamplesColumn", "notebook.pharma.manifest.column.numOfSamples")}
                <span className="required-marker">*</span>
              </div>
              {mappingField("chemicalNameColumn", "notebook.pharma.manifest.column.chemicalName")}
              {mappingField("gradeColumn", "notebook.pharma.manifest.column.grade")}
              {mappingField("lotNumberColumn", "notebook.pharma.manifest.column.lotNumber")}
              {mappingField("dateOfManufactureColumn", "notebook.pharma.manifest.column.dateOfManufacture")}
              {mappingField("expiryOrRetestDateColumn", "notebook.pharma.manifest.column.expiryOrRetestDate")}
              {mappingField("storageConditionColumn", "notebook.pharma.manifest.column.storageCondition")}
              {mappingField("ownerColumn", "notebook.pharma.manifest.column.owner")}
              {mappingField("patientIdColumn", "notebook.pharma.manifest.column.patientId")}
              {mappingField("clinicalTrialNumberColumn", "notebook.pharma.manifest.column.clinicalTrialNumber")}
              {mappingField("consentStatusColumn", "notebook.pharma.manifest.column.consentStatus")}
              {mappingField("notesColumn", "notebook.pharma.manifest.column.notes")}
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
                        values={{ count: previewData.totalSamplesToCreate }}
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
                id="notebook.manifest.success.message"
                defaultMessage="Samples have been created and linked to the notebook entry."
              />
            </p>
          </div>
        )}
      </div>
    </Modal>
  );
}

export default PharmaManifestImportModal;

