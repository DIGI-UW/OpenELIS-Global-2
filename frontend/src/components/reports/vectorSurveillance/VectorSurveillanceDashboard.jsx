import React, { useState, useEffect, useCallback } from "react";
import {
  Grid,
  Column,
  Section,
  Select,
  SelectItem,
  Button,
  Tile,
  Tag,
  SkeletonText,
  DataTable,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  TableContainer,
} from "@carbon/react";
import { Download } from "@carbon/react/icons";
import { LineChart, DonutChart, SimpleBarChart } from "@carbon/charts-react";
import "@carbon/charts/styles.css";
import { FormattedMessage, useIntl } from "react-intl";
import CustomDatePicker from "../../common/CustomDatePicker";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import { encodeDate } from "../../utils/Utils";
import {
  getSurveillanceIndices,
  getSurveillanceSites,
} from "./VectorSurveillanceService";
import { generateVectorSurveillancePDF } from "./vectorPdfGenerator";

const chartHeight = "300px";

/** True when the payload carries no figures in any panel (FR-012 empty state). */
const isEmptyPayload = (data) =>
  !data ||
  ((data.collectionDensity || []).length === 0 &&
    (data.speciesDistribution || []).length === 0 &&
    (data.mirBySpecies || []).length === 0 &&
    (data.pathogenPositivity || []).length === 0 &&
    !(data.qcPassRate && data.qcPassRate.analysesTotal > 0));

function VectorSurveillanceDashboard() {
  const intl = useIntl();

  const [dateFrom, setDateFrom] = useState("");
  const [dateTo, setDateTo] = useState("");
  const [siteId, setSiteId] = useState("");
  const [sites, setSites] = useState([]);
  const [appliedScope, setAppliedScope] = useState(null);
  const [indices, setIndices] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  // Load the site filter options once.
  useEffect(() => {
    getSurveillanceSites((res) => {
      setSites(Array.isArray(res) ? res : []);
    });
  }, []);

  const handleApply = useCallback(() => {
    const scope = {
      dateFrom: encodeDate(dateFrom),
      dateTo: encodeDate(dateTo),
      siteId: siteId || undefined,
      siteName: sites.find((s) => String(s.id) === String(siteId))?.name,
    };
    setAppliedScope(scope);
    setLoading(true);
    setError(null);
    setIndices(null);
    getSurveillanceIndices(scope, (res) => {
      if (res) {
        setIndices(res);
      } else {
        setError(intl.formatMessage({ id: "vectorReport.loadError" }));
      }
      setLoading(false);
    });
  }, [dateFrom, dateTo, siteId, sites, intl]);

  const handleExport = useCallback(() => {
    if (!indices || !appliedScope) return;
    generateVectorSurveillancePDF(
      indices,
      {
        dateFrom: appliedScope.dateFrom
          ? decodeURIComponent(appliedScope.dateFrom)
          : "",
        dateTo: appliedScope.dateTo
          ? decodeURIComponent(appliedScope.dateTo)
          : "",
        siteName: appliedScope.siteName,
      },
      intl.formatMessage,
    );
  }, [indices, appliedScope, intl]);

  // PageBreadCrumb translates each `label` as a message id itself, so pass the
  // raw ids (not pre-translated strings) to avoid a double formatMessage.
  const breadcrumb = [
    { label: "vectorReport.home", link: "/" },
    {
      label: "vectorReport.title",
      link: "/VectorSurveillanceReport",
    },
  ];

  // ---- Chart data shaping (mapped to @carbon/charts group/key/value) ----
  const densityData = (indices?.collectionDensity || []).map((r) => ({
    group: r.siteName || intl.formatMessage({ id: "vectorReport.filter.allSites" }),
    key: r.periodLabel,
    value: r.specimenCount,
  }));

  const speciesData = (indices?.speciesDistribution || []).map((r) => ({
    group: `${r.genus} ${r.species}`.trim(),
    value: r.specimenCount,
  }));

  const positivityData = (indices?.pathogenPositivity || []).map((r) => ({
    group: r.pathogen,
    value: Number(r.positivityPct),
  }));

  const qc = indices?.qcPassRate;

  const empty = isEmptyPayload(indices);

  // Positivity-dependent panels (MIR, pathogen positivity, sporozoite) require a
  // catalog significance classification. When the backend reports it is absent we
  // show a "not configured" notice instead of misleading zeros (keyed off the
  // flag, never off a 0% value). Defaults true when the field is missing.
  const positivityConfigured = indices?.positivityConfigured !== false;

  return (
    <div data-testid="vector-surveillance-dashboard">
      <PageBreadCrumb breadcrumbs={breadcrumb} />
      <div style={{ padding: "0 1rem" }}>
        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            marginBottom: "1rem",
          }}
        >
          <div>
            <h2>
              <FormattedMessage id="vectorReport.title" />
            </h2>
            <p style={{ color: "var(--cds-text-secondary)", fontSize: "14px" }}>
              <FormattedMessage id="vectorReport.description" />
            </p>
          </div>
          <Button
            kind="tertiary"
            renderIcon={Download}
            onClick={handleExport}
            disabled={!indices || empty}
            data-testid="vector-export-pdf"
          >
            <FormattedMessage id="vectorReport.exportPdf" />
          </Button>
        </div>

        {/* Filter bar: date range + site + Apply */}
        <Grid fullWidth style={{ marginBottom: "1rem" }}>
          <Column lg={4} md={4} sm={4}>
            <CustomDatePicker
              id="vector-date-from"
              labelText={intl.formatMessage({ id: "vectorReport.filter.from" })}
              autofillDate={true}
              value={dateFrom}
              onChange={(date) => setDateFrom(date)}
            />
          </Column>
          <Column lg={4} md={4} sm={4}>
            <CustomDatePicker
              id="vector-date-to"
              labelText={intl.formatMessage({ id: "vectorReport.filter.to" })}
              autofillDate={true}
              value={dateTo}
              onChange={(date) => setDateTo(date)}
            />
          </Column>
          <Column lg={4} md={4} sm={4}>
            <Select
              id="vector-site"
              labelText={intl.formatMessage({ id: "vectorReport.filter.site" })}
              value={siteId}
              onChange={(e) => setSiteId(e.target.value)}
            >
              <SelectItem
                value=""
                text={intl.formatMessage({ id: "vectorReport.filter.allSites" })}
              />
              {sites.map((s) => (
                <SelectItem
                  key={s.id}
                  value={String(s.id)}
                  text={s.name || s.code}
                />
              ))}
            </Select>
          </Column>
          <Column
            lg={4}
            md={4}
            sm={4}
            style={{ display: "flex", alignItems: "flex-end" }}
          >
            <Button onClick={handleApply} data-testid="vector-apply">
              <FormattedMessage id="vectorReport.filter.apply" />
            </Button>
          </Column>
        </Grid>

        {/* Freshness indicator (FR-013) */}
        {indices && (
          <div style={{ marginBottom: "1rem" }} data-testid="vector-freshness">
            <Tag type="gray">
              {intl.formatMessage(
                { id: "vectorReport.freshness.as" },
                {
                  time: indices.freshness
                    ? new Date(indices.freshness).toLocaleString()
                    : intl.formatMessage({
                        id: "vectorReport.freshness.unknown",
                      }),
                },
              )}
            </Tag>
          </div>
        )}

        {error && (
          <div
            style={{
              color: "var(--cds-support-error)",
              padding: "1rem",
              marginBottom: "1rem",
            }}
            data-testid="vector-error"
          >
            {error}
          </div>
        )}

        {loading && (
          <div style={{ padding: "1rem" }}>
            <SkeletonText paragraph lineCount={6} />
          </div>
        )}

        {!loading && !error && indices && empty && (
          <div
            style={{
              padding: "2rem",
              textAlign: "center",
              color: "var(--cds-text-helper)",
            }}
            data-testid="vector-empty"
          >
            <FormattedMessage id="vectorReport.empty" />
          </div>
        )}

        {!loading && !error && indices && !empty && (
          <Grid fullWidth>
            {/* Panel 1: Collection density over time */}
            <Column lg={16} md={8} sm={4} style={{ marginBottom: "1.5rem" }}>
              <Tile data-testid="panel-density">
                <h4>
                  <FormattedMessage id="vectorReport.density.title" />
                </h4>
                {densityData.length > 0 ? (
                  <LineChart
                    data={densityData}
                    options={{
                      title: "",
                      axes: {
                        bottom: { mapsTo: "key", scaleType: "labels" },
                        left: { mapsTo: "value", scaleType: "linear" },
                      },
                      legend: { position: "bottom" },
                      height: chartHeight,
                    }}
                  />
                ) : (
                  <p style={{ color: "var(--cds-text-helper)" }}>
                    <FormattedMessage id="vectorReport.empty" />
                  </p>
                )}
              </Tile>
            </Column>

            {/* Panel 2: Species distribution */}
            <Column lg={8} md={4} sm={4} style={{ marginBottom: "1.5rem" }}>
              <Tile data-testid="panel-species">
                <h4>
                  <FormattedMessage id="vectorReport.species.title" />
                </h4>
                {speciesData.length > 0 ? (
                  <DonutChart
                    data={speciesData}
                    options={{
                      title: "",
                      resizable: true,
                      legend: { position: "bottom" },
                      height: chartHeight,
                    }}
                  />
                ) : (
                  <p style={{ color: "var(--cds-text-helper)" }}>
                    <FormattedMessage id="vectorReport.empty" />
                  </p>
                )}
              </Tile>
            </Column>

            {/* Panel 5: QC pass-rate KPI */}
            <Column lg={8} md={4} sm={4} style={{ marginBottom: "1.5rem" }}>
              <Tile data-testid="panel-qc">
                <h4>
                  <FormattedMessage id="vectorReport.qc.title" />
                </h4>
                {qc ? (
                  <>
                    <p style={{ fontSize: "32px", fontWeight: 600 }}>
                      {Number(qc.passRatePct || 0).toFixed(1)}%
                    </p>
                    <p style={{ color: "var(--cds-text-secondary)" }}>
                      {intl.formatMessage(
                        { id: "vectorReport.qc.detail" },
                        {
                          passed: qc.analysesPassed || 0,
                          total: qc.analysesTotal || 0,
                        },
                      )}
                    </p>
                  </>
                ) : (
                  <p style={{ color: "var(--cds-text-helper)" }}>
                    <FormattedMessage id="vectorReport.empty" />
                  </p>
                )}
              </Tile>
            </Column>

            {/* Positivity-dependent panels (sporozoite, positivity, MIR) need the
                catalog significance classification. When it is absent, show a clear
                notice — never fake 0% / MIR 0. Density/species/QC still render. */}
            {!positivityConfigured && (
              <Column lg={16} md={8} sm={4} style={{ marginBottom: "1.5rem" }}>
                <Tile data-testid="vector-positivity-not-configured">
                  <h4>
                    <FormattedMessage id="vectorReport.positivity.title" />
                  </h4>
                  <p style={{ color: "var(--cds-text-helper)" }}>
                    <FormattedMessage id="vectorReport.positivity.notConfigured" />
                  </p>
                </Tile>
              </Column>
            )}

            {/* Sporozoite rate KPI (Anopheles CSP-ELISA) — top-level figure */}
            {positivityConfigured && (
              <Column lg={8} md={4} sm={4} style={{ marginBottom: "1.5rem" }}>
                <Tile data-testid="vector-sporozoite">
                  <h4>
                    <FormattedMessage id="vectorReport.sporozoite.title" />
                  </h4>
                  <p style={{ fontSize: "32px", fontWeight: 600 }}>
                    {indices.sporozoiteRatePct == null
                      ? "—"
                      : `${Number(indices.sporozoiteRatePct).toFixed(2)}%`}
                  </p>
                  <p
                    style={{
                      color: "var(--cds-text-secondary)",
                      fontSize: "12px",
                    }}
                  >
                    <FormattedMessage id="vectorReport.mir.sporozoiteNote" />
                  </p>
                </Tile>
              </Column>
            )}

            {/* Panel 4: Pathogen positivity */}
            {positivityConfigured && (
            <Column lg={16} md={8} sm={4} style={{ marginBottom: "1.5rem" }}>
              <Tile data-testid="panel-positivity">
                <h4>
                  <FormattedMessage id="vectorReport.positivity.title" />
                </h4>
                {positivityData.length > 0 ? (
                  <SimpleBarChart
                    data={positivityData}
                    options={{
                      title: "",
                      axes: {
                        left: { mapsTo: "value", scaleType: "linear" },
                        bottom: { mapsTo: "group", scaleType: "labels" },
                      },
                      legend: { enabled: false },
                      height: chartHeight,
                    }}
                  />
                ) : (
                  <p style={{ color: "var(--cds-text-helper)" }}>
                    <FormattedMessage id="vectorReport.empty" />
                  </p>
                )}
              </Tile>
            </Column>
            )}

            {/* Panel 3: MIR by species × pathogen (DataTable) */}
            {positivityConfigured && (
            <Column lg={16} md={8} sm={4} style={{ marginBottom: "1.5rem" }}>
              <Tile data-testid="panel-mir">
                <h4>
                  <FormattedMessage id="vectorReport.mir.title" />
                </h4>
                <p
                  style={{
                    fontSize: "12px",
                    color: "var(--cds-text-helper)",
                    marginBottom: "0.5rem",
                  }}
                >
                  <FormattedMessage id="vectorReport.mir.sporozoiteNote" />
                </p>
                <DataTable
                  rows={(indices.mirBySpecies || []).map((r, i) => ({
                    id: `mir-${i}`,
                    species: r.speciesLabel,
                    pathogen: r.pathogen,
                    mirClassic: Number(r.mirClassic).toFixed(2),
                    infectionRateObserved: Number(
                      r.infectionRateObserved,
                    ).toFixed(2),
                    positiveResolutionPct: `${Number(
                      r.positiveResolutionPct,
                    ).toFixed(1)}%`,
                  }))}
                  headers={[
                    {
                      key: "species",
                      header: intl.formatMessage({
                        id: "vectorReport.mir.species",
                      }),
                    },
                    {
                      key: "pathogen",
                      header: intl.formatMessage({
                        id: "vectorReport.mir.pathogen",
                      }),
                    },
                    {
                      key: "mirClassic",
                      header: intl.formatMessage({
                        id: "vectorReport.mir.classic",
                      }),
                    },
                    {
                      key: "infectionRateObserved",
                      header: intl.formatMessage({
                        id: "vectorReport.mir.observed",
                      }),
                    },
                    {
                      key: "positiveResolutionPct",
                      header: intl.formatMessage({
                        id: "vectorReport.mir.resolution",
                      }),
                    },
                  ]}
                >
                  {({ rows, headers, getHeaderProps, getTableProps }) => (
                    <TableContainer>
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
                            <TableRow key={row.id} data-testid="mir-row">
                              {row.cells.map((cell) => (
                                <TableCell
                                  key={cell.id}
                                  data-testid={
                                    cell.info.header === "pathogen"
                                      ? "mir-pathogen"
                                      : undefined
                                  }
                                >
                                  {cell.value}
                                </TableCell>
                              ))}
                            </TableRow>
                          ))}
                        </TableBody>
                      </Table>
                    </TableContainer>
                  )}
                </DataTable>
              </Tile>
            </Column>
            )}
          </Grid>
        )}

        {!loading && !error && !indices && (
          <Section>
            <div
              style={{
                padding: "2rem",
                textAlign: "center",
                color: "var(--cds-text-helper)",
              }}
              data-testid="vector-prompt"
            >
              <FormattedMessage id="vectorReport.prompt" />
            </div>
          </Section>
        )}
      </div>
    </div>
  );
}

export default VectorSurveillanceDashboard;
