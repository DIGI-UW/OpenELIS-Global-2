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
  TextArea,
  Tile,
  Modal,
  Tag,
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
 * SmearMicroscopyPanel - AFB Smear Microscopy results panel.
 *
 * Smear Methods:
 * - ZN (Ziehl-Neelsen)
 * - CONCENTRATED (Concentrated smear)
 * - FLUORESCENT (Fluorescent microscopy)
 * - OTHER
 *
 * AFB Results:
 * - NEGATIVE: No AFB seen
 * - SCANTY: 1-9 AFB per 100 fields
 * - PLUS1: 10-99 AFB per 100 fields
 * - PLUS2: 1-10 AFB per field
 * - PLUS3: >10 AFB per field
 */
function SmearMicroscopyPanel({
  pageData,
  onProgressUpdate,
  cultureSamples = [],
}) {
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
        smearMethod: null,
        afbResult: null,
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

  // Smear result form state
  const [resultData, setResultData] = useState({
    method: "ZN",
    afbResult: "",
    resultDate: configurationProperties?.currentDateAsText || "",
    notes: "",
  });

  // Smear method options
  const smearMethodOptions = [
    { value: "ZN", label: "ZN (Ziehl-Neelsen)" },
    { value: "CONCENTRATED", label: "Concentrated Smear" },
    { value: "FLUORESCENT", label: "Fluorescent Microscopy" },
    { value: "OTHER", label: "Other" },
  ];

  // AFB result options
  const afbResultOptions = [
    { value: "NEGATIVE", label: "Negative (No AFB)" },
    { value: "SCANTY", label: "Scanty (1-9 AFB/100 fields)" },
    { value: "PLUS1", label: "1+ (10-99 AFB/100 fields)" },
    { value: "PLUS2", label: "2+ (1-10 AFB/field)" },
    { value: "PLUS3", label: "3+ (>10 AFB/field)" },
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
              // Smear result fields
              smearMethod: sample.data?.smearMethod,
              afbResult: sample.data?.afbResult,
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
      method: "ZN",
      afbResult: "",
      resultDate: configurationProperties?.currentDateAsText || "",
      notes: "",
    });
  };

  // Calculate stats
  const stats = useMemo(() => {
    const tested = samples.filter((s) => s.afbResult).length;
    const positive = samples.filter(
      (s) => s.afbResult && s.afbResult !== "NEGATIVE",
    ).length;
    const negative = samples.filter((s) => s.afbResult === "NEGATIVE").length;
    const highLoad = samples.filter(
      (s) => s.afbResult === "PLUS2" || s.afbResult === "PLUS3",
    ).length;
    const pending = samples.filter((s) => !s.afbResult).length;

    return {
      total: samples.length,
      tested,
      positive,
      negative,
      highLoad,
      pending,
    };
  }, [samples]);

  // Handle opening result modal
  const handleOpenResultModal = useCallback(() => {
    if (selectedSampleIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.page.tb.smear.error.noSelection",
          defaultMessage: "Please select at least one sample.",
        }),
      );
      return;
    }
    setResultModalOpen(true);
  }, [selectedSampleIds, intl]);

  // Handle saving smear result
  const handleSaveResult = useCallback(() => {
    if (!resultData.afbResult) {
      setError(
        intl.formatMessage({
          id: "notebook.page.tb.smear.error.noResult",
          defaultMessage: "Please select an AFB result.",
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
      smearMethod: resultData.method,
      afbResult: resultData.afbResult,
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
              const isPositive = resultData.afbResult !== "NEGATIVE";
              setSuccessMessage(
                intl.formatMessage(
                  {
                    id: isPositive
                      ? "notebook.page.tb.smear.resultSaved.positive"
                      : "notebook.page.tb.smear.resultSaved.negative",
                    defaultMessage: isPositive
                      ? "AFB positive result recorded for {count} sample(s)."
                      : "AFB negative result recorded for {count} sample(s).",
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
              id: "notebook.page.tb.smear.error.save",
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

    // Check if selected samples have AFB result
    const selectedSamples = samples.filter((s) =>
      selectedSampleIds.includes(s.id),
    );
    const withoutResult = selectedSamples.filter((s) => !s.afbResult);
    if (withoutResult.length > 0) {
      setError(
        intl.formatMessage(
          {
            id: "notebook.page.tb.smear.error.missingResult",
            defaultMessage: "{count} sample(s) do not have a smear result yet.",
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
                id: "notebook.page.tb.smear.markedComplete",
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

  // Get AFB result tag
  const getAfbResultTag = (result) => {
    if (!result) return <Tag type="gray">Pending</Tag>;
    if (result === "NEGATIVE") return <Tag type="blue">Negative</Tag>;
    if (result === "SCANTY") return <Tag type="teal">Scanty</Tag>;
    if (result === "PLUS1") return <Tag type="cyan">1+</Tag>;
    if (result === "PLUS2") return <Tag type="green">2+</Tag>;
    if (result === "PLUS3") return <Tag type="green">3+</Tag>;
    return <Tag type="gray">{result}</Tag>;
  };

  // Get smear method tag
  const getSmearMethodTag = (method) => {
    if (!method) return null;
    const methodLabels = {
      ZN: { type: "purple", label: "ZN" },
      CONCENTRATED: { type: "cyan", label: "Concentrated" },
      FLUORESCENT: { type: "teal", label: "Fluorescent" },
      OTHER: { type: "gray", label: "Other" },
    };
    const config = methodLabels[method] || { type: "gray", label: method };
    return <Tag type={config.type}>{config.label}</Tag>;
  };

  // Get positivity indicator
  const getPositivityTag = (result) => {
    if (!result) return null;
    if (result === "NEGATIVE") {
      return <Tag type="blue">AFB Negative</Tag>;
    }
    return <Tag type="green">AFB Positive</Tag>;
  };

  return (
    <div className="smear-microscopy-panel">
      {/* Section Header */}
      <div className="panel-section-header">
        <h5>
          <FormattedMessage
            id="notebook.page.tb.smear.title"
            defaultMessage="AFB Smear Microscopy"
          />
        </h5>
        <p className="panel-description">
          <FormattedMessage
            id="notebook.page.tb.smear.description"
            defaultMessage="Record Acid-Fast Bacilli (AFB) smear microscopy results. Select samples and enter results using the appropriate staining method."
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
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.tb.smear.positive"
                  defaultMessage="AFB Positive"
                />
              </span>
              <span className="progress-value">{stats.positive}</span>
            </Tile>
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.tb.smear.negative"
                  defaultMessage="AFB Negative"
                />
              </span>
              <span className="progress-value">{stats.negative}</span>
            </Tile>
            <Tile className="progress-tile error">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.tb.smear.highLoad"
                  defaultMessage="High Load (2+/3+)"
                />
              </span>
              <span className="progress-value">{stats.highLoad}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.tb.smear.pending"
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
            id="notebook.page.tb.smear.addResult"
            defaultMessage="Enter Smear Result ({count})"
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
              id="notebook.page.tb.smear.markComplete"
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
              id="notebook.page.tb.smear.pendingTable.title"
              defaultMessage="Samples Pending Smear"
            />
            <Tag type="gray" className="count-tag">
              {samples.filter((s) => !s.afbResult).length}
            </Tag>
          </h5>
          <p className="table-section-description">
            <FormattedMessage
              id="notebook.page.tb.smear.pendingTable.description"
              defaultMessage="Select samples and enter AFB smear microscopy results."
            />
          </p>
        </div>
        <div className="sample-grid-container">
          <SampleGrid
            gridId="smear-pending"
            samples={samples.filter((s) => !s.afbResult)}
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
                key: "specimenType",
                header: intl.formatMessage({
                  id: "notebook.grid.specimenType",
                  defaultMessage: "Specimen Type",
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
                key: "smearMethod",
                header: intl.formatMessage({
                  id: "notebook.grid.smearMethod",
                  defaultMessage: "Method",
                }),
                render: (value) => getSmearMethodTag(value),
              },
              {
                key: "afbResult",
                header: intl.formatMessage({
                  id: "notebook.grid.afbResult",
                  defaultMessage: "AFB Result",
                }),
                render: (value) => getAfbResultTag(value),
              },
            ]}
          />
        </div>
        {!loading && samples.filter((s) => !s.afbResult).length === 0 && (
          <div className="empty-table-state">
            <p>
              <FormattedMessage
                id="notebook.page.tb.smear.pendingTable.empty"
                defaultMessage="No samples pending smear microscopy."
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
              id="notebook.page.tb.smear.completedTable.title"
              defaultMessage="Completed Smear Results"
            />
            <Tag type="green" className="count-tag">
              {samples.filter((s) => s.afbResult).length}
            </Tag>
          </h5>
          <p className="table-section-description">
            <FormattedMessage
              id="notebook.page.tb.smear.completedTable.description"
              defaultMessage="Samples with completed AFB smear results."
            />
          </p>
        </div>
        <div className="sample-grid-container">
          <SampleGrid
            gridId="smear-completed"
            samples={samples.filter((s) => s.afbResult)}
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
                key: "smearMethod",
                header: intl.formatMessage({
                  id: "notebook.grid.smearMethod",
                  defaultMessage: "Method",
                }),
                render: (value) => getSmearMethodTag(value),
              },
              {
                key: "afbResult",
                header: intl.formatMessage({
                  id: "notebook.grid.afbResult",
                  defaultMessage: "AFB Result",
                }),
                render: (value) => getAfbResultTag(value),
              },
              {
                key: "positivity",
                header: intl.formatMessage({
                  id: "notebook.grid.positivity",
                  defaultMessage: "Positivity",
                }),
                render: (_, row) => getPositivityTag(row.afbResult),
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
        {!loading && samples.filter((s) => s.afbResult).length === 0 && (
          <div className="empty-table-state">
            <p>
              <FormattedMessage
                id="notebook.page.tb.smear.completedTable.empty"
                defaultMessage="No completed smear results yet."
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
              id="notebook.page.tb.smear.empty"
              defaultMessage="No samples available for smear microscopy. Complete previous workflow steps first."
            />
          </p>
        </div>
      )}

      {/* Smear Result Modal */}
      <Modal
        open={resultModalOpen}
        onRequestClose={() => setResultModalOpen(false)}
        modalHeading={intl.formatMessage({
          id: "notebook.tb.smear.modal.title",
          defaultMessage: "Enter AFB Smear Result",
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
        primaryButtonDisabled={isSaving || !resultData.afbResult}
      >
        <div className="smear-result-modal">
          <p className="modal-description">
            <FormattedMessage
              id="notebook.tb.smear.modal.description"
              defaultMessage="Record AFB smear microscopy result for {count} selected sample(s)."
              values={{ count: selectedSampleIds.length }}
            />
          </p>

          <Grid fullWidth>
            {/* Smear Method */}
            <Column lg={8} md={4} sm={4}>
              <Select
                id="smearMethod"
                labelText={intl.formatMessage({
                  id: "notebook.tb.smear.method",
                  defaultMessage: "Staining Method",
                })}
                value={resultData.method}
                onChange={(e) =>
                  setResultData((prev) => ({
                    ...prev,
                    method: e.target.value,
                  }))
                }
              >
                {smearMethodOptions.map((option) => (
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
                  id: "notebook.tb.smear.resultDate",
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

            {/* AFB Result */}
            <Column lg={16} md={8} sm={4}>
              <Select
                id="afbResult"
                labelText={intl.formatMessage({
                  id: "notebook.tb.smear.afbResult",
                  defaultMessage: "AFB Result",
                })}
                value={resultData.afbResult}
                onChange={(e) =>
                  setResultData((prev) => ({
                    ...prev,
                    afbResult: e.target.value,
                  }))
                }
              >
                <SelectItem value="" text="Select AFB result..." />
                {afbResultOptions.map((option) => (
                  <SelectItem
                    key={option.value}
                    value={option.value}
                    text={option.label}
                  />
                ))}
              </Select>
            </Column>

            {/* Notes */}
            <Column lg={16} md={8} sm={4}>
              <TextArea
                id="notes"
                labelText={intl.formatMessage({
                  id: "notebook.tb.smear.notes",
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
                  id: "notebook.tb.smear.notes.placeholder",
                  defaultMessage:
                    "Add any observations about the smear result...",
                })}
                rows={3}
              />
            </Column>
          </Grid>

          {/* Result preview */}
          {resultData.afbResult && (
            <div className="result-preview" style={{ marginTop: "1rem" }}>
              <InlineNotification
                kind={resultData.afbResult === "NEGATIVE" ? "info" : "success"}
                title={intl.formatMessage({
                  id:
                    resultData.afbResult === "NEGATIVE"
                      ? "notebook.tb.smear.preview.negative"
                      : "notebook.tb.smear.preview.positive",
                  defaultMessage:
                    resultData.afbResult === "NEGATIVE"
                      ? "AFB Negative"
                      : "AFB Positive",
                })}
                subtitle={
                  resultData.afbResult !== "NEGATIVE"
                    ? intl.formatMessage({
                        id: "notebook.tb.smear.preview.positiveNote",
                        defaultMessage:
                          "Sample shows presence of acid-fast bacilli. Consider for further testing.",
                      })
                    : intl.formatMessage({
                        id: "notebook.tb.smear.preview.negativeNote",
                        defaultMessage:
                          "No acid-fast bacilli detected. Culture recommended for confirmation.",
                      })
                }
                hideCloseButton
                lowContrast
              />
            </div>
          )}

          {/* High load warning */}
          {(resultData.afbResult === "PLUS2" ||
            resultData.afbResult === "PLUS3") && (
            <InlineNotification
              kind="warning"
              title={intl.formatMessage({
                id: "notebook.tb.smear.highLoad.title",
                defaultMessage: "High Bacillary Load",
              })}
              subtitle={intl.formatMessage({
                id: "notebook.tb.smear.highLoad.subtitle",
                defaultMessage:
                  "Patient may be highly infectious. Follow appropriate biosafety protocols.",
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

export default SmearMicroscopyPanel;
