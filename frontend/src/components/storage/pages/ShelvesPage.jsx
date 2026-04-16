import React, { useState, useCallback } from "react";
import { useIntl } from "react-intl";
import StorageResourcePage, { ActiveTag } from "./StorageResourcePage";

/**
 * ShelvesPage — /Storage/shelves. Read-only in Phase 9; Edit in Phase 10.
 */
export default function ShelvesPage() {
  const intl = useIntl();
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(25);

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
    />
  );
}
