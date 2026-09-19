import React, { useState, useCallback } from "react";
import { useIntl } from "react-intl";
import StorageResourcePage, { ActiveTag } from "./StorageResourcePage";

/** BoxesPage — /Storage/boxes. List of boxes with per-row Edit. */
export default function BoxesPage() {
  const intl = useIntl();
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(5);

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
    <StorageResourcePage
      level="box"
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
    />
  );
}
