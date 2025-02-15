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
    label: "configuration.panel.manage",
    link: "/MasterListsPage#PanelManagement",
  },
];

function PanelManagement() {
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
                <FormattedMessage id="configuration.panel.manage" />
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
                  window.location.assign("/MasterListsPage#PanelCreate");
                }}
              >
                <FormattedMessage id="configuration.panel.create" />
              </ListItem>
              <br />
              <ListItem
                onClick={() => {
                  window.location.assign("/MasterListsPage#PanelOrder");
                }}
              >
                <FormattedMessage id="configuration.panel.order" />
              </ListItem>
              <br />
              <ListItem
                onClick={() => {
                  window.location.assign("/MasterListsPage#PanelTestAssign");
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

export default injectIntl(PanelManagement);
