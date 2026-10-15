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
import InventoryItemsBoard from "./InventoryItemsBoard";
import ReceiveDelivery from "./ReceiveDelivery";
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
  const [boardVersion, setBoardVersion] = useState(0);

  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <div className="orderLegendBody">
            <h1 className="page-heading-05">
              <FormattedMessage id="inventory.list.title" />
            </h1>

            <Tabs
              selectedIndex={selectedTab}
              onChange={({ selectedIndex }) => setSelectedTab(selectedIndex)}
            >
              <TabList aria-label="Inventory management tabs" contained>
                <Tab>
                  <FormattedMessage id="inventory.board.title" />
                </Tab>
                <Tab>
                  <FormattedMessage id="inventory.tab.receive" />
                </Tab>
                <Tab>
                  <FormattedMessage id="inventory.tab.reports" />
                </Tab>
              </TabList>

              <TabPanels>
                {/* Items Tab - Items Board */}
                <TabPanel>
                  <InventoryItemsBoard reloadKey={boardVersion} />
                </TabPanel>

                <TabPanel>
                  <ReceiveDelivery
                    onSaved={() => setBoardVersion((version) => version + 1)}
                  />
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
