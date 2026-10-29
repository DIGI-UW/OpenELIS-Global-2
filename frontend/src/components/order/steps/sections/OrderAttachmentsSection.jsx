import React, { useCallback, useEffect, useRef, useState } from "react";
import { useIntl, FormattedMessage } from "react-intl";
import {
  Tile,
  Button,
  FileUploaderDropContainer,
  InlineNotification,
  Link,
  Modal,
  Tag,
} from "@carbon/react";
import { TrashCan } from "@carbon/icons-react";
import {
  deleteFromOpenElisServer,
  getFromOpenElisServer,
  postToOpenElisServerFormData,
} from "../../../utils/Utils";
import config from "../../../../config.json";
import { useOrderContext } from "../../OrderContext";

const MAX_FILES = 5;
const MAX_SIZE_BYTES = 10 * 1024 * 1024;
const ALLOWED_EXTENSIONS = [".pdf", ".jpg", ".jpeg", ".png", ".tif", ".tiff"];

const formatFileSize = (bytes) => {
  if (!bytes) return "0 B";
  const units = ["B", "KB", "MB", "GB"];
  const index = Math.floor(Math.log(bytes) / Math.log(1024));
  return `${parseFloat((bytes / Math.pow(1024, index)).toFixed(2))} ${units[index]}`;
};

/**
 * Request forms, referral letters and consent scans attached to an order.
 *
 * The capability existed on the legacy screen and its REST API is unchanged;
 * only the new lanes had no way in. Attachments are keyed by accession number,
 * so the section appears once the order has a lab number, and files can be
 * listed and added once the order has been saved under that number.
 */
const OrderAttachmentsSection = ({ labNumber, isReadOnly }) => {
  const intl = useIntl();
  const {
    orderId,
    stagedAttachments,
    setStagedAttachments,
    attachmentsVersion,
  } = useOrderContext();
  const isSaved = Boolean(orderId);
  const componentMounted = useRef(true);
  const [attachments, setAttachments] = useState([]);
  const fileCount = isSaved ? attachments.length : stagedAttachments.length;
  const [error, setError] = useState(null);
  const [attachmentToDelete, setAttachmentToDelete] = useState(null);

  useEffect(() => {
    componentMounted.current = true;
    return () => {
      componentMounted.current = false;
    };
  }, []);

  const load = useCallback(() => {
    if (!labNumber || !isSaved) {
      setAttachments([]);
      return;
    }
    getFromOpenElisServer(
      `/rest/order/${encodeURIComponent(labNumber)}/attachments`,
      (data) => {
        if (componentMounted.current && Array.isArray(data)) {
          setAttachments(data);
        }
      },
    );
  }, [labNumber, isSaved, attachmentsVersion]);

  useEffect(() => {
    load();
  }, [load]);

  const reject = (files) => {
    if (fileCount + files.length > MAX_FILES) {
      return intl.formatMessage(
        { id: "order.attachment.error.maxFiles" },
        { maxFiles: MAX_FILES },
      );
    }
    for (const file of files) {
      const extension = file.name
        .slice(file.name.lastIndexOf("."))
        .toLowerCase();
      if (!ALLOWED_EXTENSIONS.includes(extension)) {
        return intl.formatMessage(
          { id: "order.attachment.error.type" },
          { types: ALLOWED_EXTENSIONS.join(", ") },
        );
      }
      if (file.size > MAX_SIZE_BYTES) {
        return intl.formatMessage(
          { id: "order.attachment.error.size" },
          { maxSize: formatFileSize(MAX_SIZE_BYTES) },
        );
      }
    }
    return null;
  };

  const handleAdd = (addedFiles) => {
    setError(null);
    if (!addedFiles || addedFiles.length === 0 || !labNumber) {
      return;
    }
    const problem = reject(addedFiles);
    if (problem) {
      setError(problem);
      return;
    }
    if (!isSaved) {
      setStagedAttachments((current) => [...current, ...addedFiles]);
      return;
    }
    const formData = new FormData();
    addedFiles.forEach((file) => formData.append("files", file, file.name));
    postToOpenElisServerFormData(
      `/rest/order/${encodeURIComponent(labNumber)}/attachments`,
      formData,
      (status) => {
        if (status >= 200 && status < 300) {
          load();
        } else {
          setError(
            intl.formatMessage({ id: "order.attachment.upload.failed" }),
          );
        }
      },
    );
  };

  const openAttachment = (attachmentId, action) =>
    window.open(
      `${config.serverBaseUrl}/rest/order/attachments/${encodeURIComponent(attachmentId)}/${action}`,
      "_blank",
    );

  const confirmDelete = () => {
    const { id } = attachmentToDelete;
    setAttachmentToDelete(null);
    deleteFromOpenElisServer(
      `/rest/order/attachments/${encodeURIComponent(id)}`,
      (status) => {
        if (status >= 200 && status < 300) {
          load();
        } else {
          setError(
            intl.formatMessage({ id: "order.attachment.delete.failed" }),
          );
        }
      },
    );
  };

  if (!labNumber) {
    return null;
  }

  return (
    <Tile className="order-section order-attachments-section">
      <h4 className="section-title">
        <FormattedMessage
          id="order.attachment.heading"
          defaultMessage="Attachments"
        />
      </h4>
      <p className="helper-text">
        <FormattedMessage
          id="order.attachment.hint"
          defaultMessage="Optional. Up to {maxFiles} files, 10MB each. Allowed: PDF, JPG, PNG, TIFF."
          values={{ maxFiles: MAX_FILES }}
        />
      </p>

      {error && (
        <InlineNotification
          kind="error"
          lowContrast
          title={intl.formatMessage({ id: "notification.title" })}
          subtitle={error}
          onCloseButtonClick={() => setError(null)}
        />
      )}

      {!isReadOnly && (
        <FileUploaderDropContainer
          accept={ALLOWED_EXTENSIONS}
          multiple
          disabled={fileCount >= MAX_FILES}
          labelText={intl.formatMessage({ id: "order.attachment.dropzone" })}
          onAddFiles={(_event, { addedFiles }) => handleAdd(addedFiles)}
        />
      )}

      {isSaved && attachments.length === 0 && (
        <p className="helper-text">
          <FormattedMessage
            id="order.attachment.empty"
            defaultMessage="No attachments yet."
          />
        </p>
      )}

      <ul className="order-attachment-list">
        {!isSaved &&
          stagedAttachments.map((file, index) => (
            <li key={`${file.name}-${index}`}>
              <span>{file.name}</span>
              <span className="order-attachment-size">
                {formatFileSize(file.size)}
              </span>
              <Tag type="gray" size="sm">
                <FormattedMessage id="order.attachment.staged" />
              </Tag>
              <Button
                kind="ghost"
                size="sm"
                onClick={() =>
                  setStagedAttachments((current) =>
                    current.filter((_, i) => i !== index),
                  )
                }
              >
                <FormattedMessage id="label.button.remove" />
              </Button>
            </li>
          ))}
        {attachments.map((attachment) => (
          <li key={attachment.id}>
            <Link
              href="#"
              onClick={(event) => {
                event.preventDefault();
                openAttachment(attachment.id, "view");
              }}
            >
              {attachment.fileName}
            </Link>
            <span className="order-attachment-size">
              {formatFileSize(attachment.fileSizeBytes)}
            </span>
            <Button
              kind="ghost"
              size="sm"
              onClick={() => openAttachment(attachment.id, "download")}
            >
              <FormattedMessage
                id="order.attachment.action.download"
                defaultMessage="Download attachment"
              />
            </Button>
            {!isReadOnly && (
              <Button
                kind="ghost"
                size="sm"
                hasIconOnly
                renderIcon={TrashCan}
                iconDescription={intl.formatMessage({
                  id: "order.attachment.action.delete",
                  defaultMessage: "Delete attachment",
                })}
                onClick={() => setAttachmentToDelete(attachment)}
              />
            )}
          </li>
        ))}
      </ul>
      <Modal
        open={attachmentToDelete !== null}
        modalHeading={intl.formatMessage({
          id: "order.attachment.delete.confirm.title",
        })}
        primaryButtonText={intl.formatMessage({ id: "label.button.delete" })}
        secondaryButtonText={intl.formatMessage({ id: "label.button.cancel" })}
        onRequestClose={() => setAttachmentToDelete(null)}
        onRequestSubmit={confirmDelete}
        danger
      >
        <p>
          <FormattedMessage
            id="order.attachment.delete.confirm.body"
            values={{ fileName: attachmentToDelete?.fileName ?? "" }}
          />
        </p>
      </Modal>
    </Tile>
  );
};

export default OrderAttachmentsSection;
