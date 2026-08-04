import React from "react";
import {
  Column,
  Grid,
  Heading,
  Section,
  Tab,
  TabList,
  Tabs,
} from "@carbon/react";
import { useIntl } from "react-intl";
import {
  Redirect,
  useHistory,
  useParams,
  useRouteMatch,
} from "react-router-dom";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import AstPanelPage from "./AstPanelPage";
import BreakpointPage from "./BreakpointPage";
import { REFERENCE_DEFINITIONS } from "./definitions";
import ReferenceDataPage from "./ReferenceDataPage";
import {
  DEFAULT_MICROBIOLOGY_REFERENCE_SECTION,
  isKnownSection,
  MICROBIOLOGY_REFERENCE_SECTIONS,
  sectionPath,
} from "./sectionConfig";
import { useReferenceQuery } from "./useReferenceQuery";
import "./microbiologyReference.scss";

const MicrobiologyReferenceAdmin = () => {
  const intl = useIntl();
  const history = useHistory();
  const { section, detailId } = useParams();
  const { url } = useRouteMatch();
  const { query, setQuery } = useReferenceQuery();

  if (!isKnownSection(section)) {
    const basePath = url.split("/MicrobiologyReference")[0];
    return (
      <Redirect
        to={sectionPath(basePath, DEFAULT_MICROBIOLOGY_REFERENCE_SECTION)}
      />
    );
  }

  const selectedIndex = MICROBIOLOGY_REFERENCE_SECTIONS.findIndex(
    (entry) => entry.key === section,
  );
  const basePath = url.split("/MicrobiologyReference")[0];
  const breadcrumbs = [
    { label: "home.label", link: "/Dashboard" },
    { label: "breadcrums.admin.managment", link: basePath },
    {
      label: "microbiology.admin.title",
      link: sectionPath(basePath, DEFAULT_MICROBIOLOGY_REFERENCE_SECTION),
      isCurrentPage: !detailId,
    },
  ];
  if (detailId) {
    breadcrumbs.push({
      label: "microbiology.admin.breakpoints.detail",
      link: sectionPath(basePath, section, detailId),
      isCurrentPage: true,
    });
  }

  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Grid fullWidth className="microbiology-admin">
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Heading>
              {intl.formatMessage({ id: "microbiology.admin.title" })}
            </Heading>
          </Section>
          <Tabs
            selectedIndex={selectedIndex}
            onChange={({ selectedIndex: nextIndex }) => {
              const nextSection = MICROBIOLOGY_REFERENCE_SECTIONS[nextIndex];
              history.push(sectionPath(basePath, nextSection.key));
            }}
          >
            <TabList
              contained
              aria-label={intl.formatMessage({
                id: "microbiology.admin.sections",
              })}
            >
              {MICROBIOLOGY_REFERENCE_SECTIONS.map((entry) => (
                <Tab key={entry.key}>
                  {intl.formatMessage({ id: entry.label })}
                </Tab>
              ))}
            </TabList>
          </Tabs>
        </Column>
        <Column lg={16} md={8} sm={4}>
          {REFERENCE_DEFINITIONS[section] && (
            <ReferenceDataPage
              definition={REFERENCE_DEFINITIONS[section]}
              query={query}
              setQuery={setQuery}
            />
          )}
          {section === "ast-panels" && (
            <AstPanelPage query={query} setQuery={setQuery} />
          )}
          {section === "breakpoints" && (
            <BreakpointPage
              standardId={detailId}
              basePath={basePath}
              query={query}
              setQuery={setQuery}
            />
          )}
        </Column>
      </Grid>
    </>
  );
};

export default MicrobiologyReferenceAdmin;
