import React, { useContext, useEffect, useState } from "react";
import { Grid, Column, Section,Heading } from "@carbon/react";
import { NotificationContext, ConfigurationContext } from "../../layout/Layout";
import { FormattedMessage } from "react-intl";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import { AlertDialog } from "../../common/CustomNotification";
import { useLocation } from "react-router-dom";
import InitialEntry from "./InitialEntry";

let breadcrumbs = [
  { label: "home.label", link: "/" },
];

const Index = () => {

    const queryParams = new URLSearchParams(useLocation().search);
    const type = queryParams.get("type"); 

  const { notificationVisible, setNotificationVisible, addNotification } = useContext(NotificationContext);
  const { configurationProperties } = useContext(ConfigurationContext);

  const components ={
    initial: <InitialEntry />
  }

  return (
    <div className="pageContent">
      {notificationVisible === true ? <AlertDialog /> : ""}
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Section>
              <Heading>
                <FormattedMessage id="patient.label.create" />
              </Heading>
            </Section>
          </Section>
        </Column>
      </Grid>
      {components[type]}
    </div>
  )
}

export default Index;