import React, {
  useState,
  useEffect,
  useRef,
  useCallback,
  useMemo,
  useContext,
} from "react";
import { Grid, Column, Button, Tile, Tag } from "@carbon/react";
import { Upload, Checkmark } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
} from "../../../utils/Utils";
import SampleGrid from "../../workflow/SampleGrid";
import VirologyManifestImportModal from "../../workflow/VirologyManifestImportModal";
import { NotificationContext } from "../../../layout/Layout";
import { NotificationKinds } from "../../../common/CustomNotification";
import "../../workflow/NotebookWorkflow.css";

/**
 * VirologySampleReceptionPage - Page 1 of the Virology & Vaccine Unit workflow.
 * Captures full reception metadata and sets status to "Received - Pending Verification".
 *
 * Reception Metadata (per PDF spec):
 * - Sample ID
 * - Source (patient, animal model, environmental, production batch)
 * - Sample Type
 * - Reception Date & Time
 * - Test Type (viral vs. bacterial)
 * - Project/Study Association
 *
 * Virus/Vaccine Production Metadata:
 * - Batch ID
 * - Production Stage
 * - Cell Line Used
 * - Passage Number
 * - Titer Values
 * - Quality Control Results
 * - Formulation Details
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - The notebook page data
 * @param {Object} props.progress - Page progress
 * @param {function} props.onProgressUpdate - Callback when progress changes
 */
function VirologySampleReceptionPage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
}) {
  const intl = useIntl();
  const { addNotification, setNotificationVisible } =
    useContext(NotificationContext);
  const componentMounted = useRef(false);

  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [loading, setLoading] = useState(true);

  const [importModalOpen, setImportModalOpen] = useState(false);

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
        if (componentMounted.current) {
          if (response && Array.isArray(response)) {
            const transformedSamples = response.map((sample) => ({
              id: String(sample.id || sample.sampleItemId),
              externalId: sample.externalId,
              accessionNumber: sample.accessionNumber,
              sampleType: sample.sampleType || sample.typeOfSample?.description,
              collectionDate: sample.collectionDate,
              status: sample.pageStatus || sample.status || "PENDING",
              // Virology-specific reception metadata from JSONB data
              sampleId: sample.data?.sampleId,
              source: sample.data?.source,
              testType: sample.data?.testType,
              projectStudyAssociation: sample.data?.projectStudyAssociation,
              receptionDateTime: sample.data?.receptionDateTime,
              // Virus/Vaccine Production metadata
              batchId: sample.data?.batchId,
              productionStage: sample.data?.productionStage,
              cellLineUsed: sample.data?.cellLineUsed,
              passageNumber: sample.data?.passageNumber,
              titerValues: sample.data?.titerValues,
              qualityControlResults: sample.data?.qualityControlResults,
              formulationDetails: sample.data?.formulationDetails,
              // Optional fields
              collectionDateTime: sample.data?.collectionDateTime,
              storageConditionOnArrival: sample.data?.storageConditionOnArrival,
              transportTemperature: sample.data?.transportTemperature,
              receivingPersonnelName: sample.data?.receivingPersonnelName,
              manifestVerificationStatus:
                sample.data?.manifestVerificationStatus,
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

  const handleImportSuccess = useCallback(() => {
    setImportModalOpen(false);
    loadPageSamples();
    if (onProgressUpdate) {
      onProgressUpdate();
    }
  }, [loadPageSamples, onProgressUpdate]);

  const hasRealPageId =
    pageData?.id && !String(pageData.id).startsWith("default-");

  const markAsVerified = useCallback(() => {
    if (selectedSampleIds.length === 0) {
      notify({
        kind: NotificationKinds.warning,
        title: intl.formatMessage({
          id: "notification.title",
          defaultMessage: "Notification",
        }),
        subtitle: intl.formatMessage({
          id: "notebook.page.virology.error.noSelection",
          defaultMessage: "Please select at least one sample.",
        }),
      });
      return;
    }

    if (!hasRealPageId) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notification.error",
          defaultMessage: "Error",
        }),
        subtitle: intl.formatMessage({
          id: "notebook.page.virology.error.noPage",
          defaultMessage:
            "Cannot update samples: Page not properly initialized.",
        }),
      });
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
          notify({
            kind: NotificationKinds.success,
            title: intl.formatMessage({
              id: "notification.title",
              defaultMessage: "Success",
            }),
            subtitle: intl.formatMessage(
              {
                id: "notebook.page.virology.success.verified",
                defaultMessage:
                  "Marked {count} sample(s) as Verified. They can now proceed to Media Preparation.",
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
              id: "notebook.page.virology.error.status",
              defaultMessage: "Failed to verify samples. Please try again.",
            }),
          });
        }
      },
    );
  }, [
    selectedSampleIds,
    hasRealPageId,
    intl,
    loadPageSamples,
    onProgressUpdate,
    pageData?.id,
    notify,
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

  // Custom columns for virology reception metadata
  // Note: sampleType is already included in SampleGrid's default columns
  const getAdditionalColumns = (intl) => [
    {
      key: "source",
      header: intl.formatMessage({
        id: "notebook.sample.source",
        defaultMessage: "Source",
      }),
      render: (value, sample) => {
        const source = value || sample?.source;
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
        const testType = value || sample?.testType;
        if (!testType) return "-";

        // Handle testType as string or object - ensure it's always a string
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
      key: "projectStudyAssociation",
      header: intl.formatMessage({
        id: "notebook.sample.project",
        defaultMessage: "Project/Study",
      }),
      render: (value, sample) => {
        const project = value || sample?.projectStudyAssociation;
        if (!project) return "-";
        if (typeof project === "string") return project;
        if (typeof project === "object") {
          return project.value || project.name || project.description || "-";
        }
        return String(project);
      },
    },
    {
      key: "batchId",
      header: intl.formatMessage({
        id: "notebook.sample.batchId",
        defaultMessage: "Batch ID",
      }),
      render: (value, sample) => {
        const batchId = value || sample?.batchId;
        if (!batchId) return "-";
        if (typeof batchId === "string") return batchId;
        if (typeof batchId === "number") return String(batchId);
        if (typeof batchId === "object") {
          return batchId.value || batchId.name || batchId.id || "-";
        }
        return String(batchId);
      },
    },
    {
      key: "manifestVerificationStatus",
      header: intl.formatMessage({
        id: "notebook.sample.verificationStatus",
        defaultMessage: "Verification",
      }),
      render: (value, sample) => {
        const status = value || sample?.manifestVerificationStatus;
        if (!status) return "-";

        // Ensure status is a string
        let statusStr;
        if (typeof status === "string") {
          statusStr = status;
        } else if (typeof status === "object") {
          statusStr = status.value || status.name || status.description || "-";
        } else {
          statusStr = String(status);
        }

        const tagType =
          statusStr === "Verified"
            ? "green"
            : statusStr === "Pending"
              ? "gray"
              : "red";
        return (
          <Tag type={tagType} size="sm">
            {statusStr}
          </Tag>
        );
      },
    },
  ];

  return (
    <div className="virology-sample-reception-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.page.virology.sampleReception.title"
            defaultMessage="Sample Reception &amp; Registration"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.virology.sampleReception.description"
            defaultMessage="Import virology and vaccine samples from delivery manifest with complete reception metadata. Select samples and mark as Verified to proceed to the next workflow stage."
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
                  id="notebook.page.virology.totalSamples"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{samples.length}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.virology.pendingInProgress"
                  defaultMessage="Pending / In Progress"
                />
              </span>
              <span className="progress-value">{pendingCount}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.virology.verified"
                  defaultMessage="Verified"
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
          renderIcon={Upload}
          onClick={() => setImportModalOpen(true)}
        >
          <FormattedMessage
            id="notebook.page.virology.importManifest"
            defaultMessage="Import from Manifest"
          />
        </Button>

        {selectedSampleIds.length > 0 && (
          <>
            <Button
              kind="secondary"
              size="sm"
              renderIcon={Checkmark}
              onClick={markAsVerified}
            >
              <FormattedMessage
                id="notebook.page.virology.markAsVerified"
                defaultMessage="Mark as Verified ({count})"
                values={{ count: selectedSampleIds.length }}
              />
            </Button>
          </>
        )}
      </div>

      {/* Pending / In Progress Samples Table */}
      <div className="sample-table-section">
        <div className="table-section-header">
          <h5>
            <FormattedMessage
              id="notebook.page.virology.pendingSamples.title"
              defaultMessage="Pending / In Progress"
            />
            <Tag type="gray" size="sm" className="count-tag">
              {pendingCount}
            </Tag>
          </h5>
          <p className="table-section-description">
            <FormattedMessage
              id="notebook.page.virology.pendingSamples.description"
              defaultMessage="Samples awaiting reception verification or currently in progress. Select samples and mark as Verified to move them to the completed section."
            />
          </p>
        </div>
        <div className="sample-grid-container">
          {!loading && pendingSamples.length === 0 ? (
            <div className="empty-table-state">
              <p>
                <FormattedMessage
                  id="notebook.page.virology.pendingSamples.empty"
                  defaultMessage="No pending samples. Import a delivery manifest to add samples."
                />
              </p>
            </div>
          ) : (
            <SampleGrid
              gridId="pending-samples"
              samples={pendingSamples}
              selectedIds={selectedSampleIds}
              onSelectionChange={setSelectedSampleIds}
              showSelection={true}
              loading={loading}
              additionalColumns={getAdditionalColumns(intl)}
            />
          )}
        </div>
      </div>

      {/* Completed Samples Table */}
      <div className="sample-table-section">
        <div className="table-section-header">
          <h5>
            <FormattedMessage
              id="notebook.page.virology.completedSamples.title"
              defaultMessage="Completed"
            />
            <Tag type="green" size="sm" className="count-tag">
              {completedCount}
            </Tag>
          </h5>
          <p className="table-section-description">
            <FormattedMessage
              id="notebook.page.virology.completedSamples.description"
              defaultMessage="Samples that have been verified and are ready for the next workflow step."
            />
          </p>
        </div>
        <div className="sample-grid-container">
          {!loading && completedSamples.length === 0 ? (
            <div className="empty-table-state">
              <p>
                <FormattedMessage
                  id="notebook.page.virology.completedSamples.empty"
                  defaultMessage="No verified samples yet. Select pending samples and mark them as verified."
                />
              </p>
            </div>
          ) : (
            <SampleGrid
              gridId="completed-samples"
              samples={completedSamples}
              selectedIds={[]}
              showSelection={false}
              loading={loading}
              additionalColumns={getAdditionalColumns(intl)}
            />
          )}
        </div>
      </div>

      {/* Global Empty state - only show when no samples at all */}
      {!loading && samples.length === 0 && (
        <div className="empty-state global-empty">
          <p>
            <FormattedMessage
              id="notebook.page.virology.empty"
              defaultMessage="No samples have been added yet. Import a delivery manifest to add samples with reception metadata."
            />
          </p>
        </div>
      )}

      {/* Manifest Import Modal */}
      <VirologyManifestImportModal
        open={importModalOpen}
        onClose={() => setImportModalOpen(false)}
        entryId={entryId}
        onImportSuccess={handleImportSuccess}
      />
    </div>
  );
}

export default VirologySampleReceptionPage;
