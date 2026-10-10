import React, { useState, useCallback } from "react";
import { useIntl } from "react-intl";
import StorageResourcePage, { ActiveTag } from "./StorageResourcePage";

/** ShelvesPage — /Storage/shelves. List of shelves with per-row Edit. */
export default function ShelvesPage() {
  const intl = useIntl();
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(5);

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
      level="shelf"
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
    />
  );
}
