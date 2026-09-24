import React, { useEffect, useState } from "react";
import {
  Column,
  Grid,
  Heading,
  InlineNotification,
  Loading,
  Section,
  Table,
  TableBody,
  TableCell,
  TableExpandedRow,
  TableExpandHeader,
  TableExpandRow,
  TableHead,
  TableHeader,
  TableRow,
  Tag,
} from "@carbon/react";
import { useIntl } from "react-intl";
import { useParams, Link as RouterLink } from "react-router-dom";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import { hintStyle } from "../eqaCommon";
import { fetchParticipantPerformance } from "./Workbench/workbenchApi";

const PERFORMANCE_TAG = {
  ACCEPTABLE: "green",
  QUESTIONABLE: "cyan",
  UNACCEPTABLE: "red",
};

/**
 * — how each participating laboratory is doing over time.
 *
 * The provider could already see one cycle at a time, on the workbench's
 * Receipts tab, and never the trend. This is the trend: one row per enrolled
 * laboratory, its rolling pass rate over its last four scored cycles, its most
 * recent verdict and its open follow-ups — which is the judgement the FR exists
 * to support, namely which laboratory is drifting.
 *
 * A row expands to the cycles behind its rate, each with the values reported and
 * their z-scores. That history rides the same server read as the row, so opening
 * one costs no request.
 *
 * No paging or filtering here — a scheme carries a handful of enrolled
 * laboratories, and the rate is computed server-side. Add a filter when a
 * deployment has enough participants to need one.
 */
const ParticipantPerformance = () => {
  const intl = useIntl();
  const { schemeId } = useParams();
  const t = (id, defaultMessage, values) =>
    intl.formatMessage({ id, defaultMessage }, values);

  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    let live = true;
    fetchParticipantPerformance(schemeId, (data) => {
      if (!live) {
        return;
      }
      setRows(data);
      setFailed(!Array.isArray(data));
      setLoading(false);
    });
    return () => {
      live = false;
    };
  }, [schemeId]);

  const breadcrumbs = [
    { label: "home.label", link: "/" },
    { label: "banner.menu.eqa.provider", link: "/qa/eqa/provider/schemes" },
  ];

  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Heading>
              {t("eqa.performance.title", "Participant performance")}
            </Heading>
            <p style={hintStyle}>
              {t(
                "eqa.performance.subtitle",
                "Each laboratory's pass rate over its last four scored cycles in this scheme. Expand a row for the cycles behind the rate.",
              )}
            </p>
          </Section>
        </Column>

        {loading && (
          <Column lg={16} md={8} sm={4}>
            <Loading withOverlay={false} small />
          </Column>
        )}

        {failed && (
          <Column lg={16} md={8} sm={4}>
            <InlineNotification
              kind="error"
              lowContrast
              hideCloseButton
              title={t("notification.title", "Notification Message")}
              subtitle={t(
                "eqa.performance.loadFailed",
                "Could not load participant performance",
              )}
            />
          </Column>
        )}

        {!loading && !failed && rows.length === 0 && (
          <Column lg={16} md={8} sm={4}>
            <p style={hintStyle}>
              {t(
                "eqa.performance.empty",
                "No laboratory is enrolled in this scheme yet.",
              )}
            </p>
          </Column>
        )}

        {!loading && !failed && rows.length > 0 && (
          <Column lg={16} md={8} sm={4}>
            <Table useZebraStyles>
              <TableHead>
                <TableRow>
                  <TableExpandHeader />
                  <TableHeader>
                    {t("eqa.performance.laboratory", "Laboratory")}
                  </TableHeader>
                  <TableHeader>
                    {t("eqa.performance.region", "Region")}
                  </TableHeader>
                  <TableHeader>
                    {t("eqa.performance.enrollment", "Enrollment")}
                  </TableHeader>
                  <TableHeader>
                    {t("eqa.performance.passRate", "Pass rate (last 4)")}
                  </TableHeader>
                  <TableHeader>
                    {t("eqa.performance.mostRecent", "Most recent")}
                  </TableHeader>
                  <TableHeader>
                    {t("eqa.performance.openFollowups", "Open follow-ups")}
                  </TableHeader>
                </TableRow>
              </TableHead>
              <TableBody>
                {rows.map((row) => (
                  <ParticipantRow key={row.organizationId} row={row} t={t} />
                ))}
              </TableBody>
            </Table>
          </Column>
        )}
      </Grid>
    </>
  );
};

/**
 * One laboratory, and the cycles behind its rate.
 *
 * The expanded row is mounted only while it is open, which is the lesson the scheme list
 * paid for: Carbon leaves an always-rendered expanded row's inner container at
 * max-height 0, and the history table then paints over the row above and
 * swallows its own links. Checked here with elementFromPoint rather than by
 * looking at a screenshot, which is how that defect hid the first time.
 */
const ParticipantRow = ({ row, t }) => {
  const [expanded, setExpanded] = useState(false);
  const rate =
    row.passRate === null || row.passRate === undefined
      ? t("eqa.performance.noRate", "No scored cycle yet")
      : `${row.passRate}%`;

  return (
    <>
      <TableExpandRow
        isExpanded={expanded}
        onExpand={() => setExpanded(!expanded)}
        ariaLabel={t("eqa.performance.expand", "Cycle history")}
      >
        <TableCell>{row.organizationName}</TableCell>
        <TableCell>{row.region || "—"}</TableCell>
        <TableCell>{row.enrollmentStatus || "—"}</TableCell>
        <TableCell>
          {rate}
          {row.judged > 0 && (
            <span style={hintStyle}>
              {" "}
              {t("eqa.performance.ofJudged", "{accepted} of {judged}", {
                accepted: row.accepted,
                judged: row.judged,
              })}
            </span>
          )}
        </TableCell>
        <TableCell>
          {row.mostRecentPerformance ? (
            <Tag type={PERFORMANCE_TAG[row.mostRecentPerformance] || "gray"}>
              {t(
                `eqa.performanceStatus.${row.mostRecentPerformance.toLowerCase()}`,
                row.mostRecentPerformance,
              )}
            </Tag>
          ) : (
            "—"
          )}
        </TableCell>
        <TableCell>{row.openFollowups}</TableCell>
      </TableExpandRow>
      {expanded && (
        <TableExpandedRow colSpan={7}>
          {(row.cycles || []).length === 0 ? (
            <p style={hintStyle}>
              {t(
                "eqa.performance.noCycles",
                "This laboratory has not reported into a scored cycle yet.",
              )}
            </p>
          ) : (
            <Table size="sm">
              <TableHead>
                <TableRow>
                  <TableHeader>{t("eqa.col.cycle", "Cycle")}</TableHeader>
                  <TableHeader>
                    {t("eqa.report.table.analyte", "Analyte")}
                  </TableHeader>
                  <TableHeader>
                    {t("eqa.report.table.reported", "Reported")}
                  </TableHeader>
                  <TableHeader>
                    {t("eqa.report.table.zscore", "Z-score")}
                  </TableHeader>
                  <TableHeader>
                    {t("eqa.report.table.performance", "Performance")}
                  </TableHeader>
                </TableRow>
              </TableHead>
              <TableBody>
                {row.cycles.map((cycle) =>
                  (cycle.analytes || []).map((analyte, index) => (
                    <TableRow key={`${cycle.cycleId}-${analyte.test}-${index}`}>
                      <TableCell>
                        {index === 0 && (
                          <RouterLink
                            to={`/qa/eqa/provider/cycles/${cycle.cycleId}/workbench`}
                          >
                            {cycle.cycleName || `#${cycle.cycleNumber}`}
                          </RouterLink>
                        )}
                      </TableCell>
                      <TableCell>{analyte.test || "—"}</TableCell>
                      <TableCell>{analyte.reported ?? "—"}</TableCell>
                      <TableCell>{analyte.zScore ?? "—"}</TableCell>
                      <TableCell>
                        {analyte.performance
                          ? t(
                              `eqa.performanceStatus.${analyte.performance.toLowerCase()}`,
                              analyte.performance,
                            )
                          : "—"}
                      </TableCell>
                    </TableRow>
                  )),
                )}
              </TableBody>
            </Table>
          )}
        </TableExpandedRow>
      )}
    </>
  );
};

export default ParticipantPerformance;
