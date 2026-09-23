import React from "react";
import { Grid, Column } from "@carbon/react";
import { FormattedMessage } from "react-intl";
import PageBreadCrumb from "../common/PageBreadCrumb";
import InventoryItemsBoard from "./InventoryItemsBoard";
import ReceiveDelivery from "./ReceiveDelivery";
import InventoryReports from "./InventoryReports";
import "./InventoryList.css";

// Inventory is a menu section with three pages, not one page with three tabs, so
// the middle crumb carries no link: menu_inventory has no action_url either, and
// PageBreadCrumb renders a linkless crumb as text rather than as a dead link.
const crumbsFor = (labelId) => [
  { label: "home.label", link: "/" },
  { label: "sidenav.label.inventory" },
  { label: labelId },
];

/**
 * The chrome every inventory page shares. Each page is its own route, so the
 * board no longer needs telling whether it is the visible tab — navigating away
 * unmounts it and coming back mounts it again, which is the refetch.
 */
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
