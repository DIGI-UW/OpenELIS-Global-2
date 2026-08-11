import React, { useState, useCallback } from "react";
import { useHistory, useLocation } from "react-router-dom";
import { useIntl } from "react-intl";
import StorageResourcePage, { ActiveTag } from "./StorageResourcePage";
import DeleteLocationConfirmModal from "../components/DeleteLocationConfirmModal";
import AddLocationModal from "../components/AddLocationModal";

/** DevicesPage — /Storage/devices. List of devices with per-row Edit. */
export default function DevicesPage({ embedded = false }) {
  const intl = useIntl();
  const history = useHistory();
  const location = useLocation();
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(25);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [addOpen, setAddOpen] = useState(false);

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
    <>
      <StorageResourcePage
        embedded={embedded}
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
        searchUrl="/rest/storage/devices/search"
        searchPlaceholderId="storage.search.devices.placeholder"
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
        onAddRequested={() => setAddOpen(true)}
        onDeleteRequested={setDeleteTarget}
      />
      <AddLocationModal
        level="device"
        open={addOpen}
        onClose={() => setAddOpen(false)}
        onCreated={() => {
          setAddOpen(false);
          history.replace({
            pathname: location.pathname,
            search: `?t=${Date.now()}`,
          });
        }}
      />
      <DeleteLocationConfirmModal
        isOpen={Boolean(deleteTarget)}
        type="device"
        location={deleteTarget}
        onClose={() => setDeleteTarget(null)}
        onDeleted={() => {
          setDeleteTarget(null);
          history.replace({
            pathname: location.pathname,
            search: `?t=${Date.now()}`,
          });
        }}
      />
    </>
  );
}
