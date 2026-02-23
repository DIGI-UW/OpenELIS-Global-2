import React, {
  useState,
  useEffect,
  useRef,
  useCallback,
  useMemo,
  useContext,
} from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  Tag,
  Modal,
  Dropdown,
  TextArea,
  Loading,
  NumberInput,
  InlineNotification,
} from "@carbon/react";
import {
  Renew,
  CheckmarkFilled,
  Pending,
  WarningAltFilled,
  Edit,
  Archive,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import { NotificationContext } from "../../../layout/Layout";
import { NotificationKinds } from "../../../common/CustomNotification";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import SampleGrid from "../../workflow/SampleGrid";
import { Permissions } from "../../../../constants/roles";
import PermissionGate from "../../../security/PermissionGate";
import "../../workflow/NotebookWorkflow.css";

/**
 * TraditionalMedicinePreparationPage - Page 3 of the Traditional Medicine workflow.
 *
 * SRS Requirements - STAGE 4: Sample Preparation for Analysis
 * - Physical processing: Freshly processed, drying (air, oven, freeze)
 * - Preparation methods: Grinding, chopping, powdering
 * - Weight tracking and yield calculation
 * - Documentation: processing method, before/after weight, yield %, operator, date
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - The notebook page data
 * @param {Object} props.progress - Page progress
 * @param {function} props.onProgressUpdate - Callback when progress changes
 */
function TraditionalMedicinePreparationPage({
  entryId,
  pageData,
  progress: _progress,
  onProgressUpdate,
}) {
  const intl = useIntl();
  const { setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const componentMounted = useRef(false);

  // Use standard permissions instead of custom TMMRD-specific logic
  // Page-level access control should be handled by usePageAccessControl() in parent workflow component
  // This component focuses on action-level permissions using standard role groups

  // All state must be declared before any conditional returns (React Hooks Rule)
  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [loading, setLoading] = useState(true);

  // Preparation modal state
  const [preparationModalOpen, setPreparationModalOpen] = useState(false);
  const [isApplyingPrep, setIsApplyingPrep] = useState(false);
  const [isCompleting, setIsCompleting] = useState(false);

  // Preparation form fields
  const [processingMethod, setProcessingMethod] = useState(null);
  const [dryingMethod, setDryingMethod] = useState(null);
  const [weightBefore, setWeightBefore] = useState("");
  const [weightAfter, setWeightAfter] = useState("");
  const [dryingTemperature, setDryingTemperature] = useState("");
  const [dryingDuration, setDryingDuration] = useState("");
  const [freezeDryingTemperature, setFreezeDryingTemperature] = useState("");
  const [freezeDryingVacuum, setFreezeDryingVacuum] = useState("");
  const [freezeDryingSublimationTime, setFreezeDryingSublimationTime] =
    useState("");
  const [prepNotes, setPrepNotes] = useState("");
  const [operatorInfo, setOperatorInfo] = useState("");
  const [preparedAtTime, setPreparedAtTime] = useState("");

  // Processing method options (per SRS)
  const processingMethodOptions = [
    { id: "grinding", label: "Grinding (to powder)" },
    { id: "chopping", label: "Chopping (coarse pieces)" },
    { id: "powdering", label: "Powdering (fine powder for extraction)" },
    {
      id: "freshly_processed",
      label: "Freshly processed samples: Used immediately",
    },
  ];

  // Drying method options (per SRS)
  const dryingMethodOptions = [
    { id: "air_drying", label: "Air drying" },
    { id: "oven_drying", label: "Oven drying (controlled temperature)" },
    {
      id: "freeze_drying",
      label: "Freeze drying (for heat-sensitive materials)",
    },
  ];

  // Notification callback
  const notify = useCallback(
    ({ kind = NotificationKinds.info, title, message }) => {
      setNotificationVisible(true);
      addNotification({ kind, title, message });
    },
    [addNotification, setNotificationVisible],
  );

  const hasRealPageId =
    pageData?.id && !String(pageData.id).startsWith("default-");

  const loadPageSamples = useCallback(() => {
    if (!pageData?.id || String(pageData.id).startsWith("default-")) {
      setLoading(false);
      return;
    }

    setLoading(true);

    getFromOpenElisServer(
      `/rest/notebook/page/${pageData.id}/samples`,
      (response) => {
        if (componentMounted.current) {
          let samplesToProcess = [];

          // Handle both array and object responses from API
          if (response) {
            if (Array.isArray(response)) {
              samplesToProcess = response;
            } else if (response.samples && Array.isArray(response.samples)) {
              samplesToProcess = response.samples;
            }
          }

          if (samplesToProcess.length > 0) {
            const transformedSamples = samplesToProcess.map((sample) => ({
              id: String(sample.id || sample.sampleItemId),
              externalId: sample.externalId,
              accessionNumber: sample.accessionNumber,
              status: sample.pageStatus || sample.status || "PENDING",
              localName: sample.data?.localName,
              scientificName: sample.data?.scientificName,
              // Preparation data
              processingMethod: sample.data?.processingMethod,
              dryingMethod: sample.data?.dryingMethod,
              weightBefore: sample.data?.weightBefore,
              weightAfter: sample.data?.weightAfter,
              yieldPercent: sample.data?.yieldPercent,
              dryingTemperature: sample.data?.dryingTemperature,
              dryingDuration: sample.data?.dryingDuration,
              preparedAt: sample.data?.preparedAt,
              preparedBy: sample.data?.preparedBy,
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

  useEffect(() => {
    componentMounted.current = true;
    loadPageSamples();
    return () => {
      componentMounted.current = false;
    };
  }, [entryId, pageData?.id, loadPageSamples]);

  const resetPrepForm = useCallback(() => {
    setProcessingMethod(null);
    setDryingMethod(null);
    setWeightBefore("");
    setWeightAfter("");
    setDryingTemperature("");
    setDryingDuration("");
    setFreezeDryingTemperature("");
    setFreezeDryingVacuum("");
    setFreezeDryingSublimationTime("");
    setPrepNotes("");
    setOperatorInfo("");
    setPreparedAtTime("");
  }, []);

  const openPrepModal = useCallback(() => {
    if (selectedSampleIds.length === 0) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.page.tradmed.error.noSelection",
          defaultMessage: "Please select at least one sample.",
        }),
      });
      return;
    }
    resetPrepForm();
    // Auto-populate operator and date info
    const now = new Date();
    setPreparedAtTime(now.toLocaleString());
    setOperatorInfo(`${new Date().toISOString().split("T")[0]} - Auto-logged`);
    setPreparationModalOpen(true);
  }, [selectedSampleIds, intl, resetPrepForm, notify]);

  const applyPreparation = useCallback(() => {
    if (!processingMethod) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.page.tradmed.prep.error.methodRequired",
          defaultMessage: "Please select processing method.",
        }),
      });
      return;
    }

    if (weightBefore && weightAfter) {
      const before = parseFloat(weightBefore);
      const after = parseFloat(weightAfter);
      if (after > before) {
        notify({
          kind: NotificationKinds.error,
          title: intl.formatMessage({
            id: "notebook.page.tradmed.prep.error.weightAfterGreater",
            defaultMessage: "Weight after drying cannot exceed weight before.",
          }),
        });
        return;
      }
    }

    if (!hasRealPageId) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.page.tradmed.error.noPage",
          defaultMessage:
            "Cannot update samples: Page not properly initialized.",
        }),
      });
      return;
    }

    setIsApplyingPrep(true);

    const sampleIds = selectedSampleIds.map((id) => parseInt(id, 10));
    const yieldPercent =
      weightBefore && weightAfter
        ? (
            ((parseFloat(weightBefore) - parseFloat(weightAfter)) /
              parseFloat(weightBefore)) *
            100
          ).toFixed(2)
        : null;

    postToOpenElisServerJsonResponse(
      `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
      JSON.stringify({
        sampleIds: sampleIds,
        data: {
          processingMethod: processingMethod.id,
          processingMethodLabel: processingMethod.label,
          dryingMethod: dryingMethod?.id || null,
          dryingMethodLabel: dryingMethod?.label || null,
          weightBefore: weightBefore || null,
          weightAfter: weightAfter || null,
          yieldPercent: yieldPercent,
          dryingTemperature: dryingTemperature || null,
          dryingDuration: dryingDuration || null,
          freezeDryingTemperature: freezeDryingTemperature || null,
          freezeDryingVacuum: freezeDryingVacuum || null,
          freezeDryingSublimationTime: freezeDryingSublimationTime || null,
          prepNotes: prepNotes || null,
          preparedAt: preparedAtTime || null,
          preparedBy: operatorInfo || null,
        },
      }),
      (response) => {
        setIsApplyingPrep(false);

        if (response && response.success) {
          // Update sample status using bulk endpoint after preparation
          postToOpenElisServer(
            `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
            JSON.stringify({
              sampleIds: sampleIds,
              status: "IN_PROGRESS",
            }),
            (statusCode) => {
              if (statusCode === 200) {
                notify({
                  kind: NotificationKinds.success,
                  title:
                    response.message ||
                    intl.formatMessage(
                      {
                        id: "notebook.page.tradmed.prep.success",
                        defaultMessage: "Prepared {count} sample(s).",
                      },
                      {
                        count:
                          response.updatedCount || selectedSampleIds.length,
                      },
                    ),
                });
                setPreparationModalOpen(false);
                setSelectedSampleIds([]);
                loadPageSamples();
                if (onProgressUpdate) onProgressUpdate();
              } else {
                notify({
                  kind: NotificationKinds.error,
                  title: intl.formatMessage({
                    id: "notebook.page.tradmed.error.statusUpdate",
                    defaultMessage:
                      "Preparation recorded but failed to update sample status.",
                  }),
                });
              }
            },
          );
        } else {
          notify({
            kind: NotificationKinds.error,
            title:
              response?.error ||
              intl.formatMessage({
                id: "notebook.page.tradmed.prep.error.failed",
                defaultMessage: "Failed to prepare samples. Please try again.",
              }),
          });
        }
      },
    );
  }, [
    processingMethod,
    dryingMethod,
    weightBefore,
    weightAfter,
    dryingTemperature,
    dryingDuration,
    freezeDryingTemperature,
    freezeDryingVacuum,
    freezeDryingSublimationTime,
    prepNotes,
    operatorInfo,
    preparedAtTime,
    hasRealPageId,
    pageData?.id,
    selectedSampleIds,
    intl,
    loadPageSamples,
    onProgressUpdate,
    notify,
  ]);

  // Handle marking prepared samples complete (moving to next page)
  const handleMarkComplete = useCallback(() => {
    // Filter samples that can be marked complete: selected, prepared, and not already completed
    const samplesToComplete = samples.filter(
      (s) =>
        selectedSampleIds.includes(s.id) &&
        s.processingMethod &&
        s.status !== "COMPLETED",
    );

    if (samplesToComplete.length === 0) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.tradmed.prep.noEligibleSamples",
          defaultMessage:
            "Selected samples must have processing method recorded before completing.",
        }),
      });
      return;
    }

    setIsCompleting(true);

    const sampleIds = samplesToComplete.map((s) => parseInt(s.id, 10));

    postToOpenElisServerJsonResponse(
      `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
      JSON.stringify({ sampleIds: sampleIds, status: "COMPLETED" }),
      (response) => {
        setIsCompleting(false);

        if (response && response.success) {
          notify({
            kind: NotificationKinds.success,
            title: intl.formatMessage(
              {
                id: "notebook.tradmed.prep.completeSuccess",
                defaultMessage:
                  "Successfully marked {count} samples as complete.",
              },
              { count: response.updatedCount || sampleIds.length },
            ),
          });
          setSelectedSampleIds([]);
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          notify({
            kind: NotificationKinds.error,
            title:
              response?.error ||
              intl.formatMessage({
                id: "notebook.tradmed.prep.completeFailed",
                defaultMessage: "Failed to mark samples complete.",
              }),
          });
        }
      },
    );
  }, [
    selectedSampleIds,
    samples,
    pageData?.id,
    intl,
    notify,
    loadPageSamples,
    onProgressUpdate,
  ]);

  const unpreparedSamples = useMemo(
    () =>
      samples.filter(
        (s) => s.status === "PENDING" || s.status === "IN_PROGRESS",
      ),
    [samples],
  );
  const preparedCompletedSamples = useMemo(
    () => samples.filter((s) => s.status === "COMPLETED"),
    [samples],
  );

  const renderYieldTag = (sample) => {
    if (!sample.yieldPercent) return "-";
    const yieldNum = parseFloat(sample.yieldPercent);
    const type = yieldNum > 50 ? "green" : yieldNum > 30 ? "blue" : "red";
    return (
      <Tag type={type} size="sm">
        {sample.yieldPercent}%
      </Tag>
    );
  };

  // Helper to render sample status - simple status display matching API response
  const renderStatus = (sample) => {
    const status = sample.status || "PENDING";

    switch (status.toUpperCase()) {
      case "COMPLETED":
        return (
          <Tag type="green" size="sm" renderIcon={CheckmarkFilled}>
            <FormattedMessage
              id="notebook.tradmed.status.completed"
              defaultMessage="Completed"
            />
          </Tag>
        );
      case "IN_PROGRESS":
        return (
          <Tag type="blue" size="sm" renderIcon={Archive}>
            <FormattedMessage
              id="notebook.tradmed.status.inProgress"
              defaultMessage="In Progress"
            />
          </Tag>
        );
      case "SKIPPED":
        return (
          <Tag type="gray" size="sm">
            <FormattedMessage
              id="notebook.tradmed.status.skipped"
              defaultMessage="Skipped"
            />
          </Tag>
        );
      default:
        return (
          <Tag type="gray" size="sm" renderIcon={Pending}>
            <FormattedMessage
              id="notebook.tradmed.status.pending"
              defaultMessage="Pending"
            />
          </Tag>
        );
    }
  };

  return (
    <div className="tradmed-preparation-page">
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.page.tradmed.prep.title"
            defaultMessage="Sample Preparation for Analysis"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.tradmed.prep.description"
            defaultMessage="Process and prepare samples through grinding, chopping, drying, or powdering with weight tracking and yield calculation."
          />
        </p>
      </div>

      <Grid fullWidth className="progress-section">
        <Column lg={16} md={8} sm={4}>
          <div className="progress-tiles">
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.tradmed.prep.readyForPrep"
                  defaultMessage="Ready for Preparation"
                />
              </span>
              <span className="progress-value">{unpreparedSamples.length}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.tradmed.prep.prepared"
                  defaultMessage="Prepared"
                />
              </span>
              <span className="progress-value">
                {preparedCompletedSamples.length}
              </span>
            </Tile>
          </div>
        </Column>
      </Grid>

      <div className="page-actions-bar">
        <PermissionGate permissions={[Permissions.UPDATE_SAMPLES]}>
          <Button
            kind="primary"
            size="sm"
            renderIcon={Edit}
            onClick={openPrepModal}
            disabled={selectedSampleIds.length === 0 || !hasRealPageId}
            title={
              selectedSampleIds.length === 0
                ? intl.formatMessage({
                    id: "notebook.tradmed.tooltip.selectSamples",
                    defaultMessage: "Select samples to record preparation",
                  })
                : ""
            }
          >
            <FormattedMessage
              id="notebook.page.tradmed.prep.recordPrep"
              defaultMessage="Record Preparation ({count})"
              values={{ count: selectedSampleIds.length }}
            />
          </Button>
        </PermissionGate>

        <PermissionGate permissions={[Permissions.PROCESS_SAMPLES]}>
          <Button
            kind="tertiary"
            size="sm"
            renderIcon={CheckmarkFilled}
            onClick={handleMarkComplete}
            disabled={
              selectedSampleIds.length === 0 || isCompleting || !hasRealPageId
            }
          >
            <FormattedMessage
              id="notebook.tradmed.prep.markComplete"
              defaultMessage="Mark Complete ({count})"
              values={{ count: selectedSampleIds.length }}
            />
          </Button>
        </PermissionGate>

        <Button
          kind="ghost"
          size="sm"
          renderIcon={Renew}
          onClick={loadPageSamples}
          disabled={loading}
        >
          <FormattedMessage
            id="notebook.page.tradmed.refresh"
            defaultMessage="Refresh"
          />
        </Button>
      </div>

      <div className="sample-table-section">
        <div className="table-section-header">
          <h5>
            <FormattedMessage
              id="notebook.page.tradmed.prep.unprepared.title"
              defaultMessage="Samples Awaiting Preparation"
            />
            <Tag type="blue" size="sm" className="count-tag">
              {unpreparedSamples.length}
            </Tag>
          </h5>
        </div>
        <div className="sample-grid-container">
          {!loading && unpreparedSamples.length === 0 ? (
            <div className="empty-table-state">
              <p>
                <FormattedMessage
                  id="notebook.page.tradmed.prep.unprepared.empty"
                  defaultMessage="No samples awaiting preparation."
                />
              </p>
            </div>
          ) : (
            <SampleGrid
              gridId="unprepared-samples"
              samples={unpreparedSamples}
              selectedIds={selectedSampleIds}
              onSelectionChange={setSelectedSampleIds}
              showSelection={true}
              loading={loading}
              columns={[
                { key: "accessionNumber", header: "Accession #" },
                { key: "externalId", header: "Sample ID" },
                { key: "localName", header: "Local Name" },
                { key: "scientificName", header: "Scientific Name" },
                {
                  key: "status",
                  header: intl.formatMessage({
                    id: "notebook.tradmed.column.status",
                    defaultMessage: "Status",
                  }),
                  render: (_value, sample) => renderStatus(sample),
                },
              ]}
            />
          )}
        </div>
      </div>

      {/* Prepared Samples Section - COMPLETED */}
      {preparedCompletedSamples.length > 0 && (
        <div className="sample-table-section">
          <div className="table-section-header">
            <h5>
              <FormattedMessage
                id="notebook.page.tradmed.prep.completed.title"
                defaultMessage="Preparation Completed"
              />
              <Tag type="green" size="sm" className="count-tag">
                {preparedCompletedSamples.length}
              </Tag>
            </h5>
          </div>
          <div className="sample-grid-container">
            <SampleGrid
              gridId="prepared-completed-samples"
              samples={preparedCompletedSamples}
              showSelection={false}
              loading={loading}
              columns={[
                { key: "accessionNumber", header: "Accession #" },
                { key: "externalId", header: "Sample ID" },
                { key: "localName", header: "Local Name" },
                { key: "scientificName", header: "Scientific Name" },
                {
                  key: "status",
                  header: intl.formatMessage({
                    id: "notebook.tradmed.column.status",
                    defaultMessage: "Status",
                  }),
                  render: (_value, sample) => renderStatus(sample),
                },
                { key: "processingMethod", header: "Processing Method" },
                { key: "dryingMethod", header: "Drying Method" },
                { key: "weightBefore", header: "Weight Before (g)" },
                { key: "weightAfter", header: "Weight After (g)" },
                {
                  key: "yieldPercent",
                  header: "Yield %",
                  render: (_value, row) => renderYieldTag(row),
                },
              ]}
            />
          </div>
        </div>
      )}

      <Modal
        open={preparationModalOpen}
        onRequestClose={() => setPreparationModalOpen(false)}
        onRequestSubmit={applyPreparation}
        modalHeading={intl.formatMessage({
          id: "notebook.page.tradmed.prep.modal.title",
          defaultMessage: "Record Sample Preparation",
        })}
        primaryButtonText={
          isApplyingPrep
            ? intl.formatMessage({
                id: "label.recording",
                defaultMessage: "Recording...",
              })
            : intl.formatMessage({
                id: "notebook.page.tradmed.prep.modal.record",
                defaultMessage: "Record Preparation",
              })
        }
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        primaryButtonDisabled={isApplyingPrep}
        size="md"
      >
        <div style={{ marginBottom: "1rem" }}>
          <p>
            <FormattedMessage
              id="notebook.page.tradmed.prep.modal.description"
              defaultMessage="Record preparation details for {count} sample(s)."
              values={{ count: selectedSampleIds.length }}
            />
          </p>
          {preparedAtTime && (
            <p style={{ fontSize: "0.875rem", color: "#525252" }}>
              <strong>
                <FormattedMessage
                  id="notebook.page.tradmed.prep.modal.timestamp"
                  defaultMessage="Timestamp:"
                />
              </strong>{" "}
              {preparedAtTime}
            </p>
          )}
        </div>

        <div style={{ marginBottom: "1.5rem" }}>
          <InlineNotification
            kind="info"
            title={intl.formatMessage({
              id: "notebook.page.tradmed.prep.yieldCalculation.title",
              defaultMessage: "Yield Percentage Calculation",
            })}
            subtitle={intl.formatMessage({
              id: "notebook.page.tradmed.prep.yieldCalculation.description",
              defaultMessage:
                "Yield % = ((Weight Before - Weight After) / Weight Before) × 100. Example: If you start with 100g and end with 75g, the yield is 25%.",
            })}
            hideCloseButton
          />
        </div>

        {isApplyingPrep && <Loading withOverlay={false} small />}

        <Grid narrow>
          <Column lg={16} md={16} sm={4} style={{ marginBottom: "1rem" }}>
            <Dropdown
              id="processing-method"
              titleText={intl.formatMessage({
                id: "notebook.page.tradmed.prep.modal.method",
                defaultMessage: "Processing Method *",
              })}
              label={intl.formatMessage({
                id: "label.select",
                defaultMessage: "Select...",
              })}
              items={processingMethodOptions}
              itemToString={(item) => (item ? item.label : "")}
              selectedItem={processingMethod}
              onChange={({ selectedItem }) => setProcessingMethod(selectedItem)}
            />
          </Column>

          <Column lg={16} md={16} sm={4} style={{ marginBottom: "1rem" }}>
            <Dropdown
              id="drying-method"
              titleText={intl.formatMessage({
                id: "notebook.page.tradmed.prep.modal.drying",
                defaultMessage: "Drying Method",
              })}
              label={intl.formatMessage({
                id: "label.select",
                defaultMessage: "Select...",
              })}
              items={dryingMethodOptions}
              itemToString={(item) => (item ? item.label : "")}
              selectedItem={dryingMethod}
              onChange={({ selectedItem }) => setDryingMethod(selectedItem)}
            />
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <NumberInput
              id="weight-before"
              label={intl.formatMessage({
                id: "notebook.page.tradmed.prep.modal.weightBefore",
                defaultMessage: "Weight Before (g)",
              })}
              value={weightBefore}
              onChange={(e) =>
                setWeightBefore(
                  e.imaginaryTarget?.value || e.target?.value || "",
                )
              }
              step={0.1}
            />
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <NumberInput
              id="weight-after"
              label={intl.formatMessage({
                id: "notebook.page.tradmed.prep.modal.weightAfter",
                defaultMessage: "Weight After (g)",
              })}
              value={weightAfter}
              onChange={(e) =>
                setWeightAfter(
                  e.imaginaryTarget?.value || e.target?.value || "",
                )
              }
              step={0.1}
            />
          </Column>

          {dryingMethod?.id === "oven_drying" && (
            <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
              <NumberInput
                id="drying-temp"
                label={intl.formatMessage({
                  id: "notebook.page.tradmed.prep.modal.dryingTemp",
                  defaultMessage: "Drying Temperature (°C)",
                })}
                value={dryingTemperature}
                onChange={(e) =>
                  setDryingTemperature(
                    e.imaginaryTarget?.value || e.target?.value || "",
                  )
                }
                step={1}
              />
            </Column>
          )}

          {dryingMethod?.id === "oven_drying" && (
            <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
              <NumberInput
                id="drying-duration"
                label={intl.formatMessage({
                  id: "notebook.page.tradmed.prep.modal.dryingDuration",
                  defaultMessage: "Drying Duration (hours)",
                })}
                value={dryingDuration}
                onChange={(e) =>
                  setDryingDuration(
                    e.imaginaryTarget?.value || e.target?.value || "",
                  )
                }
                step={0.5}
              />
            </Column>
          )}

          {dryingMethod?.id === "freeze_drying" && (
            <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
              <NumberInput
                id="freeze-drying-temp"
                label={intl.formatMessage({
                  id: "notebook.page.tradmed.prep.modal.freezeDryingTemp",
                  defaultMessage: "Cooling Temperature (°C)",
                })}
                value={freezeDryingTemperature}
                onChange={(e) =>
                  setFreezeDryingTemperature(
                    e.imaginaryTarget?.value || e.target?.value || "",
                  )
                }
                step={1}
              />
            </Column>
          )}

          {dryingMethod?.id === "freeze_drying" && (
            <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
              <NumberInput
                id="freeze-drying-vacuum"
                label={intl.formatMessage({
                  id: "notebook.page.tradmed.prep.modal.freezeDryingVacuum",
                  defaultMessage: "Vacuum Pressure (mBar)",
                })}
                value={freezeDryingVacuum}
                onChange={(e) =>
                  setFreezeDryingVacuum(
                    e.imaginaryTarget?.value || e.target?.value || "",
                  )
                }
                step={0.1}
              />
            </Column>
          )}

          {dryingMethod?.id === "freeze_drying" && (
            <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
              <NumberInput
                id="freeze-drying-sublimation"
                label={intl.formatMessage({
                  id: "notebook.page.tradmed.prep.modal.freezeDryingSublimation",
                  defaultMessage: "Sublimation Time (hours)",
                })}
                value={freezeDryingSublimationTime}
                onChange={(e) =>
                  setFreezeDryingSublimationTime(
                    e.imaginaryTarget?.value || e.target?.value || "",
                  )
                }
                step={0.5}
              />
            </Column>
          )}

          <Column lg={16} md={16} sm={4} style={{ marginBottom: "1rem" }}>
            <TextArea
              id="prep-notes"
              labelText={intl.formatMessage({
                id: "notebook.page.tradmed.prep.modal.notes",
                defaultMessage: "Preparation Notes",
              })}
              value={prepNotes}
              onChange={(e) => setPrepNotes(e.target.value)}
              rows={2}
            />
          </Column>
        </Grid>
      </Modal>
    </div>
  );
}

export default TraditionalMedicinePreparationPage;
