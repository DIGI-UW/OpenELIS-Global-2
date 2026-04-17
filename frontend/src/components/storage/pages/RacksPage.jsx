import React, { useState, useCallback } from "react";
import { useIntl } from "react-intl";
import StorageResourcePage, { ActiveTag } from "./StorageResourcePage";

/** RacksPage — /Storage/racks. List of racks with per-row Edit. */
export default function RacksPage() {
  const intl = useIntl();
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(25);

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
    />
  );
}
