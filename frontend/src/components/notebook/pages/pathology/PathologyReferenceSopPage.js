import React, { useState, useEffect, useRef, useCallback } from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  Tag,
  InlineNotification,
  Loading,
  TextInput,
  TextArea,
  Select,
  SelectItem,
  DatePicker,
  DatePickerInput,
  Modal,
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  FileUploaderDropContainer,
  FileUploaderItem,
} from "@carbon/react";
import { DocumentAdd, View, Download, Edit } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
  toBase64,
} from "../../../utils/Utils";
import "../../workflow/NotebookWorkflow.css";

/**
 * PathologyReferenceSopPage - Page 8 of the pathology workflow.
 * Purpose: Provide controlled access to SOPs and protocols.
 * Who uses it: All users (view), Lab managers/supervisors (upload/edit)
 */
function PathologyReferenceSopPage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
}) {
  const intl = useIntl();
  const componentMounted = useRef(false);

  const [sops, setSops] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [uploadModalOpen, setUploadModalOpen] = useState(false);
  const [viewModalOpen, setViewModalOpen] = useState(false);
  const [selectedSop, setSelectedSop] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [uploadedFiles, setUploadedFiles] = useState([]);

  const [sopData, setSopData] = useState({
    sopTitle: "",
    sopCategory: "",
    version: "",
    effectiveDate: "",
    reviewDate: "",
    previousVersion: "",
    changesSummary: "",
    approvedBy: "",
    approvalDate: "",
    sopDocument: null,
  });

  useEffect(() => {
    componentMounted.current = true;
    loadSops();
    return () => {
      componentMounted.current = false;
    };
  }, [entryId, pageData?.id]);

  const loadSops = useCallback(() => {
    setLoading(true);
    getFromOpenElisServer(
      `/rest/notebook/pathology/sops?entryId=${entryId}`,
      (response) => {
        if (componentMounted.current) {
          if (response && Array.isArray(response)) {
            setSops(response);
          } else {
            setSops([]);
          }
          setLoading(false);
        }
      },
    );
  }, [entryId]);

  // Function to view/preview a document in a new window
  const handleViewDocument = (sop) => {
    if (sop.fileData && sop.fileType) {
      const win = window.open();
      win.document.write(
        '<iframe src="data:' +
          sop.fileType +
          ";base64," +
          sop.fileData +
          '" frameborder="0" style="border:0; top:0px; left:0px; bottom:0px; right:0px; width:100%; height:100%;" allowfullscreen></iframe>',
      );
    }
  };

  // Function to download a document
  const handleDownloadDocument = (sop) => {
    if (sop.fileData && sop.fileType && sop.fileName) {
      // Create a download link
      const link = document.createElement("a");
      link.href = "data:" + sop.fileType + ";base64," + sop.fileData;
      link.download = sop.fileName;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
    }
  };

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setSopData((prev) => ({ ...prev, [name]: value }));
  };

  const handleDateChange = (dates, fieldName) => {
    if (dates?.[0]) {
      setSopData((prev) => ({
        ...prev,
        [fieldName]: dates[0].toISOString().split("T")[0],
      }));
    }
  };

  const handleAddFiles = async (event) => {
    const newFiles = Array.from(event.target.files);
    const fileForms = await Promise.all(
      newFiles.map(async (file) => {
        const base64 = await toBase64(file);
        return {
          base64File: base64,
          fileType: file.type,
          fileName: file.name,
        };
      }),
    );
    setSopData((prev) => ({ ...prev, sopDocument: fileForms[0] }));
    setUploadedFiles(newFiles.map((f) => ({ file: f, status: "complete" })));
  };

  const handleRemoveFile = () => {
    setSopData((prev) => ({ ...prev, sopDocument: null }));
    setUploadedFiles([]);
  };

  const openUploadModal = () => {
    setSopData({
      sopTitle: "",
      sopCategory: "",
      version: "",
      effectiveDate: "",
      reviewDate: "",
      previousVersion: "",
      changesSummary: "",
      approvedBy: "",
      approvalDate: "",
      sopDocument: null,
    });
    setUploadedFiles([]);
    setUploadModalOpen(true);
  };

  const openViewModal = (sop) => {
    setSelectedSop(sop);
    setViewModalOpen(true);
  };

  const handleUploadSop = () => {
    if (submitting) return;
    if (!sopData.sopTitle || !sopData.sopCategory) {
      setError("Please fill in SOP Title and Category");
      return;
    }

    setSubmitting(true);
    postToOpenElisServer(
      `/rest/notebook/pathology/sop/upload`,
      JSON.stringify({ entryId, pageId: pageData?.id, ...sopData }),
      (status) => {
        setSubmitting(false);
        if (status === 200) {
          setUploadModalOpen(false);
          loadSops();
          onProgressUpdate?.();
        } else {
          setError("Failed to upload SOP. Please try again.");
        }
      },
    );
  };

  const headers = [
    { key: "sopTitle", header: "SOP Title" },
    { key: "sopCategory", header: "Category" },
    { key: "version", header: "Version" },
    { key: "effectiveDate", header: "Effective Date" },
    { key: "status", header: "Status" },
    { key: "actions", header: "Actions" },
  ];

  const sopCategoryOptions = [
    { id: "sample_reception", text: "Sample Reception" },
    { id: "quality_control", text: "Quality Control" },
    { id: "processing", text: "Processing" },
    { id: "testing", text: "Testing" },
    { id: "storage", text: "Storage" },
    { id: "disposal", text: "Disposal" },
    { id: "safety", text: "Safety" },
    { id: "equipment", text: "Equipment" },
    { id: "other", text: "Other" },
  ];

  return (
    <div className="pathology-sop-page">
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="pathology.page.sop.title"
            defaultMessage="Reference & SOP Module"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="pathology.page.sop.description"
            defaultMessage="Provide controlled access to SOPs and protocols. View, upload, and manage standard operating procedures."
          />
        </p>
      </div>

      {/* Access Level Info */}
      <Grid fullWidth className="progress-section">
        <Column lg={16} md={8} sm={4}>
          <div className="progress-tiles">
            <Tile className="progress-tile">
              <span className="progress-label">Total SOPs</span>
              <span className="progress-value">{sops.length}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">View Access</span>
              <span className="progress-value">All Users</span>
            </Tile>
            <Tile
              className="progress-tile"
              style={{ backgroundColor: "#e0f0ff" }}
            >
              <span className="progress-label">Edit Access</span>
              <span className="progress-value">Managers Only</span>
            </Tile>
          </div>
        </Column>
      </Grid>

      <div className="page-actions-bar">
        <Button
          kind="primary"
          size="sm"
          renderIcon={DocumentAdd}
          onClick={openUploadModal}
        >
          <FormattedMessage
            id="pathology.sop.uploadNew"
            defaultMessage="Upload New SOP"
          />
        </Button>
      </div>

      {error && (
        <InlineNotification
          kind="error"
          title={error}
          onCloseButtonClick={() => setError(null)}
          lowContrast
        />
      )}

      <div className="sample-grid-container">
        {loading ? (
          <Loading withOverlay={false} description="Loading SOPs..." />
        ) : sops.length === 0 ? (
          <div className="empty-state">
            <p>
              No SOPs have been uploaded yet. Click 'Upload New SOP' to add one.
            </p>
          </div>
        ) : (
          <DataTable
            rows={sops}
            headers={headers}
            isSortable
            render={({
              rows,
              headers,
              getTableProps,
              getHeaderProps,
              getRowProps,
            }) => (
              <TableContainer>
                <Table {...getTableProps()}>
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
                    {rows.map((row) => {
                      const sop = sops.find((s) => s.id === row.id);
                      return (
                        <TableRow key={row.id} {...getRowProps({ row })}>
                          {row.cells.map((cell) => (
                            <TableCell key={cell.id}>
                              {cell.info.header === "status" ? (
                                <Tag
                                  type={
                                    cell.value === "Active" ? "green" : "gray"
                                  }
                                  size="sm"
                                >
                                  {cell.value}
                                </Tag>
                              ) : cell.info.header === "actions" ? (
                                <div style={{ display: "flex", gap: "0.5rem" }}>
                                  <Button
                                    kind="ghost"
                                    size="sm"
                                    hasIconOnly
                                    renderIcon={View}
                                    iconDescription="View"
                                    onClick={() => openViewModal(sop)}
                                  />
                                  <Button
                                    kind="ghost"
                                    size="sm"
                                    hasIconOnly
                                    renderIcon={Download}
                                    iconDescription="Download"
                                    disabled={!sop?.fileData}
                                    onClick={() => handleDownloadDocument(sop)}
                                  />
                                </div>
                              ) : (
                                cell.value
                              )}
                            </TableCell>
                          ))}
                        </TableRow>
                      );
                    })}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          />
        )}
      </div>

      {/* Upload SOP Modal */}
      <Modal
        open={uploadModalOpen}
        modalHeading="Upload New SOP"
        primaryButtonText="Upload"
        secondaryButtonText="Cancel"
        onRequestClose={() => setUploadModalOpen(false)}
        onRequestSubmit={handleUploadSop}
        primaryButtonDisabled={submitting}
        size="lg"
      >
        <Grid fullWidth>
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="sopTitle"
              name="sopTitle"
              labelText="SOP Title *"
              value={sopData.sopTitle}
              onChange={handleInputChange}
            />
          </Column>
          <Column lg={8} md={4} sm={4}>
            <Select
              id="sopCategory"
              name="sopCategory"
              labelText="SOP Category *"
              value={sopData.sopCategory}
              onChange={handleInputChange}
            >
              <SelectItem value="" text="" />
              {sopCategoryOptions.map((opt) => (
                <SelectItem key={opt.id} value={opt.id} text={opt.text} />
              ))}
            </Select>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="version"
              name="version"
              labelText="Version"
              value={sopData.version}
              onChange={handleInputChange}
            />
          </Column>
          <Column lg={8} md={4} sm={4}>
            <DatePicker
              datePickerType="single"
              onChange={(dates) => handleDateChange(dates, "effectiveDate")}
            >
              <DatePickerInput
                id="effectiveDate"
                labelText="Effective Date"
                placeholder="mm/dd/yyyy"
              />
            </DatePicker>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <DatePicker
              datePickerType="single"
              onChange={(dates) => handleDateChange(dates, "reviewDate")}
            >
              <DatePickerInput
                id="reviewDate"
                labelText="Review Date"
                placeholder="mm/dd/yyyy"
              />
            </DatePicker>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="approvedBy"
              name="approvedBy"
              labelText="Approved By"
              value={sopData.approvedBy}
              onChange={handleInputChange}
            />
          </Column>
          <Column lg={16} md={8} sm={4}>
            <h5 style={{ marginTop: "1.5rem", marginBottom: "1rem" }}>
              Version Control
            </h5>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="previousVersion"
              name="previousVersion"
              labelText="Previous Version"
              value={sopData.previousVersion}
              onChange={handleInputChange}
            />
          </Column>
          <Column lg={16} md={8} sm={4}>
            <TextArea
              id="changesSummary"
              name="changesSummary"
              labelText="Summary of Changes"
              value={sopData.changesSummary}
              onChange={handleInputChange}
              rows={3}
            />
          </Column>
          <Column lg={16} md={8} sm={4}>
            <h5 style={{ marginTop: "1.5rem", marginBottom: "1rem" }}>
              Document Upload
            </h5>
            <FileUploaderDropContainer
              labelText="Drag and drop files here or click to upload"
              multiple={false}
              onAddFiles={handleAddFiles}
              accept={[".pdf", ".doc", ".docx"]}
            />
            {uploadedFiles.map((fileObj, index) => (
              <FileUploaderItem
                key={index}
                name={fileObj.file.name}
                status={fileObj.status}
                onDelete={handleRemoveFile}
              />
            ))}
          </Column>
        </Grid>
      </Modal>

      {/* View SOP Modal */}
      <Modal
        open={viewModalOpen}
        modalHeading={selectedSop?.sopTitle || "SOP Details"}
        primaryButtonText="Close"
        onRequestClose={() => setViewModalOpen(false)}
        onRequestSubmit={() => setViewModalOpen(false)}
        size="lg"
      >
        {selectedSop && (
          <Grid fullWidth>
            <Column lg={8} md={4} sm={4}>
              <p>
                <strong>Category:</strong> {selectedSop.sopCategory}
              </p>
            </Column>
            <Column lg={8} md={4} sm={4}>
              <p>
                <strong>Version:</strong> {selectedSop.version}
              </p>
            </Column>
            <Column lg={8} md={4} sm={4}>
              <p>
                <strong>Effective Date:</strong> {selectedSop.effectiveDate}
              </p>
            </Column>
            <Column lg={8} md={4} sm={4}>
              <p>
                <strong>Status:</strong>{" "}
                <Tag
                  type={selectedSop.status === "Active" ? "green" : "gray"}
                  size="sm"
                >
                  {selectedSop.status}
                </Tag>
              </p>
            </Column>
            {selectedSop.reviewDate && (
              <Column lg={8} md={4} sm={4}>
                <p>
                  <strong>Review Date:</strong> {selectedSop.reviewDate}
                </p>
              </Column>
            )}
            {selectedSop.approvedBy && (
              <Column lg={8} md={4} sm={4}>
                <p>
                  <strong>Approved By:</strong> {selectedSop.approvedBy}
                </p>
              </Column>
            )}
            {selectedSop.previousVersion && (
              <Column lg={8} md={4} sm={4}>
                <p>
                  <strong>Previous Version:</strong>{" "}
                  {selectedSop.previousVersion}
                </p>
              </Column>
            )}
            {selectedSop.changesSummary && (
              <Column lg={16} md={8} sm={4}>
                <p style={{ marginTop: "1rem" }}>
                  <strong>Summary of Changes:</strong>
                </p>
                <p>{selectedSop.changesSummary}</p>
              </Column>
            )}
            <Column lg={16} md={8} sm={4}>
              <Tile
                style={{
                  marginTop: "1rem",
                  padding: "2rem",
                  textAlign: "center",
                }}
              >
                {selectedSop.fileData ? (
                  <>
                    <p>
                      <strong>Attached Document:</strong> {selectedSop.fileName}
                    </p>
                    <p style={{ color: "#525252", marginBottom: "1rem" }}>
                      Type: {selectedSop.fileType}
                    </p>
                    <div
                      style={{
                        display: "flex",
                        gap: "1rem",
                        justifyContent: "center",
                      }}
                    >
                      <Button
                        kind="secondary"
                        size="sm"
                        renderIcon={View}
                        onClick={() => handleViewDocument(selectedSop)}
                      >
                        <FormattedMessage
                          id="pathology.label.view"
                          defaultMessage="View"
                        />
                      </Button>
                      <Button
                        kind="primary"
                        size="sm"
                        renderIcon={Download}
                        onClick={() => handleDownloadDocument(selectedSop)}
                      >
                        <FormattedMessage
                          id="pathology.sop.download"
                          defaultMessage="Download"
                        />
                      </Button>
                    </div>
                  </>
                ) : (
                  <p style={{ color: "#525252" }}>
                    <FormattedMessage
                      id="pathology.sop.noDocument"
                      defaultMessage="No document attached to this SOP."
                    />
                  </p>
                )}
              </Tile>
            </Column>
          </Grid>
        )}
      </Modal>
    </div>
  );
}

export default PathologyReferenceSopPage;
