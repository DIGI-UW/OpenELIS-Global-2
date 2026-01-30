import {
  useState,
  useEffect,
  useRef,
  useCallback,
  useMemo,
  useContext,
} from "react";
import {
  Grid,
  Column,
  Button,
  InlineNotification,
  Select,
  SelectItem,
  TextInput,
  TextArea,
  Tile,
  Modal,
  Tag,
  NumberInput,
} from "@carbon/react";
import { Checkmark, Add, Renew } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
  convertToISODate,
} from "../../../../utils/Utils";
import SampleGrid from "../../../workflow/SampleGrid";
import CustomDatePicker from "../../../../common/CustomDatePicker";
import { ConfigurationContext } from "../../../../layout/Layout";

/**
 * GeneXpertPanel - Molecular PCR (GeneXpert MTB/RIF) results panel.
 *
 * GeneXpert Methods:
 * - GENEXPERT: Xpert MTB/RIF assay
 * - REALTIME_PCR: Other real-time PCR methods
 * - OTHER: Other molecular methods
 *
 * GeneXpert Results:
 * - MTB_NOT_DETECTED: No MTB detected
 * - MTB_DETECTED_RIF_SENSITIVE: MTB detected, RIF resistance not detected
 * - MTB_DETECTED_RIF_RESISTANT: MTB detected, RIF resistance detected
 * - MTB_DETECTED_RIF_INDETERMINATE: MTB detected, RIF resistance indeterminate
 * - INVALID: Invalid result
 * - ERROR: Error during processing
 * - NO_RESULT: No result obtained
 */
function GeneXpertPanel({ pageData, onProgressUpdate, cultureSamples = [] }) {
  const intl = useIntl();
  const { configurationProperties } = useContext(ConfigurationContext);
  const componentMounted = useRef(false);

  const [pageSamples, setPageSamples] = useState([]);
  const [selectedSampleIds, setSelectedSampleIds] = useState([]);
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [loading, setLoading] = useState(true);

  // Merge page samples with culture-positive samples
  const samples = useMemo(() => {
    // Create a map of culture data by sampleItemId for easy lookup
    const cultureDataMap = new Map();
    cultureSamples
      .filter((s) => s.cultureResult === "POSITIVE")
      .forEach((sample) => {
        cultureDataMap.set(sample.sampleItemId, {
          cultureResult: sample.cultureResult,
          cultureMethod: sample.cultureMethod,
          positiveWeek: sample.positiveWeek,
        });
      });

    // Enrich page samples with culture data
    const enrichedPageSamples = pageSamples.map((sample) => {
      const cultureData = cultureDataMap.get(sample.id);
      if (cultureData) {
        return {
          ...sample,
          cultureResult: cultureData.cultureResult,
          cultureMethod: cultureData.cultureMethod,
          positiveWeek: cultureData.positiveWeek,
        };
      }
      return sample;
    });

    // Add culture-positive samples that aren't in page samples
    const pageSampleIds = new Set(pageSamples.map((s) => s.id));
    const additionalCultureSamples = cultureSamples
      .filter(
        (s) =>
          s.cultureResult === "POSITIVE" && !pageSampleIds.has(s.sampleItemId),
      )
      .map((sample) => ({
        id: sample.sampleItemId,
        externalId: sample.externalId,
        accessionNumber: sample.accessionNumber,
        sampleType: null,
        specimenType: null,
        status: "PENDING",
        source: "culture",
        cultureResult: sample.cultureResult,
        cultureMethod: sample.cultureMethod,
        positiveWeek: sample.positiveWeek,
        // GeneXpert fields
        geneXpertMethod: null,
        geneXpertResult: null,
        ctValue: null,
        rifCtValue: null,
        cartridgeLot: null,
        equipmentId: null,
        resultDate: null,
        notes: null,
      }));

    return [...enrichedPageSamples, ...additionalCultureSamples];
  }, [pageSamples, cultureSamples]);

  const [error, setError] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);

  // Result entry modal state
  const [resultModalOpen, setResultModalOpen] = useState(false);
  const [isSaving, setIsSaving] = useState(false);

  // GeneXpert result form state
  const [resultData, setResultData] = useState({
    method: "GENEXPERT",
    geneXpertResult: "",
    ctValue: "",
    rifCtValue: "",
    cartridgeLot: "",
    equipmentId: "",
    resultDate: configurationProperties?.currentDateAsText || "",
    notes: "",
  });

  // Molecular method options
  const methodOptions = [
    { value: "GENEXPERT", label: "GeneXpert MTB/RIF" },
    { value: "REALTIME_PCR", label: "Real-time PCR" },
    { value: "OTHER", label: "Other Molecular Method" },
  ];

  // GeneXpert result options
  const geneXpertResultOptions = [
    { value: "MTB_NOT_DETECTED", label: "MTB Not Detected" },
    {
      value: "MTB_DETECTED_RIF_SENSITIVE",
      label: "MTB Detected, RIF Sensitive",
    },
    {
      value: "MTB_DETECTED_RIF_RESISTANT",
      label: "MTB Detected, RIF Resistant",
    },
    {
      value: "MTB_DETECTED_RIF_INDETERMINATE",
      label: "MTB Detected, RIF Indeterminate",
    },
    { value: "INVALID", label: "Invalid" },
    { value: "ERROR", label: "Error" },
    { value: "NO_RESULT", label: "No Result" },
  ];

  useEffect(() => {
    componentMounted.current = true;
    loadPageSamples();
    return () => {
      componentMounted.current = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pageData?.id]);

  const loadPageSamples = useCallback(() => {
    if (!pageData?.id) {
      setLoading(false);
      return;
    }

    if (String(pageData.id).startsWith("default-")) {
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
              sampleType: sample.sampleType || sample.typeOfSample?.description,
              specimenType: sample.data?.specimenType,
              status: sample.pageStatus || sample.status || "PENDING",
              // GeneXpert result fields
              geneXpertMethod: sample.data?.geneXpertMethod,
              geneXpertResult: sample.data?.geneXpertResult,
              ctValue: sample.data?.ctValue,
              rifCtValue: sample.data?.rifCtValue,
              cartridgeLot: sample.data?.cartridgeLot,
              equipmentId: sample.data?.equipmentId,
              resultDate: sample.data?.resultDate,
              notes: sample.data?.notes,
            }));
            setPageSamples(transformedSamples);
          } else {
            setPageSamples([]);
          }
          setLoading(false);
        }
      },
    );
  }, [pageData?.id]);

  const hasRealPageId =
    pageData?.id && !String(pageData.id).startsWith("default-");

  // Reset result form
  const resetResultData = () => {
    setResultData({
      method: "GENEXPERT",
      geneXpertResult: "",
      ctValue: "",
      rifCtValue: "",
      cartridgeLot: "",
      equipmentId: "",
      resultDate: configurationProperties?.currentDateAsText || "",
      notes: "",
    });
  };

  // Calculate stats
  const stats = useMemo(() => {
    const tested = samples.filter((s) => s.geneXpertResult).length;
    const mtbDetected = samples.filter(
      (s) => s.geneXpertResult && s.geneXpertResult.startsWith("MTB_DETECTED"),
    ).length;
    const rifSensitive = samples.filter(
      (s) => s.geneXpertResult === "MTB_DETECTED_RIF_SENSITIVE",
    ).length;
    const rifResistant = samples.filter(
      (s) => s.geneXpertResult === "MTB_DETECTED_RIF_RESISTANT",
    ).length;
    const rifIndeterminate = samples.filter(
      (s) => s.geneXpertResult === "MTB_DETECTED_RIF_INDETERMINATE",
    ).length;
    const mtbNotDetected = samples.filter(
      (s) => s.geneXpertResult === "MTB_NOT_DETECTED",
    ).length;
    const pending = samples.filter((s) => !s.geneXpertResult).length;

    return {
      total: samples.length,
      tested,
      mtbDetected,
      rifSensitive,
      rifResistant,
      rifIndeterminate,
      mtbNotDetected,
      pending,
    };
  }, [samples]);

  // Handle opening result modal
  const handleOpenResultModal = useCallback(() => {
    if (selectedSampleIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.page.tb.genexpert.error.noSelection",
          defaultMessage: "Please select at least one sample.",
        }),
      );
      return;
    }
    setResultModalOpen(true);
  }, [selectedSampleIds, intl]);

  // Handle saving GeneXpert result
  const handleSaveResult = useCallback(() => {
    if (!resultData.geneXpertResult) {
      setError(
        intl.formatMessage({
          id: "notebook.page.tb.genexpert.error.noResult",
          defaultMessage: "Please select a GeneXpert result.",
        }),
      );
      return;
    }

    if (!hasRealPageId) {
      setError(
        intl.formatMessage({
          id: "notebook.page.tb.error.noPage",
          defaultMessage:
            "Cannot update samples: Page not properly initialized.",
        }),
      );
      return;
    }

    setIsSaving(true);
    setError(null);

    const data = {
      geneXpertMethod: resultData.method,
      geneXpertResult: resultData.geneXpertResult,
      ctValue: resultData.ctValue || null,
      rifCtValue: resultData.rifCtValue || null,
      cartridgeLot: resultData.cartridgeLot || null,
      equipmentId: resultData.equipmentId || null,
      resultDate: convertToISODate(resultData.resultDate),
      notes: resultData.notes,
    };

    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
      JSON.stringify({
        sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
        data,
      }),
      (status) => {
        if (status === 200) {
          // Update status to COMPLETED
          postToOpenElisServer(
            `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
            JSON.stringify({
              sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
              status: "COMPLETED",
            }),
            () => {
              setIsSaving(false);
              const isRifResistant =
                resultData.geneXpertResult === "MTB_DETECTED_RIF_RESISTANT";
              setSuccessMessage(
                intl.formatMessage(
                  {
                    id: isRifResistant
                      ? "notebook.page.tb.genexpert.resultSaved.resistant"
                      : "notebook.page.tb.genexpert.resultSaved",
                    defaultMessage: isRifResistant
                      ? "RIF RESISTANT result recorded for {count} sample(s). Consider DST testing."
                      : "GeneXpert result recorded for {count} sample(s).",
                  },
                  { count: selectedSampleIds.length },
                ),
              );
              setResultModalOpen(false);
              setSelectedSampleIds([]);
              resetResultData();
              loadPageSamples();
              if (onProgressUpdate) {
                onProgressUpdate();
              }
            },
          );
        } else {
          setIsSaving(false);
          setError(
            intl.formatMessage({
              id: "notebook.page.tb.genexpert.error.save",
              defaultMessage: "Failed to save result. Please try again.",
            }),
          );
        }
      },
    );
  }, [
    resultData,
    selectedSampleIds,
    hasRealPageId,
    intl,
    loadPageSamples,
    onProgressUpdate,
    pageData?.id,
  ]);

  // Mark samples as complete
  const handleMarkComplete = useCallback(() => {
    if (selectedSampleIds.length === 0) return;

    if (!hasRealPageId) {
      setError(
        intl.formatMessage({
          id: "notebook.page.tb.error.noPage",
          defaultMessage:
            "Cannot update samples: Page not properly initialized.",
        }),
      );
      return;
    }

    // Check if selected samples have GeneXpert result
    const selectedSamples = samples.filter((s) =>
      selectedSampleIds.includes(s.id),
    );
    const withoutResult = selectedSamples.filter((s) => !s.geneXpertResult);
    if (withoutResult.length > 0) {
      setError(
        intl.formatMessage(
          {
            id: "notebook.page.tb.genexpert.error.missingResult",
            defaultMessage:
              "{count} sample(s) do not have a GeneXpert result yet.",
          },
          { count: withoutResult.length },
        ),
      );
      return;
    }

    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
      JSON.stringify({
        sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
        status: "COMPLETED",
      }),
      (status) => {
        if (status === 200) {
          setSuccessMessage(
            intl.formatMessage(
              {
                id: "notebook.page.tb.genexpert.markedComplete",
                defaultMessage: "Marked {count} sample(s) as complete.",
              },
              { count: selectedSampleIds.length },
            ),
          );
          setSelectedSampleIds([]);
          loadPageSamples();
          if (onProgressUpdate) {
            onProgressUpdate();
          }
        } else {
          setError(
            intl.formatMessage({
              id: "notebook.page.tb.error.status",
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

  // Get GeneXpert result tag
  const getGeneXpertResultTag = (result) => {
    if (!result) return <Tag type="gray">Pending</Tag>;
    const config = {
      MTB_NOT_DETECTED: { type: "blue", label: "MTB Not Detected" },
      MTB_DETECTED_RIF_SENSITIVE: {
        type: "green",
        label: "MTB+, RIF Sensitive",
      },
      MTB_DETECTED_RIF_RESISTANT: { type: "red", label: "MTB+, RIF Resistant" },
      MTB_DETECTED_RIF_INDETERMINATE: {
        type: "purple",
        label: "MTB+, RIF Indeterminate",
      },
      INVALID: { type: "magenta", label: "Invalid" },
      ERROR: { type: "magenta", label: "Error" },
      NO_RESULT: { type: "gray", label: "No Result" },
    };
    const cfg = config[result] || { type: "gray", label: result };
    return <Tag type={cfg.type}>{cfg.label}</Tag>;
  };

  // Get method tag
  const getMethodTag = (method) => {
    if (!method) return null;
    const methodLabels = {
      GENEXPERT: { type: "teal", label: "GeneXpert" },
      REALTIME_PCR: { type: "cyan", label: "RT-PCR" },
      OTHER: { type: "gray", label: "Other" },
    };
    const config = methodLabels[method] || { type: "gray", label: method };
    return <Tag type={config.type}>{config.label}</Tag>;
  };

  return (
    <div className="genexpert-panel">
      {/* Section Header */}
      <div className="panel-section-header">
        <h5>
          <FormattedMessage
            id="notebook.page.tb.genexpert.title"
            defaultMessage="GeneXpert (Molecular PCR)"
          />
        </h5>
        <p className="panel-description">
          <FormattedMessage
            id="notebook.page.tb.genexpert.description"
            defaultMessage="Record GeneXpert MTB/RIF assay results. Detects MTB and rifampicin resistance in a single test."
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
                  id="notebook.page.tb.totalSamples"
                  defaultMessage="Total Samples"
                />
              </span>
              <span className="progress-value">{stats.total}</span>
            </Tile>
            <Tile className="progress-tile rejected">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.tb.genexpert.mtbDetected"
                  defaultMessage="MTB Detected"
                />
              </span>
              <span className="progress-value">{stats.mtbDetected}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.tb.genexpert.rifSensitive"
                  defaultMessage="RIF Sensitive"
                />
              </span>
              <span className="progress-value">{stats.rifSensitive}</span>
            </Tile>
            <Tile className="progress-tile error">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.tb.genexpert.rifResistant"
                  defaultMessage="RIF Resistant"
                />
              </span>
              <span className="progress-value">{stats.rifResistant}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.tb.genexpert.pending"
                  defaultMessage="Pending"
                />
              </span>
              <span className="progress-value">{stats.pending}</span>
            </Tile>
          </div>
        </Column>
      </Grid>

      {/* Action Buttons */}
      <div className="page-actions-bar">
        <Button
          kind="primary"
          size="sm"
          renderIcon={Add}
          onClick={handleOpenResultModal}
          disabled={selectedSampleIds.length === 0}
        >
          <FormattedMessage
            id="notebook.page.tb.genexpert.addResult"
            defaultMessage="Enter GeneXpert Result ({count})"
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
              id="notebook.page.tb.genexpert.markComplete"
              defaultMessage="Mark Complete ({count})"
              values={{ count: selectedSampleIds.length }}
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
            id="notebook.page.tb.refresh"
            defaultMessage="Refresh"
          />
        </Button>
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

      {/* Pending Samples Table */}
      <div className="sample-table-section">
        <div className="table-section-header">
          <h5>
            <FormattedMessage
              id="notebook.page.tb.genexpert.pendingTable.title"
              defaultMessage="Samples Pending GeneXpert"
            />
            <Tag type="gray" className="count-tag">
              {samples.filter((s) => !s.geneXpertResult).length}
            </Tag>
          </h5>
          <p className="table-section-description">
            <FormattedMessage
              id="notebook.page.tb.genexpert.pendingTable.description"
              defaultMessage="Select samples and enter GeneXpert MTB/RIF results."
            />
          </p>
        </div>
        <div className="sample-grid-container">
          <SampleGrid
            gridId="genexpert-pending"
            samples={samples.filter((s) => !s.geneXpertResult)}
            selectedIds={selectedSampleIds}
            onSelectionChange={setSelectedSampleIds}
            statusFilter={statusFilter}
            onStatusFilterChange={setStatusFilter}
            showSelection={true}
            loading={loading}
            columns={[
              {
                key: "accessionNumber",
                header: intl.formatMessage({
                  id: "notebook.grid.accessionNumber",
                  defaultMessage: "Accession Number",
                }),
              },
              {
                key: "externalId",
                header: intl.formatMessage({
                  id: "notebook.grid.sampleId",
                  defaultMessage: "Sample ID",
                }),
              },
              {
                key: "cultureResult",
                header: intl.formatMessage({
                  id: "notebook.grid.cultureResult",
                  defaultMessage: "Culture",
                }),
                render: (value) =>
                  value ? (
                    <Tag type="red" size="sm">
                      Culture+
                    </Tag>
                  ) : null,
              },
              {
                key: "geneXpertMethod",
                header: intl.formatMessage({
                  id: "notebook.grid.geneXpertMethod",
                  defaultMessage: "Method",
                }),
                render: (value) => getMethodTag(value),
              },
              {
                key: "geneXpertResult",
                header: intl.formatMessage({
                  id: "notebook.grid.geneXpertResult",
                  defaultMessage: "Result",
                }),
                render: (value) => getGeneXpertResultTag(value),
              },
            ]}
          />
        </div>
        {!loading && samples.filter((s) => !s.geneXpertResult).length === 0 && (
          <div className="empty-table-state">
            <p>
              <FormattedMessage
                id="notebook.page.tb.genexpert.pendingTable.empty"
                defaultMessage="No samples pending GeneXpert testing."
              />
            </p>
          </div>
        )}
      </div>

      {/* Completed Results Table */}
      <div className="sample-table-section">
        <div className="table-section-header">
          <h5>
            <FormattedMessage
              id="notebook.page.tb.genexpert.completedTable.title"
              defaultMessage="Completed GeneXpert Results"
            />
            <Tag type="green" className="count-tag">
              {samples.filter((s) => s.geneXpertResult).length}
            </Tag>
          </h5>
          <p className="table-section-description">
            <FormattedMessage
              id="notebook.page.tb.genexpert.completedTable.description"
              defaultMessage="Samples with completed GeneXpert results."
            />
          </p>
        </div>
        <div className="sample-grid-container">
          <SampleGrid
            gridId="genexpert-completed"
            samples={samples.filter((s) => s.geneXpertResult)}
            selectedIds={[]}
            showSelection={false}
            loading={loading}
            columns={[
              {
                key: "accessionNumber",
                header: intl.formatMessage({
                  id: "notebook.grid.accessionNumber",
                  defaultMessage: "Accession Number",
                }),
              },
              {
                key: "externalId",
                header: intl.formatMessage({
                  id: "notebook.grid.sampleId",
                  defaultMessage: "Sample ID",
                }),
              },
              {
                key: "geneXpertMethod",
                header: intl.formatMessage({
                  id: "notebook.grid.geneXpertMethod",
                  defaultMessage: "Method",
                }),
                render: (value) => getMethodTag(value),
              },
              {
                key: "geneXpertResult",
                header: intl.formatMessage({
                  id: "notebook.grid.geneXpertResult",
                  defaultMessage: "Result",
                }),
                render: (value) => getGeneXpertResultTag(value),
              },
              {
                key: "ctValue",
                header: intl.formatMessage({
                  id: "notebook.grid.ctValue",
                  defaultMessage: "CT Value",
                }),
              },
              {
                key: "resultDate",
                header: intl.formatMessage({
                  id: "notebook.grid.resultDate",
                  defaultMessage: "Result Date",
                }),
              },
            ]}
          />
        </div>
        {!loading && samples.filter((s) => s.geneXpertResult).length === 0 && (
          <div className="empty-table-state">
            <p>
              <FormattedMessage
                id="notebook.page.tb.genexpert.completedTable.empty"
                defaultMessage="No completed GeneXpert results yet."
              />
            </p>
          </div>
        )}
      </div>

      {/* Global empty state */}
      {!loading && samples.length === 0 && (
        <div className="empty-state global-empty">
          <p>
            <FormattedMessage
              id="notebook.page.tb.genexpert.empty"
              defaultMessage="No samples available for GeneXpert testing. Culture-positive samples will appear here."
            />
          </p>
        </div>
      )}

      {/* GeneXpert Result Modal */}
      <Modal
        open={resultModalOpen}
        onRequestClose={() => setResultModalOpen(false)}
        modalHeading={intl.formatMessage({
          id: "notebook.tb.genexpert.modal.title",
          defaultMessage: "Enter GeneXpert Result",
        })}
        primaryButtonText={
          isSaving
            ? intl.formatMessage({
                id: "label.saving",
                defaultMessage: "Saving...",
              })
            : intl.formatMessage({
                id: "label.save",
                defaultMessage: "Save Result",
              })
        }
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestSubmit={handleSaveResult}
        onSecondarySubmit={() => setResultModalOpen(false)}
        size="md"
        primaryButtonDisabled={isSaving || !resultData.geneXpertResult}
      >
        <div className="genexpert-result-modal">
          <p className="modal-description">
            <FormattedMessage
              id="notebook.tb.genexpert.modal.description"
              defaultMessage="Record GeneXpert MTB/RIF result for {count} selected sample(s)."
              values={{ count: selectedSampleIds.length }}
            />
          </p>

          <Grid fullWidth>
            {/* Method */}
            <Column lg={8} md={4} sm={4}>
              <Select
                id="geneXpertMethod"
                labelText={intl.formatMessage({
                  id: "notebook.tb.genexpert.method",
                  defaultMessage: "Molecular Method",
                })}
                value={resultData.method}
                onChange={(e) =>
                  setResultData((prev) => ({
                    ...prev,
                    method: e.target.value,
                  }))
                }
              >
                {methodOptions.map((option) => (
                  <SelectItem
                    key={option.value}
                    value={option.value}
                    text={option.label}
                  />
                ))}
              </Select>
            </Column>

            {/* Result Date */}
            <Column lg={8} md={4} sm={4}>
              <CustomDatePicker
                id="resultDate"
                labelText={intl.formatMessage({
                  id: "notebook.tb.genexpert.resultDate",
                  defaultMessage: "Result Date",
                })}
                value={resultData.resultDate}
                onChange={(date) =>
                  setResultData((prev) => ({
                    ...prev,
                    resultDate: date,
                  }))
                }
              />
            </Column>

            {/* GeneXpert Result */}
            <Column lg={16} md={8} sm={4}>
              <Select
                id="geneXpertResult"
                labelText={intl.formatMessage({
                  id: "notebook.tb.genexpert.result",
                  defaultMessage: "GeneXpert Result",
                })}
                value={resultData.geneXpertResult}
                onChange={(e) =>
                  setResultData((prev) => ({
                    ...prev,
                    geneXpertResult: e.target.value,
                  }))
                }
              >
                <SelectItem value="" text="Select result..." />
                {geneXpertResultOptions.map((option) => (
                  <SelectItem
                    key={option.value}
                    value={option.value}
                    text={option.label}
                  />
                ))}
              </Select>
            </Column>

            {/* CT Values (only for MTB detected results) */}
            {resultData.geneXpertResult &&
              resultData.geneXpertResult.startsWith("MTB_DETECTED") && (
                <>
                  <Column lg={8} md={4} sm={4}>
                    <NumberInput
                      id="ctValue"
                      label={intl.formatMessage({
                        id: "notebook.tb.genexpert.ctValue",
                        defaultMessage: "CT Value (MTB)",
                      })}
                      value={resultData.ctValue}
                      onChange={(_, { value }) =>
                        setResultData((prev) => ({
                          ...prev,
                          ctValue: value,
                        }))
                      }
                      min={0}
                      max={50}
                      step={0.1}
                      allowEmpty
                      hideSteppers
                    />
                  </Column>
                  <Column lg={8} md={4} sm={4}>
                    <NumberInput
                      id="rifCtValue"
                      label={intl.formatMessage({
                        id: "notebook.tb.genexpert.rifCtValue",
                        defaultMessage: "CT Value (RIF)",
                      })}
                      value={resultData.rifCtValue}
                      onChange={(_, { value }) =>
                        setResultData((prev) => ({
                          ...prev,
                          rifCtValue: value,
                        }))
                      }
                      min={0}
                      max={50}
                      step={0.1}
                      allowEmpty
                      hideSteppers
                    />
                  </Column>
                </>
              )}

            {/* Cartridge & Equipment */}
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="cartridgeLot"
                labelText={intl.formatMessage({
                  id: "notebook.tb.genexpert.cartridgeLot",
                  defaultMessage: "Cartridge Lot",
                })}
                value={resultData.cartridgeLot}
                onChange={(e) =>
                  setResultData((prev) => ({
                    ...prev,
                    cartridgeLot: e.target.value,
                  }))
                }
                placeholder="e.g., LOT-2024-001"
              />
            </Column>
            <Column lg={8} md={4} sm={4}>
              <TextInput
                id="equipmentId"
                labelText={intl.formatMessage({
                  id: "notebook.tb.genexpert.equipmentId",
                  defaultMessage: "Equipment ID",
                })}
                value={resultData.equipmentId}
                onChange={(e) =>
                  setResultData((prev) => ({
                    ...prev,
                    equipmentId: e.target.value,
                  }))
                }
                placeholder="e.g., GX-001"
              />
            </Column>

            {/* Notes */}
            <Column lg={16} md={8} sm={4}>
              <TextArea
                id="notes"
                labelText={intl.formatMessage({
                  id: "notebook.tb.genexpert.notes",
                  defaultMessage: "Notes",
                })}
                value={resultData.notes}
                onChange={(e) =>
                  setResultData((prev) => ({
                    ...prev,
                    notes: e.target.value,
                  }))
                }
                placeholder={intl.formatMessage({
                  id: "notebook.tb.genexpert.notes.placeholder",
                  defaultMessage: "Add any observations about the test...",
                })}
                rows={3}
              />
            </Column>
          </Grid>

          {/* RIF Resistant Warning */}
          {resultData.geneXpertResult === "MTB_DETECTED_RIF_RESISTANT" && (
            <InlineNotification
              kind="error"
              title={intl.formatMessage({
                id: "notebook.tb.genexpert.rifResistant.title",
                defaultMessage: "RIF Resistance Detected",
              })}
              subtitle={intl.formatMessage({
                id: "notebook.tb.genexpert.rifResistant.subtitle",
                defaultMessage:
                  "This indicates potential MDR-TB. Patient should be referred for second-line DST and appropriate treatment.",
              })}
              hideCloseButton
              lowContrast
              style={{ marginTop: "1rem" }}
            />
          )}

          {/* MTB Detected Info */}
          {resultData.geneXpertResult &&
            resultData.geneXpertResult.startsWith("MTB_DETECTED") &&
            resultData.geneXpertResult !== "MTB_DETECTED_RIF_RESISTANT" && (
              <InlineNotification
                kind={
                  resultData.geneXpertResult === "MTB_DETECTED_RIF_SENSITIVE"
                    ? "success"
                    : "warning"
                }
                title={intl.formatMessage({
                  id: "notebook.tb.genexpert.mtbDetected.title",
                  defaultMessage: "MTB Detected",
                })}
                subtitle={intl.formatMessage({
                  id:
                    resultData.geneXpertResult === "MTB_DETECTED_RIF_SENSITIVE"
                      ? "notebook.tb.genexpert.rifSensitive.subtitle"
                      : "notebook.tb.genexpert.rifIndeterminate.subtitle",
                  defaultMessage:
                    resultData.geneXpertResult === "MTB_DETECTED_RIF_SENSITIVE"
                      ? "Patient can be started on first-line TB treatment."
                      : "RIF result indeterminate. Consider repeat testing or phenotypic DST.",
                })}
                hideCloseButton
                lowContrast
                style={{ marginTop: "1rem" }}
              />
            )}

          {/* MTB Not Detected Info */}
          {resultData.geneXpertResult === "MTB_NOT_DETECTED" && (
            <InlineNotification
              kind="info"
              title={intl.formatMessage({
                id: "notebook.tb.genexpert.mtbNotDetected.title",
                defaultMessage: "MTB Not Detected",
              })}
              subtitle={intl.formatMessage({
                id: "notebook.tb.genexpert.mtbNotDetected.subtitle",
                defaultMessage:
                  "Culture remains the gold standard. Continue monitoring culture results if clinical suspicion is high.",
              })}
              hideCloseButton
              lowContrast
              style={{ marginTop: "1rem" }}
            />
          )}
        </div>
      </Modal>
    </div>
  );
}

export default GeneXpertPanel;
