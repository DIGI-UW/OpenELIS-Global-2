import React, { useEffect, useState } from "react";
import {
  Button,
  Column,
  Grid,
  Heading,
  Loading,
  Search,
  Section,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
  Tag,
  Tile,
} from "@carbon/react";
import { ChevronDown, ChevronUp, Download } from "@carbon/react/icons";
import { useIntl } from "react-intl";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import { formatDateOnly } from "../../utils/Utils";
import {
  csvCell,
  downloadCsv,
  hintStyle,
  kpiLabelStyle,
  kpiValueStyle,
} from "../eqaCommon";
import { fetchAnalystCompetency } from "./competencyApi";

const breadcrumbs = [
  { label: "home.label", link: "/" },
  {
    label: "banner.menu.eqa.analystCompetency",
    link: "/qa/eqa/analyst-competency",
  },
];

const BAND_TAG = {
  COMPETENT: "green",
  UNDER_REVIEW: "warm-gray",
  NOT_COMPETENT: "red",
};

const PERFORMANCE_TAG = {
  acceptable: "green",
  questionable: "warm-gray",
  unacceptable: "red",
  missed: "magenta",
  dismissed: "cool-gray",
};

/**
 * FR-V2.3-06 — the ISO 15189 §6.2.3 evidence an assessor asks for: who ran PT
 * in the last twelve months, what each analyte says about them, and every event
 * the band was computed from.
 *
 * <p>Bands are asserted by the server so the page cannot round them differently
 * from the register beneath it; this renders the verdict, not the rules.
 */
const AnalystCompetencyPage = () => {
  const intl = useIntl();
  const t = (id, defaultMessage, values) =>
    intl.formatMessage({ id, defaultMessage }, values);

  const [data, setData] = useState(null);
  const [filter, setFilter] = useState("");
  const [expanded, setExpanded] = useState(null);

  useEffect(() => {
    fetchAnalystCompetency(setData);
  }, []);

  const bandLabel = (band) =>
    ({
      COMPETENT: t("eqa.competency.band.competent", "Competent"),
      UNDER_REVIEW: t("eqa.competency.band.underReview", "Under review"),
      NOT_COMPETENT: t("eqa.competency.band.notCompetent", "Not competent"),
    })[band] || band;

  const outcomeLabel = (outcome) =>
    ({
      acceptable: t("eqa.competency.outcome.acceptable", "Acceptable"),
      questionable: t("eqa.competency.outcome.questionable", "Questionable"),
      unacceptable: t("eqa.competency.outcome.unacceptable", "Unacceptable"),
      missed: t("eqa.competency.outcome.missed", "Missed deadline"),
      dismissed: t("eqa.competency.outcome.dismissed", "Dismissed on triage"),
    })[outcome] || "—";

  const eventLabel = (eventType) =>
    eventType
      ? t(`eqa.competency.event.${eventType}`, eventType.replace(/_/g, " "))
      : t("eqa.competency.event.scored", "Scored result");

  if (!data) {
    return (
      <>
        <PageBreadCrumb breadcrumbs={breadcrumbs} />
        <Loading withOverlay={false} small />
      </>
    );
  }

  const { kpis, analysts } = data;
  const rows = analysts.filter((analyst) =>
    filter
      ? (analyst.analystName || "").toLowerCase().includes(filter.toLowerCase())
      : true,
  );

  const exportCsv = () => {
    const header = [
      "Analyst",
      "Status",
      "PT samples (12 mo)",
      "Samples this year",
      "Evaluable",
      "Failures",
      "Most recent",
      "Most recent date",
    ];
    const body = rows.map((analyst) =>
      [
        analyst.analystName,
        bandLabel(analyst.status),
        analyst.sampleCount,
        analyst.sampleCountThisYear,
        analyst.evaluableCount,
        analyst.failureCount,
        outcomeLabel(analyst.mostRecentPerformance),
        analyst.mostRecentDate || "",
      ]
        .map(csvCell)
        .join(","),
    );
    downloadCsv(
      [header.map(csvCell).join(","), ...body].join("\n"),
      "eqa-analyst-competency.csv",
    );
  };

  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Heading>
              {t("banner.menu.eqa.analystCompetency", "Analyst Competency")}
            </Heading>
            <p style={{ ...hintStyle, marginBottom: "1.5rem" }}>
              {t(
                "eqa.competency.subtitle",
                "Every analyst assigned to proficiency testing in the last twelve months, banded per analyte. An analyst is competent only where the evidence says so — fewer than four assessable samples reads as under review, not as a pass.",
              )}
            </p>
          </Section>

          <Grid condensed style={{ marginBottom: "1.5rem" }}>
            <Column lg={4} md={2} sm={4}>
              <Tile data-testid="kpi-analysts">
                <h4 style={kpiLabelStyle}>
                  {t("eqa.competency.kpi.analysts", "Analysts assessed")}
                </h4>
                <p style={kpiValueStyle}>{kpis.analystCount ?? 0}</p>
                <span style={hintStyle}>
                  {t("eqa.competency.kpi.samples", "{count} PT samples", {
                    count: kpis.assessedSampleCount ?? 0,
                  })}
                </span>
              </Tile>
            </Column>
            <Column lg={4} md={2} sm={4}>
              <Tile data-testid="kpi-competent">
                <h4 style={kpiLabelStyle}>
                  {t("eqa.competency.band.competent", "Competent")}
                </h4>
                <p style={kpiValueStyle}>{kpis.competentCount ?? 0}</p>
              </Tile>
            </Column>
            <Column lg={4} md={2} sm={4}>
              <Tile data-testid="kpi-under-review">
                <h4 style={kpiLabelStyle}>
                  {t("eqa.competency.band.underReview", "Under review")}
                </h4>
                <p style={kpiValueStyle}>{kpis.underReviewCount ?? 0}</p>
              </Tile>
            </Column>
            <Column lg={4} md={2} sm={4}>
              <Tile data-testid="kpi-not-competent">
                <h4 style={kpiLabelStyle}>
                  {t("eqa.competency.band.notCompetent", "Not competent")}
                </h4>
                <p style={kpiValueStyle}>{kpis.notCompetentCount ?? 0}</p>
                <span style={hintStyle}>
                  {t(
                    "eqa.competency.kpi.notCompetent.hint",
                    "Open escalated NCE",
                  )}
                </span>
              </Tile>
            </Column>
          </Grid>

          <div
            style={{
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
              marginBottom: "0.5rem",
              gap: "1rem",
            }}
          >
            <Search
              size="sm"
              labelText={t("eqa.competency.search", "Filter analysts")}
              placeholder={t("eqa.competency.search", "Filter analysts")}
              value={filter}
              onChange={(event) => setFilter(event.target.value)}
            />
            <Button
              kind="ghost"
              size="sm"
              renderIcon={Download}
              disabled={rows.length === 0}
              onClick={exportCsv}
            >
              {t("eqa.competency.export", "Export CSV")}
            </Button>
          </div>

          <Table useZebraStyles>
            <TableHead>
              <TableRow>
                <TableHeader>
                  {t("eqa.competency.analyst", "Analyst")}
                </TableHeader>
                <TableHeader>
                  {t("eqa.competency.samples", "PT samples (12 mo)")}
                </TableHeader>
                <TableHeader>
                  {t("eqa.competency.recent", "Most recent performance")}
                </TableHeader>
                <TableHeader>
                  {t("eqa.competency.status", "Competency")}
                </TableHeader>
                <TableHeader>
                  {t("eqa.competency.history", "History")}
                </TableHeader>
              </TableRow>
            </TableHead>
            <TableBody>
              {rows.length === 0 && (
                <TableRow>
                  <TableCell colSpan={5}>
                    {t(
                      "eqa.competency.empty",
                      "No analyst has been recorded on an EQA result in the last twelve months. Analysts appear here once results carry an assigned analyst.",
                    )}
                  </TableCell>
                </TableRow>
              )}
              {rows.map((analyst) => (
                <React.Fragment key={analyst.analystId}>
                  <TableRow>
                    <TableCell>
                      <strong>{analyst.analystName}</strong>
                      <div style={hintStyle}>
                        {t(
                          "eqa.competency.analyteCount",
                          "{count} analytes assessed",
                          { count: (analyst.analytes || []).length },
                        )}
                      </div>
                    </TableCell>
                    <TableCell>
                      {analyst.sampleCount}
                      <div style={hintStyle}>
                        {t(
                          "eqa.competency.thisYear",
                          "{count} this year · {failures} failed",
                          {
                            count: analyst.sampleCountThisYear,
                            failures: analyst.failureCount,
                          },
                        )}
                      </div>
                    </TableCell>
                    <TableCell>
                      {analyst.mostRecentPerformance ? (
                        <>
                          <Tag
                            type={
                              PERFORMANCE_TAG[analyst.mostRecentPerformance]
                            }
                            size="sm"
                          >
                            {outcomeLabel(analyst.mostRecentPerformance)}
                          </Tag>
                          <div style={hintStyle}>
                            {analyst.mostRecentDate
                              ? formatDateOnly(analyst.mostRecentDate)
                              : "—"}
                          </div>
                        </>
                      ) : (
                        "—"
                      )}
                    </TableCell>
                    <TableCell>
                      <Tag type={BAND_TAG[analyst.status]} size="sm">
                        {bandLabel(analyst.status)}
                      </Tag>
                    </TableCell>
                    <TableCell>
                      <Button
                        kind="ghost"
                        size="sm"
                        hasIconOnly
                        renderIcon={
                          expanded === analyst.analystId
                            ? ChevronUp
                            : ChevronDown
                        }
                        iconDescription={t(
                          "eqa.competency.viewHistory",
                          "View history",
                        )}
                        onClick={() =>
                          setExpanded(
                            expanded === analyst.analystId
                              ? null
                              : analyst.analystId,
                          )
                        }
                      />
                    </TableCell>
                  </TableRow>
                  {expanded === analyst.analystId && (
                    <TableRow data-testid={`history-${analyst.analystId}`}>
                      <TableCell colSpan={5}>
                        <div style={{ fontWeight: 600, marginBottom: 6 }}>
                          {t(
                            "eqa.competency.byAnalyte",
                            "Competency by analyte",
                          )}
                        </div>
                        <Table size="sm" style={{ marginBottom: "1rem" }}>
                          <TableHead>
                            <TableRow>
                              <TableHeader>
                                {t("eqa.competency.analyte", "Analyte")}
                              </TableHeader>
                              <TableHeader>
                                {t("eqa.competency.evaluable", "Evaluable")}
                              </TableHeader>
                              <TableHeader>
                                {t("eqa.competency.failures", "Failures")}
                              </TableHeader>
                              <TableHeader>
                                {t("eqa.competency.latest", "Latest")}
                              </TableHeader>
                              <TableHeader>
                                {t("eqa.competency.status", "Competency")}
                              </TableHeader>
                            </TableRow>
                          </TableHead>
                          <TableBody>
                            {(analyst.analytes || []).map((analyte) => (
                              <TableRow
                                key={analyte.analyteId || analyte.analyteName}
                              >
                                <TableCell>
                                  {analyte.analyteName ||
                                    t(
                                      "eqa.competency.unnamedAnalyte",
                                      "Unnamed analyte",
                                    )}
                                </TableCell>
                                <TableCell>{analyte.evaluableCount}</TableCell>
                                <TableCell>{analyte.failureCount}</TableCell>
                                <TableCell>
                                  {outcomeLabel(analyte.latestPerformance)}
                                </TableCell>
                                <TableCell>
                                  <Tag
                                    type={BAND_TAG[analyte.status]}
                                    size="sm"
                                  >
                                    {bandLabel(analyte.status)}
                                  </Tag>
                                </TableCell>
                              </TableRow>
                            ))}
                          </TableBody>
                        </Table>

                        <div style={{ fontWeight: 600, marginBottom: 6 }}>
                          {t(
                            "eqa.competency.evidence",
                            "Evidence · every event in the window",
                          )}
                        </div>
                        <Table size="sm">
                          <TableHead>
                            <TableRow>
                              <TableHeader>
                                {t("eqa.competency.date", "Date")}
                              </TableHeader>
                              <TableHeader>
                                {t("eqa.competency.scheme", "Scheme")}
                              </TableHeader>
                              <TableHeader>
                                {t("eqa.competency.analyte", "Analyte")}
                              </TableHeader>
                              <TableHeader>
                                {t("eqa.competency.event", "Event")}
                              </TableHeader>
                              <TableHeader>
                                {t("eqa.competency.outcome", "Outcome")}
                              </TableHeader>
                              <TableHeader>
                                {t("eqa.competency.counted", "Counts against")}
                              </TableHeader>
                            </TableRow>
                          </TableHead>
                          <TableBody>
                            {(analyst.history || []).map((event, index) => (
                              <TableRow key={index}>
                                <TableCell>
                                  {formatDateOnly(event.date)}
                                </TableCell>
                                <TableCell>{event.schemeName || "—"}</TableCell>
                                <TableCell>
                                  {event.analyteName || "—"}
                                </TableCell>
                                <TableCell>
                                  {eventLabel(event.eventType)}
                                </TableCell>
                                <TableCell>
                                  <Tag
                                    type={PERFORMANCE_TAG[event.outcome]}
                                    size="sm"
                                  >
                                    {outcomeLabel(event.outcome)}
                                  </Tag>
                                </TableCell>
                                <TableCell>
                                  {event.failure
                                    ? t("eqa.competency.countsFail", "Failure")
                                    : event.counted
                                      ? t(
                                          "eqa.competency.countsPass",
                                          "Assessed",
                                        )
                                      : t(
                                          "eqa.competency.countsExcused",
                                          "Excused",
                                        )}
                                </TableCell>
                              </TableRow>
                            ))}
                          </TableBody>
                        </Table>
                      </TableCell>
                    </TableRow>
                  )}
                </React.Fragment>
              ))}
            </TableBody>
          </Table>
        </Column>
      </Grid>
    </>
  );
};

export default AnalystCompetencyPage;
