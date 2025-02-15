import React, { useContext, useRef } from "react";
import {
  Heading,
  Grid,
  Column,
  Section,
  UnorderedList,
  ListItem,
} from "@carbon/react";

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
    label: "configuration.testUnit.manage",
    link: "/MasterListsPage#TestSectionManagement",
  },
];

function TestSectionManagement() {
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
                <FormattedMessage id="configuration.testUnit.manage" />
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
                  window.location.assign("/MasterListsPage#TestSectionCreate");
                }}
              >
                <FormattedMessage id="configuration.method.create" />
              </ListItem>
              <br />
              <ListItem
                onClick={() => {
                  window.location.assign("/MasterListsPage#TestSectionOrder");
                }}
              >
                <FormattedMessage id="configuration.testUnit.order" />
              </ListItem>
              <br />
              <ListItem
                onClick={() => {
                  window.location.assign(
                    "/MasterListsPage#TestSectionTestAssign",
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
