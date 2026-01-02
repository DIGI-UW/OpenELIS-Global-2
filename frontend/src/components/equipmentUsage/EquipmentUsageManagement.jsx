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
import EquipmentUsageLog from "./EquipmentUsageLog";
import EquipmentUsageHistory from "./EquipmentUsageHistory";
import EquipmentUsagePrintableReport from "./EquipmentUsagePrintableReport";
import "./EquipmentUsage.css";

const breadcrumbs = [
  { label: "home.label", link: "/", defaultMessage: "Home" },
  {
    label: "sidenav.label.inventory.management",
    link: "/inventory",
    defaultMessage: "Inventory Management",
  },
  {
    label: "equipment.usage.title",
    link: "/equipment-usage",
    defaultMessage: "Equipment Usage",
  },
];

/**
 * EquipmentUsageManagement Component
 *
 * Main container for equipment (cartridge) usage tracking feature.
 * Provides tabs for:
 * - Equipment Usage Log: Record new cartridge usage
 * - Usage History: View historical usage records
 * - Reports: Generate and view usage reports
 */
const EquipmentUsageManagement = () => {
  const [selectedTab, setSelectedTab] = useState(0);

  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <div className="equipmentUsageManagementContainer">
            <h2>
              <FormattedMessage
                id="equipment.usage.title"
                defaultMessage="Equipment Usage Log"
              />
            </h2>

            <Tabs
              selectedIndex={selectedTab}
              onChange={({ selectedIndex }) => setSelectedTab(selectedIndex)}
            >
              <TabList aria-label="Equipment usage management tabs" contained>
                <Tab>
                  <FormattedMessage
                    id="equipment.usage.tab.log"
                    defaultMessage="Equipment Usage Log"
                  />
                </Tab>
                <Tab>
                  <FormattedMessage
                    id="equipment.usage.tab.history"
                    defaultMessage="Usage History"
                  />
                </Tab>
                <Tab>
                  <FormattedMessage
                    id="equipment.usage.tab.reports"
                    defaultMessage="Reports"
                  />
                </Tab>
              </TabList>

              <TabPanels>
                {/* Equipment Usage Log Tab - Record new usage */}
                <TabPanel>
                  <EquipmentUsageLog />
                </TabPanel>

                {/* Usage History Tab - View historical records */}
                <TabPanel>
                  <EquipmentUsageHistory />
                </TabPanel>

                {/* Reports Tab - Generate usage reports */}
                <TabPanel>
                  <EquipmentUsagePrintableReport />
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
