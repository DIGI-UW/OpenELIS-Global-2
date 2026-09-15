import React, { useState, useCallback, useContext } from "react";
import { useHistory, useLocation } from "react-router-dom";
import { useIntl } from "react-intl";
import StorageResourcePage, { ActiveTag } from "./StorageResourcePage";
import DeleteLocationConfirmModal from "../components/DeleteLocationConfirmModal";
import { NotificationContext } from "../../layout/Layout";
import { NotificationKinds } from "../../common/CustomNotification";
import { storageLevel } from "../storageLevels";
import AddLocationModal from "../components/AddLocationModal";

/** ShelvesPage — /Storage/shelves. List of shelves with per-row Edit. */
export default function ShelvesPage({ embedded = false }) {
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
  const level = storageLevel("shelf");
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
    (s) => ({
      id: String(s.id),
      label: s.label || s.name || "",
      code: s.code || "",
      device: s.parentDeviceName || s.deviceName || "",
      active: <ActiveTag active={s.active !== false} />,
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
              id: "storage.nav.shelves",
              defaultMessage: "Shelves",
            }),
            href: "/Storage/shelves",
          },
        ]}
        heading={intl.formatMessage({
          id: "storage.nav.shelves",
          defaultMessage: "Shelves",
        })}
        listUrl="/rest/storage/shelves"
        searchUrl="/rest/storage/shelves/search"
        searchPlaceholderId="storage.search.shelves.placeholder"
        headers={[
          {
            key: "label",
            header: intl.formatMessage({
              id: "label.label",
              defaultMessage: "Label",
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
            key: "device",
            header: intl.formatMessage({
              id: "storage.nav.device",
              defaultMessage: "Device",
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
        editHref={(shelf) => `/Storage/shelves/${shelf.id}/edit`}
        onAddRequested={() => setAddOpen(true)}
        onDeleteRequested={setDeleteTarget}
      />
      <AddLocationModal
        level="shelf"
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
        type="shelf"
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
