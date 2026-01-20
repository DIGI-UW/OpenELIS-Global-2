import React, {
  useState,
  useEffect,
  useRef,
  useCallback,
  useContext,
  useMemo,
} from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  Tag,
  Modal,
  TextInput,
  TextArea,
  ComboBox,
  DatePicker,
  DatePickerInput,
  TimePicker,
  TimePickerSelect,
  SelectItem,
} from "@carbon/react";
import { Add, TrashCan, Save, Checkmark, View } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import { NotificationContext } from "../../../layout/Layout";
import { NotificationKinds } from "../../../common/CustomNotification";
import SampleGrid from "../../workflow/SampleGrid";
import "../../workflow/NotebookWorkflow.css";

/**
 * VirologyFeedingPage - Page for logging culture feeding and maintenance activities.
 * Displays samples and allows logging feeding schedule and reagents used.
 *
 * Features:
 * - Display and select samples from the notebook
 * - Open dialog to log feeding activities
 * - Select reagents from inventory with full traceability
 * - Record feeding schedule and observations
 * - Attach selected samples to the feeding batch
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - The notebook page data
 * @param {Object} props.progress - Page progress
 * @param {function} props.onProgressUpdate - Callback when progress changes
 */
function VirologyFeedingPage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
}) {
  const intl = useIntl();
  const { addNotification, setNotificationVisible } =
    useContext(NotificationContext);
  const componentMounted = useRef(false);

  // State
  const [loading, setLoading] = useState(true);
  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [modalOpen, setModalOpen] = useState(false);
  const [historyModalOpen, setHistoryModalOpen] = useState(false);
  const [selectedSampleHistory, setSelectedSampleHistory] = useState(null);

  // Inventory data
  const [availableReagents, setAvailableReagents] = useState([]);

  // Form data (for modal form)
  const [feedingDate, setFeedingDate] = useState("");
  const [feedingTime, setFeedingTime] = useState("");
  const [feedingSchedule, setFeedingSchedule] = useState("");
  const [reagentList, setReagentList] = useState([]);
  const [feedingNotes, setFeedingNotes] = useState("");
  const [volumeUsed, setVolumeUsed] = useState("");
  const [batchNumber, setBatchNumber] = useState("");

  // Notification helper function
  const notify = useCallback(
    ({ kind = NotificationKinds.info, title, subtitle }) => {
      setNotificationVisible(true);
      addNotification({ kind, title, subtitle });
    },
    [addNotification, setNotificationVisible],
  );

  // Load samples for this page
  useEffect(() => {
    componentMounted.current = true;
    loadPageSamples();
    loadInventory();

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

    getFromOpenElisServer(
      `/rest/notebook/page/${pageData.id}/samples`,
      (response) => {
        console.log(
          "Loaded samples for feeding page",
          pageData.id,
          ":",
          JSON.stringify(response, null, 2),
        );
        if (componentMounted.current) {
          if (response && Array.isArray(response)) {
            const transformedSamples = response.map((sample) => {
              return {
                id: String(sample.id || sample.sampleItemId),
                externalId: sample.externalId,
                accessionNumber: sample.accessionNumber,
                sampleType:
                  sample.sampleType || sample.typeOfSample?.description,
                collectionDate: sample.collectionDate,
                status: sample.pageStatus || sample.status || "PENDING",
                // Virology-specific metadata
                sampleId: sample.data?.sampleId,
                source: sample.data?.source,
                testType: sample.data?.testType,
                projectStudyAssociation: sample.data?.projectStudyAssociation,
                // Feeding data (now an array of feeding history)
                feedingHistory: sample.data?.feedingHistory || [],
                // Keep full data for column rendering
                data: sample.data,
              };
            });
            setSamples(transformedSamples);
          } else {
            setSamples([]);
          }
          setLoading(false);
        }
      },
    );
  }, [pageData?.id]);

  const loadInventory = useCallback(() => {
    getFromOpenElisServer(
      "/rest/inventory/reagents?status=active",
      (response) => {
        if (componentMounted.current && response && Array.isArray(response)) {
          // All inventory items for reagents
          const reagents = response.map((item) => ({
            ...item,
            id: item.itemId,
            text: item.name,
            category: item.category,
            lotNumber: item.lotNumber,
            expirationDate: item.expirationDate,
          }));
          setAvailableReagents(reagents);
        }
      },
    );
  }, []);

  const handleAddReagent = () => {
    setReagentList([
      ...reagentList,
      {
        id: Date.now(),
        reagentId: null,
        reagentName: "",
        lotNumber: "",
        expiryDate: "",
        category: "",
        volumeUsed: "",
      },
    ]);
  };

  const handleRemoveReagent = (id) => {
    setReagentList(reagentList.filter((r) => r.id !== id));
  };

  const handleReagentChange = (id, field, value) => {
    setReagentList(
      reagentList.map((r) => {
        if (r.id === id) {
          if (field === "reagent") {
            // When selecting reagent, auto-fill lot and expiry if available
            return {
              ...r,
              reagentId: value.id,
              reagentName: value.text,
              category: value.category || "",
              lotNumber: value.lotNumber || "",
              expiryDate: value.expirationDate || "",
            };
          }
          return { ...r, [field]: value };
        }
        return r;
      }),
    );
  };

  const handleSaveFeeding = useCallback(() => {
    // Validation
    if (!feedingDate) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.error" }),
        subtitle: "Please select a feeding date",
      });
      return;
    }

    if (reagentList.length === 0) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.error" }),
        subtitle: "Please add at least one reagent used for feeding",
      });
      return;
    }

    // Check all reagents have required fields
    const incompleteReagents = reagentList.filter(
      (r) => !r.reagentName || !r.lotNumber || !r.expiryDate,
    );
    if (incompleteReagents.length > 0) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.error" }),
        subtitle: "All reagents must have name, lot number, and expiry date",
      });
      return;
    }

    if (selectedSampleIds.length === 0) {
      notify({
        kind: NotificationKinds.warning,
        title: intl.formatMessage({ id: "notification.warning" }),
        subtitle: "Please select at least one sample",
      });
      return;
    }

    setLoading(true);

    const payload = {
      notebookPageId: pageData?.id,
      sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
      feedingDate,
      feedingTime,
      feedingSchedule,
      batchNumber,
      volumeUsed,
      reagents: reagentList.map((r) => ({
        reagentId: r.reagentId,
        name: r.reagentName,
        category: r.category,
        lotNumber: r.lotNumber,
        expiryDate: r.expiryDate,
        volumeUsed: r.volumeUsed,
      })),
      notes: feedingNotes,
    };

    console.log("Feeding payload:", JSON.stringify(payload, null, 2));

    postToOpenElisServerJsonResponse(
      "/rest/virology/feeding",
      JSON.stringify(payload),
      (response) => {
        setLoading(false);

        if (response.success || response.feedingId) {
          const samplesUpdated = response.samplesUpdated || 0;
          notify({
            kind: NotificationKinds.success,
            title: intl.formatMessage({ id: "notification.success" }),
            subtitle: `Feeding logged successfully. ${samplesUpdated} sample(s) updated.`,
          });
          console.log("Backend response:", response);

          // Clear form and selection
          setFeedingDate("");
          setFeedingTime("");
          setFeedingSchedule("");
          setBatchNumber("");
          setVolumeUsed("");
          setReagentList([]);
          setFeedingNotes("");
          setSelectedSampleIds([]);
          setModalOpen(false);

          // Reload samples to show updated feeding data
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          const errorMessage =
            response.error || response.message || "Unknown error";
          notify({
            kind: NotificationKinds.error,
            title: intl.formatMessage({ id: "notification.error" }),
            subtitle: errorMessage,
          });
        }
      },
    );
  }, [
    feedingDate,
    feedingTime,
    feedingSchedule,
    batchNumber,
    volumeUsed,
    reagentList,
    feedingNotes,
    selectedSampleIds,
    pageData?.id,
    intl,
    notify,
    loadPageSamples,
    onProgressUpdate,
  ]);

  // Handle completing feeding and moving samples to next page
  const handleCompleteFeeding = useCallback(() => {
    if (selectedSampleIds.length === 0) {
      notify({
        kind: NotificationKinds.warning,
        title: intl.formatMessage({
          id: "notification.title",
          defaultMessage: "Notification",
        }),
        subtitle: intl.formatMessage({
          id: "virology.feeding.error.noSelection",
          defaultMessage: "Please select at least one sample to complete.",
        }),
      });
      return;
    }

    if (!pageData?.id || String(pageData.id).startsWith("default-")) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notification.error",
          defaultMessage: "Error",
        }),
        subtitle: intl.formatMessage({
          id: "virology.feeding.error.noPage",
          defaultMessage:
            "Cannot complete samples: Page not properly initialized.",
        }),
      });
      return;
    }

    setLoading(true);

    postToOpenElisServerJsonResponse(
      `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
      JSON.stringify({
        sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
        status: "COMPLETED",
      }),
      (response) => {
        setLoading(false);

        if (response && (response.success || response === 200)) {
          notify({
            kind: NotificationKinds.success,
            title: intl.formatMessage({
              id: "notification.success",
              defaultMessage: "Success",
            }),
            subtitle: intl.formatMessage(
              {
                id: "virology.feeding.success.completed",
                defaultMessage:
                  "Completed feeding for {count} sample(s). They will proceed to the next step.",
              },
              { count: selectedSampleIds.length },
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
            title: intl.formatMessage({
              id: "notification.error",
              defaultMessage: "Error",
            }),
            subtitle: intl.formatMessage({
              id: "virology.feeding.error.complete",
              defaultMessage: "Failed to complete feeding. Please try again.",
            }),
          });
        }
      },
    );
  }, [
    selectedSampleIds,
    pageData?.id,
    intl,
    notify,
    loadPageSamples,
    onProgressUpdate,
  ]);

  // Check if all reagents are complete
  const allReagentsComplete = reagentList.every(
    (r) => r.reagentName && r.lotNumber && r.expiryDate,
  );

  // Split samples into pending/in-progress and completed
  const pendingSamples = useMemo(
    () =>
      samples.filter(
        (s) => s.status === "PENDING" || s.status === "IN_PROGRESS",
      ),
    [samples],
  );

  const completedSamples = useMemo(
    () => samples.filter((s) => s.status === "COMPLETED"),
    [samples],
  );

  const pendingCount = pendingSamples.length;
  const completedCount = completedSamples.length;

  // Custom columns for virology sample display
  const additionalColumns = useMemo(
    () => [
      {
        key: "source",
        header: intl.formatMessage({
          id: "notebook.sample.source",
          defaultMessage: "Source",
        }),
        render: (value, sample) => {
          const source = value || sample?.source || sample?.data?.source;
          if (!source) return "-";
          if (typeof source === "string") return source;
          if (typeof source === "object") {
            return source.value || source.name || source.description || "-";
          }
          return String(source);
        },
      },
      {
        key: "testType",
        header: intl.formatMessage({
          id: "notebook.sample.testType",
          defaultMessage: "Test Type",
        }),
        render: (value, sample) => {
          const testType = value || sample?.testType || sample?.data?.testType;
          if (!testType) return "-";

          let testTypeStr;
          if (typeof testType === "string") {
            testTypeStr = testType;
          } else if (typeof testType === "object") {
            testTypeStr =
              testType.value ||
              testType.name ||
              testType.description ||
              JSON.stringify(testType);
          } else {
            testTypeStr = String(testType);
          }

          const tagType =
            testTypeStr.toLowerCase() === "viral"
              ? "purple"
              : testTypeStr.toLowerCase() === "bacterial"
                ? "teal"
                : "gray";
          return (
            <Tag type={tagType} size="sm">
              {testTypeStr}
            </Tag>
          );
        },
      },
      {
        key: "feedingHistory",
        header: intl.formatMessage({
          id: "virology.feeding.column.feedingCount",
          defaultMessage: "Feeding Count",
        }),
        render: (value, sample) => {
          const history =
            value || sample?.feedingHistory || sample?.data?.feedingHistory;
          if (!Array.isArray(history) || history.length === 0) return "-";

          return (
            <div
              style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}
            >
              <Tag type="blue" size="sm">
                {history.length} feeding{history.length !== 1 ? "s" : ""}
              </Tag>
              <Button
                kind="ghost"
                size="sm"
                renderIcon={View}
                iconDescription="View history"
                hasIconOnly
                onClick={() => {
                  setSelectedSampleHistory({
                    sampleId: sample.externalId || sample.accessionNumber,
                    history: history,
                  });
                  setHistoryModalOpen(true);
                }}
              />
            </div>
          );
        },
      },
      {
        key: "lastFeedingDate",
        header: intl.formatMessage({
          id: "virology.feeding.column.lastFeeding",
          defaultMessage: "Last Feeding",
        }),
        render: (value, sample) => {
          const history =
            sample?.feedingHistory || sample?.data?.feedingHistory;
          if (!Array.isArray(history) || history.length === 0) return "-";

          // Get the most recent feeding (last entry in array)
          const lastFeeding = history[history.length - 1];
          return lastFeeding?.feedingDate || "-";
        },
      },
      {
        key: "feedingSchedule",
        header: intl.formatMessage({
          id: "virology.feeding.column.schedule",
          defaultMessage: "Schedule(s)",
        }),
        render: (value, sample) => {
          const history =
            sample?.feedingHistory || sample?.data?.feedingHistory;
          if (!Array.isArray(history) || history.length === 0) return "-";

          // Collect unique schedules
          const schedules = new Set();
          history.forEach((feeding) => {
            if (feeding.feedingSchedule) {
              schedules.add(feeding.feedingSchedule);
            }
          });

          if (schedules.size === 0) return "-";
          const scheduleArray = Array.from(schedules);

          // Show up to 2 unique schedules
          if (scheduleArray.length === 1) {
            return scheduleArray[0];
          } else if (scheduleArray.length === 2) {
            return scheduleArray.join(", ");
          } else {
            return `${scheduleArray[0]}, ${scheduleArray[1]} +${scheduleArray.length - 2}`;
          }
        },
      },
      {
        key: "reagents",
        header: intl.formatMessage({
          id: "virology.feeding.column.reagents",
          defaultMessage: "Reagents Used",
        }),
        render: (value, sample) => {
          const history =
            sample?.feedingHistory || sample?.data?.feedingHistory;
          if (!Array.isArray(history) || history.length === 0) return "-";

          // Collect all unique reagents from all feeding events
          const reagentSet = new Set();
          history.forEach((feeding) => {
            if (Array.isArray(feeding.reagents)) {
              feeding.reagents.forEach((r) => {
                const name = r.name || r.reagentName || "";
                if (name) reagentSet.add(name);
              });
            }
          });

          if (reagentSet.size === 0) return "-";
          const reagentArray = Array.from(reagentSet);

          // Show first 2 unique reagents, then count if more
          if (reagentArray.length <= 2) {
            return reagentArray.join(", ");
          } else {
            return `${reagentArray.slice(0, 2).join(", ")} +${reagentArray.length - 2}`;
          }
        },
      },
      {
        key: "batchNumbers",
        header: intl.formatMessage({
          id: "virology.feeding.column.batches",
          defaultMessage: "Batch #(s)",
        }),
        render: (value, sample) => {
          const history =
            sample?.feedingHistory || sample?.data?.feedingHistory;
          if (!Array.isArray(history) || history.length === 0) return "-";

          // Collect unique batch numbers
          const batches = new Set();
          history.forEach((feeding) => {
            if (feeding.batchNumber) {
              batches.add(feeding.batchNumber);
            }
          });

          if (batches.size === 0) return "-";
          const batchArray = Array.from(batches);

          // Show all batches if 2 or fewer, otherwise show first 2 + count
          if (batchArray.length <= 2) {
            return batchArray.join(", ");
          } else {
            return `${batchArray[0]}, ${batchArray[1]} +${batchArray.length - 2}`;
          }
        },
      },
    ],
    [intl],
  );

  return (
    <div className="virology-feeding-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="virology.feeding.title"
            defaultMessage="Culture Feeding & Maintenance"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="virology.feeding.page.description"
            defaultMessage="Log feeding schedule and reagents used to maintain cell cultures with full traceability."
          />
        </p>
      </div>

      {/* Action Buttons */}
      <div className="page-actions-bar">
        <Button
          kind="primary"
          size="md"
          renderIcon={Save}
          onClick={() => setModalOpen(true)}
          disabled={loading || selectedSampleIds.length === 0}
        >
          <FormattedMessage
            id="virology.feeding.open"
            defaultMessage="Log Feeding"
          />
        </Button>
        <Button
          kind="tertiary"
          size="md"
          renderIcon={Checkmark}
          onClick={handleCompleteFeeding}
          disabled={loading || selectedSampleIds.length === 0}
          style={{ marginLeft: "0.5rem" }}
        >
          <FormattedMessage
            id="virology.feeding.complete"
            defaultMessage="Complete Feeding ({count})"
            values={{ count: selectedSampleIds.length }}
          />
        </Button>
      </div>

      {/* Pending/In-Progress Samples Section */}
      {pendingCount > 0 && (
        <div className="sample-table-section">
          <div className="table-section-header">
            <h5>
              <FormattedMessage
                id="virology.feeding.samples.pending"
                defaultMessage="Samples Pending Feeding"
              />
              <Tag type="blue" size="sm" className="count-tag">
                {pendingCount}
              </Tag>
              {selectedSampleIds.length > 0 && (
                <Tag type="purple" size="sm" className="count-tag">
                  {selectedSampleIds.length} selected
                </Tag>
              )}
            </h5>
            <p className="table-section-description">
              <FormattedMessage
                id="virology.feeding.samples.description"
                defaultMessage="Select samples to log feeding activities. Click 'Log Feeding' to record reagents and schedule."
              />
            </p>
          </div>
          <div className="sample-grid-container">
            <SampleGrid
              gridId="feeding-pending-samples"
              samples={pendingSamples}
              selectedIds={selectedSampleIds}
              onSelectionChange={setSelectedSampleIds}
              showSelection={true}
              loading={loading}
              additionalColumns={additionalColumns}
            />
          </div>
        </div>
      )}

      {/* Completed Samples Section */}
      {completedCount > 0 && (
        <div className="sample-table-section">
          <div className="table-section-header">
            <h5>
              <FormattedMessage
                id="virology.feeding.samples.completed"
                defaultMessage="Completed Feeding"
              />
              <Tag type="green" size="sm" className="count-tag">
                {completedCount}
              </Tag>
            </h5>
          </div>
          <div className="sample-grid-container">
            <SampleGrid
              gridId="feeding-completed-samples"
              samples={completedSamples}
              additionalColumns={additionalColumns}
              readOnly
            />
          </div>
        </div>
      )}

      {/* Empty State */}
      {!loading && samples.length === 0 && (
        <div className="empty-table-state">
          <p>
            <FormattedMessage
              id="virology.feeding.samples.empty"
              defaultMessage="No samples available for feeding. Complete previous workflow steps first."
            />
          </p>
        </div>
      )}

      {/* Feeding Logging Modal */}
      <Modal
        open={modalOpen}
        onRequestClose={() => setModalOpen(false)}
        modalHeading={
          <FormattedMessage
            id="virology.feeding.modal.title"
            defaultMessage="Log Culture Feeding"
          />
        }
        modalLabel={
          <FormattedMessage
            id="virology.feeding.modal.subtitle"
            defaultMessage="Record feeding schedule and reagents used with full traceability"
          />
        }
        primaryButtonText={
          <FormattedMessage
            id="virology.feeding.save"
            defaultMessage="Save Feeding Log"
          />
        }
        secondaryButtonText={
          <FormattedMessage id="button.cancel" defaultMessage="Cancel" />
        }
        onRequestSubmit={handleSaveFeeding}
        primaryButtonDisabled={
          loading ||
          !feedingDate ||
          reagentList.length === 0 ||
          !allReagentsComplete
        }
        size="lg"
      >
        <Grid fullWidth>
          {/* Batch Number */}
          <Column lg={8}>
            <TextInput
              id="batch-number"
              labelText={
                <FormattedMessage
                  id="virology.feeding.batchNumber"
                  defaultMessage="Batch Number"
                />
              }
              value={batchNumber}
              onChange={(e) => setBatchNumber(e.target.value)}
              placeholder="e.g., FEED-2026-001"
            />
          </Column>

          {/* Feeding Date */}
          <Column lg={8}>
            <DatePicker
              datePickerType="single"
              dateFormat="Y-m-d"
              value={feedingDate}
              onChange={(dates) =>
                setFeedingDate(
                  dates[0] ? dates[0].toISOString().split("T")[0] : "",
                )
              }
            >
              <DatePickerInput
                id="feeding-date"
                placeholder="YYYY-MM-DD"
                labelText={
                  <span>
                    <FormattedMessage
                      id="virology.feeding.date"
                      defaultMessage="Feeding Date"
                    />
                    {" *"}
                  </span>
                }
                invalid={!feedingDate}
                invalidText="Required"
              />
            </DatePicker>
          </Column>

          {/* Feeding Time */}
          <Column lg={8}>
            <TextInput
              id="feeding-time"
              labelText={
                <FormattedMessage
                  id="virology.feeding.time"
                  defaultMessage="Feeding Time"
                />
              }
              value={feedingTime}
              onChange={(e) => setFeedingTime(e.target.value)}
              placeholder="e.g., 10:00 AM"
              type="time"
            />
          </Column>

          {/* Feeding Schedule */}
          <Column lg={8}>
            <TextInput
              id="feeding-schedule"
              labelText={
                <FormattedMessage
                  id="virology.feeding.schedule"
                  defaultMessage="Feeding Schedule"
                />
              }
              value={feedingSchedule}
              onChange={(e) => setFeedingSchedule(e.target.value)}
              placeholder="e.g., Every 2 days, Daily, 3x per week"
            />
          </Column>

          {/* Volume Used */}
          <Column lg={8}>
            <TextInput
              id="volume-used"
              labelText={
                <FormattedMessage
                  id="virology.feeding.volume"
                  defaultMessage="Total Volume Used (mL)"
                />
              }
              value={volumeUsed}
              onChange={(e) => setVolumeUsed(e.target.value)}
              placeholder="e.g., 10"
              type="number"
            />
          </Column>

          {/* Feeding Notes */}
          <Column lg={16}>
            <TextArea
              id="feeding-notes"
              labelText={
                <FormattedMessage
                  id="virology.feeding.notes"
                  defaultMessage="Feeding Notes"
                />
              }
              placeholder="Add any observations (e.g., cell confluence, media color, contamination check)..."
              value={feedingNotes}
              onChange={(e) => setFeedingNotes(e.target.value)}
              rows={4}
            />
          </Column>

          {/* Reagents Section */}
          <Column lg={16}>
            <Tile style={{ marginTop: "1rem", marginBottom: "3rem" }}>
              <div
                style={{
                  display: "flex",
                  justifyContent: "space-between",
                  alignItems: "center",
                  marginBottom: "1rem",
                }}
              >
                <h4>
                  <FormattedMessage
                    id="virology.feeding.reagents.title"
                    defaultMessage="Reagents Used for Feeding"
                  />
                  {" *"}
                </h4>
                <Button
                  kind="tertiary"
                  size="sm"
                  renderIcon={Add}
                  onClick={handleAddReagent}
                  disabled={loading}
                >
                  <FormattedMessage
                    id="virology.feeding.reagent.add"
                    defaultMessage="Add Reagent"
                  />
                </Button>
              </div>

              {reagentList.length === 0 ? (
                <p style={{ color: "#6f6f6f", fontStyle: "italic" }}>
                  <FormattedMessage
                    id="virology.feeding.reagents.empty"
                    defaultMessage="No reagents added yet. Click 'Add Reagent' to start."
                  />
                </p>
              ) : (
                <div style={{ width: "100%" }}>
                  {reagentList.map((reagent) => (
                    <div
                      key={reagent.id}
                      style={{
                        border: "1px solid #e0e0e0",
                        borderRadius: "4px",
                        padding: "1rem",
                        marginBottom: "1rem",
                        backgroundColor: "#ffffff",
                      }}
                    >
                      <Grid narrow>
                        <Column lg={5} md={4} sm={4}>
                          <ComboBox
                            id={`reagent-${reagent.id}`}
                            titleText={
                              <span>
                                {intl.formatMessage({
                                  id: "virology.reagent.name",
                                  defaultMessage: "Reagent",
                                })}
                                {" *"}
                              </span>
                            }
                            placeholder="Select reagent from inventory..."
                            items={availableReagents}
                            itemToString={(item) => (item ? item.text : "")}
                            selectedItem={
                              availableReagents.find(
                                (r) => r.text === reagent.reagentName,
                              ) || null
                            }
                            onChange={({ selectedItem }) =>
                              handleReagentChange(
                                reagent.id,
                                "reagent",
                                selectedItem,
                              )
                            }
                            size="sm"
                            invalid={!reagent.reagentName}
                            invalidText="Required"
                          />
                        </Column>
                        <Column lg={3} md={3} sm={4}>
                          <TextInput
                            id={`category-${reagent.id}`}
                            labelText={intl.formatMessage({
                              id: "virology.reagent.category",
                              defaultMessage: "Category",
                            })}
                            value={reagent.category}
                            readOnly
                            size="sm"
                          />
                        </Column>
                        <Column lg={3} md={3} sm={4}>
                          <TextInput
                            id={`lot-${reagent.id}`}
                            labelText={
                              <span>
                                {intl.formatMessage({
                                  id: "virology.reagent.lot",
                                  defaultMessage: "Lot Number",
                                })}
                                {" *"}
                              </span>
                            }
                            value={reagent.lotNumber}
                            onChange={(e) =>
                              handleReagentChange(
                                reagent.id,
                                "lotNumber",
                                e.target.value,
                              )
                            }
                            placeholder="Lot #"
                            size="sm"
                            invalid={!reagent.lotNumber}
                            invalidText="Required"
                          />
                        </Column>
                        <Column lg={3} md={3} sm={4}>
                          <DatePicker
                            datePickerType="single"
                            dateFormat="Y-m-d"
                            value={reagent.expiryDate}
                            onChange={(dates) =>
                              handleReagentChange(
                                reagent.id,
                                "expiryDate",
                                dates[0]
                                  ? dates[0].toISOString().split("T")[0]
                                  : "",
                              )
                            }
                          >
                            <DatePickerInput
                              id={`expiry-${reagent.id}`}
                              placeholder="YYYY-MM-DD"
                              labelText={
                                <span>
                                  {intl.formatMessage({
                                    id: "virology.reagent.expiry",
                                    defaultMessage: "Expiry Date",
                                  })}
                                  {" *"}
                                </span>
                              }
                              size="sm"
                              invalid={!reagent.expiryDate}
                              invalidText="Required"
                            />
                          </DatePicker>
                        </Column>
                        <Column lg={2} md={2} sm={4}>
                          <TextInput
                            id={`volume-${reagent.id}`}
                            labelText={intl.formatMessage({
                              id: "virology.reagent.volume",
                              defaultMessage: "Volume (mL)",
                            })}
                            value={reagent.volumeUsed}
                            onChange={(e) =>
                              handleReagentChange(
                                reagent.id,
                                "volumeUsed",
                                e.target.value,
                              )
                            }
                            placeholder="10"
                            type="number"
                            size="sm"
                          />
                        </Column>
                        <Column lg={2} md={2} sm={2}>
                          <div style={{ marginTop: "1.5rem" }}>
                            <Button
                              kind="danger--ghost"
                              size="sm"
                              renderIcon={TrashCan}
                              iconDescription="Remove"
                              onClick={() => handleRemoveReagent(reagent.id)}
                            >
                              <FormattedMessage
                                id="virology.reagent.remove"
                                defaultMessage="Remove"
                              />
                            </Button>
                          </div>
                        </Column>
                      </Grid>
                    </div>
                  ))}
                </div>
              )}
            </Tile>
          </Column>

          {/* Info about selected samples */}
          <Column lg={16}>
            <div style={{ marginTop: "1rem" }}>
              <Tag type="blue">
                {selectedSampleIds.length} sample(s) selected
              </Tag>
            </div>
          </Column>
        </Grid>
      </Modal>

      {/* Feeding History Modal */}
      <Modal
        open={historyModalOpen}
        onRequestClose={() => {
          setHistoryModalOpen(false);
          setSelectedSampleHistory(null);
        }}
        modalHeading={
          <FormattedMessage
            id="virology.feeding.history.title"
            defaultMessage="Feeding History - {sampleId}"
            values={{ sampleId: selectedSampleHistory?.sampleId || "" }}
          />
        }
        passiveModal
        size="lg"
      >
        <div style={{ maxHeight: "500px", overflowY: "auto" }}>
          {selectedSampleHistory?.history?.map((feeding, index) => (
            <Tile
              key={index}
              style={{
                marginBottom: "1rem",
                border: "1px solid #e0e0e0",
              }}
            >
              <div style={{ marginBottom: "0.5rem" }}>
                <Tag type="blue" size="sm">
                  Feeding #{selectedSampleHistory.history.length - index}
                </Tag>
                {index === selectedSampleHistory.history.length - 1 && (
                  <Tag type="green" size="sm" style={{ marginLeft: "0.5rem" }}>
                    Most Recent
                  </Tag>
                )}
              </div>

              <Grid narrow>
                <Column lg={8} md={4} sm={4}>
                  <div style={{ marginBottom: "0.5rem" }}>
                    <strong>Date:</strong> {feeding.feedingDate || "-"}
                    {feeding.feedingTime && ` at ${feeding.feedingTime}`}
                  </div>
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <div style={{ marginBottom: "0.5rem" }}>
                    <strong>Schedule:</strong> {feeding.feedingSchedule || "-"}
                  </div>
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <div style={{ marginBottom: "0.5rem" }}>
                    <strong>Batch #:</strong> {feeding.batchNumber || "-"}
                  </div>
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <div style={{ marginBottom: "0.5rem" }}>
                    <strong>Volume Used:</strong>{" "}
                    {feeding.volumeUsed ? `${feeding.volumeUsed} mL` : "-"}
                  </div>
                </Column>

                {feeding.reagents && feeding.reagents.length > 0 && (
                  <Column lg={16}>
                    <div style={{ marginTop: "0.5rem" }}>
                      <strong>Reagents Used:</strong>
                      <ul
                        style={{ marginTop: "0.25rem", paddingLeft: "1.5rem" }}
                      >
                        {feeding.reagents.map((reagent, rIndex) => (
                          <li key={rIndex}>
                            <strong>{reagent.name}</strong>
                            {reagent.volumeUsed &&
                              ` (${reagent.volumeUsed} mL)`}
                            <br />
                            <span
                              style={{ fontSize: "0.875rem", color: "#6f6f6f" }}
                            >
                              Lot: {reagent.lotNumber} | Expiry:{" "}
                              {reagent.expiryDate}
                              {reagent.category && ` | ${reagent.category}`}
                            </span>
                          </li>
                        ))}
                      </ul>
                    </div>
                  </Column>
                )}

                {feeding.notes && (
                  <Column lg={16}>
                    <div
                      style={{
                        marginTop: "0.5rem",
                        padding: "0.5rem",
                        backgroundColor: "#f4f4f4",
                        borderRadius: "4px",
                      }}
                    >
                      <strong>Notes:</strong>
                      <p style={{ marginTop: "0.25rem", marginBottom: 0 }}>
                        {feeding.notes}
                      </p>
                    </div>
                  </Column>
                )}
              </Grid>
            </Tile>
          ))}

          {(!selectedSampleHistory?.history ||
            selectedSampleHistory.history.length === 0) && (
            <p
              style={{ textAlign: "center", color: "#6f6f6f", padding: "2rem" }}
            >
              No feeding history available.
            </p>
          )}
        </div>
      </Modal>
    </div>
  );
}

export default VirologyFeedingPage;
