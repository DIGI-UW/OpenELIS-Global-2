/* eslint-disable @typescript-eslint/no-unused-vars -- preserve unused declarations from the original JavaScript */
import React, { useContext, useRef } from "react";
import { Heading, Grid, Column, Section } from "@carbon/react";
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
    link: "/MasterListsPage/MethodManagement",
  },
  {
    label: "configuration.method.create",
    link: "/MasterListsPage/MethodCreate",
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

function MethodCreate() {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext) as NotificationContextValue;

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
                <FormattedMessage id="configuration.method.create" />
              </Heading>
            </Section>
          </Column>
        </Grid>
      </div>
    </>
  );
}

export default injectIntl(MethodCreate);
