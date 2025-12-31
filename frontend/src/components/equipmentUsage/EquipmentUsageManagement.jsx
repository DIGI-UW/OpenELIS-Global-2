import React, { useState } from "react";
import {
  Tabs,
  TabList,
  Tab,
  TabPanels,
  TabPanel,
  Grid,
  Column,
} from "@carbon/react";
import { FormattedMessage } from "react-intl";
import PageBreadCrumb from "../common/PageBreadCrumb";
import EquipmentManagement from "./EquipmentManagement";
import EquipmentUsageLog from "./EquipmentUsageLog";
import EquipmentUsageApproval from "./EquipmentUsageApproval";
import EquipmentUsageHistory from "./EquipmentUsageHistory";
import EquipmentUsageReports from "./EquipmentUsageReports";
import "./EquipmentUsage.css";

const breadcrumbs = [
  { label: "home.label", link: "/", defaultMessage: "Home" },
  {
    label: "sidenav.label.equipment.usage.log",
    link: "/equipment-usage",
    defaultMessage: "Equipment Usage Log",
  },
];

const EquipmentUsageManagement = () => {
  const [selectedTab, setSelectedTab] = useState(0);
  const [refreshTrigger, setRefreshTrigger] = useState(0);

  const triggerRefresh = () => {
    setRefreshTrigger((prev) => prev + 1);
  };

  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <div className="orderLegendBody">
            <h2>
              <FormattedMessage id="equipment.usage.title" defaultMessage="Equipment Usage Log" />
            </h2>

            <Tabs
              selectedIndex={selectedTab}
              onChange={({ selectedIndex }) => setSelectedTab(selectedIndex)}
            >
              <TabList aria-label="Equipment usage tabs" contained>
                <Tab>
                  <FormattedMessage id="equipment.tab.equipment" defaultMessage="Equipment" />
                </Tab>
                <Tab>
                  <FormattedMessage id="equipment.usage.tab.log" defaultMessage="Usage Log" />
                </Tab>
                <Tab>
                  <FormattedMessage id="equipment.usage.tab.history" defaultMessage="History" />
                </Tab>
                <Tab>
                  <FormattedMessage id="equipment.usage.tab.approval" defaultMessage="Approval" />
                </Tab>
                <Tab>
                  <FormattedMessage id="equipment.usage.tab.reports" defaultMessage="Reports" />
                </Tab>
              </TabList>

              <TabPanels>
                {/* Equipment Management Tab - Add/Edit equipment */}
                <TabPanel>
                  <EquipmentManagement />
                </TabPanel>

                {/* Usage Log Tab - Create/Edit entries */}
                <TabPanel>
                  <EquipmentUsageLog onEntrySubmitted={triggerRefresh} />
                </TabPanel>

                {/* History Tab - View all entries */}
                <TabPanel>
                  <EquipmentUsageHistory refreshTrigger={refreshTrigger} />
                </TabPanel>

                {/* Approval Tab - Supervisor approval */}
                <TabPanel>
                  <EquipmentUsageApproval onApprovalSubmitted={triggerRefresh} />
                </TabPanel>

                {/* Reports Tab - Export/Print */}
                <TabPanel>
                  <EquipmentUsageReports />
                </TabPanel>
              </TabPanels>
            </Tabs>
          </div>
        </Column>
      </Grid>
    </>
  );
};

export default EquipmentUsageManagement;
