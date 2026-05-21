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
  InlineNotification,
  Loading,
  Modal,
  Tag,
} from "@carbon/react";
import { Add, CheckmarkFilled } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer, postToOpenElisServer } from "../../utils/Utils";
import SampleGrid from "../workflow/SampleGrid";

/**
 * AssaysPage - Page 3: Additional Assays
 *
 * Allows technicians to:
 * - Perform supplementary tests or assays prior to extraction
 * - Record test type, operator, reagents used, and results
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - Page configuration data
 * @param {Object} props.progress - Page progress info
 * @param {function} props.onProgressUpdate - Callback when progress changes
 */
function AssaysPage({ entryId, pageData, progress, onProgressUpdate }) {
  const componentMounted = useRef(true);
  const intl = useIntl();

  // State
  const [loading, setLoading] = useState(true);
  const [samples, setSamples] = useState([]);
  const [selectedIds, setSelectedIds] = useState([]);
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  // Assay recording modal state
  const [showAssayModal, setShowAssayModal] = useState(false);
  const [assayData, setAssayData] = useState({
    testType: "",
    operator: "",
    reagentsUsed: "",
    results: "",
    notes: "",
    performedDate: new Date().toISOString().split("T")[0],
  });

  // Common test types for immunology
  const testTypes = [
    { id: "elisa", text: "ELISA" },
    { id: "flow_cytometry", text: "Flow Cytometry" },
    { id: "western_blot", text: "Western Blot" },
    { id: "pcr", text: "PCR" },
    { id: "serology", text: "Serology" },
    { id: "other", text: "Other" },
  ];

  // Load samples for this specific page
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
              // Assay-specific data from the sample's page data
              assayTestType: sample.data?.testType || "",
              assayOperator: sample.data?.operator || "",
              assayResults: sample.data?.results || "",
              assayDate: sample.data?.performedDate || "",
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
    // Reset selection when page changes
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
                    id: "notebook.assays.statusUpdated",
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

  const handleRecordAssay = () => {
    if (selectedIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.assays.noSamplesSelected",
          defaultMessage: "Please select samples to record assay data",
        }),
      );
      return;
    }
    setShowAssayModal(true);
  };

  const handleSaveAssayData = () => {
    if (!assayData.testType) {
      setError(
        intl.formatMessage({
          id: "notebook.assays.testTypeRequired",
          defaultMessage: "Test type is required",
        }),
      );
      return;
    }

    if (!pageData?.id || String(pageData.id).startsWith("default-")) {
      setShowAssayModal(false);
      return;
    }

    const numericIds = selectedIds.map((id) => parseInt(id, 10));

    // Save assay data to samples
    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
      JSON.stringify({
        sampleIds: numericIds,
        data: {
          testType: assayData.testType,
          operator: assayData.operator,
          reagentsUsed: assayData.reagentsUsed,
          results: assayData.results,
          notes: assayData.notes,
          performedDate: assayData.performedDate,
        },
      }),
      (response) => {
        if (componentMounted.current) {
          if (response && !response.error) {
            // Also update status to IN_PROGRESS since assay work has started
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
                      id: "notebook.assays.dataSaved",
                      defaultMessage: "Assay data saved for {count} samples",
                    },
                    { count: selectedIds.length },
                  ),
                );
                setShowAssayModal(false);
                setSelectedIds([]);
                // Reset form
                setAssayData({
                  testType: "",
                  operator: "",
                  reagentsUsed: "",
                  results: "",
                  notes: "",
                  performedDate: new Date().toISOString().split("T")[0],
                });
                loadSamples();
                if (onProgressUpdate) {
                  onProgressUpdate();
                }
              },
            );
          } else {
            setError(response?.error || "Failed to save assay data");
          }
        }
      },
    );
  };

  const handleMarkComplete = () => {
    if (selectedIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.assays.noSamplesSelected",
          defaultMessage: "Please select samples to mark as complete",
        }),
      );
      return;
    }
    handleStatusChange(selectedIds, "COMPLETED");
    setSelectedIds([]);
  };

  // Render assay info column
  // Note: render function receives (value, sample) from SampleGrid
  const renderAssayInfo = (value, sample) => {
    if (sample?.assayTestType) {
      return (
        <div style={{ fontSize: "12px" }}>
          <Tag type="blue" size="sm">
            {sample.assayTestType}
          </Tag>
          {sample.assayOperator && (
            <span style={{ marginLeft: "4px", color: "#525252" }}>
              by {sample.assayOperator}
            </span>
          )}
        </div>
      );
    }
    return (
      <span style={{ color: "#8d8d8d", fontSize: "12px" }}>
        <FormattedMessage
          id="notebook.assays.noAssayRecorded"
          defaultMessage="No assay recorded"
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
    <div className="assays-page">
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
                id="notebook.assays.instructions"
                defaultMessage="Perform supplementary tests or assays prior to extraction. Record test type, operator, reagents used, and results for each sample."
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
              onClick={handleRecordAssay}
              disabled={selectedIds.length === 0}
            >
              <FormattedMessage
                id="notebook.assays.recordAssay"
                defaultMessage="Record Assay Data"
              />
            </Button>

            <Button
              kind="secondary"
              size="md"
              renderIcon={CheckmarkFilled}
              onClick={handleMarkComplete}
              disabled={selectedIds.length === 0}
            >
              <FormattedMessage
                id="notebook.assays.markComplete"
                defaultMessage="Mark Complete"
              />
            </Button>

            {selectedIds.length > 0 && (
              <span style={{ alignSelf: "center", color: "#525252" }}>
                <FormattedMessage
                  id="notebook.assays.selectedCount"
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
            gridId="assays"
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
                key: "assayInfo",
                header: intl.formatMessage({
                  id: "notebook.assays.assayInfo",
                  defaultMessage: "Assay Info",
                }),
                render: renderAssayInfo,
              },
            ]}
          />
        </Column>
      </Grid>

      {/* Record Assay Modal */}
      <Modal
        open={showAssayModal}
        onRequestClose={() => setShowAssayModal(false)}
        onRequestSubmit={handleSaveAssayData}
        modalHeading={intl.formatMessage({
          id: "notebook.assays.recordAssayTitle",
          defaultMessage: "Record Assay Data",
        })}
        primaryButtonText={intl.formatMessage({
          id: "notebook.assays.save",
          defaultMessage: "Save",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "notebook.assays.cancel",
          defaultMessage: "Cancel",
        })}
        size="md"
      >
        <div style={{ marginBottom: "1rem" }}>
          <p style={{ color: "#525252", marginBottom: "1rem" }}>
            <FormattedMessage
              id="notebook.assays.applyToSelected"
              defaultMessage="This will apply assay data to {count} selected samples."
              values={{ count: selectedIds.length }}
            />
          </p>

          <Dropdown
            id="test-type"
            titleText={intl.formatMessage({
              id: "notebook.assays.testType",
              defaultMessage: "Test Type",
            })}
            label={intl.formatMessage({
              id: "notebook.assays.selectTestType",
              defaultMessage: "Select test type",
            })}
            items={testTypes}
            itemToString={(item) => (item ? item.text : "")}
            onChange={({ selectedItem }) =>
              setAssayData({ ...assayData, testType: selectedItem?.text || "" })
            }
            style={{ marginBottom: "1rem" }}
          />

          <TextInput
            id="operator"
            labelText={intl.formatMessage({
              id: "notebook.assays.operator",
              defaultMessage: "Operator",
            })}
            value={assayData.operator}
            onChange={(e) =>
              setAssayData({ ...assayData, operator: e.target.value })
            }
            style={{ marginBottom: "1rem" }}
          />

          <TextArea
            id="reagents"
            labelText={intl.formatMessage({
              id: "notebook.assays.reagentsUsed",
              defaultMessage: "Reagents Used",
            })}
            value={assayData.reagentsUsed}
            onChange={(e) =>
              setAssayData({ ...assayData, reagentsUsed: e.target.value })
            }
            placeholder={intl.formatMessage({
              id: "notebook.assays.reagentsPlaceholder",
              defaultMessage: "List reagents, lot numbers, etc.",
            })}
            rows={3}
            style={{ marginBottom: "1rem" }}
          />

          <TextArea
            id="results"
            labelText={intl.formatMessage({
              id: "notebook.assays.results",
              defaultMessage: "Results",
            })}
            value={assayData.results}
            onChange={(e) =>
              setAssayData({ ...assayData, results: e.target.value })
            }
            placeholder={intl.formatMessage({
              id: "notebook.assays.resultsPlaceholder",
              defaultMessage: "Enter assay results",
            })}
            rows={3}
            style={{ marginBottom: "1rem" }}
          />

          <TextArea
            id="notes"
            labelText={intl.formatMessage({
              id: "notebook.assays.notes",
              defaultMessage: "Notes",
            })}
            value={assayData.notes}
            onChange={(e) =>
              setAssayData({ ...assayData, notes: e.target.value })
            }
            rows={2}
            style={{ marginBottom: "1rem" }}
          />

          <DatePicker
            datePickerType="single"
            onChange={([date]) =>
              setAssayData({
                ...assayData,
                performedDate: date?.toISOString().split("T")[0] || "",
              })
            }
          >
            <DatePickerInput
              id="performed-date"
              labelText={intl.formatMessage({
                id: "notebook.assays.performedDate",
                defaultMessage: "Date Performed",
              })}
              placeholder="mm/dd/yyyy"
            />
          </DatePicker>
        </div>
      </Modal>
    </div>
  );
}

export default AssaysPage;
