import React, { useState, useCallback, useContext } from "react";
import { useHistory, useLocation } from "react-router-dom";
import { useIntl } from "react-intl";
import StorageResourcePage, { ActiveTag } from "./StorageResourcePage";
import DeleteLocationConfirmModal from "../components/DeleteLocationConfirmModal";
import { NotificationContext } from "../../layout/Layout";
import { NotificationKinds } from "../../common/CustomNotification";
import { storageLevel } from "../storageLevels";
import AddLocationModal from "../components/AddLocationModal";

/** RoomsPage — /Storage/rooms. List of rooms with per-row Edit. */
export default function RoomsPage({ embedded = false }) {
  const intl = useIntl();
  const history = useHistory();
  const location = useLocation();
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(25);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [addOpen, setAddOpen] = useState(false);
  const { setNotificationVisible, addNotification } =
    useContext(NotificationContext);

  // Name the level being acted on — "Rack created", not a generic
  // "Storage location created" that reads identically for all five.
  const level = storageLevel("room");
  const notify = (kind, messageId, defaultMessage) => {
    setNotificationVisible(true);
    addNotification({
      kind,
      title: intl.formatMessage({
        id:
          kind === NotificationKinds.success
            ? "notification.title"
            : "notification.error",
      }),
      message: intl.formatMessage(
        { id: messageId, defaultMessage },
        {
          level: intl.formatMessage({
            id: level.labelId,
            defaultMessage: level.label,
          }),
        },
      ),
    });
  };

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
        searchUrl="/rest/storage/rooms/search"
        searchPlaceholderId="storage.search.rooms.placeholder"
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
        onAddRequested={() => setAddOpen(true)}
        onDeleteRequested={setDeleteTarget}
      />
      <AddLocationModal
        level="room"
        open={addOpen}
        onClose={() => setAddOpen(false)}
        onCreated={() => {
          setAddOpen(false);
          notify(
            NotificationKinds.success,
            "storage.location.created",
            "{level} created",
          );
          history.replace({
            pathname: location.pathname,
            search: `?t=${Date.now()}`,
          });
        }}
      />
      <DeleteLocationConfirmModal
        isOpen={Boolean(deleteTarget)}
        type="room"
        location={deleteTarget}
        onClose={() => setDeleteTarget(null)}
        onDeleted={() => {
          setDeleteTarget(null);
          notify(
            NotificationKinds.success,
            "storage.location.deleted",
            "{level} deleted",
          );
          history.replace({
            pathname: location.pathname,
            search: `?t=${Date.now()}`,
          });
        }}
      />
    </>
  );
}
