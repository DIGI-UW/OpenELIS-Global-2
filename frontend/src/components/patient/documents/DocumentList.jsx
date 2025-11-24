import React, { useState, useEffect } from "react";
import {
  DataTable,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableHeader,
  TableRow,
  Tag,
  Button,
  ButtonSet,
  Image,
} from "@carbon/react";
import { View, Edit, TrashCan } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import documentService from "../../../services/documentService";
import DocumentThumbnail from "./DocumentThumbnail";
import config from "../../../config.json";

/**
 * DocumentList component for displaying a list of patient documents.
 * Uses Carbon Design System DataTable component.
 */
const DocumentList = ({ patientId, onView, onEdit, onDelete, onRefresh }) => {
  const intl = useIntl();
  const [documents, setDocuments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    loadDocuments();
  }, [patientId]);

  useEffect(() => {
    if (onRefresh) {
      // Reload when refresh is triggered
      loadDocuments();
    }
  }, [onRefresh]);

  const loadDocuments = () => {
    if (!patientId) {
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);

    documentService.getDocuments(patientId, (docs) => {
      setLoading(false);
      if (docs && Array.isArray(docs)) {
        setDocuments(docs);
      } else {
        setDocuments([]);
      }
    });
  };

  const getThumbnailUrl = (doc) => {
    if (doc.thumbnailUrl) {
      if (doc.thumbnailUrl.startsWith("http")) {
        return doc.thumbnailUrl;
      }
      return config.serverBaseUrl + doc.thumbnailUrl;
    }
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

  const headers = [
    { key: "thumbnail", header: intl.formatMessage({ id: "document.list.header.thumbnail" }) },
    { key: "type", header: intl.formatMessage({ id: "document.list.header.type" }) },
    { key: "description", header: intl.formatMessage({ id: "document.list.header.description" }) },
    { key: "createdAt", header: intl.formatMessage({ id: "document.list.header.createdAt" }) },
    { key: "actions", header: intl.formatMessage({ id: "document.list.header.actions" }) },
  ];

  const rows = documents.map((doc) => ({
    id: doc.documentId,
    thumbnail: getThumbnailUrl(doc) ? (
      <Image
        alt={doc.description || doc.documentType}
        src={getThumbnailUrl(doc)}
        style={{ width: "80px", height: "80px", objectFit: "cover" }}
      />
    ) : (
      <div
        style={{
          width: "80px",
          height: "80px",
          backgroundColor: "#f4f4f4",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          fontSize: "0.75rem",
          color: "#8d8d8d",
        }}
      >
        <FormattedMessage id="document.thumbnail.placeholder" />
      </div>
    ),
    type: (
      <Tag type={doc.documentType === "NATIONAL_ID" ? "blue" : "green"}>
        {getDocumentTypeLabel(doc.documentType)}
      </Tag>
    ),
    description: doc.description || "-",
    createdAt: formatDate(doc.createdAt),
    actions: (
      <ButtonSet>
        {onView && (
          <Button
            kind="ghost"
            size="sm"
            renderIcon={View}
            onClick={() => onView(doc)}
            iconDescription={intl.formatMessage({ id: "document.action.view" })}
          />
        )}
        {onEdit && (
          <Button
            kind="ghost"
            size="sm"
            renderIcon={Edit}
            onClick={() => onEdit(doc)}
            iconDescription={intl.formatMessage({ id: "document.action.edit" })}
          />
        )}
        {onDelete && (
          <Button
            kind="ghost"
            size="sm"
            renderIcon={TrashCan}
            onClick={() => onDelete(doc)}
            iconDescription={intl.formatMessage({ id: "document.action.delete" })}
            danger
          />
        )}
      </ButtonSet>
    ),
  }));

  if (loading) {
    return <div><FormattedMessage id="document.list.loading" /></div>;
  }

  if (error) {
    return <div><FormattedMessage id="document.list.error" />: {error}</div>;
  }

  if (documents.length === 0) {
    return <div><FormattedMessage id="document.list.empty" /></div>;
  }

  return (
    <TableContainer>
      <Table>
        <TableHead>
          <TableRow>
            {headers.map((header) => (
              <TableHeader key={header.key}>{header.header}</TableHeader>
            ))}
          </TableRow>
        </TableHead>
        <TableBody>
          {rows.map((row) => (
            <TableRow key={row.id}>
              {headers.map((header) => (
                <TableCell key={header.key}>{row[header.key]}</TableCell>
              ))}
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </TableContainer>
  );
};

export default DocumentList;

