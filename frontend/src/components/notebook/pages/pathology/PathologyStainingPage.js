import { useState, useEffect, useRef, useCallback } from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  Tag,
  InlineNotification,
  TextInput,
  TextArea,
  Select,
  SelectItem,
  Checkbox,
  Modal,
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableSelectAll,
  TableSelectRow,
  Loading,
  TableContainer,
  MultiSelect,
} from "@carbon/react";
import {
  Checkmark,
  Renew,
  Chemistry,
  CheckmarkFilled,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import "../../workflow/NotebookWorkflow.css";

/**
 * PathologyStainingPage - Slide Staining workflow step.
 * Purpose: Apply stains to prepared slides for microscopic examination.
 * Who uses it: Histology Technicians
 *
 * Key activities:
 * - Apply routine stains (H&E, Pap, etc.)
 * - Apply special stains (PAS, Trichrome, etc.)
 * - Run IHC/ICC if needed
 * - Document staining quality and controls
 */
function PathologyStainingPage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
  notebookId,
}) {
  const intl = useIntl();
  const componentMounted = useRef(false);

  // Sample list state
  const [samples, setSamples] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);

  // Selection state
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);

  // Staining Modal state
  const [stainingModalOpen, setStainingModalOpen] = useState(false);
  const [selectedSample, setSelectedSample] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [stainingLoading, setStainingLoading] = useState(false);
  const [stainingViewMode, setStainingViewMode] = useState(false);

  // Staining form data
  const [stainingData, setStainingData] = useState({
    // Routine Staining
    routineStainingCategory: "",
    routineStains: [],
    // Special Stains
    specialStains: [],
    specialStainIndication: "",
    // IHC/ICC
    ihcIccPerformed: false,
    ihcIccMarkers: "",
    ihcIccPrimaryAntibody: "",
    ihcIccClone: "",
    ihcIccDilution: "",
    ihcIccAntigenRetrieval: "",
    // Controls
    positiveControlRun: false,
    positiveControlResult: "",
    negativeControlRun: false,
    negativeControlResult: "",
    controlsAccepted: false,
    // Quality
    stainQualityAdequate: false,
    qualityNotes: "",
    // Batch info
    batchNumber: "",
    batchDate: "",
    // Staff
    technicianName: "",
    technicianInitials: "",
    stainingDate: "",
    notes: "",
    // Quality Control
    qcStatus: "PASS",
    qcStainIntensity: true,
    qcBackgroundClean: true,
    qcControlsValid: true,
    qcLabelingCorrect: true,
    qcIssues: "",
    qcCorrectiveAction: "",
    qcReviewedBy: "",
    qcReviewDate: "",
  });

  // Staining category options
  const stainingCategories = [
    { value: "histology", text: "Histology" },
    { value: "cytology", text: "Cytology" },
    { value: "hematology", text: "Hematology" },
    { value: "microbiology", text: "Microbiology" },
  ];

  // Routine stain options
  const routineStainOptions = {
    histology: [
      { id: "he", label: "H&E (Hematoxylin & Eosin)" },
      { id: "pap", label: "Pap Stain" },
      { id: "giemsa", label: "Giemsa" },
      { id: "wg", label: "Wright-Giemsa" },
    ],
    cytology: [
      { id: "pap", label: "Pap Stain" },
      { id: "dq", label: "Diff-Quik" },
      { id: "giemsa", label: "Giemsa" },
    ],
    hematology: [
      { id: "wg", label: "Wright-Giemsa" },
      { id: "wright", label: "Wright" },
      { id: "giemsa", label: "Giemsa" },
      { id: "pas", label: "PAS" },
    ],
    microbiology: [
      { id: "gram", label: "Gram Stain" },
      { id: "afb", label: "AFB (Ziehl-Neelsen)" },
      { id: "gms", label: "GMS (Fungal)" },
      { id: "pas", label: "PAS" },
    ],
  };

  // Special stain options
  const specialStainOptions = [
    { id: "pas", label: "PAS (Periodic Acid-Schiff)" },
    { id: "pasd", label: "PAS-D (with Diastase)" },
    { id: "masson", label: "Masson Trichrome" },
    { id: "reticulin", label: "Reticulin" },
    { id: "iron", label: "Iron (Prussian Blue)" },
    { id: "congo", label: "Congo Red" },
    { id: "mucicarmine", label: "Mucicarmine" },
    { id: "alcian", label: "Alcian Blue" },
    { id: "evg", label: "EVG (Elastic)" },
    { id: "afb", label: "AFB (Acid Fast)" },
    { id: "gms", label: "GMS (Grocott)" },
    { id: "warthin", label: "Warthin-Starry" },
    { id: "oil_red", label: "Oil Red O" },
    { id: "fontana", label: "Fontana-Masson" },
  ];

  // Load samples
  useEffect(() => {
    componentMounted.current = true;
    loadPageSamples();

    return () => {
      componentMounted.current = false;
    };
  }, [entryId, pageData?.id]);

  const loadPageSamples = useCallback(() => {
    if (!entryId || !pageData?.id) {
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);

    // Fetch samples that have completed the previous step (slides)
    // and merge with current staining page data
    getFromOpenElisServer(
      `/rest/notebook/pathology/workflow/samples-ready?entryId=${entryId}&currentStep=staining`,
      (workflowResponse) => {
        if (!componentMounted.current) return;

        // Also fetch current page samples to get staining data
        getFromOpenElisServer(
          `/rest/notebook/page/${pageData.id}/samples`,
          (pageResponse) => {
            if (!componentMounted.current) return;

            // Build a map of current page sample data by sampleItemId
            const pageSampleMap = {};
            if (pageResponse && Array.isArray(pageResponse)) {
              pageResponse.forEach((ps) => {
                const sampleId = String(ps.sampleItemId || ps.id);
                pageSampleMap[sampleId] = ps;
              });
            }

            if (workflowResponse && Array.isArray(workflowResponse)) {
              const transformedSamples = workflowResponse.map((sample) => {
                const sampleId = String(sample.id || sample.sampleItemId);
                // For expanded items (e.g., "123_slide_0"), try the full ID first,
                // then fall back to parent ID for backward compatibility
                const pageSample =
                  pageSampleMap[sampleId] ||
                  pageSampleMap[sample.parentSampleId] ||
                  pageSampleMap[sampleId.split("_")[0]];
                const stainingData = pageSample?.data || {};
                // Note: workflowData contains data from PREVIOUS step (slides)
                // We should NOT use it for staining-specific fields

                return {
                  id: sampleId,
                  externalId: sample.externalId,
                  accessionNumber: sample.accessionNumber,
                  sampleType:
                    sample.sampleType || sample.typeOfSample?.description,
                  specimenCategory: sample.specimenCategory || "histopathology",
                  collectionDate: sample.collectionDate,
                  // ONLY use status from current staining page, default to PENDING
                  // Backend returns status as "pageStatus" field
                  status:
                    pageSample?.pageStatus || pageSample?.status || "PENDING",
                  patientName: sample.patientName,
                  // Parent info from slide step (workflow expansion)
                  parentSampleId: sample.parentSampleId,
                  childIndex: sample.childIndex,
                  childLabel: sample.childLabel,
                  slideLabel: sample.slideLabel || sample.childLabel || "",
                  // Staining status from current page data ONLY
                  stainingComplete:
                    stainingData.stainingCompleted === true ||
                    stainingData.stainingComplete === true,
                  routineStains: stainingData.routineStains || [],
                  specialStains: stainingData.specialStains || [],
                  ihcPerformed: stainingData.ihcIccPerformed === true,
                  stainQuality: stainingData.stainQualityAdequate
                    ? "Adequate"
                    : "",
                  technicianName: stainingData.technicianName || "",
                  stainingDate: stainingData.stainingDate || "",
                  // QC status from current page ONLY - show nothing until staining complete
                  qcStatus: stainingData.qcStatus || "",
                };
              });
              setSamples(transformedSamples);
            } else {
              setSamples([]);
            }
            setLoading(false);
          },
        );
      },
    );
  }, [entryId, pageData?.id]);

  // Check if we have the required entry ID to load samples
  const hasValidEntry = !!entryId;

  const handleStainingInputChange = (e) => {
    const { name, value, type, checked } = e.target;
    setStainingData((prev) => ({
      ...prev,
      [name]: type === "checkbox" ? checked : value,
    }));
  };

  // Open staining modal
  const openStainingModal = (sample) => {
    setSelectedSample(sample);
    setStainingViewMode(false);
    setStainingLoading(true);

    // Reset form data with defaults
    setStainingData({
      routineStainingCategory: "",
      routineStains: [],
      specialStains: [],
      specialStainIndication: "",
      ihcIccPerformed: false,
      ihcIccMarkers: "",
      ihcIccPrimaryAntibody: "",
      ihcIccClone: "",
      ihcIccDilution: "",
      ihcIccAntigenRetrieval: "",
      positiveControlRun: false,
      positiveControlResult: "",
      negativeControlRun: false,
      negativeControlResult: "",
      controlsAccepted: false,
      stainQualityAdequate: false,
      qualityNotes: "",
      batchNumber: "",
      batchDate: "",
      technicianName: "",
      technicianInitials: "",
      stainingDate: new Date().toISOString().split("T")[0],
      notes: "",
      // QC fields
      qcStatus: "PASS",
      qcStainIntensity: true,
      qcBackgroundClean: true,
      qcControlsValid: true,
      qcLabelingCorrect: true,
      qcIssues: "",
      qcCorrectiveAction: "",
      qcReviewedBy: "",
      qcReviewDate: new Date().toISOString().split("T")[0],
    });

    setStainingModalOpen(true);

    // Fetch existing staining data if available
    if (pageData?.id && sample?.id) {
      getFromOpenElisServer(
        `/rest/notebook/pathology/staining/${sample.id}?pageId=${pageData.id}`,
        (response) => {
          setStainingLoading(false);
          if (response && response.success && response.hasData) {
            setStainingViewMode(true);
            setStainingData((prev) => ({
              ...prev,
              routineStainingCategory: response.routineStainingCategory || "",
              routineStains: response.routineStains || [],
              specialStains: response.specialStains || [],
              specialStainIndication: response.specialStainIndication || "",
              ihcIccPerformed: response.ihcIccPerformed === true,
              ihcIccMarkers: response.ihcIccMarkers || "",
              ihcIccPrimaryAntibody: response.ihcIccPrimaryAntibody || "",
              ihcIccClone: response.ihcIccClone || "",
              ihcIccDilution: response.ihcIccDilution || "",
              ihcIccAntigenRetrieval: response.ihcIccAntigenRetrieval || "",
              positiveControlRun: response.positiveControlRun === true,
              positiveControlResult: response.positiveControlResult || "",
              negativeControlRun: response.negativeControlRun === true,
              negativeControlResult: response.negativeControlResult || "",
              controlsAccepted: response.controlsAccepted === true,
              stainQualityAdequate: response.stainQualityAdequate === true,
              qualityNotes: response.qualityNotes || "",
              batchNumber: response.batchNumber || "",
              batchDate: response.batchDate || "",
              technicianName: response.technicianName || "",
              technicianInitials: response.technicianInitials || "",
              stainingDate: response.stainingDate || "",
              notes: response.notes || "",
              // QC fields
              qcStatus: response.qcStatus || "PASS",
              qcStainIntensity: response.qcStainIntensity !== false,
              qcBackgroundClean: response.qcBackgroundClean !== false,
              qcControlsValid: response.qcControlsValid !== false,
              qcLabelingCorrect: response.qcLabelingCorrect !== false,
              qcIssues: response.qcIssues || "",
              qcCorrectiveAction: response.qcCorrectiveAction || "",
              qcReviewedBy: response.qcReviewedBy || "",
              qcReviewDate: response.qcReviewDate || "",
            }));
          }
        },
      );
    } else {
      setStainingLoading(false);
    }
  };

  // Submit staining data
  const handleSubmitStaining = () => {
    if (submitting) return;

    setSubmitting(true);
    setError(null);

    // Use the full composite sample ID (e.g., "123_slide_0") to ensure
    // each slide's staining data is stored separately
    const sampleId = selectedSample.id;

    const payload = {
      sampleId: sampleId,
      pageId: pageData?.id,
      ...stainingData,
      stainingComplete: true,
      // Include parent hierarchy info for reference
      parentSampleId:
        selectedSample.parentSampleId || selectedSample.id.split("_")[0],
      parentSlideLabel: selectedSample.slideLabel || selectedSample.childLabel,
      parentSlideIndex: selectedSample.childIndex,
    };

    postToOpenElisServerJsonResponse(
      `/rest/notebook/pathology/staining/submit`,
      JSON.stringify(payload),
      (response) => {
        setSubmitting(false);
        if (response && response.success) {
          setStainingModalOpen(false);
          setSuccessMessage(
            intl.formatMessage({
              id: "pathology.staining.success",
              defaultMessage: "Staining data saved successfully.",
            }),
          );
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(response?.error || "Failed to save staining data.");
        }
      },
    );
  };

  // Mark samples as complete
  const handleMarkComplete = () => {
    if (submitting || selectedSampleIds.length === 0) return;

    const samplesToComplete = samples.filter(
      (s) => selectedSampleIds.includes(s.id) && s.stainingComplete,
    );

    if (samplesToComplete.length === 0) {
      setError(
        intl.formatMessage({
          id: "pathology.staining.error.noComplete",
          defaultMessage:
            "Please complete staining before marking samples as complete.",
        }),
      );
      return;
    }

    setSubmitting(true);
    setError(null);

    // Use string IDs for composite sample IDs (e.g., "123_slide_0")
    const sampleIds = samplesToComplete.map((s) => s.id);

    postToOpenElisServerJsonResponse(
      `/rest/notebook/bulk/page/${pageData?.id}/samples/status-string`,
      JSON.stringify({
        sampleIds: sampleIds,
        status: "COMPLETED",
      }),
      (response) => {
        setSubmitting(false);
        if (response && response.success) {
          setSelectedSampleIds([]);
          setSuccessMessage(
            intl.formatMessage(
              {
                id: "pathology.staining.success.complete",
                defaultMessage:
                  "Successfully marked {count} samples as complete.",
              },
              { count: samplesToComplete.length },
            ),
          );
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(response?.error || "Failed to update status.");
        }
      },
    );
  };

  // Handle selection changes
  const handleSelectionChange = (selectedRows) => {
    const ids = selectedRows.map((row) => row.id);
    setSelectedSampleIds(ids);
  };

  // Get available routine stains based on category
  const getRoutineStainOptions = () => {
    return routineStainOptions[stainingData.routineStainingCategory] || [];
  };

  // Table headers
  const headers = [
    {
      key: "accessionNumber",
      header: intl.formatMessage({
        id: "pathology.table.accessionNumber",
        defaultMessage: "Accession Number",
      }),
    },
    {
      key: "slideLabel",
      header: intl.formatMessage({
        id: "pathology.table.slide",
        defaultMessage: "Slide",
      }),
    },
    {
      key: "sampleType",
      header: intl.formatMessage({
        id: "pathology.table.specimenType",
        defaultMessage: "Specimen Type",
      }),
    },
    {
      key: "stainingStatus",
      header: intl.formatMessage({
        id: "pathology.table.stainingStatus",
        defaultMessage: "Staining Status",
      }),
    },
    {
      key: "stains",
      header: intl.formatMessage({
        id: "pathology.table.stains",
        defaultMessage: "Stains Applied",
      }),
    },
    {
      key: "stainQuality",
      header: intl.formatMessage({
        id: "pathology.table.quality",
        defaultMessage: "Quality",
      }),
    },
    {
      key: "qcStatus",
      header: intl.formatMessage({
        id: "pathology.table.qcStatus",
        defaultMessage: "QC",
      }),
    },
    {
      key: "status",
      header: intl.formatMessage({
        id: "pathology.table.status",
        defaultMessage: "Status",
      }),
    },
  ];

  // Computed values
  const completedCount = samples.filter((s) => s.stainingComplete).length;
  const pendingCount = samples.filter(
    (s) => s.status === "PENDING" || !s.stainingComplete,
  ).length;

  return (
    <div className="pathology-page staining-page">
      {/* Header */}
      <Grid className="page-header">
        <Column lg={12} md={8} sm={4}>
          <h3>
            <FormattedMessage
              id="pathology.page.staining.title"
              defaultMessage="Slide Staining"
            />
          </h3>
          <p className="page-description">
            <FormattedMessage
              id="pathology.page.staining.description"
              defaultMessage="Apply routine and special stains to prepared slides."
            />
          </p>
        </Column>
        <Column lg={4} md={8} sm={4} className="header-actions">
          <Button
            kind="ghost"
            size="sm"
            renderIcon={Renew}
            onClick={loadPageSamples}
            disabled={loading}
          >
            <FormattedMessage
              id="pathology.page.refresh"
              defaultMessage="Refresh"
            />
          </Button>
        </Column>
      </Grid>

      {/* Notifications */}
      {error && (
        <InlineNotification
          kind="error"
          title={intl.formatMessage({
            id: "notification.error",
            defaultMessage: "Error",
          })}
          subtitle={error}
          onCloseButtonClick={() => setError(null)}
          style={{ marginBottom: "1rem" }}
        />
      )}

      {successMessage && (
        <InlineNotification
          kind="success"
          title={intl.formatMessage({
            id: "notification.success",
            defaultMessage: "Success",
          })}
          subtitle={successMessage}
          onCloseButtonClick={() => setSuccessMessage(null)}
          style={{ marginBottom: "1rem" }}
        />
      )}

      {/* Summary Stats */}
      <Grid className="summary-stats" style={{ marginBottom: "1rem" }}>
        <Column lg={4} md={4} sm={4}>
          <Tile className="stat-tile">
            <span className="stat-value">{samples.length}</span>
            <span className="stat-label">
              <FormattedMessage
                id="pathology.stats.total"
                defaultMessage="Total Samples"
              />
            </span>
          </Tile>
        </Column>
        <Column lg={4} md={4} sm={4}>
          <Tile className="stat-tile">
            <span className="stat-value" style={{ color: "#24a148" }}>
              {completedCount}
            </span>
            <span className="stat-label">
              <FormattedMessage
                id="pathology.stats.stained"
                defaultMessage="Stained"
              />
            </span>
          </Tile>
        </Column>
        <Column lg={4} md={4} sm={4}>
          <Tile className="stat-tile">
            <span className="stat-value" style={{ color: "#f1c21b" }}>
              {pendingCount}
            </span>
            <span className="stat-label">
              <FormattedMessage
                id="pathology.stats.pending"
                defaultMessage="Pending"
              />
            </span>
          </Tile>
        </Column>
      </Grid>

      {/* Action Buttons */}
      {hasValidEntry && samples.length > 0 && (
        <div
          className="action-buttons"
          style={{ marginBottom: "1rem", display: "flex", gap: "0.5rem" }}
        >
          <Button
            kind="primary"
            size="md"
            renderIcon={Chemistry}
            onClick={() => {
              if (selectedSampleIds.length === 1) {
                const sample = samples.find(
                  (s) => s.id === selectedSampleIds[0],
                );
                if (sample) openStainingModal(sample);
              }
            }}
            disabled={selectedSampleIds.length !== 1 || submitting}
          >
            <FormattedMessage
              id="pathology.page.applyStaining"
              defaultMessage="Apply Staining"
            />
          </Button>
          <Button
            kind="secondary"
            size="md"
            renderIcon={Checkmark}
            onClick={handleMarkComplete}
            disabled={selectedSampleIds.length === 0 || submitting}
          >
            <FormattedMessage
              id="pathology.page.markComplete"
              defaultMessage="Mark Complete ({count})"
              values={{ count: selectedSampleIds.length }}
            />
          </Button>
        </div>
      )}

      {/* Sample Table */}
      <div className="sample-table">
        {loading ? (
          <Loading description="Loading samples..." withOverlay={false} />
        ) : !hasValidEntry ? (
          <div className="empty-state">
            <p>
              <FormattedMessage
                id="pathology.page.noEntryId"
                defaultMessage="No workflow entry available. Samples will appear here once the workflow is started."
              />
            </p>
          </div>
        ) : samples.length === 0 ? (
          <div className="empty-state">
            <p>
              <FormattedMessage
                id="pathology.page.staining.empty"
                defaultMessage="No samples available for staining. Samples must have slides created first."
              />
            </p>
          </div>
        ) : (
          <DataTable
            rows={samples.map((s) => ({
              ...s,
              stainingStatus:
                s.status === "COMPLETED"
                  ? "Complete"
                  : s.stainingComplete
                    ? "In Progress"
                    : "Pending",
            }))}
            headers={headers}
            isSortable
            render={({
              rows,
              headers,
              getTableProps,
              getHeaderProps,
              getRowProps,
              getSelectionProps,
              selectedRows,
            }) => {
              if (
                selectedRows.length !== selectedSampleIds.length ||
                !selectedRows.every((r) => selectedSampleIds.includes(r.id))
              ) {
                setTimeout(() => handleSelectionChange(selectedRows), 0);
              }

              return (
                <TableContainer>
                  <Table {...getTableProps()}>
                    <TableHead>
                      <TableRow>
                        <TableSelectAll {...getSelectionProps()} />
                        {headers.map((header) => (
                          <TableHeader
                            key={header.key}
                            {...getHeaderProps({ header })}
                          >
                            {header.header}
                          </TableHeader>
                        ))}
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {rows.map((row) => {
                        const sample = samples.find((s) => s.id === row.id);
                        return (
                          <TableRow key={row.id} {...getRowProps({ row })}>
                            <TableSelectRow {...getSelectionProps({ row })} />
                            {row.cells.map((cell) => (
                              <TableCell key={cell.id}>
                                {cell.info.header === "stainingStatus" ? (
                                  <span
                                    style={{
                                      display: "flex",
                                      alignItems: "center",
                                      gap: "0.25rem",
                                      cursor: "pointer",
                                    }}
                                    onClick={() => openStainingModal(sample)}
                                    title="Click to view/edit staining"
                                  >
                                    {sample?.status === "COMPLETED" ? (
                                      <>
                                        <CheckmarkFilled
                                          size={16}
                                          style={{ color: "#24a148" }}
                                        />
                                        <span>Complete</span>
                                      </>
                                    ) : sample?.stainingComplete ? (
                                      <Tag type="blue" size="sm">
                                        In Progress
                                      </Tag>
                                    ) : (
                                      <Tag type="gray" size="sm">
                                        Pending
                                      </Tag>
                                    )}
                                  </span>
                                ) : cell.info.header === "status" ? (
                                  <Tag
                                    type={
                                      sample?.status === "COMPLETED"
                                        ? "green"
                                        : sample?.status === "IN_PROGRESS"
                                          ? "blue"
                                          : "gray"
                                    }
                                    size="sm"
                                  >
                                    {sample?.status || "PENDING"}
                                  </Tag>
                                ) : cell.info.header === "stains" ? (
                                  <span>
                                    {sample?.routineStains?.length > 0 ||
                                    sample?.specialStains?.length > 0 ? (
                                      <div
                                        style={{
                                          display: "flex",
                                          flexWrap: "wrap",
                                          gap: "0.25rem",
                                        }}
                                      >
                                        {sample.routineStains
                                          ?.slice(0, 2)
                                          .map((stain, i) => (
                                            <Tag key={i} type="blue" size="sm">
                                              {stain}
                                            </Tag>
                                          ))}
                                        {sample.specialStains
                                          ?.slice(0, 2)
                                          .map((stain, i) => (
                                            <Tag
                                              key={i}
                                              type="purple"
                                              size="sm"
                                            >
                                              {stain}
                                            </Tag>
                                          ))}
                                        {(sample.routineStains?.length || 0) +
                                          (sample.specialStains?.length || 0) >
                                          4 && (
                                          <Tag type="gray" size="sm">
                                            +more
                                          </Tag>
                                        )}
                                      </div>
                                    ) : (
                                      "-"
                                    )}
                                  </span>
                                ) : cell.info.header === "stainQuality" ? (
                                  sample?.stainingComplete ? (
                                    <Tag
                                      type={
                                        sample.stainQuality === "Adequate"
                                          ? "green"
                                          : "gray"
                                      }
                                      size="sm"
                                    >
                                      {sample.stainQuality}
                                    </Tag>
                                  ) : (
                                    "-"
                                  )
                                ) : cell.info.header === "qcStatus" ? (
                                  sample?.qcStatus ? (
                                    <Tag
                                      type={
                                        sample.qcStatus === "PASS"
                                          ? "green"
                                          : sample.qcStatus === "FLAG"
                                            ? "yellow"
                                            : sample.qcStatus === "FAIL"
                                              ? "red"
                                              : "gray"
                                      }
                                      size="sm"
                                    >
                                      {sample.qcStatus === "PASS"
                                        ? "✓ Pass"
                                        : sample.qcStatus === "FLAG"
                                          ? "⚠ Flag"
                                          : sample.qcStatus === "FAIL"
                                            ? "✗ Fail"
                                            : "-"}
                                    </Tag>
                                  ) : (
                                    "-"
                                  )
                                ) : (
                                  cell.value || "-"
                                )}
                              </TableCell>
                            ))}
                          </TableRow>
                        );
                      })}
                    </TableBody>
                  </Table>
                </TableContainer>
              );
            }}
          />
        )}
      </div>

      {/* Staining Modal */}
      <Modal
        open={stainingModalOpen}
        modalHeading={intl.formatMessage(
          {
            id: "pathology.modal.staining.title",
            defaultMessage: "Slide Staining - {accession}",
          },
          { accession: selectedSample?.accessionNumber || "" },
        )}
        primaryButtonText={
          stainingViewMode
            ? intl.formatMessage({
                id: "pathology.modal.edit",
                defaultMessage: "Edit",
              })
            : intl.formatMessage({
                id: "pathology.modal.save",
                defaultMessage: "Save",
              })
        }
        secondaryButtonText={intl.formatMessage({
          id: "pathology.modal.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => {
          setStainingModalOpen(false);
          setSelectedSample(null);
          setStainingViewMode(false);
          setError(null);
        }}
        onRequestSubmit={
          stainingViewMode
            ? () => setStainingViewMode(false)
            : handleSubmitStaining
        }
        primaryButtonDisabled={submitting || stainingLoading}
        size="lg"
        hasScrollingContent
        preventCloseOnClickOutside
      >
        {stainingLoading ? (
          <div style={{ textAlign: "center", padding: "2rem" }}>
            <Loading description="Loading data..." withOverlay={false} />
          </div>
        ) : (
          <div style={{ maxHeight: "70vh", overflowY: "auto" }}>
            {/* View Mode Banner */}
            {stainingViewMode && (
              <InlineNotification
                kind="info"
                title={intl.formatMessage({
                  id: "pathology.staining.viewMode",
                  defaultMessage: "Viewing existing data",
                })}
                subtitle={intl.formatMessage({
                  id: "pathology.staining.viewMode.description",
                  defaultMessage: "Click 'Edit' to make changes.",
                })}
                lowContrast
                hideCloseButton
                style={{ marginBottom: "1rem" }}
              />
            )}

            {/* Routine Staining */}
            <h5 style={{ marginBottom: "1rem" }}>
              <Chemistry size={16} style={{ marginRight: "0.5rem" }} />
              <FormattedMessage
                id="pathology.staining.section.routine"
                defaultMessage="Routine Staining"
              />
            </h5>
            <Grid>
              <Column lg={8} md={4} sm={4}>
                <Select
                  id="routineStainingCategory"
                  name="routineStainingCategory"
                  labelText={intl.formatMessage({
                    id: "pathology.staining.category",
                    defaultMessage: "Staining Category",
                  })}
                  value={stainingData.routineStainingCategory}
                  onChange={handleStainingInputChange}
                  disabled={stainingViewMode}
                >
                  <SelectItem value="" text="Select category..." />
                  {stainingCategories.map((cat) => (
                    <SelectItem
                      key={cat.value}
                      value={cat.value}
                      text={cat.text}
                    />
                  ))}
                </Select>
              </Column>
              <Column lg={8} md={4} sm={4}>
                {stainingData.routineStainingCategory && (
                  <MultiSelect
                    id="routineStains"
                    titleText={intl.formatMessage({
                      id: "pathology.staining.routineStains",
                      defaultMessage: "Routine Stains",
                    })}
                    items={getRoutineStainOptions()}
                    itemToString={(item) => (item ? item.label : "")}
                    selectedItems={getRoutineStainOptions().filter((opt) =>
                      stainingData.routineStains.includes(opt.id),
                    )}
                    onChange={({ selectedItems }) =>
                      setStainingData((prev) => ({
                        ...prev,
                        routineStains: selectedItems.map((item) => item.id),
                      }))
                    }
                    disabled={stainingViewMode}
                  />
                )}
              </Column>
            </Grid>

            {/* Special Stains */}
            <h5 style={{ marginTop: "1.5rem", marginBottom: "1rem" }}>
              <FormattedMessage
                id="pathology.staining.section.special"
                defaultMessage="Special Stains"
              />
            </h5>
            <Grid>
              <Column lg={8} md={4} sm={4}>
                <MultiSelect
                  id="specialStains"
                  titleText={intl.formatMessage({
                    id: "pathology.staining.specialStains",
                    defaultMessage: "Special Stains",
                  })}
                  items={specialStainOptions}
                  itemToString={(item) => (item ? item.label : "")}
                  selectedItems={specialStainOptions.filter((opt) =>
                    stainingData.specialStains.includes(opt.id),
                  )}
                  onChange={({ selectedItems }) =>
                    setStainingData((prev) => ({
                      ...prev,
                      specialStains: selectedItems.map((item) => item.id),
                    }))
                  }
                  disabled={stainingViewMode}
                />
              </Column>
              <Column lg={8} md={4} sm={4}>
                <TextInput
                  id="specialStainIndication"
                  name="specialStainIndication"
                  labelText={intl.formatMessage({
                    id: "pathology.staining.indication",
                    defaultMessage: "Indication for Special Stains",
                  })}
                  value={stainingData.specialStainIndication}
                  onChange={handleStainingInputChange}
                  disabled={stainingViewMode}
                  placeholder="e.g., Rule out fungal infection"
                />
              </Column>
            </Grid>

            {/* IHC/ICC Section */}
            <h5 style={{ marginTop: "1.5rem", marginBottom: "1rem" }}>
              <FormattedMessage
                id="pathology.staining.section.ihc"
                defaultMessage="Immunohistochemistry (IHC/ICC)"
              />
            </h5>
            <Grid>
              <Column lg={4} md={4} sm={4}>
                <Checkbox
                  id="ihcIccPerformed"
                  name="ihcIccPerformed"
                  labelText={intl.formatMessage({
                    id: "pathology.staining.ihcPerformed",
                    defaultMessage: "IHC/ICC Performed",
                  })}
                  checked={stainingData.ihcIccPerformed}
                  onChange={(e) =>
                    setStainingData((prev) => ({
                      ...prev,
                      ihcIccPerformed: e.target.checked,
                    }))
                  }
                  disabled={stainingViewMode}
                />
              </Column>
            </Grid>

            {stainingData.ihcIccPerformed && (
              <Grid style={{ marginTop: "1rem" }}>
                <Column lg={4} md={4} sm={4}>
                  <TextInput
                    id="ihcIccMarkers"
                    name="ihcIccMarkers"
                    labelText={intl.formatMessage({
                      id: "pathology.staining.markers",
                      defaultMessage: "Markers",
                    })}
                    value={stainingData.ihcIccMarkers}
                    onChange={handleStainingInputChange}
                    disabled={stainingViewMode}
                    placeholder="e.g., CD3, CD20, Ki-67"
                  />
                </Column>
                <Column lg={4} md={4} sm={4}>
                  <TextInput
                    id="ihcIccPrimaryAntibody"
                    name="ihcIccPrimaryAntibody"
                    labelText={intl.formatMessage({
                      id: "pathology.staining.primaryAntibody",
                      defaultMessage: "Primary Antibody",
                    })}
                    value={stainingData.ihcIccPrimaryAntibody}
                    onChange={handleStainingInputChange}
                    disabled={stainingViewMode}
                  />
                </Column>
                <Column lg={4} md={4} sm={4}>
                  <TextInput
                    id="ihcIccClone"
                    name="ihcIccClone"
                    labelText={intl.formatMessage({
                      id: "pathology.staining.clone",
                      defaultMessage: "Clone",
                    })}
                    value={stainingData.ihcIccClone}
                    onChange={handleStainingInputChange}
                    disabled={stainingViewMode}
                  />
                </Column>
                <Column lg={4} md={4} sm={4}>
                  <TextInput
                    id="ihcIccDilution"
                    name="ihcIccDilution"
                    labelText={intl.formatMessage({
                      id: "pathology.staining.dilution",
                      defaultMessage: "Dilution",
                    })}
                    value={stainingData.ihcIccDilution}
                    onChange={handleStainingInputChange}
                    disabled={stainingViewMode}
                    placeholder="e.g., 1:100"
                  />
                </Column>
              </Grid>
            )}

            {/* Controls */}
            <h5 style={{ marginTop: "1.5rem", marginBottom: "1rem" }}>
              <FormattedMessage
                id="pathology.staining.section.controls"
                defaultMessage="Quality Controls"
              />
            </h5>
            <Grid>
              <Column lg={4} md={4} sm={4}>
                <Checkbox
                  id="positiveControlRun"
                  name="positiveControlRun"
                  labelText={intl.formatMessage({
                    id: "pathology.staining.positiveControl",
                    defaultMessage: "Positive Control Run",
                  })}
                  checked={stainingData.positiveControlRun}
                  onChange={(e) =>
                    setStainingData((prev) => ({
                      ...prev,
                      positiveControlRun: e.target.checked,
                    }))
                  }
                  disabled={stainingViewMode}
                />
              </Column>
              <Column lg={4} md={4} sm={4}>
                <Checkbox
                  id="negativeControlRun"
                  name="negativeControlRun"
                  labelText={intl.formatMessage({
                    id: "pathology.staining.negativeControl",
                    defaultMessage: "Negative Control Run",
                  })}
                  checked={stainingData.negativeControlRun}
                  onChange={(e) =>
                    setStainingData((prev) => ({
                      ...prev,
                      negativeControlRun: e.target.checked,
                    }))
                  }
                  disabled={stainingViewMode}
                />
              </Column>
              <Column lg={4} md={4} sm={4}>
                <Checkbox
                  id="controlsAccepted"
                  name="controlsAccepted"
                  labelText={intl.formatMessage({
                    id: "pathology.staining.controlsAccepted",
                    defaultMessage: "Controls Accepted",
                  })}
                  checked={stainingData.controlsAccepted}
                  onChange={(e) =>
                    setStainingData((prev) => ({
                      ...prev,
                      controlsAccepted: e.target.checked,
                    }))
                  }
                  disabled={stainingViewMode}
                />
              </Column>
              <Column lg={4} md={4} sm={4}>
                <Checkbox
                  id="stainQualityAdequate"
                  name="stainQualityAdequate"
                  labelText={intl.formatMessage({
                    id: "pathology.staining.qualityAdequate",
                    defaultMessage: "Stain Quality Adequate",
                  })}
                  checked={stainingData.stainQualityAdequate}
                  onChange={(e) =>
                    setStainingData((prev) => ({
                      ...prev,
                      stainQualityAdequate: e.target.checked,
                    }))
                  }
                  disabled={stainingViewMode}
                />
              </Column>
            </Grid>

            {/* Batch Info */}
            <Grid style={{ marginTop: "1rem" }}>
              <Column lg={4} md={4} sm={4}>
                <TextInput
                  id="batchNumber"
                  name="batchNumber"
                  labelText={intl.formatMessage({
                    id: "pathology.staining.batchNumber",
                    defaultMessage: "Batch Number",
                  })}
                  value={stainingData.batchNumber}
                  onChange={handleStainingInputChange}
                  disabled={stainingViewMode}
                />
              </Column>
              <Column lg={4} md={4} sm={4}>
                <TextInput
                  id="batchDate"
                  name="batchDate"
                  labelText={intl.formatMessage({
                    id: "pathology.staining.batchDate",
                    defaultMessage: "Batch Date",
                  })}
                  value={stainingData.batchDate}
                  onChange={handleStainingInputChange}
                  disabled={stainingViewMode}
                  type="date"
                />
              </Column>
            </Grid>

            {/* Staff Info */}
            <h5 style={{ marginTop: "1.5rem", marginBottom: "1rem" }}>
              <FormattedMessage
                id="pathology.staining.section.staff"
                defaultMessage="Staff & Date"
              />
            </h5>
            <Grid>
              <Column lg={4} md={4} sm={4}>
                <TextInput
                  id="technicianName"
                  name="technicianName"
                  labelText={intl.formatMessage({
                    id: "pathology.staining.technicianName",
                    defaultMessage: "Technician Name",
                  })}
                  value={stainingData.technicianName}
                  onChange={handleStainingInputChange}
                  disabled={stainingViewMode}
                />
              </Column>
              <Column lg={4} md={4} sm={4}>
                <TextInput
                  id="technicianInitials"
                  name="technicianInitials"
                  labelText={intl.formatMessage({
                    id: "pathology.staining.technicianInitials",
                    defaultMessage: "Initials",
                  })}
                  value={stainingData.technicianInitials}
                  onChange={handleStainingInputChange}
                  disabled={stainingViewMode}
                />
              </Column>
              <Column lg={4} md={4} sm={4}>
                <TextInput
                  id="stainingDate"
                  name="stainingDate"
                  labelText={intl.formatMessage({
                    id: "pathology.staining.date",
                    defaultMessage: "Date",
                  })}
                  value={stainingData.stainingDate}
                  onChange={handleStainingInputChange}
                  disabled={stainingViewMode}
                  type="date"
                />
              </Column>
            </Grid>

            {/* Notes */}
            <Grid style={{ marginTop: "1rem" }}>
              <Column lg={16} md={8} sm={4}>
                <TextArea
                  id="notes"
                  name="notes"
                  labelText={intl.formatMessage({
                    id: "pathology.staining.notes",
                    defaultMessage: "Additional Notes",
                  })}
                  value={stainingData.notes}
                  onChange={handleStainingInputChange}
                  disabled={stainingViewMode}
                  rows={2}
                />
              </Column>
            </Grid>

            {/* Quality Control Section */}
            <h5 style={{ marginTop: "1.5rem", marginBottom: "1rem" }}>
              <FormattedMessage
                id="pathology.qc.section.title"
                defaultMessage="Quality Control & Assessment"
              />
            </h5>

            {/* QC Status Indicator */}
            <div
              style={{
                padding: "1rem",
                marginBottom: "1rem",
                backgroundColor:
                  stainingData.qcStatus === "PASS"
                    ? "#defbe6"
                    : stainingData.qcStatus === "FLAG"
                      ? "#fff8e1"
                      : "#fff1f1",
                borderRadius: "4px",
                border: `1px solid ${
                  stainingData.qcStatus === "PASS"
                    ? "#24a148"
                    : stainingData.qcStatus === "FLAG"
                      ? "#f1c21b"
                      : "#da1e28"
                }`,
              }}
            >
              <Grid>
                <Column lg={8} md={4} sm={4}>
                  <Select
                    id="qcStatus"
                    name="qcStatus"
                    labelText={intl.formatMessage({
                      id: "pathology.qc.status",
                      defaultMessage: "QC Status",
                    })}
                    value={stainingData.qcStatus}
                    onChange={handleStainingInputChange}
                    disabled={stainingViewMode}
                  >
                    <SelectItem value="PASS" text="✓ Pass - All criteria met" />
                    <SelectItem
                      value="FLAG"
                      text="⚠ Flag - Proceed with caution"
                    />
                    <SelectItem
                      value="FAIL"
                      text="✗ Fail - Requires corrective action"
                    />
                  </Select>
                </Column>
                <Column lg={8} md={4} sm={4}>
                  {stainingData.qcStatus === "FLAG" && (
                    <InlineNotification
                      kind="warning"
                      title={intl.formatMessage({
                        id: "pathology.qc.flagged",
                        defaultMessage: "Sample Flagged",
                      })}
                      subtitle={intl.formatMessage({
                        id: "pathology.qc.flagged.description",
                        defaultMessage:
                          "Processing can continue but issue is documented.",
                      })}
                      lowContrast
                      hideCloseButton
                    />
                  )}
                  {stainingData.qcStatus === "FAIL" && (
                    <InlineNotification
                      kind="error"
                      title={intl.formatMessage({
                        id: "pathology.qc.failed",
                        defaultMessage: "QC Failed",
                      })}
                      subtitle={intl.formatMessage({
                        id: "pathology.qc.failed.description",
                        defaultMessage:
                          "Corrective action required. Sample is still processed to preserve specimen.",
                      })}
                      lowContrast
                      hideCloseButton
                    />
                  )}
                </Column>
              </Grid>
            </div>

            {/* QC Checkpoints */}
            <Grid>
              <Column lg={4} md={4} sm={4}>
                <Checkbox
                  id="qcStainIntensity"
                  name="qcStainIntensity"
                  labelText={intl.formatMessage({
                    id: "pathology.qc.staining.stainIntensity",
                    defaultMessage: "Stain intensity acceptable",
                  })}
                  checked={stainingData.qcStainIntensity}
                  onChange={(e) =>
                    setStainingData((prev) => ({
                      ...prev,
                      qcStainIntensity: e.target.checked,
                    }))
                  }
                  disabled={stainingViewMode}
                />
              </Column>
              <Column lg={4} md={4} sm={4}>
                <Checkbox
                  id="qcBackgroundClean"
                  name="qcBackgroundClean"
                  labelText={intl.formatMessage({
                    id: "pathology.qc.staining.backgroundClean",
                    defaultMessage: "Background clean",
                  })}
                  checked={stainingData.qcBackgroundClean}
                  onChange={(e) =>
                    setStainingData((prev) => ({
                      ...prev,
                      qcBackgroundClean: e.target.checked,
                    }))
                  }
                  disabled={stainingViewMode}
                />
              </Column>
              <Column lg={4} md={4} sm={4}>
                <Checkbox
                  id="qcControlsValid"
                  name="qcControlsValid"
                  labelText={intl.formatMessage({
                    id: "pathology.qc.staining.controlsValid",
                    defaultMessage: "Controls valid",
                  })}
                  checked={stainingData.qcControlsValid}
                  onChange={(e) =>
                    setStainingData((prev) => ({
                      ...prev,
                      qcControlsValid: e.target.checked,
                    }))
                  }
                  disabled={stainingViewMode}
                />
              </Column>
              <Column lg={4} md={4} sm={4}>
                <Checkbox
                  id="qcLabelingCorrect"
                  name="qcLabelingCorrect"
                  labelText={intl.formatMessage({
                    id: "pathology.qc.labelingCorrect",
                    defaultMessage: "Labeling correct & complete",
                  })}
                  checked={stainingData.qcLabelingCorrect}
                  onChange={(e) =>
                    setStainingData((prev) => ({
                      ...prev,
                      qcLabelingCorrect: e.target.checked,
                    }))
                  }
                  disabled={stainingViewMode}
                />
              </Column>
            </Grid>

            {/* QC Issues and Corrective Actions */}
            {(stainingData.qcStatus === "FLAG" ||
              stainingData.qcStatus === "FAIL") && (
              <Grid style={{ marginTop: "1rem" }}>
                <Column lg={8} md={4} sm={4}>
                  <TextArea
                    id="qcIssues"
                    name="qcIssues"
                    labelText={intl.formatMessage({
                      id: "pathology.qc.issues",
                      defaultMessage: "QC Issues Identified",
                    })}
                    placeholder={intl.formatMessage({
                      id: "pathology.qc.issues.placeholder",
                      defaultMessage: "Describe any quality issues observed...",
                    })}
                    value={stainingData.qcIssues}
                    onChange={handleStainingInputChange}
                    disabled={stainingViewMode}
                    rows={3}
                  />
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <TextArea
                    id="qcCorrectiveAction"
                    name="qcCorrectiveAction"
                    labelText={intl.formatMessage({
                      id: "pathology.qc.correctiveAction",
                      defaultMessage: "Corrective Action Taken",
                    })}
                    placeholder={intl.formatMessage({
                      id: "pathology.qc.correctiveAction.placeholder",
                      defaultMessage:
                        "Describe corrective actions or mitigation steps...",
                    })}
                    value={stainingData.qcCorrectiveAction}
                    onChange={handleStainingInputChange}
                    disabled={stainingViewMode}
                    rows={3}
                  />
                </Column>
              </Grid>
            )}

            {/* QC Reviewer */}
            <Grid style={{ marginTop: "1rem" }}>
              <Column lg={4} md={4} sm={4}>
                <TextInput
                  id="qcReviewedBy"
                  name="qcReviewedBy"
                  labelText={intl.formatMessage({
                    id: "pathology.qc.reviewedBy",
                    defaultMessage: "QC Reviewed By",
                  })}
                  value={stainingData.qcReviewedBy}
                  onChange={handleStainingInputChange}
                  disabled={stainingViewMode}
                />
              </Column>
              <Column lg={4} md={4} sm={4}>
                <TextInput
                  id="qcReviewDate"
                  name="qcReviewDate"
                  labelText={intl.formatMessage({
                    id: "pathology.qc.reviewDate",
                    defaultMessage: "QC Review Date",
                  })}
                  value={stainingData.qcReviewDate}
                  onChange={handleStainingInputChange}
                  disabled={stainingViewMode}
                  type="date"
                />
              </Column>
            </Grid>
          </div>
        )}
      </Modal>
    </div>
  );
}

export default PathologyStainingPage;
