import React, { useState, useCallback } from "react";
import { useHistory, useLocation } from "react-router-dom";
import { useIntl } from "react-intl";
import StorageResourcePage, { ActiveTag } from "./StorageResourcePage";
import DeleteLocationConfirmModal from "../components/DeleteLocationConfirmModal";
import AddLocationModal from "../components/AddLocationModal";

/** RacksPage — /Storage/racks. List of racks with per-row Edit. */
export default function RacksPage({ embedded = false }) {
  const intl = useIntl();
  const history = useHistory();
  const location = useLocation();
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(25);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [addOpen, setAddOpen] = useState(false);

  const mapRow = useCallback(
    (r) => ({
      id: String(r.id),
      label: r.label || r.name || "",
      code: r.code || "",
      shelf: r.parentShelfLabel || r.shelfLabel || "",
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
              id: "storage.nav.racks",
              defaultMessage: "Racks",
            }),
            href: "/Storage/racks",
          },
        ]}
        heading={intl.formatMessage({
          id: "storage.nav.racks",
          defaultMessage: "Racks",
        })}
        listUrl="/rest/storage/racks"
        searchUrl="/rest/storage/racks/search"
        searchPlaceholderId="storage.search.racks.placeholder"
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
            key: "shelf",
            header: intl.formatMessage({
              id: "storage.nav.shelf",
              defaultMessage: "Shelf",
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
        editHref={(rack) => `/Storage/racks/${rack.id}/edit`}
        onAddRequested={() => setAddOpen(true)}
        onDeleteRequested={setDeleteTarget}
      />
      <AddLocationModal
        level="rack"
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
        type="rack"
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
