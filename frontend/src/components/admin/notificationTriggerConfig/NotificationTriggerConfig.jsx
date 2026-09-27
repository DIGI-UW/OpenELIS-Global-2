import React from "react";
import {
  Tabs,
  TabList,
  Tab,
  TabPanels,
  TabPanel,
  Grid,
  Column,
} from "@carbon/react";
import { FormattedMessage, injectIntl, useIntl } from "react-intl";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import TriggersTab from "./tabs/TriggersTab";
import TemplatesTab from "./tabs/TemplatesTab";
import SentMessagesTab from "./tabs/SentMessagesTab";

const breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
  {
    label: "notificationtrigger.config.title",
    link: "/MasterListsPage/notificationTriggerConfig",
  },
];

function NotificationTriggerConfig() {
  const intl = useIntl();

  return (
    <div className="adminPageContent">
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <h2 style={{ marginTop: "1rem" }}>
            <FormattedMessage id="notificationtrigger.config.title" />
          </h2>
          <p style={{ color: "#525252", marginBottom: "1rem" }}>
            <FormattedMessage id="notificationtrigger.config.subtitle" />
          </p>
        </Column>
      </Grid>
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <Tabs>
            <TabList
              aria-label={intl.formatMessage({
                id: "notificationtrigger.config.title",
              })}
            >
              <Tab>
                <FormattedMessage id="notificationtrigger.tabs.triggers" />
              </Tab>
              <Tab>
                <FormattedMessage id="notificationtrigger.tabs.templates" />
              </Tab>
              <Tab>
                <FormattedMessage id="notificationtrigger.tabs.sentmessages" />
              </Tab>
            </TabList>
            <TabPanels>
              <TabPanel>
                <TriggersTab />
              </TabPanel>
              <TabPanel>
                <TemplatesTab />
              </TabPanel>
              <TabPanel>
                <SentMessagesTab />
              </TabPanel>
            </TabPanels>
          </Tabs>
        </Column>
      </Grid>
    </div>
  );
}

export default injectIntl(NotificationTriggerConfig);
