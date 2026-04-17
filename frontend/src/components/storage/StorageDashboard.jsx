import React from "react";
import { Link } from "react-router-dom";
import { Grid, Column, ClickableTile } from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import BreadcrumbNav from "./components/BreadcrumbNav";
import StorageLocationsMetricCard from "./StorageDashboard/StorageLocationsMetricCard";
import "./StorageDashboard.css";

/**
 * StorageDashboard — /Storage landing page.
 *
 * Shows a storage-locations metric summary plus navigation tiles to
 * the six per-resource pages (Sample Items, Rooms, Devices, Shelves,
 * Racks, Boxes). Each resource has its own URL; this landing is for
 * users arriving via the sidenav who want an at-a-glance view before
 * drilling in.
 */
const RESOURCES = [
  {
    key: "sample-items",
    labelId: "storage.tab.samples",
    defaultLabel: "Sample Items",
  },
  { key: "rooms", labelId: "storage.nav.rooms", defaultLabel: "Rooms" },
  { key: "devices", labelId: "storage.nav.devices", defaultLabel: "Devices" },
  { key: "shelves", labelId: "storage.nav.shelves", defaultLabel: "Shelves" },
  { key: "racks", labelId: "storage.nav.racks", defaultLabel: "Racks" },
  { key: "boxes", labelId: "storage.nav.boxes", defaultLabel: "Boxes" },
];

export default function StorageDashboard() {
  const intl = useIntl();

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
    <div className="storage-dashboard">
      <BreadcrumbNav crumbs={crumbs} />
      <h1>
        <FormattedMessage
          id="storage.dashboard.title"
          defaultMessage="Storage"
        />
      </h1>

      <section
        className="storage-dashboard-metrics"
        style={{ margin: "1rem 0 2rem" }}
      >
        <StorageLocationsMetricCard />
      </section>

      <section className="storage-dashboard-nav">
        <Grid>
          {RESOURCES.map((r) => (
            <Column key={r.key} sm={2} md={4} lg={4}>
              <ClickableTile
                as={Link}
                to={`/Storage/${r.key}`}
                className="storage-dashboard-tile"
              >
                <h4>
                  <FormattedMessage
                    id={r.labelId}
                    defaultMessage={r.defaultLabel}
                  />
                </h4>
              </ClickableTile>
            </Column>
          ))}
        </Grid>
      </section>
    </div>
  );
}
