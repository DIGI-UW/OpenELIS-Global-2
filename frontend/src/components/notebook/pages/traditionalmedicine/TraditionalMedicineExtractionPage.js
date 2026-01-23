import React, { useState, useEffect, useRef, useCallback, useMemo, useContext } from "react";
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
  const { setNotificationVisible, addNotification } = useContext(NotificationContext);
  const componentMounted = useRef(false);
  const { hasAnyRole } = usePermissions();

  // TMMRD permissions per SRS Section 11
  const {
    getPagePermissionLevel,
    canSaveData,
    canAccessStage3to4,
  } = useTMMRDPermissions();

  // STAGE 4 allowed roles per TMMRD SRS Section 11 - Lab Technicians and Researchers
  const allowedRoles = [
    "Lab Technician",
    "Researcher",
    "Pharmacognosist",
    "Lab Manager",
    "Principal Investigator"
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
  const pagePermissionLevel = getPagePermissionLevel("Extraction & Concentration");
  const canEditData = canSaveData(pagePermissionLevel);

  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [loading, setLoading] = useState(true);

  const [extractionModalOpen, setExtractionModalOpen] = useState(false);
  const [isApplyingExtraction, setIsApplyingExtraction] = useState(false);

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

  const solventOptions = [
    { id: "ethanol", label: "Ethanol" },
    { id: "methanol", label: "Methanol" },
    { id: "water", label: "Water" },
    { id: "hexane", label: "Hexane" },
    { id: "chloroform", label: "Chloroform" },
    { id: "acetone", label: "Acetone" },
    { id: "other", label: "Other Solvent" },
  ];

  const techniqueOptions = [
    { id: "maceration", label: "Maceration" },
    { id: "soxhlet", label: "Soxhlet Extraction" },
    { id: "ultrasonic", label: "Ultrasonic Extraction" },
    { id: "distillation", label: "Distillation" },
    { id: "other", label: "Other Technique" },
  ];

  const filtrationOptions = [
    { id: "filter_paper", label: "Filter Paper" },
    { id: "vacuum", label: "Vacuum Filtration" },
    { id: "centrifugation", label: "Centrifugation" },
  ];

  const concentrationOptions = [
    { id: "rotary_evaporator", label: "Rotary Evaporator" },
    { id: "distillation", label: "Distillation" },
    { id: "no_concentration", label: "No Concentration" },
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
          setSamples(
            response && Array.isArray(response)
              ? response.map((s) => ({
                  id: String(s.id || s.sampleItemId),
                  externalId: s.externalId,
                  accessionNumber: s.accessionNumber,
                  status: s.pageStatus || s.status || "PENDING",
                  localName: s.data?.localName,
                  scientificName: s.data?.scientificName,
                  solventType: s.data?.solventType,
                  extractionTechnique: s.data?.extractionTechnique,
                  yieldPercent: s.data?.yieldPercent,
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
        ? (((parseFloat(extractWeight) / parseFloat(plantMaterialWeight)) *
            100).toFixed(2))
        : null;

    postToOpenElisServerJsonResponse(
      `/rest/notebook/tradmed/page/${pageData.id}/extraction`,
      JSON.stringify({
        sampleIds,
        solventType: solventType.id,
        solventTypeLabel: solventType.label,
        extractionTechnique: extractionTechnique.id,
        plantMaterialWeight,
        solventVolume,
        extractionTemp,
        extractionDuration,
        filtrationMethod: filtrationMethod?.id || null,
        concentrationMethod: concentrationMethod?.id || null,
        extractWeight,
        yieldPercent,
        extractNotes,
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
                notify({
                  kind: NotificationKinds.success,
                  title: response.message ||
                    intl.formatMessage(
                      {
                        id: "notebook.page.tradmed.extraction.success",
                        defaultMessage: "Extracted {count} sample(s).",
                      },
                      { count: response.updatedCount || selectedSampleIds.length },
                    ),
                });
                setExtractionModalOpen(false);
                setSelectedSampleIds([]);
                loadPageSamples();
                if (onProgressUpdate) onProgressUpdate();
              } else {
                notify({
                  kind: NotificationKinds.error,
                  title: intl.formatMessage({
                    id: "notebook.page.tradmed.error.statusUpdate",
                    defaultMessage: "Extraction completed but failed to update sample status.",
                  }),
                });
              }
            }
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
    hasRealPageId,
    pageData?.id,
    selectedSampleIds,
    intl,
    loadPageSamples,
    onProgressUpdate,
    notify,
  ]);

  const unpreparedSamples = useMemo(
    () => samples.filter((s) => !s.solventType),
    [samples],
  );
  const extractedSamples = useMemo(
    () =>
      samples.filter(
        (s) => s.solventType && s.status === "COMPLETED",
      ),
    [samples],
  );

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
              <span className="progress-value">{extractedSamples.length}</span>
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
                { key: "localName", header: "Local Name" },
                { key: "scientificName", header: "Scientific Name" },
              ]}
            />
          )}
        </div>
      </div>

      <div className="sample-table-section">
        <div className="table-section-header">
          <h5>
            <FormattedMessage
              id="notebook.page.tradmed.extraction.extracted.title"
              defaultMessage="Extracted Samples"
            />
            <Tag type="green" size="sm" className="count-tag">
              {extractedSamples.length}
            </Tag>
          </h5>
        </div>
        <div className="sample-grid-container">
          {!loading && extractedSamples.length === 0 ? (
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
              gridId="extracted-samples"
              samples={extractedSamples}
              showSelection={false}
              loading={loading}
              columns={[
                { key: "accessionNumber", header: "Accession #" },
                { key: "localName", header: "Local Name" },
                { key: "solventType", header: "Solvent" },
                { key: "extractionTechnique", header: "Technique" },
                { key: "extractWeight", header: "Extract Weight (g)" },
                { key: "yieldPercent", header: "Yield %" },
              ]}
            />
          )}
        </div>
      </div>

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

        <Grid fullWidth narrow>
          <Column lg={8} md={4} sm={4}>
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

          <Column lg={8} md={4} sm={4}>
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

          <Column lg={8} md={4} sm={4}>
            <NumberInput
              id="plant-weight"
              label={intl.formatMessage({
                id: "notebook.page.tradmed.extraction.modal.plantWeight",
                defaultMessage: "Plant Material Weight (g)",
              })}
              value={plantMaterialWeight}
              onChange={(e) => setPlantMaterialWeight(e.imaginaryTarget?.value || e.target?.value || "")}
              step={0.1}
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <NumberInput
              id="solvent-vol"
              label={intl.formatMessage({
                id: "notebook.page.tradmed.extraction.modal.solventVolume",
                defaultMessage: "Solvent Volume (mL)",
              })}
              value={solventVolume}
              onChange={(e) => setSolventVolume(e.imaginaryTarget?.value || e.target?.value || "")}
              step={1}
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <NumberInput
              id="ext-temp"
              label={intl.formatMessage({
                id: "notebook.page.tradmed.extraction.modal.temp",
                defaultMessage: "Temperature (°C)",
              })}
              value={extractionTemp}
              onChange={(e) => setExtractionTemp(e.imaginaryTarget?.value || e.target?.value || "")}
              step={1}
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
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

          <Column lg={8} md={4} sm={4}>
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

          <Column lg={8} md={4} sm={4}>
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

          <Column lg={8} md={4} sm={4}>
            <NumberInput
              id="extract-weight"
              label={intl.formatMessage({
                id: "notebook.page.tradmed.extraction.modal.extractWeight",
                defaultMessage: "Extract Weight (g)",
              })}
              value={extractWeight}
              onChange={(e) => setExtractWeight(e.imaginaryTarget?.value || e.target?.value || "")}
              step={0.1}
            />
          </Column>

          <Column lg={16} md={8} sm={4}>
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
        </Grid>
      </Modal>
    </div>
  );
}

export default TraditionalMedicineExtractionPage;
