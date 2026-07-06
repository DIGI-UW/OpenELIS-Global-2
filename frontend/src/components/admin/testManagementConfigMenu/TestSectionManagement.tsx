/* eslint-disable @typescript-eslint/no-unused-vars -- preserve unused declarations from the original JavaScript */
import React, { useContext, useRef } from "react";
import {
  Heading,
  Grid,
  Column,
  Section,
  UnorderedList,
  ClickableTile,
} from "@carbon/react";
import { NotificationContext } from "../../layout/Layout";
import { AlertDialog } from "../../common/CustomNotification";
import { FormattedMessage, injectIntl, useIntl } from "react-intl";
import PageBreadCrumb from "../../common/PageBreadCrumb";

// eslint-disable-next-line prefer-const -- preserve the original JavaScript let declaration
let breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
  {
    label: "master.lists.page.test.management",
    link: "/MasterListsPage/testManagementConfigMenu",
  },
  {
    label: "configuration.testUnit.manage",
    link: "/MasterListsPage/TestSectionManagement",
  },
];

interface NotificationContextValue {
  notificationVisible: boolean;
  setNotificationVisible: (visible: boolean) => void;
  addNotification: (notification: {
    kind: string;
    title: string;
    message: string;
  }) => void;
}

function TestSectionManagement() {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext) as NotificationContextValue;

  const intl = useIntl();

  const componentMounted = useRef(false);

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
                <ClickableTile
                  id="TestSectionCreate"
                  href="/MasterListsPage/TestSectionCreate"
                >
                  <FormattedMessage id="configuration.testUnit.create" />
                </ClickableTile>
                <br />
                <ClickableTile
                  id="TestSectionOrder"
                  href="/MasterListsPage/TestSectionOrder"
                >
                  <FormattedMessage id="configuration.testUnit.order" />
                </ClickableTile>
                <br />
                <ClickableTile
                  href="/MasterListsPage/TestSectionTestAssign"
                  id="TestSectionTestAssign"
                >
                  <FormattedMessage id="configuration.panel.assign" />
                </ClickableTile>
              </UnorderedList>
            </Column>
          </Grid>
        </div>
      </div>
    </>
  );
}

export default injectIntl(TestSectionManagement);
