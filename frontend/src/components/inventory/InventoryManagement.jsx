import React from "react";
import { Grid, Column } from "@carbon/react";
import { FormattedMessage } from "react-intl";
import PageBreadCrumb from "../common/PageBreadCrumb";
import InventoryItemsBoard from "./InventoryItemsBoard";
import ReceiveDelivery from "./ReceiveDelivery";
import InventoryReports from "./InventoryReports";
import "./InventoryList.css";

// No link on the Inventory crumb: it is a menu section with no page of its own.
const crumbsFor = (labelId) => [
  { label: "home.label", link: "/" },
  { label: "sidenav.label.inventory" },
  { label: labelId },
];

const InventoryPage = ({ titleId, children }) => (
  <>
    <PageBreadCrumb breadcrumbs={crumbsFor(titleId)} />
    <Grid fullWidth={true}>
      <Column lg={16} md={8} sm={4}>
        <div className="orderLegendBody">
          <h2>
            <FormattedMessage id={titleId} />
          </h2>
          {children}
        </div>
      </Column>
    </Grid>
  </>
);

export const InventoryItemsPage = () => (
  <InventoryPage titleId="sidenav.label.inventory.items">
    <InventoryItemsBoard />
  </InventoryPage>
);

export const InventoryReceivePage = () => (
  <InventoryPage titleId="sidenav.label.inventory.receive">
    <ReceiveDelivery />
  </InventoryPage>
);

export const InventoryReportsPage = () => (
  <InventoryPage titleId="sidenav.label.inventory.reports">
    <InventoryReports />
  </InventoryPage>
);

export default InventoryPage;
