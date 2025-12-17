import React, { useState, useEffect, useRef, useCallback } from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  InlineNotification,
  Loading,
  Modal,
  TextInput,
  TextArea,
  Select,
  SelectItem,
  DatePicker,
  DatePickerInput,
  FilterableMultiSelect,
  Checkbox,
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableSelectRow,
  TableSelectAll,
  TableContainer,
  Tag,
} from "@carbon/react";
import {
  Checkmark,
  Edit,
  Report,
  SendAlt,
  DocumentPdf,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
} from "../../../utils/Utils";
import "../../workflow/NotebookWorkflow.css";

/**
 * AnalyticsResultReviewPage - Page 4 of the Analytics Laboratory workflow.
 * Purpose: Validate and release analytical results.
 *
 * Data Points:
 * - Review & Validation: Reviewed by, Review date, Compliance, Comments
 * - Reporting: Report generated, Version, Released to
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - The notebook page data
 * @param {Object} props.progress - Page progress
 * @param {function} props.onProgressUpdate - Callback when progress changes
 * @param {number} props.notebookId - The notebook ID
 */
function AnalyticsResultReviewPage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
  notebookId,
}) {
  const intl = useIntl();
  const componentMounted = useRef(false);

  // State for samples
  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Modal state for review
  const [reviewModalOpen, setReviewModalOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Form state for review and release
  const [formData, setFormData] = useState({
    // Review & Validation
    reviewedBy: "",
    reviewDate: "",
    complianceWithSpec: "",
    comments: "",
    // Reporting
    reportGenerated: false,
    reportVersion: "",
    releasedTo: [],
  });

  // Options for dropdowns
  const complianceOptions = [
    { value: "PASS", label: "Pass" },
    { value: "FAIL", label: "Fail" },
  ];

  const releaseRecipientOptions = [
    { id: "REQUESTING_UNIT", label: "Requesting unit" },
    { id: "RESEARCHER", label: "Researcher" },
    { id: "CLIENT", label: "Client" },
  ];

  // Load samples for this page
  useEffect(() => {
    componentMounted.current = true;
    loadPageSamples();

    return () => {
      componentMounted.current = false;
    };
  }, [entryId, pageData?.id]);

  const loadPageSamples = useCallback(() => {
    if (!pageData?.id) {
      setLoading(false);
      return;
    }

    if (String(pageData.id).startsWith("default-")) {
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);

    getFromOpenElisServer(
      `/rest/notebook/page/${pageData.id}/samples`,
      (response) => {
        if (componentMounted.current) {
          if (response && Array.isArray(response)) {
            const transformedSamples = response.map((sample) => ({
              id: String(sample.id || sample.sampleItemId),
              externalId: sample.externalId,
              accessionNumber: sample.accessionNumber,
              sampleType: sample.sampleType || sample.typeOfSample?.description,
              status: sample.pageStatus || "PENDING",
              // Review fields
              reviewedBy: sample.data?.reviewedBy,
              reviewDate: sample.data?.reviewDate,
              complianceWithSpec: sample.data?.complianceWithSpec,
              reportGenerated: sample.data?.reportGenerated,
              releasedTo: sample.data?.releasedTo || [],
              // From previous page
              analystPerforming: sample.data?.analystPerforming,
            }));
            setSamples(transformedSamples);
          } else {
            setSamples([]);
          }
          setLoading(false);
        }
      },
    );
  }, [pageData?.id]);

  // Handle form input changes
  const handleFormChange = (field, value) => {
    setFormData((prev) => ({
      ...prev,
      [field]: value,
    }));
  };

  // Handle multi-select change for release recipients
  const handleReleaseToChange = ({ selectedItems }) => {
    setFormData((prev) => ({
      ...prev,
      releasedTo: selectedItems,
    }));
  };

  // Reset form
  const resetForm = () => {
    setFormData({
      reviewedBy: "",
      reviewDate: "",
      complianceWithSpec: "",
      comments: "",
      reportGenerated: false,
      reportVersion: "",
      releasedTo: [],
    });
  };

  // Submit review and release data
  const handleSubmitReview = async () => {
    if (!entryId || !pageData?.id) {
      setError("Cannot save: Page not properly initialized.");
      return;
    }

    if (selectedSampleIds.length === 0) {
      setError("Please select at least one sample.");
      return;
    }

    // Validate required fields
    if (
      !formData.reviewedBy ||
      !formData.reviewDate ||
      !formData.complianceWithSpec
    ) {
      setError("Please fill in all required review fields.");
      return;
    }

    if (formData.releasedTo.length === 0) {
      setError("Please select at least one release recipient.");
      return;
    }

    setIsSubmitting(true);
    setError(null);

    const reviewData = {
      sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
      reviewedBy: formData.reviewedBy,
      reviewDate: formData.reviewDate,
      complianceWithSpec: formData.complianceWithSpec,
      comments: formData.comments,
      reportGenerated: formData.reportGenerated,
      reportVersion: formData.reportVersion,
      releasedTo: formData.releasedTo.map((r) => r.id),
      status: "RESULTS_RELEASED",
    };

    postToOpenElisServer(
      `/rest/notebook/analytics/entry/${entryId}/page/${pageData.id}/samples/review`,
      JSON.stringify(reviewData),
      (status, response) => {
        setIsSubmitting(false);
        if (status === 200 || status === 201) {
          setReviewModalOpen(false);
          resetForm();
          setSelectedSampleIds([]);
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(
            response?.error || "Failed to save review data. Please try again.",
          );
        }
      },
    );
  };

  // Handle bulk mark as released
  const handleBulkMarkReleased = useCallback(() => {
    if (selectedSampleIds.length === 0) return;

    const hasRealPageId =
      pageData?.id && !String(pageData.id).startsWith("default-");
    if (!hasRealPageId) {
      setError("Cannot update samples: Page not properly initialized.");
      return;
    }

    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
      JSON.stringify({
        sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
        status: "COMPLETED",
      }),
      (status) => {
        if (status === 200) {
          loadPageSamples();
          setSelectedSampleIds([]);
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError("Failed to update sample status.");
        }
      },
    );
  }, [selectedSampleIds, pageData?.id, loadPageSamples, onProgressUpdate]);

  // Calculate stats
  const releasedCount = samples.filter(
    (s) => s.status === "COMPLETED" || s.status === "RESULTS_RELEASED",
  ).length;
  const pendingReviewCount = samples.filter(
    (s) => s.status === "PENDING",
  ).length;
  const reviewedCount = samples.filter((s) => s.status === "REVIEWED").length;

  // Table headers
  const tableHeaders = [
    { key: "externalId", header: "Sample Identifier" },
    { key: "sampleType", header: "Sample Type" },
    { key: "analystPerforming", header: "Analyst" },
    { key: "reviewedBy", header: "Reviewed By" },
    { key: "reviewDate", header: "Review Date" },
    { key: "complianceWithSpec", header: "Compliance" },
    { key: "status", header: "Status" },
  ];

  // Get compliance label
  const getComplianceLabel = (value) => {
    const option = complianceOptions.find((o) => o.value === value);
    return option ? option.label : value;
  };

  // Get release recipients label
  const getReleaseRecipientsLabel = (recipients) => {
    if (!recipients || !Array.isArray(recipients)) return "";
    return recipients
      .map((r) => {
        const option = releaseRecipientOptions.find((o) => o.id === r);
        return option ? option.label : r;
      })
      .join(", ");
  };

  return (
    <div className="analytics-result-review-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.page.analytics.resultReview.title"
            defaultMessage="Result Review, Reporting & Release"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.analytics.resultReview.description"
            defaultMessage="Validate and release analytical results. Review compliance, generate reports, and release to requesting parties."
          />
        </p>
      </div>

      {/* Progress Summary */}
      <Grid fullWidth className="progress-section">
        <Column lg={16} md={8} sm={4}>
          <div className="progress-tiles">
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.analytics.totalSamples"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{samples.length}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.analytics.resultsReleased"
                  defaultMessage="Results Released"
                />
              </span>
              <span className="progress-value">{releasedCount}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.analytics.pendingReview"
                  defaultMessage="Pending Review"
                />
              </span>
              <span className="progress-value">{pendingReviewCount}</span>
            </Tile>
          </div>
        </Column>
      </Grid>

      {/* Action Buttons */}
      <div className="page-actions-bar">
        {selectedSampleIds.length > 0 && (
          <>
            <Button
              kind="primary"
              size="sm"
              renderIcon={Report}
              onClick={() => setReviewModalOpen(true)}
            >
              <FormattedMessage
                id="notebook.page.analytics.reviewAndRelease"
                defaultMessage="Review & Release ({count})"
                values={{ count: selectedSampleIds.length }}
              />
            </Button>

            <Button
              kind="secondary"
              size="sm"
              renderIcon={Checkmark}
              onClick={handleBulkMarkReleased}
            >
              <FormattedMessage
                id="notebook.page.analytics.markReleased"
                defaultMessage="Mark as Released ({count})"
                values={{ count: selectedSampleIds.length }}
              />
            </Button>
          </>
        )}
      </div>

      {/* Error Display */}
      {error && (
        <InlineNotification
          kind="error"
          title={error}
          onCloseButtonClick={() => setError(null)}
          lowContrast
        />
      )}

      {/* Samples Table */}
      <div className="sample-grid-container">
        {loading ? (
          <Loading description="Loading samples..." withOverlay={false} />
        ) : samples.length === 0 ? (
          <div className="empty-state">
            <p>
              <FormattedMessage
                id="notebook.page.analytics.resultReview.empty"
                defaultMessage="No samples available for review. Analysis must be completed in the Test Execution page first."
              />
            </p>
          </div>
        ) : (
          <DataTable rows={samples} headers={tableHeaders} isSortable>
            {({
              rows,
              headers,
              getTableProps,
              getHeaderProps,
              getRowProps,
              getSelectionProps,
            }) => (
              <TableContainer>
                <Table {...getTableProps()}>
                  <TableHead>
                    <TableRow>
                      <TableSelectAll
                        {...getSelectionProps()}
                        onSelect={() => {
                          if (selectedSampleIds.length === samples.length) {
                            setSelectedSampleIds([]);
                          } else {
                            setSelectedSampleIds(samples.map((s) => s.id));
                          }
                        }}
                        checked={selectedSampleIds.length === samples.length}
                        indeterminate={
                          selectedSampleIds.length > 0 &&
                          selectedSampleIds.length < samples.length
                        }
                      />
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
                    {rows.map((row) => (
                      <TableRow key={row.id} {...getRowProps({ row })}>
                        <TableSelectRow
                          {...getSelectionProps({ row })}
                          checked={selectedSampleIds.includes(row.id)}
                          onSelect={() => {
                            if (selectedSampleIds.includes(row.id)) {
                              setSelectedSampleIds(
                                selectedSampleIds.filter((id) => id !== row.id),
                              );
                            } else {
                              setSelectedSampleIds([
                                ...selectedSampleIds,
                                row.id,
                              ]);
                            }
                          }}
                        />
                        {row.cells.map((cell) => (
                          <TableCell key={cell.id}>
                            {cell.info.header === "status" ? (
                              <Tag
                                type={
                                  cell.value === "COMPLETED" ||
                                  cell.value === "RESULTS_RELEASED"
                                    ? "green"
                                    : cell.value === "PENDING"
                                      ? "gray"
                                      : cell.value === "REVIEWED"
                                        ? "blue"
                                        : "purple"
                                }
                                size="sm"
                              >
                                {cell.value === "RESULTS_RELEASED"
                                  ? "Released"
                                  : cell.value}
                              </Tag>
                            ) : cell.info.header === "complianceWithSpec" ? (
                              cell.value ? (
                                <Tag
                                  type={cell.value === "PASS" ? "green" : "red"}
                                  size="sm"
                                >
                                  {getComplianceLabel(cell.value)}
                                </Tag>
                              ) : (
                                "-"
                              )
                            ) : (
                              cell.value || "-"
                            )}
                          </TableCell>
                        ))}
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          </DataTable>
        )}
      </div>

      {/* Review and Release Modal */}
      <Modal
        open={reviewModalOpen}
        onRequestClose={() => {
          setReviewModalOpen(false);
          resetForm();
          setError(null);
        }}
        modalHeading={intl.formatMessage({
          id: "notebook.analytics.resultReview.title",
          defaultMessage: `Review & Release ${selectedSampleIds.length} Sample(s)`,
        })}
        primaryButtonText={intl.formatMessage({
          id: "label.releaseResults",
          defaultMessage: "Release Results",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestSubmit={handleSubmitReview}
        onSecondarySubmit={() => {
          setReviewModalOpen(false);
          resetForm();
          setError(null);
        }}
        size="md"
        primaryButtonDisabled={isSubmitting}
      >
        <Grid fullWidth>
          {/* Review & Validation Section */}
          <Column lg={16} md={8} sm={4}>
            <h5 style={{ marginBottom: "1rem", marginTop: "1rem" }}>
              <FormattedMessage
                id="notebook.analytics.section.reviewValidation"
                defaultMessage="Review & Validation"
              />
            </h5>
          </Column>

          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="reviewedBy"
              labelText={intl.formatMessage({
                id: "notebook.analytics.field.reviewedBy",
                defaultMessage: "Reviewed by *",
              })}
              placeholder="Enter reviewer name"
              value={formData.reviewedBy}
              onChange={(e) => handleFormChange("reviewedBy", e.target.value)}
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <DatePicker
              datePickerType="single"
              onChange={(dates) => {
                if (dates && dates[0]) {
                  const date = dates[0];
                  const formatted = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
                  handleFormChange("reviewDate", formatted);
                }
              }}
            >
              <DatePickerInput
                id="reviewDate"
                labelText={intl.formatMessage({
                  id: "notebook.analytics.field.reviewDate",
                  defaultMessage: "Review date *",
                })}
                placeholder="yyyy-mm-dd"
              />
            </DatePicker>
          </Column>

          <Column lg={8} md={4} sm={4}>
            <Select
              id="complianceWithSpec"
              labelText={intl.formatMessage({
                id: "notebook.analytics.field.complianceWithSpec",
                defaultMessage: "Compliance with specification *",
              })}
              value={formData.complianceWithSpec}
              onChange={(e) =>
                handleFormChange("complianceWithSpec", e.target.value)
              }
            >
              <SelectItem value="" text="Select compliance..." />
              {complianceOptions.map((option) => (
                <SelectItem
                  key={option.value}
                  value={option.value}
                  text={option.label}
                />
              ))}
            </Select>
          </Column>

          <Column lg={16} md={8} sm={4}>
            <TextArea
              id="comments"
              labelText={intl.formatMessage({
                id: "notebook.analytics.field.comments",
                defaultMessage: "Comments",
              })}
              placeholder="Enter any review comments or observations"
              value={formData.comments}
              onChange={(e) => handleFormChange("comments", e.target.value)}
              rows={3}
            />
          </Column>

          {/* Reporting Section */}
          <Column lg={16} md={8} sm={4}>
            <h5 style={{ marginBottom: "1rem", marginTop: "1.5rem" }}>
              <FormattedMessage
                id="notebook.analytics.section.reporting"
                defaultMessage="Reporting"
              />
            </h5>
          </Column>

          <Column lg={8} md={4} sm={4}>
            <Checkbox
              id="reportGenerated"
              labelText="Analytical Result Report generated"
              checked={formData.reportGenerated}
              onChange={(e) =>
                handleFormChange("reportGenerated", e.target.checked)
              }
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="reportVersion"
              labelText={intl.formatMessage({
                id: "notebook.analytics.field.reportVersion",
                defaultMessage: "Report version",
              })}
              placeholder="e.g., 1.0, 2.1"
              value={formData.reportVersion}
              onChange={(e) =>
                handleFormChange("reportVersion", e.target.value)
              }
              disabled={!formData.reportGenerated}
            />
          </Column>

          <Column lg={16} md={8} sm={4}>
            <FilterableMultiSelect
              id="releasedTo"
              titleText={intl.formatMessage({
                id: "notebook.analytics.field.releasedTo",
                defaultMessage: "Released to *",
              })}
              items={releaseRecipientOptions}
              itemToString={(item) => (item ? item.label : "")}
              selectedItems={formData.releasedTo}
              onChange={handleReleaseToChange}
              placeholder="Select release recipients..."
            />
          </Column>
        </Grid>
      </Modal>
    </div>
  );
}

export default AnalyticsResultReviewPage;
