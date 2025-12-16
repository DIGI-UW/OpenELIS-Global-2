import React, {
  useState,
  useEffect,
  useRef,
  useCallback,
  useMemo,
} from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  InlineNotification,
  TextInput,
  TextArea,
  Dropdown,
  DatePicker,
  DatePickerInput,
  NumberInput,
  Modal,
  Tag,
  RadioButtonGroup,
  RadioButton,
} from "@carbon/react";
import {
  Add,
  CheckmarkFilled,
  WarningAltFilled,
  Renew,
  Chemistry,
  Activity,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
} from "../../../utils/Utils";
import SampleGrid from "../../workflow/SampleGrid";
import "../../workflow/NotebookWorkflow.css";

/**
 * MNTDProcessingQCPage - Page 6 of the MNTD workflow.
 * Handles processing execution and quality control verification.
 *
 * Purpose: Execute lab processes and verify integrity.
 *
 * Who uses it:
 * - Lab technician
 * - Supervisor
 *
 * Data Points:
 * - Processing Execution: Protocol used, Start & end time, Technician
 * - Quality Control: QC result (Pass/Fail), QC metrics (Ct value, yield, integrity, etc.)
 * - Decision if failed: Re-extract, Re-run, Discard
 *
 * System Actions:
 * - Status updated: Processed - QC Passed, Processed - QC Failed
 * - Automatic branching for rework
 *
 * Leads to: Test Assignment & Machine Scheduling Page
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - The notebook page data
 * @param {Object} props.progress - Page progress
 * @param {function} props.onProgressUpdate - Callback when progress changes
 * @param {number} props.notebookId - The notebook ID
 */
function MNTDProcessingQCPage({
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
  const [selectedIds, setSelectedIds] = useState([]);
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  // Processing modal state
  const [showProcessingModal, setShowProcessingModal] = useState(false);
  const [processingData, setProcessingData] = useState({
    protocol: "",
    startDate: new Date().toISOString().split("T")[0],
    startTime: "",
    endDate: new Date().toISOString().split("T")[0],
    endTime: "",
    technician: "",
    notes: "",
  });

  // QC modal state
  const [showQCModal, setShowQCModal] = useState(false);
  const [qcData, setQCData] = useState({
    qcResult: "PASS",
    ctValue: "",
    yield: "",
    integrity: "",
    otherMetrics: "",
    failedDecision: "",
    qcNotes: "",
    qcTechnician: "",
    qcDate: new Date().toISOString().split("T")[0],
  });

  // Protocol options
  const protocolOptions = [
    { id: "DNA_EXTRACTION", text: "DNA Extraction" },
    { id: "RNA_EXTRACTION", text: "RNA Extraction" },
    { id: "PCR_AMPLIFICATION", text: "PCR Amplification" },
    { id: "ELISA", text: "ELISA" },
    { id: "MICROSCOPY", text: "Microscopy" },
    { id: "RDT", text: "Rapid Diagnostic Test (RDT)" },
    { id: "CULTURE", text: "Culture" },
    { id: "SEQUENCING", text: "Sequencing" },
    { id: "OTHER", text: "Other" },
  ];

  // Failed decision options
  const failedDecisionOptions = [
    { id: "RE_EXTRACT", text: "Re-extract" },
    { id: "RE_RUN", text: "Re-run" },
    { id: "DISCARD", text: "Discard" },
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

    // Skip loading for synthetic page IDs
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
              collectionDate: sample.collectionDate,
              status: sample.pageStatus || "PENDING",
              patientName: sample.patientName,
              // Processing data
              protocol: sample.data?.protocol,
              processingStartTime: sample.data?.startTime,
              processingEndTime: sample.data?.endTime,
              processingTechnician: sample.data?.technician,
              // QC data
              qcResult: sample.data?.qcResult,
              ctValue: sample.data?.ctValue,
              yield: sample.data?.yield,
              integrity: sample.data?.integrity,
              failedDecision: sample.data?.failedDecision,
              qcTechnician: sample.data?.qcTechnician,
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

  // Check if page has a real database ID
  const hasRealPageId =
    pageData?.id && !String(pageData.id).startsWith("default-");

  // Calculate stats
  const stats = useMemo(() => {
    const processed = samples.filter((s) => s.protocol).length;
    const qcPassed = samples.filter((s) => s.qcResult === "PASS").length;
    const qcFailed = samples.filter((s) => s.qcResult === "FAIL").length;
    const pending = samples.filter((s) => !s.protocol && !s.qcResult).length;
    return { total: samples.length, processed, qcPassed, qcFailed, pending };
  }, [samples]);

  // Handle opening processing modal
  const handleOpenProcessingModal = useCallback(() => {
    if (selectedIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.mntd.processingqc.selectSamples",
          defaultMessage: "Please select at least one sample.",
        }),
      );
      return;
    }
    setShowProcessingModal(true);
  }, [selectedIds, intl]);

  // Handle saving processing data
  const handleSaveProcessingData = useCallback(() => {
    if (!processingData.protocol) {
      setError(
        intl.formatMessage({
          id: "notebook.mntd.processingqc.protocolRequired",
          defaultMessage: "Protocol is required.",
        }),
      );
      return;
    }

    if (!hasRealPageId) {
      setShowProcessingModal(false);
      return;
    }

    const numericIds = selectedIds.map((id) => parseInt(id, 10));

    const dataToSave = {
      protocol: processingData.protocol,
      startDate: processingData.startDate,
      startTime: processingData.startTime,
      endDate: processingData.endDate,
      endTime: processingData.endTime,
      technician: processingData.technician,
      processingNotes: processingData.notes,
    };

    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
      JSON.stringify({
        sampleIds: numericIds,
        data: dataToSave,
      }),
      (response) => {
        if (componentMounted.current) {
          if (response && !response.error) {
            // Update status to IN_PROGRESS
            postToOpenElisServer(
              `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
              JSON.stringify({
                sampleIds: numericIds,
                status: "IN_PROGRESS",
              }),
              () => {
                setSuccess(
                  intl.formatMessage(
                    {
                      id: "notebook.mntd.processingqc.processingSaved",
                      defaultMessage:
                        "Processing data saved for {count} samples.",
                    },
                    { count: selectedIds.length },
                  ),
                );
                setShowProcessingModal(false);
                setSelectedIds([]);
                // Reset form
                setProcessingData({
                  protocol: "",
                  startDate: new Date().toISOString().split("T")[0],
                  startTime: "",
                  endDate: new Date().toISOString().split("T")[0],
                  endTime: "",
                  technician: "",
                  notes: "",
                });
                loadPageSamples();
                if (onProgressUpdate) {
                  onProgressUpdate();
                }
              },
            );
          } else {
            setError(response?.error || "Failed to save processing data.");
          }
        }
      },
    );
  }, [
    processingData,
    selectedIds,
    hasRealPageId,
    pageData?.id,
    loadPageSamples,
    onProgressUpdate,
    intl,
  ]);

  // Handle opening QC modal
  const handleOpenQCModal = useCallback(() => {
    if (selectedIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.mntd.processingqc.selectSamples",
          defaultMessage: "Please select at least one sample.",
        }),
      );
      return;
    }
    setShowQCModal(true);
  }, [selectedIds, intl]);

  // Handle saving QC data
  const handleSaveQCData = useCallback(() => {
    if (!qcData.qcResult) {
      setError(
        intl.formatMessage({
          id: "notebook.mntd.processingqc.qcResultRequired",
          defaultMessage: "QC result is required.",
        }),
      );
      return;
    }

    // If failed, decision is required
    if (qcData.qcResult === "FAIL" && !qcData.failedDecision) {
      setError(
        intl.formatMessage({
          id: "notebook.mntd.processingqc.failedDecisionRequired",
          defaultMessage: "Please select an action for failed samples.",
        }),
      );
      return;
    }

    if (!hasRealPageId) {
      setShowQCModal(false);
      return;
    }

    const numericIds = selectedIds.map((id) => parseInt(id, 10));

    const dataToSave = {
      qcResult: qcData.qcResult,
      ctValue: qcData.ctValue,
      yield: qcData.yield,
      integrity: qcData.integrity,
      otherMetrics: qcData.otherMetrics,
      failedDecision: qcData.qcResult === "FAIL" ? qcData.failedDecision : null,
      qcNotes: qcData.qcNotes,
      qcTechnician: qcData.qcTechnician,
      qcDate: qcData.qcDate,
    };

    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
      JSON.stringify({
        sampleIds: numericIds,
        data: dataToSave,
      }),
      (response) => {
        if (componentMounted.current) {
          if (response && !response.error) {
            // Update status based on QC result
            const newStatus =
              qcData.qcResult === "PASS" ? "COMPLETED" : "IN_PROGRESS";
            postToOpenElisServer(
              `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
              JSON.stringify({
                sampleIds: numericIds,
                status: newStatus,
              }),
              () => {
                setSuccess(
                  intl.formatMessage(
                    {
                      id: "notebook.mntd.processingqc.qcSaved",
                      defaultMessage: "QC data saved for {count} samples.",
                    },
                    { count: selectedIds.length },
                  ),
                );
                setShowQCModal(false);
                setSelectedIds([]);
                // Reset form
                setQCData({
                  qcResult: "PASS",
                  ctValue: "",
                  yield: "",
                  integrity: "",
                  otherMetrics: "",
                  failedDecision: "",
                  qcNotes: "",
                  qcTechnician: "",
                  qcDate: new Date().toISOString().split("T")[0],
                });
                loadPageSamples();
                if (onProgressUpdate) {
                  onProgressUpdate();
                }
              },
            );
          } else {
            setError(response?.error || "Failed to save QC data.");
          }
        }
      },
    );
  }, [
    qcData,
    selectedIds,
    hasRealPageId,
    pageData?.id,
    loadPageSamples,
    onProgressUpdate,
    intl,
  ]);

  // Handle status change
  const handleStatusChange = useCallback(
    (sampleId, newStatus) => {
      if (!hasRealPageId) {
        setError(
          intl.formatMessage({
            id: "notebook.mntd.processingqc.pageNotInitialized",
            defaultMessage:
              "Cannot update status: Page not properly initialized.",
          }),
        );
        return;
      }

      postToOpenElisServer(
        `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
        JSON.stringify({
          sampleIds: [parseInt(sampleId, 10)],
          status: newStatus,
        }),
        (status) => {
          if (status === 200) {
            loadPageSamples();
            if (onProgressUpdate) {
              onProgressUpdate();
            }
          } else {
            setError("Failed to update sample status. Please try again.");
          }
        },
      );
    },
    [pageData?.id, hasRealPageId, loadPageSamples, onProgressUpdate, intl],
  );

  // Bulk mark as QC passed
  const handleBulkMarkQCPassed = useCallback(() => {
    if (selectedIds.length === 0) return;

    if (!hasRealPageId) {
      setError(
        intl.formatMessage({
          id: "notebook.mntd.processingqc.pageNotInitialized",
          defaultMessage:
            "Cannot update status: Page not properly initialized.",
        }),
      );
      return;
    }

    const numericIds = selectedIds.map((id) => parseInt(id, 10));

    // First apply QC pass data
    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
      JSON.stringify({
        sampleIds: numericIds,
        data: {
          qcResult: "PASS",
          qcDate: new Date().toISOString().split("T")[0],
        },
      }),
      (response) => {
        if (response && !response.error) {
          // Then update status to COMPLETED
          postToOpenElisServer(
            `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
            JSON.stringify({
              sampleIds: numericIds,
              status: "COMPLETED",
            }),
            (status) => {
              if (status === 200) {
                setSuccess(
                  intl.formatMessage(
                    {
                      id: "notebook.mntd.processingqc.markedQCPassed",
                      defaultMessage: "Marked {count} samples as QC Passed.",
                    },
                    { count: selectedIds.length },
                  ),
                );
                loadPageSamples();
                setSelectedIds([]);
                if (onProgressUpdate) {
                  onProgressUpdate();
                }
              } else {
                setError("Failed to update sample status. Please try again.");
              }
            },
          );
        } else {
          setError(response?.error || "Failed to mark samples as QC passed.");
        }
      },
    );
  }, [
    selectedIds,
    pageData?.id,
    hasRealPageId,
    loadPageSamples,
    onProgressUpdate,
    intl,
  ]);

  // Render processing info column
  const renderProcessingInfo = (sample) => {
    if (sample.protocol) {
      const protocolLabel =
        protocolOptions.find((p) => p.id === sample.protocol)?.text ||
        sample.protocol;
      return (
        <div style={{ fontSize: "12px" }}>
          <Tag type="blue" size="sm">
            {protocolLabel}
          </Tag>
          {sample.processingTechnician && (
            <div style={{ marginTop: "2px", color: "#525252" }}>
              by {sample.processingTechnician}
            </div>
          )}
          {sample.processingStartTime && (
            <div style={{ color: "#525252" }}>
              <Activity size={12} style={{ marginRight: "4px" }} />
              {sample.processingStartTime}
            </div>
          )}
        </div>
      );
    }
    return (
      <span style={{ color: "#8d8d8d", fontSize: "12px" }}>
        <FormattedMessage
          id="notebook.mntd.processingqc.notProcessed"
          defaultMessage="Not processed"
        />
      </span>
    );
  };

  // Render QC result column
  const renderQCResult = (sample) => {
    if (sample.qcResult) {
      const isPassed = sample.qcResult === "PASS";
      return (
        <div style={{ fontSize: "12px" }}>
          <Tag type={isPassed ? "green" : "red"} size="sm">
            {isPassed ? (
              <>
                <CheckmarkFilled size={12} style={{ marginRight: "4px" }} />
                <FormattedMessage
                  id="notebook.mntd.processingqc.qcPassed"
                  defaultMessage="QC Passed"
                />
              </>
            ) : (
              <>
                <WarningAltFilled size={12} style={{ marginRight: "4px" }} />
                <FormattedMessage
                  id="notebook.mntd.processingqc.qcFailed"
                  defaultMessage="QC Failed"
                />
              </>
            )}
          </Tag>
          {!isPassed && sample.failedDecision && (
            <div style={{ marginTop: "2px" }}>
              <Tag type="gray" size="sm">
                {failedDecisionOptions.find(
                  (d) => d.id === sample.failedDecision,
                )?.text || sample.failedDecision}
              </Tag>
            </div>
          )}
          {sample.ctValue && (
            <div style={{ marginTop: "2px", color: "#525252" }}>
              Ct: {sample.ctValue}
            </div>
          )}
        </div>
      );
    }
    return (
      <span style={{ color: "#8d8d8d", fontSize: "12px" }}>
        <FormattedMessage
          id="notebook.mntd.processingqc.pendingQC"
          defaultMessage="Pending QC"
        />
      </span>
    );
  };

  return (
    <div className="mntd-processing-qc-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.page.mntd.processingqc.title"
            defaultMessage="Processing & Quality Control"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.mntd.processingqc.description"
            defaultMessage="Execute lab processes and verify sample integrity through quality control checks. Record processing protocols, timing, and QC metrics."
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
                  id="notebook.mntd.processingqc.totalSamples"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{stats.total}</span>
            </Tile>
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.mntd.processingqc.processed"
                  defaultMessage="Processed"
                />
              </span>
              <span className="progress-value">{stats.processed}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.mntd.processingqc.qcPassed"
                  defaultMessage="QC Passed"
                />
              </span>
              <span className="progress-value">{stats.qcPassed}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.mntd.processingqc.qcFailed"
                  defaultMessage="QC Failed"
                />
              </span>
              <span className="progress-value">{stats.qcFailed}</span>
            </Tile>
          </div>
        </Column>
      </Grid>

      {/* Notifications */}
      {error && (
        <InlineNotification
          kind="error"
          title={error}
          hideCloseButton={false}
          lowContrast
          onClose={() => setError(null)}
          style={{ marginBottom: "1rem" }}
        />
      )}

      {success && (
        <InlineNotification
          kind="success"
          title={success}
          hideCloseButton={false}
          lowContrast
          onClose={() => setSuccess(null)}
          style={{ marginBottom: "1rem" }}
        />
      )}

      {/* Action Buttons */}
      <div className="page-actions-bar">
        <Button
          kind="primary"
          size="sm"
          renderIcon={Chemistry}
          onClick={handleOpenProcessingModal}
          disabled={selectedIds.length === 0}
        >
          <FormattedMessage
            id="notebook.mntd.processingqc.recordProcessing"
            defaultMessage="Record Processing ({count} selected)"
            values={{ count: selectedIds.length }}
          />
        </Button>

        <Button
          kind="secondary"
          size="sm"
          renderIcon={Activity}
          onClick={handleOpenQCModal}
          disabled={selectedIds.length === 0}
        >
          <FormattedMessage
            id="notebook.mntd.processingqc.recordQC"
            defaultMessage="Record QC Result ({count} selected)"
            values={{ count: selectedIds.length }}
          />
        </Button>

        {selectedIds.length > 0 && (
          <Button
            kind="tertiary"
            size="sm"
            renderIcon={CheckmarkFilled}
            onClick={handleBulkMarkQCPassed}
          >
            <FormattedMessage
              id="notebook.mntd.processingqc.markQCPassed"
              defaultMessage="Mark QC Passed ({count})"
              values={{ count: selectedIds.length }}
            />
          </Button>
        )}

        <Button
          kind="ghost"
          size="sm"
          renderIcon={Renew}
          onClick={loadPageSamples}
        >
          <FormattedMessage
            id="notebook.mntd.processingqc.refresh"
            defaultMessage="Refresh"
          />
        </Button>
      </div>

      {/* Sample Grid */}
      <div className="sample-grid-container">
        <SampleGrid
          gridId="mntd-processing-qc"
          samples={samples}
          selectedIds={selectedIds}
          onSelectionChange={setSelectedIds}
          onStatusChange={handleStatusChange}
          statusFilter={statusFilter}
          onStatusFilterChange={setStatusFilter}
          showSelection={true}
          loading={loading}
          additionalColumns={[
            {
              key: "processingInfo",
              header: intl.formatMessage({
                id: "notebook.mntd.processingqc.processingInfo",
                defaultMessage: "Processing Info",
              }),
              render: renderProcessingInfo,
            },
            {
              key: "qcResult",
              header: intl.formatMessage({
                id: "notebook.mntd.processingqc.qcResult",
                defaultMessage: "QC Result",
              }),
              render: renderQCResult,
            },
          ]}
        />
      </div>

      {/* Empty state */}
      {!loading && samples.length === 0 && (
        <div className="empty-state">
          <p>
            <FormattedMessage
              id="notebook.mntd.processingqc.empty"
              defaultMessage="No samples available for processing. Please complete the aliquoting step first."
            />
          </p>
        </div>
      )}

      {/* Record Processing Modal */}
      <Modal
        open={showProcessingModal}
        modalHeading={intl.formatMessage({
          id: "notebook.mntd.processingqc.modal.processingTitle",
          defaultMessage: "Record Processing Data",
        })}
        primaryButtonText={intl.formatMessage({
          id: "notebook.mntd.processingqc.modal.save",
          defaultMessage: "Save",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => setShowProcessingModal(false)}
        onRequestSubmit={handleSaveProcessingData}
        size="md"
      >
        <div style={{ marginBottom: "1rem" }}>
          <p style={{ color: "#525252", marginBottom: "1rem" }}>
            <FormattedMessage
              id="notebook.mntd.processingqc.modal.processingDescription"
              defaultMessage="Record processing execution data for {count} selected sample(s)."
              values={{ count: selectedIds.length }}
            />
          </p>

          {/* Protocol Selection */}
          <Dropdown
            id="protocol"
            titleText={intl.formatMessage({
              id: "notebook.mntd.processingqc.protocol",
              defaultMessage: "Protocol Used",
            })}
            label={intl.formatMessage({
              id: "notebook.mntd.processingqc.selectProtocol",
              defaultMessage: "Select protocol",
            })}
            items={protocolOptions}
            itemToString={(item) => (item ? item.text : "")}
            selectedItem={protocolOptions.find(
              (p) => p.id === processingData.protocol,
            )}
            onChange={({ selectedItem }) =>
              setProcessingData({
                ...processingData,
                protocol: selectedItem?.id || "",
              })
            }
            style={{ marginBottom: "1rem" }}
          />

          {/* Start Date/Time */}
          <div
            style={{
              padding: "1rem",
              backgroundColor: "#f4f4f4",
              borderRadius: "4px",
              marginBottom: "1rem",
            }}
          >
            <h5 style={{ marginBottom: "0.5rem" }}>
              <FormattedMessage
                id="notebook.mntd.processingqc.startTime"
                defaultMessage="Start Time"
              />
            </h5>
            <div style={{ display: "flex", gap: "1rem" }}>
              <DatePicker
                datePickerType="single"
                onChange={([date]) =>
                  setProcessingData({
                    ...processingData,
                    startDate: date?.toISOString().split("T")[0] || "",
                  })
                }
              >
                <DatePickerInput
                  id="start-date"
                  labelText={intl.formatMessage({
                    id: "notebook.mntd.processingqc.date",
                    defaultMessage: "Date",
                  })}
                  placeholder="mm/dd/yyyy"
                />
              </DatePicker>
              <TextInput
                id="start-time"
                labelText={intl.formatMessage({
                  id: "notebook.mntd.processingqc.time",
                  defaultMessage: "Time",
                })}
                value={processingData.startTime}
                onChange={(e) =>
                  setProcessingData({
                    ...processingData,
                    startTime: e.target.value,
                  })
                }
                placeholder="HH:MM"
              />
            </div>
          </div>

          {/* End Date/Time */}
          <div
            style={{
              padding: "1rem",
              backgroundColor: "#f4f4f4",
              borderRadius: "4px",
              marginBottom: "1rem",
            }}
          >
            <h5 style={{ marginBottom: "0.5rem" }}>
              <FormattedMessage
                id="notebook.mntd.processingqc.endTime"
                defaultMessage="End Time"
              />
            </h5>
            <div style={{ display: "flex", gap: "1rem" }}>
              <DatePicker
                datePickerType="single"
                onChange={([date]) =>
                  setProcessingData({
                    ...processingData,
                    endDate: date?.toISOString().split("T")[0] || "",
                  })
                }
              >
                <DatePickerInput
                  id="end-date"
                  labelText={intl.formatMessage({
                    id: "notebook.mntd.processingqc.date",
                    defaultMessage: "Date",
                  })}
                  placeholder="mm/dd/yyyy"
                />
              </DatePicker>
              <TextInput
                id="end-time"
                labelText={intl.formatMessage({
                  id: "notebook.mntd.processingqc.time",
                  defaultMessage: "Time",
                })}
                value={processingData.endTime}
                onChange={(e) =>
                  setProcessingData({
                    ...processingData,
                    endTime: e.target.value,
                  })
                }
                placeholder="HH:MM"
              />
            </div>
          </div>

          {/* Technician */}
          <TextInput
            id="technician"
            labelText={intl.formatMessage({
              id: "notebook.mntd.processingqc.technician",
              defaultMessage: "Technician",
            })}
            value={processingData.technician}
            onChange={(e) =>
              setProcessingData({
                ...processingData,
                technician: e.target.value,
              })
            }
            style={{ marginBottom: "1rem" }}
          />

          {/* Notes */}
          <TextArea
            id="processing-notes"
            labelText={intl.formatMessage({
              id: "notebook.mntd.processingqc.notes",
              defaultMessage: "Notes",
            })}
            value={processingData.notes}
            onChange={(e) =>
              setProcessingData({ ...processingData, notes: e.target.value })
            }
            rows={3}
          />
        </div>
      </Modal>

      {/* Record QC Modal */}
      <Modal
        open={showQCModal}
        modalHeading={intl.formatMessage({
          id: "notebook.mntd.processingqc.modal.qcTitle",
          defaultMessage: "Record Quality Control Data",
        })}
        primaryButtonText={intl.formatMessage({
          id: "notebook.mntd.processingqc.modal.save",
          defaultMessage: "Save",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => setShowQCModal(false)}
        onRequestSubmit={handleSaveQCData}
        size="md"
      >
        <div style={{ marginBottom: "1rem" }}>
          <p style={{ color: "#525252", marginBottom: "1rem" }}>
            <FormattedMessage
              id="notebook.mntd.processingqc.modal.qcDescription"
              defaultMessage="Record quality control results for {count} selected sample(s)."
              values={{ count: selectedIds.length }}
            />
          </p>

          {/* QC Result */}
          <div style={{ marginBottom: "1rem" }}>
            <RadioButtonGroup
              legendText={intl.formatMessage({
                id: "notebook.mntd.processingqc.qcResultLabel",
                defaultMessage: "QC Result",
              })}
              name="qc-result"
              valueSelected={qcData.qcResult}
              onChange={(value) => setQCData({ ...qcData, qcResult: value })}
            >
              <RadioButton
                id="qc-pass"
                labelText={intl.formatMessage({
                  id: "notebook.mntd.processingqc.pass",
                  defaultMessage: "Pass",
                })}
                value="PASS"
              />
              <RadioButton
                id="qc-fail"
                labelText={intl.formatMessage({
                  id: "notebook.mntd.processingqc.fail",
                  defaultMessage: "Fail",
                })}
                value="FAIL"
              />
            </RadioButtonGroup>
          </div>

          {/* QC Metrics */}
          <div
            style={{
              padding: "1rem",
              backgroundColor: "#e0f0e0",
              borderRadius: "4px",
              marginBottom: "1rem",
            }}
          >
            <h5 style={{ marginBottom: "0.5rem" }}>
              <FormattedMessage
                id="notebook.mntd.processingqc.qcMetrics"
                defaultMessage="QC Metrics"
              />
            </h5>
            <Grid fullWidth>
              <Column lg={8} md={4} sm={4}>
                <NumberInput
                  id="ct-value"
                  label={intl.formatMessage({
                    id: "notebook.mntd.processingqc.ctValue",
                    defaultMessage: "Ct Value",
                  })}
                  value={qcData.ctValue}
                  onChange={(e, { value }) =>
                    setQCData({ ...qcData, ctValue: value })
                  }
                  min={0}
                  step={0.1}
                  allowEmpty
                />
              </Column>
              <Column lg={8} md={4} sm={4}>
                <TextInput
                  id="yield"
                  labelText={intl.formatMessage({
                    id: "notebook.mntd.processingqc.yield",
                    defaultMessage: "Yield (ng/uL)",
                  })}
                  value={qcData.yield}
                  onChange={(e) =>
                    setQCData({ ...qcData, yield: e.target.value })
                  }
                />
              </Column>
              <Column lg={8} md={4} sm={4}>
                <TextInput
                  id="integrity"
                  labelText={intl.formatMessage({
                    id: "notebook.mntd.processingqc.integrity",
                    defaultMessage: "Integrity (RIN/DV200)",
                  })}
                  value={qcData.integrity}
                  onChange={(e) =>
                    setQCData({ ...qcData, integrity: e.target.value })
                  }
                />
              </Column>
              <Column lg={8} md={4} sm={4}>
                <TextInput
                  id="other-metrics"
                  labelText={intl.formatMessage({
                    id: "notebook.mntd.processingqc.otherMetrics",
                    defaultMessage: "Other Metrics",
                  })}
                  value={qcData.otherMetrics}
                  onChange={(e) =>
                    setQCData({ ...qcData, otherMetrics: e.target.value })
                  }
                />
              </Column>
            </Grid>
          </div>

          {/* Failed Decision - shown only when FAIL is selected */}
          {qcData.qcResult === "FAIL" && (
            <div
              style={{
                padding: "1rem",
                backgroundColor: "#fff0f0",
                borderRadius: "4px",
                marginBottom: "1rem",
              }}
            >
              <h5 style={{ marginBottom: "0.5rem", color: "#da1e28" }}>
                <FormattedMessage
                  id="notebook.mntd.processingqc.failedAction"
                  defaultMessage="Action for Failed Samples"
                />
              </h5>
              <Dropdown
                id="failed-decision"
                titleText={intl.formatMessage({
                  id: "notebook.mntd.processingqc.decision",
                  defaultMessage: "Decision",
                })}
                label={intl.formatMessage({
                  id: "notebook.mntd.processingqc.selectDecision",
                  defaultMessage: "Select action",
                })}
                items={failedDecisionOptions}
                itemToString={(item) => (item ? item.text : "")}
                selectedItem={failedDecisionOptions.find(
                  (d) => d.id === qcData.failedDecision,
                )}
                onChange={({ selectedItem }) =>
                  setQCData({
                    ...qcData,
                    failedDecision: selectedItem?.id || "",
                  })
                }
              />
            </div>
          )}

          {/* QC Technician */}
          <TextInput
            id="qc-technician"
            labelText={intl.formatMessage({
              id: "notebook.mntd.processingqc.qcTechnician",
              defaultMessage: "QC Technician",
            })}
            value={qcData.qcTechnician}
            onChange={(e) =>
              setQCData({ ...qcData, qcTechnician: e.target.value })
            }
            style={{ marginBottom: "1rem" }}
          />

          {/* QC Date */}
          <DatePicker
            datePickerType="single"
            onChange={([date]) =>
              setQCData({
                ...qcData,
                qcDate: date?.toISOString().split("T")[0] || "",
              })
            }
          >
            <DatePickerInput
              id="qc-date"
              labelText={intl.formatMessage({
                id: "notebook.mntd.processingqc.qcDate",
                defaultMessage: "QC Date",
              })}
              placeholder="mm/dd/yyyy"
            />
          </DatePicker>

          {/* QC Notes */}
          <TextArea
            id="qc-notes"
            labelText={intl.formatMessage({
              id: "notebook.mntd.processingqc.qcNotes",
              defaultMessage: "QC Notes",
            })}
            value={qcData.qcNotes}
            onChange={(e) => setQCData({ ...qcData, qcNotes: e.target.value })}
            rows={3}
            style={{ marginTop: "1rem" }}
          />
        </div>
      </Modal>
    </div>
  );
}

export default MNTDProcessingQCPage;
