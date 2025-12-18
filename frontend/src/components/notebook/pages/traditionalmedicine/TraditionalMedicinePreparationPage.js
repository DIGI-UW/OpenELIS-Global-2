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
import { Checkmark, Edit, Cut, Chemistry, Scale } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import SampleGrid from "../../workflow/SampleGrid";
import "../../workflow/NotebookWorkflow.css";

/**
 * TraditionalMedicinePreparationPage - Page 3 of the Traditional Medicine workflow.
 * Prepare raw material for extraction or analysis.
 *
 * Physical Processing Data Points (per SRS):
 * - Processing type: Grinding, Chopping, Drying, Powdering
 * - Fresh processing vs dried processing
 * - Processing date
 * - Processed by
 * - Yield tracking (initial weight → final weight → yield percentage)
 * - Drying-specific parameters (temperature, duration, method)
 * - Quality control (moisture content validation)
 * - Derived material linked to parent sample
 */
function TraditionalMedicinePreparationPage({
  entryId,
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

  // Processing modal state
  const [processingModalOpen, setProcessingModalOpen] = useState(false);
  const [isProcessing, setIsProcessing] = useState(false);
  const [processingValues, setProcessingValues] = useState({
    processingTypes: [],
    materialState: "",
    processingDate: new Date().toISOString().slice(0, 10),
    processedBy: "",
    equipment: "",
    particleSize: "",
    // Yield tracking
    initialWeight: "",
    finalWeight: "",
    weightUnit: "g",
    // Drying-specific
    dryingTemperature: "",
    temperatureUnit: "°C",
    dryingDurationHours: "",
    dryingMethod: "",
    // Quality control
    moistureContent: "",
    targetMoistureContent: "10",
    // Derived material
    derivedMaterialId: "",
    aliquotNotes: "",
    // Notes
    notes: "",
  });

  // Processing type options
  const processingTypeOptions = [
    { id: "GRINDING", label: "Grinding", isDrying: false },
    { id: "CHOPPING", label: "Chopping", isDrying: false },
    { id: "POWDERING", label: "Powdering", isDrying: false },
    { id: "SIEVING", label: "Sieving", isDrying: false },
    { id: "MILLING", label: "Milling", isDrying: false },
    { id: "DRYING", label: "Drying", isDrying: true },
    {
      id: "FREEZE_DRYING",
      label: "Freeze Drying (Lyophilization)",
      isDrying: true,
    },
    { id: "SHADE_DRYING", label: "Shade Drying", isDrying: true },
    { id: "OVEN_DRYING", label: "Oven Drying", isDrying: true },
    { id: "SUN_DRYING", label: "Sun Drying", isDrying: true },
    { id: "AIR_DRYING", label: "Air Drying", isDrying: true },
  ];

  // Material state options
  const materialStateOptions = [
    { id: "FRESH", label: "Fresh material (freshly collected)" },
    { id: "DRIED", label: "Dried material (pre-dried)" },
    { id: "PRESERVED", label: "Preserved material (stored)" },
    { id: "PARTIALLY_DRIED", label: "Partially dried (in process)" },
  ];

  useEffect(() => {
    componentMounted.current = true;
    loadPageSamples();
    return () => {
      componentMounted.current = false;
    };
  }, [entryId, pageData?.id]);

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
              plantPart: sample.data?.plantPart,
              sampleCondition: sample.data?.sampleCondition,
              processingTypes: sample.data?.processingTypes || [],
              processingTypeLabels: sample.data?.processingTypeLabels || [],
              materialState: sample.data?.materialState,
              materialStateLabel: sample.data?.materialStateLabel,
              processingDate: sample.data?.processingDate,
              processedBy: sample.data?.processedBy,
              equipment: sample.data?.equipment,
              particleSize: sample.data?.particleSize,
              // Yield tracking
              initialWeight: sample.data?.initialWeight,
              finalWeight: sample.data?.finalWeight,
              weightUnit: sample.data?.weightUnit || "g",
              yieldPercentage: sample.data?.yieldPercentage,
              // Drying-specific
              dryingTemperature: sample.data?.dryingTemperature,
              dryingDurationHours: sample.data?.dryingDurationHours,
              dryingMethod: sample.data?.dryingMethod,
              // QC
              moistureContent: sample.data?.moistureContent,
              moistureQCPassed: sample.data?.moistureQCPassed,
              // Derived material
              derivedMaterialId: sample.data?.derivedMaterialId,
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

  // Check if any selected processing type is a drying method
  const hasDryingSelected = processingValues.processingTypes.some((typeId) => {
    const type = processingTypeOptions.find((t) => t.id === typeId);
    return type?.isDrying;
  });

  // Calculate yield percentage in real-time
  const calculatedYield = (() => {
    const initial = parseFloat(processingValues.initialWeight);
    const final = parseFloat(processingValues.finalWeight);
    if (initial > 0 && final >= 0) {
      return ((final / initial) * 100).toFixed(1);
    }
    return null;
  })();

  // Reset processing form
  const resetProcessingValues = () => {
    setProcessingValues({
      processingTypes: [],
      materialState: "",
      processingDate: new Date().toISOString().slice(0, 10),
      processedBy: "",
      equipment: "",
      particleSize: "",
      initialWeight: "",
      finalWeight: "",
      weightUnit: "g",
      dryingTemperature: "",
      temperatureUnit: "°C",
      dryingDurationHours: "",
      dryingMethod: "",
      moistureContent: "",
      targetMoistureContent: "10",
      derivedMaterialId: "",
      aliquotNotes: "",
      notes: "",
    });
  };

  // Handle processing type checkbox change
  const handleProcessingTypeChange = (typeId, checked) => {
    setProcessingValues((prev) => ({
      ...prev,
      processingTypes: checked
        ? [...prev.processingTypes, typeId]
        : prev.processingTypes.filter((t) => t !== typeId),
    }));
  };

  // Handle bulk apply processing using the dedicated backend endpoint
  const handleApplyProcessing = useCallback(() => {
    if (selectedSampleIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.page.tradmed.error.noSelection",
          defaultMessage: "Please select at least one sample.",
        }),
      );
      return;
    }

    if (!hasRealPageId) {
      setError(
        intl.formatMessage({
          id: "notebook.page.tradmed.error.noPage",
          defaultMessage:
            "Cannot update samples: Page not properly initialized.",
        }),
      );
      return;
    }

    // Validate required fields
    if (processingValues.processingTypes.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.page.tradmed.prep.error.noType",
          defaultMessage: "Please select at least one processing type.",
        }),
      );
      return;
    }

    if (!processingValues.materialState) {
      setError(
        intl.formatMessage({
          id: "notebook.page.tradmed.prep.error.noState",
          defaultMessage: "Please select the material state.",
        }),
      );
      return;
    }

    setIsProcessing(true);
    setError(null);

    // Build request for the dedicated backend endpoint
    const requestData = {
      sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
      processingTypes: processingValues.processingTypes,
      materialState: processingValues.materialState,
      processingDate: processingValues.processingDate,
      processedBy: processingValues.processedBy || null,
      equipment: processingValues.equipment || null,
      particleSize: processingValues.particleSize || null,
      // Yield tracking
      initialWeight: processingValues.initialWeight || null,
      finalWeight: processingValues.finalWeight || null,
      weightUnit: processingValues.weightUnit,
      // Drying parameters (only if drying methods selected)
      dryingTemperature: hasDryingSelected
        ? processingValues.dryingTemperature || null
        : null,
      temperatureUnit: hasDryingSelected
        ? processingValues.temperatureUnit
        : null,
      dryingDurationHours:
        hasDryingSelected && processingValues.dryingDurationHours
          ? parseInt(processingValues.dryingDurationHours, 10)
          : null,
      dryingMethod: hasDryingSelected
        ? processingValues.dryingMethod || null
        : null,
      // QC
      moistureContent: processingValues.moistureContent || null,
      targetMoistureContent: processingValues.targetMoistureContent || null,
      // Derived material
      derivedMaterialId: processingValues.derivedMaterialId || null,
      aliquotNotes: processingValues.aliquotNotes || null,
      // Notes
      notes: processingValues.notes || null,
    };

    postToOpenElisServerJsonResponse(
      `/rest/notebook/tradmed/page/${pageData.id}/prepare`,
      JSON.stringify(requestData),
      (response) => {
        setIsProcessing(false);
        if (response && response.success) {
          let message = intl.formatMessage(
            {
              id: "notebook.page.tradmed.prep.success",
              defaultMessage:
                "Applied processing details to {count} sample(s).",
            },
            { count: response.updatedCount || selectedSampleIds.length },
          );

          // Add yield info if available
          if (response.averageYieldPercentage) {
            message += ` Average yield: ${response.averageYieldPercentage}%`;
          }

          setSuccessMessage(message);
          setProcessingModalOpen(false);
          setSelectedSampleIds([]);
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(
            response?.error ||
              intl.formatMessage({
                id: "notebook.page.tradmed.prep.error.apply",
                defaultMessage:
                  "Failed to apply processing details. Please try again.",
              }),
          );
        }
      },
    );
  }, [
    selectedSampleIds,
    processingValues,
    hasRealPageId,
    hasDryingSelected,
    intl,
    loadPageSamples,
    onProgressUpdate,
    pageData?.id,
  ]);

  // Mark samples as preparation complete using dedicated endpoint
  const handleMarkComplete = useCallback(() => {
    if (selectedSampleIds.length === 0) return;

    if (!hasRealPageId) {
      setError(
        intl.formatMessage({
          id: "notebook.page.tradmed.error.noPage",
          defaultMessage:
            "Cannot update samples: Page not properly initialized.",
        }),
      );
      return;
    }

    // Check if all selected samples have processing info
    const selectedSamples = samples.filter((s) =>
      selectedSampleIds.includes(s.id),
    );
    const missingProcessing = selectedSamples.filter(
      (s) => !s.processingTypes || s.processingTypes.length === 0,
    );
    if (missingProcessing.length > 0) {
      setError(
        intl.formatMessage(
          {
            id: "notebook.page.tradmed.prep.error.missingProcessing",
            defaultMessage:
              "{count} sample(s) have no processing information. Please apply processing details first.",
          },
          { count: missingProcessing.length },
        ),
      );
      return;
    }

    // Check for failed QC
    const failedQC = selectedSamples.filter(
      (s) => s.moistureQCPassed === false,
    );
    if (failedQC.length > 0) {
      setError(
        intl.formatMessage(
          {
            id: "notebook.page.tradmed.prep.error.qcFailed",
            defaultMessage:
              "{count} sample(s) failed moisture content QC. Please review before marking complete.",
          },
          { count: failedQC.length },
        ),
      );
      return;
    }

    postToOpenElisServerJsonResponse(
      `/rest/notebook/tradmed/page/${pageData.id}/prepare/complete`,
      JSON.stringify({
        sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
      }),
      (response) => {
        if (response && response.success) {
          setSuccessMessage(
            intl.formatMessage(
              {
                id: "notebook.page.tradmed.prep.completed",
                defaultMessage:
                  "Marked {count} sample(s) as Prepared. They can now proceed to Extraction.",
              },
              { count: response.completedCount || selectedSampleIds.length },
            ),
          );
          setSelectedSampleIds([]);
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(
            response?.error ||
              intl.formatMessage({
                id: "notebook.page.tradmed.error.status",
                defaultMessage: "Failed to update status. Please try again.",
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

  // Calculate stats
  const processedCount = samples.filter(
    (s) => s.processingTypes && s.processingTypes.length > 0,
  ).length;
  const pendingCount = samples.filter(
    (s) => !s.processingTypes || s.processingTypes.length === 0,
  ).length;
  const completedCount = samples.filter((s) => s.status === "COMPLETED").length;
  const qcFailedCount = samples.filter(
    (s) => s.moistureQCPassed === false,
  ).length;

  // Render processing types as tags
  const renderProcessingTags = (sample) => {
    if (!sample.processingTypes || sample.processingTypes.length === 0) {
      return <Tag type="gray">Pending</Tag>;
    }

    const typeLabels = {
      GRINDING: "Ground",
      CHOPPING: "Chopped",
      DRYING: "Dried",
      POWDERING: "Powdered",
      SIEVING: "Sieved",
      MILLING: "Milled",
      FREEZE_DRYING: "Lyophilized",
      SHADE_DRYING: "Shade Dried",
      OVEN_DRYING: "Oven Dried",
      SUN_DRYING: "Sun Dried",
      AIR_DRYING: "Air Dried",
    };

    return (
      <div style={{ display: "flex", gap: "4px", flexWrap: "wrap" }}>
        {sample.processingTypes.map((type) => (
          <Tag key={type} type="cyan" size="sm">
            {typeLabels[type] || type}
          </Tag>
        ))}
      </div>
    );
  };

  // Render material state tag
  const renderMaterialStateTag = (sample) => {
    if (!sample.materialState) return null;

    const stateColors = {
      FRESH: "green",
      DRIED: "warm-gray",
      PRESERVED: "teal",
      PARTIALLY_DRIED: "purple",
    };

    const stateLabels = {
      FRESH: "Fresh",
      DRIED: "Dried",
      PRESERVED: "Preserved",
      PARTIALLY_DRIED: "Partial",
    };

    return (
      <Tag type={stateColors[sample.materialState] || "gray"} size="sm">
        {stateLabels[sample.materialState] || sample.materialState}
      </Tag>
    );
  };

  // Render yield information
  const renderYield = (sample) => {
    if (!sample.yieldPercentage) return "-";

    const yield_val = parseFloat(sample.yieldPercentage);
    let color = "green";
    if (yield_val < 30) color = "red";
    else if (yield_val < 60) color = "orange";

    return (
      <Tag type={color} size="sm">
        {sample.yieldPercentage}%
      </Tag>
    );
  };

  // Render QC status
  const renderQCStatus = (sample) => {
    if (
      sample.moistureContent === undefined ||
      sample.moistureContent === null
    ) {
      return "-";
    }

    return (
      <div style={{ display: "flex", alignItems: "center", gap: "4px" }}>
        <span>{sample.moistureContent}%</span>
        {sample.moistureQCPassed === false && (
          <Tag type="red" size="sm">
            QC Fail
          </Tag>
        )}
        {sample.moistureQCPassed === true && (
          <Tag type="green" size="sm">
            OK
          </Tag>
        )}
      </div>
    );
  };

  return (
    <div className="tradmed-preparation-page">
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.page.tradmed.preparation.title"
            defaultMessage="Sample Preparation for Analysis"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.tradmed.preparation.description"
            defaultMessage="Prepare raw material for extraction or analysis through physical processing such as grinding, chopping, drying, or powdering."
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
                  id="notebook.page.tradmed.totalSamples"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{samples.length}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.tradmed.prep.processed"
                  defaultMessage="Processed"
                />
              </span>
              <span className="progress-value">{processedCount}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.tradmed.prep.pending"
                  defaultMessage="Pending"
                />
              </span>
              <span className="progress-value">{pendingCount}</span>
            </Tile>
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.tradmed.prep.readyForExtraction"
                  defaultMessage="Ready for Extraction"
                />
              </span>
              <span className="progress-value">{completedCount}</span>
            </Tile>
            {qcFailedCount > 0 && (
              <Tile
                className="progress-tile"
                style={{ backgroundColor: "#fff1f1" }}
              >
                <span className="progress-label" style={{ color: "#da1e28" }}>
                  <FormattedMessage
                    id="notebook.page.tradmed.prep.qcFailed"
                    defaultMessage="QC Failed"
                  />
                </span>
                <span className="progress-value" style={{ color: "#da1e28" }}>
                  {qcFailedCount}
                </span>
              </Tile>
            )}
          </div>
        </Column>
      </Grid>

      {/* Action Buttons */}
      <div className="page-actions-bar">
        <Button
          kind="primary"
          size="sm"
          renderIcon={Cut}
          onClick={() => {
            resetProcessingValues();
            setProcessingModalOpen(true);
          }}
          disabled={selectedSampleIds.length === 0}
        >
          <FormattedMessage
            id="notebook.page.tradmed.prep.applyProcessing"
            defaultMessage="Apply Processing ({count})"
            values={{ count: selectedSampleIds.length }}
          />
        </Button>

        {selectedSampleIds.length > 0 && (
          <Button
            kind="secondary"
            size="sm"
            renderIcon={Checkmark}
            onClick={handleMarkComplete}
          >
            <FormattedMessage
              id="notebook.page.tradmed.prep.markComplete"
              defaultMessage="Mark Complete ({count})"
              values={{ count: selectedSampleIds.length }}
            />
          </Button>
        )}
      </div>

      {/* Messages */}
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
            { key: "localName", header: "Local Name" },
            { key: "plantPart", header: "Plant Part" },
            {
              key: "materialState",
              header: "Material",
              render: (value, sample) => renderMaterialStateTag(sample),
            },
            {
              key: "processingTypes",
              header: "Processing",
              render: (value, sample) => renderProcessingTags(sample),
            },
            {
              key: "yieldPercentage",
              header: "Yield",
              render: (value, sample) => renderYield(sample),
            },
            {
              key: "moistureContent",
              header: "Moisture %",
              render: (value, sample) => renderQCStatus(sample),
            },
            { key: "processedBy", header: "Processed By" },
            { key: "status", header: "Status" },
          ]}
        />
      </div>

      {/* Empty state */}
      {!loading && samples.length === 0 && (
        <div className="empty-state">
          <p>
            <FormattedMessage
              id="notebook.page.tradmed.prep.empty"
              defaultMessage="No samples pending preparation. Complete storage assignment first."
            />
          </p>
        </div>
      )}

      {/* Processing Modal */}
      <Modal
        open={processingModalOpen}
        onRequestClose={() => setProcessingModalOpen(false)}
        modalHeading={intl.formatMessage({
          id: "notebook.tradmed.prep.modal.title",
          defaultMessage: "Apply Processing Details",
        })}
        primaryButtonText={
          isProcessing
            ? intl.formatMessage({
                id: "label.applying",
                defaultMessage: "Applying...",
              })
            : intl.formatMessage({ id: "label.apply", defaultMessage: "Apply" })
        }
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestSubmit={handleApplyProcessing}
        onSecondarySubmit={() => setProcessingModalOpen(false)}
        size="lg"
        primaryButtonDisabled={isProcessing}
      >
        <p className="modal-description">
          <FormattedMessage
            id="notebook.tradmed.prep.modal.description"
            defaultMessage="Record physical processing details for {count} selected sample(s)."
            values={{ count: selectedSampleIds.length }}
          />
        </p>

        <Grid fullWidth>
          {/* Processing Types */}
          <Column lg={16} md={8} sm={4}>
            <h5 style={{ marginTop: "1rem", marginBottom: "0.5rem" }}>
              <FormattedMessage
                id="notebook.tradmed.prep.section.types"
                defaultMessage="Processing Types *"
              />
            </h5>
            <p className="helper-text" style={{ marginBottom: "0.5rem" }}>
              <FormattedMessage
                id="notebook.tradmed.prep.section.types.helper"
                defaultMessage="Select all processing steps applied to the sample"
              />
            </p>
          </Column>

          {/* Physical Processing */}
          <Column lg={8} md={4} sm={4}>
            <p
              style={{
                fontSize: "0.75rem",
                color: "#525252",
                marginBottom: "0.5rem",
              }}
            >
              Physical Processing
            </p>
            <div
              style={{ display: "flex", flexDirection: "column", gap: "4px" }}
            >
              {processingTypeOptions
                .filter((o) => !o.isDrying)
                .map((option) => (
                  <Checkbox
                    key={option.id}
                    id={`processing-${option.id}`}
                    labelText={option.label}
                    checked={processingValues.processingTypes.includes(
                      option.id,
                    )}
                    onChange={(e, { checked }) =>
                      handleProcessingTypeChange(option.id, checked)
                    }
                  />
                ))}
            </div>
          </Column>

          {/* Drying Methods */}
          <Column lg={8} md={4} sm={4}>
            <p
              style={{
                fontSize: "0.75rem",
                color: "#525252",
                marginBottom: "0.5rem",
              }}
            >
              Drying Methods
            </p>
            <div
              style={{ display: "flex", flexDirection: "column", gap: "4px" }}
            >
              {processingTypeOptions
                .filter((o) => o.isDrying)
                .map((option) => (
                  <Checkbox
                    key={option.id}
                    id={`processing-${option.id}`}
                    labelText={option.label}
                    checked={processingValues.processingTypes.includes(
                      option.id,
                    )}
                    onChange={(e, { checked }) =>
                      handleProcessingTypeChange(option.id, checked)
                    }
                  />
                ))}
            </div>
          </Column>

          {/* Material State */}
          <Column lg={16} md={8} sm={4}>
            <h5 style={{ marginTop: "1.5rem", marginBottom: "0.5rem" }}>
              <FormattedMessage
                id="notebook.tradmed.prep.section.state"
                defaultMessage="Material State *"
              />
            </h5>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <Select
              id="materialState"
              labelText={intl.formatMessage({
                id: "notebook.tradmed.prep.field.state",
                defaultMessage: "Starting Material State",
              })}
              value={processingValues.materialState}
              onChange={(e) =>
                setProcessingValues((prev) => ({
                  ...prev,
                  materialState: e.target.value,
                }))
              }
            >
              <SelectItem value="" text="Select material state..." />
              {materialStateOptions.map((opt) => (
                <SelectItem key={opt.id} value={opt.id} text={opt.label} />
              ))}
            </Select>
          </Column>

          {/* Yield Tracking Section */}
          <Column lg={16} md={8} sm={4}>
            <h5
              style={{
                marginTop: "1.5rem",
                marginBottom: "0.5rem",
                display: "flex",
                alignItems: "center",
                gap: "8px",
              }}
            >
              <Scale size={16} />
              <FormattedMessage
                id="notebook.tradmed.prep.section.yield"
                defaultMessage="Yield Tracking"
              />
            </h5>
            <p className="helper-text" style={{ marginBottom: "0.5rem" }}>
              <FormattedMessage
                id="notebook.tradmed.prep.section.yield.helper"
                defaultMessage="Track weight before and after processing to calculate yield percentage"
              />
            </p>
          </Column>
          <Column lg={4} md={2} sm={2}>
            <TextInput
              id="initialWeight"
              labelText={intl.formatMessage({
                id: "notebook.tradmed.prep.field.initialWeight",
                defaultMessage: "Initial Weight",
              })}
              type="number"
              min="0"
              step="0.01"
              value={processingValues.initialWeight}
              onChange={(e) =>
                setProcessingValues((prev) => ({
                  ...prev,
                  initialWeight: e.target.value,
                }))
              }
              placeholder="e.g., 500"
            />
          </Column>
          <Column lg={4} md={2} sm={2}>
            <TextInput
              id="finalWeight"
              labelText={intl.formatMessage({
                id: "notebook.tradmed.prep.field.finalWeight",
                defaultMessage: "Final Weight",
              })}
              type="number"
              min="0"
              step="0.01"
              value={processingValues.finalWeight}
              onChange={(e) =>
                setProcessingValues((prev) => ({
                  ...prev,
                  finalWeight: e.target.value,
                }))
              }
              placeholder="e.g., 250"
            />
          </Column>
          <Column lg={4} md={2} sm={2}>
            <Select
              id="weightUnit"
              labelText={intl.formatMessage({
                id: "notebook.tradmed.prep.field.unit",
                defaultMessage: "Unit",
              })}
              value={processingValues.weightUnit}
              onChange={(e) =>
                setProcessingValues((prev) => ({
                  ...prev,
                  weightUnit: e.target.value,
                }))
              }
            >
              <SelectItem value="g" text="g (grams)" />
              <SelectItem value="mg" text="mg (milligrams)" />
              <SelectItem value="kg" text="kg (kilograms)" />
            </Select>
          </Column>
          <Column lg={4} md={2} sm={2}>
            {calculatedYield && (
              <div style={{ paddingTop: "1.5rem" }}>
                <span style={{ fontSize: "0.75rem", color: "#525252" }}>
                  Yield
                </span>
                <div
                  style={{
                    fontSize: "1.25rem",
                    fontWeight: "600",
                    color:
                      parseFloat(calculatedYield) < 30
                        ? "#da1e28"
                        : parseFloat(calculatedYield) < 60
                          ? "#f1c21b"
                          : "#24a148",
                  }}
                >
                  {calculatedYield}%
                </div>
              </div>
            )}
          </Column>

          {/* Drying Parameters - only show if drying method selected */}
          {hasDryingSelected && (
            <>
              <Column lg={16} md={8} sm={4}>
                <h5
                  style={{
                    marginTop: "1.5rem",
                    marginBottom: "0.5rem",
                    display: "flex",
                    alignItems: "center",
                    gap: "8px",
                  }}
                >
                  <Chemistry size={16} />
                  <FormattedMessage
                    id="notebook.tradmed.prep.section.drying"
                    defaultMessage="Drying Parameters"
                  />
                </h5>
              </Column>
              <Column lg={4} md={2} sm={2}>
                <TextInput
                  id="dryingTemperature"
                  labelText={intl.formatMessage({
                    id: "notebook.tradmed.prep.field.dryingTemp",
                    defaultMessage: "Temperature",
                  })}
                  type="number"
                  min="0"
                  max="200"
                  value={processingValues.dryingTemperature}
                  onChange={(e) =>
                    setProcessingValues((prev) => ({
                      ...prev,
                      dryingTemperature: e.target.value,
                    }))
                  }
                  placeholder="e.g., 40"
                />
              </Column>
              <Column lg={4} md={2} sm={2}>
                <Select
                  id="temperatureUnit"
                  labelText={intl.formatMessage({
                    id: "notebook.tradmed.prep.field.tempUnit",
                    defaultMessage: "Unit",
                  })}
                  value={processingValues.temperatureUnit}
                  onChange={(e) =>
                    setProcessingValues((prev) => ({
                      ...prev,
                      temperatureUnit: e.target.value,
                    }))
                  }
                >
                  <SelectItem value="°C" text="°C (Celsius)" />
                  <SelectItem value="°F" text="°F (Fahrenheit)" />
                </Select>
              </Column>
              <Column lg={4} md={2} sm={2}>
                <TextInput
                  id="dryingDuration"
                  labelText={intl.formatMessage({
                    id: "notebook.tradmed.prep.field.dryingDuration",
                    defaultMessage: "Duration (hours)",
                  })}
                  type="number"
                  min="0"
                  value={processingValues.dryingDurationHours}
                  onChange={(e) =>
                    setProcessingValues((prev) => ({
                      ...prev,
                      dryingDurationHours: e.target.value,
                    }))
                  }
                  placeholder="e.g., 24"
                />
              </Column>
              <Column lg={4} md={2} sm={2}>
                <TextInput
                  id="dryingMethod"
                  labelText={intl.formatMessage({
                    id: "notebook.tradmed.prep.field.dryingMethod",
                    defaultMessage: "Method Details",
                  })}
                  value={processingValues.dryingMethod}
                  onChange={(e) =>
                    setProcessingValues((prev) => ({
                      ...prev,
                      dryingMethod: e.target.value,
                    }))
                  }
                  placeholder="e.g., Convection oven"
                />
              </Column>
            </>
          )}

          {/* Quality Control Section */}
          <Column lg={16} md={8} sm={4}>
            <h5 style={{ marginTop: "1.5rem", marginBottom: "0.5rem" }}>
              <FormattedMessage
                id="notebook.tradmed.prep.section.qc"
                defaultMessage="Quality Control"
              />
            </h5>
          </Column>
          <Column lg={4} md={2} sm={2}>
            <TextInput
              id="moistureContent"
              labelText={intl.formatMessage({
                id: "notebook.tradmed.prep.field.moisture",
                defaultMessage: "Moisture Content (%)",
              })}
              type="number"
              min="0"
              max="100"
              step="0.1"
              value={processingValues.moistureContent}
              onChange={(e) =>
                setProcessingValues((prev) => ({
                  ...prev,
                  moistureContent: e.target.value,
                }))
              }
              placeholder="e.g., 8.5"
            />
          </Column>
          <Column lg={4} md={2} sm={2}>
            <TextInput
              id="targetMoistureContent"
              labelText={intl.formatMessage({
                id: "notebook.tradmed.prep.field.targetMoisture",
                defaultMessage: "Target Max (%)",
              })}
              type="number"
              min="0"
              max="100"
              step="0.1"
              value={processingValues.targetMoistureContent}
              onChange={(e) =>
                setProcessingValues((prev) => ({
                  ...prev,
                  targetMoistureContent: e.target.value,
                }))
              }
              placeholder="e.g., 10"
            />
          </Column>
          <Column lg={8} md={4} sm={4}>
            {processingValues.moistureContent &&
              processingValues.targetMoistureContent && (
                <div style={{ paddingTop: "1.5rem" }}>
                  {parseFloat(processingValues.moistureContent) <=
                  parseFloat(processingValues.targetMoistureContent) ? (
                    <Tag type="green">
                      QC Passed - Moisture within acceptable range
                    </Tag>
                  ) : (
                    <Tag type="red">
                      QC Failed - Moisture exceeds target (
                      {processingValues.targetMoistureContent}%)
                    </Tag>
                  )}
                </div>
              )}
          </Column>

          {/* Processing Details */}
          <Column lg={16} md={8} sm={4}>
            <h5 style={{ marginTop: "1.5rem", marginBottom: "0.5rem" }}>
              <FormattedMessage
                id="notebook.tradmed.prep.section.details"
                defaultMessage="Processing Details"
              />
            </h5>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <div className="cds--form-item">
              <label className="cds--label">
                <FormattedMessage
                  id="notebook.tradmed.prep.field.date"
                  defaultMessage="Processing Date"
                />
              </label>
              <input
                type="date"
                className="cds--text-input"
                value={processingValues.processingDate}
                onChange={(e) =>
                  setProcessingValues((prev) => ({
                    ...prev,
                    processingDate: e.target.value,
                  }))
                }
              />
            </div>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="processedBy"
              labelText={intl.formatMessage({
                id: "notebook.tradmed.prep.field.processedBy",
                defaultMessage: "Processed By",
              })}
              value={processingValues.processedBy}
              onChange={(e) =>
                setProcessingValues((prev) => ({
                  ...prev,
                  processedBy: e.target.value,
                }))
              }
              placeholder="Enter technician name"
            />
          </Column>
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="equipment"
              labelText={intl.formatMessage({
                id: "notebook.tradmed.prep.field.equipment",
                defaultMessage: "Equipment Used",
              })}
              value={processingValues.equipment}
              onChange={(e) =>
                setProcessingValues((prev) => ({
                  ...prev,
                  equipment: e.target.value,
                }))
              }
              placeholder="e.g., Mortar & Pestle, Blender, Mill"
            />
          </Column>
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="particleSize"
              labelText={intl.formatMessage({
                id: "notebook.tradmed.prep.field.particleSize",
                defaultMessage: "Particle Size (mesh/mm)",
              })}
              value={processingValues.particleSize}
              onChange={(e) =>
                setProcessingValues((prev) => ({
                  ...prev,
                  particleSize: e.target.value,
                }))
              }
              placeholder="e.g., 40 mesh, <1mm"
            />
          </Column>

          {/* Derived Material */}
          <Column lg={16} md={8} sm={4}>
            <h5 style={{ marginTop: "1.5rem", marginBottom: "0.5rem" }}>
              <FormattedMessage
                id="notebook.tradmed.prep.section.derived"
                defaultMessage="Derived Material (Optional)"
              />
            </h5>
            <p className="helper-text" style={{ marginBottom: "0.5rem" }}>
              <FormattedMessage
                id="notebook.tradmed.prep.section.derived.helper"
                defaultMessage="If creating a new aliquot or derived sample, specify its ID"
              />
            </p>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="derivedMaterialId"
              labelText={intl.formatMessage({
                id: "notebook.tradmed.prep.field.derivedId",
                defaultMessage: "Derived Material ID",
              })}
              value={processingValues.derivedMaterialId}
              onChange={(e) =>
                setProcessingValues((prev) => ({
                  ...prev,
                  derivedMaterialId: e.target.value,
                }))
              }
              placeholder="e.g., TM-001-P1"
            />
          </Column>
          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="aliquotNotes"
              labelText={intl.formatMessage({
                id: "notebook.tradmed.prep.field.aliquotNotes",
                defaultMessage: "Aliquot Notes",
              })}
              value={processingValues.aliquotNotes}
              onChange={(e) =>
                setProcessingValues((prev) => ({
                  ...prev,
                  aliquotNotes: e.target.value,
                }))
              }
              placeholder="e.g., Portion reserved for archival"
            />
          </Column>

          {/* Notes */}
          <Column lg={16} md={8} sm={4}>
            <TextArea
              id="notes"
              labelText={intl.formatMessage({
                id: "notebook.tradmed.prep.field.notes",
                defaultMessage: "Processing Notes",
              })}
              value={processingValues.notes}
              onChange={(e) =>
                setProcessingValues((prev) => ({
                  ...prev,
                  notes: e.target.value,
                }))
              }
              placeholder="Enter any additional notes about the processing..."
            />
          </Column>
        </Grid>
      </Modal>
    </div>
  );
}

export default TraditionalMedicinePreparationPage;
