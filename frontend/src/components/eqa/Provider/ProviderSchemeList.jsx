import React, { useContext, useEffect, useState } from "react";
import {
  Button,
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
  Tile,
} from "@carbon/react";
import { useIntl } from "react-intl";
import { Link as RouterLink, useHistory } from "react-router-dom";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import UserSessionDetailsContext from "../../../UserSessionDetailsContext";
import { hasQaPermission } from "../../utils/Utils";
import {
  CycleStatusTag,
  hintStyle,
  kpiLabelStyle,
  kpiValueStyle,
} from "../eqaCommon";
import { fetchProviderSchemes } from "./Workbench/workbenchApi";

const breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "banner.menu.eqa.provider", link: "/qa/eqa/provider/schemes" },
];

/**
 * Provider scheme list (FR-V2.5-01) — the entry point to the provider lane, and
 * what qa/030 points the EQA Provider menu row at.
 *
 * A scheme appears here when another laboratory is actively enrolled in it,
 * which is what makes this lab its provider; a scheme this lab merely takes part
 * in belongs on My Cycles instead. Each row expands to the scheme's cycles,
 * every one of them a link into the prep and shipping workbenches.
 *
 * ponytail: rows come from one server read and are neither filtered nor paged
 * here — a lab provides a handful of schemes, and the server already groups the
 * counts. Add a filter when a deployment has enough schemes to need one.
 */
const ProviderSchemeList = () => {
  const intl = useIntl();
  const history = useHistory();
  const t = (id, defaultMessage, values) =>
    intl.formatMessage({ id, defaultMessage }, values);

  // FR-V2.5-01 role-conditional composition (T-39): the provider panel only
  // renders for the provider grant; a participant-only viewer gets links to
  // the participant surfaces instead of a blank page. The menu row itself is
  // visible to every EQA viewer today (menu reads are not permission-filtered)
  // — hiding it and the FR's sidebar hint are deferred to OGC-1151, which will
  // also need a system_module_url mapping for this route; until then this
  // fallback is what an ungranted click lands on.
  const { userSessionDetails } = useContext(UserSessionDetailsContext);
  const isProvider = hasQaPermission(userSessionDetails, "qa.eqa.provider");

  const [board, setBoard] = useState({ kpis: {}, schemes: [] });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!isProvider) {
      return;
    }
    fetchProviderSchemes((data) => {
      setBoard(data);
      setLoading(false);
    });
  }, [isProvider]);

  const { kpis, schemes } = board;

  if (!isProvider) {
    return (
      <>
        <PageBreadCrumb breadcrumbs={breadcrumbs} />
        <Grid fullWidth>
          <Column lg={16} md={8} sm={4}>
            <InlineNotification
              kind="info"
              lowContrast
              hideCloseButton
              title={t(
                "eqa.provider.schemes.participantOnly.title",
                "Participant view only",
              )}
              subtitle={t(
                "eqa.provider.schemes.participantOnly.body",
                "Provider scheme administration needs the provider grant. Your lab's participation lives on the pages below.",
              )}
            />
            <ul style={{ marginTop: "0.75rem" }}>
              <li>
                <RouterLink to="/qa/eqa/my-programs">
                  {t(
                    "eqa.provider.schemes.link.myPrograms",
                    "My enrollments — schemes this lab takes part in",
                  )}
                </RouterLink>
              </li>
              <li>
                <RouterLink to="/qa/eqa/my-cycles">
                  {t(
                    "eqa.provider.schemes.link.myCycles",
                    "My Cycles — panels, results and deadlines",
                  )}
                </RouterLink>
              </li>
            </ul>
          </Column>
        </Grid>
      </>
    );
  }

  if (loading) {
    return <Loading />;
  }

  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Grid fullWidth>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Heading>
              {t("eqa.provider.schemes.title", "EQA schemes we provide")}
            </Heading>
            <p style={hintStyle}>
              {t(
                "eqa.provider.schemes.hint",
                "Expand a scheme to see its cycles, or start a new cycle to define its panel and participants.",
              )}
            </p>
          </Section>
        </Column>
      </Grid>

      <Grid condensed style={{ marginBottom: "1.5rem" }}>
        <Column lg={4} md={2} sm={4}>
          <Tile data-testid="kpi-active-schemes">
            <h4 style={kpiLabelStyle}>
              {t("eqa.provider.kpi.activeSchemes", "Active schemes")}
            </h4>
            <p style={kpiValueStyle}>{kpis.activeSchemes ?? 0}</p>
          </Tile>
        </Column>
        <Column lg={4} md={2} sm={4}>
          <Tile data-testid="kpi-open-cycles">
            <h4 style={kpiLabelStyle}>
              {t("eqa.provider.kpi.openCycles", "Open cycles")}
            </h4>
            <p style={kpiValueStyle}>{kpis.openCycles ?? 0}</p>
          </Tile>
        </Column>
        <Column lg={4} md={2} sm={4}>
          <Tile data-testid="kpi-enrolled">
            <h4 style={kpiLabelStyle}>
              {t("eqa.provider.kpi.enrolled", "Enrolled participants")}
            </h4>
            <p style={kpiValueStyle}>{kpis.enrolledParticipants ?? 0}</p>
          </Tile>
        </Column>
        <Column lg={4} md={2} sm={4}>
          <Tile data-testid="kpi-followups-open">
            <h4 style={kpiLabelStyle}>
              {t("eqa.provider.kpi.followupsOpen", "Follow-ups open")}
            </h4>
            <p style={kpiValueStyle}>{kpis.followupsOpen ?? 0}</p>
          </Tile>
        </Column>
      </Grid>

      <Grid fullWidth>
        <Column lg={16} md={8} sm={4}>
          {schemes.length === 0 ? (
            <InlineNotification
              kind="info"
              lowContrast
              hideCloseButton
              title={t(
                "eqa.provider.schemes.none.title",
                "No schemes to provide yet",
              )}
              subtitle={t(
                "eqa.provider.schemes.none.body",
                "A scheme appears here once another laboratory is actively enrolled in it.",
              )}
            />
          ) : (
            <Table size="sm">
              <TableHead>
                <TableRow>
                  <TableExpandHeader />
                  <TableHeader>{t("eqa.col.scheme", "Scheme")}</TableHeader>
                  <TableHeader>
                    {t("eqa.provider.schemes.provider", "Provider")}
                  </TableHeader>
                  <TableHeader>{t("eqa.col.type", "Type")}</TableHeader>
                  <TableHeader>
                    {t("eqa.provider.schemes.discipline", "Discipline")}
                  </TableHeader>
                  <TableHeader>
                    {t("eqa.provider.schemes.enrolled", "Enrolled labs")}
                  </TableHeader>
                  <TableHeader>
                    {t("eqa.kpi.active", "Active cycles")}
                  </TableHeader>
                  <TableHeader>
                    {t(
                      "eqa.provider.schemes.lastDistribution",
                      "Last distribution",
                    )}
                  </TableHeader>
                  <TableHeader />
                </TableRow>
              </TableHead>
              <TableBody>
                {schemes.map((scheme) => (
                  <SchemeRows
                    key={scheme.id}
                    scheme={scheme}
                    t={t}
                    onNewCycle={() =>
                      history.push(
                        `/qa/eqa/provider/schemes/${scheme.id}/cycles/new`,
                      )
                    }
                  />
                ))}
              </TableBody>
            </Table>
          )}
        </Column>
      </Grid>
    </>
  );
};

/**
 * One scheme and its expanded cycle list. Carbon's TableExpandRow needs its
 * expansion state next to it, which is why this is a component rather than a
 * render helper.
 */
const SchemeRows = ({ scheme, t, onNewCycle }) => {
  const [expanded, setExpanded] = useState(false);
  const cycles = scheme.cycles || [];

  return (
    <>
      <TableExpandRow
        isExpanded={expanded}
        onExpand={() => setExpanded(!expanded)}
        ariaLabel={t("eqa.provider.schemes.expand", "Show cycles")}
      >
        <TableCell>{scheme.name}</TableCell>
        <TableCell>{scheme.provider || "—"}</TableCell>
        <TableCell>
          {scheme.schemeType
            ? t(
                `eqa.scheme.type.${scheme.schemeType.toLowerCase()}`,
                scheme.schemeType.replace(/_/g, " "),
              )
            : "—"}
        </TableCell>
        <TableCell>{scheme.discipline || "—"}</TableCell>
        <TableCell>{scheme.enrolledParticipantCount}</TableCell>
        <TableCell>{scheme.activeCycleCount}</TableCell>
        <TableCell>
          {scheme.lastDistribution ? scheme.lastDistribution.slice(0, 10) : "—"}
        </TableCell>
        <TableCell>
          <Button kind="tertiary" size="sm" onClick={onNewCycle}>
            {t("eqa.provider.schemes.newCycle", "New cycle")}
          </Button>
        </TableCell>
      </TableExpandRow>
      {/* Mounted only while open: Carbon leaves an always-rendered expanded row's
          inner container at max-height 0, and the cycle table then paints over
          the scheme row above it — covering its own links. */}
      {expanded && (
        <TableExpandedRow colSpan={9}>
          {cycles.length === 0 ? (
            <span style={hintStyle}>
              {t(
                "eqa.provider.schemes.noCycles",
                "No cycles yet. Start one to define its panel and participants.",
              )}
            </span>
          ) : (
            <Table size="sm">
              <TableHead>
                <TableRow>
                  <TableHeader>{t("eqa.col.cycle", "Cycle")}</TableHeader>
                  <TableHeader>{t("label.status", "Status")}</TableHeader>
                  <TableHeader>
                    {t("eqa.prep.participants", "Participants")}
                  </TableHeader>
                  <TableHeader>{t("eqa.prep.panels", "Panels")}</TableHeader>
                  <TableHeader>
                    {t("eqa.cycle.distributionMethod", "Distribution method")}
                  </TableHeader>
                </TableRow>
              </TableHead>
              <TableBody>
                {cycles.map((cycle) => (
                  <TableRow key={cycle.id}>
                    <TableCell>
                      <RouterLink
                        to={`/qa/eqa/provider/cycles/${cycle.id}/workbench`}
                      >
                        {cycle.cycleName || `#${cycle.cycleNumber}`}
                      </RouterLink>
                    </TableCell>
                    <TableCell>
                      <CycleStatusTag status={cycle.status} />
                    </TableCell>
                    <TableCell>{cycle.participantCount}</TableCell>
                    <TableCell>{cycle.panelCount}</TableCell>
                    <TableCell>
                      {cycle.distributionMethod
                        ? t(
                            `eqa.cycle.distributionMethod.${cycle.distributionMethod.toLowerCase()}`,
                            cycle.distributionMethod,
                          )
                        : "—"}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </TableExpandedRow>
      )}
    </>
  );
};

export default ProviderSchemeList;
