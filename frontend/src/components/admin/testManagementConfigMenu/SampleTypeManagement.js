import {
  Column,
  Grid,
  Heading,
  ListItem,
  Section,
  UnorderedList,
} from "@carbon/react";
import React, { useContext } from "react";

import { FormattedMessage, injectIntl, useIntl } from "react-intl";
import {
  AlertDialog
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
];

function TestSectionManagement() {
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
                <FormattedMessage id="configuration.sampleType.manage" />
              </Heading>
            </Section>
          </Column>
        </Grid>
        <br />
        <hr />
        <br />
        <Grid fullWidth={true}>
          <Column lg={16} md={8} sm={4}>
            <UnorderedList>
              <ListItem
                onClick={() => {
                  window.location.assign("/MasterListsPage#SampleTypeCreate");
                }}
              >
                <FormattedMessage id="configuration.sampleType.create" />
              </ListItem>
              <br />
              <ListItem
                onClick={() => {
                  window.location.assign("/MasterListsPage#SampleTypeOrder");
                }}
              >
                <FormattedMessage id="configuration.sampleType.order" />
              </ListItem>
              <br />
              <ListItem
                onClick={() => {
                  window.location.assign(
                    "/MasterListsPage#SampleTypeTestAssign",
                  );
                }}
              >
                <FormattedMessage id="configuration.panel.assign" />
              </ListItem>
            </UnorderedList>
          </Column>
        </Grid>
      </div>
    </>
  );
}

export default injectIntl(TestSectionManagement);
