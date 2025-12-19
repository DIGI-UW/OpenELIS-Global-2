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
import { Upload, Checkmark, Printer } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
} from "../../../utils/Utils";
import SampleGrid from "../../workflow/SampleGrid";
import MNTDManifestImportModal from "../../workflow/MNTDManifestImportModal";
import config from "../../../../config.json";
import "../../workflow/NotebookWorkflow.css";

/**
 * MNTDSampleIntakePage - Page 1 of the MNTD workflow.
 * Handles sample intake with CSV import. MNTD-specific data points include:
 * - Project Name
 * - Sample Type (validated against MNTD lab types)
 * - Sample ID/Tag (pre-labeled from field, external organizations)
 * - Number of Samples
 * - Sample Source Location
 * - Brought By (person/institution)
 * - Received Date & Time
 * - Receptionist Name
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - The notebook page data
 * @param {Object} props.progress - Page progress
 * @param {function} props.onProgressUpdate - Callback when progress changes
 */
function MNTDSampleIntakePage({
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
              // MNTD specific fields from questionnaire responses
              projectName: sample.data?.projectName,
              sampleSourceLocation: sample.data?.sampleSourceLocation,
              broughtBy: sample.data?.broughtBy,
              receptionistName: sample.data?.receptionistName,
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

  // Handle bulk mark as registered
  const handleBulkMarkRegistered = useCallback(() => {
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

  // Print barcode
  const handlePrintBarcode = (accessionNumber) => {
    const barcodesPdf =
      config.serverBaseUrl +
      `/LabelMakerServlet?labNo=${encodeURIComponent(accessionNumber)}&type=order&quantity=1`;
    window.open(barcodesPdf);
  };

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
    <div className="mntd-sample-intake-page pharma-sample-creation-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.page.mntd.sampleIntake.title"
            defaultMessage="Sample Intake / Sample Creation"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.mntd.sampleIntake.description"
            defaultMessage="Capture all incoming samples as they arrive at reception. Import samples via CSV manifest with MNTD-specific metadata. System generates unique IDs and barcodes."
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
                  id="notebook.page.mntd.totalSamples"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{samples.length}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.mntd.registered"
                  defaultMessage="Registered"
                />
              </span>
              <span className="progress-value">{completedCount}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.mntd.awaitingReception"
                  defaultMessage="Awaiting Reception"
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
            id="notebook.page.mntd.importManifest"
            defaultMessage="Import from Manifest"
          />
        </Button>

        {selectedSampleIds.length > 0 && (
          <Button
            kind="secondary"
            size="sm"
            renderIcon={Checkmark}
            onClick={handleBulkMarkRegistered}
          >
            <FormattedMessage
              id="notebook.page.mntd.markRegistered"
              defaultMessage="Mark as Registered ({count})"
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
              id="notebook.page.mntd.pendingSamples.title"
              defaultMessage="Pending Reception"
            />
            <Tag type="gray" size="sm" className="count-tag">
              {pendingCount}
            </Tag>
          </h5>
          <p className="table-section-description">
            <FormattedMessage
              id="notebook.page.mntd.pendingSamples.description"
              defaultMessage="Samples awaiting registration. Select samples and mark as Registered to move them to the completed section."
            />
          </p>
        </div>
        <div className="sample-grid-container">
          {!loading && pendingSamples.length === 0 ? (
            <div className="empty-table-state">
              <p>
                <FormattedMessage
                  id="notebook.page.mntd.pendingSamples.empty"
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
                  key: "projectName",
                  header: intl.formatMessage({
                    id: "notebook.sample.projectName",
                    defaultMessage: "Project",
                  }),
                  render: (sample) => sample.projectName || "-",
                },
                {
                  key: "sampleSourceLocation",
                  header: intl.formatMessage({
                    id: "notebook.sample.sourceLocation",
                    defaultMessage: "Source Location",
                  }),
                  render: (sample) => sample.sampleSourceLocation || "-",
                },
                {
                  key: "broughtBy",
                  header: intl.formatMessage({
                    id: "notebook.sample.broughtBy",
                    defaultMessage: "Brought By",
                  }),
                  render: (sample) => sample.broughtBy || "-",
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
              id="notebook.page.mntd.completedSamples.title"
              defaultMessage="Registered Samples"
            />
            <Tag type="green" size="sm" className="count-tag">
              {completedCount}
            </Tag>
          </h5>
          <p className="table-section-description">
            <FormattedMessage
              id="notebook.page.mntd.completedSamples.description"
              defaultMessage="Samples that have been registered and are ready for the next workflow step (Aliquoting)."
            />
          </p>
        </div>
        <div className="sample-grid-container">
          {!loading && completedSamples.length === 0 ? (
            <div className="empty-table-state">
              <p>
                <FormattedMessage
                  id="notebook.page.mntd.completedSamples.empty"
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
                  key: "projectName",
                  header: intl.formatMessage({
                    id: "notebook.sample.projectName",
                    defaultMessage: "Project",
                  }),
                  render: (sample) => sample.projectName || "-",
                },
                {
                  key: "sampleSourceLocation",
                  header: intl.formatMessage({
                    id: "notebook.sample.sourceLocation",
                    defaultMessage: "Source Location",
                  }),
                  render: (sample) => sample.sampleSourceLocation || "-",
                },
                {
                  key: "broughtBy",
                  header: intl.formatMessage({
                    id: "notebook.sample.broughtBy",
                    defaultMessage: "Brought By",
                  }),
                  render: (sample) => sample.broughtBy || "-",
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
              id="notebook.page.mntd.sampleIntake.empty"
              defaultMessage="No samples have been added yet. Use 'Import from Manifest' to create samples from a CSV file."
            />
          </p>
        </div>
      )}

      {/* Import Modal */}
      <MNTDManifestImportModal
        open={importModalOpen}
        onClose={() => setImportModalOpen(false)}
        entryId={entryId}
        onImportSuccess={handleImportSuccess}
      />
    </div>
  );
}

export default MNTDSampleIntakePage;
