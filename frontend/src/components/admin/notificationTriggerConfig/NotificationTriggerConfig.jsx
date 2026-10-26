import React, { useContext, useEffect } from "react";
import { Redirect, useHistory, useParams } from "react-router-dom";
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
import { NotificationContext } from "../../layout/Layout";
import { AlertDialog } from "../../common/CustomNotification";
import TriggersTab from "./tabs/TriggersTab";
import TemplatesTab from "./tabs/TemplatesTab";
import ChannelsTab from "./tabs/ChannelsTab";
import DeliverySettingsTab from "./tabs/DeliverySettingsTab";

const breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
  {
    label: "sidenav.label.admin.notifications",
    link: "/MasterListsPage/notificationTriggerConfig",
  },
];

/** Each tab is a route, so the SideNav and a bookmark can open one directly. */
export const NOTIFICATION_TABS = [
  "triggers",
  "templates",
  "channels",
  "delivery",
];

/**
 * Only the selected tab is mounted, so each tab loads its data when opened
 * and the page shows one toast for whichever tab raised it.
 */
function NotificationTriggerConfig() {
  const intl = useIntl();
  const history = useHistory();
  const { tab } = useParams();
  const { notificationVisible } = useContext(NotificationContext);
  const selectedIndex = Math.max(
    0,
    NOTIFICATION_TABS.indexOf(tab || "triggers"),
  );

  useEffect(() => {
    if (tab && tab !== "sent" && !NOTIFICATION_TABS.includes(tab)) {
      history.replace("/MasterListsPage/notificationTriggerConfig/triggers");
    }
  }, [tab, history]);

  if (tab === "sent") {
    return <Redirect to="/SentMessages?product=trigger" />;
  }

  return (
    <div className="adminPageContent">
      {notificationVisible === true ? <AlertDialog /> : ""}
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <h2 style={{ marginTop: "1rem" }}>
            <FormattedMessage id="sidenav.label.admin.notifications" />
          </h2>
          <p style={{ color: "#525252", marginBottom: "1rem" }}>
            <FormattedMessage id="notificationtrigger.config.subtitle" />
          </p>
        </Column>
      </Grid>
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <Tabs
            selectedIndex={selectedIndex}
            onChange={({ selectedIndex: next }) =>
              history.push(
                `/MasterListsPage/notificationTriggerConfig/${NOTIFICATION_TABS[next]}`,
              )
            }
          >
            <TabList
              aria-label={intl.formatMessage({
                id: "sidenav.label.admin.notifications",
              })}
            >
              <Tab>
                <FormattedMessage id="sidenav.label.admin.notifications.triggers" />
              </Tab>
              <Tab>
                <FormattedMessage id="sidenav.label.admin.notifications.templates" />
              </Tab>
              <Tab>
                <FormattedMessage id="sidenav.label.admin.notifications.channels" />
              </Tab>
              <Tab>
                <FormattedMessage id="sidenav.label.admin.notifications.delivery" />
              </Tab>
            </TabList>
            <TabPanels>
              <TabPanel>{selectedIndex === 0 && <TriggersTab />}</TabPanel>
              <TabPanel>{selectedIndex === 1 && <TemplatesTab />}</TabPanel>
              <TabPanel>{selectedIndex === 2 && <ChannelsTab />}</TabPanel>
              <TabPanel>
                {selectedIndex === 3 && <DeliverySettingsTab />}
              </TabPanel>
            </TabPanels>
          </Tabs>
        </Column>
      </Grid>
    </div>
  );
}

export default injectIntl(NotificationTriggerConfig);
