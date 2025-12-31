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
  NumberInput,
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
  Checkbox,
  Loading,
  TableContainer,
} from "@carbon/react";
import {
  Add,
  Checkmark,
  Renew,
  CheckmarkFilled,
  Archive,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import StorageHierarchySelector from "../../workflow/StorageHierarchySelector";
import BoxLayoutViewer from "../../workflow/BoxLayoutViewer";
import "../../workflow/NotebookWorkflow.css";

/**
 * PathologySlidesPage - Slide Preparation workflow step.
 * Purpose: Cut and prepare slides from tissue blocks via microtomy.
 * Who uses it: Histology Technicians
 *
 * Key activities:
 * - Cut tissue sections using microtome
 * - Mount sections on slides
 * - Label slides appropriately
 * - Document section thickness and quality
 */
function PathologySlidesPage({
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

  // Slide Modal state
  const [slideModalOpen, setSlideModalOpen] = useState(false);
  const [selectedSample, setSelectedSample] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [slideLoading, setSlideLoading] = useState(false);
  const [slideViewMode, setSlideViewMode] = useState(false);

  // Slide form data
  const [slideData, setSlideData] = useState({
    numberOfSlides: 1,
    slidePrefix: "",
    slideLabels: [],
    // Microtomy
    sectionThickness: 4,
    thicknessUnit: "um",
    microtomeId: "",
    bladeType: "",
    // Section quality
    sectionQuality: "",
    qualityNotes: "",
    floatationBathTemp: "",
    // Mounting
    slideType: "frosted",
    mountingMedium: "",
    coverslipped: false,
    // Technician info
    technicianName: "",
    technicianInitials: "",
    slideDate: "",
    notes: "",
    // Quality Control
    qcStatus: "PASS",
    qcSectionQuality: true,
    qcMountingQuality: true,
    qcLabelingCorrect: true,
    qcSlideIntegrity: true,
    qcIssues: "",
    qcCorrectiveAction: "",
    qcReviewedBy: "",
    qcReviewDate: "",
  });

  // Archive Modal state
  const [archiveModalOpen, setArchiveModalOpen] = useState(false);
  const [archiveData, setArchiveData] = useState({
    // Hierarchical storage selection
    roomId: null,
    deviceId: null,
    shelfId: null,
    rackId: null,
    boxId: null,
    // Slide-specific storage (cabinet/drawer alternative)
    slideCabinet: "",
    slideDrawer: "",
    slidePosition: "",
    // Archive metadata
    archiveReason: "completed",
    slideCondition: "excellent",
    retentionPeriod: "10",
    archiveDate: "",
    archivedBy: "",
    notes: "",
  });
  // Storage hierarchy selection state
  const [storageSelection, setStorageSelection] = useState({
    room: null,
    device: null,
    shelf: null,
    rack: null,
    box: null,
  });
  // Box layout state for archive modal
  const [boxLayout, setBoxLayout] = useState({});
  const [wellAssignments, setWellAssignments] = useState({});
  const [loadingBoxLayout, setLoadingBoxLayout] = useState(false);

  // Archive reason options
  const archiveReasons = [
    { value: "completed", text: "Diagnosis complete - standard archival" },
    { value: "reviewed", text: "Reviewed and signed out" },
    { value: "consultation", text: "Consultation case - returned" },
    { value: "research", text: "Reserved for research" },
    { value: "teaching", text: "Teaching collection" },
    { value: "qc", text: "QC retention sample" },
    { value: "legal", text: "Legal/regulatory hold" },
    { value: "reorder", text: "May need re-examination" },
    { value: "other", text: "Other" },
  ];

  // Slide condition options
  const slideConditions = [
    { value: "excellent", text: "Excellent - No damage" },
    { value: "good", text: "Good - Minor wear" },
    { value: "fair", text: "Fair - Some fading" },
    { value: "poor", text: "Poor - Significant damage" },
    { value: "broken", text: "Broken/Compromised" },
  ];

  // Retention period options (slides typically have longer retention)
  const retentionPeriods = [
    { value: "5", text: "5 years" },
    { value: "10", text: "10 years (Standard)" },
    { value: "20", text: "20 years" },
    { value: "25", text: "25 years" },
    { value: "permanent", text: "Permanent" },
  ];

  // Section quality options
  const qualityOptions = [
    { value: "excellent", text: "Excellent - No artifacts" },
    { value: "good", text: "Good - Minor artifacts" },
    { value: "acceptable", text: "Acceptable - Some artifacts" },
    { value: "poor", text: "Poor - Significant artifacts" },
    { value: "recut", text: "Needs Re-cut" },
  ];

  // Slide type options
  const slideTypes = [
    { value: "frosted", text: "Frosted End" },
    { value: "plain", text: "Plain" },
    { value: "charged", text: "Charged/Plus" },
    { value: "adhesive", text: "Adhesive Coated" },
  ];

  // Blade type options
  const bladeTypes = [
    { value: "disposable", text: "Disposable Steel" },
    { value: "tungsten", text: "Tungsten Carbide" },
    { value: "diamond", text: "Diamond" },
    { value: "glass", text: "Glass Knife" },
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

    // Fetch samples that have completed the previous step (blocks)
    // and merge with current slides page data
    getFromOpenElisServer(
      `/rest/notebook/pathology/workflow/samples-ready?entryId=${entryId}&currentStep=slides`,
      (workflowResponse) => {
        if (!componentMounted.current) return;

        // Also fetch current page samples to get slide data
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
                // For expanded items (e.g., "123_block_0"), try the full ID first,
                // then fall back to parent ID for backward compatibility
                const pageSample =
                  pageSampleMap[sampleId] ||
                  pageSampleMap[sample.parentSampleId] ||
                  pageSampleMap[sampleId.split("_")[0]];
                const slideData = pageSample?.data || {};
                // Note: workflowData contains data from PREVIOUS step (blocks)
                // We should NOT use it for slide-specific fields

                return {
                  id: sampleId,
                  externalId: sample.externalId,
                  accessionNumber: sample.accessionNumber,
                  sampleType:
                    sample.sampleType || sample.typeOfSample?.description,
                  specimenCategory: sample.specimenCategory || "histopathology",
                  collectionDate: sample.collectionDate,
                  // ONLY use status from current slides page, default to PENDING
                  // Backend returns status as "pageStatus" field
                  status:
                    pageSample?.pageStatus || pageSample?.status || "PENDING",
                  patientName: sample.patientName,
                  // Parent info from block step (from workflow expansion)
                  parentSampleId: sample.parentSampleId,
                  childIndex: sample.childIndex,
                  childLabel: sample.childLabel,
                  blockLabel: sample.blockLabel || sample.childLabel || "",
                  // Slide status from current page data ONLY
                  slidesCreated: slideData.slidesCreated === true,
                  slideCount: slideData.slideCount || 0,
                  sectionThickness: slideData.sectionThickness || "",
                  sectionQuality: slideData.sectionQuality || "",
                  technicianName: slideData.technicianName || "",
                  slideDate: slideData.slideDate || "",
                  // QC status from current page ONLY - show nothing until slides created
                  qcStatus: slideData.qcStatus || "",
                  // Storage/archive info from page data
                  storageLocation: slideData.storageLocation || "",
                  storagePath: slideData.storagePath || "",
                  storageBox: slideData.storageBox || "",
                  storageWell:
                    slideData.storageWell || slideData.wellCoordinate || "",
                  isArchived: slideData.isArchived || false,
                  terminalStatus: slideData.terminalStatus || "",
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

  const handleSlideInputChange = (e) => {
    const { name, value, type, checked } = e.target;
    setSlideData((prev) => ({
      ...prev,
      [name]: type === "checkbox" ? checked : value,
    }));
  };

  // Open slide modal
  const openSlideModal = (sample) => {
    setSelectedSample(sample);
    setSlideViewMode(false);
    setSlideLoading(true);

    // Reset form data with defaults
    setSlideData({
      numberOfSlides: 1,
      slidePrefix: sample.accessionNumber || "",
      slideLabels: [],
      sectionThickness: 4,
      thicknessUnit: "um",
      microtomeId: "",
      bladeType: "",
      sectionQuality: "",
      qualityNotes: "",
      floatationBathTemp: "",
      slideType: "frosted",
      mountingMedium: "",
      coverslipped: false,
      technicianName: "",
      technicianInitials: "",
      slideDate: new Date().toISOString().split("T")[0],
      notes: "",
      // QC fields
      qcStatus: "PASS",
      qcSectionQuality: true,
      qcMountingQuality: true,
      qcLabelingCorrect: true,
      qcSlideIntegrity: true,
      qcIssues: "",
      qcCorrectiveAction: "",
      qcReviewedBy: "",
      qcReviewDate: new Date().toISOString().split("T")[0],
    });

    setSlideModalOpen(true);

    // Fetch existing slide data if available
    if (pageData?.id && sample?.id) {
      getFromOpenElisServer(
        `/rest/notebook/pathology/slides/${sample.id}?pageId=${pageData.id}`,
        (response) => {
          setSlideLoading(false);
          if (response && response.success && response.hasData) {
            setSlideViewMode(true);
            setSlideData((prev) => ({
              ...prev,
              numberOfSlides: response.numberOfSlides || 1,
              slidePrefix: response.slidePrefix || "",
              slideLabels: response.slideLabels || [],
              sectionThickness: response.sectionThickness || 4,
              thicknessUnit: response.thicknessUnit || "um",
              microtomeId: response.microtomeId || "",
              bladeType: response.bladeType || "",
              sectionQuality: response.sectionQuality || "",
              qualityNotes: response.qualityNotes || "",
              floatationBathTemp: response.floatationBathTemp || "",
              slideType: response.slideType || "frosted",
              mountingMedium: response.mountingMedium || "",
              coverslipped: response.coverslipped === true,
              technicianName: response.technicianName || "",
              technicianInitials: response.technicianInitials || "",
              slideDate: response.slideDate || "",
              notes: response.notes || "",
              // QC fields
              qcStatus: response.qcStatus || "PASS",
              qcSectionQuality: response.qcSectionQuality !== false,
              qcMountingQuality: response.qcMountingQuality !== false,
              qcLabelingCorrect: response.qcLabelingCorrect !== false,
              qcSlideIntegrity: response.qcSlideIntegrity !== false,
              qcIssues: response.qcIssues || "",
              qcCorrectiveAction: response.qcCorrectiveAction || "",
              qcReviewedBy: response.qcReviewedBy || "",
              qcReviewDate: response.qcReviewDate || "",
            }));
          }
        },
      );
    } else {
      setSlideLoading(false);
    }
  };

  // Submit slide data
  const handleSubmitSlides = () => {
    if (submitting) return;

    setSubmitting(true);
    setError(null);

    // Generate slide labels
    const labels = [];
    for (let i = 1; i <= slideData.numberOfSlides; i++) {
      labels.push(`${slideData.slidePrefix}-S${String(i).padStart(2, "0")}`);
    }

    // Use the full composite sample ID (e.g., "123_block_0") to ensure
    // each block's slide data is stored separately
    const sampleId = selectedSample.id;

    const payload = {
      sampleId: sampleId,
      pageId: pageData?.id,
      ...slideData,
      slideLabels: labels,
      slidesCreated: true,
      slideCount: slideData.numberOfSlides,
      // Include parent hierarchy info for reference
      parentSampleId:
        selectedSample.parentSampleId || selectedSample.id.split("_")[0],
      parentBlockLabel: selectedSample.blockLabel || selectedSample.childLabel,
      parentBlockIndex: selectedSample.childIndex,
    };

    postToOpenElisServerJsonResponse(
      `/rest/notebook/pathology/slides/submit`,
      JSON.stringify(payload),
      (response) => {
        setSubmitting(false);
        if (response && response.success) {
          setSlideModalOpen(false);
          setSuccessMessage(
            intl.formatMessage({
              id: "pathology.slides.success",
              defaultMessage: "Slides created successfully.",
            }),
          );
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(response?.error || "Failed to save slide data.");
        }
      },
    );
  };

  // Mark samples as complete
  const handleMarkComplete = () => {
    if (submitting || selectedSampleIds.length === 0) return;

    const samplesToComplete = samples.filter(
      (s) => selectedSampleIds.includes(s.id) && s.slidesCreated,
    );

    if (samplesToComplete.length === 0) {
      setError(
        intl.formatMessage({
          id: "pathology.slides.error.noComplete",
          defaultMessage:
            "Please create slides before marking samples as complete.",
        }),
      );
      return;
    }

    setSubmitting(true);
    setError(null);

    // Use string IDs for composite sample IDs (e.g., "123_block_0")
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
                id: "pathology.slides.success.complete",
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

  // Handle archive input change
  const handleArchiveInputChange = (e) => {
    const { name, value } = e.target;
    setArchiveData((prev) => ({
      ...prev,
      [name]: value,
    }));
  };

  // Handle storage hierarchy selection change
  const handleStorageSelectionChange = (selection) => {
    setStorageSelection(selection);
    setArchiveData((prev) => ({
      ...prev,
      roomId: selection.room?.id || null,
      deviceId: selection.device?.id || null,
      shelfId: selection.shelf?.id || null,
      rackId: selection.rack?.id || null,
      boxId: selection.box?.id || null,
    }));

    // Load box layout when box is selected
    // Must use notebookId - do not fall back to entryId as they are different ID types
    if (selection.box?.id && notebookId) {
      setLoadingBoxLayout(true);
      setBoxLayout({});
      setWellAssignments({});
      getFromOpenElisServer(
        `/rest/notebook/${notebookId}/box/${selection.box.id}/layout`,
        (response) => {
          setLoadingBoxLayout(false);
          if (componentMounted.current && response) {
            setBoxLayout(response.wells || {});
          }
        },
      );
    } else {
      setBoxLayout({});
      setWellAssignments({});
    }
  };

  // Auto-populate wells with selected samples
  const handleAutoPopulate = () => {
    if (!storageSelection.box) return;

    const rows = storageSelection.box.rows || 8;
    const columns = storageSelection.box.columns || 12;
    const rowLetters = Array.from({ length: rows }, (_, i) =>
      String.fromCharCode("A".charCodeAt(0) + i),
    );

    const newAssignments = {};
    let sampleIndex = 0;

    // Iterate through wells row by row
    for (let row of rowLetters) {
      for (let col = 1; col <= columns; col++) {
        if (sampleIndex >= selectedSampleIds.length) break;

        const wellCoord = `${row}${col}`;
        // Skip if well is already occupied
        if (!boxLayout[wellCoord]) {
          newAssignments[selectedSampleIds[sampleIndex]] = wellCoord;
          sampleIndex++;
        }
      }
      if (sampleIndex >= selectedSampleIds.length) break;
    }

    setWellAssignments(newAssignments);

    if (sampleIndex < selectedSampleIds.length) {
      setError(
        intl.formatMessage(
          {
            id: "pathology.archive.notEnoughWells",
            defaultMessage:
              "Not enough empty wells. {assigned} of {total} samples assigned.",
          },
          { assigned: sampleIndex, total: selectedSampleIds.length },
        ),
      );
    } else {
      setError(null);
      setSuccessMessage(
        intl.formatMessage(
          {
            id: "pathology.archive.autoPopulateSuccess",
            defaultMessage: "Auto-assigned {count} samples to wells.",
          },
          { count: sampleIndex },
        ),
      );
    }
  };

  // Handle well click to manually assign/unassign sample
  const handleWellClick = (wellCoord, wellInfo) => {
    if (wellInfo) {
      // Well is already occupied - can't assign here
      return;
    }

    // Find if any sample is currently assigned to this well
    const assignedSampleId = Object.keys(wellAssignments).find(
      (sampleId) => wellAssignments[sampleId] === wellCoord,
    );

    if (assignedSampleId) {
      // Unassign the sample from this well
      const newAssignments = { ...wellAssignments };
      delete newAssignments[assignedSampleId];
      setWellAssignments(newAssignments);
    } else {
      // Find first unassigned sample and assign to this well
      const unassignedSample = selectedSampleIds.find(
        (id) => !wellAssignments[id],
      );
      if (unassignedSample) {
        setWellAssignments((prev) => ({
          ...prev,
          [unassignedSample]: wellCoord,
        }));
      }
    }
  };

  // Open archive modal
  const openArchiveModal = () => {
    if (selectedSampleIds.length === 0) return;

    // Reset archive form data with defaults
    setArchiveData({
      roomId: null,
      deviceId: null,
      shelfId: null,
      rackId: null,
      boxId: null,
      slideCabinet: "",
      slideDrawer: "",
      slidePosition: "",
      archiveReason: "completed",
      slideCondition: "excellent",
      retentionPeriod: "10",
      archiveDate: new Date().toISOString().split("T")[0],
      archivedBy: "",
      notes: "",
    });
    // Reset storage selection
    setStorageSelection({
      room: null,
      device: null,
      shelf: null,
      rack: null,
      box: null,
    });
    // Reset box layout state
    setBoxLayout({});
    setWellAssignments({});

    setArchiveModalOpen(true);
  };

  // Submit archive data - Archives samples and they do NOT proceed to next page
  const handleSubmitArchive = () => {
    if (submitting || selectedSampleIds.length === 0) return;

    setSubmitting(true);
    setError(null);

    const sampleIds = selectedSampleIds;

    // Build payload matching backend ArchiveSamplesRequest structure
    // Note: pageId is already in the URL, isArchived/terminalStatus are set by backend
    // Include well assignments if auto-populate was used
    const payload = {
      sampleIds: sampleIds,
      // Hierarchical storage location
      roomId: archiveData.roomId || null,
      deviceId: archiveData.deviceId || null,
      shelfId: archiveData.shelfId || null,
      rackId: archiveData.rackId || null,
      boxId: archiveData.boxId || null,
      // Slide-specific storage fields
      slideCabinet: archiveData.slideCabinet || null,
      slideDrawer: archiveData.slideDrawer || null,
      slidePosition: archiveData.slidePosition || null,
      slideCondition: archiveData.slideCondition || null,
      // Archive metadata
      archiveReason: archiveData.archiveReason || null,
      retentionPeriod: archiveData.retentionPeriod || null,
      archiveDate: archiveData.archiveDate || null,
      archivedBy: archiveData.archivedBy || null,
      notes: archiveData.notes || null,
      // Well assignments from auto-populate or manual selection
      wellAssignments:
        Object.keys(wellAssignments).length > 0 ? wellAssignments : null,
    };

    postToOpenElisServerJsonResponse(
      `/rest/notebook/bulk/page/${pageData?.id}/samples/archive`,
      JSON.stringify(payload),
      (response) => {
        setSubmitting(false);
        if (response && response.success) {
          setArchiveModalOpen(false);
          setSelectedSampleIds([]);
          setSuccessMessage(
            intl.formatMessage(
              {
                id: "pathology.archive.slide.success",
                defaultMessage:
                  "Successfully archived {count} slide(s). They will not proceed to staining.",
              },
              { count: sampleIds.length },
            ),
          );
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(response?.error || "Failed to archive slides.");
        }
      },
    );
  };

  // Handle selection changes
  const handleSelectionChange = (selectedRows) => {
    const ids = selectedRows.map((row) => row.id);
    setSelectedSampleIds(ids);
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
      key: "blockLabel",
      header: intl.formatMessage({
        id: "pathology.table.block",
        defaultMessage: "Block",
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
      key: "slideStatus",
      header: intl.formatMessage({
        id: "pathology.table.slideStatus",
        defaultMessage: "Slide Status",
      }),
    },
    {
      key: "slideCount",
      header: intl.formatMessage({
        id: "pathology.table.slideCount",
        defaultMessage: "Slides",
      }),
    },
    {
      key: "sectionThickness",
      header: intl.formatMessage({
        id: "pathology.table.thickness",
        defaultMessage: "Thickness",
      }),
    },
    {
      key: "sectionQuality",
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
      key: "storageLocation",
      header: intl.formatMessage({
        id: "pathology.table.storageLocation",
        defaultMessage: "Storage",
      }),
    },
  ];

  // Computed values
  const completedCount = samples.filter((s) => s.slidesCreated).length;
  const pendingCount = samples.filter(
    (s) => s.status === "PENDING" || !s.slidesCreated,
  ).length;

  return (
    <div className="pathology-page slides-page">
      {/* Header */}
      <Grid className="page-header">
        <Column lg={12} md={8} sm={4}>
          <h3>
            <FormattedMessage
              id="pathology.page.slides.title"
              defaultMessage="Slide Preparation"
            />
          </h3>
          <p className="page-description">
            <FormattedMessage
              id="pathology.page.slides.description"
              defaultMessage="Cut and prepare slides from tissue blocks via microtomy."
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
                id="pathology.stats.slidesCreated"
                defaultMessage="Slides Created"
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
            renderIcon={Add}
            onClick={() => {
              if (selectedSampleIds.length === 1) {
                const sample = samples.find(
                  (s) => s.id === selectedSampleIds[0],
                );
                if (sample) openSlideModal(sample);
              }
            }}
            disabled={selectedSampleIds.length !== 1 || submitting}
          >
            <FormattedMessage
              id="pathology.page.createSlides"
              defaultMessage="Create Slides"
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
          <Button
            kind="tertiary"
            size="md"
            renderIcon={Archive}
            onClick={openArchiveModal}
            disabled={selectedSampleIds.length === 0 || submitting}
          >
            <FormattedMessage
              id="pathology.page.archiveSlides"
              defaultMessage="Archive ({count})"
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
                id="pathology.page.slides.empty"
                defaultMessage="No samples available for slide preparation. Samples must have blocks created first."
              />
            </p>
          </div>
        ) : (
          <DataTable
            rows={samples.map((s) => ({
              ...s,
              slideStatus:
                s.status === "COMPLETED"
                  ? "Complete"
                  : s.slidesCreated
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
                                {cell.info.header === "slideStatus" ? (
                                  <span
                                    style={{
                                      display: "flex",
                                      alignItems: "center",
                                      gap: "0.25rem",
                                      cursor: "pointer",
                                    }}
                                    onClick={() => openSlideModal(sample)}
                                    title="Click to view/edit slides"
                                  >
                                    {sample?.status === "SKIPPED" ||
                                    sample?.isArchived ? (
                                      <Tag type="purple" size="sm">
                                        📦 Archived
                                      </Tag>
                                    ) : sample?.status === "COMPLETED" ? (
                                      <>
                                        <CheckmarkFilled
                                          size={16}
                                          style={{ color: "#24a148" }}
                                        />
                                        <span>Complete</span>
                                      </>
                                    ) : sample?.slidesCreated ? (
                                      <Tag type="blue" size="sm">
                                        In Progress
                                      </Tag>
                                    ) : (
                                      <Tag type="gray" size="sm">
                                        Pending
                                      </Tag>
                                    )}
                                  </span>
                                ) : cell.info.header === "slideCount" ? (
                                  <span>
                                    {sample?.slideCount > 0
                                      ? sample.slideCount
                                      : "-"}
                                  </span>
                                ) : cell.info.header === "sectionThickness" ? (
                                  sample?.sectionThickness ? (
                                    <span>{sample.sectionThickness} µm</span>
                                  ) : (
                                    "-"
                                  )
                                ) : cell.info.header === "sectionQuality" ? (
                                  sample?.sectionQuality ? (
                                    <Tag
                                      type={
                                        sample.sectionQuality === "excellent"
                                          ? "green"
                                          : sample.sectionQuality === "good"
                                            ? "teal"
                                            : sample.sectionQuality ===
                                                "acceptable"
                                              ? "blue"
                                              : "red"
                                      }
                                      size="sm"
                                    >
                                      {sample.sectionQuality}
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
                                ) : cell.info.header === "storageLocation" ? (
                                  sample?.isArchived ||
                                  sample?.storagePath ||
                                  sample?.storageLocation ? (
                                    <Tag
                                      type={
                                        sample.isArchived ? "purple" : "cyan"
                                      }
                                      size="sm"
                                      title={
                                        sample.storagePath ||
                                        sample.storageLocation
                                      }
                                    >
                                      {sample.isArchived ? "📦 " : ""}
                                      {sample.storagePath ||
                                        sample.storageLocation ||
                                        (sample.storageBox
                                          ? sample.storageBox +
                                            (sample.storageWell
                                              ? " - " + sample.storageWell
                                              : "")
                                          : "Archived")}
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

      {/* Slide Modal */}
      <Modal
        open={slideModalOpen}
        modalHeading={intl.formatMessage(
          {
            id: "pathology.modal.slides.title",
            defaultMessage: "Slide Preparation - {accession}",
          },
          { accession: selectedSample?.accessionNumber || "" },
        )}
        primaryButtonText={
          slideViewMode
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
          setSlideModalOpen(false);
          setSelectedSample(null);
          setSlideViewMode(false);
          setError(null);
        }}
        onRequestSubmit={
          slideViewMode ? () => setSlideViewMode(false) : handleSubmitSlides
        }
        primaryButtonDisabled={submitting || slideLoading}
        size="lg"
        hasScrollingContent
        preventCloseOnClickOutside
      >
        {slideLoading ? (
          <div style={{ textAlign: "center", padding: "2rem" }}>
            <Loading description="Loading data..." withOverlay={false} />
          </div>
        ) : (
          <div style={{ maxHeight: "70vh", overflowY: "auto" }}>
            {/* View Mode Banner */}
            {slideViewMode && (
              <InlineNotification
                kind="info"
                title={intl.formatMessage({
                  id: "pathology.slides.viewMode",
                  defaultMessage: "Viewing existing data",
                })}
                subtitle={intl.formatMessage({
                  id: "pathology.slides.viewMode.description",
                  defaultMessage: "Click 'Edit' to make changes.",
                })}
                lowContrast
                hideCloseButton
                style={{ marginBottom: "1rem" }}
              />
            )}

            {/* Slide Configuration */}
            <h5 style={{ marginBottom: "1rem" }}>
              <FormattedMessage
                id="pathology.slides.section.configuration"
                defaultMessage="Slide Configuration"
              />
            </h5>
            <Grid>
              <Column lg={4} md={4} sm={4}>
                <NumberInput
                  id="numberOfSlides"
                  name="numberOfSlides"
                  label={intl.formatMessage({
                    id: "pathology.slides.numberOfSlides",
                    defaultMessage: "Number of Slides",
                  })}
                  value={slideData.numberOfSlides}
                  min={1}
                  max={100}
                  onChange={(e, { value }) =>
                    setSlideData((prev) => ({
                      ...prev,
                      numberOfSlides: value,
                    }))
                  }
                  disabled={slideViewMode}
                />
              </Column>
              <Column lg={4} md={4} sm={4}>
                <TextInput
                  id="slidePrefix"
                  name="slidePrefix"
                  labelText={intl.formatMessage({
                    id: "pathology.slides.prefix",
                    defaultMessage: "Slide ID Prefix",
                  })}
                  value={slideData.slidePrefix}
                  onChange={handleSlideInputChange}
                  disabled={slideViewMode}
                />
              </Column>
              <Column lg={4} md={4} sm={4}>
                <Select
                  id="slideType"
                  name="slideType"
                  labelText={intl.formatMessage({
                    id: "pathology.slides.slideType",
                    defaultMessage: "Slide Type",
                  })}
                  value={slideData.slideType}
                  onChange={handleSlideInputChange}
                  disabled={slideViewMode}
                >
                  {slideTypes.map((type) => (
                    <SelectItem
                      key={type.value}
                      value={type.value}
                      text={type.text}
                    />
                  ))}
                </Select>
              </Column>
            </Grid>

            {/* Slide Labels Preview */}
            {slideData.numberOfSlides > 0 && slideData.slidePrefix && (
              <div style={{ marginTop: "1rem" }}>
                <p style={{ fontSize: "0.875rem", marginBottom: "0.5rem" }}>
                  <FormattedMessage
                    id="pathology.slides.labelsPreview"
                    defaultMessage="Generated Labels:"
                  />
                </p>
                <div
                  style={{ display: "flex", flexWrap: "wrap", gap: "0.5rem" }}
                >
                  {Array.from(
                    { length: Math.min(slideData.numberOfSlides, 10) },
                    (_, i) => (
                      <Tag key={i} type="cyan" size="sm">
                        {`${slideData.slidePrefix}-S${String(i + 1).padStart(2, "0")}`}
                      </Tag>
                    ),
                  )}
                  {slideData.numberOfSlides > 10 && (
                    <Tag type="gray" size="sm">
                      +{slideData.numberOfSlides - 10} more
                    </Tag>
                  )}
                </div>
              </div>
            )}

            {/* Microtomy Details */}
            <h5 style={{ marginTop: "1.5rem", marginBottom: "1rem" }}>
              <FormattedMessage
                id="pathology.slides.section.microtomy"
                defaultMessage="Microtomy Details"
              />
            </h5>
            <Grid>
              <Column lg={4} md={4} sm={4}>
                <NumberInput
                  id="sectionThickness"
                  name="sectionThickness"
                  label={intl.formatMessage({
                    id: "pathology.slides.thickness",
                    defaultMessage: "Section Thickness (µm)",
                  })}
                  value={slideData.sectionThickness}
                  min={1}
                  max={50}
                  onChange={(e, { value }) =>
                    setSlideData((prev) => ({
                      ...prev,
                      sectionThickness: value,
                    }))
                  }
                  disabled={slideViewMode}
                />
              </Column>
              <Column lg={4} md={4} sm={4}>
                <TextInput
                  id="microtomeId"
                  name="microtomeId"
                  labelText={intl.formatMessage({
                    id: "pathology.slides.microtomeId",
                    defaultMessage: "Microtome ID",
                  })}
                  value={slideData.microtomeId}
                  onChange={handleSlideInputChange}
                  disabled={slideViewMode}
                />
              </Column>
              <Column lg={4} md={4} sm={4}>
                <Select
                  id="bladeType"
                  name="bladeType"
                  labelText={intl.formatMessage({
                    id: "pathology.slides.bladeType",
                    defaultMessage: "Blade Type",
                  })}
                  value={slideData.bladeType}
                  onChange={handleSlideInputChange}
                  disabled={slideViewMode}
                >
                  <SelectItem value="" text="Select blade..." />
                  {bladeTypes.map((blade) => (
                    <SelectItem
                      key={blade.value}
                      value={blade.value}
                      text={blade.text}
                    />
                  ))}
                </Select>
              </Column>
            </Grid>

            <Grid style={{ marginTop: "1rem" }}>
              <Column lg={4} md={4} sm={4}>
                <TextInput
                  id="floatationBathTemp"
                  name="floatationBathTemp"
                  labelText={intl.formatMessage({
                    id: "pathology.slides.bathTemp",
                    defaultMessage: "Floatation Bath Temp (°C)",
                  })}
                  value={slideData.floatationBathTemp}
                  onChange={handleSlideInputChange}
                  disabled={slideViewMode}
                />
              </Column>
              <Column lg={4} md={4} sm={4}>
                <Select
                  id="sectionQuality"
                  name="sectionQuality"
                  labelText={intl.formatMessage({
                    id: "pathology.slides.quality",
                    defaultMessage: "Section Quality",
                  })}
                  value={slideData.sectionQuality}
                  onChange={handleSlideInputChange}
                  disabled={slideViewMode}
                >
                  <SelectItem value="" text="Select quality..." />
                  {qualityOptions.map((opt) => (
                    <SelectItem
                      key={opt.value}
                      value={opt.value}
                      text={opt.text}
                    />
                  ))}
                </Select>
              </Column>
              <Column lg={4} md={4} sm={4}>
                <Checkbox
                  id="coverslipped"
                  name="coverslipped"
                  labelText={intl.formatMessage({
                    id: "pathology.slides.coverslipped",
                    defaultMessage: "Coverslipped",
                  })}
                  checked={slideData.coverslipped}
                  onChange={(e) =>
                    setSlideData((prev) => ({
                      ...prev,
                      coverslipped: e.target.checked,
                    }))
                  }
                  disabled={slideViewMode}
                />
              </Column>
            </Grid>

            {/* Staff Info */}
            <h5 style={{ marginTop: "1.5rem", marginBottom: "1rem" }}>
              <FormattedMessage
                id="pathology.slides.section.staff"
                defaultMessage="Staff & Date"
              />
            </h5>
            <Grid>
              <Column lg={4} md={4} sm={4}>
                <TextInput
                  id="technicianName"
                  name="technicianName"
                  labelText={intl.formatMessage({
                    id: "pathology.slides.technicianName",
                    defaultMessage: "Technician Name",
                  })}
                  value={slideData.technicianName}
                  onChange={handleSlideInputChange}
                  disabled={slideViewMode}
                />
              </Column>
              <Column lg={4} md={4} sm={4}>
                <TextInput
                  id="technicianInitials"
                  name="technicianInitials"
                  labelText={intl.formatMessage({
                    id: "pathology.slides.technicianInitials",
                    defaultMessage: "Initials",
                  })}
                  value={slideData.technicianInitials}
                  onChange={handleSlideInputChange}
                  disabled={slideViewMode}
                />
              </Column>
              <Column lg={4} md={4} sm={4}>
                <TextInput
                  id="slideDate"
                  name="slideDate"
                  labelText={intl.formatMessage({
                    id: "pathology.slides.date",
                    defaultMessage: "Date",
                  })}
                  value={slideData.slideDate}
                  onChange={handleSlideInputChange}
                  disabled={slideViewMode}
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
                    id: "pathology.slides.notes",
                    defaultMessage: "Additional Notes",
                  })}
                  value={slideData.notes}
                  onChange={handleSlideInputChange}
                  disabled={slideViewMode}
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
                  slideData.qcStatus === "PASS"
                    ? "#defbe6"
                    : slideData.qcStatus === "FLAG"
                      ? "#fff8e1"
                      : "#fff1f1",
                borderRadius: "4px",
                border: `1px solid ${
                  slideData.qcStatus === "PASS"
                    ? "#24a148"
                    : slideData.qcStatus === "FLAG"
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
                    value={slideData.qcStatus}
                    onChange={handleSlideInputChange}
                    disabled={slideViewMode}
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
                  {slideData.qcStatus === "FLAG" && (
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
                  {slideData.qcStatus === "FAIL" && (
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
                  id="qcSectionQuality"
                  name="qcSectionQuality"
                  labelText={intl.formatMessage({
                    id: "pathology.qc.slides.sectionQuality",
                    defaultMessage: "Section quality acceptable",
                  })}
                  checked={slideData.qcSectionQuality}
                  onChange={(e) =>
                    setSlideData((prev) => ({
                      ...prev,
                      qcSectionQuality: e.target.checked,
                    }))
                  }
                  disabled={slideViewMode}
                />
              </Column>
              <Column lg={4} md={4} sm={4}>
                <Checkbox
                  id="qcMountingQuality"
                  name="qcMountingQuality"
                  labelText={intl.formatMessage({
                    id: "pathology.qc.slides.mountingQuality",
                    defaultMessage: "Mounting quality verified",
                  })}
                  checked={slideData.qcMountingQuality}
                  onChange={(e) =>
                    setSlideData((prev) => ({
                      ...prev,
                      qcMountingQuality: e.target.checked,
                    }))
                  }
                  disabled={slideViewMode}
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
                  checked={slideData.qcLabelingCorrect}
                  onChange={(e) =>
                    setSlideData((prev) => ({
                      ...prev,
                      qcLabelingCorrect: e.target.checked,
                    }))
                  }
                  disabled={slideViewMode}
                />
              </Column>
              <Column lg={4} md={4} sm={4}>
                <Checkbox
                  id="qcSlideIntegrity"
                  name="qcSlideIntegrity"
                  labelText={intl.formatMessage({
                    id: "pathology.qc.slides.slideIntegrity",
                    defaultMessage: "Slide integrity verified",
                  })}
                  checked={slideData.qcSlideIntegrity}
                  onChange={(e) =>
                    setSlideData((prev) => ({
                      ...prev,
                      qcSlideIntegrity: e.target.checked,
                    }))
                  }
                  disabled={slideViewMode}
                />
              </Column>
            </Grid>

            {/* QC Issues and Corrective Actions */}
            {(slideData.qcStatus === "FLAG" ||
              slideData.qcStatus === "FAIL") && (
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
                    value={slideData.qcIssues}
                    onChange={handleSlideInputChange}
                    disabled={slideViewMode}
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
                    value={slideData.qcCorrectiveAction}
                    onChange={handleSlideInputChange}
                    disabled={slideViewMode}
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
                  value={slideData.qcReviewedBy}
                  onChange={handleSlideInputChange}
                  disabled={slideViewMode}
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
                  value={slideData.qcReviewDate}
                  onChange={handleSlideInputChange}
                  disabled={slideViewMode}
                  type="date"
                />
              </Column>
            </Grid>
          </div>
        )}
      </Modal>

      {/* Archive Modal */}
      <Modal
        open={archiveModalOpen}
        modalHeading={intl.formatMessage(
          {
            id: "pathology.modal.slideArchive.title",
            defaultMessage: "Archive {count} Slide(s)",
          },
          { count: selectedSampleIds.length },
        )}
        primaryButtonText={intl.formatMessage({
          id: "pathology.modal.archive",
          defaultMessage: "Archive",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "pathology.modal.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => {
          setArchiveModalOpen(false);
          setError(null);
        }}
        onRequestSubmit={handleSubmitArchive}
        primaryButtonDisabled={submitting}
        size="lg"
        danger
        preventCloseOnClickOutside
      >
        <InlineNotification
          kind="warning"
          title={intl.formatMessage({
            id: "pathology.archive.warning.title",
            defaultMessage: "Confirm Archive",
          })}
          subtitle={intl.formatMessage({
            id: "pathology.archive.slide.warning.description",
            defaultMessage:
              "Archived slides will NOT proceed to Staining. They will be removed from the active workflow.",
          })}
          lowContrast
          hideCloseButton
          style={{ marginBottom: "1rem" }}
        />

        {/* Storage Location - Hierarchical Selector */}
        <h5 style={{ marginBottom: "0.5rem" }}>
          <FormattedMessage
            id="pathology.archive.section.storage"
            defaultMessage="Storage Location"
          />
        </h5>
        <StorageHierarchySelector
          onSelectionChange={handleStorageSelectionChange}
          boxRequired={false}
          showPath={true}
          entryId={notebookId}
        />

        {/* Box Layout Viewer with Auto-populate */}
        {storageSelection.box && (
          <div style={{ marginTop: "1rem" }}>
            <div
              style={{
                display: "flex",
                justifyContent: "space-between",
                alignItems: "center",
                marginBottom: "0.5rem",
              }}
            >
              <h5>
                <FormattedMessage
                  id="pathology.archive.wellAssignment"
                  defaultMessage="Well Assignment"
                />
              </h5>
              <Button
                kind="tertiary"
                size="sm"
                onClick={handleAutoPopulate}
                disabled={loadingBoxLayout}
              >
                <FormattedMessage
                  id="pathology.archive.autoPopulate"
                  defaultMessage="Auto-populate Wells"
                />
              </Button>
            </div>

            {loadingBoxLayout ? (
              <Loading
                withOverlay={false}
                small
                description="Loading box layout..."
              />
            ) : (
              <>
                <BoxLayoutViewer
                  boxId={storageSelection.box.id}
                  layout={{
                    ...boxLayout,
                    // Add current well assignments as pending (different visual)
                    ...Object.fromEntries(
                      Object.entries(wellAssignments).map(
                        ([sampleId, wellCoord]) => [
                          wellCoord,
                          { sampleItemId: sampleId, pending: true },
                        ],
                      ),
                    ),
                  }}
                  rows={storageSelection.box.rows || 8}
                  columns={storageSelection.box.columns || 12}
                  onWellClick={handleWellClick}
                />
                {Object.keys(wellAssignments).length > 0 && (
                  <div
                    style={{
                      marginTop: "0.5rem",
                      fontSize: "0.875rem",
                      color: "#525252",
                    }}
                  >
                    <FormattedMessage
                      id="pathology.archive.assignedCount"
                      defaultMessage="{count} sample(s) assigned to wells"
                      values={{ count: Object.keys(wellAssignments).length }}
                    />
                  </div>
                )}
              </>
            )}
          </div>
        )}

        {/* Slide-specific storage details (cabinet/drawer for slide archives) */}
        <Grid style={{ marginTop: "1rem" }}>
          <Column lg={5} md={4} sm={4}>
            <TextInput
              id="archiveSlideCabinet"
              name="slideCabinet"
              labelText={intl.formatMessage({
                id: "pathology.archive.slideCabinet",
                defaultMessage: "Slide Cabinet (optional)",
              })}
              value={archiveData.slideCabinet}
              onChange={handleArchiveInputChange}
              placeholder="e.g., Cabinet 5"
            />
          </Column>
          <Column lg={5} md={4} sm={4}>
            <TextInput
              id="archiveSlideDrawer"
              name="slideDrawer"
              labelText={intl.formatMessage({
                id: "pathology.archive.slideDrawer",
                defaultMessage: "Drawer (optional)",
              })}
              value={archiveData.slideDrawer}
              onChange={handleArchiveInputChange}
              placeholder="e.g., Drawer 12"
            />
          </Column>
          <Column lg={6} md={4} sm={4}>
            <TextInput
              id="archiveSlidePosition"
              name="slidePosition"
              labelText={intl.formatMessage({
                id: "pathology.archive.slidePosition",
                defaultMessage: "Position (optional)",
              })}
              value={archiveData.slidePosition}
              onChange={handleArchiveInputChange}
              placeholder="e.g., Row 3, Slot 15"
            />
          </Column>
        </Grid>

        {/* Archive Details */}
        <h5 style={{ marginTop: "1rem", marginBottom: "0.5rem" }}>
          <FormattedMessage
            id="pathology.archive.section.details"
            defaultMessage="Archive Details"
          />
        </h5>
        <Grid>
          <Column lg={8} md={4} sm={4}>
            <Select
              id="archiveReason"
              name="archiveReason"
              labelText={intl.formatMessage({
                id: "pathology.archive.reason",
                defaultMessage: "Archive Reason",
              })}
              value={archiveData.archiveReason}
              onChange={handleArchiveInputChange}
            >
              {archiveReasons.map((reason) => (
                <SelectItem
                  key={reason.value}
                  value={reason.value}
                  text={reason.text}
                />
              ))}
            </Select>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <Select
              id="archiveSlideCondition"
              name="slideCondition"
              labelText={intl.formatMessage({
                id: "pathology.archive.slideCondition",
                defaultMessage: "Slide Condition",
              })}
              value={archiveData.slideCondition}
              onChange={handleArchiveInputChange}
            >
              {slideConditions.map((condition) => (
                <SelectItem
                  key={condition.value}
                  value={condition.value}
                  text={condition.text}
                />
              ))}
            </Select>
          </Column>
        </Grid>

        <Grid style={{ marginTop: "0.5rem" }}>
          <Column lg={16} md={8} sm={4}>
            <Select
              id="archiveRetentionPeriod"
              name="retentionPeriod"
              labelText={intl.formatMessage({
                id: "pathology.archive.retentionPeriod",
                defaultMessage: "Retention Period",
              })}
              value={archiveData.retentionPeriod}
              onChange={handleArchiveInputChange}
            >
              {retentionPeriods.map((period) => (
                <SelectItem
                  key={period.value}
                  value={period.value}
                  text={period.text}
                />
              ))}
            </Select>
          </Column>
        </Grid>

        {/* Staff & Date */}
        <h5 style={{ marginTop: "1rem", marginBottom: "0.5rem" }}>
          <FormattedMessage
            id="pathology.archive.section.staff"
            defaultMessage="Staff & Date"
          />
        </h5>
        <Grid>
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="archiveArchivedBy"
              name="archivedBy"
              labelText={intl.formatMessage({
                id: "pathology.archive.archivedBy",
                defaultMessage: "Archived By",
              })}
              value={archiveData.archivedBy}
              onChange={handleArchiveInputChange}
            />
          </Column>
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="archiveDate"
              name="archiveDate"
              labelText={intl.formatMessage({
                id: "pathology.archive.date",
                defaultMessage: "Archive Date",
              })}
              value={archiveData.archiveDate}
              onChange={handleArchiveInputChange}
              type="date"
            />
          </Column>
        </Grid>

        {/* Notes */}
        <Grid style={{ marginTop: "1rem" }}>
          <Column lg={16} md={8} sm={4}>
            <TextArea
              id="archiveNotes"
              name="notes"
              labelText={intl.formatMessage({
                id: "pathology.archive.notes",
                defaultMessage: "Notes",
              })}
              value={archiveData.notes}
              onChange={handleArchiveInputChange}
              rows={3}
            />
          </Column>
        </Grid>
      </Modal>
    </div>
  );
}

export default PathologySlidesPage;
