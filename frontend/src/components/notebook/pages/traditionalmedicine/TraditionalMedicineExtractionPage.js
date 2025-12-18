import React, { useState, useEffect, useRef, useCallback } from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  InlineNotification,
  Modal,
  Select,
  SelectItem,
  TextArea,
  TextInput,
  Checkbox,
  Tag,
  NumberInput,
} from "@carbon/react";
import { Checkmark, Chemistry, Add, View } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import SampleGrid from "../../workflow/SampleGrid";
import "../../workflow/NotebookWorkflow.css";

/**
 * TraditionalMedicineExtractionPage - Page 4 of the Traditional Medicine workflow.
 *
 * Per SRS Requirements - Extraction, Filtration & Concentration:
 * - Extraction Process: Use of solvents (ethanol, methanol, water) based on target compounds
 * - Techniques: maceration, Soxhlet, ultrasonic, distillation, etc.
 * - Filtration: Remove plant debris and impurities
 * - Concentration: Evaporation or distillation to reduce volume and enrich extract
 *
 * Features:
 * - Solvent ratio tracking (material weight : solvent volume)
 * - Real-time yield calculation
 * - Temperature and duration logging
 * - Multiple extraction cycles support
 */
function TraditionalMedicineExtractionPage({
  entryId,
  notebookId,
  pageData,
  progress,
  onProgressUpdate,
}) {
  const intl = useIntl();
  const componentMounted = useRef(false);

  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);

  // Extraction options from backend
  const [extractionOptions, setExtractionOptions] = useState({
    solvents: [],
    techniques: [],
    filtrationMethods: [],
    concentrationMethods: [],
  });

  // Extraction modal state
  const [extractionModalOpen, setExtractionModalOpen] = useState(false);
  const [isApplying, setIsApplying] = useState(false);
  const [extractionValues, setExtractionValues] = useState({
    // Extraction parameters
    solvent: "",
    otherSolvent: "",
    solventConcentration: "",
    extractionTechnique: "",
    otherTechnique: "",
    extractionDate: new Date().toISOString().slice(0, 10),
    operator: "",
    // Solvent ratio tracking
    materialWeight: "",
    materialWeightUnit: "g",
    solventVolume: "",
    solventVolumeUnit: "mL",
    // Extraction conditions
    extractionTemperature: "",
    temperatureUnit: "C",
    extractionDurationMinutes: "",
    numberOfCycles: 1,
    // Filtration
    filtrationMethod: "",
    filterPoreSize: "",
    debrisRemoved: false,
    // Concentration
    concentrationMethod: "",
    concentrationTemperature: "",
    finalVolume: "",
    finalVolumeUnit: "mL",
    // Extract output
    extractId: "",
    extractWeight: "",
    extractWeightUnit: "g",
    extractAppearance: "",
    extractColor: "",
    notes: "",
  });

  // Calculated values
  const [calculatedRatio, setCalculatedRatio] = useState(null);
  const [calculatedYield, setCalculatedYield] = useState(null);

  // Aliquoting modal state
  const [aliquotModalOpen, setAliquotModalOpen] = useState(false);
  const [aliquotCount, setAliquotCount] = useState(2);
  const [aliquotPrefix, setAliquotPrefix] = useState("TM-ALQ");
  const [creatingAliquots, setCreatingAliquots] = useState(false);

  // View children modal state
  const [viewChildrenModalOpen, setViewChildrenModalOpen] = useState(false);
  const [selectedParentForView, setSelectedParentForView] = useState(null);
  const [childSamples, setChildSamples] = useState([]);
  const [loadingChildren, setLoadingChildren] = useState(false);

  useEffect(() => {
    componentMounted.current = true;
    loadPageSamples();
    loadExtractionOptions();
    return () => {
      componentMounted.current = false;
    };
  }, [entryId, pageData?.id]);

  // Calculate solvent ratio and yield when values change
  useEffect(() => {
    const materialWeight = parseFloat(extractionValues.materialWeight);
    const solventVolume = parseFloat(extractionValues.solventVolume);
    const extractWeight = parseFloat(extractionValues.extractWeight);

    // Calculate solvent ratio (1:X)
    if (materialWeight > 0 && solventVolume > 0) {
      const ratio = solventVolume / materialWeight;
      setCalculatedRatio(`1:${ratio.toFixed(1)}`);
    } else {
      setCalculatedRatio(null);
    }

    // Calculate yield percentage
    if (materialWeight > 0 && extractWeight > 0) {
      const yieldPct = (extractWeight / materialWeight) * 100;
      setCalculatedYield(yieldPct.toFixed(2));
    } else {
      setCalculatedYield(null);
    }
  }, [
    extractionValues.materialWeight,
    extractionValues.solventVolume,
    extractionValues.extractWeight,
  ]);

  const loadExtractionOptions = useCallback(() => {
    getFromOpenElisServer(
      `/rest/notebook/tradmed/extraction/options`,
      (response) => {
        if (componentMounted.current && response) {
          setExtractionOptions({
            solvents: response.solvents || [],
            techniques: response.techniques || [],
            filtrationMethods: response.filtrationMethods || [],
            concentrationMethods: response.concentrationMethods || [],
          });
        }
      },
    );
  }, []);

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
          if (response && Array.isArray(response)) {
            const transformedSamples = response.map((sample) => ({
              id: String(sample.id || sample.sampleItemId),
              externalId: sample.externalId,
              accessionNumber: sample.accessionNumber,
              status: sample.pageStatus || sample.status || "PENDING",
              localName: sample.data?.localName,
              solvent: sample.data?.solvent,
              solventLabel: sample.data?.solventLabel,
              extractionTechnique: sample.data?.extractionTechnique,
              techniqueLabel: sample.data?.techniqueLabel,
              materialWeight: sample.data?.materialWeight,
              solventVolume: sample.data?.solventVolume,
              solventRatio: sample.data?.solventRatio,
              extractWeight: sample.data?.extractWeight,
              yieldPercentage: sample.data?.yieldPercentage,
              filtrationMethod: sample.data?.filtrationMethod,
              concentrationMethod: sample.data?.concentrationMethod,
              finalVolume: sample.data?.finalVolume,
              finalVolumeUnit: sample.data?.finalVolumeUnit,
              extractId: sample.data?.extractId,
              operator: sample.data?.operator,
              // Hierarchy information
              hasChildren: sample.hasChildren || false,
              childAliquotCount: sample.childAliquotCount || 0,
              isAliquot: sample.isAliquot || false,
              parentSampleItemId: sample.parentSampleItemId,
              parentExternalId: sample.parentExternalId,
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

  const hasRealPageId =
    pageData?.id && !String(pageData.id).startsWith("default-");

  const resetExtractionValues = () => {
    setExtractionValues({
      solvent: "",
      otherSolvent: "",
      solventConcentration: "",
      extractionTechnique: "",
      otherTechnique: "",
      extractionDate: new Date().toISOString().slice(0, 10),
      operator: "",
      materialWeight: "",
      materialWeightUnit: "g",
      solventVolume: "",
      solventVolumeUnit: "mL",
      extractionTemperature: "",
      temperatureUnit: "C",
      extractionDurationMinutes: "",
      numberOfCycles: 1,
      filtrationMethod: "",
      filterPoreSize: "",
      debrisRemoved: false,
      concentrationMethod: "",
      concentrationTemperature: "",
      finalVolume: "",
      finalVolumeUnit: "mL",
      extractId: "",
      extractWeight: "",
      extractWeightUnit: "g",
      extractAppearance: "",
      extractColor: "",
      notes: "",
    });
    setCalculatedRatio(null);
    setCalculatedYield(null);
  };

  const handleApplyExtraction = useCallback(() => {
    if (selectedSampleIds.length === 0 || !hasRealPageId) {
      setError(
        intl.formatMessage({
          id: "notebook.page.tradmed.error.noSelection",
          defaultMessage: "Please select at least one sample.",
        }),
      );
      return;
    }

    if (!extractionValues.solvent || !extractionValues.extractionTechnique) {
      setError(
        intl.formatMessage({
          id: "notebook.page.tradmed.extraction.error.required",
          defaultMessage: "Solvent and extraction technique are required.",
        }),
      );
      return;
    }

    setIsApplying(true);
    setError(null);

    const requestData = {
      sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
      solvent: extractionValues.solvent,
      otherSolvent: extractionValues.otherSolvent || null,
      solventConcentration: extractionValues.solventConcentration || null,
      extractionTechnique: extractionValues.extractionTechnique,
      otherTechnique: extractionValues.otherTechnique || null,
      extractionDate: extractionValues.extractionDate,
      operator: extractionValues.operator || null,
      materialWeight: extractionValues.materialWeight || null,
      materialWeightUnit: extractionValues.materialWeightUnit,
      solventVolume: extractionValues.solventVolume || null,
      solventVolumeUnit: extractionValues.solventVolumeUnit,
      extractionTemperature: extractionValues.extractionTemperature || null,
      temperatureUnit: extractionValues.temperatureUnit,
      extractionDurationMinutes: extractionValues.extractionDurationMinutes
        ? parseInt(extractionValues.extractionDurationMinutes, 10)
        : null,
      numberOfCycles: extractionValues.numberOfCycles || 1,
      filtrationMethod: extractionValues.filtrationMethod || null,
      filterPoreSize: extractionValues.filterPoreSize || null,
      debrisRemoved: extractionValues.debrisRemoved,
      concentrationMethod: extractionValues.concentrationMethod || null,
      concentrationTemperature:
        extractionValues.concentrationTemperature || null,
      finalVolume: extractionValues.finalVolume || null,
      finalVolumeUnit: extractionValues.finalVolumeUnit,
      extractId: extractionValues.extractId || null,
      extractWeight: extractionValues.extractWeight || null,
      extractWeightUnit: extractionValues.extractWeightUnit,
      extractAppearance: extractionValues.extractAppearance || null,
      extractColor: extractionValues.extractColor || null,
      notes: extractionValues.notes || null,
    };

    postToOpenElisServerJsonResponse(
      `/rest/notebook/tradmed/page/${pageData.id}/extract`,
      JSON.stringify(requestData),
      (response) => {
        setIsApplying(false);
        if (response && response.success) {
          let message = intl.formatMessage(
            {
              id: "notebook.page.tradmed.extraction.success",
              defaultMessage:
                "Applied extraction to {count} sample(s). Yield: {yield}%",
            },
            {
              count: response.updatedCount || selectedSampleIds.length,
              yield: response.averageYieldPercentage || "N/A",
            },
          );
          setSuccessMessage(message);
          setExtractionModalOpen(false);
          setSelectedSampleIds([]);
          loadPageSamples();
          if (onProgressUpdate) onProgressUpdate();
        } else {
          setError(
            response?.error ||
              intl.formatMessage({
                id: "notebook.page.tradmed.extraction.error.apply",
                defaultMessage: "Failed to apply extraction details.",
              }),
          );
        }
      },
    );
  }, [
    selectedSampleIds,
    extractionValues,
    hasRealPageId,
    intl,
    loadPageSamples,
    onProgressUpdate,
    pageData?.id,
  ]);

  const handleMarkComplete = useCallback(() => {
    if (selectedSampleIds.length === 0 || !hasRealPageId) return;

    const selectedSamples = samples.filter((s) =>
      selectedSampleIds.includes(s.id),
    );
    const missingExtraction = selectedSamples.filter((s) => !s.solvent);
    if (missingExtraction.length > 0) {
      setError(
        intl.formatMessage(
          {
            id: "notebook.page.tradmed.extraction.error.missing",
            defaultMessage:
              "{count} sample(s) missing extraction info. Apply extraction first.",
          },
          { count: missingExtraction.length },
        ),
      );
      return;
    }

    postToOpenElisServerJsonResponse(
      `/rest/notebook/tradmed/page/${pageData.id}/extract/complete`,
      JSON.stringify({
        sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
      }),
      (response) => {
        if (response && response.success) {
          setSuccessMessage(
            intl.formatMessage(
              {
                id: "notebook.page.tradmed.extraction.completed",
                defaultMessage:
                  "Marked {count} sample(s) as complete and ready for analysis.",
              },
              { count: response.completedCount || selectedSampleIds.length },
            ),
          );
          setSelectedSampleIds([]);
          loadPageSamples();
          if (onProgressUpdate) onProgressUpdate();
        } else {
          setError(
            response?.error ||
              intl.formatMessage({
                id: "notebook.page.tradmed.error.status",
                defaultMessage: "Failed to update status.",
              }),
          );
        }
      },
    );
  }, [
    selectedSampleIds,
    samples,
    hasRealPageId,
    intl,
    loadPageSamples,
    onProgressUpdate,
    pageData?.id,
  ]);

  // Handle opening aliquot modal
  const handleOpenAliquotModal = useCallback(() => {
    if (selectedSampleIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.page.tradmed.error.noSelection",
          defaultMessage: "Please select at least one sample.",
        }),
      );
      return;
    }
    // Check that selected samples have extraction data (are extracted)
    const selectedSamples = samples.filter((s) =>
      selectedSampleIds.includes(s.id),
    );
    const unextracted = selectedSamples.filter((s) => !s.solvent);
    if (unextracted.length > 0) {
      setError(
        intl.formatMessage(
          {
            id: "notebook.page.tradmed.error.unextractedAliquot",
            defaultMessage:
              "{count} sample(s) have not been extracted yet. Apply extraction first.",
          },
          { count: unextracted.length },
        ),
      );
      return;
    }
    setAliquotModalOpen(true);
  }, [selectedSampleIds, samples, intl]);

  // Handle create aliquots
  const handleCreateAliquots = useCallback(() => {
    if (!notebookId) {
      setError(
        intl.formatMessage({
          id: "notebook.page.tradmed.error.noNotebook",
          defaultMessage: "Notebook ID is required to create aliquots.",
        }),
      );
      return;
    }

    setCreatingAliquots(true);
    setError(null);

    postToOpenElisServerJsonResponse(
      `/rest/notebook/${notebookId}/samples/create-children`,
      JSON.stringify({
        parentSampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
        childCountPerParent: aliquotCount,
        externalIdPrefix: aliquotPrefix,
        pageId: pageData?.id,
      }),
      (response) => {
        setCreatingAliquots(false);
        setAliquotModalOpen(false);

        if (response && response.success) {
          setSuccessMessage(
            intl.formatMessage(
              {
                id: "notebook.page.tradmed.success.aliquotsCreated",
                defaultMessage:
                  "Created {count} aliquot(s). They will appear on subsequent pages.",
              },
              { count: response.createdCount },
            ),
          );
          setSelectedSampleIds([]);
          loadPageSamples();
          if (onProgressUpdate) onProgressUpdate();
        } else {
          setError(
            response?.error ||
              intl.formatMessage({
                id: "notebook.page.tradmed.error.aliquotFailed",
                defaultMessage: "Failed to create aliquots.",
              }),
          );
        }
      },
    );
  }, [
    notebookId,
    selectedSampleIds,
    aliquotCount,
    aliquotPrefix,
    pageData?.id,
    intl,
    loadPageSamples,
    onProgressUpdate,
  ]);

  // Handle view children for a sample
  const handleViewChildren = useCallback((parentSampleId) => {
    setSelectedParentForView(parentSampleId);
    setViewChildrenModalOpen(true);
    setLoadingChildren(true);

    getFromOpenElisServer(
      `/rest/notebook/samples/${parentSampleId}/children`,
      (response) => {
        setLoadingChildren(false);
        if (response && Array.isArray(response)) {
          setChildSamples(response);
        } else {
          setChildSamples([]);
        }
      },
    );
  }, []);

  const extractedCount = samples.filter((s) => s.solvent).length;
  const pendingCount = samples.filter((s) => !s.solvent).length;
  const completedCount = samples.filter((s) => s.status === "COMPLETED").length;

  const renderExtractionTags = (sample) => {
    if (!sample.solvent) return <Tag type="gray">Pending</Tag>;

    return (
      <div style={{ display: "flex", gap: "4px", flexWrap: "wrap" }}>
        <Tag type="cyan" size="sm">
          {sample.solventLabel || sample.solvent}
        </Tag>
        {sample.techniqueLabel && (
          <Tag type="teal" size="sm">
            {sample.techniqueLabel}
          </Tag>
        )}
        {sample.solventRatio && (
          <Tag type="blue" size="sm">
            {sample.solventRatio}
          </Tag>
        )}
        {sample.yieldPercentage && (
          <Tag type="green" size="sm">
            {sample.yieldPercentage}% yield
          </Tag>
        )}
        {sample.finalVolume && (
          <Tag type="purple" size="sm">
            {sample.finalVolume} {sample.finalVolumeUnit || "mL"}
          </Tag>
        )}
      </div>
    );
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
            defaultMessage="Extract active compounds using appropriate solvents and techniques. Track solvent ratios and calculate yield."
          />
        </p>
      </div>

      <Grid fullWidth className="progress-section">
        <Column lg={16} md={8} sm={4}>
          <div className="progress-tiles">
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.tradmed.totalSamples"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{samples.length}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.tradmed.extraction.extracted"
                  defaultMessage="Extracted"
                />
              </span>
              <span className="progress-value">{extractedCount}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.tradmed.extraction.pending"
                  defaultMessage="Pending"
                />
              </span>
              <span className="progress-value">{pendingCount}</span>
            </Tile>
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.tradmed.extraction.ready"
                  defaultMessage="Ready for Analysis"
                />
              </span>
              <span className="progress-value">{completedCount}</span>
            </Tile>
          </div>
        </Column>
      </Grid>

      <div className="page-actions-bar">
        <Button
          kind="primary"
          size="sm"
          renderIcon={Chemistry}
          onClick={() => {
            resetExtractionValues();
            setExtractionModalOpen(true);
          }}
          disabled={selectedSampleIds.length === 0}
        >
          <FormattedMessage
            id="notebook.page.tradmed.extraction.apply"
            defaultMessage="Apply Extraction ({count})"
            values={{ count: selectedSampleIds.length }}
          />
        </Button>
        {selectedSampleIds.length > 0 && (
          <>
            <Button
              kind="tertiary"
              size="sm"
              renderIcon={Add}
              onClick={handleOpenAliquotModal}
            >
              <FormattedMessage
                id="notebook.page.tradmed.extraction.createAliquots"
                defaultMessage="Create Aliquots ({count})"
                values={{ count: selectedSampleIds.length }}
              />
            </Button>
            <Button
              kind="secondary"
              size="sm"
              renderIcon={Checkmark}
              onClick={handleMarkComplete}
            >
              <FormattedMessage
                id="notebook.page.tradmed.extraction.markComplete"
                defaultMessage="Mark Complete ({count})"
                values={{ count: selectedSampleIds.length }}
              />
            </Button>
          </>
        )}
      </div>

      {error && (
        <InlineNotification
          kind="error"
          title={error}
          onClose={() => setError(null)}
          lowContrast
        />
      )}
      {successMessage && (
        <InlineNotification
          kind="success"
          title={successMessage}
          onClose={() => setSuccessMessage(null)}
          lowContrast
        />
      )}

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
            { key: "localName", header: "Local Name" },
            {
              key: "extraction",
              header: "Extraction Info",
              render: (_, sample) => renderExtractionTags(sample),
            },
            {
              key: "aliquots",
              header: "Aliquots",
              render: (_, sample) => {
                if (sample.hasChildren && sample.childAliquotCount > 0) {
                  return (
                    <Button
                      kind="ghost"
                      size="sm"
                      renderIcon={View}
                      onClick={(e) => {
                        e.stopPropagation();
                        handleViewChildren(sample.id);
                      }}
                    >
                      {sample.childAliquotCount}
                    </Button>
                  );
                }
                if (sample.isAliquot && sample.parentExternalId) {
                  return (
                    <Tag type="purple" size="sm">
                      from {sample.parentExternalId}
                    </Tag>
                  );
                }
                return "-";
              },
            },
            { key: "operator", header: "Operator" },
            { key: "status", header: "Status" },
          ]}
        />
      </div>

      {!loading && samples.length === 0 && (
        <div className="empty-state">
          <p>
            <FormattedMessage
              id="notebook.page.tradmed.extraction.empty"
              defaultMessage="No samples pending extraction. Complete sample preparation first."
            />
          </p>
        </div>
      )}

      {/* Extraction Modal */}
      <Modal
        open={extractionModalOpen}
        onRequestClose={() => setExtractionModalOpen(false)}
        modalHeading={intl.formatMessage({
          id: "notebook.tradmed.extraction.modal.title",
          defaultMessage: "Apply Extraction Details",
        })}
        primaryButtonText={isApplying ? "Applying..." : "Apply Extraction"}
        secondaryButtonText="Cancel"
        onRequestSubmit={handleApplyExtraction}
        onSecondarySubmit={() => setExtractionModalOpen(false)}
        size="lg"
        primaryButtonDisabled={isApplying}
      >
        <p className="modal-description">
          <FormattedMessage
            id="notebook.tradmed.extraction.modal.description"
            defaultMessage="Record extraction, filtration, and concentration details for {count} selected sample(s)."
            values={{ count: selectedSampleIds.length }}
          />
        </p>

        <Grid fullWidth>
          {/* Section: Extraction Process */}
          <Column lg={16} md={8} sm={4}>
            <h5 style={{ marginTop: "1rem", marginBottom: "0.5rem" }}>
              <FormattedMessage
                id="notebook.tradmed.extraction.section.extraction"
                defaultMessage="Extraction Process"
              />
            </h5>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <Select
              id="solvent"
              labelText="Solvent Used *"
              value={extractionValues.solvent}
              onChange={(e) =>
                setExtractionValues((prev) => ({
                  ...prev,
                  solvent: e.target.value,
                }))
              }
            >
              <SelectItem value="" text="Select solvent..." />
              {extractionOptions.solvents.map((opt) => (
                <SelectItem key={opt.id} value={opt.id} text={opt.label} />
              ))}
            </Select>
          </Column>
          {extractionValues.solvent === "OTHER" && (
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="otherSolvent"
                labelText="Specify Solvent"
                value={extractionValues.otherSolvent}
                onChange={(e) =>
                  setExtractionValues((prev) => ({
                    ...prev,
                    otherSolvent: e.target.value,
                  }))
                }
                placeholder="Enter solvent name"
              />
            </Column>
          )}
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="solventConcentration"
              labelText="Solvent Concentration"
              value={extractionValues.solventConcentration}
              onChange={(e) =>
                setExtractionValues((prev) => ({
                  ...prev,
                  solventConcentration: e.target.value,
                }))
              }
              placeholder="e.g., 70% or 95%"
            />
          </Column>
          <Column lg={8} md={4} sm={4}>
            <Select
              id="extractionTechnique"
              labelText="Extraction Technique *"
              value={extractionValues.extractionTechnique}
              onChange={(e) =>
                setExtractionValues((prev) => ({
                  ...prev,
                  extractionTechnique: e.target.value,
                }))
              }
            >
              <SelectItem value="" text="Select technique..." />
              {extractionOptions.techniques.map((opt) => (
                <SelectItem key={opt.id} value={opt.id} text={opt.label} />
              ))}
            </Select>
          </Column>
          {extractionValues.extractionTechnique === "OTHER" && (
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="otherTechnique"
                labelText="Specify Technique"
                value={extractionValues.otherTechnique}
                onChange={(e) =>
                  setExtractionValues((prev) => ({
                    ...prev,
                    otherTechnique: e.target.value,
                  }))
                }
                placeholder="Enter technique name"
              />
            </Column>
          )}
          <Column lg={8} md={4} sm={4}>
            <div className="cds--form-item">
              <label className="cds--label">Extraction Date</label>
              <input
                type="date"
                className="cds--text-input"
                value={extractionValues.extractionDate}
                onChange={(e) =>
                  setExtractionValues((prev) => ({
                    ...prev,
                    extractionDate: e.target.value,
                  }))
                }
              />
            </div>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="operator"
              labelText="Operator"
              value={extractionValues.operator}
              onChange={(e) =>
                setExtractionValues((prev) => ({
                  ...prev,
                  operator: e.target.value,
                }))
              }
              placeholder="Enter operator name"
            />
          </Column>

          {/* Section: Solvent Ratio */}
          <Column lg={16} md={8} sm={4}>
            <h5 style={{ marginTop: "1.5rem", marginBottom: "0.5rem" }}>
              <FormattedMessage
                id="notebook.tradmed.extraction.section.ratio"
                defaultMessage="Solvent Ratio"
              />
              {calculatedRatio && (
                <Tag type="blue" size="sm" style={{ marginLeft: "8px" }}>
                  Ratio: {calculatedRatio}
                </Tag>
              )}
            </h5>
          </Column>
          <Column lg={4} md={2} sm={2}>
            <TextInput
              id="materialWeight"
              labelText="Material Weight"
              value={extractionValues.materialWeight}
              onChange={(e) =>
                setExtractionValues((prev) => ({
                  ...prev,
                  materialWeight: e.target.value,
                }))
              }
              placeholder="e.g., 100"
            />
          </Column>
          <Column lg={4} md={2} sm={2}>
            <Select
              id="materialWeightUnit"
              labelText="Unit"
              value={extractionValues.materialWeightUnit}
              onChange={(e) =>
                setExtractionValues((prev) => ({
                  ...prev,
                  materialWeightUnit: e.target.value,
                }))
              }
            >
              <SelectItem value="g" text="g" />
              <SelectItem value="kg" text="kg" />
              <SelectItem value="mg" text="mg" />
            </Select>
          </Column>
          <Column lg={4} md={2} sm={2}>
            <TextInput
              id="solventVolume"
              labelText="Solvent Volume"
              value={extractionValues.solventVolume}
              onChange={(e) =>
                setExtractionValues((prev) => ({
                  ...prev,
                  solventVolume: e.target.value,
                }))
              }
              placeholder="e.g., 500"
            />
          </Column>
          <Column lg={4} md={2} sm={2}>
            <Select
              id="solventVolumeUnit"
              labelText="Unit"
              value={extractionValues.solventVolumeUnit}
              onChange={(e) =>
                setExtractionValues((prev) => ({
                  ...prev,
                  solventVolumeUnit: e.target.value,
                }))
              }
            >
              <SelectItem value="mL" text="mL" />
              <SelectItem value="L" text="L" />
            </Select>
          </Column>

          {/* Section: Extraction Conditions */}
          <Column lg={16} md={8} sm={4}>
            <h5 style={{ marginTop: "1.5rem", marginBottom: "0.5rem" }}>
              <FormattedMessage
                id="notebook.tradmed.extraction.section.conditions"
                defaultMessage="Extraction Conditions"
              />
            </h5>
          </Column>
          <Column lg={4} md={2} sm={2}>
            <TextInput
              id="extractionTemperature"
              labelText="Temperature"
              value={extractionValues.extractionTemperature}
              onChange={(e) =>
                setExtractionValues((prev) => ({
                  ...prev,
                  extractionTemperature: e.target.value,
                }))
              }
              placeholder="e.g., 25"
            />
          </Column>
          <Column lg={4} md={2} sm={2}>
            <Select
              id="temperatureUnit"
              labelText="Unit"
              value={extractionValues.temperatureUnit}
              onChange={(e) =>
                setExtractionValues((prev) => ({
                  ...prev,
                  temperatureUnit: e.target.value,
                }))
              }
            >
              <SelectItem value="C" text="°C" />
              <SelectItem value="F" text="°F" />
              <SelectItem value="K" text="K" />
            </Select>
          </Column>
          <Column lg={4} md={2} sm={2}>
            <TextInput
              id="extractionDurationMinutes"
              labelText="Duration (minutes)"
              value={extractionValues.extractionDurationMinutes}
              onChange={(e) =>
                setExtractionValues((prev) => ({
                  ...prev,
                  extractionDurationMinutes: e.target.value,
                }))
              }
              placeholder="e.g., 120"
            />
          </Column>
          <Column lg={4} md={2} sm={2}>
            <NumberInput
              id="numberOfCycles"
              label="Number of Cycles"
              value={extractionValues.numberOfCycles}
              min={1}
              max={20}
              onChange={(_, { value }) =>
                setExtractionValues((prev) => ({
                  ...prev,
                  numberOfCycles: value,
                }))
              }
            />
          </Column>

          {/* Section: Filtration */}
          <Column lg={16} md={8} sm={4}>
            <h5 style={{ marginTop: "1.5rem", marginBottom: "0.5rem" }}>
              <FormattedMessage
                id="notebook.tradmed.extraction.section.filtration"
                defaultMessage="Filtration"
              />
            </h5>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <Select
              id="filtrationMethod"
              labelText="Filtration Method"
              value={extractionValues.filtrationMethod}
              onChange={(e) =>
                setExtractionValues((prev) => ({
                  ...prev,
                  filtrationMethod: e.target.value,
                }))
              }
            >
              <SelectItem value="" text="Select method..." />
              {extractionOptions.filtrationMethods.map((opt) => (
                <SelectItem key={opt.id} value={opt.id} text={opt.label} />
              ))}
            </Select>
          </Column>
          <Column lg={4} md={2} sm={2}>
            <TextInput
              id="filterPoreSize"
              labelText="Filter Pore Size"
              value={extractionValues.filterPoreSize}
              onChange={(e) =>
                setExtractionValues((prev) => ({
                  ...prev,
                  filterPoreSize: e.target.value,
                }))
              }
              placeholder="e.g., 0.45 µm"
            />
          </Column>
          <Column lg={4} md={2} sm={2}>
            <div style={{ marginTop: "1.5rem" }}>
              <Checkbox
                id="debrisRemoved"
                labelText="Debris Removed"
                checked={extractionValues.debrisRemoved}
                onChange={(_, { checked }) =>
                  setExtractionValues((prev) => ({
                    ...prev,
                    debrisRemoved: checked,
                  }))
                }
              />
            </div>
          </Column>

          {/* Section: Concentration */}
          <Column lg={16} md={8} sm={4}>
            <h5 style={{ marginTop: "1.5rem", marginBottom: "0.5rem" }}>
              <FormattedMessage
                id="notebook.tradmed.extraction.section.concentration"
                defaultMessage="Concentration"
              />
            </h5>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <Select
              id="concentrationMethod"
              labelText="Concentration Method"
              value={extractionValues.concentrationMethod}
              onChange={(e) =>
                setExtractionValues((prev) => ({
                  ...prev,
                  concentrationMethod: e.target.value,
                }))
              }
            >
              <SelectItem value="" text="Select method..." />
              {extractionOptions.concentrationMethods.map((opt) => (
                <SelectItem key={opt.id} value={opt.id} text={opt.label} />
              ))}
            </Select>
          </Column>
          <Column lg={4} md={2} sm={2}>
            <TextInput
              id="concentrationTemperature"
              labelText="Concentration Temp"
              value={extractionValues.concentrationTemperature}
              onChange={(e) =>
                setExtractionValues((prev) => ({
                  ...prev,
                  concentrationTemperature: e.target.value,
                }))
              }
              placeholder="e.g., 40°C"
            />
          </Column>
          <Column lg={4} md={2} sm={2}>
            <TextInput
              id="finalVolume"
              labelText="Final Volume"
              value={extractionValues.finalVolume}
              onChange={(e) =>
                setExtractionValues((prev) => ({
                  ...prev,
                  finalVolume: e.target.value,
                }))
              }
              placeholder="e.g., 50"
            />
          </Column>
          <Column lg={4} md={2} sm={2}>
            <Select
              id="finalVolumeUnit"
              labelText="Unit"
              value={extractionValues.finalVolumeUnit}
              onChange={(e) =>
                setExtractionValues((prev) => ({
                  ...prev,
                  finalVolumeUnit: e.target.value,
                }))
              }
            >
              <SelectItem value="mL" text="mL" />
              <SelectItem value="L" text="L" />
              <SelectItem value="g" text="g (dry)" />
            </Select>
          </Column>

          {/* Section: Extract Output */}
          <Column lg={16} md={8} sm={4}>
            <h5 style={{ marginTop: "1.5rem", marginBottom: "0.5rem" }}>
              Extract Output
              {calculatedYield && (
                <Tag type="green" size="sm" style={{ marginLeft: "8px" }}>
                  Yield: {calculatedYield}%
                </Tag>
              )}
            </h5>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="extractId"
              labelText="Extract ID"
              value={extractionValues.extractId}
              onChange={(e) =>
                setExtractionValues((prev) => ({
                  ...prev,
                  extractId: e.target.value,
                }))
              }
              placeholder="e.g., TM-001-EXT-01"
            />
          </Column>
          <Column lg={4} md={2} sm={2}>
            <TextInput
              id="extractWeight"
              labelText="Extract Weight"
              value={extractionValues.extractWeight}
              onChange={(e) =>
                setExtractionValues((prev) => ({
                  ...prev,
                  extractWeight: e.target.value,
                }))
              }
              placeholder="e.g., 12.5"
            />
          </Column>
          <Column lg={4} md={2} sm={2}>
            <Select
              id="extractWeightUnit"
              labelText="Unit"
              value={extractionValues.extractWeightUnit}
              onChange={(e) =>
                setExtractionValues((prev) => ({
                  ...prev,
                  extractWeightUnit: e.target.value,
                }))
              }
            >
              <SelectItem value="g" text="g" />
              <SelectItem value="mg" text="mg" />
              <SelectItem value="kg" text="kg" />
            </Select>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="extractAppearance"
              labelText="Appearance"
              value={extractionValues.extractAppearance}
              onChange={(e) =>
                setExtractionValues((prev) => ({
                  ...prev,
                  extractAppearance: e.target.value,
                }))
              }
              placeholder="e.g., viscous liquid, powder"
            />
          </Column>
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="extractColor"
              labelText="Color"
              value={extractionValues.extractColor}
              onChange={(e) =>
                setExtractionValues((prev) => ({
                  ...prev,
                  extractColor: e.target.value,
                }))
              }
              placeholder="e.g., dark green, brown"
            />
          </Column>
          <Column lg={16} md={8} sm={4}>
            <TextArea
              id="notes"
              labelText="Notes"
              value={extractionValues.notes}
              onChange={(e) =>
                setExtractionValues((prev) => ({
                  ...prev,
                  notes: e.target.value,
                }))
              }
              placeholder="Additional notes about the extraction process..."
            />
          </Column>
        </Grid>
      </Modal>

      {/* Aliquot Creation Modal */}
      <Modal
        open={aliquotModalOpen}
        onRequestClose={() => setAliquotModalOpen(false)}
        modalHeading={intl.formatMessage({
          id: "notebook.tradmed.aliquot.modal.title",
          defaultMessage: "Create Aliquots",
        })}
        primaryButtonText={
          creatingAliquots
            ? intl.formatMessage({
                id: "common.creating",
                defaultMessage: "Creating...",
              })
            : intl.formatMessage({
                id: "notebook.tradmed.aliquot.create",
                defaultMessage: "Create Aliquots",
              })
        }
        secondaryButtonText={intl.formatMessage({
          id: "common.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestSubmit={handleCreateAliquots}
        onSecondarySubmit={() => setAliquotModalOpen(false)}
        size="sm"
        primaryButtonDisabled={creatingAliquots || aliquotCount < 1}
      >
        <p style={{ marginBottom: "1rem" }}>
          <FormattedMessage
            id="notebook.tradmed.aliquot.modal.description"
            defaultMessage="Create aliquots (child samples) from {count} selected extract(s). Aliquots will inherit extraction data and can be processed independently through the Analytical or Testing pathways."
            values={{ count: selectedSampleIds.length }}
          />
        </p>

        <NumberInput
          id="aliquotCount"
          label={intl.formatMessage({
            id: "notebook.tradmed.aliquot.count",
            defaultMessage: "Aliquots per Sample",
          })}
          value={aliquotCount}
          min={1}
          max={10}
          onChange={(_, { value }) => setAliquotCount(value)}
          helperText={intl.formatMessage({
            id: "notebook.tradmed.aliquot.countHelper",
            defaultMessage:
              "Number of aliquots to create from each parent sample (1-10)",
          })}
        />

        <TextInput
          id="aliquotPrefix"
          labelText={intl.formatMessage({
            id: "notebook.tradmed.aliquot.prefix",
            defaultMessage: "External ID Prefix",
          })}
          value={aliquotPrefix}
          onChange={(e) => setAliquotPrefix(e.target.value)}
          helperText={intl.formatMessage({
            id: "notebook.tradmed.aliquot.prefixHelper",
            defaultMessage:
              "Prefix for generated aliquot IDs (e.g., TM-ALQ-2024-0001)",
          })}
          style={{ marginTop: "1rem" }}
        />

        <div
          style={{
            marginTop: "1rem",
            padding: "0.75rem",
            backgroundColor: "#f4f4f4",
            borderRadius: "4px",
          }}
        >
          <strong>
            <FormattedMessage
              id="notebook.tradmed.aliquot.summary"
              defaultMessage="Summary"
            />
          </strong>
          <p style={{ marginTop: "0.5rem", fontSize: "0.875rem" }}>
            <FormattedMessage
              id="notebook.tradmed.aliquot.summaryText"
              defaultMessage="This will create {total} new aliquot(s) from {parents} parent sample(s)."
              values={{
                total: selectedSampleIds.length * aliquotCount,
                parents: selectedSampleIds.length,
              }}
            />
          </p>
        </div>
      </Modal>

      {/* View Children Modal */}
      <Modal
        open={viewChildrenModalOpen}
        onRequestClose={() => {
          setViewChildrenModalOpen(false);
          setChildSamples([]);
          setSelectedParentForView(null);
        }}
        modalHeading={intl.formatMessage({
          id: "notebook.tradmed.viewChildren.title",
          defaultMessage: "Aliquots (Child Samples)",
        })}
        passiveModal
        size="md"
      >
        {loadingChildren ? (
          <p>
            <FormattedMessage id="common.loading" defaultMessage="Loading..." />
          </p>
        ) : childSamples.length === 0 ? (
          <p>
            <FormattedMessage
              id="notebook.tradmed.viewChildren.none"
              defaultMessage="No aliquots found for this sample."
            />
          </p>
        ) : (
          <div>
            <p style={{ marginBottom: "1rem" }}>
              <FormattedMessage
                id="notebook.tradmed.viewChildren.count"
                defaultMessage="Found {count} aliquot(s):"
                values={{ count: childSamples.length }}
              />
            </p>
            <table style={{ width: "100%", borderCollapse: "collapse" }}>
              <thead>
                <tr style={{ borderBottom: "1px solid #ddd" }}>
                  <th style={{ textAlign: "left", padding: "8px" }}>
                    External ID
                  </th>
                  <th style={{ textAlign: "left", padding: "8px" }}>
                    Accession #
                  </th>
                  <th style={{ textAlign: "left", padding: "8px" }}>Created</th>
                </tr>
              </thead>
              <tbody>
                {childSamples.map((child) => (
                  <tr
                    key={child.id || child.sampleItemId}
                    style={{ borderBottom: "1px solid #eee" }}
                  >
                    <td style={{ padding: "8px" }}>
                      {child.externalId || "-"}
                    </td>
                    <td style={{ padding: "8px" }}>
                      {child.accessionNumber || "-"}
                    </td>
                    <td style={{ padding: "8px" }}>
                      {child.collectionDate || "-"}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Modal>
    </div>
  );
}

export default TraditionalMedicineExtractionPage;
