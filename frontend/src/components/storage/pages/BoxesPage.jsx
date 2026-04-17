import React, { useState, useCallback } from "react";
import { useIntl } from "react-intl";
import StorageResourcePage, { ActiveTag } from "./StorageResourcePage";

/**
 * BoxesPage — /Storage/boxes. List of boxes with per-row Edit.
 * Edit uses a dedicated EditBoxPage (boxes have grid-layout fields
 * that don't fit the generic EditLocationPage shell).
 */
export default function BoxesPage() {
  const intl = useIntl();
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(25);

  const mapRow = useCallback(
    (b) => ({
      id: String(b.id),
      label: b.label || b.name || "",
      code: b.code || "",
      rack: b.parentRackLabel || b.rackLabel || "",
      capacity: b.capacityLimit || "",
      active: <ActiveTag active={b.active !== false} />,
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
    />
  );
}
