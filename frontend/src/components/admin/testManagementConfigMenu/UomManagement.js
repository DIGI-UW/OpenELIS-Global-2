import {
  ClickableTile,
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
    label: "configuration.uom.manage",
    link: "/MasterListsPage#UomManagement",
  },
];

function UomManagement() {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  const intl = useIntl();


  return (
    <>
      {notificationVisible === true ? <AlertDialog /> : ""}
      <div className="adminPageContent">
        <PageBreadCrumb breadcrumbs={breadcrumbs} />
        <div className="orderLegendBody">
          <Grid fullWidth={true}>
            <Column lg={16} md={8} sm={4}>
              <Section>
                <Heading>
                  <FormattedMessage id="configuration.uom.manage" />
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
                <ClickableTile>
                  <ListItem
                    onClick={() => {
                      window.location.assign("/MasterListsPage#UomCreate");
                    }}
                  >
                    <FormattedMessage id="configuration.uom.create" />
                  </ListItem>
                </ClickableTile>
              </UnorderedList>
            </Column>
          </Grid>
        </div>
      </div>
    </>
  );
}

export default injectIntl(UomManagement);
