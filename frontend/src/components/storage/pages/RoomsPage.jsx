import React, { useState, useCallback } from "react";
import { useIntl } from "react-intl";
import StorageResourcePage, { ActiveTag } from "./StorageResourcePage";

/** RoomsPage — /Storage/rooms. List of rooms with per-row Edit. */
export default function RoomsPage() {
  const intl = useIntl();
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(5);

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
    <StorageResourcePage
      level="room"
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
    />
  );
}
