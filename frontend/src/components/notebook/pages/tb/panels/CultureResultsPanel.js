import { useState, useMemo } from "react";
import {
  Grid,
  Column,
  Button,
  Tile,
  Tag,
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableContainer,
  TableToolbar,
  TableToolbarContent,
  TableToolbarSearch,
} from "@carbon/react";
import { Renew, Checkmark, Close, Warning } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";

/**
 * CultureResultsPanel - Read-only display of culture results from Incubation Monitoring.
 *
 * Culture Results:
 * - POSITIVE: MTB growth detected
 * - NEGATIVE: No growth after 8 weeks
 * - CONTAMINATED: Culture contaminated
 * - NTM: Non-Tuberculous Mycobacteria detected
 */
function CultureResultsPanel({
  cultureSamples = [],
  loading = false,
  onRefresh,
}) {
  const intl = useIntl();
  const [searchTerm, setSearchTerm] = useState("");

  // Calculate stats
  const stats = useMemo(() => {
    const positive = cultureSamples.filter(
      (s) => s.cultureResult === "POSITIVE",
    ).length;
    const negative = cultureSamples.filter(
      (s) => s.cultureResult === "NEGATIVE",
    ).length;
    const contaminated = cultureSamples.filter(
      (s) => s.cultureResult === "CONTAMINATED",
    ).length;
    const ntm = cultureSamples.filter((s) => s.cultureResult === "NTM").length;

    return {
      total: cultureSamples.length,
      positive,
      negative,
      contaminated,
      ntm,
    };
  }, [cultureSamples]);

  // Filter samples by search term
  const filteredSamples = useMemo(() => {
    if (!searchTerm) return cultureSamples;
    const term = searchTerm.toLowerCase();
    return cultureSamples.filter(
      (s) =>
        s.externalId?.toLowerCase().includes(term) ||
        s.cultureMethod?.toLowerCase().includes(term) ||
        s.cultureResult?.toLowerCase().includes(term),
    );
  }, [cultureSamples, searchTerm]);

  // DataTable headers
  const headers = [
    {
      key: "externalId",
      header: intl.formatMessage({
        id: "notebook.tb.culture.sampleId",
        defaultMessage: "Sample ID",
      }),
    },
    {
      key: "cultureMethod",
      header: intl.formatMessage({
        id: "notebook.tb.culture.method",
        defaultMessage: "Method",
      }),
    },
    {
      key: "cultureResult",
      header: intl.formatMessage({
        id: "notebook.tb.culture.result",
        defaultMessage: "Result",
      }),
    },
    {
      key: "positiveWeek",
      header: intl.formatMessage({
        id: "notebook.tb.culture.positiveWeek",
        defaultMessage: "Positive Week",
      }),
    },
    {
      key: "finalResultDate",
      header: intl.formatMessage({
        id: "notebook.tb.culture.resultDate",
        defaultMessage: "Result Date",
      }),
    },
  ];

  // Render result tag
  const renderResultTag = (result) => {
    const config = {
      POSITIVE: { type: "red", icon: Warning, text: "Positive" },
      NEGATIVE: { type: "green", icon: Checkmark, text: "Negative" },
      CONTAMINATED: { type: "magenta", icon: Close, text: "Contaminated" },
      NTM: { type: "purple", icon: null, text: "NTM" },
    };
    const cfg = config[result] || { type: "gray", text: result };
    const IconComponent = cfg.icon;

    return (
      <Tag type={cfg.type}>
        {IconComponent && (
          <IconComponent size={12} style={{ marginRight: "4px" }} />
        )}
        {cfg.text}
      </Tag>
    );
  };

  // Render method tag
  const renderMethodTag = (method) => {
    const config = {
      LJ: { type: "blue", text: "LJ" },
      MGIT: { type: "teal", text: "MGIT" },
      BOTH: { type: "purple", text: "LJ + MGIT" },
    };
    const cfg = config[method] || { type: "gray", text: method };
    return <Tag type={cfg.type}>{cfg.text}</Tag>;
  };

  return (
    <div className="culture-results-panel">
      {/* Section Header */}
      <div className="panel-section-header">
        <h5>
          <FormattedMessage
            id="notebook.page.tb.culture.title"
            defaultMessage="Culture Results"
          />
        </h5>
        <p className="panel-description">
          <FormattedMessage
            id="notebook.page.tb.culture.description"
            defaultMessage="View finalized culture results from Incubation Monitoring. Results are determined after weekly observation readings."
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
                  id="notebook.page.tb.culture.total"
                  defaultMessage="Total Results"
                />
              </span>
              <span className="progress-value">{stats.total}</span>
            </Tile>
            <Tile className="progress-tile rejected">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.tb.culture.positive"
                  defaultMessage="Positive"
                />
              </span>
              <span className="progress-value">{stats.positive}</span>
            </Tile>
            <Tile className="progress-tile verified">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.tb.culture.negative"
                  defaultMessage="Negative"
                />
              </span>
              <span className="progress-value">{stats.negative}</span>
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
            <Tile className="progress-tile">
              <span className="progress-label">
                <FormattedMessage
                  id="notebook.page.tb.culture.ntm"
                  defaultMessage="NTM"
                />
              </span>
              <span className="progress-value">{stats.ntm}</span>
            </Tile>
          </div>
        </Column>
      </Grid>

      {/* Info Notice */}
      <div
        style={{
          backgroundColor: "#e5f6ff",
          padding: "1rem",
          borderRadius: "4px",
          marginBottom: "1rem",
          borderLeft: "4px solid #0f62fe",
        }}
      >
        <FormattedMessage
          id="notebook.page.tb.culture.info"
          defaultMessage="Culture results are determined in the Incubation & Monitoring page based on weekly observation readings. This view displays finalized results only."
        />
      </div>

      {/* Results Table */}
      <DataTable
        rows={filteredSamples.map((sample) => ({
          id: sample.id,
          externalId: sample.externalId,
          cultureMethod: sample.cultureMethod,
          cultureResult: sample.cultureResult,
          positiveWeek: sample.positiveWeek || "-",
          finalResultDate: sample.finalResultDate
            ? new Date(sample.finalResultDate).toLocaleDateString()
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
                    id: "notebook.tb.culture.search",
                    defaultMessage: "Search results...",
                  })}
                />
                <Button
                  kind="ghost"
                  size="sm"
                  renderIcon={Renew}
                  onClick={onRefresh}
                >
                  <FormattedMessage
                    id="notebook.tb.culture.refresh"
                    defaultMessage="Refresh"
                  />
                </Button>
              </TableToolbarContent>
            </TableToolbar>
            <Table {...getTableProps()}>
              <TableHead>
                <TableRow>
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
                  <TableRow key={row.id} {...getRowProps({ row })}>
                    {row.cells.map((cell) => (
                      <TableCell key={cell.id}>
                        {cell.info.header === "cultureResult"
                          ? renderResultTag(cell.value)
                          : cell.info.header === "cultureMethod"
                            ? renderMethodTag(cell.value)
                            : cell.value}
                      </TableCell>
                    ))}
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        )}
      </DataTable>

      {/* Empty state */}
      {!loading && cultureSamples.length === 0 && (
        <div className="empty-state" style={{ marginTop: "2rem" }}>
          <p>
            <FormattedMessage
              id="notebook.tb.culture.empty"
              defaultMessage="No finalized culture results yet. Results will appear here once cultures are marked as Positive, Negative, or Contaminated in the Incubation Monitoring page."
            />
          </p>
        </div>
      )}
    </div>
  );
}

export default CultureResultsPanel;
