import React, { useContext, useEffect, useState } from "react";
import InitialEntry from "./InitialEntry";
import { Grid, Column, Section,Heading } from "@carbon/react";
import { NotificationContext, ConfigurationContext } from "../layout/Layout";
import { FormattedMessage } from "react-intl";
import PageBreadCrumb from "../common/PageBreadCrumb";
import { AlertDialog } from "../common/CustomNotification";
let breadcrumbs = [
  { label: "home.label", link: "/" },
];

const Index = () => {

  const { notificationVisible, setNotificationVisible, addNotification } = useContext(NotificationContext);
  const { configurationProperties } = useContext(ConfigurationContext);

  return (
    <div className="pageContent">
      {notificationVisible === true ? <AlertDialog /> : ""}
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Section>
              <Heading>
                <FormattedMessage id="sidenav.label.study.initialentry" />
              </Heading>
            </Section>
          </Section>
        </Column>
      </Grid>
      <InitialEntry />
    </div>
  )
}

export default Index;