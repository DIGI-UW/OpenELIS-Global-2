import React, { useEffect, useState } from "react";
import {
  ClickableTile,
  Column,
  ContentSwitcher,
  Grid,
  Heading,
  InlineNotification,
  Loading,
  Search,
  Section,
  Switch,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
  Tile,
} from "@carbon/react";
import { useIntl } from "react-intl";
import { useHistory } from "react-router-dom";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import { formatDateOnly } from "../../utils/Utils";
import {
  CycleStatusTag,
  hintStyle,
  kpiLabelStyle,
  kpiValueStyle,
} from "../eqaCommon";
import { fetchLabPerformance, NCE_REGISTER_EQA_LINK } from "./performanceApi";

const breadcrumbs = [
  { label: "home.label", link: "/" },
  {
    label: "eqa.labperf.title",
    link: "/qa/eqa/lab-performance/coverage",
  },
];

// The mockup's four-state matrix. Carbon has no token for "a cell that scored",
// so the palette is the Carbon status colours written out once.
const CELL = {
  acceptable: { glyph: "A", background: "#a7f0ba", color: "#0e6027" },
  questionable: { glyph: "?", background: "#ffd7a8", color: "#8a3800" },
  unacceptable: { glyph: "!", background: "#ffb3b8", color: "#a2191f" },
  missing: { glyph: "—", background: "#e0e0e0", color: "#6f6f6f" },
};

const PERFORMANCE_COLOR = {
  acceptable: "#0e6027",
  questionable: "#8a3800",
  unacceptable: "#a2191f",
};

const cellStyle = (verdict) => ({
  display: "inline-block",
  minWidth: "1.75rem",
  textAlign: "center",
  padding: "0.125rem 0.375rem",
  borderRadius: "2px",
  fontWeight: 600,
  ...CELL[verdict || "missing"],
});

const percent = (value) =>
  value === null || value === undefined ? "—" : `${value}%`;

/**
 * FR-V2.3-07 — the accreditation snapshot: lab-wide twelve-month KPIs above,
 * then either the per-section coverage matrix (Coverage) or the cycles behind
 * it (Recent Cycles). Both views read one rollup, so the KPI row cannot
 * disagree with the table under it.
 */
const LabPerformancePage = ({ view = "coverage" }) => {
  const intl = useIntl();
  const t = (id, defaultMessage, values) =>
    intl.formatMessage({ id, defaultMessage }, values);
  const history = useHistory();

  const [data, setData] = useState(null);
  const [filter, setFilter] = useState("");

  useEffect(() => {
    fetchLabPerformance(setData);
  }, []);

  if (!data) {
    return (
      <>
        <PageBreadCrumb breadcrumbs={breadcrumbs} />
        <Loading withOverlay={false} small />
      </>
    );
  }

  const { kpis, coverage, gaps, recentCycles } = data;
  const cycles = recentCycles.filter((cycle) =>
    filter
      ? (cycle.schemeName || "").toLowerCase().includes(filter.toLowerCase())
      : true,
  );
  const sectionLabel = (section) =>
    section || t("eqa.labperf.unassignedSection", "Unassigned section");
  const cycleColumns = Math.max(0, ...coverage.map((row) => row.cells.length));

  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Heading>
              {view === "recent"
                ? t(
                    "eqa.labperf.recent.title",
                    "Lab EQA Performance — Recent Cycles",
                  )
                : t(
                    "eqa.labperf.coverage.title",
                    "Lab EQA Performance — Coverage",
                  )}
            </Heading>
            <p style={{ ...hintStyle, marginBottom: "1rem" }}>
              {view === "recent"
                ? t(
                    "eqa.labperf.recent.subtitle",
                    "Cycles this lab reported in the last 12 months, with scoring status and verdict.",
                  )
                : t(
                    "eqa.labperf.coverage.subtitle",
                    "Accreditation snapshot: lab-wide KPIs, then EQA coverage by section across each scheme's last four cycles. Sections with no active PT are candidates for ISO 15189 §7.7.2 alternative assessment.",
                  )}
            </p>
          </Section>

          <ContentSwitcher
            selectedIndex={view === "recent" ? 1 : 0}
            onChange={({ index }) =>
              history.push(
                index === 1
                  ? "/qa/eqa/lab-performance/recent"
                  : "/qa/eqa/lab-performance/coverage",
              )
            }
            style={{ maxWidth: "24rem", marginBottom: "1rem" }}
          >
            <Switch
              name="coverage"
              text={t("eqa.labperf.tab.coverage", "Coverage")}
            />
            <Switch
              name="recent"
              text={t("eqa.labperf.tab.recent", "Recent Cycles")}
            />
          </ContentSwitcher>

          <Grid condensed style={{ marginBottom: "1.5rem" }}>
            <Column lg={4} md={2} sm={4}>
              <Tile data-testid="kpi-acceptance">
                <h4 style={kpiLabelStyle}>
                  {t("eqa.labperf.kpi.acceptance", "Acceptance rate (12 mo)")}
                </h4>
                <p style={kpiValueStyle}>{percent(kpis.acceptanceRate)}</p>
                <span style={hintStyle}>
                  {kpis.acceptanceDelta === null ||
                  kpis.acceptanceDelta === undefined
                    ? t(
                        "eqa.labperf.kpi.noPriorYear",
                        "no prior year to compare",
                      )
                    : t("eqa.labperf.kpi.delta", "{delta} vs. prior year", {
                        delta: `${kpis.acceptanceDelta > 0 ? "+" : ""}${kpis.acceptanceDelta}%`,
                      })}
                </span>
              </Tile>
            </Column>
            <Column lg={4} md={2} sm={4}>
              <Tile data-testid="kpi-ontime">
                <h4 style={kpiLabelStyle}>
                  {t("eqa.labperf.kpi.onTime", "On-time submission rate")}
                </h4>
                <p style={kpiValueStyle}>{percent(kpis.onTimeRate)}</p>
                <span style={hintStyle}>
                  {t("eqa.labperf.kpi.late", "{late} late of {total}", {
                    late: kpis.lateCount ?? 0,
                    total: kpis.submittedCount ?? 0,
                  })}
                </span>
              </Tile>
            </Column>
            <Column lg={4} md={2} sm={4}>
              <ClickableTile
                data-testid="kpi-nce"
                href={NCE_REGISTER_EQA_LINK}
                title={t(
                  "eqa.labperf.openNceRegister",
                  "Open the NCE register filtered to EQA-triggered events",
                )}
              >
                <h4 style={kpiLabelStyle}>
                  {t("eqa.labperf.kpi.nce", "EQA-triggered NCEs (12 mo)")}
                </h4>
                <p style={kpiValueStyle}>{kpis.eqaNceCount ?? 0}</p>
                <span
                  style={{
                    ...hintStyle,
                    color: kpis.eqaNceOpenCount ? "#a2191f" : undefined,
                  }}
                >
                  {t("eqa.labperf.kpi.nceOpen", "{open} open → NCE register", {
                    open: kpis.eqaNceOpenCount ?? 0,
                  })}
                </span>
              </ClickableTile>
            </Column>
            <Column lg={4} md={2} sm={4}>
              <Tile data-testid="kpi-uncovered">
                <h4 style={kpiLabelStyle}>
                  {t(
                    "eqa.labperf.kpi.uncovered",
                    "Accredited tests without EQA",
                  )}
                </h4>
                <p
                  style={{
                    ...kpiValueStyle,
                    color: kpis.uncoveredTestCount ? "#a2191f" : undefined,
                  }}
                >
                  {kpis.uncoveredTestCount ?? 0}
                </p>
                <span style={hintStyle}>
                  {t("eqa.labperf.kpi.uncovered.hint", "§7.7.2 candidates")}
                </span>
              </Tile>
            </Column>
          </Grid>

          {view === "coverage" ? (
            <>
              <p style={{ ...hintStyle, marginBottom: "0.5rem" }}>
                {t(
                  "eqa.labperf.legendIntro",
                  "Each scheme's last four cycles, by section.",
                )}{" "}
                {["acceptable", "questionable", "unacceptable", "missing"].map(
                  (verdict) => (
                    <span key={verdict} style={{ marginRight: "0.75rem" }}>
                      <span style={cellStyle(verdict)}>
                        {CELL[verdict].glyph}
                      </span>{" "}
                      {t(
                        `eqa.labperf.legend.${verdict}`,
                        verdict === "missing"
                          ? "Not enrolled / missed"
                          : verdict.charAt(0).toUpperCase() + verdict.slice(1),
                      )}
                    </span>
                  ),
                )}
              </p>

              {coverage.length === 0 ? (
                <Tile>
                  {t(
                    "eqa.labperf.coverage.empty",
                    "No scored EQA results yet. Coverage appears once a cycle this lab took part in has been scored.",
                  )}
                </Tile>
              ) : (
                <Table useZebraStyles>
                  <TableHead>
                    <TableRow>
                      <TableHeader>
                        {t("eqa.labperf.section", "Section")}
                      </TableHeader>
                      <TableHeader>
                        {t("eqa.labperf.scheme", "Scheme")}
                      </TableHeader>
                      {Array.from({ length: cycleColumns }, (_, i) => (
                        <TableHeader key={i}>
                          {i === cycleColumns - 1
                            ? t("eqa.labperf.mostRecent", "Most recent")
                            : t("eqa.labperf.cycleOffset", "Cycle −{n}", {
                                n: cycleColumns - 1 - i,
                              })}
                        </TableHeader>
                      ))}
                      <TableHeader>
                        {t("eqa.labperf.acceptanceRate", "Acceptance rate")}
                      </TableHeader>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {coverage.map((row) => (
                      <TableRow
                        key={`${row.schemeId}-${row.section || "none"}`}
                      >
                        <TableCell>
                          <strong>{sectionLabel(row.section)}</strong>
                        </TableCell>
                        <TableCell>{row.schemeName}</TableCell>
                        {Array.from({ length: cycleColumns }, (_, i) => {
                          // Rows are right-aligned on "most recent": a scheme with
                          // fewer than four cycles pads on the left, or its newest
                          // cycle would sit under another scheme's oldest.
                          const offset = cycleColumns - row.cells.length;
                          const cell =
                            i < offset ? null : row.cells[i - offset];
                          return (
                            <TableCell key={i}>
                              <span
                                style={cellStyle(
                                  cell ? cell.verdict : "missing",
                                )}
                                title={cell ? cell.cycleLabel : undefined}
                              >
                                {CELL[cell ? cell.verdict : "missing"].glyph}
                              </span>
                            </TableCell>
                          );
                        })}
                        <TableCell>{percent(row.acceptanceRate)}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              )}

              {gaps.length > 0 && (
                <InlineNotification
                  kind="warning"
                  lowContrast
                  hideCloseButton
                  data-testid="coverage-gap-callout"
                  title={t(
                    "eqa.labperf.gapTitle",
                    "Accredited without EQA cover:",
                  )}
                  subtitle={t(
                    "eqa.labperf.gapBody",
                    "{tests} — no active EQA enrollment, so ISO 15189 §7.7.2 alternative assessment is required.",
                    { tests: gaps.map((gap) => gap.testName).join(", ") },
                  )}
                  style={{ marginTop: "1rem", maxWidth: "100%" }}
                />
              )}
            </>
          ) : (
            <>
              <Search
                size="sm"
                labelText={t("eqa.labperf.filterScheme", "Filter scheme")}
                placeholder={t("eqa.labperf.filterScheme", "Filter scheme")}
                value={filter}
                onChange={(event) => setFilter(event.target.value)}
                style={{ marginBottom: "0.5rem" }}
              />
              {cycles.length === 0 ? (
                <Tile>
                  {t(
                    "eqa.labperf.recent.empty",
                    "No cycles reported in the last 12 months.",
                  )}
                </Tile>
              ) : (
                <Table useZebraStyles>
                  <TableHead>
                    <TableRow>
                      <TableHeader>
                        {t("eqa.labperf.scheme", "Scheme")}
                      </TableHeader>
                      <TableHeader>
                        {t("eqa.labperf.cycle", "Cycle")}
                      </TableHeader>
                      <TableHeader>
                        {t("eqa.labperf.status", "Status")}
                      </TableHeader>
                      <TableHeader>
                        {t("eqa.labperf.score", "Score")}
                      </TableHeader>
                      <TableHeader>
                        {t("eqa.labperf.performance", "Performance")}
                      </TableHeader>
                      <TableHeader>
                        {t("eqa.labperf.submitted", "Submitted")}
                      </TableHeader>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {cycles.map((cycle) => (
                      <TableRow key={cycle.cycleId}>
                        <TableCell>
                          <strong>{cycle.schemeName || "—"}</strong>
                        </TableCell>
                        <TableCell>{cycle.cycleLabel}</TableCell>
                        <TableCell>
                          <CycleStatusTag status={cycle.status} />
                        </TableCell>
                        <TableCell>
                          {cycle.scoredCount
                            ? t(
                                "eqa.labperf.scoreOf",
                                "{acceptable} of {scored}",
                                {
                                  acceptable: cycle.acceptableCount,
                                  scored: cycle.scoredCount,
                                },
                              )
                            : "—"}
                        </TableCell>
                        <TableCell>
                          {cycle.performance ? (
                            <span
                              style={{
                                fontWeight: 600,
                                color: PERFORMANCE_COLOR[cycle.performance],
                              }}
                            >
                              {t(
                                `eqa.labperf.verdict.${cycle.performance}`,
                                cycle.performance,
                              )}
                            </span>
                          ) : (
                            <em style={hintStyle}>
                              {t("eqa.labperf.pending", "pending")}
                            </em>
                          )}
                        </TableCell>
                        <TableCell>
                          {cycle.submittedAt
                            ? formatDateOnly(cycle.submittedAt)
                            : "—"}
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              )}
            </>
          )}
        </Column>
      </Grid>
    </>
  );
};

export default LabPerformancePage;
