import React, {
  useState,
  useEffect,
  useRef,
  useCallback,
  useContext,
  useMemo,
} from "react";
import {
  Grid,
  Column,
  Button,
  Tag,
  Modal,
  TextInput,
  TextArea,
  DatePicker,
  DatePickerInput,
  Select,
  SelectItem,
  Tabs,
  TabList,
  Tab,
  TabPanels,
  TabPanel,
  Tile,
} from "@carbon/react";
import {
  Save,
  Checkmark,
  Chemistry,
  Hospital,
  View,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import { NotificationContext } from "../../../layout/Layout";
import { NotificationKinds } from "../../../common/CustomNotification";
import SampleGrid from "../../workflow/SampleGrid";
import "../../workflow/NotebookWorkflow.css";

/**
 * VirologyTrialsPage - Manage Preclinical and Clinical Trials.
 *
 * Uses trialsHistory array pattern (like Feeding page) to allow multiple trials per sample.
 * Both Preclinical and Clinical trials can be logged for the same samples.
 *
 * Preclinical Trials (Animal Testing - External):
 * - Track trial initiation date, animal species, outcomes (immunogenicity, safety)
 *
 * Clinical Trials (Human Testing - External):
 * - Link trial phases (Phase I/II/III), outcomes, regulatory submissions
 */
function VirologyTrialsPage({ entryId, pageData, progress, onProgressUpdate }) {
  const intl = useIntl();
  const { addNotification, setNotificationVisible } =
    useContext(NotificationContext);
  const componentMounted = useRef(false);

  // State
  const [loading, setLoading] = useState(true);
  const [samples, setSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [activeTab, setActiveTab] = useState(0);

  // Modal states
  const [preclinicalModalOpen, setPreclinicalModalOpen] = useState(false);
  const [clinicalModalOpen, setClinicalModalOpen] = useState(false);
  const [historyModalOpen, setHistoryModalOpen] = useState(false);
  const [selectedSampleHistory, setSelectedSampleHistory] = useState(null);

  // Preclinical trial form data
  const [preclinicalData, setPreclinicalData] = useState({
    trialInitiationDate: "",
    animalSpecies: "",
    numberOfAnimals: "",
    studyDesign: "",
    immunogenicityOutcome: "",
    safetyOutcome: "",
    adverseEvents: "",
    notes: "",
  });

  // Clinical trial form data
  const [clinicalData, setClinicalData] = useState({
    trialPhase: "",
    trialInitiationDate: "",
    numberOfParticipants: "",
    primaryEndpoint: "",
    efficacyOutcome: "",
    safetyOutcome: "",
    adverseEvents: "",
    regulatorySubmission: "",
    regulatoryStatus: "",
    notes: "",
  });

  const notify = useCallback(
    ({ kind = NotificationKinds.info, title, subtitle }) => {
      setNotificationVisible(true);
      addNotification({ kind, title, subtitle });
    },
    [addNotification, setNotificationVisible],
  );

  // Load samples
  useEffect(() => {
    componentMounted.current = true;
    loadPageSamples();
    return () => {
      componentMounted.current = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
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
              sampleType: sample.sampleType || sample.typeOfSample?.description,
              collectionDate: sample.collectionDate,
              status: sample.pageStatus || sample.status || "PENDING",
              // Trials history array (allows multiple preclinical AND clinical trials)
              trialsHistory: sample.data?.trialsHistory || [],
              data: sample.data,
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

  // Reset preclinical form
  const resetPreclinicalForm = () => {
    setPreclinicalData({
      trialInitiationDate: "",
      animalSpecies: "",
      numberOfAnimals: "",
      studyDesign: "",
      immunogenicityOutcome: "",
      safetyOutcome: "",
      adverseEvents: "",
      notes: "",
    });
  };

  // Reset clinical form
  const resetClinicalForm = () => {
    setClinicalData({
      trialPhase: "",
      trialInitiationDate: "",
      numberOfParticipants: "",
      primaryEndpoint: "",
      efficacyOutcome: "",
      safetyOutcome: "",
      adverseEvents: "",
      regulatorySubmission: "",
      regulatoryStatus: "",
      notes: "",
    });
  };

  // Handle preclinical trial submission
  const handleSavePreclinicalTrial = useCallback(() => {
    if (
      !preclinicalData.animalSpecies ||
      !preclinicalData.trialInitiationDate
    ) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.error" }),
        subtitle: "Please provide animal species and trial initiation date",
      });
      return;
    }

    if (selectedSampleIds.length === 0) {
      notify({
        kind: NotificationKinds.warning,
        title: intl.formatMessage({ id: "notification.warning" }),
        subtitle: "Please select at least one sample",
      });
      return;
    }

    setLoading(true);

    const payload = {
      notebookPageId: pageData?.id,
      sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
      trialType: "PRECLINICAL",
      ...preclinicalData,
    };

    postToOpenElisServerJsonResponse(
      "/rest/virology/trials",
      JSON.stringify(payload),
      (response) => {
        setLoading(false);

        if (response.success) {
          notify({
            kind: NotificationKinds.success,
            title: intl.formatMessage({ id: "notification.success" }),
            subtitle: `Preclinical trial data saved successfully. ${response.samplesUpdated || 0} sample(s) updated.`,
          });

          resetPreclinicalForm();
          setSelectedSampleIds([]);
          setPreclinicalModalOpen(false);

          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          notify({
            kind: NotificationKinds.error,
            title: intl.formatMessage({ id: "notification.error" }),
            subtitle: response.error || response.message || "Unknown error",
          });
        }
      },
    );
  }, [
    preclinicalData,
    selectedSampleIds,
    pageData?.id,
    intl,
    notify,
    loadPageSamples,
    onProgressUpdate,
  ]);

  // Handle clinical trial submission
  const handleSaveClinicalTrial = useCallback(() => {
    if (!clinicalData.trialPhase || !clinicalData.trialInitiationDate) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.error" }),
        subtitle: "Please provide trial phase and initiation date",
      });
      return;
    }

    if (selectedSampleIds.length === 0) {
      notify({
        kind: NotificationKinds.warning,
        title: intl.formatMessage({ id: "notification.warning" }),
        subtitle: "Please select at least one sample",
      });
      return;
    }

    setLoading(true);

    const payload = {
      notebookPageId: pageData?.id,
      sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
      trialType: "CLINICAL",
      ...clinicalData,
    };

    postToOpenElisServerJsonResponse(
      "/rest/virology/trials",
      JSON.stringify(payload),
      (response) => {
        setLoading(false);

        if (response.success) {
          notify({
            kind: NotificationKinds.success,
            title: intl.formatMessage({ id: "notification.success" }),
            subtitle: `Clinical trial data saved successfully. ${response.samplesUpdated || 0} sample(s) updated.`,
          });

          resetClinicalForm();
          setSelectedSampleIds([]);
          setClinicalModalOpen(false);

          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          notify({
            kind: NotificationKinds.error,
            title: intl.formatMessage({ id: "notification.error" }),
            subtitle: response.error || response.message || "Unknown error",
          });
        }
      },
    );
  }, [
    clinicalData,
    selectedSampleIds,
    pageData?.id,
    intl,
    notify,
    loadPageSamples,
    onProgressUpdate,
  ]);

  // Handle completing trials
  const handleCompleteTrials = useCallback(() => {
    if (selectedSampleIds.length === 0) {
      notify({
        kind: NotificationKinds.warning,
        title: intl.formatMessage({ id: "notification.warning" }),
        subtitle: "Please select at least one sample to complete.",
      });
      return;
    }

    if (!pageData?.id || String(pageData.id).startsWith("default-")) {
      notify({
        kind: NotificationKinds.error,
        title: intl.formatMessage({ id: "notification.error" }),
        subtitle: "Cannot complete samples: Page not properly initialized.",
      });
      return;
    }

    setLoading(true);

    postToOpenElisServerJsonResponse(
      `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
      JSON.stringify({
        sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
        status: "COMPLETED",
      }),
      (response) => {
        setLoading(false);

        if (response && (response.success || response === 200)) {
          notify({
            kind: NotificationKinds.success,
            title: intl.formatMessage({ id: "notification.success" }),
            subtitle: `Completed trials for ${selectedSampleIds.length} sample(s).`,
          });
          setSelectedSampleIds([]);
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          notify({
            kind: NotificationKinds.error,
            title: intl.formatMessage({ id: "notification.error" }),
            subtitle: "Failed to complete trials. Please try again.",
          });
        }
      },
    );
  }, [
    selectedSampleIds,
    pageData?.id,
    intl,
    notify,
    loadPageSamples,
    onProgressUpdate,
  ]);

  const pendingSamples = useMemo(
    () =>
      samples.filter(
        (s) => s.status === "PENDING" || s.status === "IN_PROGRESS",
      ),
    [samples],
  );

  const completedSamples = useMemo(
    () => samples.filter((s) => s.status === "COMPLETED"),
    [samples],
  );

  // Filter by trial types in history
  const samplesWithPreclinical = useMemo(
    () =>
      completedSamples.filter((s) =>
        (s.trialsHistory || []).some((t) => t.trialType === "PRECLINICAL"),
      ),
    [completedSamples],
  );

  const samplesWithClinical = useMemo(
    () =>
      completedSamples.filter((s) =>
        (s.trialsHistory || []).some((t) => t.trialType === "CLINICAL"),
      ),
    [completedSamples],
  );

  // Get trial counts for a sample
  const getTrialCounts = (trialsHistory) => {
    if (!Array.isArray(trialsHistory))
      return { preclinical: 0, clinical: 0, total: 0 };
    const preclinical = trialsHistory.filter(
      (t) => t.trialType === "PRECLINICAL",
    ).length;
    const clinical = trialsHistory.filter(
      (t) => t.trialType === "CLINICAL",
    ).length;
    return { preclinical, clinical, total: preclinical + clinical };
  };

  const additionalColumns = useMemo(
    () => [
      {
        key: "trialsHistory",
        header: intl.formatMessage({
          id: "virology.trials.column.trialCount",
          defaultMessage: "Trials Logged",
        }),
        render: (value, sample) => {
          const history =
            value || sample?.trialsHistory || sample?.data?.trialsHistory;
          if (!Array.isArray(history) || history.length === 0) return "-";

          const counts = getTrialCounts(history);

          return (
            <div
              style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}
            >
              {counts.preclinical > 0 && (
                <Tag type="cyan" size="sm">
                  {counts.preclinical} Preclinical
                </Tag>
              )}
              {counts.clinical > 0 && (
                <Tag type="purple" size="sm">
                  {counts.clinical} Clinical
                </Tag>
              )}
              <Button
                kind="ghost"
                size="sm"
                renderIcon={View}
                iconDescription="View history"
                hasIconOnly
                onClick={() => {
                  setSelectedSampleHistory({
                    sampleId: sample.externalId || sample.accessionNumber,
                    history: history,
                  });
                  setHistoryModalOpen(true);
                }}
              />
            </div>
          );
        },
      },
      {
        key: "lastTrial",
        header: intl.formatMessage({
          id: "virology.trials.column.lastTrial",
          defaultMessage: "Last Trial",
        }),
        render: (value, sample) => {
          const history = sample?.trialsHistory || sample?.data?.trialsHistory;
          if (!Array.isArray(history) || history.length === 0) return "-";

          const lastTrial = history[history.length - 1];
          return (
            <div>
              <span>{lastTrial?.trialInitiationDate || "-"}</span>
              {lastTrial?.trialType && (
                <Tag
                  type={
                    lastTrial.trialType === "PRECLINICAL" ? "cyan" : "purple"
                  }
                  size="sm"
                  style={{ marginLeft: "0.5rem" }}
                >
                  {lastTrial.trialType === "PRECLINICAL" ? "Pre" : "Clin"}
                </Tag>
              )}
            </div>
          );
        },
      },
      {
        key: "trialPhases",
        header: intl.formatMessage({
          id: "virology.trials.column.phases",
          defaultMessage: "Phases/Species",
        }),
        render: (value, sample) => {
          const history = sample?.trialsHistory || sample?.data?.trialsHistory;
          if (!Array.isArray(history) || history.length === 0) return "-";

          // Collect unique phases and species
          const phases = new Set();
          const species = new Set();
          history.forEach((trial) => {
            if (trial.trialPhase) phases.add(trial.trialPhase);
            if (trial.animalSpecies) species.add(trial.animalSpecies);
          });

          const items = [...Array.from(phases), ...Array.from(species)];
          if (items.length === 0) return "-";

          if (items.length <= 2) {
            return items.join(", ");
          }
          return `${items.slice(0, 2).join(", ")} +${items.length - 2}`;
        },
      },
    ],
    [intl],
  );

  return (
    <div className="virology-trials-page">
      {/* Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="virology.trials.title"
            defaultMessage="Preclinical & Clinical Trials"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="virology.trials.description"
            defaultMessage="Manage preclinical (animal) and clinical (human) trials for vaccine candidates. Both trial types can be logged for the same samples."
          />
        </p>
      </div>

      {/* Action Buttons - Two main options */}
      <div className="page-actions-bar">
        <Button
          kind="primary"
          size="md"
          renderIcon={Chemistry}
          onClick={() => setPreclinicalModalOpen(true)}
          disabled={loading || selectedSampleIds.length === 0}
        >
          <FormattedMessage
            id="virology.trials.preclinical"
            defaultMessage="Preclinical Trials (Animal)"
          />
        </Button>
        <Button
          kind="secondary"
          size="md"
          renderIcon={Hospital}
          onClick={() => setClinicalModalOpen(true)}
          disabled={loading || selectedSampleIds.length === 0}
          style={{ marginLeft: "0.5rem" }}
        >
          <FormattedMessage
            id="virology.trials.clinical"
            defaultMessage="Clinical Trials (Human)"
          />
        </Button>
        <Button
          kind="tertiary"
          size="md"
          renderIcon={Checkmark}
          onClick={handleCompleteTrials}
          disabled={loading || selectedSampleIds.length === 0}
          style={{ marginLeft: "0.5rem" }}
        >
          <FormattedMessage
            id="virology.trials.complete"
            defaultMessage="Complete Trials ({count})"
            values={{ count: selectedSampleIds.length }}
          />
        </Button>
      </div>

      {/* Pending Samples */}
      {pendingSamples.length > 0 && (
        <div className="sample-table-section">
          <div className="table-section-header">
            <h5>
              <FormattedMessage
                id="virology.trials.pending"
                defaultMessage="Samples Pending Trials"
              />
              <Tag type="blue" size="sm" className="count-tag">
                {pendingSamples.length}
              </Tag>
              {selectedSampleIds.length > 0 && (
                <Tag type="purple" size="sm" className="count-tag">
                  {selectedSampleIds.length} selected
                </Tag>
              )}
            </h5>
            <p className="table-section-description">
              <FormattedMessage
                id="virology.trials.pending.description"
                defaultMessage="Select samples to log preclinical or clinical trials. Multiple trials of both types can be logged per sample."
              />
            </p>
          </div>
          <div className="sample-grid-container">
            <SampleGrid
              gridId="trials-pending-samples"
              samples={pendingSamples}
              selectedIds={selectedSampleIds}
              onSelectionChange={setSelectedSampleIds}
              showSelection={true}
              loading={loading}
              additionalColumns={additionalColumns}
            />
          </div>
        </div>
      )}

      {/* Completed Samples - Tabbed view */}
      {completedSamples.length > 0 && (
        <div className="sample-table-section">
          <div className="table-section-header">
            <h5>
              <FormattedMessage
                id="virology.trials.completed"
                defaultMessage="Completed Trials"
              />
              <Tag type="green" size="sm" className="count-tag">
                {completedSamples.length}
              </Tag>
            </h5>
          </div>
          <Tabs
            selectedIndex={activeTab}
            onChange={({ selectedIndex }) => setActiveTab(selectedIndex)}
          >
            <TabList aria-label="Trial types">
              <Tab>
                <FormattedMessage
                  id="virology.trials.all"
                  defaultMessage="All ({count})"
                  values={{ count: completedSamples.length }}
                />
              </Tab>
              <Tab>
                <FormattedMessage
                  id="virology.trials.preclinicalTab"
                  defaultMessage="With Preclinical ({count})"
                  values={{ count: samplesWithPreclinical.length }}
                />
              </Tab>
              <Tab>
                <FormattedMessage
                  id="virology.trials.clinicalTab"
                  defaultMessage="With Clinical ({count})"
                  values={{ count: samplesWithClinical.length }}
                />
              </Tab>
            </TabList>
            <TabPanels>
              <TabPanel>
                <div className="sample-grid-container">
                  <SampleGrid
                    gridId="trials-all-completed"
                    samples={completedSamples}
                    additionalColumns={additionalColumns}
                    showSelection={false}
                  />
                </div>
              </TabPanel>
              <TabPanel>
                <div className="sample-grid-container">
                  <SampleGrid
                    gridId="trials-preclinical-completed"
                    samples={samplesWithPreclinical}
                    additionalColumns={additionalColumns}
                    showSelection={false}
                  />
                </div>
              </TabPanel>
              <TabPanel>
                <div className="sample-grid-container">
                  <SampleGrid
                    gridId="trials-clinical-completed"
                    samples={samplesWithClinical}
                    additionalColumns={additionalColumns}
                    showSelection={false}
                  />
                </div>
              </TabPanel>
            </TabPanels>
          </Tabs>
        </div>
      )}

      {/* Empty State */}
      {!loading && samples.length === 0 && (
        <div className="empty-table-state">
          <p>
            <FormattedMessage
              id="virology.trials.empty"
              defaultMessage="No samples available for trials."
            />
          </p>
        </div>
      )}

      {/* Preclinical Trials Modal */}
      <Modal
        open={preclinicalModalOpen}
        onRequestClose={() => setPreclinicalModalOpen(false)}
        modalHeading={intl.formatMessage({
          id: "virology.trials.preclinical.heading",
          defaultMessage: "Log Preclinical Trial",
        })}
        modalLabel={intl.formatMessage({
          id: "virology.trials.preclinical.label",
          defaultMessage: "Animal Testing (External)",
        })}
        primaryButtonText={intl.formatMessage({
          id: "virology.trials.save",
          defaultMessage: "Save Trial Data",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "button.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestSubmit={handleSavePreclinicalTrial}
        primaryButtonDisabled={
          loading ||
          !preclinicalData.animalSpecies ||
          !preclinicalData.trialInitiationDate
        }
        size="lg"
      >
        <Grid fullWidth>
          <Column lg={8} md={4} sm={4}>
            <DatePicker
              datePickerType="single"
              dateFormat="Y-m-d"
              value={preclinicalData.trialInitiationDate}
              onChange={(dates) =>
                setPreclinicalData({
                  ...preclinicalData,
                  trialInitiationDate: dates[0]
                    ? dates[0].toISOString().split("T")[0]
                    : "",
                })
              }
            >
              <DatePickerInput
                id="preclinical-initiation-date"
                placeholder="YYYY-MM-DD"
                labelText={
                  <span>
                    <FormattedMessage
                      id="virology.trials.initiationDate"
                      defaultMessage="Trial Initiation Date"
                    />
                    {" *"}
                  </span>
                }
              />
            </DatePicker>
          </Column>

          <Column lg={8} md={4} sm={4}>
            <Select
              id="animal-species"
              labelText={
                <span>
                  <FormattedMessage
                    id="virology.trials.animalSpecies"
                    defaultMessage="Animal Species"
                  />
                  {" *"}
                </span>
              }
              value={preclinicalData.animalSpecies}
              onChange={(e) =>
                setPreclinicalData({
                  ...preclinicalData,
                  animalSpecies: e.target.value,
                })
              }
            >
              <SelectItem value="" text="Select species..." />
              <SelectItem value="Mouse" text="Mouse" />
              <SelectItem value="Rat" text="Rat" />
              <SelectItem value="Rabbit" text="Rabbit" />
              <SelectItem value="Guinea Pig" text="Guinea Pig" />
              <SelectItem value="Ferret" text="Ferret" />
              <SelectItem value="Hamster" text="Hamster" />
              <SelectItem value="Non-Human Primate" text="Non-Human Primate" />
              <SelectItem value="Other" text="Other" />
            </Select>
          </Column>

          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="number-of-animals"
              labelText={
                <FormattedMessage
                  id="virology.trials.numberOfAnimals"
                  defaultMessage="Number of Animals"
                />
              }
              type="number"
              value={preclinicalData.numberOfAnimals}
              onChange={(e) =>
                setPreclinicalData({
                  ...preclinicalData,
                  numberOfAnimals: e.target.value,
                })
              }
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="study-design"
              labelText={
                <FormattedMessage
                  id="virology.trials.studyDesign"
                  defaultMessage="Study Design"
                />
              }
              placeholder="e.g., Randomized, double-blind, placebo-controlled"
              value={preclinicalData.studyDesign}
              onChange={(e) =>
                setPreclinicalData({
                  ...preclinicalData,
                  studyDesign: e.target.value,
                })
              }
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <Select
              id="immunogenicity-outcome"
              labelText={
                <FormattedMessage
                  id="virology.trials.immunogenicity"
                  defaultMessage="Immunogenicity Outcome"
                />
              }
              value={preclinicalData.immunogenicityOutcome}
              onChange={(e) =>
                setPreclinicalData({
                  ...preclinicalData,
                  immunogenicityOutcome: e.target.value,
                })
              }
            >
              <SelectItem value="" text="Select outcome..." />
              <SelectItem value="Strong Response" text="Strong Response" />
              <SelectItem value="Moderate Response" text="Moderate Response" />
              <SelectItem value="Weak Response" text="Weak Response" />
              <SelectItem value="No Response" text="No Response" />
              <SelectItem value="Pending" text="Pending" />
            </Select>
          </Column>

          <Column lg={8} md={4} sm={4}>
            <Select
              id="safety-outcome"
              labelText={
                <FormattedMessage
                  id="virology.trials.safetyOutcome"
                  defaultMessage="Safety Outcome"
                />
              }
              value={preclinicalData.safetyOutcome}
              onChange={(e) =>
                setPreclinicalData({
                  ...preclinicalData,
                  safetyOutcome: e.target.value,
                })
              }
            >
              <SelectItem value="" text="Select outcome..." />
              <SelectItem value="Well Tolerated" text="Well Tolerated" />
              <SelectItem
                value="Minor Adverse Events"
                text="Minor Adverse Events"
              />
              <SelectItem
                value="Moderate Adverse Events"
                text="Moderate Adverse Events"
              />
              <SelectItem
                value="Severe Adverse Events"
                text="Severe Adverse Events"
              />
              <SelectItem value="Pending" text="Pending" />
            </Select>
          </Column>

          <Column lg={16} md={8} sm={4}>
            <TextArea
              id="adverse-events"
              labelText={
                <FormattedMessage
                  id="virology.trials.adverseEvents"
                  defaultMessage="Adverse Events (if any)"
                />
              }
              placeholder="Document any adverse events observed..."
              value={preclinicalData.adverseEvents}
              onChange={(e) =>
                setPreclinicalData({
                  ...preclinicalData,
                  adverseEvents: e.target.value,
                })
              }
              rows={3}
            />
          </Column>

          <Column lg={16} md={8} sm={4}>
            <TextArea
              id="preclinical-notes"
              labelText={
                <FormattedMessage
                  id="virology.trials.notes"
                  defaultMessage="Additional Notes"
                />
              }
              placeholder="Any additional notes about the preclinical trial..."
              value={preclinicalData.notes}
              onChange={(e) =>
                setPreclinicalData({
                  ...preclinicalData,
                  notes: e.target.value,
                })
              }
              rows={3}
            />
          </Column>

          <Column lg={16} md={8} sm={4}>
            <div style={{ marginTop: "1rem" }}>
              <Tag type="blue">
                {selectedSampleIds.length} sample(s) selected
              </Tag>
            </div>
          </Column>
        </Grid>
      </Modal>

      {/* Clinical Trials Modal */}
      <Modal
        open={clinicalModalOpen}
        onRequestClose={() => setClinicalModalOpen(false)}
        modalHeading={intl.formatMessage({
          id: "virology.trials.clinical.heading",
          defaultMessage: "Log Clinical Trial",
        })}
        modalLabel={intl.formatMessage({
          id: "virology.trials.clinical.label",
          defaultMessage: "Human Testing (External)",
        })}
        primaryButtonText={intl.formatMessage({
          id: "virology.trials.save",
          defaultMessage: "Save Trial Data",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "button.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestSubmit={handleSaveClinicalTrial}
        primaryButtonDisabled={
          loading ||
          !clinicalData.trialPhase ||
          !clinicalData.trialInitiationDate
        }
        size="lg"
      >
        <Grid fullWidth>
          <Column lg={8} md={4} sm={4}>
            <Select
              id="trial-phase"
              labelText={
                <span>
                  <FormattedMessage
                    id="virology.trials.phase"
                    defaultMessage="Trial Phase"
                  />
                  {" *"}
                </span>
              }
              value={clinicalData.trialPhase}
              onChange={(e) =>
                setClinicalData({
                  ...clinicalData,
                  trialPhase: e.target.value,
                })
              }
            >
              <SelectItem value="" text="Select phase..." />
              <SelectItem value="Phase I" text="Phase I - Safety & Dosage" />
              <SelectItem
                value="Phase II"
                text="Phase II - Efficacy & Side Effects"
              />
              <SelectItem
                value="Phase III"
                text="Phase III - Large Scale Testing"
              />
              <SelectItem value="Phase IV" text="Phase IV - Post-Marketing" />
            </Select>
          </Column>

          <Column lg={8} md={4} sm={4}>
            <DatePicker
              datePickerType="single"
              dateFormat="Y-m-d"
              value={clinicalData.trialInitiationDate}
              onChange={(dates) =>
                setClinicalData({
                  ...clinicalData,
                  trialInitiationDate: dates[0]
                    ? dates[0].toISOString().split("T")[0]
                    : "",
                })
              }
            >
              <DatePickerInput
                id="clinical-initiation-date"
                placeholder="YYYY-MM-DD"
                labelText={
                  <span>
                    <FormattedMessage
                      id="virology.trials.initiationDate"
                      defaultMessage="Trial Initiation Date"
                    />
                    {" *"}
                  </span>
                }
              />
            </DatePicker>
          </Column>

          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="number-of-participants"
              labelText={
                <FormattedMessage
                  id="virology.trials.numberOfParticipants"
                  defaultMessage="Number of Participants"
                />
              }
              type="number"
              value={clinicalData.numberOfParticipants}
              onChange={(e) =>
                setClinicalData({
                  ...clinicalData,
                  numberOfParticipants: e.target.value,
                })
              }
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="primary-endpoint"
              labelText={
                <FormattedMessage
                  id="virology.trials.primaryEndpoint"
                  defaultMessage="Primary Endpoint"
                />
              }
              placeholder="e.g., Seroconversion rate, neutralizing antibody titer"
              value={clinicalData.primaryEndpoint}
              onChange={(e) =>
                setClinicalData({
                  ...clinicalData,
                  primaryEndpoint: e.target.value,
                })
              }
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <Select
              id="efficacy-outcome"
              labelText={
                <FormattedMessage
                  id="virology.trials.efficacyOutcome"
                  defaultMessage="Efficacy Outcome"
                />
              }
              value={clinicalData.efficacyOutcome}
              onChange={(e) =>
                setClinicalData({
                  ...clinicalData,
                  efficacyOutcome: e.target.value,
                })
              }
            >
              <SelectItem value="" text="Select outcome..." />
              <SelectItem
                value="Highly Effective (>90%)"
                text="Highly Effective (>90%)"
              />
              <SelectItem
                value="Effective (70-90%)"
                text="Effective (70-90%)"
              />
              <SelectItem
                value="Moderately Effective (50-70%)"
                text="Moderately Effective (50-70%)"
              />
              <SelectItem
                value="Low Efficacy (<50%)"
                text="Low Efficacy (<50%)"
              />
              <SelectItem value="Pending" text="Pending" />
            </Select>
          </Column>

          <Column lg={8} md={4} sm={4}>
            <Select
              id="clinical-safety-outcome"
              labelText={
                <FormattedMessage
                  id="virology.trials.safetyOutcome"
                  defaultMessage="Safety Outcome"
                />
              }
              value={clinicalData.safetyOutcome}
              onChange={(e) =>
                setClinicalData({
                  ...clinicalData,
                  safetyOutcome: e.target.value,
                })
              }
            >
              <SelectItem value="" text="Select outcome..." />
              <SelectItem value="Well Tolerated" text="Well Tolerated" />
              <SelectItem
                value="Minor Adverse Events"
                text="Minor Adverse Events"
              />
              <SelectItem
                value="Moderate Adverse Events"
                text="Moderate Adverse Events"
              />
              <SelectItem
                value="Serious Adverse Events"
                text="Serious Adverse Events"
              />
              <SelectItem value="Pending" text="Pending" />
            </Select>
          </Column>

          <Column lg={16} md={8} sm={4}>
            <TextArea
              id="clinical-adverse-events"
              labelText={
                <FormattedMessage
                  id="virology.trials.adverseEvents"
                  defaultMessage="Adverse Events (if any)"
                />
              }
              placeholder="Document any adverse events observed..."
              value={clinicalData.adverseEvents}
              onChange={(e) =>
                setClinicalData({
                  ...clinicalData,
                  adverseEvents: e.target.value,
                })
              }
              rows={3}
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <TextInput
              id="regulatory-submission"
              labelText={
                <FormattedMessage
                  id="virology.trials.regulatorySubmission"
                  defaultMessage="Regulatory Submission ID"
                />
              }
              placeholder="e.g., IND-2026-001, EMA-2026-XYZ"
              value={clinicalData.regulatorySubmission}
              onChange={(e) =>
                setClinicalData({
                  ...clinicalData,
                  regulatorySubmission: e.target.value,
                })
              }
            />
          </Column>

          <Column lg={8} md={4} sm={4}>
            <Select
              id="regulatory-status"
              labelText={
                <FormattedMessage
                  id="virology.trials.regulatoryStatus"
                  defaultMessage="Regulatory Status"
                />
              }
              value={clinicalData.regulatoryStatus}
              onChange={(e) =>
                setClinicalData({
                  ...clinicalData,
                  regulatoryStatus: e.target.value,
                })
              }
            >
              <SelectItem value="" text="Select status..." />
              <SelectItem value="Pre-IND" text="Pre-IND" />
              <SelectItem value="IND Submitted" text="IND Submitted" />
              <SelectItem value="IND Approved" text="IND Approved" />
              <SelectItem value="BLA Submitted" text="BLA Submitted" />
              <SelectItem value="BLA Approved" text="BLA Approved" />
              <SelectItem value="EUA Granted" text="EUA Granted" />
              <SelectItem value="Full Approval" text="Full Approval" />
            </Select>
          </Column>

          <Column lg={16} md={8} sm={4}>
            <TextArea
              id="clinical-notes"
              labelText={
                <FormattedMessage
                  id="virology.trials.notes"
                  defaultMessage="Additional Notes"
                />
              }
              placeholder="Any additional notes about the clinical trial..."
              value={clinicalData.notes}
              onChange={(e) =>
                setClinicalData({
                  ...clinicalData,
                  notes: e.target.value,
                })
              }
              rows={3}
            />
          </Column>

          <Column lg={16} md={8} sm={4}>
            <div style={{ marginTop: "1rem" }}>
              <Tag type="purple">
                {selectedSampleIds.length} sample(s) selected
              </Tag>
            </div>
          </Column>
        </Grid>
      </Modal>

      {/* Trials History Modal */}
      <Modal
        open={historyModalOpen}
        onRequestClose={() => {
          setHistoryModalOpen(false);
          setSelectedSampleHistory(null);
        }}
        modalHeading={
          <FormattedMessage
            id="virology.trials.history.title"
            defaultMessage="Trials History - {sampleId}"
            values={{ sampleId: selectedSampleHistory?.sampleId || "" }}
          />
        }
        passiveModal
        size="lg"
      >
        <div style={{ maxHeight: "500px", overflowY: "auto" }}>
          {selectedSampleHistory?.history?.map((trial, index) => (
            <Tile
              key={index}
              style={{
                marginBottom: "1rem",
                border: "1px solid #e0e0e0",
              }}
            >
              <div style={{ marginBottom: "0.5rem" }}>
                <Tag
                  type={trial.trialType === "PRECLINICAL" ? "cyan" : "purple"}
                  size="sm"
                >
                  {trial.trialType === "PRECLINICAL"
                    ? "Preclinical"
                    : "Clinical"}
                </Tag>
                <Tag type="blue" size="sm" style={{ marginLeft: "0.5rem" }}>
                  Trial #{selectedSampleHistory.history.length - index}
                </Tag>
                {index === selectedSampleHistory.history.length - 1 && (
                  <Tag type="green" size="sm" style={{ marginLeft: "0.5rem" }}>
                    Most Recent
                  </Tag>
                )}
              </div>

              <Grid narrow>
                <Column lg={8} md={4} sm={4}>
                  <div style={{ marginBottom: "0.5rem" }}>
                    <strong>Initiation Date:</strong>{" "}
                    {trial.trialInitiationDate || "-"}
                  </div>
                </Column>

                {trial.trialType === "PRECLINICAL" ? (
                  <>
                    <Column lg={8} md={4} sm={4}>
                      <div style={{ marginBottom: "0.5rem" }}>
                        <strong>Animal Species:</strong>{" "}
                        {trial.animalSpecies || "-"}
                      </div>
                    </Column>
                    <Column lg={8} md={4} sm={4}>
                      <div style={{ marginBottom: "0.5rem" }}>
                        <strong>Number of Animals:</strong>{" "}
                        {trial.numberOfAnimals || "-"}
                      </div>
                    </Column>
                    <Column lg={8} md={4} sm={4}>
                      <div style={{ marginBottom: "0.5rem" }}>
                        <strong>Study Design:</strong>{" "}
                        {trial.studyDesign || "-"}
                      </div>
                    </Column>
                    <Column lg={8} md={4} sm={4}>
                      <div style={{ marginBottom: "0.5rem" }}>
                        <strong>Immunogenicity:</strong>{" "}
                        {trial.immunogenicityOutcome || "-"}
                      </div>
                    </Column>
                  </>
                ) : (
                  <>
                    <Column lg={8} md={4} sm={4}>
                      <div style={{ marginBottom: "0.5rem" }}>
                        <strong>Trial Phase:</strong> {trial.trialPhase || "-"}
                      </div>
                    </Column>
                    <Column lg={8} md={4} sm={4}>
                      <div style={{ marginBottom: "0.5rem" }}>
                        <strong>Participants:</strong>{" "}
                        {trial.numberOfParticipants || "-"}
                      </div>
                    </Column>
                    <Column lg={8} md={4} sm={4}>
                      <div style={{ marginBottom: "0.5rem" }}>
                        <strong>Primary Endpoint:</strong>{" "}
                        {trial.primaryEndpoint || "-"}
                      </div>
                    </Column>
                    <Column lg={8} md={4} sm={4}>
                      <div style={{ marginBottom: "0.5rem" }}>
                        <strong>Efficacy:</strong>{" "}
                        {trial.efficacyOutcome || "-"}
                      </div>
                    </Column>
                    <Column lg={8} md={4} sm={4}>
                      <div style={{ marginBottom: "0.5rem" }}>
                        <strong>Regulatory ID:</strong>{" "}
                        {trial.regulatorySubmission || "-"}
                      </div>
                    </Column>
                    <Column lg={8} md={4} sm={4}>
                      <div style={{ marginBottom: "0.5rem" }}>
                        <strong>Regulatory Status:</strong>{" "}
                        {trial.regulatoryStatus || "-"}
                      </div>
                    </Column>
                  </>
                )}

                <Column lg={8} md={4} sm={4}>
                  <div style={{ marginBottom: "0.5rem" }}>
                    <strong>Safety Outcome:</strong>{" "}
                    {trial.safetyOutcome || "-"}
                  </div>
                </Column>

                {trial.adverseEvents && (
                  <Column lg={16}>
                    <div style={{ marginTop: "0.5rem" }}>
                      <strong>Adverse Events:</strong>
                      <p style={{ marginTop: "0.25rem", marginBottom: 0 }}>
                        {trial.adverseEvents}
                      </p>
                    </div>
                  </Column>
                )}

                {trial.notes && (
                  <Column lg={16}>
                    <div
                      style={{
                        marginTop: "0.5rem",
                        padding: "0.5rem",
                        backgroundColor: "#f4f4f4",
                        borderRadius: "4px",
                      }}
                    >
                      <strong>Notes:</strong>
                      <p style={{ marginTop: "0.25rem", marginBottom: 0 }}>
                        {trial.notes}
                      </p>
                    </div>
                  </Column>
                )}
              </Grid>
            </Tile>
          ))}

          {(!selectedSampleHistory?.history ||
            selectedSampleHistory.history.length === 0) && (
            <p
              style={{ textAlign: "center", color: "#6f6f6f", padding: "2rem" }}
            >
              No trials history available.
            </p>
          )}
        </div>
      </Modal>
    </div>
  );
}

export default VirologyTrialsPage;
