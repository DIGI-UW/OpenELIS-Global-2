import React, { useContext, useEffect, useMemo, useState } from "react";
import { useHistory, useLocation, useParams } from "react-router-dom";
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
import { NotificationContext } from "../layout/Layout";
import { AlertDialog } from "../common/CustomNotification";
import { getFromOpenElisServer } from "../utils/Utils";
import SampleItemsPage from "./pages/SampleItemsPage";
import InventoryLotsPage from "./pages/InventoryLotsPage";
import RoomsPage from "./pages/RoomsPage";
import DevicesPage from "./pages/DevicesPage";
import ShelvesPage from "./pages/ShelvesPage";
import RacksPage from "./pages/RacksPage";
import BoxesPage from "./pages/BoxesPage";
import "./StorageManagementPage.css";

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
 * Three tabs: Dashboard, Sample Items, Inventory Lots. The Dashboard tab
 * carries one counted tile per hierarchy level over a single table, and
 * picking a tile swaps which level that table shows.
 */

// Hierarchy order, not alphabetical.
const LEVELS = [
  {
    key: "rooms",
    labelId: "storage.nav.rooms",
    label: "Rooms",
    Page: RoomsPage,
  },
  {
    key: "devices",
    labelId: "storage.nav.devices",
    label: "Devices",
    Page: DevicesPage,
  },
  {
    key: "shelves",
    labelId: "storage.nav.shelves",
    label: "Shelves",
    Page: ShelvesPage,
  },
  {
    key: "racks",
    labelId: "storage.nav.racks",
    label: "Racks",
    Page: RacksPage,
  },
  {
    key: "boxes",
    labelId: "storage.nav.boxes",
    label: "Boxes",
    Page: BoxesPage,
  },
];

const TABS = ["dashboard", "sample-items", "inventory-lots"];

function LocationTiles({ activeLevel, onSelect }) {
  const intl = useIntl();
  const location = useLocation();
  const [counts, setCounts] = useState({});

  // Creating or deleting a location stamps ?t= on the URL to refresh the table
  // below; the tiles have to follow, or a new device leaves its count stale.
  const refreshKey = new URLSearchParams(location.search).get("t") || "initial";

  useEffect(() => {
    let mounted = true;
    getFromOpenElisServer("/rest/storage/dashboard/location-counts", (res) => {
      if (mounted && res) setCounts(res);
    });
    return () => {
      mounted = false;
    };
  }, [refreshKey]);

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
            className={
              level.key === activeLevel
                ? "storage-metric-tile storage-metric-tile--selected"
                : "storage-metric-tile"
            }
            aria-pressed={level.key === activeLevel}
            onClick={() => onSelect(level.key)}
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
  const intl = useIntl();
  const history = useHistory();
  const { notificationVisible } = useContext(NotificationContext);
  // The URL is the source of truth for the active tab, so a deep link and a
  // click land on the same place and the tab is shareable.
  const { resource } = useParams();

  const levelIndex = useMemo(
    () => LEVELS.findIndex((l) => l.key === resource),
    [resource],
  );
  // A level in the URL means the Dashboard tab with that level's table.
  const activeLevel = levelIndex >= 0 ? LEVELS[levelIndex] : LEVELS[0];

  const tabIndex = useMemo(() => {
    if (levelIndex >= 0) return 0;
    const direct = TABS.indexOf(resource);
    return direct > 0 ? direct : 0;
  }, [resource, levelIndex]);

  const LevelTable = activeLevel.Page;

  return (
    <>
      {notificationVisible === true ? <AlertDialog /> : ""}
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
              <TabList
                aria-label={intl.formatMessage({
                  id: "storage.tab.ariaLabel",
                  defaultMessage: "Storage management tabs",
                })}
                contained
              >
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
                {/* Carbon only hides an unselected TabPanel, so gate the
                    children or every tab fetches on every visit. */}
                <TabPanel>
                  {tabIndex === 0 && (
                    <>
                      <LocationTiles
                        activeLevel={activeLevel.key}
                        onSelect={(key) => history.push(`/Storage/${key}`)}
                      />
                      <LevelTable />
                    </>
                  )}
                </TabPanel>

                <TabPanel>{tabIndex === 1 && <SampleItemsPage />}</TabPanel>

                <TabPanel>{tabIndex === 2 && <InventoryLotsPage />}</TabPanel>
              </TabPanels>
            </Tabs>
          </div>
        </Column>
      </Grid>
    </>
  );
}
