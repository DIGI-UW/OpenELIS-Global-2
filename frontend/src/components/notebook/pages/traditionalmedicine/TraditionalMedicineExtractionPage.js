import React, {
  useState,
  useEffect,
  useRef,
  useCallback,
  useMemo,
  useContext,
} from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  Tag,
  Modal,
  Dropdown,
  TextInput,
  TextArea,
  Loading,
  NumberInput,
} from "@carbon/react";
import {
  Renew,
  CheckmarkFilled,
  Edit,
  Archive,
  Pending,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import { NotificationContext } from "../../../layout/Layout";
import { NotificationKinds } from "../../../common/CustomNotification";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import SampleGrid from "../../workflow/SampleGrid";
import { usePermissions } from "../../../../hooks/usePermissions";
import { useTMMRDPermissions } from "../../../../hooks/useTMMRDPermissions";
import AccessDeniedMessage from "../../../common/AccessDeniedMessage";
import "../../workflow/NotebookWorkflow.css";

/**
 * TraditionalMedicineExtractionPage - Page 4 of the Traditional Medicine workflow.
 *
 * SRS Requirements - STAGE 5: Extraction, Filtration & Concentration
 * - Extraction: Solvents (ethanol, methanol, water, hexane, chloroform)
 * - Techniques: Maceration, Soxhlet, Ultrasonic, Distillation
 * - Filtration: Methods (filter paper, vacuum, centrifugation)
 * - Concentration: Evaporation, distillation
 * - Yield calculation and tracking
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - The notebook page data
 */
function TraditionalMedicineExtractionPage({
  entryId,
  pageData,
  progress: _progress,
  onProgressUpdate,
}) {
  const intl = useIntl();
  const { setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const componentMounted = useRef(false);
  const { hasAnyRole } = usePermissions();

  // TMMRD permissions per SRS Section 11
  const { getPagePermissionLevel, canSaveData, canAccessStage3to4 } =
    useTMMRDPermissions();

  // STAGE 4 allowed roles per TMMRD SRS Section 11 - Lab Technicians and Researchers
  const allowedRoles = [
    "Lab Technician",
    "Researcher",
    "Pharmacognosist",
    "Lab Manager",
    "Principal Investigator",
  ];

  const canAccessPage = hasAnyRole(allowedRoles);

  // Check page access - show access denied if user lacks required roles
  if (!canAccessPage) {
    return (
      <AccessDeniedMessage
        page="Extraction & Concentration"
        reason="This page requires specific Traditional Medicine extraction roles to access."
        requiredRoles={allowedRoles}
      />
    );
  }

  // Get user's action-level permission for this page
  const pagePermissionLevel = getPagePermissionLevel(
    "Extraction & Concentration",
  );
  const canEditData = canSaveData(pagePermissionLevel);

  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [loading, setLoading] = useState(true);

  const [extractionModalOpen, setExtractionModalOpen] = useState(false);
  const [isApplyingExtraction, setIsApplyingExtraction] = useState(false);
  const [isCompleting, setIsCompleting] = useState(false);

  const [solventType, setSolventType] = useState(null);
  const [extractionTechnique, setExtractionTechnique] = useState(null);
  const [plantMaterialWeight, setPlantMaterialWeight] = useState("");
  const [solventVolume, setSolventVolume] = useState("");
  const [extractionTemp, setExtractionTemp] = useState("");
  const [extractionDuration, setExtractionDuration] = useState("");
  const [filtrationMethod, setFiltrationMethod] = useState(null);
  const [concentrationMethod, setConcentrationMethod] = useState(null);
  const [extractWeight, setExtractWeight] = useState("");
  const [extractNotes, setExtractNotes] = useState("");

  // Extract Quality Assessment - SRS Section 5: Extraction QC
  const [extractColor, setExtractColor] = useState("");
  const [extractOdor, setExtractOdor] = useState("");
  const [extractConsistency, setExtractConsistency] = useState("");
  const [extractContaminationFree, setExtractContaminationFree] =
    useState(false);

  const solventOptions = [
    { id: "ETHANOL", label: "Ethanol" },
    { id: "METHANOL", label: "Methanol" },
    { id: "WATER", label: "Water" },
    { id: "HEXANE", label: "Hexane" },
    { id: "CHLOROFORM", label: "Chloroform" },
    { id: "ACETONE", label: "Acetone" },
    { id: "OTHER", label: "Other Solvent" },
  ];

  const techniqueOptions = [
    { id: "MACERATION", label: "Maceration" },
    { id: "SOXHLET", label: "Soxhlet Extraction" },
    { id: "ULTRASONIC", label: "Ultrasonic Extraction" },
    { id: "DISTILLATION", label: "Distillation" },
    { id: "OTHER", label: "Other Technique" },
  ];

  const filtrationOptions = [
    { id: "FILTER_PAPER", label: "Filter Paper" },
    { id: "VACUUM", label: "Vacuum Filtration" },
    { id: "CENTRIFUGATION", label: "Centrifugation" },
  ];

  const concentrationOptions = [
    { id: "ROTARY_EVAPORATOR", label: "Rotary Evaporator" },
    { id: "DISTILLATION", label: "Distillation" },
    { id: "NONE", label: "No Concentration" },
  ];

  // Notification callback
  const notify = useCallback(
    ({ kind = NotificationKinds.info, title, message }) => {
      setNotificationVisible(true);
      addNotification({ kind, title, message });
    },
    [addNotification, setNotificationVisible],
  );

  const hasRealPageId =
    pageData?.id && !String(pageData.id).startsWith("default-");

  const loadPageSamples = useCallback(() => {
    if (!pageData?.id || String(pageData.id).startsWith("default-")) {
      setLoading(false);
      return;
    }

    setLoading(true);

    getFromOpenElisServer(
      `/rest/notebook/page/${pageData.id}/samples`,
      (response) => {
        if (componentMounted.current) {
          let samplesToProcess = [];

          // Handle both array and object responses from API
          if (response) {
            if (Array.isArray(response)) {
              samplesToProcess = response;
            } else if (response.samples && Array.isArray(response.samples)) {
              samplesToProcess = response.samples;
            }
          }

          setSamples(
            samplesToProcess.length > 0
              ? samplesToProcess.map((s) => ({
                  id: String(s.id || s.sampleItemId),
                  externalId: s.externalId,
                  accessionNumber: s.accessionNumber,
                  status: s.pageStatus || s.status || "PENDING",
                  localName: s.data?.localName,
                  scientificName: s.data?.scientificName,
                  sampleCategory: s.data?.sampleCategory,
                  plantPart: s.data?.plantPart,
                  collectionDate: s.data?.collectionDate,
                  intendedUse: s.data?.intendedUse,
                  solventType: s.data?.solvent,
                  extractionTechnique: s.data?.extractionTechnique,
                  filtrationMethod: s.data?.filtrationMethod,
                  concentrationMethod: s.data?.concentrationMethod,
                  yieldPercent: s.data?.extractYieldPercentage,
                  extractWeight: s.data?.extractWeight,
                }))
              : [],
          );
          setLoading(false);
        }
      },
    );
  }, [pageData?.id]);

  useEffect(() => {
    componentMounted.current = true;
    loadPageSamples();
    return () => {
      componentMounted.current = false;
    };
  }, [entryId, pageData?.id, loadPageSamples]);

  const resetForm = useCallback(() => {
    setSolventType(null);
    setExtractionTechnique(null);
    setPlantMaterialWeight("");
    setSolventVolume("");
    setExtractionTemp("");
    setExtractionDuration("");
    setFiltrationMethod(null);
    setConcentrationMethod(null);
    setExtractWeight("");
    setExtractNotes("");
    // Reset QC fields
    setExtractColor("");
    setExtractOdor("");
    setExtractConsistency("");
    setExtractContaminationFree(false);
  }, []);

  const openModal = useCallback(() => {
    if (selectedSampleIds.length === 0) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.page.tradmed.error.noSelection",
          defaultMessage: "Please select at least one sample.",
        }),
      });
      return;
    }
    resetForm();
    setExtractionModalOpen(true);
  }, [selectedSampleIds, intl, resetForm, notify]);

  const applyExtraction = useCallback(() => {
    if (!solventType || !extractionTechnique) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.page.tradmed.extraction.error.required",
          defaultMessage: "Please select solvent and technique.",
        }),
      });
      return;
    }

    if (!hasRealPageId) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.page.tradmed.error.noPage",
          defaultMessage:
            "Cannot update samples: Page not properly initialized.",
        }),
      });
      return;
    }

    setIsApplyingExtraction(true);

    const sampleIds = selectedSampleIds.map((id) => parseInt(id, 10));
    const yieldPercent =
      plantMaterialWeight && extractWeight
        ? (
            (parseFloat(extractWeight) / parseFloat(plantMaterialWeight)) *
            100
          ).toFixed(2)
        : null;

    postToOpenElisServerJsonResponse(
      `/rest/notebook/tradmed/page/${pageData.id}/extract`,
      JSON.stringify({
        sampleIds,
        solvent: solventType.id,
        extractionTechnique: extractionTechnique.id,
        materialWeight: plantMaterialWeight,
        materialWeightUnit: "g",
        solventVolume,
        solventVolumeUnit: "mL",
        extractionTemperature: extractionTemp,
        temperatureUnit: "C",
        extractionDurationMinutes: Math.round(
          parseFloat(extractionDuration) * 60,
        ),
        numberOfCycles: 1,
        filtrationMethod: filtrationMethod?.id || "NONE",
        concentrationMethod: concentrationMethod?.id || "NONE",
        extractWeight,
        extractWeightUnit: "g",
        extractAppearance: `${extractColor} with ${extractOdor} odor, ${extractConsistency} consistency`,
        extractColor,
        notes: extractNotes,
      }),
      (response) => {
        setIsApplyingExtraction(false);
        if (response?.success) {
          // Update sample status using bulk endpoint after extraction
          postToOpenElisServer(
            `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
            JSON.stringify({
              sampleIds,
              status: "IN_PROGRESS",
            }),
            (statusCode) => {
              if (statusCode === 200) {
                // Apply QC metadata to store extract quality assessment
                const qcResult =
                  extractColor && extractConsistency && extractContaminationFree
                    ? "PASS"
                    : "PENDING";

                postToOpenElisServerJsonResponse(
                  `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
                  JSON.stringify({
                    sampleIds,
                    data: {
                      extractQuality: {
                        color: extractColor,
                        odor: extractOdor,
                        consistency: extractConsistency,
                        contaminationFree: extractContaminationFree,
                        qcResult,
                      },
                    },
                  }),
                  (qcResponse) => {
                    notify({
                      kind: NotificationKinds.success,
                      title:
                        response.message ||
                        intl.formatMessage(
                          {
                            id: "notebook.page.tradmed.extraction.success",
                            defaultMessage: "Extracted {count} sample(s).",
                          },
                          {
                            count:
                              response.updatedCount || selectedSampleIds.length,
                          },
                        ),
                    });
                    setExtractionModalOpen(false);
                    setSelectedSampleIds([]);
                    loadPageSamples();
                    if (onProgressUpdate) onProgressUpdate();
                  },
                );
              } else {
                notify({
                  kind: NotificationKinds.error,
                  title: intl.formatMessage({
                    id: "notebook.page.tradmed.error.statusUpdate",
                    defaultMessage:
                      "Extraction completed but failed to update sample status.",
                  }),
                });
              }
            },
          );
        } else {
          notify({
            kind: NotificationKinds.error,
            title: response?.error || "Extraction failed",
          });
        }
      },
    );
  }, [
    solventType,
    extractionTechnique,
    plantMaterialWeight,
    solventVolume,
    extractionTemp,
    extractionDuration,
    filtrationMethod,
    concentrationMethod,
    extractWeight,
    extractNotes,
    extractColor,
    extractOdor,
    extractConsistency,
    extractContaminationFree,
    hasRealPageId,
    pageData?.id,
    selectedSampleIds,
    intl,
    loadPageSamples,
    onProgressUpdate,
    notify,
  ]);

  // Handle marking extracted samples complete (moving to next page)
  const handleMarkComplete = useCallback(() => {
    // Filter samples that can be marked complete: selected and in extraction (IN_PROGRESS status)
    // IN_PROGRESS status indicates extraction was recorded
    const samplesToComplete = samples.filter(
      (s) => selectedSampleIds.includes(s.id) && s.status === "IN_PROGRESS",
    );

    if (samplesToComplete.length === 0) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({
          id: "notebook.tradmed.extract.noEligibleSamples",
          defaultMessage:
            "Selected samples must have extraction recorded (status: In Progress) before completing.",
        }),
      });
      return;
    }

    setIsCompleting(true);

    const sampleIds = samplesToComplete.map((s) => parseInt(s.id, 10));

    postToOpenElisServerJsonResponse(
      `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
      JSON.stringify({ sampleIds: sampleIds, status: "COMPLETED" }),
      (response) => {
        setIsCompleting(false);

        if (response && response.success) {
          notify({
            kind: NotificationKinds.success,
            title: intl.formatMessage(
              {
                id: "notebook.tradmed.extract.completeSuccess",
                defaultMessage:
                  "Successfully marked {count} samples as complete.",
              },
              { count: response.updatedCount || sampleIds.length },
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
            title:
              response?.error ||
              intl.formatMessage({
                id: "notebook.tradmed.extract.completeFailed",
                defaultMessage: "Failed to mark samples complete.",
              }),
          });
        }
      },
    );
  }, [
    selectedSampleIds,
    samples,
    pageData?.id,
    intl,
    notify,
    loadPageSamples,
    onProgressUpdate,
  ]);

  // Filter samples by status - more reliable than checking solventType field
  const unpreparedSamples = useMemo(
    () =>
      samples.filter(
        (s) => s.status === "PENDING" || s.status === "IN_PROGRESS",
      ),
    [samples],
  );
  const extractedCompletedSamples = useMemo(
    () => samples.filter((s) => s.status === "COMPLETED"),
    [samples],
  );

  // Kept for grid sections - IN_PROGRESS only for "pending completion" section
  const extractedInProgressSamples = useMemo(
    () => samples.filter((s) => s.status === "IN_PROGRESS"),
    [samples],
  );

  // Helper to render sample status - simple status display matching API response
  const renderStatus = (sample) => {
    const status = sample.status || "PENDING";

    switch (status.toUpperCase()) {
      case "COMPLETED":
        return (
          <Tag type="green" size="sm" renderIcon={CheckmarkFilled}>
            <FormattedMessage
              id="notebook.tradmed.status.completed"
              defaultMessage="Completed"
            />
          </Tag>
        );
      case "IN_PROGRESS":
        return (
          <Tag type="blue" size="sm" renderIcon={Archive}>
            <FormattedMessage
              id="notebook.tradmed.status.inProgress"
              defaultMessage="In Progress"
            />
          </Tag>
        );
      case "SKIPPED":
        return (
          <Tag type="gray" size="sm">
            <FormattedMessage
              id="notebook.tradmed.status.skipped"
              defaultMessage="Skipped"
            />
          </Tag>
        );
      default:
        return (
          <Tag type="gray" size="sm" renderIcon={Pending}>
            <FormattedMessage
              id="notebook.tradmed.status.pending"
              defaultMessage="Pending"
            />
          </Tag>
        );
    }
  };

  // Helper to render Extract Quality Assessment status
  const renderExtractQcStatus = (sample) => {
    const qcResult = sample.data?.extractQuality?.qcResult || "PENDING";

    switch (qcResult.toUpperCase()) {
      case "PASS":
        return (
          <Tag type="green" size="sm" renderIcon={CheckmarkFilled}>
            <FormattedMessage
              id="notebook.page.tradmed.extraction.qc.status.pass"
              defaultMessage="Extract QC Pass"
            />
          </Tag>
        );
      case "FAIL":
        return (
          <Tag type="red" size="sm" renderIcon={WarningAltFilled}>
            <FormattedMessage
              id="notebook.page.tradmed.extraction.qc.status.fail"
              defaultMessage="Extract QC Fail"
            />
          </Tag>
        );
      default:
        return (
          <Tag type="gray" size="sm">
            <FormattedMessage
              id="notebook.page.tradmed.extraction.qc.status.pending"
              defaultMessage="Extract QC Pending"
            />
          </Tag>
        );
    }
  };

  return (
    <div className="tradmed-extraction-page">
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.page.tradmed.extraction.title"
            defaultMessage="Extraction, Filtration & Concentration"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.tradmed.extraction.description"
            defaultMessage="Extract plant material using various solvents and techniques, filter, and concentrate extracts with yield tracking."
          />
        </p>
      </div>

      <Grid fullWidth className="progress-section">
        <Column lg={16} md={8} sm={4}>
          <div className="progress-tiles">
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.tradmed.extraction.ready"
                  defaultMessage="Ready for Extraction"
                />
              </span>
              <span className="progress-value">{unpreparedSamples.length}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.tradmed.extraction.extracted"
                  defaultMessage="Extracted"
                />
              </span>
              <span className="progress-value">
                {extractedCompletedSamples.length}
              </span>
            </Tile>
          </div>
        </Column>
      </Grid>

      <div className="page-actions-bar">
        <Button
          kind="primary"
          size="sm"
          renderIcon={Edit}
          onClick={openModal}
          disabled={selectedSampleIds.length === 0 || !hasRealPageId}
        >
          <FormattedMessage
            id="notebook.page.tradmed.extraction.recordExtraction"
            defaultMessage="Record Extraction ({count})"
            values={{ count: selectedSampleIds.length }}
          />
        </Button>

        <Button
          kind="tertiary"
          size="sm"
          renderIcon={CheckmarkFilled}
          onClick={handleMarkComplete}
          disabled={
            selectedSampleIds.length === 0 || isCompleting || !hasRealPageId
          }
        >
          <FormattedMessage
            id="notebook.tradmed.extract.markComplete"
            defaultMessage="Mark Complete ({count})"
            values={{ count: selectedSampleIds.length }}
          />
        </Button>

        <Button
          kind="ghost"
          size="sm"
          renderIcon={Renew}
          onClick={loadPageSamples}
          disabled={loading}
        >
          <FormattedMessage
            id="notebook.page.tradmed.refresh"
            defaultMessage="Refresh"
          />
        </Button>
      </div>

      <div className="sample-table-section">
        <div className="table-section-header">
          <h5>
            <FormattedMessage
              id="notebook.page.tradmed.extraction.ready.title"
              defaultMessage="Samples Ready for Extraction"
            />
            <Tag type="blue" size="sm" className="count-tag">
              {unpreparedSamples.length}
            </Tag>
          </h5>
        </div>
        <div className="sample-grid-container">
          {!loading && unpreparedSamples.length === 0 ? (
            <div className="empty-table-state">
              <p>
                <FormattedMessage
                  id="notebook.page.tradmed.extraction.ready.empty"
                  defaultMessage="No samples ready for extraction."
                />
              </p>
            </div>
          ) : (
            <SampleGrid
              gridId="ready-extraction"
              samples={unpreparedSamples}
              selectedIds={selectedSampleIds}
              onSelectionChange={setSelectedSampleIds}
              showSelection={true}
              loading={loading}
              columns={[
                { key: "accessionNumber", header: "Accession #" },
                { key: "externalId", header: "Sample ID" },
                { key: "localName", header: "Local Name" },
                { key: "scientificName", header: "Scientific Name" },
                { key: "sampleCategory", header: "Category" },
                { key: "plantPart", header: "Plant Part" },
                { key: "collectionDate", header: "Collection Date" },
                { key: "intendedUse", header: "Intended Use" },
                { key: "solventType", header: "Solvent" },
                { key: "extractionTechnique", header: "Technique" },
                { key: "filtrationMethod", header: "Filtration" },
                { key: "concentrationMethod", header: "Concentration" },
                {
                  key: "status",
                  header: intl.formatMessage({
                    id: "notebook.tradmed.column.status",
                    defaultMessage: "Status",
                  }),
                  render: (_value, sample) => renderStatus(sample),
                },
                {
                  key: "extractQuality",
                  header: intl.formatMessage({
                    id: "notebook.page.tradmed.extraction.column.qc",
                    defaultMessage: "Extract QC",
                  }),
                  render: (_value, sample) => renderExtractQcStatus(sample),
                },
              ]}
            />
          )}
        </div>
      </div>

      {/* Extracted Samples Section - IN PROGRESS (with checkboxes) */}
      <div className="sample-table-section">
        <div className="table-section-header">
          <h5>
            <FormattedMessage
              id="notebook.page.tradmed.extraction.extracted.inProgress.title"
              defaultMessage="Extracted (Pending Completion)"
            />
            <Tag type="blue" size="sm" className="count-tag">
              {extractedInProgressSamples.length}
            </Tag>
          </h5>
        </div>
        <div className="sample-grid-container">
          {!loading && extractedInProgressSamples.length === 0 ? (
            <div className="empty-table-state">
              <p>
                <FormattedMessage
                  id="notebook.page.tradmed.extraction.extracted.empty"
                  defaultMessage="No samples extracted yet."
                />
              </p>
            </div>
          ) : (
            <SampleGrid
              gridId="extracted-in-progress-samples"
              samples={extractedInProgressSamples}
              selectedIds={selectedSampleIds}
              onSelectionChange={setSelectedSampleIds}
              loading={loading}
              columns={[
                { key: "accessionNumber", header: "Accession #" },
                { key: "externalId", header: "Sample ID" },
                { key: "localName", header: "Local Name" },
                { key: "scientificName", header: "Scientific Name" },
                { key: "sampleCategory", header: "Category" },
                { key: "plantPart", header: "Plant Part" },
                { key: "solventType", header: "Solvent" },
                { key: "extractionTechnique", header: "Technique" },
                { key: "filtrationMethod", header: "Filtration" },
                { key: "concentrationMethod", header: "Concentration" },
                { key: "extractWeight", header: "Extract Weight (g)" },
                { key: "yieldPercent", header: "Yield %" },
                { key: "collectionDate", header: "Collection Date" },
                { key: "intendedUse", header: "Intended Use" },
                {
                  key: "status",
                  header: intl.formatMessage({
                    id: "notebook.tradmed.column.status",
                    defaultMessage: "Status",
                  }),
                  render: (_value, sample) => renderStatus(sample),
                },
                {
                  key: "extractQuality",
                  header: intl.formatMessage({
                    id: "notebook.page.tradmed.extraction.column.qc",
                    defaultMessage: "Extract QC",
                  }),
                  render: (_value, sample) => renderExtractQcStatus(sample),
                },
              ]}
            />
          )}
        </div>
      </div>

      {/* Extracted Samples Section - COMPLETED (without checkboxes) */}
      {extractedCompletedSamples.length > 0 && (
        <div className="sample-table-section">
          <div className="table-section-header">
            <h5>
              <FormattedMessage
                id="notebook.page.tradmed.extraction.extracted.completed.title"
                defaultMessage="Extraction Completed"
              />
              <Tag type="green" size="sm" className="count-tag">
                {extractedCompletedSamples.length}
              </Tag>
            </h5>
          </div>
          <div className="sample-grid-container">
            <SampleGrid
              gridId="extracted-completed-samples"
              samples={extractedCompletedSamples}
              showSelection={false}
              loading={loading}
              columns={[
                { key: "accessionNumber", header: "Accession #" },
                { key: "externalId", header: "Sample ID" },
                { key: "localName", header: "Local Name" },
                { key: "scientificName", header: "Scientific Name" },
                { key: "sampleCategory", header: "Category" },
                { key: "plantPart", header: "Plant Part" },
                { key: "solventType", header: "Solvent" },
                { key: "extractionTechnique", header: "Technique" },
                { key: "filtrationMethod", header: "Filtration" },
                { key: "concentrationMethod", header: "Concentration" },
                { key: "extractWeight", header: "Extract Weight (g)" },
                { key: "yieldPercent", header: "Yield %" },
                { key: "collectionDate", header: "Collection Date" },
                { key: "intendedUse", header: "Intended Use" },
                {
                  key: "status",
                  header: intl.formatMessage({
                    id: "notebook.tradmed.column.status",
                    defaultMessage: "Status",
                  }),
                  render: (_value, sample) => renderStatus(sample),
                },
                {
                  key: "extractQuality",
                  header: intl.formatMessage({
                    id: "notebook.page.tradmed.extraction.column.qc",
                    defaultMessage: "Extract QC",
                  }),
                  render: (_value, sample) => renderExtractQcStatus(sample),
                },
              ]}
            />
          </div>
        </div>
      )}

      <Modal
        open={extractionModalOpen}
        onRequestClose={() => setExtractionModalOpen(false)}
        onRequestSubmit={applyExtraction}
        modalHeading={intl.formatMessage({
          id: "notebook.page.tradmed.extraction.modal.title",
          defaultMessage: "Record Extraction Process",
        })}
        primaryButtonText={
          isApplyingExtraction
            ? intl.formatMessage({
                id: "label.recording",
                defaultMessage: "Recording...",
              })
            : intl.formatMessage({
                id: "notebook.page.tradmed.extraction.modal.record",
                defaultMessage: "Record Extraction",
              })
        }
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        primaryButtonDisabled={isApplyingExtraction}
        size="lg"
      >
        {isApplyingExtraction && <Loading withOverlay={false} small />}

        <Grid narrow>
          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <Dropdown
              id="solvent"
              titleText={intl.formatMessage({
                id: "notebook.page.tradmed.extraction.modal.solvent",
                defaultMessage: "Solvent *",
              })}
              label="Select..."
              items={solventOptions}
              itemToString={(item) => (item ? item.label : "")}
              selectedItem={solventType}
              onChange={({ selectedItem }) => setSolventType(selectedItem)}
            />
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <Dropdown
              id="technique"
              titleText={intl.formatMessage({
                id: "notebook.page.tradmed.extraction.modal.technique",
                defaultMessage: "Technique *",
              })}
              label="Select..."
              items={techniqueOptions}
              itemToString={(item) => (item ? item.label : "")}
              selectedItem={extractionTechnique}
              onChange={({ selectedItem }) =>
                setExtractionTechnique(selectedItem)
              }
            />
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <NumberInput
              id="plant-weight"
              label={intl.formatMessage({
                id: "notebook.page.tradmed.extraction.modal.plantWeight",
                defaultMessage: "Plant Material Weight (g)",
              })}
              value={plantMaterialWeight}
              onChange={(e) =>
                setPlantMaterialWeight(
                  e.imaginaryTarget?.value || e.target?.value || "",
                )
              }
              step={0.1}
            />
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <NumberInput
              id="solvent-vol"
              label={intl.formatMessage({
                id: "notebook.page.tradmed.extraction.modal.solventVolume",
                defaultMessage: "Solvent Volume (mL)",
              })}
              value={solventVolume}
              onChange={(e) =>
                setSolventVolume(
                  e.imaginaryTarget?.value || e.target?.value || "",
                )
              }
              step={1}
            />
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <NumberInput
              id="ext-temp"
              label={intl.formatMessage({
                id: "notebook.page.tradmed.extraction.modal.temp",
                defaultMessage: "Temperature (°C)",
              })}
              value={extractionTemp}
              onChange={(e) =>
                setExtractionTemp(
                  e.imaginaryTarget?.value || e.target?.value || "",
                )
              }
              step={1}
            />
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <TextInput
              id="ext-duration"
              labelText={intl.formatMessage({
                id: "notebook.page.tradmed.extraction.modal.duration",
                defaultMessage: "Duration (hours)",
              })}
              value={extractionDuration}
              onChange={(e) => setExtractionDuration(e.target.value)}
            />
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <Dropdown
              id="filtration"
              titleText={intl.formatMessage({
                id: "notebook.page.tradmed.extraction.modal.filtration",
                defaultMessage: "Filtration Method",
              })}
              label="Select..."
              items={filtrationOptions}
              itemToString={(item) => (item ? item.label : "")}
              selectedItem={filtrationMethod}
              onChange={({ selectedItem }) => setFiltrationMethod(selectedItem)}
            />
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <Dropdown
              id="concentration"
              titleText={intl.formatMessage({
                id: "notebook.page.tradmed.extraction.modal.concentration",
                defaultMessage: "Concentration Method",
              })}
              label="Select..."
              items={concentrationOptions}
              itemToString={(item) => (item ? item.label : "")}
              selectedItem={concentrationMethod}
              onChange={({ selectedItem }) =>
                setConcentrationMethod(selectedItem)
              }
            />
          </Column>

          <Column lg={8} md={8} sm={4} style={{ marginBottom: "1rem" }}>
            <NumberInput
              id="extract-weight"
              label={intl.formatMessage({
                id: "notebook.page.tradmed.extraction.modal.extractWeight",
                defaultMessage: "Extract Weight (g)",
              })}
              value={extractWeight}
              onChange={(e) =>
                setExtractWeight(
                  e.imaginaryTarget?.value || e.target?.value || "",
                )
              }
              step={0.1}
            />
          </Column>

          <Column lg={16} md={16} sm={4} style={{ marginBottom: "1rem" }}>
            <TextArea
              id="extract-notes"
              labelText={intl.formatMessage({
                id: "notebook.page.tradmed.extraction.modal.notes",
                defaultMessage: "Extraction Notes",
              })}
              value={extractNotes}
              onChange={(e) => setExtractNotes(e.target.value)}
              rows={2}
            />
          </Column>

          {/* Extract Quality Assessment - SRS Section 5: Extraction QC */}
          <Column lg={16} md={16} sm={4} style={{ marginBottom: "1rem" }}>
            <div
              style={{
                padding: "1rem",
                backgroundColor: "var(--cds-layer-01)",
                borderRadius: "4px",
                marginBottom: "0.5rem",
              }}
            >
              <h5 style={{ margin: "0 0 0.5rem 0", fontSize: "0.95rem" }}>
                <FormattedMessage
                  id="notebook.page.tradmed.extraction.qc.title"
                  defaultMessage="Extract Quality Assessment"
                />
              </h5>
              <p
                style={{
                  margin: "0",
                  fontSize: "0.75rem",
                  color: "var(--cds-text-secondary)",
                }}
              >
                <FormattedMessage
                  id="notebook.page.tradmed.extraction.qc.description"
                  defaultMessage="Assess extract quality: color, odor, consistency, and contamination"
                />
              </p>
            </div>
          </Column>

          {/* Extract Color Dropdown */}
          <Column lg={8} md={4} sm={2} style={{ marginBottom: "1rem" }}>
            <Dropdown
              id="extractColor"
              titleText={intl.formatMessage({
                id: "notebook.page.tradmed.extraction.qc.color",
                defaultMessage: "Extract Color",
              })}
              label="Select color..."
              items={[
                { id: "clear", text: "Clear" },
                { id: "pale_yellow", text: "Pale Yellow" },
                { id: "yellow", text: "Yellow" },
                { id: "brown", text: "Brown" },
                { id: "dark_brown", text: "Dark Brown" },
                { id: "black", text: "Black" },
                { id: "green", text: "Green" },
                { id: "red", text: "Red" },
                { id: "other", text: "Other" },
              ]}
              itemToString={(item) => (item ? item.text : "")}
              selectedItem={
                extractColor
                  ? [
                      { id: "clear", text: "Clear" },
                      { id: "pale_yellow", text: "Pale Yellow" },
                      { id: "yellow", text: "Yellow" },
                      { id: "brown", text: "Brown" },
                      { id: "dark_brown", text: "Dark Brown" },
                      { id: "black", text: "Black" },
                      { id: "green", text: "Green" },
                      { id: "red", text: "Red" },
                      { id: "other", text: "Other" },
                    ].find((i) => i.id === extractColor) || null
                  : null
              }
              onChange={({ selectedItem }) => {
                if (selectedItem) {
                  setExtractColor(selectedItem.id);
                }
              }}
            />
          </Column>

          {/* Extract Odor Dropdown */}
          <Column lg={8} md={4} sm={2} style={{ marginBottom: "1rem" }}>
            <Dropdown
              id="extractOdor"
              titleText={intl.formatMessage({
                id: "notebook.page.tradmed.extraction.qc.odor",
                defaultMessage: "Extract Odor",
              })}
              label="Select odor..."
              items={[
                { id: "no_odor", text: "No Odor" },
                { id: "mild", text: "Mild" },
                { id: "aromatic", text: "Aromatic" },
                { id: "strong", text: "Strong" },
                { id: "unpleasant", text: "Unpleasant" },
                { id: "musty", text: "Musty (Indicates Contamination)" },
              ]}
              itemToString={(item) => (item ? item.text : "")}
              selectedItem={
                extractOdor
                  ? [
                      { id: "no_odor", text: "No Odor" },
                      { id: "mild", text: "Mild" },
                      { id: "aromatic", text: "Aromatic" },
                      { id: "strong", text: "Strong" },
                      { id: "unpleasant", text: "Unpleasant" },
                      { id: "musty", text: "Musty (Indicates Contamination)" },
                    ].find((i) => i.id === extractOdor) || null
                  : null
              }
              onChange={({ selectedItem }) => {
                if (selectedItem) {
                  setExtractOdor(selectedItem.id);
                }
              }}
            />
          </Column>

          {/* Extract Consistency Dropdown */}
          <Column lg={8} md={4} sm={2} style={{ marginBottom: "1rem" }}>
            <Dropdown
              id="extractConsistency"
              titleText={intl.formatMessage({
                id: "notebook.page.tradmed.extraction.qc.consistency",
                defaultMessage: "Extract Consistency",
              })}
              label="Select consistency..."
              items={[
                { id: "liquid", text: "Liquid" },
                { id: "syrup", text: "Syrupy" },
                { id: "paste", text: "Paste" },
                { id: "powder", text: "Powder" },
                { id: "solid", text: "Solid/Cake" },
              ]}
              itemToString={(item) => (item ? item.text : "")}
              selectedItem={
                extractConsistency
                  ? [
                      { id: "liquid", text: "Liquid" },
                      { id: "syrup", text: "Syrupy" },
                      { id: "paste", text: "Paste" },
                      { id: "powder", text: "Powder" },
                      { id: "solid", text: "Solid/Cake" },
                    ].find((i) => i.id === extractConsistency) || null
                  : null
              }
              onChange={({ selectedItem }) => {
                if (selectedItem) {
                  setExtractConsistency(selectedItem.id);
                }
              }}
            />
          </Column>

          {/* Contamination-Free Checkbox */}
          <Column lg={8} md={4} sm={2} style={{ marginBottom: "1rem" }}>
            <div
              style={{
                display: "flex",
                alignItems: "center",
                gap: "0.5rem",
                marginTop: "1.5rem",
              }}
            >
              <input
                type="checkbox"
                id="extractContaminationFree"
                checked={extractContaminationFree}
                onChange={(e) => setExtractContaminationFree(e.target.checked)}
              />
              <label
                htmlFor="extractContaminationFree"
                style={{ margin: "0", fontSize: "0.875rem" }}
              >
                <FormattedMessage
                  id="notebook.page.tradmed.extraction.qc.contamination_free"
                  defaultMessage="Free from contamination"
                />
              </label>
            </div>
          </Column>
        </Grid>
      </Modal>
    </div>
  );
}

export default TraditionalMedicineExtractionPage;
