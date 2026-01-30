import { useState, useEffect, useRef, useCallback, useMemo } from "react";
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
  RadioButtonGroup,
  RadioButton,
  Tag,
  Checkbox,
  NumberInput,
  DatePicker,
  DatePickerInput,
} from "@carbon/react";
import { Checkmark, Edit, Add, Renew } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
} from "../../../utils/Utils";
import SampleGrid from "../../workflow/SampleGrid";
import "../../workflow/NotebookWorkflow.css";

/**
 * TBCultureTrackingPage - Page 5 of the TB workflow.
 * Handles weekly culture inoculation and monitoring for TB samples.
 *
 * Culture Methods:
 * - LJ (Lowenstein-Jensen solid culture)
 * - MGIT (Mycobacteria Growth Indicator Tube liquid culture)
 * - BOTH (LJ and MGIT)
 *
 * Growth Observations (per week):
 * - NO_GROWTH: No growth detected
 * - GROWTH_DETECTED: Growth detected (positive)
 * - CONTAMINATED: Culture contaminated
 *
 * Monitoring continues for up to 8 weeks until:
 * - Growth is detected (positive result)
 * - Week 8 reached with no growth (final negative)
 * - Culture is contaminated
 */
function TBCultureTrackingPage({
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

  // Weekly reading modal state
  const [readingModalOpen, setReadingModalOpen] = useState(false);
  const [isSaving, setIsSaving] = useState(false);

  // Culture reading form state
  const [readingData, setReadingData] = useState({
    weekNumber: 1,
    readingDate: new Date().toISOString().split("T")[0],
    cultureMethod: "LJ",
    growthObservation: "",
    ljObservation: "",
    mgitObservation: "",
    notes: "",
    isDelayed: false,
  });

  // Culture method options
  const cultureMethodOptions = [
    { value: "LJ", label: "LJ (Lowenstein-Jensen)" },
    { value: "MGIT", label: "MGIT (Liquid culture)" },
    { value: "BOTH", label: "Both LJ and MGIT" },
  ];

  // Growth observation options
  const growthOptions = [
    { value: "NO_GROWTH", label: "No Growth" },
    { value: "GROWTH_DETECTED", label: "Growth Detected" },
    { value: "CONTAMINATED", label: "Contaminated" },
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
              // Culture tracking fields
              cultureMethod: sample.data?.cultureMethod,
              currentWeek: sample.data?.currentWeek || 0,
              lastReading: sample.data?.lastReading,
              lastGrowthObservation: sample.data?.lastGrowthObservation,
              inoculationDate: sample.data?.inoculationDate,
              // Weekly readings history
              weeklyReadings: sample.data?.weeklyReadings || [],
              // Final result
              cultureResult: sample.data?.cultureResult,
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

  // Reset reading form
  const resetReadingData = () => {
    setReadingData({
      weekNumber: 1,
      readingDate: new Date().toISOString().split("T")[0],
      cultureMethod: "LJ",
      growthObservation: "",
      ljObservation: "",
      mgitObservation: "",
      notes: "",
      isDelayed: false,
    });
  };

  // Calculate stats
  const stats = useMemo(() => {
    const inoculated = samples.filter((s) => s.cultureMethod).length;
    const growthDetected = samples.filter(
      (s) =>
        s.lastGrowthObservation === "GROWTH_DETECTED" ||
        s.cultureResult === "GROWTH_DETECTED",
    ).length;
    const noGrowth = samples.filter(
      (s) => s.currentWeek >= 8 && s.lastGrowthObservation === "NO_GROWTH",
    ).length;
    const contaminated = samples.filter(
      (s) => s.lastGrowthObservation === "CONTAMINATED",
    ).length;
    const inProgress = samples.filter(
      (s) =>
        s.cultureMethod &&
        s.currentWeek < 8 &&
        s.lastGrowthObservation === "NO_GROWTH",
    ).length;
    const pending = samples.filter((s) => !s.cultureMethod).length;

    return {
      total: samples.length,
      inoculated,
      growthDetected,
      noGrowth,
      contaminated,
      inProgress,
      pending,
    };
  }, [samples]);

  // Handle opening reading modal
  const handleOpenReadingModal = useCallback(() => {
    if (selectedSampleIds.length === 0) {
      setError(
        intl.formatMessage({
          id: "notebook.page.tb.culture.error.noSelection",
          defaultMessage: "Please select at least one sample.",
        }),
      );
      return;
    }

    // Pre-populate week number based on selected samples' current week
    const selectedSamples = samples.filter((s) =>
      selectedSampleIds.includes(s.id),
    );
    const maxCurrentWeek = Math.max(
      ...selectedSamples.map((s) => s.currentWeek || 0),
    );
    const nextWeek = Math.min(maxCurrentWeek + 1, 8);

    setReadingData((prev) => ({
      ...prev,
      weekNumber: nextWeek,
    }));

    setReadingModalOpen(true);
  }, [selectedSampleIds, samples, intl]);

  // Handle saving weekly reading
  const handleSaveReading = useCallback(() => {
    if (!readingData.growthObservation) {
      setError(
        intl.formatMessage({
          id: "notebook.page.tb.culture.error.noObservation",
          defaultMessage: "Please select a growth observation.",
        }),
      );
      return;
    }

    // If using BOTH method, require both observations
    if (readingData.cultureMethod === "BOTH") {
      if (!readingData.ljObservation || !readingData.mgitObservation) {
        setError(
          intl.formatMessage({
            id: "notebook.page.tb.culture.error.bothRequired",
            defaultMessage: "Please select observations for both LJ and MGIT.",
          }),
        );
        return;
      }
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

    // Build data object with reading info
    const data = {
      cultureMethod: readingData.cultureMethod,
      currentWeek: readingData.weekNumber,
      lastReading: readingData.readingDate,
      lastGrowthObservation: readingData.growthObservation,
      notes: readingData.notes,
      isDelayed: readingData.isDelayed,
    };

    // Add individual observations for BOTH method
    if (readingData.cultureMethod === "BOTH") {
      data.ljObservation = readingData.ljObservation;
      data.mgitObservation = readingData.mgitObservation;
    }

    // Set culture result if definitive
    if (readingData.growthObservation === "GROWTH_DETECTED") {
      data.cultureResult = "GROWTH_DETECTED";
    } else if (readingData.growthObservation === "CONTAMINATED") {
      data.cultureResult = "CONTAMINATED";
    } else if (
      readingData.weekNumber === 8 &&
      readingData.growthObservation === "NO_GROWTH"
    ) {
      data.cultureResult = "FINAL_NEGATIVE";
    }

    postToOpenElisServer(
      `/rest/notebook/bulk/page/${pageData.id}/samples/apply`,
      JSON.stringify({
        sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
        data,
      }),
      (status) => {
        if (status === 200) {
          // Determine new status based on result
          let newStatus = "IN_PROGRESS";
          if (data.cultureResult) {
            newStatus = "COMPLETED";
          }

          postToOpenElisServer(
            `/rest/notebook/bulk/page/${pageData.id}/samples/status`,
            JSON.stringify({
              sampleIds: selectedSampleIds.map((id) => parseInt(id, 10)),
              status: newStatus,
            }),
            () => {
              setIsSaving(false);
              setSuccessMessage(
                intl.formatMessage(
                  {
                    id: "notebook.page.tb.culture.readingSaved",
                    defaultMessage:
                      "Week {week} reading saved for {count} sample(s).",
                  },
                  {
                    week: readingData.weekNumber,
                    count: selectedSampleIds.length,
                  },
                ),
              );
              setReadingModalOpen(false);
              setSelectedSampleIds([]);
              resetReadingData();
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
              id: "notebook.page.tb.culture.error.save",
              defaultMessage: "Failed to save reading. Please try again.",
            }),
          );
        }
      },
    );
  }, [
    readingData,
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

    // Check if selected samples have a definitive result
    const selectedSamples = samples.filter((s) =>
      selectedSampleIds.includes(s.id),
    );
    const withoutResult = selectedSamples.filter((s) => !s.cultureResult);
    if (withoutResult.length > 0) {
      setError(
        intl.formatMessage(
          {
            id: "notebook.page.tb.culture.error.noResult",
            defaultMessage:
              "{count} sample(s) do not have a final culture result yet.",
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
                id: "notebook.page.tb.culture.markedComplete",
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

  // Get growth observation tag
  const getGrowthTag = (observation) => {
    if (!observation) return <Tag type="gray">Not read</Tag>;
    if (observation === "NO_GROWTH") return <Tag type="blue">No Growth</Tag>;
    if (observation === "GROWTH_DETECTED")
      return <Tag type="green">Growth Detected</Tag>;
    if (observation === "CONTAMINATED")
      return <Tag type="red">Contaminated</Tag>;
    return <Tag type="gray">{observation}</Tag>;
  };

  // Get culture result tag
  const getCultureResultTag = (result) => {
    if (!result) return null;
    if (result === "GROWTH_DETECTED") return <Tag type="green">Positive</Tag>;
    if (result === "FINAL_NEGATIVE")
      return <Tag type="blue">Negative (8 wks)</Tag>;
    if (result === "CONTAMINATED") return <Tag type="red">Contaminated</Tag>;
    return <Tag type="gray">{result}</Tag>;
  };

  // Get culture method tag
  const getCultureMethodTag = (method) => {
    if (!method) return null;
    const methodLabels = {
      LJ: { type: "purple", label: "LJ" },
      MGIT: { type: "teal", label: "MGIT" },
      BOTH: { type: "cyan", label: "LJ+MGIT" },
    };
    const config = methodLabels[method] || { type: "gray", label: method };
    return <Tag type={config.type}>{config.label}</Tag>;
  };

  // Get week progress display
  const getWeekProgress = (currentWeek) => {
    if (!currentWeek) return "-";
    return `Week ${currentWeek}/8`;
  };

  return (
    <div className="tb-culture-tracking-page">
      <div className="page-section-header">
        <h4>
          <FormattedMessage
            id="notebook.page.tb.culture.title"
            defaultMessage="Culture Inoculation & Weekly Monitoring"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.tb.culture.description"
            defaultMessage="Track weekly culture readings for TB samples. Monitor growth observations on LJ and/or MGIT media for up to 8 weeks."
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
                  id="notebook.page.tb.culture.growthDetected"
                  defaultMessage="Growth Detected"
                />
              </span>
              <span className="progress-value">{stats.growthDetected}</span>
            </Tile>
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.tb.culture.noGrowth"
                  defaultMessage="Final Negative"
                />
              </span>
              <span className="progress-value">{stats.noGrowth}</span>
            </Tile>
            <Tile className="progress-tile error">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.tb.culture.contaminated"
                  defaultMessage="Contaminated"
                />
              </span>
              <span className="progress-value">{stats.contaminated}</span>
            </Tile>
            <Tile className="progress-tile pending">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.tb.culture.inProgress"
                  defaultMessage="In Progress"
                />
              </span>
              <span className="progress-value">{stats.inProgress}</span>
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
          onClick={handleOpenReadingModal}
          disabled={selectedSampleIds.length === 0}
        >
          <FormattedMessage
            id="notebook.page.tb.culture.addReading"
            defaultMessage="Add Weekly Reading ({count})"
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
              id="notebook.page.tb.culture.markComplete"
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

      {/* In Progress Samples Table */}
      <div className="sample-table-section">
        <div className="table-section-header">
          <h5>
            <FormattedMessage
              id="notebook.page.tb.culture.inProgressTable.title"
              defaultMessage="Cultures In Progress"
            />
            <Tag type="blue" className="count-tag">
              {
                samples.filter(
                  (s) => s.status !== "COMPLETED" && s.status !== "SKIPPED",
                ).length
              }
            </Tag>
          </h5>
          <p className="table-section-description">
            <FormattedMessage
              id="notebook.page.tb.culture.inProgressTable.description"
              defaultMessage="Select samples and add weekly readings to track culture progress."
            />
          </p>
        </div>
        <div className="sample-grid-container">
          <SampleGrid
            gridId="culture-in-progress"
            samples={samples.filter(
              (s) => s.status !== "COMPLETED" && s.status !== "SKIPPED",
            )}
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
                key: "cultureMethod",
                header: intl.formatMessage({
                  id: "notebook.grid.cultureMethod",
                  defaultMessage: "Method",
                }),
                render: (value) => getCultureMethodTag(value),
              },
              {
                key: "currentWeek",
                header: intl.formatMessage({
                  id: "notebook.grid.week",
                  defaultMessage: "Week",
                }),
                render: (value) => getWeekProgress(value),
              },
              {
                key: "lastGrowthObservation",
                header: intl.formatMessage({
                  id: "notebook.grid.lastReading",
                  defaultMessage: "Last Reading",
                }),
                render: (value) => getGrowthTag(value),
              },
              {
                key: "status",
                header: intl.formatMessage({
                  id: "notebook.grid.status",
                  defaultMessage: "Status",
                }),
              },
            ]}
          />
        </div>
        {!loading &&
          samples.filter(
            (s) => s.status !== "COMPLETED" && s.status !== "SKIPPED",
          ).length === 0 && (
            <div className="empty-table-state">
              <p>
                <FormattedMessage
                  id="notebook.page.tb.culture.inProgressTable.empty"
                  defaultMessage="No cultures in progress."
                />
              </p>
            </div>
          )}
      </div>

      {/* Completed Cultures Table */}
      <div className="sample-table-section">
        <div className="table-section-header">
          <h5>
            <FormattedMessage
              id="notebook.page.tb.culture.completedTable.title"
              defaultMessage="Completed Cultures"
            />
            <Tag type="green" className="count-tag">
              {
                samples.filter(
                  (s) => s.status === "COMPLETED" || s.status === "SKIPPED",
                ).length
              }
            </Tag>
          </h5>
          <p className="table-section-description">
            <FormattedMessage
              id="notebook.page.tb.culture.completedTable.description"
              defaultMessage="Cultures with final results (positive, negative, or contaminated)."
            />
          </p>
        </div>
        <div className="sample-grid-container">
          <SampleGrid
            gridId="culture-completed"
            samples={samples.filter(
              (s) => s.status === "COMPLETED" || s.status === "SKIPPED",
            )}
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
                key: "cultureMethod",
                header: intl.formatMessage({
                  id: "notebook.grid.cultureMethod",
                  defaultMessage: "Method",
                }),
                render: (value) => getCultureMethodTag(value),
              },
              {
                key: "currentWeek",
                header: intl.formatMessage({
                  id: "notebook.grid.finalWeek",
                  defaultMessage: "Final Week",
                }),
                render: (value) => `Week ${value || "-"}`,
              },
              {
                key: "cultureResult",
                header: intl.formatMessage({
                  id: "notebook.grid.cultureResult",
                  defaultMessage: "Culture Result",
                }),
                render: (value) => getCultureResultTag(value),
              },
              {
                key: "status",
                header: intl.formatMessage({
                  id: "notebook.grid.status",
                  defaultMessage: "Status",
                }),
              },
            ]}
          />
        </div>
        {!loading &&
          samples.filter(
            (s) => s.status === "COMPLETED" || s.status === "SKIPPED",
          ).length === 0 && (
            <div className="empty-table-state">
              <p>
                <FormattedMessage
                  id="notebook.page.tb.culture.completedTable.empty"
                  defaultMessage="No completed cultures yet."
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
              id="notebook.page.tb.culture.empty"
              defaultMessage="No samples available for culture tracking. Complete previous workflow steps first."
            />
          </p>
        </div>
      )}

      {/* Weekly Reading Modal */}
      <Modal
        open={readingModalOpen}
        onRequestClose={() => setReadingModalOpen(false)}
        modalHeading={intl.formatMessage({
          id: "notebook.tb.culture.modal.title",
          defaultMessage: "Add Weekly Culture Reading",
        })}
        primaryButtonText={
          isSaving
            ? intl.formatMessage({
                id: "label.saving",
                defaultMessage: "Saving...",
              })
            : intl.formatMessage({
                id: "label.save",
                defaultMessage: "Save Reading",
              })
        }
        secondaryButtonText={intl.formatMessage({
          id: "label.cancel",
          defaultMessage: "Cancel",
        })}
        onRequestSubmit={handleSaveReading}
        onSecondarySubmit={() => setReadingModalOpen(false)}
        size="md"
        primaryButtonDisabled={isSaving || !readingData.growthObservation}
      >
        <div className="culture-reading-modal">
          <p className="modal-description">
            <FormattedMessage
              id="notebook.tb.culture.modal.description"
              defaultMessage="Record weekly culture reading for {count} selected sample(s)."
              values={{ count: selectedSampleIds.length }}
            />
          </p>

          <Grid fullWidth>
            {/* Week Number */}
            <Column lg={8} md={4} sm={4}>
              <NumberInput
                id="weekNumber"
                label={intl.formatMessage({
                  id: "notebook.tb.culture.weekNumber",
                  defaultMessage: "Week Number",
                })}
                value={readingData.weekNumber}
                onChange={(e, { value }) =>
                  setReadingData((prev) => ({
                    ...prev,
                    weekNumber: value,
                  }))
                }
                min={1}
                max={8}
                step={1}
              />
            </Column>

            {/* Reading Date */}
            <Column lg={8} md={4} sm={4}>
              <DatePicker
                datePickerType="single"
                onChange={([date]) =>
                  setReadingData((prev) => ({
                    ...prev,
                    readingDate: date?.toISOString().split("T")[0] || "",
                  }))
                }
              >
                <DatePickerInput
                  id="readingDate"
                  labelText={intl.formatMessage({
                    id: "notebook.tb.culture.readingDate",
                    defaultMessage: "Reading Date",
                  })}
                  placeholder="mm/dd/yyyy"
                />
              </DatePicker>
            </Column>

            {/* Culture Method */}
            <Column lg={8} md={4} sm={4}>
              <Select
                id="cultureMethod"
                labelText={intl.formatMessage({
                  id: "notebook.tb.culture.method",
                  defaultMessage: "Culture Method",
                })}
                value={readingData.cultureMethod}
                onChange={(e) =>
                  setReadingData((prev) => ({
                    ...prev,
                    cultureMethod: e.target.value,
                  }))
                }
              >
                {cultureMethodOptions.map((option) => (
                  <SelectItem
                    key={option.value}
                    value={option.value}
                    text={option.label}
                  />
                ))}
              </Select>
            </Column>

            {/* Growth Observation */}
            <Column lg={8} md={4} sm={4}>
              <Select
                id="growthObservation"
                labelText={intl.formatMessage({
                  id: "notebook.tb.culture.growthObservation",
                  defaultMessage: "Growth Observation",
                })}
                value={readingData.growthObservation}
                onChange={(e) =>
                  setReadingData((prev) => ({
                    ...prev,
                    growthObservation: e.target.value,
                  }))
                }
              >
                <SelectItem value="" text="Select observation..." />
                {growthOptions.map((option) => (
                  <SelectItem
                    key={option.value}
                    value={option.value}
                    text={option.label}
                  />
                ))}
              </Select>
            </Column>

            {/* LJ and MGIT specific observations (only for BOTH method) */}
            {readingData.cultureMethod === "BOTH" && (
              <>
                <Column lg={8} md={4} sm={4}>
                  <Select
                    id="ljObservation"
                    labelText={intl.formatMessage({
                      id: "notebook.tb.culture.ljObservation",
                      defaultMessage: "LJ Observation",
                    })}
                    value={readingData.ljObservation}
                    onChange={(e) =>
                      setReadingData((prev) => ({
                        ...prev,
                        ljObservation: e.target.value,
                      }))
                    }
                  >
                    <SelectItem value="" text="Select..." />
                    {growthOptions.map((option) => (
                      <SelectItem
                        key={option.value}
                        value={option.value}
                        text={option.label}
                      />
                    ))}
                  </Select>
                </Column>
                <Column lg={8} md={4} sm={4}>
                  <Select
                    id="mgitObservation"
                    labelText={intl.formatMessage({
                      id: "notebook.tb.culture.mgitObservation",
                      defaultMessage: "MGIT Observation",
                    })}
                    value={readingData.mgitObservation}
                    onChange={(e) =>
                      setReadingData((prev) => ({
                        ...prev,
                        mgitObservation: e.target.value,
                      }))
                    }
                  >
                    <SelectItem value="" text="Select..." />
                    {growthOptions.map((option) => (
                      <SelectItem
                        key={option.value}
                        value={option.value}
                        text={option.label}
                      />
                    ))}
                  </Select>
                </Column>
              </>
            )}

            {/* Delayed checkbox */}
            <Column lg={16} md={8} sm={4}>
              <Checkbox
                id="isDelayed"
                labelText={intl.formatMessage({
                  id: "notebook.tb.culture.isDelayed",
                  defaultMessage: "Reading was delayed",
                })}
                checked={readingData.isDelayed}
                onChange={(_, { checked }) =>
                  setReadingData((prev) => ({
                    ...prev,
                    isDelayed: checked,
                  }))
                }
              />
            </Column>

            {/* Notes */}
            <Column lg={16} md={8} sm={4}>
              <TextArea
                id="notes"
                labelText={intl.formatMessage({
                  id: "notebook.tb.culture.notes",
                  defaultMessage: "Notes",
                })}
                value={readingData.notes}
                onChange={(e) =>
                  setReadingData((prev) => ({
                    ...prev,
                    notes: e.target.value,
                  }))
                }
                placeholder={intl.formatMessage({
                  id: "notebook.tb.culture.notes.placeholder",
                  defaultMessage:
                    "Add any observations or notes about this reading...",
                })}
                rows={3}
              />
            </Column>
          </Grid>

          {/* Week 8 notice */}
          {readingData.weekNumber === 8 &&
            readingData.growthObservation === "NO_GROWTH" && (
              <InlineNotification
                kind="info"
                title={intl.formatMessage({
                  id: "notebook.tb.culture.week8Notice.title",
                  defaultMessage: "Final Week",
                })}
                subtitle={intl.formatMessage({
                  id: "notebook.tb.culture.week8Notice.subtitle",
                  defaultMessage:
                    "This is the final reading week. No growth at Week 8 will be recorded as a final negative result.",
                })}
                hideCloseButton
                lowContrast
                style={{ marginTop: "1rem" }}
              />
            )}

          {/* Growth detected notice */}
          {readingData.growthObservation === "GROWTH_DETECTED" && (
            <InlineNotification
              kind="success"
              title={intl.formatMessage({
                id: "notebook.tb.culture.growthNotice.title",
                defaultMessage: "Growth Detected",
              })}
              subtitle={intl.formatMessage({
                id: "notebook.tb.culture.growthNotice.subtitle",
                defaultMessage:
                  "Culture will be marked as positive. Proceed to species identification.",
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

export default TBCultureTrackingPage;
