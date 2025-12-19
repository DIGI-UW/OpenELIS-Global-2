import React, {
  useState,
  useEffect,
  useRef,
  useCallback,
  useMemo,
} from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  InlineNotification,
  TextInput,
  TextArea,
  Dropdown,
  MultiSelect,
  DatePicker,
  DatePickerInput,
  NumberInput,
  Modal,
  Tag,
  RadioButtonGroup,
  RadioButton,
  InlineLoading,
} from "@carbon/react";
import { CheckmarkFilled, Renew, Chemistry } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
} from "../../../utils/Utils";
import SampleGrid from "../../workflow/SampleGrid";
import "../../workflow/NotebookWorkflow.css";

/**
 * MNTDProcessingQCPage - Page 6 of the MNTD workflow.
 * Handles Nucleic Acid Extraction.
 *
 * STAGE 4: Nucleic Acid Extraction
 *
 * Purpose: Execute nucleic acid extraction.
 *
 * Who uses it:
 * - Lab technician
 * - Supervisor
 *
 * Data Points:
 * - Sample Type: Parasite or Vector
 * - Extraction Type: Manual or Automatic
 * - Extraction Method: Based on sample type
 * - Kit Lot Number
 * - Yield (ng/µL)
 * - Extraction Date
 * - Operator
 *
 * System Actions:
 * - Status updated: Extracted
 *
 * Leads to: Test Assignment & Machine Scheduling Page
 */

// Parasite Manual Extraction Methods
const PARASITE_MANUAL_METHODS = [
  { id: "QIAGEN_DNA", text: "QIAGEN DNA Extraction Kits" },
  { id: "CHELEX_TWEEN", text: "Chelex-Tween 100 Chelating Resin" },
  { id: "PAXGENE_DNA", text: "Paxgene Blood DNA Kits" },
  { id: "QIAGEN_RNA", text: "QIAGEN RNA Extraction Kits" },
  { id: "PAXGENE_RNA", text: "Paxgene Blood RNA Kits" },
  { id: "TRIZOL_TOTAL_NA", text: "TRIzol (Total Nucleic Acid)" },
  { id: "OTHER_MANUAL", text: "Others" },
];

// Parasite Automatic Extraction Methods
const PARASITE_AUTO_METHODS = [
  { id: "KINGFISHER", text: "KingFisher" },
  { id: "MAGNAPURE_96", text: "MagNAPure 96" },
  { id: "MAGMAX_DNA", text: "MAGMAX (DNA)" },
  { id: "MAGMAX_RNA", text: "MAGMAX (RNA)" },
  { id: "MAGMAX_TOTAL_NA", text: "MAGMAX (Total NA)" },
  { id: "NUCLEOMAG_DNA", text: "NucleoMag (DNA)" },
  { id: "NUCLEOMAG_RNA", text: "NucleoMag (RNA)" },
  { id: "NUCLEOMAG_TOTAL_NA", text: "NucleoMag (Total NA)" },
];

// Vector Manual Extraction Methods
const VECTOR_MANUAL_METHODS = [
  { id: "QIAGEN_DNA_V", text: "QIAGEN DNA Kits" },
  { id: "CTAB", text: "CTAB" },
  { id: "QUICK_RNA_TISSUE", text: "Quick-RNA Tissue/Insect Kit" },
  { id: "TRIZOL_V", text: "TRIzol" },
  { id: "OTHER_MANUAL_V", text: "Others" },
];

// Vector Automatic Extraction Methods
const VECTOR_AUTO_METHODS = [
  { id: "MAGMAX_DNA_V", text: "MagMax (DNA)" },
  { id: "MAGMAX_RNA_V", text: "MagMax (RNA)" },
  { id: "MAGMAX_TOTAL_NA_V", text: "MagMax (Total NA)" },
  { id: "NUCLEOMAG_DNA_V", text: "NucleoMag (DNA)" },
  { id: "NUCLEOMAG_RNA_V", text: "NucleoMag (RNA)" },
  { id: "NUCLEOMAG_TOTAL_NA_V", text: "NucleoMag (Total NA)" },
];

function MNTDProcessingQCPage({ entryId, pageData, onProgressUpdate }) {
  const intl = useIntl();
  const componentMounted = useRef(false);

  // State for samples
  const [samples, setSamples] = useState([]);
  const [selectedIds, setSelectedIds] = useState([]);
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  // Extraction modal state
  const [showExtractionModal, setShowExtractionModal] = useState(false);
  const [extractionData, setExtractionData] = useState({
    sampleType: "PARASITE", // PARASITE or VECTOR
    extractionType: "MANUAL", // MANUAL or AUTOMATIC
    extractionMethod: "",
    otherMethodDescription: "",
    selectedKits: [], // Array of selected kit IDs for multiselect
    yield: "",
    yieldUnit: "ng/uL",
    extractionDate: new Date().toISOString().split("T")[0],
    operator: "",
    notes: "",
  });

  // Reagents from inventory (for extraction kit selection)
  const [reagents, setReagents] = useState([]);
  const [loadingReagents, setLoadingReagents] = useState(false);

  // Get extraction methods based on sample type and extraction type
  const getExtractionMethods = useCallback(() => {
    const { sampleType, extractionType } = extractionData;
    if (sampleType === "PARASITE") {
      return extractionType === "MANUAL"
        ? PARASITE_MANUAL_METHODS
        : PARASITE_AUTO_METHODS;
    } else {
      return extractionType === "MANUAL"
        ? VECTOR_MANUAL_METHODS
        : VECTOR_AUTO_METHODS;
    }
  }, [extractionData.sampleType, extractionData.extractionType]);

  // Load reagents from inventory (used for kit lot number selection)
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
          } else {
            setReagents([]);
          }
          setLoadingReagents(false);
        }
      },
    );
  }, []);

  // Load samples for this page
  useEffect(() => {
    componentMounted.current = true;
    loadPageSamples();

    return () => {
      componentMounted.current = false;
    };
  }, [entryId, pageData?.id]);

  const loadPageSamples = useCallback(() => {
    if (!pageData?.id) {
      setLoading(false);
      return;
    }

    // Skip loading for synthetic page IDs
    if (String(pageData.id).startsWith("default-")) {
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
              patientName: sample.patientName,
              // Extraction data
              extractionSampleType: sample.data?.extractionSampleType,
              extractionType: sample.data?.extractionType,
              extractionMethod: sample.data?.extractionMethod,
              kitLotNumber: sample.data?.kitLotNumber,
              yield: sample.data?.yield,
              extractionDate: sample.data?.extractionDate,
              operator: sample.data?.operator,
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

  // Check if page has a real database ID
  const hasRealPageId =
    pageData?.id && !String(pageData.id).startsWith("default-");

  // Calculate stats
  const stats = useMemo(() => {
    const extracted = samples.filter((s) => s.extractionMethod).length;
    const pending = samples.filter((s) => !s.extractionMethod).length;
    const completed = samples.filter((s) => s.status === "COMPLETED").length;
    return { total: samples.length, extracted, pending, completed };
  }, [samples]);

  // Handle opening extraction modal
  const handleOpenExtractionModal = useCallback(() => {
    if (selectedIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.mntd.extraction.selectSamples",
          defaultMessage: "Please select at least one sample.",
        }),
      );
      return;
    }
    // Reset extraction data
    setExtractionData({
      sampleType: "PARASITE",
      extractionType: "MANUAL",
      extractionMethod: "",
      otherMethodDescription: "",
      selectedKits: [],
      yield: "",
      yieldUnit: "ng/uL",
      extractionDate: new Date().toISOString().split("T")[0],
      operator: "",
      notes: "",
    });
    // Load reagents from inventory
    loadReagents();
    setShowExtractionModal(true);
  }, [selectedIds, intl, loadReagents]);

  // Handle saving extraction data
  const handleSaveExtractionData = useCallback(() => {
    if (!extractionData.extractionMethod) {
      setError(
        intl.formatMessage({
          id: "notebook.mntd.extraction.methodRequired",
          defaultMessage: "Extraction method is required.",
        }),
      );
      return;
    }

    if (!hasRealPageId) {
      setShowExtractionModal(false);
      return;
    }

    const numericIds = selectedIds.map((id) => parseInt(id, 10));

    // Get selected kit objects from reagents list based on selected IDs
    const selectedKitObjects = reagents.filter((r) =>
      extractionData.selectedKits.includes(r.id),
    );

    // Build kit lot numbers string from selected kits
    const kitLotNumbers = selectedKitObjects
      .map((kit) => kit.lotNumber)
      .filter(Boolean)
      .join(", ");

    // Build selectedReagents array for inventory consumption (using itemId)
    // The backend looks for 'selectedReagents' to trigger automatic inventory reduction
    const selectedReagents = selectedKitObjects
      .map((kit) => kit.itemId)
      .filter(Boolean);

    const dataToSave = {
      extractionSampleType: extractionData.sampleType,
      extractionType: extractionData.extractionType,
      extractionMethod: extractionData.extractionMethod,
      otherMethodDescription: extractionData.otherMethodDescription,
      kitLotNumber: kitLotNumbers,
      selectedKitIds: extractionData.selectedKits,
      yield: extractionData.yield,
      yieldUnit: extractionData.yieldUnit,
      extractionDate: extractionData.extractionDate,
      operator: extractionData.operator,
      extractionNotes: extractionData.notes,
      // Include selectedReagents for automatic inventory consumption
      selectedReagents: selectedReagents,
    };

    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
      JSON.stringify({
        sampleIds: numericIds,
        data: dataToSave,
      }),
      (status) => {
        if (componentMounted.current) {
          if (status === 200) {
            // Update status to IN_PROGRESS
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
                      id: "notebook.mntd.extraction.saved",
                      defaultMessage:
                        "Extraction data saved for {count} samples.",
                    },
                    { count: selectedIds.length },
                  ),
                );
                setShowExtractionModal(false);
                setSelectedIds([]);
                loadPageSamples();
                if (onProgressUpdate) {
                  onProgressUpdate();
                }
              },
            );
          } else {
            setError("Failed to save extraction data.");
          }
        }
      },
    );
  }, [
    extractionData,
    selectedIds,
    hasRealPageId,
    pageData?.id,
    loadPageSamples,
    onProgressUpdate,
    intl,
    reagents,
  ]);

  // Bulk mark as completed
  const handleBulkMarkCompleted = useCallback(() => {
    if (selectedIds.length === 0) return;

    if (!hasRealPageId) {
      setError(
        intl.formatMessage({
          id: "notebook.mntd.extraction.pageNotInitialized",
          defaultMessage:
            "Cannot update status: Page not properly initialized.",
        }),
      );
      return;
    }

    const numericIds = selectedIds.map((id) => parseInt(id, 10));

    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
      JSON.stringify({
        sampleIds: numericIds,
        status: "COMPLETED",
      }),
      (status) => {
        if (status === 200) {
          setSuccess(
            intl.formatMessage(
              {
                id: "notebook.mntd.extraction.markedCompleted",
                defaultMessage: "Marked {count} samples as completed.",
              },
              { count: selectedIds.length },
            ),
          );
          loadPageSamples();
          setSelectedIds([]);
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError("Failed to update sample status. Please try again.");
        }
      },
    );
  }, [
    selectedIds,
    pageData?.id,
    hasRealPageId,
    loadPageSamples,
    onProgressUpdate,
    intl,
  ]);

  // Get method label from ID
  const getMethodLabel = (methodId) => {
    const allMethods = [
      ...PARASITE_MANUAL_METHODS,
      ...PARASITE_AUTO_METHODS,
      ...VECTOR_MANUAL_METHODS,
      ...VECTOR_AUTO_METHODS,
    ];
    return allMethods.find((m) => m.id === methodId)?.text || methodId;
  };

  // Render extraction info column
  const renderExtractionInfo = (sample) => {
    if (sample.extractionMethod) {
      const methodLabel = getMethodLabel(sample.extractionMethod);
      const sampleTypeLabel =
        sample.extractionSampleType === "VECTOR" ? "Vector" : "Parasite";
      const extractionTypeLabel =
        sample.extractionType === "AUTOMATIC" ? "Auto" : "Manual";
      return (
        <div style={{ fontSize: "12px" }}>
          <div style={{ display: "flex", gap: "4px", flexWrap: "wrap" }}>
            <Tag type="blue" size="sm">
              {sampleTypeLabel}
            </Tag>
            <Tag type="purple" size="sm">
              {extractionTypeLabel}
            </Tag>
          </div>
          <div style={{ marginTop: "4px", fontWeight: "500" }}>
            {methodLabel}
          </div>
          {sample.kitLotNumber && (
            <div style={{ color: "#525252", fontSize: "11px" }}>
              Lot: {sample.kitLotNumber}
            </div>
          )}
          {sample.yield && (
            <div style={{ color: "#525252", fontSize: "11px" }}>
              Yield: {sample.yield} ng/µL
            </div>
          )}
          {sample.operator && (
            <div style={{ color: "#525252", fontSize: "11px" }}>
              By: {sample.operator}
            </div>
          )}
        </div>
      );
    }
    return (
      <span style={{ color: "#8d8d8d", fontSize: "12px" }}>
        <FormattedMessage
          id="notebook.mntd.extraction.notExtracted"
          defaultMessage="Not extracted"
        />
      </span>
    );
  };

  return (
    <div className="mntd-processing-qc-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <Chemistry
            size={20}
            style={{ marginRight: "8px", verticalAlign: "middle" }}
          />
          <FormattedMessage
            id="notebook.page.mntd.extraction.title"
            defaultMessage="Nucleic Acid Extraction"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.mntd.extraction.description"
            defaultMessage="Perform nucleic acid extraction for Parasite or Vector samples using manual or automatic methods. Record extraction details."
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
                  id="notebook.mntd.extraction.totalSamples"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{stats.total}</span>
            </Tile>
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.mntd.extraction.extracted"
                  defaultMessage="Extracted"
                />
              </span>
              <span className="progress-value">{stats.extracted}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.mntd.extraction.pending"
                  defaultMessage="Pending"
                />
              </span>
              <span className="progress-value">{stats.pending}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.mntd.extraction.completed"
                  defaultMessage="Completed"
                />
              </span>
              <span className="progress-value">{stats.completed}</span>
            </Tile>
          </div>
        </Column>
      </Grid>

      {/* Notifications */}
      {error && (
        <InlineNotification
          kind="error"
          title={error}
          hideCloseButton={false}
          lowContrast
          onClose={() => setError(null)}
          style={{ marginBottom: "1rem" }}
        />
      )}

      {success && (
        <InlineNotification
          kind="success"
          title={success}
          hideCloseButton={false}
          lowContrast
          onClose={() => setSuccess(null)}
          style={{ marginBottom: "1rem" }}
        />
      )}

      {/* Action Buttons */}
      <div className="page-actions-bar">
        <Button
          kind="primary"
          size="sm"
          renderIcon={Chemistry}
          onClick={handleOpenExtractionModal}
          disabled={selectedIds.length === 0}
        >
          <FormattedMessage
            id="notebook.mntd.extraction.recordExtraction"
            defaultMessage="Record Extraction ({count} selected)"
            values={{ count: selectedIds.length }}
          />
        </Button>

        {selectedIds.length > 0 && (
          <Button
            kind="tertiary"
            size="sm"
            renderIcon={CheckmarkFilled}
            onClick={handleBulkMarkCompleted}
          >
            <FormattedMessage
              id="notebook.mntd.extraction.markCompleted"
              defaultMessage="Mark Completed ({count})"
              values={{ count: selectedIds.length }}
            />
          </Button>
        )}

        <Button
          kind="ghost"
          size="sm"
          renderIcon={Renew}
          onClick={loadPageSamples}
        >
          <FormattedMessage
            id="notebook.mntd.extraction.refresh"
            defaultMessage="Refresh"
          />
        </Button>
      </div>

      {/* Sample Grid */}
      <div className="sample-grid-container">
        <SampleGrid
          gridId="mntd-extraction"
          samples={samples}
          selectedIds={selectedIds}
          onSelectionChange={setSelectedIds}
          statusFilter={statusFilter}
          onStatusFilterChange={setStatusFilter}
          showSelection={true}
          loading={loading}
          additionalColumns={[
            {
              key: "extractionInfo",
              header: intl.formatMessage({
                id: "notebook.mntd.extraction.extractionInfo",
                defaultMessage: "Extraction Info",
              }),
              render: renderExtractionInfo,
            },
          ]}
        />
      </div>

      {/* Empty state */}
      {!loading && samples.length === 0 && (
        <div className="empty-state">
          <p>
            <FormattedMessage
              id="notebook.mntd.extraction.empty"
              defaultMessage="No samples available for extraction. Please complete the aliquoting step first."
            />
          </p>
        </div>
      )}

      {/* Record Extraction Modal */}
      <Modal
        open={showExtractionModal}
        modalHeading={intl.formatMessage({
          id: "notebook.mntd.extraction.modal.title",
          defaultMessage: "Record Nucleic Acid Extraction",
        })}
        primaryButtonText={intl.formatMessage({
          id: "notebook.mntd.extraction.modal.save",
          defaultMessage: "Save",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => setShowExtractionModal(false)}
        onRequestSubmit={handleSaveExtractionData}
        size="md"
      >
        <div style={{ marginBottom: "1rem" }}>
          <p style={{ color: "#525252", marginBottom: "1rem" }}>
            <FormattedMessage
              id="notebook.mntd.extraction.modal.description"
              defaultMessage="Record extraction data for {count} selected sample(s)."
              values={{ count: selectedIds.length }}
            />
          </p>

          {/* Sample Type Selection */}
          <div style={{ marginBottom: "1.5rem" }}>
            <RadioButtonGroup
              legendText={intl.formatMessage({
                id: "notebook.mntd.extraction.sampleType",
                defaultMessage: "Sample Type",
              })}
              name="sample-type"
              valueSelected={extractionData.sampleType}
              onChange={(value) => {
                setExtractionData({
                  ...extractionData,
                  sampleType: value,
                  extractionMethod: "", // Reset method when type changes
                });
              }}
              orientation="horizontal"
            >
              <RadioButton
                id="sample-parasite"
                labelText={intl.formatMessage({
                  id: "notebook.mntd.extraction.parasite",
                  defaultMessage: "Parasite",
                })}
                value="PARASITE"
              />
              <RadioButton
                id="sample-vector"
                labelText={intl.formatMessage({
                  id: "notebook.mntd.extraction.vector",
                  defaultMessage: "Vector",
                })}
                value="VECTOR"
              />
            </RadioButtonGroup>
          </div>

          {/* Extraction Type Selection */}
          <div style={{ marginBottom: "1.5rem" }}>
            <RadioButtonGroup
              legendText={intl.formatMessage({
                id: "notebook.mntd.extraction.extractionType",
                defaultMessage: "Extraction Type",
              })}
              name="extraction-type"
              valueSelected={extractionData.extractionType}
              onChange={(value) => {
                setExtractionData({
                  ...extractionData,
                  extractionType: value,
                  extractionMethod: "", // Reset method when type changes
                });
              }}
              orientation="horizontal"
            >
              <RadioButton
                id="extraction-manual"
                labelText={intl.formatMessage({
                  id: "notebook.mntd.extraction.manual",
                  defaultMessage: "Manual",
                })}
                value="MANUAL"
              />
              <RadioButton
                id="extraction-automatic"
                labelText={intl.formatMessage({
                  id: "notebook.mntd.extraction.automatic",
                  defaultMessage: "Automatic",
                })}
                value="AUTOMATIC"
              />
            </RadioButtonGroup>
          </div>

          {/* Extraction Method Selection */}
          <Dropdown
            id="extraction-method"
            titleText={intl.formatMessage({
              id: "notebook.mntd.extraction.method",
              defaultMessage: "Extraction Method",
            })}
            label={intl.formatMessage({
              id: "notebook.mntd.extraction.selectMethod",
              defaultMessage: "Select extraction method",
            })}
            items={getExtractionMethods()}
            itemToString={(item) => (item ? item.text : "")}
            selectedItem={getExtractionMethods().find(
              (m) => m.id === extractionData.extractionMethod,
            )}
            onChange={({ selectedItem }) =>
              setExtractionData({
                ...extractionData,
                extractionMethod: selectedItem?.id || "",
              })
            }
            style={{ marginBottom: "1rem" }}
          />

          {/* Other Method Description - shown only for "Others" */}
          {(extractionData.extractionMethod === "OTHER_MANUAL" ||
            extractionData.extractionMethod === "OTHER_MANUAL_V") && (
            <TextInput
              id="other-method-desc"
              labelText={intl.formatMessage({
                id: "notebook.mntd.extraction.otherMethodDescription",
                defaultMessage: "Specify Other Method",
              })}
              value={extractionData.otherMethodDescription}
              onChange={(e) =>
                setExtractionData({
                  ...extractionData,
                  otherMethodDescription: e.target.value,
                })
              }
              style={{ marginBottom: "1rem" }}
            />
          )}

          {/* Kit Lot Number - Multiselect from inventory reagents */}
          {loadingReagents ? (
            <InlineLoading
              description={intl.formatMessage({
                id: "notebook.mntd.extraction.loadingReagents",
                defaultMessage: "Loading reagents...",
              })}
              style={{ marginBottom: "1rem" }}
            />
          ) : (
            <MultiSelect
              id="kit-lot-number"
              titleText={intl.formatMessage({
                id: "notebook.mntd.extraction.kitLotNumber",
                defaultMessage: "Kit Lot Number",
              })}
              label={intl.formatMessage({
                id: "notebook.mntd.extraction.selectKits",
                defaultMessage: "Select extraction kits...",
              })}
              items={reagents}
              itemToString={(item) => (item ? item.label : "")}
              selectedItems={reagents.filter((r) =>
                extractionData.selectedKits.includes(r.id),
              )}
              onChange={({ selectedItems }) =>
                setExtractionData({
                  ...extractionData,
                  selectedKits: selectedItems.map((item) => item.id),
                })
              }
              disabled={loadingReagents}
            />
          )}

          {/* Yield */}
          <Grid fullWidth style={{ marginBottom: "1rem" }}>
            <Column lg={8} md={4} sm={4}>
              <NumberInput
                id="yield"
                label={intl.formatMessage({
                  id: "notebook.mntd.extraction.yield",
                  defaultMessage: "Yield",
                })}
                value={extractionData.yield}
                onChange={(e, { value }) =>
                  setExtractionData({ ...extractionData, yield: value })
                }
                min={0}
                step={0.01}
                allowEmpty
                hideSteppers
              />
            </Column>
            <Column lg={8} md={4} sm={4}>
              <Dropdown
                id="yield-unit"
                titleText={intl.formatMessage({
                  id: "notebook.mntd.extraction.unit",
                  defaultMessage: "Unit",
                })}
                label={intl.formatMessage({
                  id: "notebook.mntd.extraction.selectUnit",
                  defaultMessage: "Select unit",
                })}
                items={[
                  { id: "ng/uL", text: "ng/µL" },
                  { id: "ug/mL", text: "µg/mL" },
                  { id: "pg/uL", text: "pg/µL" },
                ]}
                itemToString={(item) => (item ? item.text : "")}
                selectedItem={{
                  id: extractionData.yieldUnit,
                  text:
                    extractionData.yieldUnit === "ng/uL"
                      ? "ng/µL"
                      : extractionData.yieldUnit,
                }}
                onChange={({ selectedItem }) =>
                  setExtractionData({
                    ...extractionData,
                    yieldUnit: selectedItem?.id || "ng/uL",
                  })
                }
              />
            </Column>
          </Grid>

          {/* Extraction Date */}
          <DatePicker
            datePickerType="single"
            value={extractionData.extractionDate}
            onChange={([date]) =>
              setExtractionData({
                ...extractionData,
                extractionDate: date?.toISOString().split("T")[0] || "",
              })
            }
          >
            <DatePickerInput
              id="extraction-date"
              labelText={intl.formatMessage({
                id: "notebook.mntd.extraction.date",
                defaultMessage: "Extraction Date",
              })}
              placeholder="mm/dd/yyyy"
            />
          </DatePicker>

          {/* Operator */}
          <TextInput
            id="operator"
            labelText={intl.formatMessage({
              id: "notebook.mntd.extraction.operator",
              defaultMessage: "Operator",
            })}
            value={extractionData.operator}
            onChange={(e) =>
              setExtractionData({
                ...extractionData,
                operator: e.target.value,
              })
            }
            style={{ marginTop: "1rem", marginBottom: "1rem" }}
          />

          {/* Notes */}
          <TextArea
            id="extraction-notes"
            labelText={intl.formatMessage({
              id: "notebook.mntd.extraction.notes",
              defaultMessage: "Notes",
            })}
            value={extractionData.notes}
            onChange={(e) =>
              setExtractionData({ ...extractionData, notes: e.target.value })
            }
            rows={3}
          />
        </div>
      </Modal>
    </div>
  );
}

export default MNTDProcessingQCPage;
