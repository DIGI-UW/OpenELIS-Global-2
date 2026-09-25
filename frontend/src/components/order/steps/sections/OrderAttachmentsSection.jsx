import React, { useCallback, useEffect, useRef, useState } from "react";
import { useIntl, FormattedMessage } from "react-intl";
import {
  Tile,
  Button,
  FileUploaderDropContainer,
  InlineNotification,
  Link,
} from "@carbon/react";
import { TrashCan } from "@carbon/icons-react";
import {
  deleteFromOpenElisServer,
  getFromOpenElisServer,
  postToOpenElisServerFormData,
} from "../../../utils/Utils";
import config from "../../../../config.json";

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
 * so the section appears once the order has a lab number.
 */
const OrderAttachmentsSection = ({ labNumber, isReadOnly }) => {
  const intl = useIntl();
  const componentMounted = useRef(true);
  const [attachments, setAttachments] = useState([]);
  const [error, setError] = useState(null);

  useEffect(() => {
    componentMounted.current = true;
    return () => {
      componentMounted.current = false;
    };
  }, []);

  const load = useCallback(() => {
    if (!labNumber) {
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
  }, [labNumber]);

  useEffect(() => {
    load();
  }, [load]);

  const reject = (files) => {
    if (attachments.length + files.length > MAX_FILES) {
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
          disabled={attachments.length >= MAX_FILES}
          labelText={intl.formatMessage({ id: "order.attachment.dropzone" })}
          onAddFiles={(_event, { addedFiles }) => handleAdd(addedFiles)}
        />
      )}

      {attachments.length === 0 && (
        <p className="helper-text">
          <FormattedMessage
            id="order.attachment.empty"
            defaultMessage="No attachments yet."
          />
        </p>
      )}

      <ul className="order-attachment-list">
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
              {formatFileSize(attachment.fileSize)}
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
                kind="danger--ghost"
                size="sm"
                hasIconOnly
                renderIcon={TrashCan}
                iconDescription={intl.formatMessage({
                  id: "order.attachment.delete.confirm.title",
                  defaultMessage: "Delete attachment",
                })}
                onClick={() =>
                  deleteFromOpenElisServer(
                    `/rest/order/attachments/${encodeURIComponent(attachment.id)}`,
                    (status) => {
                      if (status >= 200 && status < 300) {
                        load();
                      } else {
                        setError(
                          intl.formatMessage({
                            id: "order.attachment.delete.failed",
                          }),
                        );
                      }
                    },
                  )
                }
              />
            )}
          </li>
        ))}
      </ul>
    </Tile>
  );
};

export default OrderAttachmentsSection;
