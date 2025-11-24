import React, { useState } from "react";
import {
  FileUploader,
  FormGroup,
  Select,
  SelectItem,
  TextInput,
  Button,
  InlineLoading,
  InlineNotification,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import documentService from "../../../services/documentService";

/**
 * DocumentUploader component for uploading patient ID documents.
 * Uses Carbon Design System FileUploader component.
 */
const DocumentUploader = ({ patientId, onUploadSuccess, onUploadError }) => {
  const intl = useIntl();
  const [file, setFile] = useState(null);
  const [documentType, setDocumentType] = useState("");
  const [description, setDescription] = useState("");
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(false);

  const handleFileChange = (event) => {
    const files = event.target.files;
    if (files && files.length > 0) {
      setFile(files[0]);
      setError(null);
      setSuccess(false);
    }
  };

  const handleUpload = () => {
    if (!file) {
      setError(intl.formatMessage({ id: "document.upload.error.noFile" }));
      return;
    }

    if (!documentType) {
      setError(intl.formatMessage({ id: "document.upload.error.noType" }));
      return;
    }

    // Validate file size (10MB max)
    const maxSize = 10 * 1024 * 1024; // 10MB
    if (file.size > maxSize) {
      setError(intl.formatMessage({ id: "document.upload.error.fileTooLarge" }));
      return;
    }

    // Validate file type
    const allowedTypes = ["image/jpeg", "image/png", "application/pdf"];
    if (!allowedTypes.includes(file.type)) {
      setError(intl.formatMessage({ id: "document.upload.error.invalidType" }));
      return;
    }

    setUploading(true);
    setError(null);
    setSuccess(false);

    documentService.uploadDocument(
      patientId,
      file,
      documentType,
      description,
      (result, err) => {
        setUploading(false);
        if (err || !result) {
          setError(err?.message || intl.formatMessage({ id: "document.upload.error.failed" }));
          if (onUploadError) {
            onUploadError(err);
          }
        } else {
          setSuccess(true);
          setFile(null);
          setDocumentType("");
          setDescription("");
          if (onUploadSuccess) {
            onUploadSuccess(result);
          }
          // Clear success message after 3 seconds
          setTimeout(() => setSuccess(false), 3000);
        }
      }
    );
  };

  return (
    <div>
      <FormGroup legendText={intl.formatMessage({ id: "document.upload.title" })}>
        <FileUploader
          buttonLabel={intl.formatMessage({ id: "document.upload.button.selectFile" })}
          filenameStatus={file ? "complete" : "edit"}
          accept={["image/jpeg", "image/png", "application/pdf"]}
          multiple={false}
          onChange={handleFileChange}
          disabled={uploading}
        />
      </FormGroup>

      <FormGroup>
        <Select
          id="document-type-select"
          labelText={intl.formatMessage({ id: "document.upload.type.label" })}
          value={documentType}
          onChange={(e) => setDocumentType(e.target.value)}
          disabled={uploading}
        >
          <SelectItem value="" text={intl.formatMessage({ id: "document.upload.type.select" })} />
          <SelectItem value="NATIONAL_ID" text={intl.formatMessage({ id: "document.type.nationalId" })} />
          <SelectItem value="INSURANCE_CARD" text={intl.formatMessage({ id: "document.type.insuranceCard" })} />
          <SelectItem value="OTHER" text={intl.formatMessage({ id: "document.type.other" })} />
        </Select>
      </FormGroup>

      {documentType === "OTHER" && (
        <FormGroup>
          <TextInput
            id="document-description"
            labelText={intl.formatMessage({ id: "document.upload.description.label" })}
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            disabled={uploading}
            placeholder={intl.formatMessage({ id: "document.upload.description.placeholder" })}
          />
        </FormGroup>
      )}

      {error && (
        <InlineNotification
          kind="error"
          title={intl.formatMessage({ id: "notification.error.title" })}
          subtitle={error}
          onClose={() => setError(null)}
        />
      )}

      {success && (
        <InlineNotification
          kind="success"
          title={intl.formatMessage({ id: "notification.success.title" })}
          subtitle={intl.formatMessage({ id: "document.upload.success" })}
          onClose={() => setSuccess(false)}
        />
      )}

      <Button
        onClick={handleUpload}
        disabled={!file || !documentType || uploading}
        style={{ marginTop: "1rem" }}
      >
        {uploading ? (
          <InlineLoading description={intl.formatMessage({ id: "document.upload.uploading" })} />
        ) : (
          <FormattedMessage id="document.upload.button.upload" />
        )}
      </Button>
    </div>
  );
};

export default DocumentUploader;

