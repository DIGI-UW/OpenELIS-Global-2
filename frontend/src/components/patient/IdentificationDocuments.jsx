import React, { useState, useEffect, useRef } from "react";
import {
  Button,
  Select,
  SelectItem,
  TextArea,
  Modal,
  Tile,
  Tag,
} from "@carbon/react";
import {
  Add,
  TrashCan,
  View,
  Edit,
  DocumentBlank,
  DocumentPdf,
  CloudUpload,
  Camera,
} from "@carbon/icons-react";
import ImagePreviewModal from "./photoManagement/uploadPhoto/ImagePreviewModal";
import {
  getFromOpenElisServer,
  deleteFromOpenElisServer,
  putToOpenElisServer,
} from "../utils/Utils";
import { useIntl } from "react-intl";
import "./IdentificationDocuments.css";

const DOCUMENT_CATEGORIES = [
  { value: "NATIONAL_ID", labelId: "patient.idDoc.category.nationalId" },
  { value: "INSURANCE_CARD", labelId: "patient.idDoc.category.insuranceCard" },
  { value: "OTHER", labelId: "patient.idDoc.category.other" },
];

const ACCEPTED_FORMATS = "image/jpeg,image/png,image/jpg,application/pdf";
const MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

const IdentificationDocuments = ({
  patientId,
  pendingDocuments = [],
  onDocumentsChange,
  disabled = false,
}) => {
  const intl = useIntl();
  const fileInputRef = useRef(null);

  const [savedDocuments, setSavedDocuments] = useState([]);
  const [isUploadModalOpen, setIsUploadModalOpen] = useState(false);
  const [isCameraModalOpen, setIsCameraModalOpen] = useState(false);
  const [isViewModalOpen, setIsViewModalOpen] = useState(false);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [viewDocUrl, setViewDocUrl] = useState(null);
  const [viewDocType, setViewDocType] = useState(null);
  const [selectedDoc, setSelectedDoc] = useState(null);
  const [editCategory, setEditCategory] = useState("");
  const [editDescription, setEditDescription] = useState("");

  // Upload form state
  const [newDocCategory, setNewDocCategory] = useState("NATIONAL_ID");
  const [newDocDescription, setNewDocDescription] = useState("");
  const [newDocPreview, setNewDocPreview] = useState(null);
  const [newDocData, setNewDocData] = useState(null);
  const [newDocIsPdf, setNewDocIsPdf] = useState(false);
  const [isDragging, setIsDragging] = useState(false);
  const [fileError, setFileError] = useState("");

  useEffect(() => {
    if (patientId) {
      loadSavedDocuments();
    }
  }, [patientId]);

  const loadSavedDocuments = () => {
    getFromOpenElisServer(
      `/rest/patient-id-documents/${patientId}`,
      (response) => {
        if (response && Array.isArray(response)) {
          setSavedDocuments(response);
        }
      },
    );
  };

  const resetUploadForm = () => {
    setNewDocCategory("NATIONAL_ID");
    setNewDocDescription("");
    setNewDocPreview(null);
    setNewDocData(null);
    setNewDocIsPdf(false);
    setFileError("");
    setIsDragging(false);
  };

  const processFile = (file) => {
    setFileError("");

    if (!file) return;

    const validTypes = [
      "image/jpeg",
      "image/png",
      "image/jpg",
      "application/pdf",
    ];
    if (!validTypes.includes(file.type)) {
      setFileError(
        intl.formatMessage({ id: "patient.idDoc.error.invalidFormat" }),
      );
      return;
    }

    if (file.size > MAX_FILE_SIZE) {
      setFileError(
        intl.formatMessage({ id: "patient.idDoc.error.fileTooLarge" }),
      );
      return;
    }

    const isPdf = file.type === "application/pdf";
    setNewDocIsPdf(isPdf);

    const reader = new FileReader();
    reader.onloadend = () => {
      setNewDocData(reader.result);
      setNewDocPreview(isPdf ? null : reader.result);
    };
    reader.readAsDataURL(file);
  };

  const handleFileChange = (event) => {
    processFile(event.target.files[0]);
  };

  const handleDragOver = (event) => {
    event.preventDefault();
    setIsDragging(true);
  };

  const handleDragLeave = (event) => {
    event.preventDefault();
    setIsDragging(false);
  };

  const handleDrop = (event) => {
    event.preventDefault();
    setIsDragging(false);
    processFile(event.dataTransfer.files[0]);
  };

  const handleUploadSubmit = () => {
    if (!newDocData) return;
    const newDoc = {
      data: newDocData,
      category: newDocCategory,
      description: newDocDescription,
    };
    onDocumentsChange([...pendingDocuments, newDoc]);
    resetUploadForm();
    setIsUploadModalOpen(false);
  };

  const handleCameraCapture = (imageData) => {
    setNewDocData(imageData);
    setNewDocPreview(imageData);
    setNewDocIsPdf(false);
    setFileError("");
    setIsCameraModalOpen(false);
  };

  const handleRemovePending = (index) => {
    const updated = pendingDocuments.filter((_, i) => i !== index);
    onDocumentsChange(updated);
  };

  const handleViewSavedDocument = (doc) => {
    getFromOpenElisServer(
      `/rest/patient-id-documents/${patientId}/${doc.id}/full`,
      (response) => {
        if (response && response.data) {
          setViewDocUrl(response.data);
          setViewDocType(
            response.data.startsWith("data:application/pdf") ? "pdf" : "image",
          );
          setIsViewModalOpen(true);
        }
      },
    );
  };

  const handleViewPendingDocument = (doc) => {
    setViewDocUrl(doc.data);
    setViewDocType(
      doc.data.startsWith("data:application/pdf") ? "pdf" : "image",
    );
    setIsViewModalOpen(true);
  };

  const handleEditDocument = (doc) => {
    setSelectedDoc(doc);
    setEditCategory(doc.category);
    setEditDescription(doc.description || "");
    // Reset replacement-image state so the dropzone starts empty.
    // If the user picks a new file before saving, it goes in the PUT body.
    setNewDocData(null);
    setNewDocPreview(null);
    setNewDocIsPdf(false);
    setFileError("");
    setIsEditModalOpen(true);
  };

  const handleSaveEdit = () => {
    if (!selectedDoc) return;
    const body = {
      category: editCategory,
      description: editDescription,
    };
    if (newDocData) {
      body.data = newDocData;
    }
    putToOpenElisServer(
      `/rest/patient-id-documents/${selectedDoc.id}`,
      JSON.stringify(body),
      () => {
        loadSavedDocuments();
        setIsEditModalOpen(false);
        setSelectedDoc(null);
        setNewDocData(null);
        setNewDocPreview(null);
        setNewDocIsPdf(false);
      },
    );
  };

  const handleDeleteDocument = (doc) => {
    setSelectedDoc(doc);
    setIsDeleteModalOpen(true);
  };

  const handleConfirmDelete = () => {
    if (!selectedDoc) return;
    deleteFromOpenElisServer(
      `/rest/patient-id-documents/${selectedDoc.id}`,
      () => {
        loadSavedDocuments();
        setIsDeleteModalOpen(false);
        setSelectedDoc(null);
      },
    );
  };

  const getCategoryLabel = (categoryValue) => {
    const cat = DOCUMENT_CATEGORIES.find((c) => c.value === categoryValue);
    return cat ? intl.formatMessage({ id: cat.labelId }) : categoryValue || "";
  };

  const isPdfData = (data) => data && data.startsWith("data:application/pdf");

  return (
    <div className="id-documents-section">
      {/* Single hidden file input shared by both upload and edit modals.
          Hoisted out of any Modal so its ref stays valid regardless of
          which modal (or none) is currently open. */}
      <input
        ref={fileInputRef}
        type="file"
        accept={ACCEPTED_FORMATS}
        onChange={handleFileChange}
        style={{ display: "none" }}
      />
      <div className="id-documents-header">
        <h5 className="id-documents-title">
          {intl.formatMessage({ id: "patient.idDoc.title" })}
        </h5>
        {!disabled && (
          <Button
            kind="tertiary"
            size="sm"
            renderIcon={Add}
            onClick={() => {
              resetUploadForm();
              setIsUploadModalOpen(true);
            }}
          >
            {intl.formatMessage({ id: "patient.idDoc.addDocument" })}
          </Button>
        )}
      </div>

      {/* Upload Modal: file picker + category + description in one step */}
      <Modal
        open={isUploadModalOpen}
        onRequestClose={() => {
          resetUploadForm();
          setIsUploadModalOpen(false);
        }}
        modalHeading={intl.formatMessage({
          id: "patient.idDoc.addDocument",
        })}
        primaryButtonText={intl.formatMessage({
          id: "patient.idDoc.upload",
        })}
        primaryButtonDisabled={!newDocData}
        secondaryButtonText={intl.formatMessage({
          id: "patient.photo.cancel",
        })}
        onRequestSubmit={handleUploadSubmit}
        size="md"
      >
        <div className="id-documents-upload-form">
          <Select
            id="new-doc-category"
            labelText={intl.formatMessage({
              id: "patient.idDoc.documentType",
            })}
            value={newDocCategory}
            onChange={(e) => setNewDocCategory(e.target.value)}
          >
            {DOCUMENT_CATEGORIES.map((cat) => (
              <SelectItem
                key={cat.value}
                value={cat.value}
                text={intl.formatMessage({ id: cat.labelId })}
              />
            ))}
          </Select>

          <TextArea
            id="new-doc-description"
            labelText={intl.formatMessage({
              id: "patient.idDoc.description",
            })}
            value={newDocDescription}
            onChange={(e) => setNewDocDescription(e.target.value)}
            placeholder={intl.formatMessage({
              id: "patient.idDoc.description.placeholder",
            })}
            rows={2}
            maxCount={255}
          />

          {!newDocData ? (
            <div className="id-doc-upload-area">
              <div
                className={`id-doc-dropzone ${isDragging ? "id-doc-dropzone-active" : ""}`}
                onDragOver={handleDragOver}
                onDragLeave={handleDragLeave}
                onDrop={handleDrop}
                onClick={() => fileInputRef.current?.click()}
              >
                <CloudUpload size={48} className="id-doc-dropzone-icon" />
                <p className="id-doc-dropzone-title">
                  {intl.formatMessage({ id: "patient.idDoc.dragndrop" })}
                </p>
                <p className="id-doc-dropzone-subtitle">
                  {intl.formatMessage({ id: "patient.idDoc.browse" })}
                </p>
                <p className="id-doc-dropzone-formats">
                  {intl.formatMessage({ id: "patient.idDoc.formats" })}
                </p>
              </div>
              <Button
                kind="tertiary"
                size="sm"
                renderIcon={Camera}
                onClick={() => setIsCameraModalOpen(true)}
                className="id-doc-camera-btn"
              >
                {intl.formatMessage({ id: "patient.idDoc.useCamera" })}
              </Button>
            </div>
          ) : (
            <div className="id-doc-file-preview">
              {newDocIsPdf ? (
                <div className="id-doc-pdf-indicator">
                  <DocumentPdf size={48} />
                  <span>
                    {intl.formatMessage({ id: "patient.idDoc.pdfSelected" })}
                  </span>
                </div>
              ) : (
                <img
                  src={newDocPreview}
                  alt={intl.formatMessage({
                    id: "patient.photo.preview.alt",
                  })}
                  className="id-doc-preview-image"
                />
              )}
              <Button
                kind="tertiary"
                size="sm"
                onClick={() => {
                  setNewDocData(null);
                  setNewDocPreview(null);
                  setNewDocIsPdf(false);
                  setFileError("");
                }}
              >
                {intl.formatMessage({ id: "patient.photo.change" })}
              </Button>
            </div>
          )}

          {fileError && <p className="id-doc-error">{fileError}</p>}
        </div>
      </Modal>

      {/* Camera modal - reuses existing ImagePreviewModal */}
      <ImagePreviewModal
        open={isCameraModalOpen}
        onClose={() => setIsCameraModalOpen(false)}
        onImageSelect={handleCameraCapture}
        currentImage={null}
      />

      {/* Document grid */}
      <div className="id-documents-grid">
        {/* Saved documents (from server) */}
        {savedDocuments.map((doc) => (
          <Tile key={`saved-${doc.id}`} className="id-document-card">
            <div
              className="id-document-thumbnail"
              onClick={() => !disabled && handleViewSavedDocument(doc)}
            >
              {doc.thumbnail &&
              doc.thumbnail.startsWith("data:application/pdf") ? (
                <div className="id-doc-pdf-thumb">
                  <DocumentPdf size={40} />
                  <span>PDF</span>
                </div>
              ) : (
                <img
                  src={doc.thumbnail}
                  alt={getCategoryLabel(doc.category)}
                  className="id-document-image"
                />
              )}
            </div>
            <div className="id-document-info">
              <Tag type="blue" size="sm">
                {getCategoryLabel(doc.category)}
              </Tag>
              {doc.description && (
                <span className="id-document-description">
                  {doc.description}
                </span>
              )}
            </div>
            {!disabled && (
              <div className="id-document-actions">
                <Button
                  kind="ghost"
                  size="sm"
                  hasIconOnly
                  iconDescription={intl.formatMessage({
                    id: "patient.idDoc.view",
                  })}
                  renderIcon={View}
                  onClick={() => handleViewSavedDocument(doc)}
                />
                <Button
                  kind="ghost"
                  size="sm"
                  hasIconOnly
                  iconDescription={intl.formatMessage({
                    id: "patient.idDoc.edit",
                  })}
                  renderIcon={Edit}
                  onClick={() => handleEditDocument(doc)}
                />
                <Button
                  kind="danger--ghost"
                  size="sm"
                  hasIconOnly
                  iconDescription={intl.formatMessage({
                    id: "patient.idDoc.delete",
                  })}
                  renderIcon={TrashCan}
                  onClick={() => handleDeleteDocument(doc)}
                />
              </div>
            )}
          </Tile>
        ))}

        {/* Pending (unsaved) documents */}
        {pendingDocuments.map((doc, index) => (
          <Tile key={`pending-${index}`} className="id-document-card pending">
            <div
              className="id-document-thumbnail"
              onClick={() => handleViewPendingDocument(doc)}
            >
              {isPdfData(doc.data) ? (
                <div className="id-doc-pdf-thumb">
                  <DocumentPdf size={40} />
                  <span>PDF</span>
                </div>
              ) : (
                <img
                  src={doc.data}
                  alt={getCategoryLabel(doc.category)}
                  className="id-document-image"
                />
              )}
            </div>
            <div className="id-document-info">
              <Tag type="green" size="sm">
                {getCategoryLabel(doc.category)}
              </Tag>
              <Tag type="outline" size="sm">
                {intl.formatMessage({ id: "patient.idDoc.pending" })}
              </Tag>
              {doc.description && (
                <span className="id-document-description">
                  {doc.description}
                </span>
              )}
            </div>
            {!disabled && (
              <div className="id-document-actions">
                <Button
                  kind="ghost"
                  size="sm"
                  hasIconOnly
                  iconDescription={intl.formatMessage({
                    id: "patient.idDoc.view",
                  })}
                  renderIcon={View}
                  onClick={() => handleViewPendingDocument(doc)}
                />
                <Button
                  kind="danger--ghost"
                  size="sm"
                  hasIconOnly
                  iconDescription={intl.formatMessage({
                    id: "patient.idDoc.remove",
                  })}
                  renderIcon={TrashCan}
                  onClick={() => handleRemovePending(index)}
                />
              </div>
            )}
          </Tile>
        ))}

        {/* Empty state */}
        {savedDocuments.length === 0 && pendingDocuments.length === 0 && (
          <div className="id-documents-empty">
            <DocumentBlank size={32} />
            <p>{intl.formatMessage({ id: "patient.idDoc.empty" })}</p>
          </div>
        )}
      </div>

      {/* View Modal - supports both images and PDFs */}
      <Modal
        open={isViewModalOpen}
        onRequestClose={() => {
          setIsViewModalOpen(false);
          setViewDocUrl(null);
          setViewDocType(null);
        }}
        modalHeading={intl.formatMessage({ id: "patient.idDoc.viewDocument" })}
        passiveModal
        size="lg"
      >
        {viewDocUrl && (
          <div className="id-document-view-container">
            {viewDocType === "pdf" ? (
              <iframe
                src={viewDocUrl}
                title={intl.formatMessage({
                  id: "patient.idDoc.viewDocument",
                })}
                className="id-document-view-pdf"
              />
            ) : (
              <img
                src={viewDocUrl}
                alt={intl.formatMessage({
                  id: "patient.idDoc.viewDocument",
                })}
                className="id-document-view-image"
              />
            )}
          </div>
        )}
      </Modal>

      {/* Edit Modal */}
      <Modal
        open={isEditModalOpen}
        onRequestClose={() => {
          setIsEditModalOpen(false);
          setSelectedDoc(null);
          setNewDocData(null);
          setNewDocPreview(null);
          setNewDocIsPdf(false);
          setFileError("");
        }}
        modalHeading={intl.formatMessage({
          id: "patient.idDoc.editDocument",
        })}
        primaryButtonText={intl.formatMessage({ id: "label.button.save" })}
        secondaryButtonText={intl.formatMessage({ id: "patient.photo.cancel" })}
        onRequestSubmit={handleSaveEdit}
        size="md"
      >
        <div className="id-documents-upload-form">
          <Select
            id="edit-doc-category"
            labelText={intl.formatMessage({
              id: "patient.idDoc.documentType",
            })}
            value={editCategory}
            onChange={(e) => setEditCategory(e.target.value)}
          >
            {DOCUMENT_CATEGORIES.map((cat) => (
              <SelectItem
                key={cat.value}
                value={cat.value}
                text={intl.formatMessage({ id: cat.labelId })}
              />
            ))}
          </Select>
          <TextArea
            id="edit-doc-description"
            labelText={intl.formatMessage({
              id: "patient.idDoc.description",
            })}
            value={editDescription}
            onChange={(e) => setEditDescription(e.target.value)}
            rows={2}
            maxCount={255}
          />

          {/* Replacement image picker. Shows the current document's thumbnail
              until the user drops/selects/captures a new file, then shows the
              chosen replacement. Saving with no replacement keeps the existing
              image (back-compat with metadata-only edit). */}
          {!newDocData ? (
            <div className="id-doc-upload-area">
              <div
                className={`id-doc-dropzone ${isDragging ? "id-doc-dropzone-active" : ""}`}
                onDragOver={handleDragOver}
                onDragLeave={handleDragLeave}
                onDrop={handleDrop}
                onClick={() => fileInputRef.current?.click()}
              >
                {selectedDoc &&
                !isPdfData(selectedDoc.thumbnail) &&
                selectedDoc.thumbnail ? (
                  <img
                    src={selectedDoc.thumbnail}
                    alt={intl.formatMessage({
                      id: "patient.photo.preview.alt",
                    })}
                    className="id-doc-preview-image"
                  />
                ) : (
                  <CloudUpload size={48} className="id-doc-dropzone-icon" />
                )}
                <p className="id-doc-dropzone-title">
                  {intl.formatMessage({ id: "patient.idDoc.dragndrop" })}
                </p>
                <p className="id-doc-dropzone-subtitle">
                  {intl.formatMessage({ id: "patient.idDoc.browse" })}
                </p>
                <p className="id-doc-dropzone-formats">
                  {intl.formatMessage({ id: "patient.idDoc.formats" })}
                </p>
              </div>
              <Button
                kind="tertiary"
                size="sm"
                renderIcon={Camera}
                onClick={() => setIsCameraModalOpen(true)}
                className="id-doc-camera-btn"
              >
                {intl.formatMessage({ id: "patient.idDoc.useCamera" })}
              </Button>
            </div>
          ) : (
            <div className="id-doc-file-preview">
              {newDocIsPdf ? (
                <div className="id-doc-pdf-indicator">
                  <DocumentPdf size={48} />
                  <span>
                    {intl.formatMessage({ id: "patient.idDoc.pdfSelected" })}
                  </span>
                </div>
              ) : (
                <img
                  src={newDocPreview}
                  alt={intl.formatMessage({
                    id: "patient.photo.preview.alt",
                  })}
                  className="id-doc-preview-image"
                />
              )}
              <Button
                kind="tertiary"
                size="sm"
                onClick={() => {
                  setNewDocData(null);
                  setNewDocPreview(null);
                  setNewDocIsPdf(false);
                  setFileError("");
                }}
              >
                {intl.formatMessage({ id: "patient.photo.change" })}
              </Button>
            </div>
          )}

          {fileError && <p className="id-doc-error">{fileError}</p>}
        </div>
      </Modal>

      {/* Delete Confirmation Modal */}
      <Modal
        open={isDeleteModalOpen}
        onRequestClose={() => {
          setIsDeleteModalOpen(false);
          setSelectedDoc(null);
        }}
        modalHeading={intl.formatMessage({
          id: "patient.idDoc.confirmDelete",
        })}
        primaryButtonText={intl.formatMessage({
          id: "patient.idDoc.delete",
        })}
        secondaryButtonText={intl.formatMessage({ id: "patient.photo.cancel" })}
        onRequestSubmit={handleConfirmDelete}
        danger
        size="sm"
      >
        <p>{intl.formatMessage({ id: "patient.idDoc.deleteWarning" })}</p>
      </Modal>
    </div>
  );
};

export default IdentificationDocuments;
