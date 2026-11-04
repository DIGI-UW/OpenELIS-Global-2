import React, { useCallback, useContext, useRef, useState } from "react";
import SearchForm from "./SearchForm";
import Validation from "./Validation";
import { AlertDialog } from "../common/CustomNotification";
import { NotificationContext } from "../layout/Layout";
import { Heading, Grid, Column, Section } from "@carbon/react";
import { FormattedMessage } from "react-intl";
import PageBreadCrumb from "../common/PageBreadCrumb";
import { serverPageSizeOf } from "../utils/serverPaging";
import ExtensionSlot, { useSlot } from "../addons/ExtensionSlot";
import { SLOT_RESULT_VALIDATION } from "../addons/slots";

let breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "sidenav.label.validation", link: "/validation" },
];

/** Lab numbers of the rows the page currently holds, at most 50. */
export const accessionsOnScreen = (results) =>
  Array.from(
    new Set(
      (results?.resultList || []).map((r) => r.accessionNumber).filter(Boolean),
    ),
  ).slice(0, 50);

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
  // Add-on side panel (result-validation-side-panel-slot); none: layout unchanged.
  const hasAddons = useSlot(SLOT_RESULT_VALIDATION).length > 0;
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
      {notificationVisible === true ? <AlertDialog /> : ""}
      <SearchForm
        setParams={setParams}
        setResults={receiveResults}
        registerRefresh={registerRefresh}
        registerPageLoader={registerPageLoader}
      />
      <Grid fullWidth={true}>
        <Column lg={hasAddons ? 12 : 16} md={8} sm={4}>
          <Validation
            params={params}
            results={results}
            refreshResults={refreshResults}
            serverPageSize={serverPageSize}
            loadPage={loadPage}
          />
        </Column>
        {hasAddons && (
          <Column lg={4} md={8} sm={4}>
            <ExtensionSlot
              name={SLOT_RESULT_VALIDATION}
              state={{
                page: "result-validation",
                accessionNumbers: accessionsOnScreen(results),
              }}
            />
          </Column>
        )}
      </Grid>
    </>
  );
};

export default Index;
