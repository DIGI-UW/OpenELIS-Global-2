import { useState } from "react";
import {
  Grid,
  Column,
  Tabs,
  TabList,
  Tab,
  TabPanels,
  TabPanel,
} from "@carbon/react";
import { FormattedMessage } from "react-intl";
import PageBreadCrumb from "../common/PageBreadCrumb";
import EquipmentUsageLog from "./EquipmentUsageLog";
import EquipmentUsageDashboard from "./EquipmentUsageDashboard";
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
 * Provides tabbed interface with:
 * - Equipment Usage Log: Form for recording equipment usage
 * - Usage Metrics Dashboard: Aggregated statistics and recent submissions
 */
const EquipmentUsageManagement = () => {
  const [activeTabIndex, setActiveTabIndex] = useState(0);
  const [dashboardKey, setDashboardKey] = useState(0);
  const [lastSubmission, setLastSubmission] = useState(null);

  const handleSubmitSuccess = (submissionData) => {
    // Store submission and switch to metrics tab
    setLastSubmission(submissionData);
    setActiveTabIndex(1);
    // Force dashboard to re-render with new submission
    setDashboardKey((prev) => prev + 1);
  };

  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <div className="equipmentUsageManagementContainer">
            <h2>
              <FormattedMessage
                id="equipment.usage.title"
                defaultMessage="Equipment Usage"
              />
            </h2>

            <Tabs
              selectedIndex={activeTabIndex}
              onChange={({ selectedIndex }) => setActiveTabIndex(selectedIndex)}
            >
              <TabList aria-label="Equipment Usage Tabs">
                <Tab>
                  <FormattedMessage
                    id="equipment.usage.tab.log"
                    defaultMessage="Usage Log"
                  />
                </Tab>
                <Tab>
                  <FormattedMessage
                    id="equipment.usage.tab.metrics"
                    defaultMessage="Metrics Dashboard"
                  />
                </Tab>
              </TabList>
              <TabPanels>
                <TabPanel>
                  <EquipmentUsageLog onSubmitSuccess={handleSubmitSuccess} />
                </TabPanel>
                <TabPanel>
                  <EquipmentUsageDashboard
                    key={dashboardKey}
                    initialSubmission={lastSubmission}
                  />
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
