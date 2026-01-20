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
  Tag,
  Modal,
  TextInput,
  TextArea,
  DatePicker,
  DatePickerInput,
} from "@carbon/react";
import { Save, Checkmark } from "@carbon/react/icons";
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
 * VirologyVirusIsolationPage - Page for isolating virus from culture batches.
 * Displays samples and allows logging isolation methods and linking to culture batch IDs.
 *
 * Features:
 * - Display and select samples from the notebook
 * - Open dialog to log virus isolation
 * - Link to culture batch ID from previous workflow steps
 * - Record isolation method, date, and results
 * - Attach selected samples to the isolation batch
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - The notebook page data
 * @param {Object} props.progress - Page progress
 * @param {function} props.onProgressUpdate - Callback when progress changes
 */
function VirologyVirusIsolationPage({
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

  // Debug logging for selection
  useEffect(() => {
    console.log("📝 VirologyVirusIsolationPage selectedSampleIds changed:", selectedSampleIds);
  }, [selectedSampleIds]);

  // Form data - simplified to match requirements
  const [cultureBatchId, setCultureBatchId] = useState("");
  const [isolationDate, setIsolationDate] = useState("");
  const [isolationMethod, setIsolationMethod] = useState("");
  const [isolationNotes, setIsolationNotes] = useState("");

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
    console.log("🚀 VirologyVirusIsolationPage VERSION: 2026-01-20-v2 LOADED");
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

    getFromOpenElisServer(
      `/rest/notebook/page/${pageData.id}/samples`,
      (response) => {
        console.log(
          "Loaded samples for virus isolation page",
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
                // Virus isolation data - simplified
                cultureBatchId: sample.data?.cultureBatchId,
                isolationMethod: sample.data?.isolationMethod,
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

  const handleSaveIsolation = useCallback(() => {
    // Validation - simplified
    if (!isolationMethod) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.error" }),
        subtitle: "Please enter an isolation method",
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
      cultureBatchId: cultureBatchId || null,
      isolationDate: isolationDate || null,
      isolationMethod,
      notes: isolationNotes,
    };

    console.log("Virus Isolation payload:", JSON.stringify(payload, null, 2));

    postToOpenElisServerJsonResponse(
      "/rest/virology/virus-isolation",
      JSON.stringify(payload),
      (response) => {
        setLoading(false);

        if (response.success || response.isolationId) {
          const samplesUpdated = response.samplesUpdated || 0;
          notify({
            kind: NotificationKinds.success,
            title: intl.formatMessage({ id: "notification.success" }),
            subtitle: `Virus isolation logged successfully. ${samplesUpdated} sample(s) updated.`,
          });
          console.log("Backend response:", response);

          // Clear form and selection
          setCultureBatchId("");
          setIsolationDate("");
          setIsolationMethod("");
          setIsolationNotes("");
          setSelectedSampleIds([]);
          setModalOpen(false);

          // Reload samples to show updated isolation data
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
    cultureBatchId,
    isolationDate,
    isolationMethod,
    isolationNotes,
    selectedSampleIds,
    pageData?.id,
    intl,
    notify,
    loadPageSamples,
    onProgressUpdate,
  ]);

  // Handle completing isolation and moving samples to next page
  const handleCompleteIsolation = useCallback(() => {
    if (selectedSampleIds.length === 0) {
      notify({
        kind: NotificationKinds.warning,
        title: intl.formatMessage({
          id: "notification.title",
          defaultMessage: "Notification",
        }),
        subtitle: intl.formatMessage({
          id: "virology.isolation.error.noSelection",
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
          id: "virology.isolation.error.noPage",
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
                id: "virology.isolation.success.completed",
                defaultMessage:
                  "Completed virus isolation for {count} sample(s). They will proceed to titer measurement.",
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
              id: "virology.isolation.error.complete",
              defaultMessage:
                "Failed to complete virus isolation. Please try again.",
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

  // Debug logging
  useEffect(() => {
    console.log("📊 Sample counts:", {
      total: samples.length,
      pending: pendingCount,
      completed: completedCount,
      pendingSamples: pendingSamples.map((s) => ({
        id: s.id,
        status: s.status,
        externalId: s.externalId,
      })),
      completedSamples: completedSamples.map((s) => ({
        id: s.id,
        status: s.status,
        externalId: s.externalId,
      })),
    });
  }, [samples, pendingCount, completedCount, pendingSamples, completedSamples]);

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
        key: "cultureBatchId",
        header: intl.formatMessage({
          id: "virology.isolation.column.cultureBatch",
          defaultMessage: "Culture Batch ID",
        }),
        render: (value, sample) => {
          const batch =
            value || sample?.cultureBatchId || sample?.data?.cultureBatchId;
          return batch ? (
            <Tag type="cyan" size="sm">
              {batch}
            </Tag>
          ) : (
            "-"
          );
        },
      },
      {
        key: "isolationMethod",
        header: intl.formatMessage({
          id: "virology.isolation.column.method",
          defaultMessage: "Isolation Method",
        }),
        render: (value, sample) => {
          const method =
            value || sample?.isolationMethod || sample?.data?.isolationMethod;
          return method || "-";
        },
      },
    ],
    [intl],
  );

  return (
    <div className="virology-isolation-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="virology.isolation.title"
            defaultMessage="Virus Isolation from Culture"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="virology.isolation.page.description"
            defaultMessage="Isolate virus from culture batches and link to source batch IDs for full traceability."
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
            id="virology.isolation.open"
            defaultMessage="Log Virus Isolation"
          />
        </Button>
        <Button
          kind="tertiary"
          size="md"
          renderIcon={Checkmark}
          onClick={handleCompleteIsolation}
          disabled={loading || selectedSampleIds.length === 0}
          style={{ marginLeft: "0.5rem" }}
        >
          <FormattedMessage
            id="virology.isolation.complete"
            defaultMessage="Complete Isolation ({count})"
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
                id="virology.isolation.samples.pending"
                defaultMessage="Samples Pending Virus Isolation"
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
                id="virology.isolation.samples.description"
                defaultMessage="Select samples to log virus isolation. Click 'Log Virus Isolation' to record isolation method and link to culture batch."
              />
            </p>
          </div>
          <div className="sample-grid-container">
            <SampleGrid
              gridId="isolation-pending-samples"
              samples={pendingSamples}
              selectedIds={selectedSampleIds}
              onSelectionChange={(newSelection) => {
                console.log("🎯 VirologyVirusIsolationPage onSelectionChange callback", {
                  from: selectedSampleIds,
                  to: newSelection,
                });
                setSelectedSampleIds(newSelection);
              }}
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
                id="virology.isolation.samples.completed"
                defaultMessage="Completed Virus Isolation"
              />
              <Tag type="green" size="sm" className="count-tag">
                {completedCount}
              </Tag>
            </h5>
          </div>
          <div className="sample-grid-container">
            <SampleGrid
              gridId="isolation-completed-samples"
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
              id="virology.isolation.samples.empty"
              defaultMessage="No samples available for virus isolation. Complete previous workflow steps first."
            />
          </p>
        </div>
      )}

      {/* Virus Isolation Logging Modal */}
      <Modal
        open={modalOpen}
        onRequestClose={() => setModalOpen(false)}
        modalHeading={
          <FormattedMessage
            id="virology.isolation.modal.title"
            defaultMessage="Log Virus Isolation"
          />
        }
        modalLabel={
          <FormattedMessage
            id="virology.isolation.modal.subtitle"
            defaultMessage="Record virus isolation method and link to source culture batch"
          />
        }
        primaryButtonText={
          <FormattedMessage
            id="virology.isolation.save"
            defaultMessage="Save Isolation Log"
          />
        }
        secondaryButtonText={
          <FormattedMessage id="button.cancel" defaultMessage="Cancel" />
        }
        onRequestSubmit={handleSaveIsolation}
        primaryButtonDisabled={loading || !isolationMethod}
        size="md"
      >
        <Grid fullWidth>
          {/* Culture Batch ID (link to culture) */}
          <Column lg={8}>
            <TextInput
              id="culture-batch-id"
              labelText={
                <FormattedMessage
                  id="virology.isolation.cultureBatch"
                  defaultMessage="Culture Batch ID"
                />
              }
              placeholder="Link to culture batch (optional)"
              value={cultureBatchId}
              onChange={(e) => setCultureBatchId(e.target.value)}
            />
          </Column>

          {/* Isolation Date */}
          <Column lg={8}>
            <DatePicker
              datePickerType="single"
              dateFormat="Y-m-d"
              value={isolationDate}
              onChange={(dates) =>
                setIsolationDate(
                  dates[0] ? dates[0].toISOString().split("T")[0] : "",
                )
              }
            >
              <DatePickerInput
                id="isolation-date"
                placeholder="YYYY-MM-DD"
                labelText={
                  <FormattedMessage
                    id="virology.isolation.date"
                    defaultMessage="Isolation Date"
                  />
                }
              />
            </DatePicker>
          </Column>

          {/* Isolation Method - FREE TEXT */}
          <Column lg={16}>
            <TextInput
              id="isolation-method"
              labelText={
                <span>
                  <FormattedMessage
                    id="virology.isolation.method"
                    defaultMessage="Isolation Method"
                  />
                  {" *"}
                </span>
              }
              placeholder="e.g., Freeze-thaw, Centrifugation, Filtration, etc."
              value={isolationMethod}
              onChange={(e) => setIsolationMethod(e.target.value)}
              invalid={!isolationMethod}
              invalidText="Required"
            />
          </Column>

          {/* Isolation Notes */}
          <Column lg={16}>
            <TextArea
              id="isolation-notes"
              labelText={
                <FormattedMessage
                  id="virology.isolation.notes"
                  defaultMessage="Notes"
                />
              }
              placeholder="Add any observations about the isolation process..."
              value={isolationNotes}
              onChange={(e) => setIsolationNotes(e.target.value)}
              rows={4}
            />
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
    </div>
  );
}

export default VirologyVirusIsolationPage;
