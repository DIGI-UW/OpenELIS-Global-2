import React, { useMemo } from "react";
import { useHistory, useParams } from "react-router-dom";
import {
  Tabs,
  TabList,
  Tab,
  TabPanels,
  TabPanel,
  ContentSwitcher,
  Switch,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import BreadcrumbNav from "./components/BreadcrumbNav";
import StorageLocationsMetricCard from "./StorageDashboard/StorageLocationsMetricCard";
import SampleItemsPage from "./pages/SampleItemsPage";
import InventoryLotsPage from "./pages/InventoryLotsPage";
import RoomsPage from "./pages/RoomsPage";
import DevicesPage from "./pages/DevicesPage";
import ShelvesPage from "./pages/ShelvesPage";
import RacksPage from "./pages/RacksPage";
import BoxesPage from "./pages/BoxesPage";
import "./StorageDashboard.css";

/**
 * StorageManagementPage — /Storage and its per-resource URLs.
 *
 * Storage used to spend seven sidenav entries on one module, presenting
 * Room/Device/Shelf/Rack/Box as five peers when they are levels of a single
 * hierarchy, and mixing them with the occupant listings. This gathers them
 * under one destination: containers live behind the Locations tab's level
 * switcher, occupants are tabs of their own.
 *
 * Every previous URL still resolves — /Storage/racks selects Locations with
 * Racks active — so bookmarks and the menu table keep working.
 */

// Level order matches the hierarchy, not the alphabet.
const LEVELS = [
  {
    slug: "rooms",
    labelId: "storage.nav.rooms",
    label: "Rooms",
    Page: RoomsPage,
  },
  {
    slug: "devices",
    labelId: "storage.nav.devices",
    label: "Devices",
    Page: DevicesPage,
  },
  {
    slug: "shelves",
    labelId: "storage.nav.shelves",
    label: "Shelves",
    Page: ShelvesPage,
  },
  {
    slug: "racks",
    labelId: "storage.nav.racks",
    label: "Racks",
    Page: RacksPage,
  },
  {
    slug: "boxes",
    labelId: "storage.nav.boxes",
    label: "Boxes",
    Page: BoxesPage,
  },
];

const TABS = ["overview", "locations", "sample-items", "inventory-lots"];

export default function StorageManagementPage() {
  const intl = useIntl();
  const history = useHistory();
  // The route is /Storage/:resource?, so the URL is the single source of
  // truth for which tab and level are showing.
  const { resource } = useParams();

  const levelIndex = useMemo(
    () => LEVELS.findIndex((l) => l.slug === resource),
    [resource],
  );

  const tabIndex = useMemo(() => {
    if (levelIndex >= 0) return TABS.indexOf("locations");
    const direct = TABS.indexOf(resource);
    return direct >= 0 ? direct : 0;
  }, [resource, levelIndex]);

  const activeLevel = levelIndex >= 0 ? levelIndex : 0;

  const goTo = (slug) => {
    history.push(slug === "overview" ? "/Storage" : `/Storage/${slug}`);
  };

  const crumbs = [
    {
      label: intl.formatMessage({
        id: "storage.breadcrumb.storage",
        defaultMessage: "Storage",
      }),
      href: "/Storage",
    },
  ];

  return (
    <div className="storage-management-page pageContent">
      <BreadcrumbNav crumbs={crumbs} />
      <h1>
        <FormattedMessage
          id="storage.dashboard.title"
          defaultMessage="Storage"
        />
      </h1>

      <Tabs
        selectedIndex={tabIndex}
        onChange={({ selectedIndex }) => {
          const next = TABS[selectedIndex];
          goTo(next === "locations" ? LEVELS[activeLevel].slug : next);
        }}
      >
        <TabList aria-label="Storage management sections" contained>
          <Tab>
            <FormattedMessage
              id="storage.tab.overview"
              defaultMessage="Overview"
            />
          </Tab>
          <Tab>
            <FormattedMessage
              id="storage.tab.locations"
              defaultMessage="Locations"
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
            <StorageLocationsMetricCard />
          </TabPanel>

          <TabPanel>
            <div
              className="storage-management-level-switcher"
              style={{ margin: "1rem 0" }}
            >
              <ContentSwitcher
                selectedIndex={activeLevel}
                onChange={({ index }) => goTo(LEVELS[index].slug)}
              >
                {LEVELS.map((level) => (
                  <Switch
                    key={level.slug}
                    name={level.slug}
                    text={intl.formatMessage({
                      id: level.labelId,
                      defaultMessage: level.label,
                    })}
                  />
                ))}
              </ContentSwitcher>
            </div>
            {React.createElement(LEVELS[activeLevel].Page, { embedded: true })}
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
  );
}
