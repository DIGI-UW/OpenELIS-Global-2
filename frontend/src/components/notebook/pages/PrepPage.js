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
} from "@carbon/react";
import { Add, CheckmarkFilled, Chemistry } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer, postToOpenElisServer } from "../../utils/Utils";
import SampleGrid from "../workflow/SampleGrid";

/**
 * PrepPage - Page 5: Analysis Preparation
 *
 * Allows technicians to prepare routed child samples for analysis:
 * a. Fresh analysis - proceed directly, mark as ready
 * b. Stored samples - log retrieval/thaw from storage, record thaw conditions
 * c. Incubation required - record incubation duration and conditions
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
    // Thaw fields
    retrievalDate: new Date().toISOString().split("T")[0],
    thawConditions: "",
    storageLocationRef: "",
    // Incubation fields
    requiresIncubation: false,
    incubationDuration: "",
    incubationDurationUnit: "minutes",
    incubationTemperature: "",
    incubationConditions: "",
    // Common fields
    technician: "",
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
      id: "THAWED",
      text: intl.formatMessage({
        id: "notebook.prep.thawed",
        defaultMessage: "Thawed from Storage",
      }),
    },
    {
      id: "INCUBATED",
      text: intl.formatMessage({
        id: "notebook.prep.incubated",
        defaultMessage: "Requires Incubation",
      }),
    },
  ];

  // Duration units
  const durationUnits = [
    { id: "minutes", text: "Minutes" },
    { id: "hours", text: "Hours" },
    { id: "days", text: "Days" },
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
              prepTechnician: sample.data?.technician || "",
              prepDate: sample.data?.prepDate || "",
              incubationDuration: sample.data?.incubationDuration || "",
              thawConditions: sample.data?.thawConditions || "",
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

    // Validate thaw fields if type is THAWED
    if (prepData.prepType === "THAWED" && !prepData.thawConditions) {
      setError(
        intl.formatMessage({
          id: "notebook.prep.thawConditionsRequired",
          defaultMessage: "Thaw conditions are required for thawed samples",
        }),
      );
      return;
    }

    // Validate incubation fields if type is INCUBATED or incubation is required
    if (
      (prepData.prepType === "INCUBATED" || prepData.requiresIncubation) &&
      !prepData.incubationDuration
    ) {
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
      technician: prepData.technician,
      notes: prepData.notes,
      prepDate: prepData.prepDate,
    };

    // Add type-specific fields
    if (prepData.prepType === "THAWED") {
      dataToSave.retrievalDate = prepData.retrievalDate;
      dataToSave.thawConditions = prepData.thawConditions;
      dataToSave.storageLocationRef = prepData.storageLocationRef;
    }

    if (prepData.prepType === "INCUBATED" || prepData.requiresIncubation) {
      dataToSave.incubationDuration = prepData.incubationDuration;
      dataToSave.incubationDurationUnit = prepData.incubationDurationUnit;
      dataToSave.incubationTemperature = prepData.incubationTemperature;
      dataToSave.incubationConditions = prepData.incubationConditions;
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
                setPrepData({
                  prepType: "FRESH",
                  retrievalDate: new Date().toISOString().split("T")[0],
                  thawConditions: "",
                  storageLocationRef: "",
                  requiresIncubation: false,
                  incubationDuration: "",
                  incubationDurationUnit: "minutes",
                  incubationTemperature: "",
                  incubationConditions: "",
                  technician: "",
                  notes: "",
                  prepDate: new Date().toISOString().split("T")[0],
                });
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
  const renderPrepInfo = (sample) => {
    if (sample.prepType) {
      return (
        <div style={{ fontSize: "12px" }}>
          {getPrepTypeTag(sample.prepType)}
          {sample.prepTechnician && (
            <span style={{ marginLeft: "4px", color: "#525252" }}>
              by {sample.prepTechnician}
            </span>
          )}
          {sample.incubationDuration && (
            <div style={{ marginTop: "2px", color: "#525252" }}>
              <Chemistry size={12} style={{ marginRight: "4px" }} />
              {sample.incubationDuration}
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
          title={intl.formatMessage({ id: "error" })}
          subtitle={error}
          onCloseButtonClick={() => setError(null)}
          style={{ marginBottom: "1rem" }}
        />
      )}

      {success && (
        <InlineNotification
          kind="success"
          title={intl.formatMessage({ id: "success" })}
          subtitle={success}
          onCloseButtonClick={() => setSuccess(null)}
          style={{ marginBottom: "1rem" }}
        />
      )}

      <Grid fullWidth>
        <Column lg={16} md={8} sm={4}>
          <div className="page-instructions" style={{ marginBottom: "1.5rem" }}>
            <p style={{ color: "#525252" }}>
              <FormattedMessage
                id="notebook.prep.instructions"
                defaultMessage="Prepare routed samples for analysis. Select preparation type: Fresh (proceed directly), Thawed (log retrieval from storage), or Incubated (record incubation conditions)."
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
        size="md"
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
              defaultMessage: "Preparation Type",
            })}
            label={intl.formatMessage({
              id: "notebook.prep.selectPrepType",
              defaultMessage: "Select preparation type",
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

          {/* Thaw Fields - shown when THAWED is selected */}
          {prepData.prepType === "THAWED" && (
            <div
              style={{
                padding: "1rem",
                backgroundColor: "#e0f0ff",
                borderRadius: "4px",
                marginBottom: "1rem",
              }}
            >
              <h5 style={{ marginBottom: "0.5rem" }}>
                <FormattedMessage
                  id="notebook.prep.thawDetails"
                  defaultMessage="Thaw Details"
                />
              </h5>

              <DatePicker
                datePickerType="single"
                onChange={([date]) =>
                  setPrepData({
                    ...prepData,
                    retrievalDate: date?.toISOString().split("T")[0] || "",
                  })
                }
              >
                <DatePickerInput
                  id="retrieval-date"
                  labelText={intl.formatMessage({
                    id: "notebook.prep.retrievalDate",
                    defaultMessage: "Retrieval Date",
                  })}
                  placeholder="mm/dd/yyyy"
                />
              </DatePicker>

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
                placeholder="e.g., Freezer A, Rack 2, Box 5"
                style={{ marginTop: "1rem" }}
              />

              <TextArea
                id="thaw-conditions"
                labelText={intl.formatMessage({
                  id: "notebook.prep.thawConditions",
                  defaultMessage: "Thaw Conditions",
                })}
                value={prepData.thawConditions}
                onChange={(e) =>
                  setPrepData({ ...prepData, thawConditions: e.target.value })
                }
                placeholder="e.g., Room temperature for 30 min, ice bath"
                rows={2}
                style={{ marginTop: "1rem" }}
              />
            </div>
          )}

          {/* Incubation Toggle - shown for FRESH or THAWED */}
          {(prepData.prepType === "FRESH" ||
            prepData.prepType === "THAWED") && (
            <Toggle
              id="requires-incubation"
              labelText={intl.formatMessage({
                id: "notebook.prep.requiresIncubation",
                defaultMessage: "Requires incubation step",
              })}
              toggled={prepData.requiresIncubation}
              onToggle={(checked) =>
                setPrepData({ ...prepData, requiresIncubation: checked })
              }
              style={{ marginBottom: "1rem" }}
            />
          )}

          {/* Incubation Fields - shown when INCUBATED or requiresIncubation */}
          {(prepData.prepType === "INCUBATED" ||
            prepData.requiresIncubation) && (
            <div
              style={{
                padding: "1rem",
                backgroundColor: "#f0e0ff",
                borderRadius: "4px",
                marginBottom: "1rem",
              }}
            >
              <h5 style={{ marginBottom: "0.5rem" }}>
                <FormattedMessage
                  id="notebook.prep.incubationDetails"
                  defaultMessage="Incubation Details"
                />
              </h5>

              <div
                style={{ display: "flex", gap: "1rem", marginBottom: "1rem" }}
              >
                <NumberInput
                  id="incubation-duration"
                  label={intl.formatMessage({
                    id: "notebook.prep.incubationDuration",
                    defaultMessage: "Duration",
                  })}
                  value={prepData.incubationDuration}
                  onChange={(e, { value }) =>
                    setPrepData({
                      ...prepData,
                      incubationDuration: value,
                    })
                  }
                  min={0}
                  step={1}
                  style={{ flex: 1 }}
                />
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
                      incubationDurationUnit: selectedItem?.id || "minutes",
                    })
                  }
                  style={{ flex: 1 }}
                />
              </div>

              <TextInput
                id="incubation-temperature"
                labelText={intl.formatMessage({
                  id: "notebook.prep.incubationTemperature",
                  defaultMessage: "Temperature",
                })}
                value={prepData.incubationTemperature}
                onChange={(e) =>
                  setPrepData({
                    ...prepData,
                    incubationTemperature: e.target.value,
                  })
                }
                placeholder="e.g., 37°C, room temperature"
                style={{ marginBottom: "1rem" }}
              />

              <TextArea
                id="incubation-conditions"
                labelText={intl.formatMessage({
                  id: "notebook.prep.incubationConditions",
                  defaultMessage: "Conditions",
                })}
                value={prepData.incubationConditions}
                onChange={(e) =>
                  setPrepData({
                    ...prepData,
                    incubationConditions: e.target.value,
                  })
                }
                placeholder="e.g., 5% CO2, humidity controlled"
                rows={2}
              />
            </div>
          )}

          {/* Common Fields */}
          <TextInput
            id="technician"
            labelText={intl.formatMessage({
              id: "notebook.prep.technician",
              defaultMessage: "Technician",
            })}
            value={prepData.technician}
            onChange={(e) =>
              setPrepData({ ...prepData, technician: e.target.value })
            }
            style={{ marginBottom: "1rem" }}
          />

          <TextArea
            id="notes"
            labelText={intl.formatMessage({
              id: "notebook.prep.notes",
              defaultMessage: "Notes",
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
      </Modal>
    </div>
  );
}

export default PrepPage;
