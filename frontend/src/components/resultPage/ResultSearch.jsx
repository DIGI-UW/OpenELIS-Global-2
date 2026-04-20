import React, { useEffect, useState } from "react";
import "../Style.css";
import { injectIntl, FormattedMessage, useIntl } from "react-intl";
import ResultSearchPage from "./SearchResultForm";
import { Heading, Grid, Column, Section } from "@carbon/react";
import PageBreadCrumb from "../common/PageBreadCrumb";

function ResultSearch() {
  const [source, setSource] = useState("");
  useEffect(() => {
    let sourceFromUrl = new URLSearchParams(window.location.search).get(
      "source",
    );
    let sources = [
      "WorkPlanByTest",
      "WorkPlanByPanel",
      "WorkPlanByTestSection",
      "WorkPlanByPriority",
    ];
    sourceFromUrl = sources.includes(sourceFromUrl) ? sourceFromUrl : "";
    setSource(sourceFromUrl);
  }, []);
  return (
    <>
      <PageBreadCrumb
        breadcrumbs={
          source
            ? [
                { label: "home.label", link: "/" },
                {
                  label: "banner.menu.workplan",
                  link: `/${source}`,
                },
              ]
            : [{ label: "home.label", link: "/" }]
        }
      />

      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Section>
              <Heading>
                <FormattedMessage id="sidenav.label.results" />
              </Heading>
            </Section>
          </Section>
        </Column>
      </Grid>
      <div className="orderLegendBody">
        <ResultSearchPage />
      </div>
    </>
  );
}

export default injectIntl(ResultSearch);
