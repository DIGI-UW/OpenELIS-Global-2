import React, { useContext } from "react";
import { FormattedMessage } from "react-intl";

import { Column, Grid, Heading, Section } from "@carbon/react";
import SearchOrder from "./SearchOrder";
import { AlertDialog } from "../common/CustomNotification";
import { NotificationContext } from "../layout/Layout";
import PageBreadCrumb from "../common/PageBreadCrumb";
let breadcrumbs = [{ label: "home.label", link: "/" }];

const Index = () => {
  const { notificationVisible } = useContext(NotificationContext);
  return (
    <div className="pageContent">
      {notificationVisible === true ? <AlertDialog /> : ""}
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Section>
              <Heading>
                <FormattedMessage id="order.label.modify" />
              </Heading>
            </Section>
          </Section>
        </Column>
      </Grid>
      <SearchOrder />
    </div>
  );
};

export default Index;
