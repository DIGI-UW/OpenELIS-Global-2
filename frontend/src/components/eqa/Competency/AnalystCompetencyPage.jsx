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
 * — the ISO 15189 §6.2.3 evidence an assessor asks for: who ran PT
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

  /**
   * Under review is reached by two rules that mean opposite things, so the band
   * alone is not a finding. The short name separates them at a glance and the
   * sentence carries the rule, the window it was read over, the denominator and
   * what the reviewer should do next.
   */
  const ruleLabel = (reason) =>
    ({
      MEETS_EVIDENCE: t("eqa.competency.rule.meetsEvidence", "Evidence met"),
      REPEATED_FAILURE: t(
        "eqa.competency.rule.repeatedFailure",
        "Repeated failure",
      ),
      INSUFFICIENT_EVIDENCE: t(
        "eqa.competency.rule.insufficientEvidence",
        "Insufficient evidence",
      ),
      OPEN_ESCALATION: t(
        "eqa.competency.rule.openEscalation",
        "Open non-conformity",
      ),
    })[reason] || null;

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
  const judgedOver = {
    from: formatDateOnly(data.windowStart),
    to: formatDateOnly(data.windowEnd),
    floor: data.evidenceFloor,
  };

  /** The sentence behind one band: rule, window, denominator, next action. */
  const reasonSentence = (reason, evaluable, failures) => {
    const values = { ...judgedOver, evaluable, failures };
    switch (reason) {
      case "MEETS_EVIDENCE":
        return t(
          "eqa.competency.reason.meetsEvidence",
          "{evaluable} assessable samples with {failures} failures between {from} and {to}, at or above the evidence floor of {floor}. Nothing to action.",
          values,
        );
      case "REPEATED_FAILURE":
        return t(
          "eqa.competency.reason.repeatedFailure",
          "{failures} failures in {evaluable} assessable samples between {from} and {to}. This is a performance concern: review the failed results and raise a non-conformity, or dismiss them on triage.",
          values,
        );
      case "INSUFFICIENT_EVIDENCE":
        return t(
          "eqa.competency.reason.insufficientEvidence",
          "{evaluable} assessable samples between {from} and {to}, short of the evidence floor of {floor}. This is not a performance concern: assign this analyst more proficiency testing samples.",
          values,
        );
      case "OPEN_ESCALATION":
        return t(
          "eqa.competency.reason.openEscalation",
          "An escalated non-conformity raised between {from} and {to} is still open, over {evaluable} assessable samples with {failures} failures. Close the non-conformity to clear the band.",
          values,
        );
      default:
        return null;
    }
  };

  /**
   * The band's rule and its sentence, under whichever tag it explains. The
   * analyte is named because a rule reads over one analyte at a time, so its
   * counts are that analyte's rather than the analyst's.
   */
  const BandReason = ({ reason, analyteName, evaluable, failures }) =>
    ruleLabel(reason) ? (
      <div style={{ marginTop: "0.25rem" }}>
        <div style={{ ...hintStyle, fontWeight: 600 }}>
          {analyteName
            ? `${ruleLabel(reason)} · ${analyteName}`
            : ruleLabel(reason)}
        </div>
        <div style={hintStyle}>
          {reasonSentence(reason, evaluable, failures)}
        </div>
      </div>
    ) : null;

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
            <p style={hintStyle}>
              {t(
                "eqa.competency.subtitle",
                "Every analyst assigned to proficiency testing in the last twelve months, banded per analyte. An analyst is competent only where the evidence says so — fewer than four assessable samples reads as under review, not as a pass.",
              )}
            </p>
            <p style={{ ...hintStyle, marginBottom: "1.5rem" }}>
              {t(
                "eqa.competency.whatIsAnAnalyst",
                "An analyst is an OpenELIS user account on the scheme's analyst roster, recorded against the sample they ran. That is a separate record from whoever entered the result and whoever validated it.",
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
                      {(analyst.statusReasons || []).map((why) => (
                        <BandReason
                          key={`${why.reason}-${why.analyteName}`}
                          reason={why.reason}
                          analyteName={why.analyteName}
                          evaluable={why.evaluableCount}
                          failures={why.failureCount}
                        />
                      ))}
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
                                  <BandReason
                                    reason={analyte.reason}
                                    evaluable={analyte.evaluableCount}
                                    failures={analyte.failureCount}
                                  />
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
