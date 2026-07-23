import { React, useRef, useState } from "react";
import EOrderSearch from "./EOrderSearch";
import EOrder from "./EOrder";
import PageBreadCrumb from "../common/PageBreadCrumb";
import { Column, Grid, Section, Heading } from "@carbon/react";
import { FormattedMessage } from "react-intl";
let breadcrumbs = [{ label: "home.label", link: "/" }];

export { default as EOrderSearch } from "./EOrderSearch";
export { default as EOrder } from "./EOrder";

const EOrderPage = () => {
  const eOrderRef = useRef(null);
  const [eOrders, setEOrders] = useState([]);
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
          <EOrderSearch setEOrders={setEOrders} eOrderRef={eOrderRef} />
        </Grid>
        <EOrder
          eOrderRef={eOrderRef}
          eOrders={eOrders}
          setEOrders={setEOrders}
        />
      </div>
    </>
  );
};

export default EOrderPage;
