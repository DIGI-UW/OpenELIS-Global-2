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
import PharmaManifestImportModal from "../../workflow/PharmaManifestImportModal";
import "../../workflow/NotebookWorkflow.css";

/**
 * PharmaceuticalSampleCreationPage - Page 1 of the Pharmaceuticals workflow.
 * Captures full metadata at sample creation and sets status to "Created - Pending QC".
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - The notebook page data
 * @param {Object} props.progress - Page progress
 * @param {function} props.onProgressUpdate - Callback when progress changes
 */
function PharmaceuticalSampleCreationPage({
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
              sampleCategory: sample.data?.sampleCategory,
              sampleMaterial: sample.data?.sampleMaterial,
              chemicalName: sample.data?.chemicalName,
              grade: sample.data?.grade,
              lotNumber: sample.data?.lotNumber,
              storageCondition: sample.data?.storageCondition,
              owner: sample.data?.owner,
              patientId: sample.data?.patientId,
              consentStatus: sample.data?.consentStatus,
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

  const markAsRegistered = useCallback(() => {
    if (selectedSampleIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.page.pharma.error.noSelection",
          defaultMessage: "Please select at least one sample.",
        }),
      );
      return;
    }

    if (!hasRealPageId) {
      setError(
        intl.formatMessage({
          id: "notebook.page.pharma.error.noPage",
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
                id: "notebook.page.pharma.success.registered",
                defaultMessage:
                  "Marked {count} sample(s) as Registered. They will appear on the QC page.",
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
              id: "notebook.page.pharma.error.status",
              defaultMessage: "Failed to register samples. Please try again.",
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

  return (
    <div className="pharma-sample-creation-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.page.pharma.sampleCreation.title"
            defaultMessage="Sample Creation &amp; Full Metadata Capture"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.pharma.sampleCreation.description"
            defaultMessage="Import pharmaceutical samples from manifest with full traceability metadata. Select samples and mark as Registered to proceed to QC."
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
                  id="notebook.page.pharma.totalSamples"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{samples.length}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.pharma.pendingRegistration"
                  defaultMessage="Pending Registration"
                />
              </span>
              <span className="progress-value">{pendingCount}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.pharma.registered"
                  defaultMessage="Registered"
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
            id="notebook.page.pharma.importManifest"
            defaultMessage="Import from Manifest"
          />
        </Button>

        {selectedSampleIds.length > 0 && (
          <Button
            kind="secondary"
            size="sm"
            renderIcon={Checkmark}
            onClick={markAsRegistered}
          >
            <FormattedMessage
              id="notebook.page.pharma.markAsRegistered"
              defaultMessage="Mark as Registered ({count})"
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

      {/* Pending Samples Table */}
      <div className="sample-table-section">
        <div className="table-section-header">
          <h5>
            <FormattedMessage
              id="notebook.page.pharma.pendingSamples.title"
              defaultMessage="Pending Registration"
            />
            <Tag type="gray" size="sm" className="count-tag">
              {pendingCount}
            </Tag>
          </h5>
          <p className="table-section-description">
            <FormattedMessage
              id="notebook.page.pharma.pendingSamples.description"
              defaultMessage="Samples awaiting registration. Select samples and mark as Registered to move them to the completed section."
            />
          </p>
        </div>
        <div className="sample-grid-container">
          {!loading && pendingSamples.length === 0 ? (
            <div className="empty-table-state">
              <p>
                <FormattedMessage
                  id="notebook.page.pharma.pendingSamples.empty"
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
                  key: "sampleType",
                  header: intl.formatMessage({
                    id: "notebook.sample.sampleType",
                    defaultMessage: "Sample Type",
                  }),
                  render: (sample) => sample.sampleType || "-",
                },
                {
                  key: "lotNumber",
                  header: intl.formatMessage({
                    id: "notebook.sample.lotNumber",
                    defaultMessage: "Lot / Batch",
                  }),
                  render: (sample) => sample.lotNumber || "-",
                },
                {
                  key: "storageCondition",
                  header: intl.formatMessage({
                    id: "notebook.sample.storage",
                    defaultMessage: "Storage",
                  }),
                  render: (sample) => sample.storageCondition || "-",
                },
              ]}
            />
          )}
        </div>
      </div>

      {/* Completed / Registered Samples Table */}
      <div className="sample-table-section">
        <div className="table-section-header">
          <h5>
            <FormattedMessage
              id="notebook.page.pharma.completedSamples.title"
              defaultMessage="Registered Samples"
            />
            <Tag type="green" size="sm" className="count-tag">
              {completedCount}
            </Tag>
          </h5>
          <p className="table-section-description">
            <FormattedMessage
              id="notebook.page.pharma.completedSamples.description"
              defaultMessage="Samples that have been registered and are ready for the next workflow step (QC)."
            />
          </p>
        </div>
        <div className="sample-grid-container">
          {!loading && completedSamples.length === 0 ? (
            <div className="empty-table-state">
              <p>
                <FormattedMessage
                  id="notebook.page.pharma.completedSamples.empty"
                  defaultMessage="No registered samples yet. Select pending samples and mark them as registered."
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
                  key: "sampleType",
                  header: intl.formatMessage({
                    id: "notebook.sample.sampleType",
                    defaultMessage: "Sample Type",
                  }),
                  render: (sample) => sample.sampleType || "-",
                },
                {
                  key: "lotNumber",
                  header: intl.formatMessage({
                    id: "notebook.sample.lotNumber",
                    defaultMessage: "Lot / Batch",
                  }),
                  render: (sample) => sample.lotNumber || "-",
                },
                {
                  key: "storageCondition",
                  header: intl.formatMessage({
                    id: "notebook.sample.storage",
                    defaultMessage: "Storage",
                  }),
                  render: (sample) => sample.storageCondition || "-",
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
              id="notebook.page.pharma.empty"
              defaultMessage="No samples have been added yet. Import a manifest or link existing samples, then apply metadata."
            />
          </p>
        </div>
      )}

      {/* Manifest Import Modal */}
      <PharmaManifestImportModal
        open={importModalOpen}
        onClose={() => setImportModalOpen(false)}
        entryId={entryId}
        onImportSuccess={handleImportSuccess}
      />
    </div>
  );
}

export default PharmaceuticalSampleCreationPage;
