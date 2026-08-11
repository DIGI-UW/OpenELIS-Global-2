import React, { useState, useCallback, useContext } from "react";
import { useHistory, useLocation } from "react-router-dom";
import { useIntl } from "react-intl";
import StorageResourcePage, { ActiveTag } from "./StorageResourcePage";
import DeleteLocationConfirmModal from "../components/DeleteLocationConfirmModal";
import { NotificationContext } from "../../layout/Layout";
import { NotificationKinds } from "../../common/CustomNotification";
import { storageLevel } from "../storageLevels";
import AddLocationModal from "../components/AddLocationModal";

/**
 * BoxesPage — /Storage/boxes. List of boxes with per-row Edit.
 * Edit uses a dedicated EditBoxPage (boxes have grid-layout fields
 * that don't fit the generic EditLocationPage shell).
 */
export default function BoxesPage({ embedded = false }) {
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
  const level = storageLevel("box");
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
    (b) => ({
      id: String(b.id),
      label: b.label || b.name || "",
      code: b.code || "",
      rack: b.parentRackLabel || b.rackLabel || "",
      capacity: b.capacity ?? "",
      active: <ActiveTag active={b.active !== false} />,
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
              id: "storage.nav.boxes",
              defaultMessage: "Boxes",
            }),
            href: "/Storage/boxes",
          },
        ]}
        heading={intl.formatMessage({
          id: "storage.nav.boxes",
          defaultMessage: "Boxes",
        })}
        listUrl="/rest/storage/boxes"
        searchUrl="/rest/storage/boxes/search"
        searchPlaceholderId="storage.search.boxes.placeholder"
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
            key: "rack",
            header: intl.formatMessage({
              id: "storage.nav.rack",
              defaultMessage: "Rack",
            }),
          },
          {
            key: "capacity",
            header: intl.formatMessage({
              id: "storage.box.capacity",
              defaultMessage: "Capacity",
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
        editHref={(box) => `/Storage/boxes/${box.id}/edit`}
        onAddRequested={() => setAddOpen(true)}
        onDeleteRequested={setDeleteTarget}
      />
      <AddLocationModal
        level="box"
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
        type="box"
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
