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
  Tile,
  TextInput,
  TextArea,
  DatePicker,
  DatePickerInput,
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
 * VirologyTiterMeasurementPage - Page for quantifying viral load.
 * Displays samples and allows logging titer measurements (TCID50, PFU/ml, etc.).
 *
 * Features:
 * - Display and select samples from the notebook
 * - Open dialog to log titer measurements
 * - Record multiple titer values with different assay methods
 * - Track dilution factors and calculation methods
 * - Multiple measurements per sample (array-based history)
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - The notebook page data
 * @param {Object} props.progress - Page progress
 * @param {function} props.onProgressUpdate - Callback when progress changes
 */
function VirologyTiterMeasurementPage({
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

  // Form data - simplified
  const [measurementDate, setMeasurementDate] = useState("");
  const [titerValue, setTiterValue] = useState("");
  const [titerUnit, setTiterUnit] = useState("");
  const [measurementNotes, setMeasurementNotes] = useState("");

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
          "Loaded samples for titer measurement page",
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
                // Titer measurement history (array)
                titerHistory: sample.data?.titerHistory || [],
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

  const handleSaveTiterMeasurement = useCallback(() => {
    // Validation - simplified
    if (!titerValue) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.error" }),
        subtitle: "Please enter a titer value",
      });
      return;
    }

    if (!titerUnit) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.error" }),
        subtitle: "Please enter a titer unit (e.g., TCID50/mL, PFU/mL)",
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
      measurementDate: measurementDate || null,
      titerValue,
      titerUnit,
      notes: measurementNotes,
    };

    console.log("Titer Measurement payload:", JSON.stringify(payload, null, 2));

    postToOpenElisServerJsonResponse(
      "/rest/virology/titer-measurement",
      JSON.stringify(payload),
      (response) => {
        setLoading(false);

        if (response.success || response.measurementId) {
          const samplesUpdated = response.samplesUpdated || 0;
          notify({
            kind: NotificationKinds.success,
            title: intl.formatMessage({ id: "notification.success" }),
            subtitle: `Titer measurement logged successfully. ${samplesUpdated} sample(s) updated.`,
          });
          console.log("Backend response:", response);

          // Clear form and selection
          setMeasurementDate("");
          setTiterValue("");
          setTiterUnit("");
          setMeasurementNotes("");
          setSelectedSampleIds([]);
          setModalOpen(false);

          // Reload samples to show updated titer data
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
    measurementDate,
    titerValue,
    titerUnit,
    measurementNotes,
    selectedSampleIds,
    pageData?.id,
    intl,
    notify,
    loadPageSamples,
    onProgressUpdate,
  ]);

  // Handle completing titer measurement and moving samples to next page
  const handleCompleteTiterMeasurement = useCallback(() => {
    if (selectedSampleIds.length === 0) {
      notify({
        kind: NotificationKinds.warning,
        title: intl.formatMessage({
          id: "notification.title",
          defaultMessage: "Notification",
        }),
        subtitle: intl.formatMessage({
          id: "virology.titer.error.noSelection",
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
          id: "virology.titer.error.noPage",
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
                id: "virology.titer.success.completed",
                defaultMessage:
                  "Completed titer measurement for {count} sample(s). Viral load quantification is complete.",
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
              id: "virology.titer.error.complete",
              defaultMessage:
                "Failed to complete titer measurement. Please try again.",
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
        key: "titerHistory",
        header: intl.formatMessage({
          id: "virology.titer.column.measurementCount",
          defaultMessage: "Measurement Count",
        }),
        render: (value, sample) => {
          const history =
            value || sample?.titerHistory || sample?.data?.titerHistory;
          if (!Array.isArray(history) || history.length === 0) return "-";

          return (
            <div
              style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}
            >
              <Tag type="blue" size="sm">
                {history.length} measurement{history.length !== 1 ? "s" : ""}
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
        key: "latestTiter",
        header: intl.formatMessage({
          id: "virology.titer.column.latestTiter",
          defaultMessage: "Latest Titer",
        }),
        render: (value, sample) => {
          const history = sample?.titerHistory || sample?.data?.titerHistory;
          if (!Array.isArray(history) || history.length === 0) return "-";

          // Get the most recent measurement (last entry in array)
          const latest = history[history.length - 1];
          return latest?.titerValue && latest?.titerUnit
            ? `${latest.titerValue} ${latest.titerUnit}`
            : "-";
        },
      },
    ],
    [intl],
  );

  return (
    <div className="virology-titer-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="virology.titer.title"
            defaultMessage="Viral Load Quantification (Titer Measurement)"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="virology.titer.page.description"
            defaultMessage="Quantify viral load using various assay methods. Record titer values (TCID50, PFU/mL, etc.) with full traceability."
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
            id="virology.titer.open"
            defaultMessage="Log Titer Measurement"
          />
        </Button>
        <Button
          kind="tertiary"
          size="md"
          renderIcon={Checkmark}
          onClick={handleCompleteTiterMeasurement}
          disabled={loading || selectedSampleIds.length === 0}
          style={{ marginLeft: "0.5rem" }}
        >
          <FormattedMessage
            id="virology.titer.complete"
            defaultMessage="Complete Measurement ({count})"
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
                id="virology.titer.samples.pending"
                defaultMessage="Samples Pending Titer Measurement"
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
                id="virology.titer.samples.description"
                defaultMessage="Select samples to log titer measurements. Click 'Log Titer Measurement' to record viral load quantification."
              />
            </p>
          </div>
          <div className="sample-grid-container">
            <SampleGrid
              gridId="titer-pending-samples"
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
                id="virology.titer.samples.completed"
                defaultMessage="Completed Titer Measurement"
              />
              <Tag type="green" size="sm" className="count-tag">
                {completedCount}
              </Tag>
            </h5>
          </div>
          <div className="sample-grid-container">
            <SampleGrid
              gridId="titer-completed-samples"
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
              id="virology.titer.samples.empty"
              defaultMessage="No samples available for titer measurement. Complete previous workflow steps first."
            />
          </p>
        </div>
      )}

      {/* Titer Measurement Logging Modal */}
      <Modal
        open={modalOpen}
        onRequestClose={() => setModalOpen(false)}
        modalHeading={
          <FormattedMessage
            id="virology.titer.modal.title"
            defaultMessage="Log Titer Measurement"
          />
        }
        modalLabel={
          <FormattedMessage
            id="virology.titer.modal.subtitle"
            defaultMessage="Record viral load quantification using selected assay method"
          />
        }
        primaryButtonText={
          <FormattedMessage
            id="virology.titer.save"
            defaultMessage="Save Titer Measurement"
          />
        }
        secondaryButtonText={
          <FormattedMessage id="button.cancel" defaultMessage="Cancel" />
        }
        onRequestSubmit={handleSaveTiterMeasurement}
        primaryButtonDisabled={loading || !titerValue || !titerUnit}
        size="md"
      >
        <Grid fullWidth>
          {/* Measurement Date */}
          <Column lg={8}>
            <DatePicker
              datePickerType="single"
              dateFormat="Y-m-d"
              value={measurementDate}
              onChange={(dates) =>
                setMeasurementDate(
                  dates[0] ? dates[0].toISOString().split("T")[0] : "",
                )
              }
            >
              <DatePickerInput
                id="measurement-date"
                placeholder="YYYY-MM-DD"
                labelText={
                  <FormattedMessage
                    id="virology.titer.date"
                    defaultMessage="Measurement Date"
                  />
                }
              />
            </DatePicker>
          </Column>

          {/* Titer Value */}
          <Column lg={8}>
            <TextInput
              id="titer-value"
              labelText={
                <span>
                  <FormattedMessage
                    id="virology.titer.value"
                    defaultMessage="Titer Value"
                  />
                  {" *"}
                </span>
              }
              value={titerValue}
              onChange={(e) => setTiterValue(e.target.value)}
              placeholder="e.g., 1.5E+06 or 1.5x10^6"
              invalid={!titerValue}
              invalidText="Required"
            />
          </Column>

          {/* Titer Unit - FREE TEXT */}
          <Column lg={16}>
            <TextInput
              id="titer-unit"
              labelText={
                <span>
                  <FormattedMessage
                    id="virology.titer.unit"
                    defaultMessage="Titer Unit"
                  />
                  {" *"}
                </span>
              }
              placeholder="e.g., TCID50/mL, PFU/mL, FFU/mL, etc."
              value={titerUnit}
              onChange={(e) => setTiterUnit(e.target.value)}
              invalid={!titerUnit}
              invalidText="Required"
            />
          </Column>

          {/* Measurement Notes */}
          <Column lg={16}>
            <TextArea
              id="measurement-notes"
              labelText={
                <FormattedMessage
                  id="virology.titer.notes"
                  defaultMessage="Notes"
                />
              }
              placeholder="Add any observations about the measurement..."
              value={measurementNotes}
              onChange={(e) => setMeasurementNotes(e.target.value)}
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

      {/* Titer Measurement History Modal */}
      <Modal
        open={historyModalOpen}
        onRequestClose={() => {
          setHistoryModalOpen(false);
          setSelectedSampleHistory(null);
        }}
        modalHeading={
          <FormattedMessage
            id="virology.titer.history.title"
            defaultMessage="Titer Measurement History - {sampleId}"
            values={{ sampleId: selectedSampleHistory?.sampleId || "" }}
          />
        }
        passiveModal
        size="lg"
      >
        <div style={{ maxHeight: "500px", overflowY: "auto" }}>
          {selectedSampleHistory?.history?.map((measurement, index) => (
            <Tile
              key={index}
              style={{
                marginBottom: "1rem",
                border: "1px solid #e0e0e0",
              }}
            >
              <div style={{ marginBottom: "0.5rem" }}>
                <Tag type="blue" size="sm">
                  Measurement #{selectedSampleHistory.history.length - index}
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
                    <strong>Date:</strong> {measurement.measurementDate || "-"}
                  </div>
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <div style={{ marginBottom: "0.5rem" }}>
                    <strong>Titer Value:</strong>{" "}
                    {measurement.titerValue && measurement.titerUnit
                      ? `${measurement.titerValue} ${measurement.titerUnit}`
                      : "-"}
                  </div>
                </Column>

                {measurement.notes && (
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
                        {measurement.notes}
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
              No titer measurement history available.
            </p>
          )}
        </div>
      </Modal>
    </div>
  );
}

export default VirologyTiterMeasurementPage;
