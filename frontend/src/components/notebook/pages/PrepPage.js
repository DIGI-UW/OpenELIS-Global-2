import React, { useState, useEffect, useRef, useCallback } from "react";
import {
  Grid,
  Column,
  Button,
  TextInput,
  TextArea,
  Dropdown,
  DatePicker,
  DatePickerInput,
  NumberInput,
  InlineNotification,
  Loading,
  Modal,
  Tag,
  Toggle,
  Select,
  SelectItem,
  Accordion,
  AccordionItem,
  Checkbox,
  TimePicker,
  TimePickerSelect,
} from "@carbon/react";
import {
  Add,
  CheckmarkFilled,
  Chemistry,
  Temperature,
  Time,
  Renew,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer, postToOpenElisServer } from "../../utils/Utils";
import SampleGrid from "../workflow/SampleGrid";

/**
 * PrepPage - Page 5: Analysis Preparation (Plate Setup)
 *
 * STAGE 5: Analysis Preparation
 *
 * Fresh Analysis Path:
 * - Proceed directly to assay execution
 * - Verify sample quality before assay
 * - Prepare assay reagents and controls
 *
 * Stored Sample Path:
 * - Retrieve sample from storage
 * - Log retrieval (date, time, personnel, purpose)
 * - Thaw sample (if frozen) following proper protocols
 * - Record thaw date/time
 * - Assess post-thaw viability (if applicable)
 *
 * Incubation Step (If Protocol Requires):
 * - Cell stimulation (if needed for assay)
 * - Record stimulation agent, concentration, duration
 * - Temperature conditions (e.g., 37°C, 5% CO₂)
 * - Start and end times, Operator name
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - Page configuration data
 * @param {Object} props.progress - Page progress info
 * @param {function} props.onProgressUpdate - Callback when progress changes
 */
function PrepPage({ entryId, pageData, progress, onProgressUpdate }) {
  const componentMounted = useRef(true);
  const intl = useIntl();

  // State
  const [loading, setLoading] = useState(true);
  const [samples, setSamples] = useState([]);
  const [selectedIds, setSelectedIds] = useState([]);
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  // Preparation modal state
  const [showPrepModal, setShowPrepModal] = useState(false);
  const [prepData, setPrepData] = useState({
    prepType: "FRESH",
    // Fresh Analysis fields
    sampleQualityVerified: false,
    reagentsPrepared: false,
    controlsPrepared: false,
    qualityNotes: "",
    // Retrieval fields (Stored Sample Path)
    retrievalDate: new Date().toISOString().split("T")[0],
    retrievalTime: "",
    retrievalPersonnel: "",
    retrievalPurpose: "",
    storageLocationRef: "",
    // Thaw fields
    thawDate: new Date().toISOString().split("T")[0],
    thawTime: "",
    thawProtocol: "",
    thawConditions: "",
    // Post-thaw viability assessment
    viabilityAssessmentRequired: false,
    viabilityPercentage: "",
    viabilityMethod: "",
    viabilityNotes: "",
    // Incubation fields
    requiresIncubation: false,
    // Cell stimulation
    stimulationAgent: "",
    stimulationConcentration: "",
    stimulationConcentrationUnit: "ug/mL",
    // Incubation conditions
    incubationDuration: "",
    incubationDurationUnit: "hours",
    incubationTemperature: "37",
    incubationCO2: "5",
    incubationHumidity: "",
    incubationStartTime: "",
    incubationEndTime: "",
    // Common fields
    operatorName: "",
    notes: "",
    prepDate: new Date().toISOString().split("T")[0],
  });

  // Preparation types
  const prepTypes = [
    {
      id: "FRESH",
      text: intl.formatMessage({
        id: "notebook.prep.fresh",
        defaultMessage: "Fresh Analysis",
      }),
    },
    {
      id: "STORED",
      text: intl.formatMessage({
        id: "notebook.prep.stored",
        defaultMessage: "Retrieved from Storage",
      }),
    },
  ];

  // Duration units
  const durationUnits = [
    { id: "minutes", text: "Minutes" },
    { id: "hours", text: "Hours" },
    { id: "days", text: "Days" },
  ];

  // Concentration units
  const concentrationUnits = [
    { id: "ug/mL", text: "µg/mL" },
    { id: "ng/mL", text: "ng/mL" },
    { id: "uM", text: "µM" },
    { id: "nM", text: "nM" },
    { id: "mM", text: "mM" },
    { id: "%", text: "%" },
  ];

  // Viability methods
  const viabilityMethods = [
    { id: "TRYPAN_BLUE", text: "Trypan Blue Exclusion" },
    { id: "FLOW_CYTOMETRY", text: "Flow Cytometry" },
    { id: "MTT_ASSAY", text: "MTT Assay" },
    { id: "VISUAL", text: "Visual Inspection" },
    { id: "OTHER", text: "Other" },
  ];

  // Thaw protocols
  const thawProtocols = [
    { id: "RAPID_37C", text: "Rapid thaw at 37°C water bath" },
    { id: "ROOM_TEMP", text: "Room temperature thaw" },
    { id: "ICE_BATH", text: "Gradual thaw on ice" },
    { id: "CONTROLLED_RATE", text: "Controlled rate thawing" },
    { id: "OTHER", text: "Other protocol" },
  ];

  // Load samples for this specific page (only routed samples)
  const loadSamples = useCallback(() => {
    if (!pageData?.id || String(pageData.id).startsWith("default-")) {
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
              // Routing info (from previous page)
              routingDestination: sample.data?.routingDestination || "",
              wellCoordinate: sample.data?.wellCoordinate || "",
              // Prep-specific data
              prepType: sample.data?.prepType || "",
              operatorName: sample.data?.operatorName || "",
              prepDate: sample.data?.prepDate || "",
              incubationDuration: sample.data?.incubationDuration || "",
              thawConditions: sample.data?.thawConditions || "",
              viabilityPercentage: sample.data?.viabilityPercentage || "",
              stimulationAgent: sample.data?.stimulationAgent || "",
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

  // Reset state and load data when page changes
  useEffect(() => {
    componentMounted.current = true;
    setSelectedIds([]);
    setStatusFilter("ALL");
    setError(null);
    setSuccess(null);
    loadSamples();

    return () => {
      componentMounted.current = false;
    };
  }, [pageData?.id, loadSamples]);

  const handleStatusChange = useCallback(
    (sampleIds, newStatus) => {
      if (!pageData?.id || String(pageData.id).startsWith("default-")) {
        return;
      }

      const numericIds = sampleIds.map((id) => parseInt(id, 10));

      postToOpenElisServer(
        `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
        JSON.stringify({ sampleIds: numericIds, status: newStatus }),
        (response) => {
          if (componentMounted.current) {
            if (response && !response.error) {
              setSuccess(
                intl.formatMessage(
                  {
                    id: "notebook.prep.statusUpdated",
                    defaultMessage: "Updated {count} samples to {status}",
                  },
                  { count: sampleIds.length, status: newStatus },
                ),
              );
              loadSamples();
              if (onProgressUpdate) {
                onProgressUpdate();
              }
            } else {
              setError(response?.error || "Failed to update status");
            }
          }
        },
      );
    },
    [pageData?.id, intl, loadSamples, onProgressUpdate],
  );

  const handleRecordPrep = () => {
    if (selectedIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.prep.noSamplesSelected",
          defaultMessage: "Please select samples to record preparation data",
        }),
      );
      return;
    }
    setShowPrepModal(true);
  };

  const handleSavePrepData = () => {
    if (!prepData.prepType) {
      setError(
        intl.formatMessage({
          id: "notebook.prep.prepTypeRequired",
          defaultMessage: "Preparation type is required",
        }),
      );
      return;
    }

    // Validate fresh analysis fields
    if (prepData.prepType === "FRESH" && !prepData.sampleQualityVerified) {
      setError(
        intl.formatMessage({
          id: "notebook.prep.qualityVerificationRequired",
          defaultMessage:
            "Please verify sample quality before proceeding with fresh analysis",
        }),
      );
      return;
    }

    // Validate stored sample fields
    if (prepData.prepType === "STORED") {
      if (!prepData.retrievalPersonnel) {
        setError(
          intl.formatMessage({
            id: "notebook.prep.retrievalPersonnelRequired",
            defaultMessage: "Retrieval personnel is required",
          }),
        );
        return;
      }
      if (!prepData.retrievalPurpose) {
        setError(
          intl.formatMessage({
            id: "notebook.prep.retrievalPurposeRequired",
            defaultMessage: "Retrieval purpose is required",
          }),
        );
        return;
      }
    }

    // Validate incubation fields if required
    if (prepData.requiresIncubation && !prepData.incubationDuration) {
      setError(
        intl.formatMessage({
          id: "notebook.prep.incubationDurationRequired",
          defaultMessage: "Incubation duration is required",
        }),
      );
      return;
    }

    if (!pageData?.id || String(pageData.id).startsWith("default-")) {
      setShowPrepModal(false);
      return;
    }

    const numericIds = selectedIds.map((id) => parseInt(id, 10));

    // Build prep data object based on prep type
    const dataToSave = {
      prepType: prepData.prepType,
      operatorName: prepData.operatorName,
      notes: prepData.notes,
      prepDate: prepData.prepDate,
    };

    // Add Fresh Analysis specific fields
    if (prepData.prepType === "FRESH") {
      dataToSave.sampleQualityVerified = prepData.sampleQualityVerified;
      dataToSave.reagentsPrepared = prepData.reagentsPrepared;
      dataToSave.controlsPrepared = prepData.controlsPrepared;
      dataToSave.qualityNotes = prepData.qualityNotes;
    }

    // Add Stored Sample specific fields
    if (prepData.prepType === "STORED") {
      // Retrieval info
      dataToSave.retrievalDate = prepData.retrievalDate;
      dataToSave.retrievalTime = prepData.retrievalTime;
      dataToSave.retrievalPersonnel = prepData.retrievalPersonnel;
      dataToSave.retrievalPurpose = prepData.retrievalPurpose;
      dataToSave.storageLocationRef = prepData.storageLocationRef;
      // Thaw info
      dataToSave.thawDate = prepData.thawDate;
      dataToSave.thawTime = prepData.thawTime;
      dataToSave.thawProtocol = prepData.thawProtocol;
      dataToSave.thawConditions = prepData.thawConditions;
      // Viability assessment
      if (prepData.viabilityAssessmentRequired) {
        dataToSave.viabilityAssessmentRequired = true;
        dataToSave.viabilityPercentage = prepData.viabilityPercentage;
        dataToSave.viabilityMethod = prepData.viabilityMethod;
        dataToSave.viabilityNotes = prepData.viabilityNotes;
      }
    }

    // Add Incubation fields if required
    if (prepData.requiresIncubation) {
      dataToSave.requiresIncubation = true;
      dataToSave.stimulationAgent = prepData.stimulationAgent;
      dataToSave.stimulationConcentration = prepData.stimulationConcentration;
      dataToSave.stimulationConcentrationUnit =
        prepData.stimulationConcentrationUnit;
      dataToSave.incubationDuration = prepData.incubationDuration;
      dataToSave.incubationDurationUnit = prepData.incubationDurationUnit;
      dataToSave.incubationTemperature = prepData.incubationTemperature;
      dataToSave.incubationCO2 = prepData.incubationCO2;
      dataToSave.incubationHumidity = prepData.incubationHumidity;
      dataToSave.incubationStartTime = prepData.incubationStartTime;
      dataToSave.incubationEndTime = prepData.incubationEndTime;
    }

    // Save prep data to samples
    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
      JSON.stringify({
        sampleIds: numericIds,
        data: dataToSave,
      }),
      (response) => {
        if (componentMounted.current) {
          if (response && !response.error) {
            // Update status to IN_PROGRESS since prep work has started
            postToOpenElisServer(
              `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
              JSON.stringify({
                sampleIds: numericIds,
                status: "IN_PROGRESS",
              }),
              () => {
                setSuccess(
                  intl.formatMessage(
                    {
                      id: "notebook.prep.dataSaved",
                      defaultMessage:
                        "Preparation data saved for {count} samples",
                    },
                    { count: selectedIds.length },
                  ),
                );
                setShowPrepModal(false);
                setSelectedIds([]);
                // Reset form
                resetPrepData();
                loadSamples();
                if (onProgressUpdate) {
                  onProgressUpdate();
                }
              },
            );
          } else {
            setError(response?.error || "Failed to save preparation data");
          }
        }
      },
    );
  };

  const resetPrepData = () => {
    setPrepData({
      prepType: "FRESH",
      sampleQualityVerified: false,
      reagentsPrepared: false,
      controlsPrepared: false,
      qualityNotes: "",
      retrievalDate: new Date().toISOString().split("T")[0],
      retrievalTime: "",
      retrievalPersonnel: "",
      retrievalPurpose: "",
      storageLocationRef: "",
      thawDate: new Date().toISOString().split("T")[0],
      thawTime: "",
      thawProtocol: "",
      thawConditions: "",
      viabilityAssessmentRequired: false,
      viabilityPercentage: "",
      viabilityMethod: "",
      viabilityNotes: "",
      requiresIncubation: false,
      stimulationAgent: "",
      stimulationConcentration: "",
      stimulationConcentrationUnit: "ug/mL",
      incubationDuration: "",
      incubationDurationUnit: "hours",
      incubationTemperature: "37",
      incubationCO2: "5",
      incubationHumidity: "",
      incubationStartTime: "",
      incubationEndTime: "",
      operatorName: "",
      notes: "",
      prepDate: new Date().toISOString().split("T")[0],
    });
  };

  const handleMarkPrepared = () => {
    if (selectedIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.prep.noSamplesSelected",
          defaultMessage: "Please select samples to mark as prepared",
        }),
      );
      return;
    }
    handleStatusChange(selectedIds, "COMPLETED");
    setSelectedIds([]);
  };

  // Get prep type display
  const getPrepTypeTag = (prepType) => {
    switch (prepType) {
      case "FRESH":
        return (
          <Tag type="green" size="sm">
            Fresh
          </Tag>
        );
      case "STORED":
        return (
          <Tag type="blue" size="sm">
            From Storage
          </Tag>
        );
      case "THAWED":
        return (
          <Tag type="blue" size="sm">
            Thawed
          </Tag>
        );
      case "INCUBATED":
        return (
          <Tag type="purple" size="sm">
            Incubated
          </Tag>
        );
      default:
        return null;
    }
  };

  // Render prep info column
  const renderPrepInfo = (value, sample) => {
    const s = sample || value;
    if (s?.prepType) {
      return (
        <div style={{ fontSize: "12px" }}>
          {getPrepTypeTag(s.prepType)}
          {s.operatorName && (
            <span style={{ marginLeft: "4px", color: "#525252" }}>
              by {s.operatorName}
            </span>
          )}
          {s.stimulationAgent && (
            <div style={{ marginTop: "2px", color: "#525252" }}>
              <Chemistry size={12} style={{ marginRight: "4px" }} />
              {s.stimulationAgent}
            </div>
          )}
          {s.viabilityPercentage && (
            <div style={{ marginTop: "2px", color: "#525252" }}>
              Viability: {s.viabilityPercentage}%
            </div>
          )}
        </div>
      );
    }
    return (
      <span style={{ color: "#8d8d8d", fontSize: "12px" }}>
        <FormattedMessage
          id="notebook.prep.notPrepared"
          defaultMessage="Not prepared"
        />
      </span>
    );
  };

  if (loading) {
    return (
      <div style={{ padding: "2rem", textAlign: "center" }}>
        <Loading withOverlay={false} description="Loading samples..." />
      </div>
    );
  }

  return (
    <div className="prep-page">
      {error && (
        <InlineNotification
          kind="error"
          title={intl.formatMessage({
            id: "error",
            defaultMessage: "Error",
          })}
          subtitle={error}
          onCloseButtonClick={() => setError(null)}
          style={{ marginBottom: "1rem" }}
        />
      )}

      {success && (
        <InlineNotification
          kind="success"
          title={intl.formatMessage({
            id: "success",
            defaultMessage: "Success",
          })}
          subtitle={success}
          onCloseButtonClick={() => setSuccess(null)}
          style={{ marginBottom: "1rem" }}
        />
      )}

      <Grid fullWidth>
        <Column lg={16} md={8} sm={4}>
          <div className="page-instructions" style={{ marginBottom: "1.5rem" }}>
            <h5 style={{ marginBottom: "0.5rem" }}>
              <FormattedMessage
                id="notebook.prep.title"
                defaultMessage="Analysis Preparation (Plate Setup)"
              />
            </h5>
            <p style={{ color: "#525252" }}>
              <FormattedMessage
                id="notebook.prep.instructions"
                defaultMessage="Prepare samples for analysis. For fresh samples, verify quality and prepare reagents. For stored samples, log retrieval details, thawing protocol, and assess post-thaw viability. Record incubation/stimulation conditions if required by the protocol."
              />
            </p>
          </div>
        </Column>

        {/* Bulk Actions */}
        <Column lg={16} md={8} sm={4}>
          <div
            className="bulk-actions"
            style={{
              display: "flex",
              gap: "1rem",
              marginBottom: "1rem",
              padding: "1rem",
              backgroundColor: "#f4f4f4",
              borderRadius: "4px",
            }}
          >
            <Button
              kind="primary"
              size="md"
              renderIcon={Add}
              onClick={handleRecordPrep}
              disabled={selectedIds.length === 0}
            >
              <FormattedMessage
                id="notebook.prep.recordPrep"
                defaultMessage="Record Preparation"
              />
            </Button>

            <Button
              kind="secondary"
              size="md"
              renderIcon={CheckmarkFilled}
              onClick={handleMarkPrepared}
              disabled={selectedIds.length === 0}
            >
              <FormattedMessage
                id="notebook.prep.markPrepared"
                defaultMessage="Mark Prepared"
              />
            </Button>

            <Button
              kind="ghost"
              size="md"
              renderIcon={Renew}
              onClick={loadSamples}
            >
              <FormattedMessage
                id="notebook.prep.refresh"
                defaultMessage="Refresh"
              />
            </Button>

            {selectedIds.length > 0 && (
              <span style={{ alignSelf: "center", color: "#525252" }}>
                <FormattedMessage
                  id="notebook.prep.selectedCount"
                  defaultMessage="{count} samples selected"
                  values={{ count: selectedIds.length }}
                />
              </span>
            )}
          </div>
        </Column>

        {/* Sample Grid */}
        <Column lg={16} md={8} sm={4}>
          <SampleGrid
            gridId="prep"
            samples={samples}
            selectedIds={selectedIds}
            onSelectionChange={setSelectedIds}
            onStatusChange={handleStatusChange}
            statusFilter={statusFilter}
            onStatusFilterChange={setStatusFilter}
            showSelection={true}
            loading={loading}
            additionalColumns={[
              {
                key: "prepInfo",
                header: intl.formatMessage({
                  id: "notebook.prep.prepInfo",
                  defaultMessage: "Preparation Info",
                }),
                render: renderPrepInfo,
              },
            ]}
          />
        </Column>
      </Grid>

      {/* Record Preparation Modal */}
      <Modal
        open={showPrepModal}
        onRequestClose={() => setShowPrepModal(false)}
        onRequestSubmit={handleSavePrepData}
        modalHeading={intl.formatMessage({
          id: "notebook.prep.recordPrepTitle",
          defaultMessage: "Record Preparation Data",
        })}
        primaryButtonText={intl.formatMessage({
          id: "notebook.prep.save",
          defaultMessage: "Save",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "notebook.prep.cancel",
          defaultMessage: "Cancel",
        })}
        size="lg"
      >
        <div style={{ marginBottom: "1rem" }}>
          <p style={{ color: "#525252", marginBottom: "1rem" }}>
            <FormattedMessage
              id="notebook.prep.applyToSelected"
              defaultMessage="This will apply preparation data to {count} selected samples."
              values={{ count: selectedIds.length }}
            />
          </p>

          {/* Preparation Type */}
          <Dropdown
            id="prep-type"
            titleText={intl.formatMessage({
              id: "notebook.prep.prepType",
              defaultMessage: "Sample Source",
            })}
            label={intl.formatMessage({
              id: "notebook.prep.selectPrepType",
              defaultMessage: "Select sample source",
            })}
            items={prepTypes}
            itemToString={(item) => (item ? item.text : "")}
            selectedItem={prepTypes.find((p) => p.id === prepData.prepType)}
            onChange={({ selectedItem }) =>
              setPrepData({
                ...prepData,
                prepType: selectedItem?.id || "FRESH",
              })
            }
            style={{ marginBottom: "1rem" }}
          />

          <Accordion>
            {/* Fresh Analysis Section */}
            {prepData.prepType === "FRESH" && (
              <AccordionItem
                title={intl.formatMessage({
                  id: "notebook.prep.freshAnalysisSection",
                  defaultMessage: "Fresh Analysis Preparation",
                })}
                open
              >
                <div style={{ padding: "0.5rem 0" }}>
                  <p
                    style={{
                      color: "#525252",
                      marginBottom: "1rem",
                      fontSize: "0.875rem",
                    }}
                  >
                    <FormattedMessage
                      id="notebook.prep.freshAnalysisInstructions"
                      defaultMessage="Verify sample quality and prepare assay reagents and controls before proceeding to assay execution."
                    />
                  </p>

                  <Checkbox
                    id="sample-quality-verified"
                    labelText={intl.formatMessage({
                      id: "notebook.prep.sampleQualityVerified",
                      defaultMessage: "Sample quality verified before assay *",
                    })}
                    checked={prepData.sampleQualityVerified}
                    onChange={(e, { checked }) =>
                      setPrepData({
                        ...prepData,
                        sampleQualityVerified: checked,
                      })
                    }
                    style={{ marginBottom: "0.5rem" }}
                  />

                  <Checkbox
                    id="reagents-prepared"
                    labelText={intl.formatMessage({
                      id: "notebook.prep.reagentsPrepared",
                      defaultMessage: "Assay reagents prepared",
                    })}
                    checked={prepData.reagentsPrepared}
                    onChange={(e, { checked }) =>
                      setPrepData({ ...prepData, reagentsPrepared: checked })
                    }
                    style={{ marginBottom: "0.5rem" }}
                  />

                  <Checkbox
                    id="controls-prepared"
                    labelText={intl.formatMessage({
                      id: "notebook.prep.controlsPrepared",
                      defaultMessage: "Controls prepared",
                    })}
                    checked={prepData.controlsPrepared}
                    onChange={(e, { checked }) =>
                      setPrepData({ ...prepData, controlsPrepared: checked })
                    }
                    style={{ marginBottom: "1rem" }}
                  />

                  <TextArea
                    id="quality-notes"
                    labelText={intl.formatMessage({
                      id: "notebook.prep.qualityNotes",
                      defaultMessage: "Quality Assessment Notes",
                    })}
                    value={prepData.qualityNotes}
                    onChange={(e) =>
                      setPrepData({ ...prepData, qualityNotes: e.target.value })
                    }
                    placeholder="Document any quality observations, reagent lot numbers, etc."
                    rows={2}
                  />
                </div>
              </AccordionItem>
            )}

            {/* Stored Sample Path - Retrieval Section */}
            {prepData.prepType === "STORED" && (
              <AccordionItem
                title={intl.formatMessage({
                  id: "notebook.prep.retrievalSection",
                  defaultMessage: "Sample Retrieval",
                })}
                open
              >
                <div style={{ padding: "0.5rem 0" }}>
                  <p
                    style={{
                      color: "#525252",
                      marginBottom: "1rem",
                      fontSize: "0.875rem",
                    }}
                  >
                    <FormattedMessage
                      id="notebook.prep.retrievalInstructions"
                      defaultMessage="Log retrieval details including date, time, personnel, and purpose."
                    />
                  </p>

                  <Grid narrow>
                    <Column lg={8} md={4} sm={4}>
                      <DatePicker
                        datePickerType="single"
                        onChange={([date]) =>
                          setPrepData({
                            ...prepData,
                            retrievalDate:
                              date?.toISOString().split("T")[0] || "",
                          })
                        }
                      >
                        <DatePickerInput
                          id="retrieval-date"
                          labelText={intl.formatMessage({
                            id: "notebook.prep.retrievalDate",
                            defaultMessage: "Retrieval Date *",
                          })}
                          placeholder="mm/dd/yyyy"
                        />
                      </DatePicker>
                    </Column>
                    <Column lg={8} md={4} sm={4}>
                      <TextInput
                        id="retrieval-time"
                        labelText={intl.formatMessage({
                          id: "notebook.prep.retrievalTime",
                          defaultMessage: "Retrieval Time",
                        })}
                        value={prepData.retrievalTime}
                        onChange={(e) =>
                          setPrepData({
                            ...prepData,
                            retrievalTime: e.target.value,
                          })
                        }
                        placeholder="HH:MM"
                      />
                    </Column>
                  </Grid>

                  <TextInput
                    id="retrieval-personnel"
                    labelText={intl.formatMessage({
                      id: "notebook.prep.retrievalPersonnel",
                      defaultMessage: "Retrieved By (Personnel) *",
                    })}
                    value={prepData.retrievalPersonnel}
                    onChange={(e) =>
                      setPrepData({
                        ...prepData,
                        retrievalPersonnel: e.target.value,
                      })
                    }
                    style={{ marginTop: "1rem" }}
                  />

                  <TextInput
                    id="retrieval-purpose"
                    labelText={intl.formatMessage({
                      id: "notebook.prep.retrievalPurpose",
                      defaultMessage: "Purpose of Retrieval *",
                    })}
                    value={prepData.retrievalPurpose}
                    onChange={(e) =>
                      setPrepData({
                        ...prepData,
                        retrievalPurpose: e.target.value,
                      })
                    }
                    placeholder="e.g., ELISA testing, Flow cytometry analysis"
                    style={{ marginTop: "1rem" }}
                  />

                  <TextInput
                    id="storage-location-ref"
                    labelText={intl.formatMessage({
                      id: "notebook.prep.storageLocationRef",
                      defaultMessage: "Storage Location Reference",
                    })}
                    value={prepData.storageLocationRef}
                    onChange={(e) =>
                      setPrepData({
                        ...prepData,
                        storageLocationRef: e.target.value,
                      })
                    }
                    placeholder="e.g., Freezer A > Rack 2 > Box 5 > Position A1"
                    style={{ marginTop: "1rem" }}
                  />
                </div>
              </AccordionItem>
            )}

            {/* Stored Sample Path - Thawing Section */}
            {prepData.prepType === "STORED" && (
              <AccordionItem
                title={intl.formatMessage({
                  id: "notebook.prep.thawingSection",
                  defaultMessage: "Thawing Protocol",
                })}
                open
              >
                <div style={{ padding: "0.5rem 0" }}>
                  <p
                    style={{
                      color: "#525252",
                      marginBottom: "1rem",
                      fontSize: "0.875rem",
                    }}
                  >
                    <FormattedMessage
                      id="notebook.prep.thawingInstructions"
                      defaultMessage="Record thawing details following proper protocols."
                    />
                  </p>

                  <Grid narrow>
                    <Column lg={8} md={4} sm={4}>
                      <DatePicker
                        datePickerType="single"
                        onChange={([date]) =>
                          setPrepData({
                            ...prepData,
                            thawDate: date?.toISOString().split("T")[0] || "",
                          })
                        }
                      >
                        <DatePickerInput
                          id="thaw-date"
                          labelText={intl.formatMessage({
                            id: "notebook.prep.thawDate",
                            defaultMessage: "Thaw Date",
                          })}
                          placeholder="mm/dd/yyyy"
                        />
                      </DatePicker>
                    </Column>
                    <Column lg={8} md={4} sm={4}>
                      <TextInput
                        id="thaw-time"
                        labelText={intl.formatMessage({
                          id: "notebook.prep.thawTime",
                          defaultMessage: "Thaw Time",
                        })}
                        value={prepData.thawTime}
                        onChange={(e) =>
                          setPrepData({ ...prepData, thawTime: e.target.value })
                        }
                        placeholder="HH:MM"
                      />
                    </Column>
                  </Grid>

                  <Dropdown
                    id="thaw-protocol"
                    titleText={intl.formatMessage({
                      id: "notebook.prep.thawProtocol",
                      defaultMessage: "Thaw Protocol",
                    })}
                    label="Select thaw protocol"
                    items={thawProtocols}
                    itemToString={(item) => (item ? item.text : "")}
                    selectedItem={thawProtocols.find(
                      (p) => p.id === prepData.thawProtocol,
                    )}
                    onChange={({ selectedItem }) =>
                      setPrepData({
                        ...prepData,
                        thawProtocol: selectedItem?.id || "",
                      })
                    }
                    style={{ marginTop: "1rem" }}
                  />

                  <TextArea
                    id="thaw-conditions"
                    labelText={intl.formatMessage({
                      id: "notebook.prep.thawConditions",
                      defaultMessage: "Thaw Conditions / Notes",
                    })}
                    value={prepData.thawConditions}
                    onChange={(e) =>
                      setPrepData({
                        ...prepData,
                        thawConditions: e.target.value,
                      })
                    }
                    placeholder="Document any special conditions or observations during thawing"
                    rows={2}
                    style={{ marginTop: "1rem" }}
                  />
                </div>
              </AccordionItem>
            )}

            {/* Stored Sample Path - Post-Thaw Viability */}
            {prepData.prepType === "STORED" && (
              <AccordionItem
                title={intl.formatMessage({
                  id: "notebook.prep.viabilitySection",
                  defaultMessage: "Post-Thaw Viability Assessment",
                })}
              >
                <div style={{ padding: "0.5rem 0" }}>
                  <Toggle
                    id="viability-assessment-required"
                    labelText={intl.formatMessage({
                      id: "notebook.prep.viabilityAssessmentRequired",
                      defaultMessage: "Viability assessment performed",
                    })}
                    toggled={prepData.viabilityAssessmentRequired}
                    onToggle={(checked) =>
                      setPrepData({
                        ...prepData,
                        viabilityAssessmentRequired: checked,
                      })
                    }
                    style={{ marginBottom: "1rem" }}
                  />

                  {prepData.viabilityAssessmentRequired && (
                    <>
                      <Grid narrow>
                        <Column lg={8} md={4} sm={4}>
                          <NumberInput
                            id="viability-percentage"
                            label={intl.formatMessage({
                              id: "notebook.prep.viabilityPercentage",
                              defaultMessage: "Viability (%)",
                            })}
                            value={prepData.viabilityPercentage}
                            onChange={(e, { value }) =>
                              setPrepData({
                                ...prepData,
                                viabilityPercentage: value,
                              })
                            }
                            min={0}
                            max={100}
                            step={0.1}
                          />
                        </Column>
                        <Column lg={8} md={4} sm={4}>
                          <Dropdown
                            id="viability-method"
                            titleText={intl.formatMessage({
                              id: "notebook.prep.viabilityMethod",
                              defaultMessage: "Assessment Method",
                            })}
                            label="Select method"
                            items={viabilityMethods}
                            itemToString={(item) => (item ? item.text : "")}
                            selectedItem={viabilityMethods.find(
                              (m) => m.id === prepData.viabilityMethod,
                            )}
                            onChange={({ selectedItem }) =>
                              setPrepData({
                                ...prepData,
                                viabilityMethod: selectedItem?.id || "",
                              })
                            }
                          />
                        </Column>
                      </Grid>

                      <TextArea
                        id="viability-notes"
                        labelText={intl.formatMessage({
                          id: "notebook.prep.viabilityNotes",
                          defaultMessage: "Viability Assessment Notes",
                        })}
                        value={prepData.viabilityNotes}
                        onChange={(e) =>
                          setPrepData({
                            ...prepData,
                            viabilityNotes: e.target.value,
                          })
                        }
                        placeholder="Document observations, cell count, etc."
                        rows={2}
                        style={{ marginTop: "1rem" }}
                      />
                    </>
                  )}
                </div>
              </AccordionItem>
            )}

            {/* Incubation Section - Available for both paths */}
            <AccordionItem
              title={intl.formatMessage({
                id: "notebook.prep.incubationSection",
                defaultMessage: "Incubation / Cell Stimulation (If Required)",
              })}
            >
              <div style={{ padding: "0.5rem 0" }}>
                <Toggle
                  id="requires-incubation"
                  labelText={intl.formatMessage({
                    id: "notebook.prep.requiresIncubation",
                    defaultMessage: "Protocol requires incubation step",
                  })}
                  toggled={prepData.requiresIncubation}
                  onToggle={(checked) =>
                    setPrepData({ ...prepData, requiresIncubation: checked })
                  }
                  style={{ marginBottom: "1rem" }}
                />

                {prepData.requiresIncubation && (
                  <>
                    <h6 style={{ marginBottom: "0.5rem", color: "#161616" }}>
                      <FormattedMessage
                        id="notebook.prep.cellStimulation"
                        defaultMessage="Cell Stimulation"
                      />
                    </h6>

                    <Grid narrow>
                      <Column lg={8} md={4} sm={4}>
                        <TextInput
                          id="stimulation-agent"
                          labelText={intl.formatMessage({
                            id: "notebook.prep.stimulationAgent",
                            defaultMessage: "Stimulation Agent",
                          })}
                          value={prepData.stimulationAgent}
                          onChange={(e) =>
                            setPrepData({
                              ...prepData,
                              stimulationAgent: e.target.value,
                            })
                          }
                          placeholder="e.g., PMA, Ionomycin, LPS, PHA"
                        />
                      </Column>
                      <Column lg={4} md={2} sm={2}>
                        <NumberInput
                          id="stimulation-concentration"
                          label={intl.formatMessage({
                            id: "notebook.prep.concentration",
                            defaultMessage: "Concentration",
                          })}
                          value={prepData.stimulationConcentration}
                          onChange={(e, { value }) =>
                            setPrepData({
                              ...prepData,
                              stimulationConcentration: value,
                            })
                          }
                          min={0}
                          step={0.1}
                        />
                      </Column>
                      <Column lg={4} md={2} sm={2}>
                        <Dropdown
                          id="concentration-unit"
                          titleText={intl.formatMessage({
                            id: "notebook.prep.unit",
                            defaultMessage: "Unit",
                          })}
                          items={concentrationUnits}
                          itemToString={(item) => (item ? item.text : "")}
                          selectedItem={concentrationUnits.find(
                            (u) =>
                              u.id === prepData.stimulationConcentrationUnit,
                          )}
                          onChange={({ selectedItem }) =>
                            setPrepData({
                              ...prepData,
                              stimulationConcentrationUnit:
                                selectedItem?.id || "ug/mL",
                            })
                          }
                        />
                      </Column>
                    </Grid>

                    <h6
                      style={{
                        marginTop: "1.5rem",
                        marginBottom: "0.5rem",
                        color: "#161616",
                      }}
                    >
                      <FormattedMessage
                        id="notebook.prep.incubationConditions"
                        defaultMessage="Incubation Conditions"
                      />
                    </h6>

                    <Grid narrow>
                      <Column lg={4} md={2} sm={2}>
                        <NumberInput
                          id="incubation-duration"
                          label={intl.formatMessage({
                            id: "notebook.prep.duration",
                            defaultMessage: "Duration *",
                          })}
                          value={prepData.incubationDuration}
                          onChange={(e, { value }) =>
                            setPrepData({
                              ...prepData,
                              incubationDuration: value,
                            })
                          }
                          min={0}
                          step={0.5}
                        />
                      </Column>
                      <Column lg={4} md={2} sm={2}>
                        <Dropdown
                          id="duration-unit"
                          titleText={intl.formatMessage({
                            id: "notebook.prep.unit",
                            defaultMessage: "Unit",
                          })}
                          items={durationUnits}
                          itemToString={(item) => (item ? item.text : "")}
                          selectedItem={durationUnits.find(
                            (u) => u.id === prepData.incubationDurationUnit,
                          )}
                          onChange={({ selectedItem }) =>
                            setPrepData({
                              ...prepData,
                              incubationDurationUnit:
                                selectedItem?.id || "hours",
                            })
                          }
                        />
                      </Column>
                      <Column lg={4} md={2} sm={2}>
                        <TextInput
                          id="incubation-temperature"
                          labelText={intl.formatMessage({
                            id: "notebook.prep.temperature",
                            defaultMessage: "Temperature (°C)",
                          })}
                          value={prepData.incubationTemperature}
                          onChange={(e) =>
                            setPrepData({
                              ...prepData,
                              incubationTemperature: e.target.value,
                            })
                          }
                          placeholder="37"
                        />
                      </Column>
                      <Column lg={4} md={2} sm={2}>
                        <TextInput
                          id="incubation-co2"
                          labelText={intl.formatMessage({
                            id: "notebook.prep.co2",
                            defaultMessage: "CO₂ (%)",
                          })}
                          value={prepData.incubationCO2}
                          onChange={(e) =>
                            setPrepData({
                              ...prepData,
                              incubationCO2: e.target.value,
                            })
                          }
                          placeholder="5"
                        />
                      </Column>
                    </Grid>

                    <TextInput
                      id="incubation-humidity"
                      labelText={intl.formatMessage({
                        id: "notebook.prep.humidity",
                        defaultMessage: "Humidity Conditions",
                      })}
                      value={prepData.incubationHumidity}
                      onChange={(e) =>
                        setPrepData({
                          ...prepData,
                          incubationHumidity: e.target.value,
                        })
                      }
                      placeholder="e.g., 95% humidity"
                      style={{ marginTop: "1rem" }}
                    />

                    <Grid narrow style={{ marginTop: "1rem" }}>
                      <Column lg={8} md={4} sm={4}>
                        <TextInput
                          id="incubation-start-time"
                          labelText={intl.formatMessage({
                            id: "notebook.prep.startTime",
                            defaultMessage: "Start Time",
                          })}
                          value={prepData.incubationStartTime}
                          onChange={(e) =>
                            setPrepData({
                              ...prepData,
                              incubationStartTime: e.target.value,
                            })
                          }
                          placeholder="HH:MM"
                        />
                      </Column>
                      <Column lg={8} md={4} sm={4}>
                        <TextInput
                          id="incubation-end-time"
                          labelText={intl.formatMessage({
                            id: "notebook.prep.endTime",
                            defaultMessage: "End Time",
                          })}
                          value={prepData.incubationEndTime}
                          onChange={(e) =>
                            setPrepData({
                              ...prepData,
                              incubationEndTime: e.target.value,
                            })
                          }
                          placeholder="HH:MM"
                        />
                      </Column>
                    </Grid>
                  </>
                )}
              </div>
            </AccordionItem>

            {/* Common Fields Section */}
            <AccordionItem
              title={intl.formatMessage({
                id: "notebook.prep.commonFieldsSection",
                defaultMessage: "Operator & Notes",
              })}
              open
            >
              <div style={{ padding: "0.5rem 0" }}>
                <TextInput
                  id="operator-name"
                  labelText={intl.formatMessage({
                    id: "notebook.prep.operatorName",
                    defaultMessage: "Operator Name",
                  })}
                  value={prepData.operatorName}
                  onChange={(e) =>
                    setPrepData({ ...prepData, operatorName: e.target.value })
                  }
                  style={{ marginBottom: "1rem" }}
                />

                <TextArea
                  id="notes"
                  labelText={intl.formatMessage({
                    id: "notebook.prep.notes",
                    defaultMessage: "Additional Notes",
                  })}
                  value={prepData.notes}
                  onChange={(e) =>
                    setPrepData({ ...prepData, notes: e.target.value })
                  }
                  rows={2}
                  style={{ marginBottom: "1rem" }}
                />

                <DatePicker
                  datePickerType="single"
                  onChange={([date]) =>
                    setPrepData({
                      ...prepData,
                      prepDate: date?.toISOString().split("T")[0] || "",
                    })
                  }
                >
                  <DatePickerInput
                    id="prep-date"
                    labelText={intl.formatMessage({
                      id: "notebook.prep.prepDate",
                      defaultMessage: "Preparation Date",
                    })}
                    placeholder="mm/dd/yyyy"
                  />
                </DatePicker>
              </div>
            </AccordionItem>
          </Accordion>
        </div>
      </Modal>
    </div>
  );
}

export default PrepPage;
