import React from "react";
import { Image, Tag, ButtonSet, Button } from "@carbon/react";
import { View, Edit, TrashCan } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import config from "../../../config.json";

/**
 * DocumentThumbnail component for displaying document thumbnails with metadata.
 * Uses Carbon Design System Image component.
 */
const DocumentThumbnail = ({ document, onView, onEdit, onDelete }) => {
  const intl = useIntl();

  const getThumbnailUrl = () => {
    if (document.thumbnailUrl) {
      // If thumbnailUrl is a full URL, return it; otherwise construct from server base URL
      if (document.thumbnailUrl.startsWith("http")) {
        return document.thumbnailUrl;
      }
      return config.serverBaseUrl + document.thumbnailUrl;
    }
    // Placeholder icon if no thumbnail
    return null;
  };

  const formatDate = (dateString) => {
    if (!dateString) return "";
    const date = new Date(dateString);
    return intl.formatDate(date, { year: "numeric", month: "short", day: "numeric" });
  };

  const getDocumentTypeLabel = (type) => {
    switch (type) {
      case "NATIONAL_ID":
        return intl.formatMessage({ id: "document.type.nationalId" });
      case "INSURANCE_CARD":
        return intl.formatMessage({ id: "document.type.insuranceCard" });
      case "OTHER":
        return intl.formatMessage({ id: "document.type.other" });
      default:
        return type;
    }
  };

  return (
    <div style={{ padding: "1rem", border: "1px solid #e0e0e0", borderRadius: "4px" }}>
      {getThumbnailUrl() ? (
        <Image
          alt={document.description || document.documentType}
          src={getThumbnailUrl()}
          style={{ width: "200px", height: "200px", objectFit: "cover" }}
        />
      ) : (
        <div
          style={{
            width: "200px",
            height: "200px",
            backgroundColor: "#f4f4f4",
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            color: "#8d8d8d",
          }}
        >
          <FormattedMessage id="document.thumbnail.placeholder" />
        </div>
      )}

      <div style={{ marginTop: "0.5rem" }}>
        <Tag type={document.documentType === "NATIONAL_ID" ? "blue" : "green"}>
          {getDocumentTypeLabel(document.documentType)}
        </Tag>
      </div>

      {document.description && (
        <div style={{ marginTop: "0.5rem", fontSize: "0.875rem", color: "#525252" }}>
          {document.description}
        </div>
      )}

      <div style={{ marginTop: "0.5rem", fontSize: "0.75rem", color: "#8d8d8d" }}>
        {formatDate(document.createdAt)}
      </div>

      <ButtonSet style={{ marginTop: "0.5rem" }}>
        {onView && (
          <Button
            kind="ghost"
            size="sm"
            renderIcon={View}
            onClick={() => onView(document)}
            iconDescription={intl.formatMessage({ id: "document.action.view" })}
          >
            <FormattedMessage id="document.action.view" />
          </Button>
        )}
        {onEdit && (
          <Button
            kind="ghost"
            size="sm"
            renderIcon={Edit}
            onClick={() => onEdit(document)}
            iconDescription={intl.formatMessage({ id: "document.action.edit" })}
          >
            <FormattedMessage id="document.action.edit" />
          </Button>
        )}
        {onDelete && (
          <Button
            kind="ghost"
            size="sm"
            renderIcon={TrashCan}
            onClick={() => onDelete(document)}
            iconDescription={intl.formatMessage({ id: "document.action.delete" })}
            danger
          >
            <FormattedMessage id="document.action.delete" />
          </Button>
        )}
      </ButtonSet>
    </div>
  );
};

export default DocumentThumbnail;

