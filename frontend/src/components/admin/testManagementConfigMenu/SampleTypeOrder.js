import {
  Column,
  Grid,
  Heading,
  Section,
} from "@carbon/react";
import React, { useContext } from "react";

import { FormattedMessage, injectIntl, useIntl } from "react-intl";
import {
  AlertDialog,
} from "../../common/CustomNotification.js";
import PageBreadCrumb from "../../common/PageBreadCrumb.js";
import { NotificationContext } from "../../layout/Layout.js";

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
    label: "configuration.sampleType.order",
    link: "/MasterListsPage#SampleTypeOrder",
  },
];

function SampleTypeOrder() {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const intl = useIntl();


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
                <FormattedMessage id="configuration.sampleType.order.explain" />
              </Heading>
            </Section>
          </Column>
        </Grid>
      </div>
    </>
  );
}

export default injectIntl(SampleTypeOrder);
