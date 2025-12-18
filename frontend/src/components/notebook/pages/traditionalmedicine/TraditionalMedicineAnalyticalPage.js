import React, { useState, useEffect, useRef, useCallback } from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  InlineNotification,
  Modal,
  TextInput,
  TextArea,
  Select,
  SelectItem,
  Checkbox,
  Tag,
  NumberInput,
} from "@carbon/react";
import { Checkmark, Edit } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../../utils/Utils";
import SampleGrid from "../../workflow/SampleGrid";
import "../../workflow/NotebookWorkflow.css";

/**
 * TraditionalMedicineAnalyticalPage - Page 6 of the Traditional Medicine workflow.
 * Perform advanced compound profiling when required (optional).
 * Includes fractionation, identification/isolation, purification, and characterization.
 */
function TraditionalMedicineAnalyticalPage({
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

  // Bulk Apply Modal State
  const [bulkModalOpen, setBulkModalOpen] = useState(false);
  const [bulkFormData, setBulkFormData] = useState({
    fractionationMethod: "",
    numberOfFractions: "",
    activeConstituentsIdentified: "",
    purificationMethod: "",
    purityLevel: "",
    structuralAnalysisNMR: false,
    structuralAnalysisMS: false,
    structuralAnalysisIR: false,
    spectralFileReference: "",
    characterizationNotes: "",
  });

  // Fractionation method options
  const fractionationMethods = [
    {
      value: "",
      label: intl.formatMessage({
        id: "common.select",
        defaultMessage: "Select...",
      }),
    },
    { value: "Column Chromatography", label: "Column Chromatography" },
    {
      value: "Thin Layer Chromatography",
      label: "Thin Layer Chromatography (TLC)",
    },
    { value: "HPLC", label: "High Performance Liquid Chromatography (HPLC)" },
    { value: "Flash Chromatography", label: "Flash Chromatography" },
    { value: "Liquid-Liquid Extraction", label: "Liquid-Liquid Extraction" },
    { value: "Solid Phase Extraction", label: "Solid Phase Extraction (SPE)" },
    { value: "Gel Filtration", label: "Gel Filtration" },
    { value: "Ion Exchange", label: "Ion Exchange Chromatography" },
    { value: "Other", label: "Other" },
  ];

  // Purification method options
  const purificationMethods = [
    {
      value: "",
      label: intl.formatMessage({
        id: "common.select",
        defaultMessage: "Select...",
      }),
    },
    { value: "Recrystallization", label: "Recrystallization" },
    { value: "Preparative HPLC", label: "Preparative HPLC" },
    { value: "Preparative TLC", label: "Preparative TLC" },
    { value: "Column Chromatography", label: "Column Chromatography" },
    { value: "Sublimation", label: "Sublimation" },
    { value: "Distillation", label: "Distillation" },
    { value: "Precipitation", label: "Precipitation" },
    { value: "Other", label: "Other" },
  ];

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
    setError(null);
    setSuccessMessage(null);

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
              extractId: sample.data?.extractId,
              fractionationMethod: sample.data?.fractionationMethod,
              numberOfFractions: sample.data?.numberOfFractions,
              activeConstituentsIdentified:
                sample.data?.activeConstituentsIdentified,
              purificationMethod: sample.data?.purificationMethod,
              purityLevel: sample.data?.purityLevel,
              structuralAnalysisNMR: sample.data?.structuralAnalysisNMR,
              structuralAnalysisMS: sample.data?.structuralAnalysisMS,
              structuralAnalysisIR: sample.data?.structuralAnalysisIR,
              spectralFileReference: sample.data?.spectralFileReference,
              characterizationNotes: sample.data?.characterizationNotes,
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

  // Open bulk apply modal
  const openBulkModal = useCallback(() => {
    if (selectedSampleIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.page.tradmed.error.noSelection",
          defaultMessage: "Please select at least one sample.",
        }),
      );
      return;
    }
    setBulkFormData({
      fractionationMethod: "",
      numberOfFractions: "",
      activeConstituentsIdentified: "",
      purificationMethod: "",
      purityLevel: "",
      structuralAnalysisNMR: false,
      structuralAnalysisMS: false,
      structuralAnalysisIR: false,
      spectralFileReference: "",
      characterizationNotes: "",
    });
    setBulkModalOpen(true);
  }, [selectedSampleIds, intl]);

  // Apply bulk analytical data
  const applyBulkAnalytical = useCallback(() => {
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

    // Validate that at least fractionation method is selected
    if (!bulkFormData.fractionationMethod) {
      setError(
        intl.formatMessage({
          id: "notebook.page.tradmed.error.noFractionation",
          defaultMessage: "Please select a fractionation method.",
        }),
      );
      return;
    }

    setError(null);
    setSuccessMessage(null);

    // Build characterization techniques array
    const characterizationTechniques = [];
    if (bulkFormData.structuralAnalysisNMR)
      characterizationTechniques.push("NMR");
    if (bulkFormData.structuralAnalysisMS)
      characterizationTechniques.push("MS");
    if (bulkFormData.structuralAnalysisIR)
      characterizationTechniques.push("IR");

    const apiRequestData = {
      sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
      fractionationMethod: bulkFormData.fractionationMethod,
      numberOfFractions: bulkFormData.numberOfFractions
        ? parseInt(bulkFormData.numberOfFractions, 10)
        : null,
      activeConstituentsIdentified:
        bulkFormData.activeConstituentsIdentified || null,
      purificationMethod: bulkFormData.purificationMethod || null,
      purityLevel: bulkFormData.purityLevel || null,
      characterizationTechniques:
        characterizationTechniques.length > 0
          ? characterizationTechniques
          : null,
      spectralFileReference: bulkFormData.spectralFileReference || null,
      notes: bulkFormData.characterizationNotes || null,
    };

    postToOpenElisServerJsonResponse(
      `/rest/notebook/tradmed/page/${pageData.id}/analytical`,
      JSON.stringify(apiRequestData),
      (response) => {
        if (response && response.success) {
          setSuccessMessage(
            response.message ||
              intl.formatMessage(
                {
                  id: "notebook.page.tradmed.success.analyticalApplied",
                  defaultMessage:
                    "Applied analytical data to {count} sample(s).",
                },
                { count: response.updatedCount || selectedSampleIds.length },
              ),
          );
          setBulkModalOpen(false);
          setSelectedSampleIds([]);
          loadPageSamples();
          if (onProgressUpdate) onProgressUpdate();
        } else {
          setError(
            response?.error ||
              intl.formatMessage({
                id: "notebook.page.tradmed.error.bulkApply",
                defaultMessage:
                  "Failed to apply analytical data. Please try again.",
              }),
          );
        }
      },
    );
  }, [
    hasRealPageId,
    bulkFormData,
    selectedSampleIds,
    intl,
    pageData?.id,
    loadPageSamples,
    onProgressUpdate,
  ]);

  // Mark selected samples as characterized
  const markAsCharacterized = useCallback(() => {
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

    // Validate selected samples have analytical data
    const selectedSamples = samples.filter((s) =>
      selectedSampleIds.includes(s.id),
    );
    const incompleteCount = selectedSamples.filter(
      (s) => !s.fractionationMethod,
    ).length;
    if (incompleteCount > 0) {
      setError(
        intl.formatMessage(
          {
            id: "notebook.page.tradmed.error.incompleteAnalytical",
            defaultMessage:
              "{count} sample(s) missing analytical data. Apply data first.",
          },
          { count: incompleteCount },
        ),
      );
      return;
    }

    setError(null);
    setSuccessMessage(null);

    postToOpenElisServerJsonResponse(
      `/rest/notebook/tradmed/page/${pageData.id}/analytical/complete`,
      JSON.stringify({
        sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
      }),
      (response) => {
        if (response && response.success) {
          setSuccessMessage(
            response.message ||
              intl.formatMessage(
                {
                  id: "notebook.page.tradmed.success.characterized",
                  defaultMessage:
                    "Marked {count} sample(s) as Characterized and advanced to Testing.",
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
                defaultMessage: "Failed to update samples. Please try again.",
              }),
          );
        }
      },
    );
  }, [
    selectedSampleIds,
    hasRealPageId,
    samples,
    intl,
    loadPageSamples,
    onProgressUpdate,
    pageData?.id,
  ]);

  // Skip analytical pathway (optional step)
  const skipAnalytical = useCallback(() => {
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

    setError(null);
    setSuccessMessage(null);

    postToOpenElisServerJsonResponse(
      `/rest/notebook/tradmed/page/${pageData.id}/analytical/skip`,
      JSON.stringify({
        sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
      }),
      (response) => {
        if (response && response.success) {
          setSuccessMessage(
            response.message ||
              intl.formatMessage(
                {
                  id: "notebook.page.tradmed.success.skipped",
                  defaultMessage:
                    "Skipped analytical pathway for {count} sample(s). They will proceed to testing.",
                },
                { count: response.skippedCount || selectedSampleIds.length },
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
                defaultMessage: "Failed to update samples. Please try again.",
              }),
          );
        }
      },
    );
  }, [
    selectedSampleIds,
    hasRealPageId,
    intl,
    loadPageSamples,
    onProgressUpdate,
    pageData?.id,
  ]);

  const pendingCount = samples.filter((s) => s.status === "PENDING").length;
  const completedCount = samples.filter((s) => s.status === "COMPLETED").length;
  const skippedCount = samples.filter((s) => s.status === "SKIPPED").length;
  const inProgressCount = samples.filter(
    (s) => s.status === "IN_PROGRESS",
  ).length;

  // Render analytical info as tags
  const renderAnalyticalInfo = (sample) => {
    const tags = [];
    if (sample.fractionationMethod) {
      tags.push(
        <Tag key="frac" type="blue" size="sm">
          {sample.fractionationMethod}
        </Tag>,
      );
    }
    if (sample.numberOfFractions) {
      tags.push(
        <Tag key="fracs" type="gray" size="sm">
          {sample.numberOfFractions} fractions
        </Tag>,
      );
    }
    if (sample.purityLevel) {
      tags.push(
        <Tag key="purity" type="green" size="sm">
          {sample.purityLevel}% pure
        </Tag>,
      );
    }
    // Structural analysis tags
    if (sample.structuralAnalysisNMR) {
      tags.push(
        <Tag key="nmr" type="purple" size="sm">
          NMR
        </Tag>,
      );
    }
    if (sample.structuralAnalysisMS) {
      tags.push(
        <Tag key="ms" type="purple" size="sm">
          MS
        </Tag>,
      );
    }
    if (sample.structuralAnalysisIR) {
      tags.push(
        <Tag key="ir" type="purple" size="sm">
          IR
        </Tag>,
      );
    }
    return tags.length > 0 ? tags : "-";
  };

  // Custom column renderer for analytical data
  const enhancedSamples = samples.map((sample) => ({
    ...sample,
    analyticalInfo: renderAnalyticalInfo(sample),
  }));

  return (
    <div className="tradmed-analytical-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.page.tradmed.analytical.title"
            defaultMessage="Analytical Pathway (Optional)"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.tradmed.analytical.description"
            defaultMessage="Perform advanced compound profiling when required, including fractionation, identification/isolation, purification, and characterization. This step is optional - samples can proceed directly to testing."
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
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.tradmed.pendingAnalysis"
                  defaultMessage="Pending Analysis"
                />
              </span>
              <span className="progress-value">{pendingCount}</span>
            </Tile>
            <Tile className="progress-tile in-progress">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.tradmed.inProgress"
                  defaultMessage="In Progress"
                />
              </span>
              <span className="progress-value">{inProgressCount}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.tradmed.characterized"
                  defaultMessage="Characterized"
                />
              </span>
              <span className="progress-value">{completedCount}</span>
            </Tile>
            <Tile className="progress-tile skipped">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.tradmed.skipped"
                  defaultMessage="Skipped"
                />
              </span>
              <span className="progress-value">{skippedCount}</span>
            </Tile>
          </div>
        </Column>
      </Grid>

      {/* Action Buttons */}
      <div className="page-actions-bar">
        {selectedSampleIds.length > 0 && (
          <>
            <Button
              kind="tertiary"
              size="sm"
              renderIcon={Edit}
              onClick={openBulkModal}
            >
              <FormattedMessage
                id="notebook.page.tradmed.applyAnalytical"
                defaultMessage="Apply Analytical Data ({count})"
                values={{ count: selectedSampleIds.length }}
              />
            </Button>
            <Button
              kind="primary"
              size="sm"
              renderIcon={Checkmark}
              onClick={markAsCharacterized}
            >
              <FormattedMessage
                id="notebook.page.tradmed.markCharacterized"
                defaultMessage="Mark as Characterized ({count})"
                values={{ count: selectedSampleIds.length }}
              />
            </Button>
            <Button kind="ghost" size="sm" onClick={skipAnalytical}>
              <FormattedMessage
                id="notebook.page.tradmed.skipAnalytical"
                defaultMessage="Skip to Testing ({count})"
                values={{ count: selectedSampleIds.length }}
              />
            </Button>
          </>
        )}
      </div>

      {/* Errors / Success */}
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

      {/* Sample Grid */}
      <div className="sample-grid-container">
        <SampleGrid
          samples={enhancedSamples}
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
            { key: "extractId", header: "Extract ID" },
            { key: "analyticalInfo", header: "Analytical Data" },
            { key: "activeConstituentsIdentified", header: "Constituents" },
            { key: "status", header: "Status" },
          ]}
        />
      </div>

      {/* Empty state */}
      {!loading && samples.length === 0 && (
        <div className="empty-state">
          <p>
            <FormattedMessage
              id="notebook.page.tradmed.analytical.empty"
              defaultMessage="No samples pending analytical pathway. This step is optional - samples can proceed directly to testing."
            />
          </p>
        </div>
      )}

      {/* Bulk Apply Modal */}
      <Modal
        open={bulkModalOpen}
        modalHeading={intl.formatMessage({
          id: "notebook.page.tradmed.analytical.modal.title",
          defaultMessage: "Apply Analytical Data",
        })}
        primaryButtonText={intl.formatMessage({
          id: "common.apply",
          defaultMessage: "Apply",
        })}
        secondaryButtonText={intl.formatMessage({
          id: "common.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestClose={() => setBulkModalOpen(false)}
        onRequestSubmit={applyBulkAnalytical}
        size="lg"
      >
        <p className="modal-description">
          <FormattedMessage
            id="notebook.page.tradmed.analytical.modal.description"
            defaultMessage="Apply analytical profiling data to {count} selected sample(s)."
            values={{ count: selectedSampleIds.length }}
          />
        </p>

        <Grid fullWidth className="modal-form-grid">
          {/* Fractionation Section */}
          <Column lg={8} md={4} sm={4}>
            <Select
              id="fractionationMethod"
              labelText={intl.formatMessage({
                id: "notebook.page.tradmed.analytical.fractionationMethod",
                defaultMessage: "Fractionation Method",
              })}
              value={bulkFormData.fractionationMethod}
              onChange={(e) =>
                setBulkFormData({
                  ...bulkFormData,
                  fractionationMethod: e.target.value,
                })
              }
            >
              {fractionationMethods.map((opt) => (
                <SelectItem
                  key={opt.value}
                  value={opt.value}
                  text={opt.label}
                />
              ))}
            </Select>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <NumberInput
              id="numberOfFractions"
              label={intl.formatMessage({
                id: "notebook.page.tradmed.analytical.numberOfFractions",
                defaultMessage: "Number of Fractions",
              })}
              value={bulkFormData.numberOfFractions}
              onChange={(e, { value }) =>
                setBulkFormData({ ...bulkFormData, numberOfFractions: value })
              }
              min={1}
              max={100}
              allowEmpty
            />
          </Column>

          {/* Identification Section */}
          <Column lg={16} md={8} sm={4}>
            <TextArea
              id="activeConstituentsIdentified"
              labelText={intl.formatMessage({
                id: "notebook.page.tradmed.analytical.activeConstituents",
                defaultMessage: "Active Constituents Identified",
              })}
              placeholder={intl.formatMessage({
                id: "notebook.page.tradmed.analytical.activeConstituentsPlaceholder",
                defaultMessage:
                  "List identified compounds (e.g., alkaloids, flavonoids, terpenoids)",
              })}
              value={bulkFormData.activeConstituentsIdentified}
              onChange={(e) =>
                setBulkFormData({
                  ...bulkFormData,
                  activeConstituentsIdentified: e.target.value,
                })
              }
              rows={2}
            />
          </Column>

          {/* Purification Section */}
          <Column lg={8} md={4} sm={4}>
            <Select
              id="purificationMethod"
              labelText={intl.formatMessage({
                id: "notebook.page.tradmed.analytical.purificationMethod",
                defaultMessage: "Purification Method",
              })}
              value={bulkFormData.purificationMethod}
              onChange={(e) =>
                setBulkFormData({
                  ...bulkFormData,
                  purificationMethod: e.target.value,
                })
              }
            >
              {purificationMethods.map((opt) => (
                <SelectItem
                  key={opt.value}
                  value={opt.value}
                  text={opt.label}
                />
              ))}
            </Select>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <NumberInput
              id="purityLevel"
              label={intl.formatMessage({
                id: "notebook.page.tradmed.analytical.purityLevel",
                defaultMessage: "Purity Level (%)",
              })}
              value={bulkFormData.purityLevel}
              onChange={(e, { value }) =>
                setBulkFormData({ ...bulkFormData, purityLevel: value })
              }
              min={0}
              max={100}
              step={0.1}
              allowEmpty
            />
          </Column>

          {/* Structural Analysis Section */}
          <Column lg={16} md={8} sm={4}>
            <div className="checkbox-group">
              <span className="checkbox-group-label">
                <FormattedMessage
                  id="notebook.page.tradmed.analytical.structuralAnalysis"
                  defaultMessage="Structural Analysis Performed"
                />
              </span>
              <div className="checkbox-group-items">
                <Checkbox
                  id="structuralAnalysisNMR"
                  labelText="NMR (Nuclear Magnetic Resonance)"
                  checked={bulkFormData.structuralAnalysisNMR}
                  onChange={(_, { checked }) =>
                    setBulkFormData({
                      ...bulkFormData,
                      structuralAnalysisNMR: checked,
                    })
                  }
                />
                <Checkbox
                  id="structuralAnalysisMS"
                  labelText="MS (Mass Spectrometry)"
                  checked={bulkFormData.structuralAnalysisMS}
                  onChange={(_, { checked }) =>
                    setBulkFormData({
                      ...bulkFormData,
                      structuralAnalysisMS: checked,
                    })
                  }
                />
                <Checkbox
                  id="structuralAnalysisIR"
                  labelText="IR (Infrared Spectroscopy)"
                  checked={bulkFormData.structuralAnalysisIR}
                  onChange={(_, { checked }) =>
                    setBulkFormData({
                      ...bulkFormData,
                      structuralAnalysisIR: checked,
                    })
                  }
                />
              </div>
            </div>
          </Column>

          {/* Spectral Files */}
          <Column lg={16} md={8} sm={4}>
            <TextInput
              id="spectralFileReference"
              labelText={intl.formatMessage({
                id: "notebook.page.tradmed.analytical.spectralFileReference",
                defaultMessage: "Spectral File Reference",
              })}
              placeholder={intl.formatMessage({
                id: "notebook.page.tradmed.analytical.spectralFilePlaceholder",
                defaultMessage: "File path or reference ID for spectral data",
              })}
              value={bulkFormData.spectralFileReference}
              onChange={(e) =>
                setBulkFormData({
                  ...bulkFormData,
                  spectralFileReference: e.target.value,
                })
              }
            />
          </Column>

          {/* Notes */}
          <Column lg={16} md={8} sm={4}>
            <TextArea
              id="characterizationNotes"
              labelText={intl.formatMessage({
                id: "notebook.page.tradmed.analytical.notes",
                defaultMessage: "Characterization Notes",
              })}
              value={bulkFormData.characterizationNotes}
              onChange={(e) =>
                setBulkFormData({
                  ...bulkFormData,
                  characterizationNotes: e.target.value,
                })
              }
              rows={3}
            />
          </Column>
        </Grid>
      </Modal>
    </div>
  );
}

export default TraditionalMedicineAnalyticalPage;
