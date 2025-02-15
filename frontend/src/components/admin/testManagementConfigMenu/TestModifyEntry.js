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
    label: "configuration.test.modify",
    link: "/MasterListsPage#TestModifyEntry",
  },
];

function TestModifyEntry() {
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
                <FormattedMessage id="configuration.test.modify" />
              </Heading>
            </Section>
          </Column>
        </Grid>
      </div>
    </>
  );
}

export default injectIntl(TestModifyEntry);
