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
  Tag,
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableExpandHeader,
  TableExpandRow,
  TableExpandedRow,
  TableContainer,
  TableToolbar,
  TableToolbarContent,
  TableToolbarSearch,
} from "@carbon/react";
import {
  Renew,
  Temperature,
  CheckmarkFilled,
  Warning,
} from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
  putToOpenElisServer,
} from "../../../utils/Utils";
import WeeklyReadingTable from "./components/WeeklyReadingTable";
import RecordReadingModal from "./components/RecordReadingModal";
import "../../workflow/NotebookWorkflow.css";

/**
 * TBIncubationMonitoringPage - Page 5 of the TB workflow.
 *
 * Monitors incubating cultures with weekly readings for up to 8 weeks.
 * Uses Carbon ExpandableRow DataTable pattern.
 *
 * Features:
 * - Expandable rows showing weekly reading history
 * - Record weekly readings via modal
 * - Auto-determination prompts (Positive/Negative)
 * - Summary tiles for incubation status
 */
function TBIncubationMonitoringPage({ entryId, pageData, onProgressUpdate }) {
  const intl = useIntl();
  const componentMounted = useRef(false);

  // State
  const [incubatingSamples, setIncubatingSamples] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);
  const [searchTerm, setSearchTerm] = useState("");

  // Summary statistics
  const [summary, setSummary] = useState({
    totalIncubating: 0,
    week1to4: 0,
    week5to8: 0,
    positive: 0,
    negative: 0,
  });

  // Reading modal state
  const [readingModalOpen, setReadingModalOpen] = useState(false);
  const [selectedSample, setSelectedSample] = useState(null);
  const [sampleReadings, setSampleReadings] = useState([]);

  // Refresh trigger for expanded rows
  const [refreshTrigger, setRefreshTrigger] = useState(0);

  // Compute summary from grouped samples (consistent with displayed data)
  const computeSummary = useCallback((samples) => {
    const incubating = samples.filter((s) => !s.cultureResult);
    const positive = samples.filter((s) => s.cultureResult === "POSITIVE");
    const negative = samples.filter((s) => s.cultureResult === "NEGATIVE");

    // Week ranges based on current week number
    const week1to4 = incubating.filter(
      (s) => s.weekNumber >= 1 && s.weekNumber <= 4,
    );
    const week5to8 = incubating.filter(
      (s) => s.weekNumber >= 5 && s.weekNumber <= 8,
    );

    setSummary({
      totalIncubating: incubating.length,
      week1to4: week1to4.length,
      week5to8: week5to8.length,
      positive: positive.length,
      negative: negative.length,
    });
  }, []);

  const loadIncubatingSamples = useCallback(() => {
    if (!entryId) {
      setIncubatingSamples([]);
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);

    getFromOpenElisServer(
      `/rest/tb/incubation/samples?entryId=${entryId}`,
      (response) => {
        if (componentMounted.current) {
          if (response && Array.isArray(response)) {
            // Group readings by sampleItemId to create one row per sample
            const sampleMap = new Map();

            response.forEach((reading) => {
              const sampleItemId = String(reading.sampleItemId);

              if (!sampleMap.has(sampleItemId)) {
                // First reading for this sample - create the sample entry
                sampleMap.set(sampleItemId, {
                  id: sampleItemId, // Use sampleItemId as unique row ID
                  sampleItemId: sampleItemId,
                  accessionNumber:
                    reading.sampleItem?.sample?.accessionNumber ||
                    `Sample-${reading.id}`,
                  sampleType:
                    reading.sampleItem?.typeOfSample?.description || "Sputum",
                  cultureMethod: reading.cultureMethod || "LJ",
                  inoculationDate: reading.inoculationDate,
                  weekNumber: reading.weekNumber || 1,
                  growthObservation: reading.growthObservation,
                  cultureResult: reading.cultureResult || null,
                  positiveWeek: reading.positiveWeek,
                  mediaBatch: reading.mediaBatch,
                  readings: [reading],
                });
              } else {
                // Additional reading for existing sample - add to readings array
                const sample = sampleMap.get(sampleItemId);
                sample.readings.push(reading);

                // Update to reflect the latest (highest weekNumber) reading's status
                if (reading.weekNumber > sample.weekNumber) {
                  sample.weekNumber = reading.weekNumber;
                  sample.growthObservation = reading.growthObservation;
                }

                // IMPORTANT: If ANY reading has a cultureResult, the sample is finalized
                if (reading.cultureResult && !sample.cultureResult) {
                  sample.cultureResult = reading.cultureResult;
                  sample.positiveWeek = reading.positiveWeek;
                }
              }
            });

            // Get all samples (including finalized ones for summary stats)
            const allSamples = Array.from(sampleMap.values());

            // Filter to only show incubating samples (those without cultureResult)
            const samples = allSamples.filter((s) => !s.cultureResult);
            setIncubatingSamples(samples);
            // Use allSamples for summary so positive/negative counts are correct
            computeSummary(allSamples);
          } else {
            setIncubatingSamples([]);
            computeSummary([]);
          }
          setLoading(false);
        }
      },
    );
  }, [entryId, computeSummary]);

  // Load data on mount
  useEffect(() => {
    componentMounted.current = true;
    loadIncubatingSamples();

    return () => {
      componentMounted.current = false;
    };
  }, [entryId, loadIncubatingSamples]);

  // Load readings for a specific sample
  const loadSampleReadings = useCallback((sampleItemId, callback) => {
    getFromOpenElisServer(
      `/rest/tb/incubation/samples/${sampleItemId}/readings`,
      (response) => {
        if (response && Array.isArray(response)) {
          callback(response);
        } else {
          callback([]);
        }
      },
    );
  }, []);

  // Calculate current week based on inoculation date
  const calculateCurrentWeek = useCallback((inoculationDate) => {
    if (!inoculationDate) return 1;
    const inocDate = new Date(inoculationDate);
    const today = new Date();
    const diffDays = Math.floor((today - inocDate) / (1000 * 60 * 60 * 24));
    const week = Math.floor(diffDays / 7) + 1;
    return Math.min(week, 8);
  }, []);

  // Open reading modal for a sample
  const handleOpenReadingModal = useCallback(
    (sample) => {
      setSelectedSample(sample);
      loadSampleReadings(sample.sampleItemId, (readings) => {
        setSampleReadings(readings);
        setReadingModalOpen(true);
      });
    },
    [loadSampleReadings],
  );

  // Save a reading
  const handleSaveReading = useCallback(
    (readingData) => {
      postToOpenElisServer(
        "/rest/tb/incubation/reading",
        JSON.stringify(readingData),
        (response) => {
          if (componentMounted.current) {
            if (response && !response.error) {
              setSuccess(
                intl.formatMessage({
                  id: "notebook.tb.incubation.readingSaved",
                  defaultMessage: "Reading recorded successfully.",
                }),
              );
              setReadingModalOpen(false);
              setSelectedSample(null);
              setSampleReadings([]);
              loadIncubatingSamples();
              setRefreshTrigger((prev) => prev + 1);
              if (onProgressUpdate) onProgressUpdate();

              // Check for auto-determination prompts
              if (response.autoDetermination) {
                if (response.autoDetermination === "POSITIVE") {
                  setSuccess(response.prompt);
                } else if (response.autoDetermination === "NEGATIVE") {
                  setSuccess(response.prompt);
                }
              }
            } else {
              setError(response?.error || "Failed to save reading.");
            }
          }
        },
      );
    },
    [intl, loadIncubatingSamples, onProgressUpdate],
  );

  // Mark culture as positive
  const handleMarkPositive = useCallback(
    (sample) => {
      const currentWeek = calculateCurrentWeek(sample.inoculationDate);
      // Use the first reading's ID since sample.id is now sampleItemId after grouping
      const cultureReadingId = sample.readings?.[0]?.id || sample.id;

      putToOpenElisServer(
        `/rest/tb/incubation/result/${cultureReadingId}/positive`,
        JSON.stringify({ positiveWeek: currentWeek }),
        (response) => {
          if (componentMounted.current) {
            if (response && !response.error) {
              setSuccess(
                intl.formatMessage({
                  id: "notebook.tb.incubation.markedPositive",
                  defaultMessage:
                    "Culture marked as POSITIVE. Sample is ready for further testing.",
                }),
              );
              loadIncubatingSamples();
              setRefreshTrigger((prev) => prev + 1);
              if (onProgressUpdate) onProgressUpdate();
            } else {
              setError(response?.error || "Failed to mark as positive.");
            }
          }
        },
      );
    },
    [calculateCurrentWeek, intl, loadIncubatingSamples, onProgressUpdate],
  );

  // Mark culture as negative
  const handleMarkNegative = useCallback(
    (sample) => {
      // Use the first reading's ID since sample.id is now sampleItemId after grouping
      const cultureReadingId = sample.readings?.[0]?.id || sample.id;

      putToOpenElisServer(
        `/rest/tb/incubation/result/${cultureReadingId}/negative`,
        JSON.stringify({}),
        (response) => {
          if (componentMounted.current) {
            if (response && !response.error) {
              setSuccess(
                intl.formatMessage({
                  id: "notebook.tb.incubation.markedNegative",
                  defaultMessage:
                    "Culture marked as NEGATIVE (no growth after 8 weeks).",
                }),
              );
              loadIncubatingSamples();
              setRefreshTrigger((prev) => prev + 1);
              if (onProgressUpdate) onProgressUpdate();
            } else {
              setError(response?.error || "Failed to mark as negative.");
            }
          }
        },
      );
    },
    [intl, loadIncubatingSamples, onProgressUpdate],
  );

  // Filter samples by search term
  const filteredSamples = useMemo(() => {
    if (!searchTerm) return incubatingSamples;
    const term = searchTerm.toLowerCase();
    return incubatingSamples.filter(
      (s) =>
        s.accessionNumber?.toLowerCase().includes(term) ||
        s.cultureMethod?.toLowerCase().includes(term),
    );
  }, [incubatingSamples, searchTerm]);

  // DataTable headers
  const headers = [
    {
      key: "accessionNumber",
      header: intl.formatMessage({
        id: "notebook.tb.incubation.sampleId",
        defaultMessage: "Sample ID",
      }),
    },
    {
      key: "cultureMethod",
      header: intl.formatMessage({
        id: "notebook.tb.incubation.method",
        defaultMessage: "Method",
      }),
    },
    {
      key: "weekNumber",
      header: intl.formatMessage({
        id: "notebook.tb.incubation.week",
        defaultMessage: "Week",
      }),
    },
    {
      key: "status",
      header: intl.formatMessage({
        id: "notebook.tb.incubation.status",
        defaultMessage: "Status",
      }),
    },
    {
      key: "inoculationDate",
      header: intl.formatMessage({
        id: "notebook.tb.incubation.inocDate",
        defaultMessage: "Inoculation Date",
      }),
    },
  ];

  // Render status cell
  const renderStatus = (sample) => {
    if (sample.cultureResult) {
      const config = {
        POSITIVE: { type: "red", text: "Positive" },
        NEGATIVE: { type: "green", text: "Negative" },
        CONTAMINATED: { type: "orange", text: "Contaminated" },
      };
      const cfg = config[sample.cultureResult] || {
        type: "gray",
        text: sample.cultureResult,
      };
      return <Tag type={cfg.type}>{cfg.text}</Tag>;
    }

    if (sample.growthObservation === "GROWTH_DETECTED") {
      return (
        <Tag type="red">
          <Warning size={12} style={{ marginRight: "4px" }} />
          Growth Detected
        </Tag>
      );
    }

    // const currentWeek = calculateCurrentWeek(sample.inoculationDate);
    // if (currentWeek <= 4) {
    //   return <Tag type="teal">Incubating (Week 1-4)</Tag>;
    // }
    return <Tag type="teal">Incubating (Week 1-8)</Tag>;
  };

  // Render expanded row content
  const renderExpandedContent = (row) => {
    const sample = incubatingSamples.find((s) => s.id === row.id);
    if (!sample) return null;

    const currentWeek = calculateCurrentWeek(sample.inoculationDate);

    return (
      <ExpandedRowContent
        sample={sample}
        currentWeek={currentWeek}
        onAddReading={() => handleOpenReadingModal(sample)}
        onMarkPositive={() => handleMarkPositive(sample)}
        onMarkNegative={() => handleMarkNegative(sample)}
        loadSampleReadings={loadSampleReadings}
        refreshTrigger={refreshTrigger}
      />
    );
  };

  return (
    <div className="tb-incubation-monitoring-page">
      {/* Page Header */}
      <div className="page-section-header">
        <h4>
          <Temperature
            size={20}
            style={{ marginRight: "8px", verticalAlign: "middle" }}
          />
          <FormattedMessage
            id="notebook.page.tb.incubationMonitoring.title"
            defaultMessage="TB Incubation Monitoring"
          />
        </h4>
        <p className="page-description">
          <FormattedMessage
            id="notebook.page.tb.incubationMonitoring.description"
            defaultMessage="Track weekly culture readings for inoculated samples. Expand rows to view reading history."
          />
        </p>
      </div>

      {/* Summary Tiles */}
      <Grid fullWidth className="progress-section">
        <Column lg={16} md={8} sm={4}>
          <div className="progress-tiles">
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.tb.incubation.totalIncubating"
                  defaultMessage="Total Incubating"
                />
              </span>
              <span className="progress-value">{summary.totalIncubating}</span>
            </Tile>
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.tb.incubation.week1to4"
                  defaultMessage="Week 1-4"
                />
              </span>
              <span className="progress-value">{summary.week1to4}</span>
            </Tile>
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.tb.incubation.week5to8"
                  defaultMessage="Week 5-8"
                />
              </span>
              <span className="progress-value">{summary.week5to8}</span>
            </Tile>
            <Tile className="progress-tile rejected">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.tb.incubation.positive"
                  defaultMessage="Positive"
                />
              </span>
              <span className="progress-value">{summary.positive}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.tb.incubation.negative"
                  defaultMessage="Negative"
                />
              </span>
              <span className="progress-value">{summary.negative}</span>
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

      {/* DataTable with Expandable Rows */}
      <DataTable
        rows={filteredSamples.map((sample) => ({
          id: sample.id,
          accessionNumber: sample.accessionNumber,
          cultureMethod: sample.cultureMethod,
          weekNumber: calculateCurrentWeek(sample.inoculationDate),
          status: sample,
          inoculationDate: sample.inoculationDate
            ? new Date(sample.inoculationDate).toLocaleDateString()
            : "-",
        }))}
        headers={headers}
      >
        {({
          rows,
          headers,
          getHeaderProps,
          getRowProps,
          getTableProps,
          getToolbarProps,
          onInputChange,
          getExpandHeaderProps,
        }) => (
          <TableContainer>
            <TableToolbar {...getToolbarProps()}>
              <TableToolbarContent>
                <TableToolbarSearch
                  onChange={(e) => {
                    onInputChange(e);
                    setSearchTerm(e.target.value);
                  }}
                  placeholder={intl.formatMessage({
                    id: "notebook.tb.incubation.search",
                    defaultMessage: "Search samples...",
                  })}
                />
                <Button
                  kind="ghost"
                  size="sm"
                  renderIcon={Renew}
                  onClick={() => {
                    loadIncubatingSamples();
                    setRefreshTrigger((prev) => prev + 1);
                  }}
                >
                  <FormattedMessage
                    id="notebook.tb.incubation.refresh"
                    defaultMessage="Refresh"
                  />
                </Button>
              </TableToolbarContent>
            </TableToolbar>
            <Table {...getTableProps()}>
              <TableHead>
                <TableRow>
                  <TableExpandHeader
                    {...getExpandHeaderProps()}
                    aria-label="expand row"
                  />
                  {headers.map((header) => (
                    <TableHeader
                      key={header.key}
                      {...getHeaderProps({ header })}
                    >
                      {header.header}
                    </TableHeader>
                  ))}
                </TableRow>
              </TableHead>
              <TableBody>
                {rows.map((row) => (
                  <React.Fragment key={row.id}>
                    <TableExpandRow {...getRowProps({ row })}>
                      {row.cells.map((cell) => (
                        <TableCell key={cell.id}>
                          {cell.info.header === "status"
                            ? renderStatus(cell.value)
                            : cell.value}
                        </TableCell>
                      ))}
                    </TableExpandRow>
                    <TableExpandedRow colSpan={headers.length + 1}>
                      {renderExpandedContent(row)}
                    </TableExpandedRow>
                  </React.Fragment>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        )}
      </DataTable>

      {/* Empty state */}
      {!loading && incubatingSamples.length === 0 && (
        <div className="empty-state" style={{ marginTop: "2rem" }}>
          <p>
            <FormattedMessage
              id="notebook.tb.incubation.empty"
              defaultMessage="No samples currently incubating. Inoculate samples from the Initial Processing page."
            />
          </p>
        </div>
      )}

      {/* Record Reading Modal */}
      <RecordReadingModal
        open={readingModalOpen}
        onClose={() => {
          setReadingModalOpen(false);
          setSelectedSample(null);
          setSampleReadings([]);
        }}
        onSave={handleSaveReading}
        sample={selectedSample}
        existingReadings={sampleReadings}
        currentWeek={
          selectedSample
            ? calculateCurrentWeek(selectedSample.inoculationDate)
            : 1
        }
      />
    </div>
  );
}

/**
 * ExpandedRowContent - Content shown when a row is expanded.
 */
function ExpandedRowContent({
  sample,
  currentWeek,
  onAddReading,
  onMarkPositive,
  onMarkNegative,
  loadSampleReadings,
  refreshTrigger,
}) {
  const [readings, setReadings] = useState([]);
  const [loadingReadings, setLoadingReadings] = useState(true);

  useEffect(() => {
    if (sample?.sampleItemId) {
      setLoadingReadings(true);
      loadSampleReadings(sample.sampleItemId, (data) => {
        setReadings(data);
        setLoadingReadings(false);
      });
    }
  }, [sample?.sampleItemId, loadSampleReadings, refreshTrigger]);

  if (loadingReadings) {
    return (
      <div style={{ padding: "1rem", color: "#525252" }}>
        <FormattedMessage
          id="notebook.tb.incubation.loadingReadings"
          defaultMessage="Loading readings..."
        />
      </div>
    );
  }

  return (
    <WeeklyReadingTable
      readings={readings}
      currentWeek={currentWeek}
      onAddReading={onAddReading}
      onMarkPositive={onMarkPositive}
      onMarkNegative={onMarkNegative}
      cultureResult={sample?.cultureResult}
    />
  );
}

export default TBIncubationMonitoringPage;
