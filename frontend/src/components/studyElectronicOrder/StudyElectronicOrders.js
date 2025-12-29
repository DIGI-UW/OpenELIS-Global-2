import React, { useRef, useState } from "react";
import StudyEOrderSearch from "./StudyEOrderSearch";
import StudyEOrder from "./StudyEOrder";
import PageBreadCrumb from "../common/PageBreadCrumb";
import { Grid, Column, Section, Heading } from "@carbon/react";
import { FormattedMessage } from "react-intl";

const breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "banner.menu.sampleCreate", link: "#" },
  { label: "banner.menu.study.eorders", link: "/StudyElectronicOrders" },
];

const StudyElectronicOrders = () => {
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
                <FormattedMessage
                  id="eorder.study.header"
                  defaultMessage="Study Electronic Orders"
                />
              </Heading>
            </Section>
          </Section>
        </Column>
      </Grid>
      <div className="orderLegendBody">
        <Grid fullWidth={true}>
          <StudyEOrderSearch setEOrders={setEOrders} eOrderRef={eOrderRef} />
        </Grid>
        <StudyEOrder
          eOrderRef={eOrderRef}
          eOrders={eOrders}
          setEOrders={setEOrders}
        />
      </div>
    </>
  );
};

export default StudyElectronicOrders;
