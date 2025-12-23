import { useState, useEffect, useRef, useCallback } from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  InlineNotification,
  Modal,
  TextArea,
  RadioButtonGroup,
  RadioButton,
  Tag,
  TextInput,
  Checkbox,
  Accordion,
  AccordionItem,
  NumberInput,
  Select,
  SelectItem,
} from "@carbon/react";
import {
  Checkmark,
  Edit,
  WarningAlt,
  Information,
  User,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
} from "../../../utils/Utils";
import SampleGrid from "../../workflow/SampleGrid";
import "../../workflow/NotebookWorkflow.css";

/**
 * PathologyQualityControlPage - Page 2 of the pathology workflow.
 * STAGE 2: Sample Quality Control - Performed by Lab Technicians
 *
 * Initial Inspection:
 * - Match requisition with sample labeling
 * - Container integrity check
 * - Volume/quantity assessment
 *
 * Histology-Specific QC:
 * - Fixation: 10% Neutral Buffered Formalin (NBF)
 * - Fixative-to-tissue ratio: 10:1
 * - Fixation duration: Adequate (typically 6-24 hours depending on size)
 * - Tissue integrity: No autolysis, proper orientation
 *
 * Cytology-Specific QC:
 * - Container integrity (no leakage)
 * - Preservative type correct (e.g., Cytolyt)
 * - Adequate volume
 * - No clot formation (if fluid sample)
 *
 * Peripheral Blood QC:
 * - Check for clots in EDTA tubes (fail if clotted)
 *
 * Research Sample QC:
 * - Verify consent documentation
 * - Confirm sample type matches requisition
 * - Check storage medium (e.g., OCT, RNAlater)
 *
 * Tissue Block QC (Post-Embedding):
 * - Pass: Smooth surface, correct depth/orientation, no paraffin overflow
 * - Fail: Cracks, bubbles, shallow/deep tissue, paraffin obscuring tissue
 * - Action: Re-embed or melt block; document in logbook; escalate to pathologist if unusable
 *
 * Documentation:
 * - QC status (Pass/Fail) in Master Accession Ledger
 * - Staff initials and date
 *
 * Actions for Failed QC:
 * - Notify submitter (clinician/PI) and lab head/pathologist
 * - Document actions: "Recollection requested", "Processed with limitations", "PI decides usability"
 */
function PathologyQualityControlPage({
  entryId,
  pageData,
  progress: _progress,
  onProgressUpdate,
}) {
  const intl = useIntl();
  const componentMounted = useRef(false);

  // State for samples
  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);

  // Bulk apply modal state
  const [bulkApplyModalOpen, setBulkApplyModalOpen] = useState(false);
  const [isBulkApplying, setIsBulkApplying] = useState(false);

  // Bulk apply form values - comprehensive QC criteria
  const [bulkApplyValues, setBulkApplyValues] = useState({
    // Documentation
    staffInitials: "",
    qcDate: new Date().toISOString().slice(0, 10),

    // Initial Inspection
    requisitionMatches: false,
    containerIntegrity: false,
    volumeAdequate: false,

    // Histology-Specific QC
    fixationType: "",
    fixativeRatio: "",
    fixationDuration: "",
    tissueIntegrity: false,
    noAutolysis: false,
    properOrientation: false,

    // Cytology-Specific QC
    cytologyContainerIntegrity: false,
    preservativeTypeCorrect: false,
    cytologyVolumeAdequate: false,
    noClotFormation: false,

    // Peripheral Blood QC
    noClotInEDTA: false,

    // Research Sample QC
    consentVerified: false,
    sampleTypeMatchesRequisition: false,
    storageMediumCorrect: false,
    storageMediumType: "",

    // Tissue Block QC (Post-Embedding)
    blockSurfaceSmooth: false,
    correctDepthOrientation: false,
    noParaffinOverflow: false,
    noCracks: false,
    noBubbles: false,

    // QC Result
    qcResult: "",

    // Fail-specific fields
    failReason: "",
    failAction: "",
    qcRemarks: "",
  });

  // Check if page has real ID
  const hasRealPageId =
    pageData?.id && !String(pageData.id).startsWith("default-");

  // Load samples for this page
  useEffect(() => {
    componentMounted.current = true;
    loadPageSamples();

    return () => {
      componentMounted.current = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
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
              collectionDate: sample.collectionDate,
              status: sample.pageStatus || "PENDING",
              sampleCategory: sample.sampleCategory || "Clinical diagnostic",
              // QC data from page data
              staffInitials: sample.data?.staffInitials,
              qcDate: sample.data?.qcDate,
              qcResult: sample.data?.qcResult,
              qcRemarks: sample.data?.qcRemarks,
              failReason: sample.data?.failReason,
              failAction: sample.data?.failAction,
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

  // Reset bulk apply values
  const resetBulkApplyValues = () => {
    setBulkApplyValues({
      staffInitials: "",
      qcDate: new Date().toISOString().slice(0, 10),
      // Initial Inspection
      requisitionMatches: false,
      containerIntegrity: false,
      volumeAdequate: false,
      // Histology-Specific
      fixationType: "",
      fixativeRatio: "",
      fixationDuration: "",
      tissueIntegrity: false,
      noAutolysis: false,
      properOrientation: false,
      // Cytology-Specific
      cytologyContainerIntegrity: false,
      preservativeTypeCorrect: false,
      cytologyVolumeAdequate: false,
      noClotFormation: false,
      // Peripheral Blood
      noClotInEDTA: false,
      // Research Sample
      consentVerified: false,
      sampleTypeMatchesRequisition: false,
      storageMediumCorrect: false,
      storageMediumType: "",
      // Tissue Block
      blockSurfaceSmooth: false,
      correctDepthOrientation: false,
      noParaffinOverflow: false,
      noCracks: false,
      noBubbles: false,
      // Result
      qcResult: "",
      failReason: "",
      failAction: "",
      qcRemarks: "",
    });
  };

  // Handle bulk apply
  const handleBulkApply = useCallback(() => {
    if (selectedSampleIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "pathology.qc.error.noSelection",
          defaultMessage: "Please select samples to apply QC values to.",
        }),
      );
      return;
    }

    if (!hasRealPageId) {
      setError(
        intl.formatMessage({
          id: "pathology.qc.error.noPage",
          defaultMessage:
            "Cannot update samples: Page not properly initialized.",
        }),
      );
      return;
    }

    // Validate QC result is selected
    if (!bulkApplyValues.qcResult) {
      setError(
        intl.formatMessage({
          id: "pathology.qc.error.noResult",
          defaultMessage: "Please select a QC result (Pass or Fail).",
        }),
      );
      return;
    }

    // Validate fail reason and action if failed
    if (bulkApplyValues.qcResult === "Fail" && !bulkApplyValues.failReason) {
      setError(
        intl.formatMessage({
          id: "pathology.qc.error.noFailReason",
          defaultMessage: "Please specify a reason for failed QC.",
        }),
      );
      return;
    }

    if (bulkApplyValues.qcResult === "Fail" && !bulkApplyValues.failAction) {
      setError(
        intl.formatMessage({
          id: "pathology.qc.error.noFailAction",
          defaultMessage: "Please select an action for failed samples.",
        }),
      );
      return;
    }

    setIsBulkApplying(true);
    setError(null);

    // Prepare the data to apply
    const data = {
      staffInitials: bulkApplyValues.staffInitials,
      qcDate: bulkApplyValues.qcDate,
      // Initial Inspection
      requisitionMatches: bulkApplyValues.requisitionMatches,
      containerIntegrity: bulkApplyValues.containerIntegrity,
      volumeAdequate: bulkApplyValues.volumeAdequate,
      // Histology-Specific
      fixationType: bulkApplyValues.fixationType,
      fixativeRatio: bulkApplyValues.fixativeRatio,
      fixationDuration: bulkApplyValues.fixationDuration,
      tissueIntegrity: bulkApplyValues.tissueIntegrity,
      noAutolysis: bulkApplyValues.noAutolysis,
      properOrientation: bulkApplyValues.properOrientation,
      // Cytology-Specific
      cytologyContainerIntegrity: bulkApplyValues.cytologyContainerIntegrity,
      preservativeTypeCorrect: bulkApplyValues.preservativeTypeCorrect,
      cytologyVolumeAdequate: bulkApplyValues.cytologyVolumeAdequate,
      noClotFormation: bulkApplyValues.noClotFormation,
      // Peripheral Blood
      noClotInEDTA: bulkApplyValues.noClotInEDTA,
      // Research Sample
      consentVerified: bulkApplyValues.consentVerified,
      sampleTypeMatchesRequisition:
        bulkApplyValues.sampleTypeMatchesRequisition,
      storageMediumCorrect: bulkApplyValues.storageMediumCorrect,
      storageMediumType: bulkApplyValues.storageMediumType,
      // Tissue Block
      blockSurfaceSmooth: bulkApplyValues.blockSurfaceSmooth,
      correctDepthOrientation: bulkApplyValues.correctDepthOrientation,
      noParaffinOverflow: bulkApplyValues.noParaffinOverflow,
      noCracks: bulkApplyValues.noCracks,
      noBubbles: bulkApplyValues.noBubbles,
      // Result
      qcResult: bulkApplyValues.qcResult,
    };

    // Add fail-specific fields if failed
    if (bulkApplyValues.qcResult === "Fail") {
      data.failReason = bulkApplyValues.failReason;
      data.failAction = bulkApplyValues.failAction;
      data.qcRemarks = bulkApplyValues.qcRemarks;
    }

    const applyData = {
      sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
      data: data,
    };

    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
      JSON.stringify(applyData),
      (status) => {
        setIsBulkApplying(false);
        if (status === 200) {
          setSuccessMessage(
            intl.formatMessage(
              {
                id: "pathology.qc.success.applied",
                defaultMessage: "Applied QC values to {count} samples.",
              },
              { count: selectedSampleIds.length },
            ),
          );
          setBulkApplyModalOpen(false);
          loadPageSamples();
          setSelectedSampleIds([]);
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(
            intl.formatMessage({
              id: "pathology.qc.error.apply",
              defaultMessage: "Failed to apply QC values. Please try again.",
            }),
          );
        }
      },
    );
  }, [
    selectedSampleIds,
    hasRealPageId,
    pageData?.id,
    bulkApplyValues,
    intl,
    loadPageSamples,
    onProgressUpdate,
  ]);

  // Handle marking samples as QC complete
  const handleMarkComplete = useCallback(() => {
    if (selectedSampleIds.length === 0) return;

    if (!hasRealPageId) {
      setError(
        intl.formatMessage({
          id: "pathology.qc.error.noPage",
          defaultMessage:
            "Cannot update samples: Page not properly initialized.",
        }),
      );
      return;
    }

    // Check if all selected samples have QC result
    const selectedSamples = samples.filter((s) =>
      selectedSampleIds.includes(s.id),
    );
    const missingQC = selectedSamples.filter((s) => !s.qcResult);
    if (missingQC.length > 0) {
      setError(
        intl.formatMessage(
          {
            id: "pathology.qc.error.missingQC",
            defaultMessage:
              "{count} sample(s) are missing QC result. Please complete QC first.",
          },
          { count: missingQC.length },
        ),
      );
      return;
    }

    // Separate samples by their fail action
    const discardedSamples = selectedSamples.filter(
      (s) => s.failAction === "discard",
    );
    const proceedingSamples = selectedSamples.filter(
      (s) => s.failAction !== "discard",
    );

    let completedRequests = 0;
    let failedRequests = 0;
    const totalRequests =
      (proceedingSamples.length > 0 ? 1 : 0) +
      (discardedSamples.length > 0 ? 1 : 0);

    const handleRequestComplete = () => {
      completedRequests++;
      if (completedRequests === totalRequests) {
        if (failedRequests === 0) {
          let message = "";
          if (proceedingSamples.length > 0 && discardedSamples.length > 0) {
            message = intl.formatMessage(
              {
                id: "pathology.qc.success.completedMixed",
                defaultMessage:
                  "{passCount} sample(s) passed QC and can proceed. {failCount} sample(s) discarded.",
              },
              {
                passCount: proceedingSamples.length,
                failCount: discardedSamples.length,
              },
            );
          } else if (discardedSamples.length > 0) {
            message = intl.formatMessage(
              {
                id: "pathology.qc.success.completedDiscarded",
                defaultMessage: "{count} sample(s) marked as discarded.",
              },
              { count: discardedSamples.length },
            );
          } else {
            message = intl.formatMessage(
              {
                id: "pathology.qc.success.completed",
                defaultMessage:
                  "Marked {count} sample(s) as QC complete. They can proceed to processing.",
              },
              { count: proceedingSamples.length },
            );
          }
          setSuccessMessage(message);
          setSelectedSampleIds([]);
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(
            intl.formatMessage({
              id: "pathology.qc.error.status",
              defaultMessage: "Failed to update status. Please try again.",
            }),
          );
        }
      }
    };

    // Update proceeding samples to COMPLETED
    if (proceedingSamples.length > 0) {
      postToOpenElisServer(
        `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
        JSON.stringify({
          sampleIds: proceedingSamples.map((s) => parseInt(s.id, 10)),
          status: "COMPLETED",
        }),
        (status) => {
          if (status !== 200) {
            failedRequests++;
          }
          handleRequestComplete();
        },
      );
    }

    // Update discarded samples to SKIPPED
    if (discardedSamples.length > 0) {
      postToOpenElisServer(
        `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
        JSON.stringify({
          sampleIds: discardedSamples.map((s) => parseInt(s.id, 10)),
          status: "SKIPPED",
        }),
        (status) => {
          if (status !== 200) {
            failedRequests++;
          }
          handleRequestComplete();
        },
      );
    }

    if (totalRequests === 0) {
      return;
    }
  }, [
    selectedSampleIds,
    samples,
    hasRealPageId,
    intl,
    loadPageSamples,
    onProgressUpdate,
    pageData?.id,
  ]);

  // Calculate stats
  const qcPassedCount = samples.filter((s) => s.qcResult === "Pass").length;
  const qcFailedCount = samples.filter((s) => s.qcResult === "Fail").length;
  const qcPendingCount = samples.filter((s) => !s.qcResult).length;
  const completedCount = samples.filter(
    (s) => s.status === "COMPLETED" || s.status === "SKIPPED",
  ).length;

  // Get QC result tag
  const getQCTag = (qcResult) => {
    if (!qcResult) return <Tag type="gray">Pending</Tag>;
    if (qcResult === "Pass") return <Tag type="green">Pass</Tag>;
    if (qcResult === "Fail") return <Tag type="red">Fail</Tag>;
    return <Tag type="gray">{qcResult}</Tag>;
  };

  // Get fail action display
  const getFailActionDisplay = (failAction) => {
    if (!failAction) return "-";
    const actionLabels = {
      recollection_requested: "Recollection requested",
      processed_with_limitations: "Processed with limitations",
      pi_decides: "PI decides usability",
      notify_submitter: "Notify submitter",
      re_embed: "Re-embed block",
      escalate_pathologist: "Escalate to pathologist",
      discard: "Discard",
    };
    return actionLabels[failAction] || failAction;
  };

  return (
    <div className="pathology-qc-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="pathology.page.qc.title"
            defaultMessage="Sample Quality Control"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="pathology.page.qc.description"
            defaultMessage="STAGE 2: Verify sample suitability before processing. Performed by Lab Technicians. Document QC status (Pass/Fail) in Master Accession Ledger with staff initials and date."
          />
        </p>
      </div>

      {/* QC Overview Info Box */}
      <div className="info-box" style={{ marginBottom: "1rem" }}>
        <div className="info-header">
          <Information size={16} />
          <span style={{ marginLeft: "0.5rem", fontWeight: 600 }}>
            <FormattedMessage
              id="pathology.qc.overview.title"
              defaultMessage="Quality Control Checklist Overview"
            />
          </span>
        </div>
        <div style={{ fontSize: "0.875rem", marginTop: "0.5rem" }}>
          <strong>Initial Inspection:</strong> Requisition match, container
          integrity, volume assessment
          <br />
          <strong>Specimen-Specific QC:</strong> Histology, Cytology, Blood,
          Research samples, Tissue blocks
          <br />
          <strong>Actions for Failed QC:</strong> Notify submitter, document
          actions, escalate if needed
        </div>
      </div>

      {/* Progress Summary */}
      <Grid fullWidth className="progress-section">
        <Column lg={16} md={8} sm={4}>
          <div className="progress-tiles">
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="pathology.qc.totalSamples"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{samples.length}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="pathology.qc.qcPassed"
                  defaultMessage="QC Passed"
                />
              </span>
              <span className="progress-value">{qcPassedCount}</span>
            </Tile>
            <Tile className="progress-tile error">
              <span className="progress-label">
                <FormattedMessage
                  id="pathology.qc.qcFailed"
                  defaultMessage="QC Failed"
                />
              </span>
              <span className="progress-value">{qcFailedCount}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="pathology.qc.qcPending"
                  defaultMessage="QC Pending"
                />
              </span>
              <span className="progress-value">{qcPendingCount}</span>
            </Tile>
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="pathology.qc.completed"
                  defaultMessage="Completed"
                />
              </span>
              <span className="progress-value">{completedCount}</span>
            </Tile>
          </div>
        </Column>
      </Grid>

      {/* Action Buttons */}
      <div className="page-actions-bar">
        <Button
          kind="primary"
          size="sm"
          renderIcon={Edit}
          onClick={() => {
            resetBulkApplyValues();
            setBulkApplyModalOpen(true);
          }}
          disabled={selectedSampleIds.length === 0}
        >
          <FormattedMessage
            id="pathology.qc.bulkApply"
            defaultMessage="Perform QC ({count})"
            values={{ count: selectedSampleIds.length }}
          />
        </Button>

        {selectedSampleIds.length > 0 && (
          <Button
            kind="secondary"
            size="sm"
            renderIcon={Checkmark}
            onClick={handleMarkComplete}
          >
            <FormattedMessage
              id="pathology.qc.markComplete"
              defaultMessage="Mark QC Complete ({count})"
              values={{ count: selectedSampleIds.length }}
            />
          </Button>
        )}
      </div>

      {/* Messages */}
      {error && (
        <InlineNotification
          kind="error"
          title={error}
          onClose={() => setError(null)}
          lowContrast
        />
      )}
      {successMessage && (
        <InlineNotification
          kind="success"
          title={successMessage}
          onClose={() => setSuccessMessage(null)}
          lowContrast
        />
      )}

      {/* Pending QC Samples Table */}
      <div className="sample-table-section">
        <div className="table-section-header">
          <h5>
            <FormattedMessage
              id="pathology.qc.pendingTable.title"
              defaultMessage="Samples Pending QC"
            />
            <Tag type="gray" className="count-tag">
              {
                samples.filter(
                  (s) => s.status !== "COMPLETED" && s.status !== "SKIPPED",
                ).length
              }
            </Tag>
          </h5>
          <p className="table-section-description">
            <FormattedMessage
              id="pathology.qc.pendingTable.description"
              defaultMessage="Select samples and use 'Perform QC' to complete quality control assessment."
            />
          </p>
        </div>
        <div className="sample-grid-container">
          <SampleGrid
            gridId="pending-qc"
            samples={samples.filter(
              (s) => s.status !== "COMPLETED" && s.status !== "SKIPPED",
            )}
            selectedIds={selectedSampleIds}
            onSelectionChange={setSelectedSampleIds}
            statusFilter={statusFilter}
            onStatusFilterChange={setStatusFilter}
            showSelection={true}
            loading={loading}
            columns={[
              {
                key: "externalId",
                header: intl.formatMessage({
                  id: "notebook.grid.sampleId",
                  defaultMessage: "Sample ID",
                }),
              },
              {
                key: "sampleType",
                header: intl.formatMessage({
                  id: "notebook.grid.sampleType",
                  defaultMessage: "Sample Type",
                }),
              },
              {
                key: "sampleCategory",
                header: intl.formatMessage({
                  id: "notebook.grid.category",
                  defaultMessage: "Category",
                }),
              },
              {
                key: "qcResult",
                header: intl.formatMessage({
                  id: "notebook.grid.qcResult",
                  defaultMessage: "QC Result",
                }),
                render: (value) => getQCTag(value),
              },
              {
                key: "staffInitials",
                header: intl.formatMessage({
                  id: "notebook.grid.staffInitials",
                  defaultMessage: "Staff",
                }),
              },
              {
                key: "failReason",
                header: intl.formatMessage({
                  id: "notebook.grid.failReason",
                  defaultMessage: "Fail Reason",
                }),
                render: (value) => value || "-",
              },
            ]}
          />
        </div>
        {!loading &&
          samples.filter(
            (s) => s.status !== "COMPLETED" && s.status !== "SKIPPED",
          ).length === 0 && (
            <div className="empty-table-state">
              <p>
                <FormattedMessage
                  id="pathology.qc.pendingTable.empty"
                  defaultMessage="No samples pending QC. All samples have been processed."
                />
              </p>
            </div>
          )}
      </div>

      {/* Completed QC Samples Table */}
      <div className="sample-table-section">
        <div className="table-section-header">
          <h5>
            <FormattedMessage
              id="pathology.qc.completedTable.title"
              defaultMessage="QC Completed Samples"
            />
            <Tag type="green" className="count-tag">
              {
                samples.filter(
                  (s) => s.status === "COMPLETED" || s.status === "SKIPPED",
                ).length
              }
            </Tag>
          </h5>
          <p className="table-section-description">
            <FormattedMessage
              id="pathology.qc.completedTable.description"
              defaultMessage="Samples that have completed quality control. Passed samples can proceed to processing."
            />
          </p>
        </div>
        <div className="sample-grid-container">
          <SampleGrid
            gridId="completed-qc"
            samples={samples.filter(
              (s) => s.status === "COMPLETED" || s.status === "SKIPPED",
            )}
            selectedIds={[]}
            showSelection={false}
            loading={loading}
            columns={[
              {
                key: "externalId",
                header: intl.formatMessage({
                  id: "notebook.grid.sampleId",
                  defaultMessage: "Sample ID",
                }),
              },
              {
                key: "sampleType",
                header: intl.formatMessage({
                  id: "notebook.grid.sampleType",
                  defaultMessage: "Sample Type",
                }),
              },
              {
                key: "qcResult",
                header: intl.formatMessage({
                  id: "notebook.grid.qcResult",
                  defaultMessage: "QC Result",
                }),
                render: (value) => getQCTag(value),
              },
              {
                key: "staffInitials",
                header: intl.formatMessage({
                  id: "notebook.grid.staffInitials",
                  defaultMessage: "Staff",
                }),
              },
              {
                key: "qcDate",
                header: intl.formatMessage({
                  id: "notebook.grid.qcDate",
                  defaultMessage: "QC Date",
                }),
              },
              {
                key: "failAction",
                header: intl.formatMessage({
                  id: "notebook.grid.disposition",
                  defaultMessage: "Disposition",
                }),
                render: (value, row) => {
                  if (row.status === "SKIPPED") {
                    return (
                      <Tag type="red">
                        <FormattedMessage
                          id="notebook.disposition.discarded"
                          defaultMessage="Discarded"
                        />
                      </Tag>
                    );
                  }
                  if (row.qcResult === "Pass") {
                    return (
                      <Tag type="green">
                        <FormattedMessage
                          id="notebook.disposition.proceeding"
                          defaultMessage="Proceeding"
                        />
                      </Tag>
                    );
                  }
                  return getFailActionDisplay(value);
                },
              },
            ]}
          />
        </div>
        {!loading &&
          samples.filter(
            (s) => s.status === "COMPLETED" || s.status === "SKIPPED",
          ).length === 0 && (
            <div className="empty-table-state">
              <p>
                <FormattedMessage
                  id="pathology.qc.completedTable.empty"
                  defaultMessage="No samples have completed QC yet."
                />
              </p>
            </div>
          )}
      </div>

      {/* Global empty state */}
      {!loading && samples.length === 0 && (
        <div className="empty-state">
          <p>
            <FormattedMessage
              id="pathology.qc.empty"
              defaultMessage="No samples available for QC. Samples must be created on Page 1 (Sample Creation & Metadata Capture) first."
            />
          </p>
        </div>
      )}

      {/* Bulk QC Apply Modal */}
      <Modal
        open={bulkApplyModalOpen}
        onRequestClose={() => setBulkApplyModalOpen(false)}
        modalHeading={intl.formatMessage({
          id: "pathology.qc.modal.title",
          defaultMessage: "Sample Quality Control Assessment",
        })}
        primaryButtonText={
          isBulkApplying
            ? intl.formatMessage({
                id: "label.applying",
                defaultMessage: "Applying...",
              })
            : bulkApplyValues.qcResult === "Pass"
              ? intl.formatMessage({
                  id: "pathology.qc.action.pass",
                  defaultMessage: "Pass - Proceed to Processing",
                })
              : bulkApplyValues.qcResult === "Fail"
                ? intl.formatMessage({
                    id: "pathology.qc.action.fail",
                    defaultMessage: "Fail - Apply Action",
                  })
                : intl.formatMessage({
                    id: "label.apply",
                    defaultMessage: "Apply QC Result",
                  })
        }
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestSubmit={handleBulkApply}
        onSecondarySubmit={() => setBulkApplyModalOpen(false)}
        size="lg"
        primaryButtonDisabled={isBulkApplying || !bulkApplyValues.qcResult}
        danger={bulkApplyValues.qcResult === "Fail"}
      >
        <div className="qc-bulk-apply-modal">
          <p className="modal-description">
            <FormattedMessage
              id="pathology.qc.modal.description"
              defaultMessage="Perform quality control assessment on {count} selected sample(s). Complete applicable sections based on specimen type."
              values={{ count: selectedSampleIds.length }}
            />
          </p>

          {/* Documentation Section */}
          <div className="qc-section">
            <h5 className="qc-section-header">
              <User size={16} style={{ marginRight: "0.5rem" }} />
              <FormattedMessage
                id="pathology.qc.section.documentation"
                defaultMessage="Documentation"
              />
            </h5>
            <Grid fullWidth>
              <Column lg={8} md={4} sm={4}>
                <TextInput
                  id="staffInitials"
                  labelText={intl.formatMessage({
                    id: "pathology.qc.staffInitials",
                    defaultMessage: "Staff Initials *",
                  })}
                  value={bulkApplyValues.staffInitials}
                  onChange={(e) =>
                    setBulkApplyValues((prev) => ({
                      ...prev,
                      staffInitials: e.target.value,
                    }))
                  }
                  placeholder="e.g., JD"
                />
              </Column>
              <Column lg={8} md={4} sm={4}>
                <div className="cds--form-item">
                  <label className="cds--label">
                    <FormattedMessage
                      id="pathology.qc.qcDate"
                      defaultMessage="QC Date *"
                    />
                  </label>
                  <input
                    type="date"
                    className="cds--text-input"
                    value={bulkApplyValues.qcDate}
                    onChange={(e) =>
                      setBulkApplyValues((prev) => ({
                        ...prev,
                        qcDate: e.target.value,
                      }))
                    }
                  />
                </div>
              </Column>
            </Grid>
          </div>

          {/* Initial Inspection Section */}
          <Accordion>
            <AccordionItem
              title={intl.formatMessage({
                id: "pathology.qc.section.initialInspection",
                defaultMessage: "Initial Inspection (All Samples)",
              })}
              open
            >
              <Grid fullWidth>
                <Column lg={16} md={8} sm={4}>
                  <Checkbox
                    id="requisitionMatches"
                    labelText={intl.formatMessage({
                      id: "pathology.qc.requisitionMatches",
                      defaultMessage:
                        "Requisition matches sample labeling (patient name, ID, specimen type)",
                    })}
                    checked={bulkApplyValues.requisitionMatches}
                    onChange={(_, { checked }) =>
                      setBulkApplyValues((prev) => ({
                        ...prev,
                        requisitionMatches: checked,
                      }))
                    }
                  />
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <Checkbox
                    id="containerIntegrity"
                    labelText={intl.formatMessage({
                      id: "pathology.qc.containerIntegrity",
                      defaultMessage:
                        "Container integrity check passed (no leakage, damage)",
                    })}
                    checked={bulkApplyValues.containerIntegrity}
                    onChange={(_, { checked }) =>
                      setBulkApplyValues((prev) => ({
                        ...prev,
                        containerIntegrity: checked,
                      }))
                    }
                  />
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <Checkbox
                    id="volumeAdequate"
                    labelText={intl.formatMessage({
                      id: "pathology.qc.volumeAdequate",
                      defaultMessage: "Volume/quantity adequate for testing",
                    })}
                    checked={bulkApplyValues.volumeAdequate}
                    onChange={(_, { checked }) =>
                      setBulkApplyValues((prev) => ({
                        ...prev,
                        volumeAdequate: checked,
                      }))
                    }
                  />
                </Column>
              </Grid>
            </AccordionItem>

            {/* Histology-Specific QC */}
            <AccordionItem
              title={intl.formatMessage({
                id: "pathology.qc.section.histology",
                defaultMessage: "Histology-Specific QC",
              })}
            >
              <Grid fullWidth>
                <Column lg={8} md={4} sm={4}>
                  <Select
                    id="fixationType"
                    labelText={intl.formatMessage({
                      id: "pathology.qc.fixationType",
                      defaultMessage: "Fixation Type",
                    })}
                    value={bulkApplyValues.fixationType}
                    onChange={(e) =>
                      setBulkApplyValues((prev) => ({
                        ...prev,
                        fixationType: e.target.value,
                      }))
                    }
                  >
                    <SelectItem value="" text="Select fixation type" />
                    <SelectItem
                      value="10% NBF"
                      text="10% Neutral Buffered Formalin (NBF)"
                    />
                    <SelectItem value="Bouin's" text="Bouin's Fixative" />
                    <SelectItem value="Alcohol" text="Alcohol-based" />
                    <SelectItem value="Glutaraldehyde" text="Glutaraldehyde" />
                    <SelectItem value="Other" text="Other" />
                  </Select>
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <TextInput
                    id="fixativeRatio"
                    labelText={intl.formatMessage({
                      id: "pathology.qc.fixativeRatio",
                      defaultMessage: "Fixative-to-Tissue Ratio",
                    })}
                    value={bulkApplyValues.fixativeRatio}
                    onChange={(e) =>
                      setBulkApplyValues((prev) => ({
                        ...prev,
                        fixativeRatio: e.target.value,
                      }))
                    }
                    placeholder="e.g., 10:1"
                    helperText="Standard ratio is 10:1"
                  />
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <TextInput
                    id="fixationDuration"
                    labelText={intl.formatMessage({
                      id: "pathology.qc.fixationDuration",
                      defaultMessage: "Fixation Duration (hours)",
                    })}
                    value={bulkApplyValues.fixationDuration}
                    onChange={(e) =>
                      setBulkApplyValues((prev) => ({
                        ...prev,
                        fixationDuration: e.target.value,
                      }))
                    }
                    placeholder="e.g., 12"
                    helperText="Typically 6-24 hours depending on size"
                  />
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <Checkbox
                    id="tissueIntegrity"
                    labelText={intl.formatMessage({
                      id: "pathology.qc.tissueIntegrity",
                      defaultMessage: "Tissue integrity maintained",
                    })}
                    checked={bulkApplyValues.tissueIntegrity}
                    onChange={(_, { checked }) =>
                      setBulkApplyValues((prev) => ({
                        ...prev,
                        tissueIntegrity: checked,
                      }))
                    }
                  />
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <Checkbox
                    id="noAutolysis"
                    labelText={intl.formatMessage({
                      id: "pathology.qc.noAutolysis",
                      defaultMessage: "No autolysis observed",
                    })}
                    checked={bulkApplyValues.noAutolysis}
                    onChange={(_, { checked }) =>
                      setBulkApplyValues((prev) => ({
                        ...prev,
                        noAutolysis: checked,
                      }))
                    }
                  />
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <Checkbox
                    id="properOrientation"
                    labelText={intl.formatMessage({
                      id: "pathology.qc.properOrientation",
                      defaultMessage: "Proper tissue orientation",
                    })}
                    checked={bulkApplyValues.properOrientation}
                    onChange={(_, { checked }) =>
                      setBulkApplyValues((prev) => ({
                        ...prev,
                        properOrientation: checked,
                      }))
                    }
                  />
                </Column>
              </Grid>
            </AccordionItem>

            {/* Cytology-Specific QC */}
            <AccordionItem
              title={intl.formatMessage({
                id: "pathology.qc.section.cytology",
                defaultMessage: "Cytology-Specific QC",
              })}
            >
              <Grid fullWidth>
                <Column lg={8} md={4} sm={4}>
                  <Checkbox
                    id="cytologyContainerIntegrity"
                    labelText={intl.formatMessage({
                      id: "pathology.qc.cytologyContainerIntegrity",
                      defaultMessage: "Container integrity (no leakage)",
                    })}
                    checked={bulkApplyValues.cytologyContainerIntegrity}
                    onChange={(_, { checked }) =>
                      setBulkApplyValues((prev) => ({
                        ...prev,
                        cytologyContainerIntegrity: checked,
                      }))
                    }
                  />
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <Checkbox
                    id="preservativeTypeCorrect"
                    labelText={intl.formatMessage({
                      id: "pathology.qc.preservativeTypeCorrect",
                      defaultMessage:
                        "Preservative type correct (e.g., Cytolyt)",
                    })}
                    checked={bulkApplyValues.preservativeTypeCorrect}
                    onChange={(_, { checked }) =>
                      setBulkApplyValues((prev) => ({
                        ...prev,
                        preservativeTypeCorrect: checked,
                      }))
                    }
                  />
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <Checkbox
                    id="cytologyVolumeAdequate"
                    labelText={intl.formatMessage({
                      id: "pathology.qc.cytologyVolumeAdequate",
                      defaultMessage: "Adequate volume",
                    })}
                    checked={bulkApplyValues.cytologyVolumeAdequate}
                    onChange={(_, { checked }) =>
                      setBulkApplyValues((prev) => ({
                        ...prev,
                        cytologyVolumeAdequate: checked,
                      }))
                    }
                  />
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <Checkbox
                    id="noClotFormation"
                    labelText={intl.formatMessage({
                      id: "pathology.qc.noClotFormation",
                      defaultMessage: "No clot formation (if fluid sample)",
                    })}
                    checked={bulkApplyValues.noClotFormation}
                    onChange={(_, { checked }) =>
                      setBulkApplyValues((prev) => ({
                        ...prev,
                        noClotFormation: checked,
                      }))
                    }
                  />
                </Column>
              </Grid>
            </AccordionItem>

            {/* Peripheral Blood QC */}
            <AccordionItem
              title={intl.formatMessage({
                id: "pathology.qc.section.blood",
                defaultMessage: "Peripheral Blood QC",
              })}
            >
              <Grid fullWidth>
                <Column lg={16} md={8} sm={4}>
                  <Checkbox
                    id="noClotInEDTA"
                    labelText={intl.formatMessage({
                      id: "pathology.qc.noClotInEDTA",
                      defaultMessage:
                        "No clots in EDTA tubes (FAIL if clotted)",
                    })}
                    checked={bulkApplyValues.noClotInEDTA}
                    onChange={(_, { checked }) =>
                      setBulkApplyValues((prev) => ({
                        ...prev,
                        noClotInEDTA: checked,
                      }))
                    }
                  />
                </Column>
              </Grid>
            </AccordionItem>

            {/* Research Sample QC */}
            <AccordionItem
              title={intl.formatMessage({
                id: "pathology.qc.section.research",
                defaultMessage: "Research Sample QC",
              })}
            >
              <Grid fullWidth>
                <Column lg={8} md={4} sm={4}>
                  <Checkbox
                    id="consentVerified"
                    labelText={intl.formatMessage({
                      id: "pathology.qc.consentVerified",
                      defaultMessage: "Consent documentation verified",
                    })}
                    checked={bulkApplyValues.consentVerified}
                    onChange={(_, { checked }) =>
                      setBulkApplyValues((prev) => ({
                        ...prev,
                        consentVerified: checked,
                      }))
                    }
                  />
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <Checkbox
                    id="sampleTypeMatchesRequisition"
                    labelText={intl.formatMessage({
                      id: "pathology.qc.sampleTypeMatchesRequisition",
                      defaultMessage: "Sample type matches requisition",
                    })}
                    checked={bulkApplyValues.sampleTypeMatchesRequisition}
                    onChange={(_, { checked }) =>
                      setBulkApplyValues((prev) => ({
                        ...prev,
                        sampleTypeMatchesRequisition: checked,
                      }))
                    }
                  />
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <Checkbox
                    id="storageMediumCorrect"
                    labelText={intl.formatMessage({
                      id: "pathology.qc.storageMediumCorrect",
                      defaultMessage: "Storage medium correct",
                    })}
                    checked={bulkApplyValues.storageMediumCorrect}
                    onChange={(_, { checked }) =>
                      setBulkApplyValues((prev) => ({
                        ...prev,
                        storageMediumCorrect: checked,
                      }))
                    }
                  />
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <Select
                    id="storageMediumType"
                    labelText={intl.formatMessage({
                      id: "pathology.qc.storageMediumType",
                      defaultMessage: "Storage Medium Type",
                    })}
                    value={bulkApplyValues.storageMediumType}
                    onChange={(e) =>
                      setBulkApplyValues((prev) => ({
                        ...prev,
                        storageMediumType: e.target.value,
                      }))
                    }
                  >
                    <SelectItem value="" text="Select storage medium" />
                    <SelectItem
                      value="OCT"
                      text="OCT (Optimal Cutting Temperature)"
                    />
                    <SelectItem value="RNAlater" text="RNAlater" />
                    <SelectItem value="Formalin" text="Formalin" />
                    <SelectItem value="Snap Frozen" text="Snap Frozen" />
                    <SelectItem value="DMSO" text="DMSO" />
                    <SelectItem value="Other" text="Other" />
                  </Select>
                </Column>
              </Grid>
            </AccordionItem>

            {/* Tissue Block QC (Post-Embedding) */}
            <AccordionItem
              title={intl.formatMessage({
                id: "pathology.qc.section.tissueBlock",
                defaultMessage: "Tissue Block QC (Post-Embedding)",
              })}
            >
              <Grid fullWidth>
                <Column lg={16} md={8} sm={4}>
                  <p
                    style={{
                      fontSize: "0.875rem",
                      color: "#525252",
                      marginBottom: "1rem",
                    }}
                  >
                    <strong>Pass criteria:</strong> Smooth surface, correct
                    depth/orientation, no paraffin overflow
                    <br />
                    <strong>Fail criteria:</strong> Cracks, bubbles,
                    shallow/deep tissue, paraffin obscuring tissue
                  </p>
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <Checkbox
                    id="blockSurfaceSmooth"
                    labelText={intl.formatMessage({
                      id: "pathology.qc.blockSurfaceSmooth",
                      defaultMessage: "Block surface smooth",
                    })}
                    checked={bulkApplyValues.blockSurfaceSmooth}
                    onChange={(_, { checked }) =>
                      setBulkApplyValues((prev) => ({
                        ...prev,
                        blockSurfaceSmooth: checked,
                      }))
                    }
                  />
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <Checkbox
                    id="correctDepthOrientation"
                    labelText={intl.formatMessage({
                      id: "pathology.qc.correctDepthOrientation",
                      defaultMessage: "Correct depth and orientation",
                    })}
                    checked={bulkApplyValues.correctDepthOrientation}
                    onChange={(_, { checked }) =>
                      setBulkApplyValues((prev) => ({
                        ...prev,
                        correctDepthOrientation: checked,
                      }))
                    }
                  />
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <Checkbox
                    id="noParaffinOverflow"
                    labelText={intl.formatMessage({
                      id: "pathology.qc.noParaffinOverflow",
                      defaultMessage: "No paraffin overflow",
                    })}
                    checked={bulkApplyValues.noParaffinOverflow}
                    onChange={(_, { checked }) =>
                      setBulkApplyValues((prev) => ({
                        ...prev,
                        noParaffinOverflow: checked,
                      }))
                    }
                  />
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <Checkbox
                    id="noCracks"
                    labelText={intl.formatMessage({
                      id: "pathology.qc.noCracks",
                      defaultMessage: "No cracks",
                    })}
                    checked={bulkApplyValues.noCracks}
                    onChange={(_, { checked }) =>
                      setBulkApplyValues((prev) => ({
                        ...prev,
                        noCracks: checked,
                      }))
                    }
                  />
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <Checkbox
                    id="noBubbles"
                    labelText={intl.formatMessage({
                      id: "pathology.qc.noBubbles",
                      defaultMessage: "No bubbles",
                    })}
                    checked={bulkApplyValues.noBubbles}
                    onChange={(_, { checked }) =>
                      setBulkApplyValues((prev) => ({
                        ...prev,
                        noBubbles: checked,
                      }))
                    }
                  />
                </Column>
              </Grid>
            </AccordionItem>
          </Accordion>

          {/* QC Result - Pass/Fail */}
          <div className="qc-section" style={{ marginTop: "1.5rem" }}>
            <h5 className="qc-section-header">
              <FormattedMessage
                id="pathology.qc.section.result"
                defaultMessage="QC Status (Master Accession Ledger)"
              />
            </h5>
            <div
              className={`qc-result-section ${bulkApplyValues.qcResult === "Fail" ? "fail" : bulkApplyValues.qcResult === "Pass" ? "pass" : ""}`}
            >
              <Grid fullWidth>
                <Column lg={16} md={8} sm={4}>
                  <RadioButtonGroup
                    legendText={intl.formatMessage({
                      id: "pathology.qc.qcResult.label",
                      defaultMessage: "QC Result *",
                    })}
                    name="qcResult"
                    valueSelected={bulkApplyValues.qcResult}
                    onChange={(value) =>
                      setBulkApplyValues((prev) => ({
                        ...prev,
                        qcResult: value,
                        failReason: value === "Pass" ? "" : prev.failReason,
                        failAction: value === "Pass" ? "" : prev.failAction,
                      }))
                    }
                    orientation="horizontal"
                  >
                    <RadioButton
                      labelText={intl.formatMessage({
                        id: "pathology.qc.qcResult.pass",
                        defaultMessage: "Pass - Proceed to processing",
                      })}
                      value="Pass"
                      id="qcResult-pass"
                    />
                    <RadioButton
                      labelText={intl.formatMessage({
                        id: "pathology.qc.qcResult.fail",
                        defaultMessage: "Fail - Requires action",
                      })}
                      value="Fail"
                      id="qcResult-fail"
                    />
                  </RadioButtonGroup>
                </Column>
              </Grid>
            </div>
          </div>

          {/* Failed QC Section - Only show if QC result is Fail */}
          {bulkApplyValues.qcResult === "Fail" && (
            <div className="qc-section">
              <h5 className="qc-section-header">
                <WarningAlt size={16} style={{ marginRight: "0.5rem" }} />
                <FormattedMessage
                  id="pathology.qc.section.failedActions"
                  defaultMessage="Actions for Failed QC"
                />
              </h5>
              <div className="qc-fail-actions">
                <Grid fullWidth>
                  <Column lg={16} md={8} sm={4}>
                    <TextInput
                      id="failReason"
                      labelText={intl.formatMessage({
                        id: "pathology.qc.failReason",
                        defaultMessage: "Failure Reason *",
                      })}
                      value={bulkApplyValues.failReason}
                      onChange={(e) =>
                        setBulkApplyValues((prev) => ({
                          ...prev,
                          failReason: e.target.value,
                        }))
                      }
                      placeholder="Describe the reason for QC failure"
                      required
                    />
                  </Column>
                  <Column lg={16} md={8} sm={4}>
                    <RadioButtonGroup
                      legendText={intl.formatMessage({
                        id: "pathology.qc.failAction.label",
                        defaultMessage: "Action Required *",
                      })}
                      name="failAction"
                      valueSelected={bulkApplyValues.failAction}
                      onChange={(value) =>
                        setBulkApplyValues((prev) => ({
                          ...prev,
                          failAction: value,
                        }))
                      }
                      orientation="vertical"
                    >
                      <RadioButton
                        labelText={intl.formatMessage({
                          id: "pathology.qc.failAction.recollection",
                          defaultMessage:
                            "Recollection requested - Notify submitter (clinician/PI)",
                        })}
                        value="recollection_requested"
                        id="failAction-recollection"
                      />
                      <RadioButton
                        labelText={intl.formatMessage({
                          id: "pathology.qc.failAction.processedWithLimitations",
                          defaultMessage:
                            "Processed with limitations - Document limitations in report",
                        })}
                        value="processed_with_limitations"
                        id="failAction-limitations"
                      />
                      <RadioButton
                        labelText={intl.formatMessage({
                          id: "pathology.qc.failAction.piDecides",
                          defaultMessage:
                            "PI decides usability - For research samples",
                        })}
                        value="pi_decides"
                        id="failAction-pi"
                      />
                      <RadioButton
                        labelText={intl.formatMessage({
                          id: "pathology.qc.failAction.notifySubmitter",
                          defaultMessage:
                            "Notify submitter and lab head/pathologist",
                        })}
                        value="notify_submitter"
                        id="failAction-notify"
                      />
                      <RadioButton
                        labelText={intl.formatMessage({
                          id: "pathology.qc.failAction.reEmbed",
                          defaultMessage:
                            "Re-embed or melt block (for tissue block failures)",
                        })}
                        value="re_embed"
                        id="failAction-reembed"
                      />
                      <RadioButton
                        labelText={intl.formatMessage({
                          id: "pathology.qc.failAction.escalate",
                          defaultMessage:
                            "Escalate to pathologist - If unusable",
                        })}
                        value="escalate_pathologist"
                        id="failAction-escalate"
                      />
                      <RadioButton
                        labelText={intl.formatMessage({
                          id: "pathology.qc.failAction.discard",
                          defaultMessage:
                            "Discard sample - Document in logbook",
                        })}
                        value="discard"
                        id="failAction-discard"
                      />
                    </RadioButtonGroup>
                  </Column>

                  {/* QC Remarks */}
                  <Column lg={16} md={8} sm={4}>
                    <TextArea
                      id="qcRemarks"
                      labelText={intl.formatMessage({
                        id: "pathology.qc.qcRemarks",
                        defaultMessage: "Documentation / Remarks",
                      })}
                      value={bulkApplyValues.qcRemarks}
                      onChange={(e) =>
                        setBulkApplyValues((prev) => ({
                          ...prev,
                          qcRemarks: e.target.value,
                        }))
                      }
                      placeholder={intl.formatMessage({
                        id: "pathology.qc.qcRemarks.placeholder",
                        defaultMessage:
                          "Document actions taken, communications, and any additional notes for the Master Accession Ledger...",
                      })}
                      rows={3}
                    />
                  </Column>
                </Grid>
              </div>
            </div>
          )}
        </div>
      </Modal>
    </div>
  );
}

export default PathologyQualityControlPage;
