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
import BacteriologyManifestImportModal from "../../workflow/BacteriologyManifestImportModal";
import "../../workflow/NotebookWorkflow.css";

/**
 * BacteriologySampleReceptionPage - Page 1 of the Bacteriology workflow.
 * Handles sample reception with CSV manifest import. Bacteriology-specific data points include:
 * - Project Name, Study ID
 * - Participant ID, Barcode
 * - Collection Site, Sample Type, Collection Date & Time
 * - Sample Received Date, Sample Arrival Time, Received By
 * - Storage Container Type, Storage Temperature on Arrival
 * - Consent Status, CRF Status
 * - Sample Origin (Human/Animal/Environmental/Food), Source Location/Facility
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - The notebook page data
 * @param {Object} props.progress - Page progress
 * @param {function} props.onProgressUpdate - Callback when progress changes
 */
function BacteriologySampleReceptionPage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
}) {
  const intl = useIntl();
  const componentMounted = useRef(false);

  // State for samples
  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Modal state for import
  const [importModalOpen, setImportModalOpen] = useState(false);

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
              // Bacteriology specific fields from data
              projectName: sample.data?.projectName,
              studyId: sample.data?.studyId,
              participantId: sample.data?.participantId,
              barcode: sample.data?.barcode,
              collectionSite: sample.data?.collectionSite,
              sampleOrigin: sample.data?.sampleOrigin,
              sourceLocationFacility: sample.data?.sourceLocationFacility,
              receivedBy: sample.data?.receivedBy,
              storageTemperature: sample.data?.storageTemperatureOnArrival,
              consentStatus: sample.data?.consentStatus,
              crfStatus: sample.data?.crfStatus,
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

  // Handle import success
  const handleImportSuccess = useCallback(
    (result) => {
      loadPageSamples();
      if (onProgressUpdate) {
        onProgressUpdate();
      }
    },
    [loadPageSamples, onProgressUpdate],
  );

  // Handle bulk mark as verified
  const handleBulkMarkVerified = useCallback(() => {
    if (selectedSampleIds.length === 0) return;

    const hasRealPageId =
      pageData?.id && !String(pageData.id).startsWith("default-");
    if (!hasRealPageId) {
      setError("Cannot update samples: Page not properly initialized.");
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
          loadPageSamples();
          setSelectedSampleIds([]);
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError("Failed to update sample status.");
        }
      },
    );
  }, [selectedSampleIds, pageData?.id, loadPageSamples, onProgressUpdate]);

  // Split samples into pending and completed
  const pendingSamples = useMemo(
    () => samples.filter((s) => s.status === "PENDING"),
    [samples],
  );
  const completedSamples = useMemo(
    () => samples.filter((s) => s.status === "COMPLETED"),
    [samples],
  );

  const pendingCount = pendingSamples.length;
  const completedCount = completedSamples.length;

  // Get origin badge type
  const getOriginBadgeType = (origin) => {
    switch (origin?.toLowerCase()) {
      case "human":
        return "blue";
      case "animal":
        return "purple";
      case "environmental":
        return "green";
      case "food/beverage":
        return "orange";
      default:
        return "gray";
    }
  };

  return (
    <div className="bacteriology-sample-reception-page pharma-sample-creation-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.page.bacteriology.sampleReception.title"
            defaultMessage="Sample Reception"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.bacteriology.sampleReception.description"
            defaultMessage="Import sample manifest and verify sample receipt. Check that all samples match the manifest. Inspect for damage, proper labeling, and temperature compliance. Capture all reception metadata including project information, participant details, collection data, storage conditions, and consent status."
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
                  id="notebook.page.bacteriology.totalSamples"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{samples.length}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.bacteriology.verified"
                  defaultMessage="Verified"
                />
              </span>
              <span className="progress-value">{completedCount}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.bacteriology.pendingVerification"
                  defaultMessage="Pending Verification"
                />
              </span>
              <span className="progress-value">{pendingCount}</span>
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
            id="notebook.page.bacteriology.importManifest"
            defaultMessage="Import from Manifest"
          />
        </Button>

        {selectedSampleIds.length > 0 && (
          <Button
            kind="secondary"
            size="sm"
            renderIcon={Checkmark}
            onClick={handleBulkMarkVerified}
          >
            <FormattedMessage
              id="notebook.page.bacteriology.markVerified"
              defaultMessage="Mark as Verified ({count})"
              values={{ count: selectedSampleIds.length }}
            />
          </Button>
        )}
      </div>

      {/* Error Display */}
      {error && (
        <InlineNotification
          kind="error"
          title={error}
          hideCloseButton
          lowContrast
        />
      )}

      {/* Pending Samples Table */}
      <div className="sample-table-section">
        <div className="table-section-header">
          <h5>
            <FormattedMessage
              id="notebook.page.bacteriology.pendingSamples.title"
              defaultMessage="Pending Verification"
            />
            <Tag type="gray" size="sm" className="count-tag">
              {pendingCount}
            </Tag>
          </h5>
          <p className="table-section-description">
            <FormattedMessage
              id="notebook.page.bacteriology.pendingSamples.description"
              defaultMessage="Samples awaiting verification. Select samples and mark as Verified to move them to the completed section."
            />
          </p>
        </div>
        <div className="sample-grid-container">
          {!loading && pendingSamples.length === 0 ? (
            <div className="empty-table-state">
              <p>
                <FormattedMessage
                  id="notebook.page.bacteriology.pendingSamples.empty"
                  defaultMessage="No pending samples. Import a manifest to add samples."
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
              additionalColumns={[
                {
                  key: "sampleOrigin",
                  header: intl.formatMessage({
                    id: "notebook.sample.sampleOrigin",
                    defaultMessage: "Origin",
                  }),
                  render: (value, sample) => {
                    const origin = sample?.sampleOrigin || value;
                    return origin ? (
                      <Tag type={getOriginBadgeType(origin)} size="sm">
                        {origin}
                      </Tag>
                    ) : (
                      "-"
                    );
                  },
                },
                {
                  key: "projectName",
                  header: intl.formatMessage({
                    id: "notebook.sample.projectName",
                    defaultMessage: "Project",
                  }),
                  render: (value, sample) =>
                    sample?.projectName || value || "-",
                },
                {
                  key: "collectionSite",
                  header: intl.formatMessage({
                    id: "notebook.sample.collectionSite",
                    defaultMessage: "Collection Site",
                  }),
                  render: (value, sample) =>
                    sample?.collectionSite || value || "-",
                },
                {
                  key: "receivedBy",
                  header: intl.formatMessage({
                    id: "notebook.sample.receivedBy",
                    defaultMessage: "Received By",
                  }),
                  render: (value, sample) => sample?.receivedBy || value || "-",
                },
              ]}
            />
          )}
        </div>
      </div>

      {/* Completed / Verified Samples Table */}
      <div className="sample-table-section">
        <div className="table-section-header">
          <h5>
            <FormattedMessage
              id="notebook.page.bacteriology.completedSamples.title"
              defaultMessage="Verified Samples"
            />
            <Tag type="green" size="sm" className="count-tag">
              {completedCount}
            </Tag>
          </h5>
          <p className="table-section-description">
            <FormattedMessage
              id="notebook.page.bacteriology.completedSamples.description"
              defaultMessage="Samples that have been verified and are ready for the next workflow step (Sample Quality Assessment)."
            />
          </p>
        </div>
        <div className="sample-grid-container">
          {!loading && completedSamples.length === 0 ? (
            <div className="empty-table-state">
              <p>
                <FormattedMessage
                  id="notebook.page.bacteriology.completedSamples.empty"
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
              additionalColumns={[
                {
                  key: "sampleOrigin",
                  header: intl.formatMessage({
                    id: "notebook.sample.sampleOrigin",
                    defaultMessage: "Origin",
                  }),
                  render: (value, sample) => {
                    const origin = sample?.sampleOrigin || value;
                    return origin ? (
                      <Tag type={getOriginBadgeType(origin)} size="sm">
                        {origin}
                      </Tag>
                    ) : (
                      "-"
                    );
                  },
                },
                {
                  key: "projectName",
                  header: intl.formatMessage({
                    id: "notebook.sample.projectName",
                    defaultMessage: "Project",
                  }),
                  render: (value, sample) =>
                    sample?.projectName || value || "-",
                },
                {
                  key: "collectionSite",
                  header: intl.formatMessage({
                    id: "notebook.sample.collectionSite",
                    defaultMessage: "Collection Site",
                  }),
                  render: (value, sample) =>
                    sample?.collectionSite || value || "-",
                },
                {
                  key: "receivedBy",
                  header: intl.formatMessage({
                    id: "notebook.sample.receivedBy",
                    defaultMessage: "Received By",
                  }),
                  render: (value, sample) => sample?.receivedBy || value || "-",
                },
              ]}
            />
          )}
        </div>
      </div>

      {/* Global Empty state - only show when no samples at all */}
      {!loading && samples.length === 0 && (
        <div className="empty-state global-empty">
          <p>
            <FormattedMessage
              id="notebook.page.bacteriology.sampleReception.empty"
              defaultMessage="No samples have been added yet. Use 'Import from Manifest' to create samples from a CSV file."
            />
          </p>
        </div>
      )}

      {/* Import Modal */}
      <BacteriologyManifestImportModal
        open={importModalOpen}
        onClose={() => setImportModalOpen(false)}
        entryId={entryId}
        onImportSuccess={handleImportSuccess}
      />
    </div>
  );
}

export default BacteriologySampleReceptionPage;
