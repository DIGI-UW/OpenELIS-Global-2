import { useState, useEffect, useRef, useCallback } from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  Tag,
  InlineNotification,
  TextInput,
  TextArea,
  Select,
  SelectItem,
  NumberInput,
  Modal,
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableSelectAll,
  TableSelectRow,
  Loading,
  TableContainer,
  Checkbox,
  DatePicker,
  DatePickerInput,
} from "@carbon/react";
import {
  Add,
  Checkmark,
  Renew,
  View,
  Edit,
  CheckmarkFilled,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import "../../workflow/NotebookWorkflow.css";

/**
 * PathologyCassettesPage - Cassette Setup workflow step.
 * Purpose: Create and label cassettes for tissue embedding.
 * Who uses it: Histology Technicians
 *
 * Key activities:
 * - Create cassettes from gross specimens
 * - Label cassettes with color coding
 * - Document tissue orientation
 * - Track cassette count per specimen
 */
function PathologyCassettesPage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
  notebookId,
}) {
  const intl = useIntl();
  const componentMounted = useRef(false);

  // Sample list state
  const [samples, setSamples] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);

  // Selection state
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);

  // Cassette Modal state
  const [cassetteModalOpen, setCassetteModalOpen] = useState(false);
  const [selectedSample, setSelectedSample] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [cassetteLoading, setCassetteLoading] = useState(false);
  const [cassetteViewMode, setCassetteViewMode] = useState(false);

  // Cassette form data
  const [cassetteData, setCassetteData] = useState({
    numberOfCassettes: 1,
    cassettePrefix: "",
    cassetteColor: "",
    cassetteLabels: [],
    tissueOrientation: "",
    tissueType: "",
    sectioningNotes: "",
    technicianName: "",
    technicianInitials: "",
    cassetteDate: "",
    notes: "",
    // Quality Control
    qcStatus: "PASS",
    qcTissueQuality: true,
    qcLabelingCorrect: true,
    qcOrientationVerified: true,
    qcCassetteIntegrity: true,
    qcIssues: "",
    qcCorrectiveAction: "",
    qcReviewedBy: "",
    qcReviewDate: "",
  });

  // Cassette color options
  const cassetteColors = [
    { value: "white", text: "White" },
    { value: "pink", text: "Pink" },
    { value: "blue", text: "Blue" },
    { value: "green", text: "Green" },
    { value: "yellow", text: "Yellow" },
    { value: "orange", text: "Orange" },
    { value: "lavender", text: "Lavender" },
    { value: "tan", text: "Tan" },
    { value: "gray", text: "Gray" },
  ];

  // Load samples
  useEffect(() => {
    componentMounted.current = true;
    loadPageSamples();

    return () => {
      componentMounted.current = false;
    };
  }, [entryId, pageData?.id]);

  const loadPageSamples = useCallback(() => {
    if (!entryId || !pageData?.id) {
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);

    // Fetch samples that have completed the previous step (gross examination)
    // and merge with current cassettes page data
    getFromOpenElisServer(
      `/rest/notebook/pathology/workflow/samples-ready?entryId=${entryId}&currentStep=cassettes`,
      (workflowResponse) => {
        if (!componentMounted.current) return;

        // Also fetch current page samples to get cassette data
        getFromOpenElisServer(
          `/rest/notebook/page/${pageData.id}/samples`,
          (pageResponse) => {
            if (!componentMounted.current) return;

            // Build a map of current page sample data by sampleItemId
            const pageSampleMap = {};
            if (pageResponse && Array.isArray(pageResponse)) {
              pageResponse.forEach((ps) => {
                const sampleId = String(ps.sampleItemId || ps.id);
                pageSampleMap[sampleId] = ps;
              });
            }

            if (workflowResponse && Array.isArray(workflowResponse)) {
              const transformedSamples = workflowResponse.map((sample) => {
                const sampleId = String(sample.id || sample.sampleItemId);
                // Get cassette page data if it exists
                const pageSample = pageSampleMap[sampleId];
                const cassetteData = pageSample?.data || {};
                // Note: workflowData contains data from PREVIOUS step (gross examination)
                // We should NOT use it for cassette-specific fields

                return {
                  id: sampleId,
                  externalId: sample.externalId,
                  accessionNumber: sample.accessionNumber,
                  sampleType:
                    sample.sampleType || sample.typeOfSample?.description,
                  specimenCategory: sample.specimenCategory || "histopathology",
                  collectionDate: sample.collectionDate,
                  // ONLY use status from current cassettes page, default to PENDING
                  // Backend returns status as "pageStatus" field
                  status:
                    pageSample?.pageStatus || pageSample?.status || "PENDING",
                  patientName: sample.patientName,
                  // Cassette status from current page data ONLY
                  cassettesCreated: cassetteData.cassettesCreated === true,
                  cassetteCount: cassetteData.cassetteCount || 0,
                  cassetteColor: cassetteData.cassetteColor || "",
                  technicianName: cassetteData.technicianName || "",
                  cassetteDate: cassetteData.cassetteDate || "",
                  // QC status from current page ONLY - show nothing until cassettes created
                  qcStatus: cassetteData.qcStatus || "",
                };
              });
              setSamples(transformedSamples);
            } else {
              setSamples([]);
            }
            setLoading(false);
          },
        );
      },
    );
  }, [entryId, pageData?.id]);

  // Check if we have the required entry ID to load samples
  const hasValidEntry = !!entryId;

  const handleCassetteInputChange = (e) => {
    const { name, value, type, checked } = e.target;
    setCassetteData((prev) => ({
      ...prev,
      [name]: type === "checkbox" ? checked : value,
    }));
  };

  // Open cassette modal
  const openCassetteModal = (sample) => {
    setSelectedSample(sample);
    setCassetteViewMode(false);
    setCassetteLoading(true);

    // Reset form data with defaults
    setCassetteData({
      numberOfCassettes: 1,
      cassettePrefix: sample.accessionNumber || "",
      cassetteColor: "",
      cassetteLabels: [],
      tissueOrientation: "",
      tissueType: "",
      sectioningNotes: "",
      technicianName: "",
      technicianInitials: "",
      cassetteDate: new Date().toISOString().split("T")[0],
      notes: "",
      // QC fields
      qcStatus: "PASS",
      qcTissueQuality: true,
      qcLabelingCorrect: true,
      qcOrientationVerified: true,
      qcCassetteIntegrity: true,
      qcIssues: "",
      qcCorrectiveAction: "",
      qcReviewedBy: "",
      qcReviewDate: new Date().toISOString().split("T")[0],
    });

    setCassetteModalOpen(true);

    // Fetch existing cassette data if available
    if (pageData?.id && sample?.id) {
      getFromOpenElisServer(
        `/rest/notebook/pathology/cassettes/${sample.id}?pageId=${pageData.id}`,
        (response) => {
          setCassetteLoading(false);
          if (response && response.success && response.hasData) {
            setCassetteViewMode(true);
            setCassetteData((prev) => ({
              ...prev,
              numberOfCassettes: response.numberOfCassettes || 1,
              cassettePrefix: response.cassettePrefix || "",
              cassetteColor: response.cassetteColor || "",
              cassetteLabels: response.cassetteLabels || [],
              tissueOrientation: response.tissueOrientation || "",
              tissueType: response.tissueType || "",
              sectioningNotes: response.sectioningNotes || "",
              technicianName: response.technicianName || "",
              technicianInitials: response.technicianInitials || "",
              cassetteDate: response.cassetteDate || "",
              notes: response.notes || "",
              // QC fields
              qcStatus: response.qcStatus || "PASS",
              qcTissueQuality: response.qcTissueQuality !== false,
              qcLabelingCorrect: response.qcLabelingCorrect !== false,
              qcOrientationVerified: response.qcOrientationVerified !== false,
              qcCassetteIntegrity: response.qcCassetteIntegrity !== false,
              qcIssues: response.qcIssues || "",
              qcCorrectiveAction: response.qcCorrectiveAction || "",
              qcReviewedBy: response.qcReviewedBy || "",
              qcReviewDate: response.qcReviewDate || "",
            }));
          }
        },
      );
    } else {
      setCassetteLoading(false);
    }
  };

  // Submit cassette data
  const handleSubmitCassettes = () => {
    if (submitting) return;

    setSubmitting(true);
    setError(null);

    // Generate cassette labels
    const labels = [];
    for (let i = 1; i <= cassetteData.numberOfCassettes; i++) {
      labels.push(
        `${cassetteData.cassettePrefix}-${String(i).padStart(2, "0")}`,
      );
    }

    const payload = {
      sampleId: selectedSample.id,
      pageId: pageData?.id,
      ...cassetteData,
      cassetteLabels: labels,
      cassettesCreated: true,
      cassetteCount: cassetteData.numberOfCassettes,
    };

    postToOpenElisServerJsonResponse(
      `/rest/notebook/pathology/cassettes/submit`,
      JSON.stringify(payload),
      (response) => {
        setSubmitting(false);
        if (response && response.success) {
          setCassetteModalOpen(false);
          setSuccessMessage(
            intl.formatMessage({
              id: "pathology.cassettes.success",
              defaultMessage: "Cassettes created successfully.",
            }),
          );
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(response?.error || "Failed to save cassette data.");
        }
      },
    );
  };

  // Mark samples as complete
  const handleMarkComplete = () => {
    if (submitting || selectedSampleIds.length === 0) return;

    const samplesToComplete = samples.filter(
      (s) => selectedSampleIds.includes(s.id) && s.cassettesCreated,
    );

    if (samplesToComplete.length === 0) {
      setError(
        intl.formatMessage({
          id: "pathology.cassettes.error.noComplete",
          defaultMessage:
            "Please create cassettes before marking samples as complete.",
        }),
      );
      return;
    }

    setSubmitting(true);
    setError(null);

    // Use string IDs for composite sample IDs (e.g., "123_grossed_0")
    const sampleIds = samplesToComplete.map((s) => s.id);

    postToOpenElisServerJsonResponse(
      `/rest/notebook/bulk/page/${pageData?.id}/samples/status-string`,
      JSON.stringify({
        sampleIds: sampleIds,
        status: "COMPLETED",
      }),
      (response) => {
        setSubmitting(false);
        if (response && response.success) {
          setSelectedSampleIds([]);
          setSuccessMessage(
            intl.formatMessage(
              {
                id: "pathology.cassettes.success.complete",
                defaultMessage:
                  "Successfully marked {count} samples as complete.",
              },
              { count: samplesToComplete.length },
            ),
          );
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(response?.error || "Failed to update status.");
        }
      },
    );
  };

  // Handle selection changes
  const handleSelectionChange = (selectedRows) => {
    const ids = selectedRows.map((row) => row.id);
    setSelectedSampleIds(ids);
  };

  // Table headers
  const headers = [
    {
      key: "accessionNumber",
      header: intl.formatMessage({
        id: "pathology.table.accessionNumber",
        defaultMessage: "Accession Number",
      }),
    },
    {
      key: "sampleType",
      header: intl.formatMessage({
        id: "pathology.table.specimenType",
        defaultMessage: "Specimen Type",
      }),
    },
    {
      key: "cassetteStatus",
      header: intl.formatMessage({
        id: "pathology.table.cassetteStatus",
        defaultMessage: "Cassette Status",
      }),
    },
    {
      key: "cassetteCount",
      header: intl.formatMessage({
        id: "pathology.table.cassetteCount",
        defaultMessage: "Cassettes",
      }),
    },
    {
      key: "cassetteColor",
      header: intl.formatMessage({
        id: "pathology.table.cassetteColor",
        defaultMessage: "Color",
      }),
    },
    {
      key: "qcStatus",
      header: intl.formatMessage({
        id: "pathology.table.qcStatus",
        defaultMessage: "QC",
      }),
    },
  ];

  // Computed values
  const completedCount = samples.filter((s) => s.cassettesCreated).length;
  const pendingCount = samples.filter(
    (s) => s.status === "PENDING" || !s.cassettesCreated,
  ).length;

  return (
    <div className="pathology-page cassettes-page">
      {/* Header */}
      <Grid className="page-header">
        <Column lg={12} md={8} sm={4}>
          <h3>
            <FormattedMessage
              id="pathology.page.cassettes.title"
              defaultMessage="Cassette Setup"
            />
          </h3>
          <p className="page-description">
            <FormattedMessage
              id="pathology.page.cassettes.description"
              defaultMessage="Create and label cassettes for tissue embedding."
            />
          </p>
        </Column>
        <Column lg={4} md={8} sm={4} className="header-actions">
          <Button
            kind="ghost"
            size="sm"
            renderIcon={Renew}
            onClick={loadPageSamples}
            disabled={loading}
          >
            <FormattedMessage
              id="pathology.page.refresh"
              defaultMessage="Refresh"
            />
          </Button>
        </Column>
      </Grid>

      {/* Notifications */}
      {error && (
        <InlineNotification
          kind="error"
          title={intl.formatMessage({
            id: "notification.error",
            defaultMessage: "Error",
          })}
          subtitle={error}
          onCloseButtonClick={() => setError(null)}
          style={{ marginBottom: "1rem" }}
        />
      )}

      {successMessage && (
        <InlineNotification
          kind="success"
          title={intl.formatMessage({
            id: "notification.success",
            defaultMessage: "Success",
          })}
          subtitle={successMessage}
          onCloseButtonClick={() => setSuccessMessage(null)}
          style={{ marginBottom: "1rem" }}
        />
      )}

      {/* Summary Stats */}
      <Grid className="summary-stats" style={{ marginBottom: "1rem" }}>
        <Column lg={4} md={4} sm={4}>
          <Tile className="stat-tile">
            <span className="stat-value">{samples.length}</span>
            <span className="stat-label">
              <FormattedMessage
                id="pathology.stats.total"
                defaultMessage="Total Samples"
              />
            </span>
          </Tile>
        </Column>
        <Column lg={4} md={4} sm={4}>
          <Tile className="stat-tile">
            <span className="stat-value" style={{ color: "#24a148" }}>
              {completedCount}
            </span>
            <span className="stat-label">
              <FormattedMessage
                id="pathology.stats.cassettesCreated"
                defaultMessage="Cassettes Created"
              />
            </span>
          </Tile>
        </Column>
        <Column lg={4} md={4} sm={4}>
          <Tile className="stat-tile">
            <span className="stat-value" style={{ color: "#f1c21b" }}>
              {pendingCount}
            </span>
            <span className="stat-label">
              <FormattedMessage
                id="pathology.stats.pending"
                defaultMessage="Pending"
              />
            </span>
          </Tile>
        </Column>
      </Grid>

      {/* Action Buttons */}
      {hasValidEntry && samples.length > 0 && (
        <div
          className="action-buttons"
          style={{ marginBottom: "1rem", display: "flex", gap: "0.5rem" }}
        >
          <Button
            kind="primary"
            size="md"
            renderIcon={Add}
            onClick={() => {
              if (selectedSampleIds.length === 1) {
                const sample = samples.find(
                  (s) => s.id === selectedSampleIds[0],
                );
                if (sample) openCassetteModal(sample);
              }
            }}
            disabled={selectedSampleIds.length !== 1 || submitting}
          >
            <FormattedMessage
              id="pathology.page.createCassettes"
              defaultMessage="Create Cassettes"
            />
          </Button>
          <Button
            kind="secondary"
            size="md"
            renderIcon={Checkmark}
            onClick={handleMarkComplete}
            disabled={selectedSampleIds.length === 0 || submitting}
          >
            <FormattedMessage
              id="pathology.page.markComplete"
              defaultMessage="Mark Complete ({count})"
              values={{ count: selectedSampleIds.length }}
            />
          </Button>
        </div>
      )}

      {/* Sample Table */}
      <div className="sample-table">
        {loading ? (
          <Loading description="Loading samples..." withOverlay={false} />
        ) : !hasValidEntry ? (
          <div className="empty-state">
            <p>
              <FormattedMessage
                id="pathology.page.noPageId"
                defaultMessage="No page data available. Samples will appear here once the workflow is started."
              />
            </p>
          </div>
        ) : samples.length === 0 ? (
          <div className="empty-state">
            <p>
              <FormattedMessage
                id="pathology.page.cassettes.empty"
                defaultMessage="No samples available for cassette setup. Samples must complete gross examination first."
              />
            </p>
          </div>
        ) : (
          <DataTable
            rows={samples.map((s) => ({
              ...s,
              cassetteStatus:
                s.status === "COMPLETED"
                  ? "Complete"
                  : s.cassettesCreated
                    ? "In Progress"
                    : "Pending",
            }))}
            headers={headers}
            isSortable
            render={({
              rows,
              headers,
              getTableProps,
              getHeaderProps,
              getRowProps,
              getSelectionProps,
              selectedRows,
            }) => {
              if (
                selectedRows.length !== selectedSampleIds.length ||
                !selectedRows.every((r) => selectedSampleIds.includes(r.id))
              ) {
                setTimeout(() => handleSelectionChange(selectedRows), 0);
              }

              return (
                <TableContainer>
                  <Table {...getTableProps()}>
                    <TableHead>
                      <TableRow>
                        <TableSelectAll {...getSelectionProps()} />
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
                        const sample = samples.find((s) => s.id === row.id);
                        return (
                          <TableRow key={row.id} {...getRowProps({ row })}>
                            <TableSelectRow {...getSelectionProps({ row })} />
                            {row.cells.map((cell) => (
                              <TableCell key={cell.id}>
                                {cell.info.header === "cassetteStatus" ? (
                                  <span
                                    style={{
                                      display: "flex",
                                      alignItems: "center",
                                      gap: "0.25rem",
                                      cursor: "pointer",
                                    }}
                                    onClick={() => openCassetteModal(sample)}
                                    title="Click to view/edit cassettes"
                                  >
                                    {sample?.status === "COMPLETED" ? (
                                      <>
                                        <CheckmarkFilled
                                          size={16}
                                          style={{ color: "#24a148" }}
                                        />
                                        <span>Complete</span>
                                      </>
                                    ) : sample?.cassettesCreated ? (
                                      <Tag type="blue" size="sm">
                                        In Progress
                                      </Tag>
                                    ) : (
                                      <Tag type="gray" size="sm">
                                        Pending
                                      </Tag>
                                    )}
                                  </span>
                                ) : cell.info.header === "cassetteCount" ? (
                                  <span>
                                    {sample?.cassetteCount > 0
                                      ? sample.cassetteCount
                                      : "-"}
                                  </span>
                                ) : cell.info.header === "cassetteColor" ? (
                                  sample?.cassetteColor ? (
                                    <Tag type="cool-gray" size="sm">
                                      {sample.cassetteColor}
                                    </Tag>
                                  ) : (
                                    "-"
                                  )
                                ) : cell.info.header === "qcStatus" ? (
                                  sample?.qcStatus ? (
                                    <Tag
                                      type={
                                        sample.qcStatus === "PASS"
                                          ? "green"
                                          : sample.qcStatus === "FLAG"
                                            ? "gold"
                                            : sample.qcStatus === "FAIL"
                                              ? "red"
                                              : "gray"
                                      }
                                      size="sm"
                                    >
                                      {sample.qcStatus === "PASS"
                                        ? "✓ Pass"
                                        : sample.qcStatus === "FLAG"
                                          ? "⚠ Flag"
                                          : sample.qcStatus === "FAIL"
                                            ? "✗ Fail"
                                            : "-"}
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
                        );
                      })}
                    </TableBody>
                  </Table>
                </TableContainer>
              );
            }}
          />
        )}
      </div>

      {/* Cassette Modal */}
      <Modal
        open={cassetteModalOpen}
        modalHeading={intl.formatMessage(
          {
            id: "pathology.modal.cassettes.title",
            defaultMessage: "Cassette Setup - {accession}",
          },
          { accession: selectedSample?.accessionNumber || "" },
        )}
        primaryButtonText={
          cassetteViewMode
            ? intl.formatMessage({
                id: "pathology.modal.edit",
                defaultMessage: "Edit",
              })
            : intl.formatMessage({
                id: "pathology.modal.save",
                defaultMessage: "Save",
              })
        }
        secondaryButtonText={intl.formatMessage({
          id: "pathology.modal.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => {
          setCassetteModalOpen(false);
          setSelectedSample(null);
          setCassetteViewMode(false);
          setError(null);
        }}
        onRequestSubmit={
          cassetteViewMode
            ? () => setCassetteViewMode(false)
            : handleSubmitCassettes
        }
        primaryButtonDisabled={submitting || cassetteLoading}
        size="lg"
        hasScrollingContent
        preventCloseOnClickOutside
      >
        {cassetteLoading ? (
          <div style={{ textAlign: "center", padding: "2rem" }}>
            <Loading description="Loading data..." withOverlay={false} />
          </div>
        ) : (
          <div style={{ maxHeight: "70vh", overflowY: "auto" }}>
            {/* View Mode Banner */}
            {cassetteViewMode && (
              <InlineNotification
                kind="info"
                title={intl.formatMessage({
                  id: "pathology.cassettes.viewMode",
                  defaultMessage: "Viewing existing data",
                })}
                subtitle={intl.formatMessage({
                  id: "pathology.cassettes.viewMode.description",
                  defaultMessage: "Click 'Edit' to make changes.",
                })}
                lowContrast
                hideCloseButton
                style={{ marginBottom: "1rem" }}
              />
            )}

            {/* Cassette Configuration */}
            <h5 style={{ marginBottom: "1rem" }}>
              <FormattedMessage
                id="pathology.cassettes.section.configuration"
                defaultMessage="Cassette Configuration"
              />
            </h5>
            <Grid>
              <Column lg={4} md={4} sm={4}>
                <NumberInput
                  id="numberOfCassettes"
                  name="numberOfCassettes"
                  label={intl.formatMessage({
                    id: "pathology.cassettes.numberOfCassettes",
                    defaultMessage: "Number of Cassettes",
                  })}
                  value={cassetteData.numberOfCassettes}
                  min={1}
                  max={50}
                  onChange={(e, { value }) =>
                    setCassetteData((prev) => ({
                      ...prev,
                      numberOfCassettes: value,
                    }))
                  }
                  disabled={cassetteViewMode}
                />
              </Column>
              <Column lg={4} md={4} sm={4}>
                <TextInput
                  id="cassettePrefix"
                  name="cassettePrefix"
                  labelText={intl.formatMessage({
                    id: "pathology.cassettes.prefix",
                    defaultMessage: "Cassette ID Prefix",
                  })}
                  value={cassetteData.cassettePrefix}
                  onChange={handleCassetteInputChange}
                  disabled={cassetteViewMode}
                />
              </Column>
              <Column lg={4} md={4} sm={4}>
                <Select
                  id="cassetteColor"
                  name="cassetteColor"
                  labelText={intl.formatMessage({
                    id: "pathology.cassettes.color",
                    defaultMessage: "Cassette Color",
                  })}
                  value={cassetteData.cassetteColor}
                  onChange={handleCassetteInputChange}
                  disabled={cassetteViewMode}
                >
                  <SelectItem value="" text="Select color..." />
                  {cassetteColors.map((color) => (
                    <SelectItem
                      key={color.value}
                      value={color.value}
                      text={color.text}
                    />
                  ))}
                </Select>
              </Column>
            </Grid>

            {/* Cassette Labels Preview */}
            {cassetteData.numberOfCassettes > 0 &&
              cassetteData.cassettePrefix && (
                <div style={{ marginTop: "1rem" }}>
                  <p style={{ fontSize: "0.875rem", marginBottom: "0.5rem" }}>
                    <FormattedMessage
                      id="pathology.cassettes.labelsPreview"
                      defaultMessage="Generated Labels:"
                    />
                  </p>
                  <div
                    style={{ display: "flex", flexWrap: "wrap", gap: "0.5rem" }}
                  >
                    {Array.from(
                      { length: Math.min(cassetteData.numberOfCassettes, 10) },
                      (_, i) => (
                        <Tag key={i} type="blue" size="sm">
                          {`${cassetteData.cassettePrefix}-${String(i + 1).padStart(2, "0")}`}
                        </Tag>
                      ),
                    )}
                    {cassetteData.numberOfCassettes > 10 && (
                      <Tag type="gray" size="sm">
                        +{cassetteData.numberOfCassettes - 10} more
                      </Tag>
                    )}
                  </div>
                </div>
              )}

            {/* Tissue Information */}
            <h5 style={{ marginTop: "1.5rem", marginBottom: "1rem" }}>
              <FormattedMessage
                id="pathology.cassettes.section.tissue"
                defaultMessage="Tissue Information"
              />
            </h5>
            <Grid>
              <Column lg={8} md={4} sm={4}>
                <TextInput
                  id="tissueType"
                  name="tissueType"
                  labelText={intl.formatMessage({
                    id: "pathology.cassettes.tissueType",
                    defaultMessage: "Tissue Type",
                  })}
                  value={cassetteData.tissueType}
                  onChange={handleCassetteInputChange}
                  disabled={cassetteViewMode}
                />
              </Column>
              <Column lg={8} md={4} sm={4}>
                <TextInput
                  id="tissueOrientation"
                  name="tissueOrientation"
                  labelText={intl.formatMessage({
                    id: "pathology.cassettes.tissueOrientation",
                    defaultMessage: "Tissue Orientation",
                  })}
                  value={cassetteData.tissueOrientation}
                  onChange={handleCassetteInputChange}
                  disabled={cassetteViewMode}
                  placeholder="e.g., Superior marked with blue ink"
                />
              </Column>
            </Grid>

            <Grid style={{ marginTop: "1rem" }}>
              <Column lg={16} md={8} sm={4}>
                <TextArea
                  id="sectioningNotes"
                  name="sectioningNotes"
                  labelText={intl.formatMessage({
                    id: "pathology.cassettes.sectioningNotes",
                    defaultMessage: "Sectioning Notes",
                  })}
                  value={cassetteData.sectioningNotes}
                  onChange={handleCassetteInputChange}
                  disabled={cassetteViewMode}
                  rows={3}
                  placeholder="Describe how tissue was sectioned and placed in cassettes"
                />
              </Column>
            </Grid>

            {/* Staff Info */}
            <h5 style={{ marginTop: "1.5rem", marginBottom: "1rem" }}>
              <FormattedMessage
                id="pathology.cassettes.section.staff"
                defaultMessage="Staff & Date"
              />
            </h5>
            <Grid>
              <Column lg={4} md={4} sm={4}>
                <TextInput
                  id="technicianName"
                  name="technicianName"
                  labelText={intl.formatMessage({
                    id: "pathology.cassettes.technicianName",
                    defaultMessage: "Technician Name",
                  })}
                  value={cassetteData.technicianName}
                  onChange={handleCassetteInputChange}
                  disabled={cassetteViewMode}
                />
              </Column>
              <Column lg={4} md={4} sm={4}>
                <TextInput
                  id="technicianInitials"
                  name="technicianInitials"
                  labelText={intl.formatMessage({
                    id: "pathology.cassettes.technicianInitials",
                    defaultMessage: "Initials",
                  })}
                  value={cassetteData.technicianInitials}
                  onChange={handleCassetteInputChange}
                  disabled={cassetteViewMode}
                />
              </Column>
              <Column lg={4} md={4} sm={4}>
                <TextInput
                  id="cassetteDate"
                  name="cassetteDate"
                  labelText={intl.formatMessage({
                    id: "pathology.cassettes.date",
                    defaultMessage: "Date",
                  })}
                  value={cassetteData.cassetteDate}
                  onChange={handleCassetteInputChange}
                  disabled={cassetteViewMode}
                  type="date"
                />
              </Column>
            </Grid>

            {/* Notes */}
            <Grid style={{ marginTop: "1rem" }}>
              <Column lg={16} md={8} sm={4}>
                <TextArea
                  id="notes"
                  name="notes"
                  labelText={intl.formatMessage({
                    id: "pathology.cassettes.notes",
                    defaultMessage: "Additional Notes",
                  })}
                  value={cassetteData.notes}
                  onChange={handleCassetteInputChange}
                  disabled={cassetteViewMode}
                  rows={2}
                />
              </Column>
            </Grid>

            {/* Quality Control Section */}
            <h5 style={{ marginTop: "1.5rem", marginBottom: "1rem" }}>
              <FormattedMessage
                id="pathology.qc.section.title"
                defaultMessage="Quality Control & Assessment"
              />
            </h5>

            {/* QC Status Indicator */}
            <div
              style={{
                padding: "1rem",
                marginBottom: "1rem",
                backgroundColor:
                  cassetteData.qcStatus === "PASS"
                    ? "#defbe6"
                    : cassetteData.qcStatus === "FLAG"
                      ? "#fff8e1"
                      : "#fff1f1",
                borderRadius: "4px",
                border: `1px solid ${
                  cassetteData.qcStatus === "PASS"
                    ? "#24a148"
                    : cassetteData.qcStatus === "FLAG"
                      ? "#f1c21b"
                      : "#da1e28"
                }`,
              }}
            >
              <Grid>
                <Column lg={8} md={4} sm={4}>
                  <Select
                    id="qcStatus"
                    name="qcStatus"
                    labelText={intl.formatMessage({
                      id: "pathology.qc.status",
                      defaultMessage: "QC Status",
                    })}
                    value={cassetteData.qcStatus}
                    onChange={handleCassetteInputChange}
                    disabled={cassetteViewMode}
                  >
                    <SelectItem value="PASS" text="✓ Pass - All criteria met" />
                    <SelectItem
                      value="FLAG"
                      text="⚠ Flag - Proceed with caution"
                    />
                    <SelectItem
                      value="FAIL"
                      text="✗ Fail - Requires corrective action"
                    />
                  </Select>
                </Column>
                <Column lg={8} md={4} sm={4}>
                  {cassetteData.qcStatus === "FLAG" && (
                    <InlineNotification
                      kind="warning"
                      title={intl.formatMessage({
                        id: "pathology.qc.flagged",
                        defaultMessage: "Sample Flagged",
                      })}
                      subtitle={intl.formatMessage({
                        id: "pathology.qc.flagged.description",
                        defaultMessage:
                          "Processing can continue but issue is documented.",
                      })}
                      lowContrast
                      hideCloseButton
                    />
                  )}
                  {cassetteData.qcStatus === "FAIL" && (
                    <InlineNotification
                      kind="error"
                      title={intl.formatMessage({
                        id: "pathology.qc.failed",
                        defaultMessage: "QC Failed",
                      })}
                      subtitle={intl.formatMessage({
                        id: "pathology.qc.failed.description",
                        defaultMessage:
                          "Corrective action required. Sample is still processed to preserve specimen.",
                      })}
                      lowContrast
                      hideCloseButton
                    />
                  )}
                </Column>
              </Grid>
            </div>

            {/* QC Checkpoints */}
            <Grid>
              <Column lg={4} md={4} sm={4}>
                <Checkbox
                  id="qcTissueQuality"
                  name="qcTissueQuality"
                  labelText={intl.formatMessage({
                    id: "pathology.qc.cassettes.tissueQuality",
                    defaultMessage: "Tissue quality acceptable",
                  })}
                  checked={cassetteData.qcTissueQuality}
                  onChange={(e) =>
                    setCassetteData((prev) => ({
                      ...prev,
                      qcTissueQuality: e.target.checked,
                    }))
                  }
                  disabled={cassetteViewMode}
                />
              </Column>
              <Column lg={4} md={4} sm={4}>
                <Checkbox
                  id="qcLabelingCorrect"
                  name="qcLabelingCorrect"
                  labelText={intl.formatMessage({
                    id: "pathology.qc.labelingCorrect",
                    defaultMessage: "Labeling correct & complete",
                  })}
                  checked={cassetteData.qcLabelingCorrect}
                  onChange={(e) =>
                    setCassetteData((prev) => ({
                      ...prev,
                      qcLabelingCorrect: e.target.checked,
                    }))
                  }
                  disabled={cassetteViewMode}
                />
              </Column>
              <Column lg={4} md={4} sm={4}>
                <Checkbox
                  id="qcOrientationVerified"
                  name="qcOrientationVerified"
                  labelText={intl.formatMessage({
                    id: "pathology.qc.cassettes.orientationVerified",
                    defaultMessage: "Orientation verified",
                  })}
                  checked={cassetteData.qcOrientationVerified}
                  onChange={(e) =>
                    setCassetteData((prev) => ({
                      ...prev,
                      qcOrientationVerified: e.target.checked,
                    }))
                  }
                  disabled={cassetteViewMode}
                />
              </Column>
              <Column lg={4} md={4} sm={4}>
                <Checkbox
                  id="qcCassetteIntegrity"
                  name="qcCassetteIntegrity"
                  labelText={intl.formatMessage({
                    id: "pathology.qc.cassettes.cassetteIntegrity",
                    defaultMessage: "Cassette integrity verified",
                  })}
                  checked={cassetteData.qcCassetteIntegrity}
                  onChange={(e) =>
                    setCassetteData((prev) => ({
                      ...prev,
                      qcCassetteIntegrity: e.target.checked,
                    }))
                  }
                  disabled={cassetteViewMode}
                />
              </Column>
            </Grid>

            {/* QC Issues and Corrective Actions */}
            {(cassetteData.qcStatus === "FLAG" ||
              cassetteData.qcStatus === "FAIL") && (
              <Grid style={{ marginTop: "1rem" }}>
                <Column lg={8} md={4} sm={4}>
                  <TextArea
                    id="qcIssues"
                    name="qcIssues"
                    labelText={intl.formatMessage({
                      id: "pathology.qc.issues",
                      defaultMessage: "QC Issues Identified",
                    })}
                    placeholder={intl.formatMessage({
                      id: "pathology.qc.issues.placeholder",
                      defaultMessage: "Describe any quality issues observed...",
                    })}
                    value={cassetteData.qcIssues}
                    onChange={handleCassetteInputChange}
                    disabled={cassetteViewMode}
                    rows={3}
                  />
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <TextArea
                    id="qcCorrectiveAction"
                    name="qcCorrectiveAction"
                    labelText={intl.formatMessage({
                      id: "pathology.qc.correctiveAction",
                      defaultMessage: "Corrective Action Taken",
                    })}
                    placeholder={intl.formatMessage({
                      id: "pathology.qc.correctiveAction.placeholder",
                      defaultMessage:
                        "Describe corrective actions or mitigation steps...",
                    })}
                    value={cassetteData.qcCorrectiveAction}
                    onChange={handleCassetteInputChange}
                    disabled={cassetteViewMode}
                    rows={3}
                  />
                </Column>
              </Grid>
            )}

            {/* QC Reviewer */}
            <Grid style={{ marginTop: "1rem" }}>
              <Column lg={4} md={4} sm={4}>
                <TextInput
                  id="qcReviewedBy"
                  name="qcReviewedBy"
                  labelText={intl.formatMessage({
                    id: "pathology.qc.reviewedBy",
                    defaultMessage: "QC Reviewed By",
                  })}
                  value={cassetteData.qcReviewedBy}
                  onChange={handleCassetteInputChange}
                  disabled={cassetteViewMode}
                />
              </Column>
              <Column lg={4} md={4} sm={4}>
                <TextInput
                  id="qcReviewDate"
                  name="qcReviewDate"
                  labelText={intl.formatMessage({
                    id: "pathology.qc.reviewDate",
                    defaultMessage: "QC Review Date",
                  })}
                  value={cassetteData.qcReviewDate}
                  onChange={handleCassetteInputChange}
                  disabled={cassetteViewMode}
                  type="date"
                />
              </Column>
            </Grid>
          </div>
        )}
      </Modal>
    </div>
  );
}

export default PathologyCassettesPage;
