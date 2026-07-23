import React, { useState, useRef } from "react";
import {
  FileUploaderDropContainer,
  FileUploaderItem,
  Button,
  InlineLoading,
} from "@carbon/react";
import { TrashCan, Add } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import "./NceFileAttachment.css";

const ACCEPTED_FILE_TYPES = [
  ".jpg",
  ".jpeg",
  ".png",
  ".gif",
  ".pdf",
  ".doc",
  ".docx",
  ".xls",
  ".xlsx",
  ".txt",
];

const MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

export const NceFileAttachment = ({
  attachments = [],
  onAdd,
  onRemove,
  onAttachmentsChange,
  disabled = false,
  maxFiles = 5,
}) => {
  const intl = useIntl();
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState(null);
  const fileInputRef = useRef(null);

  const validateFile = (file) => {
    const extension = "." + file.name.split(".").pop().toLowerCase();
    if (!ACCEPTED_FILE_TYPES.includes(extension)) {
      return intl.formatMessage(
        { id: "nce.attachment.error.type" },
        { types: ACCEPTED_FILE_TYPES.join(", ") },
      );
    }
    if (file.size > MAX_FILE_SIZE) {
      return intl.formatMessage(
        { id: "nce.attachment.error.size" },
        { maxSize: "10MB" },
      );
    }
    return null;
  };

  const handleFileChange = async (event) => {
    const files = event.target.files || event.dataTransfer?.files;
    if (!files || files.length === 0) return;

    setError(null);

    if (attachments.length + files.length > maxFiles) {
      setError(
        intl.formatMessage(
          { id: "nce.attachment.error.maxFiles" },
          { maxFiles },
        ),
      );
      return;
    }

    for (const file of files) {
      const validationError = validateFile(file);
      if (validationError) {
        setError(validationError);
        return;
      }

      setUploading(true);
      try {
        // Create attachment object with file info
        const attachment = {
          id: `temp-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
          fileName: file.name,
          fileType: file.type,
          fileSize: file.size,
          file: file, // Keep reference to actual file for upload
          isNew: true,
        };

        if (onAdd) {
          await onAdd(attachment);
        }
        if (onAttachmentsChange) {
          onAttachmentsChange([...attachments, attachment]);
        }
      } catch (err) {
        setError(
          intl.formatMessage({ id: "nce.attachment.error.upload" }) +
            ": " +
            err.message,
        );
      } finally {
        setUploading(false);
      }
    }

    // Reset file input
    if (fileInputRef.current) {
      fileInputRef.current.value = "";
    }
  };

  const handleRemove = (attachmentId) => {
    if (onRemove) {
      onRemove(attachmentId);
    }
    if (onAttachmentsChange) {
      onAttachmentsChange(attachments.filter((a) => a.id !== attachmentId));
    }
  };

  const formatFileSize = (bytes) => {
    if (bytes === 0) return "0 Bytes";
    const k = 1024;
    const sizes = ["Bytes", "KB", "MB", "GB"];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + " " + sizes[i];
  };

  const getFileIcon = (fileType) => {
    if (fileType?.startsWith("image/")) return "📷";
    if (fileType?.includes("pdf")) return "📄";
    if (fileType?.includes("word") || fileType?.includes("document"))
      return "📝";
    if (fileType?.includes("excel") || fileType?.includes("spreadsheet"))
      return "📊";
    return "📎";
  };

  return (
    <div className="nce-file-attachment">
      <div className="nce-attachment-label">
        <FormattedMessage
          id="nce.field.attachments"
          defaultMessage="Attachments"
        />
        <span className="nce-attachment-hint">
          <FormattedMessage
            id="nce.attachment.hint"
            defaultMessage="(Optional - max {maxFiles} files, 10MB each)"
            values={{ maxFiles }}
          />
        </span>
      </div>

      {error && <div className="nce-attachment-error">{error}</div>}

      <div className="nce-attachment-dropzone">
        <FileUploaderDropContainer
          accept={ACCEPTED_FILE_TYPES}
          labelText={intl.formatMessage({
            id: "nce.attachment.dropzone",
            defaultMessage: "Drag and drop files here or click to upload",
          })}
          onAddFiles={(event, { addedFiles }) => {
            handleFileChange({ target: { files: addedFiles } });
          }}
          disabled={disabled || uploading || attachments.length >= maxFiles}
        />
        <input
          type="file"
          ref={fileInputRef}
          onChange={handleFileChange}
          accept={ACCEPTED_FILE_TYPES.join(",")}
          multiple
          style={{ display: "none" }}
        />
      </div>

      {uploading && (
        <InlineLoading
          description={intl.formatMessage({
            id: "nce.attachment.uploading",
            defaultMessage: "Uploading...",
          })}
        />
      )}

      {attachments.length > 0 && (
        <div className="nce-attachment-list">
          {attachments.map((attachment) => (
            <div key={attachment.id} className="nce-attachment-item">
              <span className="nce-attachment-icon">
                {getFileIcon(attachment.fileType)}
              </span>
              <div className="nce-attachment-info">
                <span className="nce-attachment-name">
                  {attachment.fileName}
                </span>
                <span className="nce-attachment-size">
                  {formatFileSize(attachment.fileSize)}
                </span>
              </div>
              {!disabled && (
                <Button
                  kind="ghost"
                  size="sm"
                  hasIconOnly
                  iconDescription={intl.formatMessage({
                    id: "label.button.remove",
                  })}
                  renderIcon={TrashCan}
                  onClick={() => handleRemove(attachment.id)}
                  className="nce-attachment-remove"
                />
              )}
            </div>
          ))}
        </div>
      )}

      <div className="nce-attachment-footer">
        <span className="nce-attachment-count">
          {attachments.length} / {maxFiles}{" "}
          <FormattedMessage id="nce.attachment.files" defaultMessage="files" />
        </span>
      </div>
    </div>
  );
};

export default NceFileAttachment;
