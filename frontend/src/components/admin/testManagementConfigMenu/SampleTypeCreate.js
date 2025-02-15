import React, { useContext, useRef } from "react";
import { Heading, Grid, Column, Section } from "@carbon/react";

import { NotificationContext } from "../../layout/Layout.js";
import { AlertDialog } from "../../common/CustomNotification.js";
import { FormattedMessage, injectIntl, useIntl } from "react-intl";
import PageBreadCrumb from "../../common/PageBreadCrumb.js";

let breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
  {
    label: "master.lists.page.test.management",
    link: "/MasterListsPage#testManagementConfigMenu",
  },
  {
    label: "configuration.sampleType.manage",
    link: "/MasterListsPage#SampleTypeManagement",
  },
  {
    label: "configuration.sampleType.create",
    link: "/MasterListsPage#SampleTypeCreate",
  },
];

function SampleTypeCreate() {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const intl = useIntl();

  const componentMounted = useRef(false);

  return (
    <>
      {notificationVisible === true ? <AlertDialog /> : ""}
      <div className="adminPageContent">
        <PageBreadCrumb breadcrumbs={breadcrumbs} />
        <Grid fullWidth={true}>
          <Column lg={16} md={8} sm={4}>
            <Section>
              <Heading>
                <FormattedMessage id="banner.menu.patientEdit" />
              </Heading>
            </Section>
          </Column>
        </Grid>
        <br />
        <hr />
        <br />
        <Grid fullWidth={true}>
          <Column lg={16} md={8} sm={4}>
            <Section>
              <Heading>
                <FormattedMessage id="configuration.sampleType.create" />
              </Heading>
            </Section>
          </Column>
        </Grid>
      </div>
    </>
  );
}

export default injectIntl(SampleTypeCreate);
