import React, { useState, useEffect, useCallback, useMemo } from "react";
import {
  Grid,
  Column,
  Tile,
  Button,
  ProgressIndicator,
  ProgressStep,
  InlineNotification,
  Tag,
  Modal,
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Checkbox,
  OverflowMenu,
  OverflowMenuItem,
} from "@carbon/react";
import {
  ArrowRight,
  Checkmark,
  Warning,
  Error,
  Upload,
  Chemistry,
  Microscope,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer, postToOpenElisServer } from "../../utils/Utils";

/**
 * VirologyWorkflowController - Orchestrates sample progression through virology workflow stages
 *
 * Stage 1: Sample Reception & Registration (VirologySampleReceptionPage)
 * Stage 2: Virus Culture Growth (VirusCultureWorkflowPage)
 * Stage 3: Vaccine Development (VaccineevelopmentPage)
 *
 * This controller ensures:
 * - Samples can only progress to next stage after completing current stage
 * - Stage completion validation based on all required steps
 * - Proper sample routing and status tracking
 * - Quality gates and approval mechanisms
 */
function VirologyWorkflowController({ entryId, onProgressUpdate }) {
  const intl = useIntl();

  // Core workflow state
  const [currentStage, setCurrentStage] = useState("stage1_reception");
  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);

  // Stage progression state
  const [stageProgression, setStageProgression] = useState({});
  const [cultureBatches, setCultureBatches] = useState([]);
  const [vaccineData, setVaccineData] = useState({});

  // Modal states
  const [progressionModalOpen, setProgressionModalOpen] = useState(false);
  const [selectedProgression, setSelectedProgression] = useState(null);

  // Define virology workflow stages with progression requirements
  const workflowStages = useMemo(
    () => [
      {
        id: "stage1_reception",
        name: "Sample Reception & Registration",
        description:
          "Import samples from delivery manifest and verify reception metadata",
        icon: Upload,
        pageComponent: "VirologySampleReceptionPage",
        requiredStatus: "COMPLETED",
        progressionCriteria: {
          allSamplesVerified: true,
          receptionMetadataComplete: true,
          manifestValidated: true,
        },
        nextStage: "stage2_culture",
      },
      {
        id: "stage2_culture",
        name: "Virus Culture Growth",
        description:
          "9-step virus culture workflow from media preparation to packaging",
        icon: Chemistry,
        pageComponent: "VirusCultureWorkflowPage",
        requiredStatus: "COMPLETED",
        progressionCriteria: {
          allBatchesComplete: true,
          allStepsValidated: true,
          qcResultsPassed: true,
          packagingFinalized: true,
        },
        nextStage: "stage3_vaccine",
      },
      {
        id: "stage3_vaccine",
        name: "Vaccine Development",
        description:
          "6-stage vaccine development from virus isolation to clinical trials",
        icon: Microscope,
        pageComponent: "VaccineevelopmentPage",
        requiredStatus: "COMPLETED",
        progressionCriteria: {
          virusIsolationComplete: true,
          titerMeasurementComplete: true,
          genomeSequencingComplete: true,
          seedVirusProductionComplete: true,
          preclinicalTrialsComplete: true,
          clinicalTrialsComplete: true,
        },
        nextStage: null, // Final stage
      },
    ],
    [],
  );

  // Load samples and progression status
  useEffect(() => {
    if (entryId) {
      loadWorkflowData();
    }
  }, [entryId]);

  const loadWorkflowData = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      // Load samples for current entry across all stages
      const [samplesResponse, progressionResponse, batchesResponse] =
        await Promise.all([
          fetchSamples(),
          fetchStageProgression(),
          fetchCultureBatches(),
        ]);

      setSamples(samplesResponse || []);
      setStageProgression(progressionResponse || {});
      setCultureBatches(batchesResponse || []);

      // Determine current stage based on progression
      const currentStageId = determineCurrentStage(progressionResponse);
      setCurrentStage(currentStageId);
    } catch (error) {
      console.error("Error loading workflow data:", error);
      setError("Failed to load workflow data. Please try again.");
    } finally {
      setLoading(false);
    }
  }, [entryId]);

  const fetchSamples = useCallback(() => {
    return new Promise((resolve, reject) => {
      getFromOpenElisServer(
        `/rest/notebook/entry/${entryId}/virology/samples`,
        (response) => resolve(response),
        (error) => reject(error),
      );
    });
  }, [entryId]);

  const fetchStageProgression = useCallback(() => {
    return new Promise((resolve, reject) => {
      getFromOpenElisServer(
        `/rest/notebook/entry/${entryId}/virology/progression`,
        (response) => resolve(response),
        (error) => reject(error),
      );
    });
  }, [entryId]);

  const fetchCultureBatches = useCallback(() => {
    return new Promise((resolve, reject) => {
      getFromOpenElisServer(
        `/rest/notebook/virology/culture/batches/entry/${entryId}`,
        (response) => resolve(response),
        (error) => reject(error),
      );
    });
  }, [entryId]);

  // Determine current stage based on progression data
  const determineCurrentStage = useCallback((progression) => {
    if (!progression) return "stage1_reception";

    if (
      progression.stage3_vaccine?.status === "IN_PROGRESS" ||
      progression.stage3_vaccine?.status === "COMPLETED"
    ) {
      return "stage3_vaccine";
    }
    if (
      progression.stage2_culture?.status === "IN_PROGRESS" ||
      progression.stage2_culture?.status === "COMPLETED"
    ) {
      return "stage2_culture";
    }
    return "stage1_reception";
  }, []);

  // Validate stage completion criteria
  const validateStageCompletion = useCallback(
    (stageId, samples, batches, vaccineData) => {
      const stage = workflowStages.find((s) => s.id === stageId);
      if (!stage) return { isComplete: false, errors: ["Invalid stage"] };

      const errors = [];
      let isComplete = true;

      switch (stageId) {
        case "stage1_reception":
          // Check if all samples are verified (COMPLETED status)
          const unverifiedSamples = samples.filter(
            (s) => s.status !== "COMPLETED",
          );
          if (unverifiedSamples.length > 0) {
            isComplete = false;
            errors.push(
              `${unverifiedSamples.length} samples still need verification`,
            );
          }

          // Check if reception metadata is complete for all samples
          const incompleteMetadata = samples.filter(
            (s) => !s.receptionDateTime || !s.source || !s.testType,
          );
          if (incompleteMetadata.length > 0) {
            isComplete = false;
            errors.push(
              `${incompleteMetadata.length} samples have incomplete metadata`,
            );
          }
          break;

        case "stage2_culture":
          // Check if all culture batches are complete
          const incompleteBatches = batches.filter(
            (b) => b.status !== "WORKFLOW_COMPLETE",
          );
          if (incompleteBatches.length > 0) {
            isComplete = false;
            errors.push(
              `${incompleteBatches.length} culture batches are incomplete`,
            );
          }

          // Check if all 9 workflow steps are completed with QC passed
          batches.forEach((batch, index) => {
            if (batch.steps) {
              const failedQcSteps = batch.steps.filter(
                (step) =>
                  step.status === "COMPLETED" &&
                  step.qualityCheckResult === "FAILED",
              );
              if (failedQcSteps.length > 0) {
                isComplete = false;
                errors.push(
                  `Batch ${index + 1}: ${failedQcSteps.length} steps failed QC`,
                );
              }

              const incompleteSteps = batch.steps.filter(
                (step) =>
                  step.status !== "COMPLETED" && step.status !== "SKIPPED",
              );
              if (incompleteSteps.length > 0) {
                isComplete = false;
                errors.push(
                  `Batch ${index + 1}: ${incompleteSteps.length} steps incomplete`,
                );
              }
            }
          });
          break;

        case "stage3_vaccine":
          // Check if all 6 vaccine development stages are complete
          const requiredStages = [
            "virusIsolation",
            "titerMeasurement",
            "genomeSequencing",
            "seedVirusProduction",
            "preclinicalTrials",
            "clinicalTrials",
          ];

          requiredStages.forEach((stageName) => {
            const stageData = vaccineData[stageName];
            if (!stageData || Object.keys(stageData).length === 0) {
              isComplete = false;
              errors.push(
                `${stageName.replace(/([A-Z])/g, " $1").toLowerCase()} not completed`,
              );
            }
          });
          break;

        default:
          isComplete = false;
          errors.push("Unknown stage");
      }

      return { isComplete, errors };
    },
    [workflowStages],
  );

  // Progress selected samples to next stage
  const progressSamplesToNextStage = useCallback(() => {
    if (selectedSampleIds.length === 0) {
      setError("Please select samples to progress.");
      return;
    }

    const currentStageObj = workflowStages.find((s) => s.id === currentStage);
    if (!currentStageObj?.nextStage) {
      setError("This is the final stage. No further progression available.");
      return;
    }

    // Validate stage completion for selected samples
    const selectedSamples = samples.filter((s) =>
      selectedSampleIds.includes(s.id),
    );
    const validation = validateStageCompletion(
      currentStage,
      selectedSamples,
      cultureBatches,
      vaccineData,
    );

    if (!validation.isComplete) {
      setError(
        `Cannot progress samples. Issues found: ${validation.errors.join(", ")}`,
      );
      return;
    }

    setSelectedProgression({
      fromStage: currentStage,
      toStage: currentStageObj.nextStage,
      sampleCount: selectedSampleIds.length,
      samples: selectedSamples,
    });
    setProgressionModalOpen(true);
  }, [
    selectedSampleIds,
    currentStage,
    workflowStages,
    samples,
    cultureBatches,
    vaccineData,
    validateStageCompletion,
  ]);

  // Execute sample progression
  const executeProgression = useCallback(() => {
    if (!selectedProgression) return;

    setError(null);
    setSuccessMessage(null);

    const requestData = {
      entryId: parseInt(entryId),
      fromStage: selectedProgression.fromStage,
      toStage: selectedProgression.toStage,
      sampleIds: selectedSampleIds.map((id) => parseInt(id)),
    };

    postToOpenElisServer(
      `/rest/notebook/virology/progression/advance`,
      JSON.stringify(requestData),
      (status) => {
        if (status === 200) {
          setSuccessMessage(
            `Successfully progressed ${selectedProgression.sampleCount} sample(s) from ${selectedProgression.fromStage} to ${selectedProgression.toStage}`,
          );
          setSelectedSampleIds([]);
          setCurrentStage(selectedProgression.toStage);
          setProgressionModalOpen(false);
          setSelectedProgression(null);
          loadWorkflowData(); // Refresh data
          if (onProgressUpdate) onProgressUpdate();
        } else {
          setError("Failed to progress samples. Please try again.");
        }
      },
    );
  }, [
    selectedProgression,
    selectedSampleIds,
    entryId,
    loadWorkflowData,
    onProgressUpdate,
  ]);

  // Get samples for current stage with status filtering
  const getSamplesForStage = useCallback(
    (stageId) => {
      return samples.filter((sample) => {
        switch (stageId) {
          case "stage1_reception":
            return sample.stage === "reception" || !sample.stage; // Default to reception
          case "stage2_culture":
            return sample.stage === "culture";
          case "stage3_vaccine":
            return sample.stage === "vaccine";
          default:
            return false;
        }
      });
    },
    [samples],
  );

  // Calculate stage progress
  const calculateStageProgress = useCallback(
    (stageId) => {
      const stageSamples = getSamplesForStage(stageId);
      if (stageSamples.length === 0) return 0;

      const completedSamples = stageSamples.filter(
        (s) => s.status === "COMPLETED",
      );
      return Math.round((completedSamples.length / stageSamples.length) * 100);
    },
    [getSamplesForStage],
  );

  // Stage progression validation indicators
  const getStageValidation = useCallback(
    (stageId) => {
      const stageSamples = getSamplesForStage(stageId);
      const validation = validateStageCompletion(
        stageId,
        stageSamples,
        cultureBatches,
        vaccineData,
      );

      return {
        canProgress: validation.isComplete,
        issues: validation.errors,
        progress: calculateStageProgress(stageId),
      };
    },
    [
      getSamplesForStage,
      validateStageCompletion,
      cultureBatches,
      vaccineData,
      calculateStageProgress,
    ],
  );

  if (loading) return <div>Loading workflow...</div>;

  const currentStageObj = workflowStages.find((s) => s.id === currentStage);
  const currentStageSamples = getSamplesForStage(currentStage);
  const stageValidation = getStageValidation(currentStage);

  return (
    <div className="virology-workflow-controller">
      {/* Workflow Progress Indicator */}
      <div className="workflow-header">
        <h3>
          <FormattedMessage
            id="virology.workflow.controller.title"
            defaultMessage="Virology & Vaccine Development Workflow"
          />
        </h3>
        <ProgressIndicator
          currentIndex={workflowStages.findIndex((s) => s.id === currentStage)}
        >
          {workflowStages.map((stage, index) => {
            const progress = calculateStageProgress(stage.id);
            const validation = getStageValidation(stage.id);

            return (
              <ProgressStep
                key={stage.id}
                label={stage.name}
                description={`${progress}% complete`}
                complete={progress === 100}
                current={stage.id === currentStage}
                invalid={!validation.canProgress && progress > 0}
              />
            );
          })}
        </ProgressIndicator>
      </div>

      {/* Error/Success Messages */}
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

      {/* Current Stage Information */}
      <Grid fullWidth className="current-stage-section">
        <Column lg={16}>
          <Tile className="current-stage-tile">
            <div className="stage-header">
              <currentStageObj.icon size={24} />
              <div>
                <h4>{currentStageObj.name}</h4>
                <p>{currentStageObj.description}</p>
              </div>
              <Tag
                type={stageValidation.canProgress ? "green" : "red"}
                size="sm"
              >
                {stageValidation.progress}% Complete
              </Tag>
            </div>

            {/* Stage validation issues */}
            {stageValidation.issues.length > 0 && (
              <div className="validation-issues">
                <h5>Issues preventing progression:</h5>
                <ul>
                  {stageValidation.issues.map((issue, index) => (
                    <li key={index}>{issue}</li>
                  ))}
                </ul>
              </div>
            )}

            {/* Sample summary for current stage */}
            <div className="stage-samples-summary">
              <h5>Samples in {currentStageObj.name}:</h5>
              <div className="sample-status-counts">
                <Tag type="gray">Total: {currentStageSamples.length}</Tag>
                <Tag type="blue">
                  Pending:{" "}
                  {
                    currentStageSamples.filter((s) => s.status === "PENDING")
                      .length
                  }
                </Tag>
                <Tag type="cyan">
                  In Progress:{" "}
                  {
                    currentStageSamples.filter(
                      (s) => s.status === "IN_PROGRESS",
                    ).length
                  }
                </Tag>
                <Tag type="green">
                  Completed:{" "}
                  {
                    currentStageSamples.filter((s) => s.status === "COMPLETED")
                      .length
                  }
                </Tag>
              </div>
            </div>

            {/* Progression actions */}
            {currentStageObj.nextStage && (
              <div className="progression-actions">
                <Button
                  kind="primary"
                  renderIcon={ArrowRight}
                  onClick={progressSamplesToNextStage}
                  disabled={
                    !stageValidation.canProgress ||
                    selectedSampleIds.length === 0
                  }
                >
                  Progress {selectedSampleIds.length || 0} Sample(s) to{" "}
                  {
                    workflowStages.find(
                      (s) => s.id === currentStageObj.nextStage,
                    )?.name
                  }
                </Button>
              </div>
            )}
          </Tile>
        </Column>
      </Grid>

      {/* Sample Selection Grid */}
      <Grid fullWidth className="samples-section">
        <Column lg={16}>
          <Tile>
            <h4>Select Samples for Progression</h4>
            <DataTable
              rows={currentStageSamples.map((sample) => ({
                id: sample.id,
                externalId: sample.externalId,
                accessionNumber: sample.accessionNumber,
                sampleType: sample.sampleType,
                status: sample.status,
                lastUpdated: sample.lastUpdated,
              }))}
              headers={[
                { key: "externalId", header: "Sample ID" },
                { key: "accessionNumber", header: "Accession #" },
                { key: "sampleType", header: "Sample Type" },
                { key: "status", header: "Status" },
                { key: "lastUpdated", header: "Last Updated" },
              ]}
              render={({
                rows,
                headers,
                getHeaderProps,
                getRowProps,
                getSelectionProps,
              }) => (
                <TableContainer>
                  <Table>
                    <TableHead>
                      <TableRow>
                        <TableHeader>
                          <Checkbox
                            {...getSelectionProps({
                              onClick: () => {
                                const allIds = currentStageSamples.map(
                                  (s) => s.id,
                                );
                                setSelectedSampleIds(
                                  selectedSampleIds.length === allIds.length
                                    ? []
                                    : allIds,
                                );
                              },
                              checked:
                                selectedSampleIds.length ===
                                currentStageSamples.length,
                              indeterminate:
                                selectedSampleIds.length > 0 &&
                                selectedSampleIds.length <
                                  currentStageSamples.length,
                            })}
                          />
                        </TableHeader>
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
                      {rows.map((row) => (
                        <TableRow key={row.id} {...getRowProps({ row })}>
                          <TableCell>
                            <Checkbox
                              checked={selectedSampleIds.includes(row.id)}
                              onChange={() => {
                                const isSelected = selectedSampleIds.includes(
                                  row.id,
                                );
                                setSelectedSampleIds((prev) =>
                                  isSelected
                                    ? prev.filter((id) => id !== row.id)
                                    : [...prev, row.id],
                                );
                              }}
                            />
                          </TableCell>
                          {row.cells.map((cell) => (
                            <TableCell key={cell.id}>
                              {cell.id.includes("status") ? (
                                <Tag
                                  type={
                                    cell.value === "COMPLETED"
                                      ? "green"
                                      : cell.value === "IN_PROGRESS"
                                        ? "cyan"
                                        : "gray"
                                  }
                                >
                                  {cell.value}
                                </Tag>
                              ) : (
                                cell.value
                              )}
                            </TableCell>
                          ))}
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
              )}
            />
          </Tile>
        </Column>
      </Grid>

      {/* Progression Confirmation Modal */}
      <Modal
        open={progressionModalOpen}
        onRequestClose={() => setProgressionModalOpen(false)}
        modalHeading="Confirm Sample Progression"
        primaryButtonText="Confirm Progression"
        secondaryButtonText="Cancel"
        onRequestSubmit={executeProgression}
      >
        {selectedProgression && (
          <div>
            <p>
              You are about to progress{" "}
              <strong>{selectedProgression.sampleCount}</strong> sample(s) from{" "}
              <strong>{selectedProgression.fromStage}</strong> to{" "}
              <strong>{selectedProgression.toStage}</strong>.
            </p>
            <p>This action will:</p>
            <ul>
              <li>Update sample status and stage assignments</li>
              <li>Create audit trail entries</li>
              <li>Trigger next stage workflow initialization</li>
              <li>Send notifications to assigned personnel</li>
            </ul>
            <p>Are you sure you want to proceed?</p>
          </div>
        )}
      </Modal>
    </div>
  );
}

export default VirologyWorkflowController;
