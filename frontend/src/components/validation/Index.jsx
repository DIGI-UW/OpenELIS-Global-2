import React, { useContext, useState, useEffect } from "react";
import SearchForm from "./SearchForm";
import Validation from "./Validation";
import { AlertDialog } from "../common/CustomNotification";
import { NotificationContext } from "../layout/Layout";
import { Heading, Grid, Column, Section } from "@carbon/react";
import { injectIntl, FormattedMessage } from "react-intl";
import PageBreadCrumb from "../common/PageBreadCrumb";

let breadcrumbs = [{ label: "home.label", link: "/" }];

const Index = () => {
  const { notificationVisible } = useContext(NotificationContext);
  const [results, setResults] = useState({ resultList: [] });
  const [params, setParams] = useState("");
  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Section>
              <Heading>
                <FormattedMessage id="sidenav.label.validation" />
              </Heading>
            </Section>
          </Section>
        </Column>
      </Grid>
      <div className="orderLegendBody">
        {notificationVisible === true ? <AlertDialog /> : ""}
        <SearchForm setParams={setParams} setResults={setResults} />
        <Validation params={params} results={results} />
      </div>
    </>
  );
};

export default Index;
