import React, { useState, useEffect, useRef, useCallback } from "react";
import {
  Grid,
  Column,
  Button,
  TextInput,
  TextArea,
  Dropdown,
  Select,
  SelectItem,
  DatePicker,
  DatePickerInput,
  InlineNotification,
  Loading,
  Modal,
  Tag,
  Tile,
  Checkbox,
  NumberInput,
  MultiSelect,
} from "@carbon/react";
import {
  Add,
  CheckmarkFilled,
  Warning,
  Renew,
  InventoryManagement,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
} from "../../../utils/Utils";
import SampleGrid from "../../workflow/SampleGrid";
import "../../workflow/NotebookWorkflow.css";

/**
 * PharmaceuticalTestingPage - Page 4: Assay & Test Execution
 *
 * Allows technicians to:
 * - Perform pharmaceutical assays (TLC, UV-Vis, FTIR, HPLC, GC, LC-MS/MS)
 * - Execute physical tests (dissolution, friability, hardness)
 * - Record quality controls (positive/negative, internal standards)
 * - Log deviations with CAPA
 *
 * @param {Object} props
 * @param {number} props.entryId - The notebook entry ID
 * @param {Object} props.pageData - Page configuration data
 * @param {Object} props.progress - Page progress info
 * @param {function} props.onProgressUpdate - Callback when progress changes
 */
function PharmaceuticalTestingPage({
  entryId,
  pageData,
  progress,
  onProgressUpdate,
  templateInstruments,
}) {
  const componentMounted = useRef(true);
  const intl = useIntl();

  // State
  const [loading, setLoading] = useState(true);
  const [samples, setSamples] = useState([]);
  const [selectedIds, setSelectedIds] = useState([]);
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  // Instruments and reagents from inventory
  const [instruments, setInstruments] = useState([]);
  const [loadingInstruments, setLoadingInstruments] = useState(false);
  const [reagents, setReagents] = useState([]);
  const [loadingReagents, setLoadingReagents] = useState(false);

  // Assay recording modal state
  const [showAssayModal, setShowAssayModal] = useState(false);
  const [assayData, setAssayData] = useState({
    assayCategory: "",
    testType: "",
    operator: "",
    results: "",
    notes: "",
    performedDate: new Date().toISOString().split("T")[0],
    // Instruments and reagents used for testing
    selectedInstruments: [],
    selectedReagents: [],
    // QC fields
    positiveControlResult: "",
    negativeControlResult: "",
    internalStandardUsed: false,
    replicateCount: 2,
    acceptanceCriteria: "",
    rsdValue: "",
    // Deviation fields
    hasDeviation: false,
    deviationDescription: "",
    rootCause: "",
    capaAction: "",
  });

  // Assay category and test types for pharmaceuticals
  const assayCategories = [
    { id: "identification", text: "Identification Tests" },
    { id: "potency", text: "Potency / Assay" },
    { id: "impurity", text: "Impurity Profiling" },
    { id: "physical", text: "Physical & Performance Tests" },
    { id: "stability", text: "Stability Testing" },
    { id: "biological", text: "Biological Assays" },
    { id: "microbiological", text: "Microbiological Tests" },
    { id: "herbal", text: "Herbal/Natural Product Assays" },
  ];

  const testTypesByCategory = {
    identification: [
      { id: "tlc", text: "TLC (Thin Layer Chromatography)" },
      { id: "uv_vis", text: "UV/Visible Spectroscopy" },
      { id: "ftir", text: "FTIR (Fourier-Transform Infrared)" },
    ],
    potency: [
      { id: "hplc", text: "HPLC (High-Performance Liquid Chromatography)" },
      { id: "titration", text: "Titration" },
      { id: "uv_vis_spectro", text: "UV/Visible Spectrophotometry" },
    ],
    impurity: [
      { id: "hplc_stability", text: "HPLC (Stability-Indicating)" },
      { id: "gc", text: "Gas Chromatography (GC)" },
      { id: "lc_msms", text: "LC-MS/MS" },
    ],
    physical: [
      { id: "dissolution", text: "Dissolution" },
      { id: "disintegration", text: "Disintegration Time" },
      { id: "friability", text: "Friability" },
      { id: "hardness", text: "Hardness" },
      { id: "viscosity", text: "Viscosity" },
    ],
    stability: [
      { id: "ich_accelerated", text: "ICH Accelerated (40C/75% RH)" },
      { id: "ich_intermediate", text: "ICH Intermediate (30C/65% RH)" },
      { id: "ich_longterm", text: "ICH Long-term (25C/60% RH)" },
    ],
    biological: [
      { id: "clinical_chemistry", text: "Clinical Chemistry" },
      { id: "elisa", text: "ELISA" },
      { id: "hplc_pk", text: "HPLC/LC-MS/MS (Pharmacokinetics)" },
      { id: "pcr", text: "PCR/qPCR" },
      { id: "genotyping", text: "Genotyping" },
    ],
    microbiological: [
      { id: "tamc", text: "TAMC (Total Aerobic Microbial Count)" },
      { id: "tymc", text: "TYMC (Total Yeast & Mold Count)" },
      { id: "sterility", text: "Sterility Test" },
      { id: "lal", text: "LAL Assay (Endotoxin)" },
      { id: "toc", text: "TOC (Total Organic Carbon)" },
      { id: "conductivity", text: "Conductivity" },
      { id: "mic", text: "MIC (Minimum Inhibitory Concentration)" },
      { id: "aet", text: "AET (Antimicrobial Effectiveness)" },
    ],
    herbal: [
      { id: "phytochemical", text: "Phytochemical Screening" },
      { id: "hplc_fingerprint", text: "HPLC Fingerprinting" },
      { id: "bioactivity", text: "Bioactivity Assays" },
      { id: "toxicity", text: "Toxicity Assays" },
    ],
  };

  // Get test types based on selected category
  const availableTestTypes = testTypesByCategory[assayData.assayCategory] || [];

  // Load instruments from template or inventory
  const loadInstruments = useCallback(() => {
    // If template has configured instruments, use those exclusively
    if (templateInstruments && templateInstruments.length > 0) {
      setInstruments(
        templateInstruments.map((analyzer) => ({
          id: analyzer.id,
          label: analyzer.value,
          name: analyzer.value,
        })),
      );
      setLoadingInstruments(false);
      return;
    }

    // Fallback: load from inventory if no template instruments configured
    setLoadingInstruments(true);
    getFromOpenElisServer(
      "/rest/inventory/instruments?status=active",
      (response) => {
        if (componentMounted.current) {
          if (response && Array.isArray(response)) {
            setInstruments(
              response.map((i) => ({
                id: i.id,
                label: `${i.name} (${i.serialNumber || "N/A"})`,
                name: i.name,
                serialNumber: i.serialNumber,
                ...i,
              })),
            );
          }
          setLoadingInstruments(false);
        }
      },
    );
  }, [templateInstruments]);

  // Load reagents from inventory for consumption tracking
  const loadReagents = useCallback(() => {
    setLoadingReagents(true);
    getFromOpenElisServer(
      "/rest/inventory/reagents?status=active",
      (response) => {
        if (componentMounted.current) {
          if (response && Array.isArray(response)) {
            setReagents(
              response.map((r) => ({
                id: r.id,
                label: `${r.name} (Lot: ${r.lotNumber || "N/A"})`,
                name: r.name,
                lotNumber: r.lotNumber,
                ...r,
              })),
            );
          }
          setLoadingReagents(false);
        }
      },
    );
  }, []);

  // Helper to get category display text from ID
  const getCategoryDisplayText = useCallback((categoryId) => {
    if (!categoryId) return "";
    const category = assayCategories.find((c) => c.id === categoryId);
    return category ? category.text : categoryId;
  }, []);

  // Helper to get test type display text from ID
  const getTestTypeDisplayText = useCallback((categoryId, testTypeValue) => {
    if (!testTypeValue) return "";
    // testType is stored as the display text (e.g., "HPLC (High-Performance Liquid Chromatography)")
    // so we can return it directly
    return testTypeValue;
  }, []);

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
            const transformedSamples = response.map((sample) => {
              const categoryId = sample.data?.assayCategory || "";
              const testTypeValue = sample.data?.testType || "";

              return {
                id: String(sample.id || sample.sampleItemId),
                sampleItemId: sample.sampleItemId, // Keep sampleItemId for backend API calls
                externalId: sample.externalId,
                accessionNumber: sample.accessionNumber,
                sampleType:
                  sample.sampleType || sample.typeOfSample?.description,
                collectionDate: sample.collectionDate,
                status: sample.pageStatus || "PENDING",
                // Category and lot can come from multiple sources
                sampleCategory:
                  sample.data?.sampleCategory ||
                  sample.sampleCategory ||
                  sample.typeOfSample?.description ||
                  "",
                lotNumber:
                  sample.data?.lotNumber ||
                  sample.lotNumber ||
                  sample.batchNumber ||
                  "",
                // Assay-specific data - store both ID and display value
                assayCategory: categoryId,
                assayCategoryDisplay: getCategoryDisplayText(categoryId),
                assayTestType: testTypeValue,
                assayTestTypeDisplay: testTypeValue, // Already stored as display text
                assayOperator: sample.data?.operator || "",
                assayResults: sample.data?.results || "",
                assayDate: sample.data?.performedDate || "",
                hasDeviation: sample.data?.hasDeviation || false,
              };
            });
            setSamples(transformedSamples);
          } else {
            setSamples([]);
          }
          setLoading(false);
        }
      },
    );
  }, [pageData?.id, getCategoryDisplayText]);

  useEffect(() => {
    componentMounted.current = true;
    setSelectedIds([]);
    setStatusFilter("ALL");
    setError(null);
    setSuccess(null);
    loadSamples();
    loadInstruments();
    loadReagents();

    return () => {
      componentMounted.current = false;
    };
  }, [pageData?.id, loadSamples, loadInstruments, loadReagents]);

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
                    id: "notebook.pharma.testing.statusUpdated",
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
          id: "notebook.pharma.testing.noSamplesSelected",
          defaultMessage: "Please select samples to record test data",
        }),
      );
      return;
    }
    // Reset form
    setAssayData({
      assayCategory: "",
      testType: "",
      operator: "",
      results: "",
      notes: "",
      performedDate: new Date().toISOString().split("T")[0],
      selectedInstruments: [],
      selectedReagents: [],
      positiveControlResult: "",
      negativeControlResult: "",
      internalStandardUsed: false,
      replicateCount: 2,
      acceptanceCriteria: "",
      rsdValue: "",
      hasDeviation: false,
      deviationDescription: "",
      rootCause: "",
      capaAction: "",
    });
    setShowAssayModal(true);
  };

  const handleSaveAssayData = () => {
    if (!assayData.assayCategory || !assayData.testType) {
      setError(
        intl.formatMessage({
          id: "notebook.pharma.testing.categoryAndTestRequired",
          defaultMessage: "Assay category and test type are required",
        }),
      );
      return;
    }

    if (!pageData?.id || String(pageData.id).startsWith("default-")) {
      setShowAssayModal(false);
      return;
    }

    // Convert selected IDs to sampleItemIds for the backend API
    const sampleItemIds = selectedIds
      .map((id) => {
        const sample = samples.find((s) => s.id === id);
        return sample?.sampleItemId;
      })
      .filter((id) => id != null);

    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
      JSON.stringify({
        sampleIds: sampleItemIds,
        data: {
          assayCategory: assayData.assayCategory,
          testType: assayData.testType,
          operator: assayData.operator,
          results: assayData.results,
          notes: assayData.notes,
          performedDate: assayData.performedDate,
          selectedInstruments: assayData.selectedInstruments,
          selectedReagents: assayData.selectedReagents,
          positiveControlResult: assayData.positiveControlResult,
          negativeControlResult: assayData.negativeControlResult,
          internalStandardUsed: assayData.internalStandardUsed,
          replicateCount: assayData.replicateCount,
          acceptanceCriteria: assayData.acceptanceCriteria,
          rsdValue: assayData.rsdValue,
          hasDeviation: assayData.hasDeviation,
          deviationDescription: assayData.deviationDescription,
          rootCause: assayData.rootCause,
          capaAction: assayData.capaAction,
        },
      }),
      (response) => {
        if (componentMounted.current) {
          if (response && !response.error) {
            // Update status to IN_PROGRESS
            postToOpenElisServer(
              `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
              JSON.stringify({
                sampleIds: sampleItemIds,
                status: "IN_PROGRESS",
              }),
              () => {
                setSuccess(
                  intl.formatMessage(
                    {
                      id: "notebook.pharma.testing.dataSaved",
                      defaultMessage: "Test data saved for {count} samples",
                    },
                    { count: selectedIds.length },
                  ),
                );
                setShowAssayModal(false);
                setSelectedIds([]);
                loadSamples();
                if (onProgressUpdate) {
                  onProgressUpdate();
                }
              },
            );
          } else {
            setError(response?.error || "Failed to save test data");
          }
        }
      },
    );
  };

  const handleMarkComplete = () => {
    if (selectedIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.pharma.testing.noSamplesSelected",
          defaultMessage: "Please select samples to mark as complete",
        }),
      );
      return;
    }
    handleStatusChange(selectedIds, "COMPLETED");
    setSelectedIds([]);
  };

  // Calculate stats
  const completedCount = samples.filter((s) => s.status === "COMPLETED").length;
  const pendingCount = samples.filter((s) => s.status === "PENDING").length;
  const inProgressCount = samples.filter(
    (s) => s.status === "IN_PROGRESS",
  ).length;
  const deviationCount = samples.filter((s) => s.hasDeviation).length;

  // Render status with color-coded tags
  const renderStatus = (status) => {
    const statusConfig = {
      COMPLETED: { type: "green", label: "Completed" },
      IN_PROGRESS: { type: "blue", label: "In Progress" },
      PENDING: { type: "gray", label: "Pending" },
      FAILED: { type: "red", label: "Failed" },
      ON_HOLD: { type: "purple", label: "On Hold" },
    };

    const config = statusConfig[status] || { type: "gray", label: status };

    return (
      <Tag type={config.type} size="sm">
        {config.label}
      </Tag>
    );
  };

  // Render test info column
  const renderTestInfo = (sample) => {
    if (sample.assayTestType) {
      return (
        <div style={{ display: "flex", alignItems: "center", gap: "4px" }}>
          <Tag type="blue" size="sm">
            {sample.assayTestType}
          </Tag>
          {sample.hasDeviation && (
            <Tag type="red" size="sm" renderIcon={Warning}>
              Deviation
            </Tag>
          )}
        </div>
      );
    }
    return (
      <span style={{ color: "#8d8d8d", fontSize: "12px" }}>
        <FormattedMessage
          id="notebook.pharma.testing.noTestRecorded"
          defaultMessage="No test recorded"
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
    <div className="pharma-testing-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.page.pharma.testing.title"
            defaultMessage="Assay &amp; Test Execution"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.pharma.testing.description"
            defaultMessage="Perform analytical testing with data integrity. Execute pharmaceutical assays, record quality controls, and log any deviations with CAPA."
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
                  id="notebook.page.pharma.totalSamples"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{samples.length}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.pharma.testing.tested"
                  defaultMessage="Tested"
                />
              </span>
              <span className="progress-value">{completedCount}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.pharma.testing.pending"
                  defaultMessage="Pending"
                />
              </span>
              <span className="progress-value">{pendingCount}</span>
            </Tile>
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.pharma.testing.inProgress"
                  defaultMessage="In Progress"
                />
              </span>
              <span className="progress-value">{inProgressCount}</span>
            </Tile>
            {deviationCount > 0 && (
              <Tile
                className="progress-tile"
                style={{ borderColor: "#da1e28" }}
              >
                <span className="progress-label" style={{ color: "#da1e28" }}>
                  <FormattedMessage
                    id="notebook.page.pharma.testing.deviations"
                    defaultMessage="Deviations"
                  />
                </span>
                <span className="progress-value" style={{ color: "#da1e28" }}>
                  {deviationCount}
                </span>
              </Tile>
            )}
          </div>
        </Column>
      </Grid>

      {/* Notifications */}
      {error && (
        <InlineNotification
          kind="error"
          title={error}
          onCloseButtonClick={() => setError(null)}
          style={{ marginBottom: "1rem" }}
          lowContrast
        />
      )}

      {success && (
        <InlineNotification
          kind="success"
          title={success}
          onCloseButtonClick={() => setSuccess(null)}
          style={{ marginBottom: "1rem" }}
          lowContrast
        />
      )}

      {/* Action Buttons */}
      <div className="page-actions-bar">
        <Button
          kind="primary"
          size="sm"
          renderIcon={Add}
          onClick={handleRecordAssay}
          disabled={selectedIds.length === 0}
        >
          <FormattedMessage
            id="notebook.pharma.testing.recordTest"
            defaultMessage="Record Test Data ({count})"
            values={{ count: selectedIds.length }}
          />
        </Button>

        <Button
          kind="secondary"
          size="sm"
          renderIcon={CheckmarkFilled}
          onClick={handleMarkComplete}
          disabled={selectedIds.length === 0}
        >
          <FormattedMessage
            id="notebook.pharma.testing.markComplete"
            defaultMessage="Mark Complete"
          />
        </Button>

        <Button
          kind="tertiary"
          size="sm"
          renderIcon={Renew}
          onClick={loadSamples}
        >
          <FormattedMessage
            id="notebook.pharma.testing.refresh"
            defaultMessage="Refresh"
          />
        </Button>
      </div>

      {/* Sample Grid */}
      <div className="sample-grid-container">
        <SampleGrid
          gridId="pharma-testing"
          samples={samples}
          selectedIds={selectedIds}
          onSelectionChange={setSelectedIds}
          onStatusChange={handleStatusChange}
          statusFilter={statusFilter}
          onStatusFilterChange={setStatusFilter}
          showSelection={true}
          loading={loading}
          columns={[
            { key: "accessionNumber", header: "Accession Number" },
            { key: "externalId", header: "Sample ID" },
            {
              key: "assayCategoryDisplay",
              header: "Category",
              render: (value) => value || "-",
            },
            {
              key: "assayTestTypeDisplay",
              header: "Test Type",
              render: (value) => value || "-",
            },
            {
              key: "assayResults",
              header: "Result",
              render: (value) => value || "-",
            },
            {
              key: "status",
              header: "Status",
              render: (value) => renderStatus(value),
            },
          ]}
          additionalColumns={[
            {
              key: "testInfo",
              header: intl.formatMessage({
                id: "notebook.pharma.testing.testInfo",
                defaultMessage: "Test Info",
              }),
              render: renderTestInfo,
            },
          ]}
        />
      </div>

      {/* Empty state */}
      {!loading && samples.length === 0 && (
        <div className="empty-state">
          <p>
            <FormattedMessage
              id="notebook.page.pharma.testing.empty"
              defaultMessage="No samples available for testing. Complete Sample Processing first."
            />
          </p>
        </div>
      )}

      {/* Record Test Modal */}
      <Modal
        open={showAssayModal}
        onRequestClose={() => setShowAssayModal(false)}
        onRequestSubmit={handleSaveAssayData}
        modalHeading={intl.formatMessage({
          id: "notebook.pharma.testing.recordTestTitle",
          defaultMessage: "Record Assay / Test Data",
        })}
        primaryButtonText={intl.formatMessage({
          id: "notebook.pharma.testing.save",
          defaultMessage: "Save",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "notebook.pharma.testing.cancel",
          defaultMessage: "Cancel",
        })}
        size="lg"
      >
        <div style={{ display: "flex", flexDirection: "column", gap: "1rem" }}>
          <p style={{ color: "#525252" }}>
            <FormattedMessage
              id="notebook.pharma.testing.applyToSelected"
              defaultMessage="Recording test data for {count} selected samples."
              values={{ count: selectedIds.length }}
            />
          </p>

          {/* Assay Type Selection */}
          <Grid fullWidth narrow>
            <Column lg={8} md={4} sm={4}>
              <Select
                id="assay-category"
                labelText={intl.formatMessage({
                  id: "notebook.pharma.testing.category",
                  defaultMessage: "Assay Category",
                })}
                value={assayData.assayCategory}
                onChange={(e) =>
                  setAssayData({
                    ...assayData,
                    assayCategory: e.target.value,
                    testType: "",
                  })
                }
              >
                <SelectItem value="" text="Select category..." />
                {assayCategories.map((cat) => (
                  <SelectItem key={cat.id} value={cat.id} text={cat.text} />
                ))}
              </Select>
            </Column>
            <Column lg={8} md={4} sm={4}>
              <Select
                id="test-type"
                labelText={intl.formatMessage({
                  id: "notebook.pharma.testing.testType",
                  defaultMessage: "Test Type",
                })}
                value={assayData.testType}
                onChange={(e) =>
                  setAssayData({ ...assayData, testType: e.target.value })
                }
                disabled={!assayData.assayCategory}
              >
                <SelectItem value="" text="Select test type..." />
                {availableTestTypes.map((test) => (
                  <SelectItem
                    key={test.id}
                    value={test.text}
                    text={test.text}
                  />
                ))}
              </Select>
            </Column>
          </Grid>

          {/* Operator and Date */}
          <Grid fullWidth narrow>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="operator"
                labelText={intl.formatMessage({
                  id: "notebook.pharma.testing.operator",
                  defaultMessage: "Operator / Analyst",
                })}
                value={assayData.operator}
                onChange={(e) =>
                  setAssayData({ ...assayData, operator: e.target.value })
                }
              />
            </Column>
            <Column lg={8} md={4} sm={4}>
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
                    id: "notebook.pharma.testing.performedDate",
                    defaultMessage: "Date Performed",
                  })}
                  placeholder="mm/dd/yyyy"
                />
              </DatePicker>
            </Column>
          </Grid>

          {/* Results */}
          <TextArea
            id="results"
            labelText={intl.formatMessage({
              id: "notebook.pharma.testing.results",
              defaultMessage: "Results",
            })}
            value={assayData.results}
            onChange={(e) =>
              setAssayData({ ...assayData, results: e.target.value })
            }
            placeholder="Enter test results..."
            rows={3}
          />

          {/* Reagent & Instrument Selection */}
          <div style={{ marginTop: "1rem" }}>
            <h5
              style={{
                marginBottom: "0.5rem",
                display: "flex",
                alignItems: "center",
              }}
            >
              <InventoryManagement
                size={16}
                style={{ marginRight: "0.5rem" }}
              />
              <FormattedMessage
                id="notebook.pharma.testing.inventorySelection"
                defaultMessage="Reagent & Instrument Selection"
              />
            </h5>
            <Grid fullWidth narrow>
              <Column lg={8} md={4} sm={4}>
                <MultiSelect
                  id="selectedReagents"
                  titleText={intl.formatMessage({
                    id: "notebook.pharma.testing.reagentsUsed",
                    defaultMessage: "Reagents Used",
                  })}
                  label={intl.formatMessage({
                    id: "notebook.pharma.testing.reagents.placeholder",
                    defaultMessage: "Select reagents...",
                  })}
                  items={reagents}
                  itemToString={(item) => (item ? item.label : "")}
                  selectedItems={reagents.filter((r) =>
                    assayData.selectedReagents.includes(r.id),
                  )}
                  onChange={({ selectedItems }) =>
                    setAssayData({
                      ...assayData,
                      selectedReagents: selectedItems.map((r) => r.id),
                    })
                  }
                  disabled={loadingReagents}
                />
              </Column>
              <Column lg={8} md={4} sm={4}>
                <MultiSelect
                  id="selectedInstruments"
                  titleText={intl.formatMessage({
                    id: "notebook.pharma.testing.instrumentsUsed",
                    defaultMessage: "Instruments Used",
                  })}
                  label={intl.formatMessage({
                    id: "notebook.pharma.testing.instruments.placeholder",
                    defaultMessage: "Select test instruments...",
                  })}
                  items={instruments}
                  itemToString={(item) => (item ? item.label : "")}
                  selectedItems={instruments.filter((i) =>
                    assayData.selectedInstruments.includes(i.id),
                  )}
                  onChange={({ selectedItems }) =>
                    setAssayData({
                      ...assayData,
                      selectedInstruments: selectedItems.map((i) => i.id),
                    })
                  }
                  disabled={loadingInstruments}
                />
              </Column>
            </Grid>
          </div>

          {/* Quality Controls Section */}
          <h5 style={{ marginTop: "0.5rem", marginBottom: "0.5rem" }}>
            <FormattedMessage
              id="notebook.pharma.testing.qualityControls"
              defaultMessage="Quality Controls"
            />
          </h5>

          <Grid fullWidth narrow>
            <Column lg={4} md={4} sm={4}>
              <TextInput
                id="positive-control"
                labelText={intl.formatMessage({
                  id: "notebook.pharma.testing.positiveControl",
                  defaultMessage: "Positive Control",
                })}
                value={assayData.positiveControlResult}
                onChange={(e) =>
                  setAssayData({
                    ...assayData,
                    positiveControlResult: e.target.value,
                  })
                }
                placeholder="Pass/Fail"
              />
            </Column>
            <Column lg={4} md={4} sm={4}>
              <TextInput
                id="negative-control"
                labelText={intl.formatMessage({
                  id: "notebook.pharma.testing.negativeControl",
                  defaultMessage: "Negative Control",
                })}
                value={assayData.negativeControlResult}
                onChange={(e) =>
                  setAssayData({
                    ...assayData,
                    negativeControlResult: e.target.value,
                  })
                }
                placeholder="Pass/Fail"
              />
            </Column>
            <Column lg={4} md={4} sm={4}>
              <NumberInput
                id="replicate-count"
                label={intl.formatMessage({
                  id: "notebook.pharma.testing.replicates",
                  defaultMessage: "Replicates",
                })}
                value={assayData.replicateCount}
                onChange={(e, { value }) =>
                  setAssayData({ ...assayData, replicateCount: value })
                }
                min={1}
                max={10}
              />
            </Column>
            <Column lg={4} md={4} sm={4}>
              <TextInput
                id="rsd-value"
                labelText={intl.formatMessage({
                  id: "notebook.pharma.testing.rsd",
                  defaultMessage: "%RSD / CV",
                })}
                value={assayData.rsdValue}
                onChange={(e) =>
                  setAssayData({ ...assayData, rsdValue: e.target.value })
                }
                placeholder="e.g., 2.5%"
              />
            </Column>
          </Grid>

          <Checkbox
            id="internal-standard"
            labelText={intl.formatMessage({
              id: "notebook.pharma.testing.internalStandard",
              defaultMessage: "Internal Standard Used",
            })}
            checked={assayData.internalStandardUsed}
            onChange={(_, { checked }) =>
              setAssayData({ ...assayData, internalStandardUsed: checked })
            }
          />

          <TextInput
            id="acceptance-criteria"
            labelText={intl.formatMessage({
              id: "notebook.pharma.testing.acceptanceCriteria",
              defaultMessage: "Acceptance Criteria",
            })}
            value={assayData.acceptanceCriteria}
            onChange={(e) =>
              setAssayData({ ...assayData, acceptanceCriteria: e.target.value })
            }
            placeholder="e.g., 95.0% - 105.0% of label claim"
          />

          {/* Deviation Section */}
          <h5 style={{ marginTop: "0.5rem", marginBottom: "0.5rem" }}>
            <FormattedMessage
              id="notebook.pharma.testing.deviationHandling"
              defaultMessage="Deviation Handling"
            />
          </h5>

          <Checkbox
            id="has-deviation"
            labelText={intl.formatMessage({
              id: "notebook.pharma.testing.hasDeviation",
              defaultMessage: "Log a Deviation for this test",
            })}
            checked={assayData.hasDeviation}
            onChange={(_, { checked }) =>
              setAssayData({ ...assayData, hasDeviation: checked })
            }
          />

          {assayData.hasDeviation && (
            <>
              <TextArea
                id="deviation-description"
                labelText={intl.formatMessage({
                  id: "notebook.pharma.testing.deviationDescription",
                  defaultMessage: "Deviation Description",
                })}
                value={assayData.deviationDescription}
                onChange={(e) =>
                  setAssayData({
                    ...assayData,
                    deviationDescription: e.target.value,
                  })
                }
                placeholder="Describe what went wrong..."
                rows={2}
              />
              <TextInput
                id="root-cause"
                labelText={intl.formatMessage({
                  id: "notebook.pharma.testing.rootCause",
                  defaultMessage: "Root Cause",
                })}
                value={assayData.rootCause}
                onChange={(e) =>
                  setAssayData({ ...assayData, rootCause: e.target.value })
                }
              />
              <TextArea
                id="capa-action"
                labelText={intl.formatMessage({
                  id: "notebook.pharma.testing.capaAction",
                  defaultMessage: "CAPA (Corrective and Preventive Action)",
                })}
                value={assayData.capaAction}
                onChange={(e) =>
                  setAssayData({ ...assayData, capaAction: e.target.value })
                }
                placeholder="Describe corrective actions taken..."
                rows={2}
              />
            </>
          )}

          {/* Notes */}
          <TextArea
            id="notes"
            labelText={intl.formatMessage({
              id: "notebook.pharma.testing.notes",
              defaultMessage: "Additional Notes",
            })}
            value={assayData.notes}
            onChange={(e) =>
              setAssayData({ ...assayData, notes: e.target.value })
            }
            rows={2}
          />
        </div>
      </Modal>
    </div>
  );
}

export default PharmaceuticalTestingPage;
