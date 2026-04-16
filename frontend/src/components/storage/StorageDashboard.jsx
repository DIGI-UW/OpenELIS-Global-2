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
 * Phase 9 + 12 decomposition: this file was 4,902 lines juggling six
 * tab panels, modal state, pagination, search, and assignment wiring.
 * That entire stack is now in the per-resource pages under pages/
 * and the LocationPicker/ subsystem. What's left here is the landing:
 * a metrics overview + navigation tiles to the six resource pages.
 *
 * The Sample Items, Rooms, Devices, Shelves, Racks, and Boxes views
 * all live at their own URLs; click a tile to jump there. This page
 * is the right place for users who hit /Storage from the sidenav and
 * want an at-a-glance view before navigating into a specific resource.
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
