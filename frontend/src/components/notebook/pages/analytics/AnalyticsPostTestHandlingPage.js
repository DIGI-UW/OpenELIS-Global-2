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
  Select,
  SelectItem,
  DatePicker,
  DatePickerInput,
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
  RadioButtonGroup,
  RadioButton,
} from "@carbon/react";
import { Checkmark, Archive, DataShare } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
} from "../../../utils/Utils";
import "../../workflow/NotebookWorkflow.css";

/**
 * AnalyticsPostTestHandlingPage - Page 5 of the Analytics Laboratory workflow.
 * Purpose: Manage retention, biorepository transfer, and long-term traceability.
 *
 * Data Points:
 * - Sample Retention: Required, Quantity, Duration
 * - Biorepository Transfer: Transfer, Date, Receiving lab, Condition
 * - Data Handling: Raw data archived, Reports archived, Retention period
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - The notebook page data
 * @param {Object} props.progress - Page progress
 * @param {function} props.onProgressUpdate - Callback when progress changes
 * @param {number} props.notebookId - The notebook ID
 */
function AnalyticsPostTestHandlingPage({
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

  // Modal state for handling
  const [handleModalOpen, setHandleModalOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Form state for post-test handling
  const [formData, setFormData] = useState({
    // Sample Retention
    retentionRequired: "",
    retentionQuantity: "",
    retentionDuration: "",
    // Biorepository Transfer
    transferRequired: "",
    transferDate: "",
    receivingLaboratory: "",
    storageCondition: "",
    // Data Handling
    rawDataArchived: false,
    finalReportsArchived: false,
    retentionPeriodLogged: false,
  });

  // Options for dropdowns
  const yesNoOptions = [
    { value: "YES", label: "Yes" },
    { value: "NO", label: "No" },
  ];

  const retentionDurationOptions = [
    { value: "STABILITY_STUDY", label: "Stability study" },
    { value: "LEGAL_HOLD", label: "Legal hold" },
    { value: "PROTOCOL_DEFINED", label: "Protocol-defined" },
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
              // Post-test handling fields
              retentionRequired: sample.data?.retentionRequired,
              retentionDuration: sample.data?.retentionDuration,
              transferRequired: sample.data?.transferRequired,
              receivingLaboratory: sample.data?.receivingLaboratory,
              rawDataArchived: sample.data?.rawDataArchived,
              finalReportsArchived: sample.data?.finalReportsArchived,
              lifecycleCompleted: sample.data?.lifecycleCompleted,
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

  // Reset form
  const resetForm = () => {
    setFormData({
      retentionRequired: "",
      retentionQuantity: "",
      retentionDuration: "",
      transferRequired: "",
      transferDate: "",
      receivingLaboratory: "",
      storageCondition: "",
      rawDataArchived: false,
      finalReportsArchived: false,
      retentionPeriodLogged: false,
    });
  };

  // Submit post-test handling data
  const handleSubmitHandling = async () => {
    if (!entryId || !pageData?.id) {
      setError("Cannot save: Page not properly initialized.");
      return;
    }

    if (selectedSampleIds.length === 0) {
      setError("Please select at least one sample.");
      return;
    }

    // Validate required fields
    if (!formData.retentionRequired || !formData.transferRequired) {
      setError("Please fill in retention and transfer requirements.");
      return;
    }

    // Validate data handling checkboxes
    if (
      !formData.rawDataArchived ||
      !formData.finalReportsArchived ||
      !formData.retentionPeriodLogged
    ) {
      setError("Please confirm all data handling items are completed.");
      return;
    }

    setIsSubmitting(true);
    setError(null);

    const handlingData = {
      sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
      // Sample Retention
      retentionRequired: formData.retentionRequired,
      retentionQuantity: formData.retentionQuantity,
      retentionDuration: formData.retentionDuration,
      // Biorepository Transfer
      transferRequired: formData.transferRequired,
      transferDate: formData.transferDate,
      receivingLaboratory: formData.receivingLaboratory,
      storageCondition: formData.storageCondition,
      // Data Handling
      rawDataArchived: formData.rawDataArchived,
      finalReportsArchived: formData.finalReportsArchived,
      retentionPeriodLogged: formData.retentionPeriodLogged,
      lifecycleCompleted: true,
      status: "LIFECYCLE_COMPLETED",
    };

    postToOpenElisServer(
      `/rest/notebook/analytics/entry/${entryId}/page/${pageData.id}/samples/finalize`,
      JSON.stringify(handlingData),
      (status, response) => {
        setIsSubmitting(false);
        if (status === 200 || status === 201) {
          setHandleModalOpen(false);
          resetForm();
          setSelectedSampleIds([]);
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(
            response?.error ||
              "Failed to save handling data. Please try again.",
          );
        }
      },
    );
  };

  // Handle bulk mark as completed
  const handleBulkMarkCompleted = useCallback(() => {
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
  const completedCount = samples.filter(
    (s) => s.status === "COMPLETED" || s.status === "LIFECYCLE_COMPLETED",
  ).length;
  const pendingCount = samples.filter((s) => s.status === "PENDING").length;
  const archivedCount = samples.filter(
    (s) => s.rawDataArchived && s.finalReportsArchived,
  ).length;

  // Table headers
  const tableHeaders = [
    { key: "externalId", header: "Sample Identifier" },
    { key: "sampleType", header: "Sample Type" },
    { key: "retentionRequired", header: "Retention" },
    { key: "retentionDuration", header: "Duration" },
    { key: "transferRequired", header: "Transfer" },
    { key: "receivingLaboratory", header: "Receiving Lab" },
    { key: "status", header: "Status" },
  ];

  // Get retention duration label
  const getRetentionDurationLabel = (value) => {
    const option = retentionDurationOptions.find((o) => o.value === value);
    return option ? option.label : value;
  };

  return (
    <div className="analytics-post-test-handling-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.page.analytics.postTestHandling.title"
            defaultMessage="Post-Test Sample & Data Handling"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.analytics.postTestHandling.description"
            defaultMessage="Manage retention, biorepository transfer, and long-term traceability. Archive data and finalize sample lifecycle."
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
                  id="notebook.page.analytics.lifecycleComplete"
                  defaultMessage="Lifecycle Complete"
                />
              </span>
              <span className="progress-value">{completedCount}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.analytics.pendingHandling"
                  defaultMessage="Pending Handling"
                />
              </span>
              <span className="progress-value">{pendingCount}</span>
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
              renderIcon={Archive}
              onClick={() => setHandleModalOpen(true)}
            >
              <FormattedMessage
                id="notebook.page.analytics.recordHandling"
                defaultMessage="Record Handling ({count})"
                values={{ count: selectedSampleIds.length }}
              />
            </Button>

            <Button
              kind="secondary"
              size="sm"
              renderIcon={Checkmark}
              onClick={handleBulkMarkCompleted}
            >
              <FormattedMessage
                id="notebook.page.analytics.markComplete"
                defaultMessage="Mark Complete ({count})"
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
                id="notebook.page.analytics.postTestHandling.empty"
                defaultMessage="No samples available for post-test handling. Results must be released in the Result Review page first."
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
                                  cell.value === "LIFECYCLE_COMPLETED"
                                    ? "green"
                                    : cell.value === "PENDING"
                                      ? "gray"
                                      : "blue"
                                }
                                size="sm"
                              >
                                {cell.value === "LIFECYCLE_COMPLETED"
                                  ? "Complete"
                                  : cell.value}
                              </Tag>
                            ) : cell.info.header === "retentionRequired" ||
                              cell.info.header === "transferRequired" ? (
                              cell.value ? (
                                <Tag
                                  type={cell.value === "YES" ? "blue" : "gray"}
                                  size="sm"
                                >
                                  {cell.value === "YES" ? "Yes" : "No"}
                                </Tag>
                              ) : (
                                "-"
                              )
                            ) : cell.info.header === "retentionDuration" ? (
                              getRetentionDurationLabel(cell.value) || "-"
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

      {/* Post-Test Handling Modal */}
      <Modal
        open={handleModalOpen}
        onRequestClose={() => {
          setHandleModalOpen(false);
          resetForm();
          setError(null);
        }}
        modalHeading={intl.formatMessage({
          id: "notebook.analytics.postTestHandling.title",
          defaultMessage: `Finalize ${selectedSampleIds.length} Sample(s)`,
        })}
        primaryButtonText={intl.formatMessage({
          id: "label.finalize",
          defaultMessage: "Finalize Lifecycle",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestSubmit={handleSubmitHandling}
        onSecondarySubmit={() => {
          setHandleModalOpen(false);
          resetForm();
          setError(null);
        }}
        size="lg"
        primaryButtonDisabled={isSubmitting}
      >
        <Grid fullWidth>
          {/* Sample Retention Section */}
          <Column lg={16} md={8} sm={4}>
            <h5 style={{ marginBottom: "1rem", marginTop: "1rem" }}>
              <FormattedMessage
                id="notebook.analytics.section.sampleRetention"
                defaultMessage="Sample Retention"
              />
            </h5>
          </Column>

          <Column lg={8} md={4} sm={4}>
            <RadioButtonGroup
              legendText={intl.formatMessage({
                id: "notebook.analytics.field.retentionRequired",
                defaultMessage: "Retention required *",
              })}
              name="retentionRequired"
              valueSelected={formData.retentionRequired}
              onChange={(value) => handleFormChange("retentionRequired", value)}
            >
              {yesNoOptions.map((option) => (
                <RadioButton
                  key={option.value}
                  id={`retention-${option.value}`}
                  labelText={option.label}
                  value={option.value}
                />
              ))}
            </RadioButtonGroup>
          </Column>

          {formData.retentionRequired === "YES" && (
            <>
              <Column lg={4} md={4} sm={4}>
                <TextInput
                  id="retentionQuantity"
                  labelText={intl.formatMessage({
                    id: "notebook.analytics.field.retentionQuantity",
                    defaultMessage: "Retention quantity",
                  })}
                  placeholder="e.g., 10 mL, 5 tablets"
                  value={formData.retentionQuantity}
                  onChange={(e) =>
                    handleFormChange("retentionQuantity", e.target.value)
                  }
                />
              </Column>

              <Column lg={4} md={4} sm={4}>
                <Select
                  id="retentionDuration"
                  labelText={intl.formatMessage({
                    id: "notebook.analytics.field.retentionDuration",
                    defaultMessage: "Retention duration",
                  })}
                  value={formData.retentionDuration}
                  onChange={(e) =>
                    handleFormChange("retentionDuration", e.target.value)
                  }
                >
                  <SelectItem value="" text="Select duration..." />
                  {retentionDurationOptions.map((option) => (
                    <SelectItem
                      key={option.value}
                      value={option.value}
                      text={option.label}
                    />
                  ))}
                </Select>
              </Column>
            </>
          )}

          {/* Biorepository Transfer Section */}
          <Column lg={16} md={8} sm={4}>
            <h5 style={{ marginBottom: "1rem", marginTop: "1.5rem" }}>
              <FormattedMessage
                id="notebook.analytics.section.biorepositoryTransfer"
                defaultMessage="Biorepository Transfer"
              />
            </h5>
          </Column>

          <Column lg={8} md={4} sm={4}>
            <RadioButtonGroup
              legendText={intl.formatMessage({
                id: "notebook.analytics.field.transferRequired",
                defaultMessage: "Transfer to biorepository *",
              })}
              name="transferRequired"
              valueSelected={formData.transferRequired}
              onChange={(value) => handleFormChange("transferRequired", value)}
            >
              {yesNoOptions.map((option) => (
                <RadioButton
                  key={option.value}
                  id={`transfer-${option.value}`}
                  labelText={option.label}
                  value={option.value}
                />
              ))}
            </RadioButtonGroup>
          </Column>

          {formData.transferRequired === "YES" && (
            <>
              <Column lg={4} md={4} sm={4}>
                <DatePicker
                  datePickerType="single"
                  onChange={(dates) => {
                    if (dates && dates[0]) {
                      const date = dates[0];
                      const formatted = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
                      handleFormChange("transferDate", formatted);
                    }
                  }}
                >
                  <DatePickerInput
                    id="transferDate"
                    labelText={intl.formatMessage({
                      id: "notebook.analytics.field.transferDate",
                      defaultMessage: "Transfer date",
                    })}
                    placeholder="yyyy-mm-dd"
                  />
                </DatePicker>
              </Column>

              <Column lg={8} md={4} sm={4}>
                <TextInput
                  id="receivingLaboratory"
                  labelText={intl.formatMessage({
                    id: "notebook.analytics.field.receivingLaboratory",
                    defaultMessage: "Receiving laboratory",
                  })}
                  placeholder="Enter receiving laboratory name"
                  value={formData.receivingLaboratory}
                  onChange={(e) =>
                    handleFormChange("receivingLaboratory", e.target.value)
                  }
                />
              </Column>

              <Column lg={8} md={4} sm={4}>
                <TextInput
                  id="storageCondition"
                  labelText={intl.formatMessage({
                    id: "notebook.analytics.field.storageConditionTransfer",
                    defaultMessage: "Storage condition",
                  })}
                  placeholder="e.g., -20°C, -80°C"
                  value={formData.storageCondition}
                  onChange={(e) =>
                    handleFormChange("storageCondition", e.target.value)
                  }
                />
              </Column>
            </>
          )}

          {/* Data Handling Section */}
          <Column lg={16} md={8} sm={4}>
            <h5 style={{ marginBottom: "1rem", marginTop: "1.5rem" }}>
              <FormattedMessage
                id="notebook.analytics.section.dataHandling"
                defaultMessage="Data Handling"
              />
            </h5>
          </Column>

          <Column lg={16} md={8} sm={4}>
            <Checkbox
              id="rawDataArchived"
              labelText="Raw data archived *"
              checked={formData.rawDataArchived}
              onChange={(e) =>
                handleFormChange("rawDataArchived", e.target.checked)
              }
            />
          </Column>

          <Column lg={16} md={8} sm={4}>
            <Checkbox
              id="finalReportsArchived"
              labelText="Final reports archived *"
              checked={formData.finalReportsArchived}
              onChange={(e) =>
                handleFormChange("finalReportsArchived", e.target.checked)
              }
            />
          </Column>

          <Column lg={16} md={8} sm={4}>
            <Checkbox
              id="retentionPeriodLogged"
              labelText="Retention period logged *"
              checked={formData.retentionPeriodLogged}
              onChange={(e) =>
                handleFormChange("retentionPeriodLogged", e.target.checked)
              }
            />
          </Column>
        </Grid>
      </Modal>
    </div>
  );
}

export default AnalyticsPostTestHandlingPage;
