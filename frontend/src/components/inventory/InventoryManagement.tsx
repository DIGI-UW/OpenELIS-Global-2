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
import InventoryDashboard from "./InventoryDashboard";
import InventoryCatalog from "./InventoryCatalog";
import InventoryReports from "./InventoryReports";
import "./InventoryList.css";

const breadcrumbs = [
  { label: "home.label", link: "/", defaultMessage: "Home" },
  {
    label: "sidenav.label.inventory.management",
    link: "/inventory",
    defaultMessage: "Inventory Management",
  },
];

const InventoryManagement = () => {
  const [selectedTab, setSelectedTab] = useState(0);

  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <div className="orderLegendBody">
            <h2>
              <FormattedMessage id="inventory.list.title" />
            </h2>

            <Tabs
              selectedIndex={selectedTab}
              onChange={({ selectedIndex }) => setSelectedTab(selectedIndex)}
            >
              <TabList aria-label="Inventory management tabs" contained>
                <Tab>
                  <FormattedMessage id="inventory.tab.dashboard" />
                </Tab>
                <Tab>
                  <FormattedMessage id="inventory.tab.catalog" />
                </Tab>
                <Tab>
                  <FormattedMessage id="inventory.tab.reports" />
                </Tab>
              </TabList>

              <TabPanels>
                {/* Dashboard Tab - Metrics + Lots Table */}
                <TabPanel>
                  <InventoryDashboard />
                </TabPanel>

                {/* Catalog Tab - Manage Inventory Items */}
                <TabPanel>
                  <InventoryCatalog />
                </TabPanel>

                {/* Reports Tab - Generate Reports */}
                <TabPanel>
                  <InventoryReports />
                </TabPanel>
              </TabPanels>
            </Tabs>
          </div>
        </Column>
      </Grid>
    </>
  );
};

export default InventoryManagement;
