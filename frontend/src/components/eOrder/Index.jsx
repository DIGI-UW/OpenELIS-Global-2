import { React, useCallback, useRef, useState } from "react";
import EOrderSearch from "./EOrderSearch";
import EOrder from "./EOrder";
import PageBreadCrumb from "../common/PageBreadCrumb";
import { Column, Grid, Section, Heading } from "@carbon/react";
import { FormattedMessage } from "react-intl";
import { serverPageSizeOf } from "../utils/serverPaging";
let breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "eorder.header", link: "/ElectronicOrders" },
];

export { default as EOrderSearch } from "./EOrderSearch";
export { default as EOrder } from "./EOrder";

const EOrderPage = () => {
  const eOrderRef = useRef(null);
  const [eOrders, setEOrders] = useState([]);
  // The server's page announcement for the list shown, and the rows a full
  // server page holds; Carbon's items per page is pinned to the latter so
  // Carbon's page is the server's page.
  const [paging, setPaging] = useState();
  const [serverPageSize, setServerPageSize] = useState();
  const pageLoader = useRef(null);
  const registerPageLoader = useCallback((run) => {
    pageLoader.current = run;
  }, []);
  const loadPage = useCallback(
    (pageNumber) => pageLoader.current?.(pageNumber),
    [],
  );
  const receivePage = useCallback((response) => {
    setPaging(response?.paging);
    setServerPageSize((previous) =>
      serverPageSizeOf(
        response?.paging,
        response?.eOrders?.length ?? 0,
        previous,
      ),
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
                <FormattedMessage id="eorder.header" />
              </Heading>
            </Section>
          </Section>
        </Column>
      </Grid>
      <div className="orderLegendBody">
        <Grid fullWidth={true}>
          <EOrderSearch
            setEOrders={setEOrders}
            eOrderRef={eOrderRef}
            onPageReceived={receivePage}
            registerPageLoader={registerPageLoader}
          />
        </Grid>
        <EOrder
          eOrderRef={eOrderRef}
          eOrders={eOrders}
          setEOrders={setEOrders}
          paging={paging}
          serverPageSize={serverPageSize}
          loadPage={loadPage}
        />
      </div>
    </>
  );
};

export default EOrderPage;
