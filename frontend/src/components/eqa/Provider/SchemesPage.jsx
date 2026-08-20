import React, { useEffect, useState } from "react";
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
  TableExpandHeader,
  TableExpandRow,
  TableExpandedRow,
  TableHead,
  TableHeader,
  TableRow,
  Tag,
} from "@carbon/react";
import { useIntl } from "react-intl";
import { Link as RouterLink, useHistory } from "react-router-dom";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import { CycleStatusTag, hintStyle } from "../eqaCommon";
import { fetchProviderSchemes } from "./providerApi";
import { fetchProviderCycles } from "./Workbench/workbenchApi";

const breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "banner.menu.eqa.provider", link: "/qa/eqa/provider/schemes" },
];

/**
 * FR-V2.5-01: the schemes this laboratory runs for others, each expanding to its
 * cycles. Scheme records themselves are still created and edited on EQA
 * Management → Programs (V1, absorbed by T-31) — this page is the provider's way
 * in to running a round, not a second CRUD screen for the same table.
 */
const SchemesPage = () => {
  const intl = useIntl();
  const history = useHistory();
  const t = (id, defaultMessage, values) =>
    intl.formatMessage({ id, defaultMessage }, values);

  const [schemes, setSchemes] = useState([]);
  const [cycles, setCycles] = useState([]);
  const [expanded, setExpanded] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchProviderSchemes((data) => {
      setSchemes(data);
      setLoading(false);
    });
    fetchProviderCycles(setCycles);
  }, []);

  if (loading) {
    return <Loading />;
  }

  const cyclesOf = (schemeId) =>
    cycles.filter((cycle) => String(cycle.schemeId) === String(schemeId));

  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Grid fullWidth>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Heading>
              {t("eqa.provider.schemes.title", "Schemes & programs")}
            </Heading>
            <p style={hintStyle}>
              {t(
                "eqa.provider.schemes.help",
                "Schemes this laboratory provides to participant laboratories. Expand a scheme to see its cycles.",
              )}
            </p>
          </Section>
          {schemes.length === 0 ? (
            <InlineNotification
              kind="info"
              lowContrast
              hideCloseButton
              title={t("eqa.provider.schemes.none.title", "No schemes yet")}
              subtitle={t(
                "eqa.provider.schemes.none.body",
                "Create a scheme on EQA Management, then enrol the participant laboratories that take part in it.",
              )}
            />
          ) : (
            <Table size="sm" useZebraStyles>
              <TableHead>
                <TableRow>
                  <TableExpandHeader
                    aria-label={t("eqa.provider.schemes.expand", "Cycles")}
                  />
                  <TableHeader>
                    {t("eqa.provider.scheme", "Scheme")}
                  </TableHeader>
                  <TableHeader>
                    {t("eqa.program.provider", "Provider")}
                  </TableHeader>
                  <TableHeader>
                    {t("eqa.scheme.type", "Scheme type")}
                  </TableHeader>
                  <TableHeader>
                    {t("eqa.prep.participants", "Participants")}
                  </TableHeader>
                  <TableHeader>{t("eqa.prep.cycles", "Cycles")}</TableHeader>
                  <TableHeader>{t("label.actions", "Actions")}</TableHeader>
                </TableRow>
              </TableHead>
              <TableBody>
                {schemes.map((scheme) => {
                  const schemeCycles = cyclesOf(scheme.id);
                  const isExpanded = String(expanded) === String(scheme.id);
                  const enrolled = Number(scheme.participantCount) || 0;
                  return (
                    <React.Fragment key={scheme.id}>
                      <TableExpandRow
                        isExpanded={isExpanded}
                        onExpand={() =>
                          setExpanded(isExpanded ? null : scheme.id)
                        }
                        ariaLabel={t("eqa.prep.cycles", "Cycles")}
                      >
                        <TableCell>{scheme.name}</TableCell>
                        <TableCell>{scheme.provider || "—"}</TableCell>
                        <TableCell>
                          <Tag type="cool-gray" size="sm">
                            {t(
                              `eqa.scheme.type.${(scheme.schemeType || "").toLowerCase()}`,
                              (scheme.schemeType || "").replace(/_/g, " "),
                            )}
                          </Tag>
                        </TableCell>
                        <TableCell>{enrolled}</TableCell>
                        <TableCell>{schemeCycles.length}</TableCell>
                        <TableCell>
                          {enrolled === 0 ? (
                            <RouterLink to="/qa/eqa/participants">
                              {t(
                                "eqa.provider.schemes.enrolFirst",
                                "Enrol participants",
                              )}
                            </RouterLink>
                          ) : (
                            <Button
                              kind="tertiary"
                              size="sm"
                              onClick={() =>
                                history.push(
                                  `/qa/eqa/provider/cycles/new?schemeId=${scheme.id}`,
                                )
                              }
                            >
                              {t("eqa.provider.cycle.new", "New cycle")}
                            </Button>
                          )}
                        </TableCell>
                      </TableExpandRow>
                      {isExpanded && (
                        <TableExpandedRow colSpan={7}>
                          {schemeCycles.length === 0 ? (
                            <p style={hintStyle}>
                              {t(
                                "eqa.provider.schemes.noCycles",
                                "No cycles yet for this scheme.",
                              )}
                            </p>
                          ) : (
                            <Table size="sm">
                              <TableHead>
                                <TableRow>
                                  <TableHeader>
                                    {t("eqa.provider.cycle", "Cycle")}
                                  </TableHeader>
                                  <TableHeader>
                                    {t("label.status", "Status")}
                                  </TableHeader>
                                  <TableHeader>
                                    {t("eqa.prep.panels", "Panels")}
                                  </TableHeader>
                                  <TableHeader>
                                    {t(
                                      "eqa.cycle.distributionMethod",
                                      "Distribution",
                                    )}
                                  </TableHeader>
                                </TableRow>
                              </TableHead>
                              <TableBody>
                                {schemeCycles.map((cycle) => (
                                  <TableRow key={cycle.id}>
                                    <TableCell>
                                      <RouterLink
                                        to={`/qa/eqa/provider/cycles/${cycle.id}/workbench`}
                                      >
                                        {cycle.cycleName ||
                                          `#${cycle.cycleNumber}`}
                                      </RouterLink>
                                    </TableCell>
                                    <TableCell>
                                      <CycleStatusTag status={cycle.status} />
                                    </TableCell>
                                    <TableCell>{cycle.panelCount}</TableCell>
                                    <TableCell>
                                      {cycle.distributionMethod || "—"}
                                    </TableCell>
                                  </TableRow>
                                ))}
                              </TableBody>
                            </Table>
                          )}
                        </TableExpandedRow>
                      )}
                    </React.Fragment>
                  );
                })}
              </TableBody>
            </Table>
          )}
        </Column>
      </Grid>
    </>
  );
};

export default SchemesPage;
