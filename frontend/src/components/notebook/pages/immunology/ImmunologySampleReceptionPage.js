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
  Tag,
} from "@carbon/react";
import { Upload, Checkmark } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
} from "../../../utils/Utils";
import SampleGrid from "../../workflow/SampleGrid";
import ImmunologyManifestImportModal from "../../workflow/ImmunologyManifestImportModal";
import "../../workflow/NotebookWorkflow.css";

/**
 * ImmunologySampleReceptionPage - Page 1 of the Immunology workflow.
 * Captures full reception metadata and sets status to "Received - Pending Verification".
 *
 * Reception Metadata:
 * - Unique Parent Sample Identifier (assigned at reception)
 * - Project Name/ID
 * - Delivery Manifest Reference
 * - Sample Type (whole blood, PBMC, serum, plasma, etc.)
 * - Sample Volume/Quantity
 * - Collection Date & Time
 * - Reception Date & Time
 * - Source/Origin (facility, study site)
 * - Storage Condition on Arrival
 * - Transport Temperature
 * - Receiving Personnel Name
 * - Delivery Manifest Verification Status
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - The notebook page data
 * @param {Object} props.progress - Page progress
 * @param {function} props.onProgressUpdate - Callback when progress changes
 */
function ImmunologySampleReceptionPage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
}) {
  const intl = useIntl();
  const componentMounted = useRef(false);

  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);

  const [importModalOpen, setImportModalOpen] = useState(false);

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
    setSuccessMessage(null);

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
              // Immunology-specific reception metadata from JSONB data
              uniqueParentSampleId: sample.data?.uniqueParentSampleId,
              projectNameId: sample.data?.projectNameId,
              deliveryManifestReference: sample.data?.deliveryManifestReference,
              sampleVolume: sample.data?.sampleVolume,
              collectionDateTime: sample.data?.collectionDateTime,
              receptionDateTime: sample.data?.receptionDateTime,
              sourceOrigin: sample.data?.sourceOrigin,
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
      setError(
        intl.formatMessage({
          id: "notebook.page.immunology.error.noSelection",
          defaultMessage: "Please select at least one sample.",
        }),
      );
      return;
    }

    if (!hasRealPageId) {
      setError(
        intl.formatMessage({
          id: "notebook.page.immunology.error.noPage",
          defaultMessage:
            "Cannot update samples: Page not properly initialized.",
        }),
      );
      return;
    }

    setError(null);
    setSuccessMessage(null);

    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
      JSON.stringify({
        sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
        status: "COMPLETED",
      }),
      (status) => {
        if (status === 200) {
          setSuccessMessage(
            intl.formatMessage(
              {
                id: "notebook.page.immunology.success.verified",
                defaultMessage:
                  "Marked {count} sample(s) as Verified. They will proceed to Processing.",
              },
              { count: selectedSampleIds.length },
            ),
          );
          setSelectedSampleIds([]);
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(
            intl.formatMessage({
              id: "notebook.page.immunology.error.status",
              defaultMessage: "Failed to verify samples. Please try again.",
            }),
          );
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

  // Custom columns for immunology reception metadata
  // Note: Removed "sampleType" column as it's already included in SampleGrid's default headers
  const getAdditionalColumns = (intl) => [
    {
      key: "projectNameId",
      header: intl.formatMessage({
        id: "notebook.sample.projectNameId",
        defaultMessage: "Project",
      }),
      render: (value, sample) => value || sample?.projectNameId || "-",
    },
    {
      key: "sourceOrigin",
      header: intl.formatMessage({
        id: "notebook.sample.sourceOrigin",
        defaultMessage: "Source/Origin",
      }),
      render: (value, sample) => value || sample?.sourceOrigin || "-",
    },
    {
      key: "storageConditionOnArrival",
      header: intl.formatMessage({
        id: "notebook.sample.storageCondition",
        defaultMessage: "Storage",
      }),
      render: (value, sample) =>
        value || sample?.storageConditionOnArrival || "-",
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
        const tagType =
          status === "Verified"
            ? "green"
            : status === "Pending"
              ? "gray"
              : "red";
        return (
          <Tag type={tagType} size="sm">
            {status}
          </Tag>
        );
      },
    },
  ];

  return (
    <div className="immunology-sample-reception-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.page.immunology.sampleReception.title"
            defaultMessage="Sample Reception &amp; Full Metadata Capture"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.immunology.sampleReception.description"
            defaultMessage="Import immunology samples from delivery manifest with complete reception metadata. Select samples and mark as Verified to proceed to Processing."
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
                  id="notebook.page.immunology.totalSamples"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{samples.length}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.immunology.pendingInProgress"
                  defaultMessage="Pending / In Progress"
                />
              </span>
              <span className="progress-value">{pendingCount}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.immunology.verified"
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
            id="notebook.page.immunology.importManifest"
            defaultMessage="Import from Manifest"
          />
        </Button>

        {selectedSampleIds.length > 0 && (
          <Button
            kind="secondary"
            size="sm"
            renderIcon={Checkmark}
            onClick={markAsVerified}
          >
            <FormattedMessage
              id="notebook.page.immunology.markAsVerified"
              defaultMessage="Mark as Verified ({count})"
              values={{ count: selectedSampleIds.length }}
            />
          </Button>
        )}
      </div>

      {/* Errors / Success */}
      {error && (
        <InlineNotification
          kind="error"
          title={error}
          hideCloseButton
          lowContrast
        />
      )}
      {successMessage && (
        <InlineNotification
          kind="success"
          title={successMessage}
          hideCloseButton
          lowContrast
        />
      )}

      {/* Pending / In Progress Samples Table */}
      <div className="sample-table-section">
        <div className="table-section-header">
          <h5>
            <FormattedMessage
              id="notebook.page.immunology.pendingSamples.title"
              defaultMessage="Pending / In Progress"
            />
            <Tag type="gray" size="sm" className="count-tag">
              {pendingCount}
            </Tag>
          </h5>
          <p className="table-section-description">
            <FormattedMessage
              id="notebook.page.immunology.pendingSamples.description"
              defaultMessage="Samples awaiting reception verification or currently in progress. Select samples and mark as Verified to move them to the completed section."
            />
          </p>
        </div>
        <div className="sample-grid-container">
          {!loading && pendingSamples.length === 0 ? (
            <div className="empty-table-state">
              <p>
                <FormattedMessage
                  id="notebook.page.immunology.pendingSamples.empty"
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
              id="notebook.page.immunology.completedSamples.title"
              defaultMessage="Completed"
            />
            <Tag type="green" size="sm" className="count-tag">
              {completedCount}
            </Tag>
          </h5>
          <p className="table-section-description">
            <FormattedMessage
              id="notebook.page.immunology.completedSamples.description"
              defaultMessage="Samples that have been verified and are ready for the next workflow step (Initial Processing)."
            />
          </p>
        </div>
        <div className="sample-grid-container">
          {!loading && completedSamples.length === 0 ? (
            <div className="empty-table-state">
              <p>
                <FormattedMessage
                  id="notebook.page.immunology.completedSamples.empty"
                  defaultMessage="No verified samples yet. Select pending samples and mark them as verified."
                />
              </p>
            </div>
          ) : (
            <SampleGrid
              gridId="completed-samples"
              samples={completedSamples}
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
              id="notebook.page.immunology.empty"
              defaultMessage="No samples have been added yet. Import a delivery manifest to add samples with reception metadata."
            />
          </p>
        </div>
      )}

      {/* Manifest Import Modal */}
      <ImmunologyManifestImportModal
        open={importModalOpen}
        onClose={() => setImportModalOpen(false)}
        entryId={entryId}
        onImportSuccess={handleImportSuccess}
      />
    </div>
  );
}

export default ImmunologySampleReceptionPage;
