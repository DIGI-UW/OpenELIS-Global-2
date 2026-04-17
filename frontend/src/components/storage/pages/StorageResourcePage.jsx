import React, { useMemo } from "react";
import { Link, useLocation } from "react-router-dom";
import {
  DataTable,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableHeader,
  TableRow,
  Pagination,
  Tag,
  Loading,
} from "@carbon/react";
import { FormattedMessage } from "react-intl";
import BreadcrumbNav from "../components/BreadcrumbNav";
import useStorageTableData from "../hooks/useStorageTableData";

/**
 * StorageResourcePage — shared shell for the five per-resource storage
 * pages (Rooms, Devices, Shelves, Racks, Boxes).
 *
 * Each concrete page is a thin wrapper that passes the right config:
 *   - listUrl: backend endpoint (e.g. /rest/storage/rooms)
 *   - nameField: 'name' for Room/Device, 'label' for Shelf/Rack/Box
 *     (backend identifier-field naming is inconsistent)
 *   - parentLabel?: optional parent-hierarchy column header
 *   - editHref?: builder `(row) => "/Storage/.../edit"` to render a
 *     per-row Edit link
 */
export default function StorageResourcePage({
  crumbs,
  heading,
  listUrl,
  headers,
  mapRow,
  page,
  setPage,
  pageSize,
  setPageSize,
  editHref,
}) {
  const location = useLocation();

  const refreshKey = useMemo(
    () => new URLSearchParams(location.search).get("t") || "initial",
    [location.search],
  );

  const { items, totalItems, loading } = useStorageTableData({
    listUrl,
    page,
    pageSize,
    refreshKey,
  });

  const rows = useMemo(() => {
    const mapped = (items || []).map(mapRow);
    if (!editHref) return mapped;
    return mapped.map((row, idx) => {
      const rawItem = items[idx];
      return {
        ...row,
        actions: (
          <Link to={editHref(rawItem)}>
            <FormattedMessage id="label.edit" defaultMessage="Edit" />
          </Link>
        ),
      };
    });
  }, [items, mapRow, editHref]);

  const effectiveHeaders = useMemo(() => {
    if (!editHref) return headers;
    return [...headers, { key: "actions", header: "" }];
  }, [headers, editHref]);

  return (
    <div className="storage-resource-page">
      <BreadcrumbNav crumbs={crumbs} />
      <h1>{heading}</h1>

      {loading && <Loading small withOverlay={false} />}

      <DataTable rows={rows} headers={effectiveHeaders} isSortable>
        {({
          rows: r,
          headers: h,
          getTableProps,
          getHeaderProps,
          getRowProps,
        }) => (
          <TableContainer>
            <Table {...getTableProps()}>
              <TableHead>
                <TableRow>
                  {h.map((header) => (
                    <TableHeader
                      key={header.key}
                      {...getHeaderProps({ header })}
                    >
                      {header.header}
                    </TableHeader>
                  ))}
                </TableRow>
              </TableHead>
              <TableBody>
                {r.map((row) => (
                  <TableRow key={row.id} {...getRowProps({ row })}>
                    {row.cells.map((cell) => (
                      <TableCell key={cell.id}>{cell.value}</TableCell>
                    ))}
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        )}
      </DataTable>

      {!loading && (
        <Pagination
          page={page}
          pageSize={pageSize}
          pageSizes={[10, 25, 50, 100]}
          totalItems={totalItems}
          onChange={({ page: p, pageSize: s }) => {
            setPage(p);
            setPageSize(s);
          }}
        />
      )}
    </div>
  );
}

/**
 * Renders the Active/Inactive Tag consistently across pages. Kept
 * here (not exported as a separate component) because only the per-
 * resource pages use it; no point widening surface area.
 */
export function ActiveTag({ active }) {
  return active ? (
    <Tag type="green">
      <FormattedMessage id="label.active" defaultMessage="Active" />
    </Tag>
  ) : (
    <Tag type="gray">
      <FormattedMessage id="label.inactive" defaultMessage="Inactive" />
    </Tag>
  );
}
