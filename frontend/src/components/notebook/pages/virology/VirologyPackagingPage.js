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
  Dropdown,
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
 * VirologyPackagingPage - Final product packaging page.
 * Displays samples and allows logging packaging specifications.
 *
 * Features:
 * - Display and select samples from the notebook
 * - Open dialog to log packaging details
 * - Track batch ID, vial type, fill volume, labeling information
 * - Attach selected samples to the packaging batch
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - The notebook page data
 * @param {Object} props.progress - Page progress
 * @param {function} props.onProgressUpdate - Callback when progress changes
 */
function VirologyPackagingPage({
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

  // Form data
  const [batchId, setBatchId] = useState("");
  const [vialType, setVialType] = useState(null);
  const [fillVolume, setFillVolume] = useState("");
  const [labelingInfo, setLabelingInfo] = useState("");
  const [packagingNotes, setPackagingNotes] = useState("");
  const [packagingDate, setPackagingDate] = useState("");
  const [expiryDate, setExpiryDate] = useState("");
  const [storageConditions, setStorageConditions] = useState("");

  // Vial type options
  const vialTypeOptions = [
    {
      id: "glass_vial_2ml",
      text: intl.formatMessage({
        id: "virology.packaging.vialType.glass2ml",
        defaultMessage: "Glass Vial 2mL",
      }),
    },
    {
      id: "glass_vial_5ml",
      text: intl.formatMessage({
        id: "virology.packaging.vialType.glass5ml",
        defaultMessage: "Glass Vial 5mL",
      }),
    },
    {
      id: "glass_vial_10ml",
      text: intl.formatMessage({
        id: "virology.packaging.vialType.glass10ml",
        defaultMessage: "Glass Vial 10mL",
      }),
    },
    {
      id: "plastic_vial_2ml",
      text: intl.formatMessage({
        id: "virology.packaging.vialType.plastic2ml",
        defaultMessage: "Plastic Vial 2mL",
      }),
    },
    {
      id: "plastic_vial_5ml",
      text: intl.formatMessage({
        id: "virology.packaging.vialType.plastic5ml",
        defaultMessage: "Plastic Vial 5mL",
      }),
    },
    {
      id: "ampoule_1ml",
      text: intl.formatMessage({
        id: "virology.packaging.vialType.ampoule1ml",
        defaultMessage: "Ampoule 1mL",
      }),
    },
    {
      id: "ampoule_2ml",
      text: intl.formatMessage({
        id: "virology.packaging.vialType.ampoule2ml",
        defaultMessage: "Ampoule 2mL",
      }),
    },
  ];

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
          "Loaded samples for packaging page",
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
                // Packaging data
                batchId: sample.data?.batchId,
                vialType: sample.data?.vialType,
                fillVolume: sample.data?.fillVolume,
                labelingInfo: sample.data?.labelingInfo,
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

  const handleSavePackaging = useCallback(() => {
    // Validation
    if (!batchId) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.error" }),
        subtitle: "Please enter a batch ID",
      });
      return;
    }

    if (!vialType) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.error" }),
        subtitle: "Please select a vial type",
      });
      return;
    }

    if (!fillVolume) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.error" }),
        subtitle: "Please enter fill volume",
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
      batchId,
      vialType: vialType.id,
      vialTypeName: vialType.text,
      fillVolume,
      labelingInfo,
      packagingDate,
      expiryDate,
      storageConditions,
      notes: packagingNotes,
    };

    console.log("Packaging payload:", JSON.stringify(payload, null, 2));

    postToOpenElisServerJsonResponse(
      "/rest/virology/packaging",
      JSON.stringify(payload),
      (response) => {
        setLoading(false);

        if (response.success || response.packagingId) {
          const samplesUpdated = response.samplesUpdated || 0;
          notify({
            kind: NotificationKinds.success,
            title: intl.formatMessage({ id: "notification.success" }),
            subtitle: `Packaging logged successfully. ${samplesUpdated} sample(s) updated.`,
          });
          console.log("Backend response:", response);

          // Clear form and selection
          setBatchId("");
          setVialType(null);
          setFillVolume("");
          setLabelingInfo("");
          setPackagingDate("");
          setExpiryDate("");
          setStorageConditions("");
          setPackagingNotes("");
          setSelectedSampleIds([]);
          setModalOpen(false);

          // Reload samples to show updated packaging data
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
    batchId,
    vialType,
    fillVolume,
    labelingInfo,
    packagingDate,
    expiryDate,
    storageConditions,
    packagingNotes,
    selectedSampleIds,
    pageData?.id,
    intl,
    notify,
    loadPageSamples,
    onProgressUpdate,
  ]);

  // Handle completing packaging and moving samples to next page
  const handleCompletePackaging = useCallback(() => {
    if (selectedSampleIds.length === 0) {
      notify({
        kind: NotificationKinds.warning,
        title: intl.formatMessage({
          id: "notification.title",
          defaultMessage: "Notification",
        }),
        subtitle: intl.formatMessage({
          id: "virology.packaging.error.noSelection",
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
          id: "virology.packaging.error.noPage",
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
                id: "virology.packaging.success.completed",
                defaultMessage:
                  "Completed packaging for {count} sample(s). Final products are ready.",
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
              id: "virology.packaging.error.complete",
              defaultMessage: "Failed to complete packaging. Please try again.",
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
        key: "batchId",
        header: intl.formatMessage({
          id: "virology.packaging.column.batchId",
          defaultMessage: "Batch ID",
        }),
        render: (value, sample) => {
          const batch = value || sample?.batchId || sample?.data?.batchId;
          return batch || "-";
        },
      },
      {
        key: "vialType",
        header: intl.formatMessage({
          id: "virology.packaging.column.vialType",
          defaultMessage: "Vial Type",
        }),
        render: (value, sample) => {
          const vial = value || sample?.vialType || sample?.data?.vialTypeName;
          return vial || "-";
        },
      },
      {
        key: "fillVolume",
        header: intl.formatMessage({
          id: "virology.packaging.column.fillVolume",
          defaultMessage: "Fill Volume",
        }),
        render: (value, sample) => {
          const volume =
            value || sample?.fillVolume || sample?.data?.fillVolume;
          return volume ? `${volume} mL` : "-";
        },
      },
      {
        key: "labelingInfo",
        header: intl.formatMessage({
          id: "virology.packaging.column.labeling",
          defaultMessage: "Labeling",
        }),
        render: (value, sample) => {
          const labeling =
            value || sample?.labelingInfo || sample?.data?.labelingInfo;
          if (!labeling) return "-";
          // Truncate long labeling info
          return labeling.length > 50
            ? labeling.substring(0, 50) + "..."
            : labeling;
        },
      },
    ],
    [intl],
  );

  return (
    <div className="virology-packaging-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="virology.packaging.title"
            defaultMessage="Final Product Packaging"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="virology.packaging.page.description"
            defaultMessage="Log final product packaging specifications including batch ID, vial type, fill volume, and labeling information."
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
            id="virology.packaging.open"
            defaultMessage="Log Packaging"
          />
        </Button>
        <Button
          kind="tertiary"
          size="md"
          renderIcon={Checkmark}
          onClick={handleCompletePackaging}
          disabled={loading || selectedSampleIds.length === 0}
          style={{ marginLeft: "0.5rem" }}
        >
          <FormattedMessage
            id="virology.packaging.complete"
            defaultMessage="Complete Packaging ({count})"
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
                id="virology.packaging.samples.pending"
                defaultMessage="Samples Pending Packaging"
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
                id="virology.packaging.samples.description"
                defaultMessage="Select samples to log packaging specifications. Click 'Log Packaging' to record batch ID, vial type, and fill volume."
              />
            </p>
          </div>
          <div className="sample-grid-container">
            <SampleGrid
              gridId="packaging-pending-samples"
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
                id="virology.packaging.samples.completed"
                defaultMessage="Completed Packaging"
              />
              <Tag type="green" size="sm" className="count-tag">
                {completedCount}
              </Tag>
            </h5>
          </div>
          <div className="sample-grid-container">
            <SampleGrid
              gridId="packaging-completed-samples"
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
              id="virology.packaging.samples.empty"
              defaultMessage="No samples available for packaging. Complete previous workflow steps first."
            />
          </p>
        </div>
      )}

      {/* Packaging Logging Modal */}
      <Modal
        open={modalOpen}
        onRequestClose={() => setModalOpen(false)}
        modalHeading={
          <FormattedMessage
            id="virology.packaging.modal.title"
            defaultMessage="Log Product Packaging"
          />
        }
        modalLabel={
          <FormattedMessage
            id="virology.packaging.modal.subtitle"
            defaultMessage="Record packaging specifications for final product"
          />
        }
        primaryButtonText={
          <FormattedMessage
            id="virology.packaging.save"
            defaultMessage="Save Packaging Log"
          />
        }
        secondaryButtonText={
          <FormattedMessage id="button.cancel" defaultMessage="Cancel" />
        }
        onRequestSubmit={handleSavePackaging}
        primaryButtonDisabled={loading || !batchId || !vialType || !fillVolume}
        size="md"
      >
        <Grid fullWidth>
          {/* Batch ID */}
          <Column lg={8}>
            <TextInput
              id="batch-id"
              labelText={
                <span>
                  <FormattedMessage
                    id="virology.packaging.batchId"
                    defaultMessage="Batch ID"
                  />
                  {" *"}
                </span>
              }
              value={batchId}
              onChange={(e) => setBatchId(e.target.value)}
              placeholder="e.g., PKG-2026-001"
              invalid={!batchId}
              invalidText="Required"
            />
          </Column>

          {/* Packaging Date */}
          <Column lg={8}>
            <TextInput
              id="packaging-date"
              labelText={
                <FormattedMessage
                  id="virology.packaging.date"
                  defaultMessage="Packaging Date"
                />
              }
              value={packagingDate}
              onChange={(e) => setPackagingDate(e.target.value)}
              type="date"
            />
          </Column>

          {/* Vial Type */}
          <Column lg={8}>
            <Dropdown
              id="vial-type"
              titleText={
                <span>
                  <FormattedMessage
                    id="virology.packaging.vialType"
                    defaultMessage="Vial Type"
                  />
                  {" *"}
                </span>
              }
              label="Select vial type..."
              items={vialTypeOptions}
              itemToString={(item) => (item ? item.text : "")}
              selectedItem={vialType}
              onChange={({ selectedItem }) => setVialType(selectedItem)}
              disabled={loading}
              invalid={!vialType}
              invalidText="Required"
            />
          </Column>

          {/* Fill Volume */}
          <Column lg={8}>
            <TextInput
              id="fill-volume"
              labelText={
                <span>
                  <FormattedMessage
                    id="virology.packaging.fillVolume"
                    defaultMessage="Fill Volume (mL)"
                  />
                  {" *"}
                </span>
              }
              value={fillVolume}
              onChange={(e) => setFillVolume(e.target.value)}
              placeholder="e.g., 2.0"
              type="number"
              step="0.1"
              invalid={!fillVolume}
              invalidText="Required"
            />
          </Column>

          {/* Expiry Date */}
          <Column lg={8}>
            <TextInput
              id="expiry-date"
              labelText={
                <FormattedMessage
                  id="virology.packaging.expiryDate"
                  defaultMessage="Expiry Date"
                />
              }
              value={expiryDate}
              onChange={(e) => setExpiryDate(e.target.value)}
              type="date"
            />
          </Column>

          {/* Storage Conditions */}
          <Column lg={8}>
            <TextInput
              id="storage-conditions"
              labelText={
                <FormattedMessage
                  id="virology.packaging.storage"
                  defaultMessage="Storage Conditions"
                />
              }
              value={storageConditions}
              onChange={(e) => setStorageConditions(e.target.value)}
              placeholder="e.g., -80°C, 2-8°C"
            />
          </Column>

          {/* Labeling Information */}
          <Column lg={16}>
            <TextArea
              id="labeling-info"
              labelText={
                <FormattedMessage
                  id="virology.packaging.labeling"
                  defaultMessage="Labeling Information"
                />
              }
              placeholder="Enter label details: product name, batch number, expiry date, storage instructions, warnings, etc."
              value={labelingInfo}
              onChange={(e) => setLabelingInfo(e.target.value)}
              rows={3}
            />
          </Column>

          {/* Packaging Notes */}
          <Column lg={16}>
            <TextArea
              id="packaging-notes"
              labelText={
                <FormattedMessage
                  id="virology.packaging.notes"
                  defaultMessage="Packaging Notes"
                />
              }
              placeholder="Add any additional observations or notes about the packaging process..."
              value={packagingNotes}
              onChange={(e) => setPackagingNotes(e.target.value)}
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

export default VirologyPackagingPage;
