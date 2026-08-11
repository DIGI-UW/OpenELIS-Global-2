import React, { useState, useCallback } from "react";
import { useHistory, useLocation } from "react-router-dom";
import { useIntl } from "react-intl";
import StorageResourcePage, { ActiveTag } from "./StorageResourcePage";
import DeleteLocationConfirmModal from "../components/DeleteLocationConfirmModal";

/** RoomsPage — /Storage/rooms. List of rooms with per-row Edit. */
export default function RoomsPage() {
  const intl = useIntl();
  const history = useHistory();
  const location = useLocation();
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(25);
  const [deleteTarget, setDeleteTarget] = useState(null);

  const mapRow = useCallback(
    (r) => ({
      id: String(r.id),
      name: r.name || r.label || "",
      code: r.code || "",
      active: <ActiveTag active={r.active !== false} />,
    }),
    [],
  );

  return (
    <>
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
              id: "storage.nav.rooms",
              defaultMessage: "Rooms",
            }),
            href: "/Storage/rooms",
          },
        ]}
        heading={intl.formatMessage({
          id: "storage.nav.rooms",
          defaultMessage: "Rooms",
        })}
        listUrl="/rest/storage/rooms"
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
        editHref={(room) => `/Storage/rooms/${room.id}/edit`}
        addHref="/Storage/rooms/new"
        onDeleteRequested={setDeleteTarget}
      />
      <DeleteLocationConfirmModal
        isOpen={Boolean(deleteTarget)}
        type="room"
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
