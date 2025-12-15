import React, { useState, useEffect, useRef, useCallback } from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  InlineNotification,
  Loading,
  Modal,
  FileUploaderDropContainer,
  FileUploaderItem,
  Select,
  SelectItem,
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Tag,
  TextInput,
  DatePicker,
  DatePickerInput,
  TimePicker,
  Checkbox,
  TextArea,
} from "@carbon/react";
import { Upload, Checkmark, Warning, Add, Printer } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
  postToOpenElisServerFormDataJson,
} from "../../../utils/Utils";
import SampleGrid from "../../workflow/SampleGrid";
import TBManifestImportModal from "../../workflow/TBManifestImportModal";
import config from "../../../../config.json";
import "../../workflow/NotebookWorkflow.css";

/**
 * TBSampleCreationPage - Page 1 of the TB workflow.
 * Handles comprehensive sample creation with full metadata capture including:
 * A. Sample Identity (System Controlled)
 * B. Specimen Information
 * C. Request Paper Details
 * D. Patient / Participant Metadata
 * E. Clinical Context
 * F. Requested Tests
 * G. Receipt Details
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - The notebook page data
 * @param {Object} props.progress - Page progress
 * @param {function} props.onProgressUpdate - Callback when progress changes
 */
function TBSampleCreationPage({
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
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // State for sample types from type_of_sample table
  const [sampleTypes, setSampleTypes] = useState([]);

  // Modal state for import
  const [importModalOpen, setImportModalOpen] = useState(false);

  // Load sample types from type_of_sample table
  useEffect(() => {
    getFromOpenElisServer("/rest/user-sample-types", (res) => {
      setSampleTypes(res || []);
    });
  }, []);

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
              patientName: sample.patientName,
              volume: sample.volume,
              // TB specific fields from questionnaire responses
              specimenType: sample.data?.specimenType,
              documentNumber: sample.data?.documentNumber,
              referringFacility: sample.data?.referringFacility,
              patientAge: sample.data?.patientAge,
              patientSex: sample.data?.patientSex,
              patientId: sample.data?.patientId,
              studyId: sample.data?.studyId,
              requestedTests: sample.data?.requestedTests,
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

  // Handle bulk mark as completed
  const handleBulkMarkCompleted = useCallback(() => {
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

  // Calculate stats
  const completedCount = samples.filter(
    (s) => s.status === "COMPLETED",
  ).length;
  const pendingCount = samples.filter((s) => s.status === "PENDING").length;

  // Handle import success
  const handleImportSuccess = useCallback(() => {
    loadPageSamples();
    if (onProgressUpdate) {
      onProgressUpdate();
    }
  }, [loadPageSamples, onProgressUpdate]);

  return (
    <div className="tb-sample-creation-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.page.tb.sampleCreation.title"
            defaultMessage="Sample Creation & Full Metadata Capture"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.tb.sampleCreation.description"
            defaultMessage="Capture complete sample metadata including accession number, specimen information, patient details, clinical context, requested tests, and receipt details. Import samples via CSV manifest or create individually."
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
                  id="notebook.page.tb.totalSamples"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{samples.length}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.tb.completed"
                  defaultMessage="Completed"
                />
              </span>
              <span className="progress-value">{completedCount}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.tb.pending"
                  defaultMessage="Pending"
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
            id="notebook.page.tb.importManifest"
            defaultMessage="Import from Manifest"
          />
        </Button>

        {selectedSampleIds.length > 0 && (
          <Button
            kind="secondary"
            size="sm"
            renderIcon={Checkmark}
            onClick={handleBulkMarkCompleted}
          >
            <FormattedMessage
              id="notebook.page.tb.markCompleted"
              defaultMessage="Mark as Completed ({count})"
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

      {/* Sample Grid */}
      <div className="sample-grid-container">
        <SampleGrid
          samples={samples}
          selectedIds={selectedSampleIds}
          onSelectionChange={setSelectedSampleIds}
          statusFilter={statusFilter}
          onStatusFilterChange={setStatusFilter}
          showSelection={true}
          loading={loading}
          columns={[
            { key: "accessionNumber", header: "Accession Number" },
            { key: "externalId", header: "Sample ID" },
            { key: "specimenType", header: "Specimen Type" },
            { key: "patientName", header: "Patient Name" },
            { key: "patientId", header: "Patient ID" },
            { key: "referringFacility", header: "Referring Facility" },
            { key: "requestedTests", header: "Requested Tests" },
            { key: "status", header: "Status" },
          ]}
        />
      </div>

      {/* Empty state */}
      {!loading && samples.length === 0 && (
        <div className="empty-state">
          <p>
            <FormattedMessage
              id="notebook.page.tb.sampleCreation.empty"
              defaultMessage="No samples have been added yet. Use 'Import from Manifest' to create samples from a CSV file with complete TB metadata."
            />
          </p>
        </div>
      )}

      {/* Import Modal */}
      <TBManifestImportModal
        open={importModalOpen}
        onClose={() => setImportModalOpen(false)}
        entryId={entryId}
        onImportSuccess={handleImportSuccess}
      />
    </div>
  );
}

export default TBSampleCreationPage;

