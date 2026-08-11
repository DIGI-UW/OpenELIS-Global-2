import React, { useEffect, useMemo, useState } from "react";
import { useHistory, useParams } from "react-router-dom";
import {
  Tabs,
  TabList,
  Tab,
  TabPanels,
  TabPanel,
  Grid,
  Column,
  ClickableTile,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import PageBreadCrumb from "../common/PageBreadCrumb";
import { getFromOpenElisServer } from "../utils/Utils";
import SampleItemsPage from "./pages/SampleItemsPage";
import InventoryLotsPage from "./pages/InventoryLotsPage";
import "./StorageDashboard.css";

const breadcrumbs = [
  { label: "home.label", link: "/", defaultMessage: "Home" },
  {
    label: "sidenav.label.storage.management",
    link: "/Storage",
    defaultMessage: "Storage Management",
  },
];

/**
 * StorageManagementPage — /Storage.
 *
 * Storage used to spend six sidenav rows on one module, five of them
 * (Rooms/Devices/Shelves/Racks/Boxes) levels of a single hierarchy shown as
 * peers. They are now tiles on the Dashboard tab, counted and clickable, with
 * the occupant listings as tabs beside it — the same shape as Inventory
 * Management.
 */

// Hierarchy order, not alphabetical.
const LEVELS = [
  { key: "rooms", labelId: "storage.nav.rooms", label: "Rooms" },
  { key: "devices", labelId: "storage.nav.devices", label: "Devices" },
  { key: "shelves", labelId: "storage.nav.shelves", label: "Shelves" },
  { key: "racks", labelId: "storage.nav.racks", label: "Racks" },
  { key: "boxes", labelId: "storage.nav.boxes", label: "Boxes" },
];

const TABS = ["dashboard", "sample-items", "inventory-lots"];

function LocationTiles() {
  const intl = useIntl();
  const history = useHistory();
  const [counts, setCounts] = useState({});

  useEffect(() => {
    let mounted = true;
    getFromOpenElisServer("/rest/storage/dashboard/location-counts", (res) => {
      if (mounted && res) setCounts(res);
    });
    return () => {
      mounted = false;
    };
  }, []);

  return (
    <Grid className="storage-metrics-grid" fullWidth={false}>
      {LEVELS.map((level) => (
        <Column
          key={level.key}
          lg={3}
          md={2}
          sm={4}
          className="storage-metric-column"
        >
          <ClickableTile
            className="storage-metric-tile"
            onClick={() => history.push(`/Storage/${level.key}`)}
          >
            <div className="metric-value">{counts[level.key] ?? 0}</div>
            <div className="metric-label">
              {intl.formatMessage({
                id: level.labelId,
                defaultMessage: level.label,
              })}
            </div>
          </ClickableTile>
        </Column>
      ))}
    </Grid>
  );
}

export default function StorageManagementPage() {
  const history = useHistory();
  // The URL is the source of truth for the active tab, so a deep link and a
  // click land on the same place and the tab is shareable.
  const { resource } = useParams();

  const tabIndex = useMemo(() => {
    const direct = TABS.indexOf(resource);
    return direct > 0 ? direct : 0;
  }, [resource]);

  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <div className="orderLegendBody">
            <h2>
              <FormattedMessage
                id="sidenav.label.storage.management"
                defaultMessage="Storage Management"
              />
            </h2>

            <Tabs
              selectedIndex={tabIndex}
              onChange={({ selectedIndex }) => {
                const next = TABS[selectedIndex];
                history.push(
                  next === "dashboard" ? "/Storage" : `/Storage/${next}`,
                );
              }}
            >
              <TabList aria-label="Storage management tabs" contained>
                <Tab>
                  <FormattedMessage
                    id="storage.tab.dashboard"
                    defaultMessage="Dashboard"
                  />
                </Tab>
                <Tab>
                  <FormattedMessage
                    id="storage.tab.samples"
                    defaultMessage="Sample Items"
                  />
                </Tab>
                <Tab>
                  <FormattedMessage
                    id="storage.tab.inventoryLots"
                    defaultMessage="Inventory Lots"
                  />
                </Tab>
              </TabList>

              <TabPanels>
                <TabPanel>
                  <LocationTiles />
                </TabPanel>

                <TabPanel>
                  <SampleItemsPage embedded />
                </TabPanel>

                <TabPanel>
                  <InventoryLotsPage embedded />
                </TabPanel>
              </TabPanels>
            </Tabs>
          </div>
        </Column>
      </Grid>
    </>
  );
}
