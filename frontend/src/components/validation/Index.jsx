import React, { useCallback, useContext, useRef, useState } from "react";
import SearchForm from "./SearchForm";
import Validation from "./Validation";
import { AlertDialog } from "../common/CustomNotification";
import { NotificationContext } from "../layout/Layout";
import { Heading, Grid, Column, Section } from "@carbon/react";
import { FormattedMessage } from "react-intl";
import PageBreadCrumb from "../common/PageBreadCrumb";
import { serverPageSizeOf } from "../utils/serverPaging";

let breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "sidenav.label.validation", link: "/validation" },
];

const Index = () => {
  const { notificationVisible } = useContext(NotificationContext);
  const [results, setResults] = useState({ resultList: [] });
  // The rows a full server page holds, read off the responses; Carbon's items
  // per page is pinned to it so Carbon's page is the server's page.
  const [serverPageSize, setServerPageSize] = useState();
  const [params, setParams] = useState("");
  const refresh = useRef(null);
  const registerRefresh = useCallback((run) => {
    refresh.current = run;
  }, []);
  const refreshResults = useCallback(
    (pageToReopen) => refresh.current?.(pageToReopen),
    [],
  );
  const pageLoader = useRef(null);
  const registerPageLoader = useCallback((run) => {
    pageLoader.current = run;
  }, []);
  const loadPage = useCallback(
    (pageNumber) => pageLoader.current?.(pageNumber),
    [],
  );
  const receiveResults = useCallback((next) => {
    setResults(next);
    setServerPageSize((previous) =>
      serverPageSizeOf(next?.paging, next?.resultList?.length ?? 0, previous),
    );
  }, []);
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
          setResults={receiveResults}
          registerRefresh={registerRefresh}
          registerPageLoader={registerPageLoader}
        />
        <Validation
          params={params}
          results={results}
          refreshResults={refreshResults}
          serverPageSize={serverPageSize}
          loadPage={loadPage}
        />
      </div>
    </>
  );
};

export default Index;
