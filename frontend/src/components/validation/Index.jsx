import React, { useCallback, useContext, useRef, useState } from "react";
import SearchForm from "./SearchForm";
import Validation from "./Validation";
import { AlertDialog } from "../common/CustomNotification";
import { NotificationContext } from "../layout/contexts";
import { Heading, Grid, Column, Section } from "@carbon/react";
import { FormattedMessage } from "react-intl";
import PageBreadCrumb from "../common/PageBreadCrumb";

let breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "sidenav.label.validation", link: "/validation" },
];

const Index = () => {
  const { notificationVisible } = useContext(NotificationContext);
  const [results, setResults] = useState({ resultList: [] });
  const [params, setParams] = useState("");
  const refresh = useRef(null);
  const registerRefresh = useCallback((run) => {
    refresh.current = run;
  }, []);
  const refreshResults = useCallback(() => refresh.current?.(), []);
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
        <SearchForm
          setParams={setParams}
          setResults={setResults}
          registerRefresh={registerRefresh}
        />
        <Validation
          params={params}
          results={results}
          refreshResults={refreshResults}
        />
      </div>
    </>
  );
};

export default Index;
