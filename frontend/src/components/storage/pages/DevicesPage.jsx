import React, { useState, useCallback } from "react";
import { useIntl } from "react-intl";
import StorageResourcePage, { ActiveTag } from "./StorageResourcePage";

/** DevicesPage — /Storage/devices. List of devices with per-row Edit. */
export default function DevicesPage() {
  const intl = useIntl();
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(25);

  const mapRow = useCallback(
    (d) => ({
      id: String(d.id),
      name: d.name || d.label || "",
      code: d.code || "",
      room: d.parentRoomName || d.roomName || "",
      active: <ActiveTag active={d.active !== false} />,
    }),
    [],
  );

  return (
    <StorageResourcePage
      crumbs={[
        {
          label: intl.formatMessage({
            id: "storage.breadcrumb.storage",
            defaultMessage: "Storage",
          }),
          href: "/Storage",
        },
        {
          label: intl.formatMessage({
            id: "storage.nav.devices",
            defaultMessage: "Devices",
          }),
          href: "/Storage/devices",
        },
      ]}
      heading={intl.formatMessage({
        id: "storage.nav.devices",
        defaultMessage: "Devices",
      })}
      listUrl="/rest/storage/devices"
      headers={[
        {
          key: "name",
          header: intl.formatMessage({
            id: "label.name",
            defaultMessage: "Name",
          }),
        },
        {
          key: "code",
          header: intl.formatMessage({
            id: "label.code",
            defaultMessage: "Code",
          }),
        },
        {
          key: "room",
          header: intl.formatMessage({
            id: "storage.nav.room",
            defaultMessage: "Room",
          }),
        },
        {
          key: "active",
          header: intl.formatMessage({
            id: "label.status",
            defaultMessage: "Status",
          }),
        },
      ]}
      mapRow={mapRow}
      page={page}
      setPage={setPage}
      pageSize={pageSize}
      setPageSize={setPageSize}
      editHref={(device) => `/Storage/devices/${device.id}/edit`}
    />
  );
}
